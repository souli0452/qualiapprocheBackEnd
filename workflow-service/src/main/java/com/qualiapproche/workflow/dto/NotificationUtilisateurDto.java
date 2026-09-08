package com.qualiapproche.workflow.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.qualiapproche.common.enumeration.GraviteNotification;
import com.qualiapproche.workflow.model.NotificationUtilisateur;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Une ligne de la boîte, telle que l'écran la reçoit.
 *
 * <p>Le destinataire n'y figure pas : c'est toujours l'appelant, et le transporter aurait laissé
 * croire qu'on peut demander la boîte d'un autre.</p>
 */
@Builder
@Schema(description = "Une notification déposée dans la boîte de l'appelant.")
public record NotificationUtilisateurDto(
        UUID id,
        @Schema(description = "Repère stable de la nature de la ligne, sur lequel l'écran branche "
                + "son icône. Il ne suit pas le titre, qui vient du circuit et peut être reformulé.",
                example = "DOSSIER_A_DECIDER")
        String code,
        @Schema(description = "Domaine dont la ligne provient, pour regrouper l'affichage.",
                example = "NON_CONFORMITE")
        String source,
        @Schema(description = "Le nom que le circuit donne à l'étape atteinte.", example = "Imputation")
        String titre,
        @Schema(description = "Phrase à afficher telle quelle : c'est le serveur qui accorde le français.",
                example = "Le dossier NFQT-GSI-2026-0042 attend votre décision à l'étape « Imputation ».")
        String message,
        String resourceId,
        String resourceType,
        @Schema(description = "Où mène la ligne quand on la clique.")
        String lien,
        GraviteNotification gravite,
        boolean lue,
        LocalDateTime lueLe,
        @Schema(description = "Quand la ligne a été déposée.")
        LocalDateTime creeLe) {

    public static NotificationUtilisateurDto de(NotificationUtilisateur n) {
        return NotificationUtilisateurDto.builder()
                .id(n.getId())
                .code(n.getCode())
                .source(n.getSource())
                .titre(n.getTitre())
                .message(n.getMessage())
                .resourceId(n.getResourceId())
                .resourceType(n.getResourceType())
                .lien(n.getLien())
                .gravite(n.getGravite())
                .lue(n.isLue())
                .lueLe(n.getLueLe())
                .creeLe(n.getCreatedAt())
                .build();
    }
}
