package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;










@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder

public class CategorieFichierDto extends AuditEntityDto {

    private String libelleCategorie;
    private String descriptionCategorie;
    private String necessiteDemandeCreationFichier;
    //private List<FichierDto> fichiers;
}
