package com.bentahsin.regionshield.events;

import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Bir oyuncu BenthRegionShield tarafından tanınan bir bölgeden ayrıldığında tetiklenir.
 * <p>
 * Bu olay, başka eklentilerin bir oyuncunun bir bölgeden ayrılmasını engellemesine olanak tanıyan
 * {@link Cancellable} arayüzünü uygular. Bu olayı iptal etmek, oyuncunun bölgeden
 * ayrılmasına neden olan asıl hareketi (örneğin {@code PlayerMoveEvent}) de iptal edecektir.
 */
public class BenthRegionLeaveEvent extends BenthRegionEvent implements Cancellable {

    private boolean cancelled = false;

    /**
     * Yeni bir BenthRegionLeaveEvent oluşturur.
     *
     * @param player Oyuncu, ayrılan.
     * @param region Bölge, ayrılınan.
     */
    public BenthRegionLeaveEvent(Player player, RegionInfo region) {
        super(player, region);
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
     * {@code true} olarak ayarlanması, oyuncunun bölgeden ayrılmasını engelleyecektir.
     *
     * @param cancel İptal durumu; true ise olayı iptal et.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}