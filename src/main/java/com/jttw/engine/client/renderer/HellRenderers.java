package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.hell.HorseFaceEntity;
import com.jttw.engine.entity.hell.ImpermanenceEntity;
import com.jttw.engine.entity.hell.JudgeClerkEntity;
import com.jttw.engine.entity.hell.KingOfHellEntity;
import com.jttw.engine.entity.hell.OxHeadEntity;
import com.jttw.engine.entity.hell.WanderingGhostEntity;
import com.jttw.engine.entity.hell.YinSoldierEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * How Hell's creatures look: Ox-Head and Horse-Face (a big armoured frame, a beast's head, a steel
 * fork or a hooked spear), the Judge-Clerk (a red robe, a winged black cap, brush and ledger), the
 * Black and White Impermanence (long robes, tall hats; the White's long red tongue), the wandering
 * ghosts (pale, legless, trailing away, half there), and the Ten Kings (a judge's robe and wide
 * sleeves, a jade belt, the flat crown with its strings of beads, the ivory tablet held before
 * him; one frame, each king his own colours), and the Yin soldiers (dark lamellar armour, a
 * crested helmet, a halberd, a grey face and burning eyes). Skins painted by
 * {@code assets_source/hell_guards.py}, {@code hell_officials.py}, {@code hell_kings.py} and
 * {@code yin_soldier.py}.
 */
public final class HellRenderers {

    private HellRenderers() {}

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name), "main");
    }

    public static final ModelLayerLocation OX_LAYER = layer("ox_head"), HORSE_LAYER = layer("horse_face"),
            CLERK_LAYER = layer("judge_clerk"), IMPERMANENCE_LAYER = layer("impermanence"), GHOST_LAYER = layer("wandering_ghost"),
            KING_LAYER = layer("king_of_hell"), SOLDIER_LAYER = layer("yin_soldier");

    /** A box of steel, for the weapons' small parts (a swatch of the skin at 112, 40). */
    private static CubeListBuilder steel() {
        return CubeListBuilder.create().texOffs(112, 40);
    }

    // ── Ox-Head and Horse-Face ──────────────────────────────────────────

    /** The guards' common frame (128x64): a person's, in a black cuirass and armoured skirt, pauldrons on the shoulders. */
    private static MeshDefinition guardMesh() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("cuirass", CubeListBuilder.create().texOffs(64, 0).addBox(-4.5f, -0.5f, -2.5f, 9, 8, 5, new CubeDeformation(0.25f)), PartPose.ZERO);
        body.addOrReplaceChild("tassets", CubeListBuilder.create().texOffs(64, 13).addBox(-4.5f, 9, -2.5f, 9, 6, 5, new CubeDeformation(0.35f)), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(92, 0).addBox(-3.5f, -2.5f, -2.5f, 5, 4, 5, new CubeDeformation(0.2f)), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(92, 0).mirror().addBox(-1.5f, -2.5f, -2.5f, 5, 4, 5, new CubeDeformation(0.2f)), PartPose.ZERO);
        return mesh;
    }

    /** Ox-Head: a broad ox's head, a heavy muzzle, horns out to the sides and up; a steel fork, upright in his hand. */
    public static LayerDefinition oxLayer() {
        MeshDefinition mesh = guardMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(64, 24).addBox(-3, -4.5f, -6.5f, 6, 4, 3), PartPose.ZERO);
        for (int side : new int[]{-1, 1}) {
            String n = side < 0 ? "right" : "left";
            PartDefinition horn = head.addOrReplaceChild(n + "_horn", CubeListBuilder.create().texOffs(84, 24).mirror(side > 0)
                    .addBox(side < 0 ? -4 : 0, -1, -1, 4, 2, 2), PartPose.offset(side * 4, -6.5f, -1));
            horn.addOrReplaceChild(n + "_tip", CubeListBuilder.create().texOffs(84, 28).mirror(side > 0).addBox(-0.75f, -4, -0.75f, 1.5f, 4, 1.5f),
                    PartPose.offsetAndRotation(side * 3.5f, -0.5f, 0, -0.2f, 0, side * 0.25f));
            head.addOrReplaceChild(n + "_ear", CubeListBuilder.create().texOffs(96, 24).mirror(side > 0)
                    .addBox(side < 0 ? -3 : 0, -0.5f, -0.5f, 3, 2, 1), PartPose.offsetAndRotation(side * 4, -4.5f, 0.5f, 0, 0, side * 0.3f));
        }
        PartDefinition fork = root.getChild("right_arm").addOrReplaceChild("fork", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.5f, -20, -0.5f, 1, 30, 1), PartPose.offset(-1, 10, 0));
        fork.addOrReplaceChild("crossbar", steel().addBox(-2.5f, -21, -0.5f, 5, 1, 1), PartPose.ZERO);
        fork.addOrReplaceChild("prong_l", steel().addBox(-2.5f, -25, -0.5f, 1, 4, 1), PartPose.ZERO);
        fork.addOrReplaceChild("prong_m", steel().addBox(-0.5f, -26, -0.5f, 1, 5, 1), PartPose.ZERO);
        fork.addOrReplaceChild("prong_r", steel().addBox(1.5f, -25, -0.5f, 1, 4, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Horse-Face: a long horse's face, a dark mane down the back of his head, ears up; a spear with a hook below its head. */
    public static LayerDefinition horseLayer() {
        MeshDefinition mesh = guardMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(64, 24).addBox(-2.5f, -6, -10, 5, 6, 6), PartPose.ZERO);
        head.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(64, 40).addBox(-1, -9, -3, 2, 3, 8), PartPose.ZERO);
        for (int side : new int[]{-1, 1}) {
            head.addOrReplaceChild((side < 0 ? "right" : "left") + "_ear", CubeListBuilder.create().texOffs(88, 36)
                    .addBox(-0.5f, -3, -0.5f, 1.5f, 3, 1), PartPose.offsetAndRotation(side * 2.5f, -8, 0, 0, 0, side * 0.2f));
        }
        PartDefinition spear = root.getChild("right_arm").addOrReplaceChild("spear", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.5f, -20, -0.5f, 1, 30, 1), PartPose.offset(-1, 10, 0));
        spear.addOrReplaceChild("head", steel().addBox(-1, -26, -0.5f, 2, 6, 1), PartPose.ZERO);
        spear.addOrReplaceChild("hook", steel().addBox(1, -21, -0.5f, 2, 1, 1), PartPose.ZERO);
        spear.addOrReplaceChild("hook_tip", steel().addBox(2, -23, -0.5f, 1, 2, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** A guard's frame: his weapon held upright; Ox-Head's horns lowered as he charges. */
    public static final class GuardModel<T extends Mob> extends HumanoidModel<T> {
        public GuardModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing, amount, age, yaw, pitch);
            // the weapon arm held a little forward, steady
            if (attackTime <= 0) rightArm.xRot = rightArm.xRot * 0.3f - 0.35f;
            if (e instanceof OxHeadEntity ox && ox.isCharging()) {
                head.xRot = 0.7f;
                body.xRot = 0.3f;
                rightArm.xRot = -0.2f;
                leftArm.xRot = -0.2f;
            }
        }
    }

    public static final class OxRenderer extends HumanoidMobRenderer<OxHeadEntity, GuardModel<OxHeadEntity>> {
        public static final float SIZE = 1.33f;
        private static final ResourceLocation SKIN = tex("ox_head"), CAPTAIN = tex("ox_head_captain");
        private static final RenderType EYES = RenderType.eyes(tex("ox_head_eyes"));

        public OxRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new GuardModel<>(ctx.bakeLayer(OX_LAYER)), 0.6f);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return EYES;
                }
            });
        }

        @Override
        protected void scale(OxHeadEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(OxHeadEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected boolean shouldShowName(OxHeadEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    public static final class HorseRenderer extends HumanoidMobRenderer<HorseFaceEntity, GuardModel<HorseFaceEntity>> {
        public static final float SIZE = 1.28f;
        private static final ResourceLocation SKIN = tex("horse_face"), CAPTAIN = tex("horse_face_captain");
        private static final RenderType EYES = RenderType.eyes(tex("horse_face_eyes"));

        public HorseRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new GuardModel<>(ctx.bakeLayer(HORSE_LAYER)), 0.6f);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return EYES;
                }
            });
        }

        @Override
        protected void scale(HorseFaceEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(HorseFaceEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected boolean shouldShowName(HorseFaceEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── The Judge-Clerk ─────────────────────────────────────────────────

    /** A person's frame (128x64) in a robe to the ground; a black official's cap, its two wings out to the sides; a brush in his right hand, his ledger in his left. */
    public static LayerDefinition clerkLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        root.getChild("body").addOrReplaceChild("robe", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-5, 10, -3.5f, 10, 14, 7, new CubeDeformation(0.1f)), PartPose.ZERO);
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(64, 22).addBox(-4.5f, -10.5f, -3, 9, 3, 7), PartPose.ZERO);
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(96, 22).addBox(-3.5f, -12.5f, 0, 7, 2, 4), PartPose.ZERO);
        head.addOrReplaceChild("wings", CubeListBuilder.create().texOffs(64, 34).addBox(-11, -11, 3, 22, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(118, 22).addBox(-1.5f, -1, -4.3f, 3, 4, 1), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("brush", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.5f, 0, -0.5f, 1, 9, 1), PartPose.offsetAndRotation(-1, 10, -1, -1.4f, 0, 0));
        root.getChild("left_arm").addOrReplaceChild("ledger", CubeListBuilder.create().texOffs(64, 40)
                .addBox(-0.5f, 0, -4, 2, 6, 5), PartPose.offset(1, 7, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static final class ClerkModel extends HumanoidModel<JudgeClerkEntity> {
        public ClerkModel(ModelPart root) {
            super(root);
            leftLeg.visible = false;
            rightLeg.visible = false;
        }

        @Override
        public void setupAnim(JudgeClerkEntity e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing * 0.4f, amount, age, yaw, pitch);
            // the ledger held open before him; writing, the brush raised over it
            leftArm.xRot = -0.9f;
            leftArm.yRot = 0.3f;
            if (e.isWriting()) {
                rightArm.xRot = -1.3f + Mth.sin(age * 0.8f) * 0.15f;
                rightArm.yRot = -0.4f;
            } else if (attackTime <= 0) {
                rightArm.xRot = -0.4f + Mth.cos(age * 0.06f) * 0.05f;
            }
        }
    }

    public static final class ClerkRenderer extends HumanoidMobRenderer<JudgeClerkEntity, ClerkModel> {
        private static final ResourceLocation SKIN = tex("judge_clerk"), CAPTAIN = tex("judge_clerk_captain");

        public ClerkRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ClerkModel(ctx.bakeLayer(CLERK_LAYER)), 0.4f);
        }

        @Override
        public ResourceLocation getTextureLocation(JudgeClerkEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected boolean shouldShowName(JudgeClerkEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── The Black and White Impermanence ───────────────────────────────

    /**
     * A person's frame (128x64) in a long robe to the ground, a tall hat rising in three tiers, the
     * White's long red tongue hanging to his chest, a mourning staff in the right hand.
     */
    public static LayerDefinition impermanenceLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        root.getChild("body").addOrReplaceChild("robe", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-5, 10, -3.5f, 10, 14, 7, new CubeDeformation(0.1f)), PartPose.ZERO);
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("hat_1", CubeListBuilder.create().texOffs(64, 22).addBox(-4.5f, -11, -4.5f, 9, 4, 9), PartPose.ZERO);
        head.addOrReplaceChild("hat_2", CubeListBuilder.create().texOffs(64, 36).addBox(-3.5f, -15, -3.5f, 7, 4, 7), PartPose.ZERO);
        head.addOrReplaceChild("hat_3", CubeListBuilder.create().texOffs(92, 36).addBox(-2.5f, -19, -2.5f, 5, 4, 5), PartPose.ZERO);
        head.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(112, 0).addBox(-1, -2.5f, -4.4f, 2, 8, 1), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("staff", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.5f, -12, -0.5f, 1, 24, 1), PartPose.offset(-1, 10, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static final class ImpermanenceModel extends HumanoidModel<ImpermanenceEntity> {
        private final ModelPart tongue;

        public ImpermanenceModel(ModelPart root) {
            super(root);
            leftLeg.visible = false;
            rightLeg.visible = false;
            tongue = head.getChild("tongue");
        }

        @Override
        public void setupAnim(ImpermanenceEntity e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing * 0.4f, amount, age, yaw, pitch);
            tongue.visible = e.white();
            tongue.xRot = -0.05f + Mth.sin(age * 0.12f) * 0.05f;
            // the left hand raised, beckoning; the staff held before him
            leftArm.xRot = -0.6f + Mth.sin(age * 0.09f) * 0.12f;
            if (attackTime <= 0) rightArm.xRot = -0.3f;
            if (e.enraged()) {
                leftArm.xRot = -2.2f;
                rightArm.xRot = -1.6f;
            }
        }
    }

    public static final class ImpermanenceRenderer extends HumanoidMobRenderer<ImpermanenceEntity, ImpermanenceModel> {
        private static final ResourceLocation BLACK = tex("black_impermanence"), WHITE = tex("white_impermanence");

        public ImpermanenceRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ImpermanenceModel(ctx.bakeLayer(IMPERMANENCE_LAYER)), 0.4f);
        }

        @Override
        protected void scale(ImpermanenceEntity e, PoseStack pose, float partial) {
            float s = e.white() ? 1.12f : 0.92f;
            pose.scale(s, s, s);
        }

        @Override
        public ResourceLocation getTextureLocation(ImpermanenceEntity e) {
            return e.white() ? WHITE : BLACK;
        }

        @Override
        protected boolean shouldShowName(ImpermanenceEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── The wandering ghost ─────────────────────────────────────────────

    /** A person's frame (64x64), no legs: the body trails away below the waist in three lessening wisps; long loose hair down the back. */
    public static LayerDefinition ghostLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("trail_1", CubeListBuilder.create().texOffs(0, 32).addBox(-4, 12, -2, 8, 5, 4), PartPose.ZERO);
        body.addOrReplaceChild("trail_2", CubeListBuilder.create().texOffs(24, 32).addBox(-3, 17, -1.5f, 6, 4, 3), PartPose.ZERO);
        body.addOrReplaceChild("trail_3", CubeListBuilder.create().texOffs(44, 32).addBox(-1.5f, 21, -1, 3, 3, 2), PartPose.ZERO);
        root.getChild("head").addOrReplaceChild("hair", CubeListBuilder.create().texOffs(0, 44).addBox(-4, -8, 4, 8, 13, 1, new CubeDeformation(0.3f)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static final class GhostModel extends HumanoidModel<WanderingGhostEntity> {
        float alpha = 0.72f;

        public GhostModel(ModelPart root) {
            super(root, RenderType::entityTranslucent);
            leftLeg.visible = false;
            rightLeg.visible = false;
        }

        @Override
        public void setupAnim(WanderingGhostEntity e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, 0, 0, age, yaw, pitch);
            // arms out before it, groping; the whole of it drifting
            float drift = Mth.sin(age * 0.1f);
            rightArm.xRot = -1.35f + drift * 0.08f;
            leftArm.xRot = -1.35f - drift * 0.08f;
            rightArm.zRot = 0.1f;
            leftArm.zRot = -0.1f;
            body.xRot = 0.08f;
            float bob = drift * 1.2f - 1.5f;
            head.y = bob;
            hat.y = bob;
            body.y = bob;
            rightArm.y = 2 + bob;
            leftArm.y = 2 + bob;
            if (attackTime > 0) {
                rightArm.xRot -= Mth.sin(attackTime * Mth.PI) * 0.6f;
                leftArm.xRot -= Mth.sin(attackTime * Mth.PI) * 0.6f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            super.renderToBuffer(pose, vc, light, overlay, FastColor.ARGB32.color(Math.round(FastColor.ARGB32.alpha(color) * alpha),
                    FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color)));
        }
    }

    public static final class GhostRenderer extends HumanoidMobRenderer<WanderingGhostEntity, GhostModel> {
        private static final ResourceLocation SKIN = tex("wandering_ghost"), CAPTAIN = tex("wandering_ghost_captain");

        public GhostRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new GhostModel(ctx.bakeLayer(GHOST_LAYER)), 0.3f);
        }

        @Override
        public ResourceLocation getTextureLocation(WanderingGhostEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected RenderType getRenderType(WanderingGhostEntity e, boolean visible, boolean translucent, boolean glowing) {
            return RenderType.entityTranslucent(getTextureLocation(e));
        }

        @Override
        protected boolean shouldShowName(WanderingGhostEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── The Yin soldiers ────────────────────────────────────────────────

    /**
     * A soldier's frame (128x64), a person's size: the guards' cuirass, armoured skirt and pauldrons;
     * a helmet with a neck guard and a red crest along its top; a halberd (a ji: a spike, a crescent
     * blade to one side, a red tassel under them) upright in the right hand.
     */
    public static LayerDefinition soldierLayer() {
        MeshDefinition mesh = guardMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(64, 24).addBox(-4.5f, -9, -4.5f, 9, 5, 9, new CubeDeformation(0.15f)), PartPose.ZERO);
        head.addOrReplaceChild("neck_guard", CubeListBuilder.create().texOffs(64, 40).addBox(-4.5f, -4, 2.5f, 9, 3, 2, new CubeDeformation(0.2f)), PartPose.ZERO);
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(100, 24).addBox(-0.5f, -12.5f, -3, 1, 4, 6), PartPose.ZERO);
        PartDefinition ji = root.getChild("right_arm").addOrReplaceChild("halberd", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.5f, -20, -0.5f, 1, 30, 1), PartPose.offset(-1, 10, 0));
        ji.addOrReplaceChild("spike", steel().addBox(-0.5f, -26, -0.5f, 1, 6, 1), PartPose.ZERO);
        ji.addOrReplaceChild("blade", steel().addBox(0.5f, -23, -0.5f, 3, 2, 1), PartPose.ZERO);
        ji.addOrReplaceChild("blade_tip", steel().addBox(2.5f, -25, -0.5f, 1, 2, 1), PartPose.ZERO);
        ji.addOrReplaceChild("tassel", CubeListBuilder.create().texOffs(100, 40).addBox(-1, -20, -1, 2, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static final class SoldierRenderer extends HumanoidMobRenderer<YinSoldierEntity, GuardModel<YinSoldierEntity>> {
        private static final ResourceLocation SKIN = tex("yin_soldier"), CAPTAIN = tex("yin_soldier_captain");
        private static final RenderType EYES = RenderType.eyes(tex("yin_soldier_eyes"));

        public SoldierRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new GuardModel<>(ctx.bakeLayer(SOLDIER_LAYER)), 0.45f);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return EYES;
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(YinSoldierEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected boolean shouldShowName(YinSoldierEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── The Ten Kings ───────────────────────────────────────────────────

    /**
     * A judge-king's frame (128x64): a robe to the ground, wide sleeves, a jade belt; on his head the
     * mian crown, a flat board with strings of beads hanging before and behind; a beard; the ivory
     * tablet held up before him in both hands.
     */
    public static LayerDefinition kingLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("robe", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-5, 10, -3.5f, 10, 14, 7, new CubeDeformation(0.2f)), PartPose.ZERO);
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(84, 22)
                .addBox(-4.5f, 7, -2.5f, 9, 2, 5, new CubeDeformation(0.15f)), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("sleeve", CubeListBuilder.create().texOffs(64, 22)
                .addBox(-3.5f, 4, -2.5f, 5, 8, 5, new CubeDeformation(0.25f)), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("sleeve", CubeListBuilder.create().texOffs(64, 22).mirror()
                .addBox(-1.5f, 4, -2.5f, 5, 8, 5, new CubeDeformation(0.25f)), PartPose.ZERO);
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("crown_cap", CubeListBuilder.create().texOffs(98, 0).addBox(-3.5f, -10, -3.5f, 7, 2, 7), PartPose.ZERO);
        head.addOrReplaceChild("crown_board", CubeListBuilder.create().texOffs(64, 36).addBox(-5, -11, -6, 10, 1, 12), PartPose.ZERO);
        for (int k = 0; k < 5; k++) {
            float x = -4 + k * 2;
            head.addOrReplaceChild("beads_front_" + k, CubeListBuilder.create().texOffs(110, 50).addBox(x - 0.5f, -10, -6, 1, 4, 1), PartPose.ZERO);
            head.addOrReplaceChild("beads_back_" + k, CubeListBuilder.create().texOffs(110, 50).addBox(x - 0.5f, -10, 5, 1, 4, 1), PartPose.ZERO);
        }
        head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(96, 50).addBox(-2, -1, -4.4f, 4, 6, 1), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("tablet", CubeListBuilder.create().texOffs(116, 50)
                .addBox(-1, -10, -0.5f, 2, 11, 1), PartPose.offsetAndRotation(1.5f, 10, -1, 1.0f, 0.4f, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static final class KingModel<T extends KingOfHellEntity> extends HumanoidModel<T> {
        public KingModel(ModelPart root) {
            super(root);
            leftLeg.visible = false;
            rightLeg.visible = false;
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing * 0.4f, amount, age, yaw, pitch);
            if (e.isJudging()) {
                // the tablet raised high in judgement
                rightArm.xRot = -2.6f;
                rightArm.yRot = -0.2f;
                leftArm.xRot = -1.9f;
                leftArm.zRot = -0.5f;
            } else if (attackTime <= 0) {
                // both hands before him, holding up the tablet
                rightArm.xRot = -1.0f + Mth.cos(age * 0.05f) * 0.03f;
                rightArm.yRot = -0.35f;
                leftArm.xRot = -1.0f + Mth.cos(age * 0.05f) * 0.03f;
                leftArm.yRot = 0.35f;
            }
        }
    }

    public static final class KingRenderer<T extends KingOfHellEntity> extends HumanoidMobRenderer<T, KingModel<T>> {
        public static final float SIZE = 1.2f, YAMA_SIZE = 1.33f;
        private final ResourceLocation skin;
        private final boolean yama;

        public KingRenderer(EntityRendererProvider.Context ctx, String name) {
            super(ctx, new KingModel<>(ctx.bakeLayer(KING_LAYER)), 0.5f);
            skin = tex(name);
            yama = name.equals("king_yama");
            if (yama) {
                RenderType eyes = RenderType.eyes(tex("king_yama_eyes"));
                addLayer(new EyesLayer<>(this) {
                    @Override
                    public RenderType renderType() {
                        return eyes;
                    }
                });
            }
        }

        /** A renderer for the king of this name (its skin is textures/entity/{name}.png). */
        public static <T extends KingOfHellEntity> EntityRendererProvider<T> of(String name) {
            return ctx -> new KingRenderer<>(ctx, name);
        }

        @Override
        protected void scale(T e, PoseStack pose, float partial) {
            float s = yama ? YAMA_SIZE : SIZE;
            pose.scale(s, s, s);
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return skin;
        }
    }

    /** For the gallery: whether a zone creature is drawn as a captain. */
    public static boolean captain(Mob m) {
        return m instanceof ZoneMonster z && z.isCaptain();
    }
}
