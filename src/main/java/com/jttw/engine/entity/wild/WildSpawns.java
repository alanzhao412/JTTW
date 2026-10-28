package com.jttw.engine.entity.wild;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Where and when the open world's yaoguai come of themselves. Which biomes they come in is data:
 * each kind's biome modifier ({@code data/jttw_engine/neoforge/biome_modifier/}) adds it to the
 * monster spawns of the vanilla biomes in its tag ({@code tags/worldgen/biome/<kind>_haunts.json}),
 * so a zone, whose biome takes the ground's place, never has them. These rules, registered as their
 * spawn placements (ModEntities), say the rest, over a monster's usual (dark enough, not peaceful):
 *
 * <ul>
 *   <li>goblins, wolfmen and ogres: on the open surface (no roof over them but leaves) and only at
 *       night;</li>
 *   <li>kobolds: underground only, under a roof of rock, at least {@value #KOBOLD_DEPTH} blocks
 *       below the sea.</li>
 * </ul>
 *
 * Anything else (an egg, a spawner, a command) brings one anywhere.
 */
public final class WildSpawns {

    private WildSpawns() {}

    /** How far below sea level a kobold's cave must be. */
    public static final int KOBOLD_DEPTH = 8;

    private static boolean ofItself(MobSpawnType reason) {
        return reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION;
    }

    /** Whether {@code pos} is on the open surface: nothing over it but sky (or a forest's leaves). */
    public static boolean onSurface(ServerLevelAccessor level, BlockPos pos) {
        return pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 1;
    }

    /** Whether it is night where {@code level} is. */
    public static boolean night(ServerLevelAccessor level) {
        return level.getLevel().isNight();
    }

    /** Goblins, wolfmen, ogres: on the surface, by night. */
    public static boolean surfaceAtNight(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) return false;
        return !ofItself(reason) || (onSurface(level, pos) && night(level));
    }

    /** Kobolds: in the caves, under rock, well below the sea. */
    public static boolean underground(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) return false;
        return !ofItself(reason) || (!onSurface(level, pos) && pos.getY() <= level.getLevel().getSeaLevel() - KOBOLD_DEPTH);
    }
}
