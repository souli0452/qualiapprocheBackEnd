package com.qualiapproche.amelioration.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.qualiapproche.common.dto.NcStats;
import com.qualiapproche.common.dto.NcEvolutionDto;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.dto.RejectNonConformiteDto;
import com.qualiapproche.common.dto.NcCountsDto;
import com.qualiapproche.common.dto.NcDashboardDto;
import com.qualiapproche.common.dto.ValidatePlanActionDto;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;

import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.amelioration.service.NonConformiteService;

import static com.qualiapproche.common.utils.ApiUrls.*;

@RestController
@RequestMapping(NON_CONFORMITE_ROOT_URL)
@RequiredArgsConstructor
@Tag(name = "Non-Conformités", description = "Gestion des non-conformités, de leur création à leur validation")
public class NonConformiteController {
    private final NonConformiteService nonConformiteService;

    /**
     * Endpoint pour créer une non-conformité
     * 
     * @param dto Objet contenant les informations de la non-conformité
     * @return NonConformiteDto contenant les informations de la non-conformité
     *         créée
     */
    @Operation(summary = "Créer une non-conformité", description = "Initialise une nouvelle non-conformité dans le système")
    @PostMapping("/create")
    public ResponseEntity<NonConformiteDto> createNonConformite(@RequestBody NonConformiteDto dto) throws IOException {
        NonConformiteDto createdNonConformite = nonConformiteService.createNonConformite(dto);
        return ResponseEntity.ok(createdNonConformite);
    }

    /*-----------------------------------------------------------------------/
    /               Méthode de modification d'une non conformité             /
    /-----------------------------------------------------------------------*/

    @Operation(summary = "Modifier une non-conformité", description = "Met à jour les informations d'une non-conformité existante par son ID")
    @PutMapping(UPDATE_NON_CONFORMITE_PROCESSUS)
    public ResponseEntity<NonConformiteDto> updateNonConformite(@PathVariable UUID id,
            @RequestBody NonConformiteDto dto) throws IOException {
        return ResponseEntity.ok(nonConformiteService.updateNonConformite(id, dto));
    }

    @PutMapping(UPDATE_MANY_NON_CONFORMITE_PROCESSUS)
    public ResponseEntity<List<NonConformiteDto>> updateManyNonConformite(@RequestBody List<NonConformiteDto> dtos)
            throws IOException {
        return ResponseEntity.ok(nonConformiteService.updateNonConformites(dtos));
    }

    @GetMapping(GET_ALL_CONFORMITE_IMPUTED)
    public ResponseEntity<Page<NonConformiteDto>> getNonConformiteByUserId(@PathVariable Etat etapeTraitement,
            @PathVariable String userId, @ParameterObject Pageable pageable) throws IOException {
        return ResponseEntity.ok(nonConformiteService.findImupted(userId, etapeTraitement, pageable));
    }

    @GetMapping(GET_NON_CONFORMITE_BY_ETAT_AND_STRUCTORIGIN)
    public ResponseEntity<Page<NonConformiteDto>> getNonConformitesByEtatAndOrigineId(
            @PathVariable Etat etapeTraitement, @PathVariable String structureId, @ParameterObject Pageable pageable) {
        return ResponseEntity
                .ok(nonConformiteService.getNonConformitesByEtatAndStructureOrigine(etapeTraitement, structureId, pageable));
    }

