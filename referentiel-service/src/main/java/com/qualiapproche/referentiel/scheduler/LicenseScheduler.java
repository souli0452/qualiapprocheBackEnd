package com.qualiapproche.referentiel.scheduler;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.referentiel.client.UserClient;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.repository.StructureRepository;
import com.qualiapproche.referentiel.service.LicenceInstalleeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Prévient les administrateurs avant que la licence ne prenne fin.
 *
 * <p>Deux régimes qui répondent à deux besoins distincts. Un rappel isolé à <b>J-30</b> puis
 * <b>J-15</b> : assez tôt pour engager un renouvellement, qui demande un devis, une signature et
 * un paiement — prévenir trois jours avant ne laisserait pas le temps de payer. Puis un courriel
 * <b>chaque jour sur les trois derniers</b>, terme compris : un rappel isolé tombe un jour de
 * congé ou dans une boîte non relevée, et l'échéance arrive sans que personne n'y ait repensé.
 * Répété quatre jours de suite, il finit par être lu — et il reste assez rare pour ne pas être
 * filtré.</p>
 *
 * <p>Chaque message porte la <b>date d'expiration</b> en clair. « Votre licence expire bientôt »
 * n'appelle aucune décision ; une date permet de savoir s'il faut agir aujourd'hui.</p>
 *
 * <p>Destinataires : les porteurs du rôle {@code SUPER_ADMIN}, seuls habilités à poser une
 * licence. L'alerte partait auparavant à l'adresse de la direction — une boîte générique, souvent
 * relevée par quelqu'un qui ne peut rien y faire.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseScheduler {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ROLE_ADMIN = "SUPER_ADMIN";

    private final LicenceInstalleeService licenceInstalleeService;
    private final StructureRepository structureRepository;
    private final UserClient userClient;
    private final SendMailService sendMailService;

    /** Derniers jours, où le rappel devient quotidien. */
    @Value("${qualisira.licence.preavis-jours:3}")
    private int preavisJours;

    /** Rappels isolés, bien avant le terme, pour laisser le temps d'un renouvellement payant. */
    @Value("${qualisira.licence.rappels-jalons:30,15}")
    private String jalons;

    @Scheduled(cron = "0 0 8 * * *") // Chaque matin, pour être lu dans la journée
    public void checkLicenseExpirations() {
        EtatLicenceDto licence = licenceInstalleeService.etat();

        if ("ABSENTE".equals(licence.getStatut())) {
            return;
        }

        long restants = licence.getJoursRestants();

        if (restants < 0) {
            log.warn("Licence expirée depuis {} jour(s) : les écritures sont suspendues.", -restants);
            return;
        }
        if (!estUnJourDeRappel(restants)) {
            return;
        }

        alerter(licence, restants);
    }

    /**
     * Chaque jour du préavis, plus les jalons lointains.
     *
     * <p>Les deux ne se remplacent pas : le jalon prévient assez tôt pour qu'un renouvellement
     * payant aboutisse, le quotidien garantit que le terme ne surprenne personne.</p>
     */
    private boolean estUnJourDeRappel(long restants) {
        return restants <= preavisJours || jalons().contains(restants);
    }

    private Set<Long> jalons() {
        Set<Long> valeurs = new LinkedHashSet<>();
        for (String jalon : jalons.split(",")) {
            String valeur = jalon.trim();
            if (valeur.isEmpty()) {
                continue;
            }
            try {
                valeurs.add(Long.parseLong(valeur));
            } catch (NumberFormatException e) {
                // Un jalon illisible ne doit pas emporter les rappels quotidiens, qui sont les
                // plus critiques : on l'écarte, et on le dit.
                log.error("Jalon de rappel « {} » illisible dans qualisira.licence.rappels-jalons : "
                        + "il est ignoré.", valeur);
            }
        }
        return valeurs;
    }

    private void alerter(EtatLicenceDto licence, long restants) {
        String fin = licence.getFin() != null ? licence.getFin().format(JOUR) : "prochainement";
        boolean essai = "ESSAI".equals(licence.getType());

        String objet = restants == 0
                ? "QualiSira : votre " + (essai ? "essai gratuit prend" : "licence prend") + " fin aujourd'hui"
                : "QualiSira : " + (essai ? "votre essai gratuit expire" : "votre licence expire")
                  + " dans " + restants + " jour(s)";

        StringBuilder message = new StringBuilder();
        message.append(essai ? "Votre essai gratuit " : "Votre licence QualiSira ");
        message.append(restants == 0
                ? "prend fin aujourd'hui, le " + fin + "."
                : "prend fin le " + fin + ", dans " + restants + " jour(s).");
        message.append("\n\nPassé ce terme, les données restent consultables et exportables : "
                + "seules les créations et les décisions sont suspendues, jusqu'à l'installation "
                + "d'une licence.");
        message.append("\n\nPour la poser : menu « Configurations », puis « Licence de "
                + "l'installation ».");

        for (String adresse : destinataires()) {
            try {
                sendMailService.sendMail(adresse, objet, message.toString(), false);
                log.info("Échéance de licence ({} jour(s), fin le {}) signalée à {}.",
                        restants, fin, adresse);
            } catch (Exception e) {
                log.error("Échec de l'envoi de l'alerte d'échéance de licence à {}", adresse, e);
            }
        }
    }

    /**
     * À qui écrire : les super administrateurs, et à défaut l'adresse de la direction.
     *
     * <p>Le repli n'est pas cosmétique : si {@code user-service} est injoignable ou qu'aucun
     * compte ne porte le rôle, l'alerte disparaîtrait en silence, et l'échéance surviendrait
     * sans que personne n'ait été prévenu.</p>
     */
    private Set<String> destinataires() {
        Set<String> adresses = new LinkedHashSet<>();

        try {
            List<DestinataireDto> admins = userClient.getUsersByRole(ROLE_ADMIN);
            if (admins != null) {
                admins.stream()
                        .map(DestinataireDto::getEmail)
                        .filter(adresse -> adresse != null && !adresse.isBlank())
                        .forEach(adresses::add);
            }
        } catch (Exception e) {
            log.error("Porteurs du rôle {} introuvables auprès de user-service : {}",
                    ROLE_ADMIN, e.getMessage());
        }

        if (adresses.isEmpty()) {
            structureRepository.findAllByTypeStructure(TypeStructure.DIRECTION).stream()
                    .findFirst()
                    .map(Structure::getEmail)
                    .filter(adresse -> adresse != null && !adresse.isBlank())
                    .ifPresent(adresses::add);
            log.warn("Aucun super administrateur joignable : l'échéance est signalée à l'adresse "
                    + "de la direction.");
        }

        if (adresses.isEmpty()) {
            log.error("Licence à échéance, et aucune adresse où le signaler.");
        }
        return adresses;
    }
}
