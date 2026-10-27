package com.jttw.engine.world.ridge;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Where the ridge's dead come of themselves: anywhere on the ridge, in any light (it swarms with
 * them), but never inside the White Bone Cave, whose floors hold the garrison laid for them and
 * no more ({@link CaveGarrison}, {@link CaveKeeper}).
 */
public final class RidgeRules {

    private RidgeRules() {}

    public static boolean spawnRule(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) return false;
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return true;
        WhiteBoneCave.Site site = CaveSites.cached(level.getLevel().getSeed());
        return site == null || CaveKeeper.floorOf(site, pos) < 0;
    }
}
