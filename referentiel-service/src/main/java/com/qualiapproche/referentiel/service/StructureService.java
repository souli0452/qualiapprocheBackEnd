
package com.qualiapproche.referentiel.service;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.enumeration.TypeStructure;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StructureService {
    StructureDto saveStructure(StructureDto structureDto);

    StructureDto getStructureById(UUID directionId);
    String getStructureNameById(UUID directionId);

    /**
     * Page des structures, restreinte aux critères renseignés.
     *
     * @param recherche terme libre sur le libellé long ou court, facultatif
     */
    Page<StructureDto> getAllStructures(TypeStructure typeStructure, UUID directionId,
                                        String recherche, Pageable pageable);
    Page<StructureDto> getAllStructuresAll(Pageable pageable);

    StructureDto updateStructure(StructureDto direction) throws Exception;

    void deleteStructure(UUID id);

    StructureDto findStructureByLibelle(String libelle);
    StructureDto getDirection();

    java.util.Map<String, Object> getStructureLicenseStatus(java.util.UUID structureId);
}
