package com.jttw.engine.world.feature;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fire.BurningEarthBlock;
import com.jttw.engine.entity.fire.FireMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.fire.FortressPlan;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;

/**
 * Sets the Flaming Mountains alight, once {@link FlamingRangeFeature} has raised them:
 * <ul>
 *   <li><b>fire</b> on the burning earth in patches (about one block in eleven), which never goes out
 *       there, and creeps (see BurningEarthBlock);</li>
 *   <li><b>seams of lava</b> winding through the valleys, a block deep, in level ground only so they
 *       never run;</li>
 *   <li><b>smoke</b>: vents in the burning earth, a smouldering fire in a crack, a column of smoke
 *       going up from each (a campfire on hay, let into the ground);</li>
 *   <li><b>charred spires</b> of basalt on the high ground;</li>
 *   <li><b>withered pines</b> (枯松) along the ravine's floor, bare and black;</li>
 *   <li><b>the outposts</b> ({@link RangeShape#outposts}): a watchtower of red brick in a ring of
 *       black stone, a signal fire smoking on top, braziers round it, and its band put there once,
 *       a captain and his followers (kept, never despawned; about one band in three roams the
 *       range round its tower, a patrol).</li>
 * </ul>
 * The door and its forecourt are left as they are, and nothing stands in the ravine's way to them.
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

    /** A withered pine (枯松) standing on (x, top, z): a bare black trunk, a few dead boughs. */
    private static void pine(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int top, int z) {
        int high = 6 + hash(x, z, 25) % 5;
        BlockState trunk = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        for (int up = 1; up <= high; up++) level.setBlock(p.set(x, top + up, z), trunk, Block.UPDATE_CLIENTS);
        for (int b = 0; b < 3; b++) {
            Direction d = Direction.from2DDataValue(hash(x, z, 27 + b) % 4);
            int at = top + high - 1 - b * 2;
            BlockState bough = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, d.getAxis());
            for (int len = 1; len <= 2 - b % 2; len++) {
                p.set(x + d.getStepX() * len, at, z + d.getStepZ() * len);
                if ((p.getX() >> 4) == (x >> 4) && (p.getZ() >> 4) == (z >> 4) && level.isEmptyBlock(p)) level.setBlock(p, bough, Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * Raises this chunk's share of the outposts near it, and puts each one's band there if its
     * middle is in this chunk. Returns whether it built any.
     */
    private static boolean outposts(WorldGenLevel level, long seed, int[] c, int x0, int z0, RandomSource random) {
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        DensityFunction shape = null;
        for (RangeShape.Outpost o : RangeShape.outposts(seed, c[0], c[1])) {
            int r = RangeShape.OUTPOST_REACH;
            if (o.x() + r < x0 || o.x() - r > x0 + 15 || o.z() + r < z0 || o.z() - r > z0 + 15) continue;
            if (shape == null) shape = ZoneTerrain.broadShape(level);
            int base = outpostBase(level, shape, seed, o);
            for (int x = Math.max(x0, o.x() - r); x <= Math.min(x0 + 15, o.x() + r); x++) {
                for (int z = Math.max(z0, o.z() - r); z <= Math.min(z0 + 15, o.z() + r); z++) {
                    int dx = x - o.x(), dz = z - o.z();
                    if (dx * dx + dz * dz > (r + 0.5) * (r + 0.5)) continue;
                    if (!ZONE.isAt(level, p.set(x, base, z))) continue;
                    // its foundation down to the ground, the ground over it cut away
                    int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    for (int y = base - 1; y >= base - 24; y--) {
                        if (y <= ground && level.getBlockState(p.set(x, y, z)).isSolid()) break;
                        level.setBlock(p.set(x, y, z), FlamingRangeFeature.inner(y, x, z), Block.UPDATE_CLIENTS);
                    }
                    for (int dy = 0; dy <= 14; dy++) {
                        BlockState st = outpostAt(dx, dy, dz);
                        level.setBlock(p.set(x, base + dy, z), st == null ? Blocks.AIR.defaultBlockState() : st, Block.UPDATE_CLIENTS);
                    }
                    for (int y = base + 15; y <= ground; y++) level.setBlock(p.set(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            any = true;
            if ((o.x() >> 4) == (x0 >> 4) && (o.z() >> 4) == (z0 >> 4)) band(level, o, base, random);
        }
        return any;
    }

    /** The level of an outpost's paving: the land and the range over it at its middle, the same from every chunk. */
    public static int outpostBase(WorldGenLevel level, DensityFunction shape, long seed, RangeShape.Outpost o) {
        int land = Math.max(level.getSeaLevel(), ZoneTerrain.landAt(shape, o.x(), o.z(), level.getMinBuildHeight(), ZoneTerrain.landTop(level)));
        return Math.min(level.getMaxBuildHeight() - 20, land + RangeShape.riseAt(seed, o.x(), o.z()));
    }

    /** What an outpost holds at (dx, dy, dz) from its middle (dy 0 its paving); null where it is open. */
    @Nullable
    public static BlockState outpostAt(int dx, int dy, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        double r = Math.sqrt(dx * dx + dz * dz);
        if (dy == 0) return ax <= 3 && az <= 3 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        // the tower, red brick with black corners, a crown of black stone and its signal fire
        if (ax <= 2 && az <= 2 && dy <= 9) {
            if (ax == 2 && az == 2) return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            if (dy == 9 || dy == 5) return Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
            return Blocks.RED_NETHER_BRICKS.defaultBlockState();
        }
        if (dy == 10 && ax <= 3 && az <= 3) return dx == 0 && dz == 0 ? Blocks.HAY_BLOCK.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        if (dy == 11 && dx == 0 && dz == 0) return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true);
        if (dy == 11 && (ax == 3 || az == 3) && ax <= 3 && az <= 3) return (ax + az) % 2 == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : null;
        // red hangings down the tower's faces
        if (dy >= 6 && dy <= 8 && ((ax == 3 && dz == 0) || (az == 3 && dx == 0))) return Blocks.RED_WOOL.defaultBlockState();
        // braziers at its corners
        if (ax == 4 && az == 4) return dy == 1 ? Blocks.MAGMA_BLOCK.defaultBlockState() : dy == 2 ? Blocks.CAMPFIRE.defaultBlockState() : null;
        // a ring of black stone round it, a gap at each side
        if (r > 5.2 && r <= 6.5 && dy <= 2 && ax > 1 && az > 1) return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        return null;
    }

    /** An outpost's band: a captain and three or four of his kind and the other, kept; a patrol roams. */
    private static void band(WorldGenLevel level, RangeShape.Outpost o, int base, RandomSource random) {
        long h = Mth.getSeed(o.x(), 41, o.z());
        boolean imps = Math.floorMod(h, 2L) == 0;
        int n = 4 + (int) Math.floorMod(h >> 8, 2L);
        int roam = o.patrol() ? 40 : 14;
        for (int i = 0; i < n; i++) {
            boolean captain = i == 0;
            EntityType<? extends FireMonster> type = (i % 3 == 2) == imps ? ModEntities.FURNACE_BRUTE.get() : ModEntities.EMBER_IMP.get();
            FireMonster m = type.create(level.getLevel());
            if (m == null) continue;
            double a = i * Math.PI * 2 / n + 0.4;
            int x = o.x() + (int) Math.round(Math.cos(a) * 4), z = o.z() + (int) Math.round(Math.sin(a) * 4);
            m.moveTo(x + 0.5, base + 1, z + 0.5, random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.STRUCTURE, null);
            m.become(captain ? m.rollCaptainLevel(random) : m.rollRegularLevel(random), captain);
            m.setPersistenceRequired();
            m.restrictTo(new BlockPos(o.x(), base + 1, o.z()), roam);
            level.addFreshEntityWithPassengers(m);
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[] c = ZONE.centre(seed, x0 + 8, z0 + 8);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = c != null && outposts(level, seed, c, x0, z0, context.random());
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz;
            if (ZONE.depth(seed, x, z) <= 0) continue;
            boolean way = false; // the gorge's floor: fire may lie on it, but nothing stands in the way
            if (c != null) {
                int lx = x - c[0], lz = z - c[1];
                if ((Math.abs(lx) <= 26 && lz >= FortressPlan.SHELL - 2 && lz <= FortressPlan.Z_MAX + 2) || RangeShape.onForecourt(lx, lz)) continue;
                if (RangeShape.outpostAt(seed, c[0], c[1], x, z) != null) continue;
                way = RangeShape.onGorgeFloor(lx, lz);
            }
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            p.set(x, top, z);
            if (!ZONE.isAt(level, p)) continue;
            BlockState here = level.getBlockState(p);
            int roll = hash(x, z, 11);
            if (way && c != null && Math.abs(x - c[0]) >= RangeShape.GORGE_HALF - 5 && hash(x, z, 23) < 7 && !BurningEarthBlock.isLit(here)) {
                // a withered pine at the ravine's side
                pine(level, p, x, top, z);
                any = true;
                continue;
            }
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
