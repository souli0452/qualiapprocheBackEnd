package com.qualiapproche.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Un critère de recherche : une colonne, une comparaison, une valeur.
 *
 * <p>C'est ce qui remplace les signatures à dix-sept paramètres. Une recherche filtrée n'a plus à
 * être prévue colonne par colonne dans le service, le contrôleur et l'objet de transfert : l'écran
 * dit sur quoi il filtre, et un filtre nouveau n'appelle aucune livraison.</p>
 *
 * <p>Le champ admet un chemin : {@code "planActions.responsable"} traverse la relation. Le nom
 * désigne un attribut de l'<b>entité</b>, non une colonne SQL.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Un critère de recherche : une colonne, une comparaison, une valeur. Le "
        + "nom désigne un attribut de l'entité et non une colonne SQL, et il admet un chemin qui "
        + "traverse une relation.")
public class FilterExtra {

    /** Attribut filtré, éventuellement imbriqué ({@code "structure.libelle"}). */
    @Schema(description = "Attribut filtré. Un point traverse une relation. Un nom que l'entité "
            + "ne porte pas est refusé, et non ignoré silencieusement.",
            example = "structure.libelle")
    private String field;

    /**
     * Attributs alternatifs, lorsque la même comparaison doit valoir sur l'un <b>ou</b> l'autre.
     *
     * <p>« Les dossiers qui me concernent » n'est pas une colonne : c'est ceux que j'ai déclarés
     * <b>ou</b> ceux qui me sont imputés. Un périmètre de ce genre ne s'exprime pas par des
     * critères cumulés, qui se combinent par un ET, et chaque écran devait donc s'appuyer sur un
     * point d'entrée écrit pour lui.</p>
     *
     * <p>Renseigné, il remplace {@link #field} : la clause est la disjonction de la même
     * comparaison sur chacun des attributs nommés.</p>
     */
    @Schema(description = "Attributs alternatifs, quand la même comparaison doit valoir sur l'un "
            + "ou l'autre. « Les dossiers qui me concernent » n'est pas une colonne : c'est ceux "
            + "que j'ai déclarés ou ceux qui me sont imputés, et des critères cumulés se "
            + "combinent par un ET. Renseigné, il remplace le champ unique.",
            example = "[\"createdById\", \"userImputId\"]")
    private List<String> fields;

    /**
     * Valeur comparée. Une collection pour {@link FilterOperator#IN}, une valeur simple sinon.
     *
     * <p>Reçue telle que le JSON la porte — chaîne, nombre, booléen — et convertie au type réel de
     * la colonne au moment de composer la clause : un identifiant arrive en chaîne, une date aussi,
     * et les comparer sans conversion échouerait à l'exécution.</p>
     */
    @Schema(description = "Valeur comparée : une collection pour l'opérateur IN, une valeur "
            + "simple sinon. Elle est reçue telle que le JSON la porte, puis convertie au type "
            + "réel de la colonne — un identifiant et une date arrivent tous deux en chaîne.",
            example = "Majeure")
    private Object value;

    /** Comparaison appliquée. Absente, le critère est ignoré. */
    @Schema(description = "Comparaison appliquée. Absente, le critère est ignoré plutôt que "
            + "refusé.", example = "EQ")
    private FilterOperator operator;

    /** Borne haute, pour {@link FilterOperator#BETWEEN} seulement. */
    @Schema(description = "Borne haute, pour le seul opérateur BETWEEN.", example = "2026-12-31")
    private String valueTo;
}
