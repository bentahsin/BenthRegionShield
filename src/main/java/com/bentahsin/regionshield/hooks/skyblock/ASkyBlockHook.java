package com.bentahsin.regionshield.hooks.skyblock;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * ASkyBlock eklentisi için "güvenli" bir entegrasyon (hook) sağlar.
 * <p>
 * Bu sınıf, ASkyBlock API'sine karşı doğrudan bir derleme zamanı bağımlılığı (hard dependency) oluşturmaz.
 * Bunun yerine, Java Reflection (Yansıtma) kullanarak ASkyBlock'un sınıflarına ve metotlarına
 * çalışma zamanında (runtime) erişir. Bu, sunucuda ASkyBlock eklentisi yüklü olmasa bile
 * bu kodun hata vermesini ve sunucuyu çökertmesini engeller.
 * <p>
 * Tüm yansıtma işlemleri {@link #canInitialize()} metodunda bir kez yapılır ve bulunan metotlar
 * performans için sınıf alanlarında önbelleğe alınır.
 */
public class ASkyBlockHook implements IShieldHook {

    private Object apiInstance;
    private Method getIslandAtMethod;
    private Method getOwnerMethod;
    private Method getMembersMethod;

    private boolean initialized = false;

    /**
     * Hook'un benzersiz adını döndürür.
     *
     * @return "ASkyBlock" String'i.
     */
    @Override
    public String getName() {
        return "ASkyBlock";
    }

    /**
     * Bu hook'un başlatılıp başlatılamayacağını kontrol eder.
     * <p>
     * İlk olarak ASkyBlock eklentisinin sunucuda aktif olup olmadığını kontrol eder.
     * Ardından, yansıtma kullanarak gerekli tüm ASkyBlock sınıflarını ve metotlarını bulmaya çalışır.
     * Tüm gerekli bileşenler başarıyla bulunursa, bunları gelecekteki hızlı kullanım için
     * sınıf alanlarında önbelleğe alır ve {@code true} döndürür. Aksi takdirde {@code false} döndürür.
     *
     * @return Hook başarıyla başlatıldıysa true, aksi takdirde false.
     */
    @Override
    public boolean canInitialize() {
        if (!ReflectionUtils.isPluginActive("ASkyBlock")) return false;

        try {
            Class<?> apiClass = ReflectionUtils.getClass("com.wasteofplastic.askyblock.ASkyBlockAPI");
            Method getInstance = ReflectionUtils.getMethod(apiClass, "getInstance");
            this.apiInstance = ReflectionUtils.invoke(getInstance, null);

            this.getIslandAtMethod = ReflectionUtils.getMethod(apiClass, "getIslandAt", Location.class);

            Class<?> islandClass = ReflectionUtils.getClass("com.wasteofplastic.askyblock.Island");
            this.getOwnerMethod = ReflectionUtils.getMethod(islandClass, "getOwner");
            this.getMembersMethod = ReflectionUtils.getMethod(islandClass, "getMembers");

            this.initialized = apiInstance != null && getIslandAtMethod != null && getOwnerMethod != null && getMembersMethod != null;
            return this.initialized;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Bir oyuncunun belirli bir konumdaki bir adada eylem yapıp yapamayacağını kontrol eder.
     * ASkyBlock'un kendi bayrak (flag) sistemi olmadığı için, bu kontrol basitçe oyuncunun
     * adanın sahibi veya üyesi olup olmadığını kontrol etmeye dayanır.
     *
     * @param player   Eylemi gerçekleştiren oyuncu.
     * @param location Eylemin gerçekleştiği konum.
     * @param type     Gerçekleştirilen etkileşim türü (bu hook tarafından dikkate alınmaz).
     * @return Oyuncu adanın sahibi veya üyesi ise {@link ShieldResponse#allow()}, değilse {@link ShieldResponse#deny(String)}.
     *         Eğer konumda bir ada yoksa, hook başlatılmamışsa veya bir hata oluşursa varsayılan olarak izin verilir.
     */
    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        if (!initialized) return ShieldResponse.allow();

        try {
            Object island = ReflectionUtils.invoke(getIslandAtMethod, apiInstance, location);
            if (island == null) return ShieldResponse.allow();

            UUID playerUUID = player.getUniqueId();
            UUID ownerUUID = (UUID) ReflectionUtils.invoke(getOwnerMethod, island);

            if (ownerUUID != null && ownerUUID.equals(playerUUID)) {
                return ShieldResponse.allow();
            }

            java.util.Set<?> members = (java.util.Set<?>) ReflectionUtils.invoke(getMembersMethod, island);
            if (members != null && members.contains(playerUUID)) {
                return ShieldResponse.allow();
            }

            return ShieldResponse.deny(getName());

        } catch (Exception e) {
            return ShieldResponse.allow();
        }
    }
}