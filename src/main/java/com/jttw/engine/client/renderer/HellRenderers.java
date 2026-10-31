package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.hell.HellKings;
import com.jttw.engine.entity.hell.HorseFaceEntity;
import com.jttw.engine.entity.hell.ImpermanenceEntity;
import com.jttw.engine.entity.hell.JudgeClerkEntity;
import com.jttw.engine.entity.hell.KingOfHellEntity;
import com.jttw.engine.entity.hell.KingYamaEntity;
import com.jttw.engine.entity.hell.OxHeadEntity;
import com.jttw.engine.entity.hell.WanderingGhostEntity;
import com.jttw.engine.entity.hell.YinSoldierEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

/**
 * How Hell's creatures look: Ox-Head and Horse-Face (a matched pair of towering guards in black-and-gold
 * lamellar armour and tattered red cloaks, an ox's and a horse's head, hooves; a steel trident and a
 * halberd), the Judge-Clerk (a red robe, a winged black cap, brush and ledger), the
 * Black and White Impermanence (long robes, tall hats; the White's long red tongue), the wandering
 * ghosts (pale, legless, trailing away, half there), and the Ten Kings (nine of them on one frame,
 * lesser kings in King Yama's likeness: his tiered judge's robe, great sleeves and mian crown, plainer
 * and a head shorter, each in his own colour with the ivory tablet in one hand and his court's
 * emblem held up in the other; King Yama, the Fifth, a towering frame of his own:
 * a judge-emperor of the dead in a crimson-and-black dragon robe, a ring of hellfire and skulls
 * behind his head), and the Yin soldiers (dark lamellar armour, a crested helmet, a halberd, a grey
 * face and burning eyes). Skins painted by {@code assets_source/hell_guards.py},
 * {@code hell_officials.py}, {@code hell_kings.py} (the nine kings' model too), {@code king_yama.py} (his model too) and
 * {@code yin_soldier.py}.
 */
public final class HellRenderers {

