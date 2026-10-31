package com.jttw.engine.mixin;

import com.jttw.engine.client.FightingBuddhaClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No cape on the Victorious Fighting Buddha: his kasaya and mandorla are behind him (see FightingBuddhaLayer). */
@Mixin(CapeLayer.class)
public class BuddhaCapeMixin {

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void jttw_buddhaNoCape(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                   float swing, float swingAmount, float partial, float age, float headYaw, float headPitch,
                                   CallbackInfo ci) {
        if (FightingBuddhaClient.drawnAsBuddha(player)) ci.cancel();
    }
}
