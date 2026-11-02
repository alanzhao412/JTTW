package com.jttw.engine.item.craft;

import com.jttw.engine.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Crafting-table recipes only some characters can make: a vanilla shaped or shapeless recipe with a
 * {@link ClassGate} ({@code "gate"}). Types {@code jttw_engine:gated_shaped} and
 * {@code jttw_engine:gated_shapeless}; otherwise written as vanilla's.
 *
 * <pre>{@code
 * {"type": "jttw_engine:gated_shaped", "gate": {"characters": ["third_lotus_prince"]},
 *  "pattern": [...], "key": {...}, "result": {"id": "jttw_engine:fire_tip_spear"}}
 * }</pre>
 *
 * <p>On the server it matches only for a crafter the gate lets through ({@link Crafters}); on a client it
 * matches as the plain recipe would (the client never makes the item; the server decides).
 */
public interface GatedRecipe {

    ClassGate gate();

    /** Whether the one crafting now may make it. */
    static boolean crafterAllowed(ClassGate gate, Level level) {
        if (level.isClientSide) return true;
        Player p = Crafters.current();
        return gate.allows(p);
    }

    final class Shaped extends ShapedRecipe implements GatedRecipe {
        private final ClassGate gate;
        private final ItemStack result;

        public Shaped(ClassGate gate, String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean notify) {
            super(group, category, pattern, result, notify);
            this.gate = gate;
            this.result = result;
        }

        @Override public ClassGate gate() { return gate; }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return super.matches(input, level) && crafterAllowed(gate, level);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return ModRecipes.GATED_SHAPED.get();
        }

        public static final class Serializer implements RecipeSerializer<Shaped> {
            private static final MapCodec<Shaped> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    ClassGate.CODEC.fieldOf("gate").forGetter(Shaped::gate),
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
                    ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                    Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification)
            ).apply(i, Shaped::new));
            private static final StreamCodec<RegistryFriendlyByteBuf, Shaped> STREAM_CODEC = StreamCodec.of(
                    (buf, r) -> {
                        ClassGate.STREAM_CODEC.encode(buf, r.gate);
                        buf.writeUtf(r.getGroup());
                        buf.writeEnum(r.category());
                        ShapedRecipePattern.STREAM_CODEC.encode(buf, r.pattern);
                        ItemStack.STREAM_CODEC.encode(buf, r.result);
                        buf.writeBoolean(r.showNotification());
                    },
                    buf -> new Shaped(ClassGate.STREAM_CODEC.decode(buf), buf.readUtf(), buf.readEnum(CraftingBookCategory.class),
                            ShapedRecipePattern.STREAM_CODEC.decode(buf), ItemStack.STREAM_CODEC.decode(buf), buf.readBoolean()));

            @Override public MapCodec<Shaped> codec() { return CODEC; }
            @Override public StreamCodec<RegistryFriendlyByteBuf, Shaped> streamCodec() { return STREAM_CODEC; }
        }
    }

    final class Shapeless extends ShapelessRecipe implements GatedRecipe {
        private final ClassGate gate;
        private final ItemStack result;

        public Shapeless(ClassGate gate, String group, CraftingBookCategory category, ItemStack result, NonNullList<Ingredient> ingredients) {
            super(group, category, result, ingredients);
            this.gate = gate;
            this.result = result;
        }

        @Override public ClassGate gate() { return gate; }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return super.matches(input, level) && crafterAllowed(gate, level);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return ModRecipes.GATED_SHAPELESS.get();
        }

        public static final class Serializer implements RecipeSerializer<Shapeless> {
            private static final MapCodec<Shapeless> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    ClassGate.CODEC.fieldOf("gate").forGetter(Shapeless::gate),
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                    Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(
                            (List<Ingredient> l) -> l.isEmpty() || l.size() > 9
                                    ? DataResult.<NonNullList<Ingredient>>error(() -> "a shapeless recipe takes 1 to 9 ingredients")
                                    : DataResult.success(NonNullList.of(Ingredient.EMPTY, l.toArray(Ingredient[]::new))),
                            (NonNullList<Ingredient> l) -> DataResult.success((List<Ingredient>) l)).forGetter(ShapelessRecipe::getIngredients)
            ).apply(i, Shapeless::new));
            private static final StreamCodec<RegistryFriendlyByteBuf, Shapeless> STREAM_CODEC = StreamCodec.of(
                    (buf, r) -> {
                        ClassGate.STREAM_CODEC.encode(buf, r.gate);
                        buf.writeUtf(r.getGroup());
                        buf.writeEnum(r.category());
                        ItemStack.STREAM_CODEC.encode(buf, r.result);
                        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.getIngredients());
                    },
                    buf -> {
                        ClassGate gate = ClassGate.STREAM_CODEC.decode(buf);
                        String group = buf.readUtf();
                        CraftingBookCategory cat = buf.readEnum(CraftingBookCategory.class);
                        ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                        List<Ingredient> ing = Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
                        return new Shapeless(gate, group, cat, result, NonNullList.of(Ingredient.EMPTY, ing.toArray(Ingredient[]::new)));
                    });

            @Override public MapCodec<Shapeless> codec() { return CODEC; }
            @Override public StreamCodec<RegistryFriendlyByteBuf, Shapeless> streamCodec() { return STREAM_CODEC; }
        }
    }
}
