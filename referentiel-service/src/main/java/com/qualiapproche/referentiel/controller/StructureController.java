
package com.qualiapproche.referentiel.controller;


import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.referentiel.service.StructureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import static com.qualiapproche.common.utils.ApiUrls.*;


@RestController
@RequestMapping(ROOT_STRUCTURE_API)
public record StructureController(StructureService structureService) {

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
    public ResponseEntity<List<StructureDto>> getAllStructuresAll() {
        return ResponseEntity.ok(structureService.getAllStructuresAll());
    }



    @GetMapping
    public ResponseEntity<List<StructureDto>> getAllStructures(@RequestParam(required = false) TypeStructure typeStructure,
                                                               @RequestParam(required = false) UUID directionId) {
        return ResponseEntity.ok(structureService.getAllStructures(typeStructure, directionId));
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
}
