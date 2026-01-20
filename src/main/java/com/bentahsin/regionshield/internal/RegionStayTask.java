package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.BenthRegionShield;
import com.bentahsin.regionshield.events.BenthRegionStayEvent;
import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RegionStayTask extends BukkitRunnable {

    private final BenthRegionShield api;

    public RegionStayTask(BenthRegionShield api) {
        this.api = api;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            RegionInfo info = api.getRegionInfo(player.getLocation());

            if (info != null) {
                BenthRegionStayEvent event = new BenthRegionStayEvent(player, info);
                Bukkit.getPluginManager().callEvent(event);
            }
        }
    }
}