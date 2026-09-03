package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ce que toute ressource porte : son identité et la trace de qui l'a créée puis modifiée.
 *
 * <p>Ces champs sont renseignés par le serveur et non par l'appelant. Les décrire ici les décrit
 * dans les vingt-neuf ressources qui en héritent : les documenter ressource par ressource aurait
 * demandé la même phrase vingt-neuf fois, et laissé les vingt-neuf diverger.</p>
 */
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
@Schema(description = "Identité de la ressource et trace de sa création puis de sa modification. "
        + "Ces champs sont renseignés par le serveur : les envoyer n'a aucun effet.")
public class AuditEntityDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Identifiant de la ressource. Absent à la création, où le serveur "
            + "l'attribue.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private UUID id;

    @Schema(description = "Date et heure de création, posées par le serveur.",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Date et heure de la dernière modification, posées par le serveur.",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updateAt;

    @Schema(description = "Identifiant de l'auteur de la création, lu dans son jeton.",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String createdById;

    @Schema(description = "Identifiant de l'auteur de la dernière modification, lu dans son jeton.",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String updateById;

    @Schema(description = "Nom complet de l'auteur au moment du geste. Conservé tel quel : un "
            + "agent qui change de nom ne réécrit pas l'histoire des dossiers.",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "Awa Traoré")
    private String currentUserfullName;

    @Schema(description = "Adresse de l'auteur au moment du geste.",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "a.traore@exemple.bf")
    private String currentUserEmail;

    @Schema(description = "Structure de l'auteur au moment du geste. Conservée telle quelle : un "
            + "agent qui change de service n'emporte pas avec lui les dossiers qu'il a déclarés.",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "Direction des achats")
    private String currentUserStructure;
}
