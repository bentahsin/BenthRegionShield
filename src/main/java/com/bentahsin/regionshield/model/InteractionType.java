package com.bentahsin.regionshield.model;

/**
 * Oyuncuların dünyada gerçekleştirebileceği çeşitli etkileşim türlerini temsil eder.
 * Bu enum, RegionShield API'sinin hangi eylemin denetlendiğini belirlemesi için kullanılır.
 */
public enum InteractionType {
    /**
     * Blok kırma eylemini temsil eder (örneğin, madencilik).
     */
    BLOCK_BREAK,

    /**
     * Blok yerleştirme eylemini temsil eder (örneğin, inşaat).
     */
    BLOCK_PLACE,

    /**
     * Sandık, Fırın, Varil, Shulker Kutusu gibi envantere sahip bloklara erişim eylemini temsil eder.
     */
    CONTAINER_ACCESS,

    /**
     * Kapı, Tuzak Kapısı, Düğme, Şalter veya Basınç Plakası gibi
     * interaktif bloklarla etkileşim eylemini temsil eder.
     */
    INTERACT,

    /**
     * Oyuncular arasındaki savaşı (PvP - Player versus Player) temsil eder.
     */
    PVP,

    /**
     * Oyuncunun yaratıklara veya hayvanlara hasar vermesini (PvE - Player versus Environment) temsil eder.
     */
    MOB_DAMAGE,

    /**
     * Kova kullanarak yere sıvı dökme veya yerden sıvı alma eylemini temsil eder.
     */
    BUCKET_USE,

    /**
     * Zırh Askısı (Armor Stand) veya Eşya Çerçevesi (Item Frame) gibi
     * cansız varlıklara hasar verme eylemini temsil eder.
     */
    DAMAGE_ENTITY,

    /**
     * Tarım arazisi üzerindeki ekinleri çiğneyerek yok etme eylemini temsil eder.
     */
    TRAMPLE
}