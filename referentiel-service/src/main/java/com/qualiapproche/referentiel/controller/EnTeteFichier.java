package com.qualiapproche.referentiel.controller;

import org.springframework.http.ContentDisposition;

import java.nio.charset.StandardCharsets;

/**
 * En-tête {@code Content-Disposition} d'un fichier servi au navigateur.
 *
 * <p>Les contrôleurs de ce service le composaient par concaténation —
 * {@code "attachment; filename=\"" + nom + "\""} — avec un nom qui vient d'un dépôt, donc d'un
 * client. Un guillemet y cassait l'en-tête, un saut de ligne permettait d'en injecter d'autres, et
 * un nom accentué partait en octets bruts que chaque navigateur interprétait à sa façon.</p>
 *
 * <p>Même règle que {@code EnTeteFichier} d'amelioration-service, dont c'est le pendant : la
 * place naturelle de cette aide serait le module commun.</p>
 */
final class EnTeteFichier {

    private EnTeteFichier() {
    }

    /**
     * Valeur de {@code Content-Disposition} pour un téléchargement.
     *
     * <p>Les caractères de contrôle, les guillemets et les barres obliques inverses sont
     * neutralisés, puis la valeur est écrite par {@link ContentDisposition} : un nom non ASCII y
     * gagne son encodage RFC 5987 ({@code filename*=UTF-8''…}).</p>
     */
    static String attachement(String nom) {
        String propre = nom == null ? "" : nom.replaceAll("[\\p{Cntrl}\"\\\\]", "_").trim();
        if (propre.isEmpty()) {
            propre = "fichier";
        }
        ContentDisposition.Builder disposition = ContentDisposition.attachment();
        // Un nom ASCII s'écrit tel quel : l'encodage n'apporterait qu'une forme moins lisible.
        return (StandardCharsets.US_ASCII.newEncoder().canEncode(propre)
                ? disposition.filename(propre)
                : disposition.filename(propre, StandardCharsets.UTF_8)).build().toString();
    }
}
