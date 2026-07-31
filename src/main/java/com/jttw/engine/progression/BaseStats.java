package com.jttw.engine.progression;

/**
 * Per-class base stat distributions.
 *
 * <p>These are "base stats" in the Pokémon sense — raw numbers that feed the
 * scaling formula in {@link StatBlock}, NOT direct Minecraft attribute values.
 *
 * <p><b>Invariant:</b> {@code hp + str + def + qi + will == ProgressionClass.bst}
 *
 * <p>Tuning guide:
 * <ul>
 *   <li>PRIMORDIAL   — high HP and STR, strong WILL, low QI.  Unyielding force.</li>
 *   <li>DIVINE_BEAST — well-rounded with balanced QI and WILL.</li>
 *   <li>HEAVENLY     — high QI and WILL.  Spiritual dominance and mobility.</li>
 *   <li>YAOGUAI      — high STR and QI, low DEF/WILL.  Explosive but unstable.</li>
 * </ul>
 *
 * <p>To rebalance: adjust these numbers and re-check {@link StatBlock}'s scale constants.
 * Keep the invariant (sum == bst) and run {@link StatBlock#compute(ProgressionClass, int)}
 * at levels 1, 20, 50, 100 to see the attribute output.
 */
public record BaseStats(int hp, int str, int def, int qi, int will) {

    // BST 1200 — dominant HP and STR, iron WILL, weaker QI expression.
    public static final BaseStats PRIMORDIAL =
            new BaseStats(300, 300, 220, 180, 200);

    // BST 1100 — balanced domain authority.
    public static final BaseStats DIVINE_BEAST =
            new BaseStats(290, 250, 230, 150, 180);

    // BST 1000 — spiritual mobility and refined control.
    public static final BaseStats HEAVENLY =
            new BaseStats(170, 200, 130, 270, 230);

    // BST 1030 — reckless power, volatile energy.
    public static final BaseStats YAOGUAI =
            new BaseStats(230, 220, 130, 230, 220);

    public static BaseStats forClass(ProgressionClass cls) {
        return switch (cls) {
            case PRIMORDIAL   -> PRIMORDIAL;
            case DIVINE_BEAST -> DIVINE_BEAST;
            case HEAVENLY     -> HEAVENLY;
            case YAOGUAI      -> YAOGUAI;
        };
    }
}