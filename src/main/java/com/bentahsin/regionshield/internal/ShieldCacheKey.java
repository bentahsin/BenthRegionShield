package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.model.InteractionType;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * String birleştirme hamallığına son.
 * RAM dostu, GC dostu, yüksek performanslı anahtar.
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class ShieldCacheKey {
    private final UUID playerUUID;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final InteractionType type;
}