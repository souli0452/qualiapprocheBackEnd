package com.qualiapproche.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Une décision offerte à une étape, et ce qu'elle fait du dossier. C'est elle "
        + "qui trace le chemin réel du circuit : le rang des étapes n'en donne que l'ordre "
        + "d'affichage.")
public class WorkflowTransitionDto {

    @Schema(description = "Identifiant technique de la transition. Absent, elle est créée ; c'est "
            + "lui que l'exécution d'une décision reprend.",
            example = "137")
    private Long id;

    /**
     * Code de l'action au sein de son étape ({@code APPROUVE}, {@code DEMANDER_COMPLEMENT}…).
     *
     * <p>C'est lui qui identifie l'action, et non sa décision : une étape peut en offrir plusieurs
     * de même nature. Vide, le nom de la décision est repris — et rendu unique si l'étape le porte
     * déjà.</p>
     */
    @Schema(description = "Code métier de l'action au sein de son étape, stable d'une installation "
            + "à l'autre. C'est par lui qu'un champ de saisie se rattache à cette action. Vide, le "
            + "nom de la décision est repris, et rendu unique si l'étape le porte déjà.",
            example = "DEMANDER_COMPLEMENT")
    private String code;

    /** Libellé du bouton d'action. Vide, le nom de la décision est repris. */
    @Schema(description = "Texte du bouton. C'est aussi ce que l'historique conservera de la "
            + "décision prise : le laisser vide fait retomber la trace sur le nom de la décision.",
            example = "Transmettre pour approbation")
    private String label;

    /**
     * Icône du bouton, en classe PrimeIcons ({@code "pi pi-check"}). Vide, celle de la décision
     * est reprise.
     */
    @Schema(description = "Icône du bouton, en classe PrimeIcons. Stockée telle que saisie : la "
            + "liste appartient au thème du client, et la figer ici obligerait à livrer le serveur "
            + "à chaque icône nouvelle.",
            example = "pi pi-check")
    private String icon;

    /**
     * Couleur du bouton, dans le vocabulaire PrimeNG : {@code success}, {@code info},
     * {@code warn}, {@code danger}, {@code secondary}, {@code contrast}, {@code help},
     * {@code primary}. La casse est libre et {@code warning} vaut {@code warn}. Vide, la couleur
     * de la décision est reprise.
     */
    @Schema(description = "Couleur du bouton, dans le vocabulaire PrimeNG. La casse est libre et "
            + "« warning » est accepté pour « warn », la sévérité ayant changé de nom entre deux "
            + "versions de la bibliothèque. Vide, la couleur de la décision est reprise.",
            example = "success",
            allowableValues = {"primary", "secondary", "success", "info", "warn", "danger",
                    "help", "contrast"})
    private String severity;

    @Schema(description = "Nature de la décision. Elle ne suffit pas à identifier l'action — une "
            + "étape peut en offrir plusieurs de même nature — mais elle commande l'issue publiée "
            + "aux modules métier lorsque la transition clôt le circuit.",
            example = "APPROUVE",
            allowableValues = {"APPROUVE", "REJETE", "CLOTURE"})
    private String decision;

    @Schema(description = "Rôle exigé pour prendre cette décision, lorsqu'il est plus étroit que "
            + "celui de l'étape. La séparation des signatures s'y ajoute et peut écarter quelqu'un "
            + "qui porte pourtant le rôle.",
            example = "RESPONSABLE_QUALITE")
    private String requiredRole;

    /**
     * Code de l'étape de destination : clé désignant la cible, stable dans le temps et connue de
     * l'appelant avant même l'enregistrement du circuit.
     */
    @Schema(description = "Étape d'arrivée, par son code. À préférer à l'identifiant lors d'une "
            + "création : le code est connu de l'appelant avant même que le circuit soit "
            + "enregistré.",
            example = "APPROBATION")
    private String toStepCode;

    @Schema(description = "Étape d'arrivée, par son identifiant technique. Inconnu tant que "
            + "l'étape n'existe pas en base.")
    private Long toStepId;

    /** Libellé de la destination, pour l'affichage uniquement — jamais utilisé comme clé. */
    @Schema(description = "Nom de l'étape d'arrivée, pour l'affichage seul. Ne jamais s'en servir "
            + "comme clé : il se corrige librement.",
            example = "Approbation")
    private String toStepName;

    /**
     * Rang de l'étape de destination. Exposé en plus de {@code toStepId} parce que les écrans de
     * configuration raisonnent en rang d'étape et non en identifiant technique : sans ce champ,
     * la destination d'une transition ne pouvait pas être restituée.
     */
    @Schema(description = "Rang de l'étape d'arrivée. Rendu en plus de l'identifiant parce que "
            + "l'éditeur de circuits raisonne en rang : sans lui, il ne pouvait pas réafficher la "
            + "destination d'une transition qu'il venait d'enregistrer.",
            example = "3")
    private Integer toStepOrder;

    /** La décision clôt le circuit. À distinguer d'une transition simplement non configurée. */
    @Schema(description = "La décision clôt le circuit au lieu de mener ailleurs. Marqueur "
            + "explicite, car une transition sans destination pouvait aussi bien vouloir dire "
            + "« cette décision termine le dossier » que « cette décision n'a pas lieu d'être ».",
            example = "false")
    private boolean terminal;

    /**
     * Fait que le dossier doit porter pour que la transition soit franchissable, ou vide si elle
     * l'est sans condition.
     *
     * <p>C'est le seul point de contact entre une règle métier et le circuit : le moteur exige un
     * fait sans savoir ce qu'il recouvre, et le module métier le déclare sans savoir quelle
     * transition l'attend. Sans ce champ, la condition existait en base mais aucun écran ne
     * pouvait la poser — il fallait la coder dans l'initialiseur.</p>
     */
    @Schema(description = "Fait que le dossier doit porter pour que la décision soit "
            + "franchissable. Chaîne libre, mise en majuscules : le moteur l'exige sans savoir ce "
            + "qu'elle recouvre, et le module métier l'inscrit sans savoir quelle transition "
            + "l'attend. Vide, la décision ne dépend d'aucune condition.",
            example = "PLANS_ACTION_SOLDES")
    private String conditionRequise;

    /** Ce que la condition veut dire, en clair, pour l'afficher à qui attend que le dossier avance. */
    @Schema(description = "Ce que la condition veut dire, en clair, écrit par l'auteur du circuit "
            + "là où il pose la condition. L'écran ne saurait traduire seul le nom du fait sans se "
            + "doter d'une table qui mentirait au premier fait nouveau.",
            example = "Toutes les actions correctives doivent être soldées.")
    private String conditionLibelle;
}
