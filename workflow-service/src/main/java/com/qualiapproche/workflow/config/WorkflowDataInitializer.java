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
            workflowRepository.save(circuitNonConformiteParDefaut());
            log.info("Default NON_CONFORMITE workflow created successfully.");
        } else {
            log.info("Default NON_CONFORMITE workflow already exists.");
        }

        log.info("Checking for default PLAN_ACTION workflow...");
        List<Workflow> existingPlanActionWorkflows = workflowRepository.findByResourceType("PLAN_ACTION");

        if (existingPlanActionWorkflows.isEmpty()) {
            log.info("No default PLAN_ACTION workflow found. Creating one...");
            workflowRepository.save(circuitPlanActionParDefaut());
            log.info("Default PLAN_ACTION workflow created successfully.");
        } else {
            log.info("Default PLAN_ACTION workflow already exists.");
        }

        log.info("Checking for default DEMANDE_DOCUMENT workflow...");
        if (workflowRepository.findByResourceType("DEMANDE_DOCUMENT").isEmpty()) {
            log.info("No default DEMANDE_DOCUMENT workflow found. Creating one...");
            workflowRepository.save(circuitDemandeDocumentParDefaut());
            log.info("Default DEMANDE_DOCUMENT workflow created successfully.");
        } else {
            log.info("Default DEMANDE_DOCUMENT workflow already exists.");
        }

        log.info("Checking for default DOCUMENT workflow...");
        List<Workflow> existingDocumentWorkflows = workflowRepository.findByResourceType("DOCUMENT");

        if (existingDocumentWorkflows.isEmpty()) {
            log.info("No default DOCUMENT workflow found. Creating one...");
            workflowRepository.save(circuitDocumentParDefaut());
            log.info("Default DOCUMENT workflow created successfully.");
        } else {
            log.info("Default DOCUMENT workflow already exists.");
        }
    }

    /**
     * Circuit documentaire par défaut : rédaction, vérification, approbation.
     *
     * <p>Il manquait, alors que la création d'un document est refusée tant qu'aucun circuit n'est
     * associé à son type : sur une base neuve, aucun document ne pouvait être créé avant qu'un
     * circuit n'ait été monté à la main.</p>
     *
     * <p>L'approbation finale est volontairement <b>terminale</b> — sans étape de destination :
     * c'est ce qui fait remonter un statut {@code APPROVED} à support-service, qui fait alors
     * entrer le document en vigueur. Un rejet renvoie en rédaction plutôt que de clore le
     * circuit, le document restant en cours de traitement.</p>
     *
     * <p>Ces trois fabriques rendent le circuit sans l'enregistrer, et ne dépendent d'aucun état :
     * un test peut ainsi les inspecter sans base, et vérifier que les étapes livrées ici figurent
     * bien au catalogue partagé ({@code CatalogueEtapesStandard}), que sème support-service.</p>
     */
    static Workflow circuitDocumentParDefaut() {
        Workflow workflow = Workflow.builder()
                .nom("Workflow Défaut Document")
                .description("Circuit de validation par défaut des documents qualité")
                .resourceType("DOCUMENT")
                .build();

        WorkflowStep redaction = WorkflowStep.builder()
                .code("REDACTION")
                .nomEtape("Rédaction")
                .stepOrder(1)
                .etatTraitement("REDACTION")
                .description("Rédaction et dépôt du document")
                .responsableRole("AGENT")
                .emailTemplateCode("emailTemplate")
                .build();

        WorkflowStep verification = WorkflowStep.builder()
                .code("VERIFICATION")
                .nomEtape("Vérification")
                .stepOrder(2)
                .etatTraitement("VERIFICATION")
                .description("Vérification de la forme et du fond")
                .responsableRole("PILOTE")
                .emailTemplateCode("structureToStructure")
                .build();
        verification.getFields().add(WorkflowStepField.builder().step(verification)
                .fieldName("observationsVerification").fieldLabel("Observations du vérificateur")
                .type(FieldType.TEXT).isRequired(false).build());

        WorkflowStep approbation = WorkflowStep.builder()
                .code("APPROBATION")
                .nomEtape("Approbation")
                .stepOrder(3)
                .etatTraitement("APPROBATION")
                .description("Approbation et mise en vigueur")
                .responsableRole("RESPONSABLE_QUALITE")
                .emailTemplateCode("validationRq")
                .build();

        redaction.getTransitions().add(WorkflowTransition.builder()
                .fromStep(redaction).toStep(verification)
                .decision(StepDecision.APPROUVE).label("Soumettre pour vérification").build());

        verification.getTransitions().add(WorkflowTransition.builder()
                .fromStep(verification).toStep(approbation)
                .decision(StepDecision.APPROUVE).label("Transmettre pour approbation").build());
        verification.getTransitions().add(WorkflowTransition.builder()
                .fromStep(verification).toStep(redaction)
                .decision(StepDecision.REJETE).label("Retourner au rédacteur").build());

        // Pas d'étape de destination : l'approbation clôt le circuit et fait entrer le document
        // en vigueur côté support-service.
        approbation.getTransitions().add(WorkflowTransition.builder()
                .fromStep(approbation).terminal(true)
                .decision(StepDecision.APPROUVE).label("Approuver et mettre en vigueur").build());
        approbation.getTransitions().add(WorkflowTransition.builder()
                .fromStep(approbation).toStep(redaction)
                .decision(StepDecision.REJETE).label("Refuser et retourner au rédacteur").build());

        workflow.addStep(redaction);
        workflow.addStep(verification);
        workflow.addStep(approbation);

        return workflow;
    }

    static Workflow circuitNonConformiteParDefaut() {
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
                .responsableRole("AGENT") // or any
                .emailTemplateCode("emailTemplate")
                .build();

        // 2. RECEPTION
        WorkflowStep step2 = WorkflowStep.builder()
                .code("RECEPTION")
                .nomEtape("Réception")
                .stepOrder(2)
                .etatTraitement("RECEPTION")
                .description("Réception et prise en charge")
                .responsableRole("PILOTE")
                .emailTemplateCode("structureToStructure")
                .build();

        // 3. IMPUTATION
        WorkflowStep step3 = WorkflowStep.builder()
                .code("IMPUTATION")
                .nomEtape("Imputation")
                .stepOrder(3)
                .etatTraitement("IMPUTATION")
                .description("Imputation à un agent traitant")
                .responsableRole("PILOTE")
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
                .responsableRole("AGENT_IMPUTE")
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
                .responsableRole("PILOTE")
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
                .responsableRole("RESPONSABLE_QUALITE")
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
                .responsableRole("RESPONSABLE_QUALITE")
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
                .responsableRole("RESPONSABLE_QUALITE")
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

        return workflow;
    }

    static Workflow circuitPlanActionParDefaut() {
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
                .responsableRole("AGENT_IMPUTE")
                .emailTemplateCode("emailPlanAction")
                .build();

        // 2. TRAITER
        WorkflowStep step2 = WorkflowStep.builder()
                .code("TRAITER")
                .nomEtape("Traité")
                .stepOrder(2)
                .etatTraitement("TRAITER")
                .description("Le plan d'action a été exécuté")
                .responsableRole("PILOTE")
                .emailTemplateCode("emailRqPlan")
                .build();

        // 3. REJECTED
        WorkflowStep step3 = WorkflowStep.builder()
                .code("REJECTED")
                .nomEtape("Rejeté")
                .stepOrder(3)
                .etatTraitement("REJECTED")
                .description("Le plan d'action a été rejeté")
                .responsableRole("PILOTE")
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

        return workflow;
    }


    /**
     * Circuit par défaut d'une demande de modification ou de suppression de document.
     *
     * <p>Trois étapes, volontairement courtes : le demandeur soumet, le responsable qualité
     * instruit, et la décision clôt le circuit. C'est cette clôture qui déclenche l'aboutissement
     * côté support-service — dépôt du fichier remplaçant pour une modification, retrait du document
     * pour une suppression — d'où une étape terminale explicite plutôt qu'une fin déduite.</p>
     *
     * <p>Le rejet renvoie au demandeur : une demande refusée doit pouvoir être reprise et
     * réargumentée, non disparaître.</p>
     */
    static Workflow circuitDemandeDocumentParDefaut() {
        Workflow workflow = Workflow.builder()
                .nom("Workflow Défaut Demande Document")
                .description("Circuit de validation des demandes de modification et de suppression de document")
                .resourceType("DEMANDE_DOCUMENT")
                .build();

        WorkflowStep soumission = WorkflowStep.builder()
                .code("DEMANDE_SOUMISSION")
                .nomEtape("Soumission de la demande")
                .stepOrder(1)
                .etatTraitement("SOUMISSION")
                .description("Rédaction et dépôt de la demande")
                .responsableRole("AGENT")
                .emailTemplateCode("emailTemplate")
                .build();

        WorkflowStep instruction = WorkflowStep.builder()
                .code("DEMANDE_INSTRUCTION")
                .nomEtape("Instruction de la demande")
                .stepOrder(2)
                .etatTraitement("INSTRUCTION")
                .description("Examen de la demande au titre de la qualité")
                .responsableRole("RESPONSABLE_QUALITE")
                .emailTemplateCode("validationRq")
                .build();
        instruction.getFields().add(WorkflowStepField.builder().step(instruction)
                .fieldName("avisQualite").fieldLabel("Avis du responsable qualité")
                .type(FieldType.TEXT).isRequired(true).build());

        WorkflowStep decision = WorkflowStep.builder()
                .code("DEMANDE_DECISION")
                .nomEtape("Décision")
                .stepOrder(3)
                .etatTraitement("DECISION")
                .description("Suite donnée à la demande")
                .responsableRole("RESPONSABLE_QUALITE")
                .emailTemplateCode("traitementReussi")
                .build();

        soumission.getTransitions().add(WorkflowTransition.builder()
                .fromStep(soumission).toStep(instruction)
                .decision(StepDecision.APPROUVE).label("Soumettre la demande").build());

        instruction.getTransitions().add(WorkflowTransition.builder()
                .fromStep(instruction).toStep(decision)
                .decision(StepDecision.APPROUVE).label("Retenir la demande").build());
        instruction.getTransitions().add(WorkflowTransition.builder()
                .fromStep(instruction).toStep(soumission)
                .decision(StepDecision.REJETE).label("Renvoyer au demandeur").build());

        // Terminale et déclarée comme telle : sans ce marqueur, le moteur ignore la transition et
        // l'aboutissement — remplacement du fichier ou retrait du document — n'aurait pas lieu.
        decision.getTransitions().add(WorkflowTransition.builder()
                .fromStep(decision).terminal(true)
                .decision(StepDecision.APPROUVE).label("Accepter et exécuter").build());
        decision.getTransitions().add(WorkflowTransition.builder()
                .fromStep(decision).terminal(true)
                .decision(StepDecision.REJETE).label("Refuser la demande").build());

        workflow.addStep(soumission);
        workflow.addStep(instruction);
        workflow.addStep(decision);

        return workflow;
    }
}
