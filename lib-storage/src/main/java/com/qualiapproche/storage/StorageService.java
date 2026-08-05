package com.qualiapproche.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dépôt de fichiers sur le serveur d'objets, commun à tous les micro-services.
 *
 * <p>La consigne est que <b>tous</b> les fichiers de la plateforme y aillent : chaque module qui
 * en manipule ajoute la dépendance {@code lib-storage} et reçoit ce service, au lieu de recopier
 * un client MinIO — c'est ainsi que la convention de rangement, l'assainissement des noms de
 * dossier et la création du bucket restent identiques d'un module à l'autre.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class StorageService {

    /** Segment de repli lorsqu'un libellé de dossier est vide ou illisible. */
    private static final String DOSSIER_PAR_DEFAUT = "DIVERS";

    private final MinioClient minioClient;
    private final StorageProperties properties;

    /**
     * Dépose un fichier en le rangeant sous {@code dossiers[0]/dossiers[1]/…/}.
     *
     * <p>MinIO (compatible S3) n'a pas de vraie notion de répertoire : ce n'est qu'un préfixe dans
     * la clé de l'objet — aucun appel de création de dossier n'est nécessaire, le « dossier »
     * apparaît dès qu'un premier fichier y est déposé.</p>
     *
     * <p>Le nom d'origine n'est pas conservé dans la clé — deux fichiers homonymes se seraient
     * écrasés. Seule l'extension est reprise, pour que le téléchargement propose le bon type.</p>
     *
     * @param file     fichier reçu
     * @param dossiers segments de rangement, du plus général au plus précis
     *                 (ex. {@code "non-conformite", sigleStructure})
     * @return le nom d'objet complet, préfixes compris, à conserver pour le téléchargement
     *         et la suppression
     */
    public String uploadFile(MultipartFile file, String... dossiers) throws Exception {
        createBucketIfNotExist();

        String objectName = prefixe(dossiers) + UUID.randomUUID() + extension(file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType(file))
                            .build()
            );
            log.info("Fichier déposé sur le serveur d'objets : {}", objectName);
            return objectName;
        }
    }

    /**
     * Dépose un contenu déjà en mémoire, quand le fichier n'arrive pas en multipart.
     *
     * <p>Les écrans qui envoient encore leurs pièces jointes encodées en base64 dans le corps de
     * la requête passent par ici : le contenu rejoint le serveur d'objets comme les autres, sans
     * qu'il faille d'abord reprendre l'écran. Même convention de rangement, même nommage.</p>
     *
     * @param contenu     octets du fichier
     * @param nomOriginal nom d'origine, dont seule l'extension est reprise dans la clé
     * @param contentType type MIME déclaré, {@code null} accepté
     * @param dossiers    segments de rangement, du plus général au plus précis
     * @return le nom d'objet complet, préfixes compris
     */
    public String uploadContent(byte[] contenu, String nomOriginal, String contentType, String... dossiers)
            throws Exception {
        createBucketIfNotExist();

        String objectName = prefixe(dossiers) + UUID.randomUUID() + extension(nomOriginal);

        try (InputStream inputStream = new ByteArrayInputStream(contenu)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, contenu.length, -1)
                            .contentType(typeOuDefaut(contentType))
                            .build()
            );
            log.info("Contenu déposé sur le serveur d'objets : {}", objectName);
            return objectName;
        }
    }

    /**
     * Télécharge un fichier. L'appelant referme le flux.
     */
    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(properties.getBucketName())
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Supprime un fichier.
     */
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(properties.getBucketName())
                        .object(objectName)
                        .build()
        );
        log.info("Fichier supprimé du serveur d'objets : {}", objectName);
    }

    private String prefixe(String... dossiers) {
        if (dossiers == null || dossiers.length == 0) {
            return "";
        }
        return Arrays.stream(dossiers)
                .map(this::sanitizeFolderSegment)
                .collect(Collectors.joining("/")) + "/";
    }

    /**
     * Type MIME à déclarer, jamais nul : le client MinIO refuse un type vide en levant une
     * {@code IllegalArgumentException}. Or un fichier peut très bien arriver sans type déclaré —
     * un client qui ne le renseigne pas, une pièce jointe reconstruite côté serveur — et le dépôt
     * échouait alors sur un détail de transport plutôt que sur le contenu.
     */
    private String contentType(MultipartFile file) {
        return typeOuDefaut(file.getContentType());
    }

    private String typeOuDefaut(String declare) {
        return declare == null || declare.isBlank() ? "application/octet-stream" : declare;
    }

    private String extension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    /**
     * Nettoie un libellé (service, type de document, sigle de structure…) pour en faire un segment
     * de chemin sûr : accents retirés, espaces et caractères spéciaux remplacés, toujours non vide.
     */
    private String sanitizeFolderSegment(String raw) {
        if (raw == null || raw.isBlank()) {
            return DOSSIER_PAR_DEFAUT;
        }
        String withoutAccents = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String cleaned = withoutAccents.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return cleaned.isBlank() ? DOSSIER_PAR_DEFAUT : cleaned;
    }

    private void createBucketIfNotExist() throws Exception {
        String bucket = properties.getBucketName();
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket {} créé avec succès", bucket);
        }
    }
}
