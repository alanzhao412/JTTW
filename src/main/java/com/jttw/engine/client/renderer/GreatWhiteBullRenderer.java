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
 * The Great White Bull: a vast white bull, humped at the shoulder, with a broad head, a pink-nosed
 * snout and great horns curving out and up, gold at the tips; a long tail that swishes.
 */
public final class GreatWhiteBullRenderer extends MobRenderer<GreatWhiteBullEntity, GreatWhiteBullRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "great_white_bull"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/great_white_bull.png");
    /** How much larger than its model's pixels it is drawn: a great bull. */
    public static final float SCALE = 1.35f;

    public GreatWhiteBullRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.2f);
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
        // a deep body on tall legs, a great hump over the shoulders, a thick neck thrust forward
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-9, -9, -15, 18, 17, 30)
                .texOffs(0, 47).addBox(-7, -15, -14, 14, 7, 13)
                .texOffs(40, 93).addBox(-6, -8, -20, 12, 12, 6), PartPose.ZERO);
        // the head carried low, horns and all, as a bull about to charge
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(54, 47).addBox(-6, -6, -11, 12, 12, 12)
                .texOffs(102, 47).addBox(-4, 0, -15, 8, 6, 5), PartPose.offsetAndRotation(0, -1, -19, 0.2f, 0, 0));
        for (int side : new int[]{-1, 1}) {
            String n = side < 0 ? "right" : "left";
            // out from the brow, then sweeping up and forward, gold at the points
            PartDefinition base = head.addOrReplaceChild(n + "_horn", CubeListBuilder.create()
                    .texOffs(102, 58).mirror(side < 0).addBox(side < 0 ? -8 : 0, -1.5f, -1.5f, 8, 3, 3),
                    PartPose.offsetAndRotation(side * 5, -5, -6, 0, side * 0.25f, side * -0.15f));
            PartDefinition up = base.addOrReplaceChild(n + "_horn_up", CubeListBuilder.create()
                    .texOffs(102, 64).addBox(-1.5f, -7, -1.5f, 3, 7, 3),
                    PartPose.offsetAndRotation(side * 7, 0, 0, -0.55f, 0, side * 0.35f));
            up.addOrReplaceChild(n + "_horn_tip", CubeListBuilder.create()
                    .texOffs(114, 64).addBox(-1, -5, -1, 2, 5, 2),
                    PartPose.offsetAndRotation(0, -7, 0, -0.45f, 0, side * -0.35f));
        }
        for (int i = 0; i < 4; i++) {
            boolean front = i < 2, left = i % 2 == 1;
            root.addOrReplaceChild(legName(i), CubeListBuilder.create()
                    .texOffs(0, 71).mirror(left).addBox(-3, 0, -3, 6, 16, 6),
                    PartPose.offset(left ? 5.5f : -5.5f, 8, front ? -10 : 11));
        }
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(24, 71).addBox(-1, 0, -1, 2, 14, 2), PartPose.offsetAndRotation(0, -7, 15, 0.35f, 0, 0));
        tail.addOrReplaceChild("tuft", CubeListBuilder.create()
                .texOffs(32, 71).addBox(-1.5f, 13, -1.5f, 3, 4, 3), PartPose.ZERO);
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
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.5f + 0.2f;
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
