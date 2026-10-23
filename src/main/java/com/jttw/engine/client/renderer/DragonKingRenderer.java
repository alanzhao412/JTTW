package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.waters.DragonKingEntity;
import com.jttw.engine.world.waters.Sea;
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

import java.util.EnumMap;
import java.util.Map;

/**
 * A Dragon King: a dragon in a man's shape, a head taller than one. His head is a dragon's, long in
 * the snout, gold-eyed, antlered, its golden whiskers trailing, a mane down its neck and a beard
 * under its jaw, a gold crown with a pearl set in it between his horns; he wears a court robe to
 * the ground, broad-sleeved, edged in gold, a gold belt with a jade plaque, his scaled hands
 * clawed, and his tail trails out from under his robe. Each king's robe and scales are his sea's
 * colours (East blue-green, South red, West white, North black). Model and skins:
 * {@code assets_source/sea_court.py}.
 */
public final class DragonKingRenderer extends MobRenderer<DragonKingEntity, DragonKingRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("dragon_king"), "main");
    private static final Map<Sea, ResourceLocation> SKINS = new EnumMap<>(Sea.class), GLOWS = new EnumMap<>(Sea.class);

    static {
        for (Sea sea : Sea.values()) {
            SKINS.put(sea, JTTWEngine.id("textures/entity/" + sea.king + ".png"));
            GLOWS.put(sea, JTTWEngine.id("textures/entity/" + sea.king + "_glow.png"));
        }
    }

    public DragonKingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.6f);
        // his eyes and his crown's pearl, full-bright even in the deep
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, DragonKingEntity e,
                               float swing, float amount, float partial, float age, float yaw, float pitch) {
                VertexConsumer vc = buffers.getBuffer(RenderType.eyes(GLOWS.get(e.sea())));
                getParentModel().renderToBuffer(pose, vc, 0xF00000, OverlayTexture.NO_OVERLAY, -1);
            }
        });
    }

    /** A head taller than his soldiers: his model drawn larger than its pixels. */
    public static final float SCALE = 1.2f;

    @Override
    protected void scale(DragonKingEntity e, PoseStack pose, float partial) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(DragonKingEntity e) {
        return SKINS.get(e.sea());
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(41, 0).mirror(false).addBox(-5, -4, -3, 10, 14, 6)
                .texOffs(50, 37).mirror(false).addBox(-6, -4.5f, -3.5f, 12, 2.5f, 7)
                .texOffs(46, 49).mirror(false).addBox(-5.5f, 6, -3.5f, 11, 2, 7)
                .texOffs(100, 60).mirror(false).addBox(-1.5f, 5.6f, -3.9f, 3, 2.6f, 0.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-6, 0, -4, 12, 14, 8)
                .texOffs(79, 23).mirror(false).addBox(-6.3f, 12, -4.3f, 12.6f, 2, 8.6f), PartPose.offsetAndRotation(0, 9, 0, 0, 0, 0));
        PartDefinition rightBoot = root.addOrReplaceChild("right_boot", CubeListBuilder.create()
                .texOffs(74, 60).mirror(false).addBox(-2, 0, -1.5f, 4, 2, 3), PartPose.offsetAndRotation(-2.5f, 22, -3.5f, 0, 0, 0));
        PartDefinition leftBoot = root.addOrReplaceChild("left_boot", CubeListBuilder.create()
                .texOffs(74, 60).mirror(true).addBox(-2, 0, -1.5f, 4, 2, 3), PartPose.offsetAndRotation(2.5f, 22, -3.5f, 0, 0, 0));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 23).mirror(false).addBox(-2, -2, 0, 4, 4, 9), PartPose.offsetAndRotation(0, 19, 3.5f, 1.2f, 0, 0));
        PartDefinition tailEnd = tail.addOrReplaceChild("tail_end", CubeListBuilder.create()
                .texOffs(0, 37).mirror(false).addBox(-1.5f, -1.5f, 0, 3, 3, 8)
                .texOffs(89, 37).mirror(false).addBox(-0.5f, -3, 0, 1, 1.5f, 8), PartPose.offsetAndRotation(0, 0, 8.5f, -0.6f, 0, 0));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(107, 0).mirror(false).addBox(-3, -2, -2, 4, 11, 4)
                .texOffs(0, 49).mirror(false).addBox(-3.6f, 7, -2.6f, 5.2f, 4, 5.2f)
                .texOffs(48, 60).mirror(false).addBox(-2.5f, 11, -1.5f, 3, 3, 3)
                .texOffs(118, 60).mirror(false).addBox(-2.5f, 13.5f, -1.6f, 0.8f, 1.5f, 0.8f)
                .texOffs(118, 60).mirror(false).addBox(-0.9f, 13.5f, -1.6f, 0.8f, 1.5f, 0.8f), PartPose.offsetAndRotation(-6.5f, -2.5f, 0, 0, 0, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(107, 0).mirror(true).addBox(-1, -2, -2, 4, 11, 4)
                .texOffs(0, 49).mirror(true).addBox(-1.6f, 7, -2.6f, 5.2f, 4, 5.2f)
                .texOffs(48, 60).mirror(true).addBox(-0.5f, 11, -1.5f, 3, 3, 3)
                .texOffs(118, 60).mirror(true).addBox(-0.3f, 13.5f, -1.6f, 0.8f, 1.5f, 0.8f)
                .texOffs(118, 60).mirror(true).addBox(1.3f, 13.5f, -1.6f, 0.8f, 1.5f, 0.8f), PartPose.offsetAndRotation(6.5f, -2.5f, 0, 0, 0, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(74, 0).mirror(false).addBox(-4, -9, -4, 8, 9, 8)
                .texOffs(23, 37).mirror(false).addBox(-3, -6, -11, 6, 4, 7)
                .texOffs(25, 60).mirror(false).addBox(-2.5f, -7, -10, 5, 1, 6)
                .texOffs(27, 23).mirror(false).addBox(-4.5f, -8, 2.5f, 9, 10, 3)
                .texOffs(61, 60).mirror(false).addBox(-2, 0, -6, 4, 4, 2)
                .texOffs(83, 49).mirror(false).addBox(-3, -12, -3, 6, 3, 6)
                .texOffs(109, 60).mirror(false).addBox(-1.5f, -14.5f, -3.6f, 3, 3, 1)
                .texOffs(0, 70).mirror(false).addBox(-0.6f, -13.8f, -4.1f, 1.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(0, -4, 0, 0, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 60).mirror(false).addBox(-2.5f, 0, -7, 5, 1.6f, 7)
                .texOffs(7, 70).mirror(false).addBox(-2.1f, -0.9f, -6.6f, 0.6f, 1, 0.6f)
                .texOffs(7, 70).mirror(false).addBox(1.5f, -0.9f, -6.6f, 0.6f, 1, 0.6f), PartPose.offsetAndRotation(0, -2, -4, 0.15f, 0, 0));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(25, 49).mirror(false).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 8), PartPose.offsetAndRotation(-2.2f, -9, -1, 0.75f, -0.3f, 0));
        PartDefinition rightHornBranch = rightHorn.addOrReplaceChild("right_horn_branch", CubeListBuilder.create()
                .texOffs(89, 60).mirror(false).addBox(-0.5f, -0.5f, 0, 1, 1, 4), PartPose.offsetAndRotation(0, 0, 3.5f, -0.9f, -0.3f, 0));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(25, 49).mirror(true).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 8), PartPose.offsetAndRotation(2.2f, -9, -1, 0.75f, 0.3f, 0));
        PartDefinition leftHornBranch = leftHorn.addOrReplaceChild("left_horn_branch", CubeListBuilder.create()
                .texOffs(89, 60).mirror(true).addBox(-0.5f, -0.5f, 0, 1, 1, 4), PartPose.offsetAndRotation(0, 0, 3.5f, -0.9f, 0.3f, 0));
        PartDefinition rightWhisker = head.addOrReplaceChild("right_whisker", CubeListBuilder.create()
                .texOffs(52, 23).mirror(false).addBox(-0.3f, -0.3f, 0, 0.6f, 0.6f, 12), PartPose.offsetAndRotation(-3, -3.5f, -10, -0.35f, 0.5f, 0));
        PartDefinition leftWhisker = head.addOrReplaceChild("left_whisker", CubeListBuilder.create()
                .texOffs(52, 23).mirror(true).addBox(-0.3f, -0.3f, 0, 0.6f, 0.6f, 12), PartPose.offsetAndRotation(3, -3.5f, -10, -0.35f, -0.5f, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<DragonKingEntity> {
        private final ModelPart root, head, jaw, body, skirt, rightArm, leftArm, tail, tailEnd, rightWhisker, leftWhisker;

        public Model(ModelPart root) {
            this.root = root;
            head = root.getChild("head");
            jaw = head.getChild("jaw");
            body = root.getChild("body");
            skirt = body.getChild("skirt");
            rightArm = root.getChild("right_arm");
            leftArm = root.getChild("left_arm");
            tail = root.getChild("tail");
            tailEnd = tail.getChild("tail_end");
            rightWhisker = head.getChild("right_whisker");
            leftWhisker = head.getChild("left_whisker");
        }

        @Override
        public void setupAnim(DragonKingEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            float drift = Mth.sin(age * 0.07f);
            // his sleeves drifting in the water; the right hand raised to strike or cast
            float strike = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.15f + drift * 0.12f - strike * 1.6f - Mth.cos(swing * 0.6f) * 0.4f * amount;
            leftArm.xRot = -0.15f - drift * 0.12f + Mth.cos(swing * 0.6f) * 0.4f * amount;
            rightArm.zRot = 0.18f;
            leftArm.zRot = -0.18f;
            skirt.xRot = Mth.sin(age * 0.09f) * 0.05f + amount * 0.15f;
            jaw.xRot = jaw.getInitialPose().xRot + strike * 0.5f + Math.max(0, Mth.sin(age * 0.05f)) * 0.08f;
            tail.yRot = Mth.sin(age * 0.06f) * 0.3f;
            tailEnd.yRot = Mth.sin(age * 0.06f - 0.8f) * 0.4f;
            rightWhisker.yRot = rightWhisker.getInitialPose().yRot + Mth.sin(age * 0.1f) * 0.15f;
            leftWhisker.yRot = leftWhisker.getInitialPose().yRot - Mth.sin(age * 0.1f + 0.7f) * 0.15f;
            rightWhisker.xRot = rightWhisker.getInitialPose().xRot + Mth.sin(age * 0.13f) * 0.12f;
            leftWhisker.xRot = leftWhisker.getInitialPose().xRot + Mth.sin(age * 0.13f + 1.3f) * 0.12f;
            body.y = Mth.sin(age * 0.07f) * 0.4f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
