package com.jttw.engine.progression.skilltree;

import com.jttw.engine.progression.stats.ProgressionClass;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines every ability milestone for all four classes.
 *
 * <p>An {@code AbilityUnlock} is a gate — it stores what becomes available at what level for
 * which class. Reaching the level only makes it available: the player buys it with skill points
 * (see {@link #cost()} and {@link SkillPurchase#purchaseClassAbility}). Gameplay systems that
 * care about a specific ability should check:
 * <pre>{@code
 * if (PlayerLevel.hasAbility(player, "heavenly_mobility")) { ... }
 * }</pre>
 *
 * <p>Adding a new ability requires only:
 * <ol>
 *   <li>A new {@code reg()} line in the static block below.</li>
 *   <li>A check in the relevant gameplay system using the matching ability ID string.</li>
 * </ol>
 * No changes to {@link LevelManager} or {@link PlayerLevel} are needed.
 *
 * <p>Only {@code heavenly_mobility} does anything yet (the Heavenly cloud, see
 * {@link com.jttw.engine.flight.JTTWFlight#hasHeavenlyCloud}); the rest can be bought but have no
 * effect.
 *
 * <p><b>Milestone overview:</b>
 * <pre>
 * ── DIVINE BEAST (form-gated, domain authority) ──────────────────────────
 *   Lv  1   lesser_form         Base identity. Lesser domain presence.
 *   Lv 20   domain_sense        Passive territorial awareness.
 *   Lv 40   intermediate_form   Form evolution phase I.
 *   Lv 60   domain_authority    Area suppression aura.
 *   Lv 80   sovereign_form      Form evolution phase II.
 *   Lv100   apex_domain         Peak domain control.
 *
 * ── HEAVENLY (ability expansion, widest toolkit) ─────────────────────────
 *   Lv 10   heavenly_mobility   A slow cloud to fly on (sprint in flight).
 *   Lv 20   divine_perception   See through concealment and illusions.
 *   Lv 30   formation_sense     Detect enemy formations and hidden traps.
 *   Lv 40   celestial_support   Unlocks support utility package.
 *   Lv 60   heavenly_authority  Command weaker beings. Social power.
 *   Lv 80   eye_of_heaven       Omniscient situational perception.
 *   Lv100   mandate_of_heaven   Peak divine authority. Heaven ratifies your actions.
 *
 * ── PRIMORDIAL (sparse gates, massive payoffs) ───────────────────────────
 *   Lv  1   primordial_seed     Dormant power. Unremarkable start.
 *   Lv 30   awakening           First power stir. Something ancient moves.
 *   Lv 50   half_awakening      MAJOR breakpoint. Stat ramp kicks in. (see StatBlock)
 *   Lv 70   primal_form         First true form emerges.
 *   Lv 90   chaos_embodiment    Near-peak primordial state. Pre-origin power.
 *   Lv100   primordial_origin   Full awakening. Chaos made manifest.
 *
 * ── YAOGUAI (aggressive power spikes, transformation focus) ──────────────
 *   Lv 20   demon_instinct      Combat intuition. Aggression unlocked.
 *   Lv 30   first_spike         Power Spike I. (StatBlock +20% bonus applied)
 *   Lv 50   demon_transformation  Transformation ability unlocked.
 *   Lv 50   second_spike        Power Spike II.
 *   Lv 70   great_demon_body    Body refinement. Hardened form.
 *   Lv 70   third_spike         Power Spike III.
 *   Lv 90   demon_sovereign     Apex Yaoguai form. Demon king territory.
 *   Lv 90   fourth_spike        Power Spike IV.
 * </pre>
 */
public record AbilityUnlock(
        ProgressionClass cls,
        int requiredLevel,
        String abilityId,
        String displayName
) {
    private static final List<AbilityUnlock> REGISTRY = new ArrayList<>();

    static {
        // ── DIVINE BEAST ──────────────────────────────────────────────────────
        reg(ProgressionClass.DIVINE_BEAST,   1, "lesser_form",          "Lesser Form");
        reg(ProgressionClass.DIVINE_BEAST,  20, "domain_sense",         "Domain Sense");
        reg(ProgressionClass.DIVINE_BEAST,  40, "intermediate_form",    "Intermediate Form");
        reg(ProgressionClass.DIVINE_BEAST,  60, "domain_authority",     "Domain Authority");
        reg(ProgressionClass.DIVINE_BEAST,  80, "sovereign_form",       "Sovereign Form");
        reg(ProgressionClass.DIVINE_BEAST, 100, "apex_domain",          "Apex Domain");

        // ── HEAVENLY ──────────────────────────────────────────────────────────
        reg(ProgressionClass.HEAVENLY,  10, "heavenly_mobility",    "Heavenly Mobility");
        reg(ProgressionClass.HEAVENLY,  20, "divine_perception",    "Divine Perception");
        reg(ProgressionClass.HEAVENLY,  30, "formation_sense",      "Formation Sense");
        reg(ProgressionClass.HEAVENLY,  40, "celestial_support",    "Celestial Support");
        reg(ProgressionClass.HEAVENLY,  60, "heavenly_authority",   "Heavenly Authority");
        reg(ProgressionClass.HEAVENLY,  80, "eye_of_heaven",        "Eye of Heaven");
        reg(ProgressionClass.HEAVENLY, 100, "mandate_of_heaven",    "Mandate of Heaven");

        // ── PRIMORDIAL ────────────────────────────────────────────────────────
        reg(ProgressionClass.PRIMORDIAL,   1, "primordial_seed",    "Primordial Seed");
        reg(ProgressionClass.PRIMORDIAL,  30, "awakening",          "Awakening");
        reg(ProgressionClass.PRIMORDIAL,  50, "half_awakening",     "Half Awakening");
        reg(ProgressionClass.PRIMORDIAL,  70, "primal_form",        "Primal Form");
        reg(ProgressionClass.PRIMORDIAL,  90, "chaos_embodiment",   "Chaos Embodiment");
        reg(ProgressionClass.PRIMORDIAL, 100, "primordial_origin",  "Primordial Origin");

        // ── YAOGUAI ───────────────────────────────────────────────────────────
        reg(ProgressionClass.YAOGUAI,  20, "demon_instinct",        "Demon Instinct");
        reg(ProgressionClass.YAOGUAI,  30, "first_spike",           "Power Spike I");
        reg(ProgressionClass.YAOGUAI,  50, "demon_transformation",  "Demon Transformation");
        reg(ProgressionClass.YAOGUAI,  50, "second_spike",          "Power Spike II");
        reg(ProgressionClass.YAOGUAI,  70, "great_demon_body",      "Great Demon Body");
        reg(ProgressionClass.YAOGUAI,  70, "third_spike",           "Power Spike III");
        reg(ProgressionClass.YAOGUAI,  90, "demon_sovereign",       "Demon Sovereign");
        reg(ProgressionClass.YAOGUAI,  90, "fourth_spike",          "Power Spike IV");
    }

    private static void reg(ProgressionClass cls, int level, String id, String name) {
        REGISTRY.add(new AbilityUnlock(cls, level, id, name));
    }

    /**
     * Skill points to buy it: 1 below level 50, 2 from level 50, 3 at level 100. Points come one
     * every 5 levels (20 in all) and the skill trees want about twice that, so these stay cheap
     * beside a tree node, while the late, big ones cost more.
     */
    public int cost() {
        return 1 + requiredLevel / 50;
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    /** The unlock with this ability id, or null. */
    @javax.annotation.Nullable
    public static AbilityUnlock byId(String abilityId) {
        for (AbilityUnlock u : REGISTRY) {
            if (u.abilityId.equals(abilityId)) return u;
        }
        return null;
    }

    public static List<AbilityUnlock> getAll() {
        return Collections.unmodifiableList(REGISTRY);
    }

    /**
     * Returns every unlock that becomes available when a player of {@code cls}
     * advances from {@code previousLevel} to {@code newLevel} (inclusive bounds on new).
     * Handles multi-level-up edge cases correctly.
     */
    public static List<AbilityUnlock> getNewUnlocks(ProgressionClass cls, int previousLevel, int newLevel) {
        List<AbilityUnlock> triggered = new ArrayList<>();
        for (AbilityUnlock unlock : REGISTRY) {
            if (unlock.cls == cls
                    && unlock.requiredLevel > previousLevel
                    && unlock.requiredLevel <= newLevel) {
                triggered.add(unlock);
            }
        }
        return triggered;
    }
}