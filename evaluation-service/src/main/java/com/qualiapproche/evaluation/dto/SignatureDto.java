package com.qualiapproche.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.evaluation.enumeration.StatutSignature;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
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
public class SignatureDto {
    private UUID id;
    private UUID auditId;
    @NotBlank
    private String signataireId;
    private String fonctionSignataire;
    private int ordre;
    private StatutSignature statut;
    private LocalDateTime dateSignature;
    private String motifRefus;
}
