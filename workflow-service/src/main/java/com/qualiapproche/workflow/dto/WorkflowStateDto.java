package com.qualiapproche.workflow.dto;

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
public class WorkflowStateDto {
    private UUID instanceId;
    private String status;
    private String currentStateCode;
    private String currentStateName;
    /**
     * Habilitation attendue à l'étape courante — un rôle, ou {@code @TITULAIRE} quand l'étape est
     * réservée à la personne désignée sur le dossier.
     *
     * <p>Sert à dire qui l'on attend lorsque l'appelant, lui, n'a rien à décider : sans cela un
     * écran sans action ne peut que se taire, et l'utilisateur voit un dossier immobile sans savoir
     * s'il doit agir, attendre, ou relancer quelqu'un.</p>
     */
    private String currentStepRole;
    @Builder.Default
    private List<WorkflowActionDto> allowedActions = new ArrayList<>();

    /**
     * Champs à saisir pour décider à l'étape courante. Sans eux, l'appelant n'avait aucun moyen
     * de construire le formulaire de validation ni de renseigner
     * {@code WorkflowValidationRequestDto.fields}, qui est indexé par identifiant de champ.
     */
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
    @Builder.Default
    private List<SaisieDto> saisies = new ArrayList<>();

    /** Une décision prévue par l'étape, et la condition qui lui manque. */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DecisionEnAttenteDto {
        /** Libellé du bouton tel qu'il apparaîtra une fois la condition remplie. */
        private String libelle;
        /** Nom du fait exigé, tel que le module métier le déclare. */
        private String condition;
        /**
         * Ce que la condition veut dire, en clair, tel que l'auteur du circuit l'a écrit. Vide,
         * l'écran retombe sur le nom du fait — technique, mais toujours mieux que le silence.
         */
        private String conditionLibelle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowActionDto {
        /** Identifiant de la transition à jouer — c'est lui que reprend {@code /execute}. */
        private String code;
        /**
         * Code métier de l'action au sein de son étape ({@code APPROUVE},
         * {@code DEMANDER_COMPLEMENT}…), stable d'une installation à l'autre là où {@link #code}
         * est un identifiant technique. C'est par lui qu'un champ se rattache à une action précise.
         */
        private String actionCode;
        /** Libellé du bouton, tel que le circuit le déclare. */
        private String libelle;
        /** Icône du bouton, en classe PrimeIcons ({@code "pi pi-check"}). */
        private String icon;
        /**
         * Couleur du bouton, dans le vocabulaire PrimeNG ({@code success}, {@code warn},
         * {@code danger}…) : la valeur est utilisable telle quelle dans {@code [severity]}.
         */
        private String severity;
        private String permission;
        /**
         * Décision portée par la transition ({@code APPROUVE} / {@code REJETE}), pour distinguer
         * une approbation d'un rejet côté présentation.
         */
        private String decision;
    }
}
