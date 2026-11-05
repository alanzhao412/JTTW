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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
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

    /** The "moved too quickly" limit, raised for the mod's fliers at their top speed (see JTTWFlight.moveCheckLimit). */
    @ModifyConstant(method = "handleMovePlayer", constant = @Constant(floatValue = 100.0F))
    private float jttw_flightMoveLimit(float vanilla) {
        return com.jttw.engine.flight.JTTWFlight.moveCheckLimit(player, vanilla);
    }

    @Shadow private int aboveGroundTickCount;

    /** The player's fall distance at their last move. */
    private float jttw_lastFallDistance;

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void jttw_postMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        jttw_phasingMoveActive = false;
        // A fall is not flight. Vanilla takes every move through open air for floating (its drop is
        // zeroed before the test), so four seconds of falling kicked a player off a server without
        // allow-flight: off Heaven's islands, down Kunlun or Mount Wuzhuang. While their fall grows
        // they are falling, and the count starts again; one hanging still in the air grows none.
        if (!player.onGround() && player.fallDistance > jttw_lastFallDistance) aboveGroundTickCount = 0;
        jttw_lastFallDistance = player.fallDistance;
    }
}