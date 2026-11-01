package com.qualiapproche.referentiel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.common.utils.CryptoUtils;
import com.qualiapproche.referentiel.client.UserClient;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.plateforme.GestionDesClients;
import com.qualiapproche.referentiel.repository.StructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Amorçage d'une nouvelle installation : la direction et son super administrateur.
 *
 * <p>Ce fichier ne décide rien de ce qui est permis. Il a longtemps porté aussi la licence —
 * dates et modules souscrits, chiffrés avec une clé livrée dans le produit : n'importe quel
 * détenteur du code pouvait s'accorder tous les modules jusqu'en 2099 en éditant un JSON. La
 * licence est désormais signée par l'éditeur et posée par un administrateur
 * ({@link LicenceInstalleeService}) ; une installation neuve démarre donc sans licence, et
 * propose l'essai gratuit.</p>
 *
 * <p>Une installation sert désormais plusieurs clients : chaque entrée de {@code directions} est
 * un client, dont la direction devient sa propre direction ({@code direction_id = id}). Ses
 * comptes ne sont créés qu'une fois son paramétrage amorcé dans user-service — sans quoi son
 * SUPER_ADMIN n'aurait pas de rôle — ce que {@code SynchronisationDesClients} fait dès que les
 * services répondent ({@link #creerLesComptesEnAttente}). Le nœud {@code plateforme.administrateurs}
 * désigne les comptes qui créent les clients, sans appartenir à aucun.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService implements CommandLineRunner {

    private final StructureRepository structureRepository;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    /** Un compte décrit par le fichier, à créer quand user-service pourra le recevoir. */
    private record ComptePrevu(UUID direction, KcUserDto compte) {
    }

    private final Queue<ComptePrevu> enAttente = new ConcurrentLinkedQueue<>();

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
            JsonNode administrateurs = root.path("plateforme").path("administrateurs");
            if (administrateurs.isArray()) {
                administrateurs.forEach(admin -> enAttente.add(new ComptePrevu(null, compte(admin, null))));
            }
        }
    }

    private void provisionDirection(JsonNode node) {
        String libelleLong = node.get("libelleLong").asText();
        String libelleCourt = node.get("libelleCourt").asText();

        // Une direction racine de ce nom : une structure homonyme chez un autre client n'en est pas une.
        Structure direction = structureRepository.findByLibelleLong(libelleLong)
                .filter(s -> s.getTypeStructure() == TypeStructure.DIRECTION && s.getDirection() == null)
                .orElse(null);

        if (direction != null) {
            log.info("Direction {} already exists, skipping creation.", libelleLong);
            // Le code, lui, est reposé si elle n'en a pas : une installation déjà en service
            // n'aurait sinon jamais de repère, et n'exercerait aucun contrôle du destinataire —
            // précisément là où il compte le plus.
            poserLeCodeSiAbsent(direction, node);
        } else {
            log.info("Provisioning Direction: {}", libelleLong);
            direction = structureRepository.save(Structure.builder()
                    .libelleLong(libelleLong)
                    .libelleCourt(libelleCourt)
                    .email(node.get("email").asText())
                    .typeStructure(TypeStructure.DIRECTION)
                    .licenceActive(true)
                    .codePartenaire(codeObfusque(node))
                    .build());
        }
        // Le client est sa propre direction : c'est elle qui cloisonne tout ce qui lui appartient.
        structureRepository.faireSaPropreDirection(direction.getId());

        JsonNode adminNode = node.get("superAdmin");
        if (adminNode != null) {
            KcUserDto superAdmin = compte(adminNode, direction.getId());
            superAdmin.setRoles(List.of("SUPER_ADMIN"));
            enAttente.add(new ComptePrevu(direction.getId(), superAdmin));
        }
    }

    /**
     * Le code du partenaire, tel qu'il est rangé en base : obfusqué.
     *
     * <p>Il s'écrit en clair dans {@code tenant-init.json} — un fichier de livraison doit se
     * relire — et n'est chiffré qu'en base. Sur ce que cette obfuscation vaut, voir
     * {@link CodeDeLInstallation} : la clé étant livrée avec le produit, elle décourage la
     * retouche désinvolte d'une colonne, elle ne l'interdit pas.</p>
     *
     * <p>Absent, rien n'est posé : l'installation n'exerce alors aucun contrôle du destinataire,
     * et c'est le démarrage qui le dit.</p>
     */
    private String codeObfusque(JsonNode node) {
        JsonNode code = node.get("code");
        if (code == null || code.asText().isBlank()) {
            log.warn("Aucun code partenaire dans tenant-init.json : cette installation acceptera "
                    + "toute licence authentique, y compris celle d'un autre client.");
            return null;
        }
        return CryptoUtils.encrypt(code.asText().trim());
    }

    private void poserLeCodeSiAbsent(Structure direction, JsonNode node) {
        if (direction.getCodePartenaire() != null && !direction.getCodePartenaire().isBlank()) {
            return;
        }
        String obfusque = codeObfusque(node);
        if (obfusque == null) {
            return;
        }
        direction.setCodePartenaire(obfusque);
        structureRepository.save(direction);
        log.info("Code partenaire posé sur la direction {} : les licences y seront désormais "
                + "confrontées.", direction.getLibelleLong());
    }

    private static KcUserDto compte(JsonNode noeud, UUID direction) {
        return KcUserDto.builder()
                .username(noeud.get("username").asText())
                .firstName(noeud.path("firstName").asText(""))
                .lastName(noeud.path("lastName").asText(""))
                .email(noeud.get("email").asText())
                .structure(direction == null ? null : direction.toString())
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    /**
     * Crée les comptes décrits par le fichier, une fois les clients amorcés.
     *
     * @return vrai s'il n'en reste aucun à créer
     */
    public boolean creerLesComptesEnAttente(GestionDesClients gestion) {
        int aTraiter = enAttente.size();
        for (int i = 0; i < aTraiter; i++) {
            ComptePrevu prevu = enAttente.poll();
            if (prevu == null) {
                break;
            }
            boolean fait;
            if (prevu.direction() == null) {
                fait = gestion.creerUnAdministrateurDePlateforme(prevu.compte());
            } else {
                Object issue = gestion.creerLeSuperAdmin(prevu.direction(), prevu.compte());
                fait = !(issue instanceof String texte && texte.startsWith("échec"));
            }
            if (!fait) {
                enAttente.add(prevu);
            }
        }
        return enAttente.isEmpty();
    }
}
