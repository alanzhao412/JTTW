package com.jttw.engine.character.saurian;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.mixin.LivingEntityFlagInvoker;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModAttributes;
import com.jttw.engine.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The Saurian Demon King's abilities: the Flood-Dragon. Each is made to be seen from his own eyes:
 * at the crosshair, from his hand, on the ground ahead, or in how he moves.
 *
 * <ul>
 *   <li><b>V (hold, let go), Riptide Surge:</b> a trident's riptide without the trident or the
 *       rain: he launches the way he looks, spinning, striking all he passes; the longer held
 *       (up to 1 s), the further.</li>
 *   <li><b>B, Tidal Lash:</b> a rope of water from his hand to the crosshair yanks the first foe
 *       on it to him; sneaking, it pulls him to the block he aims at.</li>
 *   <li><b>N, Whirlpool:</b> opens on the ground he looks at, dragging foes into its heart and
 *       drowning them there.</li>
 *   <li><b>M, Flood-Dragon's Wrath:</b> for a time his blows burst in floodwater round the one
 *       struck, and while he is wet the Surge needs no rest. The edges of his sight run with water.</li>
 *   <li><b>Riverborn</b> (passive): he breathes water and swims swiftly; from II, wet (in water
 *       or rain) he hits harder and heals.</li>
 *   <li><b>Scaled Hide</b> (passive): tougher against blows, and at III against magic.</li>
 *   <li><b>Venom Fangs</b> (passive): his blows envenom; the venom stacks and burns by his power.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class SaurianAbilityHandler {

    private SaurianAbilityHandler() {}

    // ── keys ───────────────────────────────────────────────────────
    private static final String KEY_CHARGE_START = "jttw_sdk_charge_start";
    public static final String KEY_SURGE_UNTIL = "jttw_sdk_surge_until";
    private static final String KEY_SURGE_ID = "jttw_sdk_surge_id", KEY_SURGE_SPEED = "jttw_sdk_surge_speed", KEY_SURGE_HIT = "jttw_sdk_surge_hit";
    private static final String KEY_SAFE_FALL_UNTIL = "jttw_sdk_safe_fall_until";
    private static final String KEY_SURGE_READY = "jttw_sdk_surge_ready", KEY_LASH_READY = "jttw_sdk_lash_ready",
            KEY_WHIRL_READY = "jttw_sdk_whirl_ready", KEY_WRATH_READY = "jttw_sdk_wrath_ready";
    /** On a foe: the surge that last struck it, so one surge strikes each foe once. */
    private static final String KEY_STRUCK_BY = "jttw_sdk_struck_by";
    /** On a foe: its venom: stacks, damage a stack, until when, and whose. */
    public static final String KEY_VENOM = "jttw_sdk_venom";

    // ── numbers ────────────────────────────────────────────────────
    public static final float SURGE_QI = 8, LASH_QI = 6, WHIRL_QI = 15, WRATH_QI = 25;
    /** Ticks held for a full charge: 1 s. */
    public static final int FULL_CHARGE = 20;
    /** Ticks after a surge or a grapple in which a fall does not hurt. */
    public static final int SAFE_FALL = 60;

    public static int surgeCooldown(int tier) { return tier >= 3 ? 40 : tier >= 2 ? 50 : 60; }
    public static double surgeSpeed(int tier, float charge) { return 1.2 + 1.4 * charge + 0.25 * (tier - 1); }
    public static int surgeTicks(int tier, float charge) { return (int) (6 + 12 * charge) + 2 * tier; }
    public static float surgeDamage(float atk, int tier, float charge) { return atk * (tier >= 3 ? 1.9f : tier >= 2 ? 1.5f : 1.2f) * (0.6f + 0.4f * charge); }

    public static int lashCooldown(int tier) { return tier >= 3 ? 20 : tier >= 2 ? 30 : 40; }
    public static double lashReach(int tier) { return tier >= 3 ? 22 : tier >= 2 ? 18 : 14; }
    public static float lashDamage(float atk, int tier) { return atk * (tier >= 3 ? 1.3f : tier >= 2 ? 1.0f : 0.8f); }

    public static int whirlCooldown(int tier) { return tier >= 3 ? 160 : tier >= 2 ? 200 : 240; }
    public static int whirlTicks(int tier) { return tier >= 3 ? 160 : tier >= 2 ? 130 : 100; }
    public static double whirlRadius(int tier) { return tier >= 3 ? 6 : tier >= 2 ? 5 : 4; }
    /** Its drowning, every half second, at its heart; half that further out. */
    public static float whirlDamage(float atk, int tier) { return atk * (tier >= 3 ? 0.4f : tier >= 2 ? 0.32f : 0.25f); }
    public static final double WHIRL_REACH = 24;

    public static int wrathTicks(int tier) { return tier >= 3 ? 400 : tier >= 2 ? 300 : 240; }
    /** A burst's share of the blow that set it off. */
    public static float wrathBurst(int tier) { return tier >= 3 ? 0.7f : tier >= 2 ? 0.55f : 0.4f; }
    /** After it ends, before it can be called again: 45 s. */
    public static final int WRATH_REST = 900;
    public static final double BURST_RADIUS = 3;

    public static float wetBonus(int riverTier) { return riverTier >= 3 ? 0.30f : riverTier >= 2 ? 0.15f : 0f; }
    public static int venomMaxStacks(int tier) { return tier >= 3 ? 7 : tier >= 2 ? 5 : 3; }
    /** A stack's burn each second, as a share of his attack. */
    public static float venomPerStack(int tier) { return tier >= 3 ? 0.06f : tier >= 2 ? 0.05f : 0.04f; }
    public static final int VENOM_TICKS = 100;

    // ── shared ─────────────────────────────────────────────────────

    static int tier(Player p, String branch) {
        return SaurianSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(p), branch);
    }

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

    private static boolean isFoe(LivingEntity e, Entity p) {
        return e != p && e.isAlive() && !e.isAlliedTo(p) && !(e instanceof Player other && (other.isCreative() || other.isSpectator()));
    }

    private static float atk(ServerPlayer p) {
        return (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public static boolean wet(Entity e) {
        return e.isInWaterRainOrBubble();
    }

    public static boolean inWrath(LivingEntity p) {
        return p.hasEffect(ModEffects.FLOOD_WRATH.get());
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent s, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    /** Where a thing he throws from his hand starts: a little right of and below his eyes, ahead of him. */
    public static Vec3 hand(Player p) {
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        return p.getEyePosition().add(look.scale(0.6)).add(right.scale(0.35)).add(0, -0.3, 0);
    }

    // ── V: Riptide Surge ───────────────────────────────────────────

    public static boolean surging(Player p) {
        return p.getPersistentData().contains(KEY_SURGE_UNTIL);
    }

    /** Surging needs no rest while the Wrath is on him and he is wet. */
    private static boolean surgeFree(ServerPlayer p) {
        return inWrath(p) && wet(p);
    }

    public static void startCharge(ServerPlayer p) {
        if (!SaurianCharacter.is(p) || tier(p, "sdk_surge") < 1 || surging(p)) return;
        if (!surgeFree(p) && onCooldown(p, KEY_SURGE_READY)) return;
        p.getPersistentData().putLong(KEY_CHARGE_START, p.level().getGameTime());
        sound(p.serverLevel(), p.position(), SoundEvents.TRIDENT_RETURN, 0.8f, 0.7f);
    }

    public static void release(ServerPlayer p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(KEY_CHARGE_START)) return;
        long held = p.level().getGameTime() - pd.getLong(KEY_CHARGE_START);
        pd.remove(KEY_CHARGE_START);
        if (!spendQi(p, SURGE_QI)) return;
        surge(p, Math.max(0.25f, Math.min(1f, held / (float) FULL_CHARGE)));
    }

    /** Launches him the way he looks, spinning, at {@code charge} (0.25 to 1) of a full surge. */
    public static void surge(ServerPlayer p, float charge) {
        int tier = Math.max(1, tier(p, "sdk_surge"));
        long now = p.level().getGameTime();
        CompoundTag pd = p.getPersistentData();
        pd.putLong(KEY_SURGE_ID, now);
        pd.putLong(KEY_SURGE_UNTIL, now + surgeTicks(tier, charge));
        pd.putDouble(KEY_SURGE_SPEED, surgeSpeed(tier, charge));
        pd.putFloat(KEY_SURGE_HIT, surgeDamage(atk(p), tier, charge));
        // vanilla's riptide spin: the pose, the swirl round him, the narrow body, seen from all sides
        ((LivingEntityFlagInvoker) p).jttw_invokeSetLivingEntityFlag(4, true);
        sound(p.serverLevel(), p.position(), charge >= 0.9f ? SoundEvents.TRIDENT_RIPTIDE_3 : charge >= 0.5f ? SoundEvents.TRIDENT_RIPTIDE_2 : SoundEvents.TRIDENT_RIPTIDE_1, 1f, 1f);
        if (!surgeFree(p)) startCooldown(p, KEY_SURGE_READY, "sdk_surge", surgeCooldown(tier) + surgeTicks(tier, charge));
        tickSurge(p);
    }

    public static void tickSurge(ServerPlayer p) {
        CompoundTag pd = p.getPersistentData();
        if (p.level().getGameTime() >= pd.getLong(KEY_SURGE_UNTIL)) {
            endSurge(p);
            return;
        }
        ServerLevel level = p.serverLevel();
        Vec3 dir = p.getLookAngle();
        p.setDeltaMovement(dir.scale(pd.getDouble(KEY_SURGE_SPEED)));
        p.hurtMarked = true;
        p.resetFallDistance();
        long surgeId = pd.getLong(KEY_SURGE_ID);
        float damage = pd.getFloat(KEY_SURGE_HIT);
        AABB path = p.getBoundingBox().expandTowards(dir.scale(pd.getDouble(KEY_SURGE_SPEED))).inflate(0.8);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, path, e -> isFoe(e, p))) {
            CompoundTag ed = e.getPersistentData();
            if (ed.contains(KEY_STRUCK_BY) && ed.getLong(KEY_STRUCK_BY) == surgeId) continue;
            ed.putLong(KEY_STRUCK_BY, surgeId);
            e.hurt(p.damageSources().playerAttack(p), damage);
            e.push(dir.x * 1.2, 0.4, dir.z * 1.2);
            e.hurtMarked = true;
            level.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 20, 0.4, 0.4, 0.4, 0.2);
            sound(level, e.position(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.8f, 1.2f);
        }
        level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.5, p.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
        level.sendParticles(ParticleTypes.DOLPHIN, p.getX(), p.getY() + 0.5, p.getZ(), 4, 0.3, 0.3, 0.3, 0.05);
    }

    public static void endSurge(ServerPlayer p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(KEY_SURGE_UNTIL)) return;
        pd.remove(KEY_SURGE_UNTIL);
        pd.remove(KEY_SURGE_ID);
        pd.remove(KEY_SURGE_SPEED);
        pd.remove(KEY_SURGE_HIT);
        pd.putLong(KEY_SAFE_FALL_UNTIL, p.level().getGameTime() + SAFE_FALL);
        ((LivingEntityFlagInvoker) p).jttw_invokeSetLivingEntityFlag(4, false);
        p.setDeltaMovement(p.getDeltaMovement().scale(0.4));
        p.hurtMarked = true;
    }

    // ── B: Tidal Lash ──────────────────────────────────────────────

    /** Lashes at the crosshair. Returns what it caught (a foe, or null for a grapple or a miss). */
    @Nullable
    public static LivingEntity lash(ServerPlayer p) {
        int tier = SaurianCharacter.is(p) ? tier(p, "sdk_lash") : 0;
        if (tier < 1 || onCooldown(p, KEY_LASH_READY)) return null;
        ServerLevel level = p.serverLevel();
        double reach = lashReach(tier);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), end = eye.add(look.scale(reach));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = block.getLocation();
        boolean grapple = p.isShiftKeyDown();
        if (grapple && block.getType() == HitResult.Type.MISS) {
            p.displayClientMessage(Component.literal("§7Nothing there to catch hold of."), true);
            return null;
        }
        if (!spendQi(p, LASH_QI)) return null;
        startCooldown(p, KEY_LASH_READY, "sdk_lash", lashCooldown(tier));
        Vec3 from = hand(p);
        LivingEntity caught = null;
        Vec3 to = stop;
        if (grapple) {
            Vec3 pull = stop.subtract(p.position());
            p.setDeltaMovement(pull.normalize().scale(Math.min(2.4, 0.6 + pull.length() * 0.12)).add(0, 0.3, 0));
            p.hurtMarked = true;
            p.getPersistentData().putLong(KEY_SAFE_FALL_UNTIL, level.getGameTime() + SAFE_FALL);
        } else {
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, p, eye, stop,
                    p.getBoundingBox().expandTowards(look.scale(reach)).inflate(1), e -> e instanceof LivingEntity l && isFoe(l, p));
            if (hit != null && hit.getEntity() instanceof LivingEntity e) {
                caught = e;
                to = e.position().add(0, e.getBbHeight() / 2, 0);
                e.hurt(p.damageSources().playerAttack(p), lashDamage(atk(p), tier));
                Vec3 pull = p.position().subtract(e.position());
                e.setDeltaMovement(pull.normalize().scale(Math.min(2.0, 0.5 + pull.length() * 0.1)).add(0, 0.35, 0));
                e.hurtMarked = true;
                if (tier >= 3) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                level.sendParticles(ParticleTypes.SPLASH, to.x, to.y, to.z, 25, 0.4, 0.4, 0.4, 0.2);
            }
        }
        rope(level, from, to);
        sound(level, p.position(), SoundEvents.FISHING_BOBBER_THROW, 1f, 0.6f);
        sound(level, to, caught != null ? SoundEvents.PLAYER_SPLASH_HIGH_SPEED : SoundEvents.GENERIC_SPLASH, 1f, 1.1f);
        return caught;
    }

    /** The rope of water, from his hand to where it struck. */
    private static void rope(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        int steps = (int) Math.ceil(d.length() / 0.3);
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.add(d.scale(i / (double) Math.max(1, steps)));
            level.sendParticles(ParticleTypes.DOLPHIN, at.x, at.y, at.z, 2, 0.03, 0.03, 0.03, 0);
            if (i % 3 == 0) level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    // ── N: Whirlpool ───────────────────────────────────────────────

    /** A whirlpool turning in the world: where, whose, how strong, until when. */
    public record Whirlpool(ResourceKey<Level> dimension, Vec3 at, UUID owner, int tier, float drown, long until) {}

    private static final List<Whirlpool> WHIRLPOOLS = new ArrayList<>();

    public static List<Whirlpool> whirlpools() {
        return List.copyOf(WHIRLPOOLS);
    }

    /** Opens a whirlpool on the ground he looks at, within {@value #WHIRL_REACH} blocks. */
    @Nullable
    public static Whirlpool whirlpool(ServerPlayer p) {
        int tier = SaurianCharacter.is(p) ? tier(p, "sdk_whirl") : 0;
        if (tier < 1 || onCooldown(p, KEY_WHIRL_READY)) return null;
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(WHIRL_REACH)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
        if (hit.getType() == HitResult.Type.MISS) {
            p.displayClientMessage(Component.literal("§7No ground there for a whirlpool."), true);
            return null;
        }
        if (!spendQi(p, WHIRL_QI)) return null;
        startCooldown(p, KEY_WHIRL_READY, "sdk_whirl", whirlCooldown(tier));
        Vec3 at = hit.getLocation();
        Whirlpool w = new Whirlpool(level.dimension(), at, p.getUUID(), tier, whirlDamage(atk(p), tier), level.getGameTime() + whirlTicks(tier));
        WHIRLPOOLS.add(w);
        sound(level, at, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.5f, 0.7f);
        return w;
    }

    /** One tick of a whirlpool: it turns, drags foes in and round, and drowns those at its heart. */
    public static void tickWhirlpool(ServerLevel level, Whirlpool w) {
        long t = level.getGameTime();
        double r = whirlRadius(w.tier());
        Vec3 c = w.at();
        if (t % 2 == 0) {
            // spiral arms of water, turning inward
            for (int arm = 0; arm < 4; arm++) {
                for (double d = 0.5; d <= r; d += 0.6) {
                    double a = t * 0.25 + arm * Math.PI / 2 + d * 0.7;
                    level.sendParticles(d > r - 1 ? ParticleTypes.SPLASH : ParticleTypes.FISHING,
                            c.x + Math.cos(a) * d, c.y + 0.1, c.z + Math.sin(a) * d, 1, 0, 0, 0, 0);
                }
            }
            level.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 0.3, c.z, 4, 0.4, 0.2, 0.4, 0.02);
        }
        if (t % 20 == 0) sound(level, c, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.2f, 0.8f);
        Entity owner = level.getEntity(w.owner());
        Entity source = owner != null ? owner : null;
        AABB area = new AABB(c, c).inflate(r, 2, r);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> source == null ? e.isAlive() && !(e instanceof Player) : isFoe(e, source))) {
            Vec3 in = c.subtract(e.position());
            double dist = Math.sqrt(in.x * in.x + in.z * in.z);
            if (dist > r) continue;
            Vec3 flat = new Vec3(in.x, 0, in.z);
            Vec3 toward = flat.lengthSqr() < 1e-4 ? Vec3.ZERO : flat.normalize();
            Vec3 round = new Vec3(-toward.z, 0, toward.x);
            e.setDeltaMovement(e.getDeltaMovement().scale(0.6).add(toward.scale(0.18)).add(round.scale(0.12)).add(0, -0.02, 0));
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
            if (t % 10 == 0) {
                boolean heart = dist < 1.6;
                float dmg = heart ? w.drown() : w.drown() * 0.5f;
                e.hurt(source instanceof Player pl ? level.damageSources().indirectMagic(pl, pl) : level.damageSources().drown(), dmg);
                if (heart) level.sendParticles(ParticleTypes.BUBBLE_POP, e.getX(), e.getEyeY(), e.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || WHIRLPOOLS.isEmpty()) return;
        var server = event.getServer();
        Iterator<Whirlpool> it = WHIRLPOOLS.iterator();
        while (it.hasNext()) {
            Whirlpool w = it.next();
            ServerLevel level = server.getLevel(w.dimension());
            if (level == null || level.getGameTime() >= w.until()) {
                it.remove();
                continue;
            }
            tickWhirlpool(level, w);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        WHIRLPOOLS.clear();
    }

    // ── M: Flood-Dragon's Wrath ────────────────────────────────────

    public static boolean wrath(ServerPlayer p) {
        int tier = SaurianCharacter.is(p) ? tier(p, "sdk_wrath") : 0;
        if (tier < 1 || inWrath(p) || onCooldown(p, KEY_WRATH_READY) || !spendQi(p, WRATH_QI)) return false;
        int ticks = wrathTicks(tier);
        p.addEffect(new MobEffectInstance(ModEffects.FLOOD_WRATH.get(), ticks, tier - 1, false, false, true));
        startCooldown(p, KEY_WRATH_READY, "sdk_wrath", ticks + WRATH_REST);
        sound(p.serverLevel(), p.position(), SoundEvents.CONDUIT_ACTIVATE, 1.5f, 0.8f);
        p.serverLevel().sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 1, p.getZ(), 60, 1, 1, 1, 0.3);
        return true;
    }

    /** A blow of his under the Wrath bursts in floodwater round the one struck. Returns how many the burst hit. */
    public static int burst(ServerPlayer p, LivingEntity struck, float blow) {
        int tier = Math.max(1, tier(p, "sdk_wrath"));
        ServerLevel level = p.serverLevel();
        Vec3 c = struck.position();
        int hit = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, struck.getBoundingBox().inflate(BURST_RADIUS),
                e -> e != struck && isFoe(e, p))) {
            e.hurt(p.damageSources().indirectMagic(p, p), blow * wrathBurst(tier));
            Vec3 out = e.position().subtract(c);
            Vec3 flat = new Vec3(out.x, 0, out.z);
            if (flat.lengthSqr() > 1e-4) e.push(flat.normalize().x * 0.8, 0.3, flat.normalize().z * 0.8);
            e.hurtMarked = true;
            hit++;
        }
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            level.sendParticles(ParticleTypes.SPLASH, c.x + Math.cos(a) * 1.5, c.y + 0.3, c.z + Math.sin(a) * 1.5, 2, 0.1, 0.1, 0.1, 0.1);
        }
        level.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 1, c.z, 12, 0.4, 0.6, 0.4, 0.1);
        sound(level, c, SoundEvents.GENERIC_SPLASH, 1.2f, 0.7f);
        return hit;
    }

    // ── blows: Riverborn's might, the Wrath's bursts, the fangs' venom ──

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || !SaurianCharacter.is(p) || event.getEntity() == p) return;
        float bonus = wet(p) ? wetBonus(tier(p, "sdk_river")) : 0;
        if (bonus > 0) event.setAmount(event.getAmount() * (1 + bonus));
        // only his own blows, hand to hide, burst and envenom
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().getDirectEntity() != p) return;
        if (inWrath(p)) burst(p, event.getEntity(), event.getAmount());
        int venom = tier(p, "sdk_venom");
        if (venom >= 1) envenom(p, event.getEntity(), venom);
    }

    /** Adds a stack of his venom to {@code e}, up to the most his fangs carry. */
    public static void envenom(ServerPlayer p, LivingEntity e, int tier) {
        CompoundTag v = e.getPersistentData().getCompound(KEY_VENOM);
        long now = e.level().getGameTime();
        int stacks = v.getLong("Until") > now ? v.getInt("Stacks") : 0;
        v.putInt("Stacks", Math.min(venomMaxStacks(tier), stacks + 1));
        v.putFloat("PerStack", atk(p) * venomPerStack(tier));
        v.putLong("Until", now + VENOM_TICKS);
        v.putUUID("From", p.getUUID());
        e.getPersistentData().put(KEY_VENOM, v);
        e.addEffect(new MobEffectInstance(MobEffects.POISON, VENOM_TICKS, 0, false, true));
    }

    public static int venomStacks(LivingEntity e) {
        CompoundTag v = e.getPersistentData().getCompound(KEY_VENOM);
        return v.getLong("Until") > e.level().getGameTime() ? v.getInt("Stacks") : 0;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || e.tickCount % 20 != 0 || !e.getPersistentData().contains(KEY_VENOM)) return;
        CompoundTag v = e.getPersistentData().getCompound(KEY_VENOM);
        if (v.getLong("Until") <= e.level().getGameTime()) {
            e.getPersistentData().remove(KEY_VENOM);
            return;
        }
        Player from = v.hasUUID("From") ? e.level().getPlayerByUUID(v.getUUID("From")) : null;
        float dmg = v.getInt("Stacks") * v.getFloat("PerStack");
        e.invulnerableTime = 0; // venom burns beneath a blow's grace
        e.hurt(from != null ? e.damageSources().indirectMagic(from, from) : e.damageSources().magic(), dmg);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer p
                && (surging(p) || p.level().getGameTime() < p.getPersistentData().getLong(KEY_SAFE_FALL_UNTIL))) {
            event.setCanceled(true);
        }
    }

    // ── passives ───────────────────────────────────────────────────

    /** His passives, as they stand: refreshed each second. Public for tests. */
    public static void passives(ServerPlayer p, boolean saurian) {
        int river = saurian ? tier(p, "sdk_river") : 0, hide = saurian ? tier(p, "sdk_hide") : 0;
        PassiveModifiers.set(p, ForgeMod.SWIM_SPEED.get(), "sdk_river_swim", river >= 1 ? 0.8 : 0, AttributeModifier.Operation.MULTIPLY_BASE);
        PassiveModifiers.add(p, ModAttributes.PHYSICAL_RESISTANCE.get(), "sdk_hide_pres", hide * 10);
        PassiveModifiers.add(p, ModAttributes.MAGIC_RESISTANCE.get(), "sdk_hide_mres", hide >= 3 ? 10 : 0);
        if (river >= 2 && wet(p) && p.getHealth() < p.getMaxHealth()) {
            p.heal(LevelPower.scaled(p, river >= 3 ? 2 : 1));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        boolean saurian = SaurianCharacter.is(p);
        if (surging(p)) {
            if (saurian) tickSurge(p);
            else endSurge(p);
        }
        if (saurian && tier(p, "sdk_river") >= 1 && p.isUnderWater()) p.setAirSupply(p.getMaxAirSupply());
        if (p.tickCount % 20 == 0) passives(p, saurian);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            endSurge(p);
            p.getPersistentData().remove(KEY_CHARGE_START);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag pd = event.getEntity().getPersistentData();
        for (String k : List.of(KEY_SURGE_UNTIL, KEY_SURGE_ID, KEY_SURGE_SPEED, KEY_SURGE_HIT, KEY_CHARGE_START)) pd.remove(k);
    }
}
