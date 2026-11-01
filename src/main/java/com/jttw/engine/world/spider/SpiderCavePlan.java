package com.jttw.engine.world.spider;

import java.util.ArrayList;
import java.util.List;

/**
 * The Spider Cave (盘丝洞) under Silk-Web Ridge, worked out once, the same in every world (pure:
 * unit-tested). Local coordinates are block offsets from the region's centre, +z south; heights
 * from the cave's floor level {@code F} (local y 0 is the first block over the floor, -1 the floor).
 * <ul>
 *   <li><b>the way in</b>: a passage {@value #PASSAGE_HALF}×2+1 wide from the back of the thatched
 *       cottage at the ridge's southern foot ({@link SilkRidgeWorks}) north into the rock;</li>
 *   <li><b>the Silk Hall</b> (丝厅): a great dome {@value #HALL_RX}×2 by {@value #HALL_RZ}×2 blocks
 *       and {@value #HALL_H} high, pillars of woven silk from floor to vault, a great web hung across
 *       its northern end, cocoons hanging from the vault, egg sacs glowing green in it;</li>
 *   <li><b>the seven bowers</b> (七间绣房), the sisters', strung one after another into the ridge
 *       by tunnels, west, north and round to the east: the youngest's first, the eldest's last.
 *       Each has a dais of dark planks under a carpet of its sister's colour, a loom, silk drapes;
 *       the last the sisters' treasury chest. The tunnel on from each bower is shut by a wall of
 *       sealing silk ({@link #SEAL}) that only its sister's death loosens.</li>
 * </ul>
 * And who keeps it ({@link #POSTS}): the hall's garrison, and in each bower its sister and her sons.
 */
public final class SpiderCavePlan {

    private SpiderCavePlan() {}

    // What a block of the cave is (the feature lays each as a block)
    public static final int NONE = 0, AIR = 1, FLOOR = 2, SEAL = 3, COBWEB = 4, SILK = 5, GLOW = 6, CARPET = 7, LOOM = 8,
            CHEST = 9, BINDING_WEB = 10, CHAIN = 11, LANTERN = 12, BONES = 13, PLANKS = 14;
    // ...and the Silk Hall's own: its floor, what lies on it, its egg sacs, its victims, the light let in
    public static final int MUD = 15, MOSS = 16, MOSS_CARPET = 17, COARSE = 18, WATER = 19, BONE_FLOOR = 20, SKULL = 21, BONE_LYING = 22,
            SILK_FLOOR = 23, STRAND = 24, MEMBRANE = 25, EGG = 26, EGG_GOLD = 27, WOOL = 28, LICHEN_UP = 29, LICHEN_DOWN = 30,
            CRACK = 31, BEAM = 32, LIGHT = 33, MUSHROOM = 34, ROOTS = 35, GRASS = 36, SHROOM = 37;

    public static final int PASSAGE_HALF = 2, PASSAGE_H = 5, PASSAGE_N = 22, PASSAGE_S = 45;
    public static final int HALL_X = 0, HALL_Z = 4, HALL_RX = 34, HALL_RZ = 22, HALL_H = 26;
    public static final int TUNNEL_HALF = 2, TUNNEL_H = 5;
    /** The great web across the hall's north end: in this plane, so wide and so high. */
    public static final int WEB_Z = -9, WEB_HALF = 22, WEB_LOW = 2, WEB_HIGH = 22;

    /** A dome: centred at (x, z), its half-widths, its height at the middle. */
    public record Chamber(int x, int z, int rx, int rz, int h) {
        /** How far out (lx, lz) is, as a share of its reach: 0 at the middle, 1 at the wall's foot. */
        public double reach(double lx, double lz) {
            double dx = (lx - x) / rx, dz = (lz - z) / rz;
            return Math.sqrt(dx * dx + dz * dz);
        }

        /** Its height over (lx, lz): the dome's, or -1 outside. */
        public int heightAt(double lx, double lz) {
            double r = reach(lx, lz);
            if (r >= 1) return -1;
            return (int) Math.floor(h * Math.sqrt(1 - r * r));
        }
    }

