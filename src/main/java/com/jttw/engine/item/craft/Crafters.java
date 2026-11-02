package com.jttw.engine.item.craft;

import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Who is crafting right now, on this thread: set round the crafting grid's result check and its
 * result being taken ({@code CraftingMenuMixin}, {@code ResultSlotMixin}), and by NeoForge round the
 * remainders. A gated recipe asks this; with no one known (a Crafter block, a machine) it never matches.
 */
public final class Crafters {

    private Crafters() {}

    private static final ThreadLocal<Player> CURRENT = new ThreadLocal<>();

    public static void set(@Nullable Player p) {
        if (p == null) CURRENT.remove();
        else CURRENT.set(p);
    }

    @Nullable
    public static Player current() {
        Player p = CURRENT.get();
        return p != null ? p : net.neoforged.neoforge.common.CommonHooks.getCraftingPlayer();
    }

    /** Runs {@code r} as {@code p} crafting (for tests and machines that know their crafter). */
    public static void as(@Nullable Player p, Runnable r) {
        Player was = CURRENT.get();
        set(p);
        try {
            r.run();
        } finally {
            set(was);
        }
    }
}
