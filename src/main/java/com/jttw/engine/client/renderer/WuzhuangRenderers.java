package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wuzhuang.CloudCraneEntity;
import com.jttw.engine.entity.wuzhuang.DaoistDiscipleEntity;
import com.jttw.engine.entity.wuzhuang.TempleImmortal;
import com.jttw.engine.entity.wuzhuang.TempleYouthEntity;
import com.jttw.engine.entity.wuzhuang.ZhenyuanEntity;
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
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * How Mount Wuzhuang's household looks. The disciples, the youths and Zhenyuan share a robed
 * person's frame (128x64): a Daoist robe to the ground over a person's body; the disciples a
 * topknot with a hairpin, the youths the two buns of boys' hair (丫髻), Zhenyuan wide sleeves, a
 * small gold crown, a long white beard and his horsetail whisk (his left sleeve swells as he sweeps
 * with it). The cloud crane has a box model of its own (64x64): a long neck, a red crown, black-tipped
 * wings spread and beating in flight, folded at rest, swept back in a swoop, long legs trailing.
 * Skins painted by {@code assets_source/wuzhuang_folk.py} and {@code wuzhuang_crane.py}.
 */
public final class WuzhuangRenderers {

    private WuzhuangRenderers() {}

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name), "main");
    }

    public static final ModelLayerLocation DISCIPLE_LAYER = layer("daoist_disciple"), YOUTH_LAYER = layer("temple_youth"),
            ZHENYUAN_LAYER = layer("zhenyuan"), CRANE_LAYER = layer("cloud_crane");

    // ── The robed frame ─────────────────────────────────────────────────

    /** A person (128x64) in a Daoist robe to the ground. */
    private static MeshDefinition robed(float flare) {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        mesh.getRoot().getChild("body").addOrReplaceChild("robe", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-5, 10, -3.5f, 10, 14, 7, new CubeDeformation(flare)), PartPose.ZERO);
        return mesh;
    }

    /** A disciple: a topknot bound with a hairpin. */
    public static LayerDefinition discipleLayer() {
        MeshDefinition mesh = robed(0.1f);
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("bun", CubeListBuilder.create().texOffs(88, 22).addBox(-1.5f, -11, -1.5f, 3, 3, 3), PartPose.ZERO);
        head.addOrReplaceChild("pin", CubeListBuilder.create().texOffs(100, 22).addBox(-3, -10, -0.5f, 6, 1, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** A youth: the two buns of a boy's hair, either side of his crown. */
    public static LayerDefinition youthLayer() {
        MeshDefinition mesh = robed(0.1f);
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("bun_l", CubeListBuilder.create().texOffs(88, 22).addBox(1.5f, -10.5f, -1.5f, 3, 3, 3), PartPose.ZERO);
        head.addOrReplaceChild("bun_r", CubeListBuilder.create().texOffs(88, 22).addBox(-4.5f, -10.5f, -1.5f, 3, 3, 3), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Zhenyuan: wide sleeves, a small gold crown on his topknot, a long white beard, his whisk in his right hand. */
    public static LayerDefinition zhenyuanLayer() {
        MeshDefinition mesh = robed(0.25f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(64, 36).addBox(-2.5f, -11, -2.5f, 5, 3, 5), PartPose.ZERO);
        head.addOrReplaceChild("pin", CubeListBuilder.create().texOffs(100, 22).addBox(-3, -10, -0.5f, 6, 1, 1), PartPose.ZERO);
        head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(104, 25).addBox(-2, -1.5f, -4.4f, 4, 8, 1), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("sleeve", CubeListBuilder.create().texOffs(64, 22)
                .addBox(-3.5f, 3, -2.5f, 5, 9, 5, new CubeDeformation(0.4f)), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("sleeve", CubeListBuilder.create().texOffs(64, 22).mirror()
                .addBox(-1.5f, 3, -2.5f, 5, 9, 5, new CubeDeformation(0.4f)), PartPose.ZERO);
        PartDefinition whisk = root.getChild("right_arm").addOrReplaceChild("whisk", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.5f, -7, -0.5f, 1, 9, 1), PartPose.offsetAndRotation(-1, 10, -1, -0.5f, 0, 0));
        whisk.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(116, 36).addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(0, -7, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** The robed frame's pose: legs under the robe; a bow when they have yielded. */
    public static class RobedModel<T extends Mob> extends HumanoidModel<T> {
        public RobedModel(ModelPart root) {
            super(root);
            leftLeg.visible = false;
            rightLeg.visible = false;
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing * 0.5f, amount, age, yaw, pitch);
            if (e instanceof TempleImmortal t && t.isYielded()) {
                // hands joined before him, head bowed: he acknowledges you
                head.xRot = 0.45f;
                hat.xRot = 0.45f;
                rightArm.xRot = -1.1f;
                leftArm.xRot = -1.1f;
                rightArm.yRot = -0.55f;
                leftArm.yRot = 0.55f;
            }
        }
    }

    public static final class ZhenyuanModel extends RobedModel<ZhenyuanEntity> {
        private final ModelPart leftSleeve, whisk, tail;

        public ZhenyuanModel(ModelPart root) {
            super(root);
            leftSleeve = leftArm.getChild("sleeve");
            whisk = rightArm.getChild("whisk");
            tail = whisk.getChild("tail");
        }

        @Override
        public void setupAnim(ZhenyuanEntity e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing, amount, age, yaw, pitch);
            leftSleeve.xScale = leftSleeve.yScale = leftSleeve.zScale = 1f;
            // the horsetail hangs from the top of the handle, swaying
            tail.xRot = -(rightArm.xRot + whisk.xRot) + 0.15f + Mth.sin(age * 0.08f) * 0.06f;
            if (e.isYielded()) return;
            // the whisk held up before him
            if (attackTime <= 0) {
                rightArm.xRot = -0.6f + Mth.cos(age * 0.07f) * 0.05f;
                rightArm.yRot = -0.2f;
            }
            tail.xRot = -(rightArm.xRot + whisk.xRot) + 0.15f + Mth.sin(age * 0.08f) * 0.06f;
            if (e.sleeveRaised()) {
                // his left sleeve flung open toward his foe, swelling: the Universe in his Sleeve
                leftArm.xRot = -1.55f;
                leftArm.yRot = 0.25f;
                leftArm.zRot = -0.15f;
                float swell = 1.9f + Mth.sin(age * 0.6f) * 0.15f;
                leftSleeve.xScale = swell;
                leftSleeve.zScale = swell;
                leftSleeve.yScale = 1.25f;
            } else {
                leftArm.xRot = -0.25f + Mth.cos(age * 0.05f) * 0.04f;
                leftArm.zRot = -0.08f;
            }
        }
    }

    // ── Renderers ───────────────────────────────────────────────────────

    public static final class DiscipleRenderer extends HumanoidMobRenderer<DaoistDiscipleEntity, RobedModel<DaoistDiscipleEntity>> {
        private static final ResourceLocation SKIN = tex("daoist_disciple"), CAPTAIN = tex("daoist_disciple_captain");

        public DiscipleRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new RobedModel<>(ctx.bakeLayer(DISCIPLE_LAYER)), 0.45f);
        }

        @Override
        public ResourceLocation getTextureLocation(DaoistDiscipleEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected boolean shouldShowName(DaoistDiscipleEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    public static final class YouthRenderer<T extends TempleYouthEntity> extends HumanoidMobRenderer<T, RobedModel<T>> {
        public static final float SIZE = 0.86f;
        private final ResourceLocation skin;

        public YouthRenderer(EntityRendererProvider.Context ctx, String name) {
            super(ctx, new RobedModel<>(ctx.bakeLayer(YOUTH_LAYER)), 0.35f);
            skin = tex(name);
        }

        public static <T extends TempleYouthEntity> EntityRendererProvider<T> of(String name) {
            return ctx -> new YouthRenderer<>(ctx, name);
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

    public static final class ZhenyuanRenderer extends HumanoidMobRenderer<ZhenyuanEntity, ZhenyuanModel> {
        public static final float SIZE = 1.12f;
        private static final ResourceLocation SKIN = tex("zhenyuan");

        public ZhenyuanRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ZhenyuanModel(ctx.bakeLayer(ZHENYUAN_LAYER)), 0.5f);
        }

        @Override
        protected void scale(ZhenyuanEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(ZhenyuanEntity e) {
            return SKIN;
        }
    }

    // ── The cloud crane ─────────────────────────────────────────────────

    /** A crane (64x64): body, two-jointed neck, head with crown and beak, wings, tail, long legs. */
    public static LayerDefinition craneLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -3.5f, -7, 7, 7, 14),
                PartPose.offsetAndRotation(0, 10, 0, -0.12f, 0, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 22).addBox(-1, -8, -1, 2, 8, 2),
                PartPose.offsetAndRotation(0, -2, -6, 0.45f, 0, 0));
        PartDefinition upper = neck.addOrReplaceChild("upper_neck", CubeListBuilder.create().texOffs(8, 22).addBox(-1, -6, -1, 2, 6, 2),
                PartPose.offsetAndRotation(0, -8, 0, -0.6f, 0, 0));
        PartDefinition head = upper.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 22).addBox(-1.5f, -2.5f, -3, 3, 3, 4),
                PartPose.offsetAndRotation(0, -6, 0, 0.15f, 0, 0));
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(30, 22).addBox(-1, -3, -2, 2, 1, 2), PartPose.ZERO);
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(36, 22).addBox(-0.5f, -1.5f, -8, 1, 1, 5), PartPose.ZERO);
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 32).addBox(-14, -0.5f, -2, 14, 1, 10),
                PartPose.offset(-3.5f, -2.5f, -3));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 44).mirror().addBox(0, -0.5f, -2, 14, 1, 10),
                PartPose.offset(3.5f, -2.5f, -3));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(42, 0).addBox(-2.5f, -1, 0, 5, 2, 5),
                PartPose.offsetAndRotation(0, -1, 6.5f, 0.35f, 0, 0));
        for (int side : new int[]{-1, 1}) {
            PartDefinition leg = root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create().texOffs(50, 32)
                    .addBox(-0.5f, 0, -0.5f, 1, 11, 1), PartPose.offset(side * 1.5f, 13, 1));
            leg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(54, 44).addBox(-1, 10.5f, -2.5f, 2, 1, 3), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static final class CraneModel extends EntityModel<CloudCraneEntity> {
        private final ModelPart root, body, neck, head, rightWing, leftWing, rightLeg, leftLeg;

        public CraneModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            neck = body.getChild("neck");
            head = neck.getChild("upper_neck").getChild("head");
            rightWing = body.getChild("right_wing");
            leftWing = body.getChild("left_wing");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(CloudCraneEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            neck.yRot = yaw * Mth.DEG_TO_RAD * 0.3f;
            boolean flying = !e.onGround();
            if (e.isSwooping()) {
                // wings swept back, neck and legs stretched out
                rightWing.zRot = 0.15f;
                leftWing.zRot = -0.15f;
                rightWing.yRot = -0.75f;
                leftWing.yRot = 0.75f;
                neck.xRot = 1.25f;
                body.xRot = 0.35f;
                rightLeg.xRot = leftLeg.xRot = 1.4f;
            } else if (flying) {
                float flap = Mth.sin(age * 0.45f) * 0.75f;
                rightWing.zRot = flap;
                leftWing.zRot = -flap;
                rightWing.yRot = leftWing.yRot = 0;
                neck.xRot = 0.95f;
                body.xRot = 0.05f;
                rightLeg.xRot = leftLeg.xRot = 1.35f;
            } else {
                // folded along its sides; it stalks
                rightWing.zRot = -1.42f;
                leftWing.zRot = 1.42f;
                rightWing.yRot = leftWing.yRot = 0;
                neck.xRot = 0.45f + Mth.sin(age * 0.05f) * 0.05f;
                body.xRot = -0.12f;
                rightLeg.xRot = Mth.cos(swing * 0.6f) * 0.8f * amount;
                leftLeg.xRot = Mth.cos(swing * 0.6f + Mth.PI) * 0.8f * amount;
            }
            if (attackTime > 0) neck.xRot += Mth.sin(attackTime * Mth.PI) * 0.8f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class CraneRenderer extends MobRenderer<CloudCraneEntity, CraneModel> {
        private static final ResourceLocation SKIN = tex("cloud_crane"), CAPTAIN = tex("cloud_crane_captain");

        public CraneRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new CraneModel(ctx.bakeLayer(CRANE_LAYER)), 0.45f);
        }

        @Override
        public ResourceLocation getTextureLocation(CloudCraneEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected boolean shouldShowName(CloudCraneEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }
}
