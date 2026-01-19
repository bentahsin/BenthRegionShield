package com.bentahsin.regionshield.hooks.worldguard;

import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldGuard7Worker implements IWorldGuardWorker {

    @Override
    public boolean canBuild(Player player, Location location, InteractionType type) {
        com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        StateFlag flag = getFlag(type);
        return query.testState(weLoc, localPlayer, flag);
    }

    private StateFlag getFlag(InteractionType type) {
        switch (type) {
            case BLOCK_BREAK:
                return Flags.BLOCK_BREAK;
            case BLOCK_PLACE:
                return Flags.BLOCK_PLACE;
            case INTERACT:
                return Flags.INTERACT;
            case CONTAINER_ACCESS:
                return Flags.CHEST_ACCESS;
            case PVP:
                return Flags.PVP;
            case MOB_DAMAGE:
                return Flags.MOB_DAMAGE;
            case BUCKET_USE:
                return Flags.BUILD;
            case TRAMPLE:
                return Flags.TRAMPLE_BLOCKS;
            case DAMAGE_ENTITY:
                return Flags.MOB_DAMAGE;
            default:
                return Flags.BUILD;
        }
    }

    public RegionBounds getRegionBounds(Location location) {
        com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        com.sk89q.worldguard.protection.managers.RegionManager manager = container.get((com.sk89q.worldedit.world.World) weLoc.getExtent());
        if (manager == null) return null;
        com.sk89q.worldguard.protection.ApplicableRegionSet set = manager.getApplicableRegions(weLoc.toVector().toBlockPoint());
        if (set.size() == 0) return null;
        ProtectedRegion region = set.getRegions().iterator().next();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        World world = location.getWorld();
        Location locMin = new Location(world, min.getX(), min.getY(), min.getZ());
        Location locMax = new Location(world, max.getX(), max.getY(), max.getZ());
        return new RegionBounds(locMin, locMax);
    }
}