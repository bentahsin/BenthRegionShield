package com.bentahsin.regionshield.annotations;

/**
 * Bir oyuncunun bir bölge içindeki rolünü veya yetki seviyesini tanımlayan türleri belirtir.
 * Bu enum, {@link RegionRole} ek açıklaması ile birlikte kullanılarak bir eylemin
 * gerçekleştirilmesi için gereken minimum yetki seviyesini belirler.
 */
public enum RegionRoleType {
    /**
     * Oyuncunun, bölgenin en yetkili kişisi olmasını gerektirir.
     * Bu genellikle bölgenin kurucusu, sahibi, lideri veya belediye başkanı gibi rollere karşılık gelir.
     */
    OWNER,

    /**
     * Oyuncunun, bölgenin sahibi VEYA üyesi olmasını gerektirir.
     * Bu, bölgeye güvenilmiş (trusted) veya eklenmiş olan herkesi kapsar, ancak
     * bölgeyle hiçbir ilişkisi olmayan yabancıları (ziyaretçileri) hariç tutar.
     * Bu, en yaygın kullanılan roldür.
     */
    MEMBER_OR_OWNER,

    /**
     * Oyuncunun sadece bölgenin içinde bulunmasının yeterli olduğunu belirtir.
     * Bu, en düşük gereksinim seviyesidir ve oyuncunun bölge üzerinde herhangi bir
     * özel yetkisi olmasa bile denetimin başarılı olmasını sağlar.
     * Bu rol, bir eylemin sadece bir bölge içinde gerçekleşmesini zorunlu kılmak,
     * ancak kimin yaptığıyla ilgilenmemek için kullanılır.
     */
    VISITOR
}