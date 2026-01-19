package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bir metodu veya bir sınıfın tamamını, belirtilen bölge ID'lerine sahip alanlarda
 * otomatik olarak başarısız olacak şekilde işaretler. Bu, bir "kara liste" (blacklist)
 * mantığıyla çalışır: Oyuncu, ID'si listede olan bir bölgedeyse, işlem engellenir.
 * <p>
 * Bu ek açıklama, bir metoda veya o metodu içeren tüm sınıfa uygulanabilir.
 * Eğer hem sınıfta hem de metotta kullanılırsa, metottaki ek açıklama sınıftakini geçersiz kılar.
 * <p>
 * <b>Örnek Kullanım:</b>
 * <pre>
 * {@code
 * // "arena1" ve "mob_arena" ID'li tüm bölgelerde bu metodu engelle.
 * @RegionBlacklist(ids = {"arena1", "mob_arena"})
 * public void placeSpecialBlock(Player player) {
 *     // ...
 * }
 *
 * // Sadece WorldGuard'a ait "event" bölgesinde engelle.
 * @RegionBlacklist(ids = "event", provider = "WorldGuard")
 * public void startEvent(Player player) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RegionBlacklist {

    /**
     * İşlemin engelleneceği bölge ID'lerinin bir dizisini belirtir.
     * Bu ID'ler, hook'ların {@code RegionInfo.getId()} metoduyla döndürdüğü
     * değerlerle karşılaştırılır ve büyük/küçük harfe duyarlıdır.
     *
     * @return Kara listeye alınmış bölge ID'lerinin dizisi.
     */
    String[] ids();

    /**
     * Kara listenin uygulanacağı belirli bir bölge sağlayıcısını (hook adını) tanımlar.
     * <p>
     * Eğer bu alan boş bırakılırsa (varsayılan), kara liste, oyuncunun bulunduğu bölgenin
     * sağlayıcısına bakılmaksızın, {@link #ids()} dizisindeki herhangi bir ID ile
     * eşleştiğinde tetiklenir.
     * <p>
     * Bu, belirli bir eklentiye (örn: "WorldGuard") ait "arena" bölgesini engellemek,
     * ancak başka bir eklentiye ait "arena" bölgesini engellememek için kullanışlıdır.
     *
     * @return Kara listenin geçerli olacağı sağlayıcının adı veya tüm sağlayıcılar için boş bir string.
     */
    String provider() default "";
}