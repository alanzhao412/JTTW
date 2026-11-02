package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.XiaotianQuanEntity;
import com.jttw.engine.client.model.XiaotianQuanModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Xiaotian Quan: its amber eyes glow faintly, its gold markings a little (its glowing skin); berserk, the
 * markings and eyes turn crimson and burn, pulsing.
 */
@OnlyIn(Dist.CLIENT)
public class XiaotianQuanRenderer extends MobRenderer<XiaotianQuanEntity, XiaotianQuanModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/xiaotian_quan.png");
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/xiaotian_quan_glow.png");
    private static final ResourceLocation BERSERK =
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/xiaotian_quan_berserk.png");
    private static final ResourceLocation BERSERK_GLOW =
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/xiaotian_quan_berserk_glow.png");

    public XiaotianQuanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new XiaotianQuanModel(ctx.bakeLayer(XiaotianQuanModel.LAYER_LOCATION)), 0.5f);
        addLayer(new BerserkGlow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(XiaotianQuanEntity entity) {
        return entity.isBerserk() ? BERSERK : TEXTURE;
    }

    /** Its eyes (and, faintly, its markings) shining; the berserk hound's markings full bright and pulsing, so they burn even in the dark. */
    private static final class BerserkGlow extends RenderLayer<XiaotianQuanEntity, XiaotianQuanModel> {

        BerserkGlow(XiaotianQuanRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, XiaotianQuanEntity hound,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (hound.isInvisible()) return;
            if (!hound.isBerserk()) {
                getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
                return;
            }
            // the eyes render type adds its colour, so the pulse is in the brightness
            int v = (int) (255 * (0.65f + 0.35f * Mth.sin(ageInTicks * 0.3f)));
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(BERSERK_GLOW)),
                    0xF000F0, OverlayTexture.NO_OVERLAY, 0xFF000000 | (v << 16) | (v << 8) | v);
        }
    }
}
