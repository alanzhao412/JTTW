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
 * <p><b>Each class rides it in its own shape</b> (every class is 1 at level 1), so that at 100, by
 * stats (with each class's base stats), Primordial &gt; Divine Beast &gt; Heavenly &gt; Yaoguai. Measured in
 * "power levels" (the backbone level as strong), the four shapes read as their names:
 * <ul>
 *   <li><b>Heavenly</b>: linear, the backbone itself, steady. Its edge is utility and its own items.</li>
 *   <li><b>Yaoguai</b>: logarithmic. Quick early and flattening late: part steady, part a log curve
 *       over the whole climb ({@value #YAOGUAI_LOG_WEIGHT} of it, bent by {@value #YAOGUAI_LOG_BEND}),
 *       some eight levels ahead from 20 to 40, and {@value #YAOGUAI_BEHIND} behind the backbone at 100.</li>
 *   <li><b>Primordial</b>: exponential. Weak for long, then an ever faster climb: its growth per
 *       level rises from {@value #PRIMORDIAL_START}x to {@value #PRIMORDIAL_END}x along a square,
 *       so seven to nine levels behind from 30 to 60, level with the backbone near 84, and some
 *       twelve levels past it at 100.</li>
 *   <li><b>Divine Beast</b>: staggered. Each evolution (20, 40, 60, 80, 100) is a leap of
 *       {@value #DIVINE_LEAP}x (some thirteen levels), landing {@value #DIVINE_AHEAD} levels ahead of
 *       the backbone; the stage's growth then slows, so it falls eight behind by the next. The last,
 *       at 100, lands level with the backbone, from thirteen behind.</li>
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

    /** Primordial's growth per level at level 2 and at level 100, and how late it rises between (a square). */
    public static final double PRIMORDIAL_START = 1.07, PRIMORDIAL_END = 1.20, PRIMORDIAL_RISE = 2;

    /** Yaoguai: how much of its climb is a log curve rather than steady, and how sharply that curve bends. */
    public static final double YAOGUAI_LOG_WEIGHT = 0.35, YAOGUAI_LOG_BEND = 0.1;
    /** How many backbone levels behind Yaoguai end at the cap: the frailest class by stats there. */
    public static final double YAOGUAI_BEHIND = 4;

    /** Divine Beast: the levels it evolves at, and how much each evolution multiplies its power. */
    private static final int[] EVOLUTIONS = {20, 40, 60, 80, 100};
    public static final double DIVINE_LEAP = 3.45;
    /** How many backbone levels ahead each evolution lands it. */
    public static final double DIVINE_AHEAD = 5;
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
                            + Math.log(PRIMORDIAL_START + (PRIMORDIAL_END - PRIMORDIAL_START) * Math.pow((lv - 2) / (double) (MAX - 2), PRIMORDIAL_RISE));
                    case DIVINE_BEAST -> divine(lv, step);
                    case YAOGUAI -> step * ((1 - YAOGUAI_LOG_WEIGHT) * (lv - 1)
                            + YAOGUAI_LOG_WEIGHT * (MAX - 1) * Math.log(1 + YAOGUAI_LOG_BEND * (lv - 1)) / Math.log(1 + YAOGUAI_LOG_BEND * (MAX - 1))
                            - YAOGUAI_BEHIND * (lv - 1) / (MAX - 1));
                    default -> (lv - 1) * step;
                };
            }
        }
    }

    /**
     * Divine Beast's ln(power): each evolution lands it {@link #DIVINE_AHEAD} backbone levels ahead (the
     * last, at the cap, level with it); within a stage, log-shaped growth to the leap's size short of
     * where the next evolution lands.
     */
    private static double divine(int lv, double step) {
        double leap = Math.log(DIVINE_LEAP), ahead = DIVINE_AHEAD * step;
        int stageStart = 1;
        for (int evolution : EVOLUTIONS) {
            double start = stageStart == 1 ? 0 : (stageStart - 1) * step + ahead;
            double lands = evolution == MAX ? 0 : ahead; // the last lands level with the backbone, at the cap
            double end = (evolution - 1) * step + lands - leap; // the level before it: the leap's size short
            if (lv < evolution) {
                int length = evolution - 1 - stageStart, t = lv - stageStart;
                return start + (end - start) * Math.log(1 + DIVINE_BEND * t) / Math.log(1 + DIVINE_BEND * length);
            }
            stageStart = evolution;
        }
        return (MAX - 1) * step;
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
        return of(cls, level) * measure(who, clamp(level));
    }

    /**
     * A character's own measure over its class at a level, as a multiplier. Most build up evenly to
     * their {@link #characterFactor} at 100. The three heads of the Primordials take turns instead,
     * each by so many backbone levels over their class:
     * <ul>
     *   <li><b>Peng</b> leads the early climb: some {@value #PENG_LEAD} levels ahead about level
     *       {@value #PENG_PEAK}, gone by the top;</li>
     *   <li><b>Yuan Di</b> the middle: some {@value #YUAN_DI_LEAD} levels ahead about level
     *       {@value #YUAN_DI_PEAK} (passing Peng near 44), settling to his measure at the top;</li>
     *   <li><b>Wukong</b> is the steepest of all: {@value #WUKONG_LAG} levels behind his class, the
     *       weakest character there is until the 80s, until his surge from level {@value #WUKONG_SURGE}
     *       over {@value #WUKONG_SURGE_LENGTH} levels: past everyone by the 90s, and at 100, at his
     *       measure of 3, far the strongest of all (the furnace's boon besides).</li>
     * </ul>
     */
    public static double measure(@Nullable ReincarnationOutcome who, int level) {
        int lv = clamp(level);
        double t = (lv - 1) / (double) (MAX - 1);
        if (who == null) return 1;
        double levels = switch (who) {
            case GOLD_WINGED_PENG -> lead(lv, PENG_LEAD, PENG_PEAK, PENG_WIDTH);
            case YUAN_DI -> lead(lv, YUAN_DI_LEAD, YUAN_DI_PEAK, YUAN_DI_WIDTH) + inLevels(characterFactor(who)) * Math.pow(t, 6);
            case CELESTIAL_PRIMATE -> {
                double x = Math.max(0, Math.min(1, (lv - WUKONG_SURGE) / (double) WUKONG_SURGE_LENGTH));
                yield -WUKONG_LAG * Math.min(1, (lv - 1) / 10.0) + (WUKONG_LAG + inLevels(characterFactor(who))) * x * x * (3 - 2 * x);
            }
            default -> inLevels(characterFactor(who)) * t;
        };
        return Math.pow(GROWTH, levels);
    }

    /** Peng's lead over his class, in levels, and where and how broadly it peaks. */
    public static final double PENG_LEAD = 4, PENG_PEAK = 30, PENG_WIDTH = 22;
    /** Yuan Di's. */
    public static final double YUAN_DI_LEAD = 4, YUAN_DI_PEAK = 55, YUAN_DI_WIDTH = 18;
    /** How many levels Wukong trails his class until his surge, from what level it rises, and over how many. */
    public static final double WUKONG_LAG = 10;
    public static final int WUKONG_SURGE = 75, WUKONG_SURGE_LENGTH = 25;

    private static double inLevels(double factor) {
        return Math.log(factor) / Math.log(GROWTH);
    }

    /** A lead of {@code levels} peaking at {@code peak}: none at level 1 nor at the top. */
    private static double lead(int lv, double levels, double peak, double width) {
        double ends = Math.min(1, Math.min((lv - 1) / 10.0, (MAX - lv) / 10.0));
        return levels * Math.exp(-Math.pow((lv - peak) / width, 2)) * ends;
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
     * A character's own measure over its class at level 100 ({@link #measure} on the way there):
     * Wukong, Yuan Di and Peng at the head of the Primordials, the Nine-Headed Lion far above the
     * other Yaoguai, Marshal below the rest of the Heavenly.
     */
    public static double characterFactor(@Nullable ReincarnationOutcome who) {
        if (who == null) return 1;
        return switch (who) {
            case CELESTIAL_PRIMATE -> 3.0;
            case YUAN_DI -> 1.12;
            case GRAND_SAINT_NINE_SPIRITS -> 4.7; // his class ends behind the backbone; he does not
            case MARSHAL_HEAVENLY_CANOPY -> 0.7;
            default -> 1;
        };
    }

    @Nullable
    private static ReincarnationOutcome outcome(Player p) {
        return ReincarnationOutcome.byId(p.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME));
    }
}
