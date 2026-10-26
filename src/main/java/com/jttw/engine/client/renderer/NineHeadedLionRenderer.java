package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.ninespirits.NineSpiritsAbilityHandler;
import com.jttw.engine.entity.ninespirits.NineHeadedLionEntity;
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
import net.minecraft.world.entity.player.Player;

/**
 * The Nine-Headed Lion, the Grand Saint of Nine Spirits' true form (the Taiyi Heavenly Worthy's
 * mount): a vast golden-tawny lion, its weight in a deep chest and heavy shoulders buried in a great
 * shaggy mane of burnished gold, strong haunches, thick legs on great clawed paws, a long tail ending
 * in a tuft of curls. Out of the mane rise nine thick maned necks, fanned in an arc from the shoulders,
 * the middle head the largest and furthest forward, four to each side set back and forward in turn.
 * Each head a guardian lion's: a broad flat face under a scowling brow, eyes smouldering gold-ember,
 * a wrinkled snout, fangs, jaws open; spiral curls ringing the face and bearding the jaw, flame-locks
 * swept back. A jewelled gold breast-collar hung with bells, a great bell at the breast, a saddle-cloth
 * of celestial blue scrolled with gold clouds.
 *
 * <p>The heads sway each on its own beat, their jaws working, now one and now another roaring. The
 * heads that strike with each of his blows (2 / 3 / 4 by True Form's tier,
 * {@link NineSpiritsAbilityHandler#formHeads}) are awake, their eyes burning full; the rest smoulder.
 * As he strikes, the waking heads lunge and bite and the others rear back roaring.
 */
