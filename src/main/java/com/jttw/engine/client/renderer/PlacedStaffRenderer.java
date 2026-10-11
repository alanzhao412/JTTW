package com.jttw.engine.client.renderer;

import com.jttw.engine.registry.ModItems;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.PlacedStaffEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the placed Ruyi Jingu Bang in the world, scaled by the entity's scale factor.
 */
public class PlacedStaffRenderer extends EntityRenderer<PlacedStaffEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(JTTWEngine.MOD_ID, "textures/item/ruyi_jingu_bang_base.png");

    public PlacedStaffRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(PlacedStaffEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // drawn at its eased size, so it grows smoothly from one size to the next
        float scale = entity.shownScale(partialTick);

        // The staff model extends from Y=-15.8 to Y=31.2 in model space.
        // After FIXED display normalization (÷16, -0.5), the bottom is at ~-1.49.
        // Translate up by that amount × scale so the base stays on the ground
        // and the staff only grows upward when scaled.
        float baseOffset = 0.74f;
        poseStack.translate(0.0, baseOffset * scale, 0.0);

        // Topple animation: rotate from upright to flat over ~10 ticks
        // Falls in the direction the owner was looking when they punched it
        if (entity.isFalling()) {
            float elapsed = entity.getFallAnimTicks() + partialTick;
            float toppleDuration = 10.0f;
            float progress = Math.min(elapsed / toppleDuration, 1.0f);
            // Ease-in (accelerating fall like gravity)
            float angle = progress * progress * 90.0f;
            // Pivot at the base, rotated to match the topple yaw
            poseStack.translate(0.0, -baseOffset * scale, 0.0);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entity.getToppleYaw()));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(entity.getToppleYaw()));
            poseStack.translate(0.0, baseOffset * scale, 0.0);
        }

        poseStack.scale(scale, scale, scale);

        // Render as the item model (standing upright)
        // Use full brightness so the staff doesn't darken underwater or in shadows
        ItemStack staffItem = new ItemStack(ModItems.RUYI_JINGU_BANG.get());
        Minecraft.getInstance().getItemRenderer().renderStatic(
                staffItem, ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, entity.level(), entity.getId());

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PlacedStaffEntity entity) {
        return TEXTURE;
    }
}
