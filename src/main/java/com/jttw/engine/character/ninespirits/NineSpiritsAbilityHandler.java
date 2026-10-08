package com.jttw.engine.character.ninespirits;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncLeadHeadPacket;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.registry.ModAttributes;
import com.jttw.engine.transformation.FormClassifier;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The Grand Saint of Nine Spirits' abilities.
 *
 * <ul>
 *   <li><b>V, Heads of Many Elements:</b> fires the leading head's projectile: fire (a fireball
 *       that bursts and sets foes alight), thunder (a bolt that jumps to the nearest foes),
 *       frost (a shard that slows and freezes). Sneak + V turns the next head to lead. Each
 *       tier wakes another head.</li>
 *   <li><b>B, Heaven-Shaking Roar:</b> knocks back and slows everything in a cone in front;
 *       II leaves them dizzy and unable to fight back; III also drops flyers out of the sky
 *       and shatters projectiles in the cone.</li>
 *   <li><b>N, Golden Mane Flare:</b> blinds everything looking at him within 12 / 20 blocks.</li>
 *   <li><b>M, Nine-Headed True Form:</b> toggle; tougher, and each melee hit strikes with
 *       2 / 3 / 4 heads; drains Qi.</li>
 *   <li><b>Nine Lives</b> (passive): survives killing blows, 1 / 2 / 3 times; a spent life
 *       comes back after 5 minutes.</li>
 * </ul>
 * Foes rung by the Bell of Taiyi take {@value #BELL_MARK_BONUS}x damage from his heads.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class NineSpiritsAbilityHandler {

    private NineSpiritsAbilityHandler() {}

    // ── keys and numbers ───────────────────────────────────────────

    public static final String KEY_HEAD = "jttw_ns_head";
    private static final String KEY_HEADS_READY_AT = "jttw_ns_heads_ready_at";
    private static final String KEY_ROAR_READY_AT = "jttw_ns_roar_ready_at";
    private static final String KEY_FLARE_READY_AT = "jttw_ns_flare_ready_at";
    public static final String KEY_FORM = "jttw_ns_form";
    public static final String KEY_LIVES_USED = "jttw_ns_lives_used";
    public static final String KEY_LIFE_BACK_AT = "jttw_ns_life_back_at";
    /** On a head's projectile: which element, and the damage it carries. */
    public static final String TAG_ELEMENT = "jttw_ns_element";
    public static final String TAG_DAMAGE = "jttw_ns_damage";
    /** On a foe rung by the Bell of Taiyi: game time the mark lasts until. */
    public static final String KEY_BELL_MARK = "jttw_bell_mark_until";
    public static final float BELL_MARK_BONUS = 1.3f;

    public static final int FIRE = 0, THUNDER = 1, FROST = 2;
    private static final String[] HEAD_NAMES = {"Fire", "Thunder", "Frost"};

    public static final int HEADS_COOLDOWN = 20;
    public static final float HEADS_QI = 6f;
    public static final int ROAR_COOLDOWN = 200;
    public static final float ROAR_QI = 15f;
    public static final int FLARE_COOLDOWN = 300;
    public static final float FLARE_QI = 12f;
    public static final float FORM_QI_PER_TICK = 0.25f;
    public static final int LIFE_RECHARGE_TICKS = 6000;
    public static final float LIFE_SAVED_HEALTH = 8f;

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

    private static int tier(ServerPlayer p, String branch) {
        return NineSpiritsSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(p), branch);
    }

    private static boolean isFoe(LivingEntity e, ServerPlayer p) {
        return e != p && e.isAlive() && !e.isAlliedTo(p);
    }

    // ── V: Heads of Many Elements ──────────────────────────────────

    /** The head that leads now (never one not yet woken). */
    public static int leadHead(ServerPlayer p) {
        int heads = tier(p, "ns_heads");
        return heads <= 0 ? FIRE : Math.min(p.getPersistentData().getInt(KEY_HEAD), heads - 1);
    }

    /** Sneak + V: the next woken head leads. */
    public static void rotateHead(ServerPlayer p) {
        int heads = tier(p, "ns_heads");
        if (heads < 1) return;
        int next = (leadHead(p) + 1) % heads;
        p.getPersistentData().putInt(KEY_HEAD, next);
        p.displayClientMessage(Component.literal("§6The " + HEAD_NAMES[next] + " head leads."), true);
        syncHead(p);
    }

    private static final String KEY_HEAD_SENT = "jttw_ns_head_sent";

    /** The head the player's client should tint the view for: -1 if none leads. */
    public static int shownHead(ServerPlayer p) {
        return NineSpiritsCharacter.is(p) && tier(p, "ns_heads") >= 1 ? leadHead(p) : -1;
    }

    public static void syncHead(ServerPlayer p) {
        int head = shownHead(p);
        p.getPersistentData().putInt(KEY_HEAD_SENT, head + 1); // stored +1 so 0 means "never sent"
        NetworkHandler.sendToPlayer(p, new SyncLeadHeadPacket(head));
    }

    /** Damage a head's projectile carries: ATK x 1.0 / 1.3 / 1.6 by the heads' tier. */
    public static float headDamage(float attack, int tier) {
        return attack * (tier >= 3 ? 1.6f : tier == 2 ? 1.3f : 1.0f);
    }

    /** V: fire the leading head. */
    public static void fireHead(ServerPlayer p) {
        int heads = tier(p, "ns_heads");
        if (heads < 1 || onCooldown(p, KEY_HEADS_READY_AT) || !spendQi(p, HEADS_QI)) return;
        startCooldown(p, KEY_HEADS_READY_AT, "ns_heads", HEADS_COOLDOWN);
        float damage = headDamage((float) p.getAttributeValue(Attributes.ATTACK_DAMAGE), heads);
        int head = leadHead(p);
        if (head == THUNDER) {
            thunderBolt(p, damage, heads >= 3 ? 4 : 2);
            return;
        }
        Snowball shot = new Snowball(p.level(), p);
        shot.setItem(new ItemStack(head == FIRE ? Items.FIRE_CHARGE : Items.BLUE_ICE));
        shot.setNoGravity(true);
        shot.getPersistentData().putInt(TAG_ELEMENT, head);
        shot.getPersistentData().putFloat(TAG_DAMAGE, damage);
        shot.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, 2.2f, 0f);
        p.level().addFreshEntity(shot);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                head == FIRE ? SoundEvents.BLAZE_SHOOT : SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    /** Thunder: an instant bolt to the first foe under the crosshair, then on to the nearest others. */
    public static List<LivingEntity> thunderBolt(ServerPlayer p, float damage, int jumps) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(32));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, p, eye, end,
                new AABB(eye, end).inflate(1.0), e -> e instanceof LivingEntity le && isFoe(le, p));
        List<LivingEntity> struck = new ArrayList<>();
        sparkLine(level, eye, hit == null ? end : hit.getEntity().getEyePosition());
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4F, 1.6F);
        if (hit == null) return struck;
        LivingEntity current = (LivingEntity) hit.getEntity();
        float dmg = damage;
        for (int i = 0; i <= jumps && current != null; i++) {
            struck.add(current);
            current.hurt(p.damageSources().indirectMagic(p, p), dmg * markBonus(current));
            dmg *= 0.75f;
            final LivingEntity from = current;
            LivingEntity nextTarget = null;
            double best = 6.0;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(6.0),
                    e -> isFoe(e, p) && !struck.contains(e))) {
                double d = e.distanceTo(from);
                if (d < best) { best = d; nextTarget = e; }
            }
            if (nextTarget != null) sparkLine(level, from.getEyePosition(), nextTarget.getEyePosition());
            current = nextTarget;
        }
        return struck;
    }

    private static void sparkLine(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        int n = (int) Math.max(2, step.length() * 2);
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(step.scale(i / (double) n));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    private static float markBonus(LivingEntity target) {
        return target.getPersistentData().getLong(KEY_BELL_MARK) > target.level().getGameTime() ? BELL_MARK_BONUS : 1f;
    }

    /** A head's fireball or frost shard lands. */
    @SubscribeEvent
    public static void onHeadShotLands(ProjectileImpactEvent event) {
        Projectile shot = event.getProjectile();
        if (shot.level().isClientSide || !shot.getPersistentData().contains(TAG_ELEMENT)) return;
        if (!(shot.getOwner() instanceof ServerPlayer owner)) { shot.discard(); event.setCanceled(true); return; }
        HitResult result = event.getRayTraceResult();
        if (result instanceof EntityHitResult eh && eh.getEntity() == owner) { event.setCanceled(true); return; }
        int element = shot.getPersistentData().getInt(TAG_ELEMENT);
        float damage = shot.getPersistentData().getFloat(TAG_DAMAGE);
        ServerLevel level = (ServerLevel) shot.level();
        Vec3 at = result.getLocation();
        if (element == FIRE) {
            // Bursts: every foe within 2.5 blocks is hit and set alight
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(2.5),
                    e -> isFoe(e, owner))) {
                e.hurt(owner.damageSources().indirectMagic(shot, owner), damage * markBonus(e));
                e.setSecondsOnFire(4);
            }
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 30, 0.8, 0.6, 0.8, 0.05);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else if (result instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity target) {
            // Frost: slows, and freezes in place
            target.hurt(owner.damageSources().indirectMagic(shot, owner), damage * markBonus(target));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), owner);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9), owner);
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 60));
            level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.5, at.z, 25, 0.4, 0.6, 0.4, 0.02);
        } else {
            level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 15, 0.3, 0.3, 0.3, 0.02);
        }
        shot.discard();
        event.setCanceled(true);
    }

    // ── B: Heaven-Shaking Roar ─────────────────────────────────────

    public static double roarRange(int tier) {
        return tier >= 3 ? 12 : tier == 2 ? 10 : 8;
    }

    /** True if {@code e} is within the roar's 120-degree cone in front of {@code p} and in range. */
    public static boolean inRoarCone(ServerPlayer p, Entity e, double range) {
        Vec3 to = e.getBoundingBox().getCenter().subtract(p.getEyePosition());
        if (to.length() > range) return false;
        return to.normalize().dot(p.getLookAngle()) >= 0.5;
    }

    public static void roar(ServerPlayer p) {
        int tier = tier(p, "ns_roar");
        if (tier < 1 || onCooldown(p, KEY_ROAR_READY_AT) || !spendQi(p, ROAR_QI)) return;
        startCooldown(p, KEY_ROAR_READY_AT, "ns_roar", ROAR_COOLDOWN);
        double range = roarRange(tier);
        ServerLevel level = p.serverLevel();
        AABB box = p.getBoundingBox().inflate(range);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(e, p) && inRoarCone(p, e, range))) {
            Vec3 away = e.position().subtract(p.position()).multiply(1, 0, 1).normalize();
            boolean grounded = tier >= 3 && !e.onGround() && (FormClassifier.isFlyer(e) || e.isNoGravity());
            e.setDeltaMovement(away.x * 1.5, grounded ? -1.5 : 0.4, away.z * 1.5);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), p);
            if (grounded) e.removeEffect(MobEffects.SLOW_FALLING);
            if (tier >= 2) {
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0), p);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 9), p);
                if (e instanceof Mob mob) mob.setTarget(null);
            }
        }
        if (tier >= 3) {
            for (Projectile shot : level.getEntitiesOfClass(Projectile.class, box,
                    s -> s.getOwner() != p && inRoarCone(p, s, range))) {
                level.sendParticles(ParticleTypes.CLOUD, shot.getX(), shot.getY(), shot.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                shot.discard();
            }
        }
        Vec3 look = p.getLookAngle();
        for (int i = 1; i <= range; i++) {
            Vec3 at = p.getEyePosition().add(look.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 2.0F, 0.7F);
    }

    // ── N: Golden Mane Flare ───────────────────────────────────────

    /** True if {@code e} is looking at {@code p}: facing them and with a clear line of sight. */
    public static boolean looksAt(LivingEntity e, ServerPlayer p) {
        Vec3 toP = p.getEyePosition().subtract(e.getEyePosition()).normalize();
        return e.getViewVector(1f).normalize().dot(toP) > 0.6 && e.hasLineOfSight(p);
    }

    public static void flare(ServerPlayer p) {
        int tier = tier(p, "ns_flare");
        if (tier < 1 || onCooldown(p, KEY_FLARE_READY_AT) || !spendQi(p, FLARE_QI)) return;
        startCooldown(p, KEY_FLARE_READY_AT, "ns_flare", FLARE_COOLDOWN);
        double range = tier >= 2 ? 20 : 12;
        ServerLevel level = p.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range),
                e -> isFoe(e, p) && e.distanceTo(p) <= range && looksAt(e, p))) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, tier >= 2 ? 100 : 60, 0), p);
            if (tier >= 2) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), p);
            if (e instanceof Mob mob) mob.setTarget(null);
        }
        level.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.8f, 0.2f), 2.0f),
                p.getX(), p.getEyeY(), p.getZ(), 80, 1.2, 1.0, 1.2, 0.1);
        level.sendParticles(ParticleTypes.FLASH, p.getX(), p.getEyeY(), p.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
    }

    // ── M: Nine-Headed True Form ───────────────────────────────────

    public static boolean inTrueForm(Player p) {
        return p.getPersistentData().getBoolean(KEY_FORM);
    }

    /** Melee damage multiplier in true form: each extra head strikes for half again. */
    public static float formHitMultiplier(int tier) {
        int heads = tier >= 3 ? 4 : tier == 2 ? 3 : 2;
        return 1f + 0.5f * (heads - 1);
    }

    public static double formResistance(int tier) {
        return tier >= 3 ? 40 : tier == 2 ? 30 : 20;
    }

    public static void toggleTrueForm(ServerPlayer p) {
        int tier = tier(p, "ns_form");
        if (tier < 1) return;
        if (inTrueForm(p)) { endTrueForm(p); return; }
        if (QiData.getCurrent(p) < FORM_QI_PER_TICK * 20) {
            p.displayClientMessage(Component.literal("§cNot enough Qi."), true);
            return;
        }
        p.getPersistentData().putBoolean(KEY_FORM, true);
        applyFormModifiers(p, tier);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5F, 0.5F);
        p.displayClientMessage(Component.literal("§6§lThe nine heads rise."), true);
    }

    public static void endTrueForm(ServerPlayer p) {
        p.getPersistentData().remove(KEY_FORM);
        applyFormModifiers(p, 0);
    }

    private static void applyFormModifiers(ServerPlayer p, int tier) {
        double res = tier > 0 ? formResistance(tier) : 0;
        PassiveModifiers.add(p, ModAttributes.PHYSICAL_RESISTANCE.get(), "ns_form_pres", res);
        PassiveModifiers.add(p, ModAttributes.MAGIC_RESISTANCE.get(), "ns_form_mres", res);
        PassiveModifiers.add(p, Attributes.KNOCKBACK_RESISTANCE, "ns_form_kb", tier > 0 ? 0.6 : 0);
    }

    // ── Nine Lives ─────────────────────────────────────────────────

    public static int livesLeft(Player p) {
        if (!(p instanceof ServerPlayer sp) || !NineSpiritsCharacter.is(p)) return 0;
        return Math.max(0, tier(sp, "ns_lives") - p.getPersistentData().getInt(KEY_LIVES_USED));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || livesLeft(p) <= 0) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // /kill, the void
        event.setCanceled(true);
        CompoundTag pd = p.getPersistentData();
        int used = pd.getInt(KEY_LIVES_USED) + 1;
        pd.putInt(KEY_LIVES_USED, used);
        if (used == 1 || !pd.contains(KEY_LIFE_BACK_AT)) pd.putLong(KEY_LIFE_BACK_AT, p.level().getGameTime() + LIFE_RECHARGE_TICKS);
        p.setHealth(LIFE_SAVED_HEALTH);
        p.clearFire();
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1.0, 0.5, 0.4);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        p.displayClientMessage(Component.literal("§6A head falls; the lion lives. §7(" + livesLeft(p) + " lives left)"), true);
    }

    // ── ticks and hits ─────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        CompoundTag pd = p.getPersistentData();
        // The view tint follows the leading head (a new head unlocked, a respec, a new character)
        if (p.tickCount % 40 == 0 && pd.getInt(KEY_HEAD_SENT) != shownHead(p) + 1) syncHead(p);
        // A spent life comes back
        int used = pd.getInt(KEY_LIVES_USED);
        if (used > 0 && p.level().getGameTime() >= pd.getLong(KEY_LIFE_BACK_AT)) {
            pd.putInt(KEY_LIVES_USED, used - 1);
            if (used - 1 > 0) pd.putLong(KEY_LIFE_BACK_AT, p.level().getGameTime() + LIFE_RECHARGE_TICKS);
            else pd.remove(KEY_LIFE_BACK_AT);
        }
        // True form drains Qi and wears a golden aura
        if (inTrueForm(p)) {
            if (!NineSpiritsCharacter.is(p) || !QiData.spend(p, FORM_QI_PER_TICK)) {
                endTrueForm(p);
                p.displayClientMessage(Component.literal("§7The nine heads sink back."), true);
            } else if (p.tickCount % 4 == 0) {
                p.serverLevel().sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.75f, 0.1f), 1.5f),
                        p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.6, 0.8, 0.6, 0.0);
            }
            if (p.tickCount % 10 == 0) QiManager.syncIfChanged(p);
        }
    }

    /** True form: a melee hit strikes with every head. */
    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || !inTrueForm(p)) return;
        if (event.getSource().getDirectEntity() != p || !event.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        event.setAmount(event.getAmount() * formHitMultiplier(tier(p, "ns_form")));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            if (inTrueForm(p)) endTrueForm(p); // modifiers are not saved; start the session out of form
            syncHead(p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) syncHead(p);
    }

    /** Lives spent and their recharge carry through death, so dying cannot refill them. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag src = event.getOriginal().getPersistentData(), dst = event.getEntity().getPersistentData();
        for (String key : Set.of(KEY_LIVES_USED, KEY_LIFE_BACK_AT, KEY_HEAD)) {
            if (src.contains(key)) dst.put(key, src.get(key).copy());
        }
    }
}
