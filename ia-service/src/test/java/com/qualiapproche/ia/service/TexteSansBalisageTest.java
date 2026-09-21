package com.qualiapproche.ia.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import com.qualiapproche.ia.config.IaAssistantProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Ce que l'écran reçoit : du texte, et non le balisage dont le modèle a l'habitude.
 *
 * <p>Rien ne met ce balisage en forme. Le fil de discussion affiche le contenu brut et une
 * suggestion part dans un champ de formulaire : les dièses et les astérisques s'y montrent tels
 * quels. La consigne les proscrit déjà dans les trois familles de prompts, mais un modèle de sept
 * milliards de paramètres retombe dans ses plis — ces cas éprouvent la garantie, pas la consigne.</p>
 */
class TexteSansBalisageTest {

    private ClientDuModele client;

    @BeforeEach
    void preparer() {
        client = new ClientDuModele(mock(ChatClient.class, RETURNS_DEEP_STUBS), new IaAssistantProperties());
    }

    /** Le texte tel que le modèle l'aurait rendu. */
    private String rendu(String genere) {
        return client.texteDe(new ChatResponse(List.of(new Generation(new AssistantMessage(genere)))));
    }

    @Test
    @DisplayName("un titre perd ses dièses et garde ses mots")
    void titre_perdSesDieses() {
        assertThat(rendu("### Constat\nLa porte était bloquée.")).isEqualTo("Constat\nLa porte était bloquée.");
        assertThat(rendu("# Analyse des causes")).isEqualTo("Analyse des causes");
    }

    @Test
    @DisplayName("le gras et l'italique rendent le terme, pas les astérisques")
    void grasEtItalique_rendentLeTerme() {
        assertThat(rendu("La **cause racine** est connue.")).isEqualTo("La cause racine est connue.");
        assertThat(rendu("Une *action corrective* s'impose.")).isEqualTo("Une action corrective s'impose.");
        assertThat(rendu("__Urgent__ : traiter avant le 30.")).isEqualTo("Urgent : traiter avant le 30.");
    }

    @Test
    @DisplayName("une énumération reste une énumération, en tirets")
    void puces_deviennentDesTirets() {
        assertThat(rendu("* Former les agents\n* Réviser la procédure"))
                .isEqualTo("- Former les agents\n- Réviser la procédure");
    }

    @Test
    @DisplayName("les accents graves tombent, le terme reste")
    void accentsGraves_tombent() {
        assertThat(rendu("Le champ `numeroReference` est vide.")).isEqualTo("Le champ numeroReference est vide.");
    }

    @Test
    @DisplayName("une multiplication ou une note ne sont pas prises pour du balisage")
    void texteOrdinaire_estIntact() {
        assertThat(rendu("Un produit 3 * 4 = 12 reste lisible.")).isEqualTo("Un produit 3 * 4 = 12 reste lisible.");
        assertThat(rendu("Voir la note (*) au bas de la fiche.")).isEqualTo("Voir la note (*) au bas de la fiche.");
    }

    @Test
    @DisplayName("un texte déjà propre traverse sans être touché")
    void textePropre_traverseIntact() {
        String propre = "La porte coupe-feu du local archives était bloquée ouverte par un carton.";
        assertThat(rendu(propre)).isEqualTo(propre);
    }

    @Test
    @DisplayName("une réponse entière de l'assistant en production ne garde aucune marque")
    void reponseReelle_neGardeAucuneMarque() {
        String reel = """
                ### **2. Modifier le circuit de validation d'une NC**
                **Où ?** Dans **"Paramètres"** → **"Circuits de validation"**.

                **Comment ?**
                - **Accéder au circuit** : Sélectionnez le type (ex : *"NC Processus"*).
                - **Modifier les étapes** :
                  - Définissez les **délais** (ex : 3 jours pour la réponse).

                ---
                ### **Besoin de précisions ?**
                """;

        String propre = rendu(reel);

        // Aucune marque ne subsiste : ni titre, ni gras, ni italique, ni trait de séparation.
        assertThat(propre).doesNotContain("#").doesNotContain("*").doesNotContain("---");
        // Le texte, lui, est intact — y compris les puces imbriquées, qui portent le sens.
        assertThat(propre)
                .contains("2. Modifier le circuit de validation d'une NC")
                .contains("Où ? Dans \"Paramètres\" → \"Circuits de validation\".")
                .contains("- Accéder au circuit : Sélectionnez le type (ex : \"NC Processus\").")
                .contains("  - Définissez les délais (ex : 3 jours pour la réponse).")
                .contains("Besoin de précisions ?");
    }

    @Test
    @DisplayName("une réponse vide ou absente ne fait pas lever")
    void reponseVide_neLevePas() {
        assertThat(client.texteDe(null)).isNull();
        assertThat(rendu("")).isEmpty();
    }
}
