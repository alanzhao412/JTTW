package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.heaven.HeavenBoss;
import com.jttw.engine.client.model.XiaotianQuanAnimations;
import com.jttw.engine.client.model.XiaotianQuanModel;
import com.jttw.engine.entity.heaven.ErlangShenEntity;
import com.jttw.engine.entity.heaven.HeavenlyKingEntity;
import com.jttw.engine.entity.heaven.LiJingEntity;
import com.jttw.engine.entity.heaven.NezhaEntity;
import com.jttw.engine.registry.HeavenRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.animation.KeyframeAnimations;
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
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * How Heaven's great names look (its bosses): the Four Heavenly Kings, Li Jing, Nezha and Erlang Shen.
 * Each model and its skins are made by a generator in {@code assets_source/} (heaven_great.py and its
 * heaven_great_*.py), which writes the model's code between the GENERATED markers below; each skin has a
 * glow drawn full-bright over it, and a blaze drawn in its place while the boss winds up his great blow
 * ({@link HeavenBoss#winding()}).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeavenGreatRenderers {

    private HeavenGreatRenderers() {}

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name), "main");
    }

    public static final ModelLayerLocation KING_LAYER = layer("heaven_king"), LI_JING_LAYER = layer("heaven_li_jing"),
            NEZHA_LAYER = layer("heaven_nezha"), ERLANG_LAYER = layer("heaven_erlang_shen");

    /** How much larger than his pixels each is drawn (the generators print the reach this gives). */
    public static final float KING_SIZE = 0.88f, LI_SIZE = 0.84f, NEZHA_SIZE = 0.74f, ERLANG_SIZE = 0.72f;

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KING_LAYER, HeavenGreatRenderers::kingLayer);
        event.registerLayerDefinition(LI_JING_LAYER, HeavenGreatRenderers::liJingLayer);
        event.registerLayerDefinition(NEZHA_LAYER, HeavenGreatRenderers::nezhaLayer);
        event.registerLayerDefinition(ERLANG_LAYER, HeavenGreatRenderers::erlangLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HeavenRegistry.KING_CHIGUO.get(), ctx -> new KingRenderer<>(ctx, "chiguo"));
        event.registerEntityRenderer(HeavenRegistry.KING_ZENGZHANG.get(), ctx -> new KingRenderer<>(ctx, "zengzhang"));
        event.registerEntityRenderer(HeavenRegistry.KING_GUANGMU.get(), ctx -> new KingRenderer<>(ctx, "guangmu"));
        event.registerEntityRenderer(HeavenRegistry.KING_DUOWEN.get(), ctx -> new KingRenderer<>(ctx, "duowen"));
        event.registerEntityRenderer(HeavenRegistry.LI_JING.get(), LiJingRenderer::new);
        event.registerEntityRenderer(HeavenRegistry.NEZHA.get(), NezhaRenderer::new);
        event.registerEntityRenderer(HeavenRegistry.ERLANG_SHEN.get(), ErlangRenderer::new);
    }

    // ── Shared ──────────────────────────────────────────────────────────

    /** A part turned this much from its rest. */
    static void turn(ModelPart p, float x, float y, float z) {
        PartPose i = p.getInitialPose();
        p.xRot = i.xRot + x;
        p.yRot = i.yRot + y;
        p.zRot = i.zRot + z;
    }

    /** A part moved this much from its rest. */
    static void move(ModelPart p, float x, float y, float z) {
        PartPose i = p.getInitialPose();
        p.x = i.x + x;
        p.y = i.y + y;
        p.z = i.z + z;
    }

    /** The parts named {@code name_0}, {@code name_1}, ... under {@code parent}. */
    static List<ModelPart> numbered(ModelPart parent, String name) {
        List<ModelPart> out = new ArrayList<>();
        for (int k = 0; parent.hasChild(name + "_" + k); k++) out.add(parent.getChild(name + "_" + k));
        return out;
    }

    /** A hanging chain of parts (a streamer, a sash, a tie): {@code name}, then {@code name_2}, ... each a child of the last. */
    static List<ModelPart> chain(ModelPart parent, String name) {
        List<ModelPart> out = new ArrayList<>();
        ModelPart p = parent.getChild(name);
        out.add(p);
        for (int k = 2; p.hasChild(name + "_" + k); k++) {
            p = p.getChild(name + "_" + k);
            out.add(p);
        }
        return out;
    }

    /** A streamer fluttering: each link swaying a little more than the last, blown back as he moves. */
    static void flutter(List<ModelPart> links, float age, float amount, float phase, float side, float gust) {
        for (int k = 0; k < links.size(); k++) {
            float f = k + 1;
            turn(links.get(k), amount * 0.18f * f * 0.5f + Mth.sin(age * 0.09f + phase + k * 0.8f) * 0.035f * f * gust,
                    0, side * (Mth.sin(age * 0.11f + phase + k * 0.6f) * 0.04f * f * gust + amount * 0.04f * f));
        }
    }

    /** How far into his wind-up he is: 0 at rest, rising to 1 over its first ticks and held till the blow. */
    static float windUp(HeavenBoss e, int windUpTicks, float partial) {
        int w = e.winding();
        if (w <= 0) return 0;
        return Mth.clamp((windUpTicks + 1 - w + partial) / 6f, 0, 1);
    }

    /** A boss's renderer: his skin, its glow full-bright over it, and in his wind-up the blaze in the glow's place. */
    abstract static class GreatRenderer<T extends HeavenBoss, M extends EntityModel<T>> extends MobRenderer<T, M> {
        private final float size;
        private final ResourceLocation skin;
        private final RenderType glow, blaze;

        GreatRenderer(EntityRendererProvider.Context ctx, M model, float shadow, float size, String name) {
            super(ctx, model, shadow);
            this.size = size;
            skin = tex(name);
            glow = RenderType.eyes(tex(name + "_glow"));
            blaze = RenderType.eyes(tex(name + "_blaze"));
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, T e,
                                   float swing, float amount, float partial, float age, float yaw, float pitch) {
                    if (e.isInvisible()) return;
                    VertexConsumer vc = buffers.getBuffer(e.winding() > 0 ? blaze : glow);
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
            return skin;
        }
    }

    // ── The Four Heavenly Kings ─────────────────────────────────────────

    /**
     * The Four Heavenly Kings (256x256, one frame for the four; heaven_great.py): towering armoured
     * guardians as temple gate halls sculpt them. A tall gold crown of flame-arched plaques, a flaming jewel,
     * phoenix wings; a fierce face, eyes bulging under brows knit in a V, moustache and (but the East) a
     * three-locked beard; a moulded breastplate with two round mirrors, fish-scale gold over the belly, a
     * lion's mask biting the belt, tiered pauldrons each bitten by a gold lion's head, silk flaring at the
     * elbows; lamellar tassets over a long robe; the heavenly ribbon arcing behind his head and streaming
     * past his arms; a short cape. Each in his quarter's colours with his attribute: the East's pipa, the
     * South's great sword, the West's serpent and pearl, the North's umbrella.
     */
    public static LayerDefinition kingLayer() {
        // GENERATED kings: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(90, 69).mirror(false).addBox(-7, -2, -4.6f, 14, 4, 9.2f)
                .texOffs(88, 50).mirror(false).addBox(-8, -4.6f, -5.8f, 16, 3.8f, 11.6f)
                .texOffs(146, 113).mirror(false).addBox(-3.6f, -6.8f, -7.8f, 7.2f, 6.2f, 2)
                .texOffs(167, 113).mirror(false).addBox(-4.6f, -7.6f, -6.8f, 9.2f, 7.6f, 1), PartPose.offsetAndRotation(0, 4, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(0, 50).mirror(false).addBox(-3.2f, 0, -0.5f, 6.4f, 16.5f, 0.8f), PartPose.offsetAndRotation(0, 0.8f, -6, -0.06f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(139, 69).mirror(false).addBox(-3.8f, 0, -0.6f, 7.6f, 11.5f, 1.2f), PartPose.offsetAndRotation(-4.4f, 0.2f, -5.1f, -0.14f, 0.12f, 0.12f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(139, 69).mirror(true).addBox(-3.8f, 0, -0.6f, 7.6f, 11.5f, 1.2f), PartPose.offsetAndRotation(4.4f, 0.2f, -5.1f, -0.14f, -0.12f, -0.12f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(69, 0).mirror(false).addBox(-1.2f, 0, -4.8f, 1.2f, 11, 9.6f), PartPose.offsetAndRotation(-7.6f, 0.2f, 0.2f, 0, 0, 0.22f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(69, 0).mirror(true).addBox(0, 0, -4.8f, 1.2f, 11, 9.6f), PartPose.offsetAndRotation(7.6f, 0.2f, 0.2f, 0, 0, -0.22f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(160, 69).mirror(false).addBox(-7.2f, 0, -0.6f, 14.4f, 12, 1.2f), PartPose.offsetAndRotation(0, 0.2f, 5.4f, 0.14f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(192, 125).mirror(false).addBox(-1, 0, -0.3f, 2, 6, 0.6f), PartPose.offsetAndRotation(-7.9f, -1.6f, -3.2f, 0.02f, 0, 0.1f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(192, 125).mirror(true).addBox(-1, 0, -0.3f, 2, 6, 0.6f), PartPose.offsetAndRotation(7.9f, -1.6f, -3.2f, 0.02f, 0, -0.1f));
        PartDefinition rightSash2 = rightSash.addOrReplaceChild("right_sash_2", CubeListBuilder.create()
                .texOffs(199, 125).mirror(false).addBox(-1.1f, 0, -0.3f, 2.2f, 6, 0.6f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.06f, 0, 0));
        PartDefinition leftSash2 = leftSash.addOrReplaceChild("left_sash_2", CubeListBuilder.create()
                .texOffs(199, 125).mirror(true).addBox(-1.1f, 0, -0.3f, 2.2f, 6, 0.6f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.06f, 0, 0));
        PartDefinition rightSash3 = rightSash2.addOrReplaceChild("right_sash_3", CubeListBuilder.create()
                .texOffs(80, 135).mirror(false).addBox(-1.3f, 0, -0.3f, 2.6f, 5, 0.6f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.08f, 0, 0));
        PartDefinition leftSash3 = leftSash2.addOrReplaceChild("left_sash_3", CubeListBuilder.create()
                .texOffs(80, 135).mirror(true).addBox(-1.3f, 0, -0.3f, 2.6f, 5, 0.6f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.08f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(38, 50).mirror(false).addBox(-3.1f, 0, -3.1f, 6.2f, 9.4f, 6.2f), PartPose.offsetAndRotation(-3.8f, 6, 0.4f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(38, 50).mirror(true).addBox(-3.1f, 0, -3.1f, 6.2f, 9.4f, 6.2f), PartPose.offsetAndRotation(3.8f, 6, 0.4f, 0, 0, 0));
        PartDefinition rightRobe = rightLeg.addOrReplaceChild("right_robe", CubeListBuilder.create()
                .texOffs(197, 50).mirror(false).addBox(-3.5f, 0, -0.5f, 7, 13.6f, 0.6f), PartPose.offsetAndRotation(0, -0.6f, -3.3f, -0.05f, 0, 0.04f));
        PartDefinition leftRobe = leftLeg.addOrReplaceChild("left_robe", CubeListBuilder.create()
                .texOffs(197, 50).mirror(true).addBox(-3.5f, 0, -0.5f, 7, 13.6f, 0.6f), PartPose.offsetAndRotation(0, -0.6f, -3.3f, -0.05f, 0, -0.04f));
        PartDefinition rightRobeBack = rightLeg.addOrReplaceChild("right_robe_back", CubeListBuilder.create()
                .texOffs(197, 50).mirror(false).addBox(-3.5f, 0, -0.1f, 7, 13.6f, 0.6f), PartPose.offsetAndRotation(0, -0.6f, 3.3f, 0.06f, 0, 0.04f));
        PartDefinition leftRobeBack = leftLeg.addOrReplaceChild("left_robe_back", CubeListBuilder.create()
                .texOffs(197, 50).mirror(true).addBox(-3.5f, 0, -0.1f, 7, 13.6f, 0.6f), PartPose.offsetAndRotation(0, -0.6f, 3.3f, 0.06f, 0, -0.04f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(155, 85).mirror(false).addBox(-2.7f, 0, -2.7f, 5.4f, 6, 5.4f)
                .texOffs(58, 113).mirror(false).addBox(-3.1f, 0.6f, -3.4f, 6.2f, 5.2f, 3.4f)
                .texOffs(133, 100).mirror(false).addBox(-3.2f, 5.6f, -3.6f, 6.4f, 3.4f, 6.8f)
                .texOffs(13, 143).mirror(false).addBox(-2.4f, 7, -5.6f, 4.8f, 2, 2.2f)
                .texOffs(16, 150).mirror(false).addBox(-1.4f, 5.8f, -6.3f, 2.8f, 2, 1), PartPose.offsetAndRotation(0, 9, 0, 0, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(155, 85).mirror(true).addBox(-2.7f, 0, -2.7f, 5.4f, 6, 5.4f)
                .texOffs(58, 113).mirror(true).addBox(-3.1f, 0.6f, -3.4f, 6.2f, 5.2f, 3.4f)
                .texOffs(133, 100).mirror(true).addBox(-3.2f, 5.6f, -3.6f, 6.4f, 3.4f, 6.8f)
                .texOffs(13, 143).mirror(true).addBox(-2.4f, 7, -5.6f, 4.8f, 2, 2.2f)
                .texOffs(16, 150).mirror(true).addBox(-1.4f, 5.8f, -6.3f, 2.8f, 2, 1), PartPose.offsetAndRotation(0, 9, 0, 0, 0, 0));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(18, 0).mirror(false).addBox(-7.5f, -16, -5, 15, 15, 10)
                .texOffs(94, 0).mirror(false).addBox(-8.3f, -16.4f, -5.9f, 16.6f, 9, 11.8f)
                .texOffs(153, 0).mirror(false).addBox(-7.4f, -8, -5.6f, 14.8f, 7.6f, 11.2f)
                .texOffs(208, 125).mirror(false).addBox(-5.4f, -7.4f, -6.7f, 10.8f, 4.8f, 1.2f)
                .texOffs(235, 125).mirror(false).addBox(-6.4f, -15.2f, -6.5f, 5.2f, 5.2f, 0.7f)
                .texOffs(235, 125).mirror(false).addBox(1.2f, -15.2f, -6.5f, 5.2f, 5.2f, 0.7f)
                .texOffs(89, 135).mirror(false).addBox(-1.4f, -14.4f, -6.4f, 2.8f, 4.2f, 0.6f)
                .texOffs(180, 85).mirror(false).addBox(-5, -18.2f, -4.2f, 10, 2.6f, 8.4f)
                .texOffs(0, 69).mirror(false).addBox(-9.2f, -17, -6.4f, 18.4f, 1.4f, 12.8f)
                .texOffs(98, 135).mirror(false).addBox(-8.8f, -16.4f, -6.5f, 17.6f, 4.6f, 0.5f)
                .texOffs(98, 135).mirror(false).addBox(-8.8f, -16.4f, 6, 17.6f, 4.6f, 0.5f), PartPose.offsetAndRotation(0, -3, 0, 0, 0, 0));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(195, 69).mirror(false).addBox(-8.4f, 0, -0.4f, 16.8f, 13, 0.8f), PartPose.offsetAndRotation(0, -16.2f, 5.9f, 0.12f, 0, 0));
        PartDefinition capeLower = cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(0, 100).mirror(false).addBox(-8.8f, 0, -0.4f, 17.6f, 11, 0.8f), PartPose.offsetAndRotation(0, 12.8f, 0, 0.06f, 0, 0));
        PartDefinition ribbonArc0 = body.addOrReplaceChild("ribbon_arc_0", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(-13.552f, -14.4868f, 8.2f, 0, 0, -1.8245f));
        PartDefinition ribbonArc1 = body.addOrReplaceChild("ribbon_arc_1", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(-13.9997f, -18.0977f, 8.2f, 0, 0, -1.5638f));
        PartDefinition ribbonArc2 = body.addOrReplaceChild("ribbon_arc_2", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(-13.5017f, -21.7021f, 8.2f, 0, 0, -1.3032f));
        PartDefinition ribbonArc3 = body.addOrReplaceChild("ribbon_arc_3", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(-12.0916f, -25.0564f, 8.2f, 0, 0, -1.0425f));
        PartDefinition ribbonArc4 = body.addOrReplaceChild("ribbon_arc_4", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(-9.8649f, -27.934f, 8.2f, 0, 0, -0.7819f));
        PartDefinition ribbonArc5 = body.addOrReplaceChild("ribbon_arc_5", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(-6.9718f, -30.1406f, 8.2f, 0, 0, -0.5213f));
        PartDefinition ribbonArc6 = body.addOrReplaceChild("ribbon_arc_6", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(-3.6077f, -31.5272f, 8.2f, 0, 0, -0.2606f));
        PartDefinition ribbonArc7 = body.addOrReplaceChild("ribbon_arc_7", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(0, -32, 8.2f, 0, 0, 0));
        PartDefinition ribbonArc8 = body.addOrReplaceChild("ribbon_arc_8", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(3.6077f, -31.5272f, 8.2f, 0, 0, 0.2606f));
        PartDefinition ribbonArc9 = body.addOrReplaceChild("ribbon_arc_9", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(6.9718f, -30.1406f, 8.2f, 0, 0, 0.5213f));
        PartDefinition ribbonArc10 = body.addOrReplaceChild("ribbon_arc_10", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(9.8649f, -27.934f, 8.2f, 0, 0, 0.7819f));
        PartDefinition ribbonArc11 = body.addOrReplaceChild("ribbon_arc_11", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(12.0916f, -25.0564f, 8.2f, 0, 0, 1.0425f));
        PartDefinition ribbonArc12 = body.addOrReplaceChild("ribbon_arc_12", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(13.5017f, -21.7021f, 8.2f, 0, 0, 1.3032f));
        PartDefinition ribbonArc13 = body.addOrReplaceChild("ribbon_arc_13", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(13.9997f, -18.0977f, 8.2f, 0, 0, 1.5638f));
        PartDefinition ribbonArc14 = body.addOrReplaceChild("ribbon_arc_14", CubeListBuilder.create()
                .texOffs(169, 143).mirror(false).addBox(-2.1193f, -1.3f, -0.3f, 4.2386f, 2.6f, 0.6f), PartPose.offsetAndRotation(13.552f, -14.4868f, 8.2f, 0, 0, 1.8245f));
        PartDefinition rightStreamer = body.addOrReplaceChild("right_streamer", CubeListBuilder.create()
                .texOffs(19, 125).mirror(false).addBox(-1.3f, 0, -0.3f, 2.6f, 7, 0.6f), PartPose.offsetAndRotation(-12.9806f, -22.8445f, 8.2f, 0.15f, 0, 0.18f));
        PartDefinition leftStreamer = body.addOrReplaceChild("left_streamer", CubeListBuilder.create()
                .texOffs(19, 125).mirror(true).addBox(-1.3f, 0, -0.3f, 2.6f, 7, 0.6f), PartPose.offsetAndRotation(12.9806f, -22.8445f, 8.2f, 0.15f, 0, -0.18f));
        PartDefinition rightStreamer2 = rightStreamer.addOrReplaceChild("right_streamer_2", CubeListBuilder.create()
                .texOffs(19, 125).mirror(false).addBox(-1.4f, 0, -0.3f, 2.8f, 7, 0.6f), PartPose.offsetAndRotation(0, 6.6f, 0, 0.1f, 0, 0));
        PartDefinition leftStreamer2 = leftStreamer.addOrReplaceChild("left_streamer_2", CubeListBuilder.create()
                .texOffs(19, 125).mirror(true).addBox(-1.4f, 0, -0.3f, 2.8f, 7, 0.6f), PartPose.offsetAndRotation(0, 6.6f, 0, 0.1f, 0, 0));
        PartDefinition rightStreamer3 = rightStreamer2.addOrReplaceChild("right_streamer_3", CubeListBuilder.create()
                .texOffs(0, 135).mirror(false).addBox(-1.5f, 0, -0.3f, 3, 6, 0.6f), PartPose.offsetAndRotation(0, 6.6f, 0, 0.14f, 0, 0));
        PartDefinition leftStreamer3 = leftStreamer2.addOrReplaceChild("left_streamer_3", CubeListBuilder.create()
                .texOffs(0, 135).mirror(true).addBox(-1.5f, 0, -0.3f, 3, 6, 0.6f), PartPose.offsetAndRotation(0, 6.6f, 0, 0.14f, 0, 0));
        PartDefinition rightStreamer4 = rightStreamer3.addOrReplaceChild("right_streamer_4", CubeListBuilder.create()
                .texOffs(137, 135).mirror(false).addBox(-1.6f, 0, -0.3f, 3.2f, 5, 0.6f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.18f, 0, 0));
        PartDefinition leftStreamer4 = leftStreamer3.addOrReplaceChild("left_streamer_4", CubeListBuilder.create()
                .texOffs(137, 135).mirror(true).addBox(-1.6f, 0, -0.3f, 3.2f, 5, 0.6f), PartPose.offsetAndRotation(0, 5.6f, 0, 0.18f, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(208, 0).mirror(false).addBox(-4.6f, -10.6f, -4.4f, 9.2f, 9.2f, 8.8f)
                .texOffs(162, 100).mirror(false).addBox(-4.2f, -1.8f, -4.1f, 8.4f, 2.6f, 7.8f)
                .texOffs(81, 113).mirror(false).addBox(-5, -5.8f, -3.4f, 10, 3, 6.2f)
                .texOffs(148, 135).mirror(false).addBox(-1.3f, -7, -5.5f, 2.6f, 3.4f, 1.2f)
                .texOffs(25, 150).mirror(false).addBox(-2.1f, -4.4f, -5.1f, 4.2f, 1.2f, 0.8f)
                .texOffs(145, 50).mirror(false).addBox(-4.9f, -10.8f, -0.8f, 9.8f, 9.6f, 5.8f), PartPose.offsetAndRotation(0, -18, -0.4f, 0, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(38, 150).mirror(false).addBox(-1.5f, -0.95f, -0.6f, 3, 1.9f, 0.8f), PartPose.offsetAndRotation(-2.3f, -7, -4.45f, 0, 0.12f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(38, 150).mirror(true).addBox(-1.5f, -0.95f, -0.6f, 3, 1.9f, 0.8f), PartPose.offsetAndRotation(2.3f, -7, -4.45f, 0, -0.12f, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(182, 143).mirror(false).addBox(-4.6f, -1.1f, -0.8f, 4.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(-0.4f, -8.3f, -4.7f, 0, 0.1f, 0.3f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(182, 143).mirror(true).addBox(0, -1.1f, -0.8f, 4.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(0.4f, -8.3f, -4.7f, 0, -0.1f, -0.3f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(190, 113).mirror(false).addBox(-1, -2, -1.2f, 1, 5.6f, 2.4f), PartPose.offsetAndRotation(-4.7f, -6.8f, -0.2f, 0, 0, 0.08f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(190, 113).mirror(true).addBox(0, -2, -1.2f, 1, 5.6f, 2.4f), PartPose.offsetAndRotation(4.7f, -6.8f, -0.2f, 0, 0, -0.08f));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(197, 143).mirror(false).addBox(-4.2f, -0.55f, -0.6f, 4.2f, 1.1f, 1.2f), PartPose.offsetAndRotation(-0.5f, -3.7f, -5.3f, 0, 0.25f, -0.32f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(197, 143).mirror(true).addBox(0, -0.55f, -0.6f, 4.2f, 1.1f, 1.2f), PartPose.offsetAndRotation(0.5f, -3.7f, -5.3f, 0, -0.25f, 0.32f));
        PartDefinition rightMoustacheTip = rightMoustache.addOrReplaceChild("right_moustache_tip", CubeListBuilder.create()
                .texOffs(47, 150).mirror(false).addBox(-2.6f, -0.45f, -0.5f, 2.6f, 0.9f, 1), PartPose.offsetAndRotation(-4, 0, 0, 0, 0.2f, 0.95f));
        PartDefinition leftMoustacheTip = leftMoustache.addOrReplaceChild("left_moustache_tip", CubeListBuilder.create()
                .texOffs(47, 150).mirror(true).addBox(0, -0.45f, -0.5f, 2.6f, 0.9f, 1), PartPose.offsetAndRotation(4, 0, 0, 0, -0.2f, -0.95f));
        PartDefinition beard = head.addOrReplaceChild("beard", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -1, -4, 0, 0, 0));
        PartDefinition beardC = beard.addOrReplaceChild("beard_c", CubeListBuilder.create()
                .texOffs(116, 113).mirror(false).addBox(-1.6f, 0, -1, 3.2f, 7.5f, 2), PartPose.offsetAndRotation(0, 0.4f, 0, 0.12f, 0, 0));
        PartDefinition beardTip = beardC.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(159, 135).mirror(false).addBox(-1.1f, 0, -0.8f, 2.2f, 3.6f, 1.6f), PartPose.offsetAndRotation(0, 7.2f, 0, -0.12f, 0, 0));
        PartDefinition rightBeard = beard.addOrReplaceChild("right_beard", CubeListBuilder.create()
                .texOffs(199, 113).mirror(false).addBox(-1.2f, 0, -0.9f, 2.4f, 6.8f, 1.8f), PartPose.offsetAndRotation(-2.7f, -0.6f, 0.6f, 0.12f, 0.2f, 0.28f));
        PartDefinition leftBeard = beard.addOrReplaceChild("left_beard", CubeListBuilder.create()
                .texOffs(199, 113).mirror(true).addBox(-1.2f, 0, -0.9f, 2.4f, 6.8f, 1.8f), PartPose.offsetAndRotation(2.7f, -0.6f, 0.6f, 0.12f, -0.2f, -0.28f));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(112, 85).mirror(false).addBox(-5.1f, -1.8f, -4.9f, 10.2f, 2.2f, 9.8f)
                .texOffs(0, 85).mirror(false).addBox(-4.2f, -6, -4.2f, 8.4f, 4.4f, 8.4f)
                .texOffs(129, 113).mirror(false).addBox(-3.3f, -10.4f, -5.6f, 6.6f, 9, 0.8f)
                .texOffs(28, 125).mirror(false).addBox(-1.6f, -9.2f, -1.6f, 3.2f, 3.4f, 3.2f)
                .texOffs(170, 135).mirror(false).addBox(-1.1f, -12, -1.1f, 2.2f, 2.9f, 2.2f), PartPose.offsetAndRotation(0, -10.4f, 0, 0, 0, 0));
        PartDefinition rightCrownPetal = crown.addOrReplaceChild("right_crown_petal", CubeListBuilder.create()
                .texOffs(210, 113).mirror(false).addBox(-2.4f, -7.6f, -0.4f, 4.8f, 7.6f, 0.8f), PartPose.offsetAndRotation(-3.9f, -1.4f, -3.4f, 0, 0.72f, 0));
        PartDefinition leftCrownPetal = crown.addOrReplaceChild("left_crown_petal", CubeListBuilder.create()
                .texOffs(210, 113).mirror(true).addBox(-2.4f, -7.6f, -0.4f, 4.8f, 7.6f, 0.8f), PartPose.offsetAndRotation(3.9f, -1.4f, -3.4f, 0, -0.72f, 0));
        PartDefinition rightCrownSide = crown.addOrReplaceChild("right_crown_side", CubeListBuilder.create()
                .texOffs(45, 125).mirror(false).addBox(-2.1f, -6.2f, -0.4f, 4.2f, 6.2f, 0.8f), PartPose.offsetAndRotation(-5, -1.4f, 0.4f, 0, 1.5f, 0));
        PartDefinition leftCrownSide = crown.addOrReplaceChild("left_crown_side", CubeListBuilder.create()
                .texOffs(45, 125).mirror(true).addBox(-2.1f, -6.2f, -0.4f, 4.2f, 6.2f, 0.8f), PartPose.offsetAndRotation(5, -1.4f, 0.4f, 0, -1.5f, 0));
        PartDefinition rightCrownWing = crown.addOrReplaceChild("right_crown_wing", CubeListBuilder.create()
                .texOffs(37, 85).mirror(false).addBox(-0.4f, -7.4f, -0.6f, 0.8f, 7.4f, 5.4f), PartPose.offsetAndRotation(-4.8f, -3.4f, 2, 0.15f, 0.2f, -0.5f));
        PartDefinition leftCrownWing = crown.addOrReplaceChild("left_crown_wing", CubeListBuilder.create()
                .texOffs(37, 85).mirror(true).addBox(-0.4f, -7.4f, -0.6f, 0.8f, 7.4f, 5.4f), PartPose.offsetAndRotation(4.8f, -3.4f, 2, 0.15f, -0.2f, 0.5f));
        PartDefinition rightCrownTie = crown.addOrReplaceChild("right_crown_tie", CubeListBuilder.create()
                .texOffs(183, 135).mirror(false).addBox(-0.7f, 0, -0.2f, 1.4f, 5, 0.4f), PartPose.offsetAndRotation(-4.6f, -0.6f, 3.6f, 0.3f, 0, 0.3f));
        PartDefinition leftCrownTie = crown.addOrReplaceChild("left_crown_tie", CubeListBuilder.create()
                .texOffs(183, 135).mirror(true).addBox(-0.7f, 0, -0.2f, 1.4f, 5, 0.4f), PartPose.offsetAndRotation(4.6f, -0.6f, 3.6f, 0.3f, 0, -0.3f));
        PartDefinition rightCrownTie2 = rightCrownTie.addOrReplaceChild("right_crown_tie_2", CubeListBuilder.create()
                .texOffs(183, 135).mirror(false).addBox(-0.7f, 0, -0.2f, 1.4f, 5, 0.4f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.1f, 0, 0));
        PartDefinition leftCrownTie2 = leftCrownTie.addOrReplaceChild("left_crown_tie_2", CubeListBuilder.create()
                .texOffs(183, 135).mirror(true).addBox(-0.7f, 0, -0.2f, 1.4f, 5, 0.4f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.1f, 0, 0));
        PartDefinition rightCrownTie3 = rightCrownTie2.addOrReplaceChild("right_crown_tie_3", CubeListBuilder.create()
                .texOffs(183, 135).mirror(false).addBox(-0.85f, 0, -0.2f, 1.7f, 4.4f, 0.4f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.14f, 0, 0));
        PartDefinition leftCrownTie3 = leftCrownTie2.addOrReplaceChild("left_crown_tie_3", CubeListBuilder.create()
                .texOffs(183, 135).mirror(true).addBox(-0.85f, 0, -0.2f, 1.7f, 4.4f, 0.4f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.14f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(65, 69).mirror(false).addBox(-2.9f, -1.2f, -2.9f, 5.8f, 8.6f, 5.8f)
                .texOffs(223, 113).mirror(false).addBox(-3.3f, 4.6f, -3.3f, 6.6f, 1.4f, 6.6f), PartPose.offsetAndRotation(-10.2f, -14.2f, 0.3f, -0.1f, 0, 0.14f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(65, 69).mirror(true).addBox(-2.9f, -1.2f, -2.9f, 5.8f, 8.6f, 5.8f)
                .texOffs(223, 113).mirror(true).addBox(-3.3f, 4.6f, -3.3f, 6.6f, 1.4f, 6.6f), PartPose.offsetAndRotation(10.2f, -14.2f, 0.3f, -0.1f, 0, -0.14f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(197, 100).mirror(false).addBox(-4.8f, -1.6f, -4.3f, 7.4f, 1.8f, 8.6f)
                .texOffs(39, 100).mirror(false).addBox(-5.6f, 0, -4.8f, 8, 2, 9.6f)
                .texOffs(52, 85).mirror(false).addBox(-6.4f, 1.8f, -5.3f, 8.6f, 2.4f, 10.6f)
                .texOffs(17, 50).mirror(false).addBox(-7.4f, -2, -4.4f, 1, 8.8f, 8.8f)
                .texOffs(93, 85).mirror(false).addBox(-8.4f, -0.6f, -3.2f, 1.8f, 6.4f, 6.4f)
                .texOffs(58, 125).mirror(false).addBox(-8.1f, 5.6f, -2.6f, 1.6f, 1.4f, 5.2f), PartPose.offsetAndRotation(-0.4f, -0.9f, 0, 0, 0, 0.42f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(197, 100).mirror(true).addBox(-2.6f, -1.6f, -4.3f, 7.4f, 1.8f, 8.6f)
                .texOffs(39, 100).mirror(true).addBox(-2.4f, 0, -4.8f, 8, 2, 9.6f)
                .texOffs(52, 85).mirror(true).addBox(-2.2f, 1.8f, -5.3f, 8.6f, 2.4f, 10.6f)
                .texOffs(17, 50).mirror(true).addBox(6.4f, -2, -4.4f, 1, 8.8f, 8.8f)
                .texOffs(93, 85).mirror(true).addBox(6.6f, -0.6f, -3.2f, 1.8f, 6.4f, 6.4f)
                .texOffs(58, 125).mirror(true).addBox(6.5f, 5.6f, -2.6f, 1.6f, 1.4f, 5.2f), PartPose.offsetAndRotation(0.4f, -0.9f, 0, 0, 0, -0.42f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(76, 100).mirror(false).addBox(-2.5f, 0, -2.5f, 5, 7, 5)
                .texOffs(97, 100).mirror(false).addBox(-2.95f, 1, -2.95f, 5.9f, 5.2f, 5.9f)
                .texOffs(0, 113).mirror(false).addBox(-3.9f, -1.6f, -3.9f, 7.8f, 2.6f, 7.8f), PartPose.offsetAndRotation(0, 7.6f, 0, -0.95f, -0.35f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(76, 100).mirror(true).addBox(-2.5f, 0, -2.5f, 5, 7, 5)
                .texOffs(97, 100).mirror(true).addBox(-2.95f, 1, -2.95f, 5.9f, 5.2f, 5.9f)
                .texOffs(0, 113).mirror(true).addBox(-3.9f, -1.6f, -3.9f, 7.8f, 2.6f, 7.8f), PartPose.offsetAndRotation(0, 7.6f, 0, -0.95f, 0.35f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(75, 125).mirror(false).addBox(-2, 0, -2, 4, 4, 4), PartPose.offsetAndRotation(0, 7, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(75, 125).mirror(true).addBox(-2, 0, -2, 4, 4, 4), PartPose.offsetAndRotation(0, 7, 0, 0, 0, 0));
        PartDefinition pipa = body.addOrReplaceChild("pipa", CubeListBuilder.create()
                .texOffs(33, 113).mirror(false).addBox(-4.4f, 0, -1.4f, 8.8f, 7.5f, 2.4f)
                .texOffs(30, 143).mirror(false).addBox(-3.6f, 7.2f, -1.3f, 7.2f, 1.2f, 2.2f)
                .texOffs(190, 135).mirror(false).addBox(-3.8f, -2.6f, -1.3f, 7.6f, 2.8f, 2.2f)
                .texOffs(53, 143).mirror(false).addBox(-2.8f, -4.9f, -1.2f, 5.6f, 2.5f, 2)
                .texOffs(122, 100).mirror(false).addBox(-1.1f, -14.6f, -0.9f, 2.2f, 10, 1.6f)
                .texOffs(56, 150).mirror(false).addBox(-2.4f, 4.4f, -1.8f, 4.8f, 0.7f, 0.6f)
                .texOffs(69, 150).mirror(false).addBox(-1.4f, -6.6f, -1.1f, 2.8f, 0.5f, 0.4f)
                .texOffs(69, 150).mirror(false).addBox(-1.4f, -8.6f, -1.1f, 2.8f, 0.5f, 0.4f)
                .texOffs(69, 150).mirror(false).addBox(-1.4f, -10.6f, -1.1f, 2.8f, 0.5f, 0.4f)
                .texOffs(69, 150).mirror(false).addBox(-1.4f, -12.6f, -1.1f, 2.8f, 0.5f, 0.4f), PartPose.offsetAndRotation(-1.6f, -6.6f, -9.4f, -0.12f, 0.05f, 0.9f));
        PartDefinition pipaHead = pipa.addOrReplaceChild("pipa_head", CubeListBuilder.create()
                .texOffs(9, 135).mirror(false).addBox(-1.2f, -4.4f, -0.7f, 2.4f, 4.6f, 1.4f)
                .texOffs(78, 150).mirror(false).addBox(-2.4f, -1.6f, -0.3f, 4.8f, 0.6f, 0.6f)
                .texOffs(78, 150).mirror(false).addBox(-2.4f, -3.4f, -0.3f, 4.8f, 0.6f, 0.6f), PartPose.offsetAndRotation(0, -14.4f, 0, 0.55f, 0, 0));
        PartDefinition pipaTassel = pipaHead.addOrReplaceChild("pipa_tassel", CubeListBuilder.create()
                .texOffs(212, 143).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 3, 0.5f), PartPose.offsetAndRotation(0, -4, 0.4f, -0.55f, 0, 0));
        PartDefinition pipaTassel2 = pipaTassel.addOrReplaceChild("pipa_tassel_2", CubeListBuilder.create()
                .texOffs(213, 135).mirror(false).addBox(-0.7f, 0, -0.7f, 1.4f, 3.6f, 1.4f), PartPose.offsetAndRotation(0, 2.6f, 0, 0.05f, 0, 0));
        PartDefinition sword = rightHand.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(92, 125).mirror(false).addBox(-0.7f, -2.6f, -0.7f, 1.4f, 6, 1.4f)
                .texOffs(70, 143).mirror(false).addBox(-1.2f, 3.3f, -1.2f, 2.4f, 1.6f, 2.4f)
                .texOffs(83, 143).mirror(false).addBox(-3, -4, -1.3f, 6, 1.5f, 2.6f)
                .texOffs(217, 143).mirror(false).addBox(-1.8f, -5.4f, -0.9f, 3.6f, 1.5f, 1.8f)
                .texOffs(9, 0).mirror(false).addBox(-1.4f, -32, -0.35f, 2.8f, 26.8f, 0.7f), PartPose.offsetAndRotation(0, 2, 0, 0.9412f, 0.3237f, 0.2191f));
        PartDefinition swordTassel = sword.addOrReplaceChild("sword_tassel", CubeListBuilder.create()
                .texOffs(230, 143).mirror(false).addBox(-0.2f, 0, -0.2f, 0.4f, 2.4f, 0.4f), PartPose.offsetAndRotation(0, 4.8f, 0, 0, 0, 0));
        PartDefinition swordTassel2 = swordTassel.addOrReplaceChild("sword_tassel_2", CubeListBuilder.create()
                .texOffs(20, 135).mirror(false).addBox(-0.7f, 0, -0.7f, 1.4f, 4.2f, 1.4f), PartPose.offsetAndRotation(0, 2, 0, 0.05f, 0, 0));
        PartDefinition pearl = rightHand.addOrReplaceChild("pearl", CubeListBuilder.create()
                .texOffs(29, 135).mirror(false).addBox(-2.4f, -1.2f, -2.4f, 4.8f, 1.4f, 4.8f)
                .texOffs(101, 125).mirror(false).addBox(-2, -5.2f, -2, 4, 4, 4), PartPose.offsetAndRotation(0, -0.2f, -0.2f, 1.0612f, 0.3237f, 0.2191f));
        PartDefinition pearlFlame = pearl.addOrReplaceChild("pearl_flame", CubeListBuilder.create()
                .texOffs(0, 125).mirror(false).addBox(-3.6f, -5.4f, -0.25f, 7.2f, 7.2f, 0.5f)
                .texOffs(178, 50).mirror(false).addBox(-0.25f, -5.4f, -3.6f, 0.5f, 7.2f, 7.2f), PartPose.offsetAndRotation(0, -3.2f, 0, 0, 0.6f, 0));
        PartDefinition umb = rightHand.addOrReplaceChild("umb", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.6f, -40, -0.6f, 1.2f, 47, 1.2f)
                .texOffs(102, 143).mirror(false).addBox(-1, -41, -1, 2, 2.2f, 2)
                .texOffs(118, 125).mirror(false).addBox(-1.6f, -44.6f, -1.6f, 3.2f, 3.6f, 3.2f)
                .texOffs(111, 143).mirror(false).addBox(-0.9f, -47.2f, -0.9f, 1.8f, 2.6f, 1.8f)
                .texOffs(235, 143).mirror(false).addBox(-0.9f, 6.2f, -0.9f, 1.8f, 1.4f, 1.8f), PartPose.offsetAndRotation(0, 2, 0, 1.0612f, 0.3237f, 0.2191f));
        PartDefinition umbPanel0 = umb.addOrReplaceChild("umb_panel_0", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(-0, -40, -0.9f, -0.12f, 0, 0));
        PartDefinition umbTassel0 = umbPanel0.addOrReplaceChild("umb_tassel_0", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel1 = umb.addOrReplaceChild("umb_panel_1", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(-0.45f, -40, -0.7794f, -0.12f, 0.5236f, 0));
        PartDefinition umbTassel1 = umbPanel1.addOrReplaceChild("umb_tassel_1", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel2 = umb.addOrReplaceChild("umb_panel_2", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(-0.7794f, -40, -0.45f, -0.12f, 1.0472f, 0));
        PartDefinition umbTassel2 = umbPanel2.addOrReplaceChild("umb_tassel_2", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel3 = umb.addOrReplaceChild("umb_panel_3", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(-0.9f, -40, -0, -0.12f, 1.5708f, 0));
        PartDefinition umbTassel3 = umbPanel3.addOrReplaceChild("umb_tassel_3", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel4 = umb.addOrReplaceChild("umb_panel_4", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(-0.7794f, -40, 0.45f, -0.12f, 2.0944f, 0));
        PartDefinition umbTassel4 = umbPanel4.addOrReplaceChild("umb_tassel_4", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel5 = umb.addOrReplaceChild("umb_panel_5", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(-0.45f, -40, 0.7794f, -0.12f, 2.618f, 0));
        PartDefinition umbTassel5 = umbPanel5.addOrReplaceChild("umb_tassel_5", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel6 = umb.addOrReplaceChild("umb_panel_6", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(-0, -40, 0.9f, -0.12f, 3.1416f, 0));
        PartDefinition umbTassel6 = umbPanel6.addOrReplaceChild("umb_tassel_6", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel7 = umb.addOrReplaceChild("umb_panel_7", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(0.45f, -40, 0.7794f, -0.12f, 3.6652f, 0));
        PartDefinition umbTassel7 = umbPanel7.addOrReplaceChild("umb_tassel_7", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel8 = umb.addOrReplaceChild("umb_panel_8", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(0.7794f, -40, 0.45f, -0.12f, 4.1888f, 0));
        PartDefinition umbTassel8 = umbPanel8.addOrReplaceChild("umb_tassel_8", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel9 = umb.addOrReplaceChild("umb_panel_9", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(0.9f, -40, 0, -0.12f, 4.7124f, 0));
        PartDefinition umbTassel9 = umbPanel9.addOrReplaceChild("umb_tassel_9", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel10 = umb.addOrReplaceChild("umb_panel_10", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(0.7794f, -40, -0.45f, -0.12f, 5.236f, 0));
        PartDefinition umbTassel10 = umbPanel10.addOrReplaceChild("umb_tassel_10", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbPanel11 = umb.addOrReplaceChild("umb_panel_11", CubeListBuilder.create()
                .texOffs(67, 50).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 16, 0.6f), PartPose.offsetAndRotation(0.45f, -40, -0.7794f, -0.12f, 5.7596f, 0));
        PartDefinition umbTassel11 = umbPanel11.addOrReplaceChild("umb_tassel_11", CubeListBuilder.create()
                .texOffs(120, 143).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(244, 143).mirror(false).addBox(-0.8f, 3.2f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(125, 143).mirror(false).addBox(-0.6f, 4.6f, -0.6f, 1.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(0, 15.6f, -0.4f, 0.12f, 0, 0));
        PartDefinition umbCap0 = umb.addOrReplaceChild("umb_cap_0", CubeListBuilder.create()
                .texOffs(222, 135).mirror(false).addBox(-1.6f, 0, -0.3f, 3.2f, 4.2f, 0.6f), PartPose.offsetAndRotation(-0.3f, -42.6f, -0.5196f, -1, 0.5236f, 0));
        PartDefinition umbCap1 = umb.addOrReplaceChild("umb_cap_1", CubeListBuilder.create()
                .texOffs(222, 135).mirror(false).addBox(-1.6f, 0, -0.3f, 3.2f, 4.2f, 0.6f), PartPose.offsetAndRotation(-0.6f, -42.6f, -0, -1, 1.5708f, 0));
        PartDefinition umbCap2 = umb.addOrReplaceChild("umb_cap_2", CubeListBuilder.create()
                .texOffs(222, 135).mirror(false).addBox(-1.6f, 0, -0.3f, 3.2f, 4.2f, 0.6f), PartPose.offsetAndRotation(-0.3f, -42.6f, 0.5196f, -1, 2.618f, 0));
        PartDefinition umbCap3 = umb.addOrReplaceChild("umb_cap_3", CubeListBuilder.create()
                .texOffs(222, 135).mirror(false).addBox(-1.6f, 0, -0.3f, 3.2f, 4.2f, 0.6f), PartPose.offsetAndRotation(0.3f, -42.6f, 0.5196f, -1, 3.6652f, 0));
        PartDefinition umbCap4 = umb.addOrReplaceChild("umb_cap_4", CubeListBuilder.create()
                .texOffs(222, 135).mirror(false).addBox(-1.6f, 0, -0.3f, 3.2f, 4.2f, 0.6f), PartPose.offsetAndRotation(0.6f, -42.6f, 0, -1, 4.7124f, 0));
        PartDefinition umbCap5 = umb.addOrReplaceChild("umb_cap_5", CubeListBuilder.create()
                .texOffs(222, 135).mirror(false).addBox(-1.6f, 0, -0.3f, 3.2f, 4.2f, 0.6f), PartPose.offsetAndRotation(0.3f, -42.6f, -0.5196f, -1, 5.7596f, 0));
        PartDefinition coil = leftForearm.addOrReplaceChild("coil", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition coil0 = coil.addOrReplaceChild("coil_0", CubeListBuilder.create()
                .texOffs(233, 135).mirror(false).addBox(-0.6f, -0.6f, -0.2f, 1.2f, 1.2f, 3.2151f), PartPose.offsetAndRotation(3.2237f, 0.2f, 1.363f, -0.1638f, -0.7927f, 0));
        PartDefinition coil1 = coil.addOrReplaceChild("coil_1", CubeListBuilder.create()
                .texOffs(233, 135).mirror(false).addBox(-0.6423f, -0.6423f, -0.2f, 1.2846f, 1.2846f, 3.2151f), PartPose.offsetAndRotation(1.3157f, 0.6429f, 3.2433f, -0.1638f, -1.5781f, 0));
        PartDefinition coil2 = coil.addOrReplaceChild("coil_2", CubeListBuilder.create()
                .texOffs(233, 135).mirror(false).addBox(-0.6846f, -0.6846f, -0.2f, 1.3692f, 1.3692f, 3.2151f), PartPose.offsetAndRotation(-1.363f, 1.0857f, 3.2237f, -0.1638f, -2.3635f, 0));
        PartDefinition coil3 = coil.addOrReplaceChild("coil_3", CubeListBuilder.create()
                .texOffs(0, 143).mirror(false).addBox(-0.7269f, -0.7269f, -0.2f, 1.4538f, 1.4538f, 3.2151f), PartPose.offsetAndRotation(-3.2433f, 1.5286f, 1.3157f, -0.1638f, 3.1343f, 0));
        PartDefinition coil4 = coil.addOrReplaceChild("coil_4", CubeListBuilder.create()
                .texOffs(0, 143).mirror(false).addBox(-0.7692f, -0.7692f, -0.2f, 1.5385f, 1.5385f, 3.2151f), PartPose.offsetAndRotation(-3.2237f, 1.9714f, -1.363f, -0.1638f, 2.3489f, 0));
        PartDefinition coil5 = coil.addOrReplaceChild("coil_5", CubeListBuilder.create()
                .texOffs(0, 143).mirror(false).addBox(-0.8115f, -0.8115f, -0.2f, 1.6231f, 1.6231f, 3.2151f), PartPose.offsetAndRotation(-1.3157f, 2.4143f, -3.2433f, -0.1638f, 1.5635f, 0));
        PartDefinition coil6 = coil.addOrReplaceChild("coil_6", CubeListBuilder.create()
                .texOffs(0, 143).mirror(false).addBox(-0.8538f, -0.8538f, -0.2f, 1.7077f, 1.7077f, 3.2151f), PartPose.offsetAndRotation(1.363f, 2.8571f, -3.2237f, -0.1638f, 0.7781f, 0));
        PartDefinition coil7 = coil.addOrReplaceChild("coil_7", CubeListBuilder.create()
                .texOffs(0, 143).mirror(false).addBox(-0.8962f, -0.8962f, -0.2f, 1.7923f, 1.7923f, 3.2151f), PartPose.offsetAndRotation(3.2433f, 3.3f, -1.3157f, -0.1638f, -0.0073f, 0));
        PartDefinition coil8 = coil.addOrReplaceChild("coil_8", CubeListBuilder.create()
                .texOffs(0, 143).mirror(false).addBox(-0.9385f, -0.9385f, -0.2f, 1.8769f, 1.8769f, 3.2151f), PartPose.offsetAndRotation(3.2237f, 3.7429f, 1.363f, -0.1638f, -0.7927f, 0));
        PartDefinition coil9 = coil.addOrReplaceChild("coil_9", CubeListBuilder.create()
                .texOffs(0, 143).mirror(false).addBox(-0.9808f, -0.9808f, -0.2f, 1.9615f, 1.9615f, 3.2151f), PartPose.offsetAndRotation(1.3157f, 4.1857f, 3.2433f, -0.1638f, -1.5781f, 0));
        PartDefinition coil10 = coil.addOrReplaceChild("coil_10", CubeListBuilder.create()
                .texOffs(50, 135).mirror(false).addBox(-1.0231f, -1.0231f, -0.2f, 2.0462f, 2.0462f, 3.2151f), PartPose.offsetAndRotation(-1.363f, 4.6286f, 3.2237f, -0.1638f, -2.3635f, 0));
        PartDefinition coil11 = coil.addOrReplaceChild("coil_11", CubeListBuilder.create()
                .texOffs(50, 135).mirror(false).addBox(-1.0654f, -1.0654f, -0.2f, 2.1308f, 2.1308f, 3.2151f), PartPose.offsetAndRotation(-3.2433f, 5.0714f, 1.3157f, -0.1638f, 3.1343f, 0));
        PartDefinition coil12 = coil.addOrReplaceChild("coil_12", CubeListBuilder.create()
                .texOffs(50, 135).mirror(false).addBox(-1.1077f, -1.1077f, -0.2f, 2.2154f, 2.2154f, 3.2151f), PartPose.offsetAndRotation(-3.2237f, 5.5143f, -1.363f, -0.1638f, 2.3489f, 0));
        PartDefinition coil13 = coil.addOrReplaceChild("coil_13", CubeListBuilder.create()
                .texOffs(50, 135).mirror(false).addBox(-1.15f, -1.15f, -0.2f, 2.3f, 2.3f, 3.2151f), PartPose.offsetAndRotation(-1.3157f, 5.9571f, -3.2433f, -0.1638f, 1.5635f, 0));
        PartDefinition serpent = leftForearm.addOrReplaceChild("serpent", CubeListBuilder.create(), PartPose.offsetAndRotation(1.363f, 6.4f, -3.2237f, 1.0612f, -0.3237f, -0.2191f));
        PartDefinition serpentNeck0 = serpent.addOrReplaceChild("serpent_neck_0", CubeListBuilder.create()
                .texOffs(65, 135).mirror(false).addBox(-1.2f, -1.2f, -0.3f, 2.4f, 2.4f, 3.8802f), PartPose.offsetAndRotation(0, 0, 0, 1.3492f, 0.9828f, 0));
        PartDefinition serpentNeck1 = serpent.addOrReplaceChild("serpent_neck_1", CubeListBuilder.create()
                .texOffs(65, 135).mirror(false).addBox(-1.15f, -1.15f, -0.3f, 2.3f, 2.3f, 3.9764f), PartPose.offsetAndRotation(0.6f, -3.2f, 0.4f, 1.2461f, 2.7611f, 0));
        PartDefinition serpentNeck2 = serpent.addOrReplaceChild("serpent_neck_2", CubeListBuilder.create()
                .texOffs(135, 125).mirror(false).addBox(-1.1f, -1.1f, -0.3f, 2.2f, 2.2f, 4.0424f), PartPose.offsetAndRotation(1, -6.4f, -0.6f, 0.95f, -3.0916f, 0));
        PartDefinition serpentNeck3 = serpent.addOrReplaceChild("serpent_neck_3", CubeListBuilder.create()
                .texOffs(65, 135).mirror(false).addBox(-1.05f, -1.05f, -0.3f, 2.1f, 2.1f, 3.3946f), PartPose.offsetAndRotation(0.9f, -9.2f, -2.6f, 0.5247f, -3.0172f, 0));
        PartDefinition serpentHead = serpent.addOrReplaceChild("serpent_head", CubeListBuilder.create()
                .texOffs(152, 125).mirror(false).addBox(-1.7f, -1.4f, -4.4f, 3.4f, 2.6f, 4.6f)
                .texOffs(134, 143).mirror(false).addBox(-1.3f, -0.8f, -7.2f, 2.6f, 1.6f, 3)
                .texOffs(0, 150).mirror(false).addBox(-1.9f, -2, -3.6f, 0.8f, 1.2f, 1.6f)
                .texOffs(0, 150).mirror(false).addBox(1.1f, -2, -3.6f, 0.8f, 1.2f, 1.6f), PartPose.offsetAndRotation(0.6f, -10.6f, -5, 0.15f, 0, 0));
        PartDefinition rightSerpentHorn = serpentHead.addOrReplaceChild("right_serpent_horn", CubeListBuilder.create()
                .texOffs(147, 143).mirror(false).addBox(-0.4f, -0.4f, 0, 0.8f, 0.8f, 3.6f), PartPose.offsetAndRotation(-1, -1.6f, -1, -0.9f, 0, -0.35f));
        PartDefinition rightSerpentWhisker = serpentHead.addOrReplaceChild("right_serpent_whisker", CubeListBuilder.create()
                .texOffs(158, 143).mirror(false).addBox(-0.2f, -0.2f, 0, 0.4f, 0.4f, 4), PartPose.offsetAndRotation(-1.2f, 0.2f, -6.4f, 0.2f, 0.9f, 0.3f));
        PartDefinition serpentJaw = serpentHead.addOrReplaceChild("serpent_jaw", CubeListBuilder.create()
                .texOffs(171, 125).mirror(false).addBox(-1.4f, 0, -6.2f, 2.8f, 0.9f, 6.2f), PartPose.offsetAndRotation(0, 1.1f, -0.8f, 0.12f, 0, 0));
        PartDefinition serpentTongue = serpentJaw.addOrReplaceChild("serpent_tongue", CubeListBuilder.create()
                .texOffs(7, 150).mirror(false).addBox(-0.4f, -0.2f, -2.6f, 0.8f, 0.3f, 2.6f), PartPose.offsetAndRotation(0, 0, -6, 0.25f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED kings: end
    }

    /**
     * A king's pose: his attribute held before him; walking, his tassets and ribbons swing; striking, his
     * right arm brought down. Winding up: the East raises his pipa and strums, the South lifts his sword
     * over his head for the circling cut, the West's serpent rears to strike and gapes, the North's
     * umbrella opens over him.
     */
    public static final class KingModel<T extends HeavenlyKingEntity> extends EntityModel<T> {
        private final ModelPart root, waist, body, head, rightArm, leftArm, rightForearm, leftForearm, rightLeg, leftLeg,
                rightShin, leftShin, apron, rightTasset, leftTasset, rightSideTasset, leftSideTasset, backTasset, cape, capeLower,
                beard, pipa, sword, pearl, pearlFlame, coil, serpent, serpentHead, serpentJaw, umb;
        private final List<ModelPart> arc, panels, tassels;
        private final List<List<ModelPart>> streamers = new ArrayList<>();
        private int quarter;
        private float up;

        public KingModel(ModelPart root) {
            this.root = root;
            waist = root.getChild("waist");
            body = waist.getChild("body");
            head = body.getChild("head");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            apron = waist.getChild("apron");
            rightTasset = waist.getChild("right_tasset");
            leftTasset = waist.getChild("left_tasset");
            rightSideTasset = waist.getChild("right_side_tasset");
            leftSideTasset = waist.getChild("left_side_tasset");
            backTasset = waist.getChild("back_tasset");
            cape = body.getChild("cape");
            capeLower = cape.getChild("cape_lower");
            beard = head.getChild("beard");
            pipa = body.getChild("pipa");
            ModelPart rightHand = rightForearm.getChild("right_hand");
            sword = rightHand.getChild("sword");
            pearl = rightHand.getChild("pearl");
            pearlFlame = pearl.getChild("pearl_flame");
            coil = leftForearm.getChild("coil");
            serpent = leftForearm.getChild("serpent");
            serpentHead = serpent.getChild("serpent_head");
            serpentJaw = serpentHead.getChild("serpent_jaw");
            umb = rightHand.getChild("umb");
            arc = numbered(body, "ribbon_arc");
            panels = numbered(umb, "umb_panel");
            tassels = new ArrayList<>();
            for (int k = 0; k < panels.size(); k++) tassels.add(panels.get(k).getChild("umb_tassel_" + k));
            streamers.add(chain(body, "right_streamer"));
            streamers.add(chain(body, "left_streamer"));
            streamers.add(chain(waist, "right_sash"));
            streamers.add(chain(waist, "left_sash"));
            ModelPart crown = head.getChild("crown");
            streamers.add(chain(crown, "right_crown_tie"));
            streamers.add(chain(crown, "left_crown_tie"));
        }

        @Override
        public void prepareMobModel(T e, float swing, float amount, float partial) {
            quarter = e.quarter();
            int wind = switch (quarter) {
                case 0 -> HeavenlyKingEntity.Chiguo.WIND_UP;
                case 1 -> HeavenlyKingEntity.Zengzhang.WIND_UP;
                case 2 -> HeavenlyKingEntity.Guangmu.WIND_UP;
                default -> HeavenlyKingEntity.Duowen.WIND_UP;
            };
            up = windUp(e, wind, partial);
            pipa.visible = quarter == 0;
            sword.visible = quarter == 1;
            pearl.visible = quarter == 2;
            coil.visible = serpent.visible = quarter == 2;
            umb.visible = quarter == 3;
            beard.visible = quarter != 0;
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float walk = swing * 0.55f, step = Mth.cos(walk) * amount;
            float breathe = Mth.sin(age * 0.06f);
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            // a heavy tread: the legs swing, the knee bends as the leg comes through, the body rises and sways
            turn(rightLeg, step * 0.55f, 0, 0);
            turn(leftLeg, -step * 0.55f, 0, 0);
            turn(rightShin, Math.max(0, Mth.sin(walk)) * 0.6f * amount, 0, 0);
            turn(leftShin, Math.max(0, -Mth.sin(walk)) * 0.6f * amount, 0, 0);
            move(waist, 0, -Math.abs(Mth.cos(walk)) * 0.8f * amount + breathe * 0.15f, 0);
            turn(body, breathe * 0.015f, step * 0.06f, 0);
            // the plates over his thighs lift as each leg comes forward
            turn(rightTasset, Math.min(0, rightLeg.xRot) * 0.7f, 0, 0);
            turn(leftTasset, Math.min(0, leftLeg.xRot) * 0.7f, 0, 0);
            turn(rightSideTasset, 0, 0, Math.abs(step) * 0.08f);
            turn(leftSideTasset, 0, 0, -Math.abs(step) * 0.08f);
            turn(apron, Math.min(rightLeg.xRot, leftLeg.xRot) * 0.5f + breathe * 0.015f, 0, 0);
            turn(backTasset, Math.max(rightLeg.xRot, leftLeg.xRot) * 0.5f, 0, 0);
            turn(cape, amount * 0.3f + Mth.sin(age * 0.07f) * 0.03f, 0, 0);
            turn(capeLower, amount * 0.2f + Mth.sin(age * 0.07f + 0.8f) * 0.04f, 0, 0);
            // the arms swing a little with his tread; his attribute held before him
            float sway = -step * 0.18f;
            float rx = sway, rz = 0, lx = -sway, lz = 0, rfx = 0, lfx = 0, rfz = 0;
            switch (quarter) {
                case 0 -> {
                    // the pipa across him: the left hand high on its neck, the right at its strings
                    rx = 0.1f + sway * 0.3f;
                    rz = 0.05f;
                    rfx = -0.35f;
                    lx = -0.55f;
                    lz = -0.2f;
                    lfx = -0.55f;
                }
                case 2 -> rfx = -0.45f;            // the pearl held up
                default -> { }
            }
            // a blow: the right arm brought down
            rx -= strike * 1.3f;
            rfx += strike * 0.4f;
            float strum = 0;
            if (up > 0) {
                switch (quarter) {
                    case 0 -> {                    // the pipa raised, strummed hard
                        lx -= 0.6f * up;
                        lfx -= 0.2f * up;
                        rx -= 0.5f * up;
                        strum = Mth.sin(age * 2.2f) * 0.3f * up;
                    }
                    case 1 -> {                    // the sword lifted over his head for the circling cut
                        rx -= 2.3f * up;
                        rz += 0.25f * up;
                        rfx += 0.7f * up;
                        lx -= 0.4f * up;
                        lz -= 0.35f * up;
                    }
                    case 2 -> {                    // the serpent's arm thrust out, the pearl raised
                        lx -= 0.75f * up;
                        lz -= 0.15f * up;
                        rx -= 0.5f * up;
                    }
                    default -> {                   // the umbrella raised as it opens
                        rx -= 0.45f * up;
                        rz += 0.12f * up;
                    }
                }
            }
            turn(rightArm, rx, 0, rz);
            turn(leftArm, lx, 0, lz);
            turn(rightForearm, rfx, 0, rfz + strum);
            turn(leftForearm, lfx, 0, 0);
            // the East's pipa rises to his chest as he winds up
            move(pipa, 0, -3.5f * up, -1.5f * up);
            turn(pipa, -0.25f * up, 0, -0.35f * up);
            // the sword laid level over his head, its point to his left, for the circling cut
            turn(sword, up * 1.55f, 0, -up * 1.35f);
            turn(umb, up * 0.45f, 0, -up * 0.12f);
            // the pearl's flames turning
            turn(pearlFlame, 0, age * 0.05f, 0);
            pearl.xScale = pearl.yScale = pearl.zScale = 1 + up * 0.15f;
            // the serpent sways, rears back and gapes in the wind-up
            turn(serpent, Mth.sin(age * 0.07f) * 0.08f - up * 0.55f, Mth.sin(age * 0.05f) * 0.15f, 0);
            serpent.xScale = serpent.yScale = serpent.zScale = 1.35f + up * 0.2f;
            turn(serpentHead, up * 0.6f, 0, 0);
            turn(serpentJaw, 0.1f + Math.max(0, Mth.sin(age * 0.1f)) * 0.15f + up * 0.75f, 0, 0);
            // the umbrella furled, opening in the wind-up: its gores spread and broaden, its tassels hanging
            for (int k = 0; k < panels.size(); k++) {
                ModelPart p = panels.get(k);
                float open = up;
                turn(p, -1.18f * open, 0, 0);
                float w = 0.18f + 0.82f * open;
                p.xScale = w;
                ModelPart t = tassels.get(k);
                t.xRot = -p.xRot + Mth.sin(age * 0.1f + k) * 0.05f;
                t.xScale = 1 / w;
            }
            // the ribbon's loop rippling behind his head; its streamers, the sashes and the crown's ties fluttering
            for (int k = 0; k < arc.size(); k++) move(arc.get(k), 0, 0, Mth.sin(age * 0.08f + k * 0.55f) * 0.35f + amount * 0.6f);
            float gust = 1 + up * 1.5f;
            for (int k = 0; k < streamers.size(); k++) {
                flutter(streamers.get(k), age, amount, k * 1.3f, k % 2 == 0 ? 1 : -1, gust);
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    public static final class KingRenderer<T extends HeavenlyKingEntity> extends GreatRenderer<T, KingModel<T>> {
        KingRenderer(EntityRendererProvider.Context ctx, String name) {
            super(ctx, new KingModel<>(ctx.bakeLayer(KING_LAYER)), 0.9f, KING_SIZE, "heaven_king_" + name);
        }
    }

    // ── Li Jing ─────────────────────────────────────────────────────────

    /**
     * Li Jing, the Pagoda-Bearing Heavenly King (256x256; heaven_great_li.py): a commander in gold
     * mountain-pattern armour under a phoenix-winged gold helmet, a long black beard, a red silk sash and a
     * great red cape; the golden pagoda held up on his left palm, light in all its windows; a sword, point
     * down, in his right.
     */
    public static LayerDefinition liJingLayer() {
        // GENERATED li_jing: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(58, 91).mirror(false).addBox(-6, -2, -4, 12, 4, 8)
                .texOffs(0, 76).mirror(false).addBox(-7, -4.2f, -5, 14, 3.4f, 10)
                .texOffs(224, 117).mirror(false).addBox(-2.6f, -5.2f, -5.8f, 5.2f, 4.4f, 1.2f), PartPose.offsetAndRotation(0, 5, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(51, 57).mirror(false).addBox(-3, 0, -0.5f, 6, 15.5f, 0.8f), PartPose.offsetAndRotation(0, 0.4f, -5.2f, -0.05f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(86, 76).mirror(false).addBox(-3.4f, 0, -0.6f, 6.8f, 10.5f, 1.2f), PartPose.offsetAndRotation(-3.8f, 0, -4.4f, -0.12f, 0.15f, 0.1f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(86, 76).mirror(true).addBox(-3.4f, 0, -0.6f, 6.8f, 10.5f, 1.2f), PartPose.offsetAndRotation(3.8f, 0, -4.4f, -0.12f, -0.15f, -0.1f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(172, 0).mirror(false).addBox(-1.1f, 0, -4.2f, 1.1f, 10, 8.4f), PartPose.offsetAndRotation(-6.6f, 0, 0, 0, 0, 0.2f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(172, 0).mirror(true).addBox(0, 0, -4.2f, 1.1f, 10, 8.4f), PartPose.offsetAndRotation(6.6f, 0, 0, 0, 0, -0.2f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(105, 76).mirror(false).addBox(-6.2f, 0, -0.6f, 12.4f, 10.5f, 1.2f), PartPose.offsetAndRotation(0, 0, 4.6f, 0.12f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(135, 57).mirror(false).addBox(-2.8f, 0, -2.8f, 5.6f, 8.4f, 5.6f), PartPose.offsetAndRotation(-3.3f, 7, 0.3f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(135, 57).mirror(true).addBox(-2.8f, 0, -2.8f, 5.6f, 8.4f, 5.6f), PartPose.offsetAndRotation(3.3f, 7, 0.3f, 0, 0, 0));
        PartDefinition rightRobe = rightLeg.addOrReplaceChild("right_robe", CubeListBuilder.create()
                .texOffs(136, 76).mirror(false).addBox(-3.1f, 0, -0.5f, 6.2f, 12, 0.6f), PartPose.offsetAndRotation(0, -0.6f, -3, -0.05f, 0, 0.04f));
        PartDefinition leftRobe = leftLeg.addOrReplaceChild("left_robe", CubeListBuilder.create()
                .texOffs(136, 76).mirror(true).addBox(-3.1f, 0, -0.5f, 6.2f, 12, 0.6f), PartPose.offsetAndRotation(0, -0.6f, -3, -0.05f, 0, -0.04f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(159, 91).mirror(false).addBox(-2.4f, 0, -2.4f, 4.8f, 5.6f, 4.8f)
                .texOffs(29, 117).mirror(false).addBox(-2.8f, 0.4f, -3, 5.6f, 5, 3)
                .texOffs(180, 91).mirror(false).addBox(-2.8f, 5.2f, -3.2f, 5.6f, 3.4f, 6.2f)
                .texOffs(140, 135).mirror(false).addBox(-2.1f, 6.6f, -4.9f, 4.2f, 2, 1.8f)
                .texOffs(216, 135).mirror(false).addBox(-1.2f, 5.6f, -5.4f, 2.4f, 1.8f, 0.8f), PartPose.offsetAndRotation(0, 8.4f, 0, 0, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(159, 91).mirror(true).addBox(-2.4f, 0, -2.4f, 4.8f, 5.6f, 4.8f)
                .texOffs(29, 117).mirror(true).addBox(-2.8f, 0.4f, -3, 5.6f, 5, 3)
                .texOffs(180, 91).mirror(true).addBox(-2.8f, 5.2f, -3.2f, 5.6f, 3.4f, 6.2f)
                .texOffs(140, 135).mirror(true).addBox(-2.1f, 6.6f, -4.9f, 4.2f, 2, 1.8f)
                .texOffs(216, 135).mirror(true).addBox(-1.2f, 5.6f, -5.4f, 2.4f, 1.8f, 0.8f), PartPose.offsetAndRotation(0, 8.4f, 0, 0, 0, 0));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(118, 0).mirror(false).addBox(-6.5f, -14, -4.4f, 13, 14, 8.8f)
                .texOffs(0, 57).mirror(false).addBox(-7.1f, -14.4f, -5, 14.2f, 7.4f, 10)
                .texOffs(168, 127).mirror(false).addBox(-5.6f, -13.4f, -5.6f, 4.4f, 4.4f, 0.7f)
                .texOffs(168, 127).mirror(false).addBox(1.2f, -13.4f, -5.6f, 4.4f, 4.4f, 0.7f)
                .texOffs(153, 76).mirror(false).addBox(-6.8f, -4.6f, -4.9f, 13.6f, 2.4f, 9.8f)
                .texOffs(60, 135).mirror(false).addBox(-1.6f, -5.4f, -5.9f, 3.2f, 3, 1.2f)
                .texOffs(207, 91).mirror(false).addBox(-4.2f, -15.8f, -3.6f, 8.4f, 2.4f, 7.2f)
                .texOffs(202, 76).mirror(false).addBox(-7.8f, -14.8f, -5.4f, 15.6f, 1.2f, 10.8f)
                .texOffs(155, 135).mirror(false).addBox(-7.4f, -14.6f, -5.6f, 2.6f, 2.6f, 0.8f)
                .texOffs(155, 135).mirror(false).addBox(4.8f, -14.6f, -5.6f, 2.6f, 2.6f, 0.8f), PartPose.offsetAndRotation(0, -3, 0, 0, 0, 0));
        PartDefinition rightSashend = body.addOrReplaceChild("right_sashend", CubeListBuilder.create()
                .texOffs(181, 127).mirror(false).addBox(-0.8f, 0, -0.25f, 1.6f, 5, 0.5f), PartPose.offsetAndRotation(-0.8f, -3, -5.6f, -0.05f, 0, 0.12f));
        PartDefinition leftSashend = body.addOrReplaceChild("left_sashend", CubeListBuilder.create()
                .texOffs(181, 127).mirror(true).addBox(-0.8f, 0, -0.25f, 1.6f, 5, 0.5f), PartPose.offsetAndRotation(0.8f, -3, -5.6f, -0.05f, 0, -0.12f));
        PartDefinition rightSashend2 = rightSashend.addOrReplaceChild("right_sashend_2", CubeListBuilder.create()
                .texOffs(181, 127).mirror(false).addBox(-0.9f, 0, -0.25f, 1.8f, 5, 0.5f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.05f, 0, 0));
        PartDefinition leftSashend2 = leftSashend.addOrReplaceChild("left_sashend_2", CubeListBuilder.create()
                .texOffs(181, 127).mirror(true).addBox(-0.9f, 0, -0.25f, 1.8f, 5, 0.5f), PartPose.offsetAndRotation(0, 4.6f, 0, 0.05f, 0, 0));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(160, 57).mirror(false).addBox(-8.2f, 0, -0.4f, 16.4f, 14, 0.8f), PartPose.offsetAndRotation(0, -14.6f, 4.9f, 0.1f, 0, 0));
        PartDefinition capeLower = cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(0, 105).mirror(false).addBox(-8.8f, 0, -0.4f, 17.6f, 10, 0.8f), PartPose.offsetAndRotation(0, 13.8f, 0, 0.06f, 0, 0));
        PartDefinition capeHem = capeLower.addOrReplaceChild("cape_hem", CubeListBuilder.create()
                .texOffs(0, 127).mirror(false).addBox(-9.4f, 0, -0.4f, 18.8f, 6, 0.8f), PartPose.offsetAndRotation(0, 9.8f, 0, 0.05f, 0, 0));
        PartDefinition rightCapeWrap = cape.addOrReplaceChild("right_cape_wrap", CubeListBuilder.create()
                .texOffs(195, 0).mirror(false).addBox(-0.4f, 0, -6.5f, 0.8f, 12, 6.5f), PartPose.offsetAndRotation(-8, 0.4f, -0.2f, 0, 0.65f, 0.12f));
        PartDefinition leftCapeWrap = cape.addOrReplaceChild("left_cape_wrap", CubeListBuilder.create()
                .texOffs(195, 0).mirror(true).addBox(-0.4f, 0, -6.5f, 0.8f, 12, 6.5f), PartPose.offsetAndRotation(8, 0.4f, -0.2f, 0, -0.65f, -0.12f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(66, 57).mirror(false).addBox(-4, -9.4f, -3.9f, 8, 8.2f, 7.8f)
                .texOffs(126, 105).mirror(false).addBox(-3.6f, -1.6f, -3.6f, 7.2f, 2.2f, 6.8f)
                .texOffs(164, 135).mirror(false).addBox(-1, -6.4f, -4.7f, 2, 2.9f, 1)
                .texOffs(197, 57).mirror(false).addBox(-4.3f, -9.6f, -0.6f, 8.6f, 8.6f, 5.2f), PartPose.offsetAndRotation(0, -15.6f, -0.3f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(243, 135).mirror(false).addBox(-3.4f, -0.6f, -0.4f, 3.4f, 0.9f, 0.8f), PartPose.offsetAndRotation(-0.5f, -6.9f, -4.1f, 0, 0.1f, 0.16f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(243, 135).mirror(true).addBox(0, -0.6f, -0.4f, 3.4f, 0.9f, 0.8f), PartPose.offsetAndRotation(0.5f, -6.9f, -4.1f, 0, -0.1f, -0.16f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(41, 127).mirror(false).addBox(-0.8f, -1.6f, -1, 0.8f, 4.2f, 2), PartPose.offsetAndRotation(-4.1f, -5.8f, -0.2f, 0, 0, 0.06f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(41, 127).mirror(true).addBox(0, -1.6f, -1, 0.8f, 4.2f, 2), PartPose.offsetAndRotation(4.1f, -5.8f, -0.2f, 0, 0, -0.06f));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(0, 142).mirror(false).addBox(-3.8f, -0.5f, -0.5f, 3.8f, 1, 1), PartPose.offsetAndRotation(-0.4f, -3.1f, -4.6f, 0, 0.25f, -0.62f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(0, 142).mirror(true).addBox(0, -0.5f, -0.5f, 3.8f, 1, 1), PartPose.offsetAndRotation(0.4f, -3.1f, -4.6f, 0, -0.25f, 0.62f));
        PartDefinition beard = head.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(99, 91).mirror(false).addBox(-1.5f, 0, -1, 3, 10, 1.8f), PartPose.offsetAndRotation(0, -0.8f, -3.6f, 0.1f, 0, 0));
        PartDefinition beardTip = beard.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(48, 127).mirror(false).addBox(-1.1f, 0, -0.8f, 2.2f, 5, 1.6f), PartPose.offsetAndRotation(0, 9.6f, 0, -0.1f, 0, 0));
        PartDefinition rightBeard = head.addOrReplaceChild("right_beard", CubeListBuilder.create()
                .texOffs(110, 91).mirror(false).addBox(-1.1f, 0, -0.8f, 2.2f, 9.5f, 1.6f), PartPose.offsetAndRotation(-2.3f, -1.2f, -3.2f, 0.12f, 0.12f, 0.1f));
        PartDefinition leftBeard = head.addOrReplaceChild("left_beard", CubeListBuilder.create()
                .texOffs(110, 91).mirror(true).addBox(-1.1f, 0, -0.8f, 2.2f, 9.5f, 1.6f), PartPose.offsetAndRotation(2.3f, -1.2f, -3.2f, 0.12f, -0.12f, -0.1f));
        PartDefinition rightBeardOuter = head.addOrReplaceChild("right_beard_outer", CubeListBuilder.create()
                .texOffs(157, 105).mirror(false).addBox(-0.9f, 0, -0.7f, 1.8f, 7.5f, 1.4f), PartPose.offsetAndRotation(-3.5f, -2.6f, -2.2f, 0.05f, 0.25f, 0.14f));
        PartDefinition leftBeardOuter = head.addOrReplaceChild("left_beard_outer", CubeListBuilder.create()
                .texOffs(157, 105).mirror(true).addBox(-0.9f, 0, -0.7f, 1.8f, 7.5f, 1.4f), PartPose.offsetAndRotation(3.5f, -2.6f, -2.2f, 0.05f, -0.25f, -0.14f));
        PartDefinition helm = head.addOrReplaceChild("helm", CubeListBuilder.create()
                .texOffs(39, 105).mirror(false).addBox(-4.6f, -1.6f, -4.4f, 9.2f, 2, 8.8f)
                .texOffs(49, 76).mirror(false).addBox(-4.2f, -5.6f, -4.2f, 8.4f, 4.2f, 8.4f)
                .texOffs(48, 117).mirror(false).addBox(-3, -7.2f, -3, 6, 1.8f, 6)
                .texOffs(59, 127).mirror(false).addBox(-1.9f, -9.2f, -1.9f, 3.8f, 2.2f, 3.8f)
                .texOffs(188, 127).mirror(false).addBox(-0.7f, -12.6f, -0.7f, 1.4f, 4, 1.4f)
                .texOffs(171, 135).mirror(false).addBox(-4.5f, -0.6f, -5.4f, 9, 1.2f, 1.2f)
                .texOffs(73, 135).mirror(false).addBox(-1.4f, -4.6f, -4.9f, 2.8f, 3.2f, 0.8f), PartPose.offsetAndRotation(0, -9.2f, 0, 0, 0, 0));
        PartDefinition rightHelmWing = helm.addOrReplaceChild("right_helm_wing", CubeListBuilder.create()
                .texOffs(99, 57).mirror(false).addBox(-0.4f, -9.6f, -0.8f, 0.8f, 9.6f, 6.4f), PartPose.offsetAndRotation(-4.3f, -2.8f, 0.6f, 0.12f, 0.18f, -0.42f));
        PartDefinition leftHelmWing = helm.addOrReplaceChild("left_helm_wing", CubeListBuilder.create()
                .texOffs(99, 57).mirror(true).addBox(-0.4f, -9.6f, -0.8f, 0.8f, 9.6f, 6.4f), PartPose.offsetAndRotation(4.3f, -2.8f, 0.6f, 0.12f, -0.18f, 0.42f));
        PartDefinition rightHelmFlap = helm.addOrReplaceChild("right_helm_flap", CubeListBuilder.create()
                .texOffs(78, 105).mirror(false).addBox(-0.6f, 0, -2.2f, 0.6f, 4.2f, 5.6f), PartPose.offsetAndRotation(-4.5f, -0.4f, -0.4f, 0, 0, 0.14f));
        PartDefinition leftHelmFlap = helm.addOrReplaceChild("left_helm_flap", CubeListBuilder.create()
                .texOffs(78, 105).mirror(true).addBox(0, 0, -2.2f, 0.6f, 4.2f, 5.6f), PartPose.offsetAndRotation(4.5f, -0.4f, -0.4f, 0, 0, -0.14f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 91).mirror(false).addBox(-2.4f, -1, -2.4f, 4.8f, 7.6f, 4.8f)
                .texOffs(73, 117).mirror(false).addBox(-2.8f, 4, -2.8f, 5.6f, 1.2f, 5.6f), PartPose.offsetAndRotation(-8.6f, -12.8f, 0.2f, -0.08f, 0, 0.12f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 91).mirror(true).addBox(-2.4f, -1, -2.4f, 4.8f, 7.6f, 4.8f)
                .texOffs(73, 117).mirror(true).addBox(-2.8f, 4, -2.8f, 5.6f, 1.2f, 5.6f), PartPose.offsetAndRotation(8.6f, -12.8f, 0.2f, -0.2f, 0, -0.45f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(166, 105).mirror(false).addBox(-4.2f, -1.4f, -3.8f, 6.4f, 1.6f, 7.6f)
                .texOffs(93, 105).mirror(false).addBox(-4.9f, 0, -4.2f, 7, 1.8f, 8.4f)
                .texOffs(21, 91).mirror(false).addBox(-5.6f, 1.6f, -4.6f, 7.6f, 2.2f, 9.2f)
                .texOffs(116, 57).mirror(false).addBox(-6.5f, -1.6f, -3.8f, 0.9f, 7.6f, 7.6f)
                .texOffs(121, 91).mirror(false).addBox(-7.3f, -0.4f, -2.7f, 1.5f, 5.4f, 5.4f), PartPose.offsetAndRotation(-0.3f, -0.8f, 0, 0, 0, 0.4f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(166, 105).mirror(true).addBox(-2.2f, -1.4f, -3.8f, 6.4f, 1.6f, 7.6f)
                .texOffs(93, 105).mirror(true).addBox(-2.1f, 0, -4.2f, 7, 1.8f, 8.4f)
                .texOffs(21, 91).mirror(true).addBox(-2, 1.6f, -4.6f, 7.6f, 2.2f, 9.2f)
                .texOffs(116, 57).mirror(true).addBox(5.6f, -1.6f, -3.8f, 0.9f, 7.6f, 7.6f)
                .texOffs(121, 91).mirror(true).addBox(5.8f, -0.4f, -2.7f, 1.5f, 5.4f, 5.4f), PartPose.offsetAndRotation(0.3f, -0.8f, 0, 0, 0, -0.169f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(138, 91).mirror(false).addBox(-2.1f, 0, -2.1f, 4.2f, 6.2f, 4.2f)
                .texOffs(197, 105).mirror(false).addBox(-2.5f, 0.8f, -2.5f, 5, 4.6f, 5)
                .texOffs(218, 105).mirror(false).addBox(-3.2f, -1.2f, -3.2f, 6.4f, 2, 6.4f), PartPose.offsetAndRotation(0, 6.8f, 0, -0.55f, -0.25f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(138, 91).mirror(true).addBox(-2.1f, 0, -2.1f, 4.2f, 6.2f, 4.2f)
                .texOffs(197, 105).mirror(true).addBox(-2.5f, 0.8f, -2.5f, 5, 4.6f, 5)
                .texOffs(218, 105).mirror(true).addBox(-3.2f, -1.2f, -3.2f, 6.4f, 2, 6.4f), PartPose.offsetAndRotation(0, 6.8f, 0, -2.6f, -0.3f, 0.3f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(98, 117).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 3.6f, 3.6f), PartPose.offsetAndRotation(0, 6.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(115, 117).mirror(true).addBox(-1.9f, 0, -1.9f, 3.8f, 3.4f, 3.8f), PartPose.offsetAndRotation(0, 6.2f, 0, 0, 0, 0));
        PartDefinition sword = rightHand.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(132, 117).mirror(false).addBox(-0.6f, -2.4f, -0.6f, 1.2f, 5.4f, 1.2f)
                .texOffs(194, 135).mirror(false).addBox(-1, 2.8f, -1, 2, 1.4f, 2)
                .texOffs(82, 135).mirror(false).addBox(-2.6f, -3.6f, -1.1f, 5.2f, 1.3f, 2.2f)
                .texOffs(163, 0).mirror(false).addBox(-1.1f, -23.6f, -0.3f, 2.2f, 20.2f, 0.6f), PartPose.offsetAndRotation(0, 1.8f, 0, 3.1412f, 0.2835f, 0.0328f));
        PartDefinition pagoda = leftHand.addOrReplaceChild("pagoda", CubeListBuilder.create()
                .texOffs(0, 117).mirror(false).addBox(-3.4f, -1.4f, -3.4f, 6.8f, 1.4f, 6.8f)
                .texOffs(141, 117).mirror(false).addBox(-2.8f, -2.8f, -2.8f, 5.6f, 1.4f, 5.6f)
                .texOffs(76, 127).mirror(false).addBox(-2.3f, -4.55f, -2.3f, 4.6f, 1.75f, 4.6f)
                .texOffs(166, 117).mirror(false).addBox(-3.3f, -5.15f, -3.3f, 6.6f, 0.6f, 6.6f)
                .texOffs(11, 142).mirror(false).addBox(-3.55f, -5.65f, -3.55f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(-3.55f, -5.65f, 2.85f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(2.85f, -5.65f, -3.55f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(2.85f, -5.65f, 2.85f, 0.7f, 0.7f, 0.7f)
                .texOffs(97, 127).mirror(false).addBox(-2.09f, -6.84f, -2.09f, 4.18f, 1.69f, 4.18f)
                .texOffs(195, 117).mirror(false).addBox(-3.09f, -7.44f, -3.09f, 6.18f, 0.6f, 6.18f)
                .texOffs(11, 142).mirror(false).addBox(-3.34f, -7.94f, -3.34f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(-3.34f, -7.94f, 2.64f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(2.64f, -7.94f, -3.34f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(2.64f, -7.94f, 2.64f, 0.7f, 0.7f, 0.7f)
                .texOffs(197, 127).mirror(false).addBox(-1.88f, -9.07f, -1.88f, 3.76f, 1.63f, 3.76f)
                .texOffs(118, 127).mirror(false).addBox(-2.88f, -9.67f, -2.88f, 5.76f, 0.6f, 5.76f)
                .texOffs(11, 142).mirror(false).addBox(-3.13f, -10.17f, -3.13f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(-3.13f, -10.17f, 2.43f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(2.43f, -10.17f, -3.13f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(2.43f, -10.17f, 2.43f, 0.7f, 0.7f, 0.7f)
                .texOffs(214, 127).mirror(false).addBox(-1.67f, -11.24f, -1.67f, 3.34f, 1.57f, 3.34f)
                .texOffs(143, 127).mirror(false).addBox(-2.67f, -11.84f, -2.67f, 5.34f, 0.6f, 5.34f)
                .texOffs(11, 142).mirror(false).addBox(-2.92f, -12.34f, -2.92f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(-2.92f, -12.34f, 2.22f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(2.22f, -12.34f, -2.92f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(2.22f, -12.34f, 2.22f, 0.7f, 0.7f, 0.7f)
                .texOffs(101, 135).mirror(false).addBox(-1.46f, -13.35f, -1.46f, 2.92f, 1.51f, 2.92f)
                .texOffs(231, 127).mirror(false).addBox(-2.46f, -13.95f, -2.46f, 4.92f, 0.6f, 4.92f)
                .texOffs(11, 142).mirror(false).addBox(-2.71f, -14.45f, -2.71f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(-2.71f, -14.45f, 2.01f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(2.01f, -14.45f, -2.71f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(2.01f, -14.45f, 2.01f, 0.7f, 0.7f, 0.7f)
                .texOffs(114, 135).mirror(false).addBox(-1.25f, -15.4f, -1.25f, 2.5f, 1.45f, 2.5f)
                .texOffs(0, 135).mirror(false).addBox(-2.25f, -16, -2.25f, 4.5f, 0.6f, 4.5f)
                .texOffs(11, 142).mirror(false).addBox(-2.5f, -16.5f, -2.5f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(-2.5f, -16.5f, 1.8f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(1.8f, -16.5f, -2.5f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(1.8f, -16.5f, 1.8f, 0.7f, 0.7f, 0.7f)
                .texOffs(127, 135).mirror(false).addBox(-1.04f, -17.39f, -1.04f, 2.08f, 1.39f, 2.08f)
                .texOffs(21, 135).mirror(false).addBox(-2.04f, -17.99f, -2.04f, 4.08f, 0.6f, 4.08f)
                .texOffs(11, 142).mirror(false).addBox(-2.29f, -18.49f, -2.29f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(-2.29f, -18.49f, 1.59f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(1.59f, -18.49f, -2.29f, 0.7f, 0.7f, 0.7f)
                .texOffs(11, 142).mirror(false).addBox(1.59f, -18.49f, 1.59f, 0.7f, 0.7f, 0.7f)
                .texOffs(42, 135).mirror(false).addBox(-0.5f, -22.39f, -0.5f, 1, 4.4f, 1)
                .texOffs(203, 135).mirror(false).addBox(-1.2f, -19.39f, -1.2f, 2.4f, 0.6f, 2.4f)
                .texOffs(225, 135).mirror(false).addBox(-1, -20.59f, -1, 2, 0.6f, 2)
                .texOffs(234, 135).mirror(false).addBox(-0.8f, -21.69f, -0.8f, 1.6f, 0.5f, 1.6f)
                .texOffs(47, 135).mirror(false).addBox(-1.1f, -24.59f, -1.1f, 2.2f, 2.2f, 2.2f), PartPose.offsetAndRotation(0, 3.4f, 0, 2.8098f, -0.2545f, -0.0137f));
        PartDefinition pagodaRays = pagoda.addOrReplaceChild("pagoda_rays", CubeListBuilder.create()
                .texOffs(59, 0).mirror(false).addBox(-14, -14, 0, 28, 28, 0.01f)
                .texOffs(0, 0).mirror(false).addBox(0, -14, -14, 0.01f, 28, 28), PartPose.offsetAndRotation(0, -12, 0, 0, 0.4f, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED li_jing: end
    }

    /**
     * His pose: the pagoda held up beside his head, the sword down at his side; striking, the sword brought
     * round; winding up, the pagoda lifted high overhead, blazing, rays of light wheeling about it.
     */
    public static final class LiJingModel extends EntityModel<LiJingEntity> {
        private final ModelPart root, waist, body, head, rightArm, leftArm, rightForearm, leftForearm, rightLeg, leftLeg,
                rightShin, leftShin, apron, rightTasset, leftTasset, backTasset, cape, capeLower, capeHem, beard, pagoda, rays;
        private final List<List<ModelPart>> streamers = new ArrayList<>();
        private float up;

        public LiJingModel(ModelPart root) {
            this.root = root;
            waist = root.getChild("waist");
            body = waist.getChild("body");
            head = body.getChild("head");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            apron = waist.getChild("apron");
            rightTasset = waist.getChild("right_tasset");
            leftTasset = waist.getChild("left_tasset");
            backTasset = waist.getChild("back_tasset");
            cape = body.getChild("cape");
            capeLower = cape.getChild("cape_lower");
            capeHem = capeLower.getChild("cape_hem");
            beard = head.getChild("beard");
            pagoda = leftForearm.getChild("left_hand").getChild("pagoda");
            rays = pagoda.getChild("pagoda_rays");
            streamers.add(chain(body, "right_sashend"));
            streamers.add(chain(body, "left_sashend"));
        }

        @Override
        public void prepareMobModel(LiJingEntity e, float swing, float amount, float partial) {
            up = windUp(e, LiJingEntity.WIND_UP, partial);
            rays.visible = up > 0;
        }

        @Override
        public void setupAnim(LiJingEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float walk = swing * 0.6f, step = Mth.cos(walk) * amount;
            float breathe = Mth.sin(age * 0.06f);
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD - up * 0.2f;
            turn(rightLeg, step * 0.6f, 0, 0);
            turn(leftLeg, -step * 0.6f, 0, 0);
            turn(rightShin, Math.max(0, Mth.sin(walk)) * 0.6f * amount, 0, 0);
            turn(leftShin, Math.max(0, -Mth.sin(walk)) * 0.6f * amount, 0, 0);
            move(waist, 0, -Math.abs(Mth.cos(walk)) * 0.7f * amount + breathe * 0.12f, 0);
            turn(body, breathe * 0.015f, step * 0.05f, 0);
            turn(rightTasset, Math.min(0, rightLeg.xRot) * 0.7f, 0, 0);
            turn(leftTasset, Math.min(0, leftLeg.xRot) * 0.7f, 0, 0);
            turn(apron, Math.min(rightLeg.xRot, leftLeg.xRot) * 0.5f, 0, 0);
            turn(backTasset, Math.max(rightLeg.xRot, leftLeg.xRot) * 0.5f, 0, 0);
            // the cape billowing behind him as he strides, more in his wind-up
            turn(cape, amount * 0.45f + Mth.sin(age * 0.07f) * 0.03f + up * 0.15f, 0, 0);
            turn(capeLower, amount * 0.3f + Mth.sin(age * 0.07f + 0.8f) * 0.05f + up * 0.1f, 0, 0);
            turn(capeHem, amount * 0.2f + Mth.sin(age * 0.09f + 1.6f) * 0.07f + up * 0.1f, 0, 0);
            turn(beard, amount * 0.15f + Mth.sin(age * 0.05f) * 0.03f, 0, 0);
            // the sword at his side swings round in a blow; the pagoda lifted high in the wind-up
            turn(rightArm, -step * 0.25f - strike * 1.5f, 0, strike * 0.2f);
            turn(rightForearm, strike * -0.4f, 0, 0);
            turn(leftArm, -up * 0.35f, 0, -up * 0.75f);
            turn(leftForearm, up * 0.35f, 0, 0);
            turn(pagoda, -up * 0.3f, 0, -up * 0.75f);
            float grow = 1 + up * 0.3f;
            pagoda.xScale = pagoda.yScale = pagoda.zScale = grow;
            float r = up * (1.1f + Mth.sin(age * 0.4f) * 0.08f);
            rays.xScale = rays.yScale = rays.zScale = Math.max(0.01f, r);
            rays.yRot = rays.getInitialPose().yRot + age * 0.04f;
            for (int k = 0; k < streamers.size(); k++) flutter(streamers.get(k), age, amount, k * 1.7f, k % 2 == 0 ? 1 : -1, 1 + up);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    static final class LiJingRenderer extends GreatRenderer<LiJingEntity, LiJingModel> {
        LiJingRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new LiJingModel(ctx.bakeLayer(LI_JING_LAYER)), 0.7f, LI_SIZE, "heaven_li_jing");
        }
    }

    // ── Nezha ───────────────────────────────────────────────────────────

    /**
     * Nezha, the Third Lotus Prince (256x256; heaven_great_nezha.py): a youth in a red bodice and a skirt of
     * lotus petals, his hair in two buns, the cinnabar mark on his brow; hovering on the Wind-Fire Wheels,
     * spinning and wreathed in fire; the Universe Ring about his left arm, the long red Huntian sash looping
     * behind him, the Fire-Tipped Spear in his right hand.
     */
    public static LayerDefinition nezhaLayer() {
        // GENERATED nezha: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(142, 0).mirror(false).addBox(-3.8f, -1.8f, -2.6f, 7.6f, 2.4f, 5.2f)
                .texOffs(0, 36).mirror(false).addBox(-3.4f, 0, -2.3f, 6.8f, 2.6f, 4.6f)
                .texOffs(167, 45).mirror(false).addBox(-1.1f, -2.4f, -3.2f, 2.2f, 2, 1), PartPose.offsetAndRotation(0, 3.6f, 0, 0, 0, 0));
        PartDefinition rightWaisttail = waist.addOrReplaceChild("right_waisttail", CubeListBuilder.create()
                .texOffs(236, 36).mirror(false).addBox(-0.6f, 0, -0.2f, 1.2f, 3.2f, 0.4f), PartPose.offsetAndRotation(-0.6f, -0.6f, -3, -0.2f, 0, 0.15f));
        PartDefinition leftWaisttail = waist.addOrReplaceChild("left_waisttail", CubeListBuilder.create()
                .texOffs(236, 36).mirror(true).addBox(-0.6f, 0, -0.2f, 1.2f, 3.2f, 0.4f), PartPose.offsetAndRotation(0.6f, -0.6f, -3, -0.2f, 0, -0.15f));
        PartDefinition rightWaisttail2 = rightWaisttail.addOrReplaceChild("right_waisttail_2", CubeListBuilder.create()
                .texOffs(107, 45).mirror(false).addBox(-0.7f, 0, -0.2f, 1.4f, 3, 0.4f), PartPose.offsetAndRotation(0, 2.8f, 0, 0.1f, 0, 0));
        PartDefinition leftWaisttail2 = leftWaisttail.addOrReplaceChild("left_waisttail_2", CubeListBuilder.create()
                .texOffs(107, 45).mirror(true).addBox(-0.7f, 0, -0.2f, 1.4f, 3, 0.4f), PartPose.offsetAndRotation(0, 2.8f, 0, 0.1f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(108, 0).mirror(false).addBox(-1.7f, 0, -1.7f, 3.4f, 5.4f, 3.4f), PartPose.offsetAndRotation(-2, 3.6f, 0.1f, -0.12f, 0, 0.04f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(108, 0).mirror(true).addBox(-1.7f, 0, -1.7f, 3.4f, 5.4f, 3.4f), PartPose.offsetAndRotation(2, 3.6f, 0.1f, -0.12f, 0, -0.04f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(171, 0).mirror(false).addBox(-1.6f, 0, -1.6f, 3.2f, 4.8f, 3.2f)
                .texOffs(140, 36).mirror(false).addBox(-1.7f, 3.4f, -2.9f, 3.4f, 1.6f, 4.6f), PartPose.offsetAndRotation(0, 5.2f, 0, 0.12f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(171, 0).mirror(true).addBox(-1.6f, 0, -1.6f, 3.2f, 4.8f, 3.2f)
                .texOffs(140, 36).mirror(true).addBox(-1.7f, 3.4f, -2.9f, 3.4f, 1.6f, 4.6f), PartPose.offsetAndRotation(0, 5.2f, 0, 0.12f, 0, 0));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(51, 0).mirror(false).addBox(-3.6f, -7.2f, -2.3f, 7.2f, 7.4f, 4.6f)
                .texOffs(25, 36).mirror(false).addBox(-2.9f, -6.6f, -2.7f, 5.8f, 6.8f, 0.5f)
                .texOffs(40, 36).mirror(false).addBox(-2.9f, -8.2f, -2.7f, 5.8f, 1.3f, 5.4f)
                .texOffs(176, 45).mirror(false).addBox(-1.1f, -7.2f, -3.1f, 2.2f, 1.9f, 0.5f)
                .texOffs(243, 36).mirror(false).addBox(-1.4f, -8.8f, -1.4f, 2.8f, 1.8f, 2.8f), PartPose.offsetAndRotation(0, -1.6f, 0, 0, 0, 0));
        PartDefinition rightShoulderPetal = body.addOrReplaceChild("right_shoulder_petal", CubeListBuilder.create()
                .texOffs(0, 45).mirror(false).addBox(-3.6f, -0.3f, -2, 3.6f, 0.6f, 4), PartPose.offsetAndRotation(-3.4f, -6.9f, 0, 0, 0, 0.55f));
        PartDefinition leftShoulderPetal = body.addOrReplaceChild("left_shoulder_petal", CubeListBuilder.create()
                .texOffs(0, 45).mirror(true).addBox(0, -0.3f, -2, 3.6f, 0.6f, 4), PartPose.offsetAndRotation(3.4f, -6.9f, 0, 0, 0, -0.55f));
        PartDefinition rightShoulderPetalB = body.addOrReplaceChild("right_shoulder_petal_b", CubeListBuilder.create()
                .texOffs(17, 45).mirror(false).addBox(-3, -0.3f, -1.6f, 3, 0.6f, 3.2f), PartPose.offsetAndRotation(-3, -7.2f, 0.2f, 0, 0.6f, 0.85f));
        PartDefinition leftShoulderPetalB = body.addOrReplaceChild("left_shoulder_petal_b", CubeListBuilder.create()
                .texOffs(17, 45).mirror(true).addBox(0, -0.3f, -1.6f, 3, 0.6f, 3.2f), PartPose.offsetAndRotation(3, -7.2f, 0.2f, 0, -0.6f, -0.85f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(22, 0).mirror(false).addBox(-3.5f, -7.2f, -3.3f, 7, 7, 6.6f)
                .texOffs(65, 36).mirror(false).addBox(-3.7f, -3.4f, -2.6f, 7.4f, 2.6f, 5)
                .texOffs(185, 45).mirror(false).addBox(-0.6f, -3.7f, -3.8f, 1.2f, 1.3f, 0.6f)
                .texOffs(188, 0).mirror(false).addBox(-3.7f, -7.7f, -3.1f, 7.4f, 2, 6.8f)
                .texOffs(78, 0).mirror(false).addBox(-3.7f, -7, 0.3f, 7.4f, 6.2f, 3.4f)
                .texOffs(192, 45).mirror(false).addBox(-3.6f, -6, -3.6f, 7.2f, 1.2f, 0.6f), PartPose.offsetAndRotation(0, -8.6f, -0.2f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(216, 45).mirror(false).addBox(-2.8f, -0.45f, -0.3f, 2.8f, 0.8f, 0.5f), PartPose.offsetAndRotation(-0.5f, -4.85f, -3.45f, 0, 0.1f, 0.28f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(216, 45).mirror(true).addBox(0, -0.45f, -0.3f, 2.8f, 0.8f, 0.5f), PartPose.offsetAndRotation(0.5f, -4.85f, -3.45f, 0, -0.1f, -0.28f));
        PartDefinition rightBun = head.addOrReplaceChild("right_bun", CubeListBuilder.create()
                .texOffs(172, 36).mirror(false).addBox(-1.5f, -3, -1.5f, 3, 3, 3)
                .texOffs(32, 45).mirror(false).addBox(-1.8f, -0.9f, -1.8f, 3.6f, 0.9f, 3.6f), PartPose.offsetAndRotation(-2.7f, -7.2f, -0.2f, 0, 0, -0.45f));
        PartDefinition leftBun = head.addOrReplaceChild("left_bun", CubeListBuilder.create()
                .texOffs(172, 36).mirror(true).addBox(-1.5f, -3, -1.5f, 3, 3, 3)
                .texOffs(32, 45).mirror(true).addBox(-1.8f, -0.9f, -1.8f, 3.6f, 0.9f, 3.6f), PartPose.offsetAndRotation(2.7f, -7.2f, -0.2f, 0, 0, 0.45f));
        PartDefinition rightBunRibbon = rightBun.addOrReplaceChild("right_bun_ribbon", CubeListBuilder.create()
                .texOffs(49, 45).mirror(false).addBox(-0.45f, 0, -0.15f, 0.9f, 3.4f, 0.3f), PartPose.offsetAndRotation(-1.4f, -0.6f, 1.2f, 0.6f, 0, 0.5f));
        PartDefinition leftBunRibbon = leftBun.addOrReplaceChild("left_bun_ribbon", CubeListBuilder.create()
                .texOffs(49, 45).mirror(true).addBox(-0.45f, 0, -0.15f, 0.9f, 3.4f, 0.3f), PartPose.offsetAndRotation(1.4f, -0.6f, 1.2f, 0.6f, 0, -0.5f));
        PartDefinition rightBunRibbon2 = rightBunRibbon.addOrReplaceChild("right_bun_ribbon_2", CubeListBuilder.create()
                .texOffs(49, 45).mirror(false).addBox(-0.5f, 0, -0.15f, 1, 3.2f, 0.3f), PartPose.offsetAndRotation(0, 3, 0, 0.2f, 0, 0));
        PartDefinition leftBunRibbon2 = leftBunRibbon.addOrReplaceChild("left_bun_ribbon_2", CubeListBuilder.create()
                .texOffs(49, 45).mirror(true).addBox(-0.5f, 0, -0.15f, 1, 3.2f, 0.3f), PartPose.offsetAndRotation(0, 3, 0, 0.2f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(92, 36).mirror(false).addBox(-1.3f, -0.6f, -1.3f, 2.6f, 4.8f, 2.6f)
                .texOffs(114, 45).mirror(false).addBox(-1.5f, 1.6f, -1.5f, 3, 0.9f, 3), PartPose.offsetAndRotation(-4.6f, -6.4f, 0, -0.12f, 0, 0.16f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(92, 36).mirror(true).addBox(-1.3f, -0.6f, -1.3f, 2.6f, 4.8f, 2.6f)
                .texOffs(114, 45).mirror(true).addBox(-1.5f, 1.6f, -1.5f, 3, 0.9f, 3), PartPose.offsetAndRotation(4.6f, -6.4f, 0, -0.35f, 0, -0.35f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(105, 36).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 4.2f, 2.4f)
                .texOffs(54, 45).mirror(false).addBox(-1.45f, 2.8f, -1.45f, 2.9f, 1.1f, 2.9f), PartPose.offsetAndRotation(0, 4.4f, 0, -0.85f, -0.2f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(105, 36).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 4.2f, 2.4f)
                .texOffs(54, 45).mirror(true).addBox(-1.45f, 2.8f, -1.45f, 2.9f, 1.1f, 2.9f), PartPose.offsetAndRotation(0, 4.4f, 0, -0.9f, 0.35f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(185, 36).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(185, 36).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4, 0, 0, 0, 0));
        PartDefinition petal0 = waist.addOrReplaceChild("petal_0", CubeListBuilder.create()
                .texOffs(118, 36).mirror(false).addBox(-1.9f, 0, -0.25f, 3.8f, 6.2f, 0.5f), PartPose.offsetAndRotation(-1.3777f, -0.6f, -2.4945f, -0.42f, 0.3927f, 0));
        PartDefinition petal1 = waist.addOrReplaceChild("petal_1", CubeListBuilder.create()
                .texOffs(118, 36).mirror(false).addBox(-1.9f, 0, -0.25f, 3.8f, 6.2f, 0.5f), PartPose.offsetAndRotation(-3.326f, -0.6f, -1.0332f, -0.42f, 1.1781f, 0));
        PartDefinition petal2 = waist.addOrReplaceChild("petal_2", CubeListBuilder.create()
                .texOffs(118, 36).mirror(false).addBox(-1.9f, 0, -0.25f, 3.8f, 6.2f, 0.5f), PartPose.offsetAndRotation(-3.326f, -0.6f, 1.0332f, -0.42f, 1.9635f, 0));
        PartDefinition petal3 = waist.addOrReplaceChild("petal_3", CubeListBuilder.create()
                .texOffs(118, 36).mirror(false).addBox(-1.9f, 0, -0.25f, 3.8f, 6.2f, 0.5f), PartPose.offsetAndRotation(-1.3777f, -0.6f, 2.4945f, -0.42f, 2.7489f, 0));
        PartDefinition petal4 = waist.addOrReplaceChild("petal_4", CubeListBuilder.create()
                .texOffs(118, 36).mirror(false).addBox(-1.9f, 0, -0.25f, 3.8f, 6.2f, 0.5f), PartPose.offsetAndRotation(1.3777f, -0.6f, 2.4945f, -0.42f, 3.5343f, 0));
        PartDefinition petal5 = waist.addOrReplaceChild("petal_5", CubeListBuilder.create()
                .texOffs(118, 36).mirror(false).addBox(-1.9f, 0, -0.25f, 3.8f, 6.2f, 0.5f), PartPose.offsetAndRotation(3.326f, -0.6f, 1.0332f, -0.42f, 4.3197f, 0));
        PartDefinition petal6 = waist.addOrReplaceChild("petal_6", CubeListBuilder.create()
                .texOffs(118, 36).mirror(false).addBox(-1.9f, 0, -0.25f, 3.8f, 6.2f, 0.5f), PartPose.offsetAndRotation(3.326f, -0.6f, -1.0332f, -0.42f, 5.1051f, 0));
        PartDefinition petal7 = waist.addOrReplaceChild("petal_7", CubeListBuilder.create()
                .texOffs(118, 36).mirror(false).addBox(-1.9f, 0, -0.25f, 3.8f, 6.2f, 0.5f), PartPose.offsetAndRotation(1.3777f, -0.6f, -2.4945f, -0.42f, 5.8905f, 0));
        PartDefinition petalLow0 = waist.addOrReplaceChild("petal_low_0", CubeListBuilder.create()
                .texOffs(198, 36).mirror(false).addBox(-1.6f, 0, -0.25f, 3.2f, 4.6f, 0.5f), PartPose.offsetAndRotation(-0, 2.4f, -2.4f, -0.62f, 0, 0));
        PartDefinition petalLow1 = waist.addOrReplaceChild("petal_low_1", CubeListBuilder.create()
                .texOffs(198, 36).mirror(false).addBox(-1.6f, 0, -0.25f, 3.2f, 4.6f, 0.5f), PartPose.offsetAndRotation(-2.2627f, 2.4f, -1.6971f, -0.62f, 0.7854f, 0));
        PartDefinition petalLow2 = waist.addOrReplaceChild("petal_low_2", CubeListBuilder.create()
                .texOffs(198, 36).mirror(false).addBox(-1.6f, 0, -0.25f, 3.2f, 4.6f, 0.5f), PartPose.offsetAndRotation(-3.2f, 2.4f, -0, -0.62f, 1.5708f, 0));
        PartDefinition petalLow3 = waist.addOrReplaceChild("petal_low_3", CubeListBuilder.create()
                .texOffs(198, 36).mirror(false).addBox(-1.6f, 0, -0.25f, 3.2f, 4.6f, 0.5f), PartPose.offsetAndRotation(-2.2627f, 2.4f, 1.6971f, -0.62f, 2.3562f, 0));
        PartDefinition petalLow4 = waist.addOrReplaceChild("petal_low_4", CubeListBuilder.create()
                .texOffs(198, 36).mirror(false).addBox(-1.6f, 0, -0.25f, 3.2f, 4.6f, 0.5f), PartPose.offsetAndRotation(-0, 2.4f, 2.4f, -0.62f, 3.1416f, 0));
        PartDefinition petalLow5 = waist.addOrReplaceChild("petal_low_5", CubeListBuilder.create()
                .texOffs(198, 36).mirror(false).addBox(-1.6f, 0, -0.25f, 3.2f, 4.6f, 0.5f), PartPose.offsetAndRotation(2.2627f, 2.4f, 1.6971f, -0.62f, 3.927f, 0));
        PartDefinition petalLow6 = waist.addOrReplaceChild("petal_low_6", CubeListBuilder.create()
                .texOffs(198, 36).mirror(false).addBox(-1.6f, 0, -0.25f, 3.2f, 4.6f, 0.5f), PartPose.offsetAndRotation(3.2f, 2.4f, 0, -0.62f, 4.7124f, 0));
        PartDefinition petalLow7 = waist.addOrReplaceChild("petal_low_7", CubeListBuilder.create()
                .texOffs(198, 36).mirror(false).addBox(-1.6f, 0, -0.25f, 3.2f, 4.6f, 0.5f), PartPose.offsetAndRotation(2.2627f, 2.4f, -1.6971f, -0.62f, 5.4978f, 0));
        PartDefinition rightWheel = root.addOrReplaceChild("right_wheel", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.8f, 17.8f, 0.6f, 0, 0.6f, 0.1f));
        PartDefinition rightWheelSpin = rightWheel.addOrReplaceChild("right_wheel_spin", CubeListBuilder.create()
                .texOffs(209, 36).mirror(false).addBox(-1, -1.3f, -1.3f, 2, 2.6f, 2.6f)
                .texOffs(103, 0).mirror(false).addBox(-0.35f, -4.8f, -0.35f, 0.7f, 9.6f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition rightWheelSpoke0 = rightWheelSpin.addOrReplaceChild("right_wheel_spoke_0", CubeListBuilder.create()
                .texOffs(103, 0).mirror(false).addBox(-0.35f, -4.8f, -0.35f, 0.7f, 9.6f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 1.0472f, 0, 0));
        PartDefinition rightWheelSpoke1 = rightWheelSpin.addOrReplaceChild("right_wheel_spoke_1", CubeListBuilder.create()
                .texOffs(103, 0).mirror(false).addBox(-0.35f, -4.8f, -0.35f, 0.7f, 9.6f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 2.0944f, 0, 0));
        PartDefinition rightWheelRim0 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_0", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -4.6364f, -1.2423f, 3.4034f, 0, 0));
        PartDefinition rightWheelRim1 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_1", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -3.3941f, -3.3941f, 3.927f, 0, 0));
        PartDefinition rightWheelRim2 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_2", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -1.2423f, -4.6364f, 4.4506f, 0, 0));
        PartDefinition rightWheelRim3 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_3", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 1.2423f, -4.6364f, 4.9742f, 0, 0));
        PartDefinition rightWheelRim4 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_4", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 3.3941f, -3.3941f, 5.4978f, 0, 0));
        PartDefinition rightWheelRim5 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_5", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 4.6364f, -1.2423f, 6.0214f, 0, 0));
        PartDefinition rightWheelRim6 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_6", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 4.6364f, 1.2423f, 6.545f, 0, 0));
        PartDefinition rightWheelRim7 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_7", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 3.3941f, 3.3941f, 7.0686f, 0, 0));
        PartDefinition rightWheelRim8 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_8", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 1.2423f, 4.6364f, 7.5922f, 0, 0));
        PartDefinition rightWheelRim9 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_9", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -1.2423f, 4.6364f, 8.1158f, 0, 0));
        PartDefinition rightWheelRim10 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_10", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -3.3941f, 3.3941f, 8.6394f, 0, 0));
        PartDefinition rightWheelRim11 = rightWheelSpin.addOrReplaceChild("right_wheel_rim_11", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -4.6364f, 1.2423f, 9.163f, 0, 0));
        PartDefinition rightWheelFlame0 = rightWheel.addOrReplaceChild("right_wheel_flame_0", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.25f, -5, -1.6f, 0.5f, 5, 3.2f)
                .texOffs(220, 36).mirror(false).addBox(-1, -5, -0.25f, 2, 5, 0.5f), PartPose.offsetAndRotation(0, -5.1f, -0, -0.75f, 0, 0));
        PartDefinition rightWheelFlame1 = rightWheel.addOrReplaceChild("right_wheel_flame_1", CubeListBuilder.create()
                .texOffs(129, 36).mirror(false).addBox(-0.25f, -3.6f, -1.6f, 0.5f, 3.6f, 3.2f)
                .texOffs(78, 45).mirror(false).addBox(-1, -3.6f, -0.25f, 2, 3.6f, 0.5f), PartPose.offsetAndRotation(0, -4.126f, -2.9977f, -0.1217f, 0, 0));
        PartDefinition rightWheelFlame2 = rightWheel.addOrReplaceChild("right_wheel_flame_2", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.25f, -5, -1.6f, 0.5f, 5, 3.2f)
                .texOffs(220, 36).mirror(false).addBox(-1, -5, -0.25f, 2, 5, 0.5f), PartPose.offsetAndRotation(0, -1.576f, -4.8504f, 0.5066f, 0, 0));
        PartDefinition rightWheelFlame3 = rightWheel.addOrReplaceChild("right_wheel_flame_3", CubeListBuilder.create()
                .texOffs(129, 36).mirror(false).addBox(-0.25f, -3.6f, -1.6f, 0.5f, 3.6f, 3.2f)
                .texOffs(78, 45).mirror(false).addBox(-1, -3.6f, -0.25f, 2, 3.6f, 0.5f), PartPose.offsetAndRotation(0, 1.576f, -4.8504f, 1.135f, 0, 0));
        PartDefinition rightWheelFlame4 = rightWheel.addOrReplaceChild("right_wheel_flame_4", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.25f, -5, -1.6f, 0.5f, 5, 3.2f)
                .texOffs(220, 36).mirror(false).addBox(-1, -5, -0.25f, 2, 5, 0.5f), PartPose.offsetAndRotation(0, 4.126f, -2.9977f, 1.7633f, 0, 0));
        PartDefinition rightWheelFlame5 = rightWheel.addOrReplaceChild("right_wheel_flame_5", CubeListBuilder.create()
                .texOffs(129, 36).mirror(false).addBox(-0.25f, -3.6f, -1.6f, 0.5f, 3.6f, 3.2f)
                .texOffs(78, 45).mirror(false).addBox(-1, -3.6f, -0.25f, 2, 3.6f, 0.5f), PartPose.offsetAndRotation(0, 5.1f, -0, 2.3916f, 0, 0));
        PartDefinition rightWheelFlame6 = rightWheel.addOrReplaceChild("right_wheel_flame_6", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.25f, -5, -1.6f, 0.5f, 5, 3.2f)
                .texOffs(220, 36).mirror(false).addBox(-1, -5, -0.25f, 2, 5, 0.5f), PartPose.offsetAndRotation(0, 4.126f, 2.9977f, 3.0199f, 0, 0));
        PartDefinition rightWheelFlame7 = rightWheel.addOrReplaceChild("right_wheel_flame_7", CubeListBuilder.create()
                .texOffs(129, 36).mirror(false).addBox(-0.25f, -3.6f, -1.6f, 0.5f, 3.6f, 3.2f)
                .texOffs(78, 45).mirror(false).addBox(-1, -3.6f, -0.25f, 2, 3.6f, 0.5f), PartPose.offsetAndRotation(0, 1.576f, 4.8504f, 3.6482f, 0, 0));
        PartDefinition rightWheelFlame8 = rightWheel.addOrReplaceChild("right_wheel_flame_8", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.25f, -5, -1.6f, 0.5f, 5, 3.2f)
                .texOffs(220, 36).mirror(false).addBox(-1, -5, -0.25f, 2, 5, 0.5f), PartPose.offsetAndRotation(0, -1.576f, 4.8504f, 4.2765f, 0, 0));
        PartDefinition rightWheelFlame9 = rightWheel.addOrReplaceChild("right_wheel_flame_9", CubeListBuilder.create()
                .texOffs(129, 36).mirror(false).addBox(-0.25f, -3.6f, -1.6f, 0.5f, 3.6f, 3.2f)
                .texOffs(78, 45).mirror(false).addBox(-1, -3.6f, -0.25f, 2, 3.6f, 0.5f), PartPose.offsetAndRotation(0, -4.126f, 2.9977f, 4.9049f, 0, 0));
        PartDefinition leftWheel = root.addOrReplaceChild("left_wheel", CubeListBuilder.create(), PartPose.offsetAndRotation(2.8f, 17.8f, 0.6f, 0, -0.6f, -0.1f));
        PartDefinition leftWheelSpin = leftWheel.addOrReplaceChild("left_wheel_spin", CubeListBuilder.create()
                .texOffs(209, 36).mirror(false).addBox(-1, -1.3f, -1.3f, 2, 2.6f, 2.6f)
                .texOffs(103, 0).mirror(false).addBox(-0.35f, -4.8f, -0.35f, 0.7f, 9.6f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition leftWheelSpoke0 = leftWheelSpin.addOrReplaceChild("left_wheel_spoke_0", CubeListBuilder.create()
                .texOffs(103, 0).mirror(false).addBox(-0.35f, -4.8f, -0.35f, 0.7f, 9.6f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 1.0472f, 0, 0));
        PartDefinition leftWheelSpoke1 = leftWheelSpin.addOrReplaceChild("left_wheel_spoke_1", CubeListBuilder.create()
                .texOffs(103, 0).mirror(false).addBox(-0.35f, -4.8f, -0.35f, 0.7f, 9.6f, 0.7f), PartPose.offsetAndRotation(0, 0, 0, 2.0944f, 0, 0));
        PartDefinition leftWheelRim0 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_0", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -4.6364f, -1.2423f, 3.4034f, 0, 0));
        PartDefinition leftWheelRim1 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_1", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -3.3941f, -3.3941f, 3.927f, 0, 0));
        PartDefinition leftWheelRim2 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_2", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -1.2423f, -4.6364f, 4.4506f, 0, 0));
        PartDefinition leftWheelRim3 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_3", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 1.2423f, -4.6364f, 4.9742f, 0, 0));
        PartDefinition leftWheelRim4 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_4", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 3.3941f, -3.3941f, 5.4978f, 0, 0));
        PartDefinition leftWheelRim5 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_5", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 4.6364f, -1.2423f, 6.0214f, 0, 0));
        PartDefinition leftWheelRim6 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_6", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 4.6364f, 1.2423f, 6.545f, 0, 0));
        PartDefinition leftWheelRim7 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_7", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 3.3941f, 3.3941f, 7.0686f, 0, 0));
        PartDefinition leftWheelRim8 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_8", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, 1.2423f, 4.6364f, 7.5922f, 0, 0));
        PartDefinition leftWheelRim9 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_9", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -1.2423f, 4.6364f, 8.1158f, 0, 0));
        PartDefinition leftWheelRim10 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_10", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -3.3941f, 3.3941f, 8.6394f, 0, 0));
        PartDefinition leftWheelRim11 = leftWheelSpin.addOrReplaceChild("left_wheel_rim_11", CubeListBuilder.create()
                .texOffs(67, 45).mirror(false).addBox(-0.75f, -0.65f, -1.4923f, 1.5f, 1.3f, 2.9847f), PartPose.offsetAndRotation(0, -4.6364f, 1.2423f, 9.163f, 0, 0));
        PartDefinition leftWheelFlame0 = leftWheel.addOrReplaceChild("left_wheel_flame_0", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.25f, -5, -1.6f, 0.5f, 5, 3.2f)
                .texOffs(220, 36).mirror(false).addBox(-1, -5, -0.25f, 2, 5, 0.5f), PartPose.offsetAndRotation(0, -5.1f, -0, -0.75f, 0, 0));
        PartDefinition leftWheelFlame1 = leftWheel.addOrReplaceChild("left_wheel_flame_1", CubeListBuilder.create()
                .texOffs(129, 36).mirror(false).addBox(-0.25f, -3.6f, -1.6f, 0.5f, 3.6f, 3.2f)
                .texOffs(78, 45).mirror(false).addBox(-1, -3.6f, -0.25f, 2, 3.6f, 0.5f), PartPose.offsetAndRotation(0, -4.126f, -2.9977f, -0.1217f, 0, 0));
        PartDefinition leftWheelFlame2 = leftWheel.addOrReplaceChild("left_wheel_flame_2", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.25f, -5, -1.6f, 0.5f, 5, 3.2f)
                .texOffs(220, 36).mirror(false).addBox(-1, -5, -0.25f, 2, 5, 0.5f), PartPose.offsetAndRotation(0, -1.576f, -4.8504f, 0.5066f, 0, 0));
        PartDefinition leftWheelFlame3 = leftWheel.addOrReplaceChild("left_wheel_flame_3", CubeListBuilder.create()
                .texOffs(129, 36).mirror(false).addBox(-0.25f, -3.6f, -1.6f, 0.5f, 3.6f, 3.2f)
                .texOffs(78, 45).mirror(false).addBox(-1, -3.6f, -0.25f, 2, 3.6f, 0.5f), PartPose.offsetAndRotation(0, 1.576f, -4.8504f, 1.135f, 0, 0));
        PartDefinition leftWheelFlame4 = leftWheel.addOrReplaceChild("left_wheel_flame_4", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.25f, -5, -1.6f, 0.5f, 5, 3.2f)
                .texOffs(220, 36).mirror(false).addBox(-1, -5, -0.25f, 2, 5, 0.5f), PartPose.offsetAndRotation(0, 4.126f, -2.9977f, 1.7633f, 0, 0));
        PartDefinition leftWheelFlame5 = leftWheel.addOrReplaceChild("left_wheel_flame_5", CubeListBuilder.create()
                .texOffs(129, 36).mirror(false).addBox(-0.25f, -3.6f, -1.6f, 0.5f, 3.6f, 3.2f)
                .texOffs(78, 45).mirror(false).addBox(-1, -3.6f, -0.25f, 2, 3.6f, 0.5f), PartPose.offsetAndRotation(0, 5.1f, -0, 2.3916f, 0, 0));
        PartDefinition leftWheelFlame6 = leftWheel.addOrReplaceChild("left_wheel_flame_6", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.25f, -5, -1.6f, 0.5f, 5, 3.2f)
                .texOffs(220, 36).mirror(false).addBox(-1, -5, -0.25f, 2, 5, 0.5f), PartPose.offsetAndRotation(0, 4.126f, 2.9977f, 3.0199f, 0, 0));
        PartDefinition leftWheelFlame7 = leftWheel.addOrReplaceChild("left_wheel_flame_7", CubeListBuilder.create()
                .texOffs(129, 36).mirror(false).addBox(-0.25f, -3.6f, -1.6f, 0.5f, 3.6f, 3.2f)
                .texOffs(78, 45).mirror(false).addBox(-1, -3.6f, -0.25f, 2, 3.6f, 0.5f), PartPose.offsetAndRotation(0, 1.576f, 4.8504f, 3.6482f, 0, 0));
        PartDefinition leftWheelFlame8 = leftWheel.addOrReplaceChild("left_wheel_flame_8", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.25f, -5, -1.6f, 0.5f, 5, 3.2f)
                .texOffs(220, 36).mirror(false).addBox(-1, -5, -0.25f, 2, 5, 0.5f), PartPose.offsetAndRotation(0, -1.576f, 4.8504f, 4.2765f, 0, 0));
        PartDefinition leftWheelFlame9 = leftWheel.addOrReplaceChild("left_wheel_flame_9", CubeListBuilder.create()
                .texOffs(129, 36).mirror(false).addBox(-0.25f, -3.6f, -1.6f, 0.5f, 3.6f, 3.2f)
                .texOffs(78, 45).mirror(false).addBox(-1, -3.6f, -0.25f, 2, 3.6f, 0.5f), PartPose.offsetAndRotation(0, -4.126f, 2.9977f, 4.9049f, 0, 0));
        PartDefinition qiankun = leftArm.addOrReplaceChild("qiankun", CubeListBuilder.create(), PartPose.offsetAndRotation(0.2f, 2.2f, 0, 0.35f, 0, -0.5f));
        PartDefinition qiankun0 = qiankun.addOrReplaceChild("qiankun_0", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(3.5739f, 0, 0.9576f, 0, -1.8326f, 0));
        PartDefinition qiankun1 = qiankun.addOrReplaceChild("qiankun_1", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(2.6163f, 0, 2.6163f, 0, -2.3562f, 0));
        PartDefinition qiankun2 = qiankun.addOrReplaceChild("qiankun_2", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(0.9576f, 0, 3.5739f, 0, -2.8798f, 0));
        PartDefinition qiankun3 = qiankun.addOrReplaceChild("qiankun_3", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(-0.9576f, 0, 3.5739f, 0, -3.4034f, 0));
        PartDefinition qiankun4 = qiankun.addOrReplaceChild("qiankun_4", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(-2.6163f, 0, 2.6163f, 0, -3.927f, 0));
        PartDefinition qiankun5 = qiankun.addOrReplaceChild("qiankun_5", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(-3.5739f, 0, 0.9576f, 0, -4.4506f, 0));
        PartDefinition qiankun6 = qiankun.addOrReplaceChild("qiankun_6", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(-3.5739f, 0, -0.9576f, 0, -4.9742f, 0));
        PartDefinition qiankun7 = qiankun.addOrReplaceChild("qiankun_7", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(-2.6163f, 0, -2.6163f, 0, -5.4978f, 0));
        PartDefinition qiankun8 = qiankun.addOrReplaceChild("qiankun_8", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(-0.9576f, 0, -3.5739f, 0, -6.0214f, 0));
        PartDefinition qiankun9 = qiankun.addOrReplaceChild("qiankun_9", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(0.9576f, 0, -3.5739f, 0, -6.545f, 0));
        PartDefinition qiankun10 = qiankun.addOrReplaceChild("qiankun_10", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(2.6163f, 0, -2.6163f, 0, -7.0686f, 0));
        PartDefinition qiankun11 = qiankun.addOrReplaceChild("qiankun_11", CubeListBuilder.create()
                .texOffs(127, 45).mirror(false).addBox(-1.1576f, -0.65f, -0.65f, 2.3153f, 1.3f, 1.3f), PartPose.offsetAndRotation(3.5739f, 0, -0.9576f, 0, -7.5922f, 0));
        PartDefinition spear = rightHand.addOrReplaceChild("spear", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5f, -22, -0.5f, 1, 34, 1)
                .texOffs(138, 45).mirror(false).addBox(-0.8f, -23.2f, -0.8f, 1.6f, 1.4f, 1.6f)
                .texOffs(85, 45).mirror(false).addBox(-1.2f, -25, -1.2f, 2.4f, 1.9f, 2.4f)
                .texOffs(230, 0).mirror(false).addBox(-1.1f, -32.4f, -0.3f, 2.2f, 7.6f, 0.6f)
                .texOffs(147, 45).mirror(false).addBox(-0.7f, 11.6f, -0.7f, 1.4f, 1.6f, 1.4f), PartPose.offsetAndRotation(0, 1.2f, 0, 0.8525f, 0.1978f, 0.1057f));
        PartDefinition spearFire = spear.addOrReplaceChild("spear_fire", CubeListBuilder.create()
                .texOffs(125, 0).mirror(false).addBox(-3.2f, -8, -0.25f, 6.4f, 9, 0.5f)
                .texOffs(5, 0).mirror(false).addBox(-0.25f, -8, -3.2f, 0.5f, 9, 6.4f), PartPose.offsetAndRotation(0, -27.6f, 0, 0, 0, 0));
        PartDefinition spearTassel = spear.addOrReplaceChild("spear_tassel", CubeListBuilder.create()
                .texOffs(211, 45).mirror(false).addBox(-0.5f, 0, -0.5f, 1, 1.4f, 1), PartPose.offsetAndRotation(0, -22.4f, 0, 0, 0, 0));
        PartDefinition spearTassel2 = spearTassel.addOrReplaceChild("spear_tassel_2", CubeListBuilder.create()
                .texOffs(159, 36).mirror(false).addBox(-1.1f, 0, -1.1f, 2.2f, 3.6f, 2.2f), PartPose.offsetAndRotation(0, 1, 0, 0, 0, 0));
        PartDefinition sash = body.addOrReplaceChild("sash", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -6.5f, 3.4f, 0.45f, 0, 0));
        PartDefinition sashArc0 = sash.addOrReplaceChild("sash_arc_0", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(-7.7697f, 0.6868f, 0, 0, 0, -2.0138f));
        PartDefinition sashArc1 = sash.addOrReplaceChild("sash_arc_1", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(-8.5504f, -2.0781f, 0, 0, 0, -1.6782f));
        PartDefinition sashArc2 = sash.addOrReplaceChild("sash_arc_2", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(-8.377f, -4.9458f, 0, 0, 0, -1.3426f));
        PartDefinition sashArc3 = sash.addOrReplaceChild("sash_arc_3", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(-7.2686f, -7.5964f, 0, 0, 0, -1.0069f));
        PartDefinition sashArc4 = sash.addOrReplaceChild("sash_arc_4", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(-5.3491f, -9.734f, 0, 0, 0, -0.6713f));
        PartDefinition sashArc5 = sash.addOrReplaceChild("sash_arc_5", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(-2.8326f, -11.1201f, 0, 0, 0, -0.3356f));
        PartDefinition sashArc6 = sash.addOrReplaceChild("sash_arc_6", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(0, -11.6f, 0, 0, 0, 0));
        PartDefinition sashArc7 = sash.addOrReplaceChild("sash_arc_7", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(2.8326f, -11.1201f, 0, 0, 0, 0.3356f));
        PartDefinition sashArc8 = sash.addOrReplaceChild("sash_arc_8", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(5.3491f, -9.734f, 0, 0, 0, 0.6713f));
        PartDefinition sashArc9 = sash.addOrReplaceChild("sash_arc_9", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(7.2686f, -7.5964f, 0, 0, 0, 1.0069f));
        PartDefinition sashArc10 = sash.addOrReplaceChild("sash_arc_10", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(8.377f, -4.9458f, 0, 0, 0, 1.3426f));
        PartDefinition sashArc11 = sash.addOrReplaceChild("sash_arc_11", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(8.5504f, -2.0781f, 0, 0, 0, 1.6782f));
        PartDefinition sashArc12 = sash.addOrReplaceChild("sash_arc_12", CubeListBuilder.create()
                .texOffs(156, 45).mirror(false).addBox(-1.7365f, -1.2f, -0.3f, 3.473f, 2.4f, 0.6f), PartPose.offsetAndRotation(7.7697f, 0.6868f, 0, 0, 0, 2.0138f));
        PartDefinition rightSashEnd = sash.addOrReplaceChild("right_sash_end", CubeListBuilder.create()
                .texOffs(227, 36).mirror(false).addBox(-1.2f, 0, -0.25f, 2.4f, 4.6f, 0.5f), PartPose.offsetAndRotation(-7.0447f, 2.2328f, 0, 0.5f, 0, 0.35f));
        PartDefinition rightSashEnd2 = rightSashEnd.addOrReplaceChild("right_sash_end_2", CubeListBuilder.create()
                .texOffs(227, 36).mirror(false).addBox(-1.25f, 0, -0.25f, 2.5f, 4.6f, 0.5f), PartPose.offsetAndRotation(0, 4.2f, 0, 0.15f, 0, 0.1f));
        PartDefinition rightSashEnd3 = rightSashEnd2.addOrReplaceChild("right_sash_end_3", CubeListBuilder.create()
                .texOffs(227, 36).mirror(false).addBox(-1.3f, 0, -0.25f, 2.6f, 4.6f, 0.5f), PartPose.offsetAndRotation(0, 4.2f, 0, 0.2f, 0, 0.12f));
        PartDefinition rightSashEnd4 = rightSashEnd3.addOrReplaceChild("right_sash_end_4", CubeListBuilder.create()
                .texOffs(227, 36).mirror(false).addBox(-1.35f, 0, -0.25f, 2.7f, 4.4f, 0.5f), PartPose.offsetAndRotation(0, 4.2f, 0, 0.2f, 0, 0.1f));
        PartDefinition rightSashEnd5 = rightSashEnd4.addOrReplaceChild("right_sash_end_5", CubeListBuilder.create()
                .texOffs(98, 45).mirror(false).addBox(-1.4f, 0, -0.25f, 2.8f, 4, 0.5f), PartPose.offsetAndRotation(0, 4, 0, 0.15f, 0, 0.08f));
        PartDefinition rightSashEnd6 = rightSashEnd5.addOrReplaceChild("right_sash_end_6", CubeListBuilder.create()
                .texOffs(98, 45).mirror(false).addBox(-1.5f, 0, -0.25f, 3, 3.6f, 0.5f), PartPose.offsetAndRotation(0, 3.6f, 0, 0.1f, 0, 0.06f));
        PartDefinition leftSashEnd = sash.addOrReplaceChild("left_sash_end", CubeListBuilder.create()
                .texOffs(227, 36).mirror(false).addBox(-1.2f, 0, -0.25f, 2.4f, 4.6f, 0.5f), PartPose.offsetAndRotation(7.0447f, 2.2328f, 0, 0.5f, 0, -0.35f));
        PartDefinition leftSashEnd2 = leftSashEnd.addOrReplaceChild("left_sash_end_2", CubeListBuilder.create()
                .texOffs(227, 36).mirror(false).addBox(-1.25f, 0, -0.25f, 2.5f, 4.6f, 0.5f), PartPose.offsetAndRotation(0, 4.2f, 0, 0.15f, 0, -0.1f));
        PartDefinition leftSashEnd3 = leftSashEnd2.addOrReplaceChild("left_sash_end_3", CubeListBuilder.create()
                .texOffs(227, 36).mirror(false).addBox(-1.3f, 0, -0.25f, 2.6f, 4.6f, 0.5f), PartPose.offsetAndRotation(0, 4.2f, 0, 0.2f, 0, -0.12f));
        PartDefinition leftSashEnd4 = leftSashEnd3.addOrReplaceChild("left_sash_end_4", CubeListBuilder.create()
                .texOffs(227, 36).mirror(false).addBox(-1.35f, 0, -0.25f, 2.7f, 4.4f, 0.5f), PartPose.offsetAndRotation(0, 4.2f, 0, 0.2f, 0, -0.1f));
        PartDefinition leftSashEnd5 = leftSashEnd4.addOrReplaceChild("left_sash_end_5", CubeListBuilder.create()
                .texOffs(98, 45).mirror(false).addBox(-1.4f, 0, -0.25f, 2.8f, 4, 0.5f), PartPose.offsetAndRotation(0, 4, 0, 0.15f, 0, -0.08f));
        PartDefinition leftSashEnd6 = leftSashEnd5.addOrReplaceChild("left_sash_end_6", CubeListBuilder.create()
                .texOffs(98, 45).mirror(false).addBox(-1.5f, 0, -0.25f, 3, 3.6f, 0.5f), PartPose.offsetAndRotation(0, 3.6f, 0, 0.1f, 0, -0.06f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED nezha: end
    }

    /**
     * His pose: hovering, bobbing, his wheels spinning (faster as he goes), their flames streaming back, his
     * sash and ribbons fluttering; striking, the spear thrust; winding up, the wheels spin up and flare and
     * the spear is raised.
     */
    public static final class NezhaModel extends EntityModel<NezhaEntity> {
        private final ModelPart root, waist, body, head, rightArm, leftArm, rightForearm, leftForearm, rightLeg, leftLeg,
                rightSpin, leftSpin, ring, spear, spearFire, sash;
        private final List<ModelPart> flames = new ArrayList<>(), petals, arc;
        private final List<List<ModelPart>> streamers = new ArrayList<>();
        private float up;

        public NezhaModel(ModelPart root) {
            this.root = root;
            waist = root.getChild("waist");
            body = waist.getChild("body");
            head = body.getChild("head");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            ModelPart rightWheel = root.getChild("right_wheel"), leftWheel = root.getChild("left_wheel");
            rightSpin = rightWheel.getChild("right_wheel_spin");
            leftSpin = leftWheel.getChild("left_wheel_spin");
            flames.addAll(numbered(rightWheel, "right_wheel_flame"));
            flames.addAll(numbered(leftWheel, "left_wheel_flame"));
            ring = leftArm.getChild("qiankun");
            spear = rightForearm.getChild("right_hand").getChild("spear");
            spearFire = spear.getChild("spear_fire");
            sash = body.getChild("sash");
            arc = numbered(sash, "sash_arc");
            petals = numbered(waist, "petal");
            petals.addAll(numbered(waist, "petal_low"));
            streamers.add(chain(sash, "right_sash_end"));
            streamers.add(chain(sash, "left_sash_end"));
            streamers.add(chain(head.getChild("right_bun"), "right_bun_ribbon"));
            streamers.add(chain(head.getChild("left_bun"), "left_bun_ribbon"));
            streamers.add(chain(waist, "right_waisttail"));
            streamers.add(chain(waist, "left_waisttail"));
        }

        @Override
        public void prepareMobModel(NezhaEntity e, float swing, float amount, float partial) {
            up = windUp(e, NezhaEntity.WIND_UP, partial);
        }

        @Override
        public void setupAnim(NezhaEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            // hovering on his wheels, bobbing; leaning into his way as he goes
            root.y = Mth.sin(age * 0.1f) * 0.6f - up * 0.8f;
            root.xRot = amount * 0.18f;
            turn(body, Mth.sin(age * 0.05f) * 0.02f - amount * 0.05f, 0, 0);
            turn(rightLeg, -amount * 0.15f, 0, 0);
            turn(leftLeg, amount * 0.1f, 0, 0);
            // the wheels spin, faster as he goes and in his wind-up; their flames flicker and stream back
            float spin = age * (0.25f + up * 0.9f) + swing * 0.9f;
            rightSpin.xRot = -spin;
            leftSpin.xRot = -spin;
            float flare = 1 + up * 0.8f + amount * 0.35f;
            for (int k = 0; k < flames.size(); k++) {
                ModelPart f = flames.get(k);
                f.yScale = flare * (1 + Mth.sin(age * 0.55f + k * 1.9f) * 0.22f);
                f.xScale = f.zScale = 1 + up * 0.3f;
                turn(f, -amount * 0.35f - up * 0.2f + Mth.sin(age * 0.3f + k) * 0.06f, 0, 0);
            }
            // the spear: held at his side, thrust in a blow, raised in his wind-up
            turn(rightArm, -strike * 1.2f - up * 2.0f, 0, up * 0.2f);
            turn(rightForearm, strike * 0.6f + up * 0.6f, 0, 0);
            turn(leftArm, -up * 0.5f + Mth.sin(age * 0.06f) * 0.03f, 0, -up * 0.2f);
            turn(leftForearm, 0, 0, 0);
            turn(spear, up * 1.5f, 0, 0);
            spearFire.yScale = 1 + up * 0.5f + Mth.sin(age * 0.5f) * 0.1f;
            spearFire.yRot = age * 0.08f;
            // the Universe Ring turning about his arm
            turn(ring, 0, age * 0.04f, 0);
            // the lotus petals stirring; the sash's loop rippling, its ends and his ribbons streaming
            for (int k = 0; k < petals.size(); k++) turn(petals.get(k), Mth.sin(age * 0.08f + k) * 0.04f - amount * 0.1f, 0, 0);
            turn(sash, amount * 0.25f + Mth.sin(age * 0.05f) * 0.04f, 0, 0);
            for (int k = 0; k < arc.size(); k++) move(arc.get(k), 0, 0, Mth.sin(age * 0.12f + k * 0.6f) * 0.5f);
            float gust = 1.6f + amount * 1.5f + up * 1.5f;
            for (int k = 0; k < streamers.size(); k++) flutter(streamers.get(k), age * 1.4f, amount * 1.5f, k * 1.1f, k % 2 == 0 ? 1 : -1, gust);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    static final class NezhaRenderer extends GreatRenderer<NezhaEntity, NezhaModel> {
        NezhaRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new NezhaModel(ctx.bakeLayer(NEZHA_LAYER)), 0.45f, NEZHA_SIZE, "heaven_nezha");
        }
    }

    // ── Erlang Shen ─────────────────────────────────────────────────────

    /**
     * Erlang Shen (256x256; heaven_great_erlang.py): a handsome young god in silver scale armour over a
     * pale-gold robe, a white cape lined in sky blue, the three-mountain crown, the third eye upright in his
     * brow; the three-pointed, two-edged blade in his right hand. His hound Xiaotian is drawn at his heel.
     */
    public static LayerDefinition erlangLayer() {
        // GENERATED erlang: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(54, 41).mirror(false).addBox(-5.2f, -2, -3.6f, 10.4f, 4, 7.2f)
                .texOffs(93, 41).mirror(false).addBox(-6, -3.6f, -4.2f, 12, 3, 8.4f)
                .texOffs(63, 78).mirror(false).addBox(-2, -4.2f, -4.9f, 4, 3.6f, 1), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(80, 0).mirror(false).addBox(-2.6f, 0, -0.5f, 5.2f, 15, 0.7f), PartPose.offsetAndRotation(0, 0.2f, -4.4f, -0.05f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(215, 41).mirror(false).addBox(-3, 0, -0.5f, 6, 9.5f, 1), PartPose.offsetAndRotation(-3.2f, -0.4f, -3.8f, -0.12f, 0.15f, 0.1f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(215, 41).mirror(true).addBox(-3, 0, -0.5f, 6, 9.5f, 1), PartPose.offsetAndRotation(3.2f, -0.4f, -3.8f, -0.12f, -0.15f, -0.1f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(46, 0).mirror(false).addBox(-1, 0, -3.6f, 1, 9, 7.2f), PartPose.offsetAndRotation(-5.7f, -0.4f, 0, 0, 0, 0.2f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(46, 0).mirror(true).addBox(0, 0, -3.6f, 1, 9, 7.2f), PartPose.offsetAndRotation(5.7f, -0.4f, 0, 0, 0, -0.2f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(230, 41).mirror(false).addBox(-5.4f, 0, -0.5f, 10.8f, 9.5f, 1), PartPose.offsetAndRotation(0, -0.4f, 4, 0.12f, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 41).mirror(false).addBox(-2.4f, 0, -2.4f, 4.8f, 8, 4.8f), PartPose.offsetAndRotation(-2.8f, 8, 0.2f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 41).mirror(true).addBox(-2.4f, 0, -2.4f, 4.8f, 8, 4.8f), PartPose.offsetAndRotation(2.8f, 8, 0.2f, 0, 0, 0));
        PartDefinition rightRobe = rightLeg.addOrReplaceChild("right_robe", CubeListBuilder.create()
                .texOffs(171, 0).mirror(false).addBox(-2.7f, 0, -0.5f, 5.4f, 12.5f, 0.6f), PartPose.offsetAndRotation(0, -0.6f, -2.6f, -0.05f, 0, 0.04f));
        PartDefinition leftRobe = leftLeg.addOrReplaceChild("left_robe", CubeListBuilder.create()
                .texOffs(171, 0).mirror(true).addBox(-2.7f, 0, -0.5f, 5.4f, 12.5f, 0.6f), PartPose.offsetAndRotation(0, -0.6f, -2.6f, -0.05f, 0, -0.04f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(110, 55).mirror(false).addBox(-2.1f, 0, -2.1f, 4.2f, 5, 4.2f)
                .texOffs(90, 67).mirror(false).addBox(-2.4f, 0.4f, -2.6f, 4.8f, 4.6f, 2.6f)
                .texOffs(131, 55).mirror(false).addBox(-2.4f, 4.8f, -2.8f, 4.8f, 3.2f, 5.4f)
                .texOffs(88, 78).mirror(false).addBox(-1.8f, 6, -4.3f, 3.6f, 2, 1.6f), PartPose.offsetAndRotation(0, 8, 0, 0, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(110, 55).mirror(true).addBox(-2.1f, 0, -2.1f, 4.2f, 5, 4.2f)
                .texOffs(90, 67).mirror(true).addBox(-2.4f, 0.4f, -2.6f, 4.8f, 4.6f, 2.6f)
                .texOffs(131, 55).mirror(true).addBox(-2.4f, 4.8f, -2.8f, 4.8f, 3.2f, 5.4f)
                .texOffs(88, 78).mirror(true).addBox(-1.8f, 6, -4.3f, 3.6f, 2, 1.6f), PartPose.offsetAndRotation(0, 8, 0, 0, 0, 0));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-5.6f, -13, -3.8f, 11.2f, 13, 7.6f)
                .texOffs(95, 0).mirror(false).addBox(-6.1f, -13.4f, -4.3f, 12.2f, 6.8f, 8.6f)
                .texOffs(183, 67).mirror(false).addBox(-2.6f, -12.4f, -4.9f, 5.2f, 5.2f, 0.7f)
                .texOffs(107, 67).mirror(false).addBox(-3.6f, -14.6f, -3, 7.2f, 2, 6)
                .texOffs(136, 41).mirror(false).addBox(-6.8f, -13.8f, -4.6f, 13.6f, 1.1f, 9.2f)
                .texOffs(101, 78).mirror(false).addBox(-6.4f, -13.6f, -4.8f, 2.2f, 2.2f, 0.7f)
                .texOffs(101, 78).mirror(false).addBox(4.2f, -13.6f, -4.8f, 2.2f, 2.2f, 0.7f), PartPose.offsetAndRotation(0, -3, 0, 0, 0, 0));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(21, 41).mirror(false).addBox(-7.4f, 0, -0.4f, 14.8f, 12, 0.8f), PartPose.offsetAndRotation(0, -13.6f, 4.2f, 0.1f, 0, 0));
        PartDefinition capeLower = cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(0, 55).mirror(false).addBox(-8, 0, -0.4f, 16, 10, 0.8f), PartPose.offsetAndRotation(0, 11.8f, 0, 0.06f, 0, 0));
        PartDefinition capeHem = capeLower.addOrReplaceChild("cape_hem", CubeListBuilder.create()
                .texOffs(198, 67).mirror(false).addBox(-8.6f, 0, -0.4f, 17.2f, 6, 0.8f), PartPose.offsetAndRotation(0, 9.8f, 0, 0.06f, 0, 0));
        PartDefinition rightCapeWrap = cape.addOrReplaceChild("right_cape_wrap", CubeListBuilder.create()
                .texOffs(65, 0).mirror(false).addBox(-0.4f, 0, -5.6f, 0.8f, 11, 5.6f), PartPose.offsetAndRotation(-7.2f, 0.4f, -0.2f, 0, 0.6f, 0.12f));
        PartDefinition leftCapeWrap = cape.addOrReplaceChild("left_cape_wrap", CubeListBuilder.create()
                .texOffs(65, 0).mirror(true).addBox(-0.4f, 0, -5.6f, 0.8f, 11, 5.6f), PartPose.offsetAndRotation(7.2f, 0.4f, -0.2f, 0, -0.6f, -0.12f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(140, 0).mirror(false).addBox(-3.6f, -8.4f, -3.5f, 7.2f, 7.6f, 7)
                .texOffs(21, 67).mirror(false).addBox(-3.2f, -1.4f, -3.2f, 6.4f, 1.8f, 6.2f)
                .texOffs(110, 78).mirror(false).addBox(-0.8f, -5.4f, -4.2f, 1.6f, 2.6f, 0.8f)
                .texOffs(186, 0).mirror(false).addBox(-3.9f, -8.7f, -0.6f, 7.8f, 8.6f, 4.8f)
                .texOffs(74, 78).mirror(false).addBox(-0.75f, -8, -3.85f, 1.5f, 3.2f, 0.45f), PartPose.offsetAndRotation(0, -14.4f, -0.2f, 0, 0, 0));
        PartDefinition thirdFlare = head.addOrReplaceChild("third_flare", CubeListBuilder.create()
                .texOffs(213, 0).mirror(false).addBox(-4.5f, -6.5f, 0, 9, 13, 0.01f), PartPose.offsetAndRotation(0, -6.4f, -4.3f, 0, 0, 0));
        PartDefinition thirdLid = head.addOrReplaceChild("third_lid", CubeListBuilder.create()
                .texOffs(144, 78).mirror(false).addBox(-0.9f, 0, -0.25f, 1.8f, 1.3f, 0.3f), PartPose.offsetAndRotation(0, -8.1f, -4, 0, 0, 0));
        PartDefinition thirdLidLow = head.addOrReplaceChild("third_lid_low", CubeListBuilder.create()
                .texOffs(156, 78).mirror(false).addBox(-0.9f, -1, -0.25f, 1.8f, 1, 0.3f), PartPose.offsetAndRotation(0, -4.7f, -4, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(163, 78).mirror(false).addBox(-2.8f, -0.45f, -0.3f, 2.8f, 0.7f, 0.6f), PartPose.offsetAndRotation(-0.6f, -6.1f, -3.7f, 0, 0.1f, 0.22f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(163, 78).mirror(true).addBox(0, -0.45f, -0.3f, 2.8f, 0.7f, 0.6f), PartPose.offsetAndRotation(0.6f, -6.1f, -3.7f, 0, -0.1f, -0.22f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(34, 78).mirror(false).addBox(-0.7f, -1.4f, -0.9f, 0.7f, 3.6f, 1.8f), PartPose.offsetAndRotation(-3.7f, -5.2f, -0.2f, 0, 0, 0.06f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(34, 78).mirror(true).addBox(0, -1.4f, -0.9f, 0.7f, 3.6f, 1.8f), PartPose.offsetAndRotation(3.7f, -5.2f, -0.2f, 0, 0, -0.06f));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(154, 55).mirror(false).addBox(-4, -1.6f, -3.9f, 8, 1.9f, 7.8f)
                .texOffs(35, 55).mirror(false).addBox(-3.4f, -4.6f, -3.4f, 6.8f, 3.2f, 6.8f)
                .texOffs(50, 67).mirror(false).addBox(-2.2f, -8.4f, -4.6f, 4.4f, 7.6f, 0.7f)
                .texOffs(117, 78).mirror(false).addBox(-0.9f, -5.6f, -0.9f, 1.8f, 1.4f, 1.8f), PartPose.offsetAndRotation(0, -8.2f, 0, 0, 0, 0));
        PartDefinition rightCrownPeak = crown.addOrReplaceChild("right_crown_peak", CubeListBuilder.create()
                .texOffs(237, 67).mirror(false).addBox(-1.7f, -6, -0.35f, 3.4f, 6, 0.7f), PartPose.offsetAndRotation(-2.8f, -1.2f, -3.2f, 0, 0.55f, -0.22f));
        PartDefinition leftCrownPeak = crown.addOrReplaceChild("left_crown_peak", CubeListBuilder.create()
                .texOffs(237, 67).mirror(true).addBox(-1.7f, -6, -0.35f, 3.4f, 6, 0.7f), PartPose.offsetAndRotation(2.8f, -1.2f, -3.2f, 0, -0.55f, 0.22f));
        PartDefinition rightCrownWing = crown.addOrReplaceChild("right_crown_wing", CubeListBuilder.create()
                .texOffs(64, 55).mirror(false).addBox(-0.35f, -5.4f, -0.4f, 0.7f, 5.4f, 4.4f), PartPose.offsetAndRotation(-3.9f, -2.2f, 1, 0.1f, 0.25f, -0.5f));
        PartDefinition leftCrownWing = crown.addOrReplaceChild("left_crown_wing", CubeListBuilder.create()
                .texOffs(64, 55).mirror(true).addBox(-0.35f, -5.4f, -0.4f, 0.7f, 5.4f, 4.4f), PartPose.offsetAndRotation(3.9f, -2.2f, 1, 0.1f, -0.25f, 0.5f));
        PartDefinition rightCrownTie = crown.addOrReplaceChild("right_crown_tie", CubeListBuilder.create()
                .texOffs(41, 78).mirror(false).addBox(-0.6f, 0, -0.15f, 1.2f, 4.6f, 0.3f), PartPose.offsetAndRotation(-3.6f, -0.6f, 3, 0.3f, 0, 0.25f));
        PartDefinition leftCrownTie = crown.addOrReplaceChild("left_crown_tie", CubeListBuilder.create()
                .texOffs(41, 78).mirror(true).addBox(-0.6f, 0, -0.15f, 1.2f, 4.6f, 0.3f), PartPose.offsetAndRotation(3.6f, -0.6f, 3, 0.3f, 0, -0.25f));
        PartDefinition rightCrownTie2 = rightCrownTie.addOrReplaceChild("right_crown_tie_2", CubeListBuilder.create()
                .texOffs(41, 78).mirror(false).addBox(-0.6f, 0, -0.15f, 1.2f, 4.6f, 0.3f), PartPose.offsetAndRotation(0, 4.2f, 0, 0.1f, 0, 0));
        PartDefinition leftCrownTie2 = leftCrownTie.addOrReplaceChild("left_crown_tie_2", CubeListBuilder.create()
                .texOffs(41, 78).mirror(true).addBox(-0.6f, 0, -0.15f, 1.2f, 4.6f, 0.3f), PartPose.offsetAndRotation(0, 4.2f, 0, 0.1f, 0, 0));
        PartDefinition rightCrownTie3 = rightCrownTie2.addOrReplaceChild("right_crown_tie_3", CubeListBuilder.create()
                .texOffs(81, 78).mirror(false).addBox(-0.75f, 0, -0.15f, 1.5f, 4, 0.3f), PartPose.offsetAndRotation(0, 4.2f, 0, 0.14f, 0, 0));
        PartDefinition leftCrownTie3 = leftCrownTie2.addOrReplaceChild("left_crown_tie_3", CubeListBuilder.create()
                .texOffs(81, 78).mirror(true).addBox(-0.75f, 0, -0.15f, 1.5f, 4, 0.3f), PartPose.offsetAndRotation(0, 4.2f, 0, 0.14f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(185, 41).mirror(false).addBox(-2.1f, -1, -2.1f, 4.2f, 7, 4.2f)
                .texOffs(0, 78).mirror(false).addBox(-2.5f, 3.6f, -2.5f, 5, 1.1f, 5), PartPose.offsetAndRotation(-7.4f, -11.8f, 0.2f, -0.08f, 0, 0.12f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(185, 41).mirror(true).addBox(-2.1f, -1, -2.1f, 4.2f, 7, 4.2f)
                .texOffs(0, 78).mirror(true).addBox(-2.5f, 3.6f, -2.5f, 5, 1.1f, 5), PartPose.offsetAndRotation(7.4f, -11.8f, 0.2f, 0.05f, 0, -0.1f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(63, 67).mirror(false).addBox(-3.8f, -1.3f, -3.4f, 5.8f, 1.5f, 6.8f)
                .texOffs(187, 55).mirror(false).addBox(-4.4f, 0, -3.8f, 6.4f, 1.7f, 7.6f)
                .texOffs(77, 55).mirror(false).addBox(-5, 1.5f, -4.2f, 6.9f, 2, 8.4f)
                .texOffs(234, 0).mirror(false).addBox(-5.8f, -1.4f, -3.4f, 0.8f, 6.8f, 6.8f)
                .texOffs(218, 55).mirror(false).addBox(-6.5f, -0.3f, -2.4f, 1.3f, 4.8f, 4.8f), PartPose.offsetAndRotation(-0.3f, -0.7f, 0, 0, 0, 0.4f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(63, 67).mirror(true).addBox(-2, -1.3f, -3.4f, 5.8f, 1.5f, 6.8f)
                .texOffs(187, 55).mirror(true).addBox(-2, 0, -3.8f, 6.4f, 1.7f, 7.6f)
                .texOffs(77, 55).mirror(true).addBox(-1.9f, 1.5f, -4.2f, 6.9f, 2, 8.4f)
                .texOffs(234, 0).mirror(true).addBox(5, -1.4f, -3.4f, 0.8f, 6.8f, 6.8f)
                .texOffs(218, 55).mirror(true).addBox(5.2f, -0.3f, -2.4f, 1.3f, 4.8f, 4.8f), PartPose.offsetAndRotation(0.3f, -0.7f, 0, 0, 0, -0.4f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(233, 55).mirror(false).addBox(-1.9f, 0, -1.9f, 3.8f, 5.6f, 3.8f)
                .texOffs(0, 67).mirror(false).addBox(-2.25f, 0.7f, -2.25f, 4.5f, 4.2f, 4.5f)
                .texOffs(136, 67).mirror(false).addBox(-2.9f, -1.1f, -2.9f, 5.8f, 1.8f, 5.8f), PartPose.offsetAndRotation(0, 6.2f, 0, -0.8f, -0.25f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(233, 55).mirror(true).addBox(-1.9f, 0, -1.9f, 3.8f, 5.6f, 3.8f)
                .texOffs(0, 67).mirror(true).addBox(-2.25f, 0.7f, -2.25f, 4.5f, 4.2f, 4.5f)
                .texOffs(136, 67).mirror(true).addBox(-2.9f, -1.1f, -2.9f, 5.8f, 1.8f, 5.8f), PartPose.offsetAndRotation(0, 6.2f, 0, -0.35f, 0.2f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(161, 67).mirror(false).addBox(-1.6f, 0, -1.6f, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(0, 5.6f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(161, 67).mirror(true).addBox(-1.6f, 0, -1.6f, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(0, 5.6f, 0, 0, 0, 0));
        PartDefinition blade = rightHand.addOrReplaceChild("blade", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5f, -24, -0.5f, 1, 39, 1)
                .texOffs(126, 78).mirror(false).addBox(-0.8f, -25.2f, -0.8f, 1.6f, 1.4f, 1.6f)
                .texOffs(48, 78).mirror(false).addBox(-1.6f, -27.4f, -1.2f, 3.2f, 2.4f, 2.4f)
                .texOffs(206, 41).mirror(false).addBox(-1.4f, -37.4f, -0.3f, 2.8f, 10.4f, 0.6f)
                .texOffs(135, 78).mirror(false).addBox(-0.7f, 14.6f, -0.7f, 1.4f, 1.6f, 1.4f), PartPose.offsetAndRotation(0, 1.6f, 0, 0.8383f, 0.2199f, 0.1428f));
        PartDefinition rightBlProng = blade.addOrReplaceChild("right_bl_prong", CubeListBuilder.create()
                .texOffs(178, 67).mirror(false).addBox(-0.5f, -6.4f, -0.3f, 1, 6.6f, 0.6f), PartPose.offsetAndRotation(-1.3f, -26.6f, 0, 0, 0, 0.38f));
        PartDefinition leftBlProng = blade.addOrReplaceChild("left_bl_prong", CubeListBuilder.create()
                .texOffs(178, 67).mirror(true).addBox(-0.5f, -6.4f, -0.3f, 1, 6.6f, 0.6f), PartPose.offsetAndRotation(1.3f, -26.6f, 0, 0, 0, -0.38f));
        PartDefinition bladeTassel = blade.addOrReplaceChild("blade_tassel", CubeListBuilder.create()
                .texOffs(151, 78).mirror(false).addBox(-0.45f, 0, -0.45f, 0.9f, 1.4f, 0.9f), PartPose.offsetAndRotation(0, -24.6f, 0, 0, 0, 0));
        PartDefinition bladeTassel2 = bladeTassel.addOrReplaceChild("blade_tassel_2", CubeListBuilder.create()
                .texOffs(21, 78).mirror(false).addBox(-1.1f, 0, -1.1f, 2.2f, 3.6f, 2.2f), PartPose.offsetAndRotation(0, 1, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED erlang: end
    }

    /**
     * His pose: the blade upright at his side; striking, it swings down; winding up, two fingers lifted to
     * his brow and the third eye opening wide, blazing.
     */
    public static final class ErlangModel extends EntityModel<ErlangShenEntity> {
        private final ModelPart root, waist, body, head, rightArm, leftArm, rightForearm, leftForearm, rightLeg, leftLeg,
                rightShin, leftShin, apron, rightTasset, leftTasset, backTasset, cape, capeLower, capeHem, lid, lidLow, flare;
        private final List<List<ModelPart>> streamers = new ArrayList<>();
        private float up;

        public ErlangModel(ModelPart root) {
            this.root = root;
            waist = root.getChild("waist");
            body = waist.getChild("body");
            head = body.getChild("head");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            apron = waist.getChild("apron");
            rightTasset = waist.getChild("right_tasset");
            leftTasset = waist.getChild("left_tasset");
            backTasset = waist.getChild("back_tasset");
            cape = body.getChild("cape");
            capeLower = cape.getChild("cape_lower");
            capeHem = capeLower.getChild("cape_hem");
            lid = head.getChild("third_lid");
            lidLow = head.getChild("third_lid_low");
            flare = head.getChild("third_flare");
            ModelPart crown = head.getChild("crown");
            streamers.add(chain(crown, "right_crown_tie"));
            streamers.add(chain(crown, "left_crown_tie"));
            streamers.add(chain(rightForearm.getChild("right_hand").getChild("blade"), "blade_tassel"));
        }

        @Override
        public void prepareMobModel(ErlangShenEntity e, float swing, float amount, float partial) {
            up = windUp(e, ErlangShenEntity.WIND_UP, partial);
            flare.visible = up > 0;
        }

        @Override
        public void setupAnim(ErlangShenEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float walk = swing * 0.6f, step = Mth.cos(walk) * amount;
            float breathe = Mth.sin(age * 0.06f);
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD - up * 0.15f;
            turn(rightLeg, step * 0.65f, 0, 0);
            turn(leftLeg, -step * 0.65f, 0, 0);
            turn(rightShin, Math.max(0, Mth.sin(walk)) * 0.6f * amount, 0, 0);
            turn(leftShin, Math.max(0, -Mth.sin(walk)) * 0.6f * amount, 0, 0);
            move(waist, 0, -Math.abs(Mth.cos(walk)) * 0.6f * amount + breathe * 0.1f, 0);
            turn(body, breathe * 0.012f, step * 0.05f, 0);
            turn(rightTasset, Math.min(0, rightLeg.xRot) * 0.7f, 0, 0);
            turn(leftTasset, Math.min(0, leftLeg.xRot) * 0.7f, 0, 0);
            turn(apron, Math.min(rightLeg.xRot, leftLeg.xRot) * 0.5f, 0, 0);
            turn(backTasset, Math.max(rightLeg.xRot, leftLeg.xRot) * 0.5f, 0, 0);
            turn(cape, amount * 0.5f + Mth.sin(age * 0.07f) * 0.035f + up * 0.12f, 0, 0);
            turn(capeLower, amount * 0.3f + Mth.sin(age * 0.07f + 0.8f) * 0.05f + up * 0.1f, 0, 0);
            turn(capeHem, amount * 0.2f + Mth.sin(age * 0.09f + 1.6f) * 0.07f + up * 0.1f, 0, 0);
            // the blade swings down in a blow; in his wind-up two fingers of his left hand go to his brow
            turn(rightArm, -step * 0.2f - strike * 1.5f, 0, 0);
            turn(rightForearm, strike * 0.5f, 0, 0);
            turn(leftArm, step * 0.25f - up * 1.75f, -up * 0.55f, up * 0.2f);
            turn(leftForearm, -up * 1.3f, up * 0.6f, 0);
            float f = up * (1 + Mth.sin(age * 0.5f) * 0.12f);
            flare.xScale = flare.yScale = Math.max(0.01f, f);
            // the third eye, half shut at rest, opening wide
            float open = Math.max(0.1f, 0.7f - up * 0.6f);
            lid.yScale = open;
            lidLow.yScale = open;
            for (int k = 0; k < streamers.size(); k++) flutter(streamers.get(k), age, amount, k * 1.3f, k % 2 == 0 ? 1 : -1, 1 + up);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    /** Erlang, and his hound Xiaotian at his heel: its own model and skin, trotting when he walks. */
    static final class ErlangRenderer extends GreatRenderer<ErlangShenEntity, ErlangModel> {
        private static final ResourceLocation HOUND = tex("xiaotian_quan");
        private final XiaotianQuanModel hound;
        private final Vector3f cache = new Vector3f();

        ErlangRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ErlangModel(ctx.bakeLayer(ERLANG_LAYER)), 0.55f, ERLANG_SIZE, "heaven_erlang_shen");
            hound = new XiaotianQuanModel(ctx.bakeLayer(XiaotianQuanModel.LAYER_LOCATION));
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, ErlangShenEntity e,
                                   float swing, float amount, float partial, float age, float yaw, float pitch) {
                    if (e.isInvisible()) return;
                    hound.root().getAllParts().forEach(ModelPart::resetPose);
                    if (amount > 0.05f) {
                        KeyframeAnimations.animate(hound, XiaotianQuanAnimations.WALK, (long) (swing * 90f), 1f, cache);
                    } else if (hound.root().hasChild("head")) {
                        // at rest it looks about, its head lifting now and then
                        ModelPart h = hound.root().getChild("head");
                        h.yRot = Mth.sin(age * 0.03f) * 0.35f;
                        h.xRot = Math.min(0, Mth.sin(age * 0.05f)) * 0.15f;
                    }
                    pose.pushPose();
                    // at his left heel, a little ahead, drawn at its own size whatever his
                    float s = 1f / ERLANG_SIZE;
                    pose.translate(0.85f * s, 1.501f, -0.15f * s);
                    pose.scale(s, s, s);
                    pose.translate(0, -1.501f, 0);
                    hound.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(HOUND)), light,
                            LivingEntityRenderer.getOverlayCoords(e, 0f), -1);
                    pose.popPose();
                }
            });
        }
    }
}
