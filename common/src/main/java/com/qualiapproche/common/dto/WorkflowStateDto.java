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
    /**
     * Circuit que suit le dossier.
     *
     * <p>Relayé jusqu'à l'écran : sans lui, une fiche qui reçoit l'état d'un dossier connaît son
     * étape courante mais pas le circuit dont elle fait partie, donc ni ce qui reste à franchir ni
     * qui l'attendra ensuite.</p>
     */
    private UUID workflowId;
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
    private boolean ecarteCommeAuteur;
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
}
