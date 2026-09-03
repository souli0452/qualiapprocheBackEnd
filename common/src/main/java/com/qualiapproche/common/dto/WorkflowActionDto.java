package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Une décision que l'appelant peut prendre sur le dossier, telle que le "
        + "circuit la déclare : ce qu'elle fait, comment la présenter, et sous quel code la "
        + "rejouer. Sa seule présence vaut autorisation — le moteur ne publie que les actions "
        + "qu'il laisserait passer.")
public class WorkflowActionDto {
    /** Identifiant de la transition à jouer — c'est lui que reprend {@code /execute}. */
    @Schema(description = "Ce qu'il faut renvoyer pour jouer l'action. Identifiant technique de la "
            + "transition, propre à l'installation : à transmettre tel quel, jamais à reconnaître.",
            example = "137")
    private String code;
    /**
     * Code métier de l'action au sein de son étape ({@code APPROUVE}, {@code DEMANDER_COMPLEMENT}…),
     * stable d'une installation à l'autre là où {@link #code} est un identifiant technique. C'est
     * par lui qu'un champ se rattache à une action précise.
     */
    @Schema(description = "Repère stable de l'action au sein de son étape, identique d'une "
            + "installation à l'autre là où le code d'exécution ne l'est pas. C'est par lui qu'un "
            + "champ de saisie se rattache à une action précise, et le seul sur lequel un "
            + "traitement particulier puisse se brancher.",
            example = "DEMANDER_COMPLEMENT")
    private String actionCode;

    @Schema(description = "Ce qui s'écrit sur le bouton, dans les termes que l'auteur du circuit a "
            + "choisis. Il se reformule sans prévenir : rien ne doit en dépendre.",
            example = "Transmettre pour approbation")
    private String libelle;
    /**
     * Icône du bouton, en classe PrimeIcons ({@code "pi pi-check"}), telle que le circuit la
     * déclare. Absente de ce DTO, elle était publiée par le moteur et perdue avant l'écran, qui
     * retombait sur une présentation uniforme.
     */
    @Schema(description = "Classe PrimeIcons à poser sur le bouton, utilisable telle quelle. Le "
            + "circuit la déclare ; à défaut, la nature de la décision en fournit une, si bien que "
            + "ce champ est rarement vide.",
            example = "pi pi-arrow-right")
    private String icon;
    /**
     * Couleur du bouton dans le vocabulaire PrimeNG ({@code success}, {@code warn}, {@code danger}…),
     * utilisable telle quelle. Même remarque que pour l'icône.
     */
    @Schema(description = "Jeton de couleur PrimeNG, à passer tel quel à « [severity] ». Comme "
            + "l'icône, il vient du circuit et, à défaut, de la nature de la décision.",
            example = "info",
            allowableValues = {"primary", "secondary", "success", "info", "warn", "danger", "help",
                    "contrast"})
    private String severity;

    @Schema(description = "Habilitation que la transition exige — un rôle, « @TITULAIRE » ou "
            + "« @CREATEUR ». Donnée pour information seulement : le moteur l'a déjà vérifiée avant "
            + "de publier l'action, et la contrôler une seconde fois côté appelant ferait "
            + "disparaître des boutons légitimes.",
            example = "RESPONSABLE_QUALITE")
    private String permission;
    /**
     * Décision portée par la transition ({@code APPROUVE} / {@code REJETE}). Permet à l'appelant
     * de distinguer une action d'approbation d'un rejet — pour la présentation comme pour choisir
     * entre les points d'entrée {@code /validate} et {@code /reject}.
     */
    @Schema(description = "Nature de l'action : elle fait avancer le dossier, le renvoie en "
            + "arrière, ou le mène à sa clôture. Une étape peut offrir plusieurs actions de même "
            + "nature — valider, ou valider en demandant un complément — que seul le code d'action "
            + "distingue.",
            example = "APPROUVE",
            allowableValues = {"APPROUVE", "REJETE", "CLOTURE"})
    private String decision;
}
