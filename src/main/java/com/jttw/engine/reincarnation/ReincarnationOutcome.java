package com.jttw.engine.reincarnation;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Final reincarnation forms (primary + secondary alignment).
 */
public enum ReincarnationOutcome {
    // Primordial-dominant
    CELESTIAL_PRIMATE("celestial_primate", ReincarnationConfig.ENTITY_CELESTIAL_PRIMATE),
    YUAN_DI("yuan_di", ReincarnationConfig.ENTITY_YUAN_DI),
    GOLD_WINGED_PENG("gold_winged_peng", ReincarnationConfig.ENTITY_GOLD_WINGED_PENG),

    // Heavenly-dominant
    EYE_OF_HEAVEN("eye_of_heaven", ReincarnationConfig.ENTITY_EYE_OF_HEAVEN),
    THIRD_LOTUS_PRINCE("third_lotus_prince", ReincarnationConfig.ENTITY_THIRD_LOTUS_PRINCE),
    MARSHAL_HEAVENLY_CANOPY("marshal_of_the_heavenly_canopy", ReincarnationConfig.ENTITY_MARSHAL_HEAVENLY_CANOPY),

    // Spirit Beast-dominant
    PHOENIX("phoenix", ReincarnationConfig.ENTITY_PHOENIX),
    DRAGON("dragon", ReincarnationConfig.ENTITY_DRAGON),
    WHITE_TIGER("white_tiger", ReincarnationConfig.ENTITY_WHITE_TIGER),
    TURTLE("turtle", ReincarnationConfig.ENTITY_TURTLE),

    // Yaoguai-dominant
    BULL_DEMON_KING("bull_demon_king", ReincarnationConfig.ENTITY_BULL_DEMON),
    SAURIAN_DEMON_KING("saurian_demon_king", ReincarnationConfig.ENTITY_SAURIAN_DEMON_KING);

    public final String id;
    private final ForgeConfigSpec.ConfigValue<String> entityIdConfig;

    ReincarnationOutcome(String id, ForgeConfigSpec.ConfigValue<String> entityIdConfig) {
        this.id = id;
        this.entityIdConfig = entityIdConfig;
    }

    /** Returns the configured entity id string, or blank if unset. */
    public String configuredEntityId() {
        String v = entityIdConfig.get();
        return v == null ? "" : v.trim();
    }
}
