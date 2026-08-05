package com.qualiapproche.amelioration.utils;

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
     * <p>Les guillemets et les sauts de ligne sont neutralisés : ils casseraient l'en-tête, et un
     * saut de ligne y permettrait d'en injecter d'autres.</p>
     */
    public static String attachement(String nom) {
        String propre = nom == null || nom.isBlank() ? "fichier" : nom.replaceAll("[\"\\r\\n]", "_");
        return "attachment; filename=\"" + propre + "\"";
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
