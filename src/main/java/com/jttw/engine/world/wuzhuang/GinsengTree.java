package com.jttw.engine.world.wuzhuang;

import com.jttw.engine.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The ginseng-fruit tree (人参果树, the Grass-Returning Cinnabar, 草还丹), in the temple's garden:
 * a broad old trunk, green boughs, and leaves like the plantain's (jungle leaves), as the novel
 * paints it; its fruit hanging under the low boughs, within a tall man's reach with a long rod.
 * Part of the temple's blueprint ({@link Temple}).
 */
public final class GinsengTree {

    private GinsengTree() {}

    /** Lays a block of the blueprint at (x, y, z). */
    public interface Setter {
        void set(int x, int y, int z, BlockState state);
    }

    /** How tall the trunk rises, and the crown's heart over the root. */
    public static final int TRUNK = 10, CROWN_Y = 16;

    /** The ages the twelve fruit are laid at: half of them ripe. */
    private static final int[] AGES = {2, 2, 1, 2, 0, 2, 1, 2, 0, 2, 1, 0};

    private static BlockState log(Direction.Axis axis) {
        return Blocks.JUNGLE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    /** Lays the tree rooted at (bx, 0, bz) (the ground's top); its fruit are added to {@code fruit} as {x, y, z, age}. */
    public static void build(Setter set, int bx, int bz, List<int[]> fruit) {
        Set<Long> wood = new HashSet<>();
        Setter logs = (x, y, z, s) -> {
            wood.add(key(x, y, z));
            set.set(x, y, z, s);
        };
        // the trunk: three by three, then one, flaring at its roots
        for (int y = 1; y <= TRUNK; y++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            logs.set(bx + dx, y, bz + dz, log(Direction.Axis.Y));
        for (int y = TRUNK + 1; y <= CROWN_Y; y++) logs.set(bx, y, bz, log(Direction.Axis.Y));
        for (int[] d : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
            logs.set(bx + d[0], 1, bz + d[1], log(d[0] != 0 ? Direction.Axis.X : Direction.Axis.Z));
            logs.set(bx + d[0], 2, bz + d[1], log(Direction.Axis.Y));
        }
        // boughs: four low ones out to the sides, four up and out to the corners
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            for (int i = 2; i <= 6; i++) logs.set(bx + d[0] * i, 6 + i / 4, bz + d[1] * i, log(d[0] != 0 ? Direction.Axis.X : Direction.Axis.Z));
        }
        for (int[] d : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            for (int i = 1; i <= 5; i++) {
                logs.set(bx + d[0] * i, 8 + (i * 3) / 5, bz + d[1] * i, log(Direction.Axis.Y));
            }
        }
        // the leaves: a broad crown, clouds of leaves over the upper boughs, a low skirt over the lower ones
        BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        blob(set, wood, bx, CROWN_Y, bz, 7.5, 4.5, leaves);
        for (int[] d : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) blob(set, wood, bx + d[0] * 5, 12, bz + d[1] * 5, 3.8, 2.8, leaves);
        int[][] skirts = {{6, 0}, {-6, 0}, {0, 6}, {0, -6}};
        for (int[] d : skirts) blob(set, wood, bx + d[0], 8, bz + d[1], 3.6, 1.9, leaves);
        // the fruit: three under each low bough's leaves, hanging from the lowest of them
        int n = 0;
        for (int[] d : skirts) for (int o = -1; o <= 1; o++) {
            int radial = o == 0 ? 1 : 0, across = 2 * o;
            int x = bx + (d[0] != 0 ? d[0] + Integer.signum(d[0]) * radial : across);
            int z = bz + (d[1] != 0 ? d[1] + Integer.signum(d[1]) * radial : across);
            int low = lowestLeaf(x, z, bx, bz);
            if (low < 0 || wood.contains(key(x, low - 1, z))) continue;
            fruit.add(new int[]{x, low - 1, z, AGES[n % AGES.length]});
            set.set(x, low - 1, z, ModBlocks.HANGING_GINSENG_FRUIT.get().defaultBlockState()
                    .setValue(com.jttw.engine.world.wuzhuang.GinsengFruitBlock.AGE, AGES[n % AGES.length]));
            n++;
        }
    }

    private static long key(int x, int y, int z) {
        return net.minecraft.core.BlockPos.asLong(x, y, z);
    }

    private static final Set<Long> LEAVES = new HashSet<>();

    private static void blob(Setter set, Set<Long> wood, int cx, int cy, int cz, double r, double ry, BlockState leaves) {
        int R = (int) Math.ceil(r), RY = (int) Math.ceil(ry);
        for (int dx = -R; dx <= R; dx++) for (int dy = -RY; dy <= RY; dy++) for (int dz = -R; dz <= R; dz++) {
            double d = (dx * dx + dz * dz) / (r * r) + (dy * dy) / (ry * ry);
            // a ragged edge, the same each time
            double rag = ((Math.floorMod((cx + dx) * 73 + (cy + dy) * 37 + (cz + dz) * 19, 11)) / 11.0) * 0.35;
            if (d > 1 - rag + 0.15) continue;
            long k = key(cx + dx, cy + dy, cz + dz);
            if (wood.contains(k)) continue;
            LEAVES.add(k);
            set.set(cx + dx, cy + dy, cz + dz, leaves);
        }
    }

    /** The lowest leaf of the tree in column (x, z), over the ground; -1 if none or too low to hang fruit under. */
    private static int lowestLeaf(int x, int z, int bx, int bz) {
        for (int y = 3; y <= CROWN_Y + 6; y++) if (LEAVES.contains(key(x, y, z))) return y;
        return -1;
    }
}
