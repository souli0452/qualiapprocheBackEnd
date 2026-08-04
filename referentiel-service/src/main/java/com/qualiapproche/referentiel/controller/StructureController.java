
package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;

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
@RequirePermissions(
        create = {"structure-write", "STRUCT_MANAGE", "SERVICE_MANAGE"},
        update = {"structure-write", "STRUCT_MANAGE", "SERVICE_MANAGE"},
        read = {"structure-read", "structure-write", "CONFIG_READ", "STRUCT_MANAGE", "SERVICE_MANAGE"},
        delete = {"structure-write", "STRUCT_MANAGE", "SERVICE_MANAGE"}
)
/*
 * Classe et non plus record : un record est implicitement final, et la sécurité au niveau des
 * méthodes (@PreAuthorize) s'applique par un proxy CGLIB qui doit pouvoir sous-classer le bean.
 * Le contrôleur n'exposait par ailleurs aucun accesseur de record à l'extérieur.
 */
public class StructureController {

    private final StructureService structureService;

    public StructureController(StructureService structureService) {
        this.structureService = structureService;
    }

    @Operation(summary = "Créer une structure", description = "Ajoute une nouvelle entité organisationnelle au référentiel")
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_STRUCTURE)
    public ResponseEntity<StructureDto> saveStructure(@RequestBody StructureDto structureDto) {
        return ResponseEntity.ok(structureService.saveStructure(structureDto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_STRUCTURE)
    public ResponseEntity<StructureDto> updateStructure(@RequestBody StructureDto structureDto) throws Exception {
        return ResponseEntity.ok(structureService.updateStructure(structureDto));
    }


    // Appelé de service à service (Feign) : la gateway n'intervient pas, aucune
    // permission applicative n'y est résolue. Non gardé, comme les points de
    // pilotage d'instances de WorkflowController.
    @GetMapping(STRUCTURE_BY_ID)
    public ResponseEntity<StructureDto> getStructureById(@PathVariable UUID id)  {
        return ResponseEntity.ok(structureService.getStructureById(id));
    }



    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(STRUCTURENAME_BY_ID)
    public String getStructureNameById(@PathVariable("structureid") UUID structureid) {
        return structureService.getStructureNameById(structureid);
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_STRUCTURE)
    public ResponseEntity<Page<StructureDto>> getAllStructuresAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(structureService.getAllStructuresAll(pageable));
    }



    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<Page<StructureDto>> getAllStructures(@RequestParam(required = false) TypeStructure typeStructure,
                                                               @RequestParam(required = false) UUID directionId,
                                                               @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(structureService.getAllStructures(typeStructure, directionId, pageable));
    }


    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_STRUCTURE)
    public ResponseEntity<Void> deleteStructure(@PathVariable UUID id)  {
        structureService.deleteStructure(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(STRUCTURE_BY_LIBELLE_URL)
    public ResponseEntity<StructureDto> getStructureByLibelle(@PathVariable String libelle)  {
        return ResponseEntity.ok(structureService.findStructureByLibelle(libelle));
    }

    // Appelé de service à service (Feign) : la gateway n'intervient pas, aucune
    // permission applicative n'y est résolue. Non gardé, comme les points de
    // pilotage d'instances de WorkflowController.
    @GetMapping("/direction")
    public ResponseEntity<StructureDto> getDirection() {
        return ResponseEntity.ok(structureService.getDirection());
    }

    // Appelé de service à service (Feign) : la gateway n'intervient pas, aucune
    // permission applicative n'y est résolue. Non gardé, comme les points de
    // pilotage d'instances de WorkflowController.
    @GetMapping("/{id}/license-status")
    public ResponseEntity<java.util.Map<String, Object>> getStructureLicenseStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(structureService.getStructureLicenseStatus(id));
    }
}
