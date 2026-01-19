package com.bentahsin.regionshield.hooks.skyblock;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class ASkyBlockHook implements IShieldHook {

    private Object apiInstance;
    private Method getIslandAtMethod;
    private Method getOwnerMethod;
    private Method getMembersMethod;

    private boolean initialized = false;

    @Override
    public String getName() {
        return "ASkyBlock";
    }

    @Override
    public boolean canInitialize() {
        if (!ReflectionUtils.isPluginActive("ASkyBlock")) return false;

        try {
            Class<?> apiClass = ReflectionUtils.getClass("com.wasteofplastic.askyblock.ASkyBlockAPI");
            Method getInstance = ReflectionUtils.getMethod(apiClass, "getInstance");
            this.apiInstance = ReflectionUtils.invoke(getInstance, null);

            this.getIslandAtMethod = ReflectionUtils.getMethod(apiClass, "getIslandAt", Location.class);

            Class<?> islandClass = ReflectionUtils.getClass("com.wasteofplastic.askyblock.Island");
            this.getOwnerMethod = ReflectionUtils.getMethod(islandClass, "getOwner");
            this.getMembersMethod = ReflectionUtils.getMethod(islandClass, "getMembers"); // Set<UUID> döner

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
            Object island = ReflectionUtils.invoke(getIslandAtMethod, apiInstance, location);
            if (island == null) return ShieldResponse.allow();

            UUID playerUUID = player.getUniqueId();
            UUID ownerUUID = (UUID) ReflectionUtils.invoke(getOwnerMethod, island);

            if (ownerUUID != null && ownerUUID.equals(playerUUID)) {
                return ShieldResponse.allow();
            }

            java.util.Set<?> members = (java.util.Set<?>) ReflectionUtils.invoke(getMembersMethod, island);

            if (members != null && members.contains(playerUUID)) {
                return ShieldResponse.allow();
            }

            return ShieldResponse.deny(getName());

        } catch (Exception e) {
            return ShieldResponse.allow();
        }
    }
}