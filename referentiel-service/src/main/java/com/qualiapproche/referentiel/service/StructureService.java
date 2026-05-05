
package com.qualiapproche.referentiel.service;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.enumeration.TypeStructure;

import java.util.List;
import java.util.UUID;

public interface StructureService {
    StructureDto saveStructure(StructureDto structureDto);

    StructureDto getStructureById(UUID directionId);
    String getStructureNameById(UUID directionId);

    List<StructureDto> getAllStructures(TypeStructure typeStructure, UUID directionId);
    List<StructureDto> getAllStructuresAll();

    StructureDto updateStructure(StructureDto direction) throws Exception;

    void deleteStructure(UUID id) ;

    StructureDto findStructureByLibelle(String libelle);
    StructureDto getDirection();

    java.util.Map<String, Object> getStructureLicenseStatus(java.util.UUID structureId);
}
