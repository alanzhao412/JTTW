package com.jttw.engine.character;

import com.jttw.engine.character.byakko.ByakkoCharacter;
import com.jttw.engine.character.dragon.DragonCharacter;
import com.jttw.engine.character.erlang.ErlangShenCharacter;
import com.jttw.engine.character.phoenix.PhoenixCharacter;
import com.jttw.engine.character.turtle.TurtleCharacter;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Characters: the single mapping from saved progression state to the character being played. */
class CharactersTest {

    @ParameterizedTest
    @EnumSource(ReincarnationOutcome.class)
    void everyOutcomeHasItsCharacter(ReincarnationOutcome outcome) {
        PlayableCharacter c = Characters.byOutcome(outcome.id);
        assertNotNull(c, outcome.id);
        assertEquals(outcome.id, c.outcomeId());
    }

    /** The Saurian Demon King plays himself, the Flood-Dragon, never the Grand Saint of Nine Spirits. */
    @Test
    void theSaurianDemonKingPlaysHimself() {
        assertSame(com.jttw.engine.character.saurian.SaurianCharacter.INSTANCE,
                Characters.resolve("saurian_demon_king", ProgressionClass.YAOGUAI, null));
    }

    /**
     * Holding Shift shows a second page of four: the 18 Transformations, for those whose first four
     * keys are full (the Saurian, the World Turtle), and the Dragon's Dimension Travel; nobody else has one.
     */
    @Test
    void theShiftPageHoldsTheEighteenTransformations() {
        var saurian = com.jttw.engine.character.saurian.SaurianCharacter.INSTANCE;
        assertTrue(saurian.shiftBar(java.util.Set.of()).stream().allMatch(AbilitySlot::isEmpty), "nothing until learned");
        var page = saurian.shiftBar(java.util.Set.of("sdk_transform_1", "sdk_transform_2"));
        assertEquals(PlayableCharacter.SLOT_COUNT, page.size());
        assertEquals("sdk_transform", page.get(0).id());
        assertEquals(2, page.get(0).tier());
        assertEquals("tu_transform", TurtleCharacter.INSTANCE.shiftBar(java.util.Set.of("tu_transform_1")).get(0).id());
        for (PlayableCharacter c : Characters.all()) {
            if (c == saurian || c == TurtleCharacter.INSTANCE || c == DragonCharacter.INSTANCE) continue;
            assertTrue(c.shiftBar(new java.util.HashSet<>(c.skillTree().nodes().keySet())).stream().allMatch(AbilitySlot::isEmpty), c.outcomeId());
        }
    }

    /** Every divine beast practises the 18 Transformations: its level by its tier, its menu on a key. */
    @TestFactory
    Stream<DynamicTest> everyDivineBeastHasTheEighteen() {
        return Stream.of(ByakkoCharacter.INSTANCE, PhoenixCharacter.INSTANCE, DragonCharacter.INSTANCE, TurtleCharacter.INSTANCE)
                .map(c -> DynamicTest.dynamicTest(c.outcomeId(), () -> {
                    String p = c.skillTree().idPrefix() + "transform";
                    assertEquals(com.jttw.engine.transformation.TransformationSchool.EIGHTEEN, c.transformationSchool());
                    assertEquals(0, c.transformationLevel(Set.of()));
                    Set<String> all = Set.of(p + "_1", p + "_2", p + "_3");
                    assertEquals(3, c.transformationLevel(all));
                    var keys = Stream.concat(c.skillBar(all).stream(), c.shiftBar(all).stream()).toList();
                    assertTrue(keys.stream().anyMatch(s -> s.id().equals(p)), "its menu is on a key");
                    assertTrue(Stream.concat(c.skillBar(Set.of()).stream(), c.shiftBar(Set.of()).stream())
                            .noneMatch(s -> s.id().equals(p)), "not before it is learned");
                }));
    }

