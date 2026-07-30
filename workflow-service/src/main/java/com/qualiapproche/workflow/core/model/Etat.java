package com.qualiapproche.workflow.core.model;

import java.io.Serializable;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

/**
 * Etat d'un workflow. L'identite repose sur le seul {@code code}.
 *
 * <p>{@code equals}, {@code hashCode} et {@code toString} restent ecrits a la main :
 * l'identite porte sur le seul {@code code}, la ou {@code @Data} ou
 * {@code @EqualsAndHashCode} compareraient tous les champs.</p>
 */
@Getter
@Setter
public class Etat implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private String libelle;
    private String description;

    private String icon;
    private String badge;

    /**
     * Construit un etat vide.
     */
    public Etat() {
        // constructeur par defaut requis par les couches de mapping
    }

    /**
     * Construit un etat identifie par son code.
     *
     * @param pCode code de l'etat
     */
    public Etat(final String pCode) {
        this.code = pCode;
    }

    /**
     * Empreinte calculee sur le seul code.
     *
     * @return l'empreinte de l'etat
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.code);
    }

    /**
     * Egalite par {@code code}, tolerante aux sous-classes : une donnee peut porter un
     * {@code Etat} charge par une couche de persistance (proxy, sous-type) et rester
     * comparable a l'etat du metamodele. L'implementation historique comparait
     * {@code getClass()}, ce qui rendait ces deux instances differentes.
     *
     * @param pObj objet compare
     * @return vrai si l'objet compare est un etat de meme code
     */
    @Override
    public boolean equals(final Object pObj) {
        return this == pObj
                || pObj instanceof Etat aAutre && Objects.equals(this.code, aAutre.code);
    }

    /**
     * Representation lisible de l'etat.
     *
     * @return le code et le libelle de l'etat
     */
    @Override
    public String toString() {
        return "Etat [code=%s, libelle=%s]".formatted(this.code, this.libelle);
    }
}
