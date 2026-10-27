package com.jttw.engine.world.kunlun;

import com.jttw.engine.world.Zones;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

/**
 * The shape of Mount Kunlun, worked out from the seed and the region alone, so every chunk agrees on
 * it whichever is made first (pure: unit-tested). Not one peak but a massif, the axis of the world:
 * <ul>
 *   <li><b>its summit</b> ({@link #summit}): a level top {@value #TOP} blocks out from the region's
 *       centre, room for the Queen Mother's palace and its courts, at y {@value #SUMMIT_Y} in a world
 *       of the taller sky; in an older, lower world as high as its sky allows ({@value #UNDER_SKY}
 *       under it), the whole massif pressed down to fit;</li>
 *   <li><b>the summit pyramid</b>: four-faced, its arêtes running out from the top, falling in cliffs
 *       and bands {@value #PYRAMID} blocks out;</li>
 *   <li><b>the massif</b> ({@link #field}, {@link #height}): its flanks, falling from the pyramid
 *       steeply and then ever more gently to the land's own {@value #REACH} blocks out, meeting it
 *       without a crease, ribbed by {@value #SPURS} spurs whose crests stand over the flanks round
 *       them; on all but one a lesser peak, a sharp pyramid of its own rising from it, a saddle
 *       between it and the summit; between
 *       the spurs, valleys, and deep in each a gorge ({@link #gorge}) from about {@value #GORGE_FROM}
 *       blocks out to the foot; the whole broken by crags and stepped into ledges and cliffs;</li>
 *   <li><b>the pilgrim's way</b> ({@link #path}): a stone stair that winds up round the massif at an
 *       even climb (never more than {@value #MAX_CLIMB} a block, so it is walked in half steps), from
 *       its foot to the summit's south edge, a gate at its foot and pavilions and shrines along it
 *       ({@link Path#sites});</li>
 *   <li><b>the snow</b> ({@link #snowLine}) over its upper half, and <b>the clouds</b> ({@link #cloud})
 *       in belts at {@value #BELTS} heights round it, broken sheets the massif stands up through,
 *       the summit over the highest.</li>
 * </ul>
 * Local coordinates are block offsets from the region's centre (the stair arrives from +z, south).
 */
public final class KunlunShape {

    private KunlunShape() {}

    public static final Zones.Zone ZONE = Zones.KUNLUN;

    // ── Its plan ────────────────────────────────────────────────────────

    /** The summit's level top: a circle, from the centre. */
    public static final int TOP = 48;
    /** The farthest anything of the massif reaches from the centre: past it, the land's own. */
    public static final int REACH = 425;
    /** The massif's profile: the higher, the steeper under the summit and the gentler toward the foot. */
    public static final double POWER = 1.65;
    /** How far out from the top the summit pyramid keeps its four faces (its arêtes running out along two crossed axes), fading to round below. */
    public static final int PYRAMID = 190;
    /** The sheer cliff round the summit's level top, the palace's ground standing over it (a share of the rise). */
    public static final double CROWN = 0.05;
    /** The spurs that run down from the summit, and the lesser peaks on all but one of them. */
    public static final int SPURS = 6, PEAKS = SPURS - 1;
    /** How steeply a spur's sides fall from its crest, in blocks a block (at the height the shape is drawn for, {@link #RISE}). */
    public static final double SPUR_SIDE = 2.0;
    /** The height the shape is drawn for, the summit over its foot: its slopes are given at it. */
    public static final double RISE = 480;
    /** The gorges between the spurs: how deep at most, from how far out. */
    public static final int GORGE_DEPTH = 46, GORGE_FROM = 165;
    /** Crags (their height), the finer broken ground, and the ledges' rise (the flanks stepped into cliffs and bands this high). */
    public static final double CRAG = 13, ROUGH = 3.5, LEDGE = 8, LEDGES = 0.5;

    // ── Its height ──────────────────────────────────────────────────────

    /** The summit's ground in a world of the taller sky, and how far under the sky it always is (room for the palace's roofs). */
    public static final int SUMMIT_Y = 596, UNDER_SKY = 44;
    /** Never under this y (unless the sky is lower), and at least so far over its foot and the highest land near. */
    public static final int MIN_SUMMIT = 248, MIN_RISE = 150, HEADROOM = 40, LOOK = REACH + 40;

    // ── The pilgrim's way ───────────────────────────────────────────────

    /** The stair: its half-width (the way), and how far out its edge reaches (a rail on the drop side). */
    public static final double PATH_HALF = 1.6, SHOULDER = 2.8;
    /** Past its edge, the flank is cut back from the stair in a bank this wide (gently on the drop side, steeply on the uphill), so it is never a trench. */
    public static final double BANK = 10;
    /** How steeply the bank rises from the stair's edge, on the drop side and on the uphill. */
    public static final double BANK_OUTER = 0.8, BANK_INNER = 1.5;
    /** How steeply it climbs, as laid out and at most, in blocks up a block along. */
    public static final double CLIMB = 0.25, MAX_CLIMB = 0.33;
    /** Over a drop deeper than this under its way (and over every gorge) it crosses on a bridge of arches; over less, it is built out on stone. */
    public static final int BRIDGE = 14;
    /** A bridge's arches: a pier every so many blocks along. */
    public static final int SPAN = 11;
    /** Where the stair crosses a gorge, the gorge is cut down this far under its way, so the stream runs under the bridge. */
    public static final int CULVERT = 5;
    /** A rest pavilion every so far along the stair, a shrine halfway between. */
    public static final int PAVILION_EVERY = 210;

