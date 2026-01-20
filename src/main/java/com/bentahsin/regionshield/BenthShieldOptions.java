package com.bentahsin.regionshield;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

/**
 * BenthRegionShield kütüphanesinin çalışma davranışlarını özelleştirmek için kullanılan yapılandırma sınıfı.
 * <p>
 * Kütüphaneyi başlatan geliştirici, bu sınıfı kullanarak yetki düğümlerini (permissions),
 * mesajları, görsel efektleri ve loglama tercihlerini belirleyebilir.
 */
@Getter
@Builder
public class BenthShieldOptions {

    /**
     * Tüm bölge korumalarını (RegionCheck, RegionRole vb.) görmezden gelmeyi sağlayan genel yetki.
     * <p>
     * Varsayılan: "regionshield.bypass"
     */
    @Builder.Default
    private String bypassPermission = "regionshield.bypass";

    /**
     * Bölge kişi limiti (RegionLimit) dolu olsa bile giriş yapmayı sağlayan yetki.
     * <p>
     * Varsayılan: "regionshield.bypass.limit"
     */
    @Builder.Default
    private String limitBypassPermission = "regionshield.bypass.limit";

    /**
     * Konsola basılan logların (hook aktif, hata vb.) gösterilip gösterilmeyeceği.
     * <p>
     * Varsayılan: true
     */
    @Builder.Default
    private boolean logToConsole = true;

    /**
     * Konsol loglarının başında yer alacak önek.
     * <p>
     * Varsayılan: "[RegionShield] "
     */
    @Builder.Default
    private String logPrefix = "[RegionShield] ";

    /**
     * Bölge sınırlarını görselleştirirken kullanılacak parçacık türü.
     * <p>
     * Varsayılan: Particle.FLAME
     */
    @Builder.Default
    private Particle visualizationParticle = Particle.FLAME;

    /**
     * Bir oyuncu dolu bir bölgeye girmeye çalıştığında çalıştırılacak mantık.
     * <p>
     * İlk parametre: Oyuncu
     * İkinci parametre: Dolu olan bölge için "MevcutOyuncu/MaxOyuncu" formatında bilgi stringi (opsiyonel kullanım için).
     * <p>
     * Varsayılan: Hiçbir şey yapma (Sadece girişi engeller, mesaj atmaz).
     */
    @Builder.Default
    private BiConsumer<Player, String> limitRejectionHandler = (player, info) -> {};

    /**
     * Varsayılan ayarlarla boş bir options nesnesi döndürür.
     */
    public static BenthShieldOptions defaults() {
        return BenthShieldOptions.builder().build();
    }
}