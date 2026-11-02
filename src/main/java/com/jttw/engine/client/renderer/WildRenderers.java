package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wild.GoblinEntity;
import com.jttw.engine.entity.wild.KoboldEntity;
import com.jttw.engine.entity.wild.OgreEntity;
import com.jttw.engine.entity.wild.WolfmanEntity;
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
import net.minecraft.world.entity.Mob;

/**
 * How the open world's yaoguai look: the goblin (a hunched, long-limbed mountain demon of soot-indigo
 * skin and a wild white mane, a great horn and a broken one, tusks and fangs in a gaping mouth, bone
 * ornaments, torn talismans worn as trophies and dark rags, a jagged iron chopper in its fist), the
 * kobold (a man-tall lizard warrior and miner with a crocodile's head and a thick tail, glossy olive
 * scales over a cream belly of scutes, a light leather harness and a scrap-iron pauldron, a pick in
 * its fist, a satchel of ore and a burning lantern at its belt), the wolfman (a tall, lean wolf
 * soldier in black lamellae laced in red, a long glaive in its hands) and the ogre (a towering, red,
 * pot-bellied mountain yaksha with horns and tusks, a tiger-skin loincloth, spiked iron bracers, a
 * necklace of skulls and a great
 * iron wolf-tooth club). Each one's model and skin come from its own generator in
 * {@code assets_source/} ({@code goblin.py}, {@code kobold.py}, {@code wolfman.py}, {@code ogre.py},
 * sharing {@code wild_paint.py}), the model written between its GENERATED markers here; its eyes
 * (and lights) glow full-bright ({@code <kind>_glow.png}).
 */
public final class WildRenderers {

