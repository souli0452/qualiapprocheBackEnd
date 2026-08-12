package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.enumeration.ModuleAbonnement;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.licence.ContenuDeLicence;
import com.qualiapproche.common.licence.LicenceIllisibleException;
import com.qualiapproche.common.licence.VerificateurDeLicence;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.referentiel.entities.LicenceInstallee;
import com.qualiapproche.referentiel.repository.LicenceInstalleeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * La licence de cette installation : la poser, la relire, dire ce qu'elle ouvre.
 *
 * <p>C'est ici que se décide ce que la plateforme laisse faire. Une licence absente ou expirée ne
 * ferme pas l'application : les données restent <b>consultables</b>, seules les actions d'écriture
 * sont suspendues. Couper l'accès aux données qualité d'un client transformerait un retard de
 * paiement en litige, et le pousserait à chercher comment contourner.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LicenceInstalleeService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ESSAI = "ESSAI";
    private static final String COMMERCIALE = "COMMERCIALE";

    private final LicenceInstalleeRepository repository;

    /**
     * Clé publique de l'éditeur, embarquée à la construction du produit.
     *
     * <p>Elle permet de vérifier une licence, jamais d'en signer une : sa présence dans le code
     * livré ne donne aucun moyen d'en fabriquer.</p>
     */
    @Value("${qualisira.licence.cle-publique:}")
    private String clePublique;

    /**
     * Refuse de démarrer sans clé de vérification.
     *
     * <p>Une clé par défaut vivait dans {@code application.yml} : celle d'un poste de
     * développement, versionnée, donc embarquée dans toute installation bâtie depuis ce dépôt. Si
     * {@code QUALISIRA_CLE_PUBLIQUE} était oubliée à la livraison, rien ne le signalait — le
     * service démarrait, l'écran s'ouvrait, et c'est seulement en collant la licence du client
     * qu'apparaissait « signature invalide », des jours plus tard, sans que personne ne fasse le
     * lien avec une variable manquante.</p>
     *
     * <p>Sans clé, ce service ne peut authentifier aucune licence : il ne rendrait qu'une
     * installation où toute écriture est suspendue. Autant le dire au démarrage.</p>
     */
    @PostConstruct
    void verifierLaCleDeVerification() {
        if (clePublique == null || clePublique.isBlank()) {
            throw new IllegalStateException("""

                    ════════════════════════════════════════════════════════════════════
                      AUCUNE CLÉ DE VÉRIFICATION — le service ne démarre pas.

                      QUALISIRA_CLE_PUBLIQUE n'est pas posée. Sans elle, aucune licence
                      ne peut être authentifiée : toute licence collée serait refusée
                      pour « signature invalide », y compris la bonne.

                      Relevez la clé publique de l'éditeur sur l'outil d'émission
                      (écran « Clé de vérification », ou /actuator/info) et posez-la :

                          QUALISIRA_CLE_PUBLIQUE=<la clé, en Base64>

                      Elle vérifie une licence et n'en signe aucune : la porter dans la
                      configuration du produit livré ne divulgue rien.
                    ════════════════════════════════════════════════════════════════════
                    """);
        }
    }

    /**
     * À qui cette installation appartient — le repère auquel toute licence est confrontée.
     *
     * <p>Voir {@link CodeDeLInstallation}, qui dit d'où il vient et ce que son obfuscation vaut.</p>
     */
    private final CodeDeLInstallation installation;

    // ---------------------------------------------------------------- lecture

    /**
     * État courant, tel que l'écran d'accueil l'attend.
     *
     * <p>Le jeton est revérifié à chaque lecture plutôt que cru sur parole : c'est ce qui rend
     * inopérante une modification directe des colonnes en base. Un jeton devenu invalide vaut
     * absence de licence.</p>
     */
    @Transactional
    public EtatLicenceDto etat() {
        LicenceInstallee installee = repository.findTopByOrderByInstalleeLeDesc().orElse(null);

        if (installee == null) {
            return EtatLicenceDto.builder()
                    .statut("ABSENTE")
                    .actionsOuvertes(false)
                    .modules(List.of())
                    .message("Cette installation n'a pas encore de licence. Collez celle que vous a "
                            + "remise l'éditeur — licence d'essai comprise.")
                    .build();
        }

        // Toute licence doit être signée, sans exception.
        //
        // Un jeton absent valait auparavant « essai local, rien à vérifier » : la signature ET le
        // contrôle du code partenaire étaient alors sautés, et la validité lue directement dans les
        // colonnes. Une seule ligne insérée à la main — type COMMERCIALE, jeton nul, fin en 2099,
        // tous modules — passait donc pour une licence perpétuelle, sans rien connaître de la
        // cryptographie. Le raccourci n'était vrai que tant que le produit était seul à créer des
        // lignes sans jeton ; il ne l'est plus.
        ContenuDeLicence contenu = relire(installee);
        if (contenu == null) {
            return EtatLicenceDto.builder()
                    .statut("ABSENTE")
                    .actionsOuvertes(false)
                    .modules(List.of())
                    .message("La licence installée n'est pas vérifiable. Installez celle que vous a "
                            + "remise l'éditeur.")
                    .build();
        }

        // Contrôlé à chaque lecture, et sur le contenu relu plutôt que sur la colonne : une base
        // restaurée depuis un autre client, ou une licence posée avant que le code ne soit
        // configuré, doit être écartée ici aussi. Sans quoi le contrôle de l'installation ne
        // vaudrait que le jour où quelqu'un a bien voulu passer par l'écran.
        if (!installation.reconnait(contenu.partenaireCode())) {
            log.error("La licence {} a été émise pour « {} » ({}) alors que cette installation "
                            + "déclare « {} » : elle est tenue pour absente.", installee.getReference(),
                    contenu.partenaireNom(), contenu.partenaireCode(), installation.attendu());
            return EtatLicenceDto.builder()
                    .statut("ABSENTE")
                    .actionsOuvertes(false)
                    .modules(List.of())
                    .message("La licence installée a été émise pour « " + contenu.partenaireNom()
                            + " », qui n'est pas cette installation. Installez celle qui vous a "
                            + "été remise.")
                    .build();
        }

        LocalDate aujourdhui = jourCourant(installee);
        boolean valide = !aujourdhui.isBefore(installee.getDebut())
                && !aujourdhui.isAfter(installee.getFin());
        long restants = java.time.temporal.ChronoUnit.DAYS.between(aujourdhui, installee.getFin());

        return EtatLicenceDto.builder()
                .statut(valide ? "ACTIVE" : "EXPIREE")
                .actionsOuvertes(valide)
                .type(installee.getType())
                .reference(installee.getReference())
                .partenaireNom(installee.getPartenaireNom())
                .debut(installee.getDebut())
                .fin(installee.getFin())
                .joursRestants(restants)
                .modules(modulesDe(installee))
                .utilisateursMax(installee.getUtilisateursMax())
                .message(message(installee, valide, restants))
                .build();
    }

    /** Un module est-il ouvert par la licence en cours ? */
    @Transactional
    public boolean ouvre(ModuleAbonnement module) {
        EtatLicenceDto etat = etat();
        return etat.isActionsOuvertes() && etat.getModules().contains(module.name());
    }

    // ---------------------------------------------------------------- installation

    /**
     * Installe la licence collée par l'administrateur.
     *
     * <p>Vérifiée avant d'être enregistrée : une licence contrefaite, expirée depuis longtemps ou
     * destinée à un autre client n'a pas à être conservée. Les modules s'ouvrent immédiatement,
     * sans redémarrage.</p>
     */
    @Transactional
    public EtatLicenceDto installer(String jeton) {
        ContenuDeLicence contenu;
        try {
            contenu = VerificateurDeLicence.lire(jeton, clePublique);
        } catch (LicenceIllisibleException e) {
            throw new BusinessException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // Avant les dates : une licence destinée à un autre client n'a pas à être jugée sur sa
        // période. Ce qu'il faut dire à celui qui la colle, c'est qu'elle n'est pas la sienne.
        if (!installation.reconnait(contenu.partenaireCode())) {
            log.warn("Licence {} refusée : émise pour « {} » ({}), installation déclarée « {} ».",
                    contenu.reference(), contenu.partenaireNom(), contenu.partenaireCode(),
                    installation.attendu());
            throw new BusinessException(
                    "Cette licence a été émise pour « " + contenu.partenaireNom()
                            + " » et ne vaut que chez lui. Demandez à l'éditeur celle qui "
                            + "correspond à votre installation.",
                    HttpStatus.BAD_REQUEST);
        }

        if (contenu.fin() == null || contenu.debut() == null) {
            throw new BusinessException("Cette licence ne porte pas de période de validité.",
                    HttpStatus.BAD_REQUEST);
        }
        if (contenu.fin().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    "Cette licence a pris fin le " + contenu.fin().format(JOUR)
                            + ". Demandez-en une nouvelle à l'éditeur.",
                    HttpStatus.BAD_REQUEST);
        }
        if (contenu.modules() == null || contenu.modules().isEmpty()) {
            throw new BusinessException("Cette licence n'ouvre aucun module.", HttpStatus.BAD_REQUEST);
        }
        if (contenu.reference() != null && repository.existsByReference(contenu.reference())) {
            throw new BusinessException(
                    "La licence " + contenu.reference() + " est déjà installée.",
                    HttpStatus.CONFLICT);
        }

        LicenceInstallee installee = LicenceInstallee.builder()
                .jeton(jeton.replaceAll("\\s", ""))
                .type(contenu.estUnEssai() ? ESSAI : COMMERCIALE)
                .reference(contenu.reference())
                .partenaireCode(contenu.partenaireCode())
                .partenaireNom(contenu.partenaireNom())
                .debut(contenu.debut())
                .fin(contenu.fin())
                .modules(String.join(",", contenu.modules()))
                .utilisateursMax(contenu.utilisateursMax())
                .installeeLe(LocalDateTime.now())
                .installeePar(SecurityUtils.getCurrentUserFullName())
                .dernierJourVu(LocalDate.now())
                .build();

        repository.save(installee);
        log.info("Licence {} installée pour {} ({} → {}, {} module(s))", contenu.reference(),
                contenu.partenaireNom(), contenu.debut(), contenu.fin(), contenu.modules().size());
        return etat();
    }

    // ---------------------------------------------------------------- interne

    private ContenuDeLicence relire(LicenceInstallee installee) {
        if (installee.getJeton() == null || installee.getJeton().isBlank()) {
            // Plus aucune licence n'est posée sans jeton : une ligne sans signature ne peut venir
            // que d'une écriture directe en base, et n'ouvre donc rien.
            log.error("Licence {} sans jeton signé : écriture directe en base, elle n'ouvre rien.",
                    installee.getReference());
            return null;
        }
        try {
            return VerificateurDeLicence.lire(installee.getJeton(), clePublique);
        } catch (LicenceIllisibleException e) {
            log.error("La licence installée n'est plus vérifiable : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Le jour à retenir, une horloge reculée étant sans effet.
     *
     * <p>Le jour le plus avancé jamais vu est mémorisé. Si la date du serveur repart en arrière —
     * ce qu'un client peut faire chez lui — c'est ce repère qui fait foi, et la licence expirée
     * le reste.</p>
     */
    private LocalDate jourCourant(LicenceInstallee installee) {
        LocalDate aujourdhui = LocalDate.now();
        LocalDate repere = installee.getDernierJourVu();

        if (repere == null || aujourdhui.isAfter(repere)) {
            installee.setDernierJourVu(aujourdhui);
            repository.save(installee);
            return aujourdhui;
        }
        if (aujourdhui.isBefore(repere)) {
            log.warn("Horloge en retard sur le dernier jour observé ({} < {}) : c'est ce dernier "
                    + "qui fait foi pour la validité de la licence.", aujourdhui, repere);
            return repere;
        }
        return aujourdhui;
    }

    private List<String> modulesDe(LicenceInstallee installee) {
        if (installee.getModules() == null || installee.getModules().isBlank()) {
            return List.of();
        }
        return Arrays.stream(installee.getModules().split(","))
                .map(String::trim)
                .filter(module -> !module.isEmpty())
                .toList();
    }

    /** La phrase affichée telle quelle : c'est elle qui dit quoi faire. */
    private String message(LicenceInstallee installee, boolean valide, long restants) {
        if (!valide) {
            String fin = installee.getFin().format(JOUR);
            return ESSAI.equals(installee.getType())
                    ? "Votre essai gratuit a pris fin le " + fin + ". Les données restent "
                      + "consultables, mais les actions sont suspendues jusqu'à l'installation "
                      + "d'une licence."
                    : "Votre licence a pris fin le " + fin + ". Les données restent consultables, "
                      + "mais les actions sont suspendues jusqu'au renouvellement.";
        }
        if (ESSAI.equals(installee.getType())) {
            // Sans la liste des modules : elle a fait de ce bandeau une ligne de deux cents
            // caractères que personne ne lisait. Une phrase tient dans un bandeau, une liste
            // demande un écran — c'est celui de la licence qui la porte, ouverts et fermés.
            return "Essai gratuit : il reste " + restants + " jour(s). Il prend fin le "
                    + installee.getFin().format(JOUR) + ".";
        }
        if (restants <= 30) {
            return "Votre licence prend fin le " + installee.getFin().format(JOUR)
                    + " — dans " + restants + " jour(s). Pensez à la renouveler.";
        }
        return "Licence active jusqu'au " + installee.getFin().format(JOUR) + ".";
    }
}
