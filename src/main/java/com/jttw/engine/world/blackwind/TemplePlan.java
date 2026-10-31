package com.jttw.engine.world.blackwind;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.SlabType;

import javax.annotation.Nullable;

import static com.jttw.engine.world.blackwind.BlackWindPlan.*;

/**
 * The burnt ruin of the Guanyin Chan Temple (观音禅院, chapter 16): the old abbot, greedy for the Tang
 * Monk's brocade kasaya, set fire to the meditation hall to burn the pilgrims in it; the Monkey King
 * turned the wind on the monks' own halls, and the whole temple burned while the Black Bear Demon,
 * come over the mountain to help put out the fire, carried the kasaya off to his cave. What stands now,
 * on the terrace across the valley road: the compound's broken red wall, the mountain gate's two
 * pillars (one fallen short) and its lintel in the dirt; the bell tower's and the drum tower's charred
 * posts, the great bell lying where it fell; the side halls' stubs; the main hall's platform, its
 * columns burnt to stumps, its back wall standing black, a corner of its roof still on its beams, the
 * statue of Guanyin thrown down from her pedestal; a broken relic pagoda; ash, cinders, a few fires
 * still smouldering under the rubble; dead trees burnt black. Pure, the same in every world.
 *
 * <p>Local coordinates from the terrace's middle at its floor (local y 0 is the paving); +z south.
 */
public final class TemplePlan {

    private TemplePlan() {}

    public static final byte STONE_BRICK = 64, MOSSY_BRICK = 65, CRACKED_BRICK = 66, COBBLE = 67, MOSSY_COBBLE = 68, GRAVEL = 69,
            STEP_N = 70, STEP_S = 71, CHARRED = 72, CHARRED_X = 73, CHARRED_Z = 74, BELL = 75, CAULDRON = 76, STONE_SLAB = 77,
            COARSE = 78, CHISELED_STONE = 79;

    /** The temple's extent, local. */
    public static final int X_MIN = -38, X_MAX = 38, Z_MIN = -40, Z_MAX = 40, Y_MIN = -3, Y_MAX = 26;
    private static final int SX = X_MAX - X_MIN + 1, SZ = Z_MAX - Z_MIN + 1, SY = Y_MAX - Y_MIN + 1;
    /** Where its middle lies down the valley (local z of the cave's plan). */
    public static final int TEMPLE_Z = (BlackWindShape.TERRACE_START + BlackWindShape.TERRACE_END) / 2;
    /** The compound's wall, its half-sizes. */
    public static final int WALL_X = 34, WALL_Z = 36;
    /** The censer in the courtyard: where the temple's band gathers. */
    public static final int[] COURTYARD = {0, 1, 4};

    private static volatile byte[] cells;

    private static byte[] cells() {
        byte[] c = cells;
        if (c == null) {
            synchronized (TemplePlan.class) {
                c = cells;
                if (c == null) cells = c = new Builder().build();
            }
        }
        return c;
    }

    public static boolean covers(int x, int z) {
        return x >= X_MIN && x <= X_MAX && z >= Z_MIN && z <= Z_MAX;
    }

    public static byte code(int x, int y, int z) {
        if (x < X_MIN || x > X_MAX || y < Y_MIN || y > Y_MAX || z < Z_MIN || z > Z_MAX) return UNTOUCHED;
        return cells()[((x - X_MIN) * SZ + (z - Z_MIN)) * SY + (y - Y_MIN)];
    }

