package com.bentahsin.regionshield.hooks.gp;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class GriefPreventionHook implements IShieldHook {

    private Object dataStore;
    private Method getClaimMethod;
    private Method allowBuildMethod;
    private Method allowAccessMethod;
    private Method allowContainersMethod;

    private boolean initialized = false;

    @Override
    public String getName() {
        return "GriefPrevention";
    }

    @Override
    public boolean canInitialize() {
        if (!ReflectionUtils.isPluginActive("GriefPrevention")) return false;

        try {
            Class<?> gpClass = ReflectionUtils.getClass("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Class<?> dataStoreClass = ReflectionUtils.getClass("me.ryanhamshire.GriefPrevention.DataStore");
            Class<?> claimClass = ReflectionUtils.getClass("me.ryanhamshire.GriefPrevention.Claim");

            Object instance = ReflectionUtils.getField(gpClass, null, "instance");
            this.dataStore = ReflectionUtils.getField(gpClass, instance, "dataStore");
            this.getClaimMethod = ReflectionUtils.getMethod(dataStoreClass, "getClaimAt", Location.class, boolean.class, claimClass);
            this.allowBuildMethod = ReflectionUtils.getMethod(claimClass, "allowBuild", Player.class, Material.class);
            this.allowAccessMethod = ReflectionUtils.getMethod(claimClass, "allowAccess", Player.class);
            this.allowContainersMethod = ReflectionUtils.getMethod(claimClass, "allowContainers", Player.class);

            this.initialized = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        if (!initialized) return ShieldResponse.allow();

        try {
            Object claim = ReflectionUtils.invoke(getClaimMethod, dataStore, location, false, null);
            if (claim == null) return ShieldResponse.allow();

            String resultMessage;

            switch (type) {
                case BLOCK_BREAK:
                case BLOCK_PLACE:
                case BUCKET_USE:
                case TRAMPLE:
                case DAMAGE_ENTITY:
                    resultMessage = (String) ReflectionUtils.invoke(allowBuildMethod, claim, player, location.getBlock().getType());
                    break;

                case CONTAINER_ACCESS:
                case MOB_DAMAGE:
                    resultMessage = (String) ReflectionUtils.invoke(allowContainersMethod, claim, player);
                    break;

                case INTERACT:
                    resultMessage = (String) ReflectionUtils.invoke(allowAccessMethod, claim, player);
                    break;

                case PVP:
                    return ShieldResponse.allow();

                default:
                    resultMessage = (String) ReflectionUtils.invoke(allowBuildMethod, claim, player, Material.AIR);
            }

            return resultMessage == null ? ShieldResponse.allow() : ShieldResponse.deny(getName());

        } catch (Exception e) {
            return ShieldResponse.allow();
        }
    }
}