package com.jttw.engine.world.feature;

import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fire.FortressPlan;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Red Boy in his Fire Cloud Cave, at the heart of each region of the Flaming Mountains: put there
 * once, when the land is made, on the gilded dais of his throne room at the bottom of the cave,
 * under its eight floors. The cave itself is laid by the range, chunk by chunk
 * ({@link FlamingRangeFeature}, {@link FortressPlan}); its garrisons are mustered as the living come
 * to it ({@code FireFortress}); the finding of the centre and the putting of the boss there are
 * {@link ZoneLairFeature}'s.
 */
public class FireCloudCaveFeature extends ZoneLairFeature {

    public FireCloudCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected Zones.Zone zone() {
        return Zones.FIRE_MOUNTAIN;
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return ModEntities.RED_BOY.get();
    }

    /** The throne room's floor, under the door's level: he stands on his dais. */
    @Override
    protected int floorAt(WorldGenLevel level, int x, int z) {
        return FlamingRangeFeature.hallFloor(level, x, z) + FortressPlan.THRONE_Y;
    }

    /** Nothing more to build: the range has laid the throne room round him. */
    @Override
    protected void build(WorldGenLevel level, BlockPos centre, RandomSource random) {}
}
