package com.qualiapproche.referentiel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.common.utils.CryptoUtils;
import com.qualiapproche.referentiel.client.UserClient;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.repository.StructureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * D'où vient le repère auquel les licences sont confrontées.
 *
 * <p>Le contrôle du destinataire ne tient qu'à un fil : si le provisionnement ne pose pas le code
 * sur la direction, {@link CodeDeLInstallation} n'a rien à comparer et <b>toute</b> licence
 * authentique est acceptée — sans erreur, sans refus, sans que rien ne paraisse anormal à
 * l'usage. C'est la panne la plus coûteuse de ce dispositif, parce qu'elle est silencieuse.</p>
 *
 * <p>Ces cas gardent les deux chemins par lesquels le code arrive : la création d'une direction,
 * et le rattrapage d'une installation déjà en service — celle qui n'aurait jamais de repère
 * autrement, et qui est justement celle où il compte le plus.</p>
 */
class CodePartenairePoseAuProvisionnementTest {

    private StructureRepository structures;
    private TenantProvisioningService provisionnement;

    @BeforeEach
    void setUp() {
        structures = mock(StructureRepository.class);
        UserClient utilisateurs = mock(UserClient.class);
        provisionnement = new TenantProvisioningService(structures, utilisateurs, new ObjectMapper());

        when(structures.save(any(Structure.class))).thenAnswer(appel -> {
            Structure structure = appel.getArgument(0);
            if (structure.getId() == null) {
                structure.setId(UUID.randomUUID());
            }
            return structure;
        });
    }

    /** Le fichier de livraison, tel qu'il est réellement embarqué. */
    private JsonNode direction() throws Exception {
        try (InputStream flux = new ClassPathResource("tenant-init.json").getInputStream()) {
            return new ObjectMapper().readTree(flux).get("directions").get(0);
        }
    }

    private Structure enregistree() {
        ArgumentCaptor<Structure> capture = ArgumentCaptor.forClass(Structure.class);
        verify(structures).save(capture.capture());
        return capture.getValue();
    }

    @Test
    @DisplayName("Le fichier de livraison déclare un code : sans lui, le contrôle ne s'exerce pas")
    void fichierDeLivraison_declareUnCode() throws Exception {
        // Garde délibérément posée sur le fichier lui-même. Le retirer ne casserait rien de
        // visible : l'application démarrerait, les licences s'installeraient — et celle d'un
        // autre client aussi.
        JsonNode code = direction().get("code");

        assertThat(code).isNotNull();
        assertThat(code.asText()).isNotBlank();
    }

    @Test
    @DisplayName("À la création, la direction reçoit le code du fichier, obfusqué")
    void creation_poseLeCode() throws Exception {
        when(structures.findByLibelleLong(any())).thenReturn(Optional.empty());

        provisionnement.run();

        Structure direction = enregistree();
        assertThat(direction.getTypeStructure()).isEqualTo(TypeStructure.DIRECTION);
        // Obfusqué en base, en clair dans le fichier : un fichier de livraison doit se relire.
        assertThat(direction.getCodePartenaire()).isNotBlank()
                .isNotEqualTo(direction().get("code").asText());
        assertThat(CryptoUtils.decrypt(direction.getCodePartenaire()))
                .isEqualTo(direction().get("code").asText());
    }

    @Test
    @DisplayName("Une installation déjà en service reçoit son code au démarrage suivant")
    void directionExistante_rattrapeLeCode() throws Exception {
        // Sans ce rattrapage, une installation livrée avant ce dispositif n'aurait jamais de
        // repère, et n'exercerait aucun contrôle — là où il compte le plus.
        Structure existante = Structure.builder()
                .id(UUID.randomUUID())
                .libelleLong(direction().get("libelleLong").asText())
                .typeStructure(TypeStructure.DIRECTION)
                .build();
        when(structures.findByLibelleLong(any())).thenReturn(Optional.of(existante));

        provisionnement.run();

        assertThat(CryptoUtils.decrypt(enregistree().getCodePartenaire()))
                .isEqualTo(direction().get("code").asText());
    }

    @Test
    @DisplayName("Un code déjà posé n'est pas réécrit : il ne vient pas du fichier une fois là")
    void codeDejaPose_nestPasReecrit() throws Exception {
        Structure existante = Structure.builder()
                .id(UUID.randomUUID())
                .libelleLong(direction().get("libelleLong").asText())
                .typeStructure(TypeStructure.DIRECTION)
                .codePartenaire(CryptoUtils.encrypt("AUTRE-CODE"))
                .build();
        when(structures.findByLibelleLong(any())).thenReturn(Optional.of(existante));

        provisionnement.run();

        verify(structures, never()).save(any(Structure.class));
        assertThat(CryptoUtils.decrypt(existante.getCodePartenaire())).isEqualTo("AUTRE-CODE");
    }

    @Test
    @DisplayName("Le code posé est celui que le contrôle relira ensuite")
    void codePose_estCeluiQueLeControleAttend() throws Exception {
        when(structures.findByLibelleLong(any())).thenReturn(Optional.empty());
        provisionnement.run();
        Structure direction = enregistree();

        // Bout à bout : ce que le provisionnement écrit, CodeDeLInstallation doit savoir le
        // relire. Les deux emploient CryptoUtils, mais rien n'obligeait à ce qu'ils s'accordent.
        StructureRepository apres = mock(StructureRepository.class);
        when(apres.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of(direction));

        assertThat(new CodeDeLInstallation(apres).attendu())
                .isEqualTo(direction().get("code").asText());
    }
}
