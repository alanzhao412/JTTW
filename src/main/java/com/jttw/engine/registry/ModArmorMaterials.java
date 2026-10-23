package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/** The mod's armour materials. */
public final class ModArmorMaterials {

    private ModArmorMaterials() {}

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, JTTWEngine.MOD_ID);

    /**
     * The Dragon Kings' regalia, the gifts the Monkey King took from the Four Seas: the cap, the mail
     * and the boots. As netherite against blows (and the mod's own resistances besides: see
     * DragonRegaliaItem); worn, drawn from {@code textures/models/armor/dragon_regalia_layer_1.png}.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DRAGON_REGALIA = MATERIALS.register("dragon_regalia", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        return new ArmorMaterial(defense, 25, SoundEvents.ARMOR_EQUIP_GOLD, () -> Ingredient.of(Items.GOLD_INGOT),
                List.of(new ArmorMaterial.Layer(JTTWEngine.id("dragon_regalia"))), 3f, 0.1f);
    });

    public static void register(IEventBus bus) {
        MATERIALS.register(bus);
    }
}
