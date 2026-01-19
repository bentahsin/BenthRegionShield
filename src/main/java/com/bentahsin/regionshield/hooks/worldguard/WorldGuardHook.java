package com.bentahsin.regionshield.hooks.worldguard;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldPriority;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionInfo;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * WorldGuard Entegrasyonu.
 * Sunucu sürümünü algılar ve uygun işçiyi (Worker V6 veya V7) devreye sokar.
 */
public class WorldGuardHook implements IShieldHook {

    private IWorldGuardWorker worker;

    @Override
    public String getName() {
        return "WorldGuard";
    }

    @Override
    public boolean canInitialize() {
        if (!ReflectionUtils.isPluginActive("WorldGuard")) return false;

        if (ReflectionUtils.getClass("com.sk89q.worldguard.WorldGuard") != null) {
            this.worker = new WorldGuard7Worker();
            return true;
        }

        if (ReflectionUtils.getClass("com.sk89q.worldguard.bukkit.WGBukkit") != null) {
            this.worker = new WorldGuard6Worker();
            return true;
        }

        return false;
    }

    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        if (worker == null) return ShieldResponse.allow();

        boolean allowed = worker.canBuild(player, location, type);

        return allowed ? ShieldResponse.allow() : ShieldResponse.deny(getName());
    }

    @Override
    public ShieldPriority getPriority() {
        return ShieldPriority.HIGHEST;
    }

    @Override
    public RegionInfo getRegionInfo(Location location) {
        try {
            com.sk89q.worldedit.util.Location weLoc = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
            com.sk89q.worldguard.protection.regions.RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.managers.RegionManager regions = container.get((World) weLoc.getExtent());

            if (regions == null) return null;

            com.sk89q.worldguard.protection.ApplicableRegionSet set = regions.getApplicableRegions(weLoc.toVector().toBlockPoint());

            if (set.size() == 0) return null;

            com.sk89q.worldguard.protection.regions.ProtectedRegion region = set.getRegions().iterator().next();

            return RegionInfo.builder()
                    .id(region.getId())
                    .provider(getName())
                    .owners(new ArrayList<>(region.getOwners().getUniqueIds()))
                    .members(new ArrayList<>(region.getMembers().getUniqueIds()))
                    .build();

        } catch (Exception e) {
            return null;
        }
    }
}