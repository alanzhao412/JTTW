package com.jttw.engine.progression.skilltree;

import com.jttw.engine.progression.level.LevelConstants;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The class layer: the Divine Beast's milestones and passives, and the Heavenly cloud. */
class AbilityUnlockTest {

    private static List<String> ids(List<AbilityUnlock> unlocks) {
        return unlocks.stream().map(AbilityUnlock::abilityId).toList();
    }

    private static List<String> idsOf(ProgressionClass cls) {
        return ids(AbilityUnlock.getAll().stream().filter(u -> u.cls() == cls).toList());
    }

    @Test
    void idsAreUniqueAndLevelsInRange() {
        Set<String> ids = new HashSet<>();
        for (AbilityUnlock u : AbilityUnlock.getAll()) {
            assertTrue(ids.add(u.abilityId()), "duplicate " + u.abilityId());
            assertTrue(u.requiredLevel() >= 1 && u.requiredLevel() <= LevelConstants.MAX_LEVEL, u.abilityId());
            assertFalse(u.displayName().isBlank(), u.abilityId());
            assertFalse(u.description().isBlank(), u.abilityId() + " says what it does");
        }
    }

    @Test
    void divineBeastHasItsFormsAndPassives() {
        assertEquals(List.of("lesser_form", "domain_sense", "guardians_hide", "intermediate_form",
                "domain_authority", "sovereign_form", "apex_domain"), idsOf(ProgressionClass.DIVINE_BEAST));
        assertEquals(List.of(1, 20, 30, 40, 60, 80, 100), AbilityUnlock.getAll().stream()
                .filter(u -> u.cls() == ProgressionClass.DIVINE_BEAST).map(AbilityUnlock::requiredLevel).toList());
    }

    @Test
    void heavenlyKeepsOnlyItsCloudAndTheOthersHaveNone() {
        assertEquals(List.of("heavenly_mobility"), idsOf(ProgressionClass.HEAVENLY));
        assertEquals(List.of(), idsOf(ProgressionClass.PRIMORDIAL));
        assertEquals(List.of(), idsOf(ProgressionClass.YAOGUAI));
        AbilityUnlock mobility = AbilityUnlock.byId("heavenly_mobility");
        assertEquals(10, mobility.requiredLevel());
        assertTrue(mobility.purchasable());
        assertEquals(1, mobility.cost());
    }

    @Test
    void formsAreFreeMilestones() {
        for (String id : List.of("lesser_form", "intermediate_form", "sovereign_form")) {
            AbilityUnlock u = AbilityUnlock.byId(id);
            assertTrue(u.milestone(), id);
            assertFalse(u.purchasable(), id + " is not bought");
            assertEquals(0, u.cost(), id + " costs nothing");
            assertTrue(u.reachedAt(u.requiredLevel()), id + " reached at its level");
            assertFalse(u.reachedAt(u.requiredLevel() - 1), id + " not before");
        }
    }

    @Test
    void passivesCostByTheUsualScheme() {
        assertEquals(1, AbilityUnlock.byId("domain_sense").cost());
        assertEquals(1, AbilityUnlock.byId("guardians_hide").cost());
        assertEquals(2, AbilityUnlock.byId("domain_authority").cost());
        assertEquals(3, AbilityUnlock.byId("apex_domain").cost());
        for (AbilityUnlock u : AbilityUnlock.getAll()) {
            if (u.purchasable()) assertEquals(AbilityUnlock.costAt(u.requiredLevel()), u.cost(), u.abilityId());
        }
    }

    @Test
    void apexDomainRequiresDomainAuthorityAndNothingElseRequiresAnything() {
        assertEquals("domain_authority", AbilityUnlock.byId("apex_domain").requires());
        for (AbilityUnlock u : AbilityUnlock.getAll()) {
            if (!u.abilityId().equals("apex_domain")) assertNull(u.requires(), u.abilityId());
        }
    }

    @Test
    void levelUpGrantsOnlyMilestonesCrossed() {
        assertEquals(List.of("domain_sense"),
                ids(AbilityUnlock.getNewUnlocks(ProgressionClass.DIVINE_BEAST, 19, 20)));
        assertEquals(List.of(), ids(AbilityUnlock.getNewUnlocks(ProgressionClass.DIVINE_BEAST, 20, 21)),
                "already had level 20");
    }

    @Test
    void multiLevelJumpGrantsEverythingInBetween() {
        assertEquals(List.of("domain_sense", "guardians_hide", "intermediate_form"),
                ids(AbilityUnlock.getNewUnlocks(ProgressionClass.DIVINE_BEAST, 5, 45)));
        assertEquals(List.of("heavenly_mobility"), ids(AbilityUnlock.getNewUnlocks(ProgressionClass.HEAVENLY, 5, 100)));
    }

    @Test
    void levelOneMilestonesFireWhenStartingFromZero() {
        assertEquals(List.of("lesser_form"), ids(AbilityUnlock.getNewUnlocks(ProgressionClass.DIVINE_BEAST, 0, 1)));
    }

    @Test
    void otherClassesMilestonesAreNotGranted() {
        assertEquals(List.of(), ids(AbilityUnlock.getNewUnlocks(ProgressionClass.YAOGUAI, 0, 100)));
        assertTrue(ids(AbilityUnlock.getNewUnlocks(ProgressionClass.HEAVENLY, 0, 100))
                .stream().noneMatch(id -> id.equals("apex_domain")));
    }

    @Test
    void registryIsReadOnly() {
        assertThrows(UnsupportedOperationException.class, () -> AbilityUnlock.getAll().clear());
    }

    /** The lang file says the same as the fallbacks, for every entry. */
    @Test
    void langFileMatchesTheFallbacks() throws Exception {
        JsonObject lang;
        try (var in = AbilityUnlockTest.class.getResourceAsStream("/assets/jttw_engine/lang/en_us.json")) {
            assertNotNull(in, "the lang file on the classpath");
            lang = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (AbilityUnlock u : AbilityUnlock.getAll()) {
            assertTrue(lang.has(u.nameKey()), u.nameKey());
            assertEquals(u.displayName(), lang.get(u.nameKey()).getAsString(), u.nameKey());
            assertTrue(lang.has(u.descriptionKey()), u.descriptionKey());
            assertEquals(u.description(), lang.get(u.descriptionKey()).getAsString(), u.descriptionKey());
        }
    }
}
