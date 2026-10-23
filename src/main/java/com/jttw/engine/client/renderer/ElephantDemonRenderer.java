package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.ElephantDemonEntity;
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
 * An Elephant-Demon: a grey elephant's head, great flapping ears, short tusks and a swaying trunk,
 * on a hulking body in rough hide and iron plates, a heavy mace in his fist. Charging, he drops
 * his head, tusks forward; before the Great Peng he bows. Model and skin:
 * {@code assets_source/lion_camel.py}.
 */
public final class ElephantDemonRenderer extends MobRenderer<ElephantDemonEntity, ElephantDemonRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("elephant_demon"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/elephant_demon.png");

    public ElephantDemonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.9f);
        addLayer(LionCourt.glow(this, "elephant_demon"));
    }

    @Override
    public ResourceLocation getTextureLocation(ElephantDemonEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(ElephantDemonEntity e) {
        return super.shouldShowName(e) && LionCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-8, -20, -5, 16, 20, 10)
                .texOffs(77, 66).mirror(false).addBox(-6.5f, -19, -5.6f, 13, 8, 0.8f)
                .texOffs(0, 51).mirror(false).addBox(-8.5f, -4, -5.5f, 17, 3, 11)
                .texOffs(64, 31).mirror(false).addBox(-8.3f, -1, -5.3f, 16.6f, 5, 10.6f), PartPose.offsetAndRotation(0, 8, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 31).mirror(false).addBox(-5, -10, -6, 10, 10, 9)
                .texOffs(13, 78).mirror(false).addBox(-3.6f, -2.5f, -9, 1.4f, 1.4f, 5)
                .texOffs(13, 78).mirror(false).addBox(2.2f, -2.5f, -9, 1.4f, 1.4f, 5), PartPose.offsetAndRotation(0, -20, -1.5f, 0, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(78, 0).mirror(false).addBox(-1, -1, -1, 1, 11, 10), PartPose.offsetAndRotation(-5, -9, -2, 0, -0.35f, 0));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(78, 0).mirror(true).addBox(0, -1, -1, 1, 11, 10), PartPose.offsetAndRotation(5, -9, -2, 0, 0.35f, 0));
        PartDefinition trunk1 = head.addOrReplaceChild("trunk1", CubeListBuilder.create()
                .texOffs(31, 66).mirror(false).addBox(-1.75f, 0, -1.75f, 3.5f, 6, 3.5f), PartPose.offsetAndRotation(0, -3, -6, 0.08f, 0, 0));
        PartDefinition trunk2 = trunk1.addOrReplaceChild("trunk2", CubeListBuilder.create()
                .texOffs(106, 66).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 6, 3), PartPose.offsetAndRotation(0, 6, 0, 0.12f, 0, 0));
        PartDefinition trunk3 = trunk2.addOrReplaceChild("trunk3", CubeListBuilder.create()
                .texOffs(0, 78).mirror(false).addBox(-1.25f, 0, -1.25f, 2.5f, 5, 2.5f), PartPose.offsetAndRotation(0, 6, 0, 0.18f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(53, 0).mirror(false).addBox(-3, -2, -3, 6, 17, 6)
                .texOffs(66, 51).mirror(false).addBox(-3.6f, -3, -3.6f, 7.2f, 5, 7.2f)
                .texOffs(48, 66).mirror(false).addBox(-3.4f, 11, -3.4f, 6.8f, 3, 6.8f), PartPose.offsetAndRotation(-10, -17, 0, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(53, 0).mirror(true).addBox(-3, -2, -3, 6, 17, 6)
                .texOffs(66, 51).mirror(true).addBox(-3.6f, -3, -3.6f, 7.2f, 5, 7.2f)
                .texOffs(48, 66).mirror(true).addBox(-3.4f, 11, -3.4f, 6.8f, 3, 6.8f), PartPose.offsetAndRotation(10, -17, 0, 0, 0, 0));
        PartDefinition mace = rightArm.addOrReplaceChild("mace", CubeListBuilder.create()
                .texOffs(57, 51).mirror(false).addBox(-0.75f, -6, -0.75f, 1.5f, 12, 1.5f)
                .texOffs(99, 51).mirror(false).addBox(-2.2f, -12, -2.2f, 4.4f, 6, 4.4f), PartPose.offsetAndRotation(0, 14, 0, 1.5708f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(39, 31).mirror(false).addBox(-3, 0, -3, 6, 13, 6)
                .texOffs(0, 66).mirror(false).addBox(-3.4f, 13, -3.6f, 6.8f, 3, 7.2f), PartPose.offsetAndRotation(-4.5f, 8, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(39, 31).mirror(true).addBox(-3, 0, -3, 6, 13, 6)
                .texOffs(0, 66).mirror(true).addBox(-3.4f, 13, -3.6f, 6.8f, 3, 7.2f), PartPose.offsetAndRotation(4.5f, 8, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<ElephantDemonEntity> {
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
        public void setupAnim(ElephantDemonEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = LionCourt.walk(rightLeg, leftLeg, swing, amount, 0.5f, 0.8f);
            body.xRot = 0;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.6f;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.5f;
            float sway = Mth.sin(age * 0.08f);
            // hanging, its tip curling forward (a negative turn swings it out before him)
            trunk1.xRot = -0.05f + sway * 0.08f;
            trunk2.xRot = -0.12f + sway * 0.1f;
            trunk3.xRot = -0.25f + sway * 0.12f;
            trunk1.zRot = Mth.cos(age * 0.06f) * 0.08f;
            float flap = Mth.sin(age * 0.1f) * 0.12f;
            rightEar.yRot = -0.35f - flap;
            leftEar.yRot = 0.35f + flap;
            float blow = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.3f - stride * 0.5f - blow * 1.8f;
            leftArm.xRot = stride * 0.5f;
            if (e.isCharging()) {
                // head down, tusks forward, trunk curled back
                body.xRot = 0.3f;
                head.xRot = 0.5f;
                trunk1.xRot = 0.6f;
                trunk2.xRot = 0.5f;
                rightEar.yRot = -1.0f;
                leftEar.yRot = 1.0f;
            }
            if (e.isBowing()) {
                LionCourt.bow(body, head);
                rightArm.xRot = -0.2f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
