package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Où en est la licence de cette installation — ce que l'écran d'accueil a besoin de savoir.
 *
 * <p>Trois situations, et trois conduites différentes :</p>
 * <ul>
 *   <li>{@code ABSENTE} — rien n'a jamais été installé : on demande une licence, ou on propose
 *       l'essai ;</li>
 *   <li>{@code ACTIVE} — tout est ouvert ; un compte à rebours s'affiche à l'approche du
 *       terme ;</li>
 *   <li>{@code EXPIREE} — les données restent consultables, les actions sont suspendues. Couper
 *       l'accès aux données qualité d'un client transformerait un retard de paiement en litige,
 *       et le pousserait à chercher comment contourner.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "État de la licence de cette installation. Trois situations appellent trois "
        + "conduites : absente, on demande une licence ou on propose l'essai ; active, tout est "
        + "ouvert ; expirée, les données restent consultables et les actions sont suspendues.")
public class EtatLicenceDto {

    /** {@code ABSENTE}, {@code ACTIVE} ou {@code EXPIREE}. */
    @Schema(description = "Situation de la licence.", example = "ACTIVE",
            allowableValues = {"ABSENTE", "ACTIVE", "EXPIREE"})
    private String statut;

    /**
     * Les actions d'écriture sont-elles permises ?
     *
     * <p>Faux hors licence valide. La consultation, elle, ne se ferme jamais.</p>
     */
    @Schema(description = "Les écritures sont-elles permises. Faux hors licence valide ; la "
            + "consultation, elle, ne se ferme jamais.", example = "true")
    private boolean actionsOuvertes;

    /** {@code COMMERCIALE} ou {@code ESSAI}. */
    @Schema(description = "Nature de la licence installée.", example = "COMMERCIALE",
            allowableValues = {"COMMERCIALE", "ESSAI"})
    private String type;

    @Schema(description = "Référence de la licence, telle que l'éditeur l'a émise. C'est elle "
            + "qu'il faut citer pour toute demande de renouvellement.",
            example = "QS-2026-000148")
    private String reference;

    @Schema(description = "Nom du partenaire chez qui cette installation tourne. Il est confronté "
            + "au code inscrit dans la licence : une licence émise pour un autre est refusée.",
            example = "Direction Qualité Approche")
    private String partenaireNom;

    @Schema(description = "Premier jour de validité.", example = "2026-01-01")
    private LocalDate debut;

    @Schema(description = "Dernier jour de validité.", example = "2026-12-31")
    private LocalDate fin;

    /** Négatif une fois le terme passé : « expirée depuis 12 jours » se lit directement. */
    @Schema(description = "Jours restants avant le terme. La valeur devient négative une fois le "
            + "terme passé, ce qui se lit directement comme « expirée depuis douze jours ».",
            example = "42")
    private long joursRestants;

    @Schema(description = "Modules que la licence ouvre. Un module absent de cette liste reste "
            + "fermé, même installé.",
            example = "[\"AMELIORATION\", \"DOCUMENTAIRE\"]")
    private List<String> modules;

    /** {@code 0} vaut « sans limite ». */
    @Schema(description = "Nombre maximal d'utilisateurs. Zéro vaut « sans limite », et non "
            + "« aucun ».", example = "50")
    private int utilisateursMax;

    /** Phrase à afficher telle quelle — c'est elle qui dit quoi faire. */
    @Schema(description = "Phrase à afficher telle quelle : c'est elle qui dit quoi faire, et non "
            + "le seul statut.",
            example = "Votre licence expire le 31/12/2026. Rapprochez-vous de votre éditeur.")
    private String message;
}