    // ── Snow, ice and cloud ─────────────────────────────────────────────

    /** The snow over this share of the way from the foot to the summit (wandering a little); never under this y. */
    public static final double SNOW_SHARE = 0.5;
    public static final int MIN_SNOW = 160;
    /** Pines no higher than this under the snow. */
    public static final int TREE_LINE = 70;
    /**
     * The cloud belts: their middles, as shares of the way from the foot to the summit; how far down
     * the flank under them each spreads (in blocks of drop: the lower, the wider, broken sheets far
     * out over the shoulders; the highest a collar close round the pyramid); how thick their puffs grow at most.
     */
    public static final double[] BELT_AT = {0.36, 0.6, 0.8};
    public static final int[] BELT_DROP = {110, 100, 70};
    public static final int BELTS = 3, CLOUD_PUFF = 6;

    // ── Noise and the plan, from the seed ───────────────────────────────

    private static long noiseSeed = Long.MIN_VALUE;
    private static SimplexNoise noise;

    private static synchronized SimplexNoise noise(long seed) {
        if (noise == null || seed != noiseSeed) {
            noise = new SimplexNoise(RandomSource.create(seed ^ 0x6B0A1F5EL));
            noiseSeed = seed;
        }
        return noise;
    }

    static double smooth(double from, double to, double v) {
        double s = Mth.clamp((v - from) / (to - from), 0, 1);
        return s * s * (3 - 2 * s);
    }

    /** A lesser peak: where it stands (local), how high (a share of the summit's rise), how far its foot spreads, the turn of its faces, the spur it stands on. */
    public record Peak(double x, double z, double height, double base, double turn, int spur) {}

    /**
     * The massif's plan: the summit pyramid's turn, the spurs' bearings (radians, from east by south),
     * how far each runs and how high its crest stands over the flanks; the lesser peaks; the gorges'
     * bearings and depths (each between a spur and the next).
     */
    public record Plan(double pyramidTurn, double[] spurAt, double[] spurLength, double[] spurAmp, Peak[] peaks,
                       double[] gorgeAt, double[] gorgeAmp) {}

    private static long planSeed = Long.MIN_VALUE;
    private static Plan planAt;

    /** The massif's plan, from the seed. */
    public static synchronized Plan plan(long seed) {
        if (planAt == null || seed != planSeed) {
            RandomSource r = RandomSource.create(seed ^ 0x5B0A2EL);
            double base = r.nextDouble() * Math.PI * 2;
            double[] at = new double[SPURS], length = new double[SPURS], amp = new double[SPURS];
            for (int k = 0; k < SPURS; k++) {
                at[k] = base + k * Math.PI * 2 / SPURS + (r.nextDouble() - 0.5) * 0.45;
                length[k] = REACH * (0.84 + 0.13 * r.nextDouble());
                amp[k] = 1.07 + 0.08 * r.nextDouble();
            }
            int bare = r.nextInt(SPURS);
            List<Peak> peaks = new ArrayList<>();
            for (int k = 0; k < SPURS; k++) {
                if (k == bare) continue;
                double rho = 195 + 90 * r.nextDouble();
                double height = 0.5 + 0.22 * r.nextDouble();
                double spread = Math.min(80 + 30 * r.nextDouble(), REACH - 40 - rho);
                peaks.add(new Peak(Math.cos(at[k]) * rho, Math.sin(at[k]) * rho, height, spread, r.nextDouble() * Math.PI / 2, k));
            }
            double[] gorge = new double[SPURS], deep = new double[SPURS];
            for (int k = 0; k < SPURS; k++) {
                double a = at[k], b = at[(k + 1) % SPURS] + (k + 1 == SPURS ? Math.PI * 2 : 0);
                gorge[k] = (a + b) / 2 + (r.nextDouble() - 0.5) * 0.15;
                deep[k] = 0.7 + 0.3 * r.nextDouble();
            }
            planAt = new Plan(at[0], at, length, amp, peaks.toArray(new Peak[0]), gorge, deep);
            planSeed = seed;
        }
        return planAt;
    }

    // ── The massif ──────────────────────────────────────────────────────

    /** Whether local (dx, dz) is on the summit's level top. */
    public static boolean onTop(int dx, int dz) {
        return dx * dx + dz * dz <= TOP * TOP;
    }

    /** A faceted distance: the plain one, drawn out along two crossed axes turned by {@code turn} (so a cone on it has four faces and four arêtes). */
    private static double faceted(double dx, double dz, double turn) {
        double c = Math.cos(turn), s = Math.sin(turn);
        double u = dx * c + dz * s, v = -dx * s + dz * c;
        return 0.5 * Math.sqrt(dx * dx + dz * dz) + 0.5 * (Math.abs(u) + Math.abs(v)) * Mth.SQRT_OF_TWO / 2;
    }

