package com.qualiapproche.workflow.service;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.common.utils.RolesPlateforme;
import com.qualiapproche.workflow.client.UserRoleClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Destinataires à prévenir lorsqu'un dossier atteint une étape.
 *
 * <p>Une étape ne désigne qu'un <b>rôle</b> responsable ; les personnes qui l'occupent sont
 * connues de user-service seul. Cette résolution n'existait pas : l'adresse était fabriquée en
 * accolant le nom du rôle à un domaine ({@code role_VERIFICATEUR@qualiapproche.com}), une boîte
 * qui n'a jamais existé. Aucune notification d'étape n'atteignait donc son destinataire, sans que
 * rien ne le signale — l'envoi vers une adresse syntaxiquement valide n'échoue pas.</p>
 *
 * <p>Les affectations de rôle changent rarement, alors qu'une même étape est franchie souvent :
 * un cache de courte durée absorbe les rafales sans retarder sensiblement la prise en compte
 * d'une nouvelle affectation.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DestinatairesEtapeService {

    private final UserRoleClient userRoleClient;

    @Value("${workflow.destinataires.cache-seconds:120}")
    private long retentionSecondes;

    private final Map<String, Entree> cache = new ConcurrentHashMap<>();

    private record Entree(List<DestinataireDto> destinataires, Instant expiration) {
        boolean estPerimee() {
            return Instant.now().isAfter(expiration);
        }
    }

    /**
     * Utilisateurs à prévenir pour un rôle responsable, dans la structure où le dossier se trouve.
     *
     * <p>Le rôle seul ne suffit pas : il est porté dans toutes les structures, et l'interroger sans
     * borne écrivait à la plateforme entière à chaque franchissement. La restriction est appliquée
     * par user-service, qui sait aussi ne pas l'opposer aux rôles à portée globale — administration,
     * responsabilité qualité.</p>
     *
     * @param structureId structure du dossier, ou {@code null} pour ne pas restreindre — dossiers
     *                    antérieurs à la colonne, déclarant sans structure
     * @return les destinataires, éventuellement vide — jamais {@code null}
     */
    public List<DestinataireDto> destinatairesDuRole(String role, String structureId) {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        if (RolesPlateforme.HABILITATION_TITULAIRE.equalsIgnoreCase(role.trim())
                || RolesPlateforme.HABILITATION_CREATEUR.equalsIgnoreCase(role.trim())) {
            // Ni l'un ni l'autre n'est un rôle : personne ne les porte, et les interroger comme tels
            // ne renverrait jamais personne. Le destinataire est une personne désignée sur le dossier
            // — son titulaire, son créateur — que seul l'appelant connaît : voir
            // destinataire(String utilisateurId).
            return List.of();
        }
        String aStructure = structureId == null || structureId.isBlank() ? null : structureId.trim();
        // La structure entre dans la clé : un même rôle n'a pas les mêmes porteurs d'une structure
        // à l'autre, et une entrée par rôle seul servirait la liste d'un dossier à un autre.
        String aCle = role.trim().toUpperCase() + "|" + (aStructure == null ? "" : aStructure);

        Entree aEntree = cache.get(aCle);
        if (aEntree != null && !aEntree.estPerimee()) {
            return aEntree.destinataires();
        }

        List<DestinataireDto> aDestinataires = interroger(role.trim(), aStructure);
        if (aDestinataires != null) {
            cache.put(aCle, new Entree(aDestinataires,
                    Instant.now().plus(Duration.ofSeconds(retentionSecondes))));
            return aDestinataires;
        }

        // user-service injoignable : la dernière liste connue vaut mieux que pas de notification.
        if (aEntree != null) {
            log.warn("Destinataires du rôle {} indisponibles : réutilisation de la dernière liste connue.", role);
            return aEntree.destinataires();
        }
        return List.of();
    }

    /**
     * Destinataire unique, désigné nommément.
     *
     * <p>Une étape réservée au titulaire du dossier ne porte pas de rôle : il n'y a personne à
     * chercher par rôle, et seul l'appelant sait de qui il s'agit. Sans cette lecture, l'agent à
     * qui une non-conformité vient d'être imputée n'était prévenu par rien.</p>
     *
     * @return une liste d'au plus un destinataire, jamais {@code null}
     */
    public List<DestinataireDto> destinataire(String utilisateurId) {
        if (utilisateurId == null || utilisateurId.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> reponse = userRoleClient.getUserById(utilisateurId.trim());
            Object donnees = reponse != null ? reponse.getOrDefault("data", reponse) : null;
            if (!(donnees instanceof Map<?, ?> utilisateur)) {
                log.warn("Réponse inattendue de user-service pour l'utilisateur {}.", utilisateurId);
                return List.of();
            }
            String email = texte(utilisateur.get("email"));
            if (email == null) {
                log.warn("L'utilisateur {} n'a pas d'adresse : il ne peut pas être prévenu.", utilisateurId);
                return List.of();
            }
            String nom = texte(utilisateur.get("nomComplet"));
            return List.of(DestinataireDto.builder()
                    .userId(utilisateurId.trim())
                    .email(email)
                    .nomComplet(nom != null ? nom : email)
                    .build());
        } catch (Exception e) {
            log.error("Impossible de récupérer l'utilisateur {} auprès de user-service : {}",
                    utilisateurId, e.getMessage());
            return List.of();
        }
    }

    /** @return les destinataires, ou {@code null} si l'interrogation a échoué. */
    private List<DestinataireDto> interroger(String role, String structureId) {
        try {
            Map<String, Object> aReponse = userRoleClient.getUsersByRole(role, structureId);
            Object aData = aReponse != null ? aReponse.get("data") : null;
            if (!(aData instanceof List<?> aListe)) {
                log.warn("Réponse inattendue de user-service pour les porteurs du rôle {}.", role);
                return List.of();
            }

            List<DestinataireDto> aDestinataires = new ArrayList<>();
            for (Object aElement : aListe) {
                if (aElement instanceof Map<?, ?> aUtilisateur) {
                    String aEmail = texte(aUtilisateur.get("email"));
                    if (aEmail != null) {
                        aDestinataires.add(DestinataireDto.builder()
                                .userId(texte(aUtilisateur.get("userId")))
                                .email(aEmail)
                                .nomComplet(texte(aUtilisateur.get("nomComplet")) != null
                                        ? texte(aUtilisateur.get("nomComplet")) : aEmail)
                                .build());
                    }
                }
            }
            return aDestinataires;
        } catch (Exception e) {
            log.error("Impossible de récupérer les porteurs du rôle {} auprès de user-service : {}",
                    role, e.getMessage());
            return null;
        }
    }

    private String texte(Object valeur) {
        if (valeur == null) {
            return null;
        }
        String aTexte = valeur.toString().trim();
        return aTexte.isEmpty() ? null : aTexte;
    }
}
