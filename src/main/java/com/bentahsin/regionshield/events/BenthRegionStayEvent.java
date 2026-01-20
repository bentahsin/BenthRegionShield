package com.bentahsin.regionshield.events;

import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Bir oyuncu BenthRegionShield tarafından tanınan bir bölgenin sınırları içinde
 * kalmaya devam ettiği sürece, belirli zaman aralıklarıyla (periyodik olarak) tetiklenen olaydır.
 * <p>
 * Bu olay, oyuncu hareket etmese bile tetiklenir. Genellikle {@code RegionStayTask}
 * tarafından varsayılan olarak her 5 saniyede bir (veya yapılandırılan süre kadar) çağrılır.
 * <p>
 * <b>Kullanım Alanları:</b>
 * <ul>
 *     <li>Radyasyonlu bölgede oyuncuya zamanla hasar vermek.</li>
 *     <li>Güvenli bölgede (Safezone) oyuncunun canını veya açlığını yenilemek.</li>
 *     <li>Bölge içindeki oyunculara Action Bar veya Scoreboard üzerinden mesaj göstermek.</li>
 *     <li>Bölgeye özel "harçlık" veya ödül sistemleri (dakika başına para kazanma vb.).</li>
 * </ul>
 */
public class BenthRegionStayEvent extends BenthRegionEvent implements Cancellable {

    private boolean cancelled = false;

    /**
     * Yeni bir BenthRegionStayEvent örneği oluşturur.
     *
     * @param player O anda bölge sınırları içinde bulunan oyuncu.
     * @param region Oyuncunun içinde bulunduğu bölge.
     */
    public BenthRegionStayEvent(Player player, RegionInfo region) {
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
     * <p>
     * <b>Önemli Not:</b> Bu olayı iptal etmek ({@code true}), {@link BenthRegionEnterEvent}'in
     * aksine oyuncuyu fiziksel olarak bölgeden atmaz veya hareketini engellemez.
     * <p>
     * İptal işlemi, sadece o anki zamanlayıcı döngüsünde (tick) bu olay için planlanan
     * diğer eylemlerin (listener'ların) çalışmasını durdurur. Örneğin, bir listener
     * oyuncuya hasar veriyorsa, olayı iptal etmek o hasarı o anlık engeller.
     *
     * @param cancel İptal durumu; true ise o döngüdeki işlemleri durdur.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}