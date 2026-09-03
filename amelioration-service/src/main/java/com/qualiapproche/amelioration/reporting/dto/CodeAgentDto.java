package com.qualiapproche.amelioration.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Schema(description = "Fiche de remise des accès d'un agent, destinée à un état imprimé. Héritée "
        + "d'un autre produit : aucun gabarit livré ne l'alimente et aucun point d'entrée ne "
        + "l'expose aujourd'hui.")
public class CodeAgentDto {

  @Schema(description = "Mot de passe remis à l'agent. Il n'est ni relu ni conservé : la fiche "
          + "imprimée est la seule forme sous laquelle il circule.")
  private String motDePasse;

  @Schema(description = "Identifiant de connexion attribué à l'agent.")
  private String nomUtilisateur;

  @Schema(description = "Nom de l'agent, tel qu'il figurera sur la fiche.")
  private String nomComplet;

  @Schema(description = "Matricule de l'agent dans l'organisation, qui le désigne indépendamment "
          + "de son compte.")
  private String matricule;

  @Schema(description = "Date de remise des accès, déjà mise en forme : l'édition n'accepte que du "
          + "texte.",
          example = "12/03/2026")
  private String dateCreation;

  @Schema(description = "Profil ouvert à l'agent, qui dit l'étendue de ses droits.")
  private String profile;

  @Schema(description = "Nature du geste consigné : ouverture, renouvellement ou fermeture des "
          + "accès.")
  private String action;

  @Schema(description = "Application concernée, une même fiche ne valant que pour l'une d'elles.")
  private String logiciel;
}
