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
     * Destinataire du courriel d'étape sous la forme {@code RÔLE@PORTÉE}, ou l'une des deux
     * désignations personnelles {@code @CREATEUR} / {@code @TITULAIRE}, quand ce n'est pas celui
     * qui doit agir à l'étape. Vide, le courriel suit la règle : le rôle de l'étape, dans la
     * structure du dossier.
     */
    private String destinataireCourriel;
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

    /**
     * Identifiants des personnes qui co-signent l'étape, et dont l'auteur du dossier est écarté.
     *
     * <p>Liste, et non chaîne : l'écran de configuration y présente une sélection multiple parmi
     * les utilisateurs. La forme stockée reste une chaîne — voir {@code Cosignataires} — mais c'est
     * une affaire de colonne, dont l'éditeur n'a pas à connaître la syntaxe.</p>
     *
     * <p>Liste vide : l'étape ne pose aucune séparation des signatures, et se décide à
     * l'habilitation seule.</p>
     */
    @Builder.Default
    private List<String> cosignataires = new ArrayList<>();

    @Builder.Default
    private List<WorkflowTransitionDto> transitions = new ArrayList<>();
    @Builder.Default
    private List<WorkflowStepFieldDto> fields = new ArrayList<>();
}
