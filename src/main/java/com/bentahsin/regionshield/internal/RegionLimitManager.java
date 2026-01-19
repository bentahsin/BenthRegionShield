package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.BenthRegionShield;
import com.bentahsin.regionshield.events.BenthRegionEnterEvent;
import com.bentahsin.regionshield.events.BenthRegionLeaveEvent;
import com.bentahsin.regionshield.model.RegionInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class RegionLimitManager implements Listener {

    private final BenthRegionShield manager;
    private final Map<String, Integer> limits = new HashMap<>();
    private final Map<String, Integer> activeCounts = new ConcurrentHashMap<>();

    public RegionLimitManager(BenthRegionShield manager) {
        this.manager = manager;
        recountPlayers();
    }

    /**
     * Kütüphaneyi kullanan geliştirici limiti bu metodla ekleyecek.
     * Örn: api.setRegionLimit("WorldGuard", "arena", 10);
     */
    public void setLimit(String provider, String regionId, int limit) {
        String key = (provider + ":" + regionId).toLowerCase();
        limits.put(key, limit);
    }

    /**
     * Limiti kaldırmak için.
     */
    public void removeLimit(String provider, String regionId) {
        String key = (provider + ":" + regionId).toLowerCase();
        limits.remove(key);
    }

    public void recountPlayers() {
        activeCounts.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            RegionInfo info = manager.getRegionInfo(p.getLocation());
            if (info != null) {
                increment(info);
            }
        }
    }

    public boolean isFull(RegionInfo info) {
        if (info == null) return false;

        String key = getKey(info);
        if (!limits.containsKey(key)) return false;

        int limit = limits.get(key);
        int current = activeCounts.getOrDefault(key, 0);

        return current >= limit;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRegionEnter(BenthRegionEnterEvent event) {
        if (event.isCancelled()) return;

        RegionInfo info = event.getRegion();

        if (isFull(info)) {
            if (!event.getPlayer().hasPermission("regionshield.bypass.limit")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cBu bölge dolu! (" + activeCounts.get(getKey(info)) + "/" + limits.get(getKey(info)) + ")");
                return;
            }
        }
        increment(info);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRegionLeave(BenthRegionLeaveEvent event) {
        decrement(event.getRegion());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        RegionInfo info = manager.getRegionInfo(event.getPlayer().getLocation());
        if (info != null) decrement(info);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        RegionInfo info = manager.getRegionInfo(event.getPlayer().getLocation());
        if (info != null) increment(info);
    }

    private void increment(RegionInfo info) {
        String key = getKey(info);
        if (limits.containsKey(key)) {
            activeCounts.merge(key, 1, Integer::sum);
        }
    }

    private void decrement(RegionInfo info) {
        String key = getKey(info);
        if (limits.containsKey(key)) {
            activeCounts.computeIfPresent(key, (k, v) -> v > 0 ? v - 1 : 0);
        }
    }

    private String getKey(RegionInfo info) {
        return (info.getProvider() + ":" + info.getId()).toLowerCase();
    }
}