    @GetMapping(GET_NON_CONFORMITE_BY_ETAT_AND_STRUCTSOUMISSION)
    public ResponseEntity<Page<NonConformiteDto>> getNonConformitesByEtatAndStructureSoumission(
            @PathVariable Etat etapeTraitement, @PathVariable String structureId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.getNonConformitesByEtatAnStructure(etapeTraitement, structureId, pageable));
    }

    /*-----------------------------------------------------------------------/
    /               Méthode de création d'une NonConformité                  /
    /-----------------------------------------------------------------------
    @PostMapping(CREATE_NON_CONFORMITE)
    public ResponseEntity<NonConformiteDto> create(@RequestBody NonConformiteDto nonConformiteDto) {
        NonConformiteDto nonConformite = nonConformiteService.create(nonConformiteDto);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }  */

    /*-----------------------------------------------------------------------/
    /              Méthode de mise à jour d'une NonConformite                /
    /-----------------------------------------------------------------------*/
    @PutMapping(UPDATE_NON_CONFORMITE)
    public ResponseEntity<NonConformiteDto> update(@RequestBody NonConformiteDto nonConformiteDto) {
        NonConformiteDto nonConformite = nonConformiteService.update(nonConformiteDto);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }



    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de toutes les NonConformités              /
    /-----------------------------------------------------------------------*/
    @Operation(summary = "Lister toutes les non-conformités", description = "Récupère la liste complète des non-conformités enregistrées")
    @GetMapping(GET_ALL_NON_CONFORMITE)
    public ResponseEntity<Page<NonConformiteDto>> allNonConformites(@ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformite = nonConformiteService.allNonConformites(pageable);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }
    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de NonConformités par Etat                /
    /-----------------------------------------------------------------------*/

    @GetMapping(GET_ETAT_BAY_NON_CONFORMITE)
    public Page<NonConformiteDto> getNonConformitesByEtat(@PathVariable Etat etapeTraitement, @ParameterObject Pageable pageable) {
        return nonConformiteService.getNonConformitesByEtatNonConformite(etapeTraitement, pageable);
    }

    /*-----------------------------------------------------------------------/
    /       Méthode de récupération d'une NonConformité par son ID           /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_NON_CONFORMITE_BY_ID)
    public ResponseEntity<NonConformiteDto> getNonConformiteById(@PathVariable UUID id) {
        NonConformiteDto nonConformite = nonConformiteService.getNonConformiteById(id);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                Méthode de suppression d'une NonConformité              /
    /-----------------------------------------------------------------------*/
    @DeleteMapping(DELETE_NON_CONFORMITE)
    public void deleteById(@PathVariable UUID id) {
        nonConformiteService.delete(id);
    }

    @GetMapping("/publish")
    public ResponseEntity<Page<NonConformiteDto>> getAllPublish(@RequestParam(required = false) Status status,
            @RequestParam(required = false) String id, @ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformiteDtos = nonConformiteService.findAll(status, id, pageable);
        return ResponseEntity.ok(nonConformiteDtos);
    }

    @GetMapping
    public ResponseEntity<Page<NonConformiteDto>> getAll(@RequestParam(required = false) Status status,
            @RequestParam(required = false) String id, @ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformiteDtos = nonConformiteService.findAll(status, id, pageable);
        return ResponseEntity.ok(nonConformiteDtos);
    }

    @PutMapping(path = "delete-multiple")
    public ResponseEntity<Void> deleteMultiple(@RequestBody List<NonConformiteDto> nonConformiteDtos) {
        nonConformiteService.deleteMultiple(nonConformiteDtos);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/count-by-status/{id}")
    public ResponseEntity<List<NcStats>> getCountByStatus(@PathVariable String id) {
        return ResponseEntity.ok(nonConformiteService.getNcStats(id));
    }



    @GetMapping(path = "/stats/nf-struct/{annee}")
    public ResponseEntity<Map<String, Long>> StatStruct(@PathVariable int annee) {

        return ResponseEntity.ok(nonConformiteService.getNonConformiteStatsByStructure(annee));
    }

    @GetMapping(path = "/stats/nf/{annee}")
    public ResponseEntity<Map<String, Map<String, Long>>> StatMensuel(@PathVariable int annee) {
        return ResponseEntity.ok(nonConformiteService.getStatsParAnnee(annee));
    }

    @GetMapping(path = "/stats/nf/status/{annee}")
    public ResponseEntity<Map<String, Map<String, Map<String, Long>>>> StatMensuelWithStatus(@PathVariable int annee) {
        return ResponseEntity.ok(nonConformiteService.getStatsDetailleesParAnnee(annee));
    }

    @GetMapping(path = "/stats/nf/status/{annee}/service/{id}")
    public ResponseEntity<Map<String, Map<String, Map<String, Long>>>> StatMensuelWithServiceStatus(
            @PathVariable int annee, @PathVariable String id) {
        return ResponseEntity.ok(nonConformiteService.getStatsDetailleesServiceParAnnee(annee, id));
    }

    @GetMapping(path = "/stats/nf/status/{annee}/{id}")
    public ResponseEntity<Map<String, Map<String, Long>>> statService(@PathVariable int annee,
            @PathVariable String id) {
        return ResponseEntity.ok(nonConformiteService.getStatsMensuellesParService(annee, id));
    }

    @GetMapping("get/numero/{numero}")
    public ResponseEntity<NonConformiteDto> getNonConformiteByNumeroRef(@PathVariable String numero) {
        return ResponseEntity.ok(nonConformiteService.getByNumeroRef(numero));
    }

    @GetMapping("structure/{id}")
    public ResponseEntity<Page<NonConformiteDto>> getAllByStructure(@PathVariable String id, @ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformiteDtos = nonConformiteService.getNonConformitesByStructure(id, pageable);
        return ResponseEntity.ok(nonConformiteDtos);
    }

    @PutMapping(path = "validate/plan")
    public ResponseEntity<ValidatePlanActionDto> validate(@RequestBody ValidatePlanActionDto validatePlanActionDto) {
        nonConformiteService.validatePlan(validatePlanActionDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/stats/nf/niveau/{annee}/service/{id}")
    public ResponseEntity<Map<String, Map<String, Map<String, Long>>>> statsMajeur(@PathVariable int annee,
            @PathVariable String id) {
        return ResponseEntity.ok(nonConformiteService.getStatsNiveauParAnnee(annee, id));
    }

    @GetMapping("/all/structure/{id}")
    public ResponseEntity<Page<NonConformiteDto>> getAllByStructu(@PathVariable String id, @ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformiteDtos = nonConformiteService.findAllByStructure(id, pageable);
        return ResponseEntity.ok(nonConformiteDtos);
    }

    @GetMapping("/initiateur/{id}")
    public ResponseEntity<Page<NonConformiteDto>> getAllByInitiator(@PathVariable String id, @ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformiteDtos = nonConformiteService.findAllByInitiator(id, pageable);
        return ResponseEntity.ok(nonConformiteDtos);
    }


    // --- User specific lists ---

    @Operation(summary = "NC créées par l'utilisateur", description = "Récupère toutes les NC actives (Brouillon, Publié, En cours) créées par l'utilisateur")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NonConformiteDto>> getNCByUser(@PathVariable String userId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.findByUser(userId, pageable));
    }

    @Operation(summary = "NC imputées à l'utilisateur", description = "Récupère toutes les NC qui ont été assignées à cet utilisateur")
    @GetMapping("/user/{userId}/imputed")
    public ResponseEntity<Page<NonConformiteDto>> getImputedNCByUser(@PathVariable String userId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.findImputedByUser(userId, pageable));
    }

    @Operation(summary = "NC archivées par l'utilisateur", description = "Récupère uniquement les NC archivées par cet utilisateur")
    @GetMapping("/user/{userId}/archived")
    public ResponseEntity<Page<NonConformiteDto>> getArchivedNCByUser(@PathVariable String userId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.findArchivedByUser(userId, pageable));
    }

    @Operation(summary = "Nombres de NC pour les pastilles", description = "Renvoie les comptes (brouillons, imputées, archivées) pour l'utilisateur")
    @GetMapping("/user/{userId}/counts")
    public ResponseEntity<NcCountsDto> getNCCountsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(nonConformiteService.getCountsByUser(userId));
    }

    // --- Structure specific lists ---

    @Operation(summary = "NC par structure", description = "Toutes les NC liées à un service (en tant que Soumissionnaire ou Origine)")
    @GetMapping("/structure/{structureId}")
    public ResponseEntity<Page<NonConformiteDto>> getNCByStructure(@PathVariable String structureId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.findByStructure(structureId, pageable));
    }

    @Operation(summary = "NC de tous les utilisateurs de la structure", description = "Récupère toutes les NC créées par n'importe quel agent de cette structure")
    @GetMapping("/structure/{structureId}/all-users")
    public ResponseEntity<Page<NonConformiteDto>> getNCByStructureAllUsers(@PathVariable String structureId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.findByStructureAllUsers(structureId, pageable));
    }

    // --- Dashboard endpoints ---

    @Operation(summary = "Dashboard pour RQ", description = "Statistiques globales des NC pour le dashboard RQ")
    @GetMapping("/dashboard/rq")
    public ResponseEntity<NcDashboardDto> getDashboardRQ() {
        return ResponseEntity.ok(nonConformiteService.getDashboardRQ());
    }

    @Operation(summary = "Dashboard pour Pilote", description = "Statistiques des NC par structure pour le dashboard Pilote")
    @GetMapping("/dashboard/pilot/{structureId}")
    public ResponseEntity<NcDashboardDto> getDashboardPilot(@PathVariable String structureId) {
        return ResponseEntity.ok(nonConformiteService.getDashboardPilot(structureId));
    }

    @Operation(summary = "Dashboard pour Utilisateur", description = "Statistiques des NC liées à l'utilisateur (soumis ou imputé)")
    @GetMapping("/dashboard/user/{userId}")
    public ResponseEntity<NcDashboardDto> getDashboardUser(@PathVariable String userId) {
        return ResponseEntity.ok(nonConformiteService.getDashboardUser(userId));
    }

    @Operation(summary = "Evolution des non-conformités", description = "Statistiques d'évolution des non-conformités filtrées par année, mois optionnel, et structure optionnelle")
    @GetMapping("/stats/evolution")
    public ResponseEntity<NcEvolutionDto> getNcEvolution(
            @RequestParam int annee,
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) String structureId) {
        return ResponseEntity.ok(nonConformiteService.getNcEvolutionStats(annee, mois, structureId));
    }

    @Operation(summary = "NC par niveau", description = "Récupérer les NC en rapport avec une gravité ou niveau de NC")
    @GetMapping("/by-niveau/{niveauId}")
    public ResponseEntity<Page<NonConformiteDto>> getByNiveau(@PathVariable UUID niveauId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.getNonConformitesByNiveau(niveauId, pageable));
    }

}