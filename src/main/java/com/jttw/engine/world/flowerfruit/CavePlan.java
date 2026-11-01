package com.jttw.engine.world.flowerfruit;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Water Curtain Cave (水帘洞), behind the waterfall in the cliff of Flower-Fruit Mountain's south
 * face: a plan of it, the same in every world (pure), laid column by column with the island
 * ({@code FlowerFruitIslandFeature}). Local x and z are from the region's centre (the cave's mouth to
 * the south, +z), y from the basin's floor ({@link IslandShape#BASIN_Y}: 0 the floor's top block).
 * <ol>
 *   <li><b>the mouth</b>, an arch in the cliff behind the falling water, a ledge before it;</li>
 *   <li><b>the iron bridge</b> (铁板桥): a chamber across which a chasm runs, the cave's stream
 *       flowing in it, an iron bridge over it, chains for its rails;</li>
 *   <li><b>the gate court</b> and its stone gate, over which stands the tablet: 花果山福地，水帘洞洞天,
 *       "Blessed Land of Flower-Fruit Mountain, Cave Heaven of Water Curtain Cave";</li>
 *   <li><b>the great hall</b>, a domed cavern of stone pillars and stalactites, the monkeys' stone
 *       tables, stools, pots and bowls, the old stone throne of the Monkey King on its dais;</li>
 *   <li><b>the dormitory</b> of stone beds to the west, the <b>store</b> of stone pots, bowls and
 *       cauldrons to the east;</li>
 *   <li>and past a warning at a tunnel's mouth off the hall, <b>the false king's sanctum</b>: where the
 *       Six-Eared Macaque sits on a throne of his own, an optional fight, the hardest there is.</li>
 * </ol>
 */
public final class CavePlan {

    private CavePlan() {}

    /** Its reach: local x, z, and y from the basin's floor. */
    public static final int X_MIN = -78, X_MAX = 78, Z_MIN = -102, Z_MAX = IslandShape.CLIFF_Z, Y_MIN = -11, Y_MAX = 34;

    /** The mouth: its half-width, height; where the passage behind it ends. */
    public static final int MOUTH_HALF = 7, MOUTH_H = 13, MOUTH_Z = 56;
    /** The bridge chamber, the chasm across it and the bridge. */
    public static final int BRIDGE_Z0 = 42, BRIDGE_Z1 = 56, BRIDGE_HALF = 24, CHASM_Z0 = 44, CHASM_Z1 = 53, CHASM_FLOOR = -10,
            STREAM_TOP = -6, DECK_HALF = 3;
    /** The gate court; the gate's wall; the doorway in it. */
    public static final int COURT_Z0 = 28, COURT_HALF = 17, GATE_Z = 30, DOOR_HALF = 4, DOOR_H = 10;
    /** The great hall: its radii and height. */
    public static final int HALL_RX = 42, HALL_RZ = 30, HALL_H = 27;
    /** The old throne's dais at the hall's north end. */
    public static final int DAIS_Z0 = -29, DAIS_Z1 = -19, DAIS_HALF = 9;
    /** The side chambers: their middles and radii. */
    public static final int SIDE_X = 59, SIDE_Z = 4, SIDE_RX = 15, SIDE_RZ = 19, SIDE_H = 13;
    /** The sanctum's tunnel: its x, from the hall north; the sanctum: middle, radius, height; its throne. */
    public static final int TUNNEL_X = 24, TUNNEL_HALF = 3, TUNNEL_H = 7;
    public static final int SANCTUM_X = 0, SANCTUM_Z = -74, SANCTUM_R = 24, SANCTUM_H = 30, SANCTUM_THRONE_Z = -94;
    /** Where the Macaque is put: the sanctum's middle. */
    public static final int BOSS_X = SANCTUM_X, BOSS_Z = SANCTUM_Z;

    /** Whether local (x, z) is within its reach (to {@code margin} more). */
    public static boolean near(int x, int z, int margin) {
        return x >= X_MIN - margin && x <= X_MAX + margin && z >= Z_MIN - margin && z <= Z_MAX + margin;
    }

    public static boolean covers(int x, int z) {
        return near(x, z, 0);
    }

