package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.waters.YakshaEntity;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Yaksha (夜叉) of the sea, the Dragon King's patrol demon (巡海夜叉): a fierce, muscular sea demon,
 * taller than a man, crouched to spring. Indigo-blue skin lit cyan over its muscles, patches of
 * blue-green scales, a spined fin down its back and fins on its forearms, calves and ears, webbed and
 * clawed hands and feet; hair standing up off its head like flames and a flaming beard, bulging
 * burning eyes under a heavy brow, fangs, tusks, two ridged horns. Gold armlets, anklets and earrings,
 * a necklace of pearls and skulls, a red silk scarf flowing from its shoulders, a fish-scale wrap
 * over a red apron and a demon-faced gold belt; the army's teal and gold bracers and spaulder, and a
 * great trident with a red tassel and the wave pennant. Its hair flickers and its scarf streams as it
 * swims, kicking; it snarls, its fins ripple, its clawed hand flexes; it thrusts with the trident.
 * Drawn translucent (its fins and webs half clear), its eyes, pearls and the tips of its flames
 * full-bright. The trident it is given is drawn by its model, not held as an item. Model and skin:
 * {@code assets_source/yaksha.py}.
 */
public final class YakshaRenderer extends MobRenderer<YakshaEntity, YakshaRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("yaksha"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/yaksha.png");
    private static final RenderType GLOW = RenderType.eyes(JTTWEngine.id("textures/entity/yaksha_glow.png"));
    /** How much larger than its pixels it is drawn (yaksha.py prints its reach). */
    public static final float SIZE = 0.88f;

    public YakshaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.6f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected void scale(YakshaEntity e, PoseStack pose, float partial) {
        pose.scale(SIZE, SIZE, SIZE);
    }

    @Override
    public ResourceLocation getTextureLocation(YakshaEntity e) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(YakshaEntity e) {
        return super.shouldShowName(e) && SeaCourt.near(entityRenderDispatcher, e);
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(57, 0).mirror(false).addBox(-2.5f, -1.2f, -2.5f, 5, 8.4f, 5)
                .texOffs(53, 70).mirror(false).addBox(-1.9f, 0.6f, -3.2f, 3.8f, 5.6f, 0.9f), PartPose.offsetAndRotation(-3.4f, 8.3824f, 0.8f, -0.42f, 0, 0.14f));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(57, 0).mirror(true).addBox(-2.5f, -1.2f, -2.5f, 5, 8.4f, 5)
                .texOffs(53, 70).mirror(true).addBox(-1.9f, 0.6f, -3.2f, 3.8f, 5.6f, 0.9f), PartPose.offsetAndRotation(3.4f, 8.3824f, 0.8f, -0.42f, 0, -0.14f));
        PartDefinition rightShin = rightLeg.addOrReplaceChild("right_shin", CubeListBuilder.create()
                .texOffs(190, 0).mirror(false).addBox(-1.9f, 0, -1.9f, 3.8f, 7.4f, 3.8f)
                .texOffs(161, 70).mirror(false).addBox(-1.6f, 0.8f, 1.4f, 3.2f, 4.4f, 1)
                .texOffs(64, 70).mirror(false).addBox(-2.25f, 5.4f, -2.25f, 4.5f, 1.3f, 4.5f), PartPose.offsetAndRotation(0, 7.6f, 0, 0.78f, 0, -0.1f));
        PartDefinition leftShin = leftLeg.addOrReplaceChild("left_shin", CubeListBuilder.create()
                .texOffs(190, 0).mirror(true).addBox(-1.9f, 0, -1.9f, 3.8f, 7.4f, 3.8f)
                .texOffs(161, 70).mirror(true).addBox(-1.6f, 0.8f, 1.4f, 3.2f, 4.4f, 1)
                .texOffs(64, 70).mirror(true).addBox(-2.25f, 5.4f, -2.25f, 4.5f, 1.3f, 4.5f), PartPose.offsetAndRotation(0, 7.6f, 0, 0.78f, 0, 0.1f));
        PartDefinition rightCalffin = rightShin.addOrReplaceChild("right_calffin", CubeListBuilder.create()
                .texOffs(234, 48).mirror(false).addBox(-0.25f, 0, 0, 0.5f, 5.2f, 3), PartPose.offsetAndRotation(0, 0.6f, 2.2f, -0.35f, 0, 0));
        PartDefinition leftCalffin = leftShin.addOrReplaceChild("left_calffin", CubeListBuilder.create()
                .texOffs(234, 48).mirror(true).addBox(-0.25f, 0, 0, 0.5f, 5.2f, 3), PartPose.offsetAndRotation(0, 0.6f, 2.2f, -0.35f, 0, 0));
        PartDefinition rightFoot = rightShin.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(156, 60).mirror(false).addBox(-2.1f, 0, -3.6f, 4.2f, 1.9f, 5.4f)
                .texOffs(66, 79).mirror(false).addBox(-2.2f, 0.5f, -5.4f, 1.3f, 1.4f, 1.9f)
                .texOffs(221, 70).mirror(false).addBox(-0.65f, 0.5f, -5.8f, 1.3f, 1.4f, 2.3f)
                .texOffs(66, 79).mirror(false).addBox(0.9f, 0.5f, -5.4f, 1.3f, 1.4f, 1.9f)
                .texOffs(0, 85).mirror(false).addBox(-1.9f, 1.3f, -5.2f, 3.8f, 0.4f, 1.6f), PartPose.offsetAndRotation(0, 7.2f, 0, -0.36f, 0, 0));
        PartDefinition leftFoot = leftShin.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(156, 60).mirror(true).addBox(-2.1f, 0, -3.6f, 4.2f, 1.9f, 5.4f)
                .texOffs(66, 79).mirror(true).addBox(0.9f, 0.5f, -5.4f, 1.3f, 1.4f, 1.9f)
                .texOffs(221, 70).mirror(true).addBox(-0.65f, 0.5f, -5.8f, 1.3f, 1.4f, 2.3f)
                .texOffs(66, 79).mirror(true).addBox(-2.2f, 0.5f, -5.4f, 1.3f, 1.4f, 1.9f)
                .texOffs(0, 85).mirror(true).addBox(-1.9f, 1.3f, -5.2f, 3.8f, 0.4f, 1.6f), PartPose.offsetAndRotation(0, 7.2f, 0, -0.36f, 0, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(70, 48).mirror(false).addBox(-4.3f, -3.4f, -2.7f, 8.6f, 3.8f, 5.4f)
                .texOffs(0, 48).mirror(false).addBox(-3.9f, -8.4f, -2.5f, 7.8f, 5.4f, 5)
                .texOffs(18, 0).mirror(false).addBox(-5.6f, -15.6f, -3.1f, 11.2f, 7.6f, 6.4f)
                .texOffs(172, 70).mirror(false).addBox(-5.2f, -15, -3.9f, 5, 4.2f, 1)
                .texOffs(172, 70).mirror(false).addBox(0.2f, -15, -3.9f, 5, 4.2f, 1)
                .texOffs(85, 70).mirror(false).addBox(-3.8f, -17.2f, -1.4f, 7.6f, 2, 4.4f)
                .texOffs(179, 60).mirror(false).addBox(-5.2f, -15.2f, 2.7f, 10.4f, 6.8f, 1)
                .texOffs(0, 60).mirror(false).addBox(-4.6f, -4.2f, -3.1f, 9.2f, 1.7f, 6.2f)
                .texOffs(232, 70).mirror(false).addBox(-1.7f, -5, -3.7f, 3.4f, 3.2f, 0.8f)
                .texOffs(75, 79).mirror(false).addBox(-4.9653f, -15.505f, -4.0577f, 1.2f, 1.2f, 1.2f)
                .texOffs(75, 79).mirror(false).addBox(-4.5709f, -14.8858f, -4.1524f, 1.2f, 1.2f, 1.2f)
                .texOffs(84, 79).mirror(false).addBox(-4.2548f, -14.3809f, -4.4362f, 1.7f, 1.9f, 1.3f)
                .texOffs(75, 79).mirror(false).addBox(-3.2914f, -13.441f, -4.3055f, 1.2f, 1.2f, 1.2f)
                .texOffs(75, 79).mirror(false).addBox(-2.4617f, -12.7238f, -4.3572f, 1.2f, 1.2f, 1.2f)
                .texOffs(75, 79).mirror(false).addBox(-1.5514f, -12.0901f, -4.3892f, 1.2f, 1.2f, 1.2f)
                .texOffs(84, 79).mirror(false).addBox(-0.85f, -11.8f, -4.6f, 1.7f, 1.9f, 1.3f)
                .texOffs(75, 79).mirror(false).addBox(0.3514f, -12.0901f, -4.3892f, 1.2f, 1.2f, 1.2f)
                .texOffs(75, 79).mirror(false).addBox(1.2617f, -12.7238f, -4.3572f, 1.2f, 1.2f, 1.2f)
                .texOffs(75, 79).mirror(false).addBox(2.0914f, -13.441f, -4.3055f, 1.2f, 1.2f, 1.2f)
                .texOffs(84, 79).mirror(false).addBox(2.5548f, -14.3809f, -4.4362f, 1.7f, 1.9f, 1.3f)
                .texOffs(75, 79).mirror(false).addBox(3.3709f, -14.8858f, -4.1524f, 1.2f, 1.2f, 1.2f)
                .texOffs(75, 79).mirror(false).addBox(3.7653f, -15.505f, -4.0577f, 1.2f, 1.2f, 1.2f), PartPose.offsetAndRotation(0, 8.3824f, 0.8f, 0.24f, 0, 0));
        PartDefinition apron = body.addOrReplaceChild("apron", CubeListBuilder.create()
                .texOffs(101, 48).mirror(false).addBox(-1.8f, 0, -0.3f, 3.6f, 8.6f, 0.6f), PartPose.offsetAndRotation(0, -2.6f, -3.1f, -0.12f, 0, 0));
        PartDefinition rightWrap = body.addOrReplaceChild("right_wrap", CubeListBuilder.create()
                .texOffs(204, 60).mirror(false).addBox(-2.1f, 0, -0.3f, 3.6f, 6.8f, 0.6f), PartPose.offsetAndRotation(-2.6f, -2.6f, -3, -0.16f, 0, 0.12f));
        PartDefinition leftWrap = body.addOrReplaceChild("left_wrap", CubeListBuilder.create()
                .texOffs(204, 60).mirror(true).addBox(-1.5f, 0, -0.3f, 3.6f, 6.8f, 0.6f), PartPose.offsetAndRotation(2.6f, -2.6f, -3, -0.16f, 0, -0.12f));
        PartDefinition rightWrapSide = body.addOrReplaceChild("right_wrap_side", CubeListBuilder.create()
                .texOffs(127, 0).mirror(false).addBox(-0.3f, 0, -2.6f, 0.6f, 6.4f, 5.2f), PartPose.offsetAndRotation(-4.3f, -2.6f, 0, 0, 0, 0.2f));
        PartDefinition leftWrapSide = body.addOrReplaceChild("left_wrap_side", CubeListBuilder.create()
                .texOffs(127, 0).mirror(true).addBox(-0.3f, 0, -2.6f, 0.6f, 6.4f, 5.2f), PartPose.offsetAndRotation(4.3f, -2.6f, 0, 0, 0, -0.2f));
        PartDefinition wrapBack = body.addOrReplaceChild("wrap_back", CubeListBuilder.create()
                .texOffs(35, 60).mirror(false).addBox(-4.2f, 0, -0.3f, 8.4f, 7.6f, 0.6f), PartPose.offsetAndRotation(0, -2.6f, 2.8f, 0.2f, 0, 0));
        PartDefinition dorsal = body.addOrReplaceChild("dorsal", CubeListBuilder.create()
                .texOffs(5, 0).mirror(false).addBox(-0.25f, 0, 0, 0.5f, 13.4f, 5), PartPose.offsetAndRotation(0, -16.6f, 3.2f, 0.3f, 0, 0));
        PartDefinition spine = body.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(78, 0).mirror(false).addBox(-1.3f, 0, -0.4f, 2.6f, 13, 0.8f), PartPose.offsetAndRotation(0, -16, 3.6f, 0.02f, 0, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(87, 0).mirror(false).addBox(-3.6f, -7.4f, -3.2f, 7.2f, 6.6f, 6.6f)
                .texOffs(93, 79).mirror(false).addBox(-3.9f, -5.6f, -4.3f, 7.8f, 1.7f, 1.8f)
                .texOffs(112, 70).mirror(false).addBox(-3.7f, -4, -4, 7.4f, 2.8f, 3.6f)
                .texOffs(13, 85).mirror(false).addBox(-3.9f, -3.25f, -4.35f, 2.2f, 1.1f, 1)
                .texOffs(13, 85).mirror(false).addBox(1.7f, -3.25f, -4.35f, 2.2f, 1.1f, 1)
                .texOffs(243, 70).mirror(false).addBox(-1.7f, -4.6f, -5, 3.4f, 2.5f, 1.2f)
                .texOffs(22, 85).mirror(false).addBox(-2.5f, -3, -4.75f, 1, 1, 1.1f)
                .texOffs(22, 85).mirror(false).addBox(1.5f, -3, -4.75f, 1, 1, 1.1f)
                .texOffs(0, 79).mirror(false).addBox(-3, -2.3f, -4.5f, 6, 1.5f, 2.2f)
                .texOffs(19, 79).mirror(false).addBox(-2.6f, -0.9f, -4, 5.2f, 1.3f, 2.8f)
                .texOffs(29, 85).mirror(false).addBox(-2.35f, -1, -4.3f, 1.1f, 1.4f, 1)
                .texOffs(29, 85).mirror(false).addBox(1.25f, -1, -4.3f, 1.1f, 1.4f, 1)
                .texOffs(36, 85).mirror(false).addBox(-2.1f, 0.3f, -4.15f, 0.6f, 1.3f, 0.6f)
                .texOffs(36, 85).mirror(false).addBox(1.5f, 0.3f, -4.15f, 0.6f, 1.3f, 0.6f)
                .texOffs(56, 60).mirror(false).addBox(-3, -7, 2.6f, 6, 6.6f, 1.6f), PartPose.offsetAndRotation(0, -16.8f, -1.4f, -0.22f, 0, 0));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(215, 60).mirror(false).addBox(-3.2f, 0, -4, 6.4f, 2.4f, 4.4f)
                .texOffs(114, 79).mirror(false).addBox(-2.2f, 2.1f, -3.6f, 4.4f, 0.9f, 2.8f), PartPose.offsetAndRotation(0, -1.2f, -0.4f, 0.26f, 0, 0));
        PartDefinition beard = jaw.addOrReplaceChild("beard", CubeListBuilder.create()
                .texOffs(240, 60).mirror(false).addBox(-2.4f, 0, -1, 4.8f, 4.4f, 2.2f), PartPose.offsetAndRotation(0, 2.6f, -2.6f, 0.2f, 0, 0));
        PartDefinition rightTusk = jaw.addOrReplaceChild("right_tusk", CubeListBuilder.create()
                .texOffs(131, 79).mirror(false).addBox(-0.45f, -2, -0.45f, 0.9f, 2.3f, 0.9f), PartPose.offsetAndRotation(-2.6f, 0.5f, -3.7f, -0.3f, 0.25f, -0.38f));
        PartDefinition leftTusk = jaw.addOrReplaceChild("left_tusk", CubeListBuilder.create()
                .texOffs(131, 79).mirror(true).addBox(-0.45f, -2, -0.45f, 0.9f, 2.3f, 0.9f), PartPose.offsetAndRotation(2.6f, 0.5f, -3.7f, -0.3f, -0.25f, 0.38f));
        PartDefinition rightTuskTip = rightTusk.addOrReplaceChild("right_tusk_tip", CubeListBuilder.create()
                .texOffs(41, 85).mirror(false).addBox(-0.35f, -1.5f, -0.35f, 0.7f, 1.7f, 0.7f), PartPose.offsetAndRotation(0, -1.9f, 0, -0.5f, 0, 0.2f));
        PartDefinition leftTuskTip = leftTusk.addOrReplaceChild("left_tusk_tip", CubeListBuilder.create()
                .texOffs(41, 85).mirror(true).addBox(-0.35f, -1.5f, -0.35f, 0.7f, 1.7f, 0.7f), PartPose.offsetAndRotation(0, -1.9f, 0, -0.5f, 0, -0.2f));
        PartDefinition rightEye = head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                .texOffs(136, 79).mirror(false).addBox(-1.15f, -1, -0.9f, 2.3f, 2, 1.4f), PartPose.offsetAndRotation(-1.85f, -4.35f, -4.3f, 0, 0.18f, 0));
        PartDefinition leftEye = head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                .texOffs(136, 79).mirror(true).addBox(-1.15f, -1, -0.9f, 2.3f, 2, 1.4f), PartPose.offsetAndRotation(1.85f, -4.35f, -4.3f, 0, -0.18f, 0));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(73, 60).mirror(false).addBox(-0.25f, -3.4f, 0, 0.5f, 4.8f, 4), PartPose.offsetAndRotation(-3.6f, -4.4f, -0.6f, 0.1f, 0.6f, -0.35f));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(73, 60).mirror(true).addBox(-0.25f, -3.4f, 0, 0.5f, 4.8f, 4), PartPose.offsetAndRotation(3.6f, -4.4f, -0.6f, 0.1f, -0.6f, 0.35f));
        PartDefinition rightHoop = head.addOrReplaceChild("right_hoop", CubeListBuilder.create()
                .texOffs(46, 85).mirror(false).addBox(-0.25f, 0, -0.9f, 0.5f, 0.45f, 1.8f)
                .texOffs(46, 85).mirror(false).addBox(-0.25f, 2, -0.9f, 0.5f, 0.45f, 1.8f)
                .texOffs(53, 85).mirror(false).addBox(-0.25f, 0.3f, -1.1f, 0.5f, 1.9f, 0.45f)
                .texOffs(53, 85).mirror(false).addBox(-0.25f, 0.3f, 0.6f, 0.5f, 1.9f, 0.45f), PartPose.offsetAndRotation(-3.7f, -1.6f, -1, 0, 0, 0.1f));
        PartDefinition leftHoop = head.addOrReplaceChild("left_hoop", CubeListBuilder.create()
                .texOffs(46, 85).mirror(true).addBox(-0.25f, 0, -0.9f, 0.5f, 0.45f, 1.8f)
                .texOffs(46, 85).mirror(true).addBox(-0.25f, 2, -0.9f, 0.5f, 0.45f, 1.8f)
                .texOffs(53, 85).mirror(true).addBox(-0.25f, 0.3f, -1.1f, 0.5f, 1.9f, 0.45f)
                .texOffs(53, 85).mirror(true).addBox(-0.25f, 0.3f, 0.6f, 0.5f, 1.9f, 0.45f), PartPose.offsetAndRotation(3.7f, -1.6f, -1, 0, 0, -0.1f));
        PartDefinition rightHorn = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(185, 70).mirror(false).addBox(-0.95f, -3.2f, -0.95f, 1.9f, 3.4f, 1.9f), PartPose.offsetAndRotation(-2.4f, -7, -2, -0.2f, 0, -0.42f));
        PartDefinition leftHorn = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(185, 70).mirror(true).addBox(-0.95f, -3.2f, -0.95f, 1.9f, 3.4f, 1.9f), PartPose.offsetAndRotation(2.4f, -7, -2, -0.2f, 0, 0.42f));
        PartDefinition rightHorn2 = rightHorn.addOrReplaceChild("right_horn_2", CubeListBuilder.create()
                .texOffs(194, 70).mirror(false).addBox(-0.7f, -3, -0.7f, 1.4f, 3.2f, 1.4f), PartPose.offsetAndRotation(0, -3, 0, -0.45f, 0, 0.12f));
        PartDefinition leftHorn2 = leftHorn.addOrReplaceChild("left_horn_2", CubeListBuilder.create()
                .texOffs(194, 70).mirror(true).addBox(-0.7f, -3, -0.7f, 1.4f, 3.2f, 1.4f), PartPose.offsetAndRotation(0, -3, 0, -0.45f, 0, -0.12f));
        PartDefinition rightHorn3 = rightHorn2.addOrReplaceChild("right_horn_3", CubeListBuilder.create()
                .texOffs(147, 79).mirror(false).addBox(-0.45f, -2.6f, -0.45f, 0.9f, 2.8f, 0.9f), PartPose.offsetAndRotation(0, -2.8f, 0, -0.55f, 0, 0.1f));
        PartDefinition leftHorn3 = leftHorn2.addOrReplaceChild("left_horn_3", CubeListBuilder.create()
                .texOffs(147, 79).mirror(true).addBox(-0.45f, -2.6f, -0.45f, 0.9f, 2.8f, 0.9f), PartPose.offsetAndRotation(0, -2.8f, 0, -0.55f, 0, -0.1f));
        PartDefinition hairCrown = head.addOrReplaceChild("hair_crown", CubeListBuilder.create()
                .texOffs(207, 0).mirror(false).addBox(-2.4f, -8.4f, -1.5f, 4.8f, 8.6f, 3), PartPose.offsetAndRotation(0, -7.2f, -0.6f, -0.3f, 0, 0));
        PartDefinition rightHair1 = head.addOrReplaceChild("right_hair_1", CubeListBuilder.create()
                .texOffs(27, 48).mirror(false).addBox(-1.9f, -7.6f, -1.4f, 3.8f, 7.8f, 2.8f), PartPose.offsetAndRotation(-2.4f, -7, -0.2f, -0.15f, 0, -0.55f));
        PartDefinition leftHair1 = head.addOrReplaceChild("left_hair_1", CubeListBuilder.create()
                .texOffs(27, 48).mirror(true).addBox(-1.9f, -7.6f, -1.4f, 3.8f, 7.8f, 2.8f), PartPose.offsetAndRotation(2.4f, -7, -0.2f, -0.15f, 0, 0.55f));
        PartDefinition rightHair2 = head.addOrReplaceChild("right_hair_2", CubeListBuilder.create()
                .texOffs(112, 48).mirror(false).addBox(-1.7f, -6.8f, -1.3f, 3.4f, 7, 2.6f), PartPose.offsetAndRotation(-3, -5.6f, 1.6f, 0.4f, 0, -0.95f));
        PartDefinition leftHair2 = head.addOrReplaceChild("left_hair_2", CubeListBuilder.create()
                .texOffs(112, 48).mirror(true).addBox(-1.7f, -6.8f, -1.3f, 3.4f, 7, 2.6f), PartPose.offsetAndRotation(3, -5.6f, 1.6f, 0.4f, 0, 0.95f));
        PartDefinition hairBack = head.addOrReplaceChild("hair_back", CubeListBuilder.create()
                .texOffs(224, 0).mirror(false).addBox(-2.6f, -8.2f, -1.4f, 5.2f, 8.4f, 2.8f), PartPose.offsetAndRotation(0, -6.8f, 2.2f, 0.75f, 0, 0));
        PartDefinition rightHairBack = head.addOrReplaceChild("right_hair_back", CubeListBuilder.create()
                .texOffs(112, 48).mirror(false).addBox(-1.8f, -6.8f, -1.3f, 3.6f, 7, 2.6f), PartPose.offsetAndRotation(-2, -5, 2.8f, 1, 0, -0.5f));
        PartDefinition leftHairBack = head.addOrReplaceChild("left_hair_back", CubeListBuilder.create()
                .texOffs(112, 48).mirror(true).addBox(-1.8f, -6.8f, -1.3f, 3.6f, 7, 2.6f), PartPose.offsetAndRotation(2, -5, 2.8f, 1, 0, 0.5f));
        PartDefinition hairNape = head.addOrReplaceChild("hair_nape", CubeListBuilder.create()
                .texOffs(127, 48).mirror(false).addBox(-2.8f, -6.4f, -1.2f, 5.6f, 6.6f, 2.4f), PartPose.offsetAndRotation(0, -2.4f, 3.4f, 1.45f, 0, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(142, 0).mirror(false).addBox(-3.3f, -2.4f, -3.1f, 5.8f, 5.2f, 6.2f)
                .texOffs(169, 0).mirror(false).addBox(-2.3f, 0, -2.3f, 4.6f, 7.8f, 4.6f)
                .texOffs(203, 70).mirror(false).addBox(-1.7f, 1.2f, -3, 3.4f, 4.4f, 0.8f)
                .texOffs(0, 70).mirror(false).addBox(-2.65f, 4.8f, -2.65f, 5.3f, 1.4f, 5.3f), PartPose.offsetAndRotation(-6.4f, -14.2f, 0.2f, -0.62f, 0.1f, 0.32f));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(42, 48).mirror(false).addBox(-2, 0, -2, 4, 7, 4)
                .texOffs(146, 48).mirror(false).addBox(-2.4f, 2.2f, -2.4f, 4.8f, 4.4f, 4.8f), PartPose.offsetAndRotation(0, 7.4f, 0, -0.95f, 0.3f, 0.05f));
        PartDefinition rightArmfin = rightForearm.addOrReplaceChild("right_armfin", CubeListBuilder.create()
                .texOffs(59, 48).mirror(false).addBox(-0.25f, 0, 0, 0.5f, 6.2f, 3.6f), PartPose.offsetAndRotation(-1.2f, 0.4f, 2, 0.4f, -0.25f, 0));
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(137, 70).mirror(false).addBox(-1.9f, 0, -1.7f, 3.8f, 3, 3.4f)
                .texOffs(152, 79).mirror(false).addBox(-1.9f, 2.6f, -2.2f, 3.8f, 1.8f, 1.8f)
                .texOffs(38, 79).mirror(false).addBox(-2.4f, 0.6f, -1.4f, 0.9f, 2.2f, 1.4f), PartPose.offsetAndRotation(0, 6.8f, 0, 0, 0, 0));
        PartDefinition trident = rightHand.addOrReplaceChild("trident", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5f, -30, -0.5f, 1, 46, 1)
                .texOffs(165, 79).mirror(false).addBox(-0.9f, -31.2f, -0.9f, 1.8f, 1.4f, 1.8f)
                .texOffs(25, 70).mirror(false).addBox(-1.7f, -34.6f, -1.7f, 3.4f, 3.4f, 3.4f)
                .texOffs(174, 79).mirror(false).addBox(-6.2f, -36, -0.6f, 12.4f, 1.4f, 1.2f)
                .texOffs(118, 0).mirror(false).addBox(-0.7f, -47.2f, -0.7f, 1.4f, 11.4f, 1.4f)
                .texOffs(45, 79).mirror(false).addBox(-1.4f, -51, -0.4f, 2.8f, 4, 0.8f)
                .texOffs(167, 48).mirror(false).addBox(-6.2f, -43.6f, -0.55f, 1.2f, 7.8f, 1.1f)
                .texOffs(167, 48).mirror(false).addBox(5, -43.6f, -0.55f, 1.2f, 7.8f, 1.1f)
                .texOffs(54, 79).mirror(false).addBox(-6.6f, -46.6f, -0.35f, 2, 3.2f, 0.7f)
                .texOffs(54, 79).mirror(false).addBox(4.6f, -46.6f, -0.35f, 2, 3.2f, 0.7f)
                .texOffs(84, 60).mirror(false).addBox(-1.6f, -30.4f, -1.6f, 3.2f, 4.6f, 3.2f)
                .texOffs(165, 79).mirror(false).addBox(-0.7f, 15.6f, -0.7f, 1.4f, 1.2f, 1.4f)
                .texOffs(205, 79).mirror(false).addBox(-0.4f, 16.6f, -0.4f, 0.8f, 2.4f, 0.8f), PartPose.offsetAndRotation(0, 1.6f, -0.4f, 1.9921f, 0.0935f, -0.3435f));
        PartDefinition rightBarb = trident.addOrReplaceChild("right_barb", CubeListBuilder.create()
                .texOffs(58, 85).mirror(false).addBox(-1.8f, -0.5f, -0.3f, 1.8f, 1, 0.6f), PartPose.offsetAndRotation(-0.7f, -43, 0, 0, 0, -0.5f));
        PartDefinition leftBarb = trident.addOrReplaceChild("left_barb", CubeListBuilder.create()
                .texOffs(58, 85).mirror(true).addBox(0, -0.5f, -0.3f, 1.8f, 1, 0.6f), PartPose.offsetAndRotation(0.7f, -43, 0, 0, 0, 0.5f));
        PartDefinition rightSidebarb = trident.addOrReplaceChild("right_sidebarb", CubeListBuilder.create()
                .texOffs(58, 85).mirror(false).addBox(0, -0.5f, -0.3f, 1.6f, 1, 0.6f), PartPose.offsetAndRotation(-5, -40.6f, 0, 0, 0, 0.55f));
        PartDefinition leftSidebarb = trident.addOrReplaceChild("left_sidebarb", CubeListBuilder.create()
                .texOffs(58, 85).mirror(true).addBox(-1.6f, -0.5f, -0.3f, 1.6f, 1, 0.6f), PartPose.offsetAndRotation(5, -40.6f, 0, 0, 0, -0.55f));
        PartDefinition pennant = trident.addOrReplaceChild("pennant", CubeListBuilder.create()
                .texOffs(101, 60).mirror(false).addBox(0, 0, -0.1f, 6, 7.6f, 0.2f), PartPose.offsetAndRotation(0.5f, -25.6f, 0, 0, 0, 0));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(142, 0).mirror(true).addBox(-2.5f, -2.4f, -3.1f, 5.8f, 5.2f, 6.2f)
                .texOffs(169, 0).mirror(true).addBox(-2.3f, 0, -2.3f, 4.6f, 7.8f, 4.6f)
                .texOffs(203, 70).mirror(true).addBox(-1.7f, 1.2f, -3, 3.4f, 4.4f, 0.8f)
                .texOffs(0, 70).mirror(true).addBox(-2.65f, 4.8f, -2.65f, 5.3f, 1.4f, 5.3f), PartPose.offsetAndRotation(6.4f, -14.2f, 0.2f, -0.55f, -0.3f, -1));
        PartDefinition leftSpaulder = leftArm.addOrReplaceChild("left_spaulder", CubeListBuilder.create()
                .texOffs(116, 60).mirror(true).addBox(-2.6f, -0.8f, -3.4f, 6.2f, 1.4f, 6.8f)
                .texOffs(176, 48).mirror(true).addBox(-2.6f, 0.4f, -3.6f, 6, 1.6f, 7.2f)
                .texOffs(205, 48).mirror(true).addBox(-2.4f, 1.8f, -3.7f, 5.6f, 1.5f, 7.4f), PartPose.offsetAndRotation(0.4f, -2.2f, 0, 0, 0, -0.3f));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(42, 48).mirror(true).addBox(-2, 0, -2, 4, 7, 4)
                .texOffs(146, 48).mirror(true).addBox(-2.4f, 2.2f, -2.4f, 4.8f, 4.4f, 4.8f), PartPose.offsetAndRotation(0, 7.4f, 0, -1.35f, 0, 0.25f));
        PartDefinition leftArmfin = leftForearm.addOrReplaceChild("left_armfin", CubeListBuilder.create()
                .texOffs(59, 48).mirror(true).addBox(-0.25f, 0, 0, 0.5f, 6.2f, 3.6f), PartPose.offsetAndRotation(1.2f, 0.4f, 2, 0.4f, 0.25f, 0));
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(137, 70).mirror(true).addBox(-1.9f, 0, -1.7f, 3.8f, 3, 3.4f)
                .texOffs(38, 79).mirror(true).addBox(1.5f, 0.4f, -1.2f, 0.9f, 2.4f, 1.4f), PartPose.offsetAndRotation(0, 6.8f, 0, -0.35f, 0, 0));
        PartDefinition leftFinger1 = leftHand.addOrReplaceChild("left_finger_1", CubeListBuilder.create()
                .texOffs(214, 70).mirror(true).addBox(-0.5f, 0, -0.55f, 1, 3.8f, 1.1f), PartPose.offsetAndRotation(-1.4f, 2.8f, -0.2f, 0, 0, 0.32f));
        PartDefinition leftFinger2 = leftHand.addOrReplaceChild("left_finger_2", CubeListBuilder.create()
                .texOffs(154, 70).mirror(true).addBox(-0.5f, 0, -0.55f, 1, 4.2f, 1.1f), PartPose.offsetAndRotation(-0.2f, 2.8f, -0.2f, 0, 0, 0.1f));
        PartDefinition leftFinger3 = leftHand.addOrReplaceChild("left_finger_3", CubeListBuilder.create()
                .texOffs(214, 70).mirror(true).addBox(-0.5f, 0, -0.55f, 1, 4, 1.1f), PartPose.offsetAndRotation(1, 2.8f, -0.2f, 0, 0, -0.12f));
        PartDefinition leftFinger4 = leftHand.addOrReplaceChild("left_finger_4", CubeListBuilder.create()
                .texOffs(61, 79).mirror(true).addBox(-0.45f, 0, -0.5f, 0.9f, 3.4f, 1), PartPose.offsetAndRotation(1.8f, 2.6f, -0.2f, 0, 0, -0.38f));
        PartDefinition leftWeb = leftHand.addOrReplaceChild("left_web", CubeListBuilder.create()
                .texOffs(210, 79).mirror(true).addBox(-2.3f, 0, -0.2f, 4.6f, 3, 0.3f), PartPose.offsetAndRotation(0, 2.6f, -0.1f, 0, 0, 0));
        PartDefinition rightScarf = body.addOrReplaceChild("right_scarf", CubeListBuilder.create()
                .texOffs(42, 70).mirror(false).addBox(-1.8f, 0, -0.25f, 3.6f, 7, 0.5f), PartPose.offsetAndRotation(-6.6f, -15.4f, 1.8f, 0.3f, 0, 0.45f));
        PartDefinition rightScarf2 = rightScarf.addOrReplaceChild("right_scarf_2", CubeListBuilder.create()
                .texOffs(42, 70).mirror(false).addBox(-1.8f, 0, -0.25f, 3.6f, 7, 0.5f), PartPose.offsetAndRotation(0, 6.8f, 0, 0.3f, 0, -0.3f));
        PartDefinition rightScarf3 = rightScarf2.addOrReplaceChild("right_scarf_3", CubeListBuilder.create()
                .texOffs(145, 60).mirror(false).addBox(-1.8f, 0, -0.25f, 3.6f, 7.6f, 0.5f), PartPose.offsetAndRotation(0, 6.8f, 0, 0.35f, 0, -0.15f));
        PartDefinition leftScarf = body.addOrReplaceChild("left_scarf", CubeListBuilder.create()
                .texOffs(42, 70).mirror(true).addBox(-1.8f, 0, -0.25f, 3.6f, 7, 0.5f), PartPose.offsetAndRotation(6.6f, -15.4f, 1.8f, 0.3f, 0, -0.45f));
        PartDefinition leftScarf2 = leftScarf.addOrReplaceChild("left_scarf_2", CubeListBuilder.create()
                .texOffs(42, 70).mirror(true).addBox(-1.8f, 0, -0.25f, 3.6f, 7, 0.5f), PartPose.offsetAndRotation(0, 6.8f, 0, 0.3f, 0, 0.3f));
        PartDefinition leftScarf3 = leftScarf2.addOrReplaceChild("left_scarf_3", CubeListBuilder.create()
                .texOffs(145, 60).mirror(true).addBox(-1.8f, 0, -0.25f, 3.6f, 7.6f, 0.5f), PartPose.offsetAndRotation(0, 6.8f, 0, 0.35f, 0, 0.15f));
        PartDefinition scarfBack = body.addOrReplaceChild("scarf_back", CubeListBuilder.create()
                .texOffs(223, 79).mirror(false).addBox(-6.4f, -0.6f, -0.3f, 12.8f, 2.6f, 0.6f), PartPose.offsetAndRotation(0, -16.4f, 3.2f, 0.3f, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<YakshaEntity> {
        private static final String[] HAIR = {"hair_crown", "right_hair_1", "left_hair_1", "right_hair_2", "left_hair_2",
                "hair_back", "right_hair_back", "left_hair_back", "hair_nape"};
        private final ModelPart root, body, head, jaw, beard, rightEar, leftEar, rightLeg, leftLeg, rightShin, leftShin,
                rightFoot, leftFoot, rightArm, rightForearm, rightHand, trident, leftArm, leftForearm, leftHand, pennant, dorsal,
                rightArmFin, leftArmFin, rightCalfFin, leftCalfFin, scarfBack, apron, rightWrap, leftWrap, wrapBack;
        private final ModelPart[] hair = new ModelPart[HAIR.length], fingers = new ModelPart[4];
        private final ModelPart[][] scarf = new ModelPart[2][3];

        public Model(ModelPart root) {
            super(RenderType::entityTranslucent);
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            jaw = head.getChild("jaw");
            beard = jaw.getChild("beard");
            rightEar = head.getChild("right_ear");
            leftEar = head.getChild("left_ear");
            for (int i = 0; i < HAIR.length; i++) hair[i] = head.getChild(HAIR[i]);
            rightLeg = root.getChild("right_leg");
            leftLeg = root.getChild("left_leg");
            rightShin = rightLeg.getChild("right_shin");
            leftShin = leftLeg.getChild("left_shin");
            rightFoot = rightShin.getChild("right_foot");
            leftFoot = leftShin.getChild("left_foot");
            rightCalfFin = rightShin.getChild("right_calffin");
            leftCalfFin = leftShin.getChild("left_calffin");
            rightArm = body.getChild("right_arm");
            rightForearm = rightArm.getChild("right_forearm");
            rightHand = rightForearm.getChild("right_hand");
            rightArmFin = rightForearm.getChild("right_armfin");
            trident = rightHand.getChild("trident");
            pennant = trident.getChild("pennant");
            leftArm = body.getChild("left_arm");
            leftForearm = leftArm.getChild("left_forearm");
            leftHand = leftForearm.getChild("left_hand");
            leftArmFin = leftForearm.getChild("left_armfin");
            for (int i = 0; i < 4; i++) fingers[i] = leftHand.getChild("left_finger_" + (i + 1));
            dorsal = body.getChild("dorsal");
            scarfBack = body.getChild("scarf_back");
            String[] side = {"right", "left"};
            for (int s = 0; s < 2; s++) {
                ModelPart p = body.getChild(side[s] + "_scarf");
                scarf[s][0] = p;
                scarf[s][1] = p = p.getChild(side[s] + "_scarf_2");
                scarf[s][2] = p.getChild(side[s] + "_scarf_3");
            }
            apron = body.getChild("apron");
            rightWrap = body.getChild("right_wrap");
            leftWrap = body.getChild("left_wrap");
            wrapBack = body.getChild("wrap_back");
        }

        private static void turn(ModelPart p, float x, float y, float z) {
            p.xRot = p.getInitialPose().xRot + x;
            p.yRot = p.getInitialPose().yRot + y;
            p.zRot = p.getInitialPose().zRot + z;
        }

        /** A part's turn, as ModelPart applies it. */
        private static Quaternionf q(ModelPart p) {
            return new Quaternionf().rotationZYX(p.zRot, p.yRot, p.xRot);
        }

        /**
         * The trident levelled: turns the hand so that, by {@code t} (0 as it is held .. 1 levelled), the
         * trident lies straight along the way the yaksha faces, flat and level, its prongs ahead (the
         * model's -z) and its crossbar across, whatever its body and arm are doing.
         */
        private void level(float t) {
            if (t <= 0) return;
            // the hand's turn that lays the trident's length (its +y, the butt) along +z, its crossbar along x:
            // the parts above it undone, a quarter turn about x, the trident's own set in the hand undone
            Quaternionf above = q(body).mul(q(rightArm)).mul(q(rightForearm));
            Quaternionf levelled = above.conjugate(new Quaternionf())
                    .mul(new Quaternionf().rotationX(Mth.HALF_PI))
                    .mul(q(trident).conjugate());
            Quaternionf hand = q(rightHand).slerp(levelled, Math.min(1, t));
            Vector3f e = hand.getEulerAnglesZYX(new Vector3f());
            rightHand.xRot = e.x;
            rightHand.yRot = e.y;
            rightHand.zRot = e.z;
        }

        @Override
        public void setupAnim(YakshaEntity e, float swing, float amount, float age, float yaw, float pitch) {
            boolean swimming = e.isInWater() && !e.onGround();
            float strike = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) : 0;
            // the thrust: a stab, driven out in an instant, held levelled through the blow, drawn back as the swing ends
            float thrust = attackTime > 0 ? Mth.clamp(attackTime / 0.12f, 0, 1) * Mth.clamp((1 - attackTime) / 0.08f, 0, 1) : 0;
            // the legs: a long stride on land; swimming, a fast flutter kick, the shins bending at its back
            float w = swing * 0.66f;
            float kick = swimming ? Mth.sin(age * 0.28f) * (0.22f + amount * 0.5f) : Mth.cos(w) * 0.85f * amount;
            turn(rightLeg, kick + (swimming ? 0.25f : 0), 0, 0);
            turn(leftLeg, -kick + (swimming ? 0.25f : 0), 0, 0);
            turn(rightShin, swimming ? Math.max(0, kick) * 0.7f : Math.max(0, Mth.sin(w)) * 0.8f * amount, 0, 0);
            turn(leftShin, swimming ? Math.max(0, -kick) * 0.7f : Math.max(0, -Mth.sin(w)) * 0.8f * amount, 0, 0);
            turn(rightFoot, swimming ? 0.35f : 0, 0, 0);
            turn(leftFoot, swimming ? 0.35f : 0, 0, 0);
            float flick = Mth.sin(age * 0.2f) * 0.12f;
            turn(rightCalfFin, flick, 0, 0);
            turn(leftCalfFin, -flick, 0, 0);
            // the body: it breathes, sways as it walks, leans into a swim, lunges on a thrust
            turn(body, (swimming ? 0.3f * amount : 0) + Mth.sin(age * 0.09f) * 0.025f + strike * 0.22f,
                    Mth.sin(w) * 0.1f * amount - strike * 0.15f, Mth.cos(w) * 0.04f * amount);
            body.y = body.getInitialPose().y + Mth.sin(age * 0.09f) * 0.35f;
            // the head turns to what it watches; it snarls, its jaw working, gaping on a thrust
            turn(head, pitch * Mth.DEG_TO_RAD * 0.7f - (swimming ? 0.25f * amount : 0) - strike * 0.12f, yaw * Mth.DEG_TO_RAD, 0);
            turn(jaw, 0.06f * (Mth.sin(age * 0.11f) + 1) + strike * 0.35f, 0, 0);
            turn(beard, Mth.sin(age * 0.21f) * 0.1f + amount * 0.25f, 0, Mth.sin(age * 0.16f) * 0.08f);
            turn(rightEar, 0, Mth.sin(age * 0.17f) * 0.12f, 0);
            turn(leftEar, 0, -Mth.sin(age * 0.17f + 0.6f) * 0.12f, 0);
            // its hair flickers like flame, streaming back as it swims
            for (int i = 0; i < hair.length; i++) {
                turn(hair[i], Mth.sin(age * 0.25f + i * 1.3f) * 0.08f + amount * (swimming ? 0.35f : 0.15f),
                        0, Mth.sin(age * 0.21f + i * 2.1f) * 0.07f);
            }
            // the trident: held ready; thrust forward on a blow, the arm driving out and straightening, the
            // trident levelled straight ahead along the way it faces, at its foe
            turn(rightArm, (1 - thrust) * (Mth.sin(age * 0.08f) * 0.03f + (swimming ? -0.15f : -kick * 0.2f)) - thrust * 0.95f, thrust * 0.15f, 0);
            turn(rightForearm, thrust * 0.75f, 0, 0);
            turn(rightHand, 0, 0, 0);
            level(thrust);
            turn(pennant, 0, Mth.sin(age * 0.2f) * 0.3f + amount * 0.3f, 0);
            // the free arm: its clawed hand raised, flexing; drawn back on a thrust, sweeping as it swims
            float sweep = swimming ? Mth.sin(age * 0.28f + 1.2f) * 0.25f : kick * 0.25f;
            turn(leftArm, Mth.sin(age * 0.07f) * 0.06f + strike * 0.55f + sweep, 0, 0);
            turn(leftForearm, Mth.sin(age * 0.07f + 0.7f) * 0.08f + strike * 0.3f, 0, 0);
            turn(leftHand, 0, 0, 0);
            float curl = 0.2f * (Mth.sin(age * 0.11f) + 1) * 0.5f + strike * 0.4f;
            for (int i = 0; i < 4; i++) turn(fingers[i], curl * (0.8f + 0.1f * i), 0, 0);
            // its fins ripple
            turn(dorsal, Mth.sin(age * 0.15f) * 0.05f + (swimming ? 0.1f * amount : 0), 0, 0);
            turn(rightArmFin, 0, Mth.sin(age * 0.2f) * 0.15f, 0);
            turn(leftArmFin, 0, -Mth.sin(age * 0.2f + 0.5f) * 0.15f, 0);
            // the scarf flows, a wave running down each end; it streams back as it moves
            for (int s = 0; s < 2; s++) {
                float sg = s == 0 ? 1 : -1;
                for (int i = 0; i < 3; i++) {
                    turn(scarf[s][i], Mth.sin(age * 0.12f - i * 0.8f + s * 1.7f) * (0.12f + 0.06f * i) + amount * (0.3f + 0.1f * i) + strike * 0.2f,
                            0, sg * Mth.sin(age * 0.09f - i * 0.7f + s) * (0.08f + 0.04f * i));
                }
            }
            turn(scarfBack, Mth.sin(age * 0.12f) * 0.05f, 0, 0);
            // the apron and the wrap swing with its legs
            turn(apron, -Math.abs(kick) * 0.3f + (swimming ? 0.2f : 0), 0, 0);
            turn(rightWrap, Math.min(0, -kick) * 0.4f, 0, 0);
            turn(leftWrap, Math.min(0, kick) * 0.4f, 0, 0);
            turn(wrapBack, Math.abs(kick) * 0.25f + (swimming ? 0.25f : 0), 0, 0);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
