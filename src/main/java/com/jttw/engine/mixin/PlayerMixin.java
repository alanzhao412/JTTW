package com.jttw.engine.mixin;

import com.jttw.engine.character.marshal.MarshalAbilityHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Marshal of the Heavenly Canopy's stomachs. (A transformed player's dimensions and eye
 * height, which were overridden here, are LivingEntityMixin's now.)
 */
@Mixin(Player.class)
public class PlayerMixin {

    /** The Marshal of the Heavenly Canopy's bottomless stomach: he can always eat. */
    @Inject(method = "canEat(Z)Z", at = @At("HEAD"), cancellable = true)
    private void jttw_bottomlessStomach(boolean canAlwaysEat, CallbackInfoReturnable<Boolean> cir) {
        if (MarshalAbilityHandler.canAlwaysEat((Player) (Object) this)) cir.setReturnValue(true);
    }

    /**
     * Lets the Marshal's second stomach catch what overflows the first, seeing the food level
     * before the meal lands. (This hooked FoodData's eater-aware eat before, which is gone: the
     * player's eat is where the meal, its eater and the food level all meet now.)
     */
    @Inject(
            method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD")
    )
    private void jttw_overeat(Level level, ItemStack stack, FoodProperties food, CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object) this instanceof ServerPlayer player) || food == null) return;
        MarshalAbilityHandler.overeat(player, player.getFoodData().getFoodLevel(), food.nutrition());
    }
}
