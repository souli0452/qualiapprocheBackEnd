package com.qualiapproche.amelioration.messagerie;

import com.qualiapproche.amelioration.service.NonConformiteService;
import com.qualiapproche.amelioration.service.PlanActionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.messagerie.AdressesMessagerie;
import com.qualiapproche.common.messagerie.ConvertisseurDesMessages;
import com.qualiapproche.common.messagerie.Dedoublonnage;
import com.qualiapproche.common.messagerie.LecteurDeMessages;
import com.qualiapproche.common.messagerie.RappelDuCircuit;
import com.qualiapproche.common.utils.SecurityUtils;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Les rappels du circuit arrivent par le courtier : chacun va au bon dossier, une seule fois.
 * Remplace les webhooks HTTP que workflow-service appelait.
 */
class RappelsDuCircuitTest {

    private final NonConformiteService nonConformites = mock(NonConformiteService.class);
    private final PlanActionService plans = mock(PlanActionService.class);
    @SuppressWarnings("unchecked")
    private final Dedoublonnage<MessageTraite> dedoublonnage = mock(Dedoublonnage.class);
    private final RappelsDuCircuit rappels = new RappelsDuCircuit(nonConformites, plans, dedoublonnage,
            new LecteurDeMessages(new ConvertisseurDesMessages(new ObjectMapper())));
    private final UUID dossier = UUID.randomUUID();

    @BeforeEach
    void premiereLivraison() {
        when(dedoublonnage.premiereLivraison(anyString())).thenReturn(true);
    }

    private RappelDuCircuit rappel(String type) {
        return RappelDuCircuit.builder().typeRessource(type).ressourceId(dossier).contenu(Map.of(
                "status", "EN_COURS", "statusName", "Validation RS", "etatCode", "VALIDATION_RS",
                "conditionFranchie", "PLAN_COMPLET", "fields", Map.of("titulaire", "u-42"))).build();
    }

    @Test
    @DisplayName("Une non-conformité reçoit l'issue, l'étape, l'état, les champs saisis et le point de contrôle franchi")
    void nonConformite() {
        rappels.recevoir(rappel("NON_CONFORMITE"), "m-1");

        verify(nonConformites).updateWorkflowState(dossier, "EN_COURS", "Validation RS", "VALIDATION_RS",
                Map.of("titulaire", "u-42"), "PLAN_COMPLET");
    }

    @Test
    @DisplayName("Un plan d'action reçoit l'étape, l'état et les champs saisis")
    void planDAction() {
        rappels.recevoir(rappel("PLAN_ACTION"), "m-2");

        verify(plans).updateWorkflowState(dossier, "Validation RS", "VALIDATION_RS", Map.of("titulaire", "u-42"));
    }

    @Test
    @DisplayName("Une livraison en double est écartée : la transition n'est pas rejouée")
    void livraisonEnDouble() {
        when(dedoublonnage.premiereLivraison("m-1")).thenReturn(false);

        rappels.recevoir(rappel("NON_CONFORMITE"), "m-1");

        verifyNoInteractions(nonConformites, plans);
    }

    @Test
    @DisplayName("Un type qui ne relève pas de ce service est ignoré")
    void typeEtranger() {
        rappels.recevoir(rappel("DOCUMENT"), "m-3");

        verify(nonConformites, never()).updateWorkflowState(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(plans);
    }

    @Test
    @DisplayName("La mise à jour est attribuée à la personne qui a fait franchir la transition")
    void attribueeAuDecideur() {
        AtomicReference<String> auteur = new AtomicReference<>();
        AtomicReference<String> nom = new AtomicReference<>();
        doAnswer(i -> {
            auteur.set(SecurityUtils.getAuteurId());
            nom.set(SecurityUtils.getAuteurNom());
            return null;
        }).when(nonConformites).updateWorkflowState(any(), any(), any(), any(), any(), any());
        RappelDuCircuit rappel = rappel("NON_CONFORMITE");
        rappel.setAuteurId("u-7");
        rappel.setAuteurNom("Awa Ouédraogo");

        rappels.recevoir(rappel, "m-4");

        assertThat(auteur.get()).isEqualTo("u-7");
        assertThat(nom.get()).isEqualTo("Awa Ouédraogo");
        assertThat(SecurityUtils.getAuteurId()).isNull();
    }

    @Test
    @DisplayName("Un rappel sans type ni dossier est écarté sans erreur : relivré, il reviendrait trois jours durant")
    void rappelIncomplet() {
        rappels.recevoir(RappelDuCircuit.builder().ressourceId(dossier).build(), "m-5");
        rappels.recevoir(RappelDuCircuit.builder().typeRessource("NON_CONFORMITE").build(), "m-6");

        verifyNoInteractions(nonConformites, plans, dedoublonnage);
    }

    @Test
    @DisplayName("Du courtier : un rappel lisible est appliqué, un message illisible est écarté sans exception")
    void depuisLeCourtier() throws Exception {
        TextMessage lisible = mock(TextMessage.class);
        when(lisible.getStringProperty(AdressesMessagerie.PROPRIETE_TYPE)).thenReturn(AdressesMessagerie.TYPE_RAPPEL_CIRCUIT);
        when(lisible.getStringProperty(AdressesMessagerie.PROPRIETE_IDENTIFIANT)).thenReturn("m-7");
        when(lisible.getText()).thenReturn("{\"typeRessource\":\"PLAN_ACTION\",\"ressourceId\":\"" + dossier
                + "\",\"contenu\":{\"statusName\":\"Suivi\",\"etatCode\":\"SUIVI\"}}");
        TextMessage forge = mock(TextMessage.class);
        when(forge.getStringProperty(AdressesMessagerie.PROPRIETE_TYPE)).thenReturn("com.zaxxer.hikari.HikariConfig");
        when(forge.getText()).thenReturn("{}");

        rappels.surMessage(forge);
        verifyNoInteractions(plans);

        rappels.surMessage(lisible);
        verify(plans).updateWorkflowState(dossier, "Suivi", "SUIVI", Map.of());
    }
}
