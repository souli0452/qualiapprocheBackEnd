package com.qualiapproche.support.courriel;

import com.qualiapproche.common.courriel.FileDeCourriels;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.PlatformTransactionManager;

/** La file des courriels du service : ils sont déposés en base, puis remis par la tournée. */
@Configuration
public class CourrielsConfig {

    @Bean
    public FileDeCourriels<CourrielSortant> fileDeCourriels(CourrielSortantRepository depot,
                                                             ObjectProvider<JavaMailSender> messagerie,
                                                             @Value("${spring.mail.username:}") String expediteur,
                                                             PlatformTransactionManager transactions) {
        return new FileDeCourriels<>(depot, CourrielSortant::new, messagerie, expediteur, transactions);
    }
}
