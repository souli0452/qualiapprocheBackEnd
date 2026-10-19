package com.qualiapproche.storage;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Le dépôt de fichiers ne se câble que si son adresse <b>et</b> ses deux clés sont renseignées.
 *
 * <p>La seule présence de {@code minio.url} ne suffisait pas : un service où le serveur de fichiers
 * est facultatif (evaluation, referentiel) déclare ses clés avec un défaut vide pour démarrer sans
 * elles, et le client MinIO refuse d'être construit sur des clés vides — le service entier tombait
 * au démarrage. Ici, faute de clés, aucun dépôt n'est câblé : le service démarre, et seul le
 * chargement d'un fichier est refusé, avec un message qui dit ce qui manque.</p>
 */
class StockageRenseigne implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return StringUtils.hasText(env.getProperty("minio.url"))
                && StringUtils.hasText(env.getProperty("minio.access-key"))
                && StringUtils.hasText(env.getProperty("minio.secret-key"));
    }
}
