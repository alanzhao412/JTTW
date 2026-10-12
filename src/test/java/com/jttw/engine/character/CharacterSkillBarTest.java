package com.jttw.engine.character;

import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTrees;
import com.jttw.engine.character.marshal.MarshalCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.transformation.TransformationSchool;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Each character's skill bar: four slots, laid out the one way (see PlayableCharacter#activate).
 */
class CharacterSkillBarTest {

    private static final List<String> EVERY_NODE = SkillTrees.all().stream()
            .flatMap(t -> t.nodes().keySet().stream()).toList();

    /** Node sets worth checking for one character: none, each branch tier by tier, random mixes. */
    private static List<Set<String>> cases(PlayableCharacter c) {
        List<Set<String>> cases = new ArrayList<>();
        cases.add(Set.of());
        List<SkillNode> own = c.skillTree().allOrdered();
        // each node alone, and each node with everything before it in its column
        for (SkillNode n : own) {
            cases.add(Set.of(n.id()));
            Set<String> upTo = new HashSet<>();
            for (SkillNode m : own) if (m.col() == n.col() && m.row() <= n.row()) upTo.add(m.id());
            cases.add(upTo);
        }
        cases.add(new HashSet<>(c.skillTree().nodes().keySet()));
        // random mixes, including nodes from other characters' trees
        Random rnd = new Random(c.outcomeId().hashCode());
        for (int i = 0; i < 400; i++) {
            Set<String> s = new HashSet<>();
            for (String id : EVERY_NODE) if (rnd.nextInt(3) == 0) s.add(id);
            cases.add(s);
        }
        return cases;
    }

    /** The true forms and ultimates, which belong on Shift + M (where M holds a transformation art). */
    private static final Set<String> ULTIMATES = Set.of("bk_true", "dr_true", "tu_true", "ph_true", "bdk_bull", "sdk_wrath");

    /**
     * One key layout for everyone (PlayableCharacter#activate): a transformation art on M; the
     * true form or ultimate on Shift + M; V, B and N filled from V, without gaps.
     */
    @TestFactory
    Stream<DynamicTest> everyCharacterFollowsTheOneKeyLayout() {
        return Characters.all().stream().map(c -> DynamicTest.dynamicTest(c.outcomeId(), () -> {
            for (Set<String> unlocked : cases(c)) {
                List<AbilitySlot> bar = c.skillBar(unlocked), shift = c.shiftBar(unlocked);
                for (int i = 0; i < 4; i++) {
                    if (bar.get(i).type() == AbilityType.MENU) assertEquals(3, i, c.outcomeId() + ": the transformation art is on M");
                    if (ULTIMATES.contains(bar.get(i).id())) fail(c.outcomeId() + ": " + bar.get(i).id() + " belongs on Shift + M");
                    if (ULTIMATES.contains(shift.get(i).id())) assertEquals(3, i, c.outcomeId() + ": the true form is on Shift + M");
                }
            }
            // everything learned: V, B and N filled from V, no gaps
            List<AbilitySlot> all = c.skillBar(new HashSet<>(c.skillTree().nodes().keySet()));
            boolean gap = false;
            for (int i = 0; i < 3; i++) {
                if (all.get(i).isEmpty()) gap = true;
                else assertFalse(gap, c.outcomeId() + ": a gap before " + all.get(i).id());
            }
        }));
    }

    @TestFactory
    Stream<DynamicTest> barAlwaysHasFourSlots() {
        return Characters.all().stream().map(c -> DynamicTest.dynamicTest(c.outcomeId(), () -> {
            for (Set<String> unlocked : cases(c)) {
                assertEquals(PlayableCharacter.SLOT_COUNT, c.skillBar(unlocked).size());
            }
        }));
    }

    @Test
    void wukongBarFillsAsBranchesAreLearned() {
        List<AbilitySlot> bar = Characters.byOutcome("celestial_primate")
                .skillBar(Set.of("wu_transform_1", "wu_transform_2", "wu_ring_1"));
        assertEquals("wu_ring", bar.get(0).id(), "V: Ring of Fire");
        assertTrue(bar.get(1).isEmpty(), "clone not learned");
        assertTrue(bar.get(2).isEmpty(), "N: the Golden Eyes, not yet granted");
        assertEquals("wu_transform", bar.get(3).id(), "M: the 72 Transformations");
        assertEquals(2, bar.get(3).tier());
    }

