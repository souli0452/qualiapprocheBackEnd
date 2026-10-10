package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.config.ThymeleafConfig;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.courriel.FileDeCourriels;
import com.qualiapproche.evaluation.courriel.CourrielSortant;
import com.qualiapproche.evaluation.client.AnnuaireClient;
import com.qualiapproche.evaluation.client.StructuresClient;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.entities.TypeConstat;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Qui apprend la publication des constats, en combien de courriels, et ce qui se tait quand rien ne part. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationDuPiloteTest {

    @Mock private ConstatRepository constats;
    @Mock private AnnuaireClient annuaire;
    @Mock private StructuresClient structures;
    @Mock private FileDeCourriels<CourrielSortant> file;
    @Mock private TemplateEngine moteur;
    @Mock private PlatformTransactionManager transactions;

    private NotificationDuPilote notification;

    private final UUID processus = UUID.randomUUID();
    private Audit audit;

    @BeforeEach
    void preparer() {
        notification = new NotificationDuPilote(constats, annuaire, structures, file, moteur,
                "https://app.qualisira.com/audit/{id}", transactions);
        when(moteur.process(anyString(), any(org.thymeleaf.context.IContext.class))).thenReturn("<p>corps</p>");
        audit = Audit.builder().reference("AUD-2026-012").statut(StatutAudit.EN_COURS).processusId(processus).build();
        audit.setId(UUID.randomUUID());
        StructureDto structure = new StructureDto();
        structure.setLibelleLong("Gestion des ressources humaines");
        when(structures.structure(processus)).thenReturn(structure);
        when(annuaire.porteursDuRole("PILOTE", processus.toString())).thenReturn(Map.of("data", List.of(
                Map.of("userId", "u1", "email", "a.traore@exemple.bf", "nomComplet", "Awa Traoré"),
                Map.of("userId", "u2", "email", "b.ouedraogo@exemple.bf", "nomComplet", "Boris Ouédraogo"),
                Map.of("userId", "u3", "nomComplet", "Sans adresse"))));
    }

    private Constat constat(StatutConstat statut, boolean ecart) {
        Constat c = Constat.builder().audit(audit).statut(statut)
                .critereAudit("ISO 9001 §7.5").observation("Registres non complétés le jour même.")
                .nature(TypeConstat.builder().libelle(ecart ? "Non-conformité mineure" : "Conforme")
                        .genereNonConformite(ecart).build())
                .build();
        c.setId(UUID.randomUUID());
        return c;
    }

    @Test
    @DisplayName("Un courriel par pilote joignable de la structure auditée, qui liste tous les constats du geste")
    @SuppressWarnings("unchecked")
    void unCourrielParPilote() {
        Constat ecart = constat(StatutConstat.PUBLIE, true);
        Constat conforme = constat(StatutConstat.PUBLIE, false);
        when(constats.findAllById(any())).thenReturn(List.of(ecart, conforme));

        notification.notifier(new ConstatsPublies(audit.getId(), List.of(ecart.getId(), conforme.getId())));

        ArgumentCaptor<String> adresses = ArgumentCaptor.forClass(String.class);
        verify(file, times(2)).deposer(adresses.capture(), isNull(), eq("Audit AUD-2026-012 — 2 constats publiés"),
                eq("<p>corps</p>"), eq("publication-constats"));
        assertThat(adresses.getAllValues()).containsExactly("a.traore@exemple.bf", "b.ouedraogo@exemple.bf");
        ArgumentCaptor<org.thymeleaf.context.Context> contextes = ArgumentCaptor.forClass(org.thymeleaf.context.Context.class);
        verify(moteur, times(2)).process(eq(NotificationDuPilote.GABARIT), contextes.capture());
        org.thymeleaf.context.Context v = contextes.getValue();
        assertThat(v.getVariable("processus")).isEqualTo("Gestion des ressources humaines");
        assertThat((List<?>) v.getVariable("constats")).hasSize(2);
        assertThat(v.getVariable("ecarts")).isEqualTo(1L);
        assertThat(v.getVariable("lien")).isEqualTo("https://app.qualisira.com/audit/" + audit.getId());
    }

    @Test
    @DisplayName("Un constat qui n'est plus publié ne s'annonce pas ; sans constat publié, rien ne part")
    void rienQueDuPublie() {
        Constat compile = constat(StatutConstat.COMPILE, true);
        when(constats.findAllById(any())).thenReturn(List.of(compile));

        notification.notifier(new ConstatsPublies(audit.getId(), List.of(compile.getId())));

        verify(annuaire, never()).porteursDuRole(anyString(), anyString());
        verify(file, never()).deposer(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Sans processus audité, il n'y a pas de pilote à prévenir")
    void sansProcessus() {
        audit.setProcessusId(null);
        Constat c = constat(StatutConstat.PUBLIE, true);
        when(constats.findAllById(any())).thenReturn(List.of(c));

        notification.notifier(new ConstatsPublies(audit.getId(), List.of(c.getId())));

        verify(file, never()).deposer(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Un dépôt refusé n'empêche pas les autres pilotes d'être prévenus, ni ne remonte à la validation")
    void unEchecNArretePasLesAutres() {
        Constat c = constat(StatutConstat.PUBLIE, true);
        when(constats.findAllById(any())).thenReturn(List.of(c));
        doThrow(new RuntimeException("base indisponible"))
                .when(file).deposer(eq("a.traore@exemple.bf"), any(), any(), any(), any());

        notification.apresPublication(new ConstatsPublies(audit.getId(), List.of(c.getId())));

        verify(file, times(2)).deposer(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Un annuaire injoignable se dit dans le journal, sans défaire la validation")
    void annuaireInjoignable() {
        Constat c = constat(StatutConstat.PUBLIE, true);
        when(constats.findAllById(any())).thenReturn(List.of(c));
        when(annuaire.porteursDuRole(anyString(), anyString())).thenThrow(new RuntimeException("503"));

        notification.apresPublication(new ConstatsPublies(audit.getId(), List.of(c.getId())));

        verify(file, never()).deposer(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Le gabarit se rend : le pilote, l'audit, chaque constat, les écarts et le lien")
    void leGabaritSeRend() {
        Context contexte = new Context();
        contexte.setVariable("nomComplet", "Awa Traoré");
        contexte.setVariable("reference", "AUD-2026-012");
        contexte.setVariable("processus", "Gestion des ressources humaines");
        contexte.setVariable("constats", List.of(
                Map.of("nature", "Non-conformité mineure", "ecart", true, "point", "ISO 9001 §7.5",
                        "observation", "Registres non complétés le jour même."),
                new java.util.HashMap<>(Map.of("nature", "Conforme", "ecart", false,
                        "observation", "Procédure appliquée."))));
        contexte.setVariable("ecarts", 1L);
        contexte.setVariable("lien", "https://app.qualisira.com/audit/42");

        String html = ThymeleafConfig.getTemplateEngine().process(NotificationDuPilote.GABARIT, contexte);

        assertThat(html).contains("Bonjour Awa Traoré,", "2 constats", "AUD-2026-012",
                "Gestion des ressources humaines", "Registres non complétés le jour même.",
                "Procédure appliquée.", "Écarts parmi eux", "https://app.qualisira.com/audit/42");
    }

    @Test
    @DisplayName("La validation ne parle jamais à la messagerie : elle dépose, la tournée remettra")
    void laValidationDeposeSeulement() {
        Constat c = constat(StatutConstat.PUBLIE, true);
        when(constats.findAllById(any())).thenReturn(List.of(c));

        notification.apresPublication(new ConstatsPublies(audit.getId(), List.of(c.getId())));

        verify(file, times(2)).deposer(any(), any(), any(), any(), eq("publication-constats"));
        verify(file, never()).tournee(org.mockito.ArgumentMatchers.anyInt());
    }
}
