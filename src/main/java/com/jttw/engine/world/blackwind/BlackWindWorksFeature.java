package com.jttw.engine.world.blackwind;

import com.jttw.engine.entity.blackwind.BlackWindMonster;
import com.jttw.engine.registry.BlackWindRegistry;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.ZoneTerrain;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Black Wind Mountain's works, once the pines have grown on the range ({@link BlackWindRangeFeature}):
 * <ul>
 *   <li>the valley road kept clear: no pine stands on the valley's floor, the forecourt or the road;</li>
 *   <li><b>the burnt Guanyin Chan Temple</b> on its terrace ({@link TemplePlan}), and its band put there
 *       once: a boar-demon captain and his scavengers picking over the ruin;</li>
 *   <li><b>the watch-camps</b> ({@link BlackWindShape#camps}): a stockade of spruce stakes round a fire, a
 *       black banner on its pole, lean-tos of dark cloth; each with its band put there once, a captain
 *       and his followers (kept; about one band in three roams, a patrol).</li>
 * </ul>
 */
public class BlackWindWorksFeature extends Feature<NoneFeatureConfiguration> {

    public static final Zones.Zone ZONE = BlackWindShape.ZONE;

    public BlackWindWorksFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 1000L);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[] c = ZONE.centre(seed, x0 + 8, z0 + 8);
        if (c == null) return false;
        BlackWindShape.Floors f = BlackWindRangeFeature.floors(level, c[0], c[1]);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz, lx = x - c[0], lz = z - c[1];
            if (ZONE.depth(seed, x, z) <= 0) continue;
            int tz = lz - TemplePlan.TEMPLE_Z;
            if (TemplePlan.covers(lx, tz)) {
                // the temple's ruin, its share in this column
                int floor = f.terrace();
                for (int y = TemplePlan.Y_MIN; y <= TemplePlan.Y_MAX; y++) {
                    BlockState st = TemplePlan.blockAt(lx, y, tz, x, floor + y, z);
                    if (st != null) level.setBlock(p.set(x, floor + y, z), st, Block.UPDATE_CLIENTS);
                }
                for (int y = floor + TemplePlan.Y_MAX + 1; y <= floor + TemplePlan.Y_MAX + 16; y++) clearTree(level, p.set(x, y, z));
                any = true;
                continue;
            }
            if (BlackWindShape.onValleyFloor(lx, lz) && !BlackWindPlan.covers(lx, lz) || (lz > BlackWindPlan.FACE_Z && BlackWindShape.onValleyFloor(lx, lz))) {
                // no pine stands in the valley's way
                int floor = (int) Math.round(BlackWindShape.floorAt(f, lz));
                for (int y = floor + 1; y <= floor + 34; y++) {
                    if (BlackWindPlan.covers(lx, lz) && BlackWindPlan.code(lx, y - f.court(), lz) != BlackWindPlan.UNTOUCHED) continue;
                    clearTree(level, p.set(x, y, z));
                }
                any = true;
            }
        }
        // the temple's band, from the chunk holding its courtyard
        int bx = c[0] + TemplePlan.COURTYARD[0], bz = c[1] + TemplePlan.TEMPLE_Z + TemplePlan.COURTYARD[2];
        if ((bx >> 4) == (x0 >> 4) && (bz >> 4) == (z0 >> 4)) {
            band(level, bx, f.terrace() + 1, bz, 5, 20, true, context.random());
            any = true;
        }
        any |= camps(level, seed, c, x0, z0, context.random());
        return any;
    }

    /** Takes away a tree's log or leaves (anything the pines put there) at {@code p}. */
    private static void clearTree(WorldGenLevel level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        if (st.is(BlockTags.LOGS) || st.is(BlockTags.LEAVES) || st.is(Blocks.PODZOL) || st.is(Blocks.LARGE_FERN) || st.is(Blocks.FERN)
                || st.is(Blocks.SHORT_GRASS) || st.is(Blocks.TALL_GRASS) || st.is(Blocks.SWEET_BERRY_BUSH) || st.is(Blocks.MOSSY_COBBLESTONE)
                || st.is(Blocks.BROWN_MUSHROOM) || st.is(Blocks.RED_MUSHROOM)) {
            level.setBlock(p, s(Blocks.AIR), Block.UPDATE_CLIENTS);
        }
    }

    // ── The camps ───────────────────────────────────────────────────────

    private static boolean camps(WorldGenLevel level, long seed, int[] c, int x0, int z0, RandomSource random) {
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        DensityFunction shape = null;
        for (BlackWindShape.Camp o : BlackWindShape.camps(seed, c[0], c[1])) {
            int r = BlackWindShape.CAMP_REACH;
            if (o.x() + r < x0 || o.x() - r > x0 + 15 || o.z() + r < z0 || o.z() - r > z0 + 15) continue;
            if (shape == null) shape = ZoneTerrain.broadShape(level);
            int base = campBase(level, shape, seed, o);
            for (int x = Math.max(x0, o.x() - r); x <= Math.min(x0 + 15, o.x() + r); x++) {
                for (int z = Math.max(z0, o.z() - r); z <= Math.min(z0 + 15, o.z() + r); z++) {
                    int dx = x - o.x(), dz = z - o.z();
                    if (dx * dx + dz * dz > (r + 0.5) * (r + 0.5)) continue;
                    if (!ZONE.isAt(level, p.set(x, base, z))) continue;
                    int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    for (int y = base - 1; y >= base - 16; y--) {
                        if (y <= ground && level.getBlockState(p.set(x, y, z)).isSolid() && !level.getBlockState(p).is(BlockTags.LEAVES)) break;
                        level.setBlock(p.set(x, y, z), y == base - 1 ? s(Blocks.DIRT) : BlackWindBlocks.rock(y, x, z), Block.UPDATE_CLIENTS);
                    }
                    for (int dy = 0; dy <= 9; dy++) {
                        BlockState st = campAt(dx, dy, dz, x, z);
                        level.setBlock(p.set(x, base + dy, z), st == null ? s(Blocks.AIR) : st, Block.UPDATE_CLIENTS);
                    }
                    for (int y = base + 10; y <= base + 30; y++) clearTree(level, p.set(x, y, z));
                }
            }
            any = true;
            if ((o.x() >> 4) == (x0 >> 4) && (o.z() >> 4) == (z0 >> 4)) band(level, o.x(), base + 1, o.z(), 4 + hash(o.x(), o.z(), 3) % 2,
                    o.patrol() ? 40 : 14, false, random);
        }
        return any;
    }

    /** The level of a camp's ground: the land and the range over it at its middle, the same from every chunk. */
    public static int campBase(WorldGenLevel level, DensityFunction shape, long seed, BlackWindShape.Camp o) {
        int land = Math.max(level.getSeaLevel(), ZoneTerrain.landAt(shape, o.x(), o.z(), level.getMinBuildHeight(), ZoneTerrain.landTop(level)));
        return Math.min(level.getMaxBuildHeight() - 30, land + BlackWindShape.riseAt(seed, o.x(), o.z()));
    }

    /** What a camp holds at (dx, dy, dz) from its middle (dy 0 its ground); null where it is open. */
    @Nullable
    public static BlockState campAt(int dx, int dy, int dz, int wx, int wz) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        double r = Math.sqrt(dx * dx + dz * dz);
        if (dy == 0) {
            if (r < 2.5) return s(Blocks.COBBLESTONE);
            return hash(wx, wz, 5) < 450 ? s(Blocks.COARSE_DIRT) : hash(wx, wz, 6) < 700 ? s(Blocks.PODZOL) : s(Blocks.GRAVEL);
        }
        // the fire in its ring of stones
        if (dx == 0 && dz == 0 && dy == 1) return s(Blocks.CAMPFIRE).setValue(CampfireBlock.SIGNAL_FIRE, true);
        // the stockade: stakes of spruce round it, sharpened (fences on top), a gap to the south
        if (r > 5.6 && r <= 6.8 && !(dz > 4 && ax <= 1)) {
            int high = 2 + (int) Math.floorMod(Mth.getSeed(wx, 7, wz), 2L);
            if (dy <= high) return s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            if (dy == high + 1) return s(Blocks.SPRUCE_FENCE);
            return null;
        }
        // the black banner on its pole (west of the fire)
        if (dx == -3 && dz == -3) {
            if (dy <= 7) return s(Blocks.DARK_OAK_FENCE).setValue(CrossCollisionBlock.EAST, dy >= 6);
            return null;
        }
        if (dz == -3 && (dx == -2 || dx == -1) && dy >= 4 && dy <= 6) return dy == 5 && dx == -1 ? s(Blocks.RED_WOOL) : s(Blocks.BLACK_WOOL);
        if (dz == -3 && (dx == -2 || dx == -1) && dy == 7) return dx == -2 ? s(Blocks.DARK_OAK_FENCE).setValue(CrossCollisionBlock.WEST, true).setValue(CrossCollisionBlock.EAST, true) : null;
        // two lean-tos of black cloth on dark oak, north and east
        if (dz >= -4 && dz <= -2 && dx >= 1 && dx <= 4) {
            if (dy == 1 && dz == -4) return s(Blocks.BLACK_WOOL);
            if (dy == 2 && dz == -4) return s(Blocks.DARK_OAK_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);
            if (dy == 1 && dz == -3 && dx == 2) return s(Blocks.HAY_BLOCK);
            if (dy == 1 && dz == -2 && dx == 4) return s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP);
            return null;
        }
        if (dx >= 2 && dx <= 4 && dz >= 1 && dz <= 3) {
            if (dy == 1 && dx == 4) return s(Blocks.BLACK_WOOL);
            if (dy == 2 && dx == 4) return s(Blocks.DARK_OAK_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);
            if (dy == 1 && dx == 3 && dz == 2) return s(Blocks.HAY_BLOCK);
            return null;
        }
        // a rack of spears and a chopping log
        if (dx == -4 && dz >= 0 && dz <= 2 && dy == 1) return s(Blocks.SPRUCE_FENCE).setValue(CrossCollisionBlock.NORTH, dz > 0).setValue(CrossCollisionBlock.SOUTH, dz < 2);
        if (dx == -2 && dz == 3 && dy == 1) return s(Blocks.SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        return null;
    }

    /**
     * A band put once and kept, round (x, y, z): its first a captain, the rest regulars of the zone's three
     * kinds (the temple's boars and imps, the camps' any); held within {@code roam} of its middle.
     */
    private static void band(WorldGenLevel level, int x, int y, int z, int n, int roam, boolean temple, RandomSource random) {
        long h = Mth.getSeed(x, 43, z);
        List<EntityType<? extends BlackWindMonster>> kinds = temple
                ? List.of(BlackWindRegistry.BOAR_DEMON.get(), BlackWindRegistry.WIND_IMP.get(), BlackWindRegistry.WIND_IMP.get())
                : List.of(BlackWindRegistry.WIND_IMP.get(), BlackWindRegistry.BOAR_DEMON.get(), BlackWindRegistry.BEAR_SOLDIER.get());
        for (int i = 0; i < n; i++) {
            boolean captain = i == 0;
            EntityType<? extends BlackWindMonster> type = kinds.get((int) Math.floorMod(h + i, (long) kinds.size()));
            if (captain && temple) type = BlackWindRegistry.BOAR_DEMON.get();
            BlackWindMonster m = type.create(level.getLevel());
            if (m == null) continue;
            double a = i * Math.PI * 2 / n + 0.4;
            int mx = x + (int) Math.round(Math.cos(a) * 3), mz = z + (int) Math.round(Math.sin(a) * 3);
            BlockPos at = com.jttw.engine.entity.zone.ZoneMonster.roomNear(level, type, new BlockPos(mx, y, mz), 2); // clear of the camp's props
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.STRUCTURE, null);
            m.become(captain ? m.rollCaptainLevel(random) : m.rollRegularLevel(random), captain);
            m.setPersistenceRequired();
            m.restrictTo(new BlockPos(x, y, z), roam);
            com.jttw.engine.world.ZonePosts.keep(m); // the band comes back, 20 minutes after its last falls
            level.addFreshEntityWithPassengers(m);
        }
    }
}
