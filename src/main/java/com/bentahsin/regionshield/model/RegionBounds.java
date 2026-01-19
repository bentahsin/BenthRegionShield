package com.bentahsin.regionshield.model;

import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Bir bölgenin 3D alanını tanımlayan sınırları temsil eder.
 * Bu sınıf, bölgenin minimum ve maksimum köşe noktalarını içerir.
 * Sınıf, dışarıdan yapılacak değişikliklere karşı koruma sağlamak için
 * Lokasyon nesnelerini klonlayarak değişmez (immutable) olacak şekilde tasarlanmıştır.
 */
@ToString
public class RegionBounds {
    private final Location min;
    private final Location max;

    /**
     * Verilen minimum ve maksimum köşe noktalarıyla yeni bir RegionBounds nesnesi oluşturur.
     * Güvenlik için sağlanan Lokasyon nesneleri, dahili olarak klonlanır.
     *
     * @param min Bölgenin minimum köşe noktası (en düşük X, Y, Z koordinatları).
     * @param max Bölgenin maksimum köşe noktası (en yüksek X, Y, Z koordinatları).
     */
    public RegionBounds(Location min, Location max) {
        this.min = (min != null) ? min.clone() : null;
        this.max = (max != null) ? max.clone() : null;
    }

    /**
     * Bölgenin minimum köşe noktasının bir klonunu döndürür.
     *
     * @return Minimum köşe noktasının bir kopyası veya ayarlanmamışsa null.
     */
    public Location getMin() {
        return (min != null) ? min.clone() : null;
    }

    /**
     * Bölgenin maksimum köşe noktasının bir klonunu döndürür.
     *
     * @return Maksimum köşe noktasının bir kopyası veya ayarlanmamışsa null.
     */
    public Location getMax() {
        return (max != null) ? max.clone() : null;
    }

    /**
     * Bölgenin bulunduğu dünyayı (World) döndürür.
     * Bu, minimum köşe noktasının dünyasından alınır.
     *
     * @return Bölgenin bulunduğu {@link World} nesnesi veya ayarlanmamışsa null.
     */
    public World getWorld() {
        return (min != null) ? min.getWorld() : null;
    }
}