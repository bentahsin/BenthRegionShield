package com.bentahsin.regionshield.hooks.gp;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GriefPrevention eklentisi için "güvenli" bir entegrasyon (hook) sağlar.
 * <p>
 * Bu sınıf, GriefPrevention API'sine karşı doğrudan bir derleme zamanı bağımlılığı (hard dependency) oluşturmaz.
 * Bunun yerine, Java Reflection (Yansıtma) kullanarak GriefPrevention'ın sınıflarına, metotlarına ve
 * bazı dahili alanlarına (fields) çalışma zamanında (runtime) erişir. Bu, sunucuda GriefPrevention eklentisi
 * yüklü olmasa bile bu kodun hata vermesini ve sunucuyu çökertmesini engeller.
 * <p>
 * Tüm yansıtma işlemleri {@link #canInitialize()} metodunda bir kez yapılır ve bulunan metotlar
 * performans için sınıf alanlarında önbelleğe alınır.
 */
public class GriefPreventionHook implements IShieldHook {

    private Object dataStore;

    private Method getClaimMethod;
    private Method allowBuildMethod;
    private Method allowAccessMethod;
    private Method allowContainersMethod;

    private Method getIDMethod;
    private Method getLesserBoundaryCorner;
    private Method getGreaterBoundaryCorner;

    private boolean initialized = false;

    /**
     * Hook'un benzersiz adını döndürür.
     *
     * @return "GriefPrevention" String'i.
     */
    @Override
    public String getName() {
        return "GriefPrevention";
    }

    /**
     * Bu hook'un başlatılıp başlatılamayacağını kontrol eder.
     * GriefPrevention eklentisinin aktif olup olmadığını kontrol eder ve ardından yansıtma kullanarak
     * gerekli tüm sınıfları, metotları ve alanları bulup önbelleğe alır.
     *
     * @return Hook başarıyla başlatıldıysa true, aksi takdirde false.
     */
    @Override
    public boolean canInitialize() {
        if (!ReflectionUtils.isPluginActive("GriefPrevention")) return false;

        try {
            Class<?> gpClass = ReflectionUtils.getClass("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Class<?> dataStoreClass = ReflectionUtils.getClass("me.ryanhamshire.GriefPrevention.DataStore");
            Class<?> claimClass = ReflectionUtils.getClass("me.ryanhamshire.GriefPrevention.Claim");

            Object instance = ReflectionUtils.getField(gpClass, null, "instance");
            this.dataStore = ReflectionUtils.getField(gpClass, instance, "dataStore");

            this.getClaimMethod = ReflectionUtils.getMethod(dataStoreClass, "getClaimAt", Location.class, boolean.class, claimClass);
            this.allowBuildMethod = ReflectionUtils.getMethod(claimClass, "allowBuild", Player.class, Material.class);
            this.allowAccessMethod = ReflectionUtils.getMethod(claimClass, "allowAccess", Player.class);
            this.allowContainersMethod = ReflectionUtils.getMethod(claimClass, "allowContainers", Player.class);

            this.getIDMethod = ReflectionUtils.getMethod(claimClass, "getID");
            this.getLesserBoundaryCorner = ReflectionUtils.getMethod(claimClass, "getLesserBoundaryCorner");
            this.getGreaterBoundaryCorner = ReflectionUtils.getMethod(claimClass, "getGreaterBoundaryCorner");

            this.initialized = dataStore != null && getClaimMethod != null && allowBuildMethod != null;
            return this.initialized;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Bir oyuncunun belirli bir konumdaki bir GriefPrevention alanında (claim) eylem yapıp yapamayacağını kontrol eder.
     * GriefPrevention'ın izin metotları, izin verildiğinde {@code null}, verilmediğinde ise bir hata mesajı (String) döndürür.
     * Bu metot, bu davranışı standart bir {@link ShieldResponse} nesnesine çevirir.
     *
     * @param player   Eylemi gerçekleştiren oyuncu.
     * @param location Eylemin gerçekleştiği konum.
     * @param type     Gerçekleştirilen etkileşim türü.
     * @return GriefPrevention izin veriyorsa (metot null döndürürse) {@link ShieldResponse#allow()},
     *         vermiyorsa {@link ShieldResponse#deny(String)}.
     */
    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        if (!initialized) return ShieldResponse.allow();

        try {
            Object claim = ReflectionUtils.invoke(getClaimMethod, dataStore, location, false, null);

            if (claim == null) return ShieldResponse.allow();
            String resultMessage;

            switch (type) {
                case BLOCK_BREAK:
                case BLOCK_PLACE:
                case BUCKET_USE:
                case TRAMPLE:
                case DAMAGE_ENTITY:
                    resultMessage = (String) ReflectionUtils.invoke(allowBuildMethod, claim, player, location.getBlock().getType());
                    break;
                case CONTAINER_ACCESS:
                case MOB_DAMAGE:
                    resultMessage = (String) ReflectionUtils.invoke(allowContainersMethod, claim, player);
                    break;
                case INTERACT:
                    resultMessage = (String) ReflectionUtils.invoke(allowAccessMethod, claim, player);
                    break;
                case PVP:
                    return ShieldResponse.allow();
                default:
                    resultMessage = (String) ReflectionUtils.invoke(allowBuildMethod, claim, player, Material.AIR);
            }

            return resultMessage == null ? ShieldResponse.allow() : ShieldResponse.deny(getName());

        } catch (Exception e) {
            return ShieldResponse.allow();
        }
    }

    /**
     * Belirtilen konumdaki GriefPrevention alanı (claim) hakkında bilgi alır.
     * Alanın sahibi ve çeşitli güven seviyelerindeki üyeleri almak için yansıtma ile
     * Claim nesnesinin dahili alanlarına erişir.
     *
     * @param location Bilgi alınacak konum.
     * @return Konumda bir alan varsa bir {@link RegionInfo} nesnesi; aksi takdirde veya bir hata oluşursa null.
     */
    @Override
    public RegionInfo getRegionInfo(Location location) {
        if (!initialized) return null;

        try {
            Object claim = ReflectionUtils.invoke(getClaimMethod, dataStore, location, false, null);
            if (claim == null) return null;

            Long id = (Long) ReflectionUtils.invoke(getIDMethod, claim);

            List<UUID> owners = new ArrayList<>();
            UUID ownerID = (UUID) ReflectionUtils.getField(claim.getClass(), claim, "ownerID");
            if (ownerID != null) {
                owners.add(ownerID);
            }

            List<UUID> members = new ArrayList<>();
            addMembersFromField(claim, "builders", members);
            addMembersFromField(claim, "containers", members);
            addMembersFromField(claim, "accessors", members);
            addMembersFromField(claim, "managers", members);

            return RegionInfo.builder()
                    .id(id != null ? id.toString() : "Unknown")
                    .provider(getName())
                    .owners(owners)
                    .members(members)
                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Belirtilen konumdaki GriefPrevention alanının (claim) fiziksel sınırlarını alır.
     *
     * @param location Sınırları alınacak bölgenin içindeki herhangi bir konum.
     * @return Alanın sınırlarını içeren bir {@link RegionBounds} nesnesi veya alan bulunamazsa null.
     */
    @Override
    public RegionBounds getRegionBounds(Location location) {
        if (!initialized) return null;

        try {
            Object claim = ReflectionUtils.invoke(getClaimMethod, dataStore, location, false, null);
            if (claim == null) return null;

            Location lesser = (Location) ReflectionUtils.invoke(getLesserBoundaryCorner, claim);
            Location greater = (Location) ReflectionUtils.invoke(getGreaterBoundaryCorner, claim);

            if (lesser != null && greater != null) {
                return new RegionBounds(lesser, greater);
            }
            return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Yansıtma kullanarak bir Claim nesnesinin belirtilen alanından (field) üye listesini okuyan
     * ve bunları hedef listeye ekleyen bir yardımcı metot.
     * GriefPrevention bu listeleri UUID'lerin String temsilleri olarak tutar.
     *
     * @param claim      Üyelerin okunacağı Claim nesnesi.
     * @param fieldName  Okunacak alanın adı (örn: "builders", "managers").
     * @param targetList Bulunan UUID'lerin ekleneceği liste.
     */
    @SuppressWarnings("unchecked")
    private void addMembersFromField(Object claim, String fieldName, List<UUID> targetList) {
        try {
            ArrayList<String> list = (ArrayList<String>) ReflectionUtils.getField(claim.getClass(), claim, fieldName);
            if (list != null) {
                for (String s : list) {
                    try {
                        targetList.add(UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }
}