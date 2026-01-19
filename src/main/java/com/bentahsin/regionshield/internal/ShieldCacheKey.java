package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.model.InteractionType;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Shield (kalkan) etkileşim sonuçlarının önbelleği için kullanılan anahtar sınıfı.
 * Bir oyuncunun, belirli bir blok koordinatında, belirli bir dünyada gerçekleştirdiği
 * spesifik bir etkileşim türünü benzersiz bir şekilde temsil eder.
 * <p>
 * Bu sınıf, anahtar olarak birleştirilmiş bir String kullanmaya göre yüksek performanslı,
 * bellek (RAM) ve Çöp Toplayıcı (GC) dostu bir alternatif olarak tasarlanmıştır.
 * Sık çağrılan işlemlerde String birleştirmenin getireceği ek yükü ortadan kaldırır.
 * <p>
 * Lombok'un {@code @RequiredArgsConstructor} ve {@code @EqualsAndHashCode} ek açıklamaları ile
 * sınıf değişmez (immutable) ve haritalarda (Map) anahtar olarak güvenle kullanılabilir hale getirilmiştir.
 */
@RequiredArgsConstructor
@EqualsAndHashCode
public class ShieldCacheKey {
    /** Etkileşimi gerçekleştiren oyuncunun benzersiz kimliği (UUID). */
    private final UUID playerUUID;

    /** Etkileşimin gerçekleştiği dünyanın adı. */
    private final String worldName;

    /** Etkileşimin gerçekleştiği bloğun X koordinatı. */
    private final int x;

    /** Etkileşimin gerçekleştiği bloğun Y koordinatı. */
    private final int y;

    /** Etkileşimin gerçekleştiği bloğun Z koordinatı. */
    private final int z;

    /** Gerçekleştirilen etkileşimin türü ({@link InteractionType}). */
    private final InteractionType type;
}