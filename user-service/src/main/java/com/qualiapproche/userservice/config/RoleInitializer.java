package com.qualiapproche.userservice.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer implements CommandLineRunner {

    private final AppRoleRepository appRoleRepository;

    @Override
    public void run(String... args) {
        log.info("Synchronisation des rôles et permissions standards...");

        // 1. SUPER_ADMIN : littéralement toutes les permissions du dictionnaire.
        //
        // La liste était recopiée à la main, et avait déjà divergé : le dictionnaire servi à
        // l'écran d'attribution des rôles s'enrichit à chaque module, sans que ce rôle en
        // hérite. Un « accès total » amputé des permissions les plus récentes est le pire des
        // deux mondes — l'administrateur croit tout détenir et découvre le refus à l'usage.
        // La source unique est désormais permissions.json, lu ici même.
        syncRole("SUPER_ADMIN", "Accès total à toutes les fonctionnalités", toutesLesPermissions());

        // 2. Rôles responsables des circuits de validation.
        //
        // Ils étaient désignés par les étapes des circuits livrés sans exister nulle part :
        // aucune affectation n'était donc possible, et les étapes n'avaient ni titulaire habilité
        // à décider, ni destinataire à notifier. Les noms reprennent exactement ceux inscrits sur
        // les étapes (cf. WorkflowDataInitializer) — c'est sur cette égalité que reposent le
        // contrôle d'habilitation et la résolution des destinataires.

        syncRole("AGENT", "Déclare et suit ses propres signalements", Arrays.asList(
                "SUBMIT_NC", "NC_READ", "CONSULTATION_NC",
                "SUBMIT_RECLAMATION", "RECLAMATION_READ", "SUBMIT_RISQUE", "RISQUE_READ",
                "ACTIONS_READ", "RESOURCES_READ", "DOC_READ",
                "document-read", "document-download", "workflow-read",
                // Il demande ce qu'il ne peut pas modifier lui-même, et suit sa demande.
                "demande-document-read", "demande-document-write",
                // Signalements : il déclare et suit les siens.
                "nc-read", "nc-write", "reclamation-read", "reclamation-write",
                "risque-read", "risque-write",
                // Consultation seule du reste de son environnement de travail.
                "action-read", "action-corrective-read", "plan-action-read",
                "formation-read", "fournisseur-read", "prestataire-read", "produit-read",
                "reglementation-read", "exigence-read",
                "menu-accueil", "menu-gestion-documentaire", "menu-qualite",
                "menu-ressources", "menu-actions"
        ));

        syncRole("PILOTE", "Réceptionne, impute et valide les dossiers de son périmètre", Arrays.asList(
                "SUBMIT_NC", "NC_READ", "CONSULTATION_NC", "RECEPTION_NC", "IMPUTATION_NC",
                "TRAITEMENT_NC", "TRAITEMENT_DEMANDES", "TRAITEMENT_PLAN", "VALIDATION_CHEF",
                "ACTIONS_READ", "RESOURCES_READ", "DOC_READ", "REGLEMENTATION_READ",
                "document-read", "document-download", "document-history",
                "workflow-read", "workflow-validate",
                "demande-document-read", "demande-document-write",
                // Les trois décisions qui font son rôle : réceptionner, imputer, valider.
                "nc-read", "nc-write", "nc-receive", "nc-impute", "nc-validate",
                "plan-action-read", "plan-action-write",
                "action-read", "action-write", "action-corrective-read", "action-corrective-write",
                "efficacite-read", "efficacite-write",
                "reclamation-read", "reclamation-write", "risque-read", "risque-write",
                "formation-read", "fournisseur-read", "prestataire-read", "produit-read",
                "reglementation-read", "exigence-read", "structure-read", "departement-read",
                "menu-accueil", "menu-gestion-documentaire", "menu-qualite",
                "menu-ressources", "menu-actions", "menu-traitements"
        ));

        syncRole("AGENT_IMPUTE", "Traite les dossiers qui lui sont imputés", Arrays.asList(
                "NC_READ", "CONSULTATION_NC", "TRAITEMENT_NC", "TRAITEMENT_PLAN",
                "ACTIONS_READ", "DOC_READ",
                "document-read", "document-download", "workflow-read",
                "demande-document-read", "demande-document-write",
                // Traite ce qui lui est imputé : il écrit sur le dossier sans le décider.
                "nc-read", "nc-write", "plan-action-read", "plan-action-write",
                "action-read", "action-write", "action-corrective-read", "action-corrective-write",
                "efficacite-read", "efficacite-write",
                "menu-accueil", "menu-gestion-documentaire", "menu-qualite",
                "menu-actions", "menu-traitements"
        ));

        syncRole("RESPONSABLE_QUALITE", "Valide et clôt les dossiers au titre de la qualité", Arrays.asList(
                "NC_READ", "CONSULTATION_NC", "VALIDATION_RQ", "RQ_NC", "TRAITEMENT_PLAN",
                "RECEPTION_NC", "TRAITEMENT_DEMANDES",
                "ACTIONS_READ", "RESOURCES_READ", "AUDITE_READ",
                "REGLEMENTATION_READ", "CRITERE_EVAL_READ", "DOC_READ", "DOC_CAT_MANAGE",
                "document-read", "document-write", "document-download", "document-history",
                "document-audit", "document-validate", "document-archive",
                "document-type-read", "workflow-read", "workflow-validate",
                // Il instruit les demandes de modification et de suppression, et les décide.
                "demande-document-read", "demande-document-write", "demande-document-validate",
                // Seul rôle habilité à valider puis clore au titre de la qualité.
                "nc-read", "nc-write", "nc-receive", "nc-validate", "nc-close",
                "plan-action-read", "plan-action-write", "plan-action-validate",
                "action-read", "action-write", "action-corrective-read", "action-corrective-write",
                "efficacite-read", "efficacite-write",
                "reclamation-read", "reclamation-write", "risque-read", "risque-write",
                // Paramétrage qualité qui relève de sa responsabilité.
                // Les trois référentiels du document en font partie : priorité, niveau de
                // confidentialité — qui décide de qui voit quoi — et domaine d'application.
                // Sans eux, les écrans correspondants restaient invisibles au seul rôle censé
                // les administrer, et seul le SUPER_ADMIN pouvait les régler.
                "priorite-document-read", "priorite-document-write",
                "niveau-confidentialite-read", "niveau-confidentialite-write",
                "domaine-application-read", "domaine-application-write",
                "type-nc-read", "type-nc-write", "niveau-nc-read", "niveau-nc-write",
                "categorie-fichier-read", "categorie-fichier-write",
                "exigence-read", "exigence-write", "reglementation-read", "reglementation-write",
                "formation-read", "fournisseur-read", "prestataire-read", "produit-read",
                "structure-read", "departement-read", "type-processus-read", "config-global-read",
                "menu-accueil", "menu-gestion-documentaire", "menu-qualite",
                "menu-ressources", "menu-actions", "menu-traitements", "menu-configuration"
        ));

        log.info("Synchronisation des rôles terminée.");
    }

    /**
     * Toutes les valeurs du dictionnaire {@code permissions.json}, celui-là même que sert
     * {@code AppRoleController#getPermissionsDictionary} à l'écran d'attribution des rôles.
     *
     * <p>Un dictionnaire illisible interrompt le démarrage : un SUPER_ADMIN silencieusement
     * privé de ses permissions serait plus coûteux à diagnostiquer qu'un démarrage refusé.</p>
     */
    private List<String> toutesLesPermissions() {
        try (InputStream flux = new ClassPathResource("permissions.json").getInputStream()) {
            List<Map<String, String>> dictionnaire =
                    new ObjectMapper().readValue(flux, new TypeReference<>() {});
            return dictionnaire.stream().map(entree -> entree.get("value")).toList();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Dictionnaire des permissions (permissions.json) illisible : "
                            + "impossible de constituer le rôle SUPER_ADMIN.", e);
        }
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
