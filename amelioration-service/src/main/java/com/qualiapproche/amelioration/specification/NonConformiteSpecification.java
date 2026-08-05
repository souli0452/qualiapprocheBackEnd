package com.qualiapproche.amelioration.specification;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.enumeration.TypeDemande;
import com.qualiapproche.common.enumeration.Circuit;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NonConformiteSpecification {

    public static Specification<NonConformite> filter(
            String numeroReference,
            String nomProcessus,
            String origineId,
            String origineService,
            String structureSoumissionId,
            String structureResponsableId,
            Etat etatTraitement,
            Status status,
            TypeDemande typeDemande,
            Circuit circuit,
            String userImputeEmail,
            String typeNonConformiteLibelle,
            String niveauNonConformiteLibelle,
            UUID typeNonConformiteId,
            UUID niveauNonConformiteId,
            LocalDateTime publicationDateFrom,
            LocalDateTime publicationDateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (numeroReference != null && !numeroReference.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("numeroReference")), "%" + numeroReference.toLowerCase() + "%"));
            }
            if (nomProcessus != null && !nomProcessus.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nomProcessus")), "%" + nomProcessus.toLowerCase() + "%"));
            }
            if (origineId != null && !origineId.isBlank()) {
                predicates.add(cb.equal(root.get("origineId"), origineId));
            }
            if (origineService != null && !origineService.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("origineService")), "%" + origineService.toLowerCase() + "%"));
            }
            if (structureSoumissionId != null && !structureSoumissionId.isBlank()) {
                predicates.add(cb.equal(root.get("structureSoumissionId"), structureSoumissionId));
            }
            if (structureResponsableId != null && !structureResponsableId.isBlank()) {
                predicates.add(cb.equal(root.get("structureResponsableId"), structureResponsableId));
            }
            if (etatTraitement != null) {
                predicates.add(cb.equal(root.get("etatTraitement"), etatTraitement));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (typeDemande != null) {
                predicates.add(cb.equal(root.get("typeDemande"), typeDemande));
            }
            if (circuit != null) {
                predicates.add(cb.equal(root.get("circuit"), circuit));
            }
            if (userImputeEmail != null && !userImputeEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("userImputeEmail")), "%" + userImputeEmail.toLowerCase() + "%"));
            }
            if (typeNonConformiteLibelle != null && !typeNonConformiteLibelle.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("typeNonConformiteLibelle")), "%" + typeNonConformiteLibelle.toLowerCase() + "%"));
            }
            if (niveauNonConformiteLibelle != null && !niveauNonConformiteLibelle.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("niveauNonConformiteLibelle")), "%" + niveauNonConformiteLibelle.toLowerCase() + "%"));
            }
            if (typeNonConformiteId != null) {
                predicates.add(cb.equal(root.get("typeNonConformiteId"), typeNonConformiteId));
            }
            if (niveauNonConformiteId != null) {
                predicates.add(cb.equal(root.get("niveauNonConformiteId"), niveauNonConformiteId));
            }
            if (publicationDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publicationDate"), publicationDateFrom));
            }
            if (publicationDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publicationDate"), publicationDateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
