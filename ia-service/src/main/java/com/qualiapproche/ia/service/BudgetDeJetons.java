package com.qualiapproche.ia.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.ia.config.IaAssistantProperties;
import com.qualiapproche.ia.repository.MessageIaRepository;
import com.qualiapproche.ia.repository.SuggestionIaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Le plafond quotidien de jetons, pour tout ce que l'assistant consomme.
 *
 * <p>Un seul forfait, deux usages : les suggestions rédactionnelles et les conversations y
 * puisent ensemble. Compter l'un sans l'autre laisserait la conversation — qui renvoie son fil
 * entier à chaque tour, donc coûteuse — vider le budget sans jamais être refusée.</p>
 *
 * <p>Le décompte porte sur la <b>direction</b> du jeton — sa structure de rattachement quand il ne
 * porte pas de direction —, et à défaut sur la personne. C'est la même clé que celle qui borne la
 * lecture de l'historique : ce qu'on lit est ce qu'on consomme.</p>
 */
@Component
@RequiredArgsConstructor
public class BudgetDeJetons {

    private final SuggestionIaRepository suggestions;
    private final MessageIaRepository messages;
    private final IaAssistantProperties proprietes;

    /**
     * Vérifie le plafond <b>avant</b> l'appel au modèle : refuser après coup aurait déjà payé la
     * génération. Une valeur de budget nulle ou négative lève toute limite.
     *
     * @throws BusinessException en 429 quand le budget du jour est atteint
     */
    public void exigerDuReste() {
        long budget = proprietes.getBudgetJetonsJour();
        if (budget <= 0) {
            return;
        }
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();

        UUID directionId = SecurityUtils.getCurrentDirectionId();
        if (directionId != null) {
            long consommes = suggestions.sommeJetonsDepuisParDirection(directionId, debutJour)
                    + messages.sommeJetonsDepuisParDirection(directionId, debutJour);
            if (consommes >= budget) {
                throw new BusinessException(
                        "Le budget quotidien de l'assistant IA est atteint pour votre organisation"
                                + " (" + budget + " jetons). Réessayez demain.",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
            return;
        }

        String utilisateurId = SecurityUtils.getCurrentUserId();
        if (utilisateurId == null) {
            return;
        }
        long consommes = suggestions.sommeJetonsDepuisParUtilisateur(utilisateurId, debutJour)
                + messages.sommeJetonsDepuisParUtilisateur(utilisateurId, debutJour);
        if (consommes >= budget) {
            throw new BusinessException(
                    "Votre budget quotidien de l'assistant IA est atteint"
                            + " (" + budget + " jetons). Réessayez demain.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
    }
}
