package com.jttw.engine.reincarnation;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Common config for reincarnation.
 */
public final class ReincarnationConfig {
    private ReincarnationConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue THRESHOLD_TOTAL_POINTS;

    /**
     * Entity ids are optional. If set to a valid LivingEntity registry id, the engine will
     * auto-transform the player into that entity on reincarnation.
     */
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_CELESTIAL_PRIMATE;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_YUAN_DI;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_EYE_OF_HEAVEN;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_THIRD_LOTUS_PRINCE;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_MARSHAL_HEAVENLY_CANOPY;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_PHOENIX;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_DRAGON;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_WHITE_TIGER;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_TURTLE;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_BULL_DEMON;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_SAURIAN_DEMON_KING;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_GRAND_SAINT_NINE_SPIRITS;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_GOLD_WINGED_PENG;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("reincarnation");

        THRESHOLD_TOTAL_POINTS = b
                .comment("Total alignment points required before death triggers reincarnation.")
                .defineInRange("thresholdTotalPoints", 300, 0, 1000000);

        b.push("entityMappings");
        b.comment("Optional entity ids for each reincarnation outcome. Leave blank to disable auto-transform.");

        ENTITY_CELESTIAL_PRIMATE = b.define("celestialPrimate", "");
        ENTITY_YUAN_DI = b.define("yuanDi", "");
        ENTITY_EYE_OF_HEAVEN = b.define("eyeOfHeaven", "");
        ENTITY_THIRD_LOTUS_PRINCE = b.define("thirdLotusPrince", "");
        ENTITY_MARSHAL_HEAVENLY_CANOPY = b.define("marshalHeavenlyCanopy", "");
        ENTITY_PHOENIX = b.define("garuda", "");
        ENTITY_DRAGON = b.define("dragon", "");
        ENTITY_WHITE_TIGER = b.define("whiteTiger", "");
        ENTITY_TURTLE = b.define("turtle", "");
        ENTITY_BULL_DEMON = b.define("bullDemon", "");
        ENTITY_SAURIAN_DEMON_KING = b.define("saurianDemonKing", "");
        ENTITY_GRAND_SAINT_NINE_SPIRITS = b.define("grandSaintNineSpirits", "");
        ENTITY_GOLD_WINGED_PENG = b.define("goldWingedPeng", "");

        b.pop();
        b.pop();

        SPEC = b.build();
    }
}
