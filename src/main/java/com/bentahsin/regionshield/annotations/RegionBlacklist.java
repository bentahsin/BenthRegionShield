package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Belirtilen bölgelerde işlemi otomatik olarak engeller.
 * "Whitelist" mantığının tersidir.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RegionBlacklist {

    /**
     * Yasaklı Bölge ID'leri.
     */
    String[] ids();

    /**
     * Sadece belirli bir eklenti için mi geçerli?
     * Boş ise tüm providerlarda bu ID'yi arar.
     */
    String provider() default "";
}