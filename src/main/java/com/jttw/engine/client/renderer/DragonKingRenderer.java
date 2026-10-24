package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.waters.DragonKingEntity;
import com.jttw.engine.world.waters.Sea;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A Dragon King (龙王), as the temples carve him: a tall man's shape in an emperor's dragon robe, and a
 * dragon's head. The head long, in steps tapering to a high flared nose, the jaws open on fangs,
 * gold barbels trailing from the lip, flames of brow and of cheek-hair swept back, branching antlers,
 * a spined crest and a mane down the neck, a beard; eyes smouldering deep under a scowling brow. The
 * robe to the floor: a cloud collar over gold-edged scale pauldrons, a dragon on the breast, a hooped
 * jade belt and its pendants, an apron panel, the sea-and-cliff hem (海水江崖); wide sleeves hanging
 * from his forearms, scaled taloned hands from their mouths, clawed feet and the tail from under the
 * hem. One model for the four, each in his sea's colours with his own crown, beard and regalia:
 * <ul>
 *   <li>East, Ao Guang, the eldest and drawn largest: blue-green, the emperor's mian crown hung with
 *       beads, a long white beard, the court tablet (笏);</li>
 *   <li>South, Ao Qin: red, a gold crown of flames round a ruby, a forked beard, the flaming pearl;</li>
 *   <li>West, Ao Run: white, the black winged official's hat, a pointed silver beard and moustache,
 *       a jade ruyi;</li>
 *   <li>North, Ao Shun: black, an iron crown of icicles, a long parted beard, a sword and the frost pearl.</li>
 * </ul>
 * His sleeves, beard, mane and barbels drift in the water. Model and skins: {@code assets_source/sea_court.py}.
 */
