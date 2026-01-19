package com.bentahsin.regionshield.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bir koruma bölgesi hakkında detaylı bilgileri temsil eder.
 * Bu sınıf, bölgenin ID'si, sahipleri, üyeleri ve bu bilgiyi sağlayan
 * kaynak (provider) gibi verileri kapsar.
 * <p>
 * Sınıf, dışarıdan yapılacak değişikliklere karşı korumalı (immutable) olacak şekilde tasarlanmıştır.
 * Sahiplerin ve üyelerin listeleri, hem nesne oluşturulurken hem de getter metotlarında
 * kopyalanarak dahili durumun korunması sağlanır.
 */
@Getter
@ToString
@SuppressFBWarnings({"EI_EXPOSE_REP2"})
public class RegionInfo {
    /**
     * Bölgenin benzersiz kimliği (ID).
     */
    private final String id;

    /**
     * Bölge sahiplerinin UUID listesi.
     */
    private final List<UUID> owners;

    /**
     * Bölge üyelerinin UUID listesi.
     */
    private final List<UUID> members;

    /**
     * Bu bölge bilgisini sağlayan eklentinin veya kaynağın adı (Örn: "WorldGuard").
     */
    private final String provider;

    /**
     * Yeni bir RegionInfo nesnesi oluşturur. Genellikle Lombok'un @Builder'ı ile kullanılır.
     * Sağlanan sahip ve üye listeleri, nesnenin değişmezliğini sağlamak için
     * defansif olarak kopyalanır.
     *
     * @param id       Bölgenin benzersiz kimliği.
     * @param owners   Bölge sahiplerinin UUID'lerini içeren liste.
     * @param members  Bölge üyelerinin UUID'lerini içeren liste.
     * @param provider Bu bilgiyi sağlayan kaynağın adı.
     */
    @Builder
    public RegionInfo(String id, List<UUID> owners, List<UUID> members, String provider) {
        this.id = id;
        this.provider = provider;
        this.owners = owners != null ? new ArrayList<>(owners) : new ArrayList<>();
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    /**
     * Bölge sahiplerinin UUID listesinin bir kopyasını döndürür.
     * Bu nesnenin dahili durumunun değiştirilmesini önlemek için yeni bir liste oluşturulur.
     *
     * @return Sahiplerin UUID'lerini içeren yeni bir liste.
     */
    public List<UUID> getOwners() {
        return new ArrayList<>(owners);
    }

    /**
     * Bölge üyelerinin UUID listesinin bir kopyasını döndürür.
     * Bu nesnenin dahili durumunun değiştirilmesini önlemek için yeni bir liste oluşturulur.
     *
     * @return Üyelerin UUID'lerini içeren yeni bir liste.
     */
    public List<UUID> getMembers() {
        return new ArrayList<>(members);
    }
}