package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.waters.ShrimpSoldierEntity;
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

import java.util.ArrayList;
import java.util.List;

/**
 * The Shrimp Soldier (虾兵), the Dragon King's foot soldier: a shrimp hunched forward on two jointed
 * legs, its glossy translucent-orange shell flecked red; a long serrated rostrum, black eyes on
 * stalks, long whip antennae arching back over it, antennules, antennal scales and working
 * mouthparts; the abdomen curling down behind in six segments to a spread fan tail, swimmerets
 * fringing it, three pairs of small walking legs folded under the thorax; a soldier's teal helmet
 * with a red plume, teal shoulder plates, a sea-blue sash and a belt; little pincers holding a fine
 * spear with a red tassel and a pennant. Walking, its legs step and its small legs paddle; swimming,
 * they kick, its swimmerets ripple and its tail beats; it thrusts with the spear. Drawn translucent
 * (its fan, swimmerets and scales half clear), the glint of its eyes full-bright. Model and skin:
 * {@code assets_source/shrimp_soldier.py}.
 */
public final class ShrimpSoldierRenderer extends MobRenderer<ShrimpSoldierEntity, ShrimpSoldierRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("shrimp_soldier"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/shrimp_soldier.png");
    private static final RenderType GLOW = RenderType.eyes(JTTWEngine.id("textures/entity/shrimp_soldier_glow.png"));
    /** How much larger than its pixels it is drawn (shrimp_soldier.py prints its reach). */
    public static final float SIZE = 0.88f;

    public ShrimpSoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.5f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected void scale(ShrimpSoldierEntity e, PoseStack pose, float partial) {
        pose.scale(SIZE, SIZE, SIZE);
    }

    @Override
    public ResourceLocation getTextureLocation(ShrimpSoldierEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(ShrimpSoldierEntity e) {
        return super.shouldShowName(e) && SeaCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-4.4f, -12, -3.4f, 8.8f, 12, 7.4f)
                .texOffs(140, 0).mirror(false).addBox(-3.5f, -11.4f, -3.9f, 7, 10.8f, 0.6f)
                .texOffs(240, 59).mirror(false).addBox(-0.6f, -12.6f, 2.6f, 1.2f, 1, 1.6f)
                .texOffs(25, 38).mirror(false).addBox(-4.7f, -2.2f, -4, 9.4f, 1.8f, 8)
                .texOffs(197, 59).mirror(false).addBox(-1.3f, -2.6f, -4.6f, 2.6f, 2.6f, 0.8f), PartPose.offsetAndRotation(0, 8.4f, 1.4f, 0.5f, 0, 0));
        PartDefinition sash = body.addOrReplaceChild("sash", CubeListBuilder.create()
                .texOffs(40, 0).mirror(false).addBox(-1.1f, -7.2f, -0.3f, 2.2f, 14.4f, 0.6f), PartPose.offsetAndRotation(0, -6.2f, -4.1f, 0, 0, 0.62f));
        PartDefinition rightSashTail = body.addOrReplaceChild("right_sash_tail", CubeListBuilder.create()
                .texOffs(103, 38).mirror(false).addBox(-0.8f, 0, -0.3f, 1.6f, 7.5f, 0.6f), PartPose.offsetAndRotation(-2.4f, -0.8f, -4.3f, -0.1f, 0, 0.12f));
        PartDefinition leftSashTail = body.addOrReplaceChild("left_sash_tail", CubeListBuilder.create()
                .texOffs(103, 38).mirror(true).addBox(-0.8f, 0, -0.3f, 1.6f, 7.5f, 0.6f), PartPose.offsetAndRotation(2.4f, -0.8f, -4.3f, -0.1f, 0, -0.12f));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(80, 0).mirror(false).addBox(-3.8f, -5, -6.2f, 7.6f, 6.2f, 7.6f)
                .texOffs(130, 50).mirror(false).addBox(-3, 0.8f, -5.8f, 6, 1.4f, 5), PartPose.offsetAndRotation(0, -11.6f, -0.6f, -0.35f, 0, 0));
        PartDefinition rostrum = head.addOrReplaceChild("rostrum", CubeListBuilder.create()
                .texOffs(49, 0).mirror(false).addBox(-0.5f, -1.2f, -13, 1, 2, 13.4f), PartPose.offsetAndRotation(0, -3.8f, -6, -0.32f, 0, 0));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create()
                .texOffs(224, 38).mirror(false).addBox(-0.3f, -1.2f, 0, 0.6f, 1.4f, 6), PartPose.offsetAndRotation(0, -5, -5.4f, 0, 0, 0));
        PartDefinition rightEyestalk = head.addOrReplaceChild("right_eyestalk", CubeListBuilder.create()
                .texOffs(206, 59).mirror(false).addBox(-2.6f, -0.55f, -0.55f, 2.8f, 1.1f, 1.1f), PartPose.offsetAndRotation(-2.4f, -3.4f, -5.4f, 0.15f, -0.55f, 0.1f));
        PartDefinition leftEyestalk = head.addOrReplaceChild("left_eyestalk", CubeListBuilder.create()
                .texOffs(206, 59).mirror(true).addBox(-0.2f, -0.55f, -0.55f, 2.8f, 1.1f, 1.1f), PartPose.offsetAndRotation(2.4f, -3.4f, -5.4f, 0.15f, 0.55f, -0.1f));
        PartDefinition rightEye = rightEyestalk.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(204, 50).mirror(false).addBox(-1.3f, -1.3f, -1.3f, 2.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(-2.6f, 0, 0, 0, 0, 0));
        PartDefinition leftEye = leftEyestalk.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(204, 50).mirror(true).addBox(-1.3f, -1.3f, -1.3f, 2.6f, 2.6f, 2.6f), PartPose.offsetAndRotation(2.6f, 0, 0, 0, 0, 0));
        PartDefinition rightAntenna = head.addOrReplaceChild("right_antenna", CubeListBuilder.create()
                .texOffs(217, 50).mirror(false).addBox(-0.45f, -0.45f, -5, 0.9f, 0.9f, 5), PartPose.offsetAndRotation(-1.5f, -2.2f, -6, -0.6f, 0.32f, 0));
        PartDefinition leftAntenna = head.addOrReplaceChild("left_antenna", CubeListBuilder.create()
                .texOffs(217, 50).mirror(true).addBox(-0.45f, -0.45f, -5, 0.9f, 0.9f, 5), PartPose.offsetAndRotation(1.5f, -2.2f, -6, -0.6f, -0.32f, 0));
        PartDefinition rightAntenna2 = rightAntenna.addOrReplaceChild("right_antenna_2", CubeListBuilder.create()
                .texOffs(110, 38).mirror(false).addBox(-0.3f, -0.3f, -8, 0.6f, 0.6f, 8), PartPose.offsetAndRotation(0, 0, -4.8f, -0.8f, 0.08f, 0));
        PartDefinition leftAntenna2 = leftAntenna.addOrReplaceChild("left_antenna_2", CubeListBuilder.create()
                .texOffs(110, 38).mirror(true).addBox(-0.3f, -0.3f, -8, 0.6f, 0.6f, 8), PartPose.offsetAndRotation(0, 0, -4.8f, -0.8f, -0.08f, 0));
        PartDefinition rightAntenna3 = rightAntenna2.addOrReplaceChild("right_antenna_3", CubeListBuilder.create()
                .texOffs(157, 0).mirror(false).addBox(-0.25f, -0.25f, -11, 0.5f, 0.5f, 11), PartPose.offsetAndRotation(0, 0, -7.8f, -1, 0.3f, 0));
        PartDefinition leftAntenna3 = leftAntenna2.addOrReplaceChild("left_antenna_3", CubeListBuilder.create()
                .texOffs(157, 0).mirror(true).addBox(-0.25f, -0.25f, -11, 0.5f, 0.5f, 11), PartPose.offsetAndRotation(0, 0, -7.8f, -1, -0.3f, 0));
        PartDefinition rightAntenna4 = rightAntenna3.addOrReplaceChild("right_antenna_4", CubeListBuilder.create()
                .texOffs(113, 0).mirror(false).addBox(-0.2f, -0.2f, -12, 0.4f, 0.4f, 12), PartPose.offsetAndRotation(0, 0, -10.8f, -0.5f, 0.9f, 0));
        PartDefinition leftAntenna4 = leftAntenna3.addOrReplaceChild("left_antenna_4", CubeListBuilder.create()
                .texOffs(113, 0).mirror(true).addBox(-0.2f, -0.2f, -12, 0.4f, 0.4f, 12), PartPose.offsetAndRotation(0, 0, -10.8f, -0.5f, -0.9f, 0));
        PartDefinition rightAntennule = head.addOrReplaceChild("right_antennule", CubeListBuilder.create()
                .texOffs(230, 50).mirror(false).addBox(-0.3f, -0.3f, -4.5f, 0.6f, 0.6f, 4.5f), PartPose.offsetAndRotation(-0.9f, -2.8f, -6, -0.25f, 0.2f, 0));
        PartDefinition leftAntennule = head.addOrReplaceChild("left_antennule", CubeListBuilder.create()
                .texOffs(230, 50).mirror(true).addBox(-0.3f, -0.3f, -4.5f, 0.6f, 0.6f, 4.5f), PartPose.offsetAndRotation(0.9f, -2.8f, -6, -0.25f, -0.2f, 0));
        PartDefinition rightAntennuleA = rightAntennule.addOrReplaceChild("right_antennule_a", CubeListBuilder.create()
                .texOffs(243, 50).mirror(false).addBox(-0.2f, -0.2f, -5, 0.4f, 0.4f, 5), PartPose.offsetAndRotation(0, 0, -4.4f, -0.4f, 0.15f, 0));
        PartDefinition leftAntennuleA = leftAntennule.addOrReplaceChild("left_antennule_a", CubeListBuilder.create()
                .texOffs(243, 50).mirror(true).addBox(-0.2f, -0.2f, -5, 0.4f, 0.4f, 5), PartPose.offsetAndRotation(0, 0, -4.4f, -0.4f, -0.15f, 0));
        PartDefinition rightAntennuleB = rightAntennule.addOrReplaceChild("right_antennule_b", CubeListBuilder.create()
                .texOffs(101, 59).mirror(false).addBox(-0.2f, -0.2f, -4, 0.4f, 0.4f, 4), PartPose.offsetAndRotation(0, 0, -4.4f, 0.15f, -0.1f, 0));
        PartDefinition leftAntennuleB = leftAntennule.addOrReplaceChild("left_antennule_b", CubeListBuilder.create()
                .texOffs(101, 59).mirror(true).addBox(-0.2f, -0.2f, -4, 0.4f, 0.4f, 4), PartPose.offsetAndRotation(0, 0, -4.4f, 0.15f, 0.1f, 0));
        PartDefinition rightScale = head.addOrReplaceChild("right_scale", CubeListBuilder.create()
                .texOffs(153, 50).mirror(false).addBox(-1, -0.2f, -5.6f, 2, 0.4f, 5.6f), PartPose.offsetAndRotation(-2.2f, -1.4f, -6, -0.15f, 0.22f, 0));
        PartDefinition leftScale = head.addOrReplaceChild("left_scale", CubeListBuilder.create()
                .texOffs(153, 50).mirror(true).addBox(-1, -0.2f, -5.6f, 2, 0.4f, 5.6f), PartPose.offsetAndRotation(2.2f, -1.4f, -6, -0.15f, -0.22f, 0));
        PartDefinition rightMaxilliped = head.addOrReplaceChild("right_maxilliped", CubeListBuilder.create()
                .texOffs(0, 59).mirror(false).addBox(-0.4f, 0, -0.4f, 0.8f, 4.2f, 0.8f), PartPose.offsetAndRotation(-1.3f, 1.6f, -5.2f, -0.5f, 0, 0.12f));
        PartDefinition leftMaxilliped = head.addOrReplaceChild("left_maxilliped", CubeListBuilder.create()
                .texOffs(0, 59).mirror(true).addBox(-0.4f, 0, -0.4f, 0.8f, 4.2f, 0.8f), PartPose.offsetAndRotation(1.3f, 1.6f, -5.2f, -0.5f, 0, -0.12f));
        PartDefinition helmet = head.addOrReplaceChild("helmet", CubeListBuilder.create()
                .texOffs(129, 38).mirror(false).addBox(-4, -0.7f, -4, 8, 1, 8)
                .texOffs(209, 0).mirror(false).addBox(-3.2f, -3.8f, -3.2f, 6.4f, 3.2f, 6.4f)
                .texOffs(217, 59).mirror(false).addBox(-0.7f, -5, -0.7f, 1.4f, 1.3f, 1.4f)
                .texOffs(239, 38).mirror(false).addBox(-1.7f, -8.2f, -1.7f, 3.4f, 3.4f, 3.4f), PartPose.offsetAndRotation(0, -5, -1.2f, 0.3f, 0, 0));
        PartDefinition plumeTail = helmet.addOrReplaceChild("plume_tail", CubeListBuilder.create()
                .texOffs(0, 50).mirror(false).addBox(-1.4f, 0, -0.8f, 2.8f, 6, 1.6f), PartPose.offsetAndRotation(0, -7, 1.2f, 1, 0, 0));
        PartDefinition tail1 = root.addOrReplaceChild("tail_1", CubeListBuilder.create()
                .texOffs(182, 0).mirror(false).addBox(-3.8f, -3.2f, 0, 7.6f, 6.4f, 4.4f)
                .texOffs(5, 59).mirror(false).addBox(-4.1f, -3.55f, -0.3f, 8.2f, 3.968f, 1.3f), PartPose.offsetAndRotation(0, 8, 2.6f, 0.35f, 0, 0));
        PartDefinition rightSwimmeret1 = tail1.addOrReplaceChild("right_swimmeret_1", CubeListBuilder.create()
                .texOffs(28, 59).mirror(false).addBox(-0.5f, 0, -0.9f, 1, 3.2f, 1.8f), PartPose.offsetAndRotation(-2.7f, 3, 1.98f, 0.35f, 0, 0.3f));
        PartDefinition leftSwimmeret1 = tail1.addOrReplaceChild("left_swimmeret_1", CubeListBuilder.create()
                .texOffs(28, 59).mirror(true).addBox(-0.5f, 0, -0.9f, 1, 3.2f, 1.8f), PartPose.offsetAndRotation(2.7f, 3, 1.98f, 0.35f, 0, -0.3f));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create()
                .texOffs(0, 38).mirror(false).addBox(-3.5f, -3, 0, 7, 6, 4.2f)
                .texOffs(35, 59).mirror(false).addBox(-3.8f, -3.35f, -0.3f, 7.6f, 3.72f, 1.3f), PartPose.offsetAndRotation(0, 0, 4.2f, -0.4f, 0, 0));
        PartDefinition rightSwimmeret2 = tail2.addOrReplaceChild("right_swimmeret_2", CubeListBuilder.create()
                .texOffs(112, 59).mirror(false).addBox(-0.5f, 0, -0.9f, 1, 3, 1.8f), PartPose.offsetAndRotation(-2.4f, 2.8f, 1.89f, 0.35f, 0, 0.3f));
        PartDefinition leftSwimmeret2 = tail2.addOrReplaceChild("left_swimmeret_2", CubeListBuilder.create()
                .texOffs(112, 59).mirror(true).addBox(-0.5f, 0, -0.9f, 1, 3, 1.8f), PartPose.offsetAndRotation(2.4f, 2.8f, 1.89f, 0.35f, 0, -0.3f));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create()
                .texOffs(62, 38).mirror(false).addBox(-3.2f, -2.7f, 0, 6.4f, 5.4f, 4)
                .texOffs(56, 59).mirror(false).addBox(-3.5f, -3.05f, -0.3f, 7, 3.348f, 1.3f), PartPose.offsetAndRotation(0, 0, 4, -0.4f, 0, 0));
        PartDefinition rightSwimmeret3 = tail3.addOrReplaceChild("right_swimmeret_3", CubeListBuilder.create()
                .texOffs(112, 59).mirror(false).addBox(-0.5f, 0, -0.9f, 1, 2.8f, 1.8f), PartPose.offsetAndRotation(-2.1f, 2.5f, 1.8f, 0.35f, 0, 0.3f));
        PartDefinition leftSwimmeret3 = tail3.addOrReplaceChild("left_swimmeret_3", CubeListBuilder.create()
                .texOffs(112, 59).mirror(true).addBox(-0.5f, 0, -0.9f, 1, 2.8f, 1.8f), PartPose.offsetAndRotation(2.1f, 2.5f, 1.8f, 0.35f, 0, -0.3f));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail_4", CubeListBuilder.create()
                .texOffs(162, 38).mirror(false).addBox(-2.8f, -2.4f, 0, 5.6f, 4.8f, 3.6f)
                .texOffs(119, 59).mirror(false).addBox(-3.1f, -2.75f, -0.3f, 6.2f, 2.976f, 1.3f), PartPose.offsetAndRotation(0, 0, 3.8f, -0.4f, 0, 0));
        PartDefinition rightSwimmeret4 = tail4.addOrReplaceChild("right_swimmeret_4", CubeListBuilder.create()
                .texOffs(112, 59).mirror(false).addBox(-0.5f, 0, -0.9f, 1, 2.6f, 1.8f), PartPose.offsetAndRotation(-1.7f, 2.2f, 1.62f, 0.35f, 0, 0.3f));
        PartDefinition leftSwimmeret4 = tail4.addOrReplaceChild("left_swimmeret_4", CubeListBuilder.create()
                .texOffs(112, 59).mirror(true).addBox(-0.5f, 0, -0.9f, 1, 2.6f, 1.8f), PartPose.offsetAndRotation(1.7f, 2.2f, 1.62f, 0.35f, 0, -0.3f));
        PartDefinition tail5 = tail4.addOrReplaceChild("tail_5", CubeListBuilder.create()
                .texOffs(11, 50).mirror(false).addBox(-2.4f, -2, 0, 4.8f, 4, 3.2f)
                .texOffs(138, 59).mirror(false).addBox(-2.7f, -2.35f, -0.3f, 5.4f, 2.48f, 1.3f), PartPose.offsetAndRotation(0, 0, 3.4f, -0.4f, 0, 0));
        PartDefinition rightSwimmeret5 = tail5.addOrReplaceChild("right_swimmeret_5", CubeListBuilder.create()
                .texOffs(112, 59).mirror(false).addBox(-0.5f, 0, -0.9f, 1, 2.4f, 1.8f), PartPose.offsetAndRotation(-1.3f, 1.8f, 1.44f, 0.35f, 0, 0.3f));
        PartDefinition leftSwimmeret5 = tail5.addOrReplaceChild("left_swimmeret_5", CubeListBuilder.create()
                .texOffs(112, 59).mirror(true).addBox(-0.5f, 0, -0.9f, 1, 2.4f, 1.8f), PartPose.offsetAndRotation(1.3f, 1.8f, 1.44f, 0.35f, 0, -0.3f));
        PartDefinition tail6 = tail5.addOrReplaceChild("tail_6", CubeListBuilder.create()
                .texOffs(170, 50).mirror(false).addBox(-2, -1.7f, 0, 4, 3.4f, 3)
                .texOffs(155, 59).mirror(false).addBox(-2.3f, -2.05f, -0.3f, 4.6f, 2.108f, 1.3f), PartPose.offsetAndRotation(0, 0, 3, -0.4f, 0, 0));
        PartDefinition telson = tail6.addOrReplaceChild("telson", CubeListBuilder.create()
                .texOffs(30, 50).mirror(false).addBox(-1.1f, -0.5f, 0, 2.2f, 1, 6.4f), PartPose.offsetAndRotation(0, 0, 2.8f, -0.15f, 0, 0));
        PartDefinition rightUropod = tail6.addOrReplaceChild("right_uropod", CubeListBuilder.create()
                .texOffs(51, 50).mirror(false).addBox(-1.7f, -0.3f, 0, 3.4f, 0.6f, 6.4f), PartPose.offsetAndRotation(-0.9f, 0, 2.6f, -0.15f, -0.5f, 0));
        PartDefinition leftUropod = tail6.addOrReplaceChild("left_uropod", CubeListBuilder.create()
                .texOffs(51, 50).mirror(true).addBox(-1.7f, -0.3f, 0, 3.4f, 0.6f, 6.4f), PartPose.offsetAndRotation(0.9f, 0, 2.6f, -0.15f, 0.5f, 0));
        PartDefinition rightUropodIn = tail6.addOrReplaceChild("right_uropod_in", CubeListBuilder.create()
                .texOffs(185, 50).mirror(false).addBox(-1.4f, -0.3f, 0, 2.8f, 0.6f, 6), PartPose.offsetAndRotation(-0.5f, -0.1f, 2.7f, -0.15f, -0.22f, 0));
        PartDefinition leftUropodIn = tail6.addOrReplaceChild("left_uropod_in", CubeListBuilder.create()
                .texOffs(185, 50).mirror(true).addBox(-1.4f, -0.3f, 0, 2.8f, 0.6f, 6), PartPose.offsetAndRotation(0.5f, -0.1f, 2.7f, -0.15f, 0.22f, 0));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(85, 38).mirror(false).addBox(-1, 0, -1, 2, 7.6f, 2)
                .texOffs(170, 59).mirror(false).addBox(-1.25f, -0.6f, -1.25f, 2.5f, 1.6f, 2.5f), PartPose.offsetAndRotation(-2.6f, 8.4f, 0.6f, -0.42f, 0, 0.2f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(85, 38).mirror(true).addBox(-1, 0, -1, 2, 7.6f, 2)
                .texOffs(170, 59).mirror(true).addBox(-1.25f, -0.6f, -1.25f, 2.5f, 1.6f, 2.5f), PartPose.offsetAndRotation(2.6f, 8.4f, 0.6f, -0.42f, 0, -0.2f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(94, 38).mirror(false).addBox(-0.8f, 0, -0.8f, 1.6f, 7.6f, 1.6f)
                .texOffs(170, 59).mirror(false).addBox(-1.05f, -0.6f, -1.05f, 2.1f, 1.4f, 2.1f), PartPose.offsetAndRotation(0, 7.4f, 0, 0.78f, 0, -0.12f));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(94, 38).mirror(true).addBox(-0.8f, 0, -0.8f, 1.6f, 7.6f, 1.6f)
                .texOffs(170, 59).mirror(true).addBox(-1.05f, -0.6f, -1.05f, 2.1f, 1.4f, 2.1f), PartPose.offsetAndRotation(0, 7.4f, 0, 0.78f, 0, 0.12f));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(183, 59).mirror(false).addBox(-0.55f, 0, -0.55f, 1.1f, 2.2f, 1.1f), PartPose.offsetAndRotation(0, 7.4f, 0, -0.3f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(183, 59).mirror(true).addBox(-0.55f, 0, -0.55f, 1.1f, 2.2f, 1.1f), PartPose.offsetAndRotation(0, 7.4f, 0, -0.3f, 0, 0));
        PartDefinition rightWalker0 = body.addOrReplaceChild("right_walker_0", CubeListBuilder.create()
                .texOffs(0, 59).mirror(false).addBox(-0.4f, 0, -0.4f, 0.8f, 4.2f, 0.8f), PartPose.offsetAndRotation(-2.4f, -1.6f, -3.6f, -0.2f, 0.25f, 0.75f));
        PartDefinition leftWalker0 = body.addOrReplaceChild("left_walker_0", CubeListBuilder.create()
                .texOffs(0, 59).mirror(true).addBox(-0.4f, 0, -0.4f, 0.8f, 4.2f, 0.8f), PartPose.offsetAndRotation(2.4f, -1.6f, -3.6f, -0.2f, -0.25f, -0.75f));
        PartDefinition rightWalker0Tip = rightWalker0.addOrReplaceChild("right_walker_0_tip", CubeListBuilder.create()
                .texOffs(192, 59).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 4, 0.6f), PartPose.offsetAndRotation(0, 4, 0, -1, 0, -0.5f));
        PartDefinition leftWalker0Tip = leftWalker0.addOrReplaceChild("left_walker_0_tip", CubeListBuilder.create()
                .texOffs(192, 59).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 4, 0.6f), PartPose.offsetAndRotation(0, 4, 0, -1, 0, 0.5f));
        PartDefinition rightWalker1 = body.addOrReplaceChild("right_walker_1", CubeListBuilder.create()
                .texOffs(0, 59).mirror(false).addBox(-0.4f, 0, -0.4f, 0.8f, 4.2f, 0.8f), PartPose.offsetAndRotation(-2.6f, -4.2f, -3.6f, -0.2f, 0.25f, 0.75f));
        PartDefinition leftWalker1 = body.addOrReplaceChild("left_walker_1", CubeListBuilder.create()
                .texOffs(0, 59).mirror(true).addBox(-0.4f, 0, -0.4f, 0.8f, 4.2f, 0.8f), PartPose.offsetAndRotation(2.6f, -4.2f, -3.6f, -0.2f, -0.25f, -0.75f));
        PartDefinition rightWalker1Tip = rightWalker1.addOrReplaceChild("right_walker_1_tip", CubeListBuilder.create()
                .texOffs(192, 59).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 4, 0.6f), PartPose.offsetAndRotation(0, 4, 0, -1, 0, -0.5f));
        PartDefinition leftWalker1Tip = leftWalker1.addOrReplaceChild("left_walker_1_tip", CubeListBuilder.create()
                .texOffs(192, 59).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 4, 0.6f), PartPose.offsetAndRotation(0, 4, 0, -1, 0, 0.5f));
        PartDefinition rightWalker2 = body.addOrReplaceChild("right_walker_2", CubeListBuilder.create()
                .texOffs(0, 59).mirror(false).addBox(-0.4f, 0, -0.4f, 0.8f, 4.2f, 0.8f), PartPose.offsetAndRotation(-2.8f, -6.8f, -3.6f, -0.2f, 0.25f, 0.75f));
        PartDefinition leftWalker2 = body.addOrReplaceChild("left_walker_2", CubeListBuilder.create()
                .texOffs(0, 59).mirror(true).addBox(-0.4f, 0, -0.4f, 0.8f, 4.2f, 0.8f), PartPose.offsetAndRotation(2.8f, -6.8f, -3.6f, -0.2f, -0.25f, -0.75f));
        PartDefinition rightWalker2Tip = rightWalker2.addOrReplaceChild("right_walker_2_tip", CubeListBuilder.create()
                .texOffs(192, 59).mirror(false).addBox(-0.3f, 0, -0.3f, 0.6f, 4, 0.6f), PartPose.offsetAndRotation(0, 4, 0, -1, 0, -0.5f));
        PartDefinition leftWalker2Tip = leftWalker2.addOrReplaceChild("left_walker_2_tip", CubeListBuilder.create()
                .texOffs(192, 59).mirror(true).addBox(-0.3f, 0, -0.3f, 0.6f, 4, 0.6f), PartPose.offsetAndRotation(0, 4, 0, -1, 0, 0.5f));
        PartDefinition rightShoulder = body.addOrReplaceChild("right_shoulder", CubeListBuilder.create()
                .texOffs(74, 50).mirror(false).addBox(-3.4f, -0.8f, -2.8f, 3.8f, 1.4f, 5.6f)
                .texOffs(183, 38).mirror(false).addBox(-3.9f, 0.4f, -3.1f, 4.2f, 1.6f, 6.2f), PartPose.offsetAndRotation(-4.4f, -10.4f, -0.6f, 0, 0, 0.42f));
        PartDefinition leftShoulder = body.addOrReplaceChild("left_shoulder", CubeListBuilder.create()
                .texOffs(74, 50).mirror(true).addBox(-0.4f, -0.8f, -2.8f, 3.8f, 1.4f, 5.6f)
                .texOffs(183, 38).mirror(true).addBox(-0.3f, 0.4f, -3.1f, 4.2f, 1.6f, 6.2f), PartPose.offsetAndRotation(4.4f, -10.4f, -0.6f, 0, 0, -0.42f));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(208, 38).mirror(false).addBox(-0.95f, 0, -0.95f, 1.9f, 6.4f, 1.9f)
                .texOffs(170, 59).mirror(false).addBox(-1.2f, -0.6f, -1.2f, 2.4f, 1.4f, 2.4f), PartPose.offsetAndRotation(-4.2f, -9.2f, -2.6f, -0.55f, 0, 0.2f));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(208, 38).mirror(true).addBox(-0.95f, 0, -0.95f, 1.9f, 6.4f, 1.9f)
                .texOffs(170, 59).mirror(true).addBox(-1.2f, -0.6f, -1.2f, 2.4f, 1.4f, 2.4f), PartPose.offsetAndRotation(4.2f, -9.2f, -2.6f, -0.55f, 0, -0.2f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(95, 50).mirror(false).addBox(-0.8f, 0, -0.8f, 1.6f, 6, 1.6f)
                .texOffs(170, 59).mirror(false).addBox(-1.05f, -0.5f, -1.05f, 2.1f, 1.2f, 2.1f)
                .texOffs(75, 59).mirror(false).addBox(-1.1f, 3, -1.1f, 2.2f, 2.2f, 2.2f), PartPose.offsetAndRotation(0, 6.2f, 0, -0.9f, 0.3f, 0));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(95, 50).mirror(true).addBox(-0.8f, 0, -0.8f, 1.6f, 6, 1.6f)
                .texOffs(170, 59).mirror(true).addBox(-1.05f, -0.5f, -1.05f, 2.1f, 1.2f, 2.1f)
                .texOffs(75, 59).mirror(true).addBox(-1.1f, 3, -1.1f, 2.2f, 2.2f, 2.2f), PartPose.offsetAndRotation(0, 6.2f, 0, -0.9f, -0.3f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(88, 59).mirror(false).addBox(-1.1f, 0, -1.1f, 2.2f, 2.4f, 2.2f)
                .texOffs(226, 59).mirror(false).addBox(-1.1f, 2.2f, -0.9f, 0.9f, 2.2f, 1)
                .texOffs(249, 59).mirror(false).addBox(0.2f, 2.2f, -0.9f, 0.9f, 1.8f, 1), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(88, 59).mirror(true).addBox(-1.1f, 0, -1.1f, 2.2f, 2.4f, 2.2f)
                .texOffs(226, 59).mirror(true).addBox(0.2f, 2.2f, -0.9f, 0.9f, 2.2f, 1)
                .texOffs(249, 59).mirror(true).addBox(-1.1f, 2.2f, -0.9f, 0.9f, 1.8f, 1), PartPose.offsetAndRotation(0, 6, 0, 0, 0, 0));
        PartDefinition spear = rightHand.addOrReplaceChild("spear", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.45f, -28, -0.45f, 0.9f, 36, 0.9f)
                .texOffs(217, 38).mirror(false).addBox(-0.8f, -35.5f, -0.25f, 1.6f, 7.6f, 0.5f)
                .texOffs(231, 59).mirror(false).addBox(-0.75f, -28.8f, -0.75f, 1.5f, 1.6f, 1.5f)
                .texOffs(104, 50).mirror(false).addBox(-1.3f, -27.6f, -1.3f, 2.6f, 4.4f, 2.6f)
                .texOffs(0, 66).mirror(false).addBox(-0.6f, 7.6f, -0.6f, 1.2f, 1, 1.2f)
                .texOffs(9, 66).mirror(false).addBox(-0.35f, 8.4f, -0.35f, 0.7f, 2, 0.7f), PartPose.offsetAndRotation(0, 1.3f, 0, 1.0938f, 0.0143f, -0.2573f));
        PartDefinition pennant = spear.addOrReplaceChild("pennant", CubeListBuilder.create()
                .texOffs(117, 50).mirror(false).addBox(0, 0, -0.1f, 5, 6.4f, 0.2f), PartPose.offsetAndRotation(0.45f, -22.6f, 0, 0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<ShrimpSoldierEntity> {
        private final ModelPart root, body, head, rightLeg, leftLeg, rightShin, leftShin, rightArm, leftArm, rightForearm,
                rightAntenna, leftAntenna, rightAntennaTip, leftAntennaTip, plumeTail, rightSashTail, leftSashTail,
                rightUropod, leftUropod, telson;
        private final List<ModelPart> tail = new ArrayList<>(), swimmerets = new ArrayList<>(), walkers = new ArrayList<>(),
                mouthparts = new ArrayList<>();

        public Model(ModelPart root) {
            super(RenderType::entityTranslucent);
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            rightArm = body.getChild("right_arm");
            leftArm = body.getChild("left_arm");
            rightForearm = rightArm.getChild("right_forearm");
            rightAntenna = head.getChild("right_antenna");
            leftAntenna = head.getChild("left_antenna");
            rightAntennaTip = rightAntenna.getChild("right_antenna_2").getChild("right_antenna_3");
            leftAntennaTip = leftAntenna.getChild("left_antenna_2").getChild("left_antenna_3");
            plumeTail = head.getChild("helmet").getChild("plume_tail");
            rightSashTail = body.getChild("right_sash_tail");
            leftSashTail = body.getChild("left_sash_tail");
            mouthparts.add(head.getChild("right_maxilliped"));
            mouthparts.add(head.getChild("left_maxilliped"));
            ModelPart t = root;
            for (int k = 1; k <= 6; k++) {
                t = t.getChild("tail_" + k);
                tail.add(t);
                if (t.hasChild("right_swimmeret_" + k)) {
                    swimmerets.add(t.getChild("right_swimmeret_" + k));
                    swimmerets.add(t.getChild("left_swimmeret_" + k));
                }
            }
            telson = t.getChild("telson");
            rightUropod = t.getChild("right_uropod");
            leftUropod = t.getChild("left_uropod");
            for (int k = 0; body.hasChild("right_walker_" + k); k++) {
                walkers.add(body.getChild("right_walker_" + k));
                walkers.add(body.getChild("left_walker_" + k));
            }
        }

        private static void turn(ModelPart p, float x, float y, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.yRot = p.getInitialPose().yRot + y;
            p.zRot = p.getInitialPose().zRot + z;
        }

        @Override
        public void setupAnim(ShrimpSoldierEntity e, float swing, float amount, float age, float yaw, float pitch) {
            boolean swimming = e.isInWater();
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            turn(head, pitch * Mth.DEG_TO_RAD * 0.7f, yaw * Mth.DEG_TO_RAD, 0);
            // the two legs step on land, kick in the water; the knee bends as each comes through
            float w = swing * 0.66f;
            float kick = swimming ? Mth.sin(age * 0.35f) * (0.25f + amount * 0.6f) : Mth.cos(w) * 0.9f * amount;
            turn(rightLeg, kick, 0, 0);
            turn(leftLeg, -kick, 0, 0);
            turn(rightShin, Math.max(0, Mth.sin(w)) * 0.6f * amount, 0, 0);
            turn(leftShin, Math.max(0, -Mth.sin(w)) * 0.6f * amount, 0, 0);
            // the small legs paddle under it, one pair after another; the mouthparts work
            float paddle = swimming ? 0.35f : 0.12f + amount * 0.2f;
            for (int k = 0; k < walkers.size(); k++) turn(walkers.get(k), Mth.sin(age * 0.3f + (k / 2) * 1.1f + (k % 2) * Mth.PI) * paddle, 0, 0);
            for (int k = 0; k < mouthparts.size(); k++) turn(mouthparts.get(k), Mth.sin(age * 0.4f + k * Mth.PI) * 0.15f, 0, 0);
            // the swimmerets ripple in a wave down the abdomen; the tail beats, curling under it to swim
            float ripple = swimming ? 0.4f : 0.08f;
            for (int k = 0; k < swimmerets.size(); k++) turn(swimmerets.get(k), Mth.sin(age * 0.45f - (k / 2) * 0.8f) * ripple, 0, 0);
            float beat = Mth.sin(age * (swimming ? 0.4f : 0.1f)) * (swimming ? 0.12f : 0.025f);
            for (ModelPart s : tail) turn(s, beat, 0, 0);
            turn(telson, beat * 1.5f, 0, 0);
            float spread = Mth.sin(age * 0.2f) * (swimming ? 0.12f : 0.04f);
            turn(rightUropod, beat, -spread, 0);
            turn(leftUropod, beat, spread, 0);
            // the thorax bobs; the spear arm thrusts on a blow, the other swings
            turn(body, Mth.sin(age * 0.08f) * 0.025f + strike * 0.12f, 0, 0);
            turn(rightArm, -strike * 1.0f, 0, 0);
            turn(rightForearm, strike * 0.9f, 0, 0);
            turn(leftArm, -kick * 0.4f, 0, 0);
            // the antennae sway; the plume and the sash ends trail
            turn(rightAntenna, 0, Mth.sin(age * 0.09f) * 0.12f, 0);
            turn(leftAntenna, 0, -Mth.sin(age * 0.09f + 1f) * 0.12f, 0);
            turn(rightAntennaTip, Mth.sin(age * 0.11f) * 0.08f, 0, 0);
            turn(leftAntennaTip, Mth.sin(age * 0.11f + 0.8f) * 0.08f, 0, 0);
            turn(plumeTail, amount * 0.3f + Mth.sin(age * 0.1f) * 0.06f, 0, 0);
            turn(rightSashTail, amount * 0.25f + Mth.sin(age * 0.08f) * 0.06f, 0, 0);
            turn(leftSashTail, amount * 0.25f + Mth.sin(age * 0.08f + 1) * 0.06f, 0, 0);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
