package com.jttw.engine.item.craft;

import com.jttw.engine.item.Armaments;
import com.jttw.engine.registry.ModRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

/**
 * Where an armament's next tier is made, and from what: read from the recipes themselves (the
 * crafting table's {@link RefineRecipe}, the furnace's {@link TrigramsRefining}), for its tooltip, the
 * furnace's screen and the tests.
 */
public final class ArmamentGuide {

    private ArmamentGuide() {}

    /** The next tier's recipe: where, what it takes, and who may make it. */
    public record Next(int tier, boolean furnace, String materials, ClassGate gate) {
        public String station() {
            return furnace ? "the Eight Trigrams Furnace" : "a crafting table";
        }
    }

    /** The recipe that takes this armament to its next tier, or null (at VIII, or none known). */
    @Nullable
    public static Next next(RecipeManager recipes, ItemStack armament) {
        Armaments.Armament a = Armaments.of(armament);
        if (a == null) return null;
        int to = Armaments.tier(armament) + 1;
        if (to > Armaments.TIERS) return null;
        for (var r : recipes.getAllRecipesFor(RecipeType.CRAFTING)) {
            if (r.value() instanceof RefineRecipe rr && rr.tier() == to && rr.refines(a.id())) return new Next(to, false, rr.describe(), rr.gate());
        }
        for (var r : recipes.getAllRecipesFor(ModRecipes.TRIGRAMS_REFINING.get())) {
            TrigramsRefining tr = r.value();
            if (tr.tier() == to && tr.refines(a.id())) return new Next(to, true, tr.describe(), tr.gate());
        }
        return null;
    }
}
