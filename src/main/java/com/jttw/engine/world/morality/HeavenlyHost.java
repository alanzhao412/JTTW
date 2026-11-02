package com.jttw.engine.world.morality;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.entity.heaven.HeavenMonster;
import com.jttw.engine.entity.heaven.HeavenlySoldierEntity;
import com.jttw.engine.entity.heaven.StarLordEntity;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.HeavenRegistry;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.heaven.HeavenRegions;
import com.jttw.engine.world.heaven.HeavenSpawner;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Heaven's host against its outlaws. While a player Heaven has outlawed ({@link Morality#wanted}) is in
 * Heaven, its host comes down round them and above, wave after wave, as long as they stay: the more wanted,
 * the larger, the oftener and the stronger. A wave is regulars of every kind (soldiers, archers, drummers,
 * cavalry), from the second degree with Lunar Mansions (captains) and a Heavenly General (a miniboss) at
 * their head, at the third with a star lord too (another miniboss). They come at the higher of the
 * outlaw's own level and the road's where they stand, and {@link #ABOVE} more by degree.
 *
 * <pre>
 *   degree (karma)              every   regulars  Lunar Mansions  Heavenly Generals  star lords  most out
 *   1 outlaw    (-250)          30 s       3            0                0               0          10
 *   2 enemy     (-1,000)        20 s       5            1                1               0          18
 *   3 rebel     (-3,000)        12 s       7            2                1               1          30
 * </pre>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class HeavenlyHost {

    private HeavenlyHost() {}

    /** By degree of wrath (1 to 3): ticks between waves; regulars, Lunar Mansions, Heavenly Generals and star lords a wave; the most out at once. */
    public static final int[] INTERVAL = {0, 600, 400, 240};
    public static final int[] WAVE = {0, 3, 5, 7};
    public static final int[] MANSIONS = {0, 0, 1, 2};
    public static final int[] GENERALS = {0, 0, 1, 1};
    public static final int[] STAR_LORDS = {0, 0, 0, 1};
    public static final int[] MOST = {0, 10, 18, 30};
    /** How many levels above their quarry (or the road) they come, by degree; a captain a further {@value #CAPTAIN_ABOVE}, a miniboss {@value #GENERAL_ABOVE}. */
    public static final int[] ABOVE = {0, 0, 3, 6};
    public static final int CAPTAIN_ABOVE = 4, GENERAL_ABOVE = 5;
    /** Ticks after arriving in Heaven before the first wave: 5 s. */
    public static final int FIRST_WAVE = 100;
    /** On each one sent: whom it was sent after. */
    public static final String KEY_QUARRY = JTTWEngine.MOD_ID + ":quarry";
    /** The highest any of a wave comes at: the players' cap and one, as any captain or miniboss of a band's top. */
    public static final int TOP = com.jttw.engine.progression.level.LevelConstants.MAX_LEVEL + 1;

    private static final Map<UUID, Long> NEXT_WAVE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
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
        p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4f, degree >= 3 ? 0.8f : 1f);
    }

    /** Those of the host already out after {@code quarry}. */
    public static List<HeavenMonster> hunting(ServerLevel level, ServerPlayer quarry) {
        String id = quarry.getUUID().toString();
        return level.getEntitiesOfClass(HeavenMonster.class, new AABB(quarry.blockPosition()).inflate(128),
                m -> m.isAlive() && id.equals(m.getPersistentData().getString(KEY_QUARRY)));
    }

    /** The level the host comes at after {@code quarry}: the higher of theirs and the road's, and the degree's more. */
    public static int baseLevel(ServerLevel level, ServerPlayer quarry, int degree) {
        int road = HeavenRegions.roadLevel(level.getSeed(), quarry.getX(), quarry.getZ());
        return Math.max(Math.max(1, PlayerLevel.getLevel(quarry)), road) + ABOVE[Math.max(0, Math.min(3, degree))];
    }

    /** Sends a wave after {@code quarry}: as many as the degree sends, short of the most it lets out at once. */
    public static List<HeavenMonster> wave(ServerLevel level, ServerPlayer quarry, int degree) {
        List<HeavenMonster> sent = new ArrayList<>();
        if (degree <= 0) return sent;
        int room = MOST[degree] - hunting(level, quarry).size();
        int base = baseLevel(level, quarry, degree);
        List<EntityType<? extends HeavenMonster>> kinds = HeavenSpawner.kinds();
        int[] counts = {Math.min(WAVE[degree], room), 0, 0, 0};
        counts[1] = Math.min(MANSIONS[degree], room - counts[0]);
        counts[2] = Math.min(GENERALS[degree], room - counts[0] - counts[1]);
        counts[3] = Math.min(STAR_LORDS[degree], room - counts[0] - counts[1] - counts[2]);
        int of = Math.max(1, counts[0] + Math.max(0, counts[1]) + Math.max(0, counts[2]) + Math.max(0, counts[3])), i = 0;
        for (int role = 0; role < 4; role++) {
            for (int k = 0; k < counts[role]; k++, i++) {
                EntityType<? extends HeavenMonster> type = switch (role) {
                    case 0 -> kinds.get((i + level.random.nextInt(kinds.size())) % kinds.size());
                    case 1 -> HeavenRegistry.LUNAR_MANSION.get();
                    case 2 -> ModEntities.HEAVENLY_SOLDIER.get();
                    default -> HeavenRegistry.STAR_LORD.get();
                };
                HeavenMonster m = type.create(level);
                if (m == null) continue;
                Vec3 at = descent(level, quarry, i, of);
                m.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360f, 0f);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
                int lv = Math.min(TOP, base + level.random.nextInt(3) - 1);
                switch (role) {
                    case 0 -> {
                        if (m instanceof HeavenlySoldierEntity s) s.become(lv, false, quarry.getUUID());
                        else m.become(lv, false);
                    }
                    case 1 -> {
                        m.become(Math.min(TOP, lv + CAPTAIN_ABOVE), true);
                        m.setVariant(level.random.nextInt(m.variants()));
                    }
                    case 2 -> ((HeavenlySoldierEntity) m).become(Math.min(TOP, lv + GENERAL_ABOVE), true, quarry.getUUID());
                    default -> {
                        m.becomeMiniboss(Math.min(TOP, lv + GENERAL_ABOVE), 1);
                        m.setVariant(level.random.nextInt(StarLordEntity.LUMINARIES.length));
                    }
                }
                m.getPersistentData().putString(KEY_QUARRY, quarry.getUUID().toString());
                m.setTarget(quarry);
                level.addFreshEntity(m);
                level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 12, 0.6, 0.3, 0.6, 0.01);
                sent.add(m);
            }
        }
        return sent;
    }

    /** Where one of a wave comes down: round about the quarry, 18 to 26 blocks off and above them, in open air. */
    private static Vec3 descent(ServerLevel level, ServerPlayer quarry, int i, int of) {
        for (int tries = 0; tries < 8; tries++) {
            double a = (i + level.random.nextDouble() * 0.6) * Math.PI * 2 / Math.max(1, of) + tries;
            double d = 18 + level.random.nextDouble() * 8;
            Vec3 at = quarry.position().add(Math.cos(a) * d, 4 + level.random.nextDouble() * 6, Math.sin(a) * d);
            var box = ModEntities.HEAVENLY_SOLDIER.get().getDimensions().makeBoundingBox(at.x, at.y, at.z).inflate(0.4, 1, 0.4);
            if (level.noCollision(box)) return at;
        }
        return quarry.position().add(0, 6, 0);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT_WAVE.remove(event.getEntity().getUUID());
    }
}
