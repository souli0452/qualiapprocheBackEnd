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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer implements CommandLineRunner {

    /** Permission qui commande l'installation d'une licence (cf. {@code LicenceController}). */
    private static final String PERMISSION_LICENCE = "licence-write";

    private final AppRoleRepository appRoleRepository;

    @Override
    public void run(String... args) {
        log.info("Rôles standards : création des absents, les rôles existants sont laissés tels quels.");

        // 1. SUPER_ADMIN : à sa création, littéralement toutes les permissions du dictionnaire.
        //
        // La liste était recopiée à la main, et avait déjà divergé ; elle est donc lue dans
        // permissions.json, le même fichier que sert l'écran d'attribution des rôles.
        //
        // Elle n'est plus réappliquée ensuite : le rôle appartient à l'administrateur une fois
        // créé. Le dictionnaire s'enrichissant à chaque module, penser à compléter ce rôle
        // depuis l'écran quand un module arrive.
        creerSiAbsent("SUPER_ADMIN", "Accès total à toutes les fonctionnalités", toutesLesPermissions());
        garantirLePouvoirDePoserUneLicence();

        // 2. Rôles responsables des circuits de validation.
        //
        // Ils étaient désignés par les étapes des circuits livrés sans exister nulle part :
        // aucune affectation n'était donc possible, et les étapes n'avaient ni titulaire habilité
        // à décider, ni destinataire à notifier. Les noms reprennent exactement ceux inscrits sur
        // les étapes (cf. WorkflowDataInitializer) — c'est sur cette égalité que reposent le
        // contrôle d'habilitation et la résolution des destinataires.

        creerSiAbsent("AGENT", "Déclare et suit ses propres signalements", Arrays.asList(
                "SUBMIT_NC", "NC_READ", "CONSULTATION_NC",
                "SUBMIT_RECLAMATION", "RECLAMATION_READ", "SUBMIT_RISQUE", "RISQUE_READ",
                "ACTIONS_READ", "RESOURCES_READ", "DOC_READ",
                "document-read", "document-download", "workflow-read",
                // Il demande ce qu'il ne peut pas modifier lui-même, et suit sa demande.
                "demande-document-read", "demande-document-write",
                // Signalements : il déclare et suit les siens.
                "nc-read", "nc-write", "reclamation-read", "reclamation-write",
                "risque-read", "risque-write",
                // Ce qu'il fait quand un dossier lui est imputé : le traiter, et porter son plan
                // d'action. Ces permissions venaient d'un rôle « agent imputé » qui n'en était pas
                // un ; l'habilitation à décider, elle, vient du titulaire inscrit sur le dossier.
                "TRAITEMENT_NC", "TRAITEMENT_PLAN",
                "plan-action-write", "action-write", "action-corrective-write",
                "efficacite-read", "efficacite-write", "menu-traitements",
                // Consultation seule du reste de son environnement de travail.
                "action-read", "action-corrective-read", "plan-action-read",
                "formation-read", "fournisseur-read", "prestataire-read", "produit-read",
                "reglementation-read", "exigence-read",
                "menu-accueil", "menu-gestion-documentaire", "menu-qualite",
                "menu-ressources", "menu-actions"
        ));

        creerSiAbsent("PILOTE", "Réceptionne, impute et valide les dossiers de son périmètre", Arrays.asList(
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

        // « Agent imputé » n'est pas un rôle : c'est une personne, désignée sur un dossier
        // précis. En faire un rôle ouvrait le traitement de *tout* dossier à *tout* agent
        // imputable. L'habilitation vient désormais du titulaire inscrit sur l'instance de
        // circuit (cf. WorkflowConditionAdapter, habilitation @TITULAIRE) ; les permissions qu'il
        // portait — traiter un dossier, écrire un plan d'action — reviennent au rôle AGENT.

        creerSiAbsent("RESPONSABLE_QUALITE", "Valide et clôt les dossiers au titre de la qualité", Arrays.asList(
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

        log.info("Rôles standards : traitement terminé.");
    }

    /**
     * La seule permission que {@code SUPER_ADMIN} ne peut pas perdre : poser une licence.
     *
     * <p>Unique dérogation à la règle du dessous — les dotations ne sont pas réappliquées, le
     * rôle appartient à l'administrateur. Celle-ci l'est, parce que son absence ne se répare pas
     * depuis l'application : sans {@code licence-write}, plus personne ne peut installer de
     * licence ni démarrer d'essai, et une installation dont la licence a pris fin reste
     * définitivement en lecture seule. C'est aussi ce qui dote les installations existantes, dont
     * le rôle a été créé avant que cette permission n'entre au dictionnaire.</p>
     *
     * <p>Elle est <b>ajoutée</b>, jamais retirée : les autres permissions du rôle restent celles
     * qu'un administrateur a décidées.</p>
     */
    private void garantirLePouvoirDePoserUneLicence() {
        appRoleRepository.findByName("SUPER_ADMIN").ifPresent(role -> {
            if (role.getPermissions().contains(PERMISSION_LICENCE)) {
                return;
            }
            List<String> permissions = new ArrayList<>(role.getPermissions());
            permissions.add(PERMISSION_LICENCE);
            role.setPermissions(permissions);
            appRoleRepository.save(role);
            log.info("Permission « {} » rendue au rôle SUPER_ADMIN : sans elle, aucune licence "
                    + "ne pouvait plus être installée sur cette instance.", PERMISSION_LICENCE);
        });
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
                    new ObjectMapper().readValue(flux, new TypeReference<>() { });
            return dictionnaire.stream().map(entree -> entree.get("value")).toList();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Dictionnaire des permissions (permissions.json) illisible : "
                            + "impossible de constituer le rôle SUPER_ADMIN.", e);
        }
    }

    /**
     * Crée le rôle s'il n'existe pas, et ne touche à rien s'il existe déjà.
     *
     * <p>Ces listes ne sont qu'une dotation de départ, pas une vérité à rétablir. Elles étaient
     * réappliquées à chaque démarrage : une permission accordée depuis l'écran d'administration
     * tenait jusqu'au redémarrage suivant, puis disparaissait sans trace ni message. Le rôle
     * appartient à l'administrateur dès lors qu'il existe.</p>
     *
     * <p>Conséquence assumée : les permissions d'un module livré plus tard n'atteignent pas
     * d'elles-mêmes les rôles déjà en base — il faut les y ajouter depuis l'écran. Le démarrage
     * le signale, pour que la question se pose au bon moment plutôt qu'à l'usage.</p>
     */
    private void creerSiAbsent(String name, String description, List<String> permissions) {
        if (appRoleRepository.findByName(name).isPresent()) {
            log.debug("Rôle {} déjà présent : ses permissions restent celles enregistrées.", name);
            return;
        }
        appRoleRepository.save(AppRole.builder()
                .name(name)
                .description(description)
                .permissions(permissions)
                .build());
        log.info("Rôle {} créé avec {} permissions.", name, permissions.size());
    }
}
