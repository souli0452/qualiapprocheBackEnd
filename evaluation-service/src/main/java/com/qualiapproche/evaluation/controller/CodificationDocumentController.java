package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.entities.CodificationDocument;
import com.qualiapproche.evaluation.service.CodificationDocumentService;
import com.qualiapproche.evaluation.service.ParametrageSimpleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.evaluation.controller.AuditUrls.CODIFICATIONS;

/** La numérotation des audits et de leurs plans. Se lit avec le module, se paramètre avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(CODIFICATIONS)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class CodificationDocumentController extends ParametrageSimpleController<CodificationDocument> {

    private final CodificationDocumentService service;

    @Override
    protected ParametrageSimpleService<CodificationDocument> service() {
        return service;
    }
}
