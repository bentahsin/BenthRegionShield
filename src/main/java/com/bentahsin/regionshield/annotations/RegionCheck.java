package com.bentahsin.regionshield.annotations;

import com.bentahsin.regionshield.model.InteractionType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bir metodu, oyuncunun mevcut konumunda belirli bir etkileşim türünü gerçekleştirip
 * gerçekleştiremeyeceğini kontrol eden standart bir {@code BenthRegionShield#checkResult}
 * denetimiyle korur.
 * <p>
 * Bu, bir komutun veya eylemin yürütülmesini, oyuncunun o anda içinde bulunduğu
 * bölgenin bayraklarına (flags) veya kurallarına göre izni olup olmadığına bağlamak
 * için güçlü bir yoldur.
 * <p>
 * Bu ek açıklama, bir metoda veya o metodu içeren tüm sınıfa uygulanabilir.
 * Eğer hem sınıfta hem de metotta kullanılırsa, metottaki ek açıklama sınıftakini geçersiz kılar.
 * <p>
 * <b>Örnek Kullanım:</b>
 * <pre>
 * {@code
 * // Bu komutun sadece oyuncunun blok yerleştirme izni olan bölgelerde
 * // çalışmasını sağla.
 * @RegionCheck(type = InteractionType.BLOCK_PLACE)
 * public void giveBuildKitCommand(Player player) {
 *     // Oyuncuya inşaat malzemeleri ver...
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RegionCheck {

    /**
     * Oyuncunun konumu için hangi etkileşim türünün kontrol edileceğini belirtir.
     * <p>
     * RegionShield, bu türü tüm kayıtlı hook'lara (WorldGuard, Towny vb.) göndererek
     * bir izin sorgusu yapar.
     *
     * @return Kontrol edilecek {@link InteractionType}. Varsayılan olarak {@link InteractionType#INTERACT}.
     */
    InteractionType type() default InteractionType.INTERACT;

    /**
     * Sadece bu spesifik {@code @RegionCheck} denetimini atlamak için gereken özel
     * bir yetki (permission) tanımlar.
     * <p>
     * Eğer oyuncu bu yetkiye sahipse, bölge kurallarına bakılmaksızın denetim her zaman
     * başarılı olur. Bu, genel {@code regionshield.bypass} yetkisinden daha
     * spesifik bir kontrol sağlar.
     *
     * @return Bu denetim için özel bypass yetkisi. Boş bırakılırsa (varsayılan),
     *         sadece genel bypass izni geçerli olur.
     */
    String bypassPerm() default "";
}