package com.jttw.engine.progression;

public final class LevelConstants {
    private LevelConstants() {}

    public static final int MAX_LEVEL = 100;

    /**
     * Vanilla mob kills grant XP only while the player is below this level.
     * At this threshold, progression shifts to structured rare biome grinding.
     */
    public static final int VANILLA_CAP_LEVEL = 20;

    // -----------------------------------------------------------------------
    // NBT keys — all scoped under the player's persistent data
    // -----------------------------------------------------------------------
    static final String KEY_LEVEL     = "jttw_level";
    static final String KEY_XP        = "jttw_xp";
    static final String KEY_CLASS     = "jttw_prog_class";
    static final String KEY_ABILITIES = "jttw_abilities";
    static final String KEY_FORM      = "jttw_form_stage";
    static final String KEY_BEAST_LINE = "jttw_beast_line";
}