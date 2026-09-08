package com.qualiapproche.amelioration.client;

import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowStateDto;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.common.dto.WorkflowValidationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
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
     * Décisions successives du circuit d'un dossier : étape, décision, auteur, date, commentaire.
     *
     * <p>C'est la matière des visas de la fiche de clôture : chaque niveau y appose sa date et son
     * appréciation, et seul le moteur en détient la trace faisant foi.</p>
     */
    @GetMapping("/api/v1/workflows/instances/{resourceId}/history")
    java.util.List<com.qualiapproche.common.dto.ValidationHistoryDto> historiqueDesDecisions(
            @PathVariable("resourceId") UUID resourceId);

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
     * Les dossiers qui attendent l'appelant, groupés par étape du circuit.
     *
     * <p>Les clés sont les libellés des étapes, tels que l'éditeur les a écrits : le module n'en
     * nomme aucune, et une étape ajoutée au circuit apparaît d'elle-même dans le compte.</p>
     *
     * <p>Le moteur rend les identifiants et non leur nombre : il ne sait pas lesquels de ces
     * dossiers existent encore ici. Comptés tels quels, ils annonçaient des lignes que la liste
     * « à traiter » — qui, elle, va les relire en base — n'affichait pas.</p>
     */
    @GetMapping("/api/v1/workflows/instances/mine/par-etape")
    Map<String, java.util.List<UUID>> mesDossiersParEtape(@RequestParam("resourceType") String resourceType);

    /**
     * Dit au moteur qu'une ressource a été supprimée ici.
     *
     * <p>Sans cet appel l'instance restait « en cours » indéfiniment chez lui, orpheline de son
     * dossier, et continuait d'alimenter tout ce qui se compte sans relire la table.</p>
     */
    @DeleteMapping("/api/v1/workflows/instances/{resourceId}")
    void oublierRessource(@PathVariable("resourceId") UUID resourceId);

    /**
     * Dépose une notification dans la boîte de quelqu'un.
     *
     * <p>Employé par les relances d'échéance, qui annoncent quelque chose sans qu'aucune transition
     * de circuit ne soit franchie. La clé d'unicité du dépôt évite qu'un passage quotidien empile
     * la même ligne tous les matins.</p>
     */
    @PostMapping("/api/v1/notifications/depot")
    void deposerNotification(@RequestBody com.qualiapproche.common.dto.DepotNotificationDto depot);

    /**
     * Les dossiers que l'appelant a ouverts et qui ne sont pas arrivés, chacun avec son avancement.
     *
     * <p>Pendant de {@link #ressourcesADecider} : celui-là dit ce qu'il a à décider, celui-ci ce
     * qu'il a déposé et qui attend ailleurs — ou ce qu'il n'a jamais soumis. C'est ainsi que le
     * module compte les brouillons sans connaître de statut « brouillon ».</p>
     */
    @GetMapping("/api/v1/workflows/instances/miennes")
    Map<UUID, com.qualiapproche.common.enumeration.AvancementCircuit> mesDossiersOuverts(
            @RequestParam("resourceType") String resourceType);

    /**
     * États de plusieurs ressources en un appel : une page de N dossiers déclencherait sinon N
     * requêtes.
     */
    @PostMapping("/api/v1/workflows/instances/states")
    Map<UUID, WorkflowStateDto> getWorkflowStates(@RequestBody java.util.List<UUID> resourceIds);

    /**
     * Où en est chaque dossier — non engagé, en cours, terminé — sans le reste de son état.
     *
     * <p>Ce qu'il faut à un tableau de bord, qui compte des dossiers sans en afficher aucun.
     * C'est aussi ce qui lui évite de nommer une étape : compter les dossiers clos en cherchant
     * l'état {@code CLOTURE}, ou les brouillons en cherchant le statut {@code DRAFT}, aurait
     * recopié ici une partie du circuit — et le chiffre serait devenu faux à la première étape
     * ajoutée.</p>
     */
    @PostMapping("/api/v1/workflows/instances/avancement")
    Map<UUID, com.qualiapproche.common.enumeration.AvancementCircuit> avancementDesRessources(
            @RequestBody java.util.List<UUID> resourceIds);

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
