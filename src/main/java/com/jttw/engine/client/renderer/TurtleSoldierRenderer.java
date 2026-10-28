package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.waters.TurtleSoldierEntity;
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
 * The Turtle Soldier (龟兵), the heavy infantry of the Dragon King's host: a sea turtle stood up
 * under a great domed shell of amber tortoiseshell, every scute drawn (vertebral, costal, the
 * serrated ring of marginals) with its growth lines; a cream plastron in its paired scutes over its
 * front; a wrinkled, scaled neck thrust forward, a plated head with a hooked beak; clawed flippers,
 * stumpy legs; the army's teal and gold: helmet and red plume, spaulders, sash, belt, tassets,
 * greaves; a great round shield with a gold tortoise in a ring of waves, and a gilt melon-headed
 * mace. It plods, swings its mace overhead to strike, raises its shield when it takes a blow on it,
 * and draws its head into its shell and crouches behind its shield when it withdraws. The glint of
 * its eyes full-bright. Model and skin: {@code assets_source/turtle_soldier.py}.
 */
public final class TurtleSoldierRenderer extends MobRenderer<TurtleSoldierEntity, TurtleSoldierRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("turtle_soldier"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/turtle_soldier.png");
    private static final RenderType GLOW = RenderType.eyes(JTTWEngine.id("textures/entity/turtle_soldier_glow.png"));
    /** How much larger than its pixels it is drawn (turtle_soldier.py prints its reach). */
    public static final float SIZE = 0.95f;

    public TurtleSoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.7f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected void scale(TurtleSoldierEntity e, PoseStack pose, float partial) {
        pose.scale(SIZE, SIZE, SIZE);
    }

    @Override
    public ResourceLocation getTextureLocation(TurtleSoldierEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(TurtleSoldierEntity e) {
        return super.shouldShowName(e) && SeaCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-5, -14, -3.4f, 10, 14, 7)
                .texOffs(210, 0).mirror(false).addBox(-4.6f, -13.6f, -4.4f, 9.2f, 12.8f, 1.2f)
                .texOffs(214, 22).mirror(false).addBox(-5.4f, -2.4f, -4.9f, 10.8f, 1.8f, 8.6f)
                .texOffs(24, 69).mirror(false).addBox(-1.4f, -2.8f, -5.5f, 2.8f, 2.6f, 0.8f), PartPose.offsetAndRotation(0, 14, 0.6f, 0.05f, 0, 0));
        PartDefinition rightBridge = body.addOrReplaceChild("right_bridge", CubeListBuilder.create()
                .texOffs(82, 0).mirror(false).addBox(-1.2f, 0, 0, 1.4f, 10.4f, 6.4f), PartPose.offsetAndRotation(-5, -12.6f, -2.8f, 0, 0, 0));
        PartDefinition leftBridge = body.addOrReplaceChild("left_bridge", CubeListBuilder.create()
                .texOffs(82, 0).mirror(true).addBox(-0.2f, 0, 0, 1.4f, 10.4f, 6.4f), PartPose.offsetAndRotation(5, -12.6f, -2.8f, 0, 0, 0));
        PartDefinition sash = body.addOrReplaceChild("sash", CubeListBuilder.create()
                .texOffs(176, 0).mirror(false).addBox(-1.2f, -7.6f, -0.3f, 2.4f, 14.6f, 0.6f), PartPose.offsetAndRotation(0, -7, -5, 0, 0, 0.62f));
        PartDefinition apron = body.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(88, 50).mirror(false).addBox(-1.6f, 0, -0.2f, 3.2f, 6.6f, 0.4f), PartPose.offsetAndRotation(0, -0.8f, -4.9f, -0.06f, 0, 0));
        PartDefinition rightTasset = body.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(65, 60).mirror(false).addBox(-2.6f, 0, -0.4f, 5, 5.6f, 0.8f), PartPose.offsetAndRotation(-2.8f, -0.9f, -4.5f, -0.18f, 0, 0.14f));
        PartDefinition leftTasset = body.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(65, 60).mirror(true).addBox(-2.4f, 0, -0.4f, 5, 5.6f, 0.8f), PartPose.offsetAndRotation(2.8f, -0.9f, -4.5f, -0.18f, 0, -0.14f));
        PartDefinition shell = body.addOrReplaceChild("shell", CubeListBuilder.create()
                .texOffs(132, 38).mirror(false).addBox(-7.25f, -3.95f, -0.8f, 14.5f, 7.5f, 1.2f)
                .texOffs(79, 22).mirror(false).addBox(-6.65f, -6.05f, -0.8f, 13.3f, 11.7f, 1.2f)
                .texOffs(149, 0).mirror(false).addBox(-5.5f, -7.7f, -0.8f, 11, 15, 1.2f)
                .texOffs(35, 0).mirror(false).addBox(-3.55f, -8.9f, -0.8f, 7.1f, 17.4f, 1.2f)
                .texOffs(0, 38).mirror(false).addBox(-6.85f, -3.75f, 0.4f, 13.7f, 7.1f, 3)
                .texOffs(0, 22).mirror(false).addBox(-6.3f, -5.8f, 0.4f, 12.6f, 11.2f, 3)
                .texOffs(101, 0).mirror(false).addBox(-5.2f, -7.35f, 0.4f, 10.4f, 14.3f, 3)
                .texOffs(56, 0).mirror(false).addBox(-3.35f, -8.55f, 0.4f, 6.7f, 16.7f, 3)
                .texOffs(167, 38).mirror(false).addBox(-6, -3.4f, 3.4f, 12, 6.4f, 2.2f)
                .texOffs(112, 22).mirror(false).addBox(-5.5f, -5.2f, 3.4f, 11, 10, 2.2f)
                .texOffs(185, 0).mirror(false).addBox(-4.5f, -6.6f, 3.4f, 9, 12.8f, 2.2f)
                .texOffs(130, 0).mirror(false).addBox(-2.95f, -7.65f, 3.4f, 5.9f, 14.9f, 2.2f)
                .texOffs(99, 50).mirror(false).addBox(-4.8f, -2.85f, 5.6f, 9.6f, 5.3f, 1.6f)
                .texOffs(35, 38).mirror(false).addBox(-4.4f, -4.35f, 5.6f, 8.8f, 8.3f, 1.6f)
                .texOffs(141, 22).mirror(false).addBox(-3.65f, -5.5f, 5.6f, 7.3f, 10.6f, 1.6f)
                .texOffs(33, 22).mirror(false).addBox(-2.35f, -6.35f, 5.6f, 4.7f, 12.3f, 1.6f)
                .texOffs(208, 60).mirror(false).addBox(-3.35f, -2.2f, 7.2f, 6.7f, 4, 1)
                .texOffs(124, 50).mirror(false).addBox(-3.05f, -3.35f, 7.2f, 6.1f, 6.3f, 1)
                .texOffs(198, 38).mirror(false).addBox(-2.5f, -4.25f, 7.2f, 5, 8.1f, 1)
                .texOffs(58, 38).mirror(false).addBox(-1.65f, -4.9f, 7.2f, 3.3f, 9.4f, 1)
                .texOffs(33, 69).mirror(false).addBox(-1.75f, -1.45f, 8.2f, 3.5f, 2.5f, 0.6f)
                .texOffs(225, 60).mirror(false).addBox(-1.6f, -2.2f, 8.2f, 3.2f, 4, 0.6f)
                .texOffs(139, 60).mirror(false).addBox(-1.35f, -2.7f, 8.2f, 2.7f, 5, 0.6f)
                .texOffs(78, 60).mirror(false).addBox(-0.85f, -3.15f, 8.2f, 1.7f, 5.9f, 0.6f), PartPose.offsetAndRotation(0, -6.6f, 3.2f, -0.04f, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(85, 60).mirror(false).addBox(-1.1f, -1.1f, 0, 2.2f, 2.2f, 3.2f), PartPose.offsetAndRotation(0, 0.2f, 3, -0.9f, 0, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(69, 38).mirror(false).addBox(-2.4f, -5.2f, -2.4f, 4.8f, 5.6f, 4.8f)
                .texOffs(141, 50).mirror(false).addBox(-3, -1.2f, -3, 6, 1.8f, 6), PartPose.offsetAndRotation(0, -13.4f, -0.8f, 0.42f, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(162, 22).mirror(false).addBox(-2.9f, -4.8f, -5.6f, 5.8f, 5, 7)
                .texOffs(148, 60).mirror(false).addBox(-2.1f, -3.6f, -7.6f, 4.2f, 2.8f, 2.2f)
                .texOffs(44, 69).mirror(false).addBox(-1, -1.6f, -8.2f, 2, 1.6f, 1.2f), PartPose.offsetAndRotation(0, -4.8f, -0.4f, -0.42f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(166, 50).mirror(false).addBox(-2.5f, 0, -6, 5, 1.4f, 6)
                .texOffs(53, 69).mirror(false).addBox(-1.8f, -0.5f, -7.2f, 3.6f, 1.3f, 1.4f), PartPose.offsetAndRotation(0, 0, -0.6f, 0.08f, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(236, 60).mirror(false).addBox(-0.5f, -1, -1.3f, 0.6f, 2, 2.6f)
                .texOffs(245, 60).mirror(false).addBox(-0.8f, -2, -1.8f, 1, 1, 3.4f), PartPose.offsetAndRotation(-2.9f, -2.8f, -3.8f, 0, 0.15f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(236, 60).mirror(true).addBox(-0.1f, -1, -1.3f, 0.6f, 2, 2.6f)
                .texOffs(245, 60).mirror(true).addBox(-0.2f, -2, -1.8f, 1, 1, 3.4f), PartPose.offsetAndRotation(2.9f, -2.8f, -3.8f, 0, -0.15f, 0));
        PartDefinition helmet = head.addOrReplaceChild("helmet", CubeListBuilder.create()
                .texOffs(189, 50).mirror(false).addBox(-3.5f, -0.8f, -3.5f, 7, 1, 7)
                .texOffs(0, 50).mirror(false).addBox(-2.8f, -3.6f, -2.8f, 5.6f, 3, 5.6f)
                .texOffs(66, 69).mirror(false).addBox(-0.7f, -4.8f, -0.7f, 1.4f, 1.3f, 1.4f)
                .texOffs(218, 50).mirror(false).addBox(-1.7f, -8, -1.7f, 3.4f, 3.4f, 3.4f)
                .texOffs(75, 69).mirror(false).addBox(-3.2f, 0, 2.6f, 6.4f, 2.2f, 1), PartPose.offsetAndRotation(0, -4.8f, -1.4f, 0.1f, 0, 0));
        PartDefinition plumeTail = helmet.addOrReplaceChild("plume_tail", CubeListBuilder.create()
                .texOffs(235, 50).mirror(false).addBox(-1.4f, 0, -0.8f, 2.8f, 6, 1.6f), PartPose.offsetAndRotation(0, -6.8f, 1.2f, 1, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(90, 38).mirror(false).addBox(-2.4f, -0.4f, -2.4f, 4.8f, 5.4f, 4.8f), PartPose.offsetAndRotation(-3, 13.6f, 0.6f, -0.06f, 0, 0.08f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(90, 38).mirror(true).addBox(-2.4f, -0.4f, -2.4f, 4.8f, 5.4f, 4.8f), PartPose.offsetAndRotation(3, 13.6f, 0.6f, -0.06f, 0, -0.08f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(211, 38).mirror(false).addBox(-2.2f, 0, -2.2f, 4.4f, 4.2f, 4.4f)
                .texOffs(0, 69).mirror(false).addBox(-2.5f, 0.2f, -2.7f, 5, 3.2f, 1), PartPose.offsetAndRotation(0, 4.8f, 0, 0.08f, 0, -0.08f));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(211, 38).mirror(true).addBox(-2.2f, 0, -2.2f, 4.4f, 4.2f, 4.4f)
                .texOffs(0, 69).mirror(true).addBox(-2.5f, 0.2f, -2.7f, 5, 3.2f, 1), PartPose.offsetAndRotation(0, 4.8f, 0, 0.08f, 0, 0.08f));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(0, 60).mirror(false).addBox(-2.6f, 0, -3.2f, 5.2f, 1.8f, 5.4f)
                .texOffs(92, 69).mirror(false).addBox(-2.5f, 0.6f, -4.2f, 1.2f, 1.2f, 1.2f)
                .texOffs(92, 69).mirror(false).addBox(-0.6f, 0.6f, -4.4f, 1.2f, 1.2f, 1.4f)
                .texOffs(92, 69).mirror(false).addBox(1.3f, 0.6f, -4.2f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 4, 0, -0.02f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(0, 60).mirror(true).addBox(-2.6f, 0, -3.2f, 5.2f, 1.8f, 5.4f)
                .texOffs(92, 69).mirror(true).addBox(1.3f, 0.6f, -4.2f, 1.2f, 1.2f, 1.2f)
                .texOffs(92, 69).mirror(true).addBox(-0.6f, 0.6f, -4.4f, 1.2f, 1.2f, 1.4f)
                .texOffs(92, 69).mirror(true).addBox(-2.5f, 0.6f, -4.2f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 4, 0, -0.02f, 0, 0));
        PartDefinition rightShoulder = body.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(25, 60).mirror(false).addBox(-3.8f, -0.8f, -3, 4.4f, 1.4f, 6)
                .texOffs(25, 50).mirror(false).addBox(-4.3f, 0.4f, -3.2f, 4.6f, 1.6f, 6.4f)
                .texOffs(50, 50).mirror(false).addBox(-4.5f, 1.8f, -3.3f, 4, 1.5f, 6.6f), PartPose.offsetAndRotation(-5.4f, -13, 0.2f, 0, 0, 0.42f));
        PartDefinition leftShoulder = body.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(25, 60).mirror(true).addBox(-0.6f, -0.8f, -3, 4.4f, 1.4f, 6)
                .texOffs(25, 50).mirror(true).addBox(-0.3f, 0.4f, -3.2f, 4.6f, 1.6f, 6.4f)
                .texOffs(50, 50).mirror(true).addBox(0.5f, 1.8f, -3.3f, 4, 1.5f, 6.6f), PartPose.offsetAndRotation(5.4f, -13, 0.2f, 0, 0, -0.42f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(232, 38).mirror(false).addBox(-1.9f, -0.4f, -1.9f, 3.8f, 5.6f, 3.8f), PartPose.offsetAndRotation(-5.8f, -12.2f, 0.4f, -0.3f, 0, 0.42f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(48, 60).mirror(false).addBox(-1.7f, 0, -1.7f, 3.4f, 3.8f, 3.4f)
                .texOffs(100, 60).mirror(false).addBox(-2, 0.6f, -2, 4, 2.8f, 4), PartPose.offsetAndRotation(0, 5, 0, -0.7f, 0, -0.15f));
        PartDefinition rightFlipper = rightForearm.addOrReplaceChild("right_flipper", CubeListBuilder.create()
                .texOffs(165, 60).mirror(false).addBox(-2.2f, 0, -0.8f, 4.4f, 3.6f, 1.6f)
                .texOffs(110, 69).mirror(false).addBox(-2.6f, 0.6f, -1, 0.9f, 1.4f, 1)
                .texOffs(110, 69).mirror(false).addBox(-2.6f, 2.2f, -1, 0.9f, 1.4f, 1), PartPose.offsetAndRotation(0, 3.6f, 0, -0.2f, 0, 0));
        PartDefinition rightFlipperTip = rightFlipper.addOrReplaceChild("right_flipper_tip", CubeListBuilder.create()
                .texOffs(180, 60).mirror(false).addBox(-2, 0, -0.7f, 4, 3.2f, 1.3f), PartPose.offsetAndRotation(0, 3.4f, 0.2f, 0.9f, 0, 0));
        PartDefinition mace = rightFlipper.addOrReplaceChild("mace", CubeListBuilder.create()
                .texOffs(77, 0).mirror(false).addBox(-0.5f, -13, -0.5f, 1, 19, 1)
                .texOffs(101, 69).mirror(false).addBox(-0.8f, -13.8f, -0.8f, 1.6f, 1.4f, 1.6f)
                .texOffs(117, 60).mirror(false).addBox(-1.2f, -12.6f, -1.2f, 2.4f, 3.6f, 2.4f)
                .texOffs(111, 38).mirror(false).addBox(-2.1f, -19, -2.1f, 4.2f, 5.2f, 4.2f)
                .texOffs(193, 60).mirror(false).addBox(-2.5f, -18.4f, -1, 5, 4, 2)
                .texOffs(73, 50).mirror(false).addBox(-1, -18.4f, -2.5f, 2, 4, 5)
                .texOffs(101, 69).mirror(false).addBox(-0.6f, -20.4f, -0.6f, 1.2f, 1.6f, 1.2f)
                .texOffs(101, 69).mirror(false).addBox(-0.8f, 6, -0.8f, 1.6f, 1.4f, 1.6f), PartPose.offsetAndRotation(0, 2.6f, -1.4f, 0.9278f, 0.6112f, -0.2562f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(232, 38).mirror(true).addBox(-1.9f, -0.4f, -1.9f, 3.8f, 5.6f, 3.8f), PartPose.offsetAndRotation(5.8f, -12.2f, 0.4f, -0.85f, 0.2f, -0.28f));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(48, 60).mirror(true).addBox(-1.7f, 0, -1.7f, 3.4f, 3.8f, 3.4f)
                .texOffs(100, 60).mirror(true).addBox(-2, 0.6f, -2, 4, 2.8f, 4), PartPose.offsetAndRotation(0, 5, 0, -0.85f, 0, 0));
        PartDefinition leftFlipper = leftForearm.addOrReplaceChild("left_flipper", CubeListBuilder.create()
                .texOffs(165, 60).mirror(true).addBox(-2.2f, 0, -0.8f, 4.4f, 3.6f, 1.6f)
                .texOffs(110, 69).mirror(true).addBox(1.7f, 0.6f, -1, 0.9f, 1.4f, 1)
                .texOffs(110, 69).mirror(true).addBox(1.7f, 2.2f, -1, 0.9f, 1.4f, 1), PartPose.offsetAndRotation(0, 3.6f, 0, 0.3f, 0, 0));
        PartDefinition shield = leftForearm.addOrReplaceChild("shield", CubeListBuilder.create()
                .texOffs(48, 22).mirror(false).addBox(-7, -7, -0.8f, 14, 14, 0.8f)
                .texOffs(189, 22).mirror(false).addBox(-5.2f, -5.2f, -1.4f, 10.4f, 10.4f, 0.6f)
                .texOffs(13, 69).mirror(false).addBox(-1.8f, -1.8f, -2.4f, 3.6f, 3.6f, 1), PartPose.offsetAndRotation(0.6f, 2, -2.4f, 1.7633f, -0.2169f, -0.5408f));
        PartDefinition shieldTasselL = shield.addOrReplaceChild("shield_tassel_l", CubeListBuilder.create()
                .texOffs(130, 60).mirror(false).addBox(-0.8f, 0, -0.6f, 1.6f, 4.4f, 1.2f), PartPose.offsetAndRotation(-4.2f, 5.2f, -0.4f, 0, 0, 0));
        PartDefinition shieldTasselR = shield.addOrReplaceChild("shield_tassel_r", CubeListBuilder.create()
                .texOffs(130, 60).mirror(false).addBox(-0.8f, 0, -0.6f, 1.6f, 4.4f, 1.2f), PartPose.offsetAndRotation(4.2f, 5.2f, -0.4f, 0, 0, 0));
        PartDefinition leftFlipperTip = leftFlipper.addOrReplaceChild("left_flipper_tip", CubeListBuilder.create()
                .texOffs(180, 60).mirror(true).addBox(-2, 0, -0.7f, 4, 3.2f, 1.3f), PartPose.offsetAndRotation(0, 3.4f, 0.2f, 0.9f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<TurtleSoldierEntity> {
        private final ModelPart root, body, neck, head, jaw, rightLeg, leftLeg, rightShin, leftShin, rightArm, rightForearm,
                leftArm, leftForearm, plumeTail, apron, rightTasset, leftTasset, tail;

        public Model(ModelPart root) {
            super(RenderType::entityCutoutNoCull);
            this.root = root;
            body = root.getChild("body");
            neck = body.getChild("neck");
            head = neck.getChild("head");
            jaw = head.getChild("jaw");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            rightArm = body.getChild("right_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftArm = body.getChild("left_arm");
            leftForearm = leftArm.getChild("left_forearm");
            plumeTail = head.getChild("helmet").getChild("plume_tail");
            apron = body.getChild("apron");
            rightTasset = body.getChild("right_tasset");
            leftTasset = body.getChild("left_tasset");
            tail = body.getChild("tail");
        }

        private static void turn(ModelPart p, float x, float y, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.yRot = p.getInitialPose().yRot + y;
            p.zRot = p.getInitialPose().zRot + z;
        }

        private static void shift(ModelPart p, float x, float y, float z) {
            p.x = p.getInitialPose().x + x;
            p.y = p.getInitialPose().y + y;
            p.z = p.getInitialPose().z + z;
        }

        @Override
        public void setupAnim(TurtleSoldierEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float in = e.tucked(age - (int) age), out = 1 - in;
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float raise = Math.max(0, 1 - (e.tickCount + (age - (int) age) - e.lastBlock) / 12f);
            boolean swimming = e.isInWater() && !e.onGround();
            // a heavy, rolling plod; swimming, its legs paddle slowly
            float w = swing * 0.5f;
            float step = swimming ? Mth.sin(age * 0.15f) * 0.35f : Mth.cos(w) * 0.6f * amount;
            turn(rightLeg, step * out - 0.6f * in, 0, 0);
            turn(leftLeg, -step * out - 0.6f * in, 0, 0);
            turn(rightShin, Math.max(0, Mth.sin(w)) * 0.4f * amount * out + 1.0f * in, 0, 0);
            turn(leftShin, Math.max(0, -Mth.sin(w)) * 0.4f * amount * out + 1.0f * in, 0, 0);
            shift(rightLeg, 0, 3f * in, 0);
            shift(leftLeg, 0, 3f * in, 0);
            // the body sways side to side as it walks; withdrawn, it sinks down behind its shield
            turn(body, Mth.sin(age * 0.06f) * 0.015f + strike * 0.12f + 0.15f * in, 0, Mth.sin(w) * 0.06f * amount * out);
            shift(body, 0, 3f * in, 0);
            // the head, on its neck; withdrawn, drawn down and back into the shell
            turn(neck, -0.3f * in, 0, 0);
            shift(neck, 0, 6.5f * in, 2.6f * in);
            turn(head, (pitch * Mth.DEG_TO_RAD * 0.6f) * out + 0.3f * in, yaw * Mth.DEG_TO_RAD * out, 0);
            turn(jaw, Math.max(0, Mth.sin(age * 0.05f)) * 0.12f * out + strike * 0.3f, 0, 0);
            // the mace swings overhead and down on a blow; tucked, the arm draws in
            turn(rightArm, -strike * 1.6f - 0.2f * in, 0, -0.25f * in);
            turn(rightForearm, strike * 0.5f - 0.3f * in, 0, 0);
            // the shield comes up when it takes a blow, and in close before it when it withdraws
            float guard = Math.max(raise, in);
            turn(leftArm, -0.35f * guard - step * 0.2f * out, -0.25f * guard, 0);
            turn(leftForearm, -0.2f * guard, 0, 0);
            // the plume, the apron and the tassets trail; the tail wags
            turn(plumeTail, amount * 0.3f + Mth.sin(age * 0.1f) * 0.05f, 0, 0);
            turn(apron, -Math.abs(step) * 0.25f, 0, 0);
            turn(rightTasset, Math.min(0, -step) * 0.4f, 0, 0);
            turn(leftTasset, Math.min(0, step) * 0.4f, 0, 0);
            turn(tail, 0, Mth.sin(age * 0.12f) * 0.2f * out, 0);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
