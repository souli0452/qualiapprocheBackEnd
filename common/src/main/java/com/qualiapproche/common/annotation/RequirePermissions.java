package com.qualiapproche.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RequirePermissions {
    String[] create() default {};
    String[] update() default {};
    String[] read() default {};
    String[] delete() default {};
    String[] validate() default {};
    String[] corrected() default {};
    String[] resendActivation() default {};
    String[] resetPassword() default {};
}
