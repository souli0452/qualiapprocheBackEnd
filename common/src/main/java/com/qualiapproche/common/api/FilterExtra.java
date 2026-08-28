package com.qualiapproche.common.api;

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
public class FilterExtra {

    /** Attribut filtré, éventuellement imbriqué ({@code "structure.libelle"}). */
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
    private List<String> fields;

    /**
     * Valeur comparée. Une collection pour {@link FilterOperator#IN}, une valeur simple sinon.
     *
     * <p>Reçue telle que le JSON la porte — chaîne, nombre, booléen — et convertie au type réel de
     * la colonne au moment de composer la clause : un identifiant arrive en chaîne, une date aussi,
     * et les comparer sans conversion échouerait à l'exécution.</p>
     */
    private Object value;

    /** Comparaison appliquée. Absente, le critère est ignoré. */
    private FilterOperator operator;

    /** Borne haute, pour {@link FilterOperator#BETWEEN} seulement. */
    private String valueTo;
}
