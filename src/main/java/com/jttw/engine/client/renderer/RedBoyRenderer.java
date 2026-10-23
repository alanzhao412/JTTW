package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.fire.RedBoyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ArmedModel;
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
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Red Boy, the Holy Child King: a child with a round face and fierce brows, his head shaved but for
 * two tufts tied up on top and a lock over his brow, a red stomacher (肚兜) embroidered with gold over
 * his bare chest, red breeches to the knee, gold at his neck, wrists and ankles, bare feet; in his
 * hand the Fire-Tipped Spear (the mod's own, held as an item). Striking his nose for the Samadhi
 * fire, his left fist goes to his face. Model and skin: {@code assets_source/fire_court.py}.
 */
public final class RedBoyRenderer extends MobRenderer<RedBoyEntity, RedBoyRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("red_boy"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/red_boy.png");

    public RedBoyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.4f);
        addLayer(FireCourt.glow(this, "red_boy"));
        addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(RedBoyEntity e) {
        return TEXTURE;
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(33, 0).mirror(false).addBox(-3, 0, -2, 6, 8, 4)
                .texOffs(80, 0).mirror(false).addBox(-2.8f, 0.6f, -2.4f, 5.6f, 6.4f, 0.5f)
                .texOffs(95, 0).mirror(false).addBox(-3.4f, -0.6f, -2.4f, 6.8f, 1.2f, 4.8f), PartPose.offsetAndRotation(0, 9, 0, 0, 0, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-4, -8, -4, 8, 8, 8)
                .texOffs(0, 17).mirror(false).addBox(-3.3f, -10.2f, -1.2f, 2.4f, 2.4f, 2.4f)
                .texOffs(0, 17).mirror(false).addBox(0.9f, -10.2f, -1.2f, 2.4f, 2.4f, 2.4f)
                .texOffs(30, 17).mirror(false).addBox(-2, -8.7f, -4.3f, 4, 1.6f, 1.2f), PartPose.offsetAndRotation(0, 9, 0, 0, 0, 0));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(54, 0).mirror(false).addBox(-2, -1, -1.25f, 2.5f, 7, 2.5f)
                .texOffs(43, 17).mirror(false).addBox(-2.2f, 4.3f, -1.45f, 2.9f, 0.9f, 2.9f), PartPose.offsetAndRotation(-4, 10, 0, 0, 0, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(54, 0).mirror(true).addBox(-0.5f, -1, -1.25f, 2.5f, 7, 2.5f)
                .texOffs(43, 17).mirror(true).addBox(-0.7f, 4.3f, -1.45f, 2.9f, 0.9f, 2.9f), PartPose.offsetAndRotation(4, 10, 0, 0, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(67, 0).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 7, 3)
                .texOffs(13, 17).mirror(false).addBox(-1.7f, 5.4f, -1.7f, 3.4f, 0.8f, 3.4f), PartPose.offsetAndRotation(-1.6f, 17, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(67, 0).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 7, 3)
                .texOffs(13, 17).mirror(true).addBox(-1.7f, 5.4f, -1.7f, 3.4f, 0.8f, 3.4f), PartPose.offsetAndRotation(1.6f, 17, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<RedBoyEntity> implements ArmedModel {
        private final ModelPart root, head, body, rightArm, leftArm, rightLeg, leftLeg;

        public Model(ModelPart root) {
            this.root = root;
            head = root.getChild("head");
            body = root.getChild("body");
            rightArm = root.getChild("right_arm");
            leftArm = root.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(RedBoyEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            float stride = Mth.cos(swing * 0.8f) * 1.3f * amount;
            rightLeg.xRot = stride;
            leftLeg.xRot = -stride;
            // the spear held up before him; a thrust on a blow
            float thrust = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.6f - thrust * 1.1f;
            rightArm.yRot = -0.1f + thrust * 0.3f;
            rightArm.zRot = 0.1f;
            if (e.isWindingUp()) {
                // his fist to his nose, the fire about to pour out
                leftArm.xRot = -2.1f;
                leftArm.yRot = 0.55f;
                leftArm.zRot = 0;
            } else {
                leftArm.xRot = -stride * 0.8f;
                leftArm.yRot = 0;
                leftArm.zRot = -0.12f;
            }
        }

        @Override
        public void translateToHand(HumanoidArm arm, PoseStack pose) {
            root.translateAndRotate(pose);
            (arm == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(pose);
            pose.translate(0, -0.1f, 0); // a child's hand is short: up the arm
            pose.scale(0.8f, 0.8f, 0.8f);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
