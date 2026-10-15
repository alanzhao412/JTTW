package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * A glint of gold instead of the enchantment's purple: the same shimmering sheen drawn over an
 * item, from a texture of our own. The Ruyi Jingu Bang wears it when Qi-empowered.
 */
public final class GoldGlint extends RenderType {

    private GoldGlint(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/misc/gold_glint.png");

    /** Over an item in the hand, in a frame, on the ground. */
    public static final RenderType ITEM = create("jttw_gold_glint", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_GLINT_SHADER)
                    .setTextureState(new TextureStateShard(TEXTURE, true, false))
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setTexturingState(GLINT_TEXTURING)
                    .createCompositeState(false));

    /** Over an item drawn straight into the buffers (as some item renders are). */
    public static final RenderType DIRECT = create("jttw_gold_glint_direct", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_GLINT_SHADER) // (vanilla's direct glint shader is gone: one glint serves both)
                    .setTextureState(new TextureStateShard(TEXTURE, true, false))
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setTexturingState(GLINT_TEXTURING)
                    .createCompositeState(false));

    /** Whether the item being drawn right now wears the gold glint (set about each item's drawing). */
    public static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
}
