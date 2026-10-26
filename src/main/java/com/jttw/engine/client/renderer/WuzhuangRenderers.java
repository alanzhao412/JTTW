package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wuzhuang.CloudCraneEntity;
import com.jttw.engine.entity.wuzhuang.DaoistDiscipleEntity;
import com.jttw.engine.entity.wuzhuang.TempleYouthEntity;
import com.jttw.engine.entity.wuzhuang.ZhenyuanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
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
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * How Mount Wuzhuang's household looks. The disciples and the youths share a robed person's frame
 * (128x64): a Daoist robe to the ground over a person's body; the disciples a topknot with a
 * hairpin, the youths the two buns of boys' hair (丫髻). The Great Immortal Zhenyuan has a frame of
 * his own (192x192), towering (twice a man's height), on a soft cloud: a narrow waist and robes
 * belling out below it in tiers and folded panels to a wide hem, of deep indigo and earth-gold, great
 * billowing sleeves, a lotus crown, long white hair, brows and beard, a ribbon drifting about him, a
 * ring of the eight trigrams behind his head, his horsetail whisk (his left sleeve flung open and
 * swelling as he sweeps with it, its mouth dark with stars). The cloud crane has a box model of its
 * own (64x64): a long neck, a red crown, black-tipped wings spread and beating in flight, folded at
 * rest, swept back in a swoop, long legs trailing. Skins painted by
 * {@code assets_source/wuzhuang_folk.py}, {@code zhenyuan.py} and {@code wuzhuang_crane.py}.
 */
public final class WuzhuangRenderers {

    private WuzhuangRenderers() {}

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name), "main");
    }

    public static final ModelLayerLocation DISCIPLE_LAYER = layer("daoist_disciple"), YOUTH_LAYER = layer("temple_youth"),
            ZHENYUAN_LAYER = layer("zhenyuan"), CRANE_LAYER = layer("cloud_crane");

    // ── The robed frame ─────────────────────────────────────────────────

    /** A person (128x64) in a Daoist robe to the ground. */
    private static MeshDefinition robed(float flare) {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        mesh.getRoot().getChild("body").addOrReplaceChild("robe", CubeListBuilder.create().texOffs(64, 0)
                .addBox(-5, 10, -3.5f, 10, 14, 7, new CubeDeformation(flare)), PartPose.ZERO);
        return mesh;
    }

    /** A disciple: a topknot bound with a hairpin. */
    public static LayerDefinition discipleLayer() {
        MeshDefinition mesh = robed(0.1f);
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("bun", CubeListBuilder.create().texOffs(88, 22).addBox(-1.5f, -11, -1.5f, 3, 3, 3), PartPose.ZERO);
        head.addOrReplaceChild("pin", CubeListBuilder.create().texOffs(100, 22).addBox(-3, -10, -0.5f, 6, 1, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** A youth: the two buns of a boy's hair, either side of his crown. */
    public static LayerDefinition youthLayer() {
        MeshDefinition mesh = robed(0.1f);
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("bun_l", CubeListBuilder.create().texOffs(88, 22).addBox(1.5f, -10.5f, -1.5f, 3, 3, 3), PartPose.ZERO);
        head.addOrReplaceChild("bun_r", CubeListBuilder.create().texOffs(88, 22).addBox(-4.5f, -10.5f, -1.5f, 3, 3, 3), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /**
     * Zhenyuan (192x192, a frame of his own): a towering immortal on a soft cloud of round puffs, a
     * narrow waist under a jade belt and robes belling out below it (tiers, folded panels front, sides
     * and back, a wide sea-and-cliff hem, a train), a cloud collar, great billowing sleeves (their
     * mouths dark with stars), a
     * lotus crown and jade pin on his white topknot, long brows, beard and hair, a ribbon drifting from
     * his shoulders, a ring of the eight trigrams behind his head, his whisk in his right hand. Model
     * and skin: {@code assets_source/zhenyuan.py}.
     */
    public static LayerDefinition zhenyuanLayer() {
        // GENERATED zhenyuan: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition cloud = root.addOrReplaceChild("cloud", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 22.4f, 0, 0, 0, 0));
        PartDefinition figure = root.addOrReplaceChild("figure", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -4, 0, 0, 0, 0));
        PartDefinition body = figure.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 50).mirror(false).addBox(-4.5f, 0, -2.7f, 9, 9, 5.4f)
                .texOffs(32, 66).mirror(false).addBox(-5.6f, -0.4f, -3.2f, 11.2f, 3.8f, 6.4f)
                .texOffs(93, 66).mirror(false).addBox(-3.8f, 8.6f, -2.3f, 7.6f, 4.4f, 4.6f)
                .texOffs(47, 79).mirror(false).addBox(-4.3f, 12.4f, -2.7f, 8.6f, 2, 5.4f)
                .texOffs(91, 50).mirror(false).addBox(-5, 14, -3.2f, 10, 7, 6.4f)
                .texOffs(122, 30).mirror(false).addBox(-6.2f, 20.5f, -3.9f, 12.4f, 7.2f, 7.8f)
                .texOffs(19, 30).mirror(false).addBox(-7.4f, 27.2f, -4.6f, 14.8f, 6.6f, 9.2f)
                .texOffs(126, 50).mirror(false).addBox(-8.6f, 33.4f, -5.4f, 17.2f, 1.8f, 10.8f)
                .texOffs(182, 79).mirror(false).addBox(2.4f, 14.6f, -3.6f, 1.4f, 3, 0.4f), PartPose.offsetAndRotation(0, -10, 0, 0, 0, 0));
        PartDefinition apron = body.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(127, 0).mirror(false).addBox(-2.1f, 0, -0.5f, 4.2f, 19, 0.5f), PartPose.offsetAndRotation(0, 14.2f, -3.3f, -0.13f, 0, 0));
        PartDefinition rightSash = body.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(120, 66).mirror(false).addBox(-0.7f, 0, -0.2f, 1.4f, 9, 0.4f), PartPose.offsetAndRotation(-1.6f, 13.6f, -2.9f, -0.08f, 0, 0.05f));
        PartDefinition leftSash = body.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(120, 66).mirror(true).addBox(-0.7f, 0, -0.2f, 1.4f, 9, 0.4f), PartPose.offsetAndRotation(1.6f, 13.6f, -2.9f, -0.08f, 0, -0.05f));
        PartDefinition rightFrontPanel = body.addOrReplaceChild("right_front_panel", CubeListBuilder.create()
                .texOffs(17, 0).mirror(false).addBox(-4.4f, 0, -0.35f, 4.4f, 22, 0.7f)
                .texOffs(53, 0).mirror(false).addBox(-3.4f, 1, -0.75f, 0.8f, 20.5f, 0.45f)
                .texOffs(81, 0).mirror(false).addBox(-1.6f, 2, -0.75f, 0.8f, 19.5f, 0.45f), PartPose.offsetAndRotation(-2.4f, 13, -3.4f, -0.14f, 0.22f, 0.12f));
        PartDefinition leftFrontPanel = body.addOrReplaceChild("left_front_panel", CubeListBuilder.create()
                .texOffs(17, 0).mirror(true).addBox(0, 0, -0.35f, 4.4f, 22, 0.7f)
                .texOffs(53, 0).mirror(true).addBox(2.6f, 1, -0.75f, 0.8f, 20.5f, 0.45f)
                .texOffs(81, 0).mirror(true).addBox(0.8f, 2, -0.75f, 0.8f, 19.5f, 0.45f), PartPose.offsetAndRotation(2.4f, 13, -3.4f, -0.14f, -0.22f, -0.12f));
        PartDefinition rightSidePanel = body.addOrReplaceChild("right_side_panel", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.35f, 0, -3.5f, 0.7f, 22, 7)
                .texOffs(81, 0).mirror(false).addBox(-0.75f, 1.5f, -1.4f, 0.45f, 20, 0.8f)
                .texOffs(81, 0).mirror(false).addBox(-0.75f, 1.5f, 1.4f, 0.45f, 20, 0.8f), PartPose.offsetAndRotation(-5.2f, 13, 0, 0, 0, 0.22f));
        PartDefinition leftSidePanel = body.addOrReplaceChild("left_side_panel", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(-0.35f, 0, -3.5f, 0.7f, 22, 7)
                .texOffs(81, 0).mirror(true).addBox(0.3f, 1.5f, -1.4f, 0.45f, 20, 0.8f)
                .texOffs(81, 0).mirror(true).addBox(0.3f, 1.5f, 1.4f, 0.45f, 20, 0.8f), PartPose.offsetAndRotation(5.2f, 13, 0, 0, 0, -0.22f));
        PartDefinition backPanel = body.addOrReplaceChild("back_panel", CubeListBuilder.create()
                .texOffs(30, 0).mirror(false).addBox(-5, 0, -0.35f, 10, 22, 0.7f)
                .texOffs(53, 0).mirror(false).addBox(-2.8f, 1, 0.3f, 0.8f, 20.5f, 0.45f)
                .texOffs(53, 0).mirror(false).addBox(2, 1, 0.3f, 0.8f, 20.5f, 0.45f), PartPose.offsetAndRotation(0, 13, 3.2f, 0.14f, 0, 0));
        PartDefinition train = body.addOrReplaceChild("train", CubeListBuilder.create()
                .texOffs(0, 79).mirror(false).addBox(-7.4f, -0.6f, -0.4f, 14.8f, 0.8f, 8), PartPose.offsetAndRotation(0, 34.4f, 5, -0.32f, 0, 0));
        PartDefinition halo = body.addOrReplaceChild("halo", CubeListBuilder.create()
                .texOffs(140, 0).mirror(false).addBox(-9, -9, 0, 18, 18, 1), PartPose.offsetAndRotation(0, -7, 6.2f, 0, 0, 0));
        PartDefinition hair = body.addOrReplaceChild("hair", CubeListBuilder.create()
                .texOffs(0, 30).mirror(false).addBox(-3.8f, 0, -0.2f, 7.6f, 18, 1), PartPose.offsetAndRotation(0, -6.5f, 4.5f, 0.06f, 0, 0));
        PartDefinition hairEnd = hair.addOrReplaceChild("hair_end", CubeListBuilder.create()
                .texOffs(125, 79).mirror(false).addBox(-3, 0, -0.2f, 6, 6, 0.8f), PartPose.offsetAndRotation(0, 18, 0, 0.05f, 0, 0));
        PartDefinition rightRibbon = body.addOrReplaceChild("right_ribbon", CubeListBuilder.create()
                .texOffs(71, 66).mirror(false).addBox(-1.2f, 0, -0.2f, 2.4f, 10, 0.4f), PartPose.offsetAndRotation(-5.4f, 1.5f, 2.8f, 0.55f, 0, 0.45f));
        PartDefinition leftRibbon = body.addOrReplaceChild("left_ribbon", CubeListBuilder.create()
                .texOffs(71, 66).mirror(true).addBox(-1.2f, 0, -0.2f, 2.4f, 10, 0.4f), PartPose.offsetAndRotation(5.4f, 1.5f, 2.8f, 0.55f, 0, -0.45f));
        PartDefinition rightRibbonEnd = rightRibbon.addOrReplaceChild("right_ribbon_end", CubeListBuilder.create()
                .texOffs(0, 66).mirror(false).addBox(-1.2f, 0, -0.2f, 2.4f, 11, 0.4f), PartPose.offsetAndRotation(0, 10, 0, -0.5f, 0, -0.3f));
        PartDefinition leftRibbonEnd = leftRibbon.addOrReplaceChild("left_ribbon_end", CubeListBuilder.create()
                .texOffs(0, 66).mirror(true).addBox(-1.2f, 0, -0.2f, 2.4f, 11, 0.4f), PartPose.offsetAndRotation(0, 10, 0, -0.5f, 0, 0.3f));
        PartDefinition head = figure.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(70, 30).mirror(false).addBox(-4, -9, -4, 8, 9, 8)
                .texOffs(31, 50).mirror(false).addBox(-4.3f, -9.3f, -0.5f, 8.6f, 9.6f, 4.8f)
                .texOffs(78, 79).mirror(false).addBox(-1.6f, -12, -1.6f, 3.2f, 3.2f, 3.2f)
                .texOffs(95, 79).mirror(false).addBox(-2.6f, -11.2f, -2.6f, 5.2f, 1.6f, 5.2f)
                .texOffs(52, 89).mirror(false).addBox(-5, -11.6f, -0.4f, 10, 0.8f, 0.8f)
                .texOffs(0, 89).mirror(false).addBox(-0.7f, -13.4f, -0.7f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, -10, 0, 0, 0, 0));
        PartDefinition crownFront = head.addOrReplaceChild("crown_front", CubeListBuilder.create()
                .texOffs(158, 79).mirror(false).addBox(-1.6f, -4.4f, -0.3f, 3.2f, 4.4f, 0.6f), PartPose.offsetAndRotation(0, -11, -2.6f, -0.3f, 0, 0));
        PartDefinition crownBack = head.addOrReplaceChild("crown_back", CubeListBuilder.create()
                .texOffs(158, 79).mirror(false).addBox(-1.6f, -4.4f, -0.3f, 3.2f, 4.4f, 0.6f), PartPose.offsetAndRotation(0, -11, 2.6f, 0.3f, 0, 0));
        PartDefinition rightPetal = head.addOrReplaceChild("right_petal", CubeListBuilder.create()
                .texOffs(140, 79).mirror(false).addBox(-0.3f, -3.8f, -1.4f, 0.6f, 3.8f, 2.8f), PartPose.offsetAndRotation(-2.6f, -11, 0, 0, 0, -0.32f));
        PartDefinition leftPetal = head.addOrReplaceChild("left_petal", CubeListBuilder.create()
                .texOffs(140, 79).mirror(true).addBox(-0.3f, -3.8f, -1.4f, 0.6f, 3.8f, 2.8f), PartPose.offsetAndRotation(2.6f, -11, 0, 0, 0, 0.32f));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(75, 89).mirror(false).addBox(-4, -0.5f, -0.3f, 4, 1, 0.6f), PartPose.offsetAndRotation(-2.2f, -5.6f, -4.2f, 0, 0, -0.55f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(75, 89).mirror(true).addBox(0, -0.5f, -0.3f, 4, 1, 0.6f), PartPose.offsetAndRotation(2.2f, -5.6f, -4.2f, 0, 0, 0.55f));
        PartDefinition rightWhisker = head.addOrReplaceChild("right_whisker", CubeListBuilder.create()
                .texOffs(120, 79).mirror(false).addBox(-0.5f, 0, -0.3f, 1, 7, 0.6f), PartPose.offsetAndRotation(-1.8f, -2.6f, -4.3f, -0.1f, 0, 0.18f));
        PartDefinition leftWhisker = head.addOrReplaceChild("left_whisker", CubeListBuilder.create()
                .texOffs(120, 79).mirror(true).addBox(-0.5f, 0, -0.3f, 1, 7, 0.6f), PartPose.offsetAndRotation(1.8f, -2.6f, -4.3f, -0.1f, 0, -0.18f));
        PartDefinition beard = head.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(127, 66).mirror(false).addBox(-2.6f, 0, -0.7f, 5.2f, 8, 1.4f), PartPose.offsetAndRotation(0, -1.6f, -4.3f, 0.1f, 0, 0));
        PartDefinition beardTip = beard.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(144, 66).mirror(false).addBox(-1.8f, 0, -0.6f, 3.6f, 8, 1.2f), PartPose.offsetAndRotation(0, 8, 0, -0.04f, 0, 0));
        PartDefinition rightArm = figure.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(103, 30).mirror(false).addBox(-3.2f, -1.6f, -2.4f, 4, 12, 4.8f)
                .texOffs(169, 79).mirror(false).addBox(-2.6f, 10.4f, -1.4f, 2.6f, 2.4f, 2.8f), PartPose.offsetAndRotation(-6, -8, 0, 0, 0, 0));
        PartDefinition leftArm = figure.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(103, 30).mirror(true).addBox(-0.8f, -1.6f, -2.4f, 4, 12, 4.8f)
                .texOffs(169, 79).mirror(true).addBox(0, 10.4f, -1.4f, 2.6f, 2.4f, 2.8f), PartPose.offsetAndRotation(6, -8, 0, 0, 0, 0));
        PartDefinition rightSleeve = rightArm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(60, 50).mirror(false).addBox(-3.5f, 0, -3.5f, 7, 7, 8)
                .texOffs(86, 0).mirror(false).addBox(-4.6f, 6.4f, -4.4f, 9.2f, 10.6f, 9.8f)
                .texOffs(18, 89).mirror(false).addBox(-5.1f, 15.8f, -4.9f, 10.2f, 1.4f, 0.6f)
                .texOffs(18, 89).mirror(false).addBox(-5.1f, 15.8f, 5.3f, 10.2f, 1.4f, 0.6f)
                .texOffs(9, 66).mirror(false).addBox(-5.1f, 15.8f, -4.3f, 0.6f, 1.4f, 9.6f)
                .texOffs(9, 66).mirror(false).addBox(4.5f, 15.8f, -4.3f, 0.6f, 1.4f, 9.6f), PartPose.offsetAndRotation(-1.2f, 3, 1.2f, 0, 0, 0));
        PartDefinition leftSleeve = leftArm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(60, 50).mirror(true).addBox(-3.5f, 0, -3.5f, 7, 7, 8)
                .texOffs(86, 0).mirror(true).addBox(-4.6f, 6.4f, -4.4f, 9.2f, 10.6f, 9.8f)
                .texOffs(18, 89).mirror(true).addBox(-5.1f, 15.8f, -4.9f, 10.2f, 1.4f, 0.6f)
                .texOffs(18, 89).mirror(true).addBox(-5.1f, 15.8f, 5.3f, 10.2f, 1.4f, 0.6f)
                .texOffs(9, 66).mirror(true).addBox(4.5f, 15.8f, -4.3f, 0.6f, 1.4f, 9.6f)
                .texOffs(9, 66).mirror(true).addBox(-5.1f, 15.8f, -4.3f, 0.6f, 1.4f, 9.6f), PartPose.offsetAndRotation(1.2f, 3, 1.2f, 0, 0, 0));
        PartDefinition rightSleeveFlap = rightSleeve.addOrReplaceChild("right_sleeve_flap", CubeListBuilder.create()
                .texOffs(58, 0).mirror(false).addBox(-0.4f, 0, -4.6f, 0.8f, 11.5f, 9.4f), PartPose.offsetAndRotation(-4.6f, 6, 0.4f, 0, 0, 0.2f));
        PartDefinition leftSleeveFlap = leftSleeve.addOrReplaceChild("left_sleeve_flap", CubeListBuilder.create()
                .texOffs(58, 0).mirror(true).addBox(-0.4f, 0, -4.6f, 0.8f, 11.5f, 9.4f), PartPose.offsetAndRotation(4.6f, 6, 0.4f, 0, 0, -0.2f));
        PartDefinition whisk = rightArm.addOrReplaceChild("whisk", CubeListBuilder.create()
                .texOffs(157, 66).mirror(false).addBox(-0.5f, -7, -0.5f, 1, 9, 1)
                .texOffs(9, 89).mirror(false).addBox(-0.8f, -8, -0.8f, 1.6f, 1.2f, 1.6f)
                .texOffs(43, 89).mirror(false).addBox(-0.7f, 2, -0.7f, 1.4f, 1, 1.4f), PartPose.offsetAndRotation(-1.3f, 11.8f, -0.6f, -0.5f, 0, 0));
        PartDefinition whiskTail = whisk.addOrReplaceChild("whisk_tail", CubeListBuilder.create()
                .texOffs(80, 66).mirror(false).addBox(-1.1f, 0, -1.1f, 2.2f, 8, 2.2f)
                .texOffs(149, 79).mirror(false).addBox(-0.7f, 8, -0.7f, 1.4f, 5, 1.4f), PartPose.offsetAndRotation(0, -7.6f, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 192, 192);
        // GENERATED zhenyuan: end
    }

    /** The robed frame's pose: legs under the robe. */
    public static class RobedModel<T extends Mob> extends HumanoidModel<T> {
        public RobedModel(ModelPart root) {
            super(root);
            leftLeg.visible = false;
            rightLeg.visible = false;
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            super.setupAnim(e, swing * 0.5f, amount, age, yaw, pitch);
        }
    }

    public static final class ZhenyuanModel extends EntityModel<ZhenyuanEntity> {
        private final ModelPart root, cloud, figure, body, head, halo, train, hair, hairEnd, beard, beardTip,
                rightRibbon, rightRibbonEnd, leftRibbon, leftRibbonEnd, rightArm, leftArm, rightSleeve, leftSleeve, whisk, tail,
                apron, rightFront, leftFront, rightSide, leftSide, back, rightFlap, leftFlap;
        /** The age the model was last posed at, for the cloud's drift. */
        private float age;

        public ZhenyuanModel(ModelPart root) {
            this.root = root;
            cloud = root.getChild("cloud");
            figure = root.getChild("figure");
            body = figure.getChild("body");
            head = figure.getChild("head");
            halo = body.getChild("halo");
            train = body.getChild("train");
            hair = body.getChild("hair");
            hairEnd = hair.getChild("hair_end");
            beard = head.getChild("beard");
            beardTip = beard.getChild("beard_tip");
            rightRibbon = body.getChild("right_ribbon");
            rightRibbonEnd = rightRibbon.getChild("right_ribbon_end");
            leftRibbon = body.getChild("left_ribbon");
            leftRibbonEnd = leftRibbon.getChild("left_ribbon_end");
            rightArm = figure.getChild("right_arm");
            leftArm = figure.getChild("left_arm");
            rightSleeve = rightArm.getChild("right_sleeve");
            leftSleeve = leftArm.getChild("left_sleeve");
            whisk = rightArm.getChild("whisk");
            tail = whisk.getChild("whisk_tail");
            apron = body.getChild("apron");
            rightFront = body.getChild("right_front_panel");
            leftFront = body.getChild("left_front_panel");
            rightSide = body.getChild("right_side_panel");
            leftSide = body.getChild("left_side_panel");
            back = body.getChild("back_panel");
            rightFlap = rightSleeve.getChild("right_sleeve_flap");
            leftFlap = leftSleeve.getChild("left_sleeve_flap");
        }

        /** A part turned this much from its rest. */
        private static void turn(ModelPart p, float x, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.zRot = p.getInitialPose().zRot + z;
        }

        @Override
        public void setupAnim(ZhenyuanEntity e, float swing, float amount, float age, float yaw, float pitch) {
            this.age = age;
            // he stands on his cloud, rising and falling a little over it; it turns slowly under him
            figure.y = figure.getInitialPose().y - 0.6f - Mth.sin(age * 0.06f) * 0.6f;
            cloud.y = 22.4f + Mth.sin(age * 0.06f + 1) * 0.25f;
            cloud.yRot = age * 0.01f;
            halo.zRot = age * 0.008f;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            // gliding, he leans a little into it, and all that hangs from him streams back
            body.xRot = amount * 0.1f;
            train.xRot = -0.32f - amount * 0.3f + Mth.sin(age * 0.05f) * 0.04f;
            hair.xRot = 0.06f + amount * 0.25f + Mth.sin(age * 0.05f) * 0.03f;
            hairEnd.xRot = 0.05f + amount * 0.2f + Mth.sin(age * 0.05f + 0.8f) * 0.06f;
            // the robe's panels swing a little, and stream back as he glides; the sleeves' loose folds billow
            float sway = Mth.sin(age * 0.05f);
            turn(rightFront, -amount * 0.12f + sway * 0.02f, sway * 0.02f);
            turn(leftFront, -amount * 0.12f - sway * 0.02f, sway * 0.02f);
            turn(rightSide, amount * 0.15f, Mth.sin(age * 0.05f + 1) * 0.03f);
            turn(leftSide, amount * 0.15f, -Mth.sin(age * 0.05f + 1) * 0.03f);
            turn(back, amount * 0.3f + Mth.sin(age * 0.05f + 2) * 0.02f, 0);
            turn(apron, -amount * 0.1f + Mth.sin(age * 0.06f) * 0.02f, 0);
            turn(rightFlap, 0, Mth.sin(age * 0.07f) * 0.08f);
            turn(leftFlap, 0, -Mth.sin(age * 0.07f + 1.2f) * 0.08f);
            beard.xRot = 0.1f + Mth.sin(age * 0.045f) * 0.03f;
            beardTip.xRot = -0.04f + Mth.sin(age * 0.045f + 0.7f) * 0.05f;
            rightRibbon.xRot = leftRibbon.xRot = 0.8f + amount * 0.4f + Mth.sin(age * 0.07f) * 0.1f;
            rightRibbon.zRot = 0.7f + Mth.sin(age * 0.05f) * 0.08f;
            leftRibbon.zRot = -0.7f - Mth.sin(age * 0.05f + 1.5f) * 0.08f;
            rightRibbonEnd.xRot = leftRibbonEnd.xRot = -0.5f + Mth.sin(age * 0.07f + 1) * 0.25f;
            // the whisk held up before him, its horsetail hanging over and swaying; a stroke on a blow
            float stroke = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            rightArm.xRot = -0.7f + Mth.cos(age * 0.07f) * 0.05f - stroke * 0.9f;
            rightArm.yRot = -0.25f + stroke * 0.8f;
            rightArm.zRot = 0.1f;
            tail.xRot = -(rightArm.xRot + whisk.xRot) + 0.15f + Mth.sin(age * 0.08f) * 0.06f + stroke * 0.7f;
            hang(rightSleeve, rightArm, age);
            leftSleeve.xScale = leftSleeve.yScale = leftSleeve.zScale = 1f;
            if (e.sleeveRaised()) {
                // his left sleeve flung open toward his foe, its mouth swelling: the Universe in his Sleeve
                leftArm.xRot = -1.3f;
                leftArm.yRot = 0.4f;
                leftArm.zRot = -0.1f;
                leftSleeve.xRot = -0.1f;
                leftSleeve.zRot = 0;
                float swell = 1.6f + Mth.sin(age * 0.6f) * 0.12f;
                leftSleeve.xScale = swell;
                leftSleeve.zScale = swell;
                leftSleeve.yScale = 1.15f;
                body.xRot -= 0.08f;
            } else {
                leftArm.xRot = -0.35f + Mth.cos(age * 0.05f) * 0.04f;
                leftArm.yRot = 0.1f;
                leftArm.zRot = -0.12f;
                hang(leftSleeve, leftArm, age + 2);
            }
        }

        /** A great sleeve hanging from the forearm, swaying: its weight keeps it down whatever the arm does. */
        private static void hang(ModelPart sleeve, ModelPart arm, float age) {
            sleeve.xRot = -arm.xRot * 0.85f + Mth.sin(age * 0.05f) * 0.05f;
            sleeve.zRot = -arm.zRot * 0.8f;
        }

        /** All of him but the halo (drawn translucent, by {@link #renderVeils}); the cloud is {@link #renderCloud}'s. */
        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            halo.visible = false;
            root.render(pose, vc, light, overlay, color);
            halo.visible = true;
        }

        /** The halo behind his head: soft, its edges thinning away. */
        public void renderVeils(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            pose.pushPose();
            root.translateAndRotate(pose);
            figure.translateAndRotate(pose);
            body.translateAndRotate(pose);
            halo.render(pose, vc, light, overlay, color);
            pose.popPose();
        }

        /**
         * The cloud's puffs: x, y, z (their middles, in model pixels about the cloud's pivot), radius,
         * sprite (of {@code zhenyuan_cloud}'s: two big heaped puffs, a small lumpy one). Big ones at its
         * heart, a ring about them, smaller ones at its rim, a few heaped on top about his feet.
         */
        private static final float[][] PUFFS = puffs();

        private static float[][] puffs() {
            java.util.List<float[]> out = new java.util.ArrayList<>();
            float[][] heart = {{0, -2, 0.5f}, {-5.5f, -1.4f, -3.5f}, {5, -1.5f, 3.5f}, {-4, -1.2f, 5.5f}, {5, -1.3f, -4.5f}};
            for (int i = 0; i < heart.length; i++) out.add(new float[]{heart[i][0], heart[i][1], heart[i][2], 11.5f, i % 2});
            for (int i = 0; i < 8; i++) {                       // the ring, every 45 degrees
                double a = i * Math.PI / 4 + 0.2;
                out.add(new float[]{(float) Math.cos(a) * 10, -0.4f + (i % 3) * 0.3f, (float) Math.sin(a) * 10, 9 - (i % 3) * 0.6f, (i + 1) % 2});
            }
            for (int i = 0; i < 9; i++) {                       // the rim, smaller, offset from the ring
                double a = i * Math.PI * 2 / 9 + 0.55;
                float r = 12.5f + (i % 2) * 1.2f;
                out.add(new float[]{(float) Math.cos(a) * r, -0.2f, (float) Math.sin(a) * r, 7.5f + (i % 3) * 0.5f, i % 3 == 0 ? 1 : 2});
            }
            float[][] top = {{-3.5f, -3.6f, 2.5f}, {3.5f, -3.4f, -2}, {-1.5f, -3.8f, -4.5f}, {2.5f, -3.3f, 4.5f}};
            for (int i = 0; i < top.length; i++) out.add(new float[]{top[i][0], top[i][1], top[i][2], 6.5f, i % 2 == 0 ? 2 : 1});
            return out.toArray(new float[0][]);
        }

        /**
         * The small cloud he stands on, as soft puffs rather than boxes: each a round sprite turned always
         * to the eye (a little squat: a cloud's billows are broader than they are high) (so it never shows a cube's grey side), heaped and overlapping, drifting a little
         * and swelling and easing; flat sheets of mist turning slowly about its foot. All lit evenly
         * (full-bright, every vertex's normal straight up, so no face is shaded darker than another),
         * whiter on top, faintly lilac beneath.
         */
        public void renderCloud(PoseStack pose, VertexConsumer vc) {
            pose.pushPose();
            root.translateAndRotate(pose);
            cloud.translateAndRotate(pose);
            Matrix4f m = pose.last().pose();
            // the eye's right and up, brought into the cloud's own space
            Quaternionf eye = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
            Matrix3f toLocal = m.get3x3(new Matrix3f()).invert();
            Vector3f right = toLocal.transform(eye.transform(new Vector3f(1, 0, 0))).normalize();
            Vector3f up = toLocal.transform(eye.transform(new Vector3f(0, 1, 0))).normalize();
            // the mist about its foot, laid flat, two sheets turning opposite ways
            for (int i = 0; i < 2; i++) {
                float a = age * (i == 0 ? 0.004f : -0.006f) + i;
                float r = (i == 0 ? 19 : 15) / 16f;
                Vector3f ax = new Vector3f(Mth.cos(a) * r, 0, Mth.sin(a) * r), az = new Vector3f(-Mth.sin(a) * r, 0, Mth.cos(a) * r);
                quad(vc, m, new Vector3f(0, (1.2f + i * 0.5f) / 16f, 0), ax, az, 3, 150, 150);
            }
            for (int i = 0; i < PUFFS.length; i++) {
                float[] p = PUFFS[i];
                float ph = i * 1.7f;
                // each puff drifts a little about its place, and swells and eases
                float x = p[0] + Mth.sin(age * 0.031f + ph) * 0.6f;
                float y = p[1] + Mth.sin(age * 0.043f + ph * 2) * 0.3f;
                float z = p[2] + Mth.cos(age * 0.027f + ph * 1.3f) * 0.6f;
                float r = p[3] * (1 + Mth.sin(age * 0.05f + ph) * 0.04f) / 16f;
                quad(vc, m, new Vector3f(x / 16f, y / 16f, z / 16f), new Vector3f(right).mul(r), new Vector3f(up).mul(r * 0.66f),
                        (int) p[4], 255, 255);
            }
            pose.popPose();
        }

        /** A quad about {@code c} spanning ±a and ±b, showing one sprite; its +b edge whiter. */
        private static void quad(VertexConsumer vc, Matrix4f m, Vector3f c, Vector3f a, Vector3f b, int sprite, int alphaTop, int alphaBottom) {
            float u0 = (sprite % 2) * 0.5f, v0 = (sprite / 2) * 0.5f;
            vertex(vc, m, c.x - a.x + b.x, c.y - a.y + b.y, c.z - a.z + b.z, 255, 255, 255, alphaTop, u0, v0);
            vertex(vc, m, c.x - a.x - b.x, c.y - a.y - b.y, c.z - a.z - b.z, 232, 234, 248, alphaBottom, u0, v0 + 0.5f);
            vertex(vc, m, c.x + a.x - b.x, c.y + a.y - b.y, c.z + a.z - b.z, 232, 234, 248, alphaBottom, u0 + 0.5f, v0 + 0.5f);
            vertex(vc, m, c.x + a.x + b.x, c.y + a.y + b.y, c.z + a.z + b.z, 255, 255, 255, alphaTop, u0 + 0.5f, v0);
        }

        private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, int r, int g, int b, int a, float u, float v) {
            Vector3f q = m.transformPosition(x, y, z, new Vector3f());
            vc.addVertex(q.x, q.y, q.z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
        }
    }

    // ── Renderers ───────────────────────────────────────────────────────

    public static final class DiscipleRenderer extends HumanoidMobRenderer<DaoistDiscipleEntity, RobedModel<DaoistDiscipleEntity>> {
        private static final ResourceLocation SKIN = tex("daoist_disciple"), CAPTAIN = tex("daoist_disciple_captain");

        public DiscipleRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new RobedModel<>(ctx.bakeLayer(DISCIPLE_LAYER)), 0.45f);
        }

        @Override
        public ResourceLocation getTextureLocation(DaoistDiscipleEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected boolean shouldShowName(DaoistDiscipleEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    public static final class YouthRenderer<T extends TempleYouthEntity> extends HumanoidMobRenderer<T, RobedModel<T>> {
        public static final float SIZE = 0.86f;
        private final ResourceLocation skin;

        public YouthRenderer(EntityRendererProvider.Context ctx, String name) {
            super(ctx, new RobedModel<>(ctx.bakeLayer(YOUTH_LAYER)), 0.35f);
            skin = tex(name);
        }

        public static <T extends TempleYouthEntity> EntityRendererProvider<T> of(String name) {
            return ctx -> new YouthRenderer<>(ctx, name);
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

    public static final class ZhenyuanRenderer extends MobRenderer<ZhenyuanEntity, ZhenyuanModel> {
        /**
         * How much larger than his pixels he is drawn: twice a man's height, some 3.5 blocks to the top of
         * his head and 3.85 to his crown's pearl (zhenyuan.py prints his reach; ModEntities sizes him to match).
         */
        public static final float SIZE = 1.2f;
        private static final ResourceLocation SKIN = tex("zhenyuan");
        /** The halo, whose edges thin away. */
        private static final RenderType VEILS = RenderType.entityTranslucent(SKIN);
        /** The cloud's soft puffs (zhenyuan.py's cloud_sprites). */
        private static final RenderType CLOUD = RenderType.entityTranslucent(tex("zhenyuan_cloud"));
        /** His eyes, the halo's gold, the stars in his sleeve: dim, drawn full-bright. */
        private static final RenderType GLOW = RenderType.eyes(tex("zhenyuan_glow"));

        public ZhenyuanRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ZhenyuanModel(ctx.bakeLayer(ZHENYUAN_LAYER)), 1.0f);
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, ZhenyuanEntity e,
                                   float swing, float amount, float partial, float age, float yaw, float pitch) {
                    if (e.isInvisible()) return;
                    getParentModel().renderCloud(pose, buffers.getBuffer(CLOUD));
                    getParentModel().renderVeils(pose, buffers.getBuffer(VEILS), light,
                            LivingEntityRenderer.getOverlayCoords(e, 0), -1);
                    VertexConsumer glow = buffers.getBuffer(GLOW);
                    getParentModel().renderToBuffer(pose, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
                    getParentModel().renderVeils(pose, glow, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        @Override
        protected void scale(ZhenyuanEntity e, PoseStack pose, float partial) {
            pose.scale(SIZE, SIZE, SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(ZhenyuanEntity e) {
            return SKIN;
        }
    }

    // ── The cloud crane ─────────────────────────────────────────────────

    /** A crane (64x64): body, two-jointed neck, head with crown and beak, wings, tail, long legs. */
    public static LayerDefinition craneLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -3.5f, -7, 7, 7, 14),
                PartPose.offsetAndRotation(0, 10, 0, -0.12f, 0, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 22).addBox(-1, -8, -1, 2, 8, 2),
                PartPose.offsetAndRotation(0, -2, -6, 0.45f, 0, 0));
        PartDefinition upper = neck.addOrReplaceChild("upper_neck", CubeListBuilder.create().texOffs(8, 22).addBox(-1, -6, -1, 2, 6, 2),
                PartPose.offsetAndRotation(0, -8, 0, -0.6f, 0, 0));
        PartDefinition head = upper.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 22).addBox(-1.5f, -2.5f, -3, 3, 3, 4),
                PartPose.offsetAndRotation(0, -6, 0, 0.15f, 0, 0));
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(30, 22).addBox(-1, -3, -2, 2, 1, 2), PartPose.ZERO);
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(36, 22).addBox(-0.5f, -1.5f, -8, 1, 1, 5), PartPose.ZERO);
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 32).addBox(-14, -0.5f, -2, 14, 1, 10),
                PartPose.offset(-3.5f, -2.5f, -3));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 44).mirror().addBox(0, -0.5f, -2, 14, 1, 10),
                PartPose.offset(3.5f, -2.5f, -3));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(42, 0).addBox(-2.5f, -1, 0, 5, 2, 5),
                PartPose.offsetAndRotation(0, -1, 6.5f, 0.35f, 0, 0));
        for (int side : new int[]{-1, 1}) {
            PartDefinition leg = root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create().texOffs(50, 32)
                    .addBox(-0.5f, 0, -0.5f, 1, 11, 1), PartPose.offset(side * 1.5f, 13, 1));
            leg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(54, 44).addBox(-1, 10.5f, -2.5f, 2, 1, 3), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static final class CraneModel extends EntityModel<CloudCraneEntity> {
        private final ModelPart root, body, neck, head, rightWing, leftWing, rightLeg, leftLeg;

        public CraneModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            neck = body.getChild("neck");
            head = neck.getChild("upper_neck").getChild("head");
            rightWing = body.getChild("right_wing");
            leftWing = body.getChild("left_wing");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(CloudCraneEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            neck.yRot = yaw * Mth.DEG_TO_RAD * 0.3f;
            boolean flying = !e.onGround();
            if (e.isSwooping()) {
                // wings swept back, neck and legs stretched out
                rightWing.zRot = 0.15f;
                leftWing.zRot = -0.15f;
                rightWing.yRot = -0.75f;
                leftWing.yRot = 0.75f;
                neck.xRot = 1.25f;
                body.xRot = 0.35f;
                rightLeg.xRot = leftLeg.xRot = 1.4f;
            } else if (flying) {
                float flap = Mth.sin(age * 0.45f) * 0.75f;
                rightWing.zRot = flap;
                leftWing.zRot = -flap;
                rightWing.yRot = leftWing.yRot = 0;
                neck.xRot = 0.95f;
                body.xRot = 0.05f;
                rightLeg.xRot = leftLeg.xRot = 1.35f;
            } else {
                // folded along its sides; it stalks
                rightWing.zRot = -1.42f;
                leftWing.zRot = 1.42f;
                rightWing.yRot = leftWing.yRot = 0;
                neck.xRot = 0.45f + Mth.sin(age * 0.05f) * 0.05f;
                body.xRot = -0.12f;
                rightLeg.xRot = Mth.cos(swing * 0.6f) * 0.8f * amount;
                leftLeg.xRot = Mth.cos(swing * 0.6f + Mth.PI) * 0.8f * amount;
            }
            if (attackTime > 0) neck.xRot += Mth.sin(attackTime * Mth.PI) * 0.8f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class CraneRenderer extends MobRenderer<CloudCraneEntity, CraneModel> {
        private static final ResourceLocation SKIN = tex("cloud_crane"), CAPTAIN = tex("cloud_crane_captain");

        public CraneRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new CraneModel(ctx.bakeLayer(CRANE_LAYER)), 0.45f);
        }

        @Override
        public ResourceLocation getTextureLocation(CloudCraneEntity e) {
            return e.isCaptain() ? CAPTAIN : SKIN;
        }

        @Override
        protected boolean shouldShowName(CloudCraneEntity e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }
}
