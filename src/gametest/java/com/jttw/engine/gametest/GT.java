package com.jttw.engine.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.authlib.GameProfile;
import net.minecraftforge.common.util.FakePlayer;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared helpers for the GameTests.
 *
 * <p>Every test runs in the {@code jttw_engine:platform} structure (a 5x5 stone floor) and
 * gets a fresh mock server player, so no test sees another's saved data.
 */
public final class GT {

    private GT() {}

    /** Structure every test runs in. */
    public static final String PLATFORM = "platform";

    /**
     * A new server-side player standing on the platform, with its own empty saved data.
     *
     * <p>A Forge {@link FakePlayer}: vanilla's {@code makeMockServerPlayerInLevel()} gives the
     * player a connection with no channel, and Forge's login code dereferences it. A fake
     * player's packets go nowhere, it does not tick by itself, and it cannot be damaged, so
     * tests drive ticks and hit mobs rather than the player.
     */
    public static ServerPlayer player(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "jttw_test_" + PLAYERS.incrementAndGet());
        ServerPlayer p = new FakePlayer(helper.getLevel(), profile);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        p.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return p;
    }

    private static final AtomicInteger PLAYERS = new AtomicInteger();

    public static void check(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(condition, message);
    }

    public static void eq(GameTestHelper helper, Object expected, Object actual, String what) {
        helper.assertTrue(Objects.equals(expected, actual),
                what + ": expected " + expected + " but was " + actual);
    }

    public static void near(GameTestHelper helper, double expected, double actual, String what) {
        helper.assertTrue(Math.abs(expected - actual) < 1e-4,
                what + ": expected " + expected + " but was " + actual);
    }
}
