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
}