    /** Whether (x, z) lies in the sanctum (no creature of the world's is born there; the Macaque is kept to it). */
    public static boolean inSanctum(int x, int z) {
        return Math.hypot(x - SANCTUM_X, z - SANCTUM_Z) <= SANCTUM_R + 1;
    }

    // ── The open space ──────────────────────────────────────────────────

    private static double wob(int x, int z, int salt) {
        return Math.sin(x * 0.37 + salt) * 0.6 + Math.sin(z * 0.29 - salt * 1.7) * 0.6 + Math.sin((x + z) * 0.13 + salt * 0.5) * 0.8;
    }

    /** The open space's ceiling over column (x, z), as y from the floor (0: no open space; the column is rock). */
    public static int ceiling(int x, int z) {
        int c = 0;
        int ax = Math.abs(x);
        // the mouth and the passage behind it, arched
        if (z >= MOUTH_Z - 2 && z <= Z_MAX && ax <= MOUTH_HALF + (z < MOUTH_Z ? 3 : 0)) {
            double arch = MOUTH_H - (x * x) / 7.0 + wob(x, z, 1) * 0.6;
            c = Math.max(c, (int) Math.round(arch));
        }
        // the bridge chamber
        if (z >= BRIDGE_Z0 && z <= BRIDGE_Z1 && ax <= BRIDGE_HALF + wob(x, z, 2)) {
            double dome = 22 - (x * x) / 40.0 - Math.pow(Math.abs(z - (BRIDGE_Z0 + BRIDGE_Z1) / 2.0), 2) / 12.0;
            c = Math.max(c, (int) Math.round(Math.max(9, dome + wob(x, z, 3))));
        }
        // the gate court
        if (z >= COURT_Z0 && z < BRIDGE_Z0 && ax <= COURT_HALF + wob(x, z, 4) * 0.7) {
            c = Math.max(c, (int) Math.round(18 - (x * x) / 30.0 + wob(x, z, 5)));
        }
        // the great hall, domed
        double hx = x / (double) HALL_RX, hz = z / (double) HALL_RZ;
        double hr = Math.sqrt(hx * hx + hz * hz) + wob(x, z, 6) * 0.025;
        if (hr <= 1) c = Math.max(c, (int) Math.round(5 + (HALL_H - 5) * Math.sqrt(1 - hr * hr) + wob(x, z, 7)));
        // the side chambers and the tunnels to them
        for (int side = -1; side <= 1; side += 2) {
            double sx = (x - side * SIDE_X) / (double) SIDE_RX, sz = (z - SIDE_Z) / (double) SIDE_RZ;
            double sr = Math.sqrt(sx * sx + sz * sz) + wob(x, z, 8 + side) * 0.03;
            if (sr <= 1) c = Math.max(c, (int) Math.round(4 + (SIDE_H - 4) * Math.sqrt(1 - sr * sr) + wob(x, z, 9)));
            if (x * side >= HALL_RX - 6 && x * side <= SIDE_X - SIDE_RX + 4 && Math.abs(z - SIDE_Z) <= 3) {
                c = Math.max(c, 7 - (z - SIDE_Z) * (z - SIDE_Z) / 4);
            }
        }
        // the tunnel north to the sanctum, and the sanctum
        if (Math.abs(x - TUNNEL_X) <= TUNNEL_HALF && z <= -12 && z >= SANCTUM_Z) c = Math.max(c, TUNNEL_H - (x - TUNNEL_X) * (x - TUNNEL_X) / 3);
        if (Math.abs(z - (SANCTUM_Z + 4)) <= TUNNEL_HALF && x >= SANCTUM_X && x <= TUNNEL_X + TUNNEL_HALF) c = Math.max(c, TUNNEL_H - (z - SANCTUM_Z - 4) * (z - SANCTUM_Z - 4) / 3);
        double sd = Math.hypot(x - SANCTUM_X, z - SANCTUM_Z) / SANCTUM_R;
        if (sd <= 1) c = Math.max(c, (int) Math.round(6 + (SANCTUM_H - 6) * Math.sqrt(1 - sd * sd)));
        return c;
    }

