package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.entities.RegleEcartRQAPBF;
import com.qualiapproche.evaluation.service.ParametrageSimpleService;
import com.qualiapproche.evaluation.service.RegleEcartRQAPBFService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.evaluation.controller.AuditUrls.REGLES_ECARTS_RQAPBF;

/** Les niveaux RQAP-BF qui ouvrent une non-conformité. Se lit avec le module, se règle avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(REGLES_ECARTS_RQAPBF)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class RegleEcartRQAPBFController extends ParametrageSimpleController<RegleEcartRQAPBF> {

    private final RegleEcartRQAPBFService service;

    @Override
    protected ParametrageSimpleService<RegleEcartRQAPBF> service() {
        return service;
    }
}
