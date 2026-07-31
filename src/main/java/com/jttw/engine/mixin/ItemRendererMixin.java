package com.jttw.engine.mixin;

import com.jttw.engine.item.RuyiJinguBangItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handles Ruyi Jingu Bang visual effects:
 * - Large mode: 3x scale
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void jttw_onRenderHead(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand,
                                    PoseStack poseStack, MultiBufferSource buffer, int combinedLight,
                                    int combinedOverlay, BakedModel model, CallbackInfo ci) {
        if (!(stack.getItem() instanceof RuyiJinguBangItem)) return;

        boolean isHandContext = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        if (!isHandContext) return;

        if (RuyiJinguBangItem.isLargeMode(stack)) {
            poseStack.scale(3.0f, 3.0f, 3.0f);
        }
    }
}