    /** The floor's top block's y at (x, z): 0, but for the chasm, the daises and the sanctum's throne. */
    public static int floor(int x, int z) {
        if (inChasm(x, z)) return CHASM_FLOOR;
        if (onDais(x, z)) return z <= DAIS_Z1 - 3 ? 2 : 1;
        if (inSanctum(x, z) && z <= SANCTUM_THRONE_Z + 5 && Math.abs(x) <= 7) return z <= SANCTUM_THRONE_Z + 2 ? 3 : z <= SANCTUM_THRONE_Z + 3 ? 2 : 1;
        return 0;
    }

    public static boolean inChasm(int x, int z) {
        return z >= CHASM_Z0 && z <= CHASM_Z1 && Math.abs(x) <= BRIDGE_HALF + 2;
    }

    public static boolean onDais(int x, int z) {
        return z >= DAIS_Z0 && z <= DAIS_Z1 && Math.abs(x) <= DAIS_HALF;
    }

    public static boolean onBridge(int x, int z) {
        return z >= CHASM_Z0 - 1 && z <= CHASM_Z1 + 1 && Math.abs(x) <= DECK_HALF;
    }

    // ── Who holds it ────────────────────────────────────────────────────

    public static final int GIBBON = 0, APE = 1, BABOON = 2, PILGRIM = 3;

    /** A post of the cave's garrison: where (local x, z, on the floor), what, whether a captain, its level; for a fake pilgrim, which. */
    public record Post(int x, int z, int kind, boolean captain, int level, int variant) {}

    /**
     * The garrison, put there once when the land is made and kept: monkeys in the mouth, at the
     * bridge, in the gate court, through the hall and its rooms; and the three fake pilgrims with the
     * stolen luggage before the old throne, the zone's minibosses. Never anyone in the sanctum but him.
     */
    public static final List<Post> POSTS = List.of(
            new Post(-4, 63, GIBBON, false, 20, 0), new Post(4, 61, GIBBON, false, 21, 0), new Post(0, 59, APE, false, 20, 0),
            new Post(-5, 41, APE, true, 27, 0), new Post(5, 41, APE, false, 21, 0),
            new Post(-10, 36, BABOON, false, 21, 0), new Post(10, 34, GIBBON, false, 21, 0),
            new Post(-14, 10, APE, false, 21, 0), new Post(14, 10, APE, false, 22, 0), new Post(0, 6, APE, true, 27, 0),
            new Post(-20, -6, GIBBON, false, 21, 0), new Post(20, -6, GIBBON, false, 22, 0), new Post(-8, 18, GIBBON, false, 21, 0),
            new Post(8, 18, GIBBON, false, 21, 0), new Post(-25, 14, BABOON, false, 22, 0), new Post(25, -12, BABOON, true, 28, 0),
            new Post(0, -14, PILGRIM, true, 30, 0), new Post(-6, -12, PILGRIM, true, 29, 1), new Post(6, -12, PILGRIM, true, 28, 2),
            new Post(-SIDE_X, 0, GIBBON, false, 21, 0), new Post(-SIDE_X + 4, 10, GIBBON, false, 22, 0), new Post(-SIDE_X - 3, -8, APE, false, 22, 0),
            new Post(SIDE_X, -4, BABOON, false, 22, 0), new Post(SIDE_X - 5, 8, GIBBON, false, 21, 0), new Post(SIDE_X + 4, 9, GIBBON, false, 22, 0));

    /** The fake pilgrims' health over a captain's, as the lair makes them minibosses. */
    public static final double PILGRIM_HEALTH = 1.6;

    /** The stolen luggage: two chests before the old throne (local x, z, on the floor). */
    public static final int[][] LUGGAGE = {{-3, -17}, {3, -17}};

    // ── The tablet and the signs ────────────────────────────────────────

    /** A sign of the cave: where (local x, y, z), facing, its lines. */
    public record Sign(int x, int y, int z, Direction facing, String[] lines) {}

    /** The tablet over the stone gate, and the warning at the sanctum's tunnel. */
    public static final List<Sign> SIGNS = List.of(
            new Sign(-1, 13, GATE_Z + 1, Direction.SOUTH, new String[]{"花果山福地", "Blessed Land of", "Flower-Fruit", "Mountain"}),
            new Sign(1, 13, GATE_Z + 1, Direction.SOUTH, new String[]{"水帘洞洞天", "Cave Heaven of", "Water Curtain", "Cave"}),
            new Sign(TUNNEL_X + 4, 3, -11, Direction.SOUTH, new String[]{"Beyond sits one", "who calls himself", "the Great Sage", "§4Lv 120 — beware"}));

