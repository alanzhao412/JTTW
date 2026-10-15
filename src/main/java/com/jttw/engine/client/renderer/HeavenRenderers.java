package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.flight.NimbusCloud;
import com.jttw.engine.entity.heaven.HeavenlySoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** How Heaven's host looks: soldiers in gold, generals in crimson, each riding a small white cloud. */
public final class HeavenRenderers {

    private HeavenRenderers() {}

    public static final ResourceLocation SOLDIER = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/heavenly_soldier.png");
    public static final ResourceLocation GENERAL = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/heavenly_general.png");
    /** Names over heads show only this close, so a host is not a wall of labels. */
    private static final double NAME_RANGE = 24;
    /** The cloud a soldier rides, against Wukong's: a small one. */
    private static final float CLOUD_SIZE = 0.55f;

    public static final class SoldierRenderer extends HumanoidMobRenderer<HeavenlySoldierEntity, HumanoidModel<HeavenlySoldierEntity>> {
        public SoldierRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
        }

        @Override
        public void render(HeavenlySoldierEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            super.render(e, yaw, partial, pose, buffers, light);
            // the cloud it rides, under its feet
            pose.pushPose();
            float s = CLOUD_SIZE * e.getScale();
            pose.scale(s, s, s);
            NimbusCloud.draw(pose, buffers, e, 1f, light);
            pose.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(HeavenlySoldierEntity e) {
            return e.isGeneral() ? GENERAL : SOLDIER;
        }

        @Override
        protected boolean shouldShowName(HeavenlySoldierEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < NAME_RANGE * NAME_RANGE;
        }
    }
}
