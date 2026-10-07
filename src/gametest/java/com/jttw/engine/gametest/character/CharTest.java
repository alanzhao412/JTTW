package com.jttw.engine.gametest.character;

import com.jttw.engine.gametest.GT;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Helpers for the character ability GameTests.
 *
 * <p>The test player is a Forge FakePlayer: it never ticks, cannot be hurt, and is not in the
 * server's player list. Abilities that run from a player tick are driven with {@link #tick};
 * abilities whose server-tick loop looks the player up in the player list need {@link #online}.
 */
public final class CharTest {

    private CharTest() {}

    // ── Player setup ─────────────────────────────────────────────────

    /** A fresh player who reincarnated as {@code outcome}, with that class at level 1. */
    public static ServerPlayer as(GameTestHelper h, ReincarnationOutcome outcome) {
        ServerPlayer p = GT.player(h);
        AlignmentData.markReincarnated(p, outcome);
        LevelManager.initClass(p, outcome);
        return p;
    }

    public static void qi(ServerPlayer p, float current, float max) {
        QiData.setMax(p, max);
        QiData.setCurrent(p, current);
    }

    public static void grant(ServerPlayer p, String... nodes) {
        for (String n : nodes) SkillTreeData.grantNode(p, n);
    }

    /** Advances the player's tick counter and runs one END-phase player tick through {@code handler}. */
    public static void tick(ServerPlayer p, Consumer<TickEvent.PlayerTickEvent> handler, int times) {
        for (int i = 0; i < times; i++) {
            p.tickCount++;
            handler.accept(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
        }
    }

    /**
     * Puts the fake player in the server's player list for the rest of the test, so server-tick
     * loops that look players up by UUID see it. Removed again when the test passes or fails.
     */
    public static void online(GameTestHelper h, ServerPlayer p) {
        PlayerList list = h.getLevel().getServer().getPlayerList();
        Map<UUID, ServerPlayer> byUuid = field(list, "playersByUUID");
        List<ServerPlayer> players = field(list, "players"); // Forge's getPlayers() is a read-only view
        players.add(p);
        byUuid.put(p.getUUID(), p);
        onEnd(h, () -> {
            players.remove(p);
            byUuid.remove(p.getUUID(), p);
        });
    }

    /** Runs {@code cleanup} when the test finishes, pass or fail. */
    public static void onEnd(GameTestHelper h, Runnable cleanup) {
        GT.onEnd(h, cleanup);
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(PlayerList list, String name) {
        try {
            Field f = PlayerList.class.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(list);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot reach the player list", e);
        }
    }

    // ── World ────────────────────────────────────────────────────────

    /** A mob with no AI standing on the platform at relative (x, 2, z), level with the player. It stays where it is put. */
    public static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, int x, int z) {
        return h.spawnWithNoFreeWill(type, new BlockPos(x, 2, z));
    }

    public static <T extends Entity> List<T> near(ServerPlayer p, Class<T> type, double radius) {
        return p.level().getEntitiesOfClass(type, new AABB(p.blockPosition()).inflate(radius), Entity::isAlive);
    }

    // ── Assertions on entities ───────────────────────────────────────

    /** Amplifier of an active effect, or -1 if absent. */
    public static int amp(LivingEntity e, MobEffect effect) {
        MobEffectInstance inst = e.getEffect(effect);
        return inst == null ? -1 : inst.getAmplifier();
    }

    /** Duration of an active effect, or -1 if absent. */
    public static int duration(LivingEntity e, MobEffect effect) {
        MobEffectInstance inst = e.getEffect(effect);
        return inst == null ? -1 : inst.getDuration();
    }

    /** The amount of the modifier with this name on the attribute, or null if none. */
    public static Double modifier(LivingEntity e, Attribute attr, String name) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return null;
        for (AttributeModifier m : inst.getModifiers()) {
            if (m.getName().equals(name)) return m.getAmount();
        }
        return null;
    }

    public static void close(GameTestHelper h, double expected, double actual, double tolerance, String what) {
        h.assertTrue(Math.abs(expected - actual) <= tolerance,
                what + ": expected " + expected + " (±" + tolerance + ") but was " + actual);
    }

    // ── Damage log ───────────────────────────────────────────────────

    /** One attempt to hurt a watched entity, as the game asked for it (before armor or i-frames). */
    public record Hit(DamageSource source, float amount) {
        public boolean is(ResourceKey<DamageType> type) {
            return source.is(type);
        }
    }

    private static final Map<UUID, List<Hit>> WATCHED = new ConcurrentHashMap<>();
    private static boolean listening;

    /**
     * Records every hurt attempt on {@code e} from now on. Uses LivingAttackEvent, which fires
     * at the start of {@code hurt()} with the requested amount.
     */
    public static synchronized List<Hit> watch(LivingEntity e) {
        if (!listening) {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, LivingAttackEvent.class, ev -> {
                List<Hit> log = WATCHED.get(ev.getEntity().getUUID());
                if (log != null) log.add(new Hit(ev.getSource(), ev.getAmount()));
            });
            listening = true;
        }
        List<Hit> log = java.util.Collections.synchronizedList(new ArrayList<>());
        WATCHED.put(e.getUUID(), log);
        return log;
    }

    /** A damage source of this type whose direct and causing entity is {@code direct}. */
    public static DamageSource src(GameTestHelper h, ResourceKey<DamageType> type, Entity direct) {
        return new DamageSource(h.getLevel().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type), direct);
    }

    public static List<Hit> of(List<Hit> log, ResourceKey<DamageType> type) {
        synchronized (log) {
            return log.stream().filter(hit -> hit.is(type)).toList();
        }
    }

    public static Attribute PHYSICAL_RESISTANCE() {
        return com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get();
    }

    public static Attribute MAGIC_RESISTANCE() {
        return com.jttw.engine.registry.ModAttributes.MAGIC_RESISTANCE.get();
    }
}
