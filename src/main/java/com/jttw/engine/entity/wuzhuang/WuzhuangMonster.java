package com.jttw.engine.entity.wuzhuang;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A creature of Mount Wuzhuang: a {@link ZoneMonster} of its band (levels 78-88), the temple's
 * disciples and the mountain's cloud cranes. Captains are never past the band's top.
 */
public abstract class WuzhuangMonster extends ZoneMonster {

    protected WuzhuangMonster(EntityType<? extends WuzhuangMonster> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.MOUNT_WUZHUANG;
    }

    @Override
    public int captainLevel() {
        return Math.min(zone().maxLevel(), super.captainLevel());
    }

    @Override
    public int rollCaptainLevel(RandomSource r) {
        return Math.min(zone().maxLevel(), super.rollCaptainLevel(r));
    }
}
