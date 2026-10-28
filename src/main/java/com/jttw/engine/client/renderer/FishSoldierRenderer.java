package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.waters.FishSoldierEntity;
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
 * The Fish Soldier (鱼兵), a carp of the Dragon King's host stood up as a man: a real fish's head,
 * its thick-lipped mouth gaping, two pairs of barbels hanging from it, round glassy gold-ringed eyes,
 * gill plates flaring over red gills; individual red-gold scales, the lateral line down each side, a
 * dorsal fin rising over its back like a crest, a deep forked tail fin, pectoral fins fanning from
 * its elbows; the army's teal and gold: helmet with a red plume and gold fin-wings, fish-scale
 * cuirass with a gold breast mirror, sash, tassets, greaves; a sky-piercer halberd with a red tassel
 * and the wave pennant. Walking, it steps and its tail swings; swimming, its legs kick, its tail
 * beats side to side and its fins row; its gills open and close and its mouth gulps as it breathes;
 * it thrusts with the halberd. Drawn translucent (its fins half clear), the glint of its eyes
 * full-bright. Model and skin: {@code assets_source/fish_soldier.py}.
 */
public final class FishSoldierRenderer extends MobRenderer<FishSoldierEntity, FishSoldierRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("fish_soldier"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/fish_soldier.png");
    private static final RenderType GLOW = RenderType.eyes(JTTWEngine.id("textures/entity/fish_soldier_glow.png"));
    /** How much larger than its pixels it is drawn (fish_soldier.py prints its reach). */
    public static final float SIZE = 0.9f;

    public FishSoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.5f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected void scale(FishSoldierEntity e, PoseStack pose, float partial) {
        pose.scale(SIZE, SIZE, SIZE);
    }

    @Override
    public ResourceLocation getTextureLocation(FishSoldierEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(FishSoldierEntity e) {
        return super.shouldShowName(e) && SeaCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(41, 0).mirror(false).addBox(-4.2f, -12, -2.8f, 8.4f, 12, 6)
                .texOffs(105, 0).mirror(false).addBox(-4.7f, -12.4f, -3.4f, 9.4f, 7.6f, 7)
                .texOffs(101, 64).mirror(false).addBox(-1.6f, -10.8f, -3.9f, 3.2f, 3.2f, 0.5f)
                .texOffs(79, 42).mirror(false).addBox(-4.9f, -5.2f, -3.6f, 9.8f, 1.8f, 7.4f)
                .texOffs(141, 64).mirror(false).addBox(-1.4f, -5.6f, -4.2f, 2.8f, 2.6f, 0.8f), PartPose.offsetAndRotation(0, 12.2f, 0.4f, 0.08f, 0, 0));
        PartDefinition sash = body.addOrReplaceChild("sash", CubeListBuilder.create()
                .texOffs(155, 0).mirror(false).addBox(-1.1f, -6.8f, -0.3f, 2.2f, 13, 0.6f), PartPose.offsetAndRotation(0, -5.6f, -4, 0, 0, 0.62f));
        PartDefinition apron = body.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(116, 42).mirror(false).addBox(-1.5f, 0, -0.2f, 3, 8.4f, 0.4f), PartPose.offsetAndRotation(0, -3.4f, -3.7f, -0.06f, 0, 0));
        PartDefinition rightTasset = body.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(130, 54).mirror(false).addBox(-2.4f, 0, -0.4f, 4.6f, 7, 0.8f), PartPose.offsetAndRotation(-2.6f, -3.5f, -3.3f, -0.16f, 0, 0.12f));
        PartDefinition leftTasset = body.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(130, 54).mirror(true).addBox(-2.2f, 0, -0.4f, 4.6f, 7, 0.8f), PartPose.offsetAndRotation(2.6f, -3.5f, -3.3f, -0.16f, 0, -0.12f));
        PartDefinition rightHipTasset = body.addOrReplaceChild("right_hip_tasset", CubeListBuilder.create()
                .texOffs(164, 0).mirror(false).addBox(-0.8f, 0, -2.6f, 0.8f, 6.4f, 5.2f), PartPose.offsetAndRotation(-4.6f, -3.5f, 0.2f, 0, 0, 0.18f));
        PartDefinition leftHipTasset = body.addOrReplaceChild("left_hip_tasset", CubeListBuilder.create()
                .texOffs(164, 0).mirror(true).addBox(0, 0, -2.6f, 0.8f, 6.4f, 5.2f), PartPose.offsetAndRotation(4.6f, -3.5f, 0.2f, 0, 0, -0.18f));
        PartDefinition rearTasset = body.addOrReplaceChild("rear_tasset", CubeListBuilder.create()
                .texOffs(143, 54).mirror(false).addBox(-3.6f, 0, -0.4f, 7.2f, 6.2f, 0.8f), PartPose.offsetAndRotation(0, -3.5f, 3.4f, 0.14f, 0, 0));
        PartDefinition dorsal = body.addOrReplaceChild("dorsal", CubeListBuilder.create()
                .texOffs(26, 0).mirror(false).addBox(-0.25f, 0, 0, 0.5f, 15, 5.4f), PartPose.offsetAndRotation(0, -16.2f, 2.6f, 0.16f, 0, 0));
        PartDefinition rightPelvic = body.addOrReplaceChild("right_pelvic", CubeListBuilder.create()
                .texOffs(229, 42).mirror(false).addBox(-0.2f, 0, 0, 0.4f, 4.2f, 3.2f), PartPose.offsetAndRotation(-4, -0.4f, 1.6f, 0.7f, 0, 0.35f));
        PartDefinition leftPelvic = body.addOrReplaceChild("left_pelvic", CubeListBuilder.create()
                .texOffs(229, 42).mirror(true).addBox(-0.2f, 0, 0, 0.4f, 4.2f, 3.2f), PartPose.offsetAndRotation(4, -0.4f, 1.6f, 0.7f, 0, -0.35f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(72, 0).mirror(false).addBox(-4, -8.6f, -5, 8, 9, 8)
                .texOffs(179, 0).mirror(false).addBox(-3.6f, -7.2f, -8, 7.2f, 7.6f, 3.2f)
                .texOffs(0, 54).mirror(false).addBox(-2.9f, -5.8f, -10, 5.8f, 5.4f, 2.2f)
                .texOffs(0, 42).mirror(false).addBox(-3.4f, -0.8f, -7.6f, 6.8f, 1.4f, 8.4f), PartPose.offsetAndRotation(0, -12.2f, -0.2f, 0, 0, 0));
        PartDefinition mouth = head.addOrReplaceChild("mouth", CubeListBuilder.create()
                .texOffs(112, 64).mirror(false).addBox(-2.4f, -2.4f, -2, 4.8f, 1.2f, 2.2f)
                .texOffs(150, 64).mirror(false).addBox(-1.4f, -1.3f, -0.5f, 2.8f, 2.6f, 0.6f), PartPose.offsetAndRotation(0, -3, -9.8f, 0.1f, 0, 0));
        PartDefinition rightLip = mouth.addOrReplaceChild("right_lip", CubeListBuilder.create()
                .texOffs(129, 64).mirror(false).addBox(-1, 0, -1.8f, 1, 2.6f, 2), PartPose.offsetAndRotation(-1.4f, -1.3f, -0.2f, 0, 0.15f, 0));
        PartDefinition leftLip = mouth.addOrReplaceChild("left_lip", CubeListBuilder.create()
                .texOffs(129, 64).mirror(true).addBox(0, 0, -1.8f, 1, 2.6f, 2), PartPose.offsetAndRotation(1.4f, -1.3f, -0.2f, 0, -0.15f, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(58, 64).mirror(false).addBox(-2.6f, 0, -2.4f, 5.2f, 1.3f, 3.4f)
                .texOffs(159, 64).mirror(false).addBox(-2.3f, -0.9f, -3.6f, 4.6f, 1.3f, 1.6f), PartPose.offsetAndRotation(0, -1.2f, -7.8f, 0.36f, 0, 0));
        PartDefinition rightBarbel = mouth.addOrReplaceChild("right_barbel", CubeListBuilder.create()
                .texOffs(136, 64).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.8f, 0.6f), PartPose.offsetAndRotation(-2.3f, -0.3f, -1.6f, 0.3f, 0.2f, 0.45f));
        PartDefinition leftBarbel = mouth.addOrReplaceChild("left_barbel", CubeListBuilder.create()
                .texOffs(136, 64).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 3.8f, 0.6f), PartPose.offsetAndRotation(2.3f, -0.3f, -1.6f, 0.3f, -0.2f, -0.45f));
        PartDefinition rightBarbel2 = rightBarbel.addOrReplaceChild("right_barbel_2", CubeListBuilder.create()
                .texOffs(136, 64).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 3.6f, 0.5f), PartPose.offsetAndRotation(0, 3.7f, 0, -0.5f, 0, -0.35f));
        PartDefinition leftBarbel2 = leftBarbel.addOrReplaceChild("left_barbel_2", CubeListBuilder.create()
                .texOffs(136, 64).mirror(true).addBox(-0.25f, 0, -0.25f, 0.5f, 3.6f, 0.5f), PartPose.offsetAndRotation(0, 3.7f, 0, -0.5f, 0, 0.35f));
        PartDefinition rightBarbel3 = rightBarbel2.addOrReplaceChild("right_barbel_3", CubeListBuilder.create()
                .texOffs(174, 64).mirror(false).addBox(-0.2f, 0, -0.2f, 0.4f, 3, 0.4f), PartPose.offsetAndRotation(0, 3.5f, 0, -0.5f, 0, 0.4f));
        PartDefinition leftBarbel3 = leftBarbel2.addOrReplaceChild("left_barbel_3", CubeListBuilder.create()
                .texOffs(174, 64).mirror(true).addBox(-0.2f, 0, -0.2f, 0.4f, 3, 0.4f), PartPose.offsetAndRotation(0, 3.5f, 0, -0.5f, 0, -0.4f));
        PartDefinition rightWhisker = mouth.addOrReplaceChild("right_whisker", CubeListBuilder.create()
                .texOffs(174, 64).mirror(false).addBox(-0.2f, 0, -0.2f, 0.4f, 2.8f, 0.4f), PartPose.offsetAndRotation(-1.3f, -2.3f, -1.8f, 0.9f, 0.2f, 0.35f));
        PartDefinition leftWhisker = mouth.addOrReplaceChild("left_whisker", CubeListBuilder.create()
                .texOffs(174, 64).mirror(true).addBox(-0.2f, 0, -0.2f, 0.4f, 2.8f, 0.4f), PartPose.offsetAndRotation(1.3f, -2.3f, -1.8f, 0.9f, -0.2f, -0.35f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(125, 42).mirror(false).addBox(-0.6f, -2.5f, -2.5f, 0.8f, 5, 5)
                .texOffs(162, 54).mirror(false).addBox(-1.2f, -2, -2, 0.7f, 4, 4)
                .texOffs(79, 64).mirror(false).addBox(-1.7f, -1.2f, -1.2f, 0.6f, 2.4f, 2.4f), PartPose.offsetAndRotation(-3.6f, -4.6f, -6.2f, 0, 0.2f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(125, 42).mirror(true).addBox(-0.2f, -2.5f, -2.5f, 0.8f, 5, 5)
                .texOffs(162, 54).mirror(true).addBox(0.5f, -2, -2, 0.7f, 4, 4)
                .texOffs(79, 64).mirror(true).addBox(1.1f, -1.2f, -1.2f, 0.6f, 2.4f, 2.4f), PartPose.offsetAndRotation(3.6f, -4.6f, -6.2f, 0, -0.2f, 0));
        PartDefinition rightGills = head.addOrReplaceChild("right_gills", CubeListBuilder.create()
                .texOffs(204, 0).mirror(false).addBox(-0.4f, 0, 0, 0.5f, 7, 4.2f), PartPose.offsetAndRotation(-3.8f, -6.6f, -3.6f, 0, 0, 0));
        PartDefinition leftGills = head.addOrReplaceChild("left_gills", CubeListBuilder.create()
                .texOffs(204, 0).mirror(true).addBox(-0.1f, 0, 0, 0.5f, 7, 4.2f), PartPose.offsetAndRotation(3.8f, -6.6f, -3.6f, 0, 0, 0));
        PartDefinition rightPreop = head.addOrReplaceChild("right_preop", CubeListBuilder.create()
                .texOffs(138, 42).mirror(false).addBox(-0.4f, 0, 0, 0.4f, 7.4f, 1.6f), PartPose.offsetAndRotation(-4, -7, -4.8f, 0, -0.08f, 0));
        PartDefinition leftPreop = head.addOrReplaceChild("left_preop", CubeListBuilder.create()
                .texOffs(138, 42).mirror(true).addBox(0, 0, 0, 0.4f, 7.4f, 1.6f), PartPose.offsetAndRotation(4, -7, -4.8f, 0, 0.08f, 0));
        PartDefinition rightGill = head.addOrReplaceChild("right_gill", CubeListBuilder.create()
                .texOffs(140, 0).mirror(false).addBox(-0.5f, 0, 0, 0.5f, 8.2f, 5.6f), PartPose.offsetAndRotation(-4, -7.8f, -3.6f, 0, -0.36f, 0.03f));
        PartDefinition leftGill = head.addOrReplaceChild("left_gill", CubeListBuilder.create()
                .texOffs(140, 0).mirror(true).addBox(0, 0, 0, 0.5f, 8.2f, 5.6f), PartPose.offsetAndRotation(4, -7.8f, -3.6f, 0, 0.36f, -0.03f));
        PartDefinition helmet = head.addOrReplaceChild("helmet", CubeListBuilder.create()
                .texOffs(145, 42).mirror(false).addBox(-4.3f, -0.8f, -4.3f, 8.6f, 1, 8.6f)
                .texOffs(33, 42).mirror(false).addBox(-3.4f, -3.8f, -3.4f, 6.8f, 3.2f, 6.8f)
                .texOffs(179, 64).mirror(false).addBox(-0.7f, -5, -0.7f, 1.4f, 1.3f, 1.4f)
                .texOffs(173, 54).mirror(false).addBox(-1.7f, -8.2f, -1.7f, 3.4f, 3.4f, 3.4f)
                .texOffs(188, 64).mirror(false).addBox(-3.8f, 0, 3.4f, 7.6f, 2.4f, 1), PartPose.offsetAndRotation(0, -8.5f, -0.8f, 0.12f, 0, 0));
        PartDefinition plumeTail = helmet.addOrReplaceChild("plume_tail", CubeListBuilder.create()
                .texOffs(190, 54).mirror(false).addBox(-1.4f, 0, -0.8f, 2.8f, 6, 1.6f), PartPose.offsetAndRotation(0, -7, 1.2f, 1, 0, 0));
        PartDefinition rightHelmwing = helmet.addOrReplaceChild("right_helmwing", CubeListBuilder.create()
                .texOffs(19, 54).mirror(false).addBox(-0.2f, -4.6f, 0, 0.4f, 4.6f, 3.2f), PartPose.offsetAndRotation(-3.3f, -2, 0.6f, 0, 0.35f, 0.55f));
        PartDefinition leftHelmwing = helmet.addOrReplaceChild("left_helmwing", CubeListBuilder.create()
                .texOffs(19, 54).mirror(true).addBox(-0.2f, -4.6f, 0, 0.4f, 4.6f, 3.2f), PartPose.offsetAndRotation(3.3f, -2, 0.6f, 0, -0.35f, -0.55f));
        PartDefinition tail1 = body.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(182, 42).mirror(false).addBox(-2.5f, -2.4f, 0, 5, 4.8f, 5), PartPose.offsetAndRotation(0, -2.4f, 2.4f, -0.9f, 0, 0));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(30, 54).mirror(false).addBox(-1.8f, -1.8f, 0, 3.6f, 3.6f, 4.4f), PartPose.offsetAndRotation(0, 0, 4.8f, -0.3f, 0, 0));
        PartDefinition caudal = tail2.addOrReplaceChild("caudal", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-0.25f, -6.4f, 0, 0.5f, 12.8f, 8.4f), PartPose.offsetAndRotation(0, 0, 4, 0.3f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(62, 42).mirror(false).addBox(-1.7f, -0.6f, -1.7f, 3.4f, 6.6f, 3.4f), PartPose.offsetAndRotation(-2.3f, 12.4f, 0.6f, -0.18f, 0, 0.05f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(62, 42).mirror(true).addBox(-1.7f, -0.6f, -1.7f, 3.4f, 6.6f, 3.4f), PartPose.offsetAndRotation(2.3f, 12.4f, 0.6f, -0.18f, 0, -0.05f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(49, 54).mirror(false).addBox(-1.35f, 0, -1.35f, 2.7f, 6, 2.7f)
                .texOffs(201, 54).mirror(false).addBox(-1.6f, 0.4f, -1.7f, 3.2f, 4.4f, 2.4f), PartPose.offsetAndRotation(0, 5.8f, 0, 0.3f, 0, -0.04f));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(49, 54).mirror(true).addBox(-1.35f, 0, -1.35f, 2.7f, 6, 2.7f)
                .texOffs(201, 54).mirror(true).addBox(-1.6f, 0.4f, -1.7f, 3.2f, 4.4f, 2.4f), PartPose.offsetAndRotation(0, 5.8f, 0, 0.3f, 0, 0.04f));
        PartDefinition rightCalffin = rightShin.addOrReplaceChild("right_calffin", CubeListBuilder.create()
                .texOffs(216, 54).mirror(false).addBox(-0.2f, 0, 0, 0.4f, 4.4f, 2.4f), PartPose.offsetAndRotation(0, 0.6f, 1.2f, -0.25f, 0, 0));
        PartDefinition leftCalffin = leftShin.addOrReplaceChild("left_calffin", CubeListBuilder.create()
                .texOffs(216, 54).mirror(true).addBox(-0.2f, 0, 0, 0.4f, 4.4f, 2.4f), PartPose.offsetAndRotation(0, 0.6f, 1.2f, -0.25f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(26, 64).mirror(false).addBox(-1.6f, 0, -3.4f, 3.2f, 1.8f, 4.8f)
                .texOffs(207, 64).mirror(false).addBox(-1.7f, 0.6f, -4.8f, 1, 1.2f, 1.6f)
                .texOffs(207, 64).mirror(false).addBox(-0.5f, 0.6f, -5.1f, 1, 1.2f, 1.8f)
                .texOffs(207, 64).mirror(false).addBox(0.7f, 0.6f, -4.8f, 1, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 5.9f, 0, -0.12f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(26, 64).mirror(true).addBox(-1.6f, 0, -3.4f, 3.2f, 1.8f, 4.8f)
                .texOffs(207, 64).mirror(true).addBox(0.7f, 0.6f, -4.8f, 1, 1.2f, 1.6f)
                .texOffs(207, 64).mirror(true).addBox(-0.5f, 0.6f, -5.1f, 1, 1.2f, 1.8f)
                .texOffs(207, 64).mirror(true).addBox(-1.7f, 0.6f, -4.8f, 1, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 5.9f, 0, -0.12f, 0, 0));
        PartDefinition rightShoulder = body.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(225, 54).mirror(false).addBox(-3.6f, -0.8f, -2.9f, 4, 1.4f, 5.8f)
                .texOffs(62, 54).mirror(false).addBox(-4, 0.4f, -3.1f, 4.2f, 1.6f, 6.2f)
                .texOffs(87, 54).mirror(false).addBox(-4.2f, 1.8f, -3.2f, 3.6f, 1.5f, 6.4f), PartPose.offsetAndRotation(-4.6f, -11.6f, 0.2f, 0, 0, 0.4f));
        PartDefinition leftShoulder = body.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(225, 54).mirror(true).addBox(-0.4f, -0.8f, -2.9f, 4, 1.4f, 5.8f)
                .texOffs(62, 54).mirror(true).addBox(-0.2f, 0.4f, -3.1f, 4.2f, 1.6f, 6.2f)
                .texOffs(87, 54).mirror(true).addBox(0.6f, 1.8f, -3.2f, 3.6f, 1.5f, 6.4f), PartPose.offsetAndRotation(4.6f, -11.6f, 0.2f, 0, 0, -0.4f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(203, 42).mirror(false).addBox(-1.4f, -0.4f, -1.4f, 2.8f, 6.2f, 2.8f), PartPose.offsetAndRotation(-4.9f, -10.8f, 0.2f, -0.5f, 0, 0.16f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(110, 54).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 5.6f, 2.4f)
                .texOffs(45, 64).mirror(false).addBox(-1.5f, 1.4f, -1.5f, 3, 3.4f, 3), PartPose.offsetAndRotation(0, 5.6f, 0, -0.85f, 0.25f, 0));
        PartDefinition rightPectoral = rightForearm.addOrReplaceChild("right_pectoral", CubeListBuilder.create()
                .texOffs(217, 0).mirror(false).addBox(-0.2f, 0, 0, 0.4f, 5.4f, 5.4f), PartPose.offsetAndRotation(-0.9f, 0.2f, 0.9f, 0.35f, 0.25f, 0.2f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(88, 64).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 2.2f, 2.6f)
                .texOffs(214, 64).mirror(false).addBox(-1.3f, 2, -1.3f, 0.8f, 2.2f, 1)
                .texOffs(214, 64).mirror(false).addBox(-0.4f, 2, -1.3f, 0.8f, 2.4f, 1)
                .texOffs(214, 64).mirror(false).addBox(0.5f, 2, -1.3f, 0.8f, 2.2f, 1), PartPose.offsetAndRotation(0, 5.5f, 0, 0, 0, 0));
        PartDefinition halberd = rightHand.addOrReplaceChild("halberd", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.45f, -30, -0.45f, 0.9f, 40, 0.9f)
                .texOffs(123, 54).mirror(false).addBox(-0.8f, -37.4f, -0.25f, 1.6f, 7.6f, 0.5f)
                .texOffs(219, 64).mirror(false).addBox(-0.75f, -30.8f, -0.75f, 1.5f, 1.6f, 1.5f)
                .texOffs(219, 64).mirror(false).addBox(-0.9f, -29.4f, -0.9f, 1.8f, 1.4f, 1.8f)
                .texOffs(0, 64).mirror(false).addBox(-1.3f, -28, -1.3f, 2.6f, 4.6f, 2.6f)
                .texOffs(228, 64).mirror(false).addBox(-0.6f, 9.6f, -0.6f, 1.2f, 1, 1.2f)
                .texOffs(237, 64).mirror(false).addBox(-0.35f, 10.4f, -0.35f, 0.7f, 2, 0.7f), PartPose.offsetAndRotation(0, 1.3f, -0.4f, 1.3484f, -0.0073f, -0.222f));
        PartDefinition rightCrescent = halberd.addOrReplaceChild("right_crescent", CubeListBuilder.create()
                .texOffs(216, 42).mirror(false).addBox(-4.4f, -4.2f, -0.2f, 4.4f, 8.4f, 0.4f), PartPose.offsetAndRotation(-0.4f, -30.6f, 0, 0, 0, 0));
        PartDefinition leftCrescent = halberd.addOrReplaceChild("left_crescent", CubeListBuilder.create()
                .texOffs(216, 42).mirror(true).addBox(0, -4.2f, -0.2f, 4.4f, 8.4f, 0.4f), PartPose.offsetAndRotation(0.4f, -30.6f, 0, 0, 0, 0));
        PartDefinition pennant = halberd.addOrReplaceChild("pennant", CubeListBuilder.create()
                .texOffs(13, 64).mirror(false).addBox(0, 0, -0.1f, 5, 6.4f, 0.2f), PartPose.offsetAndRotation(0.45f, -24, 0, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(203, 42).mirror(true).addBox(-1.4f, -0.4f, -1.4f, 2.8f, 6.2f, 2.8f), PartPose.offsetAndRotation(4.9f, -10.8f, 0.2f, -0.2f, 0, -0.22f));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(110, 54).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 5.6f, 2.4f)
                .texOffs(45, 64).mirror(true).addBox(-1.5f, 1.4f, -1.5f, 3, 3.4f, 3), PartPose.offsetAndRotation(0, 5.6f, 0, -0.55f, -0.15f, 0));
        PartDefinition leftPectoral = leftForearm.addOrReplaceChild("left_pectoral", CubeListBuilder.create()
                .texOffs(217, 0).mirror(true).addBox(-0.2f, 0, 0, 0.4f, 5.4f, 5.4f), PartPose.offsetAndRotation(0.9f, 0.2f, 0.9f, 0.35f, -0.25f, -0.2f));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(88, 64).mirror(true).addBox(-1.3f, 0, -1.3f, 2.6f, 2.2f, 2.6f)
                .texOffs(214, 64).mirror(true).addBox(0.3f, 2, -1.3f, 0.8f, 2.2f, 1)
                .texOffs(214, 64).mirror(true).addBox(-0.6f, 2, -1.3f, 0.8f, 2.4f, 1)
                .texOffs(214, 64).mirror(true).addBox(-1.5f, 2, -1.3f, 0.8f, 2.2f, 1), PartPose.offsetAndRotation(0, 5.5f, 0, 0.2f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<FishSoldierEntity> {
        private final ModelPart root, body, head, jaw, rightGill, leftGill, rightBarbel, leftBarbel, rightLeg, leftLeg,
                rightShin, leftShin, rightArm, leftArm, rightForearm, leftForearm, rightPectoral, leftPectoral, tail1, tail2,
                caudal, dorsal, plumeTail, apron, rightTasset, leftTasset;

        public Model(ModelPart root) {
            super(RenderType::entityTranslucent);
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightGill = head.getChild("right_gill");
            leftGill = head.getChild("left_gill");
            rightBarbel = head.getChild("mouth").getChild("right_barbel");
            leftBarbel = head.getChild("mouth").getChild("left_barbel");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightPectoral = rightForearm.getChild("right_pectoral");
            leftPectoral = leftForearm.getChild("left_pectoral");
            tail1 = body.getChild("tail_1");
            tail2 = tail1.getChild("tail_2");
            caudal = tail2.getChild("caudal");
            dorsal = body.getChild("dorsal");
            plumeTail = head.getChild("helmet").getChild("plume_tail");
            apron = body.getChild("apron");
            rightTasset = body.getChild("right_tasset");
            leftTasset = body.getChild("left_tasset");
        }

        private static void turn(ModelPart p, float x, float y, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.yRot = p.getInitialPose().yRot + y;
            p.zRot = p.getInitialPose().zRot + z;
        }

        @Override
        public void setupAnim(FishSoldierEntity e, float swing, float amount, float age, float yaw, float pitch) {
            boolean swimming = e.isInWater();
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            turn(head, pitch * Mth.DEG_TO_RAD * 0.6f, yaw * Mth.DEG_TO_RAD, 0);
            // it breathes: the gill plates open and close, the mouth gulps
            float breath = Mth.sin(age * (swimming ? 0.22f : 0.14f));
            turn(rightGill, 0, -0.12f * (breath + 1), 0);
            turn(leftGill, 0, 0.12f * (breath + 1), 0);
            turn(jaw, 0.12f * (breath + 1) + strike * 0.25f, 0, 0);
            turn(rightBarbel, Mth.sin(age * 0.09f) * 0.12f + amount * 0.3f, 0, Mth.sin(age * 0.07f) * 0.06f);
            turn(leftBarbel, Mth.sin(age * 0.09f + 0.7f) * 0.12f + amount * 0.3f, 0, -Mth.sin(age * 0.07f + 0.5f) * 0.06f);
            // legs: stepping on land, a frog's kick in the water
            float w = swing * 0.66f;
            float kick = swimming ? Mth.sin(age * 0.4f) * (0.3f + amount * 0.6f) : Mth.cos(w) * 0.9f * amount;
            turn(rightLeg, kick, 0, 0);
            turn(leftLeg, swimming ? kick : -kick, 0, 0);
            turn(rightShin, swimming ? Math.max(0, -kick) * 0.9f : Math.max(0, Mth.sin(w)) * 0.7f * amount, 0, 0);
            turn(leftShin, swimming ? Math.max(0, -kick) * 0.9f : Math.max(0, -Mth.sin(w)) * 0.7f * amount, 0, 0);
            // the tail swings as it walks; swimming, it beats side to side, the fin last
            float beat = swimming ? Mth.sin(age * 0.45f) * (0.35f + amount * 0.3f) : Mth.sin(w * 0.5f + age * 0.06f) * (0.08f + amount * 0.15f);
            turn(tail1, 0, beat * 0.5f, 0);
            turn(tail2, 0, beat * 0.6f, 0);
            turn(caudal, 0, beat * 0.8f, 0);
            turn(dorsal, Mth.sin(age * 0.1f) * 0.04f + (swimming ? 0.08f : 0), 0, 0);
            // the pectoral fins row
            float row = Mth.sin(age * (swimming ? 0.35f : 0.12f)) * (swimming ? 0.35f : 0.1f);
            turn(rightPectoral, 0, row, 0);
            turn(leftPectoral, 0, -row, 0);
            // the body leans into a swim; the halberd thrusts on a blow; the free arm swings
            turn(body, (swimming ? 0.25f * amount : 0) + Mth.sin(age * 0.08f) * 0.02f + strike * 0.15f, 0, 0);
            turn(rightArm, -strike * 1.1f, 0, 0);
            turn(rightForearm, strike * 0.9f, 0, 0);
            turn(leftArm, -kick * 0.4f + (swimming ? -0.3f : 0), 0, swimming ? -0.3f : 0);
            turn(leftForearm, 0, 0, 0);
            // the plume, the apron and the tassets trail
            turn(plumeTail, amount * 0.3f + Mth.sin(age * 0.1f) * 0.06f, 0, 0);
            turn(apron, -Math.abs(kick) * 0.3f, 0, 0);
            turn(rightTasset, Math.min(0, -kick) * 0.4f, 0, 0);
            turn(leftTasset, Math.min(0, kick) * 0.4f, 0, 0);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
