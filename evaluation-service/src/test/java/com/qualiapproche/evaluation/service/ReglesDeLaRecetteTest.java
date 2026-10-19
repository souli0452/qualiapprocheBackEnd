package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.ActivitePlanDto;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.RegleNotification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Les règles posées après la recette du module Audit du 17 octobre 2026. */
class ReglesDeLaRecetteTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 10, 22);
    private static final LocalDate FIN = LocalDate.of(2026, 10, 23);

    private static Audit audit(StatutAudit statut) {
        return Audit.builder().reference("QS-2026-007").statut(statut).dateDebutPrevue(DEBUT).dateFinPrevue(FIN).build();
    }

    private static ActivitePlanDto activite(LocalDate jour, String debut, String fin) {
        return ActivitePlanDto.builder().libelleActivite("Réunion d'ouverture").jour(jour)
                .horaireDebut(debut).horaireFin(fin).build();
    }

    @Test
    @DisplayName("Obs. 15 — un créneau qui finit avant d'avoir commencé est refusé")
    void creneau_inverse_refuse() {
        assertThatThrownBy(() -> PlanAuditService.verifierLeCreneau(audit(StatutAudit.EN_PREPARATION),
                activite(DEBUT, "09:00", "08:30")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("avant d'avoir commencé");
    }

    @Test
    @DisplayName("Obs. 15 — une activité hors de la période de l'audit est refusée")
    void activite_hors_periode_refusee() {
        assertThatThrownBy(() -> PlanAuditService.verifierLeCreneau(audit(StatutAudit.EN_PREPARATION),
                activite(FIN.plusDays(1), "09:00", "10:00")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("hors de la période");
        assertThatCode(() -> PlanAuditService.verifierLeCreneau(audit(StatutAudit.EN_PREPARATION),
                activite(FIN, "08:30", "09:00"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Obs. 13 — le démarrage retient le jour réel, même avant la date prévue")
    void demarrage_retient_le_jour_reel() {
        Audit audit = audit(StatutAudit.EN_PREPARATION);
        audit.demarrer();
        assertThat(audit.getDateDebutReelle()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Obs. 14 — aucun rappel avant démarrage pour un audit déjà en cours")
    void pas_de_rappel_pour_un_audit_en_cours() {
        RegleNotification j10 = RegleNotification.builder().libelle("Rappel J-10").delaiJours(10).actif(true).build();
        LocalDate jMoins10 = DEBUT.minusDays(10);
        assertThat(j10.declencherSiEcheance(audit(StatutAudit.EN_PREPARATION), jMoins10)).isTrue();
        assertThat(j10.declencherSiEcheance(audit(StatutAudit.EN_COURS), jMoins10)).isFalse();
    }

    @Test
    @DisplayName("Obs. 18 — un plan validé se révise dans une nouvelle version, sans perdre qui l'a rouvert")
    void un_plan_valide_se_revise() {
        com.qualiapproche.evaluation.entities.PlanAudit plan = com.qualiapproche.evaluation.entities.PlanAudit.builder()
                .statut(com.qualiapproche.evaluation.enumeration.StatutPlanAudit.PARTAGE).build();
        plan.reviser("Aïcha OUEDRAOGO");
        assertThat(plan.getStatut()).isEqualTo(com.qualiapproche.evaluation.enumeration.StatutPlanAudit.BROUILLON);
        assertThat(plan.getVersion()).isEqualTo(2);
        assertThat(plan.getRevisePar()).isEqualTo("Aïcha OUEDRAOGO");
        assertThat(plan.getDatePartage()).isNull();
        assertThatThrownBy(() -> plan.reviser("x")).isInstanceOf(BusinessException.class);
    }
}
