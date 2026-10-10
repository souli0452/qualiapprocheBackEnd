package com.qualiapproche.common.messagerie;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La boîte d'envoi : déposer dans la transaction, publier après le commit, reprendre ce qui n'a
 * pas pu partir, ne jamais publier deux fois — et ne rien perdre quand le courtier est absent.
 */
class BoiteDEnvoiTest {

    static class Message extends AbstractMessageSortant { }

    @SuppressWarnings("unchecked")
    private final MessageSortantRepository<Message> depot = mock(MessageSortantRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JmsTemplate> courtier = mock(ObjectProvider.class);
    private final JmsTemplate jms = mock(JmsTemplate.class);
    private final Map<UUID, Message> base = new HashMap<>();
    private Instant maintenant = Instant.parse("2026-10-10T10:00:00Z");
    private BoiteDEnvoi<Message> boite;

    @BeforeEach
    void preparer() {
        Clock horloge = new Clock() {
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return maintenant; }
        };
        boite = new BoiteDEnvoi<>(depot, Message::new, new ObjectMapper(), courtier,
                mock(PlatformTransactionManager.class), horloge);
        when(courtier.getIfAvailable()).thenReturn(jms);
        when(depot.save(any(Message.class))).thenAnswer(i -> {
            Message m = i.getArgument(0);
            base.put(m.getId(), m);
            return m;
        });
        when(depot.findById(any())).thenAnswer(i -> Optional.ofNullable(base.get((UUID) i.getArgument(0))));
        when(depot.echus(any(), anyInt())).thenAnswer(i -> base.values().stream()
                .filter(m -> m.getStatut() != StatutMessage.PUBLIE)
                .filter(m -> !m.getProchaineTentativeAt().isAfter(i.getArgument(0)))
                .map(AbstractMessageSortant::getId).toList());
        when(depot.revendiquer(any(), any(), any(), any(), any())).thenAnswer(i -> {
            Message m = base.get((UUID) i.getArgument(0));
            if (m == null || m.getStatut() == StatutMessage.PUBLIE || m.getProchaineTentativeAt().isAfter(i.getArgument(3))) {
                return 0;
            }
            m.setStatut(StatutMessage.EN_COURS);
            m.setTentatives(m.getTentatives() + 1);
            m.setProchaineTentativeAt(i.getArgument(4));
            return 1;
        });
    }

