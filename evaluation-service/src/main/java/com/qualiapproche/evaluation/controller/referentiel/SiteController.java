package com.qualiapproche.evaluation.controller.referentiel;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.entities.Site;
import com.qualiapproche.evaluation.service.referentiel.SiteService;
import com.qualiapproche.evaluation.service.referentiel.ReferentielSimpleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.evaluation.controller.AuditUrls.SITES;

/** Les sites où se déroulent les audits. Se lit avec le module, se paramètre avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(SITES)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class SiteController extends ReferentielSimpleController<Site> {

    private final SiteService service;

    @Override
    protected ReferentielSimpleService<Site> service() {
        return service;
    }
}
