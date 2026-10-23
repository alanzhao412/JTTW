package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.PengDemonEntity;
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
 * A Peng-Demon: a bird-demon of the Great Peng's following, dark-feathered with a gold-edged
 * plumage, a hooked beak and a crest, talons, a pair of great pinions folded at its back and a spear
 * in its hand. On the wing its pinions spread and beat; before the Great Peng it bows. Model and
 * skin: {@code assets_source/lion_camel.py}.
 */
public final class PengDemonRenderer extends MobRenderer<PengDemonEntity, PengDemonRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("peng_demon"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/peng_demon.png");

    public PengDemonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.45f);
        addLayer(LionCourt.glow(this, "peng_demon"));
    }

    @Override
    public ResourceLocation getTextureLocation(PengDemonEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(PengDemonEntity e) {
        return super.shouldShowName(e) && LionCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(26, 0).mirror(false).addBox(-4, -16, -2.5f, 8, 16, 5)
                .texOffs(48, 32).mirror(false).addBox(-4.4f, -4, -2.9f, 8.8f, 2, 5.8f)
                .texOffs(93, 0).mirror(false).addBox(-3, -15, -2.9f, 6, 9, 0.5f), PartPose.offsetAndRotation(0, 12, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(66, 0).mirror(false).addBox(-3, -7, -3.5f, 6, 7, 7)
                .texOffs(79, 32).mirror(false).addBox(-1, -4.5f, -6.5f, 2, 2.2f, 3.2f)
                .texOffs(0, 43).mirror(false).addBox(-1, -2.6f, -6.6f, 2, 2, 1), PartPose.offsetAndRotation(0, -16, 0, 0, 0, 0));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(108, 0).mirror(false).addBox(-0.5f, -4, -1, 1, 4, 6), PartPose.offsetAndRotation(0, -7, 0, -0.3f, 0, 0));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-1, 0, 0, 1, 16, 9), PartPose.offsetAndRotation(-3.5f, -14, 2.5f, 0.15f, 0, 0.12f));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(5, 0).mirror(true).addBox(0, 0, 0, 1, 16, 9), PartPose.offsetAndRotation(3.5f, -14, 2.5f, 0.15f, 0, -0.12f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(53, 0).mirror(false).addBox(-1.5f, -1, -1.5f, 3, 12, 3), PartPose.offsetAndRotation(-5, -14, 0, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(53, 0).mirror(true).addBox(-1.5f, -1, -1.5f, 3, 12, 3), PartPose.offsetAndRotation(5, -14, 0, 0, 0, 0));
        PartDefinition spear = rightArm.addOrReplaceChild("spear", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5f, -20, -0.5f, 1, 30, 1)
                .texOffs(92, 32).mirror(false).addBox(-0.8f, -25, -0.8f, 1.6f, 5, 1.6f), PartPose.offsetAndRotation(0, 10, 0, 1.5708f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 32).mirror(false).addBox(-1.6f, 0, -1.6f, 3.2f, 6, 3.2f)
                .texOffs(101, 32).mirror(false).addBox(-1, 6, -1, 2, 5, 2)
                .texOffs(110, 32).mirror(false).addBox(-1.6f, 11, -3, 3.2f, 1, 4.4f), PartPose.offsetAndRotation(-2, 12, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 32).mirror(true).addBox(-1.6f, 0, -1.6f, 3.2f, 6, 3.2f)
                .texOffs(101, 32).mirror(true).addBox(-1, 6, -1, 2, 5, 2)
                .texOffs(110, 32).mirror(true).addBox(-1.6f, 11, -3, 3.2f, 1, 4.4f), PartPose.offsetAndRotation(2, 12, 0, 0, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(17, 32).mirror(false).addBox(-3, 0, 0, 6, 1, 9), PartPose.offsetAndRotation(0, -1, 2.5f, 0.6f, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<PengDemonEntity> {
        private final ModelPart root, body, head, rightWing, leftWing, rightArm, leftArm, rightLeg, leftLeg;

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            rightWing = body.getChild("right_wing");
            leftWing = body.getChild("left_wing");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(PengDemonEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = LionCourt.walk(rightLeg, leftLeg, swing, amount, 0.8f, 0.9f);
            body.xRot = 0.08f;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            float blow = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.6f - stride * 0.3f - blow * 1.3f;
            leftArm.xRot = stride * 0.6f;
            if (e.isAloft()) {
                // pinions spread and beating
                float beat = Mth.sin(age * 0.9f) * 0.5f;
                rightWing.zRot = 1.25f + beat;
                leftWing.zRot = -1.25f - beat;
                rightWing.xRot = leftWing.xRot = 0.1f;
                rightLeg.xRot = leftLeg.xRot = 0.6f;
                body.xRot = 0.35f;
            } else {
                // folded at its back
                rightWing.zRot = 0.12f + Mth.sin(age * 0.05f) * 0.02f;
                leftWing.zRot = -0.12f - Mth.sin(age * 0.05f) * 0.02f;
                rightWing.xRot = leftWing.xRot = 0.15f;
            }
            if (e.isBowing()) {
                LionCourt.bow(body, head);
                rightWing.zRot = 0.6f;
                leftWing.zRot = -0.6f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
