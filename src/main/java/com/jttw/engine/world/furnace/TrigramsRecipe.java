package com.jttw.engine.world.furnace;

import com.jttw.engine.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Something the Eight Trigrams Furnace forges that is not an armament ({@link com.jttw.engine.item.Armaments}):
 * from exactly these materials in its trigram slots, with the centre empty, it forges the result
 * into the centre, as it forges an armament. Data-driven ({@code data/jttw_engine/recipe/}, type
 * {@code jttw_engine:trigrams_forging}); a recipe type of its own, so no crafting table, vanilla
 * furnace or anything else makes it.
 *
 * <pre>{@code
 * {"type": "jttw_engine:trigrams_forging",
 *  "materials": [{"item": "minecraft:iron_ingot", "count": 9}, ...],
 *  "result": {"id": "jttw_engine:nine_tooth_rake"}}
 * }</pre>
 */
public record TrigramsRecipe(List<Material> materials, ItemStack result) implements Recipe<TrigramsRecipe.Trigrams> {

    /** So many of one item. */
    public record Material(Item item, int count) {
        public static final Codec<Material> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Material::item),
                Codec.intRange(1, 64 * TrigramsFurnaceBlockEntity.TRIGRAMS).fieldOf("count").forGetter(Material::count)
        ).apply(i, Material::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Material> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.ITEM), Material::item, ByteBufCodecs.VAR_INT, Material::count, Material::new);
    }

    /** What lies in the furnace's eight trigram slots. */
    public record Trigrams(List<ItemStack> items) implements RecipeInput {
        @Override public ItemStack getItem(int slot) { return items.get(slot); }
        @Override public int size() { return items.size(); }

        /** Its materials, counted by item. */
        public Map<Item, Integer> counted() {
            Map<Item, Integer> m = new HashMap<>();
            for (ItemStack s : items) if (!s.isEmpty()) m.merge(s.getItem(), s.getCount(), Integer::sum);
            return m;
        }
    }

    /** Its materials, counted by item. */
    public Map<Item, Integer> cost() {
        Map<Item, Integer> m = new java.util.LinkedHashMap<>();
        for (Material mat : materials) m.merge(mat.item(), mat.count(), Integer::sum);
        return m;
    }

    /** Exactly its materials, nothing more and nothing less (as an armament's forging). */
    @Override
    public boolean matches(Trigrams input, Level level) {
        return cost().equals(input.counted());
    }

    @Override
    public ItemStack assemble(Trigrams input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    /** Not in the recipe book: the furnace's own screen tells what it forges. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TRIGRAMS_FORGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.TRIGRAMS_FORGING.get();
    }

    /** "9 Iron Ingot, 1 Rime Shackle": its materials, written out. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        cost().forEach((item, n) -> {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(n).append(' ').append(new ItemStack(item).getHoverName().getString());
        });
        return sb.toString();
    }

    public static final class Serializer implements RecipeSerializer<TrigramsRecipe> {
        private static final MapCodec<TrigramsRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Material.CODEC.listOf().fieldOf("materials").forGetter(TrigramsRecipe::materials),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(TrigramsRecipe::result)
        ).apply(i, TrigramsRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, TrigramsRecipe> STREAM_CODEC = StreamCodec.composite(
                Material.STREAM_CODEC.apply(ByteBufCodecs.list()), TrigramsRecipe::materials,
                ItemStack.STREAM_CODEC, TrigramsRecipe::result, TrigramsRecipe::new);

        @Override public MapCodec<TrigramsRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, TrigramsRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
