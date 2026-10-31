package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.spider.CockroachSonEntity;
import com.jttw.engine.entity.spider.HundredEyedDemonEntity;
import com.jttw.engine.entity.spider.MantisSonEntity;
import com.jttw.engine.entity.spider.SpiderDemonEntity;
import com.jttw.engine.entity.spider.SpiderSpiritEntity;
import com.jttw.engine.entity.spider.StingerSonEntity;
import com.jttw.engine.registry.SpiderCaveRegistry;
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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * How Silk-Web Ridge's creatures look: the spider demon (a calf-sized spider of black-violet chitin,
 * its abdomen masked with a crimson hourglass, eight glowing eyes in a heavy brow, eight long banded
 * legs), the stinging sons (upright insect demons on glassy wings, banded abdomens curled to their
 * stings, barbed lances; honeybee, hornet and gadfly skins), the Mantis Son (an upright mantis demon,
 * scythes folded, a crimson sash), the Cockroach Son (a squat beetle-demon in bronze-bound chestnut
 * shell), the Seven Spider Spirits (seven sisters in Tang court silks, eight pale spider's legs folded
 * like a fan behind them, a diadem of eight small gems), and the Hundred-Eyed Demon Lord (a Daoist in
 * temple yellow on a centipede's body, golden eyes all over him under his robe). Models and skins
 * from {@code assets_source/silk_kin.py}, {@code spider_spirits.py} and {@code hundred_eyed.py}: the
 * code between the GENERATED markers is theirs.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SpiderCaveRenderers {

    private SpiderCaveRenderers() {}

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(JTTWEngine.id(name), "main");
    }

    public static final ModelLayerLocation SPIDER_LAYER = layer("spider_demon"), STINGER_LAYER = layer("stinger_son"),
            MANTIS_LAYER = layer("mantis_son"), ROACH_LAYER = layer("cockroach_son"), SISTER_LAYER = layer("spider_spirit"),
            LORD_LAYER = layer("hundred_eyed_demon");

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SPIDER_LAYER, SpiderCaveRenderers::spiderLayer);
        event.registerLayerDefinition(STINGER_LAYER, SpiderCaveRenderers::stingerLayer);
        event.registerLayerDefinition(MANTIS_LAYER, SpiderCaveRenderers::mantisLayer);
        event.registerLayerDefinition(ROACH_LAYER, SpiderCaveRenderers::roachLayer);
        event.registerLayerDefinition(SISTER_LAYER, SpiderCaveRenderers::sisterLayer);
        event.registerLayerDefinition(LORD_LAYER, SpiderCaveRenderers::lordLayer);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SpiderCaveRegistry.SPIDER_DEMON.get(), ctx -> new Glowing<>(ctx, new SpiderModel(ctx.bakeLayer(SPIDER_LAYER)), 1.1f,
                SpiderModel.SIZE, e -> "spider_demon"));
        event.registerEntityRenderer(SpiderCaveRegistry.STINGER_SON.get(), ctx -> new Glowing<>(ctx, new StingerModel(ctx.bakeLayer(STINGER_LAYER)), 0.5f,
                StingerModel.SIZE, e -> StingerSonEntity.KINDS[e.kind()]));
        event.registerEntityRenderer(SpiderCaveRegistry.MANTIS_SON.get(), ctx -> new Glowing<>(ctx, new MantisModel(ctx.bakeLayer(MANTIS_LAYER)), 0.7f,
                MantisModel.SIZE, e -> "mantis_son"));
        event.registerEntityRenderer(SpiderCaveRegistry.COCKROACH_SON.get(), ctx -> new Glowing<>(ctx, new RoachModel(ctx.bakeLayer(ROACH_LAYER)), 1.1f,
                RoachModel.SIZE, e -> "cockroach_son"));
        event.registerEntityRenderer(SpiderCaveRegistry.SPIDER_SPIRIT.get(), ctx -> new Glowing<>(ctx, new SisterModel(ctx.bakeLayer(SISTER_LAYER)), 0.6f,
                SisterModel.SIZE, e -> "spider_spirit_" + SpiderSpiritEntity.NAMES[e.sister()]));
        event.registerEntityRenderer(SpiderCaveRegistry.HUNDRED_EYED_DEMON.get(), LordRenderer::new);
        event.registerEntityRenderer(SpiderCaveRegistry.SILK_STRAND.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.9f, true));
    }

    /** Turns a part this much from its rest. */
    static void turn(ModelPart p, float x, float y, float z) {
        p.xRot = p.getInitialPose().xRot + x;
        p.yRot = p.getInitialPose().yRot + y;
        p.zRot = p.getInitialPose().zRot + z;
    }

    /** A part's mirror twin ("left_" for "right_"), turned the mirrored way. */
    static void turnPair(ModelPart right, ModelPart left, float x, float y, float z) {
        turn(right, x, y, z);
        turn(left, x, -y, -z);
    }

    static ModelPart find(ModelPart root, String name) {
        if (root.hasChild(name)) return root.getChild(name);
        for (ModelPart c : root.getAllParts().toList()) if (c != root && c.hasChild(name)) return c.getChild(name);
        throw new IllegalStateException("no part " + name);
    }

    /** A model drawn whole from its root. */
    abstract static class Whole<T extends Mob> extends EntityModel<T> {
        final ModelPart root;

        Whole(ModelPart root, Function<ResourceLocation, RenderType> type) {
            super(type);
            this.root = root;
        }

        Whole(ModelPart root) {
            this(root, RenderType::entityCutoutNoCull);
        }

        ModelPart p(String name) {
            return find(root, name);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    /** A creature's renderer: its skin (by its kind), drawn this much larger than its pixels, its glowing skin full-bright over it. */
    static class Glowing<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
        private final float size;
        private final Function<T, String> skin;

        Glowing(EntityRendererProvider.Context ctx, M model, float shadow, float size, Function<T, String> skin) {
            super(ctx, model, shadow);
            this.size = size;
            this.skin = skin;
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float swing, float amount, float partial,
                                   float age, float yaw, float pitch) {
                    VertexConsumer vc = buffers.getBuffer(RenderType.eyes(tex(skin.apply(e) + "_glow")));
                    getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        @Override
        protected void scale(T e, PoseStack pose, float partial) {
            pose.scale(size, size, size);
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return tex(skin.apply(e));
        }

        @Override
        protected boolean shouldShowName(T e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < 24 * 24;
        }
    }

    // ── The spider demon ────────────────────────────────────────────────

    public static LayerDefinition spiderLayer() {
        // GENERATED spider: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(86, 0).mirror(false).addBox(-5, -4, -6, 10, 7, 11)
                .texOffs(0, 43).mirror(false).addBox(-2, -5, -5, 4, 1.2f, 9)
                .texOffs(27, 43).mirror(false).addBox(-5.5f, -1, -5, 11, 2, 9)
                .texOffs(68, 43).mirror(false).addBox(-3.6f, -5.8f, -4.2f, 7.2f, 1.2f, 7.6f), PartPose.offsetAndRotation(0, 12, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(49, 27).mirror(false).addBox(-4, -3.5f, -4.5f, 8, 6.5f, 4.5f)
                .texOffs(0, 62).mirror(false).addBox(-4.6f, -4.4f, -5, 9.2f, 1.8f, 3)
                .texOffs(100, 68).mirror(false).addBox(-3, 2.2f, -5, 6, 1.2f, 1), PartPose.offsetAndRotation(0, -0.5f, -6, 0.12f, 0, 0));
        PartDefinition rightEyeGreat = head.addOrReplaceChild("right_eye_great", CubeListBuilder.create()
                .texOffs(115, 68).mirror(false).addBox(-1, -1, -0.8f, 2, 2, 1), PartPose.offsetAndRotation(-1.6f, -1.6f, -4.6f, 0, 0.15f, 0));
        PartDefinition leftEyeGreat = head.addOrReplaceChild("left_eye_great", CubeListBuilder.create()
                .texOffs(115, 68).mirror(true).addBox(-1, -1, -0.8f, 2, 2, 1), PartPose.offsetAndRotation(1.6f, -1.6f, -4.6f, 0, -0.15f, 0));
        PartDefinition rightEyeSmall0 = head.addOrReplaceChild("right_eye_small_0", CubeListBuilder.create()
                .texOffs(122, 68).mirror(false).addBox(-0.6f, -0.6f, -0.5f, 1.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(-3.2f, -2.6f, -4.5f, 0, 0.3f, 0));
        PartDefinition leftEyeSmall0 = head.addOrReplaceChild("left_eye_small_0", CubeListBuilder.create()
                .texOffs(122, 68).mirror(true).addBox(-0.6f, -0.6f, -0.5f, 1.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(3.2f, -2.6f, -4.5f, 0, -0.3f, 0));
        PartDefinition rightEyeSmall1 = head.addOrReplaceChild("right_eye_small_1", CubeListBuilder.create()
                .texOffs(5, 73).mirror(false).addBox(-0.5f, -0.5f, -0.5f, 1, 1, 0.6f), PartPose.offsetAndRotation(-0.7f, -3.3f, -4.6f, 0, 0, 0));
        PartDefinition leftEyeSmall1 = head.addOrReplaceChild("left_eye_small_1", CubeListBuilder.create()
                .texOffs(5, 73).mirror(true).addBox(-0.5f, -0.5f, -0.5f, 1, 1, 0.6f), PartPose.offsetAndRotation(0.7f, -3.3f, -4.6f, 0, 0, 0));
        PartDefinition rightEyeSmall2 = head.addOrReplaceChild("right_eye_small_2", CubeListBuilder.create()
                .texOffs(5, 73).mirror(false).addBox(-0.5f, -0.5f, -0.5f, 1, 1, 0.6f), PartPose.offsetAndRotation(-3, 0.2f, -4.4f, 0, 0.3f, 0));
        PartDefinition leftEyeSmall2 = head.addOrReplaceChild("left_eye_small_2", CubeListBuilder.create()
                .texOffs(5, 73).mirror(true).addBox(-0.5f, -0.5f, -0.5f, 1, 1, 0.6f), PartPose.offsetAndRotation(3, 0.2f, -4.4f, 0, -0.3f, 0));
        PartDefinition rightFang = head.addOrReplaceChild("right_fang", CubeListBuilder.create()
                .texOffs(101, 43).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 3.4f, 2.4f)
                .texOffs(27, 62).mirror(false).addBox(-0.6f, 3.2f, -1.3f, 1.2f, 2.4f, 1.2f), PartPose.offsetAndRotation(-1.5f, 2.6f, -4.2f, 0.25f, 0, 0.12f));
        PartDefinition leftFang = head.addOrReplaceChild("left_fang", CubeListBuilder.create()
                .texOffs(101, 43).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 3.4f, 2.4f)
                .texOffs(27, 62).mirror(true).addBox(-0.6f, 3.2f, -1.3f, 1.2f, 2.4f, 1.2f), PartPose.offsetAndRotation(1.5f, 2.6f, -4.2f, 0.25f, 0, -0.12f));
        PartDefinition rightPalp = head.addOrReplaceChild("right_palp", CubeListBuilder.create()
                .texOffs(114, 43).mirror(false).addBox(-0.7f, 0, -0.7f, 1.4f, 4, 1.4f)
                .texOffs(36, 62).mirror(false).addBox(-0.8f, 3.6f, -0.8f, 1.6f, 1.8f, 1.6f), PartPose.offsetAndRotation(-3.4f, 2.2f, -3.6f, -0.7f, 0.35f, 0.35f));
        PartDefinition leftPalp = head.addOrReplaceChild("left_palp", CubeListBuilder.create()
                .texOffs(114, 43).mirror(true).addBox(-0.7f, 0, -0.7f, 1.4f, 4, 1.4f)
                .texOffs(36, 62).mirror(true).addBox(-0.8f, 3.6f, -0.8f, 1.6f, 1.8f, 1.6f), PartPose.offsetAndRotation(3.4f, 2.2f, -3.6f, -0.7f, -0.35f, -0.35f));
        PartDefinition abdomen = body.addOrReplaceChild("abdomen", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-7, -6, 0, 14, 11, 15)
                .texOffs(0, 27).mirror(false).addBox(-5.6f, -8, 1.5f, 11.2f, 2.2f, 12)
                .texOffs(59, 0).mirror(false).addBox(-8, -4, 2, 1.2f, 8, 11)
                .texOffs(59, 0).mirror(false).addBox(6.8f, -4, 2, 1.2f, 8, 11)
                .texOffs(76, 27).mirror(false).addBox(-5.5f, -4.5f, 14, 11, 8, 4)
                .texOffs(45, 62).mirror(false).addBox(-1.5f, -1, 17.5f, 3, 2, 2)
                .texOffs(56, 62).mirror(false).addBox(-0.6f, -9.6f, 2.5f, 1.2f, 2, 1.2f)
                .texOffs(56, 62).mirror(false).addBox(-0.6f, -9.6f, 5.5f, 1.2f, 2, 1.2f)
                .texOffs(56, 62).mirror(false).addBox(-0.6f, -9.6f, 8.5f, 1.2f, 2, 1.2f)
                .texOffs(56, 62).mirror(false).addBox(-0.6f, -9.6f, 11.5f, 1.2f, 2, 1.2f), PartPose.offsetAndRotation(0, -1.5f, 4.5f, -0.28f, 0, 0));
        PartDefinition rightHip0 = body.addOrReplaceChild("right_hip_0", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.6f, 0.5f, -4.2f, 0, -0.75f, 0));
        PartDefinition leftHip0 = body.addOrReplaceChild("left_hip_0", CubeListBuilder.create(), PartPose.offsetAndRotation(4.6f, 0.5f, -4.2f, 0, 0.75f, 0));
        PartDefinition rightLeg0 = rightHip0.addOrReplaceChild("right_leg_0", CubeListBuilder.create()
                .texOffs(0, 55).mirror(false).addBox(-2, -1.5f, -1.5f, 2, 3, 3)
                .texOffs(11, 55).mirror(false).addBox(-12.32f, -1.1f, -1.1f, 12.32f, 2.2f, 2.2f)
                .texOffs(0, 73).mirror(false).addBox(-6.72f, -2.2f, -0.35f, 0.7f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.8f));
        PartDefinition leftLeg0 = leftHip0.addOrReplaceChild("left_leg_0", CubeListBuilder.create()
                .texOffs(0, 55).mirror(true).addBox(0, -1.5f, -1.5f, 2, 3, 3)
                .texOffs(11, 55).mirror(true).addBox(0, -1.1f, -1.1f, 12.32f, 2.2f, 2.2f)
                .texOffs(0, 73).mirror(true).addBox(6.02f, -2.2f, -0.35f, 0.7f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.8f));
        PartDefinition rightShin0 = rightLeg0.addOrReplaceChild("right_shin_0", CubeListBuilder.create()
                .texOffs(65, 62).mirror(false).addBox(-10.08f, -0.9f, -0.9f, 10.08f, 1.8f, 1.8f)
                .texOffs(92, 62).mirror(false).addBox(-21.28f, -0.6f, -0.6f, 11.424f, 1.2f, 1.2f)
                .texOffs(44, 55).mirror(false).addBox(-1.4f, -1.3f, -1.3f, 1.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(-12.32f, 0, 0, 0, 0, -2));
        PartDefinition leftShin0 = leftLeg0.addOrReplaceChild("left_shin_0", CubeListBuilder.create()
                .texOffs(65, 62).mirror(true).addBox(0, -0.9f, -0.9f, 10.08f, 1.8f, 1.8f)
                .texOffs(92, 62).mirror(true).addBox(9.856f, -0.6f, -0.6f, 11.424f, 1.2f, 1.2f)
                .texOffs(44, 55).mirror(true).addBox(-0.2f, -1.3f, -1.3f, 1.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(12.32f, 0, 0, 0, 0, 2));
        PartDefinition rightHip1 = body.addOrReplaceChild("right_hip_1", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.6f, 0.5f, -1.6f, 0, -0.28f, 0));
        PartDefinition leftHip1 = body.addOrReplaceChild("left_hip_1", CubeListBuilder.create(), PartPose.offsetAndRotation(4.6f, 0.5f, -1.6f, 0, 0.28f, 0));
        PartDefinition rightLeg1 = rightHip1.addOrReplaceChild("right_leg_1", CubeListBuilder.create()
                .texOffs(0, 55).mirror(false).addBox(-2, -1.5f, -1.5f, 2, 3, 3)
                .texOffs(55, 55).mirror(false).addBox(-11, -1.1f, -1.1f, 11, 2.2f, 2.2f)
                .texOffs(0, 73).mirror(false).addBox(-6, -2.2f, -0.35f, 0.7f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.8f));
        PartDefinition leftLeg1 = leftHip1.addOrReplaceChild("left_leg_1", CubeListBuilder.create()
                .texOffs(0, 55).mirror(true).addBox(0, -1.5f, -1.5f, 2, 3, 3)
                .texOffs(55, 55).mirror(true).addBox(0, -1.1f, -1.1f, 11, 2.2f, 2.2f)
                .texOffs(0, 73).mirror(true).addBox(5.3f, -2.2f, -0.35f, 0.7f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.8f));
        PartDefinition rightShin1 = rightLeg1.addOrReplaceChild("right_shin_1", CubeListBuilder.create()
                .texOffs(0, 68).mirror(false).addBox(-9, -0.9f, -0.9f, 9, 1.8f, 1.8f)
                .texOffs(23, 68).mirror(false).addBox(-19, -0.6f, -0.6f, 10.2f, 1.2f, 1.2f)
                .texOffs(44, 55).mirror(false).addBox(-1.4f, -1.3f, -1.3f, 1.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(-11, 0, 0, 0, 0, -2));
        PartDefinition leftShin1 = leftLeg1.addOrReplaceChild("left_shin_1", CubeListBuilder.create()
                .texOffs(0, 68).mirror(true).addBox(0, -0.9f, -0.9f, 9, 1.8f, 1.8f)
                .texOffs(23, 68).mirror(true).addBox(8.8f, -0.6f, -0.6f, 10.2f, 1.2f, 1.2f)
                .texOffs(44, 55).mirror(true).addBox(-0.2f, -1.3f, -1.3f, 1.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(11, 0, 0, 0, 0, 2));
        PartDefinition rightHip2 = body.addOrReplaceChild("right_hip_2", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.6f, 0.5f, 1, 0, 0.25f, 0));
        PartDefinition leftHip2 = body.addOrReplaceChild("left_hip_2", CubeListBuilder.create(), PartPose.offsetAndRotation(4.6f, 0.5f, 1, 0, -0.25f, 0));
        PartDefinition rightLeg2 = rightHip2.addOrReplaceChild("right_leg_2", CubeListBuilder.create()
                .texOffs(0, 55).mirror(false).addBox(-2, -1.5f, -1.5f, 2, 3, 3)
                .texOffs(55, 55).mirror(false).addBox(-10.45f, -1.1f, -1.1f, 10.45f, 2.2f, 2.2f)
                .texOffs(0, 73).mirror(false).addBox(-5.7f, -2.2f, -0.35f, 0.7f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.8f));
        PartDefinition leftLeg2 = leftHip2.addOrReplaceChild("left_leg_2", CubeListBuilder.create()
                .texOffs(0, 55).mirror(true).addBox(0, -1.5f, -1.5f, 2, 3, 3)
                .texOffs(55, 55).mirror(true).addBox(0, -1.1f, -1.1f, 10.45f, 2.2f, 2.2f)
                .texOffs(0, 73).mirror(true).addBox(5, -2.2f, -0.35f, 0.7f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.8f));
        PartDefinition rightShin2 = rightLeg2.addOrReplaceChild("right_shin_2", CubeListBuilder.create()
                .texOffs(0, 68).mirror(false).addBox(-8.55f, -0.9f, -0.9f, 8.55f, 1.8f, 1.8f)
                .texOffs(50, 68).mirror(false).addBox(-18.05f, -0.6f, -0.6f, 9.69f, 1.2f, 1.2f)
                .texOffs(44, 55).mirror(false).addBox(-1.4f, -1.3f, -1.3f, 1.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(-10.45f, 0, 0, 0, 0, -2));
        PartDefinition leftShin2 = leftLeg2.addOrReplaceChild("left_shin_2", CubeListBuilder.create()
                .texOffs(0, 68).mirror(true).addBox(0, -0.9f, -0.9f, 8.55f, 1.8f, 1.8f)
                .texOffs(50, 68).mirror(true).addBox(8.36f, -0.6f, -0.6f, 9.69f, 1.2f, 1.2f)
                .texOffs(44, 55).mirror(true).addBox(-0.2f, -1.3f, -1.3f, 1.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(10.45f, 0, 0, 0, 0, 2));
        PartDefinition rightHip3 = body.addOrReplaceChild("right_hip_3", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.6f, 0.5f, 3.6f, 0, 0.78f, 0));
        PartDefinition leftHip3 = body.addOrReplaceChild("left_hip_3", CubeListBuilder.create(), PartPose.offsetAndRotation(4.6f, 0.5f, 3.6f, 0, -0.78f, 0));
        PartDefinition rightLeg3 = rightHip3.addOrReplaceChild("right_leg_3", CubeListBuilder.create()
                .texOffs(0, 55).mirror(false).addBox(-2, -1.5f, -1.5f, 2, 3, 3)
                .texOffs(84, 55).mirror(false).addBox(-11.66f, -1.1f, -1.1f, 11.66f, 2.2f, 2.2f)
                .texOffs(0, 73).mirror(false).addBox(-6.36f, -2.2f, -0.35f, 0.7f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.8f));
        PartDefinition leftLeg3 = leftHip3.addOrReplaceChild("left_leg_3", CubeListBuilder.create()
                .texOffs(0, 55).mirror(true).addBox(0, -1.5f, -1.5f, 2, 3, 3)
                .texOffs(84, 55).mirror(true).addBox(0, -1.1f, -1.1f, 11.66f, 2.2f, 2.2f)
                .texOffs(0, 73).mirror(true).addBox(5.66f, -2.2f, -0.35f, 0.7f, 1.2f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.8f));
        PartDefinition rightShin3 = rightLeg3.addOrReplaceChild("right_shin_3", CubeListBuilder.create()
                .texOffs(75, 68).mirror(false).addBox(-9.54f, -0.9f, -0.9f, 9.54f, 1.8f, 1.8f)
                .texOffs(23, 68).mirror(false).addBox(-20.14f, -0.6f, -0.6f, 10.812f, 1.2f, 1.2f)
                .texOffs(44, 55).mirror(false).addBox(-1.4f, -1.3f, -1.3f, 1.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(-11.66f, 0, 0, 0, 0, -2));
        PartDefinition leftShin3 = leftLeg3.addOrReplaceChild("left_shin_3", CubeListBuilder.create()
                .texOffs(75, 68).mirror(true).addBox(0, -0.9f, -0.9f, 9.54f, 1.8f, 1.8f)
                .texOffs(23, 68).mirror(true).addBox(9.328f, -0.6f, -0.6f, 10.812f, 1.2f, 1.2f)
                .texOffs(44, 55).mirror(true).addBox(-0.2f, -1.3f, -1.3f, 1.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(11.66f, 0, 0, 0, 0, 2));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED spider: end
    }

    /**
     * The spider demon: its eight legs stepping in two alternating sets of four, each swinging and
     * lifting in turn; its palps feeling; its abdomen swelling with its breath. Rearing to spit, its
     * forebody lifts and its front legs rise; biting, its fangs open and strike in.
     */
    public static final class SpiderModel extends Whole<SpiderDemonEntity> {
        public static final float SIZE = 0.95f;
        private final ModelPart body, head, abdomen, rightFang, leftFang, rightPalp, leftPalp;
        private final ModelPart[] rightHip = new ModelPart[4], leftHip = new ModelPart[4], rightLeg = new ModelPart[4], leftLeg = new ModelPart[4],
                rightShin = new ModelPart[4], leftShin = new ModelPart[4];

        public SpiderModel(ModelPart root) {
            super(root);
            body = p("body");
            head = p("head");
            abdomen = p("abdomen");
            rightFang = p("right_fang");
            leftFang = p("left_fang");
            rightPalp = p("right_palp");
            leftPalp = p("left_palp");
            for (int i = 0; i < 4; i++) {
                rightHip[i] = p("right_hip_" + i);
                leftHip[i] = p("left_hip_" + i);
                rightLeg[i] = p("right_leg_" + i);
                leftLeg[i] = p("left_leg_" + i);
                rightShin[i] = p("right_shin_" + i);
                leftShin[i] = p("left_shin_" + i);
            }
        }

        @Override
        public void setupAnim(SpiderDemonEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            boolean rear = e.isRearing();
            float w = swing * 1.2f, breath = Mth.sin(age * 0.09f);
            turn(body, rear ? -0.38f : 0, 0, 0);
            body.y = body.getInitialPose().y - (rear ? 2.5f : 0) + Mth.abs(Mth.sin(w)) * 0.4f * amount;
            turn(head, pitch * Mth.DEG_TO_RAD * 0.5f + strike * 0.3f, yaw * Mth.DEG_TO_RAD * 0.5f, 0);
            turn(abdomen, (rear ? 0.45f : 0) + breath * 0.03f, Mth.sin(w * 0.5f) * 0.06f * amount, 0);
            abdomen.yScale = 1 + breath * 0.03f;
            float open = rear ? 0.35f : strike * 0.5f;
            turnPair(rightFang, leftFang, -strike * 0.5f, 0, -open);
            turnPair(rightPalp, leftPalp, Mth.sin(age * 0.2f) * 0.15f - strike * 0.4f, 0, Mth.cos(age * 0.17f) * 0.1f);
            for (int i = 0; i < 4; i++) {
                // two sets of four, each pair of legs crossing: right 0 and 2 with left 1 and 3
                float ph = ((i % 2 == 0) ? 0 : Mth.PI);
                float r = Mth.cos(w + ph), l = Mth.cos(w + ph + Mth.PI);
                float liftR = Math.max(0, Mth.sin(w + ph)) * 0.35f * amount, liftL = Math.max(0, Mth.sin(w + ph + Mth.PI)) * 0.35f * amount;
                float idle = Mth.sin(age * 0.07f + i) * 0.03f;
                turn(rightHip[i], 0, r * 0.32f * amount, 0);
                turn(leftHip[i], 0, -l * 0.32f * amount, 0);
                float up = rear && i < 2 ? 0.75f - i * 0.25f : 0;
                turn(rightLeg[i], 0, 0, liftR + up + idle);
                turn(leftLeg[i], 0, 0, -(liftL + up + idle));
                turn(rightShin[i], 0, 0, -liftR * 0.5f + (rear && i < 2 ? -0.4f : 0));
                turn(leftShin[i], 0, 0, liftL * 0.5f - (rear && i < 2 ? -0.4f : 0));
            }
        }
    }

    // ── The stinging sons ───────────────────────────────────────────────

    public static LayerDefinition stingerLayer() {
        // GENERATED stinger: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition thorax = root.addOrReplaceChild("thorax", CubeListBuilder.create()
                .texOffs(32, 0).mirror(false).addBox(-3.5f, -6, -2.5f, 7, 7, 5)
                .texOffs(99, 0).mirror(false).addBox(-4, -6.8f, -3, 8, 2, 6)
                .texOffs(90, 22).mirror(false).addBox(-2.4f, 0.4f, -2, 4.8f, 1.6f, 4), PartPose.offsetAndRotation(0, 8, 0, 0.15f, 0, 0));
        PartDefinition head = thorax.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(57, 0).mirror(false).addBox(-3, -5, -3.2f, 6, 5, 4.6f)
                .texOffs(72, 31).mirror(false).addBox(-2, -1.2f, -3.6f, 4, 1.8f, 1.4f), PartPose.offsetAndRotation(0, -6.6f, -0.6f, 0, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(0, 22).mirror(false).addBox(-1.3f, -2, -1.8f, 2, 4, 3.4f), PartPose.offsetAndRotation(-2.6f, -2.8f, -1.4f, 0, 0.2f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(0, 22).mirror(true).addBox(-0.7f, -2, -1.8f, 2, 4, 3.4f), PartPose.offsetAndRotation(2.6f, -2.8f, -1.4f, 0, -0.2f, 0));
        PartDefinition rightMandible = head.addOrReplaceChild("right_mandible", CubeListBuilder.create()
                .texOffs(85, 31).mirror(false).addBox(-1, 0, -1.4f, 1.6f, 1.4f, 1.6f), PartPose.offsetAndRotation(-1.1f, -0.3f, -3.2f, 0, 0, 0.15f));
        PartDefinition leftMandible = head.addOrReplaceChild("left_mandible", CubeListBuilder.create()
                .texOffs(85, 31).mirror(true).addBox(-0.6f, 0, -1.4f, 1.6f, 1.4f, 1.6f), PartPose.offsetAndRotation(1.1f, -0.3f, -3.2f, 0, 0, -0.15f));
        PartDefinition rightAntenna = head.addOrReplaceChild("right_antenna", CubeListBuilder.create()
                .texOffs(109, 22).mirror(false).addBox(-0.35f, -5, -0.35f, 0.7f, 5, 0.7f)
                .texOffs(94, 31).mirror(false).addBox(-0.45f, -7.4f, -0.45f, 0.9f, 2.6f, 0.9f), PartPose.offsetAndRotation(-1, -4.8f, -2.8f, 0.55f, 0.3f, -0.25f));
        PartDefinition leftAntenna = head.addOrReplaceChild("left_antenna", CubeListBuilder.create()
                .texOffs(109, 22).mirror(true).addBox(-0.35f, -5, -0.35f, 0.7f, 5, 0.7f)
                .texOffs(94, 31).mirror(true).addBox(-0.45f, -7.4f, -0.45f, 0.9f, 2.6f, 0.9f), PartPose.offsetAndRotation(1, -4.8f, -2.8f, 0.55f, -0.3f, 0.25f));
        PartDefinition rightArm = thorax.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(13, 22).mirror(false).addBox(-1.2f, -0.6f, -1.2f, 2, 6, 2), PartPose.offsetAndRotation(-3.6f, -5, -0.4f, -0.55f, 0, 0.25f));
        PartDefinition leftArm = thorax.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(13, 22).mirror(true).addBox(-0.8f, -0.6f, -1.2f, 2, 6, 2), PartPose.offsetAndRotation(3.6f, -5, -0.4f, -0.55f, 0, -0.25f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(22, 22).mirror(false).addBox(-1, 0, -1, 1.8f, 5, 1.8f)
                .texOffs(48, 31).mirror(false).addBox(-1.2f, 4.6f, -1.3f, 2.2f, 1.6f, 2.2f), PartPose.offsetAndRotation(-0.2f, 5.4f, 0, -0.75f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(22, 22).mirror(true).addBox(-0.8f, 0, -1, 1.8f, 5, 1.8f)
                .texOffs(48, 31).mirror(true).addBox(-1, 4.6f, -1.3f, 2.2f, 1.6f, 2.2f), PartPose.offsetAndRotation(0.2f, 5.4f, 0, -0.75f, 0, 0));
        PartDefinition lance = rightForearm.addOrReplaceChild("lance", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.4f, -12, -0.4f, 0.8f, 20, 0.8f)
                .texOffs(114, 22).mirror(false).addBox(-0.75f, -15.5f, -0.75f, 1.5f, 3.5f, 1.5f)
                .texOffs(99, 31).mirror(false).addBox(-1.4f, -12.6f, -0.25f, 2.8f, 0.8f, 0.5f), PartPose.offsetAndRotation(-0.1f, 5.4f, -0.2f, 1.3f, 0, 0));
        PartDefinition rightLeg0 = thorax.addOrReplaceChild("right_leg_0", CubeListBuilder.create()
                .texOffs(31, 22).mirror(false).addBox(-0.6f, 0, -0.6f, 1.2f, 5, 1.2f), PartPose.offsetAndRotation(-2.6f, 0, -1.2f, 0.6f, 0, 0.55f));
        PartDefinition leftLeg0 = thorax.addOrReplaceChild("left_leg_0", CubeListBuilder.create()
                .texOffs(31, 22).mirror(true).addBox(-0.6f, 0, -0.6f, 1.2f, 5, 1.2f), PartPose.offsetAndRotation(2.6f, 0, -1.2f, 0.6f, 0, -0.55f));
        PartDefinition rightShin0 = rightLeg0.addOrReplaceChild("right_shin_0", CubeListBuilder.create()
                .texOffs(40, 22).mirror(false).addBox(-0.5f, 0, -0.5f, 1, 5.5f, 1), PartPose.offsetAndRotation(0, 5, 0, -1.3f, 0, 0));
        PartDefinition leftShin0 = leftLeg0.addOrReplaceChild("left_shin_0", CubeListBuilder.create()
                .texOffs(40, 22).mirror(true).addBox(-0.5f, 0, -0.5f, 1, 5.5f, 1), PartPose.offsetAndRotation(0, 5, 0, -1.3f, 0, 0));
        PartDefinition rightLeg1 = thorax.addOrReplaceChild("right_leg_1", CubeListBuilder.create()
                .texOffs(31, 22).mirror(false).addBox(-0.6f, 0, -0.6f, 1.2f, 5, 1.2f), PartPose.offsetAndRotation(-2.4f, 0.4f, 1, 0.2f, 0, 0.6f));
        PartDefinition leftLeg1 = thorax.addOrReplaceChild("left_leg_1", CubeListBuilder.create()
                .texOffs(31, 22).mirror(true).addBox(-0.6f, 0, -0.6f, 1.2f, 5, 1.2f), PartPose.offsetAndRotation(2.4f, 0.4f, 1, 0.2f, 0, -0.6f));
        PartDefinition rightShin1 = rightLeg1.addOrReplaceChild("right_shin_1", CubeListBuilder.create()
                .texOffs(40, 22).mirror(false).addBox(-0.5f, 0, -0.5f, 1, 5.5f, 1), PartPose.offsetAndRotation(0, 5, 0, -1.1f, 0, 0));
        PartDefinition leftShin1 = leftLeg1.addOrReplaceChild("left_shin_1", CubeListBuilder.create()
                .texOffs(40, 22).mirror(true).addBox(-0.5f, 0, -0.5f, 1, 5.5f, 1), PartPose.offsetAndRotation(0, 5, 0, -1.1f, 0, 0));
        PartDefinition abdomen = thorax.addOrReplaceChild("abdomen", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-3, -1, 0, 6, 5.5f, 7)
                .texOffs(80, 0).mirror(false).addBox(-2.5f, -0.5f, 6.5f, 5, 4.5f, 4)
                .texOffs(0, 31).mirror(false).addBox(-1.4f, 0.4f, 10, 2.8f, 2.6f, 2.2f), PartPose.offsetAndRotation(0, 1.4f, 1.2f, -0.95f, 0, 0));
        PartDefinition stinger = abdomen.addOrReplaceChild("stinger", CubeListBuilder.create()
                .texOffs(61, 31).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 3.4f), PartPose.offsetAndRotation(0, 1.6f, 12, 0.25f, 0, 0));
        PartDefinition rightWingFore = thorax.addOrReplaceChild("right_wing_fore", CubeListBuilder.create()
                .texOffs(45, 22).mirror(false).addBox(-16, -0.1f, -2.6f, 16, 0.2f, 6), PartPose.offsetAndRotation(-1.4f, -5.6f, 2, 0.15f, 0.55f, 0.35f));
        PartDefinition leftWingFore = thorax.addOrReplaceChild("left_wing_fore", CubeListBuilder.create()
                .texOffs(45, 22).mirror(true).addBox(0, -0.1f, -2.6f, 16, 0.2f, 6), PartPose.offsetAndRotation(1.4f, -5.6f, 2, 0.15f, -0.55f, -0.35f));
        PartDefinition rightWingHind = thorax.addOrReplaceChild("right_wing_hind", CubeListBuilder.create()
                .texOffs(13, 31).mirror(false).addBox(-12, -0.1f, -2, 12, 0.2f, 4.4f), PartPose.offsetAndRotation(-1.4f, -4.2f, 2.6f, 0.25f, 0.85f, 0.2f));
        PartDefinition leftWingHind = thorax.addOrReplaceChild("left_wing_hind", CubeListBuilder.create()
                .texOffs(13, 31).mirror(true).addBox(0, -0.1f, -2, 12, 0.2f, 4.4f), PartPose.offsetAndRotation(1.4f, -4.2f, 2.6f, 0.25f, -0.85f, -0.2f));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED stinger: end
    }

    /**
     * A stinging son in flight: its wings a blur of beats, its body bobbing; its legs hang; its lance
     * held at the ready, thrust on a blow. Buzzing before a dart, it trembles, its abdomen curling
     * forward under it, sting first; darting, it pitches forward, wings swept back.
     */
    public static final class StingerModel extends Whole<StingerSonEntity> {
        public static final float SIZE = 1.0f;
        private final ModelPart thorax, head, abdomen, rightWing, leftWing, rightHind, leftHind, rightArm, leftArm, rightForearm,
                rightAntenna, leftAntenna;
        private final List<ModelPart> legs = new ArrayList<>();

        public StingerModel(ModelPart root) {
            super(root, RenderType::entityTranslucent);
            thorax = p("thorax");
            head = p("head");
            abdomen = p("abdomen");
            rightWing = p("right_wing_fore");
            leftWing = p("left_wing_fore");
            rightHind = p("right_wing_hind");
            leftHind = p("left_wing_hind");
            rightArm = p("right_arm");
            leftArm = p("left_arm");
            rightForearm = p("right_forearm");
            rightAntenna = p("right_antenna");
            leftAntenna = p("left_antenna");
            for (String s : new String[]{"right_leg_0", "left_leg_0", "right_leg_1", "left_leg_1"}) legs.add(p(s));
        }

        @Override
        public void setupAnim(StingerSonEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            int mode = e.mode();
            boolean buzz = mode == StingerSonEntity.BUZZING, dart = mode == StingerSonEntity.DARTING;
            float beat = Mth.sin(age * 2.6f);
            float shake = buzz ? Mth.sin(age * 4f) * 0.4f : 0;
            thorax.y = thorax.getInitialPose().y + Mth.sin(age * 0.25f) * 0.8f;
            thorax.x = thorax.getInitialPose().x + shake;
            turn(thorax, dart ? 0.75f : amount * 0.25f, 0, 0);
            turn(head, pitch * Mth.DEG_TO_RAD - (dart ? 0.5f : 0), yaw * Mth.DEG_TO_RAD, 0);
            turn(abdomen, buzz ? 0.95f : dart ? 0.7f : Mth.sin(age * 0.2f) * 0.08f, 0, 0);
            float sweep = dart ? -0.9f : 0;
            turnPair(rightWing, leftWing, 0, sweep, beat * 0.7f);
            turnPair(rightHind, leftHind, 0, sweep, -beat * 0.6f);
            turnPair(rightAntenna, leftAntenna, Mth.sin(age * 0.3f) * 0.1f - (dart ? 0.5f : 0), 0, 0);
            turn(rightArm, -strike * 1.1f - (dart ? 0.6f : 0), 0, 0);
            turn(leftArm, Mth.sin(age * 0.2f) * 0.05f, 0, 0);
            turn(rightForearm, strike * 0.5f, 0, 0);
            for (int i = 0; i < legs.size(); i++) turn(legs.get(i), Mth.sin(age * 0.15f + i) * 0.1f + (dart ? 0.5f : 0), 0, 0);
        }
    }

    // ── The Mantis Son ──────────────────────────────────────────────────

    public static LayerDefinition mantisLayer() {
        // GENERATED mantis: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(27, 22).mirror(false).addBox(-2.6f, -2.2f, -2.6f, 5.2f, 4.4f, 5.2f)
                .texOffs(5, 35).mirror(false).addBox(-3, -1.4f, -3, 6, 1.6f, 6), PartPose.offsetAndRotation(0, 6.5f, 0, 0, 0, 0));
        PartDefinition sashTail = waist.addOrReplaceChild("sash_tail", CubeListBuilder.create()
                .texOffs(30, 35).mirror(false).addBox(-0.9f, 0, -0.3f, 1.8f, 7, 0.4f), PartPose.offsetAndRotation(1.6f, 0, -3, -0.1f, 0, -0.1f));
        PartDefinition abdomen = waist.addOrReplaceChild("abdomen", CubeListBuilder.create()
                .texOffs(68, 0).mirror(false).addBox(-3.6f, -2.4f, 0, 7.2f, 5.2f, 7.5f)
                .texOffs(0, 22).mirror(false).addBox(-3, -2, 7, 6, 4.6f, 6.5f)
                .texOffs(73, 35).mirror(false).addBox(-1.8f, -1, 12, 3.6f, 2.8f, 3), PartPose.offsetAndRotation(0, -0.6f, 2.2f, -0.32f, 0, 0));
        PartDefinition wings = abdomen.addOrReplaceChild("wings", CubeListBuilder.create()
                .texOffs(17, 0).mirror(false).addBox(-3.9f, -0.9f, 0, 7.8f, 1, 16.5f), PartPose.offsetAndRotation(0, -2.7f, -0.5f, 0.06f, 0, 0));
        PartDefinition prothorax = waist.addOrReplaceChild("prothorax", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.9f, -17, -1.9f, 3.8f, 17, 3.8f)
                .texOffs(66, 22).mirror(false).addBox(-2.6f, -17.6f, -2.6f, 5.2f, 2.6f, 5.2f)
                .texOffs(77, 45).mirror(false).addBox(-2.2f, -15, -2.3f, 4.4f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, -2, -0.5f, 0.14f, 0, 0));
        PartDefinition head = prothorax.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(91, 22).mirror(false).addBox(-4.4f, -4, -2.8f, 8.8f, 4, 4.6f)
                .texOffs(52, 35).mirror(false).addBox(-2.9f, 0, -2.9f, 5.8f, 3, 3.6f)
                .texOffs(88, 35).mirror(false).addBox(-1.4f, 2.8f, -2.7f, 2.8f, 2.2f, 2.4f), PartPose.offsetAndRotation(0, -17.6f, -0.4f, -0.1f, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(37, 35).mirror(false).addBox(-2.4f, -2, -2, 3, 4, 4), PartPose.offsetAndRotation(-4.2f, -2.4f, -0.4f, 0, 0, 0.1f));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(37, 35).mirror(true).addBox(-0.6f, -2, -2, 3, 4, 4), PartPose.offsetAndRotation(4.2f, -2.4f, -0.4f, 0, 0, -0.1f));
        PartDefinition rightAntenna = head.addOrReplaceChild("right_antenna", CubeListBuilder.create()
                .texOffs(114, 0).mirror(false).addBox(-0.25f, -12, -0.25f, 0.5f, 12, 0.5f), PartPose.offsetAndRotation(-0.8f, -3.2f, -2, 0.85f, 0.25f, -0.2f));
        PartDefinition leftAntenna = head.addOrReplaceChild("left_antenna", CubeListBuilder.create()
                .texOffs(114, 0).mirror(true).addBox(-0.25f, -12, -0.25f, 0.5f, 12, 0.5f), PartPose.offsetAndRotation(0.8f, -3.2f, -2, 0.85f, -0.25f, 0.2f));
        PartDefinition rightCoxa = prothorax.addOrReplaceChild("right_coxa", CubeListBuilder.create()
                .texOffs(120, 22).mirror(false).addBox(-1, 0, -1, 2, 7, 2)
                .texOffs(101, 35).mirror(false).addBox(-1.4f, 0.5f, -1.4f, 2.8f, 1.6f, 2.8f), PartPose.offsetAndRotation(-1.9f, -14.5f, -1.4f, -0.2f, 0, 0.1f));
        PartDefinition leftCoxa = prothorax.addOrReplaceChild("left_coxa", CubeListBuilder.create()
                .texOffs(120, 22).mirror(true).addBox(-1, 0, -1, 2, 7, 2)
                .texOffs(101, 35).mirror(true).addBox(-1.4f, 0.5f, -1.4f, 2.8f, 1.6f, 2.8f), PartPose.offsetAndRotation(1.9f, -14.5f, -1.4f, -0.2f, 0, -0.1f));
        PartDefinition rightFemur = rightCoxa.addOrReplaceChild("right_femur", CubeListBuilder.create()
                .texOffs(101, 0).mirror(false).addBox(-1.4f, -0.2f, -1.4f, 2.8f, 11, 2.8f)
                .texOffs(61, 22).mirror(false).addBox(-0.5f, 1, -2.2f, 1, 9, 0.9f), PartPose.offsetAndRotation(0, 7, 0, -2.55f, 0, 0));
        PartDefinition leftFemur = leftCoxa.addOrReplaceChild("left_femur", CubeListBuilder.create()
                .texOffs(101, 0).mirror(true).addBox(-1.4f, -0.2f, -1.4f, 2.8f, 11, 2.8f)
                .texOffs(61, 22).mirror(true).addBox(-0.5f, 1, -2.2f, 1, 9, 0.9f), PartPose.offsetAndRotation(0, 7, 0, -2.55f, 0, 0));
        PartDefinition rightTibia = rightFemur.addOrReplaceChild("right_tibia", CubeListBuilder.create()
                .texOffs(52, 22).mirror(false).addBox(-0.9f, 0, -0.9f, 1.8f, 9, 1.8f)
                .texOffs(0, 35).mirror(false).addBox(-0.4f, 0.8f, -1.6f, 0.8f, 7.5f, 0.8f)
                .texOffs(114, 35).mirror(false).addBox(-0.55f, 8.6f, -1.9f, 1.1f, 1.8f, 1.9f), PartPose.offsetAndRotation(0, 10.8f, 0, 2.7f, 0, 0));
        PartDefinition leftTibia = leftFemur.addOrReplaceChild("left_tibia", CubeListBuilder.create()
                .texOffs(52, 22).mirror(true).addBox(-0.9f, 0, -0.9f, 1.8f, 9, 1.8f)
                .texOffs(0, 35).mirror(true).addBox(-0.4f, 0.8f, -1.6f, 0.8f, 7.5f, 0.8f)
                .texOffs(114, 35).mirror(true).addBox(-0.55f, 8.6f, -1.9f, 1.1f, 1.8f, 1.9f), PartPose.offsetAndRotation(0, 10.8f, 0, 2.7f, 0, 0));
        PartDefinition rightHip0 = waist.addOrReplaceChild("right_hip_0", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.4f, 1, -1.2f, 0, -0.55f, 0));
        PartDefinition leftHip0 = waist.addOrReplaceChild("left_hip_0", CubeListBuilder.create(), PartPose.offsetAndRotation(2.4f, 1, -1.2f, 0, 0.55f, 0));
        PartDefinition rightLeg0 = rightHip0.addOrReplaceChild("right_leg_0", CubeListBuilder.create()
                .texOffs(0, 45).mirror(false).addBox(-8, -0.7f, -0.7f, 8, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.5f));
        PartDefinition leftLeg0 = leftHip0.addOrReplaceChild("left_leg_0", CubeListBuilder.create()
                .texOffs(0, 45).mirror(true).addBox(0, -0.7f, -0.7f, 8, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.5f));
        PartDefinition rightShin0 = rightLeg0.addOrReplaceChild("right_shin_0", CubeListBuilder.create()
                .texOffs(21, 45).mirror(false).addBox(-21, -0.55f, -0.55f, 21, 1.1f, 1.1f)
                .texOffs(68, 45).mirror(false).addBox(-1.2f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-8, 0, 0, 0, 0, -1.75f));
        PartDefinition leftShin0 = leftLeg0.addOrReplaceChild("left_shin_0", CubeListBuilder.create()
                .texOffs(21, 45).mirror(true).addBox(0, -0.55f, -0.55f, 21, 1.1f, 1.1f)
                .texOffs(68, 45).mirror(true).addBox(-0.2f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(8, 0, 0, 0, 0, 1.75f));
        PartDefinition rightHip1 = waist.addOrReplaceChild("right_hip_1", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.4f, 1, 1.4f, 0, 0.6f, 0));
        PartDefinition leftHip1 = waist.addOrReplaceChild("left_hip_1", CubeListBuilder.create(), PartPose.offsetAndRotation(2.4f, 1, 1.4f, 0, -0.6f, 0));
        PartDefinition rightLeg1 = rightHip1.addOrReplaceChild("right_leg_1", CubeListBuilder.create()
                .texOffs(0, 45).mirror(false).addBox(-8, -0.7f, -0.7f, 8, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.5f));
        PartDefinition leftLeg1 = leftHip1.addOrReplaceChild("left_leg_1", CubeListBuilder.create()
                .texOffs(0, 45).mirror(true).addBox(0, -0.7f, -0.7f, 8, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.5f));
        PartDefinition rightShin1 = rightLeg1.addOrReplaceChild("right_shin_1", CubeListBuilder.create()
                .texOffs(21, 45).mirror(false).addBox(-21, -0.55f, -0.55f, 21, 1.1f, 1.1f)
                .texOffs(68, 45).mirror(false).addBox(-1.2f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-8, 0, 0, 0, 0, -1.75f));
        PartDefinition leftShin1 = leftLeg1.addOrReplaceChild("left_shin_1", CubeListBuilder.create()
                .texOffs(21, 45).mirror(true).addBox(0, -0.55f, -0.55f, 21, 1.1f, 1.1f)
                .texOffs(68, 45).mirror(true).addBox(-0.2f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(8, 0, 0, 0, 0, 1.75f));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED mantis: end
    }

    /**
     * The Mantis Son: its four legs stride, its tall body sways like a stalk, its head turns and
     * tilts as a mantis's does, its feelers wave; its scythes folded before it, flicking out on a
     * blow; raised, both lifted high, open, over its head before the sweep.
     */
    public static final class MantisModel extends Whole<MantisSonEntity> {
        public static final float SIZE = 1.0f;
        private final ModelPart waist, prothorax, head, abdomen, sashTail, rightFemur, leftFemur, rightTibia, leftTibia, rightCoxa, leftCoxa,
                rightAntenna, leftAntenna;
        private final ModelPart[] rightLeg = new ModelPart[2], leftLeg = new ModelPart[2], rightHip = new ModelPart[2], leftHip = new ModelPart[2];

        public MantisModel(ModelPart root) {
            super(root);
            waist = p("waist");
            prothorax = p("prothorax");
            head = p("head");
            abdomen = p("abdomen");
            sashTail = p("sash_tail");
            rightCoxa = p("right_coxa");
            leftCoxa = p("left_coxa");
            rightFemur = p("right_femur");
            leftFemur = p("left_femur");
            rightTibia = p("right_tibia");
            leftTibia = p("left_tibia");
            rightAntenna = p("right_antenna");
            leftAntenna = p("left_antenna");
            for (int i = 0; i < 2; i++) {
                rightLeg[i] = p("right_leg_" + i);
                leftLeg[i] = p("left_leg_" + i);
                rightHip[i] = p("right_hip_" + i);
                leftHip[i] = p("left_hip_" + i);
            }
        }

        @Override
        public void setupAnim(MantisSonEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            boolean raised = e.isRaised();
            float w = swing * 0.9f, sway = Mth.sin(age * 0.06f);
            waist.y = waist.getInitialPose().y + Mth.abs(Mth.sin(w)) * 0.5f * amount;
            turn(prothorax, sway * 0.04f + (raised ? -0.18f : 0) + strike * 0.2f, 0, sway * 0.03f);
            turn(head, pitch * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, Mth.sin(age * 0.05f) * 0.15f);
            turn(abdomen, Mth.sin(age * 0.08f) * 0.04f, Mth.sin(w * 0.5f) * 0.05f * amount, 0);
            turn(sashTail, amount * 0.3f + Mth.sin(age * 0.09f) * 0.05f, 0, 0);
            turnPair(rightAntenna, leftAntenna, Mth.sin(age * 0.11f) * 0.12f, 0, Mth.cos(age * 0.13f) * 0.08f);
            // the scythes: folded; raised high and open; a blow flicks them out
            float up = raised ? 1.0f : 0, open = raised ? 1.6f : strike * 1.4f;
            turnPair(rightCoxa, leftCoxa, -up * 1.5f - strike * 0.9f, 0, up * 0.25f);
            turnPair(rightFemur, leftFemur, up * 0.3f + strike * 0.6f, 0, 0);
            turnPair(rightTibia, leftTibia, -open, 0, 0);
            for (int i = 0; i < 2; i++) {
                float ph = i == 0 ? 0 : Mth.PI;
                float r = Mth.cos(w + ph), l = Mth.cos(w + ph + Mth.PI);
                turn(rightHip[i], 0, r * 0.35f * amount, 0);
                turn(leftHip[i], 0, -l * 0.35f * amount, 0);
                turn(rightLeg[i], 0, 0, Math.max(0, Mth.sin(w + ph)) * 0.3f * amount);
                turn(leftLeg[i], 0, 0, -Math.max(0, Mth.sin(w + ph + Mth.PI)) * 0.3f * amount);
            }
        }
    }

    // ── The Cockroach Son ───────────────────────────────────────────────

    public static LayerDefinition roachLayer() {
        // GENERATED roach: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-8.5f, -4.5f, -10, 17, 8, 21)
                .texOffs(0, 56).mirror(false).addBox(-7, 3, -8, 14, 1.6f, 18), PartPose.offsetAndRotation(0, 12.5f, 0, 0, 0, 0));
        PartDefinition rightElytron = body.addOrReplaceChild("right_elytron", CubeListBuilder.create()
                .texOffs(0, 30).mirror(false).addBox(-9.6f, -1.8f, 0, 9.6f, 2.2f, 21.5f)
                .texOffs(65, 30).mirror(false).addBox(-7.4f, -3.2f, 1.2f, 7.4f, 1.6f, 19)
                .texOffs(65, 56).mirror(false).addBox(-4.2f, -4.2f, 2.6f, 4.2f, 1.2f, 15.5f), PartPose.offsetAndRotation(0, -4.6f, -8.5f, 0.02f, 0, 0.04f));
        PartDefinition leftElytron = body.addOrReplaceChild("left_elytron", CubeListBuilder.create()
                .texOffs(0, 30).mirror(true).addBox(0, -1.8f, 0, 9.6f, 2.2f, 21.5f)
                .texOffs(65, 30).mirror(true).addBox(0, -3.2f, 1.2f, 7.4f, 1.6f, 19)
                .texOffs(65, 56).mirror(true).addBox(0, -4.2f, 2.6f, 4.2f, 1.2f, 15.5f), PartPose.offsetAndRotation(0, -4.6f, -8.5f, 0.02f, 0, -0.04f));
        PartDefinition pronotum = body.addOrReplaceChild("pronotum", CubeListBuilder.create()
                .texOffs(27, 77).mirror(false).addBox(-9, -1.6f, -7, 18, 3, 9)
                .texOffs(0, 91).mirror(false).addBox(-6.5f, -2.8f, -6, 13, 1.4f, 7.4f)
                .texOffs(59, 102).mirror(false).addBox(-8.5f, -2.6f, -6.5f, 1, 1, 1)
                .texOffs(49, 102).mirror(false).addBox(-4.5f, -2.8f, -6.8f, 1, 1.2f, 1)
                .texOffs(49, 102).mirror(false).addBox(3.5f, -2.8f, -6.8f, 1, 1.2f, 1)
                .texOffs(59, 102).mirror(false).addBox(7.5f, -2.6f, -6.5f, 1, 1, 1)
                .texOffs(96, 91).mirror(false).addBox(-1.5f, -2.2f, -2, 3, 0.8f, 3), PartPose.offsetAndRotation(0, -4.3f, -9.5f, 0.25f, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(82, 77).mirror(false).addBox(-4.8f, -3, -5, 9.6f, 6, 5.2f)
                .texOffs(109, 91).mirror(false).addBox(-2.2f, 2.2f, -4.8f, 4.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, -11, 0.35f, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(43, 91).mirror(false).addBox(-1.3f, -1.6f, -1.5f, 2.2f, 3.2f, 3), PartPose.offsetAndRotation(-3.4f, -1.2f, -2.6f, 0, 0.2f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(43, 91).mirror(true).addBox(-0.9f, -1.6f, -1.5f, 2.2f, 3.2f, 3), PartPose.offsetAndRotation(3.4f, -1.2f, -2.6f, 0, -0.2f, 0));
        PartDefinition rightMandible = head.addOrReplaceChild("right_mandible", CubeListBuilder.create()
                .texOffs(0, 102).mirror(false).addBox(-1.2f, -0.6f, -1.8f, 2, 1.4f, 2), PartPose.offsetAndRotation(-1.6f, 2.2f, -4.2f, 0, 0.2f, 0.1f));
        PartDefinition leftMandible = head.addOrReplaceChild("left_mandible", CubeListBuilder.create()
                .texOffs(0, 102).mirror(true).addBox(-0.8f, -0.6f, -1.8f, 2, 1.4f, 2), PartPose.offsetAndRotation(1.6f, 2.2f, -4.2f, 0, -0.2f, -0.1f));
        PartDefinition rightAntenna = head.addOrReplaceChild("right_antenna", CubeListBuilder.create()
                .texOffs(0, 77).mirror(false).addBox(-0.3f, -0.3f, -12, 0.6f, 0.6f, 12), PartPose.offsetAndRotation(-2, -2, -4, 0.15f, 0.55f, 0));
        PartDefinition leftAntenna = head.addOrReplaceChild("left_antenna", CubeListBuilder.create()
                .texOffs(0, 77).mirror(true).addBox(-0.3f, -0.3f, -12, 0.6f, 0.6f, 12), PartPose.offsetAndRotation(2, -2, -4, 0.15f, -0.55f, 0));
        PartDefinition rightAntennaTip = rightAntenna.addOrReplaceChild("right_antenna_tip", CubeListBuilder.create()
                .texOffs(0, 77).mirror(false).addBox(-0.25f, -0.25f, -12, 0.5f, 0.5f, 12), PartPose.offsetAndRotation(0, 0, -12, -0.15f, 0.25f, 0));
        PartDefinition leftAntennaTip = leftAntenna.addOrReplaceChild("left_antenna_tip", CubeListBuilder.create()
                .texOffs(0, 77).mirror(true).addBox(-0.25f, -0.25f, -12, 0.5f, 0.5f, 12), PartPose.offsetAndRotation(0, 0, -12, -0.15f, -0.25f, 0));
        PartDefinition rightCercus = body.addOrReplaceChild("right_cercus", CubeListBuilder.create()
                .texOffs(56, 91).mirror(false).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 5), PartPose.offsetAndRotation(-3, 0, 11, 0.3f, -0.35f, 0));
        PartDefinition leftCercus = body.addOrReplaceChild("left_cercus", CubeListBuilder.create()
                .texOffs(56, 91).mirror(true).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 5), PartPose.offsetAndRotation(3, 0, 11, 0.3f, 0.35f, 0));
        PartDefinition rightHip0 = body.addOrReplaceChild("right_hip_0", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.5f, 2, -6, 0, -0.65f, 0));
        PartDefinition leftHip0 = body.addOrReplaceChild("left_hip_0", CubeListBuilder.create(), PartPose.offsetAndRotation(7.5f, 2, -6, 0, 0.65f, 0));
        PartDefinition rightLeg0 = rightHip0.addOrReplaceChild("right_leg_0", CubeListBuilder.create()
                .texOffs(71, 91).mirror(false).addBox(-9, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(54, 102).mirror(false).addBox(-6, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(54, 102).mirror(false).addBox(-3, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.32f));
        PartDefinition leftLeg0 = leftHip0.addOrReplaceChild("left_leg_0", CubeListBuilder.create()
                .texOffs(71, 91).mirror(true).addBox(0, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(54, 102).mirror(true).addBox(5.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(54, 102).mirror(true).addBox(2.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.32f));
        PartDefinition rightShin0 = rightLeg0.addOrReplaceChild("right_shin_0", CubeListBuilder.create()
                .texOffs(9, 102).mirror(false).addBox(-11, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(54, 102).mirror(false).addBox(-7, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(54, 102).mirror(false).addBox(-3.5f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(36, 102).mirror(false).addBox(-14, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(-9, 0, 0, 0, 0, -1.55f));
        PartDefinition leftShin0 = leftLeg0.addOrReplaceChild("left_shin_0", CubeListBuilder.create()
                .texOffs(9, 102).mirror(true).addBox(0, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(54, 102).mirror(true).addBox(6.3f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(54, 102).mirror(true).addBox(2.8f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(36, 102).mirror(true).addBox(10.6f, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(9, 0, 0, 0, 0, 1.55f));
        PartDefinition rightHip1 = body.addOrReplaceChild("right_hip_1", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.5f, 2, 1, 0, 0.08f, 0));
        PartDefinition leftHip1 = body.addOrReplaceChild("left_hip_1", CubeListBuilder.create(), PartPose.offsetAndRotation(7.5f, 2, 1, 0, -0.08f, 0));
        PartDefinition rightLeg1 = rightHip1.addOrReplaceChild("right_leg_1", CubeListBuilder.create()
                .texOffs(71, 91).mirror(false).addBox(-9, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(54, 102).mirror(false).addBox(-6, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(54, 102).mirror(false).addBox(-3, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.32f));
        PartDefinition leftLeg1 = leftHip1.addOrReplaceChild("left_leg_1", CubeListBuilder.create()
                .texOffs(71, 91).mirror(true).addBox(0, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(54, 102).mirror(true).addBox(5.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(54, 102).mirror(true).addBox(2.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.32f));
        PartDefinition rightShin1 = rightLeg1.addOrReplaceChild("right_shin_1", CubeListBuilder.create()
                .texOffs(9, 102).mirror(false).addBox(-11, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(54, 102).mirror(false).addBox(-7, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(54, 102).mirror(false).addBox(-3.5f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(36, 102).mirror(false).addBox(-14, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(-9, 0, 0, 0, 0, -1.55f));
        PartDefinition leftShin1 = leftLeg1.addOrReplaceChild("left_shin_1", CubeListBuilder.create()
                .texOffs(9, 102).mirror(true).addBox(0, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(54, 102).mirror(true).addBox(6.3f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(54, 102).mirror(true).addBox(2.8f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(36, 102).mirror(true).addBox(10.6f, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(9, 0, 0, 0, 0, 1.55f));
        PartDefinition rightHip2 = body.addOrReplaceChild("right_hip_2", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.5f, 2, 8, 0, 0.8f, 0));
        PartDefinition leftHip2 = body.addOrReplaceChild("left_hip_2", CubeListBuilder.create(), PartPose.offsetAndRotation(7.5f, 2, 8, 0, -0.8f, 0));
        PartDefinition rightLeg2 = rightHip2.addOrReplaceChild("right_leg_2", CubeListBuilder.create()
                .texOffs(71, 91).mirror(false).addBox(-9, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(54, 102).mirror(false).addBox(-6, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(54, 102).mirror(false).addBox(-3, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.32f));
        PartDefinition leftLeg2 = leftHip2.addOrReplaceChild("left_leg_2", CubeListBuilder.create()
                .texOffs(71, 91).mirror(true).addBox(0, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(54, 102).mirror(true).addBox(5.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(54, 102).mirror(true).addBox(2.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.32f));
        PartDefinition rightShin2 = rightLeg2.addOrReplaceChild("right_shin_2", CubeListBuilder.create()
                .texOffs(9, 102).mirror(false).addBox(-11, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(54, 102).mirror(false).addBox(-7, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(54, 102).mirror(false).addBox(-3.5f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(36, 102).mirror(false).addBox(-14, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(-9, 0, 0, 0, 0, -1.55f));
        PartDefinition leftShin2 = leftLeg2.addOrReplaceChild("left_shin_2", CubeListBuilder.create()
                .texOffs(9, 102).mirror(true).addBox(0, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(54, 102).mirror(true).addBox(6.3f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(54, 102).mirror(true).addBox(2.8f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(36, 102).mirror(true).addBox(10.6f, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(9, 0, 0, 0, 0, 1.55f));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED roach: end
    }

    /**
     * The Cockroach Son: six legs in two tripods, scuttling; its feelers sweeping, each a two-jointed
     * whip; its mandibles working. Scraping before its charge, it lowers its head and paws the ground
     * with a foreleg; charging, it presses low, feelers laid back.
     */
    public static final class RoachModel extends Whole<CockroachSonEntity> {
        public static final float SIZE = 1.0f;
        private final ModelPart body, head, rightAntenna, leftAntenna, rightTip, leftTip, rightMandible, leftMandible, rightElytron, leftElytron;
        private final ModelPart[] rightHip = new ModelPart[3], leftHip = new ModelPart[3], rightLeg = new ModelPart[3], leftLeg = new ModelPart[3];

        public RoachModel(ModelPart root) {
            super(root);
            body = p("body");
            head = p("head");
            rightAntenna = p("right_antenna");
            leftAntenna = p("left_antenna");
            rightTip = p("right_antenna_tip");
            leftTip = p("left_antenna_tip");
            rightMandible = p("right_mandible");
            leftMandible = p("left_mandible");
            rightElytron = p("right_elytron");
            leftElytron = p("left_elytron");
            for (int i = 0; i < 3; i++) {
                rightHip[i] = p("right_hip_" + i);
                leftHip[i] = p("left_hip_" + i);
                rightLeg[i] = p("right_leg_" + i);
                leftLeg[i] = p("left_leg_" + i);
            }
        }

        @Override
        public void setupAnim(CockroachSonEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            int st = e.chargeState();
            boolean scrape = st == CockroachSonEntity.SCRAPING, charge = st == CockroachSonEntity.CHARGING;
            float w = swing * 1.4f;
            body.y = body.getInitialPose().y + (charge ? 1.5f : 0) + Mth.abs(Mth.sin(w * 2)) * 0.3f * amount;
            turn(body, charge ? 0.08f : scrape ? 0.12f : 0, Mth.sin(w) * 0.03f * amount, 0);
            turn(head, (scrape ? 0.35f : 0) + strike * 0.3f + pitch * Mth.DEG_TO_RAD * 0.3f, yaw * Mth.DEG_TO_RAD * 0.4f, 0);
            float lay = charge ? -0.5f : 0;
            turnPair(rightAntenna, leftAntenna, lay + Mth.sin(age * 0.17f) * 0.12f, Mth.sin(age * 0.11f) * 0.25f, 0);
            turnPair(rightTip, leftTip, Mth.sin(age * 0.23f + 1) * 0.2f, Mth.cos(age * 0.19f) * 0.3f, 0);
            turnPair(rightMandible, leftMandible, 0, Mth.sin(age * 0.5f) * 0.15f + strike * 0.4f, 0);
            float flare = strike * 0.15f;
            turnPair(rightElytron, leftElytron, 0, 0, flare);
            for (int i = 0; i < 3; i++) {
                // tripods: right 0 and 2 with left 1
                boolean setA = i % 2 == 0;
                float r = Mth.cos(w + (setA ? 0 : Mth.PI)), l = Mth.cos(w + (setA ? Mth.PI : 0));
                turn(rightHip[i], 0, r * 0.35f * amount, 0);
                turn(leftHip[i], 0, -l * 0.35f * amount, 0);
                float paw = scrape && i == 0 ? Math.max(0, Mth.sin(age * 0.8f)) * 0.6f : 0;
                turn(rightLeg[i], 0, 0, Math.max(0, Mth.sin(w + (setA ? 0 : Mth.PI))) * 0.3f * amount + paw);
                turn(leftLeg[i], 0, 0, -Math.max(0, Mth.sin(w + (setA ? Mth.PI : 0))) * 0.3f * amount);
            }
        }
    }

    // ── The Seven Spider Spirits ────────────────────────────────────────

    public static LayerDefinition sisterLayer() {
        // GENERATED sister: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(46, 0).mirror(false).addBox(-3, -11, -1.9f, 6, 11, 3.8f)
                .texOffs(175, 33).mirror(false).addBox(-2.7f, -9.4f, -2.3f, 5.4f, 2.8f, 0.6f)
                .texOffs(26, 33).mirror(false).addBox(-3.6f, -11.2f, -2.1f, 7.2f, 1.8f, 4.2f)
                .texOffs(53, 33).mirror(false).addBox(-3.3f, -11.7f, -2.25f, 6.6f, 1.8f, 4.5f)
                .texOffs(205, 19).mirror(false).addBox(-3.4f, -3.6f, -2.35f, 6.8f, 2.2f, 4.7f)
                .texOffs(190, 33).mirror(false).addBox(-1.1f, -3.8f, -3.05f, 2.2f, 2.6f, 0.9f), PartPose.offsetAndRotation(0, 11.5f, 0, 0, 0, 0));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(169, 0).mirror(false).addBox(-3.8f, -1.4f, -2.6f, 7.6f, 6.4f, 5.2f)
                .texOffs(198, 0).mirror(false).addBox(-4.6f, 4.6f, -3.2f, 9.2f, 6, 6.4f)
                .texOffs(54, 19).mirror(false).addBox(-5.7f, 9.8f, -4.1f, 11.4f, 2.8f, 8.2f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition apron = skirt.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(98, 0).mirror(false).addBox(-2.2f, 0, -0.4f, 4.4f, 12.5f, 0.4f), PartPose.offsetAndRotation(0, -1.2f, -3, -0.08f, 0, 0));
        PartDefinition train = skirt.addOrReplaceChild("train", CubeListBuilder.create()
                .texOffs(230, 19).mirror(false).addBox(-4.6f, 0, 0, 9.2f, 7, 0.6f), PartPose.offsetAndRotation(0, 6, 3.8f, 0.35f, 0, 0));
        PartDefinition rightSashEnd = body.addOrReplaceChild("right_sash_end", CubeListBuilder.create()
                .texOffs(147, 19).mirror(false).addBox(-0.6f, 0, -0.2f, 1.2f, 9, 0.4f), PartPose.offsetAndRotation(-1.6f, -2.4f, -2.9f, -0.06f, 0, 0.06f));
        PartDefinition leftSashEnd = body.addOrReplaceChild("left_sash_end", CubeListBuilder.create()
                .texOffs(147, 19).mirror(true).addBox(-0.6f, 0, -0.2f, 1.2f, 9, 0.4f), PartPose.offsetAndRotation(1.6f, -2.4f, -2.9f, -0.06f, 0, -0.06f));
        PartDefinition gauze = body.addOrReplaceChild("gauze", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-3.5f, 0, -2.45f, 7, 13, 4.9f), PartPose.offsetAndRotation(0, -10.8f, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(199, 33).mirror(false).addBox(-0.9f, -1.6f, -0.9f, 1.8f, 1.8f, 1.8f)
                .texOffs(154, 19).mirror(false).addBox(-2.2f, -6.2f, -2.3f, 4.4f, 5, 4.4f)
                .texOffs(188, 19).mirror(false).addBox(-2.5f, -6.6f, -0.4f, 5, 5.8f, 2.9f)
                .texOffs(78, 33).mirror(false).addBox(-2.5f, -6.8f, -2.5f, 5, 1.3f, 4.8f)
                .texOffs(242, 33).mirror(false).addBox(-2.35f, -6.35f, -2.55f, 4.7f, 1, 0.4f), PartPose.offsetAndRotation(0, -11.5f, -0.1f, 0, 0, 0));
        PartDefinition rightLock = head.addOrReplaceChild("right_lock", CubeListBuilder.create()
                .texOffs(99, 33).mirror(false).addBox(-0.45f, 0, -0.45f, 0.8f, 5, 1.1f), PartPose.offsetAndRotation(-2.3f, -5.8f, -1.5f, 0, 0, 0.06f));
        PartDefinition leftLock = head.addOrReplaceChild("left_lock", CubeListBuilder.create()
                .texOffs(99, 33).mirror(true).addBox(-0.35f, 0, -0.45f, 0.8f, 5, 1.1f), PartPose.offsetAndRotation(2.3f, -5.8f, -1.5f, 0, 0, -0.06f));
        PartDefinition bun = head.addOrReplaceChild("bun", CubeListBuilder.create()
                .texOffs(0, 33).mirror(false).addBox(-2.1f, -3.6f, -1.6f, 4.2f, 3.8f, 3.4f)
                .texOffs(124, 33).mirror(false).addBox(-1.4f, -5.6f, -1.1f, 2.8f, 2.2f, 2.4f), PartPose.offsetAndRotation(0, -6.7f, 0.6f, -0.15f, 0, 0));
        PartDefinition rightLoop = bun.addOrReplaceChild("right_loop", CubeListBuilder.create()
                .texOffs(106, 33).mirror(false).addBox(-0.9f, -4.2f, -0.9f, 1.8f, 4.2f, 1.8f), PartPose.offsetAndRotation(-1.8f, -3.4f, 0.1f, 0, 0, -0.55f));
        PartDefinition leftLoop = bun.addOrReplaceChild("left_loop", CubeListBuilder.create()
                .texOffs(106, 33).mirror(true).addBox(-0.9f, -4.2f, -0.9f, 1.8f, 4.2f, 1.8f), PartPose.offsetAndRotation(1.8f, -3.4f, 0.1f, 0, 0, 0.55f));
        PartDefinition rightPin = bun.addOrReplaceChild("right_pin", CubeListBuilder.create()
                .texOffs(0, 42).mirror(false).addBox(-3.5f, -0.25f, -0.25f, 3.5f, 0.5f, 0.5f)
                .texOffs(208, 33).mirror(false).addBox(-4.4f, -0.7f, -0.7f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(-1.6f, -2.8f, -1.2f, 0.3f, 0.5f, 0.4f));
        PartDefinition leftPin = bun.addOrReplaceChild("left_pin", CubeListBuilder.create()
                .texOffs(0, 42).mirror(true).addBox(0, -0.25f, -0.25f, 3.5f, 0.5f, 0.5f)
                .texOffs(208, 33).mirror(true).addBox(3, -0.7f, -0.7f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(1.6f, -2.8f, -1.2f, 0.3f, -0.5f, -0.4f));
        PartDefinition rightDangle = rightPin.addOrReplaceChild("right_dangle", CubeListBuilder.create()
                .texOffs(217, 33).mirror(false).addBox(-0.15f, 0, -0.15f, 0.3f, 2.6f, 0.3f)
                .texOffs(11, 42).mirror(false).addBox(-0.5f, 2.5f, -0.5f, 1, 1, 1), PartPose.offsetAndRotation(-3.9f, 0.4f, 0, 0, 0, 0));
        PartDefinition leftDangle = leftPin.addOrReplaceChild("left_dangle", CubeListBuilder.create()
                .texOffs(217, 33).mirror(true).addBox(-0.15f, 0, -0.15f, 0.3f, 2.6f, 0.3f)
                .texOffs(11, 42).mirror(true).addBox(-0.5f, 2.5f, -0.5f, 1, 1, 1), PartPose.offsetAndRotation(3.9f, 0.4f, 0, 0, 0, 0));
        PartDefinition comb = bun.addOrReplaceChild("comb", CubeListBuilder.create()
                .texOffs(231, 33).mirror(false).addBox(-1.6f, -1.2f, -0.3f, 3.2f, 1.4f, 0.5f), PartPose.offsetAndRotation(0, -1.8f, -1.9f, 0.2f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(175, 19).mirror(false).addBox(-1.4f, -0.7f, -1.5f, 2.4f, 6.2f, 3), PartPose.offsetAndRotation(-3.6f, -10.2f, 0, 0.12f, 0, 0.14f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(175, 19).mirror(true).addBox(-1, -0.7f, -1.5f, 2.4f, 6.2f, 3), PartPose.offsetAndRotation(3.6f, -10.2f, 0, 0.12f, 0, -0.14f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(115, 33).mirror(false).addBox(-1.05f, 0, -1.05f, 2, 4.4f, 2)
                .texOffs(137, 33).mirror(false).addBox(-0.85f, 4.2f, -0.9f, 1.7f, 2.2f, 1.8f), PartPose.offsetAndRotation(-0.3f, 5.4f, 0, -0.55f, -0.2f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(115, 33).mirror(true).addBox(-0.95f, 0, -1.05f, 2, 4.4f, 2)
                .texOffs(137, 33).mirror(true).addBox(-0.85f, 4.2f, -0.9f, 1.7f, 2.2f, 1.8f), PartPose.offsetAndRotation(0.3f, 5.4f, 0, -0.55f, 0.2f, 0));
        PartDefinition rightSleeve = rightForearm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(25, 0).mirror(false).addBox(-1.9f, -0.6f, -1.1f, 3.8f, 9.8f, 5.8f), PartPose.offsetAndRotation(0, 0.2f, 0.6f, 0.48f, 0.1f, -0.02f));
        PartDefinition leftSleeve = leftForearm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(25, 0).mirror(true).addBox(-1.9f, -0.6f, -1.1f, 3.8f, 9.8f, 5.8f), PartPose.offsetAndRotation(0, 0.2f, 0.6f, 0.48f, -0.1f, 0.02f));
        PartDefinition rightStole = rightForearm.addOrReplaceChild("right_stole", CubeListBuilder.create()
                .texOffs(233, 0).mirror(false).addBox(-0.2f, 0, -1.1f, 0.4f, 10, 2.2f), PartPose.offsetAndRotation(-0.9f, 2.6f, 0.8f, 0.4f, 0, 0.15f));
        PartDefinition leftStole = leftForearm.addOrReplaceChild("left_stole", CubeListBuilder.create()
                .texOffs(233, 0).mirror(true).addBox(-0.2f, 0, -1.1f, 0.4f, 10, 2.2f), PartPose.offsetAndRotation(0.9f, 2.6f, 0.8f, 0.4f, 0, -0.15f));
        PartDefinition rightStoleTail = rightStole.addOrReplaceChild("right_stole_tail", CubeListBuilder.create()
                .texOffs(19, 33).mirror(false).addBox(-0.2f, 0, -1, 0.4f, 5.5f, 2), PartPose.offsetAndRotation(0, 9.8f, 0, -0.45f, 0, 0.1f));
        PartDefinition leftStoleTail = leftStole.addOrReplaceChild("left_stole_tail", CubeListBuilder.create()
                .texOffs(19, 33).mirror(true).addBox(-0.2f, 0, -1, 0.4f, 5.5f, 2), PartPose.offsetAndRotation(0, 9.8f, 0, -0.45f, 0, -0.1f));
        PartDefinition stoleBack = body.addOrReplaceChild("stole_back", CubeListBuilder.create()
                .texOffs(146, 33).mirror(false).addBox(-6.4f, -0.2f, -0.2f, 12.8f, 3.4f, 0.4f), PartPose.offsetAndRotation(0, -10.4f, 2.6f, 0.18f, 0, 0));
        PartDefinition silkStream = body.addOrReplaceChild("silk_stream", CubeListBuilder.create()
                .texOffs(67, 0).mirror(false).addBox(-0.45f, -0.45f, -14, 0.9f, 0.9f, 14), PartPose.offsetAndRotation(0, -2.5f, -3.4f, 0, 0, 0));
        PartDefinition legsMount = body.addOrReplaceChild("legs_mount", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -9.2f, 1.8f, 0, 0, 0));
        PartDefinition rightSleg0 = legsMount.addOrReplaceChild("right_sleg_0", CubeListBuilder.create()
                .texOffs(111, 0).mirror(false).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 11.5f), PartPose.offsetAndRotation(-0.9f, -0.4f, 0, -1.25f, -0.62f, 0));
        PartDefinition leftSleg0 = legsMount.addOrReplaceChild("left_sleg_0", CubeListBuilder.create()
                .texOffs(111, 0).mirror(true).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 11.5f), PartPose.offsetAndRotation(0.9f, -0.4f, 0, -1.25f, 0.62f, 0));
        PartDefinition rightSlegTip0 = rightSleg0.addOrReplaceChild("right_sleg_tip_0", CubeListBuilder.create()
                .texOffs(140, 0).mirror(false).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 12.5f)
                .texOffs(222, 33).mirror(false).addBox(-0.7f, -0.7f, -0.4f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 11.2f, 1.45f, 0, 0));
        PartDefinition leftSlegTip0 = leftSleg0.addOrReplaceChild("left_sleg_tip_0", CubeListBuilder.create()
                .texOffs(140, 0).mirror(true).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 12.5f)
                .texOffs(222, 33).mirror(true).addBox(-0.7f, -0.7f, -0.4f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 11.2f, 1.45f, 0, 0));
        PartDefinition rightSleg1 = legsMount.addOrReplaceChild("right_sleg_1", CubeListBuilder.create()
                .texOffs(0, 19).mirror(false).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 10.9f), PartPose.offsetAndRotation(-0.9f, 1, 0, -0.93f, -0.88f, 0));
        PartDefinition leftSleg1 = legsMount.addOrReplaceChild("left_sleg_1", CubeListBuilder.create()
                .texOffs(0, 19).mirror(true).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 10.9f), PartPose.offsetAndRotation(0.9f, 1, 0, -0.93f, 0.88f, 0));
        PartDefinition rightSlegTip1 = rightSleg1.addOrReplaceChild("right_sleg_tip_1", CubeListBuilder.create()
                .texOffs(27, 19).mirror(false).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 11.9f)
                .texOffs(222, 33).mirror(false).addBox(-0.7f, -0.7f, -0.4f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 10.6f, 1.33f, 0, 0));
        PartDefinition leftSlegTip1 = leftSleg1.addOrReplaceChild("left_sleg_tip_1", CubeListBuilder.create()
                .texOffs(27, 19).mirror(true).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 11.9f)
                .texOffs(222, 33).mirror(true).addBox(-0.7f, -0.7f, -0.4f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 10.6f, 1.33f, 0, 0));
        PartDefinition rightSleg2 = legsMount.addOrReplaceChild("right_sleg_2", CubeListBuilder.create()
                .texOffs(0, 19).mirror(false).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 10.3f), PartPose.offsetAndRotation(-0.9f, 2.4f, 0, -0.61f, -1.14f, 0));
        PartDefinition leftSleg2 = legsMount.addOrReplaceChild("left_sleg_2", CubeListBuilder.create()
                .texOffs(0, 19).mirror(true).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 10.3f), PartPose.offsetAndRotation(0.9f, 2.4f, 0, -0.61f, 1.14f, 0));
        PartDefinition rightSlegTip2 = rightSleg2.addOrReplaceChild("right_sleg_tip_2", CubeListBuilder.create()
                .texOffs(27, 19).mirror(false).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 11.3f)
                .texOffs(222, 33).mirror(false).addBox(-0.7f, -0.7f, -0.4f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 10, 1.21f, 0, 0));
        PartDefinition leftSlegTip2 = leftSleg2.addOrReplaceChild("left_sleg_tip_2", CubeListBuilder.create()
                .texOffs(27, 19).mirror(true).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 11.3f)
                .texOffs(222, 33).mirror(true).addBox(-0.7f, -0.7f, -0.4f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 10, 1.21f, 0, 0));
        PartDefinition rightSleg3 = legsMount.addOrReplaceChild("right_sleg_3", CubeListBuilder.create()
                .texOffs(97, 19).mirror(false).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 9.7f), PartPose.offsetAndRotation(-0.9f, 3.8f, 0, -0.29f, -1.4f, 0));
        PartDefinition leftSleg3 = legsMount.addOrReplaceChild("left_sleg_3", CubeListBuilder.create()
                .texOffs(97, 19).mirror(true).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 9.7f), PartPose.offsetAndRotation(0.9f, 3.8f, 0, -0.29f, 1.4f, 0));
        PartDefinition rightSlegTip3 = rightSleg3.addOrReplaceChild("right_sleg_tip_3", CubeListBuilder.create()
                .texOffs(122, 19).mirror(false).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 10.7f)
                .texOffs(222, 33).mirror(false).addBox(-0.7f, -0.7f, -0.4f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 9.4f, 1.09f, 0, 0));
        PartDefinition leftSlegTip3 = leftSleg3.addOrReplaceChild("left_sleg_tip_3", CubeListBuilder.create()
                .texOffs(122, 19).mirror(true).addBox(-0.45f, -0.45f, 0, 0.9f, 0.9f, 10.7f)
                .texOffs(222, 33).mirror(true).addBox(-0.7f, -0.7f, -0.4f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 9.4f, 1.09f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED sister: end
    }

    /**
     * A Spider Spirit: she glides, her skirt and train swaying, her stole and sash-ends drifting; her
     * hands folded in her great sleeves; her head turns; the gems of her diadem and the jade at her
     * waist glimmer. Her spider's legs stir behind her, folded like a fan; spinning, they spread wide
     * and her arms open, the silk pouring from her waist; weaving, her hands rise and her legs reach
     * out and down.
     */
    public static final class SisterModel extends Whole<SpiderSpiritEntity> {
        public static final float SIZE = 0.98f;
        private final ModelPart body, skirt, train, apron, head, bun, rightArm, leftArm, rightForearm, leftForearm, rightSleeve, leftSleeve,
                rightStole, leftStole, rightTail, leftTail, silk, gauze;
        private final ModelPart[] rightLeg = new ModelPart[4], leftLeg = new ModelPart[4], rightTip = new ModelPart[4], leftTip = new ModelPart[4];

        public SisterModel(ModelPart root) {
            super(root, RenderType::entityTranslucent);
            body = p("body");
            skirt = p("skirt");
            train = p("train");
            apron = p("apron");
            head = p("head");
            bun = p("bun");
            gauze = p("gauze");
            rightArm = p("right_arm");
            leftArm = p("left_arm");
            rightForearm = p("right_forearm");
            leftForearm = p("left_forearm");
            rightSleeve = p("right_sleeve");
            leftSleeve = p("left_sleeve");
            rightStole = p("right_stole");
            leftStole = p("left_stole");
            rightTail = p("right_stole_tail");
            leftTail = p("left_stole_tail");
            silk = p("silk_stream");
            for (int i = 0; i < 4; i++) {
                rightLeg[i] = p("right_sleg_" + i);
                leftLeg[i] = p("left_sleg_" + i);
                rightTip[i] = p("right_sleg_tip_" + i);
                leftTip[i] = p("left_sleg_tip_" + i);
            }
        }

        @Override
        public void setupAnim(SpiderSpiritEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            boolean spin = e.isSpinning(), weave = e.isWeaving();
            float breath = Mth.sin(age * 0.06f), w = swing * 0.6f;
            body.y = body.getInitialPose().y + breath * 0.2f;
            turn(body, amount * 0.05f, Mth.sin(w) * 0.05f * amount, 0);
            turn(head, pitch * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, Mth.sin(age * 0.04f) * 0.04f);
            turn(skirt, -amount * 0.05f, 0, Mth.sin(w) * 0.03f * amount);
            turn(train, amount * 0.3f + Mth.sin(age * 0.07f) * 0.04f, 0, 0);
            turn(apron, -amount * 0.15f + Mth.sin(age * 0.05f) * 0.02f, 0, 0);
            turn(gauze, amount * 0.04f, 0, 0);
            turn(bun, 0, 0, Mth.sin(age * 0.04f) * 0.02f);
            // her arms: hands folded before her; a lash of her sleeve; spread wide as she spins; raised as she weaves
            float ax = 0, az = 0, fx = 0;
            if (spin) {
                ax = -0.4f;
                az = 0.9f;
                fx = 0.3f;
            } else if (weave) {
                ax = -1.8f;
                az = 0.35f;
                fx = 0.4f;
            }
            ax -= strike * 1.2f;
            turnPair(rightArm, leftArm, ax, 0, -az);
            turn(leftArm, (spin || weave) ? ax : Mth.sin(age * 0.05f) * 0.03f, 0, (spin || weave) ? az : 0);
            turnPair(rightForearm, leftForearm, fx + strike * 0.4f, 0, 0);
            float hang = -(ax + fx) * 0.7f;
            turnPair(rightSleeve, leftSleeve, hang + Mth.sin(age * 0.05f) * 0.04f + amount * 0.2f, 0, 0);
            turnPair(rightStole, leftStole, amount * 0.4f + Mth.sin(age * 0.07f) * 0.08f + hang * 0.5f, 0, Mth.sin(age * 0.05f) * 0.06f);
            turnPair(rightTail, leftTail, amount * 0.3f + Mth.sin(age * 0.09f + 1) * 0.12f, 0, 0);
            // the silk pouring from her waist as she spins: it swells as it gathers
            silk.visible = spin;
            silk.zScale = 0.6f + 0.4f * Mth.abs(Mth.sin(age * 0.5f));
            // her spider's legs: folded like a fan, stirring; spread wide spinning; reaching out and down weaving
            for (int i = 0; i < 4; i++) {
                float stir = Mth.sin(age * 0.08f + i * 0.9f) * 0.04f;
                float spread = spin ? -0.55f - i * 0.1f : weave ? -0.35f : 0;
                float lift = spin ? 0.2f : weave ? 0.45f : 0;
                turnPair(rightLeg[i], leftLeg[i], lift + stir, spread, 0);
                turnPair(rightTip[i], leftTip[i], (weave ? -0.5f : spin ? -0.35f : 0) + stir * 2, 0, 0);
            }
        }
    }

    // ── The Hundred-Eyed Demon Lord ─────────────────────────────────────

    public static LayerDefinition lordLayer() {
        // GENERATED lord: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition seg0 = root.addOrReplaceChild("seg_0", CubeListBuilder.create()
                .texOffs(46, 0).mirror(false).addBox(-6.5f, -4.5f, -4.5f, 13, 9, 9)
                .texOffs(195, 59).mirror(false).addBox(-5.5f, -5.2f, -4, 11, 1, 8), PartPose.offsetAndRotation(0, 18, 0, 0, 0, 0));
        PartDefinition waist = seg0.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(78, 59).mirror(false).addBox(-5.4f, -3, -3.4f, 10.8f, 3.4f, 6.8f), PartPose.offsetAndRotation(0, -4.5f, -1.5f, 0, 0, 0));
        PartDefinition torso = waist.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(137, 26).mirror(false).addBox(-5.2f, -8, -3.2f, 10.4f, 8, 6.4f)
                .texOffs(140, 0).mirror(false).addBox(-6.8f, -16, -3.8f, 13.6f, 8.2f, 7.6f)
                .texOffs(83, 72).mirror(false).addBox(-3.4f, -17.2f, -2.2f, 6.8f, 1.6f, 4.4f)
                .texOffs(0, 44).mirror(false).addBox(-1.2f, -14.6f, -4.3f, 2.4f, 13, 0.6f)
                .texOffs(241, 82).mirror(false).addBox(-7.1f, -15, -2.6f, 0.7f, 1.5f, 1.8f)
                .texOffs(241, 82).mirror(false).addBox(-7.1f, -12.7f, -0.2f, 0.7f, 1.5f, 1.8f)
                .texOffs(241, 82).mirror(false).addBox(-7.1f, -10.4f, -2.6f, 0.7f, 1.5f, 1.8f)
                .texOffs(241, 82).mirror(false).addBox(-7.1f, -8.1f, -0.2f, 0.7f, 1.5f, 1.8f)
                .texOffs(241, 82).mirror(false).addBox(6.4f, -15, -2.6f, 0.7f, 1.5f, 1.8f)
                .texOffs(241, 82).mirror(false).addBox(6.4f, -12.7f, -0.2f, 0.7f, 1.5f, 1.8f)
                .texOffs(241, 82).mirror(false).addBox(6.4f, -10.4f, -2.6f, 0.7f, 1.5f, 1.8f)
                .texOffs(241, 82).mirror(false).addBox(6.4f, -8.1f, -0.2f, 0.7f, 1.5f, 1.8f)
                .texOffs(241, 82).mirror(false).addBox(-5.6f, -7, -2.2f, 0.7f, 1.4f, 1.6f)
                .texOffs(241, 82).mirror(false).addBox(-5.6f, -4.7f, 0, 0.7f, 1.4f, 1.6f)
                .texOffs(241, 82).mirror(false).addBox(-5.6f, -2.4f, -2.2f, 0.7f, 1.4f, 1.6f)
                .texOffs(241, 82).mirror(false).addBox(4.9f, -7, -2.2f, 0.7f, 1.4f, 1.6f)
                .texOffs(241, 82).mirror(false).addBox(4.9f, -4.7f, 0, 0.7f, 1.4f, 1.6f)
                .texOffs(241, 82).mirror(false).addBox(4.9f, -2.4f, -2.2f, 0.7f, 1.4f, 1.6f)
                .texOffs(77, 90).mirror(false).addBox(-5.6f, -14.2f, -4.25f, 1.8f, 1.3f, 0.5f)
                .texOffs(77, 90).mirror(false).addBox(-3, -12.2f, -4.25f, 1.8f, 1.3f, 0.5f)
                .texOffs(77, 90).mirror(false).addBox(-0.4f, -14.2f, -4.25f, 1.8f, 1.3f, 0.5f)
                .texOffs(77, 90).mirror(false).addBox(2.2f, -12.2f, -4.25f, 1.8f, 1.3f, 0.5f)
                .texOffs(77, 90).mirror(false).addBox(4.8f, -14.2f, -4.25f, 1.8f, 1.3f, 0.5f)
                .texOffs(77, 90).mirror(false).addBox(-4.2f, -6.8f, -3.65f, 1.7f, 1.2f, 0.5f)
                .texOffs(77, 90).mirror(false).addBox(-1.4f, -4.6f, -3.65f, 1.7f, 1.2f, 0.5f)
                .texOffs(77, 90).mirror(false).addBox(1.4f, -6.8f, -3.65f, 1.7f, 1.2f, 0.5f)
                .texOffs(77, 90).mirror(false).addBox(4.2f, -4.6f, -3.65f, 1.7f, 1.2f, 0.5f), PartPose.offsetAndRotation(0, -2.6f, 0, 0, 0, 0));
        PartDefinition robe = torso.addOrReplaceChild("robe", CubeListBuilder.create()
                .texOffs(91, 0).mirror(false).addBox(-7.3f, -16.5f, -4.3f, 14.6f, 8.8f, 8.6f)
                .texOffs(185, 0).mirror(false).addBox(-5.9f, -8, -3.8f, 11.8f, 8.2f, 7.6f)
                .texOffs(169, 44).mirror(false).addBox(-6.3f, -3.2f, -4.3f, 12.6f, 2.6f, 8.6f)
                .texOffs(223, 82).mirror(false).addBox(-1.4f, -3.6f, -4.8f, 2.8f, 3.4f, 0.6f)
                .texOffs(0, 72).mirror(false).addBox(-3.8f, -17.3f, -3.4f, 7.6f, 1.6f, 6.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition collarOver = robe.addOrReplaceChild("collar_over", CubeListBuilder.create()
                .texOffs(174, 26).mirror(false).addBox(-1.1f, 0, -0.35f, 2.2f, 13.5f, 0.7f), PartPose.offsetAndRotation(3.6f, -16.4f, -4.45f, 0, 0, 0.62f));
        PartDefinition collarUnder = robe.addOrReplaceChild("collar_under", CubeListBuilder.create()
                .texOffs(60, 72).mirror(false).addBox(-1.1f, 0, -0.3f, 2.2f, 7, 0.6f), PartPose.offsetAndRotation(-3.6f, -16.4f, -4.4f, 0, 0, -0.62f));
        PartDefinition beltTail = robe.addOrReplaceChild("belt_tail", CubeListBuilder.create()
                .texOffs(135, 59).mirror(false).addBox(-0.8f, 0, -0.3f, 1.6f, 9, 0.5f), PartPose.offsetAndRotation(2.6f, -1, -4.6f, -0.05f, 0, -0.05f));
        PartDefinition skirt = waist.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(61, 26).mirror(false).addBox(-7.4f, -0.6f, -5, 14.8f, 5.4f, 10)
                .texOffs(0, 26).mirror(false).addBox(-8.6f, 4.4f, -5.9f, 17.2f, 4.6f, 11.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition apron = skirt.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(214, 44).mirror(false).addBox(-3.4f, 0, -0.5f, 6.8f, 11, 0.5f), PartPose.offsetAndRotation(0, -0.4f, -5.2f, -0.14f, 0, 0));
        PartDefinition rightSkirtPanel = skirt.addOrReplaceChild("right_skirt_panel", CubeListBuilder.create()
                .texOffs(115, 59).mirror(false).addBox(-2.6f, 0, -0.4f, 5.2f, 10, 0.4f), PartPose.offsetAndRotation(-6.4f, -0.4f, -4.9f, -0.08f, 0.4f, 0.1f));
        PartDefinition leftSkirtPanel = skirt.addOrReplaceChild("left_skirt_panel", CubeListBuilder.create()
                .texOffs(115, 59).mirror(true).addBox(-2.6f, 0, -0.4f, 5.2f, 10, 0.4f), PartPose.offsetAndRotation(6.4f, -0.4f, -4.9f, -0.08f, -0.4f, -0.1f));
        PartDefinition backPanel = skirt.addOrReplaceChild("back_panel", CubeListBuilder.create()
                .texOffs(231, 44).mirror(false).addBox(-5, 0, 0, 10, 10.5f, 0.5f), PartPose.offsetAndRotation(0, -0.5f, 5.6f, 0.12f, 0, 0));
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(108, 72).mirror(false).addBox(-1.7f, -2, -1.7f, 3.4f, 2.6f, 3.4f)
                .texOffs(183, 26).mirror(false).addBox(-3.1f, -10.2f, -3.2f, 6.2f, 8.6f, 6)
                .texOffs(125, 72).mirror(false).addBox(-3.5f, -6.2f, -2.4f, 7, 2, 4.4f)
                .texOffs(248, 82).mirror(false).addBox(-1.1f, -6, -3.9f, 2.2f, 3, 1)
                .texOffs(0, 59).mirror(false).addBox(-3.4f, -10.6f, -0.2f, 6.8f, 7.4f, 3.6f)
                .texOffs(31, 72).mirror(false).addBox(-3.4f, -10.8f, -3.4f, 6.8f, 1.4f, 6.6f), PartPose.offsetAndRotation(0, -17, -0.4f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(89, 90).mirror(false).addBox(-3.1f, -0.5f, -0.4f, 3.1f, 0.9f, 0.9f), PartPose.offsetAndRotation(-0.5f, -7.7f, -3.3f, 0, 0.1f, 0.38f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(89, 90).mirror(true).addBox(0, -0.5f, -0.4f, 3.1f, 0.9f, 0.9f), PartPose.offsetAndRotation(0.5f, -7.7f, -3.3f, 0, -0.1f, -0.38f));
        PartDefinition rightFang = head.addOrReplaceChild("right_fang", CubeListBuilder.create()
                .texOffs(0, 90).mirror(false).addBox(-0.45f, 0, -0.45f, 0.9f, 2.8f, 0.9f), PartPose.offsetAndRotation(-2.1f, -2.4f, -2.9f, 0.2f, 0, 0.3f));
        PartDefinition leftFang = head.addOrReplaceChild("left_fang", CubeListBuilder.create()
                .texOffs(0, 90).mirror(true).addBox(-0.45f, 0, -0.45f, 0.9f, 2.8f, 0.9f), PartPose.offsetAndRotation(2.1f, -2.4f, -2.9f, 0.2f, 0, -0.3f));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(142, 59).mirror(false).addBox(-0.35f, 0, -0.35f, 0.7f, 9, 0.7f), PartPose.offsetAndRotation(-0.7f, -3.6f, -3.5f, 0.05f, 0.15f, 0.12f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(142, 59).mirror(true).addBox(-0.35f, 0, -0.35f, 0.7f, 9, 0.7f), PartPose.offsetAndRotation(0.7f, -3.6f, -3.5f, 0.05f, -0.15f, -0.12f));
        PartDefinition rightMoustacheTip = rightMoustache.addOrReplaceChild("right_moustache_tip", CubeListBuilder.create()
                .texOffs(168, 82).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 5, 0.6f), PartPose.offsetAndRotation(0, 8.8f, 0, 0.1f, 0, 0.15f));
        PartDefinition leftMoustacheTip = leftMoustache.addOrReplaceChild("left_moustache_tip", CubeListBuilder.create()
                .texOffs(168, 82).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 5, 0.6f), PartPose.offsetAndRotation(0, 8.8f, 0, 0.1f, 0, -0.15f));
        PartDefinition beard = head.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(23, 59).mirror(false).addBox(-0.8f, 0, -0.6f, 1.6f, 10, 1.2f), PartPose.offsetAndRotation(0, -1.8f, -3, 0.1f, 0, 0));
        PartDefinition beardTip = beard.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(69, 72).mirror(false).addBox(-0.55f, 0, -0.45f, 1.1f, 7, 0.9f), PartPose.offsetAndRotation(0, 9.8f, 0, -0.12f, 0, 0));
        PartDefinition rightSideburn = head.addOrReplaceChild("right_sideburn", CubeListBuilder.create()
                .texOffs(76, 72).mirror(false).addBox(-0.4f, 0, -0.6f, 0.8f, 6, 1.2f), PartPose.offsetAndRotation(-3.2f, -6, -1.4f, 0.05f, 0, 0.12f));
        PartDefinition leftSideburn = head.addOrReplaceChild("left_sideburn", CubeListBuilder.create()
                .texOffs(76, 72).mirror(true).addBox(-0.4f, 0, -0.6f, 0.8f, 6, 1.2f), PartPose.offsetAndRotation(3.2f, -6, -1.4f, 0.05f, 0, -0.12f));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(173, 82).mirror(false).addBox(-1.5f, -2.6f, -1.5f, 3, 2.8f, 3)
                .texOffs(150, 72).mirror(false).addBox(-2.4f, -2.2f, -2.4f, 4.8f, 1.4f, 4.8f)
                .texOffs(100, 90).mirror(false).addBox(-5.6f, -2, -0.3f, 11.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(0, -10.8f, 0.2f, 0, 0, 0));
        PartDefinition crownPetal0 = crown.addOrReplaceChild("crown_petal_0", CubeListBuilder.create()
                .texOffs(232, 82).mirror(false).addBox(-1.2f, -3.4f, -0.3f, 2.4f, 3.4f, 0.6f), PartPose.offsetAndRotation(-0.6496f, -2.2f, 1.7024f, 0.3027f, -2.8108f, -0.1039f));
        PartDefinition crownPetal1 = crown.addOrReplaceChild("crown_petal_1", CubeListBuilder.create()
                .texOffs(232, 82).mirror(false).addBox(-1.2f, -3.4f, -0.3f, 2.4f, 3.4f, 0.6f), PartPose.offsetAndRotation(-1.6278f, -2.2f, 1.0459f, 0.1859f, -2.1908f, -0.2604f));
        PartDefinition crownPetal2 = crown.addOrReplaceChild("crown_petal_2", CubeListBuilder.create()
                .texOffs(232, 82).mirror(false).addBox(-1.2f, -3.4f, -0.3f, 2.4f, 3.4f, 0.6f), PartPose.offsetAndRotation(-2, -2.2f, -0, -0, -1.5708f, -0.32f));
        PartDefinition crownPetal3 = crown.addOrReplaceChild("crown_petal_3", CubeListBuilder.create()
                .texOffs(232, 82).mirror(false).addBox(-1.2f, -3.4f, -0.3f, 2.4f, 3.4f, 0.6f), PartPose.offsetAndRotation(-1.6278f, -2.2f, -1.0459f, -0.1859f, -0.9508f, -0.2604f));
        PartDefinition crownPetal4 = crown.addOrReplaceChild("crown_petal_4", CubeListBuilder.create()
                .texOffs(232, 82).mirror(false).addBox(-1.2f, -3.4f, -0.3f, 2.4f, 3.4f, 0.6f), PartPose.offsetAndRotation(-0.6496f, -2.2f, -1.7024f, -0.3027f, -0.3308f, -0.1039f));
        PartDefinition rightFeeler = crown.addOrReplaceChild("right_feeler", CubeListBuilder.create()
                .texOffs(130, 59).mirror(false).addBox(-0.3f, -10, -0.3f, 0.6f, 10, 0.6f), PartPose.offsetAndRotation(-1, -2.4f, -1.2f, -0.95f, 0.4f, -0.3f));
        PartDefinition leftFeeler = crown.addOrReplaceChild("left_feeler", CubeListBuilder.create()
                .texOffs(130, 59).mirror(true).addBox(-0.3f, -10, -0.3f, 0.6f, 10, 0.6f), PartPose.offsetAndRotation(1, -2.4f, -1.2f, -0.95f, -0.4f, 0.3f));
        PartDefinition rightFeelerTip = rightFeeler.addOrReplaceChild("right_feeler_tip", CubeListBuilder.create()
                .texOffs(100, 44).mirror(false).addBox(-0.25f, -12, -0.25f, 0.5f, 12, 0.5f), PartPose.offsetAndRotation(0, -10, 0, -0.8f, 0, -0.12f));
        PartDefinition leftFeelerTip = leftFeeler.addOrReplaceChild("left_feeler_tip", CubeListBuilder.create()
                .texOffs(100, 44).mirror(true).addBox(-0.25f, -12, -0.25f, 0.5f, 12, 0.5f), PartPose.offsetAndRotation(0, -10, 0, -0.8f, 0, 0.12f));
        PartDefinition rightArm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(9, 44).mirror(false).addBox(-3.2f, -1.4f, -2.2f, 4, 9, 4.4f)
                .texOffs(5, 90).mirror(false).addBox(-3.6f, 0.8f, -1.6f, 0.6f, 1.4f, 1.5f)
                .texOffs(5, 90).mirror(false).addBox(-3.6f, 3, 0.2f, 0.6f, 1.4f, 1.5f)
                .texOffs(5, 90).mirror(false).addBox(-3.6f, 5.2f, -1.6f, 0.6f, 1.4f, 1.5f)
                .texOffs(5, 90).mirror(false).addBox(-3.6f, 7.4f, 0.2f, 0.6f, 1.4f, 1.5f), PartPose.offsetAndRotation(-7, -14.6f, 0, 0.05f, 0, 0.12f));
        PartDefinition leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(9, 44).mirror(true).addBox(-0.8f, -1.4f, -2.2f, 4, 9, 4.4f)
                .texOffs(5, 90).mirror(true).addBox(3, 0.8f, -1.6f, 0.6f, 1.4f, 1.5f)
                .texOffs(5, 90).mirror(true).addBox(3, 3, 0.2f, 0.6f, 1.4f, 1.5f)
                .texOffs(5, 90).mirror(true).addBox(3, 5.2f, -1.6f, 0.6f, 1.4f, 1.5f)
                .texOffs(5, 90).mirror(true).addBox(3, 7.4f, 0.2f, 0.6f, 1.4f, 1.5f), PartPose.offsetAndRotation(7, -14.6f, 0, 0.05f, 0, -0.12f));
        PartDefinition rightRobeSleeve = rightArm.addOrReplaceChild("right_robe_sleeve", CubeListBuilder.create()
                .texOffs(112, 26).mirror(false).addBox(-3.8f, -2, -2.8f, 5.2f, 9.8f, 5.6f)
                .texOffs(147, 59).mirror(false).addBox(-4.2f, -2.6f, -3.2f, 5.6f, 3, 6.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition leftRobeSleeve = leftArm.addOrReplaceChild("left_robe_sleeve", CubeListBuilder.create()
                .texOffs(112, 26).mirror(true).addBox(-1.4f, -2, -2.8f, 5.2f, 9.8f, 5.6f)
                .texOffs(147, 59).mirror(true).addBox(-1.4f, -2.6f, -3.2f, 5.6f, 3, 6.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(32, 59).mirror(false).addBox(-1.6f, 0, -1.6f, 3.2f, 7.6f, 3.2f)
                .texOffs(186, 82).mirror(false).addBox(-2, 6.8f, -2, 4, 1.6f, 4)
                .texOffs(5, 90).mirror(false).addBox(-1.9f, 1.6f, -0.6f, 0.4f, 1.2f, 1.2f)
                .texOffs(5, 90).mirror(false).addBox(-1.9f, 4.2f, -0.6f, 0.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(-1.1f, 7.8f, 0, -0.9f, -0.35f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(32, 59).mirror(true).addBox(-1.6f, 0, -1.6f, 3.2f, 7.6f, 3.2f)
                .texOffs(186, 82).mirror(true).addBox(-2, 6.8f, -2, 4, 1.6f, 4)
                .texOffs(5, 90).mirror(true).addBox(1.5f, 1.6f, -0.6f, 0.4f, 1.2f, 1.2f)
                .texOffs(5, 90).mirror(true).addBox(1.5f, 4.2f, -0.6f, 0.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(1.1f, 7.8f, 0, -0.9f, 0.35f, 0));
        PartDefinition rightGreatSleeve = rightForearm.addOrReplaceChild("right_great_sleeve", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-3.8f, -1, -10, 7.6f, 14, 10.6f), PartPose.offsetAndRotation(0, 0.4f, 0.8f, 0.9031f, 0.0197f, 0.0486f));
        PartDefinition leftGreatSleeve = leftForearm.addOrReplaceChild("left_great_sleeve", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(-3.8f, -1, -10, 7.6f, 14, 10.6f), PartPose.offsetAndRotation(0, 0.4f, 0.8f, 0.9031f, -0.0197f, -0.0486f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(171, 72).mirror(false).addBox(-1.7f, 0, -1.7f, 3.4f, 3, 3.4f)
                .texOffs(84, 90).mirror(false).addBox(-1.5f, 2.8f, -1.9f, 0.8f, 1.8f, 0.8f)
                .texOffs(84, 90).mirror(false).addBox(0.6f, 2.8f, -1.9f, 0.8f, 1.8f, 0.8f), PartPose.offsetAndRotation(0, 8.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(171, 72).mirror(true).addBox(-1.7f, 0, -1.7f, 3.4f, 3, 3.4f)
                .texOffs(84, 90).mirror(true).addBox(0.7f, 2.8f, -1.9f, 0.8f, 1.8f, 0.8f)
                .texOffs(84, 90).mirror(true).addBox(-1.4f, 2.8f, -1.9f, 0.8f, 1.8f, 0.8f), PartPose.offsetAndRotation(0, 8.2f, 0, 0, 0, 0));
        PartDefinition sword = rightHand.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(39, 0).mirror(false).addBox(-0.3f, -26, -0.9f, 0.6f, 22, 1.8f)
                .texOffs(12, 90).mirror(false).addBox(-0.25f, -28, -0.6f, 0.5f, 2, 1.2f)
                .texOffs(203, 82).mirror(false).addBox(-0.8f, -4.4f, -2.2f, 1.6f, 1, 4.4f)
                .texOffs(188, 72).mirror(false).addBox(-0.6f, -3.4f, -0.6f, 1.2f, 5, 1.2f)
                .texOffs(19, 90).mirror(false).addBox(-0.8f, 1.6f, -0.8f, 1.6f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 1.6f, -0.4f, 0.7096f, 0.3066f, 0.1984f));
        PartDefinition tassel = sword.addOrReplaceChild("tassel", CubeListBuilder.create()
                .texOffs(218, 82).mirror(false).addBox(-0.4f, 0, -0.4f, 0.8f, 5, 0.8f), PartPose.offsetAndRotation(0, 2.8f, 0, 0.1f, 0, 0));
        PartDefinition rightCleg0 = seg0.addOrReplaceChild("right_cleg_0", CubeListBuilder.create()
                .texOffs(28, 90).mirror(false).addBox(-5.5f, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(-6.1f, 1.6f, 0, 0, -0.15f, 0.45f));
        PartDefinition leftCleg0 = seg0.addOrReplaceChild("left_cleg_0", CubeListBuilder.create()
                .texOffs(28, 90).mirror(true).addBox(0, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(6.1f, 1.6f, 0, 0, 0.15f, -0.45f));
        PartDefinition rightClegTip0 = rightCleg0.addOrReplaceChild("right_cleg_tip_0", CubeListBuilder.create()
                .texOffs(45, 90).mirror(false).addBox(-8.2f, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(false).addBox(-1, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-5.5f, 0, 0, 0, 0, -1.25f));
        PartDefinition leftClegTip0 = leftCleg0.addOrReplaceChild("left_cleg_tip_0", CubeListBuilder.create()
                .texOffs(45, 90).mirror(true).addBox(0, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(true).addBox(-0.4f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(5.5f, 0, 0, 0, 0, 1.25f));
        PartDefinition seg1 = seg0.addOrReplaceChild("seg_1", CubeListBuilder.create()
                .texOffs(210, 26).mirror(false).addBox(-5.75f, -3.75f, 0, 11.5f, 7.5f, 6.6f)
                .texOffs(197, 72).mirror(false).addBox(-5.15f, -4.35f, 0.6f, 10.3f, 0.8f, 5.2f), PartPose.offsetAndRotation(0, 0, 4.4f, 0, 0, 0));
        PartDefinition rightCleg1 = seg1.addOrReplaceChild("right_cleg_1", CubeListBuilder.create()
                .texOffs(28, 90).mirror(false).addBox(-5.5f, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(-5.35f, 1.6f, 3.3f, 0, -0.1f, 0.45f));
        PartDefinition leftCleg1 = seg1.addOrReplaceChild("left_cleg_1", CubeListBuilder.create()
                .texOffs(28, 90).mirror(true).addBox(0, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(5.35f, 1.6f, 3.3f, 0, 0.1f, -0.45f));
        PartDefinition rightClegTip1 = rightCleg1.addOrReplaceChild("right_cleg_tip_1", CubeListBuilder.create()
                .texOffs(45, 90).mirror(false).addBox(-8.2f, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(false).addBox(-1, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-5.5f, 0, 0, 0, 0, -1.25f));
        PartDefinition leftClegTip1 = leftCleg1.addOrReplaceChild("left_cleg_tip_1", CubeListBuilder.create()
                .texOffs(45, 90).mirror(true).addBox(0, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(true).addBox(-0.4f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(5.5f, 0, 0, 0, 0, 1.25f));
        PartDefinition seg2 = seg1.addOrReplaceChild("seg_2", CubeListBuilder.create()
                .texOffs(28, 44).mirror(false).addBox(-5.3f, -3.5f, 0, 10.6f, 7, 6.6f)
                .texOffs(0, 82).mirror(false).addBox(-4.7f, -4.1f, 0.6f, 9.4f, 0.8f, 5.2f), PartPose.offsetAndRotation(0, 0, 6.2f, 0, 0, 0));
        PartDefinition rightCleg2 = seg2.addOrReplaceChild("right_cleg_2", CubeListBuilder.create()
                .texOffs(28, 90).mirror(false).addBox(-5.5f, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(-4.9f, 1.6f, 3.3f, 0, -0.05f, 0.45f));
        PartDefinition leftCleg2 = seg2.addOrReplaceChild("left_cleg_2", CubeListBuilder.create()
                .texOffs(28, 90).mirror(true).addBox(0, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(4.9f, 1.6f, 3.3f, 0, 0.05f, -0.45f));
        PartDefinition rightClegTip2 = rightCleg2.addOrReplaceChild("right_cleg_tip_2", CubeListBuilder.create()
                .texOffs(45, 90).mirror(false).addBox(-8.2f, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(false).addBox(-1, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-5.5f, 0, 0, 0, 0, -1.25f));
        PartDefinition leftClegTip2 = leftCleg2.addOrReplaceChild("left_cleg_tip_2", CubeListBuilder.create()
                .texOffs(45, 90).mirror(true).addBox(0, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(true).addBox(-0.4f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(5.5f, 0, 0, 0, 0, 1.25f));
        PartDefinition seg3 = seg2.addOrReplaceChild("seg_3", CubeListBuilder.create()
                .texOffs(65, 44).mirror(false).addBox(-4.85f, -3.25f, 0, 9.7f, 6.5f, 6.6f)
                .texOffs(33, 82).mirror(false).addBox(-4.25f, -3.85f, 0.6f, 8.5f, 0.8f, 5.2f), PartPose.offsetAndRotation(0, 0, 6.2f, 0, 0, 0));
        PartDefinition rightCleg3 = seg3.addOrReplaceChild("right_cleg_3", CubeListBuilder.create()
                .texOffs(28, 90).mirror(false).addBox(-5.5f, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(-4.45f, 1.6f, 3.3f, 0, 0, 0.45f));
        PartDefinition leftCleg3 = seg3.addOrReplaceChild("left_cleg_3", CubeListBuilder.create()
                .texOffs(28, 90).mirror(true).addBox(0, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(4.45f, 1.6f, 3.3f, 0, -0, -0.45f));
        PartDefinition rightClegTip3 = rightCleg3.addOrReplaceChild("right_cleg_tip_3", CubeListBuilder.create()
                .texOffs(45, 90).mirror(false).addBox(-8.2f, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(false).addBox(-1, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-5.5f, 0, 0, 0, 0, -1.25f));
        PartDefinition leftClegTip3 = leftCleg3.addOrReplaceChild("left_cleg_tip_3", CubeListBuilder.create()
                .texOffs(45, 90).mirror(true).addBox(0, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(true).addBox(-0.4f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(5.5f, 0, 0, 0, 0, 1.25f));
        PartDefinition seg4 = seg3.addOrReplaceChild("seg_4", CubeListBuilder.create()
                .texOffs(105, 44).mirror(false).addBox(-4.4f, -3, 0, 8.8f, 6, 6.6f)
                .texOffs(64, 82).mirror(false).addBox(-3.8f, -3.6f, 0.6f, 7.6f, 0.8f, 5.2f), PartPose.offsetAndRotation(0, 0, 6.2f, 0, 0, 0));
        PartDefinition rightCleg4 = seg4.addOrReplaceChild("right_cleg_4", CubeListBuilder.create()
                .texOffs(28, 90).mirror(false).addBox(-5.5f, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(-4, 1.6f, 3.3f, 0, 0.05f, 0.45f));
        PartDefinition leftCleg4 = seg4.addOrReplaceChild("left_cleg_4", CubeListBuilder.create()
                .texOffs(28, 90).mirror(true).addBox(0, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(4, 1.6f, 3.3f, 0, -0.05f, -0.45f));
        PartDefinition rightClegTip4 = rightCleg4.addOrReplaceChild("right_cleg_tip_4", CubeListBuilder.create()
                .texOffs(45, 90).mirror(false).addBox(-8.2f, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(false).addBox(-1, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-5.5f, 0, 0, 0, 0, -1.25f));
        PartDefinition leftClegTip4 = leftCleg4.addOrReplaceChild("left_cleg_tip_4", CubeListBuilder.create()
                .texOffs(45, 90).mirror(true).addBox(0, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(true).addBox(-0.4f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(5.5f, 0, 0, 0, 0, 1.25f));
        PartDefinition seg5 = seg4.addOrReplaceChild("seg_5", CubeListBuilder.create()
                .texOffs(138, 44).mirror(false).addBox(-3.95f, -2.75f, 0, 7.9f, 5.5f, 6.6f)
                .texOffs(93, 82).mirror(false).addBox(-3.35f, -3.35f, 0.6f, 6.7f, 0.8f, 5.2f), PartPose.offsetAndRotation(0, 0, 6.2f, 0, 0, 0));
        PartDefinition rightCleg5 = seg5.addOrReplaceChild("right_cleg_5", CubeListBuilder.create()
                .texOffs(28, 90).mirror(false).addBox(-5.5f, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(-3.55f, 1.6f, 3.3f, 0, 0.1f, 0.45f));
        PartDefinition leftCleg5 = seg5.addOrReplaceChild("left_cleg_5", CubeListBuilder.create()
                .texOffs(28, 90).mirror(true).addBox(0, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(3.55f, 1.6f, 3.3f, 0, -0.1f, -0.45f));
        PartDefinition rightClegTip5 = rightCleg5.addOrReplaceChild("right_cleg_tip_5", CubeListBuilder.create()
                .texOffs(45, 90).mirror(false).addBox(-8.2f, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(false).addBox(-1, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-5.5f, 0, 0, 0, 0, -1.25f));
        PartDefinition leftClegTip5 = leftCleg5.addOrReplaceChild("left_cleg_tip_5", CubeListBuilder.create()
                .texOffs(45, 90).mirror(true).addBox(0, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(true).addBox(-0.4f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(5.5f, 0, 0, 0, 0, 1.25f));
        PartDefinition seg6 = seg5.addOrReplaceChild("seg_6", CubeListBuilder.create()
                .texOffs(49, 59).mirror(false).addBox(-3.5f, -2.5f, 0, 7, 5, 6.6f)
                .texOffs(120, 82).mirror(false).addBox(-2.9f, -3.1f, 0.6f, 5.8f, 0.8f, 5.2f), PartPose.offsetAndRotation(0, 0, 6.2f, 0, 0, 0));
        PartDefinition rightCleg6 = seg6.addOrReplaceChild("right_cleg_6", CubeListBuilder.create()
                .texOffs(28, 90).mirror(false).addBox(-5.5f, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(-3.1f, 1.6f, 3.3f, 0, 0.15f, 0.45f));
        PartDefinition leftCleg6 = seg6.addOrReplaceChild("left_cleg_6", CubeListBuilder.create()
                .texOffs(28, 90).mirror(true).addBox(0, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(3.1f, 1.6f, 3.3f, 0, -0.15f, -0.45f));
        PartDefinition rightClegTip6 = rightCleg6.addOrReplaceChild("right_cleg_tip_6", CubeListBuilder.create()
                .texOffs(45, 90).mirror(false).addBox(-8.2f, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(false).addBox(-1, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-5.5f, 0, 0, 0, 0, -1.25f));
        PartDefinition leftClegTip6 = leftCleg6.addOrReplaceChild("left_cleg_tip_6", CubeListBuilder.create()
                .texOffs(45, 90).mirror(true).addBox(0, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(true).addBox(-0.4f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(5.5f, 0, 0, 0, 0, 1.25f));
        PartDefinition seg7 = seg6.addOrReplaceChild("seg_7", CubeListBuilder.create()
                .texOffs(49, 59).mirror(false).addBox(-3.05f, -2.25f, 0, 6.1f, 4.5f, 6.6f)
                .texOffs(145, 82).mirror(false).addBox(-2.45f, -2.85f, 0.6f, 4.9f, 0.8f, 5.2f), PartPose.offsetAndRotation(0, 0, 6.2f, 0, 0, 0));
        PartDefinition rightCleg7 = seg7.addOrReplaceChild("right_cleg_7", CubeListBuilder.create()
                .texOffs(28, 90).mirror(false).addBox(-5.5f, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(-2.65f, 1.6f, 3.3f, 0, 0.2f, 0.45f));
        PartDefinition leftCleg7 = seg7.addOrReplaceChild("left_cleg_7", CubeListBuilder.create()
                .texOffs(28, 90).mirror(true).addBox(0, -0.8f, -0.8f, 5.5f, 1.6f, 1.6f), PartPose.offsetAndRotation(2.65f, 1.6f, 3.3f, 0, -0.2f, -0.45f));
        PartDefinition rightClegTip7 = rightCleg7.addOrReplaceChild("right_cleg_tip_7", CubeListBuilder.create()
                .texOffs(45, 90).mirror(false).addBox(-8.2f, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(false).addBox(-1, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-5.5f, 0, 0, 0, 0, -1.25f));
        PartDefinition leftClegTip7 = leftCleg7.addOrReplaceChild("left_cleg_tip_7", CubeListBuilder.create()
                .texOffs(45, 90).mirror(true).addBox(0, -0.55f, -0.55f, 8.2f, 1.1f, 1.1f)
                .texOffs(68, 90).mirror(true).addBox(-0.4f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(5.5f, 0, 0, 0, 0, 1.25f));
        PartDefinition rightTailspine = seg7.addOrReplaceChild("right_tailspine", CubeListBuilder.create()
                .texOffs(174, 59).mirror(false).addBox(-0.5f, -0.5f, 0, 1, 1, 9), PartPose.offsetAndRotation(-1.8f, -0.5f, 6.4f, 0.15f, -0.3f, 0));
        PartDefinition leftTailspine = seg7.addOrReplaceChild("left_tailspine", CubeListBuilder.create()
                .texOffs(174, 59).mirror(true).addBox(-0.5f, -0.5f, 0, 1, 1, 9), PartPose.offsetAndRotation(1.8f, -0.5f, 6.4f, 0.15f, 0.3f, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED lord: end
    }

    /**
     * The Hundred-Eyed Demon Lord: his centipede's body rippling behind him in a slow wave, its legs
     * stepping in a wave down its length; his torso upright over it, his beard and sleeves stirring;
     * his sword raised before him, brought down on a blow. Glaring, he flings both arms up, his eyes
     * ablaze; pouring his tea, he flicks his left sleeve forward; rearing, he rises back on his coils
     * and the body winds round. His robe and sleeves gone once he has thrown them off, the eyes bared.
     */
    public static final class LordModel extends Whole<HundredEyedDemonEntity> {
        public static final float SIZE = 1.3f;
        private final ModelPart torso, head, beard, rightArm, leftArm, rightForearm, leftForearm, rightGreat, leftGreat, robe, rightRobe, leftRobe,
                skirt, rightFeeler, leftFeeler;
        private final List<ModelPart> segs = new ArrayList<>(), rightLegs = new ArrayList<>(), leftLegs = new ArrayList<>();

        public LordModel(ModelPart root) {
            super(root);
            torso = p("torso");
            head = p("head");
            beard = p("beard");
            rightArm = p("right_arm");
            leftArm = p("left_arm");
            rightForearm = p("right_forearm");
            leftForearm = p("left_forearm");
            rightGreat = p("right_great_sleeve");
            leftGreat = p("left_great_sleeve");
            robe = p("robe");
            rightRobe = p("right_robe_sleeve");
            leftRobe = p("left_robe_sleeve");
            skirt = p("skirt");
            rightFeeler = p("right_feeler");
            leftFeeler = p("left_feeler");
            for (int s = 0; ; s++) {
                String n = "seg_" + s;
                if (s > 0 && !hasPart(n)) break;
                segs.add(p(n));
                rightLegs.add(p("right_cleg_" + s));
                leftLegs.add(p("left_cleg_" + s));
            }
        }

        private boolean hasPart(String name) {
            try {
                p(name);
                return true;
            } catch (IllegalStateException e) {
                return false;
            }
        }

        @Override
        public void setupAnim(HundredEyedDemonEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            int act = e.act();
            boolean glare = act == HundredEyedDemonEntity.GLARING, pour = act == HundredEyedDemonEntity.POURING,
                    rear = act == HundredEyedDemonEntity.REARING, bare = e.unrobed();
            float wave = age * 0.12f + swing * 0.5f;
            // the body behind him, rippling, winding round as he rears
            for (int s = 1; s < segs.size(); s++) {
                float coil = rear ? 0.22f : 0;
                turn(segs.get(s), rear ? -0.05f : 0, Mth.sin(wave - s * 0.8f) * (0.1f + 0.08f * amount) + coil, 0);
            }
            for (int s = 0; s < rightLegs.size(); s++) {
                float st = Mth.sin(wave * 2 - s * 1.1f);
                turn(rightLegs.get(s), 0, st * 0.25f, Math.max(0, st) * 0.3f);
                turn(leftLegs.get(s), 0, -st * 0.25f, -Math.max(0, -st) * 0.3f);
            }
            float breath = Mth.sin(age * 0.05f);
            turn(torso, rear ? -0.35f : 0, 0, 0);
            torso.y = torso.getInitialPose().y - (rear ? 2.5f : 0) + breath * 0.25f;
            turn(head, pitch * Mth.DEG_TO_RAD + (glare ? -0.25f : 0), yaw * Mth.DEG_TO_RAD, 0);
            turn(beard, Mth.sin(age * 0.05f) * 0.04f + amount * 0.1f, 0, 0);
            turnPair(rightFeeler, leftFeeler, Mth.sin(age * 0.09f) * 0.1f + (glare ? -0.4f : 0), Mth.cos(age * 0.07f) * 0.1f, 0);
            // his arms: the sword held up before him; flung up in his glare; his left sleeve flicked out over the tea
            float rx = Mth.sin(age * 0.05f) * 0.03f - strike * 1.4f, lx = -rx * 0.5f, rz = 0, lz = 0;
            if (glare) {
                rx = -2.6f;
                lx = -2.6f;
                rz = 0.5f;
                lz = -0.5f;
            } else if (pour) {
                lx = -1.4f;
                lz = -0.2f;
            } else if (rear) {
                rx = -1.0f;
                lx = -1.0f;
            }
            turn(rightArm, rx, 0, rz);
            turn(leftArm, lx, 0, lz);
            turn(rightForearm, strike * 0.6f + (glare ? 0.6f : 0), 0, 0);
            turn(leftForearm, glare ? 0.6f : pour ? 0.5f : 0, 0, 0);
            turn(rightGreat, -(rx + (glare ? 0.6f : 0)) * 0.8f + breath * 0.04f + amount * 0.15f, 0, 0);
            turn(leftGreat, -(lx + (glare || pour ? 0.6f : 0)) * 0.8f - breath * 0.04f + amount * 0.15f, 0, 0);
            turn(skirt, 0, 0, Mth.sin(wave) * 0.02f);
            robe.visible = !bare;
            rightRobe.visible = leftRobe.visible = !bare;
            rightGreat.visible = leftGreat.visible = !bare;
        }
    }

    /** His renderer: his skin, and his eyes glowing over it; ablaze while he glares or bares them. */
    public static final class LordRenderer extends MobRenderer<HundredEyedDemonEntity, LordModel> {
        private static final ResourceLocation SKIN = tex("hundred_eyed_demon");
        private static final RenderType GLOW = RenderType.eyes(tex("hundred_eyed_demon_glow")), BLAZE = RenderType.eyes(tex("hundred_eyed_demon_blaze_glow"));

        public LordRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new LordModel(ctx.bakeLayer(LORD_LAYER)), 1.4f);
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, HundredEyedDemonEntity e, float swing, float amount,
                                   float partial, float age, float yaw, float pitch) {
                    boolean ablaze = e.act() == HundredEyedDemonEntity.GLARING || (e.unrobed() && Mth.sin(age * 0.1f) > 0.6f);
                    VertexConsumer vc = buffers.getBuffer(ablaze ? BLAZE : GLOW);
                    getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        @Override
        protected void scale(HundredEyedDemonEntity e, PoseStack pose, float partial) {
            pose.scale(LordModel.SIZE, LordModel.SIZE, LordModel.SIZE);
        }

        @Override
        public ResourceLocation getTextureLocation(HundredEyedDemonEntity e) {
            return SKIN;
        }
    }
}
