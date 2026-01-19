package com.bentahsin.regionshield.api;

import lombok.Getter;

@Getter
public enum ShieldPriority {
    LOWEST(0),
    LOW(10),
    NORMAL(20),
    HIGH(30),
    HIGHEST(40),
    MONITOR(50);

    private final int value;

    ShieldPriority(int value) {
        this.value = value;
    }
}