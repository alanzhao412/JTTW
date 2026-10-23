package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.fire.EmberImpEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Ember Imp: a little fire demon, red-hot hide crazed with glowing cracks, a crest of living
 * flame from brow to nape, two stubby horns, yellow eyes, a flame-tipped tail; it lopes with its
 * arms swinging and flings with its right. Lit from within. Model and skin:
 * {@code assets_source/fire_court.py}.
 */
public final class EmberImpRenderer extends MobRenderer<EmberImpEntity, EmberImpRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("ember_imp"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/ember_imp.png");

    public EmberImpRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.4f);
        addLayer(FireCourt.glow(this, "ember_imp"));
    }

    @Override
    public ResourceLocation getTextureLocation(EmberImpEntity e) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(EmberImpEntity e, BlockPos pos) {
        return 15;
    }

    @Override
    protected boolean shouldShowName(EmberImpEntity e) {
        return super.shouldShowName(e) && FireCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(29, 0).mirror(false).addBox(-3, 0, -2, 6, 7, 4), PartPose.offsetAndRotation(0, 9, 0, 0, 0, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-3.5f, -7, -3.5f, 7, 7, 7)
                .texOffs(0, 15).mirror(false).addBox(-3.2f, -8.5f, -1.2f, 1.2f, 2, 1.2f)
                .texOffs(0, 15).mirror(false).addBox(2, -8.5f, -1.2f, 1.2f, 2, 1.2f), PartPose.offsetAndRotation(0, 9, -0.5f, 0, 0, 0));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(50, 0).mirror(false).addBox(-1.5f, -4, -3, 3, 4, 7)
                .texOffs(103, 0).mirror(false).addBox(-0.75f, -6.5f, -1.5f, 1.5f, 2.5f, 4), PartPose.offsetAndRotation(0, -7, 0, 0, 0, 0));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(71, 0).mirror(false).addBox(-1.5f, -1, -1.25f, 2.5f, 8, 2.5f), PartPose.offsetAndRotation(-3.8f, 10, 0, 0, 0, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(71, 0).mirror(true).addBox(-1, -1, -1.25f, 2.5f, 8, 2.5f), PartPose.offsetAndRotation(3.8f, 10, 0, 0, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(71, 0).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(-1.6f, 16, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(71, 0).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(1.6f, 16, 0, 0, 0, 0));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(84, 0).mirror(false).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 7)
                .texOffs(116, 0).mirror(false).addBox(-1, -1, 6, 2, 2, 3), PartPose.offsetAndRotation(0, 15, 2, 0.9f, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<EmberImpEntity> {
        private final ModelPart root, head, crest, rightArm, leftArm, rightLeg, leftLeg, tail;

        public Model(ModelPart root) {
            this.root = root;
            head = root.getChild("head");
            crest = head.getChild("crest");
            rightArm = root.getChild("right_arm");
            leftArm = root.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            tail = root.getChild("tail");
        }

        @Override
        public void setupAnim(EmberImpEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            float stride = Mth.cos(swing * 0.9f) * 1.3f * amount;
            rightLeg.xRot = stride;
            leftLeg.xRot = -stride;
            leftArm.xRot = stride * 0.9f;
            float fling = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -stride * 0.9f - fling * 2.4f;
            rightArm.zRot = 0.15f;
            leftArm.zRot = -0.15f;
            crest.xRot = Mth.sin(age * 0.5f) * 0.08f;
            tail.yRot = Mth.sin(age * 0.2f) * 0.4f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
