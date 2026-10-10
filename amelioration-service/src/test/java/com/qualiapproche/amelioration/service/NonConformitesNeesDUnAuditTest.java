package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.entities.NiveauNonConformite;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.TypeNonConformite;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteMapper;
import com.qualiapproche.amelioration.repository.NiveauNonConformiteRepository;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.TypeNonConformiteRepository;
import com.qualiapproche.amelioration.service.impl.NonConformitesDAuditService;
import com.qualiapproche.common.dto.NonConformiteDAuditDto;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.exception.BusinessException;
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

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le dossier qu'ouvre un écart d'audit publié (M13 vers M05).
 *
 * <p>Il appartient à la structure auditée, non à celle de l'auditeur : c'est son pilote qui en
 * analyse les causes. Et un écart ne doit jamais se perdre — ni parce que le référentiel n'a pas
 * répondu, ni parce qu'il a été transmis deux fois. Ces tests fixent ces deux exigences.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonConformitesNeesDUnAuditTest {

    private static final UUID STRUCTURE_AUDITEE = UUID.randomUUID();

    @Mock private NonConformiteRepository nonConformites;
    @Mock private NonConformiteService service;
    @Mock private NonConformiteMapper mapper;
    @Mock private TypeNonConformiteRepository types;
    @Mock private NiveauNonConformiteRepository niveaux;
    @Mock private ReferentielClient referentiel;

    @InjectMocks private NonConformitesDAuditService audits;

    private final ArgumentCaptor<NonConformiteDto> declare = ArgumentCaptor.forClass(NonConformiteDto.class);

    private static NonConformiteDAuditDto ecart() {
        return NonConformiteDAuditDto.builder()
                .cleOrigine("constat:" + UUID.randomUUID())
                .structureConcerneeId(" " + STRUCTURE_AUDITEE + " ")
                .justification("  Registre des réclamations non tenu  ")
                .origineLibelle("Audit interne")
                .nomProcessus("Achats")
                .build();
    }

    @BeforeEach
    void preparer() throws IOException {
        when(service.createNonConformite(any(NonConformiteDto.class), anyString(), anyString()))
                .thenAnswer(i -> {
                    NonConformiteDto creee = i.getArgument(0);
                    creee.setNumeroReference("NC-ACH-2026-001");
                    return creee;
                });
    }

    @Test
    @DisplayName("Le dossier s'ouvre dans la structure auditée, avec sa famille de processus")
    void le_dossier_s_ouvre_dans_la_structure_auditee() throws IOException {
        StructureDto structure = new StructureDto();
        structure.setLibelleLong("Direction des achats");
        structure.setTypeProcessusId(UUID.randomUUID());
        structure.setTypeProcessusLibelle("Support");
        when(referentiel.getStructureById(STRUCTURE_AUDITEE)).thenReturn(structure);
        NonConformiteDAuditDto ecart = ecart();

        audits.ouvrir(ecart);

        verify(service).createNonConformite(declare.capture(), eq(STRUCTURE_AUDITEE.toString()), eq(ecart.getCleOrigine()));
        NonConformiteDto dto = declare.getValue();
        assertThat(dto.getStructureSoumissionId()).isEqualTo(STRUCTURE_AUDITEE.toString());
        assertThat(dto.getStructureSoumissionLibelle()).isEqualTo("Direction des achats");
        assertThat(dto.getTypeProcessusId()).isEqualTo(structure.getTypeProcessusId());
        assertThat(dto.getJustification()).isEqualTo("Registre des réclamations non tenu");
        assertThat(dto.getOriginNonConformiteLibelle()).isEqualTo("Audit interne");
        assertThat(dto.getNomProcessus()).isEqualTo("Achats");
    }

    @Test
    @DisplayName("Le référentiel muet n'empêche pas la déclaration : l'écart ne se perd pas")
    void le_referentiel_muet_n_empeche_pas_la_declaration() throws IOException {
        when(referentiel.getStructureById(STRUCTURE_AUDITEE)).thenThrow(new RuntimeException("délai dépassé"));

        NonConformiteDto creee = audits.ouvrir(ecart());

        assertThat(creee.getNumeroReference()).isEqualTo("NC-ACH-2026-001");
        verify(service).createNonConformite(declare.capture(), anyString(), anyString());
        // La numérotation se sert de ce libellé comme sigle de repli : il ne doit jamais être nul.
        assertThat(declare.getValue().getStructureSoumissionLibelle()).isEqualTo("AUDIT");
    }

    @Test
    @DisplayName("Une structure auditée qui n'est pas un identifiant est refusée en 400")
    void une_structure_illisible_est_refusee() throws IOException {
        NonConformiteDAuditDto ecart = ecart();
        ecart.setStructureConcerneeId("Direction des achats");

        assertThatThrownBy(() -> audits.ouvrir(ecart))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(service, never()).createNonConformite(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Type et niveau connus sont recopiés avec leur libellé ; inconnus, ils restent vides")
    void type_et_niveau_recopies_s_ils_existent() {
        TypeNonConformite type = new TypeNonConformite();
        type.setId(UUID.randomUUID());
        type.setLibelle("Écart de procédure");
        when(types.findById(type.getId())).thenReturn(Optional.of(type));
        UUID niveauInconnu = UUID.randomUUID();
        when(niveaux.findById(niveauInconnu)).thenReturn(Optional.empty());
        NonConformiteDAuditDto ecart = ecart();
        ecart.setTypeNonConformiteId(type.getId());
        ecart.setNiveauNonConformiteId(niveauInconnu);

        NonConformiteDto creee = audits.ouvrir(ecart);

        assertThat(creee.getTypeNonConformiteId()).isEqualTo(type.getId());
        assertThat(creee.getTypeNonConformiteLibelle()).isEqualTo("Écart de procédure");
        assertThat(creee.getNiveauNonConformiteId()).isNull();
    }

    @Test
    @DisplayName("Un niveau connu est recopié avec son libellé")
    void un_niveau_connu_est_recopie() {
        NiveauNonConformite niveau = new NiveauNonConformite();
        niveau.setId(UUID.randomUUID());
        niveau.setLibelle("Majeure");
        when(niveaux.findById(niveau.getId())).thenReturn(Optional.of(niveau));
        NonConformiteDAuditDto ecart = ecart();
        ecart.setNiveauNonConformiteId(niveau.getId());

        assertThat(audits.ouvrir(ecart).getNiveauNonConformiteLibelle()).isEqualTo("Majeure");
    }

    @Test
    @DisplayName("Un échec d'enregistrement remonte en erreur serveur, sans dossier à moitié ouvert")
    void un_echec_d_enregistrement_remonte_en_500() throws IOException {
        when(service.createNonConformite(any(NonConformiteDto.class), anyString(), anyString()))
                .thenThrow(new IOException("stockage indisponible"));

        assertThatThrownBy(() -> audits.ouvrir(ecart()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    @DisplayName("Un écart déjà transmis rend le dossier existant : la transmission rejouée n'en crée pas un second")
    void un_ecart_deja_transmis_rend_le_dossier_existant() {
        NonConformite existante = new NonConformite();
        existante.setId(UUID.randomUUID());
        NonConformiteDto dto = new NonConformiteDto();
        dto.setId(existante.getId());
        when(nonConformites.findByCleOrigine("constat:42")).thenReturn(Optional.of(existante));
        when(mapper.toDto(existante)).thenReturn(dto);

        assertThat(audits.dejaOuverte("constat:42")).map(NonConformiteDto::getId).contains(existante.getId());
        assertThat(audits.dejaOuverte("constat:43")).isEmpty();
    }
}
