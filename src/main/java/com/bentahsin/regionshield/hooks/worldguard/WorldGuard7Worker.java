package com.bentahsin.regionshield.hooks.worldguard;

import com.bentahsin.regionshield.model.InteractionType;
import com.bentahsin.regionshield.model.RegionBounds;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * WorldGuard 7 ve üzeri sürümlerin API'si ile doğrudan etkileşim kuran {@link IWorldGuardWorker} arayüzünün bir uygulamasıdır.
 * <p>
 * Bu sınıf, WorldGuard'ın modern API'sini kullanarak izin kontrolleri ve bölge bilgisi sorgulamaları yapar.
 * Sürüm bağımlı tüm mantık burada merkezileştirilmiştir.
 */
public class WorldGuard7Worker implements IWorldGuardWorker {

    /**
     * WorldGuard 7 API'sini kullanarak bir oyuncunun belirli bir konumda bir eylemi gerçekleştirip gerçekleştiremeyeceğini kontrol eder.
     *
     * @param player   Kontrol edilecek oyuncu.
     * @param location Kontrol edilecek konum.
     * @param type     Gerçekleştirilecek eylemin türü ({@link InteractionType}).
     * @return Oyuncunun eylemi gerçekleştirmesine izin veriliyorsa true, aksi takdirde false.
     */
    @Override
    public boolean canBuild(Player player, Location location, InteractionType type) {
        com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        StateFlag flag = getFlag(type);

        return query.testState(weLoc, localPlayer, flag);
    }

    /**
     * RegionShield'ın dahili {@link InteractionType} enum'unu WorldGuard'ın ilgili {@link StateFlag} bayrağına eşler.
     * Bu metot, iki sistem arasında bir "çevirmen" görevi görür.
     *
     * @param type Çevrilecek etkileşim türü.
     * @return Karşılık gelen WorldGuard bayrağı. Eşleşme bulunamazsa varsayılan olarak {@code Flags.BUILD} döner.
     */
    private StateFlag getFlag(InteractionType type) {
        switch (type) {
            case BLOCK_BREAK:
                return Flags.BLOCK_BREAK;
            case BLOCK_PLACE:
                return Flags.BLOCK_PLACE;
            case INTERACT:
                return Flags.INTERACT;
            case CONTAINER_ACCESS:
                return Flags.CHEST_ACCESS;
            case PVP:
                return Flags.PVP;
            case MOB_DAMAGE:
                return Flags.MOB_DAMAGE;
            case BUCKET_USE:
                return Flags.BUILD;
            case TRAMPLE:
                return Flags.TRAMPLE_BLOCKS;
            case DAMAGE_ENTITY:
                return Flags.MOB_DAMAGE;
            default:
                return Flags.BUILD;
        }
    }

    /**
     * Belirtilen bir konumdaki en yüksek öncelikli WorldGuard bölgesinin fiziksel sınırlarını alır.
     *
     * @param location Sınırları alınacak bölgenin içindeki herhangi bir konum.
     * @return Bölgenin minimum ve maksimum köşe noktalarını içeren bir {@link RegionBounds} nesnesi.
     *         Eğer konumda bir bölge yoksa veya bir hata oluşursa null döner.
     */
    public RegionBounds getRegionBounds(Location location) {
        com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        com.sk89q.worldguard.protection.managers.RegionManager manager = container.get((com.sk89q.worldedit.world.World) weLoc.getExtent());

        if (manager == null) return null;

        com.sk89q.worldguard.protection.ApplicableRegionSet set = manager.getApplicableRegions(weLoc.toVector().toBlockPoint());

        if (set.size() == 0) return null;

        ProtectedRegion region = set.getRegions().iterator().next();

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        World world = location.getWorld();

        Location locMin = new Location(world, min.x(), min.y(), min.z());
        Location locMax = new Location(world, max.x(), max.y(), max.z());

        return new RegionBounds(locMin, locMax);
    }
}