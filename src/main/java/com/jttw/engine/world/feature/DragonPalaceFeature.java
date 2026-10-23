package com.jttw.engine.world.feature;

import com.jttw.engine.entity.waters.DragonKingEntity;
import com.jttw.engine.entity.waters.SeaMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.waters.DragonPalace;
import com.jttw.engine.world.waters.Sea;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Dragon King on his throne, at the heart of each region of the Mystical Waters: the king whose
 * sea lies that way ({@link Sea#of}), put there once, when the sea floor is made, with a guard of
 * his soldiers in his hall (kept, never despawned). The palace itself is laid by the trench, chunk
 * by chunk ({@link TrenchFeature}, {@link DragonPalace}); the finding of the centre and the putting
 * of the king there are {@link ZoneLairFeature}'s.
 */
public class DragonPalaceFeature extends ZoneLairFeature {

    public DragonPalaceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected Zones.Zone zone() {
        return Zones.MYSTICAL_WATERS;
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return ModEntities.AO_GUANG.get();
    }

    @Override
    protected EntityType<? extends Mob> boss(WorldGenLevel level, BlockPos centre) {
        return DragonKingEntity.typeOf(Sea.of(centre.getX(), centre.getZ()));
    }

    /** The top of his dais: he stands on it. */
    @Override
    protected int floorAt(WorldGenLevel level, int x, int z) {
        return TrenchFeature.floorY(level.getMinBuildHeight()) + DragonPalace.THRONE_Y - 1;
    }

    /** His guard in his hall: Shrimp Soldiers down its sides, a Crab General and a Yaksha at its gate. */
    @Override
    protected void build(WorldGenLevel level, BlockPos centre, RandomSource random) {
        Sea sea = Sea.of(centre.getX(), centre.getZ());
        BlockPos floor = centre.below(DragonPalace.THRONE_Y - 1); // the floor's top
        guard(level, ModEntities.SHRIMP_SOLDIER.get(), floor, sea, -3, 8, random);
        guard(level, ModEntities.SHRIMP_SOLDIER.get(), floor, sea, 3, 8, random);
        guard(level, ModEntities.SHRIMP_SOLDIER.get(), floor, sea, -3, 13, random);
        guard(level, ModEntities.SHRIMP_SOLDIER.get(), floor, sea, 3, 13, random);
        guard(level, ModEntities.CRAB_GENERAL.get(), floor, sea, 0, 17, random);
        guard(level, ModEntities.YAKSHA.get(), floor, sea, 0, 23, random);
    }

    private static void guard(WorldGenLevel level, EntityType<? extends SeaMonster> type, BlockPos floor, Sea sea, int x, int z, RandomSource random) {
        SeaMonster m = type.create(level.getLevel());
        if (m == null) return;
        BlockPos at = floor.offset(DragonPalace.toWorld(sea, x, 2, z));
        m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, sea.facing.toYRot(), 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        m.become(m.rollRegularLevel(random), false);
        m.setPersistenceRequired();
        level.addFreshEntityWithPassengers(m);
    }
}
