package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/** The mod's status effects. */
public final class ModEffects {

    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, JTTWEngine.MOD_ID);

    /** Shows the Curse of the Dead (see {@link com.jttw.engine.world.CurseOfTheDead}); does nothing itself, and no milk cures it. */
    public static final RegistryObject<MobEffect> CURSE_OF_THE_DEAD = EFFECTS.register("curse_of_the_dead", Mark::new);

    /** The Saurian Demon King's Flood-Dragon's Wrath, while it lasts (see SaurianAbilityHandler). */
    public static final RegistryObject<MobEffect> FLOOD_WRATH = EFFECTS.register("flood_wrath",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x2E8BC0) {});

    /** The Phoenix's Solar Flare, while it burns (see SolarFlare). */
    public static final RegistryObject<MobEffect> SOLAR_FLARE = EFFECTS.register("solar_flare",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFC83C) {});

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
