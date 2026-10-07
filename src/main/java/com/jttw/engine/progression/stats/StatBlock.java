package com.jttw.engine.progression.stats;

import com.jttw.engine.progression.level.LevelConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Computed stat snapshot for a player at a specific class and level.
 *
 * <p><b>Formula (Pokémon-derived):</b>
 * <pre>
 *   rawHP   = floor(2 × baseHP   × level / 100) + level + 10
 *   rawStat = floor(2 × baseStat × level / 100) + 5       (STR, DEF, QI, WILL)
 * </pre>
 *
 * <p>Raw values are converted to Minecraft attributes via the scale constants below.
 * Tune those constants to adjust overall power level without touching the formula.
 *
 * <p><b>Class late-game bonuses:</b>
 * <ul>
 *   <li>PRIMORDIAL   — +1.5% all stats per level above 50 (up to +75% at level 100).</li>
 *   <li>DIVINE_BEAST — +16% all stats per power spike milestone passed (20/40/60/80/100).</li>
 *   <li>YAOGUAI / HEAVENLY — 1.0× (power comes from abilities, not raw stats).</li>
 * </ul>
 */
public record StatBlock(
        double maxHealth,
        double attackDamage,
        double armor,
        double qi,
        double will
) {

    // -----------------------------------------------------------------------
    // Scale constants — tune these to adjust MC attribute output / internal stats
    // -----------------------------------------------------------------------

    /** Each raw HP unit → this many MC health points added on top of the base 20. */
    private static final double HP_SCALE = 0.145;

    /** Each raw STR unit → this much MC attack damage added on top of base 1.0. */
    private static final double STR_SCALE = 0.072;

    /** Each raw DEF unit → this much MC armor value (uncapped). */
    private static final double DEF_SCALE = 0.045;

    /**
     * Each raw QI unit → internal qi points (not a MC attribute).
     * Tune based on how often you want abilities / flight / techniques to be usable.
     */
    private static final double QI_SCALE = 18.35;

    /**
     * Each raw WILL unit → internal will points (not a MC attribute).
     * Tune based on how strong resist / control / domain effects should be.
     */
    private static final double WILL_SCALE = 0.10;

    // -----------------------------------------------------------------------
    // Class bonus constants
    // -----------------------------------------------------------------------

    /** Primordial: % bonus per level above 50.  Max (+75%) at level 100. */
    private static final double PRIMORDIAL_LATE_RAMP = 0.015;

    /** Divine Beast: % bonus per power spike milestone passed (20, 40, 60, 80, 100). */
    private static final double DIVINE_BEAST_SPIKE_BONUS = 0.16;
    private static final int[] DIVINE_BEAST_SPIKES = {20, 40, 60, 80, 100};

    // -----------------------------------------------------------------------
    // Attribute modifier UUIDs — must be stable across sessions
    // -----------------------------------------------------------------------

    public static final UUID UUID_HP  = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567800");
    public static final UUID UUID_STR = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567801");
    public static final UUID UUID_DEF = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567802");

    private static final String MOD_HP  = "jttw:level_hp";
    private static final String MOD_STR = "jttw:level_str";
    private static final String MOD_DEF = "jttw:level_def";

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    public static StatBlock compute(ProgressionClass cls, int level) {
        int lv = Math.max(1, Math.min(level, LevelConstants.MAX_LEVEL));
        BaseStats base = BaseStats.forClass(cls);

        // Pokémon-style raw values
        double rawHP   = 2.0 * base.hp()   * lv / 100.0 + lv + 10;
        double rawStr  = 2.0 * base.str()  * lv / 100.0 + 5;
        double rawDef  = 2.0 * base.def()  * lv / 100.0 + 5;
        double rawQi   = 2.0 * base.qi()   * lv / 100.0 + 5;
        double rawWill = 2.0 * base.will() * lv / 100.0 + 5;

        double bonus = computeBonus(cls, lv);

        return new StatBlock(
                20.0 + rawHP   * HP_SCALE   * bonus, // base 20 + earned health
                 1.0 + rawStr  * STR_SCALE  * bonus, // base 1 + earned attack
                       rawDef  * DEF_SCALE  * bonus, // no base armor; fully earned
                       rawQi   * QI_SCALE   * bonus, // internal stat
                       rawWill * WILL_SCALE * bonus  // internal stat
        );
    }

    private static double computeBonus(ProgressionClass cls, int level) {
        return switch (cls) {
            case PRIMORDIAL -> {
                if (level <= 50) yield 1.0;
                yield 1.0 + (level - 50) * PRIMORDIAL_LATE_RAMP;
            }
            case DIVINE_BEAST -> {
                int spikes = 0;
                for (int spike : DIVINE_BEAST_SPIKES) if (level >= spike) spikes++;
                yield 1.0 + spikes * DIVINE_BEAST_SPIKE_BONUS;
            }
            default -> 1.0; // YAOGUAI & HEAVENLY — power comes from abilities, not stats
        };
    }

    // -----------------------------------------------------------------------
    // Attribute application (only applies real MC attributes)
    // -----------------------------------------------------------------------

    /**
     * Apply this stat block to the player, replacing any previous JTTW level modifiers.
     * Safe to call every level-up and on login.
     */
    public void applyTo(ServerPlayer player) {
        applyMod(player, Attributes.MAX_HEALTH,    UUID_HP,  MOD_HP,  maxHealth - 20.0);
        applyMod(player, Attributes.ATTACK_DAMAGE, UUID_STR, MOD_STR, attackDamage - 1.0);
        applyMod(player, Attributes.ARMOR,         UUID_DEF, MOD_DEF, armor);

        // Clamp current HP to new max (avoids floating above max health after demotion)
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth((float) player.getMaxHealth());
        }

        // NOTE: qi and will are intentionally NOT applied as MC attributes here.
        // Use these values in your ability / technique / cooldown / resistance systems.
    }

    /**
     * Remove all JTTW level stat modifiers.  Call on class reset or if the player
     * loses their class for any reason.
     */
    public static void removeFrom(ServerPlayer player) {
        removeMod(player, Attributes.MAX_HEALTH,    UUID_HP);
        removeMod(player, Attributes.ATTACK_DAMAGE, UUID_STR);
        removeMod(player, Attributes.ARMOR,         UUID_DEF);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static void applyMod(ServerPlayer player,
                                 net.minecraft.world.entity.ai.attributes.Attribute attr,
                                 UUID uuid, String name, double value) {
        var inst = player.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(uuid);
        if (value != 0.0) {
            inst.addPermanentModifier(new AttributeModifier(uuid, name, value,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    private static void removeMod(ServerPlayer player,
                                  net.minecraft.world.entity.ai.attributes.Attribute attr,
                                  UUID uuid) {
        var inst = player.getAttribute(attr);
        if (inst != null) inst.removeModifier(uuid);
    }
}