package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.NiveauEvaluationAuditeurDto;
import com.qualiapproche.referentiel.service.impl.ParametrageParDirection;
import com.qualiapproche.referentiel.service.impl.NiveauEvaluationAuditeurService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.common.utils.ApiUrls.NIVEAU_EVALUATION_AUDITEUR_ROOT_URL;

/** L'échelle d'évaluation des auditeurs, lue par le module Audit. Se lit avec le module, se paramètre avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(NIVEAU_EVALUATION_AUDITEUR_ROOT_URL)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class NiveauEvaluationAuditeurController extends ParametrageParDirectionController<NiveauEvaluationAuditeurDto> {

    private final NiveauEvaluationAuditeurService service;

    @Override
    protected ParametrageParDirection<?, NiveauEvaluationAuditeurDto> service() {
        return service;
    }
}
