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
 *   <li>His true form, Manjushri's lion turned demon, a guardian lion (狻猊) of dark azure: lean and
 *       long-legged, a deep chest over a drawn-in waist, the hind legs bent like a cat's, great clawed
 *       paws; a broad flat face under a scowling brow, eyes smouldering deep in it, a snarling snout,
 *       fangs and a lolling tongue; the stone lions' spiral curls ringing his face and bearding his
 *       jaw, locks like flames swept back off them, down his neck, along his spine and down his
 *       breast; a gold collar hung with bells; a long tail curling up into a flame. He gapes, roars,
 *       and pounces, forelegs reaching wide, hind legs thrown back, jaws open.</li>
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
                .texOffs(0, 0).mirror(false).addBox(-9, -7, -21, 18, 17, 17)
                .texOffs(27, 61).mirror(false).addBox(-7, 0, -23, 14, 11, 8)
                .texOffs(177, 0).mirror(false).addBox(-6.5f, -6, -5, 13, 10, 16)
                .texOffs(37, 35).mirror(false).addBox(-8, -5, 9, 16, 11, 11)
                .texOffs(116, 0).mirror(false).addBox(-10.5f, -11, -22, 21, 19, 9)
                .texOffs(183, 61).mirror(false).addBox(-10, -12, -15, 20, 9, 8)
                .texOffs(120, 100).mirror(false).addBox(-9.6f, 3, -23.6f, 19.2f, 2.5f, 9)
                .texOffs(56, 127).mirror(false).addBox(-2, 5, -25.4f, 4, 4.5f, 4)
                .texOffs(141, 138).mirror(false).addBox(-6.6f, 5, -24.4f, 2.6f, 3, 2.6f)
                .texOffs(141, 138).mirror(false).addBox(4, 5, -24.4f, 2.6f, 3, 2.6f)
                .texOffs(154, 138).mirror(false).addBox(-0.8f, 9.5f, -24.2f, 1.6f, 4, 1.6f)
                .texOffs(81, 115).mirror(false).addBox(-12.9f, -8.2f, -22.7f, 2.4f, 4.4f, 4.4f)
                .texOffs(81, 115).mirror(false).addBox(-12.9f, -4.416f, -21.292f, 2.4f, 4.4f, 4.4f)
                .texOffs(81, 115).mirror(false).addBox(-12.9f, -0.632f, -19.884f, 2.4f, 4.4f, 4.4f)
                .texOffs(81, 115).mirror(false).addBox(-12.9f, 3.152f, -18.476f, 2.4f, 4.4f, 4.4f)
                .texOffs(206, 127).mirror(false).addBox(-12.4f, -10, -16.5f, 2.4f, 4, 4)
                .texOffs(206, 127).mirror(false).addBox(-12.4f, -6.56f, -15.22f, 2.4f, 4, 4)
                .texOffs(206, 127).mirror(false).addBox(-12.4f, -3.12f, -13.94f, 2.4f, 4, 4)
                .texOffs(81, 115).mirror(false).addBox(10.5f, -8.2f, -22.7f, 2.4f, 4.4f, 4.4f)
                .texOffs(81, 115).mirror(false).addBox(10.5f, -4.416f, -21.292f, 2.4f, 4.4f, 4.4f)
                .texOffs(81, 115).mirror(false).addBox(10.5f, -0.632f, -19.884f, 2.4f, 4.4f, 4.4f)
                .texOffs(81, 115).mirror(false).addBox(10.5f, 3.152f, -18.476f, 2.4f, 4.4f, 4.4f)
                .texOffs(206, 127).mirror(false).addBox(10, -10, -16.5f, 2.4f, 4, 4)
                .texOffs(206, 127).mirror(false).addBox(10, -6.56f, -15.22f, 2.4f, 4, 4)
                .texOffs(206, 127).mirror(false).addBox(10, -3.12f, -13.94f, 2.4f, 4, 4), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rump = body.addOrReplaceChild("rump", CubeListBuilder.create()
                .texOffs(72, 61).mirror(false).addBox(-7.5f, 0, 0, 15, 7, 12), PartPose.offsetAndRotation(0, -7, 10, -0.4f, 0, 0));
        PartDefinition crestA = body.addOrReplaceChild("crest_a", CubeListBuilder.create()
                .texOffs(156, 61).mirror(false).addBox(-1.32f, -7.15f, -4.95f, 2.64f, 7.15f, 9.9f), PartPose.offsetAndRotation(0, -12.5f, -16, -1.42f, 0, 0));
        PartDefinition crestAMid = crestA.addOrReplaceChild("crest_a_mid", CubeListBuilder.create()
                .texOffs(98, 82).mirror(false).addBox(-1.122f, -6.05f, -4.257f, 2.244f, 6.05f, 6.534f), PartPose.offsetAndRotation(0, -6.27f, 0, 0.3f, 0, 0));
        PartDefinition crestATip = crestAMid.addOrReplaceChild("crest_a_tip", CubeListBuilder.create()
                .texOffs(73, 127).mirror(false).addBox(-0.924f, -4.95f, -3.069f, 1.848f, 4.95f, 3.366f), PartPose.offsetAndRotation(0, -5.28f, 0, 0.375f, 0, 0));
        PartDefinition crestB = body.addOrReplaceChild("crest_b", CubeListBuilder.create()
                .texOffs(27, 82).mirror(false).addBox(-1.2f, -6.5f, -4.5f, 2.4f, 6.5f, 9), PartPose.offsetAndRotation(0, -12, -9, -1.45f, 0, 0));
        PartDefinition crestBMid = crestB.addOrReplaceChild("crest_b_mid", CubeListBuilder.create()
                .texOffs(179, 100).mirror(false).addBox(-1.02f, -5.5f, -3.87f, 2.04f, 5.5f, 5.94f), PartPose.offsetAndRotation(0, -5.7f, 0, 0.3f, 0, 0));
        PartDefinition crestBTip = crestBMid.addOrReplaceChild("crest_b_tip", CubeListBuilder.create()
                .texOffs(73, 127).mirror(false).addBox(-0.84f, -4.5f, -2.79f, 1.68f, 4.5f, 3.06f), PartPose.offsetAndRotation(0, -4.8f, 0, 0.375f, 0, 0));
        PartDefinition crestC = body.addOrReplaceChild("crest_c", CubeListBuilder.create()
                .texOffs(119, 82).mirror(false).addBox(-0.96f, -5.2f, -3.6f, 1.92f, 5.2f, 7.2f), PartPose.offsetAndRotation(0, -9, -2, -1.5f, 0, 0));
        PartDefinition crestCMid = crestC.addOrReplaceChild("crest_c_mid", CubeListBuilder.create()
                .texOffs(98, 115).mirror(false).addBox(-0.816f, -4.4f, -3.096f, 1.632f, 4.4f, 4.752f), PartPose.offsetAndRotation(0, -4.56f, 0, 0.32f, 0, 0));
        PartDefinition crestCTip = crestCMid.addOrReplaceChild("crest_c_tip", CubeListBuilder.create()
                .texOffs(40, 138).mirror(false).addBox(-0.672f, -3.6f, -2.232f, 1.344f, 3.6f, 2.448f), PartPose.offsetAndRotation(0, -3.84f, 0, 0.4f, 0, 0));
        PartDefinition crestD = body.addOrReplaceChild("crest_d", CubeListBuilder.create()
                .texOffs(229, 100).mirror(false).addBox(-0.8f, -4.03f, -2.79f, 1.6f, 4.03f, 5.58f), PartPose.offsetAndRotation(0, -8, 5.5f, -1.52f, 0, 0));
        PartDefinition crestDMid = crestD.addOrReplaceChild("crest_d_mid", CubeListBuilder.create()
                .texOffs(221, 127).mirror(false).addBox(-0.68f, -3.41f, -2.3994f, 1.36f, 3.41f, 3.6828f), PartPose.offsetAndRotation(0, -3.534f, 0, 0.35f, 0, 0));
        PartDefinition crestDTip = crestDMid.addOrReplaceChild("crest_d_tip", CubeListBuilder.create()
                .texOffs(217, 138).mirror(false).addBox(-0.56f, -2.79f, -1.7298f, 1.12f, 2.79f, 1.8972f), PartPose.offsetAndRotation(0, -2.976f, 0, 0.4375f, 0, 0));
        PartDefinition rightNeckA = body.addOrReplaceChild("right_neck_a", CubeListBuilder.create()
                .texOffs(127, 61).mirror(false).addBox(-1.44f, -7.8f, -5.4f, 2.88f, 7.8f, 10.8f), PartPose.offsetAndRotation(-6.5f, -11.5f, -17.5f, -1.42f, 0, -0.5f));
        PartDefinition leftNeckA = body.addOrReplaceChild("left_neck_a", CubeListBuilder.create()
                .texOffs(127, 61).mirror(true).addBox(-1.44f, -7.8f, -5.4f, 2.88f, 7.8f, 10.8f), PartPose.offsetAndRotation(6.5f, -11.5f, -17.5f, -1.42f, 0, 0.5f));
        PartDefinition rightNeckAMid = rightNeckA.addOrReplaceChild("right_neck_a_mid", CubeListBuilder.create()
                .texOffs(75, 82).mirror(false).addBox(-1.224f, -6.6f, -4.644f, 2.448f, 6.6f, 7.128f), PartPose.offsetAndRotation(0, -6.84f, 0, 0.25f, -0.15f, 0));
        PartDefinition leftNeckAMid = leftNeckA.addOrReplaceChild("left_neck_a_mid", CubeListBuilder.create()
                .texOffs(75, 82).mirror(true).addBox(-1.224f, -6.6f, -4.644f, 2.448f, 6.6f, 7.128f), PartPose.offsetAndRotation(0, -6.84f, 0, 0.25f, 0.15f, 0));
        PartDefinition rightNeckATip = rightNeckAMid.addOrReplaceChild("right_neck_a_tip", CubeListBuilder.create()
                .texOffs(113, 115).mirror(false).addBox(-1.008f, -5.4f, -3.348f, 2.016f, 5.4f, 3.672f), PartPose.offsetAndRotation(0, -5.76f, 0, 0.3125f, -0.15f, 0));
        PartDefinition leftNeckATip = leftNeckAMid.addOrReplaceChild("left_neck_a_tip", CubeListBuilder.create()
                .texOffs(113, 115).mirror(true).addBox(-1.008f, -5.4f, -3.348f, 2.016f, 5.4f, 3.672f), PartPose.offsetAndRotation(0, -5.76f, 0, 0.3125f, 0.15f, 0));
        PartDefinition rightNeckB = body.addOrReplaceChild("right_neck_b", CubeListBuilder.create()
                .texOffs(0, 115).mirror(false).addBox(-5.4f, -7.8f, -1.44f, 10.8f, 7.8f, 2.88f), PartPose.offsetAndRotation(-10, -5, -17.5f, -1.45f, 0, -1));
        PartDefinition leftNeckB = body.addOrReplaceChild("left_neck_b", CubeListBuilder.create()
                .texOffs(0, 115).mirror(true).addBox(-5.4f, -7.8f, -1.44f, 10.8f, 7.8f, 2.88f), PartPose.offsetAndRotation(10, -5, -17.5f, -1.45f, 0, 1));
        PartDefinition rightNeckBMid = rightNeckB.addOrReplaceChild("right_neck_b_mid", CubeListBuilder.create()
                .texOffs(128, 115).mirror(false).addBox(-4.644f, -6.6f, -1.224f, 7.128f, 6.6f, 2.448f), PartPose.offsetAndRotation(0, -6.84f, 0, 0, 0, 0.22f));
        PartDefinition leftNeckBMid = leftNeckB.addOrReplaceChild("left_neck_b_mid", CubeListBuilder.create()
                .texOffs(128, 115).mirror(true).addBox(-2.484f, -6.6f, -1.224f, 7.128f, 6.6f, 2.448f), PartPose.offsetAndRotation(0, -6.84f, 0, 0, -0, -0.22f));
        PartDefinition rightNeckBTip = rightNeckBMid.addOrReplaceChild("right_neck_b_tip", CubeListBuilder.create()
                .texOffs(86, 127).mirror(false).addBox(-3.348f, -5.4f, -1.008f, 3.672f, 5.4f, 2.016f), PartPose.offsetAndRotation(0, -5.76f, 0, 0, 0, 0.275f));
        PartDefinition leftNeckBTip = leftNeckBMid.addOrReplaceChild("left_neck_b_tip", CubeListBuilder.create()
                .texOffs(86, 127).mirror(true).addBox(-0.324f, -5.4f, -1.008f, 3.672f, 5.4f, 2.016f), PartPose.offsetAndRotation(0, -5.76f, 0, 0, -0, -0.275f));
        PartDefinition rightNeckC = body.addOrReplaceChild("right_neck_c", CubeListBuilder.create()
                .texOffs(29, 115).mirror(false).addBox(-4.95f, -7.15f, -1.32f, 9.9f, 7.15f, 2.64f), PartPose.offsetAndRotation(-10.5f, 2.5f, -18.5f, -1.5f, 0, -1.6f));
        PartDefinition leftNeckC = body.addOrReplaceChild("left_neck_c", CubeListBuilder.create()
                .texOffs(29, 115).mirror(true).addBox(-4.95f, -7.15f, -1.32f, 9.9f, 7.15f, 2.64f), PartPose.offsetAndRotation(10.5f, 2.5f, -18.5f, -1.5f, 0, 1.6f));
        PartDefinition rightNeckCMid = rightNeckC.addOrReplaceChild("right_neck_c_mid", CubeListBuilder.create()
                .texOffs(151, 115).mirror(false).addBox(-4.257f, -6.05f, -1.122f, 6.534f, 6.05f, 2.244f), PartPose.offsetAndRotation(0, -6.27f, 0, 0, 0, 0.22f));
        PartDefinition leftNeckCMid = leftNeckC.addOrReplaceChild("left_neck_c_mid", CubeListBuilder.create()
                .texOffs(151, 115).mirror(true).addBox(-2.277f, -6.05f, -1.122f, 6.534f, 6.05f, 2.244f), PartPose.offsetAndRotation(0, -6.27f, 0, 0, -0, -0.22f));
        PartDefinition rightNeckCTip = rightNeckCMid.addOrReplaceChild("right_neck_c_tip", CubeListBuilder.create()
                .texOffs(51, 138).mirror(false).addBox(-3.069f, -4.95f, -0.924f, 3.366f, 4.95f, 1.848f), PartPose.offsetAndRotation(0, -5.28f, 0, 0, 0, 0.275f));
        PartDefinition leftNeckCTip = leftNeckCMid.addOrReplaceChild("left_neck_c_tip", CubeListBuilder.create()
                .texOffs(51, 138).mirror(true).addBox(-0.297f, -4.95f, -0.924f, 3.366f, 4.95f, 1.848f), PartPose.offsetAndRotation(0, -5.28f, 0, 0, -0, -0.275f));
        PartDefinition rightNeckD = body.addOrReplaceChild("right_neck_d", CubeListBuilder.create()
                .texOffs(27, 82).mirror(false).addBox(-1.2f, -6.5f, -4.5f, 2.4f, 6.5f, 9), PartPose.offsetAndRotation(-6, -11, -10, -1.5f, 0, -0.45f));
        PartDefinition leftNeckD = body.addOrReplaceChild("left_neck_d", CubeListBuilder.create()
                .texOffs(27, 82).mirror(true).addBox(-1.2f, -6.5f, -4.5f, 2.4f, 6.5f, 9), PartPose.offsetAndRotation(6, -11, -10, -1.5f, 0, 0.45f));
        PartDefinition rightNeckDMid = rightNeckD.addOrReplaceChild("right_neck_d_mid", CubeListBuilder.create()
                .texOffs(179, 100).mirror(false).addBox(-1.02f, -5.5f, -3.87f, 2.04f, 5.5f, 5.94f), PartPose.offsetAndRotation(0, -5.7f, 0, 0.28f, -0.1f, 0));
        PartDefinition leftNeckDMid = leftNeckD.addOrReplaceChild("left_neck_d_mid", CubeListBuilder.create()
                .texOffs(179, 100).mirror(true).addBox(-1.02f, -5.5f, -3.87f, 2.04f, 5.5f, 5.94f), PartPose.offsetAndRotation(0, -5.7f, 0, 0.28f, 0.1f, 0));
        PartDefinition rightNeckDTip = rightNeckDMid.addOrReplaceChild("right_neck_d_tip", CubeListBuilder.create()
                .texOffs(73, 127).mirror(false).addBox(-0.84f, -4.5f, -2.79f, 1.68f, 4.5f, 3.06f), PartPose.offsetAndRotation(0, -4.8f, 0, 0.35f, -0.1f, 0));
        PartDefinition leftNeckDTip = leftNeckDMid.addOrReplaceChild("left_neck_d_tip", CubeListBuilder.create()
                .texOffs(73, 127).mirror(true).addBox(-0.84f, -4.5f, -2.79f, 1.68f, 4.5f, 3.06f), PartPose.offsetAndRotation(0, -4.8f, 0, 0.35f, 0.1f, 0));
        PartDefinition rightNeckE = body.addOrReplaceChild("right_neck_e", CubeListBuilder.create()
                .texOffs(172, 115).mirror(false).addBox(-4.5f, -6.5f, -1.2f, 9, 6.5f, 2.4f), PartPose.offsetAndRotation(-9.5f, -4.5f, -10, -1.55f, 0, -1.05f));
        PartDefinition leftNeckE = body.addOrReplaceChild("left_neck_e", CubeListBuilder.create()
                .texOffs(172, 115).mirror(true).addBox(-4.5f, -6.5f, -1.2f, 9, 6.5f, 2.4f), PartPose.offsetAndRotation(9.5f, -4.5f, -10, -1.55f, 0, 1.05f));
        PartDefinition rightNeckEMid = rightNeckE.addOrReplaceChild("right_neck_e_mid", CubeListBuilder.create()
                .texOffs(101, 127).mirror(false).addBox(-3.87f, -5.5f, -1.02f, 5.94f, 5.5f, 2.04f), PartPose.offsetAndRotation(0, -5.7f, 0, 0, 0, 0.25f));
        PartDefinition leftNeckEMid = leftNeckE.addOrReplaceChild("left_neck_e_mid", CubeListBuilder.create()
                .texOffs(101, 127).mirror(true).addBox(-2.07f, -5.5f, -1.02f, 5.94f, 5.5f, 2.04f), PartPose.offsetAndRotation(0, -5.7f, 0, 0, -0, -0.25f));
        PartDefinition rightNeckETip = rightNeckEMid.addOrReplaceChild("right_neck_e_tip", CubeListBuilder.create()
                .texOffs(51, 138).mirror(false).addBox(-2.79f, -4.5f, -0.84f, 3.06f, 4.5f, 1.68f), PartPose.offsetAndRotation(0, -4.8f, 0, 0, 0, 0.3125f));
        PartDefinition leftNeckETip = leftNeckEMid.addOrReplaceChild("left_neck_e_tip", CubeListBuilder.create()
                .texOffs(51, 138).mirror(true).addBox(-0.27f, -4.5f, -0.84f, 3.06f, 4.5f, 1.68f), PartPose.offsetAndRotation(0, -4.8f, 0, 0, -0, -0.3125f));
        PartDefinition rightNeckF = body.addOrReplaceChild("right_neck_f", CubeListBuilder.create()
                .texOffs(120, 127).mirror(false).addBox(-3.825f, -5.525f, -1.02f, 7.65f, 5.525f, 2.04f), PartPose.offsetAndRotation(-9.5f, 2, -11, -1.6f, 0, -1.65f));
        PartDefinition leftNeckF = body.addOrReplaceChild("left_neck_f", CubeListBuilder.create()
                .texOffs(120, 127).mirror(true).addBox(-3.825f, -5.525f, -1.02f, 7.65f, 5.525f, 2.04f), PartPose.offsetAndRotation(9.5f, 2, -11, -1.6f, 0, 1.65f));
        PartDefinition rightNeckFMid = rightNeckF.addOrReplaceChild("right_neck_f_mid", CubeListBuilder.create()
                .texOffs(64, 138).mirror(false).addBox(-3.2895f, -4.675f, -0.867f, 5.049f, 4.675f, 1.734f), PartPose.offsetAndRotation(0, -4.845f, 0, 0, 0, 0.28f));
        PartDefinition leftNeckFMid = leftNeckF.addOrReplaceChild("left_neck_f_mid", CubeListBuilder.create()
                .texOffs(64, 138).mirror(true).addBox(-1.7595f, -4.675f, -0.867f, 5.049f, 4.675f, 1.734f), PartPose.offsetAndRotation(0, -4.845f, 0, 0, -0, -0.28f));
        PartDefinition rightNeckFTip = rightNeckFMid.addOrReplaceChild("right_neck_f_tip", CubeListBuilder.create()
                .texOffs(163, 138).mirror(false).addBox(-2.3715f, -3.825f, -0.714f, 2.601f, 3.825f, 1.428f), PartPose.offsetAndRotation(0, -4.08f, 0, 0, 0, 0.35f));
        PartDefinition leftNeckFTip = leftNeckFMid.addOrReplaceChild("left_neck_f_tip", CubeListBuilder.create()
                .texOffs(163, 138).mirror(true).addBox(-0.2295f, -3.825f, -0.714f, 2.601f, 3.825f, 1.428f), PartPose.offsetAndRotation(0, -4.08f, 0, 0, -0, -0.35f));
        PartDefinition rightShoulderA = body.addOrReplaceChild("right_shoulder_a", CubeListBuilder.create()
                .texOffs(120, 127).mirror(false).addBox(-3.825f, -5.525f, -1.02f, 7.65f, 5.525f, 2.04f), PartPose.offsetAndRotation(-9, 7, -15.5f, -1.85f, 0, -1.45f));
        PartDefinition leftShoulderA = body.addOrReplaceChild("left_shoulder_a", CubeListBuilder.create()
                .texOffs(120, 127).mirror(true).addBox(-3.825f, -5.525f, -1.02f, 7.65f, 5.525f, 2.04f), PartPose.offsetAndRotation(9, 7, -15.5f, -1.85f, 0, 1.45f));
        PartDefinition rightShoulderAMid = rightShoulderA.addOrReplaceChild("right_shoulder_a_mid", CubeListBuilder.create()
                .texOffs(64, 138).mirror(false).addBox(-3.2895f, -4.675f, -0.867f, 5.049f, 4.675f, 1.734f), PartPose.offsetAndRotation(0, -4.845f, 0, 0, 0, 0.3f));
        PartDefinition leftShoulderAMid = leftShoulderA.addOrReplaceChild("left_shoulder_a_mid", CubeListBuilder.create()
                .texOffs(64, 138).mirror(true).addBox(-1.7595f, -4.675f, -0.867f, 5.049f, 4.675f, 1.734f), PartPose.offsetAndRotation(0, -4.845f, 0, 0, -0, -0.3f));
        PartDefinition rightShoulderATip = rightShoulderAMid.addOrReplaceChild("right_shoulder_a_tip", CubeListBuilder.create()
                .texOffs(163, 138).mirror(false).addBox(-2.3715f, -3.825f, -0.714f, 2.601f, 3.825f, 1.428f), PartPose.offsetAndRotation(0, -4.08f, 0, 0, 0, 0.375f));
        PartDefinition leftShoulderATip = leftShoulderAMid.addOrReplaceChild("left_shoulder_a_tip", CubeListBuilder.create()
                .texOffs(163, 138).mirror(true).addBox(-0.2295f, -3.825f, -0.714f, 2.601f, 3.825f, 1.428f), PartPose.offsetAndRotation(0, -4.08f, 0, 0, -0, -0.375f));
        PartDefinition rightShoulderB = body.addOrReplaceChild("right_shoulder_b", CubeListBuilder.create()
                .texOffs(81, 138).mirror(false).addBox(-3.375f, -4.875f, -0.9f, 6.75f, 4.875f, 1.8f), PartPose.offsetAndRotation(-8.5f, -6, -3, -1.7f, 0, -0.9f));
        PartDefinition leftShoulderB = body.addOrReplaceChild("left_shoulder_b", CubeListBuilder.create()
                .texOffs(81, 138).mirror(true).addBox(-3.375f, -4.875f, -0.9f, 6.75f, 4.875f, 1.8f), PartPose.offsetAndRotation(8.5f, -6, -3, -1.7f, 0, 0.9f));
        PartDefinition rightShoulderBMid = rightShoulderB.addOrReplaceChild("right_shoulder_b_mid", CubeListBuilder.create()
                .texOffs(100, 138).mirror(false).addBox(-2.9025f, -4.125f, -0.765f, 4.455f, 4.125f, 1.53f), PartPose.offsetAndRotation(0, -4.275f, 0, 0, 0, 0.32f));
        PartDefinition leftShoulderBMid = leftShoulderB.addOrReplaceChild("left_shoulder_b_mid", CubeListBuilder.create()
                .texOffs(100, 138).mirror(true).addBox(-1.5525f, -4.125f, -0.765f, 4.455f, 4.125f, 1.53f), PartPose.offsetAndRotation(0, -4.275f, 0, 0, -0, -0.32f));
        PartDefinition rightShoulderBTip = rightShoulderBMid.addOrReplaceChild("right_shoulder_b_tip", CubeListBuilder.create()
                .texOffs(163, 138).mirror(false).addBox(-2.0925f, -3.375f, -0.63f, 2.295f, 3.375f, 1.26f), PartPose.offsetAndRotation(0, -3.6f, 0, 0, 0, 0.4f));
        PartDefinition leftShoulderBTip = leftShoulderBMid.addOrReplaceChild("left_shoulder_b_tip", CubeListBuilder.create()
                .texOffs(163, 138).mirror(true).addBox(-0.2025f, -3.375f, -0.63f, 2.295f, 3.375f, 1.26f), PartPose.offsetAndRotation(0, -3.6f, 0, 0, -0, -0.4f));
        PartDefinition apron = body.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(234, 127).mirror(false).addBox(-3.6f, -5.2f, -0.96f, 7.2f, 5.2f, 1.92f), PartPose.offsetAndRotation(0, 7, -22, -2.75f, 0, 0));
        PartDefinition apronMid = apron.addOrReplaceChild("apron_mid", CubeListBuilder.create()
                .texOffs(100, 138).mirror(false).addBox(-3.096f, -4.4f, -0.816f, 4.752f, 4.4f, 1.632f), PartPose.offsetAndRotation(0, -4.56f, 0, -0.15f, 0, 0));
        PartDefinition apronTip = apronMid.addOrReplaceChild("apron_tip", CubeListBuilder.create()
                .texOffs(163, 138).mirror(false).addBox(-2.232f, -3.6f, -0.672f, 2.448f, 3.6f, 1.344f), PartPose.offsetAndRotation(0, -3.84f, 0, -0.1875f, 0, 0));
        PartDefinition rightApron = body.addOrReplaceChild("right_apron", CubeListBuilder.create()
                .texOffs(234, 127).mirror(false).addBox(-3.6f, -5.2f, -0.96f, 7.2f, 5.2f, 1.92f), PartPose.offsetAndRotation(-4.5f, 6, -21.5f, -2.8f, 0, 0.3f));
        PartDefinition leftApron = body.addOrReplaceChild("left_apron", CubeListBuilder.create()
                .texOffs(234, 127).mirror(true).addBox(-3.6f, -5.2f, -0.96f, 7.2f, 5.2f, 1.92f), PartPose.offsetAndRotation(4.5f, 6, -21.5f, -2.8f, 0, -0.3f));
        PartDefinition rightApronMid = rightApron.addOrReplaceChild("right_apron_mid", CubeListBuilder.create()
                .texOffs(100, 138).mirror(false).addBox(-3.096f, -4.4f, -0.816f, 4.752f, 4.4f, 1.632f), PartPose.offsetAndRotation(0, -4.56f, 0, -0.15f, 0, 0));
        PartDefinition leftApronMid = leftApron.addOrReplaceChild("left_apron_mid", CubeListBuilder.create()
                .texOffs(100, 138).mirror(true).addBox(-1.656f, -4.4f, -0.816f, 4.752f, 4.4f, 1.632f), PartPose.offsetAndRotation(0, -4.56f, 0, -0.15f, -0, 0));
        PartDefinition rightApronTip = rightApronMid.addOrReplaceChild("right_apron_tip", CubeListBuilder.create()
                .texOffs(163, 138).mirror(false).addBox(-2.232f, -3.6f, -0.672f, 2.448f, 3.6f, 1.344f), PartPose.offsetAndRotation(0, -3.84f, 0, -0.1875f, 0, 0));
        PartDefinition leftApronTip = leftApronMid.addOrReplaceChild("left_apron_tip", CubeListBuilder.create()
                .texOffs(163, 138).mirror(true).addBox(-0.216f, -3.6f, -0.672f, 2.448f, 3.6f, 1.344f), PartPose.offsetAndRotation(0, -3.84f, 0, -0.1875f, -0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(92, 35).mirror(false).addBox(-7, -6, -11, 14, 11, 11)
                .texOffs(29, 100).mirror(false).addBox(-6, -9, -10, 12, 4, 9)
                .texOffs(140, 82).mirror(false).addBox(-8.5f, -1, -10, 17, 6, 8)
                .texOffs(197, 115).mirror(false).addBox(-3, -4.2f, -16, 6, 3.6f, 5.5f)
                .texOffs(56, 115).mirror(false).addBox(-5.8f, -0.8f, -16.5f, 5.6f, 4.8f, 6)
                .texOffs(56, 115).mirror(false).addBox(0.2f, -0.8f, -16.5f, 5.6f, 4.8f, 6)
                .texOffs(222, 115).mirror(false).addBox(-1, -0.8f, -16, 2, 4.4f, 5)
                .texOffs(226, 138).mirror(false).addBox(-2.8f, -3.4f, -17, 5.6f, 2.8f, 1.4f)
                .texOffs(174, 138).mirror(false).addBox(-5.6f, 3.2f, -16, 1.7f, 3.6f, 1.7f)
                .texOffs(174, 138).mirror(false).addBox(3.9f, 3.2f, -16, 1.7f, 3.6f, 1.7f)
                .texOffs(243, 138).mirror(false).addBox(-5.3f, 6.6f, -15.8f, 1.1f, 3, 1.1f)
                .texOffs(243, 138).mirror(false).addBox(4.2f, 6.6f, -15.8f, 1.1f, 3, 1.1f)
                .texOffs(9, 147).mirror(false).addBox(-3.7f, 3.6f, -16.3f, 7.4f, 1.4f, 0.8f)
                .texOffs(0, 127).mirror(false).addBox(-3.6f, 1, -11, 7.2f, 6, 4)
                .texOffs(180, 35).mirror(false).addBox(-11, -9, -4, 22, 14, 7)
                .texOffs(71, 0).mirror(false).addBox(-7.5f, -13, -4, 15, 22, 7)
                .texOffs(143, 127).mirror(false).addBox(-5.6f, -14.7f, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(143, 127).mirror(false).addBox(0.4f, -14.7f, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(0, 138).mirror(false).addBox(-10.3f, -11.5f, -5.8f, 4.4f, 4.4f, 3)
                .texOffs(0, 138).mirror(false).addBox(5.9f, -11.5f, -5.8f, 4.4f, 4.4f, 3)
                .texOffs(143, 127).mirror(false).addBox(-13.7f, -7, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(143, 127).mirror(false).addBox(8.5f, -7, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(0, 138).mirror(false).addBox(-13.3f, -0.8f, -5.8f, 4.4f, 4.4f, 3)
                .texOffs(0, 138).mirror(false).addBox(8.9f, -0.8f, -5.8f, 4.4f, 4.4f, 3)
                .texOffs(143, 127).mirror(false).addBox(-10.7f, 3.7f, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(143, 127).mirror(false).addBox(5.5f, 3.7f, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(0, 138).mirror(false).addBox(-5.2f, 6.9f, -5.8f, 4.4f, 4.4f, 3)
                .texOffs(0, 138).mirror(false).addBox(0.8f, 6.9f, -5.8f, 4.4f, 4.4f, 3), PartPose.offsetAndRotation(0, -6, -21, 0.1f, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(183, 138).mirror(false).addBox(-7.5f, -1, -1.5f, 7.5f, 2.5f, 3), PartPose.offsetAndRotation(-0.5f, -5.5f, -11.5f, 0, 0, 0.35f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(183, 138).mirror(true).addBox(0, -1, -1.5f, 7.5f, 2.5f, 3), PartPose.offsetAndRotation(0.5f, -5.5f, -11.5f, 0, 0, -0.35f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(25, 127).mirror(false).addBox(-3.8f, 0, -6.5f, 7.6f, 2.5f, 6.5f)
                .texOffs(243, 138).mirror(false).addBox(-3.6f, -2.6f, -6.2f, 1.5f, 2.6f, 1.5f)
                .texOffs(243, 138).mirror(false).addBox(2.1f, -2.6f, -6.2f, 1.5f, 2.6f, 1.5f)
                .texOffs(0, 138).mirror(false).addBox(-7.5f, 2, -7, 5, 5, 3)
                .texOffs(0, 138).mirror(false).addBox(-2.5f, 2.5f, -7.5f, 5, 5, 3)
                .texOffs(0, 138).mirror(false).addBox(2.5f, 2, -7, 5, 5, 3), PartPose.offsetAndRotation(0, 3.5f, -10, 0.25f, 0, 0));
        PartDefinition tongue = jaw.addOrReplaceChild("tongue", CubeListBuilder.create()
                .texOffs(17, 138).mirror(false).addBox(-2, -0.5f, -5.8f, 4, 1, 6.8f), PartPose.offsetAndRotation(0, 0.3f, -3, 0.35f, 0, 0));
        PartDefinition topLock = head.addOrReplaceChild("top_lock", CubeListBuilder.create()
                .texOffs(156, 61).mirror(false).addBox(-1.32f, -7.15f, -4.95f, 2.64f, 7.15f, 9.9f), PartPose.offsetAndRotation(0, -12.5f, 0.5f, -1.3f, 0, 0));
        PartDefinition topLockMid = topLock.addOrReplaceChild("top_lock_mid", CubeListBuilder.create()
                .texOffs(98, 82).mirror(false).addBox(-1.122f, -6.05f, -4.257f, 2.244f, 6.05f, 6.534f), PartPose.offsetAndRotation(0, -6.27f, 0, 0.3f, 0, 0));
        PartDefinition topLockTip = topLockMid.addOrReplaceChild("top_lock_tip", CubeListBuilder.create()
                .texOffs(73, 127).mirror(false).addBox(-0.924f, -4.95f, -3.069f, 1.848f, 4.95f, 3.366f), PartPose.offsetAndRotation(0, -5.28f, 0, 0.375f, 0, 0));
        PartDefinition rightLockA = head.addOrReplaceChild("right_lock_a", CubeListBuilder.create()
                .texOffs(0, 82).mirror(false).addBox(-1.26f, -6.825f, -4.725f, 2.52f, 6.825f, 9.45f), PartPose.offsetAndRotation(-5.5f, -11.5f, 0, -1.25f, 0, -0.42f));
        PartDefinition leftLockA = head.addOrReplaceChild("left_lock_a", CubeListBuilder.create()
                .texOffs(0, 82).mirror(true).addBox(-1.26f, -6.825f, -4.725f, 2.52f, 6.825f, 9.45f), PartPose.offsetAndRotation(5.5f, -11.5f, 0, -1.25f, 0, 0.42f));
        PartDefinition rightLockAMid = rightLockA.addOrReplaceChild("right_lock_a_mid", CubeListBuilder.create()
                .texOffs(72, 100).mirror(false).addBox(-1.071f, -5.775f, -4.0635f, 2.142f, 5.775f, 6.237f), PartPose.offsetAndRotation(0, -5.985f, 0, 0.28f, -0.15f, 0));
        PartDefinition leftLockAMid = leftLockA.addOrReplaceChild("left_lock_a_mid", CubeListBuilder.create()
                .texOffs(72, 100).mirror(true).addBox(-1.071f, -5.775f, -4.0635f, 2.142f, 5.775f, 6.237f), PartPose.offsetAndRotation(0, -5.985f, 0, 0.28f, 0.15f, 0));
        PartDefinition rightLockATip = rightLockAMid.addOrReplaceChild("right_lock_a_tip", CubeListBuilder.create()
                .texOffs(73, 127).mirror(false).addBox(-0.882f, -4.725f, -2.9295f, 1.764f, 4.725f, 3.213f), PartPose.offsetAndRotation(0, -5.04f, 0, 0.35f, -0.15f, 0));
        PartDefinition leftLockATip = leftLockAMid.addOrReplaceChild("left_lock_a_tip", CubeListBuilder.create()
                .texOffs(73, 127).mirror(true).addBox(-0.882f, -4.725f, -2.9295f, 1.764f, 4.725f, 3.213f), PartPose.offsetAndRotation(0, -5.04f, 0, 0.35f, 0.15f, 0));
        PartDefinition rightLockB = head.addOrReplaceChild("right_lock_b", CubeListBuilder.create()
                .texOffs(0, 82).mirror(false).addBox(-1.26f, -6.825f, -4.725f, 2.52f, 6.825f, 9.45f), PartPose.offsetAndRotation(-10, -5.5f, 0, -1.15f, 0, -0.85f));
        PartDefinition leftLockB = head.addOrReplaceChild("left_lock_b", CubeListBuilder.create()
                .texOffs(0, 82).mirror(true).addBox(-1.26f, -6.825f, -4.725f, 2.52f, 6.825f, 9.45f), PartPose.offsetAndRotation(10, -5.5f, 0, -1.15f, 0, 0.85f));
        PartDefinition rightLockBMid = rightLockB.addOrReplaceChild("right_lock_b_mid", CubeListBuilder.create()
                .texOffs(72, 100).mirror(false).addBox(-1.071f, -5.775f, -4.0635f, 2.142f, 5.775f, 6.237f), PartPose.offsetAndRotation(0, -5.985f, 0, 0.26f, -0.2f, 0));
        PartDefinition leftLockBMid = leftLockB.addOrReplaceChild("left_lock_b_mid", CubeListBuilder.create()
                .texOffs(72, 100).mirror(true).addBox(-1.071f, -5.775f, -4.0635f, 2.142f, 5.775f, 6.237f), PartPose.offsetAndRotation(0, -5.985f, 0, 0.26f, 0.2f, 0));
        PartDefinition rightLockBTip = rightLockBMid.addOrReplaceChild("right_lock_b_tip", CubeListBuilder.create()
                .texOffs(73, 127).mirror(false).addBox(-0.882f, -4.725f, -2.9295f, 1.764f, 4.725f, 3.213f), PartPose.offsetAndRotation(0, -5.04f, 0, 0.325f, -0.2f, 0));
        PartDefinition leftLockBTip = leftLockBMid.addOrReplaceChild("left_lock_b_tip", CubeListBuilder.create()
                .texOffs(73, 127).mirror(true).addBox(-0.882f, -4.725f, -2.9295f, 1.764f, 4.725f, 3.213f), PartPose.offsetAndRotation(0, -5.04f, 0, 0.325f, 0.2f, 0));
        PartDefinition rightLockC = head.addOrReplaceChild("right_lock_c", CubeListBuilder.create()
                .texOffs(172, 115).mirror(false).addBox(-4.5f, -6.5f, -1.2f, 9, 6.5f, 2.4f), PartPose.offsetAndRotation(-11.5f, 1.5f, 0, -1.05f, 0, -1.45f));
        PartDefinition leftLockC = head.addOrReplaceChild("left_lock_c", CubeListBuilder.create()
                .texOffs(172, 115).mirror(true).addBox(-4.5f, -6.5f, -1.2f, 9, 6.5f, 2.4f), PartPose.offsetAndRotation(11.5f, 1.5f, 0, -1.05f, 0, 1.45f));
        PartDefinition rightLockCMid = rightLockC.addOrReplaceChild("right_lock_c_mid", CubeListBuilder.create()
                .texOffs(101, 127).mirror(false).addBox(-3.87f, -5.5f, -1.02f, 5.94f, 5.5f, 2.04f), PartPose.offsetAndRotation(0, -5.7f, 0, 0, 0, 0.25f));
        PartDefinition leftLockCMid = leftLockC.addOrReplaceChild("left_lock_c_mid", CubeListBuilder.create()
                .texOffs(101, 127).mirror(true).addBox(-2.07f, -5.5f, -1.02f, 5.94f, 5.5f, 2.04f), PartPose.offsetAndRotation(0, -5.7f, 0, 0, -0, -0.25f));
        PartDefinition rightLockCTip = rightLockCMid.addOrReplaceChild("right_lock_c_tip", CubeListBuilder.create()
                .texOffs(51, 138).mirror(false).addBox(-2.79f, -4.5f, -0.84f, 3.06f, 4.5f, 1.68f), PartPose.offsetAndRotation(0, -4.8f, 0, 0, 0, 0.3125f));
        PartDefinition leftLockCTip = leftLockCMid.addOrReplaceChild("left_lock_c_tip", CubeListBuilder.create()
                .texOffs(51, 138).mirror(true).addBox(-0.27f, -4.5f, -0.84f, 3.06f, 4.5f, 1.68f), PartPose.offsetAndRotation(0, -4.8f, 0, 0, -0, -0.3125f));
        PartDefinition rightLockD = head.addOrReplaceChild("right_lock_d", CubeListBuilder.create()
                .texOffs(162, 127).mirror(false).addBox(-4.05f, -5.85f, -1.08f, 8.1f, 5.85f, 2.16f), PartPose.offsetAndRotation(-8.5f, 7.5f, 0, -0.9f, 0, -2.1f));
        PartDefinition leftLockD = head.addOrReplaceChild("left_lock_d", CubeListBuilder.create()
                .texOffs(162, 127).mirror(true).addBox(-4.05f, -5.85f, -1.08f, 8.1f, 5.85f, 2.16f), PartPose.offsetAndRotation(8.5f, 7.5f, 0, -0.9f, 0, 2.1f));
        PartDefinition rightLockDMid = rightLockD.addOrReplaceChild("right_lock_d_mid", CubeListBuilder.create()
                .texOffs(64, 138).mirror(false).addBox(-3.483f, -4.95f, -0.918f, 5.346f, 4.95f, 1.836f), PartPose.offsetAndRotation(0, -5.13f, 0, 0, 0, 0.3f));
        PartDefinition leftLockDMid = leftLockD.addOrReplaceChild("left_lock_d_mid", CubeListBuilder.create()
                .texOffs(64, 138).mirror(true).addBox(-1.863f, -4.95f, -0.918f, 5.346f, 4.95f, 1.836f), PartPose.offsetAndRotation(0, -5.13f, 0, 0, -0, -0.3f));
        PartDefinition rightLockDTip = rightLockDMid.addOrReplaceChild("right_lock_d_tip", CubeListBuilder.create()
                .texOffs(115, 138).mirror(false).addBox(-2.511f, -4.05f, -0.756f, 2.754f, 4.05f, 1.512f), PartPose.offsetAndRotation(0, -4.32f, 0, 0, 0, 0.375f));
        PartDefinition leftLockDTip = leftLockDMid.addOrReplaceChild("left_lock_d_tip", CubeListBuilder.create()
                .texOffs(115, 138).mirror(true).addBox(-0.243f, -4.05f, -0.756f, 2.754f, 4.05f, 1.512f), PartPose.offsetAndRotation(0, -4.32f, 0, 0, -0, -0.375f));
        PartDefinition rightForeleg = body.addOrReplaceChild("right_foreleg", CubeListBuilder.create()
                .texOffs(143, 35).mirror(false).addBox(-4, -4, -5, 8, 12, 10)
                .texOffs(0, 61).mirror(false).addBox(-3, 8, -3.5f, 6, 13, 6.5f)
                .texOffs(0, 138).mirror(false).addBox(-2.5f, 9, 3, 5, 5, 3)
                .texOffs(191, 82).mirror(false).addBox(-4, 20.5f, -7.5f, 8, 4.5f, 9)
                .texOffs(206, 138).mirror(false).addBox(-3.7f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(206, 138).mirror(false).addBox(-1.65f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(206, 138).mirror(false).addBox(0.4f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(206, 138).mirror(false).addBox(2.45f, 22, -9.3f, 1.4f, 2.5f, 2.2f), PartPose.offsetAndRotation(-6.5f, -1, -14, 0, 0, 0));
        PartDefinition leftForeleg = body.addOrReplaceChild("left_foreleg", CubeListBuilder.create()
                .texOffs(143, 35).mirror(true).addBox(-4, -4, -5, 8, 12, 10)
                .texOffs(0, 61).mirror(true).addBox(-3, 8, -3.5f, 6, 13, 6.5f)
                .texOffs(0, 138).mirror(true).addBox(-2.5f, 9, 3, 5, 5, 3)
                .texOffs(191, 82).mirror(true).addBox(-4, 20.5f, -7.5f, 8, 4.5f, 9)
                .texOffs(206, 138).mirror(true).addBox(2.3f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(206, 138).mirror(true).addBox(0.25f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(206, 138).mirror(true).addBox(-1.8f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(206, 138).mirror(true).addBox(-3.85f, 22, -9.3f, 1.4f, 2.5f, 2.2f), PartPose.offsetAndRotation(6.5f, -1, -14, 0, 0, 0));
        PartDefinition rightHindleg = body.addOrReplaceChild("right_hindleg", CubeListBuilder.create(), PartPose.offsetAndRotation(-5.5f, -2, 15, 0, 0, 0));
        PartDefinition leftHindleg = body.addOrReplaceChild("left_hindleg", CubeListBuilder.create(), PartPose.offsetAndRotation(5.5f, -2, 15, 0, 0, 0));
        PartDefinition rightThigh = rightHindleg.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                .texOffs(0, 35).mirror(false).addBox(-4, -3, -5.5f, 8, 15, 10), PartPose.offsetAndRotation(0, 0, 0, -0.5f, 0, 0));
        PartDefinition leftThigh = leftHindleg.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                .texOffs(0, 35).mirror(true).addBox(-4, -3, -5.5f, 8, 15, 10), PartPose.offsetAndRotation(0, 0, 0, -0.5f, 0, 0));
        PartDefinition rightShin = rightThigh.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(52, 82).mirror(false).addBox(-2.75f, -1.5f, -2.5f, 5.5f, 11, 5), PartPose.offsetAndRotation(0, 11, -1, 1.4f, 0, 0));
        PartDefinition leftShin = leftThigh.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(52, 82).mirror(true).addBox(-2.75f, -1.5f, -2.5f, 5.5f, 11, 5), PartPose.offsetAndRotation(0, 11, -1, 1.4f, 0, 0));
        PartDefinition rightHock = rightShin.addOrReplaceChild("right_hock", CubeListBuilder.create()
                .texOffs(226, 82).mirror(false).addBox(-2.5f, -1, -2.25f, 5, 9, 4.5f), PartPose.offsetAndRotation(0, 9.5f, 0, -1.2f, 0, 0));
        PartDefinition leftHock = leftShin.addOrReplaceChild("left_hock", CubeListBuilder.create()
                .texOffs(226, 82).mirror(true).addBox(-2.5f, -1, -2.25f, 5, 9, 4.5f), PartPose.offsetAndRotation(0, 9.5f, 0, -1.2f, 0, 0));
        PartDefinition rightHindpaw = rightHock.addOrReplaceChild("right_hindpaw", CubeListBuilder.create()
                .texOffs(198, 100).mirror(false).addBox(-3.5f, -0.7f, -6.5f, 7, 4, 8)
                .texOffs(0, 147).mirror(false).addBox(-3, 1, -8.2f, 1.3f, 2.2f, 2)
                .texOffs(0, 147).mirror(false).addBox(-0.9f, 1, -8.2f, 1.3f, 2.2f, 2)
                .texOffs(0, 147).mirror(false).addBox(1.2f, 1, -8.2f, 1.3f, 2.2f, 2), PartPose.offsetAndRotation(0, 8, 0, 0.3f, 0, 0));
        PartDefinition leftHindpaw = leftHock.addOrReplaceChild("left_hindpaw", CubeListBuilder.create()
                .texOffs(198, 100).mirror(true).addBox(-3.5f, -0.7f, -6.5f, 7, 4, 8)
                .texOffs(0, 147).mirror(true).addBox(1.7f, 1, -8.2f, 1.3f, 2.2f, 2)
                .texOffs(0, 147).mirror(true).addBox(-0.4f, 1, -8.2f, 1.3f, 2.2f, 2)
                .texOffs(0, 147).mirror(true).addBox(-2.5f, 1, -8.2f, 1.3f, 2.2f, 2), PartPose.offsetAndRotation(0, 8, 0, 0.3f, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 100).mirror(false).addBox(-1.3f, -1.3f, 0, 2.6f, 2.6f, 11), PartPose.offsetAndRotation(0, -3, 20.5f, -0.5f, 0, 0));
        PartDefinition tailMid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create()
                .texOffs(93, 100).mirror(false).addBox(-1.1f, -1.1f, 0, 2.2f, 2.2f, 10), PartPose.offsetAndRotation(0, 0, 10.5f, 0.9f, 0, 0));
        PartDefinition tailTip = tailMid.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(126, 138).mirror(false).addBox(-1, -1, 0, 2, 2, 5)
                .texOffs(187, 127).mirror(false).addBox(-2.2f, -2.2f, 3.5f, 4.4f, 4.4f, 4), PartPose.offsetAndRotation(0, 0, 9.5f, 0.8f, 0, 0));
        PartDefinition tailFlame = tailTip.addOrReplaceChild("tail_flame", CubeListBuilder.create()
                .texOffs(119, 82).mirror(false).addBox(-0.96f, -5.2f, -3.6f, 1.92f, 5.2f, 7.2f), PartPose.offsetAndRotation(0, -0.5f, 6, -1.1f, 0, 0));
        PartDefinition tailFlameMid = tailFlame.addOrReplaceChild("tail_flame_mid", CubeListBuilder.create()
                .texOffs(98, 115).mirror(false).addBox(-0.816f, -4.4f, -3.096f, 1.632f, 4.4f, 4.752f), PartPose.offsetAndRotation(0, -4.56f, 0, 0.35f, 0, 0));
        PartDefinition tailFlameTip = tailFlameMid.addOrReplaceChild("tail_flame_tip", CubeListBuilder.create()
                .texOffs(40, 138).mirror(false).addBox(-0.672f, -3.6f, -2.232f, 1.344f, 3.6f, 2.448f), PartPose.offsetAndRotation(0, -3.84f, 0, 0.4375f, 0, 0));
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
        private final ModelPart root, body, head, jaw, rightFore, leftFore, rightHind, leftHind, tail, tailMid;

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
            tailMid = tail.getChild("tail_mid");
        }

        @Override
        public void setupAnim(AzureLionEntity e, float swing, float amount, float age, float yaw, float pitch) {
            // a long, prowling stride, the diagonal legs together
            float gait = Mth.cos(swing * 0.5f) * 0.7f * amount;
            rightFore.xRot = leftHind.xRot = gait;
            leftFore.xRot = rightHind.xRot = -gait;
            rightFore.zRot = leftFore.zRot = 0;
            body.xRot = 0;
            body.zRot = 0;
            // the head low, turning to what he watches; the jaws never quite shut on the fangs; the tail lashing
            head.xRot = 0.1f + pitch * Mth.DEG_TO_RAD * 0.4f;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            head.zRot = 0;
            jaw.xRot = 0.25f + Mth.sin(age * 0.07f) * 0.05f;
            tail.xRot = -0.5f + Mth.sin(age * 0.05f) * 0.08f;
            tail.yRot = Mth.sin(age * 0.08f) * 0.35f;
            tailMid.yRot = Mth.sin(age * 0.08f - 0.9f) * 0.3f;
            if (e.isRoaring()) {
                head.xRot = -0.4f;
                jaw.xRot = 1.1f;
            }
            if (e.isGaping()) {
                // the head up and the jaws thrown wide, a gate to swallow a man
                head.xRot = -0.25f;
                jaw.xRot = 1.25f;
            }
            if (e.isPouncing()) {
                // in the air, forelegs reaching, hind legs thrown back, jaws wide, the tail streaming
                body.xRot = -0.3f;
                rightFore.xRot = leftFore.xRot = -1.1f;
                rightFore.zRot = 0.3f;
                leftFore.zRot = -0.3f;
                rightHind.xRot = leftHind.xRot = 0.9f;
                head.xRot = -0.3f;
                jaw.xRot = 1.2f;
                tail.xRot = -0.05f;
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
