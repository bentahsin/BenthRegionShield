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
 * <b>BenthRegionShield API'sinin Merkezi Yönetim Sınıfı (Facade).</b>
 * <p>
 * Bu sınıf, farklı bölge koruma eklentilerini (WorldGuard, Towny, Lands vb.) tek bir çatı altında
 * toplayarak geliştiricilere birleşik ve standartlaştırılmış bir arayüz sunar.
 * <p>
 * Temel Özellikleri:
 * <ul>
 *     <li><b>Hook Yönetimi:</b> Farklı koruma eklentilerini öncelik sırasına göre yönetir.</li>
 *     <li><b>Akıllı Önbellekleme:</b> Yüksek performans için sorgu sonuçlarını kısa süreliğine önbelleğe alır.</li>
 *     <li><b>Annotation İşleme:</b> {@code @RegionCheck} gibi ek açıklamaları işleyen {@link ShieldGate}'i barındırır.</li>
 *     <li><b>Özelleştirilebilirlik:</b> {@link BenthShieldOptions} ile tüm davranışlar yapılandırılabilir.</li>
 * </ul>
 */
public class BenthRegionShield {

    /**
     * Bu API'yi başlatan ve sahipliğini yapan ana Java eklentisi.
     */
    @Getter
    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    private final JavaPlugin plugin;

    /**
     * Kütüphanenin davranışlarını (yetkiler, loglar, partiküller vb.) belirleyen yapılandırma nesnesi.
     */
    @Getter
    private final BenthShieldOptions options;

    private final List<IShieldHook> hooks;
    private final Cache<ShieldCacheKey, ShieldResponse> resultCache;

    @Getter
    @SuppressFBWarnings("EI_EXPOSE_REP")
    private final ShieldGate gate;

    private final RegionLimitManager limitManager;

    /**
     * Hata ayıklama modunu açar veya kapatır.
     * Açık olduğunda, engellenen işlemler ve hook hataları konsola detaylı basılır.
     */
    @Getter @Setter
    private boolean debugMode = false;

    /**
     * Varsayılan ayarlarla yeni bir BenthRegionShield örneği oluşturur.
     * <p>
     * Bu kurucu, {@link BenthShieldOptions#defaults()} kullanarak standart ayarları yükler.
     *
     * @param plugin Bu API'yi kullanan ana JavaPlugin örneği.
     */
    public BenthRegionShield(JavaPlugin plugin) {
        this(plugin, BenthShieldOptions.defaults());
    }

    /**
     * Özelleştirilmiş ayarlarla yeni bir BenthRegionShield örneği oluşturur.
     * <p>
     * Bu işlem sırasında:
     * <ul>
     *     <li>Önbellek sistemi (500ms expire) başlatılır.</li>
     *     <li>Olay dinleyicileri (MovementListener, LimitManager) kaydedilir.</li>
     *     <li>Periyodik görevler (StayTask) zamanlanır.</li>
     * </ul>
     *
     * @param plugin Bu API'yi kullanan ana JavaPlugin örneği.
     * @param options Özelleştirme seçeneklerini içeren {@link BenthShieldOptions} nesnesi.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public BenthRegionShield(JavaPlugin plugin, BenthShieldOptions options) {
        this.plugin = plugin;
        this.options = options;
        this.hooks = new ArrayList<>();
        this.gate = new ShieldGate(this);
        this.limitManager = new RegionLimitManager(this);

        this.resultCache = CacheBuilder.newBuilder()
                .expireAfterWrite(500, TimeUnit.MILLISECONDS)
                .maximumSize(10000)
                .build();

        plugin.getServer().getPluginManager().registerEvents(this.limitManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(new RegionMovementListener(this), plugin);

        new RegionStayTask(this).runTaskTimer(plugin, 100L, 100L);
    }

    /**
     * Sisteme yeni bir koruma entegrasyonu (hook) kaydeder.
     * <p>
     * Kayıt sırasında hook'un {@code canInitialize()} metodu çağrılır. Başarılı olursa listeye eklenir
     * ve liste {@link IShieldHook#getPriority()} değerine göre (Yüksekten Düşüğe) yeniden sıralanır.
     *
     * @param hook Kaydedilecek hook uygulaması (Örn: WorldGuardHook, TownySafeHook).
     */
    public void registerHook(IShieldHook hook) {
        if (hook == null) return;

        if (hook.canInitialize()) {
            hooks.add(hook);
            hooks.sort(Comparator.comparingInt((IShieldHook h) -> h.getPriority().getValue()).reversed());

            logInfo("Hook aktif: " + hook.getName());
        } else if (debugMode) {
            logWarning("Hook pas geçildi: " + hook.getName());
        }
    }