    /**
     * The massif's own profile (a share of the summit's rise) at (dx, dz), its reach there {@code
     * reach}: falling from the summit's edge steeply and then ever more gently, to meet the land at
     * the foot without a crease; a sheer cliff right round the summit's level top, four-faced
     * under it (the summit pyramid), round below.
     */
    private static double profile(Plan p, double dx, double dz, double r, double reach) {
        double facets = smooth(TOP + 10, TOP + 60, r) * (1 - smooth(TOP + 60, TOP + PYRAMID, r));
        double n = Mth.lerp(facets, r, faceted(dx, dz, p.pyramidTurn()));
        double t = Mth.clamp((n - TOP) / (reach - TOP), 0, 1);
        return Math.pow(1 - t, POWER) * (1 - CROWN * smooth(TOP + 0.5, TOP + 4, r));
    }

    /** How near a spur's line the bearing {@code theta} is: 1 on it, falling to 0 a sixth of the way round. */
    private static double nearSpur(Plan p, double theta, int sharpness) {
        double best = 0;
        for (double a : p.spurAt()) best = Math.max(best, Math.pow(Math.max(0, Math.cos(theta - a)), sharpness));
        return best;
    }

    /**
     * How much of the summit's rise over the land the massif takes at local (dx, dz) (world x, z): 1
     * on the summit's top, falling to 0 at its foot; past {@value #REACH} blocks, 0 whatever. The
     * greatest of its own profile (lowered between the spurs into valleys), its spurs' crests (their
     * sides falling away from them) and its lesser peaks.
     */
    public static double field(long seed, int x, int z, int dx, int dz) {
        double r = Math.sqrt((double) dx * dx + (double) dz * dz);
        if (r <= TOP) return 1;
        if (r >= REACH) return 0;
        Plan p = plan(seed);
        SimplexNoise n = noise(seed);
        double theta = Math.atan2(dz, dx);
        double reach = REACH * (0.93 + 0.05 * nearSpur(p, theta, 10)) + 12 * n.getValue(x / 90.0 + 3.1, z / 90.0 - 7.7);
        // the valleys between the spurs: the flanks lower there
        double e = profile(p, dx, dz, r, reach) * (1 - 0.2 * (1 - nearSpur(p, theta, 4)) * smooth(TOP + 40, TOP + 150, r));
        // the spurs: ribs standing over the flanks from the pyramid to the foot, their sides falling steeply away
        for (int k = 0; k < SPURS; k++) {
            double c = Math.cos(theta - p.spurAt()[k]);
            if (c <= 0.5) continue;
            double s = r * c, side = r * Math.sqrt(Math.max(0, 1 - c * c));
            double length = p.spurLength()[k];
            if (s >= length) continue;
            double sx = Math.cos(p.spurAt()[k]) * s, sz = Math.sin(p.spurAt()[k]) * s;
            double crest = profile(p, sx, sz, s, REACH * 0.97) * p.spurAmp()[k] * (1 - smooth(0.8 * length, length, s));
            e = Math.max(e, crest - side * SPUR_SIDE / RISE);
        }
        // the lesser peaks, each a faceted pyramid of its own rising from its spur, a saddle between it and the summit
        for (Peak pk : p.peaks()) {
            double d = faceted(dx - pk.x(), dz - pk.z(), pk.turn());
            if (d < pk.base()) e = Math.max(e, pk.height() * Math.pow(1 - d / pk.base(), 1.25));
        }
        return Mth.clamp(e * (1 - smooth(REACH - 30, REACH, r)), 0, 1);
    }

    /**
     * How deep the gorges cut at local (dx, dz), in blocks: down the middle of each valley between two
     * spurs, from about {@value #GORGE_FROM} blocks out, deepest along its middle reach and shallowing
     * out at the foot; steep-walled, wandering a little.
     */
    public static double gorge(long seed, int x, int z, int dx, int dz) {
        double r = Math.sqrt((double) dx * dx + (double) dz * dz);
        if (r < GORGE_FROM - 10 || r >= REACH) return 0;
        Plan p = plan(seed);
        double theta = Math.atan2(dz, dx), best = 0;
        for (int j = 0; j < SPURS; j++) {
            double a = p.gorgeAt()[j] + 0.09 * noise(seed).getValue(r / 70.0 + j * 13.1, 4.4);
            double c = Math.cos(theta - a);
            if (c <= 0) continue;
            double s = r * c, side = r * Math.sqrt(Math.max(0, 1 - c * c));
            double width = 8 + 0.045 * s;
            if (side >= width) continue;
            double depth = GORGE_DEPTH * p.gorgeAmp()[j] * smooth(GORGE_FROM, GORGE_FROM + 60, s) * (1 - smooth(REACH - 110, REACH - 25, s));
            best = Math.max(best, depth * smooth(0, 0.75, 1 - side / width));
        }
        return best;
    }

