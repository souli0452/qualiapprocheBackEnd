package com.qualiapproche.amelioration.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.qualiapproche.common.dto.*;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import org.springframework.transaction.annotation.Transactional;

public interface NonConformiteService {
    NonConformiteDto createNonConformite(NonConformiteDto dto) throws IOException;

    void deleteMultiple(List<NonConformiteDto> nonConformiteDtos);

    List<NcStats> getNcStats(String structureSoumissionId);

    void changeStatus(UUID id, Status statut);

    NonConformiteDto updateNonConformite(UUID id, NonConformiteDto dto) throws IOException;

    List<NonConformiteDto> updateNonConformites(List<NonConformiteDto> dtos) throws IOException;

    // NonConformiteDto create(NonConformiteDto nonConformiteDto);
    NonConformiteDto update(NonConformiteDto nonConformiteDto);

    List<NonConformiteDto> allNonConformites();

    List<NonConformiteDto> findImupted(String userId, Etat etat);

    // Méthode pour récupérer les non-conformités par état
    List<NonConformiteDto> getNonConformitesByEtatNonConformite(Etat etat);

    List<NonConformiteDto> getNonConformitesByEtatAnStructure(Etat etat, String uuid);

    List<NonConformiteDto> getNonConformitesByStructure(String uuid);

    List<NonConformiteDto> getNonConformitesByEtatAndStructureOrigine(Etat etat, String uuid);

    void changeManyStatus(List<NonConformiteDto> nonConformiteDtos, Status status);

    NonConformiteDto getNonConformiteById(UUID id);

    List<NonConformiteDto> findAll(Status status, String structureSoumissionId);

    List<NonConformiteDto> findAllByStructure(String structureSoumissionId);

    void delete(UUID id);

    NonConformiteDto rejectNonConformite(RejectNonConformiteDto rejectNonConformiteDto) throws IOException;

    Map<String, Long> getNonConformiteStatsByStructure(int anne);

    Map<String, Map<String, Long>> getStatsParAnnee(int annee);

    Map<String, Map<String, Map<String, Long>>> getStatsDetailleesParAnnee(int annee);

    NonConformiteDto getByNumeroRef(String numeroRef);

    Map<String, Map<String, Long>> getStatsMensuellesParService(int annee, String origineServiceId);

    Map<String, Map<String, Map<String, Long>>> getStatsDetailleesServiceParAnnee(int annee, String origineServiceId);

    ValidatePlanActionDto validatePlan(ValidatePlanActionDto validatePlanActionDto);

    Map<String, Map<String, Map<String, Long>>> getStatsNiveauParAnnee(int annee, String origineServiceId);

    List<NonConformiteDto> findAllByInitiator(String userId);

    List<NonConformiteDto> findByUser(String userId);
    List<NonConformiteDto> findImputedByUser(String userId);
    List<NonConformiteDto> findArchivedByUser(String userId);
    NcCountsDto getCountsByUser(String userId);

    List<NonConformiteDto> findByStructure(String structureId);
    List<NonConformiteDto> findByStructureAllUsers(String structureId);

    NcDashboardDto getDashboardRQ();
    NcDashboardDto getDashboardPilot(String structureId);
    NcDashboardDto getDashboardUser(String userId);

    NcEvolutionDto getNcEvolutionStats(int annee, Integer mois, String structureId);

    List<NonConformiteDto> getNonConformitesByNiveau(UUID niveauId);
}