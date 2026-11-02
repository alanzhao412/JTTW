package com.jttw.engine.item.craft;

import com.jttw.engine.JTTWEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Puts the gated recipes a character may craft in their recipe book (an armament's tier I, a beast's
 * offerings and altar, the Heavenly's talismans): at login, and as they awaken as their character.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class GatedRecipeBook {

    private GatedRecipeBook() {}

    public static void award(ServerPlayer p) {
        if (p.getServer() == null) return;
        List<RecipeHolder<?>> mine = new ArrayList<>();
        for (var r : p.getServer().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (r.value() instanceof GatedRecipe g && !r.value().isSpecial() && g.gate().allows(p)) mine.add(r);
        }
        if (!mine.isEmpty()) p.awardRecipes(mine);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) award(p);
    }
}
