package com.qualiapproche.amelioration.specification;

import com.qualiapproche.amelioration.entities.NonConformite;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Ce qui, dans la recherche des non-conformités, ne peut pas être générique.
 *
 * <p>Les critères de l'appelant — période, processus, gravité, origine, étape, référence — sont
 * traduits par {@code GenericSpecification} : l'écran nomme la colonne et la comparaison, et rien
 * n'est à prévoir ici. Cette classe n'en garde donc qu'une clause, celle qui n'appartient
 * précisément pas à l'appelant.</p>
 */
public final class NonConformiteSpecification {

    private NonConformiteSpecification() {
    }

    /**
     * Non-conformités qu'un utilisateur a le droit de voir : celles de sa structure — émises par
     * elle ou qui lui sont adressées — et les siennes, déclarées ou imputées.
     *
     * <p>Reprend mot pour mot la règle de {@code NonConformiteRepository.findVisiblesPar}. Elle doit
     * exister ici aussi, et non seulement dans la liste générale : une recherche sans borne de
     * visibilité rendrait par ses filtres ce que la consultation refuse de montrer, et il suffirait
     * de chercher pour lire les dossiers de toutes les structures.</p>
     *
     * <p>Combinée <b>en dehors</b> des critères reçus, par un ET : aucun filtre envoyé ne peut donc
     * l'élargir.</p>
     */
    public static Specification<NonConformite> visiblesPar(String structureId, String userId) {
        return (root, query, cb) -> {
            List<Predicate> ou = new ArrayList<>();
            if (structureId != null && !structureId.isBlank()) {
                ou.add(cb.equal(root.get("structureSoumissionId"), structureId));
                ou.add(cb.equal(root.get("origineId"), structureId));
            }
            if (userId != null && !userId.isBlank()) {
                ou.add(cb.equal(root.get("createdById"), userId));
                ou.add(cb.equal(root.get("userImputId"), userId));
            }
            // Appelant sans structure ni identifiant : rien ne lui est rattaché, il ne voit rien.
            // Ouvrir la liste entière serait le contraire de ce que cette borne garantit.
            return ou.isEmpty() ? cb.disjunction() : cb.or(ou.toArray(new Predicate[0]));
        };
    }
}
