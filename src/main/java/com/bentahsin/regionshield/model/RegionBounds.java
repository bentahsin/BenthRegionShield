package com.bentahsin.regionshield.model;

import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.World;

@ToString
public class RegionBounds {
    private final Location min;
    private final Location max;

    public RegionBounds(Location min, Location max) {
        this.min = (min != null) ? min.clone() : null;
        this.max = (max != null) ? max.clone() : null;
    }

    public Location getMin() {
        return (min != null) ? min.clone() : null;
    }

    public Location getMax() {
        return (max != null) ? max.clone() : null;
    }

    public World getWorld() {
        return (min != null) ? min.getWorld() : null;
    }
}