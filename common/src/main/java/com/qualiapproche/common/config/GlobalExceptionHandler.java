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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), ex.getStatus().value());
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value());
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        ApiResponse<Void> response = ApiResponse.error("Une erreur interne est survenue: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
