package com.jttw.engine.progression.stats;

import com.jttw.engine.progression.level.LevelConstants;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * How much a level is worth: the power curves the campaign scales by.
 *
 * <p><b>The backbone.</b> A level is {@value #GROWTH}x the one before: twenty levels are about
 * 6.7x, and level 100 is about 12,500x level 1. It is set so that one ordinary hit (a weapon or a
 * basic ability, about 1.5x attack) from a character twenty levels higher kills a typical
 * character: level 20 one-shots level 5, 40 one-shots 20, and on to 100 over 80; characters of a
 * level take three or four such hits to kill each other. The zones' mobs are set against it.
 *
 * <p><b>Each class rides it in its own shape</b> (every class is 1 at level 1):
 * <ul>
 *   <li><b>Yaoguai and Heavenly</b>: the backbone itself, steady. Yaoguai make up for it with
 *       damaging abilities, Heavenly with utility and their own items.</li>
 *   <li><b>Primordial</b>: a slow start and an ever faster climb. Its growth per level rises from
 *       {@value #PRIMORDIAL_START}x to {@value #PRIMORDIAL_END}x: about three quarters of the
 *       backbone through the 20s to 40s, level with it by 60, far past it at 100.</li>
 *   <li><b>Divine Beast</b>: growth that slows within each stage of its evolution, and a leap at
 *       each evolution (20, 40, 60, 80, 100) of about {@value #DIVINE_LEAP}x; across a stage it
 *       keeps pace with the backbone.</li>
 * </ul>
 *
 * <p><b>And each character in its own measure</b>, building over the campaign to its full
 * factor at level 100 ({@link #characterFactor}), so that at the top, by stats, Wukong &gt;= Yuan
 * Di &gt;= Peng &gt;= the Nine-Headed Lion, each far above everyone else.
 *
 * <p>Scaled by a player's power: their health and attack ({@link StatBlock}), every ability's
 * damage, heal and health bonus that is a fixed number at level 1, and companions' stats. Not
 * scaled: resistance (a share of each hit, which stops growing at {@link #RESISTANCE_LEVELS}), Qi,
 * Will, and anything that is a size or a distance.
 */
public final class LevelPower {

    private LevelPower() {}

    /** Each level's power over the one before, on the backbone. */
    public static final double GROWTH = 1.10;

    /** Resistance grows with level only this far: past it, power alone separates levels. */
    public static final int RESISTANCE_LEVELS = 40;

    /** Primordial's growth per level at level 1 and at level 99. */
    public static final double PRIMORDIAL_START = 1.08, PRIMORDIAL_END = 1.145;

    /** Divine Beast: the levels it evolves at, and how much each evolution multiplies its power. */
    private static final int[] EVOLUTIONS = {20, 40, 60, 80, 100};
    public static final double DIVINE_LEAP = 2.12;
    /** How sharply Divine Beast growth slows within a stage: higher, flatter toward its end. */
    private static final double DIVINE_BEND = 0.4;

    private static final int MAX = LevelConstants.MAX_LEVEL;
    /** ln(power) by level, per class: worked out once. */
    private static final double[][] LN = new double[ProgressionClass.values().length][MAX + 1];

    static {
        double step = Math.log(GROWTH);
        for (ProgressionClass cls : ProgressionClass.values()) {
            double[] ln = LN[cls.ordinal()];
            for (int lv = 1; lv <= MAX; lv++) {
                ln[lv] = switch (cls) {
                    case PRIMORDIAL -> lv == 1 ? 0 : ln[lv - 1]
                            + Math.log(PRIMORDIAL_START + (PRIMORDIAL_END - PRIMORDIAL_START) * (lv - 2) / (MAX - 2));
                    case DIVINE_BEAST -> divine(lv, step);
                    default -> (lv - 1) * step;
                };
            }
        }
    }

    /** Divine Beast's ln(power): log-shaped growth within each stage, a leap at each evolution. */
    private static double divine(int lv, double step) {
        double total = 0, leap = Math.log(DIVINE_LEAP);
        int stageStart = 1;
        for (int evolution : EVOLUTIONS) {
            int length = evolution - stageStart;
            double within = length * step - leap; // across the stage, it keeps pace with the backbone
            if (lv < evolution) {
                int t = lv - stageStart;
                return total + within * Math.log(1 + DIVINE_BEND * t) / Math.log(1 + DIVINE_BEND * length);
            }
            total += within + leap;
            stageStart = evolution;
        }
        return total;
    }

    private static int clamp(int level) {
        return Math.max(1, Math.min(level, MAX));
    }

    /** The backbone's power at a level: 1 at level 1. What the zones' mobs are measured by. */
    public static double of(int level) {
        return Math.pow(GROWTH, clamp(level) - 1);
    }

    /** A class's power at a level, before any one character's measure. */
    public static double of(ProgressionClass cls, int level) {
        return Math.exp(LN[cls.ordinal()][clamp(level)]);
    }

    /** A character's power at a level: its class's, in its own measure. */
    public static double of(ProgressionClass cls, @Nullable ReincarnationOutcome who, int level) {
        return of(cls, level) * Math.pow(characterFactor(who), (clamp(level) - 1) / (double) (MAX - 1));
    }

    /** A player's power: their character's at their level (the backbone's for a player without a class). */
    public static double of(Player p) {
        ProgressionClass cls = PlayerLevel.getProgressionClass(p);
        int level = PlayerLevel.getLevel(p);
        return (cls == null ? of(level) : of(cls, outcome(p), level)) * com.jttw.engine.world.furnace.WukongTrial.boon(p);
    }

    /** A number set for level 1, at this player's power: a fixed damage, heal or health bonus. */
    public static float scaled(Player p, double atLevelOne) {
        return (float) (atLevelOne * of(p));
    }

    /**
     * A character's own measure over its class, reached at level 100: Wukong, Yuan Di and Peng
     * at the head of the Primordials, the Nine-Headed Lion far above the other Yaoguai, Marshal
     * below the rest of the Heavenly.
     */
    public static double characterFactor(@Nullable ReincarnationOutcome who) {
        if (who == null) return 1;
        return switch (who) {
            case CELESTIAL_PRIMATE -> 1.25;
            case YUAN_DI -> 1.12;
            case GRAND_SAINT_NINE_SPIRITS -> 3.2;
            case MARSHAL_HEAVENLY_CANOPY -> 0.8;
            default -> 1;
        };
    }

    @Nullable
    private static ReincarnationOutcome outcome(Player p) {
        return ReincarnationOutcome.byId(p.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME));
    }
}
