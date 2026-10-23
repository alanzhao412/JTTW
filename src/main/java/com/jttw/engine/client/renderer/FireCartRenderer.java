package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.fire.FireCartEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * One of Red Boy's five little carts: a two-wheeled cart of red lacquer, gold at its rim and on its
 * wheels, its shafts resting on the ground, heaped with the Samadhi fire, which flickers. Model and
 * skin: {@code assets_source/fire_court.py}.
 */
public final class FireCartRenderer extends MobRenderer<FireCartEntity, FireCartRenderer.Model> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTTWEngine.id("fire_cart"), "main");
    public static final ResourceLocation TEXTURE = JTTWEngine.id("textures/entity/fire_cart.png");

    public FireCartRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(LAYER)), 0.7f);
        addLayer(FireCourt.glow(this, "fire_cart"));
    }

    @Override
    public ResourceLocation getTextureLocation(FireCartEntity e) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(FireCartEntity e, BlockPos pos) {
        return 15;
    }

    @Override
    protected boolean shouldShowName(FireCartEntity e) {
        return false;
    }

    public static LayerDefinition layer() {
        // GENERATED: begin
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 25).mirror(false).addBox(-7, -2, -7, 14, 4, 14)
                .texOffs(27, 44).mirror(false).addBox(-7, -6, -7, 14, 4, 1)
                .texOffs(27, 44).mirror(false).addBox(-7, -6, 6, 14, 4, 1)
                .texOffs(57, 25).mirror(false).addBox(-7, -6, -6, 1, 4, 12)
                .texOffs(57, 25).mirror(false).addBox(6, -6, -6, 1, 4, 12), PartPose.offsetAndRotation(0, 13, 0, 0, 0, 0));
        PartDefinition flames = body.addOrReplaceChild("flames", CubeListBuilder.create()
                .texOffs(29, 0).mirror(false).addBox(-6, -6, -6, 12, 7, 12), PartPose.offsetAndRotation(0, -6, 0, 0, 0, 0));
        PartDefinition flamesHigh = flames.addOrReplaceChild("flames_high", CubeListBuilder.create()
                .texOffs(84, 25).mirror(false).addBox(-3.5f, -8, -3.5f, 7, 8, 7), PartPose.offsetAndRotation(0, -5, 0, 0, 0, 0));
        PartDefinition rightWheel = root.addOrReplaceChild("right_wheel", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1, -6, -6, 2, 12, 12), PartPose.offsetAndRotation(-8, 18, 0, 0, 0, 0));
        PartDefinition leftWheel = root.addOrReplaceChild("left_wheel", CubeListBuilder.create()
                .texOffs(0, 0).mirror(true).addBox(-1, -6, -6, 2, 12, 12), PartPose.offsetAndRotation(8, 18, 0, 0, 0, 0));
        PartDefinition shafts = root.addOrReplaceChild("shafts", CubeListBuilder.create()
                .texOffs(0, 44).mirror(false).addBox(-5, 0, -12, 1, 1, 12)
                .texOffs(0, 44).mirror(false).addBox(4, 0, -12, 1, 1, 12), PartPose.offsetAndRotation(0, 14, -7, 0.6f, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
        // GENERATED: end
    }

    public static final class Model extends EntityModel<FireCartEntity> {
        private final ModelPart root, flames, flamesHigh;

        public Model(ModelPart root) {
            this.root = root;
            flames = root.getChild("body").getChild("flames");
            flamesHigh = flames.getChild("flames_high");
        }

        @Override
        public void setupAnim(FireCartEntity e, float swing, float amount, float age, float yaw, float pitch) {
            flames.yScale = 1f + Mth.sin(age * 0.45f) * 0.12f;
            flamesHigh.yRot = Mth.sin(age * 0.3f) * 0.3f;
            flamesHigh.yScale = 1f + Mth.sin(age * 0.6f + 1f) * 0.2f;
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            root.render(pose, vc, light, overlay, color);
        }
    }
}
