package com.qualiapproche.storage;

/**
 * Pièce dont le contenu vit sur le serveur d'objets.
 *
 * <p>Contrat minimal que doit remplir l'entité d'un module pour être gérée par
 * {@link AbstractFichierService} : de quoi retrouver l'objet ({@code url}) et de quoi le rendre à
 * l'utilisateur sous son identité d'origine ({@code nom}, {@code type}).</p>
 *
 * <p>Volontairement pauvre : l'entité de chaque module porte bien d'autres choses — un
 * rattachement, un horodatage, un auteur — qui ne regardent pas le stockage.</p>
 */
public interface FichierStocke {

    /** Nom d'origine du fichier, tel que l'utilisateur le reconnaît. */
    String getNom();

    void setNom(String nom);

    /** Extension, sans le point. */
    String getExt();

    void setExt(String ext);

    /** Type MIME déclaré au dépôt. */
    String getType();

    void setType(String type);

    /**
     * Référence de l'objet sur le serveur, préfixes de dossier compris.
     *
     * <p>C'est la seule adresse du contenu : rien n'est conservé en base ni sur le disque du
     * service.</p>
     */
    String getUrl();

    void setUrl(String url);
}