    @Test
    void yuanDiTransformOnlyTakesAKeyFromTierTwo() {
        PlayableCharacter yd = Characters.byOutcome("yuan_di");
        assertTrue(yd.skillBar(Set.of("yd_transform_1")).get(3).isEmpty());
        assertEquals(2, yd.skillBar(Set.of("yd_transform_1", "yd_transform_2")).get(3).tier(), "on M, its form");
    }

    @Test
    void dragonKeepsDimensionTravelOnShiftVWhateverElseItLearns() {
        PlayableCharacter dragon = Characters.byOutcome("dragon");
        Set<String> flight3 = Set.of("dr_flight_1", "dr_flight_2", "dr_flight_3");
        assertEquals("dr_dimension", dragon.shiftBar(flight3).get(0).id()); // Shift + V
        Set<String> withWave = new HashSet<>(flight3);
        withWave.add("dr_wave_1");
        assertEquals("dr_wave", dragon.skillBar(withWave).get(2).id());
        assertEquals("dr_dimension", dragon.shiftBar(withWave).get(0).id(), "learning Tidal Wave does not take it away");
        assertTrue(dragon.skillBar(Set.of("dr_flight_1", "dr_flight_2")).get(2).isEmpty());
    }

    @Test
    void marshalsThirtySixTransformationsComesWithItsFirstNode() {
        PlayableCharacter marshal = Characters.byOutcome("marshal_of_the_heavenly_canopy");
        assertTrue(marshal.skillBar(Set.of()).get(3).isEmpty(), "no node, no menu");
        AbilitySlot slot = marshal.skillBar(Set.of("mc_transform_1", "mc_transform_2")).get(3); // on M
        assertEquals("mc_transform", slot.id());
        assertEquals("36 Transformations", slot.displayName());
        assertEquals(AbilityType.MENU, slot.type());
        assertEquals(2, slot.tier());
    }

    @Test
    void transformationArtsAndLevelsPerCharacter() {
        assertEquals(TransformationSchool.SEVENTY_TWO, Characters.byOutcome("celestial_primate").transformationSchool());
        assertEquals(TransformationSchool.SEVENTY_TWO, Characters.byOutcome("eye_of_heaven").transformationSchool());
        assertEquals(TransformationSchool.THIRTY_SIX, Characters.byOutcome("marshal_of_the_heavenly_canopy").transformationSchool());
        assertEquals(TransformationSchool.SEVENTY_TWO, Characters.byOutcome("bull_demon_king").transformationSchool());
        assertEquals(2, Characters.byOutcome("bull_demon_king").transformationLevel(Set.of("bdk_transform_1", "bdk_transform_2")));
        for (String beast : List.of("dragon", "phoenix", "white_tiger", "turtle")) {
            assertEquals(TransformationSchool.EIGHTEEN, Characters.byOutcome(beast).transformationSchool(), beast);
        }
        for (String other : List.of("yuan_di", "gold_winged_peng", "third_lotus_prince")) {
            assertNull(Characters.byOutcome(other).transformationSchool(), other);
        }
        assertEquals(3, Characters.byOutcome("celestial_primate").transformationLevel(Set.of("wu_transform_1", "wu_transform_2", "wu_transform_3")));
        assertEquals(1, Characters.byOutcome("eye_of_heaven").transformationLevel(Set.of("es_transform_1", "wu_transform_3")));
        assertEquals(0, Characters.byOutcome("marshal_of_the_heavenly_canopy").transformationLevel(Set.of("wu_transform_1")));
    }

    @Test
    void pengSoarsOnVThrowsFeathersOnBAndEclipsesOnN() {
        List<AbilitySlot> bar = Characters.byOutcome("gold_winged_peng").skillBar(new HashSet<>(EVERY_NODE));
        assertEquals("gp_soar", bar.get(0).id());
        assertEquals(AbilityType.HELD, bar.get(0).type());
        assertEquals("gp_feathers", bar.get(1).id());
        assertEquals("gp_eclipse", bar.get(2).id());
        assertTrue(bar.get(3).isEmpty());
    }
}
