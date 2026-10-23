package com.jttw.engine.world.feature;

import com.jttw.engine.world.Zones;
import com.jttw.engine.world.waters.DragonPalace;
import com.jttw.engine.world.waters.Sea;
import com.jttw.engine.world.waters.TrenchShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import javax.annotation.Nullable;

/**
 * Sinks the Mystical Waters' trench into the deep ocean it takes over, as deep as the world goes:
 * its floor lies {@value #FLOOR_OVER_BOTTOM} blocks over the world's bottom, just over the bedrock
 * (y -58 in the overworld), some 120 blocks under the waves.
 * <ul>
 *   <li><b>Its walls</b> drop near-sheer from the sea floor, a few blocks in from the region's edge,
 *       their line wandering, broken by ledges every dozen blocks or so;</li>
 *   <li><b>its floor</b> is sand and gravel and clay, rolling in low mounds, laid level round the
 *       palace at the region's heart;</li>
 *   <li><b>the palace</b> of the Dragon King whose sea lies that way ({@link Sea#of}), and the road of
 *       lanterns from its gate, are laid here column by column ({@link DragonPalace}), so they come
 *       out whole across the chunks they span.</li>
 * </ul>
 * The trench is worked out from the region (its depth into it, its centre) and the seed alone, so
 * every chunk agrees on it whichever is made first. The sea fills it; the caves under its walls and
 * floor are filled in, so no pocket of air is left against the water. It runs first among the
 * zone's features; later ones find its floor with the live heightmaps ({@code OCEAN_FLOOR}).
 */
public class TrenchFeature extends Feature<NoneFeatureConfiguration> {

    /** The zone it sinks. */
    public static final Zones.Zone ZONE = TrenchShape.ZONE;

    public TrenchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The trench floor's top over a world whose bottom is {@code minY}, before its mounds. */
    public static int floorY(int minY) {
        return TrenchShape.floorY(minY);
    }

    /** The throne of the palace in the region holding (x, z), on the floor's top; null where there is none. */
    @Nullable
    public static BlockPos throne(long seed, int x, int z, int minY) {
        return TrenchShape.throne(seed, x, z, minY);
    }

    private static int roll(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    /** The floor's top: mostly sand, then gravel, some clay. */
    private static BlockState floorTop(int x, int z) {
        int r = roll(x >> 1, z >> 1, 5);
        return r < 55 ? Blocks.SAND.defaultBlockState() : r < 85 ? Blocks.GRAVEL.defaultBlockState() : Blocks.CLAY.defaultBlockState();
    }

    private static boolean open(BlockState s) {
        return s.isAir() || !s.getFluidState().isEmpty();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int minY = level.getMinBuildHeight(), sea = level.getSeaLevel();
        BlockPos throne = throne(seed, x0 + 8, z0 + 8, minY); // a chunk lies in one cell
        if (throne == null) return false;
        Sea which = Sea.of(throne.getX(), throne.getZ());
        BlockState water = Blocks.WATER.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (ZONE.depth(seed, x, z) <= 0) continue;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (!ZONE.isAt(level, p.set(x, top, z))) continue; // its host was not found here
                int ground = TrenchShape.groundAt(seed, x, z, top, minY);
                if (ground >= top) continue;
                // the ground under it made whole: no cave, no lava, under the trench
                for (int y = Math.max(minY + 1, ground - 6); y < ground; y++) {
                    if (open(level.getBlockState(p.set(x, y, z)))) level.setBlock(p, Blocks.DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                level.setBlock(p.set(x, ground, z), floorTop(x, z), Block.UPDATE_CLIENTS);
                // and the sea let in over it (above the sea's level, where the trench cuts into land, air)
                for (int y = ground + 1; y <= top; y++) level.setBlock(p.set(x, y, z), y < sea ? water : air, Block.UPDATE_CLIENTS);
                any = true;
                // the palace, and its road
                int[] l = DragonPalace.toLocal(which, x - throne.getX(), z - throne.getZ());
                if (DragonPalace.covers(l[0], l[1])) {
                    DragonPalace.layColumn(level, throne, which, x, z, context.random());
                } else if (DragonPalace.onRoad(l[0], l[1])) {
                    road(level, p, x, ground, z, l);
                }
            }
        }
        return any;
    }

    /** The road out of the palace's gate: prismarine edged dark, lantern posts along it every eight blocks. */
    private static void road(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int ground, int z, int[] local) {
        int ax = Math.abs(local[0]);
        if (ax <= DragonPalace.ROAD_HALF) {
            level.setBlock(p.set(x, ground, z), (ax == DragonPalace.ROAD_HALF ? Blocks.DARK_PRISMARINE : Blocks.PRISMARINE_BRICKS).defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (Math.floorMod(local[1] - DragonPalace.MAX_Z, 8) == 4) {
            for (int up = 1; up <= 4; up++) {
                level.setBlock(p.set(x, ground + up, z), (up == 4 ? Blocks.SEA_LANTERN : Blocks.DARK_PRISMARINE).defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }
}
