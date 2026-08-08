package com.qualiapproche.support.client;

import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowStateDto;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.common.dto.WorkflowValidationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

import com.qualiapproche.support.config.FeignConfig;

@FeignClient(name = "workflow-service", configuration = FeignConfig.class)
public interface WorkflowClient {

    /**
     * Sélection déterministe du workflow actif pour un type de ressource : remplace
     * l'ancien pattern "prendre le premier élément de la liste" (ordre non garanti).
     */
    @GetMapping("/api/v1/workflows/type/{resourceType}/active")
    WorkflowSummaryDto getActiveWorkflowByType(@PathVariable("resourceType") String resourceType);

    /**
     * Circuit à ouvrir pour une famille et une catégorie de dossier.
     *
     * <p>Le moteur applique toute la règle : le circuit réservé à cette catégorie — celui d'un type
     * de document — et à défaut le circuit par défaut de la famille. Un seul appel, et la règle reste
     * chez celui qui détient les circuits : la rejouer ici l'aurait dédoublée.</p>
     *
     * <p>Répond 404 quand ni l'un ni l'autre n'existe.</p>
     */
    @GetMapping("/api/v1/workflows/actif")
    WorkflowSummaryDto circuitAOuvrir(@RequestParam("famille") String famille,
                                      @RequestParam(value = "cible", required = false) String cible);

    @GetMapping("/api/v1/workflows/{workflowId}")
    Map<String, Object> getWorkflowById(@PathVariable("workflowId") UUID workflowId);

    /**
     * Ouvre le circuit en transmettant la référence lisible du dossier.
     *
     * <p>C'est elle que citent les courriels d'étape (« n°{numeroNc} ») : le moteur ne détient que
     * l'UUID, et sans elle les messages partaient avec un numéro vide.</p>
     */
    @PostMapping("/api/v1/workflows/initiate")
    WorkflowInstanceDto initiateWorkflow(@RequestParam("resourceId") UUID resourceId,
                                         @RequestParam("resourceType") String resourceType,
                                         @RequestParam("workflowId") UUID workflowId,
                                         @RequestParam(value = "reference", required = false) String reference);

    @GetMapping("/api/v1/workflows/instances/{resourceId}")
    WorkflowInstanceDto getLastValidationInstance(@PathVariable("resourceId") UUID resourceId);

    @GetMapping("/api/v1/workflows/instances/{resourceId}/state")
    WorkflowStateDto getWorkflowState(@PathVariable("resourceId") UUID resourceId);

    /** États de plusieurs ressources en un appel, pour l'affichage d'une liste. */
    @PostMapping("/api/v1/workflows/instances/states")
    Map<UUID, WorkflowStateDto> getWorkflowStates(@RequestBody java.util.List<UUID> resourceIds);

    /**
     * Ressources sur lesquelles l'appelant a une décision ouverte.
     *
     * <p>C'est le circuit qui sait qui peut agir : le déduire du statut du document reviendrait à
     * tenir une seconde table de règles, qui ferait apparaître dans la liste de travail des
     * dossiers que le moteur refuse ensuite de faire avancer.</p>
     */
    @GetMapping("/api/v1/workflows/instances/mine")
    java.util.List<UUID> ressourcesADecider(@RequestParam("resourceType") String resourceType);

    @PostMapping("/api/v1/workflows/validate/{resourceId}")
    void validateStep(@PathVariable("resourceId") UUID resourceId,
                      @RequestHeader(value = "X-User-Id", required = false) String userId,
                      @RequestBody(required = false) WorkflowValidationRequestDto request);

    @PostMapping("/api/v1/workflows/reject/{resourceId}")
    void rejectStep(@PathVariable("resourceId") UUID resourceId,
                    @RequestHeader(value = "X-User-Id", required = false) String userId,
                    @RequestBody(required = false) WorkflowValidationRequestDto request);
}
