package com.jttw.engine.client.renderer;

import com.jttw.engine.entity.ThrownSpearEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Draws a thrown spear with the same 3D model it has in the hand, full size, its long axis
 * (the model's +Y, prongs first) pointing along its flight.
 */
public class ThrownSpearRenderer extends EntityRenderer<ThrownSpearEntity> {

    private final ItemRenderer itemRenderer;

    public ThrownSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownSpearEntity spear, float entityYaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        float yaw = Mth.lerp(partialTicks, spear.yRotO, spear.getYRot());
        float pitch = Mth.lerp(partialTicks, spear.xRotO, spear.getXRot());
        // yaw turns +Z toward the flight's heading; then lay the upright model (+Y) along +Z,
        // raised by the flight's pitch
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(90f - pitch));
        itemRenderer.renderStatic(spear.getItem(), ItemDisplayContext.NONE, packedLight,
                OverlayTexture.NO_OVERLAY, pose, buffers, spear.level(), spear.getId());
        pose.popPose();
        super.render(spear, entityYaw, partialTicks, pose, buffers, packedLight);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(ThrownSpearEntity spear) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
