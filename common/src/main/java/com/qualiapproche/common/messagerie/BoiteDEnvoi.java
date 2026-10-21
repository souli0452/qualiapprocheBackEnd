package com.qualiapproche.common.messagerie;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * La boîte d'envoi d'un service : ce qu'il a à dire aux autres, conservé en base puis publié sur
 * le courtier (patron « transactional outbox »).
 *
 * <ul>
 *   <li>{@link #deposer} inscrit le message dans la table {@code message_sortant}. Dans une
 *       transaction, il n'existe que si elle est validée — une décision annulée n'annonce rien — et
 *       sa publication est tentée juste après le commit ;</li>
 *   <li>{@link #tournee}, appelée par le relais sous verrou ShedLock, publie ce qui attend encore :
 *       un courtier injoignable n'est qu'un retard, le message est retenté avec une attente qui
 *       double jusqu'à quinze minutes, sans jamais être abandonné ;</li>
 *   <li>chaque message est d'abord <b>revendiqué</b> en une instruction : la publication d'après
 *       commit et la tournée ne le publient pas deux fois.</li>
 * </ul>
 *
 * <p>Le message porte l'identifiant de sa ligne ({@link AdressesMessagerie#PROPRIETE_IDENTIFIANT}) :
 * c'est la clé de dédoublonnage du consommateur ({@link Dedoublonnage}), car un courtier livre
 * « au moins une fois ».</p>
 *
 * <p>Repris des boîtes d'envoi de sigatt, avec deux corrections : les courriels y passent aussi
 * (sigatt les publiait sans outbox, perdus si le courtier était absent), et la tournée est sous
 * verrou.</p>
 *
 * @param <E> l'entité de message du service
 */
@Slf4j
public class BoiteDEnvoi<E extends AbstractMessageSortant> {

    private static final long ATTENTE_INITIALE_SECONDES = 2;
    private static final long ATTENTE_MAX_SECONDES = 900;
    /** Au-delà, une publication revendiquée et muette est tenue pour interrompue. */
    private static final Duration DELAI_DE_REPRISE = Duration.ofMinutes(2);
    private static final int TAILLE_MAX_ERREUR = 2000;
    /** Ce qui reste d'un message confidentiel une fois publié : la ligne, sans le secret. */
    static final String CONTENU_EFFACE = "";

    private final MessageSortantRepository<E> depot;
    private final Supplier<E> fabrique;
    private final ObjectMapper json;
    private final ObjectProvider<JmsTemplate> courtier;
    private final TransactionTemplate transactionPropre;
    private final Clock horloge;

    public BoiteDEnvoi(MessageSortantRepository<E> depot, Supplier<E> fabrique, ObjectMapper json,
                       ObjectProvider<JmsTemplate> courtier, PlatformTransactionManager transactions) {
        this(depot, fabrique, json, courtier, transactions, Clock.systemDefaultZone());
    }

    BoiteDEnvoi(MessageSortantRepository<E> depot, Supplier<E> fabrique, ObjectMapper json,
                ObjectProvider<JmsTemplate> courtier, PlatformTransactionManager transactions, Clock horloge) {
        this.depot = depot;
        this.fabrique = fabrique;
        this.json = json;
        this.courtier = courtier;
        this.transactionPropre = new TransactionTemplate(transactions);
        this.transactionPropre.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.horloge = horloge;
    }

    /**
     * Inscrit un message ; sa publication est tentée après le commit de la transaction en cours,
     * ou tout de suite s'il n'y en a pas.
     *
     * @return l'identifiant du message
     */
    public UUID deposer(String destination, String type, Object contenu) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        E message = fabrique.get();
        message.setId(UUID.randomUUID());
        message.setDestination(destination);
        message.setType(type);
        message.setContenu(serialiser(contenu));
        message.setStatut(StatutMessage.A_PUBLIER);
        message.setTentatives(0);
        message.setProchaineTentativeAt(maintenant);
        message.setCreeLe(maintenant);
        depot.save(message);
        UUID id = message.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publierSansEchec(id);
                }
            });
        } else {
            publierSansEchec(id);
        }
        return id;
    }

    /** Publie ce qui attend, au plus {@code lot} messages ; rend le nombre de messages publiés. */
    public int tournee(int lot) {
        if (courtier.getIfAvailable() == null) {
            return 0;
        }
        int publies = 0;
        for (UUID id : depot.echus(LocalDateTime.now(horloge), lot)) {
            if (publier(id)) {
                publies++;
            }
        }
        return publies;
    }

    /** Efface les messages publiés depuis plus de {@code conservation}. */
    public int purger(Duration conservation) {
        Integer purges = transactionPropre.execute(s ->
                depot.purgerLesPublies(StatutMessage.PUBLIE, LocalDateTime.now(horloge).minus(conservation)));
        return purges == null ? 0 : purges;
    }

    /**
     * Efface les messages confidentiels déposés depuis plus de {@code conservation}, publiés ou
     * non. Un mot de passe temporaire ou un lien qui n'a pas pu partir à temps est perdu : il
     * faudra le redemander, ce qui vaut mieux que de le garder en clair.
     */
    public int purgerLesConfidentiels(Duration conservation) {
        Integer purges = transactionPropre.execute(s -> depot.purgerParType(
                List.of(AdressesMessagerie.TYPE_COURRIEL_CONFIDENTIEL),
                LocalDateTime.now(horloge).minus(conservation)));
        return purges == null ? 0 : purges;
    }

    /** Ce qui attend sa publication : pour une sonde ou un tableau de bord. */
    public long enAttente() {
        return depot.countByStatut(StatutMessage.A_PUBLIER) + depot.countByStatut(StatutMessage.EN_COURS);
    }

    /** Une publication d'après commit ne fait jamais échouer le geste qui l'a précédée. */
    private void publierSansEchec(UUID id) {
        try {
            publier(id);
        } catch (Exception e) {
            log.warn("Message {} : publication immédiate impossible, le relais le reprendra : {}", id, e.getMessage());
        }
    }

    /** Publie un message s'il est encore à publier ; vrai s'il vient de l'être. */
    public boolean publier(UUID id) {
        JmsTemplate jms = courtier.getIfAvailable();
        if (jms == null) {
            return false;
        }
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        E message = transactionPropre.execute(s -> {
            int acquis = depot.revendiquer(id, StatutMessage.EN_COURS,
                    EnumSet.of(StatutMessage.A_PUBLIER, StatutMessage.EN_COURS),
                    maintenant, maintenant.plus(DELAI_DE_REPRISE));
            return acquis == 1 ? depot.findById(id).orElse(null) : null;
        });
        if (message == null) {
            return false;
        }
        try {
            // Hors transaction : un courtier lent ne retient ni connexion ni verrou en base.
            jms.send(message.getDestination(), session -> {
                TextMessage texte = session.createTextMessage(message.getContenu());
                texte.setStringProperty(AdressesMessagerie.PROPRIETE_TYPE, message.getType());
                texte.setStringProperty(AdressesMessagerie.PROPRIETE_IDENTIFIANT, id.toString());
                return texte;
            });
            transactionPropre.executeWithoutResult(s -> depot.findById(id).ifPresent(m -> {
                m.setStatut(StatutMessage.PUBLIE);
                m.setPublieLe(LocalDateTime.now(horloge));
                m.setDerniereErreur(null);
                // Publié, le message ne sera plus relu en base : une relivraison repart de la copie
                // du courtier. Un secret n'a donc plus de raison d'y rester.
                if (AdressesMessagerie.estConfidentiel(m.getType())) {
                    m.setContenu(CONTENU_EFFACE);
                }
                depot.save(m);
            }));
            return true;
        } catch (Exception e) {
            reporter(id, e);
            return false;
        }
    }

    private void reporter(UUID id, Exception cause) {
        String raison = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        transactionPropre.executeWithoutResult(s -> depot.findById(id).ifPresent(m -> {
            long attente = Math.min(ATTENTE_INITIALE_SECONDES << Math.min(Math.max(m.getTentatives() - 1, 0), 20),
                    ATTENTE_MAX_SECONDES);
            m.setStatut(StatutMessage.A_PUBLIER);
            m.setProchaineTentativeAt(LocalDateTime.now(horloge).plusSeconds(attente));
            m.setDerniereErreur(raison.length() > TAILLE_MAX_ERREUR ? raison.substring(0, TAILLE_MAX_ERREUR) : raison);
            depot.save(m);
            log.warn("Message {} vers {} : publication {} échouée, nouvel essai dans {} s : {}", id,
                    m.getDestination(), m.getTentatives(), attente, raison);
        }));
    }

    private String serialiser(Object contenu) {
        try {
            return json.writeValueAsString(contenu);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Message impossible à sérialiser : " + e.getOriginalMessage(), e);
        }
    }
}
