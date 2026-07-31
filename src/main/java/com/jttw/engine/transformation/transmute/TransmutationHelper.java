package com.jttw.engine.transformation.transmute;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Provides lookup tables for transmutation: tools by tier, crops, and foods.
 */
public final class TransmutationHelper {

    private TransmutationHelper() {}

    // -----------------------------------------------------------------------
    // Tool transmutation
    // -----------------------------------------------------------------------

    /** Ordered tool types within each tier: sword, pickaxe, axe, hoe, shovel. */
    private static final Map<Tier, List<Item>> TOOL_TIERS = new LinkedHashMap<>();

    static {
        TOOL_TIERS.put(Tiers.WOOD, List.of(
                Items.WOODEN_SWORD, Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_HOE, Items.WOODEN_SHOVEL));
        TOOL_TIERS.put(Tiers.STONE, List.of(
                Items.STONE_SWORD, Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_HOE, Items.STONE_SHOVEL));
        TOOL_TIERS.put(Tiers.IRON, List.of(
                Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_HOE, Items.IRON_SHOVEL));
        TOOL_TIERS.put(Tiers.GOLD, List.of(
                Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.GOLDEN_SHOVEL));
        TOOL_TIERS.put(Tiers.DIAMOND, List.of(
                Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_HOE, Items.DIAMOND_SHOVEL));
        TOOL_TIERS.put(Tiers.NETHERITE, List.of(
                Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_HOE, Items.NETHERITE_SHOVEL));
    }

    /**
     * For a given held tool, returns the list of OTHER tool items in the same tier
     * that the player can transmute to (excluding the item they are currently holding).
     * Returns empty list if the item is not a supported tiered tool.
     */
    public static List<Item> getToolOptions(ItemStack held) {
        if (!(held.getItem() instanceof TieredItem tiered)) return Collections.emptyList();
        Tier tier = tiered.getTier();
        List<Item> all = TOOL_TIERS.get(tier);
        if (all == null) return Collections.emptyList();
        List<Item> result = new ArrayList<>();
        for (Item item : all) {
            if (item != held.getItem()) result.add(item);
        }
        return result;
    }

    /** Checks if the held item is a transmutable tool. */
    public static boolean isTransmutableTool(ItemStack held) {
        if (!(held.getItem() instanceof TieredItem tiered)) return false;
        return TOOL_TIERS.containsKey(tiered.getTier());
    }

    /**
     * Returns the target tool item given the held item and target index in the options list.
     * Returns null if invalid.
     */
    @Nullable
    public static Item getToolTarget(ItemStack held, int targetIndex) {
        List<Item> options = getToolOptions(held);
        if (targetIndex < 0 || targetIndex >= options.size()) return null;
        return options.get(targetIndex);
    }

    // -----------------------------------------------------------------------
    // Crop transmutation
    // -----------------------------------------------------------------------

    private static final List<Item> CROPS = List.of(
            Items.WHEAT_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.CARROT,
            Items.POTATO,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS
    );

    /** Returns crop options excluding the held item. Empty if not a crop. */
    public static List<Item> getCropOptions(ItemStack held) {
        if (!CROPS.contains(held.getItem())) return Collections.emptyList();
        List<Item> result = new ArrayList<>();
        for (Item item : CROPS) {
            if (item != held.getItem()) result.add(item);
        }
        return result;
    }

    public static boolean isTransmutableCrop(ItemStack held) {
        return CROPS.contains(held.getItem());
    }

    @Nullable
    public static Item getCropTarget(ItemStack held, int targetIndex) {
        List<Item> options = getCropOptions(held);
        if (targetIndex < 0 || targetIndex >= options.size()) return null;
        return options.get(targetIndex);
    }

    // -----------------------------------------------------------------------
    // Food transmutation
    // -----------------------------------------------------------------------

    private static final List<Item> RAW_FOODS = List.of(
            Items.BEEF,
            Items.PORKCHOP,
            Items.CHICKEN,
            Items.MUTTON,
            Items.RABBIT,
            Items.COD,
            Items.SALMON
    );

    private static final List<Item> COOKED_FOODS = List.of(
            Items.COOKED_BEEF,
            Items.COOKED_PORKCHOP,
            Items.COOKED_CHICKEN,
            Items.COOKED_MUTTON,
            Items.COOKED_RABBIT,
            Items.COOKED_COD,
            Items.COOKED_SALMON
    );

    public static List<Item> getRawFoodOptions(ItemStack held) {
        if (!RAW_FOODS.contains(held.getItem())) return Collections.emptyList();
        List<Item> result = new ArrayList<>();
        for (Item item : RAW_FOODS) {
            if (item != held.getItem()) result.add(item);
        }
        return result;
    }

    public static boolean isTransmutableRawFood(ItemStack held) {
        return RAW_FOODS.contains(held.getItem());
    }

    @Nullable
    public static Item getRawFoodTarget(ItemStack held, int targetIndex) {
        List<Item> options = getRawFoodOptions(held);
        if (targetIndex < 0 || targetIndex >= options.size()) return null;
        return options.get(targetIndex);
    }

    public static List<Item> getCookedFoodOptions(ItemStack held) {
        if (!COOKED_FOODS.contains(held.getItem())) return Collections.emptyList();
        List<Item> result = new ArrayList<>();
        for (Item item : COOKED_FOODS) {
            if (item != held.getItem()) result.add(item);
        }
        return result;
    }

    public static boolean isTransmutableCookedFood(ItemStack held) {
        return COOKED_FOODS.contains(held.getItem());
    }

    @Nullable
    public static Item getCookedFoodTarget(ItemStack held, int targetIndex) {
        List<Item> options = getCookedFoodOptions(held);
        if (targetIndex < 0 || targetIndex >= options.size()) return null;
        return options.get(targetIndex);
    }

    // -----------------------------------------------------------------------
    // Generic category dispatch
    // -----------------------------------------------------------------------

    /**
     * Determines the transmutation category of the held item, or null if not transmutable.
     */
    @Nullable
    public static TransmutationCategory detectCategory(ItemStack held) {
        if (held.isEmpty()) return null;
        if (isTransmutableTool(held)) return TransmutationCategory.TOOL;
        if (isTransmutableCrop(held)) return TransmutationCategory.CROP;
        if (isTransmutableRawFood(held)) return TransmutationCategory.RAW_FOOD;
        if (isTransmutableCookedFood(held)) return TransmutationCategory.COOKED_FOOD;
        return null;
    }

    /** Returns the option list for a given category and held item. */
    public static List<Item> getOptions(TransmutationCategory category, ItemStack held) {
        return switch (category) {
            case TOOL -> getToolOptions(held);
            case CROP -> getCropOptions(held);
            case RAW_FOOD -> getRawFoodOptions(held);
            case COOKED_FOOD -> getCookedFoodOptions(held);
        };
    }

    /** Returns the target item for a given category, held item, and index. */
    @Nullable
    public static Item getTarget(TransmutationCategory category, ItemStack held, int targetIndex) {
        return switch (category) {
            case TOOL -> getToolTarget(held, targetIndex);
            case CROP -> getCropTarget(held, targetIndex);
            case RAW_FOOD -> getRawFoodTarget(held, targetIndex);
            case COOKED_FOOD -> getCookedFoodTarget(held, targetIndex);
        };
    }
}
