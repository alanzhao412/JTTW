package com.jttw.engine.world.morality;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.entity.heaven.HeavenlySoldierEntity;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heaven's host. While a player Heaven has outlawed ({@link Morality#wanted}) is in Heaven, its
 * soldiers come down on their clouds in waves, round about them and above: the more wanted, the
 * larger and more often, and from the second degree with Heavenly Generals at their head.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class HeavenlyHost {

    private HeavenlyHost() {}

    /** By degree of wrath (1 to 3): ticks between waves, soldiers a wave, generals a wave, and the most out at once. */
    public static final int[] INTERVAL = {0, 900, 500, 300};
    public static final int[] WAVE = {0, 3, 5, 8};
    public static final int[] GENERALS = {0, 0, 1, 2};
    public static final int[] MOST = {0, 8, 16, 30};
    /** How many levels above their quarry they come, by degree; a general a further {@value #GENERAL_ABOVE}. */
    public static final int[] ABOVE = {0, 0, 3, 6};
    public static final int GENERAL_ABOVE = 7;
    /** Ticks after arriving in Heaven before the first wave: 5 s. */
    public static final int FIRST_WAVE = 100;

    private static final Map<UUID, Long> NEXT_WAVE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        int degree = Morality.wanted(p);
        if (degree == 0 || p.isCreative() || p.isSpectator() || !p.level().dimension().equals(JTTWDimensions.HEAVEN)) {
            NEXT_WAVE.remove(p.getUUID());
            return;
        }
        long now = p.serverLevel().getGameTime();
        Long next = NEXT_WAVE.get(p.getUUID());
        if (next == null) {
            NEXT_WAVE.put(p.getUUID(), now + FIRST_WAVE);
            announce(p, degree);
            return;
        }
        if (now < next) return;
        NEXT_WAVE.put(p.getUUID(), now + INTERVAL[degree]);
        wave(p.serverLevel(), p, degree);
    }

    private static void announce(ServerPlayer p, int degree) {
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§cHeaven's host descends")));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7You are " + Morality.standing(Morality.karma(p)))));
        p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.get(), SoundSource.HOSTILE, 4f, degree >= 3 ? 0.8f : 1f);
    }

    /** The soldiers already out after {@code quarry}. */
    public static List<HeavenlySoldierEntity> hunting(ServerLevel level, ServerPlayer quarry) {
        return level.getEntitiesOfClass(HeavenlySoldierEntity.class, new AABB(quarry.blockPosition()).inflate(128),
                s -> quarry.getUUID().equals(s.quarry()));
    }

    /** Sends a wave after {@code quarry}: as many as the degree sends, short of the most it lets out at once. */
    public static List<HeavenlySoldierEntity> wave(ServerLevel level, ServerPlayer quarry, int degree) {
        List<HeavenlySoldierEntity> sent = new ArrayList<>();
        if (degree <= 0) return sent;
        int room = MOST[degree] - hunting(level, quarry).size();
        int soldiers = Math.min(WAVE[degree], room), generals = Math.min(GENERALS[degree], room - soldiers);
        int base = Math.max(20, PlayerLevel.getLevel(quarry)) + ABOVE[degree];
        for (int i = 0; i < soldiers + Math.max(0, generals); i++) {
            boolean general = i >= soldiers;
            Vec3 at = descent(level, quarry, i, soldiers + generals);
            HeavenlySoldierEntity s = ModEntities.HEAVENLY_SOLDIER.get().create(level);
            if (s == null) continue;
            s.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360f, 0f);
            s.finalizeSpawn(level, level.getCurrentDifficultyAt(s.blockPosition()), MobSpawnType.EVENT, null, null);
            s.become(base + (general ? GENERAL_ABOVE : 0) + level.random.nextInt(3) - 1, general, quarry.getUUID());
            s.setTarget(quarry);
            level.addFreshEntity(s);
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 12, 0.6, 0.3, 0.6, 0.01);
            sent.add(s);
        }
        return sent;
    }

    /** Where one of a wave comes down: round about the quarry, 18 to 26 blocks off and above them, in open air. */
    private static Vec3 descent(ServerLevel level, ServerPlayer quarry, int i, int of) {
        for (int tries = 0; tries < 8; tries++) {
            double a = (i + level.random.nextDouble() * 0.6) * Math.PI * 2 / Math.max(1, of) + tries;
            double d = 18 + level.random.nextDouble() * 8;
            Vec3 at = quarry.position().add(Math.cos(a) * d, 4 + level.random.nextDouble() * 6, Math.sin(a) * d);
            var box = ModEntities.HEAVENLY_SOLDIER.get().getAABB(at.x, at.y, at.z);
            if (level.noCollision(box)) return at;
        }
        return quarry.position().add(0, 6, 0);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT_WAVE.remove(event.getEntity().getUUID());
    }
}
