package com.jttw.engine.world.wuzhuang;

import com.jttw.engine.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Temple of the Five Villages (五庄观), Zhenyuan's Daoist temple on Mount Longevity: a
 * blueprint, the same in every region (its gate faces south), laid column by column by each chunk
 * of the island ({@code MountWuzhuangFeature}), so it comes out whole whichever chunk is made first.
 * Local coordinates from the region's centre, which is where Zhenyuan stands before the altar; y 0
 * is the terrace's top ({@link IslandShape#TERRACE}).
 *
 * <ul>
 *   <li><b>The mountain gate</b> (山门) in the south wall, under a hip roof of dark tiles, its plaque
 *       五庄观 and the couplet the novel gives it: 长生不老神仙府 · 与天同寿道人家 ("The immortal's
 *       house that never ages; the Daoist's home as old as Heaven");</li>
 *   <li>a <b>front court</b> paved in stone, a great censer smoking at its heart, stone lanterns,
 *       pines, the <b>bell and drum towers</b> either side, the disciples' <b>wings</b> down both
 *       flanks;</li>
 *   <li>the <b>Hall of Heaven and Earth</b> on its platform, double-eaved, red columns within, and at
 *       its back the <b>altar</b> on which, as the novel says, only the two words 天地 are honoured
 *       (Zhenyuan worships Heaven and Earth alone), candles and a censer before it;</li>
 *   <li>through a <b>moon gate</b> in the rear wall, the <b>garden</b>: the ginseng-fruit tree
 *       (人参果树) at its heart, its broad plantain-like leaves, its fruit hanging under the low
 *       boughs ({@link #FRUIT}); a pond with lilies, a pavilion, rockeries, flowers.</li>
 * </ul>
 */
public final class Temple {

    private Temple() {}

    /** Where Zhenyuan stands: before the altar, on the hall's floor (local; y the block he stands in). */
    public static final int ZHENYUAN_Y = 2;
    /** The ginseng-fruit tree's root (local), and the youths' places before it. */
    public static final int TREE_X = 0, TREE_Z = -27;
    public static final int[] CLEAR_WIND = {-3, 1, -18}, BRIGHT_MOON = {3, 1, -18};
    /** The hall: its walls' bounds (local). */
    public static final int HALL_X = 10, HALL_NORTH = -6, HALL_SOUTH = 6;

    /** A sign laid with the temple: where, which way it faces, its lines. */
    public record Sign(int x, int y, int z, Direction facing, String... lines) {}

    /**
     * Someone who lives here, put in place as the land is made: who, where (local), facing. These are
     * the temple's whole household, and never any more ({@link TempleRecord}): twenty-two disciples
     * (the novel's forty-eight, half of them away with their master) and the youths Clear Wind and
     * Bright Moon, twenty-four in all.
     */
    public record Resident(String who, int x, int y, int z, float yaw) {}

    /** One block of the blueprint in a column. */
    public record Placed(int y, BlockState state) {}

    private static final Map<Long, BlockState> PLAN = new LinkedHashMap<>();
    private static final Map<Long, List<Placed>> COLUMNS = new HashMap<>();
    public static final List<Sign> SIGNS = new ArrayList<>();
    /** The tree's fruit (local x, y, z) and the age each is laid at (0 blossom, 1 green, 2 ripe). */
    public static final List<int[]> FRUIT = new ArrayList<>();
    public static final List<Resident> RESIDENTS = List.of(
            new Resident("clear_wind", CLEAR_WIND[0], CLEAR_WIND[1], CLEAR_WIND[2], 0f),
            new Resident("bright_moon", BRIGHT_MOON[0], BRIGHT_MOON[1], BRIGHT_MOON[2], 0f),
            // in the hall, before the altar and down its aisles
            new Resident("disciple", -5, 2, 2, 90f), new Resident("disciple", 5, 2, 2, 270f),
            new Resident("disciple", -8, 2, -3, 90f), new Resident("disciple", 8, 2, -3, 270f),
            new Resident("disciple", -8, 2, 4, 90f), new Resident("disciple", 8, 2, 4, 270f),
            // the front court
            new Resident("disciple", -8, 1, 21, 200f), new Resident("disciple", 8, 1, 17, 160f),
            new Resident("disciple", -6, 1, 14, 120f), new Resident("disciple", 6, 1, 23, 240f),
            // the wings
            new Resident("disciple", -16, 2, 4, 270f), new Resident("disciple", 16, 2, 9, 90f),
            new Resident("disciple", -16, 2, -3, 270f), new Resident("disciple", 16, 2, -2, 90f),
            new Resident("disciple", -17, 2, 12, 270f), new Resident("disciple", 17, 2, 14, 90f),
            // the gate
            new Resident("disciple", -4, 2, 30, 0f), new Resident("disciple", 4, 2, 30, 0f),
            // the garden and the pavilion
            new Resident("disciple", 0, 1, -36, 180f), new Resident("disciple", -13, 2, -31, 200f),
            new Resident("disciple", 10, 1, -24, 120f), new Resident("disciple", -9, 1, -22, 240f));

    // ── Building it ─────────────────────────────────────────────────────

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static void set(int x, int y, int z, BlockState state) {
        PLAN.put(BlockPos.asLong(x, y, z), state);
    }

    private static void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, state);
    }

    private static int hash(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt, z), 100L);
    }

    private static final BlockState PILLAR = s(Blocks.RED_CONCRETE), WALL = s(Blocks.RED_TERRACOTTA),
            BAND = s(Blocks.WARPED_PLANKS), WOOD = s(Blocks.DARK_OAK_PLANKS), TILE = s(Blocks.DEEPSLATE_TILES),
            PAVING = s(Blocks.POLISHED_ANDESITE), EDGE = s(Blocks.STONE_BRICKS), AIR = s(Blocks.AIR);

    private static BlockState stair(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static BlockState lantern(boolean hanging) {
        return s(Blocks.LANTERN).setValue(LanternBlock.HANGING, hanging);
    }

    /**
     * A hip roof of dark tiles over (x0, z0)..(x1, z1) from height y: each course a block in and a
     * block up, stairs round its edge and tiles at its corners, a ridge of tiles along the top
     * with gilded ends.
     */
    private static void hipRoof(int x0, int z0, int x1, int z1, int y) {
        for (int i = 0; ; i++) {
            int a0 = x0 + i, a1 = x1 - i, b0 = z0 + i, b1 = z1 - i, yy = y + i;
            if (a0 > a1 || b0 > b1) break;
            if (a1 - a0 <= 1 || b1 - b0 <= 1) {
                fill(a0, yy, b0, a1, yy, b1, TILE);
                if (a1 - a0 > b1 - b0) {
                    set(a0, yy + 1, (b0 + b1) / 2, s(Blocks.GOLD_BLOCK));
                    set(a1, yy + 1, (b0 + b1) / 2, s(Blocks.GOLD_BLOCK));
                } else {
                    set((a0 + a1) / 2, yy + 1, b0, s(Blocks.GOLD_BLOCK));
                    set((a0 + a1) / 2, yy + 1, b1, s(Blocks.GOLD_BLOCK));
                }
                break;
            }
            for (int x = a0; x <= a1; x++) {
                set(x, yy, b0, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH));
                set(x, yy, b1, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH));
            }
            for (int z = b0; z <= b1; z++) {
                set(a0, yy, z, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST));
                set(a1, yy, z, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST));
            }
            for (int[] c : new int[][]{{a0, b0}, {a1, b0}, {a0, b1}, {a1, b1}}) set(c[0], yy, c[1], TILE);
        }
    }

    /** An eave skirt one course wide round (x0, z0)..(x1, z1) at height y: stairs on its outer row, tiles within. */
    private static void eave(int x0, int z0, int x1, int z1, int y) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean outer = x == x0 || x == x1 || z == z0 || z == z1;
            boolean inner = x == x0 + 1 || x == x1 - 1 || z == z0 + 1 || z == z1 - 1;
            if (!outer && !inner) continue;
            if (outer) {
                boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                Direction in = z == z0 ? Direction.SOUTH : z == z1 ? Direction.NORTH : x == x0 ? Direction.EAST : Direction.WEST;
                set(x, y, z, corner ? TILE : stair(Blocks.DEEPSLATE_TILE_STAIRS, in));
            } else set(x, y, z, TILE);
        }
    }

    /**
     * A building's shell over (x0, z0)..(x1, z1): walls from y0 to y1, red columns at the corners
     * and every {@code bay} blocks, lattice windows, a band of painted (blue-green) beams on top.
     */
    private static void shell(int x0, int z0, int x1, int z1, int y0, int y1, int bay) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edgeX = x == x0 || x == x1, edgeZ = z == z0 || z == z1;
            if (!edgeX && !edgeZ) continue;
            boolean column = (edgeX && edgeZ) || (edgeZ && Math.floorMod(x - x0, bay) == 0) || (edgeX && Math.floorMod(z - z0, bay) == 0);
            for (int y = y0; y <= y1; y++) {
                if (column) set(x, y, z, PILLAR);
                else if (y == y0) set(x, y, z, WOOD);
                else if (y == y0 + 2 || y == y0 + 3) {
                    Direction out = z == z0 ? Direction.NORTH : z == z1 ? Direction.SOUTH : x == x0 ? Direction.WEST : Direction.EAST;
                    set(x, y, z, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.FACING, out).setValue(TrapDoorBlock.OPEN, true)
                            .setValue(TrapDoorBlock.HALF, Half.BOTTOM));
                } else set(x, y, z, WALL);
            }
            set(x, y1 + 1, z, BAND);
        }
    }

    /** A pine of height h rooted at (x, 0, z) (needles kept: no log is far). */
    private static void pine(int x, int z, int h) {
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int dy = 1; dy <= h + 1; dy++) {
            int k = IslandShape.pineAt(dx, dy, dz, h);
            if (k == 1) set(x + dx, dy, z + dz, s(Blocks.SPRUCE_LOG));
            else if (k == 2) set(x + dx, dy, z + dz, s(Blocks.SPRUCE_LEAVES).setValue(LeavesBlock.PERSISTENT, true));
        }
    }

    static {
        build();
        for (Map.Entry<Long, BlockState> e : PLAN.entrySet()) {
            long p = e.getKey();
            COLUMNS.computeIfAbsent(column(BlockPos.getX(p), BlockPos.getZ(p)), k -> new ArrayList<>())
                    .add(new Placed(BlockPos.getY(p), e.getValue()));
        }
    }

    private static long column(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /** What the blueprint lays in local column (x, z); empty where nothing. */
    public static List<Placed> columnAt(int lx, int lz) {
        return COLUMNS.getOrDefault(column(lx, lz), List.of());
    }

    /** What the blueprint lays at local (x, y, z), or null. */
    public static BlockState at(int x, int y, int z) {
        return PLAN.get(BlockPos.asLong(x, y, z));
    }

    /** How many blocks the blueprint lays. */
    public static int size() {
        return PLAN.size();
    }

    private static void build() {
        ground();
        walls();
        gate();
        court();
        wings();
        hall();
        garden();
        GinsengTree.build(Temple::set, TREE_X, TREE_Z, FRUIT);
    }

    // ── The ground ──────────────────────────────────────────────────────

    private static void ground() {
        // the paved front court and the yard behind the hall; the garden keeps its grass
        for (int x = -21; x <= 21; x++) for (int z = -11; z <= 29; z++) {
            boolean path = Math.abs(x) <= 1;
            set(x, 0, z, path ? s(Blocks.SMOOTH_STONE) : (x + z) % 7 == 0 ? s(Blocks.ANDESITE) : PAVING);
        }
        // the garden's paths: from the moon gate to the tree, round it, to the pavilion and the pond
        for (int z = -13; z >= -19; z--) for (int x = -1; x <= 1; x++) set(x, 0, z, s(Blocks.DIRT_PATH));
        for (int x = -24; x <= 24; x++) for (int z = -45; z <= -12; z++) {
            double d = Math.hypot(x - TREE_X, z - TREE_Z);
            if (d >= 8 && d < 9.3 && Math.abs(x) <= 21 && z >= -39) set(x, 0, z, s(Blocks.DIRT_PATH));
        }
        for (int x = -12; x >= -18 + 7; x--) set(x, 0, -32, s(Blocks.DIRT_PATH));
        // the road out of the gate to the rim, kerbed; its lanterns are the island's
        for (int z = 31; z <= 36; z++) for (int x = -IslandShape.ROAD_HALF; x <= IslandShape.ROAD_HALF; x++)
            set(x, 0, z, Math.abs(x) == IslandShape.ROAD_HALF ? s(Blocks.MOSSY_STONE_BRICKS) : EDGE);
    }

    // ── The walls ───────────────────────────────────────────────────────

    private static void wallRun(int x0, int z0, int x1, int z1, int height) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            set(x, 1, z, EDGE);
            for (int y = 2; y < height; y++) set(x, y, z, WALL);
            set(x, height, z, s(Blocks.DEEPSLATE_TILE_SLAB));
        }
    }

    private static void walls() {
        wallRun(-22, -40, -22, 30, 5);
        wallRun(22, -40, 22, 30, 5);
        wallRun(-22, -40, 22, -40, 5);
        wallRun(-22, 30, -8, 30, 5);
        wallRun(8, 30, 22, 30, 5);
        // the rear wall, higher, and its moon gate
        wallRun(-22, -12, 22, -12, 8);
        for (int x = -4; x <= 4; x++) for (int y = 1; y <= 7; y++) {
            if (x * x + (y - 4) * (y - 4) <= 10.5) set(x, y, -12, AIR);
            else if (x * x + (y - 4) * (y - 4) <= 17) set(x, y, -12, s(Blocks.SMOOTH_QUARTZ));
        }
        set(0, 0, -12, s(Blocks.SMOOTH_STONE));
    }

    // ── The mountain gate ───────────────────────────────────────────────

    private static void gate() {
        // its platform and steps
        fill(-8, 0, 26, 8, 0, 34, EDGE);
        fill(-8, 1, 26, 8, 1, 34, PAVING);
        for (int x = -3; x <= 3; x++) set(x, 1, 35, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH));
        for (int x = -3; x <= 3; x++) set(x, 1, 25, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH));
        // walls: the front and back pierced by the doorway, columns at the corners and either side of it
        for (int x = -7; x <= 7; x++) for (int z : new int[]{28, 32}) for (int y = 2; y <= 7; y++) {
            boolean door = Math.abs(x) <= 2 && y <= 5;
            boolean column = Math.abs(x) == 7 || Math.abs(x) == 3;
            set(x, y, z, door ? AIR : column ? PILLAR : WALL);
        }
        for (int z = 29; z <= 31; z++) for (int y = 2; y <= 7; y++) {
            set(-7, y, z, WALL);
            set(7, y, z, WALL);
        }
        for (int x = -7; x <= 7; x++) for (int z = 28; z <= 32; z++) {
            boolean ring = Math.abs(x) == 7 || z == 28 || z == 32;
            set(x, 8, z, ring ? BAND : WOOD);
        }
        // the gate's leaves, swung open against the inner walls
        for (int y = 2; y <= 5; y++) {
            set(-6, y, 29, s(Blocks.DARK_OAK_PLANKS));
            set(6, y, 29, s(Blocks.DARK_OAK_PLANKS));
        }
        hipRoof(-9, 26, 9, 34, 9);
        // the plaque over the door, the couplet either side of it
        set(-1, 6, 32, s(Blocks.GOLD_BLOCK));
        set(1, 6, 32, s(Blocks.GOLD_BLOCK));
        SIGNS.add(new Sign(0, 6, 33, Direction.SOUTH, "五庄观", "Temple of the", "Five Villages", ""));
        SIGNS.add(new Sign(-5, 4, 33, Direction.SOUTH, "长生不老", "神仙府", "Ageless, the", "immortal's house"));
        SIGNS.add(new Sign(5, 4, 33, Direction.SOUTH, "与天同寿", "道人家", "As old as Heaven,", "the Daoist's home"));
        set(-3, 2, 34, lantern(false));
        set(3, 2, 34, lantern(false));
    }

    // ── The front court ─────────────────────────────────────────────────

    private static void court() {
        // the great censer, smoking
        fill(-1, 1, 19, 1, 1, 21, s(Blocks.POLISHED_DEEPSLATE));
        set(0, 2, 20, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true));
        for (int[] c : new int[][]{{-1, 19}, {1, 19}, {-1, 21}, {1, 21}}) set(c[0], 2, c[1], s(Blocks.POLISHED_DEEPSLATE_WALL));
        // stone lanterns down the path
        for (int z : new int[]{12, 16, 24}) for (int x : new int[]{-4, 4}) {
            set(x, 1, z, s(Blocks.STONE_BRICK_WALL));
            set(x, 2, z, s(Blocks.STONE_BRICK_WALL));
            set(x, 3, z, lantern(false));
        }
        // pines in the court's corners
        pine(-10, 23, 9);
        pine(10, 23, 9);
        pine(-10, 13, 8);
        pine(10, 13, 8);
        // the bell tower (east) and the drum tower (west)
        for (int side : new int[]{-1, 1}) {
            int x0 = side < 0 ? -18 : 14, x1 = x0 + 4, z0 = 19, z1 = 23;
            fill(x0, 1, z0, x1, 1, z1, EDGE);
            for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) for (int y = 2; y <= 8; y++) set(c[0], y, c[1], PILLAR);
            fill(x0, 5, z0, x1, 5, z1, WOOD);
            for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                boolean ring = x == x0 || x == x1 || z == z0 || z == z1;
                set(x, 8, z, ring ? BAND : WOOD);
                if (y6Rail(x, z, x0, x1, z0, z1)) set(x, 6, z, s(Blocks.DARK_OAK_FENCE));
            }
            hipRoof(x0 - 1, z0 - 1, x1 + 1, z1 + 1, 9);
            int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
            if (side > 0) set(mx, 7, mz, s(Blocks.BELL).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.SOUTH));
            else {
                set(mx, 6, mz, s(Blocks.BARREL));
                set(mx, 7, mz, s(Blocks.RED_CARPET));
            }
        }
    }

    private static boolean y6Rail(int x, int z, int x0, int x1, int z0, int z1) {
        return (x == x0 || x == x1 || z == z0 || z == z1) && !((x == x0 || x == x1) && (z == z0 || z == z1));
    }

    // ── The disciples' wings ────────────────────────────────────────────

    private static void wings() {
        for (int side : new int[]{-1, 1}) {
            int xin = 13 * side, xout = 20 * side;
            int x0 = Math.min(xin, xout), x1 = Math.max(xin, xout);
            fill(x0 - 1, 1, -8, x1 + 1, 1, 18, EDGE);
            shell(x0, -6, x1, 16, 2, 5, 3);
            // doors on the court side
            for (int z : new int[]{-1, 5, 11}) for (int y = 2; y <= 4; y++) set(xin, y, z, AIR);
            // within: a floor, bunks, lanterns
            for (int x = x0 + 1; x < x1; x++) for (int z = -5; z < 16; z++) {
                set(x, 1, z, s(Blocks.SPRUCE_PLANKS));
                for (int y = 2; y <= 6; y++) set(x, y, z, AIR);
            }
            for (int z = -4; z <= 14; z += 3) {
                int x = xout - side;
                set(x, 2, z, s(Blocks.WHITE_WOOL));
                set(x, 2, z + 1, s(Blocks.WHITE_WOOL));
            }
            for (int z : new int[]{0, 8}) set((xin + xout) / 2, 6, z, lantern(true));
            fill(x0, 7, -6, x1, 7, 16, WOOD);
            hipRoof(x0 - 2, -8, x1 + 2, 18, 7);
        }
    }

    // ── The Hall of Heaven and Earth ────────────────────────────────────

    private static void hall() {
        int X = HALL_X, N = HALL_NORTH, S = HALL_SOUTH;
        // the platform and its steps
        fill(-13, 0, -8, 13, 0, 10, EDGE);
        for (int x = -13; x <= 13; x++) for (int z = -8; z <= 10; z++) {
            boolean rim = Math.abs(x) == 13 || z == -8 || z == 10;
            set(x, 1, z, rim ? EDGE : PAVING);
        }
        for (int x = -3; x <= 3; x++) set(x, 1, 11, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH));
        // walls, columns every three; the doorway and the porch's columns
        shell(-X, N, X, S, 2, 8, 3);
        for (int x = -2; x <= 2; x++) for (int y = 2; y <= 6; y++) set(x, y, S, AIR);
        for (int x = -2; x <= 2; x++) set(x, 7, S, BAND);
        for (int x : new int[]{-10, -7, -4, 4, 7, 10}) for (int y = 2; y <= 9; y++) set(x, y, 9, PILLAR);
        // within: the floor, two rows of red columns, a flat ceiling, lanterns, a carpet to the altar
        for (int x = -X + 1; x < X; x++) for (int z = N + 1; z < S; z++) {
            set(x, 1, z, s(Blocks.POLISHED_DEEPSLATE));
            for (int y = 2; y <= 8; y++) set(x, y, z, AIR);
            set(x, 9, z, WOOD);
        }
        for (int x : new int[]{-5, 5}) for (int z : new int[]{-3, 0, 3}) for (int y = 2; y <= 8; y++) set(x, y, z, PILLAR);
        for (int x : new int[]{-8, -5, 5, 8}) for (int z : new int[]{-4, -1, 2, 5}) set(x, 8, z, lantern(true));
        for (int x : new int[]{-2, 2}) set(x, 8, 0, lantern(true));
        for (int z = N + 2; z < S; z++) for (int x = -1; x <= 1; x++) set(x, 2, z, s(Blocks.RED_CARPET));
        // the altar to Heaven and Earth: a long table, candles, a censer; over it, on a dark panel framed in gold, 天地
        for (int x = -3; x <= 3; x++) set(x, 2, N + 1, s(Blocks.DARK_OAK_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
        for (int x : new int[]{-3, -1, 1, 3}) set(x, 3, N + 1, s(Blocks.WHITE_CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        set(0, 3, N + 1, s(Blocks.DECORATED_POT));
        set(-2, 3, N + 1, s(Blocks.POTTED_AZALEA));
        set(2, 3, N + 1, s(Blocks.POTTED_FLOWERING_AZALEA));
        set(0, 2, -3, s(Blocks.CAULDRON));
        for (int x = -3; x <= 3; x++) for (int y = 3; y <= 7; y++) set(x, y, N, Math.abs(x) == 3 || y == 3 || y == 7 ? s(Blocks.GOLD_BLOCK) : s(Blocks.BLACK_TERRACOTTA));
        SIGNS.add(new Sign(0, 5, N + 1, Direction.SOUTH, "", "天  地", "Heaven and Earth", ""));
        for (int x : new int[]{-2, 2}) set(x, 2, -2, s(Blocks.YELLOW_CARPET));
        // the lower eave, the upper storey, the upper roof
        eave(-X - 2, N - 2, X + 2, S + 4, 10);
        for (int x = -X; x <= X; x++) for (int z = N; z <= S; z++) set(x, 10, z, TILE);
        for (int x = -8; x <= 8; x++) for (int z = N + 2; z <= S - 2; z++) {
            boolean ring = Math.abs(x) == 8 || z == N + 2 || z == S - 2;
            if (!ring) continue;
            boolean column = Math.abs(x) == 8 && (z == N + 2 || z == S - 2) || (Math.abs(x) % 4 == 0 && (z == N + 2 || z == S - 2));
            set(x, 11, z, column ? PILLAR : s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.OPEN, true)
                    .setValue(TrapDoorBlock.FACING, z == N + 2 ? Direction.NORTH : z == S - 2 ? Direction.SOUTH : x < 0 ? Direction.WEST : Direction.EAST));
            set(x, 12, z, column ? PILLAR : WALL);
            set(x, 13, z, BAND);
        }
        hipRoof(-X - 1, N - 1, X + 1, S + 1, 14);
        // the plaque over the doors
        SIGNS.add(new Sign(0, 9, S + 1, Direction.SOUTH, "天地殿", "Hall of Heaven", "and Earth", ""));
        set(0, 9, S, s(Blocks.GOLD_BLOCK));
        // the yard behind the hall: stone lanterns
        for (int x : new int[]{-8, 8}) {
            set(x, 1, -10, s(Blocks.STONE_BRICK_WALL));
            set(x, 2, -10, lantern(false));
        }
    }

    // ── The garden ──────────────────────────────────────────────────────

    private static void garden() {
        // the pond, rimmed with stone, lilies on it
        int px = 13, pz = -33;
        for (int x = px - 6; x <= px + 6; x++) for (int z = pz - 6; z <= pz + 6; z++) {
            double d = Math.hypot(x - px, z - pz);
            if (d < 4.6) {
                set(x, 0, z, s(Blocks.WATER));
                set(x, -1, z, s(Blocks.WATER));
                set(x, -2, z, s(Blocks.CLAY));
                if (hash(x, z, 3) < 14) set(x, 1, z, s(Blocks.LILY_PAD));
            } else if (d < 5.6) set(x, 0, z, s(Blocks.MOSSY_COBBLESTONE));
        }
        // the pavilion
        int ax = -14, az = -32;
        fill(ax - 2, 1, az - 2, ax + 2, 1, az + 2, EDGE);
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) for (int y = 2; y <= 5; y++) set(ax + c[0], y, az + c[1], PILLAR);
        for (int x = ax - 2; x <= ax + 2; x++) for (int z = az - 2; z <= az + 2; z++) set(x, 6, z, BAND);
        hipRoof(ax - 3, az - 3, ax + 3, az + 3, 7);
        set(ax, 2, az, s(Blocks.DARK_OAK_FENCE));
        set(ax, 3, az, s(Blocks.DARK_OAK_PRESSURE_PLATE));
        // rockeries
        for (int[] r : new int[][]{{15, -18}, {-15, -20}, {-17, -38}}) {
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                int h = 3 - (Math.abs(dx) + Math.abs(dz)) + hash(r[0] + dx, r[1] + dz, 7) % 3 - 1;
                for (int y = 1; y <= h; y++) {
                    int k = hash(r[0] + dx, y * 7 + r[1] + dz, 9);
                    set(r[0] + dx, y, r[1] + dz, s(k < 40 ? Blocks.CALCITE : k < 75 ? Blocks.DIORITE : Blocks.TUFF));
                }
            }
        }
        // flowers and grass over the garden's lawn
        Block[] flowers = {Blocks.LILY_OF_THE_VALLEY, Blocks.OXEYE_DAISY, Blocks.PINK_TULIP, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.ALLIUM};
        for (int x = -21; x <= 21; x++) for (int z = -39; z <= -13; z++) {
            if (PLAN.containsKey(BlockPos.asLong(x, 0, z)) || PLAN.containsKey(BlockPos.asLong(x, 1, z))) continue;
            if (Math.hypot(x - TREE_X, z - TREE_Z) < 8) continue;
            int k = hash(x, z, 21);
            if (k < 9) set(x, 1, z, s(flowers[k % flowers.length]));
            else if (k < 30) set(x, 1, z, s(Blocks.SHORT_GRASS));
        }
        // a pine in each of the garden's far corners
        pine(-19, -37, 8);
        pine(19, -16, 7);
    }

    /** Whether a blueprint block is the hanging ginseng fruit. */
    public static boolean isFruit(BlockState state) {
        return state.is(ModBlocks.HANGING_GINSENG_FRUIT.get());
    }

    /** For tests: whether a blueprint state is a log (the tree's or a pine's trunk). */
    public static boolean isLog(BlockState state) {
        return state.getBlock() instanceof RotatedPillarBlock;
    }
}
