package com.qualiapproche.common.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * La demande de publication, ou de retrait, d'un lot de réponses.
 *
 * <p>Un lot et non une entrée : on relit une série de brouillons puis on les ouvre ensemble. Les
 * publier une par une aurait demandé autant d'allers-retours que de réponses, et laissé le lot à
 * moitié ouvert si l'un d'eux échouait.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicationFaqDto {

    @NotEmpty(message = "Aucune réponse n'a été désignée.")
    private List<UUID> ids;

    /** Vrai pour publier, faux pour retirer de la publication. */
    private boolean publiee;
}