    public static final Chamber HALL = new Chamber(HALL_X, HALL_Z, HALL_RX, HALL_RZ, HALL_H);
    /** The seven bowers, in the order they are reached: the youngest sister's (the seventh) first, the eldest's last. */
    public static final List<Chamber> BOWERS = List.of(
            new Chamber(-50, -12, 13, 11, 12),
            new Chamber(-74, -42, 13, 11, 12),
            new Chamber(-40, -62, 13, 11, 13),
            new Chamber(-2, -52, 14, 11, 13),
            new Chamber(34, -66, 13, 11, 13),
            new Chamber(68, -40, 13, 11, 13),
            new Chamber(60, -6, 16, 13, 15));
    public static final int BOWER_COUNT = 7;

    /** A tunnel from one chamber's middle to the next's. */
    public record Tunnel(double ax, double az, double bx, double bz) {
        double length() {
            return Math.hypot(bx - ax, bz - az);
        }

        /** How far along it (lx, lz) lies (0 at a, its length at b) and how far off its line. */
        double[] along(double lx, double lz) {
            double dx = bx - ax, dz = bz - az, len = length();
            double t = ((lx - ax) * dx + (lz - az) * dz) / len;
            double tc = Math.max(0, Math.min(len, t));
            double px = ax + dx * tc / len, pz = az + dz * tc / len;
            return new double[]{t, Math.hypot(lx - px, lz - pz)};
        }
    }

    /** The tunnels: from the hall to the first bower, then each bower to the next. */
    public static final List<Tunnel> TUNNELS;
    /** How far along each tunnel its seal stands (none on the first, from the hall), or -1. */
    private static final double[] SEAL_AT;

    static {
        List<Tunnel> t = new ArrayList<>();
        Chamber prev = HALL;
        for (Chamber b : BOWERS) {
            t.add(new Tunnel(prev.x(), prev.z(), b.x(), b.z()));
            prev = b;
        }
        TUNNELS = List.copyOf(t);
        SEAL_AT = new double[TUNNELS.size()];
        for (int i = 0; i < TUNNELS.size(); i++) {
            if (i == 0) {
                SEAL_AT[i] = -1;
                continue;
            }
            // just out of the bower it leaves
            Tunnel tun = TUNNELS.get(i);
            Chamber from = BOWERS.get(i - 1);
            double s = 0;
            while (s < tun.length() && from.reach(tun.ax() + (tun.bx() - tun.ax()) * s / tun.length(),
                    tun.az() + (tun.bz() - tun.az()) * s / tun.length()) < 1) s += 0.5;
            SEAL_AT[i] = s + 1.5;
        }
    }

    private static int hash(int x, int y, int z, int salt) {
        long h = x * 3129871L ^ z * 116129781L ^ y * 0x9E3779B9L ^ salt * 0x5DEECE66DL;
        h = h * h * 42317861L + h * 11L;
        return (int) Math.floorMod(h >> 16, 1000L);
    }

    // ── Where the cave is ───────────────────────────────────────────────

    /** The passage's interior height over (lx, lz), or -1. */
    static int passageHeight(int lx, int lz) {
        return Math.abs(lx) <= PASSAGE_HALF && lz >= PASSAGE_N && lz <= PASSAGE_S ? PASSAGE_H - (Math.abs(lx) == PASSAGE_HALF ? 1 : 0) : -1;
    }

    /** Tunnel {@code i}'s interior height over (lx, lz), or -1; its seal's place along it in {@code seal}. */
    static int tunnelHeight(int i, int lx, int lz) {
        double[] a = TUNNELS.get(i).along(lx + 0.5, lz + 0.5);
        if (a[1] > TUNNEL_HALF + 0.5 || a[0] < 0 || a[0] > TUNNELS.get(i).length()) return -1;
        return TUNNEL_H - (a[1] > TUNNEL_HALF - 0.5 ? 1 : 0);
    }

    /** Which bower (0..6) holds (lx, lz), or -1. */
    public static int bowerAt(int lx, int lz) {
        for (int i = 0; i < BOWERS.size(); i++) if (BOWERS.get(i).heightAt(lx + 0.5, lz + 0.5) >= 0) return i;
        return -1;
    }

