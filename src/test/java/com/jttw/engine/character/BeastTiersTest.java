package com.jttw.engine.character;

import com.jttw.engine.character.byakko.ByakkoAbilityHandler;
import com.jttw.engine.character.dragon.DragonAbilityHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Divine Beasts' tiered numbers, as their tooltips state them. */
class BeastTiersTest {

    @Test
    void tidalForceMultipliesKnockbackThreeThenFiveTimes() {
        assertEquals(1.0, DragonAbilityHandler.knockbackMultiplier(0));
        assertEquals(3.0, DragonAbilityHandler.knockbackMultiplier(1));
        assertEquals(5.0, DragonAbilityHandler.knockbackMultiplier(2));
        assertEquals(1.2, DragonAbilityHandler.BASE_KNOCKBACK * DragonAbilityHandler.knockbackMultiplier(1), 1e-9);
    }

    @Test
    void velocityStrikeIsFiveThenTenPercentABlockASecond() {
        assertEquals(0.0, ByakkoAbilityHandler.velocityBonusPerBlockPerSecond(0));
        assertEquals(0.05, ByakkoAbilityHandler.velocityBonusPerBlockPerSecond(1));
        assertEquals(0.10, ByakkoAbilityHandler.velocityBonusPerBlockPerSecond(2));
    }
}
