package com.qualiapproche.referentiel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.referentiel.client.UserClient;
import com.qualiapproche.referentiel.entities.AbonnementDirection;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.repository.AbonnementDirectionRepository;
import com.qualiapproche.referentiel.repository.StructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService implements CommandLineRunner {

    private final StructureRepository structureRepository;
    private final AbonnementDirectionRepository abonnementDirectionRepository;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting Tenant Provisioning...");

        ClassPathResource resource = new ClassPathResource("tenant-init.json");
        if (!resource.exists()) {
            log.warn("tenant-init.json not found, skipping provisioning.");
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode directions = root.get("directions");

            if (directions != null && directions.isArray()) {
                for (JsonNode dirNode : directions) {
                    provisionDirection(dirNode);
                }
            }
        }
    }

    private void provisionDirection(JsonNode node) {
        String libelleLong = node.get("libelleLong").asText();
        String libelleCourt = node.get("libelleCourt").asText();

        if (structureRepository.findByLibelleLong(libelleLong).isPresent()) {
            log.info("Direction {} already exists, updating license if necessary.", libelleLong);
            Structure existingDirection = structureRepository.findByLibelleLong(libelleLong).get();
            updateLicenseForExistingDirection(existingDirection, node);

            // Tenter de provisionner l'admin même si la direction existe
            JsonNode adminNode = node.get("superAdmin");
            if (adminNode != null) {
                try {
                    provisionSuperAdmin(existingDirection, adminNode);
                } catch (Exception e) {
                    log.warn("Non-critical error during Super Admin provisioning: {}.", e.getMessage());
                }
            }
            return;
        }

        log.info("Provisioning Direction: {}", libelleLong);

        Structure direction = Structure.builder()
                .libelleLong(libelleLong)
                .libelleCourt(libelleCourt)
                .email(node.get("email").asText())
                .typeStructure(TypeStructure.DIRECTION)
                .licenceActive(true)
                .build();

        direction = structureRepository.save(direction);

        // Provision Subscription (Encrypted)
        JsonNode modulesNode = node.get("modulesSubscribed");
        List<String> modules = new ArrayList<>();
        if (modulesNode != null && modulesNode.isArray()) {
            for (JsonNode m : modulesNode) {
                modules.add(m.asText());
            }
        }

        String modulesRaw = String.join(",", modules);
        String encryptedLicense = com.qualiapproche.common.utils.CryptoUtils.encrypt(modulesRaw);

        AbonnementDirection abonnement = AbonnementDirection.builder()
                .direction(direction)
                .license(encryptedLicense)
                .active(true)
                .build();

        if (node.has("dateDebutLicence")) {
            abonnement.setDateDebut(java.time.LocalDateTime.parse(node.get("dateDebutLicence").asText()));
        }
        if (node.has("dateFinLicence")) {
            abonnement.setDateFin(java.time.LocalDateTime.parse(node.get("dateFinLicence").asText()));
        }

        abonnementDirectionRepository.save(abonnement);
        log.info("License created and saved for direction: {}", direction.getLibelleLong());

        // Super Admin Info - ISOLATED to prevent rollback
        JsonNode adminNode = node.get("superAdmin");
        if (adminNode != null) {
            try {
                provisionSuperAdmin(direction, adminNode);
            } catch (Exception e) {
                log.warn("Non-critical error during Super Admin provisioning: {}. License remains active.", e.getMessage());
            }
        }
    }

    private void provisionSuperAdmin(Structure direction, JsonNode adminNode) {
        KcUserDto adminDto = KcUserDto.builder()
                .username(adminNode.get("username").asText())
                .firstName(adminNode.get("firstName").asText())
                .lastName(adminNode.get("lastName").asText())
                .email(adminNode.get("email").asText())
                .structure(direction.getId().toString())
                .roles(List.of("SUPER_ADMIN"))
                .enabled(true)
                .emailVerified(true)
                .build();

        try {
            log.info("Creating Super Admin {}...", adminDto.getUsername());
            userClient.createUser(adminDto);
            log.info("Super Admin created successfully.");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                log.info("Super Admin already exists in Keycloak. Skipping creation.");
            } else {
                log.error("Error creating Super Admin: {}", e.getMessage());
            }
        }
        }


    private void updateLicenseForExistingDirection(Structure direction, JsonNode node) {
        // Recherche de l'abonnement par UUID direct (robuste, basé sur subscribed_direction_id)
        // Double fallback : par UUID d'abord, puis par entité, puis création si rien trouvé
        AbonnementDirection abo = abonnementDirectionRepository.findByDirectionUUID(direction.getId())
                .orElseGet(() -> abonnementDirectionRepository.findByDirection(direction)
                        .orElseGet(() -> {
                            log.info("Aucun abonnement trouvé pour la direction '{}', création d'un nouvel abonnement.", direction.getLibelleLong());
                            return AbonnementDirection.builder()
                                    .direction(direction)
                                    .active(true)
                                    .build();
                        }));

        // Mise à jour systématique de toutes les données depuis tenant-init.json (update idempotent)
        if (node.has("dateDebutLicence")) {
            abo.setDateDebut(java.time.LocalDateTime.parse(node.get("dateDebutLicence").asText()));
        }
        if (node.has("dateFinLicence")) {
            abo.setDateFin(java.time.LocalDateTime.parse(node.get("dateFinLicence").asText()));
        }
        if (node.has("modulesSubscribed")) {
            List<String> modules = new java.util.ArrayList<>();
            for (JsonNode m : node.get("modulesSubscribed")) {
                modules.add(m.asText());
            }
            String modulesRaw = String.join(",", modules);
            String encryptedLicense = com.qualiapproche.common.utils.CryptoUtils.encrypt(modulesRaw);
            abo.setLicense(encryptedLicense);
            log.info("Modules mis à jour pour la direction '{}': {}", direction.getLibelleLong(), modules);
        }

        abonnementDirectionRepository.save(abo);
        log.info("Licence sauvegardée/mise à jour avec succès pour la direction: {}", direction.getLibelleLong());
    }
}
