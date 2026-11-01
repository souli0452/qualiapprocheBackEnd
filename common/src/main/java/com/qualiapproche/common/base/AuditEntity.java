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
import org.hibernate.annotations.TenantId;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.common.cloisonnement.Cloisonnable;

@Data
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntity implements Cloisonnable {
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

    /**
     * Le client à qui la ligne appartient : la direction racine de son auteur.
     *
     * <p>Posée par Hibernate à l'insertion, d'après la direction de la session
     * ({@code Cloisonnement}), et jamais modifiée ensuite ; toute lecture en est filtrée. Une
     * valeur posée à la main n'est admise que si elle est celle de la session — sauf pour le
     * système, qui écrit pour le client qu'il nomme.</p>
     */
    @TenantId
    @Column(name = "direction_id")
    private UUID directionId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        this.createdById = SecurityUtils.getAuteurId();
        this.currentUserfullName = SecurityUtils.getAuteurNom();
        this.currentUserEmail = SecurityUtils.getCurrentUserEmail();
    }

    @PreUpdate
    protected void onUpdate() {
        updateAt = LocalDateTime.now();
        this.updateById = SecurityUtils.getAuteurId();
    }
}
