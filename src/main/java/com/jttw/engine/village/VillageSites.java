package com.jttw.engine.village;

import com.jttw.engine.world.Zones;

/**
 * Where a Tang village may stand: never in a zone's region (the demons' country, Fengdu's, Mount
 * Kunlun's), nor so near one that its fields would reach in; at any height (an underground or aerial
 * zone's columns are kept clear too). Read from the zones' placement through their public API alone
 * ({@link Zones#MAP}), so a zone added or moved is kept clear of without a word here.
 */
public final class VillageSites {

    private VillageSites() {}

    /** The farthest a village reaches from its centre (its footprint's corner), and the gap kept beyond. */
    public static final int REACH = 60, GAP = 48;

    /** Whether a village centred at (x, z) in a world of this seed keeps clear of every zone's region. */
    public static boolean clearOfZones(long seed, int x, int z) {
        for (Zones.Zone zone : Zones.MAP.zones()) {
            if (zone.distanceToCentre(seed, x, z) < zone.layout().margin() + REACH + GAP) return false;
        }
        return true;
    }
}
