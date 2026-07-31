package com.jttw.engine.client.model;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.XiaotianQuanEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Xiaotian Quan model built from Blockbench geometry.
 * Uses vanilla HierarchicalModel + AnimationDefinition system.
 *
 * Coordinate conversion from Blockbench (flip_y modded entity):
 *   Java pivot: (-bb_origin_x, 24 - bb_origin_y, bb_origin_z)
 *   Cube offset: (bb_origin_x - bb_to_x, bb_origin_y - bb_to_y, bb_from_z - bb_origin_z)
 *   Cube rotation: (-bb_rot_x, -bb_rot_y, bb_rot_z) in radians
 */
@OnlyIn(Dist.CLIENT)
public class XiaotianQuanModel extends HierarchicalModel<XiaotianQuanEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(JTTWEngine.MOD_ID, "xiaotian_quan"), "main");

    private final ModelPart root;

    public XiaotianQuanModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDef = new MeshDefinition();
        PartDefinition partDef = meshDef.getRoot();

        // ── head group ── bb_origin: [-0.875, 10.65054, -5.55583]
        // java pivot: (0.875, 13.3495, -5.5558)
        PartDefinition head = partDef.addOrReplaceChild("head",
                CubeListBuilder.create(),
                PartPose.offset(0.875F, 13.3495F, -5.5558F));

        // head cube: from[-2.5,11,-11] to[1.5,16,-6], rot[-7.5,0,0], origin[-1.5,11,-10], uv[30,15]
        // sub-pivot relative to head: (-0.875-(-1.5), 10.65054-11, -10-(-5.55583)) = (0.625, -0.3495, -4.4442)
        // cube offset: (-1.5-1.5, 11-16, -11-(-10)) = (-3, -5, -1), size(4,5,5)
        head.addOrReplaceChild("head_cube",
                CubeListBuilder.create().texOffs(30, 15)
                        .addBox(-3.0F, -5.0F, -1.0F, 4.0F, 5.0F, 5.0F),
                PartPose.offsetAndRotation(0.625F, -0.3495F, -4.4442F,
                        0.1309F, 0.0F, 0.0F)); // -(-7.5deg) = 7.5deg

        // mouth: from[-1.5,11.5,-13] to[0.5,13.5,-11], rot[-7.5,0,0], origin[-0.5,11,-12], uv[42,11]
        // sub-pivot: (-0.875-(-0.5), 10.65054-11, -12-(-5.55583)) = (-0.375, -0.3495, -6.4442)
        // cube offset: (-0.5-0.5, 11-13.5, -13-(-12)) = (-1, -2.5, -1), size(2,2,2)
        head.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(42, 11)
                        .addBox(-1.0F, -2.5F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-0.375F, -0.3495F, -6.4442F,
                        0.1309F, 0.0F, 0.0F));

        // neck: from[-3,7,-6] to[2,14,-2], rot[-35,0,0], origin[-2,7,-5], uv[0,30]
        // sub-pivot: (-0.875-(-2), 10.65054-7, -5-(-5.55583)) = (1.125, 3.6505, 0.5558)
        // cube offset: (-2-2, 7-14, -6-(-5)) = (-4, -7, -1), size(5,7,4)
        head.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 30)
                        .addBox(-4.0F, -7.0F, -1.0F, 5.0F, 7.0F, 4.0F),
                PartPose.offsetAndRotation(1.125F, 3.6505F, 0.5558F,
                        0.6109F, 0.0F, 0.0F)); // -(-35deg) = 35deg

        // ears sub-group: bb_origin[-2,14,-11]
        // sub-pivot relative to head: (-0.875-(-2), 10.65054-14, -11-(-5.55583)) = (1.125, -3.3495, -5.4442)
        PartDefinition ears = head.addOrReplaceChild("ears",
                CubeListBuilder.create(),
                PartPose.offset(1.125F, -3.3495F, -5.4442F));

        // ear left: from[-3,16,-9] to[-2,18,-7], rot[-7.5,0,0], origin[-2,16,-8], uv[28,38]
        // sub-pivot relative to ears: (-2-(-2), 14-16, -8-(-11)) = (0, -2, 3)
        // cube offset: (-2-(-2), 16-18, -9-(-8)) = (0, -2, -1), size(1,2,2)
        ears.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(28, 38)
                        .addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 3.0F,
                        0.1309F, 0.0F, 0.0F));

        // ear right: from[1,16,-9] to[2,18,-7], rot[-7.5,0,0], origin[2,16,-8], uv[42,25]
        // sub-pivot relative to ears: (-2-2, 14-16, -8-(-11)) = (-4, -2, 3)
        // cube offset: (2-2, 16-18, -9-(-8)) = (0, -2, -1), size(1,2,2)
        ears.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(42, 25)
                        .addBox(0.0F, -2.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, -2.0F, 3.0F,
                        0.1309F, 0.0F, 0.0F));

        // ── upper_body group ── bb_origin: [0, 0, 0]
        // java pivot: (0, 24, 0)
        // upper_body cube: from[-4,6,-6] to[3,13,2], no rotation, uv[0,15]
        // offset: (0-3, 0-13, -6-0) = (-3, -13, -6), size(7,7,8)
        partDef.addOrReplaceChild("upper_body",
                CubeListBuilder.create().texOffs(0, 15)
                        .addBox(-3.0F, -13.0F, -6.0F, 7.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // ── lower_body group ── bb_origin: [-0.5, 9.44543, 5.58391]
        // java pivot: (0.5, 14.5546, 5.5839)
        PartDefinition lowerBody = partDef.addOrReplaceChild("lower_body",
                CubeListBuilder.create(),
                PartPose.offset(0.5F, 14.5546F, 5.5839F));

        // lower_body cube: from[-4,6.6,0] to[3,12.6,10], rot[2.5,0,0], origin[-3,7.6,2], uv[0,0]
        // sub-pivot: (-0.5-(-3), 9.44543-7.6, 2-5.58391) = (2.5, 1.8454, -3.5839)
        // cube offset: (-3-3, 7.6-12.6, 0-2) = (-6, -5, -2), size(7,6,10)
        lowerBody.addOrReplaceChild("lower_body_cube",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-6.0F, -5.0F, -2.0F, 7.0F, 6.0F, 10.0F),
                PartPose.offsetAndRotation(2.5F, 1.8454F, -3.5839F,
                        -0.0436F, 0.0F, 0.0F)); // -(2.5deg)

        // ── rear_right group ── bb_origin: [2, 10.44879, 8.86689]
        // java pivot: (-2, 13.5512, 8.8669)
        PartDefinition rearRight = partDef.addOrReplaceChild("rear_right",
                CubeListBuilder.create(),
                PartPose.offset(-2.0F, 13.5512F, 8.8669F));

        // foot: from[1,0,6.5] to[3,1,9.5], no rotation, uv[33,1]
        // offset: (2-3, 10.44879-1, 6.5-8.86689) = (-1, 9.4488, -2.3669), size(2,1,3)
        rearRight.addOrReplaceChild("rr_foot",
                CubeListBuilder.create().texOffs(33, 1)
                        .addBox(-1.0F, 9.4488F, -2.3669F, 2.0F, 1.0F, 3.0F),
                PartPose.ZERO);

        // upper: from[1,2.7,8.5] to[3,6.7,10.5], rot[-12.5,0,0], origin[2.5,3.7,10], uv[40,5]
        // sub-pivot: (2-2.5, 10.44879-3.7, 10-8.86689) = (-0.5, 6.7488, 1.1331)
        // cube offset: (2.5-3, 3.7-6.7, 8.5-10) = (-0.5, -3, -1.5), size(2,4,2)
        rearRight.addOrReplaceChild("rr_upper",
                CubeListBuilder.create().texOffs(40, 5)
                        .addBox(-0.5F, -3.0F, -1.5F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(-0.5F, 6.7488F, 1.1331F,
                        0.2182F, 0.0F, 0.0F)); // -(-12.5deg)

        // lower: from[1,0.7,7.9] to[3,3.7,9.9], rot[15,0,0], origin[2.5,0.7,9.4], uv[34,36]
        // sub-pivot: (2-2.5, 10.44879-0.7, 9.4-8.86689) = (-0.5, 9.7488, 0.5331)
        // cube offset: (2.5-3, 0.7-3.7, 7.9-9.4) = (-0.5, -3, -1.5), size(2,3,2)
        rearRight.addOrReplaceChild("rr_lower",
                CubeListBuilder.create().texOffs(34, 36)
                        .addBox(-0.5F, -3.0F, -1.5F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-0.5F, 9.7488F, 0.5331F,
                        -0.2618F, 0.0F, 0.0F)); // -(15deg)

        // ── rear_left group ── bb_origin: [-3, 10.44879, 8.86689]
        // java pivot: (3, 13.5512, 8.8669)
        PartDefinition rearLeft = partDef.addOrReplaceChild("rear_left",
                CubeListBuilder.create(),
                PartPose.offset(3.0F, 13.5512F, 8.8669F));

        // foot: from[-4,0,6.5] to[-2,1,9.5], no rotation, uv[31,26]
        // offset: (-3-(-2), 10.44879-1, 6.5-8.86689) = (-1, 9.4488, -2.3669), size(2,1,3)
        rearLeft.addOrReplaceChild("rl_foot",
                CubeListBuilder.create().texOffs(31, 26)
                        .addBox(-1.0F, 9.4488F, -2.3669F, 2.0F, 1.0F, 3.0F),
                PartPose.ZERO);

        // upper: from[-4,2.7,8.5] to[-2,6.7,10.5], rot[-12.5,0,0], origin[-2.5,3.7,10], uv[32,5]
        // sub-pivot: (-3-(-2.5), 10.44879-3.7, 10-8.86689) = (-0.5, 6.7488, 1.1331)
        // cube offset: (-2.5-(-2), 3.7-6.7, 8.5-10) = (-0.5, -3, -1.5), size(2,4,2)
        rearLeft.addOrReplaceChild("rl_upper",
                CubeListBuilder.create().texOffs(32, 5)
                        .addBox(-0.5F, -3.0F, -1.5F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(-0.5F, 6.7488F, 1.1331F,
                        0.2182F, 0.0F, 0.0F));

        // lower: from[-4,0.7,7.9] to[-2,3.7,9.9], rot[15,0,0], origin[-2.5,0.7,9.4], uv[34,30]
        // sub-pivot: (-3-(-2.5), 10.44879-0.7, 9.4-8.86689) = (-0.5, 9.7488, 0.5331)
        // cube offset: (-2.5-(-2), 0.7-3.7, 7.9-9.4) = (-0.5, -3, -1.5), size(2,3,2)
        rearLeft.addOrReplaceChild("rl_lower",
                CubeListBuilder.create().texOffs(34, 30)
                        .addBox(-0.5F, -3.0F, -1.5F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-0.5F, 9.7488F, 0.5331F,
                        -0.2618F, 0.0F, 0.0F));

        // ── front_right group ── bb_origin: [2, 10.54528, -4.6613]
        // java pivot: (-2, 13.4547, -4.6613)
        PartDefinition frontRight = partDef.addOrReplaceChild("front_right",
                CubeListBuilder.create(),
                PartPose.offset(-2.0F, 13.4547F, -4.6613F));

        // foot: from[1,0,-6.4] to[3,1,-3.4], no rotation, uv[18,38]
        // offset: (2-3, 10.54528-1, -6.4-(-4.6613)) = (-1, 9.5453, -1.7387), size(2,1,3)
        frontRight.addOrReplaceChild("fr_foot",
                CubeListBuilder.create().texOffs(18, 38)
                        .addBox(-1.0F, 9.5453F, -1.7387F, 2.0F, 1.0F, 3.0F),
                PartPose.ZERO);

        // upper: from[1,3,-5.4] to[3,6,-3.4], rot[-12.5,0,0], origin[2,3,-5.4], uv[24,42]
        // sub-pivot: (2-2, 10.54528-3, -5.4-(-4.6613)) = (0, 7.5453, -0.7387)
        // cube offset: (2-3, 3-6, -5.4-(-5.4)) = (-1, -3, 0), size(2,3,2)
        frontRight.addOrReplaceChild("fr_upper",
                CubeListBuilder.create().texOffs(24, 42)
                        .addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 7.5453F, -0.7387F,
                        0.2182F, 0.0F, 0.0F));

        // lower: from[1,1,-5.4] to[3,4,-3.4], rot[2.5,0,0], origin[2,1,-5.4], uv[16,42]
        // sub-pivot: (2-2, 10.54528-1, -5.4-(-4.6613)) = (0, 9.5453, -0.7387)
        // cube offset: (2-3, 1-4, -5.4-(-5.4)) = (-1, -3, 0), size(2,3,2)
        frontRight.addOrReplaceChild("fr_lower",
                CubeListBuilder.create().texOffs(16, 42)
                        .addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 9.5453F, -0.7387F,
                        -0.0436F, 0.0F, 0.0F));

        // ── front_left group ── bb_origin: [-3, 10.54528, -4.6613]
        // java pivot: (3, 13.4547, -4.6613)
        PartDefinition frontLeft = partDef.addOrReplaceChild("front_left",
                CubeListBuilder.create(),
                PartPose.offset(3.0F, 13.4547F, -4.6613F));

        // foot: from[-4,0,-6.4] to[-2,1,-3.4], no rotation, uv[32,11]
        // offset: (-3-(-2), 10.54528-1, -6.4-(-4.6613)) = (-1, 9.5453, -1.7387), size(2,1,3)
        frontLeft.addOrReplaceChild("fl_foot",
                CubeListBuilder.create().texOffs(32, 11)
                        .addBox(-1.0F, 9.5453F, -1.7387F, 2.0F, 1.0F, 3.0F),
                PartPose.ZERO);

        // upper: from[-4,3,-5.4] to[-2,6,-3.4], rot[-12.5,0,0], origin[-3,3,-5.4], uv[0,41]
        // sub-pivot: (-3-(-3), 10.54528-3, -5.4-(-4.6613)) = (0, 7.5453, -0.7387)
        // cube offset: (-3-(-2), 3-6, -5.4-(-5.4)) = (-1, -3, 0), size(2,3,2)
        frontLeft.addOrReplaceChild("fl_upper",
                CubeListBuilder.create().texOffs(0, 41)
                        .addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 7.5453F, -0.7387F,
                        0.2182F, 0.0F, 0.0F));

        // lower: from[-4,1,-5.4] to[-2,4,-3.4], rot[2.5,0,0], origin[-3,1,-5.4], uv[8,41]
        // sub-pivot: (-3-(-3), 10.54528-1, -5.4-(-4.6613)) = (0, 9.5453, -0.7387)
        // cube offset: (-3-(-2), 1-4, -5.4-(-5.4)) = (-1, -3, 0), size(2,3,2)
        frontLeft.addOrReplaceChild("fl_lower",
                CubeListBuilder.create().texOffs(8, 41)
                        .addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 9.5453F, -0.7387F,
                        -0.0436F, 0.0F, 0.0F));

        // ── tail group ── bb_origin: [-1, 11.27927, 8.95746]
        // java pivot: (1, 12.7207, 8.9575)
        PartDefinition tail = partDef.addOrReplaceChild("tail",
                CubeListBuilder.create(),
                PartPose.offset(1.0F, 12.7207F, 8.9575F));

        // tail cube: from[-2,10,9] to[0,12,16], rot[35,0,0], origin[-1,11,9.5], uv[17,29]
        // sub-pivot: (-1-(-1), 11.27927-11, 9.5-8.95746) = (0, 0.2793, 0.5425)
        // cube offset: (-1-0, 11-12, 9-9.5) = (-1, -1, -0.5), size(2,2,7)
        tail.addOrReplaceChild("tail_cube",
                CubeListBuilder.create().texOffs(17, 29)
                        .addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 0.2793F, 0.5425F,
                        -0.6109F, 0.0F, 0.0F)); // -(35deg)

        return LayerDefinition.create(meshDef, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(XiaotianQuanEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        if (limbSwingAmount > 0.01F) {
            entity.walkAnimState.startIfStopped(entity.tickCount);
        } else {
            entity.walkAnimState.stop();
        }
        this.animate(entity.walkAnimState, XiaotianQuanAnimations.WALK, ageInTicks);
    }
}
