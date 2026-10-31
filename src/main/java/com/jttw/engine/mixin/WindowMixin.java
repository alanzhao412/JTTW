package com.jttw.engine.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The dev clients that run on their own (the form gallery, the worldgen bench, the play-test pass,
 * with {@code -Djttw.backgroundWindow=true}) open behind whatever the developer is doing: the
 * window is never shown, takes no focus, and is put off the screen. It still draws (and its
 * screenshots, read from the game's own framebuffer, come out whole); the runs themselves already
 * keep the game from pausing when it lacks focus.
 *
 * <p>NeoForge makes the window itself ({@code ImmediateWindowHandler.setupMinecraftWindow}), from
 * the hints vanilla has just set; with its early loading window on, it was made before any mod
 * loaded, so those runs turn the early window off (build.gradle writes their fml.toml).
 */
@Mixin(Window.class)
public abstract class WindowMixin {

    private static boolean jttw$background() {
        return Boolean.getBoolean("jttw.backgroundWindow");
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"))
    private void jttw$noFocusOnCreate(CallbackInfo ci) {
        if (!jttw$background()) return;
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        // never shown at all: an unfocused window still pulls a fullscreen game out of its
        // fullscreen when it appears; the game draws into its own framebuffer either way
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void jttw$offScreen(CallbackInfo ci) {
        if (!jttw$background()) return;
        GLFW.glfwSetWindowPos(((Window) (Object) this).getWindow(), -10000, -10000);
    }
}
