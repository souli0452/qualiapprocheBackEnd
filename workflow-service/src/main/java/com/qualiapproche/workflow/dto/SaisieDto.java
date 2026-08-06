package com.qualiapproche.workflow.dto;

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
public class SaisieDto {

    /** Nom technique du champ — clé stable, la seule qu'un module métier puisse reconnaître. */
    private String fieldName;

    /**
     * Intitulé présenté à qui a saisi, conservé avec la valeur : un champ retiré du circuit laisse
     * ses saisies lisibles.
     */
    private String fieldLabel;

    /** La valeur, telle qu'elle a été saisie. */
    private String value;

    /** Code de l'étape où elle a été recueillie. */
    private String stepCode;

    /** Nom de cette étape, pour dire d'où vient la donnée sans consulter le circuit. */
    private String stepName;

    /** Date de la décision qui l'a recueillie. */
    private LocalDateTime decisionDate;

    /** Nom de qui a saisi, tel qu'il se présentait alors ; à défaut, son identifiant. */
    private String auteur;
}
