package com.jttw.engine.world;

import com.jttw.engine.world.fengdu.GateRules;
import com.jttw.engine.world.fengdu.GateRules.Trip;
import com.jttw.engine.world.fengdu.GateRules.WaveKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The rules of Fengdu's Gate of Ghosts: who may pass (only those who broke its guard, or came back
 * up from Diyu; every trip from the Ancient City fought again; leave lost on leaving), when it
 * burns and when it seals, and the swarm (its waves, how it grows with the pilgrims, when the next
 * rises, when it is beaten or given up, the level its soldiers rise at).
 */
class GateRulesTest {

    @Test
    void onlyThoseWhoBrokeTheGuardOrCameUpFromDiyuMayPass() {
        assertFalse(Trip.NONE.mayPass(), "a traveller over the land");
        assertFalse(Trip.FIGHTING.mayPass(), "a pilgrim whose swarm still stands");
        assertTrue(Trip.ADMITTED.mayPass());
        for (Trip before : Trip.values()) {
            assertEquals(Trip.FIGHTING, GateRules.arriveByCity(before), "by the city's frame, every time, fighting: from " + before);
            assertEquals(Trip.ADMITTED, GateRules.arriveFromHell(before), "up from Diyu, free to go down again: from " + before);
        }
        assertEquals(Trip.ADMITTED, GateRules.swarmBroken(Trip.FIGHTING), "the guard broken, the pilgrim may pass");
        assertEquals(Trip.NONE, GateRules.swarmBroken(Trip.NONE), "but not one who walked there and watched");
        assertEquals(Trip.ADMITTED, GateRules.swarmBroken(Trip.ADMITTED));
    }

    @Test
    void leaveIsLostOnLeavingFengdu() {
        assertFalse(GateRules.lapses(true, 3));
        assertFalse(GateRules.lapses(true, GateRules.RANGE));
        assertTrue(GateRules.lapses(true, GateRules.RANGE + 1), "away from the gate");
        assertTrue(GateRules.lapses(false, 0), "another world (Diyu, or anywhere)");
    }

    @Test
    void theGateBurnsWhileOneWhoMayPassIsNearAndSealsAfter() {
        long t = 10_000;
        assertFalse(GateRules.burns(false, false, t, 0), "sealed: no one has ever been let through");
        assertTrue(GateRules.burns(false, true, t, t), "one who may pass is here");
        assertTrue(GateRules.burns(false, true, t + GateRules.SEAL_AFTER, t), "a little after they went");
        assertFalse(GateRules.burns(false, true, t + GateRules.SEAL_AFTER + 1, t), "sealed again once none has been near for a while");
        assertFalse(GateRules.burns(true, true, t, t), "never while a swarm stands");
        assertFalse(GateRules.abandoned(t + GateRules.ABANDON_AFTER, t));
        assertTrue(GateRules.abandoned(t + GateRules.ABANDON_AFTER + 1, t), "no one near for long: the swarm given up");
        assertTrue(GateRules.SEAL_AFTER < GateRules.ABANDON_AFTER);
    }

    @Test
    void theSwarmComesInThreeWavesAndGrowsWithThePilgrims() {
        assertEquals(java.util.List.of(WaveKind.VANGUARD, WaveKind.MAIN, WaveKind.GUARD), GateRules.SWARM);
        var vanguard = GateRules.wave(WaveKind.VANGUARD, 1);
        var main = GateRules.wave(WaveKind.MAIN, 1);
        var guard = GateRules.wave(WaveKind.GUARD, 1);
        assertEquals(5, vanguard.soldiers());
        assertEquals(0, vanguard.captains());
        assertFalse(vanguard.guards());
        assertEquals(1, main.captains(), "a captain with the main body");
        assertTrue(guard.guards(), "Ox-Head and Horse-Face last");
        assertEquals(guard.soldiers() + 2, guard.size());
        assertEquals(16, GateRules.swarmSize(1), "sixteen for one");
        assertEquals(GateRules.swarmSize(1), GateRules.swarmSize(0), "at least one is counted");
        assertEquals(16 + 5, GateRules.swarmSize(2), "five more for each pilgrim past the first");
        assertTrue(GateRules.swarmSize(4) > GateRules.swarmSize(3));
    }

    @Test
    void wavesRiseAsTheLastFallsAndTheSwarmIsBeatenWhenAllAreDown() {
        assertTrue(GateRules.nextWaveDue(0, 0), "the first at once");
        assertTrue(GateRules.nextWaveDue(0, 9), "the first at once, whatever stands");
        assertFalse(GateRules.nextWaveDue(1, 2));
        assertTrue(GateRules.nextWaveDue(1, GateRules.NEXT_WAVE_AT), "the last all but gone");
        assertFalse(GateRules.beaten(1, 0), "a wave still to rise");
        assertFalse(GateRules.beaten(0, 1), "one still standing");
        assertTrue(GateRules.beaten(0, 0));
    }

    @Test
    void soldiersRiseAtThePilgrimsLevelNeverOverHells() {
        assertEquals(50, GateRules.soldierLevel(1), "no level asked, but a beginner meets no beginners' soldiers");
        assertEquals(50, GateRules.soldierLevel(0));
        assertEquals(53, GateRules.soldierLevel(53));
        assertEquals(56, GateRules.soldierLevel(60), "short of Hell's own");
        assertEquals(56, GateRules.soldierLevel(100));
        assertTrue(GateRules.TOP_LEVEL < Zones.HELL.levelBand().minLevel(), "weaker than Hell");
    }
}
