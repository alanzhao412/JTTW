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
 * (a white celestial steed in gilt barding treading cloud, its rider's spear couched as it rears) and the
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
    /** How much larger than its pixels the cloud cavalry is drawn (heaven_host_cavalry.py prints its reach). */
    public static final float CAVALRY_SIZE = 1.05f;

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
        event.registerEntityRenderer(HeavenRegistry.CLOUD_CAVALRY.get(), ctx -> new HostRenderer<CloudCavalryEntity>(ctx,
                new CavalryModel(ctx.bakeLayer(CAVALRY_LAYER)), CAVALRY_SIZE, 0.9f, 0.85f, e -> e.isCaptain() ? "heaven_cavalry_captain" : "heaven_cavalry"));
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
     * The cloud cavalry (256x256, steed and rider one model): a white celestial horse in gilt barding, a
     * silver mane and tail, gold hooves trailing wisps of cloud; the rider in the heavenly soldier's armour,
     * a long red-tasselled spear with a pennant. Model and skins: {@code assets_source/heaven_host_cavalry.py}.
     */
    public static LayerDefinition cavalryLayer() {
        // GENERATED cavalry: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition horse = root.addOrReplaceChild("horse", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-4.2f, -4.5f, -8.4f, 8.4f, 9, 16.8f)
                .texOffs(113, 46).mirror(false).addBox(-3.8f, -4.1f, -11, 7.6f, 8.2f, 3)
                .texOffs(190, 0).mirror(false).addBox(-4, -4.9f, 6, 8, 8.4f, 4.4f)
                .texOffs(149, 0).mirror(false).addBox(-3.4f, 4.1f, -7.5f, 6.8f, 1.4f, 13)
                .texOffs(72, 83).mirror(false).addBox(-4.5f, -2.6f, -11.7f, 9, 5.2f, 1.2f)
                .texOffs(81, 0).mirror(false).addBox(-4.75f, -4.75f, -4.6f, 9.5f, 6.6f, 8.6f)
                .texOffs(0, 73).mirror(false).addBox(-3, -6, -3.8f, 6, 1.5f, 6.4f)
                .texOffs(9, 109).mirror(false).addBox(-1.8f, -7.6f, -4.2f, 3.6f, 1.8f, 1)
                .texOffs(20, 109).mirror(false).addBox(-2.4f, -7.8f, 2.4f, 4.8f, 2, 1)
                .texOffs(136, 46).mirror(false).addBox(-4.6f, -5.3f, 4.6f, 9.2f, 4.6f, 6.2f), PartPose.offsetAndRotation(0, 8, 0, 0, 0, 0));
        PartDefinition peytralFringe = horse.addOrReplaceChild("peytral_fringe", CubeListBuilder.create()
                .texOffs(173, 102).mirror(false).addBox(-4.2f, 0, -0.35f, 8.4f, 2.6f, 0.7f), PartPose.offsetAndRotation(0, 2.5f, -11.6f, 0.1f, 0, 0));
        PartDefinition rightCrupperFringe = horse.addOrReplaceChild("right_crupper_fringe", CubeListBuilder.create()
                .texOffs(27, 73).mirror(false).addBox(-0.35f, 0, -3, 0.7f, 3, 6), PartPose.offsetAndRotation(-4.65f, -0.8f, 7.6f, 0, 0, 0.08f));
        PartDefinition leftCrupperFringe = horse.addOrReplaceChild("left_crupper_fringe", CubeListBuilder.create()
                .texOffs(27, 73).mirror(true).addBox(-0.35f, 0, -3, 0.7f, 3, 6), PartPose.offsetAndRotation(4.65f, -0.8f, 7.6f, 0, 0, -0.08f));
        PartDefinition rightClothFringe = horse.addOrReplaceChild("right_cloth_fringe", CubeListBuilder.create()
                .texOffs(171, 46).mirror(false).addBox(-0.3f, 0, -4.2f, 0.6f, 2.4f, 8.4f), PartPose.offsetAndRotation(-4.8f, 1.8f, -0.3f, 0, 0, 0.06f));
        PartDefinition leftClothFringe = horse.addOrReplaceChild("left_cloth_fringe", CubeListBuilder.create()
                .texOffs(171, 46).mirror(true).addBox(-0.3f, 0, -4.2f, 0.6f, 2.4f, 8.4f), PartPose.offsetAndRotation(4.8f, 1.8f, -0.3f, 0, 0, -0.06f));
        PartDefinition neck = horse.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(58, 0).mirror(false).addBox(-2.3f, -12.4f, -2.7f, 4.6f, 13.4f, 5.4f)
                .texOffs(42, 73).mirror(false).addBox(-2.6f, -1.4f, -3, 5.2f, 2.2f, 6), PartPose.offsetAndRotation(0, -2.4f, -9, 0.62f, 0, 0));
        PartDefinition horseHead = neck.addOrReplaceChild("horse_head", CubeListBuilder.create()
                .texOffs(0, 61).mirror(false).addBox(-2.4f, -2.9f, -3.2f, 4.8f, 5, 5.4f)
                .texOffs(21, 46).mirror(false).addBox(-1.9f, -2.3f, -11, 3.8f, 4.2f, 8)
                .texOffs(105, 93).mirror(false).addBox(-2.15f, -1.7f, -13.2f, 4.3f, 3.7f, 2.4f)
                .texOffs(99, 61).mirror(false).addBox(-1.75f, 1.6f, -10.6f, 3.5f, 1.6f, 8)
                .texOffs(67, 73).mirror(false).addBox(-1.55f, -2.85f, -10.8f, 3.1f, 0.7f, 7.6f)
                .texOffs(73, 102).mirror(false).addBox(-0.7f, -5.4f, -2.4f, 1.4f, 2.8f, 1.4f), PartPose.offsetAndRotation(0, -11.8f, -0.6f, 0.95f, 0, 0));
        PartDefinition rightEar = horseHead.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(82, 102).mirror(false).addBox(-0.5f, -2.6f, -0.6f, 1, 2.8f, 1.2f), PartPose.offsetAndRotation(-1.5f, -2.6f, -0.2f, 0.2f, 0, -0.25f));
        PartDefinition leftEar = horseHead.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(82, 102).mirror(true).addBox(-0.5f, -2.6f, -0.6f, 1, 2.8f, 1.2f), PartPose.offsetAndRotation(1.5f, -2.6f, -0.2f, 0.2f, 0, 0.25f));
        PartDefinition forelock = horseHead.addOrReplaceChild("forelock", CubeListBuilder.create()
                .texOffs(89, 102).mirror(false).addBox(-1.2f, 0, -0.5f, 2.4f, 3.6f, 1), PartPose.offsetAndRotation(0, -2.9f, -2.6f, -1, 0, 0));
        PartDefinition tail = horse.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(92, 73).mirror(false).addBox(-1.2f, -0.4f, -1.2f, 2.4f, 6, 2.4f), PartPose.offsetAndRotation(0, -3.6f, 10.2f, 0.45f, 0, 0));
        PartDefinition tailMid = tail.addOrReplaceChild("tail_mid", CubeListBuilder.create()
                .texOffs(23, 61).mirror(false).addBox(-1.8f, 0, -1.5f, 3.6f, 7.5f, 3), PartPose.offsetAndRotation(0, 5.4f, 0, -0.1f, 0, 0));
        PartDefinition tailEnd = tailMid.addOrReplaceChild("tail_end", CubeListBuilder.create()
                .texOffs(124, 61).mirror(false).addBox(-1.5f, 0, -1.2f, 3, 6.5f, 2.4f), PartPose.offsetAndRotation(0, 7.1f, 0, -0.08f, 0, 0));
        PartDefinition mane0 = neck.addOrReplaceChild("mane_0", CubeListBuilder.create()
                .texOffs(137, 61).mirror(false).addBox(-1.2f, 0, -1.1f, 2.4f, 7, 2.6f), PartPose.offsetAndRotation(1, -12.2f, 1.8f, 0.3f, 0, -0.4f));
        PartDefinition mane1 = neck.addOrReplaceChild("mane_1", CubeListBuilder.create()
                .texOffs(150, 61).mirror(false).addBox(-1.2f, 0, -1.1f, 2.4f, 6.7f, 2.6f), PartPose.offsetAndRotation(-1, -10.3f, 1.8f, 0.3f, 0, 0.4f));
        PartDefinition mane2 = neck.addOrReplaceChild("mane_2", CubeListBuilder.create()
                .texOffs(137, 61).mirror(false).addBox(-1.2f, 0, -1.1f, 2.4f, 6.4f, 2.6f), PartPose.offsetAndRotation(1, -8.4f, 1.8f, 0.3f, 0, -0.4f));
        PartDefinition mane3 = neck.addOrReplaceChild("mane_3", CubeListBuilder.create()
                .texOffs(150, 61).mirror(false).addBox(-1.2f, 0, -1.1f, 2.4f, 6.1f, 2.6f), PartPose.offsetAndRotation(-1, -6.5f, 1.8f, 0.3f, 0, 0.4f));
        PartDefinition mane4 = neck.addOrReplaceChild("mane_4", CubeListBuilder.create()
                .texOffs(105, 73).mirror(false).addBox(-1.2f, 0, -1.1f, 2.4f, 5.8f, 2.6f), PartPose.offsetAndRotation(1, -4.6f, 1.8f, 0.3f, 0, -0.4f));
        PartDefinition mane5 = neck.addOrReplaceChild("mane_5", CubeListBuilder.create()
                .texOffs(118, 73).mirror(false).addBox(-1.2f, 0, -1.1f, 2.4f, 5.5f, 2.6f), PartPose.offsetAndRotation(-1, -2.7f, 1.8f, 0.3f, 0, 0.4f));
        PartDefinition mane6 = neck.addOrReplaceChild("mane_6", CubeListBuilder.create()
                .texOffs(105, 73).mirror(false).addBox(-1.2f, 0, -1.1f, 2.4f, 5.2f, 2.6f), PartPose.offsetAndRotation(1, -0.8f, 1.8f, 0.3f, 0, -0.4f));
        PartDefinition rightForeleg = horse.addOrReplaceChild("right_foreleg", CubeListBuilder.create()
                .texOffs(38, 61).mirror(false).addBox(-1.4f, -1, -1.7f, 2.8f, 7, 3.4f), PartPose.offsetAndRotation(-2.4f, 3.2f, -7.4f, 0, 0, 0));
        PartDefinition leftForeleg = horse.addOrReplaceChild("left_foreleg", CubeListBuilder.create()
                .texOffs(38, 61).mirror(true).addBox(-1.4f, -1, -1.7f, 2.8f, 7, 3.4f), PartPose.offsetAndRotation(2.4f, 3.2f, -7.4f, 0, 0, 0));
        PartDefinition rightForeshin = rightForeleg.addOrReplaceChild("right_foreshin", CubeListBuilder.create()
                .texOffs(95, 83).mirror(false).addBox(-0.95f, 0, -1.05f, 1.9f, 4.6f, 2.1f)
                .texOffs(98, 102).mirror(false).addBox(-1.15f, 4.2f, -1.25f, 2.3f, 1.2f, 2.5f)
                .texOffs(111, 102).mirror(false).addBox(-1.25f, 5.2f, -1.45f, 2.5f, 1.4f, 2.8f)
                .texOffs(131, 73).mirror(false).addBox(-1.9f, 3.3f, -0.4f, 3.8f, 2.6f, 5.6f), PartPose.offsetAndRotation(0, 5.8f, 0, 0, 0, 0));
        PartDefinition leftForeshin = leftForeleg.addOrReplaceChild("left_foreshin", CubeListBuilder.create()
                .texOffs(95, 83).mirror(true).addBox(-0.95f, 0, -1.05f, 1.9f, 4.6f, 2.1f)
                .texOffs(98, 102).mirror(true).addBox(-1.15f, 4.2f, -1.25f, 2.3f, 1.2f, 2.5f)
                .texOffs(111, 102).mirror(true).addBox(-1.25f, 5.2f, -1.45f, 2.5f, 1.4f, 2.8f)
                .texOffs(131, 73).mirror(true).addBox(-1.9f, 3.3f, -0.4f, 3.8f, 2.6f, 5.6f), PartPose.offsetAndRotation(0, 5.8f, 0, 0, 0, 0));
        PartDefinition rightHindleg = horse.addOrReplaceChild("right_hindleg", CubeListBuilder.create()
                .texOffs(46, 46).mirror(false).addBox(-1.6f, -1.6f, -2.1f, 3.2f, 7.6f, 4.4f), PartPose.offsetAndRotation(-2.4f, 2.4f, 7.2f, 0, 0, 0));
        PartDefinition leftHindleg = horse.addOrReplaceChild("left_hindleg", CubeListBuilder.create()
                .texOffs(46, 46).mirror(true).addBox(-1.6f, -1.6f, -2.1f, 3.2f, 7.6f, 4.4f), PartPose.offsetAndRotation(2.4f, 2.4f, 7.2f, 0, 0, 0));
        PartDefinition rightHindshin = rightHindleg.addOrReplaceChild("right_hindshin", CubeListBuilder.create()
                .texOffs(95, 83).mirror(false).addBox(-0.95f, 0, -1.05f, 1.9f, 4.8f, 2.1f)
                .texOffs(98, 102).mirror(false).addBox(-1.15f, 4.4f, -1.25f, 2.3f, 1.2f, 2.5f)
                .texOffs(111, 102).mirror(false).addBox(-1.25f, 5.4f, -1.45f, 2.5f, 1.4f, 2.8f)
                .texOffs(131, 73).mirror(false).addBox(-1.9f, 3.5f, -0.4f, 3.8f, 2.6f, 5.6f), PartPose.offsetAndRotation(0, 5.6f, 0.6f, 0, 0, 0));
        PartDefinition leftHindshin = leftHindleg.addOrReplaceChild("left_hindshin", CubeListBuilder.create()
                .texOffs(95, 83).mirror(true).addBox(-0.95f, 0, -1.05f, 1.9f, 4.8f, 2.1f)
                .texOffs(98, 102).mirror(true).addBox(-1.15f, 4.4f, -1.25f, 2.3f, 1.2f, 2.5f)
                .texOffs(111, 102).mirror(true).addBox(-1.25f, 5.4f, -1.45f, 2.5f, 1.4f, 2.8f)
                .texOffs(131, 73).mirror(true).addBox(-1.9f, 3.5f, -0.4f, 3.8f, 2.6f, 5.6f), PartPose.offsetAndRotation(0, 5.6f, 0.6f, 0, 0, 0));
        PartDefinition rider = horse.addOrReplaceChild("rider", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -6.2f, -0.8f, 0, 0, 0));
        PartDefinition waist = rider.addOrReplaceChild("waist", CubeListBuilder.create()
                .texOffs(106, 83).mirror(false).addBox(-4, -1.5f, -2.5f, 8, 3, 5)
                .texOffs(152, 73).mirror(false).addBox(-4.7f, -2.9f, -3.1f, 9.4f, 1.9f, 6.2f)
                .texOffs(194, 102).mirror(false).addBox(-1.4f, -3.15f, -3.55f, 2.8f, 2.3f, 0.6f), PartPose.offsetAndRotation(0, -1.6f, 0, 0, 0, 0));
        PartDefinition apron = waist.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(133, 83).mirror(false).addBox(-1.4f, 0, -0.3f, 2.8f, 6.5f, 0.6f), PartPose.offsetAndRotation(0, -1.3f, -3.25f, -0.05f, 0, 0));
        PartDefinition rightTasset = waist.addOrReplaceChild("right_tasset", CubeListBuilder.create()
                .texOffs(122, 93).mirror(false).addBox(-2.4f, 0, -0.4f, 4.8f, 6, 0.8f), PartPose.offsetAndRotation(-2.3f, -1.3f, -2.85f, -0.14f, 0.14f, 0.13f));
        PartDefinition leftTasset = waist.addOrReplaceChild("left_tasset", CubeListBuilder.create()
                .texOffs(122, 93).mirror(true).addBox(-2.4f, 0, -0.4f, 4.8f, 6, 0.8f), PartPose.offsetAndRotation(2.3f, -1.3f, -2.85f, -0.14f, -0.14f, -0.13f));
        PartDefinition rightSideTasset = waist.addOrReplaceChild("right_side_tasset", CubeListBuilder.create()
                .texOffs(192, 46).mirror(false).addBox(-0.8f, 0, -2.6f, 0.8f, 5.7f, 5.2f), PartPose.offsetAndRotation(-4.25f, -1.3f, 0.1f, 0, 0, 0.27f));
        PartDefinition leftSideTasset = waist.addOrReplaceChild("left_side_tasset", CubeListBuilder.create()
                .texOffs(192, 46).mirror(true).addBox(0, 0, -2.6f, 0.8f, 5.7f, 5.2f), PartPose.offsetAndRotation(4.25f, -1.3f, 0.1f, 0, 0, -0.27f));
        PartDefinition backTasset = waist.addOrReplaceChild("back_tasset", CubeListBuilder.create()
                .texOffs(142, 83).mirror(false).addBox(-4.3f, 0, -0.4f, 8.6f, 6.2f, 0.8f), PartPose.offsetAndRotation(0, -1.3f, 2.75f, 0.16f, 0, 0));
        PartDefinition rightSash = waist.addOrReplaceChild("right_sash", CubeListBuilder.create()
                .texOffs(135, 93).mirror(false).addBox(-0.6f, 0, -0.3f, 1.2f, 5.9f, 0.6f), PartPose.offsetAndRotation(-3.5f, -2, -2.5f, -0.04f, 0.2f, 0.12f));
        PartDefinition leftSash = waist.addOrReplaceChild("left_sash", CubeListBuilder.create()
                .texOffs(135, 93).mirror(true).addBox(-0.6f, 0, -0.3f, 1.2f, 5.9f, 0.6f), PartPose.offsetAndRotation(3.5f, -2, -2.5f, -0.04f, -0.2f, -0.12f));
        PartDefinition rightLeg = waist.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(207, 46).mirror(false).addBox(-2, -0.5f, -2.1f, 4, 6.8f, 4.2f), PartPose.offsetAndRotation(-2.4f, 0.2f, 0, -1.25f, 0, 0.42f));
        PartDefinition leftLeg = waist.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(207, 46).mirror(true).addBox(-2, -0.5f, -2.1f, 4, 6.8f, 4.2f), PartPose.offsetAndRotation(2.4f, 0.2f, 0, -1.25f, 0, -0.42f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(187, 73).mirror(false).addBox(-1.75f, 0, -1.75f, 3.5f, 4.6f, 3.5f)
                .texOffs(142, 93).mirror(false).addBox(-2.05f, 0.7f, -2.3f, 4.1f, 3.7f, 2.2f)
                .texOffs(33, 109).mirror(false).addBox(-1.95f, -0.8f, -2.65f, 3.9f, 2, 1)
                .texOffs(159, 93).mirror(false).addBox(-1.95f, 4.4f, -2.2f, 3.9f, 1.6f, 4.3f)
                .texOffs(203, 102).mirror(false).addBox(-1.45f, 4.9f, -3.3f, 2.9f, 1.1f, 1.3f)
                .texOffs(88, 109).mirror(false).addBox(-0.8f, 4.3f, -3.85f, 1.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 1.35f, 0, -0.25f));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(187, 73).mirror(true).addBox(-1.75f, 0, -1.75f, 3.5f, 4.6f, 3.5f)
                .texOffs(142, 93).mirror(true).addBox(-2.05f, 0.7f, -2.3f, 4.1f, 3.7f, 2.2f)
                .texOffs(33, 109).mirror(true).addBox(-1.95f, -0.8f, -2.65f, 3.9f, 2, 1)
                .texOffs(159, 93).mirror(true).addBox(-1.95f, 4.4f, -2.2f, 3.9f, 1.6f, 4.3f)
                .texOffs(203, 102).mirror(true).addBox(-1.45f, 4.9f, -3.3f, 2.9f, 1.1f, 1.3f)
                .texOffs(88, 109).mirror(true).addBox(-0.8f, 4.3f, -3.85f, 1.6f, 0.9f, 0.7f), PartPose.offsetAndRotation(0, 6, 0, 1.35f, 0, 0.25f));
        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(120, 0).mirror(false).addBox(-4, -9.6f, -2.6f, 8, 9.8f, 5.2f)
                .texOffs(65, 46).mirror(false).addBox(-4.6f, -10, -3.1f, 9.2f, 5.8f, 6.2f)
                .texOffs(53, 61).mirror(false).addBox(-4.2f, -4.5f, -2.85f, 8.4f, 4.4f, 5.7f)
                .texOffs(163, 83).mirror(false).addBox(-4.55f, -5, -3.25f, 9.1f, 0.8f, 6.5f)
                .texOffs(198, 83).mirror(false).addBox(-4.35f, -0.9f, -3, 8.7f, 1.2f, 6)
                .texOffs(124, 102).mirror(false).addBox(-1.9f, -4.1f, -3.35f, 3.8f, 3.2f, 0.7f)
                .texOffs(178, 93).mirror(false).addBox(-3, -11, -2.5f, 6, 1.4f, 5)
                .texOffs(201, 93).mirror(false).addBox(-3.3f, -10.5f, -2.9f, 6.6f, 1, 5.8f)
                .texOffs(135, 102).mirror(false).addBox(-1.3f, -11.8f, -1.3f, 2.6f, 1.4f, 2.6f)
                .texOffs(249, 93).mirror(false).addBox(-0.45f, -9.6f, -3.35f, 0.9f, 4.8f, 0.35f)
                .texOffs(44, 109).mirror(false).addBox(-0.9f, -10, -3.55f, 1.8f, 1.4f, 0.6f)
                .texOffs(148, 102).mirror(false).addBox(-3.95f, -9.3f, -3.55f, 3.1f, 3.1f, 0.5f)
                .texOffs(148, 102).mirror(false).addBox(0.85f, -9.3f, -3.55f, 3.1f, 3.1f, 0.5f), PartPose.offsetAndRotation(0, -2.7f, 0, 0, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(163, 61).mirror(false).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(-5.3f, -8.6f, 0, -0.15f, 0, 0.12f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(163, 61).mirror(true).addBox(-1.8f, -1.2f, -1.9f, 3.6f, 5.2f, 3.8f), PartPose.offsetAndRotation(5.3f, -8.6f, 0, -0.35f, 0, -0.08f));
        PartDefinition rightPauldron = rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(0, 102).mirror(false).addBox(-2.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(229, 83).mirror(false).addBox(-3, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f)
                .texOffs(204, 73).mirror(false).addBox(-3.5f, 0.4f, -3.05f, 5, 1.4f, 6.1f)
                .texOffs(229, 73).mirror(false).addBox(-4, 1.6f, -3.4f, 5.4f, 1.6f, 6.8f)
                .texOffs(19, 102).mirror(false).addBox(-4.6f, 0.7f, -1.5f, 0.7f, 2.8f, 3), PartPose.offsetAndRotation(-0.3f, -1.6f, 0, 0, 0, 0.34f));
        PartDefinition leftPauldron = leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create()
                .texOffs(0, 102).mirror(true).addBox(-1.4f, -1.5f, -2.3f, 3.8f, 1, 4.6f)
                .texOffs(229, 83).mirror(true).addBox(-1.6f, -0.8f, -2.75f, 4.6f, 1.4f, 5.5f)
                .texOffs(204, 73).mirror(true).addBox(-1.5f, 0.4f, -3.05f, 5, 1.4f, 6.1f)
                .texOffs(229, 73).mirror(true).addBox(-1.4f, 1.6f, -3.4f, 5.4f, 1.6f, 6.8f)
                .texOffs(19, 102).mirror(true).addBox(3.9f, 0.7f, -1.5f, 0.7f, 2.8f, 3), PartPose.offsetAndRotation(0.3f, -1.6f, 0, 0, 0, -0.34f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(0, 93).mirror(false).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(13, 93).mirror(false).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f)
                .texOffs(228, 93).mirror(false).addBox(-2.2f, -0.9f, -2.2f, 4.4f, 2, 4.4f), PartPose.offsetAndRotation(0, 3.8f, 0, -0.75f, -0.25f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(0, 93).mirror(true).addBox(-1.4f, 0, -1.4f, 2.8f, 4.3f, 2.8f)
                .texOffs(13, 93).mirror(true).addBox(-1.75f, 0.9f, -1.75f, 3.5f, 3.2f, 3.5f)
                .texOffs(228, 93).mirror(true).addBox(-2.2f, -0.9f, -2.2f, 4.4f, 2, 4.4f), PartPose.offsetAndRotation(0, 3.8f, 0, -0.9f, 0.2f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(28, 102).mirror(false).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(28, 102).mirror(true).addBox(-1.2f, 0, -1.2f, 2.4f, 2.4f, 2.4f), PartPose.offsetAndRotation(0, 4.2f, 0, 0, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(217, 0).mirror(false).addBox(-3.3f, -7, -3.3f, 6.6f, 7, 6.6f)
                .texOffs(51, 109).mirror(false).addBox(-0.55f, -4.1f, -3.85f, 1.1f, 1.8f, 0.6f), PartPose.offsetAndRotation(0, -11.3f, -0.1f, 0, 0, 0));
        PartDefinition rightBrow = head.addOrReplaceChild("right_brow", CubeListBuilder.create()
                .texOffs(95, 109).mirror(false).addBox(-2.5f, -0.4f, -0.35f, 2.5f, 0.7f, 0.6f), PartPose.offsetAndRotation(-0.35f, -5.05f, -3.4f, 0, 0.1f, 0.2f));
        PartDefinition leftBrow = head.addOrReplaceChild("left_brow", CubeListBuilder.create()
                .texOffs(95, 109).mirror(true).addBox(0, -0.4f, -0.35f, 2.5f, 0.7f, 0.6f), PartPose.offsetAndRotation(0.35f, -5.05f, -3.4f, 0, -0.1f, -0.2f));
        PartDefinition helm = head.addOrReplaceChild("helm", CubeListBuilder.create()
                .texOffs(180, 61).mirror(false).addBox(-3.65f, -1.25f, -3.65f, 7.3f, 1.5f, 7.3f)
                .texOffs(213, 61).mirror(false).addBox(-3.45f, -3.45f, -3.45f, 6.9f, 2.3f, 6.9f)
                .texOffs(30, 93).mirror(false).addBox(-2.55f, -4.6f, -2.55f, 5.1f, 1.25f, 5.1f)
                .texOffs(214, 102).mirror(false).addBox(-1.25f, -5.5f, -1.25f, 2.5f, 1, 2.5f)
                .texOffs(159, 102).mirror(false).addBox(-0.35f, -8.6f, -0.35f, 0.7f, 3.2f, 0.7f)
                .texOffs(58, 109).mirror(false).addBox(-0.6f, -9.3f, -0.6f, 1.2f, 1, 1.2f)
                .texOffs(67, 109).mirror(false).addBox(-3.85f, -0.5f, -4.35f, 7.7f, 0.7f, 1.1f), PartPose.offsetAndRotation(0, -5.6f, 0, 0, 0, 0));
        PartDefinition crest = helm.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(164, 102).mirror(false).addBox(-1.5f, -3.8f, -0.3f, 3, 3.8f, 0.5f), PartPose.offsetAndRotation(0, -1, -3.6f, -0.12f, 0, 0));
        PartDefinition rightFlap = helm.addOrReplaceChild("right_flap", CubeListBuilder.create()
                .texOffs(84, 61).mirror(false).addBox(-0.6f, 0, -2.9f, 0.7f, 4.8f, 5.6f), PartPose.offsetAndRotation(-3.55f, -0.3f, 0.3f, 0, 0, 0.16f));
        PartDefinition leftFlap = helm.addOrReplaceChild("left_flap", CubeListBuilder.create()
                .texOffs(84, 61).mirror(true).addBox(-0.1f, 0, -2.9f, 0.7f, 4.8f, 5.6f), PartPose.offsetAndRotation(3.55f, -0.3f, 0.3f, 0, 0, -0.16f));
        PartDefinition neckguard = helm.addOrReplaceChild("neckguard", CubeListBuilder.create()
                .texOffs(41, 102).mirror(false).addBox(-3.55f, 0, -0.4f, 7.1f, 4.6f, 0.8f), PartPose.offsetAndRotation(0, -0.2f, 3.25f, 0.28f, 0, 0));
        PartDefinition plume = helm.addOrReplaceChild("plume", CubeListBuilder.create()
                .texOffs(227, 102).mirror(false).addBox(-1.3f, 0, -1.3f, 2.6f, 1, 2.6f)
                .texOffs(55, 93).mirror(false).addBox(-2.5f, 0.8f, -2.5f, 5, 2.5f, 5), PartPose.offsetAndRotation(0, -7, 0, 0, 0, 0));
        PartDefinition plumeTail = plume.addOrReplaceChild("plume_tail", CubeListBuilder.create()
                .texOffs(0, 83).mirror(false).addBox(-2, 0, -1.1f, 4, 6, 2.2f), PartPose.offsetAndRotation(0, 1.6f, 1.8f, 0.6f, 0, 0));
        PartDefinition rightWing = helm.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(100, 46).mirror(false).addBox(-0.25f, -7.6f, -2, 0.5f, 8, 5), PartPose.offsetAndRotation(-3.6f, -1.6f, 0.4f, 0.3f, 0.35f, -0.6f));
        PartDefinition leftWing = helm.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(100, 46).mirror(true).addBox(-0.25f, -7.6f, -2, 0.5f, 8, 5), PartPose.offsetAndRotation(3.6f, -1.6f, 0.4f, 0.3f, -0.35f, 0.6f));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
                .texOffs(15, 83).mirror(false).addBox(-5.2f, 0, -0.4f, 10.4f, 8, 0.8f), PartPose.offsetAndRotation(0, -10.4f, 3.3f, 0.12f, 0, 0));
        PartDefinition capeLower = cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
                .texOffs(76, 93).mirror(false).addBox(-6.1f, 0, -0.4f, 12.2f, 6.5f, 0.8f), PartPose.offsetAndRotation(0, 7.7f, 0, 0.08f, 0, 0));
        PartDefinition plumeBig = helm.addOrReplaceChild("plume_big", CubeListBuilder.create()
                .texOffs(227, 102).mirror(false).addBox(-1.5f, 0, -1.5f, 3, 1, 3)
                .texOffs(40, 83).mirror(false).addBox(-3, 0.8f, -3, 6, 2.8f, 6), PartPose.offsetAndRotation(0, -7.4f, 0, 0, 0, 0));
        PartDefinition plumeBigTail = plumeBig.addOrReplaceChild("plume_big_tail", CubeListBuilder.create()
                .texOffs(226, 46).mirror(false).addBox(-2.4f, 0, -1.2f, 4.8f, 8.5f, 2.4f), PartPose.offsetAndRotation(0, 2, 2.2f, 0.65f, 0, 0));
        PartDefinition rightMoustache = head.addOrReplaceChild("right_moustache", CubeListBuilder.create()
                .texOffs(104, 109).mirror(false).addBox(-2.2f, -0.35f, -0.3f, 2.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(-0.3f, -2.25f, -3.45f, 0, 0.15f, -0.35f));
        PartDefinition leftMoustache = head.addOrReplaceChild("left_moustache", CubeListBuilder.create()
                .texOffs(104, 109).mirror(true).addBox(0, -0.35f, -0.3f, 2.2f, 0.6f, 0.6f), PartPose.offsetAndRotation(0.3f, -2.25f, -3.45f, 0, -0.15f, 0.35f));
        PartDefinition lance = rightHand.addOrReplaceChild("lance", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.45f, -34, -0.45f, 0.9f, 44, 0.9f)
                .texOffs(240, 102).mirror(false).addBox(-0.8f, -35.4f, -0.8f, 1.6f, 1.6f, 1.6f)
                .texOffs(65, 83).mirror(false).addBox(-0.95f, -42.6f, -0.3f, 1.9f, 7.4f, 0.6f)
                .texOffs(249, 102).mirror(false).addBox(-0.5f, 9.6f, -0.5f, 1, 2.4f, 1), PartPose.offsetAndRotation(0, 1.2f, 0, 1.5985f, 0.2766f, 0.0964f));
        PartDefinition lanceTassel = lance.addOrReplaceChild("lance_tassel", CubeListBuilder.create()
                .texOffs(60, 102).mirror(false).addBox(-1.1f, 0, -1.1f, 2.2f, 2.6f, 2.2f)
                .texOffs(0, 109).mirror(false).addBox(-0.8f, 2.3f, -0.8f, 1.6f, 1.6f, 1.6f), PartPose.offsetAndRotation(0, -34.2f, 0, 0, 0, 0));
        PartDefinition pennant = lance.addOrReplaceChild("pennant", CubeListBuilder.create()
                .texOffs(0, 46).mirror(false).addBox(-0.1f, 0, 0, 0.2f, 5, 8.5f), PartPose.offsetAndRotation(0, -31.5f, 0.4f, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED cavalry: end
    }

    /**
     * The cloud cavalry: the steed gallops (fore and hind legs in turn, the knees folding, its body rocking,
     * mane and tail streaming) or, still, paws and swishes its tail; the rider sways with it, the spear held
     * slanting forward, thrust on a blow. Winding up the charge, the steed rears, forehooves pawing, and the
     * rider leans in, the spear couched level.
     */
    public static class CavalryModel extends EntityModel<CloudCavalryEntity> {
        private final ModelPart root, horse, neck, horseHead, tail, tailMid, tailEnd, rider, waist, body, head, rightArm, leftArm,
                rightForearm, lance, pennant;
        private final ModelPart[] legs = new ModelPart[4], shins = new ModelPart[4];
        private final List<ModelPart> mane = new ArrayList<>(), fringes = new ArrayList<>(), captainOnly = new ArrayList<>();
        @Nullable
        private final ModelPart plumeTail, cape, capeLower, plume;

        public CavalryModel(ModelPart root) {
            this.root = root;
            horse = root.getChild("horse");
            neck = horse.getChild("neck");
            horseHead = neck.getChild("horse_head");
            tail = horse.getChild("tail");
            tailMid = tail.getChild("tail_mid");
            tailEnd = tailMid.getChild("tail_end");
            String[] names = {"right_foreleg", "left_foreleg", "right_hindleg", "left_hindleg"};
            String[] shinNames = {"right_foreshin", "left_foreshin", "right_hindshin", "left_hindshin"};
            for (int i = 0; i < 4; i++) {
                legs[i] = horse.getChild(names[i]);
                shins[i] = legs[i].getChild(shinNames[i]);
            }
            for (int k = 0; neck.hasChild("mane_" + k); k++) mane.add(neck.getChild("mane_" + k));
            for (String n : new String[]{"peytral_fringe", "right_crupper_fringe", "left_crupper_fringe", "right_cloth_fringe", "left_cloth_fringe"}) {
                if (horse.hasChild(n)) fringes.add(horse.getChild(n));
            }
            rider = horse.getChild("rider");
            // the rider a size under the steed's
            rider.xScale = rider.yScale = rider.zScale = 0.8f;
            waist = rider.getChild("waist");
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
        }

        @Override
        public void setupAnim(CloudCavalryEntity e, float swing, float amount, float age, float yaw, float pitch) {
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            float rear = e.winding() > 0 ? 1f : 0f;
            float a = Math.min(1f, amount * 1.4f), w = swing * 0.55f;
            // the gallop: forelegs then hind legs, each pair a beat apart, the knees folding as each comes forward
            float[] phase = {0, 0.55f, Mth.PI, Mth.PI + 0.55f};
            for (int i = 0; i < 4; i++) {
                float s = Mth.sin(w + phase[i]);
                boolean fore = i < 2;
                float idle = fore && i == (int) (age / 60) % 2 ? -Math.max(0, Mth.sin(age * 0.12f)) * 0.35f * (1 - a) : 0;
                turn(legs[i], s * (fore ? 0.85f : 0.7f) * a + idle, 0, 0);
                float fold = Math.max(0, fore ? -Mth.cos(w + phase[i]) : Mth.cos(w + phase[i]));
                turn(shins[i], (fore ? 1.1f : -0.6f) * fold * a - idle * 1.6f, 0, 0);
            }
            // its body rocks and rises with each stride; at rest it breathes
            horse.loadPose(horse.getInitialPose());
            horse.xRot = Mth.sin(w * 2) * 0.05f * a;
            horse.y += -Math.abs(Mth.sin(w)) * 0.8f * a + Mth.sin(age * 0.08f) * 0.15f;
            turn(neck, -Mth.sin(w * 2) * 0.08f * a + Mth.sin(age * 0.06f) * 0.03f, yaw * Mth.DEG_TO_RAD * 0.35f, 0);
            turn(horseHead, pitch * Mth.DEG_TO_RAD * 0.3f + Mth.sin(age * 0.09f) * 0.04f, yaw * Mth.DEG_TO_RAD * 0.25f, 0);
            turn(tail, a * 0.55f + Mth.sin(age * 0.09f) * 0.06f, 0, Mth.sin(age * 0.06f) * 0.18f * (1 - a * 0.5f));
            turn(tailMid, a * 0.3f + Mth.sin(age * 0.09f + 0.6f) * 0.06f, 0, Mth.sin(age * 0.06f + 0.5f) * 0.12f);
            turn(tailEnd, a * 0.25f + Mth.sin(age * 0.09f + 1.2f) * 0.06f, 0, 0);
            for (int k = 0; k < mane.size(); k++) turn(mane.get(k), a * 0.5f + Mth.sin(age * 0.1f + k * 0.8f) * 0.06f, 0, 0);
            for (ModelPart f : fringes) turn(f, a * 0.4f + Mth.sin(age * 0.12f) * 0.05f, 0, 0);
            // the rider: he sways with the steed, leans into the gallop; the spear thrust on a blow
            turn(waist, a * 0.15f - horse.xRot, 0, 0);
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
            if (rear > 0) {
                // the steed rears on its hind hooves, forehooves pawing; the rider leans in, the spear couched
                float paw = Mth.sin(age * 0.5f) * 0.3f;
                horse.xRot = -0.45f;
                horse.y = horse.getInitialPose().y - 1.45f;
                horse.z = horse.getInitialPose().z + 7.66f;
                turn(legs[0], -1.25f + paw, 0, 0);
                turn(legs[1], -0.95f - paw, 0, 0);
                turn(shins[0], 1.5f - paw, 0, 0);
                turn(shins[1], 1.3f + paw, 0, 0);
                turn(legs[2], 0.45f, 0, 0);
                turn(legs[3], 0.35f, 0, 0);
                turn(shins[2], 0, 0, 0);
                turn(shins[3], 0.1f, 0, 0);
                turn(neck, 0.25f, 0, 0);
                turn(horseHead, -0.3f, 0, 0);
                turn(tail, 0.2f, 0, Mth.sin(age * 0.3f) * 0.2f);
                turn(waist, 0.6f, 0, 0);
                turn(rightArm, 0.25f, 0, 0.1f);
                turn(lance, 0.55f, 0, 0);
                head.xRot = -0.5f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
