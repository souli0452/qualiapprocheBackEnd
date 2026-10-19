package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.dto.NonConformiteDAuditDto;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.evaluation.client.NonConformitesClient;
import com.qualiapproche.evaluation.dto.TransmissionDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.entities.EvaluationRQAPBF;
import com.qualiapproche.evaluation.entities.NoeudReferentiel;
import com.qualiapproche.evaluation.entities.NotationCritere;
import com.qualiapproche.evaluation.entities.TypeAudit;
import com.qualiapproche.evaluation.entities.TypeConstat;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.enumeration.StatutNotation;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.NotationCritereRepository;
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
import org.springframework.transaction.PlatformTransactionManager;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ce qui part vers le module Non-conformités, vers où, et ce qui reste quand rien ne part. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransmissionVersM05Test {

    @Mock private NonConformitesClient client;
    @Mock private ConstatRepository constats;
    @Mock private NotationCritereRepository notations;
    @Mock private PlatformTransactionManager transactions;
    @Mock private RegleEcartRQAPBFService regles;

    @InjectMocks private TransmissionVersM05 transmission;

    private final UUID processus = UUID.randomUUID();
    private final UUID origineM05 = UUID.randomUUID();
    private final UUID graviteM05 = UUID.randomUUID();
    private final UUID dossier = UUID.randomUUID();

    @BeforeEach
    void preparer() {
        NonConformiteDto ouvert = new NonConformiteDto();
        ouvert.setId(dossier);
        ouvert.setNumeroReference("NFQT-DOC-2026-00001");
        when(client.transmettre(any())).thenReturn(ouvert);
        when(regles.niveauxEnEcart(any())).thenReturn(EnumSet.of(NiveauNotationRQAPBF.NON_CONFORME,
                NiveauNotationRQAPBF.A_AMELIORER));
    }

    private Audit audit(UUID processusAudite) {
        return Audit.builder().reference("AUD-2026-012").statut(StatutAudit.EN_COURS).processusId(processusAudite)
                .type(TypeAudit.builder().libelle("Audit interne").typeNonConformiteId(origineM05).build()).build();
    }

    private Constat constatPublie(Audit audit) {
        Constat c = Constat.builder().audit(audit).statut(StatutConstat.PUBLIE)
                .observation("Registres non complétés le jour même.")
                .nature(TypeConstat.builder().libelle("Non-conformité mineure").genereNonConformite(true)
                        .niveauNonConformiteId(graviteM05).build())
                .build();
        c.setId(UUID.randomUUID());
        when(constats.findById(c.getId())).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    @DisplayName("Un constat publié ouvre un dossier dans la structure auditée et le soumet")
    void un_constat_publie_ouvre_un_dossier_dans_la_structure_auditee_et_le_soumet() {
        Constat c = constatPublie(audit(processus));

        TransmissionDto resultat = transmission.transmettre(EcartsPublies.deConstats(List.of(c.getId())));

        ArgumentCaptor<NonConformiteDAuditDto> envoye = ArgumentCaptor.forClass(NonConformiteDAuditDto.class);
        verify(client).transmettre(envoye.capture());
        assertThat(envoye.getValue().getCleOrigine()).isEqualTo("AUDIT-CONSTAT:" + c.getId());
        assertThat(envoye.getValue().getStructureConcerneeId()).isEqualTo(processus.toString());
        assertThat(envoye.getValue().getTypeNonConformiteId()).isEqualTo(origineM05);
        assertThat(envoye.getValue().getNiveauNonConformiteId()).isEqualTo(graviteM05);
        assertThat(envoye.getValue().isSoumettre()).isTrue();
        assertThat(envoye.getValue().getJustification()).contains("AUD-2026-012", "Registres non complétés");
        assertThat(resultat.getTransmis()).isEqualTo(1);
        assertThat(c.getNonConformiteId()).isEqualTo(dossier);
    }

    @Test
    @DisplayName("Sans processus audité rien ne part et l'écart reste à transmettre")
    void sans_processus_audite_rien_ne_part_et_l_ecart_reste_a_transmettre() {
        Constat c = constatPublie(audit(null));

        TransmissionDto resultat = transmission.transmettre(EcartsPublies.deConstats(List.of(c.getId())));

        verify(client, never()).transmettre(any());
        assertThat(resultat.getEchecs()).singleElement().asString().contains("processus audité");
        assertThat(c.getNonConformiteId()).isNull();
    }

    @Test
    @DisplayName("Un refus de M05 ne lève rien et laisse l'écart à reprendre")
    void un_refus_de_m05_ne_leve_rien_et_laisse_l_ecart_a_reprendre() {
        Constat c = constatPublie(audit(processus));
        when(client.transmettre(any())).thenThrow(new IllegalStateException("503"));

        TransmissionDto resultat = transmission.transmettre(EcartsPublies.deConstats(List.of(c.getId())));

        assertThat(resultat.getTransmis()).isZero();
        assertThat(resultat.getEchecs()).hasSize(1);
        assertThat(c.getNonConformiteId()).isNull();
    }

    @Test
    @DisplayName("Un écart déjà transmis ne repart pas")
    void un_ecart_deja_transmis_ne_repart_pas() {
        Constat c = constatPublie(audit(processus));
        c.setNonConformiteId(UUID.randomUUID());

        transmission.transmettre(EcartsPublies.deConstats(List.of(c.getId())));

        verify(client, never()).transmettre(any());
    }

    private NotationCritere notationPubliee(NiveauNotationRQAPBF niveau) {
        EvaluationRQAPBF evaluation = EvaluationRQAPBF.builder().audit(audit(processus)).build();
        NoeudReferentiel critere = NoeudReferentiel.builder().code("E1.1-C1").libelle("Texte d'organisation").build();
        NotationCritere n = NotationCritere.builder().evaluation(evaluation).noeud(critere)
                .niveau(niveau).statut(StatutNotation.PUBLIE).build();
        n.setId(UUID.randomUUID());
        when(notations.findById(n.getId())).thenReturn(Optional.of(n));
        return n;
    }

    @Test
    @DisplayName("Par défaut, « À améliorer » est un écart ; « Acceptable » et « Conforme » non")
    void par_defaut_a_ameliorer_part_mais_ni_acceptable_ni_conforme() {
        NotationCritere aAmeliorer = notationPubliee(NiveauNotationRQAPBF.A_AMELIORER);
        NotationCritere acceptable = notationPubliee(NiveauNotationRQAPBF.ACCEPTABLE);
        NotationCritere conforme = notationPubliee(NiveauNotationRQAPBF.CONFORME);

        TransmissionDto resultat = transmission.transmettre(
                EcartsPublies.deNotations(List.of(aAmeliorer.getId(), acceptable.getId(), conforme.getId())));

        ArgumentCaptor<NonConformiteDAuditDto> envoye = ArgumentCaptor.forClass(NonConformiteDAuditDto.class);
        verify(client).transmettre(envoye.capture());
        assertThat(envoye.getValue().getCleOrigine()).isEqualTo("AUDIT-RQAPBF:" + aAmeliorer.getId());
        assertThat(envoye.getValue().getJustification()).contains("E1.1-C1", "À améliorer");
        assertThat(resultat.getTransmis()).isEqualTo(1);
    }

    @Test
    @DisplayName("Une direction qui tient « Acceptable » pour un écart le transmet")
    void une_direction_qui_tient_acceptable_pour_un_ecart_le_transmet() {
        when(regles.niveauxEnEcart(any())).thenReturn(EnumSet.of(NiveauNotationRQAPBF.NON_CONFORME,
                NiveauNotationRQAPBF.A_AMELIORER, NiveauNotationRQAPBF.ACCEPTABLE));
        NotationCritere acceptable = notationPubliee(NiveauNotationRQAPBF.ACCEPTABLE);

        TransmissionDto resultat = transmission.transmettre(EcartsPublies.deNotations(List.of(acceptable.getId())));

        assertThat(resultat.getTransmis()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un passage transmet un lot ; le reste est compté et attend la relance")
    void un_passage_transmet_un_lot_et_compte_le_reste() {
        List<UUID> ids = new java.util.ArrayList<>();
        for (int i = 0; i < TransmissionVersM05.LOT + 5; i++) {
            ids.add(notationPubliee(NiveauNotationRQAPBF.NON_CONFORME).getId());
        }

        TransmissionDto resultat = transmission.transmettre(EcartsPublies.deNotations(ids));

        verify(client, times(TransmissionVersM05.LOT)).transmettre(any());
        assertThat(resultat.getTransmis()).isEqualTo(TransmissionVersM05.LOT);
        assertThat(resultat.getRestants()).isEqualTo(5);
    }
}
