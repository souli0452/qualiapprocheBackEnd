package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.evaluation.enumeration.NiveauEfficacite;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
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
public class ActionMaitriseRisqueDto {
    private UUID id;
    @NotBlank
    private String libelle;
    private String responsable;
    private LocalDate delai;
    private NiveauEfficacite efficacite;
}
