package com.jttw.engine.progression;

/**
 * Form evolution stages for the Divine Beast class.
 *
 * <p>Divine Beasts progress through five form phases, each granting a +16% stat bonus
 * and a new identity. Aligned with the spike milestones at levels 20/40/60/80/100.
 *
 * <p><b>Progression lines:</b>
 * <ul>
 *   <li>Water: Water Spirit → River Guardian → Dragon → Dragon Prince → Dragon King</li>
 *   <li>Earth: Earth Spirit → Stone Sentinel → Mountain Guardian → Earth Titan → World Turtle</li>
 *   <li>Beast: Beast Spirit → Western Guardian → White Tiger → Celestial White Tiger → Byakko</li>
 *   <li>Fire:  Fire Spirit → Flame Guardian → Phoenix → Celestial Phoenix → Solar Sovereign</li>
 * </ul>
 *
 * <p><b>Mining tiers (bare-handed):</b>
 * Spirit = wood, Guardian = iron, True Form = diamond, Celestial+ = netherite.
 *
 * <p><b>Stage transitions fire automatically in {@link LevelManager} on level-up.</b>
 * Wire visual/model changes by checking {@link PlayerLevel#getFormStage(net.minecraft.world.entity.player.Player)}
 * in your rendering or transformation systems.
 */
public enum FormStage {

    /** Levels 1–19: Nascent spirit form. Mining tier: wood. */
    SPIRIT(0, 0, 0,
            "Spirit Form",
            "You stir, young beast. The land begins to recognise your presence."),

    /** Levels 20–39: Guardian form. Mining tier: iron. */
    GUARDIAN(1, 20, 2,
            "Guardian Form",
            "Your domain calls to you. You answer as its guardian."),

    /** Levels 40–59: True beast form. Mining tier: diamond. */
    TRUE_FORM(2, 40, 3,
            "True Form",
            "You have shed your lesser shell. Your true nature is revealed."),

    /** Levels 60–79: Celestial form. Mining tier: netherite. */
    CELESTIAL(3, 60, 4,
            "Celestial Form",
            "The heavens tremble. Your power can no longer be ignored."),

    /** Levels 80+: Sovereign apex. Mining tier: netherite. */
    SOVEREIGN(4, 80, 4,
            "Sovereign Form",
            "Heaven has written your name. You are legend.");

    /** Index stored in {@link PlayerLevel#getFormStage}. */
    public final int stageIndex;

    /** Minimum level required to enter this stage. */
    public final int requiredLevel;

    /**
     * Harvest level for bare-handed mining.
     * 0 = wood/gold, 1 = stone, 2 = iron, 3 = diamond, 4 = netherite.
     */
    public final int harvestLevel;

    public final String displayName;
    public final String transitionMessage;

    FormStage(int stageIndex, int requiredLevel, int harvestLevel,
              String displayName, String transitionMessage) {
        this.stageIndex = stageIndex;
        this.requiredLevel = requiredLevel;
        this.harvestLevel = harvestLevel;
        this.displayName = displayName;
        this.transitionMessage = transitionMessage;
    }

    /**
     * Returns the highest form stage the player has earned at the given level.
     * Used by {@link LevelManager} to check whether a stage transition should fire.
     */
    public static FormStage forLevel(int level) {
        FormStage result = SPIRIT;
        for (FormStage stage : values()) {
            if (level >= stage.requiredLevel) result = stage;
        }
        return result;
    }

    /**
     * Returns the form stage corresponding to a stored stage index.
     * Falls back to LESSER for unknown values.
     */
    public static FormStage fromIndex(int index) {
        for (FormStage stage : values()) {
            if (stage.stageIndex == index) return stage;
        }
        return SPIRIT;
    }
}