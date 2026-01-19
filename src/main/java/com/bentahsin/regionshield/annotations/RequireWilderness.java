package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bu metodun çalışması için oyuncunun HİÇBİR bölgede olmaması gerekir.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireWilderness {
    boolean silent() default false;
}