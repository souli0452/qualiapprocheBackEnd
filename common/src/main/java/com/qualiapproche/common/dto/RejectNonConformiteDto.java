package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;




import com.qualiapproche.common.enumeration.Etat;



import java.util.UUID;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
@Schema(description = "Le refus d'une non-conformité, avec son motif et l'étape qui le prononce. "
        + "Aucun point d'entrée ne le reçoit aujourd'hui : le refus se dit au moteur de workflow "
        + "sous la forme d'une décision d'étape.")
public class RejectNonConformiteDto {
    @Schema(description = "Non-conformité refusée.",
            example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
    private UUID id;

    @Schema(description = "Motif du refus, destiné à l'auteur du dossier : c'est là-dessus qu'il "
            + "corrigera avant de soumettre à nouveau.",
            example = "Le fait décrit relève de la maintenance et non du système qualité.")
    private String rejectReason;

    @Schema(description = "Étape du circuit d'où le refus est prononcé. Le même dossier peut être "
            + "refusé à plusieurs endroits du parcours, et c'est elle qui dit lequel.",
            example = "VALIDATION_RQ")
    private Etat etapeTraitement;
    //FichierDto docRejet;
}