    private HellRenderers() {}

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name), "main");
    }

    public static final ModelLayerLocation OX_LAYER = layer("ox_head"), HORSE_LAYER = layer("horse_face"),
            CLERK_LAYER = layer("judge_clerk"), IMPERMANENCE_LAYER = layer("impermanence"), GHOST_LAYER = layer("wandering_ghost"),
            KING_LAYER = layer("king_of_hell"), YAMA_LAYER = layer("king_yama"), SOLDIER_LAYER = layer("yin_soldier");

    /** A box of steel, for the weapons' small parts (a swatch of the skin at 112, 40). */
    private static CubeListBuilder steel() {
        return CubeListBuilder.create().texOffs(112, 40);
    }

    // ── Ox-Head and Horse-Face ──────────────────────────────────────────

    /**
     * Ox-Head (256x256): hell's enforcer, half again a man's height, in black lamellar armour edged in
     * gold and laced in red (two round mirror plates on the breast, shoulder guards in three tiers
     * bitten by gold beasts' heads, a belt with a gold demon's-face buckle, tassets over a red apron,
     * greaves and knee guards), a tattered red cloak, bare hide arms and an ox's legs and cloven
     * hooves; an ox's head on a thick humped neck: a curly brow, a broad snout with a gold nose ring,
     * burning eyes, long ivory horns ringed and black-tipped, curving out, up and forward; a steel
     * trident in his right hand, a chain and shackle at his hip. Model and skins: {@code assets_source/hell_guards.py}.
     */
    public static LayerDefinition oxLayer() {
        // GENERATED ox: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(142, 67).mirror(false).addBox(-6.2f, -2, -4, 12.4f, 4, 8)
                .texOffs(162, 49).mirror(false).addBox(-7, -3.6f, -4.6f, 14, 3.2f, 9.2f)
                .texOffs(221, 95).mirror(false).addBox(-2.8f, -4.8f, -5.7f, 5.6f, 5.2f, 1.4f)
                .texOffs(7, 122).mirror(false).addBox(-3.7f, -5.8f, -5.4f, 1.2f, 1.8f, 1)
                .texOffs(7, 122).mirror(false).addBox(2.5f, -5.8f, -5.4f, 1.2f, 1.8f, 1), PartPose.offsetAndRotation(0, 2, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(211, 49).mirror(false).addBox(-2.4f, 0, -0.4f, 4.8f, 12.5f, 0.8f), PartPose.offsetAndRotation(0, -0.2f, -4.9f, -0.06f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(185, 67).mirror(false).addBox(-3.2f, 0, -0.6f, 6.4f, 10, 1.2f), PartPose.offsetAndRotation(-4, -0.6f, -4.7f, -0.1f, 0.12f, 0.08f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(185, 67).mirror(true).addBox(-3.2f, 0, -0.6f, 6.4f, 10, 1.2f), PartPose.offsetAndRotation(4, -0.6f, -4.7f, -0.1f, -0.12f, -0.08f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(66, 0).mirror(false).addBox(-1.2f, 0, -4.2f, 1.2f, 9.5f, 8.4f), PartPose.offsetAndRotation(-7.1f, -0.6f, 0, 0, 0, 0.18f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(66, 0).mirror(true).addBox(0, 0, -4.2f, 1.2f, 9.5f, 8.4f), PartPose.offsetAndRotation(7.1f, -0.6f, 0, 0, 0, -0.18f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(25, 67).mirror(false).addBox(-6.2f, 0, -0.6f, 12.4f, 10.5f, 1.2f), PartPose.offsetAndRotation(0, -0.6f, 4.7f, 0.12f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(204, 67).mirror(false).addBox(-0.9f, 0, -0.4f, 1.8f, 11, 0.8f)
                .texOffs(201, 82).mirror(false).addBox(-0.4f, 0.5f, 0.6f, 1.8f, 8.5f, 0.8f), PartPose.offsetAndRotation(-6.4f, -2.6f, -3.8f, 0.02f, 0, 0.1f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(204, 67).mirror(true).addBox(-0.9f, 0, -0.4f, 1.8f, 11, 0.8f)
                .texOffs(201, 82).mirror(true).addBox(-1.4f, 0.5f, 0.6f, 1.8f, 8.5f, 0.8f), PartPose.offsetAndRotation(6.4f, -2.6f, -3.8f, 0.02f, 0, -0.1f));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(185, 0).mirror(false).addBox(-2.9f, 0, -2.9f, 5.8f, 10.5f, 5.8f), PartPose.offsetAndRotation(-4.3f, 2, 0.4f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(185, 0).mirror(true).addBox(-2.9f, 0, -2.9f, 5.8f, 10.5f, 5.8f), PartPose.offsetAndRotation(4.3f, 2, 0.4f, 0, 0, 0));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(224, 49).mirror(false).addBox(-2.4f, 0, -2.4f, 4.8f, 8.5f, 4.8f)
                .texOffs(147, 82).mirror(false).addBox(-2.9f, 0.4f, -3.1f, 5.8f, 7, 3.4f)
                .texOffs(145, 106).mirror(false).addBox(-2.7f, -1.6f, -3.6f, 5.4f, 3.4f, 1.4f)
                .texOffs(102, 95).mirror(false).addBox(-2.7f, 7, -2.7f, 5.4f, 2.2f, 5.4f)
                .texOffs(208, 82).mirror(false).addBox(-2.6f, 8.4f, -3.4f, 2.5f, 3.6f, 5.6f)
                .texOffs(208, 82).mirror(false).addBox(0.1f, 8.4f, -3.4f, 2.5f, 3.6f, 5.6f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(224, 49).mirror(true).addBox(-2.4f, 0, -2.4f, 4.8f, 8.5f, 4.8f)
                .texOffs(147, 82).mirror(true).addBox(-2.9f, 0.4f, -3.1f, 5.8f, 7, 3.4f)
                .texOffs(145, 106).mirror(true).addBox(-2.7f, -1.6f, -3.6f, 5.4f, 3.4f, 1.4f)
                .texOffs(102, 95).mirror(true).addBox(-2.7f, 7, -2.7f, 5.4f, 2.2f, 5.4f)
                .texOffs(208, 82).mirror(true).addBox(0.1f, 8.4f, -3.4f, 2.5f, 3.6f, 5.6f)
                .texOffs(208, 82).mirror(true).addBox(-2.6f, 8.4f, -3.4f, 2.5f, 3.6f, 5.6f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(89, 0).mirror(false).addBox(-7.6f, -15, -4.8f, 15.2f, 8, 9.6f)
                .texOffs(210, 0).mirror(false).addBox(-6.6f, -7.4f, -4.3f, 13.2f, 7.4f, 8.6f)
                .texOffs(9, 0).mirror(false).addBox(-8.2f, -15.4f, -5.5f, 16.4f, 8.4f, 11)
                .texOffs(0, 49).mirror(false).addBox(-7.1f, -7.4f, -4.9f, 14.2f, 6.2f, 9.8f)
                .texOffs(56, 67).mirror(false).addBox(-7.3f, -1.8f, -5.1f, 14.6f, 1.8f, 10.2f)
                .texOffs(133, 115).mirror(false).addBox(-1.5f, -2.4f, -5.7f, 3, 2.4f, 1)
                .texOffs(162, 106).mirror(false).addBox(-7, -14, -6, 4.6f, 4.6f, 0.6f)
                .texOffs(162, 106).mirror(false).addBox(2.4f, -14, -6, 4.6f, 4.6f, 0.6f)
                .texOffs(57, 122).mirror(false).addBox(-2.2f, -13.6f, -5.8f, 4.4f, 1, 0.4f)
                .texOffs(211, 67).mirror(false).addBox(-5, -17, -4.4f, 10, 2.2f, 8.8f)
                .texOffs(0, 82).mirror(false).addBox(-8.4f, -15.8f, -5, 16.8f, 1.2f, 10), PartPose.offsetAndRotation(0, -3, 0, 0, 0, 0));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(90, 49).mirror(false).addBox(-8, 0, -0.5f, 16, 14, 1), PartPose.offsetAndRotation(0, -15.6f, 5.8f, 0.1f, 0, 0));
        PartDefinition capeLower = cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(125, 49).mirror(false).addBox(-8.4f, 0, -0.5f, 16.8f, 13.5f, 1), PartPose.offsetAndRotation(0, 13.6f, 0, 0.08f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 67).mirror(false).addBox(-2.7f, -1.4f, -2.7f, 5.4f, 8, 5.4f)
                .texOffs(0, 106).mirror(false).addBox(-3, 4, -3, 6, 1.4f, 6), PartPose.offsetAndRotation(-10.2f, -13.6f, 0, -0.1f, 0, 0.14f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 67).mirror(true).addBox(-2.7f, -1.4f, -2.7f, 5.4f, 8, 5.4f)
                .texOffs(0, 106).mirror(true).addBox(-3, 4, -3, 6, 1.4f, 6), PartPose.offsetAndRotation(10.2f, -13.6f, 0, -0.1f, 0, -0.14f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(0, 95).mirror(false).addBox(-4.2f, -1.2f, -3.9f, 6.6f, 1.6f, 7.8f)
                .texOffs(168, 82).mirror(false).addBox(-4.8f, 0.3f, -4.3f, 7, 1.6f, 8.6f)
                .texOffs(55, 82).mirror(false).addBox(-5.4f, 1.8f, -4.7f, 7.4f, 2, 9.4f)
                .texOffs(127, 95).mirror(false).addBox(-2.8f, -2.4f, -3.4f, 1.2f, 1.4f, 6.8f)
                .texOffs(31, 95).mirror(false).addBox(-6.1f, -0.2f, -2.1f, 1, 4.2f, 4.2f)
                .texOffs(43, 115).mirror(false).addBox(-6.6f, 1.6f, -1.4f, 0.5f, 1.2f, 2.8f), PartPose.offsetAndRotation(-0.4f, -1.4f, 0, 0, 0, 0.4f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(0, 95).mirror(true).addBox(-2.4f, -1.2f, -3.9f, 6.6f, 1.6f, 7.8f)
                .texOffs(168, 82).mirror(true).addBox(-2.2f, 0.3f, -4.3f, 7, 1.6f, 8.6f)
                .texOffs(55, 82).mirror(true).addBox(-2, 1.8f, -4.7f, 7.4f, 2, 9.4f)
                .texOffs(127, 95).mirror(true).addBox(1.6f, -2.4f, -3.4f, 1.2f, 1.4f, 6.8f)
                .texOffs(31, 95).mirror(true).addBox(5.1f, -0.2f, -2.1f, 1, 4.2f, 4.2f)
                .texOffs(43, 115).mirror(true).addBox(6.1f, 1.6f, -1.4f, 0.5f, 1.2f, 2.8f), PartPose.offsetAndRotation(0.4f, -1.4f, 0, 0, 0, -0.4f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(92, 82).mirror(false).addBox(-2.3f, 0, -2.3f, 4.6f, 7, 4.6f)
                .texOffs(113, 82).mirror(false).addBox(-2.8f, 0.6f, -2.8f, 5.6f, 5.4f, 5.6f), PartPose.offsetAndRotation(0, 6.4f, 0, -0.62f, -0.18f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(92, 82).mirror(true).addBox(-2.3f, 0, -2.3f, 4.6f, 7, 4.6f)
                .texOffs(113, 82).mirror(true).addBox(-2.8f, 0.6f, -2.8f, 5.6f, 5.4f, 5.6f), PartPose.offsetAndRotation(0, 6.4f, 0, -0.62f, 0.18f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(146, 95).mirror(false).addBox(-2.1f, 0, -2.1f, 4.2f, 4, 4.2f), PartPose.offsetAndRotation(0, 7, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(146, 95).mirror(true).addBox(-2.1f, 0, -2.1f, 4.2f, 4, 4.2f), PartPose.offsetAndRotation(0, 7, 0, 0, 0, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(142, 0).mirror(false).addBox(-5.4f, -6.4f, -4.6f, 10.8f, 7.2f, 9.2f)
                .texOffs(109, 67).mirror(false).addBox(-5, -7.6f, 0.6f, 10, 6.8f, 6)
                .texOffs(44, 95).mirror(false).addBox(-3, -4, -6, 6, 7, 2.2f), PartPose.offsetAndRotation(0, -15.6f, -0.8f, 0.15f, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(51, 49).mirror(false).addBox(-5, -8, -4.4f, 10, 7, 8.4f)
                .texOffs(25, 106).mirror(false).addBox(-5.8f, -8.7f, -3.8f, 11.6f, 2.4f, 4.6f)
                .texOffs(175, 106).mirror(false).addBox(-4.5f, -7.4f, -5.6f, 9, 3.8f, 1.6f)
                .texOffs(60, 106).mirror(false).addBox(-3, -10, -5, 6, 2.8f, 4.4f)
                .texOffs(142, 115).mirror(false).addBox(-1.6f, -6.9f, -5.95f, 3.2f, 2.4f, 0.6f)
                .texOffs(63, 95).mirror(false).addBox(-5.4f, -2.8f, -5, 1.8f, 4.2f, 4.8f)
                .texOffs(63, 95).mirror(false).addBox(3.6f, -2.8f, -5, 1.8f, 4.2f, 4.8f), PartPose.offsetAndRotation(0, -6.2f, -1.6f, -0.05f, 0, 0));
        PartDefinition face = head.addOrReplaceChild("face", CubeListBuilder.create()
                .texOffs(167, 95).mirror(false).addBox(-3.7f, -3.4f, -4.4f, 7.4f, 3.8f, 4.8f)
                .texOffs(52, 115).mirror(false).addBox(-3.95f, -2.75f, -4.2f, 0.45f, 0.8f, 3.9f)
                .texOffs(52, 115).mirror(false).addBox(3.5f, -2.75f, -4.2f, 0.45f, 0.8f, 3.9f)
                .texOffs(198, 106).mirror(false).addBox(-1.5f, -3.85f, -4.6f, 3, 0.6f, 4.4f)
                .texOffs(14, 122).mirror(false).addBox(-3, -3.7f, -1.6f, 6, 0.5f, 2)
                .texOffs(115, 106).mirror(false).addBox(-3.4f, -2.9f, -7, 6.8f, 3.7f, 3)
                .texOffs(63, 115).mirror(false).addBox(-3.8f, -2.6f, -7.8f, 7.6f, 3.2f, 0.9f)
                .texOffs(153, 115).mirror(false).addBox(-4.2f, -1.9f, -7.5f, 1.3f, 2, 1.9f)
                .texOffs(153, 115).mirror(false).addBox(2.9f, -1.9f, -7.5f, 1.3f, 2, 1.9f)
                .texOffs(82, 115).mirror(false).addBox(-3.3f, 0.6f, -7.5f, 6.6f, 1, 3.6f)
                .texOffs(31, 122).mirror(false).addBox(-2.6f, 1.3f, -7.1f, 0.8f, 1.9f, 0.9f)
                .texOffs(31, 122).mirror(false).addBox(1.8f, 1.3f, -7.1f, 0.8f, 1.9f, 0.9f), PartPose.offsetAndRotation(0, -1, -4.6f, 0.3f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(194, 95).mirror(false).addBox(-3.3f, 1.25f, -6.8f, 6.6f, 2.15f, 6)
                .texOffs(105, 115).mirror(false).addBox(-2.6f, 3, -6.5f, 5.2f, 1.1f, 2.6f), PartPose.offsetAndRotation(0, -1, -4.6f, 0.3f, 0, 0));
        PartDefinition noseRing = face.addOrReplaceChild("nose_ring", CubeListBuilder.create()
                .texOffs(136, 106).mirror(false).addBox(-3.1f, 0, -0.6f, 1.2f, 5, 1.2f)
                .texOffs(136, 106).mirror(false).addBox(1.9f, 0, -0.6f, 1.2f, 5, 1.2f)
                .texOffs(162, 115).mirror(false).addBox(-3.1f, 5, -0.6f, 6.2f, 1.2f, 1.2f)
                .texOffs(181, 115).mirror(false).addBox(-1, 4.8f, -0.9f, 2, 2, 1.8f), PartPose.offsetAndRotation(0, 0.2f, -8, -0.45f, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(36, 122).mirror(false).addBox(-1.25f, -0.7f, -0.3f, 2.5f, 1.4f, 0.5f), PartPose.offsetAndRotation(-3.95f, -2.95f, -5.5f, 0, 0.38f, 0.18f));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(36, 122).mirror(true).addBox(-1.25f, -0.7f, -0.3f, 2.5f, 1.4f, 0.5f), PartPose.offsetAndRotation(3.95f, -2.95f, -5.5f, 0, -0.38f, -0.18f));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(190, 115).mirror(false).addBox(-2.1f, -0.7f, -0.9f, 3.8f, 1.4f, 1.7f), PartPose.offsetAndRotation(-3.5f, -4.05f, -5.4f, 0.1f, 0.4f, 0.22f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(190, 115).mirror(true).addBox(-1.7f, -0.7f, -0.9f, 3.8f, 1.4f, 1.7f), PartPose.offsetAndRotation(3.5f, -4.05f, -5.4f, 0.1f, -0.4f, -0.22f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(215, 106).mirror(false).addBox(-4.6f, -1, -1.6f, 4.6f, 2, 3.2f), PartPose.offsetAndRotation(-5, -4.6f, -0.4f, 0.1f, 0.3f, -0.4f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(215, 106).mirror(true).addBox(0, -1, -1.6f, 4.6f, 2, 3.2f), PartPose.offsetAndRotation(5, -4.6f, -0.4f, 0.1f, -0.3f, 0.4f));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(83, 106).mirror(false).addBox(-5, -1.9f, -1.9f, 5, 3.8f, 3.8f)
                .texOffs(78, 95).mirror(false).addBox(-2.4f, -2.2f, -2.2f, 1.6f, 4.4f, 4.4f), PartPose.offsetAndRotation(-5.2f, -6.6f, -1, 0.1f, -0.1f, 0.08f));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(83, 106).mirror(true).addBox(0, -1.9f, -1.9f, 5, 3.8f, 3.8f)
                .texOffs(78, 95).mirror(true).addBox(0.8f, -2.2f, -2.2f, 1.6f, 4.4f, 4.4f), PartPose.offsetAndRotation(5.2f, -6.6f, -1, 0.1f, 0.1f, -0.08f));
        PartDefinition rightHorn2 = rightHorn.addOrReplaceChild("right_horn_2", CubeListBuilder.create()
                .texOffs(234, 106).mirror(false).addBox(-4.8f, -1.5f, -1.5f, 4.8f, 3, 3), PartPose.offsetAndRotation(-4.7f, 0, 0, 0, -0.12f, 0.45f));
        PartDefinition leftHorn2 = leftHorn.addOrReplaceChild("left_horn_2", CubeListBuilder.create()
                .texOffs(234, 106).mirror(true).addBox(0, -1.5f, -1.5f, 4.8f, 3, 3), PartPose.offsetAndRotation(4.7f, 0, 0, 0, 0.12f, -0.45f));
        PartDefinition rightHorn3 = rightHorn2.addOrReplaceChild("right_horn_3", CubeListBuilder.create()
                .texOffs(0, 115).mirror(false).addBox(-4.2f, -1.1f, -1.1f, 4.2f, 2.2f, 2.2f), PartPose.offsetAndRotation(-4.5f, 0, 0, 0, -0.16f, 0.6f));
        PartDefinition leftHorn3 = leftHorn2.addOrReplaceChild("left_horn_3", CubeListBuilder.create()
                .texOffs(0, 115).mirror(true).addBox(0, -1.1f, -1.1f, 4.2f, 2.2f, 2.2f), PartPose.offsetAndRotation(4.5f, 0, 0, 0, 0.16f, -0.6f));
        PartDefinition rightHorn4 = rightHorn3.addOrReplaceChild("right_horn_4", CubeListBuilder.create()
                .texOffs(203, 115).mirror(false).addBox(-3.6f, -0.7f, -0.7f, 3.6f, 1.4f, 1.4f), PartPose.offsetAndRotation(-3.9f, 0, 0, 0, -0.2f, 0.55f));
        PartDefinition leftHorn4 = leftHorn3.addOrReplaceChild("left_horn_4", CubeListBuilder.create()
                .texOffs(203, 115).mirror(true).addBox(0, -0.7f, -0.7f, 3.6f, 1.4f, 1.4f), PartPose.offsetAndRotation(3.9f, 0, 0, 0, 0.2f, -0.55f));
        PartDefinition trident = rightHand.addOrReplaceChild("trident", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.6f, -31, -0.6f, 1.2f, 46, 1.2f)
                .texOffs(216, 115).mirror(false).addBox(-4.6f, -33.2f, -0.8f, 9.2f, 1.6f, 1.6f)
                .texOffs(17, 115).mirror(false).addBox(-1.4f, -32.4f, -1.4f, 2.8f, 2.8f, 2.8f)
                .texOffs(138, 82).mirror(false).addBox(-0.65f, -42.4f, -0.65f, 1.3f, 9.4f, 1.3f)
                .texOffs(241, 115).mirror(false).addBox(-0.45f, -44.6f, -0.45f, 0.9f, 2.2f, 0.9f)
                .texOffs(246, 115).mirror(false).addBox(-0.7f, -31.2f, -0.7f, 1.4f, 1.4f, 1.4f)
                .texOffs(102, 106).mirror(false).addBox(-1.5f, -29.6f, -1.5f, 3, 4.8f, 3)
                .texOffs(124, 115).mirror(false).addBox(-0.6f, 15, -0.6f, 1.2f, 3, 1.2f)
                .texOffs(246, 115).mirror(false).addBox(-0.75f, 2, -0.75f, 1.5f, 1.2f, 1.5f), PartPose.offsetAndRotation(0, 2.2f, 0, 0.7471f, 0.2384f, -0.0021f));
        PartDefinition prongR = trident.addOrReplaceChild("prong_r", CubeListBuilder.create()
                .texOffs(93, 95).mirror(false).addBox(-0.6f, -8, -0.6f, 1.2f, 8, 1.2f)
                .texOffs(45, 122).mirror(false).addBox(-0.4f, -10, -0.4f, 0.8f, 2, 0.8f), PartPose.offsetAndRotation(-4, -33, 0, 0, 0, -0.18f));
        PartDefinition prongL = trident.addOrReplaceChild("prong_l", CubeListBuilder.create()
                .texOffs(93, 95).mirror(false).addBox(-0.6f, -8, -0.6f, 1.2f, 8, 1.2f)
                .texOffs(45, 122).mirror(false).addBox(-0.4f, -10, -0.4f, 0.8f, 2, 0.8f), PartPose.offsetAndRotation(4, -33, 0, 0, 0, 0.18f));
        PartDefinition chain = waist.addOrReplaceChild("chain", CubeListBuilder.create()
                .texOffs(0, 122).mirror(false).addBox(-0.35f, 0, -0.75f, 0.7f, 2, 1.5f)
                .texOffs(50, 122).mirror(false).addBox(-0.75f, 1.8f, -0.35f, 1.5f, 2, 0.7f)
                .texOffs(0, 122).mirror(false).addBox(-0.35f, 3.6f, -0.75f, 0.7f, 2, 1.5f)
                .texOffs(50, 122).mirror(false).addBox(-0.75f, 5.4f, -0.35f, 1.5f, 2, 0.7f)
                .texOffs(0, 122).mirror(false).addBox(-0.35f, 7.2f, -0.75f, 0.7f, 2, 1.5f)
                .texOffs(50, 122).mirror(false).addBox(-0.75f, 9, -0.35f, 1.5f, 2, 0.7f)
                .texOffs(30, 115).mirror(false).addBox(-1.5f, 10.6f, -1.5f, 3, 2.2f, 3), PartPose.offsetAndRotation(7, -2.2f, -2.6f, 0, 0, -0.1f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED ox: end
    }

    /**
     * Horse-Face (256x256): Ox-Head's match in the same armour, leaner; a dark bay horse's head on a
     * long neck: a long face with a pale blaze, flared nostrils, lips drawn back off his teeth, ears
     * laid back, burning eyes under a scowling brow, a black mane down his neck and back over the
     * cloak; a halberd (a spear-point, a crescent blade, a back hook, a red tassel) in his right hand,
     * the chain and soul-hook he hauls the dead in with in his left. Model and skins: {@code assets_source/hell_guards.py}.
     */
    public static LayerDefinition horseLayer() {
        // GENERATED horse: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(175, 67).mirror(false).addBox(-5.4f, -2, -3.6f, 10.8f, 4, 7.2f)
                .texOffs(59, 67).mirror(false).addBox(-6.2f, -3.6f, -4.2f, 12.4f, 3.2f, 8.4f)
                .texOffs(45, 107).mirror(false).addBox(-2.8f, -4.8f, -5.3f, 5.6f, 5.2f, 1.4f)
                .texOffs(99, 117).mirror(false).addBox(-3.7f, -5.8f, -5, 1.2f, 1.8f, 1)
                .texOffs(99, 117).mirror(false).addBox(2.5f, -5.8f, -5, 1.2f, 1.8f, 1), PartPose.offsetAndRotation(0, 2, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(0, 67).mirror(false).addBox(-2.4f, 0, -0.4f, 4.8f, 12.5f, 0.8f), PartPose.offsetAndRotation(0, -0.2f, -4.5f, -0.06f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(214, 67).mirror(false).addBox(-3.2f, 0, -0.6f, 6.4f, 10, 1.2f), PartPose.offsetAndRotation(-3.6f, -0.6f, -4.3f, -0.1f, 0.12f, 0.08f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(214, 67).mirror(true).addBox(-3.2f, 0, -0.6f, 6.4f, 10, 1.2f), PartPose.offsetAndRotation(3.6f, -0.6f, -4.3f, -0.1f, -0.12f, -0.08f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(93, 0).mirror(false).addBox(-1.2f, 0, -3.8f, 1.2f, 9.5f, 7.6f), PartPose.offsetAndRotation(-6.3f, -0.6f, 0, 0, 0, 0.18f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(93, 0).mirror(true).addBox(0, 0, -3.8f, 1.2f, 9.5f, 7.6f), PartPose.offsetAndRotation(6.3f, -0.6f, 0, 0, 0, -0.18f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(104, 67).mirror(false).addBox(-5.4f, 0, -0.6f, 10.8f, 10.5f, 1.2f), PartPose.offsetAndRotation(0, -0.6f, 4.3f, 0.12f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(233, 67).mirror(false).addBox(-0.9f, 0, -0.4f, 1.8f, 11, 0.8f)
                .texOffs(114, 95).mirror(false).addBox(-0.4f, 0.5f, 0.6f, 1.8f, 8.5f, 0.8f), PartPose.offsetAndRotation(-5.6f, -2.6f, -3.4f, 0.02f, 0, 0.1f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(233, 67).mirror(true).addBox(-0.9f, 0, -0.4f, 1.8f, 11, 0.8f)
                .texOffs(114, 95).mirror(true).addBox(-1.4f, 0.5f, 0.6f, 1.8f, 8.5f, 0.8f), PartPose.offsetAndRotation(5.6f, -2.6f, -3.4f, 0.02f, 0, -0.1f));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(136, 0).mirror(false).addBox(-2.9f, 0, -2.9f, 5.8f, 10.5f, 5.8f), PartPose.offsetAndRotation(-3.9f, 2, 0.4f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(136, 0).mirror(true).addBox(-2.9f, 0, -2.9f, 5.8f, 10.5f, 5.8f), PartPose.offsetAndRotation(3.9f, 2, 0.4f, 0, 0, 0));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(13, 67).mirror(false).addBox(-2.4f, 0, -2.4f, 4.8f, 8.5f, 4.8f)
                .texOffs(209, 82).mirror(false).addBox(-2.9f, 0.4f, -3.1f, 5.8f, 7, 3.4f)
                .texOffs(171, 107).mirror(false).addBox(-2.7f, -1.6f, -3.6f, 5.4f, 3.4f, 1.4f)
                .texOffs(201, 95).mirror(false).addBox(-2.7f, 7, -2.7f, 5.4f, 2.2f, 5.4f)
                .texOffs(121, 95).mirror(false).addBox(-2.6f, 8.4f, -3.2f, 5.2f, 3.6f, 5.6f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(13, 67).mirror(true).addBox(-2.4f, 0, -2.4f, 4.8f, 8.5f, 4.8f)
                .texOffs(209, 82).mirror(true).addBox(-2.9f, 0.4f, -3.1f, 5.8f, 7, 3.4f)
                .texOffs(171, 107).mirror(true).addBox(-2.7f, -1.6f, -3.6f, 5.4f, 3.4f, 1.4f)
                .texOffs(201, 95).mirror(true).addBox(-2.7f, 7, -2.7f, 5.4f, 2.2f, 5.4f)
                .texOffs(121, 95).mirror(true).addBox(-2.6f, 8.4f, -3.2f, 5.2f, 3.6f, 5.6f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(161, 0).mirror(false).addBox(-6.8f, -15, -4.4f, 13.6f, 8, 8.8f)
                .texOffs(35, 49).mirror(false).addBox(-5.8f, -7.4f, -3.9f, 11.6f, 7.4f, 7.8f)
                .texOffs(40, 0).mirror(false).addBox(-7.4f, -15.4f, -5.1f, 14.8f, 8.4f, 10.2f)
                .texOffs(76, 49).mirror(false).addBox(-6.3f, -7.4f, -4.5f, 12.6f, 6.2f, 9)
                .texOffs(0, 82).mirror(false).addBox(-6.5f, -1.8f, -4.7f, 13, 1.8f, 9.4f)
                .texOffs(48, 117).mirror(false).addBox(-1.5f, -2.4f, -5.3f, 3, 2.4f, 1)
                .texOffs(188, 107).mirror(false).addBox(-6.2f, -14, -5.6f, 4.6f, 4.6f, 0.6f)
                .texOffs(188, 107).mirror(false).addBox(1.6f, -14, -5.6f, 4.6f, 4.6f, 0.6f)
                .texOffs(113, 117).mirror(false).addBox(-2.2f, -13.6f, -5.4f, 4.4f, 1, 0.4f)
                .texOffs(0, 95).mirror(false).addBox(-5, -17, -4, 10, 2.2f, 8)
                .texOffs(47, 82).mirror(false).addBox(-7.6f, -15.8f, -4.6f, 15.2f, 1.2f, 9.2f), PartPose.offsetAndRotation(0, -3, 0, 0, 0, 0));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(169, 49).mirror(false).addBox(-7.2f, 0, -0.5f, 14.4f, 14, 1), PartPose.offsetAndRotation(0, -15.6f, 5.4f, 0.1f, 0, 0));
        PartDefinition capeLower = cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(202, 49).mirror(false).addBox(-7.6f, 0, -0.5f, 15.2f, 13.5f, 1), PartPose.offsetAndRotation(0, 13.6f, 0, 0.08f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(34, 67).mirror(false).addBox(-2.7f, -1.4f, -2.7f, 5.4f, 8, 5.4f)
                .texOffs(62, 107).mirror(false).addBox(-3, 4, -3, 6, 1.4f, 6), PartPose.offsetAndRotation(-9.4f, -13.6f, 0, -0.1f, 0, 0.14f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(34, 67).mirror(true).addBox(-2.7f, -1.4f, -2.7f, 5.4f, 8, 5.4f)
                .texOffs(62, 107).mirror(true).addBox(-3, 4, -3, 6, 1.4f, 6), PartPose.offsetAndRotation(9.4f, -13.6f, 0, -0.1f, 0, -0.14f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(146, 95).mirror(false).addBox(-4.2f, -1.2f, -3.9f, 6.6f, 1.6f, 7.8f)
                .texOffs(37, 95).mirror(false).addBox(-4.8f, 0.3f, -4.3f, 7, 1.6f, 8.6f)
                .texOffs(100, 82).mirror(false).addBox(-5.4f, 1.8f, -4.7f, 7.4f, 2, 9.4f)
                .texOffs(226, 95).mirror(false).addBox(-2.8f, -2.4f, -3.4f, 1.2f, 1.4f, 6.8f)
                .texOffs(177, 95).mirror(false).addBox(-6.1f, -0.2f, -2.1f, 1, 4.2f, 4.2f)
                .texOffs(235, 107).mirror(false).addBox(-6.6f, 1.6f, -1.4f, 0.5f, 1.2f, 2.8f), PartPose.offsetAndRotation(-0.4f, -1.4f, 0, 0, 0, 0.4f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(146, 95).mirror(true).addBox(-2.4f, -1.2f, -3.9f, 6.6f, 1.6f, 7.8f)
                .texOffs(37, 95).mirror(true).addBox(-2.2f, 0.3f, -4.3f, 7, 1.6f, 8.6f)
                .texOffs(100, 82).mirror(true).addBox(-2, 1.8f, -4.7f, 7.4f, 2, 9.4f)
                .texOffs(226, 95).mirror(true).addBox(1.6f, -2.4f, -3.4f, 1.2f, 1.4f, 6.8f)
                .texOffs(177, 95).mirror(true).addBox(5.1f, -0.2f, -2.1f, 1, 4.2f, 4.2f)
                .texOffs(235, 107).mirror(true).addBox(6.1f, 1.6f, -1.4f, 0.5f, 1.2f, 2.8f), PartPose.offsetAndRotation(0.4f, -1.4f, 0, 0, 0, -0.4f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(137, 82).mirror(false).addBox(-2.3f, 0, -2.3f, 4.6f, 7, 4.6f)
                .texOffs(158, 82).mirror(false).addBox(-2.8f, 0.6f, -2.8f, 5.6f, 5.4f, 5.6f), PartPose.offsetAndRotation(0, 6.4f, 0, -0.62f, -0.18f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(137, 82).mirror(true).addBox(-2.3f, 0, -2.3f, 4.6f, 7, 4.6f)
                .texOffs(158, 82).mirror(true).addBox(-2.8f, 0.6f, -2.8f, 5.6f, 5.4f, 5.6f), PartPose.offsetAndRotation(0, 6.4f, 0, -0.62f, 0.18f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(0, 107).mirror(false).addBox(-2.1f, 0, -2.1f, 4.2f, 4, 4.2f), PartPose.offsetAndRotation(0, 7, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(0, 107).mirror(true).addBox(-2.1f, 0, -2.1f, 4.2f, 4, 4.2f), PartPose.offsetAndRotation(0, 7, 0, 0, 0, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-3.2f, -12.6f, -3.6f, 6.4f, 13.4f, 7.2f)
                .texOffs(183, 82).mirror(false).addBox(-2.6f, -12, -4.3f, 5.2f, 11, 1), PartPose.offsetAndRotation(0, -15.6f, -0.4f, 0.2f, 0, 0));
        PartDefinition mane0 = neck.addOrReplaceChild("mane_0", CubeListBuilder.create()
                .texOffs(87, 107).mirror(false).addBox(-1.2f, 0, -1, 2.4f, 6, 2), PartPose.offsetAndRotation(0, -12.2f, 3.4f, 0.75f, 0, 0));
        PartDefinition mane1 = neck.addOrReplaceChild("mane_1", CubeListBuilder.create()
                .texOffs(21, 107).mirror(false).addBox(-1.2f, 0, -1, 2.4f, 7, 2), PartPose.offsetAndRotation(-0.8f, -10.6f, 3.4f, 0.6f, 0, 0.12f));
        PartDefinition mane2 = neck.addOrReplaceChild("mane_2", CubeListBuilder.create()
                .texOffs(190, 95).mirror(false).addBox(-1.2f, 0, -1, 2.4f, 8, 2), PartPose.offsetAndRotation(0.8f, -9, 3.4f, 0.5f, 0, -0.12f));
        PartDefinition mane3 = neck.addOrReplaceChild("mane_3", CubeListBuilder.create()
                .texOffs(198, 82).mirror(false).addBox(-1.2f, 0, -1, 2.4f, 9.5f, 2), PartPose.offsetAndRotation(-0.6f, -7.4f, 3.4f, 0.4f, 0, 0.12f));
        PartDefinition mane4 = neck.addOrReplaceChild("mane_4", CubeListBuilder.create()
                .texOffs(131, 67).mirror(false).addBox(-1.2f, 0, -1, 2.4f, 11, 2), PartPose.offsetAndRotation(0.6f, -5.8f, 3.4f, 0.32f, 0, -0.12f));
        PartDefinition mane5 = neck.addOrReplaceChild("mane_5", CubeListBuilder.create()
                .texOffs(237, 49).mirror(false).addBox(-1.2f, 0, -1, 2.4f, 12.5f, 2), PartPose.offsetAndRotation(-0.2f, -4.2f, 3.4f, 0.24f, 0, 0.12f));
        PartDefinition mane6 = neck.addOrReplaceChild("mane_6", CubeListBuilder.create()
                .texOffs(121, 49).mirror(false).addBox(-1.2f, 0, -1, 2.4f, 14, 2), PartPose.offsetAndRotation(0.7f, -2.6f, 3.4f, 0.16f, 0, -0.12f));
        PartDefinition mane7 = neck.addOrReplaceChild("mane_7", CubeListBuilder.create()
                .texOffs(114, 0).mirror(false).addBox(-1.2f, 0, -1, 2.4f, 15.5f, 2), PartPose.offsetAndRotation(-0.8f, -1, 3.4f, 0.1f, 0, 0.12f));
        PartDefinition mane8 = neck.addOrReplaceChild("mane_8", CubeListBuilder.create()
                .texOffs(125, 0).mirror(false).addBox(-1.2f, 0, -1, 2.4f, 16, 2), PartPose.offsetAndRotation(0.2f, 0.4f, 3.4f, 0.06f, 0, -0.12f));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(208, 0).mirror(false).addBox(-3.8f, -5.2f, -4.6f, 7.6f, 7.4f, 8.2f)
                .texOffs(142, 67).mirror(false).addBox(-4.2f, -1.6f, -3.8f, 8.4f, 5.2f, 6.2f)
                .texOffs(0, 49).mirror(false).addBox(-2.9f, -4.6f, -15.4f, 5.8f, 5.4f, 10.8f)
                .texOffs(98, 107).mirror(false).addBox(-3.2f, -3.6f, -17.2f, 6.4f, 4.4f, 2.8f)
                .texOffs(201, 107).mirror(false).addBox(-2.6f, 0.8f, -16.6f, 5.2f, 1, 5)
                .texOffs(70, 95).mirror(false).addBox(-4.1f, -4.4f, -5, 8.2f, 1.5f, 8.8f)
                .texOffs(57, 117).mirror(false).addBox(-1.6f, -4.8f, -5.4f, 3.2f, 2.4f, 0.6f), PartPose.offsetAndRotation(0, -12.2f, -0.6f, 0.5f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(132, 49).mirror(false).addBox(-2.5f, 0, -12.6f, 5, 2.6f, 12.6f)
                .texOffs(0, 117).mirror(false).addBox(-2.3f, -0.7f, -12.4f, 4.6f, 0.8f, 3.6f), PartPose.offsetAndRotation(0, 1.8f, -4, 0.14f, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(19, 117).mirror(false).addBox(-0.6f, -1, -1.4f, 0.9f, 2, 2.8f), PartPose.offsetAndRotation(-3.75f, -3.2f, -4.8f, 0, -0.3f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(19, 117).mirror(true).addBox(-0.3f, -1, -1.4f, 0.9f, 2, 2.8f), PartPose.offsetAndRotation(3.75f, -3.2f, -4.8f, 0, 0.3f, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(156, 107).mirror(false).addBox(-1, -0.7f, -2.1f, 2, 1.4f, 4.4f), PartPose.offsetAndRotation(-3.6f, -4.6f, -4.6f, 0, -0.3f, -0.35f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(156, 107).mirror(true).addBox(-1, -0.7f, -2.1f, 2, 1.4f, 4.4f), PartPose.offsetAndRotation(3.6f, -4.6f, -4.6f, 0, 0.3f, 0.35f));
        PartDefinition rightNostril = head.addOrReplaceChild("right_nostril", CubeListBuilder.create()
                .texOffs(224, 107).mirror(false).addBox(-0.8f, -1, -1, 1.1f, 2.3f, 2.6f), PartPose.offsetAndRotation(-3, -2.6f, -16.6f, 0, 0, 0.2f));
        PartDefinition leftNostril = head.addOrReplaceChild("left_nostril", CubeListBuilder.create()
                .texOffs(224, 107).mirror(true).addBox(-0.3f, -1, -1, 1.1f, 2.3f, 2.6f), PartPose.offsetAndRotation(3, -2.6f, -16.6f, 0, 0, -0.2f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(119, 107).mirror(false).addBox(-1.1f, -5.8f, -0.8f, 2.2f, 5.8f, 1.6f), PartPose.offsetAndRotation(-2.6f, -5, -0.4f, 0.5f, 0, -0.3f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(119, 107).mirror(true).addBox(-1.1f, -5.8f, -0.8f, 2.2f, 5.8f, 1.6f), PartPose.offsetAndRotation(2.6f, -5, -0.4f, 0.5f, 0, 0.3f));
        PartDefinition forelock = head.addOrReplaceChild("forelock", CubeListBuilder.create()
                .texOffs(130, 107).mirror(false).addBox(-1.7f, 0, -0.8f, 3.4f, 6, 1.6f), PartPose.offsetAndRotation(0, -5.2f, -3.2f, -0.9f, 0, 0));
        PartDefinition pollMane = head.addOrReplaceChild("poll_mane", CubeListBuilder.create()
                .texOffs(21, 107).mirror(false).addBox(-1.4f, 0, -0.9f, 2.8f, 7, 1.8f), PartPose.offsetAndRotation(0, -5, 1, 0.6f, 0, 0));
        PartDefinition halberd = rightHand.addOrReplaceChild("halberd", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.55f, -31, -0.55f, 1.1f, 46, 1.1f)
                .texOffs(28, 117).mirror(false).addBox(-1, -32.6f, -1, 2, 2.2f, 2)
                .texOffs(107, 95).mirror(false).addBox(-0.9f, -42, -0.35f, 1.8f, 9.6f, 0.7f)
                .texOffs(68, 117).mirror(false).addBox(-0.7f, -31.6f, -0.7f, 1.4f, 1.4f, 1.4f)
                .texOffs(143, 107).mirror(false).addBox(-1.4f, -30.6f, -1.4f, 2.8f, 4.4f, 2.8f)
                .texOffs(77, 117).mirror(false).addBox(-0.5f, 15, -0.5f, 1, 3, 1)
                .texOffs(68, 117).mirror(false).addBox(-0.7f, 2, -0.7f, 1.4f, 1.2f, 1.4f), PartPose.offsetAndRotation(0, 2.2f, 0, 0.7471f, 0.2384f, -0.0021f));
        PartDefinition crescent = halberd.addOrReplaceChild("crescent", CubeListBuilder.create()
                .texOffs(32, 107).mirror(false).addBox(0, -4, -0.3f, 4.4f, 8, 0.6f), PartPose.offsetAndRotation(0.8f, -36, 0, 0, 0, 0));
        PartDefinition backHook = halberd.addOrReplaceChild("back_hook", CubeListBuilder.create()
                .texOffs(126, 117).mirror(false).addBox(-3.6f, -0.5f, -0.3f, 3.6f, 1, 0.6f)
                .texOffs(82, 117).mirror(false).addBox(-3.6f, -3, -0.3f, 1, 2.6f, 0.6f), PartPose.offsetAndRotation(-0.8f, -36.2f, 0, 0, 0, 0));
        PartDefinition chain = leftHand.addOrReplaceChild("chain", CubeListBuilder.create()
                .texOffs(87, 117).mirror(false).addBox(-0.35f, 0, -0.75f, 0.7f, 2, 1.5f)
                .texOffs(106, 117).mirror(false).addBox(-0.75f, 1.8f, -0.35f, 1.5f, 2, 0.7f)
                .texOffs(87, 117).mirror(false).addBox(-0.35f, 3.6f, -0.75f, 0.7f, 2, 1.5f)
                .texOffs(106, 117).mirror(false).addBox(-0.75f, 5.4f, -0.35f, 1.5f, 2, 0.7f)
                .texOffs(87, 117).mirror(false).addBox(-0.35f, 7.2f, -0.75f, 0.7f, 2, 1.5f)
                .texOffs(106, 117).mirror(false).addBox(-0.75f, 9, -0.35f, 1.5f, 2, 0.7f)
                .texOffs(87, 117).mirror(false).addBox(-0.35f, 10.8f, -0.75f, 0.7f, 2, 1.5f)
                .texOffs(106, 117).mirror(false).addBox(-0.75f, 12.6f, -0.35f, 1.5f, 2, 0.7f)
                .texOffs(87, 117).mirror(false).addBox(-0.35f, 14.4f, -0.75f, 0.7f, 2, 1.5f)
                .texOffs(37, 117).mirror(false).addBox(-0.4f, 16.2f, -2.6f, 0.8f, 1, 3.4f)
                .texOffs(94, 117).mirror(false).addBox(-0.4f, 13.8f, -3, 0.8f, 2.6f, 1), PartPose.offsetAndRotation(0, 3, 0, 0.7071f, -0.2384f, 0.0021f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED horse: end
    }

    /**
     * A guard's frame in motion: the hooves stride, knees bending; the tassets swing with the thighs,
     * the cloak and mane stream behind; the weapon held upright, thrust down on a blow; the head turns
     * on its neck. Ox-Head charging leans in, horns lowered; Horse-Face's lips draw back as he strikes.
     */
    public static final class BeastGuardModel<T extends Mob> extends EntityModel<T> {
        private final ModelPart root, body, neck, head, jaw, rightLeg, leftLeg, rightShin, leftShin, rightArm, leftArm,
                rightForearm, cape, capeLower, apron, rightTasset, leftTasset, backTasset, rightSash, leftSash, chain;
        private final List<ModelPart> mane = new ArrayList<>();

        public BeastGuardModel(ModelPart root) {
            this.root = root;
            ModelPart waist = root.getChild("waist");
            body = waist.getChild("body");
            neck = body.getChild("neck");
            head = neck.getChild("head");
            jaw = head.getChild("jaw");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            cape = body.getChild("cape");
            capeLower = cape.getChild("cape_lower");
            apron = waist.getChild("apron");
            rightTasset = waist.getChild("right_tasset");
            leftTasset = waist.getChild("left_tasset");
            backTasset = waist.getChild("back_tasset");
            rightSash = waist.getChild("right_sash");
            leftSash = waist.getChild("left_sash");
            ModelPart leftHand = leftArm.getChild("left_forearm").getChild("left_hand");
            chain = waist.hasChild("chain") ? waist.getChild("chain") : leftHand.getChild("chain");
            for (int k = 0; neck.hasChild("mane_" + k); k++) mane.add(neck.getChild("mane_" + k));
        }

        private static void turn(ModelPart p, float x, float y, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.yRot = p.getInitialPose().yRot + y;
            p.zRot = p.getInitialPose().zRot + z;
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            boolean charging = e instanceof OxHeadEntity ox && ox.isCharging();
            float w = swing * 0.6662f, stride = Mth.cos(w) * 0.8f * amount;
            // the legs stride, the knee bending as each comes through
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            turn(rightShin, Math.max(0, Mth.sin(w)) * 0.9f * amount, 0, 0);
            turn(leftShin, Math.max(0, -Mth.sin(w)) * 0.9f * amount, 0, 0);
            // the tassets ride up on the thigh that comes forward; the apron and sash ends swing
            turn(rightTasset, Math.min(0, stride) * 0.8f, 0, 0);
            turn(leftTasset, Math.min(0, -stride) * 0.8f, 0, 0);
            turn(backTasset, Math.abs(stride) * 0.35f, 0, 0);
            turn(apron, -amount * 0.3f + Mth.sin(age * 0.07f) * 0.03f, 0, 0);
            turn(rightSash, amount * 0.3f + Mth.sin(age * 0.06f) * 0.05f, 0, 0);
            turn(leftSash, amount * 0.3f + Mth.sin(age * 0.06f + 1) * 0.05f, 0, 0);
            // breathing; the body rocks a little as he strides; charging, he leans into it
            float breath = Mth.sin(age * 0.07f);
            body.y = body.getInitialPose().y + breath * 0.25f;
            turn(body, charging ? 0.4f : 0, Mth.cos(w) * 0.06f * amount, 0);
            // the left arm swings; the weapon arm steadier; a blow thrusts the weapon down and forward
            turn(leftArm, Mth.cos(w) * 0.45f * amount + breath * 0.02f, 0, 0);
            turn(rightArm, -Mth.cos(w) * 0.15f * amount - strike * 1.2f, 0, 0);
            turn(rightForearm, strike * 0.4f, 0, 0);
            if (charging) {
                turn(rightArm, -0.6f, 0, 0);
                turn(leftArm, -0.6f, 0, 0);
            }
            // the head turns on its neck; Ox-Head charging lowers his horns
            float y = yaw * Mth.DEG_TO_RAD, x = pitch * Mth.DEG_TO_RAD;
            turn(neck, charging ? 0.35f : 0, y * 0.4f, 0);
            turn(head, x + (charging ? 0.25f : 0) + breath * 0.02f, y * 0.6f, 0);
            turn(jaw, strike * 0.35f + Math.max(0, Mth.sin(age * 0.045f)) * 0.06f, 0, 0);
            // the cloak and mane stream back as he walks; the chain sways
            turn(cape, amount * 0.45f + breath * 0.03f, 0, 0);
            turn(capeLower, amount * 0.3f + Mth.sin(age * 0.09f) * 0.05f, 0, 0);
            for (int k = 0; k < mane.size(); k++) turn(mane.get(k), amount * 0.25f + Mth.sin(age * 0.08f + k * 0.7f) * 0.04f, 0, 0);
            turn(chain, amount * 0.3f + Mth.sin(age * 0.07f) * 0.05f, 0, Mth.sin(age * 0.05f) * 0.06f);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    /** A guard's renderer: his skin (his captain's), his eyes burning over it full-bright. */
    abstract static class GuardRenderer<T extends Mob> extends MobRenderer<T, BeastGuardModel<T>> {
        private final ResourceLocation skin, captain;
        private final float size;

        GuardRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation layer, String name, float size) {
            super(ctx, new BeastGuardModel<>(ctx.bakeLayer(layer)), 0.8f);
            this.skin = tex(name);
            this.captain = tex(name + "_captain");
            this.size = size;
            RenderType eyes = RenderType.eyes(tex(name + "_eyes"));
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return eyes;
                }
            });
        }

        @Override
        protected void scale(T e, PoseStack pose, float partial) {
            pose.scale(size, size, size);
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return captain(e) ? captain : skin;
        }

        @Override
        protected boolean shouldShowName(T e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    public static final class OxRenderer extends GuardRenderer<OxHeadEntity> {
        /** How much larger than his pixels he is drawn (hell_guards.py prints his reach). */
        public static final float SIZE = 0.78f;

        public OxRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, OX_LAYER, "ox_head", SIZE);
        }
    }

    public static final class HorseRenderer extends GuardRenderer<HorseFaceEntity> {
        /** How much larger than his pixels he is drawn (hell_guards.py prints his reach). */
        public static final float SIZE = 0.78f;

        public HorseRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, HORSE_LAYER, "horse_face", SIZE);
        }
    }

    // ── The Yin soldiers' frame ────────────────────────────────────────

    /** The Yin soldiers' frame (128x64): a person's, in a black cuirass and armoured skirt, pauldrons on the shoulders. */
    private static MeshDefinition guardMesh() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("cuirass", CubeListBuilder.create().texOffs(64, 0).addBox(-4.5f, -0.5f, -2.5f, 9, 8, 5, new CubeDeformation(0.25f)), PartPose.ZERO);
        body.addOrReplaceChild("tassets", CubeListBuilder.create().texOffs(64, 13).addBox(-4.5f, 9, -2.5f, 9, 6, 5, new CubeDeformation(0.35f)), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(92, 0).addBox(-3.5f, -2.5f, -2.5f, 5, 4, 5, new CubeDeformation(0.2f)), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(92, 0).mirror().addBox(-1.5f, -2.5f, -2.5f, 5, 4, 5, new CubeDeformation(0.2f)), PartPose.ZERO);
        return mesh;
    }

    /** A Yin soldier's frame: his halberd held upright. */
    public static final class GuardModel<T extends Mob> extends HumanoidModel<T> {
        public GuardModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing, amount, age, yaw, pitch);
            // the weapon arm held a little forward, steady
            if (attackTime <= 0) rightArm.xRot = rightArm.xRot * 0.3f - 0.35f;
        }
    }

    // ── The Judge-Clerk ─────────────────────────────────────────────────

    /** A person's frame (128x64) in a robe to the ground; a black official's cap, its two wings out to the sides; a brush in his right hand, his ledger in his left. */
    public static LayerDefinition clerkLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        root.getChild("body").addOrReplaceChild("robe", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-5, 10, -3.5f, 10, 14, 7, new CubeDeformation(0.1f)), PartPose.ZERO);
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(64, 22).addBox(-4.5f, -10.5f, -3, 9, 3, 7), PartPose.ZERO);
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(96, 22).addBox(-3.5f, -12.5f, 0, 7, 2, 4), PartPose.ZERO);
        head.addOrReplaceChild("wings", CubeListBuilder.create().texOffs(64, 34).addBox(-11, -11, 3, 22, 2, 1), PartPose.ZERO);
        head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(118, 22).addBox(-1.5f, -1, -4.3f, 3, 4, 1), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("brush", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.5f, 0, -0.5f, 1, 9, 1), PartPose.offsetAndRotation(-1, 10, -1, -1.4f, 0, 0));
        root.getChild("left_arm").addOrReplaceChild("ledger", CubeListBuilder.create().texOffs(64, 40)
                .addBox(-0.5f, 0, -4, 2, 6, 5), PartPose.offset(1, 7, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static final class ClerkModel extends HumanoidModel<JudgeClerkEntity> {
        public ClerkModel(ModelPart root) {
            super(root);
            leftLeg.visible = false;
            rightLeg.visible = false;
        }

        @Override
        public void setupAnim(JudgeClerkEntity e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing * 0.4f, amount, age, yaw, pitch);
            // the ledger held open before him; writing, the brush raised over it
            leftArm.xRot = -0.9f;
            leftArm.yRot = 0.3f;
            if (e.isWriting()) {
                rightArm.xRot = -1.3f + Mth.sin(age * 0.8f) * 0.15f;
                rightArm.yRot = -0.4f;
            } else if (attackTime <= 0) {
                rightArm.xRot = -0.4f + Mth.cos(age * 0.06f) * 0.05f;
            }
        }
    }

    public static final class ClerkRenderer extends HumanoidMobRenderer<JudgeClerkEntity, ClerkModel> {
        private static final ResourceLocation SKIN = tex("judge_clerk"), CAPTAIN = tex("judge_clerk_captain");

        public ClerkRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ClerkModel(ctx.bakeLayer(CLERK_LAYER)), 0.4f);
        }

        @Override
        public ResourceLocation getTextureLocation(JudgeClerkEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected boolean shouldShowName(JudgeClerkEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── The Black and White Impermanence ───────────────────────────────

    /**
     * A person's frame (128x64) in a long robe to the ground, a tall hat rising in three tiers, the
     * White's long red tongue hanging to his chest, a mourning staff in the right hand.
     */
    public static LayerDefinition impermanenceLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        root.getChild("body").addOrReplaceChild("robe", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-5, 10, -3.5f, 10, 14, 7, new CubeDeformation(0.1f)), PartPose.ZERO);
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("hat_1", CubeListBuilder.create().texOffs(64, 22).addBox(-4.5f, -11, -4.5f, 9, 4, 9), PartPose.ZERO);
        head.addOrReplaceChild("hat_2", CubeListBuilder.create().texOffs(64, 36).addBox(-3.5f, -15, -3.5f, 7, 4, 7), PartPose.ZERO);
        head.addOrReplaceChild("hat_3", CubeListBuilder.create().texOffs(92, 36).addBox(-2.5f, -19, -2.5f, 5, 4, 5), PartPose.ZERO);
        head.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(112, 0).addBox(-1, -2.5f, -4.4f, 2, 8, 1), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("staff", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.5f, -12, -0.5f, 1, 24, 1), PartPose.offset(-1, 10, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static final class ImpermanenceModel extends HumanoidModel<ImpermanenceEntity> {
        private final ModelPart tongue;

        public ImpermanenceModel(ModelPart root) {
            super(root);
            leftLeg.visible = false;
            rightLeg.visible = false;
            tongue = head.getChild("tongue");
        }

        @Override
        public void setupAnim(ImpermanenceEntity e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing * 0.4f, amount, age, yaw, pitch);
            tongue.visible = e.white();
            tongue.xRot = -0.05f + Mth.sin(age * 0.12f) * 0.05f;
            // the left hand raised, beckoning; the staff held before him
            leftArm.xRot = -0.6f + Mth.sin(age * 0.09f) * 0.12f;
            if (attackTime <= 0) rightArm.xRot = -0.3f;
            if (e.enraged()) {
                leftArm.xRot = -2.2f;
                rightArm.xRot = -1.6f;
            }
        }
    }

    public static final class ImpermanenceRenderer extends HumanoidMobRenderer<ImpermanenceEntity, ImpermanenceModel> {
        private static final ResourceLocation BLACK = tex("black_impermanence"), WHITE = tex("white_impermanence");

        public ImpermanenceRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ImpermanenceModel(ctx.bakeLayer(IMPERMANENCE_LAYER)), 0.4f);
        }

        @Override
        protected void scale(ImpermanenceEntity e, PoseStack pose, float partial) {
            float s = e.white() ? 1.12f : 0.92f;
            pose.scale(s, s, s);
        }

        @Override
        public ResourceLocation getTextureLocation(ImpermanenceEntity e) {
            return e.white() ? WHITE : BLACK;
        }

        @Override
        protected boolean shouldShowName(ImpermanenceEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── The wandering ghost ─────────────────────────────────────────────

    /** A person's frame (64x64), no legs: the body trails away below the waist in three lessening wisps; long loose hair down the back. */
    public static LayerDefinition ghostLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("trail_1", CubeListBuilder.create().texOffs(0, 32).addBox(-4, 12, -2, 8, 5, 4), PartPose.ZERO);
        body.addOrReplaceChild("trail_2", CubeListBuilder.create().texOffs(24, 32).addBox(-3, 17, -1.5f, 6, 4, 3), PartPose.ZERO);
        body.addOrReplaceChild("trail_3", CubeListBuilder.create().texOffs(44, 32).addBox(-1.5f, 21, -1, 3, 3, 2), PartPose.ZERO);
        root.getChild("head").addOrReplaceChild("hair", CubeListBuilder.create().texOffs(0, 44).addBox(-4, -8, 4, 8, 13, 1, new CubeDeformation(0.3f)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static final class GhostModel extends HumanoidModel<WanderingGhostEntity> {
        float alpha = 0.72f;

        public GhostModel(ModelPart root) {
            super(root, RenderType::entityTranslucent);
            leftLeg.visible = false;
            rightLeg.visible = false;
        }

        @Override
        public void setupAnim(WanderingGhostEntity e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, 0, 0, age, yaw, pitch);
            // arms out before it, groping; the whole of it drifting
            float drift = Mth.sin(age * 0.1f);
            rightArm.xRot = -1.35f + drift * 0.08f;
            leftArm.xRot = -1.35f - drift * 0.08f;
            rightArm.zRot = 0.1f;
            leftArm.zRot = -0.1f;
            body.xRot = 0.08f;
            float bob = drift * 1.2f - 1.5f;
            head.y = bob;
            hat.y = bob;
            body.y = bob;
            rightArm.y = 2 + bob;
            leftArm.y = 2 + bob;
            if (attackTime > 0) {
                rightArm.xRot -= Mth.sin(attackTime * Mth.PI) * 0.6f;
                leftArm.xRot -= Mth.sin(attackTime * Mth.PI) * 0.6f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            super.renderToBuffer(pose, vc, light, overlay, FastColor.ARGB32.color(Math.round(FastColor.ARGB32.alpha(color) * alpha),
                    FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color)));
        }
    }

    public static final class GhostRenderer extends HumanoidMobRenderer<WanderingGhostEntity, GhostModel> {
        private static final ResourceLocation SKIN = tex("wandering_ghost"), CAPTAIN = tex("wandering_ghost_captain");

        public GhostRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new GhostModel(ctx.bakeLayer(GHOST_LAYER)), 0.3f);
        }

        @Override
        public ResourceLocation getTextureLocation(WanderingGhostEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected RenderType getRenderType(WanderingGhostEntity e, boolean visible, boolean translucent, boolean glowing) {
            return RenderType.entityTranslucent(getTextureLocation(e));
        }

        @Override
        protected boolean shouldShowName(WanderingGhostEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── The Yin soldiers ────────────────────────────────────────────────

    /**
     * A soldier's frame (128x64), a person's size: the guards' cuirass, armoured skirt and pauldrons;
     * a helmet with a neck guard and a red crest along its top; a halberd (a ji: a spike, a crescent
     * blade to one side, a red tassel under them) upright in the right hand.
     */
    public static LayerDefinition soldierLayer() {
        MeshDefinition mesh = guardMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("helmet", CubeListBuilder.create().texOffs(64, 24).addBox(-4.5f, -9, -4.5f, 9, 5, 9, new CubeDeformation(0.15f)), PartPose.ZERO);
        head.addOrReplaceChild("neck_guard", CubeListBuilder.create().texOffs(64, 40).addBox(-4.5f, -4, 2.5f, 9, 3, 2, new CubeDeformation(0.2f)), PartPose.ZERO);
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(100, 24).addBox(-0.5f, -12.5f, -3, 1, 4, 6), PartPose.ZERO);
        PartDefinition ji = root.getChild("right_arm").addOrReplaceChild("halberd", CubeListBuilder.create().texOffs(120, 0)
                .addBox(-0.5f, -20, -0.5f, 1, 30, 1), PartPose.offset(-1, 10, 0));
        ji.addOrReplaceChild("spike", steel().addBox(-0.5f, -26, -0.5f, 1, 6, 1), PartPose.ZERO);
        ji.addOrReplaceChild("blade", steel().addBox(0.5f, -23, -0.5f, 3, 2, 1), PartPose.ZERO);
        ji.addOrReplaceChild("blade_tip", steel().addBox(2.5f, -25, -0.5f, 1, 2, 1), PartPose.ZERO);
        ji.addOrReplaceChild("tassel", CubeListBuilder.create().texOffs(100, 40).addBox(-1, -20, -1, 2, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static final class SoldierRenderer extends HumanoidMobRenderer<YinSoldierEntity, GuardModel<YinSoldierEntity>> {
        private static final ResourceLocation SKIN = tex("yin_soldier"), CAPTAIN = tex("yin_soldier_captain");
        private static final RenderType EYES = RenderType.eyes(tex("yin_soldier_eyes"));

        public SoldierRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new GuardModel<>(ctx.bakeLayer(SOLDIER_LAYER)), 0.45f);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return EYES;
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(YinSoldierEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected boolean shouldShowName(YinSoldierEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── The Ten Kings ───────────────────────────────────────────────────

    /**
     * The nine kings but King Yama (256x256, one frame for the nine): lesser judge-kings in his
     * likeness, a head shorter and plainer. His tiered judge's robe, cloud collar, jade belt, apron
     * and side panels, great hanging sleeves and court boots, his mian crown with its plaque and
     * strings of beads; but a man's stern face and a long beard in three locks, a plain cloud-collar
     * plate on each shoulder, the robe plain silk gold only at its edges, no skulls, flames, chain or
     * ring of fire. Each in his own colour, an ivory court tablet in his right hand and his court's
     * emblem in his left (each king's emblem a part of its own, shown only on him), the same emblem in
     * gold on the rank badge on his breast and back. Model and skins: {@code assets_source/hell_kings.py}.
     */
    public static LayerDefinition kingLayer() {
        // GENERATED kings: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-6, -15, -5, 12, 15, 10)
                .texOffs(44, 96).mirror(false).addBox(-3.8f, -16.2f, -3.2f, 7.6f, 1.6f, 6.4f)
                .texOffs(75, 96).mirror(false).addBox(-2.2f, -18.4f, -2.2f, 4.4f, 3.6f, 4.4f)
                .texOffs(178, 66).mirror(false).addBox(-7.4f, -15.6f, -6, 14.8f, 0.8f, 12)
                .texOffs(99, 107).mirror(false).addBox(-6.6f, -15.2f, -6.1f, 13.2f, 5, 0.5f)
                .texOffs(0, 107).mirror(false).addBox(-6.6f, -15.2f, 5.6f, 13.2f, 6, 0.5f)
                .texOffs(0, 66).mirror(false).addBox(-6.8f, -4.4f, -5.9f, 13.6f, 2.2f, 11.8f)
                .texOffs(78, 115).mirror(false).addBox(-2, -5, -6.6f, 4, 3, 0.8f), PartPose.offsetAndRotation(0, 4, 0, 0, 0, 0));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(117, 26).mirror(false).addBox(-6.6f, 0, -5.9f, 13.2f, 7, 11.8f)
                .texOffs(174, 0).mirror(false).addBox(-7.6f, 6.5f, -6.6f, 15.2f, 7, 13.2f)
                .texOffs(45, 0).mirror(false).addBox(-9, 13, -7.6f, 18, 7, 15.2f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition apron = skirt.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(123, 0).mirror(false).addBox(-3.4f, 0, -1, 6.8f, 21, 1), PartPose.offsetAndRotation(0, -1, -6.3f, -0.12f, 0, 0));
        PartDefinition rightPanel = skirt.addOrReplaceChild("right_panel", CubeListBuilder.create()
                .texOffs(140, 0).mirror(false).addBox(-2.2f, 0, -0.5f, 4.4f, 20.5f, 0.5f), PartPose.offsetAndRotation(-5.4f, -1, -6, -0.1f, 0.35f, 0.05f));
        PartDefinition leftPanel = skirt.addOrReplaceChild("left_panel", CubeListBuilder.create()
                .texOffs(140, 0).mirror(true).addBox(-2.2f, 0, -0.5f, 4.4f, 20.5f, 0.5f), PartPose.offsetAndRotation(5.4f, -1, -6, -0.1f, -0.35f, -0.05f));
        PartDefinition backPanel = skirt.addOrReplaceChild("back_panel", CubeListBuilder.create()
                .texOffs(153, 0).mirror(false).addBox(-4.5f, 0, 0, 9, 21, 0.6f), PartPose.offsetAndRotation(0, -1.5f, 6.2f, 0.12f, 0, 0));
        PartDefinition rightBoot = root.addOrReplaceChild("right_boot", CubeListBuilder.create()
                .texOffs(31, 107).mirror(false).addBox(-1.7f, 0, -2.4f, 3.4f, 1.6f, 4.2f)
                .texOffs(153, 115).mirror(false).addBox(-1.2f, -0.9f, -3.2f, 2.4f, 1.4f, 1), PartPose.offsetAndRotation(-3.2f, 22.4f, -7, 0, 0.12f, 0));
        PartDefinition leftBoot = root.addOrReplaceChild("left_boot", CubeListBuilder.create()
                .texOffs(31, 107).mirror(true).addBox(-1.7f, 0, -2.4f, 3.4f, 1.6f, 4.2f)
                .texOffs(153, 115).mirror(true).addBox(-1.2f, -0.9f, -3.2f, 2.4f, 1.4f, 1), PartPose.offsetAndRotation(3.2f, 22.4f, -7, 0, -0.12f, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(53, 66).mirror(false).addBox(-3.4f, -2, -2.8f, 4.8f, 9, 5.6f), PartPose.offsetAndRotation(-7.8f, -13.4f, 0, -0.08f, 0, 0.16f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(53, 66).mirror(true).addBox(-1.4f, -2, -2.8f, 4.8f, 9, 5.6f), PartPose.offsetAndRotation(7.8f, -13.4f, 0, -0.1f, 0, -0.4f));
        PartDefinition rightShoulder = rightArm.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(200, 82).mirror(false).addBox(-4.4f, -1.2f, -3.9f, 5.8f, 2, 7.8f), PartPose.offsetAndRotation(-1.2f, -2.2f, 0, 0, 0, 0.3f));
        PartDefinition leftShoulder = leftArm.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(200, 82).mirror(true).addBox(-1.4f, -1.2f, -3.9f, 5.8f, 2, 7.8f), PartPose.offsetAndRotation(1.2f, -2.2f, 0, 0, 0, -0.06f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(44, 82).mirror(false).addBox(-2.3f, 0, -2.3f, 4.6f, 7, 4.6f)
                .texOffs(134, 96).mirror(false).addBox(-2.7f, 5, -2.7f, 5.4f, 2, 5.4f), PartPose.offsetAndRotation(-1, 6.6f, 0, -1.05f, -0.5f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(44, 82).mirror(true).addBox(-2.3f, 0, -2.3f, 4.6f, 7, 4.6f)
                .texOffs(134, 96).mirror(true).addBox(-2.7f, 5, -2.7f, 5.4f, 2, 5.4f), PartPose.offsetAndRotation(1, 6.6f, 0, -2.2f, -0.35f, 0));
        PartDefinition rightSleeve = rightForearm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(0, 26).mirror(false).addBox(-3.2f, -1, -9, 6.4f, 12, 9), PartPose.offsetAndRotation(0, 0.4f, 2.1f, 1.1769f, 0.1373f, 0.155f));
        PartDefinition leftSleeve = leftForearm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(0, 26).mirror(true).addBox(-3.2f, -1, -9, 6.4f, 12, 9), PartPose.offsetAndRotation(0, 0.4f, 2.1f, 2.2488f, -0.5904f, 0.3688f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(130, 107).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(130, 107).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, 0));
        PartDefinition tablet = rightHand.addOrReplaceChild("tablet", CubeListBuilder.create()
                .texOffs(76, 66).mirror(false).addBox(-1.3f, -12, -0.35f, 2.6f, 14, 0.7f), PartPose.offsetAndRotation(0, 1.6f, -0.2f, 1.2955f, 0.48f, 0.3331f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(66, 48).mirror(false).addBox(-4.4f, -9, -4, 8.8f, 7.4f, 8)
                .texOffs(89, 115).mirror(false).addBox(-1, -5.8f, -4.8f, 2, 2.4f, 0.9f)
                .texOffs(0, 82).mirror(false).addBox(-4.6f, -9.3f, -0.6f, 9.2f, 8, 5), PartPose.offsetAndRotation(0, -18.4f, -0.8f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(162, 115).mirror(false).addBox(-3.8f, -0.9f, -0.5f, 3.8f, 1.1f, 1), PartPose.offsetAndRotation(-0.4f, -6.5f, -4.3f, 0, 0.1f, 0.24f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(162, 115).mirror(true).addBox(0, -0.9f, -0.5f, 3.8f, 1.1f, 1), PartPose.offsetAndRotation(0.4f, -6.5f, -4.3f, 0, -0.1f, -0.24f));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(173, 115).mirror(false).addBox(-3.2f, -0.5f, -0.5f, 3.2f, 1, 1), PartPose.offsetAndRotation(-0.5f, -3.3f, -4.5f, 0, 0.2f, -0.55f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(173, 115).mirror(true).addBox(0, -0.5f, -0.5f, 3.2f, 1, 1), PartPose.offsetAndRotation(0.5f, -3.3f, -4.5f, 0, -0.2f, 0.55f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 96).mirror(false).addBox(-4.1f, 0, -3.4f, 8.2f, 2.6f, 6.8f), PartPose.offsetAndRotation(0, -2, -0.6f, 0, 0, 0));
        PartDefinition beard = jaw.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(96, 82).mirror(false).addBox(-1.4f, 0, -0.9f, 2.8f, 9, 1.8f), PartPose.offsetAndRotation(0, 2, -3.4f, 0.12f, 0, 0));
        PartDefinition beardTip = beard.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(50, 107).mirror(false).addBox(-1, 0, -0.8f, 2, 4.5f, 1.6f), PartPose.offsetAndRotation(0, 8.6f, 0, -0.1f, 0, 0));
        PartDefinition rightBeard = jaw.addOrReplaceChild("right_beard", CubeListBuilder.create()
                .texOffs(33, 96).mirror(false).addBox(-1.1f, 0, -0.8f, 2.2f, 7.5f, 1.6f), PartPose.offsetAndRotation(-2.5f, 1.6f, -3.1f, 0.12f, 0.15f, 0.22f));
        PartDefinition leftBeard = jaw.addOrReplaceChild("left_beard", CubeListBuilder.create()
                .texOffs(33, 96).mirror(true).addBox(-1.1f, 0, -0.8f, 2.2f, 7.5f, 1.6f), PartPose.offsetAndRotation(2.5f, 1.6f, -3.1f, 0.12f, -0.15f, -0.22f));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(65, 82).mirror(false).addBox(-3.6f, -4, -3.4f, 7.2f, 4.2f, 6.8f)
                .texOffs(107, 82).mirror(false).addBox(-4.7f, -0.9f, -4.3f, 9.4f, 1.4f, 8.6f)
                .texOffs(96, 115).mirror(false).addBox(-2, -3.7f, -3.9f, 4, 3, 0.6f)
                .texOffs(184, 115).mirror(false).addBox(-6, -3.3f, -0.35f, 12, 0.7f, 0.7f), PartPose.offsetAndRotation(0, -9, 0, 0, 0, 0));
        PartDefinition board = crown.addOrReplaceChild("board", CubeListBuilder.create()
                .texOffs(101, 48).mirror(false).addBox(-5.8f, -1.4f, -7.6f, 11.6f, 1.4f, 14)
                .texOffs(154, 48).mirror(false).addBox(-6, -0.4f, -7.8f, 12, 0.4f, 14.4f)
                .texOffs(229, 107).mirror(false).addBox(-4.25f, 0, -7.3f, 0.5f, 3.6f, 0.5f)
                .texOffs(229, 107).mirror(false).addBox(-2.25f, 0, -7.3f, 0.5f, 3.6f, 0.5f)
                .texOffs(229, 107).mirror(false).addBox(-0.25f, 0, -7.3f, 0.5f, 3.6f, 0.5f)
                .texOffs(229, 107).mirror(false).addBox(1.75f, 0, -7.3f, 0.5f, 3.6f, 0.5f)
                .texOffs(229, 107).mirror(false).addBox(3.75f, 0, -7.3f, 0.5f, 3.6f, 0.5f)
                .texOffs(229, 107).mirror(false).addBox(-4.25f, 0, 5.8f, 0.5f, 3.6f, 0.5f)
                .texOffs(229, 107).mirror(false).addBox(-2.25f, 0, 5.8f, 0.5f, 3.6f, 0.5f)
                .texOffs(229, 107).mirror(false).addBox(-0.25f, 0, 5.8f, 0.5f, 3.6f, 0.5f)
                .texOffs(229, 107).mirror(false).addBox(1.75f, 0, 5.8f, 0.5f, 3.6f, 0.5f)
                .texOffs(229, 107).mirror(false).addBox(3.75f, 0, 5.8f, 0.5f, 3.6f, 0.5f), PartPose.offsetAndRotation(0, -4, 0, 0.07f, 0, 0));
        PartDefinition emblemQinguang = leftHand.addOrReplaceChild("emblem_qinguang", CubeListBuilder.create()
                .texOffs(159, 96).mirror(false).addBox(-0.91f, -4.42f, -0.91f, 1.82f, 5.72f, 1.82f)
                .texOffs(234, 107).mirror(false).addBox(-3.12f, -5.98f, -1.3f, 6.24f, 1.82f, 2.6f)
                .texOffs(0, 48).mirror(false).addBox(-7.15f, -20.28f, -0.78f, 14.3f, 14.3f, 1.56f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemChujiang = leftHand.addOrReplaceChild("emblem_chujiang", CubeListBuilder.create()
                .texOffs(168, 96).mirror(false).addBox(-0.78f, -2.6f, -0.78f, 1.56f, 5.2f, 1.56f)
                .texOffs(177, 96).mirror(false).addBox(-2.47f, -4.42f, -2.47f, 4.94f, 2.08f, 4.94f)
                .texOffs(170, 26).mirror(false).addBox(-1.82f, -18.2f, -1.82f, 3.64f, 14.04f, 3.64f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemChujiangL = emblemChujiang.addOrReplaceChild("emblem_chujiang_l", CubeListBuilder.create()
                .texOffs(31, 82).mirror(false).addBox(-1.3f, -9.75f, -1.3f, 2.6f, 9.75f, 2.6f), PartPose.offsetAndRotation(1.04f, -4.68f, 0, 0, 0, 0.5f));
        PartDefinition emblemChujiangR = emblemChujiang.addOrReplaceChild("emblem_chujiang_r", CubeListBuilder.create()
                .texOffs(31, 82).mirror(false).addBox(-1.3f, -9.75f, -1.3f, 2.6f, 9.75f, 2.6f), PartPose.offsetAndRotation(-1.04f, -4.68f, 0, 0, 0, -0.5f));
        PartDefinition emblemChujiangF = emblemChujiang.addOrReplaceChild("emblem_chujiang_f", CubeListBuilder.create()
                .texOffs(146, 82).mirror(false).addBox(-1.04f, -7.15f, -1.04f, 2.08f, 7.15f, 2.08f), PartPose.offsetAndRotation(0, -4.68f, -1.04f, -0.45f, 0, 0));
        PartDefinition emblemSongdi = leftHand.addOrReplaceChild("emblem_songdi", CubeListBuilder.create()
                .texOffs(114, 0).mirror(false).addBox(-0.975f, -17.42f, -0.975f, 1.95f, 20.02f, 1.95f)
                .texOffs(0, 115).mirror(false).addBox(-1.43f, -1.3f, -1.43f, 2.86f, 1.3f, 2.86f)
                .texOffs(13, 115).mirror(false).addBox(-0.39f, 2.6f, -0.91f, 0.78f, 2.34f, 1.82f)
                .texOffs(107, 115).mirror(false).addBox(-0.91f, 4.55f, -0.39f, 1.82f, 2.34f, 0.78f)
                .texOffs(13, 115).mirror(false).addBox(-0.39f, 6.5f, -0.91f, 0.78f, 2.34f, 1.82f)
                .texOffs(143, 107).mirror(false).addBox(-1.43f, 8.58f, -1.43f, 2.86f, 2.86f, 2.86f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemSongdiArc0 = emblemSongdi.addOrReplaceChild("emblem_songdi_arc_0", CubeListBuilder.create()
                .texOffs(156, 107).mirror(false).addBox(-1.8969f, -1.235f, -1.04f, 3.7938f, 2.47f, 2.08f), PartPose.offsetAndRotation(0.1938f, -18.3419f, 0, 0, 0, 4.9796f));
        PartDefinition emblemSongdiArc1 = emblemSongdi.addOrReplaceChild("emblem_songdi_arc_1", CubeListBuilder.create()
                .texOffs(156, 107).mirror(false).addBox(-1.8969f, -1.17f, -1.04f, 3.7938f, 2.34f, 2.08f), PartPose.offsetAndRotation(1.6629f, -20.8235f, 0, 0, 0, 5.5141f));
        PartDefinition emblemSongdiArc2 = emblemSongdi.addOrReplaceChild("emblem_songdi_arc_2", CubeListBuilder.create()
                .texOffs(156, 107).mirror(false).addBox(-1.8969f, -1.105f, -1.04f, 3.7938f, 2.21f, 2.08f), PartPose.offsetAndRotation(4.1912f, -22.2105f, 0, 0, 0, 6.0487f));
        PartDefinition emblemSongdiArc3 = emblemSongdi.addOrReplaceChild("emblem_songdi_arc_3", CubeListBuilder.create()
                .texOffs(156, 107).mirror(false).addBox(-1.8969f, -1.04f, -1.04f, 3.7938f, 2.08f, 2.08f), PartPose.offsetAndRotation(7.0734f, -22.1162f, 0, 0, 0, 6.5832f));
        PartDefinition emblemSongdiArc4 = emblemSongdi.addOrReplaceChild("emblem_songdi_arc_4", CubeListBuilder.create()
                .texOffs(20, 115).mirror(false).addBox(-1.8969f, -0.975f, -1.04f, 3.7938f, 1.95f, 2.08f), PartPose.offsetAndRotation(9.5056f, -20.5667f, 0, 0, 0, 7.1177f));
        PartDefinition emblemSongdiArc5 = emblemSongdi.addOrReplaceChild("emblem_songdi_arc_5", CubeListBuilder.create()
                .texOffs(20, 115).mirror(false).addBox(-1.8969f, -0.91f, -1.04f, 3.7938f, 1.82f, 2.08f), PartPose.offsetAndRotation(10.8092f, -17.9944f, 0, 0, 0, 7.6522f));
        PartDefinition emblemSongdiArc6 = emblemSongdi.addOrReplaceChild("emblem_songdi_arc_6", CubeListBuilder.create()
                .texOffs(20, 115).mirror(false).addBox(-1.8969f, -0.845f, -1.04f, 3.7938f, 1.69f, 2.08f), PartPose.offsetAndRotation(10.6206f, -15.1168f, 0, 0, 0, 8.1867f));
        PartDefinition emblemSongdiArc7 = emblemSongdi.addOrReplaceChild("emblem_songdi_arc_7", CubeListBuilder.create()
                .texOffs(20, 115).mirror(false).addBox(-1.8969f, -0.78f, -1.04f, 3.7938f, 1.56f, 2.08f), PartPose.offsetAndRotation(8.9924f, -12.7366f, 0, 0, 0, 8.7212f));
        PartDefinition emblemSongdiPoint = emblemSongdi.addOrReplaceChild("emblem_songdi_point", CubeListBuilder.create()
                .texOffs(198, 96).mirror(false).addBox(-0.78f, -5.46f, -0.78f, 1.56f, 5.98f, 1.56f), PartPose.offsetAndRotation(7.7675f, -11.9516f, 0, 0, 0, 10.5592f));
        PartDefinition emblemSongdiSpur = emblemSongdi.addOrReplaceChild("emblem_songdi_spur", CubeListBuilder.create()
                .texOffs(59, 107).mirror(false).addBox(-0.78f, -3.9f, -0.78f, 1.56f, 4.16f, 1.56f), PartPose.offsetAndRotation(8.58f, -22.304f, 0, 0, 0, 6.8068f));
        PartDefinition emblemWuguan = leftHand.addOrReplaceChild("emblem_wuguan", CubeListBuilder.create()
                .texOffs(187, 26).mirror(false).addBox(-0.78f, -15.6f, -0.78f, 1.56f, 16.9f, 1.56f)
                .texOffs(114, 115).mirror(false).addBox(-8.06f, -17.03f, -0.845f, 16.12f, 1.69f, 1.69f)
                .texOffs(171, 107).mirror(false).addBox(-1.43f, -20.28f, -0.585f, 2.86f, 3.38f, 1.17f)
                .texOffs(68, 107).mirror(false).addBox(-7.54f, -15.34f, -0.26f, 0.52f, 5.72f, 0.52f)
                .texOffs(68, 107).mirror(false).addBox(7.02f, -15.34f, -0.26f, 0.52f, 5.72f, 0.52f)
                .texOffs(207, 96).mirror(false).addBox(-10.14f, -9.62f, -2.86f, 5.72f, 1.3f, 5.72f)
                .texOffs(207, 96).mirror(false).addBox(4.42f, -9.62f, -2.86f, 5.72f, 1.3f, 5.72f)
                .texOffs(182, 107).mirror(false).addBox(-1.82f, 1.04f, -1.82f, 3.64f, 1.3f, 3.64f), PartPose.offsetAndRotation(2.6f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemBiancheng = leftHand.addOrReplaceChild("emblem_biancheng", CubeListBuilder.create()
                .texOffs(73, 107).mirror(false).addBox(-0.91f, -4.42f, -0.91f, 1.82f, 4.94f, 1.82f)
                .texOffs(82, 107).mirror(false).addBox(-1.56f, -6.24f, -1.56f, 3.12f, 2.08f, 3.12f)
                .texOffs(33, 26).mirror(false).addBox(-4.94f, 0.26f, -4.94f, 9.88f, 10.92f, 9.88f)
                .texOffs(85, 66).mirror(false).addBox(-5.72f, 10.92f, -5.72f, 11.44f, 1.56f, 11.44f)
                .texOffs(199, 107).mirror(false).addBox(-1.04f, 12.22f, -1.04f, 2.08f, 2.86f, 2.08f), PartPose.offsetAndRotation(0.4f, 2.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemTaishan = leftHand.addOrReplaceChild("emblem_taishan", CubeListBuilder.create()
                .texOffs(134, 66).mirror(false).addBox(-4.42f, -7.02f, -3.64f, 8.84f, 5.46f, 7.28f)
                .texOffs(159, 82).mirror(false).addBox(-5.07f, -8.32f, -4.29f, 10.14f, 1.3f, 8.58f)
                .texOffs(35, 115).mirror(false).addBox(-4.16f, -11.18f, -0.78f, 1.56f, 2.86f, 1.56f)
                .texOffs(35, 115).mirror(false).addBox(2.6f, -11.18f, -0.78f, 1.56f, 2.86f, 1.56f)
                .texOffs(44, 115).mirror(false).addBox(-3.77f, -1.56f, -3.12f, 1.56f, 2.34f, 1.56f)
                .texOffs(44, 115).mirror(false).addBox(2.21f, -1.56f, -3.12f, 1.56f, 2.34f, 1.56f)
                .texOffs(44, 115).mirror(false).addBox(-3.77f, -1.56f, 1.56f, 1.56f, 2.34f, 1.56f)
                .texOffs(44, 115).mirror(false).addBox(2.21f, -1.56f, 1.56f, 1.56f, 2.34f, 1.56f), PartPose.offsetAndRotation(0.4f, -0.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemTaishanFire = emblemTaishan.addOrReplaceChild("emblem_taishan_fire", CubeListBuilder.create()
                .texOffs(96, 96).mirror(false).addBox(-3.64f, -7.8f, -0.325f, 7.28f, 7.8f, 0.65f)
                .texOffs(209, 48).mirror(false).addBox(-0.325f, -7.8f, -3.64f, 0.65f, 7.8f, 7.28f), PartPose.offsetAndRotation(0, -8.32f, 0, 0, 0.4f, 0));
        PartDefinition emblemDushi = leftHand.addOrReplaceChild("emblem_dushi", CubeListBuilder.create()
                .texOffs(196, 26).mirror(false).addBox(-7.8f, -16.38f, -0.91f, 15.6f, 15.6f, 1.82f)
                .texOffs(115, 96).mirror(false).addBox(-1.56f, -10.14f, -2.08f, 3.12f, 3.12f, 4.16f), PartPose.offsetAndRotation(0.4f, 1.4f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemPingdeng = leftHand.addOrReplaceChild("emblem_pingdeng", CubeListBuilder.create()
                .texOffs(169, 66).mirror(false).addBox(-0.78f, -8.45f, -0.78f, 1.56f, 11.05f, 1.56f)
                .texOffs(35, 48).mirror(false).addBox(-6.24f, -23.4f, -0.78f, 12.48f, 14.95f, 1.56f)
                .texOffs(53, 115).mirror(false).addBox(-4.68f, -25.74f, -0.65f, 9.36f, 2.34f, 1.3f)
                .texOffs(212, 107).mirror(false).addBox(-1.82f, 1.56f, -1.82f, 3.64f, 1.56f, 3.64f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemZhuanlun = leftHand.addOrReplaceChild("emblem_zhuanlun", CubeListBuilder.create()
                .texOffs(74, 26).mirror(false).addBox(-9.1f, -19.5f, -0.65f, 18.2f, 18.2f, 1.3f)
                .texOffs(232, 96).mirror(false).addBox(-1.82f, -12.22f, -1.95f, 3.64f, 3.64f, 3.9f), PartPose.offsetAndRotation(0.4f, 1.4f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED kings: end
    }

    /**
     * A king's pose: the tablet held up before him in his right hand, his emblem in his left;
     * judging, the emblem thrust up high and the tablet raised; raising his sentence on all, both
     * lifted high over his head, trembling; striking, the tablet brought down. His own emblem alone shown.
     */
    public static final class KingModel<T extends KingOfHellEntity> extends EntityModel<T> {
        private final ModelPart root, head, beard, rightBeard, leftBeard, rightArm, leftArm, rightForearm, rightSleeve, leftSleeve,
                apron, rightBoot, leftBoot;
        /** Each king's emblem, by court (none for King Yama's). */
        private final ModelPart[] emblems = new ModelPart[11];

        public KingModel(ModelPart root) {
            this.root = root;
            ModelPart body = root.getChild("body"), skirt = body.getChild("skirt");
            head = body.getChild("head");
            ModelPart jaw = head.getChild("jaw");
            beard = jaw.getChild("beard");
            rightBeard = jaw.getChild("right_beard");
            leftBeard = jaw.getChild("left_beard");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            rightSleeve = rightForearm.getChild("right_sleeve");
            ModelPart leftForearm = leftArm.getChild("left_forearm");
            leftSleeve = leftForearm.getChild("left_sleeve");
            apron = skirt.getChild("apron");
            rightBoot = root.getChild("right_boot");
            leftBoot = root.getChild("left_boot");
            ModelPart leftHand = leftForearm.getChild("left_hand");
            for (int court = 1; court <= 10; court++) {
                if (court == 5) continue;
                emblems[court] = leftHand.getChild("emblem_" + HellKings.IDS.get(court - 1).substring("king_".length()));
            }
        }

        @Override
        public void prepareMobModel(T e, float swing, float amount, float partial) {
            for (int court = 1; court <= 10; court++) if (emblems[court] != null) emblems[court].visible = court == e.court();
        }

        /** A part turned this much from its rest. */
        private static void turn(ModelPart p, float x, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.zRot = p.getInitialPose().zRot + z;
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            head.y = head.getInitialPose().y + Mth.sin(age * 0.06f) * 0.15f;
            // the tablet before him, the emblem in his left; judging, the emblem thrust up and the tablet raised; a blow, the tablet brought down
            float rx = Mth.cos(age * 0.05f) * 0.03f, lx = -rx, rz = 0, lz = 0;
            if (e.isJudging()) {
                rx = -0.9f;
                lx = -0.5f;
            }
            if (e.raising()) {
                // his sentence on all: tablet and emblem lifted high over his head, higher as it gathers, trembling with its weight
                float up = Math.min(1f, e.raised() * 2.5f), shake = Mth.sin(age * 1.7f) * 0.04f * up;
                rx = -2.85f * up + shake;
                lx = -2.85f * up - shake;
                rz = -0.25f * up;
                lz = 0.25f * up;
            }
            rx -= strike * 1.4f;
            turn(rightArm, rx, rz);
            turn(leftArm, lx, lz);
            turn(rightForearm, strike * 0.5f, 0);
            // the great sleeves hang under their weight whatever the arms do
            turn(rightSleeve, -(rx + strike * 0.5f) * 0.85f + Mth.sin(age * 0.05f) * 0.04f + amount * 0.2f, 0);
            turn(leftSleeve, -lx * 0.85f + Mth.sin(age * 0.05f + 1) * 0.04f + amount * 0.2f, 0);
            turn(beard, Mth.sin(age * 0.05f) * 0.03f + amount * 0.15f, 0);
            turn(rightBeard, 0, Mth.sin(age * 0.045f) * 0.03f);
            turn(leftBeard, 0, -Mth.sin(age * 0.045f + 0.5f) * 0.03f);
            turn(apron, -amount * 0.2f + Mth.sin(age * 0.06f) * 0.02f, 0);
            // gliding over the floor, his boots shuffle under the hem
            float step = Mth.cos(swing * 0.6f) * 1.2f * amount;
            rightBoot.z = rightBoot.getInitialPose().z + step;
            leftBoot.z = leftBoot.getInitialPose().z - step;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class KingRenderer<T extends KingOfHellEntity> extends MobRenderer<T, KingModel<T>> {
        /** How much larger than his pixels he is drawn: some 2.7 blocks to his crown, a head under King Yama (hell_kings.py prints his reach). */
        public static final float SIZE = 0.82f;
        private final ResourceLocation skin;

        public KingRenderer(EntityRendererProvider.Context ctx, String name) {
            super(ctx, new KingModel<>(ctx.bakeLayer(KING_LAYER)), 0.6f);
            skin = tex(name);
            // what of his emblem shines (the mirror's glass, the ice, the fire in the cauldron, the wheel's hub), full-bright
            RenderType glow = RenderType.eyes(tex(name + "_glow"));
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, T e,
                                   float swing, float amount, float partial, float age, float yaw, float pitch) {
                    getParentModel().renderToBuffer(pose, buffers.getBuffer(glow), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        /** A renderer for the king of this name (his skin textures/entity/{name}.png, its glow {name}_glow.png). */
        public static <T extends KingOfHellEntity> EntityRendererProvider<T> of(String name) {
            return ctx -> new KingRenderer<>(ctx, name);
        }

        @Override
        protected void scale(T e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return skin;
        }
    }

    // ── King Yama ───────────────────────────────────────────────────────

    /**
     * King Yama (256x256, a frame of his own): a towering judge-emperor in layered dragon robes of
     * crimson and black worked with gold dragons, flames and skulls, armoured shoulders bitten by gold
     * beasts' heads, a jade belt with a skull buckle and a soul-chain at his hip, great hanging
     * sleeves; a dark oxblood face, bulging burning eyes under brows knit into a V, tusks, a wild
     * black beard and moustache flaring out; the mian crown with its 王 plaque and its strings of
     * beads; the vermilion brush and the Book of Life and Death; a ring of hellfire set with skulls
     * behind his head. Model and skins: {@code assets_source/king_yama.py}.
     */
    public static LayerDefinition yamaLayer() {
        // GENERATED yama: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(63, 0).mirror(false).addBox(-7, -18, -6, 14, 18, 12)
                .texOffs(55, 117).mirror(false).addBox(-6.4f, -9.6f, -7.6f, 12.8f, 4.8f, 1.8f)
                .texOffs(0, 106).mirror(false).addBox(-4.5f, -19.4f, -3.6f, 9, 2, 7.2f)
                .texOffs(35, 106).mirror(false).addBox(-2.6f, -22, -2.6f, 5.2f, 4, 5.2f)
                .texOffs(35, 76).mirror(false).addBox(-8.6f, -18.7f, -7, 17.2f, 1, 14)
                .texOffs(86, 117).mirror(false).addBox(-7.6f, -18.2f, -7.1f, 15.2f, 6, 0.5f)
                .texOffs(198, 106).mirror(false).addBox(-7.6f, -18.2f, 6.6f, 15.2f, 7, 0.5f)
                .texOffs(0, 57).mirror(false).addBox(-7.9f, -5, -7.9f, 15.8f, 2.5f, 15)
                .texOffs(241, 117).mirror(false).addBox(-2.5f, -5.8f, -8.4f, 5, 4, 1), PartPose.offsetAndRotation(0, 1, 0, 0, 0, 0));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(101, 32).mirror(false).addBox(-7.6f, 0, -7, 15.2f, 8, 14)
                .texOffs(0, 32).mirror(false).addBox(-8.8f, 7.5f, -7.8f, 17.6f, 8, 15.6f)
                .texOffs(116, 0).mirror(false).addBox(-10.5f, 15, -9, 21, 8, 18), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition apron = skirt.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(69, 32).mirror(false).addBox(-4, 0, -1, 8, 23, 1), PartPose.offsetAndRotation(0, -1, -7.4f, -0.13f, 0, 0));
        PartDefinition rightPanel = skirt.addOrReplaceChild("right_panel", CubeListBuilder.create()
                .texOffs(88, 32).mirror(false).addBox(-2.5f, 0, -0.5f, 5, 22, 0.5f), PartPose.offsetAndRotation(-6.2f, -1, -6.6f, -0.1f, 0.35f, 0.05f));
        PartDefinition leftPanel = skirt.addOrReplaceChild("left_panel", CubeListBuilder.create()
                .texOffs(88, 32).mirror(true).addBox(-2.5f, 0, -0.5f, 5, 22, 0.5f), PartPose.offsetAndRotation(6.2f, -1, -6.6f, -0.1f, -0.35f, -0.05f));
        PartDefinition backPanel = skirt.addOrReplaceChild("back_panel", CubeListBuilder.create()
                .texOffs(195, 0).mirror(false).addBox(-5, 0, 0, 10, 23.5f, 0.6f), PartPose.offsetAndRotation(0, -1.5f, 7.3f, 0.12f, 0, 0));
        PartDefinition backFlap = skirt.addOrReplaceChild("back_flap", CubeListBuilder.create()
                .texOffs(0, 93).mirror(false).addBox(-6.5f, 0, 0, 13, 11, 0.6f), PartPose.offsetAndRotation(0, -2.5f, 8.1f, 0.2f, 0, 0));
        PartDefinition rightBackPanel = skirt.addOrReplaceChild("right_back_panel", CubeListBuilder.create()
                .texOffs(88, 32).mirror(false).addBox(-2.5f, 0, 0, 5, 22, 0.5f), PartPose.offsetAndRotation(-6.4f, -1, 6.6f, 0.1f, -0.35f, 0.05f));
        PartDefinition leftBackPanel = skirt.addOrReplaceChild("left_back_panel", CubeListBuilder.create()
                .texOffs(88, 32).mirror(true).addBox(-2.5f, 0, 0, 5, 22, 0.5f), PartPose.offsetAndRotation(6.4f, -1, 6.6f, 0.1f, 0.35f, -0.05f));
        PartDefinition rightPendant = skirt.addOrReplaceChild("right_pendant", CubeListBuilder.create()
                .texOffs(174, 117).mirror(false).addBox(-0.4f, 0, -0.4f, 0.8f, 5, 0.8f)
                .texOffs(0, 126).mirror(false).addBox(-1.6f, 5, -0.7f, 3.2f, 3.2f, 1)
                .texOffs(11, 126).mirror(false).addBox(-0.6f, 8.2f, -0.5f, 1.2f, 4, 1), PartPose.offsetAndRotation(-5.3f, -1.6f, -7.9f, -0.14f, 0, 0.04f));
        PartDefinition leftPendant = skirt.addOrReplaceChild("left_pendant", CubeListBuilder.create()
                .texOffs(174, 117).mirror(true).addBox(-0.4f, 0, -0.4f, 0.8f, 5, 0.8f)
                .texOffs(0, 126).mirror(true).addBox(-1.6f, 5, -0.7f, 3.2f, 3.2f, 1)
                .texOffs(11, 126).mirror(true).addBox(-0.6f, 8.2f, -0.5f, 1.2f, 4, 1), PartPose.offsetAndRotation(5.3f, -1.6f, -7.9f, -0.14f, 0, -0.04f));
        PartDefinition chain = skirt.addOrReplaceChild("chain", CubeListBuilder.create()
                .texOffs(62, 126).mirror(false).addBox(-0.3f, 0, -0.7f, 0.6f, 1.8f, 1.4f)
                .texOffs(132, 126).mirror(false).addBox(-0.7f, 1.5f, -0.3f, 1.4f, 1.8f, 0.6f)
                .texOffs(62, 126).mirror(false).addBox(-0.3f, 3, -0.7f, 0.6f, 1.8f, 1.4f)
                .texOffs(132, 126).mirror(false).addBox(-0.7f, 4.5f, -0.3f, 1.4f, 1.8f, 0.6f)
                .texOffs(62, 126).mirror(false).addBox(-0.3f, 6, -0.7f, 0.6f, 1.8f, 1.4f)
                .texOffs(132, 126).mirror(false).addBox(-0.7f, 7.5f, -0.3f, 1.4f, 1.8f, 0.6f)
                .texOffs(62, 126).mirror(false).addBox(-0.3f, 9, -0.7f, 0.6f, 1.8f, 1.4f)
                .texOffs(132, 126).mirror(false).addBox(-0.7f, 10.5f, -0.3f, 1.4f, 1.8f, 0.6f)
                .texOffs(179, 117).mirror(false).addBox(-1.2f, 12.1f, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(7.4f, -2.6f, -3.2f, 0, 0, -0.08f));
        PartDefinition rightBoot = root.addOrReplaceChild("right_boot", CubeListBuilder.create()
                .texOffs(121, 117).mirror(false).addBox(-1.9f, 0, -2.6f, 3.8f, 1.6f, 4.6f)
                .texOffs(139, 126).mirror(false).addBox(-1.3f, -0.9f, -3.4f, 2.6f, 1.4f, 1), PartPose.offsetAndRotation(-3.6f, 22.4f, -7.6f, 0, 0.12f, 0));
        PartDefinition leftBoot = root.addOrReplaceChild("left_boot", CubeListBuilder.create()
                .texOffs(121, 117).mirror(true).addBox(-1.9f, 0, -2.6f, 3.8f, 1.6f, 4.6f)
                .texOffs(139, 126).mirror(true).addBox(-1.3f, -0.9f, -3.4f, 2.6f, 1.4f, 1), PartPose.offsetAndRotation(3.6f, 22.4f, -7.6f, 0, -0.12f, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(128, 57).mirror(false).addBox(-4, -2, -3.2f, 5.6f, 10, 6.4f), PartPose.offsetAndRotation(-9, -16, 0, -0.08f, 0, 0.2f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(128, 57).mirror(true).addBox(-1.6f, -2, -3.2f, 5.6f, 10, 6.4f), PartPose.offsetAndRotation(9, -16, 0, -0.08f, 0, -0.2f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(164, 76).mirror(false).addBox(-4.6f, -1.8f, -5.4f, 6, 2, 11)
                .texOffs(100, 76).mirror(false).addBox(-5.4f, 0.2f, -6, 6.5f, 2, 12)
                .texOffs(155, 57).mirror(false).addBox(-6.2f, 2.2f, -6.6f, 7, 2.5f, 13.2f)
                .texOffs(29, 93).mirror(false).addBox(-3.4f, -3.4f, -5, 1.4f, 1.8f, 10)
                .texOffs(60, 106).mirror(false).addBox(-6.8f, -0.8f, -2.5f, 1, 4.5f, 5)
                .texOffs(192, 117).mirror(false).addBox(-7.3f, 0.6f, -1.8f, 0.5f, 1.5f, 3.5f), PartPose.offsetAndRotation(-1.4f, -2.6f, 0, 0, 0, 0.32f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(164, 76).mirror(true).addBox(-1.4f, -1.8f, -5.4f, 6, 2, 11)
                .texOffs(100, 76).mirror(true).addBox(-1.1f, 0.2f, -6, 6.5f, 2, 12)
                .texOffs(155, 57).mirror(true).addBox(-0.8f, 2.2f, -6.6f, 7, 2.5f, 13.2f)
                .texOffs(29, 93).mirror(true).addBox(2, -3.4f, -5, 1.4f, 1.8f, 10)
                .texOffs(60, 106).mirror(true).addBox(5.8f, -0.8f, -2.5f, 1, 4.5f, 5)
                .texOffs(192, 117).mirror(true).addBox(6.8f, 0.6f, -1.8f, 0.5f, 1.5f, 3.5f), PartPose.offsetAndRotation(1.4f, -2.6f, 0, 0, 0, -0.32f));
        PartDefinition rightPauldronFront = rightPauldron.addOrReplaceChild("right_pauldron_front", CubeListBuilder.create()
                .texOffs(203, 117).mirror(false).addBox(-3.2f, 0, -0.4f, 6, 5, 0.8f), PartPose.offsetAndRotation(-2.6f, 1, -6.4f, -0.35f, 0, 0));
        PartDefinition leftPauldronFront = leftPauldron.addOrReplaceChild("left_pauldron_front", CubeListBuilder.create()
                .texOffs(203, 117).mirror(true).addBox(-2.8f, 0, -0.4f, 6, 5, 0.8f), PartPose.offsetAndRotation(2.6f, 1, -6.4f, -0.35f, 0, 0));
        PartDefinition rightPauldronBack = rightPauldron.addOrReplaceChild("right_pauldron_back", CubeListBuilder.create()
                .texOffs(203, 117).mirror(false).addBox(-3.2f, 0, -0.4f, 6, 5, 0.8f), PartPose.offsetAndRotation(-2.6f, 1, 6.4f, 0.35f, 0, 0));
        PartDefinition leftPauldronBack = leftPauldron.addOrReplaceChild("left_pauldron_back", CubeListBuilder.create()
                .texOffs(203, 117).mirror(true).addBox(-2.8f, 0, -0.4f, 6, 5, 0.8f), PartPose.offsetAndRotation(2.6f, 1, 6.4f, 0.35f, 0, 0));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(139, 76).mirror(false).addBox(-2.6f, 0, -2.6f, 5.2f, 8, 5.2f)
                .texOffs(73, 106).mirror(false).addBox(-3.1f, 5.8f, -3.1f, 6.2f, 2.2f, 6.2f), PartPose.offsetAndRotation(-1.2f, 7.4f, 0, -1.05f, -0.5f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(139, 76).mirror(true).addBox(-2.6f, 0, -2.6f, 5.2f, 8, 5.2f)
                .texOffs(73, 106).mirror(true).addBox(-3.1f, 5.8f, -3.1f, 6.2f, 2.2f, 6.2f), PartPose.offsetAndRotation(1.2f, 7.4f, 0, -1.05f, 0.5f, 0));
        PartDefinition rightSleeve = rightForearm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(218, 0).mirror(false).addBox(-3.6f, -1, -10.5f, 7.2f, 13.5f, 10.5f), PartPose.offsetAndRotation(0, 0.4f, 2.4f, 1.1625f, 0.1717f, 0.1381f));
        PartDefinition leftSleeve = leftForearm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(218, 0).mirror(true).addBox(-3.6f, -1, -10.5f, 7.2f, 13.5f, 10.5f), PartPose.offsetAndRotation(0, 0.4f, 2.4f, 1.1625f, -0.1717f, -0.1381f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(233, 106).mirror(false).addBox(-1.7f, 0, -1.7f, 3.4f, 3.4f, 3.4f), PartPose.offsetAndRotation(0, 8.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(233, 106).mirror(true).addBox(-1.7f, 0, -1.7f, 3.4f, 3.4f, 3.4f), PartPose.offsetAndRotation(0, 8.2f, 0, 0, 0, 0));
        PartDefinition brush = rightHand.addOrReplaceChild("brush", CubeListBuilder.create()
                .texOffs(162, 32).mirror(false).addBox(-0.5f, -14, -0.5f, 1, 18, 1)
                .texOffs(69, 126).mirror(false).addBox(-0.8f, -15, -0.8f, 1.6f, 1.5f, 1.6f)
                .texOffs(69, 126).mirror(false).addBox(-0.8f, 3.5f, -0.8f, 1.6f, 1.5f, 1.6f)
                .texOffs(218, 117).mirror(false).addBox(-1, -19, -1, 2, 4, 2), PartPose.offsetAndRotation(0, 1.8f, -0.2f, 1.3237f, 0.5616f, 0.2893f));
        PartDefinition book = leftHand.addOrReplaceChild("book", CubeListBuilder.create()
                .texOffs(54, 93).mirror(false).addBox(-3.2f, -4.4f, -1.2f, 6.4f, 8.8f, 2.4f)
                .texOffs(192, 93).mirror(false).addBox(-3.5f, -4.6f, -0.3f, 0.6f, 9.2f, 0.6f), PartPose.offsetAndRotation(0.4f, 2.4f, -1.6f, 0.7649f, -0.338f, -0.1662f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(198, 57).mirror(false).addBox(-5, -10, -4.5f, 10, 7, 9)
                .texOffs(102, 106).mirror(false).addBox(-5.8f, -5.6f, -3.2f, 11.6f, 3, 6.6f)
                .texOffs(0, 117).mirror(false).addBox(-3.6f, -3.6f, -4.1f, 7.2f, 3, 5)
                .texOffs(18, 126).mirror(false).addBox(-1.4f, -6.4f, -5.5f, 2.8f, 2.8f, 1.2f)
                .texOffs(0, 76).mirror(false).addBox(-5.3f, -10.3f, -0.6f, 10.6f, 9.4f, 5.6f)
                .texOffs(148, 126).mirror(false).addBox(-2.9f, -3.4f, -4.9f, 0.9f, 1.7f, 0.8f)
                .texOffs(148, 126).mirror(false).addBox(2, -3.4f, -4.9f, 0.9f, 1.7f, 0.8f), PartPose.offsetAndRotation(0, -21.6f, -1, 0, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(153, 126).mirror(false).addBox(-1.6f, -0.8f, -0.5f, 3.2f, 1.8f, 0.6f), PartPose.offsetAndRotation(-2.5f, -6.7f, -4.5f, 0, 0.18f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(153, 126).mirror(true).addBox(-1.6f, -0.8f, -0.5f, 3.2f, 1.8f, 0.6f), PartPose.offsetAndRotation(2.5f, -6.7f, -4.5f, 0, -0.18f, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(78, 126).mirror(false).addBox(-5.2f, -1.1f, -0.5f, 5.2f, 1.3f, 1.6f), PartPose.offsetAndRotation(-0.3f, -7.5f, -4.8f, 0, 0.12f, 0.34f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(78, 126).mirror(true).addBox(0, -1.1f, -0.5f, 5.2f, 1.3f, 1.6f), PartPose.offsetAndRotation(0.3f, -7.5f, -4.8f, 0, -0.12f, -0.34f));
        PartDefinition rightBrowLock0 = rightBrow.addOrReplaceChild("right_brow_lock_0", CubeListBuilder.create()
                .texOffs(95, 126).mirror(false).addBox(-2.4f, -0.55f, -0.7f, 2.6f, 1.1f, 1.4f), PartPose.offsetAndRotation(-0.2f, -0.9f, -0.5f, -0.06f, 0, -0.04f));
        PartDefinition leftBrowLock0 = leftBrow.addOrReplaceChild("left_brow_lock_0", CubeListBuilder.create()
                .texOffs(95, 126).mirror(true).addBox(-0.2f, -0.55f, -0.7f, 2.6f, 1.1f, 1.4f), PartPose.offsetAndRotation(0.2f, -0.9f, -0.5f, -0.06f, 0, 0.04f));
        PartDefinition rightBrowLock1 = rightBrow.addOrReplaceChild("right_brow_lock_1", CubeListBuilder.create()
                .texOffs(164, 126).mirror(false).addBox(-2.5f, -0.5f, -0.65f, 2.7f, 1, 1.3f), PartPose.offsetAndRotation(-2, -1.2f, -0.4f, 0.08f, 0.1f, 0.2f));
        PartDefinition leftBrowLock1 = leftBrow.addOrReplaceChild("left_brow_lock_1", CubeListBuilder.create()
                .texOffs(164, 126).mirror(true).addBox(-0.2f, -0.5f, -0.65f, 2.7f, 1, 1.3f), PartPose.offsetAndRotation(2, -1.2f, -0.4f, 0.08f, -0.1f, -0.2f));
        PartDefinition rightBrowLock2 = rightBrow.addOrReplaceChild("right_brow_lock_2", CubeListBuilder.create()
                .texOffs(175, 126).mirror(false).addBox(-2.8f, -0.45f, -0.6f, 2.9f, 0.9f, 1.2f), PartPose.offsetAndRotation(-3.9f, -1.5f, -0.1f, 0.16f, 0.2f, 0.46f));
        PartDefinition leftBrowLock2 = leftBrow.addOrReplaceChild("left_brow_lock_2", CubeListBuilder.create()
                .texOffs(175, 126).mirror(true).addBox(-0.1f, -0.45f, -0.6f, 2.9f, 0.9f, 1.2f), PartPose.offsetAndRotation(3.9f, -1.5f, -0.1f, 0.16f, -0.2f, -0.46f));
        PartDefinition rightBrowflame = rightBrow.addOrReplaceChild("right_browflame", CubeListBuilder.create()
                .texOffs(27, 117).mirror(false).addBox(-0.25f, -3.6f, 0, 0.5f, 4, 3.8f), PartPose.offsetAndRotation(-5.6f, -1.6f, 0.3f, 0.2f, -0.5f, 0.5f));
        PartDefinition leftBrowflame = leftBrow.addOrReplaceChild("left_browflame", CubeListBuilder.create()
                .texOffs(27, 117).mirror(true).addBox(-0.25f, -3.6f, 0, 0.5f, 4, 3.8f), PartPose.offsetAndRotation(5.6f, -1.6f, 0.3f, 0.2f, 0.5f, -0.5f));
        PartDefinition rightSideburn = head.addOrReplaceChild("right_sideburn", CubeListBuilder.create()
                .texOffs(75, 93).mirror(false).addBox(-0.4f, 0, -1.6f, 0.8f, 7.5f, 3.2f), PartPose.offsetAndRotation(-5.6f, -6.4f, -1.2f, 0.05f, 0.25f, 0.55f));
        PartDefinition leftSideburn = head.addOrReplaceChild("left_sideburn", CubeListBuilder.create()
                .texOffs(75, 93).mirror(true).addBox(-0.4f, 0, -1.6f, 0.8f, 7.5f, 3.2f), PartPose.offsetAndRotation(5.6f, -6.4f, -1.2f, 0.05f, -0.25f, -0.55f));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(106, 126).mirror(false).addBox(-5.5f, -0.75f, -0.75f, 5.5f, 1.5f, 1.5f), PartPose.offsetAndRotation(-0.7f, -4, -5.6f, 0, 0.22f, -0.12f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(106, 126).mirror(true).addBox(0, -0.75f, -0.75f, 5.5f, 1.5f, 1.5f), PartPose.offsetAndRotation(0.7f, -4, -5.6f, 0, -0.22f, 0.12f));
        PartDefinition rightMoustacheTip = rightMoustache.addOrReplaceChild("right_moustache_tip", CubeListBuilder.create()
                .texOffs(195, 126).mirror(false).addBox(-4, -0.5f, -0.5f, 4, 1, 1), PartPose.offsetAndRotation(-5.2f, 0, 0, 0, 0.2f, 0.8f));
        PartDefinition leftMoustacheTip = leftMoustache.addOrReplaceChild("left_moustache_tip", CubeListBuilder.create()
                .texOffs(195, 126).mirror(true).addBox(0, -0.5f, -0.5f, 4, 1, 1), PartPose.offsetAndRotation(5.2f, 0, 0, 0, -0.2f, -0.8f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(86, 93).mirror(false).addBox(-4.7f, 0, -3.9f, 9.4f, 3.2f, 7.4f)
                .texOffs(206, 126).mirror(false).addBox(-2.6f, -0.9f, -3.8f, 0.8f, 1, 0.7f)
                .texOffs(206, 126).mirror(false).addBox(1.8f, -0.9f, -3.8f, 0.8f, 1, 0.7f), PartPose.offsetAndRotation(0, -3, -0.8f, 0, 0, 0));
        PartDefinition beard = jaw.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(197, 93).mirror(false).addBox(-1.5f, 0, -1, 3, 9, 2), PartPose.offsetAndRotation(0, 2.2f, -3.9f, 0.2f, 0, 0));
        PartDefinition beardTip = beard.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(152, 106).mirror(false).addBox(-1.1f, 0, -0.9f, 2.2f, 7, 1.8f), PartPose.offsetAndRotation(0, 8.6f, 0, -0.14f, 0, 0));
        PartDefinition rightBeardInner = jaw.addOrReplaceChild("right_beard_inner", CubeListBuilder.create()
                .texOffs(208, 93).mirror(false).addBox(-1.2f, 0, -0.9f, 2.4f, 8.5f, 1.8f), PartPose.offsetAndRotation(-1.9f, 2, -3.7f, 0.3f, 0.04f, 0.2f));
        PartDefinition leftBeardInner = jaw.addOrReplaceChild("left_beard_inner", CubeListBuilder.create()
                .texOffs(208, 93).mirror(true).addBox(-1.2f, 0, -0.9f, 2.4f, 8.5f, 1.8f), PartPose.offsetAndRotation(1.9f, 2, -3.7f, 0.3f, -0.04f, -0.2f));
        PartDefinition rightBeardInnerEnd = rightBeardInner.addOrReplaceChild("right_beard_inner_end", CubeListBuilder.create()
                .texOffs(140, 117).mirror(false).addBox(-0.75f, 0, -0.65f, 1.5f, 4.5f, 1.3f), PartPose.offsetAndRotation(0, 8.1f, 0, -0.04f, 0, 0.22f));
        PartDefinition leftBeardInnerEnd = leftBeardInner.addOrReplaceChild("left_beard_inner_end", CubeListBuilder.create()
                .texOffs(140, 117).mirror(true).addBox(-0.75f, 0, -0.65f, 1.5f, 4.5f, 1.3f), PartPose.offsetAndRotation(0, 8.1f, 0, -0.04f, 0, -0.22f));
        PartDefinition rightBeard = jaw.addOrReplaceChild("right_beard", CubeListBuilder.create()
                .texOffs(219, 93).mirror(false).addBox(-1.1f, 0, -0.9f, 2.2f, 9, 1.8f), PartPose.offsetAndRotation(-3.4f, 1.5f, -3.2f, 0.1f, 0.28f, 0.58f));
        PartDefinition leftBeard = jaw.addOrReplaceChild("left_beard", CubeListBuilder.create()
                .texOffs(219, 93).mirror(true).addBox(-1.1f, 0, -0.9f, 2.2f, 9, 1.8f), PartPose.offsetAndRotation(3.4f, 1.5f, -3.2f, 0.1f, -0.28f, -0.58f));
        PartDefinition rightBeardEnd = rightBeard.addOrReplaceChild("right_beard_end", CubeListBuilder.create()
                .texOffs(149, 117).mirror(false).addBox(-0.75f, 0, -0.65f, 1.5f, 5, 1.3f), PartPose.offsetAndRotation(0, 8.6f, 0, 0.06f, 0, 0.38f));
        PartDefinition leftBeardEnd = leftBeard.addOrReplaceChild("left_beard_end", CubeListBuilder.create()
                .texOffs(149, 117).mirror(true).addBox(-0.75f, 0, -0.65f, 1.5f, 5, 1.3f), PartPose.offsetAndRotation(0, 8.6f, 0, 0.06f, 0, -0.38f));
        PartDefinition rightBeardOuter = jaw.addOrReplaceChild("right_beard_outer", CubeListBuilder.create()
                .texOffs(163, 106).mirror(false).addBox(-1, 0, -0.9f, 2, 7, 1.8f), PartPose.offsetAndRotation(-4.4f, 0.6f, -2.4f, 0.02f, 0.4f, 0.95f));
        PartDefinition leftBeardOuter = jaw.addOrReplaceChild("left_beard_outer", CubeListBuilder.create()
                .texOffs(163, 106).mirror(true).addBox(-1, 0, -0.9f, 2, 7, 1.8f), PartPose.offsetAndRotation(4.4f, 0.6f, -2.4f, 0.02f, -0.4f, -0.95f));
        PartDefinition rightBeardTuft = jaw.addOrReplaceChild("right_beard_tuft", CubeListBuilder.create()
                .texOffs(227, 117).mirror(false).addBox(-0.9f, 0, -0.6f, 1.8f, 4, 1.2f), PartPose.offsetAndRotation(-0.95f, 2.6f, -4.3f, 0.46f, 0, -0.05f));
        PartDefinition leftBeardTuft = jaw.addOrReplaceChild("left_beard_tuft", CubeListBuilder.create()
                .texOffs(227, 117).mirror(true).addBox(-0.9f, 0, -0.6f, 1.8f, 4, 1.2f), PartPose.offsetAndRotation(0.95f, 2.6f, -4.3f, 0.46f, 0, 0.05f));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(199, 76).mirror(false).addBox(-4.2f, -4.6f, -3.8f, 8.4f, 4.8f, 7.6f)
                .texOffs(123, 93).mirror(false).addBox(-5.4f, -1.2f, -4.9f, 10.8f, 1.6f, 9.8f)
                .texOffs(29, 126).mirror(false).addBox(-2.4f, -4.2f, -4.4f, 4.8f, 3.4f, 0.6f)
                .texOffs(42, 126).mirror(false).addBox(-0.6f, -6.6f, -0.6f, 1.2f, 2.2f, 1.2f)
                .texOffs(211, 126).mirror(false).addBox(-7, -5.2f, -0.4f, 14, 0.8f, 0.8f), PartPose.offsetAndRotation(0, -10, 0, 0, 0, 0));
        PartDefinition board = crown.addOrReplaceChild("board", CubeListBuilder.create()
                .texOffs(167, 32).mirror(false).addBox(-6.8f, -1.8f, -9.2f, 13.6f, 1.8f, 16.4f)
                .texOffs(63, 57).mirror(false).addBox(-7.1f, -0.5f, -9.5f, 14.2f, 0.5f, 17)
                .texOffs(236, 117).mirror(false).addBox(-6.25f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(-4.25f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(-2.25f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(-0.25f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(1.75f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(3.75f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(5.75f, 0, -8.9f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(-6.25f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(-4.25f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(-2.25f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(-0.25f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(1.75f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(3.75f, 0, 6.6f, 0.5f, 4.5f, 0.5f)
                .texOffs(236, 117).mirror(false).addBox(5.75f, 0, 6.6f, 0.5f, 4.5f, 0.5f), PartPose.offsetAndRotation(0, -4.6f, 0, 0.07f, 0, 0));
        PartDefinition rightEartie = crown.addOrReplaceChild("right_eartie", CubeListBuilder.create()
                .texOffs(158, 117).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 6, 0.5f)
                .texOffs(123, 126).mirror(false).addBox(-0.8f, 6, -0.8f, 1.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(-7, -4.8f, 0, 0, 0, -0.08f));
        PartDefinition leftEartie = crown.addOrReplaceChild("left_eartie", CubeListBuilder.create()
                .texOffs(158, 117).mirror(true).addBox(-0.25f, 0, -0.25f, 0.5f, 6, 0.5f)
                .texOffs(123, 126).mirror(true).addBox(-0.8f, 6, -0.8f, 1.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(7, -4.8f, 0, 0, 0, 0.08f));
        PartDefinition halo = body.addOrReplaceChild("halo", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-15, -15, 0, 30, 30, 1), PartPose.offsetAndRotation(0, -28.5f, 7.6f, 0, 0, 0));
        PartDefinition haloSkull0 = halo.addOrReplaceChild("halo_skull_0", CubeListBuilder.create()
                .texOffs(51, 126).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(186, 126).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(-9.6995f, 5.6f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull1 = halo.addOrReplaceChild("halo_skull_1", CubeListBuilder.create()
                .texOffs(51, 126).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(186, 126).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(-11.2f, -0, -0.4f, 0, 0, 0));
        PartDefinition haloSkull2 = halo.addOrReplaceChild("halo_skull_2", CubeListBuilder.create()
                .texOffs(51, 126).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(186, 126).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(-9.6995f, -5.6f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull3 = halo.addOrReplaceChild("halo_skull_3", CubeListBuilder.create()
                .texOffs(51, 126).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(186, 126).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(-5.6f, -9.6995f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull4 = halo.addOrReplaceChild("halo_skull_4", CubeListBuilder.create()
                .texOffs(51, 126).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(186, 126).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(0, -11.2f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull5 = halo.addOrReplaceChild("halo_skull_5", CubeListBuilder.create()
                .texOffs(51, 126).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(186, 126).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(5.6f, -9.6995f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull6 = halo.addOrReplaceChild("halo_skull_6", CubeListBuilder.create()
                .texOffs(51, 126).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(186, 126).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(9.6995f, -5.6f, -0.4f, 0, 0, 0));
        PartDefinition haloSkull7 = halo.addOrReplaceChild("halo_skull_7", CubeListBuilder.create()
                .texOffs(51, 126).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(186, 126).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(11.2f, -0, -0.4f, 0, 0, 0));
        PartDefinition haloSkull8 = halo.addOrReplaceChild("halo_skull_8", CubeListBuilder.create()
                .texOffs(51, 126).mirror(false).addBox(-1.3f, -1.5f, -1.8f, 2.6f, 3, 2)
                .texOffs(186, 126).mirror(false).addBox(-0.9f, 1.3f, -1.7f, 1.8f, 0.8f, 1.4f), PartPose.offsetAndRotation(9.6995f, 5.6f, -0.4f, 0, 0, 0));
        PartDefinition haloFlame0 = halo.addOrReplaceChild("halo_flame_0", CubeListBuilder.create()
                .texOffs(172, 106).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f)
                .texOffs(166, 93).mirror(false).addBox(-0.25f, -7.5f, -2, 0.5f, 7.5f, 4), PartPose.offsetAndRotation(-9.8995f, 9.8995f, 0.6f, -0.3f, 0, -2.3562f));
        PartDefinition haloFlame1 = halo.addOrReplaceChild("halo_flame_1", CubeListBuilder.create()
                .texOffs(163, 117).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f)
                .texOffs(141, 106).mirror(false).addBox(-0.25f, -5.5f, -2, 0.5f, 5.5f, 4), PartPose.offsetAndRotation(-12.4741f, 6.3559f, 0.6f, -0.12f, 0, -2.042f));
        PartDefinition haloFlame2 = halo.addOrReplaceChild("halo_flame_2", CubeListBuilder.create()
                .texOffs(172, 106).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f)
                .texOffs(166, 93).mirror(false).addBox(-0.25f, -7.5f, -2, 0.5f, 7.5f, 4), PartPose.offsetAndRotation(-13.8276f, 2.1901f, 0.6f, -0.3f, 0, -1.7279f));
        PartDefinition haloFlame3 = halo.addOrReplaceChild("halo_flame_3", CubeListBuilder.create()
                .texOffs(163, 117).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f)
                .texOffs(141, 106).mirror(false).addBox(-0.25f, -5.5f, -2, 0.5f, 5.5f, 4), PartPose.offsetAndRotation(-13.8276f, -2.1901f, 0.6f, -0.12f, 0, -1.4137f));
        PartDefinition haloFlame4 = halo.addOrReplaceChild("halo_flame_4", CubeListBuilder.create()
                .texOffs(172, 106).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f)
                .texOffs(166, 93).mirror(false).addBox(-0.25f, -7.5f, -2, 0.5f, 7.5f, 4), PartPose.offsetAndRotation(-12.4741f, -6.3559f, 0.6f, -0.3f, 0, -1.0996f));
        PartDefinition haloFlame5 = halo.addOrReplaceChild("halo_flame_5", CubeListBuilder.create()
                .texOffs(163, 117).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f)
                .texOffs(141, 106).mirror(false).addBox(-0.25f, -5.5f, -2, 0.5f, 5.5f, 4), PartPose.offsetAndRotation(-9.8995f, -9.8995f, 0.6f, -0.12f, 0, -0.7854f));
        PartDefinition haloFlame6 = halo.addOrReplaceChild("halo_flame_6", CubeListBuilder.create()
                .texOffs(172, 106).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f)
                .texOffs(166, 93).mirror(false).addBox(-0.25f, -7.5f, -2, 0.5f, 7.5f, 4), PartPose.offsetAndRotation(-6.3559f, -12.4741f, 0.6f, -0.3f, 0, -0.4712f));
        PartDefinition haloFlame7 = halo.addOrReplaceChild("halo_flame_7", CubeListBuilder.create()
                .texOffs(163, 117).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f)
                .texOffs(141, 106).mirror(false).addBox(-0.25f, -5.5f, -2, 0.5f, 5.5f, 4), PartPose.offsetAndRotation(-2.1901f, -13.8276f, 0.6f, -0.12f, 0, -0.1571f));
        PartDefinition haloFlame8 = halo.addOrReplaceChild("halo_flame_8", CubeListBuilder.create()
                .texOffs(172, 106).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f)
                .texOffs(166, 93).mirror(false).addBox(-0.25f, -7.5f, -2, 0.5f, 7.5f, 4), PartPose.offsetAndRotation(2.1901f, -13.8276f, 0.6f, -0.3f, 0, 0.1571f));
        PartDefinition haloFlame9 = halo.addOrReplaceChild("halo_flame_9", CubeListBuilder.create()
                .texOffs(163, 117).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f)
                .texOffs(141, 106).mirror(false).addBox(-0.25f, -5.5f, -2, 0.5f, 5.5f, 4), PartPose.offsetAndRotation(6.3559f, -12.4741f, 0.6f, -0.12f, 0, 0.4712f));
        PartDefinition haloFlame10 = halo.addOrReplaceChild("halo_flame_10", CubeListBuilder.create()
                .texOffs(172, 106).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f)
                .texOffs(166, 93).mirror(false).addBox(-0.25f, -7.5f, -2, 0.5f, 7.5f, 4), PartPose.offsetAndRotation(9.8995f, -9.8995f, 0.6f, -0.3f, 0, 0.7854f));
        PartDefinition haloFlame11 = halo.addOrReplaceChild("halo_flame_11", CubeListBuilder.create()
                .texOffs(163, 117).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f)
                .texOffs(141, 106).mirror(false).addBox(-0.25f, -5.5f, -2, 0.5f, 5.5f, 4), PartPose.offsetAndRotation(12.4741f, -6.3559f, 0.6f, -0.12f, 0, 1.0996f));
        PartDefinition haloFlame12 = halo.addOrReplaceChild("halo_flame_12", CubeListBuilder.create()
                .texOffs(172, 106).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f)
                .texOffs(166, 93).mirror(false).addBox(-0.25f, -7.5f, -2, 0.5f, 7.5f, 4), PartPose.offsetAndRotation(13.8276f, -2.1901f, 0.6f, -0.3f, 0, 1.4137f));
        PartDefinition haloFlame13 = halo.addOrReplaceChild("halo_flame_13", CubeListBuilder.create()
                .texOffs(163, 117).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f)
                .texOffs(141, 106).mirror(false).addBox(-0.25f, -5.5f, -2, 0.5f, 5.5f, 4), PartPose.offsetAndRotation(13.8276f, 2.1901f, 0.6f, -0.12f, 0, 1.7279f));
        PartDefinition haloFlame14 = halo.addOrReplaceChild("halo_flame_14", CubeListBuilder.create()
                .texOffs(172, 106).mirror(false).addBox(-2, -7.5f, -0.25f, 4, 7.5f, 0.5f)
                .texOffs(166, 93).mirror(false).addBox(-0.25f, -7.5f, -2, 0.5f, 7.5f, 4), PartPose.offsetAndRotation(12.4741f, 6.3559f, 0.6f, -0.3f, 0, 2.042f));
        PartDefinition haloFlame15 = halo.addOrReplaceChild("halo_flame_15", CubeListBuilder.create()
                .texOffs(163, 117).mirror(false).addBox(-2, -5.5f, -0.25f, 4, 5.5f, 0.5f)
                .texOffs(141, 106).mirror(false).addBox(-0.25f, -5.5f, -2, 0.5f, 5.5f, 4), PartPose.offsetAndRotation(9.8995f, 9.8995f, 0.6f, -0.12f, 0, 2.3562f));
        PartDefinition haloRim0 = halo.addOrReplaceChild("halo_rim_0", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(0, -11.6f, 0.5f, 0, 0, 0));
        PartDefinition haloRim1 = halo.addOrReplaceChild("halo_rim_1", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(3.0023f, -11.2047f, 0.5f, 0, 0, 0.2618f));
        PartDefinition haloRim2 = halo.addOrReplaceChild("halo_rim_2", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(5.8f, -10.0459f, 0.5f, 0, 0, 0.5236f));
        PartDefinition haloRim3 = halo.addOrReplaceChild("halo_rim_3", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(8.2024f, -8.2024f, 0.5f, 0, 0, 0.7854f));
        PartDefinition haloRim4 = halo.addOrReplaceChild("halo_rim_4", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(10.0459f, -5.8f, 0.5f, 0, 0, 1.0472f));
        PartDefinition haloRim5 = halo.addOrReplaceChild("halo_rim_5", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(11.2047f, -3.0023f, 0.5f, 0, 0, 1.309f));
        PartDefinition haloRim6 = halo.addOrReplaceChild("halo_rim_6", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(11.6f, -0, 0.5f, 0, 0, 1.5708f));
        PartDefinition haloRim7 = halo.addOrReplaceChild("halo_rim_7", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(11.2047f, 3.0023f, 0.5f, 0, 0, 1.8326f));
        PartDefinition haloRim8 = halo.addOrReplaceChild("halo_rim_8", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(10.0459f, 5.8f, 0.5f, 0, 0, 2.0944f));
        PartDefinition haloRim9 = halo.addOrReplaceChild("halo_rim_9", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(8.2024f, 8.2024f, 0.5f, 0, 0, 2.3562f));
        PartDefinition haloRim10 = halo.addOrReplaceChild("halo_rim_10", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(5.8f, 10.0459f, 0.5f, 0, 0, 2.618f));
        PartDefinition haloRim11 = halo.addOrReplaceChild("halo_rim_11", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(3.0023f, 11.2047f, 0.5f, 0, 0, 2.8798f));
        PartDefinition haloRim12 = halo.addOrReplaceChild("halo_rim_12", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(0, 11.6f, 0.5f, 0, 0, 3.1416f));
        PartDefinition haloRim13 = halo.addOrReplaceChild("halo_rim_13", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-3.0023f, 11.2047f, 0.5f, 0, 0, 3.4034f));
        PartDefinition haloRim14 = halo.addOrReplaceChild("halo_rim_14", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-5.8f, 10.0459f, 0.5f, 0, 0, 3.6652f));
        PartDefinition haloRim15 = halo.addOrReplaceChild("halo_rim_15", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-8.2024f, 8.2024f, 0.5f, 0, 0, 3.927f));
        PartDefinition haloRim16 = halo.addOrReplaceChild("halo_rim_16", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-10.0459f, 5.8f, 0.5f, 0, 0, 4.1888f));
        PartDefinition haloRim17 = halo.addOrReplaceChild("halo_rim_17", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-11.2047f, 3.0023f, 0.5f, 0, 0, 4.4506f));
        PartDefinition haloRim18 = halo.addOrReplaceChild("halo_rim_18", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-11.6f, 0, 0.5f, 0, 0, 4.7124f));
        PartDefinition haloRim19 = halo.addOrReplaceChild("halo_rim_19", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-11.2047f, -3.0023f, 0.5f, 0, 0, 4.9742f));
        PartDefinition haloRim20 = halo.addOrReplaceChild("halo_rim_20", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-10.0459f, -5.8f, 0.5f, 0, 0, 5.236f));
        PartDefinition haloRim21 = halo.addOrReplaceChild("halo_rim_21", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-8.2024f, -8.2024f, 0.5f, 0, 0, 5.4978f));
        PartDefinition haloRim22 = halo.addOrReplaceChild("halo_rim_22", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-5.8f, -10.0459f, 0.5f, 0, 0, 5.7596f));
        PartDefinition haloRim23 = halo.addOrReplaceChild("halo_rim_23", CubeListBuilder.create()
                .texOffs(38, 117).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(-3.0023f, -11.2047f, 0.5f, 0, 0, 6.0214f));
        PartDefinition hemFlame0 = skirt.addOrReplaceChild("hem_flame_0", CubeListBuilder.create()
                .texOffs(177, 93).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(3.0336f, 23, -9.0841f, 0, -0.26f, 0));
        PartDefinition hemFlame1 = skirt.addOrReplaceChild("hem_flame_1", CubeListBuilder.create()
                .texOffs(183, 106).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(8.3288f, 23, -6.6588f, 0, -0.7836f, 0));
        PartDefinition hemFlame2 = skirt.addOrReplaceChild("hem_flame_2", CubeListBuilder.create()
                .texOffs(177, 93).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(11.3924f, 23, -2.4492f, 0, -1.3072f, 0));
        PartDefinition hemFlame3 = skirt.addOrReplaceChild("hem_flame_3", CubeListBuilder.create()
                .texOffs(183, 106).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(11.4034f, 23, 2.4166f, 0, -1.8308f, 0));
        PartDefinition hemFlame4 = skirt.addOrReplaceChild("hem_flame_4", CubeListBuilder.create()
                .texOffs(177, 93).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(8.3589f, 23, 6.6348f, 0, -2.3544f, 0));
        PartDefinition hemFlame5 = skirt.addOrReplaceChild("hem_flame_5", CubeListBuilder.create()
                .texOffs(183, 106).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(3.0746f, 23, 9.0753f, 0, -2.878f, 0));
        PartDefinition hemFlame6 = skirt.addOrReplaceChild("hem_flame_6", CubeListBuilder.create()
                .texOffs(177, 93).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(-3.0336f, 23, 9.0841f, 0, -3.4016f, 0));
        PartDefinition hemFlame7 = skirt.addOrReplaceChild("hem_flame_7", CubeListBuilder.create()
                .texOffs(183, 106).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(-8.3288f, 23, 6.6588f, 0, -3.9252f, 0));
        PartDefinition hemFlame8 = skirt.addOrReplaceChild("hem_flame_8", CubeListBuilder.create()
                .texOffs(177, 93).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(-11.3924f, 23, 2.4492f, 0, -4.4488f, 0));
        PartDefinition hemFlame9 = skirt.addOrReplaceChild("hem_flame_9", CubeListBuilder.create()
                .texOffs(183, 106).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(-11.4034f, 23, -2.4166f, 0, -4.9724f, 0));
        PartDefinition hemFlame10 = skirt.addOrReplaceChild("hem_flame_10", CubeListBuilder.create()
                .texOffs(177, 93).mirror(false).addBox(-3, -11, -0.25f, 6, 11, 0.5f), PartPose.offsetAndRotation(-8.3589f, 23, -6.6348f, 0, -5.496f, 0));
        PartDefinition hemFlame11 = skirt.addOrReplaceChild("hem_flame_11", CubeListBuilder.create()
                .texOffs(183, 106).mirror(false).addBox(-3, -8, -0.25f, 6, 8, 0.5f), PartPose.offsetAndRotation(-3.0746f, 23, -9.0753f, 0, -6.0196f, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED yama: end
    }

    /**
     * His pose: the brush up in his right fist, the Book held before him in his left; judging, both
     * raised; striking, the brush brought down. In his wrath his jaws gape, his brows knot lower, the
     * hellfire behind his head flares and fire runs round his hem.
     */
    public static final class YamaModel extends EntityModel<KingYamaEntity> {
        private final ModelPart root, head, jaw, beard, beardTip, rightBeard, leftBeard, rightBrow, leftBrow, rightArm, leftArm,
                rightForearm, rightSleeve, leftSleeve, halo, chain, apron, rightBoot, leftBoot;
        private final List<ModelPart> flames = new ArrayList<>(), hemFlames = new ArrayList<>();

        public YamaModel(ModelPart root) {
            this.root = root;
            ModelPart body = root.getChild("body"), skirt = body.getChild("skirt");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            beard = jaw.getChild("beard");
            beardTip = beard.getChild("beard_tip");
            rightBeard = jaw.getChild("right_beard");
            leftBeard = jaw.getChild("left_beard");
            rightBrow = head.getChild("right_brow");
            leftBrow = head.getChild("left_brow");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            rightSleeve = rightForearm.getChild("right_sleeve");
            leftSleeve = leftArm.getChild("left_forearm").getChild("left_sleeve");
            halo = body.getChild("halo");
            chain = skirt.getChild("chain");
            apron = skirt.getChild("apron");
            rightBoot = root.getChild("right_boot");
            leftBoot = root.getChild("left_boot");
            for (int k = 0; halo.hasChild("halo_flame_" + k); k++) flames.add(halo.getChild("halo_flame_" + k));
            for (int k = 0; skirt.hasChild("hem_flame_" + k); k++) hemFlames.add(skirt.getChild("hem_flame_" + k));
        }

        /** A part turned this much from its rest. */
        private static void turn(ModelPart p, float x, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.zRot = p.getInitialPose().zRot + z;
        }

        @Override
        public void setupAnim(KingYamaEntity e, float swing, float amount, float age, float yaw, float pitch) {
            boolean wrath = e.inWrath();
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            head.y = head.getInitialPose().y + Mth.sin(age * 0.06f) * 0.2f;
            // the brush up in his right fist, the Book in his left; judging, both raised; a blow, the brush brought down
            float rx = Mth.cos(age * 0.05f) * 0.03f, lx = -rx;
            if (e.isJudging()) {
                rx = -1.25f;
                lx = -0.45f;
            }
            rx -= strike * 1.5f;
            turn(rightArm, rx, 0);
            turn(leftArm, lx, 0);
            turn(rightForearm, strike * 0.6f, 0);
            // the great sleeves hang under their weight whatever the arms do
            turn(rightSleeve, -(rx + strike * 0.6f) * 0.85f + Mth.sin(age * 0.05f) * 0.04f + amount * 0.2f, 0);
            turn(leftSleeve, -lx * 0.85f + Mth.sin(age * 0.05f + 1) * 0.04f + amount * 0.2f, 0);
            // a growl; his jaws gape in his wrath, and as he strikes
            float gape = wrath ? 0.42f + Mth.sin(age * 0.35f) * 0.05f : Math.max(0, Mth.sin(age * 0.04f)) * 0.06f;
            turn(jaw, Math.max(gape, strike * 0.35f), 0);
            turn(beard, Mth.sin(age * 0.05f) * 0.03f + amount * 0.15f, 0);
            turn(beardTip, Mth.sin(age * 0.05f + 0.7f) * 0.04f, 0);
            turn(rightBeard, 0, Mth.sin(age * 0.045f) * 0.04f);
            turn(leftBeard, 0, -Mth.sin(age * 0.045f + 0.5f) * 0.04f);
            // his brows knot lower in his wrath
            float knit = wrath ? 0.1f : 0;
            turn(rightBrow, 0, knit);
            turn(leftBrow, 0, -knit);
            rightBrow.y = rightBrow.getInitialPose().y + (wrath ? 0.3f : 0);
            leftBrow.y = rightBrow.y;
            turn(chain, amount * 0.25f + Mth.sin(age * 0.07f) * 0.03f, Mth.sin(age * 0.05f) * 0.05f);
            turn(apron, -amount * 0.2f + Mth.sin(age * 0.06f) * 0.02f, 0);
            // gliding over the floor, his boots shuffle under the hem
            float step = Mth.cos(swing * 0.6f) * 1.2f * amount;
            rightBoot.z = rightBoot.getInitialPose().z + step;
            leftBoot.z = leftBoot.getInitialPose().z - step;
            // the hellfire behind his head flickering, flaring in his wrath; and in his wrath fire round his hem
            float flare = wrath ? 1.55f : 1f;
            halo.xScale = halo.yScale = wrath ? 1.08f + Mth.sin(age * 0.2f) * 0.02f : 1f;
            for (int k = 0; k < flames.size(); k++) {
                ModelPart f = flames.get(k);
                f.yScale = flare * (1 + Mth.sin(age * 0.3f + k * 1.7f) * 0.14f);
                f.xScale = wrath ? 1.2f : 1f;
            }
            for (int k = 0; k < hemFlames.size(); k++) {
                ModelPart f = hemFlames.get(k);
                f.visible = wrath;
                f.yScale = 1 + Mth.sin(age * 0.33f + k * 2.1f) * 0.25f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class YamaRenderer extends MobRenderer<KingYamaEntity, YamaModel> {
        /** How much larger than his pixels he is drawn: some 3.3 blocks to his crown, a head over the other kings (king_yama.py prints his reach). */
        public static final float SIZE = 0.9f;
        private static final ResourceLocation SKIN = tex("king_yama");
        /** His eyes and his hellfire, drawn full-bright: smouldering, and blazing in his wrath. */
        private static final RenderType GLOW = RenderType.eyes(tex("king_yama_glow")), WRATH_GLOW = RenderType.eyes(tex("king_yama_wrath_glow"));

        public YamaRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new YamaModel(ctx.bakeLayer(YAMA_LAYER)), 1.0f);
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, KingYamaEntity e,
                                   float swing, float amount, float partial, float age, float yaw, float pitch) {
                    VertexConsumer vc = buffers.getBuffer(e.inWrath() ? WRATH_GLOW : GLOW);
                    getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        @Override
        protected void scale(KingYamaEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(KingYamaEntity e) {
            return SKIN;
        }
    }

    /** For the gallery: whether a zone creature is drawn as a captain. */
    public static boolean captain(Mob m) {
        return m instanceof ZoneMonster z && z.isCaptain();
    }
}
