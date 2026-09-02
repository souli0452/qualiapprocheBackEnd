package com.qualiapproche.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Coordonnées du serveur d'objets.
 *
 * <p>Le préfixe reste {@code minio} : c'est celui que les déploiements existants renseignent
 * ({@code MINIO_URL}, {@code MINIO_BUCKET_NAME}…), et le changer aurait fait perdre leur
 * configuration aux services déjà en production.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "minio")
public class StorageProperties {

    /** URL du serveur compatible S3. */
    private String url;

    public String getUrl() {
        if (url != null && url.contains(":9001")) {
            return url.replace(":9001", ":9000");
        }
        return url;
    }

    /** Identifiant d'accès. */
    private String accessKey;

    /** Clé secrète. */
    private String secretKey;

    /** Bucket dans lequel tous les fichiers de la plateforme sont déposés. */
    private String bucketName;
}
