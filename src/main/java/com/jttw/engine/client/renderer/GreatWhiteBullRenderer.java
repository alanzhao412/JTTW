package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.bull.GreatWhiteBullEntity;
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
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Great White Bull: a vast, wedge-shaped bull, all its weight in a towering chest and shoulder
 * hump crested with bone spikes, tapering along a long ribbed barrel to lean, hard haunches; scarred
 * white hide shaded grey-blue, every muscle cut, a shaggy mane down its neck and under its dewlap,
 * tufts at its fetlocks and hocks; an iron collar and bronze breast-band studded in gold. Its head
 * is carried low as it charges, under a heavy brow with eyes that burn red; a long, dark, squared
 * snout with flared nostrils and a gold ring; ridged horns, black at the skull, sweeping out, then
 * forward and in to dark points; split hooves; a long, tasselled tail.
 */
public final class GreatWhiteBullRenderer extends MobRenderer<GreatWhiteBullEntity, GreatWhiteBullRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "great_white_bull"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/great_white_bull.png");
    private static final net.minecraft.client.renderer.RenderType EYES = net.minecraft.client.renderer.RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/great_white_bull_eyes.png"));
    /** How much larger than its model's pixels it is drawn: a great bull, its hump at its height. */
    public static final float SCALE = 1.25f;

    public GreatWhiteBullRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.2f);
        // its eyes burn under the brow: drawn full-bright over the skin, even in the dark
        addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
            @Override
            public net.minecraft.client.renderer.RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected void scale(GreatWhiteBullEntity e, PoseStack pose, float partial) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(GreatWhiteBullEntity e) {
        return TEXTURE;
    }

    /**
     * Its parts, in model pixels with the ground at y 24 and the head toward -z: generated, with the
     * skin painted to the same layout, by {@code assets_source/great_white_bull.py}.
     */
    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-10, -9, -18, 20, 19, 16)
                .texOffs(73, 0).mirror(false).addBox(-9, -7, -3, 18, 15, 10)
                .texOffs(102, 36).mirror(false).addBox(-8, -7, 7, 16, 12.5f, 7)
                .texOffs(196, 0).mirror(false).addBox(-8.5f, -8, 13, 17, 13, 8)
                .texOffs(0, 36).mirror(false).addBox(-6, -11, -6, 12, 5, 16)
                .texOffs(0, 58).mirror(false).addBox(-7, -16, -17, 14, 8, 11)
                .texOffs(94, 95).mirror(false).addBox(-5, -18, -15, 10, 3, 7)
                .texOffs(57, 36).mirror(false).addBox(-7, -8, -24, 14, 13, 8)
                .texOffs(0, 95).mirror(false).addBox(-6, -12, -22, 12, 5, 7)
                .texOffs(0, 78).mirror(false).addBox(-3.5f, 4, -25, 7, 7, 9)
                .texOffs(94, 118).mirror(false).addBox(-8.3f, -9, 11.5f, 3, 2, 4)
                .texOffs(94, 118).mirror(false).addBox(5.3f, -9, 11.5f, 3, 2, 4)
                .texOffs(157, 0).mirror(false).addBox(-7.6f, -12.6f, -21.6f, 15.2f, 18.2f, 2.4f)
                .texOffs(175, 58).mirror(false).addBox(-10.5f, 0, -18.6f, 21, 2.6f, 15)
                .texOffs(51, 58).mirror(false).addBox(-9.6f, -7.6f, -2.2f, 19.2f, 16, 2.4f)
                .texOffs(192, 108).mirror(false).addBox(-2.2f, -1.2f, -19.6f, 4.4f, 4.6f, 1.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightPec = body.addOrReplaceChild("right_pec", CubeListBuilder.create()
                .texOffs(33, 78).mirror(false).addBox(-9, -4, -1.6f, 9, 13, 2), PartPose.offsetAndRotation(-0.4f, -1, -18, 0.08f, 0.22f, 0));
        PartDefinition leftPec = body.addOrReplaceChild("left_pec", CubeListBuilder.create()
                .texOffs(33, 78).mirror(true).addBox(0, -4, -1.6f, 9, 13, 2), PartPose.offsetAndRotation(0.4f, -1, -18, 0.08f, -0.22f, 0));
        PartDefinition rightShoulder = body.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(130, 0).mirror(false).addBox(-1.6f, -7, -5.5f, 2.2f, 14, 10), PartPose.offsetAndRotation(-10, -5, -11, -0.12f, 0, 0.1f));
        PartDefinition leftShoulder = body.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(130, 0).mirror(true).addBox(-0.6f, -7, -5.5f, 2.2f, 14, 10), PartPose.offsetAndRotation(10, -5, -11, -0.12f, 0, -0.1f));
        PartDefinition rightShoulderTop = body.addOrReplaceChild("right_shoulder_top", CubeListBuilder.create()
                .texOffs(56, 78).mirror(false).addBox(-1.6f, -1, -5, 3, 6, 9), PartPose.offsetAndRotation(-7.2f, -14, -12, 0, 0, -0.55f));
        PartDefinition leftShoulderTop = body.addOrReplaceChild("left_shoulder_top", CubeListBuilder.create()
                .texOffs(56, 78).mirror(true).addBox(-1.4f, -1, -5, 3, 6, 9), PartPose.offsetAndRotation(7.2f, -14, -12, 0, 0, 0.55f));
        PartDefinition rightNeckRoll = body.addOrReplaceChild("right_neck_roll", CubeListBuilder.create()
                .texOffs(81, 78).mirror(false).addBox(-1.4f, -4, -3, 2, 10, 5), PartPose.offsetAndRotation(-7, -6, -20, 0.45f, 0, 0.06f));
        PartDefinition leftNeckRoll = body.addOrReplaceChild("left_neck_roll", CubeListBuilder.create()
                .texOffs(81, 78).mirror(true).addBox(-0.6f, -4, -3, 2, 10, 5), PartPose.offsetAndRotation(7, -6, -20, 0.45f, 0, -0.06f));
        PartDefinition rightNeckRoll2 = body.addOrReplaceChild("right_neck_roll2", CubeListBuilder.create()
                .texOffs(39, 95).mirror(false).addBox(-1.3f, -3, -2, 1.8f, 8, 4), PartPose.offsetAndRotation(-6.7f, -1, -23.5f, 0.7f, 0, 0.08f));
        PartDefinition leftNeckRoll2 = body.addOrReplaceChild("left_neck_roll2", CubeListBuilder.create()
                .texOffs(39, 95).mirror(true).addBox(-0.5f, -3, -2, 1.8f, 8, 4), PartPose.offsetAndRotation(6.7f, -1, -23.5f, 0.7f, 0, -0.08f));
        PartDefinition rightRibcage = body.addOrReplaceChild("right_ribcage", CubeListBuilder.create()
                .texOffs(98, 58).mirror(false).addBox(-0.8f, -5, -4.5f, 1.4f, 10, 9), PartPose.offsetAndRotation(-9, -1, 2, 0, 0, 0.06f));
        PartDefinition leftRibcage = body.addOrReplaceChild("left_ribcage", CubeListBuilder.create()
                .texOffs(98, 58).mirror(true).addBox(-0.6f, -5, -4.5f, 1.4f, 10, 9), PartPose.offsetAndRotation(9, -1, 2, 0, 0, -0.06f));
        PartDefinition rightThighPlate = body.addOrReplaceChild("right_thigh_plate", CubeListBuilder.create()
                .texOffs(121, 58).mirror(false).addBox(-1.2f, -4, -4, 1.8f, 11, 8), PartPose.offsetAndRotation(-8.5f, -4, 17, 0.12f, 0, 0.05f));
        PartDefinition leftThighPlate = body.addOrReplaceChild("left_thigh_plate", CubeListBuilder.create()
                .texOffs(121, 58).mirror(true).addBox(-0.6f, -4, -4, 1.8f, 11, 8), PartPose.offsetAndRotation(8.5f, -4, 17, 0.12f, 0, -0.05f));
        PartDefinition rump = body.addOrReplaceChild("rump", CubeListBuilder.create()
                .texOffs(96, 78).mirror(false).addBox(-7.5f, 0, 0, 15, 4, 9.5f), PartPose.offsetAndRotation(0, -8, 13, -0.28f, 0, 0));
        PartDefinition spike1 = body.addOrReplaceChild("spike1", CubeListBuilder.create()
                .texOffs(207, 108).mirror(false).addBox(-1.4375f, -3.45f, -1.4375f, 2.875f, 3.45f, 2.875f)
                .texOffs(109, 118).mirror(false).addBox(-0.8625f, -6.44f, -0.8625f, 1.725f, 3.22f, 1.725f), PartPose.offsetAndRotation(0, -17.5f, -14, -0.45f, 0, 0));
        PartDefinition spike2 = body.addOrReplaceChild("spike2", CubeListBuilder.create()
                .texOffs(118, 118).mirror(false).addBox(-1.25f, -3, -1.25f, 2.5f, 3, 2.5f)
                .texOffs(185, 118).mirror(false).addBox(-0.75f, -5.6f, -0.75f, 1.5f, 2.8f, 1.5f), PartPose.offsetAndRotation(0, -17.5f, -10.5f, -0.55f, 0, 0));
        PartDefinition spike3 = body.addOrReplaceChild("spike3", CubeListBuilder.create()
                .texOffs(118, 118).mirror(false).addBox(-1.0625f, -2.55f, -1.0625f, 2.125f, 2.55f, 2.125f)
                .texOffs(185, 118).mirror(false).addBox(-0.6375f, -4.76f, -0.6375f, 1.275f, 2.38f, 1.275f), PartPose.offsetAndRotation(0, -15.5f, -7, -0.65f, 0, 0));
        PartDefinition spike4 = body.addOrReplaceChild("spike4", CubeListBuilder.create()
                .texOffs(194, 118).mirror(false).addBox(-0.875f, -2.1f, -0.875f, 1.75f, 2.1f, 1.75f)
                .texOffs(9, 126).mirror(false).addBox(-0.525f, -3.92f, -0.525f, 1.05f, 1.96f, 1.05f), PartPose.offsetAndRotation(0, -10.5f, -2, -0.8f, 0, 0));
        PartDefinition spike5 = body.addOrReplaceChild("spike5", CubeListBuilder.create()
                .texOffs(18, 126).mirror(false).addBox(-0.6875f, -1.65f, -0.6875f, 1.375f, 1.65f, 1.375f)
                .texOffs(83, 126).mirror(false).addBox(-0.4125f, -3.08f, -0.4125f, 0.825f, 1.54f, 0.825f), PartPose.offsetAndRotation(0, -10.5f, 3, -0.95f, 0, 0));
        PartDefinition mane1 = body.addOrReplaceChild("mane1", CubeListBuilder.create()
                .texOffs(82, 108).mirror(false).addBox(-3.125f, -4.375f, -1.5f, 6.25f, 4.375f, 3)
                .texOffs(220, 108).mirror(false).addBox(-2, -7.875f, -1.2f, 4, 3.75f, 2.4f)
                .texOffs(203, 118).mirror(false).addBox(-0.9375f, -10.625f, -0.9f, 1.875f, 3, 1.8f), PartPose.offsetAndRotation(0, -12, -21, -0.6f, 0, 0));
        PartDefinition rightMane1 = body.addOrReplaceChild("right_mane1", CubeListBuilder.create()
                .texOffs(103, 108).mirror(false).addBox(-3, -4.2f, -1.44f, 6, 4.2f, 2.88f)
                .texOffs(220, 108).mirror(false).addBox(-1.92f, -7.56f, -1.152f, 3.84f, 3.6f, 2.304f)
                .texOffs(203, 118).mirror(false).addBox(-0.9f, -10.2f, -0.864f, 1.8f, 2.88f, 1.728f), PartPose.offsetAndRotation(-4.5f, -11.5f, -21, -0.75f, 0, -0.55f));
        PartDefinition leftMane1 = body.addOrReplaceChild("left_mane1", CubeListBuilder.create()
                .texOffs(103, 108).mirror(true).addBox(-3, -4.2f, -1.44f, 6, 4.2f, 2.88f)
                .texOffs(220, 108).mirror(true).addBox(-1.92f, -7.56f, -1.152f, 3.84f, 3.6f, 2.304f)
                .texOffs(203, 118).mirror(true).addBox(-0.9f, -10.2f, -0.864f, 1.8f, 2.88f, 1.728f), PartPose.offsetAndRotation(4.5f, -11.5f, -21, -0.75f, 0, 0.55f));
        PartDefinition rightMane2 = body.addOrReplaceChild("right_mane2", CubeListBuilder.create()
                .texOffs(103, 108).mirror(false).addBox(-2.875f, -4.025f, -1.38f, 5.75f, 4.025f, 2.76f)
                .texOffs(220, 108).mirror(false).addBox(-1.84f, -7.245f, -1.104f, 3.68f, 3.45f, 2.208f)
                .texOffs(203, 118).mirror(false).addBox(-0.8625f, -9.775f, -0.828f, 1.725f, 2.76f, 1.656f), PartPose.offsetAndRotation(-6.8f, -7, -22, -1, 0, -1.2f));
        PartDefinition leftMane2 = body.addOrReplaceChild("left_mane2", CubeListBuilder.create()
                .texOffs(103, 108).mirror(true).addBox(-2.875f, -4.025f, -1.38f, 5.75f, 4.025f, 2.76f)
                .texOffs(220, 108).mirror(true).addBox(-1.84f, -7.245f, -1.104f, 3.68f, 3.45f, 2.208f)
                .texOffs(203, 118).mirror(true).addBox(-0.8625f, -9.775f, -0.828f, 1.725f, 2.76f, 1.656f), PartPose.offsetAndRotation(6.8f, -7, -22, -1, 0, 1.2f));
        PartDefinition rightMane3 = body.addOrReplaceChild("right_mane3", CubeListBuilder.create()
                .texOffs(235, 108).mirror(false).addBox(-2.5f, -3.5f, -1.2f, 5, 3.5f, 2.4f)
                .texOffs(212, 118).mirror(false).addBox(-1.6f, -6.3f, -0.96f, 3.2f, 3, 1.92f)
                .texOffs(203, 118).mirror(false).addBox(-0.75f, -8.5f, -0.72f, 1.5f, 2.4f, 1.44f), PartPose.offsetAndRotation(-7, -1, -23, -1.25f, 0, -1.6f));
        PartDefinition leftMane3 = body.addOrReplaceChild("left_mane3", CubeListBuilder.create()
                .texOffs(235, 108).mirror(true).addBox(-2.5f, -3.5f, -1.2f, 5, 3.5f, 2.4f)
                .texOffs(212, 118).mirror(true).addBox(-1.6f, -6.3f, -0.96f, 3.2f, 3, 1.92f)
                .texOffs(203, 118).mirror(true).addBox(-0.75f, -8.5f, -0.72f, 1.5f, 2.4f, 1.44f), PartPose.offsetAndRotation(7, -1, -23, -1.25f, 0, 1.6f));
        PartDefinition rightMane4 = body.addOrReplaceChild("right_mane4", CubeListBuilder.create()
                .texOffs(235, 108).mirror(false).addBox(-2.5f, -3.5f, -1.2f, 5, 3.5f, 2.4f)
                .texOffs(212, 118).mirror(false).addBox(-1.6f, -6.3f, -0.96f, 3.2f, 3, 1.92f)
                .texOffs(203, 118).mirror(false).addBox(-0.75f, -8.5f, -0.72f, 1.5f, 2.4f, 1.44f), PartPose.offsetAndRotation(-6.6f, -14, -16, -0.9f, 0, -0.75f));
        PartDefinition leftMane4 = body.addOrReplaceChild("left_mane4", CubeListBuilder.create()
                .texOffs(235, 108).mirror(true).addBox(-2.5f, -3.5f, -1.2f, 5, 3.5f, 2.4f)
                .texOffs(212, 118).mirror(true).addBox(-1.6f, -6.3f, -0.96f, 3.2f, 3, 1.92f)
                .texOffs(203, 118).mirror(true).addBox(-0.75f, -8.5f, -0.72f, 1.5f, 2.4f, 1.44f), PartPose.offsetAndRotation(6.6f, -14, -16, -0.9f, 0, 0.75f));
        PartDefinition dewlapTuft = body.addOrReplaceChild("dewlap_tuft", CubeListBuilder.create()
                .texOffs(122, 108).mirror(false).addBox(-2.5875f, 0, -1.725f, 5.175f, 3.45f, 3.45f)
                .texOffs(131, 118).mirror(false).addBox(-1.8112f, 3.25f, -1.2075f, 3.6225f, 2.99f, 2.415f)
                .texOffs(225, 118).mirror(false).addBox(-0.9315f, 6.01f, -0.621f, 1.863f, 2.3f, 1.242f), PartPose.offsetAndRotation(0, 10.5f, -22, 0.25f, 0, 0));
        PartDefinition rightDewlapTuft = body.addOrReplaceChild("right_dewlap_tuft", CubeListBuilder.create()
                .texOffs(146, 118).mirror(false).addBox(-2.025f, 0, -1.35f, 4.05f, 2.7f, 2.7f)
                .texOffs(234, 118).mirror(false).addBox(-1.4175f, 2.5f, -0.945f, 2.835f, 2.34f, 1.89f)
                .texOffs(88, 126).mirror(false).addBox(-0.729f, 4.66f, -0.486f, 1.458f, 1.8f, 0.972f), PartPose.offsetAndRotation(-2.6f, 10, -19, 0.2f, 0, 0.12f));
        PartDefinition leftDewlapTuft = body.addOrReplaceChild("left_dewlap_tuft", CubeListBuilder.create()
                .texOffs(146, 118).mirror(true).addBox(-2.025f, 0, -1.35f, 4.05f, 2.7f, 2.7f)
                .texOffs(234, 118).mirror(true).addBox(-1.4175f, 2.5f, -0.945f, 2.835f, 2.34f, 1.89f)
                .texOffs(88, 126).mirror(true).addBox(-0.729f, 4.66f, -0.486f, 1.458f, 1.8f, 0.972f), PartPose.offsetAndRotation(2.6f, 10, -19, 0.2f, 0, -0.12f));
        PartDefinition chestTuft = body.addOrReplaceChild("chest_tuft", CubeListBuilder.create()
                .texOffs(146, 118).mirror(false).addBox(-2.025f, 0, -1.35f, 4.05f, 2.7f, 2.7f)
                .texOffs(234, 118).mirror(false).addBox(-1.4175f, 2.5f, -0.945f, 2.835f, 2.34f, 1.89f)
                .texOffs(88, 126).mirror(false).addBox(-0.729f, 4.66f, -0.486f, 1.458f, 1.8f, 0.972f), PartPose.offsetAndRotation(0, 9.5f, -15, 0.15f, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(149, 36).mirror(false).addBox(-6, -6, -10, 12, 10, 10)
                .texOffs(0, 118).mirror(false).addBox(-7, -7, -11, 14, 3, 4)
                .texOffs(147, 78).mirror(false).addBox(-4.5f, -2, -17, 9, 7, 7)
                .texOffs(37, 118).mirror(false).addBox(-4, 5, -15, 8, 2, 5)
                .texOffs(180, 78).mirror(false).addBox(-6.8f, -1, -9, 1.6f, 6, 7)
                .texOffs(180, 78).mirror(false).addBox(5.2f, -1, -9, 1.6f, 6, 7)
                .texOffs(100, 126).mirror(false).addBox(-2.5f, 5, -17.8f, 5, 1, 1)
                .texOffs(27, 126).mirror(false).addBox(-2.5f, 2, -17.8f, 1, 3, 1)
                .texOffs(27, 126).mirror(false).addBox(1.5f, 2, -17.8f, 1, 3, 1), PartPose.offsetAndRotation(0, -3, -23, 0.25f, 0, 0));
        PartDefinition forelock = head.addOrReplaceChild("forelock", CubeListBuilder.create()
                .texOffs(143, 108).mirror(false).addBox(-2.475f, 0, -1.65f, 4.95f, 3.3f, 3.3f)
                .texOffs(131, 118).mirror(false).addBox(-1.7325f, 3.1f, -1.155f, 3.465f, 2.86f, 2.31f)
                .texOffs(225, 118).mirror(false).addBox(-0.891f, 5.74f, -0.594f, 1.782f, 2.2f, 1.188f), PartPose.offsetAndRotation(0, -5.5f, -6, -2.7f, 0, 0));
        PartDefinition rightForelock = head.addOrReplaceChild("right_forelock", CubeListBuilder.create()
                .texOffs(131, 118).mirror(false).addBox(-1.9125f, 0, -1.275f, 3.825f, 2.55f, 2.55f)
                .texOffs(234, 118).mirror(false).addBox(-1.3387f, 2.35f, -0.8925f, 2.6775f, 2.21f, 1.785f)
                .texOffs(88, 126).mirror(false).addBox(-0.6885f, 4.39f, -0.459f, 1.377f, 1.7f, 0.918f), PartPose.offsetAndRotation(-2.6f, -5.5f, -4, -2.5f, 0, 0.35f));
        PartDefinition leftForelock = head.addOrReplaceChild("left_forelock", CubeListBuilder.create()
                .texOffs(131, 118).mirror(true).addBox(-1.9125f, 0, -1.275f, 3.825f, 2.55f, 2.55f)
                .texOffs(234, 118).mirror(true).addBox(-1.3387f, 2.35f, -0.8925f, 2.6775f, 2.21f, 1.785f)
                .texOffs(88, 126).mirror(true).addBox(-0.6885f, 4.39f, -0.459f, 1.377f, 1.7f, 0.918f), PartPose.offsetAndRotation(2.6f, -5.5f, -4, -2.5f, 0, -0.35f));
        PartDefinition beard = head.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(131, 118).mirror(false).addBox(-1.6875f, 0, -1.125f, 3.375f, 2.25f, 2.25f)
                .texOffs(32, 126).mirror(false).addBox(-1.1812f, 2.05f, -0.7875f, 2.3625f, 1.95f, 1.575f)
                .texOffs(88, 126).mirror(false).addBox(-0.6075f, 3.85f, -0.405f, 1.215f, 1.5f, 0.81f), PartPose.offsetAndRotation(0, 6.5f, -12, 0.3f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(43, 126).mirror(false).addBox(-4.5f, 0, 0, 4.5f, 2.2f, 1), PartPose.offsetAndRotation(-6, -3, -3, 0.3f, 0.5f, -0.4f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(43, 126).mirror(true).addBox(0, 0, 0, 4.5f, 2.2f, 1), PartPose.offsetAndRotation(6, -3, -3, 0.3f, -0.5f, 0.4f));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(129, 95).mirror(false).addBox(-10, -2.3f, -2.3f, 10, 4.6f, 4.6f)
                .texOffs(52, 95).mirror(false).addBox(-3.5f, -2.7f, -2.7f, 2, 5.4f, 5.4f), PartPose.offsetAndRotation(-6, -5, -6, 0, -0.2f, 0.15f));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(129, 95).mirror(true).addBox(0, -2.3f, -2.3f, 10, 4.6f, 4.6f)
                .texOffs(52, 95).mirror(true).addBox(1.5f, -2.7f, -2.7f, 2, 5.4f, 5.4f), PartPose.offsetAndRotation(6, -5, -6, 0, 0.2f, -0.15f));
        PartDefinition rightHornUp = rightHorn.addOrReplaceChild("right_horn_up", CubeListBuilder.create()
                .texOffs(160, 95).mirror(false).addBox(-1.9f, -5, -1.9f, 3.8f, 5.4f, 3.8f), PartPose.offsetAndRotation(-9.2f, 0, 0, 0.6f, 0, -0.45f));
        PartDefinition leftHornUp = leftHorn.addOrReplaceChild("left_horn_up", CubeListBuilder.create()
                .texOffs(160, 95).mirror(true).addBox(-1.9f, -5, -1.9f, 3.8f, 5.4f, 3.8f), PartPose.offsetAndRotation(9.2f, 0, 0, 0.6f, 0, 0.45f));
        PartDefinition rightHornMid = rightHornUp.addOrReplaceChild("right_horn_mid", CubeListBuilder.create()
                .texOffs(162, 108).mirror(false).addBox(-1.5f, -4, -1.5f, 3, 4.4f, 3), PartPose.offsetAndRotation(0, -4.8f, 0, 0.3f, 0, 0.25f));
        PartDefinition leftHornMid = leftHornUp.addOrReplaceChild("left_horn_mid", CubeListBuilder.create()
                .texOffs(162, 108).mirror(true).addBox(-1.5f, -4, -1.5f, 3, 4.4f, 3), PartPose.offsetAndRotation(0, -4.8f, 0, 0.3f, 0, -0.25f));
        PartDefinition rightHornEnd = rightHornMid.addOrReplaceChild("right_horn_end", CubeListBuilder.create()
                .texOffs(64, 118).mirror(false).addBox(-1.1f, -3.4f, -1.1f, 2.2f, 3.8f, 2.2f), PartPose.offsetAndRotation(0, -3.8f, 0, 0.3f, 0, 0.35f));
        PartDefinition leftHornEnd = leftHornMid.addOrReplaceChild("left_horn_end", CubeListBuilder.create()
                .texOffs(64, 118).mirror(true).addBox(-1.1f, -3.4f, -1.1f, 2.2f, 3.8f, 2.2f), PartPose.offsetAndRotation(0, -3.8f, 0, 0.3f, 0, -0.35f));
        PartDefinition rightHornTip = rightHornEnd.addOrReplaceChild("right_horn_tip", CubeListBuilder.create()
                .texOffs(163, 118).mirror(false).addBox(-0.7f, -3, -0.7f, 1.4f, 3.2f, 1.4f), PartPose.offsetAndRotation(0, -3.2f, 0, 0.25f, 0, 0.35f));
        PartDefinition leftHornTip = leftHornEnd.addOrReplaceChild("left_horn_tip", CubeListBuilder.create()
                .texOffs(163, 118).mirror(true).addBox(-0.7f, -3, -0.7f, 1.4f, 3.2f, 1.4f), PartPose.offsetAndRotation(0, -3.2f, 0, 0.25f, 0, -0.35f));
        PartDefinition rightFrontLeg = root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                .texOffs(142, 58).mirror(false).addBox(-3.6f, -4, -3.8f, 7.2f, 11, 7.6f)
                .texOffs(0, 108).mirror(false).addBox(-3.3f, 6.5f, -3.4f, 6.6f, 1.5f, 6.8f), PartPose.offsetAndRotation(-6, 10, -12, 0, 0, 0));
        PartDefinition leftFrontLeg = root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                .texOffs(142, 58).mirror(true).addBox(-3.6f, -4, -3.8f, 7.2f, 11, 7.6f)
                .texOffs(0, 108).mirror(true).addBox(-3.3f, 6.5f, -3.4f, 6.6f, 1.5f, 6.8f), PartPose.offsetAndRotation(6, 10, -12, 0, 0, 0));
        PartDefinition rightFrontShin = rightFrontLeg.addOrReplaceChild("right_front_shin", CubeListBuilder.create()
                .texOffs(69, 95).mirror(false).addBox(-2.6f, 0, -2.6f, 5.2f, 4.2f, 5.2f)
                .texOffs(177, 95).mirror(false).addBox(-3.1f, 2.4f, -3.1f, 6.2f, 2.4f, 6.4f)
                .texOffs(29, 108).mirror(false).addBox(-2.9f, 3.8f, -3.4f, 2.7f, 2.7f, 5.4f)
                .texOffs(29, 108).mirror(false).addBox(0.2f, 3.8f, -3.4f, 2.7f, 2.7f, 5.4f)
                .texOffs(56, 126).mirror(false).addBox(-2, 2.8f, 2.2f, 1.4f, 1.4f, 1.2f)
                .texOffs(56, 126).mirror(false).addBox(0.6f, 2.8f, 2.2f, 1.4f, 1.4f, 1.2f), PartPose.offsetAndRotation(0, 7.5f, 0, 0, 0, 0));
        PartDefinition leftFrontShin = leftFrontLeg.addOrReplaceChild("left_front_shin", CubeListBuilder.create()
                .texOffs(69, 95).mirror(true).addBox(-2.6f, 0, -2.6f, 5.2f, 4.2f, 5.2f)
                .texOffs(177, 95).mirror(true).addBox(-3.1f, 2.4f, -3.1f, 6.2f, 2.4f, 6.4f)
                .texOffs(29, 108).mirror(true).addBox(0.2f, 3.8f, -3.4f, 2.7f, 2.7f, 5.4f)
                .texOffs(29, 108).mirror(true).addBox(-2.9f, 3.8f, -3.4f, 2.7f, 2.7f, 5.4f)
                .texOffs(56, 126).mirror(true).addBox(0.6f, 2.8f, 2.2f, 1.4f, 1.4f, 1.2f)
                .texOffs(56, 126).mirror(true).addBox(-2, 2.8f, 2.2f, 1.4f, 1.4f, 1.2f), PartPose.offsetAndRotation(0, 7.5f, 0, 0, 0, 0));
        PartDefinition rightHindLeg = root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                .texOffs(194, 36).mirror(false).addBox(-3.4f, -5, -4.6f, 6.8f, 11, 9), PartPose.offsetAndRotation(-5, 6, 16, 0, 0, 0));
        PartDefinition leftHindLeg = root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                .texOffs(194, 36).mirror(true).addBox(-3.4f, -5, -4.6f, 6.8f, 11, 9), PartPose.offsetAndRotation(5, 6, 16, 0, 0, 0));
        PartDefinition rightHindGaskin = rightHindLeg.addOrReplaceChild("right_hind_gaskin", CubeListBuilder.create()
                .texOffs(199, 78).mirror(false).addBox(-2.6f, -0.5f, -2.6f, 5.2f, 7.5f, 5), PartPose.offsetAndRotation(0, 5.5f, 0.4f, 0.4f, 0, 0));
        PartDefinition leftHindGaskin = leftHindLeg.addOrReplaceChild("left_hind_gaskin", CubeListBuilder.create()
                .texOffs(199, 78).mirror(true).addBox(-2.6f, -0.5f, -2.6f, 5.2f, 7.5f, 5), PartPose.offsetAndRotation(0, 5.5f, 0.4f, 0.4f, 0, 0));
        PartDefinition rightHindShin = rightHindGaskin.addOrReplaceChild("right_hind_shin", CubeListBuilder.create()
                .texOffs(206, 95).mirror(false).addBox(-2.2f, 0, -2.2f, 4.4f, 4.2f, 4.4f)
                .texOffs(245, 118).mirror(false).addBox(-1.4f, -1.2f, 1.4f, 2.8f, 2.6f, 1.6f)
                .texOffs(48, 108).mirror(false).addBox(-2.7f, 2.3f, -2.8f, 5.4f, 2.2f, 5.6f)
                .texOffs(175, 108).mirror(false).addBox(-2.6f, 3.64f, -3.1f, 2.5f, 2.6f, 5)
                .texOffs(175, 108).mirror(false).addBox(0.1f, 3.64f, -3.1f, 2.5f, 2.6f, 5), PartPose.offsetAndRotation(0, 6.8f, 0, -0.4f, 0, 0));
        PartDefinition leftHindShin = leftHindGaskin.addOrReplaceChild("left_hind_shin", CubeListBuilder.create()
                .texOffs(206, 95).mirror(true).addBox(-2.2f, 0, -2.2f, 4.4f, 4.2f, 4.4f)
                .texOffs(245, 118).mirror(true).addBox(-1.4f, -1.2f, 1.4f, 2.8f, 2.6f, 1.6f)
                .texOffs(48, 108).mirror(true).addBox(-2.7f, 2.3f, -2.8f, 5.4f, 2.2f, 5.6f)
                .texOffs(175, 108).mirror(true).addBox(0.1f, 3.64f, -3.1f, 2.5f, 2.6f, 5)
                .texOffs(175, 108).mirror(true).addBox(-2.6f, 3.64f, -3.1f, 2.5f, 2.6f, 5), PartPose.offsetAndRotation(0, 6.8f, 0, -0.4f, 0, 0));
        PartDefinition rightHockTuft = rightHindShin.addOrReplaceChild("right_hock_tuft", CubeListBuilder.create()
                .texOffs(32, 126).mirror(false).addBox(-1.35f, 0, -0.9f, 2.7f, 1.8f, 1.8f)
                .texOffs(65, 126).mirror(false).addBox(-0.945f, 1.6f, -0.63f, 1.89f, 1.56f, 1.26f)
                .texOffs(95, 126).mirror(false).addBox(-0.486f, 3.04f, -0.324f, 0.972f, 1.2f, 0.648f), PartPose.offsetAndRotation(0, -0.4f, 2.6f, 0.5f, 0, 0));
        PartDefinition leftHockTuft = leftHindShin.addOrReplaceChild("left_hock_tuft", CubeListBuilder.create()
                .texOffs(32, 126).mirror(true).addBox(-1.35f, 0, -0.9f, 2.7f, 1.8f, 1.8f)
                .texOffs(65, 126).mirror(true).addBox(-0.945f, 1.6f, -0.63f, 1.89f, 1.56f, 1.26f)
                .texOffs(95, 126).mirror(true).addBox(-0.486f, 3.04f, -0.324f, 0.972f, 1.2f, 0.648f), PartPose.offsetAndRotation(0, -0.4f, 2.6f, 0.5f, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(227, 95).mirror(false).addBox(-1.1f, 0, -1.1f, 2.2f, 7, 2.2f), PartPose.offsetAndRotation(0, -6, 21.2f, 0.35f, 0, 0));
        PartDefinition tailMid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create()
                .texOffs(73, 108).mirror(false).addBox(-0.9f, 0, -0.9f, 1.8f, 6.4f, 1.8f), PartPose.offsetAndRotation(0, 6.6f, 0, 0.08f, 0, 0));
        PartDefinition tailTip = tailMid.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(0, 126).mirror(false).addBox(-0.8f, 0, -0.8f, 1.6f, 3, 1.6f)
                .texOffs(77, 118).mirror(false).addBox(-1.7f, 2.2f, -1.7f, 3.4f, 3, 3.4f)
                .texOffs(172, 118).mirror(false).addBox(-1.4f, 4.8f, -1.4f, 2.8f, 2.8f, 2.8f)
                .texOffs(74, 126).mirror(false).addBox(-0.8f, 7.2f, -0.8f, 1.6f, 2, 1.6f), PartPose.offsetAndRotation(0, 6, 0, 0.06f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    private static final String[] LEGS = {"right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg"};
    private static final String[] MANE = {"mane1", "right_mane1", "left_mane1", "right_mane2", "left_mane2",
            "right_mane3", "left_mane3", "right_mane4", "left_mane4"};
    private static final String[] HANGING = {"dewlap_tuft", "right_dewlap_tuft", "left_dewlap_tuft", "chest_tuft"};

    public static final class Model extends EntityModel<GreatWhiteBullEntity> {
        private final ModelPart root, head, beard, tail, tailMid, tailTip;
        private final ModelPart[] legs = new ModelPart[4], shins = new ModelPart[4], hocks = new ModelPart[2];
        private final ModelPart[] mane = new ModelPart[MANE.length], hanging = new ModelPart[HANGING.length];

        public Model(ModelPart root) {
            this.root = root;
            ModelPart body = root.getChild("body");
            this.head = body.getChild("head");
            this.beard = head.getChild("beard");
            this.tail = body.getChild("tail");
            this.tailMid = tail.getChild("tail_mid");
            this.tailTip = tailMid.getChild("tail_tip");
            for (int i = 0; i < 4; i++) {
                legs[i] = root.getChild(LEGS[i]);
                // the forelegs bend at the knee; the hind legs at the hock, between the gaskin and the cannon
                shins[i] = i < 2 ? legs[i].getChild(LEGS[i].replace("_leg", "_shin")) : legs[i].getChild(LEGS[i].replace("_leg", "_gaskin"));
            }
            for (int i = 0; i < 2; i++) hocks[i] = shins[i + 2].getChild(LEGS[i + 2].replace("_leg", "_shin"));
            for (int i = 0; i < MANE.length; i++) mane[i] = body.getChild(MANE[i]);
            for (int i = 0; i < HANGING.length; i++) hanging[i] = body.getChild(HANGING[i]);
        }

        @Override
        public void setupAnim(GreatWhiteBullEntity e, float swing, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            // the head carried low, lower still as it charges
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.6f;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.5f + 0.25f + amount * 0.15f;
            // a heavy, diagonal gait: each leg folding at its knee or hock as it swings forward
            for (int i = 0; i < 4; i++) {
                boolean diagonal = i == 0 || i == 3;
                float phase = swing * 0.6f + (diagonal ? 0 : Mth.PI);
                legs[i].xRot = Mth.cos(phase) * 1.1f * amount;
                float lift = Math.max(0, Mth.sin(phase)) * amount;
                if (i < 2) {
                    shins[i].xRot = lift * 0.9f;
                } else {
                    shins[i].xRot += lift * 0.5f;
                    hocks[i - 2].xRot -= lift * 0.7f;
                }
            }
            // the mane stirs, swept back as it charges; the hair under its neck swings
            for (int i = 0; i < mane.length; i++) {
                mane[i].xRot -= amount * 0.3f;
                mane[i].zRot += Mth.sin(age * 0.1f + i * 0.9f) * (0.04f + amount * 0.1f);
            }
            for (int i = 0; i < hanging.length; i++) {
                hanging[i].xRot += Mth.sin(age * 0.08f + i) * 0.05f - amount * 0.3f;
                hanging[i].zRot += Mth.sin(swing * 0.6f + i) * 0.12f * amount;
            }
            beard.xRot += Mth.sin(age * 0.09f) * 0.05f - amount * 0.2f;
            // a long tail, swaying: each joint a beat behind the one before; lifted as it runs
            tail.zRot = Mth.sin(age * 0.12f) * 0.25f;
            tail.xRot = 0.35f + Mth.sin(age * 0.09f) * 0.08f + amount * 0.4f;
            tailMid.zRot = Mth.sin(age * 0.12f - 0.9f) * 0.3f;
            tailMid.xRot += amount * 0.25f;
            tailTip.zRot = Mth.sin(age * 0.12f - 1.8f) * 0.35f;
            tailTip.xRot += amount * 0.2f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
