package com.bentahsin.regionshield.events;

import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class BenthRegionLeaveEvent extends BenthRegionEvent implements Cancellable {

    private boolean cancelled = false;

    public BenthRegionLeaveEvent(Player player, RegionInfo region) {
        super(player, region);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}