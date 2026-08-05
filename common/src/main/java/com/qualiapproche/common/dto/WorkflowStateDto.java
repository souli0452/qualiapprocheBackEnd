package com.qualiapproche.common.dto;

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
     * {@code WorkflowValidationRequestDto.fields}, indexé par identifiant de champ.
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
}
