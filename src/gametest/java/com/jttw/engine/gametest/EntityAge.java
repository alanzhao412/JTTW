package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * Stamps every non-player entity with the game time it first joined the world (GameTest runs
 * only: this class lives in the gametest source set).
 *
 * <p>An entity's tick count is no measure of age here: mobs parked in a chunk that isn't
 * ticking don't age, and reappear when a later test loads that chunk. The join stamp is set
 * once and kept across reloads, so {@link GT} can tell a test's own mobs from leftovers.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class EntityAge {

    private EntityAge() {}

    static final String JOINED_AT = "jttw_gametest_joined_at";

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (event.getLevel().isClientSide() || e instanceof Player) return;
        if (!e.getPersistentData().contains(JOINED_AT)) {
            e.getPersistentData().putLong(JOINED_AT, event.getLevel().getGameTime());
        }
    }

    /** True if the entity joined before {@code gameTime}. Unstamped entities count as old. */
    static boolean joinedBefore(Entity e, long gameTime) {
        return !e.getPersistentData().contains(JOINED_AT) || e.getPersistentData().getLong(JOINED_AT) < gameTime;
    }
}
