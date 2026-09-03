package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;




import com.qualiapproche.common.utils.StatutEnum;



import java.util.List;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
@Schema(description = "Action engagée pour supprimer la cause d'un écart constaté ou pour en "
        + "prévenir l'apparition. Sa nature tient à ce qui la déclenche : un fait avéré ou un "
        + "risque.")
public class ActionCorrectivePreventiveDto extends AuditEntityDto {

    @Schema(description = "Intitulé de l'action, tel qu'il apparaît dans les listes de suivi.",
            example = "Réviser la procédure de réception")
    private String libelle;

    @Schema(description = "Ce qui sera fait, en actes, et sur quel périmètre.",
            example = "Rédiger la procédure, la faire viser, puis former les agents du magasin.")
    private String description;

    @Schema(description = "Agent qui répond de l'action. Le champ porte un nom, non un "
            + "identifiant.",
            example = "Idrissa Ouédraogo")
    private String responsable;

    @Schema(description = "Avancement de l'action. Elle n'est « TRAITER » qu'une fois son "
            + "efficacité constatée, et non dès que son responsable la déclare faite.",
            example = "EN_VERIFICATION")
    private StatutEnum statut;

    @Schema(description = "Nature de l'action : corrective quand elle traite une cause avérée, "
            + "préventive quand elle devance un risque.",
            example = "Corrective")
    private String type;

    @Schema(description = "Date d'engagement de l'action. Transportée en texte : le serveur n'en "
            + "impose pas le format.",
            example = "01-04-2026")
    private String dateDebut;

    @Schema(description = "Date d'achèvement, attendue ou constatée. Transportée en texte comme "
            + "la précédente.",
            example = "30-06-2026")
    private String dateFin;

    @Schema(description = "Réclamation à l'origine de l'action, lorsqu'elle en procède.")
    private ReclamationDto reclamation;

    @Schema(description = "Risques que l'action entend réduire.")
    private List<RisqueDto> risques;

    @Schema(description = "Exigences que l'action permet de satisfaire.")
    private List<ExigenceDto> exigences;
}
