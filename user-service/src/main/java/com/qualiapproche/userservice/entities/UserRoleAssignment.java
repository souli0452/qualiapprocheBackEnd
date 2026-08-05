package com.qualiapproche.userservice.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
