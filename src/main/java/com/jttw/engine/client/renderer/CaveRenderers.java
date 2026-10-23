package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.cave.EarthFlowDecoyEntity;
import com.jttw.engine.entity.cave.FlowerMaidEntity;
import com.jttw.engine.entity.cave.LadyEarthFlowEntity;
import com.jttw.engine.entity.cave.MouseSpiritEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * How the Bottomless Cave's household looks: the Mouse Spirit (a box model of its own), the Flower
 * Maid and Lady Earth Flow (a person's frame with a gown of petals, a robe and fur, ears, a tail),
 * and the likeness she leaves behind. Skins painted by {@code assets_source/mouse_spirit.py},
 * {@code flower_maid.py} and {@code lady_earth_flow.py}.
 */
public final class CaveRenderers {

    private CaveRenderers() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name), "main");
    }

    public static final ModelLayerLocation MOUSE_LAYER = layer("mouse_spirit"), MAID_LAYER = layer("flower_maid"),
            LADY_LAYER = layer("lady_earth_flow");
    public static final ResourceLocation MOUSE = tex("mouse_spirit"), MOUSE_CAPTAIN = tex("mouse_spirit_captain"),
            MAID = tex("flower_maid"), MAID_CAPTAIN = tex("flower_maid_captain"), LADY = tex("lady_earth_flow");

    // ── Mouse Spirit ────────────────────────────────────────────────────

    /**
     * An upright rat-demon about waist high: a big head with a long snout, round ears and beady eyes,
     * a hunched body in a ragged vest, short arms held up before it, bent legs with long feet, and a
     * long bare tail in two parts. Model pixels, ground at y 24; its skin is 64x64.
     */
    public static LayerDefinition mouseLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-3, -8, -2, 6, 8, 4), PartPose.offsetAndRotation(0, 18, 0, 0.25f, 0, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3, -5, -3, 6, 5, 5)
                .texOffs(22, 0).addBox(-1.5f, -3, -6, 3, 3, 3), PartPose.offset(0, 11, -2));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(34, 0).addBox(-1.5f, -3, -0.5f, 3, 3, 1),
                PartPose.offsetAndRotation(-2.5f, -4.5f, 0, 0, 0.2f, -0.35f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(34, 0).mirror().addBox(-1.5f, -3, -0.5f, 3, 3, 1),
                PartPose.offsetAndRotation(2.5f, -4.5f, 0, 0, -0.2f, 0.35f));
        for (int side : new int[]{-1, 1}) {
            String n = side < 0 ? "right" : "left";
            root.addOrReplaceChild(n + "_arm", CubeListBuilder.create().texOffs(24, 12).mirror(side > 0).addBox(-1, 0, -1, 2, 6, 2),
                    PartPose.offsetAndRotation(side * 3.8f, 11.5f, -1, -0.6f, 0, 0));
            root.addOrReplaceChild(n + "_leg", CubeListBuilder.create()
                            .texOffs(32, 12).mirror(side > 0).addBox(-1, 0, -1, 2, 6, 2)
                            .texOffs(40, 12).mirror(side > 0).addBox(-1, 5, -3, 2, 1, 3),
                    PartPose.offset(side * 1.6f, 18, 0.5f));
        }
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 26).addBox(-0.5f, -0.5f, 0, 1, 1, 8),
                PartPose.offsetAndRotation(0, 17, 1.5f, -0.5f, 0, 0));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(20, 26).addBox(-0.5f, -0.5f, 0, 1, 1, 7),
                PartPose.offsetAndRotation(0, 0, 8, 0.7f, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static final class MouseModel extends EntityModel<MouseSpiritEntity> {
        private final ModelPart root, head, body, rightArm, leftArm, rightLeg, leftLeg, tail;

        public MouseModel(ModelPart root) {
            this.root = root;
            head = root.getChild("head");
            body = root.getChild("body");
            rightArm = root.getChild("right_arm");
            leftArm = root.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            tail = root.getChild("tail");
        }

        @Override
        public void setupAnim(MouseSpiritEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            float run = Mth.cos(swing * 1.1f) * 1.3f * amount;
            rightLeg.xRot = run;
            leftLeg.xRot = -run;
            // claws up before it, raking as it bites
            float rake = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) * 1.2f : 0;
            rightArm.xRot = -0.6f - rake - run * 0.3f;
            leftArm.xRot = -0.6f - rake + run * 0.3f;
            body.xRot = 0.25f + amount * 0.25f;
            tail.yRot = Mth.sin(age * 0.15f) * 0.35f;
            tail.xRot = -0.5f + Mth.sin(age * 0.1f) * 0.1f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class MouseRenderer extends MobRenderer<MouseSpiritEntity, MouseModel> {
        public MouseRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new MouseModel(ctx.bakeLayer(MOUSE_LAYER)), 0.35f);
        }

        @Override
        public ResourceLocation getTextureLocation(MouseSpiritEntity e) {
            return e.isCaptain() ? MOUSE_CAPTAIN : MOUSE;
        }

        @Override
        protected boolean shouldShowName(MouseSpiritEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── Flower Maid ─────────────────────────────────────────────────────

    /**
     * A person's frame (64x64 skin) in a gown whose skirt falls to the ground in layers of petals
     * (her legs hidden under it), her hair up in a bun with a flower in it.
     */
    public static LayerDefinition maidLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        root.getChild("body").addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-5, 10, -3.5f, 10, 14, 7, new CubeDeformation(0.1f)), PartPose.ZERO);
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("bun", CubeListBuilder.create().texOffs(36, 32).addBox(-2, -10.5f, 0, 4, 3, 4), PartPose.ZERO);
        head.addOrReplaceChild("flower", CubeListBuilder.create().texOffs(36, 40).addBox(2.5f, -10, -1.5f, 3, 3, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static final class MaidModel extends HumanoidModel<FlowerMaidEntity> {
        public MaidModel(ModelPart root) {
            super(root);
            leftLeg.visible = false;
            rightLeg.visible = false;
        }

        @Override
        public void setupAnim(FlowerMaidEntity e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing * 0.4f, amount, age, yaw, pitch);
            if (e.isCasting()) {
                // arms raised, palms up, as she breathes out her pollen or tends
                rightArm.xRot = -2.4f;
                leftArm.xRot = -2.4f;
                rightArm.zRot = 0.4f;
                leftArm.zRot = -0.4f;
            } else {
                rightArm.zRot = Mth.cos(age * 0.07f) * 0.04f + 0.08f;
                leftArm.zRot = -Mth.cos(age * 0.07f) * 0.04f - 0.08f;
            }
        }
    }

    public static final class MaidRenderer extends HumanoidMobRenderer<FlowerMaidEntity, MaidModel> {
        public MaidRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new MaidModel(ctx.bakeLayer(MAID_LAYER)), 0.4f);
        }

        @Override
        protected void scale(FlowerMaidEntity e, PoseStack pose, float partial) {
            pose.scale(0.94f, 0.94f, 0.94f);
        }

        @Override
        public ResourceLocation getTextureLocation(FlowerMaidEntity e) {
            return e.isCaptain() ? MAID_CAPTAIN : MAID;
        }

        @Override
        protected boolean shouldShowName(FlowerMaidEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── Lady Earth Flow, and her likeness ───────────────────────────────

    /**
     * A tall woman's frame (128x64 skin): a robe whose skirt falls to the ground (her legs under
     * it), a collar of white fur, long white hair down her back, round white mouse's ears in it, a
     * small nose of gold, and a thin pink tail from under her hem.
     */
    public static LayerDefinition ladyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-5, 10, -3.5f, 10, 14, 7, new CubeDeformation(0.15f)), PartPose.ZERO);
        body.addOrReplaceChild("collar", CubeListBuilder.create()
                .texOffs(64, 24).addBox(-5, -0.5f, -3, 10, 2, 6), PartPose.ZERO);
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(64, 34).addBox(-0.5f, -0.5f, 0, 1, 1, 10), PartPose.offsetAndRotation(0, 21, 3.5f, -0.12f, 0, 0));
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("hair", CubeListBuilder.create()
                .texOffs(100, 0).addBox(-4, -8, 4.1f, 8, 13, 1, new CubeDeformation(0.4f)), PartPose.ZERO);
        head.addOrReplaceChild("bun", CubeListBuilder.create()
                .texOffs(98, 34).addBox(-2, -10.5f, -0.5f, 4, 3, 4), PartPose.ZERO);
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(100, 16).addBox(-1.5f, -3, -0.5f, 3, 3, 1),
                PartPose.offsetAndRotation(-3.6f, -7.2f, 0.5f, 0, 0.25f, -0.4f));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(100, 16).mirror().addBox(-1.5f, -3, -0.5f, 3, 3, 1),
                PartPose.offsetAndRotation(3.6f, -7.2f, 0.5f, 0, -0.25f, 0.4f));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(110, 16).addBox(-0.5f, -3.5f, -4.6f, 1, 1, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Her frame, for herself and for the likeness she leaves: swords raised before her as she fights. */
    public static final class LadyModel<T extends Mob> extends HumanoidModel<T> {
        private final ModelPart tail;
        /** How solid it is drawn (her likeness, a shade less). */
        float alpha = 1f;

        public LadyModel(ModelPart root) {
            super(root);
            leftLeg.visible = false;
            rightLeg.visible = false;
            tail = body.getChild("tail");
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing * 0.5f, amount, age, yaw, pitch);
            tail.yRot = Mth.sin(age * 0.09f) * 0.3f;
            if (e.isAggressive()) {
                // both blades up and forward, the right a little higher
                rightArm.xRot = -1.35f + Mth.sin(age * 0.25f) * 0.08f + rightArm.xRot * 0.3f;
                leftArm.xRot = -1.0f - Mth.sin(age * 0.25f) * 0.08f + leftArm.xRot * 0.3f;
                rightArm.yRot = -0.25f;
                leftArm.yRot = 0.35f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            super.renderToBuffer(pose, vc, light, overlay, alpha >= 1f ? color
                    : FastColor.ARGB32.color(Math.round(FastColor.ARGB32.alpha(color) * alpha), FastColor.ARGB32.red(color),
                    FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color)));
        }
    }

    public static final class LadyRenderer extends HumanoidMobRenderer<LadyEarthFlowEntity, LadyModel<LadyEarthFlowEntity>> {
        public static final float SIZE = 1.05f;

        public LadyRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new LadyModel<>(ctx.bakeLayer(LADY_LAYER)), 0.45f);
        }

        @Override
        protected void scale(LadyEarthFlowEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(LadyEarthFlowEntity e) {
            return LADY;
        }
    }

    /** Her likeness: the same, a shade less solid, as a thing not quite there. */
    public static final class DecoyRenderer extends HumanoidMobRenderer<EarthFlowDecoyEntity, LadyModel<EarthFlowDecoyEntity>> {
        public static final float SOLID = 0.82f;

        public DecoyRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new LadyModel<>(ctx.bakeLayer(LADY_LAYER)), 0.45f);
            model.alpha = SOLID;
        }

        @Override
        protected void scale(EarthFlowDecoyEntity e, PoseStack pose, float partial) {
            pose.scale(LadyRenderer.SIZE, LadyRenderer.SIZE, LadyRenderer.SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(EarthFlowDecoyEntity e) {
            return LADY;
        }

        @Override
        protected RenderType getRenderType(EarthFlowDecoyEntity e, boolean visible, boolean translucent, boolean glowing) {
            return RenderType.entityTranslucent(LADY);
        }
    }
}
