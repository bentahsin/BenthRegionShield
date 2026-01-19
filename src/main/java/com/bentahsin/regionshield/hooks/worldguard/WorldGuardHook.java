package com.bentahsin.regionshield.hooks.worldguard;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldPriority;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * BenthRegionShield API'si ile WorldGuard eklentisi arasında bir köprü (hook) görevi görür.
 * <p>
 * Bu sınıf, RegionShield'ın standart sorgularını WorldGuard'ın kendi API çağrılarına çevirir.
 * WorldGuard'ın farklı sürümleri arasındaki API değişikliklerini yönetmek için
 * bir {@link IWorldGuardWorker} arayüzü kullanır, bu sayede ana hook sınıfı temiz kalır.
 */
public class WorldGuardHook implements IShieldHook {

    /**
     * WorldGuard API'sinin sürümüne özgü işlemleri gerçekleştiren worker nesnesi.
     */
    private final IWorldGuardWorker worker;

    /**
     * Yeni bir WorldGuardHook örneği oluşturur.
     * WorldGuard 7 ve üzeri için uyumlu olan worker'ı başlatır.
     */
    public WorldGuardHook() {
        this.worker = new WorldGuard7Worker();
    }

    /**
     * Hook'un benzersiz adını döndürür.
     *
     * @return "WorldGuard" String'i.
     */
    @Override
    public String getName() {
        return "WorldGuard";
    }

    /**
     * Bu hook'un başlatılıp başlatılamayacağını kontrol eder.
     * Sadece WorldGuard eklentisi sunucuda yüklü ve aktif ise başlatılabilir.
     *
     * @return WorldGuard aktif ise true, aksi takdirde false.
     */
    @Override
    public boolean canInitialize() {
        return ReflectionUtils.isPluginActive("WorldGuard");
    }

    /**
     * Bir oyuncunun belirli bir konumda bir eylemi gerçekleştirip gerçekleştiremeyeceğini WorldGuard'a sorar.
     * Sorguyu {@link IWorldGuardWorker} aracılığıyla gerçekleştirir ve sonucu standart bir
     * {@link ShieldResponse} nesnesine dönüştürür.
     *
     * @param player   Eylemi gerçekleştiren oyuncu.
     * @param location Eylemin gerçekleştiği konum.
     * @param type     Gerçekleştirilen etkileşim türü.
     * @return Eyleme izin veriliyorsa {@code ShieldResponse.allow()}, verilmiyorsa {@code ShieldResponse.deny()}.
     */
    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        boolean allowed = worker.canBuild(player, location, type);
        return allowed ? ShieldResponse.allow() : ShieldResponse.deny(getName());
    }

    /**
     * Bu hook'un öncelik seviyesini döndürür.
     * WorldGuard genellikle temel koruma eklentisi olduğu için en yüksek önceliğe sahiptir.
     *
     * @return {@link ShieldPriority#HIGHEST}.
     */
    @Override
    public ShieldPriority getPriority() {
        return ShieldPriority.HIGHEST;
    }

    /**
     * Belirtilen konumdaki en yüksek öncelikli WorldGuard bölgesi hakkında bilgi alır.
     *
     * @param location Bilgi alınacak konum.
     * @return Konumda bir bölge varsa bir {@link RegionInfo} nesnesi,
     *         aksi takdirde veya bir hata oluşursa null.
     */
    @Override
    public RegionInfo getRegionInfo(Location location) {
        try {
            com.sk89q.worldedit.util.Location weLoc = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
            com.sk89q.worldguard.protection.regions.RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.managers.RegionManager regions = container.get((com.sk89q.worldedit.world.World) weLoc.getExtent());

            if (regions == null) return null;

            com.sk89q.worldguard.protection.ApplicableRegionSet set = regions.getApplicableRegions(weLoc.toVector().toBlockPoint());

            if (set.size() == 0) return null;

            com.sk89q.worldguard.protection.regions.ProtectedRegion region = set.getRegions().iterator().next();

            return RegionInfo.builder()
                    .id(region.getId())
                    .provider(getName())
                    .owners(new java.util.ArrayList<>(region.getOwners().getUniqueIds()))
                    .members(new java.util.ArrayList<>(region.getMembers().getUniqueIds()))
                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Belirtilen konumdaki bölgenin sınırlarını alır.
     * Bu işlemi sürüm bağımlı worker'a devreder.
     *
     * @param location Sınırları alınacak bölgenin içindeki bir konum.
     * @return Bölgenin sınırlarını içeren bir {@link com.bentahsin.regionshield.model.RegionBounds} nesnesi
     *         veya bölge bulunamazsa null.
     */
    @Override
    public com.bentahsin.regionshield.model.RegionBounds getRegionBounds(Location location) {
        return worker.getRegionBounds(location);
    }
}