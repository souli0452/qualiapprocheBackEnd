package com.qualiapproche.common.dto;

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
@Schema(description = "Une donnée recueillie sur un dossier au cours de son circuit, avec de quoi "
        + "dire d'où elle vient. Elle accompagne l'état du circuit partout où celui-ci suit la "
        + "ressource — sa fiche comme les lignes de liste — ce qui la rend lisible sans passer par "
        + "l'historique et dispense les modules métier de la recopier.")
public class SaisieDto {

    /** Nom technique du champ — clé stable, la seule qu'un module métier puisse reconnaître. */
    @Schema(description = "Nom technique du champ, et clé de la saisie : une seule ligne par nom, "
            + "la plus récente. C'est le seul repère stable pour retrouver une donnée précise, le "
            + "libellé pouvant être reformulé.",
            example = "userImputId")
    private String fieldName;

    /**
     * Intitulé présenté à qui a saisi, conservé avec la valeur : un champ retiré du circuit laisse
     * ses saisies lisibles.
     */
    @Schema(description = "Intitulé sous lequel la donnée a été demandée, conservé avec elle : un "
            + "champ retiré du circuit laisse ses saisies lisibles. À défaut, le nom technique.",
            example = "Agent chargé du traitement")
    private String fieldLabel;

    /** La valeur, telle qu'elle a été saisie. */
    @Schema(description = "La valeur, sous la forme où elle a été saisie. Toujours du texte, quel "
            + "que soit le type du champ ; une liste alimentée par une source y laisse "
            + "l'identifiant retenu, non son libellé.",
            example = "8c1f9b74-2d3e-4a55-9f0c-71b2ad6e5c48")
    private String value;

    /** Code de l'étape où elle a été recueillie. */
    @Schema(description = "Étape qui a recueilli la donnée, désignée par son identifiant technique. "
            + "Propre à l'installation : il situe la saisie dans le parcours, il ne nomme aucune "
            + "étape en particulier.",
            example = "42")
    private String stepCode;

    /** Nom de cette étape, pour dire d'où vient la donnée sans consulter le circuit. */
    @Schema(description = "Nom que portait cette étape, conservé avec la saisie plutôt que relu sur "
            + "le circuit : la donnée dit d'où elle vient même si l'étape a été renommée ou "
            + "supprimée depuis.",
            example = "Imputation")
    private String stepName;

    /** Date de la décision qui l'a recueillie. */
    @Schema(description = "Quand la donnée a été recueillie — la date de la décision qui l'a "
            + "portée, et non celle d'une modification ultérieure de la fiche.",
            example = "2026-03-14T09:25:00")
    private LocalDateTime decisionDate;

    /** Nom de qui a saisi, tel qu'il se présentait alors ; à défaut, son identifiant. */
    @Schema(description = "Qui a saisi la donnée, sous le nom qu'il portait alors. À défaut, son "
            + "identifiant : mieux vaut un repère technique qu'une colonne vide.",
            example = "Awa Traoré")
    private String auteur;
}
