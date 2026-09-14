package com.qualiapproche.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Où en est un dossier dans son circuit, et ce que l'appelant peut y faire à "
        + "cet instant. Ce n'est pas une description du circuit mais une lecture faite pour une "
        + "personne : les actions offertes dépendent de qui demande.")
public class WorkflowStateDto {

    @Schema(description = "Instance de circuit ouverte sur le dossier. Un dossier peut en avoir "
            + "connu plusieurs ; c'est la dernière qui est rendue.")
    private UUID instanceId;

    /**
     * Circuit que suit le dossier.
     *
     * <p>Sans lui, un écran qui reçoit l'état d'un dossier ne peut rien dire de ce qui l'attend : il
     * connaît l'étape courante, mais pas le circuit dont elle fait partie, donc ni les étapes
     * suivantes ni le chemin restant. Le module documentaire s'en sortait en gardant une colonne à
     * lui ; les non-conformités et les demandes n'en ont pas.</p>
     */
    @Schema(description = "Circuit que suit le dossier. Il permet à l'écran de montrer le chemin "
            + "restant et non seulement l'étape où le dossier se trouve.")
    private UUID workflowId;

    @Schema(description = "Le circuit est-il encore en cours, ou a-t-il rendu son issue ? Ne dit "
            + "pas laquelle : un dossier terminé peut l'avoir été par approbation, rejet ou "
            + "clôture, ce que seul l'historique rapporte.",
            example = "EN_COURS",
            allowableValues = {"EN_COURS", "TERMINE"})
    private String status;

    @Schema(description = "Étape où le dossier est arrêté, par son code.",
            example = "VALIDATION_PILOTE")
    private String currentStateCode;

    @Schema(description = "Nom de cette étape, tel que l'éditeur de circuits le donne.",
            example = "Validation par le pilote")
    private String currentStateName;

    /**
     * Habilitation attendue à l'étape courante — un rôle, ou {@code @TITULAIRE} quand l'étape est
     * réservée à la personne désignée sur le dossier.
     *
     * <p>Sert à dire qui l'on attend lorsque l'appelant, lui, n'a rien à décider : sans cela un
     * écran sans action ne peut que se taire, et l'utilisateur voit un dossier immobile sans savoir
     * s'il doit agir, attendre, ou relancer quelqu'un.</p>
     */
    @Schema(description = "Habilitation attendue à l'étape : un rôle, ou @TITULAIRE lorsque "
            + "l'étape est réservée à la personne désignée sur le dossier. Sert à dire qui l'on "
            + "attend quand l'appelant, lui, n'a rien à décider.",
            example = "PILOTE")
    private String currentStepRole;

    /**
     * L'appelant est écarté de l'étape courante parce qu'il a lui-même soumis le dossier.
     *
     * <p>Sans ce drapeau, l'écran répondait « le pilote du processus doit se prononcer » — à un
     * pilote, sur son propre document. La phrase était exacte et incompréhensible : c'est bien un
     * pilote qu'on attend, mais pas celui-là. {@link #currentStepRole} ne pouvait pas le dire, la
     * question n'étant pas celle du rôle.</p>
     *
     * <p>Ne vaut que lorsque {@link #allowedActions} est vide : l'administration passe outre la
     * séparation des signatures, et l'écran lui propose alors les actions comme à n'importe qui.</p>
     */
    @Schema(description = "L'appelant est écarté de l'étape non pas faute de rôle, mais parce "
            + "qu'il a lui-même soumis le dossier. À lire seulement quand aucune action n'est "
            + "offerte : l'administration passe outre la séparation des signatures.",
            example = "true")
    private boolean ecarteCommeAuteur;

    @Schema(description = "Décisions que l'appelant peut prendre ici et maintenant, une fois "
            + "retirées celles que son habilitation ou l'état du dossier lui refuse. Vide ne veut "
            + "pas dire que le dossier est arrêté, mais qu'un autre est attendu.")
    @Builder.Default
    private List<WorkflowActionDto> allowedActions = new ArrayList<>();

    /**
     * Champs à saisir pour décider à l'étape courante. Sans eux, l'appelant n'avait aucun moyen
     * de construire le formulaire de validation ni de renseigner
     * {@code WorkflowValidationRequestDto.fields}, qui est indexé par identifiant de champ.
     */
    @Schema(description = "Champs que l'étape demande pour décider. Ils portent les identifiants "
            + "qu'attend la requête de validation, indexée par identifiant de champ ; tous ne "
            + "valent pas pour toutes les actions.")
    @Builder.Default
    private List<WorkflowStepFieldDto> currentStepFields = new ArrayList<>();

    /**
     * Décisions que l'étape prévoit mais que le dossier n'admet pas encore, faute d'un fait établi.
     *
     * <p>Le moteur les retire simplement des actions offertes : la clôture d'une non-conformité
     * dont les actions correctives ne sont pas soldées n'apparaît nulle part, et le responsable
     * qualité voit un dossier arrêté sans que rien ne lui dise ce qu'il attend. Ce n'est pas une
     * question d'habilitation — c'est le dossier qui n'est pas prêt — et cela concerne donc tout
     * le monde, pas seulement celui qui décidera.</p>
     */
    @Schema(description = "Décisions que l'étape prévoit mais qu'une condition non remplie retient. "
            + "Elles sont rendues à tous, sans égard au rôle : la raison de l'attente intéresse "
            + "aussi celui qui doit agir ailleurs pour la lever.")
    @Builder.Default
    private List<DecisionEnAttenteDto> pendingDecisions = new ArrayList<>();

