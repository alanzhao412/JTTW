package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.beast.TrueFormEntity;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Azure Dragon, the Dragon's true form: a Chinese dragon, long and serpentine, azure-scaled with
 * a golden belly, its head long, barbels trailing, horns swept back, a white mane, eyes burning gold.
 * Its body is a chain of segments, each turned a little from the last, a wave running down it so it
 * undulates as it swims through the air. Model and skin: {@code assets_source/azure_dragon.py}.
 */
public final class AzureDragonRenderer extends MobRenderer<TrueFormEntity, AzureDragonRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(JTTWEngine.MOD_ID, "azure_dragon"), "main");
    public static final ResourceLocation TEXTURE = new ResourceLocation(JTTWEngine.MOD_ID, "textures/entity/azure_dragon.png");
    private static final RenderType GLOW = RenderType.eyes(new ResourceLocation(JTTWEngine.MOD_ID, "textures/entity/azure_dragon_glow.png"));
    public static final float SCALE = 1.6f;
    public static final int SEGMENTS = 11;

    public AzureDragonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.2f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected void scale(TrueFormEntity e, PoseStack pose, float partial) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(TrueFormEntity e) {
        return TEXTURE;
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(31, 22).mirror(false).addBox(-4, -3.5f, -6, 8, 7, 7)
                .texOffs(91, 22).mirror(false).addBox(-3, -1.2f, -14.5f, 6, 4, 8.5f)
                .texOffs(232, 50).mirror(false).addBox(-3.6f, -2, -15.5f, 7.2f, 3, 2.2f)
                .texOffs(168, 50).mirror(false).addBox(-4.6f, -4.6f, -7.2f, 3.2f, 2, 5)
                .texOffs(168, 50).mirror(false).addBox(1.4f, -4.6f, -7.2f, 3.2f, 2, 5)
                .texOffs(46, 62).mirror(false).addBox(-2.7f, 2.8f, -14, 0.8f, 2.4f, 0.8f)
                .texOffs(46, 62).mirror(false).addBox(1.9f, 2.8f, -14, 0.8f, 2.4f, 0.8f)
                .texOffs(54, 50).mirror(false).addBox(-3, -5.5f, -3, 6, 2.5f, 7)
                .texOffs(145, 22).mirror(false).addBox(-4.6f, -1, -2, 1.5f, 6, 6)
                .texOffs(145, 22).mirror(false).addBox(3.1f, -1, -2, 1.5f, 6, 6), PartPose.offsetAndRotation(0, -9.2914f, -6, -0.12f, -0.15f, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 38).mirror(false).addBox(-2.7f, 0, -8.5f, 5.4f, 1.8f, 8.5f)
                .texOffs(51, 62).mirror(false).addBox(-2.2f, -1.6f, -8, 0.7f, 1.6f, 0.7f)
                .texOffs(51, 62).mirror(false).addBox(1.5f, -1.6f, -8, 0.7f, 1.6f, 0.7f)
                .texOffs(187, 50).mirror(false).addBox(-1.8f, 0.3f, -7, 3.6f, 0.6f, 6), PartPose.offsetAndRotation(0, 2.6f, -5.5f, 0.5f, 0, 0));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(119, 50).mirror(false).addBox(-0.8f, -0.8f, 0, 1.6f, 1.6f, 7), PartPose.offsetAndRotation(-2.3f, -4.5f, -2.5f, 0.55f, -0.28f, 0));
        PartDefinition rightHornTip = rightHorn.addOrReplaceChild("right_horn_tip", CubeListBuilder.create()
                .texOffs(208, 50).mirror(false).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 5), PartPose.offsetAndRotation(0, 0, 7, -0.45f, 0, 0));
        PartDefinition rightHornBranch = rightHorn.addOrReplaceChild("right_horn_branch", CubeListBuilder.create()
                .texOffs(26, 62).mirror(false).addBox(-0.5f, -0.5f, 0, 1, 1, 3.5f), PartPose.offsetAndRotation(0, 0, 3.5f, 0.9f, -0.4f, 0));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(119, 50).mirror(true).addBox(-0.8f, -0.8f, 0, 1.6f, 1.6f, 7), PartPose.offsetAndRotation(2.3f, -4.5f, -2.5f, 0.55f, 0.28f, 0));
        PartDefinition leftHornTip = leftHorn.addOrReplaceChild("left_horn_tip", CubeListBuilder.create()
                .texOffs(208, 50).mirror(true).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 5), PartPose.offsetAndRotation(0, 0, 7, -0.45f, 0, 0));
        PartDefinition leftHornBranch = leftHorn.addOrReplaceChild("left_horn_branch", CubeListBuilder.create()
                .texOffs(26, 62).mirror(true).addBox(-0.5f, -0.5f, 0, 1, 1, 3.5f), PartPose.offsetAndRotation(0, 0, 3.5f, 0.9f, 0.4f, 0));
        PartDefinition rightBarbel = head.addOrReplaceChild("right_barbel", CubeListBuilder.create()
                .texOffs(81, 50).mirror(false).addBox(-0.3f, -0.3f, 0, 0.6f, 0.6f, 9), PartPose.offsetAndRotation(-3.4f, 0.5f, -13.5f, -0.1f, -0.75f, 0));
        PartDefinition rightBarbelEnd = rightBarbel.addOrReplaceChild("right_barbel_end", CubeListBuilder.create()
                .texOffs(81, 50).mirror(false).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 9), PartPose.offsetAndRotation(0, 0, 9, 0.35f, 0.3f, 0));
        PartDefinition leftBarbel = head.addOrReplaceChild("left_barbel", CubeListBuilder.create()
                .texOffs(81, 50).mirror(true).addBox(-0.3f, -0.3f, 0, 0.6f, 0.6f, 9), PartPose.offsetAndRotation(3.4f, 0.5f, -13.5f, -0.1f, 0.75f, 0));
        PartDefinition leftBarbelEnd = leftBarbel.addOrReplaceChild("left_barbel_end", CubeListBuilder.create()
                .texOffs(81, 50).mirror(true).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 9), PartPose.offsetAndRotation(0, 0, 9, 0.35f, -0.3f, 0));
        PartDefinition seg0 = root.addOrReplaceChild("seg0", CubeListBuilder.create()
                .texOffs(43, 0).mirror(false).addBox(-5, -5, 0, 10, 10, 10)
                .texOffs(31, 38).mirror(false).addBox(-4.4f, 4.6f, 0, 8.8f, 0.8f, 10)
                .texOffs(223, 50).mirror(false).addBox(-0.5f, -8.2f, 1.5f, 1, 3.2f, 3)
                .texOffs(223, 50).mirror(false).addBox(-0.5f, -8.2f, 6.5f, 1, 3.2f, 3)
                .texOffs(162, 22).mirror(false).addBox(-4, -6.2f, 0, 8, 1.5f, 10), PartPose.offsetAndRotation(0, -9.7914f, -5, -0.35f, 0.1f, 0));
        PartDefinition seg1 = seg0.addOrReplaceChild("seg1", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-5.5f, -5.5f, 0, 11, 11, 10)
                .texOffs(70, 38).mirror(false).addBox(-4.9f, 5.1f, 0, 9.8f, 0.8f, 10)
                .texOffs(223, 50).mirror(false).addBox(-0.5f, -9.02f, 1.5f, 1, 3.52f, 3)
                .texOffs(223, 50).mirror(false).addBox(-0.5f, -9.02f, 6.5f, 1, 3.52f, 3)
                .texOffs(199, 22).mirror(false).addBox(-4.5f, -6.7f, 0, 9, 1.5f, 10), PartPose.offsetAndRotation(0, 0, 10, -0.5f, 0.15f, 0));
        PartDefinition seg2 = seg1.addOrReplaceChild("seg2", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-5.5f, -5.5f, 0, 11, 11, 10)
                .texOffs(70, 38).mirror(false).addBox(-4.9f, 5.1f, 0, 9.8f, 0.8f, 10)
                .texOffs(223, 50).mirror(false).addBox(-0.5f, -9.02f, 1.5f, 1, 3.52f, 3)
                .texOffs(223, 50).mirror(false).addBox(-0.5f, -9.02f, 6.5f, 1, 3.52f, 3)
                .texOffs(199, 22).mirror(false).addBox(-4.5f, -6.7f, 0, 9, 1.5f, 10), PartPose.offsetAndRotation(0, 0, 10, 0.4f, -0.03f, 0));
        PartDefinition seg3 = seg2.addOrReplaceChild("seg3", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-5.25f, -5.25f, 0, 10.5f, 10.5f, 10)
                .texOffs(70, 38).mirror(false).addBox(-4.65f, 4.85f, 0, 9.3f, 0.8f, 10)
                .texOffs(223, 50).mirror(false).addBox(-0.5f, -8.61f, 1.5f, 1, 3.36f, 3)
                .texOffs(223, 50).mirror(false).addBox(-0.5f, -8.61f, 6.5f, 1, 3.36f, 3), PartPose.offsetAndRotation(0, 0, 10, 0.9f, -0.22f, 0));
        PartDefinition seg4 = seg3.addOrReplaceChild("seg4", CubeListBuilder.create()
                .texOffs(43, 0).mirror(false).addBox(-5, -5, 0, 10, 10, 10)
                .texOffs(31, 38).mirror(false).addBox(-4.4f, 4.6f, 0, 8.8f, 0.8f, 10)
                .texOffs(223, 50).mirror(false).addBox(-0.5f, -8.2f, 1.5f, 1, 3.2f, 3)
                .texOffs(223, 50).mirror(false).addBox(-0.5f, -8.2f, 6.5f, 1, 3.2f, 3), PartPose.offsetAndRotation(0, 0, 10, 0.55f, -0.22f, 0));
        PartDefinition seg5 = seg4.addOrReplaceChild("seg5", CubeListBuilder.create()
                .texOffs(84, 0).mirror(false).addBox(-4.5f, -4.5f, 0, 9, 9, 10)
                .texOffs(111, 38).mirror(false).addBox(-3.9f, 4.1f, 0, 7.8f, 0.8f, 10)
                .texOffs(0, 62).mirror(false).addBox(-0.5f, -7.38f, 1.5f, 1, 2.88f, 3)
                .texOffs(0, 62).mirror(false).addBox(-0.5f, -7.38f, 6.5f, 1, 2.88f, 3), PartPose.offsetAndRotation(0, 0, 10, -0.45f, -0.1f, 0));
        PartDefinition seg6 = seg5.addOrReplaceChild("seg6", CubeListBuilder.create()
                .texOffs(123, 0).mirror(false).addBox(-4, -4, 0, 8, 8, 10)
                .texOffs(148, 38).mirror(false).addBox(-3.4f, 3.6f, 0, 6.8f, 0.8f, 10)
                .texOffs(0, 62).mirror(false).addBox(-0.5f, -6.56f, 1.5f, 1, 2.56f, 3)
                .texOffs(0, 62).mirror(false).addBox(-0.5f, -6.56f, 6.5f, 1, 2.56f, 3), PartPose.offsetAndRotation(0, 0, 10, -1, 0.12f, 0));
        PartDefinition seg7 = seg6.addOrReplaceChild("seg7", CubeListBuilder.create()
                .texOffs(160, 0).mirror(false).addBox(-3.5f, -3.5f, 0, 7, 7, 10)
                .texOffs(183, 38).mirror(false).addBox(-2.9f, 3.1f, 0, 5.8f, 0.8f, 10)
                .texOffs(0, 62).mirror(false).addBox(-0.5f, -5.74f, 1.5f, 1, 2.24f, 3)
                .texOffs(0, 62).mirror(false).addBox(-0.5f, -5.74f, 6.5f, 1, 2.24f, 3), PartPose.offsetAndRotation(0, 0, 10, -0.55f, 0.2f, 0));
        PartDefinition seg8 = seg7.addOrReplaceChild("seg8", CubeListBuilder.create()
                .texOffs(195, 0).mirror(false).addBox(-3, -3, 0, 6, 6, 10)
                .texOffs(216, 38).mirror(false).addBox(-2.4f, 2.6f, 0, 4.8f, 0.8f, 10)
                .texOffs(37, 62).mirror(false).addBox(-0.5f, -4.92f, 1.5f, 1, 1.92f, 3)
                .texOffs(37, 62).mirror(false).addBox(-0.5f, -4.92f, 6.5f, 1, 1.92f, 3), PartPose.offsetAndRotation(0, 0, 10, 0.45f, 0.22f, 0));
        PartDefinition seg9 = seg8.addOrReplaceChild("seg9", CubeListBuilder.create()
                .texOffs(0, 22).mirror(false).addBox(-2.3f, -2.3f, 0, 4.6f, 4.6f, 10)
                .texOffs(0, 50).mirror(false).addBox(-1.7f, 1.9f, 0, 3.4f, 0.8f, 10)
                .texOffs(37, 62).mirror(false).addBox(-0.5f, -3.772f, 1.5f, 1, 1.472f, 3)
                .texOffs(37, 62).mirror(false).addBox(-0.5f, -3.772f, 6.5f, 1, 1.472f, 3), PartPose.offsetAndRotation(0, 0, 10, 0.85f, 0.08f, 0));
        PartDefinition seg10 = seg9.addOrReplaceChild("seg10", CubeListBuilder.create()
                .texOffs(62, 22).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 10)
                .texOffs(29, 50).mirror(false).addBox(-1, 1.2f, 0, 2, 0.8f, 10), PartPose.offsetAndRotation(0, 0, 10, 0.4f, -0.1f, 0));
        PartDefinition tailTuft = seg10.addOrReplaceChild("tail_tuft", CubeListBuilder.create()
                .texOffs(122, 22).mirror(false).addBox(-2.5f, -3.5f, 0, 5, 7, 6)
                .texOffs(138, 50).mirror(false).addBox(-1.5f, -2, 5, 3, 4, 5), PartPose.offsetAndRotation(0, 0, 10, 0, 0, 0));
        PartDefinition rightFrontLeg = seg1.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                .texOffs(102, 50).mirror(false).addBox(-1.6f, 0, -1.6f, 3.2f, 5.5f, 3.2f), PartPose.offsetAndRotation(-5, 4, 5, 0.35f, 0, 0.45f));
        PartDefinition rightFrontLegLower = rightFrontLeg.addOrReplaceChild("right_front_leg_lower", CubeListBuilder.create()
                .texOffs(155, 50).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 4.5f, 2.6f)
                .texOffs(9, 62).mirror(false).addBox(-1.6f, 4, -2.2f, 3.2f, 1.2f, 3.2f), PartPose.offsetAndRotation(0, 5.5f, 0, -0.8f, 0, -0.55f));
        PartDefinition rightFrontLegClaw0 = rightFrontLegLower.addOrReplaceChild("right_front_leg_claw0", CubeListBuilder.create()
                .texOffs(56, 62).mirror(false).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(-1.1f, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition rightFrontLegClaw1 = rightFrontLegLower.addOrReplaceChild("right_front_leg_claw1", CubeListBuilder.create()
                .texOffs(56, 62).mirror(false).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(0, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition rightFrontLegClaw2 = rightFrontLegLower.addOrReplaceChild("right_front_leg_claw2", CubeListBuilder.create()
                .texOffs(56, 62).mirror(false).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(1.1f, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition leftFrontLeg = seg1.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                .texOffs(102, 50).mirror(true).addBox(-1.6f, 0, -1.6f, 3.2f, 5.5f, 3.2f), PartPose.offsetAndRotation(5, 4, 5, 0.35f, 0, -0.45f));
        PartDefinition leftFrontLegLower = leftFrontLeg.addOrReplaceChild("left_front_leg_lower", CubeListBuilder.create()
                .texOffs(155, 50).mirror(true).addBox(-1.3f, 0, -1.3f, 2.6f, 4.5f, 2.6f)
                .texOffs(9, 62).mirror(true).addBox(-1.6f, 4, -2.2f, 3.2f, 1.2f, 3.2f), PartPose.offsetAndRotation(0, 5.5f, 0, -0.8f, 0, 0.55f));
        PartDefinition leftFrontLegClaw0 = leftFrontLegLower.addOrReplaceChild("left_front_leg_claw0", CubeListBuilder.create()
                .texOffs(56, 62).mirror(true).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(-1.1f, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition leftFrontLegClaw1 = leftFrontLegLower.addOrReplaceChild("left_front_leg_claw1", CubeListBuilder.create()
                .texOffs(56, 62).mirror(true).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(0, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition leftFrontLegClaw2 = leftFrontLegLower.addOrReplaceChild("left_front_leg_claw2", CubeListBuilder.create()
                .texOffs(56, 62).mirror(true).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(1.1f, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition rightHindLeg = seg6.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                .texOffs(102, 50).mirror(false).addBox(-1.6f, 0, -1.6f, 3.2f, 5.5f, 3.2f), PartPose.offsetAndRotation(-3.5f, 2.5f, 5, 0.35f, 0, 0.45f));
        PartDefinition rightHindLegLower = rightHindLeg.addOrReplaceChild("right_hind_leg_lower", CubeListBuilder.create()
                .texOffs(155, 50).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 4.5f, 2.6f)
                .texOffs(9, 62).mirror(false).addBox(-1.6f, 4, -2.2f, 3.2f, 1.2f, 3.2f), PartPose.offsetAndRotation(0, 5.5f, 0, -0.8f, 0, -0.55f));
        PartDefinition rightHindLegClaw0 = rightHindLegLower.addOrReplaceChild("right_hind_leg_claw0", CubeListBuilder.create()
                .texOffs(56, 62).mirror(false).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(-1.1f, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition rightHindLegClaw1 = rightHindLegLower.addOrReplaceChild("right_hind_leg_claw1", CubeListBuilder.create()
                .texOffs(56, 62).mirror(false).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(0, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition rightHindLegClaw2 = rightHindLegLower.addOrReplaceChild("right_hind_leg_claw2", CubeListBuilder.create()
                .texOffs(56, 62).mirror(false).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(1.1f, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition leftHindLeg = seg6.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                .texOffs(102, 50).mirror(true).addBox(-1.6f, 0, -1.6f, 3.2f, 5.5f, 3.2f), PartPose.offsetAndRotation(3.5f, 2.5f, 5, 0.35f, 0, -0.45f));
        PartDefinition leftHindLegLower = leftHindLeg.addOrReplaceChild("left_hind_leg_lower", CubeListBuilder.create()
                .texOffs(155, 50).mirror(true).addBox(-1.3f, 0, -1.3f, 2.6f, 4.5f, 2.6f)
                .texOffs(9, 62).mirror(true).addBox(-1.6f, 4, -2.2f, 3.2f, 1.2f, 3.2f), PartPose.offsetAndRotation(0, 5.5f, 0, -0.8f, 0, 0.55f));
        PartDefinition leftHindLegClaw0 = leftHindLegLower.addOrReplaceChild("left_hind_leg_claw0", CubeListBuilder.create()
                .texOffs(56, 62).mirror(true).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(-1.1f, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition leftHindLegClaw1 = leftHindLegLower.addOrReplaceChild("left_hind_leg_claw1", CubeListBuilder.create()
                .texOffs(56, 62).mirror(true).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(0, 5, -2.2f, 0.8f, 0, 0));
        PartDefinition leftHindLegClaw2 = leftHindLegLower.addOrReplaceChild("left_hind_leg_claw2", CubeListBuilder.create()
                .texOffs(56, 62).mirror(true).addBox(-0.25f, -0.25f, -1.4f, 0.5f, 0.5f, 1.4f), PartPose.offsetAndRotation(1.1f, 5, -2.2f, 0.8f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<TrueFormEntity> {
        private final ModelPart root, head, jaw;
        private final ModelPart[] segs = new ModelPart[SEGMENTS];
        private final ModelPart[] legs = new ModelPart[4];
        private final ModelPart[] barbels = new ModelPart[2];

        public Model(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.jaw = head.getChild("jaw");
            ModelPart at = root;
            for (int i = 0; i < SEGMENTS; i++) {
                at = at.getChild("seg" + i);
                segs[i] = at;
            }
            legs[0] = segs[1].getChild("right_front_leg");
            legs[1] = segs[1].getChild("left_front_leg");
            legs[2] = segs[6].getChild("right_hind_leg");
            legs[3] = segs[6].getChild("left_hind_leg");
            barbels[0] = head.getChild("right_barbel");
            barbels[1] = head.getChild("left_barbel");
        }

        @Override
        public void setupAnim(TrueFormEntity e, float swing, float amount, float age, float yaw, float pitch) {
            // a wave running down its body, from the neck to the tail, side to side and a little up
            // and down: it swims through the air
            float speed = 0.11f + amount * 0.08f;
            // (over the coil it lies in: each segment's own turn, as built)
            for (int i = 0; i < SEGMENTS; i++) {
                float phase = age * speed - i * 0.75f;
                var rest = segs[i].getInitialPose();
                segs[i].yRot = rest.yRot + Mth.sin(phase) * (0.12f + i * 0.01f);
                segs[i].xRot = rest.xRot + Mth.sin(phase * 0.5f + 1.3f) * 0.07f;
            }
            var headRest = head.getInitialPose();
            head.yRot = headRest.yRot + yaw * Mth.DEG_TO_RAD * 0.6f + Mth.sin(age * speed + 0.75f) * 0.1f;
            head.xRot = headRest.xRot + pitch * Mth.DEG_TO_RAD * 0.5f;
            jaw.xRot = 0.5f + Mth.sin(age * 0.08f) * 0.08f;
            for (int k = 0; k < 2; k++) barbels[k].xRot = -0.1f + Mth.sin(age * 0.13f + k) * 0.18f;
            // its legs paddling slowly in the air
            for (int k = 0; k < 4; k++) legs[k].xRot = 0.35f + Mth.sin(age * 0.09f + k * 1.6f) * 0.35f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
            root.render(pose, vc, light, overlay, r, g, b, a);
        }
    }
}
