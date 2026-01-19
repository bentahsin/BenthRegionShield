package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.BenthRegionShield;
import com.bentahsin.regionshield.annotations.*;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.model.RegionInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * RegionShield ek açıklamalarını (annotations) işleyen ve mantığını yürüten dahili motor.
 * Bu sınıf, bir metot üzerindeki ek açıklamaları bir kez yansıtma (reflection) ile okur,
 * bu mantığı çalıştırılabilir bir formata "derler" ve sonraki çağrılar için önbelleğe alır.
 * Bu yaklaşım, her çağrıda maliyetli yansıtma işlemlerini tekrarlamayı önleyerek performansı önemli ölçüde artırır.
 * <p>
 * Bu sınıf, API'nin dahili bir parçasıdır ve son kullanıcılar tarafından doğrudan kullanılması amaçlanmamıştır.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ShieldGate {

    private final BenthRegionShield manager;
    private final Map<String, GateLogic> gateCache = new ConcurrentHashMap<>();

    /**
     * ShieldGate'in yeni bir örneğini oluşturur.
     *
     * @param manager Ana BenthRegionShield API yöneticisi.
     */
    public ShieldGate(BenthRegionShield manager) {
        this.manager = manager;
    }

    /**
     * Belirtilen bir metot için ek açıklama tabanlı koruma denetimini gerçekleştirir.
     * Bu metot, her çağrıda yansıtma yapmaz. Bunun yerine, metot için önceden derlenmiş ve
     * önbelleğe alınmış mantığı alır ve çalıştırır. Eğer mantık önbellekte yoksa,
     * {@link #buildLogic} metodunu çağırarak oluşturur ve önbelleğe ekler.
     *
     * @param instance   Metodun ait olduğu nesne örneği.
     * @param methodName Denetlenecek metodun adı.
     * @param player     Denetimin hedefi olan oyuncu.
     * @param paramTypes Metodun parametre türleri (overload edilmiş metotları ayırt etmek için).
     * @return Oyuncunun eylemi gerçekleştirmesine izin veriliyorsa true, aksi takdirde false.
     */
    public boolean inspect(Object instance, String methodName, Player player, Class<?>... paramTypes) {
        String key = instance.getClass().getName() + "#" + methodName;
        GateLogic logic = gateCache.computeIfAbsent(key, k -> buildLogic(instance.getClass(), methodName, paramTypes));

        if (logic.bypassPerm != null && player.hasPermission(logic.bypassPerm)) {
            return true;
        }
        if (logic.blockChecker != null && !logic.blockChecker.test(player)) {
            return false;
        }

        RegionInfo info = logic.infoFetcher.apply(player);

        for (BiPredicate<Player, RegionInfo> validator : logic.validators) {
            if (!validator.test(player, info)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Bir metot üzerindeki RegionShield ek açıklamalarını yansıtma (reflection) ile okur
     * ve bu kuralları temsil eden bir {@link GateLogic} nesnesi oluşturur.
     * Bu metot, her metot için SADECE BİR KEZ çağrılır ve sonucu önbelleğe alınır.
     * Tüm maliyetli işlemler burada yapılır.
     *
     * @param clazz      Metodun bulunduğu sınıf.
     * @param methodName İncelenecek metodun adı.
     * @param paramTypes Metodun parametre türleri.
     * @return Derlenmiş doğrulama mantığını içeren bir GateLogic nesnesi.
     */
    private GateLogic buildLogic(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        Method method;
        try {
            method = clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            return new GateLogic(null, null, p -> manager.getRegionInfo(p.getLocation()), Collections.emptyList());
        }

        ShieldBypass bypass = getAnnotation(clazz, method, ShieldBypass.class);
        RequireBlock requireBlock = getAnnotation(clazz, method, RequireBlock.class);
        RegionProvider provider = getAnnotation(clazz, method, RegionProvider.class);
        RegionBlacklist blacklist = getAnnotation(clazz, method, RegionBlacklist.class);
        RequireWilderness wilderness = getAnnotation(clazz, method, RequireWilderness.class);
        RegionCheck check = getAnnotation(clazz, method, RegionCheck.class);
        RegionLimit limit = getAnnotation(clazz, method, RegionLimit.class);
        RegionRole role = getAnnotation(clazz, method, RegionRole.class);

        String bypassPerm = (bypass != null) ? bypass.value() : null;

        Predicate<Player> blockChecker = null;
        if (requireBlock != null) {
            Set<Material> allowed = EnumSet.noneOf(Material.class);
            Collections.addAll(allowed, requireBlock.value());
            boolean checkGround = requireBlock.checkGround();
            blockChecker = p -> {
                Block b = checkGround ? p.getLocation().getBlock().getRelative(BlockFace.DOWN) : p.getLocation().getBlock();
                return allowed.contains(b.getType());
            };
        }

        Function<Player, RegionInfo> infoFetcher;
        if (provider != null) {
            String providerName = provider.value();
            infoFetcher = p -> manager.getRegionInfo(providerName, p.getLocation());
        } else {
            infoFetcher = p -> manager.getRegionInfo(p.getLocation());
        }

        List<BiPredicate<Player, RegionInfo>> validators = new ArrayList<>();

        if (blacklist != null) {
            Set<String> bannedIds = new HashSet<>(Arrays.asList(blacklist.ids()));
            String specificProvider = blacklist.provider();
            validators.add((p, info) -> {
                if (info == null) return true;
                if (specificProvider.isEmpty() || info.getProvider().equalsIgnoreCase(specificProvider)) {
                    return !bannedIds.contains(info.getId());
                }
                return true;
            });
        }

        if (wilderness != null) {
            validators.add((p, info) -> info == null);
        }

        if (check != null) {
            validators.add((p, info) -> {
                if (!check.bypassPerm().isEmpty() && p.hasPermission(check.bypassPerm())) return true;
                ShieldResponse response = manager.checkResult(p, p.getLocation(), check.type());
                return response.isAllowed();
            });
        }

        if (limit != null || role != null) {
            validators.add((p, info) -> info != null);
        }

        if (limit != null) {
            String targetId = limit.id();
            String targetProvider = limit.provider();
            validators.add((p, info) -> {
                if (!info.getId().equalsIgnoreCase(targetId)) return false;
                return targetProvider.isEmpty() || info.getProvider().equalsIgnoreCase(targetProvider);
            });
        }

        if (role != null) {
            validators.add((p, info) -> {
                UUID uuid = p.getUniqueId();
                return switch (role.value()) {
                    case OWNER -> info.getOwners().contains(uuid);
                    case MEMBER_OR_OWNER -> info.getOwners().contains(uuid) || info.getMembers().contains(uuid);
                    case VISITOR -> true;
                    default -> false;
                };
            });
        }

        return new GateLogic(bypassPerm, blockChecker, infoFetcher, validators);
    }

    /**
     * Bir ek açıklamayı (annotation) önce metot üzerinde, eğer bulunamazsa sınıf üzerinde arar.
     * Bu, bir sınıfın tamamı için varsayılan bir kural belirleyip,
     * belirli metotlar için bu kuralı geçersiz kılma (override) olanağı tanır.
     *
     * @param clazz           Sınıf referansı.
     * @param method          Metot referansı.
     * @param annotationClass Aranacak ek açıklama türü.
     * @param <T>             Ek açıklamanın tipi.
     * @return Bulunan ek açıklama örneği veya bulunamazsa null.
     */
    private <T extends Annotation> T getAnnotation(Class<?> clazz, Method method, Class<T> annotationClass) {
        if (method.isAnnotationPresent(annotationClass)) {
            return method.getAnnotation(annotationClass);
        }
        if (clazz.isAnnotationPresent(annotationClass)) {
            return clazz.getAnnotation(annotationClass);
        }
        return null;
    }

    /**
     * Bir metot için derlenmiş doğrulama mantığını tutan basit bir veri yapısı.
     * Bu sınıf, yansıtma ile elde edilen kuralları, hızlıca çalıştırılabilen
     * fonksiyonel arayüzler (Predicate, Function) olarak saklar.
     *
     * @param bypassPerm   Varsa, tüm kontrolleri atlamak için gereken yetki (permission).
     * @param blockChecker Varsa, oyuncunun belirli bir blokta/bloğun üzerinde olup olmadığını kontrol eden fonksiyon.
     * @param infoFetcher  Oyuncunun mevcut konumuna göre bölge bilgilerini getiren fonksiyon.
     * @param validators   Sırayla çalıştırılacak olan tüm doğrulama kurallarının listesi.
     */
        private record GateLogic(String bypassPerm, Predicate<Player> blockChecker,
                                 Function<Player, RegionInfo> infoFetcher,
                                 List<BiPredicate<Player, RegionInfo>> validators) {
            private GateLogic(String bypassPerm,
                              Predicate<Player> blockChecker,
                              Function<Player, RegionInfo> infoFetcher,
                              List<BiPredicate<Player, RegionInfo>> validators) {
                this.bypassPerm = bypassPerm;
                this.blockChecker = blockChecker;
                this.infoFetcher = infoFetcher;
                this.validators = Collections.unmodifiableList(validators);
            }
        }
}