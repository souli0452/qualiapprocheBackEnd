package com.qualiapproche.evaluation.controller.referentiel;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.entities.TypeAudit;
import com.qualiapproche.evaluation.service.referentiel.TypeAuditService;
import com.qualiapproche.evaluation.service.referentiel.ReferentielSimpleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.evaluation.controller.AuditUrls.TYPES_AUDIT;

/** Les types d'audit. Se lit avec le module, se paramètre avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(TYPES_AUDIT)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class TypeAuditController extends ReferentielSimpleController<TypeAudit> {

    private final TypeAuditService service;

    @Override
    protected ReferentielSimpleService<TypeAudit> service() {
        return service;
    }
}
