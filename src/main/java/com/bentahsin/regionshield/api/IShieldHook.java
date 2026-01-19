package com.bentahsin.regionshield.api;

import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * BenthRegionShield API'si ile diğer bölge koruma eklentileri arasında bir köprü (entegrasyon)
 * oluşturmak için gereken sözleşmeyi (contract) tanımlayan arayüz.
 * <p>
 * Yeni bir koruma eklentisini RegionShield'a entegre etmek isteyen her geliştirici,
 * bu arayüzü uygulayan bir sınıf oluşturmalıdır. Bu sınıf, RegionShield'ın genel sorgularını
 * hedef eklentinin kendi API çağrılarına çevirmekle sorumludur.
 */
public interface IShieldHook {

    /**
     * Hook'un (entegrasyonun) benzersiz ve insan tarafından okunabilir adını döndürür.
     * Bu ad, genellikle entegre edilen eklentinin adıdır ve loglarda, mesajlarda
     * ve hata ayıklama bilgilerinde kullanılır.
     *
     * @return Eklenti adı (Örn: "WorldGuard", "Towny", "Lands").
     */
    String getName();

    /**
     * Bu hook'un sunucuda başlatılıp başlatılamayacağını kontrol eder.
     * Bu metot, RegionShield tarafından hook kaydedilirken yalnızca bir kez çağrılır.
     * <p>
     * Tipik olarak aşağıdaki kontrolleri içermelidir:
     * <ul>
     *     <li>Entegre edilen eklenti sunucuda yüklü ve aktif mi?</li>
     *     <li>Eğer kullanılıyorsa, eklentinin API sürümü uyumlu mu?</li>
     *     <li>Yansıtma (reflection) tabanlı hook'lar için gerekli sınıflar ve metotlar bulunabiliyor mu?</li>
     * </ul>
     *
     * @return Hook'un çalışması için tüm koşullar sağlanıyorsa {@code true}, aksi takdirde {@code false}.
     */
    boolean canInitialize();

    /**
     * Hook'un ana izin kontrol metodu. Bu metot, bir oyuncunun belirli bir konumda,
     * belirli bir eylemi gerçekleştirip gerçekleştiremeyeceğini sorgular.
     * <p>
     * Uygulama, verilen {@link InteractionType} değerini hedef eklentinin kendi izin
     * sistemine (bayraklar, yetkiler vb.) çevirmeli ve sonucu bir {@link ShieldResponse}
     * olarak döndürmelidir. Bir hata durumunda, oyuncuları yanlışlıkla engellememek için
     * güvenli bir şekilde {@code ShieldResponse.allow()} döndürmesi önerilir.
     *
     * @param player   İşlemi yapan oyuncu.
     * @param location İşlemin yapıldığı konum.
     * @param type     Gerçekleştirilen etkileşimin türü.
     * @return Eyleme izin veriliyorsa {@code ShieldResponse.allow()}, reddediliyorsa {@code ShieldResponse.deny()}.
     */
    ShieldResponse check(Player player, Location location, InteractionType type);

    /**
     * Hook'un çalışma önceliğini belirtir. RegionShield, izinleri kontrol ederken
     * hook'ları en yüksek öncelikten en düşüğe doğru sırayla sorgular.
     * Reddeden ilk hook, sonucu belirler.
     * <p>
     * WorldGuard gibi temel koruma eklentileri genellikle en yüksek önceliğe sahip olmalıdır.
     *
     * @return Hook'un öncelik seviyesi. Varsayılan olarak {@link ShieldPriority#NORMAL}.
     */
    default ShieldPriority getPriority() {
        return ShieldPriority.NORMAL;
    }

    /**
     * Verilen konumdaki bölge hakkında (varsa) detaylı bilgi döndürür.
     * Bu, bölgenin adı, sahibi ve üyeleri gibi bilgileri içeren isteğe bağlı bir özelliktir.
     *
     * @param location Bilgi alınacak konum.
     * @return Konumda bir bölge varsa bir {@link RegionInfo} nesnesi; aksi takdirde veya
     *         bu özellik desteklenmiyorsa {@code null}.
     */
    default RegionInfo getRegionInfo(Location location) {
        return null;
    }

    /**
     * Verilen konumdaki bölgenin (varsa) fiziksel sınırlarını döndürür.
     * Bu, bölge sınırlarını görselleştirmek gibi özellikler için kullanılan isteğe bağlı bir metottur.
     *
     * @param location Sınırları alınacak bölgenin içindeki bir konum.
     * @return Bölgenin sınırlarını temsil eden bir {@link RegionBounds} nesnesi; aksi takdirde veya
     *         bu özellik desteklenmiyorsa {@code null}.
     */
    default RegionBounds getRegionBounds(Location location) {
        return null;
    }
}