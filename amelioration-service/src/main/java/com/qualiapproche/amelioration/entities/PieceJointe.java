package com.qualiapproche.amelioration.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "piece_jointe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PieceJointe extends AuditEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String nom;
    private String ext;
    private String type;

    @Column(name = "url")
    private String url;

    @Column(name = "entity_id")
    private UUID entityId;

    private boolean zipFile;
}