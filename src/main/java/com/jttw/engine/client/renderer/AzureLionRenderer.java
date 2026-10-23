package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.AzureLionEntity;
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
 * The Azure Lion, in his two shapes (his true form's once {@link AzureLionEntity#inTrueForm()}).
 * <ul>
 *   <li>His demon-king shape: a hulking giant two and a half times a man's height, hunched forward
 *       from the waist, his weight in his chest and spiked shoulders, arms hanging to his knees and
 *       ending in claws; dark lacquered armour edged with gold, a gold lion's face on his breast; his
 *       head thrust out low in a wild green-blue mane that bristles into points, a snarling maw full
 *       of fangs under a heavy brow, eyes smouldering amber. A giant's broadsword. Gaping, he bends
 *       to his prey, jaws wide; roaring, he throws his head back, arms flung out; revealing his true
 *       form, he rears and shudders.</li>
 *   <li>His true form, Manjushri's lion: on all fours, long and heavy, a deep chest and shoulders
 *       buried in the mane, a lean waist, haunches, the head carried low and forward; claws out, a
 *       long tail. He gapes, roars, and pounces, forelegs reaching, hind legs thrown back.</li>
 * </ul>
 * Models and skins: {@code assets_source/lion_camel.py}.
 */
public final class AzureLionRenderer extends MobRenderer<AzureLionEntity, EntityModel<AzureLionEntity>> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("azure_lion"), "main");
    public static final ModelLayerLocation TRUE_LAYER = new ModelLayerLocation(JTTWEngine.id("azure_lion"), "true_form");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/azure_lion.png");
    public static final ResourceLocation TRUE_TEXTURE = JTTWEngine.id("textures/entity/azure_lion_true.png");
    /** How much larger than their pixels his shapes are drawn: the demon king some 4.5 blocks tall, the lion twice as long as a man is tall and more. */
    public static final float SCALE = 1.15f, TRUE_SCALE = 1.7f;

    private final EntityModel<AzureLionEntity> king, lion;

    public AzureLionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.5f);
        king = model;
        lion = new TrueModel(ctx.bakeLayer(TRUE_LAYER));
        addLayer(LionCourt.glow(this, e -> e.inTrueForm() ? "azure_lion_true" : "azure_lion"));
    }

    @Override
    public void render(AzureLionEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        model = e.inTrueForm() ? lion : king;
        shadowRadius = e.inTrueForm() ? 2.8f : 1.5f;
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    protected void scale(AzureLionEntity e, PoseStack pose, float partial) {
        float k = e.inTrueForm() ? TRUE_SCALE : SCALE;
        pose.scale(k, k, k);
    }

    @Override
    public ResourceLocation getTextureLocation(AzureLionEntity e) {
        return e.inTrueForm() ? TRUE_TEXTURE : TEXTURE;
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(120, 42).mirror(false).addBox(-9.5f, -11, -6, 19, 11, 12)
                .texOffs(84, 0).mirror(false).addBox(-13, -25, -8, 26, 15, 16)
                .texOffs(201, 113).mirror(false).addBox(-8.5f, -23, -8.6f, 17, 11, 1)
                .texOffs(168, 93).mirror(false).addBox(-11, -3, -6.8f, 22, 3, 13.6f)
                .texOffs(202, 130).mirror(false).addBox(-3, -4, -7.6f, 6, 5, 1)
                .texOffs(41, 42).mirror(false).addBox(-12, 0, -7.2f, 24, 9, 14.4f)
                .texOffs(62, 69).mirror(false).addBox(-14, -31, -2, 28, 9, 12)
                .texOffs(169, 0).mirror(false).addBox(-12.5f, -24, 6, 25, 22, 5), PartPose.offsetAndRotation(0, 4, 0, 0.12f, 0, 0));
        PartDefinition rightShoulderTuft = body.addOrReplaceChild("right_shoulder_tuft", CubeListBuilder.create()
                .texOffs(0, 130).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(135, 130).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(-10, -30, 6, 0.8f, 0, -0.6f));
        PartDefinition leftShoulderTuft = body.addOrReplaceChild("left_shoulder_tuft", CubeListBuilder.create()
                .texOffs(0, 130).mirror(true).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(135, 130).mirror(true).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(10, -30, 6, 0.8f, 0, 0.6f));
        PartDefinition backTuft = body.addOrReplaceChild("back_tuft", CubeListBuilder.create()
                .texOffs(0, 130).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(135, 130).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(0, -30, 8, 1.1f, 0, 0));
        PartDefinition rightBackTuft = body.addOrReplaceChild("right_back_tuft", CubeListBuilder.create()
                .texOffs(45, 130).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(217, 130).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(-6, -22, 10, 1.3f, 0, -0.3f));
        PartDefinition leftBackTuft = body.addOrReplaceChild("left_back_tuft", CubeListBuilder.create()
                .texOffs(45, 130).mirror(true).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(217, 130).mirror(true).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(6, -22, 10, 1.3f, 0, 0.3f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(183, 42).mirror(false).addBox(-7.5f, -7, -12, 15, 11, 12)
                .texOffs(88, 113).mirror(false).addBox(-8.5f, -2, -11, 17, 5, 9)
                .texOffs(141, 113).mirror(false).addBox(-5, -4, -17, 10, 6.5f, 6)
                .texOffs(161, 130).mirror(false).addBox(-3, -5.5f, -16, 6, 2, 5)
                .texOffs(17, 143).mirror(false).addBox(-2.5f, -5.2f, -17.6f, 5, 2.4f, 1.2f)
                .texOffs(184, 130).mirror(false).addBox(-4.2f, 2.5f, -16.4f, 1.6f, 5, 1.6f)
                .texOffs(184, 130).mirror(false).addBox(2.6f, 2.5f, -16.4f, 1.6f, 5, 1.6f)
                .texOffs(19, 0).mirror(false).addBox(-12, -14, -4, 24, 24, 8)
                .texOffs(80, 93).mirror(false).addBox(-7, 4.5f, -9, 14, 10, 8), PartPose.offsetAndRotation(0, -27, -7, 0.18f, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(226, 130).mirror(false).addBox(-8, -1, -1.5f, 8, 2.5f, 3), PartPose.offsetAndRotation(-0.5f, -6.5f, -12.5f, 0, 0, 0.32f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(226, 130).mirror(true).addBox(0, -1, -1.5f, 8, 2.5f, 3), PartPose.offsetAndRotation(0.5f, -6.5f, -12.5f, 0, 0, -0.32f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(62, 130).mirror(false).addBox(-4.5f, 0, -6, 9, 2.5f, 6)
                .texOffs(32, 143).mirror(false).addBox(-3.6f, -2.4f, -5.6f, 1.4f, 2.4f, 1.4f)
                .texOffs(32, 143).mirror(false).addBox(2.2f, -2.4f, -5.6f, 1.4f, 2.4f, 1.4f), PartPose.offsetAndRotation(0, 2.5f, -11, 0.42f, 0, 0));
        PartDefinition rightCheek = head.addOrReplaceChild("right_cheek", CubeListBuilder.create()
                .texOffs(0, 69).mirror(false).addBox(-4, -8, -4, 4, 15, 8), PartPose.offsetAndRotation(-8, 1, -6, 0, 0.4f, 0.2f));
        PartDefinition leftCheek = head.addOrReplaceChild("left_cheek", CubeListBuilder.create()
                .texOffs(0, 69).mirror(true).addBox(0, -8, -4, 4, 15, 8), PartPose.offsetAndRotation(8, 1, -6, 0, -0.4f, -0.2f));
        PartDefinition topTuft = head.addOrReplaceChild("top_tuft", CubeListBuilder.create()
                .texOffs(0, 130).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(135, 130).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(0, -13, 1, -0.85f, 0, 0));
        PartDefinition rightTuftA = head.addOrReplaceChild("right_tuft_a", CubeListBuilder.create()
                .texOffs(0, 130).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(135, 130).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(-6.5f, -12, 1, -0.75f, 0, -0.5f));
        PartDefinition leftTuftA = head.addOrReplaceChild("left_tuft_a", CubeListBuilder.create()
                .texOffs(0, 130).mirror(true).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(135, 130).mirror(true).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(6.5f, -12, 1, -0.75f, 0, 0.5f));
        PartDefinition rightTuftB = head.addOrReplaceChild("right_tuft_b", CubeListBuilder.create()
                .texOffs(0, 130).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(135, 130).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(-11, -6, 1, -0.7f, 0, -1.05f));
        PartDefinition leftTuftB = head.addOrReplaceChild("left_tuft_b", CubeListBuilder.create()
                .texOffs(0, 130).mirror(true).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(135, 130).mirror(true).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(11, -6, 1, -0.7f, 0, 1.05f));
        PartDefinition rightTuftC = head.addOrReplaceChild("right_tuft_c", CubeListBuilder.create()
                .texOffs(0, 130).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(135, 130).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(-11.5f, 2, 1, -0.6f, 0, -1.6f));
        PartDefinition leftTuftC = head.addOrReplaceChild("left_tuft_c", CubeListBuilder.create()
                .texOffs(0, 130).mirror(true).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(135, 130).mirror(true).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(11.5f, 2, 1, -0.6f, 0, 1.6f));
        PartDefinition rightTuftD = head.addOrReplaceChild("right_tuft_d", CubeListBuilder.create()
                .texOffs(45, 130).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(217, 130).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(-9, 9, 0, -0.4f, 0, -2.25f));
        PartDefinition leftTuftD = head.addOrReplaceChild("left_tuft_d", CubeListBuilder.create()
                .texOffs(45, 130).mirror(true).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(217, 130).mirror(true).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(9, 9, 0, -0.4f, 0, 2.25f));
        PartDefinition rightFlareA = head.addOrReplaceChild("right_flare_a", CubeListBuilder.create()
                .texOffs(45, 130).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(217, 130).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(-9, -10, -3, 0.35f, 0, -0.75f));
        PartDefinition leftFlareA = head.addOrReplaceChild("left_flare_a", CubeListBuilder.create()
                .texOffs(45, 130).mirror(true).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(217, 130).mirror(true).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(9, -10, -3, 0.35f, 0, 0.75f));
        PartDefinition rightFlareB = head.addOrReplaceChild("right_flare_b", CubeListBuilder.create()
                .texOffs(45, 130).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(217, 130).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(-11, 0, -3, 0.3f, 0, -1.4f));
        PartDefinition leftFlareB = head.addOrReplaceChild("left_flare_b", CubeListBuilder.create()
                .texOffs(45, 130).mirror(true).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(217, 130).mirror(true).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(11, 0, -3, 0.3f, 0, 1.4f));
        PartDefinition flareTop = head.addOrReplaceChild("flare_top", CubeListBuilder.create()
                .texOffs(45, 130).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(217, 130).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(0, -12, -3, 0.4f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(25, 69).mirror(false).addBox(-4.5f, -2, -4.5f, 9, 13, 9)
                .texOffs(143, 69).mirror(false).addBox(-7, -6, -7, 14, 7, 14)
                .texOffs(125, 93).mirror(false).addBox(-7.5f, -1, -6.5f, 8, 5, 13), PartPose.offsetAndRotation(-16.5f, -21, -0.5f, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(25, 69).mirror(true).addBox(-4.5f, -2, -4.5f, 9, 13, 9)
                .texOffs(143, 69).mirror(true).addBox(-7, -6, -7, 14, 7, 14)
                .texOffs(125, 93).mirror(true).addBox(-0.5f, -1, -6.5f, 8, 5, 13), PartPose.offsetAndRotation(16.5f, -21, -0.5f, 0, 0, 0));
        PartDefinition rightPauldronSpike = rightArm.addOrReplaceChild("right_pauldron_spike", CubeListBuilder.create()
                .texOffs(93, 130).mirror(false).addBox(-1.5f, -6, -1.5f, 3, 6, 3)
                .texOffs(41, 143).mirror(false).addBox(-0.75f, -9, -0.75f, 1.5f, 3, 1.5f), PartPose.offsetAndRotation(-3.5f, -6, -2.5f, -0.25f, 0, -0.55f));
        PartDefinition leftPauldronSpike = leftArm.addOrReplaceChild("left_pauldron_spike", CubeListBuilder.create()
                .texOffs(93, 130).mirror(true).addBox(-1.5f, -6, -1.5f, 3, 6, 3)
                .texOffs(41, 143).mirror(true).addBox(-0.75f, -9, -0.75f, 1.5f, 3, 1.5f), PartPose.offsetAndRotation(3.5f, -6, -2.5f, -0.25f, 0, 0.55f));
        PartDefinition rightPauldronSpike2 = rightArm.addOrReplaceChild("right_pauldron_spike2", CubeListBuilder.create()
                .texOffs(148, 130).mirror(false).addBox(-1.25f, -5, -1.25f, 2.5f, 5, 2.5f)
                .texOffs(41, 143).mirror(false).addBox(-0.6f, -7.5f, -0.6f, 1.2f, 2.5f, 1.2f), PartPose.offsetAndRotation(-3.5f, -6, 3, 0.25f, 0, -0.55f));
        PartDefinition leftPauldronSpike2 = leftArm.addOrReplaceChild("left_pauldron_spike2", CubeListBuilder.create()
                .texOffs(148, 130).mirror(true).addBox(-1.25f, -5, -1.25f, 2.5f, 5, 2.5f)
                .texOffs(41, 143).mirror(true).addBox(-0.6f, -7.5f, -0.6f, 1.2f, 2.5f, 1.2f), PartPose.offsetAndRotation(3.5f, -6, 3, 0.25f, 0, 0.55f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(0, 93).mirror(false).addBox(-5, 0, -5, 10, 9, 10)
                .texOffs(49, 113).mirror(false).addBox(-4.5f, 9, -4.8f, 9, 5, 9.6f)
                .texOffs(193, 130).mirror(false).addBox(-3.8f, 13, -5.4f, 1.5f, 4.5f, 1.5f)
                .texOffs(193, 130).mirror(false).addBox(-0.75f, 13, -5.4f, 1.5f, 4.5f, 1.5f)
                .texOffs(193, 130).mirror(false).addBox(2.3f, 13, -5.4f, 1.5f, 4.5f, 1.5f), PartPose.offsetAndRotation(0, 11, 0, -0.2f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(0, 93).mirror(true).addBox(-5, 0, -5, 10, 9, 10)
                .texOffs(49, 113).mirror(true).addBox(-4.5f, 9, -4.8f, 9, 5, 9.6f)
                .texOffs(193, 130).mirror(true).addBox(2.3f, 13, -5.4f, 1.5f, 4.5f, 1.5f)
                .texOffs(193, 130).mirror(true).addBox(-0.75f, 13, -5.4f, 1.5f, 4.5f, 1.5f)
                .texOffs(193, 130).mirror(true).addBox(-3.8f, 13, -5.4f, 1.5f, 4.5f, 1.5f), PartPose.offsetAndRotation(0, 11, 0, -0.2f, 0, 0));
        PartDefinition sword = rightForearm.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(21, 130).mirror(false).addBox(-1, -3, -1, 2, 10, 2)
                .texOffs(0, 143).mirror(false).addBox(-1.6f, 7, -1.6f, 3.2f, 2, 3.2f)
                .texOffs(106, 130).mirror(false).addBox(-3.5f, -4.5f, -3.5f, 7, 1.5f, 7)
                .texOffs(0, 0).mirror(false).addBox(-0.8f, -38, -3, 1.6f, 34, 7)
                .texOffs(30, 130).mirror(false).addBox(-0.8f, -44, -0.5f, 1.6f, 6, 4.5f), PartPose.offsetAndRotation(0, 11.5f, 0, 1.5708f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 42).mirror(false).addBox(-5, 0, -5, 10, 16, 10)
                .texOffs(200, 69).mirror(false).addBox(-5.5f, 7, -5.5f, 11, 9, 11)
                .texOffs(0, 113).mirror(false).addBox(-5.5f, 16, -8.5f, 11, 3, 12.5f)
                .texOffs(50, 143).mirror(false).addBox(-4.6f, 17, -10.2f, 1.7f, 2, 2)
                .texOffs(50, 143).mirror(false).addBox(-0.85f, 17, -10.2f, 1.7f, 2, 2)
                .texOffs(50, 143).mirror(false).addBox(2.9f, 17, -10.2f, 1.7f, 2, 2), PartPose.offsetAndRotation(-6.5f, 5, 0.5f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 42).mirror(true).addBox(-5, 0, -5, 10, 16, 10)
                .texOffs(200, 69).mirror(true).addBox(-5.5f, 7, -5.5f, 11, 9, 11)
                .texOffs(0, 113).mirror(true).addBox(-5.5f, 16, -8.5f, 11, 3, 12.5f)
                .texOffs(50, 143).mirror(true).addBox(2.9f, 17, -10.2f, 1.7f, 2, 2)
                .texOffs(50, 143).mirror(true).addBox(-0.85f, 17, -10.2f, 1.7f, 2, 2)
                .texOffs(50, 143).mirror(true).addBox(-4.6f, 17, -10.2f, 1.7f, 2, 2), PartPose.offsetAndRotation(6.5f, 5, 0.5f, 0, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(41, 93).mirror(false).addBox(-1.3f, -1.3f, 0, 2.6f, 2.6f, 16)
                .texOffs(174, 113).mirror(false).addBox(-3, -3, 14, 6, 6, 7), PartPose.offsetAndRotation(0, -2, 7, 0.75f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static LayerDefinition trueLayer() {
        // GENERATED true form: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-11, -11, -22, 22, 22, 20)
                .texOffs(0, 43).mirror(false).addBox(-8, -6, -3, 16, 12, 17)
                .texOffs(67, 43).mirror(false).addBox(-9, -5, 13, 18, 14, 13)
                .texOffs(85, 0).mirror(false).addBox(-12, -10, -27, 24, 21, 14)
                .texOffs(173, 43).mirror(false).addBox(-9.5f, -16, -25, 19, 7, 16)
                .texOffs(121, 73).mirror(false).addBox(-11.5f, -13, -12, 23, 11, 9)
                .texOffs(186, 73).mirror(false).addBox(-8, 12, -23, 16, 5, 13), PartPose.offsetAndRotation(0, 2, 0, 0, 0, 0));
        PartDefinition crestTuft = body.addOrReplaceChild("crest_tuft", CubeListBuilder.create()
                .texOffs(66, 116).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(0, -16, -22, -1.15f, 0, 0));
        PartDefinition crestTuft2 = body.addOrReplaceChild("crest_tuft2", CubeListBuilder.create()
                .texOffs(66, 116).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(0, -15, -14, -1.3f, 0, 0));
        PartDefinition crestTuft3 = body.addOrReplaceChild("crest_tuft3", CubeListBuilder.create()
                .texOffs(110, 116).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(0, -12, -6, -1.45f, 0, 0));
        PartDefinition rightCrestTuft = body.addOrReplaceChild("right_crest_tuft", CubeListBuilder.create()
                .texOffs(66, 116).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(-6.5f, -15, -18, -1.1f, 0, -0.45f));
        PartDefinition leftCrestTuft = body.addOrReplaceChild("left_crest_tuft", CubeListBuilder.create()
                .texOffs(66, 116).mirror(true).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(true).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(6.5f, -15, -18, -1.1f, 0, 0.45f));
        PartDefinition rightManeTuft = body.addOrReplaceChild("right_mane_tuft", CubeListBuilder.create()
                .texOffs(66, 116).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(-12, -6, -19, -0.9f, 0, -1.2f));
        PartDefinition leftManeTuft = body.addOrReplaceChild("left_mane_tuft", CubeListBuilder.create()
                .texOffs(66, 116).mirror(true).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(true).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(12, -6, -19, -0.9f, 0, 1.2f));
        PartDefinition rightManeTuft2 = body.addOrReplaceChild("right_mane_tuft2", CubeListBuilder.create()
                .texOffs(110, 116).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(-11.5f, -8, -8, -1.2f, 0, -1));
        PartDefinition leftManeTuft2 = body.addOrReplaceChild("left_mane_tuft2", CubeListBuilder.create()
                .texOffs(110, 116).mirror(true).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(true).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(11.5f, -8, -8, -1.2f, 0, 1));
        PartDefinition rightManeTuft3 = body.addOrReplaceChild("right_mane_tuft3", CubeListBuilder.create()
                .texOffs(110, 116).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(-12, 5, -17, -0.6f, 0, -1.75f));
        PartDefinition leftManeTuft3 = body.addOrReplaceChild("left_mane_tuft3", CubeListBuilder.create()
                .texOffs(110, 116).mirror(true).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(true).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(12, 5, -17, -0.6f, 0, 1.75f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 73).mirror(false).addBox(-7.5f, -7, -12, 15, 11, 12)
                .texOffs(156, 97).mirror(false).addBox(-8.5f, -2, -11, 17, 5, 9)
                .texOffs(209, 97).mirror(false).addBox(-5, -4, -17, 10, 6.5f, 6)
                .texOffs(171, 116).mirror(false).addBox(-3, -5.5f, -16, 6, 2, 5)
                .texOffs(235, 116).mirror(false).addBox(-2.5f, -5.2f, -17.6f, 5, 2.4f, 1.2f)
                .texOffs(194, 116).mirror(false).addBox(-4.2f, 2.5f, -16.4f, 1.6f, 5, 1.6f)
                .texOffs(194, 116).mirror(false).addBox(2.6f, 2.5f, -16.4f, 1.6f, 5, 1.6f)
                .texOffs(162, 0).mirror(false).addBox(-12, -14, -4, 24, 24, 8)
                .texOffs(0, 97).mirror(false).addBox(-7, 4.5f, -9, 14, 10, 8), PartPose.offsetAndRotation(0, 1, -26, 0.2f, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(212, 116).mirror(false).addBox(-8, -1, -1.5f, 8, 2.5f, 3), PartPose.offsetAndRotation(-0.5f, -6.5f, -12.5f, 0, 0, 0.32f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(212, 116).mirror(true).addBox(0, -1, -1.5f, 8, 2.5f, 3), PartPose.offsetAndRotation(0.5f, -6.5f, -12.5f, 0, 0, -0.32f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(127, 116).mirror(false).addBox(-4.5f, 0, -6, 9, 2.5f, 6)
                .texOffs(0, 130).mirror(false).addBox(-3.6f, -2.4f, -5.6f, 1.4f, 2.4f, 1.4f)
                .texOffs(0, 130).mirror(false).addBox(2.2f, -2.4f, -5.6f, 1.4f, 2.4f, 1.4f), PartPose.offsetAndRotation(0, 2.5f, -11, 0.42f, 0, 0));
        PartDefinition rightCheek = head.addOrReplaceChild("right_cheek", CubeListBuilder.create()
                .texOffs(55, 73).mirror(false).addBox(-4, -8, -4, 4, 15, 8), PartPose.offsetAndRotation(-8, 1, -6, 0, 0.4f, 0.2f));
        PartDefinition leftCheek = head.addOrReplaceChild("left_cheek", CubeListBuilder.create()
                .texOffs(55, 73).mirror(true).addBox(0, -8, -4, 4, 15, 8), PartPose.offsetAndRotation(8, 1, -6, 0, -0.4f, -0.2f));
        PartDefinition topTuft = head.addOrReplaceChild("top_tuft", CubeListBuilder.create()
                .texOffs(66, 116).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(0, -13, 1, -0.85f, 0, 0));
        PartDefinition rightTuftA = head.addOrReplaceChild("right_tuft_a", CubeListBuilder.create()
                .texOffs(66, 116).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(-6.5f, -12, 1, -0.75f, 0, -0.5f));
        PartDefinition leftTuftA = head.addOrReplaceChild("left_tuft_a", CubeListBuilder.create()
                .texOffs(66, 116).mirror(true).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(true).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(6.5f, -12, 1, -0.75f, 0, 0.5f));
        PartDefinition rightTuftB = head.addOrReplaceChild("right_tuft_b", CubeListBuilder.create()
                .texOffs(66, 116).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(-11, -6, 1, -0.7f, 0, -1.05f));
        PartDefinition leftTuftB = head.addOrReplaceChild("left_tuft_b", CubeListBuilder.create()
                .texOffs(66, 116).mirror(true).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(true).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(11, -6, 1, -0.7f, 0, 1.05f));
        PartDefinition rightTuftC = head.addOrReplaceChild("right_tuft_c", CubeListBuilder.create()
                .texOffs(66, 116).mirror(false).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(false).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(-11.5f, 2, 1, -0.6f, 0, -1.6f));
        PartDefinition leftTuftC = head.addOrReplaceChild("left_tuft_c", CubeListBuilder.create()
                .texOffs(66, 116).mirror(true).addBox(-2.5f, -7, -2.5f, 5, 7, 5)
                .texOffs(158, 116).mirror(true).addBox(-1.5f, -12, -1.5f, 3, 5, 3), PartPose.offsetAndRotation(11.5f, 2, 1, -0.6f, 0, 1.6f));
        PartDefinition rightTuftD = head.addOrReplaceChild("right_tuft_d", CubeListBuilder.create()
                .texOffs(110, 116).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(-9, 9, 0, -0.4f, 0, -2.25f));
        PartDefinition leftTuftD = head.addOrReplaceChild("left_tuft_d", CubeListBuilder.create()
                .texOffs(110, 116).mirror(true).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(true).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(9, 9, 0, -0.4f, 0, 2.25f));
        PartDefinition rightFlareA = head.addOrReplaceChild("right_flare_a", CubeListBuilder.create()
                .texOffs(110, 116).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(-9, -10, -3, 0.35f, 0, -0.75f));
        PartDefinition leftFlareA = head.addOrReplaceChild("left_flare_a", CubeListBuilder.create()
                .texOffs(110, 116).mirror(true).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(true).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(9, -10, -3, 0.35f, 0, 0.75f));
        PartDefinition rightFlareB = head.addOrReplaceChild("right_flare_b", CubeListBuilder.create()
                .texOffs(110, 116).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(-11, 0, -3, 0.3f, 0, -1.4f));
        PartDefinition leftFlareB = head.addOrReplaceChild("left_flare_b", CubeListBuilder.create()
                .texOffs(110, 116).mirror(true).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(true).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(11, 0, -3, 0.3f, 0, 1.4f));
        PartDefinition flareTop = head.addOrReplaceChild("flare_top", CubeListBuilder.create()
                .texOffs(110, 116).mirror(false).addBox(-2, -5, -2, 4, 5, 4)
                .texOffs(203, 116).mirror(false).addBox(-1, -9, -1, 2, 4, 2), PartPose.offsetAndRotation(0, -12, -3, 0.4f, 0, 0));
        PartDefinition rightForeleg = body.addOrReplaceChild("right_foreleg", CubeListBuilder.create()
                .texOffs(80, 73).mirror(false).addBox(-5, -3, -5, 10, 13, 10)
                .texOffs(80, 97).mirror(false).addBox(-5.5f, 10, -7.5f, 11, 4, 12)
                .texOffs(9, 130).mirror(false).addBox(-4.7f, 12, -9.2f, 1.7f, 2, 2)
                .texOffs(9, 130).mirror(false).addBox(-0.85f, 12, -9.2f, 1.7f, 2, 2)
                .texOffs(9, 130).mirror(false).addBox(3, 12, -9.2f, 1.7f, 2, 2), PartPose.offsetAndRotation(-7.5f, 8, -13, 0, 0, 0));
        PartDefinition leftForeleg = body.addOrReplaceChild("left_foreleg", CubeListBuilder.create()
                .texOffs(80, 73).mirror(true).addBox(-5, -3, -5, 10, 13, 10)
                .texOffs(80, 97).mirror(true).addBox(-5.5f, 10, -7.5f, 11, 4, 12)
                .texOffs(9, 130).mirror(true).addBox(3, 12, -9.2f, 1.7f, 2, 2)
                .texOffs(9, 130).mirror(true).addBox(-0.85f, 12, -9.2f, 1.7f, 2, 2)
                .texOffs(9, 130).mirror(true).addBox(-4.7f, 12, -9.2f, 1.7f, 2, 2), PartPose.offsetAndRotation(7.5f, 8, -13, 0, 0, 0));
        PartDefinition rightHindleg = body.addOrReplaceChild("right_hindleg", CubeListBuilder.create()
                .texOffs(130, 43).mirror(false).addBox(-4.5f, -4, -6, 9, 12, 12)
                .texOffs(127, 97).mirror(false).addBox(-3.5f, 8, -1, 7, 8, 7)
                .texOffs(0, 116).mirror(false).addBox(-4.5f, 15, -5, 9, 3, 10)
                .texOffs(9, 130).mirror(false).addBox(-3.6f, 16, -6.6f, 1.6f, 2, 2)
                .texOffs(9, 130).mirror(false).addBox(2, 16, -6.6f, 1.6f, 2, 2), PartPose.offsetAndRotation(-6.5f, 4, 20, 0, 0, 0));
        PartDefinition leftHindleg = body.addOrReplaceChild("left_hindleg", CubeListBuilder.create()
                .texOffs(130, 43).mirror(true).addBox(-4.5f, -4, -6, 9, 12, 12)
                .texOffs(127, 97).mirror(true).addBox(-3.5f, 8, -1, 7, 8, 7)
                .texOffs(0, 116).mirror(true).addBox(-4.5f, 15, -5, 9, 3, 10)
                .texOffs(9, 130).mirror(true).addBox(2, 16, -6.6f, 1.6f, 2, 2)
                .texOffs(9, 130).mirror(true).addBox(-3.6f, 16, -6.6f, 1.6f, 2, 2), PartPose.offsetAndRotation(6.5f, 4, 20, 0, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(45, 97).mirror(false).addBox(-1.3f, -1.3f, 0, 2.6f, 2.6f, 14), PartPose.offsetAndRotation(0, -3, 26, 0.9f, 0, 0));
        PartDefinition tailTip = tail.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(87, 116).mirror(false).addBox(-1.1f, -1.1f, 0, 2.2f, 2.2f, 8)
                .texOffs(39, 116).mirror(false).addBox(-3, -3, 6, 6, 6, 7), PartPose.offsetAndRotation(0, 0, 14, -1.2f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED true form: end
    }

    /** His demon-king shape. */
    public static final class Model extends EntityModel<AzureLionEntity> {
        private final ModelPart root, body, head, jaw, rightArm, leftArm, rightForearm, leftForearm, rightLeg, leftLeg, tail;

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            tail = body.getChild("tail");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(AzureLionEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = LionCourt.walk(rightLeg, leftLeg, swing, amount, 0.45f, 0.7f);
            // hunched, his breath heaving; his head low, turning to what he watches; a snarl
            body.xRot = 0.12f + Mth.sin(age * 0.06f) * 0.015f;
            head.xRot = 0.15f + pitch * Mth.DEG_TO_RAD * 0.5f;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.6f;
            head.zRot = 0;
            jaw.xRot = 0.15f + Mth.sin(age * 0.07f) * 0.05f;
            tail.xRot = 0.75f;
            tail.yRot = Mth.sin(age * 0.09f) * 0.35f;
            float blow = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.25f - stride * 0.35f - blow * 1.9f;
            rightArm.zRot = 0.18f;
            rightForearm.xRot = -0.35f - blow * 0.3f;
            leftArm.xRot = -0.1f + stride * 0.4f;
            leftArm.zRot = -0.22f;
            leftForearm.xRot = -0.3f;
            if (e.isRoaring()) {
                // head thrown back, jaws wide, arms flung out
                body.xRot = -0.15f;
                head.xRot = -0.55f;
                jaw.xRot = 0.95f;
                leftArm.xRot = -0.6f;
                leftArm.zRot = -0.9f;
                rightArm.zRot = 0.7f;
            }
            if (e.isGaping()) {
                // bent to his prey, jaws wide as a city gate, claws reaching
                body.xRot = 0.5f;
                head.xRot = 0.3f;
                jaw.xRot = 1.1f;
                rightArm.xRot = -0.9f;
                rightArm.zRot = 0.5f;
                leftArm.xRot = -1.1f;
                leftArm.zRot = -0.5f;
                leftForearm.xRot = -0.6f;
            }
            if (e.isReeling()) {
                body.xRot = -0.25f;
                head.xRot = 0.5f;
                head.zRot = Mth.sin(age * 0.5f) * 0.3f;
                jaw.xRot = 0.5f;
            }
            if (e.isHolding()) {
                // something in his belly: he hunches over it, a claw on it
                body.xRot = 0.25f + Mth.sin(age * 0.6f) * 0.05f;
                leftArm.xRot = -0.5f;
                leftArm.zRot = 0.45f;
                leftForearm.xRot = -1.2f;
            }
            if (e.isRevealing()) {
                // his true form tearing out of him: he rears and shudders, roaring, arms flung up and out
                float shudder = Mth.sin(age * 1.7f) * 0.06f;
                body.xRot = -0.3f + shudder;
                head.xRot = -0.75f;
                head.zRot = shudder * 2;
                jaw.xRot = 1.15f;
                rightArm.xRot = leftArm.xRot = -1.3f;
                rightArm.zRot = 1.2f;
                leftArm.zRot = -1.2f;
                rightForearm.xRot = leftForearm.xRot = -0.6f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    /** His true form: the lion. */
    public static final class TrueModel extends EntityModel<AzureLionEntity> {
        private final ModelPart root, body, head, jaw, rightFore, leftFore, rightHind, leftHind, tail;

        public TrueModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightFore = body.getChild("right_foreleg");
            leftFore = body.getChild("left_foreleg");
            rightHind = body.getChild("right_hindleg");
            leftHind = body.getChild("left_hindleg");
            tail = body.getChild("tail");
        }

        @Override
        public void setupAnim(AzureLionEntity e, float swing, float amount, float age, float yaw, float pitch) {
            // a heavy, prowling gait, the diagonal legs together
            float gait = Mth.cos(swing * 0.5f) * 0.8f * amount;
            rightFore.xRot = leftHind.xRot = gait;
            leftFore.xRot = rightHind.xRot = -gait;
            body.xRot = 0;
            body.zRot = 0;
            head.xRot = 0.2f + pitch * Mth.DEG_TO_RAD * 0.4f;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            head.zRot = 0;
            jaw.xRot = 0.12f + Mth.sin(age * 0.07f) * 0.05f;
            tail.xRot = 0.9f + Mth.sin(age * 0.05f) * 0.08f;
            tail.yRot = Mth.sin(age * 0.08f) * 0.4f;
            if (e.isRoaring()) {
                head.xRot = -0.45f;
                jaw.xRot = 0.95f;
            }
            if (e.isGaping()) {
                head.xRot = -0.05f;
                jaw.xRot = 1.05f;
            }
            if (e.isPouncing()) {
                // in the air, forelegs reaching, hind legs thrown back, jaws wide
                body.xRot = -0.3f;
                rightFore.xRot = leftFore.xRot = -1.3f;
                rightHind.xRot = leftHind.xRot = 0.9f;
                head.xRot = 0.05f;
                jaw.xRot = 1.0f;
            }
            if (e.isReeling()) {
                head.xRot = 0.5f;
                head.zRot = Mth.sin(age * 0.5f) * 0.3f;
                jaw.xRot = 0.5f;
            }
            if (e.isHolding()) {
                // something in his belly: head low, his flanks heaving
                body.xRot = 0.04f + Mth.sin(age * 0.6f) * 0.03f;
                head.xRot = 0.4f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
