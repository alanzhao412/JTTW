package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.furnace.TrigramsFurnaceBlock;
import com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Draws the great Eight Trigrams Furnace whole from its core: its eight-sided body with the trigram
 * panels, three blocks across and five high, glowing from its mouth when lit, and the hatch's
 * cover when it is shut.
 */
public class TrigramsFurnaceRenderer implements BlockEntityRenderer<TrigramsFurnaceBlockEntity> {

    public static final ResourceLocation GREAT = new ResourceLocation(JTTWEngine.MOD_ID, "block/eight_trigrams_furnace_great");
    public static final ResourceLocation GREAT_LIT = new ResourceLocation(JTTWEngine.MOD_ID, "block/eight_trigrams_furnace_great_lit");
    public static final ResourceLocation HATCH = new ResourceLocation(JTTWEngine.MOD_ID, "block/eight_trigrams_furnace_hatch");

    public TrigramsFurnaceRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TrigramsFurnaceBlockEntity furnace, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = furnace.getLevel();
        if (level == null) return;
        boolean lit = furnace.getBlockState().getValue(TrigramsFurnaceBlock.LIT);
        // its light from above it (its core sits in shadow between its legs); lit, its own glow lights it
        int seen = LevelRenderer.getLightColor(level, furnace.getBlockPos().above(5));
        var models = Minecraft.getInstance().getModelManager();
        pose.pushPose();
        pose.translate(-1, 0, -1);
        pose.scale(3, 5, 3);
        draw(pose, buffers, models.getModel(lit ? GREAT_LIT : GREAT), seen, overlay);
        if (!furnace.hatchOpen()) draw(pose, buffers, models.getModel(HATCH), seen, overlay);
        pose.popPose();
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, BakedModel model, int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
                null, model, 1f, 1f, 1f, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    @Override
    public boolean shouldRenderOffScreen(TrigramsFurnaceBlockEntity furnace) {
        return true; // it stands well beyond its core's block
    }
}
