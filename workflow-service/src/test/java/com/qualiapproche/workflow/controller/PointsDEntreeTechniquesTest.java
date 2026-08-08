package com.qualiapproche.workflow.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les points d'entrée techniques du moteur restent fermés aux utilisateurs.
 *
 * <p>{@code declarerFait} déverrouille les clôtures que les conditions retiennent ;
 * {@code designerTitulaire} ouvre les étapes réservées au titulaire. L'un comme l'autre écrivent
 * sans décision de circuit : ils n'existent que pour le dialogue entre services. Une garde retirée
 * par mégarde ne se verrait à aucun test fonctionnel — les appels de service continueraient de
 * passer — d'où cette vérification par réflexion.</p>
 */
class PointsDEntreeTechniquesTest {

    private Method methode(String nom) {
        return Arrays.stream(WorkflowController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(nom))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Méthode introuvable : " + nom));
    }

    private void exigerLaGardeDeService(String nom) {
        PreAuthorize garde = methode(nom).getAnnotation(PreAuthorize.class);
        assertThat(garde)
                .withFailMessage("%s doit être réservé aux appels de service : sans la garde, "
                        + "n'importe quel agent authentifié peut y écrire.", nom)
                .isNotNull();
        assertThat(garde.value()).contains("appelDeService");
    }

    @Test
    @DisplayName("Déclarer un fait est réservé aux services")
    void declarerFait_reserveAuxServices() {
        exigerLaGardeDeService("declarerFait");
    }

    @Test
    @DisplayName("Redésigner le titulaire est réservé aux services")
    void designerTitulaire_reserveAuxServices() {
        exigerLaGardeDeService("designerTitulaire");
    }

    @Test
    @DisplayName("L'historique exige une permission de lecture, ou un appel de service")
    void historique_cloisonne() {
        PreAuthorize garde = methode("getValidationHistory").getAnnotation(PreAuthorize.class);
        assertThat(garde)
                .withFailMessage("La traçabilité nomme des personnes et rapporte leurs "
                        + "appréciations : elle ne se sert pas à quiconque est authentifié.")
                .isNotNull();
        assertThat(garde.value()).contains("canRead");
        // L'appel de service reste admis : les modules relisent l'historique pour leurs écrans.
        assertThat(garde.value()).contains("appelDeService");
    }

    @Test
    @DisplayName("Aucun point de décision n'accepte d'identité fournie par le client")
    void decisions_sansIdentiteCliente() {
        // L'en-tête X-User-Id a existé : jamais lu, mais sa présence laissait croire qu'on pouvait
        // décider au nom d'un autre. Ce test empêche son retour.
        for (String nom : new String[]{"validateStep", "rejectStep", "executeTransition"}) {
            boolean porteUnParametreDIdentite = Arrays.stream(methode(nom).getParameters())
                    .anyMatch(parametre -> Arrays.stream(parametre.getAnnotations())
                            .anyMatch(a -> a.toString().contains("X-User-Id")));
            assertThat(porteUnParametreDIdentite)
                    .withFailMessage("%s accepte une identité cliente : l'identité du décideur "
                            + "est celle du jeton, exclusivement.", nom)
                    .isFalse();
        }
    }
}
