package com.jttw.engine.mixin;

import com.jttw.engine.character.wukong.BuddhaVanish;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Fighting Buddha vanished: no other player's game is told he is there at all (no body, armour,
 * held item, name, shadow, particles or sounds of his own), as a spectator is kept from the living.
 * The server still knows him; see {@link BuddhaVanish}.
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerVanishMixin {

    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void jttw_vanished(ServerPlayer watcher, CallbackInfoReturnable<Boolean> cir) {
        if (BuddhaVanish.hiddenFrom((ServerPlayer) (Object) this, watcher)) cir.setReturnValue(false);
    }
}
