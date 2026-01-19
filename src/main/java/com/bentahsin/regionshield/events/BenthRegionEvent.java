package com.bentahsin.regionshield.events;

import com.bentahsin.regionshield.model.RegionInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * BenthRegionShield API'si tarafından tetiklenen tüm özel bölge olayları için temel (abstract) sınıf.
 * <p>
 * Bu sınıf, bir oyuncu ve bir bölge gibi tüm alt olaylar ({@link BenthRegionEnterEvent}, {@link BenthRegionLeaveEvent} vb.)
 * tarafından paylaşılan ortak verileri içerir.
 * <p>
 * Doğrudan dinlenmesi veya oluşturulması amaçlanmamıştır; bunun yerine bu sınıfı genişleten
 * spesifik olay sınıfları kullanılmalıdır.
 */
@Getter
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public abstract class BenthRegionEvent extends Event {

    /**
     * Bukkit olay sistemi için gerekli olan standart HandlerList.
     */
    private static final HandlerList handlers = new HandlerList();

    /**
     * Bu olayı tetikleyen oyuncu.
     */
    protected final Player player;
    /**
     * Bu olayla ilişkili bölge.
     */
    protected final RegionInfo region;

    /**
     * Yeni bir BenthRegionEvent oluşturur.
     *
     * @param player Olayla ilişkili oyuncu.
     * @param region Olayla ilişkili bölge.
     */
    public BenthRegionEvent(Player player, RegionInfo region) {
        this.player = player;
        this.region = region;
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