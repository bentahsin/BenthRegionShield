package com.bentahsin.regionshield.hooks.skyblock;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SuperiorSkyblockHook implements IShieldHook {

    @Override
    public String getName() {
        return "SuperiorSkyblock2";
    }

    @Override
    public boolean canInitialize() {
        return ReflectionUtils.isPluginActive("SuperiorSkyblock2");
    }

    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        Island island = SuperiorSkyblockAPI.getGrid().getIslandAt(location);

        if (island == null) return ShieldResponse.allow();
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        IslandPrivilege privilege = getPrivilege(type);
        if (privilege == null) {
            privilege = IslandPrivilege.getByName("BUILD");
        }

        boolean allowed = island.hasPermission(superiorPlayer, privilege);

        return allowed ? ShieldResponse.allow() : ShieldResponse.deny(getName());
    }

    /**
     * InteractionType -> IslandPrivilege (String Lookup)
     * Not: SuperiorSkyblock'ta yetki isimleri büyük harfle string olarak tutulur.
     */
    private IslandPrivilege getPrivilege(InteractionType type) {
        String privilegeName;

        switch (type) {
            case BLOCK_BREAK:
                privilegeName = "BREAK";
                break;
            case BLOCK_PLACE:
                privilegeName = "BUILD";
                break;
            case INTERACT:
                privilegeName = "INTERACT";
                break;
            case CONTAINER_ACCESS:
                privilegeName = "CHESTS";
                break;
            case PVP:
                privilegeName = "PVP";
                break;
            case MOB_DAMAGE:
                privilegeName = "FARMING";
                break;
            case BUCKET_USE:
                privilegeName = "BUCKETS";
                break;
            case TRAMPLE:
                privilegeName = "CROP_TRAMPLE";
                break;
            default:
                privilegeName = "BUILD";
                break;
        }

        try {
            return IslandPrivilege.getByName(privilegeName);
        } catch (Exception e) {
            try {
                return IslandPrivilege.getByName("BUILD");
            } catch (Exception ex) {
                return null;
            }
        }
    }
}