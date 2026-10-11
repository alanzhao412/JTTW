package com.jttw.engine.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * An item held to charge and let go to strike: how charged it is, after some ticks of holding, by
 * the same measure its release uses. The charge meter (client.ChargeMeter) shows it.
 */
public interface Charged {

    /**
     * How charged this use is, {@code ticks} into it, for {@code user} with these skill nodes;
     * null if this use does not charge.
     */
    @Nullable
    Charge charge(ItemStack stack, Player user, int ticks, Set<String> nodes);

    /**
     * How far toward full it is (0 to 1), and whether letting go now would strike at all.
     */
    record Charge(float fraction, boolean ready) {

        /** From {@code min} ticks (just ready, 0) to {@code full} (1). */
        public static Charge between(int ticks, int min, int full) {
            return new Charge(Math.max(0f, Math.min(1f, (ticks - min) / (float) (full - min))), ticks >= min);
        }
    }
}
