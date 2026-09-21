package com.qualiapproche.ia.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.ia.config.IaAssistantProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ce que le service dit à l'exploitant quand le fournisseur refuse ou se tait.
 *
 * <p>Ces cas tiennent à une découverte faite en production : le module rendait 503 avec une pile
 * de cent lignes pour un simple 429 de quota, et l'exploitant cherchait une panne du service là où
 * le fournisseur disait pourtant clairement pourquoi il refusait. La cause : Spring AI n'émet pas
 * les exceptions du client HTTP, il les enveloppe dans les siennes, que les tests de type ne
 * reconnaissaient pas.</p>
 *
 * <p>C'est donc le <b>journal</b> qu'on éprouve ici, pas seulement le statut rendu : les deux
 * branches répondaient déjà 503, et seul ce qui s'écrit dans les traces distingue un diagnostic
 * d'un aveu d'ignorance.</p>
 */
class PannesDuFournisseurTest {

    private ChatClient chatClient;
    private ClientDuModele client;
    private ListAppender<ILoggingEvent> journal;
    private Logger logger;

    @BeforeEach
    void preparer() {
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        IaAssistantProperties proprietes = new IaAssistantProperties();
        client = new ClientDuModele(chatClient, proprietes);

        logger = (Logger) LoggerFactory.getLogger(ClientDuModele.class);
        journal = new ListAppender<>();
        journal.start();
        logger.addAppender(journal);
    }

    @AfterEach
    void ranger() {
        logger.detachAppender(journal);
    }

    private BusinessException echecSur(RuntimeException panne) {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().chatResponse())
                .thenThrow(panne);
        return (BusinessException) org.assertj.core.api.Assertions
                .catchThrowable(() -> client.repondre("consigne", "message"));
    }

    @Test
    @DisplayName("un refus du fournisseur (4xx) est nommé, sans pile d'appels")
    void refusDuFournisseur_estNomme() {
        BusinessException rendue = echecSur(
                new NonTransientAiException("401 - Unauthorized: invalid api key"));

        assertThat(rendue.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(journal.list).noneMatch(evenement -> evenement.getLevel() == Level.ERROR);
        assertThat(journal.list).anyMatch(evenement ->
                evenement.getLevel() == Level.WARN
                        && evenement.getFormattedMessage().contains("Unauthorized"));
    }

    @Test
    @DisplayName("un quota atteint (429) est nommé lui aussi")
    void quotaAtteint_estNomme() {
        BusinessException rendue = echecSur(
                new TransientAiException("429 - Rate limit exceeded"));

        assertThat(rendue.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(journal.list).noneMatch(evenement -> evenement.getLevel() == Level.ERROR);
        assertThat(journal.list).anyMatch(evenement ->
                evenement.getFormattedMessage().contains("Rate limit exceeded"));
    }

    @Test
    @DisplayName("un statut HTTP enfoui dans les causes est lu, non ignoré")
    void statutEnfouiDansLesCauses_estLu() {
        RestClientResponseException refus = new RestClientResponseException(
                "refus", 429, "Too Many Requests", null, null, null);
        BusinessException rendue = echecSur(new RestClientException("appel au modèle", refus));

        assertThat(rendue.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(journal.list).anyMatch(evenement ->
                evenement.getFormattedMessage().contains("429"));
    }

    @Test
    @DisplayName("un fournisseur injoignable est distingué d'un refus, même enfoui")
    void fournisseurInjoignable_estDistingue() {
        BusinessException rendue = echecSur(new RestClientException("échec",
                new ResourceAccessException("connexion refusée")));

        assertThat(rendue.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(journal.list).anyMatch(evenement ->
                evenement.getFormattedMessage().contains("injoignable"));
    }

    @Test
    @DisplayName("un modèle trop lent rend 504, non 503 : deux situations à ne pas confondre")
    void modeleTropLent_rend504() {
        BusinessException rendue = echecSur(new RestClientException(
                "Error while extracting response",
                new RestClientException("lecture", new SocketTimeoutException("Read timed out"))));

        assertThat(rendue.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    @DisplayName("une erreur vraiment imprévue garde sa pile : c'est là qu'elle sert")
    void erreurImprevue_gardeSaPile() {
        BusinessException rendue = echecSur(new IllegalStateException("bean absent"));

        assertThat(rendue.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(journal.list).anyMatch(evenement ->
                evenement.getLevel() == Level.ERROR
                        && evenement.getThrowableProxy() != null);
    }
}