    /** The top of the cave's open space over (lx, lz) (its local y), or -1 where none of it is. */
    public static int roof(int lx, int lz) {
        int top = Math.max(HALL.heightAt(lx + 0.5, lz + 0.5), passageHeight(lx, lz));
        for (Chamber b : BOWERS) top = Math.max(top, b.heightAt(lx + 0.5, lz + 0.5));
        for (int i = 0; i < TUNNELS.size(); i++) top = Math.max(top, tunnelHeight(i, lx, lz));
        return top;
    }

    /** The widest the cave reaches from the region's centre, each way: for the chunks it may lie in. */
    public static final int MIN_X = -90, MAX_X = 80, MIN_Z = -80, MAX_Z = PASSAGE_S;

    /** Whether column (lx, lz) is any of the cave's. */
    public static boolean covers(int lx, int lz) {
        return lx >= MIN_X && lx <= MAX_X && lz >= MIN_Z && lz <= MAX_Z && roof(lx, lz) >= 0;
    }

    /** Whether (lx, lz) is in tunnel {@code i}'s seal (the wall of silk that shuts it), any height. */
    public static boolean inSeal(int i, int lx, int lz) {
        if (i <= 0 || i >= TUNNELS.size() || SEAL_AT[i] < 0) return false;
        double[] a = TUNNELS.get(i).along(lx + 0.5, lz + 0.5);
        return a[1] <= TUNNEL_HALF + 0.5 && a[0] >= SEAL_AT[i] && a[0] < SEAL_AT[i] + 2;
    }

    /** The seal that bower {@code bower}'s sister holds (the tunnel on from her bower): its tunnel's index, or -1 for the last. */
    public static int sealOf(int bower) {
        return bower + 1 < TUNNELS.size() ? bower + 1 : -1;
    }

    /** The seal's blocks, local (x, y, z): every place of tunnel {@code tunnel}'s seal. */
    public static List<int[]> sealBlocks(int tunnel) {
        List<int[]> out = new ArrayList<>();
        if (tunnel <= 0 || tunnel >= TUNNELS.size()) return out;
        for (int x = MIN_X; x <= MAX_X; x++) for (int z = MIN_Z; z <= MAX_Z; z++) {
            if (!inSeal(tunnel, x, z)) continue;
            int h = tunnelHeight(tunnel, x, z);
            for (int y = 0; y < h; y++) out.add(new int[]{x, y, z});
        }
        return out;
    }

    /** What the block at local (lx, ly, lz) is: {@link #NONE} where the cave is not. */
    public static int at(int lx, int ly, int lz) {
        int roof = roof(lx, lz);
        if (roof < 0 || ly < -1 || ly > roof) return NONE;
        if (ly == -1) {
            int b = bowerAt(lx, lz);
            if (b >= 0 && BOWERS.get(b).reach(lx + 0.5, lz + 0.5) < 0.55) return PLANKS;
            if (b < 0 && HALL.heightAt(lx + 0.5, lz + 0.5) >= 0) return hallFloor(lx, lz);
            return FLOOR;
        }
        if (ly == roof) {
            // the rock over it stays; but where the light gets in, it is cracked through
            return bowerAt(lx, lz) < 0 && HALL.heightAt(lx + 0.5, lz + 0.5) >= 0 && crack(lx, lz) ? CRACK : NONE;
        }
        for (int i = 1; i < TUNNELS.size(); i++) if (inSeal(i, lx, lz) && ly < tunnelHeight(i, lx, lz)) return SEAL;
        int hall = HALL.heightAt(lx + 0.5, lz + 0.5);
        int b = bowerAt(lx, lz);
        int fromRoof = roof - ly;
        // the hall
        if (hall >= 0 && b < 0) return hall(lx, ly, lz, roof);
        // a bower
        if (b >= 0) {
            Chamber c = BOWERS.get(b);
            double r = c.reach(lx + 0.5, lz + 0.5);
            if (ly == 0 && r < 0.5) {
                if (lx == c.x() + c.rx() / 2 && lz == c.z()) return LOOM;
                if (b == BOWERS.size() - 1 && lx == c.x() && lz == c.z() - c.rz() / 2) return CHEST;
                return CARPET;
            }
            if (lx == c.x() && lz == c.z()) {
                if (fromRoof >= 1 && ly >= 5) return ly == 5 ? LANTERN : CHAIN;
            }
            if (fromRoof == 1 && hash(lx, ly, lz, 8) < 60) return GLOW;
            if (r > 0.7 && fromRoof <= 3 && drape(c, lx, lz)) return SILK;
            if (fromRoof <= 1 && hash(lx, ly, lz, 9) < 160) return COBWEB;
            if (ly <= 1 && r > 0.82 && hash(lx, ly, lz, 10) < 250) return COBWEB;
            return AIR;
        }
        // the passage and the tunnels: webs in their upper corners, and here and there a binding web strung low
        if (fromRoof == 1 && hash(lx, ly, lz, 11) < 90) return COBWEB;
        if (ly == 0 && passageHeight(lx, lz) < 0 && hash(lx, ly, lz, 12) < 25 && !nearSeal(lx, lz)) return BINDING_WEB;
        return AIR;
    }

