package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;







import java.util.List;
import java.util.UUID;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
@Schema(description = "Corps de la demande de rattachement de critères d'évaluation à un "
        + "fournisseur, lui-même désigné par l'URL. Le rattachement s'ajoute : les critères "
        + "absents de la liste ne sont pas détachés.")
public class CritereEvaluationIdsRequestDto {

    @Schema(description = "Identifiants des critères à rattacher. Un identifiant inconnu fait "
            + "échouer la demande entière, sans qu'aucun rattachement soit enregistré.")
    private List<UUID> critereEvaluationIds;

}