public final class NineHeadedLionRenderer extends MobRenderer<NineHeadedLionEntity, NineHeadedLionRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("nine_headed_lion"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/nine_headed_lion.png");
    private static final RenderType EYES = RenderType.eyes(JTTWEngine.id("textures/entity/nine_headed_lion_eyes.png"));
    /** How much larger than its model's pixels it is drawn: as long as the Great White Bull, its heads far over it. */
    public static final float SCALE = 1.25f;

    public NineHeadedLionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 2.0f);
        // the eyes burn: the waking heads' full-bright, the rest dimmer
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, NineHeadedLionEntity e,
                               float swing, float amount, float partial, float age, float yaw, float pitch) {
                getParentModel().renderEyes(pose, buffers.getBuffer(EYES));
            }
        });
    }

    @Override
    public void render(NineHeadedLionEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        model.awake = NineSpiritsAbilityHandler.formHeads(formTier(e));
        super.render(e, yaw, partial, pose, buffers, light);
    }

    /** The tier of True Form of the player this form is drawn for (where he stands): 0 if none is found. */
    private static int formTier(NineHeadedLionEntity e) {
        for (Player p : e.level().players()) {
            if (p.distanceToSqr(e) < 1.0E-4) return NineSpiritsAbilityHandler.formTierShown(p);
        }
        return 0;
    }

    @Override
    protected void scale(NineHeadedLionEntity e, PoseStack pose, float partial) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(NineHeadedLionEntity e) {
        return TEXTURE;
    }

    /**
     * Its parts, in model pixels with the ground at y 24 and the heads toward -z: generated, with the
     * skin painted to the same layout, by {@code assets_source/nine_headed_lion.py}.
     */
    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-14.4f, -10.8f, -30, 28.8f, 24, 24)
                .texOffs(185, 141).mirror(false).addBox(-10.8f, 0, -32.4f, 21.6f, 13.2f, 9.6f)
                .texOffs(107, 0).mirror(false).addBox(-10.2f, -8.4f, -7.2f, 20.4f, 15.6f, 20.4f)
                .texOffs(261, 0).mirror(false).addBox(-12, -9.6f, 10.8f, 24, 16.8f, 15.6f)
                .texOffs(0, 83).mirror(false).addBox(-15.6f, -15.6f, -31.2f, 31.2f, 13.2f, 14.4f)
                .texOffs(0, 167).mirror(false).addBox(-14.4f, -16.8f, -19.2f, 28.8f, 12, 10.8f)
                .texOffs(204, 214).mirror(false).addBox(-13.2f, -14.4f, -10.8f, 26.4f, 9.6f, 7.2f)
                .texOffs(45, 214).mirror(false).addBox(-13.92f, 4.8f, -31.92f, 27.84f, 3.6f, 14.4f)
                .texOffs(194, 235).mirror(false).addBox(-3.12f, 8.4f, -33.84f, 6.24f, 7.2f, 6.24f)
                .texOffs(266, 292).mirror(false).addBox(-0.96f, 15.36f, -31.68f, 1.92f, 2.4f, 1.92f)
                .texOffs(246, 281).mirror(false).addBox(-9.84f, 8.4f, -32.64f, 3.12f, 3.6f, 3.12f)
                .texOffs(246, 281).mirror(false).addBox(6.72f, 8.4f, -32.64f, 3.12f, 3.6f, 3.12f)
                .texOffs(246, 281).mirror(false).addBox(-13.68f, 8.4f, -26.16f, 3.12f, 3.6f, 3.12f)
                .texOffs(246, 281).mirror(false).addBox(10.56f, 8.4f, -26.16f, 3.12f, 3.6f, 3.12f)
                .texOffs(217, 191).mirror(false).addBox(-11.52f, -11.04f, -6, 23.04f, 2.64f, 16.8f)
                .texOffs(55, 49).mirror(false).addBox(-11.76f, -9.6f, -6.24f, 1.68f, 13.2f, 17.28f)
                .texOffs(55, 49).mirror(false).addBox(10.08f, -9.6f, -6.24f, 1.68f, 13.2f, 17.28f)
                .texOffs(250, 254).mirror(false).addBox(-3.6f, -12.24f, -2.4f, 7.2f, 1.44f, 9.6f)
                .texOffs(263, 281).mirror(false).addBox(-12.96f, 3.6f, 8.88f, 1.68f, 6, 1.68f)
                .texOffs(263, 281).mirror(false).addBox(11.28f, 3.6f, 8.88f, 1.68f, 6, 1.68f)
                .texOffs(78, 292).mirror(false).addBox(-12.96f, 3.6f, -5.52f, 1.68f, 4.8f, 1.68f)
                .texOffs(78, 292).mirror(false).addBox(11.28f, 3.6f, -5.52f, 1.68f, 4.8f, 1.68f)
                .texOffs(100, 191).mirror(false).addBox(-12.72f, -3.6f, 10.8f, 1.44f, 3.84f, 16.08f)
                .texOffs(100, 191).mirror(false).addBox(11.28f, -3.6f, 10.8f, 1.44f, 3.84f, 16.08f)
                .texOffs(155, 292).mirror(false).addBox(-12.72f, -3.6f, 25.92f, 25.44f, 3.84f, 1.44f)
                .texOffs(207, 269).mirror(false).addBox(-3.6f, -4.08f, 26.64f, 7.2f, 7.2f, 1.44f)
                .texOffs(263, 281).mirror(false).addBox(-0.84f, 3.12f, 26.88f, 1.68f, 6, 1.68f)
                .texOffs(246, 281).mirror(false).addBox(-9.6f, 0.24f, 26.16f, 3.12f, 3.6f, 3.12f)
                .texOffs(246, 281).mirror(false).addBox(6.48f, 0.24f, 26.16f, 3.12f, 3.6f, 3.12f), PartPose.offsetAndRotation(0, -4.8f, 0, 0, 0, 0));
        PartDefinition rump = body.addOrReplaceChild("rump", CubeListBuilder.create()
                .texOffs(0, 141).mirror(false).addBox(-11.4f, 0, 0, 22.8f, 8.4f, 15.6f)
                .texOffs(275, 214).mirror(false).addBox(-1.68f, -0.72f, -0.6f, 3.36f, 0.96f, 16.32f), PartPose.offsetAndRotation(0, -9.6f, 12, -0.35f, 0, 0));
        PartDefinition spineA = body.addOrReplaceChild("spine_a", CubeListBuilder.create()
                .texOffs(223, 235).mirror(false).addBox(-1.224f, -6.63f, -3.876f, 2.448f, 6.63f, 7.752f), PartPose.offsetAndRotation(0, -12.6f, 4.8f, -1.45f, 0, 0));
        PartDefinition spineAMid = spineA.addOrReplaceChild("spine_a_mid", CubeListBuilder.create()
                .texOffs(287, 254).mirror(false).addBox(-1.0404f, -5.61f, -3.3334f, 2.0808f, 5.61f, 5.1163f), PartPose.offsetAndRotation(0, -5.814f, 0, 0.32f, 0, 0));
        PartDefinition spineATip = spineAMid.addOrReplaceChild("spine_a_tip", CubeListBuilder.create()
                .texOffs(272, 281).mirror(false).addBox(-0.8568f, -4.59f, -2.4031f, 1.7136f, 4.59f, 2.6357f), PartPose.offsetAndRotation(0, -4.896f, 0, 0.4f, 0, 0));
        PartDefinition spineB = body.addOrReplaceChild("spine_b", CubeListBuilder.create()
                .texOffs(80, 254).mirror(false).addBox(-1.0368f, -5.616f, -3.2832f, 2.0736f, 5.616f, 6.5664f), PartPose.offsetAndRotation(0, -11.4f, 13.2f, -1.5f, 0, 0));
        PartDefinition spineBMid = spineB.addOrReplaceChild("spine_b_mid", CubeListBuilder.create()
                .texOffs(228, 269).mirror(false).addBox(-0.8813f, -4.752f, -2.8236f, 1.7626f, 4.752f, 4.3338f), PartPose.offsetAndRotation(0, -4.9248f, 0, 0.35f, 0, 0));
        PartDefinition spineBTip = spineBMid.addOrReplaceChild("spine_b_tip", CubeListBuilder.create()
                .texOffs(87, 292).mirror(false).addBox(-0.7258f, -3.888f, -2.0356f, 1.4515f, 3.888f, 2.2326f), PartPose.offsetAndRotation(0, -4.1472f, 0, 0.4375f, 0, 0));
        PartDefinition spineC = body.addOrReplaceChild("spine_c", CubeListBuilder.create()
                .texOffs(335, 254).mirror(false).addBox(-0.96f, -4.524f, -2.6448f, 1.92f, 4.524f, 5.2896f), PartPose.offsetAndRotation(0, -10.8f, 20.4f, -1.55f, 0, 0));
        PartDefinition spineCMid = spineC.addOrReplaceChild("spine_c_mid", CubeListBuilder.create()
                .texOffs(283, 281).mirror(false).addBox(-0.816f, -3.828f, -2.2745f, 1.632f, 3.828f, 3.4911f), PartPose.offsetAndRotation(0, -3.9672f, 0, 0.38f, 0, 0));
        PartDefinition spineCTip = spineCMid.addOrReplaceChild("spine_c_tip", CubeListBuilder.create()
                .texOffs(212, 292).mirror(false).addBox(-0.672f, -3.132f, -1.6398f, 1.344f, 3.132f, 1.7985f), PartPose.offsetAndRotation(0, -3.3408f, 0, 0.475f, 0, 0));
        PartDefinition rightManeA = body.addOrReplaceChild("right_mane_a", CubeListBuilder.create()
                .texOffs(139, 191).mirror(false).addBox(-1.728f, -9.36f, -5.472f, 3.456f, 9.36f, 10.944f), PartPose.offsetAndRotation(-14.4f, -14.4f, -21.6f, -1.4f, 0, -0.65f));
        PartDefinition leftManeA = body.addOrReplaceChild("left_mane_a", CubeListBuilder.create()
                .texOffs(139, 191).mirror(true).addBox(-1.728f, -9.36f, -5.472f, 3.456f, 9.36f, 10.944f), PartPose.offsetAndRotation(14.4f, -14.4f, -21.6f, -1.4f, 0, 0.65f));
        PartDefinition rightManeAMid = rightManeA.addOrReplaceChild("right_mane_a_mid", CubeListBuilder.create()
                .texOffs(134, 235).mirror(false).addBox(-1.4688f, -7.92f, -4.7059f, 2.9376f, 7.92f, 7.223f), PartPose.offsetAndRotation(0, -8.208f, 0, 0.26f, 0, 0));
        PartDefinition leftManeAMid = leftManeA.addOrReplaceChild("left_mane_a_mid", CubeListBuilder.create()
                .texOffs(134, 235).mirror(true).addBox(-1.4688f, -7.92f, -4.7059f, 2.9376f, 7.92f, 7.223f), PartPose.offsetAndRotation(0, -8.208f, 0, 0.26f, -0, 0));
        PartDefinition rightManeATip = rightManeAMid.addOrReplaceChild("right_mane_a_tip", CubeListBuilder.create()
                .texOffs(0, 269).mirror(false).addBox(-1.2096f, -6.48f, -3.3926f, 2.4192f, 6.48f, 3.721f), PartPose.offsetAndRotation(0, -6.912f, 0, 0.325f, 0, 0));
        PartDefinition leftManeATip = leftManeAMid.addOrReplaceChild("left_mane_a_tip", CubeListBuilder.create()
                .texOffs(0, 269).mirror(true).addBox(-1.2096f, -6.48f, -3.3926f, 2.4192f, 6.48f, 3.721f), PartPose.offsetAndRotation(0, -6.912f, 0, 0.325f, -0, 0));
        PartDefinition rightManeB = body.addOrReplaceChild("right_mane_b", CubeListBuilder.create()
                .texOffs(277, 235).mirror(false).addBox(-5.472f, -9.36f, -1.728f, 10.944f, 9.36f, 3.456f), PartPose.offsetAndRotation(-15.6f, -4.8f, -22.8f, -1.45f, 0, -1.1f));
        PartDefinition leftManeB = body.addOrReplaceChild("left_mane_b", CubeListBuilder.create()
                .texOffs(277, 235).mirror(true).addBox(-5.472f, -9.36f, -1.728f, 10.944f, 9.36f, 3.456f), PartPose.offsetAndRotation(15.6f, -4.8f, -22.8f, -1.45f, 0, 1.1f));
        PartDefinition rightManeBMid = rightManeB.addOrReplaceChild("right_mane_b_mid", CubeListBuilder.create()
                .texOffs(15, 269).mirror(false).addBox(-4.7059f, -7.92f, -1.4688f, 7.223f, 7.92f, 2.9376f), PartPose.offsetAndRotation(0, -8.208f, 0, 0, 0, 0.24f));
        PartDefinition leftManeBMid = leftManeB.addOrReplaceChild("left_mane_b_mid", CubeListBuilder.create()
                .texOffs(15, 269).mirror(true).addBox(-2.5171f, -7.92f, -1.4688f, 7.223f, 7.92f, 2.9376f), PartPose.offsetAndRotation(0, -8.208f, 0, 0, -0, -0.24f));
        PartDefinition rightManeBTip = rightManeBMid.addOrReplaceChild("right_mane_b_tip", CubeListBuilder.create()
                .texOffs(243, 269).mirror(false).addBox(-3.3926f, -6.48f, -1.2096f, 3.721f, 6.48f, 2.4192f), PartPose.offsetAndRotation(0, -6.912f, 0, 0, 0, 0.3f));
        PartDefinition leftManeBTip = leftManeBMid.addOrReplaceChild("left_mane_b_tip", CubeListBuilder.create()
                .texOffs(243, 269).mirror(true).addBox(-0.3283f, -6.48f, -1.2096f, 3.721f, 6.48f, 2.4192f), PartPose.offsetAndRotation(0, -6.912f, 0, 0, -0, -0.3f));
        PartDefinition rightManeC = body.addOrReplaceChild("right_mane_c", CubeListBuilder.create()
                .texOffs(101, 254).mirror(false).addBox(-5.016f, -8.58f, -1.584f, 10.032f, 8.58f, 3.168f), PartPose.offsetAndRotation(-15, 3.6f, -24, -1.45f, 0, -1.7f));
        PartDefinition leftManeC = body.addOrReplaceChild("left_mane_c", CubeListBuilder.create()
                .texOffs(101, 254).mirror(true).addBox(-5.016f, -8.58f, -1.584f, 10.032f, 8.58f, 3.168f), PartPose.offsetAndRotation(15, 3.6f, -24, -1.45f, 0, 1.7f));
        PartDefinition rightManeCMid = rightManeC.addOrReplaceChild("right_mane_c_mid", CubeListBuilder.create()
                .texOffs(38, 269).mirror(false).addBox(-4.3138f, -7.26f, -1.3464f, 6.6211f, 7.26f, 2.6928f), PartPose.offsetAndRotation(0, -7.524f, 0, 0, 0, 0.26f));
        PartDefinition leftManeCMid = leftManeC.addOrReplaceChild("left_mane_c_mid", CubeListBuilder.create()
                .texOffs(38, 269).mirror(true).addBox(-2.3074f, -7.26f, -1.3464f, 6.6211f, 7.26f, 2.6928f), PartPose.offsetAndRotation(0, -7.524f, 0, 0, -0, -0.26f));
        PartDefinition rightManeCTip = rightManeCMid.addOrReplaceChild("right_mane_c_tip", CubeListBuilder.create()
                .texOffs(89, 281).mirror(false).addBox(-3.1099f, -5.94f, -1.1088f, 3.4109f, 5.94f, 2.2176f), PartPose.offsetAndRotation(0, -6.336f, 0, 0, 0, 0.325f));
        PartDefinition leftManeCTip = leftManeCMid.addOrReplaceChild("left_mane_c_tip", CubeListBuilder.create()
                .texOffs(89, 281).mirror(true).addBox(-0.301f, -5.94f, -1.1088f, 3.4109f, 5.94f, 2.2176f), PartPose.offsetAndRotation(0, -6.336f, 0, 0, -0, -0.325f));
        PartDefinition rightManeD = body.addOrReplaceChild("right_mane_d", CubeListBuilder.create()
                .texOffs(132, 214).mirror(false).addBox(-1.512f, -8.19f, -4.788f, 3.024f, 8.19f, 9.576f), PartPose.offsetAndRotation(-13.8f, -13.2f, -12, -1.5f, 0, -0.7f));
        PartDefinition leftManeD = body.addOrReplaceChild("left_mane_d", CubeListBuilder.create()
                .texOffs(132, 214).mirror(true).addBox(-1.512f, -8.19f, -4.788f, 3.024f, 8.19f, 9.576f), PartPose.offsetAndRotation(13.8f, -13.2f, -12, -1.5f, 0, 0.7f));
        PartDefinition rightManeDMid = rightManeD.addOrReplaceChild("right_mane_d_mid", CubeListBuilder.create()
                .texOffs(308, 235).mirror(false).addBox(-1.2852f, -6.93f, -4.1177f, 2.5704f, 6.93f, 6.3202f), PartPose.offsetAndRotation(0, -7.182f, 0, 0.28f, 0, 0));
        PartDefinition leftManeDMid = leftManeD.addOrReplaceChild("left_mane_d_mid", CubeListBuilder.create()
                .texOffs(308, 235).mirror(true).addBox(-1.2852f, -6.93f, -4.1177f, 2.5704f, 6.93f, 6.3202f), PartPose.offsetAndRotation(0, -7.182f, 0, 0.28f, -0, 0));
        PartDefinition rightManeDTip = rightManeDMid.addOrReplaceChild("right_mane_d_tip", CubeListBuilder.create()
                .texOffs(258, 269).mirror(false).addBox(-1.0584f, -5.67f, -2.9686f, 2.1168f, 5.67f, 3.2558f), PartPose.offsetAndRotation(0, -6.048f, 0, 0.35f, 0, 0));
        PartDefinition leftManeDTip = leftManeDMid.addOrReplaceChild("left_mane_d_tip", CubeListBuilder.create()
                .texOffs(258, 269).mirror(true).addBox(-1.0584f, -5.67f, -2.9686f, 2.1168f, 5.67f, 3.2558f), PartPose.offsetAndRotation(0, -6.048f, 0, 0.35f, -0, 0));
        PartDefinition rightManeE = body.addOrReplaceChild("right_mane_e", CubeListBuilder.create()
                .texOffs(59, 269).mirror(false).addBox(-4.56f, -7.8f, -1.44f, 9.12f, 7.8f, 2.88f), PartPose.offsetAndRotation(-13.8f, -3.6f, -12, -1.55f, 0, -1.35f));
        PartDefinition leftManeE = body.addOrReplaceChild("left_mane_e", CubeListBuilder.create()
                .texOffs(59, 269).mirror(true).addBox(-4.56f, -7.8f, -1.44f, 9.12f, 7.8f, 2.88f), PartPose.offsetAndRotation(13.8f, -3.6f, -12, -1.55f, 0, 1.35f));
        PartDefinition rightManeEMid = rightManeE.addOrReplaceChild("right_mane_e_mid", CubeListBuilder.create()
                .texOffs(273, 269).mirror(false).addBox(-3.9216f, -6.6f, -1.224f, 6.0192f, 6.6f, 2.448f), PartPose.offsetAndRotation(0, -6.84f, 0, 0, 0, 0.26f));
        PartDefinition leftManeEMid = leftManeE.addOrReplaceChild("left_mane_e_mid", CubeListBuilder.create()
                .texOffs(273, 269).mirror(true).addBox(-2.0976f, -6.6f, -1.224f, 6.0192f, 6.6f, 2.448f), PartPose.offsetAndRotation(0, -6.84f, 0, 0, -0, -0.26f));
        PartDefinition rightManeETip = rightManeEMid.addOrReplaceChild("right_mane_e_tip", CubeListBuilder.create()
                .texOffs(89, 281).mirror(false).addBox(-2.8272f, -5.4f, -1.008f, 3.1008f, 5.4f, 2.016f), PartPose.offsetAndRotation(0, -5.76f, 0, 0, 0, 0.325f));
        PartDefinition leftManeETip = leftManeEMid.addOrReplaceChild("left_mane_e_tip", CubeListBuilder.create()
                .texOffs(89, 281).mirror(true).addBox(-0.2736f, -5.4f, -1.008f, 3.1008f, 5.4f, 2.016f), PartPose.offsetAndRotation(0, -5.76f, 0, 0, -0, -0.325f));
        PartDefinition rightManeF = body.addOrReplaceChild("right_mane_f", CubeListBuilder.create()
                .texOffs(86, 269).mirror(false).addBox(-4.104f, -7.02f, -1.296f, 8.208f, 7.02f, 2.592f), PartPose.offsetAndRotation(-12, -13.2f, -4.8f, -1.62f, 0, -0.85f));
        PartDefinition leftManeF = body.addOrReplaceChild("left_mane_f", CubeListBuilder.create()
                .texOffs(86, 269).mirror(true).addBox(-4.104f, -7.02f, -1.296f, 8.208f, 7.02f, 2.592f), PartPose.offsetAndRotation(12, -13.2f, -4.8f, -1.62f, 0, 0.85f));
        PartDefinition rightManeFMid = rightManeF.addOrReplaceChild("right_mane_f_mid", CubeListBuilder.create()
                .texOffs(104, 281).mirror(false).addBox(-3.5294f, -5.94f, -1.1016f, 5.4173f, 5.94f, 2.2032f), PartPose.offsetAndRotation(0, -6.156f, 0, 0, 0, 0.3f));
        PartDefinition leftManeFMid = leftManeF.addOrReplaceChild("left_mane_f_mid", CubeListBuilder.create()
                .texOffs(104, 281).mirror(true).addBox(-1.8878f, -5.94f, -1.1016f, 5.4173f, 5.94f, 2.2032f), PartPose.offsetAndRotation(0, -6.156f, 0, 0, -0, -0.3f));
        PartDefinition rightManeFTip = rightManeFMid.addOrReplaceChild("right_mane_f_tip", CubeListBuilder.create()
                .texOffs(98, 292).mirror(false).addBox(-2.5445f, -4.86f, -0.9072f, 2.7907f, 4.86f, 1.8144f), PartPose.offsetAndRotation(0, -5.184f, 0, 0, 0, 0.375f));
        PartDefinition leftManeFTip = leftManeFMid.addOrReplaceChild("left_mane_f_tip", CubeListBuilder.create()
                .texOffs(98, 292).mirror(true).addBox(-0.2462f, -4.86f, -0.9072f, 2.7907f, 4.86f, 1.8144f), PartPose.offsetAndRotation(0, -5.184f, 0, 0, -0, -0.375f));
        PartDefinition rightManeG = body.addOrReplaceChild("right_mane_g", CubeListBuilder.create()
                .texOffs(294, 269).mirror(false).addBox(-3.876f, -6.63f, -1.224f, 7.752f, 6.63f, 2.448f), PartPose.offsetAndRotation(-12.6f, 4.8f, -14.4f, -1.8f, 0, -1.6f));
        PartDefinition leftManeG = body.addOrReplaceChild("left_mane_g", CubeListBuilder.create()
                .texOffs(294, 269).mirror(true).addBox(-3.876f, -6.63f, -1.224f, 7.752f, 6.63f, 2.448f), PartPose.offsetAndRotation(12.6f, 4.8f, -14.4f, -1.8f, 0, 1.6f));
        PartDefinition rightManeGMid = rightManeG.addOrReplaceChild("right_mane_g_mid", CubeListBuilder.create()
                .texOffs(104, 281).mirror(false).addBox(-3.3334f, -5.61f, -1.0404f, 5.1163f, 5.61f, 2.0808f), PartPose.offsetAndRotation(0, -5.814f, 0, 0, 0, 0.3f));
        PartDefinition leftManeGMid = leftManeG.addOrReplaceChild("left_mane_g_mid", CubeListBuilder.create()
                .texOffs(104, 281).mirror(true).addBox(-1.783f, -5.61f, -1.0404f, 5.1163f, 5.61f, 2.0808f), PartPose.offsetAndRotation(0, -5.814f, 0, 0, -0, -0.3f));
        PartDefinition rightManeGTip = rightManeGMid.addOrReplaceChild("right_mane_g_tip", CubeListBuilder.create()
                .texOffs(98, 292).mirror(false).addBox(-2.4031f, -4.59f, -0.8568f, 2.6357f, 4.59f, 1.7136f), PartPose.offsetAndRotation(0, -4.896f, 0, 0, 0, 0.375f));
        PartDefinition leftManeGTip = leftManeGMid.addOrReplaceChild("left_mane_g_tip", CubeListBuilder.create()
                .texOffs(98, 292).mirror(true).addBox(-0.2326f, -4.59f, -0.8568f, 2.6357f, 4.59f, 1.7136f), PartPose.offsetAndRotation(0, -4.896f, 0, 0, -0, -0.375f));
        PartDefinition apron = body.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(101, 254).mirror(false).addBox(-5.016f, -8.58f, -1.584f, 10.032f, 8.58f, 3.168f), PartPose.offsetAndRotation(0, -3.6f, -33, 2.95f, 0, 0));
        PartDefinition apronMid = apron.addOrReplaceChild("apron_mid", CubeListBuilder.create()
                .texOffs(38, 269).mirror(false).addBox(-4.3138f, -7.26f, -1.3464f, 6.6211f, 7.26f, 2.6928f), PartPose.offsetAndRotation(0, -7.524f, 0, -0.25f, 0, 0));
        PartDefinition apronTip = apronMid.addOrReplaceChild("apron_tip", CubeListBuilder.create()
                .texOffs(89, 281).mirror(false).addBox(-3.1099f, -5.94f, -1.1088f, 3.4109f, 5.94f, 2.2176f), PartPose.offsetAndRotation(0, -6.336f, 0, -0.3125f, 0, 0));
        PartDefinition rightApron = body.addOrReplaceChild("right_apron", CubeListBuilder.create()
                .texOffs(132, 254).mirror(false).addBox(-4.788f, -8.19f, -1.512f, 9.576f, 8.19f, 3.024f), PartPose.offsetAndRotation(-6, -3.6f, -32.64f, 2.95f, 0, -0.18f));
        PartDefinition leftApron = body.addOrReplaceChild("left_apron", CubeListBuilder.create()
                .texOffs(132, 254).mirror(true).addBox(-4.788f, -8.19f, -1.512f, 9.576f, 8.19f, 3.024f), PartPose.offsetAndRotation(6, -3.6f, -32.64f, 2.95f, 0, 0.18f));
        PartDefinition rightApronMid = rightApron.addOrReplaceChild("right_apron_mid", CubeListBuilder.create()
                .texOffs(273, 269).mirror(false).addBox(-4.1177f, -6.93f, -1.2852f, 6.3202f, 6.93f, 2.5704f), PartPose.offsetAndRotation(0, -7.182f, 0, -0.25f, 0, 0));
        PartDefinition leftApronMid = leftApron.addOrReplaceChild("left_apron_mid", CubeListBuilder.create()
                .texOffs(273, 269).mirror(true).addBox(-2.2025f, -6.93f, -1.2852f, 6.3202f, 6.93f, 2.5704f), PartPose.offsetAndRotation(0, -7.182f, 0, -0.25f, -0, 0));
        PartDefinition rightApronTip = rightApronMid.addOrReplaceChild("right_apron_tip", CubeListBuilder.create()
                .texOffs(89, 281).mirror(false).addBox(-2.9686f, -5.67f, -1.0584f, 3.2558f, 5.67f, 2.1168f), PartPose.offsetAndRotation(0, -6.048f, 0, -0.3125f, 0, 0));
        PartDefinition leftApronTip = leftApronMid.addOrReplaceChild("left_apron_tip", CubeListBuilder.create()
                .texOffs(89, 281).mirror(true).addBox(-0.2873f, -5.67f, -1.0584f, 3.2558f, 5.67f, 2.1168f), PartPose.offsetAndRotation(0, -6.048f, 0, -0.3125f, -0, 0));
        PartDefinition rightApronB = body.addOrReplaceChild("right_apron_b", CubeListBuilder.create()
                .texOffs(86, 269).mirror(false).addBox(-4.332f, -7.41f, -1.368f, 8.664f, 7.41f, 2.736f), PartPose.offsetAndRotation(-11.4f, -4.2f, -31.2f, 2.95f, 0, -0.4f));
        PartDefinition leftApronB = body.addOrReplaceChild("left_apron_b", CubeListBuilder.create()
                .texOffs(86, 269).mirror(true).addBox(-4.332f, -7.41f, -1.368f, 8.664f, 7.41f, 2.736f), PartPose.offsetAndRotation(11.4f, -4.2f, -31.2f, 2.95f, 0, 0.4f));
        PartDefinition rightApronBMid = rightApronB.addOrReplaceChild("right_apron_b_mid", CubeListBuilder.create()
                .texOffs(317, 269).mirror(false).addBox(-3.7255f, -6.27f, -1.1628f, 5.7182f, 6.27f, 2.3256f), PartPose.offsetAndRotation(0, -6.498f, 0, -0.25f, 0, 0));
        PartDefinition leftApronBMid = leftApronB.addOrReplaceChild("left_apron_b_mid", CubeListBuilder.create()
                .texOffs(317, 269).mirror(true).addBox(-1.9927f, -6.27f, -1.1628f, 5.7182f, 6.27f, 2.3256f), PartPose.offsetAndRotation(0, -6.498f, 0, -0.25f, -0, 0));
        PartDefinition rightApronBTip = rightApronBMid.addOrReplaceChild("right_apron_b_tip", CubeListBuilder.create()
                .texOffs(296, 281).mirror(false).addBox(-2.6858f, -5.13f, -0.9576f, 2.9458f, 5.13f, 1.9152f), PartPose.offsetAndRotation(0, -5.472f, 0, -0.3125f, 0, 0));
        PartDefinition leftApronBTip = leftApronBMid.addOrReplaceChild("left_apron_b_tip", CubeListBuilder.create()
                .texOffs(296, 281).mirror(true).addBox(-0.2599f, -5.13f, -0.9576f, 2.9458f, 5.13f, 1.9152f), PartPose.offsetAndRotation(0, -5.472f, 0, -0.3125f, -0, 0));
        PartDefinition rightForeleg = body.addOrReplaceChild("right_foreleg", CubeListBuilder.create()
                .texOffs(96, 49).mirror(false).addBox(-6, -4.8f, -7.2f, 12, 15.6f, 14.4f)
                .texOffs(256, 83).mirror(false).addBox(-4.8f, 9.6f, -5.16f, 9.6f, 15.6f, 10.08f)
                .texOffs(123, 281).mirror(false).addBox(-2.88f, 11.52f, 4.92f, 5.76f, 5.76f, 3)
                .texOffs(300, 191).mirror(false).addBox(-6, 24.24f, -10.8f, 12, 5.76f, 13.2f)
                .texOffs(307, 281).mirror(false).addBox(-5.52f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(307, 281).mirror(false).addBox(-2.46f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(307, 281).mirror(false).addBox(0.6f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(307, 281).mirror(false).addBox(3.66f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f), PartPose.offsetAndRotation(-10.2f, -1.2f, -18, 0, 0, 0));
        PartDefinition leftForeleg = body.addOrReplaceChild("left_foreleg", CubeListBuilder.create()
                .texOffs(96, 49).mirror(true).addBox(-6, -4.8f, -7.2f, 12, 15.6f, 14.4f)
                .texOffs(256, 83).mirror(true).addBox(-4.8f, 9.6f, -5.16f, 9.6f, 15.6f, 10.08f)
                .texOffs(123, 281).mirror(true).addBox(-2.88f, 11.52f, 4.92f, 5.76f, 5.76f, 3)
                .texOffs(300, 191).mirror(true).addBox(-6, 24.24f, -10.8f, 12, 5.76f, 13.2f)
                .texOffs(307, 281).mirror(true).addBox(3.48f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(307, 281).mirror(true).addBox(0.42f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(307, 281).mirror(true).addBox(-2.64f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(307, 281).mirror(true).addBox(-5.7f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f), PartPose.offsetAndRotation(10.2f, -1.2f, -18, 0, 0, 0));
        PartDefinition rightHindleg = body.addOrReplaceChild("right_hindleg", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.8f, -2.4f, 18.6f, 0, 0, 0));
        PartDefinition leftHindleg = body.addOrReplaceChild("left_hindleg", CubeListBuilder.create(), PartPose.offsetAndRotation(7.8f, -2.4f, 18.6f, 0, 0, 0));
        PartDefinition rightThigh = rightHindleg.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                .texOffs(0, 49).mirror(false).addBox(-6, -3.6f, -7.8f, 12, 18, 14.4f), PartPose.offsetAndRotation(0, 0, 0, -0.5f, 0, 0));
        PartDefinition leftThigh = leftHindleg.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                .texOffs(0, 49).mirror(true).addBox(-6, -3.6f, -7.8f, 12, 18, 14.4f), PartPose.offsetAndRotation(0, 0, 0, -0.5f, 0, 0));
        PartDefinition rightShin = rightThigh.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(234, 167).mirror(false).addBox(-4.08f, -1.8f, -3.6f, 8.16f, 13.2f, 7.2f), PartPose.offsetAndRotation(0, 13.2f, -1.2f, 1.4f, 0, 0));
        PartDefinition leftShin = leftThigh.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(234, 167).mirror(true).addBox(-4.08f, -1.8f, -3.6f, 8.16f, 13.2f, 7.2f), PartPose.offsetAndRotation(0, 13.2f, -1.2f, 1.4f, 0, 0));
        PartDefinition rightHock = rightShin.addOrReplaceChild("right_hock", CubeListBuilder.create()
                .texOffs(318, 214).mirror(false).addBox(-3.6f, -1.2f, -3.24f, 7.2f, 10.8f, 6.48f), PartPose.offsetAndRotation(0, 11.4f, 0, -1.2f, 0, 0));
        PartDefinition leftHock = leftShin.addOrReplaceChild("left_hock", CubeListBuilder.create()
                .texOffs(318, 214).mirror(true).addBox(-3.6f, -1.2f, -3.24f, 7.2f, 10.8f, 6.48f), PartPose.offsetAndRotation(0, 11.4f, 0, -1.2f, 0, 0));
        PartDefinition rightHindpaw = rightHock.addOrReplaceChild("right_hindpaw", CubeListBuilder.create()
                .texOffs(64, 235).mirror(false).addBox(-5.4f, -0.84f, -9, 10.8f, 5.28f, 10.8f)
                .texOffs(109, 292).mirror(false).addBox(-4.68f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f)
                .texOffs(109, 292).mirror(false).addBox(-1.44f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f)
                .texOffs(109, 292).mirror(false).addBox(1.8f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f), PartPose.offsetAndRotation(0, 9.12f, 0, 0.3f, 0, 0));
        PartDefinition leftHindpaw = leftHock.addOrReplaceChild("left_hindpaw", CubeListBuilder.create()
                .texOffs(64, 235).mirror(true).addBox(-5.4f, -0.84f, -9, 10.8f, 5.28f, 10.8f)
                .texOffs(109, 292).mirror(true).addBox(2.76f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f)
                .texOffs(109, 292).mirror(true).addBox(-0.48f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f)
                .texOffs(109, 292).mirror(true).addBox(-3.72f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f), PartPose.offsetAndRotation(0, 9.12f, 0, 0.3f, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 235).mirror(false).addBox(-1.68f, -1.68f, 0, 3.36f, 3.36f, 13.2f), PartPose.offsetAndRotation(0, -6, 26.4f, -0.6f, 0, 0));
        PartDefinition tailMid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create()
                .texOffs(246, 235).mirror(false).addBox(-1.44f, -1.44f, 0, 2.88f, 2.88f, 12), PartPose.offsetAndRotation(0, 0, 12.6f, 0.9f, 0, 0));
        PartDefinition tailTip = tailMid.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(142, 281).mirror(false).addBox(-1.32f, -1.32f, 0, 2.64f, 2.64f, 6)
                .texOffs(0, 254).mirror(false).addBox(-3.6f, -3.6f, 4.2f, 7.2f, 7.2f, 6)
                .texOffs(322, 281).mirror(false).addBox(-2.64f, -2.64f, 10.08f, 5.28f, 5.28f, 1.8f), PartPose.offsetAndRotation(0, 0, 11.4f, 0.7f, 0, 0));
        PartDefinition tailFlame = tailTip.addOrReplaceChild("tail_flame", CubeListBuilder.create()
                .texOffs(37, 235).mirror(false).addBox(-1.44f, -7.8f, -4.56f, 2.88f, 7.8f, 9.12f), PartPose.offsetAndRotation(0, -1.2f, 8.4f, -1.1f, 0, 0));
        PartDefinition tailFlameMid = tailFlame.addOrReplaceChild("tail_flame_mid", CubeListBuilder.create()
                .texOffs(308, 235).mirror(false).addBox(-1.224f, -6.6f, -3.9216f, 2.448f, 6.6f, 6.0192f), PartPose.offsetAndRotation(0, -6.84f, 0, 0.35f, 0, 0));
        PartDefinition tailFlameTip = tailFlameMid.addOrReplaceChild("tail_flame_tip", CubeListBuilder.create()
                .texOffs(258, 269).mirror(false).addBox(-1.008f, -5.4f, -2.8272f, 2.016f, 5.4f, 3.1008f), PartPose.offsetAndRotation(0, -5.76f, 0, 0.4375f, 0, 0));
        PartDefinition rightTailFlame = tailTip.addOrReplaceChild("right_tail_flame", CubeListBuilder.create()
                .texOffs(294, 269).mirror(false).addBox(-3.648f, -6.24f, -1.152f, 7.296f, 6.24f, 2.304f), PartPose.offsetAndRotation(-2.4f, 0, 7.8f, -1.25f, 0, -0.75f));
        PartDefinition leftTailFlame = tailTip.addOrReplaceChild("left_tail_flame", CubeListBuilder.create()
                .texOffs(294, 269).mirror(true).addBox(-3.648f, -6.24f, -1.152f, 7.296f, 6.24f, 2.304f), PartPose.offsetAndRotation(2.4f, 0, 7.8f, -1.25f, 0, 0.75f));
        PartDefinition rightTailFlameMid = rightTailFlame.addOrReplaceChild("right_tail_flame_mid", CubeListBuilder.create()
                .texOffs(0, 292).mirror(false).addBox(-3.1373f, -5.28f, -0.9792f, 4.8154f, 5.28f, 1.9584f), PartPose.offsetAndRotation(0, -5.472f, 0, 0, 0, 0.3f));
        PartDefinition leftTailFlameMid = leftTailFlame.addOrReplaceChild("left_tail_flame_mid", CubeListBuilder.create()
                .texOffs(0, 292).mirror(true).addBox(-1.6781f, -5.28f, -0.9792f, 4.8154f, 5.28f, 1.9584f), PartPose.offsetAndRotation(0, -5.472f, 0, 0, -0, -0.3f));
        PartDefinition rightTailFlameTip = rightTailFlameMid.addOrReplaceChild("right_tail_flame_tip", CubeListBuilder.create()
                .texOffs(98, 292).mirror(false).addBox(-2.2618f, -4.32f, -0.8064f, 2.4806f, 4.32f, 1.6128f), PartPose.offsetAndRotation(0, -4.608f, 0, 0, 0, 0.375f));
        PartDefinition leftTailFlameTip = leftTailFlameMid.addOrReplaceChild("left_tail_flame_tip", CubeListBuilder.create()
                .texOffs(98, 292).mirror(true).addBox(-0.2189f, -4.32f, -0.8064f, 2.4806f, 4.32f, 1.6128f), PartPose.offsetAndRotation(0, -4.608f, 0, 0, -0, -0.375f));
        PartDefinition midNeck = body.addOrReplaceChild("mid_neck", CubeListBuilder.create()
                .texOffs(192, 0).mirror(false).addBox(-3.4f, -3.4f, -1, 6.8f, 6.8f, 26.5204f)
                .texOffs(214, 49).mirror(false).addBox(-4.08f, -5.08f, -1, 8.16f, 2.88f, 26.5204f), PartPose.offsetAndRotation(0, -13.2f, -24, 1.2168f, 3.1416f, 0));
        PartDefinition midNeck2 = midNeck.addOrReplaceChild("mid_neck2", CubeListBuilder.create()
                .texOffs(0, 113).mirror(false).addBox(-3.128f, -3.128f, -1, 6.256f, 6.256f, 19.7417f)
                .texOffs(81, 167).mirror(false).addBox(-3.468f, -4.84f, -1, 6.936f, 2.64f, 19.7417f), PartPose.offsetAndRotation(0, 0, 24.5204f, -0.4503f, -0, 0));
        PartDefinition midCrestA = midNeck.addOrReplaceChild("mid_crest_a", CubeListBuilder.create()
                .texOffs(80, 254).mirror(false).addBox(-1.02f, -5.525f, -3.23f, 2.04f, 5.525f, 6.46f), PartPose.offsetAndRotation(0, -4.4f, 9.8082f, -1.25f, 0, 0));
        PartDefinition midCrestAMid = midCrestA.addOrReplaceChild("mid_crest_a_mid", CubeListBuilder.create()
                .texOffs(228, 269).mirror(false).addBox(-0.867f, -4.675f, -2.7778f, 1.734f, 4.675f, 4.2636f), PartPose.offsetAndRotation(0, -4.845f, 0, 0.3f, 0, 0));
        PartDefinition midCrestATip = midCrestAMid.addOrReplaceChild("mid_crest_a_tip", CubeListBuilder.create()
                .texOffs(87, 292).mirror(false).addBox(-0.714f, -3.825f, -2.0026f, 1.428f, 3.825f, 2.1964f), PartPose.offsetAndRotation(0, -4.08f, 0, 0.375f, 0, 0));
        PartDefinition midHead = midNeck2.addOrReplaceChild("mid_head", CubeListBuilder.create()
                .texOffs(269, 167).mirror(false).addBox(-6.5f, -6, -10.8f, 13, 11, 11)
                .texOffs(161, 254).mirror(false).addBox(-6, -9, -9.6f, 12, 3.6f, 8.4f)
                .texOffs(29, 254).mirror(false).addBox(-8.4f, -0.6f, -9.6f, 16.8f, 6, 7.2f)
                .texOffs(0, 281).mirror(false).addBox(-3, -4.32f, -15.6f, 6, 3.6f, 5.4f)
                .texOffs(111, 269).mirror(false).addBox(-5.52f, -0.96f, -16.32f, 5.4f, 4.8f, 6)
                .texOffs(111, 269).mirror(false).addBox(0.12f, -0.96f, -16.32f, 5.4f, 4.8f, 6)
                .texOffs(136, 269).mirror(false).addBox(-0.96f, -0.96f, -15.84f, 1.92f, 4.32f, 5.4f)
                .texOffs(275, 292).mirror(false).addBox(-2.76f, -3.6f, -17.04f, 5.52f, 2.76f, 1.44f)
                .texOffs(120, 292).mirror(false).addBox(-5.4f, 3.12f, -15.84f, 1.68f, 4.32f, 1.68f)
                .texOffs(120, 292).mirror(false).addBox(3.72f, 3.12f, -15.84f, 1.68f, 4.32f, 1.68f)
                .texOffs(0, 301).mirror(false).addBox(-4.92f, 6.96f, -15.6f, 0.96f, 2.4f, 0.96f)
                .texOffs(0, 301).mirror(false).addBox(3.96f, 6.96f, -15.6f, 0.96f, 2.4f, 0.96f)
                .texOffs(5, 301).mirror(false).addBox(-3.6f, 3.48f, -16.08f, 7.2f, 1.44f, 0.96f)
                .texOffs(306, 254).mirror(false).addBox(-4.08f, 0.96f, -11.4f, 8.16f, 6.6f, 4.8f)
                .texOffs(0, 191).mirror(false).addBox(-10.2f, -9.6f, -4.8f, 20.4f, 15.6f, 6)
                .texOffs(213, 83).mirror(false).addBox(-7.2f, -12.6f, -4.8f, 14.4f, 21.6f, 6)
                .texOffs(123, 281).mirror(false).addBox(-6.12f, -13.56f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(15, 292).mirror(false).addBox(-10.56f, -10.56f, -6.24f, 4.32f, 4.32f, 3)
                .texOffs(123, 281).mirror(false).addBox(-13.08f, -6.12f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(15, 292).mirror(false).addBox(-12.72f, -0.24f, -6.24f, 4.32f, 4.32f, 3)
                .texOffs(123, 281).mirror(false).addBox(-10.44f, 3.96f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(15, 292).mirror(false).addBox(-5.04f, 6.72f, -6.24f, 4.32f, 4.32f, 3)
                .texOffs(123, 281).mirror(false).addBox(1.08f, -13.56f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(15, 292).mirror(false).addBox(6.24f, -10.56f, -6.24f, 4.32f, 4.32f, 3)
                .texOffs(123, 281).mirror(false).addBox(8.04f, -6.12f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(15, 292).mirror(false).addBox(8.4f, -0.24f, -6.24f, 4.32f, 4.32f, 3)
                .texOffs(123, 281).mirror(false).addBox(5.4f, 3.96f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(15, 292).mirror(false).addBox(0.72f, 6.72f, -6.24f, 4.32f, 4.32f, 3), PartPose.offsetAndRotation(0, 0, 18.7417f, -2.2551f, -0, 3.1416f));
        PartDefinition midBrowA = midHead.addOrReplaceChild("mid_brow_a", CubeListBuilder.create()
                .texOffs(221, 292).mirror(false).addBox(-7.44f, -1.44f, -0.96f, 7.44f, 2.88f, 2.64f), PartPose.offsetAndRotation(-0.36f, -5.28f, -11.28f, 0, 0, 0.45f));
        PartDefinition midBrowB = midHead.addOrReplaceChild("mid_brow_b", CubeListBuilder.create()
                .texOffs(221, 292).mirror(false).addBox(0, -1.44f, -0.96f, 7.44f, 2.88f, 2.64f), PartPose.offsetAndRotation(0.36f, -5.28f, -11.28f, 0, 0, -0.45f));
        PartDefinition midJaw = midHead.addOrReplaceChild("mid_jaw", CubeListBuilder.create()
                .texOffs(25, 281).mirror(false).addBox(-3.72f, 0, -6.96f, 7.44f, 2.64f, 6.96f)
                .texOffs(244, 292).mirror(false).addBox(-3.6f, -3.12f, -6.6f, 1.44f, 3.12f, 1.44f)
                .texOffs(244, 292).mirror(false).addBox(2.16f, -3.12f, -6.6f, 1.44f, 3.12f, 1.44f)
                .texOffs(24, 301).mirror(false).addBox(-1.92f, -1.44f, -6.72f, 3.84f, 1.44f, 0.96f)
                .texOffs(15, 292).mirror(false).addBox(-6.36f, 2.28f, -6.96f, 4.08f, 4.08f, 2.64f)
                .texOffs(15, 292).mirror(false).addBox(-2.04f, 2.76f, -7.44f, 4.08f, 4.08f, 2.64f)
                .texOffs(15, 292).mirror(false).addBox(2.28f, 2.28f, -6.96f, 4.08f, 4.08f, 2.64f), PartPose.offsetAndRotation(0, 3.36f, -9.6f, 0.35f, 0, 0));
        PartDefinition midLockTop = midHead.addOrReplaceChild("mid_lock_top", CubeListBuilder.create()
                .texOffs(109, 235).mirror(false).addBox(-1.368f, -7.41f, -4.332f, 2.736f, 7.41f, 8.664f), PartPose.offsetAndRotation(0, -11.4f, -1.2f, -1.3f, 0, 0));
        PartDefinition midLockTopMid = midLockTop.addOrReplaceChild("mid_lock_top_mid", CubeListBuilder.create()
                .texOffs(204, 254).mirror(false).addBox(-1.1628f, -6.27f, -3.7255f, 2.3256f, 6.27f, 5.7182f), PartPose.offsetAndRotation(0, -6.498f, 0, 0.3f, 0, 0));
        PartDefinition midLockTopTip = midLockTopMid.addOrReplaceChild("mid_lock_top_tip", CubeListBuilder.create()
                .texOffs(161, 281).mirror(false).addBox(-0.9576f, -5.13f, -2.6858f, 1.9152f, 5.13f, 2.9458f), PartPose.offsetAndRotation(0, -5.472f, 0, 0.375f, 0, 0));
        PartDefinition midLockA0 = midHead.addOrReplaceChild("mid_lock_a0", CubeListBuilder.create()
                .texOffs(223, 235).mirror(false).addBox(-1.152f, -6.24f, -3.648f, 2.304f, 6.24f, 7.296f), PartPose.offsetAndRotation(-6.6f, -10.8f, -0.6f, -1.15f, 0, -0.5f));
        PartDefinition midLockA0Mid = midLockA0.addOrReplaceChild("mid_lock_a0_mid", CubeListBuilder.create()
                .texOffs(153, 269).mirror(false).addBox(-0.9792f, -5.28f, -3.1373f, 1.9584f, 5.28f, 4.8154f), PartPose.offsetAndRotation(0, -5.472f, 0, 0.28f, 0, 0));
        PartDefinition midLockA0Tip = midLockA0Mid.addOrReplaceChild("mid_lock_a0_tip", CubeListBuilder.create()
                .texOffs(272, 281).mirror(false).addBox(-0.8064f, -4.32f, -2.2618f, 1.6128f, 4.32f, 2.4806f), PartPose.offsetAndRotation(0, -4.608f, 0, 0.35f, 0, 0));
        PartDefinition midLockA1 = midHead.addOrReplaceChild("mid_lock_a1", CubeListBuilder.create()
                .texOffs(294, 269).mirror(false).addBox(-3.876f, -6.63f, -1.224f, 7.752f, 6.63f, 2.448f), PartPose.offsetAndRotation(-10.2f, -4.2f, -0.6f, -1.07f, 0, -1.05f));
        PartDefinition midLockA1Mid = midLockA1.addOrReplaceChild("mid_lock_a1_mid", CubeListBuilder.create()
                .texOffs(104, 281).mirror(false).addBox(-3.3334f, -5.61f, -1.0404f, 5.1163f, 5.61f, 2.0808f), PartPose.offsetAndRotation(0, -5.814f, 0, 0, 0, 0.28f));
        PartDefinition midLockA1Tip = midLockA1Mid.addOrReplaceChild("mid_lock_a1_tip", CubeListBuilder.create()
                .texOffs(98, 292).mirror(false).addBox(-2.4031f, -4.59f, -0.8568f, 2.6357f, 4.59f, 1.7136f), PartPose.offsetAndRotation(0, -4.896f, 0, 0, 0, 0.35f));
        PartDefinition midLockA2 = midHead.addOrReplaceChild("mid_lock_a2", CubeListBuilder.create()
                .texOffs(294, 269).mirror(false).addBox(-3.648f, -6.24f, -1.152f, 7.296f, 6.24f, 2.304f), PartPose.offsetAndRotation(-10.2f, 3.6f, -0.6f, -0.99f, 0, -1.6f));
        PartDefinition midLockA2Mid = midLockA2.addOrReplaceChild("mid_lock_a2_mid", CubeListBuilder.create()
                .texOffs(0, 292).mirror(false).addBox(-3.1373f, -5.28f, -0.9792f, 4.8154f, 5.28f, 1.9584f), PartPose.offsetAndRotation(0, -5.472f, 0, 0, 0, 0.28f));
        PartDefinition midLockA2Tip = midLockA2Mid.addOrReplaceChild("mid_lock_a2_tip", CubeListBuilder.create()
                .texOffs(98, 292).mirror(false).addBox(-2.2618f, -4.32f, -0.8064f, 2.4806f, 4.32f, 1.6128f), PartPose.offsetAndRotation(0, -4.608f, 0, 0, 0, 0.35f));
        PartDefinition midLockB0 = midHead.addOrReplaceChild("mid_lock_b0", CubeListBuilder.create()
                .texOffs(223, 235).mirror(false).addBox(-1.152f, -6.24f, -3.648f, 2.304f, 6.24f, 7.296f), PartPose.offsetAndRotation(6.6f, -10.8f, -0.6f, -1.15f, 0, 0.5f));
        PartDefinition midLockB0Mid = midLockB0.addOrReplaceChild("mid_lock_b0_mid", CubeListBuilder.create()
                .texOffs(153, 269).mirror(false).addBox(-0.9792f, -5.28f, -3.1373f, 1.9584f, 5.28f, 4.8154f), PartPose.offsetAndRotation(0, -5.472f, 0, 0.28f, 0, 0));
        PartDefinition midLockB0Tip = midLockB0Mid.addOrReplaceChild("mid_lock_b0_tip", CubeListBuilder.create()
                .texOffs(272, 281).mirror(false).addBox(-0.8064f, -4.32f, -2.2618f, 1.6128f, 4.32f, 2.4806f), PartPose.offsetAndRotation(0, -4.608f, 0, 0.35f, 0, 0));
        PartDefinition midLockB1 = midHead.addOrReplaceChild("mid_lock_b1", CubeListBuilder.create()
                .texOffs(294, 269).mirror(false).addBox(-3.876f, -6.63f, -1.224f, 7.752f, 6.63f, 2.448f), PartPose.offsetAndRotation(10.2f, -4.2f, -0.6f, -1.07f, 0, 1.05f));
        PartDefinition midLockB1Mid = midLockB1.addOrReplaceChild("mid_lock_b1_mid", CubeListBuilder.create()
                .texOffs(104, 281).mirror(false).addBox(-3.3334f, -5.61f, -1.0404f, 5.1163f, 5.61f, 2.0808f), PartPose.offsetAndRotation(0, -5.814f, 0, 0, 0, -0.28f));
        PartDefinition midLockB1Tip = midLockB1Mid.addOrReplaceChild("mid_lock_b1_tip", CubeListBuilder.create()
                .texOffs(98, 292).mirror(false).addBox(-2.4031f, -4.59f, -0.8568f, 2.6357f, 4.59f, 1.7136f), PartPose.offsetAndRotation(0, -4.896f, 0, 0, 0, -0.35f));
        PartDefinition midLockB2 = midHead.addOrReplaceChild("mid_lock_b2", CubeListBuilder.create()
                .texOffs(294, 269).mirror(false).addBox(-3.648f, -6.24f, -1.152f, 7.296f, 6.24f, 2.304f), PartPose.offsetAndRotation(10.2f, 3.6f, -0.6f, -0.99f, 0, 1.6f));
        PartDefinition midLockB2Mid = midLockB2.addOrReplaceChild("mid_lock_b2_mid", CubeListBuilder.create()
                .texOffs(0, 292).mirror(false).addBox(-3.1373f, -5.28f, -0.9792f, 4.8154f, 5.28f, 1.9584f), PartPose.offsetAndRotation(0, -5.472f, 0, 0, 0, -0.28f));
        PartDefinition midLockB2Tip = midLockB2Mid.addOrReplaceChild("mid_lock_b2_tip", CubeListBuilder.create()
                .texOffs(98, 292).mirror(false).addBox(-2.2618f, -4.32f, -0.8064f, 2.4806f, 4.32f, 1.6128f), PartPose.offsetAndRotation(0, -4.608f, 0, 0, 0, -0.35f));
        PartDefinition rightH1Neck = body.addOrReplaceChild("right_h1_neck", CubeListBuilder.create()
                .texOffs(287, 49).mirror(false).addBox(-2.6f, -2.6f, -1, 5.2f, 5.2f, 23.9789f)
                .texOffs(169, 113).mirror(false).addBox(-1.82f, -3.58f, -1, 3.64f, 1.68f, 23.9789f), PartPose.offsetAndRotation(-4.2f, -6, -28.8f, 0.6614f, -2.9736f, 0));
        PartDefinition rightH1Neck2 = rightH1Neck.addOrReplaceChild("right_h1_neck2", CubeListBuilder.create()
                .texOffs(95, 83).mirror(false).addBox(-2.392f, -2.392f, -1, 4.784f, 4.784f, 23.7796f)
                .texOffs(169, 113).mirror(false).addBox(-1.547f, -3.44f, -1, 3.094f, 1.54f, 23.7796f), PartPose.offsetAndRotation(0, 0, 21.9789f, -0.4539f, -0.0296f, 0.0231f));
        PartDefinition rightH1Head = rightH1Neck2.addOrReplaceChild("right_h1_head", CubeListBuilder.create()
                .texOffs(157, 235).mirror(false).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(56, 281).mirror(false).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(168, 269).mirror(false).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(32, 292).mirror(false).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(172, 281).mirror(false).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(172, 281).mirror(false).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(193, 281).mirror(false).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(292, 292).mirror(false).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(244, 292).mirror(false).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(244, 292).mirror(false).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(35, 301).mirror(false).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(35, 301).mirror(false).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(40, 301).mirror(false).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(208, 281).mirror(false).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(223, 254).mirror(false).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(129, 292).mirror(false).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(false).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(false).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 22.7796f, -3.0081f, -0.0762f, -3.1175f));
        PartDefinition rightH1BrowA = rightH1Head.addOrReplaceChild("right_h1_brow_a", CubeListBuilder.create()
                .texOffs(307, 292).mirror(false).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition rightH1BrowB = rightH1Head.addOrReplaceChild("right_h1_brow_b", CubeListBuilder.create()
                .texOffs(307, 292).mirror(false).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition rightH1Jaw = rightH1Head.addOrReplaceChild("right_h1_jaw", CubeListBuilder.create()
                .texOffs(53, 292).mirror(false).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(324, 292).mirror(false).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(324, 292).mirror(false).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(55, 301).mirror(false).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(333, 292).mirror(false).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(333, 292).mirror(false).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition rightH1LockTop = rightH1Head.addOrReplaceChild("right_h1_lock_top", CubeListBuilder.create()
                .texOffs(231, 281).mirror(false).addBox(-0.8f, -3.8025f, -2.223f, 1.6f, 3.8025f, 4.446f), PartPose.offsetAndRotation(0, -8.55f, -0.9f, -1.3f, 0, 0));
        PartDefinition rightH1LockTopMid = rightH1LockTop.addOrReplaceChild("right_h1_lock_top_mid", CubeListBuilder.create()
                .texOffs(144, 292).mirror(false).addBox(-0.68f, -3.2175f, -1.9118f, 1.36f, 3.2175f, 2.9344f), PartPose.offsetAndRotation(0, -3.3345f, 0, 0.3f, 0, 0));
        PartDefinition rightH1LockTopTip = rightH1LockTopMid.addOrReplaceChild("right_h1_lock_top_tip", CubeListBuilder.create()
                .texOffs(344, 292).mirror(false).addBox(-0.56f, -2.6325f, -1.3783f, 1.12f, 2.6325f, 1.5116f), PartPose.offsetAndRotation(0, -2.808f, 0, 0.375f, 0, 0));
        PartDefinition leftH1Neck = body.addOrReplaceChild("left_h1_neck", CubeListBuilder.create()
                .texOffs(287, 49).mirror(true).addBox(-2.6f, -2.6f, -1, 5.2f, 5.2f, 23.1983f)
                .texOffs(169, 113).mirror(true).addBox(-1.82f, -3.58f, -1, 3.64f, 1.68f, 23.1983f), PartPose.offsetAndRotation(4.2f, -6, -28.8f, 0.8553f, 2.9315f, 0));
        PartDefinition leftH1Neck2 = leftH1Neck.addOrReplaceChild("left_h1_neck2", CubeListBuilder.create()
                .texOffs(55, 113).mirror(true).addBox(-2.392f, -2.392f, -1, 4.784f, 4.784f, 21.1547f)
                .texOffs(250, 141).mirror(true).addBox(-1.547f, -3.44f, -1, 3.094f, 1.54f, 21.1547f), PartPose.offsetAndRotation(0, 0, 21.1983f, -0.4908f, 0.0363f, -0.0418f));
        PartDefinition leftH1Head = leftH1Neck2.addOrReplaceChild("left_h1_head", CubeListBuilder.create()
                .texOffs(157, 235).mirror(true).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(56, 281).mirror(true).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(168, 269).mirror(true).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(32, 292).mirror(true).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(172, 281).mirror(true).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(172, 281).mirror(true).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(193, 281).mirror(true).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(292, 292).mirror(true).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(244, 292).mirror(true).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(244, 292).mirror(true).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(35, 301).mirror(true).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(35, 301).mirror(true).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(40, 301).mirror(true).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(208, 281).mirror(true).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(223, 254).mirror(true).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(129, 292).mirror(true).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(true).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(true).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 20.1547f, -2.8498f, 0.0565f, 3.1212f));
        PartDefinition leftH1BrowA = leftH1Head.addOrReplaceChild("left_h1_brow_a", CubeListBuilder.create()
                .texOffs(307, 292).mirror(true).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition leftH1BrowB = leftH1Head.addOrReplaceChild("left_h1_brow_b", CubeListBuilder.create()
                .texOffs(307, 292).mirror(true).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition leftH1Jaw = leftH1Head.addOrReplaceChild("left_h1_jaw", CubeListBuilder.create()
                .texOffs(53, 292).mirror(true).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(324, 292).mirror(true).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(324, 292).mirror(true).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(55, 301).mirror(true).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(333, 292).mirror(true).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(333, 292).mirror(true).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition leftH1LockTop = leftH1Head.addOrReplaceChild("left_h1_lock_top", CubeListBuilder.create()
                .texOffs(231, 281).mirror(true).addBox(-0.8f, -3.8025f, -2.223f, 1.6f, 3.8025f, 4.446f), PartPose.offsetAndRotation(0, -8.55f, -0.9f, -1.3f, 0, 0));
        PartDefinition leftH1LockTopMid = leftH1LockTop.addOrReplaceChild("left_h1_lock_top_mid", CubeListBuilder.create()
                .texOffs(144, 292).mirror(true).addBox(-0.68f, -3.2175f, -1.9118f, 1.36f, 3.2175f, 2.9344f), PartPose.offsetAndRotation(0, -3.3345f, 0, 0.3f, -0, 0));
        PartDefinition leftH1LockTopTip = leftH1LockTopMid.addOrReplaceChild("left_h1_lock_top_tip", CubeListBuilder.create()
                .texOffs(344, 292).mirror(true).addBox(-0.56f, -2.6325f, -1.3783f, 1.12f, 2.6325f, 1.5116f), PartPose.offsetAndRotation(0, -2.808f, 0, 0.375f, -0, 0));
        PartDefinition rightH2Neck = body.addOrReplaceChild("right_h2_neck", CubeListBuilder.create()
                .texOffs(287, 49).mirror(false).addBox(-2.6f, -2.6f, -1, 5.2f, 5.2f, 23.3645f)
                .texOffs(169, 113).mirror(false).addBox(-1.82f, -3.58f, -1, 3.64f, 1.68f, 23.3645f), PartPose.offsetAndRotation(-7.8f, -14.4f, -15.6f, 1.2391f, -1.8925f, 0));
        PartDefinition rightH2Neck2 = rightH2Neck.addOrReplaceChild("right_h2_neck2", CubeListBuilder.create()
                .texOffs(55, 191).mirror(false).addBox(-2.392f, -2.392f, -1, 4.784f, 4.784f, 16.9424f)
                .texOffs(161, 214).mirror(false).addBox(-1.547f, -3.44f, -1, 3.094f, 1.54f, 16.9424f), PartPose.offsetAndRotation(0, 0, 21.3645f, -0.367f, -0.1588f, 0.4837f));
        PartDefinition rightH2Head = rightH2Neck2.addOrReplaceChild("right_h2_head", CubeListBuilder.create()
                .texOffs(157, 235).mirror(false).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(56, 281).mirror(false).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(168, 269).mirror(false).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(32, 292).mirror(false).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(172, 281).mirror(false).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(172, 281).mirror(false).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(193, 281).mirror(false).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(292, 292).mirror(false).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(244, 292).mirror(false).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(244, 292).mirror(false).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(35, 301).mirror(false).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(35, 301).mirror(false).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(40, 301).mirror(false).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(208, 281).mirror(false).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(223, 254).mirror(false).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(129, 292).mirror(false).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(false).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(false).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 15.9424f, -2.2357f, 0.1202f, -2.8309f));
        PartDefinition rightH2BrowA = rightH2Head.addOrReplaceChild("right_h2_brow_a", CubeListBuilder.create()
                .texOffs(307, 292).mirror(false).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition rightH2BrowB = rightH2Head.addOrReplaceChild("right_h2_brow_b", CubeListBuilder.create()
                .texOffs(307, 292).mirror(false).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition rightH2Jaw = rightH2Head.addOrReplaceChild("right_h2_jaw", CubeListBuilder.create()
                .texOffs(53, 292).mirror(false).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(324, 292).mirror(false).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(324, 292).mirror(false).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(55, 301).mirror(false).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(333, 292).mirror(false).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(333, 292).mirror(false).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition rightH2LockTop = rightH2Head.addOrReplaceChild("right_h2_lock_top", CubeListBuilder.create()
                .texOffs(231, 281).mirror(false).addBox(-0.8f, -3.8025f, -2.223f, 1.6f, 3.8025f, 4.446f), PartPose.offsetAndRotation(0, -8.55f, -0.9f, -1.3f, 0, 0));
        PartDefinition rightH2LockTopMid = rightH2LockTop.addOrReplaceChild("right_h2_lock_top_mid", CubeListBuilder.create()
                .texOffs(144, 292).mirror(false).addBox(-0.68f, -3.2175f, -1.9118f, 1.36f, 3.2175f, 2.9344f), PartPose.offsetAndRotation(0, -3.3345f, 0, 0.3f, 0, 0));
        PartDefinition rightH2LockTopTip = rightH2LockTopMid.addOrReplaceChild("right_h2_lock_top_tip", CubeListBuilder.create()
                .texOffs(344, 292).mirror(false).addBox(-0.56f, -2.6325f, -1.3783f, 1.12f, 2.6325f, 1.5116f), PartPose.offsetAndRotation(0, -2.808f, 0, 0.375f, 0, 0));
        PartDefinition leftH2Neck = body.addOrReplaceChild("left_h2_neck", CubeListBuilder.create()
                .texOffs(154, 83).mirror(true).addBox(-2.6f, -2.6f, -1, 5.2f, 5.2f, 22.0459f)
                .texOffs(79, 141).mirror(true).addBox(-1.82f, -3.58f, -1, 3.64f, 1.68f, 22.0459f), PartPose.offsetAndRotation(7.8f, -14.4f, -15.6f, 1.1383f, 2.2381f, 0));
        PartDefinition leftH2Neck2 = leftH2Neck.addOrReplaceChild("left_h2_neck2", CubeListBuilder.create()
                .texOffs(136, 167).mirror(true).addBox(-2.392f, -2.392f, -1, 4.784f, 4.784f, 17.1233f)
                .texOffs(0, 214).mirror(true).addBox(-1.547f, -3.44f, -1, 3.094f, 1.54f, 17.1233f), PartPose.offsetAndRotation(0, 0, 20.0459f, -0.4415f, 0.1359f, -0.3007f));
        PartDefinition leftH2Head = leftH2Neck2.addOrReplaceChild("left_h2_head", CubeListBuilder.create()
                .texOffs(157, 235).mirror(true).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(56, 281).mirror(true).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(168, 269).mirror(true).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(32, 292).mirror(true).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(172, 281).mirror(true).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(172, 281).mirror(true).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(193, 281).mirror(true).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(292, 292).mirror(true).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(244, 292).mirror(true).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(244, 292).mirror(true).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(35, 301).mirror(true).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(35, 301).mirror(true).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(40, 301).mirror(true).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(208, 281).mirror(true).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(223, 254).mirror(true).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(129, 292).mirror(true).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(true).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(true).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 16.1233f, -2.4517f, -0.054f, 2.9724f));
        PartDefinition leftH2BrowA = leftH2Head.addOrReplaceChild("left_h2_brow_a", CubeListBuilder.create()
                .texOffs(307, 292).mirror(true).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition leftH2BrowB = leftH2Head.addOrReplaceChild("left_h2_brow_b", CubeListBuilder.create()
                .texOffs(307, 292).mirror(true).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition leftH2Jaw = leftH2Head.addOrReplaceChild("left_h2_jaw", CubeListBuilder.create()
                .texOffs(53, 292).mirror(true).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(324, 292).mirror(true).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(324, 292).mirror(true).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(55, 301).mirror(true).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(333, 292).mirror(true).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(333, 292).mirror(true).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition leftH2LockTop = leftH2Head.addOrReplaceChild("left_h2_lock_top", CubeListBuilder.create()
                .texOffs(231, 281).mirror(true).addBox(-0.8f, -3.8025f, -2.223f, 1.6f, 3.8025f, 4.446f), PartPose.offsetAndRotation(0, -8.55f, -0.9f, -1.3f, 0, 0));
        PartDefinition leftH2LockTopMid = leftH2LockTop.addOrReplaceChild("left_h2_lock_top_mid", CubeListBuilder.create()
                .texOffs(144, 292).mirror(true).addBox(-0.68f, -3.2175f, -1.9118f, 1.36f, 3.2175f, 2.9344f), PartPose.offsetAndRotation(0, -3.3345f, 0, 0.3f, -0, 0));
        PartDefinition leftH2LockTopTip = leftH2LockTopMid.addOrReplaceChild("left_h2_lock_top_tip", CubeListBuilder.create()
                .texOffs(344, 292).mirror(true).addBox(-0.56f, -2.6325f, -1.3783f, 1.12f, 2.6325f, 1.5116f), PartPose.offsetAndRotation(0, -2.808f, 0, 0.375f, -0, 0));
        PartDefinition rightH3Neck = body.addOrReplaceChild("right_h3_neck", CubeListBuilder.create()
                .texOffs(134, 141).mirror(false).addBox(-2.6f, -2.6f, -1, 5.2f, 5.2f, 18.2622f)
                .texOffs(170, 191).mirror(false).addBox(-1.82f, -3.58f, -1, 3.64f, 1.68f, 18.2622f), PartPose.offsetAndRotation(-9.6f, -4.8f, -25.2f, 0.6545f, -2.2298f, 0));
        PartDefinition rightH3Neck2 = rightH3Neck.addOrReplaceChild("right_h3_neck2", CubeListBuilder.create()
                .texOffs(136, 167).mirror(false).addBox(-2.392f, -2.392f, -1, 4.784f, 4.784f, 17.8137f)
                .texOffs(0, 214).mirror(false).addBox(-1.547f, -3.44f, -1, 3.094f, 1.54f, 17.8137f), PartPose.offsetAndRotation(0, 0, 16.2622f, -0.4592f, -0.1919f, 0.1496f));
        PartDefinition rightH3Head = rightH3Neck2.addOrReplaceChild("right_h3_head", CubeListBuilder.create()
                .texOffs(157, 235).mirror(false).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(56, 281).mirror(false).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(168, 269).mirror(false).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(32, 292).mirror(false).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(172, 281).mirror(false).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(172, 281).mirror(false).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(193, 281).mirror(false).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(292, 292).mirror(false).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(244, 292).mirror(false).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(244, 292).mirror(false).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(35, 301).mirror(false).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(35, 301).mirror(false).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(40, 301).mirror(false).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(208, 281).mirror(false).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(223, 254).mirror(false).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(129, 292).mirror(false).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(false).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(false).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 16.8137f, -2.9254f, 0.0864f, -3.0006f));
        PartDefinition rightH3BrowA = rightH3Head.addOrReplaceChild("right_h3_brow_a", CubeListBuilder.create()
                .texOffs(307, 292).mirror(false).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition rightH3BrowB = rightH3Head.addOrReplaceChild("right_h3_brow_b", CubeListBuilder.create()
                .texOffs(307, 292).mirror(false).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition rightH3Jaw = rightH3Head.addOrReplaceChild("right_h3_jaw", CubeListBuilder.create()
                .texOffs(53, 292).mirror(false).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(324, 292).mirror(false).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(324, 292).mirror(false).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(55, 301).mirror(false).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(333, 292).mirror(false).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(333, 292).mirror(false).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition rightH3LockTop = rightH3Head.addOrReplaceChild("right_h3_lock_top", CubeListBuilder.create()
                .texOffs(231, 281).mirror(false).addBox(-0.8f, -3.8025f, -2.223f, 1.6f, 3.8025f, 4.446f), PartPose.offsetAndRotation(0, -8.55f, -0.9f, -1.3f, 0, 0));
        PartDefinition rightH3LockTopMid = rightH3LockTop.addOrReplaceChild("right_h3_lock_top_mid", CubeListBuilder.create()
                .texOffs(144, 292).mirror(false).addBox(-0.68f, -3.2175f, -1.9118f, 1.36f, 3.2175f, 2.9344f), PartPose.offsetAndRotation(0, -3.3345f, 0, 0.3f, 0, 0));
        PartDefinition rightH3LockTopTip = rightH3LockTopMid.addOrReplaceChild("right_h3_lock_top_tip", CubeListBuilder.create()
                .texOffs(344, 292).mirror(false).addBox(-0.56f, -2.6325f, -1.3783f, 1.12f, 2.6325f, 1.5116f), PartPose.offsetAndRotation(0, -2.808f, 0, 0.375f, 0, 0));
        PartDefinition leftH3Neck = body.addOrReplaceChild("left_h3_neck", CubeListBuilder.create()
                .texOffs(134, 141).mirror(true).addBox(-2.6f, -2.6f, -1, 5.2f, 5.2f, 18.7872f)
                .texOffs(170, 191).mirror(true).addBox(-1.82f, -3.58f, -1, 3.64f, 1.68f, 18.7872f), PartPose.offsetAndRotation(9.6f, -4.8f, -25.2f, 0.8311f, 2.0186f, 0));
        PartDefinition leftH3Neck2 = leftH3Neck.addOrReplaceChild("left_h3_neck2", CubeListBuilder.create()
                .texOffs(55, 191).mirror(true).addBox(-2.392f, -2.392f, -1, 4.784f, 4.784f, 16.4289f)
                .texOffs(161, 214).mirror(true).addBox(-1.547f, -3.44f, -1, 3.094f, 1.54f, 16.4289f), PartPose.offsetAndRotation(0, 0, 16.7872f, -0.458f, 0.2154f, -0.2421f));
        PartDefinition leftH3Head = leftH3Neck2.addOrReplaceChild("left_h3_head", CubeListBuilder.create()
                .texOffs(157, 235).mirror(true).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(56, 281).mirror(true).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(168, 269).mirror(true).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(32, 292).mirror(true).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(172, 281).mirror(true).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(172, 281).mirror(true).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(193, 281).mirror(true).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(292, 292).mirror(true).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(244, 292).mirror(true).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(244, 292).mirror(true).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(35, 301).mirror(true).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(35, 301).mirror(true).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(40, 301).mirror(true).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(208, 281).mirror(true).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(223, 254).mirror(true).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(129, 292).mirror(true).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(true).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(true).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 15.4289f, -2.7153f, -0.1774f, 2.9332f));
        PartDefinition leftH3BrowA = leftH3Head.addOrReplaceChild("left_h3_brow_a", CubeListBuilder.create()
                .texOffs(307, 292).mirror(true).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition leftH3BrowB = leftH3Head.addOrReplaceChild("left_h3_brow_b", CubeListBuilder.create()
                .texOffs(307, 292).mirror(true).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition leftH3Jaw = leftH3Head.addOrReplaceChild("left_h3_jaw", CubeListBuilder.create()
                .texOffs(53, 292).mirror(true).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(324, 292).mirror(true).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(324, 292).mirror(true).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(55, 301).mirror(true).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(333, 292).mirror(true).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(333, 292).mirror(true).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition leftH3LockTop = leftH3Head.addOrReplaceChild("left_h3_lock_top", CubeListBuilder.create()
                .texOffs(231, 281).mirror(true).addBox(-0.8f, -3.8025f, -2.223f, 1.6f, 3.8025f, 4.446f), PartPose.offsetAndRotation(0, -8.55f, -0.9f, -1.3f, 0, 0));
        PartDefinition leftH3LockTopMid = leftH3LockTop.addOrReplaceChild("left_h3_lock_top_mid", CubeListBuilder.create()
                .texOffs(144, 292).mirror(true).addBox(-0.68f, -3.2175f, -1.9118f, 1.36f, 3.2175f, 2.9344f), PartPose.offsetAndRotation(0, -3.3345f, 0, 0.3f, -0, 0));
        PartDefinition leftH3LockTopTip = leftH3LockTopMid.addOrReplaceChild("left_h3_lock_top_tip", CubeListBuilder.create()
                .texOffs(344, 292).mirror(true).addBox(-0.56f, -2.6325f, -1.3783f, 1.12f, 2.6325f, 1.5116f), PartPose.offsetAndRotation(0, -2.808f, 0, 0.375f, -0, 0));
        PartDefinition rightH4Neck = body.addOrReplaceChild("right_h4_neck", CubeListBuilder.create()
                .texOffs(151, 49).mirror(false).addBox(-2.6f, -2.6f, -1, 5.2f, 5.2f, 24.1057f)
                .texOffs(110, 113).mirror(false).addBox(-1.82f, -3.58f, -1, 3.64f, 1.68f, 24.1057f), PartPose.offsetAndRotation(-11.4f, -13.2f, -7.2f, 0.6627f, -1.45f, 0));
        PartDefinition rightH4Neck2 = rightH4Neck.addOrReplaceChild("right_h4_neck2", CubeListBuilder.create()
                .texOffs(226, 113).mirror(false).addBox(-2.392f, -2.392f, -1, 4.784f, 4.784f, 20.4489f)
                .texOffs(183, 167).mirror(false).addBox(-1.547f, -3.44f, -1, 3.094f, 1.54f, 20.4489f), PartPose.offsetAndRotation(0, 0, 22.1057f, -0.2364f, -0.225f, 0.1795f));
        PartDefinition rightH4Head = rightH4Neck2.addOrReplaceChild("right_h4_head", CubeListBuilder.create()
                .texOffs(157, 235).mirror(false).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(56, 281).mirror(false).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(168, 269).mirror(false).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(32, 292).mirror(false).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(172, 281).mirror(false).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(172, 281).mirror(false).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(193, 281).mirror(false).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(292, 292).mirror(false).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(244, 292).mirror(false).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(244, 292).mirror(false).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(35, 301).mirror(false).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(35, 301).mirror(false).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(40, 301).mirror(false).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(208, 281).mirror(false).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(223, 254).mirror(false).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(129, 292).mirror(false).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(false).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(false).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(false).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 19.4489f, -2.4665f, 0.4011f, -2.7387f));
        PartDefinition rightH4BrowA = rightH4Head.addOrReplaceChild("right_h4_brow_a", CubeListBuilder.create()
                .texOffs(307, 292).mirror(false).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition rightH4BrowB = rightH4Head.addOrReplaceChild("right_h4_brow_b", CubeListBuilder.create()
                .texOffs(307, 292).mirror(false).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition rightH4Jaw = rightH4Head.addOrReplaceChild("right_h4_jaw", CubeListBuilder.create()
                .texOffs(53, 292).mirror(false).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(324, 292).mirror(false).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(324, 292).mirror(false).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(55, 301).mirror(false).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(333, 292).mirror(false).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(333, 292).mirror(false).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition rightH4LockTop = rightH4Head.addOrReplaceChild("right_h4_lock_top", CubeListBuilder.create()
                .texOffs(231, 281).mirror(false).addBox(-0.8f, -3.8025f, -2.223f, 1.6f, 3.8025f, 4.446f), PartPose.offsetAndRotation(0, -8.55f, -0.9f, -1.3f, 0, 0));
        PartDefinition rightH4LockTopMid = rightH4LockTop.addOrReplaceChild("right_h4_lock_top_mid", CubeListBuilder.create()
                .texOffs(144, 292).mirror(false).addBox(-0.68f, -3.2175f, -1.9118f, 1.36f, 3.2175f, 2.9344f), PartPose.offsetAndRotation(0, -3.3345f, 0, 0.3f, 0, 0));
        PartDefinition rightH4LockTopTip = rightH4LockTopMid.addOrReplaceChild("right_h4_lock_top_tip", CubeListBuilder.create()
                .texOffs(344, 292).mirror(false).addBox(-0.56f, -2.6325f, -1.3783f, 1.12f, 2.6325f, 1.5116f), PartPose.offsetAndRotation(0, -2.808f, 0, 0.375f, 0, 0));
        PartDefinition leftH4Neck = body.addOrReplaceChild("left_h4_neck", CubeListBuilder.create()
                .texOffs(154, 83).mirror(true).addBox(-2.6f, -2.6f, -1, 5.2f, 5.2f, 22.5745f)
                .texOffs(79, 141).mirror(true).addBox(-1.82f, -3.58f, -1, 3.64f, 1.68f, 22.5745f), PartPose.offsetAndRotation(11.4f, -13.2f, -7.2f, 0.5698f, 1.6228f, 0));
        PartDefinition leftH4Neck2 = leftH4Neck.addOrReplaceChild("left_h4_neck2", CubeListBuilder.create()
                .texOffs(226, 113).mirror(true).addBox(-2.392f, -2.392f, -1, 4.784f, 4.784f, 20.207f)
                .texOffs(183, 167).mirror(true).addBox(-1.547f, -3.44f, -1, 3.094f, 1.54f, 20.207f), PartPose.offsetAndRotation(0, 0, 20.5745f, -0.2754f, 0.2321f, -0.152f));
        PartDefinition leftH4Head = leftH4Neck2.addOrReplaceChild("left_h4_head", CubeListBuilder.create()
                .texOffs(157, 235).mirror(true).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(56, 281).mirror(true).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(168, 269).mirror(true).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(32, 292).mirror(true).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(172, 281).mirror(true).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(172, 281).mirror(true).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(193, 281).mirror(true).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(292, 292).mirror(true).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(244, 292).mirror(true).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(244, 292).mirror(true).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(35, 301).mirror(true).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(35, 301).mirror(true).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(40, 301).mirror(true).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(208, 281).mirror(true).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(223, 254).mirror(true).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(129, 292).mirror(true).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(true).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(129, 292).mirror(true).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(253, 292).mirror(true).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 19.207f, -2.6548f, -0.3031f, 2.8644f));
        PartDefinition leftH4BrowA = leftH4Head.addOrReplaceChild("left_h4_brow_a", CubeListBuilder.create()
                .texOffs(307, 292).mirror(true).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition leftH4BrowB = leftH4Head.addOrReplaceChild("left_h4_brow_b", CubeListBuilder.create()
                .texOffs(307, 292).mirror(true).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition leftH4Jaw = leftH4Head.addOrReplaceChild("left_h4_jaw", CubeListBuilder.create()
                .texOffs(53, 292).mirror(true).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(324, 292).mirror(true).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(324, 292).mirror(true).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(55, 301).mirror(true).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(333, 292).mirror(true).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(333, 292).mirror(true).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition leftH4LockTop = leftH4Head.addOrReplaceChild("left_h4_lock_top", CubeListBuilder.create()
                .texOffs(231, 281).mirror(true).addBox(-0.8f, -3.8025f, -2.223f, 1.6f, 3.8025f, 4.446f), PartPose.offsetAndRotation(0, -8.55f, -0.9f, -1.3f, 0, 0));
        PartDefinition leftH4LockTopMid = leftH4LockTop.addOrReplaceChild("left_h4_lock_top_mid", CubeListBuilder.create()
                .texOffs(144, 292).mirror(true).addBox(-0.68f, -3.2175f, -1.9118f, 1.36f, 3.2175f, 2.9344f), PartPose.offsetAndRotation(0, -3.3345f, 0, 0.3f, -0, 0));
        PartDefinition leftH4LockTopTip = leftH4LockTopMid.addOrReplaceChild("left_h4_lock_top_tip", CubeListBuilder.create()
                .texOffs(344, 292).mirror(true).addBox(-0.56f, -2.6325f, -1.3783f, 1.12f, 2.6325f, 1.5116f), PartPose.offsetAndRotation(0, -2.808f, 0, 0.375f, -0, 0));
        return LayerDefinition.create(mesh, 352, 352);
        // GENERATED: end
    }

    /** The nine heads, in the order they wake: the middle, then out from it, right and left in turn. */
    static final String[] HEADS = {"mid", "right_h1", "left_h1", "right_h2", "left_h2", "right_h3", "left_h3", "right_h4", "left_h4"};
    private static final String[] BODY_LOCKS = {"spine_a", "spine_b", "spine_c", "apron", "right_apron", "left_apron", "right_apron_b", "left_apron_b",
            "right_mane_a", "left_mane_a", "right_mane_b", "left_mane_b", "right_mane_c", "left_mane_c",
            "right_mane_d", "left_mane_d", "right_mane_e", "left_mane_e", "right_mane_f", "left_mane_f",
            "right_mane_g", "left_mane_g"};
    /** A dormant head's eyes, against a waking one's: dimmer. */
    private static final int DIM = 0xFF8C8C8C;

    public static final class Model extends EntityModel<NineHeadedLionEntity> {
        private final ModelPart root, body, rightFore, leftFore, rightHind, leftHind, tail, tailMid, tailTip;
        private final ModelPart[] necks = new ModelPart[9], necks2 = new ModelPart[9], heads = new ModelPart[9], jaws = new ModelPart[9];
        private final ModelPart[][] locks = new ModelPart[9][];
        private final ModelPart[] bodyLocks = new ModelPart[BODY_LOCKS.length];
        /** How many heads are awake: those that strike with each blow. */
        int awake = 2;

        public Model(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            rightFore = body.getChild("right_foreleg");
            leftFore = body.getChild("left_foreleg");
            rightHind = body.getChild("right_hindleg");
            leftHind = body.getChild("left_hindleg");
            tail = body.getChild("tail");
            tailMid = tail.getChild("tail_mid");
            tailTip = tailMid.getChild("tail_tip");
            for (int i = 0; i < 9; i++) {
                String n = HEADS[i];
                necks[i] = body.getChild(n + "_neck");
                necks2[i] = necks[i].getChild(n + "_neck2");
                heads[i] = necks2[i].getChild(n + "_head");
                jaws[i] = heads[i].getChild(n + "_jaw");
                // the middle head's locks and the crest along its neck
                java.util.List<ModelPart> l = new java.util.ArrayList<>();
                if (necks[i].hasChild(n + "_crest_a")) l.add(necks[i].getChild(n + "_crest_a"));
                if (heads[i].hasChild(n + "_lock_top")) l.add(heads[i].getChild(n + "_lock_top"));
                for (int j = 0; heads[i].hasChild(n + "_lock_a" + j); j++) {
                    l.add(heads[i].getChild(n + "_lock_a" + j));
                    l.add(heads[i].getChild(n + "_lock_b" + j));
                }
                locks[i] = l.toArray(new ModelPart[0]);
            }
            for (int i = 0; i < BODY_LOCKS.length; i++) bodyLocks[i] = body.getChild(BODY_LOCKS[i]);
        }

        @Override
        public void setupAnim(NineHeadedLionEntity e, float swing, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            // a heavy, prowling stride, the diagonal legs together, the body rolling with it
            float gait = Mth.cos(swing * 0.45f) * 0.6f * amount;
            rightFore.xRot = leftHind.xRot = gait;
            leftFore.xRot = rightHind.xRot = -gait;
            body.zRot = Mth.sin(swing * 0.45f) * 0.03f * amount;
            body.xRot = Mth.sin(age * 0.05f) * 0.01f;                       // his breath
            float lookYaw = yaw * Mth.DEG_TO_RAD, lookPitch = pitch * Mth.DEG_TO_RAD;
            float blow = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            for (int i = 0; i < 9; i++) {
                float ph = i * 1.7f;
                // each head sways on its own beat, all turned toward what he watches
                necks[i].yRot += Mth.sin(age * 0.045f + ph) * 0.07f + lookYaw * 0.2f;
                necks[i].xRot += Mth.sin(age * 0.06f + ph * 1.3f) * 0.05f - lookPitch * 0.15f;
                necks2[i].xRot += Mth.sin(age * 0.06f + ph + 0.8f) * 0.06f;
                necks2[i].yRot += Mth.sin(age * 0.05f + ph + 1.1f) * 0.08f;
                heads[i].xRot += lookPitch * 0.3f + Mth.sin(age * 0.07f + ph) * 0.05f;
                heads[i].yRot += lookYaw * 0.3f;
                heads[i].zRot += Mth.sin(age * 0.04f + ph) * 0.06f;
                // the jaws working; now and then a head throws itself up and roars
                float roar = Mth.clamp(Mth.sin(age * 0.035f + ph * 2.3f) * 3f - 2f, 0, 1);
                jaws[i].xRot += Mth.sin(age * 0.09f + ph) * 0.05f + roar * 0.55f;
                heads[i].xRot -= roar * 0.3f;
                if (blow > 0) {
                    if (i < awake) {
                        // the waking heads lunge and bite
                        necks[i].xRot -= blow * 0.45f;
                        necks2[i].xRot -= blow * 0.25f;
                        heads[i].xRot += blow * 0.3f;
                        jaws[i].xRot += blow * 0.7f * (1 - roar);
                    } else {
                        // the rest rear back, roaring
                        necks[i].xRot += blow * 0.15f;
                        heads[i].xRot -= blow * 0.35f;
                        jaws[i].xRot += blow * 0.6f * (1 - roar);
                    }
                }
                for (int j = 0; j < locks[i].length; j++) {
                    locks[i][j].zRot += Mth.sin(age * 0.08f + ph + j) * 0.05f;
                    locks[i][j].xRot -= amount * 0.2f;
                }
            }
            // the mane stirs, swept back as he runs
            for (int i = 0; i < bodyLocks.length; i++) {
                bodyLocks[i].zRot += Mth.sin(age * 0.07f + i * 0.9f) * (0.04f + amount * 0.08f);
                bodyLocks[i].xRot -= amount * 0.2f;
            }
            // a long tail, swaying, each joint a beat behind the one before; lifted as he runs
            tail.yRot = Mth.sin(age * 0.08f) * 0.3f;
            tail.xRot += Mth.sin(age * 0.05f) * 0.08f + amount * 0.3f;
            tailMid.yRot = Mth.sin(age * 0.08f - 0.9f) * 0.3f;
            tailTip.yRot = Mth.sin(age * 0.08f - 1.8f) * 0.35f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }

        /** The glowing eyes: the waking heads' full-bright, then the dormant heads' dimmer. */
        void renderEyes(PoseStack pose, VertexConsumer vc) {
            for (int i = 0; i < 9; i++) necks[i].visible = i < awake;
            root.render(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
            for (int i = 0; i < 9; i++) necks[i].visible = i >= awake;
            root.render(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, DIM);
            for (int i = 0; i < 9; i++) necks[i].visible = true;
        }
    }
}