    @AfterEach
    void fermer() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private LocalDateTime ici() {
        return LocalDateTime.ofInstant(maintenant, ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Dans une transaction, rien ne part avant le commit ; après, le message est publié avec son type et son identifiant")
    void publie_apres_le_commit() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        UUID id = boite.deposer("qualisira.test", "courriel", Map.of("sujet", "Bonjour"));
        verify(jms, never()).send(anyString(), any(MessageCreator.class));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        org.mockito.ArgumentCaptor<MessageCreator> createur = org.mockito.ArgumentCaptor.forClass(MessageCreator.class);
        verify(jms).send(eq("qualisira.test"), createur.capture());
        Session session = mock(Session.class);
        TextMessage texte = mock(TextMessage.class);
        when(session.createTextMessage(anyString())).thenReturn(texte);
        createur.getValue().createMessage(session);
        verify(session).createTextMessage("{\"sujet\":\"Bonjour\"}");
        verify(texte).setStringProperty(AdressesMessagerie.PROPRIETE_TYPE, "courriel");
        verify(texte).setStringProperty(AdressesMessagerie.PROPRIETE_IDENTIFIANT, id.toString());
        assertThat(base.get(id).getStatut()).isEqualTo(StatutMessage.PUBLIE);
    }

    @Test
    @DisplayName("Courtier injoignable : le message attend, l'attente double, il n'est jamais abandonné")
    void courtier_injoignable_rien_ne_se_perd() {
        doThrow(new UncategorizedJmsException("connexion refusée")).when(jms).send(anyString(), any(MessageCreator.class));
        UUID id = boite.deposer("qualisira.test", "courriel", Map.of());

        Message m = base.get(id);
        assertThat(m.getStatut()).isEqualTo(StatutMessage.A_PUBLIER);
        assertThat(m.getProchaineTentativeAt()).isEqualTo(ici().plusSeconds(2));
        assertThat(m.getDerniereErreur()).contains("connexion refusée");

        for (int i = 0; i < 30; i++) {
            maintenant = maintenant.plusSeconds(900);
            boite.tournee(100);
        }
        assertThat(base.get(id).getStatut()).isEqualTo(StatutMessage.A_PUBLIER);
        assertThat(base.get(id).getProchaineTentativeAt()).isEqualTo(ici().plusSeconds(900));
    }

    @Test
    @DisplayName("La tournée reprend ce qui attend ; un message déjà publié ne repart pas")
    void la_tournee_reprend_sans_doublon() {
        doThrow(new UncategorizedJmsException("absent")).when(jms).send(anyString(), any(MessageCreator.class));
        UUID id = boite.deposer("qualisira.test", "courriel", Map.of());
        org.mockito.Mockito.reset(jms);

        maintenant = maintenant.plusSeconds(2);
        assertThat(boite.tournee(100)).isEqualTo(1);
        assertThat(boite.tournee(100)).isZero();
        assertThat(boite.publier(id)).isFalse();
        verify(jms, times(1)).send(anyString(), any(MessageCreator.class));
    }

    @Test
    @DisplayName("Sans messagerie configurée, le message est conservé tel quel")
    void sans_messagerie_conserve() {
        when(courtier.getIfAvailable()).thenReturn(null);
        UUID id = boite.deposer("qualisira.test", "courriel", Map.of());

        assertThat(boite.tournee(100)).isZero();
        assertThat(base.get(id).getStatut()).isEqualTo(StatutMessage.A_PUBLIER);
        assertThat(base.get(id).getTentatives()).isZero();
    }

    @Test
    @DisplayName("Un courriel confidentiel part entier, puis son contenu est effacé de la boîte d'envoi")
    void confidentiel_efface_une_fois_publie() throws Exception {
        // Comme le vrai courtier, le message est composé pendant l'envoi — avant l'effacement.
        Session session = mock(Session.class);
        when(session.createTextMessage(anyString())).thenReturn(mock(TextMessage.class));
        org.mockito.Mockito.doAnswer(i -> ((MessageCreator) i.getArgument(1)).createMessage(session))
                .when(jms).send(anyString(), any(MessageCreator.class));

        UUID id = boite.deposer(AdressesMessagerie.COURRIELS, AdressesMessagerie.TYPE_COURRIEL_CONFIDENTIEL,
                Map.of("corps", "mot de passe : Xy7!"));

        verify(session).createTextMessage("{\"corps\":\"mot de passe : Xy7!\"}");
        assertThat(base.get(id).getStatut()).isEqualTo(StatutMessage.PUBLIE);
        assertThat(base.get(id).getContenu()).isEqualTo(BoiteDEnvoi.CONTENU_EFFACE);
    }

    @Test
    @DisplayName("Tant qu'il n'est pas parti, un courriel confidentiel garde son contenu : il faut pouvoir le republier")
    void confidentiel_garde_tant_qu_il_attend() {
        doThrow(new UncategorizedJmsException("absent")).when(jms).send(anyString(), any(MessageCreator.class));
        UUID id = boite.deposer(AdressesMessagerie.COURRIELS, AdressesMessagerie.TYPE_COURRIEL_CONFIDENTIEL,
                Map.of("corps", "secret"));

        assertThat(base.get(id).getContenu()).contains("secret");
    }

    @Test
    @DisplayName("Un courriel ordinaire garde son contenu une fois publié, pour le diagnostic")
    void ordinaire_garde_son_contenu() {
        UUID id = boite.deposer(AdressesMessagerie.COURRIELS, AdressesMessagerie.TYPE_COURRIEL, Map.of("corps", "Bonjour"));

        assertThat(base.get(id).getStatut()).isEqualTo(StatutMessage.PUBLIE);
        assertThat(base.get(id).getContenu()).contains("Bonjour");
    }
}