    private WildRenderers() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(JTTWEngine.id(name), "main");
    }

    static ResourceLocation tex(String name) {
        return JTTWEngine.id("textures/entity/" + name + ".png");
    }

    public static final ModelLayerLocation GOBLIN_LAYER = layer("goblin"), KOBOLD_LAYER = layer("kobold"),
            WOLFMAN_LAYER = layer("wolfman"), OGRE_LAYER = layer("ogre");

    /** A part turned this much from its rest. */
    static void turn(ModelPart p, float x, float y, float z) {
        p.xRot = p.getInitialPose().xRot + x;
        p.yRot = p.getInitialPose().yRot + y;
        p.zRot = p.getInitialPose().zRot + z;
    }

    /** Names are shown only near, as the zones' creatures' are. */
    static boolean near(net.minecraft.client.renderer.entity.EntityRenderDispatcher d, Mob e) {
        return d.distanceToSqr(e) < 24 * 24;
    }

    // ── The goblin ──────────────────────────────────────────────────────

    public static LayerDefinition goblinLayer() {
        // GENERATED goblin: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(47, 0).mirror(false).addBox(-1.6f, -1, -1.7f, 3.2f, 7.6f, 3.4f)
                .texOffs(0, 32).mirror(false).addBox(-1.2f, 5.8f, -2.1f, 2.4f, 1.8f, 1), PartPose.offsetAndRotation(-2.4f, 7.0799f, 0.6f, -0.6f, 0, 0.1f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(47, 0).mirror(true).addBox(-1.6f, -1, -1.7f, 3.2f, 7.6f, 3.4f)
                .texOffs(0, 32).mirror(true).addBox(-1.2f, 5.8f, -2.1f, 2.4f, 1.8f, 1), PartPose.offsetAndRotation(2.4f, 7.0799f, 0.6f, -0.6f, 0, -0.1f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(140, 0).mirror(false).addBox(-1.15f, 0, -1.15f, 2.3f, 7.2f, 2.3f)
                .texOffs(127, 15).mirror(false).addBox(-1.45f, 1.2f, -1.45f, 2.9f, 3.6f, 2.9f), PartPose.offsetAndRotation(0, 6.9f, 0.1f, 1.25f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(140, 0).mirror(true).addBox(-1.15f, 0, -1.15f, 2.3f, 7.2f, 2.3f)
                .texOffs(127, 15).mirror(true).addBox(-1.45f, 1.2f, -1.45f, 2.9f, 3.6f, 2.9f), PartPose.offsetAndRotation(0, 6.9f, 0.1f, 1.25f, 0, 0));
        PartDefinition rightAnkle = rightShin.addOrReplaceChild("right_ankle", CubeListBuilder.create()
                .texOffs(63, 15).mirror(false).addBox(-0.9f, 0, -0.9f, 1.8f, 5.4f, 1.8f)
                .texOffs(179, 25).mirror(false).addBox(-1.1f, 0.4f, -1.1f, 2.2f, 0.8f, 2.2f), PartPose.offsetAndRotation(0, 6.9f, 0, -1.05f, 0, 0));
        PartDefinition leftAnkle = leftShin.addOrReplaceChild("left_ankle", CubeListBuilder.create()
                .texOffs(63, 15).mirror(true).addBox(-0.9f, 0, -0.9f, 1.8f, 5.4f, 1.8f)
                .texOffs(179, 25).mirror(true).addBox(-1.1f, 0.4f, -1.1f, 2.2f, 0.8f, 2.2f), PartPose.offsetAndRotation(0, 6.9f, 0, -1.05f, 0, 0));
        PartDefinition rightFoot = rightAnkle.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(219, 15).mirror(false).addBox(-1.5f, -0.5f, -2.8f, 3, 1.5f, 3.6f)
                .texOffs(9, 32).mirror(false).addBox(-1.55f, 0, -4.5f, 0.85f, 1, 1.9f)
                .texOffs(192, 25).mirror(false).addBox(-0.42f, 0, -4.8f, 0.85f, 1, 2.2f)
                .texOffs(9, 32).mirror(false).addBox(0.7f, 0, -4.5f, 0.85f, 1, 1.9f)
                .texOffs(16, 32).mirror(false).addBox(-0.4f, -0.3f, 0.6f, 0.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(0, 5, 0, 0.4f, 0, 0));
        PartDefinition leftFoot = leftAnkle.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(219, 15).mirror(true).addBox(-1.5f, -0.5f, -2.8f, 3, 1.5f, 3.6f)
                .texOffs(9, 32).mirror(true).addBox(0.7f, 0, -4.5f, 0.85f, 1, 1.9f)
                .texOffs(192, 25).mirror(true).addBox(-0.43f, 0, -4.8f, 0.85f, 1, 2.2f)
                .texOffs(9, 32).mirror(true).addBox(-1.55f, 0, -4.5f, 0.85f, 1, 1.9f)
                .texOffs(16, 32).mirror(true).addBox(-0.4f, -0.3f, 0.6f, 0.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(0, 5, 0, 0.4f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(213, 0).mirror(false).addBox(-3.2f, -2.6f, -2.2f, 6.4f, 3.4f, 4.4f)
                .texOffs(0, 15).mirror(false).addBox(-2.8f, -7.4f, -2, 5.6f, 5, 4)
                .texOffs(7, 0).mirror(false).addBox(-4.3f, -13.6f, -2.7f, 8.6f, 6.6f, 5.4f)
                .texOffs(72, 15).mirror(false).addBox(-3.7f, -15, -0.9f, 7.4f, 2.6f, 4.2f)
                .texOffs(234, 15).mirror(false).addBox(-1.5f, -15.6f, -2.2f, 3, 2.4f, 3)
                .texOffs(140, 15).mirror(false).addBox(-3.45f, -3.5f, -2.45f, 6.9f, 1.1f, 4.9f)
                .texOffs(72, 32).mirror(false).addBox(-3.7761f, -12.3081f, -3.1035f, 1, 1, 0.8f)
                .texOffs(23, 32).mirror(false).addBox(-3.2822f, -11.4372f, -3.0512f, 0.7f, 1.5f, 0.5f)
                .texOffs(72, 32).mirror(false).addBox(-2.8912f, -11.1374f, -3.1922f, 1, 1, 0.8f)
                .texOffs(23, 32).mirror(false).addBox(-2.0394f, -10.5834f, -3.1236f, 0.7f, 1.5f, 0.5f)
                .texOffs(72, 32).mirror(false).addBox(-1.3741f, -10.5709f, -3.2433f, 1, 1, 0.8f)
                .texOffs(23, 32).mirror(false).addBox(-0.35f, -10.3f, -3.15f, 0.7f, 1.5f, 0.5f)
                .texOffs(72, 32).mirror(false).addBox(0.3741f, -10.5709f, -3.2433f, 1, 1, 0.8f)
                .texOffs(23, 32).mirror(false).addBox(1.3394f, -10.5834f, -3.1236f, 0.7f, 1.5f, 0.5f)
                .texOffs(72, 32).mirror(false).addBox(1.8912f, -11.1374f, -3.1922f, 1, 1, 0.8f)
                .texOffs(23, 32).mirror(false).addBox(2.5822f, -11.4372f, -3.0512f, 0.7f, 1.5f, 0.5f)
                .texOffs(72, 32).mirror(false).addBox(2.7761f, -12.3081f, -3.1035f, 1, 1, 0.8f)
                .texOffs(201, 25).mirror(false).addBox(-1, -10.6f, -3.6f, 2, 2, 1.4f)
                .texOffs(77, 32).mirror(false).addBox(-0.45f, -12.5f, 2.75f, 0.9f, 0.9f, 0.7f)
                .texOffs(77, 32).mirror(false).addBox(-0.45f, -10.6f, 2.75f, 0.9f, 0.9f, 0.7f)
                .texOffs(77, 32).mirror(false).addBox(-0.45f, -8.7f, 2.5f, 0.9f, 0.9f, 0.7f)
                .texOffs(77, 32).mirror(false).addBox(-0.45f, -6.8f, 2.5f, 0.9f, 0.9f, 0.7f)
                .texOffs(77, 32).mirror(false).addBox(-0.45f, -5, 2.15f, 0.9f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 7.0799f, 0.6f, 0.78f, 0, 0));
        PartDefinition sash = body.addOrReplaceChild("sash", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.9f, 0, -0.2f, 1.8f, 13, 0.4f), PartPose.offsetAndRotation(3.4f, -13.4f, -2.95f, 0, 0, 0.6f));
        PartDefinition sashBack = body.addOrReplaceChild("sash_back", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.9f, 0, -0.2f, 1.8f, 13, 0.4f), PartPose.offsetAndRotation(3.4f, -13.4f, 2.95f, 0, 0, 0.6f));
        PartDefinition sashEnd = body.addOrReplaceChild("sash_end", CubeListBuilder.create()
                .texOffs(247, 15).mirror(false).addBox(-0.7f, 0, -0.2f, 1.4f, 5, 0.4f), PartPose.offsetAndRotation(-3.2f, -3.6f, -2.7f, -0.1f, 0, 0.15f));
        PartDefinition loinFront = body.addOrReplaceChild("loin_front", CubeListBuilder.create()
                .texOffs(21, 15).mirror(false).addBox(-2.2f, 0, -0.25f, 4.4f, 7.2f, 0.5f), PartPose.offsetAndRotation(0, -2.6f, -2.5f, -0.1f, 0, 0));
        PartDefinition loinBack = body.addOrReplaceChild("loin_back", CubeListBuilder.create()
                .texOffs(99, 15).mirror(false).addBox(-2.6f, 0, -0.25f, 5.2f, 6.4f, 0.5f), PartPose.offsetAndRotation(0, -2.6f, 2.3f, 0.12f, 0, 0));
        PartDefinition rightLoinSide = body.addOrReplaceChild("right_loin_side", CubeListBuilder.create()
                .texOffs(164, 0).mirror(false).addBox(-0.25f, 0, -1.8f, 0.5f, 5.4f, 3.6f), PartPose.offsetAndRotation(-3.2f, -2.6f, 0.1f, 0, 0, 0.1f));
        PartDefinition leftLoinSide = body.addOrReplaceChild("left_loin_side", CubeListBuilder.create()
                .texOffs(164, 0).mirror(true).addBox(-0.25f, 0, -1.8f, 0.5f, 5.4f, 3.6f), PartPose.offsetAndRotation(3.2f, -2.6f, 0.1f, 0, 0, -0.1f));
        PartDefinition rightTalisman = body.addOrReplaceChild("right_talisman", CubeListBuilder.create()
                .texOffs(0, 25).mirror(false).addBox(-0.9f, 0, -0.1f, 1.8f, 4.6f, 0.2f), PartPose.offsetAndRotation(-2, -3, -2.6f, -0.15f, 0.1f, 0.18f));
        PartDefinition leftTalisman = body.addOrReplaceChild("left_talisman", CubeListBuilder.create()
                .texOffs(0, 25).mirror(true).addBox(-0.9f, 0, -0.1f, 1.8f, 4.6f, 0.2f), PartPose.offsetAndRotation(2, -3, -2.6f, -0.15f, -0.1f, -0.18f));
        PartDefinition talismanBack = body.addOrReplaceChild("talisman_back", CubeListBuilder.create()
                .texOffs(43, 25).mirror(false).addBox(-0.9f, 0, -0.1f, 1.8f, 3.8f, 0.2f), PartPose.offsetAndRotation(1.8f, -3, 2.5f, 0.2f, 0, -0.1f));
        PartDefinition mane = body.addOrReplaceChild("mane", CubeListBuilder.create()
                .texOffs(64, 0).mirror(false).addBox(-4.2f, 0, -0.6f, 8.4f, 8.8f, 2.4f)
                .texOffs(7, 25).mirror(false).addBox(-3.2f, 7.2f, 0.2f, 6.4f, 3.4f, 1.4f), PartPose.offsetAndRotation(0, -15.2f, 1.6f, 0.25f, 0, 0));
        PartDefinition rightManeShoulder = body.addOrReplaceChild("right_mane_shoulder", CubeListBuilder.create()
                .texOffs(175, 0).mirror(false).addBox(-1.8f, 0, -1.8f, 2.6f, 5.2f, 3.6f), PartPose.offsetAndRotation(-3.4f, -15, -0.2f, 0, 0, 0.35f));
        PartDefinition leftManeShoulder = body.addOrReplaceChild("left_mane_shoulder", CubeListBuilder.create()
                .texOffs(175, 0).mirror(true).addBox(-0.8f, 0, -1.8f, 2.6f, 5.2f, 3.6f), PartPose.offsetAndRotation(3.4f, -15, -0.2f, 0, 0, -0.35f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(89, 0).mirror(false).addBox(-3, -6.6f, -3.6f, 6, 5.8f, 5.8f)
                .texOffs(50, 25).mirror(false).addBox(-3.3f, -3, -4.1f, 6.6f, 1.5f, 3)
                .texOffs(71, 25).mirror(false).addBox(-2.4f, -2.5f, -5.3f, 4.8f, 2, 2.4f)
                .texOffs(28, 32).mirror(false).addBox(-1, -3.7f, -4.9f, 2, 1.3f, 1)
                .texOffs(35, 32).mirror(false).addBox(-1.2f, -2.9f, -5.65f, 2.4f, 1.2f, 0.9f)
                .texOffs(34, 15).mirror(false).addBox(-3.4f, -7.4f, -3.2f, 6.8f, 1.6f, 6.4f)
                .texOffs(190, 0).mirror(false).addBox(-3.6f, -6.6f, 1.6f, 7.2f, 6.2f, 2.2f)
                .texOffs(88, 25).mirror(false).addBox(-1.9f, -1, -4.2f, 3.8f, 1.2f, 3), PartPose.offsetAndRotation(0, -15.2f, -1.2f, -0.8f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(165, 15).mirror(false).addBox(-2.3f, 0, -4.4f, 4.6f, 1.4f, 4.4f)
                .texOffs(103, 25).mirror(false).addBox(-1.6f, 1.2f, -4, 3.2f, 1, 3.4f), PartPose.offsetAndRotation(0, -0.6f, -1.4f, 0.36f, 0, 0));
        PartDefinition rightTusk = jaw.addOrReplaceChild("right_tusk", CubeListBuilder.create()
                .texOffs(210, 25).mirror(false).addBox(-0.45f, -2.6f, -0.45f, 0.9f, 2.8f, 0.9f), PartPose.offsetAndRotation(-1.85f, 0.2f, -3.95f, -0.25f, 0, -0.18f));
        PartDefinition leftTusk = jaw.addOrReplaceChild("left_tusk", CubeListBuilder.create()
                .texOffs(210, 25).mirror(true).addBox(-0.45f, -2.6f, -0.45f, 0.9f, 2.8f, 0.9f), PartPose.offsetAndRotation(1.85f, 0.2f, -3.95f, -0.25f, 0, 0.18f));
        PartDefinition rightFang = head.addOrReplaceChild("right_fang", CubeListBuilder.create()
                .texOffs(44, 32).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 1.5f, 0.6f), PartPose.offsetAndRotation(-1.55f, -0.6f, -4.95f, 0, 0, 0));
        PartDefinition leftFang = head.addOrReplaceChild("left_fang", CubeListBuilder.create()
                .texOffs(44, 32).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 1.5f, 0.6f), PartPose.offsetAndRotation(1.55f, -0.6f, -4.95f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(215, 25).mirror(false).addBox(-2.1f, -0.75f, -0.8f, 4, 1.5f, 1.9f), PartPose.offsetAndRotation(-1.55f, -4.55f, -3.75f, 0.1f, 0.12f, 0.32f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(215, 25).mirror(true).addBox(-1.9f, -0.75f, -0.8f, 4, 1.5f, 1.9f), PartPose.offsetAndRotation(1.55f, -4.55f, -3.75f, 0.1f, -0.12f, -0.32f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(82, 32).mirror(false).addBox(-1.1f, -0.5f, -0.3f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(-1.65f, -3.5f, -3.75f, 0, 0.18f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(82, 32).mirror(true).addBox(-1.1f, -0.5f, -0.3f, 2.2f, 1, 0.4f), PartPose.offsetAndRotation(1.65f, -3.5f, -3.75f, 0, -0.18f, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(186, 15).mirror(false).addBox(-0.3f, -1.3f, 0, 0.6f, 2.4f, 3.4f), PartPose.offsetAndRotation(-2.9f, -4.4f, -0.8f, 0.1f, -0.55f, -0.2f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(186, 15).mirror(true).addBox(-0.3f, -1.3f, 0, 0.6f, 2.4f, 3.4f), PartPose.offsetAndRotation(2.9f, -4.4f, -0.8f, 0.1f, 0.55f, 0.2f));
        PartDefinition rightLock = head.addOrReplaceChild("right_lock", CubeListBuilder.create()
                .texOffs(153, 0).mirror(false).addBox(-1, 0, -1.4f, 1.4f, 7.2f, 3), PartPose.offsetAndRotation(-3.1f, -5.6f, -1.6f, 0.1f, 0, 0.12f));
        PartDefinition leftLock = head.addOrReplaceChild("left_lock", CubeListBuilder.create()
                .texOffs(153, 0).mirror(true).addBox(-0.4f, 0, -1.4f, 1.4f, 7.2f, 3), PartPose.offsetAndRotation(3.1f, -5.6f, -1.6f, 0.1f, 0, -0.12f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(114, 0).mirror(false).addBox(-1.4f, -1, -1.4f, 2.8f, 8.6f, 2.8f)
                .texOffs(120, 25).mirror(false).addBox(-1.55f, 3.8f, -1.55f, 3.1f, 1, 3.1f), PartPose.offsetAndRotation(-4.9f, -12.6f, 0.2f, -0.92f, 0, 0.16f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(114, 0).mirror(true).addBox(-1.4f, -1, -1.4f, 2.8f, 8.6f, 2.8f)
                .texOffs(120, 25).mirror(true).addBox(-1.55f, 3.8f, -1.55f, 3.1f, 1, 3.1f), PartPose.offsetAndRotation(4.9f, -12.6f, 0.2f, -0.92f, 0, -0.16f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(127, 0).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 8.2f, 2.4f)
                .texOffs(114, 15).mirror(false).addBox(-1.45f, 3.4f, -1.45f, 2.9f, 4.2f, 2.9f), PartPose.offsetAndRotation(0, 7.8f, 0, -0.42f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(127, 0).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 8.2f, 2.4f)
                .texOffs(114, 15).mirror(true).addBox(-1.45f, 3.4f, -1.45f, 2.9f, 4.2f, 2.9f), PartPose.offsetAndRotation(0, 7.8f, 0, -0.42f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(197, 15).mirror(false).addBox(-1.5f, 0, -1.6f, 3, 2.4f, 3.2f)
                .texOffs(228, 25).mirror(false).addBox(-1.5f, 2.1f, -1.55f, 0.7f, 2.9f, 0.7f)
                .texOffs(137, 25).mirror(false).addBox(-0.62f, 2.1f, -1.55f, 0.7f, 3.2f, 0.7f)
                .texOffs(228, 25).mirror(false).addBox(0.26f, 2.1f, -1.55f, 0.7f, 3, 0.7f)
                .texOffs(233, 25).mirror(false).addBox(1, 2, -1.35f, 0.6f, 2.4f, 0.6f)
                .texOffs(49, 32).mirror(false).addBox(-1.75f, 0.8f, -2.1f, 0.75f, 2, 0.75f), PartPose.offsetAndRotation(0, 8, 0, 0.15f, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(197, 15).mirror(true).addBox(-1.5f, 0, -1.6f, 3, 2.4f, 3.2f)
                .texOffs(228, 25).mirror(true).addBox(0.8f, 2.1f, -1.55f, 0.7f, 2.9f, 0.7f)
                .texOffs(137, 25).mirror(true).addBox(-0.08f, 2.1f, -1.55f, 0.7f, 3.2f, 0.7f)
                .texOffs(228, 25).mirror(true).addBox(-0.96f, 2.1f, -1.55f, 0.7f, 3, 0.7f)
                .texOffs(233, 25).mirror(true).addBox(-1.6f, 2, -1.35f, 0.6f, 2.4f, 0.6f)
                .texOffs(49, 32).mirror(true).addBox(1, 0.8f, -2.1f, 0.75f, 2, 0.75f), PartPose.offsetAndRotation(0, 8, 0, 0.15f, 0, 0));
        PartDefinition horn = head.addOrReplaceChild("horn", CubeListBuilder.create()
                .texOffs(142, 25).mirror(false).addBox(-0.95f, -2.6f, -0.95f, 1.9f, 2.9f, 1.9f), PartPose.offsetAndRotation(-2, -6.3f, -2.4f, -0.35f, 0, -0.45f));
        PartDefinition hornMid = horn.addOrReplaceChild("horn_mid", CubeListBuilder.create()
                .texOffs(151, 25).mirror(false).addBox(-0.7f, -2.6f, -0.7f, 1.4f, 2.8f, 1.4f), PartPose.offsetAndRotation(0, -2.4f, 0, -0.55f, 0, 0.12f));
        PartDefinition hornTip = hornMid.addOrReplaceChild("horn_tip", CubeListBuilder.create()
                .texOffs(238, 25).mirror(false).addBox(-0.45f, -2.4f, -0.45f, 0.9f, 2.6f, 0.9f), PartPose.offsetAndRotation(0, -2.4f, 0, -0.75f, 0, 0.1f));
        PartDefinition hornBroken = head.addOrReplaceChild("horn_broken", CubeListBuilder.create()
                .texOffs(160, 25).mirror(false).addBox(-0.95f, -2.2f, -0.95f, 1.9f, 2.5f, 1.9f), PartPose.offsetAndRotation(2, -6.3f, -2.4f, -0.35f, 0, 0.45f));
        PartDefinition shoulderBone = leftArm.addOrReplaceChild("shoulder_bone", CubeListBuilder.create()
                .texOffs(26, 25).mirror(false).addBox(-1, -0.6f, -2.2f, 3, 1, 4.4f)
                .texOffs(54, 32).mirror(false).addBox(-0.6f, 0.3f, -2, 2.4f, 1.4f, 0.6f)
                .texOffs(54, 32).mirror(false).addBox(-0.6f, 0.3f, 1.4f, 2.4f, 1.4f, 0.6f), PartPose.offsetAndRotation(0.6f, -1.2f, 0, 0, 0, -0.25f));
        PartDefinition armTalisman = leftArm.addOrReplaceChild("arm_talisman", CubeListBuilder.create()
                .texOffs(212, 15).mirror(false).addBox(-0.1f, 0, -0.9f, 0.2f, 4.2f, 1.8f), PartPose.offsetAndRotation(1.42f, 2, 0, 0, 0, 0.05f));
        PartDefinition blade = rightHand.addOrReplaceChild("blade", CubeListBuilder.create()
                .texOffs(169, 25).mirror(false).addBox(-0.5f, -1.6f, -0.5f, 1, 3.6f, 1)
                .texOffs(63, 32).mirror(false).addBox(-0.6f, 1.8f, -0.6f, 1.2f, 0.9f, 1.2f)
                .texOffs(243, 25).mirror(false).addBox(-0.8f, -2.3f, -1.3f, 1.6f, 0.8f, 2.6f)
                .texOffs(38, 0).mirror(false).addBox(-0.25f, -12, -1.2f, 0.5f, 9.8f, 2.9f), PartPose.offsetAndRotation(0, 1.2f, -0.2f, 1.5555f, 0.1485f, -0.0599f));
        PartDefinition gripRag = blade.addOrReplaceChild("grip_rag", CubeListBuilder.create()
                .texOffs(174, 25).mirror(false).addBox(-0.4f, 0, -0.1f, 0.8f, 3.2f, 0.2f), PartPose.offsetAndRotation(0, 1.6f, 0.5f, 0.4f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED goblin: end
    }

    /**
     * The goblin's pose: hunched low on its bony legs, long arms hanging, the jagged blade held low
     * and forward; walking, a loping prowl, the arms swinging long; a blow, the blade hauled up over its
     * shoulder and hacked down while the left hand rakes; falling back, it drops into a crouch, arms
     * spread and claws up, its head thrust out and its jaws wide in a hiss, ears flat. Its jaw works as
     * it breathes, its mane and its rags stir.
     */
    public static final class GoblinModel extends EntityModel<GoblinEntity> {
        private final ModelPart root, body, head, jaw, rightEar, leftEar, rightArm, leftArm, rightForearm, leftForearm,
                rightLeg, leftLeg, rightShin, leftShin, mane, loinFront, loinBack, talisman, rightLock, leftLock;

        public GoblinModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
            rightLock = head.getChild("right_lock");
            leftLock = head.getChild("left_lock");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            mane = body.getChild("mane");
            loinFront = body.getChild("loin_front");
            loinBack = body.getChild("loin_back");
            talisman = body.getChild("right_talisman");
        }

        @Override
        public void setupAnim(GoblinEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = Mth.cos(swing * 0.75f) * 0.9f * amount;
            float bob = Math.abs(Mth.sin(swing * 0.75f)) * amount;
            float breath = Mth.sin(age * 0.1f);
            float wary = e.isFleeing() ? 1 : 0;
            // a blow: the blade hauled up over its shoulder, then hacked down
            float hack = 0;
            if (attackTime > 0) hack = attackTime < 0.4f ? -1.9f * attackTime / 0.4f : -1.9f + 2.6f * (attackTime - 0.4f) / 0.6f;
            float rake = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            // the legs prowl; falling back, they bend into a crouch
            turn(rightLeg, stride - wary * 0.75f, 0, wary * 0.15f);
            turn(leftLeg, -stride - wary * 0.75f, 0, -wary * 0.15f);
            turn(rightShin, Math.max(0, -stride) * 0.7f + wary * 0.85f, 0, 0);
            turn(leftShin, Math.max(0, stride) * 0.7f + wary * 0.85f, 0, 0);
            float drop = wary * 4.0f;
            rightLeg.y = rightLeg.getInitialPose().y + drop;
            leftLeg.y = leftLeg.getInitialPose().y + drop;
            // the body heaves as it breathes (fast, hissing, when crouched) and bobs as it lopes; it lunges into a blow
            turn(body, bob * 0.08f + breath * 0.03f + rake * 0.18f - wary * (0.05f + Mth.sin(age * 0.5f) * 0.03f), 0, stride * 0.06f);
            body.y = body.getInitialPose().y + bob * 0.7f + drop;
            // the head looks about, thrust out and up when it hisses; the jaw works, gapes in a blow, wide in a hiss
            turn(head, pitch * Mth.DEG_TO_RAD + wary * 0.05f, yaw * Mth.DEG_TO_RAD, Mth.sin(age * 0.05f) * 0.04f * (1 - wary));
            turn(jaw, breath * 0.06f + rake * 0.2f + wary * (0.3f + Mth.sin(age * 0.9f) * 0.05f), 0, 0);
            turn(rightEar, wary * 0.35f, wary * 0.4f, 0);
            turn(leftEar, wary * 0.35f, -wary * 0.4f, 0);
            float sway = Mth.sin(age * 0.07f) * 0.04f;
            turn(rightLock, -amount * 0.3f + sway, 0, 0);
            turn(leftLock, -amount * 0.3f - sway, 0, 0);
            // the long arms swing; a blow hacks the blade down and rakes the left claws; crouched, they spread
            // wide, claws out, the blade drawn back
            float twitch = Mth.sin(age * 0.6f) * 0.06f * wary;
            float rx = -stride * 0.75f + hack + wary * 0.1f;
            float lx = stride * 0.75f - rake * 1.2f + wary * 0.1f;
            turn(rightArm, rx, wary * 0.5f, wary * (0.7f + twitch) + Mth.cos(age * 0.08f) * 0.03f);
            turn(leftArm, lx, -wary * 0.5f, -wary * (0.7f - twitch) - rake * 0.2f - Mth.cos(age * 0.08f) * 0.03f);
            turn(rightForearm, (hack < 0 ? -hack * 0.35f : 0) - wary * 0.5f, 0, 0);
            turn(leftForearm, -rake * 0.6f - wary * 0.6f, 0, 0);
            // the mane, the loincloth's rags and the talisman stream as it moves
            turn(mane, amount * 0.35f + bob * 0.1f + breath * 0.02f, 0, 0);
            turn(loinFront, -Math.abs(stride) * 0.5f - wary * 0.3f, 0, 0);
            turn(loinBack, Math.abs(stride) * 0.5f + amount * 0.2f, 0, 0);
            turn(talisman, -amount * 0.4f + Mth.sin(age * 0.11f) * 0.06f, 0, stride * 0.15f);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class GoblinRenderer extends MobRenderer<GoblinEntity, GoblinModel> {
        /** How much larger than its pixels it is drawn: some 1.9 blocks to its crown, 2.0 to its horn's tip (goblin.py prints it). */
        public static final float SIZE = 0.86f;
        private static final ResourceLocation SKIN = tex("goblin");

        public GoblinRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new GoblinModel(ctx.bakeLayer(GOBLIN_LAYER)), 0.5f);
            addLayer(LionCourt.glow(this, "goblin"));
        }

        @Override
        protected void scale(GoblinEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        protected boolean shouldShowName(GoblinEntity e) {
            return super.shouldShowName(e) && near(entityRenderDispatcher, e);
        }

        @Override
        public ResourceLocation getTextureLocation(GoblinEntity e) {
            return SKIN;
        }
    }

    // ── The kobold ──────────────────────────────────────────────────────

    public static LayerDefinition koboldLayer() {
        // GENERATED kobold: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(41, 0).mirror(false).addBox(-1.9f, -1.2f, -2, 3.8f, 7, 4), PartPose.offsetAndRotation(-2.6f, 8.5185f, 1.2f, -0.55f, 0, 0.06f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(41, 0).mirror(true).addBox(-1.9f, -1.2f, -2, 3.8f, 7, 4), PartPose.offsetAndRotation(2.6f, 8.5185f, 1.2f, -0.55f, 0, -0.06f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(167, 0).mirror(false).addBox(-1.4f, 0, -1.4f, 2.8f, 6, 2.8f), PartPose.offsetAndRotation(0, 6.2f, 0.2f, 1.15f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(167, 0).mirror(true).addBox(-1.4f, 0, -1.4f, 2.8f, 6, 2.8f), PartPose.offsetAndRotation(0, 6.2f, 0.2f, 1.15f, 0, 0));
        PartDefinition rightAnkle = rightShin.addOrReplaceChild("right_ankle", CubeListBuilder.create()
                .texOffs(11, 15).mirror(false).addBox(-1.1f, 0, -1.1f, 2.2f, 4.8f, 2.2f), PartPose.offsetAndRotation(0, 5.8f, 0, -0.95f, 0, 0));
        PartDefinition leftAnkle = leftShin.addOrReplaceChild("left_ankle", CubeListBuilder.create()
                .texOffs(11, 15).mirror(true).addBox(-1.1f, 0, -1.1f, 2.2f, 4.8f, 2.2f), PartPose.offsetAndRotation(0, 5.8f, 0, -0.95f, 0, 0));
        PartDefinition rightFoot = rightAnkle.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(208, 15).mirror(false).addBox(-1.7f, -0.5f, -2.8f, 3.4f, 1.5f, 3.8f)
                .texOffs(141, 25).mirror(false).addBox(-1.75f, -0.1f, -4.6f, 1, 1.1f, 2)
                .texOffs(58, 25).mirror(false).addBox(-0.5f, -0.1f, -4.9f, 1, 1.1f, 2.3f)
                .texOffs(141, 25).mirror(false).addBox(0.75f, -0.1f, -4.6f, 1, 1.1f, 2)
                .texOffs(209, 25).mirror(false).addBox(-0.4f, -0.2f, 0.7f, 0.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(0, 4.5f, 0, 0.35f, 0, 0));
        PartDefinition leftFoot = leftAnkle.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(208, 15).mirror(true).addBox(-1.7f, -0.5f, -2.8f, 3.4f, 1.5f, 3.8f)
                .texOffs(141, 25).mirror(true).addBox(0.75f, -0.1f, -4.6f, 1, 1.1f, 2)
                .texOffs(58, 25).mirror(true).addBox(-0.5f, -0.1f, -4.9f, 1, 1.1f, 2.3f)
                .texOffs(141, 25).mirror(true).addBox(-1.75f, -0.1f, -4.6f, 1, 1.1f, 2)
                .texOffs(209, 25).mirror(true).addBox(-0.4f, -0.2f, 0.7f, 0.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(0, 4.5f, 0, 0.35f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(180, 0).mirror(false).addBox(-3.3f, -2.8f, -2.4f, 6.6f, 3.6f, 5)
                .texOffs(58, 0).mirror(false).addBox(-3.1f, -7.8f, -2.5f, 6.2f, 5.2f, 4.8f)
                .texOffs(12, 0).mirror(false).addBox(-3.9f, -13.8f, -2.8f, 7.8f, 6.2f, 5.6f)
                .texOffs(24, 15).mirror(false).addBox(-3.5f, -3.7f, -2.7f, 7, 1.2f, 5.4f)
                .texOffs(216, 25).mirror(false).addBox(-0.9f, -4, -2.95f, 1.8f, 1.6f, 0.4f)
                .texOffs(148, 25).mirror(false).addBox(-0.35f, -13, 2.6f, 0.7f, 2, 2)
                .texOffs(148, 25).mirror(false).addBox(-0.35f, -11.2f, 2.6f, 0.7f, 2, 2)
                .texOffs(148, 25).mirror(false).addBox(-0.35f, -9.4f, 2.6f, 0.7f, 2, 2)
                .texOffs(148, 25).mirror(false).addBox(-0.35f, -7.6f, 2.6f, 0.7f, 2, 2)
                .texOffs(148, 25).mirror(false).addBox(-0.35f, -5.8f, 2.6f, 0.7f, 2, 2)
                .texOffs(148, 25).mirror(false).addBox(-0.35f, -4, 2.6f, 0.7f, 2, 2), PartPose.offsetAndRotation(0, 8.5185f, 1.2f, 0.3f, 0, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(205, 0).mirror(false).addBox(-2, -3.6f, -2, 4, 4.2f, 3.8f)
                .texOffs(148, 25).mirror(false).addBox(-0.3f, -3.6f, 1.6f, 0.6f, 1.4f, 1.6f)
                .texOffs(148, 25).mirror(false).addBox(-0.3f, -1.8f, 1.6f, 0.6f, 1.4f, 1.6f), PartPose.offsetAndRotation(0, -13.2f, -0.6f, 0.05f, 0, 0));
        PartDefinition flap = body.addOrReplaceChild("flap", CubeListBuilder.create()
                .texOffs(225, 15).mirror(false).addBox(-1.8f, 0, -0.25f, 3.6f, 4.6f, 0.5f), PartPose.offsetAndRotation(0, -2.6f, -2.6f, -0.1f, 0, 0));
        PartDefinition strap = body.addOrReplaceChild("strap", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.6f, 0, -0.2f, 1.2f, 12.4f, 0.4f), PartPose.offsetAndRotation(-3, -13.6f, -2.95f, 0, 0, -0.62f));
        PartDefinition strapBack = body.addOrReplaceChild("strap_back", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.6f, 0, -0.2f, 1.2f, 12.4f, 0.4f), PartPose.offsetAndRotation(-3, -13.6f, 2.95f, 0, 0, -0.62f));
        PartDefinition satchel = body.addOrReplaceChild("satchel", CubeListBuilder.create()
                .texOffs(51, 15).mirror(false).addBox(-0.4f, 0, -2, 2.4f, 3.4f, 4)
                .texOffs(108, 15).mirror(false).addBox(-0.5f, -0.3f, -2.1f, 2.6f, 1.3f, 4.2f)
                .texOffs(223, 25).mirror(false).addBox(0, -1, -1.4f, 1.2f, 1, 1.2f)
                .texOffs(232, 25).mirror(false).addBox(0.3f, -1.4f, 0, 1, 1.4f, 1)
                .texOffs(14, 32).mirror(false).addBox(0.1f, -0.9f, 0.9f, 1.2f, 0.9f, 1), PartPose.offsetAndRotation(3.6f, -4, 0.2f, 0.05f, 0, -0.08f));
        PartDefinition lantern = body.addOrReplaceChild("lantern", CubeListBuilder.create()
                .texOffs(21, 32).mirror(false).addBox(-0.15f, 0, -0.15f, 0.3f, 1, 0.3f)
                .texOffs(155, 25).mirror(false).addBox(-1.1f, 0.9f, -1.1f, 2.2f, 0.5f, 2.2f)
                .texOffs(67, 25).mirror(false).addBox(-0.9f, 1.4f, -0.9f, 1.8f, 2.4f, 1.8f)
                .texOffs(168, 25).mirror(false).addBox(-0.55f, 1.75f, -0.55f, 1.1f, 1.7f, 1.1f)
                .texOffs(155, 25).mirror(false).addBox(-1.1f, 3.8f, -1.1f, 2.2f, 0.5f, 2.2f), PartPose.offsetAndRotation(-3.7f, -3, -1, -0.3f, 0, 0.05f));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(83, 0).mirror(false).addBox(-2.1f, -2, -0.6f, 4.2f, 4, 6.6f)
                .texOffs(148, 25).mirror(false).addBox(-0.3f, -3.4f, 0.6f, 0.6f, 1.4f, 1.6f)
                .texOffs(148, 25).mirror(false).addBox(-0.3f, -3.4f, 3.4f, 0.6f, 1.4f, 1.6f), PartPose.offsetAndRotation(0, -1.6f, 2.2f, -0.85f, 0, 0));
        PartDefinition tailMid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create()
                .texOffs(133, 0).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 6)
                .texOffs(177, 25).mirror(false).addBox(-0.25f, -2.8f, 0.8f, 0.5f, 1.2f, 1.4f)
                .texOffs(177, 25).mirror(false).addBox(-0.25f, -2.8f, 3.6f, 0.5f, 1.2f, 1.4f), PartPose.offsetAndRotation(0, 0.1f, 5.6f, -0.3f, 0, 0));
        PartDefinition tailEnd = tailMid.addOrReplaceChild("tail_end", CubeListBuilder.create()
                .texOffs(222, 0).mirror(false).addBox(-1.15f, -1.15f, 0, 2.3f, 2.3f, 5.4f)
                .texOffs(237, 25).mirror(false).addBox(-0.2f, -2.1f, 1.2f, 0.4f, 1, 1.2f), PartPose.offsetAndRotation(0, 0.2f, 5.6f, 0.2f, 0, 0));
        PartDefinition tailTip = tailEnd.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(125, 15).mirror(false).addBox(-0.7f, -0.7f, 0, 1.4f, 1.4f, 4.4f), PartPose.offsetAndRotation(0, 0.1f, 5, 0.22f, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(108, 0).mirror(false).addBox(-2.875f, -4.485f, -2.99f, 5.75f, 4.14f, 5.06f)
                .texOffs(76, 25).mirror(false).addBox(-1.84f, -5.2325f, -2.3f, 3.68f, 0.805f, 3.91f)
                .texOffs(236, 15).mirror(false).addBox(-2.1275f, -4.025f, 2.07f, 4.255f, 3.335f, 1.265f)
                .texOffs(140, 15).mirror(false).addBox(-2.5875f, -3.105f, -7.13f, 5.175f, 1.955f, 4.14f)
                .texOffs(93, 25).mirror(false).addBox(-1.8975f, -2.76f, -9.775f, 3.795f, 1.61f, 2.645f)
                .texOffs(184, 25).mirror(false).addBox(-1.265f, -2.415f, -11.73f, 2.53f, 1.265f, 1.955f)
                .texOffs(244, 25).mirror(false).addBox(-0.92f, -2.8175f, -11.5f, 1.84f, 0.46f, 1.15f)
                .texOffs(177, 25).mirror(false).addBox(-0.345f, -6.2675f, -1.61f, 0.69f, 1.38f, 1.61f)
                .texOffs(148, 25).mirror(false).addBox(-0.345f, -5.635f, 0.46f, 0.69f, 1.61f, 1.84f)
                .texOffs(26, 32).mirror(false).addBox(2.0125f, -1.15f, -3.933f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(-2.4955f, -1.15f, -3.933f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(2.0125f, -1.15f, -5.2555f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(-2.4955f, -1.15f, -5.2555f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(2.0125f, -1.15f, -6.578f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(-2.4955f, -1.15f, -6.578f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(1.3225f, -1.15f, -7.613f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(-1.8055f, -1.15f, -7.613f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(1.3225f, -1.15f, -8.9355f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(-1.8055f, -1.15f, -8.9355f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(0.69f, -1.15f, -10.258f, 0.483f, 0.8625f, 0.483f)
                .texOffs(26, 32).mirror(false).addBox(-1.173f, -1.15f, -10.258f, 0.483f, 0.8625f, 0.483f), PartPose.offsetAndRotation(0, -3, -0.6f, -0.2f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(66, 15).mirror(false).addBox(-2.0125f, 0, -4.6f, 4.025f, 1.265f, 5.29f)
                .texOffs(108, 25).mirror(false).addBox(-1.4375f, 0.0575f, -7.13f, 2.875f, 1.0925f, 2.645f)
                .texOffs(0, 32).mirror(false).addBox(-0.92f, 0.115f, -8.97f, 1.84f, 0.92f, 1.955f)
                .texOffs(163, 15).mirror(false).addBox(-1.725f, 1.15f, -4.37f, 3.45f, 1.265f, 4.6f)
                .texOffs(31, 32).mirror(false).addBox(1.3915f, -0.8625f, -1.8055f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(-1.8745f, -0.8625f, -1.8055f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(1.3915f, -0.8625f, -3.128f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(-1.8745f, -0.8625f, -3.128f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(1.3915f, -0.8625f, -4.4505f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(-1.8745f, -0.8625f, -4.4505f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(0.8165f, -0.8625f, -5.7155f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(-1.2995f, -0.8625f, -5.7155f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(0.8165f, -0.8625f, -7.038f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(-1.2995f, -0.8625f, -7.038f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(0.299f, -0.8625f, -8.2455f, 0.483f, 0.8625f, 0.483f)
                .texOffs(31, 32).mirror(false).addBox(-0.782f, -0.8625f, -8.2455f, 0.483f, 0.8625f, 0.483f), PartPose.offsetAndRotation(0, -1.035f, -2.76f, 0.05f, 0, 0));
        PartDefinition rightJowl = head.addOrReplaceChild("right_jowl", CubeListBuilder.create()
                .texOffs(182, 15).mirror(false).addBox(-0.805f, -1.035f, -2.07f, 1.035f, 2.185f, 3.91f), PartPose.offsetAndRotation(-2.8175f, -1.38f, -1.38f, 0, 0.18f, -0.1f));
        PartDefinition leftJowl = head.addOrReplaceChild("left_jowl", CubeListBuilder.create()
                .texOffs(182, 15).mirror(true).addBox(-0.23f, -1.035f, -2.07f, 1.035f, 2.185f, 3.91f), PartPose.offsetAndRotation(2.8175f, -1.38f, -1.38f, 0, -0.18f, 0.1f));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(0, 25).mirror(false).addBox(-1.0925f, -0.6325f, -1.84f, 1.84f, 1.035f, 3.22f), PartPose.offsetAndRotation(-2.5875f, -4.4275f, -2.3f, 0.1f, 0.3f, 0.3f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(0, 25).mirror(true).addBox(-0.7475f, -0.6325f, -1.84f, 1.84f, 1.035f, 3.22f), PartPose.offsetAndRotation(2.5875f, -4.4275f, -2.3f, 0.1f, -0.3f, -0.3f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(121, 25).mirror(false).addBox(-0.5175f, -0.7475f, -1.035f, 0.805f, 1.495f, 2.07f), PartPose.offsetAndRotation(-3.105f, -3.45f, -2.415f, 0, 0.4f, 0.1f));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(121, 25).mirror(true).addBox(-0.2875f, -0.7475f, -1.035f, 0.805f, 1.495f, 2.07f), PartPose.offsetAndRotation(3.105f, -3.45f, -2.415f, 0, -0.4f, -0.1f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(154, 0).mirror(false).addBox(-1.5f, -1.1f, -1.5f, 3, 6.8f, 3), PartPose.offsetAndRotation(-4.5f, -12.8f, 0.1f, -0.3f, 0, 0.12f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(154, 0).mirror(true).addBox(-1.5f, -1.1f, -1.5f, 3, 6.8f, 3), PartPose.offsetAndRotation(4.5f, -12.8f, 0.1f, -0.3f, 0, -0.12f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(241, 0).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 6, 2.6f)
                .texOffs(13, 25).mirror(false).addBox(-1.5f, 2.4f, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, 6, 0, -0.85f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(241, 0).mirror(true).addBox(-1.3f, 0, -1.3f, 2.6f, 6, 2.6f)
                .texOffs(13, 25).mirror(true).addBox(-1.5f, 2.4f, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, 6, 0, -0.85f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(26, 25).mirror(false).addBox(-1.45f, 0, -1.55f, 2.9f, 2, 3.1f)
                .texOffs(195, 25).mirror(false).addBox(-1.45f, 1.8f, -1.5f, 0.8f, 2.4f, 0.8f)
                .texOffs(195, 25).mirror(false).addBox(-0.4f, 1.8f, -1.5f, 0.8f, 2.6f, 0.8f)
                .texOffs(195, 25).mirror(false).addBox(0.65f, 1.8f, -1.5f, 0.8f, 2.4f, 0.8f)
                .texOffs(9, 32).mirror(false).addBox(-1.7f, 0.6f, -2.05f, 0.8f, 1.8f, 0.8f), PartPose.offsetAndRotation(0, 5.9f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(26, 25).mirror(true).addBox(-1.45f, 0, -1.55f, 2.9f, 2, 3.1f)
                .texOffs(195, 25).mirror(true).addBox(0.65f, 1.8f, -1.5f, 0.8f, 2.4f, 0.8f)
                .texOffs(195, 25).mirror(true).addBox(-0.4f, 1.8f, -1.5f, 0.8f, 2.6f, 0.8f)
                .texOffs(195, 25).mirror(true).addBox(-1.45f, 1.8f, -1.5f, 0.8f, 2.4f, 0.8f)
                .texOffs(9, 32).mirror(true).addBox(0.9f, 0.6f, -2.05f, 0.8f, 1.8f, 0.8f), PartPose.offsetAndRotation(0, 5.9f, 0, 0, 0, 0));
        PartDefinition pauldron = leftArm.addOrReplaceChild("pauldron", CubeListBuilder.create()
                .texOffs(89, 15).mirror(false).addBox(-1.4f, -1.6f, -2.1f, 3.6f, 2.4f, 4.2f)
                .texOffs(41, 25).mirror(false).addBox(-0.9f, 0.7f, -1.9f, 3.3f, 1.8f, 3.8f), PartPose.offsetAndRotation(0.3f, -0.6f, 0, 0, 0, -0.3f));
        PartDefinition thighPlate = rightLeg.addOrReplaceChild("thigh_plate", CubeListBuilder.create()
                .texOffs(0, 15).mirror(false).addBox(-0.4f, 0, -2, 0.6f, 4.2f, 4), PartPose.offsetAndRotation(-1.95f, 1, 0, 0, 0, 0.05f));
        PartDefinition pick = rightHand.addOrReplaceChild("pick", CubeListBuilder.create()
                .texOffs(7, 0).mirror(false).addBox(-0.45f, -11, -0.45f, 0.9f, 13, 0.9f)
                .texOffs(200, 25).mirror(false).addBox(-0.6f, -11.8f, -0.6f, 1.2f, 1.8f, 1.2f)
                .texOffs(195, 15).mirror(false).addBox(-0.5f, -11.5f, -5.4f, 1, 1.2f, 4.9f)
                .texOffs(130, 25).mirror(false).addBox(-0.6f, -11.6f, 0.5f, 1.2f, 1.4f, 2.8f), PartPose.offsetAndRotation(0, 1, -0.2f, 0.5973f, 0.1095f, -0.0492f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED kobold: end
    }

    /**
     * The kobold's pose: upright on its heavy lizard's legs, leaning a little forward, the pick held up
     * in its right fist, its tail laid out behind it to the ground; walking, a heavy stride, the body
     * rolling, the tail swinging side to side against it; a blow, the pick raised high and brought down,
     * the jaws gaping. Now and then its jaws part a moment; its lantern swings at its belt.
     */
    public static final class KoboldModel extends EntityModel<KoboldEntity> {
        private final ModelPart root, body, neck, head, jaw, rightArm, leftArm, rightForearm, leftForearm,
                rightLeg, leftLeg, rightShin, leftShin, tail, tailMid, tailEnd, tailTip, lantern, satchel;

        public KoboldModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            neck = body.getChild("neck");
            head = neck.getChild("head");
            jaw = head.getChild("jaw");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            tail = body.getChild("tail");
            tailMid = tail.getChild("tail_mid");
            tailEnd = tailMid.getChild("tail_end");
            tailTip = tailEnd.getChild("tail_tip");
            lantern = body.getChild("lantern");
            satchel = body.getChild("satchel");
        }

        @Override
        public void setupAnim(KoboldEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = Mth.cos(swing * 0.7f) * 0.85f * amount;
            float bob = Math.abs(Mth.sin(swing * 0.7f)) * amount;
            float breath = Mth.sin(age * 0.07f);
            // the pick raised high, then brought down
            float chop = 0;
            if (attackTime > 0) chop = attackTime < 0.4f ? -1.6f * attackTime / 0.4f : -1.6f + 2.5f * (attackTime - 0.4f) / 0.6f;
            float gape = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            turn(rightShin, Math.max(0, -stride) * 0.55f, 0, 0);
            turn(leftShin, Math.max(0, stride) * 0.55f, 0, 0);
            // the body rolls with its stride, and leans into a blow
            float lean = bob * 0.05f + breath * 0.015f + (chop > 0 ? chop * 0.12f : 0);
            turn(body, lean, stride * 0.08f, stride * 0.07f);
            body.y = body.getInitialPose().y + bob * 0.6f;
            turn(neck, -lean * 0.5f, 0, 0);
            turn(head, pitch * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, -stride * 0.05f);
            turn(jaw, gape * 0.5f + Math.max(0, Mth.sin(age * 0.04f) - 0.9f) * 2.5f, 0, 0);
            // the arms: the pick held up and swung; the left swings with its stride
            turn(rightArm, -stride * 0.3f + chop, 0, 0);
            turn(rightForearm, chop < 0 ? -chop * 0.3f : 0, 0, 0);
            turn(leftArm, stride * 0.6f - gape * 0.3f, 0, Mth.cos(age * 0.06f) * 0.03f);
            turn(leftForearm, -Math.abs(stride) * 0.2f, 0, 0);
            // the tail swings side to side against its stride, and twitches at rest
            float sway = Mth.sin(swing * 0.35f) * 0.3f * amount + Mth.sin(age * 0.045f) * 0.07f;
            turn(tail, -bob * 0.06f, sway, 0);
            turn(tailMid, 0, sway * 0.8f, 0);
            turn(tailEnd, 0, sway * 0.8f, 0);
            turn(tailTip, 0, sway * 0.7f + Mth.sin(age * 0.09f) * 0.12f, 0);
            // its gear swings at its belt
            turn(lantern, -lean + stride * 0.3f + Mth.sin(age * 0.07f) * 0.06f, 0, -stride * 0.15f);
            turn(satchel, 0, 0, stride * 0.12f);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class KoboldRenderer extends MobRenderer<KoboldEntity, KoboldModel> {
        /** How much larger than its pixels it is drawn: some 1.9 blocks to its crown (kobold.py prints it). */
        public static final float SIZE = 0.9f;
        private static final ResourceLocation SKIN = tex("kobold");

        public KoboldRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new KoboldModel(ctx.bakeLayer(KOBOLD_LAYER)), 0.5f);
            addLayer(LionCourt.glow(this, "kobold"));
        }

        @Override
        protected void scale(KoboldEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        protected boolean shouldShowName(KoboldEntity e) {
            return super.shouldShowName(e) && near(entityRenderDispatcher, e);
        }

        @Override
        public ResourceLocation getTextureLocation(KoboldEntity e) {
            return SKIN;
        }
    }

    // ── The wolfman ─────────────────────────────────────────────────────

    public static LayerDefinition wolfmanLayer() {
        // GENERATED wolfman: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(85, 0).mirror(false).addBox(-2, -1, -2.2f, 4, 8.6f, 4.4f), PartPose.offsetAndRotation(-2.6f, 4.7302f, 0.8f, -0.42f, 0, 0.05f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(85, 0).mirror(true).addBox(-2, -1, -2.2f, 4, 8.6f, 4.4f), PartPose.offsetAndRotation(2.6f, 4.7302f, 0.8f, -0.42f, 0, -0.05f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(133, 0).mirror(false).addBox(-1.55f, 0, -1.6f, 3.1f, 7.2f, 3.2f)
                .texOffs(142, 49).mirror(false).addBox(-1.85f, 0.8f, -2.1f, 3.7f, 4.6f, 1.6f), PartPose.offsetAndRotation(0, 7.4f, 0.3f, 1.05f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(133, 0).mirror(true).addBox(-1.55f, 0, -1.6f, 3.1f, 7.2f, 3.2f)
                .texOffs(142, 49).mirror(true).addBox(-1.85f, 0.8f, -2.1f, 3.7f, 4.6f, 1.6f), PartPose.offsetAndRotation(0, 7.4f, 0.3f, 1.05f, 0, 0));
        PartDefinition rightAnkle = rightShin.addOrReplaceChild("right_ankle", CubeListBuilder.create()
                .texOffs(36, 38).mirror(false).addBox(-1.15f, 0, -1.25f, 2.3f, 6, 2.5f)
                .texOffs(242, 49).mirror(false).addBox(-0.9f, 0.6f, 0.6f, 1.8f, 3.2f, 1.4f), PartPose.offsetAndRotation(0, 6.9f, 0.1f, -0.95f, 0, 0));
        PartDefinition leftAnkle = leftShin.addOrReplaceChild("left_ankle", CubeListBuilder.create()
                .texOffs(36, 38).mirror(true).addBox(-1.15f, 0, -1.25f, 2.3f, 6, 2.5f)
                .texOffs(242, 49).mirror(true).addBox(-0.9f, 0.6f, 0.6f, 1.8f, 3.2f, 1.4f), PartPose.offsetAndRotation(0, 6.9f, 0.1f, -0.95f, 0, 0));
        PartDefinition rightPaw = rightAnkle.addOrReplaceChild("right_paw", CubeListBuilder.create()
                .texOffs(155, 49).mirror(false).addBox(-1.75f, -0.5f, -3.4f, 3.5f, 1.9f, 4.4f)
                .texOffs(41, 59).mirror(false).addBox(-1.75f, 0.1f, -4.4f, 0.8f, 1.2f, 1.2f)
                .texOffs(41, 59).mirror(false).addBox(-0.8f, 0.1f, -4.7f, 0.8f, 1.2f, 1.4f)
                .texOffs(41, 59).mirror(false).addBox(0.05f, 0.1f, -4.7f, 0.8f, 1.2f, 1.4f)
                .texOffs(41, 59).mirror(false).addBox(0.95f, 0.1f, -4.4f, 0.8f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 5.7f, 0, 0.32f, 0, 0));
        PartDefinition leftPaw = leftAnkle.addOrReplaceChild("left_paw", CubeListBuilder.create()
                .texOffs(155, 49).mirror(true).addBox(-1.75f, -0.5f, -3.4f, 3.5f, 1.9f, 4.4f)
                .texOffs(41, 59).mirror(true).addBox(0.95f, 0.1f, -4.4f, 0.8f, 1.2f, 1.2f)
                .texOffs(41, 59).mirror(true).addBox(0, 0.1f, -4.7f, 0.8f, 1.2f, 1.4f)
                .texOffs(41, 59).mirror(true).addBox(-0.85f, 0.1f, -4.7f, 0.8f, 1.2f, 1.4f)
                .texOffs(41, 59).mirror(true).addBox(-1.75f, 0.1f, -4.4f, 0.8f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 5.7f, 0, 0.32f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(49, 38).mirror(false).addBox(-3.7f, -2.7f, -2.5f, 7.4f, 3.8f, 5)
                .texOffs(180, 0).mirror(false).addBox(-3.3f, -7.4f, -2.3f, 6.6f, 4.9f, 4.6f)
                .texOffs(50, 0).mirror(false).addBox(-5, -15.2f, -3.3f, 10, 8.4f, 6.6f)
                .texOffs(63, 49).mirror(false).addBox(-4.4f, -8, -2.9f, 8.8f, 1.8f, 5.8f)
                .texOffs(76, 38).mirror(false).addBox(-4.1f, -6.6f, -3, 8.2f, 2.1f, 6)
                .texOffs(48, 59).mirror(false).addBox(-1.7f, -7.1f, -3.55f, 3.4f, 3, 0.8f)
                .texOffs(205, 0).mirror(false).addBox(-4.5f, -17.6f, -2.7f, 9, 3.2f, 5.6f)
                .texOffs(107, 38).mirror(false).addBox(-2.6f, -17, 1.6f, 5.2f, 7, 2), PartPose.offsetAndRotation(0, 4.7302f, 0.8f, 0.1f, 0, 0));
        PartDefinition rightSashTail = body.addOrReplaceChild("right_sash_tail", CubeListBuilder.create()
                .texOffs(124, 38).mirror(false).addBox(-0.85f, 0, -0.25f, 1.7f, 8, 0.5f), PartPose.offsetAndRotation(-3.2f, -5.2f, -2.6f, -0.06f, 0.2f, 0.12f));
        PartDefinition leftSashTail = body.addOrReplaceChild("left_sash_tail", CubeListBuilder.create()
                .texOffs(124, 38).mirror(true).addBox(-0.85f, 0, -0.25f, 1.7f, 8, 0.5f), PartPose.offsetAndRotation(3.2f, -5.2f, -2.6f, -0.06f, -0.2f, -0.12f));
        PartDefinition sashTail2 = body.addOrReplaceChild("sash_tail_2", CubeListBuilder.create()
                .texOffs(94, 49).mirror(false).addBox(-0.7f, 0, -0.25f, 1.4f, 6.4f, 0.5f), PartPose.offsetAndRotation(-2.2f, -5, -2.9f, -0.1f, 0.1f, 0.02f));
        PartDefinition rightTasset = body.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(131, 38).mirror(false).addBox(-1.9f, 0, -0.45f, 3.8f, 7.6f, 0.9f), PartPose.offsetAndRotation(-1.9f, -2.4f, -2.9f, -0.1f, 0.06f, 0.05f));
        PartDefinition leftTasset = body.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(131, 38).mirror(true).addBox(-1.9f, 0, -0.45f, 3.8f, 7.6f, 0.9f), PartPose.offsetAndRotation(1.9f, -2.4f, -2.9f, -0.1f, -0.06f, -0.05f));
        PartDefinition rightTassetSide = body.addOrReplaceChild("right_tasset_side", CubeListBuilder.create()
                .texOffs(150, 0).mirror(false).addBox(-0.45f, 0, -2.4f, 0.9f, 7, 4.8f), PartPose.offsetAndRotation(-3.9f, -2.4f, 0, 0, 0, 0.12f));
        PartDefinition leftTassetSide = body.addOrReplaceChild("left_tasset_side", CubeListBuilder.create()
                .texOffs(150, 0).mirror(true).addBox(-0.45f, 0, -2.4f, 0.9f, 7, 4.8f), PartPose.offsetAndRotation(3.9f, -2.4f, 0, 0, 0, -0.12f));
        PartDefinition tassetBack = body.addOrReplaceChild("tasset_back", CubeListBuilder.create()
                .texOffs(142, 38).mirror(false).addBox(-3.6f, 0, -0.45f, 7.2f, 7.4f, 0.9f), PartPose.offsetAndRotation(0, -2.4f, 2.6f, 0.12f, 0, 0));
        PartDefinition cloak = body.addOrReplaceChild("cloak", CubeListBuilder.create()
                .texOffs(10, 0).mirror(false).addBox(-5.2f, 0, -0.3f, 10.4f, 15.5f, 0.6f), PartPose.offsetAndRotation(0, -15.6f, 3.4f, 0.14f, 0, 0));
        PartDefinition bannerPole = body.addOrReplaceChild("banner_pole", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-0.35f, -17, -0.35f, 0.7f, 18, 0.7f)
                .texOffs(59, 59).mirror(false).addBox(-0.6f, -18, -0.6f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(2.6f, -9, 3.9f, -0.12f, 0, 0.1f));
        PartDefinition banner = bannerPole.addOrReplaceChild("banner", CubeListBuilder.create()
                .texOffs(236, 0).mirror(false).addBox(0.3f, 0, -0.1f, 5.2f, 9, 0.2f), PartPose.offsetAndRotation(0, -16.6f, 0, 0, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(101, 49).mirror(false).addBox(-1.4f, -1.4f, 0, 2.8f, 2.8f, 4.8f), PartPose.offsetAndRotation(0, -2, 2.7f, -0.95f, 0, 0));
        PartDefinition tailMid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create()
                .texOffs(161, 38).mirror(false).addBox(-1.8f, -1.8f, 0, 3.6f, 3.6f, 5), PartPose.offsetAndRotation(0, 0, 4.4f, -0.3f, 0, 0));
        PartDefinition tailTip = tailMid.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(174, 49).mirror(false).addBox(-1.4f, -1.4f, 0, 2.8f, 2.8f, 3.6f), PartPose.offsetAndRotation(0, 0, 4.6f, 0.35f, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(104, 0).mirror(false).addBox(-3.2f, -6.2f, -3.4f, 6.4f, 5.8f, 6.2f)
                .texOffs(0, 38).mirror(false).addBox(-1.75f, -3.9f, -9.9f, 3.5f, 2.7f, 6.8f)
                .texOffs(189, 49).mirror(false).addBox(-1.35f, -4.6f, -9.2f, 2.7f, 0.9f, 6)
                .texOffs(86, 59).mirror(false).addBox(-0.95f, -4.5f, -10.4f, 1.9f, 1.4f, 0.8f)
                .texOffs(180, 38).mirror(false).addBox(-3.4f, -6.3f, 1, 6.8f, 6.2f, 2), PartPose.offsetAndRotation(0, -17.6f, -1, -0.1f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(199, 38).mirror(false).addBox(-1.6f, 0, -6.6f, 3.2f, 1.4f, 6.6f)
                .texOffs(208, 49).mirror(false).addBox(-1.3f, 1.2f, -4, 2.6f, 1.6f, 4.4f), PartPose.offsetAndRotation(0, -1.15f, -3.1f, 0.03f, 0, 0));
        PartDefinition rightFang = head.addOrReplaceChild("right_fang", CubeListBuilder.create()
                .texOffs(93, 59).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 1.5f, 0.6f), PartPose.offsetAndRotation(-1.15f, -1.2f, -8.6f, 0, 0, 0));
        PartDefinition leftFang = head.addOrReplaceChild("left_fang", CubeListBuilder.create()
                .texOffs(93, 59).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 1.5f, 0.6f), PartPose.offsetAndRotation(1.15f, -1.2f, -8.6f, 0, 0, 0));
        PartDefinition rightCheek = head.addOrReplaceChild("right_cheek", CubeListBuilder.create()
                .texOffs(118, 49).mirror(false).addBox(-1.8f, -1.6f, -1, 2, 3.2f, 3.4f), PartPose.offsetAndRotation(-2.9f, -2.6f, -1.4f, 0, 0.55f, 0.3f));
        PartDefinition leftCheek = head.addOrReplaceChild("left_cheek", CubeListBuilder.create()
                .texOffs(118, 49).mirror(true).addBox(-0.2f, -1.6f, -1, 2, 3.2f, 3.4f), PartPose.offsetAndRotation(2.9f, -2.6f, -1.4f, 0, -0.55f, -0.3f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(98, 59).mirror(false).addBox(-1.05f, -0.5f, -0.3f, 2.1f, 1.05f, 0.45f), PartPose.offsetAndRotation(-1.9f, -4.55f, -3.5f, 0, 0.32f, -0.12f));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(98, 59).mirror(true).addBox(-1.05f, -0.5f, -0.3f, 2.1f, 1.05f, 0.45f), PartPose.offsetAndRotation(1.9f, -4.55f, -3.5f, 0, -0.32f, 0.12f));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(68, 59).mirror(false).addBox(-1.5f, -0.55f, -0.8f, 3, 1.1f, 1.6f), PartPose.offsetAndRotation(-1.75f, -5.15f, -3.7f, 0.1f, 0.25f, 0.22f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(68, 59).mirror(true).addBox(-1.5f, -0.55f, -0.8f, 3, 1.1f, 1.6f), PartPose.offsetAndRotation(1.75f, -5.15f, -3.7f, 0.1f, -0.25f, -0.22f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(131, 49).mirror(false).addBox(-1.4f, -5, -0.65f, 2.8f, 5.2f, 1.3f), PartPose.offsetAndRotation(-2.1f, -5.9f, -0.4f, -0.2f, 0.15f, -0.22f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(131, 49).mirror(true).addBox(-1.4f, -5, -0.65f, 2.8f, 5.2f, 1.3f), PartPose.offsetAndRotation(2.1f, -5.9f, -0.4f, -0.2f, -0.15f, 0.22f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(163, 0).mirror(false).addBox(-1.8f, -1, -1.8f, 3.6f, 7.2f, 3.6f)
                .texOffs(222, 38).mirror(false).addBox(-3.1f, -2.4f, -2.7f, 4.6f, 2.4f, 5.4f)
                .texOffs(0, 49).mirror(false).addBox(-3.5f, -0.4f, -2.9f, 4.6f, 2.6f, 5.8f)
                .texOffs(23, 49).mirror(false).addBox(-3.9f, 1.8f, -3, 4.6f, 2.4f, 6)
                .texOffs(0, 59).mirror(false).addBox(-4.3f, -0.4f, -1.3f, 0.8f, 2.8f, 2.6f), PartPose.offsetAndRotation(-5.7f, -13.8f, 0, -0.18f, 0, 0.14f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(163, 0).mirror(true).addBox(-1.8f, -1, -1.8f, 3.6f, 7.2f, 3.6f)
                .texOffs(222, 38).mirror(true).addBox(-1.5f, -2.4f, -2.7f, 4.6f, 2.4f, 5.4f)
                .texOffs(0, 49).mirror(true).addBox(-1.1f, -0.4f, -2.9f, 4.6f, 2.6f, 5.8f)
                .texOffs(23, 49).mirror(true).addBox(-0.7f, 1.8f, -3, 4.6f, 2.4f, 6)
                .texOffs(0, 59).mirror(true).addBox(3.5f, -0.4f, -1.3f, 0.8f, 2.8f, 2.6f), PartPose.offsetAndRotation(5.7f, -13.8f, 0, -0.18f, 0, -0.14f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(23, 38).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 6.6f, 3)
                .texOffs(46, 49).mirror(false).addBox(-1.85f, 1.4f, -1.85f, 3.7f, 4.4f, 3.7f), PartPose.offsetAndRotation(-0.2f, 6.6f, 0, -0.62f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(23, 38).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 6.6f, 3)
                .texOffs(46, 49).mirror(true).addBox(-1.85f, 1.4f, -1.85f, 3.7f, 4.4f, 3.7f), PartPose.offsetAndRotation(0.2f, 6.6f, 0, -0.62f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(225, 49).mirror(false).addBox(-1.55f, 0, -1.65f, 3.1f, 2.4f, 3.3f)
                .texOffs(107, 59).mirror(false).addBox(-1.5f, 2.1f, -1.6f, 0.7f, 1.6f, 0.7f)
                .texOffs(107, 59).mirror(false).addBox(-0.6f, 2.1f, -1.6f, 0.7f, 1.8f, 0.7f)
                .texOffs(107, 59).mirror(false).addBox(0.3f, 2.1f, -1.6f, 0.7f, 1.7f, 0.7f)
                .texOffs(107, 59).mirror(false).addBox(-1.7f, 0.6f, -2.2f, 0.8f, 1.5f, 0.8f), PartPose.offsetAndRotation(0, 6.5f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(225, 49).mirror(true).addBox(-1.55f, 0, -1.65f, 3.1f, 2.4f, 3.3f)
                .texOffs(107, 59).mirror(true).addBox(0.8f, 2.1f, -1.6f, 0.7f, 1.6f, 0.7f)
                .texOffs(107, 59).mirror(true).addBox(-0.1f, 2.1f, -1.6f, 0.7f, 1.8f, 0.7f)
                .texOffs(107, 59).mirror(true).addBox(-1, 2.1f, -1.6f, 0.7f, 1.7f, 0.7f)
                .texOffs(107, 59).mirror(true).addBox(0.9f, 0.6f, -2.2f, 0.8f, 1.5f, 0.8f), PartPose.offsetAndRotation(0, 6.5f, 0, 0, 0, 0));
        PartDefinition earring = leftEar.addOrReplaceChild("earring", CubeListBuilder.create()
                .texOffs(79, 59).mirror(false).addBox(-0.25f, 0, -0.75f, 0.5f, 1.6f, 1.5f), PartPose.offsetAndRotation(1, -2, 0, 0, 0, 0));
        PartDefinition glaive = rightHand.addOrReplaceChild("glaive", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5f, -26, -0.5f, 1, 36, 1)
                .texOffs(112, 59).mirror(false).addBox(-0.65f, -12, -0.65f, 1.3f, 1, 1.3f)
                .texOffs(112, 59).mirror(false).addBox(-0.65f, -2, -0.65f, 1.3f, 1, 1.3f)
                .texOffs(112, 59).mirror(false).addBox(-0.65f, 6, -0.65f, 1.3f, 1, 1.3f)
                .texOffs(20, 59).mirror(false).addBox(-0.6f, 10, -0.6f, 1.2f, 2.4f, 1.2f)
                .texOffs(9, 59).mirror(false).addBox(-0.9f, -28.4f, -1.2f, 1.8f, 2.6f, 2.4f)
                .texOffs(121, 59).mirror(false).addBox(-0.3f, -29.2f, -0.3f, 0.6f, 0.9f, 0.6f)
                .texOffs(35, 0).mirror(false).addBox(-0.4f, -39, -5.4f, 0.8f, 11, 5.4f)
                .texOffs(29, 59).mirror(false).addBox(-0.3f, -32, 0.9f, 0.6f, 2.4f, 1.8f), PartPose.offsetAndRotation(0, 1.2f, 0, 0.9951f, 0.6712f, 0.4425f));
        PartDefinition glaiveTassel = glaive.addOrReplaceChild("glaive_tassel", CubeListBuilder.create()
                .texOffs(36, 59).mirror(false).addBox(-0.45f, 0, -0.45f, 0.9f, 4, 0.9f), PartPose.offsetAndRotation(0, -27.4f, 1.1f, 0.2f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED wolfman: end
    }

    /**
     * The wolfman's pose: upright on its wolf's legs, the glaive grounded at its right side; walking, a
     * long loping stride, the cloak and the tail swinging behind, the banner fluttering; a blow, the
     * glaive heaved up and brought down; howling, it throws its head back, jaws wide, ears flat.
     */
    public static final class WolfmanModel extends EntityModel<WolfmanEntity> {
        private final ModelPart root, body, head, jaw, rightEar, leftEar, rightArm, leftArm, rightForearm, leftForearm,
                rightLeg, leftLeg, rightShin, leftShin, tail, tailMid, tailTip, cloak, banner, sashTail;

        public WolfmanModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            tail = body.getChild("tail");
            tailMid = tail.getChild("tail_mid");
            tailTip = tailMid.getChild("tail_tip");
            cloak = body.getChild("cloak");
            banner = body.getChild("banner_pole").getChild("banner");
            sashTail = body.getChild("right_sash_tail");
        }

        @Override
        public void setupAnim(WolfmanEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = Mth.cos(swing * 0.6662f) * 0.95f * amount;
            float bob = Math.abs(Mth.sin(swing * 0.6662f)) * amount;
            float breath = Mth.sin(age * 0.07f);
            float howl = e.howling() > 0 ? Mth.clamp(Math.min(e.howling(), WolfmanEntity.HOWL_TICKS - e.howling() + 4) / 6f, 0, 1) : 0;
            // the glaive heaved up over its shoulder, then brought down
            float chop = 0;
            if (attackTime > 0) chop = attackTime < 0.35f ? -2.1f * attackTime / 0.35f : -2.1f + 2.7f * (attackTime - 0.35f) / 0.65f;
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            turn(rightShin, Math.max(0, -stride) * 0.6f, 0, 0);
            turn(leftShin, Math.max(0, stride) * 0.6f, 0, 0);
            turn(body, bob * 0.05f + breath * 0.015f - howl * 0.22f + (chop > 0 ? chop * 0.12f : 0), 0, 0);
            body.y = body.getInitialPose().y + bob * 0.8f;
            // the head looks about; howling, it is thrown back, the jaws wide
            turn(head, pitch * Mth.DEG_TO_RAD * (1 - howl) - howl * 0.95f, yaw * Mth.DEG_TO_RAD * (1 - howl), 0);
            turn(jaw, howl * 0.6f + (attackTime > 0 ? 0.35f : Math.max(0, Mth.sin(age * 0.05f)) * 0.05f), 0, 0);
            float twitch = Mth.sin(age * 0.29f) * Mth.sin(age * 0.05f) * 0.1f;
            turn(rightEar, howl * 0.5f, 0, twitch);
            turn(leftEar, howl * 0.5f, 0, -twitch);
            // the arms: the glaive held steady, swung in a blow; the left swings, flung out when it howls
            turn(rightArm, -stride * 0.2f + chop - howl * 0.3f, 0, howl * 0.2f);
            turn(rightForearm, chop < 0 ? -chop * 0.25f : 0, 0, 0);
            turn(leftArm, stride * 0.6f - howl * 0.5f, 0, -howl * 0.5f);
            turn(leftForearm, -howl * 0.3f, 0, 0);
            // the tail swings and lifts, the cloak streams behind, the banner flutters
            float sway = Mth.sin(swing * 0.333f) * 0.3f * amount + Mth.sin(age * 0.06f) * 0.08f;
            turn(tail, bob * 0.15f + amount * 0.45f, sway, 0);
            turn(tailMid, amount * 0.15f, sway * 0.7f, 0);
            turn(tailTip, 0, sway * 0.7f, 0);
            turn(cloak, amount * 0.55f + bob * 0.1f + Mth.sin(age * 0.08f) * 0.03f, 0, 0);
            turn(banner, 0, Mth.sin(age * 0.15f) * 0.25f + amount * 0.3f, 0);
            turn(sashTail, -Math.abs(stride) * 0.3f, 0, Mth.sin(age * 0.1f) * 0.05f);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class WolfmanRenderer extends MobRenderer<WolfmanEntity, WolfmanModel> {
        /** How much larger than its pixels it is drawn: some 2.4 blocks to its crown, 2.7 to its ears' tips (wolfman.py prints it). */
        public static final float SIZE = 0.9f;
        private static final ResourceLocation SKIN = tex("wolfman");

        public WolfmanRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new WolfmanModel(ctx.bakeLayer(WOLFMAN_LAYER)), 0.6f);
            addLayer(LionCourt.glow(this, "wolfman"));
        }

        @Override
        protected void scale(WolfmanEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        protected boolean shouldShowName(WolfmanEntity e) {
            return super.shouldShowName(e) && near(entityRenderDispatcher, e);
        }

        @Override
        public ResourceLocation getTextureLocation(WolfmanEntity e) {
            return SKIN;
        }
    }

    // ── The ogre ────────────────────────────────────────────────────────

    public static LayerDefinition ogreLayer() {
        // GENERATED ogre: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(174, 0).mirror(false).addBox(-3.3f, -1.6f, -3.4f, 6.6f, 9.2f, 6.8f), PartPose.offsetAndRotation(-4.4f, 6.0763f, 0.6f, -0.12f, 0, 0.1f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(174, 0).mirror(true).addBox(-3.3f, -1.6f, -3.4f, 6.6f, 9.2f, 6.8f), PartPose.offsetAndRotation(4.4f, 6.0763f, 0.6f, -0.12f, 0, -0.1f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(115, 26).mirror(false).addBox(-2.8f, 0, -2.8f, 5.6f, 7.8f, 5.6f)
                .texOffs(129, 56).mirror(false).addBox(-3.2f, 5, -3.2f, 6.4f, 1.8f, 6.4f), PartPose.offsetAndRotation(0, 8.2f, 0.3f, 0.26f, 0, -0.08f));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(115, 26).mirror(true).addBox(-2.8f, 0, -2.8f, 5.6f, 7.8f, 5.6f)
                .texOffs(129, 56).mirror(true).addBox(-3.2f, 5, -3.2f, 6.4f, 1.8f, 6.4f), PartPose.offsetAndRotation(0, 8.2f, 0.3f, 0.26f, 0, 0.08f));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(199, 43).mirror(false).addBox(-3, -0.3f, -5, 6, 2.5f, 7.4f)
                .texOffs(0, 77).mirror(false).addBox(-2.95f, 0.5f, -6.3f, 1.4f, 1.7f, 1.6f)
                .texOffs(0, 77).mirror(false).addBox(-1.45f, 0.5f, -6.6f, 1.4f, 1.7f, 1.8f)
                .texOffs(0, 77).mirror(false).addBox(0.05f, 0.5f, -6.5f, 1.4f, 1.7f, 1.7f)
                .texOffs(0, 77).mirror(false).addBox(1.5f, 0.6f, -6.1f, 1.4f, 1.6f, 1.4f), PartPose.offsetAndRotation(0, 7.6f, 0, -0.14f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(199, 43).mirror(true).addBox(-3, -0.3f, -5, 6, 2.5f, 7.4f)
                .texOffs(0, 77).mirror(true).addBox(1.55f, 0.5f, -6.3f, 1.4f, 1.7f, 1.6f)
                .texOffs(0, 77).mirror(true).addBox(0.05f, 0.5f, -6.6f, 1.4f, 1.7f, 1.8f)
                .texOffs(0, 77).mirror(true).addBox(-1.45f, 0.5f, -6.5f, 1.4f, 1.7f, 1.7f)
                .texOffs(0, 77).mirror(true).addBox(-2.9f, 0.6f, -6.1f, 1.4f, 1.6f, 1.4f), PartPose.offsetAndRotation(0, 7.6f, 0, -0.14f, 0, -0));
        PartDefinition rightChain = rightShin.addOrReplaceChild("right_chain", CubeListBuilder.create()
                .texOffs(9, 77).mirror(false).addBox(-0.3f, 0, -0.7f, 0.6f, 1.8f, 1.4f)
                .texOffs(94, 77).mirror(false).addBox(-0.7f, 1.5f, -0.3f, 1.4f, 1.8f, 0.6f), PartPose.offsetAndRotation(2.6f, 6.2f, -1.6f, 0.3f, 0, 0.5f));
        PartDefinition leftChain = leftShin.addOrReplaceChild("left_chain", CubeListBuilder.create()
                .texOffs(9, 77).mirror(true).addBox(-0.3f, 0, -0.7f, 0.6f, 1.8f, 1.4f)
                .texOffs(94, 77).mirror(true).addBox(-0.7f, 1.5f, -0.3f, 1.4f, 1.8f, 0.6f), PartPose.offsetAndRotation(-2.6f, 6.2f, -1.6f, 0.3f, 0, -0.5f));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(140, 26).mirror(false).addBox(-6.3f, -3.6f, -4.2f, 12.6f, 5, 8.6f)
                .texOffs(25, 0).mirror(false).addBox(-7.4f, -11.6f, -5.6f, 14.8f, 8.8f, 10.8f)
                .texOffs(0, 56).mirror(false).addBox(-5.8f, -10.8f, -7.6f, 11.6f, 7.2f, 2.2f)
                .texOffs(77, 68).mirror(false).addBox(-3.8f, -9.6f, -8.6f, 7.6f, 4.6f, 1)
                .texOffs(78, 0).mirror(false).addBox(-8.6f, -20.6f, -4.6f, 17.2f, 9.8f, 9.4f)
                .texOffs(203, 56).mirror(false).addBox(-7.8f, -20.2f, -5.7f, 7.4f, 5.4f, 1.2f)
                .texOffs(203, 56).mirror(false).addBox(0.4f, -20.2f, -5.7f, 7.4f, 5.4f, 1.2f)
                .texOffs(0, 43).mirror(false).addBox(-6.8f, -23.4f, -2.6f, 13.6f, 3.4f, 7.2f)
                .texOffs(75, 56).mirror(false).addBox(-5.6f, -21.6f, 3.6f, 11.2f, 7, 2.2f)
                .texOffs(45, 43).mirror(false).addBox(-7, -4.8f, -4.7f, 14, 1.9f, 9.8f)
                .texOffs(152, 68).mirror(false).addBox(-1.4f, -5.4f, -5.4f, 2.8f, 2.8f, 1.2f)
                .texOffs(185, 26).mirror(false).addBox(-4.6f, -22.4f, 4.4f, 9.2f, 11, 1.6f)
                .texOffs(163, 68).mirror(false).addBox(-7.4595f, -20.931f, -6.507f, 2.2f, 2.4f, 1.8f)
                .texOffs(16, 77).mirror(false).addBox(-6.5813f, -19.9137f, -6.3874f, 1.5f, 1.5f, 1.5f)
                .texOffs(16, 77).mirror(false).addBox(-5.7804f, -19.1098f, -6.4589f, 1.5f, 1.5f, 1.5f)
                .texOffs(163, 68).mirror(false).addBox(-5.0942f, -18.5834f, -6.7184f, 2.2f, 2.4f, 1.8f)
                .texOffs(16, 77).mirror(false).addBox(-3.5213f, -17.3602f, -6.563f, 1.5f, 1.5f, 1.5f)
                .texOffs(16, 77).mirror(false).addBox(-2.1688f, -16.5717f, -6.5906f, 1.5f, 1.5f, 1.5f)
                .texOffs(163, 68).mirror(false).addBox(-1.1f, -16.3f, -6.8f, 2.2f, 2.4f, 1.8f)
                .texOffs(16, 77).mirror(false).addBox(0.6688f, -16.5717f, -6.5906f, 1.5f, 1.5f, 1.5f)
                .texOffs(16, 77).mirror(false).addBox(2.0213f, -17.3602f, -6.563f, 1.5f, 1.5f, 1.5f)
                .texOffs(163, 68).mirror(false).addBox(2.8942f, -18.5834f, -6.7184f, 2.2f, 2.4f, 1.8f)
                .texOffs(16, 77).mirror(false).addBox(4.2804f, -19.1098f, -6.4589f, 1.5f, 1.5f, 1.5f)
                .texOffs(16, 77).mirror(false).addBox(5.0813f, -19.9137f, -6.3874f, 1.5f, 1.5f, 1.5f)
                .texOffs(163, 68).mirror(false).addBox(5.2595f, -20.931f, -6.507f, 2.2f, 2.4f, 1.8f), PartPose.offsetAndRotation(0, 6.0763f, 0.6f, 0.14f, 0, 0));
        PartDefinition loinFront = body.addOrReplaceChild("loin_front", CubeListBuilder.create()
                .texOffs(31, 56).mirror(false).addBox(-3.6f, 0, -0.45f, 7.2f, 9.4f, 0.9f), PartPose.offsetAndRotation(0, -3.6f, -4.6f, -0.16f, 0, 0));
        PartDefinition tigerHead = body.addOrReplaceChild("tiger_head", CubeListBuilder.create()
                .texOffs(13, 68).mirror(false).addBox(-2.3f, -1.4f, -2.6f, 4.6f, 3.6f, 2.8f)
                .texOffs(101, 77).mirror(false).addBox(-1.3f, 0.8f, -3.4f, 2.6f, 1.6f, 1)
                .texOffs(110, 77).mirror(false).addBox(-2.3f, -2.4f, -1.4f, 1.2f, 1.2f, 0.6f)
                .texOffs(110, 77).mirror(false).addBox(1.1f, -2.4f, -1.4f, 1.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(0, -4.6f, -5, -0.1f, 0, 0));
        PartDefinition loinBack = body.addOrReplaceChild("loin_back", CubeListBuilder.create()
                .texOffs(106, 56).mirror(false).addBox(-4.8f, 0, -0.45f, 9.6f, 8.6f, 0.9f), PartPose.offsetAndRotation(0, -3.6f, 4.2f, 0.2f, 0, 0));
        PartDefinition rightLoinSide = body.addOrReplaceChild("right_loin_side", CubeListBuilder.create()
                .texOffs(31, 26).mirror(false).addBox(-0.45f, 0, -3.4f, 0.9f, 7.4f, 6.8f), PartPose.offsetAndRotation(-6.2f, -3.4f, 0, 0, 0, 0.16f));
        PartDefinition leftLoinSide = body.addOrReplaceChild("left_loin_side", CubeListBuilder.create()
                .texOffs(31, 26).mirror(true).addBox(-0.45f, 0, -3.4f, 0.9f, 7.4f, 6.8f), PartPose.offsetAndRotation(6.2f, -3.4f, 0, 0, 0, -0.16f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(135, 0).mirror(false).addBox(-4.6f, -8.6f, -4.6f, 9.2f, 8.4f, 8.6f)
                .texOffs(25, 77).mirror(false).addBox(-5, -6.3f, -5.5f, 10, 1.9f, 2)
                .texOffs(224, 56).mirror(false).addBox(-5.3f, -3.9f, -4.9f, 10.6f, 2.4f, 5)
                .texOffs(174, 68).mirror(false).addBox(-1.9f, -5, -6, 3.8f, 2.6f, 1.6f)
                .texOffs(50, 77).mirror(false).addBox(-3.6f, -2, -5.3f, 7.2f, 1.3f, 2)
                .texOffs(0, 26).mirror(false).addBox(-5.4f, -9.4f, 1.6f, 10.8f, 11.6f, 3.6f), PartPose.offsetAndRotation(0, -21.4f, -2.8f, 0.05f, 0, 0));
        PartDefinition hairTop = head.addOrReplaceChild("hair_top", CubeListBuilder.create()
                .texOffs(158, 56).mirror(false).addBox(-1.5f, -4.6f, -1.6f, 3, 5, 3.4f), PartPose.offsetAndRotation(0, -8.4f, -1, -0.35f, 0, 0));
        PartDefinition rightHairTop = head.addOrReplaceChild("right_hair_top", CubeListBuilder.create()
                .texOffs(173, 56).mirror(false).addBox(-1.4f, -4, -1.5f, 2.8f, 4.4f, 3.2f), PartPose.offsetAndRotation(-2.8f, -8.2f, -0.6f, -0.3f, 0, -0.45f));
        PartDefinition leftHairTop = head.addOrReplaceChild("left_hair_top", CubeListBuilder.create()
                .texOffs(173, 56).mirror(true).addBox(-1.4f, -4, -1.5f, 2.8f, 4.4f, 3.2f), PartPose.offsetAndRotation(2.8f, -8.2f, -0.6f, -0.3f, 0, 0.45f));
        PartDefinition rightHairBack = head.addOrReplaceChild("right_hair_back", CubeListBuilder.create()
                .texOffs(0, 68).mirror(false).addBox(-1.4f, -4.2f, -1.4f, 2.8f, 4.6f, 3), PartPose.offsetAndRotation(-2.2f, -8, 1.8f, 0.35f, 0, -0.3f));
        PartDefinition leftHairBack = head.addOrReplaceChild("left_hair_back", CubeListBuilder.create()
                .texOffs(0, 68).mirror(true).addBox(-1.4f, -4.2f, -1.4f, 2.8f, 4.6f, 3), PartPose.offsetAndRotation(2.2f, -8, 1.8f, 0.35f, 0, 0.3f));
        PartDefinition hairBack = head.addOrReplaceChild("hair_back", CubeListBuilder.create()
                .texOffs(188, 56).mirror(false).addBox(-1.6f, -4.8f, -1.4f, 3.2f, 5.2f, 3), PartPose.offsetAndRotation(0, -8.2f, 2.2f, 0.55f, 0, 0));
        PartDefinition noseRing = head.addOrReplaceChild("nose_ring", CubeListBuilder.create()
                .texOffs(117, 77).mirror(false).addBox(-1.1f, 0, -0.25f, 2.2f, 1.8f, 0.5f), PartPose.offsetAndRotation(0, -2.6f, -6.2f, 0.3f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(94, 43).mirror(false).addBox(-4.5f, 0, -4.7f, 9, 3.2f, 7.4f)
                .texOffs(96, 68).mirror(false).addBox(-3.3f, 2.8f, -4.4f, 6.6f, 3, 3), PartPose.offsetAndRotation(0, -1.4f, -1.2f, 0.1f, 0, 0));
        PartDefinition rightTusk = jaw.addOrReplaceChild("right_tusk", CubeListBuilder.create()
                .texOffs(117, 68).mirror(false).addBox(-0.75f, -3.6f, -0.75f, 1.5f, 3.9f, 1.5f), PartPose.offsetAndRotation(-3, 0.4f, -4.1f, -0.18f, 0, -0.22f));
        PartDefinition leftTusk = jaw.addOrReplaceChild("left_tusk", CubeListBuilder.create()
                .texOffs(117, 68).mirror(true).addBox(-0.75f, -3.6f, -0.75f, 1.5f, 3.9f, 1.5f), PartPose.offsetAndRotation(3, 0.4f, -4.1f, -0.18f, 0, 0.22f));
        PartDefinition rightTuskTip = rightTusk.addOrReplaceChild("right_tusk_tip", CubeListBuilder.create()
                .texOffs(187, 68).mirror(false).addBox(-0.55f, -2, -0.55f, 1.1f, 2.2f, 1.1f), PartPose.offsetAndRotation(0, -3.5f, 0, -0.35f, 0, 0.1f));
        PartDefinition leftTuskTip = leftTusk.addOrReplaceChild("left_tusk_tip", CubeListBuilder.create()
                .texOffs(187, 68).mirror(true).addBox(-0.55f, -2, -0.55f, 1.1f, 2.2f, 1.1f), PartPose.offsetAndRotation(0, -3.5f, 0, -0.35f, 0, -0.1f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(126, 77).mirror(false).addBox(-1.45f, -0.8f, -0.5f, 2.9f, 1.7f, 0.8f), PartPose.offsetAndRotation(-2.4f, -4.75f, -5, 0, 0.12f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(126, 77).mirror(true).addBox(-1.45f, -0.8f, -0.5f, 2.9f, 1.7f, 0.8f), PartPose.offsetAndRotation(2.4f, -4.75f, -5, 0, -0.12f, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(196, 68).mirror(false).addBox(-2.8f, -1.8f, -0.4f, 2.8f, 3.8f, 0.8f), PartPose.offsetAndRotation(-4.6f, -4.6f, -0.6f, 0.05f, 0.45f, -0.3f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(196, 68).mirror(true).addBox(0, -1.8f, -0.4f, 2.8f, 3.8f, 0.8f), PartPose.offsetAndRotation(4.6f, -4.6f, -0.6f, 0.05f, -0.45f, 0.3f));
        PartDefinition rightHoop = rightEar.addOrReplaceChild("right_hoop", CubeListBuilder.create()
                .texOffs(71, 77).mirror(false).addBox(-0.3f, 0, -1.2f, 0.6f, 0.5f, 2.4f)
                .texOffs(71, 77).mirror(false).addBox(-0.3f, 2.6f, -1.2f, 0.6f, 0.5f, 2.4f)
                .texOffs(80, 77).mirror(false).addBox(-0.3f, 0.3f, -1.4f, 0.6f, 2.5f, 0.5f)
                .texOffs(80, 77).mirror(false).addBox(-0.3f, 0.3f, 0.9f, 0.6f, 2.5f, 0.5f), PartPose.offsetAndRotation(-1.6f, 1.8f, 0, 0, 0, 0.1f));
        PartDefinition leftHoop = leftEar.addOrReplaceChild("left_hoop", CubeListBuilder.create()
                .texOffs(71, 77).mirror(true).addBox(-0.3f, 0, -1.2f, 0.6f, 0.5f, 2.4f)
                .texOffs(71, 77).mirror(true).addBox(-0.3f, 2.6f, -1.2f, 0.6f, 0.5f, 2.4f)
                .texOffs(80, 77).mirror(true).addBox(-0.3f, 0.3f, -1.4f, 0.6f, 2.5f, 0.5f)
                .texOffs(80, 77).mirror(true).addBox(-0.3f, 0.3f, 0.9f, 0.6f, 2.5f, 0.5f), PartPose.offsetAndRotation(1.6f, 1.8f, 0, 0, 0, -0.1f));
        PartDefinition rightMane = head.addOrReplaceChild("right_mane", CubeListBuilder.create()
                .texOffs(203, 0).mirror(false).addBox(-1.6f, 0, -3.4f, 1.8f, 9.4f, 6.4f), PartPose.offsetAndRotation(-5, -8.2f, 0.4f, 0.15f, 0, 0.32f));
        PartDefinition leftMane = head.addOrReplaceChild("left_mane", CubeListBuilder.create()
                .texOffs(203, 0).mirror(true).addBox(-0.2f, 0, -3.4f, 1.8f, 9.4f, 6.4f), PartPose.offsetAndRotation(5, -8.2f, 0.4f, 0.15f, 0, -0.32f));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(30, 68).mirror(false).addBox(-1.2f, -3.4f, -1.2f, 2.4f, 3.6f, 2.4f), PartPose.offsetAndRotation(-2.8f, -8.4f, -2.4f, -0.3f, 0, -0.38f));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(30, 68).mirror(true).addBox(-1.2f, -3.4f, -1.2f, 2.4f, 3.6f, 2.4f), PartPose.offsetAndRotation(2.8f, -8.4f, -2.4f, -0.3f, 0, 0.38f));
        PartDefinition rightHornMid = rightHorn.addOrReplaceChild("right_horn_mid", CubeListBuilder.create()
                .texOffs(126, 68).mirror(false).addBox(-0.9f, -3.2f, -0.9f, 1.8f, 3.4f, 1.8f), PartPose.offsetAndRotation(0, -3.2f, 0, -0.42f, 0, 0.18f));
        PartDefinition leftHornMid = leftHorn.addOrReplaceChild("left_horn_mid", CubeListBuilder.create()
                .texOffs(126, 68).mirror(true).addBox(-0.9f, -3.2f, -0.9f, 1.8f, 3.4f, 1.8f), PartPose.offsetAndRotation(0, -3.2f, 0, -0.42f, 0, -0.18f));
        PartDefinition rightHornTip = rightHornMid.addOrReplaceChild("right_horn_tip", CubeListBuilder.create()
                .texOffs(205, 68).mirror(false).addBox(-0.55f, -2.8f, -0.55f, 1.1f, 3, 1.1f), PartPose.offsetAndRotation(0, -3, 0, -0.45f, 0, 0.12f));
        PartDefinition leftHornTip = leftHornMid.addOrReplaceChild("left_horn_tip", CubeListBuilder.create()
                .texOffs(205, 68).mirror(true).addBox(-0.55f, -2.8f, -0.55f, 1.1f, 3, 1.1f), PartPose.offsetAndRotation(0, -3, 0, -0.45f, 0, -0.12f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(48, 26).mirror(false).addBox(-4, -2.8f, -3.9f, 7.4f, 6.4f, 7.8f)
                .texOffs(222, 0).mirror(false).addBox(-3.2f, -0.4f, -3.2f, 6.2f, 9.4f, 6.4f)
                .texOffs(43, 68).mirror(false).addBox(-2.6f, 1.4f, -4, 5, 5.2f, 1), PartPose.offsetAndRotation(-10, -18.6f, 0.2f, -0.45f, 0, 0.3f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(48, 26).mirror(true).addBox(-3.4f, -2.8f, -3.9f, 7.4f, 6.4f, 7.8f)
                .texOffs(222, 0).mirror(true).addBox(-3, -0.4f, -3.2f, 6.2f, 9.4f, 6.4f)
                .texOffs(43, 68).mirror(true).addBox(-2.4f, 1.4f, -4, 5, 5.2f, 1), PartPose.offsetAndRotation(10, -18.6f, 0.2f, -0.45f, 0, -0.3f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(81, 26).mirror(false).addBox(-2.9f, 0, -2.9f, 5.8f, 8.8f, 5.8f)
                .texOffs(210, 26).mirror(false).addBox(-3.4f, 2.4f, -3.4f, 6.8f, 5.4f, 6.8f)
                .texOffs(135, 77).mirror(false).addBox(-0.5f, 4.2f, -4.6f, 1, 1, 1.4f)
                .texOffs(135, 77).mirror(false).addBox(-0.5f, 4.2f, 3.2f, 1, 1, 1.4f)
                .texOffs(149, 77).mirror(false).addBox(-4.6f, 4.2f, -0.5f, 1.4f, 1, 1)
                .texOffs(149, 77).mirror(false).addBox(3.2f, 4.2f, -0.5f, 1.4f, 1, 1), PartPose.offsetAndRotation(-0.1f, 8.6f, 0, -2.5f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(81, 26).mirror(true).addBox(-2.9f, 0, -2.9f, 5.8f, 8.8f, 5.8f)
                .texOffs(210, 26).mirror(true).addBox(-3.4f, 2.4f, -3.4f, 6.8f, 5.4f, 6.8f)
                .texOffs(135, 77).mirror(true).addBox(-0.5f, 4.2f, -4.6f, 1, 1, 1.4f)
                .texOffs(135, 77).mirror(true).addBox(-0.5f, 4.2f, 3.2f, 1, 1, 1.4f)
                .texOffs(149, 77).mirror(true).addBox(3.2f, 4.2f, -0.5f, 1.4f, 1, 1)
                .texOffs(149, 77).mirror(true).addBox(-4.6f, 4.2f, -0.5f, 1.4f, 1, 1), PartPose.offsetAndRotation(0.1f, 8.6f, 0, -2.5f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(50, 56).mirror(false).addBox(-2.8f, 0, -3, 5.6f, 4.4f, 6)
                .texOffs(214, 68).mirror(false).addBox(-3.2f, 0.6f, -3.4f, 1.4f, 2.6f, 2), PartPose.offsetAndRotation(0, 8.6f, 0, 0.2f, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(50, 56).mirror(true).addBox(-2.8f, 0, -3, 5.6f, 4.4f, 6)
                .texOffs(214, 68).mirror(true).addBox(1.8f, 0.6f, -3.4f, 1.4f, 2.6f, 2), PartPose.offsetAndRotation(0, 8.6f, 0, 0.2f, 0, 0));
        PartDefinition shoulderPlate = leftArm.addOrReplaceChild("shoulder_plate", CubeListBuilder.create()
                .texOffs(129, 43).mirror(false).addBox(-3.4f, -1, -4.4f, 7.6f, 2.4f, 8.8f)
                .texOffs(164, 43).mirror(false).addBox(-2.8f, 1.2f, -4.6f, 6.8f, 2, 9.2f)
                .texOffs(223, 68).mirror(false).addBox(0.6f, -2.8f, -2.6f, 1.2f, 2.2f, 1.2f)
                .texOffs(223, 68).mirror(false).addBox(0.6f, -2.8f, 1.2f, 1.2f, 2.2f, 1.2f)
                .texOffs(85, 77).mirror(false).addBox(2.8f, -2, -0.6f, 1.2f, 2, 1.2f), PartPose.offsetAndRotation(0.6f, -3, 0, 0, 0, -0.25f));
        PartDefinition club = rightHand.addOrReplaceChild("club", CubeListBuilder.create()
                .texOffs(106, 26).mirror(false).addBox(-0.95f, -6, -0.95f, 1.9f, 13, 1.9f)
                .texOffs(232, 68).mirror(false).addBox(-1.4f, 7, -1.4f, 2.8f, 1.6f, 2.8f)
                .texOffs(135, 68).mirror(false).addBox(-1.9f, -7.6f, -1.9f, 3.8f, 1.8f, 3.8f)
                .texOffs(0, 0).mirror(false).addBox(-2.7f, -26, -2.7f, 5.4f, 18.6f, 5.4f)
                .texOffs(56, 68).mirror(false).addBox(-2.2f, -27.6f, -2.2f, 4.4f, 1.8f, 4.4f)
                .texOffs(245, 68).mirror(false).addBox(-0.6f, -29.6f, -0.6f, 1.2f, 2.2f, 1.2f)
                .texOffs(142, 77).mirror(false).addBox(-1.8f, -24, -4.4f, 1, 1, 1.8f)
                .texOffs(142, 77).mirror(false).addBox(-1.8f, -24, 2.6f, 1, 1, 1.8f)
                .texOffs(156, 77).mirror(false).addBox(-4.4f, -24, -1.8f, 1.8f, 1, 1)
                .texOffs(156, 77).mirror(false).addBox(2.6f, -24, -1.8f, 1.8f, 1, 1)
                .texOffs(142, 77).mirror(false).addBox(0.2f, -24, -4.4f, 1, 1, 1.8f)
                .texOffs(142, 77).mirror(false).addBox(0.2f, -24, 2.6f, 1, 1, 1.8f)
                .texOffs(156, 77).mirror(false).addBox(-4.4f, -24, 0.2f, 1.8f, 1, 1)
                .texOffs(156, 77).mirror(false).addBox(2.6f, -24, 0.2f, 1.8f, 1, 1)
                .texOffs(142, 77).mirror(false).addBox(-0.8f, -20.4f, -4.4f, 1, 1, 1.8f)
                .texOffs(142, 77).mirror(false).addBox(-0.8f, -20.4f, 2.6f, 1, 1, 1.8f)
                .texOffs(156, 77).mirror(false).addBox(-4.4f, -20.4f, -0.8f, 1.8f, 1, 1)
                .texOffs(156, 77).mirror(false).addBox(2.6f, -20.4f, -0.8f, 1.8f, 1, 1)
                .texOffs(142, 77).mirror(false).addBox(1.2f, -20.4f, -4.4f, 1, 1, 1.8f)
                .texOffs(142, 77).mirror(false).addBox(1.2f, -20.4f, 2.6f, 1, 1, 1.8f)
                .texOffs(156, 77).mirror(false).addBox(-4.4f, -20.4f, 1.2f, 1.8f, 1, 1)
                .texOffs(156, 77).mirror(false).addBox(2.6f, -20.4f, 1.2f, 1.8f, 1, 1)
                .texOffs(142, 77).mirror(false).addBox(-1.8f, -16.8f, -4.4f, 1, 1, 1.8f)
                .texOffs(142, 77).mirror(false).addBox(-1.8f, -16.8f, 2.6f, 1, 1, 1.8f)
                .texOffs(156, 77).mirror(false).addBox(-4.4f, -16.8f, -1.8f, 1.8f, 1, 1)
                .texOffs(156, 77).mirror(false).addBox(2.6f, -16.8f, -1.8f, 1.8f, 1, 1)
                .texOffs(142, 77).mirror(false).addBox(0.2f, -16.8f, -4.4f, 1, 1, 1.8f)
                .texOffs(142, 77).mirror(false).addBox(0.2f, -16.8f, 2.6f, 1, 1, 1.8f)
                .texOffs(156, 77).mirror(false).addBox(-4.4f, -16.8f, 0.2f, 1.8f, 1, 1)
                .texOffs(156, 77).mirror(false).addBox(2.6f, -16.8f, 0.2f, 1.8f, 1, 1)
                .texOffs(142, 77).mirror(false).addBox(-0.8f, -13.2f, -4.4f, 1, 1, 1.8f)
                .texOffs(142, 77).mirror(false).addBox(-0.8f, -13.2f, 2.6f, 1, 1, 1.8f)
                .texOffs(156, 77).mirror(false).addBox(-4.4f, -13.2f, -0.8f, 1.8f, 1, 1)
                .texOffs(156, 77).mirror(false).addBox(2.6f, -13.2f, -0.8f, 1.8f, 1, 1)
                .texOffs(142, 77).mirror(false).addBox(1.2f, -13.2f, -4.4f, 1, 1, 1.8f)
                .texOffs(142, 77).mirror(false).addBox(1.2f, -13.2f, 2.6f, 1, 1, 1.8f)
                .texOffs(156, 77).mirror(false).addBox(-4.4f, -13.2f, 1.2f, 1.8f, 1, 1)
                .texOffs(156, 77).mirror(false).addBox(2.6f, -13.2f, 1.2f, 1.8f, 1, 1)
                .texOffs(142, 77).mirror(false).addBox(-1.8f, -9.6f, -4.4f, 1, 1, 1.8f)
                .texOffs(142, 77).mirror(false).addBox(-1.8f, -9.6f, 2.6f, 1, 1, 1.8f)
                .texOffs(156, 77).mirror(false).addBox(-4.4f, -9.6f, -1.8f, 1.8f, 1, 1)
                .texOffs(156, 77).mirror(false).addBox(2.6f, -9.6f, -1.8f, 1.8f, 1, 1)
                .texOffs(142, 77).mirror(false).addBox(0.2f, -9.6f, -4.4f, 1, 1, 1.8f)
                .texOffs(142, 77).mirror(false).addBox(0.2f, -9.6f, 2.6f, 1, 1, 1.8f)
                .texOffs(156, 77).mirror(false).addBox(-4.4f, -9.6f, 0.2f, 1.8f, 1, 1)
                .texOffs(156, 77).mirror(false).addBox(2.6f, -9.6f, 0.2f, 1.8f, 1, 1), PartPose.offsetAndRotation(0, 2, -0.2f, 1.9236f, 0.386f, 0.8556f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED ogre: end
    }

    /**
     * The ogre's pose: hunched, the club over its right shoulder, its left fist hanging; walking, a
     * heavy rolling stomp; a blow, the club swung down off its shoulder; heaving up for a slam, both
     * arms raise the club high over its head, its jaws gape, and it brings it down. Its jaw works, its
     * mane and its tiger skin swing.
     */
    public static final class OgreModel extends EntityModel<OgreEntity> {
        private final ModelPart root, body, head, jaw, rightArm, leftArm, rightForearm, leftForearm, rightLeg, leftLeg,
                rightShin, leftShin, loinFront, loinBack, tigerHead, rightMane, leftMane;

        public OgreModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightMane = head.getChild("right_mane");
            leftMane = head.getChild("left_mane");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            loinFront = body.getChild("loin_front");
            loinBack = body.getChild("loin_back");
            tigerHead = body.getChild("tiger_head");
        }

        @Override
        public void setupAnim(OgreEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = Mth.cos(swing * 0.5f) * 0.55f * amount;
            float bob = Math.abs(Mth.sin(swing * 0.5f)) * amount;
            float breath = Mth.sin(age * 0.05f);
            // heaving the club up for a slam (and, its last ticks, bringing it down)
            int w = e.slamWindup();
            float raise = 0, down = 0;
            if (w > 0) {
                raise = w > 3 ? Math.min(1, (e.slamWindupTicks() - w) / 8f) : w / 3f;
                down = w <= 3 ? 1 - w / 3f : 0;
            }
            // a blow: the club swung down off its shoulder
            float chop = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            turn(rightShin, Math.max(0, -stride) * 0.5f, 0, 0);
            turn(leftShin, Math.max(0, stride) * 0.5f, 0, 0);
            turn(body, bob * 0.04f + breath * 0.02f - raise * 0.18f + (chop + down) * 0.22f, 0, Mth.sin(swing * 0.5f) * 0.07f * amount);
            body.y = body.getInitialPose().y + bob * 1.2f;
            turn(head, pitch * Mth.DEG_TO_RAD * 0.7f - raise * 0.2f, yaw * Mth.DEG_TO_RAD * 0.8f, 0);
            turn(jaw, Math.max(0, Mth.sin(age * 0.045f)) * 0.08f + Math.max(raise, chop) * 0.35f, 0, 0);
            // the club: on its shoulder; a blow unfolds the arm and swings it down before it; for a
            // slam both arms heave it straight up over its head, then bring it down on the ground ahead
            float lift = w > 3 ? raise : w > 0 ? 1 - down : 0;         // how high the arms are, 0..1
            float straight = w > 0 ? 1 : 0;                            // the forearm unfolded for a slam
            turn(rightArm, -stride * 0.15f - chop * 0.3f - lift * 2.25f - down * 0.1f, 0, -lift * 0.15f);
            turn(rightForearm, chop * 2.2f + straight * 2.5f, 0, 0);
            turn(leftArm, stride * 0.5f - lift * 2.8f - down * 1.0f, 0, lift * 0.3f);
            turn(leftForearm, -lift * 0.3f, 0, 0);
            // its mane and tiger skin swing
            turn(rightMane, 0, 0, Mth.sin(age * 0.06f) * 0.04f - bob * 0.05f);
            turn(leftMane, 0, 0, -Mth.sin(age * 0.06f) * 0.04f + bob * 0.05f);
            turn(loinFront, -Math.abs(stride) * 0.5f, 0, 0);
            turn(loinBack, Math.abs(stride) * 0.4f, 0, 0);
            turn(tigerHead, -Math.abs(stride) * 0.2f, 0, stride * 0.1f);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static class OgreRenderer extends MobRenderer<OgreEntity, OgreModel> {
        /** How much larger than its pixels it is drawn: some 3.2 blocks to its horns' tips (ogre.py prints it). */
        public static final float SIZE = 0.95f;
        private final ResourceLocation skin;
        private final float size;

        public OgreRenderer(EntityRendererProvider.Context ctx) {
            this(ctx, "ogre", SIZE);
        }

        /** The ogre's frame in another skin ({@code textures/entity/<skin>.png}, its glow {@code <skin>_glow.png}), drawn {@code size} large: the raid chief's. */
        public OgreRenderer(EntityRendererProvider.Context ctx, String skin, float size) {
            super(ctx, new OgreModel(ctx.bakeLayer(OGRE_LAYER)), 1.2f * size / SIZE);
            this.skin = tex(skin);
            this.size = size;
            addLayer(LionCourt.glow(this, skin));
        }

        @Override
        protected void scale(OgreEntity e, PoseStack pose, float partial) {
            pose.scale(size, size, size);
        }

        @Override
        protected boolean shouldShowName(OgreEntity e) {
            return super.shouldShowName(e) && near(entityRenderDispatcher, e);
        }

        @Override
        public ResourceLocation getTextureLocation(OgreEntity e) {
            return skin;
        }
    }
}
