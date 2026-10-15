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
 * The Black Tortoise, the World Turtle's true form: a vast dark tortoise under a high, ridged,
 * bronze-plated shell, on columns of legs; a great serpent coiled about its shell, rising over the
 * tortoise's head, hooded, jaws open, eyes burning red. Model and skin:
 * {@code assets_source/black_tortoise.py}.
 */
public final class BlackTortoiseRenderer extends MobRenderer<TrueFormEntity, BlackTortoiseRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "black_tortoise"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/black_tortoise.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/black_tortoise_glow.png"));
    public static final float SCALE = 4.2f;

    public BlackTortoiseRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 4.4f);
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
        PartDefinition shell = root.addOrReplaceChild("shell", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-13, 5, -15, 26, 4, 30)
                .texOffs(113, 0).mirror(false).addBox(-12, 1, -14, 24, 4, 28)
                .texOffs(105, 35).mirror(false).addBox(-10, -2, -11.5f, 20, 3, 23)
                .texOffs(0, 66).mirror(false).addBox(-7, -4.5f, -8, 14, 2.5f, 16)
                .texOffs(0, 35).mirror(false).addBox(-12, 9, -14, 24, 1.5f, 28)
                .texOffs(38, 100).mirror(false).addBox(-1, -6.5f, -6.5f, 2, 2, 3)
                .texOffs(38, 100).mirror(false).addBox(-1, -6.5f, -0.5f, 2, 2, 3)
                .texOffs(38, 100).mirror(false).addBox(-1, -6.5f, 5.5f, 2, 2, 3)
                .texOffs(38, 100).mirror(false).addBox(-8.5f, -3.5f, -2, 2, 2, 3)
                .texOffs(38, 100).mirror(false).addBox(6.5f, -3.5f, -2, 2, 2, 3), PartPose.offsetAndRotation(0, 5, 0, 0, 0, 0));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(98, 86).mirror(false).addBox(-1.6f, -1.6f, 0, 3.2f, 3.2f, 6), PartPose.offsetAndRotation(0, 13, 14.5f, -0.4f, 0, 0));
        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(61, 66).mirror(false).addBox(-3.2f, -3.2f, -8, 6.4f, 6.4f, 8.5f), PartPose.offsetAndRotation(0, 11.5f, -13.5f, 0.15f, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(131, 66).mirror(false).addBox(-3.6f, -3.6f, -6.5f, 7.2f, 6.2f, 7)
                .texOffs(222, 86).mirror(false).addBox(-2.6f, -0.5f, -9, 5.2f, 3.6f, 3)
                .texOffs(49, 100).mirror(false).addBox(-1.6f, 2.5f, -9.6f, 3.2f, 1.6f, 1.2f)
                .texOffs(241, 86).mirror(false).addBox(-4.1f, -4.2f, -6.8f, 2.6f, 1.6f, 4)
                .texOffs(241, 86).mirror(false).addBox(1.5f, -4.2f, -6.8f, 2.6f, 1.6f, 4), PartPose.offsetAndRotation(0, -0.5f, -7.5f, -0.1f, 0, 0));
        PartDefinition rightFrontLeg = root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                .texOffs(94, 66).mirror(false).addBox(-4.4f, 0, -4.4f, 8.8f, 7, 8.8f)
                .texOffs(162, 66).mirror(false).addBox(-5, 6.3f, -5.6f, 10, 2.2f, 10.2f), PartPose.offsetAndRotation(-11, 15.5f, -9.5f, 0, 0, 0.25f));
        PartDefinition rightFrontLegClaw0 = rightFrontLeg.addOrReplaceChild("right_front_leg_claw0", CubeListBuilder.create()
                .texOffs(62, 100).mirror(false).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(-3, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition rightFrontLegClaw1 = rightFrontLeg.addOrReplaceChild("right_front_leg_claw1", CubeListBuilder.create()
                .texOffs(62, 100).mirror(false).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(0, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition rightFrontLegClaw2 = rightFrontLeg.addOrReplaceChild("right_front_leg_claw2", CubeListBuilder.create()
                .texOffs(62, 100).mirror(false).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(3, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition leftFrontLeg = root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                .texOffs(94, 66).mirror(true).addBox(-4.4f, 0, -4.4f, 8.8f, 7, 8.8f)
                .texOffs(162, 66).mirror(true).addBox(-5, 6.3f, -5.6f, 10, 2.2f, 10.2f), PartPose.offsetAndRotation(11, 15.5f, -9.5f, 0, 0, -0.25f));
        PartDefinition leftFrontLegClaw0 = leftFrontLeg.addOrReplaceChild("left_front_leg_claw0", CubeListBuilder.create()
                .texOffs(62, 100).mirror(true).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(-3, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition leftFrontLegClaw1 = leftFrontLeg.addOrReplaceChild("left_front_leg_claw1", CubeListBuilder.create()
                .texOffs(62, 100).mirror(true).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(0, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition leftFrontLegClaw2 = leftFrontLeg.addOrReplaceChild("left_front_leg_claw2", CubeListBuilder.create()
                .texOffs(62, 100).mirror(true).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(3, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition rightHindLeg = root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                .texOffs(94, 66).mirror(false).addBox(-4.4f, 0, -4.4f, 8.8f, 7, 8.8f)
                .texOffs(162, 66).mirror(false).addBox(-5, 6.3f, -5.6f, 10, 2.2f, 10.2f), PartPose.offsetAndRotation(-11, 15.5f, 9.5f, 0, 0, 0.25f));
        PartDefinition rightHindLegClaw0 = rightHindLeg.addOrReplaceChild("right_hind_leg_claw0", CubeListBuilder.create()
                .texOffs(62, 100).mirror(false).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(-3, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition rightHindLegClaw1 = rightHindLeg.addOrReplaceChild("right_hind_leg_claw1", CubeListBuilder.create()
                .texOffs(62, 100).mirror(false).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(0, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition rightHindLegClaw2 = rightHindLeg.addOrReplaceChild("right_hind_leg_claw2", CubeListBuilder.create()
                .texOffs(62, 100).mirror(false).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(3, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition leftHindLeg = root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                .texOffs(94, 66).mirror(true).addBox(-4.4f, 0, -4.4f, 8.8f, 7, 8.8f)
                .texOffs(162, 66).mirror(true).addBox(-5, 6.3f, -5.6f, 10, 2.2f, 10.2f), PartPose.offsetAndRotation(11, 15.5f, 9.5f, 0, 0, -0.25f));
        PartDefinition leftHindLegClaw0 = leftHindLeg.addOrReplaceChild("left_hind_leg_claw0", CubeListBuilder.create()
                .texOffs(62, 100).mirror(true).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(-3, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition leftHindLegClaw1 = leftHindLeg.addOrReplaceChild("left_hind_leg_claw1", CubeListBuilder.create()
                .texOffs(62, 100).mirror(true).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(0, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition leftHindLegClaw2 = leftHindLeg.addOrReplaceChild("left_hind_leg_claw2", CubeListBuilder.create()
                .texOffs(62, 100).mirror(true).addBox(-0.45f, -0.45f, -1.8f, 0.9f, 0.9f, 1.8f), PartPose.offsetAndRotation(3, 7.6f, -5.4f, 0.5f, 0, 0));
        PartDefinition snake0 = root.addOrReplaceChild("snake0", CubeListBuilder.create()
                .texOffs(182, 86).mirror(false).addBox(-0.8f, -0.8f, -0.4f, 1.6f, 1.6f, 5.9372f), PartPose.offsetAndRotation(7.0369f, 11.5f, -15.5926f, 0.0487f, -1.9145f, 0));
        PartDefinition snake1 = root.addOrReplaceChild("snake1", CubeListBuilder.create()
                .texOffs(163, 86).mirror(false).addBox(-1.0167f, -1.0167f, -0.4f, 2.0333f, 2.0333f, 5.8744f), PartPose.offsetAndRotation(2.2059f, 11.25f, -17.3219f, 0.0493f, -1.5466f, 0));
        PartDefinition snake2 = root.addOrReplaceChild("snake2", CubeListBuilder.create()
                .texOffs(163, 86).mirror(false).addBox(-1.2333f, -1.2333f, -0.4f, 2.4667f, 2.4667f, 5.9552f), PartPose.offsetAndRotation(-2.8609f, 11, -17.1993f, 0.0485f, -1.18f, 0));
        PartDefinition snake3 = root.addOrReplaceChild("snake3", CubeListBuilder.create()
                .texOffs(119, 86).mirror(false).addBox(-1.45f, -1.45f, -0.4f, 2.9f, 2.9f, 6.1409f), PartPose.offsetAndRotation(-7.6219f, 10.75f, -15.2381f, 0.0468f, -0.8326f, 0));
        PartDefinition snake4 = root.addOrReplaceChild("snake4", CubeListBuilder.create()
                .texOffs(52, 86).mirror(false).addBox(-1.6667f, -1.6667f, -0.4f, 3.3333f, 3.3333f, 6.3485f), PartPose.offsetAndRotation(-11.568f, 10.5f, -11.6477f, 0.0451f, -0.5107f, 0));
        PartDefinition snake5 = root.addOrReplaceChild("snake5", CubeListBuilder.create()
                .texOffs(52, 86).mirror(false).addBox(-1.8833f, -1.8833f, -0.4f, 3.7667f, 3.7667f, 6.4937f), PartPose.offsetAndRotation(-14.2774f, 10.25f, -6.8122f, 0.0439f, -0.2095f, 0));
        PartDefinition snake6 = root.addOrReplaceChild("snake6", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.5221f), PartPose.offsetAndRotation(-15.4605f, 10, -1.2484f, 0.0437f, 0.0823f, 0));
        PartDefinition snake7 = root.addOrReplaceChild("snake7", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.4234f), PartPose.offsetAndRotation(-14.9908f, 9.75f, 4.4488f, 0.0445f, 0.3778f, 0));
        PartDefinition snake8 = root.addOrReplaceChild("snake8", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.2338f), PartPose.offsetAndRotation(-12.9184f, 9.5f, 9.6705f, 0.046f, 0.6896f, 0));
        PartDefinition snake9 = root.addOrReplaceChild("snake9", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.0273f), PartPose.offsetAndRotation(-9.465f, 9.25f, 13.8583f, 0.0478f, 1.0259f, 0));
        PartDefinition snake10 = root.addOrReplaceChild("snake10", CubeListBuilder.create()
                .texOffs(75, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 5.8927f), PartPose.offsetAndRotation(-4.9998f, 9, 16.5646f, 0.0491f, 1.3858f, 0));
        PartDefinition snake11 = root.addOrReplaceChild("snake11", CubeListBuilder.create()
                .texOffs(75, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 5.8927f), PartPose.offsetAndRotation(-0, 8.75f, 17.5f, 0.0491f, 1.7558f, 0));
        PartDefinition snake12 = root.addOrReplaceChild("snake12", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.0273f), PartPose.offsetAndRotation(4.9998f, 8.5f, 16.5646f, 0.0478f, 2.1157f, 0));
        PartDefinition snake13 = root.addOrReplaceChild("snake13", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.2338f), PartPose.offsetAndRotation(9.465f, 8.25f, 13.8583f, 0.046f, 2.452f, 0));
        PartDefinition snake14 = root.addOrReplaceChild("snake14", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.4234f), PartPose.offsetAndRotation(12.9184f, 8, 9.6705f, 0.0445f, 2.7638f, 0));
        PartDefinition snake15 = root.addOrReplaceChild("snake15", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.5221f), PartPose.offsetAndRotation(14.9908f, 7.75f, 4.4488f, 0.0437f, 3.0593f, 0));
        PartDefinition snake16 = root.addOrReplaceChild("snake16", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.4937f), PartPose.offsetAndRotation(15.4605f, 7.5f, -1.2484f, 0.0439f, -2.9321f, 0));
        PartDefinition snake17 = root.addOrReplaceChild("snake17", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.3485f), PartPose.offsetAndRotation(14.2774f, 7.25f, -6.8122f, 0.0451f, -2.6309f, 0));
        PartDefinition snake18 = root.addOrReplaceChild("snake18", CubeListBuilder.create()
                .texOffs(27, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 6.1409f), PartPose.offsetAndRotation(11.568f, 7, -11.6477f, 0.0468f, -2.309f, 0));
        PartDefinition snake19 = root.addOrReplaceChild("snake19", CubeListBuilder.create()
                .texOffs(75, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 5.9552f), PartPose.offsetAndRotation(7.6219f, 6.75f, -15.2381f, 0.0485f, -1.9616f, 0));
        PartDefinition snake20 = root.addOrReplaceChild("snake20", CubeListBuilder.create()
                .texOffs(75, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 5.8744f), PartPose.offsetAndRotation(2.8609f, 6.5f, -17.1993f, 0.0493f, -1.595f, 0));
        PartDefinition snake21 = root.addOrReplaceChild("snake21", CubeListBuilder.create()
                .texOffs(75, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 5.9372f), PartPose.offsetAndRotation(-2.2059f, 6.25f, -17.3219f, 0.0487f, -1.2271f, 0));
        PartDefinition snake22 = root.addOrReplaceChild("snake22", CubeListBuilder.create()
                .texOffs(192, 35).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 17.1201f), PartPose.offsetAndRotation(-7.0369f, 6, -15.5926f, 0.1849f, 1.5962f, 0));
        PartDefinition snake23 = root.addOrReplaceChild("snake23", CubeListBuilder.create()
                .texOffs(205, 66).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 8.233f), PartPose.offsetAndRotation(9, 3, -16, 1.2278f, -2.4981f, 0));
        PartDefinition snake24 = root.addOrReplaceChild("snake24", CubeListBuilder.create()
                .texOffs(0, 86).mirror(false).addBox(-2.1f, -2.1f, -0.4f, 4.2f, 4.2f, 7.5268f), PartPose.offsetAndRotation(7.5f, -4, -18, 1.1016f, -1.4056f, 0));
        PartDefinition snakeHead = root.addOrReplaceChild("snake_head", CubeListBuilder.create()
                .texOffs(140, 86).mirror(false).addBox(-2.4f, -2, -0.5f, 4.8f, 3.6f, 5.5f)
                .texOffs(0, 100).mirror(false).addBox(-1.8f, -1.2f, 5, 3.6f, 2.2f, 2.6f)
                .texOffs(15, 100).mirror(false).addBox(-4.6f, -2.2f, -1.5f, 9.2f, 4.6f, 1)
                .texOffs(69, 100).mirror(false).addBox(-1.6f, 1.2f, 6.2f, 0.6f, 1.6f, 0.6f)
                .texOffs(69, 100).mirror(false).addBox(1, 1.2f, 6.2f, 0.6f, 1.6f, 0.6f), PartPose.offsetAndRotation(4.5f, -10, -17.5f, 0.2016f, -1.4056f, 0));
        PartDefinition snakeJaw = snakeHead.addOrReplaceChild("snake_jaw", CubeListBuilder.create()
                .texOffs(199, 86).mirror(false).addBox(-1.8f, 0, 0, 3.6f, 1, 7)
                .texOffs(69, 100).mirror(false).addBox(-1.4f, -1.2f, 6, 0.5f, 1.2f, 0.5f)
                .texOffs(69, 100).mirror(false).addBox(0.9f, -1.2f, 6, 0.5f, 1.2f, 0.5f), PartPose.offsetAndRotation(0, 1.6f, 0.5f, 0.55f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<TrueFormEntity> {
        private final ModelPart root, neck, snakeHead, snakeJaw;
        private final ModelPart[] legs = new ModelPart[4];

        public Model(ModelPart root) {
            this.root = root;
            this.neck = root.getChild("neck");
            this.snakeHead = root.getChild("snake_head");
            this.snakeJaw = snakeHead.getChild("snake_jaw");
            String[] names = {"right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg"};
            for (int i = 0; i < 4; i++) legs[i] = root.getChild(names[i]);
        }

        @Override
        public void setupAnim(TrueFormEntity e, float swing, float amount, float age, float yaw, float pitch) {
            neck.yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            neck.xRot = neck.getInitialPose().xRot + pitch * Mth.DEG_TO_RAD * 0.3f;
            // a slow, heavy tread: the legs of a diagonal together
            for (int i = 0; i < 4; i++) {
                boolean diagonal = i == 0 || i == 3;
                legs[i].xRot = Mth.cos(swing * 0.4f + (diagonal ? 0 : Mth.PI)) * 0.45f * amount;
            }
            // the serpent sways over him, watching, ready to strike
            var rest = snakeHead.getInitialPose();
            snakeHead.yRot = rest.yRot + Mth.sin(age * 0.07f) * 0.35f;
            snakeHead.xRot = rest.xRot + Mth.sin(age * 0.11f) * 0.12f;
            snakeJaw.xRot = 0.55f + Mth.sin(age * 0.2f) * 0.1f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
