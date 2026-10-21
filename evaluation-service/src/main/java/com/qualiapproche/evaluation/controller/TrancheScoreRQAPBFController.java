package com.qualiapproche.evaluation.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.entities.TrancheScoreRQAPBF;
import com.qualiapproche.evaluation.service.GrilleDesMentionsRQAPBF;
import com.qualiapproche.evaluation.service.ParametrageSimpleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.evaluation.controller.AuditUrls.TRANCHES_SCORE_RQAPBF;

/** La grille des mentions RQAP-BF. Se lit avec le module, se règle avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(TRANCHES_SCORE_RQAPBF)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class TrancheScoreRQAPBFController extends ParametrageSimpleController<TrancheScoreRQAPBF> {

    private final GrilleDesMentionsRQAPBF service;

    @Override
    protected ParametrageSimpleService<TrancheScoreRQAPBF> service() {
        return service;
    }
}
