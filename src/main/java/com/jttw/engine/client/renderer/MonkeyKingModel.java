package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Sun Wukong as he is drawn from the start, in place of the player's own body (MonkeyKingLayer; his
 * clones too, WuCloneRenderer; and his arms in first person): the Monkey King's sculpted head (the
 * Fighting Buddha's, fighting_buddha_head.py, without the crown) on a lean, muscular monkey's body of
 * golden-brown fur with bare tan chest, belly, hands and feet, long arms, a tail curling up behind him,
 * and the golden fillet on his head. No armour or outfit of its own: real armour is drawn over him as on
 * anyone. Its model and skin come from assets_source/monkey_king.py; each piece hangs from a copy of a
 * humanoid model's head, body, arms or legs, posed as that model is each frame.
 */
public final class MonkeyKingModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("monkey_king"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/monkey_king.png");

    private final ModelPart root, head, body, rightArm, leftArm, rightLeg, leftLeg, tail, tail1, tail2, rightForearm;
    private final ModelPart headRoot, monkeyHead;

    public MonkeyKingModel(EntityModelSet models) {
        root = models.bakeLayer(LAYER);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        tail = body.getChild("tail");
        tail1 = tail.getChild("tail_1");
        tail2 = tail1.getChild("tail_2");
        rightForearm = rightArm.getChild("right_upper_arm").getChild("right_forearm");
        headRoot = models.bakeLayer(FightingBuddhaLayer.HEAD_LAYER);
        monkeyHead = headRoot.getChild("head");
    }

    /** Takes up the pose of {@code m}; the tail swings with his stride and sways as he stands. */
    public void pose(HumanoidModel<?> m, float age, float swingAmount) {
        head.copyFrom(m.head);
        monkeyHead.copyFrom(m.head);
        body.copyFrom(m.body);
        rightArm.copyFrom(m.rightArm);
        leftArm.copyFrom(m.leftArm);
        rightLeg.copyFrom(m.rightLeg);
        leftLeg.copyFrom(m.leftLeg);
        float sway = (float) Math.sin(age * 0.07f);
        tail.zRot = 0.18f * sway;
        tail.yRot = 0.12f * (float) Math.sin(age * 0.05f) + 0.3f * swingAmount * (float) Math.sin(age * 0.6f);
        tail1.zRot = 0.14f * (float) Math.sin(age * 0.07f - 0.8f);
        tail2.zRot = 0.12f * (float) Math.sin(age * 0.07f - 1.6f);
        rightForearm.xRot = 0;
    }

    /** Folds his right forearm at the elbow to grip the staff carried at ease (StaffGrip; the upper arm is the pose's). */
    public void gripStaff() {
        rightForearm.xRot = com.jttw.engine.client.StaffGrip.MONKEY_ELBOW;
    }

    /** Draws him, posed (see {@link #pose}). */
    public void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        headRoot.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(FightingBuddhaLayer.HEAD_TEXTURE)), light, overlay);
        headRoot.render(pose, buffers.getBuffer(RenderType.eyes(FightingBuddhaLayer.HEAD_GLOW)), 0xF000F0, overlay);
        root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
    }

    /** One of his arms alone, as first person shows it: at its rest pose, the pose stack set as for the player's own arm. */
    public void renderArm(PoseStack pose, MultiBufferSource buffers, int light, HumanoidArm side) {
        ModelPart arm = side == HumanoidArm.RIGHT ? rightArm : leftArm;
        arm.resetPose();
        rightForearm.xRot = 0;
        pose.pushPose();
        float s = 0.8f;
        pose.translate(arm.x / 16f * (1 - s), arm.y / 16f * (1 - s), 0);
        pose.scale(s, s, s);
        arm.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(-5, 2, 0, 0, 0, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offsetAndRotation(5, 2, 0, 0, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.9f, 12, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(1.9f, 12, 0, 0, 0, 0));
        PartDefinition fillet = head.addOrReplaceChild("fillet", CubeListBuilder.create()
                .texOffs(50, 0).mirror(false).addBox(-4.45f, -9, -4.15f, 8.9f, 1, 9), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightFilletCurl = head.addOrReplaceChild("right_fillet_curl", CubeListBuilder.create()
                .texOffs(73, 31).mirror(false).addBox(-0.75f, -0.75f, -0.25f, 1.5f, 1.5f, 0.5f), PartPose.offsetAndRotation(-0.9f, -8.45f, -4.35f, 0, 0, 0.785f));
        PartDefinition leftFilletCurl = head.addOrReplaceChild("left_fillet_curl", CubeListBuilder.create()
                .texOffs(73, 31).mirror(true).addBox(-0.75f, -0.75f, -0.25f, 1.5f, 1.5f, 0.5f), PartPose.offsetAndRotation(0.9f, -8.45f, -4.35f, 0, 0, -0.785f));
        PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-4.2f, -0.1f, -2.25f, 8.4f, 5.6f, 4.5f)
                .texOffs(87, 0).mirror(false).addBox(-3.5f, 5.3f, -1.95f, 7, 5, 3.9f)
                .texOffs(55, 12).mirror(false).addBox(-3.8f, 9.9f, -2.05f, 7.6f, 2.2f, 4.1f)
                .texOffs(82, 12).mirror(false).addBox(-2.7f, -1.1f, -2.6f, 5.4f, 1.5f, 5.2f)
                .texOffs(21, 22).mirror(false).addBox(-1.95f, 4.5f, -2.3f, 3.9f, 5.2f, 0.45f)
                .texOffs(32, 22).mirror(false).addBox(-3.7f, 0.4f, 2.05f, 7.4f, 5.2f, 0.5f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightPec = body.addOrReplaceChild("right_pec", CubeListBuilder.create()
                .texOffs(103, 22).mirror(false).addBox(-3.35f, 0.55f, -2.75f, 3.25f, 3.7f, 0.55f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition leftPec = body.addOrReplaceChild("left_pec", CubeListBuilder.create()
                .texOffs(103, 22).mirror(true).addBox(0.1f, 0.55f, -2.75f, 3.25f, 3.7f, 0.55f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightFlank = body.addOrReplaceChild("right_flank", CubeListBuilder.create()
                .texOffs(110, 0).mirror(false).addBox(-4.6f, 0.7f, -1.7f, 0.45f, 4.4f, 3.5f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition leftFlank = body.addOrReplaceChild("left_flank", CubeListBuilder.create()
                .texOffs(110, 0).mirror(true).addBox(4.15f, 0.7f, -1.7f, 0.45f, 4.4f, 3.5f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition upperBack = body.addOrReplaceChild("upper_back", CubeListBuilder.create()
                .texOffs(0, 31).mirror(false).addBox(-3.2f, 0.3f, 2.5f, 6.4f, 4, 0.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightUpperArm = rightArm.addOrReplaceChild("right_upper_arm", CubeListBuilder.create()
                .texOffs(0, 12).mirror(false).addBox(-3.65f, -2.6f, -2.4f, 4.6f, 3.1f, 4.8f)
                .texOffs(21, 12).mirror(false).addBox(-3.05f, -0.2f, -1.95f, 3.8f, 4.6f, 3.9f)
                .texOffs(17, 31).mirror(false).addBox(-2.65f, 0.4f, -2.35f, 2.9f, 3.1f, 0.5f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition leftUpperArm = leftArm.addOrReplaceChild("left_upper_arm", CubeListBuilder.create()
                .texOffs(0, 12).mirror(true).addBox(-0.95f, -2.6f, -2.4f, 4.6f, 3.1f, 4.8f)
                .texOffs(21, 12).mirror(true).addBox(-0.75f, -0.2f, -1.95f, 3.8f, 4.6f, 3.9f)
                .texOffs(17, 31).mirror(true).addBox(-0.25f, 0.4f, -2.35f, 2.9f, 3.1f, 0.5f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightForearm = rightUpperArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(51, 22).mirror(false).addBox(-2, 0, -2, 3.8f, 3, 4)
                .texOffs(107, 12).mirror(false).addBox(-1.8f, 2.8f, -1.8f, 3.4f, 3.4f, 3.6f)
                .texOffs(68, 22).mirror(false).addBox(-1.85f, 6, -1.85f, 3.5f, 2.3f, 3.7f)
                .texOffs(40, 31).mirror(false).addBox(-1.8f, 8.1f, -1.95f, 3.4f, 1, 2.9f)
                .texOffs(80, 31).mirror(false).addBox(0.85f, 6.3f, -2.4f, 0.9f, 1.7f, 0.9f), PartPose.offsetAndRotation(-1, 4.2f, 0, 0, 0, 0));
        PartDefinition leftForearm = leftUpperArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(51, 22).mirror(true).addBox(-1.8f, 0, -2, 3.8f, 3, 4)
                .texOffs(107, 12).mirror(true).addBox(-1.6f, 2.8f, -1.8f, 3.4f, 3.4f, 3.6f)
                .texOffs(68, 22).mirror(true).addBox(-1.65f, 6, -1.85f, 3.5f, 2.3f, 3.7f)
                .texOffs(40, 31).mirror(true).addBox(-1.6f, 8.1f, -1.95f, 3.4f, 1, 2.9f)
                .texOffs(80, 31).mirror(true).addBox(-1.75f, 6.3f, -2.4f, 0.9f, 1.7f, 0.9f), PartPose.offsetAndRotation(1, 4.2f, 0, 0, 0, 0));
        PartDefinition rightThigh = rightLeg.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                .texOffs(29, 0).mirror(false).addBox(-2.2f, -0.1f, -2.2f, 4.4f, 6, 4.4f)
                .texOffs(38, 12).mirror(false).addBox(-1.95f, 5.8f, -1.95f, 3.9f, 4.8f, 3.9f)
                .texOffs(55, 31).mirror(false).addBox(-1.6f, 6.2f, 1.9f, 3.2f, 2.6f, 0.5f)
                .texOffs(0, 22).mirror(false).addBox(-2, 10.5f, -3.3f, 4, 1.5f, 5.1f)
                .texOffs(85, 31).mirror(false).addBox(-1.85f, 10.9f, -4.15f, 3.1f, 1.1f, 0.9f)
                .texOffs(66, 31).mirror(false).addBox(1.05f, 10.85f, -3.75f, 0.9f, 1.15f, 1.3f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.04f));
        PartDefinition leftThigh = leftLeg.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                .texOffs(29, 0).mirror(true).addBox(-2.2f, -0.1f, -2.2f, 4.4f, 6, 4.4f)
                .texOffs(38, 12).mirror(true).addBox(-1.95f, 5.8f, -1.95f, 3.9f, 4.8f, 3.9f)
                .texOffs(55, 31).mirror(true).addBox(-1.6f, 6.2f, 1.9f, 3.2f, 2.6f, 0.5f)
                .texOffs(0, 22).mirror(true).addBox(-2, 10.5f, -3.3f, 4, 1.5f, 5.1f)
                .texOffs(85, 31).mirror(true).addBox(-1.25f, 10.9f, -4.15f, 3.1f, 1.1f, 0.9f)
                .texOffs(66, 31).mirror(true).addBox(-1.95f, 10.85f, -3.75f, 0.9f, 1.15f, 1.3f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.04f));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(85, 22).mirror(false).addBox(-0.65f, 0, -0.65f, 1.3f, 4.4f, 1.3f), PartPose.offsetAndRotation(0, 10.4f, 1.9f, 0.95f, 0, 0));
        PartDefinition tail1 = tail.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(85, 22).mirror(false).addBox(-0.6f, 0, -0.6f, 1.2f, 4.2f, 1.2f), PartPose.offsetAndRotation(0, 4.1f, 0, 0.6f, 0, 0));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(94, 22).mirror(false).addBox(-0.55f, 0, -0.55f, 1.1f, 4, 1.1f), PartPose.offsetAndRotation(0, 3.9f, 0, 0.6f, 0, 0));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create()
                .texOffs(26, 31).mirror(false).addBox(-0.5f, 0, -0.5f, 1, 3.4f, 1), PartPose.offsetAndRotation(0, 3.7f, 0, 0.5f, 0, 0));
        PartDefinition tailTip = tail3.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(31, 31).mirror(false).addBox(-0.75f, 0, -0.75f, 1.5f, 3, 1.5f), PartPose.offsetAndRotation(0, 3.1f, 0, 0.25f, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }
}
