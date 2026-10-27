package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.village.Calling;
import com.jttw.engine.village.TangFolkEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The folk of the Tang villages and the supernatural world's merchants, as the novel's commoners
 * look: one frame (a person of a player's proportions, 128 x 128) with every piece of dress any of
 * them wears, each look showing its own:
 * <ul>
 *   <li>the <b>farmer</b> in hemp, a conical straw hat (斗笠), trousers rolled; his wife in a
 *       headscarf, jacket and long skirt, an apron;</li>
 *   <li>the <b>monk</b>, shaven, in a grey robe to the ground under a russet kasaya, prayer beads;</li>
 *   <li>the <b>scholar</b> in a green round-collar robe (圆领袍) and the black futou cap (幞头), its
 *       two ties hanging behind; the <b>merchant</b> in a brown one; the herbalist in a grey-blue one,
 *       bearded;</li>
 *   <li>the <b>innkeeper</b> in a headcloth and a white apron; the <b>smith</b> in a headband and a
 *       leather apron, sleeves tight;</li>
 *   <li>the women of the market (innkeeper, merchant, herbalist) in ru-qun: a short jacket over a
 *       high-waisted skirt to the ground, the hair dressed high in a bun with a pin, a shawl (披帛);</li>
 *   <li>the <b>fortune teller</b> in a long blue robe, a square black cap, a thin beard;</li>
 *   <li>the <b>ghost merchant</b>, pale and half-seen, floating, in a funeral-white robe and a black
 *       futou, a paper lantern in his hand; the <b>celestial merchant</b> in white and gold, a gold
 *       crown on his topknot, a long white beard, a shawl drifting about him.</li>
 * </ul>
 * Model and skins: {@code assets_source/tang_folk.py}.
 */
public final class TangFolkRenderer extends MobRenderer<TangFolkEntity, TangFolkRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("tang_folk"), "main");

    /** Which look a creature wears: its calling, and a woman's for a woman. */
    public static String look(TangFolkEntity e) {
        return e.calling().id() + (e.isWoman() ? "_woman" : "");
    }

    private static final Map<String, ResourceLocation> SKINS = new HashMap<>();

    static ResourceLocation skin(String look) {
        return SKINS.computeIfAbsent(look, l -> JTTWEngine.id("textures/entity/tang_folk/" + l + ".png"));
    }

    public TangFolkRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(TangFolkEntity e) {
        return skin(LOOKS.containsKey(look(e)) ? look(e) : e.calling().id());
    }

    /** A ghost is half-seen. */
    @Nullable
    @Override
    protected RenderType getRenderType(TangFolkEntity e, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (e.calling() == Calling.GHOST_MERCHANT && bodyVisible) return RenderType.entityTranslucent(getTextureLocation(e));
        return super.getRenderType(e, bodyVisible, translucent, glowing);
    }

    /** A ghost floats a little over the ground, rising and falling. */
    @Override
    public void render(TangFolkEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        if (e.calling() == Calling.GHOST_MERCHANT && !e.isSleeping()) pose.translate(0, 0.18 + Mth.sin((e.tickCount + partial) * 0.07f) * 0.06, 0);
        super.render(e, yaw, partial, pose, buffers, light);
        pose.popPose();
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 19).mirror(false).addBox(-4, -8, -4, 8, 8, 8), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition futou = head.addOrReplaceChild("futou", CubeListBuilder.create()
                .texOffs(41, 36).mirror(false).addBox(-4.4f, -8.7f, -4.4f, 8.8f, 3.6f, 8.8f)
                .texOffs(0, 79).mirror(false).addBox(-2.4f, -11.2f, -1.2f, 4.8f, 3, 4.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition futouTies = head.addOrReplaceChild("futou_ties", CubeListBuilder.create()
                .texOffs(77, 67).mirror(false).addBox(-2.6f, 0, -0.25f, 1.3f, 7.5f, 0.5f)
                .texOffs(77, 67).mirror(false).addBox(1.3f, 0, -0.25f, 1.3f, 7.5f, 0.5f), PartPose.offsetAndRotation(0, -6.5f, 4.2f, 0.22f, 0, 0));
        PartDefinition strawHat = head.addOrReplaceChild("straw_hat", CubeListBuilder.create()
                .texOffs(33, 0).mirror(false).addBox(-8, -8.6f, -8, 16, 1, 16)
                .texOffs(78, 36).mirror(false).addBox(-5.5f, -9.8f, -5.5f, 11, 1.4f, 11)
                .texOffs(84, 67).mirror(false).addBox(-3.5f, -11.1f, -3.5f, 7, 1.4f, 7)
                .texOffs(40, 88).mirror(false).addBox(-1.5f, -12.2f, -1.5f, 3, 1.2f, 3), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition headcloth = head.addOrReplaceChild("headcloth", CubeListBuilder.create()
                .texOffs(0, 53).mirror(false).addBox(-4.35f, -8.6f, -4.35f, 8.7f, 3.2f, 8.7f)
                .texOffs(53, 88).mirror(false).addBox(-1.2f, -7.8f, 4.2f, 2.4f, 2.4f, 1.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition squareCap = head.addOrReplaceChild("square_cap", CubeListBuilder.create()
                .texOffs(33, 19).mirror(false).addBox(-4.45f, -12, -4.45f, 8.9f, 6.2f, 8.9f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition bun = head.addOrReplaceChild("bun", CubeListBuilder.create()
                .texOffs(21, 67).mirror(false).addBox(-2.2f, -12.6f, -1.2f, 4.4f, 4.8f, 4.4f)
                .texOffs(42, 67).mirror(false).addBox(-4.3f, -9, -3.6f, 8.6f, 1.4f, 8), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition hairpin = head.addOrReplaceChild("hairpin", CubeListBuilder.create()
                .texOffs(83, 88).mirror(false).addBox(-4.6f, -11.2f, 0.6f, 9.2f, 0.7f, 0.7f)
                .texOffs(64, 88).mirror(false).addBox(3.6f, -11.6f, 0.3f, 1.4f, 1.4f, 1.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition topknot = head.addOrReplaceChild("topknot", CubeListBuilder.create()
                .texOffs(21, 79).mirror(false).addBox(-1.6f, -11, -1.6f, 3.2f, 3.2f, 3.2f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition crown = head.addOrReplaceChild("crown", CubeListBuilder.create()
                .texOffs(57, 79).mirror(false).addBox(-2.4f, -10.4f, -2.4f, 4.8f, 2, 4.8f)
                .texOffs(73, 88).mirror(false).addBox(-0.5f, -12.4f, -0.5f, 1, 2.2f, 1), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition beard = head.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(29, 88).mirror(false).addBox(-1.8f, 0, -0.4f, 3.6f, 4.5f, 0.8f), PartPose.offsetAndRotation(0, -1.6f, -4.25f, 0.12f, 0, 0));
        PartDefinition longBeard = head.addOrReplaceChild("long_beard", CubeListBuilder.create()
                .texOffs(113, 67).mirror(false).addBox(-2.4f, 0, -0.5f, 4.8f, 8, 1), PartPose.offsetAndRotation(0, -1.6f, -4.25f, 0.08f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(70, 19).mirror(false).addBox(-4, 0, -2, 8, 12, 4), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition robeSkirt = body.addOrReplaceChild("robe_skirt", CubeListBuilder.create()
                .texOffs(95, 19).mirror(false).addBox(-4.6f, 11, -2.6f, 9.2f, 10, 5.2f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition longSkirt = body.addOrReplaceChild("long_skirt", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-4.6f, 8.5f, -2.6f, 9.2f, 12, 5.2f)
                .texOffs(81, 53).mirror(false).addBox(-5.4f, 20, -3.3f, 10.8f, 4, 6.6f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition belt = body.addOrReplaceChild("belt", CubeListBuilder.create()
                .texOffs(78, 79).mirror(false).addBox(-4.35f, 10, -2.35f, 8.7f, 1.4f, 4.7f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition sash = body.addOrReplaceChild("sash", CubeListBuilder.create()
                .texOffs(0, 88).mirror(false).addBox(-4.4f, 7.6f, -2.4f, 8.8f, 1.3f, 4.8f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition apron = body.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(62, 53).mirror(false).addBox(-3.6f, -1.5f, -0.4f, 7.2f, 10.5f, 0.6f), PartPose.offsetAndRotation(0, 11, -2.7f, -0.06f, 0, 0));
        PartDefinition rightShawl = body.addOrReplaceChild("right_shawl", CubeListBuilder.create()
                .texOffs(38, 79).mirror(false).addBox(-1.4f, -0.7f, -2.6f, 2.8f, 1.1f, 5.2f)
                .texOffs(0, 36).mirror(false).addBox(-1.5f, 0, -2.75f, 1.2f, 15, 0.5f), PartPose.offsetAndRotation(-3.2f, 0.4f, 0, 0, 0, 0.12f));
        PartDefinition leftShawl = body.addOrReplaceChild("left_shawl", CubeListBuilder.create()
                .texOffs(38, 79).mirror(true).addBox(-1.4f, -0.7f, -2.6f, 2.8f, 1.1f, 5.2f)
                .texOffs(0, 36).mirror(true).addBox(0.3f, 0, -2.75f, 1.2f, 15, 0.5f), PartPose.offsetAndRotation(3.2f, 0.4f, 0, 0, 0, -0.12f));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(7, 36).mirror(false).addBox(-3, -2, -2, 4, 12, 4), PartPose.offsetAndRotation(-5, 2, 0, 0, 0, 0));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(7, 36).mirror(true).addBox(-1, -2, -2, 4, 12, 4), PartPose.offsetAndRotation(5, 2, 0, 0, 0, 0));
        PartDefinition rightCuff = rightArm.addOrReplaceChild("right_cuff", CubeListBuilder.create()
                .texOffs(37, 53).mirror(false).addBox(-3.7f, 4.5f, -2.7f, 5.4f, 6.2f, 5.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition leftCuff = leftArm.addOrReplaceChild("left_cuff", CubeListBuilder.create()
                .texOffs(37, 53).mirror(true).addBox(-1.7f, 4.5f, -2.7f, 5.4f, 6.2f, 5.4f), PartPose.offsetAndRotation(0, 0, 0, 0, 0, 0));
        PartDefinition lantern = leftArm.addOrReplaceChild("lantern", CubeListBuilder.create()
                .texOffs(78, 88).mirror(false).addBox(-0.4f, 0, -0.4f, 0.8f, 3, 0.8f)
                .texOffs(0, 67).mirror(false).addBox(-2.2f, 2.5f, -2.2f, 4.4f, 5.5f, 4.4f), PartPose.offsetAndRotation(1, 10, 0, 0, 0, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(24, 36).mirror(false).addBox(-2, 0, -2, 4, 12, 4), PartPose.offsetAndRotation(-1.9f, 12, 0, 0, 0, 0));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(24, 36).mirror(true).addBox(-2, 0, -2, 4, 12, 4), PartPose.offsetAndRotation(1.9f, 12, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    /** The pieces of dress a look wears (the frame's other parts every look shows). */
    public static final Map<String, Set<String>> LOOKS = new HashMap<>();
    /** The pieces of dress some look leaves off. */
    public static final List<String> DRESS;

    static {
        // GENERATED looks: begin
        DRESS = List.of("futou", "futou_ties", "straw_hat", "headcloth", "square_cap", "bun", "hairpin", "topknot", "crown", "beard", "long_beard", "robe_skirt", "long_skirt", "belt", "sash", "apron", "right_shawl", "left_shawl", "right_cuff", "left_cuff", "lantern");
        LOOKS.put("farmer", Set.of("straw_hat", "robe_skirt", "belt"));
        LOOKS.put("farmer_woman", Set.of("headcloth", "long_skirt", "sash", "apron"));
        LOOKS.put("monk", Set.of("long_skirt", "right_cuff", "left_cuff", "sash"));
        LOOKS.put("scholar", Set.of("futou", "futou_ties", "robe_skirt", "belt", "right_cuff", "left_cuff"));
        LOOKS.put("merchant", Set.of("futou", "futou_ties", "robe_skirt", "belt", "right_cuff", "left_cuff"));
        LOOKS.put("merchant_woman", Set.of("bun", "hairpin", "long_skirt", "sash", "right_shawl", "left_shawl", "right_cuff", "left_cuff"));
        LOOKS.put("innkeeper", Set.of("headcloth", "robe_skirt", "belt", "apron"));
        LOOKS.put("innkeeper_woman", Set.of("bun", "hairpin", "long_skirt", "sash", "apron", "right_cuff", "left_cuff"));
        LOOKS.put("herbalist", Set.of("futou", "futou_ties", "robe_skirt", "belt", "right_cuff", "left_cuff", "beard"));
        LOOKS.put("herbalist_woman", Set.of("bun", "hairpin", "long_skirt", "sash", "right_shawl", "left_shawl", "right_cuff", "left_cuff"));
        LOOKS.put("fortune_teller", Set.of("square_cap", "beard", "long_skirt", "sash", "right_cuff", "left_cuff"));
        LOOKS.put("smith", Set.of("headcloth", "robe_skirt", "belt", "apron"));
        LOOKS.put("ghost_merchant", Set.of("futou", "futou_ties", "long_skirt", "sash", "right_cuff", "left_cuff", "lantern"));
        LOOKS.put("celestial_merchant", Set.of("topknot", "crown", "long_beard", "long_skirt", "sash", "right_shawl", "left_shawl", "right_cuff", "left_cuff"));
        // GENERATED looks: end
    }

    public static final class Model extends EntityModel<TangFolkEntity> {
        private final ModelPart root, head, body, rightArm, leftArm, rightLeg, leftLeg;
        private final Map<String, ModelPart> dress = new HashMap<>();
        private boolean ghost;

        public Model(ModelPart root) {
            this.root = root;
            head = root.getChild("head");
            body = root.getChild("body");
            rightArm = root.getChild("right_arm");
            leftArm = root.getChild("left_arm");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            for (String name : DRESS) find(root, name);
        }

        private void find(ModelPart part, String name) {
            if (part.hasChild(name)) {
                dress.put(name, part.getChild(name));
                return;
            }
            for (ModelPart child : part.getAllParts().toList()) if (child != part && child.hasChild(name)) dress.put(name, child.getChild(name));
        }

        @Override
        public void setupAnim(TangFolkEntity e, float swing, float amount, float age, float yaw, float pitch) {
            String look = look(e);
            Set<String> shown = LOOKS.getOrDefault(look, LOOKS.getOrDefault(e.calling().id(), Set.of()));
            dress.forEach((name, part) -> part.visible = shown.contains(name));
            ghost = e.calling() == Calling.GHOST_MERCHANT;
            boolean gown = shown.contains("long_skirt");
            rightLeg.visible = leftLeg.visible = !gown;
            head.yRot = yaw * Mth.DEG_TO_RAD;
            head.xRot = pitch * Mth.DEG_TO_RAD;
            float stride = Mth.cos(swing * 0.6662f) * 1.1f * amount;
            rightLeg.xRot = stride;
            leftLeg.xRot = -stride;
            // arms: a walker's swing, slight; a ghost's held still before it, the lantern raised
            float idle = Mth.sin(age * 0.067f) * 0.03f;
            rightArm.xRot = -stride * 0.5f - idle;
            leftArm.xRot = stride * 0.5f + idle;
            rightArm.zRot = 0.06f + idle;
            leftArm.zRot = -0.06f - idle;
            if (ghost) {
                leftArm.xRot = -0.3f + idle;
                rightArm.xRot = -0.25f;
            }
            if (e.isTrading()) {
                // hands folded in the sleeves before one: the shopkeeper's courtesy
                rightArm.xRot = leftArm.xRot = -0.75f;
                rightArm.yRot = -0.45f;
                leftArm.yRot = 0.45f;
            } else {
                rightArm.yRot = leftArm.yRot = 0;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, ghost ? (color & 0x00FFFFFF) | 0xA8000000 : color);
        }
    }
}
