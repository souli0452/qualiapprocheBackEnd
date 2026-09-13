package com.qualiapproche.ia.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Assemblage du client de dialogue avec le modèle.
 *
 * <p>Le {@code OpenAiChatModel} vient de l'auto-configuration du starter (base-url, clé et modèle
 * issus de {@code spring.ai.openai}) ; on ne pose ici que les bornes de génération de l'assistant,
 * en options par défaut pour que chaque appel en hérite sans les répéter.</p>
 */
@Configuration
@EnableConfigurationProperties(IaAssistantProperties.class)
public class IaChatConfig {

    /** Une connexion au fournisseur doit s'établir vite : borne haute du délai de connexion. */
    private static final int DELAI_CONNEXION_MAX_MS = 10_000;

    @Bean
    public ChatClient chatClient(OpenAiChatModel modele, IaAssistantProperties proprietes) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .maxTokens(proprietes.getMaxTokens())
                .temperature(proprietes.getTemperature())
                .build();
        return ChatClient.builder(modele).defaultOptions(options).build();
    }

    /**
     * Borne de temps sur les appels HTTP au fournisseur de modèle.
     *
     * <p>L'auto-configuration du starter construit son client HTTP à partir du
     * {@code RestClient.Builder} de Spring Boot, auquel ce personnalisateur s'applique : pas de
     * client reconstruit à la main, seulement une usine de connexions avec des délais explicites.
     * Sans eux, un fournisseur qui ne répond plus retient le fil de la requête jusqu'au délai du
     * système — des minutes — et la passerelle elle-même aura déjà rendu son propre délai. La
     * lecture est bornée par {@code ia.assistant.timeout-ms}, la connexion par le plus petit de ce
     * délai et de dix secondes : établir une connexion n'a pas à attendre 45 secondes.</p>
     */
    @Bean
    public RestClientCustomizer delaisAppelModele(IaAssistantProperties proprietes) {
        SimpleClientHttpRequestFactory usine = new SimpleClientHttpRequestFactory();
        usine.setReadTimeout((int) proprietes.getTimeoutMs());
        usine.setConnectTimeout((int) Math.min(proprietes.getTimeoutMs(), DELAI_CONNEXION_MAX_MS));
        return constructeur -> constructeur.requestFactory(usine);
    }
}
