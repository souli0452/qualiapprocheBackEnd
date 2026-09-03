package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;









@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
@Schema(description = "Verdict porté sur l'effet d'une action, une fois son résultat confronté "
        + "au critère d'efficacité fixé à sa définition. Les valeurs appartiennent à "
        + "l'organisation.")
public class EfficaciteDto extends AuditEntityDto {

    @Schema(description = "Intitulé du verdict, tel qu'il apparaît dans les listes de choix.",
            example = "Efficace")
    private String libelle;

    @Schema(description = "Ce qu'il faut avoir constaté pour retenir ce verdict plutôt qu'un "
            + "autre.",
            example = "Critère atteint et aucune récidive observée sur la période d'observation.")
    private String description;
}
