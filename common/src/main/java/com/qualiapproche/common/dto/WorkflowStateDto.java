package com.qualiapproche.common.dto;

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
@Schema(description = "Où en est un dossier dans son circuit, et ce que l'appelant peut y faire. "
        + "Tout y est calculé pour lui : les actions offertes sont celles que son habilitation lui "
        + "ouvre, un autre utilisateur recevant une autre liste sur le même dossier.")
public class WorkflowStateDto {
    @Schema(description = "Instance de circuit ouverte sur ce dossier. Un dossier repris après "
            + "clôture en ouvre une nouvelle : c'est elle qui identifie le parcours en cours, non "
            + "la ressource.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private UUID instanceId;
    /**
     * Circuit que suit le dossier.
     *
     * <p>Relayé jusqu'à l'écran : sans lui, une fiche qui reçoit l'état d'un dossier connaît son
     * étape courante mais pas le circuit dont elle fait partie, donc ni ce qui reste à franchir ni
     * qui l'attendra ensuite.</p>
     */
    @Schema(description = "Circuit que le dossier suit. Sans lui, une fiche connaît son étape "
            + "courante mais pas le parcours dont elle fait partie, donc ni ce qui reste à "
            + "franchir ni qui l'attendra ensuite. Nul sur les instances dont le circuit ne se "
            + "laisse plus résoudre.",
            example = "9b1f0c22-4b3e-4c2a-9a77-2f4d1b8e6c10")
    private UUID workflowId;

    @Schema(description = "Le circuit court-il encore, ou a-t-il rendu son verdict. Il ne dit pas "
            + "lequel : c'est l'étape terminale qui distingue une approbation d'un rejet.",
            example = "EN_COURS",
            allowableValues = {"EN_COURS", "TERMINE"})
    private String status;

    @Schema(description = "Identifiant technique de l'étape courante, à reprendre tel quel dans "
            + "« expectedStateCode » au moment de décider. Ce n'est pas le code fonctionnel de "
            + "l'étape : il change d'une installation à l'autre, et ne doit servir à reconnaître "
            + "aucune étape en particulier. Le circuit achevé, il prend la forme "
            + "« TERMINATED_APPROUVE », « TERMINATED_REJETE » ou « TERMINATED_CLOTURE ».",
            example = "42")
    private String currentStateCode;

    @Schema(description = "Étape courante telle qu'elle se présente à l'utilisateur, dans les "
            + "termes du circuit. Elle se reformule sans prévenir : ne rien y brancher.",
            example = "Vérification")
    private String currentStateName;
    /**
     * Habilitation attendue à l'étape courante — un rôle, ou {@code @TITULAIRE} quand l'étape est
     * réservée à la personne désignée sur le dossier.
     *
     * <p>Sert à dire qui l'on attend lorsque l'appelant, lui, n'a rien à décider : sans cela un
     * écran sans action ne peut que se taire, et l'utilisateur voit un dossier immobile sans savoir
     * s'il doit agir, attendre, ou relancer quelqu'un.</p>
     */
    @Schema(description = "Qui est attendu à l'étape courante : un rôle, ou l'une des deux "
            + "habilitations personnelles — « @TITULAIRE » pour la personne à qui le dossier a été "
            + "confié, « @CREATEUR » pour celle qui l'a ouvert. Sert à nommer l'attente quand "
            + "l'appelant, lui, n'a rien à décider : sans cela un écran sans action ne peut que se "
            + "taire.",
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
    @Schema(description = "L'appelant porte bien le rôle attendu, mais l'étape lui est fermée : il "
            + "a soumis ce dossier et l'étape le compte parmi ses signataires. Répond à la seule "
            + "question que le rôle attendu laisse sans réponse — pourquoi un pilote ne peut rien "
            + "sur son propre document. Ne vaut la peine d'être lu que lorsque aucune action n'est "
            + "offerte.",
            example = "true")
    private boolean ecarteCommeAuteur;

    @Schema(description = "Ce que l'appelant peut décider ici, et rien d'autre : la liste est déjà "
            + "filtrée par son habilitation, par la séparation des signatures et par les conditions "
            + "que le dossier remplit. Vide, il n'a qu'à attendre. Il n'y a donc aucun tri à "
            + "refaire côté appelant.")
    @Builder.Default
    private List<WorkflowActionDto> allowedActions = new ArrayList<>();

    /**
     * Champs à saisir pour décider à l'étape courante. Sans eux, l'appelant n'avait aucun moyen
     * de construire le formulaire de validation ni de renseigner
     * {@code WorkflowValidationRequestDto.fields}, indexé par identifiant de champ.
     */
    @Schema(description = "Tous les champs que l'étape déclare, sans égard à l'action retenue. "
            + "C'est à l'appelant de ne présenter que ceux dont la décision et le code d'action "
            + "correspondent à ce qu'il s'apprête à faire : un motif de rejet figure ici même "
            + "lorsque l'utilisateur approuve.")
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
            + "Elles ne figurent pas dans les actions offertes, et disent pourquoi le dossier "
            + "semble arrêté — la clôture attend le solde des actions correctives. Renseignées pour "
            + "tout le monde, y compris ceux qui n'ont rien à décider ici, puisque c'est le dossier "
            + "et non l'habilitation qui les retient.")
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
    @Schema(description = "Tout ce que le dossier a recueilli depuis l'ouverture de son circuit, "
            + "dans l'ordre où il l'a reçu. Une seule ligne par champ : la valeur la plus récente "
            + "fait foi, l'historique gardant les précédentes. Ces données suivent le dossier "
            + "partout où son état l'accompagne, ce qui dispense les modules métier de prévoir une "
            + "colonne pour chaque champ ajouté depuis l'éditeur de circuits.")
    @Builder.Default
    private List<SaisieDto> saisies = new ArrayList<>();

    /** Une décision prévue par l'étape, et la condition qui lui manque. */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @Schema(description = "Une décision que l'étape prévoit, et le fait qui manque au dossier pour "
            + "qu'elle devienne franchissable.")
    public static class DecisionEnAttenteDto {
        /** Libellé du bouton tel qu'il apparaîtra une fois la condition remplie. */
        @Schema(description = "Nom sous lequel la décision se présentera une fois la condition "
                + "remplie — le même libellé que portera l'action offerte.",
                example = "Clôturer la non-conformité")
        private String libelle;
        /** Nom du fait exigé, tel que le module métier le déclare. */
        @Schema(description = "Fait exigé du dossier, sous le nom que le module métier lui donne en "
                + "l'inscrivant. Le moteur ne compare que des chaînes : il ignore ce qu'un plan "
                + "d'action ou une efficacité peuvent être.",
                example = "PLANS_ACTION_SOLDES")
        private String condition;
        /**
         * Ce que la condition veut dire, en clair, tel que l'auteur du circuit l'a écrit. Vide,
         * l'écran retombe sur le nom du fait — technique, mais toujours mieux que le silence.
         */
        @Schema(description = "Ce que la condition veut dire, en clair, tel que l'auteur du circuit "
                + "l'a écrit. Vide, il reste à afficher le nom du fait — technique, mais toujours "
                + "mieux que le silence.",
                example = "Toutes les actions correctives doivent être soldées.")
        private String conditionLibelle;
    }
}
