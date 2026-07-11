package com.qualiapproche.common.config;

import com.qualiapproche.common.annotation.RequirePermissions;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PermissionChecker {

    public boolean canCreate(final Object controller) {
        return check(controller, RequirePermissions::create);
    }

    public boolean canUpdate(final Object controller) {
        return check(controller, RequirePermissions::update);
    }

    public boolean canRead(final Object controller) {
        return check(controller, RequirePermissions::read);
    }

    public boolean canDelete(final Object controller) {
        return check(controller, RequirePermissions::delete);
    }

    public boolean canValidate(final Object controller) {
        return check(controller, RequirePermissions::validate);
    }

    public boolean canCorrected(final Object controller) {
        return check(controller, RequirePermissions::corrected);
    }

    public boolean canResendActivation(final Object controller) {
        return check(controller, RequirePermissions::resendActivation);
    }

    public boolean canResetPassword(final Object controller) {
        return check(controller, RequirePermissions::resetPassword);
    }

    private boolean check(final Object controller, final Function<RequirePermissions, String[]> extractor) {
        RequirePermissions ann = controller.getClass().getAnnotation(RequirePermissions.class);
        if (ann == null) {
            return false;
        }

        String[] required = extractor.apply(ann);
        if (required.length == 0) {
            return true;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return false;
        }

        Set<String> userPerms = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        return Arrays.stream(required)
                .map(String::toUpperCase)
                .anyMatch(req -> userPerms.contains(req) 
                        || userPerms.contains("ROLE_" + req) 
                        || (req.startsWith("ROLE_") && userPerms.contains(req.substring(5))));
    }
}
