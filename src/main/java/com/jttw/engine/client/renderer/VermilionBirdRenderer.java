package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.beast.TrueFormEntity;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Vermilion Bird, the Phoenix's true form: a vast bird of flame, its wings spread wide and
 * beating, plumage vermilion running to gold, a crest of flame, a hooked beak, eyes burning gold and
 * long streaming tail plumes; its flight feathers, crest and plumes glow. Model and skin:
 * {@code assets_source/vermilion_bird.py}.
 */
public final class VermilionBirdRenderer extends MobRenderer<TrueFormEntity, VermilionBirdRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(JTTWEngine.MOD_ID, "vermilion_bird"), "main");
    public static final ResourceLocation TEXTURE = new ResourceLocation(JTTWEngine.MOD_ID, "textures/entity/vermilion_bird.png");
    private static final RenderType GLOW = RenderType.eyes(new ResourceLocation(JTTWEngine.MOD_ID, "textures/entity/vermilion_bird_glow.png"));
    public static final float SCALE = 2.0f;

    public VermilionBirdRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.2f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected void scale(TrueFormEntity e, PoseStack pose, float partial) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(TrueFormEntity e) {
        return TEXTURE;
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-5, -5, -8, 10, 10, 16)
                .texOffs(53, 0).mirror(false).addBox(-4, 0, -7, 8, 6, 12), PartPose.offsetAndRotation(0, -2, 0, -0.2f, 0, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(199, 0).mirror(false).addBox(-3, -3, -6, 6, 6, 7), PartPose.offsetAndRotation(0, -3, -7, -0.55f, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(226, 0).mirror(false).addBox(-3.2f, -3.2f, -6, 6.4f, 6, 6.5f)
                .texOffs(33, 41).mirror(false).addBox(-1.4f, -1, -10.5f, 2.8f, 2.6f, 5)
                .texOffs(78, 41).mirror(false).addBox(-1, 1, -11.5f, 2, 1.4f, 1.5f), PartPose.offsetAndRotation(0, -1, -5.5f, 0.75f, 0, 0));
        PartDefinition legs = body.addOrReplaceChild("legs", CubeListBuilder.create()
                .texOffs(229, 27).mirror(false).addBox(-3, 0, -1, 2, 7, 2)
                .texOffs(229, 27).mirror(false).addBox(1, 0, -1, 2, 7, 2), PartPose.offsetAndRotation(0, 5, 3, 0.9f, 0, 0));
        PartDefinition crest0 = head.addOrReplaceChild("crest0", CubeListBuilder.create()
                .texOffs(50, 41).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 6), PartPose.offsetAndRotation(-1.5f, -3, -1.5f, 0.7f, -0.375f, 0));
        PartDefinition crest1 = head.addOrReplaceChild("crest1", CubeListBuilder.create()
                .texOffs(238, 27).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 8), PartPose.offsetAndRotation(0, -3, -1.5f, 0.5f, 0, 0));
        PartDefinition crest2 = head.addOrReplaceChild("crest2", CubeListBuilder.create()
                .texOffs(50, 41).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 6), PartPose.offsetAndRotation(1.5f, -3, -1.5f, 0.7f, 0.375f, 0));
        PartDefinition crest3 = head.addOrReplaceChild("crest3", CubeListBuilder.create()
                .texOffs(65, 41).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 5), PartPose.offsetAndRotation(-0.8f, -3, -1.5f, 0.95f, -0.2f, 0));
        PartDefinition crest4 = head.addOrReplaceChild("crest4", CubeListBuilder.create()
                .texOffs(65, 41).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 5), PartPose.offsetAndRotation(0.8f, -3, -1.5f, 0.95f, 0.2f, 0));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(60, 27).mirror(false).addBox(-12, -1, -4, 12, 2.4f, 9), PartPose.offsetAndRotation(-4.5f, -3.5f, -3, 0, 0, 0.45f));
        PartDefinition rightWingMid = rightWing.addOrReplaceChild("right_wing_mid", CubeListBuilder.create()
                .texOffs(103, 27).mirror(false).addBox(-12, -0.8f, -3.5f, 12, 1.8f, 9.5f), PartPose.offsetAndRotation(-12, 0, 0, 0, 0, -0.15f));
        PartDefinition rightWingTip = rightWingMid.addOrReplaceChild("right_wing_tip", CubeListBuilder.create()
                .texOffs(0, 41).mirror(false).addBox(-9, -0.5f, -2.5f, 9, 1.2f, 7), PartPose.offsetAndRotation(-12, 0, 0, 0, 0, -0.3f));
        PartDefinition rightFeather0 = rightWing.addOrReplaceChild("right_feather0", CubeListBuilder.create()
                .texOffs(204, 27).mirror(false).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 9), PartPose.offsetAndRotation(-1.5f, 0.4f, 4, 0.15f, -0, 0));
        PartDefinition rightFeather1 = rightWing.addOrReplaceChild("right_feather1", CubeListBuilder.create()
                .texOffs(148, 27).mirror(false).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 10.2f), PartPose.offsetAndRotation(-5.5f, 0.4f, 4, 0.15f, -0.05f, 0));
        PartDefinition rightFeather2 = rightWingMid.addOrReplaceChild("right_feather2", CubeListBuilder.create()
                .texOffs(0, 27).mirror(false).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 11.4f), PartPose.offsetAndRotation(2.5f, 0.4f, 4, 0.15f, -0.1f, 0));
        PartDefinition rightFeather3 = rightWingMid.addOrReplaceChild("right_feather3", CubeListBuilder.create()
                .texOffs(166, 0).mirror(false).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 12.6f), PartPose.offsetAndRotation(-1.5f, 0.4f, 4, 0.15f, -0.15f, 0));
        PartDefinition rightFeather4 = rightWingTip.addOrReplaceChild("right_feather4", CubeListBuilder.create()
                .texOffs(131, 0).mirror(false).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 13.8f), PartPose.offsetAndRotation(6.5f, 0.4f, 4, 0.15f, -0.2f, 0));
        PartDefinition rightFeather5 = rightWingTip.addOrReplaceChild("right_feather5", CubeListBuilder.create()
                .texOffs(94, 0).mirror(false).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 15), PartPose.offsetAndRotation(2.5f, 0.4f, 4, 0.15f, -0.25f, 0));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(60, 27).mirror(true).addBox(0, -1, -4, 12, 2.4f, 9), PartPose.offsetAndRotation(4.5f, -3.5f, -3, 0, 0, -0.45f));
        PartDefinition leftWingMid = leftWing.addOrReplaceChild("left_wing_mid", CubeListBuilder.create()
                .texOffs(103, 27).mirror(true).addBox(0, -0.8f, -3.5f, 12, 1.8f, 9.5f), PartPose.offsetAndRotation(12, 0, 0, 0, 0, 0.15f));
        PartDefinition leftWingTip = leftWingMid.addOrReplaceChild("left_wing_tip", CubeListBuilder.create()
                .texOffs(0, 41).mirror(true).addBox(0, -0.5f, -2.5f, 9, 1.2f, 7), PartPose.offsetAndRotation(12, 0, 0, 0, 0, 0.3f));
        PartDefinition leftFeather0 = leftWing.addOrReplaceChild("left_feather0", CubeListBuilder.create()
                .texOffs(204, 27).mirror(true).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 9), PartPose.offsetAndRotation(1.5f, 0.4f, 4, 0.15f, 0, 0));
        PartDefinition leftFeather1 = leftWing.addOrReplaceChild("left_feather1", CubeListBuilder.create()
                .texOffs(148, 27).mirror(true).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 10.2f), PartPose.offsetAndRotation(5.5f, 0.4f, 4, 0.15f, 0.05f, 0));
        PartDefinition leftFeather2 = leftWingMid.addOrReplaceChild("left_feather2", CubeListBuilder.create()
                .texOffs(0, 27).mirror(true).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 11.4f), PartPose.offsetAndRotation(-2.5f, 0.4f, 4, 0.15f, 0.1f, 0));
        PartDefinition leftFeather3 = leftWingMid.addOrReplaceChild("left_feather3", CubeListBuilder.create()
                .texOffs(166, 0).mirror(true).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 12.6f), PartPose.offsetAndRotation(1.5f, 0.4f, 4, 0.15f, 0.15f, 0));
        PartDefinition leftFeather4 = leftWingTip.addOrReplaceChild("left_feather4", CubeListBuilder.create()
                .texOffs(131, 0).mirror(true).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 13.8f), PartPose.offsetAndRotation(-6.5f, 0.4f, 4, 0.15f, 0.2f, 0));
        PartDefinition leftFeather5 = leftWingTip.addOrReplaceChild("left_feather5", CubeListBuilder.create()
                .texOffs(94, 0).mirror(true).addBox(-1.2f, -0.4f, 0, 2.4f, 0.8f, 15), PartPose.offsetAndRotation(-2.5f, 0.4f, 4, 0.15f, 0.25f, 0));
        PartDefinition plume0 = body.addOrReplaceChild("plume0", CubeListBuilder.create()
                .texOffs(31, 27).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 12), PartPose.offsetAndRotation(-3, -1, 7.5f, 0.35f, -0.3f, 0));
        PartDefinition plume0End = plume0.addOrReplaceChild("plume0_end", CubeListBuilder.create()
                .texOffs(177, 27).mirror(false).addBox(-1.3f, -0.4f, 0, 2.6f, 0.8f, 10), PartPose.offsetAndRotation(0, 0, 12, 0.25f, -0.18f, 0));
        PartDefinition plume1 = body.addOrReplaceChild("plume1", CubeListBuilder.create()
                .texOffs(31, 27).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 12), PartPose.offsetAndRotation(-1.5f, -1, 7.5f, 0.35f, -0.15f, 0));
        PartDefinition plume1End = plume1.addOrReplaceChild("plume1_end", CubeListBuilder.create()
                .texOffs(177, 27).mirror(false).addBox(-1.3f, -0.4f, 0, 2.6f, 0.8f, 10), PartPose.offsetAndRotation(0, 0, 12, 0.25f, -0.09f, 0));
        PartDefinition plume2 = body.addOrReplaceChild("plume2", CubeListBuilder.create()
                .texOffs(31, 27).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 12), PartPose.offsetAndRotation(0, -1, 7.5f, 0.35f, 0, 0));
        PartDefinition plume2End = plume2.addOrReplaceChild("plume2_end", CubeListBuilder.create()
                .texOffs(177, 27).mirror(false).addBox(-1.3f, -0.4f, 0, 2.6f, 0.8f, 10), PartPose.offsetAndRotation(0, 0, 12, 0.25f, 0, 0));
        PartDefinition plume3 = body.addOrReplaceChild("plume3", CubeListBuilder.create()
                .texOffs(31, 27).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 12), PartPose.offsetAndRotation(1.5f, -1, 7.5f, 0.35f, 0.15f, 0));
        PartDefinition plume3End = plume3.addOrReplaceChild("plume3_end", CubeListBuilder.create()
                .texOffs(177, 27).mirror(false).addBox(-1.3f, -0.4f, 0, 2.6f, 0.8f, 10), PartPose.offsetAndRotation(0, 0, 12, 0.25f, 0.09f, 0));
        PartDefinition plume4 = body.addOrReplaceChild("plume4", CubeListBuilder.create()
                .texOffs(31, 27).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 12), PartPose.offsetAndRotation(3, -1, 7.5f, 0.35f, 0.3f, 0));
        PartDefinition plume4End = plume4.addOrReplaceChild("plume4_end", CubeListBuilder.create()
                .texOffs(177, 27).mirror(false).addBox(-1.3f, -0.4f, 0, 2.6f, 0.8f, 10), PartPose.offsetAndRotation(0, 0, 12, 0.25f, 0.18f, 0));
        PartDefinition rightTalon0 = legs.addOrReplaceChild("right_talon0", CubeListBuilder.create()
                .texOffs(87, 41).mirror(false).addBox(-0.25f, -0.25f, -1.6f, 0.5f, 0.5f, 1.6f), PartPose.offsetAndRotation(-2.6f, 7, 0, 0.6f, 0, 0));
        PartDefinition rightTalon1 = legs.addOrReplaceChild("right_talon1", CubeListBuilder.create()
                .texOffs(87, 41).mirror(false).addBox(-0.25f, -0.25f, -1.6f, 0.5f, 0.5f, 1.6f), PartPose.offsetAndRotation(-1.4f, 7, 0, 0.6f, 0, 0));
        PartDefinition leftTalon0 = legs.addOrReplaceChild("left_talon0", CubeListBuilder.create()
                .texOffs(87, 41).mirror(false).addBox(-0.25f, -0.25f, -1.6f, 0.5f, 0.5f, 1.6f), PartPose.offsetAndRotation(1.4f, 7, 0, 0.6f, 0, 0));
        PartDefinition leftTalon1 = legs.addOrReplaceChild("left_talon1", CubeListBuilder.create()
                .texOffs(87, 41).mirror(false).addBox(-0.25f, -0.25f, -1.6f, 0.5f, 0.5f, 1.6f), PartPose.offsetAndRotation(2.6f, 7, 0, 0.6f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<TrueFormEntity> {
        private final ModelPart root, body, neck, head;
        private final ModelPart[] wings = new ModelPart[6];
        private final ModelPart[] plumes = new ModelPart[5];

        public Model(ModelPart root) {
            this.root = root;
            this.body = root.getChild("body");
            this.neck = body.getChild("neck");
            this.head = neck.getChild("head");
            String[] sides = {"right", "left"};
            for (int s = 0; s < 2; s++) {
                wings[s * 3] = body.getChild(sides[s] + "_wing");
                wings[s * 3 + 1] = wings[s * 3].getChild(sides[s] + "_wing_mid");
                wings[s * 3 + 2] = wings[s * 3 + 1].getChild(sides[s] + "_wing_tip");
            }
            for (int k = 0; k < 5; k++) plumes[k] = body.getChild("plume" + k);
        }

        @Override
        public void setupAnim(TrueFormEntity e, float swing, float amount, float age, float yaw, float pitch) {
            // great slow wingbeats, the outer lengths following through
            float beat = age * 0.16f;
            for (int s = 0; s < 2; s++) {
                float side = s == 0 ? -1 : 1;
                for (int k = 0; k < 3; k++) {
                    var rest = wings[s * 3 + k].getInitialPose();
                    wings[s * 3 + k].zRot = rest.zRot - side * Mth.sin(beat - k * 0.6f) * (0.42f + k * 0.12f);
                }
            }
            body.y = body.getInitialPose().y + Mth.sin(beat + Mth.PI) * 1.2f;
            var headRest = head.getInitialPose();
            head.yRot = headRest.yRot + yaw * Mth.DEG_TO_RAD * 0.5f;
            head.xRot = headRest.xRot + pitch * Mth.DEG_TO_RAD * 0.4f;
            for (int k = 0; k < 5; k++) {
                var rest = plumes[k].getInitialPose();
                plumes[k].xRot = rest.xRot + Mth.sin(age * 0.12f + k * 0.7f) * 0.08f;
            }
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
            root.render(pose, vc, light, overlay, r, g, b, a);
        }
    }
}
