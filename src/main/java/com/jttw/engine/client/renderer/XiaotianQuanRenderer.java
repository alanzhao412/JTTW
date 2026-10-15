package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.XiaotianQuanEntity;
import com.jttw.engine.client.model.XiaotianQuanModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class XiaotianQuanRenderer extends MobRenderer<XiaotianQuanEntity, XiaotianQuanModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/xiaotian_quan.png");

    public XiaotianQuanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new XiaotianQuanModel(ctx.bakeLayer(XiaotianQuanModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(XiaotianQuanEntity entity) {
        return TEXTURE;
    }
}
