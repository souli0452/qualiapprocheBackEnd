package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.AuditeurDto;
import com.qualiapproche.evaluation.dto.SignatureDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.Signature;
import com.qualiapproche.evaluation.entities.TypeAudit;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutAuditeur;
import com.qualiapproche.evaluation.enumeration.StatutSignature;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.AuditeurRepository;
import com.qualiapproche.evaluation.repository.EvaluationAuditeurRepository;
import com.qualiapproche.evaluation.repository.SignatureRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le circuit de signature du rapport, puis le vivier d'auditeurs.
 *
 * <p>Un visa est un acte nominatif : seul le signataire désigné le donne, et le nom comme la
 * fonction recopiés au circuit restent la trace de qui a visé quoi. Le vivier, lui, ne perd
 * jamais un auditeur : ses constats et ses équipes passées le citent.</p>
 */
class CircuitDeSignatureEtVivierTest {

    private static final UUID DIRECTION = UUID.randomUUID();

    static void connecter(String sujet) {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject(sujet).claim("direction_id", DIRECTION.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class CircuitDeSignature {

        @Mock private SignatureRepository depot;
        @Mock private AuditService audits;
        @Mock private AuditRepository depotDesAudits;
        @Mock private CodificationDocumentService codification;
        @Mock private RelanceDeSignature relance;

        @InjectMocks private SignatureService service;

        private Audit audit;

        @BeforeEach
        void preparer() {
            connecter("directeur-qualite");
            audit = Audit.builder().reference("AUD-2026-020").statut(StatutAudit.EN_COURS).build();
            audit.setId(UUID.randomUUID());
            audit.setDirectionId(DIRECTION);
            when(audits.sienOuRien(audit.getId())).thenReturn(audit);
            when(depot.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
            when(depot.save(any(Signature.class))).thenAnswer(i -> i.getArgument(0));
        }

        private Signature visa(String signataire, StatutSignature statut, int ordre) {
            return Signature.builder().audit(audit).signataireId(signataire).fonctionSignataire("Fonction " + ordre)
                    .ordre(ordre).statut(statut).build();
        }

        @Test
        @DisplayName("Poser le circuit numérote les visas, les met en attente et garde la fonction de chacun")
        void poser_le_circuit_numerote_les_visas() {
            when(codification.genererReference(CodificationDocumentService.RAPPORT_AUDIT)).thenReturn("RAP-2026-001");

            List<SignatureDto> circuit = service.poserLeCircuit(audit.getId(), List.of(
                    SignatureDto.builder().signataireId(" chef-equipe ").fonctionSignataire("Responsable d'équipe").build(),
                    SignatureDto.builder().signataireId("directeur-qualite").fonctionSignataire("Directeur qualité").build()));

            assertThat(circuit).extracting(SignatureDto::getOrdre).containsExactly(1, 2);
            assertThat(circuit).extracting(SignatureDto::getSignataireId).containsExactly("chef-equipe", "directeur-qualite");
            assertThat(circuit).extracting(SignatureDto::getFonctionSignataire)
                    .containsExactly("Responsable d'équipe", "Directeur qualité");
            assertThat(circuit).allSatisfy(s -> assertThat(s.getStatut()).isEqualTo(StatutSignature.EN_ATTENTE));
            assertThat(audit.getReferenceRapport()).isEqualTo("RAP-2026-001");
            verify(depot).deleteAllByAuditId(audit.getId());
        }

        @Test
        @DisplayName("Remplacer un signataire en attente garde son visa, marqué remplacé, et les signatures données")
        void remplacer_un_signataire_garde_l_historique() {
            Signature signe = visa("chef-equipe", StatutSignature.SIGNEE, 1);
            signe.setId(UUID.randomUUID());
            Signature absent = visa("super-admin", StatutSignature.EN_ATTENTE, 2);
            absent.setId(UUID.randomUUID());
            absent.setRole(com.qualiapproche.evaluation.enumeration.RoleVisa.APPROUVE);
            when(depot.findAllByAuditIdOrderByOrdreAsc(audit.getId())).thenReturn(List.of(signe, absent));
            org.mockito.ArgumentCaptor<Signature> enregistre = org.mockito.ArgumentCaptor.forClass(Signature.class);

            service.remplacer(audit.getId(), absent.getId(),
                    SignatureDto.builder().signataireId("directeur-qualite").fonctionSignataire("Directeur qualité").build());

            verify(depot, org.mockito.Mockito.times(2)).save(enregistre.capture());
            assertThat(absent.getStatut()).isEqualTo(StatutSignature.REMPLACEE);
            assertThat(absent.getRemplaceLe()).isNotNull();
            Signature nouveau = enregistre.getAllValues().get(1);
            assertThat(nouveau.getSignataireId()).isEqualTo("directeur-qualite");
            assertThat(nouveau.getStatut()).isEqualTo(StatutSignature.EN_ATTENTE);
            assertThat(nouveau.getOrdre()).isEqualTo(2);
            assertThat(nouveau.getRole()).isEqualTo(com.qualiapproche.evaluation.enumeration.RoleVisa.APPROUVE);
            assertThat(signe.getStatut()).isEqualTo(StatutSignature.SIGNEE);
            verify(depot, never()).deleteAllByAuditId(any());
        }

        @Test
        @DisplayName("Un visa déjà donné ne se remplace pas")
        void un_visa_donne_ne_se_remplace_pas() {
            Signature signe = visa("chef-equipe", StatutSignature.SIGNEE, 1);
            signe.setId(UUID.randomUUID());
            when(depot.findAllByAuditIdOrderByOrdreAsc(audit.getId())).thenReturn(List.of(signe));

            assertThatThrownBy(() -> service.remplacer(audit.getId(), signe.getId(),
                    SignatureDto.builder().signataireId("dq").build())).isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Une même personne ne figure pas deux fois au circuit")
        void pas_de_doublon_dans_le_circuit() {
            assertThatThrownBy(() -> service.poserLeCircuit(audit.getId(), List.of(
                    SignatureDto.builder().signataireId("dq").build(),
                    SignatureDto.builder().signataireId(" dq ").build())))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("deux fois");
        }

        @Test
        @DisplayName("Seul un visa en attente se relance")
        void seul_un_visa_en_attente_se_relance() {
            Signature attendu = visa("dq", StatutSignature.EN_ATTENTE, 1);
            attendu.setId(UUID.randomUUID());
            Signature signe = visa("chef", StatutSignature.SIGNEE, 2);
            signe.setId(UUID.randomUUID());
            when(depot.findAllByAuditIdOrderByOrdreAsc(audit.getId())).thenReturn(List.of(attendu, signe));

            service.relancer(audit.getId(), attendu.getId());

            verify(relance).relancer(org.mockito.ArgumentMatchers.eq(attendu), any());
            assertThatThrownBy(() -> service.relancer(audit.getId(), signe.getId())).isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Reposer le circuit ne change pas la référence déjà attribuée au rapport")
        void reposer_le_circuit_garde_la_reference() {
            audit.setReferenceRapport("RAP-2026-001");

            service.poserLeCircuit(audit.getId(), List.of(SignatureDto.builder().signataireId("dq").build()));

            assertThat(audit.getReferenceRapport()).isEqualTo("RAP-2026-001");
            verify(codification, never()).genererReference(any());
        }

        @Test
        @DisplayName("Un circuit vide est refusé")
        void un_circuit_vide_est_refuse() {
            assertThatThrownBy(() -> service.poserLeCircuit(audit.getId(), List.of()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("au moins un signataire");
            verify(depot, never()).deleteAllByAuditId(any());
        }

        @Test
        @DisplayName("Un type d'audit à circuit externe ne reçoit pas de circuit interne")
        void un_circuit_externe_ne_recoit_pas_de_circuit() {
            audit.setType(TypeAudit.builder().libelle("Certification").circuitExterne(true).build());

            assertThatThrownBy(() -> service.poserLeCircuit(audit.getId(),
                    List.of(SignatureDto.builder().signataireId("dq").build())))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("hors de l'application");
        }

        @Test
        @DisplayName("Le circuit ne se pose que sur un audit en cours")
        void le_circuit_ne_se_pose_que_sur_un_audit_en_cours() {
            audit.setStatut(StatutAudit.CLOTURE);

            assertThatThrownBy(() -> service.poserLeCircuit(audit.getId(),
                    List.of(SignatureDto.builder().signataireId("dq").build())))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("audit en cours");
            verify(depot, never()).deleteAllByAuditId(any());
        }

        @Test
        @DisplayName("Seul le signataire désigné vise : un tiers est refusé")
        void un_tiers_ne_vise_pas() {
            when(depot.findAllByAuditIdOrderByOrdreAsc(audit.getId()))
                    .thenReturn(List.of(visa("chef-equipe", StatutSignature.EN_ATTENTE, 1)));

            assertThatThrownBy(() -> service.signer(audit.getId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("Le signataire désigné vise sans attendre les précédents, et le visa est daté")
        void le_signataire_vise_dans_l_ordre_qu_il_veut() {
            Signature premier = visa("chef-equipe", StatutSignature.EN_ATTENTE, 1);
            Signature second = visa("directeur-qualite", StatutSignature.EN_ATTENTE, 2);
            when(depot.findAllByAuditIdOrderByOrdreAsc(audit.getId())).thenReturn(List.of(premier, second));

            SignatureDto vise = service.signer(audit.getId());

            assertThat(vise.getSignataireId()).isEqualTo("directeur-qualite");
            assertThat(vise.getStatut()).isEqualTo(StatutSignature.SIGNEE);
            assertThat(vise.getDateSignature()).isNotNull();
            assertThat(premier.getStatut()).isEqualTo(StatutSignature.EN_ATTENTE);
        }

        @Test
        @DisplayName("Un visa déjà donné ne se redonne pas")
        void un_visa_deja_donne_ne_se_redonne_pas() {
            when(depot.findAllByAuditIdOrderByOrdreAsc(audit.getId()))
                    .thenReturn(List.of(visa("directeur-qualite", StatutSignature.SIGNEE, 1)));

            assertThatThrownBy(() -> service.signer(audit.getId())).isInstanceOf(BusinessException.class)
                    .hasMessageContaining("aucun visa en attente");
        }

        @Test
        @DisplayName("Un refus arrête le circuit : plus personne ne vise")
        void un_refus_arrete_le_circuit() {
            when(depot.findAllByAuditIdOrderByOrdreAsc(audit.getId())).thenReturn(List.of(
                    visa("chef-equipe", StatutSignature.REFUSEE, 1),
                    visa("directeur-qualite", StatutSignature.EN_ATTENTE, 2)));

            assertThatThrownBy(() -> service.signer(audit.getId()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("arrêté par un refus");
            verify(depot, never()).save(any());
        }

        @Test
        @DisplayName("Un refus se motive, et le motif reste au circuit")
        void un_refus_se_motive() {
            Signature mien = visa("directeur-qualite", StatutSignature.EN_ATTENTE, 1);
            when(depot.findAllByAuditIdOrderByOrdreAsc(audit.getId())).thenReturn(List.of(mien));

            assertThatThrownBy(() -> service.refuser(audit.getId(), "  ")).isInstanceOf(BusinessException.class)
                    .hasMessageContaining("se motive");
            assertThat(mien.getStatut()).isEqualTo(StatutSignature.EN_ATTENTE);

            SignatureDto refuse = service.refuser(audit.getId(), " Conclusions incomplètes ");
            assertThat(refuse.getStatut()).isEqualTo(StatutSignature.REFUSEE);
            assertThat(refuse.getMotifRefus()).isEqualTo("Conclusions incomplètes");
        }

        @Test
        @DisplayName("On ne vise pas le rapport d'un audit qui n'est plus en cours")
        void pas_de_visa_hors_audit_en_cours() {
            audit.setStatut(StatutAudit.CLOTURE);

            assertThatThrownBy(() -> service.signer(audit.getId()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("n'est pas en cours");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class VivierDAuditeurs {

        @Mock private AuditeurRepository depot;
        @Mock private AuditRepository audits;
        @Mock private EvaluationAuditeurRepository evaluations;

        @InjectMocks private AuditeurService service;

        private Auditeur auditeur;

        @BeforeEach
        void preparer() {
            connecter("auditeur-1");
            auditeur = Auditeur.builder().utilisateurId("auditeur-1").statut(StatutAuditeur.ACTIF).build();
            auditeur.setId(UUID.randomUUID());
            auditeur.setDirectionId(DIRECTION);
            when(depot.findById(auditeur.getId())).thenReturn(Optional.of(auditeur));
            when(depot.save(any(Auditeur.class))).thenAnswer(i -> i.getArgument(0));
        }

        private static Audit auditDu(UUID id) {
            Audit audit = Audit.builder().reference("AUD").statut(StatutAudit.EN_PREPARATION).build();
            audit.setId(id);
            return audit;
        }

        @Test
        @DisplayName("Une même personne ne s'inscrit pas deux fois au vivier de sa direction")
        void pas_de_double_inscription() {
            when(depot.existsByDirectionIdAndUtilisateurId(DIRECTION, "auditeur-2")).thenReturn(true);

            assertThatThrownBy(() -> service.creer(AuditeurDto.builder().utilisateurId(" auditeur-2 ").build()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("déjà inscrite");
            verify(depot, never()).save(any());
        }

        @Test
        @DisplayName("Un auditeur inscrit sans statut part actif, ses listes nettoyées")
        void un_auditeur_inscrit_part_actif() {
            AuditeurDto inscrit = service.creer(AuditeurDto.builder().utilisateurId("auditeur-2")
                    .certifications(List.of(" ISO 19011 ", "ISO 19011", " ")).build());

            assertThat(inscrit.getStatut()).isEqualTo(StatutAuditeur.ACTIF);
            assertThat(inscrit.getCertifications()).containsExactly("ISO 19011");
        }

        @Test
        @DisplayName("Retirer un auditeur le désactive sans l'effacer : ses constats le citent")
        void retirer_un_auditeur_le_desactive() {
            service.desactiver(auditeur.getId());

            assertThat(auditeur.getStatut()).isEqualTo(StatutAuditeur.INACTIF);
            verify(depot, never()).delete(any(Auditeur.class));
            verify(depot, never()).deleteById(any());
        }

        @Test
        @DisplayName("Un auditeur inactif n'est jamais disponible")
        void un_inactif_n_est_pas_disponible() {
            auditeur.setStatut(StatutAuditeur.INACTIF);

            assertThat(service.estDisponible(auditeur.getId(), null, null, null)).isFalse();
        }

        @Test
        @DisplayName("L'audit en cours de composition ne se compte pas comme un conflit avec lui-même")
        void l_audit_compose_ne_se_compte_pas() {
            UUID compose = UUID.randomUUID();
            LocalDate debut = LocalDate.of(2026, 11, 2);
            LocalDate fin = LocalDate.of(2026, 11, 4);
            when(audits.chevauchant(auditeur.getId(), debut, fin)).thenReturn(List.of(auditDu(compose)));

            assertThat(service.estDisponible(auditeur.getId(), debut, fin, compose)).isTrue();
            assertThat(service.estDisponible(auditeur.getId(), debut, fin, UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("Sans période, un auditeur actif est disponible")
        void sans_periode_un_actif_est_disponible() {
            assertThat(service.estDisponible(auditeur.getId(), null, LocalDate.now(), null)).isTrue();
            verify(audits, never()).chevauchant(any(), any(), any());
        }

        @Test
        @DisplayName("Qui n'est pas au vivier de sa direction ne passe pas pour un auditeur")
        void hors_vivier_pas_d_auditeur_courant() {
            when(depot.findByDirectionIdAndUtilisateurId(DIRECTION, "auditeur-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.auditeurCourant())
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("L'auditeur d'une autre direction est introuvable")
        void l_auditeur_d_une_autre_direction_est_introuvable() {
            auditeur.setDirectionId(UUID.randomUUID());

            assertThatThrownBy(() -> service.lire(auditeur.getId()))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("Aucun(e) auditeur");
        }
    }
}
