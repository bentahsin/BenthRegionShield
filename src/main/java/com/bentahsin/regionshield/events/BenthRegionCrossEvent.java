package com.bentahsin.regionshield.events;

import com.bentahsin.regionshield.model.RegionInfo;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Bir oyuncunun, aradaki "Vahşi Doğa" (Wilderness) alanına uğramadan, doğrudan bir korumalı
 * bölgeden (Region A) bitişik başka bir korumalı bölgeye (Region B) geçtiği anlarda tetiklenir.
 * <p>
 * <b>Performans Notu:</b>
 * Bu olay tetiklendiğinde, sistem ayrıca bir {@link BenthRegionLeaveEvent} veya
 * {@link BenthRegionEnterEvent} <u>TETİKLEMEZ</u>. Bu bir optimizasyon tercihidir;
 * bitişik bölgeler arasındaki geçişlerde (örneğin yan yana olan Towny arsaları veya
 * iç içe geçmiş WorldGuard bölgeleri) olay trafiğini yarıya indirir.
 * <p>
 * Eğer hem çıkış hem giriş işlemlerini dinlemek istiyorsanız, bu olayı ayrıca dinlemeniz gerekmektedir.
 * <p>
 * Bu olay {@link Cancellable} arayüzünü uygular. İptal edilmesi durumunda, oyuncunun
 * o hareketi (PlayerMoveEvent veya PlayerTeleportEvent) iptal edilir ve oyuncu
 * {@code fromRegion} (eski bölge) sınırları içinde kalır.
 */
@Getter
public class BenthRegionCrossEvent extends BenthRegionEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    /**
     * Oyuncunun ayrılmakta olduğu (çıktığı) bölge.
     */
    private final RegionInfo fromRegion;

    /**
     * Oyuncunun girmekte olduğu (hedef) bölge.
     */
    private final RegionInfo toRegion;

    /**
     * Yeni bir BenthRegionCrossEvent örneği oluşturur.
     *
     * @param player     Sınır geçişini yapan oyuncu.
     * @param fromRegion Oyuncunun çıktığı kaynak bölge.
     * @param toRegion   Oyuncunun girdiği hedef bölge.
     */
    public BenthRegionCrossEvent(Player player, RegionInfo fromRegion, RegionInfo toRegion) {
        super(player, toRegion);
        this.fromRegion = fromRegion;
        this.toRegion = toRegion;
    }

    /**
     * Bu olayın iptal edilip edilmediğini kontrol eder.
     *
     * @return Olay iptal edilmişse {@code true}, aksi takdirde {@code false}.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Bu olayın iptal durumunu ayarlar.
     * <p>
     * {@code true} olarak ayarlanması, oyuncunun fiziksel hareketini durdurur.
     * Oyuncu, {@code toRegion}'a giremez ve {@code fromRegion}'da kalmaya zorlanır.
     *
     * @param cancel İptal durumu; true ise geçişi engelle.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Bukkit'in olay sisteminin gerektirdiği standart HandlerList'i döndürür.
     *
     * @return Bu olay türü için olan HandlerList.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Bukkit'in olay sisteminin gerektirdiği standart statik HandlerList'i döndürür.
     *
     * @return Bu olay türü için olan statik HandlerList.
     */
    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}