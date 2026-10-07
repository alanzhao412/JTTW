package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A player in another player's form shows that player's name over their head. */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
