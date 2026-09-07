package com.qualiapproche.common.dto;

import com.qualiapproche.common.enumeration.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Les chiffres d'un tableau de bord de non-conformités. Le périmètre — tout "
        + "l'organisme, une structure, un agent — est fixé par le point d'entrée appelé et n'est "
        + "pas rappelé ici.\n\n"
        + "« En cours » et « clôturées » sont établis par le moteur de workflow, non par l'état "
        + "inscrit sur le dossier : ils restent donc justes quand le circuit gagne une étape ou "
        + "qu'on la renomme. Leur somme est inférieure au total du nombre de dossiers jamais "
        + "soumis, qui n'appartiennent à ni l'un ni l'autre.")
public class NcDashboardDto {
    @Schema(description = "Nombre de dossiers du périmètre, compté avant toute répartition. Les "
            + "deux répartitions écartent les dossiers dont l'état ou la gravité manque : leur "
            + "somme peut donc lui être inférieure.",
            example = "148")
    private long totalNC;

    /**
     * Le même nombre que {@link #totalNC}, sous le nom que les écrans emploient.
     *
     * <p>Les deux coexistent délibérément : {@code totalNC} est lu par ce qui existe, {@code total}
     * par ce qui s'écrit. Renommer aurait vidé les compteurs déjà branchés, et le nombre est le
     * même — il n'y a donc rien à départager.</p>
     */
    @Schema(description = "Le même nombre que « totalNC », sous le nom que les nouveaux écrans "
            + "emploient. Les deux coexistent pour ne pas vider les compteurs déjà branchés.",
            example = "52")
    private long total;

    @Schema(description = "Dossiers dont le circuit court encore : soumis, pas encore arrivés. Un "
            + "dossier que son auteur n'a jamais soumis n'y figure pas — il n'est pas en cours, il "
            + "attend chez lui.",
            example = "14")
    private long enCours;

    @Schema(description = "Dossiers dont le circuit a rendu son verdict, quel qu'il soit. Établi "
            + "par le moteur : aucun état de traitement n'est nommé ici.",
            example = "35")
    private long cloturees;

    @Schema(description = "Dossiers portant au moins une action corrective dont l'échéance est "
            + "passée sans qu'elle soit soldée. C'est le seul chiffre du tableau qui compte des "
            + "dossiers d'après leurs actions, et non d'après leur propre avancement : un dossier "
            + "en retard peut aussi bien être en cours que déjà clos.",
            example = "3")
    private long enRetard;

    @Schema(description = "Part des actions correctives menées dans les temps, en pourcentage. "
            + "Une action est en retard si elle a été réalisée après son échéance, ou si l'échéance "
            + "est passée sans qu'elle le soit. Les actions sans échéance saisie sont écartées du "
            + "calcul plutôt que comptées à leur avantage. Nul quand aucune action n'a d'échéance.",
            example = "85.0")
    private Double tauxSla;

    @Schema(description = "Part des dossiers clôturés sur l'ensemble du périmètre, en pourcentage. "
            + "Nul quand le périmètre est vide.",
            example = "67.3")
    private Double tauxResolution;

    @Schema(description = "Nombre de dossiers par état. Un état sans dossier est absent de la "
            + "carte plutôt que présent à zéro : c'est au client d'afficher le zéro.")
    private Map<Status, Long> statsByStatus;

    @Schema(description = "Même répartition, ventilée par libellé de niveau de gravité au sein de "
            + "chaque état. Un dossier dont le niveau n'est pas renseigné n'y figure pas.")
    private Map<Status, Map<String, Long>> statsByStatusAndGravity;
}
