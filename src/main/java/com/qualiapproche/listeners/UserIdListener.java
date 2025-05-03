package com.qualiapproche.listeners;

import com.qualiapproche.entities.AuditEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

public class UserIdListener {
    @PrePersist
    public void beforeCreate(final AuditEntity entity) {
        entity.setCreatedById(KeycloakUtils.getCurrentUserId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCurrentUserfullName(KeycloakUtils.getUserFullname());
        entity.setCurrentUserEmail(KeycloakUtils.getUserEmail());
    }

    @PreUpdate
    public void beforeUpdate(final AuditEntity entity) {
        // entity.setUpdateAt(LocalDateTime.now());
        // entity.setUpdateById(KeycloakUtils.getCurrentUserId());
        entity.setUpdateAt(LocalDateTime.now());
        entity.setUpdateById(KeycloakUtils.getCurrentUserId());
        entity.setCurrentUserfullName(KeycloakUtils.getUserFullname());
        entity.setCurrentUserEmail(KeycloakUtils.getUserEmail());
    }
}