    /** The block at local (x, y, z) of the ruin; null where it leaves the ground as it is. */
    @Nullable
    public static BlockState blockAt(int x, int y, int z, int wx, int wy, int wz) {
        return BlackWindBlocks.state(code(x, y, z), wx, wy, wz, TemplePlan::code, x, y, z);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** The temple's own codes (the cave's are {@link BlackWindBlocks}'). */
    @Nullable
    static BlockState state(byte c, int wx, int wy, int wz, BlackWindBlocks.Codes around, int x, int y, int z) {
        return switch (c) {
            case STONE_BRICK -> s(Blocks.STONE_BRICKS);
            case MOSSY_BRICK -> s(Blocks.MOSSY_STONE_BRICKS);
            case CRACKED_BRICK -> s(Blocks.CRACKED_STONE_BRICKS);
            case CHISELED_STONE -> s(Blocks.CHISELED_STONE_BRICKS);
            case COBBLE -> s(Blocks.COBBLESTONE);
            case MOSSY_COBBLE -> s(Blocks.MOSSY_COBBLESTONE);
            case GRAVEL -> s(Blocks.GRAVEL);
            case COARSE -> s(Blocks.COARSE_DIRT);
            case STEP_N -> s(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.NORTH);
            case STEP_S -> s(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.SOUTH);
            case CHARRED -> s(Blocks.DARK_OAK_WOOD).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            case CHARRED_X -> s(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            case CHARRED_Z -> s(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
            case BELL -> s(Blocks.BELL).setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR).setValue(BellBlock.FACING, Direction.EAST);
            case CAULDRON -> s(Blocks.CAULDRON);
            case STONE_SLAB -> s(Blocks.STONE_BRICK_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            default -> s(Blocks.AIR);
        };
    }

    private static final class Builder {
        private final byte[] c = new byte[SX * SY * SZ];

        void set(int x, int y, int z, byte code) {
            if (x < X_MIN || x > X_MAX || y < Y_MIN || y > Y_MAX || z < Z_MIN || z > Z_MAX) return;
            c[((x - X_MIN) * SZ + (z - Z_MIN)) * SY + (y - Y_MIN)] = code;
        }

        byte get(int x, int y, int z) {
            if (x < X_MIN || x > X_MAX || y < Y_MIN || y > Y_MAX || z < Z_MIN || z > Z_MAX) return UNTOUCHED;
            return c[((x - X_MIN) * SZ + (z - Z_MIN)) * SY + (y - Y_MIN)];
        }

        int r(int x, int z, int salt) {
            return BlackWindPlan.rough(x, z, salt + 700);
        }

        /** A ruined wall course: brick below, red plaster above, blackened by the fire, broken to its height. */
        void wall(int x, int z, int high) {
            for (int y = 1; y <= high; y++) {
                int roll = r(x * 7 + y, z, 3);
                byte b = y <= 2 ? (roll < 200 ? MOSSY_BRICK : roll < 400 ? CRACKED_BRICK : STONE_BRICK)
                        : roll < 350 ? BLACKSTONE : TERRACOTTA;
                set(x, y, z, b);
            }
        }

        /** A charred post standing {@code high}, its broken top black. */
        void post(int x, int base, int z, int high) {
            for (int y = base; y < base + high; y++) set(x, y, z, CHARRED);
            if (high > 0) set(x, base + high - 1, z, high > 3 ? COAL : CHARRED);
        }

        byte[] build() {
            // the terrace: its paving of rubble, ash and cinders; the air over it clear
            for (int x = X_MIN; x <= X_MAX; x++) for (int z = Z_MIN; z <= Z_MAX; z++) {
                for (int y = Y_MIN; y < 0; y++) set(x, y, z, COBBLE);
                int roll = r(x >> 1, z >> 1, 1);
                set(x, 0, z, roll < 280 ? COBBLE : roll < 480 ? GRAVEL : roll < 680 ? SOUL_SOIL : roll < 780 ? MOSSY_COBBLE
                        : roll < 830 ? COAL : roll < 900 ? COARSE : STONE_BRICK);
                for (int y = 1; y <= Y_MAX; y++) set(x, y, z, AIR);
            }
            // the compound's wall, broken; the mountain gate in its south side
            for (int x = -WALL_X; x <= WALL_X; x++) for (int z = -WALL_Z; z <= WALL_Z; z++) {
                if (Math.abs(x) != WALL_X && Math.abs(z) != WALL_Z) continue;
                if (z == WALL_Z && Math.abs(x) <= 7) continue;
                int roll = r(x / 3, z / 3, 2);
                wall(x, z, roll < 150 ? 0 : 1 + roll % 4 + (roll > 800 ? 1 : 0));
            }
            for (int side = -1; side <= 1; side += 2) {
                int high = side < 0 ? 10 : 5; // one pillar standing, the other broken short
                for (int x = side * 5; Math.abs(x) <= 6; x += side) for (int z = WALL_Z - 1; z <= WALL_Z; z++) for (int y = 1; y <= high; y++) {
                    set(x, y, z, y == high ? (high > 6 ? CHISELED_STONE : CRACKED_BRICK) : y % 4 == 0 ? CHISELED_STONE : STONE_BRICK);
                }
            }
            for (int x = -6; x <= -1; x++) set(x, 10, WALL_Z, CHARRED_X); // what is left of the lintel, hanging from the standing pillar
            for (int x = -3; x <= 4; x++) set(x, 1, WALL_Z - 5, CHARRED_X); // and the rest of it, in the dirt
            set(1, 2, WALL_Z - 5, ROOF_S);
            set(2, 2, WALL_Z - 5, ROOF_S);
            for (int x = -4; x <= 4; x++) set(x, 0, WALL_Z + 1, STONE_BRICK);
            // the path up the courtyard to the hall's steps
            for (int z = -10; z <= WALL_Z; z++) for (int x = -2; x <= 2; x++) set(x, 0, z, r(x, z, 4) < 300 ? CRACKED_BRICK : STONE_BRICK);
            // the censer
            for (int x = -1; x <= 1; x++) for (int z = 3; z <= 5; z++) set(x, 1, z, POLISHED_BLACKSTONE);
            set(0, 2, 4, CAULDRON);
            set(-1, 2, 3, CANDLE);
            set(1, 2, 5, CANDLE);
            // the bell tower and the drum tower
            tower(-22, 18, new int[]{7, 3, 8, 2}, true);
            tower(22, 18, new int[]{4, 8, 2, 6}, false);
            // the side halls, burnt to their stubs
            sideHall(-26);
            sideHall(26);
            mainHall();
            pagoda(28, -26);
            // dead trees burnt black
            int[][] trees = {{-30, 28}, {30, 30}, {-12, 24}, {14, 12}, {-31, -20}, {33, -6}, {-14, 6}};
            for (int[] t : trees) tree(t[0], t[1]);
            // cobwebs and ash heaps among the rubble
            for (int x = X_MIN; x <= X_MAX; x++) for (int z = Z_MIN; z <= Z_MAX; z++) {
                if (get(x, 1, z) != AIR || get(x, 0, z) == AIR) continue;
                int roll = r(x, z, 9);
                if (roll < 12) set(x, 1, z, COBWEB);
                else if (roll < 30 && Math.abs(x) > 3) set(x, 1, z, SOUL_SOIL);
            }
            return c;
        }

        void tower(int cx, int cz, int[] highs, boolean bell) {
            for (int x = cx - 4; x <= cx + 4; x++) for (int z = cz - 4; z <= cz + 4; z++) set(x, 1, z, Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4 ? CRACKED_BRICK : STONE_BRICK);
            int k = 0;
            for (int dx : new int[]{-3, 3}) for (int dz : new int[]{-3, 3}) post(cx + dx, 2, cz + dz, highs[k++]);
            for (int x = cx - 3; x <= cx; x++) set(x, 2 + Math.max(highs[0], highs[2]) - 1, cz - 3, CHARRED_X); // a beam, half fallen
            if (bell) set(cx + 1, 2, cz + 1, BELL);
            else {
                set(cx + 1, 2, cz, BARREL);
                set(cx - 1, 2, cz + 2, CHARRED_Z);
                set(cx - 1, 2, cz + 1, CHARRED_Z);
            }
        }

        void sideHall(int cx) {
            for (int x = cx - 7; x <= cx + 7; x++) for (int z = -9; z <= 9; z++) {
                boolean edge = Math.abs(x - cx) == 7 || Math.abs(z) == 9;
                set(x, 1, z, STONE_BRICK);
                if (edge) {
                    int roll = r(x / 2, z / 2, 5);
                    for (int y = 2; y <= 1 + (roll < 300 ? 0 : 1 + roll % 3); y++) set(x, y, z, roll % 3 == 0 ? BLACKSTONE : TERRACOTTA);
                } else if (r(x, z, 6) < 60) set(x, 2, z, r(x, z, 7) < 300 ? COAL : SOUL_SOIL);
            }
            for (int dx : new int[]{-7, 0, 7}) for (int dz : new int[]{-9, 9}) post(cx + dx, 2, dz, 2 + r(cx + dx, dz, 8) % 5);
            // a fallen roof beam across it
            for (int z = -6; z <= 5; z++) set(cx + (z / 4), 2, z, CHARRED_Z);
        }

        void mainHall() {
            int x0 = -22, x1 = 22, z0 = -36, z1 = -13;
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                set(x, 1, z, STONE_BRICK);
                set(x, 2, z, Math.abs(x) == 22 || z == z1 || z == z0 ? CHISELED_STONE : r(x, z, 10) < 300 ? CRACKED_BRICK : STONE_BRICK);
            }
            for (int x = -7; x <= 7; x++) {
                set(x, 2, z1, STEP_N);
                set(x, 1, z1 + 1, STEP_N);
            }
            // the columns, burnt to stumps; one fallen across the floor
            for (int x : new int[]{-18, -10, -3, 3, 10, 18}) for (int z : new int[]{-16, -25, -33}) {
                int roll = r(x, z, 11);
                if (roll < 160) {
                    for (int k = 1; k <= 5; k++) set(x + (x > 0 ? -k : k) / 2, 3, z + 1, CHARRED_X);
                    continue;
                }
                post(x, 3, z, 2 + roll % 9);
            }
            // the back wall, standing black, highest in its middle; the side walls broken low
            for (int x = x0; x <= x1; x++) {
                int high = 9 - Math.abs(x) / 3 + r(x, 0, 12) % 3 - (r(x / 4, 1, 13) < 250 ? 5 : 0);
                for (int y = 3; y <= 2 + Math.max(0, high); y++) set(x, y, z0, y <= 4 ? TERRACOTTA : r(x, y, 14) < 600 ? BLACKSTONE : TERRACOTTA);
            }
            for (int z = z0; z <= z1 - 4; z++) for (int side : new int[]{x0, x1}) {
                int high = r(side, z / 3, 15) % 5;
                for (int y = 3; y <= 2 + high; y++) set(side, y, z, r(side, z * 3 + y, 16) < 500 ? BLACKSTONE : TERRACOTTA);
            }
            // a corner of the roof still on its beams over the north-east, the rest fallen in
            for (int x = 10; x <= x1 + 1; x++) for (int z = z0 - 1; z <= z0 + 9; z++) {
                set(x, 11, z, (x + z) % 3 == 0 ? CHARRED_X : DARK_PLANKS);
                if (r(x, z, 17) < 650) set(x, 12, z, z <= z0 + 4 ? ROOF_S : ROOF_N);
            }
            for (int x : new int[]{10, 18}) for (int y = 3; y <= 10; y++) set(x, y, z0 + 8, CHARRED);
            for (int x = -16; x <= 8; x++) for (int z = z0 + 2; z <= z1 - 2; z++) {
                int roll = r(x, z, 18);
                if (roll < 40) set(x, 3, z, ROOF_N);
                else if (roll < 70) set(x, 3, z, SOUL_SOIL);
                else if (roll < 85) set(x, 3, z, COAL);
            }
            // her pedestal, empty; Guanyin thrown down across the floor before it, her head apart
            for (int x = -2; x <= 2; x++) for (int z = z0 + 3; z <= z0 + 5; z++) {
                set(x, 3, z, CHISELED_STONE);
                set(x, 4, z, Math.abs(x) <= 1 && z == z0 + 4 ? CALCITE : STONE_SLAB);
            }
            for (int x = -7; x <= 0; x++) for (int z = -27; z <= -26; z++) set(x, 3, z, CALCITE);
            for (int x = -6; x <= -1; x++) set(x, 4, -27, CALCITE);
            set(-8, 3, -26, CALCITE);
            set(3, 3, -24, CALCITE);
            set(3, 4, -24, CALCITE);
            set(4, 3, -24, CALCITE);
            for (int x = -3; x <= 3; x += 3) set(x, 3, z0 + 6, CANDLE);
            // fires still smouldering under the rubble
            set(-14, 3, -20, CAMPFIRE);
            set(12, 3, -30, CAMPFIRE);
            set(-26, 2, 4, CAMPFIRE);
        }

        void pagoda(int cx, int cz) {
            for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) for (int y = 1; y <= 2; y++) set(x, y, z, STONE_BRICK);
            for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) for (int y = 3; y <= 6; y++) set(x, y, z, r(x, z * 9 + y, 19) < 300 ? CRACKED_BRICK : STONE_BRICK);
            for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) if (Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2) set(x, 7, z, STONE_SLAB);
            for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) for (int y = 8; y <= 10; y++) {
                if (x == cx + 1 && z == cz - 1 && y >= 9) continue; // broken
                set(x, y, z, MOSSY_BRICK);
            }
            set(cx - 1, 11, cz, STONE_SLAB);
            set(cx + 3, 1, cz + 1, MOSSY_BRICK); // its top, fallen
            set(cx + 3, 1, cz + 2, STONE_SLAB);
        }

        void tree(int x, int z) {
            int high = 4 + r(x, z, 20) % 5;
            for (int y = 1; y <= high; y++) set(x, y, z, CHARRED);
            for (int b = 0; b < 3; b++) {
                int y = high - 1 - b * 2;
                if (y < 2) break;
                int dir = r(x, z, 21 + b) % 4;
                int dx = dir == 0 ? 1 : dir == 1 ? -1 : 0, dz = dir == 2 ? 1 : dir == 3 ? -1 : 0;
                for (int len = 1; len <= 2; len++) set(x + dx * len, y + (len == 2 ? 1 : 0), z + dz * len, dx != 0 ? CHARRED_X : CHARRED_Z);
            }
        }
    }
}
