package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bir metodu veya bir sınıfın tamamını, belirtilen özel bir yetkiye (permission) sahip
 * oyuncular için diğer tüm RegionShield kısıtlamalarını ({@code @RegionCheck},
 * {@code @RegionRole}, {@code @RegionLimit}, {@code @RequireBlock} vb.) devre dışı
 * bırakacak şekilde işaretler.
 * <p>
 * Bu, genel {@code regionshield.bypass} yetkisine sahip olmayan, ancak belirli bir
 * komut için özel bir bypass izni olan oyuncular (örneğin adminler) için bir
 * "kaçış yolu" sağlamak amacıyla kullanılır.
 * <p>
 * Bu ek açıklama, bir metoda veya o metodu içeren tüm sınıfa uygulanabilir.
 * Eğer hem sınıfta hem de metotta kullanılırsa, metottaki ek açıklama sınıftakini geçersiz kılar.
 * <p>
 * <b>Örnek Kullanım:</b>
 * <pre>
 * {@code
 * // Bu komut normalde sadece bölge sahipleri tarafından kullanılabilir,
 * // ancak "myplugin.godmode.admin" yetkisine sahip olanlar bu kuralı
 * // görmezden gelebilir.
 * @RegionRole(RegionRoleType.OWNER)
 * @ShieldBypass("myplugin.godmode.admin")
 * public void activateGodMode(Player player) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ShieldBypass {

    /**
     * Oyuncunun, bu metot veya sınıf üzerindeki diğer tüm RegionShield
     * kısıtlamalarını atlaması için sahip olması gereken yetki (permission)
     * düğümünü belirtir.
     *
     * @return Bypass işlemini sağlayacak olan özel yetki.
     */
    String value();
}