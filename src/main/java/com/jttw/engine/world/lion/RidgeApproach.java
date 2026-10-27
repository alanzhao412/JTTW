package com.jttw.engine.world.lion;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The ways up to Lion-Camel City through the ridge's hordes (pure: the same from every chunk):
 * <ul>
 *   <li><b>three roads</b>, south, east and west, from the edge of the region to the city's gates,
 *       winding; each is cut down through the crags and built up over the hollows, level across,
 *       {@code 2 *} {@value #ROAD_HALF}{@code + 1} blocks wide, skulls on stakes along it;</li>
 *   <li><b>the outposts</b> beside them, {@value #OUTPOSTS} to a road: a watch tower of dark brick
 *       with a beacon of soul fire on its platform, in a ring of stakes, a cooking fire, and the
 *       demons who keep it, a captain among them;</li>
 *   <li><b>the patrols</b> out on the roads, three demons at each of {@link #PATROLS}.</li>
 * </ul>
 * Places are local to the region's centre (the city's), in blocks; a road's {@code along} is how
 * far out from the centre, its {@code across} how far to its side.
 */
public final class RidgeApproach {

    private RidgeApproach() {}

    /** The roads' ways out from the centre: south (the main gate's), east, west. */
    private static final int[][] DIRS = {{0, 1}, {1, 0}, {-1, 0}};
    public static final int ROADS = DIRS.length;
    /** Where the roads start (just out from the city's walls) and end (past the region's edge); half their width. */
    public static final int ROAD_FROM = LionCamelCity.OUTER + 2, ROAD_TO = 340, ROAD_HALF = 2;
    /** The outposts along each road: how far out, how far to its side, how far their ring reaches. */
    public static final int OUTPOSTS = 3, OUTPOST_ACROSS = 15, OUTPOST_R = 10, TOWER = 3, TOWER_H = 14;
    private static final int[] OUTPOST_ALONG = {120, 200, 280};
    /** How far out the patrols stand. */
    public static final int[] PATROLS = {95, 165, 240, 310};

    /** A place on or by a road: which, how far out, how far to its side. */
    public record Spot(int road, int along, int across) {}

    /** How far the road's middle strays to its side at {@code along}: none at the gate, winding further out. */
    public static int bend(int road, int along) {
        double ramp = Mth.clamp((along - ROAD_FROM) / 40.0, 0, 1);
        return (int) Math.round(ramp * (8 * Math.sin(along / 31.0 + road * 1.7) + 4 * Math.sin(along / 57.0 + road * 0.9)));
    }

    /** The local (dx, dz) of a road's middle at {@code along} (shifted {@code across} to its side). */
    public static int[] point(int road, int along, int across) {
        int[] d = DIRS[road];
        int side = bend(road, along) + across;
        return new int[]{d[0] * along - d[1] * side, d[1] * along + d[0] * side};
    }

    /** The road at local (dx, dz), and where on it, or null if none is there (its shoulders count). */
    @Nullable
    public static Spot road(int dx, int dz) {
        for (int r = 0; r < ROADS; r++) {
            int[] d = DIRS[r];
            int along = d[0] * dx + d[1] * dz;
            if (along < ROAD_FROM || along > ROAD_TO) continue;
            int side = -d[1] * dx + d[0] * dz;
            int across = side - bend(r, along);
            if (Math.abs(across) <= ROAD_HALF + 1) return new Spot(r, along, across);
        }
        return null;
    }

    /** An outpost: its road, how far out, which side; its centre (local dx, dz). */
    public record Outpost(int road, int along, int side, int dx, int dz) {}

    public static final List<Outpost> OUTPOST_LIST = outposts();

    private static List<Outpost> outposts() {
        List<Outpost> out = new ArrayList<>();
        for (int r = 0; r < ROADS; r++) for (int i = 0; i < OUTPOSTS; i++) {
            int along = OUTPOST_ALONG[i], side = (r + i) % 2 == 0 ? 1 : -1;
            int[] p = point(r, along, side * OUTPOST_ACROSS);
            out.add(new Outpost(r, along, side, p[0], p[1]));
        }
        return List.copyOf(out);
    }

    /** The outpost whose ring local (dx, dz) is in, or null. */
    @Nullable
    public static Outpost outpostAt(int dx, int dz) {
        for (Outpost o : OUTPOST_LIST) {
            int ox = dx - o.dx(), oz = dz - o.dz();
            if (ox * ox + oz * oz <= OUTPOST_R * OUTPOST_R) return o;
        }
        return null;
    }

    /** Whether local (dx, dz) is near enough a road or an outpost that the ridge shapes its ground for them. */
    public static boolean shaped(int dx, int dz) {
        return road(dx, dz) != null || outpostAt(dx, dz) != null;
    }

    private static int hash(int x, int y, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x * 31 + y, salt, z), 100L);
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    /** The road's surface at a spot: stone paving down its middle, gravel and trodden earth at its sides. */
    public static BlockState roadTop(Spot sp, int x, int z) {
        int r = hash(x, 0, z, 601), a = Math.abs(sp.across());
        if (a <= 1) return r < 40 ? s(Blocks.STONE_BRICKS) : r < 60 ? s(Blocks.CRACKED_STONE_BRICKS) : r < 80 ? s(Blocks.COBBLESTONE) : s(Blocks.GRAVEL);
        if (a == ROAD_HALF) return r < 45 ? s(Blocks.GRAVEL) : r < 75 ? s(Blocks.COARSE_DIRT) : s(Blocks.COBBLESTONE);
        return r < 50 ? s(Blocks.COARSE_DIRT) : r < 80 ? s(Blocks.PACKED_MUD) : s(Blocks.GRAVEL);
    }

    /** What stands on the road's shoulder, {@code h} blocks over its surface: now and then a skull on a stake. */
    @Nullable
    public static BlockState roadside(Spot sp, int h, int x, int z) {
        if (Math.abs(sp.across()) != ROAD_HALF + 1 || Math.floorMod(sp.along() + (sp.across() > 0 ? 4 : 0), 9) != 0) return null;
        if (h == 1 || h == 2) return s(Blocks.DARK_OAK_FENCE);
        if (h == 3) return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, hash(x, 3, z, 602) % 16);
        return null;
    }

    /**
     * What stands at an outpost, {@code ox}, {@code oz} from its centre and {@code h} over its
     * ground (0 its ground): its ground of trodden earth, its tower, its ring of stakes (open toward
     * the road), its fire. Null where nothing of it is.
     */
    @Nullable
    public static BlockState outpost(Outpost o, int ox, int h, int oz) {
        int d2 = ox * ox + oz * oz;
        if (d2 > OUTPOST_R * OUTPOST_R) return null;
        int r = hash(ox + o.dx(), h, oz + o.dz(), 603);
        int ax = Math.abs(ox), az = Math.abs(oz);
        // which way the road lies from it, to leave the ring open and the tower's door that way
        int[] toward = toRoad(o);
        boolean tower = ax <= TOWER && az <= TOWER;
        if (h == 0) return tower ? s(Blocks.DEEPSLATE_TILES) : r < 40 ? s(Blocks.COARSE_DIRT) : r < 70 ? s(Blocks.PACKED_MUD) : s(Blocks.GRAVEL);
        if (h < 0) return null;
        if (tower) {
            boolean corner = ax == TOWER && az == TOWER, rim = ax == TOWER || az == TOWER;
            int along = toward[0] * ox + toward[1] * oz, side = Math.abs(-toward[1] * ox + toward[0] * oz);
            if (h <= TOWER_H - 1) {
                if (corner) return Blocks.DARK_OAK_LOG.defaultBlockState();
                if (rim) {
                    if (along == TOWER && side == 0 && h <= 2) return s(Blocks.AIR); // its door, toward the road
                    if (side == 0 && h % 4 == 3) return s(Blocks.AIR);               // arrow slits
                    return r < 20 ? s(Blocks.CRACKED_DEEPSLATE_BRICKS) : s(Blocks.DEEPSLATE_BRICKS);
                }
                // within: a ladder up the far wall
                if (along == -(TOWER - 1) && side == 0) return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing(toward));
                return s(Blocks.AIR);
            }
            if (h == TOWER_H) {
                if (along == -(TOWER - 1) && side == 0) return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing(toward));
                return s(Blocks.DARK_OAK_PLANKS);
            }
        }
        // the platform over the tower, wider than it, railed, its beacon of soul fire
        if (ax <= TOWER + 1 && az <= TOWER + 1) {
            boolean edge = ax == TOWER + 1 || az == TOWER + 1;
            if (h == TOWER_H) return edge ? s(Blocks.DARK_OAK_SLAB) : s(Blocks.DARK_OAK_PLANKS);
            if (h == TOWER_H + 1) {
                if (ox == 0 && oz == 0) return Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true);
                if (edge) return s(Blocks.DARK_OAK_FENCE);
                return null;
            }
            if (h == TOWER_H + 2 && ax == TOWER + 1 && az == TOWER + 1) return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r % 16);
            if (h > TOWER_H) return null;
            return null;
        }
        // the ring of stakes, open toward the road
        double d = Math.sqrt(d2);
        if (d >= OUTPOST_R - 1.0) {
            int along = toward[0] * ox + toward[1] * oz;
            if (along > OUTPOST_R / 2 && Math.abs(-toward[1] * ox + toward[0] * oz) <= 3) return null;
            if (h <= 2) return s(Blocks.DARK_OAK_FENCE);
            if (h == 3 && Math.floorMod(ox * 3 + oz * 5, 4) == 0) return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r % 16);
            return null;
        }
        // its cooking fire and pot, bones by it
        if (ox == 6 && oz == 0 || ox == -6 && oz == 0) {
            if (h == 1) return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false);
            if (h == 2 && ox > 0) return s(Blocks.CAULDRON);
            return null;
        }
        if (h == 1 && r < 4) return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        if (h == 1 && r < 7) return Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r % 16);
        return null;
    }

    /** The way (one of the four, as {dx, dz}) from an outpost toward its road. */
    public static int[] toRoad(Outpost o) {
        int[] d = DIRS[o.road()];
        // the road lies to the side opposite the one the outpost stands on
        return new int[]{d[1] * o.side(), -d[0] * o.side()};
    }

    private static Direction facing(int[] toward) {
        // a ladder on the far wall faces toward the road
        if (toward[0] > 0) return Direction.EAST;
        if (toward[0] < 0) return Direction.WEST;
        return toward[1] > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    // ── Who keeps the ways ──────────────────────────────────────────────

    /**
     * A post out on the ridge: who, a captain or not, at which road's height (its road and how far
     * out, whose middle's height the ground there was brought to), where (local dx, dz), how high over
     * that ground.
     */
    public record Post(LionFortress.Who who, boolean captain, int road, int along, int dx, int dz, int up) {}

    public static final List<Post> POSTS = posts();

    private static List<Post> posts() {
        List<Post> out = new ArrayList<>();
        for (Outpost o : OUTPOST_LIST) {
            int[] t = toRoad(o);
            // round the fire, at the gap in the ring, and up on the tower
            int[][] at = {{-5, 3}, {4, -4}, {-3, -6}, {t[0] * 7 + 1, t[1] * 7 + 1}};
            for (int i = 0; i < at.length; i++) {
                var who = i == 0 ? LionFortress.Who.LION_DEMON : i == 1 ? LionFortress.Who.LION_DEMON : i == 2 ? LionFortress.Who.ELEPHANT_DEMON : LionFortress.Who.WIND_DRILLER;
                out.add(new Post(who, i == 0, o.road(), o.along(), o.dx() + at[i][0], o.dz() + at[i][1], 1));
            }
            out.add(new Post(LionFortress.Who.PENG_DEMON, false, o.road(), o.along(), o.dx() + 2, o.dz() + 2, TOWER_H + 1));
        }
        for (int r = 0; r < ROADS; r++) for (int i = 0; i < PATROLS.length; i++) {
            int along = PATROLS[i];
            for (int a = -1; a <= 1; a++) {
                int[] p = point(r, along + a * 2, a);
                var who = a == 0 && i % 2 == 1 ? LionFortress.Who.ELEPHANT_DEMON : a == 1 && i % 2 == 0 ? LionFortress.Who.PENG_DEMON : LionFortress.Who.LION_DEMON;
                out.add(new Post(who, a == 0 && i == PATROLS.length - 1, r, along, p[0], p[1], 1));
            }
        }
        return List.copyOf(out);
    }
}
