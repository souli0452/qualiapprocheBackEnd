package com.qualiapproche.common.config;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), ex.getStatus().value());
        return new ResponseEntity<>(response, ex.getStatus());
    }

    /**
     * Introuvable : 404, avec le message du service quand il en a écrit un.
     *
     * <p>Celui d'Hibernate — « Unable to find com.qualiapproche…Entite with id … » — livrait le nom
     * complet de la classe persistée, donc l'organisation interne du code. Il est remplacé par un
     * message neutre ; les messages métier (« Non-conformité non trouvée… ») passent tels quels.</p>
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex) {
        String message = messageTechnique(ex.getMessage()) ? "Élément introuvable." : ex.getMessage();
        ApiResponse<Void> response = ApiResponse.error(message, HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Levée par JPA (via le champ @Version) quand deux mises à jour concurrentes portent sur la
     * même entité : la seconde à tenter d'écrire échoue proprement au lieu d'écraser silencieusement
     * la première ou de laisser un état incohérent (ex. deux transitions de workflow simultanées).
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        ApiResponse<Void> response = ApiResponse.error(
                "Cette ressource a été modifiée entre-temps par une autre action. Veuillez recharger et réessayer.",
                HttpStatus.CONFLICT.value());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Levée par @PreAuthorize("@perm.canX(this)") quand l'utilisateur authentifié n'a pas
     * l'autorité requise. Sans ce handler dédié, elle tombait dans le handler générique
     * ci-dessous et remontait en 500 "erreur interne" au lieu d'un 403 explicite.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        ApiResponse<Void> response = ApiResponse.error(
                "Accès refusé : vous n'avez pas la permission requise pour cette action.",
                HttpStatus.FORBIDDEN.value());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Argument refusé : 400.
     *
     * <p>Les services s'en servent pour dire en français ce qui ne va pas (« Type de document
     * introuvable… »), et ce message doit parvenir à l'utilisateur. Mais le JDK lève la même
     * exception pour ses propres raisons — {@code UUID.fromString}, {@code Enum.valueOf},
     * {@code Integer.parseInt} — et son message nomme alors des classes internes
     * (« No enum constant com.qualiapproche…Statut.X ») : c'est une carte du code offerte à qui
     * sonde l'API. Ceux-là sont remplacés par un message neutre et journalisés.</p>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (messageTechnique(message)) {
            LOG.warn("Argument refusé, message technique masqué à l'appelant : {}", message);
            message = "Requête invalide : une valeur fournie n'est pas reconnue.";
        }
        ApiResponse<Void> response = ApiResponse.error(message, HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /** Un nom de classe qualifié : {@code com.qualiapproche.X.}, {@code java.util.}… */
    private static final java.util.regex.Pattern NOM_DE_CLASSE = java.util.regex.Pattern.compile(
            "(?<![\\w.])(?:java|javax|jakarta|com|org|net|io|sun)\\.[a-z]\\w*\\.");

    /** Débuts de messages que le JDK ou Hibernate produisent, et qu'aucun service n'écrit. */
    private static final java.util.List<String> MESSAGES_DU_JDK = java.util.List.of(
            "No enum constant", "Invalid UUID string", "UUID string too large", "For input string",
            "argument type mismatch", "Illegal base64", "Unable to find", "Failed to convert",
            "Illegal character", "Name is null");

    /**
     * Le message vient-il de la mécanique Java plutôt que d'un service ?
     *
     * <p>Un message absent compte comme technique : il n'y a rien à montrer.</p>
     */
    static boolean messageTechnique(String message) {
        if (message == null || message.isBlank()) {
            return true;
        }
        return MESSAGES_DU_JDK.stream().anyMatch(message::contains) || NOM_DE_CLASSE.matcher(message).find();
    }

    /**
     * Corps illisible — JSON mal formé, champ du mauvais type, corps absent : 400.
     *
     * <p>Remontait en 500. Le message de Jackson n'est pas repris : il cite les classes Java
     * visées par la désérialisation.</p>
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return new ResponseEntity<>(ApiResponse.error("Le corps de la requête est absent ou mal formé.",
                HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
    }

    /** Paramètre obligatoire absent : erreur de l'appelant, 400. Son nom vient du code et peut être dit. */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return new ResponseEntity<>(ApiResponse.error("Paramètre obligatoire manquant : « "
                + ex.getParameterName() + " ».", HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
    }

    /**
     * Contrainte violée hors d'un corps {@code @Valid} — paramètre annoté, validation d'un service :
     * 400, avec les messages des contraintes, qui sont écrits pour l'appelant.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        String erreurs = ex.getConstraintViolations().stream()
                .map(v -> dernierSegment(v.getPropertyPath()) + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        return new ResponseEntity<>(ApiResponse.error("Erreur de validation: " + erreurs,
                HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
    }

    /** Validation des paramètres d'une méthode de contrôleur (Spring 6.1 et suivants) : même règle. */
    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            org.springframework.web.method.annotation.HandlerMethodValidationException ex) {
        String erreurs = ex.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        return new ResponseEntity<>(ApiResponse.error("Erreur de validation: " + erreurs,
                HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
    }

    /** Le chemin d'une propriété sans le nom de la méthode Java qui la portait. */
    private static String dernierSegment(jakarta.validation.Path chemin) {
        String nom = null;
        for (jakarta.validation.Path.Node noeud : chemin) {
            nom = noeud.getName();
        }
        return nom != null ? nom : String.valueOf(chemin);
    }

    /** Type de contenu non accepté : 415, et non 500. */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            org.springframework.web.HttpMediaTypeNotSupportedException ex) {
        return new ResponseEntity<>(ApiResponse.error("Type de contenu non pris en charge.",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    /** Verbe HTTP non pris en charge sur cette adresse : 405, avec l'en-tête {@code Allow} qu'exige la norme. */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        org.springframework.http.HttpHeaders entetes = new org.springframework.http.HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            entetes.setAllow(ex.getSupportedHttpMethods());
        }
        return new ResponseEntity<>(ApiResponse.error("Méthode HTTP non prise en charge pour cette adresse.",
                HttpStatus.METHOD_NOT_ALLOWED.value()), entetes, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /** Fichier trop volumineux : 413, que l'écran peut traduire en « fichier trop gros ». */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return new ResponseEntity<>(ApiResponse.error("Le fichier dépasse la taille maximale autorisée.",
                HttpStatus.PAYLOAD_TOO_LARGE.value()), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiResponse<Void> response = ApiResponse.error("Erreur de validation: " + errors, HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Restitue le statut porté par une {@link ResponseStatusException}.
     *
     * <p>Sans ce gestionnaire, ces exceptions tombaient dans le filet générique et repartaient
     * toutes en 500 : un refus métier explicite — document déjà validé, circuit non configuré,
     * ressource introuvable — devenait indiscernable d'une panne pour l'appelant, alors que le
     * statut voulu était porté par l'exception elle-même.</p>
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(
            ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return new ResponseEntity<>(ApiResponse.error(message, status.value()), status);
    }

    /**
     * Une écriture que la base refuse — clé étrangère, unicité — est un conflit avec l'état des
     * données, pas une panne. Les services qui savent nommer la cause le font eux-mêmes ; ceci est
     * le filet des autres, qui répondaient 500.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {
        LOG.warn("Écriture refusée par une contrainte de la base : {}", ex.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(ApiResponse.error("Opération refusée : elle contredit l'état des données "
                + "(élément encore employé ailleurs, ou déjà existant).", HttpStatus.CONFLICT.value()), HttpStatus.CONFLICT);
    }

    /** Un paramètre d'adresse du mauvais type (un identifiant qui n'est pas un UUID) est une erreur de l'appelant. */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return new ResponseEntity<>(ApiResponse.error("Valeur invalide pour « " + ex.getName() + " ».",
                HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
    }

    /**
     * Une adresse qui ne mène à rien est un 404 : elle remontait jusqu'au gestionnaire général, en 500.
     *
     * <p>Le chemin demandé n'est pas répété dans la réponse : renvoyer tel quel ce que l'appelant a
     * écrit dans l'URL est le terrain des injections réfléchies, que les scanners signalent.</p>
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return new ResponseEntity<>(ApiResponse.error("Ressource introuvable.",
                HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
    }

    /**
     * Même 404 quand aucun contrôleur ne répond à l'adresse.
     *
     * <p>C'est la forme que prend l'adresse inconnue dans un service qui déclare
     * {@code @EnableWebMvc} (user-service) : la configuration de Spring Boot s'y efface, aucun
     * gestionnaire de ressources ne vient lever {@code NoResourceFoundException}, et l'adresse
     * inconnue répondait 500.</p>
     */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            org.springframework.web.servlet.NoHandlerFoundException ex) {
        return new ResponseEntity<>(ApiResponse.error("Ressource introuvable.",
                HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
    }

    /**
     * Dernier recours : ce qu'aucun autre gestionnaire n'a reconnu.
     *
     * <p>La pile est <b>journalisée</b>. Elle ne l'était pas : l'appelant recevait « une erreur
     * interne est survenue » suivie du seul message de l'exception, et le serveur n'en gardait
     * aucune trace — un 500 en production ne laissait donc rien à examiner, ni ligne fautive, ni
     * cause. C'est la seule information qui compte ici, et la seule qui manquait.</p>
     *
     * <p>L'appelant ne reçoit plus le message de l'exception : il livrait des requêtes SQL, des
     * adresses internes de services, des noms de classes. Il reçoit une <b>référence</b>, que le
     * journal porte avec la pile — c'est elle qui relie une plainte d'utilisateur à sa cause. Les
     * exceptions porteuses d'un message destiné à l'utilisateur ont leur propre gestionnaire plus
     * haut.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        String reference = java.util.UUID.randomUUID().toString().substring(0, 8);
        LOG.error("Erreur non gérée remontée jusqu'au gestionnaire général [référence {}].", reference, ex);
        ApiResponse<Void> response = ApiResponse.error(
                "Une erreur interne est survenue (référence " + reference + "). "
                        + "Communiquez cette référence au support si le problème persiste.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
