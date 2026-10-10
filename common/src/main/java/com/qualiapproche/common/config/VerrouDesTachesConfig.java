package com.qualiapproche.common.config;

import org.springframework.core.Ordered;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Une tâche planifiée ne s'exécute qu'une fois, même quand plusieurs instances d'un service
 * tournent.
 *
 * <p>Sans verrou, chaque instance déroulait sa propre tournée : deux réplicas envoyaient deux fois
 * chaque relance, chaque alerte de révision, chaque courriel de licence. Le verrou est une ligne de
 * la table {@code shedlock}, dans la base du service ; chaque tâche planifiée le prend par son nom
 * ({@code @SchedulerLock}), et l'instance qui ne l'obtient pas passe son tour.</p>
 *
 * <p>L'heure est celle de la base ({@code usingDbTime}) et non celle de chaque instance : deux
 * conteneurs dont les horloges divergent ne peuvent pas tenir le verrou en même temps.</p>
 *
 * <p>L'ordre place le verrou <b>autour</b> de la transaction d'une tâche {@code @Transactional} : il
 * est pris avant elle et rendu après son commit.</p>
 *
 * <p>{@code defaultLockAtMostFor} borne un verrou orphelin : une instance arrêtée en pleine tâche ne
 * bloque pas les autres au-delà de trente minutes. Chaque tâche précise sa propre borne.</p>
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M", order = Ordered.LOWEST_PRECEDENCE - 100)
public class VerrouDesTachesConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    }
}
