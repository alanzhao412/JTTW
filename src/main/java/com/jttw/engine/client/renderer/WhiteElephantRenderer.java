package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.WhiteElephantEntity;
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
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The White Elephant, in his two shapes (his true form's once {@link WhiteElephantEntity#inTrueForm()}).
 * <ul>
 *   <li>His demon-king shape: a giant two and a half times a man's height, broader and heavier than
 *       the Lion, in ivory armour edged with gold (a gold wheel on his breast) over white hide, a
 *       crimson mantle, round pauldrons; a great domed elephant's head under a gold crown and
 *       frontlet, eyes smouldering red under a heavy brow, ears like shields, a long heavy trunk in
 *       four joints, six tusks (two great ones, gold-capped); pillar legs; a giant's lance. His
 *       trunk sways; rearing to strike it lifts high, and holding a foe it reaches out and up before
 *       him, coiled at its end; stamping, he rears back; charging, he puts his head down.</li>
 *   <li>His true form, Samantabhadra's white elephant: a towering war elephant in a crimson and gold
 *       saddle-cloth, the Bodhisattva's empty lotus seat on his back, gold on his brow, trunk and
 *       legs; ears like sails, a trunk to the ground, six tusks, the two great ones longer than a
 *       man. Holding a foe, his trunk lifts it high over his head; trampling, he rears up on his hind
 *       legs; charging, his tusks come down.</li>
 * </ul>
 * Models and skins: {@code assets_source/lion_camel.py}.
 */
public final class WhiteElephantRenderer extends MobRenderer<WhiteElephantEntity, EntityModel<WhiteElephantEntity>> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("white_elephant"), "main");
    public static final ModelLayerLocation TRUE_LAYER = new ModelLayerLocation(JTTWEngine.id("white_elephant"), "true_form");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/white_elephant.png");
    public static final ResourceLocation TRUE_TEXTURE = JTTWEngine.id("textures/entity/white_elephant_true.png");
    /** How much larger than their pixels his shapes are drawn: the demon king some 4.6 blocks tall, the elephant some five. */
    public static final float SCALE = 1.08f, TRUE_SCALE = 1.55f;

    private final EntityModel<WhiteElephantEntity> king, elephant;

    public WhiteElephantRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.6f);
        king = model;
        elephant = new TrueModel(ctx.bakeLayer(TRUE_LAYER));
        addLayer(LionCourt.glow(this, e -> e.inTrueForm() ? "white_elephant_true" : "white_elephant"));
    }

    @Override
    public void render(WhiteElephantEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        model = e.inTrueForm() ? elephant : king;
        shadowRadius = e.inTrueForm() ? 2.8f : 1.6f;
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    protected void scale(WhiteElephantEntity e, PoseStack pose, float partial) {
        float k = e.inTrueForm() ? TRUE_SCALE : SCALE;
        pose.scale(k, k, k);
    }

    @Override
    public ResourceLocation getTextureLocation(WhiteElephantEntity e) {
        return e.inTrueForm() ? TRUE_TEXTURE : TEXTURE;
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 69).mirror(false).addBox(-10.5f, -12, -7, 21, 12, 14)
                .texOffs(54, 0).mirror(false).addBox(-14, -27, -8.5f, 28, 16, 17)
                .texOffs(95, 161).mirror(false).addBox(-9, -25, -9.1f, 18, 12, 1)
                .texOffs(45, 121).mirror(false).addBox(-12, -3, -7.6f, 24, 3, 15.2f)
                .texOffs(71, 69).mirror(false).addBox(-12.5f, 0, -7.8f, 25, 10, 15.6f)
                .texOffs(0, 142).mirror(false).addBox(-12, -30, -7, 24, 4, 14)
                .texOffs(145, 0).mirror(false).addBox(-13, -27, 8.5f, 26, 28, 2), PartPose.offsetAndRotation(0, 3, 0, 0.08f, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(121, 96).mirror(false).addBox(-9, -16, -12, 18, 9, 12)
                .texOffs(176, 161).mirror(false).addBox(-8, -19, -10, 7, 3, 8)
                .texOffs(176, 161).mirror(false).addBox(1, -19, -10, 7, 3, 8)
                .texOffs(182, 96).mirror(false).addBox(-8, -7, -13, 16, 9, 12)
                .texOffs(0, 161).mirror(false).addBox(-5.5f, 2, -13, 11, 6, 9)
                .texOffs(43, 177).mirror(false).addBox(-3.5f, 7, -10, 7, 3, 6)
                .texOffs(70, 177).mirror(false).addBox(-7, 3, -12, 3, 4, 5)
                .texOffs(70, 177).mirror(false).addBox(4, 3, -12, 3, 4, 5)
                .texOffs(126, 121).mirror(false).addBox(-4, -17.5f, -13.4f, 8, 17, 1.4f)
                .texOffs(0, 188).mirror(false).addBox(-9.5f, -13, -12.4f, 19, 2, 1), PartPose.offsetAndRotation(0, -30, -6, 0.12f, 0, 0));
        PartDefinition rightEbrow = head.addOrReplaceChild("right_ebrow", CubeListBuilder.create()
                .texOffs(148, 177).mirror(false).addBox(-8.5f, -1, -1.5f, 8.5f, 2.5f, 3), PartPose.offsetAndRotation(-0.5f, -7, -13.6f, 0, 0, 0.28f));
        PartDefinition leftEbrow = head.addOrReplaceChild("left_ebrow", CubeListBuilder.create()
                .texOffs(148, 177).mirror(true).addBox(0, -1, -1.5f, 8.5f, 2.5f, 3), PartPose.offsetAndRotation(0.5f, -7, -13.6f, 0, 0, -0.28f));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(104, 177).mirror(false).addBox(-1.25f, -5, -1.25f, 2.5f, 5, 2.5f)
                .texOffs(208, 177).mirror(false).addBox(-0.6f, -7.5f, -0.6f, 1.2f, 2.5f, 1.2f), PartPose.offsetAndRotation(0, -17.5f, -12, -0.35f, 0, 0));
        PartDefinition rightCrest = head.addOrReplaceChild("right_crest", CubeListBuilder.create()
                .texOffs(173, 177).mirror(false).addBox(-1, -4, -1, 2, 4, 2)
                .texOffs(41, 188).mirror(false).addBox(-0.5f, -6, -0.5f, 1, 2, 1), PartPose.offsetAndRotation(-3, -17, -12, -0.35f, 0, -0.35f));
        PartDefinition leftCrest = head.addOrReplaceChild("left_crest", CubeListBuilder.create()
                .texOffs(173, 177).mirror(true).addBox(-1, -4, -1, 2, 4, 2)
                .texOffs(41, 188).mirror(true).addBox(-0.5f, -6, -0.5f, 1, 2, 1), PartPose.offsetAndRotation(3, -17, -12, -0.35f, 0, 0.35f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-1.5f, -3, 0, 1.5f, 15, 20)
                .texOffs(199, 69).mirror(false).addBox(-1.5f, 12, 2, 1.5f, 9, 15)
                .texOffs(155, 161).mirror(false).addBox(-1.5f, 21, 5, 1.5f, 4, 8), PartPose.offsetAndRotation(-9, -13, -6, 0, -0.7f, 0.14f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(9, 0).mirror(true).addBox(0, -3, 0, 1.5f, 15, 20)
                .texOffs(199, 69).mirror(true).addBox(0, 12, 2, 1.5f, 9, 15)
                .texOffs(155, 161).mirror(true).addBox(0, 21, 5, 1.5f, 4, 8), PartPose.offsetAndRotation(9, -13, -6, 0, 0.7f, -0.14f));
        PartDefinition trunk1 = head.addOrReplaceChild("trunk1", CubeListBuilder.create()
                .texOffs(41, 161).mirror(false).addBox(-3.2f, 0, -3.2f, 6.4f, 8, 6.4f)
                .texOffs(207, 161).mirror(false).addBox(-3.6f, 0, -3.6f, 7.2f, 2.5f, 7.2f)
                .texOffs(126, 177).mirror(false).addBox(-2.2f, 2.5f, -3.8f, 4.4f, 5.5f, 0.6f), PartPose.offsetAndRotation(0, 6, -11.5f, -0.05f, 0, 0));
        PartDefinition trunk2 = trunk1.addOrReplaceChild("trunk2", CubeListBuilder.create()
                .texOffs(70, 161).mirror(false).addBox(-2.7f, 0, -2.7f, 5.4f, 8, 5.4f), PartPose.offsetAndRotation(0, 8, 0, -0.05f, 0, 0));
        PartDefinition trunk3 = trunk2.addOrReplaceChild("trunk3", CubeListBuilder.create()
                .texOffs(134, 161).mirror(false).addBox(-2.2f, 0, -2.2f, 4.4f, 8, 4.4f), PartPose.offsetAndRotation(0, 8, 0, -0.05f, 0, 0));
        PartDefinition trunk4 = trunk3.addOrReplaceChild("trunk4", CubeListBuilder.create()
                .texOffs(240, 161).mirror(false).addBox(-1.7f, 0, -1.7f, 3.4f, 7, 3.4f)
                .texOffs(182, 177).mirror(false).addBox(-2, 4.5f, -2, 4, 1.5f, 4), PartPose.offsetAndRotation(0, 8, 0, -0.05f, 0, 0));
        PartDefinition rightTusk = head.addOrReplaceChild("right_tusk", CubeListBuilder.create()
                .texOffs(147, 121).mirror(false).addBox(-2.25f, 0, -2.25f, 4.5f, 14, 4.5f), PartPose.offsetAndRotation(-5.5f, 6, -11, -1.4f, 0, 0.2f));
        PartDefinition leftTusk = head.addOrReplaceChild("left_tusk", CubeListBuilder.create()
                .texOffs(147, 121).mirror(true).addBox(-2.25f, 0, -2.25f, 4.5f, 14, 4.5f), PartPose.offsetAndRotation(5.5f, 6, -11, -1.4f, 0, -0.2f));
        PartDefinition rightTuskCurve = rightTusk.addOrReplaceChild("right_tusk_curve", CubeListBuilder.create()
                .texOffs(126, 142).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 12, 3.6f), PartPose.offsetAndRotation(0, 12.2f, 0, -0.55f, 0, 0));
        PartDefinition leftTuskCurve = leftTusk.addOrReplaceChild("left_tusk_curve", CubeListBuilder.create()
                .texOffs(126, 142).mirror(true).addBox(-1.8f, 0, -1.8f, 3.6f, 12, 3.6f), PartPose.offsetAndRotation(0, 12.2f, 0, -0.55f, 0, 0));
        PartDefinition rightTuskTip = rightTuskCurve.addOrReplaceChild("right_tusk_tip", CubeListBuilder.create()
                .texOffs(0, 177).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 5.04f, 3.6f)
                .texOffs(199, 177).mirror(false).addBox(-0.9f, 5.04f, -0.9f, 1.8f, 3.6f, 1.8f), PartPose.offsetAndRotation(0, 10.8f, 0, -0.65f, 0, 0));
        PartDefinition leftTuskTip = leftTuskCurve.addOrReplaceChild("left_tusk_tip", CubeListBuilder.create()
                .texOffs(0, 177).mirror(true).addBox(-1.8f, 0, -1.8f, 3.6f, 5.04f, 3.6f)
                .texOffs(199, 177).mirror(true).addBox(-0.9f, 5.04f, -0.9f, 1.8f, 3.6f, 1.8f), PartPose.offsetAndRotation(0, 10.8f, 0, -0.65f, 0, 0));
        PartDefinition rightTuskHigh = head.addOrReplaceChild("right_tusk_high", CubeListBuilder.create()
                .texOffs(117, 177).mirror(false).addBox(-0.9f, 0, -0.9f, 1.8f, 6, 1.8f), PartPose.offsetAndRotation(-6.5f, 3, -12, -1, 0, 0.45f));
        PartDefinition leftTuskHigh = head.addOrReplaceChild("left_tusk_high", CubeListBuilder.create()
                .texOffs(117, 177).mirror(true).addBox(-0.9f, 0, -0.9f, 1.8f, 6, 1.8f), PartPose.offsetAndRotation(6.5f, 3, -12, -1, 0, -0.45f));
        PartDefinition rightTuskHighCurve = rightTuskHigh.addOrReplaceChild("right_tusk_high_curve", CubeListBuilder.create()
                .texOffs(139, 177).mirror(false).addBox(-0.7f, 0, -0.7f, 1.4f, 5, 1.4f), PartPose.offsetAndRotation(0, 5.3f, 0, -0.55f, 0, 0));
        PartDefinition leftTuskHighCurve = leftTuskHigh.addOrReplaceChild("left_tusk_high_curve", CubeListBuilder.create()
                .texOffs(139, 177).mirror(true).addBox(-0.7f, 0, -0.7f, 1.4f, 5, 1.4f), PartPose.offsetAndRotation(0, 5.3f, 0, -0.55f, 0, 0));
        PartDefinition rightTuskHighTip = rightTuskHighCurve.addOrReplaceChild("right_tusk_high_tip", CubeListBuilder.create()
                .texOffs(217, 177).mirror(false).addBox(-0.8f, 0, -0.8f, 1.6f, 2.24f, 1.6f)
                .texOffs(46, 188).mirror(false).addBox(-0.4f, 2.24f, -0.4f, 0.8f, 1.6f, 0.8f), PartPose.offsetAndRotation(0, 4.4667f, 0, -0.65f, 0, 0));
        PartDefinition leftTuskHighTip = leftTuskHighCurve.addOrReplaceChild("left_tusk_high_tip", CubeListBuilder.create()
                .texOffs(217, 177).mirror(true).addBox(-0.8f, 0, -0.8f, 1.6f, 2.24f, 1.6f)
                .texOffs(46, 188).mirror(true).addBox(-0.4f, 2.24f, -0.4f, 0.8f, 1.6f, 0.8f), PartPose.offsetAndRotation(0, 4.4667f, 0, -0.65f, 0, 0));
        PartDefinition rightTuskLow = head.addOrReplaceChild("right_tusk_low", CubeListBuilder.create()
                .texOffs(117, 177).mirror(false).addBox(-0.9f, 0, -0.9f, 1.8f, 6, 1.8f), PartPose.offsetAndRotation(-2.5f, 8.5f, -10, -0.7f, 0, 0.1f));
        PartDefinition leftTuskLow = head.addOrReplaceChild("left_tusk_low", CubeListBuilder.create()
                .texOffs(117, 177).mirror(true).addBox(-0.9f, 0, -0.9f, 1.8f, 6, 1.8f), PartPose.offsetAndRotation(2.5f, 8.5f, -10, -0.7f, 0, -0.1f));
        PartDefinition rightTuskLowCurve = rightTuskLow.addOrReplaceChild("right_tusk_low_curve", CubeListBuilder.create()
                .texOffs(139, 177).mirror(false).addBox(-0.7f, 0, -0.7f, 1.4f, 5, 1.4f), PartPose.offsetAndRotation(0, 5.3f, 0, -0.55f, 0, 0));
        PartDefinition leftTuskLowCurve = leftTuskLow.addOrReplaceChild("left_tusk_low_curve", CubeListBuilder.create()
                .texOffs(139, 177).mirror(true).addBox(-0.7f, 0, -0.7f, 1.4f, 5, 1.4f), PartPose.offsetAndRotation(0, 5.3f, 0, -0.55f, 0, 0));
        PartDefinition rightTuskLowTip = rightTuskLowCurve.addOrReplaceChild("right_tusk_low_tip", CubeListBuilder.create()
                .texOffs(217, 177).mirror(false).addBox(-0.8f, 0, -0.8f, 1.6f, 2.24f, 1.6f)
                .texOffs(46, 188).mirror(false).addBox(-0.4f, 2.24f, -0.4f, 0.8f, 1.6f, 0.8f), PartPose.offsetAndRotation(0, 4.4667f, 0, -0.65f, 0, 0));
        PartDefinition leftTuskLowTip = leftTuskLowCurve.addOrReplaceChild("left_tusk_low_tip", CubeListBuilder.create()
                .texOffs(217, 177).mirror(true).addBox(-0.8f, 0, -0.8f, 1.6f, 2.24f, 1.6f)
                .texOffs(46, 188).mirror(true).addBox(-0.4f, 2.24f, -0.4f, 0.8f, 1.6f, 0.8f), PartPose.offsetAndRotation(0, 4.4667f, 0, -0.65f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(61, 96).mirror(false).addBox(-5, -2, -5, 10, 13, 10)
                .texOffs(0, 96).mirror(false).addBox(-8, -7, -7.5f, 15, 9, 15)
                .texOffs(168, 121).mirror(false).addBox(-8.5f, -1, -7, 8, 5, 14)
                .texOffs(17, 177).mirror(false).addBox(-8.6f, -4.5f, -2.5f, 1, 5, 5), PartPose.offsetAndRotation(-18, -24, -0.5f, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(61, 96).mirror(true).addBox(-5, -2, -5, 10, 13, 10)
                .texOffs(0, 96).mirror(true).addBox(-7, -7, -7.5f, 15, 9, 15)
                .texOffs(168, 121).mirror(true).addBox(0.5f, -1, -7, 8, 5, 14)
                .texOffs(17, 177).mirror(true).addBox(7.6f, -4.5f, -2.5f, 1, 5, 5), PartPose.offsetAndRotation(18, -24, -0.5f, 0, 0, 0));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(0, 121).mirror(false).addBox(-5.5f, 0, -5.5f, 11, 9, 11)
                .texOffs(143, 142).mirror(false).addBox(-5, 9, -5.2f, 10, 5, 10.4f), PartPose.offsetAndRotation(0, 11, 0, -0.2f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(0, 121).mirror(true).addBox(-5.5f, 0, -5.5f, 11, 9, 11)
                .texOffs(143, 142).mirror(true).addBox(-5, 9, -5.2f, 10, 5, 10.4f), PartPose.offsetAndRotation(0, 11, 0, -0.2f, 0, 0));
        PartDefinition lance = rightForearm.addOrReplaceChild("lance", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1, -48, -1, 2, 66, 2)
                .texOffs(102, 96).mirror(false).addBox(-1.2f, -64, -3, 2.4f, 16, 6)
                .texOffs(30, 177).mirror(false).addBox(-0.8f, -70, -1.8f, 1.6f, 6, 3.6f)
                .texOffs(182, 177).mirror(false).addBox(-2, -49, -2, 4, 2, 4)
                .texOffs(87, 177).mirror(false).addBox(-1.8f, -47, -1.8f, 3.6f, 5, 3.6f)
                .texOffs(226, 177).mirror(false).addBox(-1.4f, 17, -1.4f, 2.8f, 2, 2.8f), PartPose.offsetAndRotation(0, 11.5f, 0, 0.7f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(154, 69).mirror(false).addBox(-5.5f, 0, -5.5f, 11, 15, 11)
                .texOffs(77, 142).mirror(false).addBox(-6, 3, -6, 12, 6, 12)
                .texOffs(186, 142).mirror(false).addBox(-6, 15, -6, 12, 4, 12), PartPose.offsetAndRotation(-7, 5, 0.5f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(154, 69).mirror(true).addBox(-5.5f, 0, -5.5f, 11, 15, 11)
                .texOffs(77, 142).mirror(true).addBox(-6, 3, -6, 12, 6, 12)
                .texOffs(186, 142).mirror(true).addBox(-6, 15, -6, 12, 4, 12), PartPose.offsetAndRotation(7, 5, 0.5f, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static LayerDefinition trueLayer() {
        // GENERATED true form: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-14, -16, -21, 28, 24, 48)
                .texOffs(0, 151).mirror(false).addBox(-13, -20, -18, 26, 5, 18)
                .texOffs(0, 73).mirror(false).addBox(-15, -18, -10, 30, 15, 29)
                .texOffs(0, 118).mirror(false).addBox(-15.4f, -4, -10.4f, 30.8f, 2, 29.8f)
                .texOffs(107, 175).mirror(false).addBox(-7, -21, -6, 14, 3, 14)
                .texOffs(152, 211).mirror(false).addBox(-14.6f, -10, -21, 29.2f, 6, 2)
                .texOffs(22, 223).mirror(false).addBox(-2, -4.5f, -22.5f, 4, 4, 3), PartPose.offsetAndRotation(0, -2, 2, 0, 0, 0));
        PartDefinition rightSeatPost = body.addOrReplaceChild("right_seat_post", CubeListBuilder.create()
                .texOffs(82, 211).mirror(false).addBox(-0.75f, -7, -0.75f, 1.5f, 7, 1.5f), PartPose.offsetAndRotation(-6.5f, -21, -5.5f, 0, 0, 0));
        PartDefinition leftSeatPost = body.addOrReplaceChild("left_seat_post", CubeListBuilder.create()
                .texOffs(82, 211).mirror(true).addBox(-0.75f, -7, -0.75f, 1.5f, 7, 1.5f), PartPose.offsetAndRotation(6.5f, -21, -5.5f, 0, 0, 0));
        PartDefinition rightSeatPost2 = body.addOrReplaceChild("right_seat_post2", CubeListBuilder.create()
                .texOffs(82, 211).mirror(false).addBox(-0.75f, -7, -0.75f, 1.5f, 7, 1.5f), PartPose.offsetAndRotation(-6.5f, -21, 7.5f, 0, 0, 0));
        PartDefinition leftSeatPost2 = body.addOrReplaceChild("left_seat_post2", CubeListBuilder.create()
                .texOffs(82, 211).mirror(true).addBox(-0.75f, -7, -0.75f, 1.5f, 7, 1.5f), PartPose.offsetAndRotation(6.5f, -21, 7.5f, 0, 0, 0));
        PartDefinition seatCanopy = body.addOrReplaceChild("seat_canopy", CubeListBuilder.create()
                .texOffs(42, 175).mirror(false).addBox(-8, -1.5f, -8, 16, 1.5f, 16)
                .texOffs(92, 223).mirror(false).addBox(-1, -4.5f, -1, 2, 3, 2), PartPose.offsetAndRotation(0, -28, 1, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(114, 151).mirror(false).addBox(-9, -16, -12, 18, 9, 12)
                .texOffs(0, 211).mirror(false).addBox(-8, -19, -10, 7, 3, 8)
                .texOffs(0, 211).mirror(false).addBox(1, -19, -10, 7, 3, 8)
                .texOffs(175, 151).mirror(false).addBox(-8, -7, -13, 16, 9, 12)
                .texOffs(197, 175).mirror(false).addBox(-5.5f, 2, -13, 11, 6, 9)
                .texOffs(91, 211).mirror(false).addBox(-3.5f, 7, -10, 7, 3, 6)
                .texOffs(118, 211).mirror(false).addBox(-7, 3, -12, 3, 4, 5)
                .texOffs(118, 211).mirror(false).addBox(4, 3, -12, 3, 4, 5)
                .texOffs(0, 175).mirror(false).addBox(-4, -17.5f, -13.4f, 8, 17, 1.4f)
                .texOffs(110, 223).mirror(false).addBox(-9.5f, -13, -12.4f, 19, 2, 1), PartPose.offsetAndRotation(0, -12, -20, 0.12f, 0, 0));
        PartDefinition rightEbrow = head.addOrReplaceChild("right_ebrow", CubeListBuilder.create()
                .texOffs(58, 223).mirror(false).addBox(-8.5f, -1, -1.5f, 8.5f, 2.5f, 3), PartPose.offsetAndRotation(-0.5f, -7, -13.6f, 0, 0, 0.28f));
        PartDefinition leftEbrow = head.addOrReplaceChild("left_ebrow", CubeListBuilder.create()
                .texOffs(58, 223).mirror(true).addBox(0, -1, -1.5f, 8.5f, 2.5f, 3), PartPose.offsetAndRotation(0.5f, -7, -13.6f, 0, 0, -0.28f));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(217, 211).mirror(false).addBox(-1.25f, -5, -1.25f, 2.5f, 5, 2.5f)
                .texOffs(92, 223).mirror(false).addBox(-0.6f, -7.5f, -0.6f, 1.2f, 2.5f, 1.2f), PartPose.offsetAndRotation(0, -17.5f, -12, -0.35f, 0, 0));
        PartDefinition rightCrest = head.addOrReplaceChild("right_crest", CubeListBuilder.create()
                .texOffs(83, 223).mirror(false).addBox(-1, -4, -1, 2, 4, 2)
                .texOffs(151, 223).mirror(false).addBox(-0.5f, -6, -0.5f, 1, 2, 1), PartPose.offsetAndRotation(-3, -17, -12, -0.35f, 0, -0.35f));
        PartDefinition leftCrest = head.addOrReplaceChild("left_crest", CubeListBuilder.create()
                .texOffs(83, 223).mirror(true).addBox(-1, -4, -1, 2, 4, 2)
                .texOffs(151, 223).mirror(true).addBox(-0.5f, -6, -0.5f, 1, 2, 1), PartPose.offsetAndRotation(3, -17, -12, -0.35f, 0, 0.35f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(119, 73).mirror(false).addBox(-1.5f, -3, 0, 1.5f, 17.25f, 23)
                .texOffs(172, 118).mirror(false).addBox(-1.5f, 14.25f, 2.3f, 1.5f, 10.35f, 17.25f)
                .texOffs(0, 195).mirror(false).addBox(-1.5f, 24.6f, 5.75f, 1.5f, 4.6f, 9.2f), PartPose.offsetAndRotation(-9, -13, -6, 0, -0.7f, 0.14f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(119, 73).mirror(true).addBox(0, -3, 0, 1.5f, 17.25f, 23)
                .texOffs(172, 118).mirror(true).addBox(0, 14.25f, 2.3f, 1.5f, 10.35f, 17.25f)
                .texOffs(0, 195).mirror(true).addBox(0, 24.6f, 5.75f, 1.5f, 4.6f, 9.2f), PartPose.offsetAndRotation(9, -13, -6, 0, 0.7f, -0.14f));
        PartDefinition trunk1 = head.addOrReplaceChild("trunk1", CubeListBuilder.create()
                .texOffs(164, 175).mirror(false).addBox(-3.8f, 0, -3.8f, 7.6f, 9, 7.6f)
                .texOffs(185, 195).mirror(false).addBox(-4.2f, 0, -4.2f, 8.4f, 2.5f, 8.4f)
                .texOffs(230, 211).mirror(false).addBox(-2.8f, 2.5f, -4.4f, 5.6f, 6.5f, 0.6f), PartPose.offsetAndRotation(0, 6, -11.5f, -0.05f, 0, 0));
        PartDefinition trunk2 = trunk1.addOrReplaceChild("trunk2", CubeListBuilder.create()
                .texOffs(25, 195).mirror(false).addBox(-3.3f, 0, -3.3f, 6.6f, 8, 6.6f), PartPose.offsetAndRotation(0, 9, 0, -0.05f, 0, 0));
        PartDefinition trunk3 = trunk2.addOrReplaceChild("trunk3", CubeListBuilder.create()
                .texOffs(160, 195).mirror(false).addBox(-2.8f, 0, -2.8f, 5.6f, 8, 5.6f), PartPose.offsetAndRotation(0, 8, 0, -0.05f, 0, 0));
        PartDefinition trunk4 = trunk3.addOrReplaceChild("trunk4", CubeListBuilder.create()
                .texOffs(222, 195).mirror(false).addBox(-2.3f, 0, -2.3f, 4.6f, 7, 4.6f), PartPose.offsetAndRotation(0, 8, 0, -0.05f, 0, 0));
        PartDefinition trunk5 = trunk4.addOrReplaceChild("trunk5", CubeListBuilder.create()
                .texOffs(52, 211).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 6, 3.6f)
                .texOffs(37, 223).mirror(false).addBox(-2.1f, 3.5f, -2.1f, 4.2f, 1.5f, 4.2f), PartPose.offsetAndRotation(0, 7, 0, -0.05f, 0, 0));
        PartDefinition rightTusk = head.addOrReplaceChild("right_tusk", CubeListBuilder.create()
                .texOffs(89, 151).mirror(false).addBox(-2.5875f, 0, -2.5875f, 5.175f, 16.1f, 5.175f), PartPose.offsetAndRotation(-5.5f, 6, -11, -1.4f, 0, 0.2f));
        PartDefinition leftTusk = head.addOrReplaceChild("left_tusk", CubeListBuilder.create()
                .texOffs(89, 151).mirror(true).addBox(-2.5875f, 0, -2.5875f, 5.175f, 16.1f, 5.175f), PartPose.offsetAndRotation(5.5f, 6, -11, -1.4f, 0, -0.2f));
        PartDefinition rightTuskCurve = rightTusk.addOrReplaceChild("right_tusk_curve", CubeListBuilder.create()
                .texOffs(21, 175).mirror(false).addBox(-2.07f, 0, -2.07f, 4.14f, 13.8f, 4.14f), PartPose.offsetAndRotation(0, 14.03f, 0, -0.55f, 0, 0));
        PartDefinition leftTuskCurve = leftTusk.addOrReplaceChild("left_tusk_curve", CubeListBuilder.create()
                .texOffs(21, 175).mirror(true).addBox(-2.07f, 0, -2.07f, 4.14f, 13.8f, 4.14f), PartPose.offsetAndRotation(0, 14.03f, 0, -0.55f, 0, 0));
        PartDefinition rightTuskTip = rightTuskCurve.addOrReplaceChild("right_tusk_tip", CubeListBuilder.create()
                .texOffs(31, 211).mirror(false).addBox(-2.07f, 0, -2.07f, 4.14f, 5.796f, 4.14f)
                .texOffs(0, 223).mirror(false).addBox(-1.035f, 5.796f, -1.035f, 2.07f, 4.14f, 2.07f), PartPose.offsetAndRotation(0, 12.42f, 0, -0.65f, 0, 0));
        PartDefinition leftTuskTip = leftTuskCurve.addOrReplaceChild("left_tusk_tip", CubeListBuilder.create()
                .texOffs(31, 211).mirror(true).addBox(-2.07f, 0, -2.07f, 4.14f, 5.796f, 4.14f)
                .texOffs(0, 223).mirror(true).addBox(-1.035f, 5.796f, -1.035f, 2.07f, 4.14f, 2.07f), PartPose.offsetAndRotation(0, 12.42f, 0, -0.65f, 0, 0));
        PartDefinition rightTuskHigh = head.addOrReplaceChild("right_tusk_high", CubeListBuilder.create()
                .texOffs(69, 211).mirror(false).addBox(-1.035f, 0, -1.035f, 2.07f, 6.9f, 2.07f), PartPose.offsetAndRotation(-6.5f, 3, -12, -1, 0, 0.45f));
        PartDefinition leftTuskHigh = head.addOrReplaceChild("left_tusk_high", CubeListBuilder.create()
                .texOffs(69, 211).mirror(true).addBox(-1.035f, 0, -1.035f, 2.07f, 6.9f, 2.07f), PartPose.offsetAndRotation(6.5f, 3, -12, -1, 0, -0.45f));
        PartDefinition rightTuskHighCurve = rightTuskHigh.addOrReplaceChild("right_tusk_high_curve", CubeListBuilder.create()
                .texOffs(13, 223).mirror(false).addBox(-0.805f, 0, -0.805f, 1.61f, 5.75f, 1.61f), PartPose.offsetAndRotation(0, 6.095f, 0, -0.55f, 0, 0));
        PartDefinition leftTuskHighCurve = leftTuskHigh.addOrReplaceChild("left_tusk_high_curve", CubeListBuilder.create()
                .texOffs(13, 223).mirror(true).addBox(-0.805f, 0, -0.805f, 1.61f, 5.75f, 1.61f), PartPose.offsetAndRotation(0, 6.095f, 0, -0.55f, 0, 0));
        PartDefinition rightTuskHighTip = rightTuskHighCurve.addOrReplaceChild("right_tusk_high_tip", CubeListBuilder.create()
                .texOffs(101, 223).mirror(false).addBox(-0.92f, 0, -0.92f, 1.84f, 2.576f, 1.84f)
                .texOffs(156, 223).mirror(false).addBox(-0.46f, 2.576f, -0.46f, 0.92f, 1.84f, 0.92f), PartPose.offsetAndRotation(0, 5.1367f, 0, -0.65f, 0, 0));
        PartDefinition leftTuskHighTip = leftTuskHighCurve.addOrReplaceChild("left_tusk_high_tip", CubeListBuilder.create()
                .texOffs(101, 223).mirror(true).addBox(-0.92f, 0, -0.92f, 1.84f, 2.576f, 1.84f)
                .texOffs(156, 223).mirror(true).addBox(-0.46f, 2.576f, -0.46f, 0.92f, 1.84f, 0.92f), PartPose.offsetAndRotation(0, 5.1367f, 0, -0.65f, 0, 0));
        PartDefinition rightTuskLow = head.addOrReplaceChild("right_tusk_low", CubeListBuilder.create()
                .texOffs(69, 211).mirror(false).addBox(-1.035f, 0, -1.035f, 2.07f, 6.9f, 2.07f), PartPose.offsetAndRotation(-2.5f, 8.5f, -10, -0.7f, 0, 0.1f));
        PartDefinition leftTuskLow = head.addOrReplaceChild("left_tusk_low", CubeListBuilder.create()
                .texOffs(69, 211).mirror(true).addBox(-1.035f, 0, -1.035f, 2.07f, 6.9f, 2.07f), PartPose.offsetAndRotation(2.5f, 8.5f, -10, -0.7f, 0, -0.1f));
        PartDefinition rightTuskLowCurve = rightTuskLow.addOrReplaceChild("right_tusk_low_curve", CubeListBuilder.create()
                .texOffs(13, 223).mirror(false).addBox(-0.805f, 0, -0.805f, 1.61f, 5.75f, 1.61f), PartPose.offsetAndRotation(0, 6.095f, 0, -0.55f, 0, 0));
        PartDefinition leftTuskLowCurve = leftTuskLow.addOrReplaceChild("left_tusk_low_curve", CubeListBuilder.create()
                .texOffs(13, 223).mirror(true).addBox(-0.805f, 0, -0.805f, 1.61f, 5.75f, 1.61f), PartPose.offsetAndRotation(0, 6.095f, 0, -0.55f, 0, 0));
        PartDefinition rightTuskLowTip = rightTuskLowCurve.addOrReplaceChild("right_tusk_low_tip", CubeListBuilder.create()
                .texOffs(101, 223).mirror(false).addBox(-0.92f, 0, -0.92f, 1.84f, 2.576f, 1.84f)
                .texOffs(156, 223).mirror(false).addBox(-0.46f, 2.576f, -0.46f, 0.92f, 1.84f, 0.92f), PartPose.offsetAndRotation(0, 5.1367f, 0, -0.65f, 0, 0));
        PartDefinition leftTuskLowTip = leftTuskLowCurve.addOrReplaceChild("left_tusk_low_tip", CubeListBuilder.create()
                .texOffs(101, 223).mirror(true).addBox(-0.92f, 0, -0.92f, 1.84f, 2.576f, 1.84f)
                .texOffs(156, 223).mirror(true).addBox(-0.46f, 2.576f, -0.46f, 0.92f, 1.84f, 0.92f), PartPose.offsetAndRotation(0, 5.1367f, 0, -0.65f, 0, 0));
        PartDefinition rightForeleg = body.addOrReplaceChild("right_foreleg", CubeListBuilder.create()
                .texOffs(123, 118).mirror(false).addBox(-6, -2, -6, 12, 18, 12)
                .texOffs(54, 195).mirror(false).addBox(-6.4f, 9, -6.4f, 12.8f, 2, 12.8f)
                .texOffs(107, 195).mirror(false).addBox(-6.5f, 16, -6.5f, 13, 2, 13), PartPose.offsetAndRotation(-8.5f, 8, -11, 0, 0, 0));
        PartDefinition leftForeleg = body.addOrReplaceChild("left_foreleg", CubeListBuilder.create()
                .texOffs(123, 118).mirror(true).addBox(-6, -2, -6, 12, 18, 12)
                .texOffs(54, 195).mirror(true).addBox(-6.4f, 9, -6.4f, 12.8f, 2, 12.8f)
                .texOffs(107, 195).mirror(true).addBox(-6.5f, 16, -6.5f, 13, 2, 13), PartPose.offsetAndRotation(8.5f, 8, -11, 0, 0, 0));
        PartDefinition rightHindleg = body.addOrReplaceChild("right_hindleg", CubeListBuilder.create()
                .texOffs(123, 118).mirror(false).addBox(-6, -2, -6, 12, 18, 12)
                .texOffs(54, 195).mirror(false).addBox(-6.4f, 9, -6.4f, 12.8f, 2, 12.8f)
                .texOffs(107, 195).mirror(false).addBox(-6.5f, 16, -6.5f, 13, 2, 13), PartPose.offsetAndRotation(-8.5f, 8, 20, 0, 0, 0));
        PartDefinition leftHindleg = body.addOrReplaceChild("left_hindleg", CubeListBuilder.create()
                .texOffs(123, 118).mirror(true).addBox(-6, -2, -6, 12, 18, 12)
                .texOffs(54, 195).mirror(true).addBox(-6.4f, 9, -6.4f, 12.8f, 2, 12.8f)
                .texOffs(107, 195).mirror(true).addBox(-6.5f, 16, -6.5f, 13, 2, 13), PartPose.offsetAndRotation(8.5f, 8, 20, 0, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(232, 151).mirror(false).addBox(-1, 0, -1, 2, 18, 2)
                .texOffs(135, 211).mirror(false).addBox(-1.6f, 16, -1.6f, 3.2f, 5, 3.2f), PartPose.offsetAndRotation(0, -12, 27, 0.35f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED true form: end
    }

    /** His trunk at rest: hanging, swaying, its tip curling forward (a negative turn swings a joint out before him). */
    private static void sway(ModelPart[] trunk, float age, float[] rest) {
        float sway = Mth.sin(age * 0.07f);
        for (int i = 0; i < trunk.length; i++) {
            trunk[i].xRot = rest[i] + sway * (0.05f + i * 0.03f);
            trunk[i].zRot = i == 0 ? Mth.cos(age * 0.05f) * 0.07f : 0;
        }
    }

    /** His demon-king shape. */
    public static final class Model extends EntityModel<WhiteElephantEntity> {
        private static final float[] REST = {0f, -0.03f, -0.06f, -0.35f};
        private final ModelPart root, body, head, rightEar, leftEar, rightArm, leftArm, rightForearm, leftForearm, lance, rightLeg, leftLeg;
        private final ModelPart[] trunk = new ModelPart[4];

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            trunk[0] = head.getChild("trunk1");
            for (int i = 1; i < trunk.length; i++) trunk[i] = trunk[i - 1].getChild("trunk" + (i + 1));
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            lance = rightForearm.getChild("lance");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(WhiteElephantEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = LionCourt.walk(rightLeg, leftLeg, swing, amount, 0.4f, 0.6f);
            body.xRot = 0.08f + Mth.sin(age * 0.05f) * 0.012f;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            head.xRot = 0.12f + pitch * Mth.DEG_TO_RAD * 0.4f;
            sway(trunk, age, REST);
            float flap = Mth.sin(age * 0.08f) * 0.1f;
            rightEar.yRot = -0.55f - flap;
            leftEar.yRot = 0.55f + flap;
            float blow = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.3f - stride * 0.35f - blow * 1.6f;
            rightArm.zRot = 0.15f;
            rightForearm.xRot = -0.4f - blow * 0.4f;
            leftArm.xRot = -0.05f + stride * 0.35f;
            leftArm.zRot = -0.2f;
            leftForearm.xRot = -0.3f;
            switch (e.trunk()) {
                case WhiteElephantEntity.TRUNK_REARING -> {
                    // raised high over his head, about to lash out
                    trunk[0].xRot = -2.6f;
                    trunk[1].xRot = -0.4f;
                    trunk[2].xRot = -0.3f;
                    trunk[3].xRot = -0.3f;
                    head.xRot = -0.25f;
                }
                case WhiteElephantEntity.TRUNK_HOLDING -> {
                    // reaching out and up before him, coiled at its end round what it holds
                    trunk[0].xRot = -1.95f;
                    trunk[1].xRot = 0.05f;
                    trunk[2].xRot = 0.1f;
                    trunk[3].xRot = 1.6f;
                    leftArm.xRot = -0.8f;
                    leftForearm.xRot = -0.8f;
                }
                default -> {}
            }
            if (e.isRearing()) {
                body.xRot = -0.3f;
                head.xRot = -0.4f;
                rightArm.xRot = leftArm.xRot = -2.4f;
            }
            if (e.isCharging()) {
                body.xRot = 0.4f;
                head.xRot = 0.55f;
                trunk[0].xRot = 0.5f;
                rightEar.yRot = -1.1f;
                leftEar.yRot = 1.1f;
            }
            if (e.isRevealing()) {
                // his true form bursting out of him: he rears and shudders, trumpeting, trunk high, arms flung out
                float shudder = Mth.sin(age * 1.7f) * 0.06f;
                body.xRot = -0.3f + shudder;
                head.xRot = -0.6f;
                head.zRot = shudder * 2;
                trunk[0].xRot = -2.8f;
                trunk[1].xRot = -0.5f;
                trunk[2].xRot = -0.4f;
                trunk[3].xRot = -0.4f;
                rightArm.xRot = leftArm.xRot = -1.3f;
                rightArm.zRot = 1.2f;
                leftArm.zRot = -1.2f;
                rightEar.yRot = -1.2f;
                leftEar.yRot = 1.2f;
            } else {
                head.zRot = 0;
            }
            // his lance held upright at rest, its butt by his foot; in a blow it comes down along his arm, a thrust
            lance.xRot = -(rightArm.xRot + rightForearm.xRot + body.xRot) * (1 - blow);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    /** His true form: the war elephant. */
    public static final class TrueModel extends EntityModel<WhiteElephantEntity> {
        private static final float[] REST = {0f, -0.02f, -0.04f, -0.15f, -0.6f};
        /** The body's pivot, and where his hind feet meet the ground from it (y, z): he rears about them. */
        private static final float BODY_Y = -2, BODY_Z = 2, HIND_Y = 26, HIND_Z = 20;
        private final ModelPart root, body, head, rightEar, leftEar, rightFore, leftFore, rightHind, leftHind, tail;
        private final ModelPart[] trunk = new ModelPart[5];

        public TrueModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            trunk[0] = head.getChild("trunk1");
            for (int i = 1; i < trunk.length; i++) trunk[i] = trunk[i - 1].getChild("trunk" + (i + 1));
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
            rightFore = body.getChild("right_foreleg");
            leftFore = body.getChild("left_foreleg");
            rightHind = body.getChild("right_hindleg");
            leftHind = body.getChild("left_hindleg");
            tail = body.getChild("tail");
        }

        @Override
        public void setupAnim(WhiteElephantEntity e, float swing, float amount, float age, float yaw, float pitch) {
            // a slow, ponderous gait, the legs on each side moving together
            float gait = Mth.cos(swing * 0.4f) * 0.55f * amount;
            rightFore.xRot = rightHind.xRot = gait;
            leftFore.xRot = leftHind.xRot = -gait;
            body.xRot = 0;
            body.y = BODY_Y;
            body.z = BODY_Z;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.4f;
            head.xRot = 0.15f + pitch * Mth.DEG_TO_RAD * 0.3f;
            head.zRot = 0;
            sway(trunk, age, REST);
            float flap = Mth.sin(age * 0.06f) * 0.12f;
            rightEar.yRot = -0.6f - flap;
            leftEar.yRot = 0.6f + flap;
            tail.zRot = Mth.sin(age * 0.1f) * 0.2f;
            switch (e.trunk()) {
                case WhiteElephantEntity.TRUNK_REARING -> {
                    trunk[0].xRot = -2.7f;
                    trunk[1].xRot = -0.3f;
                    trunk[2].xRot = -0.3f;
                    trunk[3].xRot = -0.3f;
                    trunk[4].xRot = -0.3f;
                    head.xRot = -0.2f;
                }
                case WhiteElephantEntity.TRUNK_HOLDING -> {
                    // lifted high over his head, coiled at its end round what it holds
                    trunk[0].xRot = -2.95f;
                    trunk[1].xRot = 0.05f;
                    trunk[2].xRot = 0.15f;
                    trunk[3].xRot = 1.4f;
                    trunk[4].xRot = 1.3f;
                    head.xRot = -0.15f;
                }
                default -> {}
            }
            if (e.isRearing()) {
                // up on his hind legs, forefeet pawing the air, about to bring them down
                rear(-0.5f);
                rightFore.xRot = -0.9f;
                leftFore.xRot = -0.4f;
                head.xRot = -0.3f;
                trunk[0].xRot = -2.2f;
            }
            if (e.isCharging()) {
                head.xRot = 0.45f;
                trunk[0].xRot = 0.6f;
                trunk[1].xRot = 0.3f;
                rightEar.yRot = -1.2f;
                leftEar.yRot = 1.2f;
            }
        }

        /** Tilts him by {@code angle} (negative: nose up) about where his hind feet stand, his hind legs kept upright. */
        private void rear(float angle) {
            float c = Mth.cos(angle), s = Mth.sin(angle);
            body.xRot = angle;
            body.y = BODY_Y + HIND_Y - (HIND_Y * c - HIND_Z * s);
            body.z = BODY_Z + HIND_Z - (HIND_Y * s + HIND_Z * c);
            rightHind.xRot = leftHind.xRot = -angle;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
