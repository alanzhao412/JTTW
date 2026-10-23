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
                .texOffs(154, 61).mirror(false).addBox(-10, -12, -15, 20, 9, 8)
                .texOffs(95, 99).mirror(false).addBox(-9.6f, 3, -23.6f, 19.2f, 2.5f, 9)
                .texOffs(210, 113).mirror(false).addBox(-2, 5, -25.4f, 4, 4.5f, 4)
                .texOffs(201, 125).mirror(false).addBox(-6.6f, 5, -24.4f, 2.6f, 3, 2.6f)
                .texOffs(201, 125).mirror(false).addBox(4, 5, -24.4f, 2.6f, 3, 2.6f)
                .texOffs(214, 125).mirror(false).addBox(-0.8f, 9.5f, -24.2f, 1.6f, 4, 1.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rump = body.addOrReplaceChild("rump", CubeListBuilder.create()
                .texOffs(72, 61).mirror(false).addBox(-7.5f, 0, 0, 15, 7, 12), PartPose.offsetAndRotation(0, -7, 10, -0.4f, 0, 0));
        PartDefinition crestA = body.addOrReplaceChild("crest_a", CubeListBuilder.create()
                .texOffs(234, 61).mirror(false).addBox(-1.43f, -7.7f, -3.575f, 2.86f, 7.7f, 7.15f)
                .texOffs(154, 99).mirror(false).addBox(-0.9152f, -13.2f, -2.805f, 1.8304f, 5.5f, 5.005f)
                .texOffs(227, 113).mirror(false).addBox(-0.429f, -17.6f, -1.925f, 0.858f, 4.4f, 3.2175f), PartPose.offsetAndRotation(0, -12, -15, -1.35f, 0, 0));
        PartDefinition crestB = body.addOrReplaceChild("crest_b", CubeListBuilder.create()
                .texOffs(23, 82).mirror(false).addBox(-1.3f, -7, -3.25f, 2.6f, 7, 6.5f)
                .texOffs(46, 113).mirror(false).addBox(-0.832f, -12, -2.55f, 1.664f, 5, 4.55f)
                .texOffs(153, 125).mirror(false).addBox(-0.39f, -16, -1.75f, 0.78f, 4, 2.925f), PartPose.offsetAndRotation(0, -11.5f, -8, -1.35f, 0, 0));
        PartDefinition crestC = body.addOrReplaceChild("crest_c", CubeListBuilder.create()
                .texOffs(171, 99).mirror(false).addBox(-1.04f, -5.6f, -2.6f, 2.08f, 5.6f, 5.2f)
                .texOffs(74, 125).mirror(false).addBox(-0.6656f, -9.6f, -2.04f, 1.3312f, 4, 3.64f)
                .texOffs(153, 125).mirror(false).addBox(-0.312f, -12.8f, -1.4f, 0.624f, 3.2f, 2.34f), PartPose.offsetAndRotation(0, -8.5f, -1, -1.45f, 0, 0));
        PartDefinition crestD = body.addOrReplaceChild("crest_d", CubeListBuilder.create()
                .texOffs(238, 113).mirror(false).addBox(-0.78f, -4.2f, -1.95f, 1.56f, 4.2f, 3.9f)
                .texOffs(223, 125).mirror(false).addBox(-0.4992f, -7.2f, -1.53f, 0.9984f, 3, 2.73f)
                .texOffs(43, 135).mirror(false).addBox(-0.234f, -9.6f, -1.05f, 0.468f, 2.4f, 1.755f), PartPose.offsetAndRotation(0, -8, 7, -1.5f, 0, 0));
        PartDefinition rightNeckA = body.addOrReplaceChild("right_neck_a", CubeListBuilder.create()
                .texOffs(127, 61).mirror(false).addBox(-1.69f, -9.1f, -4.225f, 3.38f, 9.1f, 8.45f)
                .texOffs(207, 82).mirror(false).addBox(-1.0816f, -15.6f, -3.315f, 2.1632f, 6.5f, 5.915f)
                .texOffs(61, 113).mirror(false).addBox(-0.507f, -20.8f, -2.275f, 1.014f, 5.2f, 3.8025f), PartPose.offsetAndRotation(-7.5f, -10, -18, -1.4f, 0, -0.55f));
        PartDefinition leftNeckA = body.addOrReplaceChild("left_neck_a", CubeListBuilder.create()
                .texOffs(127, 61).mirror(true).addBox(-1.69f, -9.1f, -4.225f, 3.38f, 9.1f, 8.45f)
                .texOffs(207, 82).mirror(true).addBox(-1.0816f, -15.6f, -3.315f, 2.1632f, 6.5f, 5.915f)
                .texOffs(61, 113).mirror(true).addBox(-0.507f, -20.8f, -2.275f, 1.014f, 5.2f, 3.8025f), PartPose.offsetAndRotation(7.5f, -10, -18, -1.4f, 0, 0.55f));
        PartDefinition rightNeckB = body.addOrReplaceChild("right_neck_b", CubeListBuilder.create()
                .texOffs(44, 82).mirror(false).addBox(-4.225f, -9.1f, -1.69f, 8.45f, 9.1f, 3.38f)
                .texOffs(74, 113).mirror(false).addBox(-2.704f, -15.6f, -0.78f, 5.408f, 6.5f, 2.366f)
                .texOffs(87, 125).mirror(false).addBox(-1.2675f, -20.8f, 0.26f, 2.535f, 5.2f, 1.521f), PartPose.offsetAndRotation(-10, -4, -18, -1.4f, 0, -1.2f));
        PartDefinition leftNeckB = body.addOrReplaceChild("left_neck_b", CubeListBuilder.create()
                .texOffs(44, 82).mirror(true).addBox(-4.225f, -9.1f, -1.69f, 8.45f, 9.1f, 3.38f)
                .texOffs(74, 113).mirror(true).addBox(-2.704f, -15.6f, -0.78f, 5.408f, 6.5f, 2.366f)
                .texOffs(87, 125).mirror(true).addBox(-1.2675f, -20.8f, 0.26f, 2.535f, 5.2f, 1.521f), PartPose.offsetAndRotation(10, -4, -18, -1.4f, 0, 1.2f));
        PartDefinition rightNeckC = body.addOrReplaceChild("right_neck_c", CubeListBuilder.create()
                .texOffs(190, 99).mirror(false).addBox(-3.7375f, -8.05f, -1.495f, 7.475f, 8.05f, 2.99f)
                .texOffs(0, 125).mirror(false).addBox(-2.392f, -13.8f, -0.69f, 4.784f, 5.75f, 2.093f)
                .texOffs(162, 125).mirror(false).addBox(-1.1212f, -18.4f, 0.23f, 2.2425f, 4.6f, 1.3455f), PartPose.offsetAndRotation(-9.5f, 3, -19, -1.3f, 0, -1.9f));
        PartDefinition leftNeckC = body.addOrReplaceChild("left_neck_c", CubeListBuilder.create()
                .texOffs(190, 99).mirror(true).addBox(-3.7375f, -8.05f, -1.495f, 7.475f, 8.05f, 2.99f)
                .texOffs(0, 125).mirror(true).addBox(-2.392f, -13.8f, -0.69f, 4.784f, 5.75f, 2.093f)
                .texOffs(162, 125).mirror(true).addBox(-1.1212f, -18.4f, 0.23f, 2.2425f, 4.6f, 1.3455f), PartPose.offsetAndRotation(9.5f, 3, -19, -1.3f, 0, 1.9f));
        PartDefinition rightNeckD = body.addOrReplaceChild("right_neck_d", CubeListBuilder.create()
                .texOffs(93, 113).mirror(false).addBox(-3.0875f, -6.65f, -1.235f, 6.175f, 6.65f, 2.47f)
                .texOffs(173, 125).mirror(false).addBox(-1.976f, -11.4f, -0.57f, 3.952f, 4.75f, 1.729f)
                .texOffs(232, 125).mirror(false).addBox(-0.9262f, -15.2f, 0.19f, 1.8525f, 3.8f, 1.1115f), PartPose.offsetAndRotation(-9.5f, -5, -9, -1.5f, 0, -1.05f));
        PartDefinition leftNeckD = body.addOrReplaceChild("left_neck_d", CubeListBuilder.create()
                .texOffs(93, 113).mirror(true).addBox(-3.0875f, -6.65f, -1.235f, 6.175f, 6.65f, 2.47f)
                .texOffs(173, 125).mirror(true).addBox(-1.976f, -11.4f, -0.57f, 3.952f, 4.75f, 1.729f)
                .texOffs(232, 125).mirror(true).addBox(-0.9262f, -15.2f, 0.19f, 1.8525f, 3.8f, 1.1115f), PartPose.offsetAndRotation(9.5f, -5, -9, -1.5f, 0, 1.05f));
        PartDefinition apron = body.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(17, 125).mirror(false).addBox(-2.6f, -5.6f, -1.04f, 5.2f, 5.6f, 2.08f)
                .texOffs(241, 125).mirror(false).addBox(-1.664f, -9.6f, -0.48f, 3.328f, 4, 1.456f)
                .texOffs(50, 135).mirror(false).addBox(-0.78f, -12.8f, 0.16f, 1.56f, 3.2f, 0.936f), PartPose.offsetAndRotation(0, 7, -22, -2.75f, 0, 0));
        PartDefinition rightApron = body.addOrReplaceChild("right_apron", CubeListBuilder.create()
                .texOffs(17, 125).mirror(false).addBox(-2.6f, -5.6f, -1.04f, 5.2f, 5.6f, 2.08f)
                .texOffs(241, 125).mirror(false).addBox(-1.664f, -9.6f, -0.48f, 3.328f, 4, 1.456f)
                .texOffs(50, 135).mirror(false).addBox(-0.78f, -12.8f, 0.16f, 1.56f, 3.2f, 0.936f), PartPose.offsetAndRotation(-4.5f, 6, -21.5f, -2.8f, 0, 0.3f));
        PartDefinition leftApron = body.addOrReplaceChild("left_apron", CubeListBuilder.create()
                .texOffs(17, 125).mirror(true).addBox(-2.6f, -5.6f, -1.04f, 5.2f, 5.6f, 2.08f)
                .texOffs(241, 125).mirror(true).addBox(-1.664f, -9.6f, -0.48f, 3.328f, 4, 1.456f)
                .texOffs(50, 135).mirror(true).addBox(-0.78f, -12.8f, 0.16f, 1.56f, 3.2f, 0.936f), PartPose.offsetAndRotation(4.5f, 6, -21.5f, -2.8f, 0, -0.3f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(92, 35).mirror(false).addBox(-7, -6, -11, 14, 11, 11)
                .texOffs(0, 99).mirror(false).addBox(-6, -9, -10, 12, 4, 9)
                .texOffs(71, 82).mirror(false).addBox(-8.5f, -1, -10, 17, 6, 8)
                .texOffs(114, 113).mirror(false).addBox(-3, -4.2f, -16, 6, 3.6f, 5.5f)
                .texOffs(0, 113).mirror(false).addBox(-5.8f, -0.8f, -16.5f, 5.6f, 4.8f, 6)
                .texOffs(0, 113).mirror(false).addBox(0.2f, -0.8f, -16.5f, 5.6f, 4.8f, 6)
                .texOffs(139, 113).mirror(false).addBox(-1, -0.8f, -16, 2, 4.4f, 5)
                .texOffs(57, 135).mirror(false).addBox(-2.8f, -3.4f, -17, 5.6f, 2.8f, 1.4f)
                .texOffs(0, 135).mirror(false).addBox(-5.6f, 3.2f, -16, 1.7f, 3.6f, 1.7f)
                .texOffs(0, 135).mirror(false).addBox(3.9f, 3.2f, -16, 1.7f, 3.6f, 1.7f)
                .texOffs(74, 135).mirror(false).addBox(-5.3f, 6.6f, -15.8f, 1.1f, 3, 1.1f)
                .texOffs(74, 135).mirror(false).addBox(4.2f, 6.6f, -15.8f, 1.1f, 3, 1.1f)
                .texOffs(92, 135).mirror(false).addBox(-3.7f, 3.6f, -16.3f, 7.4f, 1.4f, 0.8f)
                .texOffs(154, 113).mirror(false).addBox(-3.6f, 1, -11, 7.2f, 6, 4)
                .texOffs(180, 35).mirror(false).addBox(-11, -9, -4, 22, 14, 7)
                .texOffs(71, 0).mirror(false).addBox(-7.5f, -13, -4, 15, 22, 7)
                .texOffs(36, 125).mirror(false).addBox(-5.6f, -14.7f, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(36, 125).mirror(false).addBox(0.4f, -14.7f, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(98, 125).mirror(false).addBox(-10.3f, -11.5f, -5.8f, 4.4f, 4.4f, 3)
                .texOffs(98, 125).mirror(false).addBox(5.9f, -11.5f, -5.8f, 4.4f, 4.4f, 3)
                .texOffs(36, 125).mirror(false).addBox(-13.7f, -7, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(36, 125).mirror(false).addBox(8.5f, -7, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(98, 125).mirror(false).addBox(-13.3f, -0.8f, -5.8f, 4.4f, 4.4f, 3)
                .texOffs(98, 125).mirror(false).addBox(8.9f, -0.8f, -5.8f, 4.4f, 4.4f, 3)
                .texOffs(36, 125).mirror(false).addBox(-10.7f, 3.7f, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(36, 125).mirror(false).addBox(5.5f, 3.7f, -6.5f, 5.2f, 5.2f, 3)
                .texOffs(98, 125).mirror(false).addBox(-5.2f, 6.9f, -5.8f, 4.4f, 4.4f, 3)
                .texOffs(98, 125).mirror(false).addBox(0.8f, 6.9f, -5.8f, 4.4f, 4.4f, 3), PartPose.offsetAndRotation(0, -6, -21, 0.1f, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(9, 135).mirror(false).addBox(-7.5f, -1, -1.5f, 7.5f, 2.5f, 3), PartPose.offsetAndRotation(-0.5f, -5.5f, -11.5f, 0, 0, 0.35f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(9, 135).mirror(true).addBox(0, -1, -1.5f, 7.5f, 2.5f, 3), PartPose.offsetAndRotation(0.5f, -5.5f, -11.5f, 0, 0, -0.35f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(179, 113).mirror(false).addBox(-3.8f, 0, -6.5f, 7.6f, 2.5f, 6.5f)
                .texOffs(74, 135).mirror(false).addBox(-3.6f, -2.6f, -6.2f, 1.5f, 2.6f, 1.5f)
                .texOffs(74, 135).mirror(false).addBox(2.1f, -2.6f, -6.2f, 1.5f, 2.6f, 1.5f)
                .texOffs(98, 125).mirror(false).addBox(-7.5f, 2, -7, 5, 5, 3)
                .texOffs(98, 125).mirror(false).addBox(-2.5f, 2.5f, -7.5f, 5, 5, 3)
                .texOffs(98, 125).mirror(false).addBox(2.5f, 2, -7, 5, 5, 3), PartPose.offsetAndRotation(0, 3.5f, -10, 0.25f, 0, 0));
        PartDefinition tongue = jaw.addOrReplaceChild("tongue", CubeListBuilder.create()
                .texOffs(115, 125).mirror(false).addBox(-2, -0.5f, -5.8f, 4, 1, 6.8f), PartPose.offsetAndRotation(0, 0.3f, -3, 0.35f, 0, 0));
        PartDefinition topLock = head.addOrReplaceChild("top_lock", CubeListBuilder.create()
                .texOffs(211, 61).mirror(false).addBox(-1.495f, -8.05f, -3.7375f, 2.99f, 8.05f, 7.475f)
                .texOffs(154, 99).mirror(false).addBox(-0.9568f, -13.8f, -2.9325f, 1.9136f, 5.75f, 5.2325f)
                .texOffs(227, 113).mirror(false).addBox(-0.4485f, -18.4f, -2.0125f, 0.897f, 4.6f, 3.3638f), PartPose.offsetAndRotation(0, -12, 1, -1.3f, 0, 0));
        PartDefinition rightLockA = head.addOrReplaceChild("right_lock_a", CubeListBuilder.create()
                .texOffs(211, 61).mirror(false).addBox(-1.495f, -8.05f, -3.7375f, 2.99f, 8.05f, 7.475f)
                .texOffs(154, 99).mirror(false).addBox(-0.9568f, -13.8f, -2.9325f, 1.9136f, 5.75f, 5.2325f)
                .texOffs(227, 113).mirror(false).addBox(-0.4485f, -18.4f, -2.0125f, 0.897f, 4.6f, 3.3638f), PartPose.offsetAndRotation(-6, -11, 0, -1.1f, 0, -0.5f));
        PartDefinition leftLockA = head.addOrReplaceChild("left_lock_a", CubeListBuilder.create()
                .texOffs(211, 61).mirror(true).addBox(-1.495f, -8.05f, -3.7375f, 2.99f, 8.05f, 7.475f)
                .texOffs(154, 99).mirror(true).addBox(-0.9568f, -13.8f, -2.9325f, 1.9136f, 5.75f, 5.2325f)
                .texOffs(227, 113).mirror(true).addBox(-0.4485f, -18.4f, -2.0125f, 0.897f, 4.6f, 3.3638f), PartPose.offsetAndRotation(6, -11, 0, -1.1f, 0, 0.5f));
        PartDefinition rightLockB = head.addOrReplaceChild("right_lock_b", CubeListBuilder.create()
                .texOffs(43, 99).mirror(false).addBox(-3.9f, -8.4f, -1.56f, 7.8f, 8.4f, 3.12f)
                .texOffs(0, 125).mirror(false).addBox(-2.496f, -14.4f, -0.72f, 4.992f, 6, 2.184f)
                .texOffs(162, 125).mirror(false).addBox(-1.17f, -19.2f, 0.24f, 2.34f, 4.8f, 1.404f), PartPose.offsetAndRotation(-10, -5, 0, -0.9f, 0, -1.05f));
        PartDefinition leftLockB = head.addOrReplaceChild("left_lock_b", CubeListBuilder.create()
                .texOffs(43, 99).mirror(true).addBox(-3.9f, -8.4f, -1.56f, 7.8f, 8.4f, 3.12f)
                .texOffs(0, 125).mirror(true).addBox(-2.496f, -14.4f, -0.72f, 4.992f, 6, 2.184f)
                .texOffs(162, 125).mirror(true).addBox(-1.17f, -19.2f, 0.24f, 2.34f, 4.8f, 1.404f), PartPose.offsetAndRotation(10, -5, 0, -0.9f, 0, 1.05f));
        PartDefinition rightLockC = head.addOrReplaceChild("right_lock_c", CubeListBuilder.create()
                .texOffs(43, 99).mirror(false).addBox(-3.9f, -8.4f, -1.56f, 7.8f, 8.4f, 3.12f)
                .texOffs(0, 125).mirror(false).addBox(-2.496f, -14.4f, -0.72f, 4.992f, 6, 2.184f)
                .texOffs(162, 125).mirror(false).addBox(-1.17f, -19.2f, 0.24f, 2.34f, 4.8f, 1.404f), PartPose.offsetAndRotation(-11, 2, 0, -0.8f, 0, -1.6f));
        PartDefinition leftLockC = head.addOrReplaceChild("left_lock_c", CubeListBuilder.create()
                .texOffs(43, 99).mirror(true).addBox(-3.9f, -8.4f, -1.56f, 7.8f, 8.4f, 3.12f)
                .texOffs(0, 125).mirror(true).addBox(-2.496f, -14.4f, -0.72f, 4.992f, 6, 2.184f)
                .texOffs(162, 125).mirror(true).addBox(-1.17f, -19.2f, 0.24f, 2.34f, 4.8f, 1.404f), PartPose.offsetAndRotation(11, 2, 0, -0.8f, 0, 1.6f));
        PartDefinition rightLockD = head.addOrReplaceChild("right_lock_d", CubeListBuilder.create()
                .texOffs(25, 113).mirror(false).addBox(-3.4125f, -7.35f, -1.365f, 6.825f, 7.35f, 2.73f)
                .texOffs(138, 125).mirror(false).addBox(-2.184f, -12.6f, -0.63f, 4.368f, 5.25f, 1.911f)
                .texOffs(162, 125).mirror(false).addBox(-1.0237f, -16.8f, 0.21f, 2.0475f, 4.2f, 1.2285f), PartPose.offsetAndRotation(-8, 8, 0, -0.7f, 0, -2.2f));
        PartDefinition leftLockD = head.addOrReplaceChild("left_lock_d", CubeListBuilder.create()
                .texOffs(25, 113).mirror(true).addBox(-3.4125f, -7.35f, -1.365f, 6.825f, 7.35f, 2.73f)
                .texOffs(138, 125).mirror(true).addBox(-2.184f, -12.6f, -0.63f, 4.368f, 5.25f, 1.911f)
                .texOffs(162, 125).mirror(true).addBox(-1.0237f, -16.8f, 0.21f, 2.0475f, 4.2f, 1.2285f), PartPose.offsetAndRotation(8, 8, 0, -0.7f, 0, 2.2f));
        PartDefinition rightForeleg = body.addOrReplaceChild("right_foreleg", CubeListBuilder.create()
                .texOffs(143, 35).mirror(false).addBox(-4, -4, -5, 8, 12, 10)
                .texOffs(0, 61).mirror(false).addBox(-3, 8, -3.5f, 6, 13, 6.5f)
                .texOffs(98, 125).mirror(false).addBox(-2.5f, 9, 3, 5, 5, 3)
                .texOffs(122, 82).mirror(false).addBox(-4, 20.5f, -7.5f, 8, 4.5f, 9)
                .texOffs(32, 135).mirror(false).addBox(-3.7f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(32, 135).mirror(false).addBox(-1.65f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(32, 135).mirror(false).addBox(0.4f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(32, 135).mirror(false).addBox(2.45f, 22, -9.3f, 1.4f, 2.5f, 2.2f), PartPose.offsetAndRotation(-6.5f, -1, -14, 0, 0, 0));
        PartDefinition leftForeleg = body.addOrReplaceChild("left_foreleg", CubeListBuilder.create()
                .texOffs(143, 35).mirror(true).addBox(-4, -4, -5, 8, 12, 10)
                .texOffs(0, 61).mirror(true).addBox(-3, 8, -3.5f, 6, 13, 6.5f)
                .texOffs(98, 125).mirror(true).addBox(-2.5f, 9, 3, 5, 5, 3)
                .texOffs(122, 82).mirror(true).addBox(-4, 20.5f, -7.5f, 8, 4.5f, 9)
                .texOffs(32, 135).mirror(true).addBox(2.3f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(32, 135).mirror(true).addBox(0.25f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(32, 135).mirror(true).addBox(-1.8f, 22, -9.3f, 1.4f, 2.5f, 2.2f)
                .texOffs(32, 135).mirror(true).addBox(-3.85f, 22, -9.3f, 1.4f, 2.5f, 2.2f), PartPose.offsetAndRotation(6.5f, -1, -14, 0, 0, 0));
        PartDefinition rightHindleg = body.addOrReplaceChild("right_hindleg", CubeListBuilder.create(), PartPose.offsetAndRotation(-5.5f, -2, 15, 0, 0, 0));
        PartDefinition leftHindleg = body.addOrReplaceChild("left_hindleg", CubeListBuilder.create(), PartPose.offsetAndRotation(5.5f, -2, 15, 0, 0, 0));
        PartDefinition rightThigh = rightHindleg.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                .texOffs(0, 35).mirror(false).addBox(-4, -3, -5.5f, 8, 15, 10), PartPose.offsetAndRotation(0, 0, 0, -0.5f, 0, 0));
        PartDefinition leftThigh = leftHindleg.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                .texOffs(0, 35).mirror(true).addBox(-4, -3, -5.5f, 8, 15, 10), PartPose.offsetAndRotation(0, 0, 0, -0.5f, 0, 0));
        PartDefinition rightShin = rightThigh.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(0, 82).mirror(false).addBox(-2.75f, -1.5f, -2.5f, 5.5f, 11, 5), PartPose.offsetAndRotation(0, 11, -1, 1.4f, 0, 0));
        PartDefinition leftShin = leftThigh.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(0, 82).mirror(true).addBox(-2.75f, -1.5f, -2.5f, 5.5f, 11, 5), PartPose.offsetAndRotation(0, 11, -1, 1.4f, 0, 0));
        PartDefinition rightHock = rightShin.addOrReplaceChild("right_hock", CubeListBuilder.create()
                .texOffs(157, 82).mirror(false).addBox(-2.5f, -1, -2.25f, 5, 9, 4.5f), PartPose.offsetAndRotation(0, 9.5f, 0, -1.2f, 0, 0));
        PartDefinition leftHock = leftShin.addOrReplaceChild("left_hock", CubeListBuilder.create()
                .texOffs(157, 82).mirror(true).addBox(-2.5f, -1, -2.25f, 5, 9, 4.5f), PartPose.offsetAndRotation(0, 9.5f, 0, -1.2f, 0, 0));
        PartDefinition rightHindpaw = rightHock.addOrReplaceChild("right_hindpaw", CubeListBuilder.create()
                .texOffs(213, 99).mirror(false).addBox(-3.5f, -0.7f, -6.5f, 7, 4, 8)
                .texOffs(83, 135).mirror(false).addBox(-3, 1, -8.2f, 1.3f, 2.2f, 2)
                .texOffs(83, 135).mirror(false).addBox(-0.9f, 1, -8.2f, 1.3f, 2.2f, 2)
                .texOffs(83, 135).mirror(false).addBox(1.2f, 1, -8.2f, 1.3f, 2.2f, 2), PartPose.offsetAndRotation(0, 8, 0, 0.3f, 0, 0));
        PartDefinition leftHindpaw = leftHock.addOrReplaceChild("left_hindpaw", CubeListBuilder.create()
                .texOffs(213, 99).mirror(true).addBox(-3.5f, -0.7f, -6.5f, 7, 4, 8)
                .texOffs(83, 135).mirror(true).addBox(1.7f, 1, -8.2f, 1.3f, 2.2f, 2)
                .texOffs(83, 135).mirror(true).addBox(-0.4f, 1, -8.2f, 1.3f, 2.2f, 2)
                .texOffs(83, 135).mirror(true).addBox(-2.5f, 1, -8.2f, 1.3f, 2.2f, 2), PartPose.offsetAndRotation(0, 8, 0, 0.3f, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(178, 82).mirror(false).addBox(-1.3f, -1.3f, 0, 2.6f, 2.6f, 11), PartPose.offsetAndRotation(0, -3, 20.5f, -0.5f, 0, 0));
        PartDefinition tailMid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create()
                .texOffs(68, 99).mirror(false).addBox(-1.1f, -1.1f, 0, 2.2f, 2.2f, 10), PartPose.offsetAndRotation(0, 0, 10.5f, 0.9f, 0, 0));
        PartDefinition tailTip = tailMid.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(186, 125).mirror(false).addBox(-1, -1, 0, 2, 2, 5)
                .texOffs(55, 125).mirror(false).addBox(-2.2f, -2.2f, 3.5f, 4.4f, 4.4f, 4), PartPose.offsetAndRotation(0, 0, 9.5f, 0.8f, 0, 0));
        PartDefinition tailFlame = tailTip.addOrReplaceChild("tail_flame", CubeListBuilder.create()
                .texOffs(23, 82).mirror(false).addBox(-1.235f, -6.65f, -3.0875f, 2.47f, 6.65f, 6.175f)
                .texOffs(46, 113).mirror(false).addBox(-0.7904f, -11.4f, -2.4225f, 1.5808f, 4.75f, 4.3225f)
                .texOffs(153, 125).mirror(false).addBox(-0.3705f, -15.2f, -1.6625f, 0.741f, 3.8f, 2.7788f), PartPose.offsetAndRotation(0, -0.5f, 6, -1.1f, 0, 0));
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
