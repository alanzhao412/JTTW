package com.jttw.engine.world.fire;

import com.jttw.engine.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import javax.annotation.Nullable;

/**
 * Red Boy's Fire Cloud Cave (火云洞) in Mount Haohao, at the heart of each region of the Flaming
 * Mountains (chapter 40: "a stone gate, and on it in large letters, Fire Cloud Cave, Mount Haohao"):
 * <ul>
 *   <li><b>the stone gate</b>: a facade of polished blackstone set in the mountain's flank at the head
 *       of the gorge, its gilded plaque over an arched way, red pillars and braziers before it;</li>
 *   <li>a <b>passage</b> into the mountain, lanterns hung along it;</li>
 *   <li><b>the inner hall</b>: a domed cavern of red brick round the region's centre, six pillars
 *       with fire-lamps, four pools of lava, a ring and a gilded dais in its floor where Red Boy
 *       stands, a red carpet to it, and behind it his throne between two braziers.</li>
 * </ul>
 * A blueprint, as the Dragon Kings' palaces are: what stands at each local place, from the place
 * alone, so each chunk of the range lays its own share ({@code FlamingRangeFeature}) and it comes out
 * whole whichever chunk is made first. Local y 0 is the hall floor's top block, (0, 0) the region's
 * centre, the gate toward +z.
 */
public final class FireCloudCave {

    private FireCloudCave() {}

    /** The hall's half-widths (x, z), its dome's height at its middle and at its edge, the rock round it. */
    public static final int HALL_RX = 12, HALL_RZ = 11, DOME = 11, DOME_EDGE = 4, SHELL = 3;
    /** The passage's half-width and height; where the gate's face stands (local z). */
    public static final int PASSAGE_HALF = 2, PASSAGE_HIGH = 5, GATE_Z = 21;
    /** The facade's half-width and height. */
    public static final int GATE_HALF = 7, GATE_HIGH = 10;
    /** Its footprint round the centre, and how far down and up it reaches. */
    public static final int MIN_X = -HALL_RX - SHELL, MAX_X = HALL_RX + SHELL, MIN_Z = -HALL_RZ - SHELL, MAX_Z = GATE_Z + 3;
    public static final int BOTTOM = -3, TOP = DOME + SHELL;
    /** Where the throne stands (local z), and the lava pools (their inner corners, mirrored to all four quarters). */
    public static final int THRONE_Z = -8, POOL_X = 7, POOL_Z = 5;

    public static boolean covers(int x, int z) {
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState rock() {
        return ModBlocks.FLAME_ROCK.get().defaultBlockState();
    }

    /** How far out in the hall local (x, z) is: 0 at the centre, 1 at its wall. */
    public static double hallRadius(int x, int z) {
        double a = x / (double) HALL_RX, b = z / (double) HALL_RZ;
        return Math.sqrt(a * a + b * b);
    }

    /** The dome's height over the floor at {@code r} (0..1) out. */
    public static int ceiling(double r) {
        return DOME_EDGE + (int) Math.round((DOME - DOME_EDGE) * Math.sqrt(Math.max(0, 1 - r * r)));
    }

    /** The passage's height at local x across it (lower at its sides: an arch). */
    private static int passageTop(int x) {
        return Math.abs(x) >= PASSAGE_HALF ? PASSAGE_HIGH - 1 : PASSAGE_HIGH;
    }

    private static boolean inPassage(int x, int z) {
        return Math.abs(x) <= PASSAGE_HALF && z >= HALL_RZ - 3 && z <= GATE_Z + 1;
    }

    /** Whether local (x, z) is one of the hall's six pillars (2x2 each, round it at 0.72 of the way out, clear of the way in and the throne). */
    public static boolean pillar(int x, int z) {
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            int px = (int) Math.floor(Math.cos(a) * HALL_RX * 0.72), pz = (int) Math.floor(Math.sin(a) * HALL_RZ * 0.72);
            if (x >= px && x <= px + 1 && z >= pz && z <= pz + 1) return true;
        }
        return false;
    }

    /** Whether local (x, z) is in one of the four lava pools. */
    public static boolean pool(int x, int z) {
        int ax = Math.abs(x), az = Math.abs(z);
        return ax >= POOL_X && ax <= POOL_X + 1 && az >= POOL_Z && az <= POOL_Z + 1;
    }

    /**
     * What stands at local (x, y, z): a block, air where the cave is open, or null where the
     * mountain is left as it is.
     */
    @Nullable
    public static BlockState at(int x, int y, int z) {
        if (!covers(x, z) || y < BOTTOM || y > TOP) return null;
        double r = hallRadius(x, z);
        if (r <= 1) return hall(x, y, z, r);
        if (z >= GATE_Z && z <= GATE_Z + 1 && Math.abs(x) <= GATE_HALF) return facade(x, y, z);
        if (z >= GATE_Z + 2) return porch(x, y, z);
        if (Math.abs(x) <= PASSAGE_HALF + 2 && z >= HALL_RZ - 3 && z < GATE_Z) return passage(x, y, z);
        double a = x / (double) (HALL_RX + SHELL), b = z / (double) (HALL_RZ + SHELL);
        if (a * a + b * b <= 1 && y <= DOME_EDGE + SHELL) return rock(); // the rock round the hall's foot
        return null;
    }

