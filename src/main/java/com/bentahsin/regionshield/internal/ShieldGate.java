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
 * <b>RegionShield Annotation İşleme Motoru.</b>
 * <p>
 * Bu sınıf, metotların üzerine eklenen {@code @RegionCheck}, {@code @RegionLimit} vb.
 * ek açıklamaları okur, analiz eder ve çalıştırılabilir mantığa dönüştürür.
 * <p>
 * <b>Performans Notu:</b> Yansıtma (Reflection) işlemi her metot için yalnızca <u>bir kez</u> yapılır.
 * Oluşturulan mantık {@link GateLogic} kaydı (record) olarak önbelleğe alınır ve sonraki çağrılarda
 * doğrudan bu önbellek kullanılır. Bu sayede çalışma zamanı maliyeti minimuma indirilir.
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class ShieldGate {

    private final BenthRegionShield manager;
    private final Map<String, GateLogic> gateCache = new ConcurrentHashMap<>();

    /**
     * ShieldGate motorunu başlatır.
     *
     * @param manager Ana API yöneticisi.
     */
    public ShieldGate(BenthRegionShield manager) {
        this.manager = manager;
    }

    /**
     * Bir metodun çalıştırılıp çalıştırılmayacağını, üzerindeki ek açıklamalara göre denetler.
     * <p>
     * Bu metot, geliştiricilerin kodlarının başına {@code if (!api.guard(...)) return;} şeklinde
     * ekleyerek koruma sağlamaları için tasarlanmıştır.
     *
     * @param instance   Metodun ait olduğu sınıf örneği (this).
     * @param methodName Denetlenecek metodun adı.
     * @param player     İşlemi yapan oyuncu.
     * @param paramTypes Metodun parametre türleri (Overloading desteği için).
     * @return Oyuncunun geçişine izin veriliyorsa {@code true}, engelleniyorsa {@code false}.
     */
    public boolean inspect(Object instance, String methodName, Player player, Class<?>... paramTypes) {
        String key = instance.getClass().getName() + "#" + methodName;
        GateLogic logic = gateCache.computeIfAbsent(key, k -> buildLogic(instance.getClass(), methodName, paramTypes));

        if (player.hasPermission(manager.getOptions().getBypassPermission()) || player.isOp()) {
            return true;
        }

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
     * Belirtilen metodu analiz eder ve koruma mantığını inşa eder.
     * <p>
     * Bu metot "Ağır İş" yapan kısımdır ve sadece önbellekte veri yoksa çağrılır.
     *
     * @param clazz      Sınıf.
     * @param methodName Metot Adı.
     * @param paramTypes Parametreler.
     * @return Derlenmiş mantık seti.
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
                };
            });
        }

        return new GateLogic(bypassPerm, blockChecker, infoFetcher, validators);
    }

    /**
     * Annotation okuma yardımcısı. Öncelik metottadır, yoksa sınıfa bakar.
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
     * Derlenmiş ve çalıştırılmaya hazır koruma kurallarını tutan veri kaydı.
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