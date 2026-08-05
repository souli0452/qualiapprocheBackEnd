package com.qualiapproche.workflow.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowValidationRequestDto;
import com.qualiapproche.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * API du service de workflow.
 *
 * <p>Deux familles d'appels y coexistent, et elles ne peuvent pas être protégées de la même
 * façon :</p>
 * <ul>
 *   <li><b>L'administration des circuits</b> (création, modification, suppression) vient du front
 *       et transite par la gateway, qui résout les permissions applicatives et les propage dans
 *       {@code X-User-Permissions}. C'est la seule famille où {@code @perm} peut statuer, et elle
 *       est désormais réservée à {@code workflow-write} — jusqu'ici n'importe quel utilisateur
 *       authentifié pouvait réécrire ou supprimer le circuit de validation de toute
 *       l'organisation.</li>
 *   <li><b>Le pilotage des instances</b> (initiation, décisions, lecture d'état) est appelé de
 *       service à service par support-service et amelioration-service. Leur intercepteur Feign
 *       propage désormais {@code X-User-Permissions} en plus du jeton, mais l'habilitation reste
 *       portée là où elle a du sens : par {@code WorkflowConditionAdapter}, qui exige pour chaque
 *       transition le rôle de son étape, vérifié auprès de user-service. Une permission générale
 *       ne dirait pas qui a le droit de décider de <em>cette</em> étape.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@RequirePermissions(
        create = {"workflow-write"},
        update = {"workflow-write"},
        delete = {"workflow-write"}
)
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    public ResponseEntity<List<com.qualiapproche.workflow.dto.WorkflowDto>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }

    @PostMapping
    @PreAuthorize("@perm.canCreate(this)")
    public ResponseEntity<com.qualiapproche.workflow.dto.WorkflowDto> createWorkflow(
            @RequestBody com.qualiapproche.workflow.dto.WorkflowDto workflowDto) {
        return ResponseEntity.ok(workflowService.createWorkflow(workflowDto));
    }

    @PutMapping("/{workflowId}")
    @PreAuthorize("@perm.canUpdate(this)")
    public ResponseEntity<com.qualiapproche.workflow.dto.WorkflowDto> updateWorkflow(@PathVariable UUID workflowId,
            @RequestBody com.qualiapproche.workflow.dto.WorkflowDto workflowDto) {
        return ResponseEntity.ok(workflowService.updateWorkflow(workflowId, workflowDto));
    }

    @DeleteMapping("/{workflowId}")
    @PreAuthorize("@perm.canDelete(this)")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable UUID workflowId) {
        workflowService.deleteWorkflow(workflowId);
        return ResponseEntity.ok().build();
    }

    /**
     * Familles de ressources sur lesquelles un circuit peut être ouvert.
     *
     * <p>Le front proposait une saisie libre pour ce champ, d'où des circuits configurés sur un
     * code de type documentaire ('PRO', 'ENR'…) qu'aucun service métier n'ouvre jamais. Cette
     * liste est la seule valable : à présenter en choix fermé.</p>
     */
    @GetMapping("/resource-types")
    public ResponseEntity<List<String>> getResourceTypes() {
        return ResponseEntity.ok(com.qualiapproche.workflow.model.TypeRessource.valeursAutorisees());
    }

    @GetMapping("/type/{documentType}")
    public ResponseEntity<List<com.qualiapproche.workflow.dto.WorkflowDto>> getWorkflowsByType(@PathVariable String documentType) {
        return ResponseEntity.ok(workflowService.getWorkflowsByType(documentType));
    }

    /**
     * Sélection déterministe du workflow actif pour un type : à utiliser à la place de
     * {@code /type/{documentType}} + "premier élément" côté services appelants.
     */
    @GetMapping("/type/{documentType}/active")
    public ResponseEntity<com.qualiapproche.workflow.dto.WorkflowDto> getActiveWorkflowByType(@PathVariable String documentType) {
        return ResponseEntity.ok(workflowService.getActiveWorkflowByType(documentType));
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<com.qualiapproche.workflow.dto.WorkflowDto> getWorkflowById(@PathVariable UUID workflowId) {
        return ResponseEntity.ok(workflowService.getWorkflowById(workflowId));
    }

    @GetMapping("/instances/{resourceId}")
    public ResponseEntity<WorkflowInstanceDto> getLastValidationInstance(@PathVariable UUID resourceId) {
        WorkflowInstanceDto instance = workflowService.getLastValidationInstance(resourceId);
        return instance != null ? ResponseEntity.ok(instance) : ResponseEntity.notFound().build();
    }

    @GetMapping("/instances/{resourceId}/state")
    public ResponseEntity<com.qualiapproche.workflow.dto.WorkflowStateDto> getWorkflowState(@PathVariable UUID resourceId) {
        com.qualiapproche.workflow.dto.WorkflowStateDto state = workflowService.getWorkflowStateForResource(resourceId);
        if (state == null) {
            state = com.qualiapproche.workflow.dto.WorkflowStateDto.builder()
                    .allowedActions(java.util.Collections.emptyList())
                    .build();
        }
        return ResponseEntity.ok(state);
    }

    /**
     * États de plusieurs ressources en un seul appel, pour l'affichage d'une liste : sans cela,
     * une page de N documents déclenchait N requêtes.
     */
    @PostMapping("/instances/states")
    public ResponseEntity<java.util.Map<UUID, com.qualiapproche.workflow.dto.WorkflowStateDto>> getWorkflowStates(
            @RequestBody List<UUID> resourceIds) {
        return ResponseEntity.ok(workflowService.getWorkflowStatesForResources(resourceIds));
    }

    /**
     * Ressources d'une famille sur lesquelles l'appelant a une décision à prendre.
     *
     * <p>Destiné aux listes « à traiter » des modules métier : c'est le circuit qui sait qui peut
     * agir, un module qui le déduirait de son propre état de dossier afficherait des dossiers que
     * le moteur refuse ensuite de faire avancer.</p>
     */
    @GetMapping("/instances/mine")
    public ResponseEntity<List<UUID>> ressourcesADecider(@RequestParam("resourceType") String resourceType) {
        return ResponseEntity.ok(workflowService.ressourcesADeciderParLAppelant(resourceType));
    }

    /**
     * Faits connus, à proposer dans l'éditeur de circuits.
     *
     * <p>Enveloppé explicitement : {@code GlobalResponseHandler} pagine d'office toute réponse de
     * type {@code List}, et le sélecteur de l'éditeur recevrait un objet paginé là où il attend un
     * tableau.</p>
     */
    @GetMapping("/faits")
    public ResponseEntity<com.qualiapproche.common.response.ApiResponse<List<String>>> faitsConnus() {
        return ResponseEntity.ok(com.qualiapproche.common.response.ApiResponse.success(workflowService.faitsConnus()));
    }

    /**
     * Redésigne le titulaire d'un dossier en cours.
     *
     * <p>Appelé de service à service, quand le module métier change la personne qui répond du
     * dossier hors d'une décision de circuit. Les deux doivent dire la même chose, sans quoi l'étape
     * reste ouverte à celui qui n'en répond plus.</p>
     */
    @PutMapping("/instances/{resourceId}/titulaire")
    public ResponseEntity<Void> designerTitulaire(
            @PathVariable UUID resourceId,
            @RequestParam(value = "titulaireId", required = false) String titulaireId) {
        workflowService.designerTitulaire(resourceId, titulaireId);
        return ResponseEntity.ok().build();
    }

    /**
     * Déclare ou retire un fait établi sur un dossier.
     *
     * <p>Appelé de service à service : le module métier sait quand la condition devient vraie, le
     * moteur se contente de l'exiger. C'est ce qui permet à un circuit de dire « on ne clôt pas
     * tant que les plans d'action ne sont pas soldés » sans que le moteur sache ce qu'est un plan
     * d'action.</p>
     */
    @PutMapping("/instances/{resourceId}/faits/{fait}")
    public ResponseEntity<Void> declarerFait(
            @PathVariable UUID resourceId,
            @PathVariable String fait,
            @RequestParam(value = "etabli", defaultValue = "true") boolean etabli) {
        workflowService.declarerFait(resourceId, fait, etabli);
        return ResponseEntity.ok().build();
    }

    /**
     * Prend la même décision sur plusieurs dossiers.
     *
     * <p>Rend un compte rendu par dossier, et non un simple succès : le moteur juge chacun pour
     * lui-même, et une partie de la sélection peut être refusée — habilitation manquante, étape
     * changée depuis l'affichage de la liste, champ requis absent. Le code de retour reste 200 :
     * un refus partiel n'est pas une erreur de la demande, c'est son résultat.</p>
     */
    @PostMapping("/decisions-groupees")
    public ResponseEntity<List<com.qualiapproche.workflow.dto.ResultatDecisionDto>> deciderEnLot(
            @RequestParam("decision") com.qualiapproche.workflow.model.StepDecision decision,
            @RequestParam("resourceIds") List<UUID> resourceIds,
            @RequestBody(required = false) WorkflowValidationRequestDto request) {
        return ResponseEntity.ok(workflowService.deciderEnLot(resourceIds, decision, request));
    }

    /** Traçabilité du circuit d'une ressource : décisions, auteurs, commentaires, valeurs saisies. */
    @GetMapping("/instances/{resourceId}/history")
    public ResponseEntity<List<com.qualiapproche.workflow.dto.ValidationHistoryDto>> getValidationHistory(
            @PathVariable UUID resourceId) {
        return ResponseEntity.ok(workflowService.getValidationHistory(resourceId));
    }

    @PostMapping("/initiate")
    public ResponseEntity<WorkflowInstanceDto> initiateWorkflow(
            @RequestParam("resourceId") UUID resourceId,
            @RequestParam("resourceType") String resourceType,
            @RequestParam("workflowId") UUID workflowId,
            @RequestParam(value = "titulaireId", required = false) String titulaireId) {

        return ResponseEntity.ok(
                workflowService.initiateWorkflow(resourceId, resourceType, workflowId, titulaireId));
    }

    @PostMapping("/validate/{resourceId}")
    public ResponseEntity<Void> validateStep(
            @PathVariable UUID resourceId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody(required = false) WorkflowValidationRequestDto request) {

        workflowService.validateStep(resourceId, userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reject/{resourceId}")
    public ResponseEntity<Void> rejectStep(
            @PathVariable UUID resourceId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody(required = false) WorkflowValidationRequestDto request) {

        workflowService.rejectStep(resourceId, userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/execute/{resourceId}")
    public ResponseEntity<Void> executeTransition(
            @PathVariable UUID resourceId,
            @RequestParam("transitionCode") String transitionCode,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody(required = false) WorkflowValidationRequestDto request) {

        workflowService.executeDynamicTransition(resourceId, userId, transitionCode, request);
        return ResponseEntity.ok().build();
    }
}
