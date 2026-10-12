package com.jttw.engine.mixin;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Set;

/**
 * Third person, the camera stands back as far as the player's shape needs: vanilla's four blocks
 * for a player's size, further for a larger form, and far further for a divine beast's true form,
 * which is drawn twice its body's size. (It still closes in where a wall is in the way.)
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    /** The true forms drawn about twice their bodies (and the tiger, drawn a little past its own). */
    private static final Set<String> DRAWN_LARGE = Set.of(
            "jttw_engine:azure_dragon", "jttw_engine:black_tortoise", "jttw_engine:vermilion_bird");

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"), index = 0)
    private double jttw_standBack(double distance) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return distance;
        TransformationData data = TransformationManager.getTransformation(p);
        if (data == null || data.getEntityId() == null) return distance;
        double size = Math.max(Math.max(data.getHeight(), data.getWidth()), data.getEyeHeight());
        double scaled = size * 2.2 * (DRAWN_LARGE.contains(data.getEntityId().toString()) ? 1.9 : 1.0);
        return Math.max(distance, scaled);
    }
}
