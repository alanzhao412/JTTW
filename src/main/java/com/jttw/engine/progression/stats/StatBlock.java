package com.jttw.engine.progression.stats;

import net.minecraft.resources.ResourceLocation;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.LevelConstants;
import com.jttw.engine.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Computed stat snapshot for a player at a specific class and level.
 *
 * <p><b>Health and attack: class shape x power.</b> The class's base stats give its shape
 * (its health and attack as the Pokémon-derived formula below puts them at level
 * {@value #SHAPE_LEVEL}); the level gives the power, by the class's and the character's own
 * curve ({@link LevelPower}, about x1.10 a level). Health is
 * set so the frailest class has 20 at level 1, attack so characters of a level take three or four
 * ordinary hits to kill each other. So level 40 one-shots level 20, and 100 one-shots 80.
 *
 * <p><b>Everything else: the Pokémon-derived formula.</b>
 * <pre>
 *   rawHP   = floor(2 × baseHP   × level / 100) + level + 10
 *   rawStat = floor(2 × baseStat × level / 100) + 5       (STR, PRES, MRES, QI, WILL)
 * </pre>
 * Resistance grows by it to level {@value LevelPower#RESISTANCE_LEVELS} and holds there; Qi and
 * Will grow by it to 100. {@link #strength()} is STR by it: the number the wield requirements
 * name ("30 STR") and the Character screen shows; attack is what a blow does.
 *
 * <p>Raw values are converted to Minecraft attributes via the scale constants below.
 * Physical and magic resistance go to the mod's own attributes, not vanilla armor, so
 * they are not capped; see {@link ResistanceHandler} for how they reduce damage.
 * Tune those constants to adjust overall power level without touching the formula.
 *
 * <p><b>Class late-game bonuses</b>, on resistance, Qi, Will and STR (health and attack carry
 * them in the class's power curve instead):
 * <ul>
 *   <li>PRIMORDIAL   — +1.5% per level above 50 (up to +75% at level 100).</li>
 *   <li>DIVINE_BEAST — +16% per evolution passed (20/40/60/80/100).</li>
 *   <li>YAOGUAI / HEAVENLY — 1.0× (power comes from abilities, not raw stats).</li>
 * </ul>
 */
public record StatBlock(
        double maxHealth,
        double attackDamage,
        double physicalResistance,
        double magicResistance,
        double qi,
        double will,
        double strength
) {

    /** The level a class's shape is read at. */
    private static final int SHAPE_LEVEL = 50;
    /** Attack, as a share of the class's attack shape: sets how many hits a same-level fight takes. */
    private static final double ATTACK_SHARE = 0.13;

    // -----------------------------------------------------------------------
    // Scale constants — tune these to adjust MC attribute output / internal stats
    // -----------------------------------------------------------------------

    /** Each raw HP unit → this many MC health points added on top of the base 20. */
    private static final double HP_SCALE = 0.435; // a high-scaling game: x3 the old 0.145

    /** Each raw STR unit → this much MC attack damage added on top of base 1.0. */
    private static final double STR_SCALE = 0.216; // x3 the old 0.072

    /** Each raw PRES / MRES unit → this many resistance points (uncapped). */
    private static final double RES_SCALE = 0.60; // x1.5 the old 0.40

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

    public static final ResourceLocation UUID_HP  = JTTWEngine.id("hp");
    public static final ResourceLocation UUID_STR = JTTWEngine.id("str");
    /** The old DEF → vanilla armor modifier. Only removed now, from players who still carry it. */
    public static final ResourceLocation UUID_DEF = JTTWEngine.id("def");
    public static final ResourceLocation UUID_PRES = JTTWEngine.id("pres");
    public static final ResourceLocation UUID_MRES = JTTWEngine.id("mres");

    private static final String MOD_HP  = "jttw:level_hp";
    private static final String MOD_STR = "jttw:level_str";
    private static final String MOD_PRES = "jttw:level_pres";
    private static final String MOD_MRES = "jttw:level_mres";

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    /** A player's stats: their character's at their level, or null without a class. */
    @javax.annotation.Nullable
    public static StatBlock of(net.minecraft.world.entity.player.Player p) {
        ProgressionClass cls = com.jttw.engine.progression.level.PlayerLevel.getProgressionClass(p);
        if (cls == null) return null;
        var who = com.jttw.engine.reincarnation.ReincarnationOutcome.byId(
                p.getPersistentData().getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME));
        return compute(cls, who, com.jttw.engine.progression.level.PlayerLevel.getLevel(p), com.jttw.engine.world.furnace.WukongTrial.boon(p));
    }

    /** A class's stats at a level, before any one character's measure. */
    public static StatBlock compute(ProgressionClass cls, int level) {
        return compute(cls, null, level);
    }

    /** A character's stats at a level: its class's shape, at its own power (see {@link LevelPower}). */
    public static StatBlock compute(ProgressionClass cls, @javax.annotation.Nullable com.jttw.engine.reincarnation.ReincarnationOutcome who, int level) {
        return compute(cls, who, level, 1);
    }

    /** As above, with a further measure on its power (Wukong's, after the Eight Trigrams Furnace). */
    public static StatBlock compute(ProgressionClass cls, @javax.annotation.Nullable com.jttw.engine.reincarnation.ReincarnationOutcome who, int level, double boon) {
        int lv = Math.max(1, Math.min(level, LevelConstants.MAX_LEVEL));
        BaseStats base = BaseStats.forClass(cls);

        // Pokémon-style raw values
        double rawStr  = 2.0 * base.str()  * lv / 100.0 + 5;
        double rawQi   = 2.0 * base.qi()   * lv / 100.0 + 5;
        double rawWill = 2.0 * base.will() * lv / 100.0 + 5;
        // resistance only grows so far: past it, power alone separates levels
        int rl = Math.min(lv, LevelPower.RESISTANCE_LEVELS);
        double rawPres = 2.0 * base.pres() * rl / 100.0 + 5;
        double rawMres = 2.0 * base.mres() * rl / 100.0 + 5;

        double bonus = computeBonus(cls, lv);
        double resBonus = computeBonus(cls, rl);
        double power = LevelPower.of(cls, who, lv) * boon; // each class's own curve (it carries the class's late growth and leaps)

        return new StatBlock(
                hpShape(base) * HP_NORM * power,                   // class shape x level power
                atkShape(base) * ATTACK_SHARE * power,
                rawPres * RES_SCALE  * resBonus,                   // fully earned, to level 40
                rawMres * RES_SCALE  * resBonus,
                rawQi   * QI_SCALE   * bonus,                      // internal stat
                rawWill * WILL_SCALE * bonus,                      // internal stat
                1.0 + rawStr * STR_SCALE * bonus                   // STR, for wield requirements
        );
    }

    /** A class's health shape: its health at the shape level by the Pokémon-derived formula. */
    private static double hpShape(BaseStats b) {
        return 20.0 + (2.0 * b.hp() * SHAPE_LEVEL / 100.0 + SHAPE_LEVEL + 10) * HP_SCALE;
    }

    /** A class's attack shape: its attack at the shape level by the Pokémon-derived formula. */
    private static double atkShape(BaseStats b) {
        return 1.0 + (2.0 * b.str() * SHAPE_LEVEL / 100.0 + 5) * STR_SCALE;
    }

    /** So that the frailest class has 20 health at level 1, as a vanilla player does. */
    private static final double HP_NORM = 20.0 / hpShape(BaseStats.HEAVENLY);

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
        applyMod(player, ModAttributes.PHYSICAL_RESISTANCE.get(), UUID_PRES, MOD_PRES, physicalResistance);
        applyMod(player, ModAttributes.MAGIC_RESISTANCE.get(),    UUID_MRES, MOD_MRES, magicResistance);
        removeMod(player, Attributes.ARMOR, UUID_DEF); // DEF no longer feeds vanilla armor

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
        removeMod(player, ModAttributes.PHYSICAL_RESISTANCE.get(), UUID_PRES);
        removeMod(player, ModAttributes.MAGIC_RESISTANCE.get(),    UUID_MRES);
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
            inst.addPermanentModifier(new AttributeModifier(uuid, value,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void removeMod(ServerPlayer player,
                                  net.minecraft.world.entity.ai.attributes.Attribute attr,
                                  UUID uuid) {
        var inst = player.getAttribute(attr);
        if (inst != null) inst.removeModifier(uuid);
    }
}