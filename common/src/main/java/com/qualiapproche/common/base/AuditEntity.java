package com.qualiapproche.common.base;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;
import com.qualiapproche.common.utils.SecurityUtils;

@Data
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy ' à ' HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "update_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy ' à ' HH:mm:ss")
    private LocalDateTime updateAt;

    @Column(name = "created_by_id")
    private String createdById;

    @Column(name = "update_by_id")
    private String updateById;

    @Column(name = "current_user_full_name")
    private String currentUserfullName;

    @Column(name = "current_user_email")
    private String currentUserEmail;

    @Column(name = "current_user_structure")
    private String currentUserStructure;

    @Column(name = "direction_id")
    private UUID directionId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        this.createdById = SecurityUtils.getCurrentUserId();
        this.currentUserfullName = SecurityUtils.getCurrentUserFullName();
        this.currentUserEmail = SecurityUtils.getCurrentUserEmail();
        this.directionId = SecurityUtils.getCurrentDirectionId();
    }

    @PreUpdate
    protected void onUpdate() {
        updateAt = LocalDateTime.now();
        this.updateById = SecurityUtils.getCurrentUserId();
    }
}
