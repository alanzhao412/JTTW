package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.furnace.TrigramsRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's recipe types: the Eight Trigrams Furnace's, and the class-gated crafting recipes. */
public final class ModRecipes {

    private ModRecipes() {}

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, JTTWEngine.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, JTTWEngine.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<TrigramsRecipe>> TRIGRAMS_FORGING =
            TYPES.register("trigrams_forging", () -> RecipeType.simple(JTTWEngine.id("trigrams_forging")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TrigramsRecipe>> TRIGRAMS_FORGING_SERIALIZER =
            SERIALIZERS.register("trigrams_forging", TrigramsRecipe.Serializer::new);

    /** The furnace's refining of a Heavenly armament, tiers V to VIII (see TrigramsRefining). */
    public static final DeferredHolder<RecipeType<?>, RecipeType<com.jttw.engine.item.craft.TrigramsRefining>> TRIGRAMS_REFINING =
            TYPES.register("trigrams_refining", () -> RecipeType.simple(JTTWEngine.id("trigrams_refining")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<com.jttw.engine.item.craft.TrigramsRefining>> TRIGRAMS_REFINING_SERIALIZER =
            SERIALIZERS.register("trigrams_refining", com.jttw.engine.item.craft.TrigramsRefining.Serializer::new);

    /** Crafting-table recipes only some characters can make (see ClassGate, GatedRecipe). */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<com.jttw.engine.item.craft.GatedRecipe.Shaped>> GATED_SHAPED =
            SERIALIZERS.register("gated_shaped", com.jttw.engine.item.craft.GatedRecipe.Shaped.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<com.jttw.engine.item.craft.GatedRecipe.Shapeless>> GATED_SHAPELESS =
            SERIALIZERS.register("gated_shapeless", com.jttw.engine.item.craft.GatedRecipe.Shapeless.Serializer::new);
    /** Refining a Heavenly armament at a crafting table, tiers II to IV (see RefineRecipe). */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<com.jttw.engine.item.craft.RefineRecipe>> REFINING =
            SERIALIZERS.register("refining", com.jttw.engine.item.craft.RefineRecipe.Serializer::new);

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        SERIALIZERS.register(modEventBus);
    }
}