    private static BlockState hall(int x, int y, int z, double r) {
        if (y < 0) return pool(x, z) && y == -1 ? s(Blocks.MAGMA_BLOCK) : rock();
        if (y == 0) return floor(x, z, r);
        int ceil = ceiling(r);
        if (inPassage(x, z)) ceil = Math.max(ceil, passageTop(x));
        if (y > ceil) {
            if (y > ceil + SHELL) return null;
            // fire-lamps let into the dome
            if (y == ceil + 1 && r < 0.8 && Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) return s(Blocks.SHROOMLIGHT);
            return rock();
        }
        if (pillar(x, z)) return y == 4 ? s(Blocks.SHROOMLIGHT) : s(Blocks.RED_NETHER_BRICKS);
        if (y == 1) {
            // the throne, and a brazier either side of it
            if (x == 0 && z == THRONE_Z) return Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            if (Math.abs(x) == 1 && z == THRONE_Z) return Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            if (Math.abs(x) <= 1 && z == THRONE_Z - 1) return s(Blocks.GILDED_BLACKSTONE);
            if (Math.abs(x) == 4 && z == THRONE_Z) return Blocks.CAMPFIRE.defaultBlockState();
            // the carpet from the passage to the dais
            if (Math.abs(x) <= 1 && z >= 3 && z <= HALL_RZ) return s(Blocks.RED_CARPET);
        }
        if (y == 2 && x == 0 && z == THRONE_Z - 1) return s(Blocks.GILDED_BLACKSTONE); // the throne's back
        return s(Blocks.AIR);
    }

    private static BlockState floor(int x, int z, double r) {
        if (pool(x, z)) return s(Blocks.LAVA);
        if (Math.max(Math.abs(x), Math.abs(z)) <= 2) return (x + z) % 2 == 0 ? s(Blocks.GILDED_BLACKSTONE) : s(Blocks.POLISHED_BLACKSTONE);
        if (r > 0.58 && r < 0.68) return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        if (r > 0.92) return rock();
        return Math.floorMod(x * 7 + z * 3, 11) == 0 ? s(Blocks.CRACKED_NETHER_BRICKS) : s(Blocks.RED_NETHER_BRICKS);
    }

    private static BlockState passage(int x, int y, int z) {
        if (y < 0) return rock();
        boolean inside = Math.abs(x) <= PASSAGE_HALF;
        if (y == 0) return inside ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : rock();
        if (inside && y <= passageTop(x)) {
            if (x == 0 && y == PASSAGE_HIGH && Math.floorMod(z, 4) == 1) return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            return s(Blocks.AIR);
        }
        return y <= PASSAGE_HIGH + 2 ? (Math.abs(x) == PASSAGE_HALF + 1 && y <= PASSAGE_HIGH ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : rock()) : null;
    }

    private static BlockState facade(int x, int y, int z) {
        if (y < 0) return rock();
        int ax = Math.abs(x);
        if (ax <= PASSAGE_HALF && y >= 1 && y <= passageTop(x)) return s(Blocks.AIR);
        if (y > GATE_HIGH) return null;
        boolean front = z == GATE_Z + 1;
        if (front && y == 7 && ax <= 3) return s(Blocks.GILDED_BLACKSTONE);       // the plaque: 火云洞
        if (front && y == 7 && ax == 4) return s(Blocks.CHISELED_POLISHED_BLACKSTONE);
        if (y == GATE_HIGH) return s(Blocks.POLISHED_BLACKSTONE);
        if (front && ax == PASSAGE_HALF + 1 && y <= PASSAGE_HIGH + 1) return s(Blocks.CHISELED_POLISHED_BLACKSTONE); // the gate's jambs
        return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
    }

    private static BlockState porch(int x, int y, int z) {
        int ax = Math.abs(x);
        if (y < 0) return null;
        // two red pillars at the gate, and a brazier before each
        if (z == GATE_Z + 2 && ax == PASSAGE_HALF + 2) {
            if (y <= 7) return s(Blocks.RED_NETHER_BRICKS);
            if (y == 8) return s(Blocks.MAGMA_BLOCK);
            return null;
        }
        if (z == GATE_Z + 3 && ax == 5) {
            if (y == 0) return s(Blocks.POLISHED_BLACKSTONE);
            if (y == 1) return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false);
        }
        return null;
    }

    /** Lays the whole cave round {@code floor} (its floor's top at the centre) at once: the gallery's, and tests'. */
    public static void layWhole(LevelAccessor level, BlockPos floor) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = MIN_X; x <= MAX_X; x++) for (int z = MIN_Z; z <= MAX_Z; z++) for (int y = BOTTOM; y <= TOP; y++) {
            BlockState s = at(x, y, z);
            if (s != null) level.setBlock(p.set(floor.getX() + x, floor.getY() + y, floor.getZ() + z), s, Block.UPDATE_CLIENTS);
        }
    }
}
