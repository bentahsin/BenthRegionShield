package com.bentahsin.regionshield.hooks.worldguard;

import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

public class WorldGuard6Worker implements IWorldGuardWorker {

    private Object wgPluginInstance;
    private Method getRegionManagerMethod;
    private Method getApplicableRegionsMethod;
    private Method wrapPlayerMethod;
    private Method queryStateMethod;

    private Class<?> defaultFlagClass;
    private Class<?> stateFlagClass;

    private boolean initialized;

    public WorldGuard6Worker() {
        try {
            Class<?> wgBukkitClass = ReflectionUtils.getClass("com.sk89q.worldguard.bukkit.WGBukkit");
            Class<?> pluginClass = ReflectionUtils.getClass("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Class<?> regionManagerClass = ReflectionUtils.getClass("com.sk89q.worldguard.protection.managers.RegionManager");
            Class<?> applicableRegionSetClass = ReflectionUtils.getClass("com.sk89q.worldguard.protection.ApplicableRegionSet");
            Class<?> regionAssociableClass = ReflectionUtils.getClass("com.sk89q.worldguard.protection.association.RegionAssociable");

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