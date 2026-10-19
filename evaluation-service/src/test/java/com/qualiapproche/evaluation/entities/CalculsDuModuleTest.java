package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.enumeration.ConstatRQAPBF;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutChecklist;
import com.qualiapproche.evaluation.enumeration.StatutSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Scores RQAPBF, numérotation, échéances, signatures et checklists. */
class CalculsDuModuleTest {

    /** La règle d'une direction qui n'en a pas posé d'autre. */
    private static final java.util.Set<NiveauNotationRQAPBF> PAR_DEFAUT = java.util.EnumSet.of(
            NiveauNotationRQAPBF.NON_CONFORME, NiveauNotationRQAPBF.A_AMELIORER);


    private static NotationCritere note(NoeudReferentiel critere, NiveauNotationRQAPBF niveau) {
        return NotationCritere.builder().noeud(critere).niveau(niveau).build();
    }

    @Test
    @DisplayName("Le score ignore les critères non applicables et se ventile par domaine")
    void le_score_ignore_les_criteres_non_applicables_et_se_ventile_par_domaine() {
        NoeudReferentiel d1 = NoeudReferentiel.builder().code("D1").libelle("Gouvernance").ordre(1).build();
        NoeudReferentiel d2 = NoeudReferentiel.builder().code("D2").libelle("Ressources").ordre(2).build();
        NoeudReferentiel c11 = NoeudReferentiel.builder().code("C1.1").libelle("a").build();
        NoeudReferentiel c12 = NoeudReferentiel.builder().code("C1.2").libelle("b").build();
        NoeudReferentiel c21 = NoeudReferentiel.builder().code("C2.1").libelle("c").build();
        d1.ajouterEnfant(c11);
        d1.ajouterEnfant(c12);
        d2.ajouterEnfant(c21);

        EvaluationRQAPBF evaluation = new EvaluationRQAPBF();
        evaluation.getNotations().add(note(c11, NiveauNotationRQAPBF.CONFORME));
        evaluation.getNotations().add(note(c12, NiveauNotationRQAPBF.NON_CONFORME));
        evaluation.getNotations().add(note(c21, NiveauNotationRQAPBF.NON_APPLICABLE));

        assertThat(evaluation.calculerScoreGlobal()).isEqualTo(50.0);
        assertThat(evaluation.getNiveauConformite()).isEqualTo("A_AMELIORER");
        Map<String, Double> parDomaine = evaluation.genererResultatsParDomaine();
        assertThat(parDomaine).containsKeys("D1 — Gouvernance", "D2 — Ressources");
        assertThat(parDomaine.get("D1 — Gouvernance")).isEqualTo(50.0);
        assertThat(parDomaine.get("D2 — Ressources")).isNull();
        assertThat(evaluation.getNotations().get(1).constatRetenu(PAR_DEFAUT)).isEqualTo(ConstatRQAPBF.NC);
    }

    @Test
    @DisplayName("Les résultats officiels ne comptent que les critères publiés et chaque section a son score")
    void les_resultats_officiels_ne_comptent_que_les_criteres_publies_et_chaque_section_a_son_score() {
        NoeudReferentiel domaine = NoeudReferentiel.builder().code("D1").libelle("Gouvernance").ordre(1).build();
        NoeudReferentiel section = NoeudReferentiel.builder().code("D1-S1").libelle("Organisation").build();
        NoeudReferentiel c1 = NoeudReferentiel.builder().code("E1.1-C1").libelle("a").build();
        NoeudReferentiel c2 = NoeudReferentiel.builder().code("E1.1-C2").libelle("b").build();
        domaine.ajouterEnfant(section);
        section.ajouterEnfant(c1);
        section.ajouterEnfant(c2);

        EvaluationRQAPBF evaluation = new EvaluationRQAPBF();
        NotationCritere publie = note(c1, NiveauNotationRQAPBF.CONFORME);
        publie.setStatut(com.qualiapproche.evaluation.enumeration.StatutNotation.PUBLIE);
        NotationCritere brouillon = note(c2, NiveauNotationRQAPBF.NON_CONFORME);
        brouillon.setStatut(com.qualiapproche.evaluation.enumeration.StatutNotation.BROUILLON);
        evaluation.getNotations().add(publie);
        evaluation.getNotations().add(brouillon);

        assertThat(evaluation.scoreOfficiel()).isEqualTo(100.0);
        assertThat(evaluation.genererResultatsParDomaine(false)).containsEntry("D1 — Gouvernance", 50.0);
        assertThat(evaluation.scoresParNoeud(false)).containsEntry("D1-S1", 50.0).containsEntry("D1", 50.0);
        assertThat(evaluation.scoresParNoeud(true)).containsEntry("D1-S1", 100.0);
    }

    @Test
    @DisplayName("Les jours de retard se comptent sur la date qui compte")
    void les_jours_de_retard_se_comptent_sur_la_date_qui_compte() {
        LocalDate debut = LocalDate.of(2026, 10, 1);
        Audit pasDemarre = Audit.builder().statut(StatutAudit.EN_PREPARATION)
                .dateDebutPrevue(debut).dateFinPrevue(debut.plusDays(2)).build();
        Audit enCours = Audit.builder().statut(StatutAudit.EN_COURS)
                .dateDebutPrevue(debut).dateFinPrevue(debut.plusDays(2)).build();
        assertThat(pasDemarre.joursDeRetard(debut.plusDays(12))).isEqualTo(12);
        assertThat(enCours.joursDeRetard(debut.plusDays(12))).isEqualTo(10);
        assertThat(enCours.joursDeRetard(debut.plusDays(1))).isNull();
    }

