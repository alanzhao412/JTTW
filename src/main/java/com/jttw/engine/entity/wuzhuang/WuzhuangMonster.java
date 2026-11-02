package com.jttw.engine.entity.wuzhuang;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A creature of Mount Wuzhuang: a {@link ZoneMonster} of its band (levels 78-88), the temple's
 * disciples and the mountain's cloud cranes: of the island's edge's level to the band's top at the
 * temple in its middle, its captains three to five over the regulars round them (never more than one
 * over the band's top).
 */
public abstract class WuzhuangMonster extends ZoneMonster {

    protected WuzhuangMonster(EntityType<? extends WuzhuangMonster> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.MOUNT_WUZHUANG;
    }
}
