package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;







import java.util.List;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
@Schema(description = "Entité soumise à un audit — service, processus ou fournisseur — avec "
        + "l'objet du contrôle et ce qu'il a établi.")
public class AuditeDto extends AuditEntityDto {

    @Schema(description = "Désignation de l'entité auditée, telle qu'elle apparaît dans les "
            + "listes de choix.",
            example = "Magasin central")
    private String libelleAudite;

    @Schema(description = "Périmètre retenu : ce que le contrôle examine, et ce qu'il laisse de "
            + "côté.",
            example = "Réception et stockage des consommables, hors gestion des retours.")
    private String descriptionAudite;

    @Schema(description = "Conclusion de l'audit, telle que l'auditeur la formule.",
            example = "Conforme avec observations.")
    private String resultatAudite;

    @Schema(description = "Où en est l'audit. Champ libre : les étapes appartiennent à "
            + "l'organisation.",
            example = "Clôturé")
    private String statutAudite;

    @Schema(description = "Ce que le contrôle cherche à vérifier.",
            example = "Vérifier la traçabilité des lots depuis leur réception.")
    private String objectifAudite;

    @Schema(description = "Nature du contrôle : audit interne, audit fournisseur, inspection "
            + "réglementaire.",
            example = "Audit interne")
    private String typeAudite;

    @Schema(description = "Risques mis en évidence par le contrôle.")
    private List<RisqueDto> risques;

    @Schema(description = "Écarts relevés au cours du contrôle.")
    private List<NonConformiteDto> nonConformites;
}
