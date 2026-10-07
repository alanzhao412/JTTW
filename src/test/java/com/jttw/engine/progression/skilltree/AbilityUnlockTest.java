package com.jttw.engine.progression.skilltree;

import com.jttw.engine.progression.level.LevelConstants;
import com.jttw.engine.progression.stats.ProgressionClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Level milestones that grant each class its innate abilities. */
class AbilityUnlockTest {

    private static List<String> ids(List<AbilityUnlock> unlocks) {
        return unlocks.stream().map(AbilityUnlock::abilityId).toList();
    }

    @Test
    void idsAreUniqueAndLevelsInRange() {
        Set<String> ids = new HashSet<>();
        for (AbilityUnlock u : AbilityUnlock.getAll()) {
            assertTrue(ids.add(u.abilityId()), "duplicate " + u.abilityId());
            assertTrue(u.requiredLevel() >= 1 && u.requiredLevel() <= LevelConstants.MAX_LEVEL, u.abilityId());
            assertFalse(u.displayName().isBlank(), u.abilityId());
        }
    }

    @ParameterizedTest
    @EnumSource(ProgressionClass.class)
    void everyClassHasMilestones(ProgressionClass cls) {
        assertFalse(AbilityUnlock.getNewUnlocks(cls, 0, LevelConstants.MAX_LEVEL).isEmpty());
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
        assertEquals(List.of("heavenly_mobility", "divine_perception", "formation_sense"),
                ids(AbilityUnlock.getNewUnlocks(ProgressionClass.HEAVENLY, 5, 35)));
    }

    @Test
    void levelOneMilestonesFireWhenStartingFromZero() {
        assertEquals(List.of("primordial_seed"), ids(AbilityUnlock.getNewUnlocks(ProgressionClass.PRIMORDIAL, 0, 1)));
    }

    @Test
    void otherClassesMilestonesAreNotGranted() {
        assertEquals(List.of(), ids(AbilityUnlock.getNewUnlocks(ProgressionClass.YAOGUAI, 0, 19)));
        assertTrue(ids(AbilityUnlock.getNewUnlocks(ProgressionClass.YAOGUAI, 0, 100))
                .stream().noneMatch(id -> id.equals("eye_of_heaven")));
    }

    @Test
    void yaoguaiSpikesLandOnTheStatSpikeLevels() {
        List<AbilityUnlock> spikes = AbilityUnlock.getAll().stream()
                .filter(u -> u.abilityId().endsWith("_spike")).toList();
        assertEquals(List.of(30, 50, 70, 90), spikes.stream().map(AbilityUnlock::requiredLevel).toList());
    }

    @Test
    void registryIsReadOnly() {
        assertThrows(UnsupportedOperationException.class, () -> AbilityUnlock.getAll().clear());
    }
}
