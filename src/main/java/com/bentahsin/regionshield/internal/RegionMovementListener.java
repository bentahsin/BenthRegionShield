package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.BenthRegionShield;
import com.bentahsin.regionshield.events.BenthRegionEnterEvent;
import com.bentahsin.regionshield.events.BenthRegionLeaveEvent;
import com.bentahsin.regionshield.model.RegionInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class RegionMovementListener implements Listener {

    private final BenthRegionShield manager;
    private final Map<UUID, RegionInfo> lastRegions = new HashMap<>();

    public RegionMovementListener(BenthRegionShield manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        handleMove(event.getPlayer(), event.getPlayer().getLocation(), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastRegions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) return;

        handleMove(event.getPlayer(), to, event);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        handleMove(event.getPlayer(), event.getTo(), event);
    }

    /**
     * Hareket mantığını işler.
     * @param parentEvent Eğer bu hareket bir Event sonucuysa (Move/Teleport) buraya gelir. İptal için kullanılır.
     */
    private void handleMove(Player player, Location to, Cancellable parentEvent) {
        UUID uuid = player.getUniqueId();
        RegionInfo currentRegion = manager.getRegionInfo(to);
        RegionInfo lastRegion = lastRegions.get(uuid);
        boolean changed = !Objects.equals(lastRegion, currentRegion);
        if (changed) {
            if (lastRegion != null) {
                BenthRegionLeaveEvent leaveEvent = new BenthRegionLeaveEvent(player, lastRegion);
                Bukkit.getPluginManager().callEvent(leaveEvent);

                if (leaveEvent.isCancelled()) {
                    if (parentEvent != null) {
                        parentEvent.setCancelled(true);
                    }
                    return;
                }
            }
            if (currentRegion != null) {
                BenthRegionEnterEvent enterEvent = new BenthRegionEnterEvent(player, currentRegion);
                Bukkit.getPluginManager().callEvent(enterEvent);

                if (enterEvent.isCancelled()) {
                    if (parentEvent != null) {
                        parentEvent.setCancelled(true);
                    }
                    return;
                }
            }
            lastRegions.put(uuid, currentRegion);
        }
    }
}