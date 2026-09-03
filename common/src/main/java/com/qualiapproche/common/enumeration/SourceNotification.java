package com.qualiapproche.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Le domaine dont une notification provient.
 *
 * <p>La cloche est unique, ses sources ne le sont pas : chaque module dit ce qu'il a en attente,
 * et l'écran regroupe. Sans ce repère, une liste assemblée depuis trois services aurait présenté
 * pêle-mêle un document à viser et une licence qui expire, sans que rien ne dise d'où chaque ligne
 * venait ni pourquoi l'une avait disparu.</p>
 *
 * <p>C'est aussi ce qui permet d'annoncer une source momentanément muette : un module injoignable
 * retire ses lignes, et l'écran peut le dire au lieu de laisser croire qu'il n'y a rien à faire.</p>
 */
@Schema(description = "Domaine dont une notification provient. Sert à regrouper l'affichage et "
        + "à signaler un module momentanément muet.")
public enum SourceNotification {

    /** Non-conformités et plans d'action. */
    AMELIORATION,

    /** Documents du système qualité et demandes qui les visent. */
    DOCUMENTAIRE,

    /** Licence de l'installation. */
    LICENCE
}
