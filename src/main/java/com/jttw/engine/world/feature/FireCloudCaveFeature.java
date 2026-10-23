package com.jttw.engine.world.feature;

import com.jttw.engine.entity.fire.StrongGeneralEntity;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fire.FireCloudCave;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Red Boy in his Fire Cloud Cave, at the heart of each region of the Flaming Mountains, put there
 * once, when the land is made, on the gilded dais of his inner hall; and his Six Strong Generals
 * (六健将) with him: the two Winds keeping the stone gate, the other four in the hall (kept, never
 * despawned). The cave itself is laid by the range, chunk by chunk ({@link FlamingRangeFeature},
 * {@link FireCloudCave}); the finding of the centre and the putting of the boss there are
 * {@link ZoneLairFeature}'s.
 */
public class FireCloudCaveFeature extends ZoneLairFeature {

    /** Where each of the six stands (local x, z: the gate toward +z), in the order of {@link StrongGeneralEntity#NAMES}. */
    public static final int[][] GENERALS = {
            {-6, -4}, {6, -4},                                          // Cloud-Soaring Sprite, Fog-Diving Monster: in the hall
            {-3, FireCloudCave.GATE_Z + 5}, {3, FireCloudCave.GATE_Z + 5}, // Mad-Hurrying Wind, Swift-Rushing Wind: at the gate
            {-6, 4}, {6, 4}};                                           // Flame-Head General, Eye-Spark Officer: in the hall

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

    /** The hall floor's top: he stands on his dais. */
    @Override
    protected int floorAt(WorldGenLevel level, int x, int z) {
        return FlamingRangeFeature.hallFloor(level, x, z);
    }

    /** His six generals at their posts. */
    @Override
    protected void build(WorldGenLevel level, BlockPos centre, RandomSource random) {
        for (int v = 0; v < GENERALS.length; v++) {
            StrongGeneralEntity g = ModEntities.STRONG_GENERAL.get().create(level.getLevel());
            if (g == null) continue;
            BlockPos at = centre.offset(GENERALS[v][0], 1, GENERALS[v][1]);
            g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
            g.setVariant(v);
            g.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            g.become(g.rollCaptainLevel(random), true);
            g.setPersistenceRequired();
            level.addFreshEntityWithPassengers(g);
        }
    }
}
