package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.messagerie.Courriels;
import com.qualiapproche.evaluation.client.AnnuaireClient;
import com.qualiapproche.evaluation.client.StructuresClient;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.repository.AuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Les courriels qui annoncent un audit validé et un plan partagé, et la liste de leurs
 * destinataires, que l'écran montre avant le geste.
 *
 * <p>« Valider et notifier » et « Partager avec l'audité » ne prévenaient personne par courriel,
 * et l'écran ne disait pas qui le serait (recette du 17 oct. 2026, observation 14). L'audit validé
 * s'annonce à l'équipe d'audit et aux pilotes du processus audité ; le plan partagé, aux pilotes.</p>
 *
 * <p>Comme pour la publication des constats : après le commit, jamais pendant, et sans défaire le
 * geste si l'annuaire ou la messagerie font défaut — le journal le dit.</p>
 */
@Slf4j
@Component
public class AnnoncesDeLAudit {

    static final String GABARIT = "courriel/annonceAudit";
    static final String ROLE_PILOTE = NotificationDuPilote.ROLE_PILOTE;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Ce qui s'annonce. */
    public enum Geste { VALIDATION, PARTAGE_PLAN }

    /** L'événement publié par le service, traité après commit. */
    public record Annonce(UUID auditId, Geste geste) {
    }

    /** Un destinataire : son nom et son adresse, et à quel titre il est prévenu. */
    public record Destinataire(String nom, String email, String titre) {
    }

    private final AuditRepository audits;
    private final AnnuaireClient annuaire;
    private final StructuresClient structures;
    private final Courriels courriels;
    private final TemplateEngine moteur;
    private final String lienAudit;
    private final TransactionTemplate lecture;

    public AnnoncesDeLAudit(AuditRepository audits, AnnuaireClient annuaire, StructuresClient structures,
                            Courriels courriels, TemplateEngine moteur,
                            @Value("${qualisira.audit.lien-audit:}") String lienAudit,
                            PlatformTransactionManager transactions) {
        this.audits = audits;
        this.annuaire = annuaire;
        this.structures = structures;
        this.courriels = courriels;
        this.moteur = moteur;
        this.lienAudit = lienAudit;
        this.lecture = new TransactionTemplate(transactions);
        this.lecture.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.lecture.setReadOnly(true);
    }

