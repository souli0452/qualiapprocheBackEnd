package com.qualiapproche.userservice.repository;

import com.qualiapproche.userservice.entities.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppRoleRepository extends JpaRepository<AppRole, UUID> {
    Optional<AppRole> findByName(String name);

    /**
     * Variante tolérante aux homonymes, pour la règle qui les interdit désormais.
     *
     * <p>{@link #findByName(String)} lève sur deux lignes de même nom : l'employer pour vérifier
     * l'unicité aurait rendu impossible toute création de rôle sur une base en portant déjà.</p>
     */
    List<AppRole> findAllByName(String name);

    /**
     * Rôles dont le nom égale {@code name} sans égard à la casse.
     *
     * <p>La création s'en sert pour refuser {@code pilote} à côté de {@code PILOTE} : plusieurs
     * contrôles comparent les noms de rôles sans tenir compte de la casse (la reconnaissance du
     * SUPER_ADMIN, {@code PermissionChecker}), d'autres à l'égalité exacte. Deux noms qui ne
     * diffèrent que par la casse seraient tantôt un seul rôle, tantôt deux.</p>
     */
    List<AppRole> findAllByNameIgnoreCase(String name);
}
