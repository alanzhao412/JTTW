package com.jttw.engine.entity.fire;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * A creature of the Flaming Mountains: a {@link ZoneMonster} of the fire, about level 67, its
 * captains about 74. Born of the mountains' fire, it is fire-proof (its type says so): the burning
 * earth, the fire on it and lava are nothing to it, and it walks through them as through air. Red
 * Boy's own Samadhi fire spares it (see SamadhiFire).
 */
public abstract class FireMonster extends ZoneMonster {

    /** The level a regular one is about, and a captain. */
    public static final int LEVEL = 67, CAPTAIN_LEVEL = LEVEL + CAPTAIN_OVER;

    protected FireMonster(EntityType<? extends FireMonster> type, Level level) {
        super(type, level);
        // fire is its element: it paths through it, and over the burning earth, as over stone
        setPathfindingMalus(PathType.DANGER_FIRE, 0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0f);
        setPathfindingMalus(PathType.LAVA, 8f);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.FIRE_MOUNTAIN;
    }

    @Override
    public int regularLevel() {
        return LEVEL;
    }

    @Override
    public int captainLevel() {
        return CAPTAIN_LEVEL;
    }

    /** Where it is kept (an outpost's band, a floor of the cave's garrison), kept with it: a creature's post is not saved by itself. */
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) {
            tag.put("Post", net.minecraft.nbt.NbtUtils.writeBlockPos(getRestrictCenter()));
            tag.putFloat("PostReach", getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        net.minecraft.nbt.NbtUtils.readBlockPos(tag, "Post").ifPresent(post -> restrictTo(post, (int) tag.getFloat("PostReach")));
    }

    /** The mountains' bands are led more often than most: one in eight of the world's own is a captain. */
    @Override
    public float captainChance() {
        return 0.125f;
    }
}
