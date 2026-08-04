package com.qualiapproche.workflow.service;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.core.port.input.IWorkflowEnginePort;
import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowValidationRequestDto;
import com.qualiapproche.workflow.dto.WorkflowDto;
import com.qualiapproche.workflow.dto.WorkflowStepDto;
import com.qualiapproche.workflow.dto.WorkflowTransitionDto;
import com.qualiapproche.workflow.event.CatalogueWorkflowModifieEvent;
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
        workflow.setResourceType(TypeRessource.normaliser(dto.getResourceType()));

        attribuerCodesEtapes(workflow);
        resolveTransitions(workflow, dto);
        horodaterModification(workflow);

        workflow = workflowRepository.save(workflow);
        reloadEngineCatalogue();
        return mapper.toDto(workflow);
    }

    @Transactional
    public WorkflowDto updateWorkflow(UUID id, WorkflowDto dto) {
        WorkflowMapper mapper = new WorkflowMapper();
        Workflow existing = workflowRepository.findById(id)
                .orElseThrow(() -> new com.qualiapproche.common.exception.BusinessException(
                        "Circuit de validation introuvable.", org.springframework.http.HttpStatus.NOT_FOUND));

        existing.setNom(dto.getNom());
        existing.setDescription(dto.getDescription());
        existing.setResourceType(TypeRessource.normaliser(dto.getResourceType()));

        mergeSteps(existing, dto);
        resolveTransitions(existing, dto);
        horodaterModification(existing);

        workflowRepository.save(existing);
        reloadEngineCatalogue();
        return mapper.toDto(existing);
    }

    /**
     * Supprime un circuit, à condition qu'aucun dossier ne soit en cours dessus.
     *
     * <p>La suppression était inconditionnelle, alors même que {@code mergeSteps} refuse déjà de
     * retirer une simple étape portant des instances actives. L'incohérence était coûteuse :
     * supprimer un circuit laissait ses instances en cours définitivement inexploitables — le
     * moteur ne peut plus résoudre leur workflow, et toute lecture d'état comme toute décision
     * échouait dès lors en erreur serveur, sans issue possible pour l'utilisateur.</p>
     */
    @Transactional
    public void deleteWorkflow(UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new com.qualiapproche.common.exception.BusinessException(
                        "Circuit de validation introuvable.", org.springframework.http.HttpStatus.NOT_FOUND));

        List<String> codesEtapes = workflow.getSteps().stream()
                .map(WorkflowStep::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();

        if (!codesEtapes.isEmpty()
                && validationInstanceRepository.existsByEtatCodeInAndStatus(codesEtapes, ValidationStatus.EN_COURS)) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Impossible de supprimer ce circuit : des dossiers y sont actuellement en cours. "
                            + "Terminez-les ou migrez-les avant de le supprimer.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }

        workflowRepository.delete(workflow);
        reloadEngineCatalogue();
    }

    /**
     * Demande le rechargement du catalogue du moteur, <b>une fois la transaction committée</b>.
     *
     * <p>Le moteur ({@link com.qualiapproche.workflow.core.engine.WorkflowEngine}) ne recharge
     * de lui-même que si la signature de la source change ; sans cette demande, créer ou modifier
     * un circuit resterait sans effet sur l'instance qui a reçu l'appel jusqu'à ce qu'elle
     * constate le changement.</p>
     *
     * <p>Le rechargement avait lieu au sein même de la transaction : le moteur lisait alors des
     * données non encore committées et mémorisait leur signature. Une transaction annulée ensuite
     * — contrainte violée, panne — lui laissait un catalogue décrivant des circuits qui
     * n'existaient pas, et la signature mémorisée correspondant à cet état fantôme, plus rien ne
     * venait le corriger avant la modification suivante. Passer par un événement d'après commit
     * garantit que le moteur ne recharge que ce qui est réellement enregistré.</p>
     */
    private void reloadEngineCatalogue() {
        eventPublisher.publishEvent(new CatalogueWorkflowModifieEvent(this));
    }

    /**
     * Recharge le catalogue du moteur après le commit de la modification.
     *
     * <p>Un échec est journalisé sans être propagé : la transaction est déjà acquise, et le
     * moteur rattrapera de toute façon le changement au prochain contrôle de signature. Lever ici
     * ne ferait que remonter une erreur à un appelant dont la demande a, elle, abouti.</p>
     */
    @org.springframework.transaction.event.TransactionalEventListener(
            phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void rechargerCatalogueApresCommit(CatalogueWorkflowModifieEvent event) {
        try {
            this.moteur.init();
        } catch (com.qualiapproche.workflow.core.exception.WorkflowException e) {
            log.error("Rechargement du catalogue de workflows en échec après modification. "
                    + "Le moteur le reprendra au prochain contrôle de signature.", e);
        }
    }

    /**
     * Horodate la modification du circuit pour que la signature du catalogue s'en trouve changée.
     *
     * <p>La signature repose sur {@code max(updateAt)} des circuits, et {@code @PreUpdate} ne se
     * déclenche que si la ligne du circuit est elle-même modifiée. Or l'essentiel des changements
     * ne portent que sur les étapes, les transitions ou les champs : la ligne parente restait
     * intacte, la signature aussi, et les autres instances continuaient à servir un catalogue
     * périmé. Horodater explicitement fait de {@code updateAt} le repère de la dernière mutation
     * du circuit, quelle qu'en soit la profondeur.</p>
     */
    private void horodaterModification(Workflow workflow) {
        workflow.setUpdateAt(LocalDateTime.now());
    }

    /**
     * Attribue à chaque étape son code fonctionnel et en garantit l'unicité au sein du circuit.
     *
     * <p>Le code fourni par l'appelant fait foi ; à défaut il est dérivé du nom de l'étape. Un
     * doublon est refusé plutôt que corrigé en silence : deux étapes partageant un code rendraient
     * toute destination de transition ambiguë.</p>
     */
    private void attribuerCodesEtapes(Workflow workflow) {
        if (workflow.getSteps() == null) {
            return;
        }

        java.util.Set<String> codesUtilises = new java.util.HashSet<>();

        // Les étapes déjà enregistrées gardent leur code — il est immuable — et le réservent.
        for (WorkflowStep step : workflow.getSteps()) {
            if (step.getId() != null && step.getCode() != null && !step.getCode().isBlank()) {
                codesUtilises.add(step.getCode());
            }
        }

        for (WorkflowStep step : workflow.getSteps()) {
            if (codesUtilises.contains(step.getCode()) && step.getId() != null) {
                continue;
            }
            String souhaite = step.getCode() != null && !step.getCode().isBlank()
                    ? normaliserCode(step.getCode())
                    : normaliserCode(step.getNomEtape());

            // Suffixé plutôt que refusé : la même entrée de catalogue peut légitimement servir
            // deux fois dans un circuit — deux vérifications successives, par exemple — et le
            // code venu du catalogue n'est qu'une valeur par défaut, pas une identité imposée.
            String code = codeDisponible(souhaite, codesUtilises);
            codesUtilises.add(code);
            step.setCode(code);
        }
    }

    /** Normalise un code : majuscules, séparateurs réduits au souligné, longueur bornée. */
    private String normaliserCode(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return "ETAPE";
        }
        String normalise = java.text.Normalizer.normalize(valeur, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
        if (normalise.isBlank()) {
            return "ETAPE";
        }
        return normalise.length() > 60 ? normalise.substring(0, 60) : normalise;
    }

    private String codeDisponible(String base, java.util.Set<String> dejaPris) {
        if (!dejaPris.contains(base)) {
            return base;
        }
        for (int suffixe = 2; ; suffixe++) {
            String candidat = base + "_" + suffixe;
            if (!dejaPris.contains(candidat)) {
                return candidat;
            }
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

        // Rapprochement par code fonctionnel et non plus par nom : le nom est un libellé, et le
        // renommer d'une étape suffisait à la faire passer pour une nouvelle.
        Map<String, WorkflowStep> existingByCode = existing.getSteps().stream()
                .filter(s -> s.getCode() != null)
                .collect(java.util.stream.Collectors.toMap(WorkflowStep::getCode, s -> s, (s1, s2) -> s1));

        List<WorkflowStep> merged = new java.util.ArrayList<>();
        java.util.Set<Long> seenStepIds = new java.util.HashSet<>();
        /** Étapes existantes déjà reprises, pour qu'aucune ne soit rattachée deux fois. */
        java.util.Set<Long> dejaRattachees = new java.util.HashSet<>();

        for (WorkflowStepDto stepDto : incomingSteps) {
            if (stepDto.getId() != null && !seenStepIds.add(stepDto.getId())) {
                continue;
            }
            String codeDemande = stepDto.getCode() != null && !stepDto.getCode().isBlank()
                    ? normaliserCode(stepDto.getCode()) : null;

            WorkflowStep step = null;
            if (stepDto.getId() != null) {
                step = existingById.get(stepDto.getId());
            }
            if (step == null && codeDemande != null) {
                // Une étape déjà rattachée à une entrée précédente ne peut pas l'être une seconde
                // fois : sans ce garde, ajouter une étape reprenant le code d'une étape existante
                // les fusionnait en une seule, écrasant son libellé et la dupliquant dans la liste.
                WorkflowStep candidat = existingByCode.get(codeDemande);
                if (candidat != null && !dejaRattachees.contains(candidat.getId())) {
                    step = candidat;
                }
            }
            if (step == null) {
                // Étape nouvelle : son code sera suffixé s'il est déjà pris dans ce circuit.
                step = new WorkflowStep();
            } else {
                dejaRattachees.add(step.getId());
            }

            verifierCodeImmuable(step, codeDemande);
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
        // Les étapes ajoutées lors de cette modification n'ont pas encore de code.
        attribuerCodesEtapes(existing);
    }

    /**
     * Interdit la réécriture du code d'une étape déjà enregistrée.
     *
     * <p>Le code est l'identité fonctionnelle de l'étape : les transitions des autres étapes le
     * désignent, et les instances en cours s'y rapportent. Le laisser changer reviendrait à
     * rompre silencieusement ces rattachements. Une étape nouvelle, elle, reçoit le code demandé.</p>
     */
    private void verifierCodeImmuable(WorkflowStep step, String codeDemande) {
        if (step.getId() == null) {
            step.setCode(codeDemande);
            return;
        }
        if (codeDemande != null && step.getCode() != null && !codeDemande.equals(step.getCode())) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Le code de l'étape « " + step.getNomEtape() + " » ne peut pas être modifié ("
                            + step.getCode() + " → " + codeDemande + "). Créez une nouvelle étape "
                            + "si le circuit doit changer.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }
        if (step.getCode() == null) {
            step.setCode(codeDemande);
        }
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

    /**
     * Destination désignée par code d'étape, à défaut par identifiant, à défaut par rang.
     *
     * <p>Le code prime : c'est la seule clé à la fois stable dans le temps et connue de l'appelant
     * avant enregistrement. Le nom n'est plus une clé de résolution — le corriger cassait
     * silencieusement toutes les transitions qui pointaient vers l'étape. Le rang ne subsiste que
     * pour les appelants qui n'ont pas encore basculé sur le code, et il se décale dès qu'on
     * réordonne le circuit.</p>
     */
    private WorkflowStep etapeDestination(Workflow workflow, WorkflowTransitionDto transitionDto) {
        if (transitionDto.getToStepCode() != null && !transitionDto.getToStepCode().isBlank()) {
            String code = normaliserCode(transitionDto.getToStepCode());
            WorkflowStep parCode = workflow.getSteps().stream()
                    .filter(s -> code.equals(s.getCode()))
                    .findFirst().orElse(null);
            if (parCode != null) {
                return parCode;
            }
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Aucune étape de ce circuit ne porte le code « " + code + " », "
                            + "désigné comme destination d'une transition.",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (transitionDto.getToStepId() != null) {
            WorkflowStep parId = workflow.getSteps().stream()
                    .filter(s -> transitionDto.getToStepId().equals(s.getId()))
                    .findFirst().orElse(null);
            if (parId != null) {
                return parId;
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

    /**
     * État de validation d'une ressource : étape courante, actions offertes et champs à saisir.
     *
     * <p>Le dossier n'est plus lu qu'une fois. Il l'était deux fois — une première pour
     * l'instance, une seconde par {@code getTransitionsPossibles(UUID)} — et chaque transition
     * offerte donnait ensuite lieu à sa propre requête pour retrouver sa décision.</p>
     */
    @Transactional(readOnly = true)
    public com.qualiapproche.workflow.dto.WorkflowStateDto getWorkflowStateForResource(UUID resourceId) {
        WorkflowValidationInstance instance = findLastValidationInstanceEntity(resourceId);
        if (instance == null) {
            return null;
        }

        rattacherEtat(instance);
        java.util.Set<TransitionPersistante> transitions = transitionsPossiblesDe(instance);

        return construireEtat(instance, transitions,
                decisionsDesTransitions(List.of(transitions)),
                champsDesEtapes(List.of(instance)));
    }

    /**
     * Assemble la vue d'état à partir d'éléments déjà résolus, sans aucun accès à la base.
     */
    private com.qualiapproche.workflow.dto.WorkflowStateDto construireEtat(
            WorkflowValidationInstance instance,
            java.util.Set<TransitionPersistante> transitions,
            Map<Long, String> decisions,
            Map<Long, List<com.qualiapproche.workflow.dto.WorkflowStepFieldDto>> champs) {

        List<com.qualiapproche.workflow.dto.WorkflowStateDto.WorkflowActionDto> actions = transitions.stream()
                .map(t -> com.qualiapproche.workflow.dto.WorkflowStateDto.WorkflowActionDto.builder()
                        .code(t.getCode())
                        .libelle(t.getLibelle())
                        .permission(t.getPermission())
                        .decision(decisions.get(identifiantNumerique(t.getCode())))
                        .build())
                .toList();

        Long etapeCourante = identifiantNumerique(instance.getEtatCode());

        return com.qualiapproche.workflow.dto.WorkflowStateDto.builder()
                .instanceId(instance.getId())
                .status(instance.getStatus().name())
                .currentStateCode(instance.getEtatCode())
                .currentStateName(instance.getEtat() != null ? instance.getEtat().getLibelle() : null)
                .allowedActions(actions)
                .currentStepFields(etapeCourante != null
                        ? champs.getOrDefault(etapeCourante, List.of()) : List.of())
                .build();
    }

    /**
     * Décision portée par chacune des transitions citées, en une seule requête.
     *
     * <p>Elles étaient résolues une par une : afficher une liste de dossiers offrant chacun
     * plusieurs actions multipliait d'autant les allers-retours.</p>
     */
    private Map<Long, String> decisionsDesTransitions(
            java.util.Collection<java.util.Set<TransitionPersistante>> lots) {
        java.util.Set<Long> identifiants = lots.stream()
                .flatMap(java.util.Set::stream)
                .map(t -> identifiantNumerique(t.getCode()))
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (identifiants.isEmpty()) {
            return Map.of();
        }
        return transitionRepository.findAllById(identifiants).stream()
                .filter(t -> t.getDecision() != null)
                .collect(java.util.stream.Collectors.toMap(
                        WorkflowTransition::getId, t -> t.getDecision().name()));
    }

    /**
     * Champs de saisie des étapes courantes, en une seule requête, leurs champs compris.
     *
     * <p>Une étape terminale ne correspond à aucune ligne : elle est simplement absente du
     * résultat, et l'appelant obtient une liste vide.</p>
     */
    private Map<Long, List<com.qualiapproche.workflow.dto.WorkflowStepFieldDto>> champsDesEtapes(
            java.util.Collection<WorkflowValidationInstance> instances) {
        java.util.Set<Long> etapes = instances.stream()
                .map(i -> identifiantNumerique(i.getEtatCode()))
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (etapes.isEmpty()) {
            return Map.of();
        }
        WorkflowMapper mapper = new WorkflowMapper();
        return stepRepository.findAvecChampsByIdIn(etapes).stream()
                .collect(java.util.stream.Collectors.toMap(
                        WorkflowStep::getId,
                        step -> step.getFields().stream().map(mapper::toDto).toList()));
    }

    /**
     * Identifiant numérique porté par un code d'état ou de transition, ou {@code null}.
     *
     * <p>Les états terminaux synthétiques ({@code TERMINATED_*}) n'en portent pas. Cette
     * distinction était faite en attrapant {@link NumberFormatException} à quatre endroits :
     * une exception servait de test, et le contrat implicite « code d'état = identifiant
     * d'étape » n'était énoncé nulle part.</p>
     */
    private static Long identifiantNumerique(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(code);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Au-delà, la demande est refusée plutôt que servie au prix d'un balayage sans borne. */
    static final int TAILLE_LOT_MAX = 200;

    /**
     * États de plusieurs ressources en un appel, à coût constant en nombre de requêtes.
     *
     * <p>L'appel unique masquait jusqu'ici un coût par ressource resté intact : la méthode
     * répétait simplement la lecture unitaire, soit six requêtes par dossier plus une par action
     * offerte — près de quatre cents pour une page de cinquante documents, dont une centaine de
     * contrôles de version du catalogue rigoureusement identiques. Les dossiers, les décisions et
     * les champs de saisie sont désormais chargés en trois requêtes, quel que soit le nombre de
     * ressources demandées.</p>
     *
     * <p>Un dossier dont l'étape courante a disparu du circuit est écarté du résultat plutôt que
     * de faire échouer toute la page : c'est une consultation, et l'incohérence ne concerne qu'un
     * dossier.</p>
     */
    @Transactional(readOnly = true)
    public Map<UUID, com.qualiapproche.workflow.dto.WorkflowStateDto> getWorkflowStatesForResources(List<UUID> resourceIds) {
        Map<UUID, com.qualiapproche.workflow.dto.WorkflowStateDto> etats = new java.util.LinkedHashMap<>();
        if (resourceIds == null || resourceIds.isEmpty()) {
            return etats;
        }
        List<UUID> demandees = resourceIds.stream().filter(Objects::nonNull).distinct().toList();
        if (demandees.size() > TAILLE_LOT_MAX) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Trop de ressources demandées en une fois (" + demandees.size() + "). "
                            + "Le maximum est de " + TAILLE_LOT_MAX + ".",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        Map<UUID, WorkflowValidationInstance> instances = dernieresInstances(demandees);
        if (instances.isEmpty()) {
            return etats;
        }

        // Les transitions se calculent sur le catalogue en mémoire : aucune requête ici.
        Map<UUID, java.util.Set<TransitionPersistante>> actionsParRessource = new java.util.LinkedHashMap<>();
        for (Map.Entry<UUID, WorkflowValidationInstance> entree : instances.entrySet()) {
            try {
                rattacherEtat(entree.getValue());
                actionsParRessource.put(entree.getKey(), transitionsPossiblesDe(entree.getValue()));
            } catch (com.qualiapproche.common.exception.BusinessException e) {
                log.warn("État de la ressource {} non exploitable, elle est écartée de la liste : {}",
                        entree.getKey(), e.getMessage());
            }
        }

        Map<Long, String> decisions = decisionsDesTransitions(actionsParRessource.values());
        Map<Long, List<com.qualiapproche.workflow.dto.WorkflowStepFieldDto>> champs =
                champsDesEtapes(instances.values());

        for (Map.Entry<UUID, java.util.Set<TransitionPersistante>> entree : actionsParRessource.entrySet()) {
            etats.put(entree.getKey(),
                    construireEtat(instances.get(entree.getKey()), entree.getValue(), decisions, champs));
        }
        return etats;
    }

    /**
     * Dernière instance de chacune des ressources, en une seule requête.
     *
     * <p>Le tri décroissant est porté par la requête : la première ligne rencontrée pour une
     * ressource est donc la plus récente, et les suivantes — l'historique de ses circuits
     * antérieurs — sont ignorées.</p>
     */
    private Map<UUID, WorkflowValidationInstance> dernieresInstances(List<UUID> resourceIds) {
        Map<String, UUID> parCle = resourceIds.stream()
                .collect(java.util.stream.Collectors.toMap(UUID::toString, id -> id, (a, b) -> a));

        Map<UUID, WorkflowValidationInstance> dernieres = new java.util.LinkedHashMap<>();
        for (WorkflowValidationInstance instance :
                validationInstanceRepository.findByResourceIdInOrderByStartedAtDesc(parCle.keySet())) {
            UUID resourceId = parCle.get(instance.getResourceId());
            if (resourceId != null) {
                dernieres.putIfAbsent(resourceId, instance);
            }
        }
        return dernieres;
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
                .validatorFullName(history.getValidatorFullName())
                .decisionDate(history.getDecisionDate())
                .fieldValues(valeurs)
                .build();
    }

    /**
     * Ouvre un circuit de validation sur une ressource.
     *
     * <p>Aucune vérification n'était faite : deux appels successifs créaient deux instances
     * {@code EN_COURS} sur la même ressource, et {@code findTopBy…OrderByStartedAtDesc} en
     * désignait ensuite une arbitrairement — les décisions prises se répartissaient entre deux
     * circuits parallèles, dont un seul remontait au service métier. Le circuit choisi n'était pas
     * davantage contrôlé : un circuit désactivé, dépourvu d'étapes, ou prévu pour un autre type de
     * ressource était accepté sans broncher, la dernière erreur ne se manifestant qu'en violation
     * de contrainte au moment de l'enregistrement.</p>
     *
     * <p>Un appel répété sur le <b>même</b> circuit rend l'instance déjà ouverte plutôt que
     * d'échouer : c'est un rejeu (double clic, reprise Feign), et l'appelant attend légitimement
     * l'état courant. Un appel désignant un <b>autre</b> circuit est en revanche refusé — basculer
     * un dossier d'un circuit à l'autre en cours de route n'est jamais un effet voulu de cette
     * opération.</p>
     */
    @Transactional
    public WorkflowInstanceDto initiateWorkflow(UUID resourceId, String resourceType, UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new com.qualiapproche.common.exception.BusinessException(
                        "Circuit de validation introuvable.", org.springframework.http.HttpStatus.NOT_FOUND));

        verifierCircuitOuvrable(workflow, resourceType);

        WorkflowValidationInstance enCours = validationInstanceRepository
                .findTopByResourceIdAndStatusOrderByStartedAtDesc(resourceId.toString(), ValidationStatus.EN_COURS)
                .orElse(null);
        if (enCours != null) {
            if (workflow.getId().toString().equals(enCours.getWorkflowCode())) {
                return toInstanceDto(enCours);
            }
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Un circuit de validation est déjà en cours sur cette ressource. "
                            + "Terminez-le avant d'en ouvrir un autre.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }

        WorkflowValidationInstance instance = WorkflowValidationInstance.builder()
                .resourceId(resourceId.toString())
                .resourceType(resourceType)
                .status(ValidationStatus.EN_COURS)
                .startedAt(LocalDateTime.now())
                .build();

        WorkflowValidationInstance saved = this.initialiser(instance, workflow.getId().toString());
        return toInstanceDto(saved);
    }

    /** Refuse d'ouvrir un circuit désactivé, vide, ou prévu pour un autre type de ressource. */
    private void verifierCircuitOuvrable(Workflow workflow, String resourceType) {
        if (!workflow.isActif()) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Le circuit « " + workflow.getNom() + " » est désactivé et ne peut plus être ouvert "
                            + "sur de nouveaux dossiers.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }
        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Le circuit « " + workflow.getNom() + " » ne comporte aucune étape : "
                            + "il n'y a pas d'état initial où placer le dossier.",
                    org.springframework.http.HttpStatus.CONFLICT);
        }
        if (resourceType != null && workflow.getResourceType() != null
                && !workflow.getResourceType().equalsIgnoreCase(resourceType)) {
            throw new com.qualiapproche.common.exception.BusinessException(
                    "Le circuit « " + workflow.getNom() + " » s'applique aux ressources de type "
                            + workflow.getResourceType() + ", pas " + resourceType + ".",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
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
        Long stepId = identifiantNumerique(instance.getEtatCode());
        if (stepId == null) {
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
        // Les états terminaux synthétiques (TERMINATED_*) ne portent pas d'étape : plus rien à décider.
        Long stepId = identifiantNumerique(instance.getEtatCode());
        if (stepId == null) {
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
                            .orElseThrow(() -> new com.qualiapproche.common.exception.BusinessException(
                                    "Le champ de saisie " + entry.getKey() + " n'existe pas.",
                                    org.springframework.http.HttpStatus.BAD_REQUEST));

                    if (field.isRequired() && (entry.getValue() == null || entry.getValue().trim().isEmpty())) {
                        throw new com.qualiapproche.common.exception.BusinessException(
                                "Le champ « " + field.getFieldLabel() + " » est requis.",
                                org.springframework.http.HttpStatus.BAD_REQUEST);
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
