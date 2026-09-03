package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Fichier attaché à un dossier. Le contenu ne circule qu'au dépôt : les "
        + "lectures ne rendent que la description du fichier et l'adresse à laquelle le "
        + "télécharger.")
public class PieceJointeDTO extends AuditEntityDto {

    @Schema(description = "Nom d'origine du fichier, tel que déposé.", example = "Rapport_audit.pdf")
    private String nom;

    @Schema(description = "Extension, sans le point.", example = "pdf")
    private String ext;

    @Schema(description = "Type MIME du fichier.", example = "application/pdf")
    private String type;

    @Schema(description = "Référence de l'objet dans le serveur de fichiers. C'est elle qu'il faut "
            + "citer pour télécharger la pièce, et non le nom, qui peut être porté par plusieurs.",
            example = "non-conformite/GAI/5e0ca370-03a6-4465-a446-1d22ed758fe2.pdf")
    private String url;

    @Schema(description = "Identifiant du dossier auquel la pièce est rattachée : une "
            + "non-conformité ou un plan d'action.")
    private UUID entityId;

    @Schema(description = "Contenu du fichier, au dépôt seulement. Les lectures le laissent vide : "
            + "renvoyer chaque fichier dans la fiche aurait fait peser des mégaoctets sur chaque "
            + "consultation.")
    private byte[] fichier;

    @Schema(description = "Le fichier est une archive compressée.", example = "false")
    private boolean zipFile;
}
