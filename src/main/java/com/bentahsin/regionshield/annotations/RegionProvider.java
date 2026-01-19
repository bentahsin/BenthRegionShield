package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bir metodu veya bir sınıfın tamamını, yalnızca oyuncu belirli bir sağlayıcı (hook)
 * tarafından yönetilen bir bölgenin içindeyken çalışacak şekilde işaretler.
 * <p>
 * Örneğin, {@code @RegionProvider("Towny")} ek açıklaması, oyuncu herhangi bir Towny
 * kasabası veya arsası içindeyse metodun çalışmasına izin verir, ancak WorldGuard
 * bölgesi veya vahşi doğa (wilderness) gibi başka bir alandaysa engeller.
 * <p>
 * Bu, belirli eklenti ekosistemleriyle entegre olan özellikler oluşturmak için kullanışlıdır.
 * Bu ek açıklama, bir metoda veya o metodu içeren tüm sınıfa uygulanabilir.
 * Eğer hem sınıfta hem de metotta kullanılırsa, metottaki ek açıklama sınıftakini geçersiz kılar.
 * <p>
 * <b>Örnek Kullanım:</b>
 * <pre>
 * {@code
 * // Bu komutun sadece oyuncu bir Towny kasabası içindeyken çalışmasını sağla.
 * @RegionProvider("Towny")
 * public void showTownInfo(Player player) {
 *     // Kasaba bilgilerini göster...
 * }
 *
 * // Bu komutun sadece WorldGuard tarafından yönetilen bir bölgede çalışmasını sağla.
 * @RegionProvider("WorldGuard")
 * public void checkRegionFlags(Player player) {
 *     // Bölge bayraklarını kontrol et...
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RegionProvider {

    /**
     * İşlemin gerçekleştirilmesi için oyuncunun içinde bulunması gereken bölgenin
     * sağlayıcısının (hook'un) adını belirtir.
     * <p>
     * Bu ad, {@code IShieldHook.getName()} metodu tarafından döndürülen değerle
     * (büyük/küçük harf duyarsız) eşleşmelidir.
     *
     * @return Gerekli olan bölge sağlayıcısının adı (örn: "Towny", "WorldGuard").
     */
    String value();
}