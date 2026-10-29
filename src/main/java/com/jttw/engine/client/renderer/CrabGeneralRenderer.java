package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.waters.CrabGeneralEntity;
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
 * The Crab General (蟹将): a massive crab carrying a general's regalia. A broad domed carapace,
 * sculpted (its swellings and grooves, tubercles, five spines down each front margin, red-orange
 * deepening to crimson, cream-spotted), eyes on stalks, mouthparts working under its brow; four pairs
 * of jointed, spined legs with dark spiky tips; two huge claws, the right the larger, serrated and
 * tipped black. On the front of its crown a gilded general's helmet with a jewelled frontlet, a tall
 * red plume and two pheasant plumes (翎子) arching back; a small seat of teal lamellar plates behind
 * it, teal plates on its claws, and at the shell's rear a narrow post with four back flags (靠旗) rising
 * close-set from it and a red cape hanging over the rear edge, so its shell shows. It scuttles on
 * its legs, its claws open and shut and snap closed on a blow, its eyestalks twitch, its mouthparts
 * work; its flags, plumes and cape stir in the water. Model and skin:
 * {@code assets_source/crab_general.py}.
 */
public final class CrabGeneralRenderer extends MobRenderer<CrabGeneralEntity, CrabGeneralRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("crab_general"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/crab_general.png");
    private static final RenderType GLOW = RenderType.eyes(JTTWEngine.id("textures/entity/crab_general_glow.png"));
    /** How much larger than its pixels it is drawn (crab_general.py prints its reach). */
    public static final float SIZE = 1.0f;

    public CrabGeneralRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.2f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected void scale(CrabGeneralEntity e, PoseStack pose, float partial) {
        pose.scale(SIZE, SIZE, SIZE);
    }

    @Override
    public ResourceLocation getTextureLocation(CrabGeneralEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(CrabGeneralEntity e) {
        return super.shouldShowName(e) && SeaCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 63).mirror(false).addBox(-10.9f, 14.8f, -11, 21.8f, 2.2f, 2)
                .texOffs(49, 63).mirror(false).addBox(-13.1f, 14.8f, -9, 26.2f, 2.2f, 2)
                .texOffs(108, 63).mirror(false).addBox(-14.6f, 14.8f, -7, 29.2f, 2.2f, 2)
                .texOffs(173, 63).mirror(false).addBox(-15.4f, 14.8f, -5, 30.8f, 2.2f, 2)
                .texOffs(0, 69).mirror(false).addBox(-15.6f, 14.8f, -3, 31.2f, 2.2f, 2)
                .texOffs(69, 69).mirror(false).addBox(-15.35f, 14.8f, -1, 30.7f, 2.2f, 2)
                .texOffs(136, 69).mirror(false).addBox(-14.5f, 14.8f, 1, 29, 2.2f, 2)
                .texOffs(0, 75).mirror(false).addBox(-13.1f, 14.8f, 3, 26.2f, 2.2f, 2)
                .texOffs(59, 75).mirror(false).addBox(-11.4f, 14.8f, 5, 22.8f, 2.2f, 2)
                .texOffs(110, 75).mirror(false).addBox(-9.6f, 14.8f, 7, 19.2f, 2.2f, 2)
                .texOffs(155, 75).mirror(false).addBox(-7.9f, 14.8f, 9, 15.8f, 2.2f, 2)
                .texOffs(192, 75).mirror(false).addBox(-9.05f, 13.2f, -10.3f, 18.1f, 1.6f, 2.05f)
                .texOffs(0, 81).mirror(false).addBox(-11.6f, 13.2f, -8.25f, 23.19f, 1.6f, 2.05f)
                .texOffs(55, 81).mirror(false).addBox(-13.3f, 13.2f, -6.21f, 26.6f, 1.6f, 2.05f)
                .texOffs(116, 81).mirror(false).addBox(-14.27f, 13.2f, -4.16f, 28.53f, 1.6f, 2.05f)
                .texOffs(181, 81).mirror(false).addBox(-14.3f, 13.2f, -2.12f, 28.6f, 1.6f, 2.05f)
                .texOffs(0, 87).mirror(false).addBox(-13.43f, 13.2f, -0.07f, 26.86f, 1.6f, 2.05f)
                .texOffs(61, 87).mirror(false).addBox(-11.96f, 13.2f, 1.98f, 23.92f, 1.6f, 2.05f)
                .texOffs(116, 87).mirror(false).addBox(-10.18f, 13.2f, 4.02f, 20.37f, 1.6f, 2.05f)
                .texOffs(165, 87).mirror(false).addBox(-8.34f, 13.2f, 6.07f, 16.68f, 1.6f, 2.05f)
                .texOffs(206, 87).mirror(false).addBox(-6.68f, 13.2f, 8.11f, 13.35f, 1.6f, 2.05f)
                .texOffs(127, 93).mirror(false).addBox(-7.91f, 11.8f, -9, 15.82f, 1.4f, 1.96f)
                .texOffs(164, 93).mirror(false).addBox(-10.24f, 11.8f, -7.04f, 20.48f, 1.4f, 1.96f)
                .texOffs(0, 99).mirror(false).addBox(-11.73f, 11.8f, -5.09f, 23.47f, 1.4f, 1.96f)
                .texOffs(53, 99).mirror(false).addBox(-12.41f, 11.8f, -3.13f, 24.82f, 1.4f, 1.96f)
                .texOffs(108, 99).mirror(false).addBox(-12.08f, 11.8f, -1.18f, 24.16f, 1.4f, 1.96f)
                .texOffs(163, 99).mirror(false).addBox(-10.84f, 11.8f, 0.78f, 21.67f, 1.4f, 1.96f)
                .texOffs(212, 99).mirror(false).addBox(-9.2f, 11.8f, 2.73f, 18.4f, 1.4f, 1.96f)
                .texOffs(0, 104).mirror(false).addBox(-7.44f, 11.8f, 4.69f, 14.88f, 1.4f, 1.96f)
                .texOffs(35, 104).mirror(false).addBox(-5.82f, 11.8f, 6.64f, 11.64f, 1.4f, 1.96f)
                .texOffs(64, 104).mirror(false).addBox(-6.41f, 10.7f, -7.2f, 12.83f, 1.1f, 1.95f)
                .texOffs(95, 104).mirror(false).addBox(-8.52f, 10.7f, -5.25f, 17.04f, 1.1f, 1.95f)
                .texOffs(136, 104).mirror(false).addBox(-9.53f, 10.7f, -3.3f, 19.06f, 1.1f, 1.95f)
                .texOffs(181, 104).mirror(false).addBox(-9.36f, 10.7f, -1.35f, 18.72f, 1.1f, 1.95f)
                .texOffs(0, 109).mirror(false).addBox(-8.05f, 10.7f, 0.59f, 16.1f, 1.1f, 1.95f)
                .texOffs(39, 109).mirror(false).addBox(-6.35f, 10.7f, 2.54f, 12.7f, 1.1f, 1.95f)
                .texOffs(70, 109).mirror(false).addBox(-4.69f, 10.7f, 4.49f, 9.37f, 1.1f, 1.95f)
                .texOffs(95, 109).mirror(false).addBox(-4.75f, 9.9f, -5, 9.5f, 0.8f, 2.2f)
                .texOffs(122, 109).mirror(false).addBox(-6.18f, 9.9f, -2.8f, 12.36f, 0.8f, 2.2f)
                .texOffs(155, 109).mirror(false).addBox(-5.32f, 9.9f, -0.6f, 10.64f, 0.8f, 2.2f)
                .texOffs(184, 109).mirror(false).addBox(-3.4f, 9.9f, 1.6f, 6.8f, 0.8f, 2.2f)
                .texOffs(106, 114).mirror(false).addBox(-0.7f, 9.93f, -6.2f, 1.4f, 0.98f, 1.4f)
                .texOffs(106, 114).mirror(false).addBox(-0.55f, 9.295f, -3.35f, 1.1f, 0.77f, 1.1f)
                .texOffs(106, 114).mirror(false).addBox(-0.65f, 9.985f, 3.55f, 1.3f, 0.91f, 1.3f)
                .texOffs(177, 114).mirror(false).addBox(-0.5f, 11.25f, 6.1f, 1, 0.7f, 1)
                .texOffs(106, 114).mirror(false).addBox(1.8f, 10.04f, -7, 1.2f, 0.84f, 1.2f)
                .texOffs(106, 114).mirror(false).addBox(-3, 10.04f, -7, 1.2f, 0.84f, 1.2f)
                .texOffs(177, 114).mirror(false).addBox(3.1f, 9.35f, -4.1f, 1, 0.7f, 1)
                .texOffs(177, 114).mirror(false).addBox(-4.1f, 9.35f, -4.1f, 1, 0.7f, 1)
                .texOffs(177, 114).mirror(false).addBox(1.35f, 9.405f, 0.55f, 0.9f, 0.63f, 0.9f)
                .texOffs(177, 114).mirror(false).addBox(-2.25f, 9.405f, 0.55f, 0.9f, 0.63f, 0.9f)
                .texOffs(177, 114).mirror(false).addBox(1.7f, 10.15f, 4.9f, 1, 0.7f, 1)
                .texOffs(177, 114).mirror(false).addBox(-2.7f, 10.15f, 4.9f, 1, 0.7f, 1)
                .texOffs(106, 114).mirror(false).addBox(6.2f, 10.04f, -5.6f, 1.2f, 0.84f, 1.2f)
                .texOffs(106, 114).mirror(false).addBox(-7.4f, 10.04f, -5.6f, 1.2f, 0.84f, 1.2f)
                .texOffs(106, 114).mirror(false).addBox(8.45f, 10.095f, -3.55f, 1.1f, 0.77f, 1.1f)
                .texOffs(106, 114).mirror(false).addBox(-9.55f, 10.095f, -3.55f, 1.1f, 0.77f, 1.1f)
                .texOffs(177, 114).mirror(false).addBox(10.7f, 11.25f, -1.5f, 1, 0.7f, 1)
                .texOffs(177, 114).mirror(false).addBox(-11.7f, 11.25f, -1.5f, 1, 0.7f, 1)
                .texOffs(177, 114).mirror(false).addBox(7.35f, 10.205f, -1.65f, 0.9f, 0.63f, 0.9f)
                .texOffs(177, 114).mirror(false).addBox(-8.25f, 10.205f, -1.65f, 0.9f, 0.63f, 0.9f)
                .texOffs(177, 114).mirror(false).addBox(9.9f, 11.25f, 1.7f, 1, 0.7f, 1)
                .texOffs(177, 114).mirror(false).addBox(-10.9f, 11.25f, 1.7f, 1, 0.7f, 1)
                .texOffs(106, 114).mirror(false).addBox(7.05f, 11.195f, 2.45f, 1.1f, 0.77f, 1.1f)
                .texOffs(106, 114).mirror(false).addBox(-8.15f, 11.195f, 2.45f, 1.1f, 0.77f, 1.1f)
                .texOffs(177, 114).mirror(false).addBox(5.15f, 11.305f, 5.75f, 0.9f, 0.63f, 0.9f)
                .texOffs(177, 114).mirror(false).addBox(-6.05f, 11.305f, 5.75f, 0.9f, 0.63f, 0.9f)
                .texOffs(177, 114).mirror(false).addBox(12.15f, 12.705f, -3.85f, 0.9f, 0.63f, 0.9f)
                .texOffs(177, 114).mirror(false).addBox(-13.05f, 12.705f, -3.85f, 0.9f, 0.63f, 0.9f)
                .texOffs(177, 114).mirror(false).addBox(4.7f, 11.25f, -8.9f, 1, 0.7f, 1)
                .texOffs(177, 114).mirror(false).addBox(-5.7f, 11.25f, -8.9f, 1, 0.7f, 1)
                .texOffs(177, 114).mirror(false).addBox(8.15f, 12.705f, 5.35f, 0.9f, 0.63f, 0.9f)
                .texOffs(177, 114).mirror(false).addBox(-9.05f, 12.705f, 5.35f, 0.9f, 0.63f, 0.9f)
                .texOffs(5, 0).mirror(false).addBox(-10.6f, 17, -8.4f, 21.2f, 2.4f, 16.4f)
                .texOffs(187, 30).mirror(false).addBox(-2.6f, 19.2f, -3, 5.2f, 0.8f, 9)
                .texOffs(0, 93).mirror(false).addBox(-6.6f, 17, -10.6f, 13.2f, 2, 2.4f), PartPose.offsetAndRotation(0, 0.8379f, 0, 0, 0, 0));
        PartDefinition rightTooth0 = body.addOrReplaceChild("right_tooth_0", CubeListBuilder.create()
                .texOffs(205, 109).mirror(false).addBox(-1.1f, -0.75f, -0.8f, 1.1f, 1.5f, 1.6f)
                .texOffs(182, 114).mirror(false).addBox(-2, -0.45f, -0.45f, 1, 0.9f, 0.9f), PartPose.offsetAndRotation(-9.72f, 15.6f, -9.6f, 0, -0.55f, 0.12f));
        PartDefinition leftTooth0 = body.addOrReplaceChild("left_tooth_0", CubeListBuilder.create()
                .texOffs(205, 109).mirror(true).addBox(0, -0.75f, -0.8f, 1.1f, 1.5f, 1.6f)
                .texOffs(182, 114).mirror(true).addBox(1, -0.45f, -0.45f, 1, 0.9f, 0.9f), PartPose.offsetAndRotation(9.72f, 15.6f, -9.6f, 0, 0.55f, -0.12f));
        PartDefinition rightTooth1 = body.addOrReplaceChild("right_tooth_1", CubeListBuilder.create()
                .texOffs(205, 109).mirror(false).addBox(-1.1f, -0.75f, -0.8f, 1.1f, 1.5f, 1.6f)
                .texOffs(182, 114).mirror(false).addBox(-2, -0.45f, -0.45f, 1, 0.9f, 0.9f), PartPose.offsetAndRotation(-12.16f, 15.6f, -7.6f, 0, -0.45f, 0.12f));
        PartDefinition leftTooth1 = body.addOrReplaceChild("left_tooth_1", CubeListBuilder.create()
                .texOffs(205, 109).mirror(true).addBox(0, -0.75f, -0.8f, 1.1f, 1.5f, 1.6f)
                .texOffs(182, 114).mirror(true).addBox(1, -0.45f, -0.45f, 1, 0.9f, 0.9f), PartPose.offsetAndRotation(12.16f, 15.6f, -7.6f, 0, 0.45f, -0.12f));
        PartDefinition rightTooth2 = body.addOrReplaceChild("right_tooth_2", CubeListBuilder.create()
                .texOffs(205, 109).mirror(false).addBox(-1.1f, -0.75f, -0.8f, 1.1f, 1.5f, 1.6f)
                .texOffs(182, 114).mirror(false).addBox(-2, -0.45f, -0.45f, 1, 0.9f, 0.9f), PartPose.offsetAndRotation(-13.84f, 15.6f, -5.6f, 0, -0.35f, 0.12f));
        PartDefinition leftTooth2 = body.addOrReplaceChild("left_tooth_2", CubeListBuilder.create()
                .texOffs(205, 109).mirror(true).addBox(0, -0.75f, -0.8f, 1.1f, 1.5f, 1.6f)
                .texOffs(182, 114).mirror(true).addBox(1, -0.45f, -0.45f, 1, 0.9f, 0.9f), PartPose.offsetAndRotation(13.84f, 15.6f, -5.6f, 0, 0.35f, -0.12f));
        PartDefinition rightTooth3 = body.addOrReplaceChild("right_tooth_3", CubeListBuilder.create()
                .texOffs(205, 109).mirror(false).addBox(-1.1f, -0.75f, -0.8f, 1.1f, 1.5f, 1.6f)
                .texOffs(182, 114).mirror(false).addBox(-2, -0.45f, -0.45f, 1, 0.9f, 0.9f), PartPose.offsetAndRotation(-14.86f, 15.6f, -3.7f, 0, -0.25f, 0.12f));
        PartDefinition leftTooth3 = body.addOrReplaceChild("left_tooth_3", CubeListBuilder.create()
                .texOffs(205, 109).mirror(true).addBox(0, -0.75f, -0.8f, 1.1f, 1.5f, 1.6f)
                .texOffs(182, 114).mirror(true).addBox(1, -0.45f, -0.45f, 1, 0.9f, 0.9f), PartPose.offsetAndRotation(14.86f, 15.6f, -3.7f, 0, 0.25f, -0.12f));
        PartDefinition rightTooth4 = body.addOrReplaceChild("right_tooth_4", CubeListBuilder.create()
                .texOffs(214, 109).mirror(false).addBox(-2.31f, -0.75f, -0.8f, 2.31f, 1.5f, 1.6f)
                .texOffs(187, 114).mirror(false).addBox(-4.2f, -0.45f, -0.45f, 2.1f, 0.9f, 0.9f), PartPose.offsetAndRotation(-15.125f, 15.6f, -1.7f, 0, -0.15f, 0.22f));
        PartDefinition leftTooth4 = body.addOrReplaceChild("left_tooth_4", CubeListBuilder.create()
                .texOffs(214, 109).mirror(true).addBox(0, -0.75f, -0.8f, 2.31f, 1.5f, 1.6f)
                .texOffs(187, 114).mirror(true).addBox(2.1f, -0.45f, -0.45f, 2.1f, 0.9f, 0.9f), PartPose.offsetAndRotation(15.125f, 15.6f, -1.7f, 0, 0.15f, -0.22f));
        PartDefinition rightBrowTooth0 = body.addOrReplaceChild("right_brow_tooth_0", CubeListBuilder.create()
                .texOffs(225, 109).mirror(false).addBox(-0.7f, -0.6f, -1.4f, 1.4f, 1.3f, 1.6f), PartPose.offsetAndRotation(-1.2f, 15.6f, -11, 0, 0, 0));
        PartDefinition leftBrowTooth0 = body.addOrReplaceChild("left_brow_tooth_0", CubeListBuilder.create()
                .texOffs(225, 109).mirror(true).addBox(-0.7f, -0.6f, -1.4f, 1.4f, 1.3f, 1.6f), PartPose.offsetAndRotation(1.2f, 15.6f, -11, 0, -0, 0));
        PartDefinition rightBrowTooth1 = body.addOrReplaceChild("right_brow_tooth_1", CubeListBuilder.create()
                .texOffs(225, 109).mirror(false).addBox(-0.7f, -0.6f, -1.4f, 1.4f, 1.3f, 1.6f), PartPose.offsetAndRotation(-3.6f, 15.6f, -11, 0, 0.15f, 0));
        PartDefinition leftBrowTooth1 = body.addOrReplaceChild("left_brow_tooth_1", CubeListBuilder.create()
                .texOffs(225, 109).mirror(true).addBox(-0.7f, -0.6f, -1.4f, 1.4f, 1.3f, 1.6f), PartPose.offsetAndRotation(3.6f, 15.6f, -11, 0, -0.15f, 0));
        PartDefinition rightEyestalk = body.addOrReplaceChild("right_eyestalk", CubeListBuilder.create()
                .texOffs(21, 44).mirror(false).addBox(-0.85f, -6.2f, -0.85f, 1.7f, 6.4f, 1.7f)
                .texOffs(35, 93).mirror(false).addBox(-1.5f, -0.6f, -1.5f, 3, 1.4f, 3), PartPose.offsetAndRotation(-6.6f, 15, -10.4f, -0.3f, 0.2f, 0.3f));
        PartDefinition leftEyestalk = body.addOrReplaceChild("left_eyestalk", CubeListBuilder.create()
                .texOffs(21, 44).mirror(true).addBox(-0.85f, -6.2f, -0.85f, 1.7f, 6.4f, 1.7f)
                .texOffs(35, 93).mirror(true).addBox(-1.5f, -0.6f, -1.5f, 3, 1.4f, 3), PartPose.offsetAndRotation(6.6f, 15, -10.4f, -0.3f, -0.2f, -0.3f));
        PartDefinition rightEye = rightEyestalk.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(104, 44).mirror(false).addBox(-1.6f, -3, -1.6f, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(0, -6, 0, 0, 0, 0));
        PartDefinition leftEye = leftEyestalk.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(104, 44).mirror(true).addBox(-1.6f, -3, -1.6f, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(0, -6, 0, 0, 0, 0));
        PartDefinition rightMaxilliped = body.addOrReplaceChild("right_maxilliped", CubeListBuilder.create()
                .texOffs(131, 55).mirror(false).addBox(-1.3f, 0, -0.5f, 2.6f, 4.2f, 0.9f)
                .texOffs(115, 114).mirror(false).addBox(-0.9f, 3.8f, -0.4f, 1.8f, 1.4f, 0.7f), PartPose.offsetAndRotation(-1.5f, 17, -10.6f, -0.15f, 0, 0.06f));
        PartDefinition leftMaxilliped = body.addOrReplaceChild("left_maxilliped", CubeListBuilder.create()
                .texOffs(131, 55).mirror(true).addBox(-1.3f, 0, -0.5f, 2.6f, 4.2f, 0.9f)
                .texOffs(115, 114).mirror(true).addBox(-0.9f, 3.8f, -0.4f, 1.8f, 1.4f, 0.7f), PartPose.offsetAndRotation(1.5f, 17, -10.6f, -0.15f, 0, -0.06f));
        PartDefinition rightAntenna = body.addOrReplaceChild("right_antenna", CubeListBuilder.create()
                .texOffs(48, 93).mirror(false).addBox(-0.25f, -0.25f, -4, 0.5f, 0.5f, 4), PartPose.offsetAndRotation(-2.6f, 15.8f, -10.8f, -0.9f, 0.5f, 0));
        PartDefinition leftAntenna = body.addOrReplaceChild("left_antenna", CubeListBuilder.create()
                .texOffs(48, 93).mirror(true).addBox(-0.25f, -0.25f, -4, 0.5f, 0.5f, 4), PartPose.offsetAndRotation(2.6f, 15.8f, -10.8f, -0.9f, -0.5f, 0));
        PartDefinition rightAntennule = body.addOrReplaceChild("right_antennule", CubeListBuilder.create()
                .texOffs(122, 114).mirror(false).addBox(-0.35f, -0.35f, -1.8f, 0.7f, 0.7f, 1.8f), PartPose.offsetAndRotation(-1, 15.6f, -11, -1.2f, 0.2f, 0));
        PartDefinition leftAntennule = body.addOrReplaceChild("left_antennule", CubeListBuilder.create()
                .texOffs(122, 114).mirror(true).addBox(-0.35f, -0.35f, -1.8f, 0.7f, 0.7f, 1.8f), PartPose.offsetAndRotation(1, 15.6f, -11, -1.2f, -0.2f, 0));
        PartDefinition rightLeg0 = body.addOrReplaceChild("right_leg_0", CubeListBuilder.create()
                .texOffs(140, 55).mirror(false).addBox(-8.6f, -1.35f, -1.15f, 8.6f, 2.7f, 2.3f)
                .texOffs(129, 114).mirror(false).addBox(-3.612f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f)
                .texOffs(129, 114).mirror(false).addBox(-6.708f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f), PartPose.offsetAndRotation(-11.76f, 16.4f, -4.4f, 0, -0.5f, 0.75f));
        PartDefinition leftLeg0 = body.addOrReplaceChild("left_leg_0", CubeListBuilder.create()
                .texOffs(140, 55).mirror(true).addBox(0, -1.35f, -1.15f, 8.6f, 2.7f, 2.3f)
                .texOffs(129, 114).mirror(true).addBox(2.912f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f)
                .texOffs(129, 114).mirror(true).addBox(6.008f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f), PartPose.offsetAndRotation(11.76f, 16.4f, -4.4f, 0, 0.5f, -0.75f));
        PartDefinition rightLeg0Carpus = rightLeg0.addOrReplaceChild("right_leg_0_carpus", CubeListBuilder.create()
                .texOffs(59, 93).mirror(false).addBox(-4.4f, -1.15f, -1, 4.4f, 2.3f, 2)
                .texOffs(196, 114).mirror(false).addBox(-1.4f, -1.9f, -0.3f, 0.6f, 1, 0.6f), PartPose.offsetAndRotation(-8.4f, 0, 0, 0, 0, -1));
        PartDefinition leftLeg0Carpus = leftLeg0.addOrReplaceChild("left_leg_0_carpus", CubeListBuilder.create()
                .texOffs(59, 93).mirror(true).addBox(0, -1.15f, -1, 4.4f, 2.3f, 2)
                .texOffs(196, 114).mirror(true).addBox(0.8f, -1.9f, -0.3f, 0.6f, 1, 0.6f), PartPose.offsetAndRotation(8.4f, 0, 0, 0, 0, 1));
        PartDefinition rightLeg0Propodus = rightLeg0Carpus.addOrReplaceChild("right_leg_0_propodus", CubeListBuilder.create()
                .texOffs(234, 109).mirror(false).addBox(-5.8f, -0.95f, -0.9f, 5.8f, 1.9f, 1.8f), PartPose.offsetAndRotation(-4.2f, 0, 0, 0, 0, -0.95f));
        PartDefinition leftLeg0Propodus = leftLeg0Carpus.addOrReplaceChild("left_leg_0_propodus", CubeListBuilder.create()
                .texOffs(234, 109).mirror(true).addBox(0, -0.95f, -0.9f, 5.8f, 1.9f, 1.8f), PartPose.offsetAndRotation(4.2f, 0, 0, 0, 0, 0.95f));
        PartDefinition rightLeg0Dactyl = rightLeg0Propodus.addOrReplaceChild("right_leg_0_dactyl", CubeListBuilder.create()
                .texOffs(206, 44).mirror(false).addBox(-0.7f, -0.3f, -0.7f, 1.4f, 4.8f, 1.4f), PartPose.offsetAndRotation(-5.6f, 0, 0, 0, 0, 1.4f));
        PartDefinition leftLeg0Dactyl = leftLeg0Propodus.addOrReplaceChild("left_leg_0_dactyl", CubeListBuilder.create()
                .texOffs(206, 44).mirror(true).addBox(-0.7f, -0.3f, -0.7f, 1.4f, 4.8f, 1.4f), PartPose.offsetAndRotation(5.6f, 0, 0, 0, 0, -1.4f));
        PartDefinition rightLeg1 = body.addOrReplaceChild("right_leg_1", CubeListBuilder.create()
                .texOffs(165, 55).mirror(false).addBox(-9.2f, -1.35f, -1.15f, 9.2f, 2.7f, 2.3f)
                .texOffs(129, 114).mirror(false).addBox(-3.864f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f)
                .texOffs(129, 114).mirror(false).addBox(-7.176f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f), PartPose.offsetAndRotation(-12.1f, 16.4f, -0.8f, 0, -0.12f, 0.75f));
        PartDefinition leftLeg1 = body.addOrReplaceChild("left_leg_1", CubeListBuilder.create()
                .texOffs(165, 55).mirror(true).addBox(0, -1.35f, -1.15f, 9.2f, 2.7f, 2.3f)
                .texOffs(129, 114).mirror(true).addBox(3.164f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f)
                .texOffs(129, 114).mirror(true).addBox(6.476f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f), PartPose.offsetAndRotation(12.1f, 16.4f, -0.8f, 0, 0.12f, -0.75f));
        PartDefinition rightLeg1Carpus = rightLeg1.addOrReplaceChild("right_leg_1_carpus", CubeListBuilder.create()
                .texOffs(59, 93).mirror(false).addBox(-4.4f, -1.15f, -1, 4.4f, 2.3f, 2)
                .texOffs(196, 114).mirror(false).addBox(-1.4f, -1.9f, -0.3f, 0.6f, 1, 0.6f), PartPose.offsetAndRotation(-9, 0, 0, 0, 0, -1));
        PartDefinition leftLeg1Carpus = leftLeg1.addOrReplaceChild("left_leg_1_carpus", CubeListBuilder.create()
                .texOffs(59, 93).mirror(true).addBox(0, -1.15f, -1, 4.4f, 2.3f, 2)
                .texOffs(196, 114).mirror(true).addBox(0.8f, -1.9f, -0.3f, 0.6f, 1, 0.6f), PartPose.offsetAndRotation(9, 0, 0, 0, 0, 1));
        PartDefinition rightLeg1Propodus = rightLeg1Carpus.addOrReplaceChild("right_leg_1_propodus", CubeListBuilder.create()
                .texOffs(234, 109).mirror(false).addBox(-5.8f, -0.95f, -0.9f, 5.8f, 1.9f, 1.8f), PartPose.offsetAndRotation(-4.2f, 0, 0, 0, 0, -0.95f));
        PartDefinition leftLeg1Propodus = leftLeg1Carpus.addOrReplaceChild("left_leg_1_propodus", CubeListBuilder.create()
                .texOffs(234, 109).mirror(true).addBox(0, -0.95f, -0.9f, 5.8f, 1.9f, 1.8f), PartPose.offsetAndRotation(4.2f, 0, 0, 0, 0, 0.95f));
        PartDefinition rightLeg1Dactyl = rightLeg1Propodus.addOrReplaceChild("right_leg_1_dactyl", CubeListBuilder.create()
                .texOffs(206, 44).mirror(false).addBox(-0.7f, -0.3f, -0.7f, 1.4f, 4.8f, 1.4f), PartPose.offsetAndRotation(-5.6f, 0, 0, 0, 0, 1.4f));
        PartDefinition leftLeg1Dactyl = leftLeg1Propodus.addOrReplaceChild("left_leg_1_dactyl", CubeListBuilder.create()
                .texOffs(206, 44).mirror(true).addBox(-0.7f, -0.3f, -0.7f, 1.4f, 4.8f, 1.4f), PartPose.offsetAndRotation(5.6f, 0, 0, 0, 0, -1.4f));
        PartDefinition rightLeg2 = body.addOrReplaceChild("right_leg_2", CubeListBuilder.create()
                .texOffs(140, 55).mirror(false).addBox(-8.8f, -1.35f, -1.15f, 8.8f, 2.7f, 2.3f)
                .texOffs(129, 114).mirror(false).addBox(-3.696f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f)
                .texOffs(129, 114).mirror(false).addBox(-6.864f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f), PartPose.offsetAndRotation(-10.06f, 16.4f, 2.8f, 0, 0.3f, 0.75f));
        PartDefinition leftLeg2 = body.addOrReplaceChild("left_leg_2", CubeListBuilder.create()
                .texOffs(140, 55).mirror(true).addBox(0, -1.35f, -1.15f, 8.8f, 2.7f, 2.3f)
                .texOffs(129, 114).mirror(true).addBox(2.996f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f)
                .texOffs(129, 114).mirror(true).addBox(6.164f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f), PartPose.offsetAndRotation(10.06f, 16.4f, 2.8f, 0, -0.3f, -0.75f));
        PartDefinition rightLeg2Carpus = rightLeg2.addOrReplaceChild("right_leg_2_carpus", CubeListBuilder.create()
                .texOffs(59, 93).mirror(false).addBox(-4.4f, -1.15f, -1, 4.4f, 2.3f, 2)
                .texOffs(196, 114).mirror(false).addBox(-1.4f, -1.9f, -0.3f, 0.6f, 1, 0.6f), PartPose.offsetAndRotation(-8.6f, 0, 0, 0, 0, -1));
        PartDefinition leftLeg2Carpus = leftLeg2.addOrReplaceChild("left_leg_2_carpus", CubeListBuilder.create()
                .texOffs(59, 93).mirror(true).addBox(0, -1.15f, -1, 4.4f, 2.3f, 2)
                .texOffs(196, 114).mirror(true).addBox(0.8f, -1.9f, -0.3f, 0.6f, 1, 0.6f), PartPose.offsetAndRotation(8.6f, 0, 0, 0, 0, 1));
        PartDefinition rightLeg2Propodus = rightLeg2Carpus.addOrReplaceChild("right_leg_2_propodus", CubeListBuilder.create()
                .texOffs(234, 109).mirror(false).addBox(-5.8f, -0.95f, -0.9f, 5.8f, 1.9f, 1.8f), PartPose.offsetAndRotation(-4.2f, 0, 0, 0, 0, -0.95f));
        PartDefinition leftLeg2Propodus = leftLeg2Carpus.addOrReplaceChild("left_leg_2_propodus", CubeListBuilder.create()
                .texOffs(234, 109).mirror(true).addBox(0, -0.95f, -0.9f, 5.8f, 1.9f, 1.8f), PartPose.offsetAndRotation(4.2f, 0, 0, 0, 0, 0.95f));
        PartDefinition rightLeg2Dactyl = rightLeg2Propodus.addOrReplaceChild("right_leg_2_dactyl", CubeListBuilder.create()
                .texOffs(206, 44).mirror(false).addBox(-0.7f, -0.3f, -0.7f, 1.4f, 4.8f, 1.4f), PartPose.offsetAndRotation(-5.6f, 0, 0, 0, 0, 1.4f));
        PartDefinition leftLeg2Dactyl = leftLeg2Propodus.addOrReplaceChild("left_leg_2_dactyl", CubeListBuilder.create()
                .texOffs(206, 44).mirror(true).addBox(-0.7f, -0.3f, -0.7f, 1.4f, 4.8f, 1.4f), PartPose.offsetAndRotation(5.6f, 0, 0, 0, 0, -1.4f));
        PartDefinition rightLeg3 = body.addOrReplaceChild("right_leg_3", CubeListBuilder.create()
                .texOffs(192, 55).mirror(false).addBox(-7.8f, -1.35f, -1.15f, 7.8f, 2.7f, 2.3f)
                .texOffs(129, 114).mirror(false).addBox(-3.276f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f)
                .texOffs(129, 114).mirror(false).addBox(-6.084f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f), PartPose.offsetAndRotation(-7.12f, 16.4f, 6.2f, 0, 0.72f, 0.75f));
        PartDefinition leftLeg3 = body.addOrReplaceChild("left_leg_3", CubeListBuilder.create()
                .texOffs(192, 55).mirror(true).addBox(0, -1.35f, -1.15f, 7.8f, 2.7f, 2.3f)
                .texOffs(129, 114).mirror(true).addBox(2.576f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f)
                .texOffs(129, 114).mirror(true).addBox(5.384f, -2.2f, -0.35f, 0.7f, 1.1f, 0.7f), PartPose.offsetAndRotation(7.12f, 16.4f, 6.2f, 0, -0.72f, -0.75f));
        PartDefinition rightLeg3Carpus = rightLeg3.addOrReplaceChild("right_leg_3_carpus", CubeListBuilder.create()
                .texOffs(59, 93).mirror(false).addBox(-4.4f, -1.15f, -1, 4.4f, 2.3f, 2)
                .texOffs(196, 114).mirror(false).addBox(-1.4f, -1.9f, -0.3f, 0.6f, 1, 0.6f), PartPose.offsetAndRotation(-7.6f, 0, 0, 0, 0, -1));
        PartDefinition leftLeg3Carpus = leftLeg3.addOrReplaceChild("left_leg_3_carpus", CubeListBuilder.create()
                .texOffs(59, 93).mirror(true).addBox(0, -1.15f, -1, 4.4f, 2.3f, 2)
                .texOffs(196, 114).mirror(true).addBox(0.8f, -1.9f, -0.3f, 0.6f, 1, 0.6f), PartPose.offsetAndRotation(7.6f, 0, 0, 0, 0, 1));
        PartDefinition rightLeg3Propodus = rightLeg3Carpus.addOrReplaceChild("right_leg_3_propodus", CubeListBuilder.create()
                .texOffs(234, 109).mirror(false).addBox(-5.8f, -0.95f, -0.9f, 5.8f, 1.9f, 1.8f), PartPose.offsetAndRotation(-4.2f, 0, 0, 0, 0, -0.95f));
        PartDefinition leftLeg3Propodus = leftLeg3Carpus.addOrReplaceChild("left_leg_3_propodus", CubeListBuilder.create()
                .texOffs(234, 109).mirror(true).addBox(0, -0.95f, -0.9f, 5.8f, 1.9f, 1.8f), PartPose.offsetAndRotation(4.2f, 0, 0, 0, 0, 0.95f));
        PartDefinition rightLeg3Dactyl = rightLeg3Propodus.addOrReplaceChild("right_leg_3_dactyl", CubeListBuilder.create()
                .texOffs(206, 44).mirror(false).addBox(-0.7f, -0.3f, -0.7f, 1.4f, 4.8f, 1.4f), PartPose.offsetAndRotation(-5.6f, 0, 0, 0, 0, 1.4f));
        PartDefinition leftLeg3Dactyl = leftLeg3Propodus.addOrReplaceChild("left_leg_3_dactyl", CubeListBuilder.create()
                .texOffs(206, 44).mirror(true).addBox(-0.7f, -0.3f, -0.7f, 1.4f, 4.8f, 1.4f), PartPose.offsetAndRotation(5.6f, 0, 0, 0, 0, -1.4f));
        PartDefinition helmet = body.addOrReplaceChild("helmet", CubeListBuilder.create()
                .texOffs(27, 30).mirror(false).addBox(-4.588f, -1.184f, -4.588f, 9.176f, 1.332f, 9.176f)
                .texOffs(198, 0).mirror(false).addBox(-3.848f, -5.328f, -3.848f, 7.696f, 4.292f, 7.696f)
                .texOffs(121, 44).mirror(false).addBox(-2.812f, -6.586f, -2.812f, 5.624f, 1.332f, 5.624f)
                .texOffs(74, 93).mirror(false).addBox(-0.592f, -8.732f, -0.592f, 1.184f, 2.22f, 1.184f)
                .texOffs(83, 93).mirror(false).addBox(-1.036f, -10.064f, -1.036f, 2.072f, 1.48f, 2.072f)
                .texOffs(0, 114).mirror(false).addBox(-4.144f, -1.184f, 3.404f, 8.288f, 2.96f, 0.888f)
                .texOffs(134, 114).mirror(false).addBox(-0.888f, -2.146f, -5.18f, 1.776f, 1.776f, 0.888f), PartPose.offsetAndRotation(0, 10.2f, -5.6f, -0.08f, 0, 0));
        PartDefinition frontlet = helmet.addOrReplaceChild("frontlet", CubeListBuilder.create()
                .texOffs(215, 44).mirror(false).addBox(-2.96f, -4.884f, -0.222f, 5.92f, 5.624f, 0.444f), PartPose.offsetAndRotation(0, -5.032f, -4.07f, -0.12f, 0, 0));
        PartDefinition jewel = frontlet.addOrReplaceChild("jewel", CubeListBuilder.create()
                .texOffs(141, 114).mirror(false).addBox(-0.74f, -0.74f, -0.37f, 1.48f, 1.48f, 0.444f), PartPose.offsetAndRotation(0, -2.072f, -0.296f, 0, 0, 0));
        PartDefinition plume = helmet.addOrReplaceChild("plume", CubeListBuilder.create()
                .texOffs(68, 30).mirror(false).addBox(-2, -7, -2, 4, 7.2f, 4)
                .texOffs(230, 44).mirror(false).addBox(-1.4f, -10.4f, -1.4f, 2.8f, 3.6f, 2.8f), PartPose.offsetAndRotation(0, -9.916f, 0, 0, 0, 0));
        PartDefinition plumeTail = plume.addOrReplaceChild("plume_tail", CubeListBuilder.create()
                .texOffs(218, 30).mirror(false).addBox(-1.6f, 0, -0.8f, 3.2f, 7.6f, 1.6f), PartPose.offsetAndRotation(0, -6, 1.4f, 1.05f, 0, 0));
        PartDefinition rightHelmwing = helmet.addOrReplaceChild("right_helmwing", CubeListBuilder.create()
                .texOffs(231, 30).mirror(false).addBox(-0.222f, -5.328f, 0, 0.444f, 5.328f, 3.7f), PartPose.offsetAndRotation(-3.7f, -2.516f, 0.444f, 0, 0.3f, 0.55f));
        PartDefinition leftHelmwing = helmet.addOrReplaceChild("left_helmwing", CubeListBuilder.create()
                .texOffs(231, 30).mirror(true).addBox(-0.222f, -5.328f, 0, 0.444f, 5.328f, 3.7f), PartPose.offsetAndRotation(3.7f, -2.516f, 0.444f, 0, -0.3f, -0.55f));
        PartDefinition rightPompom = helmet.addOrReplaceChild("right_pompom", CubeListBuilder.create()
                .texOffs(21, 114).mirror(false).addBox(-0.888f, -0.888f, -0.888f, 1.776f, 1.776f, 1.776f), PartPose.offsetAndRotation(-4.44f, -1.48f, -2.22f, 0, 0, 0));
        PartDefinition leftPompom = helmet.addOrReplaceChild("left_pompom", CubeListBuilder.create()
                .texOffs(21, 114).mirror(true).addBox(-0.888f, -0.888f, -0.888f, 1.776f, 1.776f, 1.776f), PartPose.offsetAndRotation(4.44f, -1.48f, -2.22f, 0, 0, 0));
        PartDefinition rightPompomBack = helmet.addOrReplaceChild("right_pompom_back", CubeListBuilder.create()
                .texOffs(21, 114).mirror(false).addBox(-0.74f, -0.74f, -0.74f, 1.48f, 1.48f, 1.48f), PartPose.offsetAndRotation(-3.404f, -1.48f, 2.96f, 0, 0, 0));
        PartDefinition leftPompomBack = helmet.addOrReplaceChild("left_pompom_back", CubeListBuilder.create()
                .texOffs(21, 114).mirror(true).addBox(-0.74f, -0.74f, -0.74f, 1.48f, 1.48f, 1.48f), PartPose.offsetAndRotation(3.404f, -1.48f, 2.96f, 0, 0, 0));
        PartDefinition rightFeather = helmet.addOrReplaceChild("right_feather", CubeListBuilder.create()
                .texOffs(242, 30).mirror(false).addBox(-1, -8, -0.3f, 2, 8.2f, 0.6f), PartPose.offsetAndRotation(-1.628f, -5.92f, 1.628f, 0.1f, 0, -0.3f));
        PartDefinition leftFeather = helmet.addOrReplaceChild("left_feather", CubeListBuilder.create()
                .texOffs(242, 30).mirror(true).addBox(-1, -8, -0.3f, 2, 8.2f, 0.6f), PartPose.offsetAndRotation(1.628f, -5.92f, 1.628f, 0.1f, 0, 0.3f));
        PartDefinition rightFeather2 = rightFeather.addOrReplaceChild("right_feather_2", CubeListBuilder.create()
                .texOffs(242, 30).mirror(false).addBox(-0.95f, -8, -0.3f, 1.9f, 8.2f, 0.6f), PartPose.offsetAndRotation(0, -7.8f, 0, -0.16f, 0, -0.06f));
        PartDefinition leftFeather2 = leftFeather.addOrReplaceChild("left_feather_2", CubeListBuilder.create()
                .texOffs(242, 30).mirror(true).addBox(-0.95f, -8, -0.3f, 1.9f, 8.2f, 0.6f), PartPose.offsetAndRotation(0, -7.8f, 0, -0.16f, 0, 0.06f));
        PartDefinition rightFeather3 = rightFeather2.addOrReplaceChild("right_feather_3", CubeListBuilder.create()
                .texOffs(242, 30).mirror(false).addBox(-0.85f, -8, -0.3f, 1.7f, 8.2f, 0.6f), PartPose.offsetAndRotation(0, -7.8f, 0, -0.26f, 0, -0.05f));
        PartDefinition leftFeather3 = leftFeather2.addOrReplaceChild("left_feather_3", CubeListBuilder.create()
                .texOffs(242, 30).mirror(true).addBox(-0.85f, -8, -0.3f, 1.7f, 8.2f, 0.6f), PartPose.offsetAndRotation(0, -7.8f, 0, -0.26f, 0, 0.05f));
        PartDefinition rightFeather4 = rightFeather3.addOrReplaceChild("right_feather_4", CubeListBuilder.create()
                .texOffs(30, 44).mirror(false).addBox(-0.75f, -7.4f, -0.25f, 1.5f, 7.6f, 0.5f), PartPose.offsetAndRotation(0, -7.8f, 0, -0.34f, 0, -0.04f));
        PartDefinition leftFeather4 = leftFeather3.addOrReplaceChild("left_feather_4", CubeListBuilder.create()
                .texOffs(30, 44).mirror(true).addBox(-0.75f, -7.4f, -0.25f, 1.5f, 7.6f, 0.5f), PartPose.offsetAndRotation(0, -7.8f, 0, -0.34f, 0, 0.04f));
        PartDefinition rightFeather5 = rightFeather4.addOrReplaceChild("right_feather_5", CubeListBuilder.create()
                .texOffs(146, 44).mirror(false).addBox(-0.6f, -6.8f, -0.25f, 1.2f, 7, 0.5f), PartPose.offsetAndRotation(0, -7.2f, 0, -0.45f, 0, -0.03f));
        PartDefinition leftFeather5 = leftFeather4.addOrReplaceChild("left_feather_5", CubeListBuilder.create()
                .texOffs(146, 44).mirror(true).addBox(-0.6f, -6.8f, -0.25f, 1.2f, 7, 0.5f), PartPose.offsetAndRotation(0, -7.2f, 0, -0.45f, 0, 0.03f));
        PartDefinition saddle = body.addOrReplaceChild("saddle", CubeListBuilder.create()
                .texOffs(0, 55).mirror(false).addBox(-3.6f, -0.6f, 0, 7.2f, 1.2f, 4.6f)
                .texOffs(30, 114).mirror(false).addBox(-2.7f, -1.3f, 0.5f, 5.4f, 0.8f, 3)
                .texOffs(148, 114).mirror(false).addBox(-1.2f, -1.8f, 1, 2.4f, 0.6f, 2)
                .texOffs(49, 114).mirror(false).addBox(-2.7f, -3.2f, 3.5f, 5.4f, 2.6f, 0.9f), PartPose.offsetAndRotation(0, 9.7f, 0.4f, 0.08f, 0, 0));
        PartDefinition rightSaddleFlap = saddle.addOrReplaceChild("right_saddle_flap", CubeListBuilder.create()
                .texOffs(27, 55).mirror(false).addBox(-0.5f, 0, -2, 0.8f, 3, 4), PartPose.offsetAndRotation(-3.4f, 0, 2.3f, 0, 0, 0.62f));
        PartDefinition leftSaddleFlap = saddle.addOrReplaceChild("left_saddle_flap", CubeListBuilder.create()
                .texOffs(27, 55).mirror(true).addBox(-0.3f, 0, -2, 0.8f, 3, 4), PartPose.offsetAndRotation(3.4f, 0, 2.3f, 0, 0, -0.62f));
        PartDefinition flagPost = body.addOrReplaceChild("flag_post", CubeListBuilder.create()
                .texOffs(153, 44).mirror(false).addBox(-1.6f, -4.4f, -1.1f, 3.2f, 4.8f, 2.2f)
                .texOffs(64, 114).mirror(false).addBox(-2, -0.6f, -1.5f, 4, 1, 3), PartPose.offsetAndRotation(0, 12, 8.4f, 0, 0, 0));
        PartDefinition cape = flagPost.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(38, 55).mirror(false).addBox(-4.4f, 0, -0.3f, 8.8f, 5.6f, 0.6f), PartPose.offsetAndRotation(0, 0.2f, 1.4f, 0.42f, 0, 0));
        PartDefinition cape2 = cape.addOrReplaceChild("cape_2", CubeListBuilder.create()
                .texOffs(59, 55).mirror(false).addBox(-5, 0, -0.3f, 10, 5.2f, 0.6f), PartPose.offsetAndRotation(0, 5.2f, 0, 0.55f, 0, 0));
        PartDefinition rightFlag1 = flagPost.addOrReplaceChild("right_flag_1", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.45f, -27, -0.45f, 0.9f, 27.4f, 0.9f)
                .texOffs(96, 93).mirror(false).addBox(-0.65f, -30.4f, -0.3f, 1.3f, 3.6f, 0.6f)
                .texOffs(159, 114).mirror(false).addBox(-0.7f, -27.4f, -0.7f, 1.4f, 1, 1.4f)
                .texOffs(82, 55).mirror(false).addBox(-1.2f, -26.6f, -1.2f, 2.4f, 3.6f, 2.4f), PartPose.offsetAndRotation(-0.6f, -4.2f, -0.2f, -0.1f, 0, -0.12f));
        PartDefinition leftFlag1 = flagPost.addOrReplaceChild("left_flag_1", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(-0.45f, -27, -0.45f, 0.9f, 27.4f, 0.9f)
                .texOffs(96, 93).mirror(true).addBox(-0.65f, -30.4f, -0.3f, 1.3f, 3.6f, 0.6f)
                .texOffs(159, 114).mirror(true).addBox(-0.7f, -27.4f, -0.7f, 1.4f, 1, 1.4f)
                .texOffs(82, 55).mirror(true).addBox(-1.2f, -26.6f, -1.2f, 2.4f, 3.6f, 2.4f), PartPose.offsetAndRotation(0.6f, -4.2f, -0.2f, -0.1f, 0, 0.12f));
        PartDefinition rightFlag1Cloth = rightFlag1.addOrReplaceChild("right_flag_1_cloth", CubeListBuilder.create()
                .texOffs(119, 0).mirror(false).addBox(-10, 0, -0.3f, 10, 13.6f, 0.6f), PartPose.offsetAndRotation(-0.4f, -22.6f, 0, 0, 0.1f, 0));
        PartDefinition leftFlag1Cloth = leftFlag1.addOrReplaceChild("left_flag_1_cloth", CubeListBuilder.create()
                .texOffs(119, 0).mirror(true).addBox(0, 0, -0.3f, 10, 13.6f, 0.6f), PartPose.offsetAndRotation(0.4f, -22.6f, 0, 0, -0.1f, 0));
        PartDefinition rightFlag2 = flagPost.addOrReplaceChild("right_flag_2", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.45f, -27, -0.45f, 0.9f, 27.4f, 0.9f)
                .texOffs(96, 93).mirror(false).addBox(-0.65f, -30.4f, -0.3f, 1.3f, 3.6f, 0.6f)
                .texOffs(159, 114).mirror(false).addBox(-0.7f, -27.4f, -0.7f, 1.4f, 1, 1.4f)
                .texOffs(82, 55).mirror(false).addBox(-1.2f, -26.6f, -1.2f, 2.4f, 3.6f, 2.4f), PartPose.offsetAndRotation(-1.2f, -4.2f, 0.3f, -0.16f, 0, -0.36f));
        PartDefinition leftFlag2 = flagPost.addOrReplaceChild("left_flag_2", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(-0.45f, -27, -0.45f, 0.9f, 27.4f, 0.9f)
                .texOffs(96, 93).mirror(true).addBox(-0.65f, -30.4f, -0.3f, 1.3f, 3.6f, 0.6f)
                .texOffs(159, 114).mirror(true).addBox(-0.7f, -27.4f, -0.7f, 1.4f, 1, 1.4f)
                .texOffs(82, 55).mirror(true).addBox(-1.2f, -26.6f, -1.2f, 2.4f, 3.6f, 2.4f), PartPose.offsetAndRotation(1.2f, -4.2f, 0.3f, -0.16f, 0, 0.36f));
        PartDefinition rightFlag2Cloth = rightFlag2.addOrReplaceChild("right_flag_2_cloth", CubeListBuilder.create()
                .texOffs(119, 0).mirror(false).addBox(-10, 0, -0.3f, 10, 13.6f, 0.6f), PartPose.offsetAndRotation(-0.4f, -22.6f, 0, 0, 0.1f, 0));
        PartDefinition leftFlag2Cloth = leftFlag2.addOrReplaceChild("left_flag_2_cloth", CubeListBuilder.create()
                .texOffs(119, 0).mirror(true).addBox(0, 0, -0.3f, 10, 13.6f, 0.6f), PartPose.offsetAndRotation(0.4f, -22.6f, 0, 0, -0.1f, 0));
        PartDefinition rightClaw = body.addOrReplaceChild("right_claw", CubeListBuilder.create()
                .texOffs(169, 0).mirror(false).addBox(-2.34f, -2.21f, -8.06f, 4.68f, 4.42f, 8.06f)
                .texOffs(37, 44).mirror(false).addBox(-2.99f, -3.25f, -7.54f, 5.98f, 1.43f, 6.5f)
                .texOffs(79, 114).mirror(false).addBox(-0.65f, -3.9f, -6.24f, 1.3f, 1.3f, 1.3f)
                .texOffs(79, 114).mirror(false).addBox(-0.65f, -3.9f, -3.38f, 1.3f, 1.3f, 1.3f), PartPose.offsetAndRotation(-7.2f, 16.4f, -9.2f, -1.1f, 1.3f, 0.1f));
        PartDefinition rightWrist = rightClaw.addOrReplaceChild("right_wrist", CubeListBuilder.create()
                .texOffs(85, 30).mirror(false).addBox(-2.6f, -2.47f, -6.24f, 5.2f, 4.94f, 6.24f)
                .texOffs(168, 44).mirror(false).addBox(-2.925f, -2.795f, -1.82f, 5.85f, 5.59f, 1.3f)
                .texOffs(88, 114).mirror(false).addBox(-0.715f, -4.03f, -5.2f, 1.43f, 1.82f, 1.82f)
                .texOffs(88, 114).mirror(false).addBox(2.08f, -0.78f, -4.68f, 1.82f, 1.43f, 1.43f), PartPose.offsetAndRotation(0, 0, -7.8f, 0.2f, -1.3f, 0));
        PartDefinition rightPincer = rightWrist.addOrReplaceChild("right_pincer", CubeListBuilder.create()
                .texOffs(84, 0).mirror(false).addBox(-2.99f, -4.94f, -10.14f, 5.98f, 8.84f, 10.14f)
                .texOffs(139, 30).mirror(false).addBox(-1.43f, -5.85f, -9.36f, 2.86f, 1.04f, 8.58f)
                .texOffs(112, 30).mirror(false).addBox(-2.08f, 0.52f, -17.16f, 4.16f, 3.25f, 7.28f)
                .texOffs(95, 55).mirror(false).addBox(-1.365f, 1.04f, -20.54f, 2.73f, 2.21f, 3.64f)
                .texOffs(97, 114).mirror(false).addBox(-0.78f, -0.39f, -16.12f, 1.56f, 1.04f, 1.43f)
                .texOffs(97, 114).mirror(false).addBox(-0.78f, -0.39f, -13.52f, 1.56f, 1.04f, 1.43f)
                .texOffs(97, 114).mirror(false).addBox(-0.78f, -0.39f, -11.18f, 1.56f, 1.04f, 1.43f), PartPose.offsetAndRotation(0, 0, -5.98f, 0.2f, -0.7f, 0));
        PartDefinition rightFinger = rightPincer.addOrReplaceChild("right_finger", CubeListBuilder.create()
                .texOffs(0, 30).mirror(false).addBox(-1.95f, -1.56f, -8.58f, 3.9f, 3.38f, 8.58f)
                .texOffs(95, 55).mirror(false).addBox(-1.3f, -0.78f, -11.96f, 2.6f, 2.34f, 3.64f)
                .texOffs(97, 114).mirror(false).addBox(-0.78f, 1.56f, -7.54f, 1.56f, 1.04f, 1.43f)
                .texOffs(97, 114).mirror(false).addBox(-0.78f, 1.56f, -4.94f, 1.56f, 1.04f, 1.43f), PartPose.offsetAndRotation(0, -2.08f, -9.62f, -0.3f, 0, 0));
        PartDefinition leftClaw = body.addOrReplaceChild("left_claw", CubeListBuilder.create()
                .texOffs(164, 30).mirror(true).addBox(-1.8f, -1.7f, -6.2f, 3.6f, 3.4f, 6.2f)
                .texOffs(110, 55).mirror(true).addBox(-2.3f, -2.5f, -5.8f, 4.6f, 1.1f, 5)
                .texOffs(196, 114).mirror(true).addBox(-0.5f, -3, -4.8f, 1, 1, 1)
                .texOffs(196, 114).mirror(true).addBox(-0.5f, -3, -2.6f, 1, 1, 1), PartPose.offsetAndRotation(7.2f, 16.4f, -9.2f, -1.1f, -1.3f, -0.1f));
        PartDefinition leftWrist = leftClaw.addOrReplaceChild("left_wrist", CubeListBuilder.create()
                .texOffs(64, 44).mirror(true).addBox(-2, -1.9f, -4.8f, 4, 3.8f, 4.8f)
                .texOffs(215, 55).mirror(true).addBox(-2.25f, -2.15f, -1.4f, 4.5f, 4.3f, 1)
                .texOffs(88, 114).mirror(true).addBox(-0.55f, -3.1f, -4, 1.1f, 1.4f, 1.4f)
                .texOffs(88, 114).mirror(true).addBox(-3, -0.6f, -3.6f, 1.4f, 1.1f, 1.1f), PartPose.offsetAndRotation(0, 0, -6, 0.2f, 1.3f, 0));
        PartDefinition leftPincer = leftWrist.addOrReplaceChild("left_pincer", CubeListBuilder.create()
                .texOffs(142, 0).mirror(true).addBox(-2.3f, -3.8f, -7.8f, 4.6f, 6.8f, 7.8f)
                .texOffs(185, 44).mirror(true).addBox(-1.1f, -4.5f, -7.2f, 2.2f, 0.8f, 6.6f)
                .texOffs(83, 44).mirror(true).addBox(-1.6f, 0.4f, -13.2f, 3.2f, 2.5f, 5.6f)
                .texOffs(103, 93).mirror(true).addBox(-1.05f, 0.8f, -15.8f, 2.1f, 1.7f, 2.8f)
                .texOffs(168, 114).mirror(true).addBox(-0.6f, -0.3f, -12.4f, 1.2f, 0.8f, 1.1f)
                .texOffs(168, 114).mirror(true).addBox(-0.6f, -0.3f, -10.4f, 1.2f, 0.8f, 1.1f)
                .texOffs(168, 114).mirror(true).addBox(-0.6f, -0.3f, -8.6f, 1.2f, 0.8f, 1.1f), PartPose.offsetAndRotation(0, 0, -4.6f, 0.2f, 0.7f, 0));
        PartDefinition leftFinger = leftPincer.addOrReplaceChild("left_finger", CubeListBuilder.create()
                .texOffs(0, 44).mirror(true).addBox(-1.5f, -1.2f, -6.6f, 3, 2.6f, 6.6f)
                .texOffs(116, 93).mirror(true).addBox(-1, -0.6f, -9.2f, 2, 1.8f, 2.8f)
                .texOffs(168, 114).mirror(true).addBox(-0.6f, 1.2f, -5.8f, 1.2f, 0.8f, 1.1f)
                .texOffs(168, 114).mirror(true).addBox(-0.6f, 1.2f, -3.8f, 1.2f, 0.8f, 1.1f), PartPose.offsetAndRotation(0, -1.6f, -7.4f, -0.3f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<CrabGeneralEntity> {
        private static final String[] SIDE = {"right", "left"};
        private final ModelPart root, body, helmet, plumeTail, post, cape, cape2;
        /** Per side: [leg][merus, carpus, propodus, dactyl]. */
        private final ModelPart[][][] legs = new ModelPart[2][4][4];
        /** Per side: arm, wrist, palm, finger. */
        private final ModelPart[][] claws = new ModelPart[2][4];
        private final ModelPart[] eyestalks = new ModelPart[2], maxillipeds = new ModelPart[2], antennae = new ModelPart[2];
        private final ModelPart[][] flags = new ModelPart[2][2], cloths = new ModelPart[2][2], feathers = new ModelPart[2][5];

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            helmet = body.getChild("helmet");
            plumeTail = helmet.getChild("plume").getChild("plume_tail");
            post = body.getChild("flag_post");
            cape = post.getChild("cape");
            cape2 = cape.getChild("cape_2");
            for (int s = 0; s < 2; s++) {
                String side = SIDE[s];
                for (int n = 0; n < 4; n++) {
                    String leg = side + "_leg_" + n;
                    ModelPart m = body.getChild(leg), c = m.getChild(leg + "_carpus"), p = c.getChild(leg + "_propodus");
                    legs[s][n] = new ModelPart[]{m, c, p, p.getChild(leg + "_dactyl")};
                }
                ModelPart arm = body.getChild(side + "_claw"), wrist = arm.getChild(side + "_wrist"), palm = wrist.getChild(side + "_pincer");
                claws[s] = new ModelPart[]{arm, wrist, palm, palm.getChild(side + "_finger")};
                eyestalks[s] = body.getChild(side + "_eyestalk");
                maxillipeds[s] = body.getChild(side + "_maxilliped");
                antennae[s] = body.getChild(side + "_antenna");
                for (int n = 0; n < 2; n++) {
                    flags[s][n] = post.getChild(side + "_flag_" + (n + 1));
                    cloths[s][n] = flags[s][n].getChild(side + "_flag_" + (n + 1) + "_cloth");
                }
                ModelPart f = helmet.getChild(side + "_feather");
                feathers[s][0] = f;
                for (int n = 1; n < 5; n++) feathers[s][n] = f = f.getChild(side + "_feather_" + (n + 1));
            }
        }

        private static void turn(ModelPart p, float x, float y, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.yRot = p.getInitialPose().yRot + y;
            p.zRot = p.getInitialPose().zRot + z;
        }

        @Override
        public void setupAnim(CrabGeneralEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            boolean aloft = !e.onGround();
            float w = swing * 0.9f;
            for (int s = 0; s < 2; s++) {
                // the right's turns are the left's mirrored: what lifts or swings back a right leg is negated on the left
                float sg = s == 0 ? 1 : -1;
                for (int n = 0; n < 4; n++) {
                    // two alternating sets of four, as a crab walks: legs 0 and 2 of one side with 1 and 3 of the other
                    float ph = w + ((n + s) % 2) * Mth.PI + n * 0.35f;
                    float lift = Math.max(0, Mth.sin(ph)) * 0.38f * amount;
                    float reach = Mth.cos(ph) * 0.28f * amount;
                    float idle = Mth.sin(age * 0.05f + n * 1.7f + s * 2.3f) * 0.025f;
                    // springing, its legs spread and hang
                    float hang = aloft ? 0.25f : 0;
                    ModelPart[] l = legs[s][n];
                    turn(l[0], 0, sg * reach, sg * (lift + idle + hang));
                    turn(l[1], 0, 0, sg * (-lift * 0.5f + hang * 0.6f));
                    turn(l[2], 0, 0, sg * (lift * 0.3f - hang * 0.4f));
                    turn(l[3], 0, 0, 0);
                }
                // the claws: held up, opening and closing slowly; on a blow they lunge and snap shut
                float breathe = Mth.sin(age * 0.07f + s * 1.9f);
                ModelPart[] c = claws[s];
                turn(c[0], -strike * 0.45f + breathe * 0.04f + Mth.cos(w) * 0.05f * amount, sg * strike * 0.25f, 0);
                turn(c[1], 0, sg * strike * 0.45f, 0);
                turn(c[2], strike * 0.2f, 0, 0);
                // the finger: open (turned up) by `open`, shut on a blow; the crusher now and then gapes wide
                float open = 0.14f + 0.14f * (breathe + 1) * 0.5f + (s == 0 ? Math.max(0, Mth.sin(age * 0.023f)) * 0.18f : 0);
                c[3].yRot = 0;
                c[3].zRot = 0;
                c[3].xRot = -open * (1 - strike) + 0.04f * strike;
                // the eyestalks twitch about, and turn after what it watches
                turn(eyestalks[s], Mth.sin(age * 0.13f + s) * 0.06f + pitch * Mth.DEG_TO_RAD * 0.3f,
                        yaw * Mth.DEG_TO_RAD * 0.5f + Mth.sin(age * 0.09f + s * 2) * 0.08f, Mth.sin(age * 0.11f + s * 3) * 0.05f * sg);
                // the mouthparts work, as doors opening and closing; the antennae flick
                turn(maxillipeds[s], 0, sg * (0.12f + 0.12f * Mth.sin(age * 0.55f + s * 1.3f)), 0);
                turn(antennae[s], Mth.sin(age * 0.3f + s) * 0.15f, sg * Mth.sin(age * 0.21f + s) * 0.1f, 0);
                // the flags: their staffs sway, their cloths flutter on them
                for (int n = 0; n < 2; n++) {
                    turn(flags[s][n], Mth.sin(age * 0.06f + n) * 0.03f - amount * 0.08f, 0, sg * Mth.sin(age * 0.05f + n * 0.8f) * 0.03f);
                    turn(cloths[s][n], 0, sg * (Mth.sin(age * 0.17f + n * 1.4f + s) * 0.22f + amount * 0.25f), 0);
                }
                // the pheasant plumes bob, each section following the one below
                for (int n = 0; n < 5; n++) {
                    turn(feathers[s][n], Mth.sin(age * 0.08f - n * 0.6f) * (0.03f + 0.025f * n) - amount * 0.05f * n,
                            0, sg * Mth.sin(age * 0.06f - n * 0.5f + s) * 0.03f);
                }
            }
            // the body: a slow bob, a sway from side to side as it walks; it rears a little on a blow
            turn(body, -strike * 0.08f + (aloft ? -0.1f : 0), 0, Mth.sin(w) * 0.035f * amount);
            body.y = body.getInitialPose().y + Mth.sin(age * 0.1f) * 0.25f;
            turn(helmet, 0, 0, 0);
            turn(plumeTail, amount * 0.3f + Mth.sin(age * 0.1f) * 0.06f, 0, 0);
            turn(cape, Mth.sin(age * 0.09f) * 0.05f + amount * 0.25f, 0, 0);
            turn(cape2, Mth.sin(age * 0.09f - 0.8f) * 0.08f - amount * 0.1f, 0, Mth.sin(age * 0.07f) * 0.04f);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
