package com.jttw.engine.character.phoenix;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Phoenix's Solar Flare (B): for {@value #TICKS} ticks it is a small sun. Once a second
 * everything within its reach (6 / 8 / 10 blocks by tier) burns and takes fire damage, the undead
 * twice; arrows and other missiles that enter it burn up; it lights the dark about it as it goes.
 * It ends in a blinding burst. A ring of flame marks how far it reaches; its own sight is rimmed
 * in white-gold while it lasts (client.SolarFlareOverlay).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class SolarFlare {

    private SolarFlare() {}

    public static final int TICKS = 160, COOLDOWN = 600;
    public static final float QI_COST = 20f;
    /** Fire damage a second, at level 1 (grown by level power); the burst is twice it. */
    public static float burnPerSecond(int tier) { return 2f + tier; }
    public static double reach(int tier) { return tier >= 3 ? 10 : tier == 2 ? 8 : 6; }

    private static final String KEY_ENDS = "jttw_ph_flare_ends", KEY_READY = "jttw_ph_flare_ready";
    /** The light it carries, where it was put. */
    private static final Map<UUID, net.minecraft.core.GlobalPos> LIGHT = new HashMap<>();

    public static boolean active(ServerPlayer p) {
        return p.getPersistentData().getLong(KEY_ENDS) > p.level().getGameTime();
    }

    private static int tier(ServerPlayer p) {
        return PhoenixSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(p), "ph_radiance");
    }

    /** B: becomes a sun, if learned, ready and paid for. Returns whether it did. */
    public static boolean cast(ServerPlayer p) {
        if (tier(p) < 1 || active(p)) return false;
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong(KEY_READY)) return false;
        if (!QiData.spend(p, QI_COST)) return false;
        NetworkHandler.sendToPlayer(p, new SyncQiPacket(QiData.getCurrent(p), QiData.getMax(p)));
        p.getPersistentData().putLong(KEY_ENDS, now + TICKS);
        p.getPersistentData().putLong(KEY_READY, now + TICKS + COOLDOWN);
        p.addEffect(new MobEffectInstance(ModEffects.SOLAR_FLARE.get(), TICKS, 0, false, false, true));
        NetworkHandler.sendToPlayer(p, new SyncCooldownPacket(p.getUUID(), "ph_radiance", TICKS + COOLDOWN, TICKS + COOLDOWN));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 0.6f);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        long ends = p.getPersistentData().getLong(KEY_ENDS);
        if (ends == 0) return;
        long now = p.level().getGameTime();
        if (now >= ends) {
            burst(p);
            end(p);
            return;
        }
        int tier = Math.max(1, tier(p));
        double r = reach(tier);
        ServerLevel level = p.serverLevel();
        AABB around = p.getBoundingBox().inflate(r);

        // missiles that enter it burn up
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, around,
                s -> s.getOwner() != p && s.distanceTo(p) <= r)) {
            level.sendParticles(ParticleTypes.FLAME, shot.getX(), shot.getY(), shot.getZ(), 8, 0.1, 0.1, 0.1, 0.02);
            shot.discard();
        }
        // once a second, all within it burn
        if ((ends - now) % 20 == 0) {
            float burn = LevelPower.scaled(p, burnPerSecond(tier));
            for (LivingEntity e : within(p, r)) {
                e.setSecondsOnFire(3);
                e.hurt(p.damageSources().playerAttack(p), e.getMobType() == MobType.UNDEAD ? burn * 2 : burn);
            }
        }
        // its body blazes; a ring of flame marks its reach
        // (shown to all but itself: from inside, they would stand in front of its eyes; its rim of
        // white-gold says it instead)
        for (ServerPlayer watcher : level.players()) {
            if (watcher != p) level.sendParticles(watcher, ParticleTypes.FLAME, false, p.getX(), p.getY() + 1, p.getZ(), 6, 0.4, 0.6, 0.4, 0.02);
        }
        if (now % 4 == 0) {
            int n = (int) (r * 6);
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * i / n;
                level.sendParticles(ParticleTypes.SMALL_FLAME, p.getX() + Math.cos(a) * r, p.getY() + 0.2,
                        p.getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0.005);
            }
        }
        carryLight(p);
    }

    /** All living things within {@code r} of it but itself. */
    private static List<LivingEntity> within(ServerPlayer p, double r) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e != p && e.isAlive() && e.distanceTo(p) <= r);
    }

    /** The end: a blinding burst, twice a second's burn, over all within it. */
    private static void burst(ServerPlayer p) {
        int tier = Math.max(1, tier(p));
        double r = reach(tier);
        float burn = LevelPower.scaled(p, burnPerSecond(tier)) * 2;
        for (LivingEntity e : within(p, r)) {
            e.hurt(p.damageSources().playerAttack(p), e.getMobType() == MobType.UNDEAD ? burn * 2 : burn);
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), p);
            e.setSecondsOnFire(4);
        }
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 80, r * 0.3, 0.6, r * 0.3, 0.25);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2f, 0.5f);
    }

    private static void end(ServerPlayer p) {
        p.getPersistentData().remove(KEY_ENDS);
        p.removeEffect(ModEffects.SOLAR_FLARE.get());
        dropLight(p);
    }

    /** The light it gives: a light block where its head is, moved along as it goes, never over anything. */
    private static void carryLight(ServerPlayer p) {
        var at = net.minecraft.core.GlobalPos.of(p.level().dimension(), BlockPos.containing(p.getEyePosition()));
        if (at.equals(LIGHT.get(p.getUUID()))) return;
        dropLight(p);
        if (p.level().getBlockState(at.pos()).isAir()) {
            p.level().setBlock(at.pos(), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
            LIGHT.put(p.getUUID(), at);
        }
    }

    private static void dropLight(ServerPlayer p) {
        var was = LIGHT.remove(p.getUUID());
        if (was == null) return;
        ServerLevel where = p.server.getLevel(was.dimension()); // wherever it was left, in whichever world
        if (where != null && where.getBlockState(was.pos()).is(Blocks.LIGHT)) where.removeBlock(was.pos(), false);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) end(p);
    }

    @SubscribeEvent
    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) end(p);
    }
}
