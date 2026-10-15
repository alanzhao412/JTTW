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

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "vermilion_bird"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/vermilion_bird.png");
    private static final RenderType GLOW = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/vermilion_bird_glow.png"));
    public static final float SCALE = 4.0f;

    public VermilionBirdRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 2.4f);
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
                .texOffs(232, 43).mirror(false).addBox(-4, -3.5f, -10, 8, 8, 2)
                .texOffs(206, 25).mirror(false).addBox(-5, -4.5f, -8, 10, 10, 3)
                .texOffs(37, 25).mirror(false).addBox(-6, -5, -5, 12, 10.5f, 5)
                .texOffs(0, 43).mirror(false).addBox(-5.5f, -4.5f, 0, 11, 9, 4)
                .texOffs(207, 43).mirror(false).addBox(-4.5f, -4, 4, 9, 8, 3)
                .texOffs(0, 57).mirror(false).addBox(-3.5f, -3.5f, 7, 7, 6.5f, 3)
                .texOffs(0, 68).mirror(false).addBox(-2.4f, -3, 10, 4.8f, 4.5f, 2)
                .texOffs(31, 43).mirror(false).addBox(-3.5f, 4.5f, -7, 7, 2, 11)
                .texOffs(0, 25).mirror(false).addBox(-1.2f, -6.2f, -6, 2.4f, 1.4f, 15), PartPose.offsetAndRotation(0, -2, 0, -0.2f, 0, 0));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(68, 43).mirror(false).addBox(-3, -3, -6, 6, 6, 7), PartPose.offsetAndRotation(0, -3, -7, -0.55f, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(95, 43).mirror(false).addBox(-3.2f, -3.2f, -6, 6.4f, 6, 6.5f)
                .texOffs(15, 68).mirror(false).addBox(-3.7f, 0.5f, -1.5f, 1, 3, 4)
                .texOffs(15, 68).mirror(false).addBox(2.7f, 0.5f, -1.5f, 1, 3, 4)
                .texOffs(153, 57).mirror(false).addBox(-1.4f, -1, -10.5f, 2.8f, 2.6f, 5)
                .texOffs(178, 76).mirror(false).addBox(-1, 1, -11.5f, 2, 1.6f, 1.6f), PartPose.offsetAndRotation(0, -1, -5.5f, 0.75f, 0, 0));
        PartDefinition legs = body.addOrReplaceChild("legs", CubeListBuilder.create()
                .texOffs(26, 68).mirror(false).addBox(-3.4f, -1, -1.5f, 2.8f, 4, 3)
                .texOffs(26, 68).mirror(false).addBox(0.6f, -1, -1.5f, 2.8f, 4, 3)
                .texOffs(39, 68).mirror(false).addBox(-2.8f, 3, -0.8f, 1.6f, 5, 1.6f)
                .texOffs(39, 68).mirror(false).addBox(1.2f, 3, -0.8f, 1.6f, 5, 1.6f), PartPose.offsetAndRotation(0, 5, 3, 0.9f, 0, 0));
        PartDefinition mantle00 = body.addOrReplaceChild("mantle0_0", CubeListBuilder.create()
                .texOffs(182, 68).mirror(false).addBox(-1.9667f, -0.35f, 0, 3.9333f, 0.7f, 4.2f), PartPose.offsetAndRotation(-3.125f, -4.4f, -6, -0.28f, 0, -0.35f));
        PartDefinition mantle01 = body.addOrReplaceChild("mantle0_1", CubeListBuilder.create()
                .texOffs(182, 68).mirror(false).addBox(-1.9667f, -0.35f, 0, 3.9333f, 0.7f, 4.2f), PartPose.offsetAndRotation(0, -5.2f, -6, -0.28f, 0, 0));
        PartDefinition mantle02 = body.addOrReplaceChild("mantle0_2", CubeListBuilder.create()
                .texOffs(182, 68).mirror(false).addBox(-1.9667f, -0.35f, 0, 3.9333f, 0.7f, 4.2f), PartPose.offsetAndRotation(3.125f, -4.4f, -6, -0.28f, 0, 0.35f));
        PartDefinition flank0R0 = body.addOrReplaceChild("flank0_r0", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(-5.1f, -2.2f, -6, -0.15f, -0.22f, -0.25f));
        PartDefinition flank0R1 = body.addOrReplaceChild("flank0_r1", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(-5.1f, 1.2f, -6, -0.15f, -0.22f, -0.4f));
        PartDefinition flank0L0 = body.addOrReplaceChild("flank0_l0", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(5.1f, -2.2f, -6, -0.15f, 0.22f, 0.25f));
        PartDefinition flank0L1 = body.addOrReplaceChild("flank0_l1", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(5.1f, 1.2f, -6, -0.15f, 0.22f, 0.4f));
        PartDefinition mantle10 = body.addOrReplaceChild("mantle1_0", CubeListBuilder.create()
                .texOffs(201, 68).mirror(false).addBox(-2.1333f, -0.35f, 0, 4.2667f, 0.7f, 4.2f), PartPose.offsetAndRotation(-3.4375f, -4.4f, -3, -0.28f, 0, -0.35f));
        PartDefinition mantle11 = body.addOrReplaceChild("mantle1_1", CubeListBuilder.create()
                .texOffs(201, 68).mirror(false).addBox(-2.1333f, -0.35f, 0, 4.2667f, 0.7f, 4.2f), PartPose.offsetAndRotation(0, -5.2f, -3, -0.28f, 0, 0));
        PartDefinition mantle12 = body.addOrReplaceChild("mantle1_2", CubeListBuilder.create()
                .texOffs(201, 68).mirror(false).addBox(-2.1333f, -0.35f, 0, 4.2667f, 0.7f, 4.2f), PartPose.offsetAndRotation(3.4375f, -4.4f, -3, -0.28f, 0, 0.35f));
        PartDefinition flank1R0 = body.addOrReplaceChild("flank1_r0", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(-5.6f, -2.2f, -3, -0.15f, -0.22f, -0.25f));
        PartDefinition flank1R1 = body.addOrReplaceChild("flank1_r1", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(-5.6f, 1.2f, -3, -0.15f, -0.22f, -0.4f));
        PartDefinition flank1L0 = body.addOrReplaceChild("flank1_l0", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(5.6f, -2.2f, -3, -0.15f, 0.22f, 0.25f));
        PartDefinition flank1L1 = body.addOrReplaceChild("flank1_l1", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(5.6f, 1.2f, -3, -0.15f, 0.22f, 0.4f));
        PartDefinition mantle20 = body.addOrReplaceChild("mantle2_0", CubeListBuilder.create()
                .texOffs(201, 68).mirror(false).addBox(-2.05f, -0.35f, 0, 4.1f, 0.7f, 4.2f), PartPose.offsetAndRotation(-3.2812f, -4.4f, 0, -0.28f, 0, -0.35f));
        PartDefinition mantle21 = body.addOrReplaceChild("mantle2_1", CubeListBuilder.create()
                .texOffs(201, 68).mirror(false).addBox(-2.05f, -0.35f, 0, 4.1f, 0.7f, 4.2f), PartPose.offsetAndRotation(0, -5.2f, 0, -0.28f, 0, 0));
        PartDefinition mantle22 = body.addOrReplaceChild("mantle2_2", CubeListBuilder.create()
                .texOffs(201, 68).mirror(false).addBox(-2.05f, -0.35f, 0, 4.1f, 0.7f, 4.2f), PartPose.offsetAndRotation(3.2812f, -4.4f, 0, -0.28f, 0, 0.35f));
        PartDefinition flank2R0 = body.addOrReplaceChild("flank2_r0", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(-5.35f, -2.2f, 0, -0.15f, -0.22f, -0.25f));
        PartDefinition flank2R1 = body.addOrReplaceChild("flank2_r1", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(-5.35f, 1.2f, 0, -0.15f, -0.22f, -0.4f));
        PartDefinition flank2L0 = body.addOrReplaceChild("flank2_l0", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(5.35f, -2.2f, 0, -0.15f, 0.22f, 0.25f));
        PartDefinition flank2L1 = body.addOrReplaceChild("flank2_l1", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(5.35f, 1.2f, 0, -0.15f, 0.22f, 0.4f));
        PartDefinition mantle30 = body.addOrReplaceChild("mantle3_0", CubeListBuilder.create()
                .texOffs(182, 68).mirror(false).addBox(-1.8833f, -0.35f, 0, 3.7667f, 0.7f, 4.2f), PartPose.offsetAndRotation(-2.9688f, -4.4f, 3, -0.28f, 0, -0.35f));
        PartDefinition mantle31 = body.addOrReplaceChild("mantle3_1", CubeListBuilder.create()
                .texOffs(182, 68).mirror(false).addBox(-1.8833f, -0.35f, 0, 3.7667f, 0.7f, 4.2f), PartPose.offsetAndRotation(0, -5.2f, 3, -0.28f, 0, 0));
        PartDefinition mantle32 = body.addOrReplaceChild("mantle3_2", CubeListBuilder.create()
                .texOffs(182, 68).mirror(false).addBox(-1.8833f, -0.35f, 0, 3.7667f, 0.7f, 4.2f), PartPose.offsetAndRotation(2.9688f, -4.4f, 3, -0.28f, 0, 0.35f));
        PartDefinition flank3R0 = body.addOrReplaceChild("flank3_r0", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(-4.85f, -2.2f, 3, -0.15f, -0.22f, -0.25f));
        PartDefinition flank3R1 = body.addOrReplaceChild("flank3_r1", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(-4.85f, 1.2f, 3, -0.15f, -0.22f, -0.4f));
        PartDefinition flank3L0 = body.addOrReplaceChild("flank3_l0", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(4.85f, -2.2f, 3, -0.15f, 0.22f, 0.25f));
        PartDefinition flank3L1 = body.addOrReplaceChild("flank3_l1", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(4.85f, 1.2f, 3, -0.15f, 0.22f, 0.4f));
        PartDefinition mantle40 = body.addOrReplaceChild("mantle4_0", CubeListBuilder.create()
                .texOffs(182, 68).mirror(false).addBox(-1.6333f, -0.35f, 0, 3.2667f, 0.7f, 4.2f), PartPose.offsetAndRotation(-2.5f, -4.4f, 6, -0.28f, 0, -0.35f));
        PartDefinition mantle41 = body.addOrReplaceChild("mantle4_1", CubeListBuilder.create()
                .texOffs(182, 68).mirror(false).addBox(-1.6333f, -0.35f, 0, 3.2667f, 0.7f, 4.2f), PartPose.offsetAndRotation(0, -5.2f, 6, -0.28f, 0, 0));
        PartDefinition mantle42 = body.addOrReplaceChild("mantle4_2", CubeListBuilder.create()
                .texOffs(182, 68).mirror(false).addBox(-1.6333f, -0.35f, 0, 3.2667f, 0.7f, 4.2f), PartPose.offsetAndRotation(2.5f, -4.4f, 6, -0.28f, 0, 0.35f));
        PartDefinition flank4R0 = body.addOrReplaceChild("flank4_r0", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(-4.1f, -2.2f, 6, -0.15f, -0.22f, -0.25f));
        PartDefinition flank4R1 = body.addOrReplaceChild("flank4_r1", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(-4.1f, 1.2f, 6, -0.15f, -0.22f, -0.4f));
        PartDefinition flank4L0 = body.addOrReplaceChild("flank4_l0", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(4.1f, -2.2f, 6, -0.15f, 0.22f, 0.25f));
        PartDefinition flank4L1 = body.addOrReplaceChild("flank4_l1", CubeListBuilder.create()
                .texOffs(117, 57).mirror(false).addBox(-0.35f, -1.6f, 0, 0.7f, 3.4f, 4.4f), PartPose.offsetAndRotation(4.1f, 1.2f, 6, -0.15f, 0.22f, 0.4f));
        PartDefinition breastrow00 = body.addOrReplaceChild("breastrow0_0", CubeListBuilder.create()
                .texOffs(135, 76).mirror(false).addBox(-1.3667f, -0.3f, -3.2f, 2.7333f, 0.6f, 3.4f), PartPose.offsetAndRotation(-2.2581f, -2.5f, -9.55f, 0.55f, -0.25f, 0));
        PartDefinition breastrow01 = body.addOrReplaceChild("breastrow0_1", CubeListBuilder.create()
                .texOffs(135, 76).mirror(false).addBox(-1.3667f, -0.3f, -3.2f, 2.7333f, 0.6f, 3.4f), PartPose.offsetAndRotation(0, -2.5f, -9.55f, 0.55f, 0, 0));
        PartDefinition breastrow02 = body.addOrReplaceChild("breastrow0_2", CubeListBuilder.create()
                .texOffs(135, 76).mirror(false).addBox(-1.3667f, -0.3f, -3.2f, 2.7333f, 0.6f, 3.4f), PartPose.offsetAndRotation(2.2581f, -2.5f, -9.55f, 0.55f, 0.25f, 0));
        PartDefinition breastrow10 = body.addOrReplaceChild("breastrow1_0", CubeListBuilder.create()
                .texOffs(150, 76).mirror(false).addBox(-1.5333f, -0.3f, -3.2f, 3.0667f, 0.6f, 3.4f), PartPose.offsetAndRotation(-2.5806f, 0, -10.05f, 0.55f, -0.25f, 0));
        PartDefinition breastrow11 = body.addOrReplaceChild("breastrow1_1", CubeListBuilder.create()
                .texOffs(150, 76).mirror(false).addBox(-1.5333f, -0.3f, -3.2f, 3.0667f, 0.6f, 3.4f), PartPose.offsetAndRotation(0, 0, -10.05f, 0.55f, 0, 0));
        PartDefinition breastrow12 = body.addOrReplaceChild("breastrow1_2", CubeListBuilder.create()
                .texOffs(150, 76).mirror(false).addBox(-1.5333f, -0.3f, -3.2f, 3.0667f, 0.6f, 3.4f), PartPose.offsetAndRotation(2.5806f, 0, -10.05f, 0.55f, 0.25f, 0));
        PartDefinition breastrow20 = body.addOrReplaceChild("breastrow2_0", CubeListBuilder.create()
                .texOffs(135, 76).mirror(false).addBox(-1.45f, -0.3f, -3.2f, 2.9f, 0.6f, 3.4f), PartPose.offsetAndRotation(-2.4194f, 2.5f, -10.05f, 0.55f, -0.25f, 0));
        PartDefinition breastrow21 = body.addOrReplaceChild("breastrow2_1", CubeListBuilder.create()
                .texOffs(135, 76).mirror(false).addBox(-1.45f, -0.3f, -3.2f, 2.9f, 0.6f, 3.4f), PartPose.offsetAndRotation(0, 2.5f, -10.05f, 0.55f, 0, 0));
        PartDefinition breastrow22 = body.addOrReplaceChild("breastrow2_2", CubeListBuilder.create()
                .texOffs(135, 76).mirror(false).addBox(-1.45f, -0.3f, -3.2f, 2.9f, 0.6f, 3.4f), PartPose.offsetAndRotation(2.4194f, 2.5f, -10.05f, 0.55f, 0.25f, 0));
        PartDefinition breastrow30 = body.addOrReplaceChild("breastrow3_0", CubeListBuilder.create()
                .texOffs(135, 76).mirror(false).addBox(-1.2f, -0.3f, -3.2f, 2.4f, 0.6f, 3.4f), PartPose.offsetAndRotation(-1.9355f, 5, -9.55f, 0.55f, -0.25f, 0));
        PartDefinition breastrow31 = body.addOrReplaceChild("breastrow3_1", CubeListBuilder.create()
                .texOffs(135, 76).mirror(false).addBox(-1.2f, -0.3f, -3.2f, 2.4f, 0.6f, 3.4f), PartPose.offsetAndRotation(0, 5, -9.55f, 0.55f, 0, 0));
        PartDefinition breastrow32 = body.addOrReplaceChild("breastrow3_2", CubeListBuilder.create()
                .texOffs(135, 76).mirror(false).addBox(-1.2f, -0.3f, -3.2f, 2.4f, 0.6f, 3.4f), PartPose.offsetAndRotation(1.9355f, 5, -9.55f, 0.55f, 0.25f, 0));
        PartDefinition shoulderR0 = body.addOrReplaceChild("shoulder_r0", CubeListBuilder.create()
                .texOffs(222, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 5), PartPose.offsetAndRotation(-4.5f, -4.8f, -4, -0.35f, -0.5f, -0.7f));
        PartDefinition shoulderR1 = body.addOrReplaceChild("shoulder_r1", CubeListBuilder.create()
                .texOffs(48, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(-5.1f, -4.2f, -2, -0.35f, -0.5f, -0.7f));
        PartDefinition shoulderR2 = body.addOrReplaceChild("shoulder_r2", CubeListBuilder.create()
                .texOffs(170, 57).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 7), PartPose.offsetAndRotation(-5.7f, -3.6f, 0, -0.35f, -0.5f, -0.7f));
        PartDefinition shoulderL0 = body.addOrReplaceChild("shoulder_l0", CubeListBuilder.create()
                .texOffs(222, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 5), PartPose.offsetAndRotation(4.5f, -4.8f, -4, -0.35f, 0.5f, 0.7f));
        PartDefinition shoulderL1 = body.addOrReplaceChild("shoulder_l1", CubeListBuilder.create()
                .texOffs(48, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(5.1f, -4.2f, -2, -0.35f, 0.5f, 0.7f));
        PartDefinition shoulderL2 = body.addOrReplaceChild("shoulder_l2", CubeListBuilder.create()
                .texOffs(170, 57).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 7), PartPose.offsetAndRotation(5.7f, -3.6f, 0, -0.35f, 0.5f, 0.7f));
        PartDefinition backflame0 = body.addOrReplaceChild("backflame0", CubeListBuilder.create()
                .texOffs(191, 57).mirror(false).addBox(-0.7f, -0.4f, 0, 1.4f, 0.8f, 7), PartPose.offsetAndRotation(0, -6, -4, -1.15f, 0, 0));
        PartDefinition backflame0End = backflame0.addOrReplaceChild("backflame0_end", CubeListBuilder.create()
                .texOffs(239, 68).mirror(false).addBox(-0.5f, -0.35f, 0, 1, 0.7f, 4.2f), PartPose.offsetAndRotation(0, 0, 7, 0.9f, 0, 0));
        PartDefinition backflame1 = body.addOrReplaceChild("backflame1", CubeListBuilder.create()
                .texOffs(21, 57).mirror(false).addBox(-0.7f, -0.4f, 0, 1.4f, 0.8f, 9), PartPose.offsetAndRotation(0, -6, 0, -1.15f, 0, 0));
        PartDefinition backflame1End = backflame1.addOrReplaceChild("backflame1_end", CubeListBuilder.create()
                .texOffs(67, 68).mirror(false).addBox(-0.5f, -0.35f, 0, 1, 0.7f, 5.4f), PartPose.offsetAndRotation(0, 0, 9, 0.9f, 0, 0));
        PartDefinition backflame2 = body.addOrReplaceChild("backflame2", CubeListBuilder.create()
                .texOffs(191, 57).mirror(false).addBox(-0.7f, -0.4f, 0, 1.4f, 0.8f, 7), PartPose.offsetAndRotation(0, -6, 4, -1.15f, 0, 0));
        PartDefinition backflame2End = backflame2.addOrReplaceChild("backflame2_end", CubeListBuilder.create()
                .texOffs(239, 68).mirror(false).addBox(-0.5f, -0.35f, 0, 1, 0.7f, 4.2f), PartPose.offsetAndRotation(0, 0, 7, 0.9f, 0, 0));
        PartDefinition backflame3 = body.addOrReplaceChild("backflame3", CubeListBuilder.create()
                .texOffs(0, 76).mirror(false).addBox(-0.7f, -0.4f, 0, 1.4f, 0.8f, 5), PartPose.offsetAndRotation(0, -6, 7, -1.15f, 0, 0));
        PartDefinition backflame3End = backflame3.addOrReplaceChild("backflame3_end", CubeListBuilder.create()
                .texOffs(187, 76).mirror(false).addBox(-0.5f, -0.35f, 0, 1, 0.7f, 3), PartPose.offsetAndRotation(0, 0, 5, 0.9f, 0, 0));
        PartDefinition hangR0 = body.addOrReplaceChild("hang_r0", CubeListBuilder.create()
                .texOffs(48, 68).mirror(false).addBox(-1.1f, -0.3f, 0, 2.2f, 0.6f, 6), PartPose.offsetAndRotation(-3.2f, 5, -3, 0.55f, 0, -0.12f));
        PartDefinition hangR1 = body.addOrReplaceChild("hang_r1", CubeListBuilder.create()
                .texOffs(170, 57).mirror(false).addBox(-1.1f, -0.3f, 0, 2.2f, 0.6f, 7), PartPose.offsetAndRotation(-2.9f, 5, 0, 0.6f, 0, -0.12f));
        PartDefinition hangR2 = body.addOrReplaceChild("hang_r2", CubeListBuilder.create()
                .texOffs(130, 57).mirror(false).addBox(-1.1f, -0.3f, 0, 2.2f, 0.6f, 8), PartPose.offsetAndRotation(-2.6f, 5, 3, 0.65f, 0, -0.12f));
        PartDefinition hangR3 = body.addOrReplaceChild("hang_r3", CubeListBuilder.create()
                .texOffs(44, 57).mirror(false).addBox(-1.1f, -0.3f, 0, 2.2f, 0.6f, 9), PartPose.offsetAndRotation(-2.3f, 5, 6, 0.7f, 0, -0.12f));
        PartDefinition hangL0 = body.addOrReplaceChild("hang_l0", CubeListBuilder.create()
                .texOffs(48, 68).mirror(false).addBox(-1.1f, -0.3f, 0, 2.2f, 0.6f, 6), PartPose.offsetAndRotation(3.2f, 5, -3, 0.55f, 0, 0.12f));
        PartDefinition hangL1 = body.addOrReplaceChild("hang_l1", CubeListBuilder.create()
                .texOffs(170, 57).mirror(false).addBox(-1.1f, -0.3f, 0, 2.2f, 0.6f, 7), PartPose.offsetAndRotation(2.9f, 5, 0, 0.6f, 0, 0.12f));
        PartDefinition hangL2 = body.addOrReplaceChild("hang_l2", CubeListBuilder.create()
                .texOffs(130, 57).mirror(false).addBox(-1.1f, -0.3f, 0, 2.2f, 0.6f, 8), PartPose.offsetAndRotation(2.6f, 5, 3, 0.65f, 0, 0.12f));
        PartDefinition hangL3 = body.addOrReplaceChild("hang_l3", CubeListBuilder.create()
                .texOffs(44, 57).mirror(false).addBox(-1.1f, -0.3f, 0, 2.2f, 0.6f, 9), PartPose.offsetAndRotation(2.3f, 5, 6, 0.7f, 0, 0.12f));
        PartDefinition ruff0 = neck.addOrReplaceChild("ruff0", CubeListBuilder.create()
                .texOffs(82, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(0, -3.4f, -1, 0.85f, 0, 0));
        PartDefinition ruff1 = neck.addOrReplaceChild("ruff1", CubeListBuilder.create()
                .texOffs(82, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(1.9985f, -2.7507f, -1, 0.7545f, 0.2939f, 0));
        PartDefinition ruff2 = neck.addOrReplaceChild("ruff2", CubeListBuilder.create()
                .texOffs(82, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(3.2336f, -1.0507f, -1, 0.5045f, 0.4755f, 0));
        PartDefinition ruff3 = neck.addOrReplaceChild("ruff3", CubeListBuilder.create()
                .texOffs(82, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(3.2336f, 1.0507f, -1, 0.1955f, 0.4755f, 0));
        PartDefinition ruff4 = neck.addOrReplaceChild("ruff4", CubeListBuilder.create()
                .texOffs(82, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(1.9985f, 2.7507f, -1, -0.0545f, 0.2939f, 0));
        PartDefinition ruff5 = neck.addOrReplaceChild("ruff5", CubeListBuilder.create()
                .texOffs(82, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(0, 3.4f, -1, -0.15f, 0, 0));
        PartDefinition ruff6 = neck.addOrReplaceChild("ruff6", CubeListBuilder.create()
                .texOffs(82, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(-1.9985f, 2.7507f, -1, -0.0545f, -0.2939f, 0));
        PartDefinition ruff7 = neck.addOrReplaceChild("ruff7", CubeListBuilder.create()
                .texOffs(82, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(-3.2336f, 1.0507f, -1, 0.1955f, -0.4755f, 0));
        PartDefinition ruff8 = neck.addOrReplaceChild("ruff8", CubeListBuilder.create()
                .texOffs(82, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(-3.2336f, -1.0507f, -1, 0.5045f, -0.4755f, 0));
        PartDefinition ruff9 = neck.addOrReplaceChild("ruff9", CubeListBuilder.create()
                .texOffs(82, 68).mirror(false).addBox(-1.2f, -0.3f, 0, 2.4f, 0.6f, 6), PartPose.offsetAndRotation(-1.9985f, -2.7507f, -1, 0.7545f, -0.2939f, 0));
        PartDefinition crest0 = head.addOrReplaceChild("crest0", CubeListBuilder.create()
                .texOffs(210, 57).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 7), PartPose.offsetAndRotation(-2, -3, -2, 0.75f, -0.3333f, 0));
        PartDefinition crest0End = crest0.addOrReplaceChild("crest0_end", CubeListBuilder.create()
                .texOffs(167, 76).mirror(false).addBox(-0.4f, -0.35f, 0, 0.8f, 0.7f, 3.5f), PartPose.offsetAndRotation(0, 0, 7, -0.6f, 0, 0));
        PartDefinition crest1 = head.addOrReplaceChild("crest1", CubeListBuilder.create()
                .texOffs(69, 57).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 9), PartPose.offsetAndRotation(-1, -3, -2, 0.55f, -0.1667f, 0));
        PartDefinition crest1End = crest1.addOrReplaceChild("crest1_end", CubeListBuilder.create()
                .texOffs(239, 68).mirror(false).addBox(-0.4f, -0.35f, 0, 0.8f, 0.7f, 4.5f), PartPose.offsetAndRotation(0, 0, 9, -0.6f, 0, 0));
        PartDefinition crest2 = head.addOrReplaceChild("crest2", CubeListBuilder.create()
                .texOffs(124, 43).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 11), PartPose.offsetAndRotation(0, -3, -2, 0.4f, 0, 0));
        PartDefinition crest2End = crest2.addOrReplaceChild("crest2_end", CubeListBuilder.create()
                .texOffs(67, 68).mirror(false).addBox(-0.4f, -0.35f, 0, 0.8f, 0.7f, 5.5f), PartPose.offsetAndRotation(0, 0, 11, -0.6f, 0, 0));
        PartDefinition crest3 = head.addOrReplaceChild("crest3", CubeListBuilder.create()
                .texOffs(69, 57).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 9), PartPose.offsetAndRotation(1, -3, -2, 0.55f, 0.1667f, 0));
        PartDefinition crest3End = crest3.addOrReplaceChild("crest3_end", CubeListBuilder.create()
                .texOffs(239, 68).mirror(false).addBox(-0.4f, -0.35f, 0, 0.8f, 0.7f, 4.5f), PartPose.offsetAndRotation(0, 0, 9, -0.6f, 0, 0));
        PartDefinition crest4 = head.addOrReplaceChild("crest4", CubeListBuilder.create()
                .texOffs(210, 57).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 7), PartPose.offsetAndRotation(2, -3, -2, 0.75f, 0.3333f, 0));
        PartDefinition crest4End = crest4.addOrReplaceChild("crest4_end", CubeListBuilder.create()
                .texOffs(167, 76).mirror(false).addBox(-0.4f, -0.35f, 0, 0.8f, 0.7f, 3.5f), PartPose.offsetAndRotation(0, 0, 7, -0.6f, 0, 0));
        PartDefinition crest5 = head.addOrReplaceChild("crest5", CubeListBuilder.create()
                .texOffs(101, 68).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 6), PartPose.offsetAndRotation(-0.5f, -3, -2, 0.95f, -0.0833f, 0));
        PartDefinition crest5End = crest5.addOrReplaceChild("crest5_end", CubeListBuilder.create()
                .texOffs(187, 76).mirror(false).addBox(-0.4f, -0.35f, 0, 0.8f, 0.7f, 3), PartPose.offsetAndRotation(0, 0, 6, -0.6f, 0, 0));
        PartDefinition crest6 = head.addOrReplaceChild("crest6", CubeListBuilder.create()
                .texOffs(101, 68).mirror(false).addBox(-0.5f, -0.4f, 0, 1, 0.8f, 6), PartPose.offsetAndRotation(0.5f, -3, -2, 0.95f, 0.0833f, 0));
        PartDefinition crest6End = crest6.addOrReplaceChild("crest6_end", CubeListBuilder.create()
                .texOffs(187, 76).mirror(false).addBox(-0.4f, -0.35f, 0, 0.8f, 0.7f, 3), PartPose.offsetAndRotation(0, 0, 6, -0.6f, 0, 0));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(15, 76).mirror(false).addBox(-12, -1.6f, -5, 12, 2.6f, 3)
                .texOffs(116, 68).mirror(false).addBox(-12, -0.9f, -2.2f, 12, 1.8f, 5)
                .texOffs(46, 76).mirror(false).addBox(-12, -0.4f, 2.6f, 12, 1.2f, 4), PartPose.offsetAndRotation(-4.5f, -3.5f, -3, -0.2f, -0.3f, 0.72f));
        PartDefinition rightQ00 = rightWing.addOrReplaceChild("right_q0_0", CubeListBuilder.create()
                .texOffs(149, 43).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 11), PartPose.offsetAndRotation(-1.2f, 0, 6.2f, 0.1f, -0, 0));
        PartDefinition rightQ01 = rightWing.addOrReplaceChild("right_q0_1", CubeListBuilder.create()
                .texOffs(149, 43).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 11), PartPose.offsetAndRotation(-3.6f, 0, 6.2f, 0.1f, -0.04f, 0));
        PartDefinition rightQ02 = rightWing.addOrReplaceChild("right_q0_2", CubeListBuilder.create()
                .texOffs(149, 43).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 11), PartPose.offsetAndRotation(-6, 0, 6.2f, 0.1f, -0.08f, 0));
        PartDefinition rightQ03 = rightWing.addOrReplaceChild("right_q0_3", CubeListBuilder.create()
                .texOffs(149, 43).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 11), PartPose.offsetAndRotation(-8.4f, 0, 6.2f, 0.1f, -0.12f, 0));
        PartDefinition rightQ04 = rightWing.addOrReplaceChild("right_q0_4", CubeListBuilder.create()
                .texOffs(149, 43).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 11), PartPose.offsetAndRotation(-10.8f, 0, 6.2f, 0.1f, -0.16f, 0));
        PartDefinition rightWingMid = rightWing.addOrReplaceChild("right_wing_mid", CubeListBuilder.create()
                .texOffs(15, 76).mirror(false).addBox(-12, -1.6f, -5, 12, 2.6f, 3)
                .texOffs(116, 68).mirror(false).addBox(-12, -0.9f, -2.2f, 12, 1.8f, 5)
                .texOffs(46, 76).mirror(false).addBox(-12, -0.4f, 2.6f, 12, 1.2f, 4), PartPose.offsetAndRotation(-12, 0, 0, 0, -0.12f, -0.22f));
        PartDefinition rightQ10 = rightWingMid.addOrReplaceChild("right_q1_0", CubeListBuilder.create()
                .texOffs(109, 25).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 12.5f), PartPose.offsetAndRotation(-1.2f, 0, 6.2f, 0.1f, -0, 0));
        PartDefinition rightQ11 = rightWingMid.addOrReplaceChild("right_q1_1", CubeListBuilder.create()
                .texOffs(109, 25).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 12.5f), PartPose.offsetAndRotation(-3.6f, 0, 6.2f, 0.1f, -0.04f, 0));
        PartDefinition rightQ12 = rightWingMid.addOrReplaceChild("right_q1_2", CubeListBuilder.create()
                .texOffs(109, 25).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 12.5f), PartPose.offsetAndRotation(-6, 0, 6.2f, 0.1f, -0.08f, 0));
        PartDefinition rightQ13 = rightWingMid.addOrReplaceChild("right_q1_3", CubeListBuilder.create()
                .texOffs(109, 25).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 12.5f), PartPose.offsetAndRotation(-8.4f, 0, 6.2f, 0.1f, -0.12f, 0));
        PartDefinition rightQ14 = rightWingMid.addOrReplaceChild("right_q1_4", CubeListBuilder.create()
                .texOffs(109, 25).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 12.5f), PartPose.offsetAndRotation(-10.8f, 0, 6.2f, 0.1f, -0.16f, 0));
        PartDefinition rightWingTip = rightWingMid.addOrReplaceChild("right_wing_tip", CubeListBuilder.create()
                .texOffs(79, 76).mirror(false).addBox(-10, -1.6f, -5, 10, 2.6f, 3)
                .texOffs(151, 68).mirror(false).addBox(-10, -0.9f, -2.2f, 10, 1.8f, 5)
                .texOffs(106, 76).mirror(false).addBox(-10, -0.4f, 2.6f, 10, 1.2f, 4), PartPose.offsetAndRotation(-12, 0, 0, 0, -0.12f, -0.3f));
        PartDefinition rightQ20 = rightWingTip.addOrReplaceChild("right_q2_0", CubeListBuilder.create()
                .texOffs(142, 25).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 13), PartPose.offsetAndRotation(-0.7143f, 0, 4.5f, 0.05f, -0.25f, 0));
        PartDefinition rightQ21 = rightWingTip.addOrReplaceChild("right_q2_1", CubeListBuilder.create()
                .texOffs(72, 25).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 14.6f), PartPose.offsetAndRotation(-2.1429f, 0, 4.5f, 0.05f, -0.42f, 0));
        PartDefinition rightQ22 = rightWingTip.addOrReplaceChild("right_q2_2", CubeListBuilder.create()
                .texOffs(192, 0).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 16.2f), PartPose.offsetAndRotation(-3.5714f, 0, 4.5f, 0.05f, -0.59f, 0));
        PartDefinition rightQ23 = rightWingTip.addOrReplaceChild("right_q2_3", CubeListBuilder.create()
                .texOffs(149, 0).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 17.8f), PartPose.offsetAndRotation(-5, 0, 4.5f, 0.05f, -0.76f, 0));
        PartDefinition rightQ24 = rightWingTip.addOrReplaceChild("right_q2_4", CubeListBuilder.create()
                .texOffs(102, 0).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 19.4f), PartPose.offsetAndRotation(-6.4286f, 0, 4.5f, 0.05f, -0.93f, 0));
        PartDefinition rightQ25 = rightWingTip.addOrReplaceChild("right_q2_5", CubeListBuilder.create()
                .texOffs(53, 0).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 21), PartPose.offsetAndRotation(-7.8571f, 0, 4.5f, 0.05f, -1.1f, 0));
        PartDefinition rightQ26 = rightWingTip.addOrReplaceChild("right_q2_6", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 22.6f), PartPose.offsetAndRotation(-9.2857f, 0, 4.5f, 0.05f, -1.27f, 0));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(15, 76).mirror(true).addBox(0, -1.6f, -5, 12, 2.6f, 3)
                .texOffs(116, 68).mirror(true).addBox(0, -0.9f, -2.2f, 12, 1.8f, 5)
                .texOffs(46, 76).mirror(true).addBox(0, -0.4f, 2.6f, 12, 1.2f, 4), PartPose.offsetAndRotation(4.5f, -3.5f, -3, -0.2f, 0.3f, -0.72f));
        PartDefinition leftQ00 = leftWing.addOrReplaceChild("left_q0_0", CubeListBuilder.create()
                .texOffs(149, 43).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 11), PartPose.offsetAndRotation(1.2f, 0, 6.2f, 0.1f, 0, 0));
        PartDefinition leftQ01 = leftWing.addOrReplaceChild("left_q0_1", CubeListBuilder.create()
                .texOffs(149, 43).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 11), PartPose.offsetAndRotation(3.6f, 0, 6.2f, 0.1f, 0.04f, 0));
        PartDefinition leftQ02 = leftWing.addOrReplaceChild("left_q0_2", CubeListBuilder.create()
                .texOffs(149, 43).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 11), PartPose.offsetAndRotation(6, 0, 6.2f, 0.1f, 0.08f, 0));
        PartDefinition leftQ03 = leftWing.addOrReplaceChild("left_q0_3", CubeListBuilder.create()
                .texOffs(149, 43).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 11), PartPose.offsetAndRotation(8.4f, 0, 6.2f, 0.1f, 0.12f, 0));
        PartDefinition leftQ04 = leftWing.addOrReplaceChild("left_q0_4", CubeListBuilder.create()
                .texOffs(149, 43).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 11), PartPose.offsetAndRotation(10.8f, 0, 6.2f, 0.1f, 0.16f, 0));
        PartDefinition leftWingMid = leftWing.addOrReplaceChild("left_wing_mid", CubeListBuilder.create()
                .texOffs(15, 76).mirror(true).addBox(0, -1.6f, -5, 12, 2.6f, 3)
                .texOffs(116, 68).mirror(true).addBox(0, -0.9f, -2.2f, 12, 1.8f, 5)
                .texOffs(46, 76).mirror(true).addBox(0, -0.4f, 2.6f, 12, 1.2f, 4), PartPose.offsetAndRotation(12, 0, 0, 0, 0.12f, 0.22f));
        PartDefinition leftQ10 = leftWingMid.addOrReplaceChild("left_q1_0", CubeListBuilder.create()
                .texOffs(109, 25).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 12.5f), PartPose.offsetAndRotation(1.2f, 0, 6.2f, 0.1f, 0, 0));
        PartDefinition leftQ11 = leftWingMid.addOrReplaceChild("left_q1_1", CubeListBuilder.create()
                .texOffs(109, 25).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 12.5f), PartPose.offsetAndRotation(3.6f, 0, 6.2f, 0.1f, 0.04f, 0));
        PartDefinition leftQ12 = leftWingMid.addOrReplaceChild("left_q1_2", CubeListBuilder.create()
                .texOffs(109, 25).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 12.5f), PartPose.offsetAndRotation(6, 0, 6.2f, 0.1f, 0.08f, 0));
        PartDefinition leftQ13 = leftWingMid.addOrReplaceChild("left_q1_3", CubeListBuilder.create()
                .texOffs(109, 25).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 12.5f), PartPose.offsetAndRotation(8.4f, 0, 6.2f, 0.1f, 0.12f, 0));
        PartDefinition leftQ14 = leftWingMid.addOrReplaceChild("left_q1_4", CubeListBuilder.create()
                .texOffs(109, 25).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 12.5f), PartPose.offsetAndRotation(10.8f, 0, 6.2f, 0.1f, 0.16f, 0));
        PartDefinition leftWingTip = leftWingMid.addOrReplaceChild("left_wing_tip", CubeListBuilder.create()
                .texOffs(79, 76).mirror(true).addBox(0, -1.6f, -5, 10, 2.6f, 3)
                .texOffs(151, 68).mirror(true).addBox(0, -0.9f, -2.2f, 10, 1.8f, 5)
                .texOffs(106, 76).mirror(true).addBox(0, -0.4f, 2.6f, 10, 1.2f, 4), PartPose.offsetAndRotation(12, 0, 0, 0, 0.12f, 0.3f));
        PartDefinition leftQ20 = leftWingTip.addOrReplaceChild("left_q2_0", CubeListBuilder.create()
                .texOffs(142, 25).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 13), PartPose.offsetAndRotation(0.7143f, 0, 4.5f, 0.05f, 0.25f, 0));
        PartDefinition leftQ21 = leftWingTip.addOrReplaceChild("left_q2_1", CubeListBuilder.create()
                .texOffs(72, 25).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 14.6f), PartPose.offsetAndRotation(2.1429f, 0, 4.5f, 0.05f, 0.42f, 0));
        PartDefinition leftQ22 = leftWingTip.addOrReplaceChild("left_q2_2", CubeListBuilder.create()
                .texOffs(192, 0).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 16.2f), PartPose.offsetAndRotation(3.5714f, 0, 4.5f, 0.05f, 0.59f, 0));
        PartDefinition leftQ23 = leftWingTip.addOrReplaceChild("left_q2_3", CubeListBuilder.create()
                .texOffs(149, 0).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 17.8f), PartPose.offsetAndRotation(5, 0, 4.5f, 0.05f, 0.76f, 0));
        PartDefinition leftQ24 = leftWingTip.addOrReplaceChild("left_q2_4", CubeListBuilder.create()
                .texOffs(102, 0).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 19.4f), PartPose.offsetAndRotation(6.4286f, 0, 4.5f, 0.05f, 0.93f, 0));
        PartDefinition leftQ25 = leftWingTip.addOrReplaceChild("left_q2_5", CubeListBuilder.create()
                .texOffs(53, 0).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 21), PartPose.offsetAndRotation(7.8571f, 0, 4.5f, 0.05f, 1.1f, 0));
        PartDefinition leftQ26 = leftWingTip.addOrReplaceChild("left_q2_6", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(-1.1f, -0.35f, -0.5f, 2.2f, 0.7f, 22.6f), PartPose.offsetAndRotation(9.2857f, 0, 4.5f, 0.05f, 1.27f, 0));
        PartDefinition plume0 = body.addOrReplaceChild("plume0", CubeListBuilder.create()
                .texOffs(175, 25).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 13), PartPose.offsetAndRotation(-3.9f, -1, 7.5f, 0.21f, -0.36f, 0));
        PartDefinition plume0Mid = plume0.addOrReplaceChild("plume0_mid", CubeListBuilder.create()
                .texOffs(178, 43).mirror(false).addBox(-1.1f, -0.4f, 0, 2.2f, 0.8f, 11), PartPose.offsetAndRotation(0, 0, 13, 0.1f, -0.18f, 0));
        PartDefinition plume0End = plume0Mid.addOrReplaceChild("plume0_end", CubeListBuilder.create()
                .texOffs(90, 57).mirror(false).addBox(-1.6f, -0.4f, 0, 3.2f, 0.8f, 9), PartPose.offsetAndRotation(0, 0, 11, 0.18f, -0.144f, 0));
        PartDefinition plume1 = body.addOrReplaceChild("plume1", CubeListBuilder.create()
                .texOffs(175, 25).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 13), PartPose.offsetAndRotation(-2.6f, -1, 7.5f, 0.18f, -0.24f, 0));
        PartDefinition plume1Mid = plume1.addOrReplaceChild("plume1_mid", CubeListBuilder.create()
                .texOffs(178, 43).mirror(false).addBox(-1.1f, -0.4f, 0, 2.2f, 0.8f, 11), PartPose.offsetAndRotation(0, 0, 13, 0.1f, -0.12f, 0));
        PartDefinition plume1End = plume1Mid.addOrReplaceChild("plume1_end", CubeListBuilder.create()
                .texOffs(90, 57).mirror(false).addBox(-1.6f, -0.4f, 0, 3.2f, 0.8f, 9), PartPose.offsetAndRotation(0, 0, 11, 0.18f, -0.096f, 0));
        PartDefinition plume2 = body.addOrReplaceChild("plume2", CubeListBuilder.create()
                .texOffs(175, 25).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 13), PartPose.offsetAndRotation(-1.3f, -1, 7.5f, 0.15f, -0.12f, 0));
        PartDefinition plume2Mid = plume2.addOrReplaceChild("plume2_mid", CubeListBuilder.create()
                .texOffs(178, 43).mirror(false).addBox(-1.1f, -0.4f, 0, 2.2f, 0.8f, 11), PartPose.offsetAndRotation(0, 0, 13, 0.1f, -0.06f, 0));
        PartDefinition plume2End = plume2Mid.addOrReplaceChild("plume2_end", CubeListBuilder.create()
                .texOffs(90, 57).mirror(false).addBox(-1.6f, -0.4f, 0, 3.2f, 0.8f, 9), PartPose.offsetAndRotation(0, 0, 11, 0.18f, -0.048f, 0));
        PartDefinition plume3 = body.addOrReplaceChild("plume3", CubeListBuilder.create()
                .texOffs(175, 25).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 13), PartPose.offsetAndRotation(0, -1, 7.5f, 0.12f, 0, 0));
        PartDefinition plume3Mid = plume3.addOrReplaceChild("plume3_mid", CubeListBuilder.create()
                .texOffs(178, 43).mirror(false).addBox(-1.1f, -0.4f, 0, 2.2f, 0.8f, 11), PartPose.offsetAndRotation(0, 0, 13, 0.1f, 0, 0));
        PartDefinition plume3End = plume3Mid.addOrReplaceChild("plume3_end", CubeListBuilder.create()
                .texOffs(90, 57).mirror(false).addBox(-1.6f, -0.4f, 0, 3.2f, 0.8f, 9), PartPose.offsetAndRotation(0, 0, 11, 0.18f, 0, 0));
        PartDefinition plume4 = body.addOrReplaceChild("plume4", CubeListBuilder.create()
                .texOffs(175, 25).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 13), PartPose.offsetAndRotation(1.3f, -1, 7.5f, 0.15f, 0.12f, 0));
        PartDefinition plume4Mid = plume4.addOrReplaceChild("plume4_mid", CubeListBuilder.create()
                .texOffs(178, 43).mirror(false).addBox(-1.1f, -0.4f, 0, 2.2f, 0.8f, 11), PartPose.offsetAndRotation(0, 0, 13, 0.1f, 0.06f, 0));
        PartDefinition plume4End = plume4Mid.addOrReplaceChild("plume4_end", CubeListBuilder.create()
                .texOffs(90, 57).mirror(false).addBox(-1.6f, -0.4f, 0, 3.2f, 0.8f, 9), PartPose.offsetAndRotation(0, 0, 11, 0.18f, 0.048f, 0));
        PartDefinition plume5 = body.addOrReplaceChild("plume5", CubeListBuilder.create()
                .texOffs(175, 25).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 13), PartPose.offsetAndRotation(2.6f, -1, 7.5f, 0.18f, 0.24f, 0));
        PartDefinition plume5Mid = plume5.addOrReplaceChild("plume5_mid", CubeListBuilder.create()
                .texOffs(178, 43).mirror(false).addBox(-1.1f, -0.4f, 0, 2.2f, 0.8f, 11), PartPose.offsetAndRotation(0, 0, 13, 0.1f, 0.12f, 0));
        PartDefinition plume5End = plume5Mid.addOrReplaceChild("plume5_end", CubeListBuilder.create()
                .texOffs(90, 57).mirror(false).addBox(-1.6f, -0.4f, 0, 3.2f, 0.8f, 9), PartPose.offsetAndRotation(0, 0, 11, 0.18f, 0.096f, 0));
        PartDefinition plume6 = body.addOrReplaceChild("plume6", CubeListBuilder.create()
                .texOffs(175, 25).mirror(false).addBox(-1, -0.4f, 0, 2, 0.8f, 13), PartPose.offsetAndRotation(3.9f, -1, 7.5f, 0.21f, 0.36f, 0));
        PartDefinition plume6Mid = plume6.addOrReplaceChild("plume6_mid", CubeListBuilder.create()
                .texOffs(178, 43).mirror(false).addBox(-1.1f, -0.4f, 0, 2.2f, 0.8f, 11), PartPose.offsetAndRotation(0, 0, 13, 0.1f, 0.18f, 0));
        PartDefinition plume6End = plume6Mid.addOrReplaceChild("plume6_end", CubeListBuilder.create()
                .texOffs(90, 57).mirror(false).addBox(-1.6f, -0.4f, 0, 3.2f, 0.8f, 9), PartPose.offsetAndRotation(0, 0, 11, 0.18f, 0.144f, 0));
        PartDefinition tailcovert0 = body.addOrReplaceChild("tailcovert0", CubeListBuilder.create()
                .texOffs(227, 57).mirror(false).addBox(-1.3f, -0.4f, 0, 2.6f, 0.8f, 7), PartPose.offsetAndRotation(-3.2f, -2.5f, 6.5f, 0.2f, -0.36f, 0));
        PartDefinition tailcovert1 = body.addOrReplaceChild("tailcovert1", CubeListBuilder.create()
                .texOffs(227, 57).mirror(false).addBox(-1.3f, -0.4f, 0, 2.6f, 0.8f, 7), PartPose.offsetAndRotation(-1.6f, -2.5f, 6.5f, 0.2f, -0.18f, 0));
        PartDefinition tailcovert2 = body.addOrReplaceChild("tailcovert2", CubeListBuilder.create()
                .texOffs(227, 57).mirror(false).addBox(-1.3f, -0.4f, 0, 2.6f, 0.8f, 7), PartPose.offsetAndRotation(0, -2.5f, 6.5f, 0.2f, 0, 0));
        PartDefinition tailcovert3 = body.addOrReplaceChild("tailcovert3", CubeListBuilder.create()
                .texOffs(227, 57).mirror(false).addBox(-1.3f, -0.4f, 0, 2.6f, 0.8f, 7), PartPose.offsetAndRotation(1.6f, -2.5f, 6.5f, 0.2f, 0.18f, 0));
        PartDefinition tailcovert4 = body.addOrReplaceChild("tailcovert4", CubeListBuilder.create()
                .texOffs(227, 57).mirror(false).addBox(-1.3f, -0.4f, 0, 2.6f, 0.8f, 7), PartPose.offsetAndRotation(3.2f, -2.5f, 6.5f, 0.2f, 0.36f, 0));
        PartDefinition rightTalon0 = legs.addOrReplaceChild("right_talon0", CubeListBuilder.create()
                .texOffs(196, 76).mirror(false).addBox(-0.25f, -0.25f, -1.8f, 0.5f, 0.5f, 1.8f), PartPose.offsetAndRotation(-2.6f, 8, 0, 0.6f, -0.4f, 0));
        PartDefinition rightTalon1 = legs.addOrReplaceChild("right_talon1", CubeListBuilder.create()
                .texOffs(196, 76).mirror(false).addBox(-0.25f, -0.25f, -1.8f, 0.5f, 0.5f, 1.8f), PartPose.offsetAndRotation(-2, 8, 0, 0.6f, 0, 0));
        PartDefinition rightTalon2 = legs.addOrReplaceChild("right_talon2", CubeListBuilder.create()
                .texOffs(196, 76).mirror(false).addBox(-0.25f, -0.25f, -1.8f, 0.5f, 0.5f, 1.8f), PartPose.offsetAndRotation(-1.4f, 8, 0, 0.6f, 0.4f, 0));
        PartDefinition leftTalon0 = legs.addOrReplaceChild("left_talon0", CubeListBuilder.create()
                .texOffs(196, 76).mirror(false).addBox(-0.25f, -0.25f, -1.8f, 0.5f, 0.5f, 1.8f), PartPose.offsetAndRotation(1.4f, 8, 0, 0.6f, -0.4f, 0));
        PartDefinition leftTalon1 = legs.addOrReplaceChild("left_talon1", CubeListBuilder.create()
                .texOffs(196, 76).mirror(false).addBox(-0.25f, -0.25f, -1.8f, 0.5f, 0.5f, 1.8f), PartPose.offsetAndRotation(2, 8, 0, 0.6f, 0, 0));
        PartDefinition leftTalon2 = legs.addOrReplaceChild("left_talon2", CubeListBuilder.create()
                .texOffs(196, 76).mirror(false).addBox(-0.25f, -0.25f, -1.8f, 0.5f, 0.5f, 1.8f), PartPose.offsetAndRotation(2.6f, 8, 0, 0.6f, 0.4f, 0));
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
            for (int k = 0; k < 5; k++) plumes[k] = body.getChild("plume" + (k + 1));
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
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
