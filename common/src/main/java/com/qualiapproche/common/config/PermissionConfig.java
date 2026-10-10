package com.qualiapproche.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PermissionConfig {

    @Bean("perm")
    @ConditionalOnMissingBean(name = "perm")
    public PermissionChecker permissionChecker(
            @org.springframework.beans.factory.annotation.Value("${qualisira.permissions.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            org.slf4j.LoggerFactory.getLogger(PermissionConfig.class).error(
                    "QUALISIRA_PERMISSIONS_SECRET absent : les permissions applicatives transmises par la "
                            + "passerelle seront ignorées, seuls les rôles du jeton compteront.");
        }
        return new PermissionChecker(secret);
    }
}
