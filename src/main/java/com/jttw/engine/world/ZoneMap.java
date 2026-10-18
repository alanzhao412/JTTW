package com.jttw.engine.world;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Where a set of overworld zones lie together, from the seed alone. Each zone rolls its regions
 * by its own layout; then, so no ground is ever claimed twice, a region of a later zone that
 * would come within reach of a region of an earlier zone sharing any of its heights is given up
 * whole (never cut into). Zones that share no heights (one underground, one aerial) may lie in
 * the same columns. The first zone keeps every region its layout rolls.
 */
public final class ZoneMap {

    private final List<Zones.Zone> zones;

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

    /** How far into one of the zone's regions (x, z) lies: 0 at the edge or outside, 1 at the centre. */
    public double depth(Zones.Zone zone, long seed, int x, int z) {
        if (!zone.hasFootprint()) return 0;
        ZoneLayout l = zone.layout();
        double d = l.depth(seed, x, z);
        if (d <= 0) return 0;
        return yields(zone, seed, Math.floorDiv(x, l.cellSize()), Math.floorDiv(z, l.cellSize())) ? 0 : d;
    }

    /** The centre of the zone's region in the cell holding (x, z), as {x, z}; null if it holds none. */
    @Nullable
    public int[] centre(Zones.Zone zone, long seed, int x, int z) {
        if (!zone.hasFootprint()) return null;
        ZoneLayout l = zone.layout();
        int cx = Math.floorDiv(x, l.cellSize()), cz = Math.floorDiv(z, l.cellSize());
        int[] c = l.centreOfCell(seed, cx, cz);
        return c == null || yields(zone, seed, cx, cz) ? null : c;
    }

    /** The zone whose region covers block (x, y, z), before any host is checked; null if none does. */
    @Nullable
    public Zones.Zone zoneAt(long seed, int x, int y, int z) {
        for (Zones.Zone zone : zones) {
            if (zone.vertical().contains(y) && depth(zone, seed, x, z) > 0) return zone;
        }
        return null;
    }

    /** Whether the zone gives up its region in cell (cx, cz) to an earlier zone's sharing its heights. */
    private boolean yields(Zones.Zone zone, long seed, int cx, int cz) {
        int index = zones.indexOf(zone);
        if (index <= 0) return false; // the first, or not one of these: it keeps all it rolls
        ZoneLayout l = zone.layout();
        int[] c = l.centreOfCell(seed, cx, cz);
        if (c == null) return false;
        for (int i = 0; i < index; i++) {
            Zones.Zone earlier = zones.get(i);
            if (!earlier.vertical().overlaps(zone.vertical())) continue;
            ZoneLayout e = earlier.layout();
            int reach = l.margin() + e.margin();
            // the earlier zone's cells within reach of this region
            for (int ex = Math.floorDiv(c[0] - reach, e.cellSize()); ex <= Math.floorDiv(c[0] + reach, e.cellSize()); ex++) {
                for (int ez = Math.floorDiv(c[1] - reach, e.cellSize()); ez <= Math.floorDiv(c[1] + reach, e.cellSize()); ez++) {
                    int[] o = e.centreOfCell(seed, ex, ez);
                    if (o == null || Math.hypot(o[0] - c[0], o[1] - c[1]) >= reach) continue;
                    if (!yields(earlier, seed, ex, ez)) return true; // a region that stands
                }
            }
        }
        return false;
    }
}
