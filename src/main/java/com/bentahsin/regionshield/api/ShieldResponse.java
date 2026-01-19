package com.bentahsin.regionshield.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ShieldResponse {

    private final boolean allowed;
    private final String providerName;

    private static final ShieldResponse ALLOW_DEFAULT = new ShieldResponse(true, "None");

    public static ShieldResponse allow() {
        return ALLOW_DEFAULT;
    }

    public static ShieldResponse deny(String provider) {
        return new ShieldResponse(false, provider);
    }

    public boolean isDenied() {
        return !allowed;
    }
}