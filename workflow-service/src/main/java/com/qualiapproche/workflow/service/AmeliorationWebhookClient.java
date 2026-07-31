package com.qualiapproche.workflow.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "amelioration-service")
public interface AmeliorationWebhookClient {

    @PostMapping("/api/v1/internal/callbacks/non-conformites/{ncId}/status")
    void updateNonConformiteStatus(@PathVariable("ncId") UUID ncId, @RequestBody Map<String, Object> payload);

    /**
     * Le point de rappel côté amelioration-service existait déjà mais n'était appelé par personne :
     * les instances de workflow PLAN_ACTION étaient créées puis jamais reflétées dans le plan d'action.
     */
    @PostMapping("/api/v1/internal/callbacks/plan-actions/{paId}/status")
    void updatePlanActionStatus(@PathVariable("paId") UUID paId, @RequestBody Map<String, Object> payload);
}
