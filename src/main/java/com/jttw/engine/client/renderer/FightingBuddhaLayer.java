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
 * The Victorious Fighting Buddha's look, drawn in place of Sun Wukong's own body once he is ascended
 * (FightingBuddhaClient; his body, armour and cape are not drawn then): the Monkey King's golden-furred
 * head with its heart-shaped face and fiery golden eyes, the jewelled crown the fillet has become with
 * its ribbons, the crimson robe and the patched kasaya over it with its folds, cord, apron and panels,
 * the prayer beads, the Ruyi Jingu Bang in red and gold while he holds it, and behind him the halo and
 * the mandorla of golden light and flame, drawn full-bright. A little larger than a man. Its model and
 * skins come from assets_source/fighting_buddha.py; each piece hangs from a copy of the player's head,
 * body, arms or legs, posed as the player is each frame.
 */
public class FightingBuddhaLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("fighting_buddha"), "main");
    private static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/fighting_buddha.png");
    private static final ResourceLocation GLOW = JTTWEngine.id("textures/entity/fighting_buddha_glow.png");
    /** The head, the Monkey King's face, is its own model, drawn at twice the body's detail. */
    public static final ModelLayerLocation HEAD_LAYER = new ModelLayerLocation(JTTWEngine.id("fighting_buddha"), "head");
    private static final ResourceLocation HEAD_TEXTURE = JTTWEngine.id("textures/entity/fighting_buddha_head.png");
    private static final ResourceLocation HEAD_GLOW = JTTWEngine.id("textures/entity/fighting_buddha_head_glow.png");
    /** How much larger than a man he is drawn, about his feet. */
    public static final float SIZE = 1.1f;

    private final ModelPart headRoot, monkeyHead, root, head, body, rightArm, leftArm, rightLeg, leftLeg, halo, mandorla, staff, staffRest;

    public FightingBuddhaLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        root = models.bakeLayer(LAYER);
        headRoot = models.bakeLayer(HEAD_LAYER);
        monkeyHead = headRoot.getChild("head");
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        halo = body.getChild("halo");
        mandorla = body.getChild("mandorla");
        staff = rightArm.getChild("right_hand").getChild("staff");
        staffRest = body.getChild("staff_rest");
    }

    /** Whether he holds the Ruyi Jingu Bang in his hand now (not at rest across his shoulders): this draws it then. */
    public static boolean holdsStaff(AbstractClientPlayer p) {
        return p.getMainHandItem().getItem() instanceof com.jttw.engine.item.RuyiJinguBangItem
                && !com.jttw.engine.client.StaffStance.resting(p);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p,
                       float swing, float swingAmount, float partial, float age, float headYaw, float headPitch) {
        if (p.isInvisible() || !FightingBuddhaClient.ascended(p)) return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        head.copyFrom(m.head);
        monkeyHead.copyFrom(m.head);
        body.copyFrom(m.body);
        rightArm.copyFrom(m.rightArm);
        leftArm.copyFrom(m.leftArm);
        rightLeg.copyFrom(m.rightLeg);
        leftLeg.copyFrom(m.leftLeg);
        staff.visible = holdsStaff(p);
        staffRest.visible = p.getMainHandItem().getItem() instanceof com.jttw.engine.item.RuyiJinguBangItem
                && com.jttw.engine.client.StaffStance.resting(p);
        // the halo turns slowly; the mandorla breathes
        halo.zRot = age * 0.01f;
        float breath = 1.0f + 0.03f * (float) Math.sin(age * 0.07f);
        mandorla.xScale = mandorla.yScale = breath;
        pose.pushPose();
        pose.translate(0, 1.5f, 0);
        pose.scale(SIZE, SIZE, SIZE);
        pose.translate(0, -1.5f, 0);
        root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        root.render(pose, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF000F0, OverlayTexture.NO_OVERLAY);
        headRoot.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(HEAD_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        headRoot.render(pose, buffers.getBuffer(RenderType.eyes(HEAD_GLOW)), 0xF000F0, OverlayTexture.NO_OVERLAY);
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
        PartDefinition circlet = head.addOrReplaceChild("circlet", CubeListBuilder.create()
                .texOffs(52, 39).mirror(false).addBox(-4.95f, -8.4f, -4.75f, 9.9f, 1.5f, 9.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightRibbon = head.addOrReplaceChild("right_ribbon", CubeListBuilder.create()
                .texOffs(183, 39).mirror(false).addBox(-0.75f, 0, -0.15f, 1.5f, 6.5f, 0.3f), PartPose.offsetAndRotation(-4.9f, -7.6f, 2.2f, 0.45f, 0.25f, 0.5f));
        PartDefinition leftRibbon = head.addOrReplaceChild("left_ribbon", CubeListBuilder.create()
                .texOffs(183, 39).mirror(true).addBox(-0.75f, 0, -0.15f, 1.5f, 6.5f, 0.3f), PartPose.offsetAndRotation(4.9f, -7.6f, 2.2f, 0.45f, -0.25f, -0.5f));
        PartDefinition rightRibbonEnd = rightRibbon.addOrReplaceChild("right_ribbon_end", CubeListBuilder.create()
                .texOffs(237, 39).mirror(false).addBox(-0.7f, 0, -0.15f, 1.4f, 6, 0.3f), PartPose.offsetAndRotation(0, 6.3f, 0, 0.25f, 0, 0.4f));
        PartDefinition leftRibbonEnd = leftRibbon.addOrReplaceChild("left_ribbon_end", CubeListBuilder.create()
                .texOffs(237, 39).mirror(true).addBox(-0.7f, 0, -0.15f, 1.4f, 6, 0.3f), PartPose.offsetAndRotation(0, 6.3f, 0, 0.25f, 0, -0.4f));
        PartDefinition rightRibbonB = head.addOrReplaceChild("right_ribbon_b", CubeListBuilder.create()
                .texOffs(183, 39).mirror(false).addBox(-0.65f, 0, -0.15f, 1.3f, 7, 0.3f), PartPose.offsetAndRotation(-4.7f, -7.2f, 3.4f, 0.75f, 0.1f, 0.22f));
        PartDefinition leftRibbonB = head.addOrReplaceChild("left_ribbon_b", CubeListBuilder.create()
                .texOffs(183, 39).mirror(true).addBox(-0.65f, 0, -0.15f, 1.3f, 7, 0.3f), PartPose.offsetAndRotation(4.7f, -7.2f, 3.4f, 0.75f, -0.1f, -0.22f));
        PartDefinition rightRibbonBEnd = rightRibbonB.addOrReplaceChild("right_ribbon_b_end", CubeListBuilder.create()
                .texOffs(98, 53).mirror(false).addBox(-0.6f, 0, -0.15f, 1.2f, 5, 0.3f), PartPose.offsetAndRotation(0, 6.8f, 0, 0.3f, 0, 0.25f));
        PartDefinition leftRibbonBEnd = leftRibbonB.addOrReplaceChild("left_ribbon_b_end", CubeListBuilder.create()
                .texOffs(98, 53).mirror(true).addBox(-0.6f, 0, -0.15f, 1.2f, 5, 0.3f), PartPose.offsetAndRotation(0, 6.8f, 0, 0.3f, 0, -0.25f));
        PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(125, 0).mirror(false).addBox(-4.85f, -0.15f, -2.95f, 9.7f, 11, 5.9f)
                .texOffs(0, 53).mirror(false).addBox(-2.3f, -0.9f, -2.3f, 4.6f, 1.2f, 4.6f)
                .texOffs(141, 39).mirror(false).addBox(-5.1f, 9.6f, -3.2f, 10.2f, 1.4f, 6.4f)
                .texOffs(245, 53).mirror(false).addBox(-1, 9.3f, -3.8f, 2, 2, 0.9f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition edgeFront = body.addOrReplaceChild("edge_front", CubeListBuilder.create()
                .texOffs(118, 0).mirror(false).addBox(-0.95f, -8.8f, -0.5f, 1.9f, 17, 1), PartPose.offsetAndRotation(0.2f, 5, -3.05f, 0, 0, -0.66f));
        PartDefinition edgeBack = body.addOrReplaceChild("edge_back", CubeListBuilder.create()
                .texOffs(118, 0).mirror(false).addBox(-0.95f, -8.8f, -0.5f, 1.9f, 17, 1), PartPose.offsetAndRotation(0.2f, 5, 3.05f, 0, 0, -0.66f));
        PartDefinition fold0 = body.addOrReplaceChild("fold_0", CubeListBuilder.create()
                .texOffs(178, 39).mirror(false).addBox(-0.4f, -4.4f, -0.35f, 0.8f, 8, 0.7f), PartPose.offsetAndRotation(1.9f, 7.2f, -3, 0, 0, -0.5f));
        PartDefinition fold1 = body.addOrReplaceChild("fold_1", CubeListBuilder.create()
                .texOffs(21, 53).mirror(false).addBox(-0.4f, -3.2f, -0.35f, 0.8f, 5.6f, 0.7f), PartPose.offsetAndRotation(3.4f, 8.6f, -3, 0, 0, -0.38f));
        PartDefinition foldBack = body.addOrReplaceChild("fold_back", CubeListBuilder.create()
                .texOffs(190, 39).mirror(false).addBox(-0.4f, -4.2f, -0.35f, 0.8f, 7, 0.7f), PartPose.offsetAndRotation(2.4f, 7.8f, 3, 0, 0, -0.45f));
        PartDefinition clasp = body.addOrReplaceChild("clasp", CubeListBuilder.create()
                .texOffs(25, 61).mirror(false).addBox(-1.1f, -1.1f, -0.3f, 2.2f, 0.5f, 0.6f)
                .texOffs(25, 61).mirror(false).addBox(-1.1f, 0.6f, -0.3f, 2.2f, 0.5f, 0.6f)
                .texOffs(252, 53).mirror(false).addBox(-1.1f, -0.6f, -0.3f, 0.5f, 1.2f, 0.6f)
                .texOffs(252, 53).mirror(false).addBox(0.6f, -0.6f, -0.3f, 0.5f, 1.2f, 0.6f)
                .texOffs(252, 53).mirror(false).addBox(-0.25f, 1, -0.25f, 0.5f, 1.4f, 0.5f), PartPose.offsetAndRotation(2.7f, 2.4f, -3.35f, 0, 0, 0));
        PartDefinition tasselRight = body.addOrReplaceChild("tassel_right", CubeListBuilder.create()
                .texOffs(105, 53).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 5, 0.6f), PartPose.offsetAndRotation(-0.5f, 10.9f, -3.6f, -0.05f, 0, 0.06f));
        PartDefinition tasselLeft = body.addOrReplaceChild("tassel_left", CubeListBuilder.create()
                .texOffs(105, 53).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 4.2f, 0.6f), PartPose.offsetAndRotation(0.55f, 10.9f, -3.6f, -0.05f, 0, -0.06f));
        PartDefinition apron = body.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(93, 39).mirror(false).addBox(-3.6f, 0, -0.45f, 7.2f, 10.8f, 0.9f), PartPose.offsetAndRotation(0, 10.9f, -3.1f, -0.07f, 0, 0));
        PartDefinition backPanel = body.addOrReplaceChild("back_panel", CubeListBuilder.create()
                .texOffs(198, 0).mirror(false).addBox(-4.6f, 0, -0.45f, 9.2f, 11.4f, 0.9f), PartPose.offsetAndRotation(0, 10.9f, 3.1f, 0.09f, 0, 0));
        PartDefinition rightSidePanel = body.addOrReplaceChild("right_side_panel", CubeListBuilder.create()
                .texOffs(158, 0).mirror(false).addBox(-0.45f, 0, -2.8f, 0.9f, 10.2f, 5.6f), PartPose.offsetAndRotation(-4.9f, 10.9f, 0, 0, 0, 0.1f));
        PartDefinition leftSidePanel = body.addOrReplaceChild("left_side_panel", CubeListBuilder.create()
                .texOffs(158, 0).mirror(true).addBox(-0.45f, 0, -2.8f, 0.9f, 10.2f, 5.6f), PartPose.offsetAndRotation(4.9f, 10.9f, 0, 0, 0, -0.1f));
        PartDefinition rightSleeveUpper = rightArm.addOrReplaceChild("right_sleeve_upper", CubeListBuilder.create()
                .texOffs(221, 0).mirror(false).addBox(-3.6f, -2.5f, -2.55f, 5.1f, 6.6f, 5.1f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition leftSleeveUpper = leftArm.addOrReplaceChild("left_sleeve_upper", CubeListBuilder.create()
                .texOffs(221, 0).mirror(true).addBox(-1.5f, -2.5f, -2.55f, 5.1f, 6.6f, 5.1f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition armlet = rightArm.addOrReplaceChild("armlet", CubeListBuilder.create()
                .texOffs(26, 53).mirror(false).addBox(-3.95f, 0.4f, -2.85f, 5.8f, 1, 5.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightSleeveWide = rightArm.addOrReplaceChild("right_sleeve_wide", CubeListBuilder.create()
                .texOffs(0, 39).mirror(false).addBox(-2.9f, 0, -3.3f, 5.8f, 6, 6.6f)
                .texOffs(195, 39).mirror(false).addBox(-3.05f, 5.4f, -3.45f, 6.1f, 0.9f, 6.9f), PartPose.offsetAndRotation(-1, 3.8f, 0, 0.08f, 0, 0.05f));
        PartDefinition leftSleeveWide = leftArm.addOrReplaceChild("left_sleeve_wide", CubeListBuilder.create()
                .texOffs(0, 39).mirror(true).addBox(-2.9f, 0, -3.3f, 5.8f, 6, 6.6f)
                .texOffs(195, 39).mirror(true).addBox(-3.05f, 5.4f, -3.45f, 6.1f, 0.9f, 6.9f), PartPose.offsetAndRotation(1, 3.8f, 0, 0.08f, 0, -0.05f));
        PartDefinition rightHand = rightArm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(51, 53).mirror(false).addBox(-1.8f, 0, -1.9f, 3.6f, 2.8f, 3.8f)
                .texOffs(110, 53).mirror(false).addBox(-2.05f, -0.5f, -2.15f, 4.1f, 0.8f, 4.3f), PartPose.offsetAndRotation(-1, 8, 0, 0, 0, 0));
        PartDefinition leftHand = leftArm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(51, 53).mirror(true).addBox(-1.8f, 0, -1.9f, 3.6f, 2.8f, 3.8f)
                .texOffs(110, 53).mirror(true).addBox(-2.05f, -0.5f, -2.15f, 4.1f, 0.8f, 4.3f), PartPose.offsetAndRotation(1, 8, 0, 0, 0, 0));
        PartDefinition leftKasayaSleeve = leftArm.addOrReplaceChild("left_kasaya_sleeve", CubeListBuilder.create()
                .texOffs(173, 0).mirror(false).addBox(-1.55f, -2.7f, -2.85f, 5.3f, 7.2f, 5.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition leftDrape = leftArm.addOrReplaceChild("left_drape", CubeListBuilder.create()
                .texOffs(64, 0).mirror(false).addBox(0, 0, -2.9f, 0.7f, 13.5f, 5.8f), PartPose.offsetAndRotation(3.6f, -1.8f, 0, 0, 0, -0.1f));
        PartDefinition staff = rightHand.addOrReplaceChild("staff", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.65f, -23, -0.65f, 1.3f, 36, 1.3f)
                .texOffs(162, 53).mirror(false).addBox(-1, -25.2f, -1, 2, 2.6f, 2)
                .texOffs(162, 53).mirror(false).addBox(-1, 12.6f, -1, 2, 2.6f, 2)
                .texOffs(0, 61).mirror(false).addBox(-0.85f, -21.4f, -0.85f, 1.7f, 0.8f, 1.7f)
                .texOffs(0, 61).mirror(false).addBox(-0.85f, 10.6f, -0.85f, 1.7f, 0.8f, 1.7f)
                .texOffs(0, 61).mirror(false).addBox(-0.8f, -1.4f, -0.8f, 1.6f, 0.5f, 1.6f)
                .texOffs(0, 61).mirror(false).addBox(-0.8f, 3, -0.8f, 1.6f, 0.5f, 1.6f), PartPose.offsetAndRotation(0, 1.4f, -0.2f, 0.22f, 0, 0.05f));
        PartDefinition staffRest = body.addOrReplaceChild("staff_rest", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.65f, -23, -0.65f, 1.3f, 36, 1.3f)
                .texOffs(162, 53).mirror(false).addBox(-1, -25.2f, -1, 2, 2.6f, 2)
                .texOffs(162, 53).mirror(false).addBox(-1, 12.6f, -1, 2, 2.6f, 2)
                .texOffs(0, 61).mirror(false).addBox(-0.85f, -21.4f, -0.85f, 1.7f, 0.8f, 1.7f)
                .texOffs(0, 61).mirror(false).addBox(-0.85f, 10.6f, -0.85f, 1.7f, 0.8f, 1.7f), PartPose.offsetAndRotation(-5, -0.5f, 0.6f, -0.785f, 0, -0.21f));
        PartDefinition rightRobeLeg = rightLeg.addOrReplaceChild("right_robe_leg", CubeListBuilder.create()
                .texOffs(27, 39).mirror(false).addBox(-2.65f, -0.2f, -2.65f, 5.3f, 6.4f, 5.3f)
                .texOffs(112, 39).mirror(false).addBox(-3.1f, 5.8f, -3.1f, 6.2f, 4.7f, 6.2f)
                .texOffs(68, 53).mirror(false).addBox(-2.05f, 10.3f, -3.2f, 4.1f, 1.7f, 5)
                .texOffs(191, 53).mirror(false).addBox(-1.2f, 9.6f, -3.9f, 2.4f, 1.1f, 1.1f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition leftRobeLeg = leftLeg.addOrReplaceChild("left_robe_leg", CubeListBuilder.create()
                .texOffs(27, 39).mirror(true).addBox(-2.65f, -0.2f, -2.65f, 5.3f, 6.4f, 5.3f)
                .texOffs(112, 39).mirror(true).addBox(-3.1f, 5.8f, -3.1f, 6.2f, 4.7f, 6.2f)
                .texOffs(68, 53).mirror(true).addBox(-2.05f, 10.3f, -3.2f, 4.1f, 1.7f, 5)
                .texOffs(191, 53).mirror(true).addBox(-1.2f, 9.6f, -3.9f, 2.4f, 1.1f, 1.1f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition crownLeaf0 = head.addOrReplaceChild("crown_leaf_0", CubeListBuilder.create()
                .texOffs(171, 53).mirror(false).addBox(-1.25f, -3.3f, -0.3f, 2.5f, 3.3f, 0.6f), PartPose.offsetAndRotation(-4.3706f, -8, -2.3239f, 0.16f, 1.0821f, 0));
        PartDefinition crownJewel0 = crownLeaf0.addOrReplaceChild("crown_jewel_0", CubeListBuilder.create()
                .texOffs(34, 61).mirror(false).addBox(-0.5f, -0.5f, -0.25f, 1, 1, 0.5f), PartPose.offsetAndRotation(0, -1.6f, -0.4f, 0, 0, 0.785f));
        PartDefinition crownLeaf1 = head.addOrReplaceChild("crown_leaf_1", CubeListBuilder.create()
                .texOffs(171, 53).mirror(false).addBox(-1.25f, -3.3f, -0.3f, 2.5f, 3.3f, 0.6f), PartPose.offsetAndRotation(-2.5494f, -8, -4.243f, 0.16f, 0.5411f, 0));
        PartDefinition crownJewel1 = crownLeaf1.addOrReplaceChild("crown_jewel_1", CubeListBuilder.create()
                .texOffs(39, 61).mirror(false).addBox(-0.5f, -0.5f, -0.25f, 1, 1, 0.5f), PartPose.offsetAndRotation(0, -1.6f, -0.4f, 0, 0, 0.785f));
        PartDefinition crownLeaf2 = head.addOrReplaceChild("crown_leaf_2", CubeListBuilder.create()
                .texOffs(131, 53).mirror(false).addBox(-1.6f, -4.4f, -0.3f, 3.2f, 4.4f, 0.6f), PartPose.offsetAndRotation(0, -8, -4.95f, 0.16f, -0, 0));
        PartDefinition crownJewel2 = crownLeaf2.addOrReplaceChild("crown_jewel_2", CubeListBuilder.create()
                .texOffs(34, 61).mirror(false).addBox(-0.5f, -0.5f, -0.25f, 1, 1, 0.5f), PartPose.offsetAndRotation(0, -2, -0.4f, 0, 0, 0.785f));
        PartDefinition crownLeaf3 = head.addOrReplaceChild("crown_leaf_3", CubeListBuilder.create()
                .texOffs(171, 53).mirror(false).addBox(-1.25f, -3.3f, -0.3f, 2.5f, 3.3f, 0.6f), PartPose.offsetAndRotation(2.5494f, -8, -4.243f, 0.16f, -0.5411f, 0));
        PartDefinition crownJewel3 = crownLeaf3.addOrReplaceChild("crown_jewel_3", CubeListBuilder.create()
                .texOffs(39, 61).mirror(false).addBox(-0.5f, -0.5f, -0.25f, 1, 1, 0.5f), PartPose.offsetAndRotation(0, -1.6f, -0.4f, 0, 0, 0.785f));
        PartDefinition crownLeaf4 = head.addOrReplaceChild("crown_leaf_4", CubeListBuilder.create()
                .texOffs(171, 53).mirror(false).addBox(-1.25f, -3.3f, -0.3f, 2.5f, 3.3f, 0.6f), PartPose.offsetAndRotation(4.3706f, -8, -2.3239f, 0.16f, -1.0821f, 0));
        PartDefinition crownJewel4 = crownLeaf4.addOrReplaceChild("crown_jewel_4", CubeListBuilder.create()
                .texOffs(34, 61).mirror(false).addBox(-0.5f, -0.5f, -0.25f, 1, 1, 0.5f), PartPose.offsetAndRotation(0, -1.6f, -0.4f, 0, 0, 0.785f));
        PartDefinition bead0 = body.addOrReplaceChild("bead_0", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(-3.3f, 0.2f, -3.35f, 0, 0, 0));
        PartDefinition bead1 = body.addOrReplaceChild("bead_1", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(-2.75f, 2.1556f, -3.35f, 0, 0, 0));
        PartDefinition bead2 = body.addOrReplaceChild("bead_2", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(-2.2f, 3.7556f, -3.35f, 0, 0, 0));
        PartDefinition bead3 = body.addOrReplaceChild("bead_3", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(-1.65f, 5, -3.35f, 0, 0, 0));
        PartDefinition bead4 = body.addOrReplaceChild("bead_4", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(-1.1f, 5.8889f, -3.35f, 0, 0, 0));
        PartDefinition bead5 = body.addOrReplaceChild("bead_5", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(-0.55f, 6.4222f, -3.35f, 0, 0, 0));
        PartDefinition bead6 = body.addOrReplaceChild("bead_6", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(0, 6.6f, -3.35f, 0, 0, 0));
        PartDefinition bead7 = body.addOrReplaceChild("bead_7", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(0.55f, 6.4222f, -3.35f, 0, 0, 0));
        PartDefinition bead8 = body.addOrReplaceChild("bead_8", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(1.1f, 5.8889f, -3.35f, 0, 0, 0));
        PartDefinition bead9 = body.addOrReplaceChild("bead_9", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(1.65f, 5, -3.35f, 0, 0, 0));
        PartDefinition bead10 = body.addOrReplaceChild("bead_10", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(2.2f, 3.7556f, -3.35f, 0, 0, 0));
        PartDefinition bead11 = body.addOrReplaceChild("bead_11", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(2.75f, 2.1556f, -3.35f, 0, 0, 0));
        PartDefinition bead12 = body.addOrReplaceChild("bead_12", CubeListBuilder.create()
                .texOffs(202, 53).mirror(false).addBox(-0.55f, -0.55f, -0.55f, 1.1f, 1.1f, 1.1f), PartPose.offsetAndRotation(3.3f, 0.2f, -3.35f, 0, 0, 0));
        PartDefinition guruBead = body.addOrReplaceChild("guru_bead", CubeListBuilder.create()
                .texOffs(211, 53).mirror(false).addBox(-0.8f, -0.8f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(220, 53).mirror(false).addBox(-0.35f, 0.8f, -0.35f, 0.7f, 2.8f, 0.7f), PartPose.offsetAndRotation(0, 7.4f, -3.4f, 0, 0, 0));
        PartDefinition halo = body.addOrReplaceChild("halo", CubeListBuilder.create()
                .texOffs(79, 0).mirror(false).addBox(-9, -9, 0.3f, 18, 18, 0.2f), PartPose.offsetAndRotation(0, -4.2f, 6.4f, 0, 0, 0));
        PartDefinition haloInner0 = halo.addOrReplaceChild("halo_inner_0", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(0, -5.3f, 0, 0, 0, 0));
        PartDefinition haloInner1 = halo.addOrReplaceChild("halo_inner_1", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(1.6378f, -5.0406f, 0, 0, 0, 0.3142f));
        PartDefinition haloInner2 = halo.addOrReplaceChild("halo_inner_2", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(3.1153f, -4.2878f, 0, 0, 0, 0.6283f));
        PartDefinition haloInner3 = halo.addOrReplaceChild("halo_inner_3", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(4.2878f, -3.1153f, 0, 0, 0, 0.9425f));
        PartDefinition haloInner4 = halo.addOrReplaceChild("halo_inner_4", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(5.0406f, -1.6378f, 0, 0, 0, 1.2566f));
        PartDefinition haloInner5 = halo.addOrReplaceChild("halo_inner_5", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(5.3f, -0, 0, 0, 0, 1.5708f));
        PartDefinition haloInner6 = halo.addOrReplaceChild("halo_inner_6", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(5.0406f, 1.6378f, 0, 0, 0, 1.885f));
        PartDefinition haloInner7 = halo.addOrReplaceChild("halo_inner_7", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(4.2878f, 3.1153f, 0, 0, 0, 2.1991f));
        PartDefinition haloInner8 = halo.addOrReplaceChild("halo_inner_8", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(3.1153f, 4.2878f, 0, 0, 0, 2.5133f));
        PartDefinition haloInner9 = halo.addOrReplaceChild("halo_inner_9", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(1.6378f, 5.0406f, 0, 0, 0, 2.8274f));
        PartDefinition haloInner10 = halo.addOrReplaceChild("halo_inner_10", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(0, 5.3f, 0, 0, 0, 3.1416f));
        PartDefinition haloInner11 = halo.addOrReplaceChild("halo_inner_11", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(-1.6378f, 5.0406f, 0, 0, 0, 3.4558f));
        PartDefinition haloInner12 = halo.addOrReplaceChild("halo_inner_12", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(-3.1153f, 4.2878f, 0, 0, 0, 3.7699f));
        PartDefinition haloInner13 = halo.addOrReplaceChild("halo_inner_13", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(-4.2878f, 3.1153f, 0, 0, 0, 4.0841f));
        PartDefinition haloInner14 = halo.addOrReplaceChild("halo_inner_14", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(-5.0406f, 1.6378f, 0, 0, 0, 4.3982f));
        PartDefinition haloInner15 = halo.addOrReplaceChild("halo_inner_15", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(-5.3f, 0, 0, 0, 0, 4.7124f));
        PartDefinition haloInner16 = halo.addOrReplaceChild("halo_inner_16", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(-5.0406f, -1.6378f, 0, 0, 0, 5.0265f));
        PartDefinition haloInner17 = halo.addOrReplaceChild("halo_inner_17", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(-4.2878f, -3.1153f, 0, 0, 0, 5.3407f));
        PartDefinition haloInner18 = halo.addOrReplaceChild("halo_inner_18", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(-3.1153f, -4.2878f, 0, 0, 0, 5.6549f));
        PartDefinition haloInner19 = halo.addOrReplaceChild("halo_inner_19", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-0.95f, -0.45f, -0.1f, 1.9f, 0.9f, 0.5f), PartPose.offsetAndRotation(-1.6378f, -5.0406f, 0, 0, 0, 5.969f));
        PartDefinition haloOuter0 = halo.addOrReplaceChild("halo_outer_0", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, -8.6f, 0, 0, 0, 0));
        PartDefinition haloOuter1 = halo.addOrReplaceChild("halo_outer_1", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(1.9137f, -8.3844f, 0, 0, 0, 0.2244f));
        PartDefinition haloOuter2 = halo.addOrReplaceChild("halo_outer_2", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(3.7314f, -7.7483f, 0, 0, 0, 0.4488f));
        PartDefinition haloOuter3 = halo.addOrReplaceChild("halo_outer_3", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(5.362f, -6.7238f, 0, 0, 0, 0.6732f));
        PartDefinition haloOuter4 = halo.addOrReplaceChild("halo_outer_4", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(6.7238f, -5.362f, 0, 0, 0, 0.8976f));
        PartDefinition haloOuter5 = halo.addOrReplaceChild("halo_outer_5", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(7.7483f, -3.7314f, 0, 0, 0, 1.122f));
        PartDefinition haloOuter6 = halo.addOrReplaceChild("halo_outer_6", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(8.3844f, -1.9137f, 0, 0, 0, 1.3464f));
        PartDefinition haloOuter7 = halo.addOrReplaceChild("halo_outer_7", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(8.6f, -0, 0, 0, 0, 1.5708f));
        PartDefinition haloOuter8 = halo.addOrReplaceChild("halo_outer_8", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(8.3844f, 1.9137f, 0, 0, 0, 1.7952f));
        PartDefinition haloOuter9 = halo.addOrReplaceChild("halo_outer_9", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(7.7483f, 3.7314f, 0, 0, 0, 2.0196f));
        PartDefinition haloOuter10 = halo.addOrReplaceChild("halo_outer_10", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(6.7238f, 5.362f, 0, 0, 0, 2.244f));
        PartDefinition haloOuter11 = halo.addOrReplaceChild("halo_outer_11", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(5.362f, 6.7238f, 0, 0, 0, 2.4684f));
        PartDefinition haloOuter12 = halo.addOrReplaceChild("halo_outer_12", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(3.7314f, 7.7483f, 0, 0, 0, 2.6928f));
        PartDefinition haloOuter13 = halo.addOrReplaceChild("halo_outer_13", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(1.9137f, 8.3844f, 0, 0, 0, 2.9172f));
        PartDefinition haloOuter14 = halo.addOrReplaceChild("halo_outer_14", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, 8.6f, 0, 0, 0, 3.1416f));
        PartDefinition haloOuter15 = halo.addOrReplaceChild("halo_outer_15", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-1.9137f, 8.3844f, 0, 0, 0, 3.366f));
        PartDefinition haloOuter16 = halo.addOrReplaceChild("halo_outer_16", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-3.7314f, 7.7483f, 0, 0, 0, 3.5904f));
        PartDefinition haloOuter17 = halo.addOrReplaceChild("halo_outer_17", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-5.362f, 6.7238f, 0, 0, 0, 3.8148f));
        PartDefinition haloOuter18 = halo.addOrReplaceChild("halo_outer_18", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-6.7238f, 5.362f, 0, 0, 0, 4.0392f));
        PartDefinition haloOuter19 = halo.addOrReplaceChild("halo_outer_19", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-7.7483f, 3.7314f, 0, 0, 0, 4.2636f));
        PartDefinition haloOuter20 = halo.addOrReplaceChild("halo_outer_20", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-8.3844f, 1.9137f, 0, 0, 0, 4.488f));
        PartDefinition haloOuter21 = halo.addOrReplaceChild("halo_outer_21", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-8.6f, 0, 0, 0, 0, 4.7124f));
        PartDefinition haloOuter22 = halo.addOrReplaceChild("halo_outer_22", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-8.3844f, -1.9137f, 0, 0, 0, 4.9368f));
        PartDefinition haloOuter23 = halo.addOrReplaceChild("halo_outer_23", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-7.7483f, -3.7314f, 0, 0, 0, 5.1612f));
        PartDefinition haloOuter24 = halo.addOrReplaceChild("halo_outer_24", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-6.7238f, -5.362f, 0, 0, 0, 5.3856f));
        PartDefinition haloOuter25 = halo.addOrReplaceChild("halo_outer_25", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-5.362f, -6.7238f, 0, 0, 0, 5.61f));
        PartDefinition haloOuter26 = halo.addOrReplaceChild("halo_outer_26", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-3.7314f, -7.7483f, 0, 0, 0, 5.8344f));
        PartDefinition haloOuter27 = halo.addOrReplaceChild("halo_outer_27", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.05f, -0.6f, -0.2f, 2.1f, 1.2f, 0.7f), PartPose.offsetAndRotation(-1.9137f, -8.3844f, 0, 0, 0, 6.0588f));
        PartDefinition haloFlame0 = halo.addOrReplaceChild("halo_flame_0", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.6f, -4.6f, -0.1f, 3.2f, 4.6f, 0.2f)
                .texOffs(89, 53).mirror(false).addBox(-0.1f, -3.91f, -1.2f, 0.2f, 3.91f, 2.4f), PartPose.offsetAndRotation(0, -9.2f, 0.2f, 0, 0, 0));
        PartDefinition haloFlameBack0 = halo.addOrReplaceChild("halo_flame_back_0", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(2.0472f, -8.9693f, 0.55f, 0, 0, 0.2244f));
        PartDefinition haloFlame1 = halo.addOrReplaceChild("halo_flame_1", CubeListBuilder.create()
                .texOffs(180, 53).mirror(false).addBox(-1.6f, -3.4f, -0.1f, 3.2f, 3.4f, 0.2f)
                .texOffs(153, 53).mirror(false).addBox(-0.1f, -2.89f, -1.2f, 0.2f, 2.89f, 2.4f), PartPose.offsetAndRotation(3.9917f, -8.2889f, 0.2f, 0, 0, 0.4488f));
        PartDefinition haloFlameBack1 = halo.addOrReplaceChild("halo_flame_back_1", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(5.7361f, -7.1928f, 0.55f, 0, 0, 0.6732f));
        PartDefinition haloFlame2 = halo.addOrReplaceChild("halo_flame_2", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.6f, -4.6f, -0.1f, 3.2f, 4.6f, 0.2f)
                .texOffs(89, 53).mirror(false).addBox(-0.1f, -3.91f, -1.2f, 0.2f, 3.91f, 2.4f), PartPose.offsetAndRotation(7.1928f, -5.7361f, 0.2f, 0, 0, 0.8976f));
        PartDefinition haloFlameBack2 = halo.addOrReplaceChild("halo_flame_back_2", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(8.2889f, -3.9917f, 0.55f, 0, 0, 1.122f));
        PartDefinition haloFlame3 = halo.addOrReplaceChild("halo_flame_3", CubeListBuilder.create()
                .texOffs(180, 53).mirror(false).addBox(-1.6f, -3.4f, -0.1f, 3.2f, 3.4f, 0.2f)
                .texOffs(153, 53).mirror(false).addBox(-0.1f, -2.89f, -1.2f, 0.2f, 2.89f, 2.4f), PartPose.offsetAndRotation(8.9693f, -2.0472f, 0.2f, 0, 0, 1.3464f));
        PartDefinition haloFlameBack3 = halo.addOrReplaceChild("halo_flame_back_3", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(9.2f, -0, 0.55f, 0, 0, 1.5708f));
        PartDefinition haloFlame4 = halo.addOrReplaceChild("halo_flame_4", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.6f, -4.6f, -0.1f, 3.2f, 4.6f, 0.2f)
                .texOffs(89, 53).mirror(false).addBox(-0.1f, -3.91f, -1.2f, 0.2f, 3.91f, 2.4f), PartPose.offsetAndRotation(8.9693f, 2.0472f, 0.2f, 0, 0, 1.7952f));
        PartDefinition haloFlameBack4 = halo.addOrReplaceChild("halo_flame_back_4", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(8.2889f, 3.9917f, 0.55f, 0, 0, 2.0196f));
        PartDefinition haloFlame5 = halo.addOrReplaceChild("halo_flame_5", CubeListBuilder.create()
                .texOffs(180, 53).mirror(false).addBox(-1.6f, -3.4f, -0.1f, 3.2f, 3.4f, 0.2f)
                .texOffs(153, 53).mirror(false).addBox(-0.1f, -2.89f, -1.2f, 0.2f, 2.89f, 2.4f), PartPose.offsetAndRotation(7.1928f, 5.7361f, 0.2f, 0, 0, 2.244f));
        PartDefinition haloFlameBack5 = halo.addOrReplaceChild("halo_flame_back_5", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(5.7361f, 7.1928f, 0.55f, 0, 0, 2.4684f));
        PartDefinition haloFlame6 = halo.addOrReplaceChild("halo_flame_6", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.6f, -4.6f, -0.1f, 3.2f, 4.6f, 0.2f)
                .texOffs(89, 53).mirror(false).addBox(-0.1f, -3.91f, -1.2f, 0.2f, 3.91f, 2.4f), PartPose.offsetAndRotation(3.9917f, 8.2889f, 0.2f, 0, 0, 2.6928f));
        PartDefinition haloFlameBack6 = halo.addOrReplaceChild("halo_flame_back_6", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(2.0472f, 8.9693f, 0.55f, 0, 0, 2.9172f));
        PartDefinition haloFlame7 = halo.addOrReplaceChild("halo_flame_7", CubeListBuilder.create()
                .texOffs(180, 53).mirror(false).addBox(-1.6f, -3.4f, -0.1f, 3.2f, 3.4f, 0.2f)
                .texOffs(153, 53).mirror(false).addBox(-0.1f, -2.89f, -1.2f, 0.2f, 2.89f, 2.4f), PartPose.offsetAndRotation(0, 9.2f, 0.2f, 0, 0, 3.1416f));
        PartDefinition haloFlameBack7 = halo.addOrReplaceChild("halo_flame_back_7", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(-2.0472f, 8.9693f, 0.55f, 0, 0, 3.366f));
        PartDefinition haloFlame8 = halo.addOrReplaceChild("halo_flame_8", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.6f, -4.6f, -0.1f, 3.2f, 4.6f, 0.2f)
                .texOffs(89, 53).mirror(false).addBox(-0.1f, -3.91f, -1.2f, 0.2f, 3.91f, 2.4f), PartPose.offsetAndRotation(-3.9917f, 8.2889f, 0.2f, 0, 0, 3.5904f));
        PartDefinition haloFlameBack8 = halo.addOrReplaceChild("halo_flame_back_8", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(-5.7361f, 7.1928f, 0.55f, 0, 0, 3.8148f));
        PartDefinition haloFlame9 = halo.addOrReplaceChild("halo_flame_9", CubeListBuilder.create()
                .texOffs(180, 53).mirror(false).addBox(-1.6f, -3.4f, -0.1f, 3.2f, 3.4f, 0.2f)
                .texOffs(153, 53).mirror(false).addBox(-0.1f, -2.89f, -1.2f, 0.2f, 2.89f, 2.4f), PartPose.offsetAndRotation(-7.1928f, 5.7361f, 0.2f, 0, 0, 4.0392f));
        PartDefinition haloFlameBack9 = halo.addOrReplaceChild("halo_flame_back_9", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(-8.2889f, 3.9917f, 0.55f, 0, 0, 4.2636f));
        PartDefinition haloFlame10 = halo.addOrReplaceChild("halo_flame_10", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.6f, -4.6f, -0.1f, 3.2f, 4.6f, 0.2f)
                .texOffs(89, 53).mirror(false).addBox(-0.1f, -3.91f, -1.2f, 0.2f, 3.91f, 2.4f), PartPose.offsetAndRotation(-8.9693f, 2.0472f, 0.2f, 0, 0, 4.488f));
        PartDefinition haloFlameBack10 = halo.addOrReplaceChild("halo_flame_back_10", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(-9.2f, 0, 0.55f, 0, 0, 4.7124f));
        PartDefinition haloFlame11 = halo.addOrReplaceChild("halo_flame_11", CubeListBuilder.create()
                .texOffs(180, 53).mirror(false).addBox(-1.6f, -3.4f, -0.1f, 3.2f, 3.4f, 0.2f)
                .texOffs(153, 53).mirror(false).addBox(-0.1f, -2.89f, -1.2f, 0.2f, 2.89f, 2.4f), PartPose.offsetAndRotation(-8.9693f, -2.0472f, 0.2f, 0, 0, 4.9368f));
        PartDefinition haloFlameBack11 = halo.addOrReplaceChild("halo_flame_back_11", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(-8.2889f, -3.9917f, 0.55f, 0, 0, 5.1612f));
        PartDefinition haloFlame12 = halo.addOrReplaceChild("halo_flame_12", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.6f, -4.6f, -0.1f, 3.2f, 4.6f, 0.2f)
                .texOffs(89, 53).mirror(false).addBox(-0.1f, -3.91f, -1.2f, 0.2f, 3.91f, 2.4f), PartPose.offsetAndRotation(-7.1928f, -5.7361f, 0.2f, 0, 0, 5.3856f));
        PartDefinition haloFlameBack12 = halo.addOrReplaceChild("halo_flame_back_12", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(-5.7361f, -7.1928f, 0.55f, 0, 0, 5.61f));
        PartDefinition haloFlame13 = halo.addOrReplaceChild("halo_flame_13", CubeListBuilder.create()
                .texOffs(180, 53).mirror(false).addBox(-1.6f, -3.4f, -0.1f, 3.2f, 3.4f, 0.2f)
                .texOffs(153, 53).mirror(false).addBox(-0.1f, -2.89f, -1.2f, 0.2f, 2.89f, 2.4f), PartPose.offsetAndRotation(-3.9917f, -8.2889f, 0.2f, 0, 0, 5.8344f));
        PartDefinition haloFlameBack13 = halo.addOrReplaceChild("halo_flame_back_13", CubeListBuilder.create()
                .texOffs(225, 53).mirror(false).addBox(-1.3f, -2.8f, -0.1f, 2.6f, 2.8f, 0.2f), PartPose.offsetAndRotation(-2.0472f, -8.9693f, 0.55f, 0, 0, 6.0588f));
        PartDefinition mandorla = body.addOrReplaceChild("mandorla", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-13, -18, 0.4f, 26, 36, 0.2f), PartPose.offsetAndRotation(0, 7.5f, 7.6f, 0, 0, 0));
        PartDefinition mandorlaRim0 = mandorla.addOrReplaceChild("mandorla_rim_0", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.0986f, -0.55f, -0.25f, 2.1971f, 1.1f, 0.6f), PartPose.offsetAndRotation(-6.0778f, 14.8085f, 0.1f, 0, 0, -2.4923f));
        PartDefinition mandorlaRim1 = mandorla.addOrReplaceChild("mandorla_rim_1", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.14f, -0.55f, -0.25f, 2.2801f, 1.1f, 0.6f), PartPose.offsetAndRotation(-7.5233f, 13.525f, 0.1f, 0, 0, -2.342f));
        PartDefinition mandorlaRim2 = mandorla.addOrReplaceChild("mandorla_rim_2", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.1831f, -0.55f, -0.25f, 2.3661f, 1.1f, 0.6f), PartPose.offsetAndRotation(-8.8247f, 11.9823f, 0.1f, 0, 0, -2.2041f));
        PartDefinition mandorlaRim3 = mandorla.addOrReplaceChild("mandorla_rim_3", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.2243f, -0.55f, -0.25f, 2.4487f, 1.1f, 0.6f), PartPose.offsetAndRotation(-9.957f, 10.21f, 0.1f, 0, 0, -2.077f));
        PartDefinition mandorlaRim4 = mandorla.addOrReplaceChild("mandorla_rim_4", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.2611f, -0.55f, -0.25f, 2.5222f, 1.1f, 0.6f), PartPose.offsetAndRotation(-10.8984f, 8.2419f, 0.1f, 0, 0, -1.9589f));
        PartDefinition mandorlaRim5 = mandorla.addOrReplaceChild("mandorla_rim_5", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.2911f, -0.55f, -0.25f, 2.5821f, 1.1f, 0.6f), PartPose.offsetAndRotation(-11.6309f, 6.1159f, 0.1f, 0, 0, -1.8478f));
        PartDefinition mandorlaRim6 = mandorla.addOrReplaceChild("mandorla_rim_6", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.3126f, -0.55f, -0.25f, 2.6251f, 1.1f, 0.6f), PartPose.offsetAndRotation(-12.1405f, 3.8727f, 0.1f, 0, 0, -1.7416f));
        PartDefinition mandorlaRim7 = mandorla.addOrReplaceChild("mandorla_rim_7", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.3245f, -0.55f, -0.25f, 2.649f, 1.1f, 0.6f), PartPose.offsetAndRotation(-12.4175f, 1.5552f, 0.1f, 0, 0, -1.6384f));
        PartDefinition mandorlaRim8 = mandorla.addOrReplaceChild("mandorla_rim_8", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.328f, -0.55f, -0.25f, 2.6561f, 1.1f, 0.6f), PartPose.offsetAndRotation(-12.419f, -0.7921f, 0.1f, 0, 0, -1.5047f));
        PartDefinition mandorlaRim9 = mandorla.addOrReplaceChild("mandorla_rim_9", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.3435f, -0.55f, -0.25f, 2.6871f, 1.1f, 0.6f), PartPose.offsetAndRotation(-12.0466f, -3.1241f, 0.1f, 0, 0, -1.3214f));
        PartDefinition mandorlaRim10 = mandorla.addOrReplaceChild("mandorla_rim_10", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.366f, -0.55f, -0.25f, 2.7321f, 1.1f, 0.6f), PartPose.offsetAndRotation(-11.2681f, -5.3964f, 0.1f, 0, 0, -1.1615f));
        PartDefinition mandorlaRim11 = mandorla.addOrReplaceChild("mandorla_rim_11", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.3761f, -0.55f, -0.25f, 2.7522f, 1.1f, 0.6f), PartPose.offsetAndRotation(-10.1563f, -7.5651f, 0.1f, 0, 0, -1.0332f));
        PartDefinition mandorlaRim12 = mandorla.addOrReplaceChild("mandorla_rim_12", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.3565f, -0.55f, -0.25f, 2.7129f, 1.1f, 0.6f), PartPose.offsetAndRotation(-8.8119f, -9.5889f, 0.1f, 0, 0, -0.9348f));
        PartDefinition mandorlaRim13 = mandorla.addOrReplaceChild("mandorla_rim_13", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.2975f, -0.55f, -0.25f, 2.595f, 1.1f, 0.6f), PartPose.offsetAndRotation(-7.3464f, -11.4289f, 0.1f, 0, 0, -0.8597f));
        PartDefinition mandorlaRim14 = mandorla.addOrReplaceChild("mandorla_rim_14", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.1991f, -0.55f, -0.25f, 2.3981f, 1.1f, 0.6f), PartPose.offsetAndRotation(-5.8656f, -13.0499f, 0.1f, 0, 0, -0.7986f));
        PartDefinition mandorlaRim15 = mandorla.addOrReplaceChild("mandorla_rim_15", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.0694f, -0.55f, -0.25f, 2.1389f, 1.1f, 0.6f), PartPose.offsetAndRotation(-4.4541f, -14.4207f, 0.1f, 0, 0, -0.739f));
        PartDefinition mandorlaRim16 = mandorla.addOrReplaceChild("mandorla_rim_16", CubeListBuilder.create()
                .texOffs(18, 61).mirror(false).addBox(-0.9234f, -0.55f, -0.25f, 1.8468f, 1.1f, 0.6f), PartPose.offsetAndRotation(-3.1643f, -15.5152f, 0.1f, 0, 0, -0.6616f));
        PartDefinition mandorlaRim17 = mandorla.addOrReplaceChild("mandorla_rim_17", CubeListBuilder.create()
                .texOffs(18, 61).mirror(false).addBox(-0.7813f, -0.55f, -0.25f, 1.5627f, 1.1f, 0.6f), PartPose.offsetAndRotation(-2.0111f, -16.3123f, 0.1f, 0, 0, -0.5351f));
        PartDefinition mandorlaRim18 = mandorla.addOrReplaceChild("mandorla_rim_18", CubeListBuilder.create()
                .texOffs(18, 61).mirror(false).addBox(-0.6711f, -0.55f, -0.25f, 1.3422f, 1.1f, 0.6f), PartPose.offsetAndRotation(-0.9729f, -16.7967f, 0.1f, 0, 0, -0.3172f));
        PartDefinition mandorlaRim19 = mandorla.addOrReplaceChild("mandorla_rim_19", CubeListBuilder.create()
                .texOffs(18, 61).mirror(false).addBox(-0.6278f, -0.55f, -0.25f, 1.2555f, 1.1f, 0.6f), PartPose.offsetAndRotation(-0, -16.9592f, 0.1f, 0, 0, 0));
        PartDefinition mandorlaRim20 = mandorla.addOrReplaceChild("mandorla_rim_20", CubeListBuilder.create()
                .texOffs(18, 61).mirror(false).addBox(-0.6711f, -0.55f, -0.25f, 1.3422f, 1.1f, 0.6f), PartPose.offsetAndRotation(0.9729f, -16.7967f, 0.1f, 0, 0, 0.3172f));
        PartDefinition mandorlaRim21 = mandorla.addOrReplaceChild("mandorla_rim_21", CubeListBuilder.create()
                .texOffs(18, 61).mirror(false).addBox(-0.7813f, -0.55f, -0.25f, 1.5627f, 1.1f, 0.6f), PartPose.offsetAndRotation(2.0111f, -16.3123f, 0.1f, 0, 0, 0.5351f));
        PartDefinition mandorlaRim22 = mandorla.addOrReplaceChild("mandorla_rim_22", CubeListBuilder.create()
                .texOffs(18, 61).mirror(false).addBox(-0.9234f, -0.55f, -0.25f, 1.8468f, 1.1f, 0.6f), PartPose.offsetAndRotation(3.1643f, -15.5152f, 0.1f, 0, 0, 0.6616f));
        PartDefinition mandorlaRim23 = mandorla.addOrReplaceChild("mandorla_rim_23", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.0694f, -0.55f, -0.25f, 2.1389f, 1.1f, 0.6f), PartPose.offsetAndRotation(4.4541f, -14.4207f, 0.1f, 0, 0, 0.739f));
        PartDefinition mandorlaRim24 = mandorla.addOrReplaceChild("mandorla_rim_24", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.1991f, -0.55f, -0.25f, 2.3981f, 1.1f, 0.6f), PartPose.offsetAndRotation(5.8656f, -13.0499f, 0.1f, 0, 0, 0.7986f));
        PartDefinition mandorlaRim25 = mandorla.addOrReplaceChild("mandorla_rim_25", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.2975f, -0.55f, -0.25f, 2.595f, 1.1f, 0.6f), PartPose.offsetAndRotation(7.3464f, -11.4289f, 0.1f, 0, 0, 0.8597f));
        PartDefinition mandorlaRim26 = mandorla.addOrReplaceChild("mandorla_rim_26", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.3565f, -0.55f, -0.25f, 2.7129f, 1.1f, 0.6f), PartPose.offsetAndRotation(8.8119f, -9.5889f, 0.1f, 0, 0, 0.9348f));
        PartDefinition mandorlaRim27 = mandorla.addOrReplaceChild("mandorla_rim_27", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.3761f, -0.55f, -0.25f, 2.7522f, 1.1f, 0.6f), PartPose.offsetAndRotation(10.1563f, -7.5651f, 0.1f, 0, 0, 1.0332f));
        PartDefinition mandorlaRim28 = mandorla.addOrReplaceChild("mandorla_rim_28", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.366f, -0.55f, -0.25f, 2.7321f, 1.1f, 0.6f), PartPose.offsetAndRotation(11.2681f, -5.3964f, 0.1f, 0, 0, 1.1615f));
        PartDefinition mandorlaRim29 = mandorla.addOrReplaceChild("mandorla_rim_29", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.3435f, -0.55f, -0.25f, 2.6871f, 1.1f, 0.6f), PartPose.offsetAndRotation(12.0466f, -3.1241f, 0.1f, 0, 0, 1.3214f));
        PartDefinition mandorlaRim30 = mandorla.addOrReplaceChild("mandorla_rim_30", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.328f, -0.55f, -0.25f, 2.6561f, 1.1f, 0.6f), PartPose.offsetAndRotation(12.419f, -0.7921f, 0.1f, 0, 0, 1.5047f));
        PartDefinition mandorlaRim31 = mandorla.addOrReplaceChild("mandorla_rim_31", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.3245f, -0.55f, -0.25f, 2.649f, 1.1f, 0.6f), PartPose.offsetAndRotation(12.4175f, 1.5552f, 0.1f, 0, 0, 1.6384f));
        PartDefinition mandorlaRim32 = mandorla.addOrReplaceChild("mandorla_rim_32", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.3126f, -0.55f, -0.25f, 2.6251f, 1.1f, 0.6f), PartPose.offsetAndRotation(12.1405f, 3.8727f, 0.1f, 0, 0, 1.7416f));
        PartDefinition mandorlaRim33 = mandorla.addOrReplaceChild("mandorla_rim_33", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.2911f, -0.55f, -0.25f, 2.5821f, 1.1f, 0.6f), PartPose.offsetAndRotation(11.6309f, 6.1159f, 0.1f, 0, 0, 1.8478f));
        PartDefinition mandorlaRim34 = mandorla.addOrReplaceChild("mandorla_rim_34", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.2611f, -0.55f, -0.25f, 2.5222f, 1.1f, 0.6f), PartPose.offsetAndRotation(10.8984f, 8.2419f, 0.1f, 0, 0, 1.9589f));
        PartDefinition mandorlaRim35 = mandorla.addOrReplaceChild("mandorla_rim_35", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.2243f, -0.55f, -0.25f, 2.4487f, 1.1f, 0.6f), PartPose.offsetAndRotation(9.957f, 10.21f, 0.1f, 0, 0, 2.077f));
        PartDefinition mandorlaRim36 = mandorla.addOrReplaceChild("mandorla_rim_36", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.1831f, -0.55f, -0.25f, 2.3661f, 1.1f, 0.6f), PartPose.offsetAndRotation(8.8247f, 11.9823f, 0.1f, 0, 0, 2.2041f));
        PartDefinition mandorlaRim37 = mandorla.addOrReplaceChild("mandorla_rim_37", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.14f, -0.55f, -0.25f, 2.2801f, 1.1f, 0.6f), PartPose.offsetAndRotation(7.5233f, 13.525f, 0.1f, 0, 0, 2.342f));
        PartDefinition mandorlaRim38 = mandorla.addOrReplaceChild("mandorla_rim_38", CubeListBuilder.create()
                .texOffs(9, 61).mirror(false).addBox(-1.0986f, -0.55f, -0.25f, 2.1971f, 1.1f, 0.6f), PartPose.offsetAndRotation(6.0778f, 14.8085f, 0.1f, 0, 0, 2.4923f));
        PartDefinition mandorlaFlame1 = mandorla.addOrReplaceChild("mandorla_flame_1", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.7f, -4.2f, -0.1f, 3.4f, 4.2f, 0.2f), PartPose.offsetAndRotation(-6.8333f, 14.235f, 0.3f, 0, 0, -2.4155f));
        PartDefinition mandorlaFlame3 = mandorla.addOrReplaceChild("mandorla_flame_3", CubeListBuilder.create()
                .texOffs(234, 53).mirror(false).addBox(-1.7f, -3, -0.1f, 3.4f, 3, 0.2f), PartPose.offsetAndRotation(-9.4361f, 11.1496f, 0.3f, 0, 0, -2.1393f));
        PartDefinition mandorlaFlame5 = mandorla.addOrReplaceChild("mandorla_flame_5", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.7f, -4.2f, -0.1f, 3.4f, 4.2f, 0.2f), PartPose.offsetAndRotation(-11.3189f, 7.2135f, 0.3f, 0, 0, -1.9026f));
        PartDefinition mandorlaFlame7 = mandorla.addOrReplaceChild("mandorla_flame_7", CubeListBuilder.create()
                .texOffs(234, 53).mirror(false).addBox(-1.7f, -3, -0.1f, 3.4f, 3, 0.2f), PartPose.offsetAndRotation(-12.3381f, 2.727f, 0.3f, 0, 0, -1.6897f));
        PartDefinition mandorlaFlame9 = mandorla.addOrReplaceChild("mandorla_flame_9", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.7f, -4.2f, -0.1f, 3.4f, 4.2f, 0.2f), PartPose.offsetAndRotation(-12.3412f, -1.9675f, 0.3f, 0, 0, -1.4125f));
        PartDefinition mandorlaFlame11 = mandorla.addOrReplaceChild("mandorla_flame_11", CubeListBuilder.create()
                .texOffs(234, 53).mirror(false).addBox(-1.7f, -3, -0.1f, 3.4f, 3, 0.2f), PartPose.offsetAndRotation(-10.7842f, -6.5119f, 0.3f, 0, 0, -1.0971f));
        PartDefinition mandorlaFlame13 = mandorla.addOrReplaceChild("mandorla_flame_13", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.7f, -4.2f, -0.1f, 3.4f, 4.2f, 0.2f), PartPose.offsetAndRotation(-8.0953f, -10.5595f, 0.3f, 0, 0, -0.8982f));
        PartDefinition mandorlaFlame15 = mandorla.addOrReplaceChild("mandorla_flame_15", CubeListBuilder.create()
                .texOffs(234, 53).mirror(false).addBox(-1.7f, -3, -0.1f, 3.4f, 3, 0.2f), PartPose.offsetAndRotation(-5.1337f, -13.8014f, 0.3f, 0, 0, -0.7708f));
        PartDefinition mandorlaFlame17 = mandorla.addOrReplaceChild("mandorla_flame_17", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.7f, -4.2f, -0.1f, 3.4f, 4.2f, 0.2f), PartPose.offsetAndRotation(-2.5541f, -15.9903f, 0.3f, 0, 0, -0.6048f));
        PartDefinition mandorlaFlame19 = mandorla.addOrReplaceChild("mandorla_flame_19", CubeListBuilder.create()
                .texOffs(234, 53).mirror(false).addBox(-1.7f, -3, -0.1f, 3.4f, 3, 0.2f), PartPose.offsetAndRotation(-0.4778f, -16.9592f, 0.3f, 0, 0, -0.1655f));
        PartDefinition mandorlaFlame21 = mandorla.addOrReplaceChild("mandorla_flame_21", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.7f, -4.2f, -0.1f, 3.4f, 4.2f, 0.2f), PartPose.offsetAndRotation(1.468f, -16.6342f, 0.3f, 0, 0, 0.4366f));
        PartDefinition mandorlaFlame23 = mandorla.addOrReplaceChild("mandorla_flame_23", CubeListBuilder.create()
                .texOffs(234, 53).mirror(false).addBox(-1.7f, -3, -0.1f, 3.4f, 3, 0.2f), PartPose.offsetAndRotation(3.7745f, -15.04f, 0.3f, 0, 0, 0.7037f));
        PartDefinition mandorlaFlame25 = mandorla.addOrReplaceChild("mandorla_flame_25", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.7f, -4.2f, -0.1f, 3.4f, 4.2f, 0.2f), PartPose.offsetAndRotation(6.5975f, -12.2983f, 0.3f, 0, 0, 0.8305f));
        PartDefinition mandorlaFlame27 = mandorla.addOrReplaceChild("mandorla_flame_27", CubeListBuilder.create()
                .texOffs(234, 53).mirror(false).addBox(-1.7f, -3, -0.1f, 3.4f, 3, 0.2f), PartPose.offsetAndRotation(9.5285f, -8.6183f, 0.3f, 0, 0, 0.9844f));
        PartDefinition mandorlaFlame29 = mandorla.addOrReplaceChild("mandorla_flame_29", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.7f, -4.2f, -0.1f, 3.4f, 4.2f, 0.2f), PartPose.offsetAndRotation(11.752f, -4.2808f, 0.3f, 0, 0, 1.2407f));
        PartDefinition mandorlaFlame31 = mandorla.addOrReplaceChild("mandorla_flame_31", CubeListBuilder.create()
                .texOffs(234, 53).mirror(false).addBox(-1.7f, -3, -0.1f, 3.4f, 3, 0.2f), PartPose.offsetAndRotation(12.4968f, 0.3834f, 0.3f, 0, 0, 1.5714f));
        PartDefinition mandorlaFlame33 = mandorla.addOrReplaceChild("mandorla_flame_33", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.7f, -4.2f, -0.1f, 3.4f, 4.2f, 0.2f), PartPose.offsetAndRotation(11.943f, 5.0183f, 0.3f, 0, 0, 1.7942f));
        PartDefinition mandorlaFlame35 = mandorla.addOrReplaceChild("mandorla_flame_35", CubeListBuilder.create()
                .texOffs(234, 53).mirror(false).addBox(-1.7f, -3, -0.1f, 3.4f, 3, 0.2f), PartPose.offsetAndRotation(10.4779f, 9.2703f, 0.3f, 0, 0, 2.017f));
        PartDefinition mandorlaFlame37 = mandorla.addOrReplaceChild("mandorla_flame_37", CubeListBuilder.create()
                .texOffs(142, 53).mirror(false).addBox(-1.7f, -4.2f, -0.1f, 3.4f, 4.2f, 0.2f), PartPose.offsetAndRotation(8.2134f, 12.8151f, 0.3f, 0, 0, 2.2715f));
        PartDefinition mandorlaCrest = mandorla.addOrReplaceChild("mandorla_crest", CubeListBuilder.create()
                .texOffs(224, 39).mirror(false).addBox(-2.2f, -6.5f, -0.1f, 4.4f, 6.5f, 0.2f), PartPose.offsetAndRotation(0, -16.7f, 0.3f, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static LayerDefinition headLayer() {
        // GENERATED head: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition skull = head.addOrReplaceChild("skull", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-4.3f, -8.6f, -3, 8.6f, 8.4f, 7.4f)
                .texOffs(0, 18).mirror(false).addBox(-3.3f, -9.8f, -2.2f, 6.6f, 1.6f, 6.2f)
                .texOffs(35, 0).mirror(false).addBox(-4.1f, -8.2f, 4.2f, 8.2f, 7.2f, 1.6f)
                .texOffs(29, 18).mirror(false).addBox(-3.4f, -6.5f, -3.55f, 6.8f, 6.2f, 0.6f)
                .texOffs(37, 28).mirror(false).addBox(-2.1f, -0.7f, -5.2f, 4.2f, 1.2f, 2.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(24, 37).mirror(false).addBox(-3.8f, -0.95f, -1.55f, 3.8f, 1.35f, 1.6f), PartPose.offsetAndRotation(0, -6.45f, -3.9f, 0, 0, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(24, 37).mirror(true).addBox(0, -0.95f, -1.55f, 3.8f, 1.35f, 1.6f), PartPose.offsetAndRotation(0, -6.45f, -3.9f, 0, 0, -0.2f));
        PartDefinition glabella = head.addOrReplaceChild("glabella", CubeListBuilder.create()
                .texOffs(44, 37).mirror(false).addBox(-0.75f, -0.6f, -0.6f, 1.5f, 1.6f, 1), PartPose.offsetAndRotation(0, -6.3f, -4.3f, 0, 0, 0));
        PartDefinition bridge = head.addOrReplaceChild("bridge", CubeListBuilder.create()
                .texOffs(51, 37).mirror(false).addBox(-0.6f, 0, -0.5f, 1.2f, 1.6f, 1), PartPose.offsetAndRotation(0, -4.75f, -3.7f, -0.9f, 0, 0));
        PartDefinition muzzle = head.addOrReplaceChild("muzzle", CubeListBuilder.create()
                .texOffs(11, 28).mirror(false).addBox(-2.05f, -3.75f, -6.4f, 4.1f, 2.5f, 2.9f)
                .texOffs(0, 43).mirror(false).addBox(-1.5f, -4.25f, -5.7f, 3, 0.55f, 2)
                .texOffs(37, 37).mirror(false).addBox(-2.5f, -3.3f, -5.6f, 0.6f, 1.9f, 1.9f)
                .texOffs(37, 37).mirror(false).addBox(1.9f, -3.3f, -5.6f, 0.6f, 1.9f, 1.9f)
                .texOffs(0, 37).mirror(false).addBox(-1.6f, -1.3f, -5.7f, 3.2f, 1.25f, 2.1f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightCheekbone = head.addOrReplaceChild("right_cheekbone", CubeListBuilder.create()
                .texOffs(11, 43).mirror(false).addBox(-1.25f, -0.55f, -0.8f, 2.5f, 1.15f, 1), PartPose.offsetAndRotation(-2.35f, -3.55f, -3.75f, 0, 0.32f, 0));
        PartDefinition leftCheekbone = head.addOrReplaceChild("left_cheekbone", CubeListBuilder.create()
                .texOffs(11, 43).mirror(true).addBox(-1.25f, -0.55f, -0.8f, 2.5f, 1.15f, 1), PartPose.offsetAndRotation(2.35f, -3.55f, -3.75f, 0, -0.32f, 0));
        PartDefinition rightSideburn = head.addOrReplaceChild("right_sideburn", CubeListBuilder.create()
                .texOffs(46, 18).mirror(false).addBox(-0.95f, 0, -1, 1.9f, 5.4f, 2), PartPose.offsetAndRotation(-3.7f, -6, -3.2f, 0.05f, 0.2f, 0.12f));
        PartDefinition leftSideburn = head.addOrReplaceChild("left_sideburn", CubeListBuilder.create()
                .texOffs(46, 18).mirror(true).addBox(-0.95f, 0, -1, 1.9f, 5.4f, 2), PartPose.offsetAndRotation(3.7f, -6, -3.2f, 0.05f, -0.2f, -0.12f));
        PartDefinition rightCheekTuft0 = head.addOrReplaceChild("right_cheek_tuft_0", CubeListBuilder.create()
                .texOffs(28, 28).mirror(false).addBox(-0.8f, 0, -0.9f, 1.6f, 3.8f, 1.8f), PartPose.offsetAndRotation(-3.5f, -3.3f, -3.6f, 0.15f, 0.25f, 0.62f));
        PartDefinition leftCheekTuft0 = head.addOrReplaceChild("left_cheek_tuft_0", CubeListBuilder.create()
                .texOffs(28, 28).mirror(true).addBox(-0.8f, 0, -0.9f, 1.6f, 3.8f, 1.8f), PartPose.offsetAndRotation(3.5f, -3.3f, -3.6f, 0.15f, -0.25f, -0.62f));
        PartDefinition rightCheekTuft1 = head.addOrReplaceChild("right_cheek_tuft_1", CubeListBuilder.create()
                .texOffs(28, 28).mirror(false).addBox(-0.8f, 0, -0.9f, 1.6f, 3.6f, 1.8f), PartPose.offsetAndRotation(-3.9f, -2.2f, -2.6f, 0.22f, 0.35f, 0.95f));
        PartDefinition leftCheekTuft1 = head.addOrReplaceChild("left_cheek_tuft_1", CubeListBuilder.create()
                .texOffs(28, 28).mirror(true).addBox(-0.8f, 0, -0.9f, 1.6f, 3.6f, 1.8f), PartPose.offsetAndRotation(3.9f, -2.2f, -2.6f, 0.22f, -0.35f, -0.95f));
        PartDefinition rightCheekTuft2 = head.addOrReplaceChild("right_cheek_tuft_2", CubeListBuilder.create()
                .texOffs(15, 37).mirror(false).addBox(-0.7f, 0, -0.8f, 1.4f, 3, 1.6f), PartPose.offsetAndRotation(-3.2f, -0.9f, -3.5f, 0.45f, 0.2f, 0.55f));
        PartDefinition leftCheekTuft2 = head.addOrReplaceChild("left_cheek_tuft_2", CubeListBuilder.create()
                .texOffs(15, 37).mirror(true).addBox(-0.7f, 0, -0.8f, 1.4f, 3, 1.6f), PartPose.offsetAndRotation(3.2f, -0.9f, -3.5f, 0.45f, -0.2f, -0.55f));
        PartDefinition rightBrowTuft = head.addOrReplaceChild("right_brow_tuft", CubeListBuilder.create()
                .texOffs(15, 37).mirror(false).addBox(-0.6f, 0, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(-3.6f, -6.9f, -3.9f, 0, 0.3f, 1.2f));
        PartDefinition leftBrowTuft = head.addOrReplaceChild("left_brow_tuft", CubeListBuilder.create()
                .texOffs(15, 37).mirror(true).addBox(-0.6f, 0, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(3.6f, -6.9f, -3.9f, 0, -0.3f, -1.2f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(0, 28).mirror(false).addBox(-0.45f, -1.8f, -1.8f, 0.9f, 3.6f, 3.6f), PartPose.offsetAndRotation(-5, -4.7f, -0.2f, 0.1f, -0.5f, 0.15f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(0, 28).mirror(true).addBox(-0.45f, -1.8f, -1.8f, 0.9f, 3.6f, 3.6f), PartPose.offsetAndRotation(5, -4.7f, -0.2f, 0.1f, 0.5f, -0.15f));
        PartDefinition rightEarTuft = head.addOrReplaceChild("right_ear_tuft", CubeListBuilder.create()
                .texOffs(15, 37).mirror(false).addBox(-0.6f, 0, -0.7f, 1.2f, 2.6f, 1.4f), PartPose.offsetAndRotation(-4.6f, -6.1f, 0.9f, 0.5f, 0, 0.5f));
        PartDefinition leftEarTuft = head.addOrReplaceChild("left_ear_tuft", CubeListBuilder.create()
                .texOffs(15, 37).mirror(true).addBox(-0.6f, 0, -0.7f, 1.2f, 2.6f, 1.4f), PartPose.offsetAndRotation(4.6f, -6.1f, 0.9f, 0.5f, -0, -0.5f));
        return LayerDefinition.create(mesh, 64, 64);
        // GENERATED head: end
    }
}
