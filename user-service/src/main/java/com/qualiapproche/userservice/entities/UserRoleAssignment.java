package com.qualiapproche.userservice.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_role_assignments")
public class UserRoleAssignment extends AuditEntity {

    @Column(nullable = false)
    private String userId; // ID Keycloak de l'utilisateur

    @ManyToOne
    @JoinColumn(name = "role_id")
    private AppRole role;
}
