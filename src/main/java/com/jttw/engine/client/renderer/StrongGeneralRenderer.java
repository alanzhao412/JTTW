package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.fire.StrongGeneralEntity;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Red Boy's Six Strong Generals: demon officers in lacquered lamellar armour, pauldrons and a
 * tasset skirt, a horned helm with a crest, tusks, a long-bladed glaive in the right hand. One shape
 * for all six; each his own skin ({@code strong_general_<name>.png}) and glowing parts: the
 * Cloud-Soaring Sprite white and silver, the Fog-Diving Monster slate and grey, the Mad-Hurrying
 * Wind orange, the Swift-Rushing Wind green, the Flame-Head General crimson with his head ablaze,
 * the Eye-Spark Officer black and gold with great burning eyes. Model and skins:
 * {@code assets_source/fire_court.py}.
 */
public final class StrongGeneralRenderer extends MobRenderer<StrongGeneralEntity, StrongGeneralRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("strong_general"), "main");
    private static final ResourceLocation[] SKINS = new ResourceLocation[StrongGeneralEntity.NAMES.length];
    private static final RenderType[] GLOWS = new RenderType[StrongGeneralEntity.NAMES.length];

    static {
        for (int i = 0; i < SKINS.length; i++) {
            SKINS[i] = JTTWEngine.id("textures/entity/strong_general_" + StrongGeneralEntity.NAMES[i] + ".png");
            GLOWS[i] = RenderType.eyes(JTTWEngine.id("textures/entity/strong_general_" + StrongGeneralEntity.NAMES[i] + "_glow.png"));
        }
    }

    public StrongGeneralRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.6f);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, StrongGeneralEntity e, float swing, float amount,
                               float partial, float age, float yaw, float pitch) {
                VertexConsumer vc = buffers.getBuffer(GLOWS[e.variant()]);
                getParentModel().renderToBuffer(pose, vc, 0xF00000, OverlayTexture.NO_OVERLAY, -1);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(StrongGeneralEntity e) {
        return SKINS[e.variant()];
    }

    @Override
    protected boolean shouldShowName(StrongGeneralEntity e) {
        return super.shouldShowName(e) && FireCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-5, -2, -3, 10, 14, 6)
                .texOffs(17, 52).mirror(false).addBox(-5.5f, 8, -3.4f, 11, 1.5f, 6.8f)
                .texOffs(0, 36).mirror(false).addBox(-5.5f, 9.5f, -3.6f, 11, 6.5f, 7.2f)
                .texOffs(109, 52).mirror(false).addBox(-2, -0.5f, -3.6f, 4, 4, 0.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(76, 0).mirror(false).addBox(-4, -8, -4, 8, 8, 8)
                .texOffs(39, 36).mirror(false).addBox(-4.6f, -9.5f, -4.6f, 9.2f, 4.5f, 9.2f)
                .texOffs(13, 63).mirror(false).addBox(-1.5f, -1.4f, -4.4f, 1, 1.6f, 0.6f)
                .texOffs(13, 63).mirror(false).addBox(0.5f, -1.4f, -4.4f, 1, 1.6f, 0.6f), PartPose.offsetAndRotation(0, -2, 0, 0, 0, 0));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(0, 52).mirror(false).addBox(-0.75f, -4, -3, 1.5f, 4, 6), PartPose.offsetAndRotation(0, -9.5f, 0, 0, 0, 0));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(100, 52).mirror(false).addBox(-0.75f, -5, -0.75f, 1.5f, 5, 1.5f), PartPose.offsetAndRotation(-3.5f, -8.5f, -1, 0, 0, -0.5f));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(100, 52).mirror(true).addBox(-0.75f, -5, -0.75f, 1.5f, 5, 1.5f), PartPose.offsetAndRotation(3.5f, -8.5f, -1, 0, 0, 0.5f));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(38, 0).mirror(false).addBox(-2.5f, -2, -2, 4, 13, 4)
                .texOffs(93, 36).mirror(false).addBox(-3.3f, -3, -2.8f, 5.6f, 4.5f, 5.6f)
                .texOffs(79, 52).mirror(false).addBox(-2.9f, 8, -2.4f, 4.8f, 2.5f, 4.8f), PartPose.offsetAndRotation(-6.5f, 0, 0, 0, 0, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(38, 0).mirror(true).addBox(-1.5f, -2, -2, 4, 13, 4)
                .texOffs(93, 36).mirror(true).addBox(-2.3f, -3, -2.8f, 5.6f, 4.5f, 5.6f)
                .texOffs(79, 52).mirror(true).addBox(-1.9f, 8, -2.4f, 4.8f, 2.5f, 4.8f), PartPose.offsetAndRotation(6.5f, 0, 0, 0, 0, 0));
        PartDefinition glaive = rightArm.addOrReplaceChild("glaive", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5f, -22, -0.5f, 1, 34, 1)
                .texOffs(80, 36).mirror(false).addBox(-0.6f, -30, -1.8f, 1.2f, 8, 3.2f)
                .texOffs(0, 63).mirror(false).addBox(-1.5f, -22.5f, -1.2f, 3, 1, 2.4f), PartPose.offsetAndRotation(-0.5f, 10, 0, 1.5708f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(55, 0).mirror(false).addBox(-2.2f, 0, -2.2f, 4.4f, 12, 4.4f)
                .texOffs(54, 52).mirror(false).addBox(-2.6f, 9.5f, -2.8f, 5.2f, 2.5f, 5.6f), PartPose.offsetAndRotation(-2.4f, 12, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(55, 0).mirror(true).addBox(-2.2f, 0, -2.2f, 4.4f, 12, 4.4f)
                .texOffs(54, 52).mirror(true).addBox(-2.6f, 9.5f, -2.8f, 5.2f, 2.5f, 5.6f), PartPose.offsetAndRotation(2.4f, 12, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<StrongGeneralEntity> {
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
        public void setupAnim(StrongGeneralEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            float stride = Mth.cos(swing * 0.6662f) * 1.2f * amount;
            rightLeg.xRot = stride;
            leftLeg.xRot = -stride;
            leftArm.xRot = stride * 0.6f;
            leftArm.zRot = -0.08f;
            // the glaive held slanted before him; swung down on a blow
            float blow = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.55f - stride * 0.3f - blow * 1.5f;
            rightArm.zRot = 0.08f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
