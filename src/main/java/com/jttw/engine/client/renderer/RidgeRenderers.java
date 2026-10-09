package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.ridge.BoneServantEntity;
import com.jttw.engine.entity.ridge.LadyWhiteBoneEntity;
import com.jttw.engine.entity.ridge.WraithEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** How White Bone Ridge's dead look: models and renderers for the Bone Servant, the Wraith and Lady White Bone. */
public final class RidgeRenderers {

    private RidgeRenderers() {}

    private static final ResourceLocation SKELETON = new ResourceLocation("minecraft", "textures/entity/skeleton/skeleton.png");
    public static final ResourceLocation WRAITH = new ResourceLocation(JTTWEngine.MOD_ID, "textures/entity/wraith.png");
    public static final ResourceLocation WRAITH_CAPTAIN = new ResourceLocation(JTTWEngine.MOD_ID, "textures/entity/wraith_captain.png");

    public static final ModelLayerLocation WRAITH_LAYER = new ModelLayerLocation(new ResourceLocation(JTTWEngine.MOD_ID, "wraith"), "main");
    public static final ModelLayerLocation VEIL_LAYER = new ModelLayerLocation(new ResourceLocation(JTTWEngine.MOD_ID, "lady_white_bone"), "veil");

    /** Names over heads show only this close, so a crowd of the dead is not a wall of labels. */
    private static final double NAME_RANGE = 24;

