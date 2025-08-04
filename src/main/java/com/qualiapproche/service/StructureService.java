
package com.qualiapproche.service;
import com.qualiapproche.dto.StructureDto;
import com.qualiapproche.enumeration.TypeStructure;

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
}
