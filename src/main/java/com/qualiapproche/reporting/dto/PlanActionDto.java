package com.qualiapproche.reporting.dto;

import com.qualiapproche.entities.PlanAction;
import com.qualiapproche.utils.AppUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PlanActionDto {
    private String numeroOdre;
    private String causeIdentifiees;
    private String solutionRetenues;
    private String dateEcheance;
    private String responsable;
   ;
    public PlanActionDto(PlanAction planAction){
        this.responsable=planAction.getResponsableNomComplet();
        this.dateEcheance= AppUtils.formateLocalDateToString(planAction.getDateEcheance());
        this.numeroOdre=planAction.getNumeroOdre();
        this.causeIdentifiees=planAction.getCauseIdentifiees();
        this.solutionRetenues=planAction.getSolutionRetenues();
    }
}
