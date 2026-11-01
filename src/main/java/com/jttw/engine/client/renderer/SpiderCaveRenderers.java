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

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * How Silk-Web Ridge's creatures look: the spider demon (a calf-sized spider of black-violet chitin,
 * its abdomen masked with a crimson hourglass, eight glowing eyes in a heavy brow, eight long banded
 * legs), the stinging sons (upright insect demons on glassy wings, banded abdomens curled to their
 * stings, barbed lances; honeybee, hornet and gadfly skins), the Mantis Son (an upright mantis demon,
 * scythes folded, a crimson sash), the Cockroach Son (a squat beetle-demon in glossy, domed chestnut
 * shell), the Seven Spider Spirits (seven sisters in layered Tang court silks, eight jointed spider's legs
 * fanned behind them, a diadem of eight small gems), and the Hundred-Eyed Demon Lord (a Daoist in
 * temple yellow trailing a centipede's tail, golden eyes all through his robe and his body under it). Models and skins
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
                .texOffs(71, 22).mirror(false).addBox(-3.4f, -2.6f, -3.2f, 6.8f, 5.2f, 6.4f)
                .texOffs(84, 38).mirror(false).addBox(-3.8f, -1.4f, -3.6f, 7.6f, 1.6f, 7.2f), PartPose.offsetAndRotation(0, 6.5f, 0, 0, 0, 0));
        PartDefinition sashTail = waist.addOrReplaceChild("sash_tail", CubeListBuilder.create()
                .texOffs(77, 51).mirror(false).addBox(-0.9f, 0, -0.3f, 1.8f, 7, 0.4f), PartPose.offsetAndRotation(1.6f, 0, -3, -0.1f, 0, -0.1f));
        PartDefinition abdomen = waist.addOrReplaceChild("abdomen", CubeListBuilder.create()
                .texOffs(25, 22).mirror(false).addBox(-3.6f, -2.4f, 0, 7.2f, 5.2f, 7.5f)
                .texOffs(0, 38).mirror(false).addBox(-3, -2, 7, 6, 4.6f, 6.5f)
                .texOffs(109, 51).mirror(false).addBox(-1.8f, -1, 12, 3.6f, 2.8f, 3), PartPose.offsetAndRotation(0, -0.6f, 2.2f, -0.32f, 0, 0));
        PartDefinition wings = abdomen.addOrReplaceChild("wings", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -2.7f, -0.5f, 0.06f, 0, 0));
        PartDefinition rightTegmen = wings.addOrReplaceChild("right_tegmen", CubeListBuilder.create()
                .texOffs(17, 0).mirror(false).addBox(-4.6f, -0.3f, 0, 4.6f, 0.6f, 17.5f), PartPose.offsetAndRotation(-0.1f, -0.2f, 0, 0, 0, 0.42f));
        PartDefinition leftTegmen = wings.addOrReplaceChild("left_tegmen", CubeListBuilder.create()
                .texOffs(17, 0).mirror(true).addBox(0, -0.3f, 0, 4.6f, 0.6f, 17.5f), PartPose.offsetAndRotation(0.1f, -0.2f, 0, 0, 0, -0.42f));
        PartDefinition rightHindwing = wings.addOrReplaceChild("right_hindwing", CubeListBuilder.create()
                .texOffs(64, 0).mirror(false).addBox(-4.4f, -0.2f, 0, 4.4f, 0.4f, 15.5f), PartPose.offsetAndRotation(-0.3f, 0.5f, 0.6f, 0, 0, 0.62f));
        PartDefinition leftHindwing = wings.addOrReplaceChild("left_hindwing", CubeListBuilder.create()
                .texOffs(64, 0).mirror(true).addBox(0, -0.2f, 0, 4.4f, 0.4f, 15.5f), PartPose.offsetAndRotation(0.3f, 0.5f, 0.6f, 0, 0, -0.62f));
        PartDefinition prothorax = waist.addOrReplaceChild("prothorax", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.9f, -17, -1.9f, 3.8f, 17, 3.8f)
                .texOffs(0, 22).mirror(false).addBox(-2.8f, -8, -2.7f, 5.6f, 8.2f, 5.4f)
                .texOffs(48, 38).mirror(false).addBox(-3.4f, -15, -2.9f, 6.8f, 4.8f, 5.6f)
                .texOffs(38, 51).mirror(false).addBox(-2.6f, -17.6f, -2.6f, 5.2f, 2.6f, 5.2f)
                .texOffs(0, 68).mirror(false).addBox(-2.2f, -15, -2.3f, 4.4f, 1.2f, 0.8f)
                .texOffs(20, 68).mirror(false).addBox(-2.25f, -9.5f, -0.4f, 0.5f, 0.5f, 0.9f)
                .texOffs(20, 68).mirror(false).addBox(1.75f, -9.5f, -0.4f, 0.5f, 0.5f, 0.9f)
                .texOffs(20, 68).mirror(false).addBox(-2.25f, -6, -0.4f, 0.5f, 0.5f, 0.9f)
                .texOffs(20, 68).mirror(false).addBox(1.75f, -6, -0.4f, 0.5f, 0.5f, 0.9f)
                .texOffs(20, 68).mirror(false).addBox(-2.25f, -2.5f, -0.4f, 0.5f, 0.5f, 0.9f)
                .texOffs(20, 68).mirror(false).addBox(1.75f, -2.5f, -0.4f, 0.5f, 0.5f, 0.9f), PartPose.offsetAndRotation(0, -2, -0.5f, 0.14f, 0, 0));
        PartDefinition head = prothorax.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 51).mirror(false).addBox(-5.4f, -4.4f, -2.9f, 10.8f, 4.4f, 4.8f)
                .texOffs(84, 51).mirror(false).addBox(-3.6f, 0, -2.9f, 7.2f, 2.6f, 3.8f)
                .texOffs(0, 62).mirror(false).addBox(-2.2f, 2.4f, -2.8f, 4.4f, 1.8f, 3)
                .texOffs(17, 62).mirror(false).addBox(-1.2f, 4, -2.7f, 2.4f, 2, 2.4f), PartPose.offsetAndRotation(0, -17.6f, -0.4f, -0.1f, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(27, 38).mirror(false).addBox(-3, -2.8f, -2.8f, 4, 5.6f, 5.6f), PartPose.offsetAndRotation(-4.9f, -2.8f, -0.5f, 0, 0.15f, 0.15f));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(27, 38).mirror(true).addBox(-1, -2.8f, -2.8f, 4, 5.6f, 5.6f), PartPose.offsetAndRotation(4.9f, -2.8f, -0.5f, 0, -0.15f, -0.15f));
        PartDefinition rightAntenna = head.addOrReplaceChild("right_antenna", CubeListBuilder.create()
                .texOffs(100, 22).mirror(false).addBox(-0.25f, -12, -0.25f, 0.5f, 12, 0.5f), PartPose.offsetAndRotation(-0.8f, -3.2f, -2, 0.85f, 0.25f, -0.2f));
        PartDefinition leftAntenna = head.addOrReplaceChild("left_antenna", CubeListBuilder.create()
                .texOffs(100, 22).mirror(true).addBox(-0.25f, -12, -0.25f, 0.5f, 12, 0.5f), PartPose.offsetAndRotation(0.8f, -3.2f, -2, 0.85f, -0.25f, 0.2f));
        PartDefinition rightCoxa = prothorax.addOrReplaceChild("right_coxa", CubeListBuilder.create()
                .texOffs(63, 51).mirror(false).addBox(-1, 0, -1, 2, 7, 2)
                .texOffs(30, 62).mirror(false).addBox(-1.4f, 0.5f, -1.4f, 2.8f, 1.6f, 2.8f), PartPose.offsetAndRotation(-2.6f, -14, -1.6f, -0.85f, 0.15f, 0.3f));
        PartDefinition leftCoxa = prothorax.addOrReplaceChild("left_coxa", CubeListBuilder.create()
                .texOffs(63, 51).mirror(true).addBox(-1, 0, -1, 2, 7, 2)
                .texOffs(30, 62).mirror(true).addBox(-1.4f, 0.5f, -1.4f, 2.8f, 1.6f, 2.8f), PartPose.offsetAndRotation(2.6f, -14, -1.6f, -0.85f, -0.15f, -0.3f));
        PartDefinition rightFemur = rightCoxa.addOrReplaceChild("right_femur", CubeListBuilder.create()
                .texOffs(58, 22).mirror(false).addBox(-1.4f, -0.2f, -1.4f, 2.8f, 11, 2.8f)
                .texOffs(33, 51).mirror(false).addBox(-0.5f, 1, -2.2f, 1, 9, 0.9f)
                .texOffs(13, 68).mirror(false).addBox(-0.3f, 2, -3.4f, 0.6f, 0.6f, 1.6f)
                .texOffs(13, 68).mirror(false).addBox(-0.3f, 4.4f, -3.6f, 0.6f, 0.6f, 1.8f)
                .texOffs(13, 68).mirror(false).addBox(-0.3f, 6.8f, -3.4f, 0.6f, 0.6f, 1.6f)
                .texOffs(13, 68).mirror(false).addBox(-0.3f, 9, -3, 0.6f, 0.6f, 1.2f), PartPose.offsetAndRotation(0, 7, 0, -2.55f, 0, 0));
        PartDefinition leftFemur = leftCoxa.addOrReplaceChild("left_femur", CubeListBuilder.create()
                .texOffs(58, 22).mirror(true).addBox(-1.4f, -0.2f, -1.4f, 2.8f, 11, 2.8f)
                .texOffs(33, 51).mirror(true).addBox(-0.5f, 1, -2.2f, 1, 9, 0.9f)
                .texOffs(13, 68).mirror(true).addBox(-0.3f, 2, -3.4f, 0.6f, 0.6f, 1.6f)
                .texOffs(13, 68).mirror(true).addBox(-0.3f, 4.4f, -3.6f, 0.6f, 0.6f, 1.8f)
                .texOffs(13, 68).mirror(true).addBox(-0.3f, 6.8f, -3.4f, 0.6f, 0.6f, 1.6f)
                .texOffs(13, 68).mirror(true).addBox(-0.3f, 9, -3, 0.6f, 0.6f, 1.2f), PartPose.offsetAndRotation(0, 7, 0, -2.55f, 0, 0));
        PartDefinition rightTibia = rightFemur.addOrReplaceChild("right_tibia", CubeListBuilder.create()
                .texOffs(75, 38).mirror(false).addBox(-0.9f, 0, -0.9f, 1.8f, 9, 1.8f)
                .texOffs(72, 51).mirror(false).addBox(-0.4f, 0.8f, -1.6f, 0.8f, 7.5f, 0.8f)
                .texOffs(43, 62).mirror(false).addBox(-0.55f, 8.6f, -1.9f, 1.1f, 1.8f, 1.9f), PartPose.offsetAndRotation(0, 10.8f, 0, 2.7f, 0, 0));
        PartDefinition leftTibia = leftFemur.addOrReplaceChild("left_tibia", CubeListBuilder.create()
                .texOffs(75, 38).mirror(true).addBox(-0.9f, 0, -0.9f, 1.8f, 9, 1.8f)
                .texOffs(72, 51).mirror(true).addBox(-0.4f, 0.8f, -1.6f, 0.8f, 7.5f, 0.8f)
                .texOffs(43, 62).mirror(true).addBox(-0.55f, 8.6f, -1.9f, 1.1f, 1.8f, 1.9f), PartPose.offsetAndRotation(0, 10.8f, 0, 2.7f, 0, 0));
        PartDefinition rightHip0 = waist.addOrReplaceChild("right_hip_0", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.4f, 1, -1.2f, 0, -0.55f, 0));
        PartDefinition leftHip0 = waist.addOrReplaceChild("left_hip_0", CubeListBuilder.create(), PartPose.offsetAndRotation(2.4f, 1, -1.2f, 0, 0.55f, 0));
        PartDefinition rightLeg0 = rightHip0.addOrReplaceChild("right_leg_0", CubeListBuilder.create()
                .texOffs(52, 62).mirror(false).addBox(-8, -0.7f, -0.7f, 8, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.5f));
        PartDefinition leftLeg0 = leftHip0.addOrReplaceChild("left_leg_0", CubeListBuilder.create()
                .texOffs(52, 62).mirror(true).addBox(0, -0.7f, -0.7f, 8, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.5f));
        PartDefinition rightShin0 = rightLeg0.addOrReplaceChild("right_shin_0", CubeListBuilder.create()
                .texOffs(73, 62).mirror(false).addBox(-21, -0.55f, -0.55f, 21, 1.1f, 1.1f)
                .texOffs(120, 62).mirror(false).addBox(-1.2f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-8, 0, 0, 0, 0, -1.75f));
        PartDefinition leftShin0 = leftLeg0.addOrReplaceChild("left_shin_0", CubeListBuilder.create()
                .texOffs(73, 62).mirror(true).addBox(0, -0.55f, -0.55f, 21, 1.1f, 1.1f)
                .texOffs(120, 62).mirror(true).addBox(-0.2f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(8, 0, 0, 0, 0, 1.75f));
        PartDefinition rightHip1 = waist.addOrReplaceChild("right_hip_1", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.4f, 1, 1.4f, 0, 0.6f, 0));
        PartDefinition leftHip1 = waist.addOrReplaceChild("left_hip_1", CubeListBuilder.create(), PartPose.offsetAndRotation(2.4f, 1, 1.4f, 0, -0.6f, 0));
        PartDefinition rightLeg1 = rightHip1.addOrReplaceChild("right_leg_1", CubeListBuilder.create()
                .texOffs(52, 62).mirror(false).addBox(-8, -0.7f, -0.7f, 8, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.5f));
        PartDefinition leftLeg1 = leftHip1.addOrReplaceChild("left_leg_1", CubeListBuilder.create()
                .texOffs(52, 62).mirror(true).addBox(0, -0.7f, -0.7f, 8, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.5f));
        PartDefinition rightShin1 = rightLeg1.addOrReplaceChild("right_shin_1", CubeListBuilder.create()
                .texOffs(73, 62).mirror(false).addBox(-21, -0.55f, -0.55f, 21, 1.1f, 1.1f)
                .texOffs(120, 62).mirror(false).addBox(-1.2f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(-8, 0, 0, 0, 0, -1.75f));
        PartDefinition leftShin1 = leftLeg1.addOrReplaceChild("left_shin_1", CubeListBuilder.create()
                .texOffs(73, 62).mirror(true).addBox(0, -0.55f, -0.55f, 21, 1.1f, 1.1f)
                .texOffs(120, 62).mirror(true).addBox(-0.2f, -1, -1, 1.4f, 2, 2), PartPose.offsetAndRotation(8, 0, 0, 0, 0, 1.75f));
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
                .texOffs(55, 56).mirror(false).addBox(-7, 3, -8, 14, 1.6f, 18), PartPose.offsetAndRotation(0, 12.5f, 0, 0, 0, 0));
        PartDefinition rightElytron = body.addOrReplaceChild("right_elytron", CubeListBuilder.create()
                .texOffs(0, 30).mirror(false).addBox(-9.6f, -1.8f, 0, 9.6f, 2.2f, 21.5f)
                .texOffs(65, 30).mirror(false).addBox(-10.1f, 0, 0.8f, 0.9f, 2.8f, 20)
                .texOffs(84, 110).mirror(false).addBox(-8.2f, -1.6f, 21.3f, 8, 1.8f, 1.6f)
                .texOffs(0, 56).mirror(false).addBox(-7.4f, -3.2f, 1.2f, 7.4f, 1.6f, 19)
                .texOffs(0, 78).mirror(false).addBox(-4.2f, -4.2f, 2.6f, 4.2f, 1.2f, 15.5f), PartPose.offsetAndRotation(0, -4.6f, -8.5f, 0.02f, 0, 0.04f));
        PartDefinition leftElytron = body.addOrReplaceChild("left_elytron", CubeListBuilder.create()
                .texOffs(0, 30).mirror(true).addBox(0, -1.8f, 0, 9.6f, 2.2f, 21.5f)
                .texOffs(65, 30).mirror(true).addBox(9.2f, 0, 0.8f, 0.9f, 2.8f, 20)
                .texOffs(84, 110).mirror(true).addBox(0.2f, -1.6f, 21.3f, 8, 1.8f, 1.6f)
                .texOffs(0, 56).mirror(true).addBox(0, -3.2f, 1.2f, 7.4f, 1.6f, 19)
                .texOffs(0, 78).mirror(true).addBox(0, -4.2f, 2.6f, 4.2f, 1.2f, 15.5f), PartPose.offsetAndRotation(0, -4.6f, -8.5f, 0.02f, 0, -0.04f));
        PartDefinition pronotum = body.addOrReplaceChild("pronotum", CubeListBuilder.create()
                .texOffs(70, 78).mirror(false).addBox(-9, -1.6f, -7, 18, 3, 9)
                .texOffs(33, 97).mirror(false).addBox(-6.5f, -2.8f, -6, 13, 1.4f, 7.4f)
                .texOffs(76, 97).mirror(false).addBox(-8.2f, -2.2f, -6.6f, 16.4f, 0.8f, 8.4f)
                .texOffs(0, 110).mirror(false).addBox(-4.2f, -3.6f, -5.2f, 8.4f, 1, 5.6f), PartPose.offsetAndRotation(0, -4.3f, -9.5f, 0.25f, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 97).mirror(false).addBox(-4.8f, -3, -5, 9.6f, 6, 5.2f)
                .texOffs(105, 110).mirror(false).addBox(-2.2f, 2.2f, -4.8f, 4.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, -11, 0.35f, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(31, 110).mirror(false).addBox(-1.3f, -1.6f, -1.5f, 2.2f, 3.2f, 3), PartPose.offsetAndRotation(-3.4f, -1.2f, -2.6f, 0, 0.2f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(31, 110).mirror(true).addBox(-0.9f, -1.6f, -1.5f, 2.2f, 3.2f, 3), PartPose.offsetAndRotation(3.4f, -1.2f, -2.6f, 0, -0.2f, 0));
        PartDefinition rightMandible = head.addOrReplaceChild("right_mandible", CubeListBuilder.create()
                .texOffs(120, 110).mirror(false).addBox(-1.2f, -0.6f, -1.8f, 2, 1.4f, 2), PartPose.offsetAndRotation(-1.6f, 2.2f, -4.2f, 0, 0.2f, 0.1f));
        PartDefinition leftMandible = head.addOrReplaceChild("left_mandible", CubeListBuilder.create()
                .texOffs(120, 110).mirror(true).addBox(-0.8f, -0.6f, -1.8f, 2, 1.4f, 2), PartPose.offsetAndRotation(1.6f, 2.2f, -4.2f, 0, -0.2f, -0.1f));
        PartDefinition rightAntenna = head.addOrReplaceChild("right_antenna", CubeListBuilder.create()
                .texOffs(43, 78).mirror(false).addBox(-0.3f, -0.3f, -12, 0.6f, 0.6f, 12), PartPose.offsetAndRotation(-2, -2, -4, 0.15f, 0.55f, 0));
        PartDefinition leftAntenna = head.addOrReplaceChild("left_antenna", CubeListBuilder.create()
                .texOffs(43, 78).mirror(true).addBox(-0.3f, -0.3f, -12, 0.6f, 0.6f, 12), PartPose.offsetAndRotation(2, -2, -4, 0.15f, -0.55f, 0));
        PartDefinition rightAntennaTip = rightAntenna.addOrReplaceChild("right_antenna_tip", CubeListBuilder.create()
                .texOffs(43, 78).mirror(false).addBox(-0.25f, -0.25f, -12, 0.5f, 0.5f, 12), PartPose.offsetAndRotation(0, 0, -12, -0.15f, 0.25f, 0));
        PartDefinition leftAntennaTip = leftAntenna.addOrReplaceChild("left_antenna_tip", CubeListBuilder.create()
                .texOffs(43, 78).mirror(true).addBox(-0.25f, -0.25f, -12, 0.5f, 0.5f, 12), PartPose.offsetAndRotation(0, 0, -12, -0.15f, -0.25f, 0));
        PartDefinition rightCercus = body.addOrReplaceChild("right_cercus", CubeListBuilder.create()
                .texOffs(44, 110).mirror(false).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 5), PartPose.offsetAndRotation(-3, 0, 11, 0.3f, -0.35f, 0));
        PartDefinition leftCercus = body.addOrReplaceChild("left_cercus", CubeListBuilder.create()
                .texOffs(44, 110).mirror(true).addBox(-0.6f, -0.6f, 0, 1.2f, 1.2f, 5), PartPose.offsetAndRotation(3, 0, 11, 0.3f, 0.35f, 0));
        PartDefinition rightHip0 = body.addOrReplaceChild("right_hip_0", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.5f, 2, -6, 0, -0.65f, 0));
        PartDefinition leftHip0 = body.addOrReplaceChild("left_hip_0", CubeListBuilder.create(), PartPose.offsetAndRotation(7.5f, 2, -6, 0, 0.65f, 0));
        PartDefinition rightLeg0 = rightHip0.addOrReplaceChild("right_leg_0", CubeListBuilder.create()
                .texOffs(59, 110).mirror(false).addBox(-9, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(40, 118).mirror(false).addBox(-6, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(40, 118).mirror(false).addBox(-3, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.32f));
        PartDefinition leftLeg0 = leftHip0.addOrReplaceChild("left_leg_0", CubeListBuilder.create()
                .texOffs(59, 110).mirror(true).addBox(0, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(40, 118).mirror(true).addBox(5.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(40, 118).mirror(true).addBox(2.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.32f));
        PartDefinition rightShin0 = rightLeg0.addOrReplaceChild("right_shin_0", CubeListBuilder.create()
                .texOffs(0, 118).mirror(false).addBox(-11, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(40, 118).mirror(false).addBox(-7, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(40, 118).mirror(false).addBox(-3.5f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(27, 118).mirror(false).addBox(-14, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(-9, 0, 0, 0, 0, -1.55f));
        PartDefinition leftShin0 = leftLeg0.addOrReplaceChild("left_shin_0", CubeListBuilder.create()
                .texOffs(0, 118).mirror(true).addBox(0, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(40, 118).mirror(true).addBox(6.3f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(40, 118).mirror(true).addBox(2.8f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(27, 118).mirror(true).addBox(10.6f, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(9, 0, 0, 0, 0, 1.55f));
        PartDefinition rightHip1 = body.addOrReplaceChild("right_hip_1", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.5f, 2, 1, 0, 0.08f, 0));
        PartDefinition leftHip1 = body.addOrReplaceChild("left_hip_1", CubeListBuilder.create(), PartPose.offsetAndRotation(7.5f, 2, 1, 0, -0.08f, 0));
        PartDefinition rightLeg1 = rightHip1.addOrReplaceChild("right_leg_1", CubeListBuilder.create()
                .texOffs(59, 110).mirror(false).addBox(-9, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(40, 118).mirror(false).addBox(-6, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(40, 118).mirror(false).addBox(-3, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.32f));
        PartDefinition leftLeg1 = leftHip1.addOrReplaceChild("left_leg_1", CubeListBuilder.create()
                .texOffs(59, 110).mirror(true).addBox(0, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(40, 118).mirror(true).addBox(5.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(40, 118).mirror(true).addBox(2.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.32f));
        PartDefinition rightShin1 = rightLeg1.addOrReplaceChild("right_shin_1", CubeListBuilder.create()
                .texOffs(0, 118).mirror(false).addBox(-11, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(40, 118).mirror(false).addBox(-7, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(40, 118).mirror(false).addBox(-3.5f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(27, 118).mirror(false).addBox(-14, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(-9, 0, 0, 0, 0, -1.55f));
        PartDefinition leftShin1 = leftLeg1.addOrReplaceChild("left_shin_1", CubeListBuilder.create()
                .texOffs(0, 118).mirror(true).addBox(0, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(40, 118).mirror(true).addBox(6.3f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(40, 118).mirror(true).addBox(2.8f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(27, 118).mirror(true).addBox(10.6f, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(9, 0, 0, 0, 0, 1.55f));
        PartDefinition rightHip2 = body.addOrReplaceChild("right_hip_2", CubeListBuilder.create(), PartPose.offsetAndRotation(-7.5f, 2, 8, 0, 0.8f, 0));
        PartDefinition leftHip2 = body.addOrReplaceChild("left_hip_2", CubeListBuilder.create(), PartPose.offsetAndRotation(7.5f, 2, 8, 0, -0.8f, 0));
        PartDefinition rightLeg2 = rightHip2.addOrReplaceChild("right_leg_2", CubeListBuilder.create()
                .texOffs(59, 110).mirror(false).addBox(-9, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(40, 118).mirror(false).addBox(-6, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(40, 118).mirror(false).addBox(-3, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0.32f));
        PartDefinition leftLeg2 = leftHip2.addOrReplaceChild("left_leg_2", CubeListBuilder.create()
                .texOffs(59, 110).mirror(true).addBox(0, -1.3f, -1.3f, 9, 2.6f, 2.6f)
                .texOffs(40, 118).mirror(true).addBox(5.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f)
                .texOffs(40, 118).mirror(true).addBox(2.2f, -2.4f, -0.4f, 0.8f, 1.2f, 0.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, -0.32f));
        PartDefinition rightShin2 = rightLeg2.addOrReplaceChild("right_shin_2", CubeListBuilder.create()
                .texOffs(0, 118).mirror(false).addBox(-11, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(40, 118).mirror(false).addBox(-7, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(40, 118).mirror(false).addBox(-3.5f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(27, 118).mirror(false).addBox(-14, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(-9, 0, 0, 0, 0, -1.55f));
        PartDefinition leftShin2 = leftLeg2.addOrReplaceChild("left_shin_2", CubeListBuilder.create()
                .texOffs(0, 118).mirror(true).addBox(0, -0.9f, -0.9f, 11, 1.8f, 1.8f)
                .texOffs(40, 118).mirror(true).addBox(6.3f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(40, 118).mirror(true).addBox(2.8f, -1.8f, -0.35f, 0.7f, 1.2f, 0.7f)
                .texOffs(27, 118).mirror(true).addBox(10.6f, -0.6f, -0.6f, 3.4f, 1.2f, 1.2f), PartPose.offsetAndRotation(9, 0, 0, 0, 0, 1.55f));
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
                .texOffs(95, 0).mirror(false).addBox(-3, -11, -1.9f, 6, 11, 3.8f)
                .texOffs(29, 49).mirror(false).addBox(-2.7f, -9.4f, -2.3f, 5.4f, 2.8f, 0.6f)
                .texOffs(51, 39).mirror(false).addBox(-3.6f, -11.2f, -2.1f, 7.2f, 1.8f, 4.2f)
                .texOffs(78, 39).mirror(false).addBox(-3.3f, -11.7f, -2.25f, 6.6f, 1.8f, 4.5f)
                .texOffs(7, 39).mirror(false).addBox(-3.4f, -3.6f, -2.35f, 6.8f, 2.2f, 4.7f)
                .texOffs(44, 49).mirror(false).addBox(-1.1f, -3.8f, -3.05f, 2.2f, 2.6f, 0.9f), PartPose.offsetAndRotation(0, 8.5f, 0, 0, 0, 0));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(116, 0).mirror(false).addBox(-3.8f, -1.4f, -2.6f, 7.6f, 8.4f, 5.2f)
                .texOffs(185, 0).mirror(false).addBox(-4.8f, 6.6f, -3.4f, 9.6f, 6.2f, 6.8f)
                .texOffs(27, 25).mirror(false).addBox(-6.2f, 12.6f, -4.4f, 12.4f, 2.9f, 8.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition apron = skirt.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(82, 0).mirror(false).addBox(-2.2f, 0, -0.4f, 4.4f, 16.4f, 0.4f), PartPose.offsetAndRotation(0, -1.2f, -3, -0.1f, 0, 0));
        PartDefinition train = skirt.addOrReplaceChild("train", CubeListBuilder.create()
                .texOffs(204, 25).mirror(false).addBox(-5, 0, 0, 10, 7.6f, 0.6f), PartPose.offsetAndRotation(0, 8.5f, 4.2f, 0.42f, 0, 0));
        PartDefinition rightCoat = body.addOrReplaceChild("right_coat", CubeListBuilder.create()
                .texOffs(72, 25).mirror(false).addBox(-1.6f, 0, -0.35f, 2.4f, 10.4f, 0.5f), PartPose.offsetAndRotation(-2.6f, -10.6f, -2.2f, -0.04f, 0.06f, 0.1f));
        PartDefinition leftCoat = body.addOrReplaceChild("left_coat", CubeListBuilder.create()
                .texOffs(72, 25).mirror(true).addBox(-0.8f, 0, -0.35f, 2.4f, 10.4f, 0.5f), PartPose.offsetAndRotation(2.6f, -10.6f, -2.2f, -0.04f, -0.06f, -0.1f));
        PartDefinition rightCoatDrape = rightCoat.addOrReplaceChild("right_coat_drape", CubeListBuilder.create()
                .texOffs(131, 25).mirror(false).addBox(-2, 0, -0.35f, 3, 9.6f, 0.5f), PartPose.offsetAndRotation(-0.2f, 10.2f, 0, -0.14f, 0.42f, 0.06f));
        PartDefinition leftCoatDrape = leftCoat.addOrReplaceChild("left_coat_drape", CubeListBuilder.create()
                .texOffs(131, 25).mirror(true).addBox(-1, 0, -0.35f, 3, 9.6f, 0.5f), PartPose.offsetAndRotation(0.2f, 10.2f, 0, -0.14f, -0.42f, -0.06f));
        PartDefinition rightCoatLapel = rightCoatDrape.addOrReplaceChild("right_coat_lapel", CubeListBuilder.create()
                .texOffs(163, 25).mirror(false).addBox(-0.1f, 0, -0.3f, 1.3f, 9, 0.4f), PartPose.offsetAndRotation(0.9f, 0.2f, 0, 0, -0.9f, 0));
        PartDefinition leftCoatLapel = leftCoatDrape.addOrReplaceChild("left_coat_lapel", CubeListBuilder.create()
                .texOffs(163, 25).mirror(true).addBox(-1.2f, 0, -0.3f, 1.3f, 9, 0.4f), PartPose.offsetAndRotation(-0.9f, 0.2f, 0, 0, 0.9f, 0));
        PartDefinition rightCoatSide = body.addOrReplaceChild("right_coat_side", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5f, 0, -2.2f, 0.5f, 19, 4.4f), PartPose.offsetAndRotation(-3.5f, -10.4f, 0, 0, 0, 0.12f));
        PartDefinition leftCoatSide = body.addOrReplaceChild("left_coat_side", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(0, 0, -2.2f, 0.5f, 19, 4.4f), PartPose.offsetAndRotation(3.5f, -10.4f, 0, 0, 0, -0.12f));
        PartDefinition coatBack = body.addOrReplaceChild("coat_back", CubeListBuilder.create()
                .texOffs(13, 0).mirror(false).addBox(-3.8f, 0, 0, 7.6f, 19.8f, 0.5f), PartPose.offsetAndRotation(0, -10.8f, 2, 0.1f, 0, 0));
        PartDefinition rightSashEnd = body.addOrReplaceChild("right_sash_end", CubeListBuilder.create()
                .texOffs(220, 0).mirror(false).addBox(-0.6f, 0, -0.2f, 1.2f, 13, 0.4f), PartPose.offsetAndRotation(-1.6f, -2.4f, -2.9f, -0.06f, 0, 0.06f));
        PartDefinition leftSashEnd = body.addOrReplaceChild("left_sash_end", CubeListBuilder.create()
                .texOffs(220, 0).mirror(true).addBox(-0.6f, 0, -0.2f, 1.2f, 13, 0.4f), PartPose.offsetAndRotation(1.6f, -2.4f, -2.9f, -0.06f, 0, -0.06f));
        PartDefinition gauze = body.addOrReplaceChild("gauze", CubeListBuilder.create()
                .texOffs(55, 0).mirror(false).addBox(-3.6f, 0, -2.5f, 7.2f, 13.5f, 5), PartPose.offsetAndRotation(0, -10.8f, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(53, 49).mirror(false).addBox(-0.9f, -1.6f, -0.9f, 1.8f, 1.8f, 1.8f)
                .texOffs(170, 25).mirror(false).addBox(-2.2f, -6.2f, -2.3f, 4.4f, 5, 4.4f)
                .texOffs(227, 25).mirror(false).addBox(-2.5f, -6.6f, -0.4f, 5, 5.8f, 2.9f)
                .texOffs(103, 39).mirror(false).addBox(-2.5f, -6.8f, -2.5f, 5, 1.3f, 4.8f)
                .texOffs(154, 49).mirror(false).addBox(-2.35f, -6.25f, -2.55f, 4.7f, 0.6f, 0.4f), PartPose.offsetAndRotation(0, -11.5f, -0.1f, 0, 0, 0));
        PartDefinition rightLock = head.addOrReplaceChild("right_lock", CubeListBuilder.create()
                .texOffs(244, 25).mirror(false).addBox(-0.45f, 0, -0.45f, 0.8f, 6.5f, 1.1f), PartPose.offsetAndRotation(-2.3f, -5.8f, -1.5f, 0, 0, 0.06f));
        PartDefinition leftLock = head.addOrReplaceChild("left_lock", CubeListBuilder.create()
                .texOffs(244, 25).mirror(true).addBox(-0.35f, 0, -0.45f, 0.8f, 6.5f, 1.1f), PartPose.offsetAndRotation(2.3f, -5.8f, -1.5f, 0, 0, -0.06f));
        PartDefinition rightEarring = head.addOrReplaceChild("right_earring", CubeListBuilder.create()
                .texOffs(112, 49).mirror(false).addBox(-0.15f, 0, -0.15f, 0.3f, 1.2f, 0.3f)
                .texOffs(117, 49).mirror(false).addBox(-0.45f, 1.2f, -0.45f, 0.9f, 1.4f, 0.9f), PartPose.offsetAndRotation(-2.3f, -2.6f, -0.6f, 0, 0, 0.05f));
        PartDefinition leftEarring = head.addOrReplaceChild("left_earring", CubeListBuilder.create()
                .texOffs(112, 49).mirror(true).addBox(-0.15f, 0, -0.15f, 0.3f, 1.2f, 0.3f)
                .texOffs(117, 49).mirror(true).addBox(-0.45f, 1.2f, -0.45f, 0.9f, 1.4f, 0.9f), PartPose.offsetAndRotation(2.3f, -2.6f, -0.6f, 0, 0, -0.05f));
        PartDefinition bun = head.addOrReplaceChild("bun", CubeListBuilder.create()
                .texOffs(32, 39).mirror(false).addBox(-2.1f, -3.6f, -1.6f, 4.2f, 3.8f, 3.4f)
                .texOffs(157, 39).mirror(false).addBox(-1.4f, -5.6f, -1.1f, 2.8f, 2.2f, 2.4f)
                .texOffs(189, 39).mirror(false).addBox(-2.3f, -1.4f, -1.9f, 4.6f, 0.9f, 3.9f), PartPose.offsetAndRotation(0, -6.7f, 0.6f, -0.15f, 0, 0));
        PartDefinition rightLoop = bun.addOrReplaceChild("right_loop", CubeListBuilder.create()
                .texOffs(124, 39).mirror(false).addBox(-0.9f, -4.6f, -0.9f, 1.8f, 4.6f, 1.8f), PartPose.offsetAndRotation(-1.8f, -3.4f, 0.1f, 0, 0, -0.55f));
        PartDefinition leftLoop = bun.addOrReplaceChild("left_loop", CubeListBuilder.create()
                .texOffs(124, 39).mirror(true).addBox(-0.9f, -4.6f, -0.9f, 1.8f, 4.6f, 1.8f), PartPose.offsetAndRotation(1.8f, -3.4f, 0.1f, 0, 0, 0.55f));
        PartDefinition rightLoopTie = rightLoop.addOrReplaceChild("right_loop_tie", CubeListBuilder.create()
                .texOffs(122, 49).mirror(false).addBox(-1, -0.6f, -1, 2, 0.8f, 2), PartPose.offsetAndRotation(0, -4.4f, 0, 0, 0, 0));
        PartDefinition leftLoopTie = leftLoop.addOrReplaceChild("left_loop_tie", CubeListBuilder.create()
                .texOffs(122, 49).mirror(true).addBox(-1, -0.6f, -1, 2, 0.8f, 2), PartPose.offsetAndRotation(0, -4.4f, 0, 0, 0, 0));
        PartDefinition phoenix = bun.addOrReplaceChild("phoenix", CubeListBuilder.create()
                .texOffs(208, 39).mirror(false).addBox(-0.7f, -1.6f, -1.6f, 1.4f, 1.6f, 2.6f)
                .texOffs(131, 49).mirror(false).addBox(-0.45f, -2.6f, -2.2f, 0.9f, 1.4f, 1)
                .texOffs(167, 49).mirror(false).addBox(-0.3f, -2, -2.9f, 0.6f, 0.5f, 0.8f), PartPose.offsetAndRotation(0, -5.6f, -0.4f, -0.1f, 0, 0));
        PartDefinition phoenixTail = phoenix.addOrReplaceChild("phoenix_tail", CubeListBuilder.create()
                .texOffs(170, 39).mirror(false).addBox(-1.8f, -0.2f, 0, 3.6f, 0.3f, 4.2f), PartPose.offsetAndRotation(0, -1.2f, 0.9f, 0.6f, 0, 0));
        PartDefinition rightPhoenixWing = phoenix.addOrReplaceChild("right_phoenix_wing", CubeListBuilder.create()
                .texOffs(62, 49).mirror(false).addBox(-2.6f, -0.15f, -1.2f, 2.6f, 0.3f, 2.4f), PartPose.offsetAndRotation(-0.6f, -1, -0.4f, 0, 0.3f, 0.7f));
        PartDefinition leftPhoenixWing = phoenix.addOrReplaceChild("left_phoenix_wing", CubeListBuilder.create()
                .texOffs(62, 49).mirror(true).addBox(0, -0.15f, -1.2f, 2.6f, 0.3f, 2.4f), PartPose.offsetAndRotation(0.6f, -1, -0.4f, 0, -0.3f, -0.7f));
        PartDefinition phoenixDangle = phoenix.addOrReplaceChild("phoenix_dangle", CubeListBuilder.create()
                .texOffs(75, 49).mirror(false).addBox(-0.12f, 0, -0.12f, 0.24f, 2.6f, 0.24f)
                .texOffs(172, 49).mirror(false).addBox(-0.4f, 2.6f, -0.4f, 0.8f, 0.8f, 0.8f), PartPose.offsetAndRotation(0, -2, -2.8f, 0.3f, 0, 0));
        PartDefinition rightFlower = bun.addOrReplaceChild("right_flower", CubeListBuilder.create()
                .texOffs(80, 49).mirror(false).addBox(-0.9f, -0.9f, -0.9f, 1.8f, 1.8f, 1.8f)
                .texOffs(177, 49).mirror(false).addBox(-1.3f, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f)
                .texOffs(89, 49).mirror(false).addBox(-0.4f, -1.3f, -0.4f, 0.8f, 2.6f, 0.8f), PartPose.offsetAndRotation(-2, -1.6f, -1, 0.2f, 0.4f, 0.3f));
        PartDefinition leftFlower = bun.addOrReplaceChild("left_flower", CubeListBuilder.create()
                .texOffs(80, 49).mirror(true).addBox(-0.9f, -0.9f, -0.9f, 1.8f, 1.8f, 1.8f)
                .texOffs(177, 49).mirror(true).addBox(-1.3f, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f)
                .texOffs(89, 49).mirror(true).addBox(-0.4f, -1.3f, -0.4f, 0.8f, 2.6f, 0.8f), PartPose.offsetAndRotation(2, -1.6f, -1, 0.2f, -0.4f, -0.3f));
        PartDefinition rightPin = bun.addOrReplaceChild("right_pin", CubeListBuilder.create()
                .texOffs(186, 49).mirror(false).addBox(-4.2f, -0.25f, -0.25f, 4.2f, 0.5f, 0.5f)
                .texOffs(94, 49).mirror(false).addBox(-5.2f, -0.8f, -0.8f, 1.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(-1.6f, -2.8f, -1.2f, 0.3f, 0.5f, 0.4f));
        PartDefinition leftPin = bun.addOrReplaceChild("left_pin", CubeListBuilder.create()
                .texOffs(186, 49).mirror(true).addBox(0, -0.25f, -0.25f, 4.2f, 0.5f, 0.5f)
                .texOffs(94, 49).mirror(true).addBox(3.6f, -0.8f, -0.8f, 1.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(1.6f, -2.8f, -1.2f, 0.3f, -0.5f, -0.4f));
        PartDefinition rightDangle = rightPin.addOrReplaceChild("right_dangle", CubeListBuilder.create()
                .texOffs(219, 39).mirror(false).addBox(-0.12f, 0, -0.12f, 0.24f, 3.4f, 0.24f)
                .texOffs(199, 49).mirror(false).addBox(-0.45f, 3.3f, -0.45f, 0.9f, 0.9f, 0.9f)
                .texOffs(112, 49).mirror(false).addBox(-0.12f, 4.1f, -0.12f, 0.24f, 1.6f, 0.24f)
                .texOffs(172, 49).mirror(false).addBox(-0.35f, 5.6f, -0.35f, 0.7f, 1, 0.7f), PartPose.offsetAndRotation(-4.6f, 0.6f, 0, -0.3f, 0, -0.4f));
        PartDefinition leftDangle = leftPin.addOrReplaceChild("left_dangle", CubeListBuilder.create()
                .texOffs(219, 39).mirror(true).addBox(-0.12f, 0, -0.12f, 0.24f, 3.4f, 0.24f)
                .texOffs(199, 49).mirror(true).addBox(-0.45f, 3.3f, -0.45f, 0.9f, 0.9f, 0.9f)
                .texOffs(112, 49).mirror(true).addBox(-0.12f, 4.1f, -0.12f, 0.24f, 1.6f, 0.24f)
                .texOffs(172, 49).mirror(true).addBox(-0.35f, 5.6f, -0.35f, 0.7f, 1, 0.7f), PartPose.offsetAndRotation(4.6f, 0.6f, 0, -0.3f, 0, 0.4f));
        PartDefinition rightDangle2 = rightPin.addOrReplaceChild("right_dangle_2", CubeListBuilder.create()
                .texOffs(75, 49).mirror(false).addBox(-0.12f, 0, -0.12f, 0.24f, 2.2f, 0.24f)
                .texOffs(172, 49).mirror(false).addBox(-0.4f, 2.1f, -0.4f, 0.8f, 0.8f, 0.8f), PartPose.offsetAndRotation(-3.6f, 0.6f, 0, -0.3f, 0, -0.4f));
        PartDefinition leftDangle2 = leftPin.addOrReplaceChild("left_dangle_2", CubeListBuilder.create()
                .texOffs(75, 49).mirror(true).addBox(-0.12f, 0, -0.12f, 0.24f, 2.2f, 0.24f)
                .texOffs(172, 49).mirror(true).addBox(-0.4f, 2.1f, -0.4f, 0.8f, 0.8f, 0.8f), PartPose.offsetAndRotation(3.6f, 0.6f, 0, -0.3f, 0, 0.4f));
        PartDefinition comb = bun.addOrReplaceChild("comb", CubeListBuilder.create()
                .texOffs(136, 49).mirror(false).addBox(-1.8f, -1.4f, -0.3f, 3.6f, 1.6f, 0.5f), PartPose.offsetAndRotation(0, -1.8f, -1.9f, 0.2f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(191, 25).mirror(false).addBox(-1.4f, -0.7f, -1.5f, 2.4f, 6.2f, 3), PartPose.offsetAndRotation(-3.6f, -10.2f, 0, 0.12f, 0, 0.14f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(191, 25).mirror(true).addBox(-1, -0.7f, -1.5f, 2.4f, 6.2f, 3), PartPose.offsetAndRotation(3.6f, -10.2f, 0, 0.12f, 0, -0.14f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(133, 39).mirror(false).addBox(-1.05f, 0, -1.05f, 2, 4.4f, 2)
                .texOffs(224, 39).mirror(false).addBox(-0.85f, 4.2f, -0.9f, 1.7f, 2.2f, 1.8f), PartPose.offsetAndRotation(-0.3f, 5.4f, 0, -0.55f, -0.2f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(133, 39).mirror(true).addBox(-0.95f, 0, -1.05f, 2, 4.4f, 2)
                .texOffs(224, 39).mirror(true).addBox(-0.85f, 4.2f, -0.9f, 1.7f, 2.2f, 1.8f), PartPose.offsetAndRotation(0.3f, 5.4f, 0, -0.55f, 0.2f, 0));
        PartDefinition rightSleeve = rightForearm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(32, 0).mirror(false).addBox(-2, -0.6f, -1.2f, 4, 13.5f, 6.2f), PartPose.offsetAndRotation(0, 0.2f, 0.6f, 0.48f, 0.1f, -0.02f));
        PartDefinition leftSleeve = leftForearm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(32, 0).mirror(true).addBox(-2, -0.6f, -1.2f, 4, 13.5f, 6.2f), PartPose.offsetAndRotation(0, 0.2f, 0.6f, 0.48f, -0.1f, 0.02f));
        PartDefinition rightStole = rightForearm.addOrReplaceChild("right_stole", CubeListBuilder.create()
                .texOffs(145, 0).mirror(false).addBox(-0.2f, 0, -1.1f, 0.4f, 12, 2.2f), PartPose.offsetAndRotation(-0.9f, 2.6f, 0.8f, 0.4f, 0, 0.15f));
        PartDefinition leftStole = leftForearm.addOrReplaceChild("left_stole", CubeListBuilder.create()
                .texOffs(145, 0).mirror(true).addBox(-0.2f, 0, -1.1f, 0.4f, 12, 2.2f), PartPose.offsetAndRotation(0.9f, 2.6f, 0.8f, 0.4f, 0, -0.15f));
        PartDefinition rightStoleTail = rightStole.addOrReplaceChild("right_stole_tail", CubeListBuilder.create()
                .texOffs(0, 39).mirror(false).addBox(-0.2f, 0, -1, 0.4f, 7, 2), PartPose.offsetAndRotation(0, 11.8f, 0, -0.45f, 0, 0.1f));
        PartDefinition leftStoleTail = leftStole.addOrReplaceChild("left_stole_tail", CubeListBuilder.create()
                .texOffs(0, 39).mirror(true).addBox(-0.2f, 0, -1, 0.4f, 7, 2), PartPose.offsetAndRotation(0, 11.8f, 0, -0.45f, 0, -0.1f));
        PartDefinition stoleBack = body.addOrReplaceChild("stole_back", CubeListBuilder.create()
                .texOffs(0, 49).mirror(false).addBox(-6.4f, -0.2f, -0.2f, 12.8f, 3.4f, 0.4f), PartPose.offsetAndRotation(0, -10.4f, 2.6f, 0.18f, 0, 0));
        PartDefinition silkStream = body.addOrReplaceChild("silk_stream", CubeListBuilder.create()
                .texOffs(154, 0).mirror(false).addBox(-0.45f, -0.45f, -14, 0.9f, 0.9f, 14), PartPose.offsetAndRotation(0, -2.5f, -3.4f, 0, 0, 0));
        PartDefinition legsMount = body.addOrReplaceChild("legs_mount", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -9.2f, 1.8f, 0, 0, 0));
        PartDefinition rightSleg0 = legsMount.addOrReplaceChild("right_sleg_0", CubeListBuilder.create()
                .texOffs(227, 0).mirror(false).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 10.5f), PartPose.offsetAndRotation(-0.9f, -0.6f, 0, 1.2f, -0.45f, 0));
        PartDefinition leftSleg0 = legsMount.addOrReplaceChild("left_sleg_0", CubeListBuilder.create()
                .texOffs(227, 0).mirror(true).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 10.5f), PartPose.offsetAndRotation(0.9f, -0.6f, 0, 1.2f, 0.45f, 0));
        PartDefinition rightSlegTip0 = rightSleg0.addOrReplaceChild("right_sleg_tip_0", CubeListBuilder.create()
                .texOffs(0, 25).mirror(false).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 10.5f)
                .texOffs(103, 49).mirror(false).addBox(-0.85f, -0.85f, -0.6f, 1.7f, 1.7f, 1.7f), PartPose.offsetAndRotation(0, 0, 10.2f, -1.45f, 0, 0));
        PartDefinition leftSlegTip0 = leftSleg0.addOrReplaceChild("left_sleg_tip_0", CubeListBuilder.create()
                .texOffs(0, 25).mirror(true).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 10.5f)
                .texOffs(103, 49).mirror(true).addBox(-0.85f, -0.85f, -0.6f, 1.7f, 1.7f, 1.7f), PartPose.offsetAndRotation(0, 0, 10.2f, -1.45f, 0, 0));
        PartDefinition rightSlegClaw0 = rightSlegTip0.addOrReplaceChild("right_sleg_claw_0", CubeListBuilder.create()
                .texOffs(142, 39).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 6)
                .texOffs(103, 49).mirror(false).addBox(-0.6f, -0.6f, -0.4f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 0, 10.3f, -0.55f, 0, 0));
        PartDefinition leftSlegClaw0 = leftSlegTip0.addOrReplaceChild("left_sleg_claw_0", CubeListBuilder.create()
                .texOffs(142, 39).mirror(true).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 6)
                .texOffs(103, 49).mirror(true).addBox(-0.6f, -0.6f, -0.4f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 0, 10.3f, -0.55f, 0, 0));
        PartDefinition rightSlegHook0 = rightSlegClaw0.addOrReplaceChild("right_sleg_hook_0", CubeListBuilder.create()
                .texOffs(147, 49).mirror(false).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 2), PartPose.offsetAndRotation(0, 0, 5.8f, -0.7f, 0, 0));
        PartDefinition leftSlegHook0 = leftSlegClaw0.addOrReplaceChild("left_sleg_hook_0", CubeListBuilder.create()
                .texOffs(147, 49).mirror(true).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 2), PartPose.offsetAndRotation(0, 0, 5.8f, -0.7f, 0, 0));
        PartDefinition rightSleg1 = legsMount.addOrReplaceChild("right_sleg_1", CubeListBuilder.create()
                .texOffs(81, 25).mirror(false).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 10), PartPose.offsetAndRotation(-0.9f, 0.9f, 0, 0.88f, -0.77f, 0));
        PartDefinition leftSleg1 = legsMount.addOrReplaceChild("left_sleg_1", CubeListBuilder.create()
                .texOffs(81, 25).mirror(true).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 10), PartPose.offsetAndRotation(0.9f, 0.9f, 0, 0.88f, 0.77f, 0));
        PartDefinition rightSlegTip1 = rightSleg1.addOrReplaceChild("right_sleg_tip_1", CubeListBuilder.create()
                .texOffs(0, 25).mirror(false).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 10.1f)
                .texOffs(103, 49).mirror(false).addBox(-0.85f, -0.85f, -0.6f, 1.7f, 1.7f, 1.7f), PartPose.offsetAndRotation(0, 0, 9.7f, -1.33f, 0, 0));
        PartDefinition leftSlegTip1 = leftSleg1.addOrReplaceChild("left_sleg_tip_1", CubeListBuilder.create()
                .texOffs(0, 25).mirror(true).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 10.1f)
                .texOffs(103, 49).mirror(true).addBox(-0.85f, -0.85f, -0.6f, 1.7f, 1.7f, 1.7f), PartPose.offsetAndRotation(0, 0, 9.7f, -1.33f, 0, 0));
        PartDefinition rightSlegClaw1 = rightSlegTip1.addOrReplaceChild("right_sleg_claw_1", CubeListBuilder.create()
                .texOffs(142, 39).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 5.7f)
                .texOffs(103, 49).mirror(false).addBox(-0.6f, -0.6f, -0.4f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 0, 9.9f, -0.55f, 0, 0));
        PartDefinition leftSlegClaw1 = leftSlegTip1.addOrReplaceChild("left_sleg_claw_1", CubeListBuilder.create()
                .texOffs(142, 39).mirror(true).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 5.7f)
                .texOffs(103, 49).mirror(true).addBox(-0.6f, -0.6f, -0.4f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 0, 9.9f, -0.55f, 0, 0));
        PartDefinition rightSlegHook1 = rightSlegClaw1.addOrReplaceChild("right_sleg_hook_1", CubeListBuilder.create()
                .texOffs(147, 49).mirror(false).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 2), PartPose.offsetAndRotation(0, 0, 5.5f, -0.7f, 0, 0));
        PartDefinition leftSlegHook1 = leftSlegClaw1.addOrReplaceChild("left_sleg_hook_1", CubeListBuilder.create()
                .texOffs(147, 49).mirror(true).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 2), PartPose.offsetAndRotation(0, 0, 5.5f, -0.7f, 0, 0));
        PartDefinition rightSleg2 = legsMount.addOrReplaceChild("right_sleg_2", CubeListBuilder.create()
                .texOffs(81, 25).mirror(false).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 9.5f), PartPose.offsetAndRotation(-0.9f, 2.4f, 0, 0.56f, -1.09f, 0));
        PartDefinition leftSleg2 = legsMount.addOrReplaceChild("left_sleg_2", CubeListBuilder.create()
                .texOffs(81, 25).mirror(true).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 9.5f), PartPose.offsetAndRotation(0.9f, 2.4f, 0, 0.56f, 1.09f, 0));
        PartDefinition rightSlegTip2 = rightSleg2.addOrReplaceChild("right_sleg_tip_2", CubeListBuilder.create()
                .texOffs(106, 25).mirror(false).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 9.7f)
                .texOffs(103, 49).mirror(false).addBox(-0.85f, -0.85f, -0.6f, 1.7f, 1.7f, 1.7f), PartPose.offsetAndRotation(0, 0, 9.2f, -1.21f, 0, 0));
        PartDefinition leftSlegTip2 = leftSleg2.addOrReplaceChild("left_sleg_tip_2", CubeListBuilder.create()
                .texOffs(106, 25).mirror(true).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 9.7f)
                .texOffs(103, 49).mirror(true).addBox(-0.85f, -0.85f, -0.6f, 1.7f, 1.7f, 1.7f), PartPose.offsetAndRotation(0, 0, 9.2f, -1.21f, 0, 0));
        PartDefinition rightSlegClaw2 = rightSlegTip2.addOrReplaceChild("right_sleg_claw_2", CubeListBuilder.create()
                .texOffs(142, 39).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 5.4f)
                .texOffs(103, 49).mirror(false).addBox(-0.6f, -0.6f, -0.4f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 0, 9.5f, -0.55f, 0, 0));
        PartDefinition leftSlegClaw2 = leftSlegTip2.addOrReplaceChild("left_sleg_claw_2", CubeListBuilder.create()
                .texOffs(142, 39).mirror(true).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 5.4f)
                .texOffs(103, 49).mirror(true).addBox(-0.6f, -0.6f, -0.4f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 0, 9.5f, -0.55f, 0, 0));
        PartDefinition rightSlegHook2 = rightSlegClaw2.addOrReplaceChild("right_sleg_hook_2", CubeListBuilder.create()
                .texOffs(147, 49).mirror(false).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 2), PartPose.offsetAndRotation(0, 0, 5.2f, -0.7f, 0, 0));
        PartDefinition leftSlegHook2 = leftSlegClaw2.addOrReplaceChild("left_sleg_hook_2", CubeListBuilder.create()
                .texOffs(147, 49).mirror(true).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 2), PartPose.offsetAndRotation(0, 0, 5.2f, -0.7f, 0, 0));
        PartDefinition rightSleg3 = legsMount.addOrReplaceChild("right_sleg_3", CubeListBuilder.create()
                .texOffs(140, 25).mirror(false).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 9), PartPose.offsetAndRotation(-0.9f, 3.9f, 0, 0.24f, -1.41f, 0));
        PartDefinition leftSleg3 = legsMount.addOrReplaceChild("left_sleg_3", CubeListBuilder.create()
                .texOffs(140, 25).mirror(true).addBox(-0.75f, -0.75f, 0, 1.5f, 1.5f, 9), PartPose.offsetAndRotation(0.9f, 3.9f, 0, 0.24f, 1.41f, 0));
        PartDefinition rightSlegTip3 = rightSleg3.addOrReplaceChild("right_sleg_tip_3", CubeListBuilder.create()
                .texOffs(106, 25).mirror(false).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 9.3f)
                .texOffs(103, 49).mirror(false).addBox(-0.85f, -0.85f, -0.6f, 1.7f, 1.7f, 1.7f), PartPose.offsetAndRotation(0, 0, 8.7f, -1.09f, 0, 0));
        PartDefinition leftSlegTip3 = leftSleg3.addOrReplaceChild("left_sleg_tip_3", CubeListBuilder.create()
                .texOffs(106, 25).mirror(true).addBox(-0.55f, -0.55f, 0, 1.1f, 1.1f, 9.3f)
                .texOffs(103, 49).mirror(true).addBox(-0.85f, -0.85f, -0.6f, 1.7f, 1.7f, 1.7f), PartPose.offsetAndRotation(0, 0, 8.7f, -1.09f, 0, 0));
        PartDefinition rightSlegClaw3 = rightSlegTip3.addOrReplaceChild("right_sleg_claw_3", CubeListBuilder.create()
                .texOffs(142, 39).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 5.1f)
                .texOffs(103, 49).mirror(false).addBox(-0.6f, -0.6f, -0.4f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 0, 9.1f, -0.55f, 0, 0));
        PartDefinition leftSlegClaw3 = leftSlegTip3.addOrReplaceChild("left_sleg_claw_3", CubeListBuilder.create()
                .texOffs(142, 39).mirror(true).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 5.1f)
                .texOffs(103, 49).mirror(true).addBox(-0.6f, -0.6f, -0.4f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 0, 9.1f, -0.55f, 0, 0));
        PartDefinition rightSlegHook3 = rightSlegClaw3.addOrReplaceChild("right_sleg_hook_3", CubeListBuilder.create()
                .texOffs(147, 49).mirror(false).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 2), PartPose.offsetAndRotation(0, 0, 4.9f, -0.7f, 0, 0));
        PartDefinition leftSlegHook3 = leftSlegClaw3.addOrReplaceChild("left_sleg_hook_3", CubeListBuilder.create()
                .texOffs(147, 49).mirror(true).addBox(-0.25f, -0.25f, 0, 0.5f, 0.5f, 2), PartPose.offsetAndRotation(0, 0, 4.9f, -0.7f, 0, 0));
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
        public static final float SIZE = 1.08f;
        private final ModelPart body, skirt, train, apron, head, bun, rightArm, leftArm, rightForearm, leftForearm, rightSleeve, leftSleeve,
                rightStole, leftStole, rightTail, leftTail, silk, gauze, rightCoat, leftCoat, coatBack, rightDangle, leftDangle, phoenixTail;
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
            rightCoat = p("right_coat");
            leftCoat = p("left_coat");
            coatBack = p("coat_back");
            rightDangle = p("right_dangle");
            leftDangle = p("left_dangle");
            phoenixTail = p("phoenix_tail");
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
            turnPair(rightCoat, leftCoat, -amount * 0.12f + Mth.sin(age * 0.05f) * 0.02f, 0, Mth.sin(age * 0.045f) * 0.02f + (spin ? 0.25f : 0));
            turn(coatBack, amount * 0.25f + Mth.sin(age * 0.06f) * 0.03f + (spin ? 0.2f : 0), 0, 0);
            turnPair(rightDangle, leftDangle, Mth.sin(age * 0.11f) * 0.12f + amount * 0.2f, 0, Mth.cos(age * 0.09f) * 0.1f);
            turn(phoenixTail, Mth.sin(age * 0.05f) * 0.04f, 0, 0);
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
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(187, 86).mirror(false).addBox(-1.5f, -19.2f, -1.6f, 3, 5, 2.8f)
                .texOffs(185, 62).mirror(false).addBox(-5.2f, -14.4f, -3.1f, 10.4f, 2.7f, 6.2f)
                .texOffs(20, 119).mirror(false).addBox(-5.2667f, -13.7f, -3.4f, 2.8f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(1.6667f, -13.7f, -3.4f, 2.8f, 1.3f, 0.35f)
                .texOffs(173, 105).mirror(false).addBox(-5.5f, -13.7f, -1.3f, 0.35f, 1.3f, 2.6f)
                .texOffs(173, 105).mirror(false).addBox(5.15f, -13.7f, -1.3f, 0.35f, 1.3f, 2.6f)
                .texOffs(20, 119).mirror(false).addBox(-3.9f, -13.7f, 3.05f, 2.6f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(1.3f, -13.7f, 3.05f, 2.6f, 1.3f, 0.35f)
                .texOffs(63, 75).mirror(false).addBox(-4.925f, -12, -3, 9.85f, 2.7f, 6)
                .texOffs(20, 119).mirror(false).addBox(-4.1833f, -11.3f, -3.3f, 2.8f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(2.3833f, -11.3f, -3.3f, 2.8f, 1.3f, 0.35f)
                .texOffs(173, 105).mirror(false).addBox(-5.225f, -11.3f, -0.5f, 0.35f, 1.3f, 2.6f)
                .texOffs(173, 105).mirror(false).addBox(4.875f, -11.3f, -0.5f, 0.35f, 1.3f, 2.6f)
                .texOffs(20, 119).mirror(false).addBox(-3.7625f, -11.3f, 2.95f, 2.6f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(1.1625f, -11.3f, 2.95f, 2.6f, 1.3f, 0.35f)
                .texOffs(96, 75).mirror(false).addBox(-4.65f, -9.6f, -2.9f, 9.3f, 2.7f, 5.8f)
                .texOffs(20, 119).mirror(false).addBox(-4.9f, -8.9f, -3.2f, 2.8f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(1.3f, -8.9f, -3.2f, 2.8f, 1.3f, 0.35f)
                .texOffs(173, 105).mirror(false).addBox(-4.95f, -8.9f, -1.3f, 0.35f, 1.3f, 2.6f)
                .texOffs(173, 105).mirror(false).addBox(4.6f, -8.9f, -1.3f, 0.35f, 1.3f, 2.6f)
                .texOffs(20, 119).mirror(false).addBox(-3.625f, -8.9f, 2.85f, 2.6f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(1.025f, -8.9f, 2.85f, 2.6f, 1.3f, 0.35f)
                .texOffs(129, 75).mirror(false).addBox(-4.375f, -7.2f, -2.8f, 8.75f, 2.7f, 5.6f)
                .texOffs(20, 119).mirror(false).addBox(-3.8167f, -6.5f, -3.1f, 2.8f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(2.0167f, -6.5f, -3.1f, 2.8f, 1.3f, 0.35f)
                .texOffs(173, 105).mirror(false).addBox(-4.675f, -6.5f, -0.5f, 0.35f, 1.3f, 2.6f)
                .texOffs(173, 105).mirror(false).addBox(4.325f, -6.5f, -0.5f, 0.35f, 1.3f, 2.6f)
                .texOffs(20, 119).mirror(false).addBox(-3.4875f, -6.5f, 2.75f, 2.6f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(0.8875f, -6.5f, 2.75f, 2.6f, 1.3f, 0.35f)
                .texOffs(160, 75).mirror(false).addBox(-4.1f, -4.8f, -2.7f, 8.2f, 2.7f, 5.4f)
                .texOffs(20, 119).mirror(false).addBox(-4.5333f, -4.1f, -3, 2.8f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(-1.8f, -4.1f, -3, 2.8f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(0.9333f, -4.1f, -3, 2.8f, 1.3f, 0.35f)
                .texOffs(173, 105).mirror(false).addBox(-4.4f, -4.1f, -1.3f, 0.35f, 1.3f, 2.6f)
                .texOffs(173, 105).mirror(false).addBox(4.05f, -4.1f, -1.3f, 0.35f, 1.3f, 2.6f)
                .texOffs(20, 119).mirror(false).addBox(-3.35f, -4.1f, 2.65f, 2.6f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(0.75f, -4.1f, 2.65f, 2.6f, 1.3f, 0.35f)
                .texOffs(191, 75).mirror(false).addBox(-3.825f, -2.4f, -2.6f, 7.65f, 2.7f, 5.2f)
                .texOffs(20, 119).mirror(false).addBox(-3.45f, -1.7f, -2.9f, 2.8f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(-0.9f, -1.7f, -2.9f, 2.8f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(1.65f, -1.7f, -2.9f, 2.8f, 1.3f, 0.35f)
                .texOffs(173, 105).mirror(false).addBox(-4.125f, -1.7f, -0.5f, 0.35f, 1.3f, 2.6f)
                .texOffs(173, 105).mirror(false).addBox(3.775f, -1.7f, -0.5f, 0.35f, 1.3f, 2.6f)
                .texOffs(20, 119).mirror(false).addBox(-3.2125f, -1.7f, 2.55f, 2.6f, 1.3f, 0.35f)
                .texOffs(20, 119).mirror(false).addBox(0.6125f, -1.7f, 2.55f, 2.6f, 1.3f, 0.35f)
                .texOffs(156, 113).mirror(false).addBox(-1.2f, -14, -3.75f, 2.4f, 2.1f, 0.7f)
                .texOffs(156, 113).mirror(false).addBox(-1.2f, -11.6f, -3.53f, 2.4f, 2.1f, 0.7f)
                .texOffs(156, 113).mirror(false).addBox(-1.2f, -9.2f, -3.31f, 2.4f, 2.1f, 0.7f)
                .texOffs(156, 113).mirror(false).addBox(-1.2f, -6.8f, -3.09f, 2.4f, 2.1f, 0.7f)
                .texOffs(156, 113).mirror(false).addBox(-1.2f, -4.4f, -2.87f, 2.4f, 2.1f, 0.7f)
                .texOffs(182, 105).mirror(false).addBox(-2, -14.6f, 3.3f, 4, 2.3f, 1.5f)
                .texOffs(182, 105).mirror(false).addBox(-1.88f, -12.45f, 3.18f, 3.76f, 2.3f, 1.5f)
                .texOffs(182, 105).mirror(false).addBox(-1.76f, -10.3f, 3.06f, 3.52f, 2.3f, 1.5f)
                .texOffs(182, 105).mirror(false).addBox(-1.64f, -8.15f, 2.94f, 3.28f, 2.3f, 1.5f)
                .texOffs(182, 105).mirror(false).addBox(-1.52f, -6, 2.82f, 3.04f, 2.3f, 1.5f)
                .texOffs(195, 105).mirror(false).addBox(-1.4f, -3.85f, 2.7f, 2.8f, 2.3f, 1.5f)
                .texOffs(195, 105).mirror(false).addBox(-1.28f, -1.7f, 2.58f, 2.56f, 2.3f, 1.5f), PartPose.offsetAndRotation(0, 3, 0, 0, 0, 0));
        PartDefinition robe = body.addOrReplaceChild("robe", CubeListBuilder.create()
                .texOffs(210, 0).mirror(false).addBox(-5.6f, -15.2f, -3.6f, 11.2f, 8.8f, 7.2f)
                .texOffs(21, 46).mirror(false).addBox(-4.8f, -6.6f, -3.4f, 9.6f, 7, 6.8f)
                .texOffs(0, 75).mirror(false).addBox(-6, -15.6f, -3.8f, 12, 1.4f, 7.6f)
                .texOffs(200, 86).mirror(false).addBox(-3.2f, -16.4f, -2.8f, 6.4f, 1.6f, 5.6f)
                .texOffs(29, 119).mirror(false).addBox(-4.6f, -12.8f, -3.85f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(1, -12.8f, -3.85f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(-4.9f, -10, -3.85f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(-1.6f, -10, -3.85f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(1.7f, -10, -3.85f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(-4, -7.2f, -4.05f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(0.4f, -7.2f, -4.05f, 3.6f, 1.6f, 0.3f)
                .texOffs(57, 105).mirror(false).addBox(-5.85f, -13.4f, -1.9f, 0.3f, 1.6f, 3.6f)
                .texOffs(57, 105).mirror(false).addBox(5.55f, -13.4f, -1.9f, 0.3f, 1.6f, 3.6f)
                .texOffs(57, 105).mirror(false).addBox(-5.85f, -9.8f, -0.7f, 0.3f, 1.6f, 3.6f)
                .texOffs(57, 105).mirror(false).addBox(5.55f, -9.8f, -0.7f, 0.3f, 1.6f, 3.6f)
                .texOffs(29, 119).mirror(false).addBox(-4.6f, -11, 3.55f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(1, -11, 3.55f, 3.6f, 1.6f, 0.3f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition collarOver = robe.addOrReplaceChild("collar_over", CubeListBuilder.create()
                .texOffs(56, 46).mirror(false).addBox(-1.1f, 0, -0.4f, 2.2f, 12.6f, 0.8f), PartPose.offsetAndRotation(2.8f, -15.6f, -3.85f, 0, 0, 0.6f));
        PartDefinition collarUnder = robe.addOrReplaceChild("collar_under", CubeListBuilder.create()
                .texOffs(227, 86).mirror(false).addBox(-1.1f, 0, -0.3f, 2.2f, 6.4f, 0.6f), PartPose.offsetAndRotation(-2.8f, -15.6f, -3.8f, 0, 0, -0.6f));
        PartDefinition highCollar = robe.addOrReplaceChild("high_collar", CubeListBuilder.create()
                .texOffs(220, 75).mirror(false).addBox(-5.2f, -7.4f, 1.8f, 10.4f, 7.8f, 0.9f)
                .texOffs(41, 75).mirror(false).addBox(-5.2f, -5.6f, -2, 0.9f, 6, 4)
                .texOffs(41, 75).mirror(false).addBox(4.3f, -5.6f, -2, 0.9f, 6, 4), PartPose.offsetAndRotation(0, -15.4f, 0.6f, 0.3f, 0, 0));
        PartDefinition sash = body.addOrReplaceChild("sash", CubeListBuilder.create()
                .texOffs(43, 62).mirror(false).addBox(-5.4f, -1.6f, -3.75f, 10.8f, 2.8f, 7.5f)
                .texOffs(206, 105).mirror(false).addBox(-1.6f, -2, -4.4f, 3.2f, 3.4f, 1), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightSashEnd = sash.addOrReplaceChild("right_sash_end", CubeListBuilder.create()
                .texOffs(65, 46).mirror(false).addBox(-0.9f, 0, -0.25f, 1.8f, 13, 0.5f), PartPose.offsetAndRotation(-1, 0.6f, -4.3f, -0.12f, 0, 0.07f));
        PartDefinition leftSashEnd = sash.addOrReplaceChild("left_sash_end", CubeListBuilder.create()
                .texOffs(65, 46).mirror(true).addBox(-0.9f, 0, -0.25f, 1.8f, 13, 0.5f), PartPose.offsetAndRotation(1, 0.6f, -4.3f, -0.12f, 0, -0.07f));
        PartDefinition sashEndShort = sash.addOrReplaceChild("sash_end_short", CubeListBuilder.create()
                .texOffs(82, 62).mirror(false).addBox(-0.8f, 0, -0.25f, 1.6f, 10, 0.5f), PartPose.offsetAndRotation(1.1f, 0.8f, -4.2f, -0.1f, 0, -0.04f));
        PartDefinition pendant = sash.addOrReplaceChild("pendant", CubeListBuilder.create()
                .texOffs(217, 105).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 4, 0.5f)
                .texOffs(165, 113).mirror(false).addBox(-1.5f, 4, -0.5f, 3, 3, 1)
                .texOffs(68, 105).mirror(false).addBox(-0.5f, 7, -0.4f, 1, 4.5f, 0.8f), PartPose.offsetAndRotation(-3.8f, 0.6f, -4.1f, -0.12f, 0, 0.03f));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(89, 62).mirror(false).addBox(-5.1f, 0.6f, -3.6f, 10.2f, 2.4f, 7.2f)
                .texOffs(128, 62).mirror(false).addBox(-5.3652f, 2.85f, -3.7669f, 10.7303f, 2.4f, 7.5712f)
                .texOffs(0, 62).mirror(false).addBox(-5.85f, 5.1f, -4.05f, 11.7f, 2.4f, 8.25f)
                .texOffs(149, 46).mirror(false).addBox(-6.4778f, 7.35f, -4.3957f, 12.9557f, 2.4f, 9.129f)
                .texOffs(72, 46).mirror(false).addBox(-7.2213f, 9.6f, -4.7849f, 14.4426f, 2.4f, 10.1698f)
                .texOffs(161, 28).mirror(false).addBox(-8.0646f, 11.85f, -5.2065f, 16.1293f, 2.4f, 11.3505f)
                .texOffs(50, 28).mirror(false).addBox(-8.9971f, 14.1f, -5.653f, 17.9942f, 2.4f, 12.656f)
                .texOffs(137, 0).mirror(false).addBox(-10.0109f, 16.35f, -6.1189f, 20.0219f, 2.4f, 14.0753f)
                .texOffs(58, 0).mirror(false).addBox(-11.1f, 18.6f, -6.6f, 22.2f, 2.4f, 15.6f)
                .texOffs(29, 119).mirror(false).addBox(-6, 7.75f, -4.6957f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(2.4f, 7.75f, -4.6957f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(-8.2f, 12.25f, -5.5065f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(-3.3f, 12.25f, -5.5065f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(4.6f, 12.25f, -5.5065f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(-6.6f, 16.75f, -6.4189f, 3.6f, 1.6f, 0.3f)
                .texOffs(29, 119).mirror(false).addBox(3, 16.75f, -6.4189f, 3.6f, 1.6f, 0.3f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightFold0 = skirt.addOrReplaceChild("right_fold_0", CubeListBuilder.create()
                .texOffs(25, 0).mirror(false).addBox(-1.7f, 0, -0.45f, 3.4f, 19.6f, 0.4f), PartPose.offsetAndRotation(-2.1204f, 0.9f, -3.6263f, -0.25f, -0.42f, 0));
        PartDefinition leftFold0 = skirt.addOrReplaceChild("left_fold_0", CubeListBuilder.create()
                .texOffs(25, 0).mirror(true).addBox(-1.7f, 0, -0.45f, 3.4f, 19.6f, 0.4f), PartPose.offsetAndRotation(2.1204f, 0.9f, -3.6263f, -0.25f, 0.42f, 0));
        PartDefinition rightFold1 = skirt.addOrReplaceChild("right_fold_1", CubeListBuilder.create()
                .texOffs(36, 0).mirror(false).addBox(-1.7f, 0, -0.45f, 3.4f, 19.9f, 0.4f), PartPose.offsetAndRotation(-4.2298f, 0.9f, -2.2012f, -0.29f, -0.95f, 0));
        PartDefinition leftFold1 = skirt.addOrReplaceChild("left_fold_1", CubeListBuilder.create()
                .texOffs(36, 0).mirror(true).addBox(-1.7f, 0, -0.45f, 3.4f, 19.9f, 0.4f), PartPose.offsetAndRotation(4.2298f, 0.9f, -2.2012f, -0.29f, 0.95f, 0));
        PartDefinition rightFold2 = skirt.addOrReplaceChild("right_fold_2", CubeListBuilder.create()
                .texOffs(14, 0).mirror(false).addBox(-1.7f, 0, -0.45f, 3.4f, 20.2f, 0.4f), PartPose.offsetAndRotation(-5.1978f, 0.9f, 0.4256f, -0.33f, -1.6f, 0));
        PartDefinition leftFold2 = skirt.addOrReplaceChild("left_fold_2", CubeListBuilder.create()
                .texOffs(14, 0).mirror(true).addBox(-1.7f, 0, -0.45f, 3.4f, 20.2f, 0.4f), PartPose.offsetAndRotation(5.1978f, 0.9f, 0.4256f, -0.33f, 1.6f, 0));
        PartDefinition rightFold3 = skirt.addOrReplaceChild("right_fold_3", CubeListBuilder.create()
                .texOffs(36, 0).mirror(false).addBox(-1.7f, 0, -0.45f, 3.4f, 19.9f, 0.4f), PartPose.offsetAndRotation(-3.8777f, 0.9f, 3.165f, -0.37f, -2.3f, 0));
        PartDefinition leftFold3 = skirt.addOrReplaceChild("left_fold_3", CubeListBuilder.create()
                .texOffs(36, 0).mirror(true).addBox(-1.7f, 0, -0.45f, 3.4f, 19.9f, 0.4f), PartPose.offsetAndRotation(3.8777f, 0.9f, 3.165f, -0.37f, 2.3f, 0));
        PartDefinition rightFold4 = skirt.addOrReplaceChild("right_fold_4", CubeListBuilder.create()
                .texOffs(25, 0).mirror(false).addBox(-1.7f, 0, -0.45f, 3.4f, 19.6f, 0.4f), PartPose.offsetAndRotation(-1.7419f, 0.9f, 4.3516f, -0.41f, -2.8f, 0));
        PartDefinition leftFold4 = skirt.addOrReplaceChild("left_fold_4", CubeListBuilder.create()
                .texOffs(25, 0).mirror(true).addBox(-1.7f, 0, -0.45f, 3.4f, 19.6f, 0.4f), PartPose.offsetAndRotation(1.7419f, 0.9f, 4.3516f, -0.41f, 2.8f, 0));
        PartDefinition rightBackPanel = skirt.addOrReplaceChild("right_back_panel", CubeListBuilder.create()
                .texOffs(47, 0).mirror(false).addBox(-3.2f, 0, 0, 3.4f, 20, 0.5f), PartPose.offsetAndRotation(-2.1f, 1, 4.4f, 0.3f, 0, 0.05f));
        PartDefinition leftBackPanel = skirt.addOrReplaceChild("left_back_panel", CubeListBuilder.create()
                .texOffs(47, 0).mirror(true).addBox(-0.2f, 0, 0, 3.4f, 20, 0.5f), PartPose.offsetAndRotation(2.1f, 1, 4.4f, 0.3f, 0, -0.05f));
        PartDefinition belly = skirt.addOrReplaceChild("belly", CubeListBuilder.create()
                .texOffs(174, 113).mirror(false).addBox(-1.25f, 2.85f, -3.9169f, 2.5f, 2.2f, 0.4f)
                .texOffs(183, 113).mirror(false).addBox(-1.2f, 5.1f, -4.2f, 2.4f, 2.2f, 0.4f)
                .texOffs(174, 113).mirror(false).addBox(-1.15f, 7.35f, -4.5457f, 2.3f, 2.2f, 0.4f)
                .texOffs(183, 113).mirror(false).addBox(-1.1f, 9.6f, -4.9349f, 2.2f, 2.2f, 0.4f)
                .texOffs(174, 113).mirror(false).addBox(-1.05f, 11.85f, -5.3565f, 2.1f, 2.2f, 0.4f)
                .texOffs(192, 113).mirror(false).addBox(-1, 14.1f, -5.803f, 2, 2.2f, 0.4f)
                .texOffs(199, 113).mirror(false).addBox(-0.95f, 16.35f, -6.2689f, 1.9f, 2.2f, 0.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightSlitLeg0 = skirt.addOrReplaceChild("right_slit_leg_0", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.6f, -0.35f, -0.35f, 2.6f, 0.7f, 0.7f), PartPose.offsetAndRotation(-1.2f, 6.3f, -4.05f, 0.5f, 0.6f, 0.5f));
        PartDefinition leftSlitLeg0 = skirt.addOrReplaceChild("left_slit_leg_0", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.35f, -0.35f, 2.6f, 0.7f, 0.7f), PartPose.offsetAndRotation(1.2f, 6.3f, -4.05f, 0.5f, -0.6f, -0.5f));
        PartDefinition rightSlitLegTip0 = rightSlitLeg0.addOrReplaceChild("right_slit_leg_tip_0", CubeListBuilder.create()
                .texOffs(80, 119).mirror(false).addBox(-2, -0.25f, -0.25f, 2, 0.5f, 0.5f), PartPose.offsetAndRotation(-2.5f, 0, 0, 0, 0, -1));
        PartDefinition leftSlitLegTip0 = leftSlitLeg0.addOrReplaceChild("left_slit_leg_tip_0", CubeListBuilder.create()
                .texOffs(80, 119).mirror(true).addBox(0, -0.25f, -0.25f, 2, 0.5f, 0.5f), PartPose.offsetAndRotation(2.5f, 0, 0, 0, 0, 1));
        PartDefinition rightSlitLeg1 = skirt.addOrReplaceChild("right_slit_leg_1", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.6f, -0.35f, -0.35f, 2.6f, 0.7f, 0.7f), PartPose.offsetAndRotation(-1.2f, 10.8f, -4.7849f, 0.5f, 0.6f, 0.5f));
        PartDefinition leftSlitLeg1 = skirt.addOrReplaceChild("left_slit_leg_1", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.35f, -0.35f, 2.6f, 0.7f, 0.7f), PartPose.offsetAndRotation(1.2f, 10.8f, -4.7849f, 0.5f, -0.6f, -0.5f));
        PartDefinition rightSlitLegTip1 = rightSlitLeg1.addOrReplaceChild("right_slit_leg_tip_1", CubeListBuilder.create()
                .texOffs(80, 119).mirror(false).addBox(-2, -0.25f, -0.25f, 2, 0.5f, 0.5f), PartPose.offsetAndRotation(-2.5f, 0, 0, 0, 0, -1));
        PartDefinition leftSlitLegTip1 = leftSlitLeg1.addOrReplaceChild("left_slit_leg_tip_1", CubeListBuilder.create()
                .texOffs(80, 119).mirror(true).addBox(0, -0.25f, -0.25f, 2, 0.5f, 0.5f), PartPose.offsetAndRotation(2.5f, 0, 0, 0, 0, 1));
        PartDefinition rightSlitLeg2 = skirt.addOrReplaceChild("right_slit_leg_2", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.6f, -0.35f, -0.35f, 2.6f, 0.7f, 0.7f), PartPose.offsetAndRotation(-1.2f, 15.3f, -5.653f, 0.5f, 0.6f, 0.5f));
        PartDefinition leftSlitLeg2 = skirt.addOrReplaceChild("left_slit_leg_2", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.35f, -0.35f, 2.6f, 0.7f, 0.7f), PartPose.offsetAndRotation(1.2f, 15.3f, -5.653f, 0.5f, -0.6f, -0.5f));
        PartDefinition rightSlitLegTip2 = rightSlitLeg2.addOrReplaceChild("right_slit_leg_tip_2", CubeListBuilder.create()
                .texOffs(80, 119).mirror(false).addBox(-2, -0.25f, -0.25f, 2, 0.5f, 0.5f), PartPose.offsetAndRotation(-2.5f, 0, 0, 0, 0, -1));
        PartDefinition leftSlitLegTip2 = leftSlitLeg2.addOrReplaceChild("left_slit_leg_tip_2", CubeListBuilder.create()
                .texOffs(80, 119).mirror(true).addBox(0, -0.25f, -0.25f, 2, 0.5f, 0.5f), PartPose.offsetAndRotation(2.5f, 0, 0, 0, 0, 1));
        PartDefinition rightHemLeg0 = skirt.addOrReplaceChild("right_hem_leg_0", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.6f, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(-9.5f, 20.2f, -5.1f, 0, -0.5f, 0.55f));
        PartDefinition leftHemLeg0 = skirt.addOrReplaceChild("left_hem_leg_0", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(9.5f, 20.2f, -5.1f, 0, 0.5f, -0.55f));
        PartDefinition rightHemLegTip0 = rightHemLeg0.addOrReplaceChild("right_hem_leg_tip_0", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-2.4f, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.5f, 0, 0, 0, 0, -1));
        PartDefinition leftHemLegTip0 = leftHemLeg0.addOrReplaceChild("left_hem_leg_tip_0", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(2.5f, 0, 0, 0, 0, 1));
        PartDefinition rightHemLeg1 = skirt.addOrReplaceChild("right_hem_leg_1", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.6f, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(-9.9f, 20.2f, -2.58f, 0, -0.3f, 0.55f));
        PartDefinition leftHemLeg1 = skirt.addOrReplaceChild("left_hem_leg_1", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(9.9f, 20.2f, -2.58f, 0, 0.3f, -0.55f));
        PartDefinition rightHemLegTip1 = rightHemLeg1.addOrReplaceChild("right_hem_leg_tip_1", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-2.4f, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.5f, 0, 0, 0, 0, -1));
        PartDefinition leftHemLegTip1 = leftHemLeg1.addOrReplaceChild("left_hem_leg_tip_1", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(2.5f, 0, 0, 0, 0, 1));
        PartDefinition rightHemLeg2 = skirt.addOrReplaceChild("right_hem_leg_2", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.6f, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(-10.3f, 20.2f, -0.06f, 0, -0.1f, 0.55f));
        PartDefinition leftHemLeg2 = skirt.addOrReplaceChild("left_hem_leg_2", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(10.3f, 20.2f, -0.06f, 0, 0.1f, -0.55f));
        PartDefinition rightHemLegTip2 = rightHemLeg2.addOrReplaceChild("right_hem_leg_tip_2", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-2.4f, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.5f, 0, 0, 0, 0, -1));
        PartDefinition leftHemLegTip2 = leftHemLeg2.addOrReplaceChild("left_hem_leg_tip_2", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(2.5f, 0, 0, 0, 0, 1));
        PartDefinition rightHemLeg3 = skirt.addOrReplaceChild("right_hem_leg_3", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.6f, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(-10.3f, 20.2f, 2.46f, 0, 0.1f, 0.55f));
        PartDefinition leftHemLeg3 = skirt.addOrReplaceChild("left_hem_leg_3", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(10.3f, 20.2f, 2.46f, 0, -0.1f, -0.55f));
        PartDefinition rightHemLegTip3 = rightHemLeg3.addOrReplaceChild("right_hem_leg_tip_3", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-2.4f, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.5f, 0, 0, 0, 0, -1));
        PartDefinition leftHemLegTip3 = leftHemLeg3.addOrReplaceChild("left_hem_leg_tip_3", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(2.5f, 0, 0, 0, 0, 1));
        PartDefinition rightHemLeg4 = skirt.addOrReplaceChild("right_hem_leg_4", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.6f, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(-9.9f, 20.2f, 4.98f, 0, 0.3f, 0.55f));
        PartDefinition leftHemLeg4 = skirt.addOrReplaceChild("left_hem_leg_4", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(9.9f, 20.2f, 4.98f, 0, -0.3f, -0.55f));
        PartDefinition rightHemLegTip4 = rightHemLeg4.addOrReplaceChild("right_hem_leg_tip_4", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-2.4f, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.5f, 0, 0, 0, 0, -1));
        PartDefinition leftHemLegTip4 = leftHemLeg4.addOrReplaceChild("left_hem_leg_tip_4", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(2.5f, 0, 0, 0, 0, 1));
        PartDefinition rightHemLeg5 = skirt.addOrReplaceChild("right_hem_leg_5", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.6f, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(-9.5f, 20.2f, 7.5f, 0, 0.5f, 0.55f));
        PartDefinition leftHemLeg5 = skirt.addOrReplaceChild("left_hem_leg_5", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.6f, 0.8f, 0.8f), PartPose.offsetAndRotation(9.5f, 20.2f, 7.5f, 0, -0.5f, -0.55f));
        PartDefinition rightHemLegTip5 = rightHemLeg5.addOrReplaceChild("right_hem_leg_tip_5", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-2.4f, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.5f, 0, 0, 0, 0, -1));
        PartDefinition leftHemLegTip5 = leftHemLeg5.addOrReplaceChild("left_hem_leg_tip_5", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(2.5f, 0, 0, 0, 0, 1));
        PartDefinition rightFlankLeg0 = body.addOrReplaceChild("right_flank_leg_0", CubeListBuilder.create()
                .texOffs(96, 119).mirror(false).addBox(-4.6f, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(-5, -13.1f, 1.2f, 0, -0.55f, 0.25f));
        PartDefinition leftFlankLeg0 = body.addOrReplaceChild("left_flank_leg_0", CubeListBuilder.create()
                .texOffs(96, 119).mirror(true).addBox(0, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(5, -13.1f, 1.2f, 0, 0.55f, -0.25f));
        PartDefinition rightFlankLegTip0 = rightFlankLeg0.addOrReplaceChild("right_flank_leg_tip_0", CubeListBuilder.create()
                .texOffs(109, 119).mirror(false).addBox(-4, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(-4.5f, 0, 0, 0, 0.2f, -1.1f));
        PartDefinition leftFlankLegTip0 = leftFlankLeg0.addOrReplaceChild("left_flank_leg_tip_0", CubeListBuilder.create()
                .texOffs(109, 119).mirror(true).addBox(0, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(4.5f, 0, 0, 0, -0.2f, 1.1f));
        PartDefinition rightFlankLeg1 = body.addOrReplaceChild("right_flank_leg_1", CubeListBuilder.create()
                .texOffs(96, 119).mirror(false).addBox(-4.6f, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(-4.725f, -10.7f, 1.2f, 0, -0.61f, 0.29f));
        PartDefinition leftFlankLeg1 = body.addOrReplaceChild("left_flank_leg_1", CubeListBuilder.create()
                .texOffs(96, 119).mirror(true).addBox(0, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(4.725f, -10.7f, 1.2f, 0, 0.61f, -0.29f));
        PartDefinition rightFlankLegTip1 = rightFlankLeg1.addOrReplaceChild("right_flank_leg_tip_1", CubeListBuilder.create()
                .texOffs(109, 119).mirror(false).addBox(-4, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(-4.5f, 0, 0, 0, 0.2f, -1.1f));
        PartDefinition leftFlankLegTip1 = leftFlankLeg1.addOrReplaceChild("left_flank_leg_tip_1", CubeListBuilder.create()
                .texOffs(109, 119).mirror(true).addBox(0, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(4.5f, 0, 0, 0, -0.2f, 1.1f));
        PartDefinition rightFlankLeg2 = body.addOrReplaceChild("right_flank_leg_2", CubeListBuilder.create()
                .texOffs(96, 119).mirror(false).addBox(-4.6f, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(-4.45f, -8.3f, 1.2f, 0, -0.67f, 0.33f));
        PartDefinition leftFlankLeg2 = body.addOrReplaceChild("left_flank_leg_2", CubeListBuilder.create()
                .texOffs(96, 119).mirror(true).addBox(0, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(4.45f, -8.3f, 1.2f, 0, 0.67f, -0.33f));
        PartDefinition rightFlankLegTip2 = rightFlankLeg2.addOrReplaceChild("right_flank_leg_tip_2", CubeListBuilder.create()
                .texOffs(109, 119).mirror(false).addBox(-4, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(-4.5f, 0, 0, 0, 0.2f, -1.1f));
        PartDefinition leftFlankLegTip2 = leftFlankLeg2.addOrReplaceChild("left_flank_leg_tip_2", CubeListBuilder.create()
                .texOffs(109, 119).mirror(true).addBox(0, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(4.5f, 0, 0, 0, -0.2f, 1.1f));
        PartDefinition rightFlankLeg3 = body.addOrReplaceChild("right_flank_leg_3", CubeListBuilder.create()
                .texOffs(96, 119).mirror(false).addBox(-4.6f, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(-4.175f, -5.9f, 1.2f, 0, -0.73f, 0.37f));
        PartDefinition leftFlankLeg3 = body.addOrReplaceChild("left_flank_leg_3", CubeListBuilder.create()
                .texOffs(96, 119).mirror(true).addBox(0, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(4.175f, -5.9f, 1.2f, 0, 0.73f, -0.37f));
        PartDefinition rightFlankLegTip3 = rightFlankLeg3.addOrReplaceChild("right_flank_leg_tip_3", CubeListBuilder.create()
                .texOffs(109, 119).mirror(false).addBox(-4, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(-4.5f, 0, 0, 0, 0.2f, -1.1f));
        PartDefinition leftFlankLegTip3 = leftFlankLeg3.addOrReplaceChild("left_flank_leg_tip_3", CubeListBuilder.create()
                .texOffs(109, 119).mirror(true).addBox(0, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(4.5f, 0, 0, 0, -0.2f, 1.1f));
        PartDefinition rightFlankLeg4 = body.addOrReplaceChild("right_flank_leg_4", CubeListBuilder.create()
                .texOffs(96, 119).mirror(false).addBox(-4.6f, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(-3.9f, -3.5f, 1.2f, 0, -0.79f, 0.41f));
        PartDefinition leftFlankLeg4 = body.addOrReplaceChild("left_flank_leg_4", CubeListBuilder.create()
                .texOffs(96, 119).mirror(true).addBox(0, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(3.9f, -3.5f, 1.2f, 0, 0.79f, -0.41f));
        PartDefinition rightFlankLegTip4 = rightFlankLeg4.addOrReplaceChild("right_flank_leg_tip_4", CubeListBuilder.create()
                .texOffs(109, 119).mirror(false).addBox(-4, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(-4.5f, 0, 0, 0, 0.2f, -1.1f));
        PartDefinition leftFlankLegTip4 = leftFlankLeg4.addOrReplaceChild("left_flank_leg_tip_4", CubeListBuilder.create()
                .texOffs(109, 119).mirror(true).addBox(0, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(4.5f, 0, 0, 0, -0.2f, 1.1f));
        PartDefinition rightFlankLeg5 = body.addOrReplaceChild("right_flank_leg_5", CubeListBuilder.create()
                .texOffs(96, 119).mirror(false).addBox(-4.6f, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(-3.625f, -1.1f, 1.2f, 0, -0.85f, 0.45f));
        PartDefinition leftFlankLeg5 = body.addOrReplaceChild("left_flank_leg_5", CubeListBuilder.create()
                .texOffs(96, 119).mirror(true).addBox(0, -0.45f, -0.45f, 4.6f, 0.9f, 0.9f), PartPose.offsetAndRotation(3.625f, -1.1f, 1.2f, 0, 0.85f, -0.45f));
        PartDefinition rightFlankLegTip5 = rightFlankLeg5.addOrReplaceChild("right_flank_leg_tip_5", CubeListBuilder.create()
                .texOffs(109, 119).mirror(false).addBox(-4, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(-4.5f, 0, 0, 0, 0.2f, -1.1f));
        PartDefinition leftFlankLegTip5 = leftFlankLeg5.addOrReplaceChild("left_flank_leg_tip_5", CubeListBuilder.create()
                .texOffs(109, 119).mirror(true).addBox(0, -0.3f, -0.3f, 4, 0.6f, 0.6f), PartPose.offsetAndRotation(4.5f, 0, 0, 0, -0.2f, 1.1f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(113, 28).mirror(false).addBox(-3, -11.4f, -3, 6, 9.6f, 6)
                .texOffs(118, 96).mirror(false).addBox(-2.2f, -2.4f, -2.7f, 4.4f, 1.2f, 4.2f)
                .texOffs(73, 105).mirror(false).addBox(-3.35f, -7, -2.5f, 6.7f, 1.3f, 4)
                .texOffs(120, 119).mirror(false).addBox(-3.15f, -8.5f, -3.45f, 6.3f, 0.9f, 1)
                .texOffs(222, 105).mirror(false).addBox(-0.6f, -7.9f, -3.75f, 1.2f, 3.6f, 0.8f)
                .texOffs(196, 46).mirror(false).addBox(-3.3f, -11.8f, -0.4f, 6.6f, 8.4f, 3.7f)
                .texOffs(0, 86).mirror(false).addBox(-3.3f, -12.1f, -3.3f, 6.6f, 1.2f, 6.5f), PartPose.offsetAndRotation(0, -18.6f, -1.3f, 0.14f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(96, 105).mirror(false).addBox(-0.6f, -1.2f, -0.5f, 0.6f, 3.6f, 1.3f), PartPose.offsetAndRotation(-3, -7.4f, -0.4f, 0, 0.2f, 0));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(96, 105).mirror(true).addBox(0, -1.2f, -0.5f, 0.6f, 3.6f, 1.3f), PartPose.offsetAndRotation(3, -7.4f, -0.4f, 0, -0.2f, 0));
        PartDefinition beard = head.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(52, 75).mirror(false).addBox(-1.1f, 0, -0.55f, 2.2f, 7.5f, 1.1f), PartPose.offsetAndRotation(0, -1.7f, -3, -0.42f, 0, 0));
        PartDefinition beardMid = beard.addOrReplaceChild("beard_mid", CubeListBuilder.create()
                .texOffs(236, 86).mirror(false).addBox(-0.9f, 0, -0.5f, 1.8f, 6.5f, 1), PartPose.offsetAndRotation(0, 7.3f, 0, 0.36f, 0, 0));
        PartDefinition beardTip = beardMid.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(103, 105).mirror(false).addBox(-0.55f, 0, -0.4f, 1.1f, 5, 0.8f), PartPose.offsetAndRotation(0, 6.3f, 0, 0.08f, 0, 0));
        PartDefinition rightBeardLock = head.addOrReplaceChild("right_beard_lock", CubeListBuilder.create()
                .texOffs(243, 86).mirror(false).addBox(-0.55f, 0, -0.45f, 1.1f, 6.5f, 0.9f), PartPose.offsetAndRotation(-1.7f, -2.3f, -2.7f, -0.36f, 0.05f, 0.12f));
        PartDefinition leftBeardLock = head.addOrReplaceChild("left_beard_lock", CubeListBuilder.create()
                .texOffs(243, 86).mirror(true).addBox(-0.55f, 0, -0.45f, 1.1f, 6.5f, 0.9f), PartPose.offsetAndRotation(1.7f, -2.3f, -2.7f, -0.36f, -0.05f, -0.12f));
        PartDefinition rightBeardLockTip = rightBeardLock.addOrReplaceChild("right_beard_lock_tip", CubeListBuilder.create()
                .texOffs(229, 105).mirror(false).addBox(-0.4f, 0, -0.35f, 0.8f, 4, 0.7f), PartPose.offsetAndRotation(0, 6.3f, 0, 0.3f, 0, -0.08f));
        PartDefinition leftBeardLockTip = leftBeardLock.addOrReplaceChild("left_beard_lock_tip", CubeListBuilder.create()
                .texOffs(229, 105).mirror(true).addBox(-0.4f, 0, -0.35f, 0.8f, 4, 0.7f), PartPose.offsetAndRotation(0, 6.3f, 0, 0.3f, 0, 0.08f));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(110, 105).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 4.5f, 0.6f), PartPose.offsetAndRotation(-0.5f, -4, -3.35f, 0.06f, 0.1f, 0.32f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(110, 105).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 4.5f, 0.6f), PartPose.offsetAndRotation(0.5f, -4, -3.35f, 0.06f, -0.1f, -0.32f));
        PartDefinition rightMoustacheTip = rightMoustache.addOrReplaceChild("right_moustache_tip", CubeListBuilder.create()
                .texOffs(250, 86).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 6.5f, 0.5f), PartPose.offsetAndRotation(0, 4.3f, 0, 0.05f, 0, -0.25f));
        PartDefinition leftMoustacheTip = leftMoustache.addOrReplaceChild("left_moustache_tip", CubeListBuilder.create()
                .texOffs(250, 86).mirror(true).addBox(-0.25f, 0, -0.25f, 0.5f, 6.5f, 0.5f), PartPose.offsetAndRotation(0, 4.3f, 0, 0.05f, 0, 0.25f));
        PartDefinition rightFang = head.addOrReplaceChild("right_fang", CubeListBuilder.create()
                .texOffs(206, 113).mirror(false).addBox(-0.4f, 0, -0.4f, 0.8f, 2.2f, 0.8f), PartPose.offsetAndRotation(-1.9f, -2.6f, -2.5f, 0.25f, 0.2f, 0.3f));
        PartDefinition leftFang = head.addOrReplaceChild("left_fang", CubeListBuilder.create()
                .texOffs(206, 113).mirror(true).addBox(-0.4f, 0, -0.4f, 0.8f, 2.2f, 0.8f), PartPose.offsetAndRotation(1.9f, -2.6f, -2.5f, 0.25f, -0.2f, -0.3f));
        PartDefinition rightFangTip = rightFang.addOrReplaceChild("right_fang_tip", CubeListBuilder.create()
                .texOffs(40, 119).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 1.8f, 0.6f), PartPose.offsetAndRotation(0, 2.1f, 0, -0.3f, 0, -0.7f));
        PartDefinition leftFangTip = leftFang.addOrReplaceChild("left_fang_tip", CubeListBuilder.create()
                .texOffs(40, 119).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 1.8f, 0.6f), PartPose.offsetAndRotation(0, 2.1f, 0, -0.3f, 0, 0.7f));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(115, 105).mirror(false).addBox(-1.4f, -2, -1.4f, 2.8f, 2.2f, 2.8f)
                .texOffs(139, 96).mirror(false).addBox(-2.3f, -1.4f, -2.3f, 4.6f, 1.3f, 4.6f)
                .texOffs(29, 86).mirror(false).addBox(-1.9f, -4.9f, -2.1f, 3.8f, 3.6f, 4.2f)
                .texOffs(0, 96).mirror(false).addBox(-0.7f, -6, -2.6f, 1.4f, 1.3f, 5.2f)
                .texOffs(137, 119).mirror(false).addBox(-5.4f, -3.6f, -0.3f, 10.8f, 0.6f, 0.6f)
                .texOffs(45, 119).mirror(false).addBox(-1, -4, -2.45f, 2, 2, 0.5f), PartPose.offsetAndRotation(0, -12, 0.4f, 0, 0, 0));
        PartDefinition rightRibbon = crown.addOrReplaceChild("right_ribbon", CubeListBuilder.create()
                .texOffs(48, 86).mirror(false).addBox(-0.6f, 0, -0.1f, 1.2f, 8, 0.2f), PartPose.offsetAndRotation(-1.8f, -1, 1.8f, 0.32f, 0, 0.12f));
        PartDefinition leftRibbon = crown.addOrReplaceChild("left_ribbon", CubeListBuilder.create()
                .texOffs(48, 86).mirror(true).addBox(-0.6f, 0, -0.1f, 1.2f, 8, 0.2f), PartPose.offsetAndRotation(1.8f, -1, 1.8f, 0.32f, 0, -0.12f));
        PartDefinition rightRibbonTail = rightRibbon.addOrReplaceChild("right_ribbon_tail", CubeListBuilder.create()
                .texOffs(17, 96).mirror(false).addBox(-0.6f, 0, -0.1f, 1.2f, 7, 0.2f), PartPose.offsetAndRotation(0, 7.9f, 0, 0.25f, 0, 0.05f));
        PartDefinition leftRibbonTail = leftRibbon.addOrReplaceChild("left_ribbon_tail", CubeListBuilder.create()
                .texOffs(17, 96).mirror(true).addBox(-0.6f, 0, -0.1f, 1.2f, 7, 0.2f), PartPose.offsetAndRotation(0, 7.9f, 0, 0.25f, 0, -0.05f));
        PartDefinition rightFeeler = crown.addOrReplaceChild("right_feeler", CubeListBuilder.create()
                .texOffs(167, 62).mirror(false).addBox(-0.3f, -10, -0.3f, 0.6f, 10, 0.6f), PartPose.offsetAndRotation(-1, -4.4f, -0.8f, -1, 0.35f, -0.3f));
        PartDefinition leftFeeler = crown.addOrReplaceChild("left_feeler", CubeListBuilder.create()
                .texOffs(167, 62).mirror(true).addBox(-0.3f, -10, -0.3f, 0.6f, 10, 0.6f), PartPose.offsetAndRotation(1, -4.4f, -0.8f, -1, -0.35f, 0.3f));
        PartDefinition rightFeelerTip = rightFeeler.addOrReplaceChild("right_feeler_tip", CubeListBuilder.create()
                .texOffs(125, 46).mirror(false).addBox(-0.25f, -13, -0.25f, 0.5f, 13, 0.5f), PartPose.offsetAndRotation(0, -10, 0, -0.75f, 0, -0.1f));
        PartDefinition leftFeelerTip = leftFeeler.addOrReplaceChild("left_feeler_tip", CubeListBuilder.create()
                .texOffs(125, 46).mirror(true).addBox(-0.25f, -13, -0.25f, 0.5f, 13, 0.5f), PartPose.offsetAndRotation(0, -10, 0, -0.75f, 0, 0.1f));
        PartDefinition crownPetal0 = crown.addOrReplaceChild("crown_petal_0", CubeListBuilder.create()
                .texOffs(211, 113).mirror(false).addBox(-1, -2.6f, -0.25f, 2, 2.6f, 0.5f), PartPose.offsetAndRotation(-0.4964f, -1.6f, 1.8406f, 0.2906f, -2.8908f, -0.0745f));
        PartDefinition crownPetal1 = crown.addOrReplaceChild("crown_petal_1", CubeListBuilder.create()
                .texOffs(211, 113).mirror(false).addBox(-1, -2.6f, -0.25f, 2, 2.6f, 0.5f), PartPose.offsetAndRotation(-1.58f, -1.6f, 1.1649f, 0.1839f, -2.2308f, -0.237f));
        PartDefinition crownPetal2 = crown.addOrReplaceChild("crown_petal_2", CubeListBuilder.create()
                .texOffs(211, 113).mirror(false).addBox(-1, -2.6f, -0.25f, 2, 2.6f, 0.5f), PartPose.offsetAndRotation(-2, -1.6f, -0, -0, -1.5708f, -0.3f));
        PartDefinition crownPetal3 = crown.addOrReplaceChild("crown_petal_3", CubeListBuilder.create()
                .texOffs(211, 113).mirror(false).addBox(-1, -2.6f, -0.25f, 2, 2.6f, 0.5f), PartPose.offsetAndRotation(-1.58f, -1.6f, -1.1649f, -0.1839f, -0.9108f, -0.237f));
        PartDefinition crownPetal4 = crown.addOrReplaceChild("crown_petal_4", CubeListBuilder.create()
                .texOffs(211, 113).mirror(false).addBox(-1, -2.6f, -0.25f, 2, 2.6f, 0.5f), PartPose.offsetAndRotation(-0.4964f, -1.6f, -1.8406f, -0.2906f, -0.2508f, -0.0745f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(55, 86).mirror(false).addBox(-2.6f, -1.2f, -1.9f, 3.4f, 4.6f, 3.8f)
                .texOffs(72, 86).mirror(false).addBox(-2.4f, 3.2f, -1.8f, 3, 4.8f, 3.6f)
                .texOffs(173, 105).mirror(false).addBox(-2.9f, 0.4f, -1.3f, 0.35f, 1.3f, 2.6f)
                .texOffs(173, 105).mirror(false).addBox(-2.7f, 5, -0.8f, 0.35f, 1.3f, 2.6f), PartPose.offsetAndRotation(-5.6f, -13.6f, 0, 0.06f, 0, 0.1f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(55, 86).mirror(true).addBox(-0.8f, -1.2f, -1.9f, 3.4f, 4.6f, 3.8f)
                .texOffs(72, 86).mirror(true).addBox(-0.6f, 3.2f, -1.8f, 3, 4.8f, 3.6f)
                .texOffs(173, 105).mirror(true).addBox(2.55f, 0.4f, -1.3f, 0.35f, 1.3f, 2.6f)
                .texOffs(173, 105).mirror(true).addBox(2.35f, 5, -0.8f, 0.35f, 1.3f, 2.6f), PartPose.offsetAndRotation(5.6f, -13.6f, 0, 0.06f, 0, -0.1f));
        PartDefinition rightRobeSleeve = rightArm.addOrReplaceChild("right_robe_sleeve", CubeListBuilder.create()
                .texOffs(220, 28).mirror(false).addBox(-3.1f, -1.8f, -2.4f, 4.6f, 10, 4.8f)
                .texOffs(57, 105).mirror(false).addBox(-3.4f, 2.4f, -1.8f, 0.3f, 1.6f, 3.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition leftRobeSleeve = leftArm.addOrReplaceChild("left_robe_sleeve", CubeListBuilder.create()
                .texOffs(220, 28).mirror(true).addBox(-1.5f, -1.8f, -2.4f, 4.6f, 10, 4.8f)
                .texOffs(57, 105).mirror(true).addBox(3.1f, 2.4f, -1.8f, 0.3f, 1.6f, 3.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(24, 96).mirror(false).addBox(-2.6f, -1.4f, -2.6f, 4, 1.5f, 5.2f)
                .texOffs(45, 96).mirror(false).addBox(-3.2f, -0.1f, -2.9f, 4.2f, 1.6f, 5.8f)
                .texOffs(87, 86).mirror(false).addBox(-3.8f, 1.3f, -3.2f, 4.5f, 1.7f, 6.4f)
                .texOffs(173, 105).mirror(false).addBox(-4.1f, 1.5f, -1, 0.35f, 1.3f, 2.6f)
                .texOffs(68, 96).mirror(false).addBox(-4, 2.8f, -3, 0.8f, 1.3f, 6), PartPose.offsetAndRotation(-0.4f, -1.8f, 0, 0, 0, 0.6f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(24, 96).mirror(true).addBox(-1.4f, -1.4f, -2.6f, 4, 1.5f, 5.2f)
                .texOffs(45, 96).mirror(true).addBox(-1, -0.1f, -2.9f, 4.2f, 1.6f, 5.8f)
                .texOffs(87, 86).mirror(true).addBox(-0.7f, 1.3f, -3.2f, 4.5f, 1.7f, 6.4f)
                .texOffs(173, 105).mirror(true).addBox(3.75f, 1.5f, -1, 0.35f, 1.3f, 2.6f)
                .texOffs(68, 96).mirror(true).addBox(3.2f, 2.8f, -3, 0.8f, 1.3f, 6), PartPose.offsetAndRotation(0.4f, -1.8f, 0, 0, 0, -0.6f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(172, 62).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 7.8f, 3)
                .texOffs(128, 105).mirror(false).addBox(-1.9f, 6.6f, -1.9f, 3.8f, 1.6f, 3.8f)
                .texOffs(173, 105).mirror(false).addBox(-1.85f, 2, -1.3f, 0.35f, 1.3f, 2.6f), PartPose.offsetAndRotation(-0.9f, 7.8f, 0, -0.6f, -0.25f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(172, 62).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 7.8f, 3)
                .texOffs(128, 105).mirror(true).addBox(-1.9f, 6.6f, -1.9f, 3.8f, 1.6f, 3.8f)
                .texOffs(173, 105).mirror(true).addBox(1.5f, 2, -1.3f, 0.35f, 1.3f, 2.6f), PartPose.offsetAndRotation(0.9f, 7.8f, 0, -0.6f, 0.25f, 0));
        PartDefinition rightGreatSleeve = rightForearm.addOrReplaceChild("right_great_sleeve", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0.4f, 0.6f, 0.587f, -0.0437f, 0.0135f));
        PartDefinition leftGreatSleeve = leftForearm.addOrReplaceChild("left_great_sleeve", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0.4f, 0.6f, 0.587f, 0.0437f, -0.0135f));
        PartDefinition rightSleeveOuter = rightGreatSleeve.addOrReplaceChild("right_sleeve_outer", CubeListBuilder.create()
                .texOffs(219, 46).mirror(false).addBox(-0.25f, 0, -5.4f, 0.5f, 6, 6.6f)
                .texOffs(0, 46).mirror(false).addBox(-0.25f, 5.8f, -7, 0.5f, 5.6f, 8.4f)
                .texOffs(0, 28).mirror(false).addBox(-0.25f, 11.2f, -8.8f, 0.5f, 6, 10.4f)
                .texOffs(236, 46).mirror(false).addBox(0.25f, 0.4f, -5.2f, 0.3f, 5.6f, 6.2f)
                .texOffs(130, 46).mirror(false).addBox(0.25f, 5.8f, -6.8f, 0.3f, 5.6f, 8)
                .texOffs(138, 28).mirror(false).addBox(0.25f, 11.2f, -8.6f, 0.3f, 5.6f, 10)
                .texOffs(57, 105).mirror(false).addBox(-0.55f, 7, -4.8f, 0.3f, 1.6f, 3.6f)
                .texOffs(57, 105).mirror(false).addBox(-0.55f, 12.8f, -7.4f, 0.3f, 1.6f, 3.6f), PartPose.offsetAndRotation(-2.9f, -1, -0.6f, 0, 0, 0.12f));
        PartDefinition leftSleeveOuter = leftGreatSleeve.addOrReplaceChild("left_sleeve_outer", CubeListBuilder.create()
                .texOffs(219, 46).mirror(true).addBox(-0.25f, 0, -5.4f, 0.5f, 6, 6.6f)
                .texOffs(0, 46).mirror(true).addBox(-0.25f, 5.8f, -7, 0.5f, 5.6f, 8.4f)
                .texOffs(0, 28).mirror(true).addBox(-0.25f, 11.2f, -8.8f, 0.5f, 6, 10.4f)
                .texOffs(236, 46).mirror(true).addBox(-0.55f, 0.4f, -5.2f, 0.3f, 5.6f, 6.2f)
                .texOffs(130, 46).mirror(true).addBox(-0.55f, 5.8f, -6.8f, 0.3f, 5.6f, 8)
                .texOffs(138, 28).mirror(true).addBox(-0.55f, 11.2f, -8.6f, 0.3f, 5.6f, 10)
                .texOffs(57, 105).mirror(true).addBox(0.25f, 7, -4.8f, 0.3f, 1.6f, 3.6f)
                .texOffs(57, 105).mirror(true).addBox(0.25f, 12.8f, -7.4f, 0.3f, 1.6f, 3.6f), PartPose.offsetAndRotation(2.9f, -1, -0.6f, 0, 0, -0.12f));
        PartDefinition rightSleeveInner = rightGreatSleeve.addOrReplaceChild("right_sleeve_inner", CubeListBuilder.create()
                .texOffs(219, 46).mirror(false).addBox(-0.25f, 0, -5.4f, 0.5f, 6, 6.6f)
                .texOffs(0, 46).mirror(false).addBox(-0.25f, 5.8f, -7, 0.5f, 5.6f, 8.4f)
                .texOffs(25, 28).mirror(false).addBox(-0.25f, 11.2f, -8.8f, 0.5f, 5.2f, 10.4f)
                .texOffs(236, 46).mirror(false).addBox(-0.55f, 0.4f, -5.2f, 0.3f, 5.6f, 6.2f)
                .texOffs(130, 46).mirror(false).addBox(-0.55f, 5.8f, -6.8f, 0.3f, 5.6f, 8), PartPose.offsetAndRotation(2.7f, -1, -0.6f, 0, 0, -0.05f));
        PartDefinition leftSleeveInner = leftGreatSleeve.addOrReplaceChild("left_sleeve_inner", CubeListBuilder.create()
                .texOffs(219, 46).mirror(true).addBox(-0.25f, 0, -5.4f, 0.5f, 6, 6.6f)
                .texOffs(0, 46).mirror(true).addBox(-0.25f, 5.8f, -7, 0.5f, 5.6f, 8.4f)
                .texOffs(25, 28).mirror(true).addBox(-0.25f, 11.2f, -8.8f, 0.5f, 5.2f, 10.4f)
                .texOffs(236, 46).mirror(true).addBox(0.25f, 0.4f, -5.2f, 0.3f, 5.6f, 6.2f)
                .texOffs(130, 46).mirror(true).addBox(0.25f, 5.8f, -6.8f, 0.3f, 5.6f, 8), PartPose.offsetAndRotation(-2.7f, -1, -0.6f, 0, 0, 0.05f));
        PartDefinition rightSleeveFront = rightGreatSleeve.addOrReplaceChild("right_sleeve_front", CubeListBuilder.create()
                .texOffs(160, 96).mirror(false).addBox(-2.9f, 0, -0.25f, 5.8f, 6, 0.5f)
                .texOffs(175, 96).mirror(false).addBox(-3.3f, 5.8f, -0.25f, 6.6f, 5.6f, 0.5f)
                .texOffs(192, 96).mirror(false).addBox(-3.8f, 11.2f, -0.25f, 7.6f, 5.8f, 0.5f)
                .texOffs(29, 119).mirror(false).addBox(-1.8f, 6.6f, -0.55f, 3.6f, 1.6f, 0.3f), PartPose.offsetAndRotation(0, -1, -5.9f, -0.22f, 0, 0));
        PartDefinition leftSleeveFront = leftGreatSleeve.addOrReplaceChild("left_sleeve_front", CubeListBuilder.create()
                .texOffs(160, 96).mirror(true).addBox(-2.9f, 0, -0.25f, 5.8f, 6, 0.5f)
                .texOffs(175, 96).mirror(true).addBox(-3.3f, 5.8f, -0.25f, 6.6f, 5.6f, 0.5f)
                .texOffs(192, 96).mirror(true).addBox(-3.8f, 11.2f, -0.25f, 7.6f, 5.8f, 0.5f)
                .texOffs(29, 119).mirror(true).addBox(-1.8f, 6.6f, -0.55f, 3.6f, 1.6f, 0.3f), PartPose.offsetAndRotation(0, -1, -5.9f, -0.22f, 0, 0));
        PartDefinition rightSleeveBack = rightGreatSleeve.addOrReplaceChild("right_sleeve_back", CubeListBuilder.create()
                .texOffs(160, 96).mirror(false).addBox(-2.9f, 0, -0.25f, 5.8f, 6, 0.5f)
                .texOffs(175, 96).mirror(false).addBox(-3.2f, 5.8f, -0.25f, 6.4f, 5.6f, 0.5f)
                .texOffs(211, 96).mirror(false).addBox(-3.5f, 11.2f, -0.25f, 7, 5.4f, 0.5f), PartPose.offsetAndRotation(0, -1, 0.9f, 0.06f, 0, 0));
        PartDefinition leftSleeveBack = leftGreatSleeve.addOrReplaceChild("left_sleeve_back", CubeListBuilder.create()
                .texOffs(160, 96).mirror(true).addBox(-2.9f, 0, -0.25f, 5.8f, 6, 0.5f)
                .texOffs(175, 96).mirror(true).addBox(-3.2f, 5.8f, -0.25f, 6.4f, 5.6f, 0.5f)
                .texOffs(211, 96).mirror(true).addBox(-3.5f, 11.2f, -0.25f, 7, 5.4f, 0.5f), PartPose.offsetAndRotation(0, -1, 0.9f, 0.06f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(145, 105).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 2.4f, 3)
                .texOffs(234, 105).mirror(false).addBox(-1.55f, 2.2f, -1.5f, 0.8f, 3.4f, 0.8f)
                .texOffs(234, 105).mirror(false).addBox(-0.4f, 2.2f, -1.6f, 0.8f, 3.8f, 0.8f)
                .texOffs(234, 105).mirror(false).addBox(0.75f, 2.2f, -1.5f, 0.8f, 3.2f, 0.8f)
                .texOffs(218, 113).mirror(false).addBox(-1.4f, 1, -2.2f, 0.8f, 2.2f, 0.8f), PartPose.offsetAndRotation(0, 8, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(145, 105).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 2.4f, 3)
                .texOffs(234, 105).mirror(true).addBox(0.75f, 2.2f, -1.5f, 0.8f, 3.4f, 0.8f)
                .texOffs(234, 105).mirror(true).addBox(-0.4f, 2.2f, -1.6f, 0.8f, 3.8f, 0.8f)
                .texOffs(234, 105).mirror(true).addBox(-1.55f, 2.2f, -1.5f, 0.8f, 3.2f, 0.8f)
                .texOffs(218, 113).mirror(true).addBox(0.6f, 1, -2.2f, 0.8f, 2.2f, 0.8f), PartPose.offsetAndRotation(0, 8, 0, 0, 0, 0));
        PartDefinition rightArmLeg0 = rightArm.addOrReplaceChild("right_arm_leg_0", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.8f, -0.35f, -0.35f, 2.8f, 0.7f, 0.7f), PartPose.offsetAndRotation(-2.4f, 1, 2.2f, 0.3f, -0.9f, 0.4f));
        PartDefinition leftArmLeg0 = leftArm.addOrReplaceChild("left_arm_leg_0", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.35f, -0.35f, 2.8f, 0.7f, 0.7f), PartPose.offsetAndRotation(2.4f, 1, 2.2f, 0.3f, 0.9f, -0.4f));
        PartDefinition rightArmLegTip0 = rightArmLeg0.addOrReplaceChild("right_arm_leg_tip_0", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-2.2f, -0.25f, -0.25f, 2.2f, 0.5f, 0.5f), PartPose.offsetAndRotation(-2.7f, 0, 0, 0, 0, -0.8f));
        PartDefinition leftArmLegTip0 = leftArmLeg0.addOrReplaceChild("left_arm_leg_tip_0", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.25f, -0.25f, 2.2f, 0.5f, 0.5f), PartPose.offsetAndRotation(2.7f, 0, 0, 0, 0, 0.8f));
        PartDefinition rightArmLeg1 = rightArm.addOrReplaceChild("right_arm_leg_1", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.8f, -0.35f, -0.35f, 2.8f, 0.7f, 0.7f), PartPose.offsetAndRotation(-2.4f, 3.6f, 2.2f, 0.3f, -0.9f, 0.4f));
        PartDefinition leftArmLeg1 = leftArm.addOrReplaceChild("left_arm_leg_1", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.35f, -0.35f, 2.8f, 0.7f, 0.7f), PartPose.offsetAndRotation(2.4f, 3.6f, 2.2f, 0.3f, 0.9f, -0.4f));
        PartDefinition rightArmLegTip1 = rightArmLeg1.addOrReplaceChild("right_arm_leg_tip_1", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-2.2f, -0.25f, -0.25f, 2.2f, 0.5f, 0.5f), PartPose.offsetAndRotation(-2.7f, 0, 0, 0, 0, -0.8f));
        PartDefinition leftArmLegTip1 = leftArmLeg1.addOrReplaceChild("left_arm_leg_tip_1", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.25f, -0.25f, 2.2f, 0.5f, 0.5f), PartPose.offsetAndRotation(2.7f, 0, 0, 0, 0, 0.8f));
        PartDefinition rightArmLeg2 = rightArm.addOrReplaceChild("right_arm_leg_2", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.8f, -0.35f, -0.35f, 2.8f, 0.7f, 0.7f), PartPose.offsetAndRotation(-2.4f, 6.2f, 2.2f, 0.3f, -0.9f, 0.4f));
        PartDefinition leftArmLeg2 = leftArm.addOrReplaceChild("left_arm_leg_2", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.35f, -0.35f, 2.8f, 0.7f, 0.7f), PartPose.offsetAndRotation(2.4f, 6.2f, 2.2f, 0.3f, 0.9f, -0.4f));
        PartDefinition rightArmLegTip2 = rightArmLeg2.addOrReplaceChild("right_arm_leg_tip_2", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-2.2f, -0.25f, -0.25f, 2.2f, 0.5f, 0.5f), PartPose.offsetAndRotation(-2.7f, 0, 0, 0, 0, -0.8f));
        PartDefinition leftArmLegTip2 = leftArmLeg2.addOrReplaceChild("left_arm_leg_tip_2", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.25f, -0.25f, 2.2f, 0.5f, 0.5f), PartPose.offsetAndRotation(2.7f, 0, 0, 0, 0, 0.8f));
        PartDefinition rightForearmLeg0 = rightForearm.addOrReplaceChild("right_forearm_leg_0", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.4f, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(-1.4f, 1.6f, 1.4f, 0.2f, -0.8f, 0.3f));
        PartDefinition leftForearmLeg0 = leftForearm.addOrReplaceChild("left_forearm_leg_0", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(1.4f, 1.6f, 1.4f, 0.2f, 0.8f, -0.3f));
        PartDefinition rightForearmLeg1 = rightForearm.addOrReplaceChild("right_forearm_leg_1", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.4f, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(-1.4f, 4.6f, 1.4f, 0.2f, -0.8f, 0.3f));
        PartDefinition leftForearmLeg1 = leftForearm.addOrReplaceChild("left_forearm_leg_1", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.3f, -0.3f, 2.4f, 0.6f, 0.6f), PartPose.offsetAndRotation(1.4f, 4.6f, 1.4f, 0.2f, 0.8f, -0.3f));
        PartDefinition sword = rightHand.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.3f, -27.5f, -1.25f, 0.6f, 23.5f, 2.5f)
                .texOffs(9, 0).mirror(false).addBox(-0.45f, -27, -0.3f, 0.9f, 23, 0.6f)
                .texOffs(239, 105).mirror(false).addBox(-0.25f, -30, -0.8f, 0.5f, 2.6f, 1.6f)
                .texOffs(52, 119).mirror(false).addBox(-0.2f, -31.4f, -0.3f, 0.4f, 1.5f, 0.6f)
                .texOffs(228, 96).mirror(false).addBox(-0.9f, -4.6f, -2.4f, 1.8f, 1.2f, 4.8f)
                .texOffs(223, 113).mirror(false).addBox(-0.7f, -5.4f, -1.2f, 1.4f, 0.9f, 2.4f)
                .texOffs(83, 96).mirror(false).addBox(-0.6f, -3.4f, -0.6f, 1.2f, 5.2f, 1.2f)
                .texOffs(234, 113).mirror(false).addBox(-0.9f, 1.8f, -0.9f, 1.8f, 1.4f, 1.8f), PartPose.offsetAndRotation(0, 2, -0.5f, 1.93f, 1.1091f, 1.6668f));
        PartDefinition guardWingFront = sword.addOrReplaceChild("guard_wing_front", CubeListBuilder.create()
                .texOffs(57, 119).mirror(false).addBox(-0.45f, 0, -0.4f, 0.9f, 1.8f, 0.8f), PartPose.offsetAndRotation(0, -4, -2.3f, 0.55f, 0, 0));
        PartDefinition guardWingBack = sword.addOrReplaceChild("guard_wing_back", CubeListBuilder.create()
                .texOffs(57, 119).mirror(false).addBox(-0.45f, 0, -0.4f, 0.9f, 1.8f, 0.8f), PartPose.offsetAndRotation(0, -4, 2.3f, -0.55f, 0, 0));
        PartDefinition tassel = sword.addOrReplaceChild("tassel", CubeListBuilder.create()
                .texOffs(243, 113).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 2.2f, 0.6f)
                .texOffs(248, 113).mirror(false).addBox(-0.6f, 2.2f, -0.6f, 1.2f, 1.2f, 1.2f)
                .texOffs(92, 96).mirror(false).addBox(-0.55f, 3.4f, -0.55f, 1.1f, 5.5f, 1.1f), PartPose.offsetAndRotation(0, 3.2f, 0, 0.1f, 0, 0));
        PartDefinition teacup = leftHand.addOrReplaceChild("teacup", CubeListBuilder.create()
                .texOffs(0, 113).mirror(false).addBox(-1.2f, -0.2f, -1.2f, 2.4f, 1.8f, 2.4f)
                .texOffs(0, 119).mirror(false).addBox(-1.5f, -0.6f, -1.5f, 3, 0.5f, 3)
                .texOffs(62, 119).mirror(false).addBox(-0.8f, 1.6f, -0.8f, 1.6f, 0.4f, 1.6f), PartPose.offsetAndRotation(0, 2.6f, -1, 0.5346f, -0.2575f, -0.0553f));
        PartDefinition tail0 = root.addOrReplaceChild("tail_0", CubeListBuilder.create()
                .texOffs(112, 86).mirror(false).addBox(-3.7f, -1.9f, 0, 7.4f, 3.8f, 4.4f)
                .texOffs(13, 113).mirror(false).addBox(-4.05f, -2.35f, 0.35f, 8.1f, 1, 3.9f)
                .texOffs(13, 119).mirror(false).addBox(-4.1f, -0.4f, 1.4f, 0.45f, 1.2f, 1.6f)
                .texOffs(13, 119).mirror(false).addBox(3.65f, -0.4f, 1.4f, 0.45f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 21.6f, 4.6f, 0.04f, 0, 0));
        PartDefinition rightTleg0 = tail0.addOrReplaceChild("right_tleg_0", CubeListBuilder.create()
                .texOffs(162, 119).mirror(false).addBox(-3.4f, -0.4f, -0.4f, 3.4f, 0.8f, 0.8f), PartPose.offsetAndRotation(-3.5f, 1.3f, 2.2f, 0, -0.25f, 0.45f));
        PartDefinition leftTleg0 = tail0.addOrReplaceChild("left_tleg_0", CubeListBuilder.create()
                .texOffs(162, 119).mirror(true).addBox(0, -0.4f, -0.4f, 3.4f, 0.8f, 0.8f), PartPose.offsetAndRotation(3.5f, 1.3f, 2.2f, 0, 0.25f, -0.45f));
        PartDefinition rightTlegTip0 = rightTleg0.addOrReplaceChild("right_tleg_tip_0", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-3, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(-3.3f, 0, 0, 0, 0, -1.15f));
        PartDefinition leftTlegTip0 = leftTleg0.addOrReplaceChild("left_tleg_tip_0", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(3.3f, 0, 0, 0, 0, 1.15f));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(139, 86).mirror(false).addBox(-3.4125f, -1.7812f, 0, 6.825f, 3.5625f, 4.3f)
                .texOffs(40, 113).mirror(false).addBox(-3.7625f, -2.2313f, 0.35f, 7.525f, 1, 3.8f)
                .texOffs(13, 119).mirror(false).addBox(-3.8125f, -0.4f, 1.35f, 0.45f, 1.2f, 1.6f)
                .texOffs(13, 119).mirror(false).addBox(3.3625f, -0.4f, 1.35f, 0.45f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 0.1f, 4, -0.02f, 0, 0));
        PartDefinition rightTleg1 = tail1.addOrReplaceChild("right_tleg_1", CubeListBuilder.create()
                .texOffs(162, 119).mirror(false).addBox(-3.275f, -0.4f, -0.4f, 3.275f, 0.8f, 0.8f), PartPose.offsetAndRotation(-3.2125f, 1.1812f, 2.15f, 0, -0.1875f, 0.45f));
        PartDefinition leftTleg1 = tail1.addOrReplaceChild("left_tleg_1", CubeListBuilder.create()
                .texOffs(162, 119).mirror(true).addBox(0, -0.4f, -0.4f, 3.275f, 0.8f, 0.8f), PartPose.offsetAndRotation(3.2125f, 1.1812f, 2.15f, 0, 0.1875f, -0.45f));
        PartDefinition rightTlegTip1 = rightTleg1.addOrReplaceChild("right_tleg_tip_1", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-3, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(-3.175f, 0, 0, 0, 0, -1.15f));
        PartDefinition leftTlegTip1 = leftTleg1.addOrReplaceChild("left_tleg_tip_1", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(3.175f, 0, 0, 0, 0, 1.15f));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(139, 86).mirror(false).addBox(-3.125f, -1.6625f, 0, 6.25f, 3.325f, 4.2f)
                .texOffs(65, 113).mirror(false).addBox(-3.475f, -2.1125f, 0.35f, 6.95f, 1, 3.7f)
                .texOffs(13, 119).mirror(false).addBox(-3.525f, -0.4f, 1.3f, 0.45f, 1.2f, 1.6f)
                .texOffs(13, 119).mirror(false).addBox(3.075f, -0.4f, 1.3f, 0.45f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 0.1f, 3.9f, -0.02f, 0, 0));
        PartDefinition rightTleg2 = tail2.addOrReplaceChild("right_tleg_2", CubeListBuilder.create()
                .texOffs(162, 119).mirror(false).addBox(-3.15f, -0.4f, -0.4f, 3.15f, 0.8f, 0.8f), PartPose.offsetAndRotation(-2.925f, 1.0625f, 2.1f, 0, -0.125f, 0.45f));
        PartDefinition leftTleg2 = tail2.addOrReplaceChild("left_tleg_2", CubeListBuilder.create()
                .texOffs(162, 119).mirror(true).addBox(0, -0.4f, -0.4f, 3.15f, 0.8f, 0.8f), PartPose.offsetAndRotation(2.925f, 1.0625f, 2.1f, 0, 0.125f, -0.45f));
        PartDefinition rightTlegTip2 = rightTleg2.addOrReplaceChild("right_tleg_tip_2", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-3, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(-3.05f, 0, 0, 0, 0, -1.15f));
        PartDefinition leftTlegTip2 = leftTleg2.addOrReplaceChild("left_tleg_tip_2", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(3.05f, 0, 0, 0, 0, 1.15f));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create()
                .texOffs(164, 86).mirror(false).addBox(-2.8375f, -1.5437f, 0, 5.675f, 3.0875f, 4.1f)
                .texOffs(65, 113).mirror(false).addBox(-3.1875f, -1.9937f, 0.35f, 6.375f, 1, 3.6f)
                .texOffs(13, 119).mirror(false).addBox(-3.2375f, -0.4f, 1.25f, 0.45f, 1.2f, 1.6f)
                .texOffs(13, 119).mirror(false).addBox(2.7875f, -0.4f, 1.25f, 0.45f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 0.1f, 3.8f, -0.02f, 0, 0));
        PartDefinition rightTleg3 = tail3.addOrReplaceChild("right_tleg_3", CubeListBuilder.create()
                .texOffs(162, 119).mirror(false).addBox(-3.025f, -0.4f, -0.4f, 3.025f, 0.8f, 0.8f), PartPose.offsetAndRotation(-2.6375f, 0.9437f, 2.05f, 0, -0.0625f, 0.45f));
        PartDefinition leftTleg3 = tail3.addOrReplaceChild("left_tleg_3", CubeListBuilder.create()
                .texOffs(162, 119).mirror(true).addBox(0, -0.4f, -0.4f, 3.025f, 0.8f, 0.8f), PartPose.offsetAndRotation(2.6375f, 0.9437f, 2.05f, 0, 0.0625f, -0.45f));
        PartDefinition rightTlegTip3 = rightTleg3.addOrReplaceChild("right_tleg_tip_3", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-3, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.925f, 0, 0, 0, 0, -1.15f));
        PartDefinition leftTlegTip3 = leftTleg3.addOrReplaceChild("left_tleg_tip_3", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(2.925f, 0, 0, 0, 0, 1.15f));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail_4", CubeListBuilder.create()
                .texOffs(0, 105).mirror(false).addBox(-2.55f, -1.425f, 0, 5.1f, 2.85f, 4)
                .texOffs(88, 113).mirror(false).addBox(-2.9f, -1.875f, 0.35f, 5.8f, 1, 3.5f)
                .texOffs(13, 119).mirror(false).addBox(-2.95f, -0.4f, 1.2f, 0.45f, 1.2f, 1.6f)
                .texOffs(13, 119).mirror(false).addBox(2.5f, -0.4f, 1.2f, 0.45f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 0.1f, 3.7f, -0.02f, 0, 0));
        PartDefinition rightTleg4 = tail4.addOrReplaceChild("right_tleg_4", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.9f, -0.4f, -0.4f, 2.9f, 0.8f, 0.8f), PartPose.offsetAndRotation(-2.35f, 0.825f, 2, 0, 0, 0.45f));
        PartDefinition leftTleg4 = tail4.addOrReplaceChild("left_tleg_4", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.9f, 0.8f, 0.8f), PartPose.offsetAndRotation(2.35f, 0.825f, 2, 0, -0, -0.45f));
        PartDefinition rightTlegTip4 = rightTleg4.addOrReplaceChild("right_tleg_tip_4", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-3, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.8f, 0, 0, 0, 0, -1.15f));
        PartDefinition leftTlegTip4 = leftTleg4.addOrReplaceChild("left_tleg_tip_4", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(2.8f, 0, 0, 0, 0, 1.15f));
        PartDefinition tail5 = tail4.addOrReplaceChild("tail_5", CubeListBuilder.create()
                .texOffs(21, 105).mirror(false).addBox(-2.2625f, -1.3062f, 0, 4.525f, 2.6125f, 3.9f)
                .texOffs(88, 113).mirror(false).addBox(-2.6125f, -1.7562f, 0.35f, 5.225f, 1, 3.4f)
                .texOffs(13, 119).mirror(false).addBox(-2.6625f, -0.4f, 1.15f, 0.45f, 1.2f, 1.6f)
                .texOffs(13, 119).mirror(false).addBox(2.2125f, -0.4f, 1.15f, 0.45f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 0.1f, 3.6f, -0.02f, 0, 0));
        PartDefinition rightTleg5 = tail5.addOrReplaceChild("right_tleg_5", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.775f, -0.4f, -0.4f, 2.775f, 0.8f, 0.8f), PartPose.offsetAndRotation(-2.0625f, 0.7062f, 1.95f, 0, 0.0625f, 0.45f));
        PartDefinition leftTleg5 = tail5.addOrReplaceChild("left_tleg_5", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.775f, 0.8f, 0.8f), PartPose.offsetAndRotation(2.0625f, 0.7062f, 1.95f, 0, -0.0625f, -0.45f));
        PartDefinition rightTlegTip5 = rightTleg5.addOrReplaceChild("right_tleg_tip_5", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-3, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.675f, 0, 0, 0, 0, -1.15f));
        PartDefinition leftTlegTip5 = leftTleg5.addOrReplaceChild("left_tleg_tip_5", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(2.675f, 0, 0, 0, 0, 1.15f));
        PartDefinition tail6 = tail5.addOrReplaceChild("tail_6", CubeListBuilder.create()
                .texOffs(40, 105).mirror(false).addBox(-1.975f, -1.1875f, 0, 3.95f, 2.375f, 3.8f)
                .texOffs(109, 113).mirror(false).addBox(-2.325f, -1.6375f, 0.35f, 4.65f, 1, 3.3f)
                .texOffs(13, 119).mirror(false).addBox(-2.375f, -0.4f, 1.1f, 0.45f, 1.2f, 1.6f)
                .texOffs(13, 119).mirror(false).addBox(1.925f, -0.4f, 1.1f, 0.45f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 0.1f, 3.5f, -0.02f, 0, 0));
        PartDefinition rightTleg6 = tail6.addOrReplaceChild("right_tleg_6", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.65f, -0.4f, -0.4f, 2.65f, 0.8f, 0.8f), PartPose.offsetAndRotation(-1.775f, 0.5875f, 1.9f, 0, 0.125f, 0.45f));
        PartDefinition leftTleg6 = tail6.addOrReplaceChild("left_tleg_6", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.65f, 0.8f, 0.8f), PartPose.offsetAndRotation(1.775f, 0.5875f, 1.9f, 0, -0.125f, -0.45f));
        PartDefinition rightTlegTip6 = rightTleg6.addOrReplaceChild("right_tleg_tip_6", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-3, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.55f, 0, 0, 0, 0, -1.15f));
        PartDefinition leftTlegTip6 = leftTleg6.addOrReplaceChild("left_tleg_tip_6", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(2.55f, 0, 0, 0, 0, 1.15f));
        PartDefinition tail7 = tail6.addOrReplaceChild("tail_7", CubeListBuilder.create()
                .texOffs(40, 105).mirror(false).addBox(-1.6875f, -1.0688f, 0, 3.375f, 2.1375f, 3.7f)
                .texOffs(109, 113).mirror(false).addBox(-2.0375f, -1.5188f, 0.35f, 4.075f, 1, 3.2f)
                .texOffs(13, 119).mirror(false).addBox(-2.0875f, -0.4f, 1.05f, 0.45f, 1.2f, 1.6f)
                .texOffs(13, 119).mirror(false).addBox(1.6375f, -0.4f, 1.05f, 0.45f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 0.1f, 3.4f, -0.02f, 0, 0));
        PartDefinition rightTleg7 = tail7.addOrReplaceChild("right_tleg_7", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.525f, -0.4f, -0.4f, 2.525f, 0.8f, 0.8f), PartPose.offsetAndRotation(-1.4875f, 0.4688f, 1.85f, 0, 0.1875f, 0.45f));
        PartDefinition leftTleg7 = tail7.addOrReplaceChild("left_tleg_7", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.525f, 0.8f, 0.8f), PartPose.offsetAndRotation(1.4875f, 0.4688f, 1.85f, 0, -0.1875f, -0.45f));
        PartDefinition rightTlegTip7 = rightTleg7.addOrReplaceChild("right_tleg_tip_7", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-3, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.425f, 0, 0, 0, 0, -1.15f));
        PartDefinition leftTlegTip7 = leftTleg7.addOrReplaceChild("left_tleg_tip_7", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(2.425f, 0, 0, 0, 0, 1.15f));
        PartDefinition tail8 = tail7.addOrReplaceChild("tail_8", CubeListBuilder.create()
                .texOffs(158, 105).mirror(false).addBox(-1.4f, -0.95f, 0, 2.8f, 1.9f, 3.6f)
                .texOffs(128, 113).mirror(false).addBox(-1.75f, -1.4f, 0.35f, 3.5f, 1, 3.1f)
                .texOffs(13, 119).mirror(false).addBox(-1.8f, -0.4f, 1, 0.45f, 1.2f, 1.6f)
                .texOffs(13, 119).mirror(false).addBox(1.35f, -0.4f, 1, 0.45f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 0.1f, 3.3f, -0.02f, 0, 0));
        PartDefinition rightTleg8 = tail8.addOrReplaceChild("right_tleg_8", CubeListBuilder.create()
                .texOffs(71, 119).mirror(false).addBox(-2.4f, -0.4f, -0.4f, 2.4f, 0.8f, 0.8f), PartPose.offsetAndRotation(-1.2f, 0.35f, 1.8f, 0, 0.25f, 0.45f));
        PartDefinition leftTleg8 = tail8.addOrReplaceChild("left_tleg_8", CubeListBuilder.create()
                .texOffs(71, 119).mirror(true).addBox(0, -0.4f, -0.4f, 2.4f, 0.8f, 0.8f), PartPose.offsetAndRotation(1.2f, 0.35f, 1.8f, 0, -0.25f, -0.45f));
        PartDefinition rightTlegTip8 = rightTleg8.addOrReplaceChild("right_tleg_tip_8", CubeListBuilder.create()
                .texOffs(87, 119).mirror(false).addBox(-3, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(-2.3f, 0, 0, 0, 0, -1.15f));
        PartDefinition leftTlegTip8 = leftTleg8.addOrReplaceChild("left_tleg_tip_8", CubeListBuilder.create()
                .texOffs(87, 119).mirror(true).addBox(0, -0.3f, -0.3f, 3, 0.6f, 0.6f), PartPose.offsetAndRotation(2.3f, 0, 0, 0, 0, 1.15f));
        PartDefinition rightTailSpine = tail8.addOrReplaceChild("right_tail_spine", CubeListBuilder.create()
                .texOffs(101, 96).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 6.5f), PartPose.offsetAndRotation(-0.8f, -0.2f, 3.4f, 0.18f, -0.35f, 0));
        PartDefinition leftTailSpine = tail8.addOrReplaceChild("left_tail_spine", CubeListBuilder.create()
                .texOffs(101, 96).mirror(true).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 6.5f), PartPose.offsetAndRotation(0.8f, -0.2f, 3.4f, 0.18f, 0.35f, 0));
        PartDefinition rightTailHook = rightTailSpine.addOrReplaceChild("right_tail_hook", CubeListBuilder.create()
                .texOffs(145, 113).mirror(false).addBox(-0.3f, -0.3f, 0, 0.6f, 0.6f, 3.2f), PartPose.offsetAndRotation(0, 0, 6.3f, 0.3f, 0.6f, 0));
        PartDefinition leftTailHook = leftTailSpine.addOrReplaceChild("left_tail_hook", CubeListBuilder.create()
                .texOffs(145, 113).mirror(true).addBox(-0.3f, -0.3f, 0, 0.6f, 0.6f, 3.2f), PartPose.offsetAndRotation(0, 0, 6.3f, 0.3f, -0.6f, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED lord: end
    }

    /**
     * The Hundred-Eyed Demon Lord: he glides on the little legs round his hem, scuttling in a wave, his
     * robe swaying, his tail winding behind him on its own legs; the little legs down his flanks and
     * arms stir in ripples; his beard, ribbons and sleeves drift; his sword held up before him, cut
     * down on a blow. His golden light: he leans back and flings both arms up and wide, his great
     * sleeves swept back and out behind them, his head thrown back, his feelers and flank legs flaring, his tail rearing,
     * and every eye ablaze. His tea: he lifts the cup in his left hand, his sleeve swept forward, his
     * sword lowered. His sweep: he winds round to his right, his sword drawn back wide, the tail coiling
     * round to the side, and cuts round on the blow. His robe and sleeves gone once he has thrown them
     * off, the eyes bared.
     */
    public static final class LordModel extends Whole<HundredEyedDemonEntity> {
        public static final float SIZE = 1.05f;
        private final ModelPart body, head, beard, beardMid, rightArm, leftArm, rightForearm, leftForearm, rightGreat, leftGreat, robe,
                rightRobe, leftRobe, skirt, rightBack, leftBack, rightSash, leftSash, rightFeeler, leftFeeler,
                rightFeelerTip, leftFeelerTip, rightRibbon, leftRibbon, rightMoustache, leftMoustache, teacup, sword, rightPauldron,
                leftPauldron;
        private final List<ModelPart> tail = new ArrayList<>(), rightTLegs = new ArrayList<>(), leftTLegs = new ArrayList<>(),
                rightHem = new ArrayList<>(), leftHem = new ArrayList<>(), rightFlank = new ArrayList<>(), leftFlank = new ArrayList<>(),
                rightArmLegs = new ArrayList<>(), leftArmLegs = new ArrayList<>(), rightFolds = new ArrayList<>(), leftFolds = new ArrayList<>();

        public LordModel(ModelPart root) {
            super(root);
            body = p("body");
            head = p("head");
            beard = p("beard");
            beardMid = p("beard_mid");
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
            rightBack = p("right_back_panel");
            leftBack = p("left_back_panel");
            rightSash = p("right_sash_end");
            leftSash = p("sash_end_short");
            rightFeeler = p("right_feeler");
            leftFeeler = p("left_feeler");
            rightFeelerTip = p("right_feeler_tip");
            leftFeelerTip = p("left_feeler_tip");
            rightRibbon = p("right_ribbon");
            leftRibbon = p("left_ribbon");
            rightMoustache = p("right_moustache");
            leftMoustache = p("left_moustache");
            rightPauldron = p("right_pauldron");
            leftPauldron = p("left_pauldron");
            teacup = p("teacup");
            sword = p("sword");
            for (int s = 0; hasPart("tail_" + s); s++) {
                tail.add(p("tail_" + s));
                rightTLegs.add(p("right_tleg_" + s));
                leftTLegs.add(p("left_tleg_" + s));
            }
            for (int s = 0; hasPart("right_hem_leg_" + s); s++) {
                rightHem.add(p("right_hem_leg_" + s));
                leftHem.add(p("left_hem_leg_" + s));
            }
            for (int s = 0; hasPart("right_flank_leg_" + s); s++) {
                rightFlank.add(p("right_flank_leg_" + s));
                leftFlank.add(p("left_flank_leg_" + s));
            }
            for (int s = 0; hasPart("right_fold_" + s); s++) {
                rightFolds.add(p("right_fold_" + s));
                leftFolds.add(p("left_fold_" + s));
            }
            for (String n : new String[]{"arm_leg_0", "arm_leg_1", "arm_leg_2", "forearm_leg_0", "forearm_leg_1", "slit_leg_0", "slit_leg_1",
                    "slit_leg_2"}) {
                rightArmLegs.add(p("right_" + n));
                leftArmLegs.add(p("left_" + n));
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

        /** Turns a great sleeve so it hangs from the forearm as it hangs at rest, however the arm is raised; then swings it {@code sway}. */
        private static void hang(ModelPart sleeve, ModelPart arm, ModelPart forearm, float sway, float out) {
            PartPose a0 = arm.getInitialPose(), f0 = forearm.getInitialPose(), s0 = sleeve.getInitialPose();
            Quaternionf now = new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).rotateZYX(forearm.zRot, forearm.yRot, forearm.xRot);
            Quaternionf rest = new Quaternionf().rotationZYX(a0.zRot, a0.yRot, a0.xRot).rotateZYX(f0.zRot, f0.yRot, f0.xRot)
                    .rotateZYX(s0.zRot, s0.yRot, s0.xRot);
            Vector3f r = now.conjugate().mul(rest).getEulerAnglesZYX(new Vector3f());
            sleeve.xRot = r.x + sway;
            sleeve.yRot = r.y;
            sleeve.zRot = r.z + out;
        }

        @Override
        public void setupAnim(HundredEyedDemonEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            int act = e.act();
            boolean glare = act == HundredEyedDemonEntity.GLARING, pour = act == HundredEyedDemonEntity.POURING,
                    sweep = act == HundredEyedDemonEntity.REARING, bare = e.unrobed();
            float breath = Mth.sin(age * 0.05f), wave = age * 0.1f + swing * 0.6f;

            // his body: upright, breathing; leaning back in his glare; wound round to his right before his sweep
            float lean = glare ? -0.16f : sweep ? 0.12f : amount * 0.06f;
            float twist = sweep ? 0.55f - strike * 1.4f : Mth.sin(wave * 0.5f) * 0.03f * amount;
            turn(body, lean, twist, 0);
            body.y = body.getInitialPose().y + breath * 0.2f + (sweep ? 0.8f : 0) + Mth.abs(Mth.sin(wave * 2)) * 0.25f * amount;
            turn(head, pitch * Mth.DEG_TO_RAD + (glare ? -0.4f : pour ? 0.18f : 0), yaw * Mth.DEG_TO_RAD - twist * 0.6f, pour ? -0.12f : 0);
            turn(beard, Mth.sin(age * 0.05f) * 0.04f + amount * 0.12f + (glare ? 0.35f : 0), 0, Mth.sin(age * 0.037f) * 0.03f);
            turn(beardMid, Mth.sin(age * 0.05f + 0.8f) * 0.04f + amount * 0.06f, 0, 0);
            turnPair(rightMoustache, leftMoustache, Mth.sin(age * 0.06f) * 0.04f + amount * 0.1f, 0, 0);
            turnPair(rightRibbon, leftRibbon, amount * 0.35f + Mth.sin(age * 0.07f) * 0.06f + (glare ? 0.3f : 0), 0, Mth.sin(age * 0.05f) * 0.05f);
            float flare = glare ? 0.5f : 0;
            turnPair(rightFeeler, leftFeeler, Mth.sin(age * 0.09f) * 0.1f + flare * 0.9f, Mth.cos(age * 0.07f) * 0.12f, -flare * 0.4f);
            turnPair(rightFeelerTip, leftFeelerTip, Mth.sin(age * 0.11f + 1) * 0.15f + flare * 0.5f, 0, 0);

            // his arms
            float rx = Mth.sin(age * 0.05f) * 0.03f - strike * 1.3f, rz = 0, ry = 0, lx = Mth.sin(age * 0.05f + 1) * 0.03f, lz = 0;
            float rfx = strike * 0.5f, lfx = 0;
            if (glare) {
                rx = -2.55f;
                lx = -2.55f;
                rz = 0.75f;
                lz = -0.75f;
                rfx = 0.55f;
                lfx = 0.55f;
            } else if (pour) {
                lx = -1.25f;
                lz = 0.12f;
                lfx = -0.35f;
                rx = 0.35f;
                rz = 0.12f;
                rfx = 0.7f;
            } else if (sweep) {
                rx = -1.2f + strike * 0.6f;
                rz = 1.15f - strike * 1.9f;
                ry = 0.4f;
                rfx = 0.5f;
                lx = -0.6f;
                lz = -0.5f;
            }
            turn(rightArm, rx, ry, rz);
            turn(leftArm, lx, 0, lz);
            turn(rightForearm, rfx, 0, 0);
            turn(leftForearm, lfx, 0, 0);
            // the great sleeves hang from his forearms whatever the arms do: raised, they fall down them
            // (in his glare they sweep back and out behind his raised arms, clear of his breast)
            hang(rightGreat, rightArm, rightForearm, breath * 0.03f + amount * 0.15f + (glare ? 1.25f : 0), glare ? 0.55f : 0);
            hang(leftGreat, leftArm, leftForearm, -breath * 0.03f + amount * 0.15f + (pour ? -0.35f : glare ? 1.25f : 0), glare ? -0.55f : 0);
            turnPair(rightPauldron, leftPauldron, 0, 0, glare ? 0.25f : 0);
            teacup.visible = pour;
            turn(sword, pour ? 0.6f : sweep ? 0.35f : 0, 0, 0);

            // the robe's skirt swaying as he glides; its panels trailing
            turn(skirt, 0, 0, Mth.sin(wave) * 0.02f * amount);
            turnPair(rightBack, leftBack, amount * 0.25f + Mth.sin(age * 0.06f) * 0.03f, 0, 0);
            for (int i = 0; i < rightFolds.size(); i++) {
                float drift = Mth.sin(age * 0.05f + i * 0.9f) * 0.03f;
                turn(rightFolds.get(i), drift + amount * 0.08f * (i + 1) / rightFolds.size(), 0, sweep ? 0.12f : 0);
                turn(leftFolds.get(i), -drift + amount * 0.08f * (i + 1) / leftFolds.size(), 0, sweep ? -0.12f : 0);
            }
            turn(rightSash, -amount * 0.25f + Mth.sin(age * 0.07f) * 0.05f, 0, Mth.sin(age * 0.05f) * 0.04f);
            turn(leftSash, -amount * 0.22f + Mth.sin(age * 0.07f + 0.7f) * 0.05f, 0, -Mth.sin(age * 0.05f) * 0.04f);

            // the little legs: scuttling round his hem in a wave as he goes, rippling down his flanks and arms
            float scuttle = age * 0.35f + swing * 2.2f;
            float splay = sweep || glare ? 0.25f : 0;
            for (int i = 0; i < rightHem.size(); i++) {
                float a = 0.12f + 0.3f * amount, r = Mth.sin(scuttle - i * 0.9f) * a, l = Mth.sin(scuttle - i * 0.9f + Mth.PI) * a;
                turn(rightHem.get(i), 0, r, Math.max(0, r) * 0.6f - splay);
                turn(leftHem.get(i), 0, -l, -Math.max(0, l) * 0.6f + splay);
            }
            for (int i = 0; i < rightFlank.size(); i++) {
                float r = Mth.sin(age * 0.25f - i * 0.8f) * 0.18f, l = Mth.sin(age * 0.25f - i * 0.8f + 1.3f) * 0.18f;
                turn(rightFlank.get(i), 0, r, r * 0.5f - flare * 0.6f);
                turn(leftFlank.get(i), 0, -l, -l * 0.5f + flare * 0.6f);
            }
            for (int i = 0; i < rightArmLegs.size(); i++) {
                float rip = Mth.sin(age * 0.3f - i * 1.1f) * 0.2f;
                turnPair(rightArmLegs.get(i), leftArmLegs.get(i), 0, rip, rip * 0.4f - flare * 0.4f);
            }

            // the tail: winding behind him in a slow wave, its legs stepping down its length; rearing in his
            // glare; coiled round to his side before his sweep
            for (int s = 0; s < tail.size(); s++) {
                float side = Mth.sin(wave - s * 0.7f) * (0.08f + 0.1f * amount) + (sweep ? 0.2f - strike * 0.45f : 0);
                float rise = glare ? (s == 0 ? -0.25f : -0.1f) : sweep ? (s == 0 ? -0.1f : 0.02f) : 0;
                turn(tail.get(s), rise, side, 0);
                float st = Mth.sin(wave * 2.4f - s * 1.0f);
                turn(rightTLegs.get(s), 0, st * 0.3f, Math.max(0, st) * 0.35f);
                turn(leftTLegs.get(s), 0, -st * 0.3f, -Math.max(0, -st) * 0.35f);
            }

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
