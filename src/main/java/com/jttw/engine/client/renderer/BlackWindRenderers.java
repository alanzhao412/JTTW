package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.blackwind.BearSoldierEntity;
import com.jttw.engine.entity.blackwind.BlackBearDemonEntity;
import com.jttw.engine.entity.blackwind.BoarDemonEntity;
import com.jttw.engine.entity.blackwind.LingxuziEntity;
import com.jttw.engine.entity.blackwind.WhiteRobedScholarEntity;
import com.jttw.engine.entity.blackwind.WindImpEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.BlackWindRegistry;
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
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
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
 * How Black Wind Mountain's creatures look: the Black Bear Demon (a towering black bear in black
 * lamellae edged in gold, a gold diadem, a black war cloak, the stolen brocade kasaya thrown across his
 * breast, his black-tasselled spear), his Black Bear Soldiers (red-laced, horned helms, a shield on the
 * back, a spiked mace), the Boar Demons (a boar's head, bronze scales, a broadaxe), the Black-Wind Imps
 * (fan-eared, ragged black scarves streaming), Lingxuzi (an old grey wolf as a Daoist, sword and whisk)
 * and the White-Robed Scholar (a scholar in white, a serpent's eyes and tongue, his white tail behind).
 * Each one's model and skin come from {@code assets_source/black_bear.py} and
 * {@code black_wind_folk.py}, the model written between its GENERATED markers here; eyes (and the
 * king's gold) glow full-bright ({@code <name>_glow.png}); a captain wears violet ({@code <name>_captain.png}).
 * Their attacks are posed: the black wind gathering (arms spread, roaring), rearing for the maul, the
 * gale drawn back; the boar pawing and charging; the imp spinning up and riding its gust; the wolf
 * crouching, leaping, drinking his elixir; the snake rearing to spit and coiling to lash.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BlackWindRenderers {

    private BlackWindRenderers() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(JTTWEngine.id(name), "main");
    }

    static ResourceLocation tex(String name) {
        return JTTWEngine.id("textures/entity/" + name + ".png");
    }

    public static final ModelLayerLocation KING_LAYER = layer("black_bear_demon"), SOLDIER_LAYER = layer("bear_soldier"),
            BOAR_LAYER = layer("boar_demon"), IMP_LAYER = layer("wind_imp"), WOLF_LAYER = layer("lingxuzi"),
            SCHOLAR_LAYER = layer("white_robed_scholar");

    /** How much larger than their pixels each is drawn (the generators print their reach). */
    public static final float KING_SIZE = 1.0f, SOLDIER_SIZE = 0.78f, BOAR_SIZE = 0.85f, IMP_SIZE = 0.85f, WOLF_SIZE = 0.9f, SCHOLAR_SIZE = 0.92f;

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KING_LAYER, BlackWindRenderers::kingLayer);
        event.registerLayerDefinition(SOLDIER_LAYER, BlackWindRenderers::soldierLayer);
        event.registerLayerDefinition(BOAR_LAYER, BlackWindRenderers::boarLayer);
        event.registerLayerDefinition(IMP_LAYER, BlackWindRenderers::impLayer);
        event.registerLayerDefinition(WOLF_LAYER, BlackWindRenderers::wolfLayer);
        event.registerLayerDefinition(SCHOLAR_LAYER, BlackWindRenderers::scholarLayer);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BlackWindRegistry.BLACK_BEAR_DEMON.get(),
                ctx -> new Renderer<>(ctx, new BearModel<>(ctx.bakeLayer(KING_LAYER)), 1.4f, KING_SIZE, e -> "black_bear_demon"));
        event.registerEntityRenderer(BlackWindRegistry.BEAR_SOLDIER.get(),
                ctx -> new Renderer<>(ctx, new BearModel<>(ctx.bakeLayer(SOLDIER_LAYER)), 1.0f, SOLDIER_SIZE, e -> captained("bear_soldier", e)));
        event.registerEntityRenderer(BlackWindRegistry.BOAR_DEMON.get(),
                ctx -> new Renderer<>(ctx, new BoarModel(ctx.bakeLayer(BOAR_LAYER)), 0.7f, BOAR_SIZE, e -> captained("boar_demon", e)));
        event.registerEntityRenderer(BlackWindRegistry.WIND_IMP.get(),
                ctx -> new Renderer<>(ctx, new ImpModel(ctx.bakeLayer(IMP_LAYER)), 0.45f, IMP_SIZE, e -> captained("wind_imp", e)));
        event.registerEntityRenderer(BlackWindRegistry.LINGXUZI.get(),
                ctx -> new Renderer<>(ctx, new WolfModel(ctx.bakeLayer(WOLF_LAYER)), 0.6f, WOLF_SIZE, e -> "lingxuzi"));
        event.registerEntityRenderer(BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(),
                ctx -> new Renderer<>(ctx, new ScholarModel(ctx.bakeLayer(SCHOLAR_LAYER)), 0.6f, SCHOLAR_SIZE, e -> "white_robed_scholar"));
        event.registerEntityRenderer(BlackWindRegistry.BLACK_WHIRLWIND.get(), NoopRenderer::new);
        event.registerEntityRenderer(BlackWindRegistry.VENOM_BOLT.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.8f, true));
    }

    private static String captained(String name, Mob e) {
        return e instanceof ZoneMonster z && z.isCaptain() ? name + "_captain" : name;
    }

    /** A part turned this much from its rest. */
    static void turn(ModelPart p, float x, float y, float z) {
        p.xRot = p.getInitialPose().xRot + x;
        p.yRot = p.getInitialPose().yRot + y;
        p.zRot = p.getInitialPose().zRot + z;
    }

    static ModelPart child(ModelPart p, String name) {
        return p != null && p.hasChild(name) ? p.getChild(name) : null;
    }

    static void turnIf(ModelPart p, float x, float y, float z) {
        if (p != null) turn(p, x, y, z);
    }

    /** A creature's renderer: its skin (a captain's), its glow over it full-bright, drawn at its size; its name only near. */
    public static final class Renderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
        private final float size;
        private final Function<T, String> skin;

        public Renderer(EntityRendererProvider.Context ctx, M model, float shadow, float size, Function<T, String> skin) {
            super(ctx, model, shadow);
            this.size = size;
            this.skin = skin;
            addLayer(LionCourt.glow(this, skin));
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

    // ── The Black Bear Demon and his soldiers ───────────────────────────

    /** The Black Bear Demon (256x256): model and skin from {@code assets_source/black_bear.py}. */
    public static LayerDefinition kingLayer() {
        // GENERATED king: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(214, 107).mirror(false).addBox(-5, -1, -5, 10, 11.5f, 10), PartPose.offsetAndRotation(-6.5f, 2.147f, 1.5f, -0.2f, 0, 0.05f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(214, 107).mirror(true).addBox(-5, -1, -5, 10, 11.5f, 10), PartPose.offsetAndRotation(6.5f, 2.147f, 1.5f, -0.2f, 0, -0.05f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(174, 132).mirror(false).addBox(-4.2f, 0, -4.2f, 8.4f, 8.6f, 8.4f)
                .texOffs(245, 172).mirror(false).addBox(-4.7f, 0.4f, -4.9f, 9.4f, 6.4f, 3.4f)
                .texOffs(306, 198).mirror(false).addBox(-4.5f, -1.6f, -5.3f, 9, 2.8f, 1.6f), PartPose.offsetAndRotation(0, 10, 0.4f, 0.3f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(174, 132).mirror(true).addBox(-4.2f, 0, -4.2f, 8.4f, 8.6f, 8.4f)
                .texOffs(245, 172).mirror(true).addBox(-4.7f, 0.4f, -4.9f, 9.4f, 6.4f, 3.4f)
                .texOffs(306, 198).mirror(true).addBox(-4.5f, -1.6f, -5.3f, 9, 2.8f, 1.6f), PartPose.offsetAndRotation(0, 10, 0.4f, 0.3f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(290, 154).mirror(false).addBox(-4.8f, 0, -6.8f, 9.6f, 3.8f, 10.4f)
                .texOffs(329, 198).mirror(false).addBox(-4.6f, 1.2f, -8.3f, 1.7f, 2.2f, 2)
                .texOffs(222, 198).mirror(false).addBox(-2.3f, 1.2f, -8.6f, 1.7f, 2.2f, 2.2f)
                .texOffs(222, 198).mirror(false).addBox(0.6f, 1.2f, -8.6f, 1.7f, 2.2f, 2.2f)
                .texOffs(329, 198).mirror(false).addBox(2.9f, 1.2f, -8.3f, 1.7f, 2.2f, 2), PartPose.offsetAndRotation(0, 8, 0, -0.1f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(290, 154).mirror(true).addBox(-4.8f, 0, -6.8f, 9.6f, 3.8f, 10.4f)
                .texOffs(329, 198).mirror(true).addBox(2.9f, 1.2f, -8.3f, 1.7f, 2.2f, 2)
                .texOffs(222, 198).mirror(true).addBox(0.6f, 1.2f, -8.6f, 1.7f, 2.2f, 2.2f)
                .texOffs(222, 198).mirror(true).addBox(-2.3f, 1.2f, -8.6f, 1.7f, 2.2f, 2.2f)
                .texOffs(329, 198).mirror(true).addBox(-4.6f, 1.2f, -8.3f, 1.7f, 2.2f, 2), PartPose.offsetAndRotation(0, 8, 0, -0.1f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(242, 0).mirror(false).addBox(-12, -12, -8.8f, 24, 13, 17.6f)
                .texOffs(56, 0).mirror(false).addBox(-13, -27, -9, 26, 16, 18)
                .texOffs(145, 0).mirror(false).addBox(-13.7f, -27.6f, -9.8f, 27.4f, 13.4f, 19.6f)
                .texOffs(122, 77).mirror(false).addBox(-12.8f, -14.4f, -9.5f, 25.6f, 8, 19)
                .texOffs(0, 107).mirror(false).addBox(-13, -6.6f, -9.7f, 26, 3.6f, 19.4f)
                .texOffs(113, 187).mirror(false).addBox(-3.4f, -7.8f, -10.8f, 6.8f, 6, 1.4f)
                .texOffs(210, 187).mirror(false).addBox(-12, -26.4f, -10.6f, 6, 6, 0.8f)
                .texOffs(210, 187).mirror(false).addBox(6, -26.4f, -10.6f, 6, 6, 0.8f)
                .texOffs(255, 107).mirror(false).addBox(-15.2f, -35, -4, 30.4f, 9.5f, 12)
                .texOffs(295, 132).mirror(false).addBox(-11.5f, -34, -8.5f, 23, 8, 9)
                .texOffs(338, 198).mirror(false).addBox(-7, -30.4f, -9.3f, 14, 3.8f, 1), PartPose.offsetAndRotation(0, 2.147f, 1.5f, 0.24f, 0, 0));
        PartDefinition rightTasset = body.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(75, 172).mirror(false).addBox(-5.9f, 0, -0.6f, 11.8f, 11, 1.2f), PartPose.offsetAndRotation(-6.5f, -3.4f, -9.7f, -0.12f, 0.05f, 0.05f));
        PartDefinition leftTasset = body.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(75, 172).mirror(true).addBox(-5.9f, 0, -0.6f, 11.8f, 11, 1.2f), PartPose.offsetAndRotation(6.5f, -3.4f, -9.7f, -0.12f, -0.05f, -0.05f));
        PartDefinition rightSideTasset = body.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(213, 77).mirror(false).addBox(-1.2f, 0, -8.2f, 1.2f, 10, 16.4f), PartPose.offsetAndRotation(-12.8f, -3.4f, 0, 0, 0, 0.18f));
        PartDefinition leftSideTasset = body.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(213, 77).mirror(true).addBox(0, 0, -8.2f, 1.2f, 10, 16.4f), PartPose.offsetAndRotation(12.8f, -3.4f, 0, 0, 0, -0.18f));
        PartDefinition backTasset = body.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(0, 172).mirror(false).addBox(-12, 0, -0.6f, 24, 11.5f, 1.2f), PartPose.offsetAndRotation(0, -3.4f, 9.5f, 0.14f, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 187).mirror(false).addBox(-2.6f, -2.6f, 0, 5.2f, 5.2f, 3.8f), PartPose.offsetAndRotation(0, -3, 8.6f, -0.6f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(93, 107).mirror(false).addBox(-5.6f, -2.4f, -5, 10.4f, 13.5f, 10), PartPose.offsetAndRotation(-16.2f, -27.5f, 0, -0.2f, 0, 0.2f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(93, 107).mirror(true).addBox(-4.8f, -2.4f, -5, 10.4f, 13.5f, 10), PartPose.offsetAndRotation(16.2f, -27.5f, 0, -0.2f, 0, -0.2f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(333, 154).mirror(false).addBox(-6, -1.8f, -6, 9.6f, 2.2f, 12)
                .texOffs(0, 154).mirror(false).addBox(-6.8f, 0.4f, -6.6f, 10.4f, 2.4f, 13.2f)
                .texOffs(211, 132).mirror(false).addBox(-7.6f, 2.8f, -7.2f, 11, 2.6f, 14.4f)
                .texOffs(195, 172).mirror(false).addBox(-8.4f, 0, -2.9f, 1.2f, 5.6f, 5.8f), PartPose.offsetAndRotation(-0.8f, -2.6f, 0, 0, 0, 0.42f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(333, 154).mirror(true).addBox(-3.6f, -1.8f, -6, 9.6f, 2.2f, 12)
                .texOffs(0, 154).mirror(true).addBox(-3.6f, 0.4f, -6.6f, 10.4f, 2.4f, 13.2f)
                .texOffs(211, 132).mirror(true).addBox(-3.4f, 2.8f, -7.2f, 11, 2.6f, 14.4f)
                .texOffs(195, 172).mirror(true).addBox(7.2f, 0, -2.9f, 1.2f, 5.6f, 5.8f), PartPose.offsetAndRotation(0.8f, -2.6f, 0, 0, 0, -0.42f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(63, 132).mirror(false).addBox(-4.4f, 0, -4.4f, 8.8f, 10.5f, 8.8f)
                .texOffs(51, 154).mirror(false).addBox(-4.9f, 1.4f, -4.9f, 9.8f, 6.8f, 9.8f)
                .texOffs(21, 187).mirror(false).addBox(-5.4f, -1, -3, 2, 3.4f, 6), PartPose.offsetAndRotation(0, 10.4f, 0, -0.7f, -0.1f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(63, 132).mirror(true).addBox(-4.4f, 0, -4.4f, 8.8f, 10.5f, 8.8f)
                .texOffs(51, 154).mirror(true).addBox(-4.9f, 1.4f, -4.9f, 9.8f, 6.8f, 9.8f)
                .texOffs(21, 187).mirror(true).addBox(3.4f, -1, -3, 2, 3.4f, 6), PartPose.offsetAndRotation(0, 10.4f, 0, -0.7f, 0.1f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(107, 154).mirror(false).addBox(-4.6f, 0, -4.8f, 9.2f, 5.6f, 9.6f)
                .texOffs(329, 198).mirror(false).addBox(-4.6f, 4.8f, -5.2f, 1.8f, 2.8f, 1.8f)
                .texOffs(329, 198).mirror(false).addBox(-2.2f, 4.8f, -5.4f, 1.8f, 3, 1.8f)
                .texOffs(329, 198).mirror(false).addBox(0.4f, 4.8f, -5.4f, 1.8f, 3, 1.8f)
                .texOffs(329, 198).mirror(false).addBox(2.8f, 4.8f, -5.2f, 1.8f, 2.8f, 1.8f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(107, 154).mirror(true).addBox(-4.6f, 0, -4.8f, 9.2f, 5.6f, 9.6f)
                .texOffs(329, 198).mirror(true).addBox(2.8f, 4.8f, -5.2f, 1.8f, 2.8f, 1.8f)
                .texOffs(329, 198).mirror(true).addBox(0.4f, 4.8f, -5.4f, 1.8f, 3, 1.8f)
                .texOffs(329, 198).mirror(true).addBox(-2.2f, 4.8f, -5.4f, 1.8f, 3, 1.8f)
                .texOffs(329, 198).mirror(true).addBox(-4.6f, 4.8f, -5.2f, 1.8f, 2.8f, 1.8f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(136, 107).mirror(false).addBox(-7.6f, -11.4f, -6.8f, 15.2f, 11, 12.6f)
                .texOffs(104, 172).mirror(false).addBox(-6, -13.4f, -5.4f, 12, 2.4f, 9.6f)
                .texOffs(212, 172).mirror(false).addBox(-5.2f, -6.8f, -10.2f, 10.4f, 6.6f, 4.2f)
                .texOffs(38, 187).mirror(false).addBox(-4, -6.2f, -13.6f, 8, 5.4f, 3.8f)
                .texOffs(132, 187).mirror(false).addBox(-3, -5.6f, -16.2f, 6, 4.4f, 3)
                .texOffs(0, 206).mirror(false).addBox(-2.6f, -6.4f, -17.2f, 5.2f, 3, 1.8f)
                .texOffs(151, 187).mirror(false).addBox(-3.4f, -1.6f, -16, 6.8f, 1.6f, 5.8f)
                .texOffs(17, 206).mirror(false).addBox(-3, -1, -15.4f, 1.1f, 2.6f, 1.1f)
                .texOffs(17, 206).mirror(false).addBox(1.9f, -1, -15.4f, 1.1f, 2.6f, 1.1f)
                .texOffs(225, 187).mirror(false).addBox(-7.2f, -14.6f, -4.4f, 14.4f, 2.6f, 4), PartPose.offsetAndRotation(0, -33.5f, -4, -0.12f, 0, 0));
        PartDefinition bridge = head.addOrReplaceChild("bridge", CubeListBuilder.create()
                .texOffs(84, 187).mirror(false).addBox(-3.2f, -1.4f, -6.6f, 6.4f, 1.6f, 6.8f), PartPose.offsetAndRotation(0, -6.6f, -9.4f, -0.18f, 0, 0));
        PartDefinition rightJowl = head.addOrReplaceChild("right_jowl", CubeListBuilder.create()
                .texOffs(148, 154).mirror(false).addBox(-2.6f, -2.8f, -3.6f, 3.4f, 6.8f, 8.6f), PartPose.offsetAndRotation(-7, -4.6f, -4.6f, 0, 0.25f, 0.22f));
        PartDefinition leftJowl = head.addOrReplaceChild("left_jowl", CubeListBuilder.create()
                .texOffs(148, 154).mirror(true).addBox(-0.8f, -2.8f, -3.6f, 3.4f, 6.8f, 8.6f), PartPose.offsetAndRotation(7, -4.6f, -4.6f, 0, -0.25f, -0.22f));
        PartDefinition rightFaceRuff = head.addOrReplaceChild("right_face_ruff", CubeListBuilder.create()
                .texOffs(175, 154).mirror(false).addBox(-3, -4, -1.2f, 3.2f, 9.6f, 5.2f), PartPose.offsetAndRotation(-7.6f, -8.4f, 1.6f, 0, -0.35f, 0.3f));
        PartDefinition leftFaceRuff = head.addOrReplaceChild("left_face_ruff", CubeListBuilder.create()
                .texOffs(175, 154).mirror(true).addBox(-0.2f, -4, -1.2f, 3.2f, 9.6f, 5.2f), PartPose.offsetAndRotation(7.6f, -8.4f, 1.6f, 0, 0.35f, -0.3f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(149, 172).mirror(false).addBox(-4, 0, -9, 8, 2.9f, 9.2f)
                .texOffs(100, 206).mirror(false).addBox(-3.7f, -1, -8.8f, 7.4f, 1, 1.2f)
                .texOffs(80, 206).mirror(false).addBox(-3.2f, -2.4f, -8.4f, 1, 2.4f, 1)
                .texOffs(80, 206).mirror(false).addBox(2.2f, -2.4f, -8.4f, 1, 2.4f, 1), PartPose.offsetAndRotation(0, -0.6f, -6.4f, 0.16f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(178, 187).mirror(false).addBox(-2.8f, -4.4f, -1.3f, 5.6f, 4.6f, 2.6f)
                .texOffs(26, 206).mirror(false).addBox(-2, -5.6f, -1.1f, 4, 1.4f, 2.2f), PartPose.offsetAndRotation(-6, -11.8f, -0.8f, -0.12f, 0.25f, -0.36f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(178, 187).mirror(true).addBox(-2.8f, -4.4f, -1.3f, 5.6f, 4.6f, 2.6f)
                .texOffs(26, 206).mirror(true).addBox(-2, -5.6f, -1.1f, 4, 1.4f, 2.2f), PartPose.offsetAndRotation(6, -11.8f, -0.8f, -0.12f, -0.25f, 0.36f));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(233, 198).mirror(false).addBox(-5.6f, -1.3f, -1.2f, 5.8f, 2.4f, 2.6f), PartPose.offsetAndRotation(-0.6f, -10.2f, -7, 0.1f, 0.15f, 0.3f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(233, 198).mirror(true).addBox(-0.2f, -1.3f, -1.2f, 5.8f, 2.4f, 2.6f), PartPose.offsetAndRotation(0.6f, -10.2f, -7, 0.1f, -0.15f, -0.3f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(121, 206).mirror(false).addBox(-1.3f, -0.7f, -0.5f, 2.6f, 1.5f, 0.7f), PartPose.offsetAndRotation(-3.7f, -8.2f, -6.9f, 0, 0.22f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(121, 206).mirror(true).addBox(-1.3f, -0.7f, -0.5f, 2.6f, 1.5f, 0.7f), PartPose.offsetAndRotation(3.7f, -8.2f, -6.9f, 0, -0.22f, 0));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(196, 154).mirror(false).addBox(-7.6f, -1.4f, -6.8f, 15.2f, 2, 13.2f)
                .texOffs(197, 187).mirror(false).addBox(-1.7f, -7.4f, -7.4f, 3.4f, 6, 1.2f)
                .texOffs(41, 206).mirror(false).addBox(-6.6f, -4.4f, -7.1f, 2.6f, 3.2f, 1)
                .texOffs(41, 206).mirror(false).addBox(4, -4.4f, -7.1f, 2.6f, 3.2f, 1)
                .texOffs(130, 206).mirror(false).addBox(-0.9f, -4, -7.8f, 1.8f, 1.8f, 0.6f), PartPose.offsetAndRotation(0, -12.6f, -0.5f, 0, 0, 0));
        PartDefinition rightEarring = rightEar.addOrReplaceChild("right_earring", CubeListBuilder.create()
                .texOffs(252, 198).mirror(false).addBox(-0.3f, 0, -1.2f, 0.6f, 2.6f, 2.4f), PartPose.offsetAndRotation(-2.2f, -0.4f, 0, 0, 0, 0));
        PartDefinition leftEarring = leftEar.addOrReplaceChild("left_earring", CubeListBuilder.create()
                .texOffs(252, 198).mirror(true).addBox(-0.3f, 0, -1.2f, 0.6f, 2.6f, 2.4f), PartPose.offsetAndRotation(2.2f, -0.4f, 0, 0, 0, 0));
        PartDefinition cloak = body.addOrReplaceChild("cloak", CubeListBuilder.create()
                .texOffs(0, 132).mirror(false).addBox(-14.5f, 0, -0.6f, 29, 19, 1.2f), PartPose.offsetAndRotation(0, -32, 11.4f, 0.14f, 0, 0));
        PartDefinition cloakLower = cloak.addOrReplaceChild("cloak_lower", CubeListBuilder.create()
                .texOffs(100, 132).mirror(false).addBox(-15.2f, 0, -0.6f, 30.4f, 17, 1.2f), PartPose.offsetAndRotation(0, 18.6f, 0, 0.06f, 0, 0));
        PartDefinition kasayaShoulder = body.addOrReplaceChild("kasaya_shoulder", CubeListBuilder.create()
                .texOffs(53, 77).mirror(false).addBox(-5.5f, -1.6f, -12, 10, 3.2f, 24)
                .texOffs(252, 77).mirror(false).addBox(-4.3f, -2.7f, -11.2f, 7.6f, 1.3f, 22.4f)
                .texOffs(0, 77).mirror(false).addBox(-5.5f, 1.4f, -12, 2, 5, 24), PartPose.offsetAndRotation(14.8f, -30.5f, 0, 0, 0, -0.38f));
        PartDefinition kasayaShoulderFall = kasayaShoulder.addOrReplaceChild("kasaya_shoulder_fall", CubeListBuilder.create()
                .texOffs(327, 0).mirror(false).addBox(-0.7f, 0, -11.6f, 1.4f, 6.5f, 23.2f), PartPose.offsetAndRotation(-5.3f, 1.2f, 0, 0, 0, 0.32f));
        PartDefinition kasayaSkirt = body.addOrReplaceChild("kasaya_skirt", CubeListBuilder.create()
                .texOffs(274, 172).mirror(false).addBox(-3.9f, 0, -0.7f, 7.8f, 9, 1.4f), PartPose.offsetAndRotation(4.55f, -4.4f, -11.4f, -0.12f, 0, -0.04f));
        PartDefinition kasayaSkirtHem = kasayaSkirt.addOrReplaceChild("kasaya_skirt_hem", CubeListBuilder.create()
                .texOffs(295, 172).mirror(false).addBox(-4.16f, 0, -0.7f, 8.32f, 8.6f, 1.4f), PartPose.offsetAndRotation(0, 8.6f, 0, -0.14f, 0, 0));
        PartDefinition kasayaPleatIn = kasayaSkirt.addOrReplaceChild("kasaya_pleat_in", CubeListBuilder.create()
                .texOffs(318, 172).mirror(false).addBox(-3.38f, 0, -0.6f, 6.76f, 8.6f, 1.2f), PartPose.offsetAndRotation(-6.5f, 0.3f, 0.5f, -0.03f, 0.2f, 0.06f));
        PartDefinition kasayaPleatInHem = kasayaPleatIn.addOrReplaceChild("kasaya_pleat_in_hem", CubeListBuilder.create()
                .texOffs(63, 187).mirror(false).addBox(-3.51f, 0, -0.6f, 7.02f, 7.4f, 1.2f), PartPose.offsetAndRotation(0, 8.3f, 0, -0.1f, 0, 0));
        PartDefinition kasayaPleatOut = kasayaSkirt.addOrReplaceChild("kasaya_pleat_out", CubeListBuilder.create()
                .texOffs(318, 172).mirror(false).addBox(-3.38f, 0, -0.6f, 6.76f, 8.8f, 1.2f), PartPose.offsetAndRotation(6.76f, 0.2f, 0.6f, 0, -0.22f, -0.06f));
        PartDefinition kasayaPleatOutHem = kasayaPleatOut.addOrReplaceChild("kasaya_pleat_out_hem", CubeListBuilder.create()
                .texOffs(337, 172).mirror(false).addBox(-3.51f, 0, -0.6f, 7.02f, 8.2f, 1.2f), PartPose.offsetAndRotation(0, 8.5f, 0, -0.1f, 0, 0));
        PartDefinition kasayaSide = body.addOrReplaceChild("kasaya_side", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-0.7f, 0, -10.6f, 1.4f, 15, 20.8f), PartPose.offsetAndRotation(13.8f, -4.4f, 0, 0, 0, -0.1f));
        PartDefinition kasayaTail = body.addOrReplaceChild("kasaya_tail", CubeListBuilder.create()
                .texOffs(195, 107).mirror(false).addBox(-3.2f, 0, -0.6f, 6.4f, 22, 1.2f), PartPose.offsetAndRotation(11, -24.5f, -11.6f, -0.08f, 0, 0.04f));
        PartDefinition kasayaTailOver = kasayaTail.addOrReplaceChild("kasaya_tail_over", CubeListBuilder.create()
                .texOffs(53, 172).mirror(false).addBox(-2.4f, 0, -0.5f, 4.8f, 13, 1), PartPose.offsetAndRotation(1.2f, -0.4f, -0.9f, -0.04f, 0, -0.06f));
        PartDefinition kasayaCape = body.addOrReplaceChild("kasaya_cape", CubeListBuilder.create()
                .texOffs(264, 132).mirror(false).addBox(-6.5f, 0, -0.6f, 13, 16, 1.2f), PartPose.offsetAndRotation(10, -31, 11.6f, 0.16f, 0, -0.06f));
        PartDefinition kasayaCapeLower = kasayaCape.addOrReplaceChild("kasaya_cape_lower", CubeListBuilder.create()
                .texOffs(257, 154).mirror(false).addBox(-7, 0, -0.6f, 14, 14, 1.2f), PartPose.offsetAndRotation(0, 15.6f, 0, 0.08f, 0, 0));
        PartDefinition jadeRing = body.addOrReplaceChild("jade_ring", CubeListBuilder.create()
                .texOffs(264, 187).mirror(false).addBox(-2.2f, -2.2f, -0.6f, 4.4f, 4.4f, 1.2f), PartPose.offsetAndRotation(10, -27, -12.4f, 0, 0, 0));
        PartDefinition kasayaFront0 = body.addOrReplaceChild("kasaya_front_0", CubeListBuilder.create()
                .texOffs(279, 187).mirror(false).addBox(-18.5f, -2.2f, -0.75f, 37, 4.4f, 1.5f), PartPose.offsetAndRotation(1.4f, -18.8f, -11.2f, 0.05f, 0, -0.74f));
        PartDefinition kasayaFront1 = body.addOrReplaceChild("kasaya_front_1", CubeListBuilder.create()
                .texOffs(0, 198).mirror(false).addBox(-15.3f, -2.2f, -0.75f, 30.6f, 4.4f, 1.5f), PartPose.offsetAndRotation(3.4622f, -16.2237f, -11.93f, 0.07f, 0, -0.675f));
        PartDefinition kasayaFront2 = body.addOrReplaceChild("kasaya_front_2", CubeListBuilder.create()
                .texOffs(67, 198).mirror(false).addBox(-12.1f, -2.2f, -0.75f, 24.2f, 4.4f, 1.5f), PartPose.offsetAndRotation(5.1809f, -13.3903f, -11.56f, 0.09f, 0, -0.61f));
        PartDefinition kasayaFront3 = body.addOrReplaceChild("kasaya_front_3", CubeListBuilder.create()
                .texOffs(122, 198).mirror(false).addBox(-8.9f, -2.2f, -0.75f, 17.8f, 4.4f, 1.5f), PartPose.offsetAndRotation(6.5323f, -10.3342f, -12.29f, 0.11f, 0, -0.545f));
        PartDefinition kasayaFront4 = body.addOrReplaceChild("kasaya_front_4", CubeListBuilder.create()
                .texOffs(163, 198).mirror(false).addBox(-5.7f, -2.2f, -0.75f, 11.4f, 4.8f, 1.5f), PartPose.offsetAndRotation(7.4955f, -7.0917f, -11.92f, 0.13f, 0, -0.48f));
        PartDefinition kasayaFrontSwag0 = body.addOrReplaceChild("kasaya_front_swag_0", CubeListBuilder.create()
                .texOffs(50, 206).mirror(false).addBox(-3, -0.4f, -0.6f, 6, 2.6f, 1.2f), PartPose.offsetAndRotation(1.1567f, -0.8604f, -12.2f, 0.12f, 0, -0.168f));
        PartDefinition kasayaFrontSwag1 = body.addOrReplaceChild("kasaya_front_swag_1", CubeListBuilder.create()
                .texOffs(261, 198).mirror(false).addBox(-3.3f, -0.4f, -0.6f, 6.6f, 3.2f, 1.2f), PartPose.offsetAndRotation(7.3656f, -4.0928f, -12.2f, 0.12f, 0, -0.168f));
        PartDefinition kasayaFrontSwag2 = body.addOrReplaceChild("kasaya_front_swag_2", CubeListBuilder.create()
                .texOffs(50, 206).mirror(false).addBox(-2.7f, -0.4f, -0.6f, 5.4f, 2.4f, 1.2f), PartPose.offsetAndRotation(13.1311f, -7.0944f, -12.2f, 0.12f, 0, -0.168f));
        PartDefinition kasayaBack0 = body.addOrReplaceChild("kasaya_back_0", CubeListBuilder.create()
                .texOffs(279, 187).mirror(false).addBox(-18.5f, -2.2f, -0.75f, 37, 4.4f, 1.5f), PartPose.offsetAndRotation(1.4f, -18.8f, 10.8f, -0.05f, 0, -0.74f));
        PartDefinition kasayaBack1 = body.addOrReplaceChild("kasaya_back_1", CubeListBuilder.create()
                .texOffs(0, 198).mirror(false).addBox(-15.3f, -2.2f, -0.75f, 30.6f, 4.4f, 1.5f), PartPose.offsetAndRotation(3.4622f, -16.2237f, 11.53f, -0.07f, 0, -0.675f));
        PartDefinition kasayaBack2 = body.addOrReplaceChild("kasaya_back_2", CubeListBuilder.create()
                .texOffs(67, 198).mirror(false).addBox(-12.1f, -2.2f, -0.75f, 24.2f, 4.4f, 1.5f), PartPose.offsetAndRotation(5.1809f, -13.3903f, 11.16f, -0.09f, 0, -0.61f));
        PartDefinition kasayaBack3 = body.addOrReplaceChild("kasaya_back_3", CubeListBuilder.create()
                .texOffs(122, 198).mirror(false).addBox(-8.9f, -2.2f, -0.75f, 17.8f, 4.4f, 1.5f), PartPose.offsetAndRotation(6.5323f, -10.3342f, 11.89f, -0.11f, 0, -0.545f));
        PartDefinition kasayaBack4 = body.addOrReplaceChild("kasaya_back_4", CubeListBuilder.create()
                .texOffs(163, 198).mirror(false).addBox(-5.7f, -2.2f, -0.75f, 11.4f, 4.8f, 1.5f), PartPose.offsetAndRotation(7.4955f, -7.0917f, 11.52f, -0.13f, 0, -0.48f));
        PartDefinition kasayaBackSwag0 = body.addOrReplaceChild("kasaya_back_swag_0", CubeListBuilder.create()
                .texOffs(50, 206).mirror(false).addBox(-3, -0.4f, -0.6f, 6, 2.6f, 1.2f), PartPose.offsetAndRotation(1.1567f, -0.8604f, 11.8f, -0.12f, 0, -0.168f));
        PartDefinition kasayaBackSwag1 = body.addOrReplaceChild("kasaya_back_swag_1", CubeListBuilder.create()
                .texOffs(261, 198).mirror(false).addBox(-3.3f, -0.4f, -0.6f, 6.6f, 3.2f, 1.2f), PartPose.offsetAndRotation(7.3656f, -4.0928f, 11.8f, -0.12f, 0, -0.168f));
        PartDefinition kasayaBackSwag2 = body.addOrReplaceChild("kasaya_back_swag_2", CubeListBuilder.create()
                .texOffs(50, 206).mirror(false).addBox(-2.7f, -0.4f, -0.6f, 5.4f, 2.4f, 1.2f), PartPose.offsetAndRotation(13.1311f, -7.0944f, 11.8f, -0.12f, 0, -0.168f));
        PartDefinition spear = rightHand.addOrReplaceChild("spear", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.8f, -48, -0.8f, 1.6f, 74, 1.6f)
                .texOffs(280, 198).mirror(false).addBox(-1.4f, -50, -1.4f, 2.8f, 2.4f, 2.8f)
                .texOffs(85, 206).mirror(false).addBox(-2.4f, -51.6f, -0.7f, 4.8f, 1.8f, 1.4f)
                .texOffs(293, 198).mirror(false).addBox(-1.3f, -54.4f, -1.3f, 2.6f, 3, 2.6f)
                .texOffs(92, 154).mirror(false).addBox(-3, -70, -0.45f, 6, 16, 0.9f)
                .texOffs(167, 132).mirror(false).addBox(-0.5f, -70.6f, -0.9f, 1, 17, 1.8f)
                .texOffs(192, 198).mirror(false).addBox(-1.2f, 24, -1.2f, 2.4f, 3.2f, 2.4f)
                .texOffs(67, 206).mirror(false).addBox(-1.1f, -30, -1.1f, 2.2f, 1.2f, 2.2f)
                .texOffs(67, 206).mirror(false).addBox(-1.1f, 8, -1.1f, 2.2f, 1.2f, 2.2f), PartPose.offsetAndRotation(0, 2.8f, -0.2f, 0.6629f, 0.2324f, -0.0623f));
        PartDefinition tassel = spear.addOrReplaceChild("tassel", CubeListBuilder.create()
                .texOffs(205, 198).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 2.2f, 3.6f), PartPose.offsetAndRotation(0, -48.4f, 0, 0, 0, 0));
        PartDefinition tasselStrand0 = tassel.addOrReplaceChild("tassel_strand_0", CubeListBuilder.create()
                .texOffs(358, 172).mirror(false).addBox(-0.75f, 0, -0.75f, 1.5f, 9, 1.5f), PartPose.offsetAndRotation(1.3f, 1.8f, 0, 0, 0, -0.32f));
        PartDefinition tasselStrand1 = tassel.addOrReplaceChild("tassel_strand_1", CubeListBuilder.create()
                .texOffs(186, 172).mirror(false).addBox(-0.75f, 0, -0.75f, 1.5f, 10.5f, 1.5f), PartPose.offsetAndRotation(0.9192f, 1.8f, 0.9192f, 0.2263f, 0, -0.2263f));
        PartDefinition tasselStrand2 = tassel.addOrReplaceChild("tassel_strand_2", CubeListBuilder.create()
                .texOffs(66, 172).mirror(false).addBox(-0.75f, 0, -0.75f, 1.5f, 12, 1.5f), PartPose.offsetAndRotation(0, 1.8f, 1.3f, 0.32f, 0, -0));
        PartDefinition tasselStrand3 = tassel.addOrReplaceChild("tassel_strand_3", CubeListBuilder.create()
                .texOffs(358, 172).mirror(false).addBox(-0.75f, 0, -0.75f, 1.5f, 9, 1.5f), PartPose.offsetAndRotation(-0.9192f, 1.8f, 0.9192f, 0.2263f, 0, 0.2263f));
        PartDefinition tasselStrand4 = tassel.addOrReplaceChild("tassel_strand_4", CubeListBuilder.create()
                .texOffs(186, 172).mirror(false).addBox(-0.75f, 0, -0.75f, 1.5f, 10.5f, 1.5f), PartPose.offsetAndRotation(-1.3f, 1.8f, 0, 0, 0, 0.32f));
        PartDefinition tasselStrand5 = tassel.addOrReplaceChild("tassel_strand_5", CubeListBuilder.create()
                .texOffs(66, 172).mirror(false).addBox(-0.75f, 0, -0.75f, 1.5f, 12, 1.5f), PartPose.offsetAndRotation(-0.9192f, 1.8f, -0.9192f, -0.2263f, 0, 0.2263f));
        PartDefinition tasselStrand6 = tassel.addOrReplaceChild("tassel_strand_6", CubeListBuilder.create()
                .texOffs(358, 172).mirror(false).addBox(-0.75f, 0, -0.75f, 1.5f, 9, 1.5f), PartPose.offsetAndRotation(-0, 1.8f, -1.3f, -0.32f, 0, 0));
        PartDefinition tasselStrand7 = tassel.addOrReplaceChild("tassel_strand_7", CubeListBuilder.create()
                .texOffs(186, 172).mirror(false).addBox(-0.75f, 0, -0.75f, 1.5f, 10.5f, 1.5f), PartPose.offsetAndRotation(0.9192f, 1.8f, -0.9192f, -0.2263f, 0, -0.2263f));
        return LayerDefinition.create(mesh, 384, 384);
        // GENERATED king: end
    }

    /** The Black Bear Soldier (256x256): model and skins from {@code assets_source/black_bear.py}. */
    public static LayerDefinition soldierLayer() {
        // GENERATED soldier: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(59, 33).mirror(false).addBox(-5, -1, -5, 10, 11.5f, 10), PartPose.offsetAndRotation(-5.25f, 2.147f, 1.5f, -0.2f, 0, 0.05f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(59, 33).mirror(true).addBox(-5, -1, -5, 10, 11.5f, 10), PartPose.offsetAndRotation(5.25f, 2.147f, 1.5f, -0.2f, 0, -0.05f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(330, 33).mirror(false).addBox(-4.2f, 0, -4.2f, 8.4f, 8.6f, 8.4f)
                .texOffs(50, 94).mirror(false).addBox(-4.7f, 0.4f, -4.9f, 9.4f, 6.4f, 3.4f)
                .texOffs(52, 107).mirror(false).addBox(-4.5f, -1.6f, -5.3f, 9, 2.8f, 1.6f), PartPose.offsetAndRotation(0, 10, 0.4f, 0.3f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(330, 33).mirror(true).addBox(-4.2f, 0, -4.2f, 8.4f, 8.6f, 8.4f)
                .texOffs(50, 94).mirror(true).addBox(-4.7f, 0.4f, -4.9f, 9.4f, 6.4f, 3.4f)
                .texOffs(52, 107).mirror(true).addBox(-4.5f, -1.6f, -5.3f, 9, 2.8f, 1.6f), PartPose.offsetAndRotation(0, 10, 0.4f, 0.3f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(61, 77).mirror(false).addBox(-4.8f, 0, -6.8f, 9.6f, 3.8f, 10.4f)
                .texOffs(75, 107).mirror(false).addBox(-4.6f, 1.2f, -8.3f, 1.7f, 2.2f, 2)
                .texOffs(0, 107).mirror(false).addBox(-2.3f, 1.2f, -8.6f, 1.7f, 2.2f, 2.2f)
                .texOffs(0, 107).mirror(false).addBox(0.6f, 1.2f, -8.6f, 1.7f, 2.2f, 2.2f)
                .texOffs(75, 107).mirror(false).addBox(2.9f, 1.2f, -8.3f, 1.7f, 2.2f, 2), PartPose.offsetAndRotation(0, 8, 0, -0.1f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(61, 77).mirror(true).addBox(-4.8f, 0, -6.8f, 9.6f, 3.8f, 10.4f)
                .texOffs(75, 107).mirror(true).addBox(2.9f, 1.2f, -8.3f, 1.7f, 2.2f, 2)
                .texOffs(0, 107).mirror(true).addBox(0.6f, 1.2f, -8.6f, 1.7f, 2.2f, 2.2f)
                .texOffs(0, 107).mirror(true).addBox(-2.3f, 1.2f, -8.6f, 1.7f, 2.2f, 2.2f)
                .texOffs(75, 107).mirror(true).addBox(-4.6f, 1.2f, -8.3f, 1.7f, 2.2f, 2), PartPose.offsetAndRotation(0, 8, 0, -0.1f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(163, 0).mirror(false).addBox(-9.5f, -12, -7.3f, 19, 13, 14.6f)
                .texOffs(9, 0).mirror(false).addBox(-10.5f, -27, -7.5f, 21, 16, 15)
                .texOffs(82, 0).mirror(false).addBox(-11.2f, -27.6f, -8.3f, 22.4f, 13.4f, 16.6f)
                .texOffs(232, 0).mirror(false).addBox(-10.3f, -14.4f, -8, 20.6f, 8, 16)
                .texOffs(177, 33).mirror(false).addBox(-10.5f, -6.6f, -8.2f, 21, 3.6f, 16.4f)
                .texOffs(228, 94).mirror(false).addBox(-3.4f, -7.8f, -9.3f, 6.8f, 6, 1.4f)
                .texOffs(312, 94).mirror(false).addBox(-9.5f, -26.4f, -9.1f, 6, 6, 0.8f)
                .texOffs(312, 94).mirror(false).addBox(3.5f, -26.4f, -9.1f, 6, 6, 0.8f)
                .texOffs(100, 33).mirror(false).addBox(-12.7f, -35, -4, 25.4f, 9.5f, 12)
                .texOffs(139, 58).mirror(false).addBox(-9, -34, -7, 18, 8, 9)
                .texOffs(84, 107).mirror(false).addBox(-7, -30.4f, -7.8f, 14, 3.8f, 1), PartPose.offsetAndRotation(0, 2.147f, 1.5f, 0.24f, 0, 0));
        PartDefinition rightTasset = body.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(264, 77).mirror(false).addBox(-4.65f, 0, -0.6f, 9.3f, 11, 1.2f), PartPose.offsetAndRotation(-5.25f, -3.4f, -8.2f, -0.12f, 0.05f, 0.05f));
        PartDefinition leftTasset = body.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(264, 77).mirror(true).addBox(-4.65f, 0, -0.6f, 9.3f, 11, 1.2f), PartPose.offsetAndRotation(5.25f, -3.4f, -8.2f, -0.12f, -0.05f, -0.05f));
        PartDefinition rightSideTasset = body.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(307, 0).mirror(false).addBox(-1.2f, 0, -6.7f, 1.2f, 10, 13.4f), PartPose.offsetAndRotation(-10.3f, -3.4f, 0, 0, 0, 0.18f));
        PartDefinition leftSideTasset = body.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(307, 0).mirror(true).addBox(0, 0, -6.7f, 1.2f, 10, 13.4f), PartPose.offsetAndRotation(10.3f, -3.4f, 0, 0, 0, -0.18f));
        PartDefinition backTasset = body.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(202, 77).mirror(false).addBox(-9.5f, 0, -0.6f, 19, 11.5f, 1.2f), PartPose.offsetAndRotation(0, -3.4f, 8, 0.14f, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(102, 94).mirror(false).addBox(-2.6f, -2.6f, 0, 5.2f, 5.2f, 3.8f), PartPose.offsetAndRotation(0, -3, 7.1f, -0.6f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(340, 0).mirror(false).addBox(-4.56f, -2.4f, -5, 8.4f, 13.5f, 10), PartPose.offsetAndRotation(-13.7f, -27.5f, 0, -0.2f, 0, 0.2f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(340, 0).mirror(true).addBox(-3.84f, -2.4f, -5, 8.4f, 13.5f, 10), PartPose.offsetAndRotation(13.7f, -27.5f, 0, -0.2f, 0, -0.2f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(104, 77).mirror(false).addBox(-6, -1.8f, -6, 9.6f, 2.2f, 12)
                .texOffs(194, 58).mirror(false).addBox(-6.8f, 0.4f, -6.6f, 10.4f, 2.4f, 13.2f)
                .texOffs(0, 58).mirror(false).addBox(-7.6f, 2.8f, -7.2f, 11, 2.6f, 14.4f)
                .texOffs(0, 94).mirror(false).addBox(-8.4f, 0, -2.9f, 1.2f, 5.6f, 5.8f), PartPose.offsetAndRotation(-0.8f, -2.6f, 0, 0, 0, 0.42f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(104, 77).mirror(true).addBox(-3.6f, -1.8f, -6, 9.6f, 2.2f, 12)
                .texOffs(194, 58).mirror(true).addBox(-3.6f, 0.4f, -6.6f, 10.4f, 2.4f, 13.2f)
                .texOffs(0, 58).mirror(true).addBox(-3.4f, 2.8f, -7.2f, 11, 2.6f, 14.4f)
                .texOffs(0, 94).mirror(true).addBox(7.2f, 0, -2.9f, 1.2f, 5.6f, 5.8f), PartPose.offsetAndRotation(0.8f, -2.6f, 0, 0, 0, -0.42f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(254, 33).mirror(false).addBox(-4.4f, 0, -4.4f, 8.8f, 10.5f, 8.8f)
                .texOffs(245, 58).mirror(false).addBox(-4.9f, 1.4f, -4.9f, 9.8f, 6.8f, 9.8f)
                .texOffs(123, 94).mirror(false).addBox(-5.4f, -1, -3, 2, 3.4f, 6), PartPose.offsetAndRotation(0, 10.4f, 0, -0.7f, -0.1f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(254, 33).mirror(true).addBox(-4.4f, 0, -4.4f, 8.8f, 10.5f, 8.8f)
                .texOffs(245, 58).mirror(true).addBox(-4.9f, 1.4f, -4.9f, 9.8f, 6.8f, 9.8f)
                .texOffs(123, 94).mirror(true).addBox(3.4f, -1, -3, 2, 3.4f, 6), PartPose.offsetAndRotation(0, 10.4f, 0, -0.7f, 0.1f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(286, 58).mirror(false).addBox(-4.6f, 0, -4.8f, 9.2f, 5.6f, 9.6f)
                .texOffs(75, 107).mirror(false).addBox(-4.6f, 4.8f, -5.2f, 1.8f, 2.8f, 1.8f)
                .texOffs(75, 107).mirror(false).addBox(-2.2f, 4.8f, -5.4f, 1.8f, 3, 1.8f)
                .texOffs(75, 107).mirror(false).addBox(0.4f, 4.8f, -5.4f, 1.8f, 3, 1.8f)
                .texOffs(75, 107).mirror(false).addBox(2.8f, 4.8f, -5.2f, 1.8f, 2.8f, 1.8f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(286, 58).mirror(true).addBox(-4.6f, 0, -4.8f, 9.2f, 5.6f, 9.6f)
                .texOffs(75, 107).mirror(true).addBox(2.8f, 4.8f, -5.2f, 1.8f, 2.8f, 1.8f)
                .texOffs(75, 107).mirror(true).addBox(0.4f, 4.8f, -5.4f, 1.8f, 3, 1.8f)
                .texOffs(75, 107).mirror(true).addBox(-2.2f, 4.8f, -5.4f, 1.8f, 3, 1.8f)
                .texOffs(75, 107).mirror(true).addBox(-4.6f, 4.8f, -5.2f, 1.8f, 2.8f, 1.8f), PartPose.offsetAndRotation(0, 10, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 33).mirror(false).addBox(-7.6f, -11.4f, -6.8f, 15.2f, 11, 12.6f)
                .texOffs(289, 77).mirror(false).addBox(-6, -13.4f, -5.4f, 12, 2.4f, 9.6f)
                .texOffs(17, 94).mirror(false).addBox(-5.2f, -6.8f, -10.2f, 10.4f, 6.6f, 4.2f)
                .texOffs(140, 94).mirror(false).addBox(-4, -6.2f, -13.6f, 8, 5.4f, 3.8f)
                .texOffs(247, 94).mirror(false).addBox(-3, -5.6f, -16.2f, 6, 4.4f, 3)
                .texOffs(115, 107).mirror(false).addBox(-2.6f, -6.4f, -17.2f, 5.2f, 3, 1.8f)
                .texOffs(266, 94).mirror(false).addBox(-3.4f, -1.6f, -16, 6.8f, 1.6f, 5.8f)
                .texOffs(132, 107).mirror(false).addBox(-3, -1, -15.4f, 1.1f, 2.6f, 1.1f)
                .texOffs(132, 107).mirror(false).addBox(1.9f, -1, -15.4f, 1.1f, 2.6f, 1.1f)
                .texOffs(327, 94).mirror(false).addBox(-7.2f, -14.6f, -4.4f, 14.4f, 2.6f, 4), PartPose.offsetAndRotation(0, -33.5f, -4, -0.12f, 0, 0));
        PartDefinition bridge = head.addOrReplaceChild("bridge", CubeListBuilder.create()
                .texOffs(199, 94).mirror(false).addBox(-3.2f, -1.4f, -6.6f, 6.4f, 1.6f, 6.8f), PartPose.offsetAndRotation(0, -6.6f, -9.4f, -0.18f, 0, 0));
        PartDefinition rightJowl = head.addOrReplaceChild("right_jowl", CubeListBuilder.create()
                .texOffs(327, 58).mirror(false).addBox(-2.6f, -2.8f, -3.6f, 3.4f, 6.8f, 8.6f), PartPose.offsetAndRotation(-7, -4.6f, -4.6f, 0, 0.25f, 0.22f));
        PartDefinition leftJowl = head.addOrReplaceChild("left_jowl", CubeListBuilder.create()
                .texOffs(327, 58).mirror(true).addBox(-0.8f, -2.8f, -3.6f, 3.4f, 6.8f, 8.6f), PartPose.offsetAndRotation(7, -4.6f, -4.6f, 0, -0.25f, -0.22f));
        PartDefinition rightFaceRuff = head.addOrReplaceChild("right_face_ruff", CubeListBuilder.create()
                .texOffs(354, 58).mirror(false).addBox(-3, -4, -1.2f, 3.2f, 9.6f, 5.2f), PartPose.offsetAndRotation(-7.6f, -8.4f, 1.6f, 0, -0.35f, 0.3f));
        PartDefinition leftFaceRuff = head.addOrReplaceChild("left_face_ruff", CubeListBuilder.create()
                .texOffs(354, 58).mirror(true).addBox(-0.2f, -4, -1.2f, 3.2f, 9.6f, 5.2f), PartPose.offsetAndRotation(7.6f, -8.4f, 1.6f, 0, 0.35f, -0.3f));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(334, 77).mirror(false).addBox(-4, 0, -9, 8, 2.9f, 9.2f)
                .texOffs(202, 107).mirror(false).addBox(-3.7f, -1, -8.8f, 7.4f, 1, 1.2f)
                .texOffs(176, 107).mirror(false).addBox(-3.2f, -2.4f, -8.4f, 1, 2.4f, 1)
                .texOffs(176, 107).mirror(false).addBox(2.2f, -2.4f, -8.4f, 1, 2.4f, 1), PartPose.offsetAndRotation(0, -0.6f, -6.4f, 0.16f, 0, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(293, 94).mirror(false).addBox(-2.8f, -4.4f, -1.3f, 5.6f, 4.6f, 2.6f)
                .texOffs(141, 107).mirror(false).addBox(-2, -5.6f, -1.1f, 4, 1.4f, 2.2f), PartPose.offsetAndRotation(-6, -11.8f, -0.8f, -0.12f, 0.25f, -0.36f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(293, 94).mirror(true).addBox(-2.8f, -4.4f, -1.3f, 5.6f, 4.6f, 2.6f)
                .texOffs(141, 107).mirror(true).addBox(-2, -5.6f, -1.1f, 4, 1.4f, 2.2f), PartPose.offsetAndRotation(6, -11.8f, -0.8f, -0.12f, -0.25f, 0.36f));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(11, 107).mirror(false).addBox(-5.6f, -1.3f, -1.2f, 5.8f, 2.4f, 2.6f), PartPose.offsetAndRotation(-0.6f, -10.2f, -7, 0.1f, 0.15f, 0.3f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(11, 107).mirror(true).addBox(-0.2f, -1.3f, -1.2f, 5.8f, 2.4f, 2.6f), PartPose.offsetAndRotation(0.6f, -10.2f, -7, 0.1f, -0.15f, -0.3f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(223, 107).mirror(false).addBox(-1.3f, -0.7f, -0.5f, 2.6f, 1.5f, 0.7f), PartPose.offsetAndRotation(-3.7f, -8.2f, -6.9f, 0, 0.22f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(223, 107).mirror(true).addBox(-1.3f, -0.7f, -0.5f, 2.6f, 1.5f, 0.7f), PartPose.offsetAndRotation(3.7f, -8.2f, -6.9f, 0, -0.22f, 0));
        PartDefinition helm = head.addOrReplaceChild("helm", CubeListBuilder.create()
                .texOffs(53, 58).mirror(false).addBox(-7.8f, -2.6f, -7, 15.6f, 3.8f, 13.6f)
                .texOffs(149, 77).mirror(false).addBox(-7, -4.6f, -5.8f, 14, 2.2f, 11.4f)
                .texOffs(0, 77).mirror(false).addBox(-8, 0.6f, -7.1f, 16, 1.2f, 13.8f)
                .texOffs(366, 94).mirror(false).addBox(-1.2f, -7.6f, -1.2f, 2.4f, 3.2f, 2.4f)
                .texOffs(245, 77).mirror(false).addBox(-8.6f, 0.8f, -5.2f, 1.4f, 6.4f, 6.4f)
                .texOffs(245, 77).mirror(false).addBox(7.2f, 0.8f, -5.2f, 1.4f, 6.4f, 6.4f), PartPose.offsetAndRotation(0, -12.6f, -0.5f, 0, 0, 0));
        PartDefinition plume = helm.addOrReplaceChild("plume", CubeListBuilder.create()
                .texOffs(79, 94).mirror(false).addBox(-1.3f, -1, 0, 2.6f, 2.6f, 8), PartPose.offsetAndRotation(0, -7.2f, 0.6f, 0.7f, 0, 0));
        PartDefinition rightHorn = helm.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(165, 94).mirror(false).addBox(-1.1f, -6.5f, -1.1f, 2.2f, 6.5f, 2.2f)
                .texOffs(30, 107).mirror(false).addBox(-0.8f, -9.8f, -0.7f, 1.6f, 3.6f, 1.6f), PartPose.offsetAndRotation(-6.8f, -2.6f, -2, 0, 0.2f, -0.7f));
        PartDefinition leftHorn = helm.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(165, 94).mirror(true).addBox(-1.1f, -6.5f, -1.1f, 2.2f, 6.5f, 2.2f)
                .texOffs(30, 107).mirror(true).addBox(-0.8f, -9.8f, -0.7f, 1.6f, 3.6f, 1.6f), PartPose.offsetAndRotation(6.8f, -2.6f, -2, 0, -0.2f, 0.7f));
        PartDefinition shield = body.addOrReplaceChild("shield", CubeListBuilder.create()
                .texOffs(291, 33).mirror(false).addBox(-8.5f, -8.5f, 0, 17, 17, 1.4f)
                .texOffs(156, 107).mirror(false).addBox(-2, -2, 1.2f, 4, 4, 1), PartPose.offsetAndRotation(0, -20, 8.4f, 0.1f, 0, 0));
        PartDefinition mace = rightHand.addOrReplaceChild("mace", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.9f, -21, -0.9f, 1.8f, 30, 1.8f)
                .texOffs(114, 58).mirror(false).addBox(-3, -33, -3, 6, 12, 6)
                .texOffs(181, 107).mirror(false).addBox(-3.7f, -31, -0.55f, 7.4f, 1.1f, 1.1f)
                .texOffs(178, 94).mirror(false).addBox(-0.55f, -31, -3.7f, 1.1f, 1.1f, 7.4f)
                .texOffs(181, 107).mirror(false).addBox(-3.7f, -25.5f, -0.55f, 7.4f, 1.1f, 1.1f)
                .texOffs(178, 94).mirror(false).addBox(-0.55f, -25.5f, -3.7f, 1.1f, 1.1f, 7.4f)
                .texOffs(167, 107).mirror(false).addBox(-0.6f, -35.4f, -0.6f, 1.2f, 2.6f, 1.2f)
                .texOffs(39, 107).mirror(false).addBox(-1.3f, 7.6f, -1.3f, 2.6f, 2.2f, 2.6f), PartPose.offsetAndRotation(0, 2.8f, -0.2f, 1.1429f, 0.2324f, -0.0623f));
        return LayerDefinition.create(mesh, 384, 384);
        // GENERATED soldier: end
    }

    /**
     * A bear's pose: heavy strides, the body rolling; the spear held upright, thrust on a blow (the
     * soldier's mace brought down over his shoulder). The king gathering the black wind spreads his arms
     * wide, throws his head back and roars; rearing (for the maul, or a soldier's slam) both arms go up,
     * the body back; drawing back for the gale, the spear arm drawn behind him, the body turned. The
     * cloak, the kasaya's end, the spear's tassel and the soldier's plume swing.
     */
    public static final class BearModel<T extends Mob> extends EntityModel<T> {
        private final ModelPart root, body, head, jaw, rightLeg, leftLeg, rightShin, leftShin, rightArm, leftArm, rightForearm, leftForearm,
                rightTasset, leftTasset, backTasset, rightEar, leftEar, tail;
        private final ModelPart cloak, cloakLower, kasayaTail, kasayaCape, kasayaSkirt, tassel, plume;
        private final List<ModelPart> strands = new ArrayList<>();

        public BearModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightTasset = body.getChild("right_tasset");
            leftTasset = body.getChild("left_tasset");
            backTasset = body.getChild("back_tasset");
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
            tail = body.getChild("tail");
            cloak = child(body, "cloak");
            cloakLower = child(cloak, "cloak_lower");
            kasayaTail = child(body, "kasaya_tail");
            tassel = child(child(rightForearm.getChild("right_hand"), "spear"), "tassel");
            plume = child(child(head, "helm"), "plume");
            kasayaCape = child(body, "kasaya_cape");
            kasayaSkirt = child(body, "kasaya_skirt");
            for (int k = 0; tassel != null && tassel.hasChild("tassel_strand_" + k); k++) strands.add(tassel.getChild("tassel_strand_" + k));
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            boolean king = e instanceof BlackBearDemonEntity;
            int action = e instanceof BlackBearDemonEntity b ? b.action()
                    : e instanceof BearSoldierEntity s && s.isRaising() ? BlackBearDemonEntity.REARING : BlackBearDemonEntity.IDLE;
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float w = swing * 0.5f, stride = Mth.cos(w) * 0.7f * amount;
            float breath = Mth.sin(age * 0.06f);
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            turn(rightShin, Math.max(0, Mth.sin(w)) * 0.6f * amount, 0, 0);
            turn(leftShin, Math.max(0, -Mth.sin(w)) * 0.6f * amount, 0, 0);
            turn(rightTasset, Math.min(0, stride) * 0.8f, 0, 0);
            turn(leftTasset, Math.min(0, -stride) * 0.8f, 0, 0);
            turn(backTasset, Math.abs(stride) * 0.3f, 0, 0);
            body.y = body.getInitialPose().y + breath * 0.3f + Math.abs(Mth.sin(w)) * amount * 0.6f;
            float bx = 0, by = Mth.cos(w) * 0.08f * amount, hx = pitch * Mth.DEG_TO_RAD, hy = yaw * Mth.DEG_TO_RAD * 0.7f;
            float gape = Math.max(0, Mth.sin(age * 0.04f)) * 0.05f;
            float rx = -Mth.cos(w) * 0.2f * amount, rz = 0, lx = Mth.cos(w) * 0.4f * amount + breath * 0.02f, lz = 0, rfx = 0, lfx = 0;
            // a blow: the spear thrust forward, or the mace brought down
            rx -= strike * (king ? 1.1f : 1.7f);
            rfx += king ? strike * 0.9f : -strike * 0.3f;
            bx += strike * 0.12f;
            gape = Math.max(gape, strike * 0.3f);
            if (action == BlackBearDemonEntity.GATHERING) { // the black wind gathering: arms spread wide, head thrown back, roaring
                rx = -0.5f;
                rz = 1.15f;
                lx = -0.5f;
                lz = -1.15f;
                rfx = 0.3f;
                lfx = 0.3f;
                bx = -0.18f;
                hx = -0.55f;
                gape = 0.5f + Mth.sin(age * 0.5f) * 0.05f;
            } else if (action == BlackBearDemonEntity.REARING) { // up on his hind legs, both arms high, about to crash down
                rx = -2.7f;
                lx = -2.7f;
                rz = 0.2f;
                lz = -0.2f;
                rfx = -0.2f;
                lfx = -0.2f;
                bx = -0.35f;
                hx = -0.35f;
                gape = 0.45f;
            } else if (action == BlackBearDemonEntity.DRAWING) { // the spear drawn back behind him, the body turned, the left arm thrust out
                rx = 0.9f;
                rz = 0.4f;
                lx = -1.3f;
                by = 0.45f;
                bx = 0.1f;
                gape = 0.3f;
            }
            turn(body, bx, by, 0);
            turn(rightArm, rx, 0, rz);
            turn(leftArm, lx, 0, lz);
            turn(rightForearm, rfx, 0, 0);
            turn(leftForearm, lfx, 0, 0);
            turn(head, hx + breath * 0.02f, hy, 0);
            turn(jaw, gape, 0, 0);
            float twitch = Mth.sin(age * 0.23f) * Mth.sin(age * 0.05f) * 0.08f;
            turn(rightEar, 0, 0, twitch);
            turn(leftEar, 0, 0, -twitch);
            turn(tail, amount * 0.2f, Mth.sin(w) * 0.2f * amount, 0);
            float flow = action == BlackBearDemonEntity.GATHERING ? 0.5f + Mth.sin(age * 0.6f) * 0.15f : 0;
            turnIf(cloak, amount * 0.4f + breath * 0.03f + flow, 0, 0);
            turnIf(cloakLower, amount * 0.25f + Mth.sin(age * 0.08f) * 0.05f + flow * 0.6f, 0, 0);
            turnIf(kasayaTail, -Math.abs(stride) * 0.3f + Mth.sin(age * 0.07f) * 0.05f - flow * 0.5f, 0, Mth.sin(age * 0.05f) * 0.05f);
            turnIf(tassel, Mth.sin(age * 0.1f) * 0.08f + amount * 0.2f, 0, Mth.sin(age * 0.07f) * 0.08f + flow * 0.4f);
            turnIf(plume, amount * 0.3f + Mth.sin(age * 0.12f) * 0.06f, 0, 0);
            turnIf(kasayaCape, amount * 0.35f + breath * 0.03f + flow * 0.8f, 0, Mth.sin(age * 0.06f) * 0.03f);
            turnIf(kasayaSkirt, -Math.abs(stride) * 0.25f, 0, 0);
            for (int k = 0; k < strands.size(); k++) {
                ModelPart st = strands.get(k);
                st.xRot = st.getInitialPose().xRot + Mth.sin(age * 0.11f + k * 0.8f) * 0.1f + amount * 0.25f + flow * 0.4f;
                st.zRot = st.getInitialPose().zRot + Mth.sin(age * 0.09f + k * 1.3f) * 0.1f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    // ── The Boar Demon ──────────────────────────────────────────────────

    /** The Boar Demon (256x256): model and skins from {@code assets_source/black_wind_folk.py}. */
    public static LayerDefinition boarLayer() {
        // GENERATED boar: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(220, 0).mirror(false).addBox(-2.9f, -1, -3, 5.8f, 9, 6), PartPose.offsetAndRotation(-4, 3.9773f, 0.5f, -0.5f, 0, 0.06f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(220, 0).mirror(true).addBox(-2.9f, -1, -3, 5.8f, 9, 6), PartPose.offsetAndRotation(4, 3.9773f, 0.5f, -0.5f, 0, -0.06f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(163, 31).mirror(false).addBox(-2.1f, 0, -2.2f, 4.2f, 7.4f, 4.4f)
                .texOffs(64, 47).mirror(false).addBox(-2.5f, 1, -2.6f, 5, 4.6f, 5.2f), PartPose.offsetAndRotation(0, 8, 0.2f, 1.1f, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(163, 31).mirror(true).addBox(-2.1f, 0, -2.2f, 4.2f, 7.4f, 4.4f)
                .texOffs(64, 47).mirror(true).addBox(-2.5f, 1, -2.6f, 5, 4.6f, 5.2f), PartPose.offsetAndRotation(0, 8, 0.2f, 1.1f, 0, 0));
        PartDefinition rightAnkle = rightShin.addOrReplaceChild("right_ankle", CubeListBuilder.create()
                .texOffs(27, 61).mirror(false).addBox(-1.6f, 0, -1.7f, 3.2f, 5, 3.4f), PartPose.offsetAndRotation(0, 7, 0, -0.85f, 0, 0));
        PartDefinition leftAnkle = leftShin.addOrReplaceChild("left_ankle", CubeListBuilder.create()
                .texOffs(27, 61).mirror(true).addBox(-1.6f, 0, -1.7f, 3.2f, 5, 3.4f), PartPose.offsetAndRotation(0, 7, 0, -0.85f, 0, 0));
        PartDefinition rightHoof = rightAnkle.addOrReplaceChild("right_hoof", CubeListBuilder.create()
                .texOffs(126, 61).mirror(false).addBox(-1.9f, 0, -2.6f, 1.8f, 2.4f, 3.6f)
                .texOffs(126, 61).mirror(false).addBox(0.1f, 0, -2.6f, 1.8f, 2.4f, 3.6f), PartPose.offsetAndRotation(0, 4.8f, 0, 0.25f, 0, 0));
        PartDefinition leftHoof = leftAnkle.addOrReplaceChild("left_hoof", CubeListBuilder.create()
                .texOffs(126, 61).mirror(true).addBox(0.1f, 0, -2.6f, 1.8f, 2.4f, 3.6f)
                .texOffs(126, 61).mirror(true).addBox(-1.9f, 0, -2.6f, 1.8f, 2.4f, 3.6f), PartPose.offsetAndRotation(0, 4.8f, 0, 0.25f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(21, 31).mirror(false).addBox(-5.6f, -3, -4.2f, 11.2f, 4.4f, 8.4f)
                .texOffs(132, 0).mirror(false).addBox(-6.2f, -10, -5.2f, 12.4f, 8, 10.4f)
                .texOffs(62, 0).mirror(false).addBox(-6.8f, -19, -4.8f, 13.6f, 10, 9.6f)
                .texOffs(9, 0).mirror(false).addBox(-7.2f, -19.4f, -5.4f, 14.4f, 9.6f, 10.8f)
                .texOffs(184, 31).mirror(false).addBox(-6.6f, -3.6f, -5.4f, 13.2f, 1.6f, 10.8f)
                .texOffs(64, 31).mirror(false).addBox(-7.4f, -22, -3.4f, 14.8f, 4.6f, 8.4f)
                .texOffs(113, 31).mirror(false).addBox(-1.6f, -26.4f, -1, 3.2f, 5.2f, 7.4f), PartPose.offsetAndRotation(0, 3.9773f, 0.5f, 0.28f, 0, 0));
        PartDefinition crest1 = body.addOrReplaceChild("crest_1", CubeListBuilder.create()
                .texOffs(100, 61).mirror(false).addBox(-1.4f, -4.8f, -1.2f, 2.8f, 4.8f, 2.4f), PartPose.offsetAndRotation(0, -19, 4.8f, 0.3f, 0, 0));
        PartDefinition crest2 = body.addOrReplaceChild("crest_2", CubeListBuilder.create()
                .texOffs(100, 61).mirror(false).addBox(-1.3f, -4.2f, -1.2f, 2.6f, 4.2f, 2.4f), PartPose.offsetAndRotation(0, -14, 5.4f, 0.5f, 0, 0));
        PartDefinition crest3 = body.addOrReplaceChild("crest_3", CubeListBuilder.create()
                .texOffs(227, 61).mirror(false).addBox(-1, -2.8f, -1, 2, 2.8f, 2), PartPose.offsetAndRotation(0, -9, 5.4f, 0.7f, 0, 0));
        PartDefinition kiltFront = body.addOrReplaceChild("kilt_front", CubeListBuilder.create()
                .texOffs(179, 47).mirror(false).addBox(-5, 0, -0.6f, 10, 8, 1.2f), PartPose.offsetAndRotation(0, -2, -4.6f, -0.08f, 0, 0));
        PartDefinition kiltBack = body.addOrReplaceChild("kilt_back", CubeListBuilder.create()
                .texOffs(204, 47).mirror(false).addBox(-5.2f, 0, -0.6f, 10.4f, 7.5f, 1.2f), PartPose.offsetAndRotation(0, -2, 4.4f, 0.14f, 0, 0));
        PartDefinition rightKilt = body.addOrReplaceChild("right_kilt", CubeListBuilder.create()
                .texOffs(0, 31).mirror(false).addBox(-0.6f, 0, -3.8f, 1.2f, 7, 7.6f), PartPose.offsetAndRotation(-5.6f, -2, 0, 0, 0, 0.14f));
        PartDefinition leftKilt = body.addOrReplaceChild("left_kilt", CubeListBuilder.create()
                .texOffs(0, 31).mirror(true).addBox(-0.6f, 0, -3.8f, 1.2f, 7, 7.6f), PartPose.offsetAndRotation(5.6f, -2, 0, 0, 0, -0.14f));
        PartDefinition skullCharm = body.addOrReplaceChild("skull_charm", CubeListBuilder.create()
                .texOffs(167, 61).mirror(false).addBox(-1.4f, 0.4f, -1.2f, 2.8f, 3, 2.4f)
                .texOffs(9, 72).mirror(false).addBox(-0.3f, -0.6f, -0.3f, 0.6f, 1.2f, 0.6f), PartPose.offsetAndRotation(-4.4f, -3, -5.6f, 0, 0, 0.05f));
        PartDefinition pouch = body.addOrReplaceChild("pouch", CubeListBuilder.create()
                .texOffs(139, 61).mirror(false).addBox(-1.8f, 0, -1.2f, 3.6f, 3.6f, 2.4f), PartPose.offsetAndRotation(4.6f, -3, -4.8f, 0, 0, -0.05f));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(180, 61).mirror(false).addBox(-0.5f, 0, -0.5f, 1, 5, 1)
                .texOffs(245, 61).mirror(false).addBox(-0.9f, 4.6f, -0.9f, 1.8f, 1.6f, 1.8f), PartPose.offsetAndRotation(0, -4, 4.6f, -0.3f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(138, 31).mirror(false).addBox(-2.6f, -1.4f, -2.6f, 5.2f, 8, 5.2f)
                .texOffs(87, 47).mirror(false).addBox(-3.6f, -2.6f, -3.4f, 5.6f, 3.8f, 6.8f), PartPose.offsetAndRotation(-8.3f, -17.4f, 0, -0.3f, 0, 0.18f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(138, 31).mirror(true).addBox(-2.6f, -1.4f, -2.6f, 5.2f, 8, 5.2f)
                .texOffs(87, 47).mirror(true).addBox(-2, -2.6f, -3.4f, 5.6f, 3.8f, 6.8f), PartPose.offsetAndRotation(8.3f, -17.4f, 0, -0.3f, 0, -0.18f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(235, 31).mirror(false).addBox(-2.3f, 0, -2.3f, 4.6f, 7.4f, 4.6f)
                .texOffs(114, 47).mirror(false).addBox(-2.7f, 1.2f, -2.7f, 5.4f, 5, 5.4f), PartPose.offsetAndRotation(0, 6.6f, 0, -0.8f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(235, 31).mirror(true).addBox(-2.3f, 0, -2.3f, 4.6f, 7.4f, 4.6f)
                .texOffs(114, 47).mirror(true).addBox(-2.7f, 1.2f, -2.7f, 5.4f, 5, 5.4f), PartPose.offsetAndRotation(0, 6.6f, 0, -0.8f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(44, 61).mirror(false).addBox(-2.2f, 0, -2.4f, 4.4f, 3.6f, 4.8f), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(44, 61).mirror(true).addBox(-2.2f, 0, -2.4f, 4.4f, 3.6f, 4.8f), PartPose.offsetAndRotation(0, 7.2f, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(181, 0).mirror(false).addBox(-4.6f, -6.4f, -5, 9.2f, 8, 8.4f)
                .texOffs(139, 47).mirror(false).addBox(-3.8f, -5.4f, -8.2f, 7.6f, 6.6f, 3.6f)
                .texOffs(0, 47).mirror(false).addBox(-3, -4.6f, -11.6f, 6, 5.6f, 7)
                .texOffs(185, 61).mirror(false).addBox(-2.9f, -4.4f, -12.4f, 5.8f, 4.6f, 1)
                .texOffs(65, 61).mirror(false).addBox(-5, -6.8f, -3, 10, 2, 7)
                .texOffs(27, 47).mirror(false).addBox(-5.2f, -2, -3.6f, 10.4f, 4.6f, 7), PartPose.offsetAndRotation(0, -21.4f, -3.6f, 0, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 61).mirror(false).addBox(-2.6f, 0, -7, 5.2f, 2.2f, 7), PartPose.offsetAndRotation(0, 0.8f, -4.2f, 0.05f, 0, 0));
        PartDefinition rightTusk = head.addOrReplaceChild("right_tusk", CubeListBuilder.create()
                .texOffs(200, 61).mirror(false).addBox(-0.8f, -3.6f, -0.8f, 1.6f, 4, 1.6f), PartPose.offsetAndRotation(-3, -1.4f, -10.4f, -0.25f, 0, 0.45f));
        PartDefinition leftTusk = head.addOrReplaceChild("left_tusk", CubeListBuilder.create()
                .texOffs(200, 61).mirror(true).addBox(-0.8f, -3.6f, -0.8f, 1.6f, 4, 1.6f), PartPose.offsetAndRotation(3, -1.4f, -10.4f, -0.25f, 0, -0.45f));
        PartDefinition rightTuskTip = rightTusk.addOrReplaceChild("right_tusk_tip", CubeListBuilder.create()
                .texOffs(209, 61).mirror(false).addBox(-0.6f, -3.2f, -0.6f, 1.2f, 3.4f, 1.2f), PartPose.offsetAndRotation(0, -3.4f, 0, -0.7f, 0, -0.35f));
        PartDefinition leftTuskTip = leftTusk.addOrReplaceChild("left_tusk_tip", CubeListBuilder.create()
                .texOffs(209, 61).mirror(true).addBox(-0.6f, -3.2f, -0.6f, 1.2f, 3.4f, 1.2f), PartPose.offsetAndRotation(0, -3.4f, 0, -0.7f, 0, 0.35f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(154, 61).mirror(false).addBox(-1.6f, -4, -0.6f, 3.2f, 4.2f, 1.2f), PartPose.offsetAndRotation(-4.4f, -6, -1, 0.3f, 0.4f, -0.7f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(154, 61).mirror(true).addBox(-1.6f, -4, -0.6f, 3.2f, 4.2f, 1.2f), PartPose.offsetAndRotation(4.4f, -6, -1, 0.3f, -0.4f, 0.7f));
        PartDefinition rightCheekBristle = head.addOrReplaceChild("right_cheek_bristle", CubeListBuilder.create()
                .texOffs(164, 47).mirror(false).addBox(-1.4f, -2, -2.4f, 1.6f, 5.6f, 5), PartPose.offsetAndRotation(-5, -2, -2.6f, 0, 0.3f, 0.35f));
        PartDefinition leftCheekBristle = head.addOrReplaceChild("left_cheek_bristle", CubeListBuilder.create()
                .texOffs(164, 47).mirror(true).addBox(-0.2f, -2, -2.4f, 1.6f, 5.6f, 5), PartPose.offsetAndRotation(5, -2, -2.6f, 0, -0.3f, -0.35f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(14, 72).mirror(false).addBox(-0.8f, -0.5f, -0.4f, 1.6f, 1.1f, 0.6f), PartPose.offsetAndRotation(-3.1f, -4.4f, -5, 0, 0.3f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(14, 72).mirror(true).addBox(-0.8f, -0.5f, -0.4f, 1.6f, 1.1f, 0.6f), PartPose.offsetAndRotation(3.1f, -4.4f, -5, 0, -0.3f, 0));
        PartDefinition axe = rightHand.addOrReplaceChild("axe", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.6f, -20, -0.6f, 1.2f, 28, 1.2f)
                .texOffs(218, 61).mirror(false).addBox(-0.8f, 2, -0.8f, 1.6f, 4, 1.6f)
                .texOffs(111, 0).mirror(false).addBox(-0.45f, -25.5f, -8.6f, 0.9f, 11, 8.6f)
                .texOffs(113, 61).mirror(false).addBox(-0.9f, -21, -1.6f, 1.8f, 3.6f, 3.2f)
                .texOffs(236, 61).mirror(false).addBox(-0.4f, -22, 0.6f, 0.8f, 1.6f, 2.4f)
                .texOffs(0, 72).mirror(false).addBox(-0.8f, 7.8f, -0.8f, 1.6f, 1.2f, 1.6f), PartPose.offsetAndRotation(0, 1.8f, 0, 1.1634f, 0.1602f, -0.0824f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED boar: end
    }

    /** The boar's pose: hunched, a heavy stride; pawing, his head down and a hind leg scraping; charging, bent low and running; a blow, the axe heaved up and down. */
    public static final class BoarModel extends EntityModel<BoarDemonEntity> {
        private final ModelPart root, body, head, jaw, rightLeg, leftLeg, rightShin, leftShin, rightArm, leftArm, rightForearm, rightEar, leftEar,
                kiltFront, kiltBack, tail;

        public BoarModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
            kiltFront = body.getChild("kilt_front");
            kiltBack = body.getChild("kilt_back");
            tail = body.getChild("tail");
        }

        @Override
        public void setupAnim(BoarDemonEntity e, float swing, float amount, float age, float yaw, float pitch) {
            int s = e.state();
            boolean charging = s == BoarDemonEntity.CHARGING, pawing = s == BoarDemonEntity.PAWING;
            float w = charging ? age * 0.9f : swing * 0.62f, reach = charging ? 1.1f : 0.9f * amount;
            float stride = Mth.cos(w) * reach;
            float chop = 0;
            if (attackTime > 0) chop = attackTime < 0.35f ? -2.2f * attackTime / 0.35f : -2.2f + 2.8f * (attackTime - 0.35f) / 0.65f;
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            turn(rightShin, Math.max(0, -stride) * 0.5f, 0, 0);
            turn(leftShin, Math.max(0, stride) * 0.5f, 0, 0);
            if (pawing) { // a hind leg scraping back, the dirt flying
                float scrape = Mth.sin(age * 0.9f);
                turn(rightLeg, 0.3f + scrape * 0.6f, 0, 0);
                turn(rightShin, 0.3f, 0, 0);
                turn(leftLeg, -0.1f, 0, 0);
            }
            float lean = charging ? 0.45f : pawing ? 0.3f : 0;
            turn(body, lean + (chop > 0 ? chop * 0.1f : 0), 0, 0);
            body.y = body.getInitialPose().y + Math.abs(Mth.sin(w)) * (charging ? 1.2f : 0.6f * amount);
            turn(head, pitch * Mth.DEG_TO_RAD * 0.6f + (charging || pawing ? 0.35f : 0) - lean * 0.6f, yaw * Mth.DEG_TO_RAD * 0.6f, 0);
            turn(jaw, attackTime > 0 || pawing ? 0.3f : Math.max(0, Mth.sin(age * 0.06f)) * 0.06f, 0, 0);
            float flick = Mth.sin(age * 0.3f) * 0.1f;
            turn(rightEar, charging ? 0.5f : 0, 0, flick);
            turn(leftEar, charging ? 0.5f : 0, 0, -flick);
            turn(rightArm, (charging ? 0.5f : -stride * 0.25f) + chop, 0, 0);
            turn(rightForearm, chop < 0 ? -chop * 0.25f : 0, 0, 0);
            turn(leftArm, charging ? 0.6f : stride * 0.6f, 0, 0);
            turn(kiltFront, -Math.abs(stride) * 0.4f, 0, 0);
            turn(kiltBack, Math.abs(stride) * 0.3f + (charging ? 0.4f : 0), 0, 0);
            turn(tail, Mth.sin(age * 0.4f) * 0.3f, Mth.sin(age * 0.3f) * 0.4f, 0);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    // ── The Black-Wind Imp ──────────────────────────────────────────────

    /** The Black-Wind Imp (256x256): model and skins from {@code assets_source/black_wind_folk.py}. */
    public static LayerDefinition impLayer() {
        // GENERATED imp: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(148, 0).mirror(false).addBox(-1.4f, -0.5f, -1.5f, 2.8f, 5, 3), PartPose.offsetAndRotation(-2.2f, 14.0905f, 0.5f, -0.3f, 0, 0.18f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(148, 0).mirror(true).addBox(-1.4f, -0.5f, -1.5f, 2.8f, 5, 3), PartPose.offsetAndRotation(2.2f, 14.0905f, 0.5f, -0.3f, 0, -0.18f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(161, 0).mirror(false).addBox(-1.1f, 0, -1.1f, 2.2f, 4.6f, 2.2f), PartPose.offsetAndRotation(0, 4.4f, 0, 0.55f, 0, -0.1f));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(161, 0).mirror(true).addBox(-1.1f, 0, -1.1f, 2.2f, 4.6f, 2.2f), PartPose.offsetAndRotation(0, 4.4f, 0, 0.55f, 0, 0.1f));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(25, 17).mirror(false).addBox(-1.4f, 0, -3, 2.8f, 1.4f, 4)
                .texOffs(129, 17).mirror(false).addBox(-1.4f, 0.4f, -4.4f, 0.8f, 1, 1.6f)
                .texOffs(129, 17).mirror(false).addBox(0.6f, 0.4f, -4.4f, 0.8f, 1, 1.6f), PartPose.offsetAndRotation(0, 4.4f, 0, -0.25f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(25, 17).mirror(true).addBox(-1.4f, 0, -3, 2.8f, 1.4f, 4)
                .texOffs(129, 17).mirror(true).addBox(0.6f, 0.4f, -4.4f, 0.8f, 1, 1.6f)
                .texOffs(129, 17).mirror(true).addBox(-1.4f, 0.4f, -4.4f, 0.8f, 1, 1.6f), PartPose.offsetAndRotation(0, 4.4f, 0, -0.25f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(35, 0).mirror(false).addBox(-3.2f, -4, -2.8f, 6.4f, 4.8f, 5.6f)
                .texOffs(85, 0).mirror(false).addBox(-3.6f, -8.6f, -2.6f, 7.2f, 5, 5)
                .texOffs(66, 17).mirror(false).addBox(-1.4f, -9.8f, 1, 2.8f, 1.6f, 2.4f)
                .texOffs(174, 0).mirror(false).addBox(-3.8f, -1.2f, -3, 7.6f, 1.8f, 6), PartPose.offsetAndRotation(0, 14.0905f, 0.5f, 0.35f, 0, 0));
        PartDefinition loincloth = body.addOrReplaceChild("loincloth", CubeListBuilder.create()
                .texOffs(40, 17).mirror(false).addBox(-2.2f, 0, -0.3f, 4.4f, 4.5f, 0.6f), PartPose.offsetAndRotation(0, 0.4f, -2.6f, -0.1f, 0, 0));
        PartDefinition gourd = body.addOrReplaceChild("gourd", CubeListBuilder.create()
                .texOffs(53, 17).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 2.6f, 2.4f)
                .texOffs(115, 17).mirror(false).addBox(-0.8f, -1.4f, -0.8f, 1.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(3.6f, -1, -1, 0, 0, -0.2f));
        PartDefinition scarf = body.addOrReplaceChild("scarf", CubeListBuilder.create()
                .texOffs(121, 0).mirror(false).addBox(-3.4f, -1.2f, -3, 6.8f, 2.4f, 6), PartPose.offsetAndRotation(0, -8.8f, 0, 0, 0, 0));
        PartDefinition scarfTail0 = scarf.addOrReplaceChild("scarf_tail_0", CubeListBuilder.create()
                .texOffs(203, 0).mirror(false).addBox(-1, 0, -0.2f, 2, 7, 0.4f), PartPose.offsetAndRotation(-1.6f, 0, 2.8f, 1.2f, 0.2f, 0));
        PartDefinition scarfTail1 = scarf.addOrReplaceChild("scarf_tail_1", CubeListBuilder.create()
                .texOffs(112, 0).mirror(false).addBox(-1.1f, 0, -0.2f, 2.2f, 9, 0.4f), PartPose.offsetAndRotation(0.4f, 0.4f, 2.9f, 1.35f, -0.1f, 0));
        PartDefinition scarfTail2 = scarf.addOrReplaceChild("scarf_tail_2", CubeListBuilder.create()
                .texOffs(228, 0).mirror(false).addBox(-0.9f, 0, -0.2f, 1.8f, 6, 0.4f), PartPose.offsetAndRotation(2, 0, 2.8f, 1.1f, -0.3f, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(210, 0).mirror(false).addBox(-1, -0.6f, -1, 2, 6, 2), PartPose.offsetAndRotation(-4.2f, -7.8f, 0, -0.35f, 0, 0.35f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(210, 0).mirror(true).addBox(-1, -0.6f, -1, 2, 6, 2), PartPose.offsetAndRotation(4.2f, -7.8f, 0, -0.35f, 0, -0.35f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(219, 0).mirror(false).addBox(-0.9f, 0, -0.9f, 1.8f, 6, 1.8f), PartPose.offsetAndRotation(0, 5.6f, 0, -0.5f, 0, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(219, 0).mirror(true).addBox(-0.9f, 0, -0.9f, 1.8f, 6, 1.8f), PartPose.offsetAndRotation(0, 5.6f, 0, -0.5f, 0, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(79, 17).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 1.6f, 2.4f)
                .texOffs(124, 17).mirror(false).addBox(-1.2f, 1.4f, -1.2f, 0.6f, 3, 0.6f)
                .texOffs(92, 17).mirror(false).addBox(-0.3f, 1.4f, -1.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(124, 17).mirror(false).addBox(0.6f, 1.4f, -1.2f, 0.6f, 3, 0.6f), PartPose.offsetAndRotation(0, 5.8f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(79, 17).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 1.6f, 2.4f)
                .texOffs(124, 17).mirror(true).addBox(0.6f, 1.4f, -1.2f, 0.6f, 3, 0.6f)
                .texOffs(92, 17).mirror(true).addBox(-0.3f, 1.4f, -1.3f, 0.6f, 3.4f, 0.6f)
                .texOffs(124, 17).mirror(true).addBox(-1.2f, 1.4f, -1.2f, 0.6f, 3, 0.6f), PartPose.offsetAndRotation(0, 5.8f, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-4.4f, -8, -4.4f, 8.8f, 8, 8)
                .texOffs(97, 17).mirror(false).addBox(-1, -4.6f, -5.6f, 2, 2.6f, 1.4f)
                .texOffs(136, 17).mirror(false).addBox(-3.8f, -1.2f, -4.8f, 7.6f, 1.6f, 1), PartPose.offsetAndRotation(0, -9.6f, -1.4f, -0.3f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 17).mirror(false).addBox(-3.4f, 0, -3, 6.8f, 1.8f, 4.6f), PartPose.offsetAndRotation(0, 0, -1.8f, 0.08f, 0, 0));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(106, 17).mirror(false).addBox(-0.6f, -2, -0.6f, 1.2f, 2.2f, 1.2f), PartPose.offsetAndRotation(-2.2f, -8, -1.6f, -0.2f, 0, -0.35f));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(106, 17).mirror(true).addBox(-0.6f, -2, -0.6f, 1.2f, 2.2f, 1.2f), PartPose.offsetAndRotation(2.2f, -8, -1.6f, -0.2f, 0, 0.35f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(62, 0).mirror(false).addBox(-10, -5.5f, -0.3f, 10, 9.5f, 0.6f), PartPose.offsetAndRotation(-4.2f, -5.6f, 0, 0, -0.45f, 0.3f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(62, 0).mirror(true).addBox(0, -5.5f, -0.3f, 10, 9.5f, 0.6f), PartPose.offsetAndRotation(4.2f, -5.6f, 0, 0, 0.45f, -0.3f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(155, 17).mirror(false).addBox(-1.3f, -0.9f, -0.3f, 2.6f, 2, 0.5f), PartPose.offsetAndRotation(-2.2f, -4.8f, -4.5f, 0, 0.15f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(155, 17).mirror(true).addBox(-1.3f, -0.9f, -0.3f, 2.6f, 2, 0.5f), PartPose.offsetAndRotation(2.2f, -4.8f, -4.5f, 0, -0.15f, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED imp: end
    }

    /** The imp's pose: a scuttling crouch, long arms swinging, ears twitching, the scarf fluttering; spinning up its gust it whirls on the spot, arms flung out; riding it, it lies along the wind, arms back, the scarf streaming. */
    public static final class ImpModel extends EntityModel<WindImpEntity> {
        private final ModelPart root, body, head, jaw, rightLeg, leftLeg, rightArm, leftArm, rightForearm, rightEar, leftEar;
        private final List<ModelPart> scarf = new ArrayList<>();

        public ImpModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
            ModelPart s = body.getChild("scarf");
            for (int k = 0; s.hasChild("scarf_tail_" + k); k++) scarf.add(s.getChild("scarf_tail_" + k));
        }

        @Override
        public void setupAnim(WindImpEntity e, float swing, float amount, float age, float yaw, float pitch) {
            int s = e.state();
            float w = swing * 0.9f, stride = Mth.cos(w) * amount;
            float claw = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            root.yRot = s == WindImpEntity.WHIRLING ? age * 1.4f : 0;
            float lean = s == WindImpEntity.RIDING ? 0.7f : 0;
            turn(body, lean + Math.abs(Mth.sin(w)) * 0.08f * amount, 0, 0);
            body.y = body.getInitialPose().y + Math.abs(Mth.sin(w)) * amount * 0.8f;
            turn(head, pitch * Mth.DEG_TO_RAD - lean * 0.8f, yaw * Mth.DEG_TO_RAD, Mth.sin(age * 0.1f) * 0.05f);
            turn(jaw, claw * 0.4f + (s != WindImpEntity.IDLE ? 0.35f : 0), 0, 0);
            float out = s == WindImpEntity.WHIRLING ? 1.1f : 0, back = s == WindImpEntity.RIDING ? 1.3f : 0;
            turn(rightArm, -stride * 0.8f - claw * 1.6f + back, 0, out);
            turn(leftArm, stride * 0.8f + back, 0, -out);
            turn(rightForearm, -claw * 0.5f, 0, 0);
            float twitch = Mth.sin(age * 0.35f) * Mth.sin(age * 0.07f) * 0.15f;
            turn(rightEar, 0, twitch + back * 0.4f, 0);
            turn(leftEar, 0, -twitch - back * 0.4f, 0);
            float stream = s == WindImpEntity.IDLE ? amount * 0.4f : 0.5f;
            for (int k = 0; k < scarf.size(); k++) {
                turn(scarf.get(k), -stream + Mth.sin(age * 0.5f + k * 1.3f) * (0.12f + stream * 0.3f), Mth.sin(age * 0.3f + k) * 0.15f, 0);
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    // ── Lingxuzi ────────────────────────────────────────────────────────

    /** Lingxuzi (256x256): model and skin from {@code assets_source/black_wind_folk.py}. */
    public static LayerDefinition wolfLayer() {
        // GENERATED wolf: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(100, 0).mirror(false).addBox(-2, 0, -2, 4, 13, 4), PartPose.offsetAndRotation(-2.6f, 8.4f, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(100, 0).mirror(true).addBox(-2, 0, -2, 4, 13, 4), PartPose.offsetAndRotation(2.6f, 8.4f, 0, 0, 0, 0));
        PartDefinition rightShoe = rightLeg.addOrReplaceChild("right_shoe", CubeListBuilder.create()
                .texOffs(59, 37).mirror(false).addBox(-2.2f, 0, -3.6f, 4.4f, 2.6f, 5.8f)
                .texOffs(42, 48).mirror(false).addBox(-1.2f, -0.8f, -4.4f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(0, 13, 0, 0, 0, 0));
        PartDefinition leftShoe = leftLeg.addOrReplaceChild("left_shoe", CubeListBuilder.create()
                .texOffs(59, 37).mirror(true).addBox(-2.2f, 0, -3.6f, 4.4f, 2.6f, 5.8f)
                .texOffs(42, 48).mirror(true).addBox(-1.2f, -0.8f, -4.4f, 2.4f, 2.2f, 1.2f), PartPose.offsetAndRotation(0, 13, 0, 0, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-5, -15, -3.4f, 10, 15, 6.8f)
                .texOffs(133, 23).mirror(false).addBox(-5.4f, -6.4f, -3.8f, 10.8f, 2.6f, 7.6f)
                .texOffs(53, 48).mirror(false).addBox(-1.6f, -7.2f, -4.4f, 3.2f, 3.6f, 0.8f)
                .texOffs(82, 37).mirror(false).addBox(-3.4f, -16.4f, -2.6f, 6.8f, 2.6f, 5.4f), PartPose.offsetAndRotation(0, 8.4f, 0, 0.06f, 0, 0));
        PartDefinition collarRight = body.addOrReplaceChild("collar_right", CubeListBuilder.create()
                .texOffs(172, 23).mirror(false).addBox(-1.2f, 0, -0.3f, 2.4f, 9.6f, 0.6f), PartPose.offsetAndRotation(-1.2f, -15.2f, -3.6f, 0, 0, -0.55f));
        PartDefinition collarLeft = body.addOrReplaceChild("collar_left", CubeListBuilder.create()
                .texOffs(172, 23).mirror(false).addBox(-1.2f, 0, -0.3f, 2.4f, 9.6f, 0.6f), PartPose.offsetAndRotation(1.2f, -15.2f, -3.7f, 0, 0, 0.55f));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(117, 0).mirror(false).addBox(-5.6f, 0, -3.8f, 11.2f, 8, 7.6f)
                .texOffs(35, 0).mirror(false).addBox(-6, 7.6f, -4.2f, 12, 9.6f, 8.4f), PartPose.offsetAndRotation(0, -4, 0, 0, 0, 0));
        PartDefinition skirtFront = skirt.addOrReplaceChild("skirt_front", CubeListBuilder.create()
                .texOffs(78, 0).mirror(false).addBox(-2.8f, 0, -0.4f, 5.6f, 17.4f, 0.8f), PartPose.offsetAndRotation(0, -0.4f, -3.9f, -0.08f, 0, 0));
        PartDefinition sashTail = body.addOrReplaceChild("sash_tail", CubeListBuilder.create()
                .texOffs(181, 23).mirror(false).addBox(-0.9f, 0, -0.2f, 1.8f, 10, 0.4f)
                .texOffs(95, 48).mirror(false).addBox(-1, 10, -0.3f, 2, 2.4f, 0.6f), PartPose.offsetAndRotation(2, -5.2f, -4.3f, -0.06f, 0, -0.05f));
        PartDefinition beltGourd = body.addOrReplaceChild("belt_gourd", CubeListBuilder.create()
                .texOffs(109, 37).mirror(false).addBox(-1.8f, 0, -1.8f, 3.6f, 4.2f, 3.6f)
                .texOffs(231, 37).mirror(false).addBox(-1.2f, -2.6f, -1.2f, 2.4f, 2.8f, 2.4f)
                .texOffs(165, 48).mirror(false).addBox(-0.5f, -3.4f, -0.5f, 1, 1, 1), PartPose.offsetAndRotation(-5.4f, -5, -1, 0, 0, 0.1f));
        PartDefinition pendant = body.addOrReplaceChild("pendant", CubeListBuilder.create()
                .texOffs(102, 48).mirror(false).addBox(-0.25f, 0, -0.25f, 0.5f, 2.6f, 0.5f)
                .texOffs(107, 48).mirror(false).addBox(-1.2f, 2.6f, -0.4f, 2.4f, 2.4f, 0.8f), PartPose.offsetAndRotation(3.6f, -5, -4.2f, 0, 0, 0));
        PartDefinition mantle = body.addOrReplaceChild("mantle", CubeListBuilder.create()
                .texOffs(0, 23).mirror(false).addBox(-6.8f, -0.6f, -3.9f, 13.6f, 4.4f, 7.9f), PartPose.offsetAndRotation(0, -15.6f, 0.2f, 0.05f, 0, 0));
        PartDefinition mantleBack = body.addOrReplaceChild("mantle_back", CubeListBuilder.create()
                .texOffs(0, 37).mirror(false).addBox(-6.6f, 0, -0.4f, 13.2f, 9, 0.8f), PartPose.offsetAndRotation(0, -12.6f, 3.6f, 0.1f, 0, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(163, 37).mirror(false).addBox(-1.5f, -1.5f, 0, 3, 3, 5), PartPose.offsetAndRotation(0, -3.4f, 3.6f, -0.9f, 0, 0));
        PartDefinition tailMid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create()
                .texOffs(126, 37).mirror(false).addBox(-1.9f, -1.9f, 0, 3.8f, 3.8f, 5), PartPose.offsetAndRotation(0, 0, 4.6f, -0.4f, 0, 0));
        PartDefinition tailTip = tailMid.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(180, 37).mirror(false).addBox(-1.4f, -1.4f, 0, 2.8f, 2.8f, 4), PartPose.offsetAndRotation(0, 0, 4.6f, 0.4f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(188, 23).mirror(false).addBox(-1.7f, -1, -1.7f, 3.4f, 7, 3.4f), PartPose.offsetAndRotation(-6, -13.6f, 0, -0.15f, 0, 0.12f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(188, 23).mirror(true).addBox(-1.7f, -1, -1.7f, 3.4f, 7, 3.4f), PartPose.offsetAndRotation(6, -13.6f, 0, -0.3f, 0, -0.18f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(145, 37).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 6, 3), PartPose.offsetAndRotation(0, 6.4f, 0, -0.95f, -0.25f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(145, 37).mirror(true).addBox(-1.5f, 0, -1.5f, 3, 6, 3), PartPose.offsetAndRotation(0, 6.4f, 0, -1.25f, 0.4f, 0));
        PartDefinition rightSleeve = rightForearm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(158, 0).mirror(false).addBox(-2.3f, -1, -5.4f, 4.6f, 9.5f, 6), PartPose.offsetAndRotation(0, 0.4f, 0.6f, 1.1362f, 0.1478f, 0.2112f));
        PartDefinition leftSleeve = leftForearm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(158, 0).mirror(true).addBox(-2.3f, -1, -5.4f, 4.6f, 9.5f, 6), PartPose.offsetAndRotation(0, 0.4f, 0.6f, 1.5956f, -0.1833f, -0.389f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(244, 37).mirror(false).addBox(-1.4f, 0, -1.5f, 2.8f, 2.6f, 3)
                .texOffs(170, 48).mirror(false).addBox(-1.3f, 2.4f, -1.5f, 2.6f, 0.8f, 0.6f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(244, 37).mirror(true).addBox(-1.4f, 0, -1.5f, 2.8f, 2.6f, 3)
                .texOffs(170, 48).mirror(true).addBox(-1.3f, 2.4f, -1.5f, 2.6f, 0.8f, 0.6f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(181, 0).mirror(false).addBox(-4, -7.6f, -4, 8, 7.4f, 7.2f)
                .texOffs(45, 23).mirror(false).addBox(-2.3f, -5, -12.4f, 4.6f, 3.4f, 8.6f)
                .texOffs(0, 48).mirror(false).addBox(-1.7f, -4.7f, -14.6f, 3.4f, 2.8f, 2.4f)
                .texOffs(116, 48).mirror(false).addBox(-1.2f, -5.2f, -15.3f, 2.4f, 1.6f, 0.9f)
                .texOffs(205, 23).mirror(false).addBox(-4.2f, -7.8f, 1.4f, 8.4f, 7.4f, 2.2f), PartPose.offsetAndRotation(0, -16.2f, -1.4f, 0, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(74, 23).mirror(false).addBox(-2, 0, -10.4f, 4, 1.8f, 10.4f), PartPose.offsetAndRotation(0, -1.6f, -3.8f, 0.06f, 0, 0));
        PartDefinition beard = jaw.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(105, 23).mirror(false).addBox(-1.6f, 0, -1.2f, 3.2f, 8.5f, 2.4f), PartPose.offsetAndRotation(0, 1.4f, -4.6f, 0.15f, 0, 0));
        PartDefinition beardTip = beard.addOrReplaceChild("beard_tip", CubeListBuilder.create()
                .texOffs(15, 48).mirror(false).addBox(-0.9f, 0, -0.8f, 1.8f, 4, 1.6f), PartPose.offsetAndRotation(0, 6.6f, 0, -0.1f, 0, 0));
        PartDefinition rightBeard = jaw.addOrReplaceChild("right_beard", CubeListBuilder.create()
                .texOffs(31, 37).mirror(false).addBox(-1.1f, 0, -0.9f, 2.2f, 7.5f, 1.8f), PartPose.offsetAndRotation(-1.8f, 1, -2.4f, 0.1f, 0, 0.2f));
        PartDefinition leftBeard = jaw.addOrReplaceChild("left_beard", CubeListBuilder.create()
                .texOffs(31, 37).mirror(true).addBox(-1.1f, 0, -0.9f, 2.2f, 7.5f, 1.8f), PartPose.offsetAndRotation(1.8f, 1, -2.4f, 0.1f, 0, -0.2f));
        PartDefinition rightCheekfur = head.addOrReplaceChild("right_cheekfur", CubeListBuilder.create()
                .texOffs(42, 37).mirror(false).addBox(-2.2f, -2, -1.2f, 2.6f, 4.4f, 4.2f), PartPose.offsetAndRotation(-3.7f, -3.2f, -1.6f, 0, 0.5f, 0.3f));
        PartDefinition leftCheekfur = head.addOrReplaceChild("left_cheekfur", CubeListBuilder.create()
                .texOffs(42, 37).mirror(true).addBox(-0.4f, -2, -1.2f, 2.6f, 4.4f, 4.2f), PartPose.offsetAndRotation(3.7f, -3.2f, -1.6f, 0, -0.5f, -0.3f));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(64, 48).mirror(false).addBox(-2, -0.6f, -1.2f, 3.6f, 1.2f, 2.2f), PartPose.offsetAndRotation(-2.1f, -6.4f, -4.1f, 0.35f, 0.3f, 0.35f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(64, 48).mirror(true).addBox(-1.6f, -0.6f, -1.2f, 3.6f, 1.2f, 2.2f), PartPose.offsetAndRotation(2.1f, -6.4f, -4.1f, 0.35f, -0.3f, -0.35f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(125, 48).mirror(false).addBox(-1.2f, -0.55f, -0.3f, 2.4f, 1.1f, 0.45f), PartPose.offsetAndRotation(-2.3f, -5.4f, -4.1f, 0, 0.32f, -0.1f));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(125, 48).mirror(true).addBox(-1.2f, -0.55f, -0.3f, 2.4f, 1.1f, 0.45f), PartPose.offsetAndRotation(2.3f, -5.4f, -4.1f, 0, -0.32f, 0.1f));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(24, 48).mirror(false).addBox(-1.8f, -3.6f, -0.8f, 3.6f, 3.8f, 1.6f)
                .texOffs(79, 48).mirror(false).addBox(-1.1f, -6, -0.6f, 2.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(-2.5f, -7.2f, 0.4f, -0.35f, 0.15f, -0.28f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(24, 48).mirror(true).addBox(-1.8f, -3.6f, -0.8f, 3.6f, 3.8f, 1.6f)
                .texOffs(79, 48).mirror(true).addBox(-1.1f, -6, -0.6f, 2.2f, 2.6f, 1.2f), PartPose.offsetAndRotation(2.5f, -7.2f, 0.4f, -0.35f, -0.15f, 0.28f));
        PartDefinition topknot = head.addOrReplaceChild("topknot", CubeListBuilder.create()
                .texOffs(195, 37).mirror(false).addBox(-1.6f, -2.6f, -1.6f, 3.2f, 2.8f, 3.2f)
                .texOffs(212, 37).mirror(false).addBox(-2, -3.6f, -2.2f, 4, 1.4f, 4.4f)
                .texOffs(179, 48).mirror(false).addBox(-2.8f, -2.8f, -0.3f, 5.6f, 0.6f, 0.6f), PartPose.offsetAndRotation(0, -7.8f, 0.4f, 0, 0, 0));
        PartDefinition sword = rightHand.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(37, 48).mirror(false).addBox(-0.5f, -2.2f, -0.5f, 1, 4.4f, 1)
                .texOffs(134, 48).mirror(false).addBox(-2, -2.8f, -0.7f, 4, 0.8f, 1.4f)
                .texOffs(147, 48).mirror(false).addBox(-0.7f, 2, -0.7f, 1.4f, 1, 1.4f)
                .texOffs(93, 0).mirror(false).addBox(-0.7f, -19.8f, -0.2f, 1.4f, 17, 0.4f), PartPose.offsetAndRotation(0, 1.6f, -0.4f, 2.9941f, 0.2519f, 0.1503f));
        PartDefinition swordTassel = sword.addOrReplaceChild("sword_tassel", CubeListBuilder.create()
                .texOffs(90, 48).mirror(false).addBox(-0.4f, 0, -0.4f, 0.8f, 4, 0.8f), PartPose.offsetAndRotation(0, 3, 0, 0.2f, 0, 0));
        PartDefinition whisk = leftHand.addOrReplaceChild("whisk", CubeListBuilder.create()
                .texOffs(158, 37).mirror(false).addBox(-0.4f, -6, -0.4f, 0.8f, 8, 0.8f)
                .texOffs(156, 48).mirror(false).addBox(-0.6f, -6.6f, -0.6f, 1.2f, 1, 1.2f), PartPose.offsetAndRotation(0, 1.4f, 0, 1.1088f, -0.5028f, -0.3692f));
        PartDefinition whiskHair = whisk.addOrReplaceChild("whisk_hair", CubeListBuilder.create()
                .texOffs(120, 23).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 9, 2.4f), PartPose.offsetAndRotation(0, -6.4f, 0, 2.4f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED wolf: end
    }

    /** Lingxuzi's pose: the measured step of an old Daoist, sword low, whisk in the crook of his arm; crouching to spring, bent low, the sword back; springing, the sword out before him; drinking, his left paw to his muzzle, head back; a blow, the sword thrust. */
    public static final class WolfModel extends EntityModel<LingxuziEntity> {
        private final ModelPart root, body, head, jaw, beard, rightLeg, leftLeg, rightArm, leftArm, rightForearm, leftForearm, rightSleeve, leftSleeve,
                skirt, tail, tailMid, mantleBack, sashTail;

        public WolfModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            beard = jaw.getChild("beard");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightSleeve = rightForearm.getChild("right_sleeve");
            leftSleeve = leftForearm.getChild("left_sleeve");
            skirt = body.getChild("skirt");
            tail = body.getChild("tail");
            tailMid = tail.getChild("tail_mid");
            mantleBack = body.getChild("mantle_back");
            sashTail = body.getChild("sash_tail");
        }

        @Override
        public void setupAnim(LingxuziEntity e, float swing, float amount, float age, float yaw, float pitch) {
            int s = e.state();
            float w = swing * 0.6f, stride = Mth.cos(w) * 0.6f * amount;
            float thrust = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float breath = Mth.sin(age * 0.06f);
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            float bx = 0, hx = pitch * Mth.DEG_TO_RAD, rx = -stride * 0.3f - thrust * 1.3f, rz = 0, lx = stride * 0.3f, lz = 0, rfx = thrust * 0.7f, lfx = 0, drop = 0;
            if (s == LingxuziEntity.CROUCHING) {
                bx = 0.55f;
                drop = 3.5f;
                rx = 0.7f;
                rz = 0.3f;
                lx = -0.6f;
                hx = -0.4f;
            } else if (s == LingxuziEntity.LEAPING) {
                bx = 0.35f;
                rx = -2.3f;
                rfx = 0.4f;
                lx = 0.8f;
                lz = -0.4f;
                hx = -0.3f;
            } else if (s == LingxuziEntity.DRINKING) {
                lx = -2.0f;
                lz = 0.5f;
                lfx = -0.6f;
                hx = -0.6f;
                bx = -0.12f;
            }
            root.y = drop;
            turn(body, bx + breath * 0.01f, Mth.cos(w) * 0.05f * amount, 0);
            turn(head, hx, yaw * Mth.DEG_TO_RAD, 0);
            turn(jaw, s == LingxuziEntity.DRINKING ? 0.4f : thrust * 0.25f, 0, 0);
            turn(beard, amount * 0.15f + Mth.sin(age * 0.05f) * 0.04f, 0, 0);
            turn(rightArm, rx, 0, rz);
            turn(leftArm, lx, 0, lz);
            turn(rightForearm, rfx, 0, 0);
            turn(leftForearm, lfx, 0, 0);
            // the great sleeves hang under their weight
            turn(rightSleeve, -(rx + rfx) * 0.7f + Mth.sin(age * 0.05f) * 0.04f + amount * 0.2f, 0, 0);
            turn(leftSleeve, -(lx + lfx) * 0.7f + Mth.sin(age * 0.05f + 1) * 0.04f + amount * 0.2f, 0, 0);
            turn(skirt, -bx * 0.8f, 0, 0);
            float sway = Mth.sin(age * 0.07f) * 0.12f + Mth.sin(w * 0.5f) * 0.25f * amount;
            turn(tail, amount * 0.3f + (s == LingxuziEntity.LEAPING ? 0.6f : 0), sway, 0);
            turn(tailMid, amount * 0.1f, sway * 0.7f, 0);
            turn(mantleBack, amount * 0.35f + breath * 0.03f, 0, 0);
            turn(sashTail, -Math.abs(stride) * 0.3f, 0, Mth.sin(age * 0.08f) * 0.05f);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    // ── The White-Robed Scholar ─────────────────────────────────────────

    /** The White-Robed Scholar (256x256): model and skin from {@code assets_source/black_wind_folk.py}. */
    public static LayerDefinition scholarLayer() {
        // GENERATED scholar: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(71, 0).mirror(false).addBox(-1.6f, 0, -1.6f, 3.2f, 14, 3.2f), PartPose.offsetAndRotation(-2.2f, 8.8f, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(71, 0).mirror(true).addBox(-1.6f, 0, -1.6f, 3.2f, 14, 3.2f), PartPose.offsetAndRotation(2.2f, 8.8f, 0, 0, 0, 0));
        PartDefinition rightShoe = rightLeg.addOrReplaceChild("right_shoe", CubeListBuilder.create()
                .texOffs(46, 50).mirror(false).addBox(-1.8f, 0, -3.6f, 3.6f, 1.2f, 5)
                .texOffs(215, 50).mirror(false).addBox(-1, -0.6f, -4.1f, 2, 1.4f, 0.8f), PartPose.offsetAndRotation(0, 14, 0, 0, 0, 0));
        PartDefinition leftShoe = leftLeg.addOrReplaceChild("left_shoe", CubeListBuilder.create()
                .texOffs(46, 50).mirror(true).addBox(-1.8f, 0, -3.6f, 3.6f, 1.2f, 5)
                .texOffs(215, 50).mirror(true).addBox(-1, -0.6f, -4.1f, 2, 1.4f, 0.8f), PartPose.offsetAndRotation(0, 14, 0, 0, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-4.4f, -15, -2.8f, 8.8f, 15, 5.6f)
                .texOffs(65, 50).mirror(false).addBox(-2, -15.2f, -3.1f, 4, 6, 0.4f)
                .texOffs(174, 22).mirror(false).addBox(-4.9f, -7, -3.3f, 9.8f, 3.2f, 6.6f)
                .texOffs(160, 50).mirror(false).addBox(-1.4f, -7.6f, -3.9f, 2.8f, 3.6f, 0.8f)
                .texOffs(112, 50).mirror(false).addBox(-1.4f, -17.2f, -1.4f, 2.8f, 2.8f, 2.8f)
                .texOffs(204, 38).mirror(false).addBox(-4.6f, -15.4f, -2.9f, 9.2f, 1.2f, 5.8f), PartPose.offsetAndRotation(0, 8.8f, 0, 0, 0, 0));
        PartDefinition scollarRight = body.addOrReplaceChild("scollar_right", CubeListBuilder.create()
                .texOffs(209, 22).mirror(false).addBox(-1.3f, 0, -0.3f, 2.6f, 9.4f, 0.6f), PartPose.offsetAndRotation(-0.6f, -15.3f, -3, 0, 0, -0.52f));
        PartDefinition scollarLeft = body.addOrReplaceChild("scollar_left", CubeListBuilder.create()
                .texOffs(209, 22).mirror(false).addBox(-1.3f, 0, -0.3f, 2.6f, 9.4f, 0.6f), PartPose.offsetAndRotation(0.6f, -15.3f, -3.1f, 0, 0, 0.52f));
        PartDefinition skirt = body.addOrReplaceChild("skirt", CubeListBuilder.create()
                .texOffs(210, 0).mirror(false).addBox(-5.2f, 0, -3.4f, 10.4f, 8, 6.8f)
                .texOffs(117, 0).mirror(false).addBox(-6.2f, 7.6f, -4.2f, 12.4f, 6.4f, 8.4f)
                .texOffs(0, 22).mirror(false).addBox(-7, 13.6f, -4.8f, 14, 4.6f, 9.6f), PartPose.offsetAndRotation(0, -4.2f, 0, 0, 0, 0));
        PartDefinition skirtFront = skirt.addOrReplaceChild("skirt_front", CubeListBuilder.create()
                .texOffs(31, 0).mirror(false).addBox(-2.2f, 0, -0.3f, 4.4f, 18, 0.6f), PartPose.offsetAndRotation(0, 0.2f, -3.6f, -0.06f, 0, 0));
        PartDefinition skirtBack = skirt.addOrReplaceChild("skirt_back", CubeListBuilder.create()
                .texOffs(44, 0).mirror(false).addBox(-5.6f, 0, -0.4f, 11.2f, 17.6f, 0.8f), PartPose.offsetAndRotation(0, 1.4f, 3.6f, 0.12f, 0, 0));
        PartDefinition ssashTail = body.addOrReplaceChild("ssash_tail", CubeListBuilder.create()
                .texOffs(105, 22).mirror(false).addBox(-0.9f, 0, -0.2f, 1.8f, 12, 0.4f), PartPose.offsetAndRotation(-1.4f, -4.6f, -3.6f, -0.05f, 0, 0.05f));
        PartDefinition ssashTailShort = body.addOrReplaceChild("ssash_tail_short", CubeListBuilder.create()
                .texOffs(75, 38).mirror(false).addBox(-0.8f, 0, -0.2f, 1.6f, 8.5f, 0.4f), PartPose.offsetAndRotation(0.4f, -4.6f, -3.7f, -0.04f, 0, -0.03f));
        PartDefinition spendant = body.addOrReplaceChild("spendant", CubeListBuilder.create()
                .texOffs(187, 50).mirror(false).addBox(-0.2f, 0, -0.2f, 0.4f, 3, 0.4f)
                .texOffs(192, 50).mirror(false).addBox(-1, 3, -0.4f, 2, 2.6f, 0.8f)
                .texOffs(169, 50).mirror(false).addBox(-0.6f, 5.6f, -0.4f, 1.2f, 3.4f, 0.8f), PartPose.offsetAndRotation(3.2f, -4.2f, -3.4f, 0, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(82, 38).mirror(false).addBox(-1.4f, -1, -1.4f, 2.8f, 7, 2.8f), PartPose.offsetAndRotation(-5.4f, -13.8f, 0, -0.2f, 0, 0.1f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(82, 38).mirror(true).addBox(-1.4f, -1, -1.4f, 2.8f, 7, 2.8f), PartPose.offsetAndRotation(5.4f, -13.8f, 0, 0.35f, 0, -0.1f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(147, 38).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 6, 2.6f), PartPose.offsetAndRotation(0, 6.4f, 0, -1.1f, -0.35f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(147, 38).mirror(true).addBox(-1.3f, 0, -1.3f, 2.6f, 6, 2.6f), PartPose.offsetAndRotation(0, 6.4f, 0, -0.4f, 0.3f, -0.4f));
        PartDefinition rightSleeve = rightForearm.addOrReplaceChild("right_sleeve", CubeListBuilder.create()
                .texOffs(162, 0).mirror(false).addBox(-2.2f, -1.3f, -6.9f, 4.4f, 5.4f, 9.2f)
                .texOffs(125, 50).mirror(false).addBox(-2.55f, -1.6f, -7.25f, 5.1f, 4.3f, 0.6f), PartPose.offsetAndRotation(0, 0.4f, 0.8f, 1.4248f, 0.1562f, 0.3145f));
        PartDefinition leftSleeve = leftForearm.addOrReplaceChild("left_sleeve", CubeListBuilder.create()
                .texOffs(162, 0).mirror(true).addBox(-2.2f, -1.3f, -6.9f, 4.4f, 5.4f, 9.2f)
                .texOffs(125, 50).mirror(true).addBox(-2.55f, -1.6f, -7.25f, 5.1f, 4.3f, 0.6f), PartPose.offsetAndRotation(0, 0.4f, 0.8f, 0.0296f, -0.4141f, 0.2796f));
        PartDefinition rightSleeveFall = rightSleeve.addOrReplaceChild("right_sleeve_fall", CubeListBuilder.create()
                .texOffs(88, 0).mirror(false).addBox(-2.6f, 0, -8, 5.2f, 10, 8), PartPose.offsetAndRotation(0, 1, 4.75f, -0.45f, 0, 0));
        PartDefinition leftSleeveFall = leftSleeve.addOrReplaceChild("left_sleeve_fall", CubeListBuilder.create()
                .texOffs(88, 0).mirror(true).addBox(-2.6f, 0, -8, 5.2f, 10, 8), PartPose.offsetAndRotation(0, 1, 4.75f, -0.45f, 0, 0));
        PartDefinition rightSleeveSwell = rightSleeve.addOrReplaceChild("right_sleeve_swell", CubeListBuilder.create()
                .texOffs(49, 22).mirror(false).addBox(-2.4f, 0, -5.5f, 4.8f, 9, 5.5f), PartPose.offsetAndRotation(0, 0.6f, 3.2f, -0.2f, 0, 0));
        PartDefinition leftSleeveSwell = leftSleeve.addOrReplaceChild("left_sleeve_swell", CubeListBuilder.create()
                .texOffs(49, 22).mirror(true).addBox(-2.4f, 0, -5.5f, 4.8f, 9, 5.5f), PartPose.offsetAndRotation(0, 0.6f, 3.2f, -0.2f, 0, 0));
        PartDefinition rightSleeveDrape = rightSleeveFall.addOrReplaceChild("right_sleeve_drape", CubeListBuilder.create()
                .texOffs(193, 0).mirror(false).addBox(-0.4f, 0, -6.8f, 0.45f, 8.6f, 6.4f), PartPose.offsetAndRotation(-2.75f, 0.6f, -0.8f, 0.03f, 0, 0.06f));
        PartDefinition leftSleeveDrape = leftSleeveFall.addOrReplaceChild("left_sleeve_drape", CubeListBuilder.create()
                .texOffs(193, 0).mirror(true).addBox(-0.05f, 0, -6.8f, 0.45f, 8.6f, 6.4f), PartPose.offsetAndRotation(2.75f, 0.6f, -0.8f, 0.03f, 0, -0.06f));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(140, 50).mirror(false).addBox(-1.1f, 0, -1.2f, 2.2f, 2.8f, 2.4f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(140, 50).mirror(true).addBox(-1.1f, 0, -1.2f, 2.2f, 2.8f, 2.4f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(218, 22).mirror(false).addBox(-2.6f, -7.6f, -3, 5.2f, 4.8f, 5.8f)
                .texOffs(237, 38).mirror(false).addBox(-2, -2.8f, -2.8f, 4, 2.6f, 4.8f)
                .texOffs(199, 50).mirror(false).addBox(-1, -0.4f, -2.7f, 2, 0.8f, 2.2f)
                .texOffs(210, 50).mirror(false).addBox(-0.5f, -5.4f, -3.5f, 1, 2.4f, 0.6f)
                .texOffs(0, 38).mirror(false).addBox(-2.9f, -7.8f, -0.6f, 5.8f, 6.2f, 3.8f)
                .texOffs(153, 50).mirror(false).addBox(-2.8f, -6.4f, -2.4f, 0.4f, 3.6f, 1.6f)
                .texOffs(153, 50).mirror(false).addBox(2.4f, -6.4f, -2.4f, 0.4f, 3.6f, 1.6f), PartPose.offsetAndRotation(0, -17.2f, -0.6f, 0, 0, 0));
        PartDefinition tongue = head.addOrReplaceChild("tongue", CubeListBuilder.create()
                .texOffs(176, 50).mirror(false).addBox(-0.3f, 0, -3.8f, 0.6f, 0.2f, 3.8f), PartPose.offsetAndRotation(0, -1.6f, -2.9f, 0.1f, 0, 0));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(227, 50).mirror(false).addBox(-1, -0.3f, -0.2f, 2, 0.6f, 0.3f), PartPose.offsetAndRotation(-1.25f, -4.9f, -3.05f, 0, 0.15f, 0.22f));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(227, 50).mirror(true).addBox(-1, -0.3f, -0.2f, 2, 0.6f, 0.3f), PartPose.offsetAndRotation(1.25f, -4.9f, -3.05f, 0, -0.15f, -0.22f));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(234, 50).mirror(false).addBox(-1.3f, -0.25f, -0.2f, 2.4f, 0.5f, 0.4f), PartPose.offsetAndRotation(-1.2f, -5.7f, -3.1f, 0, 0.15f, 0.4f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(234, 50).mirror(true).addBox(-1.1f, -0.25f, -0.2f, 2.4f, 0.5f, 0.4f), PartPose.offsetAndRotation(1.2f, -5.7f, -3.1f, 0, -0.15f, -0.4f));
        PartDefinition cap = head.addOrReplaceChild("cap", CubeListBuilder.create()
                .texOffs(21, 38).mirror(false).addBox(-2.9f, -3.6f, -3.2f, 5.8f, 3.8f, 6.2f)
                .texOffs(76, 50).mirror(false).addBox(-2.3f, -6, -0.2f, 4.6f, 2.8f, 3.4f)
                .texOffs(0, 50).mirror(false).addBox(-3, -0.6f, -3.3f, 6, 0.8f, 6.4f), PartPose.offsetAndRotation(0, -7.6f, 0, 0, 0, 0));
        PartDefinition rightCapWing = cap.addOrReplaceChild("right_cap_wing", CubeListBuilder.create()
                .texOffs(0, 59).mirror(false).addBox(-11, -0.45f, -0.2f, 11, 0.9f, 0.4f), PartPose.offsetAndRotation(-2.9f, -2.6f, 2.4f, 0, 0.06f, -0.08f));
        PartDefinition leftCapWing = cap.addOrReplaceChild("left_cap_wing", CubeListBuilder.create()
                .texOffs(0, 59).mirror(true).addBox(0, -0.45f, -0.2f, 11, 0.9f, 0.4f), PartPose.offsetAndRotation(2.9f, -2.6f, 2.4f, 0, -0.06f, 0.08f));
        PartDefinition tail0 = body.addOrReplaceChild("tail_0", CubeListBuilder.create()
                .texOffs(72, 22).mirror(false).addBox(-4, -2.68f, 0, 8, 5.28f, 7.4f), PartPose.offsetAndRotation(0, 12.2f, 2.4f, 0.12f, 0.1f, 0));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(112, 22).mirror(false).addBox(-3.69f, -2.2708f, 0, 7.38f, 4.8708f, 7.1f), PartPose.offsetAndRotation(0, 0, 6.9f, -0.03f, 0.42f, 0));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(145, 22).mirror(false).addBox(-3.38f, -1.8616f, 0, 6.76f, 4.4616f, 6.8f), PartPose.offsetAndRotation(0, 0, 6.6f, -0.03f, 0.44f, 0));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create()
                .texOffs(145, 22).mirror(false).addBox(-3.07f, -1.4524f, 0, 6.14f, 4.0524f, 6.5f), PartPose.offsetAndRotation(0, 0, 6.3f, 0, 0.46f, 0));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail_4", CubeListBuilder.create()
                .texOffs(48, 38).mirror(false).addBox(-2.76f, -1.0432f, 0, 5.52f, 3.6432f, 6.2f), PartPose.offsetAndRotation(0, 0, 6, 0, 0.48f, 0));
        PartDefinition tail5 = tail4.addOrReplaceChild("tail_5", CubeListBuilder.create()
                .texOffs(95, 38).mirror(false).addBox(-2.45f, -0.634f, 0, 4.9f, 3.234f, 5.9f), PartPose.offsetAndRotation(0, 0, 5.7f, 0, 0.5f, 0));
        PartDefinition tail6 = tail5.addOrReplaceChild("tail_6", CubeListBuilder.create()
                .texOffs(160, 38).mirror(false).addBox(-2.14f, -0.2248f, 0, 4.28f, 2.8248f, 5.6f), PartPose.offsetAndRotation(0, 0, 5.4f, 0, 0.52f, 0));
        PartDefinition tail7 = tail6.addOrReplaceChild("tail_7", CubeListBuilder.create()
                .texOffs(183, 38).mirror(false).addBox(-1.83f, 0.1844f, 0, 3.66f, 2.4156f, 5.3f), PartPose.offsetAndRotation(0, 0, 5.1f, 0, 0.54f, 0));
        PartDefinition tail8 = tail7.addOrReplaceChild("tail_8", CubeListBuilder.create()
                .texOffs(27, 50).mirror(false).addBox(-1.52f, 0.5936f, 0, 3.04f, 2.0064f, 5), PartPose.offsetAndRotation(0, 0, 4.8f, 0, 0.56f, 0));
        PartDefinition tail9 = tail8.addOrReplaceChild("tail_9", CubeListBuilder.create()
                .texOffs(95, 50).mirror(false).addBox(-1.21f, 1.0028f, 0, 2.42f, 1.5972f, 4.7f), PartPose.offsetAndRotation(0, 0, 4.5f, 0, 0.58f, 0));
        PartDefinition fan = rightHand.addOrReplaceChild("fan", CubeListBuilder.create()
                .texOffs(222, 50).mirror(false).addBox(-0.4f, -1, -0.4f, 0.8f, 2, 0.8f)
                .texOffs(118, 38).mirror(false).addBox(-6.5f, -9, -0.15f, 13, 8.6f, 0.3f), PartPose.offsetAndRotation(0, 2.4f, -0.4f, 1.674f, 0.3417f, 0.8048f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED scholar: end
    }

    /** The Scholar's pose: a smooth gliding step, the robe swaying, the fan held up; his tongue flickering now and then; rearing to spit, he draws his head back and the tongue darts out; coiling, his body twists and his tail sweeps wide round him; a blow, the fan slashed across. */
    public static final class ScholarModel extends EntityModel<WhiteRobedScholarEntity> {
        private final ModelPart root, body, head, tongue, rightLeg, leftLeg, rightArm, leftArm, rightSleeve, leftSleeve, skirt, skirtBack;
        private final List<ModelPart> tail = new ArrayList<>();

        public ScholarModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            tongue = head.getChild("tongue");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightSleeve = rightArm.getChild("right_forearm").getChild("right_sleeve");
            leftSleeve = leftArm.getChild("left_forearm").getChild("left_sleeve");
            skirt = body.getChild("skirt");
            skirtBack = skirt.getChild("skirt_back");
            ModelPart p = body;
            for (int k = 0; p.hasChild("tail_" + k); k++) {
                p = p.getChild("tail_" + k);
                tail.add(p);
            }
        }

        @Override
        public void setupAnim(WhiteRobedScholarEntity e, float swing, float amount, float age, float yaw, float pitch) {
            int s = e.state();
            float w = swing * 0.55f, stride = Mth.cos(w) * 0.5f * amount;
            float slash = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            turn(rightLeg, stride, 0, 0);
            turn(leftLeg, -stride, 0, 0);
            boolean rearing = s == WhiteRobedScholarEntity.REARING, coiling = s == WhiteRobedScholarEntity.COILING;
            turn(body, rearing ? -0.32f : 0, coiling ? Mth.sin(age * 0.6f) * 0.5f : slash * 0.4f, 0);
            turn(head, pitch * Mth.DEG_TO_RAD + (rearing ? -0.35f : 0), yaw * Mth.DEG_TO_RAD, rearing ? 0 : Mth.sin(age * 0.05f) * 0.04f);
            tongue.visible = rearing || (age % 70) < 6;
            tongue.zScale = rearing ? 1.4f + Mth.sin(age * 1.2f) * 0.2f : 0.6f + Mth.sin(age * 2.5f) * 0.4f;
            turn(rightArm, -slash * 0.6f, -slash * 0.9f, slash * 0.4f);
            turn(leftArm, stride * 0.3f, 0, 0);
            turn(rightSleeve, slash * 0.4f + Mth.sin(age * 0.05f) * 0.04f, 0, 0);
            turn(leftSleeve, -stride * 0.2f + Mth.sin(age * 0.05f + 1) * 0.04f, 0, 0);
            turn(skirt, 0, 0, Mth.sin(age * 0.06f) * 0.02f);
            turn(skirtBack, amount * 0.25f, 0, 0);
            for (int k = 0; k < tail.size(); k++) {
                float wave = Mth.sin(age * 0.12f - k * 0.7f) * (0.1f + amount * 0.15f);
                float sweep = coiling ? Mth.sin(age * 0.6f - k * 0.5f) * 0.5f : 0;
                turn(tail.get(k), 0, wave + sweep, 0);
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
