package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.SiteDto;
import com.qualiapproche.referentiel.service.impl.ParametrageParDirection;
import com.qualiapproche.referentiel.service.impl.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.qualiapproche.common.utils.ApiUrls.SITE_ROOT_URL;

/** Les sites de l'organisation, où se déroulent notamment les audits. Se lit avec le module, se paramètre avec {@code audit-config}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(SITE_ROOT_URL)
@RequirePermissions(
        create = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        update = {"audit-config", "CONFIG_GLOBAL_MANAGE"},
        read = {"audit-read", "audit-write", "audit-conduct", "audit-config", "CONFIG_READ"},
        delete = {"audit-config", "CONFIG_GLOBAL_MANAGE"}
)
public class SiteController extends ParametrageParDirectionController<SiteDto> {

    private final SiteService service;

    @Override
    protected ParametrageParDirection<?, SiteDto> service() {
        return service;
    }
}
