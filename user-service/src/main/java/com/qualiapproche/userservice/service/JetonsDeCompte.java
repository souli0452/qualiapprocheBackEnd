package com.qualiapproche.userservice.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.entities.JetonDeCompte;
import com.qualiapproche.userservice.entities.JetonDeCompte.Usage;
import com.qualiapproche.userservice.repository.JetonDeCompteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Émet et consomme les jetons des liens envoyés par courriel.
 *
 * <p>Un jeton vaut pour <b>un</b> compte, <b>un</b> usage, <b>une</b> fois, et pas au-delà de sa
 * durée. Toute autre présentation reçoit la même réponse — « lien invalide ou expiré » — pour ne
 * rien dire de ce qui a manqué.</p>
 */
@Service
public class JetonsDeCompte {

    /** Une vérification d'adresse attend que le nouvel arrivant ouvre sa messagerie. */
    public static final Duration DUREE_VERIFICATION = Duration.ofDays(7);
    /** Une réinitialisation se fait dans la foulée de la demande. */
    public static final Duration DUREE_REINITIALISATION = Duration.ofMinutes(30);

    private static final SecureRandom HASARD = new SecureRandom();

    private final JetonDeCompteRepository depot;
    private final Clock horloge;

    @org.springframework.beans.factory.annotation.Autowired
    public JetonsDeCompte(JetonDeCompteRepository depot) {
        this(depot, Clock.systemDefaultZone());
    }

    /** Pour les tests : une horloge que l'on fait avancer. */
    JetonsDeCompte(JetonDeCompteRepository depot, Clock horloge) {
        this.depot = depot;
        this.horloge = horloge;
    }

    /** Un nouveau jeton, en clair : il part dans le lien et n'est gardé que par son empreinte. */
    @Transactional
    public String emettre(String userId, Usage usage, Duration duree) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        depot.annulerLesPrecedents(userId, usage, maintenant);
        byte[] octets = new byte[32];
        HASARD.nextBytes(octets);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        depot.save(JetonDeCompte.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .usage(usage)
                .empreinte(empreinte(jeton))
                .creeLe(maintenant)
                .expireLe(maintenant.plus(duree))
                .build());
        return jeton;
    }

    /**
     * Consomme le jeton, ou refuse en 400 s'il n'est pas valide pour ce compte et cet usage.
     *
     * <p>Deux présentations simultanées du même jeton n'en laissent passer qu'une
     * ({@link JetonDeCompteRepository#marquerServi}).</p>
     */
    @Transactional
    public void consommer(String userId, Usage usage, String jeton) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        JetonDeCompte trouve = (jeton == null || jeton.isBlank()) ? null
                : depot.findByEmpreinte(empreinte(jeton)).orElse(null);
        boolean valide = trouve != null
                && trouve.getUserId().equals(userId)
                && trouve.getUsage() == usage
                && trouve.getUtiliseLe() == null
                && trouve.getExpireLe().isAfter(maintenant);
        // La consommation elle-même est conditionnelle et atomique : si une requête simultanée a
        // servi le jeton entre la lecture ci-dessus et maintenant, aucune ligne n'est touchée.
        if (!valide || depot.marquerServi(trouve.getId(), maintenant) != 1) {
            throw new BusinessException("Ce lien est invalide ou a expiré. Demandez-en un nouveau.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    static String empreinte(String jeton) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
