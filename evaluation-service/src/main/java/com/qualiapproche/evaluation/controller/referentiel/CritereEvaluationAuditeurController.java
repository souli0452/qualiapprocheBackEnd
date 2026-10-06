package com.qualiapproche.evaluation.controller.referentiel;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.entities.CritereEvaluationAuditeur;
import com.qualiapproche.evaluation.service.referentiel.CritereEvaluationAuditeurService;
import com.qualiapproche.evaluation.service.referentiel.ReferentielSimpleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.evaluation.controller.AuditUrls.CRITERES_EVALUATION_AUDITEUR;

/** Les critères d'évaluation des auditeurs. Se lit avec le module, se paramètre avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(CRITERES_EVALUATION_AUDITEUR)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class CritereEvaluationAuditeurController extends ReferentielSimpleController<CritereEvaluationAuditeur> {

    private final CritereEvaluationAuditeurService service;

    @Override
    protected ReferentielSimpleService<CritereEvaluationAuditeur> service() {
        return service;
    }
}
