package com.qualiapproche.support.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qualiapproche.common.dto.NotificationDto;
import com.qualiapproche.common.enumeration.GraviteNotification;
import com.qualiapproche.common.enumeration.SourceNotification;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.support.service.DemandeDocumentService;
import com.qualiapproche.support.service.QmsDocumentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ce que l'utilisateur connecté a en attente dans le module documentaire.
 *
 * <p>Recalculé à chaque appel, jamais conservé : une notification n'est que la lecture d'un travail
 * qui attend, et un document visé entre deux consultations disparaît de lui-même.</p>
 *
 * <p>Sans habilitation propre : les deux listes sous-jacentes sont déjà bornées à ce que l'appelant
 * peut voir et décider. En exiger une seconde ici aurait pu masquer à quelqu'un le dossier que le
 * circuit lui ouvre pourtant.</p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Ce que l'utilisateur connecté a en attente")
public class NotificationsController {

    public static final String CODE_DOCUMENT_A_TRAITER = "DOCUMENT_A_TRAITER";
    public static final String CODE_DEMANDE_A_INSTRUIRE = "DEMANDE_DOCUMENT_A_INSTRUIRE";

    private final QmsDocumentService documentService;
    private final DemandeDocumentService demandeService;

    /**
     * Les lignes du documentaire.
     *
     * <p>Enveloppé explicitement : {@code GlobalResponseHandler} pagine d'office toute réponse de
     * type {@code List}, à dix éléments.</p>
     *
     * <p>L'échec d'une des deux lectures ne vide pas l'autre : un incident sur les demandes ne doit
     * pas faire disparaître les documents que l'on sait par ailleurs.</p>
     */
    @Operation(summary = "Notifications documentaires de l'utilisateur connecté")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> notifications() {
        List<NotificationDto> lignes = new ArrayList<>();
        ligne(CODE_DOCUMENT_A_TRAITER, "Documents à traiter", "document", this::documentsATraiter)
                .ifPresent(lignes::add);
        ligne(CODE_DEMANDE_A_INSTRUIRE, "Demandes à instruire", "demande", this::demandesAInstruire)
                .ifPresent(lignes::add);
        return ResponseEntity.ok(ApiResponse.success(lignes));
    }

    private long documentsATraiter() {
        return documentService.aTraiterParLAppelant().size();
    }

    private long demandesAInstruire() {
        return demandeService.aTraiterParLAppelant().size();
    }

    /**
     * Compte une source et en fait une ligne, ou rien si elle est vide ou momentanément muette.
     *
     * @param nom le nom du dossier compté, au singulier, pour accorder la phrase
     */
    private java.util.Optional<NotificationDto> ligne(String code, String titre, String nom,
                                                      java.util.function.LongSupplier compte) {
        long nombre;
        try {
            nombre = compte.getAsLong();
        } catch (Exception e) {
            log.warn("Notifications documentaires : « {} » indisponible : {}", titre, e.getMessage());
            return java.util.Optional.empty();
        }
        if (nombre == 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(NotificationDto.builder()
                .source(SourceNotification.DOCUMENTAIRE)
                .code(code)
                .titre(titre)
                .detail(nombre == 1
                        ? "Un " + nom + " attend votre décision."
                        : nombre + " " + nom + "s attendent votre décision.")
                .gravite(GraviteNotification.ATTENTION)
                .nombre(nombre)
                .build());
    }
}
