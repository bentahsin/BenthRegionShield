package com.bentahsin.regionshield;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.model.InteractionType;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * BenthRegionShield kütüphanesinin ana yönetici sınıfıdır.
 * Tüm koruma hook'larını yönetir, sorguları işler ve performans için önbellekleme (caching) yapar.
 */
public class BenthRegionShield {

    @Getter
    private final JavaPlugin plugin;
    private final List<IShieldHook> hooks;

    /**
     * Akıllı Önbellek Sistemi.
     * Aynı oyuncu, aynı blok ve aynı işlem için 1 saniye içinde tekrar sorgu yaparsa
     * hook'ları yormadan hafızadan cevap verir.
     */
    private final Cache<String, ShieldResponse> resultCache;

    @Getter @Setter
    private boolean debugMode = false;

    /**
     * Adminlerin korumaları aşması için gerekli yetki.
     * Varsayılan: "regionshield.bypass"
     */
    @Getter @Setter
    private String bypassPermission = "regionshield.bypass";

    public BenthRegionShield(JavaPlugin plugin) {
        this.plugin = plugin;
        this.hooks = new ArrayList<>();

        this.resultCache = CacheBuilder.newBuilder()
                .expireAfterWrite(500, TimeUnit.MILLISECONDS)
                .maximumSize(10000)
                .build();
    }

    public void registerHook(IShieldHook hook) {
        if (hook == null) return;

        if (hook.canInitialize()) {
            hooks.add(hook);
            hooks.sort(Comparator.comparingInt((IShieldHook h) -> h.getPriority().getValue()).reversed());

            plugin.getLogger().info("[RegionShield] Hook aktif: " + hook.getName());
        } else {
            if (debugMode) {
                plugin.getLogger().warning("[RegionShield] Hook pas geçildi: " + hook.getName());
            }
        }
    }

    public void unregisterAll() {
        hooks.clear();
        resultCache.invalidateAll();
    }

    public boolean canInteract(Player player, Location location, InteractionType type) {
        return checkResult(player, location, type).isAllowed();
    }

    /**
     * Detaylı yetki sorgusu (Cache Destekli).
     */
    public ShieldResponse checkResult(Player player, Location location, InteractionType type) {
        if (player.hasPermission(bypassPermission) || player.isOp()) {
            return ShieldResponse.allow();
        }

        String cacheKey = generateCacheKey(player, location, type);
        ShieldResponse cachedResponse = resultCache.getIfPresent(cacheKey);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        for (IShieldHook hook : hooks) {
            try {
                ShieldResponse response = hook.check(player, location, type);

                if (response.isDenied()) {
                    if (debugMode) {
                        plugin.getLogger().info(String.format(
                                "[RegionShield] Engellendi -> Oyuncu: %s, Sebep: %s",
                                player.getName(), response.getProviderName()
                        ));
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
     * Cache için benzersiz bir anahtar oluşturur.
     */
    private String generateCacheKey(Player player, Location location, InteractionType type) {
        return player.getUniqueId() + "|" +
                Objects.requireNonNull(location.getWorld()).getName() + "|" +
                location.getBlockX() + "|" +
                location.getBlockY() + "|" +
                location.getBlockZ() + "|" +
                type.name();
    }

    /**
     * İsimle özel bir hook'u getirir.
     * Örn: getHook("WorldGuard")
     */
    public IShieldHook getHook(String name) {
        for (IShieldHook hook : hooks) {
            if (hook.getName().equalsIgnoreCase(name)) {
                return hook;
            }
        }
        return null;
    }

    /**
     * Belirli bir hook'u devre dışı bırakmak için (Runtime).
     */
    public void unregisterHook(String name) {
        hooks.removeIf(hook -> hook.getName().equalsIgnoreCase(name));
        resultCache.invalidateAll();
    }

    /**
     * Sadece belirtilen koruma eklentisine sorar.
     * Diğerlerini ve Cache'i tamamen pas geçer.
     * * Kullanım: manager.checkSpecific("WorldGuard", player, loc, type);
     */
    public ShieldResponse checkSpecific(String hookName, Player player, Location location, InteractionType type) {
        IShieldHook hook = getHook(hookName);
        if (hook == null) return ShieldResponse.allow();

        try {
            return hook.check(player, location, type);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "[RegionShield] Specific Check hatası (" + hookName + ")", e);
            return ShieldResponse.allow();
        }
    }
}