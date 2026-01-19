package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Diğer tüm RegionShield kısıtlamalarını (Check, Role, Limit, Block vb.)
 * belirtilen izne sahip oyuncular için devre dışı bırakır.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ShieldBypass {
    String value();
}