package com.qualiapproche.workflow.config;

import com.qualiapproche.workflow.model.FieldType;
import com.qualiapproche.workflow.model.SeveriteAction;
import com.qualiapproche.workflow.model.StepDecision;
import com.qualiapproche.workflow.model.Workflow;
import com.qualiapproche.workflow.model.WorkflowStep;
import com.qualiapproche.workflow.model.WorkflowStepField;
import com.qualiapproche.workflow.model.WorkflowTransition;
import com.qualiapproche.workflow.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le rattrapage des circuits déjà en base, quand le circuit livré s'enrichit.
 *
 * <p>L'initialiseur ne crée un circuit que si aucun n'existe : toute étape ajoutée après la mise en
 * service restait invisible partout sauf sur une base vierge, et la seule issue était de supprimer
 * le circuit — c'est-à-dire de perdre l'historique de tous les dossiers qu'il pilote.</p>
 */
class RattrapageDesCircuitsLivresTest {

    private WorkflowRepository workflowRepository;
    private RattrapageDesCircuitsLivres rattrapage;

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);
        rattrapage = new RattrapageDesCircuitsLivres(workflowRepository);
        when(workflowRepository.findByResourceType(any())).thenReturn(List.of());
    }

    private void enBase(String typeRessource, Workflow circuit) {
        when(workflowRepository.findByResourceType(typeRessource)).thenReturn(List.of(circuit));
    }

    private WorkflowStep etape(Workflow circuit, String code) {
        return circuit.getSteps().stream()
                .filter(s -> code.equals(s.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Étape absente : " + code));
    }

    /** Le circuit livré, amputé d'une étape et de la route qui la traverse : une base d'avant. */
    private Workflow circuitNonConformiteDAvant() {
        Workflow circuit = WorkflowDataInitializer.circuitNonConformiteParDefaut();
        WorkflowStep validationRq = etape(circuit, "VALIDATION_RQ");
        WorkflowStep imputation = etape(circuit, "IMPUTATION");

        // La réception menait directement à l'imputation, sans validation qualité.
        etape(circuit, "RECEPTION").getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.APPROUVE)
                .forEach(t -> t.setToStep(imputation));
        circuit.getSteps().remove(validationRq);
        return circuit;
    }

    @Test
    @DisplayName("Une étape absente du circuit en base y est ajoutée")
    void etapeManquante_ajoutee() {
        Workflow circuit = circuitNonConformiteDAvant();
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        assertThat(circuit.getSteps()).extracting(WorkflowStep::getCode).contains("VALIDATION_RQ");
        verify(workflowRepository).save(circuit);
    }

    @Test
    @DisplayName("Les routes existantes passent par l'étape ajoutée au lieu de l'enjamber")
    void routes_traversentLEtapeAjoutee() {
        Workflow circuit = circuitNonConformiteDAvant();
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // Sans cela l'étape serait dans le circuit, visible dans l'éditeur, et jamais atteinte par
        // un seul dossier.
        WorkflowTransition approbation = etape(circuit, "RECEPTION").getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.APPROUVE)
                .findFirst().orElseThrow();
        assertThat(approbation.getToStep().getCode()).isEqualTo("VALIDATION_RQ");
    }

    @Test
    @DisplayName("Un champ ajouté au circuit livré atteint les circuits déjà en base")
    void champManquant_ajoute() {
        // Une étape complète mais sans point de saisie laisse passer la décision et perd ce qu'elle
        // devait justifier : le compte rendu du responsable n'irait nulle part.
        Workflow circuit = WorkflowDataInitializer.circuitPlanActionParDefaut();
        etape(circuit, "NON_TRAITER").getFields().clear();
        enBase("PLAN_ACTION", circuit);

        rattrapage.run();

        assertThat(etape(circuit, "NON_TRAITER").getFields())
                .extracting(WorkflowStepField::getFieldName)
                .contains("causeIdentifiees", "solutionRetenues");
        verify(workflowRepository).save(circuit);
    }

    @Test
    @DisplayName("Un champ déjà porté par l'étape n'est pas redéfini")
    void champExistant_preserve() {
        Workflow circuit = WorkflowDataInitializer.circuitPlanActionParDefaut();
        WorkflowStep aRealiser = etape(circuit, "NON_TRAITER");
        aRealiser.getFields().removeIf(f -> f.getFieldName().equals("solutionRetenues"));
        WorkflowStepField personnalise = aRealiser.getFields().stream()
                .filter(f -> f.getFieldName().equals("causeIdentifiees"))
                .findFirst().orElseThrow();
        personnalise.setFieldLabel("Origine du problème");
        personnalise.setRequired(false);
        enBase("PLAN_ACTION", circuit);

        rattrapage.run();

        // L'administrateur a pu changer le libellé ou la portée : ce n'est pas à un rattrapage de
        // défaire son choix. Seul ce qui manque est ajouté.
        assertThat(personnalise.getFieldLabel()).isEqualTo("Origine du problème");
        assertThat(personnalise.isRequired()).isFalse();
        assertThat(aRealiser.getFields())
                .extracting(WorkflowStepField::getFieldName)
                .contains("solutionRetenues");
    }

    @Test
    @DisplayName("Un circuit déjà complet n'est pas réenregistré")
    void circuitComplet_intact() {
        enBase("NON_CONFORMITE", WorkflowDataInitializer.circuitNonConformiteParDefaut());
        enBase("PLAN_ACTION", WorkflowDataInitializer.circuitPlanActionParDefaut());

        rattrapage.run();

        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un libellé personnalisé n'est pas défait")
    void personnalisation_preservee() {
        Workflow circuit = circuitNonConformiteDAvant();
        WorkflowTransition approbation = etape(circuit, "RECEPTION").getTransitions().stream()
                .filter(t -> t.getDecision() == StepDecision.APPROUVE)
                .findFirst().orElseThrow();
        approbation.setLabel("Prendre en charge");
        approbation.setSeverity(SeveriteAction.INFO);
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // La route est la structure du circuit et doit suivre ; l'apparence du bouton est le choix
        // de l'administrateur et n'a pas à être défait.
        assertThat(approbation.getLabel()).isEqualTo("Prendre en charge");
        assertThat(approbation.getSeverity()).isEqualTo(SeveriteAction.INFO);
    }

    @Test
    @DisplayName("Un circuit recomposé à la main est laissé intact")
    void circuitRecompose_intact() {
        Workflow circuit = circuitNonConformiteDAvant();
        circuit.addStep(WorkflowStep.builder()
                .code("ARBITRAGE_DIRECTION").nomEtape("Arbitrage").stepOrder(20)
                .responsableRole("PILOTE").build());
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // Y greffer les étapes livrées mêlerait deux conceptions du circuit, et le résultat ne
        // serait celui de personne.
        assertThat(circuit.getSteps()).extracting(WorkflowStep::getCode).doesNotContain("VALIDATION_RQ");
        verify(workflowRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une étape de traitement ouverte à un rôle est rendue à son titulaire")
    void traitementOuvertAUnRole_rendueAuTitulaire() {
        Workflow circuit = WorkflowDataInitializer.circuitPlanActionParDefaut();
        WorkflowStep aTraiter = etape(circuit, "NON_TRAITER");
        aTraiter.setResponsableRole("AGENT");
        aTraiter.getTransitions().forEach(t -> t.setRequiredRole(null));
        enBase("PLAN_ACTION", circuit);

        rattrapage.run();

        // Ouverte au rôle, l'étape laissait tout agent solder le plan d'un autre, et par là ouvrir
        // la clôture d'une non-conformité qui ne le concernait pas.
        assertThat(aTraiter.getResponsableRole()).isEqualTo(WorkflowDataInitializer.HABILITATION_TITULAIRE);
        assertThat(aTraiter.getTransitions())
                .allSatisfy(t -> assertThat(t.getRequiredRole())
                        .isEqualTo(WorkflowDataInitializer.HABILITATION_TITULAIRE));
        verify(workflowRepository).save(circuit);
    }

    @Test
    @DisplayName("Un champ que le circuit livré ne demande plus est retiré des bases en service")
    void champAbandonne_retire() {
        Workflow circuit = WorkflowDataInitializer.circuitNonConformiteParDefaut();
        WorkflowStep traitement = etape(circuit, "TRAITEMENT");
        traitement.getFields().add(WorkflowStepField.builder().step(traitement)
                .fieldName("actionPreventive").fieldLabel("Action préventive proposée")
                .type(FieldType.TEXT).isRequired(true).build());
        WorkflowStep validation = etape(circuit, "VALIDATION");
        validation.getFields().add(WorkflowStepField.builder().step(validation)
                .fieldName("pertinancePilote").fieldLabel("Pertinence de l'action")
                .type(FieldType.TEXT).isRequired(true).build());
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // Le rattrapage ne complétant que ce qui manque, un champ retiré du circuit livré restait
        // en place sur toutes les bases en service — et, obligatoire, continuait d'être exigé à
        // chaque décision.
        assertThat(traitement.getFields()).extracting(WorkflowStepField::getFieldName)
                .doesNotContain("actionPreventive");
        assertThat(validation.getFields()).extracting(WorkflowStepField::getFieldName)
                .doesNotContain("pertinancePilote");
        verify(workflowRepository).save(circuit);
    }

    @Test
    @DisplayName("Le retrait est nommé : un champ ajouté depuis l'éditeur n'est pas emporté")
    void champPersonnalise_conserve() {
        Workflow circuit = WorkflowDataInitializer.circuitNonConformiteParDefaut();
        WorkflowStep traitement = etape(circuit, "TRAITEMENT");
        traitement.getFields().add(WorkflowStepField.builder().step(traitement)
                .fieldName("impactBudgetaire").fieldLabel("Impact budgétaire")
                .type(FieldType.TEXT).isRequired(false).build());
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // Seuls les champs abandonnés par le circuit livré disparaissent : le reste appartient à
        // l'administrateur qui l'a posé.
        assertThat(traitement.getFields()).extracting(WorkflowStepField::getFieldName)
                .contains("impactBudgetaire");
    }

    @Test
    @DisplayName("Une étape qui doit nommer quelqu'un reçoit le champ qui le désigne")
    void champTitulaire_ajoute() {
        Workflow circuit = WorkflowDataInitializer.circuitPlanActionParDefaut();
        WorkflowStep rejete = etape(circuit, "REJECTED");
        String champ = rejete.getChampTitulaire();
        rejete.setChampTitulaire(null);
        rejete.getFields().clear();
        enBase("PLAN_ACTION", circuit);

        rattrapage.run();

        // Sans le champ, l'étape serait réservée à une personne que rien ne permet de nommer, et
        // deviendrait indécidable.
        assertThat(rejete.getChampTitulaire()).isEqualTo(champ);
        assertThat(rejete.getFields()).extracting(f -> f.getFieldName()).contains(champ);
    }

    /**
     * Le circuit tel qu'il était avant que la validation qualité n'offre deux issues : une seule
     * action pour approuver, nommée d'après sa décision par {@code RattrapageDesActionsDEtape}.
     */
    private Workflow circuitAvantLaClotureDirecte() {
        Workflow circuit = WorkflowDataInitializer.circuitNonConformiteParDefaut();
        WorkflowStep validationRq = etape(circuit, "VALIDATION_RQ");

        validationRq.getTransitions().removeIf(t -> WorkflowDataInitializer.ACTION_CLOTURER_SANS_SUITE
                .equals(t.getCode()));
        validationRq.getTransitions().stream()
                .filter(t -> WorkflowDataInitializer.ACTION_VALIDER_ET_ORIENTER.equals(t.getCode()))
                .forEach(t -> t.setCode("APPROUVE"));
        validationRq.getFields().removeIf(champ ->
                WorkflowDataInitializer.CHAMP_CIRCUIT_TRAITEMENT.equals(champ.getFieldName())
                        || WorkflowDataInitializer.CHAMP_MOTIF_CLOTURE_DIRECTE.equals(champ.getFieldName()));
        validationRq.getFields().forEach(champ -> champ.setActionCode(null));
        return circuit;
    }

    @Test
    @DisplayName("Une action renommée est renommée, non dupliquée")
    void actionRenommee_pasDeDoublon() {
        Workflow circuit = circuitAvantLaClotureDirecte();
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // Une action est reconnue par son code. Sans renommage préalable, l'action livrée sous son
        // nouveau nom passerait pour absente et serait ajoutée : le dossier offrirait deux boutons
        // menant à l'imputation, dont un seul réclamerait les saisies attendues.
        assertThat(etape(circuit, "VALIDATION_RQ").getTransitions())
                .filteredOn(t -> t.getToStep() != null && "IMPUTATION".equals(t.getToStep().getCode()))
                .singleElement()
                .satisfies(t -> assertThat(t.getCode())
                        .isEqualTo(WorkflowDataInitializer.ACTION_VALIDER_ET_ORIENTER));
    }

    @Test
    @DisplayName("La clôture sans suite et ses champs arrivent sur un circuit en service")
    void clotureDirecte_ajoutee() {
        Workflow circuit = circuitAvantLaClotureDirecte();
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        WorkflowStep validationRq = etape(circuit, "VALIDATION_RQ");
        assertThat(validationRq.getTransitions())
                .filteredOn(t -> WorkflowDataInitializer.ACTION_CLOTURER_SANS_SUITE.equals(t.getCode()))
                .singleElement()
                .satisfies(t -> assertThat(t.getToStep().getCode()).isEqualTo("CLOTURE"));

        assertThat(validationRq.getFields()).extracting(WorkflowStepField::getFieldName)
                .contains(WorkflowDataInitializer.CHAMP_CIRCUIT_TRAITEMENT,
                        WorkflowDataInitializer.CHAMP_MOTIF_CLOTURE_DIRECTE);
    }

    @Test
    @DisplayName("Les champs de l'orientation cessent d'être exigés de la clôture sans suite")
    void champsExistants_rattachesALeurAction() {
        Workflow circuit = circuitAvantLaClotureDirecte();
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // Sans portée, le processus destinataire — obligatoire — aurait été réclamé des deux
        // actions : il serait devenu impossible de clore sans désigner à qui l'on transmet un
        // dossier qu'on vient de décider de ne transmettre à personne.
        assertThat(etape(circuit, "VALIDATION_RQ").getFields())
                .filteredOn(champ -> WorkflowDataInitializer.CHAMP_STRUCTURE_DESTINATAIRE_ID
                        .equals(champ.getFieldName()))
                .singleElement()
                .satisfies(champ -> assertThat(champ.getActionCode())
                        .isEqualTo(WorkflowDataInitializer.ACTION_VALIDER_ET_ORIENTER));
    }

    @Test
    @DisplayName("Un compte rendu devenu facultatif cesse d'être exigé")
    void champDevenuFacultatif_libere() {
        Workflow circuit = WorkflowDataInitializer.circuitPlanActionParDefaut();
        WorkflowStep aRealiser = etape(circuit, "NON_TRAITER");
        aRealiser.getFields().forEach(champ -> champ.setRequired(true));
        enBase("PLAN_ACTION", circuit);

        rattrapage.run();

        // La cause et la solution retenue sont désormais posées à la proposition du plan. Laissées
        // obligatoires ici, elles empêchaient le responsable de déclarer son action réalisée tant
        // qu'il n'aurait pas recopié ce que le dossier portait déjà.
        assertThat(aRealiser.getFields())
                .filteredOn(champ -> List.of("causeIdentifiees", "solutionRetenues")
                        .contains(champ.getFieldName()))
                .isNotEmpty()
                .allSatisfy(champ -> assertThat(champ.isRequired()).isFalse());
    }

    @Test
    @DisplayName("Les étapes reçoivent leur propre message, sauf celui qu'un administrateur a choisi")
    void gabarits_alignesSaufChoixExplicite() {
        Workflow circuit = WorkflowDataInitializer.circuitNonConformiteParDefaut();
        // Ce que porte une base en service : les modèles génériques d'avant.
        etape(circuit, "SOUMISSION").setEmailTemplateCode("emailTemplate");
        etape(circuit, "RECEPTION").setEmailTemplateCode("structureToStructure");
        // Et un gabarit rédigé sur place, qui n'appartient pas au circuit livré.
        etape(circuit, "IMPUTATION").setEmailTemplateCode("monGabaritMaison");
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        assertThat(etape(circuit, "SOUMISSION").getEmailTemplateCode())
                .isEqualTo("ncRenvoyeeAuDeclarant");
        assertThat(etape(circuit, "RECEPTION").getEmailTemplateCode()).isEqualTo("ncRecue");
        assertThat(etape(circuit, "IMPUTATION").getEmailTemplateCode())
                .as("un gabarit choisi par un administrateur n'est pas défait par un rattrapage")
                .isEqualTo("monGabaritMaison");
    }

    @Test
    @DisplayName("La clôture apprend à qui elle doit s'adresser")
    void destinataireDeLaCloture_ajoute() {
        Workflow circuit = WorkflowDataInitializer.circuitNonConformiteParDefaut();
        WorkflowStep cloture = etape(circuit, "CLOTURE");
        cloture.setDestinataireCourriel(null);
        enBase("NON_CONFORMITE", circuit);

        rattrapage.run();

        // Sans elle, la clôture était annoncée au responsable qualité qui venait de la prononcer, et
        // non au pilote du processus qui avait signalé l'écart et attendait d'apprendre ce qu'il
        // était devenu.
        assertThat(cloture.getDestinataireCourriel()).isEqualTo("PILOTE@STRUCTURE_EMETTRICE");
    }
    // ------------------------------------------------------- les circuits documentaires

    @Test
    @DisplayName("Un circuit documentaire en service cesse d'annoncer ses étapes en termes de non-conformité")
    void circuitDocumentaire_gabaritsReadresses() {
        Workflow enService = WorkflowDataInitializer.circuitDocumentParDefaut();
        // La base d'avant : les trois étapes empruntaient les gabarits génériques des
        // non-conformités, et le rédacteur d'une procédure recevait « Nouvelle Non-Conformité
        // imputée ». L'initialiseur ne recréant les circuits que sur une base vierge, aucune
        // installation en service n'aurait vu les gabarits neufs.
        etape(enService, "REDACTION").setEmailTemplateCode("emailTemplate");
        etape(enService, "REDACTION").setDestinataireCourriel(null);
        etape(enService, "VERIFICATION").setEmailTemplateCode("structureToStructure");
        etape(enService, "APPROBATION").setEmailTemplateCode("validationRq");
        enBase("DOCUMENT", enService);

        rattrapage.run();

        assertThat(etape(enService, "REDACTION").getEmailTemplateCode())
                .isEqualTo("documentRenvoyeAuRedacteur");
        assertThat(etape(enService, "REDACTION").getDestinataireCourriel()).isEqualTo("@CREATEUR");
        assertThat(etape(enService, "VERIFICATION").getEmailTemplateCode())
                .isEqualTo("documentAVerifier");
        assertThat(etape(enService, "APPROBATION").getEmailTemplateCode())
                .isEqualTo("documentAApprouver");
        verify(workflowRepository).save(enService);
    }

    @Test
    @DisplayName("Un gabarit choisi par un administrateur n'est pas remplacé")
    void circuitDocumentaire_gabaritChoisiConserve() {
        Workflow enService = WorkflowDataInitializer.circuitDocumentParDefaut();
        etape(enService, "VERIFICATION").setEmailTemplateCode("monGabaritMaison");
        etape(enService, "APPROBATION").setEmailTemplateCode("validationRq");
        enBase("DOCUMENT", enService);

        rattrapage.run();

        // Seuls les anciens codes livrés sont repris : le corps d'un courriel appartient à qui l'a
        // choisi, et le rattrapage n'a pas à défaire ce choix.
        assertThat(etape(enService, "VERIFICATION").getEmailTemplateCode())
                .isEqualTo("monGabaritMaison");
        assertThat(etape(enService, "APPROBATION").getEmailTemplateCode())
                .isEqualTo("documentAApprouver");
    }

    @Test
    @DisplayName("Le rattrapage documentaire ne touche ni aux étapes ni aux routes du circuit")
    void circuitDocumentaire_formeIntacte() {
        Workflow enService = WorkflowDataInitializer.circuitDocumentParDefaut();
        // Un circuit dont l'administrateur a retiré l'approbation : le rattrapage complet la lui
        // rendrait, ce qui reviendrait à défaire une décision prise en connaissance de cause. Seul
        // l'adressage des courriels est repris sur ces circuits.
        enService.getSteps().remove(etape(enService, "APPROBATION"));
        etape(enService, "VERIFICATION").setEmailTemplateCode("structureToStructure");
        enBase("DOCUMENT", enService);

        rattrapage.run();

        assertThat(enService.getSteps()).hasSize(2);
        assertThat(etape(enService, "VERIFICATION").getEmailTemplateCode())
                .isEqualTo("documentAVerifier");
    }

    @Test
    @DisplayName("Un circuit documentaire déjà à jour n'est pas réenregistré")
    void circuitDocumentaire_dejaAJour_pasDEcriture() {
        enBase("DOCUMENT", WorkflowDataInitializer.circuitDocumentParDefaut());

        rattrapage.run();

        verify(workflowRepository, never()).save(any());
    }

}
