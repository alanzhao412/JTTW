package com.jttw.engine.world.hell;

import com.jttw.engine.entity.hell.HellKings;
import com.jttw.engine.entity.hell.HellMonster;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Hell's works, laid through a {@link Site} (a chunk at a time as the land is made; whole in the
 * gallery): the arrival plaza and the Gate of Ghosts; the road north, lit by soul lanterns; the
 * Bridge of Helplessness over the Naihe, with Meng Po's soup pavilion at its north end; and the
 * fortress of the Ten Courts ({@link TenCourts}): its walls, gatehouse, avenue and braziers, the
 * Office of Records, the ten court halls each with its judge's bench and throne, ledgers, scrolls,
 * braziers and the instruments of its court, and the Wheel of Reincarnation. The courts' guards
 * and their kings are put there once, as the land is made.
 *
 * <p>Everything is worked out from the plan and {@link HellTerrain} alone (the ground's height
 * included), so the chunks agree whichever is made first.
 */
public final class HellWorks {

    private HellWorks() {}

    static final int G = HellTerrain.GROUND;

    private static BlockState b(net.minecraft.world.level.block.Block block) {
        return block.defaultBlockState();
    }

    private static final BlockState AIR = b(Blocks.AIR), BRICK = b(Blocks.POLISHED_BLACKSTONE_BRICKS),
            CRACKED = b(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS), POLISHED = b(Blocks.POLISHED_BLACKSTONE),
            CHISELED = b(Blocks.CHISELED_POLISHED_BLACKSTONE), BLACKSTONE = b(Blocks.BLACKSTONE), GILDED = b(Blocks.GILDED_BLACKSTONE),
            RED = b(Blocks.RED_NETHER_BRICKS), DEEP = b(Blocks.DEEPSLATE_BRICKS), TILES = b(Blocks.DEEPSLATE_TILES),
            PLANKS = b(Blocks.DARK_OAK_PLANKS), SHELF = b(Blocks.BOOKSHELF), GOLD = b(Blocks.GOLD_BLOCK),
            CARPET = b(Blocks.RED_CARPET), BASALT = b(Blocks.POLISHED_BASALT), SOUL_SOIL = b(Blocks.SOUL_SOIL),
            WALL = b(Blocks.POLISHED_BLACKSTONE_WALL), BRICK_WALL = b(Blocks.POLISHED_BLACKSTONE_BRICK_WALL);

    private static BlockState slab(net.minecraft.world.level.block.Block s, SlabType t) {
        return s.defaultBlockState().setValue(SlabBlock.TYPE, t);
    }

