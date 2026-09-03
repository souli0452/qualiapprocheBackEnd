package com.qualiapproche.amelioration.reporting.dto;

import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.common.utils.AppUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Schema(description = "Une ligne du tableau des actions correctives de la fiche de non-conformité. "
        + "Elle est sérialisée en JSON puis lue comme source de données par le gabarit Jasper : "
        + "les noms de champs sont ceux que le gabarit cite, et les renommer viderait les colonnes "
        + "de l'état sans qu'aucune compilation ne s'en plaigne. Tout y est du texte, l'édition "
        + "n'acceptant rien d'autre.")
public class PlanActionDto {

    @Schema(description = "Rang de l'action dans le plan, tel que saisi. Nom hérité du gabarit, "
            + "coquille comprise.",
            example = "1")
    private String numeroOdre;

    @Schema(description = "Cause de l'écart, telle que l'analyse l'a retenue. Le texte est recopié "
            + "de la base sans être dépouillé de ses balises, à la différence des justifications "
            + "de la fiche : une saisie faite en éditeur riche paraît donc balisée sur l'état.")
    private String causeIdentifiees;

    @Schema(description = "Ce qui a été décidé pour traiter la cause. C'est la colonne que "
            + "l'auditeur lit en regard de la précédente.")
    private String solutionRetenues;

    @Schema(description = "Date limite de réalisation, déjà mise en forme pour l'impression. C'est "
            + "elle qui commande les relances et le signalement des retards, mais l'état ne la "
            + "rend que telle qu'elle a été arrêtée.",
            example = "31/12/2026")
    private String dateEcheance;

    @Schema(description = "Nom de qui répond de l'action, figé à la génération de l'état. Le "
            + "gabarit ne reçoit pas d'identifiant : la fiche imprimée ne se relie à rien.")
    private String responsable;

    @Schema(description = "À quoi se jugera l'efficacité de l'action. Porté par la ligne mais "
            + "absent du gabarit livré : il ne paraît sur aucune fiche aujourd'hui.")
    private String critereEfficacite;

    public PlanActionDto(PlanAction planAction) {
        this.responsable = planAction.getResponsableNomComplet();
        this.dateEcheance = AppUtils.formateLocalDateToString(planAction.getDateEcheance());
        this.numeroOdre = planAction.getNumeroOdre();
        this.causeIdentifiees = planAction.getCauseIdentifiees();
        this.solutionRetenues = planAction.getSolutionRetenues();
        this.critereEfficacite = planAction.getCritereEfficacite();
    }
}
