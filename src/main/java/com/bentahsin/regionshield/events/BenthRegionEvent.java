package com.bentahsin.regionshield.events;

import com.bentahsin.regionshield.model.RegionInfo;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Tüm bölge olayları için temel sınıf.
 */
@Getter
public abstract class BenthRegionEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    protected final Player player;
    protected final RegionInfo region;

    public BenthRegionEvent(Player player, RegionInfo region) {
        this.player = player;
        this.region = region;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}