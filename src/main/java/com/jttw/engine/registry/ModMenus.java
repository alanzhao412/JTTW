package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.furnace.TrigramsFurnaceMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** The mod's menus (container screens). */
public final class ModMenus {

    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, JTTWEngine.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<TrigramsFurnaceMenu>> TRIGRAMS_FURNACE =
            MENUS.register("eight_trigrams_furnace", () -> IMenuTypeExtension.create(TrigramsFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<com.jttw.engine.item.ArmoryMenu>> ARMORY =
            MENUS.register("armory", () -> IMenuTypeExtension.create(com.jttw.engine.item.ArmoryMenu::new));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
