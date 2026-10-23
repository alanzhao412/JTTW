package com.jttw.engine.world.fengdu;

import java.util.List;

/**
 * The rules of Fengdu's Gate of Ghosts, apart from any world (pure, so they are tested without
 * one; {@link FengduGate} keeps them).
 *
 * <p><b>Who may pass.</b> The gate is sealed. It burns open for those who come to it the hard way,
 * through the veil of an Ancient City's frame: each such arrival raises a swarm of Hell's soldiers
 * on the terrace, and when the last of them has fallen the gate ignites by itself and every pilgrim
 * who came by the frame and is still on the terrace may pass ({@link Trip#ADMITTED}). Someone come
 * back up out of Diyu by the side doors of Hell's gate may pass again at once (they have been down
 * already; the way back must not trap them). No one else may, though the veil burns before them:
 * not the traveller who walked to Fengdu over the land, nor one who came up from the frame later
 * while the fight was won by others. No level is asked of anyone, but the soldiers are no
 * beginners' foes: they rise at the highest level of those near the gate, never under
 * {@value #BOTTOM_LEVEL} nor over {@value #TOP_LEVEL} (strong, but short of Hell's own 57-66).
 *
 * <p><b>When it seals again.</b> The gate burns while someone who may pass stands within
 * {@value #RANGE} blocks of it; once none has for {@value #SEAL_AFTER} ticks (they have all gone
 * down, or wandered off), it goes out. A new swarm puts it out at once. Leave is lost on leaving
 * Fengdu ({@value #RANGE} blocks from the gate, or another world, or death): every trip down from
 * the Ancient City is fought for again.
 *
 * <p><b>The swarm.</b> Three waves, each rising when all but one of the last have fallen: a
 * vanguard of Yin soldiers, the main body with a captain among them, and last the gate's own
 * guards, Ox-Head and Horse-Face, with a few more. Every wave grows by two soldiers (the last by
 * one) for each player within {@value #RANGE} blocks of the gate past the first. A pilgrim who arrives while a swarm is
 * already up raises another vanguard at once. The swarm rises, and is beaten, only while someone is
 * at the gate; if no one at all is within {@value #RANGE} blocks for {@value #ABANDON_AFTER}
 * ticks, it sinks back into the ground and is forgotten.
 */
public final class GateRules {

    private GateRules() {}

    /** How far from the gate (horizontally) one is still at it. */
    public static final int RANGE = 64;
    /** How long the gate burns with no one near who may pass. */
    public static final int SEAL_AFTER = 200;
    /** How long a swarm stands with no one near at all before it is given up. */
    public static final int ABANDON_AFTER = 600;
    /** How far from where one arrives on the terrace a soldier may stray before it is brought back. */
    public static final int LEASH = 22;
    /** A wave rises when no more than this many of the last still stand. */
    public static final int NEXT_WAVE_AT = 1;
    /** The weakest the gate's soldiers rise: never a beginner's fight. */
    public static final int BOTTOM_LEVEL = 50;
    /** The strongest: still short of Hell's own, which begin at 57. */
    public static final int TOP_LEVEL = 56;

    /** Where a player stands with the gate. */
    public enum Trip {
        /** Not on a trip: the gate will not let them pass. */
        NONE,
        /** Came through the Ancient City's frame; their swarm is not yet beaten. */
        FIGHTING,
        /** May pass: they broke the swarm, or came back up out of Diyu. */
        ADMITTED;

        public boolean mayPass() {
            return this == ADMITTED;
        }
    }

    /** Coming through the Ancient City's frame: fighting, every time, whatever leave was had before. */
    public static Trip arriveByCity(Trip before) {
        return Trip.FIGHTING;
    }

    /** Coming back up out of Diyu: free to go down again. */
    public static Trip arriveFromHell(Trip before) {
        return Trip.ADMITTED;
    }

    /** The swarm broken: a pilgrim on the terrace may pass. */
    public static Trip swarmBroken(Trip before) {
        return before == Trip.FIGHTING ? Trip.ADMITTED : before;
    }

    /** Whether a trip lapses: away from the gate (farther than {@link #RANGE}), or in another world. */
    public static boolean lapses(boolean atFengdu, double distance) {
        return !atFengdu || distance > RANGE;
    }

    /** Whether the gate burns: no swarm up, and someone who may pass was near within {@link #SEAL_AFTER} ticks. */
    public static boolean burns(boolean swarm, boolean everAdmitted, long now, long lastAdmittedNear) {
        return !swarm && everAdmitted && now - lastAdmittedNear <= SEAL_AFTER;
    }

    /** Whether a swarm is given up: no one near for longer than {@link #ABANDON_AFTER} ticks. */
    public static boolean abandoned(long now, long lastAnyoneNear) {
        return now - lastAnyoneNear > ABANDON_AFTER;
    }

    /** The kinds of wave. */
    public enum WaveKind { VANGUARD, MAIN, GUARD }

    /** One full swarm's waves, in order. */
    public static final List<WaveKind> SWARM = List.of(WaveKind.VANGUARD, WaveKind.MAIN, WaveKind.GUARD);

    /** A wave: so many Yin soldiers, so many of them captains, and whether Ox-Head and Horse-Face come with it. */
    public record Wave(int soldiers, int captains, boolean guards) {
        public int size() {
            return soldiers + (guards ? 2 : 0);
        }
    }

    /** The wave of this kind for {@code pilgrims} players at the gate (at least one counted). */
    public static Wave wave(WaveKind kind, int pilgrims) {
        int more = Math.max(0, pilgrims - 1);
        return switch (kind) {
            case VANGUARD -> new Wave(5 + 2 * more, 0, false);
            case MAIN -> new Wave(6 + 2 * more, 1, false);
            case GUARD -> new Wave(3 + more, 0, true);
        };
    }

    /** How many rise in a whole swarm for {@code pilgrims}. */
    public static int swarmSize(int pilgrims) {
        return SWARM.stream().mapToInt(k -> wave(k, pilgrims).size()).sum();
    }

    /** Whether the next wave rises: the first at once, the rest when the last is all but gone. */
    public static boolean nextWaveDue(int raised, int standing) {
        return raised == 0 || standing <= NEXT_WAVE_AT;
    }

    /** Whether the swarm is beaten: every wave risen, none standing. */
    public static boolean beaten(int waitingWaves, int standing) {
        return waitingWaves == 0 && standing == 0;
    }

    /** The level the soldiers rise at: the highest of those at the gate, kept within {@link #BOTTOM_LEVEL}-{@link #TOP_LEVEL}. */
    public static int soldierLevel(int highestPilgrim) {
        return Math.max(BOTTOM_LEVEL, Math.min(TOP_LEVEL, highestPilgrim));
    }
}
