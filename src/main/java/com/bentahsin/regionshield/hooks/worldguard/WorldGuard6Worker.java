package com.bentahsin.regionshield.hooks.worldguard;

import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Iterator;

public class WorldGuard6Worker implements IWorldGuardWorker {

    private Object wgPluginInstance;
    private Method getRegionManagerMethod;
    private Method getApplicableRegionsMethod;
    private Method wrapPlayerMethod;
    private Method queryStateMethod;

    private Class<?> defaultFlagClass;
    private Class<?> stateFlagClass;

    private Method getMinimumPointMethod;
    private Method getMaximumPointMethod;
    private Method getVectorXMethod;
    private Method getVectorYMethod;
    private Method getVectorZMethod;

    private boolean initialized;

    public WorldGuard6Worker() {
        try {
            Class<?> wgBukkitClass = ReflectionUtils.getClass("com.sk89q.worldguard.bukkit.WGBukkit");
            Class<?> pluginClass = ReflectionUtils.getClass("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Class<?> regionManagerClass = ReflectionUtils.getClass("com.sk89q.worldguard.protection.managers.RegionManager");
            Class<?> applicableRegionSetClass = ReflectionUtils.getClass("com.sk89q.worldguard.protection.ApplicableRegionSet");
            Class<?> regionAssociableClass = ReflectionUtils.getClass("com.sk89q.worldguard.protection.association.RegionAssociable");
            Class<?> protectedRegionClass = ReflectionUtils.getClass("com.sk89q.worldguard.protection.regions.ProtectedRegion");

            Class<?> blockVectorClass = ReflectionUtils.getClass("com.sk89q.worldedit.BlockVector");

            this.stateFlagClass = ReflectionUtils.getClass("com.sk89q.worldguard.protection.flags.StateFlag");
            this.defaultFlagClass = ReflectionUtils.getClass("com.sk89q.worldguard.protection.flags.DefaultFlag");

            Method getPlugin = ReflectionUtils.getMethod(wgBukkitClass, "getPlugin");
            this.wgPluginInstance = ReflectionUtils.invoke(getPlugin, null);

            this.getRegionManagerMethod = ReflectionUtils.getMethod(wgBukkitClass, "getRegionManager", World.class);
            this.getApplicableRegionsMethod = ReflectionUtils.getMethod(regionManagerClass, "getApplicableRegions", Location.class);
            this.wrapPlayerMethod = ReflectionUtils.getMethod(pluginClass, "wrapPlayer", Player.class);

            this.queryStateMethod = ReflectionUtils.getMethod(applicableRegionSetClass, "queryState", regionAssociableClass, stateFlagClass);

            if (this.queryStateMethod == null) {
                Class<?> stateFlagArrayClass = Array.newInstance(stateFlagClass, 0).getClass();
                this.queryStateMethod = ReflectionUtils.getMethod(applicableRegionSetClass, "queryState", regionAssociableClass, stateFlagArrayClass);
            }

            this.getMinimumPointMethod = ReflectionUtils.getMethod(protectedRegionClass, "getMinimumPoint");
            this.getMaximumPointMethod = ReflectionUtils.getMethod(protectedRegionClass, "getMaximumPoint");

            this.getVectorXMethod = ReflectionUtils.getMethod(blockVectorClass, "getX");
            this.getVectorYMethod = ReflectionUtils.getMethod(blockVectorClass, "getY");
            this.getVectorZMethod = ReflectionUtils.getMethod(blockVectorClass, "getZ");

            this.initialized = true;

        } catch (Exception e) {
            this.initialized = false;
        }
    }

    @Override
    public boolean canBuild(Player player, Location location, InteractionType type) {
        if (!initialized) return true;

        try {
            Object regionManager = ReflectionUtils.invoke(getRegionManagerMethod, null, location.getWorld());
            if (regionManager == null) return true;

            Object regionSet = ReflectionUtils.invoke(getApplicableRegionsMethod, regionManager, location);
            if (regionSet == null) return true;

            Object flag = getFlag(type);
            if (flag == null) return true;

            Object localPlayer = ReflectionUtils.invoke(wrapPlayerMethod, wgPluginInstance, player);
            Object flagArray = Array.newInstance(stateFlagClass, 1);
            Array.set(flagArray, 0, flag);
            Object result = ReflectionUtils.invoke(queryStateMethod, regionSet, localPlayer, flagArray);

            return result == null || !result.toString().equals("DENY");

        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public RegionBounds getRegionBounds(Location location) {
        if (!initialized) return null;

        try {
            Object regionManager = ReflectionUtils.invoke(getRegionManagerMethod, null, location.getWorld());
            if (regionManager == null) return null;

            Object regionSet = ReflectionUtils.invoke(getApplicableRegionsMethod, regionManager, location);
            if (regionSet == null) return null;

            Iterator<?> iterator = ((Iterable<?>) regionSet).iterator();
            if (!iterator.hasNext()) return null;

            Object region = iterator.next();

            Object minVec = ReflectionUtils.invoke(getMinimumPointMethod, region);
            Object maxVec = ReflectionUtils.invoke(getMaximumPointMethod, region);

            double minX = (double) ReflectionUtils.invoke(getVectorXMethod, minVec);
            double minY = (double) ReflectionUtils.invoke(getVectorYMethod, minVec);
            double minZ = (double) ReflectionUtils.invoke(getVectorZMethod, minVec);

            double maxX = (double) ReflectionUtils.invoke(getVectorXMethod, maxVec);
            double maxY = (double) ReflectionUtils.invoke(getVectorYMethod, maxVec);
            double maxZ = (double) ReflectionUtils.invoke(getVectorZMethod, maxVec);

            World world = location.getWorld();
            Location locMin = new Location(world, minX, minY, minZ);
            Location locMax = new Location(world, maxX, maxY, maxZ);

            return new RegionBounds(locMin, locMax);

        } catch (Exception e) {
            return null;
        }
    }

    private Object getFlag(InteractionType type) {
        String flagName;
        switch (type) {
            case BLOCK_BREAK:
            case BLOCK_PLACE:
            case BUCKET_USE:
                flagName = "BUILD";
                break;
            case INTERACT:
                flagName = "INTERACT";
                break;
            case CONTAINER_ACCESS:
                flagName = "CHEST_ACCESS";
                break;
            case PVP:
                flagName = "PVP";
                break;
            case MOB_DAMAGE:
            case DAMAGE_ENTITY:
                flagName = "DAMAGE_ANIMALS";
                break;
            case TRAMPLE:
                flagName = "BUILD";
                break;
            default:
                flagName = "BUILD";
                break;
        }
        return ReflectionUtils.getField(defaultFlagClass, null, flagName);
    }
}