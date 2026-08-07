package com.qualiapproche.referentiel.config;

import com.qualiapproche.common.utils.ClesReglages;
import com.qualiapproche.referentiel.entities.Parametre;
import com.qualiapproche.referentiel.entities.TypeParametre;
import com.qualiapproche.referentiel.repository.ParametreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Réglages que l'application sait utiliser, semés vides au premier démarrage.
 *
 * <p>Semer les <b>clés</b> et non les valeurs : ce sont les clés que le code cite — le pied de page
 * des courriels demande {@code CONTACT_EMAIL} — et l'administrateur ne peut pas les deviner. Il les
 * trouve donc dans son écran, avec leur intitulé et leur usage, et n'a qu'à les renseigner.</p>
 *
 * <p>Les valeurs restent vides : inventer un téléphone ou une adresse ferait figurer une donnée
 * fausse au bas de courriels envoyés à l'extérieur. Un réglage vide est simplement ignoré par ce qui
 * le lit.</p>
 *
 * <p>Le responsable qualité ne fait pas exception : son nom et son courriel sont semés vides comme le
 * reste. Le super administrateur les saisit au lancement, et l'écran l'y contraint — l'adresse
 * commande la mise en copie des courriels de non-conformité, aussi son absence ne peut pas passer
 * inaperçue.</p>
 *
 * <p>Idempotent : chaque clé absente est créée, celles qui existent ne sont pas touchées — un
 * redémarrage n'écrase jamais ce que l'organisation a saisi.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParametresLivres {

    private record Livre(String cle, String libelle, String description, TypeParametre type,
                        boolean lisibleSansHabilitation) { }

    /**
     * Les réglages dont le code se sert aujourd'hui.
     *
     * <p>« Lisible sans habilitation » désigne ce qu'un service peut demander <b>hors de toute
     * requête utilisateur</b> : composer le pied d'un courriel ou en calculer la copie se fait après
     * commit, sur un fil de fond, où aucune permission ne circule. Ce qui n'est lu que depuis l'écran
     * d'administration n'a pas à y figurer.</p>
     */
    private static final List<Livre> LIVRES = List.of(
            new Livre(ClesReglages.ORGANISATION_NOM, "Nom de l'organisation",
                    "Affiché au bas des courriels et sur les documents édités.",
                    TypeParametre.TEXTE, true),
            new Livre(ClesReglages.CONTACT_EMAIL, "Courriel de contact",
                    "Adresse à laquelle un destinataire de courriel peut répondre ou demander de l'aide.",
                    TypeParametre.COURRIEL, true),
            new Livre(ClesReglages.CONTACT_TELEPHONE, "Téléphone de contact",
                    "Numéro affiché au bas des courriels.", TypeParametre.TELEPHONE, true),
            new Livre(ClesReglages.ADRESSE_POSTALE, "Adresse postale",
                    "Adresse de l'organisation, affichée au bas des courriels.",
                    TypeParametre.ADRESSE, true),
            new Livre(ClesReglages.SITE_WEB, "Site web",
                    "Adresse du site institutionnel, affichée au bas des courriels.",
                    TypeParametre.URL, true),
            new Livre(ClesReglages.LOGO_URL, "Logo",
                    "Adresse de l'image du logo. Doit être accessible depuis l'extérieur pour "
                            + "s'afficher dans un courriel : une image servie derrière une "
                            + "authentification apparaîtra vide chez le destinataire.",
                    TypeParametre.IMAGE, true),
            new Livre(ClesReglages.RESPONSABLE_QUALITE_NOM, "Nom du responsable qualité",
                    "Nom complet du responsable qualité de l'organisation.",
                    TypeParametre.TEXTE, true),
            new Livre(ClesReglages.RESPONSABLE_QUALITE_EMAIL, "Courriel du responsable qualité",
                    "Mis en copie des courriels d'imputation de non-conformité. Laissé vide, "
                            + "le message part sans copie.",
                    TypeParametre.COURRIEL, true),
            new Livre(ClesReglages.RAPPEL_ECHEANCE_JOURS, "Rappel avant échéance (jours)",
                    "Nombre de jours avant l'échéance d'un plan d'action à partir duquel le "
                            + "responsable est relancé. Vide, deux jours s'appliquent.",
                    TypeParametre.NOMBRE, true)
    );

    private final ParametreRepository repository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void semer() {
        int crees = 0;
        for (Livre livre : LIVRES) {
            if (repository.existsByCle(livre.cle())) {
                continue;
            }
            repository.save(Parametre.builder()
                    .cle(livre.cle())
                    .libelle(livre.libelle())
                    .description(livre.description())
                    .type(livre.type())
                    .lisibleSansHabilitation(livre.lisibleSansHabilitation())
                    .build());
            crees++;
        }
        if (crees > 0) {
            log.info("{} réglage(s) d'organisation créé(s), sans valeur : à renseigner depuis l'écran "
                    + "de configuration. Un réglage vide n'apparaît nulle part.", crees);
        }
    }
}
