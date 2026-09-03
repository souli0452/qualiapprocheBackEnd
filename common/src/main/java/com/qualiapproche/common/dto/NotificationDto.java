package com.qualiapproche.common.dto;

import com.qualiapproche.common.enumeration.GraviteNotification;
import com.qualiapproche.common.enumeration.SourceNotification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Une ligne de la cloche : ce qui attend l'utilisateur connecté, à l'instant où il regarde.
 *
 * <p>Rien n'est conservé. La liste est recalculée à chaque demande, et c'est ce qui la rend juste :
 * une notification n'a pas d'existence propre, elle n'est que la lecture d'un travail en attente.
 * Un dossier traité entre deux consultations disparaît de lui-même, sans qu'aucun geste n'ait à
 * marquer quoi que ce soit comme lu.</p>
 *
 * <p>Le back ne transporte ni icône ni classe de couleur : il dit ce que la ligne vaut
 * ({@link #gravite}) et ce qu'elle désigne ({@link #code}), l'écran décide du reste. Sans quoi la
 * moindre retouche de charte graphique passerait par une livraison du serveur.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Une ligne de la cloche : un travail qui attend l'utilisateur connecté. "
        + "Recalculée à chaque appel et jamais conservée, elle disparaît d'elle-même une fois le "
        + "dossier traité — il n'y a donc ni état « lu » ni historique.")
public class NotificationDto {

    @Schema(description = "Domaine dont la ligne provient. Sert à regrouper l'affichage et à "
            + "signaler un module momentanément muet.",
            example = "AMELIORATION")
    private SourceNotification source;

    /**
     * Repère stable de la nature de la ligne, sur lequel l'écran branche sa destination et son
     * icône. Stable veut dire : il ne suit pas le libellé, qui lui peut être reformulé.
     */
    @Schema(description = "Repère stable de la nature de la ligne, sur lequel brancher la "
            + "destination et l'icône. Il ne suit pas le libellé, qui peut être reformulé sans "
            + "prévenir.",
            example = "PLAN_ACTION_ECHEANCE_DEPASSEE",
            allowableValues = {"NC_A_DECIDER", "PLAN_ACTION_A_DECIDER",
                    "PLAN_ACTION_ECHEANCE_DEPASSEE", "PLAN_ACTION_ECHEANCE_PROCHE", "NC_BROUILLON",
                    "DOCUMENT_A_TRAITER", "DEMANDE_DOCUMENT_A_INSTRUIRE",
                    "LICENCE_EXPIREE", "LICENCE_BIENTOT_EXPIREE"})
    private String code;

    @Schema(description = "Ce que la ligne annonce, en une formule courte. Pour les décisions "
            + "ouvertes, c'est le nom que le circuit donne lui-même à l'étape.",
            example = "Plans d'action en retard")
    private String titre;

    @Schema(description = "Phrase à afficher telle quelle : c'est le serveur qui compte les "
            + "dossiers et qui accorde le français.",
            example = "3 plans d'action dont vous répondez ont dépassé leur échéance.")
    private String detail;

    @Schema(description = "Ce que la ligne réclame de son destinataire. L'écran y branche sa "
            + "couleur et son icône ; le serveur n'en transporte aucune.",
            example = "URGENT")
    private GraviteNotification gravite;

    /**
     * Nombre de dossiers que la ligne représente.
     *
     * <p>La cloche en fait la somme pour sa pastille. Elle comptait jusqu'ici ses propres lignes,
     * ce qui annonçait « 3 » pour trois catégories portant vingt dossiers.</p>
     */
    @Schema(description = "Nombre de dossiers que la ligne représente. La pastille en fait la "
            + "somme : compter les lignes annoncerait « 3 » pour trois catégories portant vingt "
            + "dossiers.",
            example = "3")
    private long nombre;
}
