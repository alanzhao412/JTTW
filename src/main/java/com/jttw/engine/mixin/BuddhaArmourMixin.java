package com.jttw.engine.mixin;

import com.jttw.engine.client.FightingBuddhaClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The Victorious Fighting Buddha wears his robe and kasaya, not his armour: it is not drawn on him (see FightingBuddhaLayer). */
@Mixin(HumanoidArmorLayer.class)
public class BuddhaArmourMixin {

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void jttw_buddhaRobes(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity,
                                  float swing, float swingAmount, float partial, float age, float headYaw, float headPitch,
                                  CallbackInfo ci) {
        if (FightingBuddhaClient.drawnAsBuddha(entity)) ci.cancel();
    }
}
