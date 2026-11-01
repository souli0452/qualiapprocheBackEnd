package com.qualiapproche.support.model;

import com.qualiapproche.common.cloisonnement.Cloisonnable;
import org.hibernate.annotations.TenantId;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "qms_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QmsAuditLog implements Cloisonnable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String action; // CONSULTATION, TELECHARGEMENT, MODIFICATION, CREATION, LINK_NC

    private String documentNumber;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * Le client à qui la ligne appartient, posé par Hibernate d'après la session
     * ({@code Cloisonnement}). Cloisonnée en propre et non par son document : elle est aussi
     * cherchée sans lui ({@code par numéro de document, qui se répète d’un client à l’autre}).
     */
    @TenantId
    @Column(name = "direction_id")
    private UUID directionId;
}
