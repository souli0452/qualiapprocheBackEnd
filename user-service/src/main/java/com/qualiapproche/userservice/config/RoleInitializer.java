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
