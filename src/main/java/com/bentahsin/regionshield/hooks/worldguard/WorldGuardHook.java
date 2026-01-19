package com.bentahsin.regionshield.hooks.worldguard;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldPriority;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook implements IShieldHook {

    private final IWorldGuardWorker worker;

    public WorldGuardHook() {
        this.worker = new WorldGuard7Worker();
    }

    @Override
    public String getName() {
        return "WorldGuard";
    }

    @Override
    public boolean canInitialize() {
        return ReflectionUtils.isPluginActive("WorldGuard");
    }

    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
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
            com.sk89q.worldguard.protection.managers.RegionManager regions = container.get((com.sk89q.worldedit.world.World) weLoc.getExtent());

            if (regions == null) return null;

            com.sk89q.worldguard.protection.ApplicableRegionSet set = regions.getApplicableRegions(weLoc.toVector().toBlockPoint());

            if (set.size() == 0) return null;

            com.sk89q.worldguard.protection.regions.ProtectedRegion region = set.getRegions().iterator().next();

            return RegionInfo.builder()
                    .id(region.getId())
                    .provider(getName())
                    .owners(new java.util.ArrayList<>(region.getOwners().getUniqueIds()))
                    .members(new java.util.ArrayList<>(region.getMembers().getUniqueIds()))
                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public com.bentahsin.regionshield.model.RegionBounds getRegionBounds(Location location) {
        return worker.getRegionBounds(location);
    }
}