package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.storage.FichierStocke;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/** Une pièce justificative jointe à la notation d'un critère RQAP-BF ; le fichier vit sur le serveur de fichiers. */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "preuve_notation")
public class PreuveNotation extends AuditEntity implements FichierStocke {

    @Column(name = "notation_id", nullable = false)
    private UUID notationId;

    @Column(name = "nom")
    private String nom;

    @Column(name = "ext", length = 32)
    private String ext;

    @Column(name = "type", length = 150)
    private String type;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;
}
