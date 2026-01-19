package com.bentahsin.regionshield.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bir metodu veya bir sınıfın tamamını, oyuncunun mevcut bölgedeki rolüne göre
 * kısıtlamak için işaretler.
 * <p>
 * Bu, yalnızca bölge sahiplerinin veya üyelerinin kullanabileceği komutlar gibi,
 * role dayalı özellikler oluşturmak için kullanılır. Bu denetimin çalışması için,
 * oyuncunun bir bölgenin içinde olması gerekir; vahşi doğada (wilderness) olan
 * bir oyuncu bu denetimi geçemez.
 * <p>
 * Bu ek açıklama, bir metoda veya o metodu içeren tüm sınıfa uygulanabilir.
 * Eğer hem sınıfta hem de metotta kullanılırsa, metottaki ek açıklama sınıftakini geçersiz kılar.
 * <p>
 * <b>Örnek Kullanım:</b>
 * <pre>
 * {@code
 * // Bu komutun sadece bölge sahipleri tarafından kullanılmasını sağla.
 * @RegionRole(RegionRoleType.OWNER)
 * public void renameRegionCommand(Player player) {
 *     // Bölgeyi yeniden adlandırma mantığı...
 * }
 *
 * // Bu komutun bölge sahipleri veya üyeleri tarafından kullanılmasını sağla (varsayılan davranış).
 * @RegionRole
 * public void openRegionStorage(Player player) {
 *     // Bölge deposunu aç...
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RegionRole {

    /**
     * İşlemi gerçekleştirmek için oyuncunun sahip olması gereken minimum rolü belirtir.
     *
     * @return Gerekli olan {@link RegionRoleType}. Varsayılan olarak
     *         {@link RegionRoleType#MEMBER_OR_OWNER}, yani oyuncunun en azından
     *         üye olması gerekir.
     */
    RegionRoleType value() default RegionRoleType.MEMBER_OR_OWNER;
}