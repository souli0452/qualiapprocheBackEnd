package com.qualiapproche.amelioration.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.storage.FichierStocke;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
@Schema(description = "Représente un fichier ou une preuve documentaire stockée sur le serveur d'objets (MinIO/S3) et rattachée à un dossier.")
public class PieceJointe extends AuditEntity implements Serializable, FichierStocke {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Nom d'origine du fichier tel que déposé par l'utilisateur", example = "Rapport_Audit_2026.pdf")
    @Column(name = "nom", nullable = false)
    private String nom;

    @Schema(description = "Extension du fichier sans le point", example = "pdf")
    @Column(name = "ext", length = 20)
    private String ext;

    @Schema(description = "Type MIME officiel du fichier", example = "application/pdf")
    @Column(name = "type", length = 100)
    private String type;

    @Schema(
        description = "Chemin / clé de l'objet dans le bucket MinIO (ex: DOSSIER/STRUCTURE/nom_uuid.ext)",
        example = "non-conformite/GAI/5e0ca370-03a6-4465-a446-1d22ed758fe2.pdf"
    )
    @Column(name = "url", nullable = false)
    private String url;

    @Schema(description = "Identifiant de l'entité parente propriétaire de la pièce (ID de la Non-Conformité ou ID du Plan d'Action)")
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Schema(
        description = "Indique si la pièce a été déposée lors d'une transition de workflow "
            + "(ex: justificatif de rejet) pour ne pas être écrasée lors de l'édition classique de la fiche",
        example = "false"
    )
    @Column(name = "depose_par_circuit", columnDefinition = "boolean not null default false")
    private boolean deposeParCircuit;

    @Schema(description = "Indique si le fichier est une archive compressée ZIP", example = "false")
    @Column(name = "zip_file")
    private boolean zipFile;

    /**
     * (Optionnel mais très recommandé)
     * Taille du fichier en octets. Permet à l'interface et à l'IA d'afficher le poids
     * sans avoir à interroger MinIO à chaque consultation de la fiche.
     */
    @Schema(description = "Taille du fichier en octets", example = "570368")
    @Column(name = "taille")
    private Long taille;
}

