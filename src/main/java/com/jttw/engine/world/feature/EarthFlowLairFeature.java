package com.jttw.engine.world.feature;

import com.jttw.engine.entity.cave.FlowerMaidEntity;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Lady Earth Flow's lair: her palace at the bottom of the labyrinth, laid by {@link BottomlessCaveFeature}
 * with the rest (so no chunk made later undoes it). This puts her on her dais, once, when the land
 * is made, and two of her flower maids in attendance either side of it.
 */
public class EarthFlowLairFeature extends ZoneLairFeature {

    /** Where her maids stand, from her dais's middle. */
    public static final int[][] MAIDS = {{-3, 1}, {3, 1}};

    public EarthFlowLairFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected Zones.Zone zone() {
        return Zones.BOTTOMLESS;
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return ModEntities.LADY_EARTH_FLOW.get();
    }

    /** The palace floor, at the bottom of the labyrinth: her dais is the block over it. */
    @Override
    protected int floorAt(WorldGenLevel level, int x, int z) {
        return Labyrinth.FLOOR[Labyrinth.LEVELS - 1];
    }

    @Override
    protected void build(WorldGenLevel level, BlockPos centre, RandomSource random) {
        for (int[] m : MAIDS) {
            FlowerMaidEntity maid = ModEntities.FLOWER_MAID.get().create(level.getLevel());
            if (maid == null) continue;
            maid.moveTo(centre.getX() + m[0] + 0.5, centre.getY(), centre.getZ() + m[1] + 0.5, 180f, 0f);
            maid.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), MobSpawnType.STRUCTURE, null);
            maid.setPersistenceRequired();
            level.addFreshEntityWithPassengers(maid);
        }
    }
}
