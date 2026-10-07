package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, JTTWEngine.MOD_ID);

    public static final RegistryObject<Attribute> PHYSICAL_RESISTANCE =
            ATTRIBUTES.register("physical_resistance", () ->
                    new RangedAttribute("attribute.name.jttw_engine.physical_resistance", 0.0, 0.0, 100000.0)
                            .setSyncable(true));

    public static final RegistryObject<Attribute> MAGIC_RESISTANCE =
            ATTRIBUTES.register("magic_resistance", () ->
                    new RangedAttribute("attribute.name.jttw_engine.magic_resistance", 0.0, 0.0, 100000.0)
                            .setSyncable(true));

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(ModAttributes::onAttributeModify);
    }

    private static void onAttributeModify(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, PHYSICAL_RESISTANCE.get());
        event.add(EntityType.PLAYER, MAGIC_RESISTANCE.get());
    }
}
