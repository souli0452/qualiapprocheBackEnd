package com.qualiapproche.common.cloisonnement;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * Le cloisonnement par direction, posé sur chaque service qui a une base.
 *
 * <p>Auto-configuration plutôt que composant : workflow-service n'analyse pas
 * {@code com.qualiapproche.common.config}, et un service qui échapperait au cloisonnement le
 * ferait sans le dire.</p>
 */
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration")
@ConditionalOnClass(name = {"org.hibernate.context.spi.CurrentTenantIdentifierResolver",
        "org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer"})
public class CloisonnementAutoConfiguration {

    @Bean
    public HibernatePropertiesCustomizer resolveurDeDirection() {
        return proprietes -> proprietes.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                new ResolveurDeDirection());
    }

    /** Tous les dépôts prennent {@link DepotCloisonne} pour base, sans retoucher chaque application. */
    @Bean
    public static BeanPostProcessor depotsCloisonnes() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String nom) throws BeansException {
                if (bean instanceof org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean<?, ?, ?> fabrique) {
                    fabrique.setRepositoryBaseClass(DepotCloisonne.class);
                }
                return bean;
            }
        };
    }

    /*
     * Sans @ConditionalOnBean : évaluée avant que la configuration JPA ait déclaré son
     * gestionnaire de transactions, la condition écartait ces beans partout, et referentiel-service
     * refusait de démarrer. Ils ne résolvent leurs dépendances qu'à l'usage.
     */
    @Bean
    public ExecutionCloisonnee executionCloisonnee(ObjectProvider<PlatformTransactionManager> transactions) {
        return new ExecutionCloisonnee(transactions);
    }

    @Bean
    public AmorceDesDirections amorceDesDirections(ObjectProvider<Amorce> amorces, ExecutionCloisonnee execution) {
        return new AmorceDesDirections(amorces, execution);
    }

    @Bean
    public RattrapageDesDirections rattrapageDesDirections(ObjectProvider<EntityManagerFactory> fabrique,
                                                           ExecutionCloisonnee execution) {
        return new RattrapageDesDirections(fabrique, execution);
    }
}
