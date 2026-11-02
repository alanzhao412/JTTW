package com.jttw.engine.progression.stats;

import com.jttw.engine.item.ArmamentBonuses;
import com.jttw.engine.item.ArmamentTiers;
import com.jttw.engine.item.Armory;
import com.jttw.engine.reincarnation.ReincarnationOutcome;

/**
 * Each character's overall power: their stats ({@link StatBlock}, on their {@link LevelPower} curve)
 * times their <b>kit</b>, what their class's way of fighting adds when played as it is meant to be.
 * The measure the class identities are balanced by; nothing in play reads it.
 *
 * <ul>
 *   <li><b>Primordial</b>: normal gameplay, kit x1.</li>
 *   <li><b>Divine Beast</b>: their power is their evolutions, in their stats; kit x1 (the Phoenix's
 *       abilities, x{@value AbilityDamage#PHOENIX_ABILITY}, on its casting share).</li>
 *   <li><b>Heavenly</b>: their armaments, every one at their ring's tier (attunement x1): the armory's
 *       {@value Armory#PER_ARMAMENT_PERCENT}% each on health and attack, and their best weapon's own bonus at
 *       that tier on attack.</li>
 *   <li><b>Yaoguai</b>: casting: {@value #CAST_SHARE} of their damage from abilities at
 *       x{@value AbilityDamage#YAOGUAI_ABILITY}, the rest from their weapon at x{@value AbilityDamage#YAOGUAI_WEAPON}.</li>
 * </ul>
 *
 * <p>Overall power is the geometric mean of effective health and attack (a fight is won by both), read in
 * "power levels" against a plain Heavenly character (the steady backbone): how many levels ahead
 * or behind it, at the same level, a character stands.
 */
public final class ClassKit {

    private ClassKit() {}

    /** The share of a caster's damage that comes from abilities, played as meant (cast on every cooldown). */
    public static final double CAST_SHARE = 0.55;

    /** How many armaments each Heavenly character keeps. */
    public static int armaments(ReincarnationOutcome who) {
        return switch (who) {
            case THIRD_LOTUS_PRINCE -> 6;
            case EYE_OF_HEAVEN -> 3;
            case MARSHAL_HEAVENLY_CANOPY -> 1;
            default -> 0;
        };
    }

    /** An armament's own bonus at a tier, from its tier-I bonus. */
    public static double atTier(double bonus, int tier) {
        int t = Math.max(1, Math.min(ArmamentTiers.TIERS, tier));
        return bonus * (1 + (t - 1) * ArmamentTiers.PER_TIER_PERCENT / 100.0);
    }

    /** A Heavenly character's best weapon in hand at a tier, as what it multiplies attack by. */
    public static double weapon(ReincarnationOutcome who, int tier) {
        return switch (who) {
            case THIRD_LOTUS_PRINCE -> Math.max(1 + atTier(ArmamentBonuses.FIRE_TIP_SPEAR, tier),
                    Math.pow(1 + atTier(ArmamentBonuses.YIN_YANG_SWORD, tier), 2));
            case EYE_OF_HEAVEN -> 1 + atTier(Math.max(ArmamentBonuses.ERLANG_AXE, ArmamentBonuses.THREE_POINTED_SPEAR), tier);
            case MARSHAL_HEAVENLY_CANOPY -> 1 + atTier(ArmamentBonuses.NINE_TOOTH_RAKE, tier);
            default -> 1;
        };
    }

    /** What the kit multiplies attack by at a level. */
    public static double attack(ProgressionClass cls, ReincarnationOutcome who, int level) {
        return switch (cls) {
            case HEAVENLY -> (1 + armaments(who) * Armory.PER_ARMAMENT_PERCENT / 100.0) * weapon(who, ArmamentTiers.ringTier(level));
            case YAOGUAI -> CAST_SHARE * AbilityDamage.YAOGUAI_ABILITY + (1 - CAST_SHARE) * AbilityDamage.YAOGUAI_WEAPON;
            case DIVINE_BEAST -> who == ReincarnationOutcome.PHOENIX ? CAST_SHARE * AbilityDamage.PHOENIX_ABILITY + (1 - CAST_SHARE) : 1;
            default -> 1;
        };
    }

    /** What the kit multiplies health by at a level. */
    public static double health(ProgressionClass cls, ReincarnationOutcome who, int level) {
        return cls == ProgressionClass.HEAVENLY ? 1 + armaments(who) * Armory.PER_ARMAMENT_PERCENT / 100.0 : 1;
    }

    /** The kit alone, as one multiplier on overall power. */
    public static double kit(ReincarnationOutcome who, int level) {
        ProgressionClass cls = ProgressionClass.fromOutcome(who);
        return Math.sqrt(attack(cls, who, level) * health(cls, who, level));
    }

    /** Stats alone: the geometric mean of health and attack. */
    public static double byStats(ReincarnationOutcome who, int level) {
        StatBlock s = StatBlock.compute(ProgressionClass.fromOutcome(who), who, level);
        return Math.sqrt(s.maxHealth() * s.attackDamage());
    }

    /** Overall: stats times kit. */
    public static double overall(ReincarnationOutcome who, int level) {
        return byStats(who, level) * kit(who, level);
    }

    /** The steady backbone's overall at a level: a plain Heavenly character's stats. */
    public static double backbone(int level) {
        StatBlock s = StatBlock.compute(ProgressionClass.HEAVENLY, null, level);
        return Math.sqrt(s.maxHealth() * s.attackDamage());
    }

    /** A number as power levels (x{@value LevelPower#GROWTH} each) over or under the backbone. */
    public static double levels(double overThe) {
        return Math.log(overThe) / Math.log(LevelPower.GROWTH);
    }

    /** Overall power in power levels against the backbone at the same level. */
    public static double overallLevels(ReincarnationOutcome who, int level) {
        return levels(overall(who, level) / backbone(level));
    }

    /** Stats alone in power levels against the backbone. */
    public static double statLevels(ReincarnationOutcome who, int level) {
        return levels(byStats(who, level) / backbone(level));
    }
}
