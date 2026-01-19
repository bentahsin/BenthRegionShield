package com.bentahsin.regionshield.hooks.gp;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GriefPreventionHook implements IShieldHook {

    private Object dataStore;

    private Method getClaimMethod;
    private Method allowBuildMethod;
    private Method allowAccessMethod;
    private Method allowContainersMethod;

    private Method getIDMethod;
    private Method getLesserBoundaryCorner;
    private Method getGreaterBoundaryCorner;

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

            this.getIDMethod = ReflectionUtils.getMethod(claimClass, "getID");
            this.getLesserBoundaryCorner = ReflectionUtils.getMethod(claimClass, "getLesserBoundaryCorner");
            this.getGreaterBoundaryCorner = ReflectionUtils.getMethod(claimClass, "getGreaterBoundaryCorner");

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

    @Override
    public RegionInfo getRegionInfo(Location location) {
        if (!initialized) return null;

        try {
            Object claim = ReflectionUtils.invoke(getClaimMethod, dataStore, location, false, null);
            if (claim == null) return null;

            Long id = (Long) ReflectionUtils.invoke(getIDMethod, claim);

            List<UUID> owners = new ArrayList<>();
            UUID ownerID = (UUID) ReflectionUtils.getField(claim.getClass(), claim, "ownerID");
            if (ownerID != null) {
                owners.add(ownerID);
            }

            List<UUID> members = new ArrayList<>();
            addMembersFromField(claim, "builders", members);
            addMembersFromField(claim, "containers", members);
            addMembersFromField(claim, "accessors", members);
            addMembersFromField(claim, "managers", members);

            return RegionInfo.builder()
                    .id(id != null ? id.toString() : "Unknown")
                    .provider(getName())
                    .owners(owners)
                    .members(members)
                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public RegionBounds getRegionBounds(Location location) {
        if (!initialized) return null;

        try {
            Object claim = ReflectionUtils.invoke(getClaimMethod, dataStore, location, false, null);
            if (claim == null) return null;

            Location lesser = (Location) ReflectionUtils.invoke(getLesserBoundaryCorner, claim);
            Location greater = (Location) ReflectionUtils.invoke(getGreaterBoundaryCorner, claim);

            if (lesser != null && greater != null) {
                return new RegionBounds(lesser, greater);
            }
            return null;

        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void addMembersFromField(Object claim, String fieldName, List<UUID> targetList) {
        try {
            ArrayList<String> list = (ArrayList<String>) ReflectionUtils.getField(claim.getClass(), claim, fieldName);
            if (list != null) {
                for (String s : list) {
                    try {
                        targetList.add(UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }
}