    /**
     * Mevcut olarak kayıtlı tüm hook'ları kaldırır ve sonuç önbelleğini temizler.
     * Plugin disable edilirken veya reload atılırken kullanılması önerilir.
     */
    public void unregisterAll() {
        hooks.clear();
        resultCache.invalidateAll();
    }

    /**
     * Bir oyuncunun belirli bir konumda belirli bir etkileşimi gerçekleştirmesine izin verilip verilmediğini kontrol eder.
     * <p>
     * Bu metot {@link #checkResult(Player, Location, InteractionType)} metodunun basitleştirilmiş bir sarmalayıcısıdır.
     *
     * @param player   Etkileşimi gerçekleştiren oyuncu.
     * @param location Etkileşimin gerçekleştiği konum.
     * @param type     Gerçekleştirilen etkileşim türü (Blok Kırma, Koyma vb.).
     * @return Oyuncunun etkileşimde bulunmasına izin veriliyorsa {@code true}, aksi takdirde {@code false}.
     */
    public boolean canInteract(Player player, Location location, InteractionType type) {
        return checkResult(player, location, type).isAllowed();
    }

    /**
     * Bir oyuncunun bir konumda etkileşim kurup kuramayacağını belirlemek için tam kapsamlı bir kontrol gerçekleştirir.
     * <p>
     * <b>Kontrol Sıralaması:</b>
     * <ol>
     *     <li><b>Bypass Kontrolü:</b> {@code options.getBypassPermission()} yetkisine sahipse veya OP ise izin verilir.</li>
     *     <li><b>Dünya Kontrolü:</b> Konum geçersiz bir dünyadaysa izin verilir.</li>
     *     <li><b>Önbellek (Cache):</b> Aynı sorgu son 500ms içinde yapıldıysa, önbellekten yanıt döndürülür.</li>
     *     <li><b>Hook Sorgusu:</b> Kayıtlı hook'lar öncelik sırasına göre gezilir. İşlemi reddeden (DENY) ilk hook sonucu belirler.</li>
     * </ol>
     *
     * @param player   Kontrol edilecek oyuncu.
     * @param location Kontrol edilecek konum.
     * @param type     Kontrol edilecek etkileşim türü.
     * @return Etkileşimin sonucunu (izin/red) ve sağlayıcısını içeren {@link ShieldResponse} nesnesi.
     */
    public ShieldResponse checkResult(Player player, Location location, InteractionType type) {
        if (player.hasPermission(options.getBypassPermission()) || player.isOp()) {
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
                plugin.getLogger().log(Level.SEVERE, options.getLogPrefix() + "Hook hatası: " + hook.getName(), e);
            }
        }

