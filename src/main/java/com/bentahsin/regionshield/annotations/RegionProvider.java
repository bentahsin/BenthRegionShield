package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sadece belirli bir eklentinin alanında çalışır.
 * Örn: @RegionProvider("Towny") -> Sadece Towny kasabalarında çalışır.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RegionProvider {
    String value();
}