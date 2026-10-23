package com.jttw.engine.world.feature;

import com.jttw.engine.world.Zones;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;

/**
 * A zone's boss lair, at the heart of each of its regions, and the boss put there once, when the
 * land is made: the feature runs in the one chunk that holds the region's centre, and a chunk is
 * made once, so there is one boss to each region; killed, it does not come back.
 *
 * <p>A subclass names its {@link #zone()} and {@link #boss()}, and {@link #build}s the lair; it may
 * move the lair's floor ({@link #floorAt}: the surface, unless it says otherwise; a cave's or a
 * floating island's would be elsewhere). Nothing is built where the zone's biome is not (its host
 * was not found there, or the floor is outside the heights the zone takes).
 */
public abstract class ZoneLairFeature extends Feature<NoneFeatureConfiguration> {

    protected ZoneLairFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Its zone. */
    protected abstract Zones.Zone zone();

    /** The boss put in it. */
    protected abstract EntityType<? extends Mob> boss();

    /** The boss put in the lair round {@code centre}: {@link #boss()}, unless a zone's lairs differ by region. */
    protected EntityType<? extends Mob> boss(WorldGenLevel level, BlockPos centre) {
        return boss();
    }

    /** Builds the lair round {@code centre}, the first block over its floor, where the boss will stand. */
    protected abstract void build(WorldGenLevel level, BlockPos centre, RandomSource random);

    /** The first block over the lair's floor at (x, z): the surface, unless a subclass says otherwise. */
    protected int floorAt(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[] c = zone().centreInChunk(level.getSeed(), x0, z0);
        if (c == null) return false;
        BlockPos centre = new BlockPos(c[0], floorAt(level, c[0], c[1]), c[1]);
        if (!zone().isAt(level, centre)) return false;
        build(level, centre, context.random());
        placeBoss(level, boss(level, centre), centre.above(), context.random());
        return true;
    }

    /** Puts a boss at {@code at}, facing anywhere, as a structure's (so it takes its lair there). */
    @Nullable
    public static <T extends Mob> T placeBoss(WorldGenLevel level, EntityType<T> type, BlockPos at, RandomSource random) {
        T boss = type.create(level.getLevel());
        if (boss == null) return null;
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(boss);
        return boss;
    }
}
