package com.qualiapproche.amelioration.specification;

import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.common.utils.StatutEnum;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlanActionSpecification {

    public static Specification<PlanAction> filter(
            String numeroOdre,
            String responsableEmail,
            String responsableNomComplet,
            String numeroNc,
            StatutEnum status,
            UUID nonConformeId,
            LocalDate dateEcheanceFrom,
            LocalDate dateEcheanceTo,
            LocalDate dateTraitementFrom,
            LocalDate dateTraitementTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (numeroOdre != null && !numeroOdre.isBlank())
                predicates.add(cb.like(cb.lower(root.get("numeroOdre")), "%" + numeroOdre.toLowerCase() + "%"));
            if (responsableEmail != null && !responsableEmail.isBlank())
                predicates.add(cb.like(cb.lower(root.get("responsableEmail")), "%" + responsableEmail.toLowerCase() + "%"));
            if (responsableNomComplet != null && !responsableNomComplet.isBlank())
                predicates.add(cb.like(cb.lower(root.get("responsableNomComplet")), "%" + responsableNomComplet.toLowerCase() + "%"));
            if (numeroNc != null && !numeroNc.isBlank())
                predicates.add(cb.like(cb.lower(root.get("numeroNc")), "%" + numeroNc.toLowerCase() + "%"));
            if (status != null)
                predicates.add(cb.equal(root.get("status"), status));
            if (nonConformeId != null)
                predicates.add(cb.equal(root.get("nonConformeId"), nonConformeId));
            if (dateEcheanceFrom != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateEcheance"), dateEcheanceFrom));
            if (dateEcheanceTo != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("dateEcheance"), dateEcheanceTo));
            if (dateTraitementFrom != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateTraitement"), dateTraitementFrom));
            if (dateTraitementTo != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("dateTraitement"), dateTraitementTo));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
