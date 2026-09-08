package com.qualiapproche.common.dto;

import com.qualiapproche.common.enumeration.GraviteNotification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Ce qu'un producteur a à déposer dans la boîte de quelqu'un.
 *
 * <p>Dans {@code common} parce que les deux côtés en dépendent : workflow-service qui reçoit, et
 * les modules qui déposent depuis leurs relances d'échéance. Une forme recopiée de chaque côté
 * aurait divergé au premier champ ajouté, et l'écart ne se serait vu qu'à l'exécution.</p>
 */
@Builder
@Schema(description = "Une notification à déposer dans la boîte d'une personne.")
public record DepotNotificationDto(

        @Schema(description = "À qui la ligne s'adresse : l'identifiant de la personne, jamais un rôle.")
        String destinataireId,

        @Schema(description = "Ce qui distingue cette ligne d'une autre chez la même personne. "
                + "Deux dépôts sur la même clé ne font qu'une ligne — c'est ce qui empêche une "
                + "relance quotidienne d'empiler sept fois la même échéance en une semaine.",
                example = "PLAN_ACTION_ECHEANCE:3e005487-6c68-4370-96d6-8e3a0a4d0340")
        String cleUnicite,

        @Schema(description = "Repère stable de la nature de la ligne, sur lequel l'écran branche "
                + "son icône et sa destination.", example = "PLAN_ACTION_ECHEANCE_DEPASSEE")
        String code,

        @Schema(description = "Domaine dont la ligne provient.", example = "AMELIORATION")
        String source,

        String titre,

        @Schema(description = "Phrase à afficher telle quelle : c'est le producteur qui compte les "
                + "dossiers et qui accorde le français.")
        String message,

        String resourceId,
        String resourceType,

        @Schema(description = "Où mène la ligne quand on la clique.")
        String lien,

        GraviteNotification gravite,

        @Schema(description = "Un nouveau dépôt sur une ligne déjà lue doit-il la rendre non lue ? "
                + "Vrai pour un franchissement — le dossier revient, il faut y regarder à nouveau. "
                + "Faux pour une relance d'échéance, sans quoi marquer lu ne servirait à rien : la "
                + "ligne redeviendrait non lue le lendemain matin.")
        boolean reveiller) {
}
