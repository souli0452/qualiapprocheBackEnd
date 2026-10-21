package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.dto.StructureDto;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Ce que rendent, sans jeton, les deux points d'entrée que la sécurité laisse ouverts.
 *
 * <p>{@code /structures/direction} et {@code /licence/etat} sont lus avant toute identification :
 * le premier par user-service pendant l'ouverture de session, pour joindre l'état de la licence à
 * la réponse ; le second par la passerelle, qui en tire les modules ouverts avant de résoudre
 * l'utilisateur, et par user-service pour le plafond de comptes. Les fermer casserait la connexion
 * et le contrôle de licence.</p>
 *
 * <p>Mais ils étaient servis en entier à quiconque, la passerelle routant
 * {@code /referentiel-service/**} : la fiche de la direction — son courriel, l'autorité qui signe
 * et son titre, l'identifiant de qui l'a créée — et la licence — sa référence, le nom du titulaire.
 * Rien de cela ne sert aux lecteurs anonymes. Ils reçoivent désormais ce qu'ils lisent, et rien
 * d'autre ; un appel authentifié reçoit la vue complète, comme avant.</p>
 */
final class VuesAnonymes {

    private VuesAnonymes() {
    }

    /** L'appel arrive-t-il sans jeton valide ? */
    static boolean appelAnonyme() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        return authentification == null
                || authentification instanceof AnonymousAuthenticationToken
                || !authentification.isAuthenticated();
    }

    /**
     * La direction réduite à l'état de sa licence : c'est tout ce que l'ouverture de session lit
     * ({@code KcUserService}, licence active, jours restants, modules souscrits).
     */
    static StructureDto direction(StructureDto complete) {
        if (complete == null) {
            return null;
        }
        return StructureDto.builder()
                .licenceActive(complete.getLicenceActive())
                .licenseDaysRemaining(complete.getLicenseDaysRemaining())
                .modulesSubscribed(complete.getModulesSubscribed())
                .dateDebutLicence(complete.getDateDebutLicence())
                .dateFinLicence(complete.getDateFinLicence())
                .build();
    }

    /**
     * L'état de la licence sans ce qui l'identifie : la passerelle lit l'ouverture des actions,
     * les modules et le message ; user-service, le plafond de comptes. La référence, la nature et
     * le titulaire restent à l'écran de la licence, qui est authentifié.
     */
    static EtatLicenceDto etatLicence(EtatLicenceDto complet) {
        if (complet == null) {
            return null;
        }
        return EtatLicenceDto.builder()
                .statut(complet.getStatut())
                .actionsOuvertes(complet.isActionsOuvertes())
                .debut(complet.getDebut())
                .fin(complet.getFin())
                .joursRestants(complet.getJoursRestants())
                .modules(complet.getModules())
                .utilisateursMax(complet.getUtilisateursMax())
                .message(complet.getMessage())
                .build();
    }
}