    public static LayerDefinition wraithLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0f), 64, 32);
    }

    public static final ResourceLocation LADY_VEIL = new ResourceLocation(JTTWEngine.MOD_ID, "textures/entity/lady_white_bone_veil.png");

    /**
     * Her veil over the skeleton: a hood, long black hair down her back, a robe with sleeves, and a
     * long skirt from her waist to the ground (her legs are hidden under it).
     */
    public static LayerDefinition veilLayer() {
        net.minecraft.client.model.geom.builders.MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.45f), 0f);
        var root = mesh.getRoot();
        root.getChild("body").addOrReplaceChild("skirt", net.minecraft.client.model.geom.builders.CubeListBuilder.create()
                .texOffs(0, 32).addBox(-5f, 10f, -3.5f, 10f, 14f, 7f, new CubeDeformation(0.2f)), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("head").addOrReplaceChild("hair", net.minecraft.client.model.geom.builders.CubeListBuilder.create()
                .texOffs(36, 32).addBox(-4f, -8f, 4.1f, 8f, 13f, 1f, new CubeDeformation(0.5f)), net.minecraft.client.model.geom.PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static boolean near(net.minecraft.client.renderer.entity.EntityRenderDispatcher d, net.minecraft.world.entity.Entity e) {
        return d.distanceToSqr(e) < NAME_RANGE * NAME_RANGE;
    }

    // ── Bone Servant ────────────────────────────────────────────────────

    /** A skeleton's frame, its club raised over its head before a blow. */
    public static final class BoneServantModel extends HumanoidModel<BoneServantEntity> {
        public BoneServantModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(BoneServantEntity e, float swing, float swingAmount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing, swingAmount, age, yaw, pitch);
            if (e.isRaising()) {
                rightArm.xRot = -2.9f;
                leftArm.xRot = -2.7f;
                rightArm.zRot = 0.15f;
                leftArm.zRot = -0.15f;
                body.xRot = -0.15f;
            }
        }
    }

    public static final class BoneServantRenderer extends HumanoidMobRenderer<BoneServantEntity, BoneServantModel> {
        /** Four blocks tall: twice a skeleton. */
        public static final float SIZE = 2.0f;

        public BoneServantRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new BoneServantModel(ctx.bakeLayer(ModelLayers.SKELETON)), 0.5f * SIZE);
        }

        @Override
        protected void scale(BoneServantEntity e, PoseStack pose, float partial) {
            float s = SIZE * e.getScale();
            pose.scale(s, s, s);
        }

        @Override
        public ResourceLocation getTextureLocation(BoneServantEntity e) {
            return SKELETON;
        }

        @Override
        protected boolean shouldShowName(BoneServantEntity e) {
            return super.shouldShowName(e) && near(entityRenderDispatcher, e);
        }
    }

    // ── Wraith ──────────────────────────────────────────────────────────

    /** A ghost: no legs, arms reaching, swaying as it drifts. */
    public static final class WraithModel extends HumanoidModel<WraithEntity> {
        public WraithModel(ModelPart root) {
            super(root);
            leftLeg.visible = false;
            rightLeg.visible = false;
        }

        @Override
        public void setupAnim(WraithEntity e, float swing, float swingAmount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing, swingAmount, age, yaw, pitch);
            AnimationUtils.animateZombieArms(leftArm, rightArm, e.isAggressive(), attackTime, age);
            float sway = Mth.sin(age * 0.08f);
            body.zRot = sway * 0.06f;
            head.zRot = sway * 0.05f;
        }
    }

    public static final class WraithRenderer extends HumanoidMobRenderer<WraithEntity, WraithModel> {
        public WraithRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new WraithModel(ctx.bakeLayer(WRAITH_LAYER)), 0.2f);
        }

        @Override
        protected void scale(WraithEntity e, PoseStack pose, float partial) {
            float s = e.getScale();
            pose.scale(s, s, s);
            // bobs as it hangs in the air
            pose.translate(0, Mth.sin((e.tickCount + partial) * 0.1f) * 0.06f, 0);
        }

        @Override
        public ResourceLocation getTextureLocation(WraithEntity e) {
            return e.isCaptain() ? WRAITH_CAPTAIN : WRAITH;
        }

        /** Half seen: drawn through its texture's own transparency. */
        @Override
        protected RenderType getRenderType(WraithEntity e, boolean visible, boolean translucent, boolean glowing) {
            return RenderType.entityTranslucent(getTextureLocation(e));
        }

        /** A faint light of its own, so it shows pale in the dark. */
        @Override
        protected int getBlockLightLevel(WraithEntity e, BlockPos pos) {
            return Math.max(10, super.getBlockLightLevel(e, pos));
        }

        @Override
        protected boolean shouldShowName(WraithEntity e) {
            return super.shouldShowName(e) && near(entityRenderDispatcher, e);
        }
    }

    // ── Lady White Bone ─────────────────────────────────────────────────

    /** A skeleton, tall and thin, arm raised to throw when she fights. */
    public static final class LadyModel extends HumanoidModel<LadyWhiteBoneEntity> {
        public LadyModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(LadyWhiteBoneEntity e, float swing, float swingAmount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing, swingAmount, age, yaw, pitch);
            if (e.isAggressive()) {
                rightArm.xRot = -1.5f + Mth.sin(age * 0.2f) * 0.1f;
                leftArm.xRot = -0.4f;
                leftArm.zRot = -0.5f;
            }
        }
    }

    public static final class LadyWhiteBoneRenderer extends HumanoidMobRenderer<LadyWhiteBoneEntity, LadyModel> {
        public static final float SIZE = 1.45f;

        public LadyWhiteBoneRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new LadyModel(ctx.bakeLayer(ModelLayers.SKELETON)), 0.6f);
            addLayer(new Veil(this, new HumanoidModel<>(ctx.bakeLayer(VEIL_LAYER))));
        }

        @Override
        protected void scale(LadyWhiteBoneEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(LadyWhiteBoneEntity e) {
            return SKELETON;
        }
    }

    /** Her veil: a ghost's shape about her bones, half seen. */
    static final class Veil extends RenderLayer<LadyWhiteBoneEntity, LadyModel> {
        private final HumanoidModel<LadyWhiteBoneEntity> veil;

        Veil(RenderLayerParent<LadyWhiteBoneEntity, LadyModel> parent, HumanoidModel<LadyWhiteBoneEntity> veil) {
            super(parent);
            this.veil = veil;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, LadyWhiteBoneEntity e,
                           float swing, float swingAmount, float partial, float age, float yaw, float pitch) {
            getParentModel().copyPropertiesTo(veil);
            veil.prepareMobModel(e, swing, swingAmount, partial);
            veil.setupAnim(e, swing, swingAmount, age, yaw, pitch);
            veil.rightArm.copyFrom(getParentModel().rightArm);
            veil.leftArm.copyFrom(getParentModel().leftArm);
            veil.leftLeg.visible = false;
            veil.rightLeg.visible = false;
            VertexConsumer v = buffers.getBuffer(RenderType.entityTranslucent(LADY_VEIL));
            veil.renderToBuffer(pose, v, light, LivingEntityRenderer.getOverlayCoords(e, 0f), 1f, 1f, 1f, 0.85f);
        }
    }
}
