package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 




import java.time.LocalDateTime;
import java.util.List;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder

public class CategorieFichierDto extends AuditEntityDto{

    private String libelleCategorie;
    private String descriptionCategorie;
    private String necessiteDemandeCreationFichier;
    //private List<FichierDto> fichiers;
}
