package com.qualiapproche.common.messagerie;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

import java.util.Optional;

/**
 * Lit un message reçu, ou l'écarte s'il est illisible.
 *
 * <p>Un message illisible — JSON invalide, type inconnu ou inattendu sur cette file — ne le sera
 * pas davantage à la trois-centième livraison. Laissé à la conversion de Spring, il levait une
 * exception, revenait au courtier et était relivré trois jours durant avant le rebut. Il est ici
 * journalisé puis acquitté.</p>
 *
 * <p>Seul l'illisible est écarté : une erreur de <b>traitement</b> (dossier pas encore visible,
 * base indisponible) remonte toujours, et le message est relivré — un rappel peut arriver avant
 * que la transaction qui a créé son dossier soit validée.</p>
 */
@Slf4j
public class LecteurDeMessages {

    private final MessageConverter convertisseur;

    public LecteurDeMessages(MessageConverter convertisseur) {
        this.convertisseur = convertisseur;
    }

    /** Le contenu du message s'il est lisible et du type attendu ; vide sinon (et journalisé). */
    public <T> Optional<T> lire(Message message, Class<T> attendu) {
        Object contenu;
        try {
            contenu = convertisseur.fromMessage(message);
        } catch (MessageConversionException | JMSException e) {
            log.error("Message {} illisible, écarté sans nouvelle livraison : {}", identifiant(message),
                    e.getMessage());
            return Optional.empty();
        }
        if (!attendu.isInstance(contenu)) {
            log.error("Message {} écarté : {} attendu, {} reçu.", identifiant(message), attendu.getSimpleName(),
                    contenu == null ? "rien" : contenu.getClass().getSimpleName());
            return Optional.empty();
        }
        return Optional.of(attendu.cast(contenu));
    }

    /** L'identifiant de la ligne d'outbox qui a produit le message : la clé de dédoublonnage. */
    public String identifiant(Message message) {
        try {
            return message.getStringProperty(AdressesMessagerie.PROPRIETE_IDENTIFIANT);
        } catch (JMSException e) {
            return null;
        }
    }
}
