package com.jttw.engine.world.blackwind;

import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Where Black Wind Mountain's creatures come of themselves: anywhere on the mountain, in any light, but
 * never inside the Black Wind Cave (its garrison is mustered, no more: {@link BlackWindCave}) nor on the
 * valley's floor before its door.
 */
public final class BlackWindRules {

    private BlackWindRules() {}

    public static boolean spawnRule(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) return false;
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return true;
        int[] c = Zones.BLACK_WIND.centre(level.getLevel().getSeed(), pos.getX(), pos.getZ());
        if (c == null) return true;
        int lx = pos.getX() - c[0], lz = pos.getZ() - c[1];
        if (BlackWindPlan.covers(lx, lz) && lz <= BlackWindPlan.Z_MAX) return false;
        return true;
    }
}
