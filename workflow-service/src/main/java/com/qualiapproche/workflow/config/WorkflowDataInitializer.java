package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowStepField;
import com.qualiapproche.workflow.model.FieldType;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
@org.springframework.core.annotation.Order(10) // avant WorkflowStepCodeInitializer, qui complète les codes
public class WorkflowDataInitializer implements CommandLineRunner {

    private final WorkflowRepository workflowRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking for default NON_CONFORMITE workflow...");
        List<Workflow> existingWorkflows = workflowRepository.findByResourceType("NON_CONFORMITE");
        
        if (existingWorkflows.isEmpty()) {
            log.info("No default NON_CONFORMITE workflow found. Creating one...");
            createDefaultNonConformiteWorkflow();
            log.info("Default NON_CONFORMITE workflow created successfully.");
        } else {
            log.info("Default NON_CONFORMITE workflow already exists.");
        }

        log.info("Checking for default PLAN_ACTION workflow...");
        List<Workflow> existingPlanActionWorkflows = workflowRepository.findByResourceType("PLAN_ACTION");
        
        if (existingPlanActionWorkflows.isEmpty()) {
            log.info("No default PLAN_ACTION workflow found. Creating one...");
            createDefaultPlanActionWorkflow();
            log.info("Default PLAN_ACTION workflow created successfully.");
        } else {
            log.info("Default PLAN_ACTION workflow already exists.");
        }


    }

    private void createDefaultNonConformiteWorkflow() {
        Workflow workflow = Workflow.builder()
                .nom("Workflow Défaut Non-Conformité")
                .description("Workflow par défaut pour le traitement des non-conformités")
                .resourceType("NON_CONFORMITE")
                .build();

        // 1. SOUMISSION
        WorkflowStep step1 = WorkflowStep.builder()
                .code("SOUMISSION")
                .nomEtape("Soumission")
                .stepOrder(1)
                .etatTraitement("SOUMISSION")
                .description("Création et soumission de la NC")
                .responsableRole("ROLE_AGENT") // or any
                .emailTemplateCode("emailTemplate")
                .build();

        // 2. RECEPTION
        WorkflowStep step2 = WorkflowStep.builder()
                .code("RECEPTION")
                .nomEtape("Réception")
                .stepOrder(2)
                .etatTraitement("RECEPTION")
                .description("Réception et prise en charge")
                .responsableRole("ROLE_PILOTE")
                .emailTemplateCode("structureToStructure")
                .build();

        // 3. IMPUTATION
        WorkflowStep step3 = WorkflowStep.builder()
                .code("IMPUTATION")
                .nomEtape("Imputation")
                .stepOrder(3)
                .etatTraitement("IMPUTATION")
                .description("Imputation à un agent traitant")
                .responsableRole("ROLE_PILOTE")
                .emailTemplateCode("emailTemplate")
                .build();
        step3.getFields().add(WorkflowStepField.builder().step(step3).fieldName("userImputId").fieldLabel("Agent responsable du traitement").type(FieldType.TEXT).isRequired(true).build());

        // 4. TRAITEMENT
        WorkflowStep step4 = WorkflowStep.builder()
                .code("TRAITEMENT")
                .nomEtape("Traitement")
                .stepOrder(4)
                .etatTraitement("TRAITEMENT")
                .description("Analyse et mise en œuvre du plan d'action")
                .responsableRole("ROLE_AGENT_IMPUTE")
                .emailTemplateCode("emailPlanAction")
                .build();
        step4.getFields().add(WorkflowStepField.builder().step(step4).fieldName("actionPreventive").fieldLabel("Action préventive proposée").type(FieldType.TEXT).isRequired(true).build());
        step4.getFields().add(WorkflowStepField.builder().step(step4).fieldName("delaisMiseOeuvre").fieldLabel("Délai de mise en œuvre").type(FieldType.TEXT).isRequired(true).build());
        step4.getFields().add(WorkflowStepField.builder().step(step4).fieldName("actionDsc").fieldLabel("Action de DSC").type(FieldType.TEXT).isRequired(false).build());

        // 5. VALIDATION
        WorkflowStep step5 = WorkflowStep.builder()
                .code("VALIDATION")
                .nomEtape("Validation")
                .stepOrder(5)
                .etatTraitement("VALIDATION")
                .description("Validation de la pertinence des actions")
                .responsableRole("ROLE_PILOTE")
                .emailTemplateCode("validationNonConformite")
                .build();
        step5.getFields().add(WorkflowStepField.builder().step(step5).fieldName("pertinancePilote").fieldLabel("Pertinence de l'action").type(FieldType.TEXT).isRequired(true).build());
        step5.getFields().add(WorkflowStepField.builder().step(step5).fieldName("justificationPilote").fieldLabel("Justification du Pilote").type(FieldType.TEXT).isRequired(true).build());

        // 6. VALIDATION_RS
        WorkflowStep step6 = WorkflowStep.builder()
                .code("VALIDATION_RS")
                .nomEtape("Validation RS")
                .stepOrder(6)
                .etatTraitement("VALIDATION_RS")
                .description("Validation RQ des actions")
                .responsableRole("ROLE_RESPONSABLE_QUALITE")
                .emailTemplateCode("validationRq")
                .build();
        step6.getFields().add(WorkflowStepField.builder().step(step6).fieldName("pertinanceRs").fieldLabel("Pertinence de l'action (RS)").type(FieldType.TEXT).isRequired(true).build());
        step6.getFields().add(WorkflowStepField.builder().step(step6).fieldName("justificationRs").fieldLabel("Justification du RS").type(FieldType.TEXT).isRequired(true).build());

        // 7. SUIVI_RQ
        WorkflowStep step7 = WorkflowStep.builder()
                .code("SUIVI_RQ")
                .nomEtape("Suivi RQ")
                .stepOrder(7)
                .etatTraitement("SUIVI_RQ")
                .description("Suivi de l'efficacité et clôture")
                .responsableRole("ROLE_RESPONSABLE_QUALITE")
                .emailTemplateCode("validationAfterPlan")
                .build();
        step7.getFields().add(WorkflowStepField.builder().step(step7).fieldName("efficaciteId").fieldLabel("Efficacité (ID)").type(FieldType.TEXT).isRequired(true).build());
        step7.getFields().add(WorkflowStepField.builder().step(step7).fieldName("observationsRq").fieldLabel("Observations finales (RQ)").type(FieldType.TEXT).isRequired(true).build());

        // 8. CLOTURE
        WorkflowStep step8 = WorkflowStep.builder()
                .code("CLOTURE")
                .nomEtape("Clôture")
                .stepOrder(8)
                .etatTraitement("CLOTURE")
                .description("Clôture finale de la NC")
                .responsableRole("ROLE_RESPONSABLE_QUALITE")
                .emailTemplateCode("traitementReussi")
                .build();

        // Setup transitions
        step1.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step1).toStep(step2).decision(StepDecision.APPROUVE).label("Soumettre la NC").build());
        
        step2.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step2).toStep(step3).decision(StepDecision.APPROUVE).label("Prendre en charge").build());

        step3.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step3).toStep(step4).decision(StepDecision.APPROUVE).label("Imputer à l'agent").build());

        step4.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step4).toStep(step5).decision(StepDecision.APPROUVE).label("Soumettre le traitement").build());

        step5.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step5).toStep(step6).decision(StepDecision.APPROUVE).label("Valider pour le RQ").build());
        step5.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step5).toStep(step4).decision(StepDecision.REJETE).label("Retourner pour retraitement").build());

        step6.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step6).toStep(step7).decision(StepDecision.APPROUVE).label("Approuver et suivre").build());
        step6.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step6).toStep(step4).decision(StepDecision.REJETE).label("Rejeter les actions").build());

        step7.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step7).toStep(step8).decision(StepDecision.APPROUVE).label("Clôturer la NC").build());
        step7.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step7).toStep(step4).decision(StepDecision.REJETE).label("Demander des corrections").build());

        workflow.addStep(step1);
        workflow.addStep(step2);
        workflow.addStep(step3);
        workflow.addStep(step4);
        workflow.addStep(step5);
        workflow.addStep(step6);
        workflow.addStep(step7);
        workflow.addStep(step8);

        workflowRepository.save(workflow);
    }

    private void createDefaultPlanActionWorkflow() {
        Workflow workflow = Workflow.builder()
                .nom("Workflow Défaut Plan d'Action")
                .description("Workflow par défaut pour le suivi d'un plan d'action")
                .resourceType("PLAN_ACTION")
                .build();

        // 1. NON_TRAITER
        WorkflowStep step1 = WorkflowStep.builder()
                .code("NON_TRAITER")
                .nomEtape("À Traiter")
                .stepOrder(1)
                .etatTraitement("NON_TRAITER")
                .description("Plan d'action en attente de traitement par l'agent")
                .responsableRole("ROLE_AGENT_IMPUTE")
                .emailTemplateCode("emailPlanAction")
                .build();

        // 2. TRAITER
        WorkflowStep step2 = WorkflowStep.builder()
                .code("TRAITER")
                .nomEtape("Traité")
                .stepOrder(2)
                .etatTraitement("TRAITER")
                .description("Le plan d'action a été exécuté")
                .responsableRole("ROLE_PILOTE")
                .emailTemplateCode("emailRqPlan")
                .build();

        // 3. REJECTED
        WorkflowStep step3 = WorkflowStep.builder()
                .code("REJECTED")
                .nomEtape("Rejeté")
                .stepOrder(3)
                .etatTraitement("REJECTED")
                .description("Le plan d'action a été rejeté")
                .responsableRole("ROLE_PILOTE")
                .emailTemplateCode("rejectPlanAction")
                .build();

        step1.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step1).toStep(step2).decision(StepDecision.APPROUVE).label("Marquer comme traité").build());
        step1.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step1).toStep(step3).decision(StepDecision.REJETE).label("Rejeter l'attribution").build());

        step2.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step2).toStep(step1).decision(StepDecision.REJETE).label("Demander une correction").build());

        step3.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step3).toStep(step1).decision(StepDecision.APPROUVE).label("Ré-attribuer").build());

        workflow.addStep(step1);
        workflow.addStep(step2);
        workflow.addStep(step3);

        workflowRepository.save(workflow);
    }


}
