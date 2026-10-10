package com.qualiapproche.storage;

/** La mise en page d'un document a échoué : au service hôte d'en faire une réponse. */
public class RenduDocumentException extends RuntimeException {

    public RenduDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
