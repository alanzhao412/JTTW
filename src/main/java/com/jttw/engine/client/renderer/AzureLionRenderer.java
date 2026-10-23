package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.AzureLionEntity;
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
 * The Azure Lion: a towering demon king with a lion's head, its great mane azure-blue, golden
 * eyes; dark lacquered armour edged with gold, a gold belt, a skirt of plates; a broad curved
 * sword in his right hand; a tail with an azure tuft. Gaping, his lower jaw drops wide; roaring,
 * he throws his head back, jaws open. Model and skin: {@code assets_source/lion_camel.py}.
 */
public final class AzureLionRenderer extends MobRenderer<AzureLionEntity, AzureLionRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("azure_lion"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/azure_lion.png");

    public AzureLionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.0f);
        addLayer(LionCourt.glow(this, "azure_lion"));
    }

    @Override
    public ResourceLocation getTextureLocation(AzureLionEntity e) {
        return TEXTURE;
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-8, -22, -4.5f, 16, 22, 9)
                .texOffs(0, 51).mirror(false).addBox(-6, -21, -5.1f, 12, 10, 0.8f)
                .texOffs(125, 32).mirror(false).addBox(-8.6f, -6, -5.1f, 17.2f, 3, 10.2f)
                .texOffs(0, 32).mirror(false).addBox(-8.4f, -3, -4.9f, 16.8f, 8, 9.8f)
                .texOffs(55, 32).mirror(false).addBox(-7.5f, -23, 4.2f, 15, 13, 2.5f), PartPose.offsetAndRotation(0, 4, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(169, 0).mirror(false).addBox(-6, -11, -7, 12, 11, 10)
                .texOffs(130, 51).mirror(false).addBox(-4.5f, -6, -11, 9, 5, 4.5f)
                .texOffs(39, 63).mirror(false).addBox(-1.75f, -7, -11.6f, 3.5f, 2, 1.6f)
                .texOffs(64, 0).mirror(false).addBox(-9, -15, -3, 18, 18, 9)
                .texOffs(27, 51).mirror(false).addBox(-7, -16.5f, -6, 14, 4, 7)
                .texOffs(217, 32).mirror(false).addBox(-9.5f, -8, -6, 3, 8, 4)
                .texOffs(217, 32).mirror(false).addBox(6.5f, -8, -6, 3, 8, 4), PartPose.offsetAndRotation(0, -22, -1, 0, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(189, 51).mirror(false).addBox(-4, 0, -5.5f, 8, 2.5f, 6)
                .texOffs(218, 51).mirror(false).addBox(-5, 2, -4.5f, 10, 4, 5), PartPose.offsetAndRotation(0, -1, -5, 0.05f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(28, 63).mirror(false).addBox(-1.5f, -3, -1, 3, 3, 2), PartPose.offsetAndRotation(-5, -14, -2, 0, 0, -0.4f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(28, 63).mirror(true).addBox(-1.5f, -3, -1, 3, 3, 2), PartPose.offsetAndRotation(5, -14, -2, 0, 0, 0.4f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(119, 0).mirror(false).addBox(-3, -2, -3, 6, 19, 6)
                .texOffs(184, 32).mirror(false).addBox(-4, -4, -4, 8, 6, 8)
                .texOffs(70, 51).mirror(false).addBox(-3.5f, 12, -3.5f, 7, 4, 7), PartPose.offsetAndRotation(-10.5f, -19, 0, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(119, 0).mirror(true).addBox(-3, -2, -3, 6, 19, 6)
                .texOffs(184, 32).mirror(true).addBox(-4, -4, -4, 8, 6, 8)
                .texOffs(70, 51).mirror(true).addBox(-3.5f, 12, -3.5f, 7, 4, 7), PartPose.offsetAndRotation(10.5f, -19, 0, 0, 0, 0));
        PartDefinition sword = rightArm.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(159, 51).mirror(false).addBox(-0.75f, -3, -0.75f, 1.5f, 8, 1.5f)
                .texOffs(11, 63).mirror(false).addBox(-2, -4, -2, 4, 1.2f, 4)
                .texOffs(51, 0).mirror(false).addBox(-0.5f, -30, -2.8f, 1, 26, 5)
                .texOffs(0, 63).mirror(false).addBox(-0.5f, -33, -1.8f, 1, 3, 3.6f), PartPose.offsetAndRotation(0, 16, 0, 1.5708f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(144, 0).mirror(false).addBox(-3, 0, -3, 6, 17, 6)
                .texOffs(99, 51).mirror(false).addBox(-3.4f, 17, -4.6f, 6.8f, 3, 7.4f), PartPose.offsetAndRotation(-4, 4, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(144, 0).mirror(true).addBox(-3, 0, -3, 6, 17, 6)
                .texOffs(99, 51).mirror(true).addBox(-3.4f, 17, -4.6f, 6.8f, 3, 7.4f), PartPose.offsetAndRotation(4, 4, 0, 0, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(92, 32).mirror(false).addBox(-0.8f, -0.8f, 0, 1.6f, 1.6f, 14)
                .texOffs(168, 51).mirror(false).addBox(-2.2f, -2.2f, 12.5f, 4.4f, 4.4f, 5), PartPose.offsetAndRotation(0, -3, 4.5f, 0.7f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<AzureLionEntity> {
        private final ModelPart root, body, head, jaw, rightArm, leftArm, rightLeg, leftLeg, tail;

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            tail = body.getChild("tail");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(AzureLionEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = LionCourt.walk(rightLeg, leftLeg, swing, amount, 0.5f, 0.9f);
            body.xRot = 0;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.7f;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.6f;
            jaw.xRot = 0.05f + Mth.sin(age * 0.07f) * 0.04f;
            tail.yRot = Mth.sin(age * 0.09f) * 0.35f;
            tail.xRot = 0.7f;
            float blow = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.45f - stride * 0.4f - blow * 1.7f;
            rightArm.zRot = 0.12f;
            leftArm.xRot = stride * 0.5f;
            leftArm.zRot = -0.12f;
            if (e.isRoaring()) {
                head.xRot = -0.6f;
                jaw.xRot = 0.8f;
                leftArm.xRot = -0.9f;
                leftArm.zRot = -0.5f;
            }
            if (e.isGaping()) {
                // jaws wide as a city gate, head thrust forward and down
                body.xRot = 0.25f;
                head.xRot = 0.3f;
                jaw.xRot = 1.1f;
                leftArm.xRot = -1.2f;
            }
            if (e.isReeling()) {
                body.xRot = -0.2f;
                head.xRot = 0.5f;
                head.zRot = Mth.sin(age * 0.5f) * 0.3f;
                jaw.xRot = 0.5f;
            } else {
                head.zRot = 0;
            }
            if (e.isHolding()) {
                // something in his belly: he hunches over it
                body.xRot = 0.15f + Mth.sin(age * 0.6f) * 0.05f;
                leftArm.xRot = -0.6f;
                leftArm.zRot = 0.4f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
