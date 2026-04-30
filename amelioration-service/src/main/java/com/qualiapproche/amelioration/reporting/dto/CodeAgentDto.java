package com.qualiapproche.amelioration.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CodeAgentDto {
  private String motDePasse;
  private String nomUtilisateur;
  private String nomComplet;
  private String matricule;
  private String dateCreation;
  private String profile;
  private String action;
  private String logiciel;
}
