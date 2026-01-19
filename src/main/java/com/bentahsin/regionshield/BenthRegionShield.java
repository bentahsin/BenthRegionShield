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

    public void unregisterAll() {
        hooks.clear();
        resultCache.invalidateAll();
    }

    public boolean canInteract(Player player, Location location, InteractionType type) {
        return checkResult(player, location, type).isAllowed();
    }

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
                location.getWorld().getName(),
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

    public RegionInfo getRegionInfo(String hookName, Location location) {
        IShieldHook hook = getHook(hookName);
        return (hook != null) ? hook.getRegionInfo(location) : null;
    }

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

    public IShieldHook getHook(String name) {
        return hooks.stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public void unregisterHook(String name) {
        hooks.removeIf(hook -> hook.getName().equalsIgnoreCase(name));
        resultCache.invalidateAll();
    }

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
     * Çağrıldığı metodu Annotation açısından denetler.
     * Kullanım: if (!api.guard(this, "metodIsmi", player)) return;
     */
    public boolean guard(Object instance, String methodName, Player player, Class<?>... paramTypes) {
        return gate.inspect(instance, methodName, player, paramTypes);
    }

    /**
     * Bir bölgeye oyuncu limiti koyar.
     * @param provider Eklenti ismi (WorldGuard, Towny vb.)
     * @param regionId Bölge ID'si
     * @param limit Maksimum oyuncu sayısı
     */
    public void setRegionLimit(String provider, String regionId, int limit) {
        limitManager.setLimit(provider, regionId, limit);
    }
}