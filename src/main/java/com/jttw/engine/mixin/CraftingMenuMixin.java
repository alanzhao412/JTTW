package com.jttw.engine.mixin;

import com.jttw.engine.item.craft.Crafters;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The crafting grid (a table's or the inventory's) tells gated recipes who is crafting ({@link Crafters}). */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"))
    private static void jttw$crafterIn(AbstractContainerMenu menu, Level level, Player player, CraftingContainer grid,
                                       ResultContainer result, RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        Crafters.set(player);
    }

    @Inject(method = "slotChangedCraftingGrid", at = @At("RETURN"))
    private static void jttw$crafterOut(AbstractContainerMenu menu, Level level, Player player, CraftingContainer grid,
                                        ResultContainer result, RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        Crafters.set(null);
    }
}
