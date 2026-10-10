package com.qualiapproche.evaluation.client;

import com.qualiapproche.common.dto.CritereEvaluationAuditeurDto;
import com.qualiapproche.common.dto.NiveauEvaluationAuditeurDto;
import com.qualiapproche.common.dto.SiteDto;
import com.qualiapproche.evaluation.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.qualiapproche.common.utils.ApiUrls.CRITERE_EVALUATION_AUDITEUR_ROOT_URL;
import static com.qualiapproche.common.utils.ApiUrls.NIVEAU_EVALUATION_AUDITEUR_ROOT_URL;
import static com.qualiapproche.common.utils.ApiUrls.SITE_ROOT_URL;

/**
 * Le paramétrage que le module lit dans referentiel-service, sous le jeton de l'appelant : celui
 * de sa direction, et rien au-delà.
 *
 * <p>{@code contextId} distingue ce client de {@link StructuresClient}, qui vise le même service :
 * sans lui, les deux se disputeraient le même nom de bean.</p>
 */
@FeignClient(name = "referentiel-service", contextId = "parametrage", configuration = FeignConfig.class)
public interface ParametrageClient {

    @GetMapping(SITE_ROOT_URL + "/all")
    List<SiteDto> sites(@RequestParam("inactifs") boolean inactifs);

    @GetMapping(CRITERE_EVALUATION_AUDITEUR_ROOT_URL + "/all")
    List<CritereEvaluationAuditeurDto> criteres(@RequestParam("inactifs") boolean inactifs);

    @GetMapping(NIVEAU_EVALUATION_AUDITEUR_ROOT_URL + "/all")
    List<NiveauEvaluationAuditeurDto> niveaux(@RequestParam("inactifs") boolean inactifs);
}
