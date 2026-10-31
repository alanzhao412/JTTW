package com.jttw.engine.character.wukong;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.FightingBuddhaPacket;
import com.jttw.engine.progression.level.LevelConstants;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.skilltree.AbilityUnlock;
import com.jttw.engine.progression.stats.ProgressionClass;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Victorious Fighting Buddha (斗战胜佛): what Sun Wukong becomes at level 100, the cap, as the
 * Buddha names him at the journey's end. A free milestone, reached by level alone (as a Divine
 * Beast's forms are), announced once, and from then his for good; a save already at level 100 is
 * told on its next login.
 *
 * <p>Four powers come with it:
 * <ul>
 *   <li><b>Indestructible body</b> (here): nothing harms him. Every damage source, players, bosses,
 *       fire, lava, falling, drowning, the void, magic, wither, poison, blasts, is refused before it
 *       lands (no flinch, no knockback, no lost health); harmful effects, the mod's Stunned and
 *       Pinned among them, never take hold. Only an operator's {@code /kill} still kills him. In the
 *       void he is unharmed: he may fall in it, or stay, as he likes.</li>
 *   <li><b>Appear and Disappear</b> ({@link BuddhaVanish}): Shift + B.</li>
 *   <li><b>Breaks anything</b> ({@link BuddhaBreaking}): what his abilities break ignores hardness
 *       and every protection.</li>
 *   <li><b>Somersault Anywhere</b> ({@link SomersaultTravel}): Shift + M.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class FightingBuddha {

    private FightingBuddha() {}

    /** The level he ascends at: the cap. */
    public static final int LEVEL = LevelConstants.MAX_LEVEL;
    /** Set once his ascension has been announced. */
    public static final String KEY_ANNOUNCED = "jttw_fighting_buddha";

    /** Ability ids of his two active powers (their skill-bar slots). */
    public static final String VANISH = "wu_buddha_vanish";
    public static final String TRAVEL = "wu_somersault_anywhere";

    /** The four powers, as the character screen lists them: free milestones of level 100. */
    public static final List<AbilityUnlock> MILESTONES = List.of(
            milestone("fighting_buddha", "Victorious Fighting Buddha",
                    "Free milestone. Sun Wukong becomes the Victorious Fighting Buddha (斗战胜佛): golden kasaya, "
                            + "a halo of golden light, a gold-ringed fillet."),
            milestone("buddha_indestructible", "Indestructible Body",
                    "Nothing can damage him: blows, bosses, fire, lava, falls, drowning, the void, magic, wither, "
                            + "poison, blasts. No flinch, no knockback; harmful effects, Stunned and Pinned never take hold."),
            milestone("buddha_vanish", "Appear and Disappear",
                    "Shift + B: vanish wholly from players and creatures (mobs lose him); press again to "
                            + "reappear in a golden flash."),
            milestone("buddha_breaks_all", "Breaks Anything",
                    "What his abilities break (the staff, its slam, the Qi Wave, the eye laser) ignores hardness and "
                            + "protection: bedrock, obsidian, reinforced deepslate, lairs, anything."),
            milestone("buddha_somersault", "Somersault Anywhere",
                    "Shift + M: one somersault of the cloud to any player in any realm, any zone, lair or "
                            + "named place, his spawn, or typed coordinates. Lands him safely."));

    private static AbilityUnlock milestone(String id, String name, String desc) {
        return new AbilityUnlock(ProgressionClass.PRIMORDIAL, LEVEL, id, name, desc, true, null);
    }

    /** Whether {@code e} is the Fighting Buddha: Wukong, at level 100. */
    public static boolean is(@Nullable Entity e) {
        return e instanceof Player p && PlayerLevel.getLevel(p) >= LEVEL && WukongCharacter.is(p);
    }

    /** Whether he refuses this damage: everything but an operator's {@code /kill}. */
    public static boolean refuses(DamageSource source) {
        return !source.is(DamageTypes.GENERIC_KILL);
    }

    /** Whether an effect can hold him: none that harms. */
    public static boolean refusesEffect(MobEffectInstance effect) {
        MobEffect e = effect.getEffect().value();
        return e.getCategory() == MobEffectCategory.HARMFUL;
    }

    // ── The body that nothing harms ─────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (is(event.getEntity()) && refuses(event.getSource())) event.setInvulnerable(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (is(event.getEntity()) && refuses(event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (is(event.getEntity()) && refuses(event.getSource())) {
            event.setCanceled(true);
            event.getEntity().setHealth(event.getEntity().getMaxHealth());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (is(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        if (is(event.getAffectedEntity())) event.setKnockbackVelocity(Vec3.ZERO);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (is(event.getEntity()) && refusesEffect(event.getEffectInstance())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /**
     * Each tick: whatever slipped past the events (health set straight down, fire, frost, air,
     * an effect laid before he ascended) is undone, and his ascension announced if it has not been.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !is(p)) return;
        keepWhole(p);
        if (!p.getPersistentData().getBoolean(KEY_ANNOUNCED)) ascend(p);
    }

    /** Undoes whatever harm is on him: full health, no fire, no frost, full air, no fall, no harmful effect. */
    public static void keepWhole(ServerPlayer p) {
        if (p.isDeadOrDying()) return;
        if (p.getHealth() < p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        if (p.getRemainingFireTicks() > 0) p.clearFire();
        if (p.getTicksFrozen() > 0) p.setTicksFrozen(0);
        if (p.getAirSupply() < p.getMaxAirSupply()) p.setAirSupply(p.getMaxAirSupply());
        p.fallDistance = 0;
        if (!p.getActiveEffects().isEmpty()) {
            List<net.minecraft.core.Holder<MobEffect>> bad = p.getActiveEffects().stream()
                    .filter(FightingBuddha::refusesEffect).map(MobEffectInstance::getEffect).toList();
            for (var e : bad) p.removeEffect(e);
        }
    }

    // ── Ascension ───────────────────────────────────────────────────────

    /** Announces his ascension, once: to him in title, chat and sound, and to the world in chat. */
    public static void ascend(ServerPlayer p) {
        p.getPersistentData().putBoolean(KEY_ANNOUNCED, true);
        p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 100, 40));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l斗战胜佛")));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eThe Victorious Fighting Buddha")));
        p.sendSystemMessage(Component.literal(
                "§6§l✦ 斗战胜佛 · The Victorious Fighting Buddha ✦\n"
                        + "§eThe journey is done. The Tathāgata names you Victorious Fighting Buddha:"
                        + " the golden fillet no longer binds but shines, a halo of light rises behind you,"
                        + " and a golden kasaya settles on your shoulders.\n"
                        + "§6Indestructible Body §7— nothing can harm you (only an operator's /kill).\n"
                        + "§6Appear and Disappear §7— §fShift + B§7: vanish from every eye and every hunter.\n"
                        + "§6Breaks Anything §7— what your abilities break ignores hardness and protection.\n"
                        + "§6Somersault Anywhere §7— §fShift + M§7: to anyone, anyplace, in one somersault.\n"
                        + "§8(Character screen, H: Skills tab)"));
        var server = p.getServer();
        if (server != null) {
            Component all = Component.literal("§6✦ §e" + p.getGameProfile().getName()
                    + " §6has become the Victorious Fighting Buddha (斗战胜佛) §6✦");
            for (ServerPlayer other : server.getPlayerList().getPlayers()) if (other != p) other.sendSystemMessage(all);
        }
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 0.7f);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0f, 0.6f);
        goldenFlash(level, p.position().add(0, 1, 0), 2.0);
        p.setHealth(p.getMaxHealth());
        sync(p);
        NetworkHandler.sendToPlayer(p, com.jttw.engine.network.s2c.SyncAbilitiesPacket.build(p));
    }

    /** A burst of golden light round {@code at}, {@code size} blocks across or so. */
    public static void goldenFlash(ServerLevel level, Vec3 at, double size) {
        DustParticleOptions gold = new DustParticleOptions(new org.joml.Vector3f(1.0f, 0.82f, 0.25f), 2.0f);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(gold, at.x, at.y, at.z, (int) (40 * size), 0.5 * size, 0.8 * size, 0.5 * size, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, (int) (20 * size), 0.3 * size, 0.6 * size, 0.3 * size, 0.12);
    }

    // ── What the clients are told: ascended (drawn as the Buddha) and vanished ──

    private static final Map<UUID, Integer> LAST = new ConcurrentHashMap<>();

    /** Bit 0: he is the Fighting Buddha; bit 1: vanished; bit 2: a Wukong player, at any level (drawn as the Monkey King). */
    public static int packed(ServerPlayer p) {
        return (is(p) ? 1 : 0) | (BuddhaVanish.isVanished(p) ? 2 : 0) | (WukongCharacter.is(p) ? 4 : 0);
    }

    /** Tells him and those who see him how he is now. */
    public static void sync(ServerPlayer p) {
        int now = packed(p);
        LAST.put(p.getUUID(), now);
        FightingBuddhaPacket pkt = new FightingBuddhaPacket(p.getId(), now);
        NetworkHandler.sendToPlayer(p, pkt);
        NetworkHandler.sendToTrackingPlayers(p, pkt);
    }

    @SubscribeEvent
    public static void onSyncTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        int now = packed(p);
        Integer was = LAST.get(p.getUUID());
        if (was == null ? now != 0 : was != now) sync(p);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer seen && event.getEntity() instanceof ServerPlayer watcher) {
            int now = packed(seen);
            if (now != 0) NetworkHandler.sendToPlayer(watcher, new FightingBuddhaPacket(seen.getId(), now));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
