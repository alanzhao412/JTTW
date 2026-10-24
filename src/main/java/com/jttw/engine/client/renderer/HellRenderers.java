package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.hell.HorseFaceEntity;
import com.jttw.engine.entity.hell.ImpermanenceEntity;
import com.jttw.engine.entity.hell.JudgeClerkEntity;
import com.jttw.engine.entity.hell.KingOfHellEntity;
import com.jttw.engine.entity.hell.KingYamaEntity;
import com.jttw.engine.entity.hell.OxHeadEntity;
import com.jttw.engine.entity.hell.WanderingGhostEntity;
import com.jttw.engine.entity.hell.YinSoldierEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

/**
 * How Hell's creatures look: Ox-Head and Horse-Face (a big armoured frame, a beast's head, a steel
 * fork or a hooked spear), the Judge-Clerk (a red robe, a winged black cap, brush and ledger), the
 * Black and White Impermanence (long robes, tall hats; the White's long red tongue), the wandering
 * ghosts (pale, legless, trailing away, half there), and the Ten Kings (a judge's robe and wide
 * sleeves, a jade belt, the flat crown with its strings of beads, the ivory tablet held before
 * him; one frame, each king his own colours; King Yama, the Fifth, a towering frame of his own:
 * a judge-emperor of the dead in a crimson-and-black dragon robe, a ring of hellfire and skulls
 * behind his head), and the Yin soldiers (dark lamellar armour, a crested helmet, a halberd, a grey
 * face and burning eyes). Skins painted by {@code assets_source/hell_guards.py},
 * {@code hell_officials.py}, {@code hell_kings.py}, {@code king_yama.py} (his model too) and
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
            KING_LAYER = layer("king_of_hell"), YAMA_LAYER = layer("king_yama"), SOLDIER_LAYER = layer("yin_soldier");

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
        public static final float SIZE = 1.2f;
        private final ResourceLocation skin;

        public KingRenderer(EntityRendererProvider.Context ctx, String name) {
            super(ctx, new KingModel<>(ctx.bakeLayer(KING_LAYER)), 0.5f);
            skin = tex(name);
        }

        /** A renderer for the king of this name (its skin is textures/entity/{name}.png). */
        public static <T extends KingOfHellEntity> EntityRendererProvider<T> of(String name) {
            return ctx -> new KingRenderer<>(ctx, name);
        }

        @Override
        protected void scale(T e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return skin;
        }
    }

    // ── King Yama ───────────────────────────────────────────────────────

    /**
     * King Yama (256x256, a frame of his own): a towering judge-emperor in layered dragon robes of
     * crimson and black worked with gold dragons, flames and skulls, armoured shoulders bitten by gold
     * beasts' heads, a jade belt with a skull buckle and a soul-chain at his hip, great hanging
     * sleeves; a dark oxblood face, bulging burning eyes under brows knit into a V, tusks, a wild
     * black beard and moustache flaring out; the mian crown with its 王 plaque and its strings of
     * beads; the vermilion brush and the Book of Life and Death; a ring of hellfire set with skulls
     * behind his head. Model and skins: {@code assets_source/king_yama.py}.
     */
    public static LayerDefinition yamaLayer() {
        // GENERATED yama: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(63, 0).mirror(false).addBox(-7, -18, -5, 14, 18, 10)
                .texOffs(17, 87).mirror(false).addBox(-4.5f, -19.4f, -3.6f, 9, 2, 7.2f)
                .texOffs(52, 87).mirror(false).addBox(-2.6f, -22, -2.6f, 5.2f, 4, 5.2f)
                .texOffs(195, 56).mirror(false).addBox(-8.6f, -18.7f, -6, 17.2f, 1, 12)
                .texOffs(44, 99).mirror(false).addBox(-7.6f, -18.2f, -6.1f, 15.2f, 6, 0.5f)
                .texOffs(212, 87).mirror(false).addBox(-7.6f, -18.2f, 5.6f, 15.2f, 7, 0.5f)
                .texOffs(0, 56).mirror(false).addBox(-7.9f, -5, -6.1f, 15.8f, 2.5f, 12.2f)
                .texOffs(157, 99).mirror(false).addBox(-2.5f, -5.8f, -6.6f, 5, 4, 1), PartPose.offsetAndRotation(0, 1, 0, 0, 0, 0));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(76, 32).mirror(false).addBox(-7.6f, 0, -5.5f, 15.2f, 8, 11)
                .texOffs(13, 32).mirror(false).addBox(-8.8f, 7.5f, -6.5f, 17.6f, 8, 13)
                .texOffs(151, 0).mirror(false).addBox(-10.5f, 15, -8, 21, 8, 16), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition apron = skirt.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(226, 0).mirror(false).addBox(-4, 0, -1, 8, 23, 1), PartPose.offsetAndRotation(0, -1, -5.8f, -0.13f, 0, 0));
        PartDefinition rightPanel = skirt.addOrReplaceChild("right_panel", CubeListBuilder.create()
                .texOffs(0, 32).mirror(false).addBox(-2.5f, 0, -0.5f, 5, 22, 0.5f), PartPose.offsetAndRotation(-6.2f, -1, -5.1f, -0.1f, 0.35f, 0.05f));
        PartDefinition leftPanel = skirt.addOrReplaceChild("left_panel", CubeListBuilder.create()
                .texOffs(0, 32).mirror(true).addBox(-2.5f, 0, -0.5f, 5, 22, 0.5f), PartPose.offsetAndRotation(6.2f, -1, -5.1f, -0.1f, -0.35f, -0.05f));
        PartDefinition rightPendant = skirt.addOrReplaceChild("right_pendant", CubeListBuilder.create()
                .texOffs(114, 99).mirror(false).addBox(-0.4f, 0, -0.4f, 0.8f, 5, 0.8f)
                .texOffs(170, 99).mirror(false).addBox(-1.6f, 5, -0.7f, 3.2f, 3.2f, 1)
                .texOffs(181, 99).mirror(false).addBox(-0.6f, 8.2f, -0.5f, 1.2f, 4, 1), PartPose.offsetAndRotation(-5.3f, -1.6f, -6.4f, -0.14f, 0, 0.04f));
        PartDefinition leftPendant = skirt.addOrReplaceChild("left_pendant", CubeListBuilder.create()
                .texOffs(114, 99).mirror(true).addBox(-0.4f, 0, -0.4f, 0.8f, 5, 0.8f)
                .texOffs(170, 99).mirror(true).addBox(-1.6f, 5, -0.7f, 3.2f, 3.2f, 1)
                .texOffs(181, 99).mirror(true).addBox(-0.6f, 8.2f, -0.5f, 1.2f, 4, 1), PartPose.offsetAndRotation(5.3f, -1.6f, -6.4f, -0.14f, 0, -0.04f));
        PartDefinition chain = skirt.addOrReplaceChild("chain", CubeListBuilder.create()
                .texOffs(0, 108).mirror(false).addBox(-0.3f, 0, -0.7f, 0.6f, 1.8f, 1.4f)
                .texOffs(42, 108).mirror(false).addBox(-0.7f, 1.5f, -0.3f, 1.4f, 1.8f, 0.6f)
                .texOffs(0, 108).mirror(false).addBox(-0.3f, 3, -0.7f, 0.6f, 1.8f, 1.4f)
                .texOffs(42, 108).mirror(false).addBox(-0.7f, 4.5f, -0.3f, 1.4f, 1.8f, 0.6f)
                .texOffs(0, 108).mirror(false).addBox(-0.3f, 6, -0.7f, 0.6f, 1.8f, 1.4f)
                .texOffs(42, 108).mirror(false).addBox(-0.7f, 7.5f, -0.3f, 1.4f, 1.8f, 0.6f)
                .texOffs(0, 108).mirror(false).addBox(-0.3f, 9, -0.7f, 0.6f, 1.8f, 1.4f)
                .texOffs(42, 108).mirror(false).addBox(-0.7f, 10.5f, -0.3f, 1.4f, 1.8f, 0.6f)
                .texOffs(119, 99).mirror(false).addBox(-1.2f, 12.1f, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(7.4f, -2.6f, -2.5f, 0, 0, -0.08f));
        PartDefinition rightBoot = root.addOrReplaceChild("right_boot", CubeListBuilder.create()
                .texOffs(79, 99).mirror(false).addBox(-1.9f, 0, -2.6f, 3.8f, 1.6f, 4.6f)
                .texOffs(49, 108).mirror(false).addBox(-1.3f, -0.9f, -3.4f, 2.6f, 1.4f, 1), PartPose.offsetAndRotation(-3.6f, 22.4f, -6.4f, 0, 0.12f, 0));
        PartDefinition leftBoot = root.addOrReplaceChild("left_boot", CubeListBuilder.create()
                .texOffs(79, 99).mirror(true).addBox(-1.9f, 0, -2.6f, 3.8f, 1.6f, 4.6f)
                .texOffs(49, 108).mirror(true).addBox(-1.3f, -0.9f, -3.4f, 2.6f, 1.4f, 1), PartPose.offsetAndRotation(3.6f, 22.4f, -6.4f, 0, -0.12f, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(199, 32).mirror(false).addBox(-4, -2, -3.2f, 5.6f, 10, 6.4f), PartPose.offsetAndRotation(-9, -16, 0, -0.08f, 0, 0.2f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(199, 32).mirror(true).addBox(-1.6f, -2, -3.2f, 5.6f, 10, 6.4f), PartPose.offsetAndRotation(9, -16, 0, -0.08f, 0, -0.2f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(210, 73).mirror(false).addBox(-4.6f, -1.8f, -4.4f, 6, 2, 9)
                .texOffs(35, 73).mirror(false).addBox(-5.4f, 0.2f, -4.8f, 6.5f, 2, 10)
                .texOffs(133, 56).mirror(false).addBox(-6.2f, 2.2f, -5.2f, 7, 2.5f, 10.5f)
                .texOffs(77, 87).mirror(false).addBox(-6.8f, -0.8f, -2.5f, 1, 4.5f, 5)
                .texOffs(132, 99).mirror(false).addBox(-7.3f, 0.6f, -1.8f, 0.5f, 1.5f, 3.5f), PartPose.offsetAndRotation(-1.4f, -2.6f, 0, 0, 0, 0.32f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(210, 73).mirror(true).addBox(-1.4f, -1.8f, -4.4f, 6, 2, 9)
                .texOffs(35, 73).mirror(true).addBox(-1.1f, 0.2f, -4.8f, 6.5f, 2, 10)
                .texOffs(133, 56).mirror(true).addBox(-0.8f, 2.2f, -5.2f, 7, 2.5f, 10.5f)
                .texOffs(77, 87).mirror(true).addBox(5.8f, -0.8f, -2.5f, 1, 4.5f, 5)
                .texOffs(132, 99).mirror(true).addBox(6.8f, 0.6f, -1.8f, 0.5f, 1.5f, 3.5f), PartPose.offsetAndRotation(1.4f, -2.6f, 0, 0, 0, -0.32f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(170, 56).mirror(false).addBox(-2.6f, 0, -2.6f, 5.2f, 8, 5.2f)
                .texOffs(90, 87).mirror(false).addBox(-3.1f, 5.8f, -3.1f, 6.2f, 2.2f, 6.2f), PartPose.offsetAndRotation(-1.2f, 7.4f, 0, -1.05f, -0.5f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(170, 56).mirror(true).addBox(-2.6f, 0, -2.6f, 5.2f, 8, 5.2f)
                .texOffs(90, 87).mirror(true).addBox(-3.1f, 5.8f, -3.1f, 6.2f, 2.2f, 6.2f), PartPose.offsetAndRotation(1.2f, 7.4f, 0, -1.05f, 0.5f, 0));
        PartDefinition rightSleeve = rightForearm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(112, 0).mirror(false).addBox(-3.6f, -1, -10.5f, 7.2f, 13.5f, 10.5f), PartPose.offsetAndRotation(0, 0.4f, 2.4f, 1.1625f, 0.1717f, 0.1381f));
        PartDefinition leftSleeve = leftForearm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(112, 0).mirror(true).addBox(-3.6f, -1, -10.5f, 7.2f, 13.5f, 10.5f), PartPose.offsetAndRotation(0, 0.4f, 2.4f, 1.1625f, -0.1717f, -0.1381f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(0, 99).mirror(false).addBox(-1.7f, 0, -1.7f, 3.4f, 3.4f, 3.4f), PartPose.offsetAndRotation(0, 8.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(0, 99).mirror(true).addBox(-1.7f, 0, -1.7f, 3.4f, 3.4f, 3.4f), PartPose.offsetAndRotation(0, 8.2f, 0, 0, 0, 0));
        PartDefinition brush = rightHand.addOrReplaceChild("brush", CubeListBuilder.create()
                .texOffs(131, 32).mirror(false).addBox(-0.5f, -14, -0.5f, 1, 18, 1)
                .texOffs(7, 108).mirror(false).addBox(-0.8f, -15, -0.8f, 1.6f, 1.5f, 1.6f)
                .texOffs(7, 108).mirror(false).addBox(-0.8f, 3.5f, -0.8f, 1.6f, 1.5f, 1.6f)
                .texOffs(143, 99).mirror(false).addBox(-1, -19, -1, 2, 4, 2), PartPose.offsetAndRotation(0, 1.8f, -0.2f, 1.3237f, 0.5616f, 0.2893f));
        PartDefinition book = leftHand.addOrReplaceChild("book", CubeListBuilder.create()
                .texOffs(70, 73).mirror(false).addBox(-3.2f, -4.4f, -1.2f, 6.4f, 8.8f, 2.4f)
                .texOffs(241, 73).mirror(false).addBox(-3.5f, -4.6f, -0.3f, 0.6f, 9.2f, 0.6f), PartPose.offsetAndRotation(0.4f, 2.4f, -1.6f, 0.7649f, -0.338f, -0.1662f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(59, 56).mirror(false).addBox(-5, -10, -4.5f, 10, 7, 9)
                .texOffs(119, 87).mirror(false).addBox(-5.8f, -5.6f, -3.2f, 11.6f, 3, 6.6f)
                .texOffs(17, 99).mirror(false).addBox(-3.6f, -3.6f, -4.1f, 7.2f, 3, 5)
                .texOffs(188, 99).mirror(false).addBox(-1.4f, -6.4f, -5.5f, 2.8f, 2.8f, 1.2f)
                .texOffs(98, 56).mirror(false).addBox(-5.3f, -10.3f, -0.6f, 10.6f, 9.4f, 5.6f)
                .texOffs(58, 108).mirror(false).addBox(-2.9f, -3.4f, -4.9f, 0.9f, 1.7f, 0.8f)
                .texOffs(58, 108).mirror(false).addBox(2, -3.4f, -4.9f, 0.9f, 1.7f, 0.8f), PartPose.offsetAndRotation(0, -21.6f, -1, 0, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(63, 108).mirror(false).addBox(-1.6f, -0.8f, -0.5f, 3.2f, 1.8f, 0.6f), PartPose.offsetAndRotation(-2.5f, -6.7f, -4.5f, 0, 0.18f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(63, 108).mirror(true).addBox(-1.6f, -0.8f, -0.5f, 3.2f, 1.8f, 0.6f), PartPose.offsetAndRotation(2.5f, -6.7f, -4.5f, 0, -0.18f, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(199, 99).mirror(false).addBox(-5.4f, -1.5f, -0.9f, 5.4f, 1.9f, 2.2f), PartPose.offsetAndRotation(-0.3f, -7.5f, -4.8f, 0, 0.12f, 0.34f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(199, 99).mirror(true).addBox(0, -1.5f, -0.9f, 5.4f, 1.9f, 2.2f), PartPose.offsetAndRotation(0.3f, -7.5f, -4.8f, 0, -0.12f, -0.34f));
        PartDefinition rightBrowflame = rightBrow.addOrReplaceChild("right_browflame", CubeListBuilder.create()
                .texOffs(158, 87).mirror(false).addBox(-0.25f, -4, 0, 0.5f, 4.5f, 4.5f), PartPose.offsetAndRotation(-5, -1, 0.4f, 0.2f, -0.5f, 0.5f));
        PartDefinition leftBrowflame = leftBrow.addOrReplaceChild("left_browflame", CubeListBuilder.create()
                .texOffs(158, 87).mirror(true).addBox(-0.25f, -4, 0, 0.5f, 4.5f, 4.5f), PartPose.offsetAndRotation(5, -1, 0.4f, 0.2f, 0.5f, -0.5f));
        PartDefinition rightSideburn = head.addOrReplaceChild("right_sideburn", CubeListBuilder.create()
                .texOffs(91, 73).mirror(false).addBox(-0.4f, 0, -1.6f, 0.8f, 7.5f, 3.2f), PartPose.offsetAndRotation(-5.6f, -6.4f, -1.2f, 0.05f, 0.25f, 0.55f));
        PartDefinition leftSideburn = head.addOrReplaceChild("left_sideburn", CubeListBuilder.create()
                .texOffs(91, 73).mirror(true).addBox(-0.4f, 0, -1.6f, 0.8f, 7.5f, 3.2f), PartPose.offsetAndRotation(5.6f, -6.4f, -1.2f, 0.05f, -0.25f, -0.55f));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(16, 108).mirror(false).addBox(-5.5f, -0.75f, -0.75f, 5.5f, 1.5f, 1.5f), PartPose.offsetAndRotation(-0.7f, -4, -5.6f, 0, 0.22f, -0.12f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(16, 108).mirror(true).addBox(0, -0.75f, -0.75f, 5.5f, 1.5f, 1.5f), PartPose.offsetAndRotation(0.7f, -4, -5.6f, 0, -0.22f, 0.12f));
        PartDefinition rightMoustacheTip = rightMoustache.addOrReplaceChild("right_moustache_tip", CubeListBuilder.create()
                .texOffs(83, 108).mirror(false).addBox(-4, -0.5f, -0.5f, 4, 1, 1), PartPose.offsetAndRotation(-5.2f, 0, 0, 0, 0.2f, 0.8f));
        PartDefinition leftMoustacheTip = leftMoustache.addOrReplaceChild("left_moustache_tip", CubeListBuilder.create()
                .texOffs(83, 108).mirror(true).addBox(0, -0.5f, -0.5f, 4, 1, 1), PartPose.offsetAndRotation(5.2f, 0, 0, 0, -0.2f, -0.8f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(102, 73).mirror(false).addBox(-4.7f, 0, -3.9f, 9.4f, 3.2f, 7.4f)
                .texOffs(94, 108).mirror(false).addBox(-2.6f, -0.9f, -3.8f, 0.8f, 1, 0.7f)
                .texOffs(94, 108).mirror(false).addBox(1.8f, -0.9f, -3.8f, 0.8f, 1, 0.7f), PartPose.offsetAndRotation(0, -3, -0.8f, 0, 0, 0));
        PartDefinition beard = jaw.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(0, 87).mirror(false).addBox(-3, 0, -1, 6, 8.5f, 2), PartPose.offsetAndRotation(0, 2, -3.9f, 0.16f, 0, 0));
        PartDefinition beardTip = beard.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(171, 87).mirror(false).addBox(-2.5f, 0, -1, 5, 7, 2), PartPose.offsetAndRotation(0, 8.2f, 0, -0.08f, 0, 0));
        PartDefinition rightBeard = jaw.addOrReplaceChild("right_beard", CubeListBuilder.create()
                .texOffs(139, 73).mirror(false).addBox(-2, 0, -1, 4, 10, 2), PartPose.offsetAndRotation(-3.4f, 1.4f, -3.2f, 0.14f, 0.25f, 0.62f));
        PartDefinition leftBeard = jaw.addOrReplaceChild("left_beard", CubeListBuilder.create()
                .texOffs(139, 73).mirror(true).addBox(-2, 0, -1, 4, 10, 2), PartPose.offsetAndRotation(3.4f, 1.4f, -3.2f, 0.14f, -0.25f, -0.62f));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(0, 73).mirror(false).addBox(-4.2f, -4.6f, -3.8f, 8.4f, 4.8f, 7.6f)
                .texOffs(152, 73).mirror(false).addBox(-5.4f, -1.2f, -4.9f, 10.8f, 1.6f, 9.8f)
                .texOffs(218, 99).mirror(false).addBox(-2.4f, -4.2f, -4.4f, 4.8f, 3.4f, 0.6f)
                .texOffs(231, 99).mirror(false).addBox(-0.6f, -6.6f, -0.6f, 1.2f, 2.2f, 1.2f)
                .texOffs(99, 108).mirror(false).addBox(-7, -5.2f, -0.4f, 14, 0.8f, 0.8f), PartPose.offsetAndRotation(0, -10, 0, 0, 0, 0));
        PartDefinition board = crown.addOrReplaceChild("board", CubeListBuilder.create()
                .texOffs(136, 32).mirror(false).addBox(-6.8f, -0.8f, -9.2f, 13.6f, 0.8f, 16.4f)
                .texOffs(152, 99).mirror(false).addBox(-6.25f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(-4.25f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(-2.25f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(-0.25f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(1.75f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(3.75f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(5.75f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(-6.25f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(-4.25f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(-2.25f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(-0.25f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(1.75f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(3.75f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(152, 99).mirror(false).addBox(5.75f, 0, 6.6f, 0.5f, 4.5f, 0.5f), PartPose.offsetAndRotation(0, -4.6f, 0, 0.07f, 0, 0));
        PartDefinition rightEartie = crown.addOrReplaceChild("right_eartie", CubeListBuilder.create()
                .texOffs(98, 99).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 6, 0.5f)
                .texOffs(33, 108).mirror(false).addBox(-0.8f, 6, -0.8f, 1.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(-7, -4.8f, 0, 0, 0, -0.08f));
        PartDefinition leftEartie = crown.addOrReplaceChild("left_eartie", CubeListBuilder.create()
                .texOffs(98, 99).mirror(true).addBox(-0.25f, 0, -0.25f, 0.5f, 6, 0.5f)
                .texOffs(33, 108).mirror(true).addBox(-0.8f, 6, -0.8f, 1.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(7, -4.8f, 0, 0, 0, 0.08f));
        PartDefinition halo = body.addOrReplaceChild("halo", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-15, -15, 0, 30, 30, 1), PartPose.offsetAndRotation(0, -28.5f, 7.6f, 0, 0, 0));
        PartDefinition haloSkull0 = halo.addOrReplaceChild("halo_skull_0", CubeListBuilder.create()
                .texOffs(240, 99).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(74, 108).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(-9.6995f, 5.6f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull1 = halo.addOrReplaceChild("halo_skull_1", CubeListBuilder.create()
                .texOffs(240, 99).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(74, 108).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(-11.2f, -0, -0.4f, 0, 0, 0));
        PartDefinition haloSkull2 = halo.addOrReplaceChild("halo_skull_2", CubeListBuilder.create()
                .texOffs(240, 99).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(74, 108).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(-9.6995f, -5.6f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull3 = halo.addOrReplaceChild("halo_skull_3", CubeListBuilder.create()
                .texOffs(240, 99).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(74, 108).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(-5.6f, -9.6995f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull4 = halo.addOrReplaceChild("halo_skull_4", CubeListBuilder.create()
                .texOffs(240, 99).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(74, 108).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(0, -11.2f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull5 = halo.addOrReplaceChild("halo_skull_5", CubeListBuilder.create()
                .texOffs(240, 99).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(74, 108).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(5.6f, -9.6995f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull6 = halo.addOrReplaceChild("halo_skull_6", CubeListBuilder.create()
                .texOffs(240, 99).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(74, 108).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(9.6995f, -5.6f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull7 = halo.addOrReplaceChild("halo_skull_7", CubeListBuilder.create()
                .texOffs(240, 99).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(74, 108).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(11.2f, -0, -0.4f, 0, 0, 0));
        PartDefinition haloSkull8 = halo.addOrReplaceChild("halo_skull_8", CubeListBuilder.create()
                .texOffs(240, 99).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(74, 108).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(9.6995f, 5.6f, -0.4f, 0, 0, 0));
        PartDefinition haloFlame0 = halo.addOrReplaceChild("halo_flame_0", CubeListBuilder.create()
                .texOffs(186, 87).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f), PartPose.offsetAndRotation(-9.8995f, 9.8995f, 0.3f, 0, 0, -2.3562f));
        PartDefinition haloFlame1 = halo.addOrReplaceChild("halo_flame_1", CubeListBuilder.create()
                .texOffs(103, 99).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f), PartPose.offsetAndRotation(-12.4741f, 6.3559f, 0.3f, 0, 0, -2.042f));
        PartDefinition haloFlame2 = halo.addOrReplaceChild("halo_flame_2", CubeListBuilder.create()
                .texOffs(186, 87).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f), PartPose.offsetAndRotation(-13.8276f, 2.1901f, 0.3f, 0, 0, -1.7279f));
        PartDefinition haloFlame3 = halo.addOrReplaceChild("halo_flame_3", CubeListBuilder.create()
                .texOffs(103, 99).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f), PartPose.offsetAndRotation(-13.8276f, -2.1901f, 0.3f, 0, 0, -1.4137f));
        PartDefinition haloFlame4 = halo.addOrReplaceChild("halo_flame_4", CubeListBuilder.create()
                .texOffs(186, 87).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f), PartPose.offsetAndRotation(-12.4741f, -6.3559f, 0.3f, 0, 0, -1.0996f));
        PartDefinition haloFlame5 = halo.addOrReplaceChild("halo_flame_5", CubeListBuilder.create()
                .texOffs(103, 99).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f), PartPose.offsetAndRotation(-9.8995f, -9.8995f, 0.3f, 0, 0, -0.7854f));
        PartDefinition haloFlame6 = halo.addOrReplaceChild("halo_flame_6", CubeListBuilder.create()
                .texOffs(186, 87).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f), PartPose.offsetAndRotation(-6.3559f, -12.4741f, 0.3f, 0, 0, -0.4712f));
        PartDefinition haloFlame7 = halo.addOrReplaceChild("halo_flame_7", CubeListBuilder.create()
                .texOffs(103, 99).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f), PartPose.offsetAndRotation(-2.1901f, -13.8276f, 0.3f, 0, 0, -0.1571f));
        PartDefinition haloFlame8 = halo.addOrReplaceChild("halo_flame_8", CubeListBuilder.create()
                .texOffs(186, 87).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f), PartPose.offsetAndRotation(2.1901f, -13.8276f, 0.3f, 0, 0, 0.1571f));
        PartDefinition haloFlame9 = halo.addOrReplaceChild("halo_flame_9", CubeListBuilder.create()
                .texOffs(103, 99).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f), PartPose.offsetAndRotation(6.3559f, -12.4741f, 0.3f, 0, 0, 0.4712f));
        PartDefinition haloFlame10 = halo.addOrReplaceChild("halo_flame_10", CubeListBuilder.create()
                .texOffs(186, 87).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f), PartPose.offsetAndRotation(9.8995f, -9.8995f, 0.3f, 0, 0, 0.7854f));
        PartDefinition haloFlame11 = halo.addOrReplaceChild("halo_flame_11", CubeListBuilder.create()
                .texOffs(103, 99).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f), PartPose.offsetAndRotation(12.4741f, -6.3559f, 0.3f, 0, 0, 1.0996f));
        PartDefinition haloFlame12 = halo.addOrReplaceChild("halo_flame_12", CubeListBuilder.create()
                .texOffs(186, 87).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f), PartPose.offsetAndRotation(13.8276f, -2.1901f, 0.3f, 0, 0, 1.4137f));
        PartDefinition haloFlame13 = halo.addOrReplaceChild("halo_flame_13", CubeListBuilder.create()
                .texOffs(103, 99).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f), PartPose.offsetAndRotation(13.8276f, 2.1901f, 0.3f, 0, 0, 1.7279f));
        PartDefinition haloFlame14 = halo.addOrReplaceChild("halo_flame_14", CubeListBuilder.create()
                .texOffs(186, 87).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f), PartPose.offsetAndRotation(12.4741f, 6.3559f, 0.3f, 0, 0, 2.042f));
        PartDefinition haloFlame15 = halo.addOrReplaceChild("halo_flame_15", CubeListBuilder.create()
                .texOffs(103, 99).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f), PartPose.offsetAndRotation(9.8995f, 9.8995f, 0.3f, 0, 0, 2.3562f));
        PartDefinition hemFlame0 = skirt.addOrReplaceChild("hem_flame_0", CubeListBuilder.create()
                .texOffs(195, 73).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(3.0336f, 23, -9.0841f, 0, -0.26f, 0));
        PartDefinition hemFlame1 = skirt.addOrReplaceChild("hem_flame_1", CubeListBuilder.create()
                .texOffs(197, 87).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(8.3288f, 23, -6.6588f, 0, -0.7836f, 0));
        PartDefinition hemFlame2 = skirt.addOrReplaceChild("hem_flame_2", CubeListBuilder.create()
                .texOffs(195, 73).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(11.3924f, 23, -2.4492f, 0, -1.3072f, 0));
        PartDefinition hemFlame3 = skirt.addOrReplaceChild("hem_flame_3", CubeListBuilder.create()
                .texOffs(197, 87).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(11.4034f, 23, 2.4166f, 0, -1.8308f, 0));
        PartDefinition hemFlame4 = skirt.addOrReplaceChild("hem_flame_4", CubeListBuilder.create()
                .texOffs(195, 73).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(8.3589f, 23, 6.6348f, 0, -2.3544f, 0));
        PartDefinition hemFlame5 = skirt.addOrReplaceChild("hem_flame_5", CubeListBuilder.create()
                .texOffs(197, 87).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(3.0746f, 23, 9.0753f, 0, -2.878f, 0));
        PartDefinition hemFlame6 = skirt.addOrReplaceChild("hem_flame_6", CubeListBuilder.create()
                .texOffs(195, 73).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(-3.0336f, 23, 9.0841f, 0, -3.4016f, 0));
        PartDefinition hemFlame7 = skirt.addOrReplaceChild("hem_flame_7", CubeListBuilder.create()
                .texOffs(197, 87).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(-8.3288f, 23, 6.6588f, 0, -3.9252f, 0));
        PartDefinition hemFlame8 = skirt.addOrReplaceChild("hem_flame_8", CubeListBuilder.create()
                .texOffs(195, 73).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(-11.3924f, 23, 2.4492f, 0, -4.4488f, 0));
        PartDefinition hemFlame9 = skirt.addOrReplaceChild("hem_flame_9", CubeListBuilder.create()
                .texOffs(197, 87).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(-11.4034f, 23, -2.4166f, 0, -4.9724f, 0));
        PartDefinition hemFlame10 = skirt.addOrReplaceChild("hem_flame_10", CubeListBuilder.create()
                .texOffs(195, 73).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(-8.3589f, 23, -6.6348f, 0, -5.496f, 0));
        PartDefinition hemFlame11 = skirt.addOrReplaceChild("hem_flame_11", CubeListBuilder.create()
                .texOffs(197, 87).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(-3.0746f, 23, -9.0753f, 0, -6.0196f, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED yama: end
    }

    /**
     * His pose: the brush up in his right fist, the Book held before him in his left; judging, both
     * raised; striking, the brush brought down. In his wrath his jaws gape, his brows knot lower, the
     * hellfire behind his head flares and fire runs round his hem.
     */
    public static final class YamaModel extends EntityModel<KingYamaEntity> {
        private final ModelPart root, head, jaw, beard, beardTip, rightBeard, leftBeard, rightBrow, leftBrow, rightArm, leftArm,
                rightForearm, rightSleeve, leftSleeve, halo, chain, apron, rightBoot, leftBoot;
        private final List<ModelPart> flames = new ArrayList<>(), hemFlames = new ArrayList<>();

        public YamaModel(ModelPart root) {
            this.root = root;
            ModelPart body = root.getChild("body"), skirt = body.getChild("skirt");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            beard = jaw.getChild("beard");
            beardTip = beard.getChild("beard_tip");
            rightBeard = jaw.getChild("right_beard");
            leftBeard = jaw.getChild("left_beard");
            rightBrow = head.getChild("right_brow");
            leftBrow = head.getChild("left_brow");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            rightSleeve = rightForearm.getChild("right_sleeve");
            leftSleeve = leftArm.getChild("left_forearm").getChild("left_sleeve");
            halo = body.getChild("halo");
            chain = skirt.getChild("chain");
            apron = skirt.getChild("apron");
            rightBoot = root.getChild("right_boot");
            leftBoot = root.getChild("left_boot");
            for (int k = 0; halo.hasChild("halo_flame_" + k); k++) flames.add(halo.getChild("halo_flame_" + k));
            for (int k = 0; skirt.hasChild("hem_flame_" + k); k++) hemFlames.add(skirt.getChild("hem_flame_" + k));
        }

        /** A part turned this much from its rest. */
        private static void turn(ModelPart p, float x, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.zRot = p.getInitialPose().zRot + z;
        }

        @Override
        public void setupAnim(KingYamaEntity e, float swing, float amount, float age, float yaw, float pitch) {
            boolean wrath = e.inWrath();
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            head.y = head.getInitialPose().y + Mth.sin(age * 0.06f) * 0.2f;
            // the brush up in his right fist, the Book in his left; judging, both raised; a blow, the brush brought down
            float rx = Mth.cos(age * 0.05f) * 0.03f, lx = -rx;
            if (e.isJudging()) {
                rx = -1.25f;
                lx = -0.45f;
            }
            rx -= strike * 1.5f;
            turn(rightArm, rx, 0);
            turn(leftArm, lx, 0);
            turn(rightForearm, strike * 0.6f, 0);
            // the great sleeves hang under their weight whatever the arms do
            turn(rightSleeve, -(rx + strike * 0.6f) * 0.85f + Mth.sin(age * 0.05f) * 0.04f + amount * 0.2f, 0);
            turn(leftSleeve, -lx * 0.85f + Mth.sin(age * 0.05f + 1) * 0.04f + amount * 0.2f, 0);
            // a growl; his jaws gape in his wrath, and as he strikes
            float gape = wrath ? 0.42f + Mth.sin(age * 0.35f) * 0.05f : Math.max(0, Mth.sin(age * 0.04f)) * 0.06f;
            turn(jaw, Math.max(gape, strike * 0.35f), 0);
            turn(beard, Mth.sin(age * 0.05f) * 0.03f + amount * 0.15f, 0);
            turn(beardTip, Mth.sin(age * 0.05f + 0.7f) * 0.04f, 0);
            turn(rightBeard, 0, Mth.sin(age * 0.045f) * 0.04f);
            turn(leftBeard, 0, -Mth.sin(age * 0.045f + 0.5f) * 0.04f);
            // his brows knot lower in his wrath
            float knit = wrath ? 0.1f : 0;
            turn(rightBrow, 0, knit);
            turn(leftBrow, 0, -knit);
            rightBrow.y = rightBrow.getInitialPose().y + (wrath ? 0.3f : 0);
            leftBrow.y = rightBrow.y;
            turn(chain, amount * 0.25f + Mth.sin(age * 0.07f) * 0.03f, Mth.sin(age * 0.05f) * 0.05f);
            turn(apron, -amount * 0.2f + Mth.sin(age * 0.06f) * 0.02f, 0);
            // gliding over the floor, his boots shuffle under the hem
            float step = Mth.cos(swing * 0.6f) * 1.2f * amount;
            rightBoot.z = rightBoot.getInitialPose().z + step;
            leftBoot.z = leftBoot.getInitialPose().z - step;
            // the hellfire behind his head flickering, flaring in his wrath; and in his wrath fire round his hem
            float flare = wrath ? 1.55f : 1f;
            halo.xScale = halo.yScale = wrath ? 1.08f + Mth.sin(age * 0.2f) * 0.02f : 1f;
            for (int k = 0; k < flames.size(); k++) {
                ModelPart f = flames.get(k);
                f.yScale = flare * (1 + Mth.sin(age * 0.3f + k * 1.7f) * 0.14f);
                f.xScale = wrath ? 1.2f : 1f;
            }
            for (int k = 0; k < hemFlames.size(); k++) {
                ModelPart f = hemFlames.get(k);
                f.visible = wrath;
                f.yScale = 1 + Mth.sin(age * 0.33f + k * 2.1f) * 0.25f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class YamaRenderer extends MobRenderer<KingYamaEntity, YamaModel> {
        /** How much larger than his pixels he is drawn: some 3.3 blocks to his crown, a head over the other kings (king_yama.py prints his reach). */
        public static final float SIZE = 0.9f;
        private static final ResourceLocation SKIN = tex("king_yama");
        /** His eyes and his hellfire, drawn full-bright: smouldering, and blazing in his wrath. */
        private static final RenderType GLOW = RenderType.eyes(tex("king_yama_glow")), WRATH_GLOW = RenderType.eyes(tex("king_yama_wrath_glow"));

        public YamaRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new YamaModel(ctx.bakeLayer(YAMA_LAYER)), 1.0f);
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, KingYamaEntity e,
                                   float swing, float amount, float partial, float age, float yaw, float pitch) {
                    VertexConsumer vc = buffers.getBuffer(e.inWrath() ? WRATH_GLOW : GLOW);
                    getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        @Override
        protected void scale(KingYamaEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(KingYamaEntity e) {
            return SKIN;
        }
    }

    /** For the gallery: whether a zone creature is drawn as a captain. */
    public static boolean captain(Mob m) {
        return m instanceof ZoneMonster z && z.isCaptain();
    }
}
