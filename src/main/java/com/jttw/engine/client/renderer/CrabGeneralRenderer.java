package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.waters.CrabGeneralEntity;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Crab General: a great red crab, its shell broad and knobbed, two huge claws held before it,
 * three legs a side, its eyes on stalks, a gilded general's helm with a red plume on its back. It
 * scuttles on its legs; its claws snap open and shut, and close hard on an attack. Model and skin:
 * {@code assets_source/sea_court.py}.
 */
public final class CrabGeneralRenderer extends MobRenderer<CrabGeneralEntity, CrabGeneralRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("crab_general"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/crab_general.png");
    private static final RenderType GLOW = RenderType.eyes(JTTWEngine.id("textures/entity/crab_general_glow.png"));

    public CrabGeneralRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.0f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(CrabGeneralEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(CrabGeneralEntity e) {
        return super.shouldShowName(e) && SeaCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-12, -6, -9, 24, 7, 18)
                .texOffs(0, 26).mirror(false).addBox(-9, 1, -7, 18, 3, 14)
                .texOffs(49, 57).mirror(false).addBox(-3, -3, -9.6f, 6, 3, 1)
                .texOffs(0, 44).mirror(false).addBox(-4, -10, -5, 8, 4, 8)
                .texOffs(33, 44).mirror(false).addBox(-5, -7, -6, 10, 1, 10)
                .texOffs(74, 44).mirror(false).addBox(-0.5f, -15, -2, 1, 5, 6), PartPose.offsetAndRotation(0, 14, 0, 0, 0, 0));
        PartDefinition rightEye = body.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(27, 57).mirror(false).addBox(-0.75f, -4, -0.75f, 1.5f, 4, 1.5f)
                .texOffs(36, 57).mirror(false).addBox(-1.25f, -6, -1.25f, 2.5f, 2, 2.5f), PartPose.offsetAndRotation(-3.5f, -6, -8, 0, 0, 0));
        PartDefinition leftEye = body.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(27, 57).mirror(true).addBox(-0.75f, -4, -0.75f, 1.5f, 4, 1.5f)
                .texOffs(36, 57).mirror(true).addBox(-1.25f, -6, -1.25f, 2.5f, 2, 2.5f), PartPose.offsetAndRotation(3.5f, -6, -8, 0, 0, 0));
        PartDefinition rightClaw = root.addOrReplaceChild("right_claw", CubeListBuilder.create()
                .texOffs(89, 44).mirror(false).addBox(-1.5f, -1.5f, -8, 3, 3, 8), PartPose.offsetAndRotation(-9, 15, -8, 0, 0.5f, 0));
        PartDefinition rightPincer = rightClaw.addOrReplaceChild("right_pincer", CubeListBuilder.create()
                .texOffs(65, 26).mirror(false).addBox(-3.5f, -3, -9, 7, 6, 9), PartPose.offsetAndRotation(0, 0, -8, 0, -0.7f, 0));
        PartDefinition rightFinger = rightPincer.addOrReplaceChild("right_finger", CubeListBuilder.create()
                .texOffs(0, 57).mirror(false).addBox(-2.5f, 0, -8, 5, 2, 8), PartPose.offsetAndRotation(0, 3, -1, 0.3f, 0, 0));
        PartDefinition leftClaw = root.addOrReplaceChild("left_claw", CubeListBuilder.create()
                .texOffs(89, 44).mirror(true).addBox(-1.5f, -1.5f, -8, 3, 3, 8), PartPose.offsetAndRotation(9, 15, -8, 0, -0.5f, 0));
        PartDefinition leftPincer = leftClaw.addOrReplaceChild("left_pincer", CubeListBuilder.create()
                .texOffs(65, 26).mirror(true).addBox(-3.5f, -3, -9, 7, 6, 9), PartPose.offsetAndRotation(0, 0, -8, 0, 0.7f, 0));
        PartDefinition leftFinger = leftPincer.addOrReplaceChild("left_finger", CubeListBuilder.create()
                .texOffs(0, 57).mirror(true).addBox(-2.5f, 0, -8, 5, 2, 8), PartPose.offsetAndRotation(0, 3, -1, 0.3f, 0, 0));
        PartDefinition rightLeg0 = root.addOrReplaceChild("right_leg0", CubeListBuilder.create()
                .texOffs(64, 57).mirror(false).addBox(-8, -1, -1, 8, 2, 2), PartPose.offsetAndRotation(-11, 15, -3.5f, 0, 0, 0.4f));
        PartDefinition rightLeg0Low = root.addOrReplaceChild("right_leg0_low", CubeListBuilder.create()
                .texOffs(98, 26).mirror(false).addBox(-1, 0, -1, 2, 12, 2), PartPose.offsetAndRotation(-18.4f, 12, -3.5f, 0, 0, 0.15f));
        PartDefinition rightLeg1 = root.addOrReplaceChild("right_leg1", CubeListBuilder.create()
                .texOffs(64, 57).mirror(false).addBox(-8, -1, -1, 8, 2, 2), PartPose.offsetAndRotation(-11, 15, 1.5f, 0, 0, 0.4f));
        PartDefinition rightLeg1Low = root.addOrReplaceChild("right_leg1_low", CubeListBuilder.create()
                .texOffs(98, 26).mirror(false).addBox(-1, 0, -1, 2, 12, 2), PartPose.offsetAndRotation(-18.4f, 12, 1.5f, 0, 0, 0.15f));
        PartDefinition rightLeg2 = root.addOrReplaceChild("right_leg2", CubeListBuilder.create()
                .texOffs(64, 57).mirror(false).addBox(-8, -1, -1, 8, 2, 2), PartPose.offsetAndRotation(-11, 15, 6.5f, 0, 0, 0.4f));
        PartDefinition rightLeg2Low = root.addOrReplaceChild("right_leg2_low", CubeListBuilder.create()
                .texOffs(98, 26).mirror(false).addBox(-1, 0, -1, 2, 12, 2), PartPose.offsetAndRotation(-18.4f, 12, 6.5f, 0, 0, 0.15f));
        PartDefinition leftLeg0 = root.addOrReplaceChild("left_leg0", CubeListBuilder.create()
                .texOffs(64, 57).mirror(true).addBox(0, -1, -1, 8, 2, 2), PartPose.offsetAndRotation(11, 15, -3.5f, 0, 0, -0.4f));
        PartDefinition leftLeg0Low = root.addOrReplaceChild("left_leg0_low", CubeListBuilder.create()
                .texOffs(98, 26).mirror(true).addBox(-1, 0, -1, 2, 12, 2), PartPose.offsetAndRotation(18.4f, 12, -3.5f, 0, 0, -0.15f));
        PartDefinition leftLeg1 = root.addOrReplaceChild("left_leg1", CubeListBuilder.create()
                .texOffs(64, 57).mirror(true).addBox(0, -1, -1, 8, 2, 2), PartPose.offsetAndRotation(11, 15, 1.5f, 0, 0, -0.4f));
        PartDefinition leftLeg1Low = root.addOrReplaceChild("left_leg1_low", CubeListBuilder.create()
                .texOffs(98, 26).mirror(true).addBox(-1, 0, -1, 2, 12, 2), PartPose.offsetAndRotation(18.4f, 12, 1.5f, 0, 0, -0.15f));
        PartDefinition leftLeg2 = root.addOrReplaceChild("left_leg2", CubeListBuilder.create()
                .texOffs(64, 57).mirror(true).addBox(0, -1, -1, 8, 2, 2), PartPose.offsetAndRotation(11, 15, 6.5f, 0, 0, -0.4f));
        PartDefinition leftLeg2Low = root.addOrReplaceChild("left_leg2_low", CubeListBuilder.create()
                .texOffs(98, 26).mirror(true).addBox(-1, 0, -1, 2, 12, 2), PartPose.offsetAndRotation(18.4f, 12, 6.5f, 0, 0, -0.15f));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<CrabGeneralEntity> {
        private final ModelPart root, body, rightClaw, leftClaw, rightFinger, leftFinger;
        private final ModelPart[] legs = new ModelPart[12];

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            rightClaw = root.getChild("right_claw");
            leftClaw = root.getChild("left_claw");
            rightFinger = rightClaw.getChild("right_pincer").getChild("right_finger");
            leftFinger = leftClaw.getChild("left_pincer").getChild("left_finger");
            int k = 0;
            for (String side : new String[]{"right", "left"}) {
                for (int i = 0; i < 3; i++) {
                    legs[k++] = root.getChild(side + "_leg" + i);
                    legs[k++] = root.getChild(side + "_leg" + i + "_low");
                }
            }
        }

        @Override
        public void setupAnim(CrabGeneralEntity e, float swing, float amount, float age, float yaw, float pitch) {
            for (int k = 0; k < 12; k++) {
                int leg = k / 2;
                float step = Mth.cos(swing * 1.2f + leg * 2.1f) * 0.45f * amount;
                legs[k].xRot = step;
                legs[k].zRot = legs[k].getInitialPose().zRot + (k % 2 == 0 ? Mth.sin(swing * 1.2f + leg * 2.1f) * 0.2f * amount : 0);
            }
            float snap = Mth.sin(attackTime * Mth.PI);
            float open = 0.3f + Mth.sin(age * 0.08f) * 0.12f - snap * 0.35f;
            rightFinger.xRot = open;
            leftFinger.xRot = open;
            rightClaw.xRot = -snap * 0.5f;
            leftClaw.xRot = -snap * 0.5f;
            body.y = body.getInitialPose().y + Mth.sin(age * 0.1f) * 0.3f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
