package com.bentahsin.regionshield.hooks.skyblock;

import com.bentahsin.regionshield.api.IShieldHook;
import com.bentahsin.regionshield.api.ShieldResponse;
import com.bentahsin.regionshield.internal.ReflectionUtils;
import com.bentahsin.regionshield.model.InteractionType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Optional;

public class BentoBoxHook implements IShieldHook {

    @Override
    public String getName() {
        return "BentoBox";
    }

    @Override
    public boolean canInitialize() {
        return ReflectionUtils.isPluginActive("BentoBox");
    }

    @Override
    public ShieldResponse check(Player player, Location location, InteractionType type) {
        Optional<Island> islandOpt = BentoBox.getInstance().getIslands().getIslandAt(location);

        if (!islandOpt.isPresent()) return ShieldResponse.allow();

        Island island = islandOpt.get();

        Flag flag = getBentoFlag(type);

        if (flag == null) {
            flag = BentoBox.getInstance().getFlagsManager().getFlag("PLACE").orElse(null);
        }

        if (flag == null) return ShieldResponse.allow();

        User user = User.getInstance(player);
        boolean allowed = island.isAllowed(user, flag);

        return allowed ? ShieldResponse.allow() : ShieldResponse.deny(getName());
    }

    /**
     * InteractionType -> Flag Nesnesi (İsim ile arama)
     */
    private Flag getBentoFlag(InteractionType type) {
        String flagName;

        switch (type) {
            case BLOCK_BREAK:
                flagName = "BREAK";
                break;
            case BLOCK_PLACE:
                flagName = "PLACE";
                break;
            case CONTAINER_ACCESS:
                flagName = "CONTAINER";
                break;
            case INTERACT:
                flagName = "DOOR";
                break;
            case PVP:
                flagName = "PVP";
                break;
            case MOB_DAMAGE:
                flagName = "HURT_ANIMALS";
                break;
            case TRAMPLE:
                flagName = "PLACE";
                break;
            case BUCKET_USE:
                flagName = "BUCKET";
                break;
            default:
                flagName = "PLACE";
                break;
        }

        return BentoBox.getInstance().getFlagsManager().getFlag(flagName).orElse(null);
    }
}