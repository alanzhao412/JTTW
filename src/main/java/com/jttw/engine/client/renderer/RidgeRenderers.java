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
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/** How White Bone Ridge's dead look: models and renderers for the Bone Servant, the Wraith and Lady White Bone. */
public final class RidgeRenderers {

    private RidgeRenderers() {}

    private static final ResourceLocation SKELETON = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/skeleton/skeleton.png");
    public static final ResourceLocation WRAITH = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/wraith.png");
    public static final ResourceLocation WRAITH_CAPTAIN = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/wraith_captain.png");

    public static final ModelLayerLocation WRAITH_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "wraith"), "main");
    public static final ModelLayerLocation LADY_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "lady_white_bone"), "main");

    /** Names over heads show only this close, so a crowd of the dead is not a wall of labels. */
    private static final double NAME_RANGE = 24;

    public static LayerDefinition wraithLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0f), 64, 32);
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
            // a captain's getScale() is applied by LivingEntityRenderer
            pose.scale(SIZE, SIZE, SIZE);
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

    /**
     * How solid she is drawn: 1, or less for the mirage of her true form that the Fiery Golden
     * Eyes and the Eye of Heaven see over her disguise.
     */
    static float mirage = 1f;

    public static final ResourceLocation LADY = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/lady_white_bone.png");
    private static final RenderType LADY_GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/lady_white_bone_glow.png"));

    /**
     * Her true form, in model pixels with the ground at y 24 (the boxes' texture offsets are where
     * the generated skin is painted: see {@code assets_source/white_bone.py}, which writes this).
     */
    public static LayerDefinition ladyLayer() {
        // GENERATED lady: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(44, 24).mirror(false).addBox(-4.5f, -11, -3, 9, 9, 6)
                .texOffs(102, 42).mirror(false).addBox(-3.5f, -10.5f, -2.5f, 7, 8, 5)
                .texOffs(0, 71).mirror(false).addBox(-2.5f, -2.5f, -2, 5, 3, 4)
                .texOffs(0, 24).mirror(false).addBox(-4.5f, -13, 3.1f, 9, 16, 1)
                .texOffs(85, 71).mirror(false).addBox(-1, -14.5f, -1.3f, 2, 4, 2)
                .texOffs(101, 79).mirror(false).addBox(-3.9f, -10.6f, -3.5f, 1.4f, 1.4f, 1)
                .texOffs(101, 79).mirror(false).addBox(2.5f, -10.6f, -3.5f, 1.4f, 1.4f, 1)
                .texOffs(101, 79).mirror(false).addBox(-3, -9.5f, -3.4f, 1.4f, 1.4f, 1)
                .texOffs(101, 79).mirror(false).addBox(1.6f, -9.5f, -3.4f, 1.4f, 1.4f, 1)
                .texOffs(46, 79).mirror(false).addBox(-1, -8.8f, -3.6f, 2, 2, 1.2f), PartPose.offsetAndRotation(0, -3, 0, 0, 0, 0));
        PartDefinition ridge0 = torso.addOrReplaceChild("ridge_0", CubeListBuilder.create()
                .texOffs(19, 71).mirror(false).addBox(-0.6f, -0.7f, 0, 1.2f, 1.4f, 4.2f), PartPose.offsetAndRotation(0, -11, 2.6f, 0.5f, 0, 0));
        PartDefinition ridge1 = torso.addOrReplaceChild("ridge_1", CubeListBuilder.create()
                .texOffs(94, 71).mirror(false).addBox(-0.6f, -0.7f, 0, 1.2f, 1.4f, 3.8f), PartPose.offsetAndRotation(0, -8.4f, 2.6f, 0.5f, 0, 0));
        PartDefinition ridge2 = torso.addOrReplaceChild("ridge_2", CubeListBuilder.create()
                .texOffs(94, 71).mirror(false).addBox(-0.6f, -0.7f, 0, 1.2f, 1.4f, 3.3f), PartPose.offsetAndRotation(0, -5.8f, 2.6f, 0.5f, 0, 0));
        PartDefinition ridge3 = torso.addOrReplaceChild("ridge_3", CubeListBuilder.create()
                .texOffs(0, 79).mirror(false).addBox(-0.6f, -0.7f, 0, 1.2f, 1.4f, 2.8f), PartPose.offsetAndRotation(0, -3.2f, 2.6f, 0.5f, 0, 0));
        PartDefinition ridge4 = torso.addOrReplaceChild("ridge_4", CubeListBuilder.create()
                .texOffs(0, 79).mirror(false).addBox(-0.6f, -0.7f, 0, 1.2f, 1.4f, 2.2f), PartPose.offsetAndRotation(0, -0.8f, 2.6f, 0.5f, 0, 0));
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(75, 24).mirror(false).addBox(-3, -8, -3.5f, 6, 8, 7)
                .texOffs(0, 42).mirror(false).addBox(-3.5f, -8.6f, -2.9f, 7, 7, 7)
                .texOffs(53, 57).mirror(false).addBox(-4, -9, -4.2f, 8, 1, 9)
                .texOffs(46, 79).mirror(false).addBox(-1, -10.6f, -4.6f, 2, 2, 1.2f)
                .texOffs(55, 0).mirror(false).addBox(-3.9f, -6, -3.7f, 1.3f, 18, 1)
                .texOffs(23, 57).mirror(false).addBox(2.6f, -6, -2.9f, 1.3f, 11, 1)
                .texOffs(0, 57).mirror(false).addBox(-3.3f, -7, -3.9f, 1.2f, 12, 0.4f)
                .texOffs(29, 42).mirror(false).addBox(-4.2f, -7.5f, -2, 1, 9, 5)
                .texOffs(30, 57).mirror(false).addBox(3.2f, -7.5f, -1, 1, 7, 4), PartPose.offsetAndRotation(0, -13.5f, -0.5f, 0, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(34, 71).mirror(false).addBox(-2, 0, -3, 4, 3, 4), PartPose.offsetAndRotation(0, -0.2f, -0.6f, 0.16f, 0, 0));
        PartDefinition crown0 = head.addOrReplaceChild("crown_0", CubeListBuilder.create()
                .texOffs(107, 71).mirror(false).addBox(-0.6f, -4, -0.6f, 1.2f, 4, 1.2f), PartPose.offsetAndRotation(0, -9, -3.4f, -0.08f, 0, 0));
        PartDefinition crown0Tip = crown0.addOrReplaceChild("crown_0_tip", CubeListBuilder.create()
                .texOffs(11, 79).mirror(false).addBox(-0.4f, -4, -0.4f, 0.8f, 4, 0.8f), PartPose.offsetAndRotation(0, -3.8f, 0, -0.2f, 0, 0));
        PartDefinition crown1 = head.addOrReplaceChild("crown_1", CubeListBuilder.create()
                .texOffs(16, 79).mirror(false).addBox(-0.6f, -3, -0.6f, 1.2f, 3, 1.2f), PartPose.offsetAndRotation(-1.9f, -9, -3.5f, 0, 0, -0.3f));
        PartDefinition crown1Tip = crown1.addOrReplaceChild("crown_1_tip", CubeListBuilder.create()
                .texOffs(55, 79).mirror(false).addBox(-0.4f, -3, -0.4f, 0.8f, 3, 0.8f), PartPose.offsetAndRotation(0, -2.8f, 0, 0, 0, -0.3f));
        PartDefinition crown2 = head.addOrReplaceChild("crown_2", CubeListBuilder.create()
                .texOffs(16, 79).mirror(false).addBox(-0.6f, -3, -0.6f, 1.2f, 3, 1.2f), PartPose.offsetAndRotation(1.9f, -9, -3.5f, 0.05f, 0, 0.26f));
        PartDefinition crown2Tip = crown2.addOrReplaceChild("crown_2_tip", CubeListBuilder.create()
                .texOffs(55, 79).mirror(false).addBox(-0.4f, -2.5f, -0.4f, 0.8f, 2.5f, 0.8f), PartPose.offsetAndRotation(0, -2.8f, 0, 0, 0, 0.35f));
        PartDefinition crown3 = head.addOrReplaceChild("crown_3", CubeListBuilder.create()
                .texOffs(16, 79).mirror(false).addBox(-0.6f, -2.5f, -0.6f, 1.2f, 2.5f, 1.2f), PartPose.offsetAndRotation(-3.5f, -9, -2, -0.1f, 0, -0.7f));
        PartDefinition crown3Tip = crown3.addOrReplaceChild("crown_3_tip", CubeListBuilder.create()
                .texOffs(55, 79).mirror(false).addBox(-0.4f, -2.5f, -0.4f, 0.8f, 2.5f, 0.8f), PartPose.offsetAndRotation(0, -2.3f, 0, 0, 0, -0.35f));
        PartDefinition crown4 = head.addOrReplaceChild("crown_4", CubeListBuilder.create()
                .texOffs(16, 79).mirror(false).addBox(-0.6f, -2.5f, -0.6f, 1.2f, 2.5f, 1.2f), PartPose.offsetAndRotation(3.5f, -9, -2, -0.1f, 0, 0.6f));
        PartDefinition crown4Tip = crown4.addOrReplaceChild("crown_4_tip", CubeListBuilder.create()
                .texOffs(108, 79).mirror(false).addBox(-0.4f, -2, -0.4f, 0.8f, 2, 0.8f), PartPose.offsetAndRotation(0, -2.3f, 0, 0, 0, 0.3f));
        PartDefinition crown6 = head.addOrReplaceChild("crown_6", CubeListBuilder.create()
                .texOffs(60, 79).mirror(false).addBox(-0.6f, -2, -0.6f, 1.2f, 2, 1.2f), PartPose.offsetAndRotation(0, -9, 4.2f, -0.7f, 0, 0));
        PartDefinition crown6Tip = crown6.addOrReplaceChild("crown_6_tip", CubeListBuilder.create()
                .texOffs(108, 79).mirror(false).addBox(-0.4f, -2, -0.4f, 0.8f, 2, 0.8f), PartPose.offsetAndRotation(0, -1.8f, 0, -0.3f, 0, 0));
        PartDefinition crown5 = head.addOrReplaceChild("crown_5", CubeListBuilder.create()
                .texOffs(113, 79).mirror(false).addBox(-0.5f, -2, -0.5f, 1, 2, 1), PartPose.offsetAndRotation(-3.7f, -9, 1, -0.3f, 0, -1));
        PartDefinition horn = head.addOrReplaceChild("horn", CubeListBuilder.create()
                .texOffs(95, 57).mirror(false).addBox(-0.75f, -6, -0.75f, 1.5f, 6, 1.5f), PartPose.offsetAndRotation(3.3f, -8.6f, 1.6f, -0.8f, 0, 0.5f));
        PartDefinition hornTip = horn.addOrReplaceChild("horn_tip", CubeListBuilder.create()
                .texOffs(116, 71).mirror(false).addBox(-0.5f, -5, -0.5f, 1, 5, 1), PartPose.offsetAndRotation(0, -5.8f, 0, -0.6f, 0, -0.35f));
        PartDefinition shoulderSpike = torso.addOrReplaceChild("shoulder_spike", CubeListBuilder.create()
                .texOffs(51, 71).mirror(false).addBox(-0.75f, -5, -0.75f, 1.5f, 5, 1.5f), PartPose.offsetAndRotation(5, -11.6f, 0.6f, -0.3f, 0, 0.42f));
        PartDefinition shoulderSpikeTip = shoulderSpike.addOrReplaceChild("shoulder_spike_tip", CubeListBuilder.create()
                .texOffs(11, 79).mirror(false).addBox(-0.5f, -4, -0.5f, 1, 4, 1), PartPose.offsetAndRotation(0, -4.8f, 0, -0.45f, 0, -0.5f));
        PartDefinition rightArm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(7, 57).mirror(false).addBox(-0.75f, 0, -0.75f, 1.5f, 11, 1.5f)
                .texOffs(102, 24).mirror(false).addBox(-2.5f, -1.6f, -2.5f, 5, 10, 5)
                .texOffs(69, 79).mirror(false).addBox(-1.25f, -2.4f, -1.25f, 2.5f, 1, 2.5f), PartPose.offsetAndRotation(-5.6f, -10.2f, 0, 0, 0, 0));
        PartDefinition leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(7, 57).mirror(true).addBox(-0.75f, 0, -0.75f, 1.5f, 11, 1.5f)
                .texOffs(102, 24).mirror(true).addBox(-2.5f, -1.6f, -2.5f, 5, 10, 5)
                .texOffs(69, 79).mirror(true).addBox(-1.25f, -2.4f, -1.25f, 2.5f, 1, 2.5f), PartPose.offsetAndRotation(5.6f, -10.2f, 0, 0, 0, 0));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(41, 57).mirror(false).addBox(-0.9f, 0, -0.45f, 0.8f, 10, 0.8f)
                .texOffs(41, 57).mirror(false).addBox(0.1f, 0, -0.35f, 0.8f, 10, 0.8f)
                .texOffs(82, 79).mirror(false).addBox(-1, -0.7f, -0.7f, 2, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 11, 0, 0, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(41, 57).mirror(true).addBox(0.1f, 0, -0.45f, 0.8f, 10, 0.8f)
                .texOffs(41, 57).mirror(true).addBox(-0.9f, 0, -0.35f, 0.8f, 10, 0.8f)
                .texOffs(82, 79).mirror(true).addBox(-1, -0.7f, -0.7f, 2, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 11, 0, 0, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(25, 79).mirror(false).addBox(-1.3f, 0, -0.7f, 2.6f, 2.6f, 1.4f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(25, 79).mirror(true).addBox(-1.3f, 0, -0.7f, 2.6f, 2.6f, 1.4f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition rightFinger0 = rightHand.addOrReplaceChild("right_finger_0", CubeListBuilder.create()
                .texOffs(36, 79).mirror(false).addBox(-0.35f, 0, -0.35f, 0.7f, 3.2f, 0.7f), PartPose.offsetAndRotation(-1, 2.4f, 0, 0, 0, 0));
        PartDefinition leftFinger0 = leftHand.addOrReplaceChild("left_finger_0", CubeListBuilder.create()
                .texOffs(36, 79).mirror(true).addBox(-0.35f, 0, -0.35f, 0.7f, 3.2f, 0.7f), PartPose.offsetAndRotation(1, 2.4f, 0, 0, 0, 0));
        PartDefinition rightClaw0 = rightFinger0.addOrReplaceChild("right_claw_0", CubeListBuilder.create()
                .texOffs(91, 79).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3, 0.6f), PartPose.offsetAndRotation(0, 3.1f, 0, 0, 0, 0));
        PartDefinition leftClaw0 = leftFinger0.addOrReplaceChild("left_claw_0", CubeListBuilder.create()
                .texOffs(91, 79).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 3, 0.6f), PartPose.offsetAndRotation(0, 3.1f, 0, 0, 0, 0));
        PartDefinition rightFinger1 = rightHand.addOrReplaceChild("right_finger_1", CubeListBuilder.create()
                .texOffs(36, 79).mirror(false).addBox(-0.35f, 0, -0.35f, 0.7f, 3.8f, 0.7f), PartPose.offsetAndRotation(-0.33f, 2.4f, 0, 0, 0, 0));
        PartDefinition leftFinger1 = leftHand.addOrReplaceChild("left_finger_1", CubeListBuilder.create()
                .texOffs(36, 79).mirror(true).addBox(-0.35f, 0, -0.35f, 0.7f, 3.8f, 0.7f), PartPose.offsetAndRotation(0.33f, 2.4f, 0, 0, 0, 0));
        PartDefinition rightClaw1 = rightFinger1.addOrReplaceChild("right_claw_1", CubeListBuilder.create()
                .texOffs(41, 79).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.6f, 0.6f), PartPose.offsetAndRotation(0, 3.7f, 0, 0, 0, 0));
        PartDefinition leftClaw1 = leftFinger1.addOrReplaceChild("left_claw_1", CubeListBuilder.create()
                .texOffs(41, 79).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 3.6f, 0.6f), PartPose.offsetAndRotation(0, 3.7f, 0, 0, 0, 0));
        PartDefinition rightFinger2 = rightHand.addOrReplaceChild("right_finger_2", CubeListBuilder.create()
                .texOffs(36, 79).mirror(false).addBox(-0.35f, 0, -0.35f, 0.7f, 3.6f, 0.7f), PartPose.offsetAndRotation(0.33f, 2.4f, 0, 0, 0, 0));
        PartDefinition leftFinger2 = leftHand.addOrReplaceChild("left_finger_2", CubeListBuilder.create()
                .texOffs(36, 79).mirror(true).addBox(-0.35f, 0, -0.35f, 0.7f, 3.6f, 0.7f), PartPose.offsetAndRotation(-0.33f, 2.4f, 0, 0, 0, 0));
        PartDefinition rightClaw2 = rightFinger2.addOrReplaceChild("right_claw_2", CubeListBuilder.create()
                .texOffs(41, 79).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f), PartPose.offsetAndRotation(0, 3.5f, 0, 0, 0, 0));
        PartDefinition leftClaw2 = leftFinger2.addOrReplaceChild("left_claw_2", CubeListBuilder.create()
                .texOffs(41, 79).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f), PartPose.offsetAndRotation(0, 3.5f, 0, 0, 0, 0));
        PartDefinition rightFinger3 = rightHand.addOrReplaceChild("right_finger_3", CubeListBuilder.create()
                .texOffs(96, 79).mirror(false).addBox(-0.35f, 0, -0.35f, 0.7f, 3, 0.7f), PartPose.offsetAndRotation(1, 2.4f, 0, 0, 0, 0));
        PartDefinition leftFinger3 = leftHand.addOrReplaceChild("left_finger_3", CubeListBuilder.create()
                .texOffs(96, 79).mirror(true).addBox(-0.35f, 0, -0.35f, 0.7f, 3, 0.7f), PartPose.offsetAndRotation(-1, 2.4f, 0, 0, 0, 0));
        PartDefinition rightClaw3 = rightFinger3.addOrReplaceChild("right_claw_3", CubeListBuilder.create()
                .texOffs(91, 79).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 2.8f, 0.6f), PartPose.offsetAndRotation(0, 2.9f, 0, 0, 0, 0));
        PartDefinition leftClaw3 = leftFinger3.addOrReplaceChild("left_claw_3", CubeListBuilder.create()
                .texOffs(91, 79).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 2.8f, 0.6f), PartPose.offsetAndRotation(0, 2.9f, 0, 0, 0, 0));
        PartDefinition rightThumb = rightHand.addOrReplaceChild("right_thumb", CubeListBuilder.create()
                .texOffs(96, 79).mirror(false).addBox(-0.35f, 0, -0.35f, 0.7f, 2.6f, 0.7f), PartPose.offsetAndRotation(1.2f, 1, -0.5f, 0, 0, 0));
        PartDefinition leftThumb = leftHand.addOrReplaceChild("left_thumb", CubeListBuilder.create()
                .texOffs(96, 79).mirror(true).addBox(-0.35f, 0, -0.35f, 0.7f, 2.6f, 0.7f), PartPose.offsetAndRotation(-1.2f, 1, -0.5f, 0, 0, 0));
        PartDefinition rightThumbClaw = rightThumb.addOrReplaceChild("right_thumb_claw", CubeListBuilder.create()
                .texOffs(91, 79).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 2.4f, 0.6f), PartPose.offsetAndRotation(0, 2.5f, 0, 0, 0, 0));
        PartDefinition leftThumbClaw = leftThumb.addOrReplaceChild("left_thumb_claw", CubeListBuilder.create()
                .texOffs(91, 79).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 2.4f, 0.6f), PartPose.offsetAndRotation(0, 2.5f, 0, 0, 0, 0));
        PartDefinition skirt = root.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(60, 71).mirror(false).addBox(-3.5f, -0.5f, -2.5f, 7, 2, 5)
                .texOffs(42, 42).mirror(false).addBox(-4, 1, -3, 8, 8, 6)
                .texOffs(62, 0).mirror(false).addBox(-5.5f, 8, -4.5f, 11, 9, 9)
                .texOffs(104, 57).mirror(false).addBox(2.4f, 1.4f, -3.4f, 1.5f, 7, 0.4f), PartPose.offsetAndRotation(0, -3, 0, 0, 0, 0));
        PartDefinition streamer0 = skirt.addOrReplaceChild("streamer_0", CubeListBuilder.create()
                .texOffs(103, 0).mirror(false).addBox(-0.8f, 0, -0.3f, 1.6f, 17, 0.3f), PartPose.offsetAndRotation(1, 0.8f, -2.7f, -0.27f, 0, 0.03f));
        PartDefinition streamer1 = skirt.addOrReplaceChild("streamer_1", CubeListBuilder.create()
                .texOffs(16, 57).mirror(false).addBox(-0.6f, 0, -0.3f, 1.2f, 12, 0.3f), PartPose.offsetAndRotation(-2.3f, 0.8f, -2.7f, -0.25f, 0, -0.05f));
        PartDefinition hem = skirt.addOrReplaceChild("hem", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-7.5f, 0, -6, 15, 11, 12), PartPose.offsetAndRotation(0, 16, 0, 0, 0, 0));
        PartDefinition shroud = skirt.addOrReplaceChild("shroud", CubeListBuilder.create()
                .texOffs(21, 24).mirror(false).addBox(-5, 0, -0.5f, 10, 15, 1), PartPose.offsetAndRotation(0, 0.5f, 2.6f, 0.27f, 0, 0));
        PartDefinition shroudLow = shroud.addOrReplaceChild("shroud_low", CubeListBuilder.create()
                .texOffs(71, 42).mirror(false).addBox(-7, -0.2f, -0.5f, 14, 12.8f, 1), PartPose.offsetAndRotation(0, 15, 0, 0.13f, 0, 0));
        PartDefinition tatter0 = hem.addOrReplaceChild("tatter_0", CubeListBuilder.create()
                .texOffs(46, 57).mirror(false).addBox(-0.8f, 0, -0.2f, 1.6f, 9.5f, 0.4f), PartPose.offsetAndRotation(-4.5f, 1, -6.2f, -0.06f, 0, 0.02f));
        PartDefinition tatter1 = hem.addOrReplaceChild("tatter_1", CubeListBuilder.create()
                .texOffs(88, 57).mirror(false).addBox(-0.8f, 0, -0.2f, 1.6f, 8.5f, 0.4f), PartPose.offsetAndRotation(2, 1, -6.2f, -0.05f, 0, -0.03f));
        PartDefinition tatter2 = hem.addOrReplaceChild("tatter_2", CubeListBuilder.create()
                .texOffs(88, 57).mirror(false).addBox(-0.8f, 0, -0.2f, 1.6f, 9, 0.4f), PartPose.offsetAndRotation(5.8f, 1, -6.2f, -0.07f, 0, -0.05f));
        PartDefinition tatter3 = hem.addOrReplaceChild("tatter_3", CubeListBuilder.create()
                .texOffs(88, 57).mirror(false).addBox(-0.8f, 0, -0.2f, 1.6f, 9, 0.4f), PartPose.offsetAndRotation(-7.7f, 1, -2.5f, 0, 1.5708f, 0.07f));
        PartDefinition tatter4 = hem.addOrReplaceChild("tatter_4", CubeListBuilder.create()
                .texOffs(88, 57).mirror(false).addBox(-0.8f, 0, -0.2f, 1.6f, 8.5f, 0.4f), PartPose.offsetAndRotation(-7.7f, 1, 3, 0, 1.5708f, 0.06f));
        PartDefinition tatter5 = hem.addOrReplaceChild("tatter_5", CubeListBuilder.create()
                .texOffs(46, 57).mirror(false).addBox(-0.8f, 0, -0.2f, 1.6f, 9.5f, 0.4f), PartPose.offsetAndRotation(7.7f, 1, 0.5f, 0, 1.5708f, -0.07f));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED lady: end
    }

    /**
     * Lady White Bone as she truly is: a gaunt corpse-fiend half again a man's height, in cracked
     * porcelain-white bone under a crown of finger bones and long black hair, her ribs bare through
     * a torn grave shroud. She stands hunched forward from her waist, her head tilted, her right hand
     * hanging with its claws twitching and her left raised beside her, clawed; she glides, her shroud
     * swaying and trailing behind her. Fighting, she lifts her right hand high to hurl and
     * reaches for her foe with her left, claws spread, her jaw dropping.
     */
    public static final class LadyModel extends net.minecraft.client.model.HierarchicalModel<LadyWhiteBoneEntity> {
        private final ModelPart root, torso, head, jaw, skirt, hem, shroud;
        private final ModelPart[] arm = new ModelPart[2], forearm = new ModelPart[2], hand = new ModelPart[2], thumb = new ModelPart[2];
        private final ModelPart[][] finger = new ModelPart[2][4], claw = new ModelPart[2][4];

        public LadyModel(ModelPart root) {
            this.root = root;
            torso = root.getChild("torso");
            head = torso.getChild("head");
            jaw = head.getChild("jaw");
            skirt = root.getChild("skirt");
            hem = skirt.getChild("hem");
            shroud = skirt.getChild("shroud");
            for (int s = 0; s < 2; s++) {
                String n = s == 0 ? "right" : "left";
                arm[s] = torso.getChild(n + "_arm");
                forearm[s] = arm[s].getChild(n + "_forearm");
                hand[s] = forearm[s].getChild(n + "_hand");
                thumb[s] = hand[s].getChild(n + "_thumb");
                for (int k = 0; k < 4; k++) {
                    finger[s][k] = hand[s].getChild(n + "_finger_" + k);
                    claw[s][k] = finger[s][k].getChild(n + "_claw_" + k);
                }
            }
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(LadyWhiteBoneEntity e, float swing, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            boolean fighting = e.isAggressive();
            float breath = Mth.sin(age * 0.06f);
            // hunched forward from the waist, her head thrust out and tilted
            torso.xRot = 0.2f + breath * 0.02f + (fighting ? 0.06f : 0f);
            head.yRot = Mth.clamp(yaw, -60f, 60f) * Mth.DEG_TO_RAD * 0.6f;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.6f - 0.08f;
            head.zRot = 0.14f + Mth.sin(age * 0.023f) * 0.04f;
            jaw.xRot += (fighting ? 0.22f : 0f) + Math.max(0f, Mth.sin(age * 0.05f)) * 0.06f;
            // she glides: her shroud sways, its back lifting behind her as she goes
            float sway = Mth.cos(swing * 0.5f) * amount;
            skirt.xRot = sway * 0.06f;
            skirt.zRot = Mth.sin(age * 0.04f) * 0.015f;
            hem.xRot = Mth.cos(swing * 0.5f - 0.9f) * amount * 0.1f;
            shroud.xRot += amount * 0.25f + Mth.sin(age * 0.05f) * 0.02f;
            if (fighting) {
                // her right hand high to hurl (and down on the throw), her left reaching, claws spread
                float hurl = Mth.sin(attackTime * Mth.PI);
                arm[0].xRot = -2.7f + hurl * 1.8f;
                arm[0].zRot = 0.25f;
                forearm[0].xRot = -0.55f + Mth.sin(age * 0.2f) * 0.08f;
                arm[1].xRot = -1.2f + breath * 0.05f;
                arm[1].yRot = 0.3f;
                arm[1].zRot = -0.12f;
                forearm[1].xRot = -0.35f;
            } else {
                // her right hand hanging past her knee, her left raised and clawed beside her
                arm[0].xRot = -0.1f - sway * 0.25f;
                arm[0].zRot = 0.1f + breath * 0.02f;
                forearm[0].xRot = -0.3f;
                arm[1].xRot = -0.55f + sway * 0.2f;
                arm[1].yRot = -0.15f;
                arm[1].zRot = -0.32f;
                forearm[1].xRot = -1.45f + breath * 0.05f;
                hand[1].xRot = -0.25f;
            }
            for (int s = 0; s < 2; s++) {
                float out = s == 0 ? 1f : -1f; // the way a hanging bone leans outward from her, about z
                boolean spread = fighting || s == 1;
                for (int k = 0; k < 4; k++) {
                    float twitch = Mth.sin(age * 0.17f + k * 1.7f + s * 2f) * 0.08f;
                    finger[s][k].xRot = (spread ? -0.3f : -0.2f) + twitch;
                    finger[s][k].zRot = (1.5f - k) * (spread ? 0.22f : 0.06f) * out;
                    claw[s][k].xRot = (spread ? -0.75f : -0.5f) + twitch;
                }
                thumb[s].xRot = -0.5f;
                thumb[s].zRot = -0.45f * out;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            // the mirage: pale, cold, and thin as breath
            if (mirage < 1f) color = FastColor.ARGB32.color(
                    Math.round(FastColor.ARGB32.alpha(color) * mirage), Math.round(FastColor.ARGB32.red(color) * 0.8f),
                    Math.round(FastColor.ARGB32.green(color) * 0.9f), FastColor.ARGB32.blue(color));
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class LadyWhiteBoneRenderer extends net.minecraft.client.renderer.entity.MobRenderer<LadyWhiteBoneEntity, LadyModel> {

        public LadyWhiteBoneRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new LadyModel(ctx.bakeLayer(LADY_LAYER)), 0.6f);
            // her eyes and the ember behind her ribs: drawn full-bright, dim as they are
            addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return LADY_GLOW;
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(LadyWhiteBoneEntity e) {
            return LADY;
        }

        /** The stand-ins her disguises are drawn with, one per her and disguise. */
        private final java.util.Map<Integer, com.jttw.engine.entity.ridge.RidgeMonster> standIns = new java.util.HashMap<>();

        /** Whether the one looking sees what she truly is: the Fiery Golden Eyes or the Eye of Heaven open. */
        private static boolean seesTruth() {
            return GoldenEyesRenderer.isActive() || EyeOfHeavenRenderer.isActive();
        }

        /**
         * Disguised, she is drawn as the very thing she passes for: one of her dead, drawn by its
         * own renderer, posed as she is. Whoever has truth-seeing eyes open sees, over it, a
         * wavering mirage of her true form.
         */
        @Override
        public void render(LadyWhiteBoneEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            int d = e.disguise();
            if (d == LadyWhiteBoneEntity.HERSELF) {
                shadowRadius = 0.6f;
                super.render(e, yaw, partial, pose, buffers, light);
                return;
            }
            com.jttw.engine.entity.ridge.RidgeMonster standIn = standIn(e, d);
            if (standIn != null) {
                pose(standIn, e);
                entityRenderDispatcher.getRenderer(standIn).render(standIn, yaw, partial, pose, buffers, light);
            }
            shadowRadius = LadyWhiteBoneEntity.wraith(d) ? 0.2f : 0.5f * BoneServantRenderer.SIZE;
            if (seesTruth()) {
                mirage = 0.55f + 0.12f * Mth.sin((e.tickCount + partial) * 0.3f);
                // as tall as the shape she wears, so it is plainly that shape's wearer
                float fill = e.getBbHeight() / com.jttw.engine.registry.ModEntities.LADY_WHITE_BONE.get().getDimensions().height();
                pose.pushPose();
                pose.scale(fill, fill, fill);
                try {
                    // lit from within, so it shows the same by day or night
                    super.render(e, yaw, partial, pose, buffers, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
                } finally {
                    mirage = 1f;
                    pose.popPose();
                }
            }
        }

        private com.jttw.engine.entity.ridge.RidgeMonster standIn(LadyWhiteBoneEntity e, int d) {
            int key = e.getId() * 8 + d;
            com.jttw.engine.entity.ridge.RidgeMonster m = standIns.get(key);
            if (m == null || m.level() != e.level()) {
                m = (LadyWhiteBoneEntity.wraith(d) ? com.jttw.engine.registry.ModEntities.WRAITH : com.jttw.engine.registry.ModEntities.BONE_SERVANT).get().create(e.level());
                if (m == null) return null;
                boolean captain = LadyWhiteBoneEntity.captain(d);
                m.become(captain ? com.jttw.engine.entity.ridge.RidgeMonster.CAPTAIN_LEVEL : com.jttw.engine.entity.ridge.RidgeMonster.LEVEL, captain);
                m.setSilent(true);
                if (standIns.size() > 32) standIns.clear();
                standIns.put(key, m);
            }
            return m;
        }

        /** The stand-in where she is, turned and moving as she is. */
        private static void pose(net.minecraft.world.entity.LivingEntity m, LadyWhiteBoneEntity e) {
            m.setPos(e.getX(), e.getY(), e.getZ());
            m.xo = e.xo; m.yo = e.yo; m.zo = e.zo;
            m.setYRot(e.getYRot()); m.yRotO = e.yRotO;
            m.setXRot(e.getXRot()); m.xRotO = e.xRotO;
            m.yBodyRot = e.yBodyRot; m.yBodyRotO = e.yBodyRotO;
            m.yHeadRot = e.yHeadRot; m.yHeadRotO = e.yHeadRotO;
            m.tickCount = e.tickCount;
            m.hurtTime = e.hurtTime;
            m.attackAnim = e.attackAnim; m.oAttackAnim = e.oAttackAnim;
            m.swinging = e.swinging;
            m.setOnGround(e.onGround());
            if (m instanceof net.minecraft.world.entity.Mob mob) mob.setAggressive(e.isAggressive());
            com.jttw.engine.transformation.MorphPose.copyWalkAnim(e.walkAnimation, m.walkAnimation);
        }

        @Override
        protected RenderType getRenderType(LadyWhiteBoneEntity e, boolean visible, boolean translucent, boolean glowing) {
            return mirage < 1f ? RenderType.entityTranslucent(LADY) : super.getRenderType(e, visible, translucent, glowing);
        }

        @Override
        protected boolean shouldShowName(LadyWhiteBoneEntity e) {
            return mirage >= 1f && !e.disguised() && super.shouldShowName(e);
        }
    }

    // ── The bodies she leaves ───────────────────────────────────────────

    public static final ResourceLocation[] HUSKS = {
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/lady_husk_girl.png"),
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/lady_husk_old_woman.png"),
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/lady_husk_old_man.png")};

    /** A mortal's body, lying on its back where she fell. */
    public static final class HuskRenderer extends HumanoidMobRenderer<com.jttw.engine.entity.ridge.LadyHuskEntity,
            HumanoidModel<com.jttw.engine.entity.ridge.LadyHuskEntity>> {
        public HuskRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)) {
                @Override
                public void setupAnim(com.jttw.engine.entity.ridge.LadyHuskEntity e, float swing, float swingAmount, float age, float yaw, float pitch) {
                    super.setupAnim(e, 0, 0, 0, 0, 0); // still
                    rightArm.zRot = 0.15f;
                    leftArm.zRot = -0.15f;
                }
            }, 0.3f);
        }

        @Override
        protected void setupRotations(com.jttw.engine.entity.ridge.LadyHuskEntity e, PoseStack pose, float age, float yaw, float partial, float scale) {
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f - yaw));
            // laid out on its back, its feet where it stood
            pose.translate(0, 0.25f, 0);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
            pose.translate(0, -0.9f, 0);
        }

        @Override
        public ResourceLocation getTextureLocation(com.jttw.engine.entity.ridge.LadyHuskEntity e) {
            return HUSKS[Math.max(0, Math.min(HUSKS.length - 1, e.mortal()))];
        }
    }
}