    // ── The blocks ──────────────────────────────────────────────────────

    private static BlockState s(net.minecraft.world.level.block.Block b) {
        return b.defaultBlockState();
    }

    private static int h(int x, int y, int z) {
        long v = x * 73428767L ^ y * 912931L ^ z * 2654435761L;
        v = (v ^ (v >>> 13)) * 0x5bd1e995L;
        return (int) Math.floorMod(v ^ (v >>> 15), 1000L);
    }

    /** The cave's rock: mossy and wet low down, banded stone over. */
    public static BlockState rock(int x, int y, int z) {
        int q = h(x, y, z);
        if (y <= 2) return q < 350 ? s(Blocks.MOSSY_COBBLESTONE) : q < 600 ? s(Blocks.MOSS_BLOCK) : s(Blocks.STONE);
        return q < 120 ? s(Blocks.ANDESITE) : q < 200 ? s(Blocks.TUFF) : q < 260 ? s(Blocks.DRIPSTONE_BLOCK) : s(Blocks.STONE);
    }

    /**
     * What stands at local (x, y, z): a block, air where it is open, or null where the cave leaves the
     * mountain as it is.
     */
    @Nullable
    public static BlockState at(int x, int y, int z) {
        if (!covers(x, z) || y < Y_MIN || y > Y_MAX) return null;
        int ceil = ceiling(x, z);
        if (ceil <= 0) return null;
        int fl = floor(x, z);
        if (y > ceil + 1) return null;
        if (y == ceil + 1) return ceilingBlock(x, y, z);
        // the chasm and the stream in it
        if (inChasm(x, z)) {
            if (onBridge(x, z) || (Math.abs(x) <= DECK_HALF + 1 && (z == CHASM_Z0 - 1 || z == CHASM_Z1 + 1))) {
                BlockState b = bridge(x, y, z);
                if (b != null) return b;
            }
            if (y < CHASM_FLOOR) return null;
            if (y == CHASM_FLOOR) return h(x, y, z) < 500 ? s(Blocks.GRAVEL) : s(Blocks.MOSSY_COBBLESTONE);
            if (y <= STREAM_TOP) return s(Blocks.WATER);
            if (Math.abs(x) >= BRIDGE_HALF && y <= 0) return rock(x, y, z);
            return s(Blocks.AIR);
        }
        if (y < fl) return floorFill(x, y, z);
        if (y == fl) return floorBlock(x, z, fl);
        // over the floor: the works, else open
        BlockState w = works(x, y, z, fl, ceil);
        return w != null ? w : s(Blocks.AIR);
    }

    private static BlockState floorFill(int x, int y, int z) {
        if (onDais(x, z) || inSanctum(x, z)) return s(Blocks.POLISHED_ANDESITE);
        return rock(x, y, z);
    }

    private static BlockState floorBlock(int x, int z, int fl) {
        int q = h(x, 0, z);
        if (inSanctum(x, z)) {
            double d = Math.hypot(x - SANCTUM_X, z - SANCTUM_Z);
            if (fl > 0) return Math.abs(x) <= 1 ? s(Blocks.RED_WOOL) : s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            if (Math.abs(d - 9) < 0.6) return s(Blocks.SHROOMLIGHT);                    // a ring of light let into the floor
            if (Math.abs(d - 17) < 0.6) return s(Blocks.GILDED_BLACKSTONE);
            return q < 600 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : q < 850 ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        }
        if (onDais(x, z)) return Math.abs(x) <= 1 ? s(Blocks.YELLOW_WOOL) : s(Blocks.POLISHED_ANDESITE);
        // a way of stone flags from the mouth to the dais down the hall's middle
        if (Math.abs(x) <= 2 && z <= Z_MAX && z >= DAIS_Z1) return q < 700 ? s(Blocks.STONE_BRICKS) : s(Blocks.MOSSY_STONE_BRICKS);
        if (Math.abs(x - TUNNEL_X) <= 1 && z < -10) return s(Blocks.POLISHED_BLACKSTONE_BRICKS);
        return q < 260 ? s(Blocks.MOSS_BLOCK) : q < 420 ? s(Blocks.MOSSY_COBBLESTONE) : q < 520 ? s(Blocks.ANDESITE) : s(Blocks.STONE);
    }

