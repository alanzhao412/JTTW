package com.jttw.engine.item;

import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The Heavenly characters' armaments (Nezha's, Erlang Shen's, the Marshal's): what each is, whose it
 * is, and its tier.
 *
 * <p>Every armament is crafted, never found: forged at a crafting table at <b>tier I</b> from the
 * first ring's materials (a gated recipe, {@code jttw_engine:gated_shaped}: its owner alone can make
 * it), then refined tier by tier, <b>II to VIII</b>, one tier for each ring of levels, from that
 * ring's zones' materials ({@link com.jttw.engine.item.craft.RefineRecipe}: tiers II to IV at a
 * crafting table, V to VIII in the Eight Trigrams Furnace). The recipes are data
 * ({@code data/jttw_engine/recipe/heavenly/}).
 *
 * <p>A tier raises the armament's own bonus (its attack, its resistance, its speed) by
 * {@value #PER_TIER_PERCENT}% of its tier-I bonus; and the armory's attunement ({@link Armory})
 * holds a Heavenly character to the tier of their ring: each tier their armaments trail it costs them
 * a step of their health and attack.
 */
public final class Armaments {

    private Armaments() {}

    /** The tiers there are: I to VIII, one for each ring of levels. */
    public static final int TIERS = ArmamentTiers.TIERS;
    /** How far an armament can be refined past tier I. */
    public static final int MAX_REFINE = TIERS - 1;
    /** What each tier past I adds to an armament's own bonus, in percent of its tier-I bonus. */
    public static final int PER_TIER_PERCENT = ArmamentTiers.PER_TIER_PERCENT;

    public static final String TAG_REFINE = "jttw_refine";

    /**
     * One armament.
     *
     * @param id     its name in the armory
     * @param owner  the character it belongs to
     * @param forged a fresh one at tier I, as its recipe makes it
     * @param is     whether a stack is this armament
     */
    public record Armament(String id, ReincarnationOutcome owner, Supplier<ItemStack> forged, Predicate<ItemStack> is) {

        public String displayName() {
            return forged.get().getHoverName().getString();
        }
    }

    private static Predicate<ItemStack> of(Supplier<Item> item) {
        return s -> s.is(item.get());
    }

    private static final ReincarnationOutcome NEZHA = ReincarnationOutcome.THIRD_LOTUS_PRINCE, ERLANG = ReincarnationOutcome.EYE_OF_HEAVEN,
            MARSHAL = ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY;

    /** Every armament, in each armory's order: weapons first, then what is worn. */
    public static final List<Armament> ALL = List.of(
            // Nezha: the most of anyone
            new Armament("fire_tip_spear", NEZHA, () -> new ItemStack(ModItems.FIRE_TIP_SPEAR.get()), of(ModItems.FIRE_TIP_SPEAR::get)),
            new Armament("qiankun_ring", NEZHA, () -> new ItemStack(ModItems.QIANKUN_RING.get()), of(ModItems.QIANKUN_RING::get)),
            new Armament("yin_sword", NEZHA, YinYangSwordItem::createYin, YinYangSwordItem::isYin),
            new Armament("yang_sword", NEZHA, YinYangSwordItem::createYang, YinYangSwordItem::isYang),
            new Armament("wind_fire_wheels", NEZHA, () -> new ItemStack(ModItems.WIND_FIRE_WHEELS.get()), of(ModItems.WIND_FIRE_WHEELS::get)),
            new Armament("huntian_cloth", NEZHA, () -> new ItemStack(ModItems.HUNTIAN_CLOTH.get()), of(ModItems.HUNTIAN_CLOTH::get)),
            // Erlang Shen
            new Armament("three_pointed_spear", ERLANG, () -> new ItemStack(ModItems.THREE_POINTED_SPEAR.get()), of(ModItems.THREE_POINTED_SPEAR::get)),
            new Armament("erlang_axe", ERLANG, () -> new ItemStack(ModItems.ERLANG_AXE.get()), of(ModItems.ERLANG_AXE::get)),
            new Armament("golden_bow", ERLANG, () -> new ItemStack(ModItems.GOLDEN_BOW.get()), of(ModItems.GOLDEN_BOW::get)),
            // the Marshal of the Heavenly Canopy
            new Armament("nine_tooth_rake", MARSHAL, () -> new ItemStack(ModItems.NINE_TOOTH_RAKE.get()), of(ModItems.NINE_TOOTH_RAKE::get)));

    /** The armament this stack is, or null. */
    @Nullable
    public static Armament of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Armament a : ALL) if (a.is().test(stack)) return a;
        return null;
    }

    /** The armament of this id, or null. */
    @Nullable
    public static Armament byId(String id) {
        for (Armament a : ALL) if (a.id().equals(id)) return a;
        return null;
    }

    /** A character's armaments, in their armory's order. */
    public static List<Armament> of(@Nullable ReincarnationOutcome owner) {
        return ALL.stream().filter(a -> a.owner() == owner).toList();
    }

    // ── Tiers ────────────────────────────────────────────────────────────

    /** How many times it has been refined past tier I: 0 to {@value #MAX_REFINE}. */
    public static int refine(ItemStack stack) {
        return Math.min(MAX_REFINE, StackData.getInt(stack, TAG_REFINE));
    }

    public static void setRefine(ItemStack stack, int level) {
        StackData.putInt(stack, TAG_REFINE, Math.max(0, Math.min(MAX_REFINE, level)));
    }

    /** Its tier: I (1) to VIII (8). */
    public static int tier(ItemStack stack) {
        return refine(stack) + 1;
    }

    public static void setTier(ItemStack stack, int tier) {
        setRefine(stack, tier - 1);
    }

    /** A fresh armament of this id at this tier. */
    public static ItemStack at(Armament a, int tier) {
        ItemStack s = a.forged().get();
        setTier(s, tier);
        return s;
    }

    /** What an armament's own bonus is multiplied by at its tier: x1 at I, x1.7 at VIII. */
    public static double refineMultiplier(ItemStack stack) {
        return tierMultiplier(tier(stack));
    }

    public static double tierMultiplier(int tier) {
        return ArmamentTiers.multiplier(tier);
    }

    public static int ringTier(int level) {
        return ArmamentTiers.ringTier(level);
    }

    public static String roman(int tier) {
        return ArmamentTiers.roman(tier);
    }

    public static String ring(int tier) {
        return ArmamentTiers.ring(tier);
    }
}
