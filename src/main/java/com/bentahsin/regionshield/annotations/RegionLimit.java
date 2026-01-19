package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RegionLimit {

    /**
     * Bölge ID'si (Örn: "spawn", "market", "arena")
     * Regex desteklemez, tam eşleşme arar.
     */
    String id();

    /**
     * Sadece belirli bir eklentiye ait bölgelerde mi çalışsın?
     * Örn: "WorldGuard"
     */
    String provider() default "";
}