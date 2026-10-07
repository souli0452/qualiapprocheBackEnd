package com.qualiapproche.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Une entrée de la foire aux questions.
 *
 * <p>Partagée par les deux usages : l'écran d'aide qui la montre, et ia-service qui la lit pour
 * la joindre à la consigne de l'assistant. Elle vit donc dans {@code common}, comme les autres
 * contrats traversant les modules.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class FaqDto {

    private UUID id;

    @NotBlank(message = "La question est obligatoire.")
    @Size(max = 300, message = "La question ne peut pas dépasser 300 caractères.")
    private String question;

    /**
     * Bornée court, et pour une raison qui dépasse l'écran d'aide : ce texte part au fournisseur
     * de l'assistant à chaque tour de conversation. Une réponse de FAQ est une réponse, pas un
     * manuel — au-delà, c'est le document qu'il faut joindre, non le recopier.
     */
    @NotBlank(message = "La réponse est obligatoire.")
    @Size(max = 1500, message = "La réponse ne peut pas dépasser 1500 caractères.")
    private String reponse;


    /** Visible dans l'aide et récitée par l'assistant. */
    private boolean publiee;


    private LocalDateTime updateAt;

    /**
     * Les pièces jointes de l'entrée, s'il y en a.
     *
     * <p>Jamais fournie par le client : elle est renseignée à la lecture. Un dépôt passe par son
     * propre point d'entrée, en multipart — une pièce ne s'attache pas en recopiant son nom dans
     * un formulaire.</p>
     */
    private List<FichierFaqDto> fichiers;
}
