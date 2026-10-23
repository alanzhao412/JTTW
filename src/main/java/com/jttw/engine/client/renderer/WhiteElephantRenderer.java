package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.WhiteElephantEntity;
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
 * The White Elephant: a towering demon king with an elephant's head, ivory-white, six gold-capped
 * tusks (Samantabhadra's elephant had six), great ears, a long trunk in three joints, a gold
 * crown; robes of deep blue under gold collar and belt; a long lance. His trunk sways; rearing to
 * strike it lifts high, and holding a foe it is coiled up before him; stamping, he rears back.
 * Model and skin: {@code assets_source/lion_camel.py}.
 */
public final class WhiteElephantRenderer extends MobRenderer<WhiteElephantEntity, WhiteElephantRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("white_elephant"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/white_elephant.png");

    public WhiteElephantRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.1f);
        addLayer(LionCourt.glow(this, "white_elephant"));
    }

    @Override
    public ResourceLocation getTextureLocation(WhiteElephantEntity e) {
        return TEXTURE;
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-9, -24, -5.5f, 18, 24, 11)
                .texOffs(165, 55).mirror(false).addBox(-7.5f, -25, -5.2f, 15, 3, 10.4f)
                .texOffs(63, 55).mirror(false).addBox(-9.6f, -7, -6, 19.2f, 3, 12)
                .texOffs(0, 55).mirror(false).addBox(-9.4f, -4, -5.8f, 18.8f, 8, 11.6f)
                .texOffs(165, 76).mirror(false).addBox(-2, -21, -6, 4, 4, 0.6f), PartPose.offsetAndRotation(0, 4, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(126, 0).mirror(false).addBox(-6.5f, -13, -7.5f, 13, 13, 11)
                .texOffs(0, 76).mirror(false).addBox(-5, -16, -5, 10, 3, 8)
                .texOffs(189, 76).mirror(false).addBox(-1, -18, -2, 2, 2, 2)
                .texOffs(116, 76).mirror(false).addBox(-5, -3, -14, 1.6f, 1.6f, 7)
                .texOffs(116, 76).mirror(false).addBox(3.4f, -3, -14, 1.6f, 1.6f, 7)
                .texOffs(135, 76).mirror(false).addBox(-6.3f, -5.5f, -11, 1.4f, 1.4f, 4)
                .texOffs(135, 76).mirror(false).addBox(4.9f, -5.5f, -11, 1.4f, 1.4f, 4)
                .texOffs(135, 76).mirror(false).addBox(-4.2f, -0.8f, -11, 1.3f, 1.3f, 3.5f)
                .texOffs(135, 76).mirror(false).addBox(2.9f, -0.8f, -11, 1.3f, 1.3f, 3.5f), PartPose.offsetAndRotation(0, -25, -1, 0, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(68, 0).mirror(false).addBox(-1, 0, 0, 1, 15, 13), PartPose.offsetAndRotation(-6.5f, -12, -3, 0, -0.4f, 0));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(68, 0).mirror(true).addBox(0, 0, 0, 1, 15, 13), PartPose.offsetAndRotation(6.5f, -12, -3, 0, 0.4f, 0));
        PartDefinition trunk1 = head.addOrReplaceChild("trunk1", CubeListBuilder.create()
                .texOffs(218, 55).mirror(false).addBox(-2, 0, -2, 4, 8, 4), PartPose.offsetAndRotation(0, -5, -7.5f, 0.1f, 0, 0));
        PartDefinition trunk2 = trunk1.addOrReplaceChild("trunk2", CubeListBuilder.create()
                .texOffs(218, 55).mirror(false).addBox(-1.75f, 0, -1.75f, 3.5f, 8, 3.5f), PartPose.offsetAndRotation(0, 8, 0, 0.15f, 0, 0));
        PartDefinition trunk3 = trunk2.addOrReplaceChild("trunk3", CubeListBuilder.create()
                .texOffs(103, 76).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 7, 3)
                .texOffs(148, 76).mirror(false).addBox(-1.8f, 6, -1.8f, 3.6f, 1.6f, 3.6f), PartPose.offsetAndRotation(0, 8, 0, 0.2f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(97, 0).mirror(false).addBox(-3.5f, -2, -3.5f, 7, 21, 7)
                .texOffs(128, 55).mirror(false).addBox(-4.4f, -4, -4.4f, 8.8f, 6, 8.8f)
                .texOffs(37, 76).mirror(false).addBox(-3.9f, 14, -3.9f, 7.8f, 3, 7.8f), PartPose.offsetAndRotation(-11.5f, -21, 0, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(97, 0).mirror(true).addBox(-3.5f, -2, -3.5f, 7, 21, 7)
                .texOffs(128, 55).mirror(true).addBox(-4.4f, -4, -4.4f, 8.8f, 6, 8.8f)
                .texOffs(37, 76).mirror(true).addBox(-3.9f, 14, -3.9f, 7.8f, 3, 7.8f), PartPose.offsetAndRotation(11.5f, -21, 0, 0, 0, 0));
        PartDefinition lance = rightArm.addOrReplaceChild("lance", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.6f, -36, -0.6f, 1.2f, 52, 1.2f)
                .texOffs(235, 55).mirror(false).addBox(-0.7f, -44, -1.6f, 1.4f, 8, 3.2f)
                .texOffs(176, 76).mirror(false).addBox(-1.3f, -36.5f, -1.3f, 2.6f, 2, 2.6f), PartPose.offsetAndRotation(0, 18, 0, 1.5708f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(175, 0).mirror(false).addBox(-3.5f, 0, -3.5f, 7, 17, 7)
                .texOffs(70, 76).mirror(false).addBox(-4, 17, -4, 8, 3, 8), PartPose.offsetAndRotation(-4.8f, 4, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(175, 0).mirror(true).addBox(-3.5f, 0, -3.5f, 7, 17, 7)
                .texOffs(70, 76).mirror(true).addBox(-4, 17, -4, 8, 3, 8), PartPose.offsetAndRotation(4.8f, 4, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<WhiteElephantEntity> {
        private final ModelPart root, body, head, trunk1, trunk2, trunk3, rightEar, leftEar, rightArm, leftArm, rightLeg, leftLeg;

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            trunk1 = head.getChild("trunk1");
            trunk2 = trunk1.getChild("trunk2");
            trunk3 = trunk2.getChild("trunk3");
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(WhiteElephantEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = LionCourt.walk(rightLeg, leftLeg, swing, amount, 0.45f, 0.7f);
            body.xRot = 0;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.6f;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.5f;
            float sway = Mth.sin(age * 0.07f);
            // hanging, its tip curling forward (a negative turn swings it out before him)
            trunk1.xRot = -0.05f + sway * 0.07f;
            trunk2.xRot = -0.12f + sway * 0.09f;
            trunk3.xRot = -0.3f + sway * 0.12f;
            trunk1.zRot = Mth.cos(age * 0.05f) * 0.07f;
            float flap = Mth.sin(age * 0.08f) * 0.1f;
            rightEar.yRot = -0.4f - flap;
            leftEar.yRot = 0.4f + flap;
            float blow = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.35f - stride * 0.4f - blow * 1.5f;
            rightArm.zRot = 0.1f;
            leftArm.xRot = stride * 0.4f;
            leftArm.zRot = -0.1f;
            switch (e.trunk()) {
                case WhiteElephantEntity.TRUNK_REARING -> {
                    // raised high, about to lash out
                    trunk1.xRot = -2.5f;
                    trunk2.xRot = -0.4f;
                    trunk3.xRot = -0.3f;
                    head.xRot = -0.25f;
                }
                case WhiteElephantEntity.TRUNK_HOLDING -> {
                    // stretched out and up, coiled at its end round what it holds
                    trunk1.xRot = -1.95f;
                    trunk2.xRot = -0.35f;
                    trunk3.xRot = -1.2f;
                    leftArm.xRot = -0.8f;
                }
                default -> {}
            }
            if (e.isRearing()) {
                body.xRot = -0.3f;
                head.xRot = -0.4f;
                rightArm.xRot = leftArm.xRot = -2.4f;
            }
            if (e.isCharging()) {
                body.xRot = 0.35f;
                head.xRot = 0.55f;
                trunk1.xRot = 0.7f;
                rightEar.yRot = -1.1f;
                leftEar.yRot = 1.1f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
