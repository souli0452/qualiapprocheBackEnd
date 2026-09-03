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
@Schema(description = "Formation inscrite au développement des compétences : ce qu'elle vise, "
        + "ce qu'elle suppose acquis et comment son effet est apprécié.")
public class FormationDto extends AuditEntityDto {

    @Schema(description = "Intitulé de la formation, tel qu'il apparaît au plan de formation.",
            example = "Conduite d'audit interne")
    private String libelle;

    @Schema(description = "Contenu et modalités de la session.",
            example = "Trois jours en présentiel, alternant apports et mises en situation.")
    private String description;

    @Schema(description = "Ce que le participant doit savoir faire à l'issue de la session.",
            example = "Conduire seul un audit interne et en rédiger le rapport.")
    private String objectif;

    @Schema(description = "Ce qu'il faut déjà maîtriser pour suivre la session utilement.",
            example = "Connaissance du système documentaire de sa structure.")
    private String prerequis;

    @Schema(description = "Compétence que la formation vise à acquérir ou à entretenir.",
            example = "Audit interne")
    private String competence;

    @Schema(description = "Avancement de la formation. L'énumération est partagée avec les "
            + "actions correctives : tous ses états n'ont pas de sens ici.",
            example = "ACTIF")
    private StatutEnum statut;

    @Schema(description = "Exigences auxquelles la formation permet de répondre, telle une "
            + "obligation de qualification.")
    private List<ExigenceDto> exigences;

    @Schema(description = "Modalité retenue pour apprécier l'effet de la formation.")
    private EvaluationDto evaluation;
}
