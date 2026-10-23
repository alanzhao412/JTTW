package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.waters.ShrimpSoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Shrimp Soldier: a red-shelled shrimp the height of a man, upright on its legs, its shell in
 * segments, a spiked rostrum over its face, stalked black eyes and long feelers sweeping back, a
 * curled tail ending in a fan, a spear levelled in its right hand. Swimming, its legs kick and its
 * tail beats; it thrusts with the spear. Model and skin: {@code assets_source/sea_court.py}.
 */
public final class ShrimpSoldierRenderer extends MobRenderer<ShrimpSoldierEntity, ShrimpSoldierRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("shrimp_soldier"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/shrimp_soldier.png");

    public ShrimpSoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ShrimpSoldierEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(ShrimpSoldierEntity e) {
        return super.shouldShowName(e) && SeaCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(44, 0).mirror(false).addBox(-4, 0, -2.5f, 8, 12, 5)
                .texOffs(13, 32).mirror(false).addBox(-3.5f, 1, -3, 7, 10, 0.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(88, 0).mirror(false).addBox(-3.5f, -7, -5, 7, 7, 8)
                .texOffs(30, 32).mirror(false).addBox(-0.5f, -5.5f, -11, 1, 1.5f, 6)
                .texOffs(25, 48).mirror(false).addBox(-2, -1, -5.5f, 4, 1.5f, 2), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(38, 48).mirror(false).addBox(-1.8f, -0.9f, -0.9f, 1.8f, 1.8f, 1.8f), PartPose.offsetAndRotation(-2.6f, -6.5f, -4.5f, 0, 0.3f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(38, 48).mirror(true).addBox(0, -0.9f, -0.9f, 1.8f, 1.8f, 1.8f), PartPose.offsetAndRotation(2.6f, -6.5f, -4.5f, 0, -0.3f, 0));
        PartDefinition rightAntenna = head.addOrReplaceChild("right_antenna", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-0.3f, -0.3f, 0, 0.6f, 0.6f, 18), PartPose.offsetAndRotation(-1.5f, -6.5f, -5, 0.3f, 0.35f, 0));
        PartDefinition leftAntenna = head.addOrReplaceChild("left_antenna", CubeListBuilder.create()
                .texOffs(5, 0).mirror(true).addBox(-0.3f, -0.3f, 0, 0.6f, 0.6f, 18), PartPose.offsetAndRotation(1.5f, -6.5f, -5, 0.3f, -0.35f, 0));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 32).mirror(false).addBox(-2, -2, -1.5f, 3, 12, 3), PartPose.offsetAndRotation(-5, 2, 0, 0, 0, 0));
        PartDefinition spear = rightArm.addOrReplaceChild("spear", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5f, -18, -0.5f, 1, 30, 1)
                .texOffs(85, 32).mirror(false).addBox(-1, -23, -0.5f, 2, 5, 1)
                .texOffs(47, 48).mirror(false).addBox(-1.5f, -18.5f, -1, 3, 1, 2), PartPose.offsetAndRotation(-0.5f, 9, 0, 1.5708f, 0, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 32).mirror(true).addBox(-1, -2, -1.5f, 3, 12, 3), PartPose.offsetAndRotation(5, 2, 0, 0, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(71, 0).mirror(false).addBox(-2, 0, -2, 4, 12, 4), PartPose.offsetAndRotation(-2, 12, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(71, 0).mirror(true).addBox(-2, 0, -2, 4, 12, 4), PartPose.offsetAndRotation(2, 12, 0, 0, 0, 0));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(45, 32).mirror(false).addBox(-3, -2, 0, 6, 4, 4)
                .texOffs(66, 32).mirror(false).addBox(-2.5f, -1.5f, 3.5f, 5, 3, 4)
                .texOffs(92, 32).mirror(false).addBox(-2, -1, 7, 4, 2, 3), PartPose.offsetAndRotation(0, 9, 2.5f, 0.7f, 0, 0));
        PartDefinition tailFan = tail.addOrReplaceChild("tail_fan", CubeListBuilder.create()
                .texOffs(0, 48).mirror(false).addBox(-4, -0.5f, 0, 8, 1, 4), PartPose.offsetAndRotation(0, 0, 9.5f, 0.4f, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<ShrimpSoldierEntity> {
        private final ModelPart root, head, rightArm, leftArm, rightLeg, leftLeg, tail, fan, rightAntenna, leftAntenna;

        public Model(ModelPart root) {
            this.root = root;
            head = root.getChild("head");
            rightArm = root.getChild("right_arm");
            leftArm = root.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            tail = root.getChild("tail");
            fan = tail.getChild("tail_fan");
            rightAntenna = head.getChild("right_antenna");
            leftAntenna = head.getChild("left_antenna");
        }

        @Override
        public void setupAnim(ShrimpSoldierEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            boolean swimming = e.isInWater();
            float kick = swimming ? Mth.sin(age * 0.35f) * (0.25f + amount * 0.6f) : Mth.cos(swing * 0.66f) * 1.1f * amount;
            rightLeg.xRot = kick;
            leftLeg.xRot = -kick;
            leftArm.xRot = -kick * 0.6f;
            // the spear levelled in front; thrust on an attack
            rightArm.xRot = -0.35f - Mth.sin(attackTime * Mth.PI) * 1.1f;
            rightArm.z = -Mth.sin(attackTime * Mth.PI) * 3f;
            float beat = Mth.sin(age * (swimming ? 0.4f : 0.12f));
            tail.xRot = tail.getInitialPose().xRot + beat * (swimming ? 0.35f : 0.08f);
            fan.xRot = fan.getInitialPose().xRot + beat * 0.3f;
            rightAntenna.yRot = rightAntenna.getInitialPose().yRot + Mth.sin(age * 0.09f) * 0.15f;
            leftAntenna.yRot = leftAntenna.getInitialPose().yRot - Mth.sin(age * 0.09f + 1f) * 0.15f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
