package com.jttw.engine.world.feature;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fire.BurningEarthBlock;
import com.jttw.engine.world.fire.FireCloudCave;
import com.jttw.engine.world.fire.RangeShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Sets the Flaming Mountains alight, once {@link FlamingRangeFeature} has raised them:
 * <ul>
 *   <li><b>fire</b> on the burning earth in patches (about one block in eleven), which never goes out
 *       there, and creeps (see BurningEarthBlock);</li>
 *   <li><b>seams of lava</b> winding through the valleys, a block deep, in level ground only so they
 *       never run;</li>
 *   <li><b>smoke</b>: vents in the burning earth, a smouldering fire in a crack, a column of smoke
 *       going up from each (a campfire on hay, let into the ground);</li>
 *   <li><b>charred spires</b> of basalt on the high ground.</li>
 * </ul>
 * The cave and its forecourt are left as they are, and nothing stands in the gorge's way to them.
 * Nothing grows.
 */
public class FlameSeamsFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = RangeShape.ZONE;

    public FlameSeamsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 1000L);
    }

    /** Whether every neighbour of (x, top, z) holds the lava in: solid, and no lower. */
    private static boolean levelAround(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int top, int z) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getHeight(Heightmap.Types.WORLD_SURFACE, x + d.getStepX(), z + d.getStepZ()) - 1 < top) return false;
            BlockState n = level.getBlockState(p.set(x + d.getStepX(), top, z + d.getStepZ()));
            if (!n.isSolid() && !n.is(Blocks.LAVA)) return false;
        }
        return true;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[] c = ZONE.centre(seed, x0 + 8, z0 + 8);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz;
            if (ZONE.depth(seed, x, z) <= 0) continue;
            boolean way = false; // the gorge's floor: fire may lie on it, but nothing stands in the way
            if (c != null) {
                int lx = x - c[0], lz = z - c[1];
                if (FireCloudCave.covers(lx, lz) || RangeShape.onForecourt(lx, lz)) continue;
                way = RangeShape.onGorgeFloor(lx, lz);
            }
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            p.set(x, top, z);
            if (!ZONE.isAt(level, p)) continue;
            BlockState here = level.getBlockState(p);
            int roll = hash(x, z, 11);
            if (BurningEarthBlock.isLit(here)) {
                if (RangeShape.seam(seed, x, z) && levelAround(level, p, x, top, z)) {
                    level.setBlock(p.set(x, top, z), Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
                    level.setBlock(p.set(x, top - 1, z), Blocks.MAGMA_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                } else if (roll < 4 && !way) {
                    // a vent: a smouldering crack, smoke going up from it
                    level.setBlock(p.set(x, top - 1, z), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                    level.setBlock(p.set(x, top, z), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true), Block.UPDATE_CLIENTS);
                } else if (hash(Math.floorDiv(x, 3), Math.floorDiv(z, 3), 13) < 300 && hash(x, z, 17) < 300 && level.isEmptyBlock(p.set(x, top + 1, z))) {
                    level.setBlock(p, Blocks.FIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                any = true;
            } else if (roll < 3 && !way && (here.is(ModBlocks.FLAME_ROCK.get()) || here.is(ModBlocks.CINDER_ASH.get()))) {
                // a charred spire
                int high = 3 + hash(x, z, 19) % 6;
                for (int up = 1; up <= high; up++) {
                    level.setBlock(p.set(x, top + up, z), up == high ? Blocks.BLACKSTONE.defaultBlockState()
                            : Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), Block.UPDATE_CLIENTS);
                }
                any = true;
            }
        }
        return any;
    }
}
