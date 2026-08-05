package com.qualiapproche.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepDto {
    private Long id;
    /**
     * Identifiant fonctionnel de l'étape, fixé à la création et non modifiable ensuite.
     * Généré à partir du nom si l'appelant n'en fournit pas.
     */
    private String code;
    private String nomEtape;
    private int stepOrder;
    private String responsableRole;
    private String description;
    private String etatTraitement;
    private String emailTemplateCode;
    /**
     * Modèle du catalogue d'étapes ayant servi à pré-remplir l'étape. Champ obligatoire côté écran
     * de configuration, il n'existait pas ici : la valeur transmise était silencieusement écartée
     * à la désérialisation.
     */
    private java.util.UUID stepTemplateId;

    /**
     * Nom du champ dont la valeur désigne le titulaire du dossier, s'il y en a un.
     *
     * <p>Absent du DTO, il était perdu à chaque enregistrement du circuit depuis l'éditeur : une
     * étape d'imputation cessait de nommer qui que ce soit, et les étapes réservées au titulaire
     * devenaient indécidables — plus personne ne pouvait faire avancer le dossier.</p>
     */
    private String champTitulaire;
    @Builder.Default
    private List<WorkflowTransitionDto> transitions = new ArrayList<>();
    @Builder.Default
    private List<WorkflowStepFieldDto> fields = new ArrayList<>();
}
