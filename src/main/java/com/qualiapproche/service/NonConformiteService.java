package com.qualiapproche.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.qualiapproche.dto.NcStats;
import com.qualiapproche.dto.NonConformiteByStructDto;
import com.qualiapproche.dto.NonConformiteDto;
import com.qualiapproche.dto.RejectNonConformiteDto;
import com.qualiapproche.enumeration.Etat;
import com.qualiapproche.enumeration.Status;
import org.springframework.transaction.annotation.Transactional;

public interface NonConformiteService {
    NonConformiteDto createNonConformite(NonConformiteDto dto) throws IOException;
    void deleteMultiple(List<NonConformiteDto> nonConformiteDtos) ;
    List <NcStats> getNcStats(String structureSoumissionId);
    void changeStatus(UUID id, Status statut);
    NonConformiteDto updateNonConformite(UUID id, NonConformiteDto dto) throws IOException;
    List<NonConformiteDto> updateNonConformites(List<NonConformiteDto> dtos) throws IOException;
   // NonConformiteDto create(NonConformiteDto nonConformiteDto);
    NonConformiteDto update(NonConformiteDto nonConformiteDto);
    List<NonConformiteDto> allNonConformites();
    List<NonConformiteDto> findImupted(String userId,Etat etat);
    // Méthode pour récupérer les non-conformités par état
    List<NonConformiteDto> getNonConformitesByEtatNonConformite(Etat etat);
    List<NonConformiteDto> getNonConformitesByEtatAnStructure(Etat etat, String uuid);
    List<NonConformiteDto> getNonConformitesByEtatAndStructureOrigine(Etat etat, String uuid);
    void changeManyStatus(List<NonConformiteDto> nonConformiteDtos, Status status);
    NonConformiteDto getNonConformiteById(UUID id);
    List<NonConformiteDto> findAll( Status status,String structureSoumissionId);
    void delete(UUID id);
    NonConformiteDto rejectNonConformite(RejectNonConformiteDto rejectNonConformiteDto) throws IOException;
    Map<String, Long>  getNonConformiteStatsByStructure(int anne);
    Map<String, Map<String, Long>> getStatsParAnnee(int annee);
    Map<String, Map<String, Map<String, Long>>> getStatsDetailleesParAnnee(int annee);
    NonConformiteDto getByNumeroRef(String numeroRef);
    Map<String, Map<String, Long>>getStatsMensuellesParService(int annee, String origineServiceId);
    Map<String, Map<String, Map<String, Long>>> getStatsDetailleesServiceParAnnee(int annee, String origineServiceId);

}