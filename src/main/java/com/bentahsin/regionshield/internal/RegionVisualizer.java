package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.model.RegionBounds;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RegionVisualizer {

    /**
     * Sınırları 5 saniye boyunca çizer.
     */
    public static void show(JavaPlugin plugin, Player player, RegionBounds bounds) {
        if (bounds == null || !player.isOnline()) return;

        new BukkitRunnable() {
            int duration = 20;

            @Override
            public void run() {
                if (duration-- <= 0 || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                drawCuboid(player, bounds.getMin(), bounds.getMax());
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private static void drawCuboid(Player player, Location min, Location max) {
        World world = min.getWorld();
        if (world == null || !world.equals(player.getWorld())) return;

        double minX = min.getX(); double minY = min.getY(); double minZ = min.getZ();
        double maxX = max.getX() + 1; double maxY = max.getY() + 1; double maxZ = max.getZ() + 1;

        drawLine(player, minX, minY, minZ, maxX, minY, minZ);
        drawLine(player, minX, minY, minZ, minX, minY, maxZ);
        drawLine(player, maxX, minY, minZ, maxX, minY, maxZ);
        drawLine(player, minX, minY, maxZ, maxX, minY, maxZ);

        drawLine(player, minX, maxY, minZ, maxX, maxY, minZ);
        drawLine(player, minX, maxY, minZ, minX, maxY, maxZ);
        drawLine(player, maxX, maxY, minZ, maxX, maxY, maxZ);
        drawLine(player, minX, maxY, maxZ, maxX, maxY, maxZ);

        drawLine(player, minX, minY, minZ, minX, maxY, minZ);
        drawLine(player, maxX, minY, minZ, maxX, maxY, minZ);
        drawLine(player, minX, minY, maxZ, minX, maxY, maxZ);
        drawLine(player, maxX, minY, maxZ, maxX, maxY, maxZ);
    }

    private static void drawLine(Player player, double x1, double y1, double z1, double x2, double y2, double z2) {
        Location start = new Location(player.getWorld(), x1, y1, z1);
        double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
        double step = 0.5;

        Vector vector = new Vector(x2 - x1, y2 - y1, z2 - z1).normalize().multiply(step);

        for (double d = 0; d < distance; d += step) {
            player.spawnParticle(Particle.FLAME, start, 1, 0, 0, 0, 0);
            start.add(vector);
        }
    }
}