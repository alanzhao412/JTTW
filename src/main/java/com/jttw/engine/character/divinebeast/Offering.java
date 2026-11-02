package com.jttw.engine.character.divinebeast;

import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.registry.ModItems;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;

/**
 * The Divine Beasts' evolution offerings: one for each beast and each evolution (20, 40, 60, 80,
 * 100), crafted at a crafting table (a gated recipe: that beast alone) from the materials of the part
 * of the world that leads to it, and burned at the Altar of the Four Symbols ({@link BeastEvolution}).
 * Their recipes are data ({@code data/jttw_engine/recipe/offering/}).
 */
public enum Offering {
    // the Vermilion Bird: feathers and fire
    KINDLED_PLUME(DivineBeastLine.FIRE, 20, "Kindled Plume"),
    WISP_FIRE_PLUME(DivineBeastLine.FIRE, 40, "Wisp-Fire Plume"),
    CANDLE_FLAME_PLUME(DivineBeastLine.FIRE, 60, "Candle-Flame Plume"),
    SAMADHI_PLUME(DivineBeastLine.FIRE, 80, "Samadhi Plume"),
    SOVEREIGN_SUN_PLUME(DivineBeastLine.FIRE, 100, "Sovereign Sun Plume"),
    // the Azure Dragon: pearls and water
    RAIN_PEARL(DivineBeastLine.WATER, 20, "Rain Pearl"),
    WATER_CURTAIN_PEARL(DivineBeastLine.WATER, 40, "Water-Curtain Pearl"),
    FOUR_SEAS_PEARL(DivineBeastLine.WATER, 60, "Pearl of the Four Seas"),
    NAIHE_PEARL(DivineBeastLine.WATER, 80, "Naihe Pearl"),
    DRAGON_KINGS_PEARL(DivineBeastLine.WATER, 100, "Dragon King's Pearl"),
    // the White Tiger: fang, pelt and mane
    FANG_AND_PELT_TOTEM(DivineBeastLine.BEAST, 20, "Fang-and-Pelt Totem"),
    BONE_FANG_TOTEM(DivineBeastLine.BEAST, 40, "Bone-Fang Totem"),
    MOONLIT_WHISKER_TOTEM(DivineBeastLine.BEAST, 60, "Moonlit Whisker Totem"),
    OX_AND_HORSE_TOTEM(DivineBeastLine.BEAST, 80, "Ox-and-Horse Totem"),
    WESTERN_MANE_TOTEM(DivineBeastLine.BEAST, 100, "Western Mane Totem"),
    // the Black Tortoise: shell and earth
    DEEPSTONE_SHELL(DivineBeastLine.EARTH, 20, "Deepstone Shell"),
    RIDGE_BONE_SHELL(DivineBeastLine.EARTH, 40, "Ridge-Bone Shell"),
    TIDE_WORN_SHELL(DivineBeastLine.EARTH, 60, "Tide-Worn Shell"),
    FURNACE_BRICK_SHELL(DivineBeastLine.EARTH, 80, "Furnace-Brick Shell"),
    WORLD_TURTLE_SHELL(DivineBeastLine.EARTH, 100, "World-Turtle Shell");

    public final DivineBeastLine line;
    /** The evolution it is burned for. */
    public final int level;
    public final String displayName;

    Offering(DivineBeastLine line, int level, String displayName) {
        this.line = line;
        this.level = level;
        this.displayName = displayName;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public Item item() {
        return ModItems.OFFERINGS.get(this).get();
    }

    /** The beast that burns it. */
    public ReincarnationOutcome beast() {
        return switch (line) {
            case FIRE -> ReincarnationOutcome.PHOENIX;
            case WATER -> ReincarnationOutcome.DRAGON;
            case BEAST -> ReincarnationOutcome.WHITE_TIGER;
            case EARTH -> ReincarnationOutcome.TURTLE;
        };
    }

    /** Where its materials come from: the part of the world on the way to its evolution. */
    public String regions() {
        return switch (level) {
            case 20 -> "the open world: its beasts, its wild yaoguai and its ores";
            case 40 -> "White Bone Ridge, Black Wind Mountain and Flower-Fruit Mountain";
            case 60 -> "the Bottomless Cave and the Mystical Waters";
            case 80 -> "Hell and the Flaming Mountains";
            default -> "Mount Wuzhuang, the Spider Cave and Lion-Camel Ridge";
        };
    }

    /** The offering this beast burns for the evolution at this level, or null. */
    @Nullable
    public static Offering of(@Nullable DivineBeastLine line, int level) {
        for (Offering o : values()) if (o.line == line && o.level == level) return o;
        return null;
    }

    /** The offering this item is, or null. */
    @Nullable
    public static Offering of(Item item) {
        for (Offering o : values()) if (ModItems.OFFERINGS.get(o).get() == item) return o;
        return null;
    }
}
