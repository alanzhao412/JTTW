package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.FightingBuddhaClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Victorious Fighting Buddha's look, worn over Sun Wukong's own (and over his armour) once he is
 * ascended (FightingBuddhaClient): the golden kasaya, the red-and-gold sash and its ring, the
 * gold-ringed fillet, a gold armlet, the halo of golden light turning behind his head and the aura of
 * golden flame behind his body, these last drawn full-bright. Its model and skins come from
 * assets_source/fighting_buddha.py; each piece hangs from a copy of the player's head, body, arms or
 * legs, posed as the player is each frame.
 */
public class FightingBuddhaLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("fighting_buddha"), "main");
    private static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/fighting_buddha.png");
    private static final ResourceLocation GLOW = JTTWEngine.id("textures/entity/fighting_buddha_glow.png");

    private final ModelPart root, head, body, rightArm, leftArm, rightLeg, leftLeg, halo, aura;

    public FightingBuddhaLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        root = models.bakeLayer(LAYER);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        halo = head.getChild("halo");
        aura = body.getChild("aura");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p,
                       float swing, float swingAmount, float partial, float age, float headYaw, float headPitch) {
        if (p.isInvisible() || !FightingBuddhaClient.ascended(p)) return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        head.copyFrom(m.head);
        body.copyFrom(m.body);
        rightArm.copyFrom(m.rightArm);
        leftArm.copyFrom(m.leftArm);
        rightLeg.copyFrom(m.rightLeg);
        leftLeg.copyFrom(m.leftLeg);
        // the halo turns slowly; the aura breathes
        halo.zRot = age * 0.015f;
        float breath = 1.0f + 0.04f * (float) Math.sin(age * 0.08f);
        aura.xScale = aura.yScale = breath;
        root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        root.render(pose, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF000F0, OverlayTexture.NO_OVERLAY);
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
        PartDefinition robe = body.addOrReplaceChild("robe", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-5.4f, -0.4f, -3.4f, 10.8f, 12.8f, 6.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition sashFront = body.addOrReplaceChild("sash_front", CubeListBuilder.create()
                .texOffs(37, 0).mirror(false).addBox(-1.7f, -8.2f, -0.3f, 3.4f, 16.4f, 0.6f), PartPose.offsetAndRotation(0.4f, 5.6f, -3.55f, 0, 0, -0.62f));
        PartDefinition sashBack = body.addOrReplaceChild("sash_back", CubeListBuilder.create()
                .texOffs(37, 0).mirror(false).addBox(-1.7f, -8.2f, -0.3f, 3.4f, 16.4f, 0.6f), PartPose.offsetAndRotation(0.4f, 5.6f, 3.55f, 0, 0, -0.62f));
        PartDefinition clasp = body.addOrReplaceChild("clasp", CubeListBuilder.create()
                .texOffs(0, 35).mirror(false).addBox(-1.1f, -1.1f, -0.35f, 2.2f, 2.2f, 0.7f)
                .texOffs(55, 35).mirror(false).addBox(-0.5f, -0.5f, -0.55f, 1, 1, 0.4f), PartPose.offsetAndRotation(2.6f, 2.6f, -3.95f, 0, 0, 0.785f));
        PartDefinition sleeve = leftArm.addOrReplaceChild("sleeve", CubeListBuilder.create()
                .texOffs(77, 0).mirror(false).addBox(-2.4f, -3.4f, -3.4f, 6.8f, 8.6f, 6.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition sleeveHem = leftArm.addOrReplaceChild("sleeve_hem", CubeListBuilder.create()
                .texOffs(45, 21).mirror(false).addBox(-2.6f, 4.6f, -3.6f, 7.2f, 1.4f, 7.2f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition skirtRight = rightLeg.addOrReplaceChild("skirt_right", CubeListBuilder.create()
                .texOffs(48, 0).mirror(false).addBox(-3.4f, -0.4f, -3.4f, 6.8f, 9.2f, 6.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition skirtLeft = leftLeg.addOrReplaceChild("skirt_left", CubeListBuilder.create()
                .texOffs(48, 0).mirror(false).addBox(-3.4f, -0.4f, -3.4f, 6.8f, 9.2f, 6.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition hemRight = rightLeg.addOrReplaceChild("hem_right", CubeListBuilder.create()
                .texOffs(45, 21).mirror(false).addBox(-3.6f, 8.6f, -3.6f, 7.2f, 1.4f, 7.2f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition hemLeft = leftLeg.addOrReplaceChild("hem_left", CubeListBuilder.create()
                .texOffs(45, 21).mirror(false).addBox(-3.6f, 8.6f, -3.6f, 7.2f, 1.4f, 7.2f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition armlet = rightArm.addOrReplaceChild("armlet", CubeListBuilder.create()
                .texOffs(78, 21).mirror(false).addBox(-4.4f, 2.4f, -3.4f, 6.8f, 1.4f, 6.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition fillet = head.addOrReplaceChild("fillet", CubeListBuilder.create()
                .texOffs(0, 21).mirror(false).addBox(-5.3f, -6.9f, -5.3f, 10.6f, 1.5f, 10.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition filletJewel = head.addOrReplaceChild("fillet_jewel", CubeListBuilder.create()
                .texOffs(9, 35).mirror(false).addBox(-1.3f, -1.2f, -0.4f, 2.6f, 2.4f, 0.6f), PartPose.offsetAndRotation(0, -7.2f, -5.6f, 0, 0, 0));
        PartDefinition filletRingRight = head.addOrReplaceChild("fillet_ring_right", CubeListBuilder.create()
                .texOffs(107, 21).mirror(false).addBox(-0.4f, -1.4f, -1.4f, 0.8f, 2.8f, 2.8f), PartPose.offsetAndRotation(-5.6f, -6.2f, -0.6f, 0, 0, 0));
        PartDefinition filletRingLeft = head.addOrReplaceChild("fillet_ring_left", CubeListBuilder.create()
                .texOffs(107, 21).mirror(false).addBox(-0.4f, -1.4f, -1.4f, 0.8f, 2.8f, 2.8f), PartPose.offsetAndRotation(5.6f, -6.2f, -0.6f, 0, 0, 0));
        PartDefinition halo = head.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -4, 6.6f, 0, 0, 0));
        PartDefinition haloRing0 = halo.addOrReplaceChild("halo_ring_0", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(0, -7.6f, 0, 0, 0, 0));
        PartDefinition haloRing1 = halo.addOrReplaceChild("halo_ring_1", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(1.967f, -7.341f, 0, 0, 0, 0.2618f));
        PartDefinition haloRing2 = halo.addOrReplaceChild("halo_ring_2", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(3.8f, -6.5818f, 0, 0, 0, 0.5236f));
        PartDefinition haloRing3 = halo.addOrReplaceChild("halo_ring_3", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(5.374f, -5.374f, 0, 0, 0, 0.7854f));
        PartDefinition haloRing4 = halo.addOrReplaceChild("halo_ring_4", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(6.5818f, -3.8f, 0, 0, 0, 1.0472f));
        PartDefinition haloRing5 = halo.addOrReplaceChild("halo_ring_5", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(7.341f, -1.967f, 0, 0, 0, 1.309f));
        PartDefinition haloRing6 = halo.addOrReplaceChild("halo_ring_6", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(7.6f, -0, 0, 0, 0, 1.5708f));
        PartDefinition haloRing7 = halo.addOrReplaceChild("halo_ring_7", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(7.341f, 1.967f, 0, 0, 0, 1.8326f));
        PartDefinition haloRing8 = halo.addOrReplaceChild("halo_ring_8", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(6.5818f, 3.8f, 0, 0, 0, 2.0944f));
        PartDefinition haloRing9 = halo.addOrReplaceChild("halo_ring_9", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(5.374f, 5.374f, 0, 0, 0, 2.3562f));
        PartDefinition haloRing10 = halo.addOrReplaceChild("halo_ring_10", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(3.8f, 6.5818f, 0, 0, 0, 2.618f));
        PartDefinition haloRing11 = halo.addOrReplaceChild("halo_ring_11", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(1.967f, 7.341f, 0, 0, 0, 2.8798f));
        PartDefinition haloRing12 = halo.addOrReplaceChild("halo_ring_12", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(0, 7.6f, 0, 0, 0, 3.1416f));
        PartDefinition haloRing13 = halo.addOrReplaceChild("halo_ring_13", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-1.967f, 7.341f, 0, 0, 0, 3.4034f));
        PartDefinition haloRing14 = halo.addOrReplaceChild("halo_ring_14", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-3.8f, 6.5818f, 0, 0, 0, 3.6652f));
        PartDefinition haloRing15 = halo.addOrReplaceChild("halo_ring_15", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-5.374f, 5.374f, 0, 0, 0, 3.927f));
        PartDefinition haloRing16 = halo.addOrReplaceChild("halo_ring_16", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-6.5818f, 3.8f, 0, 0, 0, 4.1888f));
        PartDefinition haloRing17 = halo.addOrReplaceChild("halo_ring_17", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-7.341f, 1.967f, 0, 0, 0, 4.4506f));
        PartDefinition haloRing18 = halo.addOrReplaceChild("halo_ring_18", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-7.6f, 0, 0, 0, 0, 4.7124f));
        PartDefinition haloRing19 = halo.addOrReplaceChild("halo_ring_19", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-7.341f, -1.967f, 0, 0, 0, 4.9742f));
        PartDefinition haloRing20 = halo.addOrReplaceChild("halo_ring_20", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-6.5818f, -3.8f, 0, 0, 0, 5.236f));
        PartDefinition haloRing21 = halo.addOrReplaceChild("halo_ring_21", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-5.374f, -5.374f, 0, 0, 0, 5.4978f));
        PartDefinition haloRing22 = halo.addOrReplaceChild("halo_ring_22", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-3.8f, -6.5818f, 0, 0, 0, 5.7596f));
        PartDefinition haloRing23 = halo.addOrReplaceChild("halo_ring_23", CubeListBuilder.create()
                .texOffs(18, 35).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 2.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-1.967f, -7.341f, 0, 0, 0, 6.0214f));
        PartDefinition haloRay0 = halo.addOrReplaceChild("halo_ray_0", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 0.2618f));
        PartDefinition haloRay1 = halo.addOrReplaceChild("halo_ray_1", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 0.7854f));
        PartDefinition haloRay2 = halo.addOrReplaceChild("halo_ray_2", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 1.309f));
        PartDefinition haloRay3 = halo.addOrReplaceChild("halo_ray_3", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 1.8326f));
        PartDefinition haloRay4 = halo.addOrReplaceChild("halo_ray_4", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 2.3562f));
        PartDefinition haloRay5 = halo.addOrReplaceChild("halo_ray_5", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 2.8798f));
        PartDefinition haloRay6 = halo.addOrReplaceChild("halo_ray_6", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 3.4034f));
        PartDefinition haloRay7 = halo.addOrReplaceChild("halo_ray_7", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 3.927f));
        PartDefinition haloRay8 = halo.addOrReplaceChild("halo_ray_8", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 4.4506f));
        PartDefinition haloRay9 = halo.addOrReplaceChild("halo_ray_9", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 4.9742f));
        PartDefinition haloRay10 = halo.addOrReplaceChild("halo_ray_10", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 5.4978f));
        PartDefinition haloRay11 = halo.addOrReplaceChild("halo_ray_11", CubeListBuilder.create()
                .texOffs(116, 21).mirror(false).addBox(-0.3f, -7, -0.1f, 0.6f, 4.6f, 0.2f), PartPose.offsetAndRotation(0, 0, 0.1f, 0, 0, 6.0214f));
        PartDefinition haloPetal0 = halo.addOrReplaceChild("halo_petal_0", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(0, -9, 0.1f, 0, 0, 0));
        PartDefinition haloPetal1 = halo.addOrReplaceChild("halo_petal_1", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(3.4442f, -8.3149f, 0.1f, 0, 0, 0.3927f));
        PartDefinition haloPetal2 = halo.addOrReplaceChild("halo_petal_2", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(6.364f, -6.364f, 0.1f, 0, 0, 0.7854f));
        PartDefinition haloPetal3 = halo.addOrReplaceChild("halo_petal_3", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(8.3149f, -3.4442f, 0.1f, 0, 0, 1.1781f));
        PartDefinition haloPetal4 = halo.addOrReplaceChild("halo_petal_4", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(9, -0, 0.1f, 0, 0, 1.5708f));
        PartDefinition haloPetal5 = halo.addOrReplaceChild("halo_petal_5", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(8.3149f, 3.4442f, 0.1f, 0, 0, 1.9635f));
        PartDefinition haloPetal6 = halo.addOrReplaceChild("halo_petal_6", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(6.364f, 6.364f, 0.1f, 0, 0, 2.3562f));
        PartDefinition haloPetal7 = halo.addOrReplaceChild("halo_petal_7", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(3.4442f, 8.3149f, 0.1f, 0, 0, 2.7489f));
        PartDefinition haloPetal8 = halo.addOrReplaceChild("halo_petal_8", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(0, 9, 0.1f, 0, 0, 3.1416f));
        PartDefinition haloPetal9 = halo.addOrReplaceChild("halo_petal_9", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(-3.4442f, 8.3149f, 0.1f, 0, 0, 3.5343f));
        PartDefinition haloPetal10 = halo.addOrReplaceChild("halo_petal_10", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(-6.364f, 6.364f, 0.1f, 0, 0, 3.927f));
        PartDefinition haloPetal11 = halo.addOrReplaceChild("halo_petal_11", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(-8.3149f, 3.4442f, 0.1f, 0, 0, 4.3197f));
        PartDefinition haloPetal12 = halo.addOrReplaceChild("halo_petal_12", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(-9, 0, 0.1f, 0, 0, 4.7124f));
        PartDefinition haloPetal13 = halo.addOrReplaceChild("halo_petal_13", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(-8.3149f, -3.4442f, 0.1f, 0, 0, 5.1051f));
        PartDefinition haloPetal14 = halo.addOrReplaceChild("halo_petal_14", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(-6.364f, -6.364f, 0.1f, 0, 0, 5.4978f));
        PartDefinition haloPetal15 = halo.addOrReplaceChild("halo_petal_15", CubeListBuilder.create()
                .texOffs(27, 35).mirror(false).addBox(-0.8f, -1.4f, -0.15f, 1.6f, 1.9f, 0.3f), PartPose.offsetAndRotation(-3.4442f, -8.3149f, 0.1f, 0, 0, 5.8905f));
        PartDefinition aura = body.addOrReplaceChild("aura", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 5, 7.2f, 0, 0, 0));
        PartDefinition auraFlame0 = aura.addOrReplaceChild("aura_flame_0", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(-6.5f, 11.2583f, 0, 0, 0, -2.618f));
        PartDefinition auraFlame1 = aura.addOrReplaceChild("aura_flame_1", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.41f, -0.12f, 1.6f, 1.21f, 0.24f)
                .texOffs(60, 35).mirror(false).addBox(-0.4f, -3.4f, -0.1f, 0.8f, 0.99f, 0.2f), PartPose.offsetAndRotation(-9.1924f, 9.1924f, 0, 0, 0, -2.3562f));
        PartDefinition auraFlame2 = aura.addOrReplaceChild("aura_flame_2", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(-11.2583f, 6.5f, 0, 0, 0, -2.0944f));
        PartDefinition auraFlame3 = aura.addOrReplaceChild("aura_flame_3", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.41f, -0.12f, 1.6f, 1.21f, 0.24f)
                .texOffs(60, 35).mirror(false).addBox(-0.4f, -3.4f, -0.1f, 0.8f, 0.99f, 0.2f), PartPose.offsetAndRotation(-12.557f, 3.3646f, 0, 0, 0, -1.8326f));
        PartDefinition auraFlame4 = aura.addOrReplaceChild("aura_flame_4", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(-13, -0, 0, 0, 0, -1.5708f));
        PartDefinition auraFlame5 = aura.addOrReplaceChild("aura_flame_5", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.41f, -0.12f, 1.6f, 1.21f, 0.24f)
                .texOffs(60, 35).mirror(false).addBox(-0.4f, -3.4f, -0.1f, 0.8f, 0.99f, 0.2f), PartPose.offsetAndRotation(-12.557f, -3.3646f, 0, 0, 0, -1.309f));
        PartDefinition auraFlame6 = aura.addOrReplaceChild("aura_flame_6", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(-11.2583f, -6.5f, 0, 0, 0, -1.0472f));
        PartDefinition auraFlame7 = aura.addOrReplaceChild("aura_flame_7", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.41f, -0.12f, 1.6f, 1.21f, 0.24f)
                .texOffs(60, 35).mirror(false).addBox(-0.4f, -3.4f, -0.1f, 0.8f, 0.99f, 0.2f), PartPose.offsetAndRotation(-9.1924f, -9.1924f, 0, 0, 0, -0.7854f));
        PartDefinition auraFlame8 = aura.addOrReplaceChild("aura_flame_8", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(-6.5f, -11.2583f, 0, 0, 0, -0.5236f));
        PartDefinition auraFlame9 = aura.addOrReplaceChild("aura_flame_9", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.41f, -0.12f, 1.6f, 1.21f, 0.24f)
                .texOffs(60, 35).mirror(false).addBox(-0.4f, -3.4f, -0.1f, 0.8f, 0.99f, 0.2f), PartPose.offsetAndRotation(-3.3646f, -12.557f, 0, 0, 0, -0.2618f));
        PartDefinition auraFlame10 = aura.addOrReplaceChild("aura_flame_10", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(0, -13, 0, 0, 0, 0));
        PartDefinition auraFlame11 = aura.addOrReplaceChild("aura_flame_11", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.41f, -0.12f, 1.6f, 1.21f, 0.24f)
                .texOffs(60, 35).mirror(false).addBox(-0.4f, -3.4f, -0.1f, 0.8f, 0.99f, 0.2f), PartPose.offsetAndRotation(3.3646f, -12.557f, 0, 0, 0, 0.2618f));
        PartDefinition auraFlame12 = aura.addOrReplaceChild("aura_flame_12", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(6.5f, -11.2583f, 0, 0, 0, 0.5236f));
        PartDefinition auraFlame13 = aura.addOrReplaceChild("aura_flame_13", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.41f, -0.12f, 1.6f, 1.21f, 0.24f)
                .texOffs(60, 35).mirror(false).addBox(-0.4f, -3.4f, -0.1f, 0.8f, 0.99f, 0.2f), PartPose.offsetAndRotation(9.1924f, -9.1924f, 0, 0, 0, 0.7854f));
        PartDefinition auraFlame14 = aura.addOrReplaceChild("aura_flame_14", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(11.2583f, -6.5f, 0, 0, 0, 1.0472f));
        PartDefinition auraFlame15 = aura.addOrReplaceChild("aura_flame_15", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.41f, -0.12f, 1.6f, 1.21f, 0.24f)
                .texOffs(60, 35).mirror(false).addBox(-0.4f, -3.4f, -0.1f, 0.8f, 0.99f, 0.2f), PartPose.offsetAndRotation(12.557f, -3.3646f, 0, 0, 0, 1.309f));
        PartDefinition auraFlame16 = aura.addOrReplaceChild("aura_flame_16", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(13, -0, 0, 0, 0, 1.5708f));
        PartDefinition auraFlame17 = aura.addOrReplaceChild("aura_flame_17", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.41f, -0.12f, 1.6f, 1.21f, 0.24f)
                .texOffs(60, 35).mirror(false).addBox(-0.4f, -3.4f, -0.1f, 0.8f, 0.99f, 0.2f), PartPose.offsetAndRotation(12.557f, 3.3646f, 0, 0, 0, 1.8326f));
        PartDefinition auraFlame18 = aura.addOrReplaceChild("aura_flame_18", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(11.2583f, 6.5f, 0, 0, 0, 2.0944f));
        PartDefinition auraFlame19 = aura.addOrReplaceChild("aura_flame_19", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.41f, -0.12f, 1.6f, 1.21f, 0.24f)
                .texOffs(60, 35).mirror(false).addBox(-0.4f, -3.4f, -0.1f, 0.8f, 0.99f, 0.2f), PartPose.offsetAndRotation(9.1924f, 9.1924f, 0, 0, 0, 2.3562f));
        PartDefinition auraFlame20 = aura.addOrReplaceChild("aura_flame_20", CubeListBuilder.create()
                .texOffs(34, 35).mirror(false).addBox(-1.2f, -1.2f, -0.15f, 2.4f, 1.2f, 0.3f)
                .texOffs(43, 35).mirror(false).addBox(-0.8f, -2.96f, -0.12f, 1.6f, 1.76f, 0.24f)
                .texOffs(50, 35).mirror(false).addBox(-0.4f, -4.4f, -0.1f, 0.8f, 1.44f, 0.2f), PartPose.offsetAndRotation(6.5f, 11.2583f, 0, 0, 0, 2.618f));
        PartDefinition auraRim0 = aura.addOrReplaceChild("aura_rim_0", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-6, 10.3923f, 0.05f, 0, 0, -2.618f));
        PartDefinition auraRim1 = aura.addOrReplaceChild("aura_rim_1", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-7.7135f, 9.1925f, 0.05f, 0, 0, -2.4435f));
        PartDefinition auraRim2 = aura.addOrReplaceChild("aura_rim_2", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-9.1925f, 7.7135f, 0.05f, 0, 0, -2.2689f));
        PartDefinition auraRim3 = aura.addOrReplaceChild("aura_rim_3", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-10.3923f, 6, 0.05f, 0, 0, -2.0944f));
        PartDefinition auraRim4 = aura.addOrReplaceChild("aura_rim_4", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-11.2763f, 4.1042f, 0.05f, 0, 0, -1.9199f));
        PartDefinition auraRim5 = aura.addOrReplaceChild("aura_rim_5", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-11.8177f, 2.0838f, 0.05f, 0, 0, -1.7453f));
        PartDefinition auraRim6 = aura.addOrReplaceChild("aura_rim_6", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-12, -0, 0.05f, 0, 0, -1.5708f));
        PartDefinition auraRim7 = aura.addOrReplaceChild("aura_rim_7", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-11.8177f, -2.0838f, 0.05f, 0, 0, -1.3963f));
        PartDefinition auraRim8 = aura.addOrReplaceChild("aura_rim_8", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-11.2763f, -4.1042f, 0.05f, 0, 0, -1.2217f));
        PartDefinition auraRim9 = aura.addOrReplaceChild("aura_rim_9", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-10.3923f, -6, 0.05f, 0, 0, -1.0472f));
        PartDefinition auraRim10 = aura.addOrReplaceChild("aura_rim_10", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-9.1925f, -7.7135f, 0.05f, 0, 0, -0.8727f));
        PartDefinition auraRim11 = aura.addOrReplaceChild("aura_rim_11", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-7.7135f, -9.1925f, 0.05f, 0, 0, -0.6981f));
        PartDefinition auraRim12 = aura.addOrReplaceChild("aura_rim_12", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-6, -10.3923f, 0.05f, 0, 0, -0.5236f));
        PartDefinition auraRim13 = aura.addOrReplaceChild("aura_rim_13", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-4.1042f, -11.2763f, 0.05f, 0, 0, -0.3491f));
        PartDefinition auraRim14 = aura.addOrReplaceChild("aura_rim_14", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-2.0838f, -11.8177f, 0.05f, 0, 0, -0.1745f));
        PartDefinition auraRim15 = aura.addOrReplaceChild("aura_rim_15", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(0, -12, 0.05f, 0, 0, 0));
        PartDefinition auraRim16 = aura.addOrReplaceChild("aura_rim_16", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(2.0838f, -11.8177f, 0.05f, 0, 0, 0.1745f));
        PartDefinition auraRim17 = aura.addOrReplaceChild("aura_rim_17", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(4.1042f, -11.2763f, 0.05f, 0, 0, 0.3491f));
        PartDefinition auraRim18 = aura.addOrReplaceChild("aura_rim_18", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(6, -10.3923f, 0.05f, 0, 0, 0.5236f));
        PartDefinition auraRim19 = aura.addOrReplaceChild("aura_rim_19", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(7.7135f, -9.1925f, 0.05f, 0, 0, 0.6981f));
        PartDefinition auraRim20 = aura.addOrReplaceChild("aura_rim_20", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(9.1925f, -7.7135f, 0.05f, 0, 0, 0.8727f));
        PartDefinition auraRim21 = aura.addOrReplaceChild("aura_rim_21", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(10.3923f, -6, 0.05f, 0, 0, 1.0472f));
        PartDefinition auraRim22 = aura.addOrReplaceChild("aura_rim_22", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(11.2763f, -4.1042f, 0.05f, 0, 0, 1.2217f));
        PartDefinition auraRim23 = aura.addOrReplaceChild("aura_rim_23", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(11.8177f, -2.0838f, 0.05f, 0, 0, 1.3963f));
        PartDefinition auraRim24 = aura.addOrReplaceChild("aura_rim_24", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(12, -0, 0.05f, 0, 0, 1.5708f));
        PartDefinition auraRim25 = aura.addOrReplaceChild("aura_rim_25", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(11.8177f, 2.0838f, 0.05f, 0, 0, 1.7453f));
        PartDefinition auraRim26 = aura.addOrReplaceChild("aura_rim_26", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(11.2763f, 4.1042f, 0.05f, 0, 0, 1.9199f));
        PartDefinition auraRim27 = aura.addOrReplaceChild("aura_rim_27", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(10.3923f, 6, 0.05f, 0, 0, 2.0944f));
        PartDefinition auraRim28 = aura.addOrReplaceChild("aura_rim_28", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(9.1925f, 7.7135f, 0.05f, 0, 0, 2.2689f));
        PartDefinition auraRim29 = aura.addOrReplaceChild("aura_rim_29", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(7.7135f, 9.1925f, 0.05f, 0, 0, 2.4435f));
        PartDefinition auraRim30 = aura.addOrReplaceChild("aura_rim_30", CubeListBuilder.create()
                .texOffs(65, 35).mirror(false).addBox(-1.1f, -0.5f, -0.2f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(6, 10.3923f, 0.05f, 0, 0, 2.618f));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }
}
