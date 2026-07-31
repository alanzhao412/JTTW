package com.jttw.engine.mixin;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses server-side movement validation for phasing players.
 *
 * handleMovePlayer has two rejection paths we need to bypass:
 *   1. isInWall() check -> rollback
 *   2. distance threshold check -> only runs if isSpectator() is false
 *
 * We spoof isSpectator()=true ONLY during handleMovePlayer for phasing players.
 * This is a local spoof via a mixin field flag — no gamemode packet is sent,
 * the client never sees any gamemode change, and survival logic is unaffected.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerMixin {

    @Shadow public ServerPlayer player;

    private boolean jttw_phasingMoveActive = false;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void jttw_preMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        TransformationData data = TransformationManager.getTransformation(player);
        if (data != null && data.canPhase()) {
            jttw_phasingMoveActive = true;
            player.noPhysics = true;
        }
    }

    @Redirect(
            method = "handleMovePlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"),
            require = 0
    )
    private boolean jttw_spoofSpectator(ServerPlayer instance) {
        if (jttw_phasingMoveActive) return true;
        return instance.isSpectator();
    }

    @Redirect(
            method = "handleMovePlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInWall()Z"),
            require = 0
    )
    private boolean jttw_skipInWall(Entity instance) {
        if (jttw_phasingMoveActive && instance instanceof ServerPlayer) return false;
        return instance.isInWall();
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void jttw_postMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        jttw_phasingMoveActive = false;
    }
}