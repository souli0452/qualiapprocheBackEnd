package com.qualiapproche.userservice.service;

import com.qualiapproche.common.dto.auth.ProfilPersonnelDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ce qu'un utilisateur modifie de son propre compte : son nom, son prénom, son téléphone.
 *
 * <p>Classe à part plutôt que méthode de {@link KcUserService} : celui-ci sert l'administration
 * des comptes, où l'on dispose de l'activation, des rôles et de la structure. Le libre-service
 * obéit à d'autres règles — c'est l'intéressé qui écrit, sans qu'aucune habilitation ne l'y
 * autorise — et les tenir dans le même corps de méthode reviendrait à espérer qu'on n'oublie
 * jamais lequel des deux appelants on sert.</p>
 *
 * <p>Le rapprochement avec {@code updateUser} dit ce qui est en jeu. Celui-ci pose
 * {@code setEnabled(dto.isEnabled())} et {@code syncAppRoles(id, dto.getRoles())} : appelé depuis
 * un écran de profil, dont le formulaire ne porte évidemment ni activation ni rôles, il
 * désactiverait le compte de l'utilisateur — le booléen primitif vaut {@code false} par défaut —
 * et effacerait ses rôles applicatifs au passage. D'où cette écriture-ci, qui ne relit et ne
 * réécrit que trois champs.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilPersonnel {

    /** Attribut Keycloak où le téléphone est rangé, comme à la création du compte. */
    private static final String ATTRIBUT_TELEPHONE = "phoneNumber";

    /** Chiffres, espaces et la ponctuation usuelle des numéros — indicatif international compris. */
    private static final String FORME_TELEPHONE = "[+()./\\-\\s0-9]{6,25}";

    private final Keycloak keycloak;
    private final KcAuthProperties kcAuthProperties;

    /**
     * Écrit le nom, le prénom et le téléphone de l'utilisateur désigné.
     *
     * <p>Rien d'autre n'est touché : ni l'activation, ni l'adresse électronique, ni la structure,
     * ni les rôles. La représentation est relue avant d'être réécrite, et les attributs existants
     * sont conservés — poser une carte d'attributs neuve effacerait la structure de l'utilisateur,
     * donc ses habilitations, et sa fonction avec.</p>
     *
     * @param userId identifiant Keycloak de l'intéressé, tiré de son jeton et non du corps de la
     *               requête : le lire dans le corps laisserait chacun modifier le compte d'autrui
     * @param profil les valeurs proposées
     * @throws BusinessException {@code 400} si le nom ou le prénom manque, ou si le téléphone est
     *         malformé
     */
    public void mettreAJour(String userId, ProfilPersonnelDto profil) {
        String prenom = exige(profil.getFirstName(), "Le prénom est obligatoire.");
        String nom = exige(profil.getLastName(), "Le nom est obligatoire.");
        String telephone = telephoneValide(profil.getPhoneNumber());

        UserResource ressource = keycloak.realm(kcAuthProperties.getRealm()).users().get(userId);
        UserRepresentation utilisateur = ressource.toRepresentation();

        utilisateur.setFirstName(prenom);
        utilisateur.setLastName(nom);
        utilisateur.setAttributes(avecTelephone(utilisateur.getAttributes(), telephone));

        ressource.update(utilisateur);
        log.info("Profil de l'utilisateur {} mis à jour par lui-même.", userId);
    }

    /**
     * Les attributs existants, le seul téléphone remplacé.
     *
     * <p>Copie défensive : la carte rendue par la représentation n'est pas garantie modifiable, et
     * un téléphone effacé doit retirer l'attribut plutôt que d'y laisser une chaîne vide — un
     * attribut vide s'affiche comme un numéro absent mais se relit comme un numéro présent.</p>
     */
    private Map<String, List<String>> avecTelephone(Map<String, List<String>> existants, String telephone) {
        Map<String, List<String>> attributs =
                existants == null ? new HashMap<>() : new HashMap<>(existants);
        if (telephone == null) {
            attributs.remove(ATTRIBUT_TELEPHONE);
        } else {
            attributs.put(ATTRIBUT_TELEPHONE, Collections.singletonList(telephone));
        }
        return attributs;
    }

    private String exige(String valeur, String message) {
        if (valeur == null || valeur.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }
        return valeur.trim();
    }

    /** @return le numéro nettoyé, ou {@code null} s'il n'est pas renseigné — le champ est facultatif */
    private String telephoneValide(String telephone) {
        if (telephone == null || telephone.isBlank()) {
            return null;
        }
        String nettoye = telephone.trim();
        if (!nettoye.matches(FORME_TELEPHONE)) {
            throw new BusinessException(
                    "Le numéro de téléphone n'est pas dans un format reconnu.", HttpStatus.BAD_REQUEST);
        }
        return nettoye;
    }
}
