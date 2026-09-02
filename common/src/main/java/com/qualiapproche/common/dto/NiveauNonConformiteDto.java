package com.qualiapproche.common.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class NiveauNonConformiteDto extends AuditEntityDto {
    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    private String description;

    @Min(value = 1, message = "Le score de gravité doit être supérieur ou égal à 1")
    @Max(value = 3, message = "Le score de gravité doit être inférieur ou égal à 3")
    private Integer scoreGravite;

    @Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "La couleur doit être au format hexadécimal, par exemple #f59e0b"
    )
    private String couleur;
}
