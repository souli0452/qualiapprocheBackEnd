
package com.qualiapproche.referentiel.controller;


import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.referentiel.service.StructureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import static com.qualiapproche.common.utils.ApiUrls.*;


@RestController
@RequestMapping(ROOT_STRUCTURE_API)
@Tag(name = "Structures", description = "Gestion des structures organisationnelles (Directions, Services, etc.)")
public record StructureController(StructureService structureService) {

    @Operation(summary = "Créer une structure", description = "Ajoute une nouvelle entité organisationnelle au référentiel")
    @PostMapping(CREATE_STRUCTURE)
    public ResponseEntity<StructureDto> saveStructure(@RequestBody StructureDto structureDto) {
        return ResponseEntity.ok(structureService.saveStructure(structureDto));
    }

    @PutMapping(UPDATE_STRUCTURE)
    public ResponseEntity<StructureDto> updateStructure(@RequestBody StructureDto structureDto) throws Exception {
        return ResponseEntity.ok(structureService.updateStructure(structureDto));
    }


    @GetMapping(STRUCTURE_BY_ID)
    public ResponseEntity<StructureDto> getStructureById(@PathVariable UUID id)  {
        return ResponseEntity.ok(structureService.getStructureById(id));
    }



    @GetMapping(STRUCTURENAME_BY_ID)
    public String getStructureNameById(@PathVariable("structureid") UUID structureid) {
        return structureService.getStructureNameById(structureid);
    }

    @GetMapping(GET_ALL_STRUCTURE)
    public ResponseEntity<Page<StructureDto>> getAllStructuresAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(structureService.getAllStructuresAll(pageable));
    }



    @GetMapping
    public ResponseEntity<Page<StructureDto>> getAllStructures(@RequestParam(required = false) TypeStructure typeStructure,
                                                               @RequestParam(required = false) UUID directionId,
                                                               @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(structureService.getAllStructures(typeStructure, directionId, pageable));
    }


    @DeleteMapping(DELETE_STRUCTURE)
    public ResponseEntity<Void> deleteStructure(@PathVariable UUID id)  {
        structureService.deleteStructure(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(STRUCTURE_BY_LIBELLE_URL)
    public ResponseEntity<StructureDto> getStructureByLibelle(@PathVariable String libelle)  {
        return ResponseEntity.ok(structureService.findStructureByLibelle(libelle));
    }

    @GetMapping("/direction")
    public ResponseEntity<StructureDto> getDirection() {
        return ResponseEntity.ok(structureService.getDirection());
    }

    @GetMapping("/{id}/license-status")
    public ResponseEntity<java.util.Map<String, Object>> getStructureLicenseStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(structureService.getStructureLicenseStatus(id));
    }
}
