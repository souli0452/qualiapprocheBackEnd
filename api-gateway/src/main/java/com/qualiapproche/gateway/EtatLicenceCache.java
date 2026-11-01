package com.qualiapproche.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * L'état de la licence, relu périodiquement auprès de referentiel-service.
 *
 * <p>La gateway voit passer chaque requête : interroger le référentiel à chacune multiplierait
 * les appels sans rien apprendre de neuf — une licence ne change qu'à son installation. Une
 * rétention de quelques dizaines de secondes absorbe les rafales, et une licence tout juste
 * installée s'applique au plus tard une minute après.</p>
 *
 * <p><b>Une indisponibilité du référentiel ne ferme pas la plateforme.</b> Le dernier état connu
 * fait foi, et à défaut on laisse passer : une panne de service ne doit pas se traduire, pour le
 * client, par un « abonnement expiré » qu'il n'a aucun moyen de corriger. Le risque inverse — un
 * abonné laissé libre quelques minutes de trop pendant une panne — n'a pas de conséquence.</p>
 */
@Component
@Slf4j
public class EtatLicenceCache {

    /** Ce que la gateway retient d'une licence : de quoi décider, rien de plus. */
    public record Etat(boolean actionsOuvertes, List<String> modules, String message) {

        public boolean ouvre(String module) {
            return modules != null && modules.stream().anyMatch(m -> m.equalsIgnoreCase(module));
        }
    }

    /** Laisse tout passer : employé quand l'état n'a jamais pu être lu. */
    private static final Etat INCONNU = new Etat(true, List.of(), null);

    /** L'en-tête qui désigne le client (Cloisonnement.EN_TETE). */
    static final String EN_TETE_DIRECTION = "X-Direction-Id";

    private final WebClient webClient;
    private final long retentionSecondes;

    /** Ce qui a été lu pour une direction, et quand. */
    private record Lecture(Etat etat, Instant le) {
    }

    /** Chaque client a sa licence : l'état est retenu par direction. */
    private final Map<String, Lecture> parDirection = new ConcurrentHashMap<>();

    public EtatLicenceCache(WebClient.Builder loadBalancedWebClientBuilder,
                            @Value("${gateway.licence-cache-ttl-seconds:60}") long retentionSecondes) {
        this.webClient = loadBalancedWebClientBuilder.build();
        this.retentionSecondes = retentionSecondes;
    }

    /**
     * L'état de la licence du client {@code direction}.
     *
     * <p>referentiel-service la sert sans jeton à qui annonce la direction dans
     * {@code X-Direction-Id} : la passerelle la tire du jeton de l'appelant.</p>
     */
    public Mono<Etat> etat(String direction) {
        Lecture connue = parDirection.get(direction);
        if (connue != null && Instant.now().isBefore(connue.le().plusSeconds(retentionSecondes))) {
            return Mono.just(connue.etat());
        }

        return webClient.get()
                .uri("http://REFERENTIEL-SERVICE/api/v1/licence/etat")
                .header(EN_TETE_DIRECTION, direction)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(3))
                .map(this::lire)
                .doOnNext(etat -> parDirection.put(direction, new Lecture(etat, Instant.now())))
                .onErrorResume(e -> {
                    Etat precedent = connue != null ? connue.etat() : null;
                    log.warn("État de la licence de {} indisponible ({}) : {}", direction, e.getMessage(),
                            precedent != null ? "le dernier état connu est réutilisé"
                                    : "les actions restent ouvertes le temps du rétablissement");
                    return Mono.just(precedent != null ? precedent : INCONNU);
                });
    }

    private Etat lire(JsonNode reponse) {
        // referentiel-service enveloppe ses réponses dans ApiResponse ; on accepte les deux
        // formes pour ne pas dépendre de cette enveloppe.
        JsonNode corps = reponse.has("data") ? reponse.get("data") : reponse;

        List<String> modules = new java.util.ArrayList<>();
        JsonNode declares = corps.path("modules");
        if (declares.isArray()) {
            declares.forEach(module -> modules.add(module.asText()));
        }

        return new Etat(
                // Ouvert par défaut : une réponse mal formée ne doit pas fermer la plateforme.
                corps.path("actionsOuvertes").asBoolean(true),
                modules,
                corps.path("message").asText(null));
    }
}
