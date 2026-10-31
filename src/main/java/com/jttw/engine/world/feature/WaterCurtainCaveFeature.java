package com.jttw.engine.world.feature;

import com.jttw.engine.registry.FlowerFruitRegistry;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.flowerfruit.CavePlan;
import com.jttw.engine.world.flowerfruit.IslandShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Six-Eared Macaque in his sanctum, at the back of the Water Curtain Cave: put there once, when
 * the land is made, in the middle of the sanctum's arena ({@link CavePlan#BOSS_X}, {@link CavePlan#BOSS_Z}
 * from the region's centre), which the island has laid round him ({@link FlowerFruitIslandFeature}).
 * Not at the region's centre, as most lairs' bosses are: the feature runs in the chunk that holds the
 * sanctum's middle.
 */
public class WaterCurtainCaveFeature extends ZoneLairFeature {

    public WaterCurtainCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected Zones.Zone zone() {
        return Zones.FLOWER_FRUIT;
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return FlowerFruitRegistry.SIX_EARED_MACAQUE.get();
    }

    /** The sanctum's floor. */
    @Override
    protected int floorAt(WorldGenLevel level, int x, int z) {
        return IslandShape.BASIN_Y;
    }

    @Override
    protected void build(WorldGenLevel level, BlockPos centre, RandomSource random) {}

    /** Where the Macaque stands in the region centred at {@code c}: the sanctum's middle, on its floor. */
    public static BlockPos bossAt(int[] c) {
        return new BlockPos(c[0] + CavePlan.BOSS_X, IslandShape.BASIN_Y + 1, c[1] + CavePlan.BOSS_Z);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        int[] c = zone().centre(level.getSeed(), x0 + 8, z0 + 8);
        if (c == null) return false;
        BlockPos at = bossAt(c);
        if ((at.getX() >> 4) != (x0 >> 4) || (at.getZ() >> 4) != (z0 >> 4)) return false;
        if (!zone().isAt(level, at)) return false;
        placeBoss(level, boss(), at, context.random());
        return true;
    }
}
