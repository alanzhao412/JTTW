package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.gui.SignFormScreen;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.UseFormPacket;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SignBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Right-clicking with an empty hand while transformed uses the form: Forge reports empty-hand
 * clicks only on the client, so this passes them to the server ({@link UseFormPacket}), or, for a
 * sign form, opens the screen to write on it.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class FormUseClient {

    private FormUseClient() {}

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        use(event.getEntity());
    }

    /**
     * Pointing at a block: block forms (and the chest boat) use themselves instead of the
     * block, so a chest form opens itself rather than the door in front of it.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        if (!player.getMainHandItem().isEmpty() || !usesItself(player)) return;
        use(player);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean usesItself(Player player) {
        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null || data.isPlayerForm()) return false;
        return data.isBlockForm() || data.getEntityType() == EntityType.CHEST_BOAT;
    }

    private static void use(Player player) {
        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null || !player.getMainHandItem().isEmpty()) return;
        if (data.getBlock() instanceof SignBlock) {
            Minecraft.getInstance().setScreen(new SignFormScreen(data.getSignLines()));
            return;
        }
        NetworkHandler.sendToServer(new UseFormPacket());
    }
}
