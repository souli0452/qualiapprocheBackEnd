package com.qualiapproche.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Une étape d'un circuit : qui y est attendu, ce qu'on lui demande de saisir, "
        + "et par quelles décisions le dossier en sort. Elle ne s'enregistre pas seule mais avec "
        + "le circuit entier.")
public class WorkflowStepDto {

    @Schema(description = "Identifiant technique de l'étape. Absent, l'étape est créée ; présent, "
            + "elle est retrouvée et mise à jour — c'est ce qui distingue une modification d'un "
            + "remplacement.")
    private Long id;

    /**
     * Identifiant fonctionnel de l'étape, fixé à la création et non modifiable ensuite.
     * Généré à partir du nom si l'appelant n'en fournit pas.
     */
    @Schema(description = "Identifiant fonctionnel de l'étape, fixé à la création et non "
            + "modifiable ensuite : les dossiers en cours s'y réfèrent. Il est dérivé du nom si "
            + "l'appelant n'en fournit pas.",
            example = "VALIDATION_PILOTE")
    private String code;

    @Schema(description = "Nom de l'étape, présenté aux utilisateurs. Il se corrige librement, "
            + "sans conséquence sur les dossiers en cours.",
            example = "Validation par le pilote")
    private String nomEtape;

    @Schema(description = "Rang de l'étape, qui fixe l'ordre du circuit. Ce n'est pas lui qui "
            + "décide de l'enchaînement : le chemin réel est celui que tracent les transitions.",
            example = "2")
    private int stepOrder;

    @Schema(description = "Rôle attendu pour décider ici, ou @TITULAIRE pour réserver l'étape à la "
            + "personne désignée sur le dossier. C'est aussi lui qui reçoit le courriel d'étape, à "
            + "moins qu'un destinataire ne soit précisé plus bas.",
            example = "PILOTE")
    private String responsableRole;

    @Schema(description = "Ce que l'étape attend et de qui, à l'usage de l'administrateur qui "
            + "configure le circuit.")
    private String description;

    @Schema(description = "Code d'état publié au module métier quand le dossier entre dans "
            + "l'étape. C'est par lui que la non-conformité ou le document nomme sa propre "
            + "situation, dans ses termes et non dans ceux du moteur.",
            example = "VALIDATION_RQ")
    private String etatTraitement;

    @Schema(description = "Modèle de courriel envoyé à l'entrée dans l'étape. Vide, aucun message "
            + "n'est adressé et l'étape n'est signalée que par la cloche.",
            example = "ncImputee")
    private String emailTemplateCode;

    /**
     * Destinataire du courriel d'étape sous la forme {@code RÔLE@PORTÉE}, ou l'une des deux
     * désignations personnelles {@code @CREATEUR} / {@code @TITULAIRE}, quand ce n'est pas celui
     * qui doit agir à l'étape. Vide, le courriel suit la règle : le rôle de l'étape, dans la
     * structure du dossier.
     */
    @Schema(description = "Destinataire du courriel d'étape quand ce n'est pas celui qui doit y "
            + "agir : soit RÔLE@PORTÉE, soit @CREATEUR ou @TITULAIRE. Vide, le courriel part au "
            + "rôle de l'étape dans la structure où le dossier se trouve. Une valeur mal formée "
            + "est ignorée sans erreur, la règle ordinaire reprenant la main.",
            example = "PILOTE@STRUCTURE_EMETTRICE")
    private String destinataireCourriel;

    /**
     * Modèle du catalogue d'étapes ayant servi à pré-remplir l'étape. Champ obligatoire côté écran
     * de configuration, il n'existait pas ici : la valeur transmise était silencieusement écartée
     * à la désérialisation.
     */
    @Schema(description = "Modèle du catalogue d'étapes ayant servi à pré-remplir celle-ci. Il "
            + "garde la filiation ; les valeurs, une fois copiées, vivent leur vie.")
    private java.util.UUID stepTemplateId;

    /**
     * Nom du champ dont la valeur désigne le titulaire du dossier, s'il y en a un.
     *
     * <p>Absent du DTO, il était perdu à chaque enregistrement du circuit depuis l'éditeur : une
     * étape d'imputation cessait de nommer qui que ce soit, et les étapes réservées au titulaire
     * devenaient indécidables — plus personne ne pouvait faire avancer le dossier.</p>
     */
    @Schema(description = "Nom du champ de cette étape dont la valeur saisie désigne le titulaire "
            + "du dossier. La décision ne fait alors pas qu'avancer le dossier : elle en nomme le "
            + "responsable, dont les étapes suivantes pourront se réclamer.",
            example = "userImputId")
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
    @Schema(description = "Personnes qui co-signent l'étape, par leur identifiant. Les nommer "
            + "écarte de l'étape l'auteur du dossier, fût-il l'une d'elles : c'est la séparation "
            + "des signatures. Liste vide, l'étape se décide à la seule habilitation.")
    @Builder.Default
    private List<String> cosignataires = new ArrayList<>();

    @Schema(description = "Décisions qui font sortir le dossier de cette étape. Une étape sans "
            + "transition est un cul-de-sac : le dossier y entre et n'en repart plus.")
    @Builder.Default
    private List<WorkflowTransitionDto> transitions = new ArrayList<>();

    @Schema(description = "Champs demandés au moment de décider. Chacun peut ne valoir que pour "
            + "une décision, ou pour une action précise.")
    @Builder.Default
    private List<WorkflowStepFieldDto> fields = new ArrayList<>();
}
