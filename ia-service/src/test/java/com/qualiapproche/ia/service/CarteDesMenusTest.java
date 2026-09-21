package com.qualiapproche.ia.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La carte des menus arrive-t-elle jusqu'au modèle, et sous une forme exploitable ?
 *
 * <p>Sans elle, l'assistant inventait les emplacements : un menu « Paramètres », un onglet
 * « Administration », un écran « Documentation à réviser », dont aucun n'existe. Ces cas ne
 * mesurent pas ce que le modèle en fait — cela se vérifie en exécution — mais qu'il la reçoive :
 * un fichier renommé ou vidé la ferait disparaître en silence, et l'assistant se remettrait à
 * broder sans que rien ne le signale.</p>
 */
class CarteDesMenusTest {

    private final PromptRegistry registre = new PromptRegistry();

    @Test
    @DisplayName("la consigne de conversation porte les emplacements réels de l'application")
    void consigne_porteLesEmplacementsReels() {
        String consigne = registre.promptDeConversation().contenu();

        assertThat(consigne)
                .contains("Qualité & Conformité > Non-Conformités = /non-conformite")
                .contains("Gestion documentaire > Gestion documentaire = /gestion-documentaire")
                .contains("Configurations > Documentation > Niveaux de confidentialité "
                        + "= /parametrage-document/confidentialite");
    }

    @Test
    @DisplayName("les commentaires du fichier ne partent pas au modèle : ils coûteraient des jetons pour rien")
    void commentairesDuFichier_nePartentPas() {
        assertThat(registre.promptDeConversation().contenu())
                .doesNotContain("# Carte des menus")
                .doesNotContain("FORMAT :")
                .doesNotContain("app.menu.ts");
    }

    @Test
    @DisplayName("la carte est accompagnée de la règle qui interdit d'extrapoler un menu voisin")
    void carte_vientAvecSaRegleDUsage() {
        String consigne = registre.promptDeConversation().contenu();

        // Fragments tenant sur une seule ligne : la consigne est repliée, et une phrase entière
        // s'y trouve coupée par un retour à la ligne suivi d'une indentation.
        assertThat(consigne)
                .contains("N'AVANCE ALORS AUCUN MENU")
                .contains("ne promets jamais l'accès")
                .contains("recopie jamais une ligne de la liste telle quelle");
    }

    @Test
    @DisplayName("la consigne enrichie porte une version distincte : l'historique reste comparable")
    void version_distingueLaConsigneEnrichie() {
        assertThat(registre.promptDeConversation().version()).isEqualTo("2.0");
    }
}
