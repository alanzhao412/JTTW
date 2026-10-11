package com.jttw.engine.item;

import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Nezha's and Erlang Shen's armaments: what each is, whose it is, what forging it in the Eight
 * Trigrams Furnace takes, and how refining it there makes it stronger.
 *
 * <p>Every armament is forged from the zones' own materials (the bones, wisps, sigils and hearts
 * their dead leave), never at a crafting table or a vanilla furnace. Each is then refined, level by
 * level to {@value #MAX_REFINE}, by feeding it more: every level adds {@value #PER_LEVEL_PERCENT}%
 * to what it gives (its attack, its resistance, its speed).
 */
public final class Armaments {

    private Armaments() {}

    /** How far an armament can be refined (with the zones there are). */
    public static final int MAX_REFINE = 10;
    /** What each level of refinement adds to an armament's own bonus, in percent. */
    public static final int PER_LEVEL_PERCENT = 10;

    public static final String TAG_REFINE = "jttw_refine";

    /**
     * One armament.
     *
     * @param id       its name in the armory
     * @param owner    the character it belongs to
     * @param forged   a fresh one, as the furnace forges it
     * @param is       whether a stack is this armament
     * @param forging  what forging it takes, material by material
     */
    public record Armament(String id, ReincarnationOutcome owner, Supplier<ItemStack> forged,
                           Predicate<ItemStack> is, Map<Supplier<Item>, Integer> forging) {

        public String displayName() {
            return forged.get().getHoverName().getString();
        }
    }

    private static Map<Supplier<Item>, Integer> cost(Object... itemsAndCounts) {
        Map<Supplier<Item>, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < itemsAndCounts.length; i += 2) {
            @SuppressWarnings("unchecked") Supplier<Item> item = (Supplier<Item>) itemsAndCounts[i];
            m.put(item, (Integer) itemsAndCounts[i + 1]);
        }
        return m;
    }

    private static Predicate<ItemStack> of(Supplier<Item> item) {
        return s -> s.is(item.get());
    }

    private static final Supplier<Item> BONE = ModItems.SERVANT_BONE::get, WISP = ModItems.WRAITH_WISP::get,
            SIGIL = ModItems.CAPTAINS_SIGIL::get, HEART = ModItems.HEART_BONE::get;

    private static final ReincarnationOutcome NEZHA = ReincarnationOutcome.THIRD_LOTUS_PRINCE, ERLANG = ReincarnationOutcome.EYE_OF_HEAVEN;

    /** Every armament, in each armory's order: weapons first, then what is worn. */
    public static final List<Armament> ALL = List.of(
            // Nezha: the most of anyone
            new Armament("fire_tip_spear", NEZHA, () -> new ItemStack(ModItems.FIRE_TIP_SPEAR.get()), of(ModItems.FIRE_TIP_SPEAR::get),
                    cost(BONE, 6, WISP, 2, SIGIL, 1)),
            new Armament("qiankun_ring", NEZHA, () -> new ItemStack(ModItems.QIANKUN_RING.get()), of(ModItems.QIANKUN_RING::get),
                    cost(WISP, 4, BONE, 2, SIGIL, 1)),
            new Armament("yin_sword", NEZHA, YinYangSwordItem::createYin, YinYangSwordItem::isYin,
                    cost(WISP, 4, BONE, 3, SIGIL, 1)),
            new Armament("yang_sword", NEZHA, YinYangSwordItem::createYang, YinYangSwordItem::isYang,
                    cost(BONE, 4, WISP, 3, SIGIL, 1)),
            new Armament("wind_fire_wheels", NEZHA, () -> new ItemStack(ModItems.WIND_FIRE_WHEELS.get()), of(ModItems.WIND_FIRE_WHEELS::get),
                    cost(WISP, 4, SIGIL, 2)),
            new Armament("huntian_cloth", NEZHA, () -> new ItemStack(ModItems.HUNTIAN_CLOTH.get()), of(ModItems.HUNTIAN_CLOTH::get),
                    cost(WISP, 6, SIGIL, 1)),
            // Erlang Shen
            new Armament("three_pointed_spear", ERLANG, () -> new ItemStack(ModItems.THREE_POINTED_SPEAR.get()), of(ModItems.THREE_POINTED_SPEAR::get),
                    cost(BONE, 6, SIGIL, 2)),
            new Armament("erlang_axe", ERLANG, () -> new ItemStack(ModItems.ERLANG_AXE.get()), of(ModItems.ERLANG_AXE::get),
                    cost(BONE, 7, SIGIL, 1)),
            new Armament("golden_bow", ERLANG, () -> new ItemStack(ModItems.GOLDEN_BOW.get()), of(ModItems.GOLDEN_BOW::get),
                    cost(WISP, 5, BONE, 2, SIGIL, 2)));

    /** The armament this stack is, or null. */
    @Nullable
    public static Armament of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Armament a : ALL) if (a.is().test(stack)) return a;
        return null;
    }

    /** A character's armaments, in their armory's order. */
    public static List<Armament> of(@Nullable ReincarnationOutcome owner) {
        return ALL.stream().filter(a -> a.owner() == owner).toList();
    }

    // ── Refinement ───────────────────────────────────────────────────────

    public static int refine(ItemStack stack) {
        return stack.hasTag() ? Math.min(MAX_REFINE, stack.getTag().getInt(TAG_REFINE)) : 0;
    }

    public static void setRefine(ItemStack stack, int level) {
        stack.getOrCreateTag().putInt(TAG_REFINE, Math.max(0, Math.min(MAX_REFINE, level)));
    }

    /** What an armament's own bonus is multiplied by at its refinement: x1 unrefined, x2 at 10. */
    public static double refineMultiplier(ItemStack stack) {
        return 1 + refine(stack) * PER_LEVEL_PERCENT / 100.0;
    }

    /**
     * What raising an armament from {@code level} to the next takes: more bones and wisps each
     * level, a captain's sigil from level 4, and for the last, Lady White Bone's heart.
     */
    public static Map<Supplier<Item>, Integer> refineCost(int level) {
        Map<Supplier<Item>, Integer> m = new LinkedHashMap<>();
        m.put(BONE, 2 + level);
        m.put(WISP, 1 + level / 2);
        if (level >= 4) m.put(SIGIL, 1 + (level - 4) / 3);
        if (level == MAX_REFINE - 1) m.put(HEART, 1);
        return m;
    }

    /** "6 Servant Bone, 2 Wraith Wisp": a cost, written out. */
    public static String describe(Map<Supplier<Item>, Integer> cost) {
        StringBuilder sb = new StringBuilder();
        cost.forEach((item, n) -> {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(n).append(' ').append(new ItemStack(item.get()).getHoverName().getString());
        });
        return sb.toString();
    }
}
