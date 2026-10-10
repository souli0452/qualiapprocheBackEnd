package com.qualiapproche.amelioration.utils;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

/**
 * En-têtes de réponse d'un fichier servi au navigateur.
 *
 * <p>Le nom et le type viennent d'un client : ni l'un ni l'autre ne peut être recopié tel quel
 * dans un en-tête.</p>
 */
public final class EnTeteFichier {

    private EnTeteFichier() {
    }

    /**
     * Valeur de {@code Content-Disposition} pour un téléchargement.
     *
     * <p>Les caractères de contrôle, les guillemets et les barres obliques inverses sont
     * neutralisés : ils casseraient l'en-tête, et un saut de ligne y permettrait d'en injecter
     * d'autres. La valeur est ensuite écrite par {@link ContentDisposition} plutôt que
     * concaténée : un nom accentué — « Procédure d'achat.pdf » — y gagne son encodage RFC 5987
     * ({@code filename*=UTF-8''…}), là où la concaténation l'envoyait en octets bruts que chaque
     * navigateur interprétait à sa façon.</p>
     */
    public static String attachement(String nom) {
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

    /** Type MIME déclaré, ou un type générique s'il est absent ou illisible. */
    public static MediaType typeDeContenu(String type) {
        if (type == null || type.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(type);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
