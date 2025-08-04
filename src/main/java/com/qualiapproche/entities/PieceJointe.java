package com.qualiapproche.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
public class PieceJointe extends AuditEntity {
    private String nom;

    private String ext;

    private String url;

    private String type;

    private Long entityId;

    private byte[] fichier;
    private boolean zipFile;

}
