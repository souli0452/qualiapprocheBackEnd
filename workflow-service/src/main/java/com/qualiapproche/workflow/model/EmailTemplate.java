package com.qualiapproche.workflow.model;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
// Unique par client (V14) : chaque direction reçoit les gabarits livrés sous les mêmes codes.
@Table(name = "workflow_email_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_email_template_direction_code",
                columnNames = {"direction_id", "code"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate extends AuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

}
