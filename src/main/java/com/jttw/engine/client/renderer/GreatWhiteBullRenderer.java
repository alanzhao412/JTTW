package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.bull.GreatWhiteBullEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Great White Bull: a vast, wedge-shaped bull, all its weight in a towering chest and shoulder
 * hump crested with bone spikes, tapering to lean hindquarters. Its head is carried low as it
 * charges, under a heavy brow with eyes that burn red; a long, dark, squared snout with flared
 * nostrils and a gold ring; horns thick and black at the skull that sweep out, then forward and in
 * to gold points; a whip of a tail.
 */
public final class GreatWhiteBullRenderer extends MobRenderer<GreatWhiteBullEntity, GreatWhiteBullRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "great_white_bull"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/great_white_bull.png");
    private static final net.minecraft.client.renderer.RenderType EYES = net.minecraft.client.renderer.RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/great_white_bull_eyes.png"));
    /** How much larger than its model's pixels it is drawn: a great bull, its hump at its height. */
    public static final float SCALE = 1.25f;

    public GreatWhiteBullRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.2f);
        // its eyes burn under the brow: drawn full-bright over the skin, even in the dark
        addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
            @Override
            public net.minecraft.client.renderer.RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected void scale(GreatWhiteBullEntity e, PoseStack pose, float partial) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(GreatWhiteBullEntity e) {
        return TEXTURE;
    }

    /**
     * Its parts, in model pixels with the ground at y 24 (each box's texture at the offsets the
     * generated skin is painted to: see {@code assets_source/great_white_bull.py}).
     */
    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // a wedge: a towering chest and shoulders, lean hindquarters, a hump crested with bone
        // spikes, and a thick neck thrust forward and down
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10, -9, -16, 20, 19, 14)     // chest and shoulders
                .texOffs(0, 33).addBox(-8, -5, -2, 16, 15, 17)      // hindquarters, lower than the shoulders
                .texOffs(28, 104).addBox(-6, -11, -4, 12, 5, 9)     // the back, stepping down from the hump
                .texOffs(0, 104).addBox(-4, 5, -21, 8, 6, 6)        // the dewlap, heavy under the neck
                .texOffs(68, 0).addBox(-7, -16, -15, 14, 8, 11)     // the hump
                .texOffs(68, 19).addBox(-7, -8, -22, 14, 13, 8)     // the neck
                .texOffs(84, 80).addBox(-1, -19, -13, 2, 4, 2)      // the hump's spikes, falling away
                .texOffs(84, 80).addBox(-1, -18, -9, 2, 4, 2)
                .texOffs(84, 80).addBox(-1, -17, -5, 2, 4, 2), PartPose.ZERO);
        // the head carried low, as a bull about to charge: a skull under a heavy brow, a long
        // squared snout and jaw, a ring through the nose
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(68, 40).addBox(-6, -6, -10, 12, 10, 10)     // skull
                .texOffs(66, 60).addBox(-7, -7, -11, 14, 3, 4)       // brow, overhanging the eyes
                .texOffs(0, 66).addBox(-4.5f, -2, -17, 9, 7, 7)      // snout
                .texOffs(32, 66).addBox(-4, 5, -15, 8, 2, 5)         // jaw
                .texOffs(92, 80).addBox(-2.5f, 5, -17.8f, 5, 1, 1)   // nose ring
                .texOffs(104, 80).addBox(-2.5f, 2, -17.8f, 1, 3, 1)
                .texOffs(104, 80).addBox(1.5f, 2, -17.8f, 1, 3, 1),
                PartPose.offsetAndRotation(0, -3, -21, 0.25f, 0, 0));
        for (int side : new int[]{-1, 1}) {
            String n = side < 0 ? "right" : "left";
            // ears laid back under the horns
            head.addOrReplaceChild(n + "_ear", CubeListBuilder.create()
                    .texOffs(72, 80).mirror(side < 0).addBox(side < 0 ? -4 : 0, 0, 0, 4, 2, 1),
                    PartPose.offsetAndRotation(side * 6, -3, -3, 0.3f, side * -0.5f, side * 0.4f));
            // thick from the skull and out, then forward and up, then in to the points
            PartDefinition base = head.addOrReplaceChild(n + "_horn", CubeListBuilder.create()
                    .texOffs(58, 67).mirror(side < 0).addBox(side < 0 ? -9 : 0, -2, -2, 9, 4, 4),
                    PartPose.offsetAndRotation(side * 6, -5, -6, 0, side * 0.2f, side * -0.15f));
            PartDefinition mid = base.addOrReplaceChild(n + "_horn_up", CubeListBuilder.create()
                    .texOffs(84, 67).addBox(-1.5f, -8, -1.5f, 3, 8, 3),
                    PartPose.offsetAndRotation(side * 8.5f, 0, 0, 0.6f, 0, side * 0.45f));
            mid.addOrReplaceChild(n + "_horn_tip", CubeListBuilder.create()
                    .texOffs(96, 67).addBox(-1, -6, -1, 2, 6, 2),
                    PartPose.offsetAndRotation(0, -8, 0, 0.5f, 0, side * -0.6f));
        }
        // thick forelegs under the weight, leaner hind legs
        for (int i = 0; i < 4; i++) {
            boolean front = i < 2, left = i % 2 == 1;
            CubeListBuilder leg = front
                    ? CubeListBuilder.create().texOffs(0, 80).mirror(left).addBox(-3.5f, 0, -3.5f, 7, 14, 7)
                    : CubeListBuilder.create().texOffs(28, 80).mirror(left).addBox(-3, 0, -3, 6, 14, 6);
            root.addOrReplaceChild(legName(i), leg,
                    PartPose.offset(front ? (left ? 6 : -6) : (left ? 5 : -5), 10, front ? -10 : 11));
        }
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(52, 80).addBox(-1, 0, -1, 2, 14, 2), PartPose.offsetAndRotation(0, -5, 15, 0.35f, 0, 0));
        tail.addOrReplaceChild("tuft", CubeListBuilder.create()
                .texOffs(60, 80).addBox(-1.5f, 13, -1.5f, 3, 5, 3), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static String legName(int i) {
        return switch (i) {
            case 0 -> "right_front_leg";
            case 1 -> "left_front_leg";
            case 2 -> "right_hind_leg";
            default -> "left_hind_leg";
        };
    }

    public static final class Model extends EntityModel<GreatWhiteBullEntity> {
        private final ModelPart root, head, tail;
        private final ModelPart[] legs = new ModelPart[4];

        public Model(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.tail = root.getChild("tail");
            for (int i = 0; i < 4; i++) legs[i] = root.getChild(legName(i));
        }

        @Override
        public void setupAnim(GreatWhiteBullEntity e, float swing, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.6f;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.5f + 0.25f;
            // a heavy, diagonal gait
            for (int i = 0; i < 4; i++) {
                boolean diagonal = i == 0 || i == 3;
                legs[i].xRot = Mth.cos(swing * 0.6f + (diagonal ? 0 : Mth.PI)) * 1.1f * amount;
            }
            tail.zRot = Mth.sin(age * 0.12f) * 0.25f;
            tail.xRot = 0.35f + Mth.sin(age * 0.09f) * 0.08f + amount * 0.4f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
