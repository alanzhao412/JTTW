package com.jttw.engine.world.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Pit-Hole Mountain (陷空山), over the Bottomless Cave: what the land round the cave's door is made
 * into, worked out from the seed, the region's centre and the lie of the land alone (as the
 * {@link Labyrinth} under it is), so every chunk agrees. Pure: no Minecraft in it.
 * <ul>
 *   <li><b>the mountain</b>: a craggy massif raised out of the land round the centre
 *       ({@link #massif}), steep-sided, its summit sunk in a hollow, as its name says; over the
 *       door, a brow of rock;</li>
 *   <li><b>the forecourt</b> before the great door, cut level into the mountainside, open to the
 *       sky, guarded;</li>
 *   <li><b>the road</b> up to it: from the forecourt it winds round the mountain's flank, cut into
 *       the slope as a shelf, lanterns along its outer edge, down to the land below, and on out
 *       across it ({@link #road});</li>
 *   <li><b>outposts</b> along it and about the slopes ({@link #outposts}): watchtowers, burrow
 *       mounds, the mice's camps, guard nooks on the road; and where the road leaves the mountain,
 *       a stele naming it.</li>
 * </ul>
 */
public final class PitHole {

    /** The mountain: its rim's height, the shoulder's radius, how steeply it falls beyond, how deep its crags and its summit's hollow. */
    public static final int PEAK = 200, SHOULDER = 20, HOLLOW = 18;
    public static final double SLOPE = 1.5, CRAGS = 7;
    /** The forecourt: from the door's frame out, and either side of the way in. */
    public static final int FORE_OUT = 14, FORE_HALF = 11;
    /** The rock over the door rises at least this far over its threshold. */
    public static final int BROW = 22;
    /** The road: how much it falls a block, how wide (from its middle), how far it runs on across the land below. */
    public static final double ROAD_FALL = 0.42, ROAD_HALF = 1.6;
    public static final int ROAD_ON = 70;
    /** How far out the outposts about the slopes lie. */
    public static final int OUTPOST_FROM = 112, OUTPOST_TO = 150;

    /** An outpost's kind. */
    public static final int TOWER = 0, MOUND = 1, CAMP = 2, NOOK = 3, STELE = 4;

    /** An outpost: where, the land's height there (or the road's), its kind, which way it faces (0-3, as Labyrinth.DIRS). */
    public record Outpost(int x, int z, int y, int kind, int facing) {}

    public final Labyrinth lab;
    public final long seed;
    public final int cx, cz;
    /** The road, a point a block: x, z, and the height of its surface (Integer.MIN_VALUE where it runs on the land and takes its height). */
    private final int[] roadX, roadZ, roadY;
    private final int roadLength, mountainLength;
    private final Map<Long, int[]> roadByChunk = new HashMap<>();
    private final List<Outpost> outposts = new ArrayList<>();

    public PitHole(Labyrinth lab, Labyrinth.Land land) {
        this.lab = lab;
        this.seed = lab.seed;
        this.cx = lab.cx;
        this.cz = lab.cz;
        Random r = new Random(Labyrinth.mix(seed ^ 0x917_401EL, cx, cz));
        List<int[]> pts = new ArrayList<>();
        // up on the mountain: from the forecourt's outer edge, round the flank and down
        int[] d = Labyrinth.DIRS[lab.doorDir];
        double sx = cx + d[0] * (Labyrinth.DOOR_AT + 2 + FORE_OUT), sz = cz + d[1] * (Labyrinth.DOOR_AT + 2 + FORE_OUT);
        double radius = Math.hypot(sx - cx, sz - cz), theta = Math.atan2(sz - cz, sx - cx);
        int sense = r.nextBoolean() ? 1 : -1;
        double h = lab.doorY - 1;
        int mountain = 0;
        for (int s = 0; s < 900; s++) {
            int x = (int) Math.round(cx + radius * Math.cos(theta)), z = (int) Math.round(cz + radius * Math.sin(theta));
            if (s > 0 && (h <= land.at(x, z) + 1 || radius > 118)) break;
            pts.add(new int[]{x, z, (int) Math.round(h)});
            mountain++;
            h -= ROAD_FALL;
            double want = SHOULDER + (PEAK - (h + 3)) / SLOPE;
            double dr = Math.max(0.04, Math.min(0.55, want - radius));
            radius += dr;
            theta += sense * Math.sqrt(Math.max(0.2, 1 - dr * dr)) / radius;
        }
        mountainLength = mountain;
        // then on out across the land, wandering a little
        double hx = cx + radius * Math.cos(theta), hz = cz + radius * Math.sin(theta), heading = theta + sense * 0.35;
        for (int s = 0; s < ROAD_ON; s++) {
            heading += (r.nextDouble() - 0.5) * 0.12;
            hx += Math.cos(heading);
            hz += Math.sin(heading);
            pts.add(new int[]{(int) Math.round(hx), (int) Math.round(hz), Integer.MIN_VALUE});
        }
        roadLength = pts.size();
        roadX = new int[roadLength];
        roadZ = new int[roadLength];
        roadY = new int[roadLength];
        Map<Long, List<Integer>> byChunk = new HashMap<>();
        for (int n = 0; n < roadLength; n++) {
            int[] p = pts.get(n);
            roadX[n] = p[0];
            roadZ[n] = p[1];
            roadY[n] = p[2];
            for (int ox = -2; ox <= 2; ox += 2) for (int oz = -2; oz <= 2; oz += 2) {
                long key = chunkKey((p[0] + ox) >> 4, (p[1] + oz) >> 4);
                List<Integer> list = byChunk.computeIfAbsent(key, k -> new ArrayList<>());
                if (list.isEmpty() || list.get(list.size() - 1) != n) list.add(n);
            }
        }
        for (Map.Entry<Long, List<Integer>> e : byChunk.entrySet()) {
            roadByChunk.put(e.getKey(), e.getValue().stream().mapToInt(Integer::intValue).toArray());
        }
        // guard nooks along the mountain road, outside its outer edge
        for (int n = 24; n < mountainLength - 6; n += 30) {
            int[] o = outward(n, 4);
            outposts.add(new Outpost(o[0], o[1], roadY[n], NOOK, facingOf(n)));
        }
        // where it comes down, the stele; and watchtowers along the road below
        int foot = Math.min(mountainLength, roadLength - 1);
        int[] st = outward(foot, 3);
        outposts.add(new Outpost(st[0], st[1], land.at(st[0], st[1]), STELE, facingOf(foot)));
        for (int n = mountainLength + 22, side = 1; n < roadLength - 4; n += 28, side = -side) {
            int[] o = outward(n, 7 * side);
            outposts.add(new Outpost(o[0], o[1], land.at(o[0], o[1]), side > 0 ? TOWER : CAMP, facingOf(n)));
        }
        // and about the slopes, all round
        int about = 6;
        double start = r.nextDouble() * Math.PI * 2;
        for (int k = 0; k < about; k++) {
            for (int attempt = 0; attempt < 20; attempt++) {
                double a = start + k * Math.PI * 2 / about + (r.nextDouble() - 0.5) * 0.6;
                double dist = OUTPOST_FROM + r.nextDouble() * (OUTPOST_TO - OUTPOST_FROM);
                int x = (int) Math.round(cx + Math.cos(a) * dist), z = (int) Math.round(cz + Math.sin(a) * dist);
                if (nearRoad(x, z, 12) || nearOutpost(x, z, 24)) continue;
                int kind = k % 3 == 0 ? TOWER : k % 3 == 1 ? MOUND : CAMP;
                outposts.add(new Outpost(x, z, land.at(x, z), kind, Math.floorMod((int) Math.round(-a / (Math.PI / 2)) + 1, 4)));
                break;
            }
        }
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    /** A point {@code by} blocks out from the road's point n, away from the centre. */
    private int[] outward(int n, double by) {
        double dx = roadX[n] - cx, dz = roadZ[n] - cz, len = Math.max(1, Math.hypot(dx, dz));
        return new int[]{(int) Math.round(roadX[n] + dx / len * by), (int) Math.round(roadZ[n] + dz / len * by)};
    }

    /** The way (0-3) from the road's point n toward the centre: the way an outpost by it faces. */
    private int facingOf(int n) {
        double dx = cx - roadX[n], dz = cz - roadZ[n];
        if (Math.abs(dx) >= Math.abs(dz)) return dx >= 0 ? 1 : 3;
        return dz >= 0 ? 2 : 0;
    }

    private boolean nearRoad(int x, int z, double within) {
        for (int n = 0; n < roadLength; n += 2) if (Math.hypot(roadX[n] - x, roadZ[n] - z) < within) return true;
        return false;
    }

    private boolean nearOutpost(int x, int z, double within) {
        for (Outpost o : outposts) if (Math.hypot(o.x() - x, o.z() - z) < within) return true;
        return false;
    }

    // ── The mountain ────────────────────────────────────────────────────

    /**
     * The height the mountain's ground reaches at (x, z) (the top block's): the land there is raised
     * to it if lower. Its profile falls {@value #SLOPE} a block from the rim of its summit's hollow,
     * broken by crags; over the great door it stands at least {@value #BROW} over the threshold.
     */
    public int massif(int x, int z) {
        double r = Math.hypot(x - cx, z - cz);
        double base = r < SHOULDER ? PEAK - HOLLOW * (1 - r / SHOULDER) * (1 - r / SHOULDER) : PEAK - SLOPE * (r - SHOULDER);
        double crag = CRAGS * (2 * ridged(x, z) - 1) * Math.min(1, r / 30.0);
        int m = (int) Math.round(base + crag);
        int[] ac = doorFrame(x, z);
        int a = ac[0], c = Math.abs(ac[1]);
        if (a >= Labyrinth.DOOR_AT - 26 && a <= Labyrinth.DOOR_AT + 1 && c <= FORE_HALF + 8) {
            int brow = lab.doorY + BROW - 2 * Math.max(0, c - FORE_HALF) - Math.max(0, Labyrinth.DOOR_AT - 14 - a) / 2;
            m = Math.max(m, brow);
        }
        // under and round the forecourt, a talus of rock falling away from its edge, so it stands out of
        // the mountainside as a shoulder of it, not a block set on it
        int out = a - (Labyrinth.DOOR_AT + 2);
        if (out >= -2 && out <= FORE_OUT + 40 && c <= FORE_HALF + 40) {
            double past = Math.hypot(Math.max(0, out - FORE_OUT), Math.max(0, c - FORE_HALF));
            m = Math.max(m, lab.doorY - 2 - (int) Math.round(past * 1.25));
        }
        return m;
    }

    /** (x, z) as {along the door's way out from the centre, across it}. */
    public int[] doorFrame(int x, int z) {
        int[] d = Labyrinth.DIRS[lab.doorDir];
        int u = x - cx, v = z - cz;
        return new int[]{u * d[0] + v * d[1], u * d[1] - v * d[0]};
    }

    /** Whether (x, z) is in the forecourt before the door (its rounded outer corners cut). */
    public boolean forecourt(int x, int z) {
        int[] ac = doorFrame(x, z);
        int a = ac[0] - (Labyrinth.DOOR_AT + 2), c = Math.abs(ac[1]);
        if (a < 0 || a > FORE_OUT || c > FORE_HALF) return false;
        int ca = a - (FORE_OUT - 4), cc = c - (FORE_HALF - 4);
        return ca <= 0 || cc <= 0 || ca * ca + cc * cc <= 16;
    }

    /** The crags: ridged value noise, 0..1, peaked along its ridges. */
    private double ridged(int x, int z) {
        double n1 = value(x, z, 13), n2 = value(x + 57, z - 31, 6);
        double a = 1 - Math.abs(2 * n1 - 1), b = 1 - Math.abs(2 * n2 - 1);
        return 0.7 * a * a + 0.3 * b;
    }

    private double value(int x, int z, int scale) {
        int x0 = Math.floorDiv(x, scale), z0 = Math.floorDiv(z, scale);
        double tx = (x - x0 * scale) / (double) scale, tz = (z - z0 * scale) / (double) scale;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double v00 = lattice(x0, z0), v10 = lattice(x0 + 1, z0), v01 = lattice(x0, z0 + 1), v11 = lattice(x0 + 1, z0 + 1);
        return (v00 + (v10 - v00) * tx) + ((v01 + (v11 - v01) * tx) - (v00 + (v10 - v00) * tx)) * tz;
    }

    private double lattice(int i, int j) {
        return (Labyrinth.mix(seed ^ 0xC4A6L, i, j) >>> 11) * 0x1.0p-53;
    }

    // ── The road ────────────────────────────────────────────────────────

    /**
     * The road at (x, z), if it passes there: {its surface's height (Integer.MIN_VALUE where it runs on
     * the land), how far from its middle (in tenths of a block), the point's index}; else null.
     */
    public int[] roadAt(int x, int z) {
        int[] near = roadByChunk.get(chunkKey(x >> 4, z >> 4));
        if (near == null) return null;
        double best = ROAD_HALF + 0.01;
        int bi = -1;
        for (int n : near) {
            double dd = Math.hypot(roadX[n] - x, roadZ[n] - z);
            if (dd < best) {
                best = dd;
                bi = n;
            }
        }
        if (bi < 0) return null;
        return new int[]{roadY[bi], (int) Math.round(best * 10), bi};
    }

    /** Whether the road's point n is up on the mountain (cut into it), not on the land below. */
    public boolean onMountain(int n) {
        return n < mountainLength;
    }

    /** The road's point n, as {x, height (or Integer.MIN_VALUE), z}. */
    public int[] road(int n) {
        return new int[]{roadX[n], roadY[n], roadZ[n]};
    }

    public int roadLength() {
        return roadLength;
    }

    /** How long the road runs up on the mountain. */
    public int mountainLength() {
        return mountainLength;
    }

    /** Whether a lantern post stands by the road's point n, on its outer edge. */
    public boolean lanternPost(int n) {
        return n % 9 == 4 && n < roadLength - 2;
    }

    /** Where the road's lantern post n stands, as {x, z}. */
    public int[] lanternAt(int n) {
        return outward(n, 2);
    }

    // ── The outposts ────────────────────────────────────────────────────

    public List<Outpost> outposts() {
        return outposts;
    }

    /** The outpost whose ground (x, z) is in, or null. */
    public Outpost outpostAt(int x, int z) {
        for (Outpost o : outposts) {
            int reach = o.kind() == MOUND ? 6 : o.kind() == STELE ? 2 : 4;
            if (Math.abs(x - o.x()) <= reach && Math.abs(z - o.z()) <= reach) return o;
        }
        return null;
    }
}
