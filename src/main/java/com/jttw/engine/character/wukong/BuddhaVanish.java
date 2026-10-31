package com.jttw.engine.character.wukong;

import com.jttw.engine.JTTWEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Appear and Disappear (Shift + B), the Fighting Buddha's: a toggle, kept by the server and its
 * state saved with him (a relog finds him as he was).
 *
 * <p>Vanished, he is wholly unseen. No other player's game is told he is there
 * ({@link com.jttw.engine.mixin.ServerPlayerVanishMixin}: the server stops pairing him with their
 * clients, as with a spectator), so nothing of him is drawn: no body, armour, held item, name tag,
 * shadow or particles; and he is made silent, so no step or blow of his is heard. Creatures lose
 * him: any that has him as its target lets him go (its brain's target and a neutral creature's
 * anger too), none can take him as one, and he counts as unseen to their senses. Reappearing, he is
 * shown again at once, in a golden flash.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class BuddhaVanish {

    private BuddhaVanish() {}

    public static final String KEY = "jttw_buddha_vanished";
    /** How far round him creatures are made to let him go. */
    public static final double FORGET_RADIUS = 96;

    /** Whether he is vanished now (the Fighting Buddha only). */
    public static boolean isVanished(@Nullable Entity e) {
        return e instanceof Player p && p.getPersistentData().getBoolean(KEY) && FightingBuddha.is(p);
    }

    /** Whether {@code watcher}'s game is kept from knowing {@code self} is there. */
    public static boolean hiddenFrom(ServerPlayer self, ServerPlayer watcher) {
        return watcher != self && isVanished(self);
    }

    /** Shift + B: vanish, or reappear. */
    public static void toggle(ServerPlayer p) {
        if (!FightingBuddha.is(p)) return;
        set(p, !isVanished(p));
    }

    /** Vanishes or reappears. */
    public static void set(ServerPlayer p, boolean vanished) {
        if (vanished && !FightingBuddha.is(p)) return;
        p.getPersistentData().putBoolean(KEY, vanished);
        p.setSilent(vanished);
        ServerLevel level = p.serverLevel();
        if (vanished) {
            forget(p);
            p.displayClientMessage(Component.literal("§6You vanish. §7No eye sees you, no creature hunts you. §8(Shift + B to appear)"), true);
            level.playSound(p, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6f, 1.6f);
            p.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
        refreshTracking(p);
        if (!vanished) {
            FightingBuddha.goldenFlash(level, p.position().add(0, 1, 0), 1.2);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.4f);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0f, 0.8f);
            p.displayClientMessage(Component.literal("§6You appear in a flash of golden light."), true);
        }
        FightingBuddha.sync(p);
    }

    /** Every creature near him that has him as its target lets him go. */
    public static void forget(ServerPlayer p) {
        AABB box = p.getBoundingBox().inflate(FORGET_RADIUS);
        for (Mob mob : p.serverLevel().getEntitiesOfClass(Mob.class, box, m -> true)) forget(mob, p);
    }

    /** {@code mob} lets {@code p} go: its target, its brain's target and grudge, a neutral creature's anger. */
    public static void forget(Mob mob, Player p) {
        if (mob.getTarget() == p) mob.setTarget(null);
        var brain = mob.getBrain();
        if (brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == p) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
        if (brain.hasMemoryValue(MemoryModuleType.ANGRY_AT)
                && p.getUUID().equals(brain.getMemory(MemoryModuleType.ANGRY_AT).orElse(null))) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        }
        if (mob instanceof NeutralMob neutral && p.getUUID().equals(neutral.getPersistentAngerTarget())) {
            neutral.stopBeingAngry();
        }
        if (mob.getLastHurtByMob() == p) mob.setLastHurtByMob(null);
    }

    /** No creature takes him as its target while he is vanished. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (isVanished(event.getNewAboutToBeSetTarget())) event.setCanceled(true);
    }

    /** To any creature's senses he is not there. */
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (isVanished(event.getEntity())) event.modifyVisibility(0);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (p.getPersistentData().getBoolean(KEY) && !FightingBuddha.is(p)) {
            set(p, false); // no longer the Buddha (a debug level change): seen again
            return;
        }
        if (p.tickCount % 5 == 0 && isVanished(p)) {
            forget(p);
            if (!p.isSilent()) p.setSilent(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isVanished(p)) {
            p.setSilent(true);
            p.displayClientMessage(Component.literal("§6You are still vanished. §8(Shift + B to appear)"), true);
        }
    }

    // ── Telling the other players' games at once ────────────────────────

    private static Field entityMap;
    private static Method updatePlayers;

    /**
     * Asks the server's tracker, now, whether each player in his world sees him, so he goes from
     * (or comes back to) their games at once rather than when one of them next crosses a chunk.
     */
    public static void refreshTracking(ServerPlayer p) {
        try {
            ChunkMap map = p.serverLevel().getChunkSource().chunkMap;
            if (entityMap == null) {
                entityMap = ChunkMap.class.getDeclaredField("entityMap");
                entityMap.setAccessible(true);
            }
            Object tracked = ((it.unimi.dsi.fastutil.ints.Int2ObjectMap<?>) entityMap.get(map)).get(p.getId());
            if (tracked == null) return;
            if (updatePlayers == null) {
                updatePlayers = tracked.getClass().getDeclaredMethod("updatePlayers", List.class);
                updatePlayers.setAccessible(true);
            }
            updatePlayers.invoke(tracked, List.copyOf(p.serverLevel().players()));
        } catch (ReflectiveOperationException | RuntimeException e) {
            JTTWEngine.LOGGER.warn("Could not refresh who sees {}", p.getGameProfile().getName(), e);
        }
    }

    /** For tests: whether a creature could take {@code target} as its target now. */
    public static boolean canTarget(LivingEntity target) {
        return !isVanished(target);
    }
}
