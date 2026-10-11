package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.saurian.SaurianAbilityHandler;
import com.jttw.engine.item.Charged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Above the crosshair, while something is being charged: how far, as a percentage and a bar. Grey
 * until letting go would strike at all; gold when full. Items say how charged they are (Charged);
 * an ability charged by holding its key (the Saurian's Riptide Surge) is timed here, from the
 * moment its key went down.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class ChargeMeter {

    private ChargeMeter() {}

    /** The held ability being charged, and the game time its key went down; null if none. */
    @Nullable private static String heldAbility;
    private static long heldSince;

    /** Its key went down: if it is an ability that charges, start timing it. */
    public static void pressed(@Nullable String abilityId) {
        if (abilityId == null || fullCharge(abilityId) <= 0 || ClientAbilityData.isOnCooldown(abilityId)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        heldAbility = abilityId;
        heldSince = mc.level.getGameTime();
    }

    /** Its key came up. */
    public static void released() {
        heldAbility = null;
    }

    /** Ticks to a full charge of a held ability; 0 if it does not charge. */
    private static int fullCharge(String abilityId) {
        return "sdk_surge".equals(abilityId) ? SaurianAbilityHandler.FULL_CHARGE : 0;
    }

    @Nullable
    static Charged.Charge reading(Minecraft mc) {
        var p = mc.player;
        if (p == null || mc.level == null) return null;
        if (p.isUsingItem()) {
            ItemStack stack = p.getUseItem();
            if (stack.getItem() instanceof Charged c)
                return c.charge(stack, p, p.getTicksUsingItem(), ClientAbilityData.getUnlockedNodes());
            return null;
        }
        if (heldAbility != null) {
            long held = mc.level.getGameTime() - heldSince;
            // the surge: a quarter at once, full after a second
            return new Charged.Charge(Math.max(0.25f, Math.min(1f, held / (float) fullCharge(heldAbility))), true);
        }
        return null;
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        Charged.Charge c = reading(mc);
        if (c == null) return;
        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        boolean full = c.ready() && c.fraction() >= 1f;
        int colour = !c.ready() ? 0xFF9A9A9A : full ? 0xFFFFD24A : 0xFFFFFFFF;
        String text = Math.round(c.fraction() * 100) + "%";
        int barW = 40, x0 = (w - barW) / 2, y = h / 2 - 22;
        g.drawString(font, text, (w - font.width(text)) / 2, y - 10, colour, true);
        g.fill(x0 - 1, y - 1, x0 + barW + 1, y + 3, 0xAA000000);
        g.fill(x0, y, x0 + Math.round(barW * c.fraction()), y + 2, colour);
    }
}
