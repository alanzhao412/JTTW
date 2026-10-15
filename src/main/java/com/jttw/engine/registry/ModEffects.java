package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/** The mod's status effects. */
public final class ModEffects {

    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, JTTWEngine.MOD_ID);

    /** Shows the Curse of the Dead (see {@link com.jttw.engine.world.CurseOfTheDead}); does nothing itself, and no milk cures it. */
    public static final DeferredHolder<MobEffect, MobEffect> CURSE_OF_THE_DEAD = EFFECTS.register("curse_of_the_dead", Mark::new);

    /** The Saurian Demon King's Flood-Dragon's Wrath, while it lasts (see SaurianAbilityHandler). */
    public static final DeferredHolder<MobEffect, MobEffect> FLOOD_WRATH = EFFECTS.register("flood_wrath",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x2E8BC0) {});

    /** The Phoenix's Solar Flare, while it burns (see SolarFlare). */
    public static final DeferredHolder<MobEffect, MobEffect> SOLAR_FLARE = EFFECTS.register("solar_flare",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFC83C) {});

    /** After a divine beast's true form: no ability answers and Qi does not come back (see TrueForm). */
    public static final DeferredHolder<MobEffect, MobEffect> EXHAUSTED = EFFECTS.register("exhausted",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x6B6B78) {});

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }

    private static final class Mark extends MobEffect {
        Mark() {
            super(MobEffectCategory.HARMFUL, 0x4B2A6B);
        }

        @Override
        public List<ItemStack> getCurativeItems() {
            return List.of();
        }
    }
}
