package com.jttw.engine.village;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What each calling trades, and for how much, in wen ({@link Coins}): a table of plain data, items
 * named by id, so it can be checked without a world (prices sane, nothing to be had for less than it
 * fetches, no talisman sold by a mortal). {@link VillageOffers} makes the merchant offers of it.
 *
 * <p>A {@link Deal} is one line of a trader's list: the trader <b>sells</b> {@code count} of an item
 * for {@code wen}, paid in silver and copper; or <b>buys</b> {@code count} of it from the player for
 * {@code wen}, paid out in one stack (copper up to 64, else whole taels). A map's item is
 * {@code "map:<place>"}, made when the offer is (an explorer's map of the place, marked).
 *
 * <p>The scale: a day's farm work (twenty wheat) fetches four wen; a bowl of rice costs four, a
 * steamed bun two; a Qi-gathering pill a tael and a half; the talismans of the supernatural world two
 * to six taels; a Rime Shackle twelve. The money-changer exchanges coin for coin at par.
 */
public final class TangTrades {

    private TangTrades() {}

    public enum Side { SELLS, BUYS }

    /** How a seller asks to be paid: in silver and copper as the price falls, or all in copper (the money-changer's silver and strings). */
    public enum Pay { COINS, COPPER }

    /**
     * One line of a trader's list.
     *
     * @param side     whether the trader sells it or buys it
     * @param item     the item's id ({@code "minecraft:wheat"}), or {@code "map:<place>"}
     * @param count    how many change hands
     * @param wen      what they cost (or fetch), in wen
     * @param maxUses  how many times a day the trader will deal so
     * @param xp       the experience it gives the player
     * @param pay      how it is paid for, if sold
     */
    public record Deal(Side side, String item, int count, int wen, int maxUses, int xp, Pay pay) {
        /** What one of the item costs or fetches, in wen. */
        public double unit() {
            return (double) wen / count;
        }

        public boolean isMap() {
            return item.startsWith(MAP);
        }
    }

    public static final String MAP = "map:";
    private static final String M = "jttw_engine:";

    /** The talismans, charms and incense: traded only in the supernatural world. */
    public static final Set<String> TALISMANS = Set.of(M + "demon_quelling_talisman", M + "ghost_warding_talisman", M + "house_ward_talisman",
            M + "incense", M + "peace_charm");

    /** The places the scholars sell maps of, in the order of the journey: the zones, then Fengdu and Mount Kunlun. */
    public static final List<String> MAP_PLACES = List.of("white_bone_ridge", "bottomless_cave", "mystical_waters", "fire_mountain",
            "mount_wuzhuang", "lion_camel_ridge", "fengdu", "kunlun");

    private static Deal sells(String item, int count, int wen, int maxUses) {
        return new Deal(Side.SELLS, item, count, wen, maxUses, Math.max(1, Math.min(30, wen / 8)), Pay.COINS);
    }

    private static Deal buys(String item, int count, int wen, int maxUses) {
        return new Deal(Side.BUYS, item, count, wen, maxUses, Math.max(1, Math.min(20, wen / 8)), Pay.COINS);
    }

    private static String mc(String id) {
        return "minecraft:" + id;
    }

    private static final Map<Calling, List<Deal>> TABLE = new EnumMap<>(Calling.class);

    static {
        TABLE.put(Calling.FARMER, List.of(
                buys(mc("wheat"), 20, 4, 16), buys(mc("carrot"), 22, 4, 16), buys(mc("potato"), 26, 4, 16), buys(mc("beetroot"), 15, 4, 16),
                sells(M + "rice", 8, 6, 16), sells(mc("egg"), 4, 3, 12), sells(mc("apple"), 4, 4, 12), sells(mc("pumpkin_pie"), 2, 5, 8)));
        TABLE.put(Calling.INNKEEPER, List.of(
                sells(M + "steamed_bun", 3, 6, 16), sells(M + "mantou", 4, 4, 16), sells(M + "rice_bowl", 1, 4, 16),
                sells(M + "rice_wine", 1, 12, 12), sells(M + "tea", 1, 8, 12),
                buys(mc("chicken"), 6, 5, 16), buys(mc("porkchop"), 5, 5, 16), buys(mc("mutton"), 5, 5, 16), buys(mc("cod"), 6, 4, 16)));
        TABLE.put(Calling.HERBALIST, List.of(
                sells(M + "herbal_decoction", 1, 20, 12), sells(M + "qi_gathering_pill", 1, 96, 6), sells(M + "tea", 1, 8, 12),
                buys(mc("brown_mushroom"), 8, 3, 16), buys(mc("red_mushroom"), 8, 3, 16), buys(mc("honey_bottle"), 2, 6, 12),
                buys(mc("glow_berries"), 12, 4, 16), buys(mc("spider_eye"), 4, 3, 16)));
        TABLE.put(Calling.MONK, List.of(
                sells(M + "tea", 1, 6, 12), sells(M + "mantou", 4, 3, 16), sells(mc("book"), 1, 24, 6),
                buys(mc("candle"), 2, 6, 12), buys(mc("paper"), 24, 6, 16), buys(mc("honeycomb"), 3, 4, 12)));
        List<Deal> scholar = new ArrayList<>();
        int[] mapPrices = {64, 80, 96, 128, 160, 192, 192, 256};
        for (int i = 0; i < MAP_PLACES.size(); i++) {
            scholar.add(new Deal(Side.SELLS, MAP + MAP_PLACES.get(i), 1, mapPrices[i], 1, 12 + 2 * i, Pay.COINS));
        }
        scholar.addAll(List.of(sells(mc("paper"), 12, 6, 16), sells(mc("book"), 1, 30, 8),
                buys(mc("book"), 1, 8, 8), buys(mc("feather"), 12, 4, 16), buys(mc("ink_sac"), 6, 4, 16)));
        TABLE.put(Calling.SCHOLAR, List.copyOf(scholar));
        TABLE.put(Calling.FORTUNE_TELLER, List.of(
                sells(M + "rumour_scroll", 1, 16, 12), sells(M + "earth_god_luopan", 1, 48, 2),
                buys(mc("amethyst_shard"), 6, 6, 12), buys(mc("rabbit_foot"), 1, 12, 8)));
        TABLE.put(Calling.MERCHANT, List.of(
                // the money-changer's: coin for coin, at par
                new Deal(Side.SELLS, M + "silver_tael", 1, Coins.COPPER_PER_TAEL, 32, 1, Pay.COPPER),
                new Deal(Side.BUYS, M + "silver_tael", 1, Coins.COPPER_PER_TAEL, 32, 1, Pay.COPPER),
                new Deal(Side.SELLS, M + "string_of_cash", 1, Coins.COPPER_PER_STRING, 32, 1, Pay.COPPER),
                new Deal(Side.BUYS, M + "string_of_cash", 1, Coins.COPPER_PER_STRING, 32, 1, Pay.COPPER),
                // foreign gems and metal bought for coin; silk and dye sold
                buys(mc("emerald"), 1, 24, 16), buys(mc("gold_ingot"), 1, 40, 12), buys(mc("diamond"), 1, 128, 6),
                sells(mc("white_wool"), 2, 6, 16), sells(mc("red_dye"), 4, 4, 12)));
        TABLE.put(Calling.SMITH, List.of(
                sells(mc("iron_ingot"), 1, 10, 16), sells(mc("gold_ingot"), 1, 56, 8), sells(M + "jade_block", 1, 192, 3),
                sells(M + "servant_bone", 1, 40, 6), sells(M + "wraith_wisp", 1, 48, 6),
                sells(M + "captains_sigil", 1, 384, 2), sells(M + "rime_shackle", 1, 768, 1),
                buys(mc("iron_ingot"), 4, 24, 12), buys(mc("coal"), 12, 4, 16), buys(M + "servant_bone", 1, 16, 12),
                buys(M + "wraith_wisp", 1, 20, 12), buys(M + "captains_sigil", 1, 128, 4)));
        TABLE.put(Calling.GHOST_MERCHANT, List.of(
                sells(M + "demon_quelling_talisman", 1, 128, 6), sells(M + "ghost_warding_talisman", 1, 128, 6),
                sells(M + "house_ward_talisman", 1, 256, 3), sells(M + "incense", 3, 64, 8), sells(M + "peace_charm", 1, 384, 2),
                // the dead want what the living offer them
                buys(M + "rice_wine", 1, 10, 12), buys(M + "steamed_bun", 4, 6, 12), buys(mc("candle"), 2, 6, 12),
                buys(M + "hell_bank_note", 1, 8, 16), buys(M + "wraith_wisp", 1, 24, 8)));
        TABLE.put(Calling.CELESTIAL_MERCHANT, List.of(
                sells(M + "jade_dew", 1, 192, 4), sells(M + "demon_quelling_talisman", 1, 160, 6), sells(M + "peace_charm", 1, 320, 2),
                sells(M + "house_ward_talisman", 1, 256, 3), sells(M + "incense", 3, 64, 8),
                buys(M + "jade_block", 1, 64, 6), buys(mc("gold_ingot"), 2, 64, 12), buys(M + "crane_plume", 1, 32, 8)));
    }

    /** What a calling trades, in its list's order. */
    public static List<Deal> of(Calling calling) {
        return TABLE.getOrDefault(calling, List.of());
    }

    /** Whether this item is a talisman, charm or incense: the supernatural world's goods. */
    public static boolean isTalisman(String item) {
        return TALISMANS.contains(item);
    }
}
