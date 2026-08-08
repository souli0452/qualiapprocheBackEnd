package com.qualiapproche.userservice.service;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.client.StructureClient;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Le nombre d'utilisateurs que la licence autorise, et son application.
 *
 * <p>Une licence vendue pour deux utilisateurs en portait le nombre sans que rien ne l'applique :
 * le troisième compte se créait comme les autres. Ce qui n'est pas vérifié n'est pas une limite,
 * c'est une mention sur un document.</p>
 *
 * <p>Le refus est motivé et chiffré — « votre licence autorise 2 utilisateurs, 2 comptes sont
 * déjà actifs » — et il indique les deux issues réelles : libérer une place, ou étendre la
 * licence. Un « opération impossible » enverrait l'administrateur chercher une panne qui n'existe
 * pas.</p>
 *
 * <p>Le compte se fait sur les comptes <b>actifs</b>. Désactiver libère donc une place : c'est ce
 * qui permet de remplacer quelqu'un qui part sans effacer ce qu'il a produit, ni ce que les
 * dossiers gardent de son nom.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlafondDUtilisateurs {

    /**
     * Les comptes de service ne sont pas des utilisateurs.
     *
     * <p>Keycloak les range parmi les utilisateurs du royaume, sous ce préfixe qu'il pose
     * lui-même. Les compter reviendrait à facturer au client les appels entre nos propres
     * services — et sur une licence à deux places, à lui en confisquer une.</p>
     */
    private static final String COMPTE_DE_SERVICE = "service-account-";

    /**
     * Borne de lecture des comptes. Keycloak en rend cent par défaut, ce qui aurait sous-estimé
     * le total et laissé passer des créations au-delà du plafond, sur les installations qui en
     * ont le plus besoin.
     */
    private static final int LECTURE_MAX = 5000;

    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;
    private final StructureClient structureClient;

    /**
     * Refuse la création d'un compte si la licence n'en autorise pas davantage.
     *
     * @throws BusinessException {@code 409}, avec la phrase destinée à l'utilisateur
     */
    public void verifierAvantCreation() {
        verifier("créer un compte supplémentaire");
    }

    /**
     * Refuse la réactivation d'un compte si le plafond est déjà atteint.
     *
     * <p>Sans ce contrôle, la limite se contournait en trois gestes : désactiver un compte, en
     * créer un nouveau, réactiver le premier.</p>
     */
    public void verifierAvantReactivation() {
        verifier("réactiver ce compte");
    }

    private void verifier(String action) {
        int plafond = plafond();
        if (plafond <= 0) {
            // 0 vaut « sans limite » — c'est aussi ce que rend une licence absente, et il ne
            // revient pas à ce contrôle de fermer l'application : la passerelle s'en charge déjà.
            return;
        }

        long actifs = comptesActifs();
        if (actifs < plafond) {
            return;
        }

        log.info("Création refusée : {} compte(s) actif(s) pour un plafond de {}.", actifs, plafond);
        throw new BusinessException(
                "Votre licence autorise " + plafond + " utilisateur" + (plafond > 1 ? "s" : "")
                        + ", et " + actifs + " compte" + (actifs > 1 ? "s sont" : " est")
                        + " déjà actif" + (actifs > 1 ? "s" : "") + ". Pour " + action + ", "
                        + "désactivez un compte existant, ou demandez à l'éditeur d'étendre "
                        + "votre licence.",
                HttpStatus.CONFLICT);
    }

    /**
     * Nombre d'utilisateurs autorisé, ou {@code 0} si la question ne peut pas être tranchée.
     *
     * <p>Référentiel injoignable : on laisse passer. Empêcher de créer un compte parce qu'un
     * service voisin redémarre transformerait une panne de quelques secondes en blocage des
     * arrivées, alors que rien n'indique un dépassement.</p>
     */
    private int plafond() {
        try {
            EtatLicenceDto licence = structureClient.etatLicence();
            return licence != null ? licence.getUtilisateursMax() : 0;
        } catch (Exception e) {
            log.error("Plafond d'utilisateurs indéterminable (referentiel-service) : {}. "
                    + "La création reste ouverte.", e.getMessage());
            return 0;
        }
    }

    /** Comptes actifs, comptes de service exclus. */
    private long comptesActifs() {
        List<UserRepresentation> comptes = keycloak.realm(kcAuthProperties.getRealm())
                .users().list(0, LECTURE_MAX);

        if (comptes.size() >= LECTURE_MAX) {
            log.warn("Plus de {} comptes lus : le total peut être sous-estimé.", LECTURE_MAX);
        }

        return comptes.stream()
                .filter(UserRepresentation::isEnabled)
                .filter(compte -> compte.getUsername() == null
                        || !compte.getUsername().startsWith(COMPTE_DE_SERVICE))
                .count();
    }
}
