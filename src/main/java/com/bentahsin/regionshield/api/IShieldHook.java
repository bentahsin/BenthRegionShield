package com.bentahsin.regionshield.api;

import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface IShieldHook {

    /**
     * Hook'un (Entegrasyonun) adını döndürür.
     * @return Eklenti adı (Örn: WorldGuard)
     */
    String getName();

    /**
     * Bu hook'un sunucuda aktif olup olmadığını kontrol eder.
     * Plugin yüklü mü? Sürüm uyumlu mu?
     * @return Yüklenebilirse true
     */
    boolean canInitialize();

    /**
     * Ana yetki kontrol metodu.
     * @param player İşlemi yapan oyuncu
     * @param location İşlemin yapıldığı lokasyon
     * @param type İşlem türü
     * @return ShieldResponse (İzin veya Red)
     */
    ShieldResponse check(Player player, Location location, InteractionType type);

    /**
     * Hook'un çalışma önceliği. Yüksek olanlar önce kontrol edilir.
     * @return ShieldPriority
     */
    default ShieldPriority getPriority() {
        return ShieldPriority.NORMAL;
    }

    /**
     * Verilen lokasyondaki bölge bilgilerini döndürür.
     * Eğer bölge yoksa null döner.
     */
    default RegionInfo getRegionInfo(Location location) {
        return null;
    }
}