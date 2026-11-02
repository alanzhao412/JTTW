package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.Armaments;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * Every armament's tooltip says its tier, the ring of levels it is made for, and its next tier's recipe:
 * where it is made and from what (read from the recipes, {@link com.jttw.engine.item.craft.ArmamentGuide}).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class ArmamentTooltips {

    private ArmamentTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (Armaments.of(event.getItemStack()) == null) return;
        int tier = Armaments.tier(event.getItemStack());
        var lines = event.getToolTip();
        int at = Math.min(1, lines.size());
        lines.add(at++, Component.literal("§6Tier " + Armaments.roman(tier) + " §7(made for " + Armaments.ring(tier) + ")"));
        var level = net.minecraft.client.Minecraft.getInstance().level;
        var next = level == null ? null : com.jttw.engine.item.craft.ArmamentGuide.next(level.getRecipeManager(), event.getItemStack());
        if (tier >= Armaments.TIERS) {
            lines.add(at, Component.literal("§8Fully refined."));
        } else if (next != null) {
            lines.add(at++, Component.literal("§7Tier " + Armaments.roman(next.tier()) + " (" + Armaments.ring(next.tier()) + ") at " + next.station() + ":"));
            lines.add(at++, Component.literal("§f " + next.materials()));
            lines.add(at, Component.literal("§8 " + next.gate().describe()));
        }
    }
}