public final class DragonKingRenderer extends MobRenderer<DragonKingEntity, DragonKingRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("dragon_king"), "main");
    private static final Map<Sea, ResourceLocation> SKINS = new EnumMap<>(Sea.class), GLOWS = new EnumMap<>(Sea.class);

    static {
        for (Sea sea : Sea.values()) {
            SKINS.put(sea, JTTWEngine.id("textures/entity/" + sea.king + ".png"));
            GLOWS.put(sea, JTTWEngine.id("textures/entity/" + sea.king + "_glow.png"));
        }
    }

    public DragonKingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.8f);
        // his eyes and his crown's pearl, full-bright even in the deep
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, DragonKingEntity e,
                               float swing, float amount, float partial, float age, float yaw, float pitch) {
                VertexConsumer vc = buffers.getBuffer(RenderType.eyes(GLOWS.get(e.sea())));
                getParentModel().renderToBuffer(pose, vc, 0xF00000, OverlayTexture.NO_OVERLAY, -1);
            }
        });
    }

    /** How much larger than its pixels he is drawn (some 3.4 blocks to his crown), the East's eldest king the more. */
    public static final float SCALE = 1.0f, EAST_SCALE = 1.1f;

    @Override
    protected void scale(DragonKingEntity e, PoseStack pose, float partial) {
        float k = e.sea() == Sea.EAST ? EAST_SCALE : SCALE;
        pose.scale(k, k, k);
    }

    @Override
    public ResourceLocation getTextureLocation(DragonKingEntity e) {
        return SKINS.get(e.sea());
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-6.5f, -18, -4.5f, 13, 18, 9)
                .texOffs(148, 61).mirror(false).addBox(-4, -19.5f, -3.5f, 8, 2, 7)
                .texOffs(27, 61).mirror(false).addBox(-2.5f, -24, -2.5f, 5, 5, 5)
                .texOffs(44, 47).mirror(false).addBox(-8, -18.6f, -5.5f, 16, 0.6f, 11)
                .texOffs(141, 83).mirror(false).addBox(-7, -18.4f, -5.6f, 14, 6, 0.2f)
                .texOffs(102, 73).mirror(false).addBox(-7, -18.4f, 5.4f, 14, 7, 0.2f)
                .texOffs(170, 28).mirror(false).addBox(-7.5f, -4, -5.5f, 15, 2, 11)
                .texOffs(30, 107).mirror(false).addBox(-2.5f, -4.5f, -6, 5, 3, 1), PartPose.offsetAndRotation(0, 2, 0, 0, 0, 0));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(0, 28).mirror(false).addBox(-7.5f, 0, -5, 15, 8, 10)
                .texOffs(160, 0).mirror(false).addBox(-8.5f, 8, -6, 17, 7, 12)
                .texOffs(62, 0).mirror(false).addBox(-9.5f, 15, -7, 19, 7, 14), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition apron = skirt.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(45, 0).mirror(false).addBox(-3.5f, 0, -1, 7, 23, 1), PartPose.offsetAndRotation(0, -1.5f, -5.3f, -0.17f, 0, 0));
        PartDefinition rightPendant = skirt.addOrReplaceChild("right_pendant", CubeListBuilder.create()
                .texOffs(172, 83).mirror(false).addBox(-0.5f, 0, -0.5f, 1, 6, 1)
                .texOffs(43, 107).mirror(false).addBox(-1.5f, 6, -0.8f, 3, 2.5f, 1)
                .texOffs(143, 100).mirror(false).addBox(-0.5f, 8.5f, -0.5f, 1, 4, 1), PartPose.offsetAndRotation(-6, -1.5f, -5.2f, -0.12f, 0, 0.06f));
        PartDefinition leftPendant = skirt.addOrReplaceChild("left_pendant", CubeListBuilder.create()
                .texOffs(172, 83).mirror(true).addBox(-0.5f, 0, -0.5f, 1, 6, 1)
                .texOffs(43, 107).mirror(true).addBox(-1.5f, 6, -0.8f, 3, 2.5f, 1)
                .texOffs(143, 100).mirror(true).addBox(-0.5f, 8.5f, -0.5f, 1, 4, 1), PartPose.offsetAndRotation(6, -1.5f, -5.2f, -0.12f, 0, -0.06f));
        PartDefinition rightFoot = root.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(197, 92).mirror(false).addBox(-2, 0, -3.5f, 4, 2, 4), PartPose.offsetAndRotation(-3.4f, 22, -6, 0, 0.18f, 0));
        PartDefinition leftFoot = root.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(197, 92).mirror(true).addBox(-2, 0, -3.5f, 4, 2, 4), PartPose.offsetAndRotation(3.4f, 22, -6, 0, -0.18f, 0));
        PartDefinition rightToes = rightFoot.addOrReplaceChild("right_toes", CubeListBuilder.create()
                .texOffs(52, 107).mirror(false).addBox(-1.9f, -0.6f, -1.8f, 0.8f, 1.2f, 1.8f)
                .texOffs(148, 100).mirror(false).addBox(-0.4f, -0.6f, -2.1f, 0.8f, 1.2f, 2.1f)
                .texOffs(52, 107).mirror(false).addBox(1.1f, -0.6f, -1.8f, 0.8f, 1.2f, 1.8f), PartPose.offsetAndRotation(0, 1.2f, -3.4f, 0.35f, 0, 0));
        PartDefinition leftToes = leftFoot.addOrReplaceChild("left_toes", CubeListBuilder.create()
                .texOffs(52, 107).mirror(true).addBox(1.1f, -0.6f, -1.8f, 0.8f, 1.2f, 1.8f)
                .texOffs(148, 100).mirror(true).addBox(-0.4f, -0.6f, -2.1f, 0.8f, 1.2f, 2.1f)
                .texOffs(52, 107).mirror(true).addBox(-1.9f, -0.6f, -1.8f, 0.8f, 1.2f, 1.8f), PartPose.offsetAndRotation(0, 1.2f, -3.4f, 0.35f, 0, 0));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(99, 47).mirror(false).addBox(-2.5f, -2.5f, 0, 5, 5, 7)
                .texOffs(48, 61).mirror(false).addBox(-0.1f, -5, 0.5f, 0.2f, 3, 6.5f), PartPose.offsetAndRotation(0, 20.8f, 5.5f, -0.12f, 0, 0));
        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create()
                .texOffs(65, 61).mirror(false).addBox(-2, -2, 0, 4, 4, 6)
                .texOffs(179, 61).mirror(false).addBox(-0.1f, -4.2f, 0.3f, 0.2f, 2.5f, 5.5f), PartPose.offsetAndRotation(0, 0.2f, 6.6f, 0.12f, 0.35f, 0));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create()
                .texOffs(133, 73).mirror(false).addBox(-1.5f, -1.5f, 0, 3, 3, 5)
                .texOffs(177, 83).mirror(false).addBox(-0.1f, -3.4f, 0.3f, 0.2f, 2, 4.5f), PartPose.offsetAndRotation(0, 0.3f, 5.6f, 0, 0.45f, 0));
        PartDefinition tailTip = tail3.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(214, 92).mirror(false).addBox(-1, -1, 0, 2, 2, 4)
                .texOffs(151, 28).mirror(false).addBox(-0.1f, -3.5f, 1, 0.2f, 7, 8), PartPose.offsetAndRotation(0, 0.2f, 4.6f, -0.1f, 0.4f, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(104, 28).mirror(false).addBox(-3.5f, -2, -3, 5, 10, 6), PartPose.offsetAndRotation(-7.5f, -16, 0, -0.12f, 0, 0.1f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(104, 28).mirror(true).addBox(-1.5f, -2, -3, 5, 10, 6), PartPose.offsetAndRotation(7.5f, -16, 0, -0.12f, 0, -0.1f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(86, 61).mirror(false).addBox(-4, -1.5f, -4, 5, 2, 8)
                .texOffs(193, 47).mirror(false).addBox(-4.5f, 0.3f, -4.5f, 5, 2, 9)
                .texOffs(222, 47).mirror(false).addBox(-5, 2.1f, -4.5f, 5, 2, 9), PartPose.offsetAndRotation(-1, -2.5f, 0, 0, 0, 0.28f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(86, 61).mirror(true).addBox(-1, -1.5f, -4, 5, 2, 8)
                .texOffs(193, 47).mirror(true).addBox(-0.5f, 0.3f, -4.5f, 5, 2, 9)
                .texOffs(222, 47).mirror(true).addBox(0, 2.1f, -4.5f, 5, 2, 9), PartPose.offsetAndRotation(1, -2.5f, 0, 0, 0, -0.28f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(223, 28).mirror(false).addBox(-2.5f, 0, -2.5f, 5, 8, 5)
                .texOffs(150, 73).mirror(false).addBox(-3, 6, -3, 6, 2, 6), PartPose.offsetAndRotation(-1, 7.5f, 0, -1.2f, -0.55f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(223, 28).mirror(true).addBox(-2.5f, 0, -2.5f, 5, 8, 5)
                .texOffs(150, 73).mirror(true).addBox(-3, 6, -3, 6, 2, 6), PartPose.offsetAndRotation(1, 7.5f, 0, -1.2f, 0.55f, 0));
        PartDefinition rightSleeve = rightForearm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(129, 0).mirror(false).addBox(-3, -1, -9, 6, 12, 9), PartPose.offsetAndRotation(0, 0.5f, 2.2f, 1.3596f, 0.1566f, 0.1619f));
        PartDefinition leftSleeve = leftForearm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(129, 0).mirror(true).addBox(-3, -1, -9, 6, 12, 9), PartPose.offsetAndRotation(0, 0.5f, 2.2f, 1.3596f, -0.1566f, -0.1619f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(227, 92).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, 8.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(227, 92).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, 8.2f, 0, 0, 0, 0));
        PartDefinition rightClaws = rightHand.addOrReplaceChild("right_claws", CubeListBuilder.create()
                .texOffs(104, 107).mirror(false).addBox(-1.5f, 0, -1.1f, 0.7f, 2, 0.7f)
                .texOffs(59, 107).mirror(false).addBox(-0.35f, 0, -1.3f, 0.7f, 2.2f, 0.7f)
                .texOffs(104, 107).mirror(false).addBox(0.8f, 0, -1.1f, 0.7f, 2, 0.7f), PartPose.offsetAndRotation(0, 3, -0.6f, -0.5f, 0, 0));
        PartDefinition leftClaws = leftHand.addOrReplaceChild("left_claws", CubeListBuilder.create()
                .texOffs(104, 107).mirror(true).addBox(0.8f, 0, -1.1f, 0.7f, 2, 0.7f)
                .texOffs(59, 107).mirror(true).addBox(-0.35f, 0, -1.3f, 0.7f, 2.2f, 0.7f)
                .texOffs(104, 107).mirror(true).addBox(-1.5f, 0, -1.1f, 0.7f, 2, 0.7f), PartPose.offsetAndRotation(0, 3, -0.6f, -0.5f, 0, 0));
        PartDefinition rightThumb = rightHand.addOrReplaceChild("right_thumb", CubeListBuilder.create()
                .texOffs(104, 107).mirror(false).addBox(-0.35f, 0, -0.35f, 0.7f, 2, 0.7f), PartPose.offsetAndRotation(-1.2f, 1.5f, -1.4f, -0.6f, 0, 0.5f));
        PartDefinition leftThumb = leftHand.addOrReplaceChild("left_thumb", CubeListBuilder.create()
                .texOffs(104, 107).mirror(true).addBox(-0.35f, 0, -0.35f, 0.7f, 2, 0.7f), PartPose.offsetAndRotation(1.2f, 1.5f, -1.4f, -0.6f, 0, -0.5f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(51, 28).mirror(false).addBox(-4.5f, -8, -5, 9, 8, 9)
                .texOffs(113, 61).mirror(false).addBox(-5.5f, -4, -3, 11, 4, 6)
                .texOffs(0, 47).mirror(false).addBox(-4, -8.5f, 1.5f, 8, 9, 4)
                .texOffs(240, 92).mirror(false).addBox(-1.5f, -8.8f, -5.2f, 3, 1, 5)
                .texOffs(157, 100).mirror(false).addBox(-5.9f, -7.6f, -5.8f, 2.4f, 1.8f, 2.6f)
                .texOffs(170, 100).mirror(false).addBox(3.5f, -7.6f, -5.8f, 2.4f, 1.8f, 2.6f), PartPose.offsetAndRotation(0, -23.5f, -1.5f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(190, 83).mirror(false).addBox(-4.8f, -1.3f, -0.5f, 4.8f, 1.8f, 4.5f), PartPose.offsetAndRotation(-0.6f, -7.7f, -5.6f, 0, 0, 0.3f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(190, 83).mirror(true).addBox(0, -1.3f, -0.5f, 4.8f, 1.8f, 4.5f), PartPose.offsetAndRotation(0.6f, -7.7f, -5.6f, 0, 0, -0.3f));
        PartDefinition rightBrowflame = rightBrow.addOrReplaceChild("right_browflame", CubeListBuilder.create()
                .texOffs(124, 47).mirror(false).addBox(-0.1f, -3.5f, 0, 0.2f, 4.5f, 7), PartPose.offsetAndRotation(-4.2f, -0.6f, 0.5f, 0.45f, -0.45f, 0));
        PartDefinition leftBrowflame = leftBrow.addOrReplaceChild("left_browflame", CubeListBuilder.create()
                .texOffs(124, 47).mirror(true).addBox(-0.1f, -3.5f, 0, 0.2f, 4.5f, 7), PartPose.offsetAndRotation(4.2f, -0.6f, 0.5f, 0.45f, 0.45f, 0));
        PartDefinition rightCheekbone = head.addOrReplaceChild("right_cheekbone", CubeListBuilder.create()
                .texOffs(211, 83).mirror(false).addBox(-1.4f, -1, -0.5f, 2, 2.2f, 4), PartPose.offsetAndRotation(-4.6f, -4.4f, -4.2f, 0, 0.35f, 0.15f));
        PartDefinition leftCheekbone = head.addOrReplaceChild("left_cheekbone", CubeListBuilder.create()
                .texOffs(211, 83).mirror(true).addBox(-0.6f, -1, -0.5f, 2, 2.2f, 4), PartPose.offsetAndRotation(4.6f, -4.4f, -4.2f, 0, -0.35f, -0.15f));
        PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create()
                .texOffs(175, 73).mirror(false).addBox(-3.5f, -2.6f, -4, 7, 4, 4)
                .texOffs(198, 73).mirror(false).addBox(-3, -2.2f, -8, 6, 4, 4)
                .texOffs(219, 73).mirror(false).addBox(-3.5f, -3.2f, -11, 7, 5, 3)
                .texOffs(0, 100).mirror(false).addBox(-1, -3.2f, -7.6f, 2, 1, 4.2f)
                .texOffs(194, 61).mirror(false).addBox(-2.9f, 1.5f, -10, 0.6f, 1.3f, 7)
                .texOffs(194, 61).mirror(false).addBox(2.3f, 1.5f, -10, 0.6f, 1.3f, 7)
                .texOffs(64, 107).mirror(false).addBox(-3, 1.4f, -10.8f, 1, 2.6f, 1)
                .texOffs(64, 107).mirror(false).addBox(2, 1.4f, -10.8f, 1, 2.6f, 1), PartPose.offsetAndRotation(0, -3.4f, -5, 0.05f, 0, 0));
        PartDefinition rightNostril = snout.addOrReplaceChild("right_nostril", CubeListBuilder.create()
                .texOffs(183, 100).mirror(false).addBox(-1.1f, -1.5f, -1.1f, 2.2f, 1.5f, 2.2f), PartPose.offsetAndRotation(-2.2f, -2.9f, -9.9f, 0.45f, 0, -0.45f));
        PartDefinition leftNostril = snout.addOrReplaceChild("left_nostril", CubeListBuilder.create()
                .texOffs(183, 100).mirror(true).addBox(-1.1f, -1.5f, -1.1f, 2.2f, 1.5f, 2.2f), PartPose.offsetAndRotation(2.2f, -2.9f, -9.9f, 0.45f, 0, 0.45f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 83).mirror(false).addBox(-3.5f, 0, -5, 7, 2, 6)
                .texOffs(224, 83).mirror(false).addBox(-2.8f, 0, -10, 5.6f, 1.8f, 5)
                .texOffs(27, 83).mirror(false).addBox(-2.6f, -1.1f, -9.6f, 0.6f, 1.1f, 6)
                .texOffs(27, 83).mirror(false).addBox(2, -1.1f, -9.6f, 0.6f, 1.1f, 6)
                .texOffs(109, 107).mirror(false).addBox(-2.6f, -2, -9.9f, 0.9f, 2, 0.9f)
                .texOffs(109, 107).mirror(false).addBox(1.7f, -2, -9.9f, 0.9f, 2, 0.9f)
                .texOffs(211, 61).mirror(false).addBox(-1.5f, -0.4f, -8.5f, 3, 0.5f, 7.5f), PartPose.offsetAndRotation(0, -0.6f, -3, 0.26f, 0, 0));
        PartDefinition rightFrill = head.addOrReplaceChild("right_frill", CubeListBuilder.create()
                .texOffs(127, 28).mirror(false).addBox(-0.1f, -3.5f, 0, 0.2f, 8, 8), PartPose.offsetAndRotation(-5.2f, -4.2f, -1.5f, 0.2f, -0.55f, 0.12f));
        PartDefinition leftFrill = head.addOrReplaceChild("left_frill", CubeListBuilder.create()
                .texOffs(127, 28).mirror(true).addBox(-0.1f, -3.5f, 0, 0.2f, 8, 8), PartPose.offsetAndRotation(5.2f, -4.2f, -1.5f, 0.2f, 0.55f, -0.12f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(234, 61).mirror(false).addBox(-0.1f, -2, 0, 0.2f, 3.5f, 5), PartPose.offsetAndRotation(-4.2f, -7, 0, 0.5f, -0.7f, 0.3f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(234, 61).mirror(true).addBox(-0.1f, -2, 0, 0.2f, 3.5f, 5), PartPose.offsetAndRotation(4.2f, -7, 0, 0.5f, 0.7f, -0.3f));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(25, 47).mirror(false).addBox(-0.1f, -4, 0, 0.2f, 4.5f, 8), PartPose.offsetAndRotation(0, -8.4f, 0.5f, -0.35f, 0, 0));
        PartDefinition maneLock = head.addOrReplaceChild("mane_lock", CubeListBuilder.create()
                .texOffs(0, 73).mirror(false).addBox(-3, 0, -1.2f, 6, 6, 2.4f), PartPose.offsetAndRotation(0, -2, 5, 0.4f, 0, 0));
        PartDefinition maneLock2 = maneLock.addOrReplaceChild("mane_lock_2", CubeListBuilder.create()
                .texOffs(42, 83).mirror(false).addBox(-2.5f, 0, -1, 5, 6, 2), PartPose.offsetAndRotation(0, 5.6f, 0, 0.15f, 0, 0));
        PartDefinition maneLock3 = maneLock2.addOrReplaceChild("mane_lock_3", CubeListBuilder.create()
                .texOffs(15, 100).mirror(false).addBox(-1.75f, 0, -0.8f, 3.5f, 4, 1.6f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.1f, 0, 0));
        PartDefinition rightMane = head.addOrReplaceChild("right_mane", CubeListBuilder.create()
                .texOffs(19, 73).mirror(false).addBox(-1.5f, 0, -1.2f, 3, 6, 2.4f), PartPose.offsetAndRotation(-3.6f, -4.5f, 3.6f, 0.3f, 0.2f, 0.28f));
        PartDefinition leftMane = head.addOrReplaceChild("left_mane", CubeListBuilder.create()
                .texOffs(19, 73).mirror(true).addBox(-1.5f, 0, -1.2f, 3, 6, 2.4f), PartPose.offsetAndRotation(3.6f, -4.5f, 3.6f, 0.3f, -0.2f, -0.28f));
        PartDefinition rightMane2 = rightMane.addOrReplaceChild("right_mane_2", CubeListBuilder.create()
                .texOffs(0, 92).mirror(false).addBox(-1.2f, 0, -1, 2.4f, 5, 2), PartPose.offsetAndRotation(0, 5.6f, 0, 0.15f, 0, -0.1f));
        PartDefinition leftMane2 = leftMane.addOrReplaceChild("left_mane_2", CubeListBuilder.create()
                .texOffs(0, 92).mirror(true).addBox(-1.2f, 0, -1, 2.4f, 5, 2), PartPose.offsetAndRotation(0, 5.6f, 0, 0.15f, 0, 0.1f));
        PartDefinition rightMane3 = rightMane2.addOrReplaceChild("right_mane_3", CubeListBuilder.create()
                .texOffs(28, 100).mirror(false).addBox(-0.8f, 0, -0.7f, 1.6f, 3.5f, 1.4f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.1f, 0, -0.1f));
        PartDefinition leftMane3 = leftMane2.addOrReplaceChild("left_mane_3", CubeListBuilder.create()
                .texOffs(28, 100).mirror(true).addBox(-0.8f, 0, -0.7f, 1.6f, 3.5f, 1.4f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.1f, 0, 0.1f));
        PartDefinition rightAntler = head.addOrReplaceChild("right_antler", CubeListBuilder.create()
                .texOffs(57, 83).mirror(false).addBox(-0.9f, -0.9f, 0, 1.8f, 1.8f, 6), PartPose.offsetAndRotation(-3, -7.6f, 0.2f, 0.95f, -0.38f, 0));
        PartDefinition leftAntler = head.addOrReplaceChild("left_antler", CubeListBuilder.create()
                .texOffs(57, 83).mirror(true).addBox(-0.9f, -0.9f, 0, 1.8f, 1.8f, 6), PartPose.offsetAndRotation(3, -7.6f, 0.2f, 0.95f, 0.38f, 0));
        PartDefinition rightAntler2 = rightAntler.addOrReplaceChild("right_antler_2", CubeListBuilder.create()
                .texOffs(11, 92).mirror(false).addBox(-0.7f, -0.7f, 0, 1.4f, 1.4f, 5), PartPose.offsetAndRotation(0, 0, 5.8f, -0.45f, 0.05f, 0));
        PartDefinition leftAntler2 = leftAntler.addOrReplaceChild("left_antler_2", CubeListBuilder.create()
                .texOffs(11, 92).mirror(true).addBox(-0.7f, -0.7f, 0, 1.4f, 1.4f, 5), PartPose.offsetAndRotation(0, 0, 5.8f, -0.45f, -0.05f, 0));
        PartDefinition rightAntler3 = rightAntler2.addOrReplaceChild("right_antler_3", CubeListBuilder.create()
                .texOffs(11, 92).mirror(false).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 4.5f), PartPose.offsetAndRotation(0, 0, 4.8f, -0.4f, 0.1f, 0));
        PartDefinition leftAntler3 = leftAntler2.addOrReplaceChild("left_antler_3", CubeListBuilder.create()
                .texOffs(11, 92).mirror(true).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 4.5f), PartPose.offsetAndRotation(0, 0, 4.8f, -0.4f, -0.1f, 0));
        PartDefinition rightAntler4 = rightAntler3.addOrReplaceChild("right_antler_4", CubeListBuilder.create()
                .texOffs(69, 107).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 3), PartPose.offsetAndRotation(0, 0, 4.3f, -0.3f, 0, 0));
        PartDefinition leftAntler4 = leftAntler3.addOrReplaceChild("left_antler_4", CubeListBuilder.create()
                .texOffs(69, 107).mirror(true).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 3), PartPose.offsetAndRotation(0, 0, 4.3f, -0.3f, 0, 0));
        PartDefinition rightAntlerTine = rightAntler.addOrReplaceChild("right_antler_tine", CubeListBuilder.create()
                .texOffs(37, 100).mirror(false).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 4), PartPose.offsetAndRotation(0, 0, 3.2f, 0.9f, -0.25f, 0));
        PartDefinition leftAntlerTine = leftAntler.addOrReplaceChild("left_antler_tine", CubeListBuilder.create()
                .texOffs(37, 100).mirror(true).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 4), PartPose.offsetAndRotation(0, 0, 3.2f, 0.9f, 0.25f, 0));
        PartDefinition rightAntlerTine2 = rightAntler2.addOrReplaceChild("right_antler_tine2", CubeListBuilder.create()
                .texOffs(196, 100).mirror(false).addBox(-0.5f, -0.5f, 0, 1, 1, 3.5f), PartPose.offsetAndRotation(0, 0, 3.2f, 1, 0.2f, 0));
        PartDefinition leftAntlerTine2 = leftAntler2.addOrReplaceChild("left_antler_tine2", CubeListBuilder.create()
                .texOffs(196, 100).mirror(true).addBox(-0.5f, -0.5f, 0, 1, 1, 3.5f), PartPose.offsetAndRotation(0, 0, 3.2f, 1, -0.2f, 0));
        PartDefinition rightAntlerTine3 = rightAntler3.addOrReplaceChild("right_antler_tine3", CubeListBuilder.create()
                .texOffs(69, 107).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 2.5f), PartPose.offsetAndRotation(0, 0, 3, 0.9f, -0.3f, 0));
        PartDefinition leftAntlerTine3 = leftAntler3.addOrReplaceChild("left_antler_tine3", CubeListBuilder.create()
                .texOffs(69, 107).mirror(true).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 2.5f), PartPose.offsetAndRotation(0, 0, 3, 0.9f, 0.3f, 0));
        PartDefinition rightAntlerBrow = rightAntler.addOrReplaceChild("right_antler_brow", CubeListBuilder.create()
                .texOffs(69, 107).mirror(false).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 2.6f), PartPose.offsetAndRotation(0, 0, 1.2f, 1.75f, -0.35f, 0));
        PartDefinition leftAntlerBrow = leftAntler.addOrReplaceChild("left_antler_brow", CubeListBuilder.create()
                .texOffs(69, 107).mirror(true).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 2.6f), PartPose.offsetAndRotation(0, 0, 1.2f, 1.75f, 0.35f, 0));
        PartDefinition rightWhisker = snout.addOrReplaceChild("right_whisker", CubeListBuilder.create()
                .texOffs(50, 100).mirror(false).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 5), PartPose.offsetAndRotation(-3.5f, 0.2f, -10, -0.15f, -1.1f, 0));
        PartDefinition leftWhisker = snout.addOrReplaceChild("left_whisker", CubeListBuilder.create()
                .texOffs(50, 100).mirror(true).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 5), PartPose.offsetAndRotation(3.5f, 0.2f, -10, -0.15f, 1.1f, 0));
        PartDefinition rightWhisker2 = rightWhisker.addOrReplaceChild("right_whisker_2", CubeListBuilder.create()
                .texOffs(26, 92).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 5.5f), PartPose.offsetAndRotation(0, 0, 4.8f, -0.25f, 0.45f, 0));
        PartDefinition leftWhisker2 = leftWhisker.addOrReplaceChild("left_whisker_2", CubeListBuilder.create()
                .texOffs(26, 92).mirror(true).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 5.5f), PartPose.offsetAndRotation(0, 0, 4.8f, -0.25f, -0.45f, 0));
        PartDefinition rightWhisker3 = rightWhisker2.addOrReplaceChild("right_whisker_3", CubeListBuilder.create()
                .texOffs(50, 100).mirror(false).addBox(-0.35f, -0.35f, 0, 0.7f, 0.7f, 5), PartPose.offsetAndRotation(0, 0, 5.3f, -0.1f, 0.35f, 0));
        PartDefinition leftWhisker3 = leftWhisker2.addOrReplaceChild("left_whisker_3", CubeListBuilder.create()
                .texOffs(50, 100).mirror(true).addBox(-0.35f, -0.35f, 0, 0.7f, 0.7f, 5), PartPose.offsetAndRotation(0, 0, 5.3f, -0.1f, -0.35f, 0));
        PartDefinition rightWhisker4 = rightWhisker3.addOrReplaceChild("right_whisker_4", CubeListBuilder.create()
                .texOffs(207, 100).mirror(false).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 4), PartPose.offsetAndRotation(0, 0, 4.8f, 0.5f, 0.2f, 0));
        PartDefinition leftWhisker4 = leftWhisker3.addOrReplaceChild("left_whisker_4", CubeListBuilder.create()
                .texOffs(207, 100).mirror(true).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 4), PartPose.offsetAndRotation(0, 0, 4.8f, 0.5f, -0.2f, 0));
        PartDefinition eastCrown = head.addOrReplaceChild("east_crown", CubeListBuilder.create()
                .texOffs(32, 73).mirror(false).addBox(-2.5f, -3.5f, -2.5f, 5, 3.5f, 5)
                .texOffs(141, 47).mirror(false).addBox(-4, -4.5f, -6, 8, 1, 11)
                .texOffs(196, 107).mirror(false).addBox(-6, -2.4f, -0.4f, 12, 0.8f, 0.8f)
                .texOffs(114, 107).mirror(false).addBox(-0.8f, -3, -2.9f, 1.6f, 1.6f, 0.6f)
                .texOffs(41, 92).mirror(false).addBox(-4.6f, -3.6f, -2.6f, 9.2f, 0.8f, 5.2f)
                .texOffs(218, 100).mirror(false).addBox(-3.4f, -3.5f, -5.6f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(-2.04f, -3.5f, -5.6f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(-0.68f, -3.5f, -5.6f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(0.68f, -3.5f, -5.6f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(2.04f, -3.5f, -5.6f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(3.4f, -3.5f, -5.6f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(-3.4f, -3.5f, 4.4f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(-2.04f, -3.5f, 4.4f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(-0.68f, -3.5f, 4.4f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(0.68f, -3.5f, 4.4f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(2.04f, -3.5f, 4.4f, 0.4f, 3.8f, 0.4f)
                .texOffs(218, 100).mirror(false).addBox(3.4f, -3.5f, 4.4f, 0.4f, 3.8f, 0.4f), PartPose.offsetAndRotation(0, -8, -1, 0, 0, 0));
        PartDefinition rightEastRibbon = eastCrown.addOrReplaceChild("right_east_ribbon", CubeListBuilder.create()
                .texOffs(223, 100).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 4, 0.6f)
                .texOffs(78, 107).mirror(false).addBox(-0.7f, 4, -0.7f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(-5.8f, -2, 0, 0.1f, 0, 0.12f));
        PartDefinition leftEastRibbon = eastCrown.addOrReplaceChild("left_east_ribbon", CubeListBuilder.create()
                .texOffs(223, 100).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 4, 0.6f)
                .texOffs(78, 107).mirror(true).addBox(-0.7f, 4, -0.7f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(5.8f, -2, 0, 0.1f, 0, -0.12f));
        PartDefinition eastBeard = jaw.addOrReplaceChild("east_beard", CubeListBuilder.create()
                .texOffs(74, 92).mirror(false).addBox(-2, 0, -0.9f, 4, 5, 1.8f), PartPose.offsetAndRotation(0, 1.5f, -5.6f, -0.34f, 0, 0));
        PartDefinition eastBeard2 = eastBeard.addOrReplaceChild("east_beard_2", CubeListBuilder.create()
                .texOffs(74, 92).mirror(false).addBox(-1.7f, 0, -0.75f, 3.4f, 5, 1.5f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.12f, 0, 0.04f));
        PartDefinition eastBeard3 = eastBeard2.addOrReplaceChild("east_beard_3", CubeListBuilder.create()
                .texOffs(87, 92).mirror(false).addBox(-1.3f, 0, -0.6f, 2.6f, 5, 1.2f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.1f, 0, -0.06f));
        PartDefinition eastBeard4 = eastBeard3.addOrReplaceChild("east_beard_4", CubeListBuilder.create()
                .texOffs(228, 100).mirror(false).addBox(-0.7f, 0, -0.4f, 1.4f, 4, 0.8f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.12f, 0, 0.05f));
        PartDefinition eastItem = rightHand.addOrReplaceChild("east_item", CubeListBuilder.create()
                .texOffs(88, 28).mirror(false).addBox(-1.3f, -14, -0.4f, 2.6f, 15.5f, 0.8f), PartPose.offsetAndRotation(0, 1.8f, -0.2f, 1.096f, 0.3074f, 0.6012f));
        PartDefinition southCrown = head.addOrReplaceChild("south_crown", CubeListBuilder.create()
                .texOffs(98, 92).mirror(false).addBox(-4.7f, -1.2f, -2.4f, 9.4f, 1.8f, 4.6f)
                .texOffs(121, 107).mirror(false).addBox(-0.9f, -2.6f, -2.9f, 1.8f, 1.8f, 0.8f), PartPose.offsetAndRotation(0, -8, -3, 0, 0, 0));
        PartDefinition southFlame = southCrown.addOrReplaceChild("south_flame", CubeListBuilder.create()
                .texOffs(53, 73).mirror(false).addBox(-2, -7.5f, 0, 4, 7.5f, 0.3f), PartPose.offsetAndRotation(0, -1, -2.2f, -0.15f, 0, 0));
        PartDefinition rightSouthFlame = southCrown.addOrReplaceChild("right_south_flame", CubeListBuilder.create()
                .texOffs(129, 92).mirror(false).addBox(-1.6f, -5.5f, 0, 3.2f, 5.5f, 0.3f), PartPose.offsetAndRotation(-2.6f, -1, -2, -0.1f, 0.25f, -0.38f));
        PartDefinition leftSouthFlame = southCrown.addOrReplaceChild("left_south_flame", CubeListBuilder.create()
                .texOffs(129, 92).mirror(true).addBox(-1.6f, -5.5f, 0, 3.2f, 5.5f, 0.3f), PartPose.offsetAndRotation(2.6f, -1, -2, -0.1f, -0.25f, 0.38f));
        PartDefinition rightSouthFlame2 = southCrown.addOrReplaceChild("right_south_flame2", CubeListBuilder.create()
                .texOffs(235, 100).mirror(false).addBox(-1.2f, -4, 0, 2.4f, 4, 0.3f), PartPose.offsetAndRotation(-4.3f, -0.6f, -0.6f, -0.05f, 0.9f, -0.7f));
        PartDefinition leftSouthFlame2 = southCrown.addOrReplaceChild("left_south_flame2", CubeListBuilder.create()
                .texOffs(235, 100).mirror(true).addBox(-1.2f, -4, 0, 2.4f, 4, 0.3f), PartPose.offsetAndRotation(4.3f, -0.6f, -0.6f, -0.05f, -0.9f, 0.7f));
        PartDefinition southBeard = jaw.addOrReplaceChild("south_beard", CubeListBuilder.create()
                .texOffs(0, 107).mirror(false).addBox(-2.2f, 0, -0.9f, 4.4f, 3, 1.8f), PartPose.offsetAndRotation(0, 1.5f, -5.8f, -0.3f, 0, 0));
        PartDefinition rightSouthFork = southBeard.addOrReplaceChild("right_south_fork", CubeListBuilder.create()
                .texOffs(87, 92).mirror(false).addBox(-1.1f, 0, -0.75f, 2.2f, 5, 1.5f), PartPose.offsetAndRotation(-1.1f, 2.6f, 0, 0.1f, 0, 0.35f));
        PartDefinition leftSouthFork = southBeard.addOrReplaceChild("left_south_fork", CubeListBuilder.create()
                .texOffs(87, 92).mirror(true).addBox(-1.1f, 0, -0.75f, 2.2f, 5, 1.5f), PartPose.offsetAndRotation(1.1f, 2.6f, 0, 0.1f, 0, -0.35f));
        PartDefinition rightSouthFork2 = rightSouthFork.addOrReplaceChild("right_south_fork_2", CubeListBuilder.create()
                .texOffs(63, 100).mirror(false).addBox(-0.8f, 0, -0.55f, 1.6f, 4, 1.1f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.05f, 0, -0.15f));
        PartDefinition leftSouthFork2 = leftSouthFork.addOrReplaceChild("left_south_fork_2", CubeListBuilder.create()
                .texOffs(63, 100).mirror(true).addBox(-0.8f, 0, -0.55f, 1.6f, 4, 1.1f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.05f, 0, 0.15f));
        PartDefinition rightSouthFork3 = rightSouthFork2.addOrReplaceChild("right_south_fork_3", CubeListBuilder.create()
                .texOffs(87, 107).mirror(false).addBox(-0.45f, 0, -0.35f, 0.9f, 3, 0.7f), PartPose.offsetAndRotation(0, 3.6f, 0, 0.05f, 0, -0.2f));
        PartDefinition leftSouthFork3 = leftSouthFork2.addOrReplaceChild("left_south_fork_3", CubeListBuilder.create()
                .texOffs(87, 107).mirror(true).addBox(-0.45f, 0, -0.35f, 0.9f, 3, 0.7f), PartPose.offsetAndRotation(0, 3.6f, 0, 0.05f, 0, 0.2f));
        PartDefinition southItem = leftHand.addOrReplaceChild("south_item", CubeListBuilder.create()
                .texOffs(74, 83).mirror(false).addBox(-1.6f, -3.4f, -1.6f, 3.2f, 3.2f, 3.2f)
                .texOffs(180, 47).mirror(false).addBox(-0.1f, -7.5f, -2.5f, 0.2f, 7, 5)
                .texOffs(91, 83).mirror(false).addBox(-2.5f, -7.5f, -0.1f, 5, 7, 0.2f), PartPose.offsetAndRotation(0, 3, -1.8f, 1.3658f, -0.2885f, -0.4985f));
        PartDefinition westCrown = head.addOrReplaceChild("west_crown", CubeListBuilder.create()
                .texOffs(0, 61).mirror(false).addBox(-3.2f, -4, -3.9f, 6.4f, 4.4f, 6)
                .texOffs(72, 100).mirror(false).addBox(-2.6f, -5.6f, -0.6f, 5.2f, 1.8f, 3.6f)
                .texOffs(64, 73).mirror(false).addBox(-3.4f, -1.4f, -4.1f, 6.8f, 1.4f, 6.4f)
                .texOffs(114, 107).mirror(false).addBox(-0.8f, -3, -4.4f, 1.6f, 1.6f, 0.6f), PartPose.offsetAndRotation(0, -8, -1, 0, 0, 0));
        PartDefinition rightWestWing = westCrown.addOrReplaceChild("right_west_wing", CubeListBuilder.create()
                .texOffs(128, 107).mirror(false).addBox(-12, -0.2f, -0.6f, 12, 0.4f, 1.2f), PartPose.offsetAndRotation(-2.8f, -2.4f, 1.2f, 0, 0.05f, 0.06f));
        PartDefinition leftWestWing = westCrown.addOrReplaceChild("left_west_wing", CubeListBuilder.create()
                .texOffs(128, 107).mirror(true).addBox(0, -0.2f, -0.6f, 12, 0.4f, 1.2f), PartPose.offsetAndRotation(2.8f, -2.4f, 1.2f, 0, -0.05f, -0.06f));
        PartDefinition westBeard = jaw.addOrReplaceChild("west_beard", CubeListBuilder.create()
                .texOffs(93, 100).mirror(false).addBox(-1.7f, 0, -0.8f, 3.4f, 4, 1.6f), PartPose.offsetAndRotation(0, 1.5f, -6, -0.3f, 0, 0));
        PartDefinition westBeard2 = westBeard.addOrReplaceChild("west_beard_2", CubeListBuilder.create()
                .texOffs(106, 100).mirror(false).addBox(-1.2f, 0, -0.6f, 2.4f, 4, 1.2f), PartPose.offsetAndRotation(0, 3.6f, 0, 0.1f, 0, 0));
        PartDefinition westBeard3 = westBeard2.addOrReplaceChild("west_beard_3", CubeListBuilder.create()
                .texOffs(92, 107).mirror(false).addBox(-0.6f, 0, -0.4f, 1.2f, 3, 0.8f), PartPose.offsetAndRotation(0, 3.6f, 0, 0.15f, 0, 0));
        PartDefinition rightWestMoustache = snout.addOrReplaceChild("right_west_moustache", CubeListBuilder.create()
                .texOffs(63, 100).mirror(false).addBox(-0.6f, 0, -0.6f, 1.2f, 4, 1.2f), PartPose.offsetAndRotation(-3.2f, 1.6f, -9.6f, 0.25f, -0.5f, 0.35f));
        PartDefinition leftWestMoustache = snout.addOrReplaceChild("left_west_moustache", CubeListBuilder.create()
                .texOffs(63, 100).mirror(true).addBox(-0.6f, 0, -0.6f, 1.2f, 4, 1.2f), PartPose.offsetAndRotation(3.2f, 1.6f, -9.6f, 0.25f, 0.5f, -0.35f));
        PartDefinition rightWestMoustache2 = rightWestMoustache.addOrReplaceChild("right_west_moustache_2", CubeListBuilder.create()
                .texOffs(15, 107).mirror(false).addBox(-0.45f, 0, -0.45f, 0.9f, 3.5f, 0.9f), PartPose.offsetAndRotation(0, 3.6f, 0, 0.1f, 0, 0.2f));
        PartDefinition leftWestMoustache2 = leftWestMoustache.addOrReplaceChild("left_west_moustache_2", CubeListBuilder.create()
                .texOffs(15, 107).mirror(true).addBox(-0.45f, 0, -0.45f, 0.9f, 3.5f, 0.9f), PartPose.offsetAndRotation(0, 3.6f, 0, 0.1f, 0, -0.2f));
        PartDefinition rightWestMoustache3 = rightWestMoustache2.addOrReplaceChild("right_west_moustache_3", CubeListBuilder.create()
                .texOffs(87, 107).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3, 0.6f), PartPose.offsetAndRotation(0, 3.1f, 0, 0.1f, 0, 0.2f));
        PartDefinition leftWestMoustache3 = leftWestMoustache2.addOrReplaceChild("left_west_moustache_3", CubeListBuilder.create()
                .texOffs(87, 107).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 3, 0.6f), PartPose.offsetAndRotation(0, 3.1f, 0, 0.1f, 0, -0.2f));
        PartDefinition westItem = rightHand.addOrReplaceChild("west_item", CubeListBuilder.create()
                .texOffs(146, 28).mirror(false).addBox(-0.5f, -13, -0.5f, 1, 15, 1)
                .texOffs(140, 92).mirror(false).addBox(-1.9f, -15.6f, -1.6f, 3.8f, 3, 3.2f)
                .texOffs(117, 100).mirror(false).addBox(-0.6f, 1.5f, -0.6f, 1.2f, 4, 1.2f), PartPose.offsetAndRotation(0, 1.8f, -0.2f, 1.096f, 0.3074f, 0.6012f));
        PartDefinition northCrown = head.addOrReplaceChild("north_crown", CubeListBuilder.create()
                .texOffs(157, 92).mirror(false).addBox(-4.7f, -1.2f, -2.4f, 9.4f, 1.8f, 4.6f)
                .texOffs(157, 107).mirror(false).addBox(-0.9f, -2.6f, -2.9f, 1.8f, 1.8f, 0.8f), PartPose.offsetAndRotation(0, -8, -3, 0, 0, 0));
        PartDefinition northSpike = northCrown.addOrReplaceChild("north_spike", CubeListBuilder.create()
                .texOffs(93, 73).mirror(false).addBox(-0.7f, -7.5f, -0.7f, 1.4f, 7, 1.4f)
                .texOffs(99, 107).mirror(false).addBox(-0.35f, -9.5f, -0.35f, 0.7f, 2.5f, 0.7f), PartPose.offsetAndRotation(0, -1, -1.4f, -0.1f, 0, 0));
        PartDefinition rightNorthSpike = northCrown.addOrReplaceChild("right_north_spike", CubeListBuilder.create()
                .texOffs(188, 92).mirror(false).addBox(-0.6f, -5.5f, -0.6f, 1.2f, 5, 1.2f)
                .texOffs(164, 107).mirror(false).addBox(-0.3f, -7, -0.3f, 0.6f, 2, 0.6f), PartPose.offsetAndRotation(-2.2f, -1, -1.6f, -0.1f, 0, -0.28f));
        PartDefinition leftNorthSpike = northCrown.addOrReplaceChild("left_north_spike", CubeListBuilder.create()
                .texOffs(188, 92).mirror(true).addBox(-0.6f, -5.5f, -0.6f, 1.2f, 5, 1.2f)
                .texOffs(164, 107).mirror(true).addBox(-0.3f, -7, -0.3f, 0.6f, 2, 0.6f), PartPose.offsetAndRotation(2.2f, -1, -1.6f, -0.1f, 0, 0.28f));
        PartDefinition rightNorthSpike2 = northCrown.addOrReplaceChild("right_north_spike2", CubeListBuilder.create()
                .texOffs(20, 107).mirror(false).addBox(-0.5f, -4, -0.5f, 1, 3.6f, 1)
                .texOffs(164, 107).mirror(false).addBox(-0.25f, -5.2f, -0.25f, 0.5f, 1.6f, 0.5f), PartPose.offsetAndRotation(-4, -0.8f, -0.6f, 0, 0, -0.62f));
        PartDefinition leftNorthSpike2 = northCrown.addOrReplaceChild("left_north_spike2", CubeListBuilder.create()
                .texOffs(20, 107).mirror(true).addBox(-0.5f, -4, -0.5f, 1, 3.6f, 1)
                .texOffs(164, 107).mirror(true).addBox(-0.25f, -5.2f, -0.25f, 0.5f, 1.6f, 0.5f), PartPose.offsetAndRotation(4, -0.8f, -0.6f, 0, 0, 0.62f));
        PartDefinition northBeard = jaw.addOrReplaceChild("north_beard", CubeListBuilder.create()
                .texOffs(126, 100).mirror(false).addBox(-2.6f, 0, -0.9f, 5.2f, 3.5f, 1.8f), PartPose.offsetAndRotation(0, 1.5f, -5.6f, -0.3f, 0, 0));
        PartDefinition rightNorthStrand = northBeard.addOrReplaceChild("right_north_strand", CubeListBuilder.create()
                .texOffs(104, 83).mirror(false).addBox(-1.2f, 0, -0.8f, 2.4f, 6, 1.6f), PartPose.offsetAndRotation(-1.3f, 3, 0, 0.1f, 0, 0.12f));
        PartDefinition leftNorthStrand = northBeard.addOrReplaceChild("left_north_strand", CubeListBuilder.create()
                .texOffs(104, 83).mirror(true).addBox(-1.2f, 0, -0.8f, 2.4f, 6, 1.6f), PartPose.offsetAndRotation(1.3f, 3, 0, 0.1f, 0, -0.12f));
        PartDefinition rightNorthStrand2 = rightNorthStrand.addOrReplaceChild("right_north_strand_2", CubeListBuilder.create()
                .texOffs(115, 83).mirror(false).addBox(-0.95f, 0, -0.65f, 1.9f, 6, 1.3f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.05f, 0, -0.06f));
        PartDefinition leftNorthStrand2 = leftNorthStrand.addOrReplaceChild("left_north_strand_2", CubeListBuilder.create()
                .texOffs(115, 83).mirror(true).addBox(-0.95f, 0, -0.65f, 1.9f, 6, 1.3f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.05f, 0, 0.06f));
        PartDefinition rightNorthStrand3 = rightNorthStrand2.addOrReplaceChild("right_north_strand_3", CubeListBuilder.create()
                .texOffs(228, 100).mirror(false).addBox(-0.6f, 0, -0.45f, 1.2f, 4, 0.9f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.05f, 0, -0.08f));
        PartDefinition leftNorthStrand3 = leftNorthStrand2.addOrReplaceChild("left_north_strand_3", CubeListBuilder.create()
                .texOffs(228, 100).mirror(true).addBox(-0.6f, 0, -0.45f, 1.2f, 4, 0.9f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.05f, 0, 0.08f));
        PartDefinition northItem = rightHand.addOrReplaceChild("north_item", CubeListBuilder.create()
                .texOffs(97, 28).mirror(false).addBox(-0.6f, -17, -0.2f, 1.2f, 16, 0.4f)
                .texOffs(169, 107).mirror(false).addBox(-0.3f, -18.4f, -0.2f, 0.6f, 1.6f, 0.4f)
                .texOffs(174, 107).mirror(false).addBox(-1.8f, -1.2f, -0.7f, 3.6f, 1, 1.4f)
                .texOffs(25, 107).mirror(false).addBox(-0.5f, -0.2f, -0.5f, 1, 3.8f, 1)
                .texOffs(187, 107).mirror(false).addBox(-0.8f, 3.4f, -0.8f, 1.6f, 1, 1.6f), PartPose.offsetAndRotation(0, 1.8f, -0.2f, 1.1394f, 0.1899f, 0.623f));
        PartDefinition northItem2 = leftHand.addOrReplaceChild("north_item2", CubeListBuilder.create()
                .texOffs(124, 83).mirror(false).addBox(-1.6f, -3.4f, -1.6f, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(0, 3, -1.8f, 1.3658f, -0.2885f, -0.4985f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<DragonKingEntity> {
        private final ModelPart root, body, head, jaw, skirt, apron, rightArm, leftArm, rightSleeve, leftSleeve;
        private final ModelPart[] tail, whiskers, frills, mane;
        /** Each king's own crown, beard and regalia, shown only on him. */
        private final Map<Sea, List<ModelPart>> own = new EnumMap<>(Sea.class);
        private final Map<Sea, ModelPart> beard = new EnumMap<>(Sea.class);

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            skirt = body.getChild("skirt");
            apron = skirt.getChild("apron");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            ModelPart rightForearm = rightArm.getChild("right_forearm"), leftForearm = leftArm.getChild("left_forearm");
            rightSleeve = rightForearm.getChild("right_sleeve");
            leftSleeve = leftForearm.getChild("left_sleeve");
            ModelPart t1 = root.getChild("tail"), t2 = t1.getChild("tail2"), t3 = t2.getChild("tail3");
            tail = new ModelPart[]{t1, t2, t3, t3.getChild("tail_tip")};
            ModelPart snout = head.getChild("snout");
            whiskers = new ModelPart[8];
            for (int side = 0; side < 2; side++) {
                String s = side == 0 ? "right_whisker" : "left_whisker";
                ModelPart w = snout.getChild(s);
                for (int i = 0; i < 4; i++) {
                    whiskers[side * 4 + i] = w;
                    if (i < 3) w = w.getChild(s + "_" + (i + 2));
                }
            }
            frills = new ModelPart[]{head.getChild("right_frill"), head.getChild("left_frill")};
            ModelPart lock = head.getChild("mane_lock");
            mane = new ModelPart[]{lock, lock.getChild("mane_lock_2"), head.getChild("right_mane"), head.getChild("left_mane")};
            ModelPart rightHand = rightForearm.getChild("right_hand"), leftHand = leftForearm.getChild("left_hand");
            for (Sea sea : Sea.values()) {
                String n = sea.name().toLowerCase(Locale.ROOT);
                List<ModelPart> parts = new ArrayList<>();
                parts.add(head.getChild(n + "_crown"));
                beard.put(sea, jaw.getChild(n + "_beard"));
                parts.add(beard.get(sea));
                for (ModelPart hand : List.of(rightHand, leftHand)) {
                    for (String item : List.of(n + "_item", n + "_item2")) if (hand.hasChild(item)) parts.add(hand.getChild(item));
                }
                for (String side : List.of("right_", "left_")) {
                    if (snout.hasChild(side + n + "_moustache")) parts.add(snout.getChild(side + n + "_moustache"));
                }
                own.put(sea, parts);
            }
        }

        @Override
        public void prepareMobModel(DragonKingEntity e, float swing, float amount, float partial) {
            own.forEach((sea, parts) -> parts.forEach(p -> p.visible = sea == e.sea()));
        }

        /** A part turned this much from its rest. */
        private static void sway(ModelPart p, float x, float y) {
            p.xRot = p.getInitialPose().xRot + x;
            p.yRot = p.getInitialPose().yRot + y;
        }

        @Override
        public void setupAnim(DragonKingEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            float drift = Mth.sin(age * 0.07f);
            // his sleeves drifting in the water; the right hand raised to strike or cast
            float strike = Mth.sin(attackTime * Mth.PI);
            float walk = Mth.cos(swing * 0.6f) * 0.3f * amount;
            sway(rightArm, drift * 0.06f - strike * 1.4f - walk, 0);
            sway(leftArm, -drift * 0.06f + walk, 0);
            sway(rightSleeve, Mth.sin(age * 0.09f) * 0.06f + amount * 0.25f + strike * 0.6f, 0);
            sway(leftSleeve, Mth.sin(age * 0.09f + 1.1f) * 0.06f + amount * 0.25f, 0);
            skirt.xRot = Mth.sin(age * 0.09f) * 0.03f + amount * 0.08f;
            sway(apron, Mth.sin(age * 0.08f + 0.5f) * 0.05f - amount * 0.2f, 0);
            jaw.xRot = jaw.getInitialPose().xRot + strike * 0.45f + Math.max(0, Mth.sin(age * 0.05f)) * 0.08f;
            for (int i = 0; i < tail.length; i++) sway(tail[i], 0, Mth.sin(age * 0.06f - i * 0.7f) * (0.12f + 0.06f * i));
            // the barbels and the hair streaming in the current, each joint a little behind the last
            for (int i = 0; i < whiskers.length; i++) {
                float ph = age * 0.1f - (i % 4) * 0.6f + (i / 4) * 1.7f;
                sway(whiskers[i], Mth.sin(ph * 1.3f) * 0.1f, Mth.sin(ph) * 0.14f * (i < 4 ? 1 : -1));
            }
            for (int i = 0; i < frills.length; i++) sway(frills[i], 0, Mth.sin(age * 0.08f + i * 1.3f) * 0.08f * (i == 0 ? 1 : -1));
            for (int i = 0; i < mane.length; i++) sway(mane[i], Mth.sin(age * 0.07f - i * 0.5f) * 0.06f + amount * 0.15f, 0);
            sway(beard.get(e.sea()), Mth.sin(age * 0.07f + 0.4f) * 0.06f + amount * 0.2f - strike * 0.3f, Mth.sin(age * 0.05f) * 0.06f);
            body.y = body.getInitialPose().y + Mth.sin(age * 0.07f) * 0.4f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
