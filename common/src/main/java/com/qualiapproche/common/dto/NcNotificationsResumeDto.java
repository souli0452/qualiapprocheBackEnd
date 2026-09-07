package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Les pastilles de l'accueil pour le module amélioration : trois files, et leur somme.
 *
 * <p>Pendant chiffré de la cloche, qui elle rédige des phrases. L'écran d'accueil n'avait besoin
 * que des nombres et devait pourtant lire les lignes une à une pour les recomposer — au risque de
 * compter deux fois un même dossier, une décision ouverte sur un plan étant aussi une échéance qui
 * presse.</p>
 *
 * <p>Les trois files ne se recoupent pas : ce que l'appelant doit décider, ce qu'il n'a jamais
 * soumis, ce qu'il a soumis et qui attend ailleurs. Un dossier tombe dans l'une ou dans aucune, et
 * {@link #totalAlertes} en est la somme exacte.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Les pastilles de l'accueil pour les non-conformités et leurs plans "
        + "d'action. Recalculées à chaque appel et jamais conservées, comme la cloche dont elles "
        + "sont le pendant chiffré. Les trois files ne se recoupent pas : le total en est la somme "
        + "exacte.")
public class NcNotificationsResumeDto {

    @Schema(description = "Somme des trois files. Un même dossier n'y est jamais compté deux fois.",
            example = "5")
    private long totalAlertes;

    @Schema(description = "Non-conformités que l'appelant a commencées sans jamais les soumettre. "
            + "Établi par le moteur — un dossier sur lequel aucune décision n'a été prise — et non "
            + "par un statut inscrit sur la fiche.",
            example = "2")
    private long brouillons;

    @Schema(description = "Non-conformités et plans d'action sur lesquels le circuit ouvre une "
            + "décision à l'appelant. C'est le moteur qui les désigne : son habilitation d'étape "
            + "fait foi, aucun croisement rôle × état n'est rejoué ici.",
            example = "3")
    private long aTraiter;

    @Schema(description = "Non-conformités que l'appelant a déclarées, dont le circuit court "
            + "encore, et sur lesquelles il n'a lui-même rien à décider : elles attendent "
            + "quelqu'un d'autre. Ne recoupe donc ni les brouillons ni ce qu'il a à traiter.",
            example = "0")
    private long enAttenteValidation;
}
