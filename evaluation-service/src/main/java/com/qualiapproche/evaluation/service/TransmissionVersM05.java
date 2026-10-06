package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.dto.NonConformiteDAuditDto;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.evaluation.client.NonConformitesClient;
import com.qualiapproche.evaluation.dto.TransmissionDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.entities.NotationCritere;
import com.qualiapproche.evaluation.enumeration.ConstatRQAPBF;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.NotationCritereRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * La transmission des écarts d'audit au module Non-conformités (M05).
 *
 * <p>« Le module Audit ne gère pas les plans d'actions » : un écart publié y part, et le
 * traitement — analyse des causes, plan d'actions, délais — se mène là-bas. Le dossier est
 * ouvert dans la structure auditée et soumis à son pilote, sous le nom de l'auditeur qui publie.</p>
 *
 * <p><b>Après la publication, jamais pendant.</b> L'appel part une fois la publication committée :
 * un échec du module Non-conformités ne doit pas défaire une décision d'équipe. L'écart reste
 * alors publié sans dossier, la fiche le signale, et la reprise depuis l'audit le renvoie. La
 * clé d'origine rend le renvoi inoffensif : un dossier déjà ouvert est retrouvé, pas doublé.</p>
 *
 * <p>Chaque écart est traité dans sa propre transaction, courte, de part et d'autre de l'appel
 * réseau : on ne garde pas une connexion à la base ouverte pendant qu'un autre service répond.</p>
 */
@Slf4j
@Component
public class TransmissionVersM05 {

    private final NonConformitesClient nonConformites;
    private final ConstatRepository constats;
    private final NotationCritereRepository notations;
    private final TransactionTemplate nouvelleTransaction;

