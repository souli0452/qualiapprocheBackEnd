package com.qualiapproche.workflow.service;

import com.qualiapproche.common.dto.DestinataireDto;
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
     * Utilisateurs à prévenir pour un rôle responsable.
     *
     * @return les destinataires, éventuellement vide — jamais {@code null}
     */
    public List<DestinataireDto> destinatairesDuRole(String role) {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        String aCle = role.trim().toUpperCase();

        Entree aEntree = cache.get(aCle);
        if (aEntree != null && !aEntree.estPerimee()) {
            return aEntree.destinataires();
        }

        List<DestinataireDto> aDestinataires = interroger(role.trim());
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

    /** @return les destinataires, ou {@code null} si l'interrogation a échoué. */
    private List<DestinataireDto> interroger(String role) {
        try {
            Map<String, Object> aReponse = userRoleClient.getUsersByRole(role);
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
