package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
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
@lombok.extern.slf4j.Slf4j
@Tag(name = "Non-Conformités", description = "Gestion des non-conformités, de leur création à leur validation")
@RequirePermissions(
        create = {"nc-write", "SUBMIT_NC", "TRAITEMENT_NC"},
        update = {"nc-write", "SUBMIT_NC", "TRAITEMENT_NC"},
        read = {"nc-read", "nc-write", "NC_READ", "CONSULTATION_NC", "SUBMIT_NC", "TRAITEMENT_NC"},
        delete = {"nc-write", "SUBMIT_NC", "TRAITEMENT_NC"},
        validate = {"nc-validate", "VALIDATION_RQ", "VALIDATION_CHEF"}
)
public class NonConformiteController {
    private final NonConformiteService nonConformiteService;
    private final com.qualiapproche.amelioration.client.WorkflowClient workflowClient;

    /**
     * Endpoint pour créer une non-conformité
     * 
     * @param dto Objet contenant les informations de la non-conformité
     * @return NonConformiteDto contenant les informations de la non-conformité
     *         créée
     */
    @Operation(summary = "Créer une non-conformité", description = "Initialise une nouvelle non-conformité dans le système")
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<NonConformiteDto> createNonConformite(@RequestBody NonConformiteDto dto) throws IOException {
        NonConformiteDto createdNonConformite = nonConformiteService.createNonConformite(dto);
        return ResponseEntity.ok(createdNonConformite);
    }

    /*-----------------------------------------------------------------------/
    /               Méthode de modification d'une non conformité             /
    /-----------------------------------------------------------------------*/