    @Test
    @DisplayName("La numérotation suit le format de la maquette et repart de un chaque année")
    void la_numerotation_suit_le_format_de_la_maquette_et_repart_de_un_chaque_annee() {
        CodificationDocument audit = CodificationDocument.builder().typeDocument("AUD")
                .prefixe("AUD").separateur("-").annee("2025").compteur(41).build();
        assertThat(audit.genererProchaineReference(2025)).isEqualTo("AUD-2025-042");
        assertThat(audit.genererProchaineReference(2026)).isEqualTo("AUD-2026-001");

        CodificationDocument plan = CodificationDocument.builder().typeDocument("PLA")
                .prefixe("ABMAQ").separateur("-").annee("2026").compteur(12).build();
        assertThat(plan.genererProchaineReference(2026)).isEqualTo("ABMAQ-PLA-2026-013");
    }

    @Test
    @DisplayName("Par défaut, « Acceptable » n'est pas un écart ; le non applicable ne donne aucun constat")
    void par_defaut_acceptable_n_est_pas_un_ecart_et_le_non_applicable_ne_donne_aucun_constat() {
        NoeudReferentiel critere = NoeudReferentiel.builder().code("E1.1-C1").libelle("x").build();
        assertThat(note(critere, NiveauNotationRQAPBF.CONFORME).constatRetenu(PAR_DEFAUT)).isEqualTo(ConstatRQAPBF.CONFORME);
        assertThat(note(critere, NiveauNotationRQAPBF.ACCEPTABLE).constatRetenu(PAR_DEFAUT)).isEqualTo(ConstatRQAPBF.CONFORME);
        assertThat(note(critere, NiveauNotationRQAPBF.A_AMELIORER).constatRetenu(PAR_DEFAUT)).isEqualTo(ConstatRQAPBF.NC);
        assertThat(note(critere, NiveauNotationRQAPBF.NON_CONFORME).constatRetenu(PAR_DEFAUT)).isEqualTo(ConstatRQAPBF.NC);
        assertThat(note(critere, NiveauNotationRQAPBF.NON_APPLICABLE).constatRetenu(PAR_DEFAUT)).isNull();
        // Une direction qui tient « Acceptable » pour un écart, comme la maquette le voulait.
        assertThat(note(critere, NiveauNotationRQAPBF.ACCEPTABLE).constatRetenu(java.util.EnumSet.of(
                NiveauNotationRQAPBF.NON_CONFORME, NiveauNotationRQAPBF.A_AMELIORER, NiveauNotationRQAPBF.ACCEPTABLE)))
                .isEqualTo(ConstatRQAPBF.NC);
        assertThat(note(critere, NiveauNotationRQAPBF.A_AMELIORER).calculerScore()).isEqualTo(45.0);
        assertThatThrownBy(note(critere, null)::publier).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Une règle d'échéance part le jour exact et jamais pour un audit terminé")
    void une_regle_d_echeance_part_le_jour_exact_et_jamais_pour_un_audit_termine() {
        LocalDate debut = LocalDate.of(2026, 10, 12);
        RegleNotification j7 = RegleNotification.builder().libelle("J-7").delaiJours(7).actif(true).build();
        Audit audit = Audit.builder().statut(StatutAudit.EN_PREPARATION).dateDebutPrevue(debut).build();

        assertThat(j7.declencherSiEcheance(audit, debut.minusDays(7))).isTrue();
        assertThat(j7.declencherSiEcheance(audit, debut.minusDays(6))).isFalse();
        audit.setStatut(StatutAudit.ANNULE);
        assertThat(j7.declencherSiEcheance(audit, debut.minusDays(7))).isFalse();
    }

    @Test
    @DisplayName("Un refus de signature se motive et un visa ne se donne qu'une fois")
    void un_refus_de_signature_se_motive_et_un_visa_ne_se_donne_qu_une_fois() {
        Signature visa = Signature.builder().statut(StatutSignature.EN_ATTENTE).build();
        assertThatThrownBy(() -> visa.refuser("  ")).isInstanceOf(BusinessException.class);
        visa.signer();
        assertThat(visa.getStatut()).isEqualTo(StatutSignature.SIGNEE);
        assertThatThrownBy(visa::signer).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Une checklist vide ne se publie pas et sa copie repart en brouillon")
    void une_checklist_vide_ne_se_publie_pas_et_sa_copie_repart_en_brouillon() {
        ChecklistAudit checklist = ChecklistAudit.builder().nom("ISO 9001 §7").statut(StatutChecklist.BROUILLON).build();
        assertThatThrownBy(checklist::publier).isInstanceOf(BusinessException.class);

        checklist.getPoints().add(PointControle.builder().checklist(checklist).libelle("Compétences")
                .auditeurAssigne(new Auditeur()).build());
        checklist.publier();
        ChecklistAudit copie = checklist.dupliquer();
        assertThat(copie.getStatut()).isEqualTo(StatutChecklist.BROUILLON);
        assertThat(copie.getPoints()).hasSize(1);
        assertThat(copie.getPoints().get(0).getAuditeurAssigne()).isNull();
        assertThat(copie.getPoints().get(0).getChecklist()).isSameAs(copie);
    }
}
