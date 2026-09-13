package com.qualiapproche.ia.service;

import com.qualiapproche.ia.dto.ConversationDto;
import com.qualiapproche.ia.dto.MessageDemandeDto;
import com.qualiapproche.ia.dto.ReponseConversationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ConversationService {

    /**
     * Répond à un message, dans un fil existant ou dans un fil qui s'ouvre.
     *
     * @param demande le fil — facultatif — et la question
     * @return la réponse et le fil auquel se rattacher ensuite
     */
    ReponseConversationDto repondre(MessageDemandeDto demande);

    /**
     * Un tour de conversation dont la matière vient d'ailleurs — une question prédéfinie, dont
     * l'application a déjà obtenu les données auprès du service métier.
     *
     * <p>Ce qui est <b>inscrit</b> dans le fil et ce qui est <b>transmis</b> au modèle diffèrent :
     * le fil garde la question telle qu'elle se lit (« Qu'est-ce qui m'attend ? »), le modèle
     * reçoit en plus les données à commenter. Verser le relevé brut dans le fil le rendrait
     * illisible à la relecture, et le ferait repartir à chaque tour suivant.</p>
     *
     * @param conversationId fil à poursuivre, ou {@code null} pour en ouvrir un
     * @param questionAffichee ce que lira l'utilisateur dans le fil
     * @param matiere les données fournies au modèle, et à lui seul
     */
    ReponseConversationDto repondreSurMatiere(UUID conversationId, String questionAffichee, String matiere);

    /**
     * Les fils de l'appelant, du plus vivant au plus ancien, sans leurs messages.
     *
     * <p>Bornés à leur auteur, sans exception : une conversation n'est pas un dossier.</p>
     */
    Page<ConversationDto> mesConversations(Pageable pageable);

    /**
     * Un fil et tous ses messages, pour le rouvrir tel qu'il a été laissé.
     *
     * @throws com.qualiapproche.common.exception.BusinessException en 404 si le fil n'existe pas,
     *         en 403 s'il appartient à quelqu'un d'autre
     */
    ConversationDto conversation(UUID id);
}
