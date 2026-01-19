package com.bentahsin.regionshield.hooks.lands;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import com.bentahsin.regionshield.model.RegionInfo;
import me.angeschossen.lands.api.flags.Flags;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.integration.LandsIntegration;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.land.LandWorld;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("deprecation")
public class LandsHook implements IShieldHook {

    private final Plugin plugin;
    private LandsIntegration landsIntegration;

    public LandsHook(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Lands";
    }

    @Override
    public boolean canInitialize() {
        if (!ReflectionUtils.isPluginActive("Lands")) return false;

        try {
            this.landsIntegration = new LandsIntegration(plugin);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        if (landsIntegration == null) return ShieldResponse.allow();

        if (location == null || location.getWorld() == null) return ShieldResponse.allow();

        LandWorld landWorld = landsIntegration.getLandWorld(location.getWorld());
        if (landWorld == null) return ShieldResponse.allow();

        RoleFlag flag = getRoleFlag(type);
        if (flag == null) return ShieldResponse.allow();

        boolean allowed = landWorld.hasRoleFlag(player.getUniqueId(), location, flag);
        return allowed ? ShieldResponse.allow() : ShieldResponse.deny(getName());
    }

    @Override
    public RegionInfo getRegionInfo(Location location) {
        if (landsIntegration == null || location == null) return null;

        Land land = landsIntegration.getLand(location);

        if (land == null) return null;

        return RegionInfo.builder()
                .id(land.getName())
                .provider(getName())
                .owners(Collections.singletonList(land.getOwnerUID()))
                .members(new ArrayList<>(land.getTrustedPlayers()))
                .build();
    }

    @Override
    public RegionBounds getRegionBounds(Location location) {
        if (landsIntegration == null || location == null) return null;

        Land land = landsIntegration.getLand(location);
        if (land == null) return null;

        org.bukkit.Chunk chunk = location.getChunk();
        World world = location.getWorld();
        if (world == null) return null;

        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        int minY = 0;
        int maxY = world.getMaxHeight();

        try {
            minY = world.getMinHeight();
        } catch (NoSuchMethodError ignored) {}

        Location min = new Location(world, minX, minY, minZ);
        Location max = new Location(world, maxX, maxY, maxZ);

        return new RegionBounds(min, max);
    }

    private RoleFlag getRoleFlag(InteractionType type) {
        switch (type) {
            case BLOCK_BREAK:
                return Flags.BLOCK_BREAK;
            case BLOCK_PLACE:
                return Flags.BLOCK_PLACE;
            case INTERACT:
                return Flags.INTERACT_GENERAL;
            case CONTAINER_ACCESS:
                return Flags.INTERACT_CONTAINER;
            case PVP:
                return Flags.ATTACK_PLAYER;
            case MOB_DAMAGE:
                return Flags.ATTACK_ANIMAL;
            case TRAMPLE:
                return Flags.TRAMPLE_FARMLAND;
            case DAMAGE_ENTITY:
                return Flags.ATTACK_MONSTER;
            case BUCKET_USE:
                return Flags.BLOCK_PLACE;
            default:
                return Flags.BLOCK_PLACE;
        }
    }
}