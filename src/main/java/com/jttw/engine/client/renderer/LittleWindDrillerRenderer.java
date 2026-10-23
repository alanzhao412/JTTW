package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.LittleWindDrillerEntity;
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
 * The Little Wind-Driller: a little green-skinned demon in a patched tunic and a tall pointed cap,
 * big pointed ears, the gold patrol token at his sash and a wooden clapper with a bell in his hand.
 * Raising the alarm he lifts it high over his head and shakes it; before the Great Peng he bows.
 * Model and skin: {@code assets_source/lion_camel.py}.
 */
public final class LittleWindDrillerRenderer extends MobRenderer<LittleWindDrillerEntity, LittleWindDrillerRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("little_wind_driller"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/little_wind_driller.png");

    public LittleWindDrillerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.35f);
        addLayer(LionCourt.glow(this, "little_wind_driller"));
    }

    @Override
    public ResourceLocation getTextureLocation(LittleWindDrillerEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(LittleWindDrillerEntity e) {
        return super.shouldShowName(e) && LionCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-3.5f, -10, -2, 7, 10, 4)
                .texOffs(32, 15).mirror(false).addBox(-3.8f, -3, -2.3f, 7.6f, 1.5f, 4.6f)
                .texOffs(89, 15).mirror(false).addBox(-1, -3.8f, -2.7f, 2, 3, 0.5f)
                .texOffs(0, 15).mirror(false).addBox(-3.6f, -1.5f, -2.2f, 7.2f, 3, 4.4f), PartPose.offsetAndRotation(0, 16, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(23, 0).mirror(false).addBox(-4, -7, -3.5f, 8, 7, 7)
                .texOffs(80, 0).mirror(false).addBox(-3.6f, -9.5f, -3.1f, 7.2f, 3, 6.2f)
                .texOffs(59, 15).mirror(false).addBox(-1.6f, -12.5f, -1.6f, 3.2f, 3, 3.2f)
                .texOffs(96, 15).mirror(false).addBox(-0.6f, -14, -0.6f, 1.2f, 1.5f, 1.2f), PartPose.offsetAndRotation(0, -10, 0, 0, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(105, 15).mirror(false).addBox(-2.5f, -1, -0.5f, 2.5f, 2, 1), PartPose.offsetAndRotation(-4, -4.5f, 0, 0, 0.3f, -0.3f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(105, 15).mirror(true).addBox(0, -1, -0.5f, 2.5f, 2, 1), PartPose.offsetAndRotation(4, -4.5f, 0, 0, -0.3f, 0.3f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(54, 0).mirror(false).addBox(-1.5f, -1, -1.25f, 2.5f, 9, 2.5f), PartPose.offsetAndRotation(-4.5f, -9, 0, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(54, 0).mirror(true).addBox(-1, -1, -1.25f, 2.5f, 9, 2.5f), PartPose.offsetAndRotation(4.5f, -9, 0, 0, 0, 0));
        PartDefinition clapper = rightArm.addOrReplaceChild("clapper", CubeListBuilder.create()
                .texOffs(27, 15).mirror(false).addBox(-0.5f, -6, -0.5f, 1, 7, 1)
                .texOffs(76, 15).mirror(false).addBox(-1.5f, -9, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(-0.25f, 7.5f, 0, 1.5708f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(67, 0).mirror(false).addBox(-1.4f, 0, -1.4f, 2.8f, 8, 2.8f), PartPose.offsetAndRotation(-1.7f, 16, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(67, 0).mirror(true).addBox(-1.4f, 0, -1.4f, 2.8f, 8, 2.8f), PartPose.offsetAndRotation(1.7f, 16, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<LittleWindDrillerEntity> {
        private final ModelPart root, body, head, rightArm, leftArm, rightLeg, leftLeg;

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(LittleWindDrillerEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = LionCourt.walk(rightLeg, leftLeg, swing, amount, 0.9f, 1.2f);
            body.xRot = 0;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            float blow = Mth.sin(attackTime * Mth.PI);
            // the clapper held up before him on his rounds
            rightArm.xRot = -0.7f - stride * 0.3f - blow * 1.2f;
            rightArm.zRot = 0.1f;
            leftArm.xRot = stride * 0.9f;
            leftArm.zRot = -0.1f;
            if (e.isRinging()) {
                // high over his head, shaken
                rightArm.xRot = -2.8f;
                rightArm.zRot = 0.3f + Mth.sin(age * 2.2f) * 0.35f;
                head.xRot = -0.3f;
            }
            if (e.isBowing()) {
                LionCourt.bow(body, head);
                rightArm.xRot = -0.3f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
