package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class NoeudReferentielDto {
    private UUID id;
    @NotBlank
    private String code;
    @NotBlank
    private String libelle;
    private String typeNoeud;
    private String preuveAttendue;
    private int ordre;
    private UUID parentId;
    private List<NoeudReferentielDto> enfants;
}
