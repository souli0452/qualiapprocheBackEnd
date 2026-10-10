package com.qualiapproche.referentiel.messagerie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.messagerie.BoiteDEnvoi;
import com.qualiapproche.common.messagerie.Courriels;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** La boîte d'envoi du service, et l'envoi de courriels qui passe par elle. */
@Configuration
public class MessagerieDuServiceConfig {

    @Bean
    public BoiteDEnvoi<MessageSortant> boiteDEnvoi(MessageSortantRepository depot, ObjectMapper objectMapper,
                                                   ObjectProvider<JmsTemplate> courtier,
                                                   PlatformTransactionManager transactions) {
        return new BoiteDEnvoi<>(depot, MessageSortant::new, objectMapper, courtier, transactions);
    }

    @Bean
    public Courriels courriels(BoiteDEnvoi<MessageSortant> boiteDEnvoi) {
        return new Courriels(boiteDEnvoi);
    }
}
