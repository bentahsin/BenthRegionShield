package com.bentahsin.regionshield.hooks.lands;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import me.angeschossen.lands.api.flags.Flags;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.integration.LandsIntegration;
import me.angeschossen.lands.api.land.LandWorld;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

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

        LandWorld landWorld = landsIntegration.getLandWorld(Objects.requireNonNull(location.getWorld()));
        if (landWorld == null) return ShieldResponse.allow();

        RoleFlag flag = getRoleFlag(type);
        if (flag == null) return ShieldResponse.allow();

        boolean allowed = landWorld.hasRoleFlag(player.getUniqueId(), location, flag);
        return allowed ? ShieldResponse.allow() : ShieldResponse.deny(getName());
    }

    private RoleFlag getRoleFlag(InteractionType type) {
        switch (type) {
            case BLOCK_BREAK:
                return Flags.BLOCK_BREAK;
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
            default:
                return Flags.BLOCK_PLACE;
        }
    }
}