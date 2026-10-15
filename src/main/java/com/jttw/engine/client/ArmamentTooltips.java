package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.Armaments;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/** Every armament's tooltip says how far it is refined, and where that is done. */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class ArmamentTooltips {

    private ArmamentTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (Armaments.of(event.getItemStack()) == null) return;
        int level = Armaments.refine(event.getItemStack());
        event.getToolTip().add(1, Component.literal("§6Refined " + level + "/" + Armaments.MAX_REFINE
                + (level > 0 ? " §7(+" + level * Armaments.PER_LEVEL_PERCENT + "%)" : "")));
        if (event.getFlags().isAdvanced() || level < Armaments.MAX_REFINE) {
            event.getToolTip().add(2, Component.literal("§8Refined in the Eight Trigrams Furnace"));
        }
    }
}
