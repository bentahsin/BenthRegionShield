package com.bentahsin.regionshield.hooks.lands;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import com.bentahsin.regionshield.model.RegionInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import me.angeschossen.lands.api.flags.Flags;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.integration.LandsIntegration;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.land.LandWorld;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Lands eklentisi için bir entegrasyon (hook) sağlar.
 * <p>
 * Bu sınıf, Lands API'sine karşı doğrudan bir derleme zamanı bağımlılığına (hard dependency) sahiptir.
 * Bu, projenin derlenmesi için Lands eklentisinin bir bağımlılık olarak eklenmesini gerektirir.
 * Sınıf, Lands'ın kendi bayrak (flag) sistemini kullanarak izin kontrollerini gerçekleştirir.
 * Lands API'sinin başlatılması için ana eklentinin bir örneğine (instance) ihtiyaç duyar.
 */
@SuppressWarnings("deprecation")
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class LandsHook implements IShieldHook {

    private final Plugin plugin;
    private LandsIntegration landsIntegration;

    /**
     * Yeni bir LandsHook örneği oluşturur.
     *
     * @param plugin Lands API'sini (`LandsIntegration`) başlatmak için gereken ana eklenti örneği.
     */
    public LandsHook(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Hook'un benzersiz adını döndürür.
     *
     * @return "Lands" String'i.
     */
    @Override
    public String getName() {
        return "Lands";
    }

    /**
     * Bu hook'un başlatılıp başlatılamayacağını kontrol eder.
     * Lands eklentisinin sunucuda aktif olup olmadığını kontrol eder ve ardından
     * {@link LandsIntegration} nesnesini başarıyla başlatmaya çalışır.
     *
     * @return Lands API'si başarıyla başlatılırsa true, aksi takdirde false.
     */
    @Override
    public boolean canInitialize() {
        if (!ReflectionUtils.isPluginActive("Lands")) return false;

        try {
            this.landsIntegration = new LandsIntegration(plugin);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Bir oyuncunun belirli bir konumda bir eylemi gerçekleştirip gerçekleştiremeyeceğini Lands API'sini kullanarak kontrol eder.
     *
     * @param player   Eylemi gerçekleştiren oyuncu.
     * @param location Eylemin gerçekleştiği konum.
     * @param type     Gerçekleştirilen etkileşim türü.
     * @return Lands izin veriyorsa {@link ShieldResponse#allow()}, vermiyorsa {@link ShieldResponse#deny(String)}.
     */
    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        if (landsIntegration == null) return ShieldResponse.allow();

        if (location == null || location.getWorld() == null) return ShieldResponse.allow();

        LandWorld landWorld = landsIntegration.getLandWorld(location.getWorld());
        if (landWorld == null) return ShieldResponse.allow();

        RoleFlag flag = getRoleFlag(type);
        if (flag == null) return ShieldResponse.allow();

        boolean allowed = landWorld.hasRoleFlag(player.getUniqueId(), location, flag);
        return allowed ? ShieldResponse.allow() : ShieldResponse.deny(getName());
    }

    /**
     * Belirtilen konumdaki Land hakkında bilgi alır ve bunu standart {@link RegionInfo} modeline dönüştürür.
     *
     * @param location Bilgi alınacak konum.
     * @return Konumda bir Land varsa bir {@link RegionInfo} nesnesi; aksi takdirde null.
     */
    @Override
    public RegionInfo getRegionInfo(Location location) {
        if (landsIntegration == null || location == null) return null;

        Land land = landsIntegration.getLand(location);

        if (land == null) return null;

        return RegionInfo.builder()
                .id(land.getName())
                .provider(getName())
                .owners(Collections.singletonList(land.getOwnerUID()))
                .members(new ArrayList<>(land.getTrustedPlayers()))
                .build();
    }

    /**
     * Belirtilen konumdaki Land'in sınırlarını alır.
     * Lands chunk tabanlı çalıştığı için, bu metot Land'in bulunduğu chunk'ın sınırlarını döndürür.
     *
     * @param location Sınırları alınacak bölgenin içindeki bir konum.
     * @return Chunk'ın sınırlarını içeren bir {@link RegionBounds} nesnesi veya Land bulunamazsa null.
     */
    @Override
    public RegionBounds getRegionBounds(Location location) {
        if (landsIntegration == null || location == null) return null;

        Land land = landsIntegration.getLand(location);
        if (land == null) return null;

        org.bukkit.Chunk chunk = location.getChunk();
        World world = location.getWorld();
        if (world == null) return null;

        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        int minY = 0;
        int maxY = world.getMaxHeight();

        try {
            minY = world.getMinHeight();
        } catch (NoSuchMethodError ignored) {}

        Location min = new Location(world, minX, minY, minZ);
        Location max = new Location(world, maxX, maxY, maxZ);

        return new RegionBounds(min, max);
    }

    /**
     * RegionShield'ın {@link InteractionType} enum'unu ilgili Lands {@link RoleFlag} nesnesine çevirir.
     *
     * @param type Çevrilecek etkileşim türü.
     * @return Karşılık gelen {@code RoleFlag} nesnesi.
     */
    private RoleFlag getRoleFlag(InteractionType type) {
        switch (type) {
            case BLOCK_BREAK:
                return Flags.BLOCK_BREAK;
            case BLOCK_PLACE:
                return Flags.BLOCK_PLACE;
            case INTERACT:
                return Flags.INTERACT_GENERAL;
            case CONTAINER_ACCESS:
                return Flags.INTERACT_CONTAINER;
            case PVP:
                return Flags.ATTACK_PLAYER;
            case MOB_DAMAGE:
                return Flags.ATTACK_ANIMAL;
            case TRAMPLE:
                return Flags.TRAMPLE_FARMLAND;
            case DAMAGE_ENTITY:
                return Flags.ATTACK_MONSTER;
            case BUCKET_USE:
                return Flags.BLOCK_PLACE;
            default:
                return Flags.BLOCK_PLACE;
        }
    }
}