    /** Qui sera prévenu de ce geste sur cet audit : l'écran le montre dans sa confirmation. */
    public List<Destinataire> destinataires(Audit audit, Geste geste) {
        List<Destinataire> liste = new ArrayList<>();
        if (geste == Geste.VALIDATION) {
            Stream.concat(Stream.ofNullable(audit.getResponsableEquipe()), audit.getMembresEquipe().stream())
                    .map(Auditeur::getUtilisateurId).distinct()
                    .map(this::personne).filter(p -> p != null)
                    .forEach(p -> liste.add(new Destinataire(p.nom(), p.email(), "Équipe d'audit")));
        }
        if (audit.getProcessusId() != null) {
            pilotes(audit.getProcessusId()).forEach(p -> liste.add(new Destinataire(p.nom(), p.email(), "Pilote du processus audité")));
        }
        // Une même personne, deux titres : un seul courriel.
        Map<String, Destinataire> uniques = new LinkedHashMap<>();
        liste.forEach(d -> uniques.putIfAbsent(d.email() == null ? d.nom() : d.email().toLowerCase(), d));
        return new ArrayList<>(uniques.values());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void apresLeGeste(Annonce annonce) {
        try {
            annoncer(annonce);
        } catch (Exception e) {
            log.error("L'annonce {} de l'audit {} n'a pas pu partir : {}", annonce.geste(), annonce.auditId(), e.getMessage());
        }
    }

    void annoncer(Annonce annonce) {
        Map<String, Object> commun = lecture.execute(s -> audits.findById(annonce.auditId()).map(audit -> {
            Map<String, Object> v = new HashMap<>();
            v.put("reference", audit.getReference());
            v.put("processus", libelleDuProcessus(audit.getProcessusId()));
            v.put("debut", audit.getDateDebutPrevue() == null ? null : audit.getDateDebutPrevue().format(JOUR));
            v.put("fin", audit.getDateFinPrevue() == null ? null : audit.getDateFinPrevue().format(JOUR));
            v.put("referencePlan", null);
            v.put("lien", lienAudit.isBlank() ? null : lienAudit.replace("{id}", audit.getId().toString()));
            v.put("destinataires", destinataires(audit, annonce.geste()));
            return v;
        }).orElse(null));
        if (commun == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Destinataire> destinataires = (List<Destinataire>) commun.remove("destinataires");
        String sujet = annonce.geste() == Geste.VALIDATION
                ? "Audit " + commun.get("reference") + " validé — " + commun.get("processus")
                : "Audit " + commun.get("reference") + " — plan d'audit partagé";
        for (Destinataire d : destinataires) {
            if (d.email() == null) {
                continue;
            }
            try {
                Map<String, Object> variables = new HashMap<>(commun);
                variables.put("nomComplet", d.nom());
                variables.put("titre", d.titre());
                variables.put("geste", annonce.geste().name());
                Context contexte = new Context();
                contexte.setVariables(variables);
                courriels.envoyer(d.email(), null, sujet, moteur.process(GABARIT, contexte), "annonce-audit");
            } catch (Exception e) {
                log.error("Annonce {} de {} à {} non déposée : {}", annonce.geste(), commun.get("reference"), d.email(), e.getMessage());
            }
        }
    }

    private record Personne(String nom, String email) {
    }

    private Personne personne(String utilisateurId) {
        if (utilisateurId == null) {
            return null;
        }
        try {
            Map<String, Object> u = annuaire.utilisateur(utilisateurId);
            return u == null ? null : new Personne(nom(u), texte(u.get("email")));
        } catch (Exception e) {
            log.warn("Annuaire injoignable pour l'utilisateur {} : {}", utilisateurId, e.getMessage());
            return null;
        }
    }

    private List<Personne> pilotes(UUID processusId) {
        try {
            Map<String, Object> reponse = annuaire.porteursDuRole(ROLE_PILOTE, processusId.toString());
            Object data = reponse == null ? null : reponse.get("data");
            if (!(data instanceof List<?> liste)) {
                return List.of();
            }
            List<Personne> pilotes = new ArrayList<>();
            for (Object element : liste) {
                if (element instanceof Map<?, ?> p) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> personne = (Map<String, Object>) p;
                    pilotes.add(new Personne(nom(personne), texte(personne.get("email"))));
                }
            }
            return pilotes;
        } catch (Exception e) {
            log.warn("Pilotes du processus {} illisibles : {}", processusId, e.getMessage());
            return List.of();
        }
    }

    private String libelleDuProcessus(UUID processusId) {
        if (processusId == null) {
            return "—";
        }
        try {
            var structure = structures.structure(processusId);
            if (structure != null) {
                String l = texte(structure.getLibelleLong());
                return l != null ? l : texte(structure.getLibelleCourt()) != null ? texte(structure.getLibelleCourt()) : processusId.toString();
            }
        } catch (Exception e) {
            log.warn("Libellé du processus {} illisible : {}", processusId, e.getMessage());
        }
        return processusId.toString();
    }

    private static String nom(Map<String, Object> u) {
        String complet = texte(u.get("nomComplet"));
        if (complet == null) {
            complet = texte(u.get("fullName"));
        }
        if (complet != null) {
            return complet;
        }
        String compose = ((texte(u.get("firstName")) == null ? "" : texte(u.get("firstName"))) + " "
                + (texte(u.get("lastName")) == null ? "" : texte(u.get("lastName")))).trim();
        return compose.isEmpty() ? "Madame, Monsieur" : compose;
    }

    private static String texte(Object valeur) {
        return valeur == null || valeur.toString().isBlank() ? null : valeur.toString().strip();
    }
}
