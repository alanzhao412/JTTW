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
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/** How White Bone Ridge's dead look: models and renderers for the Bone Servant, the Wraith and Lady White Bone. */
public final class RidgeRenderers {

    private RidgeRenderers() {}

    private static final ResourceLocation SKELETON = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/skeleton/skeleton.png");
    public static final ResourceLocation WRAITH = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/wraith.png");
    public static final ResourceLocation WRAITH_CAPTAIN = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/wraith_captain.png");

    public static final ModelLayerLocation WRAITH_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "wraith"), "main");
    public static final ModelLayerLocation VEIL_LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "lady_white_bone"), "veil");

    /** Names over heads show only this close, so a crowd of the dead is not a wall of labels. */
    private static final double NAME_RANGE = 24;

    public static LayerDefinition wraithLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0f), 64, 32);
    }

    public static final ResourceLocation LADY_VEIL = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/lady_white_bone_veil.png");

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

    /** A skeleton, tall and thin, arm raised to throw when she fights. */
    public static final class LadyModel extends HumanoidModel<LadyWhiteBoneEntity> {
        public LadyModel(ModelPart root) {
            super(root);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            // the mirage: pale, cold, and thin as breath
            if (mirage < 1f) super.renderToBuffer(pose, vc, light, overlay, FastColor.ARGB32.color(
                    Math.round(FastColor.ARGB32.alpha(color) * mirage), Math.round(FastColor.ARGB32.red(color) * 0.8f),
                    Math.round(FastColor.ARGB32.green(color) * 0.9f), FastColor.ARGB32.blue(color)));
            else super.renderToBuffer(pose, vc, light, overlay, color);
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
            return mirage < 1f ? RenderType.entityTranslucent(SKELETON) : super.getRenderType(e, visible, translucent, glowing);
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
            veil.renderToBuffer(pose, v, light, LivingEntityRenderer.getOverlayCoords(e, 0f), FastColor.ARGB32.colorFromFloat(0.85f * mirage, 1f, 1f, 1f));
        }
    }
}
