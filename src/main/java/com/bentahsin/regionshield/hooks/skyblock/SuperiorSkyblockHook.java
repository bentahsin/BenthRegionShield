package com.bentahsin.regionshield.hooks.skyblock;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * SuperiorSkyblock2 eklentisi için bir entegrasyon (hook) sağlar.
 * <p>
 * Bu sınıf, SuperiorSkyblock2 API'sine karşı doğrudan bir derleme zamanı bağımlılığına (hard dependency) sahiptir.
 * Bu, projenin derlenmesi için SuperiorSkyblock2 eklentisinin bir bağımlılık olarak eklenmesini gerektirir.
 * Sınıf, SuperiorSkyblock2'nin kendi yetki (privilege) sistemini kullanarak izin kontrollerini gerçekleştirir.
 */
public class SuperiorSkyblockHook implements IShieldHook {

    /**
     * Hook'un benzersiz adını döndürür.
     *
     * @return "SuperiorSkyblock2" String'i.
     */
    @Override
    public String getName() {
        return "SuperiorSkyblock2";
    }

    /**
     * Bu hook'un başlatılıp başlatılamayacağını kontrol eder.
     * Sadece SuperiorSkyblock2 eklentisi sunucuda yüklü ve aktif ise başlatılabilir.
     *
     * @return SuperiorSkyblock2 aktif ise true, aksi takdirde false.
     */
    @Override
    public boolean canInitialize() {
        return ReflectionUtils.isPluginActive("SuperiorSkyblock2");
    }

    /**
     * Bir oyuncunun belirli bir konumdaki bir SuperiorSkyblock adasında eylem yapıp yapamayacağını kontrol eder.
     * <p>
     * Bu metot, öncelikle konumda bir ada olup olmadığını kontrol eder. Eğer bir ada varsa,
     * RegionShield'ın {@link InteractionType} enum'unu ilgili SuperiorSkyblock yetkisine ({@link IslandPrivilege})
     * çevirir ve adanın ayarlarını kullanarak oyuncunun bu eylem için izni olup olmadığını sorgular.
     *
     * @param player   Eylemi gerçekleştiren oyuncu.
     * @param location Eylemin gerçekleştiği konum.
     * @param type     Gerçekleştirilen etkileşim türü.
     * @return SuperiorSkyblock2 izin veriyorsa {@link ShieldResponse#allow()}, vermiyorsa {@link ShieldResponse#deny(String)}.
     *         Eğer konumda bir ada yoksa varsayılan olarak izin verilir.
     */
    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        Island island = SuperiorSkyblockAPI.getGrid().getIslandAt(location);
        if (island == null) return ShieldResponse.allow();

        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        IslandPrivilege privilege = getPrivilege(type);
        if (privilege == null) {
            privilege = IslandPrivilege.getByName("BUILD");
        }

        boolean allowed = island.hasPermission(superiorPlayer, privilege);
        return allowed ? ShieldResponse.allow() : ShieldResponse.deny(getName());
    }

    /**
     * RegionShield'ın {@link InteractionType} enum'unu ilgili SuperiorSkyblock {@link IslandPrivilege} nesnesine dönüştürür.
     * Bu metot, {@code InteractionType} değerini bir yetki adına (String) eşler ve ardından
     * SuperiorSkyblock'un {@code IslandPrivilege.getByName()} metodunu kullanarak bu isimdeki asıl yetki nesnesini arar.
     *
     * @param type Çevrilecek etkileşim türü.
     * @return Karşılık gelen {@code IslandPrivilege} nesnesi veya bulunamazsa {@code null}.
     *         Hata durumlarında "BUILD" yetkisine geri dönmeye çalışır.
     */
    private IslandPrivilege getPrivilege(InteractionType type) {
        String privilegeName = switch (type) {
            case BLOCK_BREAK -> "BREAK";
            case BLOCK_PLACE -> "BUILD";
            case INTERACT -> "INTERACT";
            case CONTAINER_ACCESS -> "CHESTS";
            case PVP -> "PVP";
            case MOB_DAMAGE -> "FARMING";
            case BUCKET_USE -> "BUCKETS";
            case TRAMPLE -> "CROP_TRAMPLE";
            default -> "BUILD";
        };

        try {
            return IslandPrivilege.getByName(privilegeName);
        } catch (Exception e) {
            try {
                return IslandPrivilege.getByName("BUILD");
            } catch (Exception ex) {
                return null;
            }
        }
    }
}