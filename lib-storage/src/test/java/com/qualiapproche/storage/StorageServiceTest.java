package com.qualiapproche.storage;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fige la convention de rangement : c'est elle que les modules partagent, pas seulement le client.
 */
class StorageServiceTest {

    private MinioClient minioClient;
    private StorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        StorageProperties properties = new StorageProperties();
        properties.setBucketName("quali-sira");
        storageService = new StorageService(minioClient, properties);
    }

    @Test
    void rangeLeFichierSousLesDossiersDemandesEtConserveLExtension() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile(
                "file", "justificatif de rejet.pdf", "application/pdf", "contenu".getBytes());

        String objectName = storageService.uploadFile(fichier, "non-conformite", "DSI");

        assertThat(objectName).startsWith("NON_CONFORMITE/DSI/").endsWith(".pdf");
        assertThat(objectName).isEqualTo(argumentPutObject().object());
    }

    @Test
    void assainitLesSegmentsAccentuesOuEspaces() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("file", "note.docx", null, "x".getBytes());

        String objectName = storageService.uploadFile(fichier, "non-conformite", "Direction Générale");

        assertThat(objectName).startsWith("NON_CONFORMITE/DIRECTION_GENERALE/");
    }

    @Test
    void remplaceUnSegmentVideParUnDossierDeRepli() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("file", "note.docx", null, "x".getBytes());

        String objectName = storageService.uploadFile(fichier, "non-conformite", "   ");

        assertThat(objectName).startsWith("NON_CONFORMITE/DIVERS/");
    }

    @Test
    void deuxDepotsDuMemeNomNeSeRecouvrentPas() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("file", "rapport.pdf", null, "x".getBytes());

        String premier = storageService.uploadFile(fichier, "non-conformite", "DSI");
        String second = storageService.uploadFile(fichier, "non-conformite", "DSI");

        assertThat(premier).isNotEqualTo(second);
    }

    @Test
    void creeLeBucketLorsquIlManque() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        MockMultipartFile fichier = new MockMultipartFile("file", "note.pdf", null, "x".getBytes());

        storageService.uploadFile(fichier, "non-conformite", "DSI");

        verify(minioClient).makeBucket(any(io.minio.MakeBucketArgs.class));
    }

    private PutObjectArgs argumentPutObject() throws Exception {
        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        return captor.getValue();
    }
}
