package com.qualiapproche.listeners;

import com.qualiapproche.entities.AuditEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class UserIdListener {
    @PrePersist
    public void beforeCreate(final AuditEntity entity) {
        ZoneId burkinaZone = ZoneId.of("Africa/Ouagadougou");
        entity.setCreatedById(KeycloakUtils.getCurrentUserId());
        entity.setCreatedAt(LocalDateTime.now(burkinaZone));
        entity.setCurrentUserfullName(KeycloakUtils.getUserFullname());
        entity.setCurrentUserEmail(KeycloakUtils.getUserEmail());
        entity.setCurrentUserStructure(KeycloakUtils.getUserStructure());
    }

    @PreUpdate
    public void beforeUpdate(final AuditEntity entity) {
        ZoneId burkinaZone = ZoneId.of("Africa/Ouagadougou");
         entity.setUpdateAt(LocalDateTime.now(burkinaZone));
         entity.setUpdateById(KeycloakUtils.getCurrentUserId());
        entity.setUpdateAt(LocalDateTime.now(burkinaZone));
        entity.setUpdateById(KeycloakUtils.getCurrentUserId());
        entity.setCurrentUserfullName(KeycloakUtils.getUserFullname());
        entity.setCurrentUserEmail(KeycloakUtils.getUserEmail());
        entity.setCurrentUserStructure(KeycloakUtils.getUserStructure());
    }
}