    public TransmissionVersM05(NonConformitesClient nonConformites, ConstatRepository constats,
                               NotationCritereRepository notations, PlatformTransactionManager transactions) {
        this.nonConformites = nonConformites;
        this.constats = constats;
        this.notations = notations;
        this.nouvelleTransaction = new TransactionTemplate(transactions);
        this.nouvelleTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Dans la requête qui a publié, une fois sa transaction committée : le jeton est encore là. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void apresPublication(EcartsPublies ecarts) {
        TransmissionDto resultat = transmettre(ecarts);
        if (!resultat.getEchecs().isEmpty()) {
            log.warn("{} écart(s) d'audit restent à transmettre au module Non-conformités : {}",
                    resultat.getEchecs().size(), resultat.getEchecs());
        }
    }

    /** Reprend tout ce qui, sur cet audit, est publié comme écart mais n'a pas encore de dossier. */
    public TransmissionDto reprendre(UUID auditId) {
        List<UUID> constatIds = lire(() -> constats.findAllByAuditIdAndStatutOrderByDateSaisieAsc(auditId, StatutConstat.PUBLIE)
                .stream().filter(c -> c.estNonConformite() && c.getNonConformiteId() == null).map(Constat::getId).toList());
        List<UUID> notationIds = lire(() -> notations.publieesSansDossier(auditId).stream()
                .filter(n -> n.constatRetenu() == ConstatRQAPBF.NC).map(NotationCritere::getId).toList());
        return transmettre(new EcartsPublies(constatIds, notationIds));
    }

    TransmissionDto transmettre(EcartsPublies ecarts) {
        TransmissionDto resultat = new TransmissionDto();
        ecarts.constatIds().forEach(id -> envoyer(id, this::ecartDuConstat, this::inscrireSurLeConstat, resultat));
        ecarts.notationIds().forEach(id -> envoyer(id, this::ecartDeLaNotation, this::inscrireSurLaNotation, resultat));
        return resultat;
    }

    private void envoyer(UUID id, Function<UUID, Preparation> preparer,
                         java.util.function.BiConsumer<UUID, UUID> inscrire, TransmissionDto resultat) {
        Preparation preparation = lire(() -> preparer.apply(id));
        if (preparation == null) {
            return;
        }
        if (preparation.raisonDeNePasPartir() != null) {
            resultat.getEchecs().add(preparation.libelle() + " : " + preparation.raisonDeNePasPartir());
            return;
        }
        try {
            NonConformiteDto dossier = nonConformites.transmettre(preparation.ecart());
            nouvelleTransaction.executeWithoutResult(s -> inscrire.accept(id, dossier.getId()));
            resultat.setTransmis(resultat.getTransmis() + 1);
            log.info("{} transmis au module Non-conformités : {}.", preparation.libelle(), dossier.getNumeroReference());
        } catch (Exception e) {
            log.error("Transmission de « {} » au module Non-conformités impossible : {}",
                    preparation.libelle(), e.getMessage());
            resultat.getEchecs().add(preparation.libelle() + " : le module Non-conformités n'a pas répondu"
                    + " ou a refusé la transmission.");
        }
    }

    /** Ce qui part pour un écart, ou la raison pour laquelle il ne peut pas partir. */
    record Preparation(String libelle, NonConformiteDAuditDto ecart, String raisonDeNePasPartir) {
    }

    private Preparation ecartDuConstat(UUID id) {
        Constat c = constats.findById(id).orElse(null);
        if (c == null || c.getNonConformiteId() != null || c.getStatut() != StatutConstat.PUBLIE || !c.estNonConformite()) {
            return null;
        }
        Audit audit = c.getAudit();
        String origine = c.getPointControle() != null ? c.getPointControle().getLibelle()
                : c.getCritereAudit() != null ? c.getCritereAudit() : "constat";
        String libelle = "Constat « " + court(origine) + " » de " + audit.getReference();
        StringBuilder justification = new StringBuilder()
                .append("Constat d'audit ").append(audit.getReference()).append(" — ")
                .append(c.getNature().getLibelle()).append(".\n");
        ligne(justification, "Point de contrôle", c.getPointControle() == null ? null : c.getPointControle().getLibelle());
        ligne(justification, "Critère", c.getCritereAudit());
        ligne(justification, "Observation", c.getObservation());
        ligne(justification, "Commentaire de l'équipe", c.getCommentaireEquipe());
        return preparation(libelle, audit, "AUDIT-CONSTAT:" + id, justification.toString(),
                "Audit " + audit.getReference() + " — " + court(origine), c.getNature().getNiveauNonConformiteId());
    }

    private Preparation ecartDeLaNotation(UUID id) {
        NotationCritere n = notations.findById(id).orElse(null);
        if (n == null || n.getNonConformiteId() != null || !n.estPublie() || n.constatRetenu() != ConstatRQAPBF.NC) {
            return null;
        }
        Audit audit = n.getEvaluation().getAudit();
        String libelle = "Critère RQAP-BF " + n.getNoeud().getCode() + " de " + audit.getReference();
        StringBuilder justification = new StringBuilder()
                .append("Évaluation RQAP-BF de l'audit ").append(audit.getReference()).append(" — critère ")
                .append(n.getNoeud().getCode()).append(" : ").append(n.getNoeud().getLibelle()).append('\n');
        ligne(justification, "Niveau de conformité", n.getNiveau().name());
        ligne(justification, "Éléments de preuve", n.getPreuve());
        ligne(justification, "Commentaire", n.getCommentaire());
        return preparation(libelle, audit, "AUDIT-RQAPBF:" + id, justification.toString(),
                "Audit " + audit.getReference() + " — RQAP-BF " + n.getNoeud().getCode(), null);
    }

    private static Preparation preparation(String libelle, Audit audit, String cle, String justification,
                                           String origine, UUID niveau) {
        if (audit.getProcessusId() == null) {
            return new Preparation(libelle, null, "l'audit ne désigne pas de processus audité, à qui"
                    + " adresser la non-conformité");
        }
        return new Preparation(libelle, NonConformiteDAuditDto.builder()
                .cleOrigine(cle)
                .structureConcerneeId(audit.getProcessusId().toString())
                .justification(justification)
                .origineLibelle(origine)
                .typeNonConformiteId(audit.getType() == null ? null : audit.getType().getTypeNonConformiteId())
                .niveauNonConformiteId(niveau)
                .soumettre(true)
                .build(), null);
    }

    private void inscrireSurLeConstat(UUID id, UUID dossier) {
        constats.findById(id).ifPresent(c -> {
            c.setNonConformiteId(dossier);
            constats.save(c);
        });
    }

    private void inscrireSurLaNotation(UUID id, UUID dossier) {
        notations.findById(id).ifPresent(n -> {
            n.setNonConformiteId(dossier);
            notations.save(n);
        });
    }

    private <T> T lire(java.util.function.Supplier<T> lecture) {
        return nouvelleTransaction.execute(s -> lecture.get());
    }

    private static void ligne(StringBuilder texte, String titre, String valeur) {
        if (valeur != null && !valeur.isBlank()) {
            texte.append(titre).append(" : ").append(valeur.strip()).append('\n');
        }
    }

    private static String court(String texte) {
        String net = texte.strip();
        return net.length() > 80 ? net.substring(0, 80) + "…" : net;
    }
}
