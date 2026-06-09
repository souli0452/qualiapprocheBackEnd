package com.qualiapproche.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

/**
 * Configuration Feign appliquée globalement à tous les clients Feign du user-service.
 * IMPORTANT : cette classe ne doit PAS être annotée @Configuration car elle est enregistrée
 * via @EnableFeignClients(defaultConfiguration = ...) dans le contexte enfant Feign.
 */
public class FeignConfig {

    @Bean
    public Decoder feignDecoder(@Autowired ObjectMapper objectMapper) {
        return new ApiResponseFeignDecoder(objectMapper);
    }
}
