package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.dto.CritereEvaluationAuditeurDto;
import com.qualiapproche.common.dto.NiveauEvaluationAuditeurDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.client.Parametrage;
import com.qualiapproche.evaluation.dto.EvaluationAuditeurDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.EvaluationAuditeur;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.repository.EvaluationAuditeurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L'évaluation d'un auditeur, sur un critère et un niveau paramétrés dans referentiel-service. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluationDesAuditeursTest {

    @Mock private EvaluationAuditeurRepository depot;
    @Mock private Parametrage parametrage;
    @Mock private AuditService audits;
    @Mock private AuditeurService auditeurs;

    @InjectMocks private EvaluationAuditeurService service;

    private final UUID critere = UUID.randomUUID();
    private final UUID niveau = UUID.randomUUID();
    private Audit audit;
    private Auditeur auditeur;

    @BeforeEach
    void preparer() {
        auditeur = Auditeur.builder().utilisateurId("auditeur-1").build();
        auditeur.setId(UUID.randomUUID());
        audit = Audit.builder().reference("AUD-2026-007").statut(StatutAudit.EN_COURS).responsableEquipe(auditeur).build();
        audit.setId(UUID.randomUUID());
        when(audits.sienOuRien(audit.getId())).thenReturn(audit);
        when(auditeurs.sienOuRien(auditeur.getId())).thenReturn(auditeur);
        when(depot.findByAuditIdAndAuditeurIdAndCritereId(any(), any(), any())).thenReturn(Optional.empty());
        when(depot.save(any(EvaluationAuditeur.class))).thenAnswer(i -> i.getArgument(0));
        when(parametrage.critereActif(critere)).thenReturn(
                CritereEvaluationAuditeurDto.builder().id(critere).libelle("Maîtrise du référentiel").actif(true).build());
        NiveauEvaluationAuditeurDto satisfaisant =
                NiveauEvaluationAuditeurDto.builder().id(niveau).libelle("Satisfaisant").ordre(3).actif(true).build();
        when(parametrage.niveauActif(niveau)).thenReturn(satisfaisant);
        when(parametrage.niveauxPourAffichage()).thenReturn(Map.of(niveau, satisfaisant));
        when(parametrage.criteresPourAffichage()).thenReturn(Map.of());
    }

    private EvaluationAuditeurDto saisie() {
        return EvaluationAuditeurDto.builder().auditId(audit.getId()).auditeurId(auditeur.getId())
                .critereId(critere).niveauId(niveau).build();
    }

    @Test
    @DisplayName("La valeur du niveau est recopiée : retoucher l'échelle ne réécrira pas la moyenne")
    void la_valeur_du_niveau_est_recopiee() {
        EvaluationAuditeurDto evaluee = service.evaluer(saisie());

        ArgumentCaptor<EvaluationAuditeur> enregistree = ArgumentCaptor.forClass(EvaluationAuditeur.class);
        verify(depot).save(enregistree.capture());
        assertThat(enregistree.getValue().getValeurNiveau()).isEqualTo(3);
        assertThat(enregistree.getValue().getCritereId()).isEqualTo(critere);
        assertThat(evaluee.getNiveauLibelle()).isEqualTo("Satisfaisant");
        assertThat(evaluee.getCritereLibelle()).as("libellé introuvable : affiché vide, sans échec").isNull();
    }

    @Test
    @DisplayName("Un critère retiré du paramétrage ne sert plus à évaluer")
    void un_critere_retire_ne_sert_plus() {
        when(parametrage.critereActif(critere))
                .thenThrow(new BusinessException("Aucun(e) critère d'évaluation actif(ve)", HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.evaluer(saisie())).isInstanceOf(BusinessException.class);
        verify(depot, never()).save(any());
    }
}
