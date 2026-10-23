package com.jttw.engine.entity.cave;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A creature of the Bottomless Cave: a {@link ZoneMonster} of the cave, about level 37, its
 * captains about 44. Lady Earth Flow's household: her mouse kin and her flower maids.
 */
public abstract class CaveMonster extends ZoneMonster {

    /** The level a regular one is about, and a captain. */
    public static final int LEVEL = 37, CAPTAIN_LEVEL = LEVEL + CAPTAIN_OVER;

    protected CaveMonster(EntityType<? extends CaveMonster> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.BOTTOMLESS;
    }
}
