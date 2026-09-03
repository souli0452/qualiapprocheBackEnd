package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;








import java.util.List;





@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
@Schema(description = "Risque pesant sur la conduite d'un processus : ce qu'il menace, ce qui a "
        + "été prévu pour l'atténuer et où en est ce traitement.")
public class RisqueDto extends  AuditEntityDto {
    @Schema(description = "Intitulé du risque, tel qu'il apparaît dans les tableaux de suivi.",
            example = "Rupture d'approvisionnement en consommables")
    private String libelle;

    @Schema(description = "Les circonstances qui font naître le risque et ce qu'il compromet s'il "
            + "survient.",
            example = "Fournisseur unique et délai de réapprovisionnement de six semaines.")
    private String description;

    @Schema(description = "Cotation du risque. Champ libre : l'échelle de cotation appartient à "
            + "l'organisation.",
            example = "Élevé")
    private String niveau;

    @Schema(description = "Où en est le traitement du risque. Transporté en texte, sans liste de "
            + "valeurs imposée.",
            example = "En cours de traitement")
    private String statut;

    @Schema(description = "Mesures prévues pour ramener le risque à un niveau acceptable. Le nom "
            + "du champ conserve une coquille d'origine : lire « plan d'atténuation ».",
            example = "Référencer un second fournisseur et constituer un stock de sécurité.")
    private String plantAttenuation;

    @Schema(description = "Observations recueillies au fil du suivi, postérieures à la description "
            + "initiale.")
    private String commentaireRisque;

    @Schema(description = "Élément qui atteste le risque ou l'effet de son traitement : constat, "
            + "mesure, référence de document.",
            example = "Rapport d'audit interne du 12 mars, constat n° 3.")
    private String evidenceRisque;
    // private LocalDateTime dateIdentificationRisque;
    @Schema(description = "Actions engagées au titre de ce risque.")
    private List<ActionCorrectivePreventiveDto> actionCorrectivePreventives;


}
