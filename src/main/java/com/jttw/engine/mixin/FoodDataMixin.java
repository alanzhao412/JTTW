package com.jttw.engine.mixin;

import com.jttw.engine.character.marshal.MarshalAbilityHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the Marshal of the Heavenly Canopy's second stomach catch what overflows the first.
 * Hooks Forge's eater-aware {@code eat} (not in vanilla, so not remapped), seeing the food level
 * before the meal lands.
 */
@Mixin(FoodData.class)
public class FoodDataMixin {

    @Shadow private int foodLevel;

    @Inject(
            method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void jttw_overeat(Item item, ItemStack stack, LivingEntity entity, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayer player) || !item.isEdible()) return;
        FoodProperties food = stack.getFoodProperties(entity);
        if (food != null) MarshalAbilityHandler.overeat(player, foodLevel, food.getNutrition());
    }
}
