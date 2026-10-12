package com.jttw.engine.mixin;

import com.jttw.engine.client.StaffStance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The staff at ease is across the shoulders (StaffRestLayer), not in the hand as well. */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void jttw_staffAtEase(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                  PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (entity instanceof Player p && stack == p.getMainHandItem() && StaffStance.resting(p)) ci.cancel();
    }
}