    private static BlockState stair(net.minecraft.world.level.block.Block s, Direction facing, boolean top) {
        return s.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState hanging(net.minecraft.world.level.block.Block lantern) {
        return lantern.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    /** Iron bars drawn across a wall running along x ({@code alongX}) or along z. */
    private static BlockState bars(boolean alongX) {
        return Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, alongX).setValue(IronBarsBlock.WEST, alongX)
                .setValue(IronBarsBlock.NORTH, !alongX).setValue(IronBarsBlock.SOUTH, !alongX);
    }

    /** A brazier: a post of polished blackstone, a fire in a soul campfire (or a red one) on it; its foot on {@code y}. */
    static void brazier(Site s, int x, int y, int z, boolean soul) {
        s.set(x, y, z, WALL);
        s.set(x, y + 1, z, b(soul ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE));
    }

    /** Lays everything of Hell's works that reaches into the site. */
    public static void build(Site s, long seed) {
        if (s.touches(-26, -26, 26, 26)) plaza(s);
        if (s.touches(-6, -26, 6, TenCourts.GATE_Z + 1)) road(s);
        int[] bridge = HellTerrain.bridgeEnds();
        if (s.touches(-16, bridge[0] - 14, 8, bridge[1] + 2)) bridge(s);
        if (s.touches(-TenCourts.HALF_WIDTH - 4, TenCourts.GATE_Z + 6, TenCourts.HALF_WIDTH + 4, TenCourts.NORTH_Z - 4)) fortress(s);
    }

    // ── The arrival plaza and the Gate of Ghosts ─────────────────────────

    /** How far the plaza's paving reaches from the arrival. */
    public static final int PLAZA_RADIUS = 22;

    private static void plaza(Site s) {
        int ax = HellTerrain.ARRIVAL_X, az = HellTerrain.ARRIVAL_Z;
        for (int dx = -PLAZA_RADIUS; dx <= PLAZA_RADIUS; dx++) for (int dz = -PLAZA_RADIUS; dz <= PLAZA_RADIUS; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > PLAZA_RADIUS + 0.5) continue;
            int x = ax + dx, z = az + dz;
            int ring = (int) Math.round(d);
            BlockState floor = ring == PLAZA_RADIUS ? BLACKSTONE : ring % 6 == 0 ? CHISELED : ring % 6 == 3 ? BASALT
                    : HellTerrain.grain(x, G, z) < 0.12 ? CRACKED : BRICK;
            s.set(x, G, z, floor);
            for (int y = G + 1; y <= G + 8; y++) s.set(x, y, z, AIR);
        }
        // soul lanterns on posts round its rim, leaving the way north open
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6;
            int x = ax + (int) Math.round(Math.sin(a) * (PLAZA_RADIUS - 3)), z = az + (int) Math.round(Math.cos(a) * (PLAZA_RADIUS - 3));
            if (Math.abs(x - ax) < 6 && z < az) continue;
            s.set(x, G + 1, z, BRICK_WALL);
            s.set(x, G + 2, z, BRICK_WALL);
            s.set(x, G + 3, z, b(Blocks.SOUL_LANTERN));
        }
        // the Gate of Ghosts: an arch at the plaza's north edge, where the road begins
        int gz = az - PLAZA_RADIUS + 1;
        for (int side : new int[]{-1, 1}) {
            int px = ax + side * 4;
            for (int y = G + 1; y <= G + 7; y++) s.set(px, y, gz, y == G + 1 ? GILDED : RED);
            brazier(s, ax + side * 6, G + 1, gz + 1, true);
        }
        for (int x = ax - 5; x <= ax + 5; x++) {
            s.set(x, G + 8, gz, BRICK);
            s.set(x, G + 9, gz, x == ax - 5 || x == ax + 5 ? GILDED : slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM));
            s.set(x, G + 8, gz - 1, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, false));
            s.set(x, G + 8, gz + 1, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, false));
        }
        for (int x = ax - 2; x <= ax + 2; x++) s.set(x, G + 7, gz, x == ax ? GOLD : BRICK);
        s.set(ax, G + 6, gz, hanging(Blocks.SOUL_LANTERN));
        s.sign(ax - 1, G + 7, gz + 1, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH),
                "鬼门关", "The Gate", "of Ghosts");
        s.sign(ax + 1, G + 7, gz + 1, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH),
                "The Ten Courts", "lie north,", "over the Naihe.");
    }

    // ── The road ─────────────────────────────────────────────────────────

    private static void road(Site s) {
        int[] br = HellTerrain.bridgeEnds();
        for (int z = HellTerrain.ARRIVAL_Z - PLAZA_RADIUS; z >= TenCourts.GATE_Z + 1; z--) {
            if (z >= br[0] && z <= br[1]) continue;
            for (int x = -HellTerrain.ROAD_HALF + 1; x <= HellTerrain.ROAD_HALF - 1; x++) {
                if (!s.has(x, z)) continue;
                boolean edge = Math.abs(x) == HellTerrain.ROAD_HALF - 1;
                s.set(x, G, z, edge ? BLACKSTONE : x == 0 && z % 4 == 0 ? CHISELED : HellTerrain.grain(x, G, z) < 0.15 ? CRACKED : BRICK);
                for (int y = G + 1; y <= G + 4; y++) s.set(x, y, z, AIR);
            }
            // a soul lantern on a post every 24 blocks, either side
            if (Math.floorMod(z, 24) == 0 && z < HellTerrain.ARRIVAL_Z - PLAZA_RADIUS - 4) {
                for (int side : new int[]{-1, 1}) {
                    int x = side * (HellTerrain.ROAD_HALF + 1);
                    s.set(x, G, z, BRICK);
                    s.set(x, G + 1, z, BRICK_WALL);
                    s.set(x, G + 2, z, BRICK_WALL);
                    s.set(x, G + 3, z, b(Blocks.SOUL_LANTERN));
                }
            }
        }
    }

    // ── The Bridge of Helplessness (奈何桥) and Meng Po's pavilion ───────

    private static void bridge(Site s) {
        int[] br = HellTerrain.bridgeEnds();
        for (int z = br[0]; z <= br[1]; z++) {
            int rise = HellTerrain.bridgeRise(z);
            int top = G + rise / 2; // the top full block of the deck
            for (int x = -3; x <= 3; x++) {
                if (!s.has(x, z)) continue;
                boolean rail = Math.abs(x) == 3;
                // the deck, two blocks thick, and nothing over it
                s.set(x, top - 1, z, BRICK);
                s.set(x, top, z, rail ? BRICK : x == 0 ? CHISELED : POLISHED);
                for (int y = top + 1; y <= top + 5; y++) s.set(x, y, z, AIR);
                if (rise % 2 == 1) s.set(x, top + 1, z, rail ? BRICK : slab(Blocks.POLISHED_BLACKSTONE_SLAB, SlabType.BOTTOM));
                int walk = top + 1 + (rise % 2);
                if (rail) {
                    s.set(x, walk, z, BRICK_WALL);
                    if (Math.floorMod(z - br[0], 7) == 0) s.set(x, walk + 1, z, b(Blocks.SOUL_LANTERN));
                }
                // piers down to the river's bed, every seven blocks
                if (Math.floorMod(z - br[0], 7) == 3 && rail) {
                    for (int y = top - 2; y > HellTerrain.SEA_LEVEL - 9 && y >= 1; y--) {
                        BlockState here = s.get(x, y, z);
                        if (!here.isAir() && here.getFluidState().isEmpty() && y < HellTerrain.SEA_LEVEL - 6) break;
                        s.set(x, y, z, BRICK);
                    }
                }
            }
        }
        // its name, at each end
        for (int end : new int[]{0, 1}) {
            int z = end == 0 ? br[0] - 1 : br[1] + 1;
            s.set(4, G, z, BRICK);
            s.sign(4, G + 1, z, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, end == 0 ? 0 : 8),
                    "奈何桥", "The Bridge of", "Helplessness", "");
        }
        // Meng Po's pavilion, by the north end, where every soul drinks the soup of forgetting
        int px = -9, pz = br[0] - 6, ground = HellTerrain.surface(px, pz);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            s.set(px + dx, ground, pz + dz, Math.abs(dx) == 3 || Math.abs(dz) == 3 ? BLACKSTONE : POLISHED);
            for (int y = ground + 1; y <= ground + 6; y++) s.set(px + dx, y, pz + dz, AIR);
            for (int y = ground - 1; y > ground - 4; y--) if (!s.get(px + dx, y, pz + dz).isSolid()) s.set(px + dx, y, pz + dz, BLACKSTONE);
        }
        for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            for (int y = ground + 1; y <= ground + 3; y++) s.set(px + c[0], y, pz + c[1], RED);
        }
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            boolean rim = Math.abs(dx) == 4 || Math.abs(dz) == 4;
            s.set(px + dx, ground + 4, pz + dz, rim ? slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM) : TILES);
            if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) s.set(px + dx, ground + 5, pz + dz, Math.abs(dx) == 2 || Math.abs(dz) == 2
                    ? slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM) : TILES);
        }
        s.set(px, ground + 6, pz, GILDED);
        s.set(px, ground + 1, pz, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        s.set(px - 1, ground + 1, pz, b(Blocks.CAMPFIRE));
        s.set(px + 1, ground + 1, pz, b(Blocks.BREWING_STAND));
        s.set(px, ground + 1, pz - 2, b(Blocks.BARREL));
        s.set(px, ground + 3, pz, hanging(Blocks.LANTERN));
        s.sign(px + 3, ground + 1, pz + 4, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 4),
                "孟婆汤", "Meng Po's Soup", "Drink, and", "forget.");
    }

    // ── The fortress of the Ten Courts ───────────────────────────────────

    private static void fortress(Site s) {
        int hw = TenCourts.HALF_WIDTH, gz = TenCourts.GATE_Z, nz = TenCourts.NORTH_Z, t = TenCourts.WALL_THICK, h = TenCourts.WALL_HEIGHT;
        // the walls
        if (s.touches(-hw, gz, hw, nz)) {
            for (int x = Math.max(-hw, s.minX); x <= Math.min(hw, s.maxX); x++) for (int z = Math.max(nz, s.minZ); z <= Math.min(gz, s.maxZ); z++) {
                boolean wall = Math.abs(x) > hw - t || z > gz - t || z < nz + t;
                if (!wall) continue;
                boolean outer = Math.abs(x) == hw || z == gz || z == nz;
                boolean gate = Math.abs(x) <= 3 && z > gz - t;
                for (int y = G + 1; y <= G + h; y++) {
                    if (gate && y <= G + 7) {
                        s.set(x, y, z, AIR);
                        continue;
                    }
                    s.set(x, y, z, y == G + 1 ? BLACKSTONE : y == G + h ? POLISHED
                            : HellTerrain.grain(x, y, z) < 0.1 ? CRACKED : BRICK);
                }
                if (gate) s.set(x, G, z, POLISHED);
                // merlons on the outer edge, a walk behind them
                if (outer && Math.floorMod(x + z, 2) == 0) s.set(x, G + h + 1, z, BRICK);
                // lanterns hung along the outer face
                if (outer && Math.floorMod(x + z, 12) == 0 && !gate) s.set(x + (Math.abs(x) == hw ? Integer.signum(x) : 0), G + h - 2,
                        z + (z == gz ? 1 : z == nz ? -1 : 0), b(Blocks.SOUL_LANTERN));
            }
            // towers at the corners
            for (int cx : new int[]{-hw, hw}) for (int cz : new int[]{gz, nz}) tower(s, cx - Integer.signum(cx) * 2, cz + (cz == gz ? -2 : 2));
            gatehouse(s);
        }
        // the ground within: the forecourt and the avenue paved, a path to each hall's door
        int yamaDoor = TenCourts.YAMA_Z + TenCourts.YAMA_HALF_Z;
        for (int x = Math.max(-hw + t, s.minX); x <= Math.min(hw - t, s.maxX); x++)
            for (int z = Math.max(nz + t, s.minZ); z <= Math.min(gz - t, s.maxZ); z++) {
                boolean forecourt = z > gz - t - 14 && Math.abs(x) <= 20;
                boolean avenue = Math.abs(x) <= TenCourts.AVENUE_HALF && z >= yamaDoor;
                if (forecourt || avenue) {
                    s.set(x, G, z, Math.abs(x) <= 1 && z % 3 == 0 ? CHISELED : Math.abs(x) == TenCourts.AVENUE_HALF && avenue && !forecourt ? GILDED
                            : HellTerrain.grain(x, G, z) < 0.1 ? CRACKED : BRICK);
                }
            }
        // braziers down the avenue, both sides, every twelve blocks
        for (int z = gz - t - 6; z > yamaDoor + 4; z -= 12) {
            for (int side : new int[]{-1, 1}) brazier(s, side * (TenCourts.AVENUE_HALF + 1), G + 1, z, z % 24 == 0);
        }
        // the halls
        for (TenCourts.Court c : TenCourts.HALLS) {
            int reach = Math.max(c.halfX(), c.halfZ()) + 14;
            if (s.touches(c.x() - reach, c.z() - reach, c.x() + reach, c.z() + reach)) hall(s, c);
        }
        int[] w = TenCourts.wheel();
        if (s.touches(w[0] - 9, w[1] - 3, w[0] + 9, w[1] + 3)) wheel(s, w[0], w[1]);
    }

    private static void tower(Site s, int x, int z) {
        int top = G + TenCourts.WALL_HEIGHT + 5;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            for (int y = G + 1; y <= top; y++) s.set(x + dx, y, z + dz, edge || y == top ? (Math.abs(dx) == 3 && Math.abs(dz) == 3 ? RED : BRICK) : AIR);
            if (edge && Math.floorMod(dx + dz, 2) == 0) s.set(x + dx, top + 1, z + dz, BRICK);
        }
        brazier(s, x, top + 1, z, true);
    }

    private static void gatehouse(Site s) {
        int gz = TenCourts.GATE_Z, y0 = G + TenCourts.WALL_HEIGHT + 1;
        // a hall over the gate: red posts, a roof of dark tiles, a plaque over the way in
        for (int x = -9; x <= 9; x++) for (int z = gz - 5; z <= gz + 1; z++) {
            boolean post = (Math.abs(x) == 9 || Math.abs(x) == 4) && (z == gz - 5 || z == gz + 1);
            for (int y = y0; y <= y0 + 3; y++) s.set(x, y, z, post ? RED : AIR);
        }
        roof(s, 0, gz - 2, 11, 5, y0 + 4, true);
        for (int side : new int[]{-1, 1}) {
            brazier(s, side * 6, G + 1, gz + 2, false);
            for (int y = G + 1; y <= G + 8; y++) s.set(side * 4, y, gz + 1, RED);
        }
        s.set(0, G + 8, gz + 1, GOLD);
        s.sign(-1, G + 8, gz + 1, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH),
                "地狱十殿", "The Ten Courts", "of Hell");
        s.sign(1, G + 8, gz + 1, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH),
                "All souls", "are judged", "here.");
        s.set(0, G + 7, gz + 1, hanging(Blocks.SOUL_LANTERN));
    }

    /**
     * A hip roof of dark tiles, rising half a block a ring: over (x, z) ± (halfX, halfZ), its eaves at
     * {@code y}; a ridge of blackstone, gilded at its ends; the corners' eaves turned up.
     */
    static void roof(Site s, int x, int z, int halfX, int halfZ, int y, boolean flare) {
        int rings = Math.min(halfX, halfZ);
        for (int k = 0; k <= rings; k++) {
            int ax = halfX - k, az = halfZ - k, ry = y + k / 2;
            BlockState tile = k == rings ? TILES : slab(Blocks.DEEPSLATE_TILE_SLAB, k % 2 == 0 ? SlabType.BOTTOM : SlabType.TOP);
            for (int dx = -ax; dx <= ax; dx++) for (int dz = -az; dz <= az; dz++) {
                if (k < rings && Math.abs(dx) != ax && Math.abs(dz) != az) continue;
                s.set(x + dx, ry, z + dz, tile);
                if (k == rings) s.set(x + dx, ry + 1, z + dz, Math.abs(dx) == ax && Math.abs(dz) == az && (ax > 0 || az > 0) ? GILDED : POLISHED);
            }
        }
        // the eaves' corners turned up a little
        if (flare) for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            s.set(x + c[0] * halfX, y, z + c[1] * halfZ, slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, SlabType.TOP));
        }
    }

    // ── A hall ───────────────────────────────────────────────────────────

    /** A hall's own frame: u toward its door, v across; (0, 0) its middle. */
    private record Frame(int cx, int cz, int fx, int fz) {
        int x(int u, int v) {
            return cx + fx * u - fz * v;
        }

        int z(int u, int v) {
            return cz + fz * u + fx * v;
        }

        Direction forward() {
            return Site.dir(fx, fz);
        }

        /** The direction of +v. */
        Direction right() {
            return Site.dir(-fz, fx);
        }

        void set(Site s, int u, int y, int v, BlockState st) {
            s.set(x(u, v), y, z(u, v), st);
        }
    }

    /** How tall a hall's walls are: a side hall's, and King Yama's. */
    public static final int HALL_WALL = 7, YAMA_WALL = 10;

    private static void hall(Site s, TenCourts.Court c) {
        Frame f = new Frame(c.x(), c.z(), c.doorX(), c.doorZ());
        int d = c.depth(), w = c.breadth(), wall = c.isYama() ? YAMA_WALL : HALL_WALL;
        int floor = G + 1, top = floor + wall; // floor: the hall's floor block; top: its ceiling
        Direction fwd = f.forward(), back = fwd.getOpposite();
        boolean alongX = fwd.getAxis() == Direction.Axis.Z; // side walls (parallel to the way in) run along the door's axis
        // the plinth, a step over the ground, and stairs up to the door
        for (int u = -d - 2; u <= d + 2; u++) for (int v = -w - 2; v <= w + 2; v++) {
            boolean in = Math.abs(u) <= d && Math.abs(v) <= w;
            f.set(s, u, G, v, BLACKSTONE);
            f.set(s, u, floor, v, in ? (Math.abs(v) <= 1 ? POLISHED : (u + v) % 4 == 0 ? CHISELED : BRICK)
                    : Math.abs(u) == d + 2 || Math.abs(v) == w + 2 ? slab(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, SlabType.BOTTOM) : BRICK);
            for (int y = floor + 1; y <= top + 9; y++) f.set(s, u, y, v, AIR);
        }
        for (int v = -2; v <= 2; v++) {
            f.set(s, d + 2, floor, v, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, back, false));
            f.set(s, d + 3, G, v, POLISHED);
        }
        // the walls: dark brick between red pillars, iron-barred windows down the sides
        for (int u = -d; u <= d; u++) for (int v = -w; v <= w; v++) {
            boolean edgeU = Math.abs(u) == d, edgeV = Math.abs(v) == w;
            if (!edgeU && !edgeV) continue;
            boolean pillar = (edgeU && edgeV) || (edgeV && Math.floorMod(u + d, 4) == 0) || (edgeU && Math.floorMod(v + w, 4) == 0);
            for (int y = floor + 1; y <= top; y++) {
                boolean door = u == d && Math.abs(v) <= (c.isYama() ? 2 : 1) && y <= floor + (c.isYama() ? 6 : 4);
                if (door) {
                    f.set(s, u, y, v, AIR);
                    continue;
                }
                boolean window = !pillar && edgeV && !edgeU && (y == floor + 3 || y == floor + 4);
                BlockState st = pillar ? RED : y == floor + 1 || y == top ? BRICK : window ? bars(!alongX) : DEEP;
                f.set(s, u, y, v, st);
            }
        }
        // the ceiling, dark wood under the roof; the roof
        for (int u = -d; u <= d; u++) for (int v = -w; v <= w; v++) f.set(s, u, top, v, Math.abs(u) == d || Math.abs(v) == w ? BRICK : PLANKS);
        roof(s, c.x(), c.z(), c.halfX() + 2, c.halfZ() + 2, top + 1, true);
        // red columns down the hall, either side of the aisle
        int colV = w - 3;
        for (int u = -d + 4; u <= d - 2; u += 4) for (int side : new int[]{-1, 1}) {
            for (int y = floor + 1; y < top; y++) f.set(s, u, y, side * colV, y == floor + 1 ? GILDED : RED);
        }
        // the aisle: a red carpet from the door to the bench
        int bench = -d + 6;
        for (int u = bench + 1; u <= d; u++) for (int v = -1; v <= 1; v++) f.set(s, u, floor + 1, v, CARPET);
        // the dais, two steps, and the throne on it, a screen and black banners behind
        for (int u = -d + 1; u <= -d + 4; u++) for (int v = -4; v <= 4; v++) f.set(s, u, floor + 1, v, Math.abs(v) == 4 || u == -d + 4 ? GILDED : POLISHED);
        for (int u = -d + 1; u <= -d + 3; u++) for (int v = -3; v <= 3; v++) f.set(s, u, floor + 2, v, u == -d + 3 ? BRICK : POLISHED);
        f.set(s, -d + 2, floor + 3, 0, stair(Blocks.BLACKSTONE_STAIRS, fwd, false));
        for (int v : new int[]{-1, 1}) f.set(s, -d + 2, floor + 3, v, GILDED);
        f.set(s, -d + 1, floor + 3, 0, GILDED);
        f.set(s, -d + 1, floor + 4, 0, GOLD);
        for (int v = -4; v <= 4; v++) for (int y = floor + 3; y <= top - 1; y++) {
            if (Math.abs(v) <= 1 && y <= floor + 4) continue;
            f.set(s, -d + 1, y, v, Math.abs(v) == 4 ? RED : y == top - 1 ? GILDED : RED);
        }
        for (int v : new int[]{-3, -2, 2, 3}) {
            f.set(s, -d + 2, floor + 6, v, Blocks.BLACK_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, fwd));
        }
        // the judge's bench before the dais: dark wood, ledgers open on lecterns, candles
        for (int v = -5; v <= 5; v++) {
            f.set(s, bench, floor + 1, v, Math.abs(v) <= 1 ? AIR : PLANKS);
            if (Math.abs(v) >= 2) f.set(s, bench, floor + 2, v, Math.abs(v) == 3 ? lectern(fwd) : slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        }
        for (int v : new int[]{-3, 3}) book(s, f.x(bench, v), floor + 2, f.z(bench, v));
        for (int v : new int[]{-5, 5, -2, 2}) {
            f.set(s, bench, floor + 2, v, Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        }
        // scrolls: shelves along the back of the hall's sides
        for (int u = -d + 1; u <= -d + 6; u++) for (int side : new int[]{-1, 1}) {
            for (int y = floor + 1; y <= floor + 4; y++) f.set(s, u, y, side * (w - 1), (u + y) % 3 == 0 ? b(Blocks.CHISELED_BOOKSHELF) : SHELF);
        }
        // braziers within, and at the door
        for (int u : new int[]{bench + 3, d - 3}) for (int side : new int[]{-1, 1}) brazier(s, f.x(u, side * (w - 2)), floor + 1, f.z(u, side * (w - 2)), true);
        for (int side : new int[]{-1, 1}) brazier(s, f.x(d + 3, side * 4), G + 1, f.z(d + 3, side * 4), false);
        // soul lanterns hung down the aisle
        for (int u = bench + 2; u <= d - 1; u += 4) for (int side : new int[]{-1, 1}) {
            f.set(s, u, top - 1, side * 2, b(Blocks.CHAIN));
            f.set(s, u, top - 2, side * 2, hanging(Blocks.SOUL_LANTERN));
        }
        // its name over the door
        String[] name = plaque(c.number());
        s.sign(f.x(d + 1, 0), floor + (c.isYama() ? 7 : 5), f.z(d + 1, 0),
                Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, fwd), name);
        // the instruments of its court
        instruments(s, f, c, floor, bench, w, top);
        // a path from the avenue to its door
        if (!c.isYama()) {
            int from = d + 3, to = Math.abs(c.x()) - TenCourts.AVENUE_HALF - 1; // u at the avenue's edge
            for (int u = from; u <= to; u++) for (int v = -1; v <= 1; v++) f.set(s, u, G, v, v == 0 ? POLISHED : BRICK);
        }
        // its guards, its king
        people(s, f, c, floor);
    }

    private static BlockState lectern(Direction facing) {
        return Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, facing.getOpposite()).setValue(LecternBlock.HAS_BOOK, true);
    }

    private static void book(Site s, int x, int y, int z) {
        if (s.has(x, z) && s.level.getBlockEntity(s.pos(x, y, z)) instanceof LecternBlockEntity l) l.setBook(new ItemStack(Items.WRITABLE_BOOK));
    }

    /** The plaque over a hall's door: its court, its king, what it judges. */
    public static String[] plaque(int court) {
        return switch (court) {
            case 1 -> new String[]{"第一殿 秦广王", "First Court", "King Qinguang", "the Karma Mirror"};
            case 2 -> new String[]{"第二殿 楚江王", "Second Court", "King Chujiang", "thieves, the ice"};
            case 3 -> new String[]{"第三殿 宋帝王", "Third Court", "King Songdi", "the faithless"};
            case 4 -> new String[]{"第四殿 五官王", "Fourth Court", "King Wuguan", "cheats, the scales"};
            case 5 -> new String[]{"第五殿 阎罗王", "Fifth Court", "King Yama", "Lord of Death"};
            case 6 -> new String[]{"第六殿 卞城王", "Sixth Court", "King Biancheng", "the impious"};
            case 7 -> new String[]{"第七殿 泰山王", "Seventh Court", "King Taishan", "the oil cauldron"};
            case 8 -> new String[]{"第八殿 都市王", "Eighth Court", "King Dushi", "the unfilial"};
            case 9 -> new String[]{"第九殿 平等王", "Ninth Court", "King Pingdeng", "murderers"};
            case 10 -> new String[]{"第十殿 转轮王", "Tenth Court", "King Zhuanlun", "rebirth"};
            default -> new String[]{"录事司", "The Office", "of Records", ""};
        };
    }

    /** What stands in a court for its judgements, beside the bench (and, in the Office of Records, its shelves). */
    private static void instruments(Site s, Frame f, TenCourts.Court c, int floor, int bench, int w, int top) {
        int u = bench + 3, v = -(w - 4); // the left of the hall, before the bench
        switch (c.number()) {
            case 0 -> { // rows of shelves and writing desks for the clerks
                for (int uu = bench + 2; uu <= c.depth() - 4; uu += 3) for (int side : new int[]{-1, 1}) {
                    for (int vv = 3; vv <= w - 3; vv++) {
                        f.set(s, uu, floor + 1, side * vv, SHELF);
                        f.set(s, uu, floor + 2, side * vv, vv % 3 == 0 ? lectern(f.forward()) : SHELF);
                        if (vv % 3 == 0) book(s, f.x(uu, side * vv), floor + 2, f.z(uu, side * vv));
                    }
                }
            }
            case 1 -> { // the Karma Mirror (孽镜台): a tall glass in a gold frame, on a stand
                for (int dv = -1; dv <= 1; dv++) for (int y = floor + 1; y <= floor + 5; y++) {
                    boolean frame = Math.abs(dv) == 1 || y == floor + 1 || y == floor + 5;
                    f.set(s, u, y, v + dv, frame ? GOLD : Blocks.GLASS.defaultBlockState());
                }
                f.set(s, u + 1, floor + 1, v, stair(Blocks.POLISHED_BLACKSTONE_STAIRS, f.forward().getOpposite(), false));
            }
            case 2 -> { // the cold hell: ice, and blue ice pillars
                for (int du = -1; du <= 1; du++) for (int dv = -1; dv <= 1; dv++) {
                    f.set(s, u + du, floor, v + dv, b(Blocks.PACKED_ICE));
                    if (du != 0 && dv != 0) for (int y = floor + 1; y <= floor + 3; y++) f.set(s, u + du, y, v + dv, b(Blocks.BLUE_ICE));
                }
                f.set(s, u, floor + 1, v, b(Blocks.POWDER_SNOW));
            }
            case 3 -> { // iron hooks on chains
                for (int dv = -1; dv <= 1; dv += 2) {
                    for (int y = floor + 2; y < top; y++) f.set(s, u, y, v + dv, b(Blocks.CHAIN));
                    f.set(s, u, floor + 1, v + dv, b(Blocks.LIGHTNING_ROD));
                }
            }
            case 4 -> { // the scales: a beam on a post, a pan hung at each end
                for (int y = floor + 1; y <= floor + 4; y++) f.set(s, u, y, v, BRICK_WALL);
                for (int dv = -2; dv <= 2; dv++) f.set(s, u, floor + 5, v + dv, dv == 0 ? GILDED : POLISHED);
                for (int dv : new int[]{-2, 2}) {
                    f.set(s, u, floor + 4, v + dv, b(Blocks.CHAIN));
                    f.set(s, u, floor + 3, v + dv, b(Blocks.CHAIN));
                    f.set(s, u, floor + 2, v + dv, b(Blocks.CAULDRON));
                }
            }
            case 5 -> { // King Yama's: the great register, gold seals, his guards' racks of halberds
                for (int side : new int[]{-1, 1}) {
                    f.set(s, u, floor + 1, side * (w - 5), POLISHED);
                    f.set(s, u, floor + 2, side * (w - 5), lectern(f.forward()));
                    book(s, f.x(u, side * (w - 5)), floor + 2, f.z(u, side * (w - 5)));
                    f.set(s, u + 3, floor + 1, side * (w - 5), GOLD);
                    for (int du = 6; du <= 14; du += 4) {
                        f.set(s, u + du, floor + 1, side * (w - 2), BRICK_WALL);
                        f.set(s, u + du, floor + 2, side * (w - 2), b(Blocks.LIGHTNING_ROD));
                    }
                }
                f.set(s, -c.depth() + 2, floor + 6, 0, GOLD);
            }
            case 6 -> { // the bell of the Wailing City
                for (int y = floor + 1; y <= floor + 4; y++) {
                    f.set(s, u, y, v - 1, RED);
                    f.set(s, u, y, v + 1, RED);
                }
                f.set(s, u, floor + 5, v - 1, POLISHED);
                f.set(s, u, floor + 5, v, POLISHED);
                f.set(s, u, floor + 5, v + 1, POLISHED);
                f.set(s, u, floor + 4, v, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING)
                        .setValue(BellBlock.FACING, f.right()));
            }
            case 7 -> { // cauldrons of boiling oil over fires
                for (int dv = -1; dv <= 1; dv += 2) {
                    f.set(s, u, floor, v + dv, b(Blocks.MAGMA_BLOCK));
                    f.set(s, u, floor + 1, v + dv, b(Blocks.LAVA_CAULDRON));
                }
            }
            case 8 -> { // the crushing carts: their wheels, on rails
                for (int du = -2; du <= 2; du++) f.set(s, u + du, floor + 1, v, b(Blocks.RAIL));
                f.set(s, u, floor + 2, v - 1, b(Blocks.GRINDSTONE));
                f.set(s, u, floor + 2, v + 1, b(Blocks.GRINDSTONE));
                f.set(s, u, floor + 1, v - 1, POLISHED);
                f.set(s, u, floor + 1, v + 1, POLISHED);
            }
            case 9 -> { // a cage of iron, soul fire within
                for (int du = -1; du <= 1; du++) for (int dv = -1; dv <= 1; dv++) {
                    if (du == 0 && dv == 0) {
                        f.set(s, u, floor, v, SOUL_SOIL);
                        f.set(s, u, floor + 1, v, b(Blocks.SOUL_FIRE));
                        continue;
                    }
                    for (int y = floor + 1; y <= floor + 3; y++) f.set(s, u + du, y, v + dv, Blocks.IRON_BARS.defaultBlockState());
                    f.set(s, u + du, floor + 4, v + dv, POLISHED);
                }
            }
            case 10 -> { // Meng Po's soup, ladled for those going back
                f.set(s, u, floor + 1, v, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
                f.set(s, u + 1, floor + 1, v, b(Blocks.BREWING_STAND));
                f.set(s, u - 1, floor + 1, v, b(Blocks.BARREL));
            }
            default -> {}
        }
    }

    /** Its guards at the door (Ox-Head and Horse-Face), clerks in the Office of Records, and its king on the dais. */
    private static void people(Site s, Frame f, TenCourts.Court c, int floor) {
        int d = c.depth();
        float out = (float) Math.toDegrees(Math.atan2(-f.fx(), f.fz()));
        if (c.number() == 0) {
            for (int k = 0; k < 3; k++) {
                int u = -d + 8 + k * 4, v = k % 2 == 0 ? 2 : -2;
                s.mob(ModEntities.JUDGE_CLERK.get(), f.x(u, v) + 0.5, floor + 1, f.z(u, v) + 0.5, out, null);
            }
            return;
        }
        int gv = c.isYama() ? 4 : 3;
        s.mob(ModEntities.OX_HEAD.get(), f.x(d + 4, -gv) + 0.5, G + 1, f.z(d + 4, -gv) + 0.5, out,
                m -> m.become(m.regularLevel(), c.isYama()));
        s.mob(ModEntities.HORSE_FACE.get(), f.x(d + 4, gv) + 0.5, G + 1, f.z(d + 4, gv) + 0.5, out,
                m -> m.become(m.regularLevel(), c.isYama()));
        if (c.isYama()) {
            // the soul-catchers wait on him, either side of the bench
            s.mob(ModEntities.BLACK_IMPERMANENCE.get(), f.x(-d + 8, -5) + 0.5, floor + 1, f.z(-d + 8, -5) + 0.5, out, null);
            s.mob(ModEntities.WHITE_IMPERMANENCE.get(), f.x(-d + 8, 5) + 0.5, floor + 1, f.z(-d + 8, 5) + 0.5, out, null);
            s.mob(ModEntities.JUDGE_CLERK.get(), f.x(-d + 7, -2) + 0.5, floor + 1, f.z(-d + 7, -2) + 0.5, out, null);
        }
        int[] t = c.throne();
        s.mob(HellKings.type(c.number()), t[0] + 0.5, floor + 3, t[1] + 0.5, c.throneYaw(), null);
    }

    /** Where a court's king is put: on the dais before his throne (the block his feet are in). */
    public static net.minecraft.core.BlockPos kingSpot(TenCourts.Court c) {
        int[] t = c.throne();
        return new net.minecraft.core.BlockPos(t[0], G + 4, t[1]);
    }

    // ── The Wheel of Reincarnation ───────────────────────────────────────

    /** The Wheel, upright, facing along z, its middle at (x, z): a gold rim, six spokes for the Six Paths of rebirth, a hub of gold. */
    private static void wheel(Site s, int x, int z) {
        int r = TenCourts.WHEEL_RADIUS, cy = G + 1 + r;
        // the rim: a ring of gold, gilded here and there
        for (int dx = -r - 1; dx <= r + 1; dx++) for (int dy = -r - 1; dy <= r + 1; dy++) {
            double d = Math.sqrt(dx * dx + dy * dy);
            if (cy + dy > G && Math.abs(d - r) < 0.6) s.set(x + dx, cy + dy, z, HellTerrain.grain(dx, dy, 0) < 0.7 ? GOLD : GILDED);
        }
        // six spokes, the Six Paths of rebirth, red lacquer from the hub to the rim
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3 + Math.PI / 6;
            for (double t = 1.5; t < r - 0.4; t += 0.35) {
                s.set(x + (int) Math.round(Math.cos(a) * t), cy + (int) Math.round(Math.sin(a) * t), z, RED);
            }
        }
        // the hub
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) s.set(x + dx, cy + dy, z, dx == 0 && dy == 0 ? GILDED : GOLD);
        // its stand: blackstone feet either side; braziers before it
        for (int side : new int[]{-1, 1}) {
            for (int dz = -1; dz <= 1; dz++) s.set(x + side * 2, G + 1, z + dz, POLISHED);
            brazier(s, x + side * (r + 2), G + 1, z - 2, true);
        }
        s.sign(x, G + 1, z - 3, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8),
                "六道轮回", "The Wheel of", "Reincarnation", "");
    }

    /** Whether something at (x, z) belongs to the works (so the plains' decorations keep off it). */
    public static boolean occupied(int x, int z) {
        return HellTerrain.levelled(x, z) > 0.05 || Math.abs(x - (-9)) <= 6 && Math.abs(z - (HellTerrain.bridgeEnds()[0] - 6)) <= 6;
    }

    /** For the Office of Records' clerks and a court's guards (tests): what a hall's guard is. */
    public static boolean isGuard(net.minecraft.world.entity.Entity e) {
        return e instanceof HellMonster;
    }
}
