package com.jttw.engine.world.feature;

import com.jttw.engine.entity.ridge.LadyWhiteBoneEntity;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Lady White Bone's lair, at the heart of each region of the ridge, on the round floor the massif
 * levels there: bone pillars crowned with skulls round the edge, a dais of bone in the middle, and
 * she herself, put there once, when the land is made. There is one of her to each region; killed,
 * she does not come back.
 *
 * <p>Built only in the chunk that holds the centre; it puts nothing at or under the floor, which
 * another chunk's massif may still be laying.
 */
public class LairFeature extends Feature<NoneFeatureConfiguration> {

    private static final int RADIUS = BoneMassifFeature.LAIR_RADIUS;

    public LairFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[] c = Zones.WHITE_BONE.layout().centre(seed, x0 + 8, z0 + 8);
        if (c == null || c[0] < x0 || c[0] >= x0 + 16 || c[1] < z0 || c[1] >= z0 + 16) return false;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, c[0], c[1]);
        BlockPos centre = new BlockPos(c[0], y, c[1]);
        if (!level.getBiome(centre).is(Zones.WHITE_BONE_RIDGE)) return false;

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // six pillars round the edge, each with a skull looking in
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            int x = c[0] + (int) Math.round(Math.cos(a) * (RADIUS - 1)), z = c[1] + (int) Math.round(Math.sin(a) * (RADIUS - 1));
            for (int up = 0; up < 4; up++) level.setBlock(p.set(x, y + up, z), com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            int facing = Math.floorMod(Math.round((float) Math.toDegrees(Math.atan2(c[0] - x, z - c[1])) / 22.5f), 16);
            level.setBlock(p.set(x, y + 4, z), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, facing), Block.UPDATE_CLIENTS);
        }
        // a dais of bone laid crosswise, and she stands on it
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            level.setBlock(p.set(c[0] + dx, y, c[1] + dz), com.jttw.engine.registry.ModBlocks.OSSIFIED_BONE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), Block.UPDATE_CLIENTS);
        }
        LadyWhiteBoneEntity lady = ModEntities.LADY_WHITE_BONE.get().create(level.getLevel());
        if (lady != null) {
            lady.moveTo(c[0] + 0.5, y + 1, c[1] + 0.5, context.random().nextFloat() * 360f, 0f);
            lady.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), MobSpawnType.STRUCTURE, null, null);
            level.addFreshEntityWithPassengers(lady);
        }
        return true;
    }
}