        ShieldResponse allowed = ShieldResponse.allow();
        resultCache.put(cacheKey, allowed);
        return allowed;
    }

    /**
     * Belirtilen konumdaki bölge hakkında bilgi (ID, Sahipler, Üyeler vb.) alır.
     * <p>
     * Hook'ları öncelik sırasına göre sorgular ve bölge bilgisi döndüren ilk hook'un sonucunu verir.
     *
     * @param location Bilgi alınacak konum.
     * @return Bölge varsa {@link RegionInfo} nesnesi, yoksa {@code null}.
     */
    public RegionInfo getRegionInfo(Location location) {
        for (IShieldHook hook : hooks) {
            try {
                RegionInfo info = hook.getRegionInfo(location);
                if (info != null) return info;
            } catch (Exception e) {
                if (debugMode) plugin.getLogger().severe(options.getLogPrefix() + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Adına göre belirli bir hook'tan (Örn: "WorldGuard") bölge bilgilerini alır.
     * Diğer hook'ları pas geçer.
     *
     * @param hookName   Bilgiyi sağlayacak hook'un adı (Büyük/küçük harf duyarsız).
     * @param location   Bilgi alınacak konum.
     * @return Hook bulunamazsa veya o konumda bölge yoksa {@code null}.
     */
    public RegionInfo getRegionInfo(String hookName, Location location) {
        IShieldHook hook = getHook(hookName);
        return (hook != null) ? hook.getRegionInfo(location) : null;
    }

    /**
     * Konsola "INFO" seviyesinde, yapılandırılmış önek (prefix) ile log basar.
     * {@link BenthShieldOptions#isLogToConsole()} kapalıysa işlem yapmaz.
     *
     * @param message Loglanacak mesaj.
     */
    public void logInfo(String message) {
        if (options.isLogToConsole()) {
            plugin.getLogger().info(options.getLogPrefix() + message);
        }
    }

    /**
     * Konsola "WARNING" seviyesinde, yapılandırılmış önek (prefix) ile log basar.
     * {@link BenthShieldOptions#isLogToConsole()} kapalıysa işlem yapmaz.
     *
     * @param message Loglanacak uyarı mesajı.
     */
    public void logWarning(String message) {
        if (options.isLogToConsole()) {
            plugin.getLogger().warning(options.getLogPrefix() + message);
        }
    }

    /**
     * Oyuncunun o anda içinde bulunduğu bölgenin sınırlarını görsel olarak (parçacıklarla) gösterir.
     * <p>
     * Sınırlar, oyuncunun konumunda bir bölge tanımlayan en yüksek öncelikli hook tarafından sağlanır.
     * Kullanılacak parçacık türü {@link BenthShieldOptions#getVisualizationParticle()} ile belirlenir.
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
                    plugin.getLogger().log(Level.WARNING, options.getLogPrefix() + "Error getting bounds from hook: " + hook.getName(), e);
                }
            }
        }

        if (bounds == null) {
            return;
        }

        RegionVisualizer.show(plugin, player, bounds, options.getVisualizationParticle());
    }

    /**
     * Kayıtlı bir hook'u benzersiz adına göre arar ve döndürür.
     *
     * @param name Alınacak hook'un adı (büyük/küçük harfe duyarsız).
     * @return {@link IShieldHook} örneği veya bulunamazsa {@code null}.
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
     * @return Hook tarafından döndürülen {@link ShieldResponse} veya hook bulunamazsa/hata olursa izin veren bir response.
     */
    public ShieldResponse checkSpecific(String hookName, Player player, Location location, InteractionType type) {
        IShieldHook hook = getHook(hookName);
        if (hook == null) return ShieldResponse.allow();

        try {
            return hook.check(player, location, type);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, options.getLogPrefix() + "Specific Check hatası: " + hookName, e);
            return ShieldResponse.allow();
        }
    }

    /**
     * Debug modu açıkken, engellenen bir işlem hakkında konsola bilgi verir.
     */
    private void logDebug(Player player, String provider) {
        plugin.getLogger().info(options.getLogPrefix() + "Engellendi -> Oyuncu: " + player.getName() + ", Sebep: " + provider);
    }

    /**
     * Bir metodu, üzerindeki RegionShield ek açıklamaları ({@code @RegionCheck}, {@code @RegionLimit} vb.)
     * kurallarına göre denetler.
     * <p>
     * Bu metot genellikle Aspect-Oriented yaklaşım veya manuel koruma kontrolleri için kullanılır.
     *
     * @param instance   Metodun ait olduğu nesne örneği.
     * @param methodName Denetlenecek metodun adı.
     * @param player     Kontrolün yapılacağı oyuncu.
     * @param paramTypes Metodun parametre türleri.
     * @return Oyuncunun metoda devam etmesine izin veriliyorsa {@code true}, engellendiyse {@code false}.
     */
    public boolean guard(Object instance, String methodName, Player player, Class<?>... paramTypes) {
        return gate.inspect(instance, methodName, player, paramTypes);
    }

    /**
     * Belirtilen bir bölgeye girebilecek maksimum oyuncu sayısını ayarlar.
     *
     * @param provider Bölgeyi sağlayan hook'un adı (Örn: "WorldGuard").
     * @param regionId Bölgenin kimliği (ID).
     * @param limit    Bu bölge için izin verilen maksimum oyuncu sayısı.
     */
    public void setRegionLimit(String provider, String regionId, int limit) {
        limitManager.setLimit(provider, regionId, limit);
    }
}