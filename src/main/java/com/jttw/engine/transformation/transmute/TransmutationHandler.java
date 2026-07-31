package com.jttw.engine.transformation.transmute;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.gui.TransmuteScreen;
import com.jttw.engine.progression.SkillTreeData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client-only handler for shift+right-click detection for T3 item transmutation.
 * Opens the transmutation GUI when conditions are met.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransmutationHandler {

    private TransmutationHandler() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) return;
        if (!player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;

        // Check T3 transform tier (client-side check; server will re-validate)
        if (!hasTransformT3(player)) return;

        TransmutationCategory category = TransmutationHelper.detectCategory(held);
        if (category == null) return;

        List<Item> options = TransmutationHelper.getOptions(category, held);
        if (options.isEmpty()) return;

        Minecraft.getInstance().setScreen(new TransmuteScreen(category, options));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) return;
        if (!player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;

        if (!hasTransformT3(player)) return;

        TransmutationCategory category = TransmutationHelper.detectCategory(held);
        if (category == null) return;

        List<Item> options = TransmutationHelper.getOptions(category, held);
        if (options.isEmpty()) return;

        Minecraft.getInstance().setScreen(new TransmuteScreen(category, options));
        event.setCanceled(true);
    }

    /**
     * Client-side check for T3 transform or Marshal (who gets transmutation innately).
     * Uses SkillTreeData which reads from persistent player NBT.
     */
    private static boolean hasTransformT3(Player player) {
        // Marshal of the Heavenly Canopy gets transmutation innately
        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        if ("marshal_of_the_heavenly_canopy".equals(outcomeId)) return true;

        int wuTier = SkillTreeData.getTransformTier(player);
        int esTier = SkillTreeData.getEsTransformTier(player);
        return wuTier >= 3 || esTier >= 3;
    }
}
