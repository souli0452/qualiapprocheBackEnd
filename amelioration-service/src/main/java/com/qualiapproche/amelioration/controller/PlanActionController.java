package com.qualiapproche.amelioration.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.common.utils.LotsDuMoteur;
import com.qualiapproche.common.utils.StatutEnum;

import java.io.IOException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;

import com.qualiapproche.amelioration.service.PlanActionService;
import com.qualiapproche.amelioration.service.impl.AccesAuxDossiers;
import static com.qualiapproche.common.utils.ApiUrls.*;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.common.dto.WorkflowStateDto;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
@RequestMapping(PLAN_ACTION_ROOT_URL)
@RequirePermissions(
        create = {"plan-action-write", "TRAITEMENT_PLAN"},
        update = {"plan-action-write", "TRAITEMENT_PLAN"},
        read = {"plan-action-read", "plan-action-write", "ACTIONS_READ", "TRAITEMENT_PLAN"},
        delete = {"plan-action-write", "TRAITEMENT_PLAN"},
        validate = {"plan-action-validate", "TRAITEMENT_PLAN"}
)
public class PlanActionController {

    private final PlanActionService planActionService;
    private final WorkflowClient workflowClient;

    /**
     * La borne du dossier, appliquée au plan pris par son identifiant.
     *
     * <p>La fiche d'un plan joint la non-conformité qui le motive : sans cette borne, la
     * permission générale du module suffisait à lire, corriger ou supprimer le plan — et donc à
     * lire le dossier — d'une autre structure.</p>
     */
    private final AccesAuxDossiers acces;

    /**
     * Ajoute un plan d'action au dossier désigné.
     *
     * <p>Le dossier doit être consultable par l'appelant, comme pour lire ou corriger un plan :
     * sans cette borne, un agent d'une autre structure pouvait écrire dans l'enregistrement qualité
     * d'autrui, lire le résumé du dossier en retour, et surtout s'y désigner responsable d'un plan —
     * ce qui lui ouvrait ensuite tout le dossier, puisque le responsable d'un plan compte parmi
     * ceux qui le consultent. Le front ne crée de plan que depuis la fiche d'un dossier déjà
     * ouverte : aucun parcours légitime n'est fermé.</p>
     */
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(CREATE_PLAN_ACTION)
    public ResponseEntity<PlanActionDto> create(@RequestBody PlanActionDto dto) throws IOException {
        if (dto == null || dto.getNonConformeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un plan d'action doit être rattaché à une non-conformité.");
        }
        acces.exigerLaNonConformite(dto.getNonConformeId());
        PlanActionDto planActionDto = planActionService.createPlanActionDto(dto);
        return new ResponseEntity<>(planActionDto, HttpStatus.OK);
    }

    /**
     * Corrige la description d'un plan d'action.
     *
     * <p>Ce point d'entrée manquait, alors que le front l'appelait : la correction d'un plan
     * partait en 404 et l'écran annonçait un succès. L'avancement n'est pas modifiable ici — il
     * relève du circuit.</p>
     */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping(UPDATE_PLAN_ACTION)
    public ResponseEntity<PlanActionDto> corriger(@RequestBody PlanActionDto dto) {
        if (dto != null && dto.getId() != null) {
            acces.exigerLePlan(dto.getId());
        }
        return ResponseEntity.ok(planActionService.corriger(dto));
    }

    /**
     * Actions correctives sur lesquelles l'appelant a une décision à prendre.
     *
     * <p>Le pendant, pour les actions, de {@code /non-conformite/a-traiter} : c'est le circuit qui
     * désigne les dossiers actionnables, non un croisement rôle × statut écrit dans l'écran.</p>
     */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/a-traiter")
    public ResponseEntity<Page<PlanActionDto>> aTraiter(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(avecEtatDuCircuit(planActionService.aTraiterParLAppelant(pageable)));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_PLAN_ACTION)
    public ResponseEntity<Page<PlanActionDto>> allActions(@ParameterObject Pageable pageable) {
        Page<PlanActionDto> planActionDtos = avecEtatDuCircuit(planActionService.allPlanActions(pageable));
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }

