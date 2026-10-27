package com.jttw.engine.progression.skilltree;

import com.jttw.engine.progression.stats.ProgressionClass;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The class layer beside each character's own skill tree: what a class's members gain, at what
 * level, besides what their character's tree teaches them.
 *
 * <p>It belongs, in the end, to the Divine Beasts. Their beast grows into its forms by level
 * alone, and those forms stand on the list as <b>milestones</b>: reached, never bought, costing
 * nothing. Between them come the beast's <b>passives</b>, bought with skill points once their
 * level is reached (see {@link #cost()} and {@link SkillPurchase#purchaseClassAbility}), and kept
 * running by {@link com.jttw.engine.character.divinebeast.BeastPassives}. The other classes'
 * power is in their characters' trees; of their old class abilities only the Heavenly cloud
 * remains ({@code heavenly_mobility}, see
 * {@link com.jttw.engine.flight.JTTWFlight#hasHeavenlyCloud}). Those that were retired, and the
 * points paid for them, are in {@link RetiredUnlocks}.
 *
 * <p>A system that cares about a bought entry checks:
 * <pre>{@code
 * if (PlayerLevel.hasAbility(player, "heavenly_mobility")) { ... }
 * }</pre>
 * A milestone is never held that way; it is reached when the level is ({@link #reachedAt}).
 *
 * <p><b>The list:</b>
 * <pre>
 * ── DIVINE BEAST ──────────────────────────────────────────────────────────
 *   Lv  1   lesser_form         milestone   The beast's first shape.
 *   Lv 20   domain_sense        1 pt        Who hunts you within 24 blocks glows for you alone.
 *   Lv 30   guardians_hide      1 pt        +10% physical and magic resistance.
 *   Lv 40   intermediate_form   milestone   The true beast.
 *   Lv 60   domain_authority    2 pts       Lesser hostiles within 8 blocks: Slowness I, Weakness I.
 *   Lv 80   sovereign_form      milestone   The sovereign beast.
 *   Lv100   apex_domain         3 pts       The domain to 16 blocks; those in it deal 20% less.
 *                                           Needs domain_authority.
 *
 * ── HEAVENLY ──────────────────────────────────────────────────────────────
 *   Lv 10   heavenly_mobility   1 pt        A cloud to fly on (sprint in flight).
 * </pre>
 *
 * <p>The English names and descriptions here are the fallbacks; the lang file carries the same
 * under {@link #nameKey()} and {@link #descriptionKey()}.
 */
public record AbilityUnlock(
        ProgressionClass cls,
        int requiredLevel,
        String abilityId,
        String displayName,
        String description,
        boolean milestone,
        @Nullable String requires
) {
    public static final String LESSER_FORM = "lesser_form";
    public static final String DOMAIN_SENSE = "domain_sense";
    public static final String GUARDIANS_HIDE = "guardians_hide";
    public static final String INTERMEDIATE_FORM = "intermediate_form";
    public static final String DOMAIN_AUTHORITY = "domain_authority";
    public static final String SOVEREIGN_FORM = "sovereign_form";
    public static final String APEX_DOMAIN = "apex_domain";
    public static final String HEAVENLY_MOBILITY = "heavenly_mobility";

    private static final List<AbilityUnlock> REGISTRY = new ArrayList<>();

    static {
        // ── DIVINE BEAST ──────────────────────────────────────────────────────
        milestone(ProgressionClass.DIVINE_BEAST, 1, LESSER_FORM, "Lesser Form",
                "Free milestone. The beast's first shape, worn from the day you awaken.");
        passive(ProgressionClass.DIVINE_BEAST, 20, DOMAIN_SENSE, "Domain Sense",
                "Hostile creatures that set their sights on you within 24 blocks glow for you alone.", null);
        passive(ProgressionClass.DIVINE_BEAST, 30, GUARDIANS_HIDE, "Guardian's Hide",
                "+10% physical and magic resistance.", null);
        milestone(ProgressionClass.DIVINE_BEAST, 40, INTERMEDIATE_FORM, "Intermediate Form",
                "Free milestone. The lesser shell is shed: your true beast form.");
        passive(ProgressionClass.DIVINE_BEAST, 60, DOMAIN_AUTHORITY, "Domain Authority",
                "Hostile creatures below your level within 8 blocks are slowed and weakened.", null);
        milestone(ProgressionClass.DIVINE_BEAST, 80, SOVEREIGN_FORM, "Sovereign Form",
                "Free milestone. Heaven has written your name: the sovereign beast.");
        passive(ProgressionClass.DIVINE_BEAST, 100, APEX_DOMAIN, "Apex Domain",
                "Your domain reaches 16 blocks, and the creatures it holds deal 20% less damage.",
                DOMAIN_AUTHORITY);

        // ── HEAVENLY ──────────────────────────────────────────────────────────
        passive(ProgressionClass.HEAVENLY, 10, HEAVENLY_MOBILITY, "Heavenly Mobility",
                "A cloud to fly on: sprint while flying to ride it. Held at full speed, it builds to 100 blocks a second.", null);
    }

    private static void milestone(ProgressionClass cls, int level, String id, String name, String description) {
        REGISTRY.add(new AbilityUnlock(cls, level, id, name, description, true, null));
    }

    private static void passive(ProgressionClass cls, int level, String id, String name, String description,
                                @Nullable String requires) {
        REGISTRY.add(new AbilityUnlock(cls, level, id, name, description, false, requires));
    }

    /**
     * Skill points to buy it: 1 below level 50, 2 from level 50, 3 at level 100, and nothing for
     * a milestone, which is not bought. Points come one every 5 levels (20 in all) and the skill
     * trees want about twice that, so these stay cheap beside a tree node, while the late, big
     * ones cost more.
     */
    public int cost() {
        return milestone ? 0 : costAt(requiredLevel);
    }

    /** What an entry bought at this level costs: the scheme {@link RetiredUnlocks} refunds by, too. */
    public static int costAt(int requiredLevel) {
        return 1 + requiredLevel / 50;
    }

    /** Whether it is bought with points (a milestone is not). */
    public boolean purchasable() {
        return !milestone;
    }

    /** Whether a player of this level has reached it (for a milestone, whether it is theirs). */
    public boolean reachedAt(int level) {
        return level >= requiredLevel;
    }

    /** Divine Beast entries are passives; the Heavenly cloud is still called an ability. */
    public boolean isPassive() {
        return cls == ProgressionClass.DIVINE_BEAST;
    }

    public String nameKey() {
        return "class_unlock.jttw_engine." + abilityId;
    }

    public String descriptionKey() {
        return nameKey() + ".desc";
    }

    public Component name() {
        return Component.translatableWithFallback(nameKey(), displayName);
    }

    public Component describe() {
        return Component.translatableWithFallback(descriptionKey(), description);
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    /** The unlock with this ability id, or null. */
    @Nullable
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
