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
            return FLOOR;
        }
        if (ly == roof) return NONE; // the rock over it stays
        for (int i = 1; i < TUNNELS.size(); i++) if (inSeal(i, lx, lz) && ly < tunnelHeight(i, lx, lz)) return SEAL;
        int hall = HALL.heightAt(lx + 0.5, lz + 0.5);
        int b = bowerAt(lx, lz);
        int fromRoof = roof - ly;
        // the hall
        if (hall >= 0 && b < 0) {
            int pillar = hallPillar(lx, lz);
            if (pillar > 0) return SILK;
            if (lz == WEB_Z && Math.abs(lx) <= WEB_HALF && ly >= WEB_LOW && ly <= Math.min(WEB_HIGH, hall - 1) && greatWeb(lx, ly)) return COBWEB;
            if (fromRoof == 1 && hash(lx, ly, lz, 3) < 30) return GLOW;
            if (fromRoof <= 2 && hash(lx, ly, lz, 4) < 140) return COBWEB;
            if (hall > 10 && hash(lx, 0, lz, 5) < 12 && fromRoof >= 1 && fromRoof <= 3) return SILK; // a cocoon, hanging
            if (ly == 0 && hall <= 4 && hash(lx, ly, lz, 6) < 200) return COBWEB;
            if (ly == 0 && hash(lx, ly, lz, 7) < 14) return BONES;
            return AIR;
        }
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

    /** The hall's silk pillars: ten in a ring, each a rough round three across. Returns 1 where one stands. */
    static int hallPillar(int lx, int lz) {
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI / 5 + 0.3;
            double px = HALL_X + Math.cos(a) * HALL_RX * 0.58, pz = HALL_Z + Math.sin(a) * HALL_RZ * 0.58;
            if (Math.hypot(lx + 0.5 - px, lz + 0.5 - pz) <= 1.6) return 1;
        }
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
