package com.qualiapproche.userservice.repository;

import com.qualiapproche.userservice.entities.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {
    List<UserRoleAssignment> findByUserId(String userId);

    /**
     * Affectations d'un rôle, désigné par son identifiant.
     *
     * <p>Sert la résolution inverse — « qui porte ce rôle ? » — dont dépendent les services qui
     * notifient les responsables d'une étape.</p>
     */
    List<UserRoleAssignment> findByRole_Id(UUID roleId);

    /** Même résolution, pour les appelants qui ne connaissent le rôle que par son nom. */
    List<UserRoleAssignment> findByRole_NameIgnoreCase(String roleName);
}
