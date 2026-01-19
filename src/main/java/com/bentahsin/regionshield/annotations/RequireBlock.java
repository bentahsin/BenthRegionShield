package com.bentahsin.regionshield.annotations;

import org.bukkit.Material;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * İşlemin çalışması için oyuncunun üzerinde durduğu (veya içinde olduğu)
 * bloğun belirli tiplerde olmasını zorunlu kılar.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireBlock {

    /**
     * İzin verilen materyaller.
     */
    Material[] value();

    /**
     * True ise: Oyuncunun bastığı bloğa bakar (Ayak altı).
     * False ise: Oyuncunun içindeki bloğa bakar (Ayak hizası).
     * Varsayılan: True (Bastığı blok).
     */
    boolean checkGround() default true;
}