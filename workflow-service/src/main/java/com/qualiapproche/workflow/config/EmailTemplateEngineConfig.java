package com.qualiapproche.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Moteur de rendu des courriels de workflow.
 *
 * <p>Distinct du {@code templateEngine} auto-configuré par Spring, qui résout les gabarits par
 * <b>nom</b> depuis le classpath. Ici le corps du message vient de la base
 * ({@code workflow_email_template.body}) et non d'un fichier : un administrateur peut le modifier
 * par {@code PUT /api/v1/email-templates/{id}}, et rendre le fichier d'origine reviendrait à
 * ignorer silencieusement ses corrections.</p>
 *
 * <p>Les gabarits étaient écrits en Thymeleaf ({@code th:text}, {@code ${...}}) mais rendus par un
 * simple remplacement de marqueurs {@code {{clé}}} qu'aucun d'entre eux ne contient : aucune
 * variable n'était donc jamais substituée, et les messages partaient avec leurs attributs
 * {@code th:} non interprétés.</p>
 */
@Configuration
public class EmailTemplateEngineConfig {

    /** Nom du bean, à reprendre en {@code @Qualifier} : plusieurs {@link TemplateEngine} coexistent. */
    public static final String MOTEUR_GABARIT_EMAIL = "moteurGabaritEmail";

    /**
     * {@link SpringTemplateEngine} et non {@code TemplateEngine} nu : le premier évalue les
     * expressions en SpEL, le second en OGNL — absent du classpath, faute d'être tiré par
     * {@code spring-boot-starter-thymeleaf}. C'est aussi le dialecte du moteur auto-configuré et
     * celui dans lequel les gabarits sont écrits : en introduire un second dans la même
     * application ferait diverger le rendu selon l'émetteur.
     */
    @Bean(MOTEUR_GABARIT_EMAIL)
    public TemplateEngine moteurGabaritEmail() {
        StringTemplateResolver resolveur = new StringTemplateResolver();
        resolveur.setTemplateMode(TemplateMode.HTML);
        // Le corps sert lui-même de clé de cache : le conserver ferait grossir la table à chaque
        // modification d'un gabarit. L'analyse d'un courriel est négligeable devant l'envoi SMTP.
        resolveur.setCacheable(false);

        SpringTemplateEngine moteur = new SpringTemplateEngine();
        moteur.setTemplateResolver(resolveur);
        return moteur;
    }
}
