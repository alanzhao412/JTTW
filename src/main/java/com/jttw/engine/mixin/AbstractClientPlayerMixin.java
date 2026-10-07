package com.jttw.engine.mixin;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Player forms (72 Transformations, level III): a player disguised as another wears that
 * player's skin, model type (wide/slim) and cape. Falls back to the default skin for that
 * UUID if the target isn't in the tab list.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Unique
    private UUID jttw_disguise() {
        TransformationData data = TransformationManager.getTransformation((AbstractClientPlayer) (Object) this);
        return data != null && data.isPlayerForm() ? data.getDisguiseId() : null;
    }

    @Unique
    private static PlayerInfo jttw_info(UUID id) {
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getPlayerInfo(id);
    }

    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    private void jttw_disguiseSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        UUID id = jttw_disguise();
        if (id == null) return;
        PlayerInfo info = jttw_info(id);
        cir.setReturnValue(info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id));
    }

    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
    private void jttw_disguiseModel(CallbackInfoReturnable<String> cir) {
        UUID id = jttw_disguise();
        if (id == null) return;
        PlayerInfo info = jttw_info(id);
        cir.setReturnValue(info != null ? info.getModelName() : DefaultPlayerSkin.getSkinModelName(id));
    }

    @Inject(method = "getCloakTextureLocation", at = @At("HEAD"), cancellable = true)
    private void jttw_disguiseCape(CallbackInfoReturnable<ResourceLocation> cir) {
        UUID id = jttw_disguise();
        if (id == null) return;
        PlayerInfo info = jttw_info(id);
        cir.setReturnValue(info != null ? info.getCapeLocation() : null);
    }
}
