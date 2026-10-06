package com.qualiapproche.evaluation.controller.referentiel;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.entities.TypeConstat;
import com.qualiapproche.evaluation.service.referentiel.TypeConstatService;
import com.qualiapproche.evaluation.service.referentiel.ReferentielSimpleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.evaluation.controller.AuditUrls.TYPES_CONSTAT;

/** Les natures de constat, et celles qui génèrent une non-conformité. Se lit avec le module, se paramètre avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(TYPES_CONSTAT)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class TypeConstatController extends ReferentielSimpleController<TypeConstat> {

    private final TypeConstatService service;

    @Override
    protected ReferentielSimpleService<TypeConstat> service() {
        return service;
    }
}
