package com.bentahsin.regionshield.annotations;

public enum RegionRoleType {
    /**
     * Sadece bölgenin kurucusu/sahibi (Owner/Mayor/Leader).
     */
    OWNER,

    /**
     * Bölgenin sahibi VEYA üyesi (Member/Trusted).
     * Yabancılar hariç herkes.
     */
    MEMBER_OR_OWNER,

    /**
     * Sadece o bölgede bulunması yeterli (Visitor).
     * Yetkisi olmasa bile çalışır.
     */
    VISITOR
}