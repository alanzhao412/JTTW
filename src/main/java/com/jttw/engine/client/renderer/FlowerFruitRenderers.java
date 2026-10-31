package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.flowerfruit.FakePilgrimEntity;
import com.jttw.engine.entity.flowerfruit.FlowerFruitMonster;
import com.jttw.engine.entity.flowerfruit.GreatBaboonEntity;
import com.jttw.engine.entity.flowerfruit.MacaqueCloneEntity;
import com.jttw.engine.entity.flowerfruit.SixEaredMacaqueEntity;
import com.jttw.engine.entity.flowerfruit.SpearApeEntity;
import com.jttw.engine.entity.flowerfruit.StoneGibbonEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.FlowerFruitRegistry;
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
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How Flower-Fruit Mountain's creatures look: the monkey army (the Stone-Throwing Gibbon, lean and
 * long-armed in a pale ring of fur; the Spear Ape, a red-faced macaque in a leather cuirass and red
 * headband with a long tasselled spear; the Great Baboon, a hulking silver-maned baboon with a dog's
 * fanged muzzle and iron arm-bands); the Six-Eared Macaque's fake pilgrims (a false Tang Sanzang in
 * his kasaya and Vairocana crown with a ringed staff, a false Zhu Bajie, pig-headed and pot-bellied,
 * with his nine-tooth rake, a false Sha Wujing, blue-grey, red-bearded, his necklace of skulls and
 * his crescent-moon spade, each with a monkey's tail showing under his robe); and the Six-Eared
 * Macaque himself, the Monkey King's mirror (his phoenix-plumed cap, golden chain mail, tiger-skin
 * kilt, red cloak and gold-banded iron staff, three ears a side), his hair clones in his likeness.
 * Models and skins from {@code assets_source/flower_fruit.py}, each between its GENERATED markers;
 * the eyes glow (each its {@code _glow} skin).
 *
 * <p>One frame drives them all ({@link MonkeyModel}): legs that stride and bend, a chest that
 * breathes, a head that turns, arms that swing, a tail that sways, cloth that follows; and each
 * one's warnings and blows posed so they read: the gibbon's stone up over its head, the ape's spear
 * drawn back and thrust, the baboon up beating its chest and pounding, the monkeys kneeling to the
 * true Monkey King; the false Tang Sanzang's palms together over his staff, the false Bajie's rake
 * heaved over his shoulder, the false Sha Wujing's spade levelled; the Macaque's staff whirled
 * overhead, his crouch and lean on the cloud, his hand to his mouth plucking hairs, his staff raised
 * high for the heaven-splitting blow.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FlowerFruitRenderers {

    private FlowerFruitRenderers() {}

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name), "main");
    }

    public static final ModelLayerLocation GIBBON = layer("stone_gibbon"), APE = layer("spear_ape"), BABOON = layer("great_baboon"),
            TANG = layer("false_tang_sanzang"), BAJIE = layer("false_zhu_bajie"), SHA = layer("false_sha_wujing"),
            MACAQUE = layer("six_eared_macaque");

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GIBBON, FlowerFruitRenderers::gibbonLayer);
        event.registerLayerDefinition(APE, FlowerFruitRenderers::apeLayer);
        event.registerLayerDefinition(BABOON, FlowerFruitRenderers::baboonLayer);
        event.registerLayerDefinition(TANG, FlowerFruitRenderers::tangLayer);
        event.registerLayerDefinition(BAJIE, FlowerFruitRenderers::bajieLayer);
        event.registerLayerDefinition(SHA, FlowerFruitRenderers::shaLayer);
        event.registerLayerDefinition(MACAQUE, FlowerFruitRenderers::macaqueLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FlowerFruitRegistry.STONE_GIBBON.get(), ctx -> new Single<>(ctx, GIBBON, "stone_gibbon", Kind.GIBBON, SIZE_GIBBON));
        event.registerEntityRenderer(FlowerFruitRegistry.SPEAR_APE.get(), ctx -> new Single<>(ctx, APE, "spear_ape", Kind.APE, SIZE_APE));
        event.registerEntityRenderer(FlowerFruitRegistry.GREAT_BABOON.get(), ctx -> new Single<>(ctx, BABOON, "great_baboon", Kind.BABOON, SIZE_BABOON));
        event.registerEntityRenderer(FlowerFruitRegistry.FAKE_PILGRIM.get(), PilgrimRenderer::new);
        event.registerEntityRenderer(FlowerFruitRegistry.SIX_EARED_MACAQUE.get(), ctx -> new Single<>(ctx, MACAQUE, "six_eared_macaque", Kind.MACAQUE, SIZE_MACAQUE));
        event.registerEntityRenderer(FlowerFruitRegistry.MACAQUE_CLONE.get(), ctx -> new Single<>(ctx, MACAQUE, "six_eared_macaque", Kind.MACAQUE, SIZE_MACAQUE));
        event.registerEntityRenderer(FlowerFruitRegistry.THROWN_STONE.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.2f, false));
    }

    /** How much larger than their pixels each is drawn (flower_fruit.py prints their reach). */
    public static final float SIZE_GIBBON = 0.92f, SIZE_APE = 0.95f, SIZE_BABOON = 1.1f, SIZE_PILGRIM = 0.95f, SIZE_MACAQUE = 1.0f;

    // ── The models ──────────────────────────────────────────────────────

    public static LayerDefinition gibbonLayer() {
        // GENERATED gibbon: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(104, 0).mirror(false).addBox(-1.4f, -0.5f, -1.4f, 2.8f, 5.3f, 2.8f), PartPose.offsetAndRotation(-1.7f, 14.1062f, 0.2f, -0.5f, 0, 0.06f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(104, 0).mirror(true).addBox(-1.4f, -0.5f, -1.4f, 2.8f, 5.3f, 2.8f), PartPose.offsetAndRotation(1.7f, 14.1062f, 0.2f, -0.5f, 0, -0.06f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(142, 0).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 4.6f, 2.4f), PartPose.offsetAndRotation(0, 4.8f, 0, 0.85f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(142, 0).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 4.6f, 2.4f), PartPose.offsetAndRotation(0, 4.8f, 0, 0.85f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(202, 0).mirror(false).addBox(-1.3f, 0, -3.1f, 2.6f, 1.3f, 4.6f), PartPose.offsetAndRotation(0, 4.6f, 0, -0.35f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(202, 0).mirror(true).addBox(-1.3f, 0, -3.1f, 2.6f, 1.3f, 4.6f), PartPose.offsetAndRotation(0, 4.6f, 0, -0.35f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(155, 0).mirror(false).addBox(-2.5f, -3, -1.9f, 5, 3.6f, 3.8f), PartPose.offsetAndRotation(0, 14.1062f, 0.3f, 0.22f, 0, 0));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(42, 0).mirror(false).addBox(-3.1f, -7, -2.1f, 6.2f, 7, 4.2f), PartPose.offsetAndRotation(0, -2.6f, 0, 0.1f, 0, 0));
        PartDefinition rightArm = chest.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(67, 0).mirror(false).addBox(-1.1f, -0.99f, -1.1f, 2.2f, 8.79f, 2.2f), PartPose.offsetAndRotation(-3.87f, -5.79f, 0, -0.18f, 0, 0.1f));
        PartDefinition leftArm = chest.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(67, 0).mirror(true).addBox(-1.1f, -0.99f, -1.1f, 2.2f, 8.79f, 2.2f), PartPose.offsetAndRotation(3.87f, -5.79f, 0, -0.18f, 0, -0.1f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(80, 0).mirror(false).addBox(-1, 0, -1, 2, 7.4f, 2), PartPose.offsetAndRotation(0, 7.8f, 0, -0.25f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(80, 0).mirror(true).addBox(-1, 0, -1, 2, 7.4f, 2), PartPose.offsetAndRotation(0, 7.8f, 0, -0.25f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 3.2f, 2.4f), PartPose.offsetAndRotation(0, 7.4f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(219, 0).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 3.2f, 2.4f), PartPose.offsetAndRotation(0, 7.4f, 0, 0, 0, 0));
        PartDefinition rightFingers = rightHand.addOrReplaceChild("right_fingers", CubeListBuilder.create()
                .texOffs(141, 16).mirror(false).addBox(-1.15f, 0, -1.35f, 0.5714f, 1.734f, 1.32f)
                .texOffs(141, 16).mirror(false).addBox(-0.575f, 0, -1.35f, 0.5714f, 1.984f, 1.32f)
                .texOffs(141, 16).mirror(false).addBox(0, 0, -1.35f, 0.5714f, 1.984f, 1.32f)
                .texOffs(141, 16).mirror(false).addBox(0.575f, 0, -1.35f, 0.5714f, 1.734f, 1.32f), PartPose.offsetAndRotation(0, 3, 0, 0, 0, 0));
        PartDefinition leftFingers = leftHand.addOrReplaceChild("left_fingers", CubeListBuilder.create()
                .texOffs(141, 16).mirror(true).addBox(0.5786f, 0, -1.35f, 0.5714f, 1.734f, 1.32f)
                .texOffs(141, 16).mirror(true).addBox(0.0036f, 0, -1.35f, 0.5714f, 1.984f, 1.32f)
                .texOffs(141, 16).mirror(true).addBox(-0.5714f, 0, -1.35f, 0.5714f, 1.984f, 1.32f)
                .texOffs(141, 16).mirror(true).addBox(-1.1464f, 0, -1.35f, 0.5714f, 1.734f, 1.32f), PartPose.offsetAndRotation(0, 3, 0, 0, 0, 0));
        PartDefinition rightThumb = rightHand.addOrReplaceChild("right_thumb", CubeListBuilder.create()
                .texOffs(192, 16).mirror(false).addBox(-0.2f, 0, -0.5f, 0.6286f, 1.76f, 0.6286f), PartPose.offsetAndRotation(1, 1.12f, -1, 0.25f, 0, -0.35f));
        PartDefinition leftThumb = leftHand.addOrReplaceChild("left_thumb", CubeListBuilder.create()
                .texOffs(192, 16).mirror(true).addBox(-0.4286f, 0, -0.5f, 0.6286f, 1.76f, 0.6286f), PartPose.offsetAndRotation(-1, 1.12f, -1, 0.25f, 0, 0.35f));
        PartDefinition rightToes = rightFoot.addOrReplaceChild("right_toes", CubeListBuilder.create()
                .texOffs(197, 16).mirror(false).addBox(-1.2f, 0.325f, -1.3f, 0.5652f, 0.975f, 1.5f)
                .texOffs(197, 16).mirror(false).addBox(-0.6f, 0.325f, -1.6f, 0.5652f, 0.975f, 1.8f)
                .texOffs(197, 16).mirror(false).addBox(0, 0.325f, -1.6f, 0.5652f, 0.975f, 1.8f)
                .texOffs(197, 16).mirror(false).addBox(0.6f, 0.325f, -1.3f, 0.5652f, 0.975f, 1.5f), PartPose.offsetAndRotation(0, 0, -3.1f, 0, 0, 0));
        PartDefinition leftToes = leftFoot.addOrReplaceChild("left_toes", CubeListBuilder.create()
                .texOffs(197, 16).mirror(true).addBox(0.6348f, 0.325f, -1.3f, 0.5652f, 0.975f, 1.5f)
                .texOffs(197, 16).mirror(true).addBox(0.0348f, 0.325f, -1.6f, 0.5652f, 0.975f, 1.8f)
                .texOffs(197, 16).mirror(true).addBox(-0.5652f, 0.325f, -1.6f, 0.5652f, 0.975f, 1.8f)
                .texOffs(197, 16).mirror(true).addBox(-1.1652f, 0.325f, -1.3f, 0.5652f, 0.975f, 1.5f), PartPose.offsetAndRotation(0, 0, -3.1f, 0, 0, 0));
        PartDefinition rightElbowtuft = rightArm.addOrReplaceChild("right_elbowtuft", CubeListBuilder.create()
                .texOffs(148, 16).mirror(false).addBox(-0.88f, -0.4f, -0.3f, 1.76f, 1.98f, 1.6f), PartPose.offsetAndRotation(0, 7.2f, 0.99f, 0.7f, 0, 0));
        PartDefinition leftElbowtuft = leftArm.addOrReplaceChild("left_elbowtuft", CubeListBuilder.create()
                .texOffs(148, 16).mirror(true).addBox(-0.88f, -0.4f, -0.3f, 1.76f, 1.98f, 1.6f), PartPose.offsetAndRotation(0, 7.2f, 0.99f, 0.7f, 0, 0));
        PartDefinition rightForearmtuft = rightForearm.addOrReplaceChild("right_forearmtuft", CubeListBuilder.create()
                .texOffs(23, 16).mirror(false).addBox(-1.2f, -0.4f, -0.8f, 1.4f, 3.33f, 1.6f), PartPose.offsetAndRotation(-1, 2.59f, 0, 0, 0, 0.55f));
        PartDefinition leftForearmtuft = leftForearm.addOrReplaceChild("left_forearmtuft", CubeListBuilder.create()
                .texOffs(23, 16).mirror(true).addBox(-0.2f, -0.4f, -0.8f, 1.4f, 3.33f, 1.6f), PartPose.offsetAndRotation(1, 2.59f, 0, 0, 0, -0.55f));
        PartDefinition rightCalftuft = rightShin.addOrReplaceChild("right_calftuft", CubeListBuilder.create()
                .texOffs(86, 16).mirror(false).addBox(-0.96f, -0.2f, -0.4f, 1.92f, 2.07f, 1.4f), PartPose.offsetAndRotation(0, 0.96f, 1.2f, 0.45f, 0, 0));
        PartDefinition leftCalftuft = leftShin.addOrReplaceChild("left_calftuft", CubeListBuilder.create()
                .texOffs(86, 16).mirror(true).addBox(-0.96f, -0.2f, -0.4f, 1.92f, 2.07f, 1.4f), PartPose.offsetAndRotation(0, 0.96f, 1.2f, 0.45f, 0, 0));
        PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(13, 0).mirror(false).addBox(-3.3f, -5.9f, -3.1f, 6.6f, 5.9f, 6.2f)
                .texOffs(32, 16).mirror(false).addBox(-2.772f, -5.074f, -3.65f, 5.544f, 4.366f, 0.8f)
                .texOffs(157, 16).mirror(false).addBox(-1.5f, -2.478f, -4.8f, 3, 1.7f, 1.3f)
                .texOffs(204, 16).mirror(false).addBox(-1.56f, -1.528f, -4.95f, 3.12f, 0.8f, 1.4f)
                .texOffs(228, 16).mirror(false).addBox(-0.9f, -2.878f, -5.05f, 1.8f, 0.85f, 0.55f)
                .texOffs(117, 0).mirror(false).addBox(-3.399f, -6.136f, -1.085f, 6.798f, 3.894f, 4.34f)
                .texOffs(232, 0).mirror(false).addBox(-3.135f, -1.18f, -1.86f, 6.27f, 1.652f, 4.65f), PartPose.offsetAndRotation(0, -7, -0.8f, -0.2f, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(168, 16).mirror(false).addBox(-3.135f, -0.6f, -0.65f, 3.234f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -4.956f, -3.85f, 0, 0.18f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(168, 16).mirror(true).addBox(-0.099f, -0.6f, -0.65f, 3.234f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -4.956f, -3.85f, 0, -0.18f, -0.2f));
        PartDefinition rightCheekpad = head.addOrReplaceChild("right_cheekpad", CubeListBuilder.create()
                .texOffs(95, 16).mirror(false).addBox(-1.3f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(-1.65f, -2.242f, -3.55f, 0, 0.3f, 0));
        PartDefinition leftCheekpad = head.addOrReplaceChild("left_cheekpad", CubeListBuilder.create()
                .texOffs(95, 16).mirror(true).addBox(-1.1f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(1.65f, -2.242f, -3.55f, 0, -0.3f, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(181, 16).mirror(false).addBox(-1.29f, 0, -1.368f, 2.58f, 1.4f, 1.568f)
                .texOffs(217, 16).mirror(false).addBox(-1.2f, 1.2f, -0.66f, 2.4f, 1, 1.26f), PartPose.offsetAndRotation(0, -0.978f, -3.3f, 0.06f, 0, 0));
        PartDefinition rightCheek = head.addOrReplaceChild("right_cheek", CubeListBuilder.create()
                .texOffs(89, 0).mirror(false).addBox(-1.8f, -1.8f, -1.4f, 2.2f, 5.46f, 3.6f), PartPose.offsetAndRotation(-2.97f, -2.478f, -1.24f, 0.1f, 0.55f, 0.3f));
        PartDefinition leftCheek = head.addOrReplaceChild("left_cheek", CubeListBuilder.create()
                .texOffs(89, 0).mirror(true).addBox(-0.4f, -1.8f, -1.4f, 2.2f, 5.46f, 3.6f), PartPose.offsetAndRotation(2.97f, -2.478f, -1.24f, 0.1f, -0.55f, -0.3f));
        PartDefinition rightCheekLow = head.addOrReplaceChild("right_cheek_low", CubeListBuilder.create()
                .texOffs(174, 0).mirror(false).addBox(-1.5f, -0.6f, -1.2f, 1.9f, 4.16f, 3), PartPose.offsetAndRotation(-2.475f, -0.708f, -0.775f, 0.35f, 0.35f, 0.55f));
        PartDefinition leftCheekLow = head.addOrReplaceChild("left_cheek_low", CubeListBuilder.create()
                .texOffs(174, 0).mirror(true).addBox(-0.4f, -0.6f, -1.2f, 1.9f, 4.16f, 3), PartPose.offsetAndRotation(2.475f, -0.708f, -0.775f, 0.35f, -0.35f, -0.55f));
        PartDefinition crownlock0 = head.addOrReplaceChild("crownlock_0", CubeListBuilder.create()
                .texOffs(106, 16).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(0, -5.9f, -1.55f, -0.75f, 0, 0));
        PartDefinition crownlock1 = head.addOrReplaceChild("crownlock_1", CubeListBuilder.create()
                .texOffs(106, 16).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(-1.65f, -5.9f, -0.31f, -0.75f, 0.3f, 0.18f));
        PartDefinition crownlock2 = head.addOrReplaceChild("crownlock_2", CubeListBuilder.create()
                .texOffs(106, 16).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(1.65f, -5.9f, -0.31f, -0.75f, -0.3f, -0.18f));
        PartDefinition crownlock3 = head.addOrReplaceChild("crownlock_3", CubeListBuilder.create()
                .texOffs(106, 16).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(0, -5.9f, 1.085f, -0.75f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(121, 16).mirror(false).addBox(-0.9f, -1.02f, -0.85f, 1, 2.04f, 1.785f), PartPose.offsetAndRotation(-3.4f, -3.54f, -0.465f, 0, -0.35f, 0.18f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(121, 16).mirror(true).addBox(-0.1f, -1.02f, -0.85f, 1, 2.04f, 1.785f), PartPose.offsetAndRotation(3.4f, -3.54f, -0.465f, 0, 0.35f, -0.18f));
        PartDefinition rightShoulderfur = chest.addOrReplaceChild("right_shoulderfur", CubeListBuilder.create()
                .texOffs(185, 0).mirror(false).addBox(-1.4f, -0.6f, -2.2f, 2.6f, 2.6f, 4.4f), PartPose.offsetAndRotation(-3, -6.6f, 0, 0, 0, 0.3f));
        PartDefinition leftShoulderfur = chest.addOrReplaceChild("left_shoulderfur", CubeListBuilder.create()
                .texOffs(185, 0).mirror(true).addBox(-1.2f, -0.6f, -2.2f, 2.6f, 2.6f, 4.4f), PartPose.offsetAndRotation(3, -6.6f, 0, 0, 0, -0.3f));
        PartDefinition loincloth = body.addOrReplaceChild("loincloth", CubeListBuilder.create()
                .texOffs(47, 16).mirror(false).addBox(-2.2f, 0, -0.3f, 4.4f, 4.6f, 0.6f), PartPose.offsetAndRotation(0, -0.6f, -2, -0.05f, 0, 0));
        PartDefinition swayLoinBack = body.addOrReplaceChild("sway_loin_back", CubeListBuilder.create()
                .texOffs(128, 16).mirror(false).addBox(-2.2f, 0, -0.3f, 4.4f, 3.6f, 0.6f), PartPose.offsetAndRotation(0, -0.6f, 2, 0.1f, 0, 0));
        PartDefinition loinbelt = body.addOrReplaceChild("loinbelt", CubeListBuilder.create()
                .texOffs(0, 16).mirror(false).addBox(-2.7f, -0.4f, -2.1f, 5.4f, 1.2f, 4.2f), PartPose.offsetAndRotation(0, -1.6f, 0, 0, 0, 0));
        PartDefinition strap = chest.addOrReplaceChild("strap", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5f, -4.6f, -2.3f, 1, 9.2f, 4.6f), PartPose.offsetAndRotation(0, -3.6f, 0, 0, 0, 0.62f));
        PartDefinition bag = body.addOrReplaceChild("bag", CubeListBuilder.create()
                .texOffs(60, 16).mirror(false).addBox(-1.3f, 0, -1.4f, 2.6f, 2.8f, 2.8f)
                .texOffs(235, 16).mirror(false).addBox(-1, -0.8f, -1, 0.9f, 0.9f, 0.9f)
                .texOffs(235, 16).mirror(false).addBox(0.1f, -0.7f, -0.4f, 0.9f, 0.8f, 0.9f), PartPose.offsetAndRotation(-2.6f, -1.4f, 0.6f, 0, 0, 0.12f));
        PartDefinition stone = rightHand.addOrReplaceChild("stone", CubeListBuilder.create()
                .texOffs(73, 16).mirror(false).addBox(-1.2f, -0.6f, -1.2f, 2.4f, 2.2f, 2.4f), PartPose.offsetAndRotation(0, 2.6f, -0.2f, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED gibbon: end
    }

    public static LayerDefinition apeLayer() {
        // GENERATED ape: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(141, 0).mirror(false).addBox(-1.5f, -0.5f, -1.5f, 3, 5.7f, 3), PartPose.offsetAndRotation(-1.9f, 12.721f, 0.2f, -0.35f, 0, 0.05f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(141, 0).mirror(true).addBox(-1.5f, -0.5f, -1.5f, 3, 5.7f, 3), PartPose.offsetAndRotation(1.9f, 12.721f, 0.2f, -0.35f, 0, -0.05f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(154, 0).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 5.2f, 2.6f), PartPose.offsetAndRotation(0, 5.2f, 0, 0.6f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(154, 0).mirror(true).addBox(-1.3f, 0, -1.3f, 2.6f, 5.2f, 2.6f), PartPose.offsetAndRotation(0, 5.2f, 0, 0.6f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(36, 34).mirror(false).addBox(-1.4f, 0, -3, 2.8f, 1.3f, 4.6f), PartPose.offsetAndRotation(0, 5.2f, 0, -0.25f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(36, 34).mirror(true).addBox(-1.4f, 0, -3, 2.8f, 1.3f, 4.6f), PartPose.offsetAndRotation(0, 5.2f, 0, -0.25f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(211, 0).mirror(false).addBox(-2.8f, -3.2f, -2, 5.6f, 3.8f, 4), PartPose.offsetAndRotation(0, 12.721f, 0.3f, 0.16f, 0, 0));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(34, 0).mirror(false).addBox(-3.5f, -8, -2.4f, 7, 8, 4.8f), PartPose.offsetAndRotation(0, -2.8f, 0, 0.08f, 0, 0));
        PartDefinition rightArm = chest.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(88, 0).mirror(false).addBox(-1.25f, -1.125f, -1.25f, 2.5f, 7.525f, 2.5f), PartPose.offsetAndRotation(-4.375f, -6.625f, 0, -0.2f, 0, 0.3f));
        PartDefinition leftArm = chest.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(88, 0).mirror(true).addBox(-1.25f, -1.125f, -1.25f, 2.5f, 7.525f, 2.5f), PartPose.offsetAndRotation(4.375f, -6.625f, 0, -0.2f, 0, -0.3f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(101, 0).mirror(false).addBox(-1.15f, 0, -1.15f, 2.3f, 6.2f, 2.3f), PartPose.offsetAndRotation(0, 6.4f, 0, -0.45f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(101, 0).mirror(true).addBox(-1.15f, 0, -1.15f, 2.3f, 6.2f, 2.3f), PartPose.offsetAndRotation(0, 6.4f, 0, -0.45f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(119, 34).mirror(false).addBox(-1.25f, 0, -1.25f, 2.5f, 2.8f, 2.5f), PartPose.offsetAndRotation(0, 6.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(119, 34).mirror(true).addBox(-1.25f, 0, -1.25f, 2.5f, 2.8f, 2.5f), PartPose.offsetAndRotation(0, 6.2f, 0, 0, 0, 0));
        PartDefinition rightFingers = rightHand.addOrReplaceChild("right_fingers", CubeListBuilder.create()
                .texOffs(33, 43).mirror(false).addBox(-1.2f, 0, -1.4f, 0.5952f, 1.486f, 1.375f)
                .texOffs(33, 43).mirror(false).addBox(-0.6f, 0, -1.4f, 0.5952f, 1.736f, 1.375f)
                .texOffs(33, 43).mirror(false).addBox(0, 0, -1.4f, 0.5952f, 1.736f, 1.375f)
                .texOffs(33, 43).mirror(false).addBox(0.6f, 0, -1.4f, 0.5952f, 1.486f, 1.375f), PartPose.offsetAndRotation(0, 2.6f, 0, 0, 0, 0));
        PartDefinition leftFingers = leftHand.addOrReplaceChild("left_fingers", CubeListBuilder.create()
                .texOffs(33, 43).mirror(true).addBox(0.6048f, 0, -1.4f, 0.5952f, 1.486f, 1.375f)
                .texOffs(33, 43).mirror(true).addBox(0.0048f, 0, -1.4f, 0.5952f, 1.736f, 1.375f)
                .texOffs(33, 43).mirror(true).addBox(-0.5952f, 0, -1.4f, 0.5952f, 1.736f, 1.375f)
                .texOffs(33, 43).mirror(true).addBox(-1.1952f, 0, -1.4f, 0.5952f, 1.486f, 1.375f), PartPose.offsetAndRotation(0, 2.6f, 0, 0, 0, 0));
        PartDefinition rightThumb = rightHand.addOrReplaceChild("right_thumb", CubeListBuilder.create()
                .texOffs(71, 43).mirror(false).addBox(-0.2f, 0, -0.5f, 0.6548f, 1.54f, 0.6548f), PartPose.offsetAndRotation(1.05f, 0.98f, -1.05f, 0.25f, 0, -0.35f));
        PartDefinition leftThumb = leftHand.addOrReplaceChild("left_thumb", CubeListBuilder.create()
                .texOffs(71, 43).mirror(true).addBox(-0.4548f, 0, -0.5f, 0.6548f, 1.54f, 0.6548f), PartPose.offsetAndRotation(-1.05f, 0.98f, -1.05f, 0.25f, 0, 0.35f));
        PartDefinition rightToes = rightFoot.addOrReplaceChild("right_toes", CubeListBuilder.create()
                .texOffs(76, 43).mirror(false).addBox(-1.3f, 0.325f, -1.3f, 0.6087f, 0.975f, 1.5f)
                .texOffs(76, 43).mirror(false).addBox(-0.65f, 0.325f, -1.6f, 0.6087f, 0.975f, 1.8f)
                .texOffs(76, 43).mirror(false).addBox(0, 0.325f, -1.6f, 0.6087f, 0.975f, 1.8f)
                .texOffs(76, 43).mirror(false).addBox(0.65f, 0.325f, -1.3f, 0.6087f, 0.975f, 1.5f), PartPose.offsetAndRotation(0, 0, -3, 0, 0, 0));
        PartDefinition leftToes = leftFoot.addOrReplaceChild("left_toes", CubeListBuilder.create()
                .texOffs(76, 43).mirror(true).addBox(0.6913f, 0.325f, -1.3f, 0.6087f, 0.975f, 1.5f)
                .texOffs(76, 43).mirror(true).addBox(0.0413f, 0.325f, -1.6f, 0.6087f, 0.975f, 1.8f)
                .texOffs(76, 43).mirror(true).addBox(-0.6087f, 0.325f, -1.6f, 0.6087f, 0.975f, 1.8f)
                .texOffs(76, 43).mirror(true).addBox(-1.2587f, 0.325f, -1.3f, 0.6087f, 0.975f, 1.5f), PartPose.offsetAndRotation(0, 0, -3, 0, 0, 0));
        PartDefinition rightElbowtuft = rightArm.addOrReplaceChild("right_elbowtuft", CubeListBuilder.create()
                .texOffs(198, 34).mirror(false).addBox(-1, -0.4f, -0.3f, 2, 2.25f, 1.6f), PartPose.offsetAndRotation(0, 5.8f, 1.125f, 0.7f, 0, 0));
        PartDefinition leftElbowtuft = leftArm.addOrReplaceChild("left_elbowtuft", CubeListBuilder.create()
                .texOffs(198, 34).mirror(true).addBox(-1, -0.4f, -0.3f, 2, 2.25f, 1.6f), PartPose.offsetAndRotation(0, 5.8f, 1.125f, 0.7f, 0, 0));
        PartDefinition rightForearmtuft = rightForearm.addOrReplaceChild("right_forearmtuft", CubeListBuilder.create()
                .texOffs(198, 34).mirror(false).addBox(-1.2f, -0.4f, -0.92f, 1.4f, 2.79f, 1.84f), PartPose.offsetAndRotation(-1.15f, 2.17f, 0, 0, 0, 0.55f));
        PartDefinition leftForearmtuft = leftForearm.addOrReplaceChild("left_forearmtuft", CubeListBuilder.create()
                .texOffs(198, 34).mirror(true).addBox(-0.2f, -0.4f, -0.92f, 1.4f, 2.79f, 1.84f), PartPose.offsetAndRotation(1.15f, 2.17f, 0, 0, 0, -0.55f));
        PartDefinition rightCalftuft = rightShin.addOrReplaceChild("right_calftuft", CubeListBuilder.create()
                .texOffs(207, 34).mirror(false).addBox(-1.04f, -0.2f, -0.4f, 2.08f, 2.34f, 1.4f), PartPose.offsetAndRotation(0, 1.04f, 1.3f, 0.45f, 0, 0));
        PartDefinition leftCalftuft = leftShin.addOrReplaceChild("left_calftuft", CubeListBuilder.create()
                .texOffs(207, 34).mirror(true).addBox(-1.04f, -0.2f, -0.4f, 2.08f, 2.34f, 1.4f), PartPose.offsetAndRotation(0, 1.04f, 1.3f, 0.45f, 0, 0));
        PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-3.5f, -6.1f, -3.3f, 7, 6.1f, 6.6f)
                .texOffs(132, 34).mirror(false).addBox(-2.94f, -5.246f, -3.85f, 5.88f, 4.514f, 0.8f)
                .texOffs(218, 34).mirror(false).addBox(-1.9f, -2.562f, -5.6f, 3.8f, 2.1f, 1.9f)
                .texOffs(83, 43).mirror(false).addBox(-1.976f, -1.212f, -5.75f, 3.952f, 0.8f, 2)
                .texOffs(96, 43).mirror(false).addBox(-1.14f, -2.962f, -5.85f, 2.28f, 1.05f, 0.55f)
                .texOffs(114, 0).mirror(false).addBox(-3.605f, -6.344f, -1.155f, 7.21f, 4.026f, 4.62f)
                .texOffs(53, 34).mirror(false).addBox(-3.325f, -1.22f, -1.98f, 6.65f, 1.708f, 4.95f), PartPose.offsetAndRotation(0, -8, -1, -0.12f, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(40, 43).mirror(false).addBox(-3.325f, -0.6f, -0.65f, 3.43f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -5.124f, -4.05f, 0, 0.18f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(40, 43).mirror(true).addBox(-0.105f, -0.6f, -0.65f, 3.43f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -5.124f, -4.05f, 0, -0.18f, -0.2f));
        PartDefinition rightCheekpad = head.addOrReplaceChild("right_cheekpad", CubeListBuilder.create()
                .texOffs(231, 34).mirror(false).addBox(-1.3f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(-1.75f, -2.318f, -3.75f, 0, 0.3f, 0));
        PartDefinition leftCheekpad = head.addOrReplaceChild("left_cheekpad", CubeListBuilder.create()
                .texOffs(231, 34).mirror(true).addBox(-1.1f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(1.75f, -2.318f, -3.75f, 0, -0.3f, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(242, 34).mirror(false).addBox(-1.634f, 0, -1.896f, 3.268f, 1.4f, 2.096f)
                .texOffs(105, 43).mirror(false).addBox(-1.52f, 1.2f, -1.02f, 3.04f, 1, 1.62f), PartPose.offsetAndRotation(0, -0.662f, -3.5f, 0.06f, 0, 0));
        PartDefinition rightCheek = head.addOrReplaceChild("right_cheek", CubeListBuilder.create()
                .texOffs(167, 0).mirror(false).addBox(-1.8f, -1.8f, -1.4f, 2.2f, 4.2f, 3.6f), PartPose.offsetAndRotation(-3.15f, -2.562f, -1.32f, 0.1f, 0.55f, 0.3f));
        PartDefinition leftCheek = head.addOrReplaceChild("left_cheek", CubeListBuilder.create()
                .texOffs(167, 0).mirror(true).addBox(-0.4f, -1.8f, -1.4f, 2.2f, 4.2f, 3.6f), PartPose.offsetAndRotation(3.15f, -2.562f, -1.32f, 0.1f, -0.55f, -0.3f));
        PartDefinition rightCheekLow = head.addOrReplaceChild("right_cheek_low", CubeListBuilder.create()
                .texOffs(78, 34).mirror(false).addBox(-1.5f, -0.6f, -1.2f, 1.9f, 3.2f, 3), PartPose.offsetAndRotation(-2.625f, -0.732f, -0.825f, 0.35f, 0.35f, 0.55f));
        PartDefinition leftCheekLow = head.addOrReplaceChild("left_cheek_low", CubeListBuilder.create()
                .texOffs(78, 34).mirror(true).addBox(-0.4f, -0.6f, -1.2f, 1.9f, 3.2f, 3), PartPose.offsetAndRotation(2.625f, -0.732f, -0.825f, 0.35f, -0.35f, -0.55f));
        PartDefinition crownlock0 = head.addOrReplaceChild("crownlock_0", CubeListBuilder.create()
                .texOffs(0, 43).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(0, -6.1f, -1.65f, -0.75f, 0, 0));
        PartDefinition crownlock1 = head.addOrReplaceChild("crownlock_1", CubeListBuilder.create()
                .texOffs(0, 43).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(-1.75f, -6.1f, -0.33f, -0.75f, 0.3f, 0.18f));
        PartDefinition crownlock2 = head.addOrReplaceChild("crownlock_2", CubeListBuilder.create()
                .texOffs(0, 43).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(1.75f, -6.1f, -0.33f, -0.75f, -0.3f, -0.18f));
        PartDefinition crownlock3 = head.addOrReplaceChild("crownlock_3", CubeListBuilder.create()
                .texOffs(0, 43).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(0, -6.1f, 1.155f, -0.75f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(15, 43).mirror(false).addBox(-0.9f, -1.14f, -0.95f, 1, 2.28f, 1.995f), PartPose.offsetAndRotation(-3.6f, -3.66f, -0.495f, 0, -0.35f, 0.18f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(15, 43).mirror(true).addBox(-0.1f, -1.14f, -0.95f, 1, 2.28f, 1.995f), PartPose.offsetAndRotation(3.6f, -3.66f, -0.495f, 0, 0.35f, -0.18f));
        PartDefinition cuirass = chest.addOrReplaceChild("cuirass", CubeListBuilder.create()
                .texOffs(59, 0).mirror(false).addBox(-3.9f, -7.6f, -2.8f, 7.8f, 6.4f, 5.6f)
                .texOffs(0, 34).mirror(false).addBox(-3.6f, -1.6f, -2.7f, 7.2f, 1.6f, 5.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(89, 34).mirror(false).addBox(-1.8f, -1.2f, -1.8f, 3.2f, 2.4f, 3.6f), PartPose.offsetAndRotation(-0.6f, -0.8f, 0, 0, 0, 0.28f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(89, 34).mirror(true).addBox(-1.4f, -1.2f, -1.8f, 3.2f, 2.4f, 3.6f), PartPose.offsetAndRotation(0.6f, -0.8f, 0, 0, 0, -0.28f));
        PartDefinition headband = head.addOrReplaceChild("headband", CubeListBuilder.create()
                .texOffs(182, 0).mirror(false).addBox(-3.35f, -0.6f, -3.2f, 6.7f, 1.3f, 6.4f), PartPose.offsetAndRotation(0, -4.1f, 0, 0, 0, 0));
        PartDefinition swayBandtail = headband.addOrReplaceChild("sway_bandtail", CubeListBuilder.create()
                .texOffs(147, 34).mirror(false).addBox(-0.5f, 0, -0.15f, 1, 5, 0.3f), PartPose.offsetAndRotation(0.6f, 0, 3.1f, 0.5f, 0.15f, 0));
        PartDefinition swayBandtail2 = headband.addOrReplaceChild("sway_bandtail2", CubeListBuilder.create()
                .texOffs(147, 34).mirror(false).addBox(-0.45f, 0, -0.15f, 0.9f, 4.2f, 0.3f), PartPose.offsetAndRotation(-0.6f, 0, 3.1f, 0.75f, -0.2f, 0));
        PartDefinition rightTasset = body.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(152, 34).mirror(false).addBox(-1.4f, 0, -0.4f, 2.8f, 4.6f, 0.8f), PartPose.offsetAndRotation(-1.4f, -0.4f, -2.1f, -0.08f, 0, 0.05f));
        PartDefinition leftTasset = body.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(152, 34).mirror(true).addBox(-1.4f, 0, -0.4f, 2.8f, 4.6f, 0.8f), PartPose.offsetAndRotation(1.4f, -0.4f, -2.1f, -0.08f, 0, -0.05f));
        PartDefinition swayBackTasset = body.addOrReplaceChild("sway_back_tasset", CubeListBuilder.create()
                .texOffs(161, 34).mirror(false).addBox(-2.6f, 0, -0.4f, 5.2f, 4.2f, 0.8f), PartPose.offsetAndRotation(0, -0.4f, 2.1f, 0.1f, 0, 0));
        PartDefinition rightWrap = rightShin.addOrReplaceChild("right_wrap", CubeListBuilder.create()
                .texOffs(106, 34).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 3.6f, 3), PartPose.offsetAndRotation(0, 0.6f, 0, 0, 0, 0));
        PartDefinition leftWrap = leftShin.addOrReplaceChild("left_wrap", CubeListBuilder.create()
                .texOffs(106, 34).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 3.6f, 3), PartPose.offsetAndRotation(0, 0.6f, 0, 0, 0, 0));
        PartDefinition tail0 = body.addOrReplaceChild("tail_0", CubeListBuilder.create()
                .texOffs(176, 34).mirror(false).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 3.3f), PartPose.offsetAndRotation(0, -1.6f, 1.9f, 1.15f, 0, 0));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(176, 34).mirror(false).addBox(-0.65f, -0.65f, 0, 1.3f, 1.3f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.32f, 0, 0));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(176, 34).mirror(false).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.32f, 0, 0));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create()
                .texOffs(22, 43).mirror(false).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.32f, 0, 0));
        PartDefinition spear = rightHand.addOrReplaceChild("spear", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.45f, -26, -0.45f, 0.9f, 32, 0.9f)
                .texOffs(53, 43).mirror(false).addBox(-0.6f, -26.8f, -0.6f, 1.2f, 1.4f, 1.2f)
                .texOffs(29, 34).mirror(false).addBox(-0.25f, -32.6f, -0.85f, 0.5f, 6, 1.7f)
                .texOffs(62, 43).mirror(false).addBox(-0.55f, 5.6f, -0.55f, 1.1f, 1.4f, 1.1f), PartPose.offsetAndRotation(0, 1.6f, 0, 0.4081f, 0.1798f, -0.2415f));
        PartDefinition swayTassel = spear.addOrReplaceChild("sway_tassel", CubeListBuilder.create()
                .texOffs(189, 34).mirror(false).addBox(-0.7f, 0, -0.7f, 1.4f, 3.2f, 1.4f), PartPose.offsetAndRotation(0, -25.6f, 0, 0.05f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED ape: end
    }

    public static LayerDefinition baboonLayer() {
        // GENERATED baboon: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(180, 0).mirror(false).addBox(-2.1f, -0.5f, -2.1f, 4.2f, 6.1f, 4.2f), PartPose.offsetAndRotation(-2.6f, 11.4915f, 0.2f, -0.95f, 0, 0.08f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(180, 0).mirror(true).addBox(-2.1f, -0.5f, -2.1f, 4.2f, 6.1f, 4.2f), PartPose.offsetAndRotation(2.6f, 11.4915f, 0.2f, -0.95f, 0, -0.08f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(0, 19).mirror(false).addBox(-1.7f, 0, -1.7f, 3.4f, 5.2f, 3.4f), PartPose.offsetAndRotation(0, 5.6f, 0, 1.05f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(0, 19).mirror(true).addBox(-1.7f, 0, -1.7f, 3.4f, 5.2f, 3.4f), PartPose.offsetAndRotation(0, 5.6f, 0, 1.05f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(165, 19).mirror(false).addBox(-1.9f, 0, -3.4f, 3.8f, 1.6f, 5.4f), PartPose.offsetAndRotation(0, 5.2f, 0, -0.15f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(165, 19).mirror(true).addBox(-1.9f, 0, -3.4f, 3.8f, 1.6f, 5.4f), PartPose.offsetAndRotation(0, 5.2f, 0, -0.15f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(201, 0).mirror(false).addBox(-4, -4, -3, 8, 4.6f, 6), PartPose.offsetAndRotation(0, 11.4915f, 0.3f, 0.62f, 0, 0));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-5.5f, -10, -3.8f, 11, 10, 7.6f), PartPose.offsetAndRotation(0, -3.6f, 0, 0.28f, 0, 0));
        PartDefinition rightArm = chest.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(84, 0).mirror(false).addBox(-2, -1.8f, -2, 4, 10.2f, 4), PartPose.offsetAndRotation(-6.9f, -7.8f, 0, -0.75f, 0, 0.16f));
        PartDefinition leftArm = chest.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(84, 0).mirror(true).addBox(-2, -1.8f, -2, 4, 10.2f, 4), PartPose.offsetAndRotation(6.9f, -7.8f, 0, -0.75f, 0, -0.16f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(134, 0).mirror(false).addBox(-1.9f, 0, -1.9f, 3.8f, 8.6f, 3.8f), PartPose.offsetAndRotation(0, 8.4f, 0, -0.2f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(134, 0).mirror(true).addBox(-1.9f, 0, -1.9f, 3.8f, 8.6f, 3.8f), PartPose.offsetAndRotation(0, 8.4f, 0, -0.2f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(65, 19).mirror(false).addBox(-2.1f, 0, -2.1f, 4.2f, 3.2f, 4.2f), PartPose.offsetAndRotation(0, 8.6f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(65, 19).mirror(true).addBox(-2.1f, 0, -2.1f, 4.2f, 3.2f, 4.2f), PartPose.offsetAndRotation(0, 8.6f, 0, 0, 0, 0));
        PartDefinition rightFingers = rightHand.addOrReplaceChild("right_fingers", CubeListBuilder.create()
                .texOffs(132, 30).mirror(false).addBox(-2.05f, 0, -2.25f, 1, 1.734f, 2.31f)
                .texOffs(132, 30).mirror(false).addBox(-1.025f, 0, -2.25f, 1, 1.984f, 2.31f)
                .texOffs(132, 30).mirror(false).addBox(0, 0, -2.25f, 1, 1.984f, 2.31f)
                .texOffs(132, 30).mirror(false).addBox(1.025f, 0, -2.25f, 1, 1.734f, 2.31f), PartPose.offsetAndRotation(0, 3, 0, 0, 0, 0));
        PartDefinition leftFingers = leftHand.addOrReplaceChild("left_fingers", CubeListBuilder.create()
                .texOffs(132, 30).mirror(true).addBox(1.05f, 0, -2.25f, 1, 1.734f, 2.31f)
                .texOffs(132, 30).mirror(true).addBox(0.025f, 0, -2.25f, 1, 1.984f, 2.31f)
                .texOffs(132, 30).mirror(true).addBox(-1, 0, -2.25f, 1, 1.984f, 2.31f)
                .texOffs(132, 30).mirror(true).addBox(-2.025f, 0, -2.25f, 1, 1.734f, 2.31f), PartPose.offsetAndRotation(0, 3, 0, 0, 0, 0));
        PartDefinition rightThumb = rightHand.addOrReplaceChild("right_thumb", CubeListBuilder.create()
                .texOffs(189, 30).mirror(false).addBox(-0.2f, 0, -0.5f, 1.1f, 1.76f, 1.1f), PartPose.offsetAndRotation(1.9f, 1.12f, -1.9f, 0.25f, 0, -0.35f));
        PartDefinition leftThumb = leftHand.addOrReplaceChild("left_thumb", CubeListBuilder.create()
                .texOffs(189, 30).mirror(true).addBox(-0.9f, 0, -0.5f, 1.1f, 1.76f, 1.1f), PartPose.offsetAndRotation(-1.9f, 1.12f, -1.9f, 0.25f, 0, 0.35f));
        PartDefinition rightToes = rightFoot.addOrReplaceChild("right_toes", CubeListBuilder.create()
                .texOffs(198, 30).mirror(false).addBox(-1.8f, 0.4f, -1.3f, 0.8261f, 1.2f, 1.5f)
                .texOffs(198, 30).mirror(false).addBox(-0.9f, 0.4f, -1.6f, 0.8261f, 1.2f, 1.8f)
                .texOffs(198, 30).mirror(false).addBox(0, 0.4f, -1.6f, 0.8261f, 1.2f, 1.8f)
                .texOffs(198, 30).mirror(false).addBox(0.9f, 0.4f, -1.3f, 0.8261f, 1.2f, 1.5f), PartPose.offsetAndRotation(0, 0, -3.4f, 0, 0, 0));
        PartDefinition leftToes = leftFoot.addOrReplaceChild("left_toes", CubeListBuilder.create()
                .texOffs(198, 30).mirror(true).addBox(0.9739f, 0.4f, -1.3f, 0.8261f, 1.2f, 1.5f)
                .texOffs(198, 30).mirror(true).addBox(0.0739f, 0.4f, -1.6f, 0.8261f, 1.2f, 1.8f)
                .texOffs(198, 30).mirror(true).addBox(-0.8261f, 0.4f, -1.6f, 0.8261f, 1.2f, 1.8f)
                .texOffs(198, 30).mirror(true).addBox(-1.7261f, 0.4f, -1.3f, 0.8261f, 1.2f, 1.5f), PartPose.offsetAndRotation(0, 0, -3.4f, 0, 0, 0));
        PartDefinition rightElbowtuft = rightArm.addOrReplaceChild("right_elbowtuft", CubeListBuilder.create()
                .texOffs(55, 30).mirror(false).addBox(-1.6f, -0.4f, -0.3f, 3.2f, 3.6f, 1.6f), PartPose.offsetAndRotation(0, 7.8f, 1.8f, 0.7f, 0, 0));
        PartDefinition leftElbowtuft = leftArm.addOrReplaceChild("left_elbowtuft", CubeListBuilder.create()
                .texOffs(55, 30).mirror(true).addBox(-1.6f, -0.4f, -0.3f, 3.2f, 3.6f, 1.6f), PartPose.offsetAndRotation(0, 7.8f, 1.8f, 0.7f, 0, 0));
        PartDefinition rightForearmtuft = rightForearm.addOrReplaceChild("right_forearmtuft", CubeListBuilder.create()
                .texOffs(186, 19).mirror(false).addBox(-1.2f, -0.4f, -1.52f, 1.4f, 3.87f, 3.04f), PartPose.offsetAndRotation(-1.9f, 3.01f, 0, 0, 0, 0.55f));
        PartDefinition leftForearmtuft = leftForearm.addOrReplaceChild("left_forearmtuft", CubeListBuilder.create()
                .texOffs(186, 19).mirror(true).addBox(-0.2f, -0.4f, -1.52f, 1.4f, 3.87f, 3.04f), PartPose.offsetAndRotation(1.9f, 3.01f, 0, 0, 0, -0.55f));
        PartDefinition rightCalftuft = rightShin.addOrReplaceChild("right_calftuft", CubeListBuilder.create()
                .texOffs(141, 30).mirror(false).addBox(-1.36f, -0.2f, -0.4f, 2.72f, 2.34f, 1.4f), PartPose.offsetAndRotation(0, 1.36f, 1.7f, 0.45f, 0, 0));
        PartDefinition leftCalftuft = leftShin.addOrReplaceChild("left_calftuft", CubeListBuilder.create()
                .texOffs(141, 30).mirror(true).addBox(-1.36f, -0.2f, -0.4f, 2.72f, 2.34f, 1.4f), PartPose.offsetAndRotation(0, 1.36f, 1.7f, 0.45f, 0, 0));
        PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(101, 0).mirror(false).addBox(-4, -6, -3.7f, 8, 6, 7.4f)
                .texOffs(68, 30).mirror(false).addBox(-3.36f, -5.16f, -4.25f, 6.72f, 4.44f, 0.8f)
                .texOffs(86, 19).mirror(false).addBox(-2.4f, -2.52f, -10.1f, 4.8f, 3, 6)
                .texOffs(199, 19).mirror(false).addBox(-2.496f, -0.27f, -10.25f, 4.992f, 0.8f, 6.1f)
                .texOffs(225, 30).mirror(false).addBox(-1.44f, -2.92f, -10.35f, 2.88f, 1.5f, 0.55f)
                .texOffs(17, 19).mirror(false).addBox(-4.12f, -6.24f, -1.295f, 8.24f, 3.96f, 5.18f)
                .texOffs(224, 19).mirror(false).addBox(-3.8f, -1.2f, -2.22f, 7.6f, 1.68f, 5.55f), PartPose.offsetAndRotation(0, -9, -3.2f, -0.75f, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(205, 30).mirror(false).addBox(-3.8f, -0.6f, -0.65f, 3.92f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -5.04f, -4.45f, 0, 0.18f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(205, 30).mirror(true).addBox(-0.12f, -0.6f, -0.65f, 3.92f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -5.04f, -4.45f, 0, -0.18f, -0.2f));
        PartDefinition rightCheekpad = head.addOrReplaceChild("right_cheekpad", CubeListBuilder.create()
                .texOffs(152, 30).mirror(false).addBox(-1.3f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(-2, -2.28f, -4.15f, 0, 0.3f, 0));
        PartDefinition leftCheekpad = head.addOrReplaceChild("left_cheekpad", CubeListBuilder.create()
                .texOffs(152, 30).mirror(true).addBox(-1.1f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(2, -2.28f, -4.15f, 0, -0.3f, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(109, 19).mirror(false).addBox(-2.064f, 0, -5.504f, 4.128f, 2.2f, 5.704f)
                .texOffs(85, 30).mirror(false).addBox(-1.92f, 2, -3.48f, 3.84f, 1, 4.08f), PartPose.offsetAndRotation(0, 0.28f, -3.9f, 0.06f, 0, 0));
        PartDefinition rightCheek = head.addOrReplaceChild("right_cheek", CubeListBuilder.create()
                .texOffs(230, 0).mirror(false).addBox(-1.8f, -1.8f, -1.4f, 2.2f, 6.3f, 3.6f), PartPose.offsetAndRotation(-3.6f, -2.52f, -1.48f, 0.1f, 0.55f, 0.3f));
        PartDefinition leftCheek = head.addOrReplaceChild("left_cheek", CubeListBuilder.create()
                .texOffs(230, 0).mirror(true).addBox(-0.4f, -1.8f, -1.4f, 2.2f, 6.3f, 3.6f), PartPose.offsetAndRotation(3.6f, -2.52f, -1.48f, 0.1f, -0.55f, -0.3f));
        PartDefinition rightCheekLow = head.addOrReplaceChild("right_cheek_low", CubeListBuilder.create()
                .texOffs(0, 30).mirror(false).addBox(-1.5f, -0.6f, -1.2f, 1.9f, 4.8f, 3), PartPose.offsetAndRotation(-3, -0.72f, -0.925f, 0.35f, 0.35f, 0.55f));
        PartDefinition leftCheekLow = head.addOrReplaceChild("left_cheek_low", CubeListBuilder.create()
                .texOffs(0, 30).mirror(true).addBox(-0.4f, -0.6f, -1.2f, 1.9f, 4.8f, 3), PartPose.offsetAndRotation(3, -0.72f, -0.925f, 0.35f, -0.35f, -0.55f));
        PartDefinition crownlock0 = head.addOrReplaceChild("crownlock_0", CubeListBuilder.create()
                .texOffs(163, 30).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(0, -6, -1.85f, -0.75f, 0, 0));
        PartDefinition crownlock1 = head.addOrReplaceChild("crownlock_1", CubeListBuilder.create()
                .texOffs(163, 30).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(-2, -6, -0.37f, -0.75f, 0.3f, 0.18f));
        PartDefinition crownlock2 = head.addOrReplaceChild("crownlock_2", CubeListBuilder.create()
                .texOffs(163, 30).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(2, -6, -0.37f, -0.75f, -0.3f, -0.18f));
        PartDefinition crownlock3 = head.addOrReplaceChild("crownlock_3", CubeListBuilder.create()
                .texOffs(163, 30).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(0, -6, 1.295f, -0.75f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(218, 30).mirror(false).addBox(-0.9f, -0.9f, -0.75f, 1, 1.8f, 1.575f), PartPose.offsetAndRotation(-4.1f, -3.6f, -0.555f, 0, -0.35f, 0.18f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(218, 30).mirror(true).addBox(-0.1f, -0.9f, -0.75f, 1, 1.8f, 1.575f), PartPose.offsetAndRotation(4.1f, -3.6f, -0.555f, 0, 0.35f, -0.18f));
        PartDefinition mane = chest.addOrReplaceChild("mane", CubeListBuilder.create()
                .texOffs(39, 0).mirror(false).addBox(-6.4f, -1, -3.6f, 12.8f, 9, 8.6f), PartPose.offsetAndRotation(0, -10, 0.6f, 0.35f, 0, 0));
        PartDefinition swayManeBack = chest.addOrReplaceChild("sway_mane_back", CubeListBuilder.create()
                .texOffs(151, 0).mirror(false).addBox(-5.6f, 0, -0.6f, 11.2f, 11, 1.8f), PartPose.offsetAndRotation(0, -9.6f, 4.4f, 0.25f, 0, 0));
        PartDefinition rightManefront = chest.addOrReplaceChild("right_manefront", CubeListBuilder.create()
                .texOffs(48, 19).mirror(false).addBox(-2.6f, 0, -0.6f, 5.2f, 7.5f, 1.4f), PartPose.offsetAndRotation(-3.2f, -9.6f, -3.6f, -0.2f, 0.15f, 0.1f));
        PartDefinition leftManefront = chest.addOrReplaceChild("left_manefront", CubeListBuilder.create()
                .texOffs(48, 19).mirror(true).addBox(-2.6f, 0, -0.6f, 5.2f, 7.5f, 1.4f), PartPose.offsetAndRotation(3.2f, -9.6f, -3.6f, -0.2f, -0.15f, -0.1f));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(11, 30).mirror(false).addBox(-2.2f, -1.2f, -2.4f, 4.4f, 2, 5.6f), PartPose.offsetAndRotation(0, -5.3f, 0.6f, 0.2f, 0, 0));
        PartDefinition rightBand = rightForearm.addOrReplaceChild("right_band", CubeListBuilder.create()
                .texOffs(34, 30).mirror(false).addBox(-2.2f, 0, -2.2f, 4.4f, 1.6f, 4.4f)
                .texOffs(34, 30).mirror(false).addBox(-2.2f, 3, -2.2f, 4.4f, 1.6f, 4.4f), PartPose.offsetAndRotation(0, 2.6f, 0, 0, 0, 0));
        PartDefinition leftBand = leftForearm.addOrReplaceChild("left_band", CubeListBuilder.create()
                .texOffs(34, 30).mirror(true).addBox(-2.2f, 0, -2.2f, 4.4f, 1.6f, 4.4f)
                .texOffs(34, 30).mirror(true).addBox(-2.2f, 3, -2.2f, 4.4f, 1.6f, 4.4f), PartPose.offsetAndRotation(0, 2.6f, 0, 0, 0, 0));
        PartDefinition rightFang = jaw.addOrReplaceChild("right_fang", CubeListBuilder.create()
                .texOffs(234, 30).mirror(false).addBox(-0.35f, -1.6f, -0.35f, 0.7f, 1.8f, 0.7f), PartPose.offsetAndRotation(-1.3f, -0.4f, -5.2f, 0, 0, 0));
        PartDefinition leftFang = jaw.addOrReplaceChild("left_fang", CubeListBuilder.create()
                .texOffs(234, 30).mirror(true).addBox(-0.35f, -1.6f, -0.35f, 0.7f, 1.8f, 0.7f), PartPose.offsetAndRotation(1.3f, -0.4f, -5.2f, 0, 0, 0));
        PartDefinition rightUpfang = head.addOrReplaceChild("right_upfang", CubeListBuilder.create()
                .texOffs(234, 30).mirror(false).addBox(-0.35f, 0, -0.35f, 0.7f, 1.7f, 0.7f), PartPose.offsetAndRotation(-1.5f, 0.332f, -8.6f, 0, 0, 0));
        PartDefinition leftUpfang = head.addOrReplaceChild("left_upfang", CubeListBuilder.create()
                .texOffs(234, 30).mirror(true).addBox(-0.35f, 0, -0.35f, 0.7f, 1.7f, 0.7f), PartPose.offsetAndRotation(1.5f, 0.332f, -8.6f, 0, 0, 0));
        PartDefinition loin = body.addOrReplaceChild("loin", CubeListBuilder.create()
                .texOffs(104, 30).mirror(false).addBox(-2.8f, 0, -0.4f, 5.6f, 4.8f, 0.8f), PartPose.offsetAndRotation(0, -1, -3.1f, -0.12f, 0, 0));
        PartDefinition belt = body.addOrReplaceChild("belt", CubeListBuilder.create()
                .texOffs(132, 19).mirror(false).addBox(-4.2f, -0.6f, -3.2f, 8.4f, 1.6f, 6.4f), PartPose.offsetAndRotation(0, -2.4f, 0, 0, 0, 0));
        PartDefinition tail0 = body.addOrReplaceChild("tail_0", CubeListBuilder.create()
                .texOffs(119, 30).mirror(false).addBox(-1.1f, -1.1f, 0, 2.2f, 2.2f, 2.9f), PartPose.offsetAndRotation(0, -2.6f, 2.9f, 0.25f, 0, 0));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(178, 30).mirror(false).addBox(-0.9f, -0.9f, 0, 1.8f, 1.8f, 2.9f), PartPose.offsetAndRotation(0, 0, 2.6f, -0.5f, 0, 0));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(178, 30).mirror(false).addBox(-0.7f, -0.7f, 0, 1.4f, 1.4f, 2.9f), PartPose.offsetAndRotation(0, 0, 2.6f, -0.5f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED baboon: end
    }

    public static LayerDefinition tangLayer() {
        // GENERATED tang: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(137, 0).mirror(false).addBox(-1.45f, -0.5f, -1.45f, 2.9f, 6.1f, 2.9f), PartPose.offsetAndRotation(-1.8f, 11.6087f, 0.2f, -0.25f, 0, 0.04f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(137, 0).mirror(true).addBox(-1.45f, -0.5f, -1.45f, 2.9f, 6.1f, 2.9f), PartPose.offsetAndRotation(1.8f, 11.6087f, 0.2f, -0.25f, 0, -0.04f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(199, 0).mirror(false).addBox(-1.25f, 0, -1.25f, 2.5f, 5.6f, 2.5f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.42f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(199, 0).mirror(true).addBox(-1.25f, 0, -1.25f, 2.5f, 5.6f, 2.5f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.42f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(61, 35).mirror(false).addBox(-1.4f, 0, -2.85f, 2.8f, 1.4f, 4.4f), PartPose.offsetAndRotation(0, 5.6f, 0, -0.17f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(61, 35).mirror(true).addBox(-1.4f, 0, -2.85f, 2.8f, 1.4f, 4.4f), PartPose.offsetAndRotation(0, 5.6f, 0, -0.17f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(212, 0).mirror(false).addBox(-3, -3, -2.1f, 6, 3.6f, 4.2f), PartPose.offsetAndRotation(0, 11.6087f, 0.3f, 0.04f, 0, 0));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(49, 0).mirror(false).addBox(-3.6f, -8.4f, -2.3f, 7.2f, 8.4f, 4.6f), PartPose.offsetAndRotation(0, -2.6f, 0, 0.02f, 0, 0));
        PartDefinition rightArm = chest.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(124, 0).mirror(false).addBox(-1.3f, -1.17f, -1.3f, 2.6f, 7.77f, 2.6f), PartPose.offsetAndRotation(-4.51f, -6.97f, 0, -0.1f, 0, 0.08f));
        PartDefinition leftArm = chest.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(124, 0).mirror(true).addBox(-1.3f, -1.17f, -1.3f, 2.6f, 7.77f, 2.6f), PartPose.offsetAndRotation(4.51f, -6.97f, 0, -0.1f, 0, -0.08f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(150, 0).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 6.4f, 2.4f), PartPose.offsetAndRotation(0, 6.6f, 0, -0.4f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(150, 0).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 6.4f, 2.4f), PartPose.offsetAndRotation(0, 6.6f, 0, -0.4f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(129, 35).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 2.6f, 2.4f), PartPose.offsetAndRotation(0, 6.4f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(129, 35).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 2.6f, 2.4f), PartPose.offsetAndRotation(0, 6.4f, 0, 0, 0, 0));
        PartDefinition rightFingers = rightHand.addOrReplaceChild("right_fingers", CubeListBuilder.create()
                .texOffs(216, 35).mirror(false).addBox(-1.15f, 0, -1.35f, 0.5714f, 1.362f, 1.32f)
                .texOffs(216, 35).mirror(false).addBox(-0.575f, 0, -1.35f, 0.5714f, 1.612f, 1.32f)
                .texOffs(216, 35).mirror(false).addBox(0, 0, -1.35f, 0.5714f, 1.612f, 1.32f)
                .texOffs(216, 35).mirror(false).addBox(0.575f, 0, -1.35f, 0.5714f, 1.362f, 1.32f), PartPose.offsetAndRotation(0, 2.4f, 0, 0, 0, 0));
        PartDefinition leftFingers = leftHand.addOrReplaceChild("left_fingers", CubeListBuilder.create()
                .texOffs(216, 35).mirror(true).addBox(0.5786f, 0, -1.35f, 0.5714f, 1.362f, 1.32f)
                .texOffs(216, 35).mirror(true).addBox(0.0036f, 0, -1.35f, 0.5714f, 1.612f, 1.32f)
                .texOffs(216, 35).mirror(true).addBox(-0.5714f, 0, -1.35f, 0.5714f, 1.612f, 1.32f)
                .texOffs(216, 35).mirror(true).addBox(-1.1464f, 0, -1.35f, 0.5714f, 1.362f, 1.32f), PartPose.offsetAndRotation(0, 2.4f, 0, 0, 0, 0));
        PartDefinition rightThumb = rightHand.addOrReplaceChild("right_thumb", CubeListBuilder.create()
                .texOffs(25, 45).mirror(false).addBox(-0.2f, 0, -0.5f, 0.6286f, 1.43f, 0.6286f), PartPose.offsetAndRotation(1, 0.91f, -1, 0.25f, 0, -0.35f));
        PartDefinition leftThumb = leftHand.addOrReplaceChild("left_thumb", CubeListBuilder.create()
                .texOffs(25, 45).mirror(true).addBox(-0.4286f, 0, -0.5f, 0.6286f, 1.43f, 0.6286f), PartPose.offsetAndRotation(-1, 0.91f, -1, 0.25f, 0, 0.35f));
        PartDefinition rightToes = rightFoot.addOrReplaceChild("right_toes", CubeListBuilder.create()
                .texOffs(223, 35).mirror(false).addBox(-1.3f, 0.35f, -1.3f, 0.6087f, 1.05f, 1.5f)
                .texOffs(223, 35).mirror(false).addBox(-0.65f, 0.35f, -1.6f, 0.6087f, 1.05f, 1.8f)
                .texOffs(223, 35).mirror(false).addBox(0, 0.35f, -1.6f, 0.6087f, 1.05f, 1.8f)
                .texOffs(223, 35).mirror(false).addBox(0.65f, 0.35f, -1.3f, 0.6087f, 1.05f, 1.5f), PartPose.offsetAndRotation(0, 0, -2.85f, 0, 0, 0));
        PartDefinition leftToes = leftFoot.addOrReplaceChild("left_toes", CubeListBuilder.create()
                .texOffs(223, 35).mirror(true).addBox(0.6913f, 0.35f, -1.3f, 0.6087f, 1.05f, 1.5f)
                .texOffs(223, 35).mirror(true).addBox(0.0413f, 0.35f, -1.6f, 0.6087f, 1.05f, 1.8f)
                .texOffs(223, 35).mirror(true).addBox(-0.6087f, 0.35f, -1.6f, 0.6087f, 1.05f, 1.8f)
                .texOffs(223, 35).mirror(true).addBox(-1.2587f, 0.35f, -1.3f, 0.6087f, 1.05f, 1.5f), PartPose.offsetAndRotation(0, 0, -2.85f, 0, 0, 0));
        PartDefinition swaySkirtFront = body.addOrReplaceChild("sway_skirt_front", CubeListBuilder.create()
                .texOffs(105, 0).mirror(false).addBox(-3.8f, -0.5f, -0.4f, 7.6f, 11.2f, 0.8f), PartPose.offsetAndRotation(0, 0, -2.7f, -0.06f, 0, 0));
        PartDefinition swaySkirtBack = body.addOrReplaceChild("sway_skirt_back", CubeListBuilder.create()
                .texOffs(105, 0).mirror(false).addBox(-3.8f, -0.5f, -0.4f, 7.6f, 11.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 2.7f, 0.08f, 0, 0));
        PartDefinition rightSkirtSide = body.addOrReplaceChild("right_skirt_side", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-0.4f, -0.5f, -2.7f, 0.8f, 10.8f, 5.4f), PartPose.offsetAndRotation(-3.8f, 0, 0, 0, 0, 0.1f));
        PartDefinition leftSkirtSide = body.addOrReplaceChild("left_skirt_side", CubeListBuilder.create()
                .texOffs(5, 0).mirror(true).addBox(-0.4f, -0.5f, -2.7f, 0.8f, 10.8f, 5.4f), PartPose.offsetAndRotation(3.8f, 0, 0, 0, 0, -0.1f));
        PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(76, 0).mirror(false).addBox(-3.3f, -6.2f, -3.2f, 6.6f, 6.2f, 6.4f)
                .texOffs(142, 35).mirror(false).addBox(-2.772f, -5.332f, -3.75f, 5.544f, 4.588f, 0.8f)
                .texOffs(30, 45).mirror(false).addBox(-1.5f, -2.604f, -4.6f, 3, 1.6f, 1)
                .texOffs(39, 45).mirror(false).addBox(-1.56f, -1.754f, -4.75f, 3.12f, 0.8f, 1.1f)
                .texOffs(70, 45).mirror(false).addBox(-0.9f, -3.004f, -4.85f, 1.8f, 0.8f, 0.55f)
                .texOffs(163, 0).mirror(false).addBox(-3.399f, -6.448f, -1.12f, 6.798f, 4.092f, 4.48f)
                .texOffs(78, 35).mirror(false).addBox(-3.135f, -1.24f, -1.92f, 6.27f, 1.736f, 4.8f), PartPose.offsetAndRotation(0, -8.4f, -0.6f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(230, 35).mirror(false).addBox(-3.135f, -0.6f, -0.65f, 3.234f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -5.208f, -3.95f, 0, 0.18f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(230, 35).mirror(true).addBox(-0.099f, -0.6f, -0.65f, 3.234f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -5.208f, -3.95f, 0, -0.18f, -0.2f));
        PartDefinition rightCheekpad = head.addOrReplaceChild("right_cheekpad", CubeListBuilder.create()
                .texOffs(179, 35).mirror(false).addBox(-1.3f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(-1.65f, -2.356f, -3.65f, 0, 0.3f, 0));
        PartDefinition leftCheekpad = head.addOrReplaceChild("left_cheekpad", CubeListBuilder.create()
                .texOffs(179, 35).mirror(true).addBox(-1.1f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(1.65f, -2.356f, -3.65f, 0, -0.3f, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(243, 35).mirror(false).addBox(-1.29f, 0, -1.104f, 2.58f, 1.4f, 1.304f)
                .texOffs(52, 45).mirror(false).addBox(-1.2f, 1.2f, -0.48f, 2.4f, 1, 1.08f), PartPose.offsetAndRotation(0, -1.204f, -3.4f, 0.06f, 0, 0));
        PartDefinition rightCheek = head.addOrReplaceChild("right_cheek", CubeListBuilder.create()
                .texOffs(235, 0).mirror(false).addBox(-1.8f, -1.8f, -1.4f, 2.2f, 4.2f, 3.6f), PartPose.offsetAndRotation(-2.97f, -2.604f, -1.28f, 0.1f, 0.55f, 0.3f));
        PartDefinition leftCheek = head.addOrReplaceChild("left_cheek", CubeListBuilder.create()
                .texOffs(235, 0).mirror(true).addBox(-0.4f, -1.8f, -1.4f, 2.2f, 4.2f, 3.6f), PartPose.offsetAndRotation(2.97f, -2.604f, -1.28f, 0.1f, -0.55f, -0.3f));
        PartDefinition rightCheekLow = head.addOrReplaceChild("right_cheek_low", CubeListBuilder.create()
                .texOffs(103, 35).mirror(false).addBox(-1.5f, -0.6f, -1.2f, 1.9f, 3.2f, 3), PartPose.offsetAndRotation(-2.475f, -0.744f, -0.8f, 0.35f, 0.35f, 0.55f));
        PartDefinition leftCheekLow = head.addOrReplaceChild("left_cheek_low", CubeListBuilder.create()
                .texOffs(103, 35).mirror(true).addBox(-0.4f, -0.6f, -1.2f, 1.9f, 3.2f, 3), PartPose.offsetAndRotation(2.475f, -0.744f, -0.8f, 0.35f, -0.35f, -0.55f));
        PartDefinition crownlock0 = head.addOrReplaceChild("crownlock_0", CubeListBuilder.create()
                .texOffs(190, 35).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(0, -6.2f, -1.6f, -0.75f, 0, 0));
        PartDefinition crownlock1 = head.addOrReplaceChild("crownlock_1", CubeListBuilder.create()
                .texOffs(190, 35).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(-1.65f, -6.2f, -0.32f, -0.75f, 0.3f, 0.18f));
        PartDefinition crownlock2 = head.addOrReplaceChild("crownlock_2", CubeListBuilder.create()
                .texOffs(190, 35).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(1.65f, -6.2f, -0.32f, -0.75f, -0.3f, -0.18f));
        PartDefinition crownlock3 = head.addOrReplaceChild("crownlock_3", CubeListBuilder.create()
                .texOffs(190, 35).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(0, -6.2f, 1.12f, -0.75f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(157, 35).mirror(false).addBox(-0.9f, -1.32f, -1.1f, 1, 2.64f, 2.31f), PartPose.offsetAndRotation(-3.4f, -3.72f, -0.48f, 0, -0.35f, 0.18f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(157, 35).mirror(true).addBox(-0.1f, -1.32f, -1.1f, 1, 2.64f, 2.31f), PartPose.offsetAndRotation(3.4f, -3.72f, -0.48f, 0, 0.35f, -0.18f));
        PartDefinition kasaya = chest.addOrReplaceChild("kasaya", CubeListBuilder.create()
                .texOffs(20, 0).mirror(false).addBox(-3.9f, -8.2f, -2.6f, 7.8f, 9, 5.2f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition swayKasayaHang = chest.addOrReplaceChild("sway_kasaya_hang", CubeListBuilder.create()
                .texOffs(0, 35).mirror(false).addBox(-3, 0, -0.4f, 6, 8, 0.8f), PartPose.offsetAndRotation(1.2f, -0.6f, -2.6f, -0.04f, 0, 0.04f));
        PartDefinition kasayaRing = chest.addOrReplaceChild("kasaya_ring", CubeListBuilder.create()
                .texOffs(63, 45).mirror(false).addBox(-0.8f, -0.8f, -0.4f, 1.6f, 1.6f, 0.5f), PartPose.offsetAndRotation(1.6f, -6, -2.9f, 0, 0, 0));
        PartDefinition rightCuff = rightForearm.addOrReplaceChild("right_cuff", CubeListBuilder.create()
                .texOffs(44, 35).mirror(false).addBox(-1.7f, 0, -1.7f, 3.4f, 3.2f, 3.4f), PartPose.offsetAndRotation(0, 3.4f, 0, 0, 0, 0));
        PartDefinition leftCuff = leftForearm.addOrReplaceChild("left_cuff", CubeListBuilder.create()
                .texOffs(44, 35).mirror(true).addBox(-1.7f, 0, -1.7f, 3.4f, 3.2f, 3.4f), PartPose.offsetAndRotation(0, 3.4f, 0, 0, 0, 0));
        PartDefinition crownBand = head.addOrReplaceChild("crown_band", CubeListBuilder.create()
                .texOffs(15, 35).mirror(false).addBox(-3.25f, -0.6f, -3.25f, 6.5f, 1.2f, 6.5f), PartPose.offsetAndRotation(0, -5, 0, 0, 0, 0));
        PartDefinition beads = chest.addOrReplaceChild("beads", CubeListBuilder.create()
                .texOffs(114, 35).mirror(false).addBox(-2.6f, 0, -0.4f, 5.2f, 5.2f, 0.6f), PartPose.offsetAndRotation(0, -7.8f, -2.6f, 0.05f, 0, 0));
        PartDefinition crownLeaf0 = crownBand.addOrReplaceChild("crown_leaf_0", CubeListBuilder.create()
                .texOffs(0, 45).mirror(false).addBox(-1.15f, -2.8f, -0.2f, 2.3f, 2.8f, 0.4f), PartPose.offsetAndRotation(-2.861f, -0.4f, -1.837f, 0.12f, 1, 0));
        PartDefinition crownLeaf1 = crownBand.addOrReplaceChild("crown_leaf_1", CubeListBuilder.create()
                .texOffs(0, 45).mirror(false).addBox(-1.15f, -2.8f, -0.2f, 2.3f, 2.8f, 0.4f), PartPose.offsetAndRotation(-1.63f, -0.4f, -2.9838f, 0.12f, 0.5f, 0));
        PartDefinition crownLeaf2 = crownBand.addOrReplaceChild("crown_leaf_2", CubeListBuilder.create()
                .texOffs(0, 45).mirror(false).addBox(-1.15f, -2.8f, -0.2f, 2.3f, 2.8f, 0.4f), PartPose.offsetAndRotation(0, -0.4f, -3.4f, 0.12f, -0, 0));
        PartDefinition crownLeaf3 = crownBand.addOrReplaceChild("crown_leaf_3", CubeListBuilder.create()
                .texOffs(0, 45).mirror(false).addBox(-1.15f, -2.8f, -0.2f, 2.3f, 2.8f, 0.4f), PartPose.offsetAndRotation(1.63f, -0.4f, -2.9838f, 0.12f, -0.5f, 0));
        PartDefinition crownLeaf4 = crownBand.addOrReplaceChild("crown_leaf_4", CubeListBuilder.create()
                .texOffs(0, 45).mirror(false).addBox(-1.15f, -2.8f, -0.2f, 2.3f, 2.8f, 0.4f), PartPose.offsetAndRotation(2.861f, -0.4f, -1.837f, 0.12f, -1, 0));
        PartDefinition tail0 = body.addOrReplaceChild("tail_0", CubeListBuilder.create()
                .texOffs(166, 35).mirror(false).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 3.2f), PartPose.offsetAndRotation(0, -0.4f, 2.4f, 1.25f, 0, 0));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(166, 35).mirror(false).addBox(-0.5333f, -0.5333f, 0, 1.0667f, 1.0667f, 3.2f), PartPose.offsetAndRotation(0, 0, 2.9f, 0.2f, 0, 0));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(205, 35).mirror(false).addBox(-0.4667f, -0.4667f, 0, 0.9333f, 0.9333f, 3.2f), PartPose.offsetAndRotation(0, 0, 2.9f, 0.2f, 0, 0));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create()
                .texOffs(205, 35).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 3.2f), PartPose.offsetAndRotation(0, 0, 2.9f, 0.2f, 0, 0));
        PartDefinition staff = rightHand.addOrReplaceChild("staff", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.4f, -24, -0.4f, 0.8f, 33, 0.8f)
                .texOffs(9, 45).mirror(false).addBox(-0.6f, -24.6f, -0.6f, 1.2f, 1.2f, 1.2f)
                .texOffs(188, 0).mirror(false).addBox(-0.25f, -30, -2, 0.5f, 5.6f, 4)
                .texOffs(9, 45).mirror(false).addBox(-0.6f, 8.6f, -0.6f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 1.3f, 0, 0.4887f, 0.0383f, -0.0702f));
        PartDefinition swayStaffring0 = staff.addOrReplaceChild("sway_staffring_0", CubeListBuilder.create()
                .texOffs(18, 45).mirror(false).addBox(-0.3f, 0, -0.55f, 0.6f, 1.4f, 1.1f), PartPose.offsetAndRotation(0, -26.2f, -2.4f, 0, 0, 0));
        PartDefinition swayStaffring1 = staff.addOrReplaceChild("sway_staffring_1", CubeListBuilder.create()
                .texOffs(18, 45).mirror(false).addBox(-0.3f, 0, -0.55f, 0.6f, 1.4f, 1.1f), PartPose.offsetAndRotation(0, -26.2f, 2.4f, 0, 0, 0));
        PartDefinition swayStaffring2 = staff.addOrReplaceChild("sway_staffring_2", CubeListBuilder.create()
                .texOffs(18, 45).mirror(false).addBox(-0.3f, 0, -0.55f, 0.6f, 1.4f, 1.1f), PartPose.offsetAndRotation(0, -28.2f, -2.3f, 0, 0, 0));
        PartDefinition swayStaffring3 = staff.addOrReplaceChild("sway_staffring_3", CubeListBuilder.create()
                .texOffs(18, 45).mirror(false).addBox(-0.3f, 0, -0.55f, 0.6f, 1.4f, 1.1f), PartPose.offsetAndRotation(0, -28.2f, 2.3f, 0, 0, 0));
        PartDefinition swayStaffring4 = staff.addOrReplaceChild("sway_staffring_4", CubeListBuilder.create()
                .texOffs(18, 45).mirror(false).addBox(-0.3f, 0, -0.55f, 0.6f, 1.4f, 1.1f), PartPose.offsetAndRotation(0, -30, -1.7f, 0, 0, 0));
        PartDefinition swayStaffring5 = staff.addOrReplaceChild("sway_staffring_5", CubeListBuilder.create()
                .texOffs(18, 45).mirror(false).addBox(-0.3f, 0, -0.55f, 0.6f, 1.4f, 1.1f), PartPose.offsetAndRotation(0, -30, 1.7f, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED tang: end
    }

    public static LayerDefinition bajieLayer() {
        // GENERATED bajie: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(205, 0).mirror(false).addBox(-1.8f, -0.5f, -1.8f, 3.6f, 5.3f, 3.6f), PartPose.offsetAndRotation(-2.4f, 13.553f, 0.2f, -0.25f, 0, 0.04f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(205, 0).mirror(true).addBox(-1.8f, -0.5f, -1.8f, 3.6f, 5.3f, 3.6f), PartPose.offsetAndRotation(2.4f, 13.553f, 0.2f, -0.25f, 0, -0.04f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(37, 34).mirror(false).addBox(-1.6f, 0, -1.6f, 3.2f, 4.4f, 3.2f), PartPose.offsetAndRotation(0, 4.8f, 0, 0.42f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(37, 34).mirror(true).addBox(-1.6f, 0, -1.6f, 3.2f, 4.4f, 3.2f), PartPose.offsetAndRotation(0, 4.8f, 0, 0.42f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(117, 34).mirror(false).addBox(-1.7f, 0, -2.9f, 3.4f, 1.4f, 4.8f), PartPose.offsetAndRotation(0, 4.4f, 0, -0.17f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(117, 34).mirror(true).addBox(-1.7f, 0, -2.9f, 3.4f, 1.4f, 4.8f), PartPose.offsetAndRotation(0, 4.4f, 0, -0.17f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(107, 0).mirror(false).addBox(-4.2f, -4, -3.2f, 8.4f, 4.6f, 6.4f), PartPose.offsetAndRotation(0, 13.553f, 0.3f, 0, 0, 0));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(24, 0).mirror(false).addBox(-4.8f, -8, -3.5f, 9.6f, 8, 7), PartPose.offsetAndRotation(0, -3.6f, 0, 0, 0, 0));
        PartDefinition rightArm = chest.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(192, 0).mirror(false).addBox(-1.5f, -1.35f, -1.5f, 3, 7.35f, 3), PartPose.offsetAndRotation(-5.85f, -6.35f, 0, -0.15f, 0, 0.2f));
        PartDefinition leftArm = chest.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(192, 0).mirror(true).addBox(-1.5f, -1.35f, -1.5f, 3, 7.35f, 3), PartPose.offsetAndRotation(5.85f, -6.35f, 0, -0.15f, 0, -0.2f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(54, 34).mirror(false).addBox(-1.4f, 0, -1.4f, 2.8f, 5.8f, 2.8f), PartPose.offsetAndRotation(0, 6, 0, -0.4f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(54, 34).mirror(true).addBox(-1.4f, 0, -1.4f, 2.8f, 5.8f, 2.8f), PartPose.offsetAndRotation(0, 6, 0, -0.4f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(199, 34).mirror(false).addBox(-1.4f, 0, -1.4f, 2.8f, 2.8f, 2.8f), PartPose.offsetAndRotation(0, 5.8f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(199, 34).mirror(true).addBox(-1.4f, 0, -1.4f, 2.8f, 2.8f, 2.8f), PartPose.offsetAndRotation(0, 5.8f, 0, 0, 0, 0));
        PartDefinition rightFingers = rightHand.addOrReplaceChild("right_fingers", CubeListBuilder.create()
                .texOffs(0, 45).mirror(false).addBox(-1.35f, 0, -1.55f, 0.6667f, 1.486f, 1.54f)
                .texOffs(0, 45).mirror(false).addBox(-0.675f, 0, -1.55f, 0.6667f, 1.736f, 1.54f)
                .texOffs(0, 45).mirror(false).addBox(0, 0, -1.55f, 0.6667f, 1.736f, 1.54f)
                .texOffs(0, 45).mirror(false).addBox(0.675f, 0, -1.55f, 0.6667f, 1.486f, 1.54f), PartPose.offsetAndRotation(0, 2.6f, 0, 0, 0, 0));
        PartDefinition leftFingers = leftHand.addOrReplaceChild("left_fingers", CubeListBuilder.create()
                .texOffs(0, 45).mirror(true).addBox(0.6833f, 0, -1.55f, 0.6667f, 1.486f, 1.54f)
                .texOffs(0, 45).mirror(true).addBox(0.0083f, 0, -1.55f, 0.6667f, 1.736f, 1.54f)
                .texOffs(0, 45).mirror(true).addBox(-0.6667f, 0, -1.55f, 0.6667f, 1.736f, 1.54f)
                .texOffs(0, 45).mirror(true).addBox(-1.3417f, 0, -1.55f, 0.6667f, 1.486f, 1.54f), PartPose.offsetAndRotation(0, 2.6f, 0, 0, 0, 0));
        PartDefinition rightThumb = rightHand.addOrReplaceChild("right_thumb", CubeListBuilder.create()
                .texOffs(28, 45).mirror(false).addBox(-0.2f, 0, -0.5f, 0.7333f, 1.54f, 0.7333f), PartPose.offsetAndRotation(1.2f, 0.98f, -1.2f, 0.25f, 0, -0.35f));
        PartDefinition leftThumb = leftHand.addOrReplaceChild("left_thumb", CubeListBuilder.create()
                .texOffs(28, 45).mirror(true).addBox(-0.5333f, 0, -0.5f, 0.7333f, 1.54f, 0.7333f), PartPose.offsetAndRotation(-1.2f, 0.98f, -1.2f, 0.25f, 0, 0.35f));
        PartDefinition rightToes = rightFoot.addOrReplaceChild("right_toes", CubeListBuilder.create()
                .texOffs(7, 45).mirror(false).addBox(-1.6f, 0.35f, -1.3f, 0.7391f, 1.05f, 1.5f)
                .texOffs(7, 45).mirror(false).addBox(-0.8f, 0.35f, -1.6f, 0.7391f, 1.05f, 1.8f)
                .texOffs(7, 45).mirror(false).addBox(0, 0.35f, -1.6f, 0.7391f, 1.05f, 1.8f)
                .texOffs(7, 45).mirror(false).addBox(0.8f, 0.35f, -1.3f, 0.7391f, 1.05f, 1.5f), PartPose.offsetAndRotation(0, 0, -2.9f, 0, 0, 0));
        PartDefinition leftToes = leftFoot.addOrReplaceChild("left_toes", CubeListBuilder.create()
                .texOffs(7, 45).mirror(true).addBox(0.8609f, 0.35f, -1.3f, 0.7391f, 1.05f, 1.5f)
                .texOffs(7, 45).mirror(true).addBox(0.0609f, 0.35f, -1.6f, 0.7391f, 1.05f, 1.8f)
                .texOffs(7, 45).mirror(true).addBox(-0.7391f, 0.35f, -1.6f, 0.7391f, 1.05f, 1.8f)
                .texOffs(7, 45).mirror(true).addBox(-1.5391f, 0.35f, -1.3f, 0.7391f, 1.05f, 1.5f), PartPose.offsetAndRotation(0, 0, -2.9f, 0, 0, 0));
        PartDefinition swaySkirtFront = body.addOrReplaceChild("sway_skirt_front", CubeListBuilder.create()
                .texOffs(222, 0).mirror(false).addBox(-4.6f, -0.5f, -0.4f, 9.2f, 8.6f, 0.8f), PartPose.offsetAndRotation(0, 0, -3.7f, -0.06f, 0, 0));
        PartDefinition swaySkirtBack = body.addOrReplaceChild("sway_skirt_back", CubeListBuilder.create()
                .texOffs(222, 0).mirror(false).addBox(-4.6f, -0.5f, -0.4f, 9.2f, 8.6f, 0.8f), PartPose.offsetAndRotation(0, 0, 3.7f, 0.08f, 0, 0));
        PartDefinition rightSkirtSide = body.addOrReplaceChild("right_skirt_side", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-0.4f, -0.5f, -3.7f, 0.8f, 8.2f, 7.4f), PartPose.offsetAndRotation(-4.6f, 0, 0, 0, 0, 0.1f));
        PartDefinition leftSkirtSide = body.addOrReplaceChild("left_skirt_side", CubeListBuilder.create()
                .texOffs(5, 0).mirror(true).addBox(-0.4f, -0.5f, -3.7f, 0.8f, 8.2f, 7.4f), PartPose.offsetAndRotation(4.6f, 0, 0, 0, 0, -0.1f));
        PartDefinition belly = chest.addOrReplaceChild("belly", CubeListBuilder.create()
                .texOffs(140, 0).mirror(false).addBox(-4.4f, -4.6f, -2.4f, 8.8f, 7.4f, 3.2f)
                .texOffs(136, 34).mirror(false).addBox(-3.4f, -3.6f, -3.2f, 6.8f, 5.4f, 1), PartPose.offsetAndRotation(0, -2.2f, -3.4f, 0, 0, 0));
        PartDefinition rightLapel = chest.addOrReplaceChild("right_lapel", CubeListBuilder.create()
                .texOffs(245, 0).mirror(false).addBox(-1.2f, 0, -0.5f, 2.6f, 9, 1), PartPose.offsetAndRotation(-3.6f, -8, -3.6f, 0, 0, 0.18f));
        PartDefinition leftLapel = chest.addOrReplaceChild("left_lapel", CubeListBuilder.create()
                .texOffs(245, 0).mirror(true).addBox(-1.4f, 0, -0.5f, 2.6f, 9, 1), PartPose.offsetAndRotation(3.6f, -8, -3.6f, 0, 0, -0.18f));
        PartDefinition jBelt = body.addOrReplaceChild("j_belt", CubeListBuilder.create()
                .texOffs(0, 34).mirror(false).addBox(-4.6f, -0.6f, -3.8f, 9.2f, 1.6f, 7.6f), PartPose.offsetAndRotation(0, -3.4f, 0, 0, 0, 0));
        PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(59, 0).mirror(false).addBox(-3.8f, -6, -3.6f, 7.6f, 6.2f, 7.2f)
                .texOffs(67, 34).mirror(false).addBox(-2.6f, -3.6f, -8.4f, 5.2f, 3.6f, 5)
                .texOffs(225, 34).mirror(false).addBox(-2.8f, -3.8f, -8.8f, 5.6f, 3.8f, 0.6f)
                .texOffs(90, 34).mirror(false).addBox(-4.2f, -3.2f, -3.8f, 8.4f, 3.6f, 4)
                .texOffs(38, 45).mirror(false).addBox(-3.4f, -4.9f, -3.9f, 6.8f, 1, 0.6f)
                .texOffs(153, 34).mirror(false).addBox(-3.6f, -6.4f, -1.6f, 7.2f, 1.2f, 4.6f), PartPose.offsetAndRotation(0, -8, -1.2f, 0, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(180, 34).mirror(false).addBox(-2, 0, -4.2f, 4, 1.4f, 4.6f), PartPose.offsetAndRotation(0, -0.3f, -3.2f, 0.05f, 0, 0));
        PartDefinition rightTusk = jaw.addOrReplaceChild("right_tusk", CubeListBuilder.create()
                .texOffs(33, 45).mirror(false).addBox(-0.35f, -1.8f, -0.35f, 0.7f, 1.9f, 0.7f), PartPose.offsetAndRotation(-1.6f, 0.1f, -3.8f, -0.3f, 0, 0.1f));
        PartDefinition leftTusk = jaw.addOrReplaceChild("left_tusk", CubeListBuilder.create()
                .texOffs(33, 45).mirror(true).addBox(-0.35f, -1.8f, -0.35f, 0.7f, 1.9f, 0.7f), PartPose.offsetAndRotation(1.6f, 0.1f, -3.8f, -0.3f, 0, -0.1f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(92, 0).mirror(false).addBox(-0.4f, 0, -2.6f, 0.8f, 6.8f, 5.2f), PartPose.offsetAndRotation(-3.7f, -5.8f, -0.8f, -0.95f, 0.35f, 0.55f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(92, 0).mirror(true).addBox(-0.4f, 0, -2.6f, 0.8f, 6.8f, 5.2f), PartPose.offsetAndRotation(3.7f, -5.8f, -0.8f, -0.95f, -0.35f, -0.55f));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(55, 45).mirror(false).addBox(-1.6f, -0.5f, -0.4f, 3, 0.9f, 0.8f), PartPose.offsetAndRotation(-1.8f, -4.9f, -3.75f, 0, 0, 0.25f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(55, 45).mirror(true).addBox(-1.4f, -0.5f, -0.4f, 3, 0.9f, 0.8f), PartPose.offsetAndRotation(1.8f, -4.9f, -3.75f, 0, 0, -0.25f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(64, 45).mirror(false).addBox(-0.8f, -0.4f, -0.2f, 1.6f, 0.9f, 0.3f), PartPose.offsetAndRotation(-1.8f, -4.3f, -3.65f, 0, 0, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(64, 45).mirror(true).addBox(-0.8f, -0.4f, -0.2f, 1.6f, 0.9f, 0.3f), PartPose.offsetAndRotation(1.8f, -4.3f, -3.65f, 0, 0, 0));
        PartDefinition tail0 = body.addOrReplaceChild("tail_0", CubeListBuilder.create()
                .texOffs(212, 34).mirror(false).addBox(-0.65f, -0.65f, 0, 1.3f, 1.3f, 3.3f), PartPose.offsetAndRotation(0, -0.6f, 3.4f, 1.2f, 0, 0));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(212, 34).mirror(false).addBox(-0.5667f, -0.5667f, 0, 1.1333f, 1.1333f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.25f, 0, 0));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(240, 34).mirror(false).addBox(-0.4833f, -0.4833f, 0, 0.9667f, 0.9667f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.25f, 0, 0));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create()
                .texOffs(240, 34).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.25f, 0, 0));
        PartDefinition rake = rightHand.addOrReplaceChild("rake", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.45f, -24, -0.45f, 0.9f, 32, 0.9f)
                .texOffs(14, 45).mirror(false).addBox(-0.7f, -24.8f, -0.7f, 1.4f, 1.4f, 1.4f)
                .texOffs(167, 0).mirror(false).addBox(-0.8f, -26, -4.6f, 1.6f, 1.8f, 9.2f), PartPose.offsetAndRotation(0, 1.4f, 0, 0.6411f, 0.104f, -0.1711f));
        PartDefinition rakeTooth0 = rake.addOrReplaceChild("rake_tooth_0", CubeListBuilder.create()
                .texOffs(23, 45).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.6f, 0.5f), PartPose.offsetAndRotation(0, -24.4f, -4.2f, 0, 0, 0));
        PartDefinition rakeTooth1 = rake.addOrReplaceChild("rake_tooth_1", CubeListBuilder.create()
                .texOffs(23, 45).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.6f, 0.5f), PartPose.offsetAndRotation(0, -24.4f, -3.15f, 0, 0, 0));
        PartDefinition rakeTooth2 = rake.addOrReplaceChild("rake_tooth_2", CubeListBuilder.create()
                .texOffs(23, 45).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.6f, 0.5f), PartPose.offsetAndRotation(0, -24.4f, -2.1f, 0, 0, 0));
        PartDefinition rakeTooth3 = rake.addOrReplaceChild("rake_tooth_3", CubeListBuilder.create()
                .texOffs(23, 45).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.6f, 0.5f), PartPose.offsetAndRotation(0, -24.4f, -1.05f, 0, 0, 0));
        PartDefinition rakeTooth4 = rake.addOrReplaceChild("rake_tooth_4", CubeListBuilder.create()
                .texOffs(23, 45).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.6f, 0.5f), PartPose.offsetAndRotation(0, -24.4f, 0, 0, 0, 0));
        PartDefinition rakeTooth5 = rake.addOrReplaceChild("rake_tooth_5", CubeListBuilder.create()
                .texOffs(23, 45).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.6f, 0.5f), PartPose.offsetAndRotation(0, -24.4f, 1.05f, 0, 0, 0));
        PartDefinition rakeTooth6 = rake.addOrReplaceChild("rake_tooth_6", CubeListBuilder.create()
                .texOffs(23, 45).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.6f, 0.5f), PartPose.offsetAndRotation(0, -24.4f, 2.1f, 0, 0, 0));
        PartDefinition rakeTooth7 = rake.addOrReplaceChild("rake_tooth_7", CubeListBuilder.create()
                .texOffs(23, 45).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.6f, 0.5f), PartPose.offsetAndRotation(0, -24.4f, 3.15f, 0, 0, 0));
        PartDefinition rakeTooth8 = rake.addOrReplaceChild("rake_tooth_8", CubeListBuilder.create()
                .texOffs(23, 45).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.6f, 0.5f), PartPose.offsetAndRotation(0, -24.4f, 4.2f, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED bajie: end
    }

    public static LayerDefinition shaLayer() {
        // GENERATED sha: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(154, 0).mirror(false).addBox(-1.5f, -0.5f, -1.5f, 3, 6.7f, 3), PartPose.offsetAndRotation(-1.9f, 10.4329f, 0.2f, -0.25f, 0, 0.04f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(154, 0).mirror(true).addBox(-1.5f, -0.5f, -1.5f, 3, 6.7f, 3), PartPose.offsetAndRotation(1.9f, 10.4329f, 0.2f, -0.25f, 0, -0.04f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(154, 0).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 6.2f, 2.6f), PartPose.offsetAndRotation(0, 6.2f, 0, 0.42f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(154, 0).mirror(true).addBox(-1.3f, 0, -1.3f, 2.6f, 6.2f, 2.6f), PartPose.offsetAndRotation(0, 6.2f, 0, 0.42f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(45, 38).mirror(false).addBox(-1.5f, 0, -3.2f, 3, 1.4f, 4.8f), PartPose.offsetAndRotation(0, 6.2f, 0, -0.17f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(45, 38).mirror(true).addBox(-1.5f, 0, -3.2f, 3, 1.4f, 4.8f), PartPose.offsetAndRotation(0, 6.2f, 0, -0.17f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(207, 0).mirror(false).addBox(-3.2f, -3, -2.2f, 6.4f, 3.6f, 4.4f), PartPose.offsetAndRotation(0, 10.4329f, 0.3f, 0.06f, 0, 0));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(20, 0).mirror(false).addBox(-3.9f, -9.2f, -2.4f, 7.8f, 9.2f, 4.8f), PartPose.offsetAndRotation(0, -2.6f, 0, 0.06f, 0, 0));
        PartDefinition rightArm = chest.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(76, 0).mirror(false).addBox(-1.3f, -1.17f, -1.3f, 2.6f, 8.37f, 2.6f), PartPose.offsetAndRotation(-4.81f, -7.77f, 0, -0.1f, 0, 0.1f));
        PartDefinition leftArm = chest.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(76, 0).mirror(true).addBox(-1.3f, -1.17f, -1.3f, 2.6f, 8.37f, 2.6f), PartPose.offsetAndRotation(4.81f, -7.77f, 0, -0.1f, 0, -0.1f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(167, 0).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 7, 2.4f), PartPose.offsetAndRotation(0, 7.2f, 0, -0.35f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(167, 0).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 7, 2.4f), PartPose.offsetAndRotation(0, 7.2f, 0, -0.35f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(115, 38).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 2.8f, 2.6f), PartPose.offsetAndRotation(0, 7, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(115, 38).mirror(true).addBox(-1.3f, 0, -1.3f, 2.6f, 2.8f, 2.6f), PartPose.offsetAndRotation(0, 7, 0, 0, 0, 0));
        PartDefinition rightFingers = rightHand.addOrReplaceChild("right_fingers", CubeListBuilder.create()
                .texOffs(215, 38).mirror(false).addBox(-1.25f, 0, -1.45f, 0.619f, 1.486f, 1.43f)
                .texOffs(215, 38).mirror(false).addBox(-0.625f, 0, -1.45f, 0.619f, 1.736f, 1.43f)
                .texOffs(215, 38).mirror(false).addBox(0, 0, -1.45f, 0.619f, 1.736f, 1.43f)
                .texOffs(215, 38).mirror(false).addBox(0.625f, 0, -1.45f, 0.619f, 1.486f, 1.43f), PartPose.offsetAndRotation(0, 2.6f, 0, 0, 0, 0));
        PartDefinition leftFingers = leftHand.addOrReplaceChild("left_fingers", CubeListBuilder.create()
                .texOffs(215, 38).mirror(true).addBox(0.631f, 0, -1.45f, 0.619f, 1.486f, 1.43f)
                .texOffs(215, 38).mirror(true).addBox(0.006f, 0, -1.45f, 0.619f, 1.736f, 1.43f)
                .texOffs(215, 38).mirror(true).addBox(-0.619f, 0, -1.45f, 0.619f, 1.736f, 1.43f)
                .texOffs(215, 38).mirror(true).addBox(-1.244f, 0, -1.45f, 0.619f, 1.486f, 1.43f), PartPose.offsetAndRotation(0, 2.6f, 0, 0, 0, 0));
        PartDefinition rightThumb = rightHand.addOrReplaceChild("right_thumb", CubeListBuilder.create()
                .texOffs(29, 48).mirror(false).addBox(-0.2f, 0, -0.5f, 0.681f, 1.54f, 0.681f), PartPose.offsetAndRotation(1.1f, 0.98f, -1.1f, 0.25f, 0, -0.35f));
        PartDefinition leftThumb = leftHand.addOrReplaceChild("left_thumb", CubeListBuilder.create()
                .texOffs(29, 48).mirror(true).addBox(-0.481f, 0, -0.5f, 0.681f, 1.54f, 0.681f), PartPose.offsetAndRotation(-1.1f, 0.98f, -1.1f, 0.25f, 0, 0.35f));
        PartDefinition rightToes = rightFoot.addOrReplaceChild("right_toes", CubeListBuilder.create()
                .texOffs(222, 38).mirror(false).addBox(-1.4f, 0.35f, -1.3f, 0.6522f, 1.05f, 1.5f)
                .texOffs(222, 38).mirror(false).addBox(-0.7f, 0.35f, -1.6f, 0.6522f, 1.05f, 1.8f)
                .texOffs(222, 38).mirror(false).addBox(0, 0.35f, -1.6f, 0.6522f, 1.05f, 1.8f)
                .texOffs(222, 38).mirror(false).addBox(0.7f, 0.35f, -1.3f, 0.6522f, 1.05f, 1.5f), PartPose.offsetAndRotation(0, 0, -3.2f, 0, 0, 0));
        PartDefinition leftToes = leftFoot.addOrReplaceChild("left_toes", CubeListBuilder.create()
                .texOffs(222, 38).mirror(true).addBox(0.7478f, 0.35f, -1.3f, 0.6522f, 1.05f, 1.5f)
                .texOffs(222, 38).mirror(true).addBox(0.0478f, 0.35f, -1.6f, 0.6522f, 1.05f, 1.8f)
                .texOffs(222, 38).mirror(true).addBox(-0.6522f, 0.35f, -1.6f, 0.6522f, 1.05f, 1.8f)
                .texOffs(222, 38).mirror(true).addBox(-1.3522f, 0.35f, -1.3f, 0.6522f, 1.05f, 1.5f), PartPose.offsetAndRotation(0, 0, -3.2f, 0, 0, 0));
        PartDefinition swaySkirtFront = body.addOrReplaceChild("sway_skirt_front", CubeListBuilder.create()
                .texOffs(89, 0).mirror(false).addBox(-4.1f, -0.5f, -0.4f, 8.2f, 10, 0.8f), PartPose.offsetAndRotation(0, 0, -2.8f, -0.06f, 0, 0));
        PartDefinition swaySkirtBack = body.addOrReplaceChild("sway_skirt_back", CubeListBuilder.create()
                .texOffs(89, 0).mirror(false).addBox(-4.1f, -0.5f, -0.4f, 8.2f, 10, 0.8f), PartPose.offsetAndRotation(0, 0, 2.8f, 0.08f, 0, 0));
        PartDefinition rightSkirtSide = body.addOrReplaceChild("right_skirt_side", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-0.4f, -0.5f, -2.8f, 0.8f, 9.6f, 5.6f), PartPose.offsetAndRotation(-4.1f, 0, 0, 0, 0, 0.1f));
        PartDefinition leftSkirtSide = body.addOrReplaceChild("left_skirt_side", CubeListBuilder.create()
                .texOffs(5, 0).mirror(true).addBox(-0.4f, -0.5f, -2.8f, 0.8f, 9.6f, 5.6f), PartPose.offsetAndRotation(4.1f, 0, 0, 0, 0, -0.1f));
        PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(47, 0).mirror(false).addBox(-3.4f, -6.4f, -3.2f, 6.8f, 6.4f, 6.4f)
                .texOffs(128, 38).mirror(false).addBox(-2.856f, -5.504f, -3.75f, 5.712f, 4.736f, 0.8f)
                .texOffs(229, 38).mirror(false).addBox(-1.7f, -2.688f, -4.8f, 3.4f, 1.7f, 1.2f)
                .texOffs(34, 48).mirror(false).addBox(-1.768f, -1.738f, -4.95f, 3.536f, 0.8f, 1.3f)
                .texOffs(58, 48).mirror(false).addBox(-1.02f, -3.088f, -5.05f, 2.04f, 0.85f, 0.55f)
                .texOffs(180, 0).mirror(false).addBox(-3.502f, -6.656f, -1.12f, 7.004f, 4.224f, 4.48f)
                .texOffs(62, 38).mirror(false).addBox(-3.23f, -1.28f, -1.92f, 6.46f, 1.792f, 4.8f), PartPose.offsetAndRotation(0, -9.2f, -0.8f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(242, 38).mirror(false).addBox(-3.23f, -0.6f, -0.65f, 3.332f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -5.376f, -3.95f, 0, 0.18f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(242, 38).mirror(true).addBox(-0.102f, -0.6f, -0.65f, 3.332f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -5.376f, -3.95f, 0, -0.18f, -0.2f));
        PartDefinition rightCheekpad = head.addOrReplaceChild("right_cheekpad", CubeListBuilder.create()
                .texOffs(165, 38).mirror(false).addBox(-1.3f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(-1.7f, -2.432f, -3.65f, 0, 0.3f, 0));
        PartDefinition leftCheekpad = head.addOrReplaceChild("left_cheekpad", CubeListBuilder.create()
                .texOffs(165, 38).mirror(true).addBox(-1.1f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(1.7f, -2.432f, -3.65f, 0, -0.3f, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 48).mirror(false).addBox(-1.462f, 0, -1.28f, 2.924f, 1.4f, 1.48f)
                .texOffs(47, 48).mirror(false).addBox(-1.36f, 1.2f, -0.6f, 2.72f, 1, 1.2f), PartPose.offsetAndRotation(0, -1.188f, -3.4f, 0.06f, 0, 0));
        PartDefinition rightCheek = head.addOrReplaceChild("right_cheek", CubeListBuilder.create()
                .texOffs(232, 0).mirror(false).addBox(-1.8f, -1.8f, -1.4f, 2.2f, 4.2f, 3.6f), PartPose.offsetAndRotation(-3.06f, -2.688f, -1.28f, 0.1f, 0.55f, 0.3f));
        PartDefinition leftCheek = head.addOrReplaceChild("left_cheek", CubeListBuilder.create()
                .texOffs(232, 0).mirror(true).addBox(-0.4f, -1.8f, -1.4f, 2.2f, 4.2f, 3.6f), PartPose.offsetAndRotation(3.06f, -2.688f, -1.28f, 0.1f, -0.55f, -0.3f));
        PartDefinition rightCheekLow = head.addOrReplaceChild("right_cheek_low", CubeListBuilder.create()
                .texOffs(87, 38).mirror(false).addBox(-1.5f, -0.6f, -1.2f, 1.9f, 3.2f, 3), PartPose.offsetAndRotation(-2.55f, -0.768f, -0.8f, 0.35f, 0.35f, 0.55f));
        PartDefinition leftCheekLow = head.addOrReplaceChild("left_cheek_low", CubeListBuilder.create()
                .texOffs(87, 38).mirror(true).addBox(-0.4f, -0.6f, -1.2f, 1.9f, 3.2f, 3), PartPose.offsetAndRotation(2.55f, -0.768f, -0.8f, 0.35f, -0.35f, -0.55f));
        PartDefinition crownlock0 = head.addOrReplaceChild("crownlock_0", CubeListBuilder.create()
                .texOffs(176, 38).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(0, -6.4f, -1.6f, -0.75f, 0, 0));
        PartDefinition crownlock1 = head.addOrReplaceChild("crownlock_1", CubeListBuilder.create()
                .texOffs(176, 38).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(-1.7f, -6.4f, -0.32f, -0.75f, 0.3f, 0.18f));
        PartDefinition crownlock2 = head.addOrReplaceChild("crownlock_2", CubeListBuilder.create()
                .texOffs(176, 38).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(1.7f, -6.4f, -0.32f, -0.75f, -0.3f, -0.18f));
        PartDefinition crownlock3 = head.addOrReplaceChild("crownlock_3", CubeListBuilder.create()
                .texOffs(176, 38).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(0, -6.4f, 1.12f, -0.75f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(143, 38).mirror(false).addBox(-0.9f, -1.26f, -1.05f, 1, 2.52f, 2.205f), PartPose.offsetAndRotation(-3.5f, -3.84f, -0.48f, 0, -0.35f, 0.18f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(143, 38).mirror(true).addBox(-0.1f, -1.26f, -1.05f, 1, 2.52f, 2.205f), PartPose.offsetAndRotation(3.5f, -3.84f, -0.48f, 0, 0.35f, -0.18f));
        PartDefinition beard = jaw.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(98, 38).mirror(false).addBox(-2.6f, 0, -1, 5.2f, 4.6f, 2), PartPose.offsetAndRotation(0, 1.2f, -1.2f, 0.3f, 0, 0));
        PartDefinition mane = head.addOrReplaceChild("mane", CubeListBuilder.create()
                .texOffs(110, 0).mirror(false).addBox(-3.6f, -1.6f, -2.8f, 7.2f, 3.8f, 7)
                .texOffs(15, 38).mirror(false).addBox(-3.2f, 1.6f, 2.4f, 6.4f, 5.2f, 1.8f), PartPose.offsetAndRotation(0, -5.6f, 1, 0.4f, 0, 0));
        PartDefinition rightSideburn = head.addOrReplaceChild("right_sideburn", CubeListBuilder.create()
                .texOffs(34, 38).mirror(false).addBox(-1, -1, -1.4f, 1.6f, 4.4f, 3), PartPose.offsetAndRotation(-3.1f, -3.4f, -1, 0, 0.2f, 0.3f));
        PartDefinition leftSideburn = head.addOrReplaceChild("left_sideburn", CubeListBuilder.create()
                .texOffs(34, 38).mirror(true).addBox(-0.6f, -1, -1.4f, 1.6f, 4.4f, 3), PartPose.offsetAndRotation(3.1f, -3.4f, -1, 0, -0.2f, -0.3f));
        PartDefinition skull0 = chest.addOrReplaceChild("skull_0", CubeListBuilder.create()
                .texOffs(11, 48).mirror(false).addBox(-0.7f, -0.7f, -0.8f, 1.4f, 1.5f, 1.4f), PartPose.offsetAndRotation(-3.8167f, -7.4044f, -3.1073f, 0, 0.96f, 0));
        PartDefinition skull1 = chest.addOrReplaceChild("skull_1", CubeListBuilder.create()
                .texOffs(11, 48).mirror(false).addBox(-0.7f, -0.7f, -0.8f, 1.4f, 1.5f, 1.4f), PartPose.offsetAndRotation(-3.2077f, -6.6831f, -3.4703f, 0, 0.72f, 0));
        PartDefinition skull2 = chest.addOrReplaceChild("skull_2", CubeListBuilder.create()
                .texOffs(11, 48).mirror(false).addBox(-0.7f, -0.7f, -0.8f, 1.4f, 1.5f, 1.4f), PartPose.offsetAndRotation(-2.3122f, -6.104f, -3.7555f, 0, 0.48f, 0));
        PartDefinition skull3 = chest.addOrReplaceChild("skull_3", CubeListBuilder.create()
                .texOffs(11, 48).mirror(false).addBox(-0.7f, -0.7f, -0.8f, 1.4f, 1.5f, 1.4f), PartPose.offsetAndRotation(-1.2102f, -5.7295f, -3.9375f, 0, 0.24f, 0));
        PartDefinition skull4 = chest.addOrReplaceChild("skull_4", CubeListBuilder.create()
                .texOffs(11, 48).mirror(false).addBox(-0.7f, -0.7f, -0.8f, 1.4f, 1.5f, 1.4f), PartPose.offsetAndRotation(0, -5.6f, -4, 0, -0, 0));
        PartDefinition skull5 = chest.addOrReplaceChild("skull_5", CubeListBuilder.create()
                .texOffs(11, 48).mirror(false).addBox(-0.7f, -0.7f, -0.8f, 1.4f, 1.5f, 1.4f), PartPose.offsetAndRotation(1.2102f, -5.7295f, -3.9375f, 0, -0.24f, 0));
        PartDefinition skull6 = chest.addOrReplaceChild("skull_6", CubeListBuilder.create()
                .texOffs(11, 48).mirror(false).addBox(-0.7f, -0.7f, -0.8f, 1.4f, 1.5f, 1.4f), PartPose.offsetAndRotation(2.3122f, -6.104f, -3.7555f, 0, -0.48f, 0));
        PartDefinition skull7 = chest.addOrReplaceChild("skull_7", CubeListBuilder.create()
                .texOffs(11, 48).mirror(false).addBox(-0.7f, -0.7f, -0.8f, 1.4f, 1.5f, 1.4f), PartPose.offsetAndRotation(3.2077f, -6.6831f, -3.4703f, 0, -0.72f, 0));
        PartDefinition skull8 = chest.addOrReplaceChild("skull_8", CubeListBuilder.create()
                .texOffs(11, 48).mirror(false).addBox(-0.7f, -0.7f, -0.8f, 1.4f, 1.5f, 1.4f), PartPose.offsetAndRotation(3.8167f, -7.4044f, -3.1073f, 0, -0.96f, 0));
        PartDefinition rightCuffband = rightForearm.addOrReplaceChild("right_cuffband", CubeListBuilder.create()
                .texOffs(191, 38).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 1.2f, 3), PartPose.offsetAndRotation(0, 5, 0, 0, 0, 0));
        PartDefinition tail0 = body.addOrReplaceChild("tail_0", CubeListBuilder.create()
                .texOffs(152, 38).mirror(false).addBox(-0.65f, -0.65f, 0, 1.3f, 1.3f, 3.3f), PartPose.offsetAndRotation(0, -0.4f, 2.4f, 1.2f, 0, 0));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(152, 38).mirror(false).addBox(-0.5667f, -0.5667f, 0, 1.1333f, 1.1333f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.22f, 0, 0));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(204, 38).mirror(false).addBox(-0.4833f, -0.4833f, 0, 0.9667f, 0.9667f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.22f, 0, 0));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create()
                .texOffs(204, 38).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.22f, 0, 0));
        PartDefinition spade = rightHand.addOrReplaceChild("spade", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.45f, -30, -0.45f, 0.9f, 36, 0.9f)
                .texOffs(20, 48).mirror(false).addBox(-0.7f, -30.8f, -0.7f, 1.4f, 1.4f, 1.4f)
                .texOffs(141, 0).mirror(false).addBox(-0.25f, -35.6f, -2.2f, 0.5f, 5.2f, 4.4f)
                .texOffs(20, 48).mirror(false).addBox(-0.7f, 5.2f, -0.7f, 1.4f, 1.4f, 1.4f)
                .texOffs(0, 38).mirror(false).addBox(-0.25f, 6.4f, -2.6f, 0.5f, 2.6f, 5.2f), PartPose.offsetAndRotation(0, 1.4f, 0, 0.408f, 0.0434f, -0.0901f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED sha: end
    }

    public static LayerDefinition macaqueLayer() {
        // GENERATED macaque: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(126, 0).mirror(false).addBox(-1.5f, -0.5f, -1.5f, 3, 6.5f, 3), PartPose.offsetAndRotation(-1.9f, 10.497f, 0.2f, -0.2f, 0, 0.06f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(126, 0).mirror(true).addBox(-1.5f, -0.5f, -1.5f, 3, 6.5f, 3), PartPose.offsetAndRotation(1.9f, 10.497f, 0.2f, -0.2f, 0, -0.06f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(199, 0).mirror(false).addBox(-1.35f, 0, -1.35f, 2.7f, 6, 2.7f), PartPose.offsetAndRotation(0, 6, 0, 0.32f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(199, 0).mirror(true).addBox(-1.35f, 0, -1.35f, 2.7f, 6, 2.7f), PartPose.offsetAndRotation(0, 6, 0, 0.32f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(214, 39).mirror(false).addBox(-1.5f, 0, -3.35f, 3, 1.6f, 5), PartPose.offsetAndRotation(0, 6, 0, -0.12f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(214, 39).mirror(true).addBox(-1.5f, 0, -3.35f, 3, 1.6f, 5), PartPose.offsetAndRotation(0, 6, 0, -0.12f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(212, 0).mirror(false).addBox(-3, -3.4f, -2.2f, 6, 4, 4.4f), PartPose.offsetAndRotation(0, 10.497f, 0.3f, 0.08f, 0, 0));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create()
                .texOffs(37, 0).mirror(false).addBox(-3.8f, -9, -2.5f, 7.6f, 9, 5), PartPose.offsetAndRotation(0, -3, 0, 0.05f, 0, 0));
        PartDefinition rightArm = chest.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(95, 0).mirror(false).addBox(-1.35f, -1.215f, -1.35f, 2.7f, 8.615f, 2.7f), PartPose.offsetAndRotation(-4.745f, -7.515f, 0, -0.12f, 0, 0.12f));
        PartDefinition leftArm = chest.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(95, 0).mirror(true).addBox(-1.35f, -1.215f, -1.35f, 2.7f, 8.615f, 2.7f), PartPose.offsetAndRotation(4.745f, -7.515f, 0, -0.12f, 0, -0.12f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(108, 0).mirror(false).addBox(-1.25f, 0, -1.25f, 2.5f, 7.2f, 2.5f), PartPose.offsetAndRotation(0, 7.4f, 0, -0.4f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(108, 0).mirror(true).addBox(-1.25f, 0, -1.25f, 2.5f, 7.2f, 2.5f), PartPose.offsetAndRotation(0, 7.4f, 0, -0.4f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(22, 49).mirror(false).addBox(-1.35f, 0, -1.35f, 2.7f, 2.8f, 2.7f), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(22, 49).mirror(true).addBox(-1.35f, 0, -1.35f, 2.7f, 2.8f, 2.7f), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, 0));
        PartDefinition rightFingers = rightHand.addOrReplaceChild("right_fingers", CubeListBuilder.create()
                .texOffs(190, 49).mirror(false).addBox(-1.3f, 0, -1.5f, 0.6429f, 1.486f, 1.485f)
                .texOffs(190, 49).mirror(false).addBox(-0.65f, 0, -1.5f, 0.6429f, 1.736f, 1.485f)
                .texOffs(190, 49).mirror(false).addBox(0, 0, -1.5f, 0.6429f, 1.736f, 1.485f)
                .texOffs(190, 49).mirror(false).addBox(0.65f, 0, -1.5f, 0.6429f, 1.486f, 1.485f), PartPose.offsetAndRotation(0, 2.6f, 0, 0, 0, 0));
        PartDefinition leftFingers = leftHand.addOrReplaceChild("left_fingers", CubeListBuilder.create()
                .texOffs(190, 49).mirror(true).addBox(0.6571f, 0, -1.5f, 0.6429f, 1.486f, 1.485f)
                .texOffs(190, 49).mirror(true).addBox(0.0071f, 0, -1.5f, 0.6429f, 1.736f, 1.485f)
                .texOffs(190, 49).mirror(true).addBox(-0.6429f, 0, -1.5f, 0.6429f, 1.736f, 1.485f)
                .texOffs(190, 49).mirror(true).addBox(-1.2929f, 0, -1.5f, 0.6429f, 1.486f, 1.485f), PartPose.offsetAndRotation(0, 2.6f, 0, 0, 0, 0));
        PartDefinition rightThumb = rightHand.addOrReplaceChild("right_thumb", CubeListBuilder.create()
                .texOffs(11, 57).mirror(false).addBox(-0.2f, 0, -0.5f, 0.7071f, 1.54f, 0.7071f), PartPose.offsetAndRotation(1.15f, 0.98f, -1.15f, 0.25f, 0, -0.35f));
        PartDefinition leftThumb = leftHand.addOrReplaceChild("left_thumb", CubeListBuilder.create()
                .texOffs(11, 57).mirror(true).addBox(-0.5071f, 0, -0.5f, 0.7071f, 1.54f, 0.7071f), PartPose.offsetAndRotation(-1.15f, 0.98f, -1.15f, 0.25f, 0, 0.35f));
        PartDefinition rightToes = rightFoot.addOrReplaceChild("right_toes", CubeListBuilder.create()
                .texOffs(197, 49).mirror(false).addBox(-1.4f, 0.4f, -1.3f, 0.6522f, 1.2f, 1.5f)
                .texOffs(197, 49).mirror(false).addBox(-0.7f, 0.4f, -1.6f, 0.6522f, 1.2f, 1.8f)
                .texOffs(197, 49).mirror(false).addBox(0, 0.4f, -1.6f, 0.6522f, 1.2f, 1.8f)
                .texOffs(197, 49).mirror(false).addBox(0.7f, 0.4f, -1.3f, 0.6522f, 1.2f, 1.5f), PartPose.offsetAndRotation(0, 0, -3.35f, 0, 0, 0));
        PartDefinition leftToes = leftFoot.addOrReplaceChild("left_toes", CubeListBuilder.create()
                .texOffs(197, 49).mirror(true).addBox(0.7478f, 0.4f, -1.3f, 0.6522f, 1.2f, 1.5f)
                .texOffs(197, 49).mirror(true).addBox(0.0478f, 0.4f, -1.6f, 0.6522f, 1.2f, 1.8f)
                .texOffs(197, 49).mirror(true).addBox(-0.6522f, 0.4f, -1.6f, 0.6522f, 1.2f, 1.8f)
                .texOffs(197, 49).mirror(true).addBox(-1.3522f, 0.4f, -1.3f, 0.6522f, 1.2f, 1.5f), PartPose.offsetAndRotation(0, 0, -3.35f, 0, 0, 0));
        PartDefinition rightElbowtuft = rightArm.addOrReplaceChild("right_elbowtuft", CubeListBuilder.create()
                .texOffs(120, 49).mirror(false).addBox(-1.08f, -0.4f, -0.3f, 2.16f, 2.43f, 1.6f), PartPose.offsetAndRotation(0, 6.8f, 1.215f, 0.7f, 0, 0));
        PartDefinition leftElbowtuft = leftArm.addOrReplaceChild("left_elbowtuft", CubeListBuilder.create()
                .texOffs(120, 49).mirror(true).addBox(-1.08f, -0.4f, -0.3f, 2.16f, 2.43f, 1.6f), PartPose.offsetAndRotation(0, 6.8f, 1.215f, 0.7f, 0, 0));
        PartDefinition rightForearmtuft = rightForearm.addOrReplaceChild("right_forearmtuft", CubeListBuilder.create()
                .texOffs(35, 49).mirror(false).addBox(-1.2f, -0.4f, -1, 1.4f, 3.24f, 2), PartPose.offsetAndRotation(-1.25f, 2.52f, 0, 0, 0, 0.55f));
        PartDefinition leftForearmtuft = leftForearm.addOrReplaceChild("left_forearmtuft", CubeListBuilder.create()
                .texOffs(35, 49).mirror(true).addBox(-0.2f, -0.4f, -1, 1.4f, 3.24f, 2), PartPose.offsetAndRotation(1.25f, 2.52f, 0, 0, 0, -0.55f));
        PartDefinition rightCalftuft = rightShin.addOrReplaceChild("right_calftuft", CubeListBuilder.create()
                .texOffs(120, 49).mirror(false).addBox(-1.08f, -0.2f, -0.4f, 2.16f, 2.7f, 1.4f), PartPose.offsetAndRotation(0, 1.08f, 1.35f, 0.45f, 0, 0));
        PartDefinition leftCalftuft = leftShin.addOrReplaceChild("left_calftuft", CubeListBuilder.create()
                .texOffs(120, 49).mirror(true).addBox(-1.08f, -0.2f, -0.4f, 2.16f, 2.7f, 1.4f), PartPose.offsetAndRotation(0, 1.08f, 1.35f, 0.45f, 0, 0));
        PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(64, 0).mirror(false).addBox(-3.7f, -6.5f, -3.4f, 7.4f, 6.5f, 6.8f)
                .texOffs(44, 49).mirror(false).addBox(-3.108f, -5.59f, -3.95f, 6.216f, 4.81f, 0.8f)
                .texOffs(61, 49).mirror(false).addBox(-2, -2.73f, -6, 4, 2.2f, 2.2f)
                .texOffs(204, 49).mirror(false).addBox(-2.08f, -1.28f, -6.15f, 4.16f, 0.8f, 2.3f)
                .texOffs(16, 57).mirror(false).addBox(-1.2f, -3.13f, -6.25f, 2.4f, 1.1f, 0.55f)
                .texOffs(139, 0).mirror(false).addBox(-3.811f, -6.76f, -1.19f, 7.622f, 4.29f, 4.76f)
                .texOffs(71, 39).mirror(false).addBox(-3.515f, -1.3f, -2.04f, 7.03f, 1.82f, 5.1f), PartPose.offsetAndRotation(0, -9, -0.9f, -0.05f, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(221, 49).mirror(false).addBox(-3.515f, -0.6f, -0.65f, 3.626f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -5.46f, -4.15f, 0, 0.18f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(221, 49).mirror(true).addBox(-0.111f, -0.6f, -0.65f, 3.626f, 1.25f, 1.3f), PartPose.offsetAndRotation(0, -5.46f, -4.15f, 0, -0.18f, -0.2f));
        PartDefinition rightCheekpad = head.addOrReplaceChild("right_cheekpad", CubeListBuilder.create()
                .texOffs(131, 49).mirror(false).addBox(-1.3f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(-1.85f, -2.47f, -3.85f, 0, 0.3f, 0));
        PartDefinition leftCheekpad = head.addOrReplaceChild("left_cheekpad", CubeListBuilder.create()
                .texOffs(131, 49).mirror(true).addBox(-1.1f, -1.2f, -0.7f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(1.85f, -2.47f, -3.85f, 0, -0.3f, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(142, 49).mirror(false).addBox(-1.72f, 0, -2.16f, 3.44f, 1.4f, 2.36f)
                .texOffs(25, 57).mirror(false).addBox(-1.6f, 1.2f, -1.2f, 3.2f, 1, 1.8f), PartPose.offsetAndRotation(0, -0.73f, -3.6f, 0.06f, 0, 0));
        PartDefinition rightCheek = head.addOrReplaceChild("right_cheek", CubeListBuilder.create()
                .texOffs(166, 0).mirror(false).addBox(-1.8f, -1.8f, -1.4f, 2.2f, 5.67f, 3.6f), PartPose.offsetAndRotation(-3.33f, -2.73f, -1.36f, 0.1f, 0.55f, 0.3f));
        PartDefinition leftCheek = head.addOrReplaceChild("left_cheek", CubeListBuilder.create()
                .texOffs(166, 0).mirror(true).addBox(-0.4f, -1.8f, -1.4f, 2.2f, 5.67f, 3.6f), PartPose.offsetAndRotation(3.33f, -2.73f, -1.36f, 0.1f, -0.55f, -0.3f));
        PartDefinition rightCheekLow = head.addOrReplaceChild("right_cheek_low", CubeListBuilder.create()
                .texOffs(100, 39).mirror(false).addBox(-1.5f, -0.6f, -1.2f, 1.9f, 4.32f, 3), PartPose.offsetAndRotation(-2.775f, -0.78f, -0.85f, 0.35f, 0.35f, 0.55f));
        PartDefinition leftCheekLow = head.addOrReplaceChild("left_cheek_low", CubeListBuilder.create()
                .texOffs(100, 39).mirror(true).addBox(-0.4f, -0.6f, -1.2f, 1.9f, 4.32f, 3), PartPose.offsetAndRotation(2.775f, -0.78f, -0.85f, 0.35f, -0.35f, -0.55f));
        PartDefinition crownlock0 = head.addOrReplaceChild("crownlock_0", CubeListBuilder.create()
                .texOffs(157, 49).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(0, -6.5f, -1.7f, -0.75f, 0, 0));
        PartDefinition crownlock1 = head.addOrReplaceChild("crownlock_1", CubeListBuilder.create()
                .texOffs(157, 49).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(-1.85f, -6.5f, -0.34f, -0.75f, 0.3f, 0.18f));
        PartDefinition crownlock2 = head.addOrReplaceChild("crownlock_2", CubeListBuilder.create()
                .texOffs(157, 49).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.2f), PartPose.offsetAndRotation(1.85f, -6.5f, -0.34f, -0.75f, -0.3f, -0.18f));
        PartDefinition crownlock3 = head.addOrReplaceChild("crownlock_3", CubeListBuilder.create()
                .texOffs(157, 49).mirror(false).addBox(-1.1f, -0.5f, -0.4f, 2.2f, 1, 3.8f), PartPose.offsetAndRotation(0, -6.5f, 1.19f, -0.75f, 0, 0));
        PartDefinition rightEar0 = head.addOrReplaceChild("right_ear_0", CubeListBuilder.create()
                .texOffs(172, 49).mirror(false).addBox(-0.9f, -1.08f, -0.9f, 1, 2.16f, 1.89f), PartPose.offsetAndRotation(-3.8f, -6.05f, -0.01f, 0, -0.35f, 0.18f));
        PartDefinition leftEar0 = head.addOrReplaceChild("left_ear_0", CubeListBuilder.create()
                .texOffs(172, 49).mirror(true).addBox(-0.1f, -1.08f, -0.9f, 1, 2.16f, 1.89f), PartPose.offsetAndRotation(3.8f, -6.05f, -0.01f, 0, 0.35f, -0.18f));
        PartDefinition rightEar1 = head.addOrReplaceChild("right_ear_1", CubeListBuilder.create()
                .texOffs(172, 49).mirror(false).addBox(-0.9f, -1.08f, -0.9f, 1, 2.16f, 1.89f), PartPose.offsetAndRotation(-3.8f, -3.9f, -0.51f, 0, -0.35f, 0.1f));
        PartDefinition leftEar1 = head.addOrReplaceChild("left_ear_1", CubeListBuilder.create()
                .texOffs(172, 49).mirror(true).addBox(-0.1f, -1.08f, -0.9f, 1, 2.16f, 1.89f), PartPose.offsetAndRotation(3.8f, -3.9f, -0.51f, 0, 0.35f, -0.1f));
        PartDefinition rightEar2 = head.addOrReplaceChild("right_ear_2", CubeListBuilder.create()
                .texOffs(172, 49).mirror(false).addBox(-0.9f, -1.08f, -0.9f, 1, 2.16f, 1.89f), PartPose.offsetAndRotation(-3.8f, -1.75f, -0.01f, 0, -0.35f, 0.02f));
        PartDefinition leftEar2 = head.addOrReplaceChild("left_ear_2", CubeListBuilder.create()
                .texOffs(172, 49).mirror(true).addBox(-0.1f, -1.08f, -0.9f, 1, 2.16f, 1.89f), PartPose.offsetAndRotation(3.8f, -1.75f, -0.01f, 0, 0.35f, -0.02f));
        PartDefinition cap = head.addOrReplaceChild("cap", CubeListBuilder.create()
                .texOffs(0, 39).mirror(false).addBox(-3, -2.4f, -3, 6, 2.6f, 6)
                .texOffs(234, 49).mirror(false).addBox(-0.9f, -3.4f, -3.3f, 1.8f, 1.6f, 1.2f), PartPose.offsetAndRotation(0, -5.4f, 0.2f, 0, 0, 0));
        PartDefinition rightCapwing = cap.addOrReplaceChild("right_capwing", CubeListBuilder.create()
                .texOffs(111, 39).mirror(false).addBox(-0.3f, -3.4f, -1.6f, 0.6f, 3.6f, 3.4f), PartPose.offsetAndRotation(-3.1f, -1, 0, 0.1f, 0.1f, -0.55f));
        PartDefinition leftCapwing = cap.addOrReplaceChild("left_capwing", CubeListBuilder.create()
                .texOffs(111, 39).mirror(true).addBox(-0.3f, -3.4f, -1.6f, 0.6f, 3.6f, 3.4f), PartPose.offsetAndRotation(3.1f, -1, 0, 0.1f, -0.1f, 0.55f));
        PartDefinition rightPlume = cap.addOrReplaceChild("right_plume", CubeListBuilder.create()
                .texOffs(32, 0).mirror(false).addBox(-0.35f, -14, -0.35f, 0.7f, 14, 0.7f), PartPose.offsetAndRotation(-1, -2.4f, 1, -0.95f, 0.1f, -0.15f));
        PartDefinition leftPlume = cap.addOrReplaceChild("left_plume", CubeListBuilder.create()
                .texOffs(32, 0).mirror(true).addBox(-0.35f, -14, -0.35f, 0.7f, 14, 0.7f), PartPose.offsetAndRotation(1, -2.4f, 1, -0.95f, -0.1f, 0.15f));
        PartDefinition rightPlumeTip = rightPlume.addOrReplaceChild("right_plume_tip", CubeListBuilder.create()
                .texOffs(181, 0).mirror(false).addBox(-0.3f, -9, -0.3f, 0.6f, 9, 0.6f), PartPose.offsetAndRotation(0, -13.6f, 0, -0.55f, 0, -0.1f));
        PartDefinition leftPlumeTip = leftPlume.addOrReplaceChild("left_plume_tip", CubeListBuilder.create()
                .texOffs(181, 0).mirror(true).addBox(-0.3f, -9, -0.3f, 0.6f, 9, 0.6f), PartPose.offsetAndRotation(0, -13.6f, 0, -0.55f, 0, 0.1f));
        PartDefinition fillet = head.addOrReplaceChild("fillet", CubeListBuilder.create()
                .texOffs(122, 39).mirror(false).addBox(-3.45f, -0.5f, -3.3f, 6.9f, 0.9f, 6.6f), PartPose.offsetAndRotation(0, -4.2f, 0, 0, 0, 0));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(151, 39).mirror(false).addBox(-2, -1.4f, -2.2f, 3.6f, 2.6f, 4.4f)
                .texOffs(231, 39).mirror(false).addBox(-2.4f, 0.9f, -2.4f, 3.4f, 1.8f, 4.8f), PartPose.offsetAndRotation(-0.7f, -0.8f, 0, 0, 0, 0.32f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(151, 39).mirror(true).addBox(-1.6f, -1.4f, -2.2f, 3.6f, 2.6f, 4.4f)
                .texOffs(231, 39).mirror(true).addBox(-1, 0.9f, -2.4f, 3.4f, 1.8f, 4.8f), PartPose.offsetAndRotation(0.7f, -0.8f, 0, 0, 0, -0.32f));
        PartDefinition belt = body.addOrReplaceChild("belt", CubeListBuilder.create()
                .texOffs(170, 39).mirror(false).addBox(-3.5f, -0.7f, -2.6f, 7, 1.6f, 5.2f)
                .texOffs(243, 49).mirror(false).addBox(-1.2f, -1, -2.9f, 2.4f, 2.2f, 0.6f), PartPose.offsetAndRotation(0, -3, 0, 0, 0, 0));
        PartDefinition swayKiltFront = body.addOrReplaceChild("sway_kilt_front", CubeListBuilder.create()
                .texOffs(0, 49).mirror(false).addBox(-3.2f, 0, -0.4f, 6.4f, 5.6f, 0.8f), PartPose.offsetAndRotation(0, -1.4f, -2.3f, -0.08f, 0, 0));
        PartDefinition swayKiltBack = body.addOrReplaceChild("sway_kilt_back", CubeListBuilder.create()
                .texOffs(0, 49).mirror(false).addBox(-3.2f, 0, -0.4f, 6.4f, 5.2f, 0.8f), PartPose.offsetAndRotation(0, -1.4f, 2.3f, 0.12f, 0, 0));
        PartDefinition rightKiltSide = body.addOrReplaceChild("right_kilt_side", CubeListBuilder.create()
                .texOffs(186, 0).mirror(false).addBox(-0.4f, 0, -2.2f, 0.8f, 4.8f, 4.4f), PartPose.offsetAndRotation(-3.1f, -1.4f, 0, 0, 0, 0.14f));
        PartDefinition leftKiltSide = body.addOrReplaceChild("left_kilt_side", CubeListBuilder.create()
                .texOffs(186, 0).mirror(true).addBox(-0.4f, 0, -2.2f, 0.8f, 4.8f, 4.4f), PartPose.offsetAndRotation(3.1f, -1.4f, 0, 0, 0, -0.14f));
        PartDefinition swayCloak = chest.addOrReplaceChild("sway_cloak", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-4.6f, 0, -0.3f, 9.2f, 15, 0.6f), PartPose.offsetAndRotation(0, -8.6f, 2.8f, 0.16f, 0, 0));
        PartDefinition rightBracer = rightForearm.addOrReplaceChild("right_bracer", CubeListBuilder.create()
                .texOffs(25, 39).mirror(false).addBox(-1.55f, 0, -1.55f, 3.1f, 4.2f, 3.1f), PartPose.offsetAndRotation(0, 2.4f, 0, 0, 0, 0));
        PartDefinition leftBracer = leftForearm.addOrReplaceChild("left_bracer", CubeListBuilder.create()
                .texOffs(25, 39).mirror(true).addBox(-1.55f, 0, -1.55f, 3.1f, 4.2f, 3.1f), PartPose.offsetAndRotation(0, 2.4f, 0, 0, 0, 0));
        PartDefinition rightBootcuff = rightShin.addOrReplaceChild("right_bootcuff", CubeListBuilder.create()
                .texOffs(76, 49).mirror(false).addBox(-1.7f, 0, -1.7f, 3.4f, 1.4f, 3.4f), PartPose.offsetAndRotation(0, 0.8f, 0, 0, 0, 0));
        PartDefinition leftBootcuff = leftShin.addOrReplaceChild("left_bootcuff", CubeListBuilder.create()
                .texOffs(76, 49).mirror(true).addBox(-1.7f, 0, -1.7f, 3.4f, 1.4f, 3.4f), PartPose.offsetAndRotation(0, 0.8f, 0, 0, 0, 0));
        PartDefinition neckruff = chest.addOrReplaceChild("neckruff", CubeListBuilder.create()
                .texOffs(42, 39).mirror(false).addBox(-3.6f, -1.4f, -2.9f, 7.2f, 2.2f, 5.8f), PartPose.offsetAndRotation(0, -8.6f, 0, 0, 0, 0));
        PartDefinition rightUndersleeve = rightArm.addOrReplaceChild("right_undersleeve", CubeListBuilder.create()
                .texOffs(197, 39).mirror(false).addBox(-1.55f, 0, -1.55f, 3.1f, 3.6f, 3.1f), PartPose.offsetAndRotation(0, 2.4f, 0, 0, 0, 0));
        PartDefinition leftUndersleeve = leftArm.addOrReplaceChild("left_undersleeve", CubeListBuilder.create()
                .texOffs(197, 39).mirror(true).addBox(-1.55f, 0, -1.55f, 3.1f, 3.6f, 3.1f), PartPose.offsetAndRotation(0, 2.4f, 0, 0, 0, 0));
        PartDefinition rightKneeguard = rightShin.addOrReplaceChild("right_kneeguard", CubeListBuilder.create()
                .texOffs(0, 57).mirror(false).addBox(-1.6f, -1.2f, -0.7f, 3.2f, 2.8f, 1), PartPose.offsetAndRotation(0, -0.6f, -1.3f, -0.12f, 0, 0));
        PartDefinition leftKneeguard = leftShin.addOrReplaceChild("left_kneeguard", CubeListBuilder.create()
                .texOffs(0, 57).mirror(true).addBox(-1.6f, -1.2f, -0.7f, 3.2f, 2.8f, 1), PartPose.offsetAndRotation(0, -0.6f, -1.3f, -0.12f, 0, 0));
        PartDefinition swayBelttasselR = belt.addOrReplaceChild("sway_belttassel_r", CubeListBuilder.create()
                .texOffs(93, 49).mirror(false).addBox(-0.45f, 0, -0.3f, 0.9f, 4.6f, 0.6f), PartPose.offsetAndRotation(-2.2f, 0.8f, -2.7f, -0.05f, 0, 0.05f));
        PartDefinition swayBelttasselL = belt.addOrReplaceChild("sway_belttassel_l", CubeListBuilder.create()
                .texOffs(93, 49).mirror(false).addBox(-0.45f, 0, -0.3f, 0.9f, 4.6f, 0.6f), PartPose.offsetAndRotation(2.2f, 0.8f, -2.7f, -0.05f, 0, -0.05f));
        PartDefinition rightStrap = chest.addOrReplaceChild("right_strap", CubeListBuilder.create()
                .texOffs(121, 0).mirror(false).addBox(-0.5f, 0, -0.35f, 1, 9.2f, 0.6f), PartPose.offsetAndRotation(-1.8f, -8.4f, -2.6f, 0, 0, -0.42f));
        PartDefinition leftStrap = chest.addOrReplaceChild("left_strap", CubeListBuilder.create()
                .texOffs(121, 0).mirror(true).addBox(-0.5f, 0, -0.35f, 1, 9.2f, 0.6f), PartPose.offsetAndRotation(1.8f, -8.4f, -2.6f, 0, 0, 0.42f));
        PartDefinition rightPlumeEnd = rightPlumeTip.addOrReplaceChild("right_plume_end", CubeListBuilder.create()
                .texOffs(17, 49).mirror(false).addBox(-0.25f, -6, -0.25f, 0.5f, 6, 0.5f), PartPose.offsetAndRotation(0, -8.8f, 0, -0.55f, 0, -0.08f));
        PartDefinition leftPlumeEnd = leftPlumeTip.addOrReplaceChild("left_plume_end", CubeListBuilder.create()
                .texOffs(17, 49).mirror(true).addBox(-0.25f, -6, -0.25f, 0.5f, 6, 0.5f), PartPose.offsetAndRotation(0, -8.8f, 0, -0.55f, 0, 0.08f));
        PartDefinition tail0 = body.addOrReplaceChild("tail_0", CubeListBuilder.create()
                .texOffs(98, 49).mirror(false).addBox(-0.8f, -0.8f, 0, 1.6f, 1.6f, 3.3f), PartPose.offsetAndRotation(0, -1.6f, 2.3f, 1.2f, 0, 0));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(98, 49).mirror(false).addBox(-0.7125f, -0.7125f, 0, 1.425f, 1.425f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.24f, 0, 0));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(98, 49).mirror(false).addBox(-0.625f, -0.625f, 0, 1.25f, 1.25f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.24f, 0, 0));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create()
                .texOffs(98, 49).mirror(false).addBox(-0.5375f, -0.5375f, 0, 1.075f, 1.075f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.24f, 0, 0));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail_4", CubeListBuilder.create()
                .texOffs(179, 49).mirror(false).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 3.3f), PartPose.offsetAndRotation(0, 0, 3, 0.24f, 0, 0));
        PartDefinition staff = rightHand.addOrReplaceChild("staff", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.55f, -18, -0.55f, 1.1f, 36, 1.1f)
                .texOffs(111, 49).mirror(false).addBox(-0.75f, -21, -0.75f, 1.5f, 3.2f, 1.5f)
                .texOffs(111, 49).mirror(false).addBox(-0.75f, 17.8f, -0.75f, 1.5f, 3.2f, 1.5f), PartPose.offsetAndRotation(0, 1.4f, 0, 1.5869f, 0.0595f, -0.1043f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED macaque: end
    }

    /** Which creature a frame is, for its poses. */
    public enum Kind { GIBBON, APE, BABOON, TANG, BAJIE, SHA, MACAQUE }

    /**
     * The frame of all of them. Parts are found by name, each optional: {@code body} (the hips), its
     * {@code chest}, the chest's {@code head} (its {@code jaw}, its ears {@code right_ear*}), {@code right_arm} /
     * {@code right_forearm} / {@code right_hand} (and the left), the legs {@code right_leg} / {@code right_shin}
     * / {@code right_foot}, the tail {@code tail_0}, {@code tail_1}, ...; and any part named {@code sway*} hangs
     * and swings (cloth, tassels, plumes, beads).
     */
    public static final class MonkeyModel<T extends Mob> extends EntityModel<T> {
        private final ModelPart root;
        private final Kind kind;
        private final Map<String, ModelPart> parts = new HashMap<>();
        private final List<ModelPart> tail = new ArrayList<>(), sway = new ArrayList<>(), ears = new ArrayList<>();

        public MonkeyModel(ModelPart root, Kind kind) {
            this.root = root;
            this.kind = kind;
            collect("root", root);
            for (int i = 0; parts.containsKey("tail_" + i); i++) tail.add(parts.get("tail_" + i));
        }

        private void collect(String name, ModelPart p) {
            parts.put(name, p);
            if (name.startsWith("sway")) sway.add(p);
            if (name.contains("_ear")) ears.add(p);
            for (var e : children(p).entrySet()) collect(e.getKey(), e.getValue());
        }

        private static java.lang.reflect.Field childrenField;

        /** A part's children by name (ModelPart keeps them to itself; read once, when the model is made). */
        @SuppressWarnings("unchecked")
        private static Map<String, ModelPart> children(ModelPart p) {
            try {
                if (childrenField == null) {
                    childrenField = ModelPart.class.getDeclaredField("children");
                    childrenField.setAccessible(true);
                }
                return (Map<String, ModelPart>) childrenField.get(p);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("ModelPart.children", e);
            }
        }

        private ModelPart p(String name) {
            return parts.get(name);
        }

        /** Turns a part from its pose at rest by (x, y, z); nothing if it has no such part. */
        private void turn(String name, float x, float y, float z) {
            ModelPart q = p(name);
            if (q == null) return;
            q.xRot = q.getInitialPose().xRot + x;
            q.yRot = q.getInitialPose().yRot + y;
            q.zRot = q.getInitialPose().zRot + z;
        }

        private void lift(String name, float dy, float dz) {
            ModelPart q = p(name);
            if (q == null) return;
            q.y = q.getInitialPose().y + dy;
            q.z = q.getInitialPose().z + dz;
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float w = swing * 0.6662f;
            float stride = Mth.cos(w) * 0.85f * amount;
            float breath = Mth.sin(age * 0.08f);
            float hy = yaw * Mth.DEG_TO_RAD, hx = pitch * Mth.DEG_TO_RAD;
            // the legs stride, the knees bending as each comes through
            turn("right_leg", stride, 0, 0);
            turn("left_leg", -stride, 0, 0);
            turn("right_shin", Math.max(0, Mth.sin(w)) * 0.8f * amount, 0, 0);
            turn("left_shin", Math.max(0, -Mth.sin(w)) * 0.8f * amount, 0, 0);
            // the body rocks; the chest breathes; the head turns
            turn("body", 0, Mth.cos(w) * 0.07f * amount, Mth.cos(w) * 0.03f * amount);
            turn("chest", breath * 0.015f, 0, 0);
            turn("head", hx * 0.8f, hy * 0.8f, 0);
            // the arms swing against the legs
            float arm = Mth.cos(w) * 0.6f * amount;
            turn("right_arm", -arm - strike * 1.6f, 0, breath * 0.02f);
            turn("left_arm", arm, 0, -breath * 0.02f);
            turn("right_forearm", -strike * 0.3f, 0, 0);
            // the tail sways, the ears twitch, the cloth hangs and swings
            for (int i = 0; i < tail.size(); i++) {
                ModelPart t = tail.get(i);
                t.yRot = t.getInitialPose().yRot + Mth.sin(age * 0.09f + i * 0.8f) * 0.18f;
                t.xRot = t.getInitialPose().xRot + Mth.cos(age * 0.07f + i * 0.6f) * 0.06f + amount * 0.1f;
            }
            for (int i = 0; i < ears.size(); i++) {
                ModelPart q = ears.get(i);
                q.zRot = q.getInitialPose().zRot + Mth.sin(age * 0.13f + i * 1.7f) * 0.05f;
            }
            for (int i = 0; i < sway.size(); i++) {
                ModelPart q = sway.get(i);
                q.xRot = q.getInitialPose().xRot + amount * 0.35f + Mth.sin(age * 0.07f + i * 0.9f) * 0.04f;
                q.zRot = q.getInitialPose().zRot + Mth.sin(age * 0.05f + i) * 0.03f;
            }
            turn("jaw", strike * 0.35f + Math.max(0, Mth.sin(age * 0.05f)) * 0.05f, 0, 0);
            pose(e, age, strike, breath);
        }

        /** Each one's warnings and blows. */
        private void pose(T e, float age, float strike, float breath) {
            if (e instanceof FlowerFruitMonster m && m.isBowing()) {
                kneel();
                return;
            }
            switch (kind) {
                case GIBBON -> {
                    if (e instanceof StoneGibbonEntity g && g.isRearing()) {
                        turn("right_arm", -2.9f, 0, -0.2f);
                        turn("right_forearm", -0.6f, 0, 0);
                        turn("chest", 0.25f, 0.35f, 0);
                        turn("left_arm", -0.6f, 0, 0.4f);
                    }
                    // long arms hang loose, knuckles near the ground
                    if (strike > 0) turn("right_arm", -strike * 2.6f + 1.0f * strike, 0, 0);
                }
                case APE -> {
                    if (e instanceof SpearApeEntity a && a.isDrawing()) {
                        turn("chest", 0, 0.6f, 0);
                        turn("right_arm", -0.6f, 0.5f, 0);
                        turn("right_forearm", -1.2f, 0, 0);
                        turn("left_arm", -1.2f, -0.3f, 0);
                        turn("right_leg", -0.5f, 0, 0);
                        turn("left_leg", 0.4f, 0, 0);
                    } else if (strike > 0) {
                        // the thrust: the spear driven forward from the hip
                        turn("right_arm", -1.3f * strike, 0, 0);
                        turn("right_forearm", -0.45f + 0.4f * strike, 0, 0);
                        turn("chest", 0, -0.3f * strike, 0);
                    }
                }
                case BABOON -> {
                    if (e instanceof GreatBaboonEntity b && b.isBeating()) {
                        float beat = Mth.sin(age * 1.4f);
                        turn("body", -0.45f, 0, 0);
                        turn("chest", -0.2f, 0, 0);
                        turn("right_arm", -1.6f - beat * 0.5f, 0, 0.5f);
                        turn("left_arm", -1.6f + beat * 0.5f, 0, -0.5f);
                        turn("right_forearm", -1.4f, 0, 0);
                        turn("left_forearm", -1.4f, 0, 0);
                        turn("jaw", 0.5f, 0, 0);
                    } else if (strike > 0) {
                        // both fists come down
                        turn("right_arm", -2.6f * strike + 0.4f, 0, 0);
                        turn("left_arm", -2.6f * strike + 0.4f, 0, 0);
                        turn("chest", 0.3f * strike, 0, 0);
                    }
                }
                case TANG -> {
                    if (e instanceof FakePilgrimEntity f && f.isWarning()) {
                        // palms together before his breast, the staff in the crook of his arm, chanting
                        turn("right_arm", -1.0f, -0.5f, 0);
                        turn("left_arm", -1.0f, 0.5f, 0);
                        turn("right_forearm", -0.6f, 0, 0);
                        turn("left_forearm", -0.6f, 0, 0);
                        turn("head", 0.25f, 0, 0);
                        turn("jaw", Math.abs(Mth.sin(age * 0.9f)) * 0.25f, 0, 0);
                    } else {
                        turn("right_arm", -0.3f - strike * 1.4f, 0, 0);
                    }
                }
                case BAJIE -> {
                    if (e instanceof FakePilgrimEntity f && f.isWarning()) {
                        // the rake heaved up over his right shoulder
                        turn("right_arm", -2.7f, 0, -0.3f);
                        turn("left_arm", -2.5f, 0, 0.3f);
                        turn("chest", -0.2f, 0.4f, 0);
                    } else if (strike > 0) {
                        turn("right_arm", -2.4f + strike * 1.6f, -strike * 0.8f, 0);
                        turn("left_arm", -2.2f + strike * 1.5f, -strike * 0.8f, 0);
                        turn("chest", 0, -strike * 0.6f, 0);
                    }
                }
                case SHA -> {
                    if (e instanceof FakePilgrimEntity f && (f.isWarning() || f.isCharging())) {
                        // the spade levelled before him, crouched to charge
                        turn("body", 0.35f, 0, 0);
                        turn("right_arm", -1.4f, 0.2f, 0);
                        turn("left_arm", -1.3f, -0.4f, 0);
                        turn("right_leg", -0.6f, 0, 0);
                        turn("left_leg", 0.3f, 0, 0);
                    } else {
                        turn("right_arm", -0.4f - strike * 1.5f, 0, 0);
                    }
                }
                case MACAQUE -> macaque(e, age, strike);
            }
        }

        private void kneel() {
            turn("right_leg", -1.5f, 0, 0.1f);
            turn("left_leg", -1.5f, 0, -0.1f);
            turn("right_shin", 1.5f, 0, 0);
            turn("left_shin", 1.5f, 0, 0);
            lift("body", 5.5f, 0);
            lift("right_leg", 5.5f, 0);
            lift("left_leg", 5.5f, 0);
            turn("body", 0.7f, 0, 0);
            turn("head", -0.3f, 0, 0);
            turn("right_arm", -1.3f, 0, 0.1f);
            turn("left_arm", -1.3f, 0, -0.1f);
        }

        private void macaque(T e, float age, float strike) {
            int action = e instanceof SixEaredMacaqueEntity s ? s.action() : SixEaredMacaqueEntity.IDLE;
            switch (action) {
                case SixEaredMacaqueEntity.SPINNING -> {
                    // the staff whirled over his head in his right hand
                    turn("right_arm", -2.9f, 0, 0);
                    turn("right_forearm", 0, 0, 0);
                    turn("right_hand", 0, age * 0.9f, 0);
                    turn("left_arm", -0.4f, 0, 0.6f);
                    turn("chest", -0.1f, Mth.sin(age * 0.45f) * 0.2f, 0);
                }
                case SixEaredMacaqueEntity.CROUCHING -> {
                    lift("body", 3f, 0);
                    turn("body", 0.6f, 0, 0);
                    turn("right_leg", -1.1f, 0, 0);
                    turn("left_leg", -0.9f, 0, 0);
                    turn("right_shin", 1.4f, 0, 0);
                    turn("left_shin", 1.3f, 0, 0);
                    turn("right_arm", 0.6f, 0, 0.2f);
                    turn("left_arm", 0.4f, 0, -0.3f);
                }
                case SixEaredMacaqueEntity.DASHING -> {
                    turn("body", 0.9f, 0, 0);
                    turn("head", -0.7f, 0, 0);
                    turn("right_arm", 1.2f, 0, 0.3f);
                    turn("left_arm", 1.0f, 0, -0.3f);
                    turn("right_leg", 0.7f, 0, 0);
                    turn("left_leg", 0.4f, 0, 0);
                }
                case SixEaredMacaqueEntity.PLUCKING -> {
                    // a hand to his mouth, chewing the hairs, then blowing
                    turn("left_arm", -2.2f, 0.6f, 0);
                    turn("left_forearm", -1.3f, 0, 0);
                    turn("head", 0.1f, 0, 0);
                    turn("jaw", Math.abs(Mth.sin(age * 1.2f)) * 0.3f, 0, 0);
                }
                case SixEaredMacaqueEntity.RAISING -> {
                    // the grown staff raised high in both hands
                    turn("right_arm", -3.0f, 0, -0.15f);
                    turn("left_arm", -3.0f, 0, 0.15f);
                    turn("right_forearm", 0, 0, 0);
                    turn("chest", -0.25f, 0, 0);
                    turn("head", -0.3f, 0, 0);
                }
                default -> {
                    // at the ready, the staff across him; a blow brings it down from over his head
                    turn("right_arm", -0.7f - strike * 2.0f, 0, 0);
                    turn("right_forearm", -0.5f + strike * 0.4f, 0, 0);
                    turn("left_arm", -0.5f, 0.3f, 0.1f);
                }
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    // ── The renderers ───────────────────────────────────────────────────

    /** A creature of one frame and one skin (a captain's, where it has one), its eyes glowing over it. */
    static class Single<T extends Mob> extends MobRenderer<T, MonkeyModel<T>> {
        private final ResourceLocation skin, captain;
        private final float size;

        Single(EntityRendererProvider.Context ctx, ModelLayerLocation layer, String name, Kind kind, float size) {
            super(ctx, new MonkeyModel<>(ctx.bakeLayer(layer), kind), 0.6f * size);
            this.skin = tex(name);
            this.captain = tex(name + "_captain");
            this.size = size;
            RenderType eyes = RenderType.eyes(tex(name + "_glow"));
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
            return e instanceof ZoneMonster z && z.isCaptain() && !(e instanceof MacaqueCloneEntity) && hasCaptain() ? captain : skin;
        }

        /** Whether a captain's skin was painted for it (the army's three kinds have one). */
        protected boolean hasCaptain() {
            return true;
        }

        @Override
        protected boolean shouldShowName(T e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    /** The three fake pilgrims: one entity, three frames and skins. */
    static class PilgrimRenderer extends MobRenderer<FakePilgrimEntity, MonkeyModel<FakePilgrimEntity>> {
        private final List<MonkeyModel<FakePilgrimEntity>> models = new ArrayList<>();
        private static final String[] SKINS = {"false_tang_sanzang", "false_zhu_bajie", "false_sha_wujing"};
        private final List<RenderType> glows = new ArrayList<>();

        PilgrimRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new MonkeyModel<>(ctx.bakeLayer(TANG), Kind.TANG), 0.6f);
            models.add(model);
            models.add(new MonkeyModel<>(ctx.bakeLayer(BAJIE), Kind.BAJIE));
            models.add(new MonkeyModel<>(ctx.bakeLayer(SHA), Kind.SHA));
            for (String s : SKINS) glows.add(RenderType.eyes(tex(s + "_glow")));
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return glows.get(0);
                }

                @Override
                public void render(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, FakePilgrimEntity e,
                                   float swing, float amount, float partial, float age, float yaw, float pitch) {
                    VertexConsumer vc = buffers.getBuffer(glows.get(e.variant()));
                    getParentModel().renderToBuffer(pose, vc, 15728640, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                }
            });
        }

        @Override
        public void render(FakePilgrimEntity e, float yaw, float partial, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
            model = models.get(e.variant());
            super.render(e, yaw, partial, pose, buffers, light);
        }

        @Override
        protected void scale(FakePilgrimEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE_PILGRIM, SIZE_PILGRIM, SIZE_PILGRIM);
        }

        @Override
        public ResourceLocation getTextureLocation(FakePilgrimEntity e) {
            return tex(SKINS[e.variant()]);
        }

        @Override
        protected boolean shouldShowName(FakePilgrimEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }
}
