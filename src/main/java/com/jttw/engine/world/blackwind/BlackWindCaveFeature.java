package com.jttw.engine.world.blackwind;

import com.jttw.engine.registry.BlackWindRegistry;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.ZoneLairFeature;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Black Bear Demon in his throne cavern, at the heart of Black Wind Mountain: put there once, when the
 * land is made, before his dais at the bottom of the Black Wind Cave (from the chunk that holds his post,
 * north of the region's centre). The cave itself is laid by the range ({@link BlackWindRangeFeature},
 * {@link BlackWindPlan}); its garrisons are mustered as the living come to it ({@link BlackWindCave}).
 */
public class BlackWindCaveFeature extends ZoneLairFeature {

    public BlackWindCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected Zones.Zone zone() {
        return Zones.BLACK_WIND;
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return BlackWindRegistry.BLACK_BEAR_DEMON.get();
    }

    @Override
    protected void build(WorldGenLevel level, BlockPos centre, RandomSource random) {}

    /** His post: before his dais, in the middle of the throne cavern's arena, from the chunk that holds it. */
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        int[] c = Zones.BLACK_WIND.centre(level.getSeed(), x0 + 8, z0 + 8);
        if (c == null) return false;
        int px = c[0], pz = c[1] + BlackWindPlan.THRONE_Z;
        if (px < x0 || px >= x0 + 16 || pz < z0 || pz >= z0 + 16) return false;
        int floor = BlackWindRangeFeature.floors(level, c[0], c[1]).court();
        BlockPos post = new BlockPos(px, floor + BlackWindPlan.THRONE_Y, pz);
        if (!Zones.BLACK_WIND.isAt(level, post)) return false;
        placeBoss(level, BlackWindRegistry.BLACK_BEAR_DEMON.get(), post, context.random());
        return true;
    }
}
