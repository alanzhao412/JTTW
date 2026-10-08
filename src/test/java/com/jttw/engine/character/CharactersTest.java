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
    @EnumSource(value = ReincarnationOutcome.class,
            names = {"SAURIAN_DEMON_KING"}, mode = EnumSource.Mode.EXCLUDE)
    void everyBuiltOutcomeHasItsCharacter(ReincarnationOutcome outcome) {
        PlayableCharacter c = Characters.byOutcome(outcome.id);
        assertNotNull(c, outcome.id);
        assertEquals(outcome.id, c.outcomeId());
    }

    /** The Saurian Demon King has no character yet; the Yaoguai stand-in plays him. */
    @ParameterizedTest
    @EnumSource(value = ReincarnationOutcome.class, names = {"SAURIAN_DEMON_KING"})
    void unbuiltYaoguaiPlayTheGrandSaintOfNineSpirits(ReincarnationOutcome outcome) {
        assertNull(Characters.byOutcome(outcome.id));
        assertSame(com.jttw.engine.character.ninespirits.NineSpiritsCharacter.INSTANCE,
                Characters.resolve(outcome.id, ProgressionClass.YAOGUAI, null));
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
                Characters.resolve("saurian_demon_king", ProgressionClass.PRIMORDIAL, null));
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
