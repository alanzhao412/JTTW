package com.jttw.engine.reincarnation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;
import java.util.Set;

import static com.jttw.engine.reincarnation.ReincarnationOutcome.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Which of the twelve forms an alignment spread reincarnates into.
 * Arguments are always (heavenly, spiritBeast, primordial, yaoguai).
 */
class ReincarnationResolverTest {

    private static ReincarnationOutcome r(int heavenly, int spiritBeast, int primordial, int yaoguai) {
        return ReincarnationResolver.resolve(heavenly, spiritBeast, primordial, yaoguai);
    }

    /** The README's table: the primary and secondary alignment pick the form. */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            // primordial-dominant
            "primordial+yaoguai,      10, 20, 90, 50, CELESTIAL_PRIMATE",
            "primordial+heavenly,     50, 20, 90, 10, YUAN_DI",
            "primordial+spirit beast, 10, 50, 90, 20, GOLD_WINGED_PENG",
            // heavenly-dominant
            "heavenly+primordial,     90, 20, 50, 10, EYE_OF_HEAVEN",
            "heavenly+yaoguai,        90, 20, 10, 50, THIRD_LOTUS_PRINCE",
            "heavenly+spirit beast,   90, 50, 10, 20, MARSHAL_HEAVENLY_CANOPY",
            // spirit-beast-dominant (spread out, so not the balanced Turtle case)
            "spirit beast+heavenly,   50, 90, 20, 5,  DRAGON",
            "spirit beast+primordial, 20, 90, 50, 5,  PHOENIX",
            "spirit beast+yaoguai,    5,  90, 20, 50, WHITE_TIGER",
            // yaoguai-dominant
            "yaoguai+heavenly,        50, 20, 10, 90, BULL_DEMON_KING",
            "yaoguai+spirit beast,    10, 50, 20, 90, SAURIAN_DEMON_KING",
            "yaoguai+primordial,      10, 20, 50, 90, SAURIAN_DEMON_KING",
    })
    void primaryAndSecondaryPickTheForm(String name, int h, int sb, int p, int y, ReincarnationOutcome expected) {
        assertEquals(expected, r(h, sb, p, y));
    }

    @Test
    void sameDominantAlignmentCanEndInDifferentForms() {
        // the README's point: the pair matters, not just the top alignment
        assertNotEquals(r(90, 20, 50, 10), r(90, 20, 10, 50));
    }

    @Test
    void spiritBeastWithBalancedRestIsTheTurtle() {
        // other three within 10% of their average
        assertEquals(TURTLE, r(30, 90, 30, 30));
        assertEquals(TURTLE, r(31, 90, 29, 30));
    }

    @Test
    void spiritBeastWithUnevenRestIsNotTheTurtle() {
        assertEquals(DRAGON, r(40, 90, 30, 20));
    }

    @Test
    void turtleNeedsTheOtherAlignmentsToBeNonZero() {
        // the balance rule only applies when second and third are earned at all; with the
        // others all zero the tie order makes primordial second, so Phoenix
        assertEquals(PHOENIX, r(0, 90, 0, 0));
    }

    @Test
    void turtleOnlyFromASpiritBeastPrimary() {
        // perfectly balanced others, but heavenly leads; primordial wins the tie for second
        assertEquals(EYE_OF_HEAVEN, r(90, 30, 30, 30));
    }

    @Test
    void tiesBreakPrimordialThenHeavenlyThenSpiritBeastThenYaoguai() {
        assertEquals(YUAN_DI, r(50, 50, 50, 50), "primordial first, heavenly second");
        assertEquals(YUAN_DI, r(0, 0, 0, 0), "nothing earned yet");
        assertEquals(EYE_OF_HEAVEN, r(50, 10, 40, 40), "primordial beats yaoguai for second");
        assertEquals(DRAGON, r(40, 90, 10, 40), "heavenly beats yaoguai for second");
    }

    @Test
    void spiritBeastTieWithHeavenlyLosesFirstPlace() {
        // heavenly outranks spirit beast on a tie, so this is Marshal, not Dragon
        assertEquals(MARSHAL_HEAVENLY_CANOPY, r(80, 80, 10, 5));
    }

    @Test
    void everyFormIsReachable() {
        Set<ReincarnationOutcome> seen = EnumSet.noneOf(ReincarnationOutcome.class);
        int[] v = {0, 10, 25, 50, 90};
        for (int h : v) for (int sb : v) for (int p : v) for (int y : v) seen.add(r(h, sb, p, y));
        assertEquals(EnumSet.allOf(ReincarnationOutcome.class), seen);
    }

    @Test
    void outcomeIdsAreUniqueAndLowercase() {
        Set<String> ids = new java.util.HashSet<>();
        for (ReincarnationOutcome o : values()) {
            assertTrue(ids.add(o.id), o.id);
            assertEquals(o.id.toLowerCase(), o.id);
        }
    }
}
