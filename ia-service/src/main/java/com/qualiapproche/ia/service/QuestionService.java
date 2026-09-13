package com.qualiapproche.ia.service;

import com.qualiapproche.ia.dto.QuestionDto;
import com.qualiapproche.ia.dto.ReponseQuestionDto;
import com.qualiapproche.ia.enumeration.QuestionPredefinie;

import java.util.List;
import java.util.UUID;

public interface QuestionService {

    /** Les questions que l'écran peut proposer. */
    List<QuestionDto> catalogue();

    /**
     * Répond à une question prédéfinie : l'application interroge le service métier avec les droits
     * de l'appelant, puis l'assistant commente ce qu'on lui tend.
     *
     * @param code la question, prise dans une liste close
     * @param conversationId fil auquel rattacher l'échange, ou {@code null} pour en ouvrir un
     */
    ReponseQuestionDto repondre(QuestionPredefinie code, UUID conversationId);
}
