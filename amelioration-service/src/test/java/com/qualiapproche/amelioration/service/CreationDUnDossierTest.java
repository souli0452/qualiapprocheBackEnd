package com.qualiapproche.amelioration.service;

import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.ActionRepository;
import com.qualiapproche.amelioration.repository.EfficaciteRepository;
import com.qualiapproche.amelioration.repository.NiveauNonConformiteRepository;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.repository.TypeNonConformiteRepository;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.NonConformiteServiceImpl;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.service.SendMailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.numerotation.NumerotationDesNonConformites;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.qualiapproche.common.dto.NonConformiteDto;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * L'ordre des gestes à la création d'un dossier : la ligne d'abord (une contrainte violée se
 * révèle avant tout appel au dehors), les pièces ensuite, le circuit en dernier — et la clé d'un
 * écart d'audit écrite avec le dossier, non après.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreationDUnDossierTest {

    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PieceJointeRepository pieceJointeRepository;
    @Mock private NonConformiteMapper nonConformiteMapper;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private TypeNonConformiteRepository typeNonConformiteRepository;
    @Mock private ReferentielClient referentielClient;
    @Mock private EfficaciteRepository efficaciteRepository;
    @Mock private ActionRepository actionRepository;
    @Mock private NiveauNonConformiteRepository niveauNonConformiteRepository;
    @Mock private PieceJointeStockageService fichierService;
    @Mock private NonConformiteFichierService ncFichierService;
    @Mock private SendMailService sendMailService;
    @Mock private PlanActionRepository planActionRepository;
    @Mock private WorkflowClient workflowClient;
    @Mock private PlansActionDeLaNonConformiteService plansActionService;
    @Mock private PermissionChecker permissionChecker;
    @Mock private NumerotationDesNonConformites numerotation;
    @InjectMocks private NonConformiteServiceImpl service;

    @Test
    @DisplayName("Ligne écrite, puis pièces, puis circuit ; la clé d'audit est posée avant l'écriture")
    void ordre_des_gestes() throws Exception {
        NonConformite entite = new NonConformite();
        entite.setId(UUID.randomUUID());
        when(nonConformiteMapper.toEntity(any(NonConformiteDto.class))).thenReturn(entite);
        when(nonConformiteMapper.toDto(any(NonConformite.class))).thenReturn(new NonConformiteDto());
        when(numerotation.attribuer(any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn("NFQT-DQA-2026-00001");
        when(nonConformiteRepository.saveAndFlush(any())).thenAnswer(i -> {
            assertThat(((NonConformite) i.getArgument(0)).getCleOrigine()).isEqualTo("AUDIT-CONSTAT:x");
            return i.getArgument(0);
        });
        when(nonConformiteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        com.qualiapproche.common.dto.WorkflowSummaryDto actif = new com.qualiapproche.common.dto.WorkflowSummaryDto();
        actif.setId(UUID.randomUUID());
        when(workflowClient.getActiveWorkflowByType("NON_CONFORMITE")).thenReturn(actif);

        NonConformiteDto dto = new NonConformiteDto();
        dto.setStructureSoumissionId("s-1");
        dto.setStructureSoumissionLibelle("DQA");
        service.createNonConformite(dto, "s-1", "AUDIT-CONSTAT:x");

        InOrder ordre = inOrder(nonConformiteRepository, ncFichierService, workflowClient);
        ordre.verify(nonConformiteRepository).saveAndFlush(entite);
        ordre.verify(ncFichierService).synchroniser(any(), eq(entite.getId()));
        ordre.verify(workflowClient).initiateWorkflowDansLaStructure(eq(entite.getId()), eq("NON_CONFORMITE"),
                any(), any(), eq("s-1"));
    }

    @Test
    @DisplayName("Une écriture refusée (écart déjà transmis) n'atteint jamais le moteur")
    void contrainte_violee_avant_le_moteur() {
        NonConformite entite = new NonConformite();
        when(nonConformiteMapper.toEntity(any(NonConformiteDto.class))).thenReturn(entite);
        when(numerotation.attribuer(any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn("NFQT-DQA-2026-00001");
        when(nonConformiteRepository.saveAndFlush(any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_quali_nc_cle_origine"));

        assertThatThrownBy(() -> service.createNonConformite(new NonConformiteDto(), "s-1", "AUDIT-CONSTAT:x"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        verify(workflowClient, never()).getActiveWorkflowByType(anyString());
        verify(ncFichierService, never()).synchroniser(any(), any());
    }
}
