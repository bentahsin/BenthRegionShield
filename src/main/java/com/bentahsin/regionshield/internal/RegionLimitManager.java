package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.BenthRegionShield;
import com.bentahsin.regionshield.events.BenthRegionEnterEvent;
import com.bentahsin.regionshield.events.BenthRegionLeaveEvent;
import com.bentahsin.regionshield.model.RegionInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Belirli bölgeler için oyuncu sayısı limitlerini yöneten ve uygulayan dahili sınıf.
 * Bu sınıf, {@link BenthRegionEnterEvent} olayını dinleyerek, dolu bir bölgeye girişi
 * engeller ve bölgedeki mevcut oyuncu sayısını anlık olarak takip eder.
 * <p>
 * Sunucu başlangıcında veya yeniden başlatıldığında, mevcut oyuncu sayılarını doğru bir şekilde
 * başlatmak için çevrimiçi oyuncuları yeniden sayar.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class RegionLimitManager implements Listener {

    private final BenthRegionShield manager;
    /**
     * Tanımlanmış bölge limitlerini saklar.
     * Anahtar: "provider:regionid" formatında (küçük harfli).
     * Değer: Maksimum oyuncu sayısı.
     */
    private final Map<String, Integer> limits = new HashMap<>();
    /**
     * Her bir limitli bölgedeki mevcut oyuncu sayısını anlık olarak saklar.
     * Bu harita, potansiyel olarak eşzamanlı erişimlere karşı güvenli olması için
     * {@link ConcurrentHashMap} olarak tanımlanmıştır.
     */
    private final Map<String, Integer> activeCounts = new ConcurrentHashMap<>();

    /**
     * Yeni bir RegionLimitManager örneği oluşturur ve mevcut oyuncu durumunu sayar.
     *
     * @param manager Ana BenthRegionShield API yöneticisi.
     */
    public RegionLimitManager(BenthRegionShield manager) {
        this.manager = manager;
        recountPlayers();
    }

    /**
     * Belirli bir bölge için bir oyuncu limiti belirler. Eğer aynı bölge için
     * önceden bir limit belirlenmişse, üzerine yazılır.
     * <p>
     * Örnek Kullanım: {@code api.setRegionLimit("WorldGuard", "arena", 10);}
     *
     * @param provider Bölgeyi sağlayan eklentinin adı (örn: "WorldGuard").
     * @param regionId Limitin uygulanacağı bölgenin ID'si.
     * @param limit    Bu bölge için izin verilen maksimum oyuncu sayısı.
     */
    public void setLimit(String provider, String regionId, int limit) {
        String key = (provider + ":" + regionId).toLowerCase();
        limits.put(key, limit);
    }

    /**
     * Belirli bir bölge için daha önce ayarlanmış olan oyuncu limitini kaldırır.
     *
     * @param provider Bölgeyi sağlayan eklentinin adı.
     * @param regionId Limiti kaldırılacak bölgenin ID'si.
     */
    public void removeLimit(String provider, String regionId) {
        String key = (provider + ":" + regionId).toLowerCase();
        limits.remove(key);
    }

    /**
     * Tüm çevrimiçi oyuncuları tarayarak limitli bölgelerdeki mevcut oyuncu sayılarını
     * yeniden hesaplar. Bu metot, sunucu başlangıcında veya /reload gibi durumlarda
     * oyuncu sayılarının senkronize kalmasını sağlamak için kullanılır.
     */
    public void recountPlayers() {
        activeCounts.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            RegionInfo info = manager.getRegionInfo(p.getLocation());
            if (info != null) {
                increment(info);
            }
        }
    }

    /**
     * Belirtilen bir bölgenin oyuncu limiti kapasitesine ulaşıp ulaşmadığını kontrol eder.
     *
     * @param info Kontrol edilecek bölge.
     * @return Bölge doluysa (mevcut oyuncu >= limit) true, aksi takdirde false.
     *         Eğer bölge null ise veya bölge için bir limit tanımlanmamışsa false döner.
     */
    public boolean isFull(RegionInfo info) {
        if (info == null) return false;

        String key = getKey(info);
        if (!limits.containsKey(key)) return false;

        int limit = limits.get(key);
        int current = activeCounts.getOrDefault(key, 0);

        return current >= limit;
    }

    /**
     * Bir oyuncu bir bölgeye girdiğinde tetiklenir. Bu metot, en düşük öncelikle çalışarak
     * bölgeye giriş olayını en başta yakalar ve gerekirse iptal eder.
     *
     * @param event BenthRegionEnterEvent olayı.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onRegionEnter(BenthRegionEnterEvent event) {
        if (event.isCancelled()) return;

        RegionInfo info = event.getRegion();

        if (isFull(info) && !event.getPlayer().hasPermission("regionshield.bypass.limit")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cBu bölge dolu! (" + activeCounts.get(getKey(info)) + "/" + limits.get(getKey(info)) + ")");
            return;
        }
        increment(info);
    }

    /**
     * Bir oyuncu bir bölgeden ayrıldığında tetiklenir. Sadece olayı izler ve sayacı düşürür.
     *
     * @param event BenthRegionLeaveEvent olayı.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRegionLeave(BenthRegionLeaveEvent event) {
        decrement(event.getRegion());
    }

    /**
     * Bir oyuncu oyundan ayrıldığında, bulunduğu bölgenin sayacını düşürür.
     * Bu, oyuncu bir bölgenin içindeyken oyundan çıktığında sayacın doğru kalmasını sağlar.
     *
     * @param event PlayerQuitEvent olayı.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        RegionInfo info = manager.getRegionInfo(event.getPlayer().getLocation());
        if (info != null) decrement(info);
    }

    /**
     * Bir oyuncu oyuna katıldığında, bulunduğu bölgenin sayacını artırır.
     * Bu, oyuncu limitli bir bölgede oyuna başladığında sayacın doğru olmasını sağlar.
     *
     * @param event PlayerJoinEvent olayı.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        RegionInfo info = manager.getRegionInfo(event.getPlayer().getLocation());
        if (info != null) increment(info);
    }

    /**
     * Belirtilen bölgenin oyuncu sayacını bir artırır.
     * Sadece bölge için bir limit tanımlanmışsa işlem yapar.
     *
     * @param info Sayacı artırılacak bölge.
     */
    private void increment(RegionInfo info) {
        String key = getKey(info);
        if (limits.containsKey(key)) {
            activeCounts.merge(key, 1, Integer::sum);
        }
    }

    /**
     * Belirtilen bölgenin oyuncu sayacını bir azaltır.
     * Sadece bölge için bir limit tanımlanmışsa işlem yapar. Sayacın sıfırın altına düşmesini engeller.
     *
     * @param info Sayacı azaltılacak bölge.
     */
    private void decrement(RegionInfo info) {
        String key = getKey(info);
        if (limits.containsKey(key)) {
            activeCounts.computeIfPresent(key, (k, v) -> v > 0 ? v - 1 : 0);
        }
    }

    /**
     * Bir RegionInfo nesnesinden limit haritaları için standartlaştırılmış bir anahtar oluşturur.
     *
     * @param info Anahtar oluşturulacak bölge bilgisi.
     * @return "provider:id" formatında küçük harfli bir String.
     */
    private String getKey(RegionInfo info) {
        return (info.getProvider() + ":" + info.getId()).toLowerCase();
    }
}