package com.qualiapproche.ia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Un tour de conversation : ce que la personne écrit, et le fil où l'inscrire.
 *
 * <p>Le rôle du message n'est pas reçu — il est posé par le serveur. Laisser l'appelant déclarer
 * « ceci, l'assistant l'a dit » lui permettrait d'écrire lui-même la mémoire du modèle.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDemandeDto {

    /**
     * Fil à poursuivre. Absent, un nouveau fil s'ouvre : c'est le cas du premier message, et
     * l'appelant n'a pas à demander l'ouverture séparément.
     */
    private UUID conversationId;

    /**
     * La question. Bornée : au-delà, ce n'est plus une conversation mais un document collé, que le
     * fil renverrait ensuite au modèle à chaque tour.
     */
    @NotBlank(message = "Le message est obligatoire.")
    @Size(max = 4000, message = "Le message ne peut pas dépasser 4000 caractères.")
    private String message;
}