    @Test
    void outcomeIdsAndTreePrefixesAreUnique() {
        List<PlayableCharacter> all = Characters.all();
        assertEquals(all.size(), all.stream().map(PlayableCharacter::outcomeId).distinct().count());
        assertEquals(all.size(), all.stream().map(c -> c.skillTree().idPrefix()).distinct().count());
    }

    @Test
    void savedOutcomeWinsOverClass() {
        // An outcome that names a character is used even if the saved class disagrees
        assertSame(DragonCharacter.INSTANCE,
                Characters.resolve("dragon", ProgressionClass.PRIMORDIAL, null));
        assertSame(ErlangShenCharacter.INSTANCE,
                Characters.resolve("eye_of_heaven", ProgressionClass.DIVINE_BEAST, DivineBeastLine.FIRE));
    }

    @Test
    void classStandsInWhenNoOutcomeIsSaved() {
        assertSame(WukongCharacter.INSTANCE, Characters.resolve("", ProgressionClass.PRIMORDIAL, null));
        assertSame(ErlangShenCharacter.INSTANCE, Characters.resolve("", ProgressionClass.HEAVENLY, null));
        assertSame(com.jttw.engine.character.ninespirits.NineSpiritsCharacter.INSTANCE,
                Characters.resolve("", ProgressionClass.YAOGUAI, null));
        assertNull(Characters.resolve("", null, null), "no class, no character");
    }

    @Test
    void classStandsInWhenTheOutcomeHasNoCharacter() {
        assertSame(WukongCharacter.INSTANCE,
                Characters.resolve("not_an_outcome", ProgressionClass.PRIMORDIAL, null));
    }

    @Test
    void divineBeastsPlayTheBeastOfTheirLine() {
        assertSame(DragonCharacter.INSTANCE, Characters.resolve("", ProgressionClass.DIVINE_BEAST, DivineBeastLine.WATER));
        assertSame(TurtleCharacter.INSTANCE, Characters.resolve("", ProgressionClass.DIVINE_BEAST, DivineBeastLine.EARTH));
        assertSame(ByakkoCharacter.INSTANCE, Characters.resolve("", ProgressionClass.DIVINE_BEAST, DivineBeastLine.BEAST));
        assertSame(PhoenixCharacter.INSTANCE, Characters.resolve("", ProgressionClass.DIVINE_BEAST, DivineBeastLine.FIRE));
        assertNull(Characters.resolve("", ProgressionClass.DIVINE_BEAST, null), "no line chosen yet");
    }

    @Test
    void nothingSavedMeansNoCharacter() {
        assertNull(Characters.resolve("", null, null));
        assertNull(Characters.byOutcome(""));
    }

    @Test
    void divineBeastCharactersMatchTheirLinesBothWays() {
        // the class fallback and the outcome's own line must agree
        for (ReincarnationOutcome o : ReincarnationOutcome.values()) {
            DivineBeastLine line = DivineBeastLine.fromOutcome(o);
            if (line == null) continue;
            assertSame(Characters.byOutcome(o.id), Characters.resolve("", ProgressionClass.DIVINE_BEAST, line), o.id);
        }
    }

    @TestFactory
    Stream<DynamicTest> treeScreenHasOneHeadingPerColumn() {
        return Characters.all().stream().map(c -> DynamicTest.dynamicTest(c.outcomeId(), () -> {
            Set<Integer> columns = c.skillTree().nodes().values().stream()
                    .map(SkillNode::col).collect(Collectors.toCollection(HashSet::new));
            assertEquals(columns.size(), c.treeColumns().size(),
                    c.treeTitle() + " columns " + columns + " vs headings " + c.treeColumns());
            for (int col : columns) assertTrue(col < c.treeColumns().size(), "column " + col + " has no heading");
        }));
    }

    @TestFactory
    Stream<DynamicTest> everyCharacterNamesItsTree() {
        return Characters.all().stream().map(c -> DynamicTest.dynamicTest(c.outcomeId(), () ->
                assertFalse(c.treeTitle().isBlank())));
    }
}
