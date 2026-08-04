package com.qualiapproche.support.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Restitue au client l'erreur métier renvoyée par un service appelé, au lieu d'une panne.
 *
 * <p>Une réponse 4xx d'un service aval remontait dans le gestionnaire générique et devenait un
 * 500 « Une erreur interne est survenue », précédé de la trace complète de l'appel Feign. Le
 * client recevait donc une erreur serveur là où le refus était parfaitement légitime et
 * explicable — le message rédigé par le service appelé, seul utile, se retrouvait noyé au milieu
 * d'une URL et d'une signature de méthode :</p>
 *
 * <pre>
 * 500 : "Une erreur interne est survenue: [400] during [POST] to
 *        [http://workflow-service/api/v1/workflows/initiate?...] [WorkflowClient#initiateWorkflow]:
 *        [{"message":"Le circuit « wok » s'applique aux ressources de type PRO, pas DOCUMENT."}]"
 * </pre>
 *
 * <p>Le statut d'origine et le seul message sont désormais réémis tels quels. Une panne réelle du
 * service aval (5xx) reste signalée comme telle, sans être maquillée en erreur de saisie.</p>
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
@Order(0) // avant le gestionnaire générique de common.config
public class FeignErrorAdvice {

    private final ObjectMapper objectMapper;

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException exception) {
        HttpStatus statut = statutDe(exception);
        String message = messageDe(exception, statut);

        if (statut.is5xxServerError()) {
            log.error("Le service appelé a échoué ({}) : {}", statut.value(), exception.getMessage());
        } else {
            log.info("Refus métier du service appelé ({}) : {}", statut.value(), message);
        }

        return new ResponseEntity<>(ApiResponse.error(message, statut.value()), statut);
    }

    /** Statut renvoyé par le service aval ; 503 si l'appel n'a même pas abouti. */
    private HttpStatus statutDe(FeignException exception) {
        HttpStatus statut = HttpStatus.resolve(exception.status());
        // status() vaut -1 quand la réponse n'a jamais été reçue : service arrêté, délai dépassé.
        return statut != null ? statut : HttpStatus.SERVICE_UNAVAILABLE;
    }

    /**
     * Message rédigé par le service appelé, extrait de l'enveloppe {@code ApiResponse}.
     *
     * <p>À défaut d'un corps exploitable, un libellé neutre est rendu plutôt que la trace de
     * l'appel : elle expose l'URL interne et la signature du client, sans rien apprendre à
     * l'utilisateur.</p>
     */
    private String messageDe(FeignException exception, HttpStatus statut) {
        try {
            String corps = exception.contentUTF8();
            if (corps != null && !corps.isBlank()) {
                JsonNode noeud = objectMapper.readTree(corps);
                JsonNode message = noeud.get("message");
                if (message != null && !message.asText().isBlank()) {
                    return message.asText();
                }
            }
        } catch (Exception e) {
            log.debug("Corps d'erreur du service appelé illisible : {}", e.getMessage());
        }

        return statut.is5xxServerError() || statut == HttpStatus.SERVICE_UNAVAILABLE
                ? "Un service nécessaire à cette opération est indisponible. Réessayez dans un instant."
                : "L'opération a été refusée par le service concerné.";
    }
}
