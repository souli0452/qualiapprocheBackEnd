package com.qualiapproche.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enveloppe de toute réponse du produit.
 *
 * <p>C'est aussi la seule forme que {@code GlobalResponseHandler} laisse passer intacte : un
 * contrôleur qui rend une {@code List} nue la voit paginée d'office, à dix éléments.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Enveloppe de toute réponse du produit : le contenu utile est porté par "
        + "« data », jamais à la racine.")
public class ApiResponse<T> {

    @Schema(description = "Phrase décrivant l'issue de l'appel. Destinée au journal et au "
            + "diagnostic, non à l'affichage.",
            example = "Opération réussie")
    private String message;

    @Schema(description = "Le contenu demandé. Nul en cas d'erreur.")
    private T data;

    @Schema(description = "Code HTTP repris dans le corps, pour les clients qui ne lisent que "
            + "celui-ci.", example = "200")
    private int statusCode;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .message(message)
                .data(data)
                .statusCode(200)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Opération réussie");
    }

    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .message(message)
                .data(null)
                .statusCode(statusCode)
                .build();
    }
}
