package com.qualiapproche.workflow.service;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowValidationRequestDto;
import com.qualiapproche.workflow.dto.WorkflowDto;
import com.qualiapproche.workflow.dto.WorkflowStepDto;
import com.qualiapproche.workflow.dto.WorkflowTransitionDto;
import com.qualiapproche.workflow.model.*;
import com.qualiapproche.workflow.persistence.model.IWorkflowData;
import com.qualiapproche.workflow.persistence.model.TransitionPersistante;
import com.qualiapproche.workflow.persistence.model.WorkflowPersistant;
import com.qualiapproche.workflow.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Service principal gérant les instances de validation de workflow (Quali-SIRA).
 *
 * <p>Ce service hérite de {@link AbstractWorkflowService} pour centraliser le pilotage
 * des instances spécifiques au métier. Il se charge d'initialiser les workflows,
 * de vérifier les champs de saisie requis lors d'une transition et d'enregistrer 
 * les valeurs des champs dynamiques.</p>
 */
@Service
@Slf4j
public class WorkflowService extends AbstractWorkflowService<WorkflowValidationInstance> {

    private final WorkflowRepository workflowRepository;
    private final WorkflowValidationInstanceRepository validationInstanceRepository;
    private final WorkflowStepFieldRepository stepFieldRepository;
    private final WorkflowFieldValueRepository fieldValueRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowStepRepository stepRepository;

    public WorkflowService(
            IWorkflowEnginePort<IWorkflowData, TransitionPersistante, WorkflowPersistant> moteur,
            ValidationHistoryRepository historyRepository,
            ApplicationEventPublisher eventPublisher,
            WorkflowRepository workflowRepository,
            WorkflowValidationInstanceRepository validationInstanceRepository,
            WorkflowStepFieldRepository stepFieldRepository,
            WorkflowFieldValueRepository fieldValueRepository,
            WorkflowTransitionRepository transitionRepository,
            WorkflowStepRepository stepRepository) {
        super(moteur, historyRepository, eventPublisher);
        this.workflowRepository = workflowRepository;
        this.validationInstanceRepository = validationInstanceRepository;
        this.stepFieldRepository = stepFieldRepository;
        this.fieldValueRepository = fieldValueRepository;
        this.transitionRepository = transitionRepository;
        this.stepRepository = stepRepository;
    }

