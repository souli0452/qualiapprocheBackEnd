package com.qualiapproche.workflow.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.qualiapproche.common.dto.DepotNotificationDto;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.workflow.dto.NotificationUtilisateurDto;
import com.qualiapproche.workflow.model.NotificationUtilisateur;
import com.qualiapproche.workflow.service.NotificationsUtilisateurService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * La boîte de réception de l'appelant.
 *
 * <p>Deux familles d'appels, et elles ne se protègent pas de la même façon :</p>
 * <ul>
 *   <li><b>La lecture</b> vient du front. Aucune permission ne la garde, et c'est délibéré : elle
 *       ne sert que l'appelant, dont l'identité vient du jeton. Aucun paramètre ne permet de
 *       désigner quelqu'un d'autre — c'est ce qui rend inutile de vérifier qui a le droit de lire
 *       quoi.</li>
 *   <li><b>Le dépôt</b> vient d'un service : la relance d'échéance d'amelioration-service, l'alerte
 *       de révision de support-service. Réservé aux appels de service à service, faute de quoi
 *       n'importe quel compte authentifié pourrait déposer ce qu'il veut dans la boîte d'autrui.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationsUtilisateurController {

    /** Au-delà, la boîte se parcourt page par page : personne ne lit trois cents lignes d'un coup. */
    private static final int TAILLE_MAX = 100;

    private final NotificationsUtilisateurService service;

    @Operation(summary = "Ma boîte de réception",
            description = "Les notifications de l'appelant, les plus récentes d'abord. "
                    + "Aucun paramètre ne désigne une autre personne : l'identité vient du jeton.")
    @GetMapping("/mes")
    public ResponseEntity<Page<NotificationUtilisateurDto>> mesNotifications(
            @RequestParam(defaultValue = "false") boolean seulementNonLues,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationUtilisateur> lignes = service.mesNotifications(
                seulementNonLues, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, TAILLE_MAX)));
        // Rendue nue : GlobalResponseHandler reconnaît une Page et l'enveloppe en
        // PaginatedResponse — la forme que tout le reste de l'API sert, et que le front sait déjà
        // lire. L'envelopper ici sérialiserait un PageImpl tel quel, une forme que Spring 3.3
        // déconseille et qui n'est celle d'aucun autre point d'entrée.
        return ResponseEntity.ok(lignes.map(NotificationUtilisateurDto::de));
    }

    @Operation(summary = "Combien de notifications non lues",
            description = "Le nombre que porte la pastille.")
    @GetMapping("/non-lues/nombre")
    public ResponseEntity<ApiResponse<Long>> nombreDeNonLues() {
        return ResponseEntity.ok(ApiResponse.success(service.nombreDeNonLues()));
    }

    @Operation(summary = "Marquer une notification comme lue")
    @PostMapping("/{id}/lue")
    public ResponseEntity<ApiResponse<Boolean>> marquerLue(@PathVariable UUID id) {
        // Une ligne qui n'est pas la sienne est déclarée introuvable, et non refusée : répondre
        // « interdit » confirmerait qu'une notification porte bien cet identifiant.
        if (!service.marquerLue(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable.");
        }
        return ResponseEntity.ok(ApiResponse.success(true));
    }

    @Operation(summary = "Tout marquer comme lu",
            description = "Marque lues toutes les notifications non lues de l'appelant. "
                    + "Rend le nombre de lignes concernées.")
    @PostMapping("/toutes-lues")
    public ResponseEntity<ApiResponse<Integer>> toutMarquerLu() {
        return ResponseEntity.ok(ApiResponse.success(service.toutMarquerLu()));
    }

    @Operation(summary = "Déposer une notification (appel de service à service)",
            description = "Réservé aux services : la relance d'échéance d'un plan d'action, "
                    + "l'alerte de révision d'un document. Un module y annonce quelque chose sans "
                    + "rien connaître du circuit.")
    @PreAuthorize("@perm.appelDeService()")
    @PostMapping("/depot")
    public ResponseEntity<ApiResponse<Void>> deposer(
            @RequestBody DepotNotificationDto depot) {
        service.deposer(depot);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
