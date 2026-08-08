package com.qualiapproche.storage;

import java.util.Locale;
import java.util.Set;

/**
 * Ce qu'une pièce jointe doit respecter pour être acceptée, quel que soit le module qui la reçoit.
 *
 * <p>Les dépôts n'étaient bornés que par la limite multipart du serveur — un gigaoctet — et
 * n'excluaient aucun type : un exécutable d'un giga se déposait comme justificatif d'étape. Une
 * pièce de dossier qualité est un document, une image ou une archive de bureau ; le reste n'a rien
 * à faire dans le dépôt, et sa taille doit rester celle d'un document.</p>
 *
 * <p>Appliquées au point de dépôt commun ({@link AbstractFichierService}) : non-conformités, plans
 * d'action et tout module futur les obtiennent sans y penser. La vérification lève
 * {@link IllegalArgumentException} avec un message destiné à l'utilisateur — le gestionnaire
 * d'erreurs commun la rend en 400 avec ce message.</p>
 */
public final class ReglesDePieceJointe {

    /**
     * Taille maximale d'une pièce : 25 Mo.
     *
     * <p>Au-delà du plus gros document de bureau raisonnable, en deçà de ce qui ferait d'un dépôt
     * un vecteur de saturation du serveur de fichiers.</p>
     */
    public static final long TAILLE_MAX_OCTETS = 25L * 1024 * 1024;

    /**
     * Extensions admises : documents de bureau, images, et l'archive zip pour les lots de preuves.
     *
     * <p>Liste blanche et non liste noire : une liste noire court après les extensions dangereuses
     * et en oublie toujours une.</p>
     */
    private static final Set<String> EXTENSIONS_ADMISES = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp",
            "txt", "csv", "rtf",
            "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg",
            "zip");

    private ReglesDePieceJointe() {
    }

    /**
     * Vérifie une pièce reçue, et refuse avec un message qui dit quoi corriger.
     *
     * @param nomDuFichier nom déposé, dont l'extension est jugée
     * @param taille       taille en octets
     * @throws IllegalArgumentException si la pièce est refusée — message destiné à l'utilisateur
     */
    public static void verifier(String nomDuFichier, long taille) {
        if (taille > TAILLE_MAX_OCTETS) {
            throw new IllegalArgumentException(
                    "La pièce dépasse " + (TAILLE_MAX_OCTETS / (1024 * 1024))
                            + " Mo. Compressez-la ou découpez-la avant de la déposer.");
        }
        String extension = extensionDe(nomDuFichier);
        if (extension.isEmpty() || !EXTENSIONS_ADMISES.contains(extension)) {
            throw new IllegalArgumentException(
                    "Le type de fichier « " + (extension.isEmpty() ? nomDuFichier : "." + extension)
                            + " » n'est pas admis. Formats acceptés : documents de bureau, images, "
                            + "pdf, zip.");
        }
    }

    private static String extensionDe(String nom) {
        if (nom == null) {
            return "";
        }
        int point = nom.lastIndexOf('.');
        return point < 0 ? "" : nom.substring(point + 1).toLowerCase(Locale.ROOT).trim();
    }
}
