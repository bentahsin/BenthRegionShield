package com.bentahsin.regionshield.hooks.towny;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Towny eklentisi için "güvenli" bir entegrasyon (hook) sağlar.
 * <p>
 * "Güvenli" olmasının sebebi, bu sınıfın Towny API'sine karşı doğrudan bir derleme zamanı
 * bağımlılığı (hard dependency) olmamasıdır. Bunun yerine, Java Reflection (Yansıtma) kullanarak
 * Towny'nin sınıflarına ve metotlarına çalışma zamanında (runtime) erişir.
 * Bu yaklaşım, sunucuda Towny eklentisi yüklü olmasa bile bu kodun bir {@link ClassNotFoundException} hatası
 * fırlatmasını ve sunucuyu çökertmesini engeller.
 * <p>
 * Tüm yansıtma işlemleri {@link #canInitialize()} metodunda bir kez yapılır ve sonuçlar
 * performans için sınıf alanlarında önbelleğe alınır.
 */
public class TownySafeHook implements IShieldHook {

    private Method getCachePermissionMethod;
    private Object actionBuild, actionDestroy, actionSwitch, actionItemUse;

    private Object townyAPIInstance;
    private Method getTownBlockMethod;
    private Method hasTownMethod;
    private Method getTownMethod;

    private Method getTownNameMethod;
    private Method getMayorMethod;
    private Method getResidentsMethod;

    private Method getResidentUUIDMethod;

    private boolean initialized = false;

    /**
     * Hook'un benzersiz adını döndürür.
     *
     * @return "Towny" String'i.
     */
    @Override
    public String getName() {
        return "Towny";
    }

    /**
     * Bu hook'un başlatılıp başlatılamayacağını kontrol eder.
     * <p>
     * İlk olarak Towny eklentisinin sunucuda aktif olup olmadığını kontrol eder.
     * Ardından, yansıtma kullanarak gerekli tüm Towny sınıflarını ve metotlarını bulmaya çalışır.
     * Tüm gerekli bileşenler başarıyla bulunursa, bunları gelecekteki hızlı kullanım için
     * sınıf alanlarında önbelleğe alır ve {@code true} döndürür. Aksi takdirde {@code false} döndürür.
     *
     * @return Hook başarıyla başlatıldıysa true, aksi takdirde false.
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean canInitialize() {
        if (!ReflectionUtils.isPluginActive("Towny")) return false;

        try {
            Class<?> cacheUtilClass = ReflectionUtils.getClass("com.palmergames.bukkit.towny.utils.PlayerCacheUtil");
            Class<?> actionTypeClass = ReflectionUtils.getClass("com.palmergames.bukkit.towny.object.TownyPermission$ActionType");

            if (cacheUtilClass != null && actionTypeClass != null) {
                this.getCachePermissionMethod = ReflectionUtils.getMethod(
                        cacheUtilClass, "getCachePermission",
                        Player.class, Location.class, Material.class, actionTypeClass
                );

                this.actionBuild = Enum.valueOf((Class<Enum>) actionTypeClass, "BUILD");
                this.actionDestroy = Enum.valueOf((Class<Enum>) actionTypeClass, "DESTROY");
                this.actionSwitch = Enum.valueOf((Class<Enum>) actionTypeClass, "SWITCH");
                this.actionItemUse = Enum.valueOf((Class<Enum>) actionTypeClass, "ITEM_USE");
            }

            Class<?> townyApiClass = ReflectionUtils.getClass("com.palmergames.bukkit.towny.TownyAPI");
            Class<?> townBlockClass = ReflectionUtils.getClass("com.palmergames.bukkit.towny.object.TownBlock");
            Class<?> townClass = ReflectionUtils.getClass("com.palmergames.bukkit.towny.object.Town");
            Class<?> residentClass = ReflectionUtils.getClass("com.palmergames.bukkit.towny.object.Resident");

            if (townyApiClass != null) {
                Method getInstance = ReflectionUtils.getMethod(townyApiClass, "getInstance");
                this.townyAPIInstance = ReflectionUtils.invoke(getInstance, null);

                this.getTownBlockMethod = ReflectionUtils.getMethod(townyApiClass, "getTownBlock", Location.class);

                this.hasTownMethod = ReflectionUtils.getMethod(townBlockClass, "hasTown");
                this.getTownMethod = ReflectionUtils.getMethod(townBlockClass, "getTown");

                this.getTownNameMethod = ReflectionUtils.getMethod(townClass, "getName");
                this.getMayorMethod = ReflectionUtils.getMethod(townClass, "getMayor");
                this.getResidentsMethod = ReflectionUtils.getMethod(townClass, "getResidents");

                this.getResidentUUIDMethod = ReflectionUtils.getMethod(residentClass, "getUUID");
            }

            this.initialized = getCachePermissionMethod != null && townyAPIInstance != null;
            return this.initialized;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Bir oyuncunun belirli bir konumda bir eylemi gerçekleştirip gerçekleştiremeyeceğini Towny'ye sorar.
     * Bu işlemi, önbelleğe alınmış yansıtma metotlarını kullanarak yapar.
     *
     * @param player   Eylemi gerçekleştiren oyuncu.
     * @param location Eylemin gerçekleştiği konum.
     * @param type     Gerçekleştirilen etkileşim türü.
     * @return Towny izin veriyorsa {@link ShieldResponse#allow()}, vermiyorsa {@link ShieldResponse#deny(String)}.
     *         Hook başlatılmamışsa veya bir hata oluşursa varsayılan olarak izin verilir.
     */
    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        if (!initialized) return ShieldResponse.allow();

        try {
            Object townyAction = getTownyAction(type);
            boolean hasPermission = (boolean) ReflectionUtils.invoke(
                    getCachePermissionMethod,
                    null,
                    player, location, location.getBlock().getType(), townyAction
            );

            return hasPermission ? ShieldResponse.allow() : ShieldResponse.deny(getName());

        } catch (Exception e) {
            return ShieldResponse.allow();
        }
    }

    /**
     * Belirtilen konumdaki Towny kasabası hakkında bilgi alır.
     *
     * @param location Bilgi alınacak konum.
     * @return Konumda bir kasaba varsa bir {@link RegionInfo} nesnesi; aksi takdirde veya bir hata oluşursa null.
     */
    @Override
    public RegionInfo getRegionInfo(Location location) {
        if (!initialized) return null;

        try {
            Object townBlock = ReflectionUtils.invoke(getTownBlockMethod, townyAPIInstance, location);
            if (townBlock == null) return null;

            boolean hasTown = (boolean) ReflectionUtils.invoke(hasTownMethod, townBlock);
            if (!hasTown) return null;

            Object town = ReflectionUtils.invoke(getTownMethod, townBlock);
            if (town == null) return null;

            String townName = (String) ReflectionUtils.invoke(getTownNameMethod, town);

            List<UUID> owners = new ArrayList<>();
            Object mayor = ReflectionUtils.invoke(getMayorMethod, town);
            if (mayor != null && getResidentUUIDMethod != null) {
                owners.add((UUID) ReflectionUtils.invoke(getResidentUUIDMethod, mayor));
            }

            List<UUID> members = new ArrayList<>();
            List<?> residentsList = (List<?>) ReflectionUtils.invoke(getResidentsMethod, town);
            if (residentsList != null && getResidentUUIDMethod != null) {
                for (Object resident : residentsList) {
                    members.add((UUID) ReflectionUtils.invoke(getResidentUUIDMethod, resident));
                }
            }

            return RegionInfo.builder()
                    .id(townName)
                    .provider(getName())
                    .owners(owners)
                    .members(members)
                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Belirtilen konumdaki Towny arsasının (TownBlock) sınırlarını alır.
     * Towny arsaları chunk tabanlı olduğundan, bu metot arsanın bulunduğu tüm chunk'ın sınırlarını döndürür.
     *
     * @param location Sınırları alınacak bölgenin içindeki bir konum.
     * @return Chunk'ın sınırlarını içeren bir {@link RegionBounds} nesnesi veya arsa bulunamazsa null.
     */
    @Override
    public RegionBounds getRegionBounds(Location location) {
        if (!initialized) return null;

        World world = location.getWorld();
        if (world == null) return null;

        try {
            Object townBlock = ReflectionUtils.invoke(getTownBlockMethod, townyAPIInstance, location);
            if (townBlock == null) return null;

            boolean hasTown = (boolean) ReflectionUtils.invoke(hasTownMethod, townBlock);
            if (!hasTown) return null;

            org.bukkit.Chunk chunk = location.getChunk();

            int minX = chunk.getX() * 16;
            int minZ = chunk.getZ() * 16;
            int minY = 0;
            try {
                minY = world.getMinHeight();
            } catch (NoSuchMethodError ignored) { }

            int maxX = minX + 15;
            int maxZ = minZ + 15;
            int maxY = world.getMaxHeight();

            Location min = new Location(world, minX, minY, minZ);
            Location max = new Location(world, maxX, maxY, maxZ);

            return new RegionBounds(min, max);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * RegionShield'ın {@link InteractionType} enum'unu Towny'nin {@code ActionType} enum nesnesine çevirir.
     *
     * @param type Çevrilecek etkileşim türü.
     * @return Karşılık gelen önbelleğe alınmış Towny ActionType nesnesi.
     */
    private Object getTownyAction(InteractionType type) {
        switch (type) {
            case BLOCK_BREAK:
            case TRAMPLE:
                return actionDestroy;
            case BLOCK_PLACE:
            case BUCKET_USE:
                return actionBuild;
            case INTERACT:
            case PVP:
            case DAMAGE_ENTITY:
            case MOB_DAMAGE:
                return actionItemUse;
            case CONTAINER_ACCESS:
                return actionSwitch;
            default:
                return actionBuild;
        }
    }
}