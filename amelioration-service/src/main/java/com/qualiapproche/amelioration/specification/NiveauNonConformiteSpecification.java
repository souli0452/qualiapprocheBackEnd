package com.qualiapproche.amelioration.specification;

import com.qualiapproche.amelioration.entities.NiveauNonConformite;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class NiveauNonConformiteSpecification {

    public static Specification<NiveauNonConformite> filter(String libelle, String description) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (libelle != null && !libelle.isBlank())
                predicates.add(cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%"));
            if (description != null && !description.isBlank())
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
