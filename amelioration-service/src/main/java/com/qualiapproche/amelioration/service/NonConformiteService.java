package com.qualiapproche.amelioration.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

import com.qualiapproche.common.dto.*;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.enumeration.TypeDemande;
import com.qualiapproche.common.enumeration.Circuit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface NonConformiteService {
    NonConformiteDto createNonConformite(NonConformiteDto dto) throws IOException;

    void deleteMultiple(List<NonConformiteDto> nonConformiteDtos);

    List<NcStats> getNcStats(String structureSoumissionId);

    NonConformiteDto updateNonConformite(UUID id, NonConformiteDto dto) throws IOException;

    List<NonConformiteDto> updateNonConformites(List<NonConformiteDto> dtos) throws IOException;

    // NonConformiteDto create(NonConformiteDto nonConformiteDto);
    NonConformiteDto update(NonConformiteDto nonConformiteDto);

    Page<NonConformiteDto> allNonConformites(Pageable pageable);

    Page<NonConformiteDto> findImupted(String userId, Etat etat, Pageable pageable);

    // Méthode pour récupérer les non-conformités par état
    Page<NonConformiteDto> getNonConformitesByEtatNonConformite(Etat etat, Pageable pageable);

    Page<NonConformiteDto> getNonConformitesByEtatAnStructure(Etat etat, String uuid, Pageable pageable);

    Page<NonConformiteDto> getNonConformitesByStructure(String uuid, Pageable pageable);

    Page<NonConformiteDto> getNonConformitesByEtatAndStructureOrigine(Etat etat, String uuid, Pageable pageable);

    NonConformiteDto getNonConformiteById(UUID id);

    Page<NonConformiteDto> findAll(Status status, String structureSoumissionId, Pageable pageable);

    Page<NonConformiteDto> findAllByStructure(String structureSoumissionId, Pageable pageable);

    void delete(UUID id);

    Map<String, Long> getNonConformiteStatsByStructure(int anne);

    Map<String, Map<String, Long>> getStatsParAnnee(int annee);

    Map<String, Map<String, Map<String, Long>>> getStatsDetailleesParAnnee(int annee);

    NonConformiteDto getByNumeroRef(String numeroRef);

    Map<String, Map<String, Long>> getStatsMensuellesParService(int annee, String origineServiceId);

    Map<String, Map<String, Map<String, Long>>> getStatsDetailleesServiceParAnnee(int annee, String origineServiceId);

    ValidatePlanActionDto validatePlan(ValidatePlanActionDto validatePlanActionDto);

    Map<String, Map<String, Map<String, Long>>> getStatsNiveauParAnnee(int annee, String origineServiceId);

    Page<NonConformiteDto> findAllByInitiator(String userId, Pageable pageable);

    Page<NonConformiteDto> findByUser(String userId, Pageable pageable);
    Page<NonConformiteDto> findImputedByUser(String userId, Pageable pageable);
    Page<NonConformiteDto> findArchivedByUser(String userId, Pageable pageable);
    NcCountsDto getCountsByUser(String userId);

    Page<NonConformiteDto> findByStructure(String structureId, Pageable pageable);
    Page<NonConformiteDto> findByStructureAllUsers(String structureId, Pageable pageable);

    NcDashboardDto getDashboardRQ();
    NcDashboardDto getDashboardPilot(String structureId);
    NcDashboardDto getDashboardUser(String userId);

    NcEvolutionDto getNcEvolutionStats(int annee, Integer mois, String structureId);

    Page<NonConformiteDto> getNonConformitesByNiveau(UUID niveauId, Pageable pageable);

    Page<NonConformiteDto> search(
            String numeroReference, String nomProcessus, String origineId, String origineService,
            String structureSoumissionId, String structureResponsableId,
            Etat etatTraitement, Status status, TypeDemande typeDemande, Circuit circuit,
            String userImputeEmail, String typeNonConformiteLibelle, String niveauNonConformiteLibelle,
            UUID typeNonConformiteId, UUID niveauNonConformiteId,
            LocalDateTime publicationDateFrom, LocalDateTime publicationDateTo,
            Pageable pageable);

    void updateWorkflowState(UUID nonConformiteId, String newStateName, String newEtatTraitement);
}