package com.bentahsin.regionshield.hooks.skyblock;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Optional;

/**
 * BentoBox eklentisi ve onun oyun modları (örn: BSkyBlock, AcidIsland) için bir entegrasyon (hook) sağlar.
 * <p>
 * Bu sınıf, BentoBox API'sine karşı doğrudan bir derleme zamanı bağımlılığına (hard dependency) sahiptir.
 * Bu, projenin derlenmesi için BentoBox eklentisinin bir bağımlılık olarak eklenmesini gerektirir.
 * Sınıf, BentoBox'ın kendi bayrak (flag) sistemini kullanarak izin kontrollerini gerçekleştirir.
 */
public class BentoBoxHook implements IShieldHook {

    /**
     * Hook'un benzersiz adını döndürür.
     *
     * @return "BentoBox" String'i.
     */
    @Override
    public String getName() {
        return "BentoBox";
    }

    /**
     * Bu hook'un başlatılıp başlatılamayacağını kontrol eder.
     * Sadece BentoBox eklentisi sunucuda yüklü ve aktif ise başlatılabilir.
     *
     * @return BentoBox aktif ise true, aksi takdirde false.
     */
    @Override
    public boolean canInitialize() {
        return ReflectionUtils.isPluginActive("BentoBox");
    }

    /**
     * Bir oyuncunun belirli bir konumdaki bir BentoBox adasında eylem yapıp yapamayacağını kontrol eder.
     * <p>
     * Bu metot, öncelikle konumda bir ada olup olmadığını kontrol eder. Eğer bir ada varsa,
     * RegionShield'ın {@link InteractionType} enum'unu ilgili BentoBox bayrağına ({@link Flag}) çevirir
     * ve adanın ayarlarını kullanarak oyuncunun bu eylem için izni olup olmadığını sorgular.
     *
     * @param player   Eylemi gerçekleştiren oyuncu.
     * @param location Eylemin gerçekleştiği konum.
     * @param type     Gerçekleştirilen etkileşim türü.
     * @return BentoBox izin veriyorsa {@link ShieldResponse#allow()}, vermiyorsa {@link ShieldResponse#deny(String)}.
     *         Eğer konumda bir ada yoksa veya bir hata oluşursa varsayılan olarak izin verilir.
     */
    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        Optional<Island> islandOpt = BentoBox.getInstance().getIslands().getIslandAt(location);
        if (islandOpt.isEmpty()) return ShieldResponse.allow();

        Island island = islandOpt.get();
        Flag flag = getBentoFlag(type);

        if (flag == null) {
            flag = BentoBox.getInstance().getFlagsManager().getFlag("PLACE").orElse(null);
        }

        if (flag == null) return ShieldResponse.allow();

        User user = User.getInstance(player);
        boolean allowed = island.isAllowed(user, flag);

        return allowed ? ShieldResponse.allow() : ShieldResponse.deny(getName());
    }

    /**
     * RegionShield'ın {@link InteractionType} enum'unu ilgili BentoBox {@link Flag} nesnesine dönüştürür.
     * Bu metot, {@code InteractionType} değerini bir bayrak adına (String) eşler ve ardından
     * BentoBox'ın {@code FlagsManager}'ından bu isimdeki asıl {@code Flag} nesnesini arar.
     *
     * @param type Çevrilecek etkileşim türü.
     * @return Karşılık gelen {@code Flag} nesnesi veya bulunamazsa {@code null}.
     */
    private Flag getBentoFlag(InteractionType type) {
        String flagName = switch (type) {
            case BLOCK_BREAK -> "BREAK";
            case BLOCK_PLACE -> "PLACE";
            case CONTAINER_ACCESS -> "CONTAINER";
            case INTERACT -> "DOOR";
            case PVP -> "PVP";
            case MOB_DAMAGE -> "HURT_ANIMALS";
            case TRAMPLE -> "PLACE";
            case BUCKET_USE -> "BUCKET";
            default -> "PLACE";
        };

        return BentoBox.getInstance().getFlagsManager().getFlag(flagName).orElse(null);
    }
}