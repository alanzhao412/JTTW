package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.flight.NimbusCloud;
import com.jttw.engine.entity.heaven.GreatSpiritGiantEntity;
import com.jttw.engine.entity.heaven.HeavenMonster;
import com.jttw.engine.entity.heaven.LightningMotherEntity;
import com.jttw.engine.entity.heaven.StarLordEntity;
import com.jttw.engine.entity.heaven.ThunderLordEntity;
import com.jttw.engine.registry.HeavenRegistry;
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
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.function.Function;

/**
 * How Heaven's gods and star lords look (its minibosses), each riding a cloud:
 * <ul>
 * <li>the Nine Luminaries (one frame, nine skins): star lords in court robes sown with stars, a cloud collar,
 * great sleeves and a celestial's ribbon arching behind them, each in his star's colours with his own head-dress
 * and his star's emblem held up, shining (the sun's flaming disc and its crow, the moon's disc and its hare,
 * Wood's staff in leaf, Fire's flaming sword, Earth's dragon staff and gourd, Venus's pipa, Mercury's brush and
 * scroll, Rahu's eclipse, Ketu's comet);</li>
 * <li>the Great Spirit Giant, towering in gilt and black lamellar, a fierce bearded face under a phoenix-winged
 * helmet, a great flower-etched broad axe in each fist;</li>
 * <li>the Thunder Lord, a blue bird-beaked god with dark wings, talons and a ring of drums arching over him, mallet
 * and chisel in hand; the Lightning Mother, a lady in rose and white robes and ribbons with a shining mirror in each hand;</li>
 * <li>Thousand-Mile Eye and Wind-Following Ear, a matched pair in the same armour (teal and green-faced, vermilion and
 * red-faced): great bulging burning eyes and a hand shading them, a halberd; huge fanned ears and a hand cupped
 * behind one, a broad-axe.</li>
 * </ul>
 * Models and skins: {@code assets_source/heaven_gods.py} (and its {@code heaven_gods_*.py}).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeavenGodRenderers {

    private HeavenGodRenderers() {}

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name), "main");
    }

    public static final ModelLayerLocation STAR_LORD_LAYER = layer("heaven_star_lord"), GIANT_LAYER = layer("heaven_great_spirit_giant"),
            THUNDER_LAYER = layer("heaven_thunder_lord"), LIGHTNING_LAYER = layer("heaven_lightning_mother"),
            EYE_LAYER = layer("heaven_thousand_mile_eye"), EAR_LAYER = layer("heaven_wind_following_ear");

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(STAR_LORD_LAYER, HeavenGodRenderers::starLordLayer);
        event.registerLayerDefinition(GIANT_LAYER, HeavenGodRenderers::giantLayer);
        event.registerLayerDefinition(THUNDER_LAYER, HeavenGodRenderers::thunderLayer);
        event.registerLayerDefinition(LIGHTNING_LAYER, HeavenGodRenderers::lightningLayer);
        event.registerLayerDefinition(EYE_LAYER, HeavenGodRenderers::eyeLayer);
        event.registerLayerDefinition(EAR_LAYER, HeavenGodRenderers::earLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HeavenRegistry.STAR_LORD.get(), ctx -> new GodRenderer<>(ctx, new StarLordModel(ctx.bakeLayer(STAR_LORD_LAYER)),
                StarLordModel.SIZE, 0.62f, e -> "heaven_star_lord_" + StarLordEntity.LUMINARIES[Math.floorMod(e.variant(), 9)]));
        event.registerEntityRenderer(HeavenRegistry.GREAT_SPIRIT_GIANT.get(), ctx -> new GodRenderer<>(ctx, new GiantModel(ctx.bakeLayer(GIANT_LAYER)),
                GiantModel.SIZE, 1.25f, e -> "heaven_great_spirit_giant"));
        event.registerEntityRenderer(HeavenRegistry.THUNDER_LORD.get(), ctx -> new GodRenderer<>(ctx, new ThunderModel(ctx.bakeLayer(THUNDER_LAYER)),
                ThunderModel.SIZE, 0.66f, e -> "heaven_thunder_lord"));
        event.registerEntityRenderer(HeavenRegistry.LIGHTNING_MOTHER.get(), ctx -> new GodRenderer<>(ctx, new LightningModel(ctx.bakeLayer(LIGHTNING_LAYER)),
                LightningModel.SIZE, 0.6f, e -> "heaven_lightning_mother"));
        event.registerEntityRenderer(HeavenRegistry.THOUSAND_MILE_EYE.get(), ctx -> new GodRenderer<>(ctx, new SentinelModel<>(ctx.bakeLayer(EYE_LAYER), true),
                SentinelModel.SIZE, 0.6f, e -> "heaven_thousand_mile_eye"));
        event.registerEntityRenderer(HeavenRegistry.WIND_FOLLOWING_EAR.get(), ctx -> new GodRenderer<>(ctx, new SentinelModel<>(ctx.bakeLayer(EAR_LAYER), false),
                SentinelModel.SIZE, 0.6f, e -> "heaven_wind_following_ear"));
    }

    // ── What they share ─────────────────────────────────────────────────

    /** A god's model: its parts turned from their rest, its wind-up's share 0..1 eased in. */
    abstract static class GodModel<T extends HeavenMonster> extends EntityModel<T> {
        protected final ModelPart root;
        /** How far into its wind-up's pose it is, 0..1, eased in and out (set before setupAnim). */
        protected float wind;
        /** Each one's wind-up share, last tick's and this tick's, and the tick it was taken. */
        private final java.util.Map<T, float[]> winds = new java.util.WeakHashMap<>();

        GodModel(ModelPart root) {
            this.root = root;
        }

        @Override
        public void prepareMobModel(T e, float swing, float amount, float partial) {
            // the wind-up pose eases in over a few ticks and out again
            float[] w = winds.computeIfAbsent(e, x -> new float[]{0, 0, -1});
            if (w[2] != e.tickCount) {
                w[0] = w[2] < 0 ? (e.winding() > 0 ? 1 : 0) : w[1];
                w[1] = e.winding() > 0 ? Math.min(1f, w[0] + 0.25f) : Math.max(0f, w[0] - 0.2f);
                if (w[2] < 0) w[1] = w[0];
                w[2] = e.tickCount;
            }
            wind = Mth.lerp(partial, w[0], w[1]);
            wind = wind * wind * (3 - 2 * wind);
        }

        /** A part turned this much from its rest. */
        static void turn(ModelPart p, float x, float y, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.yRot = p.getInitialPose().yRot + y;
            p.zRot = p.getInitialPose().zRot + z;
        }

        static void turn(ModelPart p, float x, float z) {
            turn(p, x, 0, z);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    /**
     * A god's renderer: its model drawn {@code size} times its pixels on a cloud {@code cloud} across, its skin
     * and its glowing skin by name (textures/entity/{name}.png, {name}_glow.png), the glow drawn full-bright,
     * brighter and pulsing while it winds up; its name shown only near.
     */
    static final class GodRenderer<T extends HeavenMonster, M extends GodModel<T>> extends MobRenderer<T, M> {
        /** Names over heads show only this close, so a host is not a wall of labels. */
        private static final double NAME_RANGE = 24;
        private final float size, cloud;
        private final Function<T, String> name;
        private final java.util.Map<String, ResourceLocation> skins = new java.util.HashMap<>();
        private final java.util.Map<String, RenderType> glows = new java.util.HashMap<>();

        GodRenderer(EntityRendererProvider.Context ctx, M model, float size, float cloud, Function<T, String> name) {
            super(ctx, model, 0.45f * size);
            this.size = size;
            this.cloud = cloud;
            this.name = name;
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, T e,
                                   float swing, float amount, float partial, float age, float yaw, float pitch) {
                    VertexConsumer vc = buffers.getBuffer(glow(e));
                    getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
                    if (e.winding() > 0) {
                        // gathering its blow: what shines flares, pulsing
                        int v = (int) (150 + 100 * Mth.sin(age * 0.6f));
                        getParentModel().renderToBuffer(pose, buffers.getBuffer(glow(e)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                                FastColor.ARGB32.color(255, v, v, v));
                    }
                }
            });
        }

        private RenderType glow(T e) {
            return glows.computeIfAbsent(name.apply(e), n -> RenderType.eyes(tex(n + "_glow")));
        }

        @Override
        protected void scale(T e, PoseStack pose, float partial) {
            pose.scale(size, size, size);
        }

        @Override
        public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            super.render(e, yaw, partial, pose, buffers, light);
            pose.pushPose();
            float s = cloud * e.getScale();
            pose.scale(s, s, s);
            NimbusCloud.draw(pose, buffers, e, 1f, light);
            pose.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return skins.computeIfAbsent(name.apply(e), HeavenGodRenderers::tex);
        }

        @Override
        protected boolean shouldShowName(T e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < NAME_RANGE * NAME_RANGE;
        }
    }

    // ── The Nine Luminaries ─────────────────────────────────────────────

    /**
     * The Nine Luminaries (256x256, one frame for the nine): a star lord's court robe in three flaring tiers
     * sown with gold stars, the sea-and-cliff hem at its foot, a jade belt hung with jade pendants, a four-lobed
     * cloud collar, great sleeves, a ribbon arching behind his shoulders and streaming down either side; the
     * ivory tablet in his right hand (Mercury's brush), his star's emblem held up in his left; each his own
     * head-dress: the sun's sunburst crown, the moon's high hair and crescent, Wood's lotus crown, Fire's crown of
     * flames, Earth's great bald brow, Venus's phoenix-cock crown, Mercury's winged cap, Rahu's flaming hair,
     * Ketu's hair streaming back. Model and skins: {@code assets_source/heaven_gods_luminaries.py}.
     */
    public static LayerDefinition starLordLayer() {
        // GENERATED luminaries: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(134, 0).mirror(false).addBox(-6, -15, -5, 12, 15, 10)
                .texOffs(93, 193).mirror(false).addBox(-3.8f, -16.2f, -3.2f, 7.6f, 1.6f, 6.4f)
                .texOffs(124, 193).mirror(false).addBox(-2.2f, -18.4f, -2.2f, 4.4f, 3.6f, 4.4f)
                .texOffs(109, 127).mirror(false).addBox(-7.6f, -15.6f, -6, 15.2f, 0.8f, 12)
                .texOffs(197, 204).mirror(false).addBox(-7.2f, -15.2f, -6.1f, 14.4f, 6, 0.5f)
                .texOffs(0, 213).mirror(false).addBox(-7.2f, -15.2f, 5.6f, 14.4f, 6, 0.5f)
                .texOffs(94, 110).mirror(false).addBox(-6.8f, -4.4f, -5.9f, 13.6f, 2.2f, 11.8f)
                .texOffs(135, 221).mirror(false).addBox(-2, -5, -6.6f, 4, 3, 0.8f), PartPose.offsetAndRotation(0, 4, 0, 0, 0, 0));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(169, 68).mirror(false).addBox(-6.6f, 0, -5.9f, 13.2f, 7, 11.8f)
                .texOffs(185, 44).mirror(false).addBox(-7.8f, 6.5f, -6.6f, 15.6f, 7, 13.2f)
                .texOffs(0, 44).mirror(false).addBox(-9.4f, 13, -7.8f, 18.8f, 7, 15.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition apron = skirt.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(106, 44).mirror(false).addBox(-3.4f, 0, -1, 6.8f, 21, 1), PartPose.offsetAndRotation(0, -1, -6.3f, -0.12f, 0, 0));
        PartDefinition rightPanel = skirt.addOrReplaceChild("right_panel", CubeListBuilder.create()
                .texOffs(123, 44).mirror(false).addBox(-2.2f, 0, -0.5f, 4.4f, 20.5f, 0.5f), PartPose.offsetAndRotation(-5.4f, -1, -6, -0.1f, 0.35f, 0.05f));
        PartDefinition leftPanel = skirt.addOrReplaceChild("left_panel", CubeListBuilder.create()
                .texOffs(123, 44).mirror(true).addBox(-2.2f, 0, -0.5f, 4.4f, 20.5f, 0.5f), PartPose.offsetAndRotation(5.4f, -1, -6, -0.1f, -0.35f, -0.05f));
        PartDefinition backPanel = skirt.addOrReplaceChild("back_panel", CubeListBuilder.create()
                .texOffs(136, 44).mirror(false).addBox(-4.5f, 0, 0, 9, 21, 0.6f), PartPose.offsetAndRotation(0, -1.5f, 6.2f, 0.12f, 0, 0));
        PartDefinition rightPendant = skirt.addOrReplaceChild("right_pendant", CubeListBuilder.create()
                .texOffs(48, 221).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(146, 221).mirror(false).addBox(-1.2f, 3.2f, -0.5f, 2.4f, 2.4f, 1)
                .texOffs(155, 221).mirror(false).addBox(-0.3f, 5.4f, -0.3f, 0.6f, 2.2f, 0.6f)
                .texOffs(53, 221).mirror(false).addBox(-0.9f, 7.4f, -0.5f, 1.8f, 3.6f, 1), PartPose.offsetAndRotation(-6.6f, -2, -2.6f, 0, 0, 0.12f));
        PartDefinition leftPendant = skirt.addOrReplaceChild("left_pendant", CubeListBuilder.create()
                .texOffs(48, 221).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(146, 221).mirror(true).addBox(-1.2f, 3.2f, -0.5f, 2.4f, 2.4f, 1)
                .texOffs(155, 221).mirror(true).addBox(-0.3f, 5.4f, -0.3f, 0.6f, 2.2f, 0.6f)
                .texOffs(53, 221).mirror(true).addBox(-0.9f, 7.4f, -0.5f, 1.8f, 3.6f, 1), PartPose.offsetAndRotation(6.6f, -2, -2.6f, 0, 0, -0.12f));
        PartDefinition rightBoot = root.addOrReplaceChild("right_boot", CubeListBuilder.create()
                .texOffs(33, 213).mirror(false).addBox(-1.7f, 0, -2.4f, 3.4f, 1.6f, 4.2f)
                .texOffs(160, 221).mirror(false).addBox(-1.2f, -1.1f, -3.4f, 2.4f, 1.6f, 1.2f), PartPose.offsetAndRotation(-3.2f, 22.4f, -7.4f, 0, 0.12f, 0));
        PartDefinition leftBoot = root.addOrReplaceChild("left_boot", CubeListBuilder.create()
                .texOffs(33, 213).mirror(true).addBox(-1.7f, 0, -2.4f, 3.4f, 1.6f, 4.2f)
                .texOffs(160, 221).mirror(true).addBox(-1.2f, -1.1f, -3.4f, 2.4f, 1.6f, 1.2f), PartPose.offsetAndRotation(3.2f, 22.4f, -7.4f, 0, -0.12f, 0));
        PartDefinition arch = body.addOrReplaceChild("arch", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -14, 6.4f, 0.12f, 0, 0));
        PartDefinition archSeg0 = arch.addOrReplaceChild("arch_seg_0", CubeListBuilder.create()
                .texOffs(171, 221).mirror(false).addBox(-2.2398f, -1.1f, -0.2f, 4.4796f, 2.2f, 0.4f), PartPose.offsetAndRotation(-12.2473f, -1.9398f, 0, 0, 0, 4.8695f));
        PartDefinition archSeg1 = arch.addOrReplaceChild("arch_seg_1", CubeListBuilder.create()
                .texOffs(171, 221).mirror(false).addBox(-2.2398f, -1.1f, -0.2f, 4.4796f, 2.2f, 0.4f), PartPose.offsetAndRotation(-11.0485f, -5.6295f, 0, 0, 0, 5.1836f));
        PartDefinition archSeg2 = arch.addOrReplaceChild("arch_seg_2", CubeListBuilder.create()
                .texOffs(171, 221).mirror(false).addBox(-2.2398f, -1.1f, -0.2f, 4.4796f, 2.2f, 0.4f), PartPose.offsetAndRotation(-8.7681f, -8.7681f, 0, 0, 0, 5.4978f));
        PartDefinition archSeg3 = arch.addOrReplaceChild("arch_seg_3", CubeListBuilder.create()
                .texOffs(171, 221).mirror(false).addBox(-2.2398f, -1.1f, -0.2f, 4.4796f, 2.2f, 0.4f), PartPose.offsetAndRotation(-5.6295f, -11.0485f, 0, 0, 0, 5.8119f));
        PartDefinition archSeg4 = arch.addOrReplaceChild("arch_seg_4", CubeListBuilder.create()
                .texOffs(171, 221).mirror(false).addBox(-2.2398f, -1.1f, -0.2f, 4.4796f, 2.2f, 0.4f), PartPose.offsetAndRotation(-1.9398f, -12.2473f, 0, 0, 0, 6.1261f));
        PartDefinition archSeg5 = arch.addOrReplaceChild("arch_seg_5", CubeListBuilder.create()
                .texOffs(171, 221).mirror(false).addBox(-2.2398f, -1.1f, -0.2f, 4.4796f, 2.2f, 0.4f), PartPose.offsetAndRotation(1.9398f, -12.2473f, 0, 0, 0, 6.4403f));
        PartDefinition archSeg6 = arch.addOrReplaceChild("arch_seg_6", CubeListBuilder.create()
                .texOffs(171, 221).mirror(false).addBox(-2.2398f, -1.1f, -0.2f, 4.4796f, 2.2f, 0.4f), PartPose.offsetAndRotation(5.6295f, -11.0485f, 0, 0, 0, 6.7544f));
        PartDefinition archSeg7 = arch.addOrReplaceChild("arch_seg_7", CubeListBuilder.create()
                .texOffs(171, 221).mirror(false).addBox(-2.2398f, -1.1f, -0.2f, 4.4796f, 2.2f, 0.4f), PartPose.offsetAndRotation(8.7681f, -8.7681f, 0, 0, 0, 7.0686f));
        PartDefinition archSeg8 = arch.addOrReplaceChild("arch_seg_8", CubeListBuilder.create()
                .texOffs(171, 221).mirror(false).addBox(-2.2398f, -1.1f, -0.2f, 4.4796f, 2.2f, 0.4f), PartPose.offsetAndRotation(11.0485f, -5.6295f, 0, 0, 0, 7.3827f));
        PartDefinition archSeg9 = arch.addOrReplaceChild("arch_seg_9", CubeListBuilder.create()
                .texOffs(171, 221).mirror(false).addBox(-2.2398f, -1.1f, -0.2f, 4.4796f, 2.2f, 0.4f), PartPose.offsetAndRotation(12.2473f, -1.9398f, 0, 0, 0, 7.6969f));
        PartDefinition rightRibbon = body.addOrReplaceChild("right_ribbon", CubeListBuilder.create()
                .texOffs(0, 127).mirror(false).addBox(-1.2f, 0, -0.2f, 2.4f, 13, 0.4f), PartPose.offsetAndRotation(-12.2f, -14.6f, 6.2f, 0.08f, 0.35f, 0.1f));
        PartDefinition leftRibbon = body.addOrReplaceChild("left_ribbon", CubeListBuilder.create()
                .texOffs(0, 127).mirror(true).addBox(-1.2f, 0, -0.2f, 2.4f, 13, 0.4f), PartPose.offsetAndRotation(12.2f, -14.6f, 6.2f, 0.08f, -0.35f, -0.1f));
        PartDefinition rightRibbon2 = rightRibbon.addOrReplaceChild("right_ribbon_2", CubeListBuilder.create()
                .texOffs(9, 127).mirror(false).addBox(-1.3f, 0, -0.2f, 2.6f, 13, 0.4f), PartPose.offsetAndRotation(0, 12.6f, 0, 0.12f, 0, 0.16f));
        PartDefinition leftRibbon2 = leftRibbon.addOrReplaceChild("left_ribbon_2", CubeListBuilder.create()
                .texOffs(9, 127).mirror(true).addBox(-1.3f, 0, -0.2f, 2.6f, 13, 0.4f), PartPose.offsetAndRotation(0, 12.6f, 0, 0.12f, 0, -0.16f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(147, 110).mirror(false).addBox(-3.4f, -2, -2.8f, 4.8f, 9, 5.6f), PartPose.offsetAndRotation(-7.8f, -13.4f, 0, -0.08f, 0, 0.16f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(147, 110).mirror(true).addBox(-1.4f, -2, -2.8f, 4.8f, 9, 5.6f), PartPose.offsetAndRotation(7.8f, -13.4f, 0, -0.1f, 0, -0.4f));
        PartDefinition rightShoulder = rightArm.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(87, 181).mirror(false).addBox(-4.4f, -1.2f, -3.9f, 5.8f, 2, 7.8f), PartPose.offsetAndRotation(-1.2f, -2.2f, 0, 0, 0, 0.3f));
        PartDefinition leftShoulder = leftArm.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(87, 181).mirror(true).addBox(-1.4f, -1.2f, -3.9f, 5.8f, 2, 7.8f), PartPose.offsetAndRotation(1.2f, -2.2f, 0, 0, 0, -0.06f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(0, 156).mirror(false).addBox(-2.3f, 0, -2.3f, 4.6f, 7, 4.6f)
                .texOffs(232, 193).mirror(false).addBox(-2.7f, 5, -2.7f, 5.4f, 2, 5.4f), PartPose.offsetAndRotation(-1, 6.6f, 0, -1.05f, -0.5f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(0, 156).mirror(true).addBox(-2.3f, 0, -2.3f, 4.6f, 7, 4.6f)
                .texOffs(232, 193).mirror(true).addBox(-2.7f, 5, -2.7f, 5.4f, 2, 5.4f), PartPose.offsetAndRotation(1, 6.6f, 0, -2.2f, -0.35f, 0));
        PartDefinition rightSleeve = rightForearm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(71, 44).mirror(false).addBox(-3.4f, -1, -9.4f, 6.8f, 12.5f, 9.4f), PartPose.offsetAndRotation(0, 0.4f, 2.1f, 1.1769f, 0.1373f, 0.155f));
        PartDefinition leftSleeve = leftForearm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(71, 44).mirror(true).addBox(-3.4f, -1, -9.4f, 6.8f, 12.5f, 9.4f), PartPose.offsetAndRotation(0, 0.4f, 2.1f, 2.2488f, -0.5904f, 0.3688f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(125, 213).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(125, 213).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, 0));
        PartDefinition tablet = rightHand.addOrReplaceChild("tablet", CubeListBuilder.create()
                .texOffs(170, 110).mirror(false).addBox(-1.3f, -12, -0.35f, 2.6f, 14, 0.7f), PartPose.offsetAndRotation(0, 1.6f, -0.2f, 1.2955f, 0.48f, 0.3331f));
        PartDefinition brush = rightHand.addOrReplaceChild("brush", CubeListBuilder.create()
                .texOffs(166, 127).mirror(false).addBox(-0.45f, -9, -0.45f, 0.9f, 12, 0.9f)
                .texOffs(138, 213).mirror(false).addBox(-0.8f, -12.4f, -0.8f, 1.6f, 3.6f, 1.6f)
                .texOffs(38, 228).mirror(false).addBox(-0.7f, 2.6f, -0.7f, 1.4f, 1, 1.4f), PartPose.offsetAndRotation(0, 1.6f, -0.2f, 1.2955f, 0.48f, 0.3331f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(190, 90).mirror(false).addBox(-4.4f, -9, -4, 8.8f, 7.4f, 8)
                .texOffs(184, 221).mirror(false).addBox(-1, -5.8f, -4.8f, 2, 2.4f, 0.9f)
                .texOffs(171, 127).mirror(false).addBox(-4.6f, -9.3f, -0.6f, 9.2f, 8, 5), PartPose.offsetAndRotation(0, -18.4f, -0.8f, 0, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(147, 213).mirror(false).addBox(-0.7f, -1.4f, -0.9f, 0.8f, 3.2f, 1.8f), PartPose.offsetAndRotation(-4.4f, -5.6f, -0.8f, 0, -0.2f, 0));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(147, 213).mirror(true).addBox(-0.1f, -1.4f, -0.9f, 0.8f, 3.2f, 1.8f), PartPose.offsetAndRotation(4.4f, -5.6f, -0.8f, 0, 0.2f, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(47, 228).mirror(false).addBox(-3.8f, -0.9f, -0.5f, 3.8f, 1.1f, 1), PartPose.offsetAndRotation(-0.4f, -6.5f, -4.3f, 0, 0.1f, 0.24f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(47, 228).mirror(true).addBox(0, -0.9f, -0.5f, 3.8f, 1.1f, 1), PartPose.offsetAndRotation(0.4f, -6.5f, -4.3f, 0, -0.1f, -0.24f));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(99, 228).mirror(false).addBox(-3.2f, -0.5f, -0.5f, 3.2f, 1, 1), PartPose.offsetAndRotation(-0.5f, -3.3f, -4.5f, 0, 0.2f, -0.55f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(99, 228).mirror(true).addBox(0, -0.5f, -0.5f, 3.2f, 1, 1), PartPose.offsetAndRotation(0.5f, -3.3f, -4.5f, 0, -0.2f, 0.55f));
        PartDefinition rightSidelock = head.addOrReplaceChild("right_sidelock", CubeListBuilder.create()
                .texOffs(21, 156).mirror(false).addBox(-0.8f, 0, -1, 1.6f, 9.5f, 2), PartPose.offsetAndRotation(-4.3f, -7.6f, -2.4f, -0.05f, 0, 0.06f));
        PartDefinition leftSidelock = head.addOrReplaceChild("left_sidelock", CubeListBuilder.create()
                .texOffs(21, 156).mirror(true).addBox(-0.8f, 0, -1, 1.6f, 9.5f, 2), PartPose.offsetAndRotation(4.3f, -7.6f, -2.4f, -0.05f, 0, -0.06f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(116, 181).mirror(false).addBox(-4.1f, 0, -3.4f, 8.2f, 2.6f, 6.8f), PartPose.offsetAndRotation(0, -2, -0.6f, 0, 0, 0));
        PartDefinition beard = jaw.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(127, 156).mirror(false).addBox(-1.4f, 0, -0.9f, 2.8f, 9, 1.8f), PartPose.offsetAndRotation(0, 2, -3.4f, 0.12f, 0, 0));
        PartDefinition beardTip = beard.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(52, 213).mirror(false).addBox(-1, 0, -0.8f, 2, 4.5f, 1.6f), PartPose.offsetAndRotation(0, 8.6f, 0, -0.1f, 0, 0));
        PartDefinition rightBeard = jaw.addOrReplaceChild("right_beard", CubeListBuilder.create()
                .texOffs(149, 181).mirror(false).addBox(-1.1f, 0, -0.8f, 2.2f, 7.5f, 1.6f), PartPose.offsetAndRotation(-2.5f, 1.6f, -3.1f, 0.12f, 0.15f, 0.22f));
        PartDefinition leftBeard = jaw.addOrReplaceChild("left_beard", CubeListBuilder.create()
                .texOffs(149, 181).mirror(true).addBox(-1.1f, 0, -0.8f, 2.2f, 7.5f, 1.6f), PartPose.offsetAndRotation(2.5f, 1.6f, -3.1f, 0.12f, -0.15f, -0.22f));
        PartDefinition hdSun = head.addOrReplaceChild("hd_sun", CubeListBuilder.create()
                .texOffs(138, 156).mirror(false).addBox(-3.4f, -3.6f, -3.2f, 6.8f, 3.8f, 6.4f)
                .texOffs(167, 156).mirror(false).addBox(-4.7f, -0.9f, -4.3f, 9.4f, 1.4f, 8.6f)
                .texOffs(154, 213).mirror(false).addBox(-2.4f, -6.8f, -4.6f, 4.8f, 4.8f, 0.8f)
                .texOffs(110, 228).mirror(false).addBox(-6, -3.2f, -0.35f, 12, 0.7f, 0.7f), PartPose.offsetAndRotation(0, -9, 0, 0, 0, 0));
        PartDefinition hdSunRay0 = hdSun.addOrReplaceChild("hd_sun_ray_0", CubeListBuilder.create()
                .texOffs(167, 213).mirror(false).addBox(-0.7f, -7.725f, -0.3f, 1.4f, 4.325f, 0.6f), PartPose.offsetAndRotation(0, -3.6f, -3.4f, 0, 0, -1.309f));
        PartDefinition hdSunRay1 = hdSun.addOrReplaceChild("hd_sun_ray_1", CubeListBuilder.create()
                .texOffs(167, 213).mirror(false).addBox(-0.7f, -8.35f, -0.3f, 1.4f, 4.95f, 0.6f), PartPose.offsetAndRotation(0, -3.6f, -3.4f, 0, 0, -0.8727f));
        PartDefinition hdSunRay2 = hdSun.addOrReplaceChild("hd_sun_ray_2", CubeListBuilder.create()
                .texOffs(61, 213).mirror(false).addBox(-0.7f, -8.975f, -0.3f, 1.4f, 5.575f, 0.6f), PartPose.offsetAndRotation(0, -3.6f, -3.4f, 0, 0, -0.4363f));
        PartDefinition hdSunRay3 = hdSun.addOrReplaceChild("hd_sun_ray_3", CubeListBuilder.create()
                .texOffs(0, 204).mirror(false).addBox(-0.7f, -9.6f, -0.3f, 1.4f, 6.2f, 0.6f), PartPose.offsetAndRotation(0, -3.6f, -3.4f, 0, 0, 0));
        PartDefinition hdSunRay4 = hdSun.addOrReplaceChild("hd_sun_ray_4", CubeListBuilder.create()
                .texOffs(61, 213).mirror(false).addBox(-0.7f, -8.975f, -0.3f, 1.4f, 5.575f, 0.6f), PartPose.offsetAndRotation(0, -3.6f, -3.4f, 0, 0, 0.4363f));
        PartDefinition hdSunRay5 = hdSun.addOrReplaceChild("hd_sun_ray_5", CubeListBuilder.create()
                .texOffs(167, 213).mirror(false).addBox(-0.7f, -8.35f, -0.3f, 1.4f, 4.95f, 0.6f), PartPose.offsetAndRotation(0, -3.6f, -3.4f, 0, 0, 0.8727f));
        PartDefinition hdSunRay6 = hdSun.addOrReplaceChild("hd_sun_ray_6", CubeListBuilder.create()
                .texOffs(167, 213).mirror(false).addBox(-0.7f, -7.725f, -0.3f, 1.4f, 4.325f, 0.6f), PartPose.offsetAndRotation(0, -3.6f, -3.4f, 0, 0, 1.309f));
        PartDefinition hdMoon = head.addOrReplaceChild("hd_moon", CubeListBuilder.create()
                .texOffs(206, 156).mirror(false).addBox(-2.4f, -5.2f, -1.4f, 4.8f, 5.6f, 4.6f)
                .texOffs(0, 169).mirror(false).addBox(-4.6f, -1, -4.2f, 9.2f, 1.4f, 8.4f)
                .texOffs(191, 221).mirror(false).addBox(-1.2f, -2.6f, -4.5f, 2.4f, 2.2f, 0.6f), PartPose.offsetAndRotation(0, -9, 0, 0, 0, 0));
        PartDefinition rightMoonLoop = hdMoon.addOrReplaceChild("right_moon_loop", CubeListBuilder.create()
                .texOffs(68, 213).mirror(false).addBox(-2.4f, -3.6f, -1.2f, 2.4f, 3.6f, 2.4f), PartPose.offsetAndRotation(-2.8f, -2.4f, 0.2f, 0, 0, -0.35f));
        PartDefinition leftMoonLoop = hdMoon.addOrReplaceChild("left_moon_loop", CubeListBuilder.create()
                .texOffs(68, 213).mirror(true).addBox(0, -3.6f, -1.2f, 2.4f, 3.6f, 2.4f), PartPose.offsetAndRotation(2.8f, -2.4f, 0.2f, 0, 0, 0.35f));
        PartDefinition rightMoonPin = hdMoon.addOrReplaceChild("right_moon_pin", CubeListBuilder.create()
                .texOffs(137, 228).mirror(false).addBox(-4.6f, -0.3f, -0.3f, 4.6f, 0.6f, 0.6f)
                .texOffs(200, 221).mirror(false).addBox(-4.4f, 0.3f, -0.3f, 0.6f, 2.8f, 0.6f)
                .texOffs(58, 228).mirror(false).addBox(-3, 0.3f, -0.3f, 0.6f, 1.8f, 0.6f), PartPose.offsetAndRotation(-1.8f, -3.6f, -0.6f, 0, 0.2f, 0.35f));
        PartDefinition leftMoonPin = hdMoon.addOrReplaceChild("left_moon_pin", CubeListBuilder.create()
                .texOffs(137, 228).mirror(true).addBox(0, -0.3f, -0.3f, 4.6f, 0.6f, 0.6f)
                .texOffs(200, 221).mirror(true).addBox(3.8f, 0.3f, -0.3f, 0.6f, 2.8f, 0.6f)
                .texOffs(58, 228).mirror(true).addBox(2.4f, 0.3f, -0.3f, 0.6f, 1.8f, 0.6f), PartPose.offsetAndRotation(1.8f, -3.6f, -0.6f, 0, -0.2f, -0.35f));
        PartDefinition hdMoonCrescent0 = hdMoon.addOrReplaceChild("hd_moon_crescent_0", CubeListBuilder.create()
                .texOffs(63, 228).mirror(false).addBox(-1.0359f, -0.7f, -0.5f, 2.0718f, 1.4f, 1), PartPose.offsetAndRotation(3.0158f, -6.8301f, -1.4f, 0, 0, 2.0508f));
        PartDefinition hdMoonCrescent1 = hdMoon.addOrReplaceChild("hd_moon_crescent_1", CubeListBuilder.create()
                .texOffs(63, 228).mirror(false).addBox(-1.0359f, -0.7f, -0.5f, 2.0718f, 1.4f, 1), PartPose.offsetAndRotation(2.0698f, -5.7026f, -1.4f, 0, 0, 2.4871f));
        PartDefinition hdMoonCrescent2 = hdMoon.addOrReplaceChild("hd_moon_crescent_2", CubeListBuilder.create()
                .texOffs(63, 228).mirror(false).addBox(-1.0359f, -0.7f, -0.5f, 2.0718f, 1.4f, 1), PartPose.offsetAndRotation(0.7359f, -5.0806f, -1.4f, 0, 0, 2.9234f));
        PartDefinition hdMoonCrescent3 = hdMoon.addOrReplaceChild("hd_moon_crescent_3", CubeListBuilder.create()
                .texOffs(63, 228).mirror(false).addBox(-1.0359f, -0.7f, -0.5f, 2.0718f, 1.4f, 1), PartPose.offsetAndRotation(-0.7359f, -5.0806f, -1.4f, 0, 0, 3.3598f));
        PartDefinition hdMoonCrescent4 = hdMoon.addOrReplaceChild("hd_moon_crescent_4", CubeListBuilder.create()
                .texOffs(63, 228).mirror(false).addBox(-1.0359f, -0.7f, -0.5f, 2.0718f, 1.4f, 1), PartPose.offsetAndRotation(-2.0698f, -5.7026f, -1.4f, 0, 0, 3.7961f));
        PartDefinition hdMoonCrescent5 = hdMoon.addOrReplaceChild("hd_moon_crescent_5", CubeListBuilder.create()
                .texOffs(63, 228).mirror(false).addBox(-1.0359f, -0.7f, -0.5f, 2.0718f, 1.4f, 1), PartPose.offsetAndRotation(-3.0158f, -6.8301f, -1.4f, 0, 0, 4.2324f));
        PartDefinition hdJupiter = head.addOrReplaceChild("hd_jupiter", CubeListBuilder.create()
                .texOffs(7, 204).mirror(false).addBox(-1.8f, -3.6f, -1.8f, 3.6f, 3.8f, 3.6f)
                .texOffs(24, 204).mirror(false).addBox(-2.6f, -1.8f, -2.6f, 5.2f, 1.6f, 5.2f)
                .texOffs(39, 169).mirror(false).addBox(-4.6f, -0.9f, -4.3f, 9.2f, 1.4f, 8.6f)
                .texOffs(150, 228).mirror(false).addBox(-5, -2.8f, -0.3f, 10, 0.6f, 0.6f), PartPose.offsetAndRotation(0, -9, 0, 0, 0, 0));
        PartDefinition hdJupiterPetal0 = hdJupiter.addOrReplaceChild("hd_jupiter_petal_0", CubeListBuilder.create()
                .texOffs(60, 221).mirror(false).addBox(-1.2f, -3.4f, -0.15f, 2.4f, 3.4f, 0.3f), PartPose.offsetAndRotation(1, -1.6f, -1.7321f, 0.4f, -0.5236f, 0));
        PartDefinition hdJupiterPetal1 = hdJupiter.addOrReplaceChild("hd_jupiter_petal_1", CubeListBuilder.create()
                .texOffs(60, 221).mirror(false).addBox(-1.2f, -3.4f, -0.15f, 2.4f, 3.4f, 0.3f), PartPose.offsetAndRotation(2, -1.6f, -0, 0.4f, -1.5708f, 0));
        PartDefinition hdJupiterPetal2 = hdJupiter.addOrReplaceChild("hd_jupiter_petal_2", CubeListBuilder.create()
                .texOffs(60, 221).mirror(false).addBox(-1.2f, -3.4f, -0.15f, 2.4f, 3.4f, 0.3f), PartPose.offsetAndRotation(1, -1.6f, 1.7321f, 0.4f, -2.618f, 0));
        PartDefinition hdJupiterPetal3 = hdJupiter.addOrReplaceChild("hd_jupiter_petal_3", CubeListBuilder.create()
                .texOffs(60, 221).mirror(false).addBox(-1.2f, -3.4f, -0.15f, 2.4f, 3.4f, 0.3f), PartPose.offsetAndRotation(-1, -1.6f, 1.7321f, 0.4f, -3.6652f, 0));
        PartDefinition hdJupiterPetal4 = hdJupiter.addOrReplaceChild("hd_jupiter_petal_4", CubeListBuilder.create()
                .texOffs(60, 221).mirror(false).addBox(-1.2f, -3.4f, -0.15f, 2.4f, 3.4f, 0.3f), PartPose.offsetAndRotation(-2, -1.6f, 0, 0.4f, -4.7124f, 0));
        PartDefinition hdJupiterPetal5 = hdJupiter.addOrReplaceChild("hd_jupiter_petal_5", CubeListBuilder.create()
                .texOffs(60, 221).mirror(false).addBox(-1.2f, -3.4f, -0.15f, 2.4f, 3.4f, 0.3f), PartPose.offsetAndRotation(-1, -1.6f, -1.7321f, 0.4f, -5.7596f, 0));
        PartDefinition hdMars = head.addOrReplaceChild("hd_mars", CubeListBuilder.create()
                .texOffs(78, 169).mirror(false).addBox(-4.7f, -0.9f, -4.3f, 9.4f, 1.4f, 8.6f)
                .texOffs(205, 221).mirror(false).addBox(-1.2f, -1.6f, -4.7f, 2.4f, 2.2f, 0.6f)
                .texOffs(145, 193).mirror(false).addBox(-3.6f, -2.6f, -2.4f, 7.2f, 2.4f, 6), PartPose.offsetAndRotation(0, -9, 0, 0, 0, 0));
        PartDefinition hdMarsFlame = hdMars.addOrReplaceChild("hd_mars_flame", CubeListBuilder.create()
                .texOffs(174, 193).mirror(false).addBox(-3.4f, -7.4f, -0.1f, 6.8f, 7.4f, 0.2f)
                .texOffs(179, 110).mirror(false).addBox(-0.1f, -7.4f, -3.4f, 0.2f, 7.4f, 6.8f), PartPose.offsetAndRotation(0, -1, -2.6f, -0.1f, 0, 0));
        PartDefinition rightMarsFlame = hdMars.addOrReplaceChild("right_mars_flame", CubeListBuilder.create()
                .texOffs(81, 213).mirror(false).addBox(-2.2f, -5.2f, -0.1f, 4.4f, 5.2f, 0.2f), PartPose.offsetAndRotation(-3.4f, -0.8f, -1.4f, 0, 0.3f, -0.35f));
        PartDefinition leftMarsFlame = hdMars.addOrReplaceChild("left_mars_flame", CubeListBuilder.create()
                .texOffs(81, 213).mirror(true).addBox(-2.2f, -5.2f, -0.1f, 4.4f, 5.2f, 0.2f), PartPose.offsetAndRotation(3.4f, -0.8f, -1.4f, 0, -0.3f, 0.35f));
        PartDefinition hdSaturn = head.addOrReplaceChild("hd_saturn", CubeListBuilder.create()
                .texOffs(18, 127).mirror(false).addBox(-3.9f, -5, -3.8f, 7.8f, 5.6f, 7.2f)
                .texOffs(49, 204).mirror(false).addBox(-3, -6.4f, -3, 6, 1.6f, 5.8f)
                .texOffs(69, 221).mirror(false).addBox(-1, -6.6f, 1.6f, 2, 2.4f, 2), PartPose.offsetAndRotation(0, -9, 0, 0, 0, 0));
        PartDefinition rightSaturnBrow = hdSaturn.addOrReplaceChild("right_saturn_brow", CubeListBuilder.create()
                .texOffs(94, 213).mirror(false).addBox(-0.6f, -0.4f, -0.5f, 1.2f, 5.6f, 1), PartPose.offsetAndRotation(-1.4f, 2.6f, -4.3f, 0, 0.15f, 0.95f));
        PartDefinition leftSaturnBrow = hdSaturn.addOrReplaceChild("left_saturn_brow", CubeListBuilder.create()
                .texOffs(94, 213).mirror(true).addBox(-0.6f, -0.4f, -0.5f, 1.2f, 5.6f, 1), PartPose.offsetAndRotation(1.4f, 2.6f, -4.3f, 0, -0.15f, -0.95f));
        PartDefinition hdVenus = head.addOrReplaceChild("hd_venus", CubeListBuilder.create()
                .texOffs(160, 181).mirror(false).addBox(-2.8f, -4.4f, -1.6f, 5.6f, 4.8f, 4.6f)
                .texOffs(117, 169).mirror(false).addBox(-4.6f, -1.6f, -4.3f, 9.2f, 2, 8.6f)
                .texOffs(214, 221).mirror(false).addBox(-2.2f, -3.4f, -4.6f, 4.4f, 2.6f, 0.6f), PartPose.offsetAndRotation(0, -9, 0, 0, 0, 0));
        PartDefinition hdVenusBird = hdVenus.addOrReplaceChild("hd_venus_bird", CubeListBuilder.create()
                .texOffs(101, 213).mirror(false).addBox(-1.1f, -2.6f, -2, 2.2f, 2.6f, 3.6f)
                .texOffs(78, 221).mirror(false).addBox(-0.75f, -4.8f, -2.8f, 1.5f, 2.6f, 1.5f)
                .texOffs(227, 221).mirror(false).addBox(-0.3f, -5.8f, -2.6f, 0.6f, 1.2f, 1.2f)
                .texOffs(173, 228).mirror(false).addBox(-0.3f, -4.2f, -3.6f, 0.6f, 0.6f, 0.9f), PartPose.offsetAndRotation(0, -4.4f, -0.2f, 0, 0, 0));
        PartDefinition hdVenusTail = hdVenusBird.addOrReplaceChild("hd_venus_tail", CubeListBuilder.create()
                .texOffs(74, 204).mirror(false).addBox(-2.2f, -6.4f, -0.1f, 4.4f, 6.4f, 0.2f)
                .texOffs(156, 169).mirror(false).addBox(-0.1f, -6.4f, -1.6f, 0.2f, 6.4f, 3.2f), PartPose.offsetAndRotation(0, -1.6f, 1.2f, -0.55f, 0, 0));
        PartDefinition rightVenusPin = hdVenus.addOrReplaceChild("right_venus_pin", CubeListBuilder.create()
                .texOffs(178, 228).mirror(false).addBox(-3.6f, -0.3f, -0.3f, 3.6f, 0.6f, 0.6f)
                .texOffs(87, 221).mirror(false).addBox(-3.4f, 0.3f, -0.3f, 0.6f, 3.2f, 0.6f)
                .texOffs(234, 221).mirror(false).addBox(-2.2f, 0.3f, -0.3f, 0.6f, 2.2f, 0.6f), PartPose.offsetAndRotation(-3.2f, -1.2f, -1.2f, 0, 0.15f, 0.25f));
        PartDefinition leftVenusPin = hdVenus.addOrReplaceChild("left_venus_pin", CubeListBuilder.create()
                .texOffs(178, 228).mirror(true).addBox(0, -0.3f, -0.3f, 3.6f, 0.6f, 0.6f)
                .texOffs(87, 221).mirror(true).addBox(2.8f, 0.3f, -0.3f, 0.6f, 3.2f, 0.6f)
                .texOffs(234, 221).mirror(true).addBox(1.6f, 0.3f, -0.3f, 0.6f, 2.2f, 0.6f), PartPose.offsetAndRotation(3.2f, -1.2f, -1.2f, 0, -0.15f, -0.25f));
        PartDefinition hdMercury = head.addOrReplaceChild("hd_mercury", CubeListBuilder.create()
                .texOffs(202, 127).mirror(false).addBox(-4, -3.6f, -3.8f, 8, 4.4f, 7.6f)
                .texOffs(87, 204).mirror(false).addBox(-3, -6.2f, -0.4f, 6, 3, 4.2f)
                .texOffs(183, 181).mirror(false).addBox(-4.2f, -0.5f, -4.1f, 8.4f, 0.9f, 8.2f), PartPose.offsetAndRotation(0, -9, 0, 0, 0, 0));
        PartDefinition rightFutouWing = hdMercury.addOrReplaceChild("right_futou_wing", CubeListBuilder.create()
                .texOffs(72, 228).mirror(false).addBox(-12, -0.75f, -0.2f, 12, 1.5f, 0.4f), PartPose.offsetAndRotation(-3.6f, -2.6f, 2.6f, 0, 0, -0.03f));
        PartDefinition leftFutouWing = hdMercury.addOrReplaceChild("left_futou_wing", CubeListBuilder.create()
                .texOffs(72, 228).mirror(true).addBox(0, -0.75f, -0.2f, 12, 1.5f, 0.4f), PartPose.offsetAndRotation(3.6f, -2.6f, 2.6f, 0, 0, 0.03f));
        PartDefinition hdRahu = head.addOrReplaceChild("hd_rahu", CubeListBuilder.create()
                .texOffs(167, 169).mirror(false).addBox(-4.7f, -0.9f, -4.3f, 9.4f, 1.6f, 8.6f)
                .texOffs(220, 181).mirror(false).addBox(-3.8f, -2.6f, -3, 7.6f, 2.4f, 6.6f), PartPose.offsetAndRotation(0, -9, 0, 0, 0, 0));
        PartDefinition hdRahuLock0 = hdRahu.addOrReplaceChild("hd_rahu_lock_0", CubeListBuilder.create()
                .texOffs(0, 193).mirror(false).addBox(-1.3f, -7, -1.1f, 2.6f, 7, 2.2f), PartPose.offsetAndRotation(-3, -1.6f, -2.2f, -0.15f, 0, -0.55f));
        PartDefinition hdRahuLock1 = hdRahu.addOrReplaceChild("hd_rahu_lock_1", CubeListBuilder.create()
                .texOffs(30, 156).mirror(false).addBox(-1.3f, -9, -1.1f, 2.6f, 9, 2.2f), PartPose.offsetAndRotation(-1.4f, -1.6f, -2.6f, -0.1f, 0, -0.2f));
        PartDefinition hdRahuLock2 = hdRahu.addOrReplaceChild("hd_rahu_lock_2", CubeListBuilder.create()
                .texOffs(235, 127).mirror(false).addBox(-1.3f, -9.5f, -1.1f, 2.6f, 9.5f, 2.2f), PartPose.offsetAndRotation(0.4f, -1.6f, -2.4f, -0.12f, 0, 0.12f));
        PartDefinition hdRahuLock3 = hdRahu.addOrReplaceChild("hd_rahu_lock_3", CubeListBuilder.create()
                .texOffs(206, 169).mirror(false).addBox(-1.3f, -8, -1.1f, 2.6f, 8, 2.2f), PartPose.offsetAndRotation(2.2f, -1.6f, -2, -0.15f, 0, 0.45f));
        PartDefinition hdRahuLock4 = hdRahu.addOrReplaceChild("hd_rahu_lock_4", CubeListBuilder.create()
                .texOffs(30, 156).mirror(false).addBox(-1.3f, -8.5f, -1.1f, 2.6f, 8.5f, 2.2f), PartPose.offsetAndRotation(-2.4f, -1.6f, 0.6f, -0.55f, 0, -0.4f));
        PartDefinition hdRahuLock5 = hdRahu.addOrReplaceChild("hd_rahu_lock_5", CubeListBuilder.create()
                .texOffs(235, 127).mirror(false).addBox(-1.3f, -10, -1.1f, 2.6f, 10, 2.2f), PartPose.offsetAndRotation(0, -1.6f, 1.2f, -0.6f, 0, 0));
        PartDefinition hdRahuLock6 = hdRahu.addOrReplaceChild("hd_rahu_lock_6", CubeListBuilder.create()
                .texOffs(30, 156).mirror(false).addBox(-1.3f, -8.5f, -1.1f, 2.6f, 8.5f, 2.2f), PartPose.offsetAndRotation(2.4f, -1.6f, 0.6f, -0.55f, 0, 0.4f));
        PartDefinition hdRahuLock7 = hdRahu.addOrReplaceChild("hd_rahu_lock_7", CubeListBuilder.create()
                .texOffs(206, 169).mirror(false).addBox(-1.3f, -8, -1.1f, 2.6f, 8, 2.2f), PartPose.offsetAndRotation(-1.2f, -1.6f, 2.6f, -0.9f, 0, -0.2f));
        PartDefinition hdRahuLock8 = hdRahu.addOrReplaceChild("hd_rahu_lock_8", CubeListBuilder.create()
                .texOffs(206, 169).mirror(false).addBox(-1.3f, -8, -1.1f, 2.6f, 8, 2.2f), PartPose.offsetAndRotation(1.4f, -1.6f, 2.6f, -0.9f, 0, 0.25f));
        PartDefinition hdKetu = head.addOrReplaceChild("hd_ketu", CubeListBuilder.create()
                .texOffs(0, 181).mirror(false).addBox(-4.7f, -0.9f, -4.3f, 9.4f, 1.4f, 8.6f)
                .texOffs(239, 221).mirror(false).addBox(-1.3f, -2.8f, -4.7f, 2.6f, 2.6f, 0.6f)
                .texOffs(13, 193).mirror(false).addBox(-3.6f, -2.2f, -3.4f, 7.2f, 1.8f, 7.4f), PartPose.offsetAndRotation(0, -9, 0, 0, 0, 0));
        PartDefinition hdKetuLock0 = hdKetu.addOrReplaceChild("hd_ketu_lock_0", CubeListBuilder.create()
                .texOffs(196, 110).mirror(false).addBox(-1.3f, -1.1f, 0, 2.6f, 2.2f, 12), PartPose.offsetAndRotation(-2.6f, -1.4f, 2.4f, 0.25f, -0.25f, 0));
        PartDefinition hdKetuLock1 = hdKetu.addOrReplaceChild("hd_ketu_lock_1", CubeListBuilder.create()
                .texOffs(66, 90).mirror(false).addBox(-1.5f, -1.1f, 0, 3, 2.2f, 15), PartPose.offsetAndRotation(0, -2.2f, 2.4f, 0.12f, 0, 0));
        PartDefinition hdKetuLock2 = hdKetu.addOrReplaceChild("hd_ketu_lock_2", CubeListBuilder.create()
                .texOffs(196, 110).mirror(false).addBox(-1.3f, -1.1f, 0, 2.6f, 2.2f, 12), PartPose.offsetAndRotation(2.6f, -1.4f, 2.4f, 0.25f, 0.25f, 0));
        PartDefinition hdKetuLock3 = hdKetu.addOrReplaceChild("hd_ketu_lock_3", CubeListBuilder.create()
                .texOffs(0, 110).mirror(false).addBox(-1.2f, -1.1f, 0, 2.4f, 2.2f, 13), PartPose.offsetAndRotation(-1.4f, 0.8f, 2.4f, -0.05f, -0.12f, 0));
        PartDefinition hdKetuLock4 = hdKetu.addOrReplaceChild("hd_ketu_lock_4", CubeListBuilder.create()
                .texOffs(0, 110).mirror(false).addBox(-1.2f, -1.1f, 0, 2.4f, 2.2f, 13), PartPose.offsetAndRotation(1.4f, 0.8f, 2.4f, -0.05f, 0.12f, 0));
        PartDefinition emblemSun = leftHand.addOrReplaceChild("emblem_sun", CubeListBuilder.create()
                .texOffs(110, 204).mirror(false).addBox(-0.91f, -4.42f, -0.91f, 1.82f, 5.72f, 1.82f)
                .texOffs(174, 213).mirror(false).addBox(-3.38f, -6.24f, -1.3f, 6.76f, 2.08f, 2.6f)
                .texOffs(0, 90).mirror(false).addBox(-8.45f, -22.88f, -0.78f, 16.9f, 16.9f, 1.56f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemMoon = leftHand.addOrReplaceChild("emblem_moon", CubeListBuilder.create()
                .texOffs(119, 204).mirror(false).addBox(-0.91f, -4.42f, -0.91f, 1.82f, 5.72f, 1.82f)
                .texOffs(195, 213).mirror(false).addBox(-3.38f, -6.24f, -1.3f, 6.76f, 2.08f, 2.6f)
                .texOffs(103, 90).mirror(false).addBox(-7.54f, -21.32f, -0.78f, 15.08f, 15.08f, 1.56f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemJupiter = leftHand.addOrReplaceChild("emblem_jupiter", CubeListBuilder.create()
                .texOffs(52, 0).mirror(false).addBox(-0.91f, -20.15f, -0.91f, 1.82f, 27.3f, 1.82f)
                .texOffs(191, 193).mirror(false).addBox(-2.08f, -23.66f, -2.08f, 4.16f, 3.9f, 4.16f)
                .texOffs(128, 204).mirror(false).addBox(-1.56f, -26.52f, -1.56f, 3.12f, 3.12f, 3.12f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemJupiterB1 = emblemJupiter.addOrReplaceChild("emblem_jupiter_b1", CubeListBuilder.create()
                .texOffs(46, 193).mirror(false).addBox(-0.585f, -7.15f, -0.585f, 1.17f, 7.54f, 1.17f)
                .texOffs(145, 204).mirror(false).addBox(-1.95f, -10.4f, -1.95f, 3.9f, 3.64f, 3.9f), PartPose.offsetAndRotation(0, -13, 0, 0, 0, -0.75f));
        PartDefinition emblemJupiterB2 = emblemJupiter.addOrReplaceChild("emblem_jupiter_b2", CubeListBuilder.create()
                .texOffs(212, 193).mirror(false).addBox(-0.585f, -5.85f, -0.585f, 1.17f, 6.24f, 1.17f)
                .texOffs(145, 204).mirror(false).addBox(-1.69f, -8.58f, -1.69f, 3.38f, 3.12f, 3.38f), PartPose.offsetAndRotation(0, -16.9f, 0, 0, 0.5f, 0.7f));
        PartDefinition emblemJupiterB3 = emblemJupiter.addOrReplaceChild("emblem_jupiter_b3", CubeListBuilder.create()
                .texOffs(116, 213).mirror(false).addBox(-0.52f, -4.55f, -0.52f, 1.04f, 4.94f, 1.04f)
                .texOffs(216, 213).mirror(false).addBox(-1.43f, -7.02f, -1.43f, 2.86f, 2.86f, 2.86f), PartPose.offsetAndRotation(0, -7.8f, 0, 0.4f, 0, 0.55f));
        PartDefinition emblemMars = leftHand.addOrReplaceChild("emblem_mars", CubeListBuilder.create()
                .texOffs(162, 204).mirror(false).addBox(-0.78f, -1.3f, -0.78f, 1.56f, 5.2f, 1.56f)
                .texOffs(92, 221).mirror(false).addBox(-1.17f, 3.9f, -1.17f, 2.34f, 1.82f, 2.34f)
                .texOffs(105, 221).mirror(false).addBox(-3.38f, -3.12f, -1.17f, 6.76f, 1.82f, 2.34f)
                .texOffs(157, 44).mirror(false).addBox(-1.04f, -22.88f, -0.39f, 2.08f, 20.02f, 0.78f)
                .texOffs(166, 44).mirror(false).addBox(-3.64f, -25.22f, -0.13f, 7.28f, 20.8f, 0.26f)
                .texOffs(61, 0).mirror(false).addBox(-0.13f, -25.22f, -3.64f, 0.26f, 20.8f, 7.28f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemSaturn = leftHand.addOrReplaceChild("emblem_saturn", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.8f, -24, -0.8f, 1.6f, 41, 1.6f)
                .texOffs(55, 193).mirror(false).addBox(-1.5f, -28, -2.8f, 3, 4.4f, 4.6f)
                .texOffs(229, 213).mirror(false).addBox(-1.1f, -26.4f, -4.6f, 2.2f, 2.2f, 2.2f)
                .texOffs(126, 221).mirror(false).addBox(-1, -30, 0.6f, 2, 2.6f, 1.4f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemSaturnGourd = emblemSaturn.addOrReplaceChild("emblem_saturn_gourd", CubeListBuilder.create()
                .texOffs(248, 221).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.2f, 0.5f)
                .texOffs(0, 228).mirror(false).addBox(-1, 2, -1, 2, 2, 2)
                .texOffs(171, 204).mirror(false).addBox(-1.6f, 3.8f, -1.6f, 3.2f, 3.4f, 3.2f), PartPose.offsetAndRotation(1.2f, -23.5f, 0, 0, 0, -0.1f));
        PartDefinition emblemVenus = leftHand.addOrReplaceChild("emblem_venus", CubeListBuilder.create()
                .texOffs(39, 181).mirror(false).addBox(-1.05f, -6.75f, -0.9f, 2.1f, 9, 1.8f)
                .texOffs(221, 193).mirror(false).addBox(-1.2f, -12.6f, -0.45f, 2.4f, 6.3f, 1.8f)
                .texOffs(111, 68).mirror(false).addBox(-6.3f, 1.8f, -1.5f, 12.6f, 16.5f, 3)
                .texOffs(0, 221).mirror(false).addBox(-4.8f, 0.3f, -1.2f, 9.6f, 2.1f, 2.4f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemVenusPegs = emblemVenus.addOrReplaceChild("emblem_venus_pegs", CubeListBuilder.create()
                .texOffs(189, 228).mirror(false).addBox(-2.7f, -0.45f, 0, 5.4f, 0.9f, 0.9f)
                .texOffs(189, 228).mirror(false).addBox(-2.7f, 2.1f, 0, 5.4f, 0.9f, 0.9f), PartPose.offsetAndRotation(0, -10.5f, 0, 0, 0, 0));
        PartDefinition emblemMercury = leftHand.addOrReplaceChild("emblem_mercury", CubeListBuilder.create()
                .texOffs(9, 228).mirror(false).addBox(-5.98f, -0.92f, -0.92f, 11.96f, 1.84f, 1.84f)
                .texOffs(227, 110).mirror(false).addBox(-5.175f, 0.92f, -0.1725f, 10.35f, 13.8f, 0.345f)
                .texOffs(9, 228).mirror(false).addBox(-5.98f, 14.49f, -0.92f, 11.96f, 1.84f, 1.84f), PartPose.offsetAndRotation(0.4f, 2.2f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemRahu = leftHand.addOrReplaceChild("emblem_rahu", CubeListBuilder.create()
                .texOffs(188, 204).mirror(false).addBox(-0.91f, -4.42f, -0.91f, 1.82f, 5.72f, 1.82f)
                .texOffs(27, 221).mirror(false).addBox(-3.38f, -6.24f, -1.3f, 6.76f, 2.08f, 2.6f)
                .texOffs(0, 68).mirror(false).addBox(-9.1f, -23.92f, -0.78f, 18.2f, 18.2f, 1.56f), PartPose.offsetAndRotation(0.4f, 1.6f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemKetu = leftHand.addOrReplaceChild("emblem_ketu", CubeListBuilder.create()
                .texOffs(72, 193).mirror(false).addBox(-2.47f, -13.52f, -2.47f, 4.94f, 4.94f, 4.94f)
                .texOffs(43, 68).mirror(false).addBox(-9.75f, -20.54f, -0.13f, 19.5f, 19.5f, 0.26f)
                .texOffs(9, 0).mirror(false).addBox(-0.13f, -20.54f, -9.75f, 0.26f, 19.5f, 19.5f)
                .texOffs(33, 110).mirror(false).addBox(-7.15f, -11.18f, -7.15f, 14.3f, 0.26f, 14.3f), PartPose.offsetAndRotation(0.4f, 0.4f, -1.2f, 2.4327f, -0.3499f, -0.1503f));
        PartDefinition emblemKetuTail0 = emblemKetu.addOrReplaceChild("emblem_ketu_tail_0", CubeListBuilder.create()
                .texOffs(50, 181).mirror(false).addBox(-3.575f, -0.13f, 0, 7.15f, 0.26f, 9.1f)
                .texOffs(140, 90).mirror(false).addBox(-0.13f, -3.575f, 0, 0.26f, 7.15f, 9.1f)
                .texOffs(51, 127).mirror(false).addBox(-1.69f, -1.69f, 0, 3.38f, 3.38f, 9.1f), PartPose.offsetAndRotation(0, -11.05f, 1.3f, 0.8f, 0.08f, 0));
        PartDefinition emblemKetuTail1 = emblemKetuTail0.addOrReplaceChild("emblem_ketu_tail_1", CubeListBuilder.create()
                .texOffs(43, 156).mirror(false).addBox(-4.875f, -0.13f, 0, 9.75f, 0.26f, 10.4f)
                .texOffs(86, 68).mirror(false).addBox(-0.13f, -4.875f, 0, 0.26f, 9.75f, 10.4f)
                .texOffs(80, 127).mirror(false).addBox(-1.17f, -1.17f, 0, 2.34f, 2.34f, 10.4f), PartPose.offsetAndRotation(0, 0, 8.58f, -0.22f, 0.03f, 0));
        PartDefinition emblemKetuTail2 = emblemKetuTail1.addOrReplaceChild("emblem_ketu_tail_2", CubeListBuilder.create()
                .texOffs(0, 142).mirror(false).addBox(-6.175f, -0.13f, 0, 12.35f, 0.26f, 11.05f)
                .texOffs(179, 0).mirror(false).addBox(-0.13f, -6.175f, 0, 0.26f, 12.35f, 11.05f), PartPose.offsetAndRotation(0, 0, 9.88f, -0.22f, 0.03f, 0));
        PartDefinition emblemKetuTail3 = emblemKetuTail2.addOrReplaceChild("emblem_ketu_tail_3", CubeListBuilder.create()
                .texOffs(51, 142).mirror(false).addBox(-7.15f, -0.13f, 0, 14.3f, 0.26f, 11.7f)
                .texOffs(107, 0).mirror(false).addBox(-0.13f, -7.15f, 0, 0.26f, 14.3f, 11.7f), PartPose.offsetAndRotation(0, 0, 10.53f, -0.2f, 0.03f, 0));
        PartDefinition emblemKetuTail4 = emblemKetuTail3.addOrReplaceChild("emblem_ketu_tail_4", CubeListBuilder.create()
                .texOffs(106, 142).mirror(false).addBox(-8.125f, -0.13f, 0, 16.25f, 0.26f, 11.7f)
                .texOffs(80, 0).mirror(false).addBox(-0.13f, -8.125f, 0, 0.26f, 16.25f, 11.7f), PartPose.offsetAndRotation(0, 0, 11.18f, -0.16f, 0.03f, 0));
        PartDefinition emblemKetuIon0 = emblemKetu.addOrReplaceChild("emblem_ketu_ion_0", CubeListBuilder.create()
                .texOffs(165, 142).mirror(false).addBox(-2.21f, -0.13f, 0, 4.42f, 0.26f, 11.7f)
                .texOffs(163, 90).mirror(false).addBox(-0.13f, -2.21f, 0, 0.26f, 4.42f, 11.7f), PartPose.offsetAndRotation(0, -11.7f, 1.3f, 0.95f, 0.5f, 0));
        PartDefinition emblemKetuIon1 = emblemKetuIon0.addOrReplaceChild("emblem_ketu_ion_1", CubeListBuilder.create()
                .texOffs(200, 142).mirror(false).addBox(-3.25f, -0.13f, 0, 6.5f, 0.26f, 11.7f)
                .texOffs(39, 90).mirror(false).addBox(-0.13f, -3.25f, 0, 0.26f, 6.5f, 11.7f), PartPose.offsetAndRotation(0, 0, 11.18f, -0.12f, 0.06f, 0));
        PartDefinition emblemKetuIon2 = emblemKetuIon1.addOrReplaceChild("emblem_ketu_ion_2", CubeListBuilder.create()
                .texOffs(86, 156).mirror(false).addBox(-4.16f, -0.13f, 0, 8.32f, 0.26f, 10.4f)
                .texOffs(144, 68).mirror(false).addBox(-0.13f, -4.16f, 0, 0.26f, 8.32f, 10.4f), PartPose.offsetAndRotation(0, 0, 11.18f, -0.12f, 0.06f, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED luminaries: end
    }

    /**
     * A star lord's pose: floating, his sleeves and ribbon stirring; his emblem held up in his left hand; a
     * blow, the tablet brought down and the emblem thrust forward; winding up his starfall, the emblem lifted
     * high over his head, his right arm flung out, his face turned up.
     */
    public static final class StarLordModel extends GodModel<StarLordEntity> {
        /** How much larger than his pixels he is drawn: some 2.3 blocks to his crown (heaven_gods.py prints his reach). */
        public static final float SIZE = 0.7f;
        private static final String[] NAMES = StarLordEntity.LUMINARIES;
        // who wears a beard, who frowns under drawn brows, whose hair hangs at her cheeks, who holds the tablet
        private static final boolean[] BEARD = {true, false, true, true, true, false, true, true, false};
        private static final boolean[] BROWS = {true, false, true, true, false, false, true, true, true};
        private static final boolean[] LOCKS = {false, true, false, false, false, true, false, false, false};
        private static final boolean[] TABLET = {true, true, true, false, true, true, false, false, false};
        private static final boolean[] FIERCE = {false, false, false, true, false, false, false, true, true};

        private final ModelPart body, head, beard, rightBeard, leftBeard, rightMoustache, leftMoustache, rightBrow, leftBrow,
                rightLock, leftLock, rightArm, leftArm, rightForearm, leftForearm, rightSleeve, leftSleeve, tablet, brush,
                apron, arch, rightRibbon, leftRibbon, rightRibbon2, leftRibbon2, rightBoot, leftBoot;
        private final ModelPart[] emblems = new ModelPart[9], dresses = new ModelPart[9];

        public StarLordModel(ModelPart root) {
            super(root);
            body = root.getChild("body");
            ModelPart skirt = body.getChild("skirt");
            head = body.getChild("head");
            ModelPart jaw = head.getChild("jaw");
            beard = jaw.getChild("beard");
            rightBeard = jaw.getChild("right_beard");
            leftBeard = jaw.getChild("left_beard");
            rightMoustache = head.getChild("right_moustache");
            leftMoustache = head.getChild("left_moustache");
            rightBrow = head.getChild("right_brow");
            leftBrow = head.getChild("left_brow");
            rightLock = head.getChild("right_sidelock");
            leftLock = head.getChild("left_sidelock");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightSleeve = rightForearm.getChild("right_sleeve");
            leftSleeve = leftForearm.getChild("left_sleeve");
            ModelPart rightHand = rightForearm.getChild("right_hand"), leftHand = leftForearm.getChild("left_hand");
            tablet = rightHand.getChild("tablet");
            brush = rightHand.getChild("brush");
            apron = skirt.getChild("apron");
            arch = body.getChild("arch");
            rightRibbon = body.getChild("right_ribbon");
            leftRibbon = body.getChild("left_ribbon");
            rightRibbon2 = rightRibbon.getChild("right_ribbon_2");
            leftRibbon2 = leftRibbon.getChild("left_ribbon_2");
            rightBoot = root.getChild("right_boot");
            leftBoot = root.getChild("left_boot");
            for (int i = 0; i < 9; i++) {
                emblems[i] = leftHand.getChild("emblem_" + NAMES[i]);
                dresses[i] = head.getChild("hd_" + NAMES[i]);
            }
        }

        @Override
        public void prepareMobModel(StarLordEntity e, float swing, float amount, float partial) {
            super.prepareMobModel(e, swing, amount, partial);
            int v = Math.floorMod(e.variant(), 9);
            for (int i = 0; i < 9; i++) {
                emblems[i].visible = i == v;
                dresses[i].visible = i == v;
            }
            for (ModelPart p : new ModelPart[]{beard, rightBeard, leftBeard, rightMoustache, leftMoustache}) p.visible = BEARD[v];
            rightBrow.visible = leftBrow.visible = BROWS[v];
            rightLock.visible = leftLock.visible = LOCKS[v];
            tablet.visible = TABLET[v];
            brush.visible = v == StarLordEntity.MERCURY;
            float scowl = FIERCE[v] ? 0.3f : 0f;
            rightBrow.zRot = rightBrow.getInitialPose().zRot + scowl;
            leftBrow.zRot = leftBrow.getInitialPose().zRot - scowl;
        }

        @Override
        public void setupAnim(StarLordEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float drift = Mth.sin(age * 0.07f);
            root.y = drift * 0.6f;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD - wind * 0.3f;
            // the tablet before him, the emblem held up; a blow, the tablet down and the emblem thrust at the foe;
            // winding up, the emblem lifted high over his head and his right arm flung out
            float rx = Mth.cos(age * 0.05f) * 0.03f - strike * 1.3f, rz = wind * 0.7f;
            float lx = -Mth.cos(age * 0.05f) * 0.03f - strike * 0.5f - wind * 2.5f, lz = wind * 0.2f;
            turn(rightArm, rx - wind * 0.5f, rz);
            turn(leftArm, lx, lz);
            turn(rightForearm, strike * 0.5f + wind * 0.6f, 0);
            turn(leftForearm, wind * 2.25f, 0);
            // the great sleeves hang under their weight whatever the arms do
            turn(rightSleeve, -(rx - wind * 0.5f + strike * 0.5f + wind * 0.6f) * 0.85f + drift * 0.04f + amount * 0.2f, 0);
            turn(leftSleeve, -(lx + wind * 2.25f) * 0.85f + Mth.sin(age * 0.07f + 1) * 0.04f + amount * 0.2f, 0);
            turn(beard, Mth.sin(age * 0.05f) * 0.03f + amount * 0.15f, 0);
            turn(rightBeard, 0, Mth.sin(age * 0.045f) * 0.03f);
            turn(leftBeard, 0, -Mth.sin(age * 0.045f + 0.5f) * 0.03f);
            turn(apron, -amount * 0.25f + Mth.sin(age * 0.06f) * 0.03f, 0);
            // the ribbon stirring in the wind of Heaven, streaming back as he flies
            float flow = Mth.sin(age * 0.09f), flow2 = Mth.sin(age * 0.09f + 1.2f);
            turn(arch, flow * 0.04f + amount * 0.25f, 0);
            turn(rightRibbon, 0.1f * flow + amount * 0.5f, -0.06f * flow2);
            turn(leftRibbon, 0.1f * flow2 + amount * 0.5f, 0.06f * flow);
            turn(rightRibbon2, 0.12f * flow2 + amount * 0.3f, -0.08f * flow);
            turn(leftRibbon2, 0.12f * flow + amount * 0.3f, 0.08f * flow2);
            float step = Mth.cos(swing * 0.6f) * amount;
            rightBoot.z = rightBoot.getInitialPose().z + step;
            leftBoot.z = leftBoot.getInitialPose().z - step;
        }
    }

    // ── The Great Spirit Giant ──────────────────────────────────────────

    /**
     * The Great Spirit Giant (256x256): a towering god in gilt-bronze lamellar laced in red, a moulded breastplate
     * with two round mirror plates, a gold beast's face for his buckle, shoulder guards in three tiers bitten by
     * gold lions' heads, tassets over a crimson robe, greaves and armoured boots, a crimson cloak and a red sash
     * looping behind his shoulders; a fierce dark-ruddy face, bulging burning eyes, a roaring mouth, a great black
     * beard flaring in locks; a phoenix-winged helmet with a flame crest and a red tassel; a great broad axe
     * (宣花斧) in each fist, its crescent blade etched with gold flowers. Model and skin:
     * {@code assets_source/heaven_gods_giant.py}.
     */
    public static LayerDefinition giantLayer() {
        // GENERATED giant: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(160, 58).mirror(false).addBox(-8, -3, -4.8f, 16, 5, 9.6f)
                .texOffs(0, 76).mirror(false).addBox(-8.9f, -4.6f, -5.5f, 17.8f, 3.6f, 11)
                .texOffs(79, 120).mirror(false).addBox(-3.6f, -6.4f, -6.7f, 7.2f, 6.6f, 1.6f)
                .texOffs(74, 131).mirror(false).addBox(-2.2f, -0.2f, -6.4f, 4.4f, 2.2f, 1), PartPose.offsetAndRotation(0, -1, 0, 0, 0, 0));
        PartDefinition robeFront = waist.addOrReplaceChild("robe_front", CubeListBuilder.create()
                .texOffs(133, 37).mirror(false).addBox(-5, 0, -0.5f, 10, 18, 1), PartPose.offsetAndRotation(0, 0.5f, -5, -0.06f, 0, 0));
        PartDefinition robeBack = waist.addOrReplaceChild("robe_back", CubeListBuilder.create()
                .texOffs(201, 0).mirror(false).addBox(-8.5f, 0, -0.5f, 17, 19, 1), PartPose.offsetAndRotation(0, 0.5f, 5, 0.1f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(153, 76).mirror(false).addBox(-4.2f, 0, -0.7f, 8.4f, 12, 1.4f), PartPose.offsetAndRotation(-5.15f, -0.6f, -5.5f, -0.12f, 0.15f, 0.1f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(153, 76).mirror(true).addBox(-4.2f, 0, -0.7f, 8.4f, 12, 1.4f), PartPose.offsetAndRotation(5.15f, -0.6f, -5.5f, -0.12f, -0.15f, -0.1f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(176, 0).mirror(false).addBox(-1.4f, 0, -5, 1.4f, 11, 10), PartPose.offsetAndRotation(-8.9f, -0.6f, 0, 0, 0, 0.2f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(176, 0).mirror(true).addBox(0, 0, -5, 1.4f, 11, 10), PartPose.offsetAndRotation(8.9f, -0.6f, 0, 0, 0, -0.2f));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(59, 76).mirror(false).addBox(-2.6f, 0, -0.4f, 5.2f, 14, 0.8f), PartPose.offsetAndRotation(0, 0.4f, -6, -0.08f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(144, 58).mirror(false).addBox(-1, 0, -0.4f, 2, 15, 0.8f)
                .texOffs(224, 92).mirror(false).addBox(-0.4f, 0.5f, 0.8f, 2, 11, 0.8f), PartPose.offsetAndRotation(-8.1f, -3.4f, -4.4f, 0.04f, 0, 0.12f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(144, 58).mirror(true).addBox(-1, 0, -0.4f, 2, 15, 0.8f)
                .texOffs(224, 92).mirror(true).addBox(-1.6f, 0.5f, 0.8f, 2, 11, 0.8f), PartPose.offsetAndRotation(8.1f, -3.4f, -4.4f, 0.04f, 0, -0.12f));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 37).mirror(false).addBox(-3.5f, 0, -3.5f, 7, 12.5f, 7), PartPose.offsetAndRotation(-4.6f, -1, 0.4f, 0, 0, 0.02f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 37).mirror(true).addBox(-3.5f, 0, -3.5f, 7, 12.5f, 7), PartPose.offsetAndRotation(4.6f, -1, 0.4f, 0, 0, -0.02f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(0, 58).mirror(false).addBox(-3, 0, -3, 6, 11, 6)
                .texOffs(80, 92).mirror(false).addBox(-3.5f, 0.6f, -3.8f, 7, 9, 4)
                .texOffs(0, 131).mirror(false).addBox(-3.3f, -1.8f, -4.2f, 6.6f, 4, 1.6f)
                .texOffs(103, 92).mirror(false).addBox(-3.4f, 10, -5.6f, 6.8f, 3, 9.4f)
                .texOffs(0, 120).mirror(false).addBox(-3.6f, 9.4f, -3.8f, 7.2f, 1.2f, 7.6f), PartPose.offsetAndRotation(0, 12, 0, 0, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(0, 58).mirror(true).addBox(-3, 0, -3, 6, 11, 6)
                .texOffs(80, 92).mirror(true).addBox(-3.5f, 0.6f, -3.8f, 7, 9, 4)
                .texOffs(0, 131).mirror(true).addBox(-3.3f, -1.8f, -4.2f, 6.6f, 4, 1.6f)
                .texOffs(103, 92).mirror(true).addBox(-3.4f, 10, -5.6f, 6.8f, 3, 9.4f)
                .texOffs(0, 120).mirror(true).addBox(-3.6f, 9.4f, -3.8f, 7.2f, 1.2f, 7.6f), PartPose.offsetAndRotation(0, 12, 0, 0, 0, 0));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(78, 0).mirror(false).addBox(-9.5f, -20, -5.6f, 19, 12, 11.2f)
                .texOffs(156, 37).mirror(false).addBox(-8.3f, -8.4f, -5, 16.6f, 8.4f, 10)
                .texOffs(9, 0).mirror(false).addBox(-10.1f, -20.4f, -6.4f, 20.2f, 12.6f, 12.8f)
                .texOffs(29, 37).mirror(false).addBox(-8.9f, -8.4f, -5.8f, 17.8f, 7.6f, 11.6f)
                .texOffs(177, 120).mirror(false).addBox(-8.3f, -18.6f, -7, 7, 7, 0.8f)
                .texOffs(177, 120).mirror(false).addBox(1.3f, -18.6f, -7, 7, 7, 0.8f)
                .texOffs(138, 92).mirror(false).addBox(-6, -22.2f, -5, 12, 2.6f, 10)
                .texOffs(176, 76).mirror(false).addBox(-10.3f, -20.8f, -6, 20.6f, 1.4f, 12)
                .texOffs(100, 120).mirror(false).addBox(-2.6f, -22.8f, -2.6f, 5.2f, 3, 5.2f), PartPose.offsetAndRotation(0, -4, 0, 0, 0, 0));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(25, 58).mirror(false).addBox(-10.1f, 0, -0.5f, 20.2f, 16, 1), PartPose.offsetAndRotation(0, -20.4f, 6.8f, 0.12f, 0, 0));
        PartDefinition rightCapeLower = cape.addOrReplaceChild("right_cape_lower", CubeListBuilder.create()
                .texOffs(70, 58).mirror(false).addBox(-5.35f, 0, -0.5f, 10.1f, 16, 1), PartPose.offsetAndRotation(-4.75f, 15.6f, 0, 0.1f, 0, 0.06f));
        PartDefinition leftCapeLower = cape.addOrReplaceChild("left_cape_lower", CubeListBuilder.create()
                .texOffs(70, 58).mirror(true).addBox(-4.75f, 0, -0.5f, 10.1f, 16, 1), PartPose.offsetAndRotation(4.75f, 15.6f, 0, 0.1f, 0, -0.06f));
        PartDefinition loop = body.addOrReplaceChild("loop", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -19, 8.2f, 0.15f, 0, 0));
        PartDefinition loop0 = loop.addOrReplaceChild("loop_0", CubeListBuilder.create()
                .texOffs(87, 131).mirror(false).addBox(-2.6f, -1.4f, -0.3f, 5.2f, 2.8f, 0.6f), PartPose.offsetAndRotation(-14.7721f, -1.4326f, 0, 0, 0, -1.2606f));
        PartDefinition loop1 = loop.addOrReplaceChild("loop_1", CubeListBuilder.create()
                .texOffs(87, 131).mirror(false).addBox(-2.6f, -1.4f, -0.3f, 5.2f, 2.8f, 0.6f), PartPose.offsetAndRotation(-13.3651f, -3.7454f, 0, 0, 0, -0.8236f));
        PartDefinition loop2 = loop.addOrReplaceChild("loop_2", CubeListBuilder.create()
                .texOffs(87, 131).mirror(false).addBox(-2.6f, -1.4f, -0.3f, 5.2f, 2.8f, 0.6f), PartPose.offsetAndRotation(-10.7901f, -5.7309f, 0, 0, 0, -0.5177f));
        PartDefinition loop3 = loop.addOrReplaceChild("loop_3", CubeListBuilder.create()
                .texOffs(87, 131).mirror(false).addBox(-2.6f, -1.4f, -0.3f, 5.2f, 2.8f, 0.6f), PartPose.offsetAndRotation(-7.2721f, -7.2156f, 0, 0, 0, -0.2959f));
        PartDefinition loop4 = loop.addOrReplaceChild("loop_4", CubeListBuilder.create()
                .texOffs(87, 131).mirror(false).addBox(-2.6f, -1.4f, -0.3f, 5.2f, 2.8f, 0.6f), PartPose.offsetAndRotation(-3.1187f, -8.0697f, 0, 0, 0, -0.1164f));
        PartDefinition loop5 = loop.addOrReplaceChild("loop_5", CubeListBuilder.create()
                .texOffs(87, 131).mirror(false).addBox(-2.6f, -1.4f, -0.3f, 5.2f, 2.8f, 0.6f), PartPose.offsetAndRotation(1.3073f, -8.2186f, 0, 0, 0, 0.0481f));
        PartDefinition loop6 = loop.addOrReplaceChild("loop_6", CubeListBuilder.create()
                .texOffs(87, 131).mirror(false).addBox(-2.6f, -1.4f, -0.3f, 5.2f, 2.8f, 0.6f), PartPose.offsetAndRotation(5.6191f, -7.6493f, 0, 0, 0, 0.2187f));
        PartDefinition loop7 = loop.addOrReplaceChild("loop_7", CubeListBuilder.create()
                .texOffs(87, 131).mirror(false).addBox(-2.6f, -1.4f, -0.3f, 5.2f, 2.8f, 0.6f), PartPose.offsetAndRotation(9.4398f, -6.4115f, 0, 0, 0, 0.419f));
        PartDefinition loop8 = loop.addOrReplaceChild("loop_8", CubeListBuilder.create()
                .texOffs(87, 131).mirror(false).addBox(-2.6f, -1.4f, -0.3f, 5.2f, 2.8f, 0.6f), PartPose.offsetAndRotation(12.4356f, -4.6133f, 0, 0, 0, 0.6841f));
        PartDefinition loop9 = loop.addOrReplaceChild("loop_9", CubeListBuilder.create()
                .texOffs(87, 131).mirror(false).addBox(-2.6f, -1.4f, -0.3f, 5.2f, 2.8f, 0.6f), PartPose.offsetAndRotation(14.3446f, -2.4121f, 0, 0, 0, 1.0635f));
        PartDefinition rightStreamer = body.addOrReplaceChild("right_streamer", CubeListBuilder.create()
                .texOffs(151, 58).mirror(false).addBox(-1.4f, 0, -0.3f, 2.8f, 15, 0.6f), PartPose.offsetAndRotation(-14.6f, -19.6f, 8.2f, 0.12f, 0.3f, 0.18f));
        PartDefinition leftStreamer = body.addOrReplaceChild("left_streamer", CubeListBuilder.create()
                .texOffs(151, 58).mirror(true).addBox(-1.4f, 0, -0.3f, 2.8f, 15, 0.6f), PartPose.offsetAndRotation(14.6f, -19.6f, 8.2f, 0.12f, -0.3f, -0.18f));
        PartDefinition rightStreamer2 = rightStreamer.addOrReplaceChild("right_streamer_2", CubeListBuilder.create()
                .texOffs(74, 76).mirror(false).addBox(-1.5f, 0, -0.3f, 3, 14, 0.6f), PartPose.offsetAndRotation(0, 14.6f, 0, 0.2f, 0, 0.18f));
        PartDefinition leftStreamer2 = leftStreamer.addOrReplaceChild("left_streamer_2", CubeListBuilder.create()
                .texOffs(74, 76).mirror(true).addBox(-1.5f, 0, -0.3f, 3, 14, 0.6f), PartPose.offsetAndRotation(0, 14.6f, 0, 0.2f, 0, -0.18f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(211, 37).mirror(false).addBox(-3.2f, -2, -3.2f, 6.4f, 11, 6.4f)
                .texOffs(33, 120).mirror(false).addBox(-3.6f, 6.6f, -3.6f, 7.2f, 1.6f, 7.2f), PartPose.offsetAndRotation(-12.5f, -18, 0, -0.12f, 0, 0.2f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(211, 37).mirror(true).addBox(-3.2f, -2, -3.2f, 6.4f, 11, 6.4f)
                .texOffs(33, 120).mirror(true).addBox(-3.6f, 6.6f, -3.6f, 7.2f, 1.6f, 7.2f), PartPose.offsetAndRotation(12.5f, -18, 0, -0.12f, 0, -0.2f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(0, 107).mirror(false).addBox(-5, -1.4f, -4.6f, 8, 2, 9.2f)
                .texOffs(183, 92).mirror(false).addBox(-5.8f, 0.4f, -5.1f, 8.6f, 2, 10.2f)
                .texOffs(83, 76).mirror(false).addBox(-6.6f, 2.2f, -5.6f, 9.2f, 2.4f, 11.2f)
                .texOffs(158, 107).mirror(false).addBox(-3.2f, -2.8f, -4.2f, 1.6f, 1.6f, 8.4f)
                .texOffs(37, 107).mirror(false).addBox(-7.4f, -0.4f, -2.6f, 1.2f, 5.2f, 5.2f)
                .texOffs(19, 131).mirror(false).addBox(-7.9f, 2, -1.8f, 0.6f, 1.6f, 3.6f), PartPose.offsetAndRotation(-0.4f, -1.8f, 0, 0, 0, 0.42f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(0, 107).mirror(true).addBox(-3, -1.4f, -4.6f, 8, 2, 9.2f)
                .texOffs(183, 92).mirror(true).addBox(-2.8f, 0.4f, -5.1f, 8.6f, 2, 10.2f)
                .texOffs(83, 76).mirror(true).addBox(-2.6f, 2.2f, -5.6f, 9.2f, 2.4f, 11.2f)
                .texOffs(158, 107).mirror(true).addBox(1.6f, -2.8f, -4.2f, 1.6f, 1.6f, 8.4f)
                .texOffs(37, 107).mirror(true).addBox(6.2f, -0.4f, -2.6f, 1.2f, 5.2f, 5.2f)
                .texOffs(19, 131).mirror(true).addBox(7.3f, 2, -1.8f, 0.6f, 1.6f, 3.6f), PartPose.offsetAndRotation(0.4f, -1.8f, 0, 0, 0, -0.42f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(128, 76).mirror(false).addBox(-2.8f, 0, -2.8f, 5.6f, 9, 5.6f)
                .texOffs(0, 92).mirror(false).addBox(-3.3f, 0.8f, -3.3f, 6.6f, 7, 6.6f), PartPose.offsetAndRotation(0, 9, 0, -0.75f, -0.15f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(128, 76).mirror(true).addBox(-2.8f, 0, -2.8f, 5.6f, 9, 5.6f)
                .texOffs(0, 92).mirror(true).addBox(-3.3f, 0.8f, -3.3f, 6.6f, 7, 6.6f), PartPose.offsetAndRotation(0, 9, 0, -0.75f, 0.15f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(181, 107).mirror(false).addBox(-2.6f, 0, -2.6f, 5.2f, 5, 5.2f), PartPose.offsetAndRotation(0, 9, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(181, 107).mirror(true).addBox(-2.6f, 0, -2.6f, 5.2f, 5, 5.2f), PartPose.offsetAndRotation(0, 9, 0, 0, 0, 0));
        PartDefinition rightAxe = rightHand.addOrReplaceChild("right_axe", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.8f, -25, -0.8f, 1.6f, 34, 1.6f)
                .texOffs(30, 131).mirror(false).addBox(-1.3f, -26.4f, -1.3f, 2.6f, 2.4f, 2.6f)
                .texOffs(30, 131).mirror(false).addBox(-1.4f, -16.4f, -1.4f, 2.8f, 2.2f, 2.8f)
                .texOffs(43, 131).mirror(false).addBox(-0.5f, -30.6f, -0.5f, 1, 4.4f, 1)
                .texOffs(61, 131).mirror(false).addBox(-1.1f, 8.6f, -1.1f, 2.2f, 2, 2.2f)
                .texOffs(194, 120).mirror(false).addBox(-1.6f, -14.8f, -1.6f, 3.2f, 4, 3.2f), PartPose.offsetAndRotation(0, 2.6f, 0, 0.8616f, 0.2992f, -0.0639f));
        PartDefinition leftAxe = leftHand.addOrReplaceChild("left_axe", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(-0.8f, -25, -0.8f, 1.6f, 34, 1.6f)
                .texOffs(30, 131).mirror(true).addBox(-1.3f, -26.4f, -1.3f, 2.6f, 2.4f, 2.6f)
                .texOffs(30, 131).mirror(true).addBox(-1.4f, -16.4f, -1.4f, 2.8f, 2.2f, 2.8f)
                .texOffs(43, 131).mirror(true).addBox(-0.5f, -30.6f, -0.5f, 1, 4.4f, 1)
                .texOffs(61, 131).mirror(true).addBox(-1.1f, 8.6f, -1.1f, 2.2f, 2, 2.2f)
                .texOffs(194, 120).mirror(true).addBox(-1.6f, -14.8f, -1.6f, 3.2f, 4, 3.2f), PartPose.offsetAndRotation(0, 2.6f, 0, 0.8616f, -0.2992f, 0.0639f));
        PartDefinition rightBlade = rightAxe.addOrReplaceChild("right_blade", CubeListBuilder.create()
                .texOffs(141, 0).mirror(false).addBox(-15.5f, -11, -0.5f, 15.5f, 22, 1)
                .texOffs(125, 120).mirror(false).addBox(-1.4f, -3.4f, -0.9f, 1.4f, 6.8f, 1.8f), PartPose.offsetAndRotation(-1, -19.5f, 0, 0, 0, 0));
        PartDefinition leftBlade = leftAxe.addOrReplaceChild("left_blade", CubeListBuilder.create()
                .texOffs(141, 0).mirror(true).addBox(0, -11, -0.5f, 15.5f, 22, 1)
                .texOffs(125, 120).mirror(true).addBox(0, -3.4f, -0.9f, 1.4f, 6.8f, 1.8f), PartPose.offsetAndRotation(1, -19.5f, 0, 0, 0, 0));
        PartDefinition rightSpike = rightAxe.addOrReplaceChild("right_spike", CubeListBuilder.create()
                .texOffs(132, 131).mirror(false).addBox(0, -0.8f, -0.5f, 4.2f, 1.6f, 1), PartPose.offsetAndRotation(0.8f, -21, 0, 0, 0, 0));
        PartDefinition leftSpike = leftAxe.addOrReplaceChild("left_spike", CubeListBuilder.create()
                .texOffs(132, 131).mirror(true).addBox(-4.2f, -0.8f, -0.5f, 4.2f, 1.6f, 1), PartPose.offsetAndRotation(-0.8f, -21, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(90, 37).mirror(false).addBox(-5.4f, -11, -5, 10.8f, 10, 10)
                .texOffs(48, 131).mirror(false).addBox(-1.6f, -6.6f, -6.2f, 3.2f, 3.2f, 1.4f)
                .texOffs(145, 131).mirror(false).addBox(-2.6f, -4.4f, -5.6f, 5.2f, 1.4f, 0.8f), PartPose.offsetAndRotation(0, -22.4f, -0.6f, 0, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(160, 131).mirror(false).addBox(-1.3f, -1, -0.6f, 2.6f, 2, 0.8f), PartPose.offsetAndRotation(-2.6f, -7.2f, -5.1f, 0, 0, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(160, 131).mirror(true).addBox(-1.3f, -1, -0.6f, 2.6f, 2, 0.8f), PartPose.offsetAndRotation(2.6f, -7.2f, -5.1f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(102, 131).mirror(false).addBox(-4.4f, -1.1f, -0.7f, 4.4f, 1.6f, 1.4f), PartPose.offsetAndRotation(-0.2f, -8.6f, -5.2f, 0, 0.12f, 0.38f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(102, 131).mirror(true).addBox(0, -1.1f, -0.7f, 4.4f, 1.6f, 1.4f), PartPose.offsetAndRotation(0.2f, -8.6f, -5.2f, 0, -0.12f, -0.38f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(237, 120).mirror(false).addBox(-1, -1.6f, -1.2f, 1, 3.6f, 2.4f), PartPose.offsetAndRotation(-5.4f, -6.6f, -0.6f, 0, -0.25f, 0));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(237, 120).mirror(true).addBox(0, -1.6f, -1.2f, 1, 3.6f, 2.4f), PartPose.offsetAndRotation(5.4f, -6.6f, -0.6f, 0, 0.25f, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(54, 107).mirror(false).addBox(-4.9f, 0, -4.2f, 9.8f, 3, 8.4f), PartPose.offsetAndRotation(0, -1, -0.6f, 0.06f, 0, 0));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(117, 131).mirror(false).addBox(-4.6f, -0.7f, -0.7f, 4.6f, 1.4f, 1.4f), PartPose.offsetAndRotation(-0.8f, -3.4f, -5.6f, 0, 0.25f, -0.35f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(117, 131).mirror(true).addBox(0, -0.7f, -0.7f, 4.6f, 1.4f, 1.4f), PartPose.offsetAndRotation(0.8f, -3.4f, -5.6f, 0, -0.25f, 0.35f));
        PartDefinition rightWhisker = head.addOrReplaceChild("right_whisker", CubeListBuilder.create()
                .texOffs(66, 120).mirror(false).addBox(-1.4f, 0, -1.2f, 2.8f, 7, 2.4f), PartPose.offsetAndRotation(-4.8f, -4.2f, -3.2f, 0.1f, 0.2f, 0.55f));
        PartDefinition leftWhisker = head.addOrReplaceChild("left_whisker", CubeListBuilder.create()
                .texOffs(66, 120).mirror(true).addBox(-1.4f, 0, -1.2f, 2.8f, 7, 2.4f), PartPose.offsetAndRotation(4.8f, -4.2f, -3.2f, 0.1f, -0.2f, -0.55f));
        PartDefinition beard = jaw.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(93, 107).mirror(false).addBox(-1.9f, 0, -1.2f, 3.8f, 9, 2.4f), PartPose.offsetAndRotation(0, 2.6f, -3.8f, 0.22f, 0, 0));
        PartDefinition beardTip = beard.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(246, 120).mirror(false).addBox(-1.4f, 0, -1, 2.8f, 5, 2), PartPose.offsetAndRotation(0, 8.6f, 0, -0.15f, 0, 0));
        PartDefinition rightBeard = jaw.addOrReplaceChild("right_beard", CubeListBuilder.create()
                .texOffs(108, 107).mirror(false).addBox(-1.5f, 0, -1.1f, 3, 8.5f, 2.2f), PartPose.offsetAndRotation(-2.8f, 2.2f, -3.4f, 0.2f, 0.15f, 0.35f));
        PartDefinition leftBeard = jaw.addOrReplaceChild("left_beard", CubeListBuilder.create()
                .texOffs(108, 107).mirror(true).addBox(-1.5f, 0, -1.1f, 3, 8.5f, 2.2f), PartPose.offsetAndRotation(2.8f, 2.2f, -3.4f, 0.2f, -0.15f, -0.35f));
        PartDefinition rightBeardOut = jaw.addOrReplaceChild("right_beard_out", CubeListBuilder.create()
                .texOffs(134, 120).mirror(false).addBox(-1.3f, 0, -1, 2.6f, 7, 2), PartPose.offsetAndRotation(-4.4f, 1.4f, -2, 0.1f, 0.2f, 0.7f));
        PartDefinition leftBeardOut = jaw.addOrReplaceChild("left_beard_out", CubeListBuilder.create()
                .texOffs(134, 120).mirror(true).addBox(-1.3f, 0, -1, 2.6f, 7, 2), PartPose.offsetAndRotation(4.4f, 1.4f, -2, 0.1f, -0.2f, -0.7f));
        PartDefinition helm = head.addOrReplaceChild("helm", CubeListBuilder.create()
                .texOffs(95, 58).mirror(false).addBox(-5.9f, -4.6f, -5.6f, 11.8f, 5, 11.2f)
                .texOffs(206, 107).mirror(false).addBox(-4.6f, -6.4f, -4.4f, 9.2f, 2, 8.8f)
                .texOffs(29, 92).mirror(false).addBox(-6.4f, -0.6f, -6, 12.8f, 1.8f, 12)
                .texOffs(211, 120).mirror(false).addBox(-0.7f, -11.6f, -0.7f, 1.4f, 5.6f, 1.4f)
                .texOffs(220, 120).mirror(false).addBox(-2, -9.6f, -2, 4, 3.6f, 4)
                .texOffs(121, 107).mirror(false).addBox(-5.8f, 0.6f, 1.4f, 11.6f, 6, 5.2f), PartPose.offsetAndRotation(0, -11, 0, 0, 0, 0));
        PartDefinition crest = helm.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(145, 120).mirror(false).addBox(-3, -7.6f, -0.4f, 6, 7.6f, 0.8f), PartPose.offsetAndRotation(0, -0.4f, -6.1f, -0.12f, 0, 0));
        PartDefinition rightHelmWing = helm.addOrReplaceChild("right_helm_wing", CubeListBuilder.create()
                .texOffs(160, 120).mirror(false).addBox(-7, -7, -0.3f, 7, 8, 0.6f), PartPose.offsetAndRotation(-5.6f, -2.4f, 0.4f, 0, 0.15f, -0.35f));
        PartDefinition leftHelmWing = helm.addOrReplaceChild("left_helm_wing", CubeListBuilder.create()
                .texOffs(160, 120).mirror(true).addBox(0, -7, -0.3f, 7, 8, 0.6f), PartPose.offsetAndRotation(5.6f, -2.4f, 0.4f, 0, -0.15f, 0.35f));
        PartDefinition rightPlumeHair = helm.addOrReplaceChild("right_plume_hair", CubeListBuilder.create()
                .texOffs(245, 107).mirror(false).addBox(-1.2f, -0.2f, -0.8f, 2.4f, 9, 1.6f), PartPose.offsetAndRotation(0, -8.4f, 0.6f, -0.6f, 0, -0.25f));
        PartDefinition leftPlumeHair = helm.addOrReplaceChild("left_plume_hair", CubeListBuilder.create()
                .texOffs(245, 107).mirror(true).addBox(-1.2f, -0.2f, -0.8f, 2.4f, 9, 1.6f), PartPose.offsetAndRotation(0, -8.4f, 0.6f, -0.6f, 0, 0.25f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED giant: end
    }

    /**
     * The Giant's pose: floating, his axes upright in his fists; a blow, both axes heaved up and brought down
     * together; winding up his axe-quake, both axes heaved high over his head and cocked behind it, his body
     * drawn back, his face turned up roaring.
     */
    public static final class GiantModel extends GodModel<GreatSpiritGiantEntity> {
        /** How much larger than his pixels he is drawn: some 4.2 blocks to his helmet's tassel. */
        public static final float SIZE = 0.96f;
        private final ModelPart body, head, jaw, beard, rightArm, leftArm, rightForearm, leftForearm, cape, rightCape, leftCape,
                loop, rightStreamer, leftStreamer, rightStreamer2, leftStreamer2, rightLeg, leftLeg, rightSash, leftSash, robe, apron;

        public GiantModel(ModelPart root) {
            super(root);
            ModelPart waist = root.getChild("waist");
            body = waist.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            beard = jaw.getChild("beard");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            cape = body.getChild("cape");
            rightCape = cape.getChild("right_cape_lower");
            leftCape = cape.getChild("left_cape_lower");
            loop = body.getChild("loop");
            rightStreamer = body.getChild("right_streamer");
            leftStreamer = body.getChild("left_streamer");
            rightStreamer2 = rightStreamer.getChild("right_streamer_2");
            leftStreamer2 = leftStreamer.getChild("left_streamer_2");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightSash = waist.getChild("right_sash");
            leftSash = waist.getChild("left_sash");
            robe = waist.getChild("robe_back");
            apron = waist.getChild("apron");
        }

        @Override
        public void setupAnim(GreatSpiritGiantEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float drift = Mth.sin(age * 0.06f);
            root.y = drift * 0.8f;
            head.yRot = Mth.clamp(yaw, -50, 50) * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.6f - wind * 0.3f;
            // roaring as he heaves
            jaw.xRot = jaw.getInitialPose().xRot + wind * 0.3f + strike * 0.25f;
            turn(body, -wind * 0.14f + strike * 0.12f, 0);
            float breathe = Mth.cos(age * 0.05f) * 0.03f;
            // a blow: both axes swung up and down; winding up, both heaved high over his head and cocked behind it
            // (raised out to either side in a great V, the axes flaring above his shoulders)
            float up = wind * 1.7f + strike * 2.1f;
            turn(rightArm, breathe - up, wind * 1.15f);
            turn(leftArm, breathe - up, -wind * 1.15f);
            turn(rightForearm, -wind * 0.2f + strike * 0.4f, 0, -wind * 0.95f);
            turn(leftForearm, -wind * 0.2f + strike * 0.4f, 0, wind * 0.95f);
            turn(beard, Mth.sin(age * 0.05f) * 0.04f + amount * 0.15f + wind * 0.2f, 0);
            // the cloak, sash and streamers stirring in the wind of Heaven
            float flow = Mth.sin(age * 0.08f), flow2 = Mth.sin(age * 0.08f + 1.3f);
            turn(cape, flow * 0.03f + amount * 0.3f + wind * 0.12f, 0);
            turn(rightCape, flow2 * 0.06f + amount * 0.2f, 0, flow * 0.03f);
            turn(leftCape, flow * 0.06f + amount * 0.2f, 0, -flow2 * 0.03f);
            turn(loop, flow * 0.05f + amount * 0.2f, 0);
            turn(rightStreamer, flow * 0.1f + amount * 0.5f, -flow2 * 0.06f);
            turn(leftStreamer, flow2 * 0.1f + amount * 0.5f, flow * 0.06f);
            turn(rightStreamer2, flow2 * 0.12f + amount * 0.3f, -flow * 0.08f);
            turn(leftStreamer2, flow * 0.12f + amount * 0.3f, flow2 * 0.08f);
            turn(rightSash, flow * 0.05f + amount * 0.3f, 0);
            turn(leftSash, flow2 * 0.05f + amount * 0.3f, 0);
            turn(robe, amount * 0.3f + flow * 0.02f, 0);
            turn(apron, -amount * 0.2f + flow2 * 0.03f, 0);
            // his legs braced on the cloud, shifting as he rides
            float step = Mth.cos(swing * 0.5f) * 0.25f * amount;
            turn(rightLeg, step, 0);
            turn(leftLeg, -step, 0);
        }
    }

    // ── The Thunder Lord and the Lightning Mother ───────────────────────

    /**
     * The Thunder Lord (256x256): a blue, bare-chested bird-demon god, a hooked gold beak, round crackling eyes, a
     * red mane standing up in flames from a gold band, a capelet of dark feathers, a tiger-skin kilt, a bird's
     * scaled legs and talons, great dark feathered wings, and behind him the ring of drums on a gold hoop arching
     * over his head, their heads marked with crackling thunder-spirals; a mallet in his right hand, a chisel in his
     * left. Model and skin: {@code assets_source/heaven_gods_thunder.py}.
     */
    public static LayerDefinition thunderLayer() {
        // GENERATED thunder: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(154, 19).mirror(false).addBox(-4.6f, -2, -3.4f, 9.2f, 4, 6.8f)
                .texOffs(0, 34).mirror(false).addBox(-5.2f, -2.6f, -3.9f, 10.4f, 2, 7.8f)
                .texOffs(140, 45).mirror(false).addBox(-1.8f, -3.2f, -4.6f, 3.6f, 3.2f, 1), PartPose.offsetAndRotation(0, 2, 0, 0, 0, 0));
        PartDefinition kilt = waist.addOrReplaceChild("kilt", CubeListBuilder.create()
                .texOffs(41, 0).mirror(false).addBox(-5.6f, 0, -4.2f, 11.2f, 6.5f, 8.4f), PartPose.offsetAndRotation(0, -0.6f, 0, 0, 0, 0));
        PartDefinition kiltFlap = waist.addOrReplaceChild("kilt_flap", CubeListBuilder.create()
                .texOffs(39, 34).mirror(false).addBox(-2.8f, 0, -0.4f, 5.6f, 9, 0.8f), PartPose.offsetAndRotation(0, -0.4f, -4.3f, -0.1f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(189, 19).mirror(false).addBox(-0.9f, 0, -0.35f, 1.8f, 10, 0.7f), PartPose.offsetAndRotation(-4.6f, -1.8f, -2.6f, 0.05f, 0, 0.18f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(189, 19).mirror(true).addBox(-0.9f, 0, -0.35f, 1.8f, 10, 0.7f), PartPose.offsetAndRotation(4.6f, -1.8f, -2.6f, 0.05f, 0, -0.18f));
        PartDefinition rightThigh = waist.addOrReplaceChild("right_thigh", CubeListBuilder.create()
                .texOffs(182, 0).mirror(false).addBox(-2.3f, -0.5f, -2.3f, 4.6f, 8.5f, 4.6f), PartPose.offsetAndRotation(-2.8f, 1, 0.2f, -0.4f, 0, 0.06f));
        PartDefinition leftThigh = waist.addOrReplaceChild("left_thigh", CubeListBuilder.create()
                .texOffs(182, 0).mirror(true).addBox(-2.3f, -0.5f, -2.3f, 4.6f, 8.5f, 4.6f), PartPose.offsetAndRotation(2.8f, 1, 0.2f, -0.4f, 0, -0.06f));
        PartDefinition rightShin = rightThigh.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(47, 19).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 8.4f, 3), PartPose.offsetAndRotation(0, 7.6f, 0, 0.85f, 0, 0));
        PartDefinition leftShin = leftThigh.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(47, 19).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 8.4f, 3), PartPose.offsetAndRotation(0, 7.6f, 0, 0.85f, 0, 0));
        PartDefinition rightTarsus = rightShin.addOrReplaceChild("right_tarsus", CubeListBuilder.create()
                .texOffs(115, 34).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 5.6f, 2.4f)
                .texOffs(151, 45).mirror(false).addBox(-1.4f, 4.6f, -1.6f, 2.8f, 1.6f, 3), PartPose.offsetAndRotation(0, 8, 0, -0.45f, 0, 0));
        PartDefinition leftTarsus = leftShin.addOrReplaceChild("left_tarsus", CubeListBuilder.create()
                .texOffs(115, 34).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 5.6f, 2.4f)
                .texOffs(151, 45).mirror(true).addBox(-1.4f, 4.6f, -1.6f, 2.8f, 1.6f, 3), PartPose.offsetAndRotation(0, 8, 0, -0.45f, 0, 0));
        PartDefinition rightToeA = rightTarsus.addOrReplaceChild("right_toe_a", CubeListBuilder.create()
                .texOffs(47, 45).mirror(false).addBox(-0.6f, -0.6f, -4.2f, 1.2f, 1.2f, 4.2f)
                .texOffs(173, 45).mirror(false).addBox(-0.5f, -0.2f, -5.4f, 1, 1.4f, 1.4f), PartPose.offsetAndRotation(-0.8f, 5.6f, -1, 0.05f, 0.3f, 0));
        PartDefinition leftToeA = leftTarsus.addOrReplaceChild("left_toe_a", CubeListBuilder.create()
                .texOffs(47, 45).mirror(true).addBox(-0.6f, -0.6f, -4.2f, 1.2f, 1.2f, 4.2f)
                .texOffs(173, 45).mirror(true).addBox(-0.5f, -0.2f, -5.4f, 1, 1.4f, 1.4f), PartPose.offsetAndRotation(0.8f, 5.6f, -1, 0.05f, -0.3f, 0));
        PartDefinition rightToeB = rightTarsus.addOrReplaceChild("right_toe_b", CubeListBuilder.create()
                .texOffs(47, 45).mirror(false).addBox(-0.6f, -0.6f, -4.8f, 1.2f, 1.2f, 4.8f)
                .texOffs(173, 45).mirror(false).addBox(-0.5f, -0.2f, -6, 1, 1.4f, 1.4f), PartPose.offsetAndRotation(0.2f, 5.6f, -1.2f, 0.05f, -0.05f, 0));
        PartDefinition leftToeB = leftTarsus.addOrReplaceChild("left_toe_b", CubeListBuilder.create()
                .texOffs(47, 45).mirror(true).addBox(-0.6f, -0.6f, -4.8f, 1.2f, 1.2f, 4.8f)
                .texOffs(173, 45).mirror(true).addBox(-0.5f, -0.2f, -6, 1, 1.4f, 1.4f), PartPose.offsetAndRotation(-0.2f, 5.6f, -1.2f, 0.05f, 0.05f, 0));
        PartDefinition rightToeC = rightTarsus.addOrReplaceChild("right_toe_c", CubeListBuilder.create()
                .texOffs(180, 45).mirror(false).addBox(-0.5f, -0.5f, 0, 1, 1, 3)
                .texOffs(173, 45).mirror(false).addBox(-0.45f, -0.2f, 2.6f, 0.9f, 1.3f, 1.3f), PartPose.offsetAndRotation(0, 5.6f, 1, -0.05f, 0, 0));
        PartDefinition leftToeC = leftTarsus.addOrReplaceChild("left_toe_c", CubeListBuilder.create()
                .texOffs(180, 45).mirror(true).addBox(-0.5f, -0.5f, 0, 1, 1, 3)
                .texOffs(173, 45).mirror(true).addBox(-0.45f, -0.2f, 2.6f, 0.9f, 1.3f, 1.3f), PartPose.offsetAndRotation(0, 5.6f, 1, -0.05f, 0, 0));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-6, -12.8f, -3.8f, 12, 9.4f, 7.6f)
                .texOffs(60, 19).mirror(false).addBox(-4.8f, -4.2f, -3.2f, 9.6f, 4.6f, 6.4f)
                .texOffs(203, 0).mirror(false).addBox(-7.2f, -13.6f, -4.6f, 14.4f, 3.2f, 9.2f)
                .texOffs(94, 45).mirror(false).addBox(-7.4f, -12, -4.7f, 14.8f, 4, 1.2f)
                .texOffs(128, 34).mirror(false).addBox(-3.8f, -14.2f, -3.6f, 7.6f, 1.4f, 6.4f)
                .texOffs(206, 34).mirror(false).addBox(-2.2f, -15.6f, -2.2f, 4.4f, 2.4f, 4.4f), PartPose.offsetAndRotation(0, -2.4f, 0, 0.08f, 0, 0));
        PartDefinition rightMantleBack = body.addOrReplaceChild("right_mantle_back", CubeListBuilder.create()
                .texOffs(159, 34).mirror(false).addBox(-3.6f, 0, -0.5f, 7.2f, 8, 1), PartPose.offsetAndRotation(-3.6f, -12.4f, 4.4f, 0.15f, 0, 0.05f));
        PartDefinition leftMantleBack = body.addOrReplaceChild("left_mantle_back", CubeListBuilder.create()
                .texOffs(159, 34).mirror(true).addBox(-3.6f, 0, -0.5f, 7.2f, 8, 1), PartPose.offsetAndRotation(3.6f, -12.4f, 4.4f, 0.15f, 0, -0.05f));
        PartDefinition drums = body.addOrReplaceChild("drums", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -11, 8.2f, -0.08f, 0, 0));
        PartDefinition hoop0 = drums.addOrReplaceChild("hoop_0", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(-19.4153f, 4.8008f, 0, 0, 0, 4.47f));
        PartDefinition hoop1 = drums.addOrReplaceChild("hoop_1", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(-19.9915f, 0.5817f, 0, 0, 0, 4.6833f));
        PartDefinition hoop2 = drums.addOrReplaceChild("hoop_2", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(-19.6616f, -3.6638f, 0, 0, 0, 4.8966f));
        PartDefinition hoop3 = drums.addOrReplaceChild("hoop_3", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(-18.4403f, -7.7432f, 0, 0, 0, 5.1099f));
        PartDefinition hoop4 = drums.addOrReplaceChild("hoop_4", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(-16.383f, -11.4715f, 0, 0, 0, 5.3233f));
        PartDefinition hoop5 = drums.addOrReplaceChild("hoop_5", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(-13.5831f, -14.6799f, 0, 0, 0, 5.5366f));
        PartDefinition hoop6 = drums.addOrReplaceChild("hoop_6", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(-10.1675f, -17.2227f, 0, 0, 0, 5.7499f));
        PartDefinition hoop7 = drums.addOrReplaceChild("hoop_7", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(-6.2909f, -18.9849f, 0, 0, 0, 5.9632f));
        PartDefinition hoop8 = drums.addOrReplaceChild("hoop_8", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(-2.1291f, -19.8863f, 0, 0, 0, 6.1765f));
        PartDefinition hoop9 = drums.addOrReplaceChild("hoop_9", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(2.1291f, -19.8863f, 0, 0, 0, 6.3898f));
        PartDefinition hoop10 = drums.addOrReplaceChild("hoop_10", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(6.2909f, -18.9849f, 0, 0, 0, 6.6032f));
        PartDefinition hoop11 = drums.addOrReplaceChild("hoop_11", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(10.1675f, -17.2227f, 0, 0, 0, 6.8165f));
        PartDefinition hoop12 = drums.addOrReplaceChild("hoop_12", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(13.5831f, -14.6799f, 0, 0, 0, 7.0298f));
        PartDefinition hoop13 = drums.addOrReplaceChild("hoop_13", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(16.383f, -11.4715f, 0, 0, 0, 7.2431f));
        PartDefinition hoop14 = drums.addOrReplaceChild("hoop_14", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(18.4403f, -7.7432f, 0, 0, 0, 7.4564f));
        PartDefinition hoop15 = drums.addOrReplaceChild("hoop_15", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(19.6616f, -3.6638f, 0, 0, 0, 7.6698f));
        PartDefinition hoop16 = drums.addOrReplaceChild("hoop_16", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(19.9915f, 0.5817f, 0, 0, 0, 7.8831f));
        PartDefinition hoop17 = drums.addOrReplaceChild("hoop_17", CubeListBuilder.create()
                .texOffs(189, 45).mirror(false).addBox(-2.4291f, -0.7f, -0.7f, 4.8583f, 1.4f, 1.4f), PartPose.offsetAndRotation(19.4153f, 4.8008f, 0, 0, 0, 8.0964f));
        PartDefinition drum0 = drums.addOrReplaceChild("drum_0", CubeListBuilder.create()
                .texOffs(95, 19).mirror(false).addBox(-3.6f, -3.6f, -1.8f, 7.2f, 7.2f, 3.6f)
                .texOffs(178, 34).mirror(false).addBox(-4, -4, -0.5f, 8, 8, 1), PartPose.offsetAndRotation(-18.7939f, 6.8404f, 0, 0, 0, 4.3633f));
        PartDefinition drum1 = drums.addOrReplaceChild("drum_1", CubeListBuilder.create()
                .texOffs(95, 19).mirror(false).addBox(-3.6f, -3.6f, -1.8f, 7.2f, 7.2f, 3.6f)
                .texOffs(178, 34).mirror(false).addBox(-4, -4, -0.5f, 8, 8, 1), PartPose.offsetAndRotation(-19.6325f, -3.8162f, 0, 0, 0, 4.9044f));
        PartDefinition drum2 = drums.addOrReplaceChild("drum_2", CubeListBuilder.create()
                .texOffs(95, 19).mirror(false).addBox(-3.6f, -3.6f, -1.8f, 7.2f, 7.2f, 3.6f)
                .texOffs(178, 34).mirror(false).addBox(-4, -4, -0.5f, 8, 8, 1), PartPose.offsetAndRotation(-14.8629f, -13.3826f, 0, 0, 0, 5.4454f));
        PartDefinition drum3 = drums.addOrReplaceChild("drum_3", CubeListBuilder.create()
                .texOffs(95, 19).mirror(false).addBox(-3.6f, -3.6f, -1.8f, 7.2f, 7.2f, 3.6f)
                .texOffs(178, 34).mirror(false).addBox(-4, -4, -0.5f, 8, 8, 1), PartPose.offsetAndRotation(-5.8474f, -19.1261f, 0, 0, 0, 5.9865f));
        PartDefinition drum4 = drums.addOrReplaceChild("drum_4", CubeListBuilder.create()
                .texOffs(95, 19).mirror(false).addBox(-3.6f, -3.6f, -1.8f, 7.2f, 7.2f, 3.6f)
                .texOffs(178, 34).mirror(false).addBox(-4, -4, -0.5f, 8, 8, 1), PartPose.offsetAndRotation(4.8384f, -19.4059f, 0, 0, 0, 6.5275f));
        PartDefinition drum5 = drums.addOrReplaceChild("drum_5", CubeListBuilder.create()
                .texOffs(95, 19).mirror(false).addBox(-3.6f, -3.6f, -1.8f, 7.2f, 7.2f, 3.6f)
                .texOffs(178, 34).mirror(false).addBox(-4, -4, -0.5f, 8, 8, 1), PartPose.offsetAndRotation(14.1421f, -14.1421f, 0, 0, 0, 7.0686f));
        PartDefinition drum6 = drums.addOrReplaceChild("drum_6", CubeListBuilder.create()
                .texOffs(95, 19).mirror(false).addBox(-3.6f, -3.6f, -1.8f, 7.2f, 7.2f, 3.6f)
                .texOffs(178, 34).mirror(false).addBox(-4, -4, -0.5f, 8, 8, 1), PartPose.offsetAndRotation(19.4059f, -4.8384f, 0, 0, 0, 7.6096f));
        PartDefinition drum7 = drums.addOrReplaceChild("drum_7", CubeListBuilder.create()
                .texOffs(95, 19).mirror(false).addBox(-3.6f, -3.6f, -1.8f, 7.2f, 7.2f, 3.6f)
                .texOffs(178, 34).mirror(false).addBox(-4, -4, -0.5f, 8, 8, 1), PartPose.offsetAndRotation(19.1261f, 5.8474f, 0, 0, 0, 8.1507f));
        PartDefinition drumStrut = body.addOrReplaceChild("drum_strut", CubeListBuilder.create()
                .texOffs(62, 45).mirror(false).addBox(-0.7f, -0.7f, 0, 1.4f, 1.4f, 4.8f)
                .texOffs(204, 45).mirror(false).addBox(-4.4f, -0.6f, 3.6f, 8.8f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, -11, 3.6f, -0.08f, 0, 0));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(84, 0).mirror(false).addBox(-14, -2.4f, -0.3f, 14, 15, 0.6f)
                .texOffs(0, 54).mirror(false).addBox(-14, -3.2f, -0.7f, 14, 1.6f, 1.4f), PartPose.offsetAndRotation(-2.6f, -11.4f, 3.6f, 0.1f, 0.55f, 0.42f));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(84, 0).mirror(true).addBox(0, -2.4f, -0.3f, 14, 15, 0.6f)
                .texOffs(0, 54).mirror(true).addBox(0, -3.2f, -0.7f, 14, 1.6f, 1.4f), PartPose.offsetAndRotation(2.6f, -11.4f, 3.6f, 0.1f, -0.55f, -0.42f));
        PartDefinition rightWingTip = rightWing.addOrReplaceChild("right_wing_tip", CubeListBuilder.create()
                .texOffs(115, 0).mirror(false).addBox(-11, -0.8f, -0.3f, 11, 15, 0.6f)
                .texOffs(33, 54).mirror(false).addBox(-11, -0.8f, -0.6f, 11, 1.2f, 1.2f), PartPose.offsetAndRotation(-13.6f, -2.4f, 0, 0, 0.45f, -0.2f));
        PartDefinition leftWingTip = leftWing.addOrReplaceChild("left_wing_tip", CubeListBuilder.create()
                .texOffs(115, 0).mirror(true).addBox(0, -0.8f, -0.3f, 11, 15, 0.6f)
                .texOffs(33, 54).mirror(true).addBox(0, -0.8f, -0.6f, 11, 1.2f, 1.2f), PartPose.offsetAndRotation(13.6f, -2.4f, 0, 0, -0.45f, 0.2f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(13, 19).mirror(false).addBox(-2.4f, -1.8f, -2.4f, 4.8f, 8, 4.8f)
                .texOffs(227, 34).mirror(false).addBox(-2.7f, 3.4f, -2.7f, 5.4f, 1.4f, 5.4f), PartPose.offsetAndRotation(-7.6f, -11.4f, 0, -0.85f, 0, 0.32f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(13, 19).mirror(true).addBox(-2.4f, -1.8f, -2.4f, 4.8f, 8, 4.8f)
                .texOffs(227, 34).mirror(true).addBox(-2.7f, 3.4f, -2.7f, 5.4f, 1.4f, 5.4f), PartPose.offsetAndRotation(7.6f, -11.4f, 0, -0.55f, 0, -0.18f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(120, 19).mirror(false).addBox(-2.1f, 0, -2.1f, 4.2f, 7, 4.2f)
                .texOffs(0, 45).mirror(false).addBox(-2.5f, 4.2f, -2.5f, 5, 2.4f, 5), PartPose.offsetAndRotation(0, 6, 0, -1.25f, 0.2f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(120, 19).mirror(true).addBox(-2.1f, 0, -2.1f, 4.2f, 7, 4.2f)
                .texOffs(0, 45).mirror(true).addBox(-2.5f, 4.2f, -2.5f, 5, 2.4f, 5), PartPose.offsetAndRotation(0, 6, 0, -0.85f, -0.2f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(21, 45).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 3.6f, 3.6f), PartPose.offsetAndRotation(0, 6.8f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(21, 45).mirror(true).addBox(-1.8f, 0, -1.8f, 3.6f, 3.6f, 3.6f), PartPose.offsetAndRotation(0, 6.8f, 0, 0, 0, 0));
        PartDefinition mallet = rightHand.addOrReplaceChild("mallet", CubeListBuilder.create()
                .texOffs(173, 0).mirror(false).addBox(-0.6f, -10, -0.6f, 1.2f, 13, 1.2f)
                .texOffs(54, 34).mirror(false).addBox(-3.6f, -13.8f, -2.1f, 7.2f, 4.2f, 4.2f)
                .texOffs(81, 34).mirror(false).addBox(-4, -14.2f, -2.5f, 0.8f, 5, 5)
                .texOffs(81, 34).mirror(false).addBox(3.2f, -14.2f, -2.5f, 0.8f, 5, 5), PartPose.offsetAndRotation(0, 1.8f, 0, 2.241f, 0.3394f, 0.0574f));
        PartDefinition chisel = leftHand.addOrReplaceChild("chisel", CubeListBuilder.create()
                .texOffs(197, 34).mirror(false).addBox(-0.7f, -4, -0.7f, 1.4f, 6.5f, 1.4f)
                .texOffs(60, 54).mirror(false).addBox(-1, -5, -1, 2, 1.2f, 2)
                .texOffs(38, 45).mirror(false).addBox(-1.3f, -12, -0.5f, 2.6f, 7, 1), PartPose.offsetAndRotation(0, 1.8f, 0, 2.3112f, -0.0462f, 0.1798f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(140, 0).mirror(false).addBox(-4, -8.4f, -3.8f, 8, 8, 7.6f)
                .texOffs(196, 19).mirror(false).addBox(-4.3f, -8.9f, -4, 8.6f, 1.4f, 8.2f)
                .texOffs(69, 54).mirror(false).addBox(-1.3f, -9.6f, -4.6f, 2.6f, 2.4f, 0.8f), PartPose.offsetAndRotation(0, -15.2f, -0.8f, 0, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(78, 54).mirror(false).addBox(-1.4f, -1.4f, -0.6f, 2.8f, 2.8f, 0.8f), PartPose.offsetAndRotation(-2.1f, -5.7f, -3.9f, 0, 0.2f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(78, 54).mirror(true).addBox(-1.4f, -1.4f, -0.6f, 2.8f, 2.8f, 0.8f), PartPose.offsetAndRotation(2.1f, -5.7f, -3.9f, 0, -0.2f, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(87, 54).mirror(false).addBox(-3.8f, -0.8f, -0.6f, 3.8f, 1.4f, 1.2f), PartPose.offsetAndRotation(-0.3f, -7.3f, -4.1f, 0, 0.15f, 0.5f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(87, 54).mirror(true).addBox(0, -0.8f, -0.6f, 3.8f, 1.4f, 1.2f), PartPose.offsetAndRotation(0.3f, -7.3f, -4.1f, 0, -0.15f, -0.5f));
        PartDefinition beak = head.addOrReplaceChild("beak", CubeListBuilder.create()
                .texOffs(94, 34).mirror(false).addBox(-1.9f, -1.6f, -5.6f, 3.8f, 3.2f, 5.8f)
                .texOffs(129, 45).mirror(false).addBox(-1.4f, 1, -6.6f, 2.8f, 3.4f, 2), PartPose.offsetAndRotation(0, -2.5f, -3.8f, 0.08f, 0, 0));
        PartDefinition lowerBeak = head.addOrReplaceChild("lower_beak", CubeListBuilder.create()
                .texOffs(77, 45).mirror(false).addBox(-1.4f, 0, -4.4f, 2.8f, 1.5f, 4.4f), PartPose.offsetAndRotation(0, -0.8f, -3.6f, 0.1f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(164, 45).mirror(false).addBox(-2.6f, -1.8f, -0.4f, 2.6f, 3.6f, 0.8f), PartPose.offsetAndRotation(-4, -5.4f, 0.6f, 0, -0.4f, -0.3f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(164, 45).mirror(true).addBox(0, -1.8f, -0.4f, 2.6f, 3.6f, 0.8f), PartPose.offsetAndRotation(4, -5.4f, 0.6f, 0, 0.4f, 0.3f));
        PartDefinition mane0 = head.addOrReplaceChild("mane_0", CubeListBuilder.create()
                .texOffs(233, 19).mirror(false).addBox(-1.4f, -7.5f, -1.2f, 2.8f, 7.5f, 2.4f), PartPose.offsetAndRotation(-2.8f, -7.4f, -1.6f, -0.3f, 0, -0.6f));
        PartDefinition mane1 = head.addOrReplaceChild("mane_1", CubeListBuilder.create()
                .texOffs(141, 19).mirror(false).addBox(-1.4f, -9, -1.2f, 2.8f, 9, 2.4f), PartPose.offsetAndRotation(-0.9f, -7.4f, -2.2f, -0.25f, 0, -0.2f));
        PartDefinition mane2 = head.addOrReplaceChild("mane_2", CubeListBuilder.create()
                .texOffs(141, 19).mirror(false).addBox(-1.4f, -9, -1.2f, 2.8f, 9, 2.4f), PartPose.offsetAndRotation(1, -7.4f, -2.2f, -0.25f, 0, 0.2f));
        PartDefinition mane3 = head.addOrReplaceChild("mane_3", CubeListBuilder.create()
                .texOffs(233, 19).mirror(false).addBox(-1.4f, -7.5f, -1.2f, 2.8f, 7.5f, 2.4f), PartPose.offsetAndRotation(2.8f, -7.4f, -1.6f, -0.3f, 0, 0.6f));
        PartDefinition mane4 = head.addOrReplaceChild("mane_4", CubeListBuilder.create()
                .texOffs(34, 19).mirror(false).addBox(-1.4f, -9.5f, -1.2f, 2.8f, 9.5f, 2.4f), PartPose.offsetAndRotation(-2.4f, -7.4f, 1, -0.75f, 0, -0.4f));
        PartDefinition mane5 = head.addOrReplaceChild("mane_5", CubeListBuilder.create()
                .texOffs(0, 19).mirror(false).addBox(-1.4f, -11, -1.2f, 2.8f, 11, 2.4f), PartPose.offsetAndRotation(0, -7.4f, 1.4f, -0.85f, 0, 0));
        PartDefinition mane6 = head.addOrReplaceChild("mane_6", CubeListBuilder.create()
                .texOffs(34, 19).mirror(false).addBox(-1.4f, -9.5f, -1.2f, 2.8f, 9.5f, 2.4f), PartPose.offsetAndRotation(2.4f, -7.4f, 1, -0.75f, 0, 0.4f));
        PartDefinition mane7 = head.addOrReplaceChild("mane_7", CubeListBuilder.create()
                .texOffs(141, 19).mirror(false).addBox(-1.4f, -9, -1.2f, 2.8f, 9, 2.4f), PartPose.offsetAndRotation(-1.4f, -7.4f, 3, -1.2f, 0, -0.25f));
        PartDefinition mane8 = head.addOrReplaceChild("mane_8", CubeListBuilder.create()
                .texOffs(141, 19).mirror(false).addBox(-1.4f, -9, -1.2f, 2.8f, 9, 2.4f), PartPose.offsetAndRotation(1.4f, -7.4f, 3, -1.2f, 0, 0.25f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED thunder: end
    }

    /**
     * The Thunder Lord's pose: hovering, his wings stirring, the mallet raised and the chisel held out; a blow,
     * the mallet brought down on the chisel; winding up his thunderbolt, his wings spread wide, mallet and chisel
     * lifted high, his beak open.
     */
    public static final class ThunderModel extends GodModel<ThunderLordEntity> {
        public static final float SIZE = 0.72f;
        private final ModelPart body, head, lowerBeak, rightArm, leftArm, rightForearm, leftForearm, rightWing, leftWing,
                rightWingTip, leftWingTip, drums, kiltFlap, rightThigh, leftThigh;
        private final ModelPart[] mane = new ModelPart[9];

        public ThunderModel(ModelPart root) {
            super(root);
            ModelPart waist = root.getChild("waist");
            body = waist.getChild("body");
            head = body.getChild("head");
            lowerBeak = head.getChild("lower_beak");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightWing = body.getChild("right_wing");
            leftWing = body.getChild("left_wing");
            rightWingTip = rightWing.getChild("right_wing_tip");
            leftWingTip = leftWing.getChild("left_wing_tip");
            drums = body.getChild("drums");
            kiltFlap = waist.getChild("kilt_flap");
            rightThigh = waist.getChild("right_thigh");
            leftThigh = waist.getChild("left_thigh");
            for (int i = 0; i < mane.length; i++) mane[i] = head.getChild("mane_" + i);
        }

        @Override
        public void setupAnim(ThunderLordEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float drift = Mth.sin(age * 0.08f);
            root.y = drift * 0.7f;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD - wind * 0.35f;
            lowerBeak.xRot = lowerBeak.getInitialPose().xRot + wind * 0.55f + strike * 0.4f + Math.max(0, Mth.sin(age * 0.11f)) * 0.08f;
            turn(body, -wind * 0.1f + strike * 0.15f, 0);
            // the mallet raised and the chisel held out; a blow, the mallet brought down on the chisel;
            // winding up, both lifted high over his head
            turn(rightArm, Mth.cos(age * 0.07f) * 0.04f - wind * 0.45f + strike * 1.3f, wind * 1.0f);
            turn(rightForearm, wind * 0.2f - strike * 0.3f, 0, -wind * 0.4f);
            turn(leftArm, -Mth.cos(age * 0.07f) * 0.03f - wind * 0.7f - strike * 0.5f, -wind * 1.1f);
            turn(leftForearm, 0, 0, wind * 0.3f);
            // the wings: stirring at rest, beating as he flies, spread wide as he winds up
            float beat = Mth.sin(age * 0.12f) * 0.06f + Mth.sin(age * 0.5f) * 0.12f * amount;
            turn(rightWing, 0, -wind * 0.5f + beat, -wind * 0.1f + beat * 0.5f);
            turn(leftWing, 0, wind * 0.5f - beat, wind * 0.1f - beat * 0.5f);
            turn(rightWingTip, 0, -wind * 0.4f + beat, wind * 0.3f);
            turn(leftWingTip, 0, wind * 0.4f - beat, -wind * 0.3f);
            turn(drums, Mth.sin(age * 0.06f) * 0.02f, 0);
            for (int i = 0; i < mane.length; i++) turn(mane[i], Mth.sin(age * 0.15f + i) * 0.05f - amount * 0.2f, Mth.cos(age * 0.13f + i) * 0.04f);
            turn(kiltFlap, -amount * 0.3f + Mth.sin(age * 0.07f) * 0.03f, 0);
            // his talons drawn up a little as he flies
            turn(rightThigh, -amount * 0.25f + drift * 0.03f, 0);
            turn(leftThigh, -amount * 0.25f - drift * 0.03f, 0);
        }
    }

    /**
     * The Lightning Mother (256x256): a lady in flowing robes of rose and white, great sleeves lined in blue, a
     * blue sash and long ribbons, one arching behind her shoulders and more streaming at her sides; her hair high
     * in two upright loops, a gold crown with a lightning-jewel, pins hung with pearls; a round bronze mirror in
     * each hand, its face shining. Model and skin: {@code assets_source/heaven_gods_thunder.py}.
     */
    public static LayerDefinition lightningLayer() {
        // GENERATED lightning: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(7, 0).mirror(false).addBox(-5, -14, -3.8f, 10, 14, 7.6f)
                .texOffs(0, 53).mirror(false).addBox(-3.2f, -15, -2.8f, 6.4f, 1.4f, 5.6f)
                .texOffs(27, 53).mirror(false).addBox(-1.8f, -16.8f, -1.8f, 3.6f, 3, 3.6f)
                .texOffs(0, 41).mirror(false).addBox(-6.4f, -14.6f, -4.6f, 12.8f, 0.8f, 9.2f)
                .texOffs(87, 53).mirror(false).addBox(-6, -14.2f, -4.7f, 12, 5, 0.5f)
                .texOffs(87, 53).mirror(false).addBox(-6, -14.2f, 4.2f, 12, 5, 0.5f)
                .texOffs(178, 24).mirror(false).addBox(-5.6f, -4.6f, -4.3f, 11.2f, 2.4f, 8.6f)
                .texOffs(142, 53).mirror(false).addBox(-1.6f, -5.4f, -4.9f, 3.2f, 3.2f, 0.8f), PartPose.offsetAndRotation(0, 5, 0, 0, 0, 0));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(0, 24).mirror(false).addBox(-5.4f, -0.4f, -4.2f, 10.8f, 7, 8.4f)
                .texOffs(187, 0).mirror(false).addBox(-6.6f, 6, -5, 13.2f, 7, 10)
                .texOffs(126, 0).mirror(false).addBox(-8.2f, 12.4f, -6.2f, 16.4f, 6.8f, 12.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition underskirt = skirt.addOrReplaceChild("underskirt", CubeListBuilder.create()
                .texOffs(44, 0).mirror(false).addBox(-3.6f, 0, -0.8f, 7.2f, 20.5f, 0.8f), PartPose.offsetAndRotation(0, -1, -4.6f, -0.14f, 0, 0));
        PartDefinition rightSashEnd = body.addOrReplaceChild("right_sash_end", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.8f, 0, -0.3f, 1.6f, 22, 0.6f), PartPose.offsetAndRotation(-1.2f, -3.4f, -4.8f, -0.12f, 0.1f, 0.1f));
        PartDefinition leftSashEnd = body.addOrReplaceChild("left_sash_end", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(-0.8f, 0, -0.3f, 1.6f, 22, 0.6f), PartPose.offsetAndRotation(1.2f, -3.4f, -4.8f, -0.12f, -0.1f, -0.1f));
        PartDefinition rightPanel = skirt.addOrReplaceChild("right_panel", CubeListBuilder.create()
                .texOffs(96, 0).mirror(false).addBox(-2, 0, -0.4f, 4, 19.5f, 0.4f), PartPose.offsetAndRotation(-5, -1, -4.8f, -0.1f, 0.4f, 0.05f));
        PartDefinition leftPanel = skirt.addOrReplaceChild("left_panel", CubeListBuilder.create()
                .texOffs(96, 0).mirror(true).addBox(-2, 0, -0.4f, 4, 19.5f, 0.4f), PartPose.offsetAndRotation(5, -1, -4.8f, -0.1f, -0.4f, -0.05f));
        PartDefinition backPanel = skirt.addOrReplaceChild("back_panel", CubeListBuilder.create()
                .texOffs(107, 0).mirror(false).addBox(-4, 0, 0, 8, 20, 0.5f), PartPose.offsetAndRotation(0, -1.5f, 4.5f, 0.14f, 0, 0));
        PartDefinition rightShoe = root.addOrReplaceChild("right_shoe", CubeListBuilder.create()
                .texOffs(114, 53).mirror(false).addBox(-1.3f, -0.6f, -2.2f, 2.6f, 1.6f, 3.4f), PartPose.offsetAndRotation(-2.6f, 22.6f, -6.6f, 0, 0.1f, 0));
        PartDefinition leftShoe = root.addOrReplaceChild("left_shoe", CubeListBuilder.create()
                .texOffs(114, 53).mirror(true).addBox(-1.3f, -0.6f, -2.2f, 2.6f, 1.6f, 3.4f), PartPose.offsetAndRotation(2.6f, 22.6f, -6.6f, 0, -0.1f, 0));
        PartDefinition arch = body.addOrReplaceChild("arch", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -13, 5.4f, 0.15f, 0, 0));
        PartDefinition archSeg0 = arch.addOrReplaceChild("arch_seg_0", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(-13.2636f, -1.907f, 0, 0, 0, 4.8552f));
        PartDefinition archSeg1 = arch.addOrReplaceChild("arch_seg_1", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(-12.1891f, -5.5666f, 0, 0, 0, 5.1408f));
        PartDefinition archSeg2 = arch.addOrReplaceChild("arch_seg_2", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(-10.127f, -8.7751f, 0, 0, 0, 5.4264f));
        PartDefinition archSeg3 = arch.addOrReplaceChild("arch_seg_3", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(-7.2446f, -11.2728f, 0, 0, 0, 5.712f));
        PartDefinition archSeg4 = arch.addOrReplaceChild("arch_seg_4", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(-3.7752f, -12.8572f, 0, 0, 0, 5.9976f));
        PartDefinition archSeg5 = arch.addOrReplaceChild("arch_seg_5", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(-0, -13.4f, 0, 0, 0, 6.2832f));
        PartDefinition archSeg6 = arch.addOrReplaceChild("arch_seg_6", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(3.7752f, -12.8572f, 0, 0, 0, 6.5688f));
        PartDefinition archSeg7 = arch.addOrReplaceChild("arch_seg_7", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(7.2446f, -11.2728f, 0, 0, 0, 6.8544f));
        PartDefinition archSeg8 = arch.addOrReplaceChild("arch_seg_8", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(10.127f, -8.7751f, 0, 0, 0, 7.14f));
        PartDefinition archSeg9 = arch.addOrReplaceChild("arch_seg_9", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(12.1891f, -5.5666f, 0, 0, 0, 7.4256f));
        PartDefinition archSeg10 = arch.addOrReplaceChild("arch_seg_10", CubeListBuilder.create()
                .texOffs(176, 53).mirror(false).addBox(-2.207f, -1.1f, -0.2f, 4.414f, 2.2f, 0.4f), PartPose.offsetAndRotation(13.2636f, -1.907f, 0, 0, 0, 7.7112f));
        PartDefinition rightRibbon = body.addOrReplaceChild("right_ribbon", CubeListBuilder.create()
                .texOffs(74, 24).mirror(false).addBox(-1.2f, 0, -0.2f, 2.4f, 13, 0.4f), PartPose.offsetAndRotation(-13.2f, -13.4f, 5.2f, 0.1f, 0.4f, 0.12f));
        PartDefinition leftRibbon = body.addOrReplaceChild("left_ribbon", CubeListBuilder.create()
                .texOffs(74, 24).mirror(true).addBox(-1.2f, 0, -0.2f, 2.4f, 13, 0.4f), PartPose.offsetAndRotation(13.2f, -13.4f, 5.2f, 0.1f, -0.4f, -0.12f));
        PartDefinition rightRibbon2 = rightRibbon.addOrReplaceChild("right_ribbon_2", CubeListBuilder.create()
                .texOffs(92, 24).mirror(false).addBox(-1.3f, 0, -0.2f, 2.6f, 12, 0.4f), PartPose.offsetAndRotation(0, 12.6f, 0, 0.15f, 0, 0.22f));
        PartDefinition leftRibbon2 = leftRibbon.addOrReplaceChild("left_ribbon_2", CubeListBuilder.create()
                .texOffs(92, 24).mirror(true).addBox(-1.3f, 0, -0.2f, 2.6f, 12, 0.4f), PartPose.offsetAndRotation(0, 12.6f, 0, 0.15f, 0, -0.22f));
        PartDefinition rightRibbon3 = rightRibbon2.addOrReplaceChild("right_ribbon_3", CubeListBuilder.create()
                .texOffs(47, 41).mirror(false).addBox(-1.4f, 0, -0.2f, 2.8f, 10, 0.4f), PartPose.offsetAndRotation(0, 11.6f, 0, 0.15f, 0, 0.25f));
        PartDefinition leftRibbon3 = leftRibbon2.addOrReplaceChild("left_ribbon_3", CubeListBuilder.create()
                .texOffs(47, 41).mirror(true).addBox(-1.4f, 0, -0.2f, 2.8f, 10, 0.4f), PartPose.offsetAndRotation(0, 11.6f, 0, 0.15f, 0, -0.25f));
        PartDefinition rightStreamer = body.addOrReplaceChild("right_streamer", CubeListBuilder.create()
                .texOffs(83, 24).mirror(false).addBox(-1.1f, 0, -0.2f, 2.2f, 13, 0.4f), PartPose.offsetAndRotation(-4.6f, -3.6f, 4, 0.35f, 0.2f, 0.25f));
        PartDefinition leftStreamer = body.addOrReplaceChild("left_streamer", CubeListBuilder.create()
                .texOffs(83, 24).mirror(true).addBox(-1.1f, 0, -0.2f, 2.2f, 13, 0.4f), PartPose.offsetAndRotation(4.6f, -3.6f, 4, 0.35f, -0.2f, -0.25f));
        PartDefinition rightStreamer2 = rightStreamer.addOrReplaceChild("right_streamer_2", CubeListBuilder.create()
                .texOffs(221, 24).mirror(false).addBox(-1.2f, 0, -0.2f, 2.4f, 11, 0.4f), PartPose.offsetAndRotation(0, 12.6f, 0, 0.25f, 0, 0.2f));
        PartDefinition leftStreamer2 = leftStreamer.addOrReplaceChild("left_streamer_2", CubeListBuilder.create()
                .texOffs(221, 24).mirror(true).addBox(-1.2f, 0, -0.2f, 2.4f, 11, 0.4f), PartPose.offsetAndRotation(0, 12.6f, 0, 0.25f, 0, -0.2f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(101, 24).mirror(false).addBox(-2.6f, -1.6f, -2.4f, 4.2f, 8, 4.8f), PartPose.offsetAndRotation(-6.4f, -12.6f, 0, -0.2f, 0, 0.42f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(101, 24).mirror(true).addBox(-1.6f, -1.6f, -2.4f, 4.2f, 8, 4.8f), PartPose.offsetAndRotation(6.4f, -12.6f, 0, -0.2f, 0, -0.42f));
        PartDefinition rightShoulder = rightArm.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(158, 41).mirror(false).addBox(-3.6f, -1, -3.3f, 4.8f, 1.8f, 6.6f), PartPose.offsetAndRotation(-1, -1.8f, 0, 0, 0, 0.25f));
        PartDefinition leftShoulder = leftArm.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(158, 41).mirror(true).addBox(-1.2f, -1, -3.3f, 4.8f, 1.8f, 6.6f), PartPose.offsetAndRotation(1, -1.8f, 0, 0, 0, -0.25f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(56, 41).mirror(false).addBox(-1.9f, 0, -1.9f, 3.8f, 6.4f, 3.8f)
                .texOffs(44, 53).mirror(false).addBox(-2.2f, 4.4f, -2.2f, 4.4f, 1.8f, 4.4f), PartPose.offsetAndRotation(-0.6f, 5.8f, 0, -1.85f, 0.3f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(56, 41).mirror(true).addBox(-1.9f, 0, -1.9f, 3.8f, 6.4f, 3.8f)
                .texOffs(44, 53).mirror(true).addBox(-2.2f, 4.4f, -2.2f, 4.4f, 1.8f, 4.4f), PartPose.offsetAndRotation(0.6f, 5.8f, 0, -1.85f, -0.3f, 0));
        PartDefinition rightSleeve = rightForearm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(63, 0).mirror(false).addBox(-3.2f, -1, -8.4f, 6.4f, 13, 8.4f), PartPose.offsetAndRotation(0, 0.6f, 1.8f, 1.985f, 0.4411f, -0.5183f));
        PartDefinition leftSleeve = leftForearm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(63, 0).mirror(true).addBox(-3.2f, -1, -8.4f, 6.4f, 13, 8.4f), PartPose.offsetAndRotation(0, 0.6f, 1.8f, 1.985f, -0.4411f, 0.5183f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(129, 53).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(0, 6.6f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(129, 53).mirror(true).addBox(-1.3f, 0, -1.3f, 2.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(0, 6.6f, 0, 0, 0, 0));
        PartDefinition rightMirror = rightHand.addOrReplaceChild("right_mirror", CubeListBuilder.create()
                .texOffs(65, 53).mirror(false).addBox(-0.6f, -3.6f, -0.6f, 1.2f, 4.6f, 1.2f)
                .texOffs(189, 53).mirror(false).addBox(-1.8f, -4.6f, -0.9f, 3.6f, 1.2f, 1.8f)
                .texOffs(122, 24).mirror(false).addBox(-5.4f, -15.4f, -0.6f, 10.8f, 10.8f, 1.2f)
                .texOffs(207, 53).mirror(false).addBox(-1.2f, -17, -0.5f, 2.4f, 1.8f, 1), PartPose.offsetAndRotation(0, 1.4f, -0.2f, 2.2638f, 0.2298f, 0.2705f));
        PartDefinition leftMirror = leftHand.addOrReplaceChild("left_mirror", CubeListBuilder.create()
                .texOffs(65, 53).mirror(true).addBox(-0.6f, -3.6f, -0.6f, 1.2f, 4.6f, 1.2f)
                .texOffs(189, 53).mirror(true).addBox(-1.8f, -4.6f, -0.9f, 3.6f, 1.2f, 1.8f)
                .texOffs(122, 24).mirror(true).addBox(-5.4f, -15.4f, -0.6f, 10.8f, 10.8f, 1.2f)
                .texOffs(207, 53).mirror(true).addBox(-1.2f, -17, -0.5f, 2.4f, 1.8f, 1), PartPose.offsetAndRotation(0, 1.4f, -0.2f, 2.2638f, -0.2298f, -0.2705f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(41, 24).mirror(false).addBox(-4, -8, -3.6f, 8, 7.2f, 7.2f)
                .texOffs(216, 53).mirror(false).addBox(-0.8f, -5, -4.3f, 1.6f, 2, 0.8f)
                .texOffs(149, 24).mirror(false).addBox(-4.2f, -8.3f, -0.6f, 8.4f, 7.6f, 4.6f)
                .texOffs(110, 41).mirror(false).addBox(-4.3f, -9.2f, -3.3f, 8.6f, 1.8f, 7.4f)
                .texOffs(183, 41).mirror(false).addBox(-3.6f, -0.9f, -3, 7.2f, 1.6f, 6.2f), PartPose.offsetAndRotation(0, -16.8f, -0.6f, 0, 0, 0));
        PartDefinition rightLock = head.addOrReplaceChild("right_lock", CubeListBuilder.create()
                .texOffs(230, 24).mirror(false).addBox(-0.7f, 0, -0.9f, 1.4f, 10, 1.8f), PartPose.offsetAndRotation(-3.9f, -7, -2, -0.04f, 0, 0.05f));
        PartDefinition leftLock = head.addOrReplaceChild("left_lock", CubeListBuilder.create()
                .texOffs(230, 24).mirror(true).addBox(-0.7f, 0, -0.9f, 1.4f, 10, 1.8f), PartPose.offsetAndRotation(3.9f, -7, -2, -0.04f, 0, -0.05f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(153, 53).mirror(false).addBox(-0.6f, -1.2f, -0.8f, 0.7f, 2.8f, 1.6f), PartPose.offsetAndRotation(-4, -4.6f, -0.6f, 0, -0.2f, 0));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(153, 53).mirror(true).addBox(-0.1f, -1.2f, -0.8f, 0.7f, 2.8f, 1.6f), PartPose.offsetAndRotation(4, -4.6f, -0.6f, 0, 0.2f, 0));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(214, 41).mirror(false).addBox(-2.6f, -3.2f, -1.8f, 5.2f, 3.6f, 4.4f)
                .texOffs(73, 41).mirror(false).addBox(-4.4f, -0.6f, -4.1f, 8.8f, 1.6f, 8.2f)
                .texOffs(160, 53).mirror(false).addBox(-2, -4.2f, -4.4f, 4, 4, 0.6f), PartPose.offsetAndRotation(0, -8.2f, 0, 0, 0, 0));
        PartDefinition rightLoop = crown.addOrReplaceChild("right_loop", CubeListBuilder.create()
                .texOffs(145, 41).mirror(false).addBox(-1.3f, -6.4f, -1.3f, 2.6f, 6.4f, 2.6f), PartPose.offsetAndRotation(-1.6f, -2.8f, 0.2f, -0.1f, 0, -0.32f));
        PartDefinition leftLoop = crown.addOrReplaceChild("left_loop", CubeListBuilder.create()
                .texOffs(145, 41).mirror(true).addBox(-1.3f, -6.4f, -1.3f, 2.6f, 6.4f, 2.6f), PartPose.offsetAndRotation(1.6f, -2.8f, 0.2f, -0.1f, 0, 0.32f));
        PartDefinition rightLoopTop = rightLoop.addOrReplaceChild("right_loop_top", CubeListBuilder.create()
                .texOffs(74, 53).mirror(false).addBox(-1.2f, -3.4f, -1.2f, 2.4f, 3.4f, 2.4f), PartPose.offsetAndRotation(0, -6.2f, 0, 0, 0, 0.9f));
        PartDefinition leftLoopTop = leftLoop.addOrReplaceChild("left_loop_top", CubeListBuilder.create()
                .texOffs(74, 53).mirror(true).addBox(-1.2f, -3.4f, -1.2f, 2.4f, 3.4f, 2.4f), PartPose.offsetAndRotation(0, -6.2f, 0, 0, 0, -0.9f));
        PartDefinition rightPin = crown.addOrReplaceChild("right_pin", CubeListBuilder.create()
                .texOffs(223, 53).mirror(false).addBox(-4.4f, -0.3f, -0.3f, 4.4f, 0.6f, 0.6f)
                .texOffs(171, 53).mirror(false).addBox(-4.2f, 0.3f, -0.3f, 0.6f, 3.2f, 0.6f)
                .texOffs(202, 53).mirror(false).addBox(-2.8f, 0.3f, -0.3f, 0.6f, 2.2f, 0.6f), PartPose.offsetAndRotation(-2.6f, -1.6f, -0.4f, 0, 0.2f, 0.3f));
        PartDefinition leftPin = crown.addOrReplaceChild("left_pin", CubeListBuilder.create()
                .texOffs(223, 53).mirror(true).addBox(0, -0.3f, -0.3f, 4.4f, 0.6f, 0.6f)
                .texOffs(171, 53).mirror(true).addBox(3.6f, 0.3f, -0.3f, 0.6f, 3.2f, 0.6f)
                .texOffs(202, 53).mirror(true).addBox(2.2f, 0.3f, -0.3f, 0.6f, 2.2f, 0.6f), PartPose.offsetAndRotation(2.6f, -1.6f, -0.4f, 0, -0.2f, -0.3f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED lightning: end
    }

    /**
     * The Lightning Mother's pose: floating, her ribbons streaming, a mirror held up in each hand; a blow, both
     * mirrors thrust at the foe; winding up her flash, both mirrors lifted high, blazing.
     */
    public static final class LightningModel extends GodModel<LightningMotherEntity> {
        public static final float SIZE = 0.74f;
        private final ModelPart head, rightArm, leftArm, rightForearm, leftForearm, rightSleeve, leftSleeve, arch, underskirt;
        private final ModelPart[] ribbons = new ModelPart[10];

        public LightningModel(ModelPart root) {
            super(root);
            ModelPart body = root.getChild("body");
            head = body.getChild("head");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightSleeve = rightForearm.getChild("right_sleeve");
            leftSleeve = leftForearm.getChild("left_sleeve");
            arch = body.getChild("arch");
            underskirt = body.getChild("skirt").getChild("underskirt");
            int n = 0;
            for (String side : new String[]{"right", "left"}) {
                ModelPart r = body.getChild(side + "_ribbon"), r2 = r.getChild(side + "_ribbon_2"), r3 = r2.getChild(side + "_ribbon_3");
                ModelPart s = body.getChild(side + "_streamer"), s2 = s.getChild(side + "_streamer_2");
                ribbons[n++] = r;
                ribbons[n++] = r2;
                ribbons[n++] = r3;
                ribbons[n++] = s;
                ribbons[n++] = s2;
            }
        }

        @Override
        public void setupAnim(LightningMotherEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float drift = Mth.sin(age * 0.07f);
            root.y = drift * 0.6f;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD - wind * 0.2f;
            head.zRot = Mth.sin(age * 0.05f) * 0.03f;
            // the mirrors held up; a blow, thrust at the foe; winding up, lifted high over her head
            float sway = Mth.cos(age * 0.06f) * 0.04f;
            float ax = sway - wind * 1.15f - strike * 0.7f, fx = wind * 0.75f + strike * 0.9f;
            turn(rightArm, ax, -wind * 0.15f);
            turn(leftArm, ax - sway * 2, wind * 0.15f);
            turn(rightForearm, fx, 0);
            turn(leftForearm, fx, 0);
            // her great sleeves hang under their weight
            turn(rightSleeve, -(ax + fx) * 0.8f + drift * 0.04f + amount * 0.2f, 0);
            turn(leftSleeve, -(ax - sway * 2 + fx) * 0.8f - drift * 0.04f + amount * 0.2f, 0);
            turn(underskirt, -amount * 0.25f + Mth.sin(age * 0.06f) * 0.03f, 0);
            // the ribbons streaming, rippling down their length
            turn(arch, Mth.sin(age * 0.08f) * 0.05f + amount * 0.3f, 0);
            for (int i = 0; i < ribbons.length; i++) {
                float w = Mth.sin(age * 0.1f - (i % 5) * 0.8f + (i < 5 ? 0 : 1.7f));
                turn(ribbons[i], 0.1f * w + amount * (i % 5 == 0 || i % 5 == 3 ? 0.5f : 0.25f), (i < 5 ? -1 : 1) * 0.07f * w);
            }
        }
    }

    // ── Thousand-Mile Eye and Wind-Following Ear ────────────────────────

    /**
     * Thousand-Mile Eye (256x256): gilt lamellar laced in teal over a teal robe, a mirror over his belly, shoulder
     * guards bitten by lions, a ribbon arching behind him; a jade-green face, two great bulging eyes burning gold,
     * two short horns, wild dark hair; his right hand held flat over his brow, shading his eyes; a halberd in his
     * left. Model and skin: {@code assets_source/heaven_gods_sentinels.py}.
     */
    public static LayerDefinition eyeLayer() {
        // GENERATED eye: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(225, 46).mirror(false).addBox(-4.4f, -2, -3, 8.8f, 4, 6)
                .texOffs(0, 59).mirror(false).addBox(-5, -3.4f, -3.5f, 10, 2.8f, 7)
                .texOffs(104, 70).mirror(false).addBox(-2.2f, -4.4f, -4.3f, 4.4f, 4.4f, 1), PartPose.offsetAndRotation(0, 9, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(83, 0).mirror(false).addBox(-2.1f, 0, -0.4f, 4.2f, 13.4f, 0.8f), PartPose.offsetAndRotation(0, 0.6f, -3.7f, -0.08f, 0, 0));
        PartDefinition robeBack = waist.addOrReplaceChild("robe_back", CubeListBuilder.create()
                .texOffs(96, 0).mirror(false).addBox(-4.6f, 0, -0.4f, 9.2f, 13.6f, 0.8f), PartPose.offsetAndRotation(0, 0, 3.1f, 0.14f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(174, 59).mirror(false).addBox(-2.6f, 0, -0.5f, 5.2f, 7.6f, 1), PartPose.offsetAndRotation(-2.7f, -0.8f, -3.3f, -0.14f, 0.18f, 0.1f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(174, 59).mirror(true).addBox(-2.6f, 0, -0.5f, 5.2f, 7.6f, 1), PartPose.offsetAndRotation(2.7f, -0.8f, -3.3f, -0.14f, -0.18f, -0.1f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(156, 0).mirror(false).addBox(-1, 0, -2.8f, 1, 7.2f, 5.6f), PartPose.offsetAndRotation(-4.8f, -0.8f, 0, 0, 0, 0.22f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(156, 0).mirror(true).addBox(0, 0, -2.8f, 1, 7.2f, 5.6f), PartPose.offsetAndRotation(4.8f, -0.8f, 0, 0, 0, -0.22f));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(61, 46).mirror(false).addBox(-0.8f, 0, -0.3f, 1.6f, 10, 0.6f), PartPose.offsetAndRotation(-4.2f, -2.4f, -2.4f, 0.04f, 0, 0.14f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(61, 46).mirror(true).addBox(-0.8f, 0, -0.3f, 1.6f, 10, 0.6f), PartPose.offsetAndRotation(4.2f, -2.4f, -2.4f, 0.04f, 0, -0.14f));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(171, 0).mirror(false).addBox(-2.1f, 0, -2.1f, 4.2f, 7.6f, 4.2f), PartPose.offsetAndRotation(-2.6f, 9, 0.3f, 0, 0, 0.03f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(171, 0).mirror(true).addBox(-2.1f, 0, -2.1f, 4.2f, 7.6f, 4.2f), PartPose.offsetAndRotation(2.6f, 9, 0.3f, 0, 0, -0.03f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(68, 46).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 7, 3.6f)
                .texOffs(189, 59).mirror(false).addBox(-2.2f, 0.4f, -2.5f, 4.4f, 5.6f, 2.6f)
                .texOffs(208, 70).mirror(false).addBox(-2, -1.4f, -2.8f, 4, 2.6f, 1)
                .texOffs(13, 70).mirror(false).addBox(-2.1f, 6.2f, -3.8f, 4.2f, 1.6f, 6)
                .texOffs(117, 70).mirror(false).addBox(-2.3f, 5.6f, -2.3f, 4.6f, 1, 4.6f), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, -0.03f));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(68, 46).mirror(true).addBox(-1.8f, 0, -1.8f, 3.6f, 7, 3.6f)
                .texOffs(189, 59).mirror(true).addBox(-2.2f, 0.4f, -2.5f, 4.4f, 5.6f, 2.6f)
                .texOffs(208, 70).mirror(true).addBox(-2, -1.4f, -2.8f, 4, 2.6f, 1)
                .texOffs(13, 70).mirror(true).addBox(-2.1f, 6.2f, -3.8f, 4.2f, 1.6f, 6)
                .texOffs(117, 70).mirror(true).addBox(-2.3f, 5.6f, -2.3f, 4.6f, 1, 4.6f), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, 0.03f));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(119, 0).mirror(false).addBox(-5.4f, -11.4f, -3.4f, 10.8f, 7.4f, 6.8f)
                .texOffs(85, 46).mirror(false).addBox(-4.6f, -4.4f, -3, 9.2f, 4.6f, 6)
                .texOffs(9, 0).mirror(false).addBox(-5.9f, -11.8f, -3.9f, 11.8f, 7.6f, 7.8f)
                .texOffs(138, 70).mirror(false).addBox(-5, -10.9f, -4.5f, 4.4f, 4.4f, 0.7f)
                .texOffs(138, 70).mirror(false).addBox(0.6f, -10.9f, -4.5f, 4.4f, 4.4f, 0.7f)
                .texOffs(201, 0).mirror(false).addBox(-4.8f, -4.6f, -3.4f, 9.6f, 4.4f, 6.8f)
                .texOffs(151, 70).mirror(false).addBox(-2.4f, -5.4f, -4.3f, 4.8f, 4.8f, 0.8f)
                .texOffs(35, 59).mirror(false).addBox(-6.2f, -12.2f, -3.6f, 12.4f, 1.2f, 7.2f)
                .texOffs(36, 70).mirror(false).addBox(-3.4f, -13.4f, -2.8f, 6.8f, 1.8f, 5.2f)
                .texOffs(74, 70).mirror(false).addBox(-1.8f, -14.4f, -1.8f, 3.6f, 2.2f, 3.6f), PartPose.offsetAndRotation(0, -2.6f, 0, 0, 0, 0));
        PartDefinition rightPauldron = body.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(78, 59).mirror(false).addBox(-4, -1.2f, -3.6f, 6.6f, 2, 7.2f)
                .texOffs(118, 46).mirror(false).addBox(-4.8f, 0.6f, -4, 7.2f, 2.2f, 8)
                .texOffs(0, 46).mirror(false).addBox(-5.6f, 2.6f, -4.4f, 7.8f, 2.2f, 8.8f)
                .texOffs(63, 70).mirror(false).addBox(-6.5f, 0.6f, -2, 1, 4, 4), PartPose.offsetAndRotation(-6.6f, -11.4f, 0, 0, 0, 0.45f));
        PartDefinition leftPauldron = body.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(78, 59).mirror(true).addBox(-2.6f, -1.2f, -3.6f, 6.6f, 2, 7.2f)
                .texOffs(118, 46).mirror(true).addBox(-2.4f, 0.6f, -4, 7.2f, 2.2f, 8)
                .texOffs(0, 46).mirror(true).addBox(-2.2f, 2.6f, -4.4f, 7.8f, 2.2f, 8.8f)
                .texOffs(63, 70).mirror(true).addBox(5.5f, 0.6f, -2, 1, 4, 4), PartPose.offsetAndRotation(6.6f, -11.4f, 0, 0, 0, -0.45f));
        PartDefinition arch = body.addOrReplaceChild("arch", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -11.6f, 4.6f, 0.15f, 0, 0));
        PartDefinition archSeg0 = arch.addOrReplaceChild("arch_seg_0", CubeListBuilder.create()
                .texOffs(9, 80).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(-11.2268f, -1.9796f, 0, 0, 0, 4.8869f));
        PartDefinition archSeg1 = arch.addOrReplaceChild("arch_seg_1", CubeListBuilder.create()
                .texOffs(9, 80).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(-9.8727f, -5.7f, 0, 0, 0, 5.236f));
        PartDefinition archSeg2 = arch.addOrReplaceChild("arch_seg_2", CubeListBuilder.create()
                .texOffs(9, 80).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(-7.3278f, -8.7329f, 0, 0, 0, 5.5851f));
        PartDefinition archSeg3 = arch.addOrReplaceChild("arch_seg_3", CubeListBuilder.create()
                .texOffs(9, 80).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(-3.899f, -10.7125f, 0, 0, 0, 5.9341f));
        PartDefinition archSeg4 = arch.addOrReplaceChild("arch_seg_4", CubeListBuilder.create()
                .texOffs(9, 80).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(-0, -11.4f, 0, 0, 0, 6.2832f));
        PartDefinition archSeg5 = arch.addOrReplaceChild("arch_seg_5", CubeListBuilder.create()
                .texOffs(9, 80).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(3.899f, -10.7125f, 0, 0, 0, 6.6323f));
        PartDefinition archSeg6 = arch.addOrReplaceChild("arch_seg_6", CubeListBuilder.create()
                .texOffs(9, 80).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(7.3278f, -8.7329f, 0, 0, 0, 6.9813f));
        PartDefinition archSeg7 = arch.addOrReplaceChild("arch_seg_7", CubeListBuilder.create()
                .texOffs(9, 80).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(9.8727f, -5.7f, 0, 0, 0, 7.3304f));
        PartDefinition archSeg8 = arch.addOrReplaceChild("arch_seg_8", CubeListBuilder.create()
                .texOffs(9, 80).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(11.2268f, -1.9796f, 0, 0, 0, 7.6794f));
        PartDefinition rightRibbon = body.addOrReplaceChild("right_ribbon", CubeListBuilder.create()
                .texOffs(192, 0).mirror(false).addBox(-1.1f, 0, -0.2f, 2.2f, 12, 0.4f), PartPose.offsetAndRotation(-11.2f, -12, 4.4f, 0.1f, 0.35f, 0.12f));
        PartDefinition leftRibbon = body.addOrReplaceChild("left_ribbon", CubeListBuilder.create()
                .texOffs(192, 0).mirror(true).addBox(-1.1f, 0, -0.2f, 2.2f, 12, 0.4f), PartPose.offsetAndRotation(11.2f, -12, 4.4f, 0.1f, -0.35f, -0.12f));
        PartDefinition rightRibbon2 = rightRibbon.addOrReplaceChild("right_ribbon_2", CubeListBuilder.create()
                .texOffs(35, 46).mirror(false).addBox(-1.2f, 0, -0.2f, 2.4f, 11, 0.4f), PartPose.offsetAndRotation(0, 11.6f, 0, 0.15f, 0, 0.18f));
        PartDefinition leftRibbon2 = leftRibbon.addOrReplaceChild("left_ribbon_2", CubeListBuilder.create()
                .texOffs(35, 46).mirror(true).addBox(-1.2f, 0, -0.2f, 2.4f, 11, 0.4f), PartPose.offsetAndRotation(0, 11.6f, 0, 0.15f, 0, -0.18f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(50, 0).mirror(false).addBox(-4, -8, -3.8f, 8, 8, 7.6f)
                .texOffs(219, 70).mirror(false).addBox(-1.1f, -4.4f, -4.7f, 2.2f, 2.6f, 1)
                .texOffs(151, 46).mirror(false).addBox(-4.3f, -7, -4.1f, 8.6f, 1.4f, 8.2f)
                .texOffs(228, 70).mirror(false).addBox(-1.2f, -7.6f, -4.6f, 2.4f, 2.4f, 0.8f)
                .texOffs(206, 59).mirror(false).addBox(-3.6f, -8.6f, -2.6f, 7.2f, 1.8f, 6.4f), PartPose.offsetAndRotation(0, -14.2f, -0.4f, 0, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(109, 59).mirror(false).addBox(-3.8f, 0, -3.2f, 7.6f, 2.2f, 6.4f), PartPose.offsetAndRotation(0, -0.6f, -0.4f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(237, 70).mirror(false).addBox(-3.6f, -0.8f, -0.6f, 3.6f, 1.3f, 1.2f), PartPose.offsetAndRotation(-0.3f, -5.6f, -4.1f, 0, 0.12f, 0.42f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(237, 70).mirror(true).addBox(0, -0.8f, -0.6f, 3.6f, 1.3f, 1.2f), PartPose.offsetAndRotation(0.3f, -5.6f, -4.1f, 0, -0.12f, -0.42f));
        PartDefinition hair0 = head.addOrReplaceChild("hair_0", CubeListBuilder.create()
                .texOffs(140, 59).mirror(false).addBox(-1.3f, -6.5f, -1.1f, 2.6f, 6.5f, 2.2f), PartPose.offsetAndRotation(-2.6f, -7.6f, 0.6f, -0.7f, 0, -0.45f));
        PartDefinition hair1 = head.addOrReplaceChild("hair_1", CubeListBuilder.create()
                .texOffs(188, 46).mirror(false).addBox(-1.3f, -7.5f, -1.1f, 2.6f, 7.5f, 2.2f), PartPose.offsetAndRotation(0, -7.6f, 1, -0.8f, 0, 0));
        PartDefinition hair2 = head.addOrReplaceChild("hair_2", CubeListBuilder.create()
                .texOffs(140, 59).mirror(false).addBox(-1.3f, -6.5f, -1.1f, 2.6f, 6.5f, 2.2f), PartPose.offsetAndRotation(2.6f, -7.6f, 0.6f, -0.7f, 0, 0.45f));
        PartDefinition hair3 = head.addOrReplaceChild("hair_3", CubeListBuilder.create()
                .texOffs(140, 59).mirror(false).addBox(-1.3f, -7, -1.1f, 2.6f, 7, 2.2f), PartPose.offsetAndRotation(-1.5f, -7.6f, 2.8f, -1.25f, 0, -0.25f));
        PartDefinition hair4 = head.addOrReplaceChild("hair_4", CubeListBuilder.create()
                .texOffs(140, 59).mirror(false).addBox(-1.3f, -7, -1.1f, 2.6f, 7, 2.2f), PartPose.offsetAndRotation(1.5f, -7.6f, 2.8f, -1.25f, 0, 0.25f));
        PartDefinition rightEyeball = head.addOrReplaceChild("right_eyeball", CubeListBuilder.create()
                .texOffs(164, 70).mirror(false).addBox(-1.7f, -1.7f, -1.6f, 3.4f, 3.4f, 2), PartPose.offsetAndRotation(-1.9f, -4.2f, -3.6f, 0, 0.08f, 0));
        PartDefinition leftEyeball = head.addOrReplaceChild("left_eyeball", CubeListBuilder.create()
                .texOffs(164, 70).mirror(true).addBox(-1.7f, -1.7f, -1.6f, 3.4f, 3.4f, 2), PartPose.offsetAndRotation(1.9f, -4.2f, -3.6f, 0, -0.08f, 0));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(177, 70).mirror(false).addBox(-0.8f, -3.4f, -0.8f, 1.6f, 3.6f, 1.6f)
                .texOffs(22, 80).mirror(false).addBox(-0.5f, -5, -0.5f, 1, 1.8f, 1), PartPose.offsetAndRotation(-2.4f, -8, -1.4f, -0.35f, 0, -0.4f));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(177, 70).mirror(true).addBox(-0.8f, -3.4f, -0.8f, 1.6f, 3.6f, 1.6f)
                .texOffs(22, 80).mirror(true).addBox(-0.5f, -5, -0.5f, 1, 1.8f, 1), PartPose.offsetAndRotation(2.4f, -8, -1.4f, -0.35f, 0, 0.4f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(44, 46).mirror(false).addBox(-2, -1.4f, -2, 4, 7.4f, 4), PartPose.offsetAndRotation(-7, -10.4f, 0, -0.4996f, 0, 3.0451f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(201, 46).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 6.2f, 3.6f)
                .texOffs(153, 59).mirror(false).addBox(-2.1f, 0.8f, -2.1f, 4.2f, 4.6f, 4.2f), PartPose.offsetAndRotation(0, 6, 0, 0.0528f, -0.394f, 0.8659f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(237, 59).mirror(false).addBox(-0.7f, -0.4f, -1.9f, 1.4f, 4.4f, 3.8f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(44, 46).mirror(true).addBox(-2, -1.4f, -2, 4, 7.4f, 4), PartPose.offsetAndRotation(7, -10.4f, 0, -0.12f, 0, -0.12f));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(201, 46).mirror(true).addBox(-1.8f, 0, -1.8f, 3.6f, 6.2f, 3.6f)
                .texOffs(153, 59).mirror(true).addBox(-2.1f, 0.8f, -2.1f, 4.2f, 4.6f, 4.2f), PartPose.offsetAndRotation(0, 6, 0, -0.75f, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(186, 70).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition halberd = leftHand.addOrReplaceChild("halberd", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.55f, -29, -0.55f, 1.1f, 43, 1.1f)
                .texOffs(0, 80).mirror(false).addBox(-1, -30.4f, -1, 2, 2, 2)
                .texOffs(218, 46).mirror(false).addBox(-0.9f, -39.4f, -0.35f, 1.8f, 9.2f, 0.7f)
                .texOffs(91, 70).mirror(false).addBox(-1.4f, -28.6f, -1.4f, 2.8f, 4, 2.8f)
                .texOffs(199, 70).mirror(false).addBox(-0.6f, 13.8f, -0.6f, 1.2f, 2.4f, 1.2f), PartPose.offsetAndRotation(0, 1.6f, 0, 0.9064f, -0.0916f, 0.0776f));
        PartDefinition crescentR = halberd.addOrReplaceChild("crescent_r", CubeListBuilder.create()
                .texOffs(0, 70).mirror(false).addBox(-4.6f, -3.6f, -0.3f, 4.6f, 7.2f, 0.6f), PartPose.offsetAndRotation(-0.8f, -34.4f, 0, 0, 0, 0));
        PartDefinition crescentL = halberd.addOrReplaceChild("crescent_l", CubeListBuilder.create()
                .texOffs(0, 70).mirror(true).addBox(0, -3.6f, -0.3f, 4.6f, 7.2f, 0.6f), PartPose.offsetAndRotation(0.8f, -34.4f, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED eye: end
    }

    /**
     * Wind-Following Ear (256x256), the Eye's companion in the same armour, laced and robed in vermilion: a red
     * face, two huge fanned ears with rings of sound shining in their hollows, one horn; his left hand cupped
     * behind his ear; a bronze broad-axe in his right. Model and skin: {@code assets_source/heaven_gods_sentinels.py}.
     */
    public static LayerDefinition earLayer() {
        // GENERATED ear: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(0, 57).mirror(false).addBox(-4.4f, -2, -3, 8.8f, 4, 6)
                .texOffs(31, 57).mirror(false).addBox(-5, -3.4f, -3.5f, 10, 2.8f, 7)
                .texOffs(153, 68).mirror(false).addBox(-2.2f, -4.4f, -4.3f, 4.4f, 4.4f, 1), PartPose.offsetAndRotation(0, 9, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(83, 0).mirror(false).addBox(-2.1f, 0, -0.4f, 4.2f, 13.4f, 0.8f), PartPose.offsetAndRotation(0, 0.6f, -3.7f, -0.08f, 0, 0));
        PartDefinition robeBack = waist.addOrReplaceChild("robe_back", CubeListBuilder.create()
                .texOffs(96, 0).mirror(false).addBox(-4.6f, 0, -0.4f, 9.2f, 13.6f, 0.8f), PartPose.offsetAndRotation(0, 0, 3.1f, 0.14f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(205, 57).mirror(false).addBox(-2.6f, 0, -0.5f, 5.2f, 7.6f, 1), PartPose.offsetAndRotation(-2.7f, -0.8f, -3.3f, -0.14f, 0.18f, 0.1f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(205, 57).mirror(true).addBox(-2.6f, 0, -0.5f, 5.2f, 7.6f, 1), PartPose.offsetAndRotation(2.7f, -0.8f, -3.3f, -0.14f, -0.18f, -0.1f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(156, 0).mirror(false).addBox(-1, 0, -2.8f, 1, 7.2f, 5.6f), PartPose.offsetAndRotation(-4.8f, -0.8f, 0, 0, 0, 0.22f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(156, 0).mirror(true).addBox(0, 0, -2.8f, 1, 7.2f, 5.6f), PartPose.offsetAndRotation(4.8f, -0.8f, 0, 0, 0, -0.22f));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(96, 44).mirror(false).addBox(-0.8f, 0, -0.3f, 1.6f, 10, 0.6f), PartPose.offsetAndRotation(-4.2f, -2.4f, -2.4f, 0.04f, 0, 0.14f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(96, 44).mirror(true).addBox(-0.8f, 0, -0.3f, 1.6f, 10, 0.6f), PartPose.offsetAndRotation(4.2f, -2.4f, -2.4f, 0.04f, 0, -0.14f));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(194, 0).mirror(false).addBox(-2.1f, 0, -2.1f, 4.2f, 7.6f, 4.2f), PartPose.offsetAndRotation(-2.6f, 9, 0.3f, 0, 0, 0.03f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(194, 0).mirror(true).addBox(-2.1f, 0, -2.1f, 4.2f, 7.6f, 4.2f), PartPose.offsetAndRotation(2.6f, 9, 0.3f, 0, 0, -0.03f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(103, 44).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 7, 3.6f)
                .texOffs(220, 57).mirror(false).addBox(-2.2f, 0.4f, -2.5f, 4.4f, 5.6f, 2.6f)
                .texOffs(235, 68).mirror(false).addBox(-2, -1.4f, -2.8f, 4, 2.6f, 1)
                .texOffs(31, 68).mirror(false).addBox(-2.1f, 6.2f, -3.8f, 4.2f, 1.6f, 6)
                .texOffs(166, 68).mirror(false).addBox(-2.3f, 5.6f, -2.3f, 4.6f, 1, 4.6f), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, -0.03f));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(103, 44).mirror(true).addBox(-1.8f, 0, -1.8f, 3.6f, 7, 3.6f)
                .texOffs(220, 57).mirror(true).addBox(-2.2f, 0.4f, -2.5f, 4.4f, 5.6f, 2.6f)
                .texOffs(235, 68).mirror(true).addBox(-2, -1.4f, -2.8f, 4, 2.6f, 1)
                .texOffs(31, 68).mirror(true).addBox(-2.1f, 6.2f, -3.8f, 4.2f, 1.6f, 6)
                .texOffs(166, 68).mirror(true).addBox(-2.3f, 5.6f, -2.3f, 4.6f, 1, 4.6f), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, 0.03f));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(119, 0).mirror(false).addBox(-5.4f, -11.4f, -3.4f, 10.8f, 7.4f, 6.8f)
                .texOffs(120, 44).mirror(false).addBox(-4.6f, -4.4f, -3, 9.2f, 4.6f, 6)
                .texOffs(9, 0).mirror(false).addBox(-5.9f, -11.8f, -3.9f, 11.8f, 7.6f, 7.8f)
                .texOffs(187, 68).mirror(false).addBox(-5, -10.9f, -4.5f, 4.4f, 4.4f, 0.7f)
                .texOffs(187, 68).mirror(false).addBox(0.6f, -10.9f, -4.5f, 4.4f, 4.4f, 0.7f)
                .texOffs(0, 44).mirror(false).addBox(-4.8f, -4.6f, -3.4f, 9.6f, 4.4f, 6.8f)
                .texOffs(200, 68).mirror(false).addBox(-2.4f, -5.4f, -4.3f, 4.8f, 4.8f, 0.8f)
                .texOffs(66, 57).mirror(false).addBox(-6.2f, -12.2f, -3.6f, 12.4f, 1.2f, 7.2f)
                .texOffs(54, 68).mirror(false).addBox(-3.4f, -13.4f, -2.8f, 6.8f, 1.8f, 5.2f)
                .texOffs(114, 68).mirror(false).addBox(-1.8f, -14.4f, -1.8f, 3.6f, 2.2f, 3.6f), PartPose.offsetAndRotation(0, -2.6f, 0, 0, 0, 0));
        PartDefinition rightPauldron = body.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(109, 57).mirror(false).addBox(-4, -1.2f, -3.6f, 6.6f, 2, 7.2f)
                .texOffs(153, 44).mirror(false).addBox(-4.8f, 0.6f, -4, 7.2f, 2.2f, 8)
                .texOffs(35, 44).mirror(false).addBox(-5.6f, 2.6f, -4.4f, 7.8f, 2.2f, 8.8f)
                .texOffs(81, 68).mirror(false).addBox(-6.5f, 0.6f, -2, 1, 4, 4), PartPose.offsetAndRotation(-6.6f, -11.4f, 0, 0, 0, 0.45f));
        PartDefinition leftPauldron = body.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(109, 57).mirror(true).addBox(-2.6f, -1.2f, -3.6f, 6.6f, 2, 7.2f)
                .texOffs(153, 44).mirror(true).addBox(-2.4f, 0.6f, -4, 7.2f, 2.2f, 8)
                .texOffs(35, 44).mirror(true).addBox(-2.2f, 2.6f, -4.4f, 7.8f, 2.2f, 8.8f)
                .texOffs(81, 68).mirror(true).addBox(5.5f, 0.6f, -2, 1, 4, 4), PartPose.offsetAndRotation(6.6f, -11.4f, 0, 0, 0, -0.45f));
        PartDefinition arch = body.addOrReplaceChild("arch", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -11.6f, 4.6f, 0.15f, 0, 0));
        PartDefinition archSeg0 = arch.addOrReplaceChild("arch_seg_0", CubeListBuilder.create()
                .texOffs(40, 78).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(-11.2268f, -1.9796f, 0, 0, 0, 4.8869f));
        PartDefinition archSeg1 = arch.addOrReplaceChild("arch_seg_1", CubeListBuilder.create()
                .texOffs(40, 78).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(-9.8727f, -5.7f, 0, 0, 0, 5.236f));
        PartDefinition archSeg2 = arch.addOrReplaceChild("arch_seg_2", CubeListBuilder.create()
                .texOffs(40, 78).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(-7.3278f, -8.7329f, 0, 0, 0, 5.5851f));
        PartDefinition archSeg3 = arch.addOrReplaceChild("arch_seg_3", CubeListBuilder.create()
                .texOffs(40, 78).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(-3.899f, -10.7125f, 0, 0, 0, 5.9341f));
        PartDefinition archSeg4 = arch.addOrReplaceChild("arch_seg_4", CubeListBuilder.create()
                .texOffs(40, 78).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(-0, -11.4f, 0, 0, 0, 6.2832f));
        PartDefinition archSeg5 = arch.addOrReplaceChild("arch_seg_5", CubeListBuilder.create()
                .texOffs(40, 78).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(3.899f, -10.7125f, 0, 0, 0, 6.6323f));
        PartDefinition archSeg6 = arch.addOrReplaceChild("arch_seg_6", CubeListBuilder.create()
                .texOffs(40, 78).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(7.3278f, -8.7329f, 0, 0, 0, 6.9813f));
        PartDefinition archSeg7 = arch.addOrReplaceChild("arch_seg_7", CubeListBuilder.create()
                .texOffs(40, 78).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(9.8727f, -5.7f, 0, 0, 0, 7.3304f));
        PartDefinition archSeg8 = arch.addOrReplaceChild("arch_seg_8", CubeListBuilder.create()
                .texOffs(40, 78).mirror(false).addBox(-2.2796f, -1, -0.2f, 4.5592f, 2, 0.4f), PartPose.offsetAndRotation(11.2268f, -1.9796f, 0, 0, 0, 7.6794f));
        PartDefinition rightRibbon = body.addOrReplaceChild("right_ribbon", CubeListBuilder.create()
                .texOffs(215, 0).mirror(false).addBox(-1.1f, 0, -0.2f, 2.2f, 12, 0.4f), PartPose.offsetAndRotation(-11.2f, -12, 4.4f, 0.1f, 0.35f, 0.12f));
        PartDefinition leftRibbon = body.addOrReplaceChild("left_ribbon", CubeListBuilder.create()
                .texOffs(215, 0).mirror(true).addBox(-1.1f, 0, -0.2f, 2.2f, 12, 0.4f), PartPose.offsetAndRotation(11.2f, -12, 4.4f, 0.1f, -0.35f, -0.12f));
        PartDefinition rightRibbon2 = rightRibbon.addOrReplaceChild("right_ribbon_2", CubeListBuilder.create()
                .texOffs(70, 44).mirror(false).addBox(-1.2f, 0, -0.2f, 2.4f, 11, 0.4f), PartPose.offsetAndRotation(0, 11.6f, 0, 0.15f, 0, 0.18f));
        PartDefinition leftRibbon2 = leftRibbon.addOrReplaceChild("left_ribbon_2", CubeListBuilder.create()
                .texOffs(70, 44).mirror(true).addBox(-1.2f, 0, -0.2f, 2.4f, 11, 0.4f), PartPose.offsetAndRotation(0, 11.6f, 0, 0.15f, 0, -0.18f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(50, 0).mirror(false).addBox(-4, -8, -3.8f, 8, 8, 7.6f)
                .texOffs(246, 68).mirror(false).addBox(-1.1f, -4.4f, -4.7f, 2.2f, 2.6f, 1)
                .texOffs(186, 44).mirror(false).addBox(-4.3f, -7, -4.1f, 8.6f, 1.4f, 8.2f)
                .texOffs(0, 78).mirror(false).addBox(-1.2f, -7.6f, -4.6f, 2.4f, 2.4f, 0.8f)
                .texOffs(0, 68).mirror(false).addBox(-3.6f, -8.6f, -2.6f, 7.2f, 1.8f, 6.4f), PartPose.offsetAndRotation(0, -14.2f, -0.4f, 0, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(140, 57).mirror(false).addBox(-3.8f, 0, -3.2f, 7.6f, 2.2f, 6.4f), PartPose.offsetAndRotation(0, -0.6f, -0.4f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(9, 78).mirror(false).addBox(-3.6f, -0.8f, -0.6f, 3.6f, 1.3f, 1.2f), PartPose.offsetAndRotation(-0.3f, -5.6f, -4.1f, 0, 0.12f, 0.42f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(9, 78).mirror(true).addBox(0, -0.8f, -0.6f, 3.6f, 1.3f, 1.2f), PartPose.offsetAndRotation(0.3f, -5.6f, -4.1f, 0, -0.12f, -0.42f));
        PartDefinition hair0 = head.addOrReplaceChild("hair_0", CubeListBuilder.create()
                .texOffs(171, 57).mirror(false).addBox(-1.3f, -6.5f, -1.1f, 2.6f, 6.5f, 2.2f), PartPose.offsetAndRotation(-2.6f, -7.6f, 0.6f, -0.7f, 0, -0.45f));
        PartDefinition hair1 = head.addOrReplaceChild("hair_1", CubeListBuilder.create()
                .texOffs(223, 44).mirror(false).addBox(-1.3f, -7.5f, -1.1f, 2.6f, 7.5f, 2.2f), PartPose.offsetAndRotation(0, -7.6f, 1, -0.8f, 0, 0));
        PartDefinition hair2 = head.addOrReplaceChild("hair_2", CubeListBuilder.create()
                .texOffs(171, 57).mirror(false).addBox(-1.3f, -6.5f, -1.1f, 2.6f, 6.5f, 2.2f), PartPose.offsetAndRotation(2.6f, -7.6f, 0.6f, -0.7f, 0, 0.45f));
        PartDefinition hair3 = head.addOrReplaceChild("hair_3", CubeListBuilder.create()
                .texOffs(171, 57).mirror(false).addBox(-1.3f, -7, -1.1f, 2.6f, 7, 2.2f), PartPose.offsetAndRotation(-1.5f, -7.6f, 2.8f, -1.25f, 0, -0.25f));
        PartDefinition hair4 = head.addOrReplaceChild("hair_4", CubeListBuilder.create()
                .texOffs(171, 57).mirror(false).addBox(-1.3f, -7, -1.1f, 2.6f, 7, 2.2f), PartPose.offsetAndRotation(1.5f, -7.6f, 2.8f, -1.25f, 0, 0.25f));
        PartDefinition rightBigear = head.addOrReplaceChild("right_bigear", CubeListBuilder.create()
                .texOffs(224, 0).mirror(false).addBox(-9, -6.2f, -0.35f, 9, 11.4f, 0.7f), PartPose.offsetAndRotation(-3.9f, -4.4f, 0, 0, 0.55f, -0.12f));
        PartDefinition leftBigear = head.addOrReplaceChild("left_bigear", CubeListBuilder.create()
                .texOffs(224, 0).mirror(true).addBox(0, -6.2f, -0.35f, 9, 11.4f, 0.7f), PartPose.offsetAndRotation(3.9f, -4.4f, 0, 0, -0.55f, 0.12f));
        PartDefinition horn = head.addOrReplaceChild("horn", CubeListBuilder.create()
                .texOffs(131, 68).mirror(false).addBox(-0.9f, -4, -0.9f, 1.8f, 4.2f, 1.8f)
                .texOffs(22, 78).mirror(false).addBox(-0.55f, -5.8f, -0.55f, 1.1f, 2, 1.1f), PartPose.offsetAndRotation(0, -8.4f, -2.2f, -0.3f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(79, 44).mirror(false).addBox(-2, -1.4f, -2, 4, 7.4f, 4), PartPose.offsetAndRotation(-7, -10.4f, 0, -0.12f, 0, 0.12f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(236, 44).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 6.2f, 3.6f)
                .texOffs(184, 57).mirror(false).addBox(-2.1f, 0.8f, -2.1f, 4.2f, 4.6f, 4.2f), PartPose.offsetAndRotation(0, 6, 0, -0.75f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(213, 68).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 3, 3), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(79, 44).mirror(true).addBox(-2, -1.4f, -2, 4, 7.4f, 4), PartPose.offsetAndRotation(7, -10.4f, 0, 0.2368f, 0, -2.3806f));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(236, 44).mirror(true).addBox(-1.8f, 0, -1.8f, 3.6f, 6.2f, 3.6f)
                .texOffs(184, 57).mirror(true).addBox(-2.1f, 0.8f, -2.1f, 4.2f, 4.6f, 4.2f), PartPose.offsetAndRotation(0, 6, 0, 0.0949f, -0.2352f, -1.6917f));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(92, 68).mirror(true).addBox(-0.6f, -0.2f, -1.8f, 1.2f, 3.6f, 3.6f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition axe = rightHand.addOrReplaceChild("axe", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.6f, -27, -0.6f, 1.2f, 41, 1.2f)
                .texOffs(31, 78).mirror(false).addBox(-1, -28.4f, -1, 2, 2, 2)
                .texOffs(105, 68).mirror(false).addBox(-0.6f, -33.6f, -0.6f, 1.2f, 5.4f, 1.2f)
                .texOffs(140, 68).mirror(false).addBox(-1.4f, -16.4f, -1.4f, 2.8f, 4, 2.8f)
                .texOffs(226, 68).mirror(false).addBox(-0.6f, 13.8f, -0.6f, 1.2f, 2.4f, 1.2f)
                .texOffs(53, 78).mirror(false).addBox(0.6f, -25.6f, -0.4f, 3, 1.6f, 0.8f), PartPose.offsetAndRotation(0, 1.6f, 0, 0.9064f, 0.0916f, -0.0776f));
        PartDefinition axeBlade = axe.addOrReplaceChild("axe_blade", CubeListBuilder.create()
                .texOffs(171, 0).mirror(false).addBox(-9.4f, -6, -0.45f, 9.4f, 12.4f, 0.9f), PartPose.offsetAndRotation(-0.8f, -22.6f, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED ear: end
    }

    /**
     * The Eye's and the Ear's pose: floating, their ribbons stirring; the Eye shading his brow, the Ear's hand
     * cupped behind his ear. A blow: the Eye thrusts his halberd, the Ear chops with his axe. Winding up: the Eye
     * leans forward staring, his eyes bulging and blazing; the Ear cups both hands behind his ears, which fan wider.
     */
    public static final class SentinelModel<T extends HeavenMonster> extends GodModel<T> {
        public static final float SIZE = 0.72f;
        private final boolean eye;
        private final ModelPart body, head, rightArm, leftArm, rightForearm, leftForearm, arch, rightRibbon, leftRibbon,
                rightRibbon2, leftRibbon2, robe, apron, rightMark, leftMark;

        public SentinelModel(ModelPart root, boolean eye) {
            super(root);
            this.eye = eye;
            ModelPart waist = root.getChild("waist");
            body = waist.getChild("body");
            head = body.getChild("head");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            arch = body.getChild("arch");
            rightRibbon = body.getChild("right_ribbon");
            leftRibbon = body.getChild("left_ribbon");
            rightRibbon2 = rightRibbon.getChild("right_ribbon_2");
            leftRibbon2 = leftRibbon.getChild("left_ribbon_2");
            robe = waist.getChild("robe_back");
            apron = waist.getChild("apron");
            rightMark = head.getChild(eye ? "right_eyeball" : "right_bigear");
            leftMark = head.getChild(eye ? "left_eyeball" : "left_bigear");
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float drift = Mth.sin(age * 0.075f);
            root.y = drift * 0.6f;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            float sway = Mth.cos(age * 0.06f) * 0.04f;
            if (eye) {
                // shading his brow, peering; winding up, leaning forward staring, his eyes bulging; a blow, the halberd thrust
                turn(body, wind * 0.32f + strike * 0.12f, 0);
                head.xRot -= wind * 0.42f;
                turn(rightArm, sway * 0.5f, 0);
                turn(rightForearm, 0, 0);
                turn(leftArm, -sway - strike * 0.9f, 0);
                turn(leftForearm, strike * 0.5f, 0);
                float bulge = 1 + wind * 0.6f + Math.max(0, Mth.sin(age * 0.2f)) * 0.08f;
                rightMark.zScale = leftMark.zScale = bulge;
                rightMark.xScale = leftMark.xScale = rightMark.yScale = leftMark.yScale = 1 + wind * 0.15f;
            } else {
                // his hand cupped behind his ear; winding up, both hands cupped and the ears fanned wide; a blow, the axe chopped down
                turn(body, -wind * 0.08f + strike * 0.15f, 0);
                head.zRot = Mth.sin(age * 0.04f) * 0.06f * (1 - wind);
                PartPose ra = rightArm.getInitialPose(), la = leftArm.getInitialPose();
                PartPose rf = rightForearm.getInitialPose(), lf = leftForearm.getInitialPose();
                rightArm.xRot = Mth.lerp(wind, ra.xRot + sway - strike * 1.5f, la.xRot);
                rightArm.yRot = Mth.lerp(wind, ra.yRot, -la.yRot);
                rightArm.zRot = Mth.lerp(wind, ra.zRot, -la.zRot);
                rightForearm.xRot = Mth.lerp(wind, rf.xRot + strike * 0.3f, lf.xRot);
                rightForearm.yRot = Mth.lerp(wind, rf.yRot, -lf.yRot);
                rightForearm.zRot = Mth.lerp(wind, rf.zRot, -lf.zRot);
                turn(leftArm, sway * 0.5f, 0);
                float fan = wind * 0.3f + Mth.sin(age * 0.09f) * 0.04f;
                turn(rightMark, 0, fan, 0);
                turn(leftMark, 0, -fan, 0);
            }
            // the ribbon stirring
            float flow = Mth.sin(age * 0.09f), flow2 = Mth.sin(age * 0.09f + 1.2f);
            turn(arch, flow * 0.04f + amount * 0.25f, 0);
            turn(rightRibbon, 0.1f * flow + amount * 0.5f, -0.06f * flow2);
            turn(leftRibbon, 0.1f * flow2 + amount * 0.5f, 0.06f * flow);
            turn(rightRibbon2, 0.12f * flow2 + amount * 0.3f, -0.08f * flow);
            turn(leftRibbon2, 0.12f * flow + amount * 0.3f, 0.08f * flow2);
            turn(robe, amount * 0.3f + flow * 0.02f, 0);
            turn(apron, -amount * 0.25f + flow2 * 0.03f, 0);
        }
    }
}
