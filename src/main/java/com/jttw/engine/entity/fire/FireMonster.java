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
}
