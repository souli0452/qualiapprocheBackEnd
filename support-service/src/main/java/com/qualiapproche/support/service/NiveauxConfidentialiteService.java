package com.qualiapproche.support.service;

import com.qualiapproche.common.dto.NiveauConfidentialiteDto;
import com.qualiapproche.support.client.ReferentielClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Niveaux de confidentialité et rôles qu'ils exigent, tels que les tient referentiel-service.
 *
 * <p>La restriction est évaluée à chaque recherche et à chaque consultation. Les niveaux sont peu
 * nombreux et changent rarement : ils sont donc chargés en bloc et conservés quelques minutes,
 * plutôt qu'interrogés document par document.</p>
 *
 * <p><b>Référentiel injoignable : la restriction est maintenue, pas levée.</b> Sans la liste des
 * rôles, on ne peut pas établir qu'un utilisateur a le droit de voir un document classé — et un
 * document confidentiel qui s'ouvre à la faveur d'une panne est exactement ce que ce niveau existe
 * pour empêcher. Les documents sans niveau, eux, restent visibles.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NiveauxConfidentialiteService {

    private final ReferentielClient referentielClient;

    @Value("${support.confidentialite.cache-seconds:300}")
    private long retentionSecondes;

    private volatile Map<String, Set<String>> rolesParNiveau = Map.of();
    private volatile List<NiveauConfidentialiteDto> catalogue = List.of();
    private volatile Instant expiration = Instant.EPOCH;
    private volatile boolean referentielLu = false;

    /**
     * Niveaux que cet utilisateur a le droit de consulter, dans l'ordre du référentiel.
     *
     * <p>Sert à composer le filtre de recherche : proposer un niveau qu'on n'a pas le droit de voir
     * reviendrait à offrir un critère qui ne rendrait jamais rien, et à révéler au passage
     * l'existence d'un classement qui ne nous regarde pas.</p>
     *
     * <p>Le responsable qualité n'y échappe pas : voir toutes les structures dispense de la
     * barrière de structure, non du classement. Lui proposer un niveau dont il n'a pas le rôle
     * lui rendrait une liste vide, sans qu'il comprenne pourquoi.</p>
     *
     * @param estAdministrateur vrai pour l'administration générale, seule dispensée du classement :
     *                          le référentiel lui est alors rendu entier
     */
    public List<NiveauConfidentialiteDto> visiblesPour(Set<String> rolesUtilisateur,
                                                       boolean estAdministrateur) {
        niveaux();
        if (estAdministrateur) {
            return catalogue;
        }
        if (!referentielLu) {
            return List.of();
        }
        Set<String> interdits = niveauxInterdits(rolesUtilisateur);
        return catalogue.stream()
                .filter(n -> n.getId() == null || !interdits.contains(n.getId().toString()))
                .toList();
    }

    /**
     * Identifiants des niveaux que cet utilisateur n'a pas le droit de consulter, compte tenu de
     * ses rôles. Un niveau sans rôle exigé n'y figure jamais.
     */
    public Set<String> niveauxInterdits(Set<String> rolesUtilisateur) {
        Map<String, Set<String>> niveaux = niveaux();
        if (!referentielLu) {
            // Rien n'a jamais pu être lu : on ne sait pas quels niveaux existent, donc on ne peut
            // interdire nommément. La garde se joue alors sur `restrictionIndecidable`.
            return Set.of();
        }
        return niveaux.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .filter(e -> e.getValue().stream().noneMatch(rolesUtilisateur::contains))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Vrai lorsque le référentiel n'a jamais pu être lu : tout document classé doit alors être
     * écarté, faute de pouvoir établir qui a le droit de le voir.
     */
    public boolean restrictionIndecidable() {
        niveaux();
        return !referentielLu;
    }

    /** Cet utilisateur peut-il voir un document de ce niveau ? */
    public boolean peutVoir(String niveauId, Set<String> rolesUtilisateur) {
        if (niveauId == null || niveauId.isBlank()) {
            return true;
        }
        if (restrictionIndecidable()) {
            return false;
        }
        Set<String> exiges = niveaux().getOrDefault(niveauId, Set.of());
        return exiges.isEmpty() || exiges.stream().anyMatch(rolesUtilisateur::contains);
    }

    /**
     * Rôles qu'admet ce niveau, vides s'il ne restreint rien ou s'il est inconnu.
     *
     * <p>Sert à confronter le classement d'un document au circuit qui doit le traiter : un
     * niveau qui n'admet aucun des rôles décidant des étapes immobilise le document.</p>
     */
    public Set<String> rolesAdmis(String niveauId) {
        if (niveauId == null || niveauId.isBlank()) {
            return Set.of();
        }
        return niveaux().getOrDefault(niveauId, Set.of());
    }

    private Map<String, Set<String>> niveaux() {
        if (Instant.now().isBefore(expiration)) {
            return rolesParNiveau;
        }
        try {
            List<NiveauConfidentialiteDto> niveaux = referentielClient.niveauxConfidentialite();
            rolesParNiveau = niveaux == null ? Map.of() : niveaux.stream()
                    .filter(n -> n.getId() != null)
                    .collect(Collectors.toMap(
                            n -> n.getId().toString(),
                            n -> n.getRolesAutorises() == null ? Set.<String>of()
                                    : n.getRolesAutorises().stream()
                                            .filter(java.util.Objects::nonNull)
                                            .map(r -> r.trim().toUpperCase())
                                            .filter(r -> !r.isEmpty())
                                            .collect(Collectors.toSet()),
                            (a, b) -> a));
            catalogue = niveaux == null ? List.of() : List.copyOf(niveaux);
            referentielLu = true;
            expiration = Instant.now().plus(Duration.ofSeconds(retentionSecondes));
        } catch (Exception e) {
            log.error("Niveaux de confidentialité indisponibles auprès de referentiel-service : {}",
                    e.getMessage());
            // Nouvelle tentative à la prochaine sollicitation plutôt qu'à l'expiration du cache :
            // la restriction est maximale entre-temps, autant en sortir dès que possible.
            expiration = Instant.EPOCH;
        }
        return rolesParNiveau;
    }
}
