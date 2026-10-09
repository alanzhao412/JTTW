package com.jttw.engine.character.peng;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.flight.FlightSteering;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Arrays;

/**
 * The Gold-Winged Peng's abilities. He flies from the start, no item needed.
 *
 * <ul>
 *   <li><b>V (hold), Ninety-Thousand-Li Flight:</b> takes off if grounded, then beats his
 *       wings and builds speed while held, over 10 seconds, to 2.0 / 2.8 / 3.6 times his flying
 *       speed; drains Qi. Let go and he slows back to cruising.</li>
 *   <li><b>Wingstrike</b> (passive): in flight, everything he passes through is struck, harder
 *       the faster he flies.</li>
 *   <li><b>B, Feather Storm:</b> a fan of 5 / 7 / 9 golden feathers, flying or not; III's
 *       pierce.</li>
 *   <li><b>N, Eclipse Wings:</b> everything within 10 / 16 blocks is weakened and slowed (players
 *       also darkened); II makes them lose their prey and glow.</li>
 * </ul>
 * With the Yin-Yang Two-Qi Vase he can also hold players and bosses, which no one else can:
 * they are held in the vase's pull for 5 seconds, dissolving, then burst out in a yin-yang blast.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class PengAbilityHandler {

    private PengAbilityHandler() {}

    // ── keys and numbers ───────────────────────────────────────────

    public static final String KEY_SOARING = "jttw_gp_soaring";
    /** Set while he plays the Peng, so what the Peng changed is undone once, when he stops. */
    private static final String KEY_WAS_PENG = "jttw_gp_was_peng";
    private static final String KEY_LAST_X = "jttw_gp_last_x", KEY_LAST_Y = "jttw_gp_last_y", KEY_LAST_Z = "jttw_gp_last_z";
    /** On a foe: game time Wingstrike last struck it. */
    private static final String KEY_STRUCK_AT = "jttw_gp_struck_at";
    private static final String KEY_FEATHERS_READY_AT = "jttw_gp_feathers_ready_at";
    private static final String KEY_ECLIPSE_READY_AT = "jttw_gp_eclipse_ready_at";
    /** On a feather: its damage, whether it pierces, and who it has already struck. */
    public static final String TAG_FEATHER = "jttw_gp_feather";
    private static final String TAG_PIERCE = "jttw_gp_pierce";
    private static final String TAG_STRUCK = "jttw_gp_struck";
    /** On something held by the vase: until when, by whom, and where. */
    public static final String KEY_JAIL_UNTIL = "jttw_vase_jail_until";
    private static final String KEY_JAIL_BY = "jttw_vase_jail_by";
    private static final String KEY_JAIL_X = "jttw_vase_jail_x", KEY_JAIL_Y = "jttw_vase_jail_y", KEY_JAIL_Z = "jttw_vase_jail_z";

    public static final float SOAR_QI_PER_TICK = 0.4f;
    public static final double BOOST_DECAY = 0.08;
    public static final double WINGSTRIKE_MIN_SPEED = 0.5;
    public static final int WINGSTRIKE_REHIT_TICKS = 10;
    public static final int FEATHERS_COOLDOWN = 30;
    public static final float FEATHERS_QI = 6f;
    public static final int ECLIPSE_COOLDOWN = 400;
    public static final float ECLIPSE_QI = 20f;
    public static final int JAIL_TICKS = 100;
    /** Share of max health the vase dissolves each second. */
    public static final float DISSOLVE_PER_SECOND = 0.05f;
    public static final double BLAST_RADIUS = 4;

    private static final DustParticleOptions YANG = new DustParticleOptions(new Vector3f(1f, 1f, 1f), 1.4f);
    private static final DustParticleOptions YIN = new DustParticleOptions(new Vector3f(0.05f, 0.05f, 0.05f), 1.4f);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.8f, 0.2f), 1.2f);

    private static boolean onCooldown(ServerPlayer p, String key) {
        return p.level().getGameTime() < p.getPersistentData().getLong(key);
    }

    private static void startCooldown(ServerPlayer p, String key, String abilityId, int ticks) {
        p.getPersistentData().putLong(key, p.level().getGameTime() + ticks);
        NetworkHandler.sendToPlayer(p, new SyncCooldownPacket(p.getUUID(), abilityId, ticks, ticks));
    }

    private static boolean spendQi(ServerPlayer p, float amount) {
        if (!QiData.spend(p, amount)) {
            p.displayClientMessage(Component.literal("§cNot enough Qi."), true);
            return false;
        }
        QiManager.syncNow(p);
        return true;
    }

    private static int tier(Player p, String branch) {
        return PengSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(p), branch);
    }

    private static boolean isFoe(LivingEntity e, Entity p) {
        return e != p && e.isAlive() && !e.isAlliedTo(p);
    }

    private static float atk(ServerPlayer p) {
        return (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    // ── V: Ninety-Thousand-Li Flight ───────────────────────────────

    /** Top boost: 2.0 / 2.8 times flying speed; III reaches the top speed anyone flies. */
    public static double maxBoost(int tier) {
        return tier >= 3 ? FlightSteering.TOP_SPEED / FlightSteering.CRUISE_SPEED : tier >= 2 ? 2.8 : tier >= 1 ? 2.0 : 1.0;
    }

    /** How much the boost climbs each tick held: the top takes 10 seconds to reach at any tier. */
    public static double boostRamp(int tier) {
        return (maxBoost(tier) - 1) / FlightSteering.BUILD_UP_TICKS;
    }

    public static double boost(ServerPlayer p) {
        return JTTWFlight.boost(p);
    }

    public static void startSoaring(ServerPlayer p) {
        if (!PengCharacter.is(p) || tier(p, "gp_soar") < 1) return;
        p.getPersistentData().putBoolean(KEY_SOARING, true);
        if (!p.getAbilities().flying && p.getAbilities().mayfly) {
            // Take off: one beat lifts him from the ground into flight
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.setDeltaMovement(p.getDeltaMovement().add(0, 0.6, 0));
            p.hurtMarked = true;
        }
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1f, 1.2f);
    }

    public static void stopSoaring(ServerPlayer p) {
        p.getPersistentData().remove(KEY_SOARING);
    }

    private static void tickSoaring(ServerPlayer p) {
        CompoundTag pd = p.getPersistentData();
        int tier = tier(p, "gp_soar");
        double b = boost(p);
        boolean soaring = pd.getBoolean(KEY_SOARING) && tier >= 1 && p.getAbilities().flying;
        if (soaring && !spendQi(p, SOAR_QI_PER_TICK)) {
            soaring = false;
            pd.remove(KEY_SOARING);
        }
        if (soaring) {
            b = Math.min(maxBoost(tier), b + boostRamp(tier));
            if (p.tickCount % 6 == 0) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.7f, 1.4f);
            }
            p.serverLevel().sendParticles(GOLD, p.getX(), p.getY() + 1, p.getZ(), 3, 0.6, 0.3, 0.6, 0);
        } else {
            b = Math.max(1.0, b - BOOST_DECAY);
        }
        JTTWFlight.setBoost(p, b, soaring); // the flight system sends it to his client
    }

    // ── Wingstrike ─────────────────────────────────────────────────

    /** Wingstrike damage: ATK x speed (blocks/tick) x 0.5 / 0.7 / 0.9. */
    public static float wingstrikeDamage(float atk, double speed, int tier) {
        return (float) (atk * speed * (tier >= 3 ? 0.9 : tier >= 2 ? 0.7 : 0.5));
    }

    /** Wingstrike's reach either side of his path. */
    public static double wingstrikeReach(int tier) {
        return tier >= 3 ? 2.0 : 1.2;
    }

    /** Strikes all he flew through between {@code from} and where he is. Returns how many. */
    public static int wingstrike(ServerPlayer p, Vec3 from) {
        int tier = tier(p, "gp_strike");
        Vec3 to = p.position();
        double speed = to.distanceTo(from);
        if (tier < 1 || !p.getAbilities().flying || speed < WINGSTRIKE_MIN_SPEED) return 0;
        double reach = wingstrikeReach(tier);
        float damage = wingstrikeDamage(atk(p), speed, tier);
        Vec3 dir = to.subtract(from).normalize();
        Vec3 mid = new Vec3(0, p.getBbHeight() / 2, 0);
        AABB swept = new AABB(from, to).inflate(reach + 1).move(mid);
        long now = p.level().getGameTime();
        int struck = 0;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, swept, e -> isFoe(e, p))) {
            Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0);
            if (distanceToSegment(c, from.add(mid), to.add(mid)) > reach + e.getBbWidth() / 2) continue;
            CompoundTag ed = e.getPersistentData();
            if (now - ed.getLong(KEY_STRUCK_AT) < WINGSTRIKE_REHIT_TICKS && ed.contains(KEY_STRUCK_AT)) continue;
            ed.putLong(KEY_STRUCK_AT, now);
            e.hurt(p.damageSources().playerAttack(p), damage);
            e.push(dir.x * speed * 0.5, 0.3, dir.z * speed * 0.5);
            e.hurtMarked = true;
            p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0, 0, 0, 0);
            struck++;
        }
        if (struck > 0) {
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.8f);
        }
        return struck;
    }

    private static double distanceToSegment(Vec3 c, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        double t = len < 1e-9 ? 0 : Math.max(0, Math.min(1, c.subtract(a).dot(ab) / len));
        return c.distanceTo(a.add(ab.scale(t)));
    }

    // ── B: Feather Storm ───────────────────────────────────────────

    public static int featherCount(int tier) { return tier >= 3 ? 9 : tier >= 2 ? 7 : 5; }

    public static float featherDamage(float atk, int tier) {
        return atk * (tier >= 3 ? 0.6f : tier >= 2 ? 0.5f : 0.4f);
    }

    /** Fans out the feathers. Returns them, or an empty array if he could not throw. */
    public static Snowball[] featherStorm(ServerPlayer p) {
        int tier = PengCharacter.is(p) ? tier(p, "gp_feathers") : 0;
        if (tier < 1 || onCooldown(p, KEY_FEATHERS_READY_AT) || !spendQi(p, FEATHERS_QI)) return new Snowball[0];
        int n = featherCount(tier);
        float damage = featherDamage(atk(p), tier);
        float spread = 50f;
        Snowball[] feathers = new Snowball[n];
        for (int i = 0; i < n; i++) {
            float yaw = p.getYRot() - spread / 2 + spread * i / (n - 1);
            Snowball f = new Snowball(p.level(), p);
            f.setItem(new ItemStack(Items.FEATHER));
            f.getPersistentData().putFloat(TAG_FEATHER, damage);
            f.getPersistentData().putBoolean(TAG_PIERCE, tier >= 3);
            f.shootFromRotation(p, p.getXRot(), yaw, 0f, 2.4f, 0f);
            p.level().addFreshEntity(f);
            feathers[i] = f;
        }
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1f, 1.8f);
        startCooldown(p, KEY_FEATHERS_READY_AT, "gp_feathers", FEATHERS_COOLDOWN);
        return feathers;
    }

    @SubscribeEvent
    public static void onFeatherLands(ProjectileImpactEvent event) {
        Projectile f = event.getProjectile();
        if (!f.getPersistentData().contains(TAG_FEATHER) || f.level().isClientSide) return;
        HitResult hit = event.getRayTraceResult();
        if (hit.getType() == HitResult.Type.ENTITY && ((EntityHitResult) hit).getEntity() instanceof LivingEntity e
                && f.getOwner() instanceof ServerPlayer owner) {
            if (e == owner) { event.setCanceled(true); return; } // through his own wings
            CompoundTag fd = f.getPersistentData();
            int[] struck = fd.getIntArray(TAG_STRUCK);
            if (Arrays.stream(struck).noneMatch(id -> id == e.getId())) {
                e.invulnerableTime = 0; // a fan of feathers lands together
                e.hurt(owner.damageSources().thrown(f, owner), fd.getFloat(TAG_FEATHER));
                ((ServerLevel) f.level()).sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
                int[] more = Arrays.copyOf(struck, struck.length + 1);
                more[struck.length] = e.getId();
                fd.putIntArray(TAG_STRUCK, more);
            }
            event.setCanceled(true);
            if (!fd.getBoolean(TAG_PIERCE)) f.discard();
            return;
        }
        event.setCanceled(true);
        f.discard();
    }

    // ── N: Eclipse Wings ───────────────────────────────────────────

    public static double eclipseRadius(int tier) { return tier >= 2 ? 16 : 10; }
    public static int eclipseTicks(int tier) { return tier >= 2 ? 160 : 100; }

    /** Darkens all within reach. Returns how many, or -1 if he could not. */
    public static int eclipse(ServerPlayer p) {
        int tier = PengCharacter.is(p) ? tier(p, "gp_eclipse") : 0;
        if (tier < 1 || onCooldown(p, KEY_ECLIPSE_READY_AT) || !spendQi(p, ECLIPSE_QI)) return -1;
        double r = eclipseRadius(tier);
        int ticks = eclipseTicks(tier);
        int count = 0;
        ServerLevel level = p.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> isFoe(e, p))) {
            if (e.distanceTo(p) > r) continue;
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
            if (e instanceof Player) e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0));
            if (tier >= 2) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0));
                if (e instanceof Mob m) m.setTarget(null);
            }
            count++;
        }
        for (int i = 0; i < 48; i++) {
            double a = 2 * Math.PI * i / 48;
            level.sendParticles(ParticleTypes.SQUID_INK, p.getX() + Math.cos(a) * 2, p.getY() + 1.5, p.getZ() + Math.sin(a) * 2,
                    1, Math.cos(a) * r / 8, 0.05, Math.sin(a) * r / 8, 0.3);
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 40, r / 3, 1, r / 3, 0.02);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS, 1.5f, 0.5f);
        startCooldown(p, KEY_ECLIPSE_READY_AT, "gp_eclipse", ECLIPSE_COOLDOWN);
        return count;
    }

    // ── Yin-Yang Two-Qi Vase: Peng's hold on the unsealable ────────

    /** Holds a player or boss in the vase's pull: dissolving, for {@value #JAIL_TICKS} ticks. */
    public static void jail(ServerPlayer peng, LivingEntity target) {
        CompoundTag t = target.getPersistentData();
        t.putLong(KEY_JAIL_UNTIL, target.level().getGameTime() + JAIL_TICKS);
        t.putUUID(KEY_JAIL_BY, peng.getUUID());
        t.putDouble(KEY_JAIL_X, target.getX());
        t.putDouble(KEY_JAIL_Y, target.getY());
        t.putDouble(KEY_JAIL_Z, target.getZ());
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    public static boolean isJailed(Entity e) {
        return e.getPersistentData().contains(KEY_JAIL_UNTIL);
    }

    /** The vase's release: black and white Qi bursts out, striking all near. Returns how many. */
    public static int yinYangBlast(ServerPlayer peng, Vec3 at) {
        ServerLevel level = peng.serverLevel();
        float damage = 0.8f * atk(peng);
        int count = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(BLAST_RADIUS), e -> isFoe(e, peng))) {
            Vec3 away = e.position().subtract(at);
            if (away.length() > BLAST_RADIUS + e.getBbWidth() / 2) continue;
            e.invulnerableTime = 0;
            e.hurt(peng.damageSources().indirectMagic(peng, peng), damage);
            Vec3 dir = away.horizontalDistanceSqr() < 1e-4 ? Vec3.ZERO : new Vec3(away.x, 0, away.z).normalize();
            e.push(dir.x * 1.2, 0.5, dir.z * 1.2);
            e.hurtMarked = true;
            count++;
        }
        for (int i = 0; i < 40; i++) {
            double a = 2 * Math.PI * i / 40;
            double r = 0.5 + i * 0.08;
            level.sendParticles(i % 2 == 0 ? YANG : YIN, at.x + Math.cos(a) * r, at.y + 0.8, at.z + Math.sin(a) * r, 2, 0.1, 0.3, 0.1, 0);
            level.sendParticles(i % 2 == 0 ? YIN : YANG, at.x - Math.cos(a) * r, at.y + 0.8, at.z - Math.sin(a) * r, 2, 0.1, 0.3, 0.1, 0);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9f, 1.5f);
        return count;
    }

    private static void tickJailed(LivingEntity e) {
        CompoundTag t = e.getPersistentData();
        long now = e.level().getGameTime();
        ServerPlayer peng = e.getServer() == null ? null : e.getServer().getPlayerList().getPlayer(t.getUUID(KEY_JAIL_BY));
        if (now >= t.getLong(KEY_JAIL_UNTIL) || !e.isAlive()) {
            Vec3 at = e.position();
            for (String k : new String[]{KEY_JAIL_UNTIL, KEY_JAIL_BY, KEY_JAIL_X, KEY_JAIL_Y, KEY_JAIL_Z}) t.remove(k);
            if (peng != null) yinYangBlast(peng, at);
            return;
        }
        // Held where the vase caught it
        double x = t.getDouble(KEY_JAIL_X), y = t.getDouble(KEY_JAIL_Y), z = t.getDouble(KEY_JAIL_Z);
        if (e.distanceToSqr(x, y, z) > 0.04) {
            if (e instanceof ServerPlayer sp) sp.connection.teleport(x, y, z, sp.getYRot(), sp.getXRot()); // the client moves players
            e.setPos(x, y, z);
        }
        e.setDeltaMovement(Vec3.ZERO);
        e.fallDistance = 0;
        if ((t.getLong(KEY_JAIL_UNTIL) - now) % 20 == 0) {
            float dissolve = DISSOLVE_PER_SECOND * e.getMaxHealth();
            e.invulnerableTime = 0;
            e.hurt(peng != null ? e.damageSources().indirectMagic(peng, peng) : e.damageSources().magic(), dissolve);
        }
        if (e.level() instanceof ServerLevel level) {
            double a = now * 0.4;
            level.sendParticles(YANG, x + Math.cos(a), y + 1, z + Math.sin(a), 1, 0, 0.3, 0, 0);
            level.sendParticles(YIN, x - Math.cos(a), y + 1, z - Math.sin(a), 1, 0, 0.3, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (!e.level().isClientSide && isJailed(e)) tickJailed(e);
    }

    // The held cannot fight or use anything
    @SubscribeEvent
    public static void onJailedAttack(AttackEntityEvent event) {
        if (isJailed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onJailedUseItem(PlayerInteractEvent.RightClickItem event) {
        if (isJailed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onJailedUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isJailed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onJailedBreak(PlayerInteractEvent.LeftClickBlock event) {
        if (isJailed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onJailedInteract(PlayerInteractEvent.EntityInteract event) {
        if (isJailed(event.getEntity())) event.setCanceled(true);
    }

    // ── ticking and lifecycle ──────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        CompoundTag pd = p.getPersistentData();
        if (!PengCharacter.is(p)) {
            if (pd.getBoolean(KEY_WAS_PENG)) {
                pd.remove(KEY_WAS_PENG);
                pd.remove(KEY_SOARING);
                JTTWFlight.setBoost(p, 1, false);
            }
            return;
        }
        pd.putBoolean(KEY_WAS_PENG, true);
        // He flies from the start: JTTWFlight checks it each tick (fliesInnately), nothing to grant

        tickSoaring(p);

        Vec3 last = pd.contains(KEY_LAST_X)
                ? new Vec3(pd.getDouble(KEY_LAST_X), pd.getDouble(KEY_LAST_Y), pd.getDouble(KEY_LAST_Z)) : p.position();
        wingstrike(p, last);
        pd.putDouble(KEY_LAST_X, p.getX());
        pd.putDouble(KEY_LAST_Y, p.getY());
        pd.putDouble(KEY_LAST_Z, p.getZ());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            p.getPersistentData().remove(KEY_SOARING);
            p.getPersistentData().remove(KEY_LAST_X);
            JTTWFlight.setBoost(p, 1, false);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            p.getPersistentData().remove(KEY_SOARING);
            p.getPersistentData().remove(KEY_LAST_X);
            JTTWFlight.setBoost(p, 1, false);
        }
    }
}