    @Operation(summary = "Modifier une non-conformité", description = "Met à jour les informations d'une non-conformité existante par son ID")
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_NON_CONFORMITE_PROCESSUS)
    public ResponseEntity<NonConformiteDto> updateNonConformite(@PathVariable UUID id,
            @RequestBody NonConformiteDto dto) throws IOException {
        return ResponseEntity.ok(nonConformiteService.updateNonConformite(id, dto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_MANY_NON_CONFORMITE_PROCESSUS)
    public ResponseEntity<List<NonConformiteDto>> updateManyNonConformite(@RequestBody List<NonConformiteDto> dtos)
            throws IOException {
        return ResponseEntity.ok(nonConformiteService.updateNonConformites(dtos));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_CONFORMITE_IMPUTED)
    public ResponseEntity<Page<NonConformiteDto>> getNonConformiteByUserId(@PathVariable Etat etapeTraitement,
            @PathVariable String userId, @ParameterObject Pageable pageable) throws IOException {
        return ResponseEntity.ok(nonConformiteService.findImupted(userId, etapeTraitement, pageable));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_NON_CONFORMITE_BY_ETAT_AND_STRUCTORIGIN)
    public ResponseEntity<Page<NonConformiteDto>> getNonConformitesByEtatAndOrigineId(
            @PathVariable Etat etapeTraitement, @PathVariable String structureId, @ParameterObject Pageable pageable) {
        return ResponseEntity
                .ok(nonConformiteService.getNonConformitesByEtatAndStructureOrigine(etapeTraitement, structureId, pageable));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_NON_CONFORMITE_BY_ETAT_AND_STRUCTSOUMISSION)
    public ResponseEntity<Page<NonConformiteDto>> getNonConformitesByEtatAndStructureSoumission(
            @PathVariable Etat etapeTraitement, @PathVariable String structureId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.getNonConformitesByEtatAnStructure(etapeTraitement, structureId, pageable));
    }

    /*-----------------------------------------------------------------------/
    /               Méthode de création d'une NonConformité                  /
    /-----------------------------------------------------------------------
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_NON_CONFORMITE)
    public ResponseEntity<NonConformiteDto> create(@RequestBody NonConformiteDto nonConformiteDto) {
        NonConformiteDto nonConformite = nonConformiteService.create(nonConformiteDto);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }  */

    /*-----------------------------------------------------------------------/
    /              Méthode de mise à jour d'une NonConformite                /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_NON_CONFORMITE)
    public ResponseEntity<NonConformiteDto> update(@RequestBody NonConformiteDto nonConformiteDto) {
        NonConformiteDto nonConformite = nonConformiteService.update(nonConformiteDto);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }



    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de toutes les NonConformités              /
    /-----------------------------------------------------------------------*/
    @Operation(summary = "Lister toutes les non-conformités", description = "Récupère la liste complète des non-conformités enregistrées")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_NON_CONFORMITE)
    public ResponseEntity<Page<NonConformiteDto>> allNonConformites(@ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformite = nonConformiteService.allNonConformites(pageable);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }
    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de NonConformités par Etat                /
    /-----------------------------------------------------------------------*/

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ETAT_BAY_NON_CONFORMITE)
    public Page<NonConformiteDto> getNonConformitesByEtat(@PathVariable Etat etapeTraitement, @ParameterObject Pageable pageable) {
        return nonConformiteService.getNonConformitesByEtatNonConformite(etapeTraitement, pageable);
    }

    /*-----------------------------------------------------------------------/
    /       Méthode de récupération d'une NonConformité par son ID           /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_NON_CONFORMITE_BY_ID)
    public ResponseEntity<NonConformiteDto> getNonConformiteById(@PathVariable UUID id) {
        NonConformiteDto nonConformite = nonConformiteService.getNonConformiteById(id);
        nonConformite.setWorkflowState(etatWorkflow(id));
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }

    /**
     * Étape courante et actions autorisées pour l'utilisateur appelant, à l'image de ce que fait
     * déjà le service documentaire : sans cette information, les écrans de non-conformité ne
     * disposaient d'aucun moyen d'afficher les actions du circuit de validation.
     *
     * <p>Une indisponibilité du service de workflow ne doit pas empêcher la consultation de la
     * non-conformité : l'état est alors simplement absent.</p>
     */
    private com.qualiapproche.common.dto.WorkflowStateDto etatWorkflow(UUID resourceId) {
        try {
            return workflowClient.getWorkflowState(resourceId);
        } catch (Exception e) {
            log.warn("État du workflow indisponible pour la non-conformité {} : {}", resourceId, e.getMessage());
            return null;
        }
    }

    /*-----------------------------------------------------------------------/
    /                Méthode de suppression d'une NonConformité              /
    /-----------------------------------------------------------------------*/
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_NON_CONFORMITE)
    public void deleteById(@PathVariable UUID id) {
        nonConformiteService.delete(id);
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/publish")
    public ResponseEntity<Page<NonConformiteDto>> getAllPublish(@RequestParam(required = false) Status status,
            @RequestParam(required = false) String id, @ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformiteDtos = nonConformiteService.findAll(status, id, pageable);
        return ResponseEntity.ok(nonConformiteDtos);
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<Page<NonConformiteDto>> getAll(@RequestParam(required = false) Status status,
            @RequestParam(required = false) String id, @ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformiteDtos = nonConformiteService.findAll(status, id, pageable);
        return ResponseEntity.ok(nonConformiteDtos);
    }

    @PreAuthorize("@perm.canDelete(this)")
    @PutMapping(path = "delete-multiple")
    public ResponseEntity<Void> deleteMultiple(@RequestBody List<NonConformiteDto> nonConformiteDtos) {
        nonConformiteService.deleteMultiple(nonConformiteDtos);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(path = "/count-by-status/{id}")
    public ResponseEntity<List<NcStats>> getCountByStatus(@PathVariable String id) {
        return ResponseEntity.ok(nonConformiteService.getNcStats(id));
    }



    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(path = "/stats/nf-struct/{annee}")
    public ResponseEntity<Map<String, Long>> StatStruct(@PathVariable int annee) {

        return ResponseEntity.ok(nonConformiteService.getNonConformiteStatsByStructure(annee));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(path = "/stats/nf/{annee}")
    public ResponseEntity<Map<String, Map<String, Long>>> StatMensuel(@PathVariable int annee) {
        return ResponseEntity.ok(nonConformiteService.getStatsParAnnee(annee));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(path = "/stats/nf/status/{annee}")
    public ResponseEntity<Map<String, Map<String, Map<String, Long>>>> StatMensuelWithStatus(@PathVariable int annee) {
        return ResponseEntity.ok(nonConformiteService.getStatsDetailleesParAnnee(annee));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(path = "/stats/nf/status/{annee}/service/{id}")
    public ResponseEntity<Map<String, Map<String, Map<String, Long>>>> StatMensuelWithServiceStatus(
            @PathVariable int annee, @PathVariable String id) {
        return ResponseEntity.ok(nonConformiteService.getStatsDetailleesServiceParAnnee(annee, id));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(path = "/stats/nf/status/{annee}/{id}")
    public ResponseEntity<Map<String, Map<String, Long>>> statService(@PathVariable int annee,
            @PathVariable String id) {
        return ResponseEntity.ok(nonConformiteService.getStatsMensuellesParService(annee, id));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("get/numero/{numero}")
    public ResponseEntity<NonConformiteDto> getNonConformiteByNumeroRef(@PathVariable String numero) {
        return ResponseEntity.ok(nonConformiteService.getByNumeroRef(numero));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("structure/{id}")
    public ResponseEntity<Page<NonConformiteDto>> getAllByStructure(@PathVariable String id, @ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformiteDtos = nonConformiteService.getNonConformitesByStructure(id, pageable);
        return ResponseEntity.ok(nonConformiteDtos);
    }

    @PreAuthorize("@perm.canValidate(this)")
    @PutMapping(path = "validate/plan")
    public ResponseEntity<ValidatePlanActionDto> validate(@RequestBody ValidatePlanActionDto validatePlanActionDto) {
        nonConformiteService.validatePlan(validatePlanActionDto);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(path = "/stats/nf/niveau/{annee}/service/{id}")
    public ResponseEntity<Map<String, Map<String, Map<String, Long>>>> statsMajeur(@PathVariable int annee,
            @PathVariable String id) {
        return ResponseEntity.ok(nonConformiteService.getStatsNiveauParAnnee(annee, id));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all/structure/{id}")
    public ResponseEntity<Page<NonConformiteDto>> getAllByStructu(@PathVariable String id, @ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformiteDtos = nonConformiteService.findAllByStructure(id, pageable);
        return ResponseEntity.ok(nonConformiteDtos);
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/initiateur/{id}")
    public ResponseEntity<Page<NonConformiteDto>> getAllByInitiator(@PathVariable String id, @ParameterObject Pageable pageable) {
        Page<NonConformiteDto> nonConformiteDtos = nonConformiteService.findAllByInitiator(id, pageable);
        return ResponseEntity.ok(nonConformiteDtos);
    }


    // --- User specific lists ---

    @Operation(summary = "NC créées par l'utilisateur", description = "Récupère toutes les NC actives (Brouillon, Publié, En cours) créées par l'utilisateur")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NonConformiteDto>> getNCByUser(@PathVariable String userId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.findByUser(userId, pageable));
    }

    @Operation(summary = "NC imputées à l'utilisateur", description = "Récupère toutes les NC qui ont été assignées à cet utilisateur")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/user/{userId}/imputed")
    public ResponseEntity<Page<NonConformiteDto>> getImputedNCByUser(@PathVariable String userId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.findImputedByUser(userId, pageable));
    }

    @Operation(summary = "NC archivées par l'utilisateur", description = "Récupère uniquement les NC archivées par cet utilisateur")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/user/{userId}/archived")
    public ResponseEntity<Page<NonConformiteDto>> getArchivedNCByUser(@PathVariable String userId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.findArchivedByUser(userId, pageable));
    }

    @Operation(summary = "Nombres de NC pour les pastilles", description = "Renvoie les comptes (brouillons, imputées, archivées) pour l'utilisateur")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/user/{userId}/counts")
    public ResponseEntity<NcCountsDto> getNCCountsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(nonConformiteService.getCountsByUser(userId));
    }

    // --- Structure specific lists ---

    @Operation(summary = "NC par structure", description = "Toutes les NC liées à un service (en tant que Soumissionnaire ou Origine)")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/structure/{structureId}")
    public ResponseEntity<Page<NonConformiteDto>> getNCByStructure(@PathVariable String structureId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.findByStructure(structureId, pageable));
    }

    @Operation(summary = "NC de tous les utilisateurs de la structure", description = "Récupère toutes les NC créées par n'importe quel agent de cette structure")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/structure/{structureId}/all-users")
    public ResponseEntity<Page<NonConformiteDto>> getNCByStructureAllUsers(@PathVariable String structureId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.findByStructureAllUsers(structureId, pageable));
    }

    // --- Dashboard endpoints ---

    @Operation(summary = "Dashboard pour RQ", description = "Statistiques globales des NC pour le dashboard RQ")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/dashboard/rq")
    public ResponseEntity<NcDashboardDto> getDashboardRQ() {
        return ResponseEntity.ok(nonConformiteService.getDashboardRQ());
    }

    @Operation(summary = "Dashboard pour Pilote", description = "Statistiques des NC par structure pour le dashboard Pilote")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/dashboard/pilot/{structureId}")
    public ResponseEntity<NcDashboardDto> getDashboardPilot(@PathVariable String structureId) {
        return ResponseEntity.ok(nonConformiteService.getDashboardPilot(structureId));
    }

    @Operation(summary = "Dashboard pour Utilisateur", description = "Statistiques des NC liées à l'utilisateur (soumis ou imputé)")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/dashboard/user/{userId}")
    public ResponseEntity<NcDashboardDto> getDashboardUser(@PathVariable String userId) {
        return ResponseEntity.ok(nonConformiteService.getDashboardUser(userId));
    }

    @Operation(summary = "Evolution des non-conformités", description = "Statistiques d'évolution des non-conformités filtrées par année, mois optionnel, et structure optionnelle")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/stats/evolution")
    public ResponseEntity<NcEvolutionDto> getNcEvolution(
            @RequestParam int annee,
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) String structureId) {
        return ResponseEntity.ok(nonConformiteService.getNcEvolutionStats(annee, mois, structureId));
    }

    @Operation(summary = "NC par niveau", description = "Récupérer les NC en rapport avec une gravité ou niveau de NC")
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/by-niveau/{niveauId}")
    public ResponseEntity<Page<NonConformiteDto>> getByNiveau(@PathVariable UUID niveauId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nonConformiteService.getNonConformitesByNiveau(niveauId, pageable));
    }

}