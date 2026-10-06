package com.qualiapproche.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Un élément de preuve d'un constat, tel que l'écran le liste ; le contenu se télécharge à part. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreuveDto {
    private UUID id;
    private UUID constatId;
    private String nom;
    private String ext;
    private String type;
}