    public List<WorkflowDto> getAllWorkflows() {
        WorkflowMapper mapper = new WorkflowMapper();
        return workflowRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public WorkflowDto createWorkflow(WorkflowDto dto) {
        WorkflowMapper mapper = new WorkflowMapper();
        Workflow workflow = mapper.toEntity(dto);

        // Resolve toStepId for transitions
        resolveTransitions(workflow, dto);

        workflow = workflowRepository.save(workflow);
        reloadEngineCatalogue();
        return mapper.toDto(workflow);
    }

    @Transactional
    public WorkflowDto updateWorkflow(UUID id, WorkflowDto dto) {
        WorkflowMapper mapper = new WorkflowMapper();
        Workflow existing = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow introuvable"));

        existing.setNom(dto.getNom());
        existing.setDescription(dto.getDescription());
        existing.setResourceType(dto.getResourceType());

        mergeSteps(existing, dto);
        resolveTransitions(existing, dto);

        workflowRepository.save(existing);
        reloadEngineCatalogue();
        return mapper.toDto(existing);
    }

    @Transactional
    public void deleteWorkflow(UUID id) {
        workflowRepository.deleteById(id);
        reloadEngineCatalogue();
    }

    /**
     * Le moteur ({@link com.qualiapproche.workflow.core.engine.WorkflowEngine}) charge son
     * catalogue une seule fois au démarrage et ne le recharge que si {@code getCatalogueVersion()}
     * change (toujours null aujourd'hui, cf. WorkflowEngineDAOAdapter). Sans cet appel explicite,
     * créer/modifier/supprimer un workflow via l'API n'a donc aucun effet tant que le service
     * n'est pas redémarré.
     */
    private void reloadEngineCatalogue() {
        try {
            this.moteur.init();
        } catch (com.qualiapproche.workflow.core.exception.WorkflowException e) {
            throw new RuntimeException("Erreur lors du rechargement du catalogue de workflows", e);
        }
    }

    /**
     * Met à jour les étapes existantes en place (par ID) plutôt que de les recréer, afin de
     * préserver les identifiants référencés par les instances de validation en cours
     * ({@code WorkflowValidationInstance.etatCode}). Les étapes retirées ne sont autorisées
     * que si aucune instance active n'y est actuellement rattachée.
     */
    private void mergeSteps(Workflow existing, WorkflowDto dto) {
        List<WorkflowStepDto> incomingSteps = dto.getSteps() != null ? dto.getSteps() : List.of();

        java.util.Set<Long> incomingIds = incomingSteps.stream()
                .map(WorkflowStepDto::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        List<Long> removedStepIds = existing.getSteps().stream()
                .map(WorkflowStep::getId)
                .filter(stepId -> !incomingIds.contains(stepId))
                .toList();

        if (!removedStepIds.isEmpty()) {
            List<String> removedCodes = removedStepIds.stream().map(String::valueOf).toList();
            boolean hasActiveInstances = validationInstanceRepository
                    .existsByEtatCodeInAndStatus(removedCodes, ValidationStatus.EN_COURS);
            if (hasActiveInstances) {
                throw new com.qualiapproche.common.exception.BusinessException(
                        "Impossible de supprimer une ou plusieurs étapes : des dossiers sont actuellement en cours sur ces étapes. "
                                + "Terminez-les ou migrez-les avant de modifier la structure de ce workflow.",
                        org.springframework.http.HttpStatus.CONFLICT);
            }
        }

        Map<Long, WorkflowStep> existingById = existing.getSteps().stream()
                .filter(s -> s.getId() != null)
                .collect(java.util.stream.Collectors.toMap(WorkflowStep::getId, s -> s));
                
        Map<String, WorkflowStep> existingByName = existing.getSteps().stream()
                .filter(s -> s.getNomEtape() != null)
                .collect(java.util.stream.Collectors.toMap(WorkflowStep::getNomEtape, s -> s, (s1, s2) -> s1));

        List<WorkflowStep> merged = new java.util.ArrayList<>();
        java.util.Set<Long> seenStepIds = new java.util.HashSet<>();
        java.util.Set<String> seenStepNames = new java.util.HashSet<>();

        for (WorkflowStepDto stepDto : incomingSteps) {
            if (stepDto.getId() != null && !seenStepIds.add(stepDto.getId())) {
                continue;
            }
            if (stepDto.getNomEtape() != null && !seenStepNames.add(stepDto.getNomEtape())) {
                continue;
            }
            WorkflowStep step = null;
            if (stepDto.getId() != null) {
                step = existingById.get(stepDto.getId());
            }
            if (step == null && stepDto.getNomEtape() != null) {
                step = existingByName.get(stepDto.getNomEtape());
            }
            if (step == null) {
                step = new WorkflowStep();
            }
            step.setNomEtape(stepDto.getNomEtape());
            step.setStepOrder(stepDto.getStepOrder());
            step.setResponsableRole(stepDto.getResponsableRole());
            step.setDescription(stepDto.getDescription());
            step.setEtatTraitement(stepDto.getEtatTraitement());
            step.setEmailTemplateCode(stepDto.getEmailTemplateCode());
            step.setStepTemplateId(stepDto.getStepTemplateId());
            step.setWorkflow(existing);
            mergeTransitions(step, stepDto);
            mergeFields(step, stepDto);
            merged.add(step);
        }

        existing.getSteps().clear();
        existing.getSteps().addAll(merged);
    }

    /**
     * Met à jour les champs de saisie d'une étape.
     *
     * <p>Ils étaient purement et simplement ignorés à la modification : seule la création les
     * enregistrait, si bien qu'un circuit existant ne pouvait plus voir ses champs corrigés,
     * ajoutés ou supprimés. Les champs sont appariés par identifiant, à défaut par nom, pour
     * préserver les identifiants déjà référencés par les valeurs saisies en historique.</p>
     */
    private void mergeFields(WorkflowStep step, WorkflowStepDto stepDto) {
        List<com.qualiapproche.workflow.dto.WorkflowStepFieldDto> incoming =
                stepDto.getFields() != null ? stepDto.getFields() : List.of();

        Map<Long, WorkflowStepField> existingById = step.getFields().stream()
                .filter(f -> f.getId() != null)
                .collect(java.util.stream.Collectors.toMap(WorkflowStepField::getId, f -> f));

        Map<String, WorkflowStepField> existingByName = step.getFields().stream()
                .filter(f -> f.getFieldName() != null)
                .collect(java.util.stream.Collectors.toMap(WorkflowStepField::getFieldName, f -> f, (f1, f2) -> f1));

        List<WorkflowStepField> merged = new java.util.ArrayList<>();
        java.util.Set<String> seenNames = new java.util.HashSet<>();

        for (com.qualiapproche.workflow.dto.WorkflowStepFieldDto fieldDto : incoming) {
            if (fieldDto.getFieldName() != null && !seenNames.add(fieldDto.getFieldName())) {
                continue;
            }

            WorkflowStepField field = null;
            if (fieldDto.getId() != null) {
                field = existingById.get(fieldDto.getId());
            }
            if (field == null && fieldDto.getFieldName() != null) {
                field = existingByName.get(fieldDto.getFieldName());
            }
            if (field == null) {
                field = new WorkflowStepField();
            }

            field.setFieldName(fieldDto.getFieldName());
            field.setFieldLabel(fieldDto.getFieldLabel());
            field.setType(typeDeChamp(fieldDto.getType()));
            field.setRequired(fieldDto.isRequired());
            field.setOptions(fieldDto.getOptions());
            field.setStep(step);
            merged.add(field);
        }

        step.getFields().clear();
        step.getFields().addAll(merged);
    }

    /** Type de champ, signalé en 400 explicite plutôt qu'en erreur serveur si le libellé est inconnu. */
    private FieldType typeDeChamp(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return FieldType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Type de champ inconnu : '" + type + "'. Valeurs acceptées : "
                            + java.util.Arrays.toString(FieldType.values()) + ".",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    private void mergeTransitions(WorkflowStep step, WorkflowStepDto stepDto) {
        List<WorkflowTransitionDto> incoming = stepDto.getTransitions() != null ? stepDto.getTransitions() : List.of();
        Map<Long, WorkflowTransition> existingById = step.getTransitions().stream()
                .filter(t -> t.getId() != null)
                .collect(java.util.stream.Collectors.toMap(WorkflowTransition::getId, t -> t));
        
        Map<StepDecision, WorkflowTransition> existingByDecision = step.getTransitions().stream()
                .filter(t -> t.getDecision() != null)
                .collect(java.util.stream.Collectors.toMap(WorkflowTransition::getDecision, t -> t, (t1, t2) -> t1));

        List<WorkflowTransition> merged = new java.util.ArrayList<>();
        java.util.Set<StepDecision> seenDecisions = new java.util.HashSet<>();
        
        for (WorkflowTransitionDto transitionDto : incoming) {
            StepDecision decision = transitionDto.getDecision() != null ? StepDecision.valueOf(transitionDto.getDecision()) : null;
            if (decision != null && !seenDecisions.add(decision)) {
                continue;
            }
            
            WorkflowTransition transition = null;
            if (transitionDto.getId() != null) {
                transition = existingById.get(transitionDto.getId());
            }
            if (transition == null && decision != null) {
                transition = existingByDecision.get(decision);
            }
            
            if (transition == null) {
                transition = new WorkflowTransition();
            }
            transition.setDecision(decision);
            transition.setRequiredRole(transitionDto.getRequiredRole());
            transition.setLabel(transitionDto.getLabel());
            transition.setFromStep(step);
            merged.add(transition);
        }

        step.getTransitions().clear();
        step.getTransitions().addAll(merged);
    }

    /**
     * Rattache chaque transition à son étape de destination.
     *
     * <p>Deux défauts corrigés ici. D'une part la destination n'était cherchée que si
     * {@code toStepId} était fourni : à la création d'un circuit, les étapes n'ont pas encore
     * d'identifiant, si bien qu'<b>aucune</b> transition ne pouvait recevoir de destination et que
     * tout le circuit se terminait dès la première décision. La résolution accepte donc aussi le
     * nom et le rang de l'étape, les deux clés dont dispose l'appelant avant enregistrement.</p>
     *
     * <p>D'autre part l'appariement entre entités et DTO se faisait par position, alors que
     * {@code mergeSteps} écarte au passage les doublons : les deux listes pouvaient diverger et
     * décaler les destinations. L'appariement se fait maintenant par identité d'étape.</p>
     */
    private void resolveTransitions(Workflow workflow, WorkflowDto dto) {
        if (workflow.getSteps() == null || dto.getSteps() == null) {
            return;
        }

        for (WorkflowStep step : workflow.getSteps()) {
            WorkflowStepDto stepDto = dtoDeLEtape(dto, step);
            if (stepDto == null || step.getTransitions() == null || stepDto.getTransitions() == null) {
                continue;
            }

            for (WorkflowTransition transition : step.getTransitions()) {
                WorkflowTransitionDto transitionDto = dtoDeLaTransition(stepDto, transition);
                if (transitionDto != null) {
                    transition.setToStep(etapeDestination(workflow, transitionDto));
                }
            }
        }
    }

    private WorkflowStepDto dtoDeLEtape(WorkflowDto dto, WorkflowStep step) {
        return dto.getSteps().stream()
                .filter(s -> (step.getId() != null && step.getId().equals(s.getId()))
                        || (step.getNomEtape() != null && step.getNomEtape().equals(s.getNomEtape())))
                .findFirst()
                .orElse(null);
    }

    private WorkflowTransitionDto dtoDeLaTransition(WorkflowStepDto stepDto, WorkflowTransition transition) {
        return stepDto.getTransitions().stream()
                .filter(t -> (transition.getId() != null && transition.getId().equals(t.getId()))
                        || (transition.getDecision() != null && t.getDecision() != null
                                && transition.getDecision().name().equals(t.getDecision())))
                .findFirst()
                .orElse(null);
    }

    /** Destination désignée par identifiant, à défaut par nom d'étape, à défaut par rang. */
    private WorkflowStep etapeDestination(Workflow workflow, WorkflowTransitionDto transitionDto) {
        if (transitionDto.getToStepId() != null) {
            WorkflowStep parId = workflow.getSteps().stream()
                    .filter(s -> transitionDto.getToStepId().equals(s.getId()))
                    .findFirst().orElse(null);
            if (parId != null) {
                return parId;
            }
        }
        if (transitionDto.getToStepName() != null) {
            WorkflowStep parNom = workflow.getSteps().stream()
                    .filter(s -> transitionDto.getToStepName().equals(s.getNomEtape()))
                    .findFirst().orElse(null);
            if (parNom != null) {
                return parNom;
            }
        }
        if (transitionDto.getToStepOrder() != null) {
            return workflow.getSteps().stream()
                    .filter(s -> transitionDto.getToStepOrder() == s.getStepOrder())
                    .findFirst().orElse(null);
        }
        // Aucune destination désignée : transition terminale, l'état de fin est synthétisé.
        return null;
    }

    @Override
    protected JpaRepository<WorkflowValidationInstance, UUID> getRepository() {
        return this.validationInstanceRepository;
    }

    @Override
    protected String getWorkflowCode() {
        // Can handle multiple, but for generic use, it's determined per-instance in `initialiser(D, code)`
        return null;
    }

    @Override
    protected String getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    /**
     * Retourne des DTO et non les entités JPA : l'entité sérialise la destination d'une transition
     * sous forme d'objet {@code toStep} imbriqué, sans {@code toStepId} ni {@code toStepOrder},
     * donnant une forme différente de celle de {@code GET /workflows} pour la même ressource.
     */
    @Transactional(readOnly = true)
    public List<WorkflowDto> getWorkflowsByType(String documentType) {
        WorkflowMapper mapper = new WorkflowMapper();
        return workflowRepository.findByResourceType(documentType).stream().map(mapper::toDto).toList();
    }

    /**
     * Sélection déterministe du workflow actif pour un type de ressource, au lieu de laisser
     * chaque service métier prendre arbitrairement le premier élément d'une liste dont l'ordre
     * n'est pas garanti.
     */
    public WorkflowDto getActiveWorkflowByType(String documentType) {
        List<Workflow> actifs = workflowRepository.findByResourceTypeAndActifTrue(documentType);
        if (actifs.isEmpty()) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Aucun workflow actif n'est configuré pour le type '" + documentType + "'.",
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        if (actifs.size() > 1) {
            log.warn("Plusieurs workflows actifs trouvés pour le type '{}' ({}). Le premier est utilisé, "
                    + "mais un seul devrait être marqué actif.", documentType, actifs.size());
        }
        return new WorkflowMapper().toDto(actifs.get(0));
    }

    /**
     * Même forme que {@code GET /workflows} (DTO, avec {@code toStepId} / {@code toStepOrder}),
     * et 404 explicite : renvoyer {@code null} produisait un 200 au corps vide, que l'appelant ne
     * pouvait pas distinguer d'un circuit réellement vide.
     */
    @Transactional(readOnly = true)
    public WorkflowDto getWorkflowById(UUID workflowId) {
        return workflowRepository.findById(workflowId)
                .map(new WorkflowMapper()::toDto)
                .orElseThrow(() -> new com.qualiapproche.common.exception.BusinessException(
                        "Circuit de validation introuvable.", org.springframework.http.HttpStatus.NOT_FOUND));
    }

    private WorkflowValidationInstance findLastValidationInstanceEntity(UUID resourceId) {
        return validationInstanceRepository.findTopByResourceIdOrderByStartedAtDesc(resourceId.toString()).orElse(null);
    }

    /**
     * Vue typée et cohérente de la dernière instance de validation, exposée aux autres
     * microservices. Remplace l'exposition directe de l'entité JPA (qui portait "workflowCode"
     * plutôt que "workflowId" : support-service lisait silencieusement une clé inexistante et
     * relançait systématiquement en brouillon au lieu de reprendre le circuit précédent).
     */
    @Transactional(readOnly = true)
    public WorkflowInstanceDto getLastValidationInstance(UUID resourceId) {
        WorkflowValidationInstance instance = findLastValidationInstanceEntity(resourceId);
        return instance != null ? toInstanceDto(instance) : null;
    }

    private WorkflowInstanceDto toInstanceDto(WorkflowValidationInstance instance) {
        // Résolution best-effort du libellé humain : à défaut d'étape trouvée, le code sert de libellé
        // (comportement déjà utilisé par initiateWorkflow avant ce correctif).
        String currentStateName = instance.getEtat() != null ? instance.getEtat().getLibelle() : instance.getEtatCode();

        UUID workflowId = null;
        try {
            workflowId = UUID.fromString(instance.getWorkflowCode());
        } catch (Exception ignored) {
            // workflowCode n'est pas (ou plus) un UUID exploitable ; on laisse workflowId à null plutôt que d'échouer.
        }

        return new WorkflowInstanceDto(
                instance.getId(),
                workflowId,
                instance.getStatus().name(),
                instance.getEtatCode(),
                currentStateName
        );
    }

    @Transactional(readOnly = true)
    public com.qualiapproche.workflow.dto.WorkflowStateDto getWorkflowStateForResource(UUID resourceId) {
        WorkflowValidationInstance instanceInfo = findLastValidationInstanceEntity(resourceId);
        if (instanceInfo == null) {
            return null;
        }

        WorkflowValidationInstance instance = fromDatabase(instanceInfo.getId());
        java.util.Set<TransitionPersistante> transitions = getTransitionsPossibles(instance.getId());

        List<com.qualiapproche.workflow.dto.WorkflowStateDto.WorkflowActionDto> actions = transitions.stream()
                .map(t -> com.qualiapproche.workflow.dto.WorkflowStateDto.WorkflowActionDto.builder()
                        .code(t.getCode())
                        .libelle(t.getLibelle())
                        .permission(t.getPermission())
                        .decision(decisionDeTransition(t.getCode()))
                        .build())
                .toList();

        return com.qualiapproche.workflow.dto.WorkflowStateDto.builder()
                .instanceId(instance.getId())
                .status(instance.getStatus().name())
                .currentStateCode(instance.getEtatCode())
                .currentStateName(instance.getEtat() != null ? instance.getEtat().getLibelle() : null)
                .allowedActions(actions)
                .currentStepFields(champsDeLEtape(instance.getEtatCode()))
                .build();
    }

    private String decisionDeTransition(String transitionCode) {
        try {
            return transitionRepository.findById(Long.valueOf(transitionCode))
                    .map(t -> t.getDecision() != null ? t.getDecision().name() : null)
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Champs de saisie exigés par l'étape courante, pour que l'appelant puisse composer le
     * formulaire de décision. Vide sur un état terminal, qui ne correspond à aucune étape.
     */
    private List<com.qualiapproche.workflow.dto.WorkflowStepFieldDto> champsDeLEtape(String etatCode) {
        WorkflowMapper mapper = new WorkflowMapper();
        try {
            return stepRepository.findById(Long.valueOf(etatCode))
                    .map(step -> step.getFields().stream().map(mapper::toDto).toList())
                    .orElseGet(List::of);
        } catch (NumberFormatException e) {
            return List.of();
        }
    }

    /**
     * États de plusieurs ressources en un appel : afficher une liste de N documents imposait
     * jusqu'ici N requêtes. Les ressources sans circuit sont simplement absentes du résultat.
     */
    @Transactional(readOnly = true)
    public Map<UUID, com.qualiapproche.workflow.dto.WorkflowStateDto> getWorkflowStatesForResources(List<UUID> resourceIds) {
        Map<UUID, com.qualiapproche.workflow.dto.WorkflowStateDto> etats = new java.util.LinkedHashMap<>();
        if (resourceIds == null) {
            return etats;
        }
        for (UUID resourceId : resourceIds) {
            com.qualiapproche.workflow.dto.WorkflowStateDto etat = getWorkflowStateForResource(resourceId);
            if (etat != null) {
                etats.put(resourceId, etat);
            }
        }
        return etats;
    }

    /**
     * Traçabilité du circuit d'une ressource : décisions successives, auteurs, commentaires et
     * valeurs saisies. Ces enregistrements existaient sans aucun point d'entrée pour les relire.
     */
    @Transactional(readOnly = true)
    public List<com.qualiapproche.workflow.dto.ValidationHistoryDto> getValidationHistory(UUID resourceId) {
        WorkflowValidationInstance instance = findLastValidationInstanceEntity(resourceId);
        if (instance == null) {
            return List.of();
        }
        return historyRepository.findByValidationInstance_IdOrderByDecisionDateAsc(instance.getId()).stream()
                .map(this::toHistoryDto)
                .toList();
    }

    private com.qualiapproche.workflow.dto.ValidationHistoryDto toHistoryDto(ValidationHistory history) {
        List<com.qualiapproche.workflow.dto.ValidationHistoryDto.FieldValueDto> valeurs =
                history.getFieldValues() == null ? List.of()
                        : history.getFieldValues().stream()
                                .map(v -> com.qualiapproche.workflow.dto.ValidationHistoryDto.FieldValueDto.builder()
                                        .fieldCode(v.getFieldCode())
                                        .fieldName(v.getFieldName())
                                        .value(v.getValue())
                                        .build())
                                .toList();

        return com.qualiapproche.workflow.dto.ValidationHistoryDto.builder()
                .id(history.getId())
                .stepCode(history.getStepCode())
                .stepName(history.getStepName())
                .decision(history.getDecision())
                .comments(history.getComments())
                .validatorUserId(history.getValidatorUserId())
                .decisionDate(history.getDecisionDate())
                .fieldValues(valeurs)
                .build();
    }

    @Transactional
    public WorkflowInstanceDto initiateWorkflow(UUID resourceId, String resourceType, UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow introuvable"));

        WorkflowValidationInstance instance = WorkflowValidationInstance.builder()
                .resourceId(resourceId.toString())
                .resourceType(resourceType)
                .status(ValidationStatus.EN_COURS)
                .startedAt(LocalDateTime.now())
                .build();

        WorkflowValidationInstance saved = this.initialiser(instance, workflow.getId().toString());
        return toInstanceDto(saved);
    }

    @Transactional
    public void validateStep(UUID resourceId, String userId, WorkflowValidationRequestDto request) {
        processStepDecision(resourceId, userId, request, StepDecision.APPROUVE);
    }

    @Transactional
    public void rejectStep(UUID resourceId, String userId, WorkflowValidationRequestDto request) {
        processStepDecision(resourceId, userId, request, StepDecision.REJETE);
    }

    @Transactional
    public void executeDynamicTransition(UUID resourceId, String userId, String transitionCode, WorkflowValidationRequestDto request) {
        WorkflowValidationInstance instance = validationInstanceRepository
                .findTopByResourceIdAndStatusOrderByStartedAtDesc(resourceId.toString(), ValidationStatus.EN_COURS)
                .orElseThrow(() -> new com.qualiapproche.common.exception.BusinessException(
                        "Aucun circuit de validation en cours pour cette ressource.",
                        org.springframework.http.HttpStatus.NOT_FOUND));

        verifierEtapeAttendue(instance, request);
        verifierChampsRequis(instance, request);

        String comments = request != null ? request.getComments() : null;

        // Perform transition using abstract service directly with the transition code
        WorkflowValidationInstance updatedInstance = this.executerTransition(instance.getId(), transitionCode, comments);

        attachHistoryFields(updatedInstance, request);
    }

    private void processStepDecision(UUID resourceId, String userId, WorkflowValidationRequestDto request, StepDecision decision) {
        WorkflowValidationInstance instance = validationInstanceRepository
                .findTopByResourceIdAndStatusOrderByStartedAtDesc(resourceId.toString(), ValidationStatus.EN_COURS)
                .orElseThrow(() -> new com.qualiapproche.common.exception.BusinessException(
                        "Aucun circuit de validation en cours pour cette ressource.",
                        org.springframework.http.HttpStatus.NOT_FOUND));

        verifierEtapeAttendue(instance, request);
        verifierChampsRequis(instance, request);

        String comments = request != null ? request.getComments() : null;

        WorkflowValidationInstance updatedInstance =
                this.executerTransition(instance.getId(), resoudreCodeTransition(instance, decision), comments);

        attachHistoryFields(updatedInstance, request);
    }

    /**
     * Rejette une décision prise depuis un écran périmé — et, du même coup, le second envoi d'un
     * double clic : la première demande a déjà changé l'étape courante, la seconde ne correspond
     * donc plus à l'étape attendue.
     *
     * <p>Le contrôle ne s'applique que si l'appelant transmet {@code expectedStateCode}. Les
     * franchissements concurrents qui passeraient malgré tout restent arbitrés par le verrouillage
     * optimiste de l'instance ({@code @Version}), qui répond également en 409.</p>
     */
    private void verifierEtapeAttendue(WorkflowValidationInstance instance, WorkflowValidationRequestDto request) {
        String attendu = request != null ? request.getExpectedStateCode() : null;
        if (attendu == null || attendu.isBlank()) {
            return;
        }
        if (!attendu.equals(instance.getEtatCode())) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Ce dossier a changé d'étape entre-temps. Rechargez-le avant de décider à nouveau.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }
    }

    /**
     * Contrôle que tous les champs obligatoires de l'étape courante sont renseignés.
     *
     * <p>La vérification n'existait que sur les champs effectivement transmis, et intervenait après
     * le franchissement : un champ requis simplement omis passait sans erreur, et la transition
     * était déjà jouée quand la validation échouait. Elle a donc lieu ici, avant toute exécution.</p>
     */
    private void verifierChampsRequis(WorkflowValidationInstance instance, WorkflowValidationRequestDto request) {
        Long stepId;
        try {
            stepId = Long.valueOf(instance.getEtatCode());
        } catch (NumberFormatException e) {
            return; // état terminal : aucune saisie attendue
        }

        List<WorkflowStepField> requis = stepFieldRepository.findByStepIdAndIsRequiredTrue(stepId);
        if (requis.isEmpty()) {
            return;
        }

        Map<Long, String> saisis = request != null && request.getFields() != null
                ? request.getFields() : Map.of();

        List<String> manquants = requis.stream()
                .filter(field -> {
                    String valeur = saisis.get(field.getId());
                    return valeur == null || valeur.isBlank();
                })
                .map(WorkflowStepField::getFieldLabel)
                .toList();

        if (!manquants.isEmpty()) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Champ(s) obligatoire(s) non renseigné(s) : " + String.join(", ", manquants) + ".",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Traduit une décision métier (APPROUVE / REJETE) en code de transition compris par le moteur.
     *
     * <p>Le moteur identifie une transition par son identifiant en base
     * (cf. {@code WorkflowEngineDAOAdapter}), alors que ce service raisonnait en
     * {@code decision.name()} : {@code getTransitionByCode} ne trouvait jamais de correspondance
     * et {@code /validate} comme {@code /reject} échouaient systématiquement sur
     * « La transition APPROUVE est inconnue ». La décision est donc résolue ici en transition
     * réelle, à partir de l'étape courante de l'instance.</p>
     */
    private String resoudreCodeTransition(WorkflowValidationInstance instance, StepDecision decision) {
        Long stepId;
        try {
            stepId = Long.valueOf(instance.getEtatCode());
        } catch (NumberFormatException e) {
            // Les états terminaux synthétiques (TERMINATED_*) ne portent pas d'étape : plus rien à décider.
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Le circuit de validation de cette ressource est terminé : aucune décision n'est possible.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }

        return transitionRepository.findByFromStepIdAndDecision(stepId, decision)
                .map(transition -> transition.getId().toString())
                .orElseThrow(() -> new com.qualiapproche.common.exception.BusinessException(
                        "L'action « " + decision.name() + " » n'est pas prévue à l'étape courante de ce circuit.",
                        org.springframework.http.HttpStatus.CONFLICT));
    }

    private void attachHistoryFields(WorkflowValidationInstance updatedInstance, WorkflowValidationRequestDto request) {
        // Fields can be attached to history inside an event listener, or here.
        // For now, attach here if provided.
        // Needs a slight refactor to attach values to the latest history entry properly
        if (request != null && request.getFields() != null) {
            // Find latest history for this instance
            ValidationHistory lastHistory = historyRepository.findTopByValidationInstanceOrderByDecisionDateDesc(updatedInstance)
                .orElse(null);

            if (lastHistory != null) {
                for (Map.Entry<Long, String> entry : request.getFields().entrySet()) {
                    WorkflowStepField field = stepFieldRepository.findById(entry.getKey())
                            .orElseThrow(() -> new RuntimeException("Champ introuvable: " + entry.getKey()));

                    if (field.isRequired() && (entry.getValue() == null || entry.getValue().trim().isEmpty())) {
                        throw new RuntimeException("Le champ " + field.getFieldName() + " est requis.");
                    }

                    WorkflowFieldValue fieldValue = WorkflowFieldValue.builder()
                            .history(lastHistory)
                            .fieldCode(field.getId().toString())
                            .fieldName(field.getFieldName())
                            .value(entry.getValue())
                            .build();
                    fieldValueRepository.save(fieldValue);
                }
            }
        }
    }
}
