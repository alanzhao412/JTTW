package com.jttw.engine.mixin;

import com.jttw.engine.client.renderer.GoldGlint;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedMap;

/**
 * The gold glint gets buffers of its own among the game's fixed ones, after all the rest, as the
 * enchantment glint has: so it is drawn after (and over) the item it lies on, and drawing the item
 * never closes it half-written.
 */
@Mixin(RenderBuffers.class)
public class RenderBuffersMixin {

    @Shadow @Final private SortedMap<RenderType, BufferBuilder> fixedBuffers;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jttw$goldGlintBuffers(CallbackInfo ci) {
        fixedBuffers.put(GoldGlint.ITEM, new BufferBuilder(GoldGlint.ITEM.bufferSize()));
        fixedBuffers.put(GoldGlint.DIRECT, new BufferBuilder(GoldGlint.DIRECT.bufferSize()));
    }
}
