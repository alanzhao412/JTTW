package com.jttw.engine.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.authlib.GameProfile;

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
     * <p>A {@link TestPlayer}, not vanilla's {@code makeMockServerPlayerInLevel()}: its packets
     * go nowhere, it does not tick by itself, and it cannot be damaged, so tests drive ticks and
     * hit mobs rather than the player.
     */
    public static ServerPlayer player(GameTestHelper helper) {
        sweepStrays(helper);
        onEnd(helper, () -> clearPlatform(helper));
        GameProfile profile = new GameProfile(UUID.randomUUID(), "jttw_test_" + PLAYERS.incrementAndGet());
        requireEntityReady(helper);
        ServerPlayer p = new TestPlayer(helper.getLevel(), profile);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        p.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return p;
    }

    private static final AtomicInteger PLAYERS = new AtomicInteger();

    /**
     * The fake player the tests were written against, Forge 1.20.1's: its packets go nowhere,
     * it ignores teleport packets, it does not tick by itself and cannot be hurt, but it opens
     * menus, rides, and keeps its advancements (in memory). NeoForge 1.21.1's FakePlayer refuses
     * menus and riding and forgets every advancement criterion, which the forms' menus, boat
     * forms and reincarnation rules all need.
     */
    static final class TestPlayer extends ServerPlayer {
        private net.minecraft.server.PlayerAdvancements advancements;

        TestPlayer(net.minecraft.server.level.ServerLevel level, GameProfile profile) {
            super(level.getServer(), level, profile, net.minecraft.server.level.ClientInformation.createDefault());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            // NeoForge asks every listed player's channel which payloads it takes
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            new Silent(level.getServer(), connection, this);
        }

        @Override
        public boolean isFakePlayer() {
            return true;
        }

        @Override
        public void awardStat(net.minecraft.stats.Stat<?> stat, int amount) {}

        @Override
        public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
            return true;
        }

        @Override
        public boolean canHarmPlayer(net.minecraft.world.entity.player.Player player) {
            return false;
        }

        @Override
        public void die(net.minecraft.world.damagesource.DamageSource source) {}

        @Override
        public void tick() {}

        @Override
        public void updateOptions(net.minecraft.server.level.ClientInformation info) {}

        @Override
        public net.minecraft.server.PlayerAdvancements getAdvancements() {
            if (advancements == null) {
                var server = serverLevel().getServer();
                // a file that does not exist: nothing is loaded, and nothing is ever saved to it
                java.nio.file.Path none = server.getServerDirectory().resolve("jttw-gametest-advancements")
                        .resolve(getUUID() + ".json");
                advancements = new net.minecraft.server.PlayerAdvancements(server.getFixerUpper(), server.getPlayerList(),
                        server.getAdvancements(), none, this);
            }
            return advancements;
        }
    }

    /** A test player's connection: sends nothing and ignores teleports, as Forge's fake player's did. */
    private static final class Silent extends net.minecraft.server.network.ServerGamePacketListenerImpl {
        Silent(net.minecraft.server.MinecraftServer server, net.minecraft.network.Connection connection, ServerPlayer player) {
            super(server, connection, player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }

        @Override
        public void send(net.minecraft.network.protocol.Packet<?> packet) {}

        @Override
        public void send(net.minecraft.network.protocol.Packet<?> packet, @org.jetbrains.annotations.Nullable net.minecraft.network.PacketSendListener listener) {}

        @Override
        public void teleport(double x, double y, double z, float yaw, float pitch) {}

        @Override
        public void teleport(double x, double y, double z, float yaw, float pitch,
                             java.util.Set<net.minecraft.world.entity.RelativeMovement> relative) {}
    }

    /** Runs {@code cleanup} when the test ends, whether it passes or fails. */
    public static void onEnd(GameTestHelper helper, Runnable cleanup) {
        try {
            java.lang.reflect.Field f = GameTestHelper.class.getDeclaredField("testInfo");
            f.setAccessible(true);
            net.minecraft.gametest.framework.GameTestInfo info = (net.minecraft.gametest.framework.GameTestInfo) f.get(helper);
            info.addListener(new net.minecraft.gametest.framework.GameTestListener() {
                @Override public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo i) {}
                @Override public void testPassed(net.minecraft.gametest.framework.GameTestInfo i,
                                                 net.minecraft.gametest.framework.GameTestRunner r) { cleanup.run(); }
                @Override public void testFailed(net.minecraft.gametest.framework.GameTestInfo i,
                                                 net.minecraft.gametest.framework.GameTestRunner r) { cleanup.run(); }
                @Override public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo old,
                                                        net.minecraft.gametest.framework.GameTestInfo rerun,
                                                        net.minecraft.gametest.framework.GameTestRunner r) {}
            });
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot hook test end", e);
        }
    }

    /** This test's platform, as an absolute box (floor at relative y=1, five blocks of air above). */
    private static net.minecraft.world.phys.AABB platform(GameTestHelper helper) {
        return new net.minecraft.world.phys.AABB(
                net.minecraft.world.phys.Vec3.atLowerCornerOf(helper.absolutePos(new net.minecraft.core.BlockPos(0, 1, 0))),
                net.minecraft.world.phys.Vec3.atLowerCornerOf(helper.absolutePos(new net.minecraft.core.BlockPos(5, 7, 5))));
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
        var p = e.getAttribute(com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE);
        p.removeModifiers();
        p.setBaseValue(pres);
        var m = e.getAttribute(com.jttw.engine.registry.ModAttributes.MAGIC_RESISTANCE);
        m.removeModifiers();
        m.setBaseValue(mres);
    }

    public static double pres(net.minecraft.world.entity.LivingEntity e) {
        return e.getAttributeValue(com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE);
    }

    public static double mres(net.minecraft.world.entity.LivingEntity e) {
        return e.getAttributeValue(com.jttw.engine.registry.ModAttributes.MAGIC_RESISTANCE);
    }

    /** The damage left after the resistance handler sees it. */
    public static float afterResistance(net.minecraft.world.entity.LivingEntity e,
                                        net.minecraft.world.damagesource.DamageSource source, float amount) {
        var event = incoming(e, source, amount);
        com.jttw.engine.progression.stats.ResistanceHandler.onHurt(event);
        return event.getAmount();
    }

    /**
     * Where an entity really stands, for failure messages: whether the level knows it, whether
     * its chunk ticks entities, its age stamp, and what else is within 2 blocks of it.
     */
    public static String whereabouts(GameTestHelper h, net.minecraft.world.entity.Entity e) {
        var level = h.getLevel();
        var others = level.getEntities(e, e.getBoundingBox().inflate(2.0), x -> true).stream()
                .map(x -> x.getType().toShortString() + "@" + h.relativePos(x.blockPosition())
                        + (x.getPersistentData().contains(EntityAge.JOINED_AT)
                            ? " joined " + x.getPersistentData().getLong(EntityAge.JOINED_AT) : " unstamped"))
                .toList();
        return "alive=" + e.isAlive() + " removed=" + e.getRemovalReason()
                + " inLevel=" + (level.getEntity(e.getUUID()) == e) + " addedToWorld=" + e.isAddedToLevel()
                + " entityTicking=" + level.isPositionEntityTicking(e.blockPosition())
                + " pos=" + h.relativePos(e.blockPosition())
                + " joined=" + e.getPersistentData().getLong(EntityAge.JOINED_AT)
                + " now=" + level.getGameTime() + " testTick=" + h.getTick()
                + " nearby=" + others;
    }

    /**
     * True once every chunk under this test's platform can hold entities: entities loaded and
     * ticking. A test's platform is placed (and its chunks force-loaded) the moment it starts,
     * but fresh chunks load their entities a few ticks later. Until then a mob spawned there
     * exists but is invisible to area searches (a cleave, a blast, an aura) and does not tick,
     * so a test would miss it at random. {@link ReadyGate} holds every test until this is true.
     */
    public static boolean entityReady(GameTestHelper helper) {
        return notReadyChunks(helper).isEmpty();
    }

    private static String notReadyChunks(GameTestHelper helper) {
        var level = helper.getLevel();
        StringBuilder bad = new StringBuilder();
        for (int x = 0; x <= 5; x += 5) for (int z = 0; z <= 5; z += 5) {
            var pos = helper.absolutePos(new net.minecraft.core.BlockPos(x, 2, z));
            long chunk = net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            boolean loaded = level.areEntitiesLoaded(chunk);
            boolean ticking = level.isPositionEntityTicking(pos);
            if (!loaded || !ticking) bad.append(" (").append(x).append(",").append(z).append(") entities loaded=")
                    .append(loaded).append(" ticking=").append(ticking);
        }
        return bad.toString();
    }

    /**
     * Takes down the barrier walls and roof that 1.21's GameTest framework builds round every
     * test structure (1.20.1 built none), for tests whose mobs stand at or are thrown to the
     * platform's edge, or whose bodies are wider than it.
     */
    public static void openAir(GameTestHelper helper) {
        net.minecraft.gametest.framework.StructureUtils.removeBarriers(helper.getBounds(), helper.getLevel());
    }

    /**
     * Fails the test straight away, saying why, if a chunk under its platform cannot hold
     * entities yet. {@link ReadyGate} should make this impossible; this keeps a regression loud.
     */
    public static void requireEntityReady(GameTestHelper helper) {
        String bad = notReadyChunks(helper);
        if (!bad.isEmpty()) {
            helper.fail("platform chunks not ready for entities at test tick " + helper.getTick() + ":" + bad
                    + " (is ReadyGate installed?)");
        }
    }

    /** The event a hit fires before armour and resistances (Forge's LivingAttackEvent and LivingHurtEvent). */
    public static net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent incoming(
            net.minecraft.world.entity.LivingEntity e, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(e,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(source, amount));
    }

    /** The event a hit fires after armour and resistances (Forge's LivingDamageEvent). */
    public static net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre damage(
            net.minecraft.world.entity.LivingEntity e, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(e,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(source, amount));
    }
}
