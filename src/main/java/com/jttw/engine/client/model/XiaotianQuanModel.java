package com.jttw.engine.client.model;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.XiaotianQuanEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Xiaotian Quan, Erlang Shen's black hound (512x512 skin): a lean, noble hound, deep-chested and
 * tuck-waisted, long-legged, a long head with pricked ears, a sickle tail; a gold collar with a bell and a
 * red tassel; gold cloud-scroll markings and amber eyes (crimson, burning, when berserk). Model and skins:
 * {@code assets_source/xiaotian_quan.py}. Its groups (head, upper_body, lower_body, the four legs, tail)
 * are what {@link XiaotianQuanAnimations#WALK} moves.
 */
@OnlyIn(Dist.CLIENT)
public class XiaotianQuanModel extends HierarchicalModel<XiaotianQuanEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "xiaotian_quan"), "main");

    private final ModelPart root;

    public XiaotianQuanModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition upperBody = root.addOrReplaceChild("upper_body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-2.924f, -11.61f, -7.052f, 5.848f, 5.504f, 7.396f)
                .texOffs(70, 15).mirror(false).addBox(-2.236f, -6.278f, -6.364f, 4.472f, 1.032f, 4.816f)
                .texOffs(91, 15).mirror(false).addBox(-2.15f, -10.836f, -7.912f, 4.3f, 4.128f, 1.032f)
                .texOffs(41, 25).mirror(false).addBox(-2.064f, -12.126f, -6.364f, 4.128f, 0.688f, 4.3f)
                .texOffs(73, 33).mirror(false).addBox(-2.494f, -7.224f, -0.344f, 4.988f, 1.032f, 1.892f), PartPose.offsetAndRotation(0, 24, 0, 0, 0, 0));
        PartDefinition rightShoulder = upperBody.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(90, 0).mirror(false).addBox(-0.516f, -0.43f, -1.892f, 0.86f, 4.644f, 3.956f), PartPose.offsetAndRotation(-2.838f, -10.836f, -5.332f, 0.3f, 0, 0.06f));
        PartDefinition leftShoulder = upperBody.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(90, 0).mirror(true).addBox(-0.344f, -0.43f, -1.892f, 0.86f, 4.644f, 3.956f), PartPose.offsetAndRotation(2.838f, -10.836f, -5.332f, 0.3f, 0, -0.06f));
        PartDefinition neck = upperBody.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(101, 0).mirror(false).addBox(-1.763f, -3.956f, -1.935f, 3.526f, 4.816f, 3.87f)
                .texOffs(13, 15).mirror(false).addBox(-1.505f, -7.396f, -1.677f, 3.01f, 3.784f, 3.354f)
                .texOffs(106, 15).mirror(false).addBox(-1.032f, -7.052f, 1.333f, 2.064f, 5.332f, 0.774f)
                .texOffs(107, 25).mirror(false).addBox(-1.29f, -4.644f, -2.365f, 2.58f, 3.956f, 0.688f), PartPose.offsetAndRotation(0, -9.804f, -5.676f, 0.62f, 0, 0));
        PartDefinition collar = neck.addOrReplaceChild("collar", CubeListBuilder.create()
                .texOffs(0, 25).mirror(false).addBox(-2.064f, -0.645f, -2.236f, 4.128f, 1.29f, 4.472f), PartPose.offsetAndRotation(0, -2.236f, 0, 0, 0, 0));
        PartDefinition bell = collar.addOrReplaceChild("bell", CubeListBuilder.create()
                .texOffs(88, 33).mirror(false).addBox(-0.688f, 0, -0.688f, 1.376f, 1.29f, 1.376f)
                .texOffs(0, 39).mirror(false).addBox(-0.86f, 1.075f, -0.86f, 1.72f, 0.43f, 1.72f)
                .texOffs(22, 39).mirror(false).addBox(-0.301f, -0.43f, -0.301f, 0.602f, 0.516f, 0.602f), PartPose.offsetAndRotation(0, 0.473f, -2.365f, -0.62f, 0, 0));
        PartDefinition tassel = bell.addOrReplaceChild("tassel", CubeListBuilder.create()
                .texOffs(27, 39).mirror(false).addBox(-0.43f, 0, -0.43f, 0.86f, 0.516f, 0.86f)
                .texOffs(97, 33).mirror(false).addBox(-0.387f, 0.43f, -0.387f, 0.774f, 2.064f, 0.774f), PartPose.offsetAndRotation(0, 1.505f, 0, 0, 0, 0));
        PartDefinition lowerBody = root.addOrReplaceChild("lower_body", CubeListBuilder.create()
                .texOffs(29, 0).mirror(false).addBox(-2.236f, -2.408f, -3.784f, 4.472f, 3.44f, 5.504f)
                .texOffs(52, 0).mirror(false).addBox(-2.666f, -2.58f, 0.86f, 5.332f, 4.644f, 4.988f)
                .texOffs(30, 15).mirror(false).addBox(-1.892f, -2.924f, -1.72f, 3.784f, 0.516f, 6.536f), PartPose.offsetAndRotation(0, 14.97f, 3.44f, 0, 0, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(53, 15).mirror(false).addBox(-1.978f, -2.236f, -3.354f, 3.956f, 3.268f, 3.956f)
                .texOffs(62, 25).mirror(false).addBox(-2.15f, -0.516f, -3.01f, 4.3f, 2.064f, 2.408f)
                .texOffs(9, 39).mirror(false).addBox(-1.677f, -2.537f, -3.741f, 3.354f, 0.645f, 1.032f)
                .texOffs(116, 25).mirror(false).addBox(-1.161f, -1.118f, -5.504f, 2.322f, 1.591f, 2.322f)
                .texOffs(0, 33).mirror(false).addBox(-0.86f, -0.989f, -7.654f, 1.72f, 1.376f, 2.236f)
                .texOffs(32, 39).mirror(false).addBox(-0.645f, -1.204f, -8.041f, 1.29f, 0.86f, 0.688f)
                .texOffs(79, 25).mirror(false).addBox(-0.989f, 0.172f, -7.224f, 1.978f, 0.559f, 4.042f)
                .texOffs(102, 33).mirror(false).addBox(-1.376f, -1.118f, 0.172f, 2.752f, 2.236f, 0.86f), PartPose.offsetAndRotation(0, 9.036f, -9.288f, 0, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(11, 33).mirror(false).addBox(-0.946f, 0, -3.87f, 1.892f, 0.86f, 3.87f), PartPose.offsetAndRotation(0, 0.516f, -3.01f, 0.07f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(24, 33).mirror(false).addBox(-0.817f, -2.838f, -0.301f, 1.634f, 3.01f, 0.602f), PartPose.offsetAndRotation(-1.247f, -1.978f, -0.946f, -0.42f, 0.1f, -0.2f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(24, 33).mirror(true).addBox(-0.817f, -2.838f, -0.301f, 1.634f, 3.01f, 0.602f), PartPose.offsetAndRotation(1.247f, -1.978f, -0.946f, -0.42f, -0.1f, 0.2f));
        PartDefinition frontRight = root.addOrReplaceChild("front_right", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.935f, 14.54f, -5.074f, 0, 0, 0));
        PartDefinition frontLeft = root.addOrReplaceChild("front_left", CubeListBuilder.create(), PartPose.offsetAndRotation(1.935f, 14.54f, -5.074f, 0, 0, 0));
        PartDefinition rearRight = root.addOrReplaceChild("rear_right", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.935f, 15.056f, 7.396f, 0, 0, 0));
        PartDefinition rearLeft = root.addOrReplaceChild("rear_left", CubeListBuilder.create(), PartPose.offsetAndRotation(1.935f, 15.056f, 7.396f, 0, 0, 0));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 13.164f, 8.944f, 0, 0, 0));
        PartDefinition rightUpperarm = frontRight.addOrReplaceChild("right_upperarm", CubeListBuilder.create()
                .texOffs(0, 15).mirror(false).addBox(-1.161f, -1.204f, -1.376f, 2.322f, 5.074f, 2.752f), PartPose.offsetAndRotation(0, 0, 0, 0.18f, 0, 0));
        PartDefinition rightForearm = rightUpperarm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(21, 25).mirror(false).addBox(-0.774f, -0.344f, -0.817f, 1.548f, 4.3f, 1.634f), PartPose.offsetAndRotation(0, 3.784f, 0, -0.18f, 0, 0));
        PartDefinition rightPastern = rightForearm.addOrReplaceChild("right_pastern", CubeListBuilder.create()
                .texOffs(111, 33).mirror(false).addBox(-0.688f, -0.258f, -0.731f, 1.376f, 1.376f, 1.462f), PartPose.offsetAndRotation(0, 3.87f, 0, -0.22f, 0, 0));
        PartDefinition rightForepaw = rightPastern.addOrReplaceChild("right_forepaw", CubeListBuilder.create()
                .texOffs(31, 33).mirror(false).addBox(-1.032f, 0, -1.72f, 2.064f, 1.032f, 2.408f)
                .texOffs(39, 39).mirror(false).addBox(-0.903f, 0.215f, -2.365f, 1.806f, 0.817f, 0.688f)
                .texOffs(46, 39).mirror(false).addBox(-0.774f, 0.731f, -2.623f, 1.548f, 0.301f, 0.344f), PartPose.offsetAndRotation(0, 1.075f, 0, 0.22f, 0, 0));
        PartDefinition rightThigh = rearRight.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                .texOffs(75, 0).mirror(false).addBox(-1.333f, -1.72f, -1.978f, 2.666f, 5.504f, 3.784f), PartPose.offsetAndRotation(0, 0, 0, -0.32f, 0, 0));
        PartDefinition rightGaskin = rightThigh.addOrReplaceChild("right_gaskin", CubeListBuilder.create()
                .texOffs(30, 25).mirror(false).addBox(-0.903f, -0.43f, -0.989f, 1.806f, 3.87f, 2.064f), PartPose.offsetAndRotation(0, 3.44f, -0.086f, 0.95f, 0, 0));
        PartDefinition rightHock = rightGaskin.addOrReplaceChild("right_hock", CubeListBuilder.create()
                .texOffs(44, 33).mirror(false).addBox(-0.688f, -0.344f, -0.731f, 1.376f, 2.58f, 1.462f), PartPose.offsetAndRotation(0, 3.268f, 0, -0.7f, 0, 0));
        PartDefinition rightHindpaw = rightHock.addOrReplaceChild("right_hindpaw", CubeListBuilder.create()
                .texOffs(31, 33).mirror(false).addBox(-1.032f, 0, -1.72f, 2.064f, 1.032f, 2.408f)
                .texOffs(39, 39).mirror(false).addBox(-0.903f, 0.215f, -2.365f, 1.806f, 0.817f, 0.688f)
                .texOffs(46, 39).mirror(false).addBox(-0.774f, 0.731f, -2.623f, 1.548f, 0.301f, 0.344f), PartPose.offsetAndRotation(0, 2.107f, 0, 0.07f, 0, 0));
        PartDefinition leftUpperarm = frontLeft.addOrReplaceChild("left_upperarm", CubeListBuilder.create()
                .texOffs(0, 15).mirror(true).addBox(-1.161f, -1.204f, -1.376f, 2.322f, 5.074f, 2.752f), PartPose.offsetAndRotation(0, 0, 0, 0.18f, 0, 0));
        PartDefinition leftForearm = leftUpperarm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(21, 25).mirror(true).addBox(-0.774f, -0.344f, -0.817f, 1.548f, 4.3f, 1.634f), PartPose.offsetAndRotation(0, 3.784f, 0, -0.18f, 0, 0));
        PartDefinition leftPastern = leftForearm.addOrReplaceChild("left_pastern", CubeListBuilder.create()
                .texOffs(111, 33).mirror(true).addBox(-0.688f, -0.258f, -0.731f, 1.376f, 1.376f, 1.462f), PartPose.offsetAndRotation(0, 3.87f, 0, -0.22f, 0, 0));
        PartDefinition leftForepaw = leftPastern.addOrReplaceChild("left_forepaw", CubeListBuilder.create()
                .texOffs(31, 33).mirror(true).addBox(-1.032f, 0, -1.72f, 2.064f, 1.032f, 2.408f)
                .texOffs(39, 39).mirror(true).addBox(-0.903f, 0.215f, -2.365f, 1.806f, 0.817f, 0.688f)
                .texOffs(46, 39).mirror(true).addBox(-0.774f, 0.731f, -2.623f, 1.548f, 0.301f, 0.344f), PartPose.offsetAndRotation(0, 1.075f, 0, 0.22f, 0, 0));
        PartDefinition leftThigh = rearLeft.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                .texOffs(75, 0).mirror(true).addBox(-1.333f, -1.72f, -1.978f, 2.666f, 5.504f, 3.784f), PartPose.offsetAndRotation(0, 0, 0, -0.32f, 0, 0));
        PartDefinition leftGaskin = leftThigh.addOrReplaceChild("left_gaskin", CubeListBuilder.create()
                .texOffs(30, 25).mirror(true).addBox(-0.903f, -0.43f, -0.989f, 1.806f, 3.87f, 2.064f), PartPose.offsetAndRotation(0, 3.44f, -0.086f, 0.95f, 0, 0));
        PartDefinition leftHock = leftGaskin.addOrReplaceChild("left_hock", CubeListBuilder.create()
                .texOffs(44, 33).mirror(true).addBox(-0.688f, -0.344f, -0.731f, 1.376f, 2.58f, 1.462f), PartPose.offsetAndRotation(0, 3.268f, 0, -0.7f, 0, 0));
        PartDefinition leftHindpaw = leftHock.addOrReplaceChild("left_hindpaw", CubeListBuilder.create()
                .texOffs(31, 33).mirror(true).addBox(-1.032f, 0, -1.72f, 2.064f, 1.032f, 2.408f)
                .texOffs(39, 39).mirror(true).addBox(-0.903f, 0.215f, -2.365f, 1.806f, 0.817f, 0.688f)
                .texOffs(46, 39).mirror(true).addBox(-0.774f, 0.731f, -2.623f, 1.548f, 0.301f, 0.344f), PartPose.offsetAndRotation(0, 2.107f, 0, 0.07f, 0, 0));
        PartDefinition tail0 = tail.addOrReplaceChild("tail_0", CubeListBuilder.create()
                .texOffs(94, 25).mirror(false).addBox(-0.688f, -0.688f, -0.172f, 1.376f, 1.376f, 3.096f)
                .texOffs(53, 33).mirror(false).addBox(-0.258f, 0.602f, 0.086f, 0.516f, 1.118f, 2.752f), PartPose.offsetAndRotation(0, 0, 0, 0.55f, 0, 0));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(62, 33).mirror(false).addBox(-0.6235f, -0.6235f, -0.172f, 1.247f, 1.247f, 2.924f)
                .texOffs(53, 33).mirror(false).addBox(-0.258f, 0.5375f, 0.086f, 0.516f, 1.376f, 2.58f), PartPose.offsetAndRotation(0, 0, 2.795f, 0.45f, 0, 0));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(62, 33).mirror(false).addBox(-0.559f, -0.559f, -0.172f, 1.118f, 1.118f, 2.666f)
                .texOffs(53, 33).mirror(false).addBox(-0.258f, 0.473f, 0.086f, 0.516f, 1.634f, 2.322f), PartPose.offsetAndRotation(0, 0, 2.623f, 0.55f, 0, 0));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create()
                .texOffs(120, 33).mirror(false).addBox(-0.4515f, -0.4515f, -0.172f, 0.903f, 0.903f, 2.408f)
                .texOffs(53, 33).mirror(false).addBox(-0.258f, 0.3655f, 0.086f, 0.516f, 1.892f, 2.064f), PartPose.offsetAndRotation(0, 0, 2.365f, 0.65f, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
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
