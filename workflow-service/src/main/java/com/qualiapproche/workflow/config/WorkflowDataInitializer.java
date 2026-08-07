package com.qualiapproche.workflow.config;

import com.qualiapproche.common.utils.RolesPlateforme;
import com.qualiapproche.workflow.model.SeveriteAction;
import com.qualiapproche.workflow.model.SourceDeChoix;
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

/**
 * Circuits livrés d'origine, semés sur une base qui n'en a encore aucun pour le type concerné.
 *
 * <p>Chaque transition porte son icône et sa couleur de bouton. La couleur y traduit l'effet sur
 * le dossier, non la seule décision : {@code INFO} pour un pas en avant (soumettre, transmettre),
 * {@code SUCCESS} pour un aboutissement (approuver, clôturer), {@code WARN} pour un renvoi en
 * correction dont le dossier se relève, {@code DANGER} pour un refus ferme. Sans quoi « Retourner
 * au rédacteur » et « Refuser la demande » — deux {@code REJETE} — s'afficheraient à l'identique,
 * alors que l'une invite à corriger et l'autre arrête le dossier.</p>
 *
 * <p>Les circuits déjà en base ne sont pas repris : ils n'ont ni icône ni couleur et s'affichent
 * avec les valeurs déduites de la décision ({@link StepDecision#getIconeParDefaut()}).</p>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@org.springframework.core.annotation.Order(10) // avant WorkflowStepCodeInitializer, qui complète les codes
public class WorkflowDataInitializer implements CommandLineRunner {

    /**
     * Nom du champ portant le document justificatif d'un rejet. Repris tel quel par
     * amelioration-service pour alimenter le {@code docRejet} de la non-conformité : c'est le nom,
     * et non l'identifiant d'étape, qui fait le lien entre le circuit et le module métier.
     */
    static final String CHAMP_DOCUMENT_REJET = "docRejet";

    /**
     * Champ par lequel l'imputation nomme la personne qui traitera le dossier.
     *
     * <p>« Agent imputé » n'est pas un rôle : c'est quelqu'un, sur ce dossier-là. Sa valeur devient
     * le titulaire de l'instance, et c'est à lui — non à un rôle — que l'étape de traitement est
     * réservée.</p>
     */
    static final String CHAMP_AGENT_IMPUTE = "userImputId";

    /** Nom métier, côté plan d'action, de la personne qui répond du plan. */
    static final String CHAMP_RESPONSABLE_PLAN = "responsableId";

    /** Ce que le responsable qualité a observé, confronté au critère d'efficacité fixé au départ. */
    static final String CHAMP_CONSTAT_EFFICACITE = "constatEfficacite";

    /**
     * Cause que le responsable a identifiée en menant son action.
     *
     * <p>Avec {@link #CHAMP_SOLUTIONS_RETENUES}, c'est le <b>compte rendu</b> de l'action : ce que
     * le pilote constate ensuite, et ce sur quoi le responsable qualité juge de l'efficacité.
     * Recueilli par un écran puis enregistré par un appel séparé, il se perdait chaque fois que la
     * décision partait la première — l'action changeait d'étape, et le compte rendu arrivait sur un
     * plan qui n'était plus en cours de réalisation. Porté par l'étape, il voyage avec la décision
     * qu'il justifie, et l'historique du moteur en garde la trace.</p>
     */
    static final String CHAMP_CAUSES_IDENTIFIEES = "causeIdentifiees";

    /** Ce que le responsable a mis en œuvre pour remédier à la cause qu'il a identifiée. */
    static final String CHAMP_SOLUTIONS_RETENUES = "solutionRetenues";

    /**
     * Fait exigé pour valider le traitement : chaque action corrective a un responsable.
     *
     * <p>Le nom doit valoir exactement celui que déclare amelioration-service. L'agent imputé peut
     * désigner le responsable en écrivant l'action, le pilote peut le désigner ou le corriger à la
     * validation — mais une action sans responsable ne serait confiée à personne, et le dossier
     * attendrait le solde d'une action que nul n'a commencée.</p>
     */
    static final String FAIT_PLANS_ACTION_AFFECTES = "PLANS_ACTION_AFFECTES";

    /**
     * Fait exigé pour clore une non-conformité : toutes ses actions correctives sont soldées.
     *
     * <p>Le moteur ne sait pas ce qu'est une action corrective. Il sait que le dossier porte ou non
     * ce fait, qu'amelioration-service inscrit quand la dernière est reconnue efficace et retire dès
     * qu'une rouvre. Clore un dossier dont les actions ne sont pas menées, c'est clore une
     * non-conformité qui n'est pas corrigée — la seule chose qu'un système qualité ne doit pas
     * permettre.</p>
     */
    static final String FAIT_PLANS_ACTION_SOLDES = "PLANS_ACTION_SOLDES";

    /**
     * Habilitation réservant une transition au titulaire du dossier.
     *
     * <p>Reconnue par {@code WorkflowConditionAdapter}. Le préfixe la distingue d'un nom de rôle,
     * qu'aucun ne peut porter.</p>
     */
    static final String HABILITATION_TITULAIRE = RolesPlateforme.HABILITATION_TITULAIRE;

    /** Habilitation réservant une étape à celui qui a ouvert le dossier. */
    static final String HABILITATION_CREATEUR = RolesPlateforme.HABILITATION_CREATEUR;

    /**
     * Champs désignant la structure à qui la non-conformité est confiée.
     *
     * <p>Le passage d'une structure à une autre s'écrivait par une mise à jour ordinaire du
     * dossier : n'importe quel enregistrement pouvait le changer, sans auteur, sans date, sans
     * trace. En faire des champs d'étape le range là où sont déjà les autres décisions — l'historique
     * du moteur dit alors qui a transféré, quand, et vers qui.</p>
     */
    static final String CHAMP_STRUCTURE_DESTINATAIRE_ID = "structureDestinataireId";

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
                .decision(StepDecision.APPROUVE).label("Soumettre pour vérification")
                .icon("pi pi-send").severity(SeveriteAction.INFO).build());

        verification.getTransitions().add(WorkflowTransition.builder()
                .fromStep(verification).toStep(approbation)
                .decision(StepDecision.APPROUVE).label("Transmettre pour approbation")
                .icon("pi pi-arrow-right").severity(SeveriteAction.INFO).build());
        verification.getTransitions().add(WorkflowTransition.builder()
                .fromStep(verification).toStep(redaction)
                .decision(StepDecision.REJETE).label("Retourner au rédacteur")
                .icon("pi pi-undo").severity(SeveriteAction.WARN).build());

        // Pas d'étape de destination : l'approbation clôt le circuit et fait entrer le document
        // en vigueur côté support-service.
        approbation.getTransitions().add(WorkflowTransition.builder()
                .fromStep(approbation).terminal(true)
                .decision(StepDecision.APPROUVE).label("Approuver et mettre en vigueur")
                .icon("pi pi-check-circle").severity(SeveriteAction.SUCCESS).build());
        approbation.getTransitions().add(WorkflowTransition.builder()
                .fromStep(approbation).toStep(redaction)
                .decision(StepDecision.REJETE).label("Refuser et retourner au rédacteur")
                .icon("pi pi-times-circle").severity(SeveriteAction.DANGER).build());

        workflow.addStep(redaction);
        workflow.addStep(verification);
        workflow.addStep(approbation);

        return workflow;
    }

    /**
     * Champ de dépôt du document justifiant un rejet, posé sur les étapes d'où part une décision
     * {@code REJETE}.
     *
     * <p>Le justificatif se demandait jusqu'ici par un écran propre à la non-conformité, en marge
     * du moteur : la pièce accompagnant la décision ne relevait donc pas de la décision. C'est un
     * champ d'étape comme les autres, à ceci près qu'il ne porte que la <b>référence</b> du fichier
     * déposé — le moteur ne transporte que des chaînes.</p>
     *
     * <p>Rattaché à la décision de rejet, et non à l'étape : la même étape sert aussi à approuver,
     * et le justificatif s'y présentait alors sans objet. Il reste facultatif — un rejet peut être
     * motivé par le seul commentaire.</p>
     */
    static WorkflowStepField champDocumentRejet(WorkflowStep step) {
        return WorkflowStepField.builder()
                .step(step)
                .fieldName(CHAMP_DOCUMENT_REJET)
                .fieldLabel("Document justificatif du rejet")
                .type(FieldType.FILE)
                .isRequired(false)
                // Il n'a de sens qu'au rejet : sans cette portée, on demandait de justifier un
                // refus à qui était en train d'approuver.
                .decision(StepDecision.REJETE)
                .build();
    }

    /**
     * Champ désignant la structure à qui le dossier est confié.
     *
     * <p>Un seul champ, et non trois. Le libellé et le sigle accompagnaient l'identifiant pour que
     * les listes les affichent sans interroger le référentiel : c'était demander à l'utilisateur de
     * ressaisir ce que le référentiel sait déjà, et lui laisser la possibilité de les contredire.
     * Le module métier les résout maintenant lui-même à partir de l'identifiant reçu.</p>
     *
     * <p>Liste de choix alimentée par le référentiel des structures : les valeurs ne sont pas
     * écrites dans le circuit, elles en sont issues. Une structure créée après le circuit y figure
     * donc sans qu'on ait à le remanier.</p>
     *
     * @param designationExigee vrai là où l'affectation <b>est</b> la décision — la validation du
     *                          responsable qualité. Approuver sans désigner laisserait le dossier à
     *                          une étape d'imputation que le pilote d'aucune structure ne se
     *                          reconnaîtrait tenu de traiter.
     */
    static WorkflowStepField champStructureDestinataire(WorkflowStep step, boolean designationExigee) {
        return WorkflowStepField.builder()
                .step(step)
                .fieldName(CHAMP_STRUCTURE_DESTINATAIRE_ID)
                .fieldLabel("Structure destinataire")
                .type(FieldType.SELECT)
                .options(SourceDeChoix.STRUCTURES.getCle())
                .isRequired(designationExigee)
                .build();
    }

    static Workflow circuitNonConformiteParDefaut() {
        Workflow workflow = Workflow.builder()
                .nom("Workflow Défaut Non-Conformité")
                .description("Workflow par défaut pour le traitement des non-conformités")
                .resourceType("NON_CONFORMITE")
                .build();

        // 1. SOUMISSION — l'émetteur déclare.
        WorkflowStep soumission = WorkflowStep.builder()
                .code("SOUMISSION")
                .nomEtape("Soumission")
                .stepOrder(1)
                .etatTraitement("SOUMISSION")
                .description("Création et soumission de la NC")
                // Qui est concerné : les agents. Qui peut agir : l'auteur seul — l'habilitation de
                // la transition « Soumettre la NC » le dit. Un rôle est collectif, soumettre sa
                // déclaration ne l'est pas, et le dossier revient ici quand on le renvoie.
                .responsableRole(HABILITATION_CREATEUR)
                .emailTemplateCode("emailTemplate")
                .build();

        // 2. RECEPTION — prise en charge du signalement.
        WorkflowStep reception = WorkflowStep.builder()
                .code("RECEPTION")
                .nomEtape("Réception")
                .stepOrder(2)
                .etatTraitement("RECEPTION")
                .description("Réception et prise en charge")
                .responsableRole("PILOTE")
                .emailTemplateCode("structureToStructure")
                .build();
        reception.getFields().add(champDocumentRejet(reception));

        // 3. VALIDATION_RQ — le responsable qualité valide et désigne la structure en charge.
        //
        // La désignation est obligatoire : approuver sans dire à qui laisserait le dossier à une
        // étape d'imputation que le pilote d'aucune structure ne se reconnaîtrait tenu de traiter.
        WorkflowStep validationRq = WorkflowStep.builder()
                .code("VALIDATION_RQ")
                .nomEtape("Validation RQ")
                .stepOrder(3)
                .etatTraitement("VALIDATION_RQ")
                .description("Validation par le responsable qualité et affectation à une structure")
                .responsableRole("RESPONSABLE_QUALITE")
                .emailTemplateCode("structureToStructure")
                .build();
        validationRq.getFields().add(champStructureDestinataire(validationRq, true));
        validationRq.getFields().add(champDocumentRejet(validationRq));

        // 4. IMPUTATION — le pilote de la structure désignée nomme le traitant.
        WorkflowStep imputation = WorkflowStep.builder()
                .code("IMPUTATION")
                .nomEtape("Imputation")
                .stepOrder(4)
                .etatTraitement("IMPUTATION")
                .description("Imputation à un agent traitant")
                .responsableRole("PILOTE")
                .emailTemplateCode("emailTemplate")
                // Imputer, c'est nommer : la valeur saisie ici devient le titulaire du dossier, et
                // c'est elle — non un rôle — qui ouvrira l'étape de traitement.
                .champTitulaire(CHAMP_AGENT_IMPUTE)
                .build();
        // Liste des agents de la structure du pilote qui impute, et non saisie libre d'un
        // identifiant : personne ne connaît par cœur l'identifiant technique d'un collègue, et
        // l'annuaire entier laisserait désigner quelqu'un qui ne relève pas de lui.
        imputation.getFields().add(WorkflowStepField.builder().step(imputation).fieldName(CHAMP_AGENT_IMPUTE)
                .fieldLabel("Agent responsable du traitement")
                .type(FieldType.SELECT)
                .options(SourceDeChoix.UTILISATEURS_DE_MA_STRUCTURE.getCle())
                .isRequired(true).build());
        imputation.getFields().add(champDocumentRejet(imputation));

        // 5. TRAITEMENT — réservé à la personne imputée, et à elle seule.
        WorkflowStep traitement = WorkflowStep.builder()
                .code("TRAITEMENT")
                .nomEtape("Traitement")
                .stepOrder(5)
                .etatTraitement("TRAITEMENT")
                .description("Analyse et mise en œuvre du plan d'action")
                // Pas un rôle : la personne nommée à l'imputation, et elle seule. Portée aussi
                // par l'étape et non par ses seules transitions, pour que le modèle d'étape
                // publié au catalogue dise la même chose.
                .responsableRole(HABILITATION_TITULAIRE)
                .emailTemplateCode("emailPlanAction")
                .build();
        // Le traitement ne demande rien en saisie libre : ce qui est décidé à cette étape s'écrit
        // dans les plans d'action (action corrective, échéance, critère d'efficacité), où le circuit
        // du plan le suit ensuite. Le redemander en texte sur l'étape faisait saisir deux fois la
        // même chose, sans que personne ne relise la seconde.
        traitement.getFields().add(champDocumentRejet(traitement));

        // 6. VALIDATION — le pilote juge la pertinence des actions proposées.
        WorkflowStep validation = WorkflowStep.builder()
                .code("VALIDATION")
                .nomEtape("Validation")
                .stepOrder(6)
                .etatTraitement("VALIDATION")
                .description("Validation de la pertinence des actions")
                .responsableRole("PILOTE")
                .emailTemplateCode("validationNonConformite")
                .build();
        // La pertinence et sa justification étaient demandées en texte alors que le pilote se
        // prononce déjà par sa décision : approuver, c'est juger l'action pertinente, et tout
        // franchissement exige un commentaire que l'historique conserve.
        validation.getFields().add(champDocumentRejet(validation));

        // 7. VALIDATION_RS — contre-validation de la qualité.
        WorkflowStep validationRs = WorkflowStep.builder()
                .code("VALIDATION_RS")
                .nomEtape("Validation RS")
                .stepOrder(7)
                .etatTraitement("VALIDATION_RS")
                .description("Validation RQ des actions")
                .responsableRole("RESPONSABLE_QUALITE")
                .emailTemplateCode("validationRq")
                .build();
        validationRs.getFields().add(WorkflowStepField.builder().step(validationRs).fieldName("pertinanceRs")
                .fieldLabel("Pertinence de l'action (RS)").type(FieldType.TEXT).isRequired(true).build());
        validationRs.getFields().add(WorkflowStepField.builder().step(validationRs).fieldName("justificationRs")
                .fieldLabel("Justification du RS").type(FieldType.TEXT).isRequired(true).build());
        validationRs.getFields().add(champDocumentRejet(validationRs));

        // 8. SUIVI_RQ — vérification de l'efficacité, une fois les actions mises en œuvre.
        WorkflowStep suiviRq = WorkflowStep.builder()
                .code("SUIVI_RQ")
                .nomEtape("Suivi RQ")
                .stepOrder(8)
                .etatTraitement("SUIVI_RQ")
                .description("Suivi de l'efficacité et clôture")
                .responsableRole("RESPONSABLE_QUALITE")
                .emailTemplateCode("validationAfterPlan")
                .build();
        suiviRq.getFields().add(WorkflowStepField.builder().step(suiviRq).fieldName("efficaciteId")
                .fieldLabel("Efficacité (ID)").type(FieldType.TEXT).isRequired(true).build());
        suiviRq.getFields().add(WorkflowStepField.builder().step(suiviRq).fieldName("observationsRq")
                .fieldLabel("Observations finales (RQ)").type(FieldType.TEXT).isRequired(true).build());
        suiviRq.getFields().add(champDocumentRejet(suiviRq));

        // 9. CLOTURE
        WorkflowStep cloture = WorkflowStep.builder()
                .code("CLOTURE")
                .nomEtape("Clôture")
                .stepOrder(9)
                .etatTraitement("CLOTURE")
                .description("Clôture finale de la NC")
                .responsableRole("RESPONSABLE_QUALITE")
                .emailTemplateCode("traitementReussi")
                .build();

        // --- Enchaînement, et les issues de rejet qui l'accompagnent -------------------------

        // Déclaration : pas de rejet, rien n'a encore été examiné.
        soumission.getTransitions().add(WorkflowTransition.builder()
                .fromStep(soumission).toStep(reception).decision(StepDecision.APPROUVE).label("Soumettre la NC")
                .icon("pi pi-send").severity(SeveriteAction.INFO).build());

        // Réception : transmettre à la qualité, ou renvoyer au déclarant pour correction.
        reception.getTransitions().add(WorkflowTransition.builder()
                .fromStep(reception).toStep(validationRq).decision(StepDecision.APPROUVE)
                .label("Transmettre au responsable qualité")
                .icon("pi pi-inbox").severity(SeveriteAction.INFO).build());
        reception.getTransitions().add(WorkflowTransition.builder()
                .fromStep(reception).toStep(soumission).decision(StepDecision.REJETE).label("Renvoyer au déclarant")
                .icon("pi pi-undo").severity(SeveriteAction.WARN).build());

        // Validation RQ : valider en désignant la structure, ou renvoyer à la réception.
        validationRq.getTransitions().add(WorkflowTransition.builder()
                .fromStep(validationRq).toStep(imputation).decision(StepDecision.APPROUVE)
                .label("Valider et affecter à la structure")
                .icon("pi pi-check").severity(SeveriteAction.SUCCESS).build());
        validationRq.getTransitions().add(WorkflowTransition.builder()
                .fromStep(validationRq).toStep(reception).decision(StepDecision.REJETE)
                .label("Renvoyer à la réception")
                .icon("pi pi-undo").severity(SeveriteAction.WARN).build());

        // Imputation : nommer le traitant, ou rendre le dossier à la qualité si la structure
        // désignée n'est pas la bonne.
        imputation.getTransitions().add(WorkflowTransition.builder()
                .fromStep(imputation).toStep(traitement).decision(StepDecision.APPROUVE).label("Imputer à l'agent")
                .icon("pi pi-user-edit").severity(SeveriteAction.INFO).build());
        imputation.getTransitions().add(WorkflowTransition.builder()
                .fromStep(imputation).toStep(validationRq).decision(StepDecision.REJETE)
                .label("Contester l'affectation")
                .icon("pi pi-undo").severity(SeveriteAction.WARN).build());

        // Traitement : réservé au titulaire. Il soumet son analyse, ou rend l'imputation.
        traitement.getTransitions().add(WorkflowTransition.builder()
                .fromStep(traitement).toStep(validation).decision(StepDecision.APPROUVE).label("Soumettre le traitement")
                .requiredRole(HABILITATION_TITULAIRE)
                .icon("pi pi-send").severity(SeveriteAction.INFO).build());
        traitement.getTransitions().add(WorkflowTransition.builder()
                .fromStep(traitement).toStep(imputation).decision(StepDecision.REJETE).label("Rendre l'imputation")
                .requiredRole(HABILITATION_TITULAIRE)
                .icon("pi pi-reply").severity(SeveriteAction.WARN).build());

        // Le pilote ne valide que des actions dont le responsable est nommé. L'agent imputé peut
        // l'avoir désigné en écrivant l'action, le pilote peut le désigner ou le corriger ici — mais
        // une action sans responsable ne serait confiée à personne à la validation qualité, et le
        // dossier attendrait indéfiniment le solde d'une action que nul n'a commencée.
        validation.getTransitions().add(WorkflowTransition.builder()
                .fromStep(validation).toStep(validationRs).decision(StepDecision.APPROUVE).label("Valider pour le RQ")
                .conditionRequise(FAIT_PLANS_ACTION_AFFECTES)
                .conditionLibelle("chaque action corrective du dossier aura un responsable désigné")
                .icon("pi pi-check").severity(SeveriteAction.SUCCESS).build());
        validation.getTransitions().add(WorkflowTransition.builder()
                .fromStep(validation).toStep(traitement).decision(StepDecision.REJETE).label("Retourner pour retraitement")
                .icon("pi pi-undo").severity(SeveriteAction.WARN).build());

        validationRs.getTransitions().add(WorkflowTransition.builder()
                .fromStep(validationRs).toStep(suiviRq).decision(StepDecision.APPROUVE).label("Approuver et suivre")
                .icon("pi pi-check-circle").severity(SeveriteAction.SUCCESS).build());
        validationRs.getTransitions().add(WorkflowTransition.builder()
                .fromStep(validationRs).toStep(traitement).decision(StepDecision.REJETE).label("Renvoyer au traitement")
                .icon("pi pi-times-circle").severity(SeveriteAction.DANGER).build());

        suiviRq.getTransitions().add(WorkflowTransition.builder()
                .fromStep(suiviRq).toStep(cloture).decision(StepDecision.APPROUVE).label("Clôturer la NC")
                // La clôture attend que les actions correctives soient menées : c'est la seule
                // chose qu'un système qualité ne doit pas laisser passer.
                .conditionRequise(FAIT_PLANS_ACTION_SOLDES)
                .conditionLibelle("toutes les actions correctives du dossier ont été réalisées, "
                        + "vérifiées et reconnues efficaces")
                .icon("pi pi-lock").severity(SeveriteAction.SUCCESS).build());
        suiviRq.getTransitions().add(WorkflowTransition.builder()
                .fromStep(suiviRq).toStep(traitement).decision(StepDecision.REJETE)
                .label("Efficacité insuffisante : reprendre")
                .icon("pi pi-replay").severity(SeveriteAction.WARN).build());

        workflow.addStep(soumission);
        workflow.addStep(reception);
        workflow.addStep(validationRq);
        workflow.addStep(imputation);
        workflow.addStep(traitement);
        workflow.addStep(validation);
        workflow.addStep(validationRs);
        workflow.addStep(suiviRq);
        workflow.addStep(cloture);

        return workflow;
    }

    /**
     * Circuit par défaut d'une action corrective.
     *
     * <p>Il suivait trois états — à traiter, traité, rejeté — et son responsable déclarait seul son
     * action faite, ce qui la comptait aussitôt soldée. Deux manques, du point de vue de la
     * qualité : personne ne <b>constatait</b> la réalisation, et personne ne <b>mesurait
     * l'efficacité</b>, alors qu'un critère avait justement été fixé à la définition de l'action
     * pour être confronté au résultat. Une action corrective ne vaut pas par son exécution mais par
     * son effet : c'est ce que demande la norme, et c'est aussi le seul moyen de savoir si la
     * non-conformité ne reviendra pas.</p>
     *
     * <p>D'où quatre temps et trois responsabilités distinctes — celui qui fait n'est pas celui qui
     * constate, et celui qui constate n'est pas celui qui juge de l'effet :</p>
     * <ol>
     *   <li><b>À réaliser</b> — le responsable de l'action, et lui seul ({@code @TITULAIRE}) ;</li>
     *   <li><b>Réalisation à vérifier</b> — le pilote du processus concerné constate que l'action
     *       annoncée a bien été menée ;</li>
     *   <li><b>Efficacité à mesurer</b> — le responsable qualité confronte le résultat au critère
     *       d'efficacité fixé au départ ;</li>
     *   <li><b>Soldée</b> — fin du circuit. C'est seulement ici que l'action compte pour soldée, et
     *       donc que la clôture de la non-conformité peut s'ouvrir.</li>
     * </ol>
     *
     * <p>Chaque rejet renvoie à l'étape de réalisation : une action inefficace n'est pas une action
     * ratée dont on prend acte, c'est une action à reprendre.</p>
     */
    static Workflow circuitPlanActionParDefaut() {
        Workflow workflow = Workflow.builder()
                .nom("Workflow Défaut Plan d'Action")
                .description("Workflow par défaut pour le suivi d'une action corrective, de sa réalisation "
                        + "à la mesure de son efficacité")
                .resourceType("PLAN_ACTION")
                .build();

        // 1. NON_TRAITER — l'action est confiée, elle reste à mener.
        WorkflowStep step1 = WorkflowStep.builder()
                .code("NON_TRAITER")
                .nomEtape("À réaliser")
                .stepOrder(1)
                .etatTraitement("NON_TRAITER")
                .description("Action corrective confiée à son responsable, en attente de réalisation")
                // L'action revient à la personne qui en répond, désignée à l'ouverture du circuit
                // depuis le responsable inscrit sur le plan. Un rôle l'aurait ouverte à tout agent,
                // sur toute action — c'est l'écueil qu'« agent imputé » nous avait déjà valu.
                .responsableRole(HABILITATION_TITULAIRE)
                .emailTemplateCode("emailPlanAction")
                .build();
        // Déclarer une action réalisée, c'est en rendre compte : la cause qu'on a identifiée et ce
        // qu'on a mis en œuvre. Sans ces champs, l'étape ne recueillait rien — le compte rendu était
        // saisi sur un écran et enregistré par un appel séparé, qui n'arrivait qu'après la décision
        // et retombait sur un plan déjà passé à la vérification, où plus rien ne l'accepte. Les deux
        // ne sont demandés qu'à l'approbation : décliner une attribution n'oblige à rendre compte de
        // rien.
        step1.getFields().add(WorkflowStepField.builder().step(step1).fieldName(CHAMP_CAUSES_IDENTIFIEES)
                .fieldLabel("Causes identifiées")
                .type(FieldType.TEXT)
                .decision(StepDecision.APPROUVE)
                .isRequired(true).build());
        step1.getFields().add(WorkflowStepField.builder().step(step1).fieldName(CHAMP_SOLUTIONS_RETENUES)
                .fieldLabel("Solutions retenues")
                .type(FieldType.TEXT)
                .decision(StepDecision.APPROUVE)
                .isRequired(true).build());

        // 2. EN_VERIFICATION — ce que le responsable annonce, un autre le constate.
        WorkflowStep step2 = WorkflowStep.builder()
                .code("EN_VERIFICATION")
                .nomEtape("Réalisation à vérifier")
                .stepOrder(2)
                .etatTraitement("EN_VERIFICATION")
                .description("Le responsable déclare l'action réalisée ; le pilote doit le constater")
                .responsableRole("PILOTE")
                .emailTemplateCode("emailRqPlan")
                .build();

        // 3. EFFICACITE_A_MESURER — l'action a été faite ; a-t-elle produit l'effet attendu ?
        WorkflowStep step3 = WorkflowStep.builder()
                .code("EFFICACITE_A_MESURER")
                .nomEtape("Efficacité à mesurer")
                .stepOrder(3)
                .etatTraitement("EFFICACITE_A_MESURER")
                .description("Le responsable qualité confronte le résultat au critère d'efficacité fixé")
                .responsableRole("RESPONSABLE_QUALITE")
                .emailTemplateCode("emailRqPlan")
                .build();
        // Le critère d'efficacité était recueilli à la définition de l'action et n'était jamais
        // confronté à rien. Ce constat est ce qui distingue une action close d'une action efficace.
        step3.getFields().add(WorkflowStepField.builder().step(step3).fieldName(CHAMP_CONSTAT_EFFICACITE)
                .fieldLabel("Constat d'efficacité")
                .type(FieldType.TEXT)
                .decision(StepDecision.APPROUVE)
                .isRequired(true).build());

        // 4. TRAITER — fin de circuit. C'est ici, et pas avant, que l'action est soldée.
        WorkflowStep step4 = WorkflowStep.builder()
                .code("TRAITER")
                .nomEtape("Soldée")
                .stepOrder(4)
                .etatTraitement("TRAITER")
                .description("Action réalisée et reconnue efficace : elle ne retient plus la clôture du dossier")
                .responsableRole("RESPONSABLE_QUALITE")
                .emailTemplateCode("emailRqPlan")
                .build();

        // 5. REJECTED — l'attribution a été déclinée, l'action attend un autre responsable.
        WorkflowStep step5 = WorkflowStep.builder()
                .code("REJECTED")
                .nomEtape("Déclinée")
                .stepOrder(5)
                .etatTraitement("REJECTED")
                .description("Attribution déclinée : l'action attend d'être confiée à quelqu'un d'autre")
                .responsableRole("PILOTE")
                .emailTemplateCode("rejectPlanAction")
                // Ré-attribuer, c'est nommer un nouveau responsable : sans cela l'action repartait
                // vers celui-là même qui venait de la décliner, et tournait en rond.
                .champTitulaire(CHAMP_RESPONSABLE_PLAN)
                .build();
        step5.getFields().add(WorkflowStepField.builder().step(step5).fieldName(CHAMP_RESPONSABLE_PLAN)
                .fieldLabel("Nouveau responsable de l'action")
                .type(FieldType.SELECT)
                .options(SourceDeChoix.UTILISATEURS_DE_MA_STRUCTURE.getCle())
                .decision(StepDecision.APPROUVE)
                .isRequired(true).build());

        // Le responsable réalise son action, ou décline une attribution qui ne le concerne pas.
        step1.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step1).toStep(step2).decision(StepDecision.APPROUVE).label("Déclarer l'action réalisée")
                .requiredRole(HABILITATION_TITULAIRE)
                .icon("pi pi-check").severity(SeveriteAction.SUCCESS).build());
        step1.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step1).toStep(step5).decision(StepDecision.REJETE).label("Décliner l'attribution")
                .requiredRole(HABILITATION_TITULAIRE)
                .icon("pi pi-times-circle").severity(SeveriteAction.DANGER).build());

        // Le pilote constate la réalisation, ou demande que l'action soit reprise.
        step2.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step2).toStep(step3).decision(StepDecision.APPROUVE).label("Confirmer la réalisation")
                .requiredRole("PILOTE")
                .icon("pi pi-verified").severity(SeveriteAction.SUCCESS).build());
        step2.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step2).toStep(step1).decision(StepDecision.REJETE).label("Demander une reprise")
                .requiredRole("PILOTE")
                .icon("pi pi-undo").severity(SeveriteAction.WARN).build());

        // Le responsable qualité juge de l'effet. Une action inefficace n'est pas une action close :
        // elle repart chez son responsable, sans quoi la non-conformité serait soldée sans avoir été
        // traitée — le seul résultat qu'un système qualité ne doit jamais produire.
        step3.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step3).toStep(step4).decision(StepDecision.APPROUVE).label("Action efficace — solder")
                .requiredRole("RESPONSABLE_QUALITE")
                .icon("pi pi-flag").severity(SeveriteAction.SUCCESS).build());
        step3.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step3).toStep(step1).decision(StepDecision.REJETE).label("Efficacité non atteinte — reprendre")
                .requiredRole("RESPONSABLE_QUALITE")
                .icon("pi pi-replay").severity(SeveriteAction.DANGER).build());

        // Une action déclinée n'est pas une action morte : le pilote la réattribue, sans quoi elle
        // resterait indéfiniment à l'écart et la non-conformité ne pourrait jamais être close.
        step5.getTransitions().add(WorkflowTransition.builder()
                .fromStep(step5).toStep(step1).decision(StepDecision.APPROUVE).label("Ré-attribuer")
                .requiredRole("PILOTE")
                .icon("pi pi-refresh").severity(SeveriteAction.INFO).build());

        workflow.addStep(step1);
        workflow.addStep(step2);
        workflow.addStep(step3);
        workflow.addStep(step4);
        workflow.addStep(step5);

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
                // Le demandeur, et lui seul : c'est son objectif qui sera instruit.
                .responsableRole(HABILITATION_CREATEUR)
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
                .decision(StepDecision.APPROUVE).label("Soumettre la demande")
                .icon("pi pi-send").severity(SeveriteAction.INFO).build());

        instruction.getTransitions().add(WorkflowTransition.builder()
                .fromStep(instruction).toStep(decision)
                .decision(StepDecision.APPROUVE).label("Retenir la demande")
                .icon("pi pi-arrow-right").severity(SeveriteAction.INFO).build());
        instruction.getTransitions().add(WorkflowTransition.builder()
                .fromStep(instruction).toStep(soumission)
                .decision(StepDecision.REJETE).label("Renvoyer au demandeur")
                .icon("pi pi-undo").severity(SeveriteAction.WARN).build());

        // Terminale et déclarée comme telle : sans ce marqueur, le moteur ignore la transition et
        // l'aboutissement — remplacement du fichier ou retrait du document — n'aurait pas lieu.
        decision.getTransitions().add(WorkflowTransition.builder()
                .fromStep(decision).terminal(true)
                .decision(StepDecision.APPROUVE).label("Accepter et exécuter")
                .icon("pi pi-check-circle").severity(SeveriteAction.SUCCESS).build());
        decision.getTransitions().add(WorkflowTransition.builder()
                .fromStep(decision).terminal(true)
                .decision(StepDecision.REJETE).label("Refuser la demande")
                .icon("pi pi-times-circle").severity(SeveriteAction.DANGER).build());

        workflow.addStep(soumission);
        workflow.addStep(instruction);
        workflow.addStep(decision);

        return workflow;
    }
}
