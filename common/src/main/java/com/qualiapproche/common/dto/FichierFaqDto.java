package com.qualiapproche.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Une pièce jointe d'entrée de FAQ, telle que l'écran la montre.
 *
 * <p>La référence de stockage n'en fait pas partie : le téléchargement passe par le service, qui
 * vérifie d'abord à qui appartient l'entrée. La rendre au client ouvrirait le dépôt entier.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class FichierFaqDto {

    private UUID id;
    private UUID faqId;
    private String nom;
    private String ext;
    private String type;
}
