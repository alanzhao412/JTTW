package com.jttw.engine.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where a set of overworld zones lie together. Each zone has one region a world (the Four Seas one
 * in each quarter), in its layout's ring round the origin; earliest first, each takes the first of
 * the places its layout offers that is on ground of its hosts ({@link Hosts}) and clear of every
 * region already placed at its heights, by both their reaches and a little more: no ground is ever
 * claimed twice. Zones that share no heights (one underground, one aerial) may lie in the same
 * columns. Where no place in the ring will do (host ground round the centre, else under it at
 * least), the search widens outward, 500 blocks at a time.
 *
 * <p>The world's placement is found once, with its biome source, when the overworld loads (or the
 * first time the biome source places a zone), and kept ({@link #settle}). Until then, and for
 * another seed (a unit test's), a placement that takes any ground for host is worked out instead.
 */
public final class ZoneMap {

    /** Whether ground at (x, z) is of a zone's hosts, judged at its host height. */
    @FunctionalInterface
    public interface Hosts {
        boolean at(Zones.Zone zone, int x, int z);

        /** Any ground at all: where no biome source is to hand. */
        Hosts ANY = (zone, x, z) -> true;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("jttw_engine/zones");

    /** Places tried in each ring before it widens. */
    static final int TRIES = 400;
    /** Rings tried beyond a layout's own, each {@link #WIDEN_STEP} blocks wide, before a region is put down off its hosts. */
    static final int WIDENINGS = 16;
    static final int WIDEN_STEP = 500;
    /** Blocks kept clear between two regions at the same heights, beyond both their reaches. */
    public static final int GAP = 48;
    /** Of the eight points round a centre, at {@link #RING_SHARE} of the radius, how many must be host ground too. */
    static final int RING_HOSTS = 6;
    static final double RING_SHARE = 0.6;

    /**
     * Where a world's zones lie: each zone's regions' centres, as {x, z} (for the seas, indexed by
     * quarter); and how it was found.
     */
    public record Placement(long seed, Map<Zones.Zone, int[][]> centres, String notes) {

        /** The zone's regions' centres; none for a zone not placed. */
        public int[][] of(Zones.Zone zone) {
            int[][] cs = centres.get(zone);
            if (cs != null) return cs;
            for (Map.Entry<Zones.Zone, int[][]> e : centres.entrySet()) if (e.getKey().equals(zone)) return e.getValue();
            return new int[0][];
        }
    }

    private final List<Zones.Zone> zones;
    /** The world's placement, found with its biome source. */
    private volatile Placement settled;
    /** Placements taking any ground for host, by seed. */
    private final Map<Long, Placement> bare = new ConcurrentHashMap<>();

    /** The overworld ones of these zones, earliest first. Their biomes and salts must differ. */
    public ZoneMap(List<Zones.Zone> all) {
        List<Zones.Zone> found = new ArrayList<>();
        Set<Long> salts = new HashSet<>();
        Set<Object> biomes = new HashSet<>();
        for (Zones.Zone zone : all) {
            if (!biomes.add(zone.biome())) throw new IllegalArgumentException("two zones of " + zone.biome().location());
            if (!zone.hasFootprint()) continue;
            if (!salts.add(zone.layout().salt())) throw new IllegalArgumentException("zone salt used twice: " + zone.biome().location());
            found.add(zone);
        }
        zones = List.copyOf(found);
    }

    /** The overworld zones, earliest first. */
    public List<Zones.Zone> zones() {
        return zones;
    }

    /** Where these zones lie in a world of this seed, judging host ground by {@code hosts}. Deterministic; it takes a while. */
    public Placement place(long seed, Hosts hosts) {
        Map<Zones.Zone, int[][]> out = new IdentityHashMap<>();
        List<Zones.Zone> placedZones = new ArrayList<>();
        List<int[]> placedCentres = new ArrayList<>();
        StringBuilder notes = new StringBuilder();
        for (Zones.Zone zone : zones) {
            ZoneLayout l = zone.layout();
            int[][] cs = new int[l.regions()][];
            for (int region = 0; region < cs.length; region++) {
                int[] c = find(seed, zone, region, hosts, placedZones, placedCentres, notes);
                cs[region] = c;
                placedZones.add(zone);
                placedCentres.add(c);
            }
            out.put(zone, cs);
        }
        return new Placement(seed, out, notes.toString());
    }

    /**
     * The first place for one of the zone's regions on its hosts and clear of those placed: in its
     * ring, with host ground round it, else at least under it; else so in each wider ring in turn;
     * failing all, the first clear place in its ring.
     */
    private static int[] find(long seed, Zones.Zone zone, int region, Hosts hosts, List<Zones.Zone> placedZones,
                              List<int[]> placedCentres, StringBuilder notes) {
        ZoneLayout l = zone.layout();
        int[] firstClear = null;
        for (int ring = 0; ring <= WIDENINGS; ring++) {
            int from = ring == 0 ? l.near() : l.far() + (ring - 1) * WIDEN_STEP, to = ring == 0 ? l.far() : from + WIDEN_STEP;
            for (boolean round : new boolean[]{true, false}) {
                for (int i = 0; i < TRIES; i++) {
                    int[] c = l.candidate(seed, region, i, from, to);
                    if (!clear(zone, c, placedZones, placedCentres)) continue;
                    if (firstClear == null) firstClear = c;
                    if (!hosted(zone, c, hosts, round)) continue;
                    if (ring > 0 || !round) {
                        String note = String.format("%s%s: %s %d-%d blocks out", zone.id(), l.perSea() ? " (quarter " + region + ")" : "",
                                ring == 0 ? "host ground under its centre only," : "no host ground " + l.near() + "-" + l.far()
                                        + " blocks out; found it" + (round ? "" : " (under its centre only)"), from, to);
                        LOGGER.warn("Zones: {}", note);
                        notes.append(note).append("; ");
                    }
                    return c;
                }
            }
        }
        if (firstClear == null) throw new IllegalStateException("no room for " + zone.id());
        String note = String.format("%s%s: no host ground within %d blocks; put down off its hosts", zone.id(),
                l.perSea() ? " (quarter " + region + ")" : "", l.far() + WIDENINGS * WIDEN_STEP);
        LOGGER.warn("Zones: {}", note);
        notes.append(note).append("; ");
        return firstClear;
    }

    /** Whether a region of the zone centred at {@code c} keeps clear of every one placed at heights it shares. */
    private static boolean clear(Zones.Zone zone, int[] c, List<Zones.Zone> placedZones, List<int[]> placedCentres) {
        for (int k = 0; k < placedZones.size(); k++) {
            Zones.Zone other = placedZones.get(k);
            if (!other.vertical().overlaps(zone.vertical())) continue;
            int[] o = placedCentres.get(k);
            if (Math.hypot(o[0] - c[0], o[1] - c[1]) < zone.layout().margin() + other.layout().margin() + GAP) return false;
        }
        return true;
    }

    /** Whether a region centred at {@code c} would stand on host ground: at its centre, and ({@code round}) mostly round it. */
    private static boolean hosted(Zones.Zone zone, int[] c, Hosts hosts, boolean round) {
        if (!hosts.at(zone, c[0], c[1])) return false;
        if (!round) return true;
        double r = zone.layout().radius() * RING_SHARE;
        int misses = 0;
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            if (!hosts.at(zone, c[0] + (int) Math.round(Math.cos(a) * r), c[1] + (int) Math.round(Math.sin(a) * r))) {
                if (++misses > 8 - RING_HOSTS) return false;
            }
        }
        return true;
    }

    /**
     * Finds the world's placement with its biome source (judging host ground by {@code hosts}) and
     * keeps it; again only if {@code force} or the kept one is another seed's. Returns it.
     */
    public synchronized Placement settle(long seed, Hosts hosts, boolean force) {
        Placement p = settled;
        if (!force && p != null && p.seed() == seed) return p;
        p = place(seed, hosts);
        settled = p;
        return p;
    }

    /** Whether the world's placement for this seed has been found with its biome source. */
    public boolean settled(long seed) {
        Placement p = settled;
        return p != null && p.seed() == seed;
    }

    /** The placement for this seed: the world's, if it is this seed's; else one taking any ground for host. */
    public Placement placement(long seed) {
        Placement p = settled;
        if (p != null && p.seed() == seed) return p;
        return bare.computeIfAbsent(seed, s -> place(s, Hosts.ANY));
    }

    /** The centres of the zone's regions, as {x, z}: one, or four for the seas (east, south, west, north); none for a dimension's zone. */
    public int[][] centres(Zones.Zone zone, long seed) {
        if (!zone.hasFootprint()) return new int[0][];
        return placement(seed).of(zone);
    }

    /** How far into one of the zone's regions (x, z) lies: 0 at the edge or outside, 1 at the centre. */
    public double depth(Zones.Zone zone, long seed, int x, int z) {
        if (!zone.hasFootprint()) return 0;
        double best = 0;
        for (int[] c : placement(seed).of(zone)) best = Math.max(best, zone.layout().depth(seed, c, x, z));
        return best;
    }

    /** The centre of the zone's region nearest (x, z), as {x, z}; null only for a zone with none (a dimension's). */
    @Nullable
    public int[] centre(Zones.Zone zone, long seed, int x, int z) {
        if (!zone.hasFootprint()) return null;
        int[] nearest = null;
        double best = Double.MAX_VALUE;
        for (int[] c : placement(seed).of(zone)) {
            double d = (double) (x - c[0]) * (x - c[0]) + (double) (z - c[1]) * (z - c[1]);
            if (d < best) {
                best = d;
                nearest = c;
            }
        }
        return nearest;
    }

    /** The zone whose region covers block (x, y, z); null if none does. */
    @Nullable
    public Zones.Zone zoneAt(long seed, int x, int y, int z) {
        for (Zones.Zone zone : zones) {
            if (zone.vertical().contains(y) && depth(zone, seed, x, z) > 0) return zone;
        }
        return null;
    }
}
