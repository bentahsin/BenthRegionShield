package com.bentahsin.regionshield.hooks.worldguard;

import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * WorldGuard eklentisinin farklı sürümleriyle etkileşim kurmak için bir sözleşme (contract) tanımlar.
 * <p>
 * Bu arayüz, ana {@link WorldGuardHook} sınıfını, WorldGuard'ın belirli bir sürümünün
 * API detaylarından soyutlar. Bu sayede, gelecekte WorldGuard'ın farklı sürümlerini
 * desteklemek için bu arayüzü uygulayan yeni "worker" sınıfları oluşturmak mümkün olur.
 */
public interface IWorldGuardWorker {

    /**
     * Bir oyuncunun belirli bir konumda, belirtilen türde bir eylem gerçekleştirme
     * iznine sahip olup olmadığını kontrol eder.
     *
     * @param player   Eylemi gerçekleştiren oyuncu.
     * @param location Eylemin gerçekleştiği konum.
     * @param type     Gerçekleştirilen etkileşimin türü.
     * @return Oyuncunun eylemi gerçekleştirmesine izin veriliyorsa {@code true}, aksi takdirde {@code false}.
     */
    boolean canBuild(Player player, Location location, InteractionType type);

    /**
     * Belirtilen bir konumdaki en yüksek öncelikli bölgenin fiziksel sınırlarını (sınır kutusunu) alır.
     *
     * @param loc Sınırları alınacak bölgenin içinde bulunan bir konum.
     * @return Bölgenin sınırlarını temsil eden bir {@link RegionBounds} nesnesi veya
     *         konumda bir bölge bulunamazsa {@code null}.
     */
    RegionBounds getRegionBounds(Location loc);
}