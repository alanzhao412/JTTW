package com.jttw.engine.progression.level;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** EnemyTier and BiomeLevelBand: how kills in level-banded biomes pay out. */
class WorldScalingTest {

    private static BiomeLevelBand band(int min, int max, int gruntXp) {
        return new BiomeLevelBand(ResourceLocation.fromNamespaceAndPath("minecraft", "plains"), min, max, gruntXp);
    }

    @Test
    void enemyTiersMultiplyXp() {
        assertEquals(1.0f, EnemyTier.GRUNT.xpMultiplier);
        assertEquals(5.0f, EnemyTier.GENERAL.xpMultiplier);
        assertEquals(20.0f, EnemyTier.BOSS.xpMultiplier);
    }

    @Test
    void enemyTierParsingDefaultsToGrunt() {
        assertEquals(EnemyTier.BOSS, EnemyTier.parse(" boss "));
        assertEquals(EnemyTier.GENERAL, EnemyTier.parse("GENERAL"));
        assertEquals(EnemyTier.GRUNT, EnemyTier.parse("dragon"));
        assertEquals(EnemyTier.GRUNT, EnemyTier.parse(null));
    }

    @Test
    void firstBandAboveTheVanillaCapPaysBaseXp() {
        // minLevel 21 = VANILLA_CAP_LEVEL + 1, so the band scalar is exactly 1
        BiomeLevelBand b = band(LevelConstants.VANILLA_CAP_LEVEL + 1, 30, 40);
        assertEquals(40, b.xpForTier(EnemyTier.GRUNT));
        assertEquals(200, b.xpForTier(EnemyTier.GENERAL));
        assertEquals(800, b.xpForTier(EnemyTier.BOSS));
    }

    @Test
    void deeperBandsPayTenPercentMorePerLevelOfDepth() {
        // minLevel 41: scalar = 1 + (41 - 21) / 10 = 3
        assertEquals(120, band(41, 50, 40).xpForTier(EnemyTier.GRUNT));
    }

    @Test
    void bandIsRewardingFromItsMinimumToFiveAboveItsMaximum() {
        BiomeLevelBand b = band(30, 40, 10);
        assertFalse(b.isRewardingForLevel(29));
        assertTrue(b.isRewardingForLevel(30));
        assertTrue(b.isRewardingForLevel(45));
        assertFalse(b.isRewardingForLevel(46));
    }
}
