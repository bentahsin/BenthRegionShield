package com.bentahsin.regionshield.model;

/**
 * Oyuncunun bölge üzerinde gerçekleştirebileceği eylem türlerini tanımlar.
 * Tüm Hook'lar bu türleri kendi iç yapılarına (Flag, Permission vb.) dönüştürmelidir.
 */
public enum InteractionType {
    /** Blok kırma işlemi (Madencilik vb.) */
    BLOCK_BREAK,

    /** Blok koyma işlemi (İnşaat) */
    BLOCK_PLACE,

    /** Sandık, Fırın, Varil, Shulker vb. envanterli blokları açma */
    CONTAINER_ACCESS,

    /** Kapı, Tuzak Kapısı, Buton, Şalter, Basınç Plakası gibi blok etkileşimleri */
    INTERACT,

    /** Oyuncular arası savaş (Player vs Player) */
    PVP,

    /** Yaratıklara veya hayvanlara hasar verme (Player vs Mob) */
    MOB_DAMAGE,

    /** Yere sıvı dökme veya alma (Kova kullanımı) */
    BUCKET_USE,

    /** Genel varlık hasarı (Armor Stand, Item Frame vb. cansız varlıklar için kullanılabilir) */
    DAMAGE_ENTITY,

    /** Tarlaları çiğneme (Ekinlere zarar verme) */
    TRAMPLE
}