package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.flight.NimbusCloud;
import com.jttw.engine.entity.heaven.CloudCavalryEntity;
import com.jttw.engine.entity.heaven.HeavenMonster;
import com.jttw.engine.entity.heaven.HeavenlyArcherEntity;
import com.jttw.engine.entity.heaven.HeavenlySoldierEntity;
import com.jttw.engine.entity.heaven.LunarMansionEntity;
import com.jttw.engine.entity.heaven.ThunderDrummerEntity;
import com.jttw.engine.registry.HeavenRegistry;
import com.jttw.engine.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
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
import org.joml.Matrix3f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * How Heaven's rank and file look, each on a small white cloud: the heavenly soldier (a halberdier in
 * gilt Tang armour, azure silk, a red-tasselled helmet, a crescent-bladed halberd; his captain in crimson
 * and gold, cloaked, phoenix-winged, a celestial ribbon arching behind his head), the heavenly archer
 * (pheasant-feathered cap, quiver, long gilt recurve bow, drawn while he fights), the thunder drummer
 * (three war drums on a frame at his back, sticks raised overhead in his wind-up), the cloud cavalry
 * (a spearman of the host on a plain Minecraft horse in gold horse armour, his spear couched as it rears) and the
 * Twenty-Eight Lunar Mansions (one frame in four quarters' colours, each his beast on crest and shield).
 * Models and skins: {@code assets_source/heaven_host.py} (with {@code heaven_host_kit.py},
 * {@code heaven_host_ranks.py}, {@code heaven_host_cavalry.py}, {@code heaven_host_mansions.py},
 * {@code heaven_host_emblems.py}).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeavenHostRenderers {

    private HeavenHostRenderers() {}

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/" + name + ".png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name), "main");
    }

    public static final ModelLayerLocation SOLDIER_LAYER = layer("heavenly_soldier"), ARCHER_LAYER = layer("heavenly_archer"),
            DRUMMER_LAYER = layer("thunder_drummer"), MANSION_LAYER = layer("lunar_mansion"), CAVALRY_LAYER = layer("cloud_cavalry");
    /** The cloud cavalry's horse is drawn as vanilla draws a horse; its rider a trooper's size (heaven_host_cavalry.py prints his reach). */
    public static final float HORSE_SIZE = 1.1f, RIDER_SIZE = 0.86f;

    /** How much larger than its pixels a trooper is drawn (heaven_host.py prints its reach). */
    public static final float TROOPER_SIZE = 0.86f;
    /** Names over heads show only this close, so a host is not a wall of labels. */
    private static final double NAME_RANGE = 24;
    /** The cloud one rides, against Wukong's: a small one. */
    private static final float CLOUD_SIZE = 0.55f;

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SOLDIER_LAYER, HeavenHostRenderers::soldierLayer);
        event.registerLayerDefinition(ARCHER_LAYER, HeavenHostRenderers::archerLayer);
        event.registerLayerDefinition(DRUMMER_LAYER, HeavenHostRenderers::drummerLayer);
        event.registerLayerDefinition(MANSION_LAYER, HeavenHostRenderers::mansionLayer);
        event.registerLayerDefinition(CAVALRY_LAYER, HeavenHostRenderers::cavalryLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HEAVENLY_SOLDIER.get(), ctx -> new HostRenderer<HeavenlySoldierEntity>(ctx,
                new TrooperModel<>(ctx.bakeLayer(SOLDIER_LAYER)), TROOPER_SIZE, 0.5f, CLOUD_SIZE, e -> e.isCaptain() ? "heaven_soldier_captain" : "heaven_soldier"));
        event.registerEntityRenderer(HeavenRegistry.HEAVENLY_ARCHER.get(), ctx -> new HostRenderer<HeavenlyArcherEntity>(ctx,
                new ArcherModel(ctx.bakeLayer(ARCHER_LAYER)), TROOPER_SIZE, 0.5f, CLOUD_SIZE, e -> e.isCaptain() ? "heaven_archer_captain" : "heaven_archer"));
        event.registerEntityRenderer(HeavenRegistry.CLOUD_CAVALRY.get(), CavalryRenderer::new);
        event.registerEntityRenderer(HeavenRegistry.THUNDER_DRUMMER.get(), ctx -> new HostRenderer<ThunderDrummerEntity>(ctx,
                new DrummerModel(ctx.bakeLayer(DRUMMER_LAYER)), TROOPER_SIZE, 0.55f, CLOUD_SIZE, e -> e.isCaptain() ? "heaven_drummer_captain" : "heaven_drummer"));
        event.registerEntityRenderer(HeavenRegistry.LUNAR_MANSION.get(), ctx -> new HostRenderer<LunarMansionEntity>(ctx,
                new MansionModel(ctx.bakeLayer(MANSION_LAYER)), TROOPER_SIZE, 0.5f, CLOUD_SIZE,
                e -> "heaven_mansion_" + LunarMansionEntity.MANSIONS[Math.floorMod(e.variant(), LunarMansionEntity.MANSIONS.length)]));
    }

    // ── The renderer ─────────────────────────────────────────────────────

    /**
     * One of the host: its skin (by what it is: a captain's, a Lunar Mansion's own), what of it shines
     * drawn full-bright over it, the cloud it rides under its feet, its name shown only near.
     */
    public static class HostRenderer<T extends HeavenMonster> extends MobRenderer<T, EntityModel<T>> {
        private final float size, cloud;
        private final Function<T, String> skin;
        private final Map<String, ResourceLocation> skins = new HashMap<>();
        private final Map<String, RenderType> glows = new HashMap<>();

        public HostRenderer(EntityRendererProvider.Context ctx, EntityModel<T> model, float size, float shadow, float cloud, Function<T, String> skin) {
            super(ctx, model, shadow);
            this.size = size;
            this.cloud = cloud;
            this.skin = skin;
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, T e,
                                   float swing, float amount, float partial, float age, float yaw, float pitch) {
                    if (e.isInvisible()) return;
                    RenderType glow = glows.computeIfAbsent(skin.apply(e), n -> RenderType.eyes(tex(n + "_glow")));
                    getParentModel().renderToBuffer(pose, buffers.getBuffer(glow), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
                }
            });
        }

        @Override
        protected void scale(T e, PoseStack pose, float partial) {
            pose.scale(size, size, size);
        }

        @Override
        public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            super.render(e, yaw, partial, pose, buffers, light);
            if (cloud <= 0) return;
            pose.pushPose();
            float s = cloud * e.getScale();
            pose.scale(s, s, s);
            NimbusCloud.draw(pose, buffers, e, 1f, light);
            pose.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return skins.computeIfAbsent(skin.apply(e), HeavenHostRenderers::tex);
        }

        @Override
        protected boolean shouldShowName(T e) {
            return super.shouldShowName(e) && entityRenderDispatcher.distanceToSqr(e) < NAME_RANGE * NAME_RANGE;
        }
    }

    // ── The trooper's frame ─────────────────────────────────────────────

    @Nullable
    static ModelPart opt(@Nullable ModelPart p, String name) {
        return p != null && p.hasChild(name) ? p.getChild(name) : null;
    }

    static void turn(@Nullable ModelPart p, float x, float y, float z) {
        if (p == null) return;
        p.xRot = p.getInitialPose().xRot + x;
        p.yRot = p.getInitialPose().yRot + y;
        p.zRot = p.getInitialPose().zRot + z;
    }

    static void show(List<ModelPart> parts, boolean shown) {
        for (ModelPart p : parts) p.visible = shown;
    }

    /**
     * A man of the host on his cloud: he hovers, bobbing, his legs swaying a little as he drifts; leaning
     * into it as he flies; his skirt, sashes, tassel, cloak and ribbon stream behind; his head turns to
     * look. The halberd held upright, chopped down on a blow. A captain's own parts (cloak, wings, ribbon,
     * great tassel, moustache) shown only on a captain.
     */
    public static class TrooperModel<T extends HeavenMonster> extends EntityModel<T> {
        protected final ModelPart root, waist, body, head, helm, rightArm, leftArm, rightForearm, leftForearm, rightLeg, leftLeg,
                rightShin, leftShin;
        @Nullable
        protected final ModelPart apron, robeBack, rightTasset, leftTasset, backTasset, rightSideTasset, leftSideTasset, rightSash, leftSash,
                plumeTail, bigPlumeTail, cape, capeLower, ribbon, rightRibbon, leftRibbon, rightFlap, leftFlap;
        protected final List<ModelPart> captainOnly = new ArrayList<>(), regularOnly = new ArrayList<>();
        protected boolean captain;

        public TrooperModel(ModelPart root) {
            this.root = root;
            waist = root.getChild("waist");
            body = waist.getChild("body");
            head = body.getChild("head");
            helm = opt(head, "helm");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            leftForearm = leftArm.getChild("left_forearm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            apron = opt(waist, "apron");
            robeBack = opt(waist, "robe_back");
            rightTasset = opt(waist, "right_tasset");
            leftTasset = opt(waist, "left_tasset");
            backTasset = opt(waist, "back_tasset");
            rightSideTasset = opt(waist, "right_side_tasset");
            leftSideTasset = opt(waist, "left_side_tasset");
            rightSash = opt(waist, "right_sash");
            leftSash = opt(waist, "left_sash");
            ModelPart plume = opt(helm, "plume"), bigPlume = opt(helm, "plume_big");
            plumeTail = opt(plume, "plume_tail");
            bigPlumeTail = opt(bigPlume, "plume_big_tail");
            cape = opt(body, "cape");
            capeLower = opt(cape, "cape_lower");
            ribbon = opt(body, "ribbon");
            rightRibbon = opt(body, "right_ribbon_end");
            leftRibbon = opt(body, "left_ribbon_end");
            rightFlap = opt(helm, "right_flap");
            leftFlap = opt(helm, "left_flap");
            for (ModelPart p : new ModelPart[]{bigPlume, cape, ribbon, rightRibbon, leftRibbon, opt(helm, "right_wing"), opt(helm, "left_wing"),
                    opt(head, "right_moustache"), opt(head, "left_moustache")}) {
                if (p != null) captainOnly.add(p);
            }
            if (plume != null && bigPlume != null) regularOnly.add(plume);
        }

        @Override
        public void prepareMobModel(T e, float swing, float amount, float partial) {
            captain = e.isCaptain();
            show(captainOnly, captain);
            show(regularOnly, !captain);
        }

        @Override
        public void setupAnim(T e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float breath = Mth.sin(age * 0.08f);
            float w = swing * 0.6662f, stride = Mth.cos(w) * 0.35f * amount;
            // hovering on his cloud: he bobs a little, his legs swaying as he drifts, the knees easing
            root.y = Mth.sin(age * 0.1f) * 0.35f;
            turn(rightLeg, stride + 0.04f, 0, 0.02f);
            turn(leftLeg, -stride - 0.02f, 0, -0.02f);
            turn(rightShin, 0.12f + Math.max(0, Mth.sin(w)) * 0.35f * amount, 0, 0);
            turn(leftShin, 0.06f + Math.max(0, -Mth.sin(w)) * 0.35f * amount, 0, 0);
            // the skirt rides on the thighs, streams back as he flies; sashes and apron sway
            turn(rightTasset, Math.min(0, stride) * 0.8f + amount * 0.1f, 0, 0);
            turn(leftTasset, Math.min(0, -stride) * 0.8f + amount * 0.1f, 0, 0);
            turn(backTasset, amount * 0.3f, 0, 0);
            turn(robeBack, amount * 0.45f + Mth.sin(age * 0.07f + 0.5f) * 0.03f, 0, 0);
            turn(rightSideTasset, 0, 0, amount * 0.06f);
            turn(leftSideTasset, 0, 0, -amount * 0.06f);
            turn(apron, amount * 0.35f + Mth.sin(age * 0.07f) * 0.04f, 0, 0);
            turn(rightSash, amount * 0.4f + Mth.sin(age * 0.06f) * 0.06f, 0, 0);
            turn(leftSash, amount * 0.4f + Mth.sin(age * 0.06f + 1) * 0.06f, 0, 0);
            // he leans into his flight, and breathes
            turn(waist, amount * 0.1f, 0, 0);
            body.y = body.getInitialPose().y + breath * 0.15f;
            turn(body, amount * 0.06f, Mth.cos(w) * 0.04f * amount, 0);
            // the head turns to look
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            // the tassel, the cloak and the ribbon stream back
            turn(plumeTail, amount * 0.4f + Mth.sin(age * 0.09f) * 0.06f, 0, Mth.sin(age * 0.05f) * 0.05f);
            turn(bigPlumeTail, amount * 0.4f + Mth.sin(age * 0.09f) * 0.06f, 0, Mth.sin(age * 0.05f) * 0.05f);
            turn(cape, amount * 0.5f + breath * 0.03f, 0, 0);
            turn(capeLower, amount * 0.35f + Mth.sin(age * 0.08f) * 0.05f, 0, 0);
            turn(ribbon, amount * 0.25f + Mth.sin(age * 0.06f) * 0.04f, 0, 0);
            turn(rightRibbon, amount * 0.6f + Mth.sin(age * 0.07f) * 0.08f, 0, Mth.sin(age * 0.05f) * 0.05f);
            turn(leftRibbon, amount * 0.6f + Mth.sin(age * 0.07f + 1.3f) * 0.08f, 0, -Mth.sin(age * 0.05f + 0.7f) * 0.05f);
            arms(e, swing, amount, age, strike, breath);
        }

        /** The arms: the weapon upright in the right, chopped down on a blow; the left swaying. */
        protected void arms(T e, float swing, float amount, float age, float strike, float breath) {
            float w = swing * 0.6662f;
            turn(rightArm, -Mth.cos(w) * 0.08f * amount - strike * 1.25f + breath * 0.02f, 0, 0);
            turn(rightForearm, strike * 0.55f, 0, 0);
            turn(leftArm, Mth.cos(w) * 0.25f * amount + breath * 0.03f, 0, 0);
            turn(leftForearm, 0, 0, 0);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }

    // ── The heavenly soldier ────────────────────────────────────────────

    /**
     * The heavenly soldier (256x256): gilt mingguang armour over azure silk, a red-tasselled helmet, a
     * crescent-bladed halberd, a sword at his hip; his captain in crimson and gold. Model and skins:
     * {@code assets_source/heaven_host.py}.
     */
    public static LayerDefinition soldierLayer() {
        // GENERATED soldier: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(126, 0).mirror(false).addBox(-2, -0.5f, -2.1f, 4, 6.8f, 4.2f), PartPose.offsetAndRotation(-2.1f, 12, 0.2f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(126, 0).mirror(true).addBox(-2, -0.5f, -2.1f, 4, 6.8f, 4.2f), PartPose.offsetAndRotation(2.1f, 12, 0.2f, 0, 0, 0));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(171, 37).mirror(false).addBox(-1.75f, 0, -1.75f, 3.5f, 4.6f, 3.5f)
                .texOffs(46, 59).mirror(false).addBox(-2.05f, 0.7f, -2.3f, 4.1f, 3.7f, 2.2f)
                .texOffs(130, 68).mirror(false).addBox(-1.95f, -0.8f, -2.65f, 3.9f, 2, 1)
                .texOffs(63, 59).mirror(false).addBox(-1.95f, 4.4f, -2.2f, 3.9f, 1.6f, 4.3f)
                .texOffs(47, 68).mirror(false).addBox(-1.45f, 4.9f, -3.3f, 2.9f, 1.1f, 1.3f)
                .texOffs(219, 68).mirror(false).addBox(-0.8f, 4.3f, -3.85f, 1.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(171, 37).mirror(true).addBox(-1.75f, 0, -1.75f, 3.5f, 4.6f, 3.5f)
                .texOffs(46, 59).mirror(true).addBox(-2.05f, 0.7f, -2.3f, 4.1f, 3.7f, 2.2f)
                .texOffs(130, 68).mirror(true).addBox(-1.95f, -0.8f, -2.65f, 3.9f, 2, 1)
                .texOffs(63, 59).mirror(true).addBox(-1.95f, 4.4f, -2.2f, 3.9f, 1.6f, 4.3f)
                .texOffs(47, 68).mirror(true).addBox(-1.45f, 4.9f, -3.3f, 2.9f, 1.1f, 1.3f)
                .texOffs(219, 68).mirror(true).addBox(-0.8f, 4.3f, -3.85f, 1.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(92, 49).mirror(false).addBox(-4, -1.5f, -2.5f, 8, 3, 5)
                .texOffs(188, 37).mirror(false).addBox(-4.7f, -2.9f, -3.1f, 9.4f, 1.9f, 6.2f)
                .texOffs(58, 68).mirror(false).addBox(-1.4f, -3.15f, -3.55f, 2.8f, 2.3f, 0.6f), PartPose.offsetAndRotation(0, 11.5f, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(54, 37).mirror(false).addBox(-1.4f, 0, -0.3f, 2.8f, 9, 0.6f), PartPose.offsetAndRotation(0, -1.3f, -3.25f, -0.05f, 0, 0));
        PartDefinition robeBack = waist.addOrReplaceChild("robe_back", CubeListBuilder.create()
                .texOffs(145, 0).mirror(false).addBox(-4, 0, -0.3f, 8, 10.2f, 0.6f), PartPose.offsetAndRotation(0, -0.9f, 2.4f, 0.1f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(119, 49).mirror(false).addBox(-2.4f, 0, -0.4f, 4.8f, 7, 0.8f), PartPose.offsetAndRotation(-2.3f, -1.3f, -2.85f, -0.14f, 0.14f, 0.13f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(119, 49).mirror(true).addBox(-2.4f, 0, -0.4f, 4.8f, 7, 0.8f), PartPose.offsetAndRotation(2.3f, -1.3f, -2.85f, -0.14f, -0.14f, -0.13f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(63, 0).mirror(false).addBox(-0.8f, 0, -2.6f, 0.8f, 6.7f, 5.2f), PartPose.offsetAndRotation(-4.25f, -1.3f, 0.1f, 0, 0, 0.27f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(63, 0).mirror(true).addBox(0, 0, -2.6f, 0.8f, 6.7f, 5.2f), PartPose.offsetAndRotation(4.25f, -1.3f, 0.1f, 0, 0, -0.27f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(223, 37).mirror(false).addBox(-4.3f, 0, -0.4f, 8.6f, 7.2f, 0.8f), PartPose.offsetAndRotation(0, -1.3f, 2.75f, 0.16f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(63, 37).mirror(false).addBox(-0.6f, 0, -0.3f, 1.2f, 8.4f, 0.6f), PartPose.offsetAndRotation(-3.5f, -2, -2.5f, -0.04f, 0.2f, 0.12f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(63, 37).mirror(true).addBox(-0.6f, 0, -0.3f, 1.2f, 8.4f, 0.6f), PartPose.offsetAndRotation(3.5f, -2, -2.5f, -0.04f, -0.2f, -0.12f));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-4, -9.6f, -2.6f, 8, 9.8f, 5.2f)
                .texOffs(78, 0).mirror(false).addBox(-4.6f, -10, -3.1f, 9.2f, 5.8f, 6.2f)
                .texOffs(190, 0).mirror(false).addBox(-4.2f, -4.5f, -2.85f, 8.4f, 4.4f, 5.7f)
                .texOffs(132, 49).mirror(false).addBox(-4.55f, -5, -3.25f, 9.1f, 0.8f, 6.5f)
                .texOffs(167, 49).mirror(false).addBox(-4.35f, -0.9f, -3, 8.7f, 1.2f, 6)
                .texOffs(236, 59).mirror(false).addBox(-1.9f, -4.1f, -3.35f, 3.8f, 3.2f, 0.7f)
                .texOffs(82, 59).mirror(false).addBox(-3, -11, -2.5f, 6, 1.4f, 5)
                .texOffs(105, 59).mirror(false).addBox(-3.3f, -10.5f, -2.9f, 6.6f, 1, 5.8f)
                .texOffs(0, 68).mirror(false).addBox(-1.3f, -11.8f, -1.3f, 2.6f, 1.4f, 2.6f)
                .texOffs(164, 59).mirror(false).addBox(-0.45f, -9.6f, -3.35f, 0.9f, 4.8f, 0.35f)
                .texOffs(141, 68).mirror(false).addBox(-0.9f, -10, -3.55f, 1.8f, 1.4f, 0.6f)
                .texOffs(13, 68).mirror(false).addBox(-3.95f, -9.3f, -3.55f, 3.1f, 3.1f, 0.5f)
                .texOffs(13, 68).mirror(false).addBox(0.85f, -9.3f, -3.55f, 3.1f, 3.1f, 0.5f), PartPose.offsetAndRotation(0, -2.7f, 0, 0, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(70, 37).mirror(false).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(-5.3f, -8.6f, 0, -0.22f, 0, 0.1f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(70, 37).mirror(true).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(5.3f, -8.6f, 0, 0.06f, 0, -0.1f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(169, 59).mirror(false).addBox(-2.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(198, 49).mirror(false).addBox(-3, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f)
                .texOffs(0, 49).mirror(false).addBox(-3.5f, 0.4f, -3.05f, 5, 1.4f, 6.1f)
                .texOffs(25, 49).mirror(false).addBox(-4, 1.6f, -3.4f, 5.4f, 1.6f, 6.8f)
                .texOffs(188, 59).mirror(false).addBox(-4.6f, 0.7f, -1.5f, 0.7f, 2.8f, 3), PartPose.offsetAndRotation(-0.3f, -1.6f, 0, 0, 0, 0.34f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(169, 59).mirror(true).addBox(-1.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(198, 49).mirror(true).addBox(-1.6f, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f)
                .texOffs(0, 49).mirror(true).addBox(-1.5f, 0.4f, -3.05f, 5, 1.4f, 6.1f)
                .texOffs(25, 49).mirror(true).addBox(-1.4f, 1.6f, -3.4f, 5.4f, 1.6f, 6.8f)
                .texOffs(188, 59).mirror(true).addBox(3.9f, 0.7f, -1.5f, 0.7f, 2.8f, 3), PartPose.offsetAndRotation(0.3f, -1.6f, 0, 0, 0, -0.34f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(221, 49).mirror(false).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(234, 49).mirror(false).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f)
                .texOffs(132, 59).mirror(false).addBox(-2.2f, -0.9f, -2.2f, 4.4f, 2, 4.4f), PartPose.offsetAndRotation(0, 3.8f, 0, -0.95f, -0.3f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(221, 49).mirror(true).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(234, 49).mirror(true).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f)
                .texOffs(132, 59).mirror(true).addBox(-2.2f, -0.9f, -2.2f, 4.4f, 2, 4.4f), PartPose.offsetAndRotation(0, 3.8f, 0, -0.3f, 0.12f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(197, 59).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(197, 59).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(34, 0).mirror(false).addBox(-3.3f, -7, -3.3f, 6.6f, 7, 6.6f)
                .texOffs(148, 68).mirror(false).addBox(-0.55f, -4.1f, -3.85f, 1.1f, 1.8f, 0.6f), PartPose.offsetAndRotation(0, -11.3f, -0.1f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(226, 68).mirror(false).addBox(-2.5f, -0.4f, -0.35f, 2.5f, 0.7f, 0.6f), PartPose.offsetAndRotation(-0.35f, -5.05f, -3.4f, 0, 0.1f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(226, 68).mirror(true).addBox(0, -0.4f, -0.35f, 2.5f, 0.7f, 0.6f), PartPose.offsetAndRotation(0.35f, -5.05f, -3.4f, 0, -0.1f, -0.2f));
        PartDefinition helm = head.addOrReplaceChild("helm", CubeListBuilder.create()
                .texOffs(87, 37).mirror(false).addBox(-3.65f, -1.25f, -3.65f, 7.3f, 1.5f, 7.3f)
                .texOffs(120, 37).mirror(false).addBox(-3.45f, -3.45f, -3.45f, 6.9f, 2.3f, 6.9f)
                .texOffs(0, 59).mirror(false).addBox(-2.55f, -4.6f, -2.55f, 5.1f, 1.25f, 5.1f)
                .texOffs(67, 68).mirror(false).addBox(-1.25f, -5.5f, -1.25f, 2.5f, 1, 2.5f)
                .texOffs(24, 68).mirror(false).addBox(-0.35f, -8.6f, -0.35f, 0.7f, 3.2f, 0.7f)
                .texOffs(155, 68).mirror(false).addBox(-0.6f, -9.3f, -0.6f, 1.2f, 1, 1.2f)
                .texOffs(164, 68).mirror(false).addBox(-3.85f, -0.5f, -4.35f, 7.7f, 0.7f, 1.1f), PartPose.offsetAndRotation(0, -5.6f, 0, 0, 0, 0));
        PartDefinition crest = helm.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(29, 68).mirror(false).addBox(-1.5f, -3.8f, -0.3f, 3, 3.8f, 0.5f), PartPose.offsetAndRotation(0, -1, -3.6f, -0.12f, 0, 0));
        PartDefinition rightFlap = helm.addOrReplaceChild("right_flap", CubeListBuilder.create()
                .texOffs(221, 0).mirror(false).addBox(-0.6f, 0, -2.9f, 0.7f, 4.8f, 5.6f), PartPose.offsetAndRotation(-3.55f, -0.3f, 0.3f, 0, 0, 0.16f));
        PartDefinition leftFlap = helm.addOrReplaceChild("left_flap", CubeListBuilder.create()
                .texOffs(221, 0).mirror(true).addBox(-0.1f, 0, -2.9f, 0.7f, 4.8f, 5.6f), PartPose.offsetAndRotation(3.55f, -0.3f, 0.3f, 0, 0, -0.16f));
        PartDefinition neckguard = helm.addOrReplaceChild("neckguard", CubeListBuilder.create()
                .texOffs(210, 59).mirror(false).addBox(-3.55f, 0, -0.4f, 7.1f, 4.6f, 0.8f), PartPose.offsetAndRotation(0, -0.2f, 3.25f, 0.28f, 0, 0));
        PartDefinition plume = helm.addOrReplaceChild("plume", CubeListBuilder.create()
                .texOffs(80, 68).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 1, 2.6f)
                .texOffs(25, 59).mirror(false).addBox(-2.5f, 0.8f, -2.5f, 5, 2.5f, 5), PartPose.offsetAndRotation(0, -7, 0, 0, 0, 0));
        PartDefinition plumeTail = plume.addOrReplaceChild("plume_tail", CubeListBuilder.create()
                .texOffs(52, 49).mirror(false).addBox(-2, 0, -1.1f, 4, 6, 2.2f), PartPose.offsetAndRotation(0, 1.6f, 1.8f, 0.6f, 0, 0));
        PartDefinition rightWing = helm.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(113, 0).mirror(false).addBox(-0.25f, -7.6f, -2, 0.5f, 8, 5), PartPose.offsetAndRotation(-3.6f, -1.6f, 0.4f, 0.3f, 0.35f, -0.6f));
        PartDefinition leftWing = helm.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(113, 0).mirror(true).addBox(-0.25f, -7.6f, -2, 0.5f, 8, 5), PartPose.offsetAndRotation(3.6f, -1.6f, 0.4f, 0.3f, -0.35f, 0.6f));
        PartDefinition halberd = rightHand.addOrReplaceChild("halberd", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.45f, -25, -0.45f, 0.9f, 35, 0.9f)
                .texOffs(93, 68).mirror(false).addBox(-0.8f, -26.6f, -0.8f, 1.6f, 1.8f, 1.6f)
                .texOffs(102, 68).mirror(false).addBox(-0.6f, -28.4f, -0.6f, 1.2f, 1.9f, 1.2f)
                .texOffs(149, 37).mirror(false).addBox(-1, -36.6f, -0.25f, 2, 8.4f, 0.5f)
                .texOffs(235, 68).mirror(false).addBox(0.35f, -29.4f, -0.2f, 2, 0.8f, 0.4f)
                .texOffs(185, 68).mirror(false).addBox(1.7f, -31, -0.2f, 0.6f, 2, 0.4f)
                .texOffs(111, 68).mirror(false).addBox(-0.5f, 9.6f, -0.5f, 1, 2.4f, 1), PartPose.offsetAndRotation(0, 1.2f, 0, 1.2131f, 0.2655f, 0.2082f));
        PartDefinition crescent = halberd.addOrReplaceChild("crescent", CubeListBuilder.create()
                .texOffs(156, 37).mirror(false).addBox(-5.4f, -4.4f, -0.25f, 5.4f, 8.8f, 0.5f), PartPose.offsetAndRotation(-0.5f, -29.6f, 0, 0, 0, 0));
        PartDefinition halberdTassel = halberd.addOrReplaceChild("halberd_tassel", CubeListBuilder.create()
                .texOffs(38, 68).mirror(false).addBox(-1, 0, -1, 2, 2.4f, 2)
                .texOffs(116, 68).mirror(false).addBox(-0.75f, 2.1f, -0.75f, 1.5f, 1.6f, 1.5f), PartPose.offsetAndRotation(0, -25.8f, 0, 0, 0, 0));
        PartDefinition sword = waist.addOrReplaceChild("sword", CubeListBuilder.create()
                .texOffs(164, 0).mirror(false).addBox(-0.55f, 0, -0.7f, 1.1f, 9.5f, 1.4f)
                .texOffs(190, 68).mirror(false).addBox(-0.65f, 9.4f, -0.8f, 1.3f, 1, 1.6f)
                .texOffs(199, 68).mirror(false).addBox(-1.3f, -0.6f, -0.95f, 2.6f, 0.6f, 1.9f)
                .texOffs(125, 68).mirror(false).addBox(-0.4f, -3.4f, -0.4f, 0.8f, 2.8f, 0.8f)
                .texOffs(210, 68).mirror(false).addBox(-0.6f, -4.2f, -0.6f, 1.2f, 0.9f, 1.2f), PartPose.offsetAndRotation(5.2f, -1.8f, -0.6f, 0.5f, 0, -0.2f));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(0, 37).mirror(false).addBox(-5.2f, 0, -0.4f, 10.4f, 10, 0.8f), PartPose.offsetAndRotation(0, -10.4f, 3.3f, 0.12f, 0, 0));
        PartDefinition capeLower = cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(25, 37).mirror(false).addBox(-6.1f, 0, -0.4f, 12.2f, 9.5f, 0.8f), PartPose.offsetAndRotation(0, 9.7f, 0, 0.08f, 0, 0));
        PartDefinition ribbon = body.addOrReplaceChild("ribbon", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -12.8f, 4.6f, 0, 0, 0));
        PartDefinition ribbon0 = ribbon.addOrReplaceChild("ribbon_0", CubeListBuilder.create()
                .texOffs(153, 59).mirror(false).addBox(-0.15f, -1.1f, -0.1f, 0.3f, 2.2f, 3.1605f), PartPose.offsetAndRotation(-7.9564f, 1.9838f, 0, 1.508f, -1.5708f, 0));
        PartDefinition ribbon1 = ribbon.addOrReplaceChild("ribbon_1", CubeListBuilder.create()
                .texOffs(153, 59).mirror(false).addBox(-0.15f, -1.1f, -0.1f, 0.3f, 2.2f, 3.1605f), PartPose.offsetAndRotation(-8.1423f, -0.9709f, 0, 1.2706f, 1.5708f, 0));
        PartDefinition ribbon2 = ribbon.addOrReplaceChild("ribbon_2", CubeListBuilder.create()
                .texOffs(153, 59).mirror(false).addBox(-0.15f, -1.1f, -0.1f, 0.3f, 2.2f, 3.1605f), PartPose.offsetAndRotation(-7.2669f, -3.799f, 0, 0.9076f, 1.5708f, 0));
        PartDefinition ribbon3 = ribbon.addOrReplaceChild("ribbon_3", CubeListBuilder.create()
                .texOffs(153, 59).mirror(false).addBox(-0.15f, -1.1f, -0.1f, 0.3f, 2.2f, 3.1605f), PartPose.offsetAndRotation(-5.4442f, -6.1319f, 0, 0.5445f, 1.5708f, 0));
        PartDefinition ribbon4 = ribbon.addOrReplaceChild("ribbon_4", CubeListBuilder.create()
                .texOffs(153, 59).mirror(false).addBox(-0.15f, -1.1f, -0.1f, 0.3f, 2.2f, 3.1605f), PartPose.offsetAndRotation(-2.9119f, -7.6656f, 0, 0.1815f, 1.5708f, 0));
        PartDefinition ribbon5 = ribbon.addOrReplaceChild("ribbon_5", CubeListBuilder.create()
                .texOffs(153, 59).mirror(false).addBox(-0.15f, -1.1f, -0.1f, 0.3f, 2.2f, 3.1605f), PartPose.offsetAndRotation(-0, -8.2f, 0, -0.1815f, 1.5708f, 0));
        PartDefinition ribbon6 = ribbon.addOrReplaceChild("ribbon_6", CubeListBuilder.create()
                .texOffs(153, 59).mirror(false).addBox(-0.15f, -1.1f, -0.1f, 0.3f, 2.2f, 3.1605f), PartPose.offsetAndRotation(2.9119f, -7.6656f, 0, -0.5445f, 1.5708f, 0));
        PartDefinition ribbon7 = ribbon.addOrReplaceChild("ribbon_7", CubeListBuilder.create()
                .texOffs(153, 59).mirror(false).addBox(-0.15f, -1.1f, -0.1f, 0.3f, 2.2f, 3.1605f), PartPose.offsetAndRotation(5.4442f, -6.1319f, 0, -0.9076f, 1.5708f, 0));
        PartDefinition ribbon8 = ribbon.addOrReplaceChild("ribbon_8", CubeListBuilder.create()
                .texOffs(153, 59).mirror(false).addBox(-0.15f, -1.1f, -0.1f, 0.3f, 2.2f, 3.1605f), PartPose.offsetAndRotation(7.2669f, -3.799f, 0, -1.2706f, 1.5708f, 0));
        PartDefinition ribbon9 = ribbon.addOrReplaceChild("ribbon_9", CubeListBuilder.create()
                .texOffs(153, 59).mirror(false).addBox(-0.15f, -1.1f, -0.1f, 0.3f, 2.2f, 3.1605f), PartPose.offsetAndRotation(8.1423f, -0.9709f, 0, -1.508f, -1.5708f, 0));
        PartDefinition rightRibbonEnd = body.addOrReplaceChild("right_ribbon_end", CubeListBuilder.create()
                .texOffs(229, 59).mirror(false).addBox(-1, 0, -0.15f, 2, 5, 0.3f), PartPose.offsetAndRotation(-7.9f, -10.8f, 4.4f, 0.14f, 0, 0.1f));
        PartDefinition leftRibbonEnd = body.addOrReplaceChild("left_ribbon_end", CubeListBuilder.create()
                .texOffs(229, 59).mirror(true).addBox(-1, 0, -0.15f, 2, 5, 0.3f), PartPose.offsetAndRotation(7.9f, -10.8f, 4.4f, 0.14f, 0, -0.1f));
        PartDefinition rightRibbonEnd2 = rightRibbonEnd.addOrReplaceChild("right_ribbon_end_2", CubeListBuilder.create()
                .texOffs(229, 59).mirror(false).addBox(-1, 0, -0.15f, 2, 5, 0.3f), PartPose.offsetAndRotation(0, 4.85f, 0, -0.1f, 0, -0.06f));
        PartDefinition leftRibbonEnd2 = leftRibbonEnd.addOrReplaceChild("left_ribbon_end_2", CubeListBuilder.create()
                .texOffs(229, 59).mirror(true).addBox(-1, 0, -0.15f, 2, 5, 0.3f), PartPose.offsetAndRotation(0, 4.85f, 0, -0.1f, 0, 0.06f));
        PartDefinition rightRibbonEnd3 = rightRibbonEnd2.addOrReplaceChild("right_ribbon_end_3", CubeListBuilder.create()
                .texOffs(229, 59).mirror(false).addBox(-1, 0, -0.15f, 2, 5, 0.3f), PartPose.offsetAndRotation(0, 4.85f, 0, -0.1f, 0, -0.06f));
        PartDefinition leftRibbonEnd3 = leftRibbonEnd2.addOrReplaceChild("left_ribbon_end_3", CubeListBuilder.create()
                .texOffs(229, 59).mirror(true).addBox(-1, 0, -0.15f, 2, 5, 0.3f), PartPose.offsetAndRotation(0, 4.85f, 0, -0.1f, 0, 0.06f));
        PartDefinition plumeBig = helm.addOrReplaceChild("plume_big", CubeListBuilder.create()
                .texOffs(80, 68).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 1, 3)
                .texOffs(67, 49).mirror(false).addBox(-3, 0.8f, -3, 6, 2.8f, 6), PartPose.offsetAndRotation(0, -7.4f, 0, 0, 0, 0));
        PartDefinition plumeBigTail = plumeBig.addOrReplaceChild("plume_big_tail", CubeListBuilder.create()
                .texOffs(173, 0).mirror(false).addBox(-2.4f, 0, -1.2f, 4.8f, 8.5f, 2.4f), PartPose.offsetAndRotation(0, 2, 2.2f, 0.65f, 0, 0));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(242, 68).mirror(false).addBox(-2.2f, -0.35f, -0.3f, 2.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(-0.3f, -2.25f, -3.45f, 0, 0.15f, -0.35f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(242, 68).mirror(true).addBox(0, -0.35f, -0.3f, 2.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(0.3f, -2.25f, -3.45f, 0, -0.15f, 0.35f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED soldier: end
    }

    // ── The heavenly archer ─────────────────────────────────────────────

    /**
     * The heavenly archer (256x256): a gilt lamellar vest over a quilted sky-blue coat, a gilt cap with two
     * long pheasant feathers, a quiver over his right shoulder, a long gilt recurve bow; his captain in
     * crimson and gold, cloaked. Model and skins: {@code assets_source/heaven_host_ranks.py}.
     */
    public static LayerDefinition archerLayer() {
        // GENERATED archer: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(181, 0).mirror(false).addBox(-2, -0.5f, -2.1f, 4, 6.8f, 4.2f), PartPose.offsetAndRotation(-2.1f, 12, 0.2f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(181, 0).mirror(true).addBox(-2, -0.5f, -2.1f, 4, 6.8f, 4.2f), PartPose.offsetAndRotation(2.1f, 12, 0.2f, 0, 0, 0));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(173, 31).mirror(false).addBox(-1.75f, 0, -1.75f, 3.5f, 4.6f, 3.5f)
                .texOffs(186, 43).mirror(false).addBox(-2.05f, 0.7f, -2.3f, 4.1f, 3.7f, 2.2f)
                .texOffs(62, 60).mirror(false).addBox(-1.95f, -0.8f, -2.65f, 3.9f, 2, 1)
                .texOffs(203, 43).mirror(false).addBox(-1.95f, 4.4f, -2.2f, 3.9f, 1.6f, 4.3f)
                .texOffs(9, 60).mirror(false).addBox(-1.45f, 4.9f, -3.3f, 2.9f, 1.1f, 1.3f)
                .texOffs(124, 60).mirror(false).addBox(-0.8f, 4.3f, -3.85f, 1.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(173, 31).mirror(true).addBox(-1.75f, 0, -1.75f, 3.5f, 4.6f, 3.5f)
                .texOffs(186, 43).mirror(true).addBox(-2.05f, 0.7f, -2.3f, 4.1f, 3.7f, 2.2f)
                .texOffs(62, 60).mirror(true).addBox(-1.95f, -0.8f, -2.65f, 3.9f, 2, 1)
                .texOffs(203, 43).mirror(true).addBox(-1.95f, 4.4f, -2.2f, 3.9f, 1.6f, 4.3f)
                .texOffs(9, 60).mirror(true).addBox(-1.45f, 4.9f, -3.3f, 2.9f, 1.1f, 1.3f)
                .texOffs(124, 60).mirror(true).addBox(-0.8f, 4.3f, -3.85f, 1.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(0, 43).mirror(false).addBox(-4, -1.5f, -2.5f, 8, 3, 5)
                .texOffs(190, 31).mirror(false).addBox(-4.7f, -2.9f, -3.1f, 9.4f, 1.9f, 6.2f)
                .texOffs(20, 60).mirror(false).addBox(-1.4f, -3.15f, -3.55f, 2.8f, 2.3f, 0.6f), PartPose.offsetAndRotation(0, 11.5f, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(85, 31).mirror(false).addBox(-1.4f, 0, -0.3f, 2.8f, 8.6f, 0.6f), PartPose.offsetAndRotation(0, -1.3f, -3.25f, -0.05f, 0, 0));
        PartDefinition robeBack = waist.addOrReplaceChild("robe_back", CubeListBuilder.create()
                .texOffs(228, 0).mirror(false).addBox(-4, 0, -0.3f, 8, 9.8f, 0.6f), PartPose.offsetAndRotation(0, -0.9f, 2.4f, 0.1f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(222, 43).mirror(false).addBox(-2.4f, 0, -0.4f, 4.8f, 5.6f, 0.8f), PartPose.offsetAndRotation(-2.3f, -1.3f, -2.85f, -0.14f, 0.14f, 0.13f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(222, 43).mirror(true).addBox(-2.4f, 0, -0.4f, 4.8f, 5.6f, 0.8f), PartPose.offsetAndRotation(2.3f, -1.3f, -2.85f, -0.14f, -0.14f, -0.13f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(200, 0).mirror(false).addBox(-0.8f, 0, -2.6f, 0.8f, 5.3f, 5.2f), PartPose.offsetAndRotation(-4.25f, -1.3f, 0.1f, 0, 0, 0.27f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(200, 0).mirror(true).addBox(0, 0, -2.6f, 0.8f, 5.3f, 5.2f), PartPose.offsetAndRotation(4.25f, -1.3f, 0.1f, 0, 0, -0.27f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(235, 43).mirror(false).addBox(-4.3f, 0, -0.4f, 8.6f, 5.8f, 0.8f), PartPose.offsetAndRotation(0, -1.3f, 2.75f, 0.16f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(225, 31).mirror(false).addBox(-0.6f, 0, -0.3f, 1.2f, 8, 0.6f), PartPose.offsetAndRotation(-3.5f, -2, -2.5f, -0.04f, 0.2f, 0.12f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(225, 31).mirror(true).addBox(-0.6f, 0, -0.3f, 1.2f, 8, 0.6f), PartPose.offsetAndRotation(3.5f, -2, -2.5f, -0.04f, -0.2f, -0.12f));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(61, 0).mirror(false).addBox(-4, -9.6f, -2.6f, 8, 9.8f, 5.2f)
                .texOffs(119, 0).mirror(false).addBox(-4.6f, -10, -3.1f, 9.2f, 5.8f, 6.2f)
                .texOffs(0, 31).mirror(false).addBox(-4.2f, -4.5f, -2.85f, 8.4f, 4.4f, 5.7f)
                .texOffs(27, 43).mirror(false).addBox(-4.55f, -5, -3.25f, 9.1f, 0.8f, 6.5f)
                .texOffs(62, 43).mirror(false).addBox(-4.35f, -0.9f, -3, 8.7f, 1.2f, 6)
                .texOffs(164, 52).mirror(false).addBox(-1.9f, -4.1f, -3.35f, 3.8f, 3.2f, 0.7f)
                .texOffs(0, 52).mirror(false).addBox(-3, -11, -2.5f, 6, 1.4f, 5)
                .texOffs(23, 52).mirror(false).addBox(-3.3f, -10.5f, -2.9f, 6.6f, 1, 5.8f)
                .texOffs(175, 52).mirror(false).addBox(-1.3f, -11.8f, -1.3f, 2.6f, 1.4f, 2.6f), PartPose.offsetAndRotation(0, -2.7f, 0, 0, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(94, 31).mirror(false).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(-5.3f, -8.6f, 0, 0.08f, 0, 0.12f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(94, 31).mirror(true).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(5.3f, -8.6f, 0, -0.12f, 0, -0.12f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(99, 52).mirror(false).addBox(-2.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(93, 43).mirror(false).addBox(-3, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f), PartPose.offsetAndRotation(-0.3f, -1.6f, 0, 0, 0, 0.34f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(99, 52).mirror(true).addBox(-1.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(93, 43).mirror(true).addBox(-1.6f, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f), PartPose.offsetAndRotation(0.3f, -1.6f, 0, 0, 0, -0.34f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(116, 43).mirror(false).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(129, 43).mirror(false).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f)
                .texOffs(50, 52).mirror(false).addBox(-2.2f, -0.9f, -2.2f, 4.4f, 2, 4.4f), PartPose.offsetAndRotation(0, 3.8f, 0, -0.35f, -0.1f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(116, 43).mirror(true).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(129, 43).mirror(true).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f)
                .texOffs(50, 52).mirror(true).addBox(-2.2f, -0.9f, -2.2f, 4.4f, 2, 4.4f), PartPose.offsetAndRotation(0, 3.8f, 0, -0.55f, 0.15f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(118, 52).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(118, 52).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(90, 0).mirror(false).addBox(-3.3f, -7, -3.3f, 6.6f, 7, 6.6f)
                .texOffs(73, 60).mirror(false).addBox(-0.55f, -4.1f, -3.85f, 1.1f, 1.8f, 0.6f), PartPose.offsetAndRotation(0, -11.3f, -0.1f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(131, 60).mirror(false).addBox(-2.5f, -0.4f, -0.35f, 2.5f, 0.7f, 0.6f), PartPose.offsetAndRotation(-0.35f, -5.05f, -3.4f, 0, 0.1f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(131, 60).mirror(true).addBox(0, -0.4f, -0.35f, 2.5f, 0.7f, 0.6f), PartPose.offsetAndRotation(0.35f, -5.05f, -3.4f, 0, -0.1f, -0.2f));
        PartDefinition helm = head.addOrReplaceChild("helm", CubeListBuilder.create()
                .texOffs(111, 31).mirror(false).addBox(-3.65f, -1.25f, -3.65f, 7.3f, 1.5f, 7.3f)
                .texOffs(144, 31).mirror(false).addBox(-3.45f, -3.45f, -3.45f, 6.9f, 2.3f, 6.9f)
                .texOffs(146, 43).mirror(false).addBox(-2.55f, -4.6f, -2.55f, 5.1f, 1.25f, 5.1f)
                .texOffs(29, 60).mirror(false).addBox(-1.25f, -5.5f, -1.25f, 2.5f, 1, 2.5f)
                .texOffs(188, 52).mirror(false).addBox(-0.35f, -8.6f, -0.35f, 0.7f, 3.2f, 0.7f)
                .texOffs(80, 60).mirror(false).addBox(-0.6f, -9.3f, -0.6f, 1.2f, 1, 1.2f)
                .texOffs(89, 60).mirror(false).addBox(-3.85f, -0.5f, -4.35f, 7.7f, 0.7f, 1.1f), PartPose.offsetAndRotation(0, -5.6f, 0, 0, 0, 0));
        PartDefinition crest = helm.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(193, 52).mirror(false).addBox(-1.5f, -3.8f, -0.3f, 3, 3.8f, 0.5f), PartPose.offsetAndRotation(0, -1, -3.6f, -0.12f, 0, 0));
        PartDefinition rightFlap = helm.addOrReplaceChild("right_flap", CubeListBuilder.create()
                .texOffs(232, 31).mirror(false).addBox(-0.6f, 0, -2.6f, 0.7f, 3.2f, 5), PartPose.offsetAndRotation(-3.55f, -0.3f, 0.3f, 0, 0, 0.1f));
        PartDefinition leftFlap = helm.addOrReplaceChild("left_flap", CubeListBuilder.create()
                .texOffs(232, 31).mirror(true).addBox(-0.1f, 0, -2.6f, 0.7f, 3.2f, 5), PartPose.offsetAndRotation(3.55f, -0.3f, 0.3f, 0, 0, -0.1f));
        PartDefinition neckguard = helm.addOrReplaceChild("neckguard", CubeListBuilder.create()
                .texOffs(202, 52).mirror(false).addBox(-3.55f, 0, -0.4f, 7.1f, 3.2f, 0.8f), PartPose.offsetAndRotation(0, -0.2f, 3.25f, 0.22f, 0, 0));
        PartDefinition rightFeather = helm.addOrReplaceChild("right_feather", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.5f, -3.4f, 2.2f, 0, 0, 0));
        PartDefinition leftFeather = helm.addOrReplaceChild("left_feather", CubeListBuilder.create(), PartPose.offsetAndRotation(1.5f, -3.4f, 2.2f, 0, 0, 0));
        PartDefinition rightFeather0 = rightFeather.addOrReplaceChild("right_feather_0", CubeListBuilder.create()
                .texOffs(171, 43).mirror(false).addBox(-0.15f, -1, -0.1f, 0.3f, 2, 5.0021f), PartPose.offsetAndRotation(0, 0, 0, 1.2142f, -0.3029f, 0));
        PartDefinition leftFeather0 = leftFeather.addOrReplaceChild("left_feather_0", CubeListBuilder.create()
                .texOffs(171, 43).mirror(true).addBox(-0.15f, -1, -0.1f, 0.3f, 2, 5.0021f), PartPose.offsetAndRotation(0, 0, 0, 1.2142f, 0.3029f, 0));
        PartDefinition rightFeather1 = rightFeather.addOrReplaceChild("right_feather_1", CubeListBuilder.create()
                .texOffs(171, 43).mirror(false).addBox(-0.15f, -1, -0.1f, 0.3f, 2, 5.1204f), PartPose.offsetAndRotation(-0.5f, -4.5f, 1.6f, 0.985f, -0.2985f, 0));
        PartDefinition leftFeather1 = leftFeather.addOrReplaceChild("left_feather_1", CubeListBuilder.create()
                .texOffs(171, 43).mirror(true).addBox(-0.15f, -1, -0.1f, 0.3f, 2, 5.1204f), PartPose.offsetAndRotation(0.5f, -4.5f, 1.6f, 0.985f, 0.2985f, 0));
        PartDefinition rightFeather2 = rightFeather.addOrReplaceChild("right_feather_2", CubeListBuilder.create()
                .texOffs(71, 52).mirror(false).addBox(-0.15f, -1, -0.1f, 0.3f, 2, 4.9381f), PartPose.offsetAndRotation(-1.3f, -8.6f, 4.2f, 0.6856f, -0.192f, 0));
        PartDefinition leftFeather2 = leftFeather.addOrReplaceChild("left_feather_2", CubeListBuilder.create()
                .texOffs(71, 52).mirror(true).addBox(-0.15f, -1, -0.1f, 0.3f, 2, 4.9381f), PartPose.offsetAndRotation(1.3f, -8.6f, 4.2f, 0.6856f, 0.192f, 0));
        PartDefinition rightFeather3 = rightFeather.addOrReplaceChild("right_feather_3", CubeListBuilder.create()
                .texOffs(71, 52).mirror(false).addBox(-0.15f, -1, -0.1f, 0.3f, 2, 4.3952f), PartPose.offsetAndRotation(-2, -11.6f, 7.8f, 0.2901f, -0.0997f, 0));
        PartDefinition leftFeather3 = leftFeather.addOrReplaceChild("left_feather_3", CubeListBuilder.create()
                .texOffs(71, 52).mirror(true).addBox(-0.15f, -1, -0.1f, 0.3f, 2, 4.3952f), PartPose.offsetAndRotation(2, -11.6f, 7.8f, 0.2901f, 0.0997f, 0));
        PartDefinition rightFeather4 = rightFeather.addOrReplaceChild("right_feather_4", CubeListBuilder.create()
                .texOffs(131, 52).mirror(false).addBox(-0.15f, -1, -0.1f, 0.3f, 2, 3.8222f), PartPose.offsetAndRotation(-2.4f, -12.8f, 11.8f, -0.1107f, 0, 0));
        PartDefinition leftFeather4 = leftFeather.addOrReplaceChild("left_feather_4", CubeListBuilder.create()
                .texOffs(131, 52).mirror(true).addBox(-0.15f, -1, -0.1f, 0.3f, 2, 3.8222f), PartPose.offsetAndRotation(2.4f, -12.8f, 11.8f, -0.1107f, -0, 0));
        PartDefinition quiver = body.addOrReplaceChild("quiver", CubeListBuilder.create()
                .texOffs(215, 0).mirror(false).addBox(-1.5f, -8.6f, -1.25f, 3, 9, 2.5f)
                .texOffs(221, 52).mirror(false).addBox(-1.75f, -9.4f, -1.5f, 3.5f, 1.2f, 3)
                .texOffs(42, 60).mirror(false).addBox(-1.7f, -1, -1.45f, 3.4f, 1, 2.9f)
                .texOffs(57, 60).mirror(false).addBox(-1.07f, -11, -0.77f, 0.34f, 3, 0.34f)
                .texOffs(57, 60).mirror(false).addBox(-0.32f, -11.6f, -0.42f, 0.34f, 3, 0.34f)
                .texOffs(57, 60).mirror(false).addBox(0.43f, -11, -0.07f, 0.34f, 3, 0.34f)
                .texOffs(57, 60).mirror(false).addBox(1.18f, -11.6f, -0.77f, 0.34f, 3, 0.34f)
                .texOffs(236, 52).mirror(false).addBox(-0.96f, -11.2f, -1.15f, 0.12f, 2.2f, 1.1f)
                .texOffs(236, 52).mirror(false).addBox(-0.21f, -11.8f, -0.8f, 0.12f, 2.2f, 1.1f)
                .texOffs(236, 52).mirror(false).addBox(0.54f, -11.2f, -0.45f, 0.12f, 2.2f, 1.1f)
                .texOffs(236, 52).mirror(false).addBox(1.29f, -11.8f, -1.15f, 0.12f, 2.2f, 1.1f), PartPose.offsetAndRotation(1.6f, -2.6f, 3.6f, 0.1f, 0, -0.5f));
        PartDefinition baldric = body.addOrReplaceChild("baldric", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-0.55f, -6.2f, -3.3f, 1.1f, 12.4f, 6.6f), PartPose.offsetAndRotation(0, -5.4f, 0, 0, 0, -0.62f));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(31, 31).mirror(false).addBox(-5.2f, 0, -0.4f, 10.4f, 10, 0.8f), PartPose.offsetAndRotation(0, -10.4f, 3.3f, 0.12f, 0, 0));
        PartDefinition capeLower = cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(56, 31).mirror(false).addBox(-6.1f, 0, -0.4f, 12.2f, 9.5f, 0.8f), PartPose.offsetAndRotation(0, 9.7f, 0, 0.08f, 0, 0));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(140, 60).mirror(false).addBox(-2.2f, -0.35f, -0.3f, 2.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(-0.3f, -2.25f, -3.45f, 0, 0.15f, -0.35f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(140, 60).mirror(true).addBox(0, -0.35f, -0.3f, 2.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(0.3f, -2.25f, -3.45f, 0, -0.15f, 0.35f));
        PartDefinition bow = leftHand.addOrReplaceChild("bow", CubeListBuilder.create()
                .texOffs(142, 52).mirror(false).addBox(-0.55f, -1.7f, -0.75f, 1.1f, 3.4f, 1.5f), PartPose.offsetAndRotation(0, 1.2f, 0, 2.0135f, -0.8856f, -1.8075f));
        PartDefinition bowUpper = bow.addOrReplaceChild("bow_upper", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition bowUpper0 = bowUpper.addOrReplaceChild("bow_upper_0", CubeListBuilder.create()
                .texOffs(84, 52).mirror(false).addBox(-0.4f, -0.5f, -0.1f, 0.8f, 1, 5.1244f), PartPose.offsetAndRotation(0, -1.4f, -0.2f, 1.3455f, 3.1416f, 0));
        PartDefinition bowUpper1 = bowUpper.addOrReplaceChild("bow_upper_1", CubeListBuilder.create()
                .texOffs(151, 52).mirror(false).addBox(-0.4f, -0.5f, -0.1f, 0.8f, 1, 4.6045f), PartPose.offsetAndRotation(0, -6.2f, -1.3f, 1.5254f, 0, 0));
        PartDefinition bowUpper2 = bowUpper.addOrReplaceChild("bow_upper_2", CubeListBuilder.create()
                .texOffs(243, 52).mirror(false).addBox(-0.4f, -0.5f, -0.1f, 0.8f, 1, 3.0636f), PartPose.offsetAndRotation(0, -10.6f, -1.1f, 1.1384f, 0, 0));
        PartDefinition bowUpper3 = bowUpper.addOrReplaceChild("bow_upper_3", CubeListBuilder.create()
                .texOffs(110, 60).mirror(false).addBox(-0.4f, -0.5f, -0.1f, 0.8f, 1, 1.7f), PartPose.offsetAndRotation(0, -13.2f, 0.1f, 0.9273f, 0, 0));
        PartDefinition bowLower = bow.addOrReplaceChild("bow_lower", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition bowLower0 = bowLower.addOrReplaceChild("bow_lower_0", CubeListBuilder.create()
                .texOffs(84, 52).mirror(false).addBox(-0.4f, -0.5f, -0.1f, 0.8f, 1, 5.1244f), PartPose.offsetAndRotation(0, 1.4f, -0.2f, -1.3455f, 3.1416f, 0));
        PartDefinition bowLower1 = bowLower.addOrReplaceChild("bow_lower_1", CubeListBuilder.create()
                .texOffs(151, 52).mirror(false).addBox(-0.4f, -0.5f, -0.1f, 0.8f, 1, 4.6045f), PartPose.offsetAndRotation(0, 6.2f, -1.3f, -1.5254f, 0, 0));
        PartDefinition bowLower2 = bowLower.addOrReplaceChild("bow_lower_2", CubeListBuilder.create()
                .texOffs(243, 52).mirror(false).addBox(-0.4f, -0.5f, -0.1f, 0.8f, 1, 3.0636f), PartPose.offsetAndRotation(0, 10.6f, -1.1f, -1.1384f, 0, 0));
        PartDefinition bowLower3 = bowLower.addOrReplaceChild("bow_lower_3", CubeListBuilder.create()
                .texOffs(110, 60).mirror(false).addBox(-0.4f, -0.5f, -0.1f, 0.8f, 1, 1.7f), PartPose.offsetAndRotation(0, 13.2f, 0.1f, -0.9273f, 0, 0));
        PartDefinition bowstring = bow.addOrReplaceChild("bowstring", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.12f, 0, -0.12f, 0.24f, 28.6f, 0.24f), PartPose.offsetAndRotation(0, -14.3f, 0.95f, 0, 0, 0));
        PartDefinition drawnUpper = bow.addOrReplaceChild("drawn_upper", CubeListBuilder.create()
                .texOffs(24, 0).mirror(false).addBox(-0.12f, -0.12f, 0, 0.24f, 0.24f, 16.2361f), PartPose.offsetAndRotation(0, -14.4f, 1, -1.0906f, 0, 0));
        PartDefinition drawnLower = bow.addOrReplaceChild("drawn_lower", CubeListBuilder.create()
                .texOffs(24, 0).mirror(false).addBox(-0.12f, -0.12f, 0, 0.24f, 0.24f, 16.2361f), PartPose.offsetAndRotation(0, 14.4f, 1, 1.0906f, 0, 0));
        PartDefinition arrow = bow.addOrReplaceChild("arrow", CubeListBuilder.create()
                .texOffs(154, 0).mirror(false).addBox(-0.18f, -0.18f, -11.7f, 0.36f, 0.36f, 11.9f)
                .texOffs(117, 60).mirror(false).addBox(-0.4f, -0.4f, -13.1f, 0.8f, 0.8f, 1.5f)
                .texOffs(0, 60).mirror(false).addBox(-0.06f, -0.7f, -0.4f, 0.12f, 1.4f, 2.6f), PartPose.offsetAndRotation(0.55f, 0, 8.5f, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED archer: end
    }

    /**
     * The archer: the bow held at his side; fighting, he turns side-on, raises it before him and draws the
     * string back with an arrow nocked, held at full draw, loosed and drawn again with every shot.
     */
    public static class ArcherModel extends TrooperModel<HeavenlyArcherEntity> {
        private final ModelPart leftHand, rightHand, bow, bowstring, drawnUpper, drawnLower, arrow;
        @Nullable
        private final ModelPart rightFeather, leftFeather;

        public ArcherModel(ModelPart root) {
            super(root);
            leftHand = leftForearm.getChild("left_hand");
            rightHand = rightForearm.getChild("right_hand");
            bow = leftHand.getChild("bow");
            bowstring = bow.getChild("bowstring");
            drawnUpper = bow.getChild("drawn_upper");
            drawnLower = bow.getChild("drawn_lower");
            arrow = bow.getChild("arrow");
            rightFeather = opt(helm, "right_feather");
            leftFeather = opt(helm, "left_feather");
        }

        @Override
        protected void arms(HeavenlyArcherEntity e, float swing, float amount, float age, float strike, float breath) {
            super.arms(e, swing, amount, age, strike, breath);
            turn(rightFeather, amount * 0.3f + Mth.sin(age * 0.07f) * 0.05f, 0, Mth.sin(age * 0.05f) * 0.04f);
            turn(leftFeather, amount * 0.3f + Mth.sin(age * 0.07f + 1) * 0.05f, 0, -Mth.sin(age * 0.05f + 1) * 0.04f);
            // at full draw while he fights; loosed for a moment with every shot
            float draw = 0;
            if (e.isAggressive()) {
                int t = e.tickCount % HeavenlyArcherEntity.EVERY;
                draw = t < 4 ? 0.55f + t * 0.1f : 1f;
            }
            boolean drawn = draw > 0.6f;
            bowstring.visible = !drawn;
            drawnUpper.visible = drawnLower.visible = arrow.visible = drawn;
            if (draw <= 0) {
                bow.loadPose(bow.getInitialPose());
                return;
            }
            // side-on, the bow arm straight before him, the bow upright and turned along the arrow's line
            float turnBody = 0.8f * draw;
            body.yRot += turnBody;
            head.yRot -= turnBody;
            blend(leftArm, -1.57f + head.xRot * 0.5f, 0.0f, 0, draw);
            blend(leftForearm, 0, 0, 0, draw);
            leftHand.xRot = leftHand.yRot = leftHand.zRot = 0;
            Vector3f zyx = new Matrix3f().rotationX(1.57f - head.xRot * 0.5f).rotateY(-0.8f).getEulerAnglesZYX(new Vector3f());
            bow.xRot = zyx.x;
            bow.yRot = zyx.y;
            bow.zRot = zyx.z;
            // the string hand drawn back to his chin
            blend(rightArm, -1.6f + head.xRot * 0.5f, -0.75f, 0, draw);
            blend(rightForearm, 0.3f, 0, 0, draw);
            rightHand.xRot = rightHand.yRot = rightHand.zRot = 0;
        }
    }

    /** A part turned toward (x, y, z) by t of the way from where it is. */
    static void blend(ModelPart p, float x, float y, float z, float t) {
        p.xRot += (x - p.xRot) * t;
        p.yRot += (y - p.yRot) * t;
        p.zRot += (z - p.zRot) * t;
    }

    // ── The thunder drummer ─────────────────────────────────────────────

    /**
     * The thunder drummer (256x256): bare-armed and bearded, a red headband, a padded indigo coat studded in
     * gold, three red-lacquered war drums on a frame at his back, a drumstick in each fist; his captain in
     * crimson and gold. Model and skins: {@code assets_source/heaven_host_ranks.py}.
     */
    public static LayerDefinition drummerLayer() {
        // GENERATED drummer: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(183, 0).mirror(false).addBox(-2, -0.5f, -2.1f, 4, 6.8f, 4.2f), PartPose.offsetAndRotation(-2.1f, 12, 0.2f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(183, 0).mirror(true).addBox(-2, -0.5f, -2.1f, 4, 6.8f, 4.2f), PartPose.offsetAndRotation(2.1f, 12, 0.2f, 0, 0, 0));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(193, 28).mirror(false).addBox(-1.75f, 0, -1.75f, 3.5f, 4.6f, 3.5f)
                .texOffs(207, 41).mirror(false).addBox(-2.05f, 0.7f, -2.3f, 4.1f, 3.7f, 2.2f)
                .texOffs(9, 59).mirror(false).addBox(-1.95f, -0.8f, -2.65f, 3.9f, 2, 1)
                .texOffs(224, 41).mirror(false).addBox(-1.95f, 4.4f, -2.2f, 3.9f, 1.6f, 4.3f)
                .texOffs(191, 51).mirror(false).addBox(-1.45f, 4.9f, -3.3f, 2.9f, 1.1f, 1.3f)
                .texOffs(36, 59).mirror(false).addBox(-0.8f, 4.3f, -3.85f, 1.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(193, 28).mirror(true).addBox(-1.75f, 0, -1.75f, 3.5f, 4.6f, 3.5f)
                .texOffs(207, 41).mirror(true).addBox(-2.05f, 0.7f, -2.3f, 4.1f, 3.7f, 2.2f)
                .texOffs(9, 59).mirror(true).addBox(-1.95f, -0.8f, -2.65f, 3.9f, 2, 1)
                .texOffs(224, 41).mirror(true).addBox(-1.95f, 4.4f, -2.2f, 3.9f, 1.6f, 4.3f)
                .texOffs(191, 51).mirror(true).addBox(-1.45f, 4.9f, -3.3f, 2.9f, 1.1f, 1.3f)
                .texOffs(36, 59).mirror(true).addBox(-0.8f, 4.3f, -3.85f, 1.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(36, 41).mirror(false).addBox(-4, -1.5f, -2.5f, 8, 3, 5)
                .texOffs(210, 28).mirror(false).addBox(-4.7f, -2.9f, -3.1f, 9.4f, 1.9f, 6.2f)
                .texOffs(202, 51).mirror(false).addBox(-1.4f, -3.15f, -3.55f, 2.8f, 2.3f, 0.6f), PartPose.offsetAndRotation(0, 11.5f, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(245, 28).mirror(false).addBox(-1.4f, 0, -0.3f, 2.8f, 8, 0.6f), PartPose.offsetAndRotation(0, -1.3f, -3.25f, -0.05f, 0, 0));
        PartDefinition robeBack = waist.addOrReplaceChild("robe_back", CubeListBuilder.create()
                .texOffs(17, 28).mirror(false).addBox(-4, 0, -0.3f, 8, 9.2f, 0.6f), PartPose.offsetAndRotation(0, -0.9f, 2.4f, 0.1f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(93, 51).mirror(false).addBox(-2.4f, 0, -0.4f, 4.8f, 5, 0.8f), PartPose.offsetAndRotation(-2.3f, -1.3f, -2.85f, -0.14f, 0.14f, 0.13f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(93, 51).mirror(true).addBox(-2.4f, 0, -0.4f, 4.8f, 5, 0.8f), PartPose.offsetAndRotation(2.3f, -1.3f, -2.85f, -0.14f, -0.14f, -0.13f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(36, 28).mirror(false).addBox(-0.8f, 0, -2.6f, 0.8f, 4.7f, 5.2f), PartPose.offsetAndRotation(-4.25f, -1.3f, 0.1f, 0, 0, 0.27f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(36, 28).mirror(true).addBox(0, 0, -2.6f, 0.8f, 4.7f, 5.2f), PartPose.offsetAndRotation(4.25f, -1.3f, 0.1f, 0, 0, -0.27f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(0, 51).mirror(false).addBox(-4.3f, 0, -0.4f, 8.6f, 5.2f, 0.8f), PartPose.offsetAndRotation(0, -1.3f, 2.75f, 0.16f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(0, 41).mirror(false).addBox(-0.6f, 0, -0.3f, 1.2f, 7.4f, 0.6f), PartPose.offsetAndRotation(-3.5f, -2, -2.5f, -0.04f, 0.2f, 0.12f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(0, 41).mirror(true).addBox(-0.6f, 0, -0.3f, 1.2f, 7.4f, 0.6f), PartPose.offsetAndRotation(3.5f, -2, -2.5f, -0.04f, -0.2f, -0.12f));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-4, -9.6f, -2.6f, 8, 9.8f, 5.2f)
                .texOffs(148, 0).mirror(false).addBox(-4.6f, -10, -3.1f, 9.2f, 5.8f, 6.2f)
                .texOffs(51, 28).mirror(false).addBox(-4.2f, -4.5f, -2.85f, 8.4f, 4.4f, 5.7f)
                .texOffs(63, 41).mirror(false).addBox(-4.55f, -5, -3.25f, 9.1f, 0.8f, 6.5f)
                .texOffs(98, 41).mirror(false).addBox(-4.35f, -0.9f, -3, 8.7f, 1.2f, 6)
                .texOffs(167, 51).mirror(false).addBox(-1.9f, -4.1f, -3.35f, 3.8f, 3.2f, 0.7f)
                .texOffs(21, 51).mirror(false).addBox(-3, -11, -2.5f, 6, 1.4f, 5)
                .texOffs(44, 51).mirror(false).addBox(-3.3f, -10.5f, -2.9f, 6.6f, 1, 5.8f)
                .texOffs(178, 51).mirror(false).addBox(-1.3f, -11.8f, -1.3f, 2.6f, 1.4f, 2.6f), PartPose.offsetAndRotation(0, -2.7f, 0, 0, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(105, 28).mirror(false).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(-5.3f, -8.6f, 0, -0.3f, 0, 0.16f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(105, 28).mirror(true).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(5.3f, -8.6f, 0, -0.3f, 0, -0.16f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(106, 51).mirror(false).addBox(-2.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(129, 41).mirror(false).addBox(-3, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f)
                .texOffs(125, 51).mirror(false).addBox(-4.6f, 0.7f, -1.5f, 0.7f, 2.8f, 3), PartPose.offsetAndRotation(-0.3f, -1.6f, 0, 0, 0, 0.34f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(106, 51).mirror(true).addBox(-1.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(129, 41).mirror(true).addBox(-1.6f, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f)
                .texOffs(125, 51).mirror(true).addBox(3.9f, 0.7f, -1.5f, 0.7f, 2.8f, 3), PartPose.offsetAndRotation(0.3f, -1.6f, 0, 0, 0, -0.34f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(152, 41).mirror(false).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(165, 41).mirror(false).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f), PartPose.offsetAndRotation(0, 3.8f, 0, -1, -0.35f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(152, 41).mirror(true).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(165, 41).mirror(true).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f), PartPose.offsetAndRotation(0, 3.8f, 0, -1, 0.35f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(134, 51).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(134, 51).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(119, 0).mirror(false).addBox(-3.3f, -7, -3.3f, 6.6f, 7, 6.6f)
                .texOffs(20, 59).mirror(false).addBox(-0.55f, -4.1f, -3.85f, 1.1f, 1.8f, 0.6f), PartPose.offsetAndRotation(0, -11.3f, -0.1f, 0, 0, 0));
        PartDefinition hair = head.addOrReplaceChild("hair", CubeListBuilder.create()
                .texOffs(7, 41).mirror(false).addBox(-3.5f, -7.4f, -3.4f, 7, 2, 7)
                .texOffs(147, 51).mirror(false).addBox(-1.3f, -9.6f, -0.6f, 2.6f, 2.4f, 2.6f)
                .texOffs(43, 59).mirror(false).addBox(-2.4f, -8.8f, 0.4f, 4.8f, 0.5f, 0.5f)
                .texOffs(122, 28).mirror(false).addBox(-3.6f, -6.3f, -3.6f, 7.2f, 1.3f, 7.2f)
                .texOffs(27, 59).mirror(false).addBox(-1.1f, -6.6f, -3.85f, 2.2f, 1.7f, 0.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition bandTail = hair.addOrReplaceChild("band_tail", CubeListBuilder.create()
                .texOffs(71, 51).mirror(false).addBox(-1.6f, 0, -0.2f, 1.4f, 5.5f, 0.4f)
                .texOffs(160, 51).mirror(false).addBox(0.2f, 0, -0.2f, 1.4f, 4.6f, 0.4f), PartPose.offsetAndRotation(0, -5.8f, 3.6f, 0.5f, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(56, 59).mirror(false).addBox(-2.6f, -0.5f, -0.4f, 2.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(-0.35f, -5.05f, -3.4f, 0, 0.1f, 0.32f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(56, 59).mirror(true).addBox(0, -0.5f, -0.4f, 2.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0.35f, -5.05f, -3.4f, 0, -0.1f, -0.32f));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(65, 59).mirror(false).addBox(-2.4f, -0.35f, -0.3f, 2.4f, 0.7f, 0.6f), PartPose.offsetAndRotation(-0.3f, -2.25f, -3.45f, 0, 0.15f, -0.45f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(65, 59).mirror(true).addBox(0, -0.35f, -0.3f, 2.4f, 0.7f, 0.6f), PartPose.offsetAndRotation(0.3f, -2.25f, -3.45f, 0, -0.15f, 0.45f));
        PartDefinition beard = head.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(78, 51).mirror(false).addBox(-2.3f, 0, -0.6f, 4.6f, 4.8f, 1.2f), PartPose.offsetAndRotation(0, -1, -3.3f, 0.2f, 0, 0));
        PartDefinition drumFrame = body.addOrReplaceChild("drum_frame", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-5.9f, -25.5f, 0, 1, 25.5f, 1)
                .texOffs(0, 0).mirror(false).addBox(4.9f, -25.5f, 0, 1, 25.5f, 1)
                .texOffs(211, 51).mirror(false).addBox(-6.6f, -26.3f, -0.1f, 13.2f, 1.2f, 1.2f)
                .texOffs(74, 59).mirror(false).addBox(-11, -13.6f, 0, 22, 1, 1)
                .texOffs(244, 51).mirror(false).addBox(-6.3f, -28.2f, -0.2f, 1.8f, 2, 1.4f)
                .texOffs(244, 51).mirror(false).addBox(4.5f, -28.2f, -0.2f, 1.8f, 2, 1.4f), PartPose.offsetAndRotation(0, 0, 3.4f, 0, 0, 0));
        PartDefinition drumTop = drumFrame.addOrReplaceChild("drum_top", CubeListBuilder.create()
                .texOffs(155, 28).mirror(false).addBox(-5.4f, -2.268f, -2.2f, 10.8f, 4.536f, 4.4f)
                .texOffs(34, 0).mirror(false).addBox(-2.268f, -5.4f, -2.2f, 4.536f, 10.8f, 4.4f)
                .texOffs(88, 0).mirror(false).addBox(-4.536f, -4.536f, -2.15f, 9.072f, 9.072f, 4.3f)
                .texOffs(55, 0).mirror(false).addBox(-5.2f, -5.2f, -2.35f, 10.4f, 10.4f, 4.7f), PartPose.offsetAndRotation(0, -20.4f, 2, 0, 0, 0));
        PartDefinition rightDrum = drumFrame.addOrReplaceChild("right_drum", CubeListBuilder.create()
                .texOffs(182, 41).mirror(false).addBox(-4, -1.68f, -1.6f, 8, 3.36f, 3.2f)
                .texOffs(202, 0).mirror(false).addBox(-1.68f, -4, -1.6f, 3.36f, 8, 3.2f)
                .texOffs(82, 28).mirror(false).addBox(-3.36f, -3.36f, -1.55f, 6.72f, 6.72f, 3.1f)
                .texOffs(219, 0).mirror(false).addBox(-3.8f, -3.8f, -1.75f, 7.6f, 7.6f, 3.5f), PartPose.offsetAndRotation(-10.4f, -12.6f, 0.6f, 0, 1.5708f, 0.08f));
        PartDefinition leftDrum = drumFrame.addOrReplaceChild("left_drum", CubeListBuilder.create()
                .texOffs(182, 41).mirror(true).addBox(-4, -1.68f, -1.6f, 8, 3.36f, 3.2f)
                .texOffs(202, 0).mirror(true).addBox(-1.68f, -4, -1.6f, 3.36f, 8, 3.2f)
                .texOffs(82, 28).mirror(true).addBox(-3.36f, -3.36f, -1.55f, 6.72f, 6.72f, 3.1f)
                .texOffs(219, 0).mirror(true).addBox(-3.8f, -3.8f, -1.75f, 7.6f, 7.6f, 3.5f), PartPose.offsetAndRotation(10.4f, -12.6f, 0.6f, 0, -1.5708f, -0.08f));
        PartDefinition rightPennant = drumFrame.addOrReplaceChild("right_pennant", CubeListBuilder.create()
                .texOffs(0, 28).mirror(false).addBox(-0.1f, -0.2f, 0, 0.2f, 4.4f, 6.6f), PartPose.offsetAndRotation(-5.4f, -28.2f, 0.5f, 0, 0, 0));
        PartDefinition leftPennant = drumFrame.addOrReplaceChild("left_pennant", CubeListBuilder.create()
                .texOffs(0, 28).mirror(true).addBox(-0.1f, -0.2f, 0, 0.2f, 4.4f, 6.6f), PartPose.offsetAndRotation(5.4f, -28.2f, 0.5f, 0, 0, 0));
        PartDefinition rightStick = rightHand.addOrReplaceChild("right_stick", CubeListBuilder.create()
                .texOffs(188, 28).mirror(false).addBox(-0.32f, -7.6f, -0.32f, 0.64f, 9, 0.64f)
                .texOffs(0, 59).mirror(false).addBox(-0.85f, -9.1f, -0.85f, 1.7f, 1.7f, 1.7f)
                .texOffs(121, 59).mirror(false).addBox(-0.45f, 1.2f, -0.45f, 0.9f, 0.6f, 0.9f), PartPose.offsetAndRotation(0, 1.2f, 0, 1.8174f, 0.3415f, 0.2574f));
        PartDefinition leftStick = leftHand.addOrReplaceChild("left_stick", CubeListBuilder.create()
                .texOffs(188, 28).mirror(true).addBox(-0.32f, -7.6f, -0.32f, 0.64f, 9, 0.64f)
                .texOffs(0, 59).mirror(true).addBox(-0.85f, -9.1f, -0.85f, 1.7f, 1.7f, 1.7f)
                .texOffs(121, 59).mirror(true).addBox(-0.45f, 1.2f, -0.45f, 0.9f, 0.6f, 0.9f), PartPose.offsetAndRotation(0, 1.2f, 0, 1.8174f, -0.3415f, -0.2574f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED drummer: end
    }

    /**
     * The drummer: his sticks raised before him, tapping the time as he hovers; a blow brings them down;
     * winding up his roll, both sticks lifted high over his head, trembling.
     */
    public static class DrummerModel extends TrooperModel<ThunderDrummerEntity> {
        @Nullable
        private final ModelPart frame, bandTail, rightPennant, leftPennant, rightStick, leftStick;

        public DrummerModel(ModelPart root) {
            super(root);
            frame = opt(body, "drum_frame");
            bandTail = opt(opt(head, "hair"), "band_tail");
            rightPennant = opt(frame, "right_pennant");
            leftPennant = opt(frame, "left_pennant");
            rightStick = opt(opt(rightForearm, "right_hand"), "right_stick");
            leftStick = opt(opt(leftForearm, "left_hand"), "left_stick");
        }

        @Override
        protected void arms(ThunderDrummerEntity e, float swing, float amount, float age, float strike, float breath) {
            float beat = Mth.sin(age * 0.35f) * 0.12f;
            turn(rightArm, beat - strike * 1.1f, 0, 0);
            turn(leftArm, -beat - strike * 0.6f, 0, 0);
            turn(rightForearm, -strike * 0.3f, 0, 0);
            turn(leftForearm, -strike * 0.2f, 0, 0);
            turn(bandTail, amount * 0.6f + Mth.sin(age * 0.11f) * 0.12f, 0, Mth.sin(age * 0.07f) * 0.1f);
            turn(rightPennant, 0, Mth.sin(age * 0.2f) * 0.25f, 0);
            turn(leftPennant, 0, -Mth.sin(age * 0.2f + 1) * 0.25f, 0);
            turn(frame, 0, 0, Mth.sin(age * 0.08f) * 0.015f);
            turn(rightStick, 0, 0, 0);
            turn(leftStick, 0, 0, 0);
            if (e.winding() > 0) {
                // both sticks lifted high over his head, trembling with the roll to come
                float shake = Mth.sin(age * 2.1f) * 0.05f;
                turn(rightArm, -2.6f + shake, 0, -0.55f);
                turn(leftArm, -2.6f - shake, 0, 0.55f);
                turn(rightForearm, 0.75f, 0, 0);
                turn(leftForearm, 0.75f, 0, 0);
                // the sticks held up, crossed over his head
                turn(rightStick, 1.4f, 0, -0.5f);
                turn(leftStick, 1.4f, 0, 0.5f);
                head.xRot -= 0.25f;
            }
        }
    }

    // ── The Lunar Mansions ──────────────────────────────────────────────

    /**
     * A Lunar Mansion (256x256, one frame for the twenty-eight): lamellae lacquered in his quarter's colour
     * and edged in gold, phoenix wings, his mansion's beast as the great crest before his helmet and on the
     * round shield on his left arm, a straight sword, a cloak of his quarter's colour. Each mansion's own skin.
     * Model and skins: {@code assets_source/heaven_host_mansions.py}.
     */
    public static LayerDefinition mansionLayer() {
        // GENERATED mansion: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(155, 0).mirror(false).addBox(-2, -0.5f, -2.1f, 4, 6.8f, 4.2f), PartPose.offsetAndRotation(-2.1f, 12, 0.2f, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(155, 0).mirror(true).addBox(-2, -0.5f, -2.1f, 4, 6.8f, 4.2f), PartPose.offsetAndRotation(2.1f, 12, 0.2f, 0, 0, 0));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(170, 17).mirror(false).addBox(-1.75f, 0, -1.75f, 3.5f, 4.6f, 3.5f)
                .texOffs(42, 40).mirror(false).addBox(-2.05f, 0.7f, -2.3f, 4.1f, 3.7f, 2.2f)
                .texOffs(78, 49).mirror(false).addBox(-1.95f, -0.8f, -2.65f, 3.9f, 2, 1)
                .texOffs(59, 40).mirror(false).addBox(-1.95f, 4.4f, -2.2f, 3.9f, 1.6f, 4.3f)
                .texOffs(32, 49).mirror(false).addBox(-1.45f, 4.9f, -3.3f, 2.9f, 1.1f, 1.3f)
                .texOffs(155, 49).mirror(false).addBox(-0.8f, 4.3f, -3.85f, 1.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(170, 17).mirror(true).addBox(-1.75f, 0, -1.75f, 3.5f, 4.6f, 3.5f)
                .texOffs(42, 40).mirror(true).addBox(-2.05f, 0.7f, -2.3f, 4.1f, 3.7f, 2.2f)
                .texOffs(78, 49).mirror(true).addBox(-1.95f, -0.8f, -2.65f, 3.9f, 2, 1)
                .texOffs(59, 40).mirror(true).addBox(-1.95f, 4.4f, -2.2f, 3.9f, 1.6f, 4.3f)
                .texOffs(32, 49).mirror(true).addBox(-1.45f, 4.9f, -3.3f, 2.9f, 1.1f, 1.3f)
                .texOffs(155, 49).mirror(true).addBox(-0.8f, 4.3f, -3.85f, 1.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(67, 30).mirror(false).addBox(-4, -1.5f, -2.5f, 8, 3, 5)
                .texOffs(187, 17).mirror(false).addBox(-4.7f, -2.9f, -3.1f, 9.4f, 1.9f, 6.2f)
                .texOffs(43, 49).mirror(false).addBox(-1.4f, -3.15f, -3.55f, 2.8f, 2.3f, 0.6f), PartPose.offsetAndRotation(0, 11.5f, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(29, 17).mirror(false).addBox(-1.4f, 0, -0.3f, 2.8f, 9.8f, 0.6f), PartPose.offsetAndRotation(0, -1.3f, -3.25f, -0.05f, 0, 0));
        PartDefinition robeBack = waist.addOrReplaceChild("robe_back", CubeListBuilder.create()
                .texOffs(174, 0).mirror(false).addBox(-4, 0, -0.3f, 8, 11, 0.6f), PartPose.offsetAndRotation(0, -0.9f, 2.4f, 0.1f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(222, 17).mirror(false).addBox(-2.4f, 0, -0.4f, 4.8f, 7.6f, 0.8f), PartPose.offsetAndRotation(-2.3f, -1.3f, -2.85f, -0.14f, 0.14f, 0.13f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(222, 17).mirror(true).addBox(-2.4f, 0, -0.4f, 4.8f, 7.6f, 0.8f), PartPose.offsetAndRotation(2.3f, -1.3f, -2.85f, -0.14f, -0.14f, -0.13f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(36, 0).mirror(false).addBox(-0.8f, 0, -2.6f, 0.8f, 7.3f, 5.2f), PartPose.offsetAndRotation(-4.25f, -1.3f, 0.1f, 0, 0, 0.27f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(36, 0).mirror(true).addBox(0, 0, -2.6f, 0.8f, 7.3f, 5.2f), PartPose.offsetAndRotation(4.25f, -1.3f, 0.1f, 0, 0, -0.27f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(235, 17).mirror(false).addBox(-4.3f, 0, -0.4f, 8.6f, 7.8f, 0.8f), PartPose.offsetAndRotation(0, -1.3f, 2.75f, 0.16f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(38, 17).mirror(false).addBox(-0.6f, 0, -0.3f, 1.2f, 9.2f, 0.6f), PartPose.offsetAndRotation(-3.5f, -2, -2.5f, -0.04f, 0.2f, 0.12f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(38, 17).mirror(true).addBox(-0.6f, 0, -0.3f, 1.2f, 9.2f, 0.6f), PartPose.offsetAndRotation(3.5f, -2, -2.5f, -0.04f, -0.2f, -0.12f));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-4, -9.6f, -2.6f, 8, 9.8f, 5.2f)
                .texOffs(80, 0).mirror(false).addBox(-4.6f, -10, -3.1f, 9.2f, 5.8f, 6.2f)
                .texOffs(45, 17).mirror(false).addBox(-4.2f, -4.5f, -2.85f, 8.4f, 4.4f, 5.7f)
                .texOffs(94, 30).mirror(false).addBox(-4.55f, -5, -3.25f, 9.1f, 0.8f, 6.5f)
                .texOffs(129, 30).mirror(false).addBox(-4.35f, -0.9f, -3, 8.7f, 1.2f, 6)
                .texOffs(223, 40).mirror(false).addBox(-1.9f, -4.1f, -3.35f, 3.8f, 3.2f, 0.7f)
                .texOffs(78, 40).mirror(false).addBox(-3, -11, -2.5f, 6, 1.4f, 5)
                .texOffs(101, 40).mirror(false).addBox(-3.3f, -10.5f, -2.9f, 6.6f, 1, 5.8f)
                .texOffs(234, 40).mirror(false).addBox(-1.3f, -11.8f, -1.3f, 2.6f, 1.4f, 2.6f)
                .texOffs(149, 40).mirror(false).addBox(-0.45f, -9.6f, -3.35f, 0.9f, 4.8f, 0.35f)
                .texOffs(89, 49).mirror(false).addBox(-0.9f, -10, -3.55f, 1.8f, 1.4f, 0.6f)
                .texOffs(0, 49).mirror(false).addBox(-3.95f, -9.3f, -3.55f, 3.1f, 3.1f, 0.5f)
                .texOffs(0, 49).mirror(false).addBox(0.85f, -9.3f, -3.55f, 3.1f, 3.1f, 0.5f), PartPose.offsetAndRotation(0, -2.7f, 0, 0, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(91, 17).mirror(false).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(-5.3f, -8.6f, 0, -0.22f, 0, 0.1f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(91, 17).mirror(true).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(5.3f, -8.6f, 0, -0.3f, 0, -0.18f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(154, 40).mirror(false).addBox(-2.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(160, 30).mirror(false).addBox(-3, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f)
                .texOffs(0, 30).mirror(false).addBox(-3.5f, 0.4f, -3.05f, 5, 1.4f, 6.1f)
                .texOffs(25, 30).mirror(false).addBox(-4, 1.6f, -3.4f, 5.4f, 1.6f, 6.8f)
                .texOffs(173, 40).mirror(false).addBox(-4.6f, 0.7f, -1.5f, 0.7f, 2.8f, 3), PartPose.offsetAndRotation(-0.3f, -1.6f, 0, 0, 0, 0.34f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(154, 40).mirror(true).addBox(-1.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(160, 30).mirror(true).addBox(-1.6f, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f)
                .texOffs(0, 30).mirror(true).addBox(-1.5f, 0.4f, -3.05f, 5, 1.4f, 6.1f)
                .texOffs(25, 30).mirror(true).addBox(-1.4f, 1.6f, -3.4f, 5.4f, 1.6f, 6.8f)
                .texOffs(173, 40).mirror(true).addBox(3.9f, 0.7f, -1.5f, 0.7f, 2.8f, 3), PartPose.offsetAndRotation(0.3f, -1.6f, 0, 0, 0, -0.34f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(183, 30).mirror(false).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(196, 30).mirror(false).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f)
                .texOffs(128, 40).mirror(false).addBox(-2.2f, -0.9f, -2.2f, 4.4f, 2, 4.4f), PartPose.offsetAndRotation(0, 3.8f, 0, -0.95f, -0.3f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(183, 30).mirror(true).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(196, 30).mirror(true).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f)
                .texOffs(128, 40).mirror(true).addBox(-2.2f, -0.9f, -2.2f, 4.4f, 2, 4.4f), PartPose.offsetAndRotation(0, 3.8f, 0, -1.15f, 0.45f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(182, 40).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(182, 40).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(51, 0).mirror(false).addBox(-3.3f, -7, -3.3f, 6.6f, 7, 6.6f)
                .texOffs(96, 49).mirror(false).addBox(-0.55f, -4.1f, -3.85f, 1.1f, 1.8f, 0.6f), PartPose.offsetAndRotation(0, -11.3f, -0.1f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(162, 49).mirror(false).addBox(-2.5f, -0.4f, -0.35f, 2.5f, 0.7f, 0.6f), PartPose.offsetAndRotation(-0.35f, -5.05f, -3.4f, 0, 0.1f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(162, 49).mirror(true).addBox(0, -0.4f, -0.35f, 2.5f, 0.7f, 0.6f), PartPose.offsetAndRotation(0.35f, -5.05f, -3.4f, 0, -0.1f, -0.2f));
        PartDefinition helm = head.addOrReplaceChild("helm", CubeListBuilder.create()
                .texOffs(108, 17).mirror(false).addBox(-3.65f, -1.25f, -3.65f, 7.3f, 1.5f, 7.3f)
                .texOffs(141, 17).mirror(false).addBox(-3.45f, -3.45f, -3.45f, 6.9f, 2.3f, 6.9f)
                .texOffs(213, 30).mirror(false).addBox(-2.55f, -4.6f, -2.55f, 5.1f, 1.25f, 5.1f)
                .texOffs(52, 49).mirror(false).addBox(-1.25f, -5.5f, -1.25f, 2.5f, 1, 2.5f)
                .texOffs(11, 49).mirror(false).addBox(-0.35f, -8.6f, -0.35f, 0.7f, 3.2f, 0.7f)
                .texOffs(103, 49).mirror(false).addBox(-0.6f, -9.3f, -0.6f, 1.2f, 1, 1.2f)
                .texOffs(112, 49).mirror(false).addBox(-3.85f, -0.5f, -4.35f, 7.7f, 0.7f, 1.1f), PartPose.offsetAndRotation(0, -5.6f, 0, 0, 0, 0));
        PartDefinition rightFlap = helm.addOrReplaceChild("right_flap", CubeListBuilder.create()
                .texOffs(76, 17).mirror(false).addBox(-0.6f, 0, -2.9f, 0.7f, 4.8f, 5.6f), PartPose.offsetAndRotation(-3.55f, -0.3f, 0.3f, 0, 0, 0.16f));
        PartDefinition leftFlap = helm.addOrReplaceChild("left_flap", CubeListBuilder.create()
                .texOffs(76, 17).mirror(true).addBox(-0.1f, 0, -2.9f, 0.7f, 4.8f, 5.6f), PartPose.offsetAndRotation(3.55f, -0.3f, 0.3f, 0, 0, -0.16f));
        PartDefinition neckguard = helm.addOrReplaceChild("neckguard", CubeListBuilder.create()
                .texOffs(195, 40).mirror(false).addBox(-3.55f, 0, -0.4f, 7.1f, 4.6f, 0.8f), PartPose.offsetAndRotation(0, -0.2f, 3.25f, 0.28f, 0, 0));
        PartDefinition plume = helm.addOrReplaceChild("plume", CubeListBuilder.create()
                .texOffs(65, 49).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 1, 2.6f)
                .texOffs(0, 40).mirror(false).addBox(-2.5f, 0.8f, -2.5f, 5, 2.5f, 5), PartPose.offsetAndRotation(0, -7, 0, 0, 0, 0));
        PartDefinition plumeTail = plume.addOrReplaceChild("plume_tail", CubeListBuilder.create()
                .texOffs(52, 30).mirror(false).addBox(-2, 0, -1.1f, 4, 6, 2.2f), PartPose.offsetAndRotation(0, 1.6f, 1.8f, 0.6f, 0, 0));
        PartDefinition rightWing = helm.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(115, 0).mirror(false).addBox(-0.25f, -7.6f, -2, 0.5f, 8, 5), PartPose.offsetAndRotation(-3.6f, -1.6f, 0.4f, 0.3f, 0.35f, -0.6f));
        PartDefinition leftWing = helm.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(115, 0).mirror(true).addBox(-0.25f, -7.6f, -2, 0.5f, 8, 5), PartPose.offsetAndRotation(3.6f, -1.6f, 0.4f, 0.3f, -0.35f, 0.6f));
        PartDefinition emblem = helm.addOrReplaceChild("emblem", CubeListBuilder.create()
                .texOffs(21, 40).mirror(false).addBox(-4.5f, -7, -0.25f, 9, 7, 0.5f), PartPose.offsetAndRotation(0, -1.6f, -3.75f, -0.08f, 0, 0));
        PartDefinition jian = rightHand.addOrReplaceChild("jian", CubeListBuilder.create()
                .texOffs(29, 0).mirror(false).addBox(-0.62f, -16.4f, -0.2f, 1.24f, 14, 0.4f)
                .texOffs(133, 49).mirror(false).addBox(-1.7f, -2.6f, -0.65f, 3.4f, 0.9f, 1.3f)
                .texOffs(16, 49).mirror(false).addBox(-0.42f, -1.8f, -0.42f, 0.84f, 3.4f, 0.84f)
                .texOffs(146, 49).mirror(false).addBox(-0.65f, 1.5f, -0.65f, 1.3f, 1, 1.3f), PartPose.offsetAndRotation(0, 1.2f, 0, 1.2631f, 0.2655f, 0.2082f));
        PartDefinition jianTassel = jian.addOrReplaceChild("jian_tassel", CubeListBuilder.create()
                .texOffs(214, 40).mirror(false).addBox(-0.7f, 0, -0.7f, 1.4f, 3.6f, 1.4f), PartPose.offsetAndRotation(0, 2.4f, 0, 0.1f, 0, 0));
        PartDefinition shield = leftForearm.addOrReplaceChild("shield", CubeListBuilder.create()
                .texOffs(193, 0).mirror(false).addBox(-5.5f, -5.5f, -3.7f, 11, 11, 0.8f)
                .texOffs(128, 0).mirror(false).addBox(-6, -6, -3, 12, 12, 0.7f)
                .texOffs(21, 49).mirror(false).addBox(-1, -1, -2.4f, 2, 2, 2.4f), PartPose.offsetAndRotation(0.2f, 2.6f, 0, 1.5335f, -0.358f, -0.3994f));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(218, 0).mirror(false).addBox(-5.2f, 0, -0.4f, 10.4f, 10.4f, 0.8f), PartPose.offsetAndRotation(0, -10.4f, 3.3f, 0.12f, 0, 0));
        PartDefinition capeLower = cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(0, 17).mirror(false).addBox(-6.1f, 0, -0.4f, 12.2f, 10.2f, 0.8f), PartPose.offsetAndRotation(0, 10.1f, 0, 0.08f, 0, 0));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(171, 49).mirror(false).addBox(-2.2f, -0.35f, -0.3f, 2.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(-0.3f, -2.25f, -3.45f, 0, 0.15f, -0.35f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(171, 49).mirror(true).addBox(0, -0.35f, -0.3f, 2.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(0.3f, -2.25f, -3.45f, 0, -0.15f, 0.35f));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED mansion: end
    }

    /** A Lunar Mansion: the trooper's motion, his sword chopped down on a blow, the shield thrust out to meet it; all his finery always shown. */
    public static class MansionModel extends TrooperModel<LunarMansionEntity> {
        public MansionModel(ModelPart root) {
            super(root);
        }

        @Override
        public void prepareMobModel(LunarMansionEntity e, float swing, float amount, float partial) {
            show(captainOnly, true);
        }

        @Override
        protected void arms(LunarMansionEntity e, float swing, float amount, float age, float strike, float breath) {
            float w = swing * 0.6662f;
            turn(rightArm, -Mth.cos(w) * 0.08f * amount - strike * 1.4f + breath * 0.02f, 0, strike * 0.2f);
            turn(rightForearm, strike * 0.5f, 0, 0);
            turn(leftArm, -strike * 0.35f + breath * 0.02f, 0, 0);
            turn(leftForearm, 0, 0, 0);
        }
    }

    // ── The cloud cavalry ───────────────────────────────────────────────

    /**
     * The cloud cavalry's rider (256x256; his horse is vanilla's): the heavenly soldier's armour, seated
     * astride, a long red-tasselled spear with a pennant. Model and skins: {@code assets_source/heaven_host_cavalry.py}.
     */
    public static LayerDefinition cavalryLayer() {
        // GENERATED cavalry: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rider = root.addOrReplaceChild("rider", CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition waist = rider.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(0, 57).mirror(false).addBox(-4, -1.5f, -2.5f, 8, 3, 5)
                .texOffs(62, 46).mirror(false).addBox(-4.7f, -2.9f, -3.1f, 9.4f, 1.9f, 6.2f)
                .texOffs(0, 74).mirror(false).addBox(-1.4f, -3.15f, -3.55f, 2.8f, 2.3f, 0.6f), PartPose.offsetAndRotation(0, -1.6f, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(27, 57).mirror(false).addBox(-1.4f, 0, -0.3f, 2.8f, 6.5f, 0.6f), PartPose.offsetAndRotation(0, -1.3f, -3.25f, -0.05f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(0, 66).mirror(false).addBox(-2.4f, 0, -0.4f, 4.8f, 6, 0.8f), PartPose.offsetAndRotation(-2.3f, -1.3f, -2.85f, -0.14f, 0.14f, 0.13f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(0, 66).mirror(true).addBox(-2.4f, 0, -0.4f, 4.8f, 6, 0.8f), PartPose.offsetAndRotation(2.3f, -1.3f, -2.85f, -0.14f, -0.14f, -0.13f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(132, 0).mirror(false).addBox(-0.8f, 0, -2.6f, 0.8f, 5.7f, 5.2f), PartPose.offsetAndRotation(-4.25f, -1.3f, 0.1f, 0, 0, 0.27f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(132, 0).mirror(true).addBox(0, 0, -2.6f, 0.8f, 5.7f, 5.2f), PartPose.offsetAndRotation(4.25f, -1.3f, 0.1f, 0, 0, -0.27f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(36, 57).mirror(false).addBox(-4.3f, 0, -0.4f, 8.6f, 6.2f, 0.8f), PartPose.offsetAndRotation(0, -1.3f, 2.75f, 0.16f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(13, 66).mirror(false).addBox(-0.6f, 0, -0.3f, 1.2f, 5.9f, 0.6f), PartPose.offsetAndRotation(-3.5f, -2, -2.5f, -0.04f, 0.2f, 0.12f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(13, 66).mirror(true).addBox(-0.6f, 0, -0.3f, 1.2f, 5.9f, 0.6f), PartPose.offsetAndRotation(3.5f, -2, -2.5f, -0.04f, -0.2f, -0.12f));
        PartDefinition rightLeg = waist.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(147, 0).mirror(false).addBox(-1.7f, -0.5f, -2.1f, 3.4f, 6.8f, 4.2f), PartPose.offsetAndRotation(-4.4f, -0.3f, 0, -1.121f, 0.732f, 0));
        PartDefinition leftLeg = waist.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(147, 0).mirror(true).addBox(-1.7f, -0.5f, -2.1f, 3.4f, 6.8f, 4.2f), PartPose.offsetAndRotation(4.4f, -0.3f, 0, -1.121f, -0.732f, 0));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(97, 46).mirror(false).addBox(-1.4f, 0, -1.75f, 2.8f, 4.6f, 3.5f)
                .texOffs(20, 66).mirror(false).addBox(-1.64f, 0.7f, -2.3f, 3.28f, 3.7f, 2.2f)
                .texOffs(69, 74).mirror(false).addBox(-1.56f, -0.8f, -2.65f, 3.12f, 2, 1)
                .texOffs(35, 66).mirror(false).addBox(-1.56f, 4.4f, -2.2f, 3.12f, 1.6f, 4.3f)
                .texOffs(9, 74).mirror(false).addBox(-1.16f, 4.9f, -3.3f, 2.32f, 1.1f, 1.3f)
                .texOffs(124, 74).mirror(false).addBox(-0.64f, 4.3f, -3.85f, 1.28f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 1.346f, -0.295f, -0.68f));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(97, 46).mirror(true).addBox(-1.4f, 0, -1.75f, 2.8f, 4.6f, 3.5f)
                .texOffs(20, 66).mirror(true).addBox(-1.64f, 0.7f, -2.3f, 3.28f, 3.7f, 2.2f)
                .texOffs(69, 74).mirror(true).addBox(-1.56f, -0.8f, -2.65f, 3.12f, 2, 1)
                .texOffs(35, 66).mirror(true).addBox(-1.56f, 4.4f, -2.2f, 3.12f, 1.6f, 4.3f)
                .texOffs(9, 74).mirror(true).addBox(-1.16f, 4.9f, -3.3f, 2.32f, 1.1f, 1.3f)
                .texOffs(124, 74).mirror(true).addBox(-0.64f, 4.3f, -3.85f, 1.28f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 1.346f, 0.295f, 0.68f));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-4, -9.6f, -2.6f, 8, 9.8f, 5.2f)
                .texOffs(84, 0).mirror(false).addBox(-4.6f, -10, -3.1f, 9.2f, 5.8f, 6.2f)
                .texOffs(183, 0).mirror(false).addBox(-4.2f, -4.5f, -2.85f, 8.4f, 4.4f, 5.7f)
                .texOffs(57, 57).mirror(false).addBox(-4.55f, -5, -3.25f, 9.1f, 0.8f, 6.5f)
                .texOffs(92, 57).mirror(false).addBox(-4.35f, -0.9f, -3, 8.7f, 1.2f, 6)
                .texOffs(203, 66).mirror(false).addBox(-1.9f, -4.1f, -3.35f, 3.8f, 3.2f, 0.7f)
                .texOffs(54, 66).mirror(false).addBox(-3, -11, -2.5f, 6, 1.4f, 5)
                .texOffs(77, 66).mirror(false).addBox(-3.3f, -10.5f, -2.9f, 6.6f, 1, 5.8f)
                .texOffs(214, 66).mirror(false).addBox(-1.3f, -11.8f, -1.3f, 2.6f, 1.4f, 2.6f)
                .texOffs(125, 66).mirror(false).addBox(-0.45f, -9.6f, -3.35f, 0.9f, 4.8f, 0.35f)
                .texOffs(80, 74).mirror(false).addBox(-0.9f, -10, -3.55f, 1.8f, 1.4f, 0.6f)
                .texOffs(227, 66).mirror(false).addBox(-3.95f, -9.3f, -3.55f, 3.1f, 3.1f, 0.5f)
                .texOffs(227, 66).mirror(false).addBox(0.85f, -9.3f, -3.55f, 3.1f, 3.1f, 0.5f), PartPose.offsetAndRotation(0, -2.7f, 0, 0, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(229, 0).mirror(false).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(-5.3f, -8.6f, 0, -0.15f, 0, 0.12f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(229, 0).mirror(true).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(5.3f, -8.6f, 0, -0.35f, 0, -0.08f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(130, 66).mirror(false).addBox(-2.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(123, 57).mirror(false).addBox(-3, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f)
                .texOffs(112, 46).mirror(false).addBox(-3.5f, 0.4f, -3.05f, 5, 1.4f, 6.1f)
                .texOffs(137, 46).mirror(false).addBox(-4, 1.6f, -3.4f, 5.4f, 1.6f, 6.8f)
                .texOffs(149, 66).mirror(false).addBox(-4.6f, 0.7f, -1.5f, 0.7f, 2.8f, 3), PartPose.offsetAndRotation(-0.3f, -1.6f, 0, 0, 0, 0.34f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(130, 66).mirror(true).addBox(-1.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(123, 57).mirror(true).addBox(-1.6f, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f)
                .texOffs(112, 46).mirror(true).addBox(-1.5f, 0.4f, -3.05f, 5, 1.4f, 6.1f)
                .texOffs(137, 46).mirror(true).addBox(-1.4f, 1.6f, -3.4f, 5.4f, 1.6f, 6.8f)
                .texOffs(149, 66).mirror(true).addBox(3.9f, 0.7f, -1.5f, 0.7f, 2.8f, 3), PartPose.offsetAndRotation(0.3f, -1.6f, 0, 0, 0, -0.34f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(146, 57).mirror(false).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(159, 57).mirror(false).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f)
                .texOffs(104, 66).mirror(false).addBox(-2.2f, -0.9f, -2.2f, 4.4f, 2, 4.4f), PartPose.offsetAndRotation(0, 3.8f, 0, -0.75f, -0.25f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(146, 57).mirror(true).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(159, 57).mirror(true).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f)
                .texOffs(104, 66).mirror(true).addBox(-2.2f, -0.9f, -2.2f, 4.4f, 2, 4.4f), PartPose.offsetAndRotation(0, 3.8f, 0, -0.9f, 0.2f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(158, 66).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(158, 66).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(34, 0).mirror(false).addBox(-3.3f, -7, -3.3f, 6.6f, 7, 6.6f)
                .texOffs(87, 74).mirror(false).addBox(-0.55f, -4.1f, -3.85f, 1.1f, 1.8f, 0.6f), PartPose.offsetAndRotation(0, -11.3f, -0.1f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(131, 74).mirror(false).addBox(-2.5f, -0.4f, -0.35f, 2.5f, 0.7f, 0.6f), PartPose.offsetAndRotation(-0.35f, -5.05f, -3.4f, 0, 0.1f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(131, 74).mirror(true).addBox(0, -0.4f, -0.35f, 2.5f, 0.7f, 0.6f), PartPose.offsetAndRotation(0.35f, -5.05f, -3.4f, 0, -0.1f, -0.2f));
        PartDefinition helm = head.addOrReplaceChild("helm", CubeListBuilder.create()
                .texOffs(0, 46).mirror(false).addBox(-3.65f, -1.25f, -3.65f, 7.3f, 1.5f, 7.3f)
                .texOffs(33, 46).mirror(false).addBox(-3.45f, -3.45f, -3.45f, 6.9f, 2.3f, 6.9f)
                .texOffs(176, 57).mirror(false).addBox(-2.55f, -4.6f, -2.55f, 5.1f, 1.25f, 5.1f)
                .texOffs(20, 74).mirror(false).addBox(-1.25f, -5.5f, -1.25f, 2.5f, 1, 2.5f)
                .texOffs(238, 66).mirror(false).addBox(-0.35f, -8.6f, -0.35f, 0.7f, 3.2f, 0.7f)
                .texOffs(94, 74).mirror(false).addBox(-0.6f, -9.3f, -0.6f, 1.2f, 1, 1.2f)
                .texOffs(103, 74).mirror(false).addBox(-3.85f, -0.5f, -4.35f, 7.7f, 0.7f, 1.1f), PartPose.offsetAndRotation(0, -5.6f, 0, 0, 0, 0));
        PartDefinition crest = helm.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(243, 66).mirror(false).addBox(-1.5f, -3.8f, -0.3f, 3, 3.8f, 0.5f), PartPose.offsetAndRotation(0, -1, -3.6f, -0.12f, 0, 0));
        PartDefinition rightFlap = helm.addOrReplaceChild("right_flap", CubeListBuilder.create()
                .texOffs(214, 0).mirror(false).addBox(-0.6f, 0, -2.9f, 0.7f, 4.8f, 5.6f), PartPose.offsetAndRotation(-3.55f, -0.3f, 0.3f, 0, 0, 0.16f));
        PartDefinition leftFlap = helm.addOrReplaceChild("left_flap", CubeListBuilder.create()
                .texOffs(214, 0).mirror(true).addBox(-0.1f, 0, -2.9f, 0.7f, 4.8f, 5.6f), PartPose.offsetAndRotation(3.55f, -0.3f, 0.3f, 0, 0, -0.16f));
        PartDefinition neckguard = helm.addOrReplaceChild("neckguard", CubeListBuilder.create()
                .texOffs(171, 66).mirror(false).addBox(-3.55f, 0, -0.4f, 7.1f, 4.6f, 0.8f), PartPose.offsetAndRotation(0, -0.2f, 3.25f, 0.28f, 0, 0));
        PartDefinition plume = helm.addOrReplaceChild("plume", CubeListBuilder.create()
                .texOffs(33, 74).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 1, 2.6f)
                .texOffs(201, 57).mirror(false).addBox(-2.5f, 0.8f, -2.5f, 5, 2.5f, 5), PartPose.offsetAndRotation(0, -7, 0, 0, 0, 0));
        PartDefinition plumeTail = plume.addOrReplaceChild("plume_tail", CubeListBuilder.create()
                .texOffs(164, 46).mirror(false).addBox(-2, 0, -1.1f, 4, 6, 2.2f), PartPose.offsetAndRotation(0, 1.6f, 1.8f, 0.6f, 0, 0));
        PartDefinition rightWing = helm.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(119, 0).mirror(false).addBox(-0.25f, -7.6f, -2, 0.5f, 8, 5), PartPose.offsetAndRotation(-3.6f, -1.6f, 0.4f, 0.3f, 0.35f, -0.6f));
        PartDefinition leftWing = helm.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(119, 0).mirror(true).addBox(-0.25f, -7.6f, -2, 0.5f, 8, 5), PartPose.offsetAndRotation(3.6f, -1.6f, 0.4f, 0.3f, -0.35f, 0.6f));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(179, 46).mirror(false).addBox(-5.2f, 0, -0.4f, 10.4f, 8, 0.8f), PartPose.offsetAndRotation(0, -10.4f, 3.3f, 0.12f, 0, 0));
        PartDefinition capeLower = cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(222, 57).mirror(false).addBox(-6.1f, 0, -0.4f, 12.2f, 6.5f, 0.8f), PartPose.offsetAndRotation(0, 7.7f, 0, 0.08f, 0, 0));
        PartDefinition plumeBig = helm.addOrReplaceChild("plume_big", CubeListBuilder.create()
                .texOffs(33, 74).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 1, 3)
                .texOffs(204, 46).mirror(false).addBox(-3, 0.8f, -3, 6, 2.8f, 6), PartPose.offsetAndRotation(0, -7.4f, 0, 0, 0, 0));
        PartDefinition plumeBigTail = plumeBig.addOrReplaceChild("plume_big_tail", CubeListBuilder.create()
                .texOffs(166, 0).mirror(false).addBox(-2.4f, 0, -1.2f, 4.8f, 8.5f, 2.4f), PartPose.offsetAndRotation(0, 2, 2.2f, 0.65f, 0, 0));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(140, 74).mirror(false).addBox(-2.2f, -0.35f, -0.3f, 2.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(-0.3f, -2.25f, -3.45f, 0, 0.15f, -0.35f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(140, 74).mirror(true).addBox(0, -0.35f, -0.3f, 2.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(0.3f, -2.25f, -3.45f, 0, -0.15f, 0.35f));
        PartDefinition lance = rightHand.addOrReplaceChild("lance", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.45f, -34, -0.45f, 0.9f, 44, 0.9f)
                .texOffs(46, 74).mirror(false).addBox(-0.8f, -35.4f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(229, 46).mirror(false).addBox(-0.95f, -42.6f, -0.3f, 1.9f, 7.4f, 0.6f)
                .texOffs(55, 74).mirror(false).addBox(-0.5f, 9.6f, -0.5f, 1, 2.4f, 1), PartPose.offsetAndRotation(0, 1.2f, 0, 1.5985f, 0.2766f, 0.0964f));
        PartDefinition lanceTassel = lance.addOrReplaceChild("lance_tassel", CubeListBuilder.create()
                .texOffs(190, 66).mirror(false).addBox(-1.1f, 0, -1.1f, 2.2f, 2.6f, 2.2f)
                .texOffs(60, 74).mirror(false).addBox(-0.8f, 2.3f, -0.8f, 1.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(0, -34.2f, 0, 0, 0, 0));
        PartDefinition pennant = lance.addOrReplaceChild("pennant", CubeListBuilder.create()
                .texOffs(63, 0).mirror(false).addBox(-0.1f, 0, 0, 0.2f, 5, 8.5f), PartPose.offsetAndRotation(0, -31.5f, 0.4f, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED cavalry: end
    }

    /** The vanilla horse coats and gold horse armour the cloud cavalry rides: a white coat, his captain's black. */
    static final ResourceLocation HORSE_WHITE = ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_white.png"),
            HORSE_BLACK = ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_black.png"),
            HORSE_GOLD_ARMOUR = ResourceLocation.withDefaultNamespace("textures/entity/horse/armor/horse_armor_gold.png");

    /**
     * The cloud cavalry: his rider (the host's skin, a captain's crimson) drawn by the model, and under him a
     * plain Minecraft horse (vanilla's model and coat) in vanilla's gold horse armour, on a small cloud.
     */
    public static class CavalryRenderer extends HostRenderer<CloudCavalryEntity> {
        public CavalryRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new CavalryModel(ctx.bakeLayer(CAVALRY_LAYER), ctx.bakeLayer(ModelLayers.HORSE), ctx.bakeLayer(ModelLayers.HORSE_ARMOR)),
                    1f, 0.9f, 0.85f, e -> e.isCaptain() ? "heaven_cavalry_captain" : "heaven_cavalry");
            addLayer(new RenderLayer<>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, CloudCavalryEntity e,
                                   float swing, float amount, float partial, float age, float yaw, float pitch) {
                    if (e.isInvisible()) return;
                    CavalryModel m = (CavalryModel) getParentModel();
                    int overlay = LivingEntityRenderer.getOverlayCoords(e, 0f);
                    m.renderHorse(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(e.isCaptain() ? HORSE_BLACK : HORSE_WHITE)), light, overlay);
                    m.renderArmour(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(HORSE_GOLD_ARMOUR)), light);
                }
            });
        }
    }

    /**
     * The cloud cavalry. The horse is vanilla's own (its model, saddle and reins), posed as vanilla poses a
     * horse: its legs swinging as it gallops, its head bobbing, its tail streaming; and winding up the
     * charge it stands on its hind legs as a vanilla horse rears ({@link CloudCavalryEntity#getStandAnim}),
     * forehooves pawing. Its gold armour is vanilla's armour model, posed the same. The rider sits its
     * saddle, borne with its body: he sways with it, leans into the gallop, the spear held slanting
     * forward, thrust on a blow; as it rears he leans in, the spear couched level.
     */
    public static class CavalryModel extends EntityModel<CloudCavalryEntity> {
        /** The rider's seat on the horse's back, in the horse body's frame (vanilla's saddle top, its middle). */
        private static final float SEAT_Y = -8.4f, SEAT_Z = -4.2f;
        private final ModelPart root, waist, body, head, rightArm, leftArm, rightForearm, lance, pennant;
        private final HorseRig horse, armour;
        private final List<ModelPart> captainOnly = new ArrayList<>();
        @Nullable
        private final ModelPart plumeTail, cape, capeLower, plume;
        private float stand;

        public CavalryModel(ModelPart root, ModelPart horseRoot, ModelPart armourRoot) {
            this.root = root;
            horse = new HorseRig(horseRoot);
            armour = new HorseRig(armourRoot);
            waist = root.getChild("rider").getChild("waist");
            body = waist.getChild("body");
            head = body.getChild("head");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            lance = rightForearm.getChild("right_hand").getChild("lance");
            pennant = lance.getChild("pennant");
            ModelPart helm = opt(head, "helm");
            plume = opt(helm, "plume");
            plumeTail = opt(plume, "plume_tail");
            cape = opt(body, "cape");
            capeLower = opt(cape, "cape_lower");
            for (ModelPart p : new ModelPart[]{opt(helm, "plume_big"), cape, opt(helm, "right_wing"), opt(helm, "left_wing"),
                    opt(head, "right_moustache"), opt(head, "left_moustache")}) {
                if (p != null) captainOnly.add(p);
            }
        }

        @Override
        public void prepareMobModel(CloudCavalryEntity e, float swing, float amount, float partial) {
            show(captainOnly, e.isCaptain());
            if (plume != null) plume.visible = !e.isCaptain();
            stand = e.getStandAnim(partial);
        }

        @Override
        public void setupAnim(CloudCavalryEntity e, float swing, float amount, float age, float yaw, float pitch) {
            horse.pose(swing, amount, age, yaw, pitch, stand);
            armour.pose(swing, amount, age, yaw, pitch, stand);
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float a = Math.min(1f, amount * 1.4f), w = swing * 0.6662f;
            // the rider: he sways with the horse, leans into the gallop; the spear thrust on a blow
            turn(waist, a * 0.15f + Mth.sin(w * 2) * 0.04f * a, 0, 0);
            body.y = body.getInitialPose().y + Mth.sin(age * 0.08f) * 0.1f;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.6f;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            turn(rightArm, -strike * 0.9f, 0, 0);
            turn(rightForearm, strike * 0.4f, 0, 0);
            turn(leftArm, Mth.sin(w) * 0.06f * a, 0, 0);
            turn(lance, strike * 0.5f, 0, 0);
            turn(pennant, 0, Mth.sin(age * 0.25f) * 0.25f + a * 0.2f, 0);
            turn(plumeTail, a * 0.5f + Mth.sin(age * 0.09f) * 0.06f, 0, 0);
            turn(cape, a * 0.7f + 0.1f, 0, 0);
            turn(capeLower, a * 0.3f + Mth.sin(age * 0.08f) * 0.05f, 0, 0);
            if (stand > 0) {
                // as the horse rears he leans in over its neck, the spear couched level
                toward(waist, 0.75f, 0, 0, stand);
                toward(rightArm, 0.25f, 0, 0.1f, stand);
                toward(lance, 0.55f, 0, 0, stand);
                head.xRot = Mth.lerp(stand, head.xRot, -0.6f);
            }
        }

        /** A part turned {@code t} of the way toward its first pose turned by (x, y, z). */
        private static void toward(ModelPart p, float x, float y, float z, float t) {
            PartPose o = p.getInitialPose();
            p.xRot = Mth.lerp(t, p.xRot, o.xRot + x);
            p.yRot = Mth.lerp(t, p.yRot, o.yRot + y);
            p.zRot = Mth.lerp(t, p.zRot, o.zRot + z);
        }

        /** Into the horse's frame: vanilla's horse scale, about the ground. */
        private static void horseFrame(PoseStack pose) {
            pose.translate(0, 1.501f, 0);
            pose.scale(HORSE_SIZE, HORSE_SIZE, HORSE_SIZE);
            pose.translate(0, -1.501f, 0);
        }

        void renderHorse(PoseStack pose, VertexConsumer vc, int light, int overlay) {
            pose.pushPose();
            horseFrame(pose);
            horse.root.render(pose, vc, light, overlay);
            pose.popPose();
        }

        void renderArmour(PoseStack pose, VertexConsumer vc, int light) {
            pose.pushPose();
            horseFrame(pose);
            armour.root.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }

        /** The rider (and, with the glowing skin, what of him shines): on the saddle, borne with the horse's body. */
        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            pose.pushPose();
            horseFrame(pose);
            horse.body.translateAndRotate(pose);
            pose.translate(0, SEAT_Y / 16f, SEAT_Z / 16f);
            // he keeps more upright than the horse's back as it rears
            pose.mulPose(Axis.XP.rotation(-horse.body.xRot * 0.45f));
            float s = RIDER_SIZE / HORSE_SIZE;
            pose.scale(s, s, s);
            root.render(pose, vc, light, overlay, color);
            pose.popPose();
        }
    }

    /**
     * Vanilla's horse model (or its armour), baked from vanilla's own layer and posed by the same rules as
     * {@link net.minecraft.client.model.HorseModel}'s, which wants an {@code AbstractHorse}: saddled and
     * ridden (its saddle, bit and reins are on its own skin), the legs swinging with its stride, the head
     * bobbing, the tail lifting; rearing ({@code stand} 0..1) its body tilts up on the hind legs, forelegs
     * pawing, head raised.
     */
    static final class HorseRig {
        final ModelPart root, body, headParts, tail, rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg;

        HorseRig(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            headParts = root.getChild("head_parts");
            tail = body.getChild("tail");
            rightHindLeg = root.getChild("right_hind_leg");
            leftHindLeg = root.getChild("left_hind_leg");
            rightFrontLeg = root.getChild("right_front_leg");
            leftFrontLeg = root.getChild("left_front_leg");
            for (String baby : new String[]{"right_hind_baby_leg", "left_hind_baby_leg", "right_front_baby_leg", "left_front_baby_leg"}) {
                root.getChild(baby).visible = false;
            }
        }

        void pose(float swing, float amount, float age, float yaw, float pitch, float stand) {
            float headYaw = Mth.clamp(yaw, -20f, 20f), headPitch = pitch * Mth.DEG_TO_RAD;
            if (amount > 0.2f) headPitch += Mth.cos(swing * 0.8f) * 0.15f * amount;
            float still = 1f - stand;
            float stride = Mth.cos(swing * 0.6662f + Mth.PI);
            float fore = stride * 0.8f * amount;
            headParts.xRot = stand * (Mth.PI / 12 + headPitch) + still * (Mth.PI / 6 + headPitch);
            headParts.yRot = headYaw * Mth.DEG_TO_RAD;
            headParts.y = stand * -4f + still * 4f;
            headParts.z = stand * -4f + still * -12f;
            body.xRot = stand * (-Mth.PI / 4);
            body.y = 11f;
            float paw = Mth.cos(age * 0.6f + Mth.PI);
            leftFrontLeg.y = rightFrontLeg.y = 2f * stand + 14f * still;
            leftFrontLeg.z = rightFrontLeg.z = -6f * stand - 10f * still;
            leftHindLeg.xRot = Mth.PI / 12 * stand - stride * 0.5f * amount * still;
            rightHindLeg.xRot = Mth.PI / 12 * stand + stride * 0.5f * amount * still;
            leftFrontLeg.xRot = (-Mth.PI / 3 + paw) * stand + fore * still;
            rightFrontLeg.xRot = (-Mth.PI / 3 - paw) * stand - fore * still;
            tail.xRot = Mth.PI / 6 + amount * 0.75f;
            tail.y = -5f + amount;
            tail.z = 2f + amount * 2f;
            // at rest it swishes its tail now and then
            tail.yRot = amount < 0.1f ? Mth.sin(age * 0.07f) * Math.max(0, Mth.sin(age * 0.013f)) * 0.5f : 0;
        }
    }
}
