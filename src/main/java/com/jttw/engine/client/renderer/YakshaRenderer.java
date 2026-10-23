package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.waters.YakshaEntity;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The Yaksha: a blue-skinned demon of the sea patrol, a shock of red hair over a scowl, eyes
 * burning red, tusks and two short horns, a green kilt under a gold belt, gold at its wrists and
 * ankles, a trident in its hand. Swimming, it kicks and its arms sweep. Model and skin:
 * {@code assets_source/sea_court.py}.
 */
public final class YakshaRenderer extends MobRenderer<YakshaEntity, YakshaRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("yaksha"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/yaksha.png");
    private static final RenderType GLOW = RenderType.eyes(JTTWEngine.id("textures/entity/yaksha_glow.png"));

    public YakshaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.5f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
        addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(YakshaEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(YakshaEntity e) {
        return super.shouldShowName(e) && SeaCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-4, -8, -4, 8, 8, 8)
                .texOffs(0, 17).mirror(false).addBox(-4.5f, -10, -2, 9, 4, 7)
                .texOffs(92, 0).mirror(false).addBox(-4.5f, -8, 2.5f, 9, 10, 2.5f)
                .texOffs(121, 17).mirror(false).addBox(-1.5f, -1, -4.6f, 1, 2.5f, 1)
                .texOffs(121, 17).mirror(false).addBox(0.5f, -1, -4.6f, 1, 2.5f, 1), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(112, 17).mirror(false).addBox(-0.75f, -4, -0.75f, 1.5f, 4, 1.5f), PartPose.offsetAndRotation(-2.5f, -8, -2, -0.4f, 0, -0.35f));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(112, 17).mirror(true).addBox(-0.75f, -4, -0.75f, 1.5f, 4, 1.5f), PartPose.offsetAndRotation(2.5f, -8, -2, -0.4f, 0, 0.35f));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(33, 0).mirror(false).addBox(-4, 0, -2, 8, 12, 4)
                .texOffs(62, 17).mirror(false).addBox(-4.5f, 9, -2.5f, 9, 1.5f, 5)
                .texOffs(33, 17).mirror(false).addBox(-4.5f, 10.5f, -2.5f, 9, 6, 5), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(58, 0).mirror(false).addBox(-3, -2, -2, 4, 12, 4)
                .texOffs(91, 17).mirror(false).addBox(-3.4f, 6, -2.4f, 4.8f, 2, 4.8f), PartPose.offsetAndRotation(-5, 2, 0, 0, 0, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(58, 0).mirror(true).addBox(-1, -2, -2, 4, 12, 4)
                .texOffs(91, 17).mirror(true).addBox(-1.4f, 6, -2.4f, 4.8f, 2, 4.8f), PartPose.offsetAndRotation(5, 2, 0, 0, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(75, 0).mirror(false).addBox(-2, 0, -2, 4, 12, 4)
                .texOffs(91, 17).mirror(false).addBox(-2.3f, 9, -2.3f, 4.6f, 1.5f, 4.6f), PartPose.offsetAndRotation(-1.9f, 12, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(75, 0).mirror(true).addBox(-2, 0, -2, 4, 12, 4)
                .texOffs(91, 17).mirror(true).addBox(-2.3f, 9, -2.3f, 4.6f, 1.5f, 4.6f), PartPose.offsetAndRotation(1.9f, 12, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<YakshaEntity> implements ArmedModel {
        private final ModelPart root, head, rightArm, leftArm, rightLeg, leftLeg;

        public Model(ModelPart root) {
            this.root = root;
            head = root.getChild("head");
            rightArm = root.getChild("right_arm");
            leftArm = root.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(YakshaEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            boolean swimming = e.isInWater();
            float kick = swimming ? Mth.sin(age * 0.3f) * (0.3f + amount * 0.5f) : Mth.cos(swing * 0.66f) * 1.2f * amount;
            rightLeg.xRot = kick;
            leftLeg.xRot = -kick;
            leftArm.xRot = swimming ? -0.4f + Mth.sin(age * 0.3f + 1f) * 0.4f : -kick * 0.8f;
            leftArm.zRot = swimming ? -0.3f : 0;
            // the trident held up; brought down on an attack
            float strike = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.5f - strike * 1.4f;
            rightArm.zRot = 0.1f;
        }

        @Override
        public void translateToHand(HumanoidArm arm, PoseStack pose) {
            root.translateAndRotate(pose);
            (arm == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(pose);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
