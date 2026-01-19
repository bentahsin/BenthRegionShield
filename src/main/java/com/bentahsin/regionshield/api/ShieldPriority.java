package com.bentahsin.regionshield.api;

import lombok.Getter;

/**
 * {@link IShieldHook} uygulamaları için yürütme önceliği seviyelerini tanımlar.
 * <p>
 * RegionShield, izinleri kontrol ederken tüm kayıtlı hook'ları öncelik sırasına göre
 * (en yüksek sayısal değerden en düşüğe doğru) sorgular. Bir eylemi reddeden (deny)
 * ilk hook, işlemi durdurur ve nihai sonucu belirler.
 * <p>
 * Bu enum, hook'ların hangi sırayla kontrol edileceğini yönetmek için standartlaştırılmış
 * bir seviyeler kümesi sağlar.
 */
@Getter
public enum ShieldPriority {
    /**
     * En düşük öncelik. Bu hook'lar en son çalışır. Geniş kapsamlı veya
     * varsayılan (fallback) kontroller için kullanışlıdır.
     */
    LOWEST(0),

    /**
     * Düşük öncelik. Genellikle daha spesifik hook'lar tarafından geçersiz kılınması
     * gereken genel amaçlı hook'lar içindir.
     */
    LOW(10),

    /**
     * Varsayılan öncelik. Çoğu hook, özel bir nedeni olmadıkça bu seviyeyi kullanmalıdır.
     */
    NORMAL(20),

    /**
     * Yüksek öncelik. Diğer birçok hook'a göre öncelikli olması gereken önemli hook'lar içindir
     * (örneğin, Towny, Lands gibi arsa eklentileri).
     */
    HIGH(30),

    /**
     * En yüksek öncelik. Neredeyse her zaman ilk olarak çalışması gereken kritik hook'lar içindir
     * (örneğin, WorldGuard gibi küresel bir koruma eklentisi).
     */
    HIGHEST(40),

    /**
     * Mutlak öncelik seviyesi; diğer tüm seviyelerden önce çalışır.
     * Herhangi bir koruma eklentisi müdahale etmeden önce bir karar vermesi veya bir eylemi
     * kaydetmesi (log) gereken özel durum hook'ları için tasarlanmıştır.
     */
    MONITOR(50);

    /**
     * Öncelik seviyesinin sayısal değeri. Daha yüksek değerler daha önce yürütülür.
     */
    private final int value;

    /**
     * Belirli bir sayısal değere sahip bir ShieldPriority oluşturur.
     * @param value Önceliğin sayısal temsili.
     */
    ShieldPriority(int value) {
        this.value = value;
    }
}