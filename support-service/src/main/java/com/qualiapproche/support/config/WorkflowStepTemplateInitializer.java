package com.qualiapproche.support.config;

import com.qualiapproche.common.referentiel.CatalogueEtapesStandard;
import com.qualiapproche.common.referentiel.CatalogueEtapesStandard.EtapeStandard;
import com.qualiapproche.support.model.WorkflowStepTemplate;
import com.qualiapproche.support.repository.WorkflowStepTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sème le catalogue d'étapes réutilisables ({@code qms_workflow_step_templates}) avec les étapes
 * des circuits livrés par défaut — celles décrites par {@link CatalogueEtapesStandard}.
 *
 * <p>Le rapprochement se fait par code, et l'insertion est la seule opération pratiquée : une
 * entrée déjà présente n'est jamais réécrite. Libellé, rôle responsable et description sont
 * modifiables depuis l'écran de configuration, et un redémarrage n'a pas à défaire ce qu'un
 * administrateur a réglé. Une entrée supprimée à dessein réapparaîtra en revanche au démarrage
 * suivant : c'est le prix de la reprise automatique, et la suppression d'une étape standard reste
 * un geste rare.</p>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WorkflowStepTemplateInitializer implements CommandLineRunner {

    private final WorkflowStepTemplateRepository stepTemplateRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<EtapeStandard> catalogue = CatalogueEtapesStandard.charger();

        int creees = 0;
        for (EtapeStandard etape : catalogue) {
            if (stepTemplateRepository.findByCode(etape.code()).isPresent()) {
                continue;
            }
            stepTemplateRepository.save(WorkflowStepTemplate.builder()
                    .code(etape.code())
                    .nomEtape(etape.nomEtape())
                    .responsableRole(etape.responsableRole())
                    .description(etape.description())
                    .build());
            creees++;
        }

        if (creees > 0) {
            log.info("Catalogue d'étapes : {} étape(s) standard ajoutée(s) sur {} décrite(s).",
                    creees, catalogue.size());
        } else {
            log.info("Catalogue d'étapes : les {} étapes standard sont déjà présentes.",
                    catalogue.size());
        }
    }
}
