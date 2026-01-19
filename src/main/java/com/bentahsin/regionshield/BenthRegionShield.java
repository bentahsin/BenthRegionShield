package com.bentahsin.regionshield;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.*;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import com.bentahsin.regionshield.model.RegionInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * BenthRegionShield API'sinin ana sınıfı.
 * Bu sınıf, birleşik bir arayüz aracılığıyla çeşitli bölge koruma eklentilerini (hook'lar)
 * yönetmek ve sorgulamak için merkezi bir merkez görevi görür.
 */
public class BenthRegionShield {

    @Getter
    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    private final JavaPlugin plugin;
    private final List<IShieldHook> hooks;
    private final Cache<ShieldCacheKey, ShieldResponse> resultCache;

    @Getter
    @SuppressFBWarnings("EI_EXPOSE_REP")
    private final ShieldGate gate;

    private final RegionLimitManager limitManager;

    @Getter @Setter
    private boolean debugMode = false;

    @Getter @Setter
    private String bypassPermission = "regionshield.bypass";

    /**
     * Yeni bir BenthRegionShield örneği oluşturur.
     *
     * @param plugin Bu API'ye sahip olan JavaPlugin örneği.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public BenthRegionShield(JavaPlugin plugin) {
        this.plugin = plugin;
        this.hooks = new ArrayList<>();
        this.gate = new ShieldGate(this);
        this.limitManager = new RegionLimitManager(this);

        this.resultCache = CacheBuilder.newBuilder()
                .expireAfterWrite(500, TimeUnit.MILLISECONDS)
                .maximumSize(10000)
                .build();

        plugin.getServer().getPluginManager().registerEvents(this.limitManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(new RegionMovementListener(this), plugin);
    }

    /**
     * Yeni bir shield kancası (hook) kaydeder. Hook'lar, farklı bölge koruma eklentileriyle
     * entegre olmak için kullanılır. Kaydedilen hook'lar öncelik değerine göre (en yüksekten en düşüğe) sıralanır.
     *
     * @param hook Kaydedilecek hook uygulaması.
     */
    public void registerHook(IShieldHook hook) {
        if (hook == null) return;

        if (hook.canInitialize()) {
            hooks.add(hook);
            hooks.sort(Comparator.comparingInt((IShieldHook h) -> h.getPriority().getValue()).reversed());

            plugin.getLogger().info("[RegionShield] Hook aktif: " + hook.getName());
        } else if (debugMode) {
            plugin.getLogger().warning("[RegionShield] Hook pas geçildi: " + hook.getName());
        }
    }

    /**
     * Mevcut olarak kayıtlı tüm hook'ları kaldırır ve sonuç önbelleğini temizler.
     */
    public void unregisterAll() {
        hooks.clear();
        resultCache.invalidateAll();
    }

    /**
     * Bir oyuncunun belirli bir konumda belirli bir etkileşimi gerçekleştirmesine izin verilip verilmediğini kontrol eder.
     * Bu, basit bir boolean döndüren kullanışlı bir metottur.
     *
     * @param player   Etkileşimi gerçekleştiren oyuncu.
     * @param location Etkileşimin gerçekleştiği konum.
     * @param type     Gerçekleştirilen etkileşim türü.
     * @return Oyuncunun etkileşimde bulunmasına izin veriliyorsa true, aksi takdirde false.
     */
    public boolean canInteract(Player player, Location location, InteractionType type) {
        return checkResult(player, location, type).isAllowed();
    }

    /**
     * Bir oyuncunun bir konumda etkileşim kurup kuramayacağını görmek için ayrıntılı bir kontrol gerçekleştirir.
     * Bu metot, bypass yetkilerini kontrol eder, önbelleğe bakar ve ardından kayıtlı tüm hook'ları
     * öncelik sırasına göre sorgular. Eylemi reddeden ilk hook, sonucu belirler.
     *
     * @param player   Kontrol edilecek oyuncu.
     * @param location Kontrol edilecek konum.
     * @param type     Kontrol edilecek etkileşim türü.
     * @return Etkileşimin sonucunu içeren bir {@link ShieldResponse} nesnesi.
     */
    public ShieldResponse checkResult(Player player, Location location, InteractionType type) {
        if (player.hasPermission(bypassPermission) || player.isOp()) {
            return ShieldResponse.allow();
        }

        World world = location.getWorld();
        if (world == null) {
            return ShieldResponse.allow();
        }

        ShieldCacheKey cacheKey = new ShieldCacheKey(
                player.getUniqueId(),
                world.getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                type
        );

        ShieldResponse cachedResponse = resultCache.getIfPresent(cacheKey);
        if (cachedResponse != null) return cachedResponse;

        for (IShieldHook hook : hooks) {
            try {
                ShieldResponse response = hook.check(player, location, type);

                if (response.isDenied()) {
                    if (debugMode) {
                        logDebug(player, response.getProviderName());
                    }
                    resultCache.put(cacheKey, response);
                    return response;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[RegionShield] Hook hatası: " + hook.getName(), e);
            }
        }

        ShieldResponse allowed = ShieldResponse.allow();
        resultCache.put(cacheKey, allowed);
        return allowed;
    }

    /**
     * Belirli bir konumdaki bölge hakkında bilgi alır.
     * Hook'ları öncelik sırasına göre sorgular ve bir bölge tanımlayan ilk hook'tan gelen bilgiyi döndürür.
     *
     * @param location Bilgi alınacak konum.
     * @return Bir {@link RegionInfo} nesnesi veya bölge bulunamazsa null.
     */
    public RegionInfo getRegionInfo(Location location) {
        for (IShieldHook hook : hooks) {
            try {
                RegionInfo info = hook.getRegionInfo(location);
                if (info != null) return info;
            } catch (Exception e) {
                if (debugMode) plugin.getLogger().severe(e.getMessage());
            }
        }
        return null;
    }

    /**
     * Adına göre belirli bir hook'tan bölge bilgilerini alır.
     *
     * @param hookName   Bilgiyi sağlayacak hook'un adı.
     * @param location   Bilgi alınacak konum.
     * @return Bir {@link RegionInfo} nesnesi veya hook bulunamazsa/bölge yoksa null.
     */
    public RegionInfo getRegionInfo(String hookName, Location location) {
        IShieldHook hook = getHook(hookName);
        return (hook != null) ? hook.getRegionInfo(location) : null;
    }

    /**
     * Oyuncunun o anda içinde bulunduğu bölgenin sınırlarını görsel olarak (parçacıklarla) gösterir.
     * Sınırlar, oyuncunun konumunda bir bölge tanımlayan en yüksek öncelikli hook tarafından sağlanır.
     *
     * @param player Sınırları görecek oyuncu.
     */
    public void showBoundaries(Player player) {
        Location loc = player.getLocation();
        RegionBounds bounds = null;

        for (IShieldHook hook : hooks) {
            try {
                bounds = hook.getRegionBounds(loc);
                if (bounds != null) break;
            } catch (Exception e) {
                if (debugMode) {
                    plugin.getLogger().log(Level.WARNING, "Error getting bounds from hook: " + hook.getName(), e);
                }
            }
        }

        if (bounds == null) {
            return;
        }

        RegionVisualizer.show(plugin, player, bounds);
    }

    /**
     * Kayıtlı bir hook'u benzersiz adına göre alır.
     *
     * @param name Alınacak hook'un adı (büyük/küçük harfe duyarsız).
     * @return {@link IShieldHook} örneği veya bulunamazsa null.
     */
    public IShieldHook getHook(String name) {
        return hooks.stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Tek bir hook'u adına göre kayıttan kaldırır ve tüm önbelleği geçersiz kılar.
     *
     * @param name Kayıttan kaldırılacak hook'un adı (büyük/küçük harfe duyarsız).
     */
    public void unregisterHook(String name) {
        hooks.removeIf(hook -> hook.getName().equalsIgnoreCase(name));
        resultCache.invalidateAll();
    }

    /**
     * Diğerlerini yoksayarak yalnızca belirli, adlandırılmış bir hook'u kullanarak bir etkileşim kontrolü gerçekleştirir.
     *
     * @param hookName   Kullanılacak hook'un adı.
     * @param player     Kontrol edilecek oyuncu.
     * @param location   Kontrol edilecek konum.
     * @param type       Kontrol edilecek etkileşim türü.
     * @return Hook tarafından döndürülen {@link ShieldResponse} veya hook bulunamazsa izin veren bir response.
     */
    public ShieldResponse checkSpecific(String hookName, Player player, Location location, InteractionType type) {
        IShieldHook hook = getHook(hookName);
        if (hook == null) return ShieldResponse.allow();

        try {
            return hook.check(player, location, type);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[RegionShield] Specific Check hatası: " + hookName, e);
            return ShieldResponse.allow();
        }
    }

    private void logDebug(Player player, String provider) {
        plugin.getLogger().info("[RegionShield] Engellendi -> Oyuncu: " + player.getName() + ", Sebep: " + provider);
    }

    /**
     * Çağrıldığı metodu RegionShield ek açıklamaları (annotations) açısından denetler.
     * Bu, geliştiricilerin kodlarını kolayca bölge korumasına almasını sağlayan güçlü bir özelliktir.
     * <p>
     * Kullanım: {@code if (!api.guard(this, "metodIsmi", player)) return;}
     *
     * @param instance   Metodun ait olduğu nesne örneği.
     * @param methodName Denetlenecek metodun adı.
     * @param player     Kontrolün yapılacağı oyuncu.
     * @param paramTypes Metodun parametre türleri (metot overload durumları için).
     * @return Oyuncunun metoda devam etmesine izin veriliyorsa true, engellendiyse false.
     */
    public boolean guard(Object instance, String methodName, Player player, Class<?>... paramTypes) {
        return gate.inspect(instance, methodName, player, paramTypes);
    }

    /**
     * Bir bölgeye oyuncu limiti koyar. Belirtilen bölgeye girebilecek maksimum oyuncu sayısını ayarlar.
     *
     * @param provider Eklenti ismi (Örn: "WorldGuard", "Towny"). Bu, hook adıyla eşleşmelidir.
     * @param regionId Bölgenin kimliği (ID).
     * @param limit    Bu bölge için maksimum oyuncu sayısı.
     */
    public void setRegionLimit(String provider, String regionId, int limit) {
        limitManager.setLimit(provider, regionId, limit);
    }
}