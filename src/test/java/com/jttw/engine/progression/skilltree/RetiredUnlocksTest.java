package com.jttw.engine.progression.skilltree;

import com.jttw.engine.progression.stats.ProgressionClass;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The class abilities taken off the class layer, and what is paid back for them. */
class RetiredUnlocksTest {

    @Test
    void everyRetiredIdIsGoneFromTheLayerButTheForms() {
        Set<String> seen = new HashSet<>();
        for (RetiredUnlocks.Retired r : RetiredUnlocks.all()) {
            assertTrue(seen.add(r.abilityId()), "duplicate " + r.abilityId());
            AbilityUnlock now = AbilityUnlock.byId(r.abilityId());
            if (now == null) continue;
            assertTrue(now.milestone(), r.abilityId() + " still on the layer only as a free milestone");
            assertEquals(ProgressionClass.DIVINE_BEAST, now.cls());
        }
        assertFalse(seen.contains("heavenly_mobility"), "the Heavenly cloud stays");
        for (String kept : List.of("domain_sense", "guardians_hide", "domain_authority", "apex_domain")) {
            assertFalse(seen.contains(kept), kept + " stays");
        }
    }

    @Test
    void theRetiredAreTheOldLadders() {
        long heavenly = RetiredUnlocks.all().stream().filter(r -> r.cls() == ProgressionClass.HEAVENLY).count();
        long primordial = RetiredUnlocks.all().stream().filter(r -> r.cls() == ProgressionClass.PRIMORDIAL).count();
        long yaoguai = RetiredUnlocks.all().stream().filter(r -> r.cls() == ProgressionClass.YAOGUAI).count();
        long beast = RetiredUnlocks.all().stream().filter(r -> r.cls() == ProgressionClass.DIVINE_BEAST).count();
        assertEquals(6, heavenly, "every Heavenly one but the cloud");
        assertEquals(6, primordial);
        assertEquals(8, yaoguai);
        assertEquals(3, beast, "the three forms");
    }

    @Test
    void refundIsWhatEachCostUnderTheOldScheme() {
        // demon_instinct (20) 1, second_spike (50) 2, mandate_of_heaven (100) 3, sovereign_form (80) 2
        assertEquals(1 + 2 + 3 + 2, RetiredUnlocks.refundFor(
                Set.of("demon_instinct", "second_spike", "mandate_of_heaven", "sovereign_form")));
        assertEquals(1, RetiredUnlocks.refundFor(Set.of("lesser_form")));
        assertEquals(1, RetiredUnlocks.refundFor(Set.of("intermediate_form")));
    }

    @Test
    void whatIsKeptIsNotRefunded() {
        assertEquals(0, RetiredUnlocks.refundFor(Set.of("heavenly_mobility", "domain_sense", "apex_domain")));
        assertEquals(0, RetiredUnlocks.refundFor(Set.of()));
        assertEquals(List.of(), RetiredUnlocks.heldAmong(Set.of("heavenly_mobility", "no_such_thing")));
    }

    @Test
    void heldAmongKeepsTheListsOrder() {
        assertEquals(List.of("lesser_form", "awakening", "fourth_spike"),
                RetiredUnlocks.heldAmong(Set.of("fourth_spike", "awakening", "lesser_form", "domain_sense"))
                        .stream().map(RetiredUnlocks.Retired::abilityId).toList());
    }
}
