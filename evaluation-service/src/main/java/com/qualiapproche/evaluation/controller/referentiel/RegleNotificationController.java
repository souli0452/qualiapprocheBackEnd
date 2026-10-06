package com.qualiapproche.evaluation.controller.referentiel;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.evaluation.entities.RegleNotification;
import com.qualiapproche.evaluation.service.referentiel.RegleNotificationService;
import com.qualiapproche.evaluation.service.referentiel.ReferentielSimpleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.evaluation.controller.AuditUrls.REGLES_NOTIFICATION;

/** Les alertes d'échéance du programme d'audit. Se lit avec le module, se paramètre avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(REGLES_NOTIFICATION)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class RegleNotificationController extends ReferentielSimpleController<RegleNotification> {

    private final RegleNotificationService service;

    @Override
    protected ReferentielSimpleService<RegleNotification> service() {
        return service;
    }
}
