package com.bentahsin.regionshield.annotations;

import org.bukkit.Material;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bir metodu veya bir sınıfın tamamını, işlemin çalışması için oyuncunun
 * belirli bir blok türünün üzerinde durmasını (veya içinde olmasını) zorunlu kılacak
 * şekilde işaretler.
 * <p>
 * Bu, örneğin sadece Zümrüt Blok üzerinde çalışacak bir "zıplama rampası" komutu veya
 * sadece oyuncu suyun içindeyken çalışacak bir "kova doldurma" komutu oluşturmak için
 * kullanılabilir.
 * <p>
 * Bu ek açıklama, bir metoda veya o metodu içeren tüm sınıfa uygulanabilir.
 * Eğer hem sınıfta hem de metotta kullanılırsa, metottaki ek açıklama sınıftakini geçersiz kılar.
 * <p>
 * <b>Örnek Kullanım:</b>
 * <pre>
 * {@code
 * // Bu komutun sadece oyuncu Zümrüt Bloğun üzerinde duruyorsa çalışmasını sağla.
 * @RequireBlock(Material.EMERALD_BLOCK)
 * public void activateJumpPad(Player player) {
 *     // Oyuncuyu zıplat...
 * }
 *
 * // Bu komutun sadece oyuncu suyun içindeyken çalışmasını sağla.
 * @RequireBlock(value = Material.WATER, checkGround = false)
 * public void refillWaterBottle(Player player) {
 *     // Oyuncunun su şişesini doldur...
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireBlock {

    /**
     * İşlemin geçerli olması için oyuncunun üzerinde/içinde olması gereken
     * materyallerin bir listesini belirtir.
     * <p>
     * İlgili bloğun türü bu listedeki materyallerden biriyse, denetim başarılı olur.
     *
     * @return İzin verilen materyallerin dizisi.
     */
    Material[] value();

    /**
     * Oyuncunun konumuna göre hangi bloğun kontrol edileceğini belirler.
     * <ul>
     *     <li>{@code true} (varsayılan): Oyuncunun ayaklarının hemen altındaki bloğu
     *     (yani üzerinde durduğu bloğu) kontrol eder.</li>
     *     <li>{@code false}: Oyuncunun ayaklarının bulunduğu koordinattaki bloğu
     *     (yani içinde durduğu bloğu) kontrol eder. Bu, su veya lav gibi
     *     sıvıların içindeyken veya ağ gibi blokların içindeyken kullanışlıdır.</li>
     * </ul>
     *
     * @return {@code true} ise basılan bloğu, {@code false} ise içindeki bloğu kontrol et.
     */
    boolean checkGround() default true;
}