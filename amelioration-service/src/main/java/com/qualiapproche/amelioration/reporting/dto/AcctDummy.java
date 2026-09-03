package com.qualiapproche.amelioration.reporting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Jeu de données minimal servant à éprouver la chaîne d'édition Jasper. "
        + "Aucun état ne s'en sert et aucun point d'entrée ne le rend : il ne subsiste que comme "
        + "gabarit d'essai.")
public class AcctDummy {

  @Schema(description = "Titre de l'état d'essai, repris tel quel par le gabarit.")
  private String title;

  @Schema(description = "Corps de l'état d'essai, sans mise en forme.")
  private String description;
}
