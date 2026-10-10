package com.qualiapproche.common.messagerie;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Laisse passer la première livraison d'un message, et écarte les suivantes.
 *
 * <p>À appeler en tête du traitement, <b>dans sa transaction</b> : la trace et l'effet du message
 * sont validés ensemble, ou pas du tout. Deux livraisons simultanées du même message se heurtent sur
 * la clé primaire ; la perdante échoue, le courtier la relivre, et elle est alors écartée.</p>
 */
@Slf4j
public class Dedoublonnage<E extends AbstractMessageTraite> {

    private final MessageTraiteRepository<E> depot;
    private final Supplier<E> fabrique;

    public Dedoublonnage(MessageTraiteRepository<E> depot, Supplier<E> fabrique) {
        this.depot = depot;
        this.fabrique = fabrique;
    }

    /** Vrai à la première livraison du message, faux s'il a déjà été traité. */
    public boolean premiereLivraison(String identifiant) {
        if (identifiant == null || identifiant.isBlank()) {
            // Message sans identifiant : publié hors des boîtes d'envoi, il ne peut être dédoublonné.
            return true;
        }
        UUID id = UUID.fromString(identifiant);
        if (depot.existsById(id)) {
            log.info("Message {} déjà traité : livraison en double écartée.", id);
            return false;
        }
        E trace = fabrique.get();
        trace.setId(id);
        trace.setRecuLe(LocalDateTime.now());
        depot.saveAndFlush(trace);
        return true;
    }
}
