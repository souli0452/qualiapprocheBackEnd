package com.qualiapproche.common.spec;

import com.qualiapproche.common.base.AuditEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Les filtres élémentaires d'une liste paginée, à composer par {@link #tous} ; pour les
 * recherches qu'un écran compose lui-même, voir {@link GenericSpecification}.
 *
 * <p>Un filtre absent rend {@code null}, et {@link #tous} le laisse de côté : on n'écrit pas de
 * {@code :param IS NULL}, qui finit en {@code bytea} côté PostgreSQL.</p>
 */
public final class Filtres {

    private Filtres() {
    }

    /** Tous les filtres posés à la fois ; les absents ({@code null}) ne comptent pas. */
    @SafeVarargs
    public static <E> Specification<E> tous(Specification<E>... filtres) {
        List<Specification<E>> poses = Arrays.stream(filtres).filter(Objects::nonNull).toList();
        return (racine, requete, cb) -> cb.and(poses.stream()
                .map(f -> f.toPredicate(racine, requete, cb))
                .toArray(jakarta.persistence.criteria.Predicate[]::new));
    }

    public static <E extends AuditEntity> Specification<E> deLaDirection(UUID directionId) {
        return (racine, requete, cb) -> cb.equal(racine.get("directionId"), directionId);
    }

    /** L'un des champs contient le texte, sans égard à la casse ; aucun filtre sans texte. */
    public static <E> Specification<E> contient(List<String> champs, String texte) {
        if (texte == null || texte.isBlank() || champs.isEmpty()) {
            return null;
        }
        String motif = "%" + texte.strip().toLowerCase() + "%";
        return (racine, requete, cb) -> cb.or(champs.stream()
                .map(champ -> cb.like(cb.lower(cb.coalesce(racine.<String>get(champ), "")), motif))
                .toArray(jakarta.persistence.criteria.Predicate[]::new));
    }

    public static <E> Specification<E> egal(String champ, Object valeur) {
        return valeur == null ? null : (racine, requete, cb) -> cb.equal(racine.get(champ), valeur);
    }

    /** Le champ prend l'une des valeurs ; une liste vide ne laisse rien passer. */
    public static <E> Specification<E> parmi(String champ, Collection<?> valeurs) {
        if (valeurs == null) {
            return null;
        }
        return valeurs.isEmpty() ? (racine, requete, cb) -> cb.disjunction()
                : (racine, requete, cb) -> racine.get(champ).in(valeurs);
    }

    /** La page demandée, dans l'ordre par défaut quand la requête n'en dit aucun. */
    public static Pageable ordonnee(Pageable demande, Sort parDefaut) {
        return demande.isUnpaged() || demande.getSort().isSorted() ? demande
                : PageRequest.of(demande.getPageNumber(), demande.getPageSize(), parDefaut);
    }
}
