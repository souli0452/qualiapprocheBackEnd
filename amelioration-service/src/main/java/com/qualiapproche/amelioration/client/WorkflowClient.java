package com.qualiapproche.amelioration.client;

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
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "workflow-service")
public interface WorkflowClient {

    /**
     * Sélection déterministe du workflow actif pour un type de ressource : remplace
     * l'ancien pattern "prendre le premier élément de la liste" (ordre non garanti).
     */
    @GetMapping("/api/v1/workflows/type/{resourceType}/active")
    WorkflowSummaryDto getActiveWorkflowByType(@PathVariable("resourceType") String resourceType);

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

    /**
     * Ouvre un circuit en désignant d'emblée son titulaire.
     *
     * <p>Un plan d'action a un responsable dès sa rédaction : c'est lui qui doit le traiter, et non
     * quiconque porterait un rôle. La non-conformité, elle, n'a de titulaire qu'à l'imputation.</p>
     */
    @PostMapping("/api/v1/workflows/initiate")
    WorkflowInstanceDto initiateWorkflow(@RequestParam("resourceId") UUID resourceId,
                                         @RequestParam("resourceType") String resourceType,
                                         @RequestParam("workflowId") UUID workflowId,
                                         @RequestParam("titulaireId") String titulaireId,
                                         @RequestParam(value = "reference", required = false) String reference);

    @GetMapping("/api/v1/workflows/instances/{resourceId}")
    WorkflowInstanceDto getLastValidationInstance(@PathVariable("resourceId") UUID resourceId);

    @GetMapping("/api/v1/workflows/instances/{resourceId}/state")
    WorkflowStateDto getWorkflowState(@PathVariable("resourceId") UUID resourceId);

    /**
     * Ressources sur lesquelles l'appelant a une décision ouverte.
     *
     * <p>C'est le circuit qui sait qui peut agir : le déduire ici de l'état de traitement du
     * dossier revenait à tenir une seconde table de règles, et faisait apparaître dans les listes
     * des dossiers que le moteur refusait ensuite de faire avancer.</p>
     */
    @GetMapping("/api/v1/workflows/instances/mine")
    java.util.List<UUID> ressourcesADecider(@RequestParam("resourceType") String resourceType);

    /**
     * États de plusieurs ressources en un appel : une page de N dossiers déclencherait sinon N
     * requêtes.
     */
    @PostMapping("/api/v1/workflows/instances/states")
    Map<UUID, WorkflowStateDto> getWorkflowStates(@RequestBody java.util.List<UUID> resourceIds);

    /**
     * Déclare ou retire un fait établi sur un dossier.
     *
     * <p>C'est ainsi qu'une règle métier devient une condition de circuit : le module sait quand
     * « tous les plans d'action sont soldés » devient vrai, le circuit l'exige pour clore. Aucun
     * des deux n'a besoin de connaître l'autre.</p>
     */
    @PutMapping("/api/v1/workflows/instances/{resourceId}/faits/{fait}")
    void declarerFait(@PathVariable("resourceId") UUID resourceId,
                      @PathVariable("fait") String fait,
                      @RequestParam("etabli") boolean etabli);

    /**
     * Redésigne la personne à qui les étapes réservées au titulaire sont ouvertes.
     *
     * <p>Sans cet appel, changer le responsable côté module laissait le moteur réserver l'étape à
     * l'ancien : l'un croyait avoir transféré la responsabilité, l'autre l'ouvrait toujours à celui
     * qui ne l'avait plus.</p>
     */
    @PutMapping("/api/v1/workflows/instances/{resourceId}/titulaire")
    void designerTitulaire(@PathVariable("resourceId") UUID resourceId,
                           @RequestParam("titulaireId") String titulaireId);

    @PostMapping("/api/v1/workflows/validate/{resourceId}")
    void validateStep(@PathVariable("resourceId") UUID resourceId,
                      @RequestHeader(value = "X-User-Id", required = false) String userId,
                      @RequestBody(required = false) WorkflowValidationRequestDto request);

    @PostMapping("/api/v1/workflows/reject/{resourceId}")
    void rejectStep(@PathVariable("resourceId") UUID resourceId,
                    @RequestHeader(value = "X-User-Id", required = false) String userId,
                    @RequestBody(required = false) WorkflowValidationRequestDto request);
}
