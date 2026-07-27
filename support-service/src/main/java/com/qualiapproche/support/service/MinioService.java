package com.qualiapproche.support.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Upload un fichier dans Minio en le rangeant sous {@code {processusFolder}/{typeDocumentFolder}/}.
     * MinIO (compatible S3) n'a pas de vraie notion de répertoire : ce n'est qu'un préfixe dans la
     * clé de l'objet — aucun appel de création de dossier n'est nécessaire, le "dossier" apparaît
     * dès qu'un premier fichier y est déposé.
     *
     * @param processusFolder     dossier de premier niveau (ex. le service/processus propriétaire du document)
     * @param typeDocumentFolder  dossier de second niveau (ex. {@link com.qualiapproche.support.model.QmsDocumentType#getFolderName()})
     * @return le nom d'objet complet (avec préfixes de dossier), à conserver pour le téléchargement/suppression.
     */
    public String uploadFile(MultipartFile file, String processusFolder, String typeDocumentFolder) throws Exception {
        createBucketIfNotExist();

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String objectName = sanitizeFolderSegment(processusFolder) + "/" + sanitizeFolderSegment(typeDocumentFolder)
                + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Fichier uploadé avec succès sur Minio : {}", objectName);
            return objectName;
        }
    }

    /**
     * Nettoie un libellé (service, type de document...) pour en faire un segment de chemin
     * sûr : accents retirés, espaces/caractères spéciaux remplacés, toujours non vide.
     */
    private String sanitizeFolderSegment(String raw) {
        if (raw == null || raw.isBlank()) {
            return "DIVERS";
        }
        String withoutAccents = java.text.Normalizer.normalize(raw.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String cleaned = withoutAccents.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return cleaned.isBlank() ? "DIVERS" : cleaned;
    }

    /**
     * Télécharge un fichier depuis Minio.
     */
    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Supprime un fichier de Minio.
     */
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        log.info("Fichier supprimé de Minio : {}", objectName);
    }

    private void createBucketIfNotExist() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Bucket {} créé avec succès", bucketName);
        }
    }
}
