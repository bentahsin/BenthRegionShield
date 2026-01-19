package com.bentahsin.regionshield.hooks.worldguard;

import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface IWorldGuardWorker {
    boolean canBuild(Player player, Location location, InteractionType type);
    RegionBounds getRegionBounds(Location loc);
}