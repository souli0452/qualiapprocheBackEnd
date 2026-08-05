package com.qualiapproche.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransitionDto {
    private Long id;
    /** Libellé du bouton d'action. Vide, le nom de la décision est repris. */
    private String label;
    /**
     * Icône du bouton, en classe PrimeIcons ({@code "pi pi-check"}). Vide, celle de la décision
     * est reprise.
     */
    private String icon;
    /**
     * Couleur du bouton, dans le vocabulaire PrimeNG : {@code success}, {@code info},
     * {@code warn}, {@code danger}, {@code secondary}, {@code contrast}, {@code help},
     * {@code primary}. La casse est libre et {@code warning} vaut {@code warn}. Vide, la couleur
     * de la décision est reprise.
     */
    private String severity;
    private String decision;
    private String requiredRole;
    /**
     * Code de l'étape de destination : clé désignant la cible, stable dans le temps et connue de
     * l'appelant avant même l'enregistrement du circuit.
     */
    private String toStepCode;
    private Long toStepId;
    /** Libellé de la destination, pour l'affichage uniquement — jamais utilisé comme clé. */
    private String toStepName;
    /**
     * Rang de l'étape de destination. Exposé en plus de {@code toStepId} parce que les écrans de
     * configuration raisonnent en rang d'étape et non en identifiant technique : sans ce champ,
     * la destination d'une transition ne pouvait pas être restituée.
     */
    private Integer toStepOrder;
    /** La décision clôt le circuit. À distinguer d'une transition simplement non configurée. */
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
    private String conditionRequise;
    /** Ce que la condition veut dire, en clair, pour l'afficher à qui attend que le dossier avance. */
    private String conditionLibelle;
}
