package com.qualiapproche.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adresse du dossier dans les courriels d'étape.
 *
 * <p>La règle qui compte : <b>jamais de lien mort</b>. Un bouton « Consulter » qui mène à une 404
 * fait perdre confiance dans tous les courriels suivants — l'absence de lien, elle, se comprend.
 * D'où les replis en cascade : type inconnu → ancien motif, racine absente → pas de lien du
 * tout.</p>
 */
class LienVersLeDossierTest {

    private LienVersLeDossier lien;

    @BeforeEach
    void setUp() {
        lien = new LienVersLeDossier();
        lien.setBaseUrl("https://qualisira.horeb.tech");
        lien.setLiens(Map.of(
                "DOCUMENT", "/gestion-documentaire/documents?documentId={resourceId}",
                "NON_CONFORMITE", "/non-conformite/suivi?ncId={resourceId}"));
    }

    @Test
    @DisplayName("Chaque type mène à son écran, l'identifiant substitué")
    void typeConnu_routeDuType() {
        assertThat(lien.pour("DOCUMENT", "42"))
                .isEqualTo("https://qualisira.horeb.tech/gestion-documentaire/documents?documentId=42");
        assertThat(lien.pour("NON_CONFORMITE", "nc-7"))
                .isEqualTo("https://qualisira.horeb.tech/non-conformite/suivi?ncId=nc-7");
    }

    @Test
    @DisplayName("La casse du type et une racine à barre finale ne changent rien")
    void formes_tolerees() {
        lien.setBaseUrl("https://qualisira.horeb.tech/");

        assertThat(lien.pour("document", "42"))
                .isEqualTo("https://qualisira.horeb.tech/gestion-documentaire/documents?documentId=42");
    }

    @Test
    @DisplayName("Type absent de la table : l'ancien motif unique sert de repli")
    void typeInconnu_motifDeRepli() {
        lien.setMotifLien("/{resourceType}/{resourceId}");

        assertThat(lien.pour("RECLAMATION", "77"))
                .isEqualTo("https://qualisira.horeb.tech/reclamation/77");
    }

    @Test
    @DisplayName("Sans racine, aucun lien : un chemin relatif serait mort dans un courriel")
    void sansRacine_aucunLien() {
        lien.setBaseUrl("");

        assertThat(lien.pour("DOCUMENT", "42")).isEmpty();
    }

    @Test
    @DisplayName("Un motif déjà absolu part tel quel, sans racine exigée")
    void motifAbsolu_telQuel() {
        lien.setBaseUrl("");
        lien.setMotifLien("https://autre-frontal.exemple/{resourceType}/{resourceId}");

        assertThat(lien.pour("RECLAMATION", "77"))
                .isEqualTo("https://autre-frontal.exemple/reclamation/77");
    }

    @Test
    @DisplayName("Type inconnu sans repli, ou dossier sans identifiant : rien")
    void riens_propres() {
        assertThat(lien.pour("RECLAMATION", "77")).isEmpty();
        assertThat(lien.pour("DOCUMENT", null)).isEmpty();
        assertThat(lien.pour(null, "42")).isEmpty();
    }
}
