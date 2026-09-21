package com.qualiapproche.ia.service;

import com.qualiapproche.ia.config.IaAssistantProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Le décompte des jetons quand le fournisseur ne le rapporte pas.
 *
 * <p>Ollama ne renseigne pas toujours l'usage. Ces appels comptaient alors zéro, et le plafond
 * quotidien ne s'appliquait jamais sur une installation auto-hébergée — le garde-fou manquait là
 * où il protège le mieux, puisqu'un modèle local ne se facture pas mais se paie en processeur.</p>
 */
class EstimationDesJetonsTest {

    private ClientDuModele client;
    private IaAssistantProperties proprietes;

    @BeforeEach
    void preparer() {
        proprietes = new IaAssistantProperties();
        client = new ClientDuModele(mock(ChatClient.class, RETURNS_DEEP_STUBS), proprietes);
    }

    private ChatResponse avecUsage(Integer total) {
        Generation g = new Generation(new AssistantMessage("réponse"));
        if (total == null) {
            return new ChatResponse(List.of(g));
        }
        return new ChatResponse(List.of(g), ChatResponseMetadata.builder()
                .usage(new DefaultUsage(0, 0, total)).build());
    }

    @Test
    @DisplayName("l'usage rapporté par le fournisseur fait foi")
    void usageRapporte_faitFoi() {
        assertThat(client.jetonsOuEstimation(avecUsage(137), "consigne et question", "réponse"))
                .isEqualTo(137L);
    }

    @Test
    @DisplayName("sans usage rapporté, une estimation prend le relais — et jamais zéro")
    void sansUsage_uneEstimationPrendLeRelais() {
        String envoye = "x".repeat(400);
        String rendu = "y".repeat(400);

        long estime = client.jetonsOuEstimation(avecUsage(null), envoye, rendu);

        // 800 caractères, quatre par jeton : deux cents.
        assertThat(estime).isEqualTo(200L);
    }

    @Test
    @DisplayName("un usage à zéro est traité comme absent : c'est ce que rend un fournisseur muet")
    void usageAZero_estTraiteCommeAbsent() {
        assertThat(client.jetonsOuEstimation(avecUsage(0), "x".repeat(40), "y".repeat(40)))
                .isEqualTo(20L);
    }

    @Test
    @DisplayName("un appel minuscule compte au moins un jeton : jamais de ligne gratuite")
    void appelMinuscule_compteAuMoinsUnJeton() {
        assertThat(client.jetonsOuEstimation(avecUsage(null), "", "")).isEqualTo(1L);
    }

    @Test
    @DisplayName("le rapport caractères/jeton se règle : une langue dense se déclare")
    void leRapport_seRegle() {
        proprietes.setCaracteresParJeton(2);

        assertThat(client.jetonsOuEstimation(avecUsage(null), "x".repeat(100), "")).isEqualTo(50L);
    }
}
