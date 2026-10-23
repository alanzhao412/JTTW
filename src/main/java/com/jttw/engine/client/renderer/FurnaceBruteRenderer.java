package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.fire.FurnaceBruteEntity;
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
 * The Furnace-Brick Brute: a hulk of the Eight Trigrams Furnace's fallen bricks, its mortar
 * glowing, the furnace's grate in its belly and the trigram of fire on its chest, a stump of
 * chimney on its back; its head sunk between its shoulders, its arms to its knees ending in great
 * fists of brick. It lumbers; raising its fists it lifts them high over its head, then brings them
 * down. Model and skin: {@code assets_source/fire_court.py}.
 */
public final class FurnaceBruteRenderer extends MobRenderer<FurnaceBruteEntity, FurnaceBruteRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("furnace_brute"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/furnace_brute.png");

    public FurnaceBruteRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.0f);
        addLayer(FireCourt.glow(this, "furnace_brute"));
    }

    @Override
    public ResourceLocation getTextureLocation(FurnaceBruteEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(FurnaceBruteEntity e) {
        return super.shouldShowName(e) && FireCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-9, -20, -5.5f, 18, 20, 11)
                .texOffs(87, 32).mirror(false).addBox(-4.5f, -13, -6.1f, 9, 7, 0.6f)
                .texOffs(66, 32).mirror(false).addBox(-2.5f, -27, 1, 5, 7, 5), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 32).mirror(false).addBox(-4, -6, -4, 8, 7, 8), PartPose.offsetAndRotation(0, -14, -3, 0, 0, 0));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(59, 0).mirror(false).addBox(-3.5f, -2, -3.5f, 7, 22, 7)
                .texOffs(33, 32).mirror(false).addBox(-4, 18, -4, 8, 7, 8), PartPose.offsetAndRotation(-11, -11, 0, 0, 0, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(59, 0).mirror(true).addBox(-3.5f, -2, -3.5f, 7, 22, 7)
                .texOffs(33, 32).mirror(true).addBox(-4, 18, -4, 8, 7, 8), PartPose.offsetAndRotation(11, -11, 0, 0, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(88, 0).mirror(false).addBox(-3.5f, 0, -3.5f, 7, 18, 7), PartPose.offsetAndRotation(-4.5f, 6, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(88, 0).mirror(true).addBox(-3.5f, 0, -3.5f, 7, 18, 7), PartPose.offsetAndRotation(4.5f, 6, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<FurnaceBruteEntity> {
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
        public void setupAnim(FurnaceBruteEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.5f;
            float stride = Mth.cos(swing * 0.5f) * 0.8f * amount;
            rightLeg.xRot = stride;
            leftLeg.xRot = -stride;
            body.yRot = Mth.cos(swing * 0.5f) * 0.08f * amount;
            if (e.isRaising()) {
                // both fists high over its head
                rightArm.xRot = leftArm.xRot = -2.9f;
                rightArm.zRot = 0.25f;
                leftArm.zRot = -0.25f;
            } else {
                float blow = Mth.sin(attackTime * Mth.PI);
                rightArm.xRot = -stride * 0.7f - blow * 1.6f;
                leftArm.xRot = stride * 0.7f - blow * 1.6f;
                rightArm.zRot = 0.08f;
                leftArm.zRot = -0.08f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
