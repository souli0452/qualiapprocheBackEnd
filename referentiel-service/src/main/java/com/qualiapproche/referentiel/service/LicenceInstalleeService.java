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
import java.util.stream.Collectors;

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

    @Value("${qualisira.licence.essai-jours:7}")
    private int joursDEssai;

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
        boolean essaiDisponible = !repository.existsByType(ESSAI);

        if (installee == null) {
            return EtatLicenceDto.builder()
                    .statut("ABSENTE")
                    .actionsOuvertes(false)
                    .essaiDisponible(essaiDisponible)
                    .modules(List.of())
                    .message("Cette installation n'a pas encore de licence. Collez celle que vous a "
                            + "remise l'éditeur, ou démarrez un essai gratuit.")
                    .build();
        }

        ContenuDeLicence contenu = relire(installee);
        if (contenu == null && installee.getJeton() != null) {
            return EtatLicenceDto.builder()
                    .statut("ABSENTE")
                    .actionsOuvertes(false)
                    .essaiDisponible(essaiDisponible)
                    .modules(List.of())
                    .message("La licence installée n'est plus vérifiable. Installez-en une nouvelle.")
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
                .essaiDisponible(essaiDisponible && !valide)
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

    /**
     * Démarre l'essai gratuit : tous les modules, quelques jours.
     *
     * <p>Il n'est pas signé — personne ici ne détient la clé de l'éditeur — et c'est précisément
     * pourquoi il est court. Un seul par installation : sans cette limite, il suffirait d'en
     * redemander un à chaque échéance.</p>
     */
    @Transactional
    public EtatLicenceDto demarrerEssai() {
        if (repository.existsByType(ESSAI)) {
            throw new BusinessException(
                    "L'essai gratuit a déjà été utilisé sur cette installation. "
                            + "Contactez l'éditeur pour obtenir une licence.",
                    HttpStatus.CONFLICT);
        }

        LocalDate debut = LocalDate.now();
        LicenceInstallee essai = LicenceInstallee.builder()
                .type(ESSAI)
                .reference("ESSAI-" + debut)
                .partenaireNom("Essai gratuit")
                .debut(debut)
                .fin(debut.plusDays(joursDEssai))
                .modules(Arrays.stream(ModuleAbonnement.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(",")))
                .utilisateursMax(0)
                .installeeLe(LocalDateTime.now())
                .installeePar(SecurityUtils.getCurrentUserFullName())
                .dernierJourVu(debut)
                .build();

        repository.save(essai);
        log.info("Essai gratuit de {} jours démarré, tous modules ouverts.", joursDEssai);
        return etat();
    }

    // ---------------------------------------------------------------- interne

    private ContenuDeLicence relire(LicenceInstallee installee) {
        if (installee.getJeton() == null || installee.getJeton().isBlank()) {
            // Essai local : rien à vérifier, il n'a jamais été signé.
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
            return "Essai gratuit : il reste " + restants + " jour(s). Tous les modules sont "
                    + "ouverts pendant cette période.";
        }
        if (restants <= 30) {
            return "Votre licence prend fin le " + installee.getFin().format(JOUR)
                    + " — dans " + restants + " jour(s). Pensez à la renouveler.";
        }
        return "Licence active jusqu'au " + installee.getFin().format(JOUR) + ".";
    }
}