    private static BlockState ceilingBlock(int x, int y, int z) {
        int q = h(x, y, z);
        // light: glowing stone set in the vault here and there (in the sanctum, a dimmer, bluer light)
        if (q < 45 && ceiling(x, z) > 6) return inSanctum(x, z) ? s(Blocks.SEA_LANTERN) : s(Blocks.SHROOMLIGHT);
        if (q < 70) return s(Blocks.DRIPSTONE_BLOCK);
        return rock(x, y, z);
    }

    /** The iron bridge over the chasm: its deck, chain rails, posts with lanterns, and its pier. */
    @Nullable
    private static BlockState bridge(int x, int y, int z) {
        int ax = Math.abs(x);
        boolean end = z == CHASM_Z0 - 1 || z == CHASM_Z1 + 1;
        int mid = (CHASM_Z0 + CHASM_Z1) / 2;
        if (y == 0) return ax <= DECK_HALF - 1 ? s(Blocks.IRON_BLOCK) : s(Blocks.POLISHED_DEEPSLATE);
        if (y == -1 && ax <= DECK_HALF) return (z + x) % 2 == 0 ? s(Blocks.IRON_BARS) : s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z);
        // the pier in the middle of the stream: stone under the deck, the water running round it
        if (y < -1 && y >= CHASM_FLOOR && ax <= 1 && Math.abs(z - mid) <= 1) return s(Blocks.STONE_BRICKS);
        if (y == 1 && ax == DECK_HALF) return end ? s(Blocks.IRON_BARS) : s(Blocks.CHAIN).setValue(ChainBlock.AXIS, Direction.Axis.Z);
        if (end && ax == DECK_HALF) {
            if (y == 2) return s(Blocks.IRON_BARS);
            if (y == 3) return s(Blocks.LANTERN);
        }
        if (y >= 1 && y <= 3 && ax < DECK_HALF) return s(Blocks.AIR);
        return null;
    }

    /** What stands over the floor at (x, y, z) in the cave's rooms: furnishings, pillars, lamps; null where open. */
    @Nullable
    private static BlockState works(int x, int y, int z, int fl, int ceil) {
        int ax = Math.abs(x);
        int dy = y - fl;
        // the stone gate and the tablet over it
        if (z >= GATE_Z - 1 && z <= GATE_Z && z >= COURT_Z0 - 1 && ax <= COURT_HALF + 2) {
            boolean door = ax <= DOOR_HALF && dy <= DOOR_H - (ax == DOOR_HALF ? 1 : 0);
            if (door) return null;
            // the tablet: a dark plaque framed in gold, close round its two halves of the inscription, a lintel of carved stone over it
            if (dy >= 12 && dy <= 14 && ax <= 4) return ax == 4 || dy == 12 || dy == 14 ? s(Blocks.GOLD_BLOCK)
                    : z == GATE_Z ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.STONE_BRICKS);
            if (dy == 15 && ax <= 5) return s(Blocks.CHISELED_STONE_BRICKS);
            if (ax == DOOR_HALF + 1 || ax == DOOR_HALF + 2) return s(Blocks.CHISELED_STONE_BRICKS);
            if (dy == DOOR_H + 1 && ax <= DOOR_HALF + 2) return s(Blocks.CHISELED_STONE_BRICKS);
            return h(x, y, z) < 300 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
        }
        // two stone monkeys squatting either side of the gate, watching the court
        if (z >= GATE_Z + 3 && z <= GATE_Z + 5 && ax >= 7 && ax <= 9) {
            BlockState m = statue(ax - 8, dy, z - GATE_Z - 4);
            if (m != null) return m;
        }
        // braziers in the court
        if ((ax == 12) && (z == COURT_Z0 + 4 || z == BRIDGE_Z0 - 3)) return dy == 1 ? s(Blocks.STONE_BRICK_WALL) : dy == 2 ? s(Blocks.CAMPFIRE) : null;
        if (x == TUNNEL_X + 4 && z == -12 && dy >= 1 && dy <= 5) return dy == 5 ? s(Blocks.SOUL_LANTERN) : s(Blocks.CHISELED_STONE_BRICKS);
        // the great hall
        double hx = x / (double) HALL_RX, hz = z / (double) HALL_RZ;
        double hr = Math.sqrt(hx * hx + hz * hz);
        if (hr <= 1 && z < COURT_Z0) {
            // the pillars: great columns of dripstone round the hall, floor to vault
            for (int k = 0; k < 10; k++) {
                double a = k * Math.PI * 2 / 10 + 0.31;
                int px = (int) Math.round(Math.cos(a) * HALL_RX * 0.66), pz = (int) Math.round(Math.sin(a) * HALL_RZ * 0.66);
                if (Math.abs(pz - (DAIS_Z0 + DAIS_Z1) / 2) < 7 && Math.abs(px) < DAIS_HALF + 4) continue;
                double d = Math.hypot(x - px, z - pz);
                double r = 2.3 + 1.2 * Math.max(0, Math.min(1, Math.abs(dy - ceil / 2.0) / (ceil / 2.0))); // waisted, flaring to floor and vault
                if (d <= r) return h(x, y, z) < 500 ? s(Blocks.DRIPSTONE_BLOCK) : (dy % 5 == 0 ? s(Blocks.CALCITE) : s(Blocks.STONE));
            }
            // the old stone throne of the Monkey King on its dais, and its back
            if (onDais(x, z)) {
                BlockState t = throne(x, dy, z - (DAIS_Z0 + 2), false);
                if (t != null) return t;
            }
            // yellow banners of the Great Sage behind the dais
            if (z == DAIS_Z0 - 1 && (ax == 6 || ax == 10) && dy >= 4 && dy <= 8) return dy == 8 ? s(Blocks.GOLD_BLOCK) : s(Blocks.YELLOW_WOOL);
            // stone tables with stools and the monkeys' stone pots and bowls on them
            BlockState f = furniture(x, dy, z);
            if (f != null) return f;
            // lamps hung on chains from the vault
            if (h(x, 0, z) < 14 && ax > 4 && dy >= ceil - 5) return dy == ceil - 5 ? s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true) : s(Blocks.CHAIN);
            // stalactites
            if (h(x, 1, z) < 35 && dy >= ceil - 3 && ceil > 9) return drip(ceil - dy);
            return null;
        }
        // the side chambers: the dormitory of stone beds (west), the store of stone pots and bowls (east)
        for (int side = -1; side <= 1; side += 2) {
            double sx = (x - side * SIDE_X) / (double) SIDE_RX, sz = (z - SIDE_Z) / (double) SIDE_RZ;
            if (sx * sx + sz * sz <= 1) {
                BlockState b = side < 0 ? dormitory(x + SIDE_X, dy, z - SIDE_Z) : store(x - SIDE_X, dy, z - SIDE_Z);
                if (b != null) return b;
                if (h(x, 2, z) < 30 && dy >= ceil - 2 && ceil > 7) return drip(ceil - dy);
                return null;
            }
        }
        // the sanctum
        if (inSanctum(x, z)) return sanctum(x - SANCTUM_X, dy, z - SANCTUM_Z, fl, ceil);
        // the warning's post at the tunnel's mouth
        if (x == TUNNEL_X + 4 && z == -12 && dy >= 1 && dy <= 5) return dy == 5 ? s(Blocks.SOUL_LANTERN) : s(Blocks.CHISELED_STONE_BRICKS);
        // the tunnel's lamps
        if (Math.abs(x - TUNNEL_X) == TUNNEL_HALF - 1 && z < -14 && Math.floorMod(z, 8) == 0 && dy == 3) return s(Blocks.SOUL_LANTERN);
        // the mouth's passage: moss and dripping stone; lamps either side of the mouth
        if (z >= MOUTH_Z && ax == MOUTH_HALF - 1 && Math.floorMod(z, 6) == 0 && dy == 1) return s(Blocks.LANTERN);
        return null;
    }

    private static BlockState drip(int fromTop) {
        DripstoneThickness t = fromTop == 0 ? DripstoneThickness.BASE : fromTop == 1 ? DripstoneThickness.MIDDLE
                : fromTop == 2 ? DripstoneThickness.FRUSTUM : DripstoneThickness.TIP;
        return s(Blocks.POINTED_DRIPSTONE).setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN).setValue(PointedDripstoneBlock.THICKNESS, t);
    }

    /** A squatting stone monkey (dx, dz from its middle; dy up from the floor). */
    @Nullable
    private static BlockState statue(int dx, int dy, int dz) {
        if (dy == 1 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) return s(Blocks.CHISELED_STONE_BRICKS);
        if (dy == 2 && Math.abs(dx) <= 1 && dz >= 0) return dz == 1 ? s(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.NORTH) : s(Blocks.ANDESITE);
        if (dy == 3 && dx == 0 && dz == 0) return s(Blocks.ANDESITE);
        if (dy == 3 && dx == 0 && dz == 1) return s(Blocks.POLISHED_ANDESITE);
        if (dy == 4 && dx == 0 && dz >= 0 && dz <= 1) return dz == 1 ? s(Blocks.ANDESITE_WALL) : s(Blocks.POLISHED_ANDESITE);
        return null;
    }

    /** A stone throne (dx from its middle, dz from its seat's back; the false king's of gold and black). */
    @Nullable
    private static BlockState throne(int dx, int dy, int dz, boolean dark) {
        int ax = Math.abs(dx);
        BlockState stone = dark ? s(Blocks.POLISHED_BLACKSTONE) : s(Blocks.POLISHED_ANDESITE);
        BlockState trim = dark ? s(Blocks.GOLD_BLOCK) : s(Blocks.CHISELED_STONE_BRICKS);
        if (ax > 3 || dz < -1 || dz > 2) return null;
        if (dz <= -1) {                                        // the back, tall, crowned
            if (dy <= 6 - ax && ax <= 3) return dy == 6 - ax || ax == 3 ? trim : stone;
            return null;
        }
        if (dz == 0 && ax <= 1 && dy == 1) return (dark ? s(Blocks.POLISHED_BLACKSTONE_STAIRS) : s(Blocks.POLISHED_ANDESITE_STAIRS))
                .setValue(StairBlock.FACING, Direction.NORTH);
        if (ax == 2 && dz <= 1 && dy <= 2) return dy == 2 ? trim : stone;     // its arms
        return null;
    }

    /** Stone tables, stools, pots and bowls round the hall (dy up from the floor). */
    @Nullable
    private static BlockState furniture(int x, int dy, int z) {
        // tables in two rows either side of the way, every 10 blocks
        int ax = Math.abs(x);
        if (ax < 10 || ax > 28 || z < -16 || z > 20) return null;
        int tx = (ax - 10) % 9, tz = Math.floorMod(z + 16, 12);
        if (tx > 4 || tz > 6) return null;
        boolean table = tx >= 1 && tx <= 3 && tz >= 2 && tz <= 4;
        if (table) {
            if (dy == 1) return tx == 2 && tz == 3 ? s(Blocks.STONE_BRICKS) : null;
            if (dy == 2) return s(Blocks.SMOOTH_STONE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP);
            if (dy == 3) {
                int q = h(x, 3, z);
                if (q < 220) return s(Blocks.FLOWER_POT);
                if (q < 300) return s(Blocks.DECORATED_POT);
                if (q < 360) return s(Blocks.CAULDRON);
                return null;
            }
            return null;
        }
        // stools: a stone slab each side of a table
        if (dy == 1 && (tz == 3 && (tx == 0 || tx == 4) || tx == 2 && (tz == 0 || tz == 6))) return s(Blocks.STONE_SLAB);
        return null;
    }

    /** The dormitory: rows of stone beds (a stone slab of a bed, moss for its straw, a stone pillow). */
    @Nullable
    private static BlockState dormitory(int dx, int dy, int dz) {
        if (dy < 1 || dy > 2) return null;
        int bx = Math.floorMod(dx + 9, 4), bz = Math.floorMod(dz + 13, 5);
        if (Math.abs(dx) > 10 || Math.abs(dz) > 14 || bx > 1 || bz > 2) return null;
        if (bx == 1) return null;
        if (dy == 1) return s(Blocks.SMOOTH_STONE);
        return bz == 0 ? s(Blocks.SMOOTH_STONE_SLAB) : s(Blocks.MOSS_CARPET);
    }

    /** The store: stone pots and bowls, cauldrons, jars of wine, the monkeys' arms against the wall. */
    @Nullable
    private static BlockState store(int dx, int dy, int dz) {
        if (Math.abs(dx) > 11 || Math.abs(dz) > 15) return null;
        int q = h(dx, 9, dz);
        // a great stone hearth in the middle, its cauldron over it
        if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
            if (dy == 1) return dx == 0 && dz == 0 ? s(Blocks.CAMPFIRE) : s(Blocks.STONE_BRICKS);
            if (dy == 2 && dx == 0 && dz == 0) return null;
            return null;
        }
        if (dy == 1 && (Math.abs(dx) == 6 || Math.abs(dz) == 9) && (dx + dz) % 2 == 0) {
            if (q < 300) return s(Blocks.CAULDRON);
            if (q < 520) return s(Blocks.DECORATED_POT);
            if (q < 700) return s(Blocks.FLOWER_POT);
            if (q < 820) return s(Blocks.BARREL);
            return s(Blocks.STONE_SLAB);
        }
        if (dy == 1 && Math.abs(dz) == 13 && Math.abs(dx) <= 8 && dx % 3 == 0) return s(Blocks.ANVIL);
        if (dy >= 1 && dy <= 3 && Math.abs(dz) == 14 && Math.abs(dx) <= 8 && dx % 2 == 0) return dy == 3 ? s(Blocks.LIGHTNING_ROD) : s(Blocks.IRON_BARS);
        return null;
    }

    /** The false king's sanctum (dx, dz from its middle): its throne of gold and black, six pillars, soul fire. */
    @Nullable
    private static BlockState sanctum(int dx, int dy, int dz, int fl, int ceil) {
        // his throne on its steps at the north
        if (dz <= SANCTUM_THRONE_Z - SANCTUM_Z + 3 && Math.abs(dx) <= 4) {
            BlockState t = throne(dx, dy, dz - (SANCTUM_THRONE_Z - SANCTUM_Z + 1), true);
            if (t != null) return t;
        }
        // six pillars round the arena, one for each of his ears, gold-banded black stone, soul fire on their heads
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3 + Math.PI / 6;
            int px = (int) Math.round(Math.cos(a) * 19), pz = (int) Math.round(Math.sin(a) * 19);
            double d = Math.hypot(dx - px, dz - pz);
            if (d <= 1.6) {
                if (dy == ceil - 8 + fl) return s(Blocks.GOLD_BLOCK);
                if (dy == 4 || dy == 11) return s(Blocks.SHROOMLIGHT);              // bands of light round each pillar
                return dy % 6 == 0 ? s(Blocks.GILDED_BLACKSTONE) : s(Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
            if (d <= 2.6 && dy == 1 && ((dx - px + dz - pz) % 2 == 0)) return s(Blocks.SOUL_CAMPFIRE);
        }
        // eight braziers round the arena
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            int bx = (int) Math.round(Math.cos(a) * 13), bz = (int) Math.round(Math.sin(a) * 13);
            if (dx == bx && dz == bz && dz > SANCTUM_THRONE_Z - SANCTUM_Z + 6) return dy == 1 ? s(Blocks.GILDED_BLACKSTONE) : dy == 2 ? s(Blocks.CAMPFIRE) : null;
        }
        // braziers either side of his throne's steps
        if (Math.abs(dx) == 6 && dz == SANCTUM_THRONE_Z - SANCTUM_Z + 6) return dy == 1 ? s(Blocks.GILDED_BLACKSTONE) : dy == 2 ? s(Blocks.CAMPFIRE) : null;
        // torn yellow banners behind his throne, on the wall
        if (dz == SANCTUM_THRONE_Z - SANCTUM_Z - 2 && Math.abs(dx) >= 6 && Math.abs(dx) <= 10 && Math.abs(dx) % 2 == 0 && dy >= 4 && dy <= 9) {
            return dy == 9 ? s(Blocks.GOLD_BLOCK) : (dy + dx) % 4 == 0 ? s(Blocks.ORANGE_WOOL) : s(Blocks.YELLOW_WOOL);
        }
        return null;
    }
}
