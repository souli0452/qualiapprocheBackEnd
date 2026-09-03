package com.qualiapproche.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Une donnée saisie sur un dossier au cours de son circuit, telle qu'elle reste attachée à lui.
 *
 * <p>Les champs d'étape étaient recueillis à la décision, écrits dans l'historique, puis perdus de
 * vue : pour qu'une saisie reparaisse sur la fiche, il fallait qu'un module métier la recopie dans
 * une colonne à lui — ce que faisaient la non-conformité pour l'agent imputé et le plan d'action
 * pour son compte rendu, et personne pour le reste. Tout ce qui était demandé aux autres étapes
 * n'existait donc nulle part hors de l'onglet d'historique, décision par décision.</p>
 *
 * <p>Ces valeurs voyagent désormais avec l'état du circuit, qui est joint au détail comme à chaque
 * ligne de liste : la saisie colle au dossier de bout en bout, sans qu'aucun module ait à la
 * connaître, à la dupliquer, ni à prévoir une colonne pour chaque champ qu'un administrateur
 * ajoutera demain depuis l'éditeur.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Donnée recueillie par une étape du circuit et restée attachée au dossier. "
        + "Elle voyage avec l'état du circuit, si bien qu'aucun module métier n'a de colonne à "
        + "prévoir pour un champ que l'éditeur ajoutera demain.")
public class SaisieDto {

    /** Nom technique du champ — clé stable, la seule qu'un module métier puisse reconnaître. */
    @Schema(description = "Nom technique du champ. C'est la seule clé stable : un module métier "
            + "qui cherche une saisie précise doit s'appuyer sur elle, et non sur l'intitulé.",
            example = "agentImpute")
    private String fieldName;

    /**
     * Intitulé présenté à qui a saisi, conservé avec la valeur : un champ retiré du circuit laisse
     * ses saisies lisibles.
     */
    @Schema(description = "Intitulé tel qu'il était présenté à qui a saisi, figé avec la valeur. "
            + "Un champ retiré du circuit, ou renommé, laisse donc ses saisies lisibles.",
            example = "Agent chargé du traitement")
    private String fieldLabel;

    /** La valeur, telle qu'elle a été saisie. */
    @Schema(description = "Valeur saisie, toujours rendue en chaîne quel que soit le type déclaré "
            + "par le champ. Pour une liste alimentée par un référentiel, c'est l'identifiant "
            + "retenu et non le libellé affiché.")
    private String value;

    /** Code de l'étape où elle a été recueillie. */
    @Schema(description = "Étape qui a recueilli la donnée, par son code. Une même clé peut être "
            + "demandée à plusieurs étapes : c'est ce qui les distingue.",
            example = "IMPUTATION")
    private String stepCode;

    /** Nom de cette étape, pour dire d'où vient la donnée sans consulter le circuit. */
    @Schema(description = "Nom de cette étape, pour dire d'où vient la donnée sans avoir à "
            + "rappeler le circuit.",
            example = "Imputation par le pilote")
    private String stepName;

    /** Date de la décision qui l'a recueillie. */
    @Schema(description = "Date de la décision qui a recueilli la donnée. C'est elle qui départage "
            + "deux saisies d'un même champ : la plus récente fait foi.")
    private LocalDateTime decisionDate;

    /** Nom de qui a saisi, tel qu'il se présentait alors ; à défaut, son identifiant. */
    @Schema(description = "Qui a saisi, sous le nom qu'il portait alors. À défaut — décisions "
            + "antérieures à la conservation du nom — son identifiant technique.")
    private String auteur;
}
