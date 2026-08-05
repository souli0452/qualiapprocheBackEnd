package com.qualiapproche.storage;

/**
 * Le serveur de fichiers n'a pas pu traiter la demande.
 *
 * <p>Non contrôlée à dessein : le client MinIO déclare {@code throws Exception}, ce que les
 * couches appelantes ne peuvent pas traiter utilement — elles se contentaient de l'avaler ou de la
 * remonter en erreur serveur nue.</p>
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
