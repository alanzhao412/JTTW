package com.jttw.engine.transformation;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.jttw.engine.transformation.FormCategory.*;

/**
 * The three transformation arts, each with its own menu and its own forms per level (1-3).
 * A character practises at most one; its level comes from that character's skill tree.
 *
 * <pre>
 *            level 1                      level 2                  level 3
 * 18   neutral land                 + neutral water          + neutral air
 * 36   neutral land, water          + neutral air            + hostile land
 * 72   neutral land, water, air     + hostile land           + bosses, hostile sea and sky,
 *                                                              players, objects
 * </pre>
 */
public enum TransformationSchool {

    EIGHTEEN(18, 0xFF8FBF6A,
            EnumSet.of(NEUTRAL_LAND),
            EnumSet.of(NEUTRAL_WATER),
            EnumSet.of(NEUTRAL_AIR)),

    THIRTY_SIX(36, 0xFF5FA8D8,
            EnumSet.of(NEUTRAL_LAND, NEUTRAL_WATER),
            EnumSet.of(NEUTRAL_AIR),
            EnumSet.of(HOSTILE_LAND)),

    SEVENTY_TWO(72, 0xFFE0B040,
            EnumSet.of(NEUTRAL_LAND, NEUTRAL_WATER, NEUTRAL_AIR),
            EnumSet.of(HOSTILE_LAND),
            EnumSet.of(BOSS, HOSTILE_WATER, HOSTILE_AIR, PLAYER, OBJECT));

    public static final int MAX_LEVEL = 3;

    /** 18, 36 or 72. */
    public final int count;
    /** Accent colour of this school's menu (ARGB). */
    public final int color;
    /** Categories each level adds, index 0 = level 1. */
    private final List<Set<FormCategory>> added;

    TransformationSchool(int count, int color, Set<FormCategory> l1, Set<FormCategory> l2, Set<FormCategory> l3) {
        this.count = count;
        this.color = color;
        this.added = List.of(l1, l2, l3);
    }

    public String title() {
        return count + " Transformations";
    }

    /** Every category open at {@code level} (0 or less: none). */
    public Set<FormCategory> allowed(int level) {
        Set<FormCategory> out = EnumSet.noneOf(FormCategory.class);
        for (int l = 1; l <= Math.min(level, MAX_LEVEL); l++) out.addAll(added.get(l - 1));
        return out;
    }

    public boolean allows(FormCategory category, int level) {
        return allowed(level).contains(category);
    }

    /** The level at which {@code category} opens in this school, or 0 if it never does. */
    public int levelFor(FormCategory category) {
        for (int l = 1; l <= MAX_LEVEL; l++) if (added.get(l - 1).contains(category)) return l;
        return 0;
    }

    /** Every category this school ever offers, in menu order. */
    public List<FormCategory> categories() {
        return allowed(MAX_LEVEL).stream().sorted().toList();
    }
}