    /** Where the gorges' springs rise, as local {x, z}: one high in each gorge, where its walls are steep. */
    public static int[][] springs(long seed) {
        Plan p = plan(seed);
        int[][] out = new int[SPURS][];
        for (int j = 0; j < SPURS; j++) {
            double r = GORGE_FROM + 75;
            double a = p.gorgeAt()[j] + 0.09 * noise(seed).getValue(r / 70.0 + j * 13.1, 4.4);
            out[j] = new int[]{(int) Math.floor(Math.cos(a) * r), (int) Math.floor(Math.sin(a) * r)};
        }
        return out;
    }

    /**
     * The height the ground at local (dx, dz) is brought to (world x, z), the land's own there being
     * {@code natural}: the summit on the top, the land's own past the foot, the massif between, its
     * crags, ledges and gorges. In a lower world (the summit less far over the land than the shape is
     * drawn for, {@value #RISE}) the crags and gorges are pressed down with it.
     */
    public static double height(long seed, int x, int z, int dx, int dz, int natural, int summit) {
        if (onTop(dx, dz)) return summit;
        double e = field(seed, x, z, dx, dz);
        if (e <= 0) return natural;
        double rise = Math.max(0, summit - natural), scale = Math.min(1.2, rise / RISE);
        double h = natural + rise * e;
        double mid = 4 * e * (1 - e); // the flanks' middle: 1 halfway up, 0 at the summit and the foot
        SimplexNoise n = noise(seed);
        double ridged = 1 - Math.abs(n.getValue(x / 38.0 - 9.4, z / 38.0 + 2.2)); // sharp crests at 1
        h += scale * mid * (CRAG * (2 * ridged - 1) + ROUGH * n.getValue(x / 9.0, z / 9.0));
        // stepped into ledges and cliff bands, the steps wandering up and down across the flank (never level rings round it)
        double shift = 6 * n.getValue(x / 31.0 + 17.3, z / 31.0 - 5.1);
        double f = (h + shift) / LEDGE, k = Math.floor(f);
        double stepped = (k + smooth(0.55, 1, f - k)) * LEDGE - shift;
        h = Mth.lerp(LEDGES * Math.min(1, mid * 1.6) * (0.6 + 0.4 * n.getValue(x / 57.0 - 3.3, z / 57.0 + 8.8)), h, stepped);
        double cut = gorge(seed, x, z, dx, dz) * Math.max(0.3, scale);
        if (cut > 0) h -= Math.min(cut, (h - natural) * 0.85);
        return Math.max(h, natural);
    }

    /** How deep in white local (x, z) is from: under this, its own ground; over it, snow (the summit {@code summit}, the land round the foot {@code foot}). */
    public static double snowLine(long seed, int x, int z, int summit, int foot) {
        return Math.max(MIN_SNOW, summit - SNOW_SHARE * (summit - foot) + 8 * noise(seed).getValue(x / 24.0 + 40.5, z / 24.0 - 11.3));
    }

    /** How much lower than the ground round it local (dx, dz) lies, in blocks (a valley's floor, where the glaciers lie): from the massif's shape 12 blocks each way. */
    public static double hollow(long seed, int x, int z, int dx, int dz, int natural, int summit) {
        double rise = Math.max(0, summit - natural);
        double round = field(seed, x + 12, z, dx + 12, dz) + field(seed, x - 12, z, dx - 12, dz)
                + field(seed, x, z + 12, dx, dz + 12) + field(seed, x, z - 12, dx, dz - 12);
        return rise * (round / 4 - field(seed, x, z, dx, dz)) + gorge(seed, x, z, dx, dz) * 0.5;
    }

    /** The middle of the {@code belt}th cloud belt, the summit {@code summit}, the land round the foot {@code foot}. */
    public static int beltY(int belt, int summit, int foot) {
        return foot + (int) Math.round(BELT_AT[belt] * (summit - foot));
    }

    /**
     * How cloudy it is at (x, y, z) in the {@code belt}th belt, over ground {@code ground} there: over
     * 0, a cloud. Each belt is a sheet of cloud at its height (rising and falling a little), flat
     * underneath and heaped up in puffs, broken into banks and drifts with the sky between: thick
     * against the flank where it falls to the belt's height, thinning out to nothing where the ground
     * is {@link #BELT_DROP} under it. So the massif stands up through it, and the summit over them all.
     */
    public static double cloud(long seed, int x, int y, int z, int belt, int ground, int summit, int foot) {
        SimplexNoise n = noise(seed);
        double mid = beltY(belt, summit, foot) + 4 * n.getValue(x / 140.0 + belt * 7.1, z / 140.0 - 2.9);
        int drop = BELT_DROP[belt];
        if (ground >= mid - 2 || ground < mid - drop || y <= ground + 1) return -1;
        double out = (mid - ground) / drop; // 0 against the flank, 1 at the belt's outer edge
        double patch = n.getValue(x / 46.0 + 7.7 + belt * 31, z / 46.0 - 3.3) + 0.5 * n.getValue(x / 15.0 - belt * 5, z / 15.0 + 50);
        double dense = patch + 0.15 - 1.4 * out * out;
        if (dense <= 0) return -1;
        double puff = 1 + CLOUD_PUFF * Math.min(1, dense) * (0.7 + 0.3 * n.getValue(x / 6.0, y / 4.0 + belt * 9, z / 6.0));
        double dy = y - mid;
        if (dy < -0.35 * puff - 0.5 || dy > puff) return -1; // flat beneath, heaped above
        return dense;
    }

