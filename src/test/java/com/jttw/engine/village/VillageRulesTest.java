package com.jttw.engine.village;

import com.jttw.engine.world.Zones;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Where villages may stand, what the talismans do to a blow, and what a rumour scroll tells. */
class VillageRulesTest {

    private static final long SEED = 12345L;

    @Test
    void noVillageStandsInOrBesideAZone() {
        for (Zones.Zone zone : Zones.MAP.zones()) {
            for (int[] c : zone.centres(SEED)) {
                assertFalse(VillageSites.clearOfZones(SEED, c[0], c[1]), "a village at the heart of " + zone.id());
                int edge = zone.layout().margin();
                assertFalse(VillageSites.clearOfZones(SEED, c[0] + edge, c[1]), "a village at the edge of " + zone.id());
                assertFalse(VillageSites.clearOfZones(SEED, c[0] + edge + VillageSites.REACH, c[1]), "its fields reaching into " + zone.id());
            }
        }
        assertTrue(VillageSites.clearOfZones(SEED, 300, 200), "near spawn, where no zone lies, a village may stand");
    }

    @Test
    void talismansChangeOnlyTheBlowsOfTheUnclean() {
        assertEquals(1.25f, Talismans.scale(true, true, false, false, false), 1e-6, "a quelling talisman, on a demon");
        assertEquals(1f, Talismans.scale(true, false, false, false, false), 1e-6, "on a mortal, nothing");
        assertEquals(0.75f, Talismans.scale(false, false, true, true, false), 1e-6, "warded against a ghost's blow");
        assertEquals(1f, Talismans.scale(false, false, false, true, true), 1e-6, "a mortal's blow is not warded");
        assertEquals(0.85f, Talismans.scale(false, false, true, false, true), 1e-6, "a charm carried");
        assertEquals(0.75f * 0.85f, Talismans.scale(false, false, true, true, true), 1e-6, "ward and charm together");
    }

    @Test
    void aRumourTellsTheRoadAhead() {
        List<String> early = Rumours.lines(5, 0, 0, SEED, true);
        assertTrue(early.get(1).contains("White Bone Ridge") && early.get(1).contains("Lady White Bone"), early.get(1));
        assertTrue(early.get(2).contains("Bottomless Cave"), "and the trial after it: " + early.get(2));
        assertTrue(early.get(1).contains("blocks to the"), "which way, how far");
        List<String> hell = Rumours.lines(60, 0, 0, SEED, true);
        assertTrue(hell.get(1).contains("Diyu") && hell.get(1).contains("Fengdu"), "the way down to Hell is through Fengdu: " + hell.get(1));
        List<String> last = Rumours.lines(100, 0, 0, SEED, true);
        assertTrue(last.get(1).contains("Lion-Camel Ridge") && last.get(2).contains("no traveller"), "nothing beyond the last");
        assertEquals(Zones.FIRE_MOUNTAIN, Rumours.next(Zones.HELL));
    }
}
