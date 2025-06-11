package com.qualiapproche.reporting.dto;
import com.qualiapproche.entities.PlanAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class JasperPlanActionDto {
  private String numeroOdre;
  private String causeIdentifiees;
  private String solutionRetenues;
  private String responsable;
  private String dateEcheance;
  public JasperPlanActionDto(PlanAction planAction){
    this.numeroOdre=planAction.getNumeroOdre();
    this.causeIdentifiees=planAction.getCauseIdentifiees();
    this.solutionRetenues=planAction.getSolutionRetenues();
    this.responsable= planAction.getResponsable();
    this.dateEcheance= planAction.getDateEcheance();

  }

}
