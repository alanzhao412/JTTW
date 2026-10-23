package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.LionDemonEntity;
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
 * A Lion-Demon: a lion's head with a shaggy tawny mane on a demon soldier's body in dark leather and
 * bronze, a curved sabre in his right hand, a tufted tail. Bounding at his foe, he crouches and
 * throws his arms forward; before the Great Peng he bows. Model and skin:
 * {@code assets_source/lion_camel.py}.
 */
public final class LionDemonRenderer extends MobRenderer<LionDemonEntity, LionDemonRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("lion_demon"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/lion_demon.png");

    public LionDemonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.5f);
        addLayer(LionCourt.glow(this, "lion_demon"));
    }

    @Override
    public ResourceLocation getTextureLocation(LionDemonEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(LionDemonEntity e) {
        return super.shouldShowName(e) && LionCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-5, -16, -3, 10, 16, 6)
                .texOffs(0, 41).mirror(false).addBox(-5.5f, -3, -3.5f, 11, 2, 7)
                .texOffs(34, 23).mirror(false).addBox(-5.4f, -1, -3.3f, 10.8f, 5, 6.6f)
                .texOffs(0, 51).mirror(false).addBox(-2, -14, -3.6f, 4, 4, 0.6f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(85, 0).mirror(false).addBox(-4.5f, -9, -5, 9, 9, 8)
                .texOffs(58, 41).mirror(false).addBox(-2.5f, -4, -7.5f, 5, 4, 2.5f)
                .texOffs(33, 0).mirror(false).addBox(-6.5f, -11.5f, -1.5f, 13, 13, 7)
                .texOffs(75, 41).mirror(false).addBox(-5, -12, -4, 10, 3, 4), PartPose.offsetAndRotation(0, -16, -0.5f, 0, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(33, 51).mirror(false).addBox(-1, -2, -0.5f, 2, 2, 1), PartPose.offsetAndRotation(-3.5f, -9, -1, 0, 0, -0.3f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(33, 51).mirror(true).addBox(-1, -2, -0.5f, 2, 2, 1), PartPose.offsetAndRotation(3.5f, -9, -1, 0, 0, 0.3f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 23).mirror(false).addBox(-2.5f, -2, -2, 4, 13, 4)
                .texOffs(96, 23).mirror(false).addBox(-3.2f, -3, -2.6f, 5.4f, 4, 5.2f), PartPose.offsetAndRotation(-6.5f, -14, 0, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 23).mirror(true).addBox(-1.5f, -2, -2, 4, 13, 4)
                .texOffs(96, 23).mirror(true).addBox(-2.2f, -3, -2.6f, 5.4f, 4, 5.2f), PartPose.offsetAndRotation(6.5f, -14, 0, 0, 0, 0));
        PartDefinition sabre = rightArm.addOrReplaceChild("sabre", CubeListBuilder.create()
                .texOffs(119, 41).mirror(false).addBox(-0.5f, -2, -0.5f, 1, 5, 1)
                .texOffs(20, 51).mirror(false).addBox(-1.5f, -3, -1.5f, 3, 1, 3)
                .texOffs(74, 0).mirror(false).addBox(-0.4f, -17, -1.5f, 0.8f, 14, 3.5f)
                .texOffs(11, 51).mirror(false).addBox(-0.4f, -19, -0.5f, 0.8f, 2, 2.5f), PartPose.offsetAndRotation(-0.5f, 10, 0, 1.5708f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(17, 23).mirror(false).addBox(-2, 0, -2, 4, 12, 4)
                .texOffs(37, 41).mirror(false).addBox(-2.3f, 11, -3, 4.6f, 3, 5), PartPose.offsetAndRotation(-2.5f, 10, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(17, 23).mirror(true).addBox(-2, 0, -2, 4, 12, 4)
                .texOffs(37, 41).mirror(true).addBox(-2.3f, 11, -3, 4.6f, 3, 5), PartPose.offsetAndRotation(2.5f, 10, 0, 0, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(71, 23).mirror(false).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 10)
                .texOffs(104, 41).mirror(false).addBox(-1.3f, -1.3f, 9, 2.6f, 2.6f, 3.5f), PartPose.offsetAndRotation(0, -2, 3, 0.7f, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<LionDemonEntity> {
        private final ModelPart root, body, head, rightArm, leftArm, rightLeg, leftLeg, tail;

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            tail = body.getChild("tail");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(LionDemonEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = LionCourt.walk(rightLeg, leftLeg, swing, amount, 0.6662f, 1.1f);
            body.xRot = 0;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            tail.yRot = Mth.sin(age * 0.12f) * 0.3f;
            float blow = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.5f - stride * 0.4f - blow * 1.6f;
            rightArm.zRot = 0.1f;
            leftArm.xRot = stride * 0.7f;
            leftArm.zRot = -0.1f;
            if (!e.onGround()) {
                // bounding: arms flung forward, legs drawn up
                rightArm.xRot = leftArm.xRot = -1.5f;
                rightLeg.xRot = -0.6f;
                leftLeg.xRot = 0.4f;
            }
            if (e.isBowing()) {
                LionCourt.bow(body, head);
                rightArm.xRot = -0.2f;
                leftArm.xRot = -0.9f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
