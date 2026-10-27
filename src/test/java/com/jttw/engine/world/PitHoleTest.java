package com.jttw.engine.world;

import com.jttw.engine.world.feature.Labyrinth;
import com.jttw.engine.world.feature.PitHole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pit-Hole Mountain over the cave: it covers the fortress, a road winds up it to the door, outposts about it. */
class PitHoleTest {

    private static final long[] SEEDS = {1L, 42L, 987654321L, -7L, 12345L};

    @Test
    void theMountainStandsOverEveryFloorThatRisesAboveTheLand() {
        for (long seed : SEEDS) {
            Labyrinth lab = Labyrinth.of(seed, 0, 0, Labyrinth.Land.flat(70));
            PitHole pit = new PitHole(lab, Labyrinth.Land.flat(70));
            for (int k = 0; k < Labyrinth.FLOORS; k++) {
                int top = Labyrinth.top(k);
                if (top < 70) continue;
                int reach = Labyrinth.REACH[k] + 2 * Labyrinth.PITCH;
                for (int a = 0; a < 72; a++) {
                    double t = a * Math.PI / 36;
                    for (int r = 0; r <= reach; r += 4) {
                        int x = (int) Math.round(Math.cos(t) * r), z = (int) Math.round(Math.sin(t) * r);
                        if (pit.forecourt(x, z)) continue;
                        assertTrue(pit.massif(x, z) >= top + 3, "seed " + seed + ": floor " + (k + 1) + " under the mountain at " + x + "," + z);
                    }
                }
            }
            // its summit sunk in a hollow, as its name says
            assertTrue(pit.massif(0, 0) < pit.massif(PitHole.SHOULDER, 0) - 8, "a hollow at its top");
            assertTrue(pit.massif(0, 0) + 30 < 300, "under the sky");
        }
    }

    @Test
    void theDoorIsInACliffOverAForecourtCutLevel() {
        for (long seed : SEEDS) {
            Labyrinth lab = Labyrinth.of(seed, 100, -300, Labyrinth.Land.flat(80));
            PitHole pit = new PitHole(lab, Labyrinth.Land.flat(80));
            int[] d = Labyrinth.DIRS[lab.doorDir];
            int fx = lab.cx + d[0] * (Labyrinth.DOOR_AT + 4), fz = lab.cz + d[1] * (Labyrinth.DOOR_AT + 4);
            assertTrue(pit.forecourt(fx, fz), "the forecourt before the door");
            int bx = lab.cx + d[0] * (Labyrinth.DOOR_AT - 2), bz = lab.cz + d[1] * (Labyrinth.DOOR_AT - 2);
            assertTrue(pit.massif(bx, bz) >= lab.doorY + PitHole.BROW, "the rock stands over the door");
        }
    }

    @Test
    void aRoadWindsDownFromTheForecourtToTheLandAndOn() {
        for (long seed : SEEDS) {
            Labyrinth lab = Labyrinth.of(seed, -500, 900, Labyrinth.Land.flat(75));
            PitHole pit = new PitHole(lab, Labyrinth.Land.flat(75));
            assertTrue(pit.mountainLength() > 40, "a long climb: " + pit.mountainLength());
            int[] first = pit.road(0);
            assertEquals(lab.doorY - 1, first[1], "it starts at the forecourt's height");
            int last = first[1];
            for (int n = 1; n < pit.mountainLength(); n++) {
                int[] p = pit.road(n), q = pit.road(n - 1);
                assertTrue(p[1] <= last, "it only goes down");
                assertTrue(last - p[1] <= 1, "never more than a block at a step");
                assertTrue(Math.abs(p[0] - q[0]) + Math.abs(p[2] - q[2]) <= 2, "unbroken");
                last = p[1];
            }
            assertTrue(last <= 77, "down to the land: " + last);
            assertEquals(Integer.MIN_VALUE, pit.road(pit.roadLength() - 1)[1], "and on across it");
            assertNotNull(pit.roadAt(first[0], first[2]), "found where it runs");
        }
    }

    @Test
    void outpostsWatchTheRoadAndTheSlopes() {
        for (long seed : SEEDS) {
            Labyrinth lab = Labyrinth.of(seed, 0, 0, Labyrinth.Land.flat(75));
            PitHole pit = new PitHole(lab, Labyrinth.Land.flat(75));
            int towers = 0, nooks = 0, steles = 0;
            for (PitHole.Outpost o : pit.outposts()) {
                if (o.kind() == PitHole.TOWER) towers++;
                if (o.kind() == PitHole.NOOK) nooks++;
                if (o.kind() == PitHole.STELE) steles++;
                assertTrue(Math.hypot(o.x(), o.z()) > Labyrinth.DOOR_AT + 10, "out on the slopes");
            }
            assertTrue(pit.outposts().size() >= 8, "seed " + seed + ": outposts " + pit.outposts().size());
            assertTrue(towers >= 2 && nooks >= 1, "watchtowers and guard nooks");
            assertEquals(1, steles, "one stele where the road comes down");
        }
    }
}
