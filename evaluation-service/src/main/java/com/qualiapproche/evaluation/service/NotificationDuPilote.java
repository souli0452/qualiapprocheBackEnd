package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.messagerie.Courriels;
import com.qualiapproche.evaluation.client.AnnuaireClient;
import com.qualiapproche.evaluation.client.StructuresClient;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le pilote du processus audité apprend la publication des constats qui le concernent.
 *
 * <p>Pilotes : les porteurs du rôle {@value #ROLE_PILOTE} dans la structure auditée, comme pour
 * les non-conformités. Un courriel par pilote et par validation, qui liste les constats publiés :
 * valider la compilation d'un audit ne fait pas trente messages.</p>
 *
 * <p><b>Après la publication, jamais pendant</b>, et sans jamais la défaire : un annuaire
 * injoignable, un pilote sans adresse ou une messagerie absente se disent dans le journal, la
 * validation de l'équipe reste acquise. Dans la requête qui valide, une fois committée : le jeton
 * de l'auditeur est encore là pour interroger l'annuaire.</p>
 *
 * <p><b>Les courriels sont déposés en base, puis remis par workflow-service.</b> Les pilotes sont
 * résolus dans la requête, qui porte le jeton de l'auditeur que l'annuaire exige ; chaque courriel
 * est rendu puis déposé dans la boîte d'envoi du service ({@link Courriels}). Ils partaient d'une file
 * en mémoire : un redémarrage, ou une file pleine, les perdait sans recours. Sans messagerie
 * configurée, ils attendent qu'elle le soit.</p>
 */
@Slf4j
@Component
public class NotificationDuPilote {

    static final String ROLE_PILOTE = "PILOTE";
    static final String GABARIT = "courriel/constatsPublies";

    private final ConstatRepository constats;
    private final AnnuaireClient annuaire;
    private final StructuresClient structures;
    private final Courriels courriels;
    private final TemplateEngine moteur;
    private final String lienAudit;
    private final TransactionTemplate lecture;

    @Autowired
    public NotificationDuPilote(ConstatRepository constats, AnnuaireClient annuaire, StructuresClient structures,
                                Courriels courriels, TemplateEngine moteur,
                                @Value("${qualisira.audit.lien-audit:}") String lienAudit,
                                PlatformTransactionManager transactions) {
        this.constats = constats;
        this.annuaire = annuaire;
        this.structures = structures;
        this.courriels = courriels;
        this.moteur = moteur;
        this.lienAudit = lienAudit;
        this.lecture = new TransactionTemplate(transactions);
        this.lecture.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.lecture.setReadOnly(true);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void apresPublication(ConstatsPublies publies) {
        try {
            notifier(publies);
        } catch (Exception e) {
            log.error("Le pilote du processus audité n'a pas pu être prévenu de la publication de {} constat(s)"
                    + " de l'audit {} : {}", publies.constatIds().size(), publies.auditId(), e.getMessage());
        }
    }

    void notifier(ConstatsPublies publies) {
        Annonce annonce = lecture.execute(s -> annonce(publies));
        if (annonce == null) {
            return;
        }
        List<Map<String, Object>> pilotes = pilotes(annonce.processusId());
        if (pilotes.isEmpty()) {
            log.warn("Publication de {} constat(s) de {} : la structure auditée {} n'a aucun pilote joignable.",
                    annonce.constats().size(), annonce.reference(), annonce.processusId());
            return;
        }
        String processus = libelleDuProcessus(annonce.processusId());
        List<Envoi> aFaire = new ArrayList<>();
        String sujet = "Audit " + annonce.reference() + " — " + annonce.constats().size()
                + (annonce.constats().size() > 1 ? " constats publiés" : " constat publié");
        for (Map<String, Object> pilote : pilotes) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("nomComplet", texteOu(pilote.get("nomComplet"), "Madame, Monsieur"));
            variables.put("reference", annonce.reference());
            variables.put("processus", processus);
            variables.put("constats", annonce.constats());
            variables.put("ecarts", annonce.constats().stream().filter(c -> Boolean.TRUE.equals(c.get("ecart"))).count());
            variables.put("lien", lienAudit.isBlank() ? null : lienAudit.replace("{id}", annonce.auditId().toString()));
            aFaire.add(new Envoi(texteOu(pilote.get("email"), null), variables));
        }
        for (Envoi e : aFaire) {
            try {
                Context contexte = new Context();
                contexte.setVariables(e.variables());
                courriels.envoyer(e.adresse(), null, sujet, moteur.process(GABARIT, contexte), "publication-constats");
            } catch (Exception ex) {
                // Un pilote qu'on n'a pas pu inscrire n'empêche pas les autres d'être prévenus.
                log.error("Courriel de publication des constats de {} à {} non déposé : {}",
                        annonce.reference(), e.adresse(), ex.getMessage());
            }
        }
    }

    /** Ce qui s'annonce, lu dans sa propre transaction : la publication est déjà committée. */
    private Annonce annonce(ConstatsPublies publies) {
        List<Constat> lus = constats.findAllById(publies.constatIds()).stream()
                .filter(c -> c.getStatut() == StatutConstat.PUBLIE).toList();
        if (lus.isEmpty()) {
            return null;
        }
        Audit audit = lus.get(0).getAudit();
        if (audit.getProcessusId() == null) {
            log.warn("Publication de constats de {} : l'audit ne désigne pas de processus audité,"
                    + " il n'y a pas de pilote à prévenir.", audit.getReference());
            return null;
        }
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (Constat c : lus) {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("nature", c.getNature() == null ? "—" : c.getNature().getLibelle());
            ligne.put("ecart", c.estNonConformite());
            ligne.put("point", c.getPointControle() != null ? c.getPointControle().getLibelle() : c.getCritereAudit());
            ligne.put("observation", c.getObservation());
            lignes.add(ligne);
        }
        return new Annonce(audit.getId(), audit.getReference(), audit.getProcessusId(), lignes);
    }

    private List<Map<String, Object>> pilotes(UUID processusId) {
        Map<String, Object> reponse = annuaire.porteursDuRole(ROLE_PILOTE, processusId.toString());
        Object data = reponse == null ? null : reponse.get("data");
        if (!(data instanceof List<?> liste)) {
            return List.of();
        }
        List<Map<String, Object>> joignables = new ArrayList<>();
        for (Object element : liste) {
            if (element instanceof Map<?, ?> personne && texteOu(personne.get("email"), null) != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> p = (Map<String, Object>) personne;
                joignables.add(p);
            }
        }
        return joignables;
    }

    /** Le nom du processus, pour le courriel ; l'identifiant fait l'affaire si le référentiel se tait. */
    private String libelleDuProcessus(UUID processusId) {
        try {
            var structure = structures.structure(processusId);
            if (structure != null) {
                return texteOu(structure.getLibelleLong(), texteOu(structure.getLibelleCourt(), processusId.toString()));
            }
        } catch (Exception e) {
            log.warn("Libellé du processus {} illisible auprès du référentiel : {}", processusId, e.getMessage());
        }
        return processusId.toString();
    }

    private static String texteOu(Object valeur, String defaut) {
        if (valeur == null || valeur.toString().isBlank()) {
            return defaut;
        }
        return valeur.toString().strip();
    }

    private record Envoi(String adresse, Map<String, Object> variables) {
    }

    private record Annonce(UUID auditId, String reference, UUID processusId, List<Map<String, Object>> constats) {
    }
}
