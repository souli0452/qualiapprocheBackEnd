package com.qualiapproche.ia.service;

import com.qualiapproche.common.dto.FaqDto;
import com.qualiapproche.ia.client.ReferentielClient;
import com.qualiapproche.ia.config.IaAssistantProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ce que l'assistant reçoit de la FAQ de l'organisation, et ce qu'il n'en reçoit jamais.
 *
 * <p>La FAQ vit dans referentiel-service : c'est un référentiel de l'application, affiché dans
 * l'aide, et l'assistant se contente de le lire. Il l'appelle avec le jeton de son interlocuteur
 * — le cloisonnement entre organisations est donc tenu là-bas, et ces cas éprouvent ce qui
 * relève d'ici : la mise en forme, le plafond, et la tenue en cas de panne.</p>
 */
class CatalogueDeFaqTest {

    private ReferentielClient referentiel;
    private IaAssistantProperties proprietes;
    private CatalogueDeFaq catalogue;

    @BeforeEach
    void preparer() {
        referentiel = mock(ReferentielClient.class);
        proprietes = new IaAssistantProperties();
        catalogue = new CatalogueDeFaq(referentiel, proprietes);
    }

    private FaqDto entree(String question, String reponse) {
        return FaqDto.builder().question(question).reponse(reponse).publiee(true).build();
    }

    private void poser(FaqDto... entrees) {
        when(referentiel.faqPubliee()).thenReturn(List.of(entrees));
    }

    @Test
    @DisplayName("les entrées publiées arrivent au modèle, question et réponse")
    void entreesPubliees_arriventAuModele() {
        poser(entree("Qui vise une procédure ?", "Le pilote du processus, puis la qualité."));

        assertThat(catalogue.pourLaConsigne())
                .contains("Q : Qui vise une procédure ?")
                .contains("R : Le pilote du processus, puis la qualité.");
    }

    @Test
    @DisplayName("la consigne dit de ne pas forcer le rapprochement quand rien ne répond")
    void laConsigne_interditDeForcerLeRapprochement() {
        poser(entree("q", "r"));

        assertThat(catalogue.pourLaConsigne())
                .contains("ne force pas le rapprochement")
                .contains("font autorité ici");
    }

    @Test
    @DisplayName("référentiel injoignable : l'assistant répond sans la FAQ plutôt que pas du tout")
    void referentielInjoignable_nEmpechePasDeRepondre() {
        when(referentiel.faqPubliee()).thenThrow(new IllegalStateException("service indisponible"));

        // La méthode et le vocabulaire n'ont que faire de la FAQ : fermer la conversation parce
        // qu'un autre service ne répond pas serait disproportionné.
        assertThat(catalogue.pourLaConsigne()).isEmpty();
    }

    @Test
    @DisplayName("aucune entrée publiée : la consigne n'est pas alourdie d'un en-tête vide")
    void aucuneEntree_nAlourditPasLaConsigne() {
        when(referentiel.faqPubliee()).thenReturn(List.of());

        assertThat(catalogue.pourLaConsigne()).isEmpty();
    }

    @Test
    @DisplayName("le plafond tranche par la fin : les plus anciennes réponses survivent")
    void lePlafond_trancheParLaFin() {
        proprietes.setFaqCaracteres(500);
        poser(entree("première", "p".repeat(300)),
              entree("deuxième", "d".repeat(300)),
              entree("troisième", "t".repeat(300)));

        String consigne = catalogue.pourLaConsigne();

        assertThat(consigne).contains("première");
        assertThat(consigne).doesNotContain("deuxième").doesNotContain("troisième");
    }

    @Test
    @DisplayName("une entrée seule passe même si elle dépasse : mieux vaut une réponse que rien")
    void uneEntreeSeule_passeMemeSiElleDepasse() {
        proprietes.setFaqCaracteres(50);
        poser(entree("longue", "x".repeat(400)));

        assertThat(catalogue.pourLaConsigne()).contains("longue");
    }

    @Test
    @DisplayName("une entrée incomplète est ignorée sans faire tomber le reste")
    void entreeIncomplete_estIgnoree() {
        poser(FaqDto.builder().question("sans réponse").publiee(true).build(),
              entree("complète", "sa réponse"));

        String consigne = catalogue.pourLaConsigne();

        assertThat(consigne).contains("complète");
        assertThat(consigne).doesNotContain("sans réponse");
    }
}
