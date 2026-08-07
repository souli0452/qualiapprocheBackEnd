package com.qualiapproche.common.config;

import com.qualiapproche.common.utils.CourrielParGabarit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Envoi de courriels partagé entre les services.
 *
 * <p>Le bean est déclaré <b>ici</b> et non par une annotation sur la classe : chaque service ne
 * balaie de ce module que {@code com.qualiapproche.common.config}. Un {@code @Component} posé dans
 * {@code common.utils} n'était donc enregistré nulle part, et les services qui l'injectent
 * refusaient de démarrer — sans qu'aucun test unitaire ne puisse le voir, faute de démarrage de
 * contexte dans ce projet. {@code BeansPartagesVisiblesTest} garde désormais cette règle.</p>
 *
 * <p>La condition reprend celle sous laquelle Spring Boot construit le {@code JavaMailSender} :
 * l'hôte de messagerie renseigné. Sans elle, les quatre services qui n'envoient aucun courriel —
 * mais qui héritent du module {@code common}, donc de sa dépendance de messagerie — auraient exigé un
 * expéditeur inexistant et n'auraient plus démarré du tout.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class CourrielConfig {

    @Bean
    public CourrielParGabarit courrielParGabarit(JavaMailSender mailSender, MailConfig mailConfig) {
        return new CourrielParGabarit(mailSender, mailConfig);
    }
}