    /**
     * Tout ce qui a été saisi sur ce dossier depuis l'ouverture de son circuit, dans l'ordre où il
     * l'a recueilli — la valeur la plus récente d'un champ faisant foi.
     *
     * <p>C'est ce qui permet à une donnée demandée à une étape de rester attachée au dossier
     * jusqu'à la fin de son traitement, et d'être lue partout où l'état du circuit accompagne la
     * ressource : sa fiche comme les lignes de liste. Aucun module métier n'a à prévoir de colonne
     * pour un champ que l'éditeur de circuits ajoutera demain.</p>
     *
     * <p>L'historique conserve les valeurs successives ; cette liste-ci n'en montre que la
     * dernière, celle qui vaut aujourd'hui pour le dossier.</p>
     */
    @Schema(description = "Ce qui a été saisi sur le dossier depuis l'ouverture de son circuit, "
            + "une entrée par champ : la valeur la plus récente fait foi. Pour lire les valeurs "
            + "successives d'un même champ, il faut l'historique.")
    @Builder.Default
    private List<SaisieDto> saisies = new ArrayList<>();

    /** Une décision prévue par l'étape, et la condition qui lui manque. */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @Schema(description = "Une décision que l'étape prévoit, et le fait qui lui manque pour "
            + "devenir franchissable.")
    public static class DecisionEnAttenteDto {

        /** Libellé du bouton tel qu'il apparaîtra une fois la condition remplie. */
        @Schema(description = "Libellé du bouton tel qu'il apparaîtra une fois la condition "
                + "remplie. Ne pas l'offrir dès maintenant : la décision serait refusée.",
                example = "Clôturer la non-conformité")
        private String libelle;

        /** Nom du fait exigé, tel que le module métier le déclare. */
        @Schema(description = "Nom du fait exigé, tel que le module métier le déclare. Repère "
                + "technique, à ne montrer qu'à défaut d'explication en clair.",
                example = "ACTIONS_SOLDEES")
        private String condition;

        /**
         * Ce que la condition veut dire, en clair, tel que l'auteur du circuit l'a écrit. Vide,
         * l'écran retombe sur le nom du fait — technique, mais toujours mieux que le silence.
         */
        @Schema(description = "Ce que la condition veut dire, en clair, tel que l'auteur du "
                + "circuit l'a écrit. Vide, il reste à montrer le nom du fait.",
                example = "Toutes les actions correctives doivent être soldées.")
        private String conditionLibelle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Une décision offerte à l'appelant, avec de quoi en dessiner le bouton. "
            + "Le serveur ne transporte ni classe CSS ni destination : il dit ce que l'action "
            + "vaut, l'écran décide du reste.")
    public static class WorkflowActionDto {

        /** Identifiant de la transition à jouer — c'est lui que reprend {@code /execute}. */
        @Schema(description = "Identifiant de la transition à jouer, à reprendre tel quel dans la "
                + "requête d'exécution. Technique : il change d'une installation à l'autre.",
                example = "137")
        private String code;

        /**
         * Code métier de l'action au sein de son étape ({@code APPROUVE},
         * {@code DEMANDER_COMPLEMENT}…), stable d'une installation à l'autre là où {@link #code}
         * est un identifiant technique. C'est par lui qu'un champ se rattache à une action précise.
         */
        @Schema(description = "Code métier de l'action au sein de son étape, stable d'une "
                + "installation à l'autre. C'est par lui qu'un champ de saisie se rattache à une "
                + "action précise, et non par l'identifiant de transition.",
                example = "DEMANDER_COMPLEMENT")
        private String actionCode;

        /** Libellé du bouton, tel que le circuit le déclare. */
        @Schema(description = "Texte du bouton, tel que l'auteur du circuit l'a écrit. Il est "
                + "aussi ce que l'historique conservera de la décision prise.",
                example = "Transmettre pour approbation")
        private String libelle;

        /** Icône du bouton, en classe PrimeIcons ({@code "pi pi-check"}). */
        @Schema(description = "Icône du bouton, en classe PrimeIcons. À défaut de valeur propre à "
                + "la transition, celle que porte la nature de la décision.",
                example = "pi pi-check")
        private String icon;

        /**
         * Couleur du bouton, dans le vocabulaire PrimeNG ({@code success}, {@code warn},
         * {@code danger}…) : la valeur est utilisable telle quelle dans {@code [severity]}.
         */
        @Schema(description = "Couleur du bouton, dans le vocabulaire PrimeNG : le jeton est "
                + "utilisable tel quel, sans conversion.",
                example = "success",
                allowableValues = {"primary", "secondary", "success", "info", "warn", "danger",
                        "help", "contrast"})
        private String severity;

        @Schema(description = "Rôle que la transition exige. Rendu à titre indicatif : l'action "
                + "ne figure ici que si l'appelant le porte déjà.",
                example = "PILOTE")
        private String permission;

        /**
         * Décision portée par la transition ({@code APPROUVE} / {@code REJETE}), pour distinguer
         * une approbation d'un rejet côté présentation.
         */
        @Schema(description = "Nature de la décision, pour distinguer à l'affichage une "
                + "approbation d'un rejet ou d'une clôture. Une étape peut offrir plusieurs "
                + "actions de même nature : c'est le code de l'action qui les sépare.",
                example = "APPROUVE",
                allowableValues = {"APPROUVE", "REJETE", "CLOTURE"})
        private String decision;
    }
}
