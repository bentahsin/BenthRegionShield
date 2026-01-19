package com.bentahsin.regionshield.hooks.towny;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionInfo;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Towny için sürüm bağımsız (Version-Agnostic) entegrasyon.
 * Towny'nin "PlayerCacheUtil" sınıfını kullanarak izinleri kontrol eder.
 * Bu sınıf, Towny'nin en optimize kontrol mekanizmasıdır.
 */
public class TownySafeHook implements IShieldHook {

    private Method getCachePermissionMethod;
    private Object actionBuild;
    private Object actionDestroy;
    private Object actionSwitch;
    private Object actionItemUse;

    private boolean initialized = false;

    @Override
    public String getName() {
        return "Towny";
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean canInitialize() {
        if (!ReflectionUtils.isPluginActive("Towny")) return false;

        try {
            Class<?> cacheUtilClass = ReflectionUtils.getClass("com.palmergames.bukkit.towny.utils.PlayerCacheUtil");
            Class<?> actionTypeClass = ReflectionUtils.getClass("com.palmergames.bukkit.towny.object.TownyPermission$ActionType");

            if (cacheUtilClass == null || actionTypeClass == null) {
                return false;
            }

            this.getCachePermissionMethod = ReflectionUtils.getMethod(
                    cacheUtilClass,
                    "getCachePermission",
                    Player.class, Location.class, Material.class, actionTypeClass
            );

            this.actionBuild = Enum.valueOf((Class<Enum>) actionTypeClass, "BUILD");
            this.actionDestroy = Enum.valueOf((Class<Enum>) actionTypeClass, "DESTROY");
            this.actionSwitch = Enum.valueOf((Class<Enum>) actionTypeClass, "SWITCH");
            this.actionItemUse = Enum.valueOf((Class<Enum>) actionTypeClass, "ITEM_USE");

            this.initialized = true;
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        if (!initialized || getCachePermissionMethod == null) return ShieldResponse.allow();

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
     * InteractionType -> Towny ActionType Dönüşümü
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

    @Override
    public RegionInfo getRegionInfo(Location location) {
        if (!initialized) return null;

        try {
            // TownyAPI (Modern) veya TownyUniverse (Eski) kullanımı
            // Basitlik adına Reflection kullanmadan modern API örneği veriyorum (Towny 0.96+):
            com.palmergames.bukkit.towny.object.TownBlock tb = com.palmergames.bukkit.towny.TownyAPI.getInstance().getTownBlock(location);

            if (tb == null || !tb.hasTown()) return null;

            com.palmergames.bukkit.towny.object.Town town = tb.getTown();

            List<UUID> residents = new ArrayList<>();
            for (com.palmergames.bukkit.towny.object.Resident res : town.getResidents()) {
                residents.add(res.getUUID());
            }

            List<UUID> owners = new ArrayList<>();
            if (town.getMayor() != null) {
                owners.add(town.getMayor().getUUID());
            }

            return RegionInfo.builder()
                    .id(town.getName())
                    .provider(getName())
                    .owners(owners)
                    .members(residents)
                    .build();

        } catch (Exception e) {
            return null;
        }
    }
}