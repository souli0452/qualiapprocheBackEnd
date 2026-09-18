package com.qualiapproche.ia.service;

import com.qualiapproche.ia.dto.DemandeAssistanceDto;
import com.qualiapproche.ia.dto.SuggestionHistoriqueDto;
import com.qualiapproche.ia.dto.SuggestionIaDto;
import com.qualiapproche.ia.enumeration.VerdictSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AssistantService {

    /**
     * Produit une suggestion rédactionnelle et en persiste la trace.
     *
     * @param demande type d'assistance, texte source et contexte
     * @return la suggestion, l'identifiant de sa trace et l'avertissement d'usage
     */
    SuggestionIaDto assister(DemandeAssistanceDto demande);

    /**
     * Inscrit le sort réservé par l'utilisateur à une suggestion (KPI d'acceptation).
     *
     * <p>Réservé à qui a sollicité la suggestion : le verdict est son jugement, et la portée
     * transverse n'y change rien — elle ouvre la lecture, pas la décision à la place d'autrui.</p>
     *
     * @param id identifiant de la trace
     * @param verdict ACCEPTEE, MODIFIEE ou REJETEE
     * @return la trace mise à jour
     */
    SuggestionHistoriqueDto enregistrerVerdict(UUID id, VerdictSuggestion verdict);

    /**
     * Historique des suggestions, de la plus récente à la plus ancienne.
     *
     * <p>Borné au périmètre de l'appelant — sa direction, ou sa structure de rattachement quand le
     * jeton ne porte pas de direction —, sauf habilitation transverse
     * ({@code portee-toutes-structures}).</p>
     *
     * @param pageable pagination et tri demandés
     */
    Page<SuggestionHistoriqueDto> historique(Pageable pageable);
}
