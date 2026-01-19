package com.bentahsin.regionshield.hooks.worldguard;

import com.bentahsin.regionshield.model.InteractionType;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * WorldGuard 7.0.0 ve üzeri (1.13+) sürümler için çalışan işçi sınıfı.
 * WorldGuard API'sini doğrudan kullanır (Compile-time dependency).
 */
public class WorldGuard7Worker implements IWorldGuardWorker {

    @Override
    public boolean canBuild(Player player, Location location, InteractionType type) {
        com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        StateFlag flag = getFlag(type);
        return query.testState(weLoc, localPlayer, flag);
    }

    /**
     * Bizim InteractionType enum'ımızı WorldGuard'ın StateFlag'lerine çevirir.
     * Referans: com.sk89q.worldguard.protection.flags.Flags
     */
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
}