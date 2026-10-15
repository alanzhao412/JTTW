package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/** A player in another player's form shows that player's name over their head. */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class DisguiseNameTags {

    private DisguiseNameTags() {}

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        TransformationData data = TransformationManager.getTransformation(player);
        if (data != null && data.isPlayerForm() && data.getDisguiseName() != null) {
            event.setContent(Component.literal(data.getDisguiseName()));
        }
    }
}
