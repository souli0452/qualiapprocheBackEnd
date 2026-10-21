package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Les transitions de l'audit, et le retard qui n'en est une que dans un seul cas. */
class CycleDeVieDeLAuditTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 10, 12);
    private static final LocalDate FIN = LocalDate.of(2026, 10, 14);

    private static Audit audit(StatutAudit statut) {
        Auditeur responsable = Auditeur.builder().utilisateurId("rq").build();
        responsable.setId(UUID.randomUUID());
        return Audit.builder().reference("AUD-2026-0001").statut(statut)
                .dateDebutPrevue(DEBUT).dateFinPrevue(FIN).responsableEquipe(responsable).build();
    }

    @Test
    @DisplayName("Le chemin nominal va de planifié à clôturé")
    void le_chemin_nominal_va_de_planifie_a_cloture() {
        Audit audit = audit(StatutAudit.PLANIFIE);
        audit.valider();
        assertThat(audit.getStatut()).isEqualTo(StatutAudit.EN_PREPARATION);
        audit.demarrer();
        assertThat(audit.getStatut()).isEqualTo(StatutAudit.EN_COURS);
        audit.cloturer();
        assertThat(audit.getStatut()).isEqualTo(StatutAudit.CLOTURE);
    }

    @Test
    @DisplayName("Un audit sans responsable ne se valide pas")
    void un_audit_sans_responsable_ne_se_valide_pas() {
        Audit audit = audit(StatutAudit.PLANIFIE);
        audit.setResponsableEquipe(null);
        assertThatThrownBy(audit::valider).isInstanceOf(BusinessException.class)
                .hasMessageContaining("responsable");
    }

    @Test
    @DisplayName("On ne démarre pas un audit dont le programme n'est pas validé")
    void on_ne_demarre_pas_un_audit_dont_le_programme_n_est_pas_valide() {
        assertThatThrownBy(audit(StatutAudit.PLANIFIE)::demarrer).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Un audit terminé ne s'annule plus")
    void un_audit_termine_ne_s_annule_plus() {
        assertThatThrownBy(audit(StatutAudit.CLOTURE)::annuler).isInstanceOf(BusinessException.class);
        Audit enCours = audit(StatutAudit.EN_COURS);
        enCours.annuler();
        assertThat(enCours.getStatut()).isEqualTo(StatutAudit.ANNULE);
    }

    @Test
    @DisplayName("Seul l'audit validé non démarré passe en retard et peut encore démarrer")
    void seul_l_audit_valide_non_demarre_passe_en_retard_et_peut_encore_demarrer() {
        LocalDate lendemain = DEBUT.plusDays(1);
        Audit planifie = audit(StatutAudit.PLANIFIE);
        Audit prepare = audit(StatutAudit.EN_PREPARATION);

        assertThat(planifie.marquerEnRetardSiEcheance(lendemain)).isFalse();
        assertThat(planifie.estEnRetard(lendemain)).isTrue();
        assertThat(prepare.marquerEnRetardSiEcheance(DEBUT)).as("le jour même n'est pas un retard").isFalse();
        assertThat(prepare.marquerEnRetardSiEcheance(lendemain)).isTrue();
        assertThat(prepare.getStatut()).isEqualTo(StatutAudit.EN_RETARD);

        prepare.demarrer();
        assertThat(prepare.getStatut()).isEqualTo(StatutAudit.EN_COURS);
    }

    @Test
    @DisplayName("Un audit en cours qui déborde est en retard sans changer de statut")
    void un_audit_en_cours_qui_deborde_est_en_retard_sans_changer_de_statut() {
        Audit audit = audit(StatutAudit.EN_COURS);
        assertThat(audit.estEnRetard(FIN.plusDays(1))).isTrue();
        assertThat(audit.marquerEnRetardSiEcheance(FIN.plusDays(1))).isFalse();
        assertThat(audit.getStatut()).isEqualTo(StatutAudit.EN_COURS);
    }

    @Test
    @DisplayName("On ne s'audite pas soi-même")
    void on_ne_s_audite_pas_soi_meme() {
        UUID processus = UUID.randomUUID();
        Audit audit = audit(StatutAudit.PLANIFIE);
        audit.setProcessusId(processus);

        assertThat(Auditeur.builder().structureAppartenance(processus).build().aConflitInteret(audit)).isTrue();
        assertThat(Auditeur.builder().processusGeres(new java.util.ArrayList<>(List.of(processus.toString())))
                .build().aConflitInteret(audit)).isTrue();
        assertThat(Auditeur.builder().structureAppartenance(UUID.randomUUID()).build().aConflitInteret(audit)).isFalse();
    }
}
