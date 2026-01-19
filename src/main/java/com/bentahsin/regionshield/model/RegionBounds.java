package com.bentahsin.regionshield.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;

@Data
@AllArgsConstructor
public class RegionBounds {
    private Location min;
    private Location max;

    public World getWorld() {
        return min.getWorld();
    }
}