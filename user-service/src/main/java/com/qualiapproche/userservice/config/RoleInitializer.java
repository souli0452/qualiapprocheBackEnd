package com.qualiapproche.userservice.config;

import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer implements CommandLineRunner {

    private final AppRoleRepository appRoleRepository;

    @Override
    public void run(String... args) {
        log.info("Synchronisation des rôles et permissions standards...");

        // 1. SUPER_ADMIN (Toutes les permissions du dictionnaire)
        syncRole("SUPER_ADMIN", "Accès total à toutes les fonctionnalités", Arrays.asList(
                "CONFIG_READ", "SERVICE_MANAGE", "CONFIG_GLOBAL_MANAGE", "TYPE_PROC_MANAGE",
                "NC_LEVEL_MANAGE", "ACTION_TYPE_MANAGE", "NC_ORIGIN_MANAGE", "RESOURCES_READ",
                "FORMATION_MANAGE", "FOURNISSEUR_MANAGE", "ACTIONS_READ", "RECLAMATION_READ",
                "SUBMIT_RECLAMATION", "RISQUE_READ", "SUBMIT_RISQUE", "AUDITE_READ",
                "SUBMIT_NC", "NC_READ", "REGLEMENTATION_READ", "CRITERE_EVAL_READ",
                "DOC_READ", "DOC_CAT_MANAGE", "TRAITEMENT_DEMANDES", "RECEPTION_NC",
                "VALIDATION_RQ", "IMPUTATION_NC", "TRAITEMENT_NC", "VALIDATION_CHEF",
                "RQ_NC", "CONSULTATION_NC", "TRAITEMENT_PLAN", "MANAGE_USER", "ROLE_MANAGE", "STRUCT_MANAGE"
        ));

        // 2. Rôles spécifiques pour le Workflow Non-Conformité (utilisés dans isUserInRoles du Frontend)
        syncRole("RECEPTION_NC", "Analyse initiale du pilote NC", Arrays.asList("TRAITEMENT_DEMANDES", "RECEPTION_NC", "NC_READ"));
        syncRole("VALIDATION_RQ", "Validation par Responsable Qualité NC", Arrays.asList("TRAITEMENT_DEMANDES", "VALIDATION_RQ", "NC_READ"));
        syncRole("IMPUTATION_NC", "Affectation des responsables NC", Arrays.asList("TRAITEMENT_DEMANDES", "IMPUTATION_NC", "NC_READ"));
        syncRole("TRAITEMENT_NC", "Proposition d'actions correctives NC", Arrays.asList("TRAITEMENT_DEMANDES", "TRAITEMENT_NC", "NC_READ"));
        syncRole("VALIDATION_CHEF", "Validation des actions par le Chef", Arrays.asList("TRAITEMENT_DEMANDES", "VALIDATION_CHEF", "NC_READ"));
        syncRole("RQ_NC", "Suivi final par Responsable Qualité NC", Arrays.asList("TRAITEMENT_DEMANDES", "RQ_NC", "NC_READ"));
        syncRole("CONSULTATION_NC", "Consultation des non-conformités", Arrays.asList("TRAITEMENT_DEMANDES", "CONSULTATION_NC", "NC_READ"));
        syncRole("TRAITEMENT_PLAN", "Traitement des plans d'actions", Arrays.asList("TRAITEMENT_DEMANDES", "TRAITEMENT_PLAN"));

        // 3. Rôles de gestion administrative
        syncRole("MANAGE_USER", "Gestion des utilisateurs et rôles", Arrays.asList("MANAGE_USER", "ROLE_MANAGE"));

        log.info("Synchronisation des rôles terminée.");
    }

    private void syncRole(String name, String description, List<String> permissions) {
        appRoleRepository.findByName(name).ifPresentOrElse(
            role -> {
                role.setPermissions(permissions);
                role.setDescription(description);
                appRoleRepository.save(role);
                log.info("Rôle {} mis à jour avec {} permissions.", name, permissions.size());
            },
            () -> {
                AppRole role = AppRole.builder()
                        .name(name)
                        .description(description)
                        .permissions(permissions)
                        .build();
                appRoleRepository.save(role);
                log.info("Rôle {} créé.", name);
            }
        );
    }
}
