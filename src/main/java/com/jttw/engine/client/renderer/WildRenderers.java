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
 * How the open world's yaoguai look: the goblin (a hunched, moss-skinned mountain imp, all ears,
 * nose and teeth, in a patched robe with prayer beads and stolen talismans, a sack of thefts on its
 * back, a bronze dagger and a sling), the kobold (a little dog-faced, scaly cave miner in a pot
 * helmet with a candle on it and scrap-iron armour, a pick in its hands and a red paper lantern
 * swinging over its pack of ore), the wolfman (a tall, lean wolf soldier in black lamellae laced in
 * red, a long glaive in its hands) and the ogre (a towering, red, pot-bellied mountain yaksha with
 * horns and tusks, a tiger-skin loincloth, spiked iron bracers, a necklace of skulls and a great
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
                .texOffs(59, 17).mirror(false).addBox(-1.35f, -0.6f, -1.4f, 2.7f, 5.8f, 2.8f)
                .texOffs(95, 46).mirror(false).addBox(-1.55f, 4.1f, -1.75f, 3.1f, 1.9f, 2.6f), PartPose.offsetAndRotation(-2.3f, 12.5522f, 0.4f, -0.42f, 0, 0.06f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(59, 17).mirror(true).addBox(-1.35f, -0.6f, -1.4f, 2.7f, 5.8f, 2.8f)
                .texOffs(95, 46).mirror(true).addBox(-1.55f, 4.1f, -1.75f, 3.1f, 1.9f, 2.6f), PartPose.offsetAndRotation(2.3f, 12.5522f, 0.4f, -0.42f, 0, -0.06f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(72, 17).mirror(false).addBox(-1.05f, 0, -1.05f, 2.1f, 5.6f, 2.1f)
                .texOffs(113, 28).mirror(false).addBox(-1.35f, 0.8f, -1.35f, 2.7f, 3.4f, 2.7f), PartPose.offsetAndRotation(0, 5.4f, 0.1f, 0.78f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(72, 17).mirror(true).addBox(-1.05f, 0, -1.05f, 2.1f, 5.6f, 2.1f)
                .texOffs(113, 28).mirror(true).addBox(-1.35f, 0.8f, -1.35f, 2.7f, 3.4f, 2.7f), PartPose.offsetAndRotation(0, 5.4f, 0.1f, 0.78f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(0, 38).mirror(false).addBox(-1.5f, -0.2f, -3.4f, 3, 1.5f, 4.6f)
                .texOffs(53, 59).mirror(false).addBox(-1.55f, 0.35f, -4.6f, 0.8f, 1, 1.4f)
                .texOffs(53, 59).mirror(false).addBox(-0.4f, 0.35f, -4.8f, 0.8f, 1, 1.5f)
                .texOffs(53, 59).mirror(false).addBox(0.75f, 0.35f, -4.5f, 0.8f, 1, 1.3f)
                .texOffs(60, 59).mirror(false).addBox(-0.6f, 0.6f, 0.9f, 1.2f, 0.8f, 1.2f), PartPose.offsetAndRotation(0, 5.4f, 0, -0.36f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(0, 38).mirror(true).addBox(-1.5f, -0.2f, -3.4f, 3, 1.5f, 4.6f)
                .texOffs(53, 59).mirror(true).addBox(0.75f, 0.35f, -4.6f, 0.8f, 1, 1.4f)
                .texOffs(53, 59).mirror(true).addBox(-0.4f, 0.35f, -4.8f, 0.8f, 1, 1.5f)
                .texOffs(53, 59).mirror(true).addBox(-1.55f, 0.35f, -4.5f, 0.8f, 1, 1.3f)
                .texOffs(60, 59).mirror(true).addBox(-0.6f, 0.6f, 0.9f, 1.2f, 0.8f, 1.2f), PartPose.offsetAndRotation(0, 5.4f, 0, -0.36f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(85, 17).mirror(false).addBox(-3.2f, -2.4f, -2.3f, 6.4f, 3.4f, 4.6f)
                .texOffs(104, 0).mirror(false).addBox(-3, -7, -2.65f, 6, 4.9f, 5)
                .texOffs(0, 17).mirror(false).addBox(-3.7f, -11.4f, -2.3f, 7.4f, 4.8f, 4.6f)
                .texOffs(17, 38).mirror(false).addBox(-4.3f, -11.9f, -2.1f, 8.6f, 1.6f, 4.3f)
                .texOffs(53, 0).mirror(false).addBox(-4, -11.6f, 1.9f, 8, 10.8f, 0.9f)
                .texOffs(17, 28).mirror(false).addBox(-3.55f, -3.3f, -2.95f, 7.1f, 1.3f, 5.6f)
                .texOffs(110, 46).mirror(false).addBox(-1.4f, -12.6f, -1.7f, 2.8f, 1.6f, 2.8f)
                .texOffs(69, 59).mirror(false).addBox(-1, -12.1f, -3.25f, 2, 1.6f, 0.5f)
                .texOffs(81, 53).mirror(false).addBox(-3.397f, -11.5998f, -3.0288f, 1.1f, 1.1f, 1.1f)
                .texOffs(90, 53).mirror(false).addBox(-3.0744f, -11.1206f, -3.0851f, 1.1f, 1.1f, 1.1f)
                .texOffs(81, 53).mirror(false).addBox(-2.5949f, -10.5406f, -3.1329f, 1.1f, 1.1f, 1.1f)
                .texOffs(81, 53).mirror(false).addBox(-1.9883f, -9.9158f, -3.1694f, 1.1f, 1.1f, 1.1f)
                .texOffs(90, 53).mirror(false).addBox(-1.2922f, -9.3007f, -3.1922f, 1.1f, 1.1f, 1.1f)
                .texOffs(81, 53).mirror(false).addBox(-0.55f, -8.75f, -3.2f, 1.1f, 1.1f, 1.1f)
                .texOffs(81, 53).mirror(false).addBox(0.1922f, -9.3007f, -3.1922f, 1.1f, 1.1f, 1.1f)
                .texOffs(90, 53).mirror(false).addBox(0.8883f, -9.9158f, -3.1694f, 1.1f, 1.1f, 1.1f)
                .texOffs(81, 53).mirror(false).addBox(1.4949f, -10.5406f, -3.1329f, 1.1f, 1.1f, 1.1f)
                .texOffs(81, 53).mirror(false).addBox(1.9744f, -11.1206f, -3.0851f, 1.1f, 1.1f, 1.1f)
                .texOffs(90, 53).mirror(false).addBox(2.297f, -11.5998f, -3.0288f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(0, 12.5522f, 0.4f, 0.32f, 0, 0));
        PartDefinition rightRobe = body.addOrReplaceChild("right_robe", CubeListBuilder.create()
                .texOffs(72, 0).mirror(false).addBox(-0.2f, 0, -0.5f, 2.7f, 10.9f, 0.6f), PartPose.offsetAndRotation(-3.95f, -11.6f, -2.35f, -0.06f, 0.1f, 0.07f));
        PartDefinition leftRobe = body.addOrReplaceChild("left_robe", CubeListBuilder.create()
                .texOffs(72, 0).mirror(true).addBox(-2.5f, 0, -0.5f, 2.7f, 10.9f, 0.6f), PartPose.offsetAndRotation(3.95f, -11.6f, -2.35f, -0.06f, -0.1f, -0.07f));
        PartDefinition rightRobeSide = body.addOrReplaceChild("right_robe_side", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.6f, 0, -2.1f, 0.6f, 10.6f, 4.2f), PartPose.offsetAndRotation(-3.95f, -11.6f, -0.2f, 0, 0, 0.09f));
        PartDefinition leftRobeSide = body.addOrReplaceChild("left_robe_side", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(0, 0, -2.1f, 0.6f, 10.6f, 4.2f), PartPose.offsetAndRotation(3.95f, -11.6f, -0.2f, 0, 0, -0.09f));
        PartDefinition loinFront = body.addOrReplaceChild("loin_front", CubeListBuilder.create()
                .texOffs(46, 38).mirror(false).addBox(-1.8f, 0, -0.3f, 3.6f, 6, 0.5f), PartPose.offsetAndRotation(0, -2.2f, -2.6f, -0.12f, 0, 0));
        PartDefinition loinBack = body.addOrReplaceChild("loin_back", CubeListBuilder.create()
                .texOffs(0, 46).mirror(false).addBox(-2.2f, 0, -0.2f, 4.4f, 5, 0.5f), PartPose.offsetAndRotation(0, -2.2f, 2.4f, 0.14f, 0, 0));
        PartDefinition talisman = body.addOrReplaceChild("talisman", CubeListBuilder.create()
                .texOffs(13, 46).mirror(false).addBox(-1, 0, -0.1f, 2, 4.2f, 0.2f), PartPose.offsetAndRotation(0.1f, -8.5f, -3.15f, -0.2f, 0, 0.12f));
        PartDefinition strap = body.addOrReplaceChild("strap", CubeListBuilder.create()
                .texOffs(46, 0).mirror(false).addBox(-0.55f, 0, -0.25f, 1.1f, 12.5f, 0.4f), PartPose.offsetAndRotation(-3.4f, -11.6f, -2.55f, 0, 0, -0.62f));
        PartDefinition sack = body.addOrReplaceChild("sack", CubeListBuilder.create()
                .texOffs(81, 0).mirror(false).addBox(-3.2f, 0, 0, 6.4f, 6.6f, 3.9f)
                .texOffs(0, 53).mirror(false).addBox(-1.3f, -1.1f, 0.8f, 2.6f, 1.3f, 2.3f)
                .texOffs(57, 38).mirror(false).addBox(-2.6f, -3.6f, 1.2f, 1.2f, 4.2f, 1.2f)
                .texOffs(20, 46).mirror(false).addBox(1.3f, -4.4f, 1.6f, 0.7f, 5, 0.7f)
                .texOffs(99, 53).mirror(false).addBox(-2.2f, 2.2f, 3.85f, 2.2f, 2.8f, 0.15f), PartPose.offsetAndRotation(0, -10.4f, 2.8f, 0.25f, 0, 0.12f));
        PartDefinition gourd = body.addOrReplaceChild("gourd", CubeListBuilder.create()
                .texOffs(0, 64).mirror(false).addBox(-0.25f, -0.5f, -0.25f, 0.5f, 0.8f, 0.5f)
                .texOffs(108, 53).mirror(false).addBox(-0.85f, 0.2f, -0.85f, 1.7f, 1.5f, 1.7f)
                .texOffs(25, 46).mirror(false).addBox(-1.3f, 1.5f, -1.3f, 2.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(3.5f, -3, -1.3f, 0.1f, 0, -0.25f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(13, 0).mirror(false).addBox(-3.9f, -8.2f, -4.2f, 7.8f, 6.4f, 7.4f)
                .texOffs(46, 28).mirror(false).addBox(-3.2f, -9.4f, -3.4f, 6.4f, 1.4f, 6)
                .texOffs(73, 28).mirror(false).addBox(-4.6f, -4.4f, -3.9f, 9.2f, 2.2f, 4.6f)
                .texOffs(0, 59).mirror(false).addBox(-4.2f, -6, -4.9f, 8.4f, 1.5f, 1.4f)
                .texOffs(66, 38).mirror(false).addBox(-3.6f, -2.5f, -5.3f, 7.2f, 1.5f, 4.4f)
                .texOffs(13, 53).mirror(false).addBox(-1.45f, -10.9f, -0.1f, 2.9f, 1.9f, 2.9f)
                .texOffs(26, 53).mirror(false).addBox(-1.6f, -9.4f, -0.25f, 3.2f, 0.7f, 3.2f)
                .texOffs(5, 64).mirror(false).addBox(-2.9f, -10.4f, 1.15f, 5.8f, 0.5f, 0.5f)
                .texOffs(27, 17).mirror(false).addBox(-3.8f, -8, 2.4f, 7.6f, 7.4f, 1.8f), PartPose.offsetAndRotation(0, -12.2f, -1.6f, -0.3f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(93, 38).mirror(false).addBox(-3.1f, 0, -4.1f, 6.2f, 1.9f, 5)
                .texOffs(76, 59).mirror(false).addBox(-1.4f, 1.5f, -3.9f, 2.8f, 1, 2)
                .texOffs(87, 59).mirror(false).addBox(-2.6f, -1.1f, -4, 0.7f, 1.2f, 0.7f)
                .texOffs(87, 59).mirror(false).addBox(1.9f, -1.1f, -4, 0.7f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, -1.3f, -1.2f, 0.16f, 0, 0));
        PartDefinition nose = head.addOrReplaceChild("nose", CubeListBuilder.create()
                .texOffs(38, 46).mirror(false).addBox(-0.85f, -0.2f, -3, 1.7f, 1.9f, 3.1f), PartPose.offsetAndRotation(0, -5.7f, -4.7f, 0.55f, 0, 0));
        PartDefinition noseTip = nose.addOrReplaceChild("nose_tip", CubeListBuilder.create()
                .texOffs(43, 53).mirror(false).addBox(-0.8f, -0.3f, -1.1f, 1.6f, 2.2f, 1.4f), PartPose.offsetAndRotation(0, 0.4f, -2.7f, 0.5f, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(92, 59).mirror(false).addBox(-1.3f, -0.7f, -0.4f, 2.6f, 1.5f, 0.5f), PartPose.offsetAndRotation(-2.05f, -4.75f, -4.75f, 0, 0.15f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(92, 59).mirror(true).addBox(-1.3f, -0.7f, -0.4f, 2.6f, 1.5f, 0.5f), PartPose.offsetAndRotation(2.05f, -4.75f, -4.75f, 0, -0.15f, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(51, 46).mirror(false).addBox(-7.4f, -2.2f, -0.3f, 7.4f, 4.4f, 0.6f), PartPose.offsetAndRotation(-3.8f, -5.2f, -0.2f, 0.05f, 0.3f, 0.22f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(51, 46).mirror(true).addBox(0, -2.2f, -0.3f, 7.4f, 4.4f, 0.6f), PartPose.offsetAndRotation(3.8f, -5.2f, -0.2f, 0.05f, -0.3f, -0.22f));
        PartDefinition rightEarTip = rightEar.addOrReplaceChild("right_ear_tip", CubeListBuilder.create()
                .texOffs(23, 59).mirror(false).addBox(-4, -1.2f, -0.25f, 4, 2.4f, 0.5f), PartPose.offsetAndRotation(-7.2f, -0.4f, 0, 0, 0.1f, -0.4f));
        PartDefinition leftEarTip = leftEar.addOrReplaceChild("left_ear_tip", CubeListBuilder.create()
                .texOffs(23, 59).mirror(true).addBox(0, -1.2f, -0.25f, 4, 2.4f, 0.5f), PartPose.offsetAndRotation(7.2f, -0.4f, 0, 0, -0.1f, 0.4f));
        PartDefinition earring = rightEar.addOrReplaceChild("earring", CubeListBuilder.create()
                .texOffs(34, 59).mirror(false).addBox(-0.25f, 0, -0.8f, 0.5f, 1.8f, 1.6f), PartPose.offsetAndRotation(-5, 1.8f, 0, 0, 0, 0));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(52, 53).mirror(false).addBox(-0.6f, -2.3f, -0.6f, 1.2f, 2.5f, 1.2f), PartPose.offsetAndRotation(-2.2f, -8.6f, -2.2f, -0.75f, 0, -0.3f));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(52, 53).mirror(true).addBox(-0.6f, -2.3f, -0.6f, 1.2f, 2.5f, 1.2f), PartPose.offsetAndRotation(2.2f, -8.6f, -2.2f, -0.75f, 0, 0.3f));
        PartDefinition rightHornTip = rightHorn.addOrReplaceChild("right_horn_tip", CubeListBuilder.create()
                .texOffs(101, 59).mirror(false).addBox(-0.4f, -1.5f, -0.4f, 0.8f, 1.6f, 0.8f), PartPose.offsetAndRotation(0, -2.2f, 0, -0.5f, 0, 0));
        PartDefinition leftHornTip = leftHorn.addOrReplaceChild("left_horn_tip", CubeListBuilder.create()
                .texOffs(101, 59).mirror(true).addBox(-0.4f, -1.5f, -0.4f, 0.8f, 1.6f, 0.8f), PartPose.offsetAndRotation(0, -2.2f, 0, -0.5f, 0, 0));
        PartDefinition rightTuft = head.addOrReplaceChild("right_tuft", CubeListBuilder.create()
                .texOffs(48, 17).mirror(false).addBox(-1, 0, -1.6f, 1, 5.4f, 3.2f), PartPose.offsetAndRotation(-3.9f, -7.6f, 1, 0.1f, 0, 0.42f));
        PartDefinition leftTuft = head.addOrReplaceChild("left_tuft", CubeListBuilder.create()
                .texOffs(48, 17).mirror(true).addBox(0, 0, -1.6f, 1, 5.4f, 3.2f), PartPose.offsetAndRotation(3.9f, -7.6f, 1, 0.1f, 0, -0.42f));
        PartDefinition headTalisman = head.addOrReplaceChild("head_talisman", CubeListBuilder.create()
                .texOffs(61, 53).mirror(false).addBox(-0.95f, 0, -0.1f, 1.9f, 3.8f, 0.2f), PartPose.offsetAndRotation(2.4f, -8.4f, -3.7f, -0.25f, 0.25f, 0.42f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(110, 17).mirror(false).addBox(-1.2f, -0.6f, -1.1f, 2.2f, 6, 2.2f)
                .texOffs(0, 28).mirror(false).addBox(-1.85f, -1.3f, -1.75f, 3.4f, 4.4f, 3.5f), PartPose.offsetAndRotation(-4.5f, -10.8f, -0.2f, -0.3f, 0, 0.2f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(110, 17).mirror(true).addBox(-1, -0.6f, -1.1f, 2.2f, 6, 2.2f)
                .texOffs(0, 28).mirror(true).addBox(-1.55f, -1.3f, -1.75f, 3.4f, 4.4f, 3.5f), PartPose.offsetAndRotation(4.5f, -10.8f, -0.2f, -0.3f, 0, -0.2f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(104, 28).mirror(false).addBox(-1, 0, -1, 2, 5.8f, 2)
                .texOffs(70, 46).mirror(false).addBox(-1.25f, 3.2f, -1.25f, 2.5f, 2.4f, 2.5f), PartPose.offsetAndRotation(-0.1f, 5.6f, 0, -0.75f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(104, 28).mirror(true).addBox(-1, 0, -1, 2, 5.8f, 2)
                .texOffs(70, 46).mirror(true).addBox(-1.25f, 3.2f, -1.25f, 2.5f, 2.4f, 2.5f), PartPose.offsetAndRotation(0.1f, 5.6f, 0, -0.75f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(68, 53).mirror(false).addBox(-1.3f, 0, -1.4f, 2.6f, 2, 2.8f)
                .texOffs(106, 59).mirror(false).addBox(-1.25f, 1.7f, -1.35f, 0.7f, 1.7f, 0.7f)
                .texOffs(106, 59).mirror(false).addBox(-0.35f, 1.7f, -1.35f, 0.7f, 1.9f, 0.7f)
                .texOffs(106, 59).mirror(false).addBox(0.55f, 1.7f, -1.35f, 0.7f, 1.6f, 0.7f)
                .texOffs(111, 59).mirror(false).addBox(-1.4f, 0.6f, -1.9f, 0.8f, 1.4f, 0.8f), PartPose.offsetAndRotation(0, 5.7f, 0, 0.1f, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(68, 53).mirror(true).addBox(-1.3f, 0, -1.4f, 2.6f, 2, 2.8f)
                .texOffs(106, 59).mirror(true).addBox(0.55f, 1.7f, -1.35f, 0.7f, 1.7f, 0.7f)
                .texOffs(106, 59).mirror(true).addBox(-0.35f, 1.7f, -1.35f, 0.7f, 1.9f, 0.7f)
                .texOffs(106, 59).mirror(true).addBox(-1.25f, 1.7f, -1.35f, 0.7f, 1.6f, 0.7f)
                .texOffs(111, 59).mirror(true).addBox(0.6f, 0.6f, -1.9f, 0.8f, 1.4f, 0.8f), PartPose.offsetAndRotation(0, 5.7f, 0, 0.1f, 0, 0));
        PartDefinition dagger = rightHand.addOrReplaceChild("dagger", CubeListBuilder.create()
                .texOffs(41, 59).mirror(false).addBox(-0.45f, -1.6f, -0.45f, 0.9f, 2.9f, 0.9f)
                .texOffs(20, 64).mirror(false).addBox(-0.5f, 1.2f, -0.5f, 1, 0.8f, 1)
                .texOffs(25, 64).mirror(false).addBox(-1.35f, -2.1f, -0.45f, 2.7f, 0.55f, 0.9f)
                .texOffs(83, 46).mirror(false).addBox(-0.85f, -7, -0.25f, 1.7f, 5, 0.5f), PartPose.offsetAndRotation(0, 1.3f, -0.3f, 1.8704f, 0.1623f, -0.1174f));
        PartDefinition bladeTip = dagger.addOrReplaceChild("blade_tip", CubeListBuilder.create()
                .texOffs(46, 59).mirror(false).addBox(-0.75f, -2.6f, -0.25f, 1.5f, 2.7f, 0.5f), PartPose.offsetAndRotation(0, -6.8f, 0, 0, 0, 0.28f));
        PartDefinition sling = leftHand.addOrReplaceChild("sling", CubeListBuilder.create()
                .texOffs(90, 46).mirror(false).addBox(-0.75f, 0, -0.15f, 0.3f, 5, 0.3f)
                .texOffs(90, 46).mirror(false).addBox(0.45f, 0, -0.15f, 0.3f, 5, 0.3f)
                .texOffs(116, 59).mirror(false).addBox(-0.95f, 4.8f, -0.6f, 1.9f, 1, 1.2f)
                .texOffs(34, 64).mirror(false).addBox(-0.45f, 4.3f, -0.45f, 0.9f, 0.9f, 0.9f), PartPose.offsetAndRotation(0, 1.6f, -0.3f, 0.3187f, -0.1235f, 0.1825f));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED goblin: end
    }

    /**
     * The goblin's pose: crouched, the dagger held out, the sling dangling; walking, a quick scuttle,
     * arms swinging; stabbing, the dagger thrust forward; running scared, it cowers with its arms over
     * its head and its ears flat. Its ears twitch, its jaw works.
     */
    public static final class GoblinModel extends EntityModel<GoblinEntity> {
        private final ModelPart root, body, head, jaw, rightEar, leftEar, rightEarTip, leftEarTip, rightArm, leftArm,
                rightForearm, leftForearm, rightLeg, leftLeg, rightShin, leftShin, sack, gourd, talisman, loinFront, sling;

        public GoblinModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
            rightEarTip = rightEar.getChild("right_ear_tip");
            leftEarTip = leftEar.getChild("left_ear_tip");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            sling = leftForearm.getChild("left_hand").getChild("sling");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            sack = body.getChild("sack");
            gourd = body.getChild("gourd");
            talisman = body.getChild("talisman");
            loinFront = body.getChild("loin_front");
        }

        @Override
        public void setupAnim(GoblinEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = Mth.cos(swing * 0.9f) * 1.0f * amount;
            float bob = Math.abs(Mth.sin(swing * 0.9f)) * amount;
            float breath = Mth.sin(age * 0.09f);
            float blow = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            boolean cower = e.isFleeing();
            // the legs scuttle, the knee bending as each comes forward
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            turn(rightShin, Math.max(0, -stride) * 0.8f, 0, 0);
            turn(leftShin, Math.max(0, stride) * 0.8f, 0, 0);
            // the body bobs and hunches; cowering, it hunches lower
            turn(body, (cower ? 0.25f : 0) + bob * 0.08f + breath * 0.02f, 0, 0);
            body.y = body.getInitialPose().y + bob * 0.6f;
            // the head looks about, the jaw works, the ears twitch (flat back when it cowers)
            turn(head, pitch * Mth.DEG_TO_RAD - (cower ? 0.15f : 0), yaw * Mth.DEG_TO_RAD, 0);
            turn(jaw, Math.max(0, Mth.sin(age * 0.23f)) * 0.12f + blow * 0.25f + (cower ? 0.3f : 0), 0, 0);
            float twitch = Mth.sin(age * 0.31f) * Mth.sin(age * 0.07f) * 0.12f;
            float flat = cower ? 0.6f : 0;
            turn(rightEar, 0, flat, twitch - flat * 0.3f);
            turn(leftEar, 0, -flat, -twitch + flat * 0.3f);
            turn(rightEarTip, 0, 0, Mth.sin(age * 0.12f) * 0.08f);
            turn(leftEarTip, 0, 0, -Mth.sin(age * 0.12f + 1) * 0.08f);
            // the arms swing against the legs; a stab thrusts the dagger forward
            float rx = -stride * 0.7f - blow * 1.3f, lx = stride * 0.7f;
            float rf = blow * 0.65f, lf = 0;
            if (cower) { // arms up over its head
                rx = -2.3f;
                lx = -2.3f;
                rf = -0.6f;
                lf = -0.6f;
            }
            turn(rightArm, rx, 0, cower ? 0.3f : 0);
            turn(leftArm, lx, 0, cower ? -0.3f : 0);
            turn(rightForearm, rf, 0, 0);
            turn(leftForearm, lf, 0, 0);
            // the sling hangs, swinging; its gear sways
            turn(sling, -(lx + lf) * 0.85f + Mth.sin(age * 0.2f) * 0.15f, 0, Mth.cos(age * 0.17f) * 0.1f);
            turn(sack, bob * 0.15f, 0, stride * 0.06f);
            turn(gourd, stride * 0.25f, 0, -stride * 0.15f);
            turn(talisman, -amount * 0.3f + Mth.sin(age * 0.1f) * 0.05f, 0, stride * 0.1f);
            turn(loinFront, -Math.abs(stride) * 0.4f, 0, 0);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class GoblinRenderer extends MobRenderer<GoblinEntity, GoblinModel> {
        /** How much larger than its pixels it is drawn: some 1.4 blocks to its topknot (goblin.py prints it). */
        public static final float SIZE = 0.68f;
        private static final ResourceLocation SKIN = tex("goblin");

        public GoblinRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new GoblinModel(ctx.bakeLayer(GOBLIN_LAYER)), 0.4f);
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
                .texOffs(29, 14).mirror(false).addBox(-1.55f, -0.8f, -1.6f, 3.1f, 5, 3.2f), PartPose.offsetAndRotation(-2.2f, 14.3215f, 1, -0.8f, 0, 0.08f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(29, 14).mirror(true).addBox(-1.55f, -0.8f, -1.6f, 3.1f, 5, 3.2f), PartPose.offsetAndRotation(2.2f, 14.3215f, 1, -0.8f, 0, -0.08f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(0, 25).mirror(false).addBox(-1.1f, 0, -1.1f, 2.2f, 4.4f, 2.2f)
                .texOffs(53, 43).mirror(false).addBox(-1.4f, 0.4f, -1.6f, 2.8f, 3, 1), PartPose.offsetAndRotation(0, 3.9f, 0.2f, 1.5f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(0, 25).mirror(true).addBox(-1.1f, 0, -1.1f, 2.2f, 4.4f, 2.2f)
                .texOffs(53, 43).mirror(true).addBox(-1.4f, 0.4f, -1.6f, 2.8f, 3, 1), PartPose.offsetAndRotation(0, 3.9f, 0.2f, 1.5f, 0, 0));
        PartDefinition rightAnkle = rightShin.addOrReplaceChild("right_ankle", CubeListBuilder.create()
                .texOffs(45, 34).mirror(false).addBox(-0.9f, 0, -0.9f, 1.8f, 3.6f, 1.8f), PartPose.offsetAndRotation(0, 4.2f, 0, -1.35f, 0, 0));
        PartDefinition leftAnkle = leftShin.addOrReplaceChild("left_ankle", CubeListBuilder.create()
                .texOffs(45, 34).mirror(true).addBox(-0.9f, 0, -0.9f, 1.8f, 3.6f, 1.8f), PartPose.offsetAndRotation(0, 4.2f, 0, -1.35f, 0, 0));
        PartDefinition rightFoot = rightAnkle.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(54, 34).mirror(false).addBox(-1.4f, -0.4f, -3, 2.8f, 1.2f, 3.6f)
                .texOffs(9, 49).mirror(false).addBox(-1.45f, 0, -4, 0.7f, 0.8f, 1.2f)
                .texOffs(9, 49).mirror(false).addBox(-0.35f, 0, -4.2f, 0.7f, 0.8f, 1.4f)
                .texOffs(9, 49).mirror(false).addBox(0.75f, 0, -4, 0.7f, 0.8f, 1.2f), PartPose.offsetAndRotation(0, 3.4f, 0, 0.62f, 0, 0));
        PartDefinition leftFoot = leftAnkle.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(54, 34).mirror(true).addBox(-1.4f, -0.4f, -3, 2.8f, 1.2f, 3.6f)
                .texOffs(9, 49).mirror(true).addBox(0.75f, 0, -4, 0.7f, 0.8f, 1.2f)
                .texOffs(9, 49).mirror(true).addBox(-0.35f, 0, -4.2f, 0.7f, 0.8f, 1.4f)
                .texOffs(9, 49).mirror(true).addBox(-1.45f, 0, -4, 0.7f, 0.8f, 1.2f), PartPose.offsetAndRotation(0, 3.4f, 0, 0.62f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(46, 14).mirror(false).addBox(-3, -2.6f, -2.5f, 6, 3.4f, 5)
                .texOffs(10, 0).mirror(false).addBox(-3.2f, -9, -2.6f, 6.4f, 6.8f, 5)
                .texOffs(86, 0).mirror(false).addBox(-3.6f, -9.5f, -2.95f, 7.2f, 3.3f, 5.7f)
                .texOffs(13, 25).mirror(false).addBox(-3.5f, -3.3f, -2.95f, 7, 1.2f, 5.6f)
                .texOffs(62, 43).mirror(false).addBox(1.6f, -3, -3.4f, 1.8f, 2, 1.2f)
                .texOffs(0, 43).mirror(false).addBox(-1.3f, -10.6f, -1.6f, 2.6f, 2, 2.8f)
                .texOffs(16, 49).mirror(false).addBox(-2.8369f, -8.7173f, -3.05f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(-2.1825f, -7.8761f, -3.05f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(-1.1705f, -7.4019f, -3.05f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(-0.0295f, -7.4019f, -3.05f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(0.9825f, -7.8761f, -3.05f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(1.6369f, -8.7173f, -3.05f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(-3.3961f, -8.6216f, -3.15f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(-2.8679f, -7.8326f, -3.15f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(-2.0757f, -7.2493f, -3.15f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(-1.1118f, -6.9396f, -3.15f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(-0.0882f, -6.9396f, -3.15f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(0.8757f, -7.2493f, -3.15f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(1.6679f, -7.8326f, -3.15f, 1.2f, 1.2f, 0.3f)
                .texOffs(16, 49).mirror(false).addBox(2.1961f, -8.6216f, -3.15f, 1.2f, 1.2f, 0.3f), PartPose.offsetAndRotation(0, 14.3215f, 1, 0.28f, 0, 0));
        PartDefinition rightTasset = body.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(69, 34).mirror(false).addBox(-1.4f, 0, -0.3f, 2.8f, 4.2f, 0.6f), PartPose.offsetAndRotation(-1.6f, -2.3f, -2.7f, -0.08f, 0.1f, 0.05f));
        PartDefinition leftTasset = body.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(69, 34).mirror(true).addBox(-1.4f, 0, -0.3f, 2.8f, 4.2f, 0.6f), PartPose.offsetAndRotation(1.6f, -2.3f, -2.7f, -0.08f, -0.1f, -0.05f));
        PartDefinition rightTassetSide = body.addOrReplaceChild("right_tasset_side", CubeListBuilder.create()
                .texOffs(40, 25).mirror(false).addBox(-0.4f, 0, -2, 0.6f, 3.6f, 4), PartPose.offsetAndRotation(-3, -2.3f, 0, 0, 0, 0.12f));
        PartDefinition leftTassetSide = body.addOrReplaceChild("left_tasset_side", CubeListBuilder.create()
                .texOffs(40, 25).mirror(true).addBox(-0.2f, 0, -2, 0.6f, 3.6f, 4), PartPose.offsetAndRotation(3, -2.3f, 0, 0, 0, -0.12f));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(51, 25).mirror(false).addBox(-1.5f, -1.5f, 0, 3, 3, 4.6f)
                .texOffs(23, 49).mirror(false).addBox(-0.3f, -2.4f, 0.6f, 0.6f, 1, 1.4f)
                .texOffs(23, 49).mirror(false).addBox(-0.3f, -2.3f, 2.6f, 0.6f, 0.9f, 1.2f), PartPose.offsetAndRotation(0, -1, 2.2f, -0.7f, 0, 0));
        PartDefinition tailMid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create()
                .texOffs(68, 25).mirror(false).addBox(-1.1f, -1.1f, 0, 2.2f, 2.2f, 4.4f)
                .texOffs(30, 49).mirror(false).addBox(-0.25f, -1.9f, 0.8f, 0.5f, 0.9f, 1.2f)
                .texOffs(119, 49).mirror(false).addBox(-0.25f, -1.8f, 2.6f, 0.5f, 0.8f, 1), PartPose.offsetAndRotation(0, 0, 4.3f, -0.35f, 0.1f, 0));
        PartDefinition tailTip = tailMid.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(78, 34).mirror(false).addBox(-0.7f, -0.7f, 0, 1.4f, 1.4f, 4)
                .texOffs(119, 49).mirror(false).addBox(-0.2f, -1.3f, 1, 0.4f, 0.7f, 1), PartPose.offsetAndRotation(0, 0, 4.2f, 0.25f, 0.15f, 0));
        PartDefinition frame = body.addOrReplaceChild("frame", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-3, -3, 0, 0.8f, 12, 0.8f)
                .texOffs(0, 0).mirror(false).addBox(2.2f, -3, 0, 0.8f, 12, 0.8f)
                .texOffs(0, 54).mirror(false).addBox(-3, -2.4f, 0.05f, 6, 0.7f, 0.7f)
                .texOffs(0, 54).mirror(false).addBox(-3, 7.6f, 0.05f, 6, 0.7f, 0.7f)
                .texOffs(40, 0).mirror(false).addBox(-2.8f, -1.4f, 0.8f, 5.6f, 6.2f, 3.6f)
                .texOffs(71, 43).mirror(false).addBox(-2.2f, -2.6f, 1.2f, 1.6f, 1.4f, 1.6f)
                .texOffs(80, 43).mirror(false).addBox(-0.5f, -2.9f, 1.8f, 1.4f, 1.6f, 1.4f)
                .texOffs(89, 43).mirror(false).addBox(0.9f, -2.4f, 1, 1.6f, 1.2f, 1.8f)
                .texOffs(37, 49).mirror(false).addBox(-1.9f, -2, 2.8f, 1.3f, 0.9f, 1.2f), PartPose.offsetAndRotation(0, -9.6f, 2.6f, 0.08f, 0, 0));
        PartDefinition pole = frame.addOrReplaceChild("pole", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-0.35f, -11, -0.35f, 0.7f, 11.4f, 0.7f), PartPose.offsetAndRotation(-2.6f, -2.6f, 0.4f, 0.5f, 0, -0.42f));
        PartDefinition poleHook = pole.addOrReplaceChild("pole_hook", CubeListBuilder.create()
                .texOffs(46, 49).mirror(false).addBox(-0.25f, -0.2f, -1.8f, 0.5f, 0.5f, 2), PartPose.offsetAndRotation(0, -10.8f, 0, -0.5f, 0, 0.42f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(61, 0).mirror(false).addBox(-3, -5, -3, 6, 5, 5.6f)
                .texOffs(69, 14).mirror(false).addBox(-2, -2.9f, -8.2f, 4, 2.5f, 5.4f)
                .texOffs(91, 34).mirror(false).addBox(-1, -3.5f, -7.8f, 2, 0.8f, 4.6f)
                .texOffs(15, 54).mirror(false).addBox(-0.7f, -2.9f, -8.6f, 1.4f, 1, 0.5f)
                .texOffs(98, 43).mirror(false).addBox(-1.2f, -0.6f, 1.8f, 2.4f, 1.6f, 1.4f), PartPose.offsetAndRotation(0, -10.4f, -1, -0.3f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(85, 25).mirror(false).addBox(-1.9f, 0, -5.5f, 3.8f, 1.3f, 5.4f), PartPose.offsetAndRotation(0, -0.5f, -2.6f, 0.18f, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(53, 49).mirror(false).addBox(-1, -0.7f, -0.35f, 2, 1.4f, 0.5f), PartPose.offsetAndRotation(-2.3f, -3.25f, -2.9f, 0, 0.35f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(53, 49).mirror(true).addBox(-1, -0.7f, -0.35f, 2, 1.4f, 0.5f), PartPose.offsetAndRotation(2.3f, -3.25f, -2.9f, 0, -0.35f, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(60, 49).mirror(false).addBox(-1.3f, -0.45f, -0.7f, 2.6f, 0.9f, 1.4f), PartPose.offsetAndRotation(-1.7f, -4.25f, -3.25f, 0, 0.2f, -0.32f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(60, 49).mirror(true).addBox(-1.3f, -0.45f, -0.7f, 2.6f, 0.9f, 1.4f), PartPose.offsetAndRotation(1.7f, -4.25f, -3.25f, 0, -0.2f, 0.32f));
        PartDefinition rightFrill = head.addOrReplaceChild("right_frill", CubeListBuilder.create()
                .texOffs(13, 43).mirror(false).addBox(-3.8f, -1.8f, -0.2f, 3.8f, 3.6f, 0.4f), PartPose.offsetAndRotation(-2.6f, -2.2f, -0.2f, 0.1f, 0.7f, 0.35f));
        PartDefinition leftFrill = head.addOrReplaceChild("left_frill", CubeListBuilder.create()
                .texOffs(13, 43).mirror(true).addBox(0, -1.8f, -0.2f, 3.8f, 3.6f, 0.4f), PartPose.offsetAndRotation(2.6f, -2.2f, -0.2f, 0.1f, -0.7f, -0.35f));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(24, 43).mirror(false).addBox(-0.5f, -3.2f, -0.5f, 1, 3.4f, 1), PartPose.offsetAndRotation(-1.7f, -4.3f, 0.2f, -1.2f, 0, -0.3f));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(24, 43).mirror(true).addBox(-0.5f, -3.2f, -0.5f, 1, 3.4f, 1), PartPose.offsetAndRotation(1.7f, -4.3f, 0.2f, -1.2f, 0, 0.3f));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(109, 43).mirror(false).addBox(-0.2f, -1.4f, -0.6f, 0.4f, 1.6f, 1.2f)
                .texOffs(109, 43).mirror(false).addBox(-0.2f, -1.1f, 0.8f, 0.4f, 1.3f, 1.1f)
                .texOffs(22, 54).mirror(false).addBox(-0.2f, -0.8f, 2, 0.4f, 1, 1), PartPose.offsetAndRotation(0, -4.4f, 1.4f, 0.3f, 0, 0));
        PartDefinition helmet = head.addOrReplaceChild("helmet", CubeListBuilder.create()
                .texOffs(0, 14).mirror(false).addBox(-3.1f, -2.6f, -3.1f, 6.2f, 2.8f, 6.2f)
                .texOffs(90, 14).mirror(false).addBox(-3.6f, -0.1f, -3.6f, 7.2f, 0.6f, 7.2f)
                .texOffs(71, 49).mirror(false).addBox(-0.6f, -3.3f, -0.6f, 1.2f, 0.8f, 1.2f)
                .texOffs(80, 49).mirror(false).addBox(-0.45f, -4.9f, -0.45f, 0.9f, 1.7f, 0.9f), PartPose.offsetAndRotation(0, -4.7f, -0.1f, 0.12f, 0, 0.14f));
        PartDefinition flame = helmet.addOrReplaceChild("flame", CubeListBuilder.create()
                .texOffs(85, 49).mirror(false).addBox(-0.4f, -1.3f, -0.05f, 0.8f, 1.3f, 0.1f)
                .texOffs(90, 49).mirror(false).addBox(-0.05f, -1.3f, -0.4f, 0.1f, 1.3f, 0.8f), PartPose.offsetAndRotation(0, -4.9f, 0, 0, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(106, 25).mirror(false).addBox(-1.2f, -0.6f, -1.2f, 2.4f, 4.2f, 2.4f)
                .texOffs(17, 34).mirror(false).addBox(-1.9f, -1.5f, -1.8f, 2.9f, 2.3f, 3.6f), PartPose.offsetAndRotation(-3.6f, -8.2f, -0.4f, -0.75f, 0, 0.12f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(106, 25).mirror(true).addBox(-1.2f, -0.6f, -1.2f, 2.4f, 4.2f, 2.4f)
                .texOffs(17, 34).mirror(true).addBox(-1, -1.5f, -1.8f, 2.9f, 2.3f, 3.6f), PartPose.offsetAndRotation(3.6f, -8.2f, -0.4f, -0.75f, 0, -0.12f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(106, 34).mirror(false).addBox(-1, 0, -1, 2, 4, 2)
                .texOffs(115, 34).mirror(false).addBox(-1.25f, 1.2f, -1.25f, 2.5f, 2.4f, 2.5f), PartPose.offsetAndRotation(-0.1f, 3.8f, 0, -0.95f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(106, 34).mirror(true).addBox(-1, 0, -1, 2, 4, 2)
                .texOffs(115, 34).mirror(true).addBox(-1.25f, 1.2f, -1.25f, 2.5f, 2.4f, 2.5f), PartPose.offsetAndRotation(0.1f, 3.8f, 0, -0.95f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(29, 43).mirror(false).addBox(-1.1f, 0, -1.2f, 2.2f, 1.6f, 2.4f)
                .texOffs(95, 49).mirror(false).addBox(-1.05f, 1.4f, -1.1f, 0.6f, 1.2f, 0.6f)
                .texOffs(95, 49).mirror(false).addBox(-0.3f, 1.4f, -1.1f, 0.6f, 1.3f, 0.6f)
                .texOffs(95, 49).mirror(false).addBox(0.45f, 1.4f, -1.1f, 0.6f, 1.2f, 0.6f), PartPose.offsetAndRotation(0, 4, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(29, 43).mirror(true).addBox(-1.1f, 0, -1.2f, 2.2f, 1.6f, 2.4f)
                .texOffs(95, 49).mirror(true).addBox(0.45f, 1.4f, -1.1f, 0.6f, 1.2f, 0.6f)
                .texOffs(95, 49).mirror(true).addBox(-0.3f, 1.4f, -1.1f, 0.6f, 1.3f, 0.6f)
                .texOffs(95, 49).mirror(true).addBox(-1.05f, 1.4f, -1.1f, 0.6f, 1.2f, 0.6f), PartPose.offsetAndRotation(0, 4, 0, 0, 0, 0));
        PartDefinition pick = rightHand.addOrReplaceChild("pick", CubeListBuilder.create()
                .texOffs(35, 0).mirror(false).addBox(-0.4f, -9, -0.4f, 0.8f, 10.4f, 0.8f)
                .texOffs(116, 43).mirror(false).addBox(-0.55f, -9.4f, -0.55f, 1.1f, 1.6f, 1.1f)
                .texOffs(32, 34).mirror(false).addBox(-0.5f, -9.2f, -4.6f, 1, 1.1f, 4.1f)
                .texOffs(42, 43).mirror(false).addBox(-0.6f, -9.3f, 0.5f, 1.2f, 1.3f, 2.4f), PartPose.offsetAndRotation(0, 0.9f, -0.1f, 1.0709f, 0.119f, 0.0155f));
        PartDefinition jade = leftHand.addOrReplaceChild("jade", CubeListBuilder.create()
                .texOffs(0, 49).mirror(false).addBox(-0.9f, -0.6f, -0.9f, 1.8f, 2, 1.8f), PartPose.offsetAndRotation(0, 1.6f, -0.3f, 0.3f, 0.4f, 0.2f));
        PartDefinition lantern = poleHook.addOrReplaceChild("lantern", CubeListBuilder.create()
                .texOffs(100, 49).mirror(false).addBox(-0.1f, 0, -0.1f, 0.2f, 1.4f, 0.2f)
                .texOffs(105, 49).mirror(false).addBox(-1, 1.3f, -1, 2, 0.5f, 2)
                .texOffs(0, 34).mirror(false).addBox(-1.6f, 1.8f, -1.6f, 3.2f, 3.6f, 3.2f)
                .texOffs(105, 49).mirror(false).addBox(-1, 5.4f, -1, 2, 0.5f, 2)
                .texOffs(114, 49).mirror(false).addBox(-0.3f, 5.9f, -0.3f, 0.6f, 2, 0.6f), PartPose.offsetAndRotation(0, 0.1f, -1.6f, -0.3128f, 0.1946f, 0.0548f));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED kobold: end
    }

    /**
     * The kobold's pose: crouched on its long-footed legs, the pick up in its right fist, the jade in
     * its left, its tail dragging behind; walking, a quick waddle, the tail swaying, the lantern
     * swinging on its pole; a blow, the pick raised and brought down. Its candle flickers, its frills
     * flutter.
     */
    public static final class KoboldModel extends EntityModel<KoboldEntity> {
        private final ModelPart root, body, head, jaw, rightFrill, leftFrill, rightArm, leftArm, rightForearm,
                rightLeg, leftLeg, rightShin, leftShin, tail, tailMid, tailTip, pole, lantern, flame;

        public KoboldModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightFrill = head.getChild("right_frill");
            leftFrill = head.getChild("left_frill");
            flame = head.getChild("helmet").getChild("flame");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            tail = body.getChild("tail");
            tailMid = tail.getChild("tail_mid");
            tailTip = tailMid.getChild("tail_tip");
            pole = body.getChild("frame").getChild("pole");
            lantern = pole.getChild("pole_hook").getChild("lantern");
        }

        @Override
        public void setupAnim(KoboldEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float stride = Mth.cos(swing * 0.85f) * 0.9f * amount;
            float bob = Math.abs(Mth.sin(swing * 0.85f)) * amount;
            float breath = Mth.sin(age * 0.08f);
            // the pick raised, then brought down
            float chop = 0;
            if (attackTime > 0) chop = attackTime < 0.4f ? -1.4f * attackTime / 0.4f : -1.4f + 2.6f * (attackTime - 0.4f) / 0.6f;
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            turn(rightShin, Math.max(0, -stride) * 0.5f, 0, 0);
            turn(leftShin, Math.max(0, stride) * 0.5f, 0, 0);
            float lean = bob * 0.06f + breath * 0.02f;
            turn(body, lean, 0, stride * 0.08f);
            body.y = body.getInitialPose().y + bob * 0.5f;
            turn(head, pitch * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, -stride * 0.06f);
            turn(jaw, Math.max(0, Mth.sin(age * 0.11f)) * 0.1f + (attackTime > 0 ? 0.3f : 0), 0, 0);
            turn(rightFrill, 0, Mth.sin(age * 0.2f) * 0.08f, 0);
            turn(leftFrill, 0, -Mth.sin(age * 0.2f + 0.6f) * 0.08f, 0);
            turn(rightArm, -stride * 0.4f + chop, 0, 0);
            turn(rightForearm, chop < 0 ? chop * -0.3f : 0, 0, 0);
            turn(leftArm, stride * 0.5f, 0, 0);
            // the tail sways side to side as it walks, and twitches at rest
            float sway = Mth.sin(swing * 0.425f) * 0.25f * amount + Mth.sin(age * 0.05f) * 0.06f;
            turn(tail, -bob * 0.1f, sway, 0);
            turn(tailMid, 0, sway * 0.8f, 0);
            turn(tailTip, 0, sway * 0.8f + Mth.sin(age * 0.09f) * 0.1f, 0);
            // the pole bends a little, the lantern swings under it, kept hanging
            turn(pole, bob * 0.05f, 0, stride * 0.05f);
            turn(lantern, -lean - bob * 0.05f + Mth.sin(age * 0.07f) * 0.08f + stride * 0.12f, 0, -stride * 0.13f + Mth.cos(age * 0.06f) * 0.06f);
            // the candle's flame flickers
            flame.yScale = 1 + Mth.sin(age * 0.9f) * 0.12f + Mth.sin(age * 1.7f) * 0.06f;
            flame.xScale = flame.zScale = 1 - Mth.sin(age * 0.9f) * 0.08f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class KoboldRenderer extends MobRenderer<KoboldEntity, KoboldModel> {
        /** How much larger than its pixels it is drawn: some 1.2 blocks to its candle (kobold.py prints it). */
        public static final float SIZE = 0.68f;
        private static final ResourceLocation SKIN = tex("kobold");

        public KoboldRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new KoboldModel(ctx.bakeLayer(KOBOLD_LAYER)), 0.4f);
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
                raise = w > 3 ? Math.min(1, (OgreEntity.SLAM_WINDUP - w) / 8f) : w / 3f;
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

    public static final class OgreRenderer extends MobRenderer<OgreEntity, OgreModel> {
        /** How much larger than its pixels it is drawn: some 3.2 blocks to its horns' tips (ogre.py prints it). */
        public static final float SIZE = 0.95f;
        private static final ResourceLocation SKIN = tex("ogre");

        public OgreRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new OgreModel(ctx.bakeLayer(OGRE_LAYER)), 1.2f);
            addLayer(LionCourt.glow(this, "ogre"));
        }

        @Override
        protected void scale(OgreEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        protected boolean shouldShowName(OgreEntity e) {
            return super.shouldShowName(e) && near(entityRenderDispatcher, e);
        }

        @Override
        public ResourceLocation getTextureLocation(OgreEntity e) {
            return SKIN;
        }
    }
}
