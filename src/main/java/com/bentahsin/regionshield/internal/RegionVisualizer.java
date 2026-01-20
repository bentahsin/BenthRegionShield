package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.model.RegionBounds;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Bir bölgenin sınırlarını oyunculara parçacıklar (particles) aracılığıyla görsel olarak göstermek için
 * yardımcı metotlar içeren bir dahili utility sınıfı.
 * <p>
 * Bu sınıfın metotları statiktir ve doğrudan bir örnek oluşturulması amaçlanmamıştır.
 */
public class RegionVisualizer {

    /**
     * Belirtilen bir bölgenin sınırlarını bir oyuncuya belirli bir süre boyunca gösterir.
     * Sınırlar, periyodik olarak parçacıklar çizilerek oluşturulur.
     * Görev, oyuncu çevrimdışı olursa veya süre dolarsa kendini otomatik olarak iptal eder.
     *
     * @param plugin Görevi (task) zamanlamak için kullanılacak olan ana eklenti (plugin) örneği.
     * @param player Sınırları görecek olan oyuncu.
     * @param bounds Görselleştirilecek olan bölgenin {@link RegionBounds} nesnesi.
     */
    public static void show(JavaPlugin plugin, Player player, RegionBounds bounds, Particle particle) {
        if (bounds == null || !player.isOnline()) return;

        new BukkitRunnable() {
            int duration = 20;

            @Override
            public void run() {
                if (duration-- <= 0 || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                drawCuboid(player, bounds.getMin(), bounds.getMax(), particle);
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    /**
     * Minimum ve maksimum köşe noktalarıyla tanımlanan bir küpoidin (dikdörtgen prizma) 12 kenarını çizer.
     * Bu metot, bölgenin tam bir çerçevesini oluşturmak için kullanılır.
     *
     * @param player Parçacıkların gösterileceği oyuncu.
     * @param min    Küpoidin minimum köşe noktası (en düşük X, Y, Z).
     * @param max    Küpoidin maksimum köşe noktası (en yüksek X, Y, Z).
     */
    private static void drawCuboid(Player player, Location min, Location max, Particle particle) {
        World world = min.getWorld();
        if (world == null || !world.equals(player.getWorld())) return;

        double minX = min.getX(); double minY = min.getY(); double minZ = min.getZ();
        double maxX = max.getX() + 1; double maxY = max.getY() + 1; double maxZ = max.getZ() + 1;

        drawLine(player, minX, minY, minZ, maxX, minY, minZ, particle);
        drawLine(player, minX, minY, minZ, minX, minY, maxZ, particle);
        drawLine(player, maxX, minY, minZ, maxX, minY, maxZ, particle);
        drawLine(player, minX, minY, maxZ, maxX, minY, maxZ, particle);

        drawLine(player, minX, maxY, minZ, maxX, maxY, minZ, particle);
        drawLine(player, minX, maxY, minZ, minX, maxY, maxZ, particle);
        drawLine(player, maxX, maxY, minZ, maxX, maxY, maxZ, particle);
        drawLine(player, minX, maxY, maxZ, maxX, maxY, maxZ, particle);

        drawLine(player, minX, minY, minZ, minX, maxY, minZ, particle);
        drawLine(player, maxX, minY, minZ, maxX, maxY, minZ, particle);
        drawLine(player, minX, minY, maxZ, minX, maxY, maxZ, particle);
        drawLine(player, maxX, minY, maxZ, maxX, maxY, maxZ, particle);
    }

    /**
     * İki 3D nokta arasında parçacıklar kullanarak düz bir çizgi çizer.
     * Çizgi, başlangıç noktasından bitiş noktasına doğru küçük adımlarla ilerleyerek oluşturulur.
     *
     * @param player Parçacıkların gösterileceği oyuncu.
     * @param x1     Başlangıç noktasının X koordinatı.
     * @param y1     Başlangıç noktasının Y koordinatı.
     * @param z1     Başlangıç noktasının Z koordinatı.
     * @param x2     Bitiş noktasının X koordinatı.
     * @param y2     Bitiş noktasının Y koordinatı.
     * @param z2     Bitiş noktasının Z koordinatı.
     */
    private static void drawLine(Player player, double x1, double y1, double z1, double x2, double y2, double z2, Particle particle) {
        Location start = new Location(player.getWorld(), x1, y1, z1);
        double distance = start.distance(new Location(player.getWorld(), x2, y2, z2));
        double step = 0.5;

        Vector vector = new Vector(x2 - x1, y2 - y1, z2 - z1).normalize().multiply(step);

        for (double d = 0; d < distance; d += step) {
            player.spawnParticle(particle, start, 1, 0, 0, 0, 0);
            start.add(vector);
        }
    }
}