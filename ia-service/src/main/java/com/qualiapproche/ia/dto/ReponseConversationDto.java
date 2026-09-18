package com.qualiapproche.ia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Ce que l'assistant répond, et le fil auquel se rattacher au tour suivant. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReponseConversationDto {

    /** Le fil — à renvoyer tel quel au message suivant. */
    private UUID conversationId;

    /** La réponse de l'assistant. */
    private String reponse;

    /** Rappel affiché sous le fil : l'assistant ne voit pas les données de l'organisation. */
    private String avertissement;

    /**
     * Messages restants avant la fin du fil. Rendu à chaque tour pour que l'écran puisse prévenir
     * avant le refus, plutôt que de laisser découvrir la borne en butant dessus.
     */
    private int messagesRestants;
}
