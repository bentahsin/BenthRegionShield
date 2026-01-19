package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.BenthRegionShield;
import com.bentahsin.regionshield.annotations.*;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class ShieldGate {

    private final BenthRegionShield manager;

    public ShieldGate(BenthRegionShield manager) {
        this.manager = manager;
    }

    public boolean inspect(Object instance, String methodName, Player player, Class<?>... paramTypes) {
        try {
            Class<?> clazz = instance.getClass();
            Method method = clazz.getMethod(methodName, paramTypes);

            if (!processAll(
                    clazz.getAnnotation(RegionCheck.class),
                    clazz.getAnnotation(RegionLimit.class),
                    clazz.getAnnotation(RegionRole.class),
                    clazz.getAnnotation(RequireWilderness.class),
                    clazz.getAnnotation(RegionProvider.class),
                    clazz.getAnnotation(RegionBlacklist.class),
                    clazz.getAnnotation(RequireBlock.class),   
                    clazz.getAnnotation(ShieldBypass.class),   
                    player)) {
                return false;
            }

            return processAll(
                    method.getAnnotation(RegionCheck.class),
                    method.getAnnotation(RegionLimit.class),
                    method.getAnnotation(RegionRole.class),
                    method.getAnnotation(RequireWilderness.class),
                    method.getAnnotation(RegionProvider.class),
                    method.getAnnotation(RegionBlacklist.class),
                    method.getAnnotation(RequireBlock.class),
                    method.getAnnotation(ShieldBypass.class),
                    player);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean processAll(RegionCheck check, RegionLimit limit, RegionRole role,
                               RequireWilderness wilderness, RegionProvider provider,
                               RegionBlacklist blacklist, RequireBlock requireBlock, ShieldBypass bypass,
                               Player player) {

        // --- 1. @ShieldBypass ---
        if (bypass != null && player.hasPermission(bypass.value())) {
            return true;
        }

        // --- 2. @RequireBlock ---
        if (requireBlock != null) {
            Block block;
            if (requireBlock.checkGround()) {
                block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            } else {
                block = player.getLocation().getBlock();
            }

            boolean match = false;
            for (Material mat : requireBlock.value()) {
                if (block.getType() == mat) {
                    match = true;
                    break;
                }
            }
            if (!match) return false;
        }

        RegionInfo info;

        // --- 3. @RegionProvider ---
        if (provider != null) {
            info = manager.getRegionInfo(provider.value(), player.getLocation());
            if (info == null) return false;

        } else {
            info = manager.getRegionInfo(player.getLocation());
        }

        // --- 4. @RegionBlacklist ---
        if (blacklist != null && info != null) {
            if (blacklist.provider().isEmpty() || info.getProvider().equalsIgnoreCase(blacklist.provider())) {
                for (String bannedId : blacklist.ids()) {
                    if (info.getId().equalsIgnoreCase(bannedId)) {
                        return false;
                    }
                }
            }
        }

        // --- 5. @RequireWilderness ---
        if (wilderness != null) {
            return info == null;
        }

        // --- 6. @RegionCheck ---
        if (check != null) {
            if (!check.bypassPerm().isEmpty() && player.hasPermission(check.bypassPerm())) {
            } else {
                ShieldResponse response = manager.checkResult(player, player.getLocation(), check.type());
                if (response.isDenied()) {
                    return false;
                }
            }
        }

        if (info == null) {
            return limit == null && role == null;
        }

        // --- 7. @RegionLimit ---
        if (limit != null) {
            if (!info.getId().equalsIgnoreCase(limit.id())) {
                return false;
            }
            if (!limit.provider().isEmpty() && !info.getProvider().equalsIgnoreCase(limit.provider())) {
                return false;
            }
        }

        // --- 8. @RegionRole ---
        if (role != null) {
            boolean authorized = false;
            UUID uuid = player.getUniqueId();

            switch (role.value()) {
                case OWNER:
                    authorized = info.getOwners().contains(uuid);
                    break;
                case MEMBER_OR_OWNER:
                    authorized = info.getOwners().contains(uuid) || info.getMembers().contains(uuid);
                    break;
                case VISITOR:
                    authorized = true;
                    break;
            }

            return authorized;
        }

        return true;
    }
}