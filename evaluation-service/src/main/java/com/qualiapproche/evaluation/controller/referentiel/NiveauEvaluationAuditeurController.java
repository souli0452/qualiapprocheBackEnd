package com.qualiapproche.evaluation.controller.referentiel;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.entities.NiveauEvaluationAuditeur;
import com.qualiapproche.evaluation.service.referentiel.NiveauEvaluationAuditeurService;
import com.qualiapproche.evaluation.service.referentiel.ReferentielSimpleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.evaluation.controller.AuditUrls.NIVEAUX_EVALUATION_AUDITEUR;

/** L'échelle d'évaluation des auditeurs. Se lit avec le module, se paramètre avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(NIVEAUX_EVALUATION_AUDITEUR)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class NiveauEvaluationAuditeurController extends ReferentielSimpleController<NiveauEvaluationAuditeur> {

    private final NiveauEvaluationAuditeurService service;

    @Override
    protected ReferentielSimpleService<NiveauEvaluationAuditeur> service() {
        return service;
    }
}
