package com.jttw.engine.character.bulldemon;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModAttributes;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The Bull Demon King's abilities, and what his two weapons do in his hands.
 *
 * <ul>
 *   <li><b>V (hold), Bull Rush:</b> charges where he looks, steering as he turns, goring and
 *       tossing all in his way. Ramming a wall ends it, stunning everything near the impact.</li>
 *   <li><b>B, 72 Transformations:</b> the menu (client-side).</li>
 *   <li><b>M, Great White Bull:</b> toggle; the giant bull: more health, harder hits, unmoved by
 *       blows; drains Qi.</li>
 *   <li><b>Thick Hide</b> (passive): fire cannot burn him; tougher against blows (and from II,
 *       magic); III hard to knock back.</li>
 *   <li><b>Banana-Leaf Fan:</b> in his hands it blows what it fans away, up to 1000 blocks,
 *       further the stronger he is.</li>
 *   <li><b>Mixed-Iron Cudgel:</b> in his hands a charged swing sends a shockwave along the
 *       ground, 1 block a tick, 6 / 9 / 12 blocks by charge, tossing all it passes. It breaks
 *       nothing.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class BullDemonAbilityHandler {

    private BullDemonAbilityHandler() {}

    // ── keys and numbers ───────────────────────────────────────────

    public static final String KEY_RUSHING = "jttw_bdk_rushing";
    private static final String KEY_RUSH_START = "jttw_bdk_rush_start";
    private static final String KEY_RUSH_READY_AT = "jttw_bdk_rush_ready_at";
    /** On a foe: the rush that last gored it, so one rush gores each foe once. */
    private static final String KEY_GORED_BY = "jttw_bdk_gored_by";
    public static final String KEY_GREAT_BULL = "jttw_bdk_great_bull";
    /** On something the fan blew away: where it lands, and when. */
    public static final String KEY_BLOWN_AT = "jttw_fan_blown_at";
    private static final String KEY_BLOWN_X = "jttw_fan_blown_x", KEY_BLOWN_Y = "jttw_fan_blown_y", KEY_BLOWN_Z = "jttw_fan_blown_z";

    public static final float RUSH_QI_PER_TICK = 0.5f;
    public static final int RUSH_COOLDOWN = 60;
    public static final float GREAT_BULL_QI_PER_TICK = 0.3f;
    public static final float GREAT_BULL_MIN_QI = 10f;
    /** Ticks a blown-away foe is seen flying before it lands far off. */
    public static final int BLOWN_FLIGHT_TICKS = 10;
    public static final double FAN_REACH_PER_ATK = 37;
    public static final double FAN_MAX_REACH = 1000;
    public static final double FAN_MIN_REACH = 16;

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
        return BullDemonSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(p), branch);
    }

    private static boolean isFoe(LivingEntity e, Entity p) {
        return e != p && e.isAlive() && !e.isAlliedTo(p);
    }

    private static float atk(ServerPlayer p) {
        return (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private static Vec3 flatLook(Entity p) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    private static boolean solid(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    // ── V: Bull Rush ───────────────────────────────────────────────

    public static double rushSpeed(int tier) { return tier >= 3 ? 1.2 : tier >= 2 ? 1.05 : 0.9; }
    public static int rushTicks(int tier) { return tier >= 3 ? 80 : tier >= 2 ? 60 : 40; }
    public static float rushDamage(float atk, int tier) { return atk * (tier >= 3 ? 1.6f : tier >= 2 ? 1.3f : 1.0f); }
    public static int stunTicks(int tier) { return tier >= 3 ? 80 : tier >= 2 ? 60 : 40; }

    public static boolean isRushing(Player p) {
        return p.getPersistentData().getBoolean(KEY_RUSHING);
    }

    public static void startRush(ServerPlayer p) {
        if (!BullDemonCharacter.is(p) || tier(p, "bdk_rush") < 1 || isRushing(p)) return;
        if (onCooldown(p, KEY_RUSH_READY_AT) || !spendQi(p, 5f)) return;
        CompoundTag pd = p.getPersistentData();
        pd.putBoolean(KEY_RUSHING, true);
        pd.putLong(KEY_RUSH_START, p.level().getGameTime());
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1f, 1.2f);
    }

    public static void endRush(ServerPlayer p) {
        if (!isRushing(p)) return;
        p.getPersistentData().remove(KEY_RUSHING);
        startCooldown(p, KEY_RUSH_READY_AT, "bdk_rush", RUSH_COOLDOWN);
    }

    /** One tick of the charge: drive forward, gore what is in the way, stop at a wall. */
    public static void tickRush(ServerPlayer p) {
        CompoundTag pd = p.getPersistentData();
        int tier = tier(p, "bdk_rush");
        long rushId = pd.getLong(KEY_RUSH_START);
        if (tier < 1 || p.level().getGameTime() - rushId > rushTicks(tier) || !spendQi(p, RUSH_QI_PER_TICK)) {
            endRush(p);
            return;
        }
        ServerLevel level = p.serverLevel();
        Vec3 dir = flatLook(p);
        double speed = rushSpeed(tier);

        // A wall ahead ends the charge in an impact
        BlockPos feet = BlockPos.containing(p.position().add(dir.scale(0.6 + p.getBbWidth() / 2)).add(0, 0.5, 0));
        if (solid(level, feet) && solid(level, feet.above())) {
            wallImpact(p, dir, tier);
            endRush(p);
            return;
        }

        p.setDeltaMovement(dir.x * speed, p.getDeltaMovement().y, dir.z * speed);
        p.hurtMarked = true;
        float damage = rushDamage(atk(p), tier);
        AABB horns = p.getBoundingBox().move(dir.scale(0.8)).inflate(0.6, 0.2, 0.6);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, horns, e -> isFoe(e, p))) {
            CompoundTag ed = e.getPersistentData();
            if (ed.getLong(KEY_GORED_BY) == rushId && ed.contains(KEY_GORED_BY)) continue;
            ed.putLong(KEY_GORED_BY, rushId);
            e.hurt(p.damageSources().playerAttack(p), damage);
            e.push(dir.x * 1.5, 0.6, dir.z * 1.5);
            e.hurtMarked = true;
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.6f);
        }
        BlockState ground = level.getBlockState(p.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), p.getX(), p.getY() + 0.1, p.getZ(),
                    4, 0.4, 0.05, 0.4, 0.1);
        }
    }

    /** The charge meets a wall: everything near the impact is stunned. Returns how many. */
    public static int wallImpact(ServerPlayer p, Vec3 dir, int tier) {
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position().add(dir.scale(1.0));
        int ticks = stunTicks(tier);
        int count = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(2.5, 1.5, 2.5), e -> isFoe(e, p))) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 9));
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks + 40, 0));
            if (e instanceof Mob m) m.setTarget(null);
            e.hurt(p.damageSources().playerAttack(p), 0.5f * atk(p));
            count++;
        }
        BlockState wall = level.getBlockState(BlockPos.containing(at.add(0, 1, 0)));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, wall), at.x, at.y + 1, at.z, 40, 0.3, 0.6, 0.3, 0.2);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y + 1.5, at.z, 20, 0.8, 0.4, 0.8, 0.3);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1f, 0.5f);
        return count;
    }

    // ── M: Great White Bull ────────────────────────────────────────

    /** The bull's extra health at level 1, also healed on taking the form: grows with level power. */
    public static double greatBullHealth(int tier) { return tier >= 3 ? 40 : tier >= 2 ? 30 : 20; }
    /** The bull's extra attack, a share of his whole attack. */
    public static double greatBullAttack(int tier) { return tier >= 3 ? 0.7 : tier >= 2 ? 0.5 : 0.3; }

    public static boolean inGreatBull(Player p) {
        return p.getPersistentData().getBoolean(KEY_GREAT_BULL);
    }

    public static void toggleGreatBull(ServerPlayer p) {
        if (inGreatBull(p)) {
            endGreatBull(p);
            return;
        }
        int tier = BullDemonCharacter.is(p) ? tier(p, "bdk_bull") : 0;
        if (tier < 1) return;
        if (QiData.getCurrent(p) < GREAT_BULL_MIN_QI) {
            p.displayClientMessage(Component.literal("§cNot enough Qi."), true);
            return;
        }
        p.getPersistentData().putBoolean(KEY_GREAT_BULL, true);
        TransformationManager.transform(p, com.jttw.engine.registry.ModEntities.GREAT_WHITE_BULL.get());
        applyGreatBull(p, tier);
        p.heal(LevelPower.scaled(p, greatBullHealth(tier)));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5f, 0.6f);
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 30, 1, 1, 1, 0.05);
    }

    /** Re-applied every tick of the form, so the health bonus keeps pace with level power. */
    private static void applyGreatBull(ServerPlayer p, int tier) {
        PassiveModifiers.add(p, Attributes.MAX_HEALTH, "bdk_bull_health", greatBullHealth(tier) * LevelPower.of(p));
        // A share of his whole attack: his base is 1, so MULTIPLY_BASE would add only the fraction
        PassiveModifiers.set(p, Attributes.ATTACK_DAMAGE, "bdk_bull_attack", greatBullAttack(tier),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PassiveModifiers.add(p, Attributes.KNOCKBACK_RESISTANCE, "bdk_bull_steady", 1.0);
    }

    public static void endGreatBull(ServerPlayer p) {
        p.getPersistentData().remove(KEY_GREAT_BULL);
        PassiveModifiers.add(p, Attributes.MAX_HEALTH, "bdk_bull_health", 0);
        PassiveModifiers.set(p, Attributes.ATTACK_DAMAGE, "bdk_bull_attack", 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PassiveModifiers.add(p, Attributes.KNOCKBACK_RESISTANCE, "bdk_bull_steady", 0);
        TransformationData data = TransformationManager.getTransformation(p);
        if (data != null && data.getEntityType() == com.jttw.engine.registry.ModEntities.GREAT_WHITE_BULL.get()) TransformationManager.demorph(p);
    }

    // ── Thick Hide ─────────────────────────────────────────────────

    private static void thickHide(ServerPlayer p, int tier) {
        PassiveModifiers.add(p, ModAttributes.PHYSICAL_RESISTANCE.get(), "bdk_hide_pres", tier * 10);
        PassiveModifiers.add(p, ModAttributes.MAGIC_RESISTANCE.get(), "bdk_hide_mres", tier >= 2 ? (tier - 1) * 10 : 0);
        PassiveModifiers.add(p, Attributes.KNOCKBACK_RESISTANCE, "bdk_hide_steady", tier >= 3 ? 0.5 : 0);
        if (tier >= 1 && p.isOnFire()) p.clearFire();
    }

    @SubscribeEvent
    public static void onAttacked(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getSource().is(DamageTypeTags.IS_FIRE)
                && BullDemonCharacter.is(p) && tier(p, "bdk_hide") >= 1) {
            event.setCanceled(true);
        }
    }

    // ── Banana-Leaf Fan, in his hands ──────────────────────────────

    /**
     * How far his fan blows: 37 blocks per point of ATK, 16 to 1000. {@code atk} is his attack with
     * level power taken out (his attack as it would be at level 1), so the reach follows his build,
     * not the power curve; see {@link #fanReach(ServerPlayer)}.
     */
    public static double fanReach(float atk) {
        return Math.max(FAN_MIN_REACH, Math.min(FAN_MAX_REACH, FAN_REACH_PER_ATK * atk));
    }

    /** How far this player's fan blows, by their attack without level power. */
    public static double fanReach(ServerPlayer p) {
        return fanReach((float) (atk(p) / LevelPower.of(p)));
    }

    /** Blows {@code e} away along {@code dir}: seen flying, then landing {@code reach} blocks off. */
    public static void blowAway(ServerPlayer bull, LivingEntity e, Vec3 dir, double reach) {
        ServerLevel level = bull.serverLevel();
        WorldBorder border = level.getWorldBorder();
        Vec3 to = e.position().add(dir.scale(reach));
        double x = Math.max(border.getMinX() + 1, Math.min(border.getMaxX() - 1, to.x));
        double z = Math.max(border.getMinZ() + 1, Math.min(border.getMaxZ() - 1, to.z));
        CompoundTag t = e.getPersistentData();
        t.putLong(KEY_BLOWN_AT, level.getGameTime() + BLOWN_FLIGHT_TICKS);
        t.putDouble(KEY_BLOWN_X, x);
        t.putDouble(KEY_BLOWN_Z, z);
        e.push(dir.x * 3, 1.5, dir.z * 3);
        e.hurtMarked = true;
    }

    /** Sets a blown-away foe down far off, drifting in from above. Public for tests. */
    public static void landBlown(LivingEntity e) {
        CompoundTag t = e.getPersistentData();
        double x = t.getDouble(KEY_BLOWN_X), z = t.getDouble(KEY_BLOWN_Z);
        t.remove(KEY_BLOWN_AT);
        t.remove(KEY_BLOWN_X);
        t.remove(KEY_BLOWN_Z);
        ServerLevel level = (ServerLevel) e.level();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        double y = surface + 12;
        e.setDeltaMovement(Vec3.ZERO);
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) sp.teleportTo(level, x, y, z, sp.getYRot(), sp.getXRot());
        else e.teleportTo(x, y, z);
        e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 20, 0.5, 0.5, 0.5, 0.05);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || !e.getPersistentData().contains(KEY_BLOWN_AT)) return;
        if (e.level().getGameTime() >= e.getPersistentData().getLong(KEY_BLOWN_AT)) landBlown(e);
    }

    // ── Mixed-Iron Cudgel, in his hands ────────────────────────────

    /** A shockwave crossing the ground, one block a tick. */
    public static final class Shockwave {
        final ServerLevel level;
        final ServerPlayer owner; // a wave lives a dozen ticks at most
        final Vec3 dir;
        final int length;
        final float damage;
        final Set<Integer> struck = new HashSet<>();
        Vec3 front;
        int step;

        Shockwave(ServerPlayer p, int length, float damage) {
            this.level = p.serverLevel();
            this.owner = p;
            this.dir = flatLook(p);
            this.length = length;
            this.damage = damage;
            this.front = p.position();
        }

        public int step() { return step; }
        public boolean done() { return step >= length; }
    }

    private static final List<Shockwave> WAVES = new ArrayList<>();

    public static int shockwaveLength(int charge) { return charge >= 3 ? 12 : charge >= 2 ? 9 : 6; }
    public static float shockwaveDamage(float atk, int charge) { return atk * (charge >= 3 ? 1.2f : charge >= 2 ? 1.0f : 0.8f); }

    /** A shockwave from his feet where he looks, for charge 1-3, not yet moving. */
    public static Shockwave newShockwave(ServerPlayer p, int charge) {
        return new Shockwave(p, shockwaveLength(charge), shockwaveDamage(atk(p), charge));
    }

    /** Sends a shockwave on its way: the server moves it a block each tick. */
    public static Shockwave startShockwave(ServerPlayer p, int charge) {
        Shockwave w = newShockwave(p, charge);
        WAVES.add(w);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 0.6f);
        return w;
    }

    /** Moves a shockwave one block on. Returns false once it is spent or stopped by a wall. */
    public static boolean advance(Shockwave w) {
        if (w.done()) return false;
        Vec3 next = w.front.add(w.dir);
        BlockPos at = BlockPos.containing(next);
        // It climbs a step or drops down one; a wall stops it
        if (solid(w.level, at)) {
            if (solid(w.level, at.above())) { w.step = w.length; return false; }
            next = next.add(0, 1, 0);
        } else if (!solid(w.level, at.below()) && solid(w.level, at.below(2))) {
            next = next.add(0, -1, 0);
        }
        w.front = next;
        w.step++;
        ServerPlayer bull = w.owner.isRemoved() ? null : w.owner;
        for (LivingEntity e : w.level.getEntitiesOfClass(LivingEntity.class, new AABB(next, next).inflate(1.5, 2, 1.5),
                e -> bull == null ? e.isAlive() : isFoe(e, bull))) {
            if (!w.struck.add(e.getId())) continue;
            e.hurt(bull != null ? bull.damageSources().mobAttack(bull) : w.level.damageSources().generic(), w.damage);
            e.push(w.dir.x * 0.5, 0.8, w.dir.z * 0.5);
            e.hurtMarked = true;
        }
        BlockState ground = w.level.getBlockState(BlockPos.containing(next).below());
        if (!ground.isAir()) {
            w.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), next.x, next.y + 0.2, next.z,
                    20, 0.6, 0.2, 0.6, 0.2);
        }
        if (w.step % 3 == 0) w.level.playSound(null, next.x, next.y, next.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.5f);
        return !w.done();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (WAVES.isEmpty()) return;
        for (Iterator<Shockwave> it = WAVES.iterator(); it.hasNext(); ) {
            if (!advance(it.next())) it.remove();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        WAVES.clear(); // a wave in flight holds its world
    }

    // ── ticking and lifecycle ──────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        boolean bull = BullDemonCharacter.is(p);
        if (isRushing(p)) {
            if (bull) tickRush(p);
            else p.getPersistentData().remove(KEY_RUSHING);
        }
        if (inGreatBull(p)) {
            int tier = bull ? tier(p, "bdk_bull") : 0;
            TransformationData data = TransformationManager.getTransformation(p);
            boolean stillBull = data != null && data.getEntityType() == com.jttw.engine.registry.ModEntities.GREAT_WHITE_BULL.get();
            if (tier < 1 || !stillBull || !QiData.spend(p, GREAT_BULL_QI_PER_TICK)) {
                endGreatBull(p);
            } else {
                applyGreatBull(p, tier);
                if (p.tickCount % 20 == 0) QiManager.syncNow(p);
            }
        }
        if (p.tickCount % 20 == 0) thickHide(p, bull ? tier(p, "bdk_hide") : 0);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            p.getPersistentData().remove(KEY_RUSHING);
            if (inGreatBull(p)) endGreatBull(p);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().remove(KEY_RUSHING);
        event.getEntity().getPersistentData().remove(KEY_GREAT_BULL);
    }
}
