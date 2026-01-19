package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RegionRole {

    RegionRoleType value() default RegionRoleType.MEMBER_OR_OWNER;

    boolean silent() default false;
}