package com.bentahsin.regionshield.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Bir {@link IShieldHook} tarafından gerçekleştirilen bir izin kontrolünün sonucunu temsil eder.
 * <p>
 * Bu sınıf, bir eyleme izin verilip verilmediğini ({@code allowed}) ve bu kararı hangi
 * sağlayıcının ({@code providerName}) verdiğini içeren, değişmez (immutable) bir veri nesnesidir.
 * <p>
 * Bu sınıfın örnekleri doğrudan {@code new ShieldResponse(...)} ile oluşturulamaz.
 * Bunun yerine, kontrollü nesne oluşturmayı sağlamak ve performansı artırmak için
 * statik fabrika metotları olan {@link #allow()} ve {@link #deny(String)} kullanılmalıdır.
 */
@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ShieldResponse {

    /**
     * İzin kontrolünün sonucunu belirtir. {@code true} ise izin verilmiş, {@code false} ise reddedilmiştir.
     */
    private final boolean allowed;

    /**
     * Kararı veren hook'un veya sağlayıcının adını belirtir.
     * Genellikle sadece eylem reddedildiğinde anlamlıdır.
     */
    private final String providerName;

    /**
     * "İzin verildi" durumları için kullanılan, önceden oluşturulmuş, paylaşılan tekil örnek.
     * Bu, en yaygın durum olan izin verildiğinde sürekli yeni nesne oluşturulmasını önleyerek performansı artırır.
     */
    private static final ShieldResponse ALLOW_DEFAULT = new ShieldResponse(true, "None");

    /**
     * Bir eyleme izin verildiğini belirten, paylaşılan bir ShieldResponse örneği döndürür.
     * Bu metot, performans için her zaman aynı önbelleğe alınmış nesneyi döndürür.
     *
     * @return İzin verildiğini gösteren standart response nesnesi.
     */
    public static ShieldResponse allow() {
        return ALLOW_DEFAULT;
    }

    /**
     * Bir eylemin reddedildiğini belirten yeni bir ShieldResponse örneği oluşturur ve döndürür.
     *
     * @param provider Eylemi reddeden hook'un adı (örn: "WorldGuard"). Bu bilgi,
     *                 hata ayıklama ve oyuncuya geri bildirim verme açısından önemlidir.
     * @return Eylemin reddedildiğini ve reddeden sağlayıcıyı içeren yeni bir response nesnesi.
     */
    public static ShieldResponse deny(String provider) {
        return new ShieldResponse(false, provider);
    }

    /**
     * Bu eylemin reddedilip reddedilmediğini kontrol eden bir yardımcı metottur.
     * Bu, {@code !isAllowed()} ifadesinin daha okunabilir bir alternatifidir.
     *
     * @return Eylem reddedilmişse {@code true}, izin verilmişse {@code false}.
     */
    public boolean isDenied() {
        return !allowed;
    }
}