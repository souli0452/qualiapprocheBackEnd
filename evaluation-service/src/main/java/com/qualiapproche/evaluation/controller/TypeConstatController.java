package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.entities.TypeConstat;
import com.qualiapproche.evaluation.service.TypeConstatService;
import com.qualiapproche.evaluation.service.ParametrageSimpleService;
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
        // audit-validate : le valideur lit la compilation des constats, qu'il faut regrouper par nature.
        read = {"audit-read", "audit-write", "audit-conduct", "audit-validate", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class TypeConstatController extends ParametrageSimpleController<TypeConstat> {

    private final TypeConstatService service;

    @Override
    protected ParametrageSimpleService<TypeConstat> service() {
        return service;
    }
}
