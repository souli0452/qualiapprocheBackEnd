package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.utils.CourrielParGabarit;
import com.qualiapproche.common.utils.EmailMessage;
import com.qualiapproche.evaluation.client.AnnuaireClient;
import com.qualiapproche.evaluation.client.StructuresClient;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.thymeleaf.TemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

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
 * <p><b>L'envoi part hors de la requête.</b> Les pilotes sont résolus dans la requête, qui porte
 * le jeton de l'auditeur que l'annuaire exige ; seul l'échange SMTP est confié à un fil à part.
 * Un serveur de messagerie lent ou injoignable ne retient donc plus la validation de l'équipe
 * jusqu'à ses délais d'attente, multipliés par le nombre de pilotes.</p>
 *
 * <p>Sans messagerie configurée ({@code spring.mail.host}), rien ne part et le journal le dit :
 * les variables de serveur relèvent du déploiement.</p>
 */
@Slf4j
@Component
public class NotificationDuPilote implements DisposableBean {

    static final String ROLE_PILOTE = "PILOTE";
    static final String GABARIT = "courriel/constatsPublies";

    private final ConstatRepository constats;
    private final AnnuaireClient annuaire;
    private final StructuresClient structures;
    private final ObjectProvider<CourrielParGabarit> courriel;
    private final TemplateEngine moteur;
    private final String lienAudit;
    private final TransactionTemplate lecture;
    private final Executor envois;
    /** Le fil d'envoi que ce composant a créé, et qu'il doit donc arrêter ; nul en test. */
    private final ThreadPoolTaskExecutor filPropre;

    @Autowired
    public NotificationDuPilote(ConstatRepository constats, AnnuaireClient annuaire, StructuresClient structures,
                                ObjectProvider<CourrielParGabarit> courriel, TemplateEngine moteur,
                                @Value("${qualisira.audit.lien-audit:}") String lienAudit,
                                PlatformTransactionManager transactions) {
        this(constats, annuaire, structures, courriel, moteur, lienAudit, transactions, filDEnvoi());
    }

    NotificationDuPilote(ConstatRepository constats, AnnuaireClient annuaire, StructuresClient structures,
                         ObjectProvider<CourrielParGabarit> courriel, TemplateEngine moteur, String lienAudit,
                         PlatformTransactionManager transactions, Executor envois) {
        this.envois = envois;
        this.filPropre = envois instanceof ThreadPoolTaskExecutor fil ? fil : null;
        this.constats = constats;
        this.annuaire = annuaire;
        this.structures = structures;
        this.courriel = courriel;
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
        CourrielParGabarit envoi = courriel.getIfAvailable();
        if (envoi == null) {
            log.warn("Publication de {} constat(s) de {} : aucune messagerie configurée (spring.mail.host),"
                    + " le pilote n'est pas prévenu.", annonce.constats().size(), annonce.reference());
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
        try {
            envois.execute(() -> envoyer(envoi, annonce.reference(), sujet, aFaire));
        } catch (TaskRejectedException e) {
            log.error("Courriels de publication des constats de {} abandonnés : la file d'envoi est pleine"
                    + " ({} pilote(s) non prévenu(s)).", annonce.reference(), aFaire.size());
        }
    }

    /** Hors de la requête : rien d'ici ne doit lire le jeton ni la base. */
    private void envoyer(CourrielParGabarit envoi, String reference, String sujet, List<Envoi> aFaire) {
        for (Envoi e : aFaire) {
            try {
                envoi.envoyer(EmailMessage.builder().toAddress(e.adresse()).subject(sujet).build(),
                        e.variables(), List.of(), GABARIT, moteur);
            } catch (Exception ex) {
                // Un pilote injoignable n'empêche pas les autres d'être prévenus.
                log.error("Courriel de publication des constats de {} à {} impossible : {}",
                        reference, e.adresse(), ex.getMessage());
            }
        }
    }

    /**
     * Un fil, une file bornée : les courriels d'un audit partent dans l'ordre, et un serveur de
     * messagerie tombé n'accumule pas sans fin des envois en mémoire. À l'arrêt, les envois en
     * cours ont le temps de finir.
     */
    private static ThreadPoolTaskExecutor filDEnvoi() {
        ThreadPoolTaskExecutor fil = new ThreadPoolTaskExecutor();
        fil.setCorePoolSize(1);
        fil.setMaxPoolSize(1);
        fil.setQueueCapacity(500);
        fil.setThreadNamePrefix("courriel-audit-");
        fil.setWaitForTasksToCompleteOnShutdown(true);
        fil.setAwaitTerminationSeconds(30);
        fil.initialize();
        return fil;
    }

    @Override
    public void destroy() {
        if (filPropre != null) {
            filPropre.shutdown();
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
