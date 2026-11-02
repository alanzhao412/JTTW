package com.jttw.engine.item.craft;

import com.jttw.engine.item.Armaments;
import com.jttw.engine.registry.ModRecipes;
import com.jttw.engine.world.furnace.TrigramsRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Refining a Heavenly armament a tier in the Eight Trigrams Furnace (tiers V to VIII): with the
 * armament, one tier short of {@code tier}, in the centre, and at least these materials in the
 * trigram slots, the furnace refines it to {@code tier} as it burns, for a smith its gate lets
 * through. Type {@code jttw_engine:trigrams_refining}.
 *
 * <pre>{@code
 * {"type": "jttw_engine:trigrams_refining", "gate": {"characters": ["marshal_of_the_heavenly_canopy"]},
 *  "armaments": ["nine_tooth_rake"], "tier": 5,
 *  "materials": [{"item": "jttw_engine:rime_shackle", "count": 1}, ...]}
 * }</pre>
 */
public record TrigramsRefining(ClassGate gate, List<String> armaments, int tier, List<TrigramsRecipe.Material> materials)
        implements Recipe<TrigramsRecipe.Trigrams> {

    public Map<Item, Integer> cost() {
        Map<Item, Integer> m = new LinkedHashMap<>();
        for (TrigramsRecipe.Material mat : materials) m.merge(mat.item(), mat.count(), Integer::sum);
        return m;
    }

    /** Whether it takes this armament, at the tier it holds, a tier up. */
    public boolean refines(ItemStack centre) {
        Armaments.Armament a = Armaments.of(centre);
        return a != null && armaments.contains(a.id()) && Armaments.tier(centre) == tier - 1;
    }

    public boolean refines(String armamentId) {
        return armaments.contains(armamentId);
    }

    /** At least its materials. */
    @Override
    public boolean matches(TrigramsRecipe.Trigrams input, Level level) {
        Map<Item, Integer> have = input.counted();
        for (var e : cost().entrySet()) if (have.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
        return true;
    }

    @Override
    public ItemStack assemble(TrigramsRecipe.Trigrams input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY; // the furnace raises the centre's tier itself
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        Armaments.Armament a = armaments.isEmpty() ? null : Armaments.byId(armaments.get(0));
        return a == null ? ItemStack.EMPTY : Armaments.at(a, tier);
    }

    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.TRIGRAMS_REFINING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.TRIGRAMS_REFINING.get(); }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        cost().forEach((item, n) -> sb.append(sb.isEmpty() ? "" : ", ").append(n).append(' ').append(new ItemStack(item).getHoverName().getString()));
        return sb.toString();
    }

    public static final class Serializer implements RecipeSerializer<TrigramsRefining> {
        private static final MapCodec<TrigramsRefining> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ClassGate.CODEC.optionalFieldOf("gate", ClassGate.ANYONE).forGetter(TrigramsRefining::gate),
                Codec.STRING.listOf().fieldOf("armaments").forGetter(TrigramsRefining::armaments),
                Codec.intRange(2, Armaments.TIERS).fieldOf("tier").forGetter(TrigramsRefining::tier),
                TrigramsRecipe.Material.CODEC.listOf().fieldOf("materials").forGetter(TrigramsRefining::materials)
        ).apply(i, TrigramsRefining::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, TrigramsRefining> STREAM_CODEC = StreamCodec.composite(
                ClassGate.STREAM_CODEC, TrigramsRefining::gate,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TrigramsRefining::armaments,
                ByteBufCodecs.VAR_INT, TrigramsRefining::tier,
                TrigramsRecipe.Material.STREAM_CODEC.apply(ByteBufCodecs.list()), TrigramsRefining::materials,
                TrigramsRefining::new);

        @Override public MapCodec<TrigramsRefining> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, TrigramsRefining> streamCodec() { return STREAM_CODEC; }
    }
}