    /**
     * Joint à chaque plan l'état de son circuit.
     *
     * <p>Sans lui, les écrans de traitement n'affichent aucune action et retombent sur des boutons
     * statiques qui écrivent le statut sans que le circuit en sache rien. Un appel par lot, et non
     * un par ligne. Le moteur indisponible n'empêche pas de consulter la liste : elle s'affiche
     * sans ses actions, ce qui vaut mieux qu'un écran en échec.</p>
     */
    private Page<PlanActionDto> avecEtatDuCircuit(Page<PlanActionDto> page) {
        List<UUID> identifiants = page.getContent().stream()
                .map(PlanActionDto::getId)
                .filter(Objects::nonNull)
                .toList();
        if (identifiants.isEmpty()) {
            return page;
        }

        Map<UUID, WorkflowStateDto> etats = new HashMap<>();
        for (int debut = 0; debut < identifiants.size(); debut += LotsDuMoteur.TAILLE_MAX) {
            List<UUID> lot = identifiants.subList(debut, Math.min(debut + LotsDuMoteur.TAILLE_MAX, identifiants.size()));
            try {
                Map<UUID, WorkflowStateDto> reponse = workflowClient.getWorkflowStates(lot);
                if (reponse != null) {
                    etats.putAll(reponse);
                }
            } catch (Exception e) {
                log.warn("États de circuit indisponibles pour {} plan(s) d'action de cette page : {}",
                        lot.size(), e.getMessage());
            }
        }

        page.getContent().forEach(plan -> plan.setWorkflowState(etats.get(plan.getId())));
        return page;
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_PLAN_ACTION_BY_ID)
    public ResponseEntity<PlanActionDto> getById(@PathVariable UUID id) {
        acces.exigerLePlan(id);
        PlanActionDto planActionDto = planActionService.getPlanActionDtoById(id);
        planActionDto.setWorkflowState(etatWorkflow(id));
        return new ResponseEntity<>(planActionDto, HttpStatus.OK);
    }

    /**
     * Étape courante et actions autorisées du circuit de validation. Une indisponibilité du
     * service de workflow ne doit pas empêcher la consultation du plan d'action.
     */
    private WorkflowStateDto etatWorkflow(UUID resourceId) {
        try {
            return workflowClient.getWorkflowState(resourceId);
        } catch (Exception e) {
            log.warn("État du workflow indisponible pour le plan d'action {} : {}", resourceId, e.getMessage());
            return null;
        }
    }
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(DELETE_PLAN_ACTION)
    public void deleteyId(@PathVariable UUID id) {
        acces.exigerLePlan(id);
        planActionService.delete(id);
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_PLAN_ACTION_RESPONSABLE)
    public ResponseEntity<Page<PlanActionDto>> getAllActionsByResponsbale(@PathVariable String email, @PathVariable StatutEnum status,
            @ParameterObject Pageable pageable) {
        Page<PlanActionDto> planActionDtos = avecEtatDuCircuit(planActionService.planActionByResponsable(email, status, pageable));
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(GET_ALL_PLAN_ACTION_ALL)
    public ResponseEntity<Page<PlanActionDto>> getAllActionsByResponsbaleAll(@PathVariable String email, @ParameterObject Pageable pageable) {
        Page<PlanActionDto> planActionDtos = avecEtatDuCircuit(planActionService.planActionByResponsableAll(email, pageable));
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(path = "/stats/status/{annee}")
    public ResponseEntity<Map<String, Map<String, Map<String, Long>>>> statService(@PathVariable int annee) {
        return ResponseEntity.ok(planActionService.getFrequenceTraitementParMois(annee));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/structure/{structureId}")
    public ResponseEntity<Page<PlanActionDto>> getPlanActionsByStructure(@PathVariable String structureId, @ParameterObject Pageable pageable) {
        Page<PlanActionDto> planActionDtos = avecEtatDuCircuit(planActionService.getPlanActionsByStructure(structureId, pageable));
        return new ResponseEntity<>(planActionDtos, HttpStatus.OK);
    }
}
