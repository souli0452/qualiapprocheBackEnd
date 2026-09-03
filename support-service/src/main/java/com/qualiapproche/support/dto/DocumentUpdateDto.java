package com.qualiapproche.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Métadonnées modifiables d'un document.
 *
 * <p>Ne reprend délibérément ni le numéro de document, ni les compteurs de version, ni les
 * indicateurs pilotés par le circuit de validation ({@code esTraiter}, {@code obsolete},
 * {@code currentEtape}) : ceux-là relèvent du versionnage et du workflow, pas d'une saisie
 * libre. Un champ laissé à {@code null} n'est pas modifié.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Métadonnées d'un document ouvertes à la retouche. Un champ absent ou nul "
        + "n'est pas modifié, ce qui permet d'envoyer une correction seule. N'y figurent ni le "
        + "numéro, ni le rang de version, ni les indicateurs que pilote le circuit : ceux-là ne se "
        + "corrigent pas à la main.")
public class DocumentUpdateDto {

    @Schema(description = "Nouvel intitulé. Il ne change ni le numéro du document, ni les "
            + "références déjà faites à lui.")
    private String titre;

    @Schema(description = "Code du document dans la convention de l'organisation, saisi par "
            + "l'auteur.",
            example = "QSE/PR/012")
    private String reference;

    @Schema(description = "Nouvel objet du document.")
    private String description;

    @Schema(description = "Structure propriétaire. La déplacer change qui voit le document et qui "
            + "décide de ses étapes : ce n'est pas une retouche d'affichage.")
    private String serviceId;

    @Schema(description = "Libellé de cette structure. À reprendre avec l'identifiant, sans quoi "
            + "l'affichage nommera l'ancienne.")
    private String serviceLibelle;

    @Schema(description = "Sigle de cette structure, à reprendre pour la même raison.",
            example = "DSI")
    private String serviceSigle;

    @Schema(description = "Rédacteur déclaré. Renseignement d'affichage : le changer ne transfère "
            + "aucun droit.")
    private String redacteur;

    @Schema(description = "Intervalle entre deux revues, en mois. La prochaine échéance en est "
            + "déduite ; la porter à nul soustrait le document à toute révision périodique.",
            example = "24")
    private Integer periodiciteMois;

    @Schema(description = "Marque de classement. Elle suit normalement le niveau de "
            + "confidentialité, qui reste ce qui commande l'accès.")
    private Boolean confidentiel;

    @Schema(description = "Le document vient de l'extérieur. C'est ce qui donne leur sens à la "
            + "référence officielle et au statut légal ci-dessous.")
    private Boolean documentExterne;

    @Schema(description = "Structure destinataire, par son identifiant.")
    private String processusDestId;

    @Schema(description = "Libellé de cette structure destinataire, à reprendre avec "
            + "l'identifiant.")
    private String processusDestLibelle;

    @Schema(description = "Référence portée par le texte d'origine, pour un document externe.",
            example = "ISO 9001:2015")
    private String referenceOfficielle;

    @Schema(description = "Libellé du domaine d'application. La retouche porte sur le libellé "
            + "affiché, non sur le domaine choisi au référentiel.",
            example = "Ressources humaines")
    private String domaine;

    @Schema(description = "Portée juridique d'un document externe.", example = "OBLIGATOIRE")
    private String statutLegal;

    /** Motif de la modification, journalisé dans la piste d'audit du document. */
    @Schema(description = "Raison de la retouche, inscrite dans la piste d'audit du document. "
            + "C'est la seule trace de ce qui a motivé le changement : les valeurs, elles, sont "
            + "écrasées.",
            example = "Correction du sigle après la réorganisation")
    private String motif;
}
