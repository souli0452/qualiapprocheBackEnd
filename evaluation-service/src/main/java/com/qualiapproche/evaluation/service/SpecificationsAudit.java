package com.qualiapproche.evaluation.service;

import com.qualiapproche.evaluation.dto.FiltreProgramme;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.NoeudReferentiel;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Le programme filtré, en critères JPA plutôt qu'en requête écrite.
 *
 * <p>Chaque filtre est facultatif. Écrits en JPQL, ils auraient demandé autant de
 * {@code :param IS NULL} — et un paramètre nul non typé finit en {@code bytea} côté PostgreSQL,
 * piège déjà payé sur la FAQ. Ici, un filtre absent n'est simplement pas posé.</p>
 */
public final class SpecificationsAudit {

    private SpecificationsAudit() {
    }

    public static Specification<Audit> du(UUID directionId, FiltreProgramme f, String moi, LocalDate aujourdHui) {
        return (racine, requete, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(racine.get("directionId"), directionId));
            if (f.getStatuts() != null && !f.getStatuts().isEmpty()) {
                conditions.add(racine.get("statut").in(f.getStatuts()));
            }
            if (f.getRecherche() != null && !f.getRecherche().isBlank()) {
                String motif = "%" + f.getRecherche().strip().toLowerCase() + "%";
                conditions.add(cb.or(
                        cb.like(cb.lower(racine.get("reference")), motif),
                        cb.like(cb.lower(cb.coalesce(racine.get("objectifsAudit"), "")), motif)));
            }
            if (f.getTypeAuditId() != null) {
                conditions.add(cb.equal(racine.get("type").get("id"), f.getTypeAuditId()));
            }
            if (f.getNiveauRisque() != null) {
                conditions.add(cb.equal(racine.get("niveauRisque"), f.getNiveauRisque()));
            }
            if (f.getResponsableId() != null) {
                conditions.add(cb.equal(racine.get("responsableEquipe").get("id"), f.getResponsableId()));
            }
            if (f.getAnnee() != null) {
                conditions.add(cb.between(racine.get("dateDebutPrevue"),
                        LocalDate.of(f.getAnnee(), 1, 1), LocalDate.of(f.getAnnee(), 12, 31)));
            }
            if (f.getDomaineId() != null) {
                Join<Audit, NoeudReferentiel> domaines = racine.join("domainesRqapbf", JoinType.INNER);
                conditions.add(cb.equal(domaines.get("id"), f.getDomaineId()));
                requete.distinct(true);
            }
            if (f.isMesAudits()) {
                Join<Audit, Auditeur> membres = racine.join("membresEquipe", JoinType.LEFT);
                Join<Audit, Auditeur> responsable = racine.join("responsableEquipe", JoinType.LEFT);
                conditions.add(cb.or(
                        cb.equal(responsable.get("utilisateurId"), moi),
                        cb.equal(membres.get("utilisateurId"), moi)));
                requete.distinct(true);
            }
            if (f.getEnRetard() != null) {
                Predicate retard = cb.or(
                        cb.and(racine.get("statut").in(StatutAudit.PLANIFIE, StatutAudit.EN_PREPARATION,
                                        StatutAudit.EN_RETARD),
                                cb.lessThan(racine.get("dateDebutPrevue"), aujourdHui)),
                        cb.and(cb.equal(racine.get("statut"), StatutAudit.EN_COURS),
                                cb.lessThan(racine.get("dateFinPrevue"), aujourdHui)));
                conditions.add(f.getEnRetard() ? retard : cb.not(retard));
            }
            return cb.and(conditions.toArray(new Predicate[0]));
        };
    }
}
