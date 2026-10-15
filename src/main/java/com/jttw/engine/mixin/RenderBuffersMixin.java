package com.jttw.engine.mixin;

import com.jttw.engine.client.renderer.GoldGlint;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.SequencedMap;

/**
 * The gold glint gets buffers of its own among the game's fixed ones, after all the rest, as the
 * enchantment glint has: so it is drawn after (and over) the item it lies on, and drawing the item
 * never closes it half-written. (The fixed buffers are a local map handed to the buffer source
 * now, so they are added there, after vanilla's and any added by RegisterRenderBuffersEvent.)
 */
@Mixin(RenderBuffers.class)
public class RenderBuffersMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource;immediateWithBuffers(Ljava/util/SequencedMap;Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;)Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"),
            index = 0)
    private SequencedMap<RenderType, ByteBufferBuilder> jttw$goldGlintBuffers(SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers) {
        fixedBuffers.put(GoldGlint.ITEM, new ByteBufferBuilder(GoldGlint.ITEM.bufferSize()));
        fixedBuffers.put(GoldGlint.DIRECT, new ByteBufferBuilder(GoldGlint.DIRECT.bufferSize()));
        return fixedBuffers;
    }
}
