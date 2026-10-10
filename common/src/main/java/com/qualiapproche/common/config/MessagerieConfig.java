package com.qualiapproche.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.messagerie.ConvertisseurDesMessages;
import com.qualiapproche.common.messagerie.LecteurDeMessages;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * La messagerie entre services (ActiveMQ Artemis) : messages JSON, consommateurs transactionnels.
 *
 * <p>Active dans les seuls services qui déclarent {@code spring-boot-starter-artemis} ; la
 * connexion au courtier vient de {@code spring.artemis.*}.</p>
 *
 * <ul>
 *   <li><b>Messages texte JSON</b>, typés par un <b>nom logique</b> ({@code _type} :
 *       « courriel », « rappel-circuit ») et non par un nom de classe : renommer ou déplacer un DTO
 *       ne rend pas illisibles les messages qui attendent dans les files ; un type inconnu est
 *       refusé ({@link ConvertisseurDesMessages}), et un message illisible est écarté sans être
 *       relivré ({@link LecteurDeMessages}) ;</li>
 *   <li><b>Session transactionnelle</b> : un message n'est acquitté qu'une fois son traitement
 *       terminé ; une exception le rend au courtier, qui le relivre avec une attente croissante,
 *       puis le range au rebut (DLQ) — voir docker/artemis/broker.properties.</li>
 * </ul>
 *
 * <p>La fabrique porte le nom {@code jmsListenerContainerFactory} : celle de Spring Boot s'efface
 * alors devant elle.</p>
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.springframework.jms.config.DefaultJmsListenerContainerFactory")
public class MessagerieConfig {

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new ConvertisseurDesMessages(objectMapper);
    }

    @Bean
    public LecteurDeMessages lecteurDeMessages(MessageConverter messageConverter) {
        return new LecteurDeMessages(messageConverter);
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connexions, MessageConverter messageConverter,
            @Value("${qualisira.messagerie.concurrence:1-4}") String concurrence) {
        DefaultJmsListenerContainerFactory fabrique = new DefaultJmsListenerContainerFactory();
        fabrique.setConnectionFactory(connexions);
        fabrique.setMessageConverter(messageConverter);
        fabrique.setSessionTransacted(true);
        fabrique.setConcurrency(concurrence);
        fabrique.setErrorHandler(e -> log.warn("Message rendu au courtier pour une nouvelle livraison : {}",
                e.getMessage()));
        return fabrique;
    }
}
