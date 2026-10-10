package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.furnace.TrigramsFurnaceMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's menus (container screens). */
public final class ModMenus {

    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, JTTWEngine.MOD_ID);

    public static final RegistryObject<MenuType<TrigramsFurnaceMenu>> TRIGRAMS_FURNACE =
            MENUS.register("eight_trigrams_furnace", () -> IForgeMenuType.create(TrigramsFurnaceMenu::new));

    public static final RegistryObject<MenuType<com.jttw.engine.item.ArmoryMenu>> ARMORY =
            MENUS.register("armory", () -> IForgeMenuType.create(com.jttw.engine.item.ArmoryMenu::new));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
