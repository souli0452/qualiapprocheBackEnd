package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.CritereEvaluationAuditeurDto;
import com.qualiapproche.referentiel.service.impl.ParametrageParDirection;
import com.qualiapproche.referentiel.service.impl.CritereEvaluationAuditeurService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.common.utils.ApiUrls.CRITERE_EVALUATION_AUDITEUR_ROOT_URL;

/** Les critères d'évaluation des auditeurs, lus par le module Audit. Se lit avec le module, se paramètre avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(CRITERE_EVALUATION_AUDITEUR_ROOT_URL)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class CritereEvaluationAuditeurController extends ParametrageParDirectionController<CritereEvaluationAuditeurDto> {

    private final CritereEvaluationAuditeurService service;

    @Override
    protected ParametrageParDirection<?, CritereEvaluationAuditeurDto> service() {
        return service;
    }
}
