package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.BenthRegionShield;
import com.bentahsin.regionshield.events.BenthRegionEnterEvent;
import com.bentahsin.regionshield.events.BenthRegionLeaveEvent;
import com.bentahsin.regionshield.model.RegionInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Oyuncuların bölgeler arasındaki hareketlerini izleyen ve bu hareketlere bağlı olarak
 * özel {@link BenthRegionEnterEvent} ve {@link BenthRegionLeaveEvent} olaylarını tetikleyen dahili bir dinleyici sınıfı.
 * <p>
 * Her oyuncunun bulunduğu son bölgeyi hafızada tutarak bölge değişikliklerini algılar.
 * Bu sınıf, API'nin dahili bir parçasıdır ve son kullanıcılar tarafından doğrudan kullanılması amaçlanmamıştır.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class RegionMovementListener implements Listener {

    private final BenthRegionShield manager;
    /**
     * Oyuncuların en son bulundukları bölge bilgilerini saklar.
     * Anahtar (Key): Oyuncunun UUID'si
     * Değer (Value): En son bulunduğu RegionInfo nesnesi (eğer vahşi doğada ise null olabilir).
     */
    private final Map<UUID, RegionInfo> lastRegions = new HashMap<>();

    /**
     * Yeni bir RegionMovementListener örneği oluşturur.
     *
     * @param manager Ana BenthRegionShield API yöneticisi.
     */
    public RegionMovementListener(BenthRegionShield manager) {
        this.manager = manager;
    }

    /**
     * Oyuncu sunucuya katıldığında, mevcut konumunu başlangıç bölgesi olarak ayarlar.
     * Bu, oyuncunun oyuna bir bölgenin içinde başlaması durumunda durumun doğru bir şekilde kaydedilmesini sağlar.
     *
     * @param event PlayerJoinEvent olayı.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        handleMove(event.getPlayer(), event.getPlayer().getLocation(), null);
    }

    /**
     * Oyuncu sunucudan ayrıldığında, izleme haritasından verilerini kaldırır.
     * Bu, gereksiz bellek kullanımını ve olası bellek sızıntılarını önler.
     *
     * @param event PlayerQuitEvent olayı.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastRegions.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Oyuncu hareket ettiğinde tetiklenir.
     * Sadece oyuncu farklı bir bloğa geçtiğinde hareket mantığını çalıştırır, bu da performansı artırır.
     *
     * @param event PlayerMoveEvent olayı.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        handleMove(event.getPlayer(), to, event);
    }

    /**
     * Oyuncu ışınlandığında tetiklenir.
     * Işınlanma da bir bölge değişikliğine neden olabileceği için hareket mantığını çağırır.
     *
     * @param event PlayerTeleportEvent olayı.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        handleMove(event.getPlayer(), event.getTo(), event);
    }

    /**
     * Tüm oyuncu hareketlerinin (normal yürüme, ışınlanma, sunucuya katılma) ana mantığını işler.
     * <p>
     * 1. Oyuncunun yeni konumundaki bölgeyi mevcut bölgesiyle karşılaştırır.
     * 2. Eğer bir değişiklik varsa, uygun {@link BenthRegionLeaveEvent} veya {@link BenthRegionEnterEvent} olaylarını tetikler.
     * 3. Bu olayların iptal edilip edilmediğini kontrol eder ve gerekirse asıl hareketi (PlayerMoveEvent/PlayerTeleportEvent) iptal eder.
     * 4. Son olarak, oyuncunun son bilinen bölgesini günceller.
     *
     * @param player      Hareketi gerçekleştiren oyuncu.
     * @param to          Oyuncunun yeni konumu.
     * @param parentEvent Eğer bu hareket bir Bukkit olayı (Move/Teleport) tarafından tetiklendiyse, o olay referansıdır.
     *                    Bu, Enter/Leave olayları iptal edildiğinde asıl hareketin de iptal edilebilmesi için kullanılır.
     *                    Sunucuya giriş gibi durumlarda null olabilir.
     */
    private void handleMove(Player player, Location to, Cancellable parentEvent) {
        UUID uuid = player.getUniqueId();
        RegionInfo currentRegion = manager.getRegionInfo(to);
        RegionInfo lastRegion = lastRegions.get(uuid);

        boolean changed = !Objects.equals(lastRegion, currentRegion);

        if (changed) {
            if (lastRegion != null) {
                BenthRegionLeaveEvent leaveEvent = new BenthRegionLeaveEvent(player, lastRegion);
                Bukkit.getPluginManager().callEvent(leaveEvent);

                if (leaveEvent.isCancelled()) {
                    if (parentEvent != null) {
                        parentEvent.setCancelled(true);
                    }
                    return;
                }
            }

            if (currentRegion != null) {
                BenthRegionEnterEvent enterEvent = new BenthRegionEnterEvent(player, currentRegion);
                Bukkit.getPluginManager().callEvent(enterEvent);

                if (enterEvent.isCancelled()) {
                    if (parentEvent != null) {
                        parentEvent.setCancelled(true);
                    }
                    return;
                }
            }

            lastRegions.put(uuid, currentRegion);
        }
    }
}