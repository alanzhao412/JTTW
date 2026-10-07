package com.jttw.engine.character;

import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTrees;
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
 * Each character's skill bar must be exactly what the old SyncAbilitiesPacket builder produced
 * for the same outcome and unlocked nodes ({@link LegacySkillBar}).
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

    @TestFactory
    Stream<DynamicTest> barMatchesTheOldBuilder() {
        return Characters.all().stream().map(c -> DynamicTest.dynamicTest(c.outcomeId(), () -> {
            for (Set<String> unlocked : cases(c)) {
                assertEquals(LegacySkillBar.build(c.outcomeId(), unlocked), c.skillBar(unlocked),
                        c.outcomeId() + " with " + unlocked);
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
        assertEquals("wu_transform", bar.get(0).id());
        assertEquals(2, bar.get(0).tier());
        assertTrue(bar.get(1).isEmpty(), "clone not learned");
        assertEquals("wu_ring", bar.get(2).id());
        assertTrue(bar.get(3).isEmpty());
    }

    @Test
    void yuanDiTransformOnlyTakesAKeyFromTierTwo() {
        PlayableCharacter yd = Characters.byOutcome("yuan_di");
        assertTrue(yd.skillBar(Set.of("yd_transform_1")).get(0).isEmpty());
        assertEquals(2, yd.skillBar(Set.of("yd_transform_1", "yd_transform_2")).get(0).tier());
    }

    @Test
    void dragonPutsDimensionTravelInSlotTwoUntilTidalWaveIsLearned() {
        PlayableCharacter dragon = Characters.byOutcome("dragon");
        Set<String> flight3 = Set.of("dr_flight_1", "dr_flight_2", "dr_flight_3");
        assertEquals("dr_dimension", dragon.skillBar(flight3).get(2).id());
        Set<String> withWave = new HashSet<>(flight3);
        withWave.add("dr_wave_1");
        assertEquals("dr_wave", dragon.skillBar(withWave).get(2).id());
        assertTrue(dragon.skillBar(Set.of("dr_flight_1", "dr_flight_2")).get(2).isEmpty());
    }

    @Test
    void marshalAlwaysHasThirtySixTransformations() {
        List<AbilitySlot> bar = Characters.byOutcome("marshal_of_the_heavenly_canopy").skillBar(Set.of());
        assertEquals("mc_transform", bar.get(0).id());
        assertEquals("36 Transformations", bar.get(0).displayName());
    }

    @Test
    void pengHasNothingYet() {
        assertEquals(PlayableCharacter.emptyBar(), Characters.byOutcome("gold_winged_peng")
                .skillBar(new HashSet<>(EVERY_NODE)));
    }
}
