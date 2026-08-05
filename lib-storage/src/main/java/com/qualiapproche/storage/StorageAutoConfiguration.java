package com.qualiapproche.storage;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Câblage automatique du dépôt de fichiers.
 *
 * <p>Les applications du dépôt déclarent un {@code scanBasePackages} explicite : une simple
 * {@code @Configuration} dans {@code com.qualiapproche.storage} ne serait vue par personne, et
 * chaque service aurait dû élargir son balayage. Passer par une auto-configuration enregistrée
 * dans {@code AutoConfiguration.imports} suffit : ajouter la dépendance {@code lib-storage}
 * apporte le service, sans une ligne de configuration côté hôte.</p>
 *
 * <p>Le câblage est conditionné à la présence de {@code minio.url} — un service qui ne dépose
 * aucun fichier peut donc porter la dépendance sans avoir à renseigner de coordonnées.</p>
 */
@AutoConfiguration
@ConditionalOnClass(MinioClient.class)
@ConditionalOnProperty(prefix = "minio", name = "url")
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getUrl())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public StorageService storageService(MinioClient minioClient, StorageProperties properties) {
        return new StorageService(minioClient, properties);
    }
}
