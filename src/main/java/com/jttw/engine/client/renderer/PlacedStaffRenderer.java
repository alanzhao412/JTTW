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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the placed Ruyi Jingu Bang in the world, scaled by the entity's scale factor.
 */
public class PlacedStaffRenderer extends EntityRenderer<PlacedStaffEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/item/ruyi_jingu_bang_base.png");

    public PlacedStaffRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    /**
     * Drawn whenever any of it could be on screen: the whole length of it, standing or lying,
     * is tested against the view, not the small box the game keeps for it.
     */
    @Override
    public boolean shouldRender(PlacedStaffEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        if (!entity.shouldRender(camX, camY, camZ)) return false;
        float r = entity.radius() + 1;
        AABB whole = entity.isRod()
                ? new AABB(entity.rodBase(), entity.rodTip()).inflate(r)
                : new AABB(entity.getX(), entity.getY(), entity.getZ(), entity.getX(), entity.getY() + entity.length() * 1.05, entity.getZ()).inflate(r);
        return frustum.isVisible(whole);
    }

    @Override
    public void render(PlacedStaffEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // drawn at its eased size, so it grows smoothly from one size to the next
        float scale = entity.shownScale(partialTick);
        // The staff model extends from Y=-15.8 to Y=31.2 in model space. After FIXED display
        // normalization (/16, -0.5), the bottom is at about -1.49: lifted by that so its foot is
        // its base and it grows upward.
        float baseOffset = 0.74f;

        if (entity.isRod()) {
            // fallen (or falling): along its two ends, turned about its length as far as it has rolled
            Vec3 foot = entity.shownRodEnd(0, partialTick), tip = entity.shownRodEnd(1, partialTick);
            Vec3 at = entity.getPosition(partialTick);
            Vec3 axis = tip.subtract(foot);
            poseStack.translate(foot.x - at.x, foot.y - at.y, foot.z - at.z);
            if (axis.lengthSqr() > 1e-6) {
                Vec3 n = axis.normalize();
                poseStack.mulPose(new Quaternionf().rotationTo(0, 1, 0, (float) n.x, (float) n.y, (float) n.z));
            }
            poseStack.mulPose(com.mojang.math.Axis.YP.rotation(entity.rodSpin()));
        }
        poseStack.translate(0.0, baseOffset * scale, 0.0);
        poseStack.scale(scale, scale, scale);

        // Render as the item model; full brightness so it does not darken underwater or in shadow
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
