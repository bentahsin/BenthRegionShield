package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bir metodu veya bir sınıfın tamamını, yalnızca oyuncu ID'si belirtilen belirli bir
 * bölgenin içindeyken çalışacak şekilde işaretler.
 * <p>
 * Bu, bir "beyaz liste" (whitelist) mantığıyla çalışır: Oyuncu, tam olarak
 * belirtilen bölgede değilse, işlem engellenir. Bu, belirli komutların veya
 * eylemlerin yalnızca belirli alanlarda (örneğin bir spawn bölgesi veya pazar alanı)
 * kullanılabilmesini sağlamak için idealdir.
 * <p>
 * Bu ek açıklama, bir metoda veya o metodu içeren tüm sınıfa uygulanabilir.
 * Eğer hem sınıfta hem de metotta kullanılırsa, metottaki ek açıklama sınıftakini geçersiz kılar.
 * <p>
 * <b>Örnek Kullanım:</b>
 * <pre>
 * {@code
 * // Bu komutun sadece "spawn" bölgesinde çalışmasını sağla.
 * @RegionLimit(id = "spawn")
 * public void claimDailyReward(Player player) {
 *     // Oyuncuya günlük ödülünü ver...
 * }
 *
 * // Sadece WorldGuard'a ait "market" bölgesinde çalışsın.
 * @RegionLimit(id = "market", provider = "WorldGuard")
 * public void openSpecialShop(Player player) {
 *     // Oyuncuya özel dükkanı aç...
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RegionLimit {

    /**
     * İşlemin gerçekleştirilmesi için oyuncunun içinde bulunması gereken bölgenin
     * benzersiz ID'sini belirtir (örn: "spawn", "market", "arena").
     * <p>
     * Bu ID, hook'ların {@code RegionInfo.getId()} metoduyla döndürdüğü değerle
     * tam olarak eşleşmelidir. Regex desteklemez; tam eşleşme aranır.
     *
     * @return Gerekli olan bölgenin ID'si.
     */
    String id();

    /**
     * Bölge ID'sinin aranacağı belirli bir bölge sağlayıcısını (hook adını) tanımlar.
     * <p>
     * Eğer bu alan boş bırakılırsa (varsayılan), RegionShield, oyuncunun bulunduğu bölgenin
     * sağlayıcısına bakılmaksızın, ID'nin eşleşip eşleşmediğini kontrol eder.
     * Bu, aynı ID'ye sahip farklı eklentilerden gelen bölgeleri ayırt etmek için
     * kullanışlıdır (örn: WorldGuard'daki "arena" ve başka bir eklentideki "arena").
     *
     * @return Bölgenin aranacağı sağlayıcının adı (örn: "WorldGuard") veya tüm sağlayıcılar
     *         için boş bir string.
     */
    String provider() default "";
}