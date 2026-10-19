package com.qualiapproche.storage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le dépôt de fichiers ne se câble qu'avec son adresse et ses deux clés : des clés vides faisaient
 * tomber le service entier au démarrage, le client MinIO refusant d'être construit sans elles.
 */
class StorageAutoConfigurationTest {

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(StorageAutoConfiguration.class));

    @Test
    void adresseEtClesRenseignees_leDepotEstCable() {
        contexte.withPropertyValues("minio.url=https://s3.exemple.test", "minio.access-key=cle",
                        "minio.secret-key=secret", "minio.bucket-name=quali-sira")
                .run(ctx -> assertThat(ctx).hasNotFailed().hasSingleBean(StorageService.class));
    }

    @Test
    void clesVides_leServiceDemarreSansDepot() {
        contexte.withPropertyValues("minio.url=https://s3.exemple.test", "minio.access-key=",
                        "minio.secret-key=")
                .run(ctx -> assertThat(ctx).hasNotFailed().doesNotHaveBean(StorageService.class));
    }

    @Test
    void sansAdresse_aucunDepot() {
        contexte.run(ctx -> assertThat(ctx).hasNotFailed().doesNotHaveBean(StorageService.class));
    }
}
