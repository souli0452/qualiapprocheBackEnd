package com.qualiapproche.evaluation.enumeration;

/**
 * Ce qu'un visa atteste sur le rapport : qui l'a rédigé, vérifié, approuvé.
 *
 * <p>Il ne fixe pas d'ordre de signature : chacun signe quand il le peut, comme la maquette le
 * veut, pour qu'un absent ne retienne pas les autres. Il dit seulement à quel titre on signe
 * (recette du 17 oct. 2026, observation 26).</p>
 */
public enum RoleVisa {
    REDIGE("Rédigé par"),
    VERIFIE("Vérifié par"),
    APPROUVE("Approuvé par");

    private final String libelle;

    RoleVisa(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
