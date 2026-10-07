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
 * <p>Every test runs in the {@code jttw_engine:platform} structure (a 5x5 stone floor, which
 * sits at relative y=1, so players and mobs stand at y=2) and
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
        sweepStrays(helper);
        onEnd(helper, () -> clearPlatform(helper));
        GameProfile profile = new GameProfile(UUID.randomUUID(), "jttw_test_" + PLAYERS.incrementAndGet());
        ServerPlayer p = new FakePlayer(helper.getLevel(), profile);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        p.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return p;
    }

    private static final AtomicInteger PLAYERS = new AtomicInteger();

    /** Runs {@code cleanup} when the test ends, whether it passes or fails. */
    public static void onEnd(GameTestHelper helper, Runnable cleanup) {
        try {
            java.lang.reflect.Field f = GameTestHelper.class.getDeclaredField("testInfo");
            f.setAccessible(true);
            net.minecraft.gametest.framework.GameTestInfo info = (net.minecraft.gametest.framework.GameTestInfo) f.get(helper);
            info.addListener(new net.minecraft.gametest.framework.GameTestListener() {
                @Override public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo i) {}
                @Override public void testPassed(net.minecraft.gametest.framework.GameTestInfo i) { cleanup.run(); }
                @Override public void testFailed(net.minecraft.gametest.framework.GameTestInfo i) { cleanup.run(); }
            });
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot hook test end", e);
        }
    }

    /** This test's platform, as an absolute box (floor at relative y=1, five blocks of air above). */
    private static net.minecraft.world.phys.AABB platform(GameTestHelper helper) {
        return new net.minecraft.world.phys.AABB(
                helper.absolutePos(new net.minecraft.core.BlockPos(0, 1, 0)),
                helper.absolutePos(new net.minecraft.core.BlockPos(5, 7, 5)));
    }

    /**
     * Removes everything but players from this test's platform when the test ends, while its
     * chunk is still loaded. Without it, mobs outlive their test: iron golems never despawn, and
     * the framework's own clearing misses entities in chunks that aren't loaded at that moment,
     * so a later test in the same slot finds an old golem standing on its target's spot.
     */
    private static void clearPlatform(GameTestHelper helper) {
        helper.getLevel().getEntities((net.minecraft.world.entity.Entity) null, platform(helper).inflate(2.0),
                        e -> !(e instanceof net.minecraft.world.entity.player.Player))
                .forEach(net.minecraft.world.entity.Entity::discard);
    }

    /**
     * Removes mobs left over from earlier tests that are on this test's platform.
     *
     * <p>Leftovers come from tests that never reach {@link #clearPlatform} (mob-only tests) and
     * from mobs knocked off their platform that wander back; iron golems never despawn. A stray
     * in front of the player intercepts strikes and beams aimed at the test's own target.
     * Anything that joined the world before this test started cannot be this test's
     * ({@link EntityAge}), so only those go, and only from this test's own 5x5 platform.
     */
    private static void sweepStrays(GameTestHelper helper) {
        long testStart = helper.getLevel().getGameTime() - helper.getTick();
        helper.getLevel().getEntities((net.minecraft.world.entity.Entity) null, platform(helper),
                        e -> !(e instanceof net.minecraft.world.entity.player.Player) && EntityAge.joinedBefore(e, testStart))
                .forEach(net.minecraft.world.entity.Entity::discard);
    }

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

    /** Resistances fixed at these values: modifiers cleared, base set. */
    public static void setResistances(net.minecraft.world.entity.LivingEntity e, double pres, double mres) {
        var p = e.getAttribute(com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get());
        p.removeModifiers();
        p.setBaseValue(pres);
        var m = e.getAttribute(com.jttw.engine.registry.ModAttributes.MAGIC_RESISTANCE.get());
        m.removeModifiers();
        m.setBaseValue(mres);
    }

    public static double pres(net.minecraft.world.entity.LivingEntity e) {
        return e.getAttributeValue(com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get());
    }

    public static double mres(net.minecraft.world.entity.LivingEntity e) {
        return e.getAttributeValue(com.jttw.engine.registry.ModAttributes.MAGIC_RESISTANCE.get());
    }

    /** The damage left after the resistance handler sees it. */
    public static float afterResistance(net.minecraft.world.entity.LivingEntity e,
                                        net.minecraft.world.damagesource.DamageSource source, float amount) {
        var event = new net.minecraftforge.event.entity.living.LivingHurtEvent(e, source, amount);
        com.jttw.engine.progression.stats.ResistanceHandler.onHurt(event);
        return event.getAmount();
    }
}
