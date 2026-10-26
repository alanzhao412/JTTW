package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.furnace.TrigramsRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's recipe types: what the Eight Trigrams Furnace forges besides the armaments. */
public final class ModRecipes {

    private ModRecipes() {}

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, JTTWEngine.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, JTTWEngine.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<TrigramsRecipe>> TRIGRAMS_FORGING =
            TYPES.register("trigrams_forging", () -> RecipeType.simple(JTTWEngine.id("trigrams_forging")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TrigramsRecipe>> TRIGRAMS_FORGING_SERIALIZER =
            SERIALIZERS.register("trigrams_forging", TrigramsRecipe.Serializer::new);

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        SERIALIZERS.register(modEventBus);
    }
}