    // ── The summit's height ─────────────────────────────────────────────

    private static final Map<Long, Integer> SUMMITS = new ConcurrentHashMap<>();

    private static long key(long seed, int cx, int cz) {
        return seed * 31 + (((long) cx) << 32 ^ (cz & 0xFFFFFFFFL));
    }

    /**
     * The summit's top (the palace's ground) in the region centred at (cx, cz): y {@value #SUMMIT_Y},
     * or more if need be to stand {@value #HEADROOM} over the highest of the land ({@code land}) within
     * {@value #LOOK} blocks and {@value #MIN_RISE} over the land round its foot; not under
     * {@code minSummit} nor over {@code maxSummit} (the sky less {@value #UNDER_SKY}: in an older, lower
     * world, lower). Worked out once for each region and sky.
     */
    public static int summit(long seed, int cx, int cz, IntBinaryOperator land, int minSummit, int maxSummit) {
        return SUMMITS.computeIfAbsent(key(seed, cx, cz) * 1_000_003L + maxSummit, k -> {
            int highest = land.applyAsInt(cx, cz);
            for (int r = 40; r <= LOOK; r += 40) {
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8;
                    highest = Math.max(highest, land.applyAsInt(cx + (int) Math.round(Math.cos(a) * r), cz + (int) Math.round(Math.sin(a) * r)));
                }
            }
            int want = Math.max(SUMMIT_Y, Math.max(highest + HEADROOM, foot(seed, cx, cz, land) + MIN_RISE));
            return Mth.clamp(want, minSummit, maxSummit);
        });
    }

    private static final Map<Long, Integer> FEET = new ConcurrentHashMap<>();

    /** The land's height round the massif's foot in the region centred at (cx, cz), on average: worked out once a region. */
    public static int foot(long seed, int cx, int cz, IntBinaryOperator land) {
        return FEET.computeIfAbsent(key(seed, cx, cz), k -> {
            int sum = 0;
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                sum += land.applyAsInt(cx + (int) Math.round(Math.cos(a) * REACH), cz + (int) Math.round(Math.sin(a) * REACH));
            }
            return Math.round(sum / 16f);
        });
    }

    // ── The stair ───────────────────────────────────────────────────────

    /** The nearest of the stair to a place: how far off its middle, the height of its way there, the stretch, and whether the place is on its outer (drop) side. */
    public record Hit(double d, double y, int index, boolean outer) {}

    /** What stands by the way: the gate at its foot, a rest pavilion over it, a shrine at its inner side. */
    public enum SiteKind { GATE, PAVILION, SHRINE }

    /**
     * A place on the way where something is built: its kind, the stretch it is at, its middle (local
     * x, z), its floor (the way's height there, whole blocks), the way up it (a unit step along x or
     * z, the axis nearest the stair's own way there), which side is the inner, toward the mountain
     * (1: to the right of the way up, -1: to the left), and its number among its kind from the foot.
     */
    public record Site(SiteKind kind, int index, int x, int z, int y, int ux, int uz, int inner, int number) {

        /** How far (x, z) is along the way up from the middle. */
        public int along(int x, int z) {
            return (x - this.x) * ux + (z - this.z) * uz;
        }

        /** How far (x, z) is across the way, to the right of the way up. */
        public int across(int x, int z) {
            return -(x - this.x) * uz + (z - this.z) * ux;
        }

        /** Whether (x, z) is within {@code along} along and {@code across} across of its middle. */
        public boolean within(int x, int z, int along, int across) {
            return Math.abs(along(x, z)) <= along && Math.abs(across(x, z)) <= across;
        }
    }

    /**
     * The stair up the mountain: its middle line, a point a block along, from the foot ({@code 0}) to
     * the summit's south edge, as local x, z and the height of its way (to the half block: under it
     * whole blocks, over a half, a slab); and the gate, pavilions and shrines along it.
     */
    public static final class Path {

        private static final int CELL = 8;
        public final double[] x, z, y;
        /** The ground under each point, as the mountain shapes it (before the way is cut or built out over it). */
        public final double[] ground;
        public final List<Site> sites;
        private final Map<Long, int[]> cells = new HashMap<>();

        Path(double[] x, double[] z, double[] y, double[] ground) {
            this.ground = ground;
            this.x = x;
            this.z = z;
            this.y = y;
            Map<Long, List<Integer>> build = new HashMap<>();
            int pad = (int) Math.ceil(SHOULDER + BANK) + 2;
            for (int i = 0; i < x.length; i++) {
                for (int cx = Math.floorDiv((int) Math.floor(x[i]) - pad, CELL); cx <= Math.floorDiv((int) Math.floor(x[i]) + pad, CELL); cx++) {
                    for (int cz = Math.floorDiv((int) Math.floor(z[i]) - pad, CELL); cz <= Math.floorDiv((int) Math.floor(z[i]) + pad, CELL); cz++) {
                        build.computeIfAbsent(((long) cx << 32) ^ (cz & 0xFFFFFFFFL), k -> new ArrayList<>()).add(i);
                    }
                }
            }
            build.forEach((k, v) -> cells.put(k, v.stream().mapToInt(Integer::intValue).toArray()));
            this.sites = List.copyOf(sites());
        }

        public int size() {
            return x.length;
        }

        /** The nearest of the stair to local (dx, dz) (a block's middle), if within {@code within} of its middle line; else null. */
        @Nullable
        public Hit nearest(int dx, int dz, double within) {
            double px = dx + 0.5, pz = dz + 0.5;
            int[] near = cells.get(((long) Math.floorDiv(dx, CELL) << 32) ^ (Math.floorDiv(dz, CELL) & 0xFFFFFFFFL));
            if (near == null) return null;
            double best = within * within;
            Hit hit = null;
            for (int i : near) {
                if (i + 1 >= x.length) continue;
                double ax = x[i], az = z[i], bx = x[i + 1] - ax, bz = z[i + 1] - az;
                double len2 = bx * bx + bz * bz;
                double t = len2 <= 0 ? 0 : Mth.clamp(((px - ax) * bx + (pz - az) * bz) / len2, 0, 1);
                double qx = ax + bx * t, qz = az + bz * t;
                double d2 = (px - qx) * (px - qx) + (pz - qz) * (pz - qz);
                if (d2 <= best) {
                    best = d2;
                    boolean outer = (px - qx) * qx + (pz - qz) * qz > 0;
                    hit = new Hit(Math.sqrt(d2), Mth.lerp(t, y[i], y[i + 1]), t < 0.5 ? i : i + 1, outer);
                }
            }
            return hit;
        }

        /** The site whose ground holds local (dx, dz), if any. */
        @Nullable
        public Site siteAt(int dx, int dz) {
            for (Site s : sites) {
                if (Math.abs(dx - s.x()) > 9 || Math.abs(dz - s.z()) > 9) continue;
                if (s.within(dx, dz, reachAlong(s.kind()), reachAcross(s.kind()))) return s;
            }
            return null;
        }

        /** How far a site of this kind reaches along the way from its middle, and across it. */
        public static int reachAlong(SiteKind kind) {
            return kind == SiteKind.GATE ? 2 : 4;
        }

        public static int reachAcross(SiteKind kind) {
            return kind == SiteKind.GATE ? 8 : kind == SiteKind.SHRINE ? 7 : 4;
        }

        /** How nearly the way runs along an axis about stretch {@code i} (1 straight along one), and that axis's step. */
        private int[] axisAt(int i) {
            int a = Math.max(0, i - 4), b = Math.min(x.length - 1, i + 4);
            double tx = x[b] - x[a], tz = z[b] - z[a], len = Math.hypot(tx, tz);
            if (len < 1e-6) return new int[]{0, 1, 0};
            tx /= len;
            tz /= len;
            boolean alongX = Math.abs(tx) >= Math.abs(tz);
            int ux = alongX ? (tx > 0 ? 1 : -1) : 0, uz = alongX ? 0 : (tz > 0 ? 1 : -1);
            return new int[]{ux, uz, (int) Math.round(1000 * Math.max(Math.abs(tx), Math.abs(tz)))};
        }

        /** The way's own straightness about stretch {@code i}: how far its middle strays from the straight line over eight blocks either way. */
        private double bend(int i) {
            int a = Math.max(0, i - 8), b = Math.min(x.length - 1, i + 8);
            double tx = x[b] - x[a], tz = z[b] - z[a], len = Math.max(1e-6, Math.hypot(tx, tz)), worst = 0;
            for (int j = a; j <= b; j++) worst = Math.max(worst, Math.abs((x[j] - x[a]) * tz - (z[j] - z[a]) * tx) / len);
            return worst;
        }

        /** The best stretch near {@code want} (within {@code slack}) for a site: the way straight and along an axis there. */
        private int best(int want, int slack) {
            int best = Math.max(8, Math.min(x.length - 9, want));
            double score = Double.MAX_VALUE;
            for (int i = Math.max(8, want - slack); i <= Math.min(x.length - 9, want + slack); i++) {
                double off = 0; // how far the way stands off the ground round here (a site wants its own ground, not a bridge nor a cutting)
                for (int j = Math.max(0, i - 4); j <= Math.min(x.length - 1, i + 4); j++) off += Math.abs(ground[j] - y[j]);
                double s = (1000 - axisAt(i)[2]) / 40.0 + bend(i) * 8 + Math.abs(i - want) * 0.01 + off / 9 * 0.6;
                if (s < score) {
                    score = s;
                    best = i;
                }
            }
            return best;
        }

        private Site site(SiteKind kind, int i, int number) {
            int[] axis = axisAt(i);
            int sx = (int) Math.floor(x[i]), sz = (int) Math.floor(z[i]);
            // the inner side: the one toward the centre (the mountain), to the right of the way up or the left
            int rx = -axis[1], rz = axis[0];
            int inner = rx * -sx + rz * -sz > 0 ? 1 : -1;
            return new Site(kind, i, sx, sz, (int) Math.floor(y[i]), axis[0], axis[1], inner, number);
        }

        /** The gate a little way up from the foot; then, every {@value #PAVILION_EVERY} blocks or so, a pavilion, a shrine halfway between; none too near the summit. */
        private List<Site> sites() {
            List<Site> out = new ArrayList<>();
            int n = x.length;
            if (n < 40) return out;
            out.add(site(SiteKind.GATE, best(14, 10), 0));
            int pavilions = 0, shrines = 0;
            for (int m = 1; m * PAVILION_EVERY < n - 70; m++) {
                int at = best(m * PAVILION_EVERY, 40);
                out.add(site(SiteKind.PAVILION, at, pavilions++));
            }
            for (int m = 0; (m + 0.5) * PAVILION_EVERY < n - 70; m++) {
                int want = (int) Math.round((m + 0.5) * PAVILION_EVERY);
                if (want < 60) continue;
                out.add(site(SiteKind.SHRINE, best(want, 40), shrines++));
            }
            // never two too near (a later one is dropped)
            List<Site> kept = new ArrayList<>();
            for (Site s : out) {
                boolean clear = true;
                for (Site o : kept) if (Math.abs(o.x() - s.x()) <= 19 && Math.abs(o.z() - s.z()) <= 19) clear = false;
                if (clear) kept.add(s);
            }
            return kept;
        }
    }

    private static final Map<Long, Path> PATHS = new ConcurrentHashMap<>();

    /** Which way the stair winds round, from the seed: 1 or -1. */
    public static int turning(long seed) {
        return (seed & 1) == 0 ? 1 : -1;
    }

    /**
     * The stair in the region centred at (cx, cz), its summit {@code summit}, the land's own height
     * {@code land} (worked out once a region). Laid out from the summit's south edge down, walked
     * a block at a time round the massif the way it winds ({@link #turning}): each step turning
     * gently, whichever way keeps the ground {@value #AHEAD} blocks ahead nearest to falling
     * {@value #CLIMB} a block (the flank as it is really shaped there), never back toward the
     * summit and never near a turn of its own laid before, until it is down on the land at the
     * foot. Then its way follows the ground (never more than a few blocks off its even descent),
     * kept to a climb of {@value #MAX_CLIMB} a block (and eased), up to the summit: where a ridge
     * stands in its way it is cut through, and where the ground falls away under it, it crosses on
     * bridges.
     */
    public static Path path(long seed, int cx, int cz, int summit, IntBinaryOperator land) {
        return PATHS.computeIfAbsent(key(seed, cx, cz) * 1_000_003L + summit, k -> {
            int dir = turning(seed);
            Peak[] peaks = plan(seed).peaks();
            int footY = foot(seed, cx, cz, land);
            Map<Long, Integer> lands = new HashMap<>();
            Ground ground = (lx, lz) -> Math.min(summit, height(seed, cx + lx, cz + lz, lx, lz, landNear(lands, land, cx + lx, cz + lz), summit));
            List<double[]> pts = new ArrayList<>();
            Map<Long, List<Integer>> crowd = new HashMap<>();
            double px = 0, pz = TOP + 1, h = summit;
            double heading = dir > 0 ? Math.PI : 0; // going down, round the peak the way it winds
            pts.add(new double[]{px, pz, h});
            for (int i = 1; i < 9000; i++) {
                double best = Double.MAX_VALUE, bh = heading;
                // each rate of turning tried a stretch ahead, kept to: the one whose way keeps nearest the climb, going on round
                for (int turn = -TURNS; turn <= TURNS; turn++) {
                    double a = heading, qx = px, qz = pz, cost = Math.abs(turn * TURN) * 2;
                    for (int ahead = 1; ahead <= AHEAD; ahead++) {
                        a += turn * TURN;
                        qx += Math.cos(a);
                        qz += Math.sin(a);
                        double rq = Math.hypot(qx, qz);
                        if (rq < TOP + 0.5) cost += 100; // never back onto the summit
                        if (rq > REACH - 4) cost += 20;
                        // kept near the massif's own contour at its height, so it winds outward as it falls and comes down to the foot
                        cost += Math.abs(rq - contour(h - ahead * CLIMB, summit, footY)) * CONTOUR / AHEAD;
                        for (Peak pk : peaks) { // never round and round a lesser peak's slopes
                            if (Math.hypot(qx - pk.x(), qz - pk.z()) < pk.base() * 0.75) cost += 25.0 / AHEAD;
                        }
                        cost += Math.abs(ground.at((int) Math.floor(qx), (int) Math.floor(qz)) - (h - ahead * CLIMB)) / AHEAD;
                        double round = (-qz * Math.cos(a) + qx * Math.sin(a)) / rq * dir; // how much it goes on round the peak
                        if (round < 0.3) cost += 10 * (0.3 - round) / AHEAD;
                        if (ahead <= 2 || ahead % 3 == 0) {
                            for (int j : near(crowd, qx, qz)) {
                                if (j >= i - 12) continue;
                                double apart = Math.hypot(pts.get(j)[0] - qx, pts.get(j)[1] - qz);
                                if (apart < 2 * SHOULDER + 10) cost += 400; // never onto a turn of its own
                                else if (ahead <= 2 && j < i - 40 && apart < 2 * SHOULDER + 2 * BANK) cost += 30; // nor its bank onto another's
                            }
                        }
                    }
                    if (cost < best) {
                        best = cost;
                        bh = heading + turn * TURN;
                    }
                }
                heading = bh;
                px += Math.cos(heading);
                pz += Math.sin(heading);
                int lx = (int) Math.floor(px), lz = (int) Math.floor(pz);
                // falling evenly; but on the low flanks and the land at the foot never sunk far under the ground, rising over it where it rolls
                h = field(seed, cx + lx, cz + lz, lx, lz) < 0.12 ? Math.max(h - CLIMB, ground.at(lx, lz) - KEEP) : h - CLIMB;
                pts.add(new double[]{px, pz, h});
                crowd.computeIfAbsent(cell(px, pz), c -> new ArrayList<>()).add(i);
                double r = Math.hypot(px, pz);
                if (r >= REACH - 5 || h <= ground.at(lx, lz) + 1 && field(seed, cx + lx, cz + lz, lx, lz) <= 0.03) break; // down on the land
            }
            java.util.Collections.reverse(pts);
            int n = pts.size();
            double[] x = new double[n], z = new double[n], raw = new double[n], y = new double[n], under = new double[n];
            for (int i = 0; i < n; i++) {
                x[i] = pts.get(i)[0];
                z[i] = pts.get(i)[1];
                // the ground, but never far off the way's own even descent: through a ridge it is cut, over a hollow built out or bridged
                under[i] = ground.at((int) Math.floor(x[i]), (int) Math.floor(z[i]));
                raw[i] = Math.max(DRY, Mth.clamp(ground.at((int) Math.floor(x[i]), (int) Math.floor(z[i])), pts.get(i)[2] - KEEP, pts.get(i)[2] + KEEP));
            }
            climb(raw, y, summit);
            // eased: a running mean, then kept to the climb again
            double[] eased = new double[n];
            for (int i = 0; i < n; i++) {
                double sum = 0;
                int count = 0;
                for (int j = Math.max(0, i - 4); j <= Math.min(n - 1, i + 4); j++) {
                    sum += y[j];
                    count++;
                }
                eased[i] = sum / count;
            }
            climb(eased, y, summit);
            return new Path(x, z, y, under);
        });
    }

    /** The ground's height at local (x, z), as the mountain shapes it. */
    @FunctionalInterface
    private interface Ground {
        double at(int lx, int lz);
    }

    /** How far the stair may turn a block, in how many steps either way it is tried, and how far ahead each is looked along. */
    private static final double TURN = 0.025;
    private static final int TURNS = 8, AHEAD = 14;
    /** How strongly the way is held to the massif's contour at its height (cost a block off it, a block ahead). */
    private static final double CONTOUR = 0.04;

    /** About how far out the massif's own profile falls to height y (the summit {@code summit}, its foot {@code foot}). */
    static double contour(double y, int summit, int foot) {
        double e = Mth.clamp((y - foot) / Math.max(1.0, summit - foot), 0, 1);
        return TOP + (REACH * 0.95 - TOP) * (1 - Math.pow(e, 1 / POWER));
    }

    /** The way never lower than this (over the sea's level, never wading through a lake at the foot). */
    private static final int DRY = 64;

    /** How far the way's height may stray from its even descent to follow the ground. */
    private static final double KEEP = 5;

    /** The land's height at (x, z), from its heights on a grid of four blocks ({@code lands}, filled as asked), between them in a straight line. */
    private static int landNear(Map<Long, Integer> lands, IntBinaryOperator land, int x, int z) {
        int gx = Math.floorDiv(x, 4), gz = Math.floorDiv(z, 4);
        double fx = (x - gx * 4) / 4.0, fz = (z - gz * 4) / 4.0;
        double h = 0;
        for (int i = 0; i <= 1; i++) for (int j = 0; j <= 1; j++) {
            int ax = (gx + i) * 4, az = (gz + j) * 4;
            int v = lands.computeIfAbsent(((long) ax << 32) ^ (az & 0xFFFFFFFFL), kk -> land.applyAsInt(ax, az));
            h += v * (i == 0 ? 1 - fx : fx) * (j == 0 ? 1 - fz : fz);
        }
        return (int) Math.round(h);
    }

    private static long cell(double x, double z) {
        return ((long) Math.floorDiv((int) Math.floor(x), 8) << 32) ^ (Math.floorDiv((int) Math.floor(z), 8) & 0xFFFFFFFFL);
    }

    /** The points laid so far in the cells round (x, z). */
    private static List<Integer> near(Map<Long, List<Integer>> crowd, double x, double z) {
        List<Integer> out = new ArrayList<>();
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
            List<Integer> c = crowd.get(cell(x + i * 8, z + j * 8));
            if (c != null) out.addAll(c);
        }
        return out;
    }

    /** {@code out}: the heights {@code want}, the last brought to the summit, each kept within {@value #MAX_CLIMB} of the next. */
    private static void climb(double[] want, double[] out, int summit) {
        int n = want.length;
        out[n - 1] = summit;
        for (int i = n - 2; i >= 0; i--) out[i] = Mth.clamp(want[i], out[i + 1] - MAX_CLIMB, Math.min(summit, out[i + 1] + MAX_CLIMB));
    }
}
