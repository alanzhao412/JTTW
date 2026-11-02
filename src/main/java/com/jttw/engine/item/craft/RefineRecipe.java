package com.jttw.engine.item.craft;

import com.jttw.engine.item.Armaments;
import com.jttw.engine.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Refining a Heavenly armament a tier at a crafting table (tiers II to IV): the armament, one tier
 * short of {@code tier}, with exactly these ingredients round it (shapeless), makes the same armament
 * at {@code tier}, all else about it kept. Type {@code jttw_engine:refining}; gated like
 * {@link GatedRecipe}. The furnace's tiers are {@link TrigramsRefining}.
 *
 * <pre>{@code
 * {"type": "jttw_engine:refining", "gate": {"characters": ["third_lotus_prince"]},
 *  "armaments": ["fire_tip_spear", "qiankun_ring"], "tier": 2,
 *  "ingredients": [{"item": "jttw_engine:servant_bone"}, ...]}
 * }</pre>
 */
public record RefineRecipe(ClassGate gate, List<String> armaments, int tier, NonNullList<Ingredient> ingredients)
        implements CraftingRecipe, GatedRecipe {

    /** The armament in the grid this recipe would refine (one of its, one tier short), or null. */
    @Nullable
    private ItemStack armamentIn(CraftingInput input) {
        ItemStack found = null;
        for (ItemStack s : input.items()) {
            Armaments.Armament a = Armaments.of(s);
            if (a == null) continue;
            if (found != null || !armaments.contains(a.id()) || Armaments.tier(s) != tier - 1) return null;
            found = s;
        }
        return found;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack armament = armamentIn(input);
        if (armament == null) return false;
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack s : input.items()) if (!s.isEmpty() && s != armament) rest.add(s);
        if (rest.size() != ingredients.size()) return false;
        if (net.neoforged.neoforge.common.util.RecipeMatcher.findMatches(rest, ingredients) == null) return false;
        return GatedRecipe.crafterAllowed(gate, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack armament = armamentIn(input);
        if (armament == null) return ItemStack.EMPTY;
        ItemStack out = armament.copyWithCount(1);
        Armaments.setTier(out, tier);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height > ingredients.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        Armaments.Armament a = armaments.isEmpty() ? null : Armaments.byId(armaments.get(0));
        return a == null ? ItemStack.EMPTY : Armaments.at(a, tier);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    /** Not in the recipe book: an armament's tooltip names its next tier's recipe. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.REFINING.get();
    }

    /** Whether it refines this armament (of any tier). */
    public boolean refines(String armamentId) {
        return armaments.contains(armamentId);
    }

    /** "4 Spirit Whisker, 2 Night Petal": its ingredients, counted, written out. */
    public String describe() {
        Map<String, Integer> counted = new LinkedHashMap<>();
        for (Ingredient i : ingredients) {
            ItemStack[] items = i.getItems();
            String name = items.length == 0 ? "?" : items[0].getHoverName().getString();
            counted.merge(name, 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder();
        counted.forEach((n, c) -> sb.append(sb.isEmpty() ? "" : ", ").append(c).append(' ').append(n));
        return sb.toString();
    }

    private static NonNullList<Ingredient> list(List<Ingredient> l) {
        return NonNullList.of(Ingredient.EMPTY, l.toArray(Ingredient[]::new));
    }

    public static final class Serializer implements RecipeSerializer<RefineRecipe> {
        private static final MapCodec<RefineRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ClassGate.CODEC.optionalFieldOf("gate", ClassGate.ANYONE).forGetter(RefineRecipe::gate),
                Codec.STRING.listOf().fieldOf("armaments").forGetter(RefineRecipe::armaments),
                Codec.intRange(2, Armaments.TIERS).fieldOf("tier").forGetter(RefineRecipe::tier),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")
                        .<NonNullList<Ingredient>>xmap(RefineRecipe::list, l -> l).forGetter(RefineRecipe::ingredients)
        ).apply(i, RefineRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, RefineRecipe> STREAM_CODEC = StreamCodec.composite(
                ClassGate.STREAM_CODEC, RefineRecipe::gate,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), RefineRecipe::armaments,
                ByteBufCodecs.VAR_INT, RefineRecipe::tier,
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).<NonNullList<Ingredient>>map(RefineRecipe::list, l -> l), RefineRecipe::ingredients,
                RefineRecipe::new);

        @Override public MapCodec<RefineRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, RefineRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
