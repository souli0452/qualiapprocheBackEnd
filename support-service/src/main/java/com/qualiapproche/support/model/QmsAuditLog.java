package com.qualiapproche.support.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "qms_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QmsAuditLog {

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

    private String details;
}
