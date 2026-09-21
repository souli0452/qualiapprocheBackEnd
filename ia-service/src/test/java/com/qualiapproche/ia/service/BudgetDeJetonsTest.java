package com.qualiapproche.ia.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.ia.config.IaAssistantProperties;
import com.qualiapproche.ia.repository.MessageIaRepository;
import com.qualiapproche.ia.repository.SuggestionIaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Le plafond quotidien de jetons : ce qu'il compte, et sur qui.
 *
 * <p>C'est la seule borne qui empêche une boucle ou un usage massif d'épuiser le forfait d'une
 * direction en une journée. Elle somme les deux usages — suggestions rédactionnelles et messages
 * de conversation — parce que les compter séparément laissait le fil, qui renvoie son historique
 * à chaque tour, vider le forfait sans jamais être refusé.</p>
 */
class BudgetDeJetonsTest {

    private static final UUID DIRECTION = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private SuggestionIaRepository suggestions;
    private MessageIaRepository messages;
    private IaAssistantProperties proprietes;
    private BudgetDeJetons budget;
    private MockedStatic<SecurityUtils> securite;

    @BeforeEach
    void preparer() {
        suggestions = mock(SuggestionIaRepository.class);
        messages = mock(MessageIaRepository.class);
        proprietes = new IaAssistantProperties();
        budget = new BudgetDeJetons(suggestions, messages, proprietes);
        securite = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void ranger() {
        securite.close();
    }

    private void poserLaDirection(long jetonsSuggestions, long jetonsMessages) {
        securite.when(SecurityUtils::getCurrentDirectionId).thenReturn(DIRECTION);
        when(suggestions.sommeJetonsDepuisParDirection(any(), any(LocalDateTime.class)))
                .thenReturn(jetonsSuggestions);
        when(messages.sommeJetonsDepuisParDirection(any(), any(LocalDateTime.class)))
                .thenReturn(jetonsMessages);
    }

    @Test
    @DisplayName("les deux usages se somment : le fil ne passe pas à côté du plafond")
    void lesDeuxUsages_seSomment() {
        proprietes.setBudgetJetonsJour(1_000);
        // Ni l'un ni l'autre n'atteint le plafond ; leur somme le dépasse.
        poserLaDirection(600, 500);

        BusinessException refus = catchThrowableOfType(
                () -> budget.exigerDuReste(), BusinessException.class);

        assertThat(refus).isNotNull();
        assertThat(refus.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("sous le plafond, l'appel passe")
    void sousLePlafond_lAppelPasse() {
        proprietes.setBudgetJetonsJour(1_000);
        poserLaDirection(400, 300);

        assertThatCode(() -> budget.exigerDuReste()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("le refus vient avant l'appel au modèle : 429, jamais une panne du fournisseur")
    void leRefus_estUnQuotaEtNonUnePanne() {
        proprietes.setBudgetJetonsJour(100);
        poserLaDirection(100, 0);

        BusinessException refus = catchThrowableOfType(
                () -> budget.exigerDuReste(), BusinessException.class);

        assertThat(refus.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(refus.getMessage()).contains("budget quotidien");
    }

    @Test
    @DisplayName("un plafond nul ou négatif vaut illimité : rien n'est interrogé")
    void plafondNul_vautIllimite() {
        proprietes.setBudgetJetonsJour(0);

        assertThatCode(() -> budget.exigerDuReste()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sans direction, le plafond s'applique à la personne — et non à tout le monde")
    void sansDirection_lePlafondSuitLaPersonne() {
        proprietes.setBudgetJetonsJour(500);
        securite.when(SecurityUtils::getCurrentDirectionId).thenReturn(null);
        securite.when(SecurityUtils::getCurrentUserId).thenReturn("agent-1");
        when(suggestions.sommeJetonsDepuisParUtilisateur(anyString(), any(LocalDateTime.class)))
                .thenReturn(300L);
        when(messages.sommeJetonsDepuisParUtilisateur(anyString(), any(LocalDateTime.class)))
                .thenReturn(300L);

        BusinessException refus = catchThrowableOfType(
                () -> budget.exigerDuReste(), BusinessException.class);

        assertThat(refus).isNotNull();
        assertThat(refus.getMessage()).contains("Votre budget");
    }
}
