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
 * The Ruyi Jingu Bang at ease (StaffStance): resting on the right shoulder, slanting up and back
 * behind him, the right hand loosely holding it low in front (HumanoidModelMixin poses the arm).
 */
public class StaffRestLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Where it rests on the body (the top of the right shoulder), in blocks of the body's frame (x to his left, y down, z back). */
    public static final float X = -0.31f, Y = -0.03f, Z = 0.04f;
    /** How it slants: back from upright, and out from the head (degrees). */
    public static final float BACK = 45f, OUT = 12f;

    public StaffRestLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p,
                       float swing, float swingAmount, float partial, float age, float headYaw, float headPitch) {
        if (p.isInvisible() || !StaffStance.resting(p)) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        pose.translate(X, Y, Z);
        // slanting up and back over the shoulder (and a little out from the head); then, the model's
        // frame being upside down, the item righted along that slant
        pose.mulPose(Axis.XP.rotationDegrees(-BACK));
        pose.mulPose(Axis.ZP.rotationDegrees(-OUT));
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        Minecraft.getInstance().getItemRenderer().renderStatic(p, p.getMainHandItem(), ItemDisplayContext.NONE, false,
                pose, buffers, p.level(), light, OverlayTexture.NO_OVERLAY, p.getId());
        pose.popPose();
    }
}
