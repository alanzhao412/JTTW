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
                .texOffs(222, 141).mirror(false).addBox(-10.8f, 0, -32.4f, 21.6f, 13.2f, 9.6f)
                .texOffs(107, 0).mirror(false).addBox(-10.2f, -8.4f, -7.2f, 20.4f, 15.6f, 20.4f)
                .texOffs(192, 0).mirror(false).addBox(-12, -9.6f, 10.8f, 24, 16.8f, 15.6f)
                .texOffs(216, 49).mirror(false).addBox(-15.6f, -15.6f, -31.2f, 31.2f, 13.2f, 14.4f)
                .texOffs(116, 167).mirror(false).addBox(-14.4f, -16.8f, -19.2f, 28.8f, 12, 10.8f)
                .texOffs(47, 237).mirror(false).addBox(-13.2f, -14.4f, -10.8f, 26.4f, 9.6f, 7.2f)
                .texOffs(220, 215).mirror(false).addBox(-13.92f, 4.8f, -31.92f, 27.84f, 3.6f, 14.4f)
                .texOffs(125, 257).mirror(false).addBox(-3.12f, 8.4f, -33.84f, 6.24f, 7.2f, 6.24f)
                .texOffs(45, 312).mirror(false).addBox(-0.96f, 15.36f, -31.68f, 1.92f, 2.4f, 1.92f)
                .texOffs(59, 302).mirror(false).addBox(-9.84f, 8.4f, -32.64f, 3.12f, 3.6f, 3.12f)
                .texOffs(59, 302).mirror(false).addBox(6.72f, 8.4f, -32.64f, 3.12f, 3.6f, 3.12f)
                .texOffs(59, 302).mirror(false).addBox(-13.68f, 8.4f, -26.16f, 3.12f, 3.6f, 3.12f)
                .texOffs(59, 302).mirror(false).addBox(10.56f, 8.4f, -26.16f, 3.12f, 3.6f, 3.12f)
                .texOffs(43, 215).mirror(false).addBox(-11.52f, -11.04f, -6, 23.04f, 2.64f, 16.8f)
                .texOffs(55, 49).mirror(false).addBox(-11.76f, -9.6f, -6.24f, 1.68f, 13.2f, 17.28f)
                .texOffs(55, 49).mirror(false).addBox(10.08f, -9.6f, -6.24f, 1.68f, 13.2f, 17.28f)
                .texOffs(135, 275).mirror(false).addBox(-3.6f, -12.24f, -2.4f, 7.2f, 1.44f, 9.6f), PartPose.offsetAndRotation(0, -4.8f, 0, 0, 0, 0));
        PartDefinition rump = body.addOrReplaceChild("rump", CubeListBuilder.create()
                .texOffs(232, 113).mirror(false).addBox(-11.4f, 0, 0, 22.8f, 8.4f, 15.6f), PartPose.offsetAndRotation(0, -9.6f, 12, -0.35f, 0, 0));
        PartDefinition spineA = body.addOrReplaceChild("spine_a", CubeListBuilder.create()
                .texOffs(172, 275).mirror(false).addBox(-1.056f, -5.76f, -2.64f, 2.112f, 5.76f, 5.28f)
                .texOffs(234, 290).mirror(false).addBox(-0.6758f, -10.08f, -2.064f, 1.3517f, 4.32f, 3.696f)
                .texOffs(219, 302).mirror(false).addBox(-0.3168f, -13.44f, -1.392f, 0.6336f, 3.36f, 2.376f), PartPose.offsetAndRotation(0, -12, 7.2f, -1.4f, 0, 0));
        PartDefinition spineB = body.addOrReplaceChild("spine_b", CubeListBuilder.create()
                .texOffs(81, 290).mirror(false).addBox(-0.858f, -4.68f, -2.145f, 1.716f, 4.68f, 4.29f)
                .texOffs(76, 302).mirror(false).addBox(-0.5491f, -8.19f, -1.677f, 1.0982f, 3.51f, 3.003f)
                .texOffs(54, 312).mirror(false).addBox(-0.2574f, -10.92f, -1.131f, 0.5148f, 2.73f, 1.9305f), PartPose.offsetAndRotation(0, -11.4f, 15.6f, -1.5f, 0, 0));
        PartDefinition rightManeA = body.addOrReplaceChild("right_mane_a", CubeListBuilder.create()
                .texOffs(118, 237).mirror(false).addBox(-1.65f, -9, -4.125f, 3.3f, 9, 8.25f)
                .texOffs(21, 275).mirror(false).addBox(-1.056f, -15.75f, -3.225f, 2.112f, 6.75f, 5.775f)
                .texOffs(96, 290).mirror(false).addBox(-0.495f, -21, -2.175f, 0.99f, 5.25f, 3.7125f), PartPose.offsetAndRotation(-14.4f, -14.4f, -21.6f, -1.3f, 0, -0.7f));
        PartDefinition leftManeA = body.addOrReplaceChild("left_mane_a", CubeListBuilder.create()
                .texOffs(118, 237).mirror(true).addBox(-1.65f, -9, -4.125f, 3.3f, 9, 8.25f)
                .texOffs(21, 275).mirror(true).addBox(-1.056f, -15.75f, -3.225f, 2.112f, 6.75f, 5.775f)
                .texOffs(96, 290).mirror(true).addBox(-0.495f, -21, -2.175f, 0.99f, 5.25f, 3.7125f), PartPose.offsetAndRotation(14.4f, -14.4f, -21.6f, -1.3f, 0, 0.7f));
        PartDefinition rightManeB = body.addOrReplaceChild("right_mane_b", CubeListBuilder.create()
                .texOffs(206, 257).mirror(false).addBox(-4.29f, -9.36f, -1.716f, 8.58f, 9.36f, 3.432f)
                .texOffs(220, 275).mirror(false).addBox(-2.7456f, -16.38f, -0.78f, 5.4912f, 7.02f, 2.4024f)
                .texOffs(89, 302).mirror(false).addBox(-1.287f, -21.84f, 0.312f, 2.574f, 5.46f, 1.5444f), PartPose.offsetAndRotation(-15.6f, -4.8f, -22.8f, -1.3f, 0, -1.25f));
        PartDefinition leftManeB = body.addOrReplaceChild("left_mane_b", CubeListBuilder.create()
                .texOffs(206, 257).mirror(true).addBox(-4.29f, -9.36f, -1.716f, 8.58f, 9.36f, 3.432f)
                .texOffs(220, 275).mirror(true).addBox(-2.7456f, -16.38f, -0.78f, 5.4912f, 7.02f, 2.4024f)
                .texOffs(89, 302).mirror(true).addBox(-1.287f, -21.84f, 0.312f, 2.574f, 5.46f, 1.5444f), PartPose.offsetAndRotation(15.6f, -4.8f, -22.8f, -1.3f, 0, 1.25f));
        PartDefinition rightManeC = body.addOrReplaceChild("right_mane_c", CubeListBuilder.create()
                .texOffs(40, 275).mirror(false).addBox(-3.795f, -8.28f, -1.518f, 7.59f, 8.28f, 3.036f)
                .texOffs(107, 290).mirror(false).addBox(-2.4288f, -14.49f, -0.69f, 4.8576f, 6.21f, 2.1252f)
                .texOffs(228, 302).mirror(false).addBox(-1.1385f, -19.32f, 0.276f, 2.277f, 4.83f, 1.3662f), PartPose.offsetAndRotation(-15, 3.6f, -24, -1.2f, 0, -1.9f));
        PartDefinition leftManeC = body.addOrReplaceChild("left_mane_c", CubeListBuilder.create()
                .texOffs(40, 275).mirror(true).addBox(-3.795f, -8.28f, -1.518f, 7.59f, 8.28f, 3.036f)
                .texOffs(107, 290).mirror(true).addBox(-2.4288f, -14.49f, -0.69f, 4.8576f, 6.21f, 2.1252f)
                .texOffs(228, 302).mirror(true).addBox(-1.1385f, -19.32f, 0.276f, 2.277f, 4.83f, 1.3662f), PartPose.offsetAndRotation(15, 3.6f, -24, -1.2f, 0, 1.9f));
        PartDefinition rightManeD = body.addOrReplaceChild("right_mane_d", CubeListBuilder.create()
                .texOffs(239, 275).mirror(false).addBox(-3.465f, -7.56f, -1.386f, 6.93f, 7.56f, 2.772f)
                .texOffs(100, 302).mirror(false).addBox(-2.2176f, -13.23f, -0.63f, 4.4352f, 5.67f, 1.9404f)
                .texOffs(228, 302).mirror(false).addBox(-1.0395f, -17.64f, 0.252f, 2.079f, 4.41f, 1.2474f), PartPose.offsetAndRotation(-13.8f, -12, -12, -1.45f, 0, -0.95f));
        PartDefinition leftManeD = body.addOrReplaceChild("left_mane_d", CubeListBuilder.create()
                .texOffs(239, 275).mirror(true).addBox(-3.465f, -7.56f, -1.386f, 6.93f, 7.56f, 2.772f)
                .texOffs(100, 302).mirror(true).addBox(-2.2176f, -13.23f, -0.63f, 4.4352f, 5.67f, 1.9404f)
                .texOffs(228, 302).mirror(true).addBox(-1.0395f, -17.64f, 0.252f, 2.079f, 4.41f, 1.2474f), PartPose.offsetAndRotation(13.8f, -12, -12, -1.45f, 0, 0.95f));
        PartDefinition rightManeE = body.addOrReplaceChild("right_mane_e", CubeListBuilder.create()
                .texOffs(124, 290).mirror(false).addBox(-3.135f, -6.84f, -1.254f, 6.27f, 6.84f, 2.508f)
                .texOffs(100, 302).mirror(false).addBox(-2.0064f, -11.97f, -0.57f, 4.0128f, 5.13f, 1.7556f)
                .texOffs(283, 302).mirror(false).addBox(-0.9405f, -15.96f, 0.228f, 1.881f, 3.99f, 1.1286f), PartPose.offsetAndRotation(-13.2f, -3.6f, -12, -1.5f, 0, -1.4f));
        PartDefinition leftManeE = body.addOrReplaceChild("left_mane_e", CubeListBuilder.create()
                .texOffs(124, 290).mirror(true).addBox(-3.135f, -6.84f, -1.254f, 6.27f, 6.84f, 2.508f)
                .texOffs(100, 302).mirror(true).addBox(-2.0064f, -11.97f, -0.57f, 4.0128f, 5.13f, 1.7556f)
                .texOffs(283, 302).mirror(true).addBox(-0.9405f, -15.96f, 0.228f, 1.881f, 3.99f, 1.1286f), PartPose.offsetAndRotation(13.2f, -3.6f, -12, -1.5f, 0, 1.4f));
        PartDefinition apron = body.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(260, 275).mirror(false).addBox(-3.63f, -7.92f, -1.452f, 7.26f, 7.92f, 2.904f)
                .texOffs(247, 290).mirror(false).addBox(-2.3232f, -13.86f, -0.66f, 4.6464f, 5.94f, 2.0328f)
                .texOffs(228, 302).mirror(false).addBox(-1.089f, -18.48f, 0.264f, 2.178f, 4.62f, 1.3068f), PartPose.offsetAndRotation(0, -3.6f, -33, 2.95f, 0, 0));
        PartDefinition rightApron = body.addOrReplaceChild("right_apron", CubeListBuilder.create()
                .texOffs(239, 275).mirror(false).addBox(-3.465f, -7.56f, -1.386f, 6.93f, 7.56f, 2.772f)
                .texOffs(100, 302).mirror(false).addBox(-2.2176f, -13.23f, -0.63f, 4.4352f, 5.67f, 1.9404f)
                .texOffs(228, 302).mirror(false).addBox(-1.0395f, -17.64f, 0.252f, 2.079f, 4.41f, 1.2474f), PartPose.offsetAndRotation(-6, -3.6f, -32.64f, 2.95f, 0, -0.18f));
        PartDefinition leftApron = body.addOrReplaceChild("left_apron", CubeListBuilder.create()
                .texOffs(239, 275).mirror(true).addBox(-3.465f, -7.56f, -1.386f, 6.93f, 7.56f, 2.772f)
                .texOffs(100, 302).mirror(true).addBox(-2.2176f, -13.23f, -0.63f, 4.4352f, 5.67f, 1.9404f)
                .texOffs(228, 302).mirror(true).addBox(-1.0395f, -17.64f, 0.252f, 2.079f, 4.41f, 1.2474f), PartPose.offsetAndRotation(6, -3.6f, -32.64f, 2.95f, 0, 0.18f));
        PartDefinition rightApronB = body.addOrReplaceChild("right_apron_b", CubeListBuilder.create()
                .texOffs(124, 290).mirror(false).addBox(-3.135f, -6.84f, -1.254f, 6.27f, 6.84f, 2.508f)
                .texOffs(100, 302).mirror(false).addBox(-2.0064f, -11.97f, -0.57f, 4.0128f, 5.13f, 1.7556f)
                .texOffs(283, 302).mirror(false).addBox(-0.9405f, -15.96f, 0.228f, 1.881f, 3.99f, 1.1286f), PartPose.offsetAndRotation(-11.4f, -4.2f, -31.2f, 2.95f, 0, -0.4f));
        PartDefinition leftApronB = body.addOrReplaceChild("left_apron_b", CubeListBuilder.create()
                .texOffs(124, 290).mirror(true).addBox(-3.135f, -6.84f, -1.254f, 6.27f, 6.84f, 2.508f)
                .texOffs(100, 302).mirror(true).addBox(-2.0064f, -11.97f, -0.57f, 4.0128f, 5.13f, 1.7556f)
                .texOffs(283, 302).mirror(true).addBox(-0.9405f, -15.96f, 0.228f, 1.881f, 3.99f, 1.1286f), PartPose.offsetAndRotation(11.4f, -4.2f, -31.2f, 2.95f, 0, 0.4f));
        PartDefinition rightForeleg = body.addOrReplaceChild("right_foreleg", CubeListBuilder.create()
                .texOffs(161, 49).mirror(false).addBox(-6, -4.8f, -7.2f, 12, 15.6f, 14.4f)
                .texOffs(228, 83).mirror(false).addBox(-4.8f, 9.6f, -5.16f, 9.6f, 15.6f, 10.08f)
                .texOffs(264, 290).mirror(false).addBox(-2.88f, 11.52f, 4.92f, 5.76f, 5.76f, 3)
                .texOffs(126, 215).mirror(false).addBox(-6, 24.24f, -10.8f, 12, 5.76f, 13.2f)
                .texOffs(115, 302).mirror(false).addBox(-5.52f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(115, 302).mirror(false).addBox(-2.46f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(115, 302).mirror(false).addBox(0.6f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(115, 302).mirror(false).addBox(3.66f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f), PartPose.offsetAndRotation(-10.2f, -1.2f, -18, 0, 0, 0));
        PartDefinition leftForeleg = body.addOrReplaceChild("left_foreleg", CubeListBuilder.create()
                .texOffs(161, 49).mirror(true).addBox(-6, -4.8f, -7.2f, 12, 15.6f, 14.4f)
                .texOffs(228, 83).mirror(true).addBox(-4.8f, 9.6f, -5.16f, 9.6f, 15.6f, 10.08f)
                .texOffs(264, 290).mirror(true).addBox(-2.88f, 11.52f, 4.92f, 5.76f, 5.76f, 3)
                .texOffs(126, 215).mirror(true).addBox(-6, 24.24f, -10.8f, 12, 5.76f, 13.2f)
                .texOffs(115, 302).mirror(true).addBox(3.48f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(115, 302).mirror(true).addBox(0.42f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(115, 302).mirror(true).addBox(-2.64f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f)
                .texOffs(115, 302).mirror(true).addBox(-5.7f, 25.92f, -13.44f, 2.04f, 3.6f, 3.12f), PartPose.offsetAndRotation(10.2f, -1.2f, -18, 0, 0, 0));
        PartDefinition rightHindleg = body.addOrReplaceChild("right_hindleg", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.8f, -2.4f, 18.6f, 0, 0, 0));
        PartDefinition leftHindleg = body.addOrReplaceChild("left_hindleg", CubeListBuilder.create(), PartPose.offsetAndRotation(7.8f, -2.4f, 18.6f, 0, 0, 0));
        PartDefinition rightThigh = rightHindleg.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                .texOffs(0, 49).mirror(false).addBox(-6, -3.6f, -7.8f, 12, 18, 14.4f), PartPose.offsetAndRotation(0, 0, 0, -0.5f, 0, 0));
        PartDefinition leftThigh = leftHindleg.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                .texOffs(0, 49).mirror(true).addBox(-6, -3.6f, -7.8f, 12, 18, 14.4f), PartPose.offsetAndRotation(0, 0, 0, -0.5f, 0, 0));
        PartDefinition rightShin = rightThigh.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(0, 192).mirror(false).addBox(-4.08f, -1.8f, -3.6f, 8.16f, 13.2f, 7.2f), PartPose.offsetAndRotation(0, 13.2f, -1.2f, 1.4f, 0, 0));
        PartDefinition leftShin = leftThigh.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(0, 192).mirror(true).addBox(-4.08f, -1.8f, -3.6f, 8.16f, 13.2f, 7.2f), PartPose.offsetAndRotation(0, 13.2f, -1.2f, 1.4f, 0, 0));
        PartDefinition rightHock = rightShin.addOrReplaceChild("right_hock", CubeListBuilder.create()
                .texOffs(145, 237).mirror(false).addBox(-3.6f, -1.2f, -3.24f, 7.2f, 10.8f, 6.48f), PartPose.offsetAndRotation(0, 11.4f, 0, -1.2f, 0, 0));
        PartDefinition leftHock = leftShin.addOrReplaceChild("left_hock", CubeListBuilder.create()
                .texOffs(145, 237).mirror(true).addBox(-3.6f, -1.2f, -3.24f, 7.2f, 10.8f, 6.48f), PartPose.offsetAndRotation(0, 11.4f, 0, -1.2f, 0, 0));
        PartDefinition rightHindpaw = rightHock.addOrReplaceChild("right_hindpaw", CubeListBuilder.create()
                .texOffs(0, 257).mirror(false).addBox(-5.4f, -0.84f, -9, 10.8f, 5.28f, 10.8f)
                .texOffs(239, 302).mirror(false).addBox(-4.68f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f)
                .texOffs(239, 302).mirror(false).addBox(-1.44f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f)
                .texOffs(239, 302).mirror(false).addBox(1.8f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f), PartPose.offsetAndRotation(0, 9.12f, 0, 0.3f, 0, 0));
        PartDefinition leftHindpaw = leftHock.addOrReplaceChild("left_hindpaw", CubeListBuilder.create()
                .texOffs(0, 257).mirror(true).addBox(-5.4f, -0.84f, -9, 10.8f, 5.28f, 10.8f)
                .texOffs(239, 302).mirror(true).addBox(2.76f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f)
                .texOffs(239, 302).mirror(true).addBox(-0.48f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f)
                .texOffs(239, 302).mirror(true).addBox(-3.72f, 1.2f, -11.4f, 1.92f, 3.12f, 2.88f), PartPose.offsetAndRotation(0, 9.12f, 0, 0.3f, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(176, 237).mirror(false).addBox(-1.68f, -1.68f, 0, 3.36f, 3.36f, 13.2f), PartPose.offsetAndRotation(0, -6, 26.4f, -0.6f, 0, 0));
        PartDefinition tailMid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create()
                .texOffs(154, 257).mirror(false).addBox(-1.44f, -1.44f, 0, 2.88f, 2.88f, 12), PartPose.offsetAndRotation(0, 0, 12.6f, 0.9f, 0, 0));
        PartDefinition tailTip = tailMid.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(283, 290).mirror(false).addBox(-1.32f, -1.32f, 0, 2.64f, 2.64f, 6)
                .texOffs(233, 257).mirror(false).addBox(-3.6f, -3.6f, 4.2f, 7.2f, 7.2f, 6)
                .texOffs(130, 302).mirror(false).addBox(-2.64f, -2.64f, 10.08f, 5.28f, 5.28f, 1.8f), PartPose.offsetAndRotation(0, 0, 11.4f, 0.7f, 0, 0));
        PartDefinition tailFlame = tailTip.addOrReplaceChild("tail_flame", CubeListBuilder.create()
                .texOffs(185, 257).mirror(false).addBox(-1.32f, -7.2f, -3.3f, 2.64f, 7.2f, 6.6f)
                .texOffs(283, 275).mirror(false).addBox(-0.8448f, -12.6f, -2.58f, 1.6896f, 5.4f, 4.62f)
                .texOffs(147, 302).mirror(false).addBox(-0.396f, -16.8f, -1.74f, 0.792f, 4.2f, 2.97f), PartPose.offsetAndRotation(0, -1.2f, 8.4f, -1.1f, 0, 0));
        PartDefinition midNeck = body.addOrReplaceChild("mid_neck", CubeListBuilder.create()
                .texOffs(96, 49).mirror(false).addBox(-3.4f, -3.4f, -1, 6.8f, 6.8f, 24.9804f)
                .texOffs(59, 83).mirror(false).addBox(-4.2f, -5.6f, -1, 8.4f, 3, 24.9804f), PartPose.offsetAndRotation(-0, -12, -20.4f, 1.2931f, 3.1416f, 0));
        PartDefinition midNeck2 = midNeck.addOrReplaceChild("mid_neck2", CubeListBuilder.create()
                .texOffs(0, 141).mirror(false).addBox(-3.128f, -3.128f, -1, 6.256f, 6.256f, 17.556f)
                .texOffs(239, 192).mirror(false).addBox(-3.8f, -5.4f, -1, 7.6f, 3, 17.556f), PartPose.offsetAndRotation(0, 0, 22.9804f, -0.4735f, -0, 0));
        PartDefinition midCrestA = midNeck.addOrReplaceChild("mid_crest_a", CubeListBuilder.create()
                .texOffs(298, 275).mirror(false).addBox(-0.935f, -5.1f, -2.3375f, 1.87f, 5.1f, 4.675f)
                .texOffs(76, 302).mirror(false).addBox(-0.5984f, -8.925f, -1.8275f, 1.1968f, 3.825f, 3.2725f)
                .texOffs(292, 302).mirror(false).addBox(-0.2805f, -11.9f, -1.2325f, 0.561f, 2.975f, 2.1037f), PartPose.offsetAndRotation(0, -4.4f, 9.1922f, -1.15f, 0, 0));
        PartDefinition midHead = midNeck2.addOrReplaceChild("mid_head", CubeListBuilder.create()
                .texOffs(35, 192).mirror(false).addBox(-6.5f, -6, -10.8f, 13, 11, 11)
                .texOffs(65, 275).mirror(false).addBox(-6, -9, -9.6f, 12, 3.6f, 8.4f)
                .texOffs(262, 257).mirror(false).addBox(-8.4f, -0.6f, -9.6f, 16.8f, 6, 7.2f)
                .texOffs(145, 290).mirror(false).addBox(-3, -4.32f, -15.6f, 6, 3.6f, 5.4f)
                .texOffs(0, 290).mirror(false).addBox(-5.52f, -0.96f, -16.32f, 5.4f, 4.8f, 6)
                .texOffs(0, 290).mirror(false).addBox(0.12f, -0.96f, -16.32f, 5.4f, 4.8f, 6)
                .texOffs(25, 290).mirror(false).addBox(-0.96f, -0.96f, -15.84f, 1.92f, 4.32f, 5.4f)
                .texOffs(61, 312).mirror(false).addBox(-2.76f, -3.6f, -17.04f, 5.52f, 2.76f, 1.44f)
                .texOffs(250, 302).mirror(false).addBox(-5.4f, 3.12f, -15.84f, 1.68f, 4.32f, 1.68f)
                .texOffs(250, 302).mirror(false).addBox(3.72f, 3.12f, -15.84f, 1.68f, 4.32f, 1.68f)
                .texOffs(130, 312).mirror(false).addBox(-4.92f, 6.96f, -15.6f, 0.96f, 2.4f, 0.96f)
                .texOffs(130, 312).mirror(false).addBox(3.96f, 6.96f, -15.6f, 0.96f, 2.4f, 0.96f)
                .texOffs(135, 312).mirror(false).addBox(-3.6f, 3.48f, -16.08f, 7.2f, 1.44f, 0.96f)
                .texOffs(191, 275).mirror(false).addBox(-4.08f, 0.96f, -11.4f, 8.16f, 6.6f, 4.8f)
                .texOffs(84, 192).mirror(false).addBox(-10.2f, -9.6f, -4.8f, 20.4f, 15.6f, 6)
                .texOffs(128, 83).mirror(false).addBox(-7.2f, -12.6f, -4.8f, 14.4f, 21.6f, 6)
                .texOffs(264, 290).mirror(false).addBox(-6.12f, -13.56f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(156, 302).mirror(false).addBox(-10.56f, -10.56f, -6.24f, 4.32f, 4.32f, 3)
                .texOffs(264, 290).mirror(false).addBox(-13.08f, -6.12f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(156, 302).mirror(false).addBox(-12.72f, -0.24f, -6.24f, 4.32f, 4.32f, 3)
                .texOffs(264, 290).mirror(false).addBox(-10.44f, 3.96f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(156, 302).mirror(false).addBox(-5.04f, 6.72f, -6.24f, 4.32f, 4.32f, 3)
                .texOffs(264, 290).mirror(false).addBox(1.08f, -13.56f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(156, 302).mirror(false).addBox(6.24f, -10.56f, -6.24f, 4.32f, 4.32f, 3)
                .texOffs(264, 290).mirror(false).addBox(8.04f, -6.12f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(156, 302).mirror(false).addBox(8.4f, -0.24f, -6.24f, 4.32f, 4.32f, 3)
                .texOffs(264, 290).mirror(false).addBox(5.4f, 3.96f, -6.24f, 5.04f, 5.04f, 3)
                .texOffs(156, 302).mirror(false).addBox(0.72f, 6.72f, -6.24f, 4.32f, 4.32f, 3), PartPose.offsetAndRotation(0, 0, 16.556f, -2.202f, -0, 3.1416f));
        PartDefinition midBrowA = midHead.addOrReplaceChild("mid_brow_a", CubeListBuilder.create()
                .texOffs(0, 312).mirror(false).addBox(-7.44f, -1.44f, -0.96f, 7.44f, 2.88f, 2.64f), PartPose.offsetAndRotation(-0.36f, -5.28f, -11.28f, 0, 0, 0.45f));
        PartDefinition midBrowB = midHead.addOrReplaceChild("mid_brow_b", CubeListBuilder.create()
                .texOffs(0, 312).mirror(false).addBox(0, -1.44f, -0.96f, 7.44f, 2.88f, 2.64f), PartPose.offsetAndRotation(0.36f, -5.28f, -11.28f, 0, 0, -0.45f));
        PartDefinition midJaw = midHead.addOrReplaceChild("mid_jaw", CubeListBuilder.create()
                .texOffs(170, 290).mirror(false).addBox(-3.72f, 0, -6.96f, 7.44f, 2.64f, 6.96f)
                .texOffs(23, 312).mirror(false).addBox(-3.6f, -3.12f, -6.6f, 1.44f, 3.12f, 1.44f)
                .texOffs(23, 312).mirror(false).addBox(2.16f, -3.12f, -6.6f, 1.44f, 3.12f, 1.44f)
                .texOffs(154, 312).mirror(false).addBox(-1.92f, -1.44f, -6.72f, 3.84f, 1.44f, 0.96f)
                .texOffs(156, 302).mirror(false).addBox(-6.36f, 2.28f, -6.96f, 4.08f, 4.08f, 2.64f)
                .texOffs(156, 302).mirror(false).addBox(-2.04f, 2.76f, -7.44f, 4.08f, 4.08f, 2.64f)
                .texOffs(156, 302).mirror(false).addBox(2.28f, 2.28f, -6.96f, 4.08f, 4.08f, 2.64f), PartPose.offsetAndRotation(0, 3.36f, -9.6f, 0.35f, 0, 0));
        PartDefinition midLockTop = midHead.addOrReplaceChild("mid_lock_top", CubeListBuilder.create()
                .texOffs(0, 275).mirror(false).addBox(-1.254f, -6.84f, -3.135f, 2.508f, 6.84f, 6.27f)
                .texOffs(283, 275).mirror(false).addBox(-0.8026f, -11.97f, -2.451f, 1.6051f, 5.13f, 4.389f)
                .texOffs(219, 302).mirror(false).addBox(-0.3762f, -15.96f, -1.653f, 0.7524f, 3.99f, 2.8215f), PartPose.offsetAndRotation(0, -11.4f, -1.2f, -1.25f, 0, 0));
        PartDefinition midLockA0 = midHead.addOrReplaceChild("mid_lock_a0", CubeListBuilder.create()
                .texOffs(0, 275).mirror(false).addBox(-1.254f, -6.84f, -3.135f, 2.508f, 6.84f, 6.27f)
                .texOffs(283, 275).mirror(false).addBox(-0.8026f, -11.97f, -2.451f, 1.6051f, 5.13f, 4.389f)
                .texOffs(219, 302).mirror(false).addBox(-0.3762f, -15.96f, -1.653f, 0.7524f, 3.99f, 2.8215f), PartPose.offsetAndRotation(-6.6f, -10.8f, -0.6f, -1.05f, 0, -0.5f));
        PartDefinition midLockA1 = midHead.addOrReplaceChild("mid_lock_a1", CubeListBuilder.create()
                .texOffs(239, 275).mirror(false).addBox(-3.3f, -7.2f, -1.32f, 6.6f, 7.2f, 2.64f)
                .texOffs(100, 302).mirror(false).addBox(-2.112f, -12.6f, -0.6f, 4.224f, 5.4f, 1.848f)
                .texOffs(259, 302).mirror(false).addBox(-0.99f, -16.8f, 0.24f, 1.98f, 4.2f, 1.188f), PartPose.offsetAndRotation(-10.2f, -4.2f, -0.6f, -0.97f, 0, -1.05f));
        PartDefinition midLockA2 = midHead.addOrReplaceChild("mid_lock_a2", CubeListBuilder.create()
                .texOffs(124, 290).mirror(false).addBox(-3.135f, -6.84f, -1.254f, 6.27f, 6.84f, 2.508f)
                .texOffs(100, 302).mirror(false).addBox(-2.0064f, -11.97f, -0.57f, 4.0128f, 5.13f, 1.7556f)
                .texOffs(283, 302).mirror(false).addBox(-0.9405f, -15.96f, 0.228f, 1.881f, 3.99f, 1.1286f), PartPose.offsetAndRotation(-10.2f, 3.6f, -0.6f, -0.89f, 0, -1.6f));
        PartDefinition midLockB0 = midHead.addOrReplaceChild("mid_lock_b0", CubeListBuilder.create()
                .texOffs(0, 275).mirror(false).addBox(-1.254f, -6.84f, -3.135f, 2.508f, 6.84f, 6.27f)
                .texOffs(283, 275).mirror(false).addBox(-0.8026f, -11.97f, -2.451f, 1.6051f, 5.13f, 4.389f)
                .texOffs(219, 302).mirror(false).addBox(-0.3762f, -15.96f, -1.653f, 0.7524f, 3.99f, 2.8215f), PartPose.offsetAndRotation(6.6f, -10.8f, -0.6f, -1.05f, 0, 0.5f));
        PartDefinition midLockB1 = midHead.addOrReplaceChild("mid_lock_b1", CubeListBuilder.create()
                .texOffs(239, 275).mirror(false).addBox(-3.3f, -7.2f, -1.32f, 6.6f, 7.2f, 2.64f)
                .texOffs(100, 302).mirror(false).addBox(-2.112f, -12.6f, -0.6f, 4.224f, 5.4f, 1.848f)
                .texOffs(259, 302).mirror(false).addBox(-0.99f, -16.8f, 0.24f, 1.98f, 4.2f, 1.188f), PartPose.offsetAndRotation(10.2f, -4.2f, -0.6f, -0.97f, 0, 1.05f));
        PartDefinition midLockB2 = midHead.addOrReplaceChild("mid_lock_b2", CubeListBuilder.create()
                .texOffs(124, 290).mirror(false).addBox(-3.135f, -6.84f, -1.254f, 6.27f, 6.84f, 2.508f)
                .texOffs(100, 302).mirror(false).addBox(-2.0064f, -11.97f, -0.57f, 4.0128f, 5.13f, 1.7556f)
                .texOffs(283, 302).mirror(false).addBox(-0.9405f, -15.96f, 0.228f, 1.881f, 3.99f, 1.1286f), PartPose.offsetAndRotation(10.2f, 3.6f, -0.6f, -0.89f, 0, 1.6f));
        PartDefinition rightH1Neck = body.addOrReplaceChild("right_h1_neck", CubeListBuilder.create()
                .texOffs(171, 83).mirror(false).addBox(-2.8f, -2.8f, -1, 5.6f, 5.6f, 21.089f)
                .texOffs(51, 141).mirror(false).addBox(-3.6f, -5, -1, 7.2f, 3, 21.089f), PartPose.offsetAndRotation(-3.84f, -10.2f, -19.8f, 0.761f, -2.9616f, 0));
        PartDefinition rightH1Neck2 = rightH1Neck.addOrReplaceChild("right_h1_neck2", CubeListBuilder.create()
                .texOffs(0, 113).mirror(false).addBox(-2.576f, -2.576f, -1, 5.152f, 5.152f, 20.2649f)
                .texOffs(0, 167).mirror(false).addBox(-3.2f, -4.8f, -1, 6.4f, 3, 20.2649f), PartPose.offsetAndRotation(0, 0, 19.089f, -0.533f, -0.0346f, 0.0329f));
        PartDefinition rightH1Head = rightH1Neck2.addOrReplaceChild("right_h1_head", CubeListBuilder.create()
                .texOffs(88, 257).mirror(false).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(201, 290).mirror(false).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(42, 290).mirror(false).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(173, 302).mirror(false).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(0, 302).mirror(false).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(0, 302).mirror(false).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(21, 302).mirror(false).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(78, 312).mirror(false).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(23, 312).mirror(false).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(23, 312).mirror(false).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(165, 312).mirror(false).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(165, 312).mirror(false).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(170, 312).mirror(false).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(36, 302).mirror(false).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(108, 275).mirror(false).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(268, 302).mirror(false).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(false).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(false).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 19.2649f, -2.9915f, -0.1159f, 3.1145f));
        PartDefinition rightH1BrowA = rightH1Head.addOrReplaceChild("right_h1_brow_a", CubeListBuilder.create()
                .texOffs(93, 312).mirror(false).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition rightH1BrowB = rightH1Head.addOrReplaceChild("right_h1_brow_b", CubeListBuilder.create()
                .texOffs(93, 312).mirror(false).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition rightH1Jaw = rightH1Head.addOrReplaceChild("right_h1_jaw", CubeListBuilder.create()
                .texOffs(194, 302).mirror(false).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(110, 312).mirror(false).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(110, 312).mirror(false).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(185, 312).mirror(false).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(119, 312).mirror(false).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(119, 312).mirror(false).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition leftH1Neck = body.addOrReplaceChild("left_h1_neck", CubeListBuilder.create()
                .texOffs(0, 83).mirror(true).addBox(-2.8f, -2.8f, -1, 5.6f, 5.6f, 22.0349f)
                .texOffs(55, 113).mirror(true).addBox(-3.6f, -5, -1, 7.2f, 3, 22.0349f), PartPose.offsetAndRotation(3.84f, -10.2f, -19.8f, 0.6214f, 2.989f, 0));
        PartDefinition leftH1Neck2 = leftH1Neck.addOrReplaceChild("left_h1_neck2", CubeListBuilder.create()
                .texOffs(0, 83).mirror(true).addBox(-2.576f, -2.576f, -1, 5.152f, 5.152f, 22.4369f)
                .texOffs(118, 113).mirror(true).addBox(-3.2f, -4.8f, -1, 6.4f, 3, 22.4369f), PartPose.offsetAndRotation(0, 0, 20.0349f, -0.4877f, 0.0291f, -0.0208f));
        PartDefinition leftH1Head = leftH1Neck2.addOrReplaceChild("left_h1_head", CubeListBuilder.create()
                .texOffs(88, 257).mirror(true).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(201, 290).mirror(true).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(42, 290).mirror(true).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(173, 302).mirror(true).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(0, 302).mirror(true).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(0, 302).mirror(true).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(21, 302).mirror(true).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(78, 312).mirror(true).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(23, 312).mirror(true).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(23, 312).mirror(true).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(165, 312).mirror(true).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(165, 312).mirror(true).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(170, 312).mirror(true).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(36, 302).mirror(true).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(108, 275).mirror(true).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(268, 302).mirror(true).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(true).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(true).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 21.4369f, -3.0864f, 0.1333f, -3.1235f));
        PartDefinition leftH1BrowA = leftH1Head.addOrReplaceChild("left_h1_brow_a", CubeListBuilder.create()
                .texOffs(93, 312).mirror(true).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition leftH1BrowB = leftH1Head.addOrReplaceChild("left_h1_brow_b", CubeListBuilder.create()
                .texOffs(93, 312).mirror(true).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition leftH1Jaw = leftH1Head.addOrReplaceChild("left_h1_jaw", CubeListBuilder.create()
                .texOffs(194, 302).mirror(true).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(110, 312).mirror(true).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(110, 312).mirror(true).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(185, 312).mirror(true).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(119, 312).mirror(true).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(119, 312).mirror(true).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition rightH2Neck = body.addOrReplaceChild("right_h2_neck", CubeListBuilder.create()
                .texOffs(179, 113).mirror(false).addBox(-2.8f, -2.8f, -1, 5.6f, 5.6f, 19.6006f)
                .texOffs(197, 167).mirror(false).addBox(-3.6f, -5, -1, 7.2f, 3, 19.6006f), PartPose.offsetAndRotation(-7.68f, -8.4f, -19.2f, 1.0513f, -2.0985f, 0));
        PartDefinition rightH2Neck2 = rightH2Neck.addOrReplaceChild("right_h2_neck2", CubeListBuilder.create()
                .texOffs(139, 192).mirror(false).addBox(-2.576f, -2.576f, -1, 5.152f, 5.152f, 15.2938f)
                .texOffs(0, 237).mirror(false).addBox(-3.2f, -4.8f, -1, 6.4f, 3, 15.2938f), PartPose.offsetAndRotation(0, 0, 17.6006f, -0.5133f, -0.1788f, 0.3216f));
        PartDefinition rightH2Head = rightH2Neck2.addOrReplaceChild("right_h2_head", CubeListBuilder.create()
                .texOffs(88, 257).mirror(false).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(201, 290).mirror(false).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(42, 290).mirror(false).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(173, 302).mirror(false).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(0, 302).mirror(false).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(0, 302).mirror(false).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(21, 302).mirror(false).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(78, 312).mirror(false).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(23, 312).mirror(false).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(23, 312).mirror(false).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(165, 312).mirror(false).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(165, 312).mirror(false).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(170, 312).mirror(false).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(36, 302).mirror(false).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(108, 275).mirror(false).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(268, 302).mirror(false).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(false).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(false).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 14.2938f, -2.6476f, 0.1466f, -3.0474f));
        PartDefinition rightH2BrowA = rightH2Head.addOrReplaceChild("right_h2_brow_a", CubeListBuilder.create()
                .texOffs(93, 312).mirror(false).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition rightH2BrowB = rightH2Head.addOrReplaceChild("right_h2_brow_b", CubeListBuilder.create()
                .texOffs(93, 312).mirror(false).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition rightH2Jaw = rightH2Head.addOrReplaceChild("right_h2_jaw", CubeListBuilder.create()
                .texOffs(194, 302).mirror(false).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(110, 312).mirror(false).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(110, 312).mirror(false).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(185, 312).mirror(false).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(119, 312).mirror(false).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(119, 312).mirror(false).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition leftH2Neck = body.addOrReplaceChild("left_h2_neck", CubeListBuilder.create()
                .texOffs(0, 113).mirror(true).addBox(-2.8f, -2.8f, -1, 5.6f, 5.6f, 20.5544f)
                .texOffs(57, 167).mirror(true).addBox(-3.6f, -5, -1, 7.2f, 3, 20.5544f), PartPose.offsetAndRotation(7.68f, -8.4f, -19.2f, 1.1297f, 1.8786f, 0));
        PartDefinition leftH2Neck2 = leftH2Neck.addOrReplaceChild("left_h2_neck2", CubeListBuilder.create()
                .texOffs(0, 215).mirror(true).addBox(-2.576f, -2.576f, -1, 5.152f, 5.152f, 14.9999f)
                .texOffs(213, 237).mirror(true).addBox(-3.2f, -4.8f, -1, 6.4f, 3, 14.9999f), PartPose.offsetAndRotation(0, 0, 18.5544f, -0.4569f, 0.1937f, -0.4283f));
        PartDefinition leftH2Head = leftH2Neck2.addOrReplaceChild("left_h2_head", CubeListBuilder.create()
                .texOffs(88, 257).mirror(true).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(201, 290).mirror(true).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(42, 290).mirror(true).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(173, 302).mirror(true).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(0, 302).mirror(true).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(0, 302).mirror(true).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(21, 302).mirror(true).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(78, 312).mirror(true).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(23, 312).mirror(true).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(23, 312).mirror(true).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(165, 312).mirror(true).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(165, 312).mirror(true).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(170, 312).mirror(true).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(36, 302).mirror(true).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(108, 275).mirror(true).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(268, 302).mirror(true).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(true).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(true).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 13.9999f, -2.485f, -0.2197f, 2.9465f));
        PartDefinition leftH2BrowA = leftH2Head.addOrReplaceChild("left_h2_brow_a", CubeListBuilder.create()
                .texOffs(93, 312).mirror(true).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition leftH2BrowB = leftH2Head.addOrReplaceChild("left_h2_brow_b", CubeListBuilder.create()
                .texOffs(93, 312).mirror(true).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition leftH2Jaw = leftH2Head.addOrReplaceChild("left_h2_jaw", CubeListBuilder.create()
                .texOffs(194, 302).mirror(true).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(110, 312).mirror(true).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(110, 312).mirror(true).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(185, 312).mirror(true).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(119, 312).mirror(true).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(119, 312).mirror(true).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition rightH3Neck = body.addOrReplaceChild("right_h3_neck", CubeListBuilder.create()
                .texOffs(112, 141).mirror(false).addBox(-2.8f, -2.8f, -1, 5.6f, 5.6f, 18.1862f)
                .texOffs(184, 192).mirror(false).addBox(-3.6f, -5, -1, 7.2f, 3, 18.1862f), PartPose.offsetAndRotation(-11.52f, -6.6f, -18.6f, 0.5422f, -2.7288f, 0));
        PartDefinition rightH3Neck2 = rightH3Neck.addOrReplaceChild("right_h3_neck2", CubeListBuilder.create()
                .texOffs(179, 113).mirror(false).addBox(-2.576f, -2.576f, -1, 5.152f, 5.152f, 19.6558f)
                .texOffs(254, 167).mirror(false).addBox(-3.2f, -4.8f, -1, 6.4f, 3, 19.6558f), PartPose.offsetAndRotation(0, 0, 16.1862f, -0.44f, -0.0927f, 0.0561f));
        PartDefinition rightH3Head = rightH3Neck2.addOrReplaceChild("right_h3_head", CubeListBuilder.create()
                .texOffs(88, 257).mirror(false).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(201, 290).mirror(false).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(42, 290).mirror(false).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(173, 302).mirror(false).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(0, 302).mirror(false).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(0, 302).mirror(false).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(21, 302).mirror(false).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(78, 312).mirror(false).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(23, 312).mirror(false).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(23, 312).mirror(false).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(165, 312).mirror(false).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(165, 312).mirror(false).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(170, 312).mirror(false).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(36, 302).mirror(false).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(108, 275).mirror(false).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(268, 302).mirror(false).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(false).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(false).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 18.6558f, -3.1053f, -0.4469f, 3.0911f));
        PartDefinition rightH3BrowA = rightH3Head.addOrReplaceChild("right_h3_brow_a", CubeListBuilder.create()
                .texOffs(93, 312).mirror(false).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition rightH3BrowB = rightH3Head.addOrReplaceChild("right_h3_brow_b", CubeListBuilder.create()
                .texOffs(93, 312).mirror(false).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition rightH3Jaw = rightH3Head.addOrReplaceChild("right_h3_jaw", CubeListBuilder.create()
                .texOffs(194, 302).mirror(false).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(110, 312).mirror(false).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(110, 312).mirror(false).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(185, 312).mirror(false).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(119, 312).mirror(false).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(119, 312).mirror(false).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition leftH3Neck = body.addOrReplaceChild("left_h3_neck", CubeListBuilder.create()
                .texOffs(179, 113).mirror(true).addBox(-2.8f, -2.8f, -1, 5.6f, 5.6f, 19.5765f)
                .texOffs(197, 167).mirror(true).addBox(-3.6f, -5, -1, 7.2f, 3, 19.5765f), PartPose.offsetAndRotation(11.52f, -6.6f, -18.6f, 0.4005f, 2.7908f, 0));
        PartDefinition leftH3Neck2 = leftH3Neck.addOrReplaceChild("left_h3_neck2", CubeListBuilder.create()
                .texOffs(171, 83).mirror(true).addBox(-2.576f, -2.576f, -1, 5.152f, 5.152f, 21.9567f)
                .texOffs(163, 141).mirror(true).addBox(-3.2f, -4.8f, -1, 6.4f, 3, 21.9567f), PartPose.offsetAndRotation(0, 0, 17.5765f, -0.3801f, 0.0756f, -0.0321f));
        PartDefinition leftH3Head = leftH3Neck2.addOrReplaceChild("left_h3_head", CubeListBuilder.create()
                .texOffs(88, 257).mirror(true).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(201, 290).mirror(true).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(42, 290).mirror(true).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(173, 302).mirror(true).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(0, 302).mirror(true).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(0, 302).mirror(true).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(21, 302).mirror(true).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(78, 312).mirror(true).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(23, 312).mirror(true).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(23, 312).mirror(true).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(165, 312).mirror(true).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(165, 312).mirror(true).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(170, 312).mirror(true).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(36, 302).mirror(true).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(108, 275).mirror(true).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(268, 302).mirror(true).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(true).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(true).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 20.9567f, 3.086f, 0.4852f, -3.1302f));
        PartDefinition leftH3BrowA = leftH3Head.addOrReplaceChild("left_h3_brow_a", CubeListBuilder.create()
                .texOffs(93, 312).mirror(true).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition leftH3BrowB = leftH3Head.addOrReplaceChild("left_h3_brow_b", CubeListBuilder.create()
                .texOffs(93, 312).mirror(true).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition leftH3Jaw = leftH3Head.addOrReplaceChild("left_h3_jaw", CubeListBuilder.create()
                .texOffs(194, 302).mirror(true).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(110, 312).mirror(true).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(110, 312).mirror(true).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(185, 312).mirror(true).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(119, 312).mirror(true).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(119, 312).mirror(true).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition rightH4Neck = body.addOrReplaceChild("right_h4_neck", CubeListBuilder.create()
                .texOffs(0, 215).mirror(false).addBox(-2.8f, -2.8f, -1, 5.6f, 5.6f, 14.4435f)
                .texOffs(258, 237).mirror(false).addBox(-3.6f, -5, -1, 7.2f, 3, 14.4435f), PartPose.offsetAndRotation(-15.36f, -4.8f, -18, 0.4827f, -1.9422f, 0));
        PartDefinition rightH4Neck2 = rightH4Neck.addOrReplaceChild("right_h4_neck2", CubeListBuilder.create()
                .texOffs(0, 215).mirror(false).addBox(-2.576f, -2.576f, -1, 5.152f, 5.152f, 14.668f)
                .texOffs(213, 237).mirror(false).addBox(-3.2f, -4.8f, -1, 6.4f, 3, 14.668f), PartPose.offsetAndRotation(0, 0, 12.4435f, -0.4664f, -0.307f, 0.1669f));
        PartDefinition rightH4Head = rightH4Neck2.addOrReplaceChild("right_h4_head", CubeListBuilder.create()
                .texOffs(88, 257).mirror(false).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(201, 290).mirror(false).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(42, 290).mirror(false).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(173, 302).mirror(false).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(0, 302).mirror(false).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(0, 302).mirror(false).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(21, 302).mirror(false).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(78, 312).mirror(false).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(23, 312).mirror(false).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(23, 312).mirror(false).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(165, 312).mirror(false).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(165, 312).mirror(false).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(170, 312).mirror(false).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(36, 302).mirror(false).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(108, 275).mirror(false).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(268, 302).mirror(false).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(false).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(false).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(false).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 13.668f, 3.1042f, -0.1539f, 3.1351f));
        PartDefinition rightH4BrowA = rightH4Head.addOrReplaceChild("right_h4_brow_a", CubeListBuilder.create()
                .texOffs(93, 312).mirror(false).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition rightH4BrowB = rightH4Head.addOrReplaceChild("right_h4_brow_b", CubeListBuilder.create()
                .texOffs(93, 312).mirror(false).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition rightH4Jaw = rightH4Head.addOrReplaceChild("right_h4_jaw", CubeListBuilder.create()
                .texOffs(194, 302).mirror(false).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(110, 312).mirror(false).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(110, 312).mirror(false).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(185, 312).mirror(false).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(119, 312).mirror(false).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(119, 312).mirror(false).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        PartDefinition leftH4Neck = body.addOrReplaceChild("left_h4_neck", CubeListBuilder.create()
                .texOffs(0, 215).mirror(true).addBox(-2.8f, -2.8f, -1, 5.6f, 5.6f, 14.7443f)
                .texOffs(258, 237).mirror(true).addBox(-3.6f, -5, -1, 7.2f, 3, 14.7443f), PartPose.offsetAndRotation(15.36f, -4.8f, -18, 0.6076f, 1.7631f, 0));
        PartDefinition leftH4Neck2 = leftH4Neck.addOrReplaceChild("left_h4_neck2", CubeListBuilder.create()
                .texOffs(179, 215).mirror(true).addBox(-2.576f, -2.576f, -1, 5.152f, 5.152f, 13.6012f)
                .texOffs(45, 257).mirror(true).addBox(-3.2f, -4.8f, -1, 6.4f, 3, 13.6012f), PartPose.offsetAndRotation(0, 0, 12.7443f, -0.4824f, 0.3302f, -0.2406f));
        PartDefinition leftH4Head = leftH4Neck2.addOrReplaceChild("left_h4_head", CubeListBuilder.create()
                .texOffs(88, 257).mirror(true).addBox(-5, -4.5f, -8.1f, 10, 8, 8)
                .texOffs(201, 290).mirror(true).addBox(-4.5f, -6.75f, -7.2f, 9, 2.7f, 6.3f)
                .texOffs(42, 290).mirror(true).addBox(-6.3f, -0.45f, -7.2f, 12.6f, 4.5f, 5.4f)
                .texOffs(173, 302).mirror(true).addBox(-2.25f, -3.24f, -11.7f, 4.5f, 2.7f, 4.05f)
                .texOffs(0, 302).mirror(true).addBox(0.09f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(0, 302).mirror(true).addBox(-4.14f, -0.72f, -12.24f, 4.05f, 3.6f, 4.5f)
                .texOffs(21, 302).mirror(true).addBox(-0.72f, -0.72f, -11.88f, 1.44f, 3.24f, 4.05f)
                .texOffs(78, 312).mirror(true).addBox(-2.07f, -2.7f, -12.78f, 4.14f, 2.07f, 1.08f)
                .texOffs(23, 312).mirror(true).addBox(2.79f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(23, 312).mirror(true).addBox(-4.05f, 2.34f, -11.88f, 1.26f, 3.24f, 1.26f)
                .texOffs(165, 312).mirror(true).addBox(2.97f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(165, 312).mirror(true).addBox(-3.69f, 5.22f, -11.7f, 0.72f, 1.8f, 0.72f)
                .texOffs(170, 312).mirror(true).addBox(-2.7f, 2.61f, -12.06f, 5.4f, 1.08f, 0.72f)
                .texOffs(36, 302).mirror(true).addBox(-3.06f, 0.72f, -8.55f, 6.12f, 4.95f, 3.6f)
                .texOffs(108, 275).mirror(true).addBox(-4.95f, -6.3f, -2.88f, 9.9f, 9.9f, 2.7f)
                .texOffs(268, 302).mirror(true).addBox(4.77f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(5.4f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(true).addBox(3.78f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(-7.83f, -6.57f, -4.68f, 3.06f, 3.06f, 2.25f)
                .texOffs(268, 302).mirror(true).addBox(-8.64f, -1.98f, -4.68f, 3.24f, 3.24f, 2.25f)
                .texOffs(32, 312).mirror(true).addBox(-6.66f, 2.7f, -4.68f, 2.88f, 2.88f, 2.25f), PartPose.offsetAndRotation(0, 0, 12.6012f, -3.056f, 0.0323f, -3.1362f));
        PartDefinition leftH4BrowA = leftH4Head.addOrReplaceChild("left_h4_brow_a", CubeListBuilder.create()
                .texOffs(93, 312).mirror(true).addBox(0, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(0.27f, -3.96f, -8.46f, 0, 0, -0.45f));
        PartDefinition leftH4BrowB = leftH4Head.addOrReplaceChild("left_h4_brow_b", CubeListBuilder.create()
                .texOffs(93, 312).mirror(true).addBox(-5.58f, -1.08f, -0.72f, 5.58f, 2.16f, 1.98f), PartPose.offsetAndRotation(-0.27f, -3.96f, -8.46f, 0, 0, 0.45f));
        PartDefinition leftH4Jaw = leftH4Head.addOrReplaceChild("left_h4_jaw", CubeListBuilder.create()
                .texOffs(194, 302).mirror(true).addBox(-2.79f, 0, -5.22f, 5.58f, 1.98f, 5.22f)
                .texOffs(110, 312).mirror(true).addBox(1.62f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(110, 312).mirror(true).addBox(-2.7f, -2.34f, -4.95f, 1.08f, 2.34f, 1.08f)
                .texOffs(185, 312).mirror(true).addBox(-1.44f, -1.08f, -5.04f, 2.88f, 1.08f, 0.72f)
                .texOffs(119, 312).mirror(true).addBox(0.36f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f)
                .texOffs(119, 312).mirror(true).addBox(-3.24f, 1.8f, -5.22f, 2.88f, 2.88f, 1.8f), PartPose.offsetAndRotation(0, 2.52f, -7.2f, 0.35f, 0, 0));
        return LayerDefinition.create(mesh, 320, 320);
        // GENERATED: end
    }

    /** The nine heads, in the order they wake: the middle, then out from it, right and left in turn. */
    static final String[] HEADS = {"mid", "right_h1", "left_h1", "right_h2", "left_h2", "right_h3", "left_h3", "right_h4", "left_h4"};
    private static final String[] BODY_LOCKS = {"spine_a", "spine_b", "apron", "right_apron", "left_apron", "right_apron_b", "left_apron_b",
            "right_mane_a", "left_mane_a", "right_mane_b", "left_mane_b", "right_mane_c", "left_mane_c",
            "right_mane_d", "left_mane_d", "right_mane_e", "left_mane_e"};
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
