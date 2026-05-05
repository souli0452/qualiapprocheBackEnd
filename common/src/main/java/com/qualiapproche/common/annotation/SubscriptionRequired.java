package com.qualiapproche.common.annotation;

import com.qualiapproche.common.enumeration.ModuleAbonnement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscriptionRequired {
    ModuleAbonnement value();
}
