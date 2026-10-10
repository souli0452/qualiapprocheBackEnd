package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.client.Annuaire;
import com.qualiapproche.evaluation.client.Parametrage;
import com.qualiapproche.evaluation.dto.ActionMaitriseRisqueDto;
import com.qualiapproche.evaluation.dto.AuditDto;
import com.qualiapproche.evaluation.dto.RedactionRapportDto;
import com.qualiapproche.evaluation.entities.ActionMaitriseRisque;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.ChecklistAudit;
import com.qualiapproche.evaluation.entities.EvaluationRQAPBF;
import com.qualiapproche.evaluation.entities.NoeudReferentiel;
import com.qualiapproche.evaluation.entities.Signature;
import com.qualiapproche.evaluation.entities.TypeAudit;
import com.qualiapproche.evaluation.enumeration.NiveauEfficacite;
import com.qualiapproche.evaluation.enumeration.OrigineDocument;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutAuditeur;
import com.qualiapproche.evaluation.enumeration.StatutChecklist;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import com.qualiapproche.evaluation.enumeration.StatutSignature;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.ChecklistAuditRepository;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.EvaluationRQAPBFRepository;
import com.qualiapproche.evaluation.repository.NoeudReferentielRepository;
import com.qualiapproche.evaluation.repository.SignatureRepository;
import com.qualiapproche.evaluation.repository.TypeAuditRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le programme d'audit vu du service : ce qui se supprime, ce qui se valide en lot, et surtout ce
 * qui empêche une clôture.
 *
 * <p>La clôture est le geste qui fige le rapport : elle ne doit passer ni avec un constat encore
 * en suspens, ni sans rapport, ni avec un circuit de signature incomplet. Ces mêmes raisons
 * grisent le bouton à l'écran ; un test qui les fixe empêche les deux de diverger.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgrammeEtClotureDAuditTest {

    private static final UUID DIRECTION = UUID.randomUUID();
    private static final LocalDate DEBUT = LocalDate.of(2026, 11, 2);
    private static final LocalDate FIN = LocalDate.of(2026, 11, 4);

    @Mock private AuditRepository depot;
    @Mock private TypeAuditRepository types;
    @Mock private Parametrage parametrage;
    @Mock private ChecklistAuditRepository checklists;
    @Mock private ConstatRepository constats;
    @Mock private SignatureRepository signatures;
    @Mock private NoeudReferentielRepository noeuds;
    @Mock private EvaluationRQAPBFRepository evaluations;
    @Mock private AuditeurService auditeurs;
    @Mock private CodificationDocumentService codification;
    @Mock private FichiersAudit fichiers;
    @Mock private Annuaire annuaire;

    @InjectMocks private AuditService service;

    private Audit audit;

    private Audit audit(StatutAudit statut) {
        Audit a = Audit.builder().reference("AUD-2026-" + UUID.randomUUID().toString().substring(0, 4))
                .statut(statut).processusId(UUID.randomUUID()).dateDebutPrevue(DEBUT).dateFinPrevue(FIN).build();
        a.setId(UUID.randomUUID());
        a.setDirectionId(DIRECTION);
        when(depot.findById(a.getId())).thenReturn(Optional.of(a));
        return a;
    }

    private static Signature signature(StatutSignature statut, int ordre) {
        return Signature.builder().signataireId("signataire-" + ordre).fonctionSignataire("Directeur qualité")
                .ordre(ordre).statut(statut).build();
    }

    @BeforeEach
    void preparer() {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject("responsable-programme").claim("direction_id", DIRECTION.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(depot.save(any(Audit.class))).thenAnswer(i -> i.getArgument(0));
        audit = audit(StatutAudit.EN_COURS);
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ Clôture

    @Test
    @DisplayName("Un audit dont tous les constats sont tranchés et le rapport signé par tout le circuit se clôture")
    void un_audit_tranche_et_signe_se_cloture() {
        when(signatures.findAllByAuditIdOrderByOrdreAsc(audit.getId()))
                .thenReturn(List.of(signature(StatutSignature.SIGNEE, 1), signature(StatutSignature.SIGNEE, 2)));

        assertThat(service.raisonsDeNePasCloturer(audit)).isEmpty();
        assertThat(service.cloturer(audit.getId()).getStatut()).isEqualTo(StatutAudit.CLOTURE);
    }

    @Test
    @DisplayName("Des constats en brouillon ou en compilation empêchent la clôture, et la raison le dit")
    void des_constats_en_suspens_empechent_la_cloture() {
        when(constats.countByAuditIdAndStatutIn(any(), any())).thenReturn(2L);
        when(signatures.findAllByAuditIdOrderByOrdreAsc(audit.getId()))
                .thenReturn(List.of(signature(StatutSignature.SIGNEE, 1)));

        assertThatThrownBy(() -> service.cloturer(audit.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("2 constat(s)");
        assertThat(audit.getStatut()).isEqualTo(StatutAudit.EN_COURS);
        verify(depot, never()).save(any());
    }

    @Test
    @DisplayName("Sans circuit de signature posé, l'audit ne se clôture pas")
    void sans_circuit_pas_de_cloture() {
        when(signatures.findAllByAuditIdOrderByOrdreAsc(audit.getId())).thenReturn(List.of());

        assertThat(service.raisonsDeNePasCloturer(audit)).singleElement().asString()
                .contains("Aucun circuit de signature");
    }

    @Test
    @DisplayName("Un circuit dont un visa manque encore empêche la clôture")
    void un_visa_manquant_empeche_la_cloture() {
        when(signatures.findAllByAuditIdOrderByOrdreAsc(audit.getId()))
                .thenReturn(List.of(signature(StatutSignature.SIGNEE, 1), signature(StatutSignature.EN_ATTENTE, 2)));

        assertThat(service.raisonsDeNePasCloturer(audit)).singleElement().asString()
                .contains("pas encore signé par tout le circuit");
    }

    @Test
    @DisplayName("Un type d'audit à circuit externe exige le rapport signé chargé, et ignore le circuit interne")
    void un_circuit_externe_exige_le_rapport_charge() {
        TypeAudit externe = TypeAudit.builder().libelle("Certification").circuitExterne(true).build();
        audit.setType(externe);

        assertThat(service.raisonsDeNePasCloturer(audit)).singleElement().asString().contains("chargez le rapport");

        audit.setFichierRapport("audits/rapport.pdf");
        assertThat(service.raisonsDeNePasCloturer(audit)).isEmpty();
        verify(signatures, never()).findAllByAuditIdOrderByOrdreAsc(any());
    }

    @Test
    @DisplayName("Toutes les raisons se donnent, dans l'ordre où on les lève")
    void toutes_les_raisons_dans_l_ordre() {
        Audit prepare = audit(StatutAudit.EN_PREPARATION);
        when(constats.countByAuditIdAndStatutIn(any(), any())).thenReturn(1L);
        when(signatures.findAllByAuditIdOrderByOrdreAsc(prepare.getId())).thenReturn(List.of());

        assertThat(service.raisonsDeNePasCloturer(prepare)).hasSize(3)
                .satisfies(r -> assertThat(r.get(0)).contains("seul un audit en cours"))
                .satisfies(r -> assertThat(r.get(1)).contains("constat(s)"))
                .satisfies(r -> assertThat(r.get(2)).contains("circuit de signature"));
    }

    // ------------------------------------------------------------------ Programme

    @Test
    @DisplayName("Seul un audit planifié se supprime ; au-delà il s'annule")
    void seul_un_audit_planifie_se_supprime() {
        Audit planifie = audit(StatutAudit.PLANIFIE);

        service.supprimer(planifie.getId());
        assertThatThrownBy(() -> service.supprimer(audit.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("il s'annule");

        verify(depot).delete(planifie);
        verify(depot, never()).delete(audit);
    }

    @Test
    @DisplayName("L'audit d'une autre direction est introuvable")
    void l_audit_d_une_autre_direction_est_introuvable() {
        audit.setDirectionId(UUID.randomUUID());

        assertThatThrownBy(() -> service.lire(audit.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Aucun(e) audit");
    }

    @Test
    @DisplayName("La validation du programme s'arrête au premier audit qui ne passe pas")
    void la_validation_du_programme_s_arrete_au_premier_refus() {
        Auditeur responsable = Auditeur.builder().utilisateurId("resp").statut(StatutAuditeur.ACTIF).build();
        responsable.setId(UUID.randomUUID());
        Audit pret = audit(StatutAudit.PLANIFIE);
        pret.setResponsableEquipe(responsable);
        Audit sansResponsable = audit(StatutAudit.PLANIFIE);
        Audit jamaisAtteint = audit(StatutAudit.PLANIFIE);
        jamaisAtteint.setResponsableEquipe(responsable);
        when(auditeurs.estDisponible(any(Auditeur.class), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.validerProgramme(
                List.of(pret.getId(), sansResponsable.getId(), jamaisAtteint.getId())))
                .isInstanceOf(BusinessException.class).hasMessageContaining(sansResponsable.getReference());

        assertThat(jamaisAtteint.getStatut()).isEqualTo(StatutAudit.PLANIFIE);
    }

    @Test
    @DisplayName("Un auditeur inactif ou déjà engagé bloque la validation de l'audit")
    void un_auditeur_indisponible_bloque_la_validation() {
        Auditeur responsable = Auditeur.builder().utilisateurId("resp").statut(StatutAuditeur.ACTIF).build();
        responsable.setId(UUID.randomUUID());
        Audit planifie = audit(StatutAudit.PLANIFIE);
        planifie.setResponsableEquipe(responsable);
        when(auditeurs.estDisponible(any(Auditeur.class), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.valider(planifie.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("déjà engagé");
        assertThat(planifie.getStatut()).isEqualTo(StatutAudit.PLANIFIE);
    }

    @Test
    @DisplayName("La fiche dit à l'utilisateur connecté s'il est de l'équipe, responsable ou associé")
    void la_fiche_dit_si_je_suis_de_l_equipe() {
        Auditeur responsable = Auditeur.builder().utilisateurId("responsable-programme").build();
        Auditeur associee = Auditeur.builder().utilisateurId("awa").build();
        audit.setResponsableEquipe(responsable);
        assertThat(service.lire(audit.getId()).isJeSuisDeLEquipe()).isTrue();

        audit.setResponsableEquipe(null);
        assertThat(service.lire(audit.getId()).isJeSuisDeLEquipe()).isFalse();

        audit.getMembresEquipe().add(associee);
        Jwt awa = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject("awa").claim("direction_id", DIRECTION.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(awa));
        assertThat(service.lire(audit.getId()).isJeSuisDeLEquipe()).isTrue();
    }

    @Test
    @DisplayName("Le refus nomme l'auditeur, et ne montre jamais son identifiant technique")
    void le_refus_nomme_l_auditeur() {
        Auditeur responsable = Auditeur.builder().utilisateurId("7f3c-uuid-keycloak").statut(StatutAuditeur.ACTIF).build();
        responsable.setId(UUID.randomUUID());
        Audit planifie = audit(StatutAudit.PLANIFIE);
        planifie.setResponsableEquipe(responsable);
        when(auditeurs.estDisponible(any(Auditeur.class), any(), any(), any())).thenReturn(false);

        when(annuaire.personne("7f3c-uuid-keycloak")).thenReturn("Awa Traoré");
        assertThatThrownBy(() -> service.valider(planifie.getId()))
                .hasMessageStartingWith("L'auditeur Awa Traoré est inactif")
                .hasMessageNotContaining("7f3c-uuid-keycloak");

        // Annuaire injoignable : il rend l'identifiant, que le message ne doit pas reprendre.
        when(annuaire.personne("7f3c-uuid-keycloak")).thenReturn("7f3c-uuid-keycloak");
        assertThatThrownBy(() -> service.valider(planifie.getId()))
                .hasMessageStartingWith("Un auditeur de l'équipe est inactif")
                .hasMessageNotContaining("7f3c-uuid-keycloak");
    }

    @Test
    @DisplayName("Un programme vide se valide sans rien toucher")
    void un_programme_vide_ne_touche_rien() {
        assertThat(service.validerProgramme(List.of())).isEmpty();
        assertThat(service.validerProgramme(null)).isEmpty();
        verify(depot, never()).save(any());
    }

    // ------------------------------------------------------------------ Saisie du programme

    private AuditDto saisie() {
        return AuditDto.builder().processusId(UUID.randomUUID()).dateDebutPrevue(DEBUT).dateFinPrevue(FIN).build();
    }

    @Test
    @DisplayName("Un audit créé part planifié, avec la référence de la codification et sa durée calculée")
    void un_audit_cree_part_planifie() {
        when(codification.genererReference(CodificationDocumentService.AUDIT)).thenReturn("AUD-2026-042");
        AuditDto saisie = saisie();
        saisie.setReferentiels(List.of(" ISO 9001 ", "ISO 9001", "", "RQAP-BF"));

        AuditDto cree = service.creer(saisie);

        assertThat(cree.getReference()).isEqualTo("AUD-2026-042");
        assertThat(cree.getStatut()).isEqualTo(StatutAudit.PLANIFIE);
        assertThat(cree.getDureeEstimeeJours()).isEqualTo(3);
        assertThat(cree.getReferentiels()).containsExactly("ISO 9001", "RQAP-BF");
    }

    @Test
    @DisplayName("Une date de fin antérieure au début est refusée")
    void une_fin_avant_le_debut_est_refusee() {
        AuditDto saisie = saisie();
        saisie.setDateFinPrevue(DEBUT.minusDays(1));

        assertThatThrownBy(() -> service.creer(saisie))
                .isInstanceOf(BusinessException.class).hasMessageContaining("précède");
        verify(depot, never()).save(any());
    }

    @Test
    @DisplayName("Un compte sans direction ne crée pas d'audit")
    void un_compte_sans_direction_ne_cree_pas() {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none").subject("orphelin").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThatThrownBy(() -> service.creer(saisie()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("aucune direction");
        verify(codification, never()).genererReference(anyString());
    }

    @Test
    @DisplayName("Une checklist encore en brouillon ne sert pas à un audit")
    void une_checklist_non_publiee_ne_sert_pas() {
        ChecklistAudit brouillon = ChecklistAudit.builder().nom("Ventes").statut(StatutChecklist.BROUILLON).build();
        brouillon.setId(UUID.randomUUID());
        brouillon.setDirectionId(DIRECTION);
        when(checklists.findById(brouillon.getId())).thenReturn(Optional.of(brouillon));
        Audit planifie = audit(StatutAudit.PLANIFIE);
        AuditDto saisie = saisie();
        saisie.setChecklistIds(List.of(brouillon.getId()));

        assertThatThrownBy(() -> service.modifier(planifie.getId(), saisie))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Ventes");
    }

    @Test
    @DisplayName("Une subdivision du référentiel RQAP-BF n'est pas un domaine d'audit")
    void une_subdivision_n_est_pas_un_domaine() {
        NoeudReferentiel domaine = NoeudReferentiel.builder().code("D1").build();
        NoeudReferentiel critere = NoeudReferentiel.builder().code("D1.2").parent(domaine).build();
        critere.setId(UUID.randomUUID());
        critere.setDirectionId(DIRECTION);
        when(noeuds.findById(critere.getId())).thenReturn(Optional.of(critere));
        Audit planifie = audit(StatutAudit.PLANIFIE);
        AuditDto saisie = saisie();
        saisie.setDomaineRqapbfIds(List.of(critere.getId()));

        assertThatThrownBy(() -> service.modifier(planifie.getId(), saisie))
                .isInstanceOf(BusinessException.class).hasMessageContaining("D1.2");
    }

    @Test
    @DisplayName("Un audit démarré ne se replanifie plus")
    void un_audit_demarre_ne_se_replanifie_plus() {
        assertThatThrownBy(() -> service.modifier(audit.getId(), saisie()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("ne se modifie plus");
    }

    @Test
    @DisplayName("Une action de maîtrise du risque reprise garde son évaluation d'efficacité")
    void une_action_reprise_garde_son_efficacite() {
        Audit planifie = audit(StatutAudit.PLANIFIE);
        ActionMaitriseRisque existante = ActionMaitriseRisque.builder().audit(planifie).libelle("Double contrôle")
                .efficacite(NiveauEfficacite.EFFICACE).build();
        existante.setId(UUID.randomUUID());
        planifie.setActionsMaitriseRisques(new ArrayList<>(List.of(existante)));
        AuditDto saisie = saisie();
        saisie.setActionsMaitriseRisques(List.of(
                ActionMaitriseRisqueDto.builder().id(existante.getId()).libelle("Double contrôle renforcé").build(),
                ActionMaitriseRisqueDto.builder().libelle("Formation").build()));

        AuditDto modifie = service.modifier(planifie.getId(), saisie);

        assertThat(modifie.getActionsMaitriseRisques()).hasSize(2);
        assertThat(modifie.getActionsMaitriseRisques().get(0).getEfficacite()).isEqualTo(existante.getEfficacite());
        assertThat(modifie.getActionsMaitriseRisques().get(0).getLibelle()).isEqualTo("Double contrôle renforcé");
        assertThat(modifie.getActionsMaitriseRisques().get(1).getEfficacite()).isEqualTo(NiveauEfficacite.NON_EVALUEE);
    }

    @Test
    @DisplayName("Évaluer une action étrangère à l'audit est refusé")
    void evaluer_une_action_etrangere_est_refuse() {
        assertThatThrownBy(() -> service.evaluerEfficacite(audit.getId(), UUID.randomUUID(),
                NiveauEfficacite.NON_EVALUEE))
                .isInstanceOf(BusinessException.class).hasMessageContaining(audit.getReference());
    }

    // ------------------------------------------------------------------ Rapport

    @Test
    @DisplayName("Le rapport ne se rédige que pendant l'audit")
    void le_rapport_ne_se_redige_que_pendant_l_audit() {
        Audit cloture = audit(StatutAudit.CLOTURE);

        assertThatThrownBy(() -> service.rediger(cloture.getId(), new RedactionRapportDto()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("pendant l'audit");
    }

    @Test
    @DisplayName("Un rapport visé ne se réécrit pas")
    void un_rapport_vise_ne_se_reecrit_pas() {
        when(signatures.existsByAuditIdAndStatut(audit.getId(), StatutSignature.SIGNEE)).thenReturn(true);
        RedactionRapportDto redaction = new RedactionRapportDto();
        redaction.setConclusions("Nouvelles conclusions");

        assertThatThrownBy(() -> service.rediger(audit.getId(), redaction)).isInstanceOf(BusinessException.class);
        assertThat(audit.getConclusionsRapport()).isNull();
    }

    @Test
    @DisplayName("Le rapport d'un audit clôturé ne se remplace plus")
    void le_rapport_d_un_audit_cloture_ne_se_remplace_plus() {
        Audit cloture = audit(StatutAudit.CLOTURE);

        assertThatThrownBy(() -> service.chargerRapport(cloture.getId(), mock(MultipartFile.class)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("ne se remplace plus");
        verify(fichiers, never()).deposer(any(), any(String[].class));
    }

    @Test
    @DisplayName("Un rapport chargé reçoit une référence une seule fois, et garde la première")
    void un_rapport_charge_garde_sa_premiere_reference() {
        when(fichiers.deposer(any(), any(String[].class))).thenReturn("audits/rapport.pdf");
        when(codification.genererReference(CodificationDocumentService.RAPPORT_AUDIT)).thenReturn("RAP-001", "RAP-002");

        service.chargerRapport(audit.getId(), mock(MultipartFile.class));
        AuditDto recharge = service.chargerRapport(audit.getId(), mock(MultipartFile.class));

        assertThat(recharge.getReferenceRapport()).isEqualTo("RAP-001");
        assertThat(recharge.getOrigineRapport()).isEqualTo(OrigineDocument.CHARGE);
        assertThat(recharge.isRapportCharge()).isTrue();
    }

    @Test
    @DisplayName("Le score de maturité retient l'évaluation validée plutôt que la plus récente")
    void le_score_retient_l_evaluation_validee() {
        EvaluationRQAPBF validee = EvaluationRQAPBF.builder().audit(audit).statut(StatutEvaluation.VALIDEE)
                .scoreGlobal(2.5).dateEvaluation(LocalDate.of(2026, 1, 1)).build();
        EvaluationRQAPBF recente = EvaluationRQAPBF.builder().audit(audit).statut(StatutEvaluation.BROUILLON)
                .scoreGlobal(3.8).dateEvaluation(LocalDate.of(2026, 6, 1)).build();
        when(evaluations.findAllByAuditIdIn(List.of(audit.getId()))).thenReturn(List.of(recente, validee));

        assertThat(service.scoresDeMaturite(List.of(audit.getId()))).containsEntry(audit.getId(), 2.5);
    }
}
