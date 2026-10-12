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
        // a Qi-empowered staff's glint is gold, not the enchantment's purple
        com.jttw.engine.client.renderer.GoldGlint.ACTIVE.set(stack.getItem() instanceof RuyiJinguBangItem && RuyiJinguBangItem.isQiEmpowered(stack));
        boolean largeAxe = stack.getItem() instanceof com.jttw.engine.item.ErlangAxeItem
                && com.jttw.engine.item.ErlangAxeItem.isLarge(stack);
        if (!(stack.getItem() instanceof RuyiJinguBangItem) && !largeAxe) return;

        boolean isHandContext = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        if (!isHandContext) return;

        if (largeAxe) {
            poseStack.scale(2.0f, 2.0f, 2.0f); // Erlang's axe, enlarged: twice its size
        } else if (RuyiJinguBangItem.isLargeMode(stack)) {
            // three times its plain size; in first person, where the plain staff is held at 0.68
            // rather than the old 0.4, the same size in view as before
            boolean firstPerson = displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            float k = firstPerson ? 3.0f * 0.4f / 0.68f : 3.0f;
            poseStack.scale(k, k, k);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void jttw_onRenderReturn(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand,
                                      PoseStack poseStack, MultiBufferSource buffer, int combinedLight,
                                      int combinedOverlay, BakedModel model, CallbackInfo ci) {
        com.jttw.engine.client.renderer.GoldGlint.ACTIVE.set(false);
    }

    @Inject(method = "getFoilBuffer", at = @At("HEAD"), cancellable = true)
    private static void jttw_goldFoil(MultiBufferSource buffer, net.minecraft.client.renderer.RenderType type, boolean isItem, boolean glint,
                                      org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<com.mojang.blaze3d.vertex.VertexConsumer> cir) {
        if (glint && com.jttw.engine.client.renderer.GoldGlint.ACTIVE.get()) {
            cir.setReturnValue(com.mojang.blaze3d.vertex.VertexMultiConsumer.create(
                    buffer.getBuffer(com.jttw.engine.client.renderer.GoldGlint.ITEM), buffer.getBuffer(type)));
        }
    }

    @Inject(method = "getFoilBufferDirect", at = @At("HEAD"), cancellable = true)
    private static void jttw_goldFoilDirect(MultiBufferSource buffer, net.minecraft.client.renderer.RenderType type, boolean noEntity, boolean glint,
                                            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<com.mojang.blaze3d.vertex.VertexConsumer> cir) {
        if (glint && com.jttw.engine.client.renderer.GoldGlint.ACTIVE.get()) {
            cir.setReturnValue(com.mojang.blaze3d.vertex.VertexMultiConsumer.create(
                    buffer.getBuffer(com.jttw.engine.client.renderer.GoldGlint.DIRECT), buffer.getBuffer(type)));
        }
    }
}
