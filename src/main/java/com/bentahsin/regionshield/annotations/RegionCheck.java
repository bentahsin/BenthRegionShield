package com.bentahsin.regionshield.annotations;

import com.bentahsin.regionshield.model.InteractionType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RegionCheck {

    /**
     * Hangi işlem kontrol edilsin? (Varsayılan: INTERACT)
     */
    InteractionType type() default InteractionType.INTERACT;

    /**
     * Engellenirse oyuncuya mesaj gönderilsin mi?
     */
    boolean silent() default false;

    /**
     * Bu kontrolü aşmak için gereken özel permission.
     * Boş bırakılırsa global bypass izni kullanılır.
     */
    String bypassPerm() default "";
}