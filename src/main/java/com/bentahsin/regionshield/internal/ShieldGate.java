package com.bentahsin.regionshield.internal;

import com.bentahsin.regionshield.BenthRegionShield;
import com.bentahsin.regionshield.annotations.*;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.model.RegionInfo;
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

            if (!processAll(clazz.getAnnotation(RegionCheck.class),
                    clazz.getAnnotation(RegionLimit.class),
                    clazz.getAnnotation(RegionRole.class),
                    clazz.getAnnotation(RequireWilderness.class),
                    clazz.getAnnotation(RegionProvider.class),
                    player)) {
                return false;
            }

            if (!processAll(method.getAnnotation(RegionCheck.class),
                    method.getAnnotation(RegionLimit.class),
                    method.getAnnotation(RegionRole.class),
                    method.getAnnotation(RequireWilderness.class),
                    method.getAnnotation(RegionProvider.class),
                    player)) {
                return false;
            }

            return true;

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean processAll(RegionCheck check, RegionLimit limit, RegionRole role,
                               RequireWilderness wilderness, RegionProvider provider, Player player) {
        RegionInfo info;

        // --- 1. @RegionProvider ---
        if (provider != null) {
            info = manager.getRegionInfo(provider.value(), player.getLocation());
            if (info == null) return false;

        } else {
            info = manager.getRegionInfo(player.getLocation());
        }

        // --- 2. @RequireWilderness ---
        if (wilderness != null) {
            return info == null;
        }

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

        // --- 4. @RegionLimit ---
        if (limit != null) {
            if (!info.getId().equalsIgnoreCase(limit.id())) {
                return false;
            }
            if (!limit.provider().isEmpty() && !info.getProvider().equalsIgnoreCase(limit.provider())) {
                return false;
            }
        }

        // --- 5. @RegionRole ---
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