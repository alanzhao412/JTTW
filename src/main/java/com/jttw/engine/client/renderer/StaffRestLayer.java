package com.jttw.engine.client.renderer;

import com.jttw.engine.client.StaffStance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * The Ruyi Jingu Bang at ease (StaffStance): across the shoulders behind the neck, level, its
 * middle at the nape, the arms draped over it (HumanoidModelMixin poses them).
 */
public class StaffRestLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Where it lies on the body, in blocks of the body's own frame (y down, z to the back). */
    public static final float Y = 0.02f, Z = 0.22f;

    public StaffRestLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p,
                       float swing, float swingAmount, float partial, float age, float headYaw, float headPitch) {
        if (p.isInvisible() || !StaffStance.resting(p)) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        pose.translate(0, Y, Z);
        // the model's frame is upside down: right the item, then lay its length across the shoulders
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        Minecraft.getInstance().getItemRenderer().renderStatic(p, p.getMainHandItem(), ItemDisplayContext.NONE, false,
                pose, buffers, p.level(), light, OverlayTexture.NO_OVERLAY, p.getId());
        pose.popPose();
    }
}
