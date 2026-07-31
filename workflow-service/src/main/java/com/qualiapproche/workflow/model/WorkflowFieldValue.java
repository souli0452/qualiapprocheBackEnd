package com.qualiapproche.workflow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflow_field_value")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id", nullable = false)
    private ValidationHistory history;

    @Column(name = "field_code", nullable = false)
    private String fieldCode;

    @Column(name = "field_name")
    private String fieldName;

    @Column(columnDefinition = "TEXT")
    private String value; // The string representation of the filled value
}
