package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The mod's own attributes: physical and magic resistance.
 *
 * <p>They replace vanilla armor as the home of a character's earned defence. They have no
 * practical cap; {@link com.jttw.engine.progression.stats.ResistanceHandler} turns them into
 * damage reduction with diminishing returns.
 */
public final class ModAttributes {

    private ModAttributes() {}

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, JTTWEngine.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> PHYSICAL_RESISTANCE =
            ATTRIBUTES.register("physical_resistance", () ->
                    new RangedAttribute("attribute.name.jttw_engine.physical_resistance", 0.0, 0.0, 100000.0)
                            .setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> MAGIC_RESISTANCE =
            ATTRIBUTES.register("magic_resistance", () ->
                    new RangedAttribute("attribute.name.jttw_engine.magic_resistance", 0.0, 0.0, 100000.0)
                            .setSyncable(true));

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(ModAttributes::onAttributeModify);
    }

    private static void onAttributeModify(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, PHYSICAL_RESISTANCE);
        event.add(EntityType.PLAYER, MAGIC_RESISTANCE);
    }
}
