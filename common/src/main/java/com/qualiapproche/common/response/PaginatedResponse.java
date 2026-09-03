package com.qualiapproche.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Une page de résultats, telle qu'elle est rendue dans le {@code data} d'une {@link ApiResponse}.
 *
 * <p>Attention : {@code GlobalResponseHandler} applique cette pagination d'office à toute réponse
 * de type {@code List}, même quand l'appelant n'en voulait pas — d'où des référentiels tronqués à
 * dix valeurs, sans que rien ne le signale.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Une page de résultats. Attention : la pagination est appliquée d'office à "
        + "toute réponse de type liste, à dix éléments en l'absence des paramètres « page » et "
        + "« size » — y compris là où l'appelant attendait la liste entière.")
public class PaginatedResponse<T> {

    @Schema(description = "Les éléments de la page courante, et eux seuls.")
    private List<T> content;

    @Schema(description = "Rang de la page rendue, la première portant le numéro zéro.",
            example = "0")
    private int pageNumber;

    @Schema(description = "Nombre d'éléments demandés par page. Dix à défaut de « size ».",
            example = "10")
    private int pageSize;

    @Schema(description = "Nombre total d'éléments, toutes pages confondues. C'est lui qu'il faut "
            + "lire pour savoir si la liste a été tronquée.", example = "137")
    private long totalElements;

    @Schema(description = "Nombre de pages que compte le total.", example = "14")
    private int totalPages;

    /**
     * Nommé {@code last} et non {@code isLast} : l'accesseur engendré étant {@code isLast()}, la
     * sérialisation retire le préfixe et la propriété sort sous le nom {@code last}. Le champ
     * portait l'autre nom, et sa description se rattachait alors à une propriété inexistante — le
     * seul attribut de cette enveloppe à n'être documenté nulle part. Le format de sortie et les
     * accesseurs ne changent pas ; seul le nom de la méthode du constructeur fluide suit.
     */
    @Schema(description = "La page rendue est-elle la dernière.", example = "false")
    private boolean last;

    public PaginatedResponse(Page<T> page) {
        this.content = page.getContent();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.last = page.isLast();
    }
}