    /** Whether (lx, lz) is within three blocks of any seal (no web is strung by one). */
    private static boolean nearSeal(int lx, int lz) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            for (int i = 1; i < TUNNELS.size(); i++) if (inSeal(i, lx + dx, lz + dz)) return true;
        }
        return false;
    }

    // ── The Silk Hall ───────────────────────────────────────────────────

    /** The hall's ten silk pillars, in a ring: their middles (x, z). */
    static final double[][] PILLARS = new double[10][];

    /** Where the light gets in through cracks in the vault: the middle of each shaft (x, z), and its crack's run (radians). */
    static final double[][] SHAFTS = {{-5, 3, 0.5}, {-20, -8, 2.1}, {14, 1, 1.2}};

    /** An egg sac: a ball of glowing eggs in a skin of silk. Its middle, its radius, the eggs' code. */
    record Sac(double x, double y, double z, double r, int code) {}

    /** The egg sacs: in clutches heaped on the floor against the walls, and hung from the vault on strands. */
    static final List<Sac> SACS = new ArrayList<>();
    /** The strands they hang by: (x, z, from y up to the vault). */
    static final List<int[]> SAC_STRANDS = new ArrayList<>();

    /** The cocooned: (x, z, the bottom of the bundle, kind: 0 wrapped whole, 1 a skull looking out, 2 a great bundle). */
    static final int[][] COCOONS = {{-4, 11, 5, 0}, {-14, 5, 7, 1}, {5, -3, 6, 2}, {-24, 2, 4, 0}, {10, 10, 8, 1}, {-8, -4, 9, 0},
            {16, -3, 5, 1}, {-16, -3, 5, 2}, {12, 13, 7, 0}, {-27, 10, 4, 1}, {24, 4, 5, 0}, {-10, 20, 4, 1}};

    /** Curtains of silk hung between neighbouring pillars (their indices). */
    static final int[][] CURTAINS = {{3, 4}, {5, 6}, {8, 9}, {0, 1}};

    static {
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI / 5 + 0.3;
            PILLARS[k] = new double[]{HALL_X + Math.cos(a) * HALL_RX * 0.58, HALL_Z + Math.sin(a) * HALL_RZ * 0.58};
        }
        // clutches: (x, z, hung (1) or heaped (0), how many)
        int[][] clutches = {{-29, 3, 0, 5}, {-16, 11, 0, 4}, {12, -13, 0, 5}, {28, 11, 0, 4}, {-3, -6, 0, 3}, {-10, 10, 1, 3}, {-11, 1, 1, 4}, {-26, -3, 1, 3}};
        int[] eggs = {EGG_GOLD, GLOW, EGG};
        for (int c = 0; c < clutches.length; c++) {
            int[] cl = clutches[c];
            boolean hung = cl[2] == 1;
            double top = HALL.heightAt(cl[0] + 0.5, cl[1] + 0.5);
            double cy = hung ? top - 11 : 0;
            for (int i = 0; i < cl[3]; i++) {
                double r = i == 0 ? 2.1 : i % 3 == 1 ? 1.6 : 0.9;
                double a = i * 2.4 + c, d = i == 0 ? 0 : 1.7 + (i % 2) * 1.0;
                int x = (int) Math.round(cl[0] + Math.cos(a) * d), z = (int) Math.round(cl[1] + Math.sin(a) * d);
                double y = hung ? cy - (i % 2) * 1.5 - (i == 0 ? 0 : 1) : Math.floor(r - 0.4);
                SACS.add(new Sac(x + 0.5, y + 0.5, z + 0.5, r, eggs[(c + i) % eggs.length]));
                if (hung) SAC_STRANDS.add(new int[]{x, z, (int) Math.ceil(y + r)});
            }
        }
    }

    /** A smooth noise over the floor, 0..1, in cells of {@code cell} blocks. */
    static double noise(int lx, int lz, int cell, int salt) {
        int gx = Math.floorDiv(lx, cell), gz = Math.floorDiv(lz, cell);
        double fx = (lx - gx * cell) / (double) cell, fz = (lz - gz * cell) / (double) cell;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = hash(gx, 0, gz, salt), b = hash(gx + 1, 0, gz, salt), c = hash(gx, 0, gz + 1, salt), d = hash(gx + 1, 0, gz + 1, salt);
        return ((a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz) / 1000.0;
    }

    /** How wide a shaft of light is at height {@code ly} under a vault at {@code roof}: narrow at the crack, wider at the floor. */
    static double shaftRadius(int ly, int roof) {
        return 1.25 + 0.55 * (1 - ly / (double) Math.max(1, roof));
    }

    /** How far (lx, lz) is from the middle of the nearest shaft of light. */
    static double toShaft(int lx, int lz) {
        double best = Double.MAX_VALUE;
        for (double[] s : SHAFTS) best = Math.min(best, Math.hypot(lx + 0.5 - s[0], lz + 0.5 - s[1]));
        return best;
    }

    /** Whether the vault over (lx, lz) is cracked through to the light: a jagged split across each shaft. */
    static boolean crack(int lx, int lz) {
        for (double[] s : SHAFTS) {
            double dx = lx + 0.5 - s[0], dz = lz + 0.5 - s[1];
            double along = dx * Math.cos(s[2]) + dz * Math.sin(s[2]), off = -dx * Math.sin(s[2]) + dz * Math.cos(s[2]);
            if (Math.abs(along) > 5.5) continue;
            double wiggle = Math.sin(along * 1.3 + s[0]) * 0.7;
            double width = 1.4 - Math.abs(along) / 5.5;
            if (Math.abs(off - wiggle) < Math.max(0.5, width)) return true;
        }
        return false;
    }

    /** The hall's floor: mud and puddles, moss under the light, beds of old silk, a boneyard; packed earth between. */
    static int hallFloor(int lx, int lz) {
        double shaft = toShaft(lx, lz);
        if (shaft < 1.6) return WATER; // a pool where the light falls
        if (shaft < 4.5) return MOSS;
        if (nearPillar(lx, lz, 3.2) || Math.abs(lx) <= PASSAGE_HALF + 1 && lz > 14) return hash(lx, 0, lz, 60) < 500 ? FLOOR : COARSE;
        double wet = noise(lx, lz, 9, 52), silk = noise(lx, lz, 7, 50), bone = noise(lx, lz, 6, 51);
        if (wet < 0.2 && HALL.reach(lx + 0.5, lz + 0.5) < 0.85) return WATER;
        if (wet < 0.3) return MUD;
        if (bone > 0.74) return BONE_FLOOR;
        if (silk > 0.72) return SILK_FLOOR;
        if (silk < 0.24) return MOSS;
        return bone < 0.4 ? COARSE : FLOOR;
    }

    private static boolean nearPillar(double lx, double lz, double within) {
        for (double[] p : PILLARS) if (Math.hypot(lx + 0.5 - p[0], lz + 0.5 - p[1]) <= within) return true;
        return false;
    }

    /** Pillar {@code k}'s radius at height {@code ly}: a rough round three across, flaring where it meets the floor and the vault. */
    static double pillarRadius(int k, int ly, int fromRoof) {
        double foot = Math.max(0, 1 - ly / 4.0), crown = Math.max(0, 1 - fromRoof / 6.0);
        double knot = (hash(k, ly, 0, 61) - 500) / 1000.0 * 0.5;
        return 1.55 + 1.5 * foot * foot + 2.2 * crown * crown + knot;
    }

    /** What the Silk Hall has at (lx, ly, lz) (ly 0 or over), under a vault at {@code roof}. */
    static int hall(int lx, int ly, int lz, int roof) {
        int fromRoof = roof - ly;
        double cx = lx + 0.5, cz = lz + 0.5, cy = ly + 0.5;
        // the pillars, wound about with loose silk
        for (int k = 0; k < PILLARS.length; k++) {
            double d = Math.hypot(cx - PILLARS[k][0], cz - PILLARS[k][1]);
            if (d > 6) continue;
            double r = pillarRadius(k, ly, fromRoof);
            if (d <= r) return SILK;
            if (d <= r + 1 && hash(lx, ly, lz, 62) < (ly < 3 ? 60 : fromRoof < 5 ? 15 : 15)) return COBWEB;
        }
        // the shafts of light, falling from the cracks in the vault: a haze of light in the air, widening as it falls
        for (double[] sh : SHAFTS) {
            double d = Math.hypot(cx - sh[0], cz - sh[1]);
            if (d > 3) continue;
            if (d < shaftRadius(ly, roof)) return BEAM;
            // light round it, so it shines all the way down, and pooled round its foot
            if (d < 2.6 && (ly % 4 == 1 || ly == 0) && hash(lx, ly, lz, 66) < 450) return LIGHT;
        }
        // egg sacs
        for (Sac sac : SACS) {
            double d = Math.sqrt((cx - sac.x) * (cx - sac.x) + (cy - sac.y) * (cy - sac.y) + (cz - sac.z) * (cz - sac.z));
            if (d < sac.r - 0.5) return hash(lx, ly, lz, 73) < 150 ? SHROOM : sac.code;
            if (d < sac.r + 0.25) {
                int h = hash(lx, ly, lz, 67);
                return h < 260 ? COBWEB : h < 420 ? MEMBRANE : AIR; // loosely wrapped in web, the eggs glowing through
            }
            // glow lichen creeping over the floor round a clutch heaped there
            if (ly == 0 && sac.y < 3 && Math.hypot(cx - sac.x, cz - sac.z) < sac.r + 2.2 && hash(lx, 0, lz, 74) < 420) return LICHEN_DOWN;
        }
        for (int[] st : SAC_STRANDS) if (lx == st[0] && lz == st[1] && ly >= st[2] && fromRoof >= 1) return STRAND;
        // the cocooned, hung up to keep
        for (int[] c : COCOONS) {
            int code = cocoon(c, lx, ly, lz, fromRoof);
            if (code != NONE) return code;
        }
        // curtains of silk between the pillars
        for (int[] cu : CURTAINS) {
            int code = curtain(PILLARS[cu[0]], PILLARS[cu[1]], lx, ly, lz, fromRoof);
            if (code != NONE) return code;
        }
        if (lz == WEB_Z && Math.abs(lx) <= WEB_HALF && ly >= WEB_LOW && ly <= Math.min(WEB_HIGH, roof - 1) && greatWeb(lx, ly)) return COBWEB;
        // the vault: webbed, hung with roots, glowing here and there with lichen and old eggs
        if (fromRoof == 1) {
            int h = hash(lx, ly, lz, 3);
            if (h < 8) return GLOW;
            if (toShaft(lx, lz) < 4.5 && h < 500) return LICHEN_UP; // lichen thick round the cracks, in the light
            if (h < 70) return LICHEN_UP;
            if (h < 100) return ROOTS;
        }
        if (fromRoof <= 2 && hash(lx, ly, lz, 4) < 15 && toShaft(lx, lz) > 5) return COBWEB;
        if (fromRoof <= 6 && fromRoof >= 3 && hash(lx, 0, lz, 5) < 3 && hash(lx, ly, lz, 68) < 600) return COBWEB; // wisps hanging lower
        if (ly == 0) return floorLitter(lx, lz, roof);
        return AIR;
    }

    /** What lies on the hall's floor at (lx, lz): by what the floor is there. */
    static int floorLitter(int lx, int lz, int roof) {
        int floor = hallFloor(lx, lz);
        int h = hash(lx, 0, lz, 7);
        if (roof <= 4 && hash(lx, 0, lz, 6) < 200) return COBWEB;
        switch (floor) {
            case WATER: return AIR;
            case MOSS:
                if (toShaft(lx, lz) < 4.5) return h < 450 ? MOSS_CARPET : h < 650 ? GRASS : AIR;
                return h < 380 ? MOSS_CARPET : h < 430 ? LICHEN_DOWN : AIR;
            case BONE_FLOOR: return h < 140 ? SKULL : h < 175 ? BONE_LYING : h < 260 ? COBWEB : AIR;
            case SILK_FLOOR: return h < 160 ? COBWEB : h < 190 ? SKULL : AIR;
            case MUD: return h < 80 ? MUSHROOM : AIR;
            default:
                if (h < 12) return SKULL;
                if (h < 16) return BONE_LYING;
                if (h < 40) return LICHEN_DOWN;
                if (h < 52) return MUSHROOM;
                return AIR;
        }
    }

    /** A cocoon's girth, row by row from its foot up: a spindle (a great bundle fatter). */
    private static final double[] SPINDLE = {0, 1, 1, 1, 0}, GREAT_SPINDLE = {0.8, 1.6, 1.6, 1.6, 1.6, 0.8};

    /**
     * Cocoon {@code c} at (lx, ly, lz): a victim bound upright in silk, hung from the vault by a
     * strand; NONE where it is not. A spindle of silk, a wisp trailing under it, fuzzed with web
     * round its girth; over it a narrow neck and the head (a skull looking out, or wrapped whole).
     */
    static int cocoon(int[] c, int lx, int ly, int lz, int fromRoof) {
        int x = c[0], z = c[1], bottom = c[2], kind = c[3];
        int dx = lx - x, dz = lz - z;
        if (Math.abs(dx) > 3 || Math.abs(dz) > 3 || ly < bottom - 1) return NONE;
        boolean great = kind == 2;
        double[] girth = great ? GREAT_SPINDLE : SPINDLE;
        double ox = great ? x + 1.0 : x + 0.5, oz = great ? z + 1.0 : z + 0.5;
        boolean axis = dx == 0 && dz == 0;
        int row = ly - bottom;
        if (row == -1) return axis ? STRAND : NONE; // a trailing wisp
        if (row < girth.length) {
            double d = Math.hypot(lx + 0.5 - ox, lz + 0.5 - oz), r = girth[row];
            if (d <= r + 0.01 || r == 0 && axis) return WOOL;
            if (d <= r + (r < 0.5 ? 1.01 : 0.45) && hash(lx, ly, lz, 70) < 420) return COBWEB; // the silk loose about it
            return NONE;
        }
        if (!axis) return NONE;
        if (row == girth.length) return kind == 1 ? WOOL : STRAND; // the neck
        if (row == girth.length + 1) return kind == 1 ? SKULL : SILK; // the head
        return fromRoof >= 1 ? STRAND : NONE;
    }

    /** A curtain of silk between pillars at a and b: a sagging cord, a torn sheet of web hanging from it, ragged at its foot. */
    static int curtain(double[] a, double[] b, int lx, int ly, int lz, int fromRoof) {
        double dx = b[0] - a[0], dz = b[1] - a[1], len = Math.hypot(dx, dz);
        double qx = lx + 0.5 - a[0], qz = lz + 0.5 - a[1];
        double t = (qx * dx + qz * dz) / (len * len);
        if (t < 0 || t > 1 || Math.abs(qx * dz - qz * dx) / len > 0.6) return NONE;
        double sag = Math.sin(Math.PI * t);
        int top = (int) Math.round(17 - 3.5 * sag);
        if (fromRoof < 1 || ly > top) return NONE;
        if (ly == top) return SILK;
        int col = hash(lx, 0, lz, 71);
        int foot = (int) Math.round(top - 6 - 4 * sag - (col % 5));
        if (ly < Math.max(4, foot)) return NONE;
        if (col % 4 == 0) return STRAND; // a long thread run down it
        return hash(lx, ly, lz, 72) < 740 ? NONE : COBWEB; // torn, hanging in rags
    }

    /** The hall's pillars: whether (lx, lz) is in one at the floor (for the ways across it). */
    static int hallPillar(int lx, int lz) {
        for (int k = 0; k < PILLARS.length; k++) if (Math.hypot(lx + 0.5 - PILLARS[k][0], lz + 0.5 - PILLARS[k][1]) <= pillarRadius(k, 0, 99)) return 1;
        return 0;
    }

    /** The great web: twelve spokes from its hub and rings out round it. */
    static boolean greatWeb(int lx, int ly) {
        double hx = 0, hy = 12;
        double dx = lx - hx, dy = ly - hy, r = Math.hypot(dx, dy);
        if (r > 12.5) return false;
        if (r < 1) return true;
        double a = Math.atan2(dy, dx);
        double spoke = Math.abs(Math.IEEEremainder(a, Math.PI / 6)) * r;
        if (spoke < 0.55) return true;
        return Math.abs(Math.IEEEremainder(r, 3)) < 0.45;
    }

    /** Silk drapes hanging from a bower's vault at eight points round it. */
    static boolean drape(Chamber c, int lx, int lz) {
        double a = Math.atan2((lz + 0.5 - c.z()) / (double) c.rz(), (lx + 0.5 - c.x()) / (double) c.rx());
        return Math.abs(Math.IEEEremainder(a, Math.PI / 4)) < 0.12;
    }

    // ── Who keeps it ────────────────────────────────────────────────────

    public static final int SPIDER = 0, STINGER = 1, MANTIS = 2, COCKROACH = 3, SISTER = 4;

    /** A post: who stands there (a kind above), at local (x, z) on the floor, captain or not, its level; the bower it is in (-1 the hall). */
    public record Post(int kind, int x, int z, boolean captain, int level, int bower) {}

    public static final List<Post> POSTS;

    static {
        List<Post> p = new ArrayList<>();
        // the hall's garrison, round its floor
        int[][] hall = {{-14, 12}, {14, 12}, {-22, 2}, {22, 2}, {0, 16}, {-8, -2}, {8, -2}, {-18, -8}, {18, -8}, {0, 6}};
        int[] kinds = {SPIDER, SPIDER, MANTIS, MANTIS, COCKROACH, SPIDER, SPIDER, STINGER, STINGER, SPIDER};
        for (int i = 0; i < hall.length; i++) p.add(new Post(kinds[i], HALL_X + hall[i][0], HALL_Z + hall[i][1], i == 0 || i == 4, i == 0 || i == 4 ? 85 : 79, -1));
        // each bower: its sister at its heart, her sons round her
        for (int b = 0; b < BOWERS.size(); b++) {
            Chamber c = BOWERS.get(b);
            p.add(new Post(SISTER, c.x(), c.z(), true, sisterLevel(b), b));
            int sons = 2 + b / 2;
            for (int s = 0; s < sons; s++) {
                double a = s * Math.PI * 2 / sons + b;
                int kind = new int[]{STINGER, MANTIS, SPIDER, COCKROACH}[(b + s) % 4];
                boolean captain = b >= 3 && s == 0;
                p.add(new Post(kind, c.x() + (int) Math.round(Math.cos(a) * c.rx() * 0.55), c.z() + (int) Math.round(Math.sin(a) * c.rz() * 0.55),
                        captain, captain ? 86 : 80 + b / 2, b));
            }
        }
        POSTS = List.copyOf(p);
    }

    /** The level of bower {@code b}'s sister: 82 for the youngest, in the first, to 88 for the eldest, in the last. */
    public static int sisterLevel(int b) {
        return 82 + b;
    }

    /** Which sister keeps bower {@code b} (0 the eldest): the youngest the first, the eldest the last. */
    public static int sisterOf(int b) {
        return BOWERS.size() - 1 - b;
    }
}
