package com.jttw.engine.transformation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.stats.LevelPower;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Handles mob-specific abilities that activate while a player is transformed.
 *
 * <ul>
 *   <li>Ghast/Blaze/Ender Dragon: right-click shoots fireballs (2s cooldown)</li>
 *   <li>Warden: right-click fires sonic boom at looked-at entity (5s cooldown, 5 Qi)</li>
 *   <li>Skeleton/Stray/Wither Skeleton: right-click fires arrows (1s cooldown)</li>
 *   <li>Bee: melee attacks apply Slowness I (3s) + Poison I (3s)</li>
 *   <li>Cave Spider/Spider: melee attacks apply Poison II (4s) + Slowness I (2s)</li>
 *   <li>Zombie variants: melee attacks apply Slowness I (3s)</li>
 *   <li>Horse variants: step assist (+1.5 blocks) and enhanced jump (~3 blocks)</li>
 *   <li>Witch: right-click throws poison splash potion (3s cd); immune to poison</li>
 *   <li>Llama: right-click spits LlamaSpit (1s cd)</li>
 *   <li>Guardian: right-click starts sustained laser on looked-at entity within 15 blocks;
 *       channels up to 5s, 1 dmg/10t, Mining Fatigue I, shift cancels, 3s cd</li>
 *   <li>Snow Golem: right-click throws snowball dealing 3 damage (0.5s cd)</li>
 *   <li>Iron Golem: right-click ground slam (5s cd); melee launches target up</li>
 *   <li>Evoker: right-click spawns fangs (3s cd); shift+right-click summons vex (5s cd)</li>
 *   <li>Ravager: melee does 3x knockback</li>
 *   <li>Slime/Magma Cube: immune to fall damage</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class TransformationAbilityHandler {

    private TransformationAbilityHandler() {}

    // --- Cooldown tracking (UUID -> game tick when cooldown expires) ---
    private static final Map<UUID, Long> FIREBALL_COOLDOWNS = new HashMap<>();
    private static final long FIREBALL_COOLDOWN_TICKS = 40L; // 2 seconds

    private static final Map<UUID, Long> SONIC_BOOM_COOLDOWNS = new HashMap<>();
    private static final long SONIC_BOOM_COOLDOWN_TICKS = 100L; // 5 seconds
    /** Sonic boom damage at level 1; it grows with the transformed player's level power. */
    private static final float SONIC_BOOM_DAMAGE = 10.0F;
    private static final double SONIC_BOOM_RANGE = 15.0D;
    private static final float SONIC_BOOM_QI_COST = 5.0F;

    private static final Map<UUID, Long> ARROW_COOLDOWNS = new HashMap<>();
    private static final long ARROW_COOLDOWN_TICKS = 20L; // 1 second

    private static final Map<UUID, Long> ENDER_TP_COOLDOWNS = new HashMap<>();
    private static final long ENDER_TP_COOLDOWN_TICKS = 60L; // 3 seconds
    private static final double ENDER_TP_RANGE = 20.0D;

    private static final Map<UUID, Long> WITHER_SKULL_COOLDOWNS = new HashMap<>();
    private static final long WITHER_SKULL_COOLDOWN_TICKS = 40L; // 2 seconds

    private static final Map<UUID, Integer> CREEPER_CHARGE = new HashMap<>();
    private static final Set<UUID> CHARGED_CREEPERS = new HashSet<>();
    private static final int CREEPER_CHARGE_TICKS = 30; // 1.5 seconds to detonate
    private static final float CREEPER_EXPLOSION_POWER = 3.0F;
    private static final float CHARGED_EXPLOSION_POWER = 6.0F;
    private static final Map<UUID, Long> CREEPER_EXPLOSION_COOLDOWNS = new HashMap<>();
    private static final long CREEPER_EXPLOSION_COOLDOWN_TICKS = 600L; // 30 seconds

    private static final Map<UUID, Long> SILVERFISH_PHASE_COOLDOWNS = new HashMap<>();
    private static final long SILVERFISH_PHASE_COOLDOWN_TICKS = 20L; // 1 second

    /** Blocks silverfish can phase through */
    private static final Set<Block> SILVERFISH_PHASEABLE = Set.of(
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.DEEPSLATE,
            Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS
    );

    /** Villager/Iron Golem: trade discount types */
    private static final Set<EntityType<?>> VILLAGER_DISCOUNT_TYPES = Set.of(
            EntityType.VILLAGER, EntityType.IRON_GOLEM
    );

    // --- New cooldown maps ---
    private static final Map<UUID, Long> WITCH_POTION_COOLDOWNS = new HashMap<>();
    private static final long WITCH_POTION_COOLDOWN_TICKS = 60L; // 3 seconds

    private static final Map<UUID, Long> LLAMA_SPIT_COOLDOWNS = new HashMap<>();
    private static final long LLAMA_SPIT_COOLDOWN_TICKS = 20L; // 1 second

    private static final Map<UUID, Long> GUARDIAN_LASER_COOLDOWNS = new HashMap<>();
    private static final long GUARDIAN_LASER_COOLDOWN_TICKS = 60L; // 3 seconds
    /** Laser damage per interval at level 1; it grows with the transformed player's level power. */
    private static final float GUARDIAN_LASER_TICK_DAMAGE = 1.0F;
    private static final int GUARDIAN_LASER_DAMAGE_INTERVAL = 10; // ticks between damage
    private static final double GUARDIAN_LASER_RANGE = 15.0D;
    private static final int GUARDIAN_LASER_MAX_TICKS = 100; // 5 seconds max channel

    /** Tracks active guardian laser beams: player UUID → LaserState(targetId, startTick). */
    private static final Map<UUID, LaserState> GUARDIAN_LASERS = new HashMap<>();
    private record LaserState(int targetId, long startTick) {}

    private static final Map<UUID, Long> SNOWBALL_COOLDOWNS = new HashMap<>();
    private static final long SNOWBALL_COOLDOWN_TICKS = 10L; // 0.5 seconds
    /** Snowball impact damage at level 1; it grows with the thrower's level power. */
    private static final float SNOWBALL_DAMAGE = 3.0F;

    private static final Map<UUID, Long> IRON_GOLEM_SLAM_COOLDOWNS = new HashMap<>();
    private static final long IRON_GOLEM_SLAM_COOLDOWN_TICKS = 100L; // 5 seconds
    /** Slam damage at level 1; it grows with the transformed player's level power. */
    private static final float IRON_GOLEM_SLAM_DAMAGE = 6.0F;
    private static final double IRON_GOLEM_SLAM_RADIUS = 4.0D;
    private static final double IRON_GOLEM_SLAM_LAUNCH_Y = 0.8D;
    private static final double IRON_GOLEM_MELEE_LAUNCH_Y = 0.5D;

    private static final Map<UUID, Long> EVOKER_FANGS_COOLDOWNS = new HashMap<>();
    private static final long EVOKER_FANGS_COOLDOWN_TICKS = 60L; // 3 seconds

    private static final Map<UUID, Long> EVOKER_VEX_COOLDOWNS = new HashMap<>();
    private static final long EVOKER_VEX_COOLDOWN_TICKS = 100L; // 5 seconds

    /** Tracks spawned vex entities and their despawn tick */
    private static final List<VexDespawnEntry> ACTIVE_VEX = new ArrayList<>();

    private static record VexDespawnEntry(Vex vex, long despawnTick) {}

    /** Tracks snowballs that need to deal extra damage on impact */
    private static final List<SnowballEntry> ACTIVE_SNOWBALLS = new ArrayList<>();

    private static record SnowballEntry(Snowball snowball, UUID ownerUUID) {}

    // --- Entity type sets for ability groupings ---

    /** Mobs that shoot fireballs on right-click */
    private static final Set<EntityType<?>> FIREBALL_LARGE = Set.of(
            EntityType.GHAST, EntityType.ENDER_DRAGON
    );
    private static final Set<EntityType<?>> FIREBALL_SMALL = Set.of(
            EntityType.BLAZE
    );

    /** Warden: sonic boom on right-click */
    private static final Set<EntityType<?>> WARDEN_TYPES = Set.of(
            EntityType.WARDEN
    );

    /** Skeleton variants: arrow shooting on right-click */
    private static final Set<EntityType<?>> SKELETON_TYPES = Set.of(
            EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON
    );

    /** Bee: Slowness I + Poison I on melee */
    private static final Set<EntityType<?>> BEE_TYPES = Set.of(
            EntityType.BEE
    );

    /** Spider/Cave Spider: Poison II + Slowness I on melee */
    private static final Set<EntityType<?>> SPIDER_TYPES = Set.of(
            EntityType.SPIDER, EntityType.CAVE_SPIDER
    );

    /** Zombie variants: Slowness I on melee */
    private static final Set<EntityType<?>> ZOMBIE_TYPES = Set.of(
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER
    );

    /** Horse variants: step assist + enhanced jump */
    private static final Set<EntityType<?>> HORSE_TYPES = Set.of(
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
            EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE
    );

    /** Guardian variants: laser on right-click */
    private static final Set<EntityType<?>> GUARDIAN_TYPES = Set.of(
            EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN
    );

    /** Slime/Magma Cube: fall immunity */
    private static final Set<EntityType<?>> SLIME_TYPES = Set.of(
            EntityType.SLIME, EntityType.MAGMA_CUBE
    );

    private static final double HORSE_JUMP_VELOCITY = 0.7D;
    private static final float HORSE_SAFE_FALL = 6.0F;

    // NBT key to track whether horse jump boost was already applied this jump

    // -------------------------------------------------------------------------
    // Right-click abilities (fireball, sonic boom, arrows, etc.)
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        // Client-side only event in Forge; we need RightClickItem for server
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        useFormAbilities(sp);
    }

    /** A form's right-click abilities (also used for empty-hand clicks, see UseFormPacket). */
    public static void useFormAbilities(ServerPlayer sp) {
        tryShootFireball(sp);
        trySonicBoom(sp);
        tryShootArrow(sp);
        tryEnderTeleport(sp);
        tryWitherSkull(sp);
        tryCreeperCharge(sp);
        tryWitchPotion(sp);
        tryLlamaSpit(sp);
        tryGuardianLaser(sp);
        trySnowballThrow(sp);
        tryIronGolemSlam(sp);
        tryEvokerAbility(sp);
    }

    private static void tryShootFireball(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;

        EntityType<?> morphType = data.getEntityType();
        boolean isLarge = FIREBALL_LARGE.contains(morphType);
        boolean isSmall = FIREBALL_SMALL.contains(morphType);
        if (!isLarge && !isSmall) return;

        // Check cooldown
        long now = sp.level().getGameTime();
        Long cooldownEnd = FIREBALL_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        // Set cooldown
        FIREBALL_COOLDOWNS.put(sp.getUUID(), now + FIREBALL_COOLDOWN_TICKS);

        // Calculate direction from player's look vector
        Vec3 look = sp.getLookAngle();
        Vec3 pos = sp.getEyePosition();

        if (isLarge) {
            LargeFireball fireball = new LargeFireball(
                    sp.level(), sp,
                    look,
                    1 // explosion power
            );
            fireball.setPos(pos.x + look.x * 1.5, pos.y + look.y * 1.5, pos.z + look.z * 1.5);
            sp.level().addFreshEntity(fireball);
        } else {
            SmallFireball fireball = new SmallFireball(
                    sp.level(), sp,
                    look
            );
            fireball.setPos(pos.x + look.x * 1.5, pos.y + look.y * 1.5, pos.z + look.z * 1.5);
            sp.level().addFreshEntity(fireball);
        }
    }

    // -------------------------------------------------------------------------
    // Warden sonic boom (right-click)
    // -------------------------------------------------------------------------

    private static void trySonicBoom(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;

        EntityType<?> morphType = data.getEntityType();
        if (!WARDEN_TYPES.contains(morphType)) return;

        // Check cooldown
        long now = sp.level().getGameTime();
        Long cooldownEnd = SONIC_BOOM_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        // Raycast to find the entity the player is looking at
        LivingEntity target = findLookedAtEntity(sp, SONIC_BOOM_RANGE);
        if (target == null) return;

        // Set cooldown
        SONIC_BOOM_COOLDOWNS.put(sp.getUUID(), now + SONIC_BOOM_COOLDOWN_TICKS);

        // Drain Qi if available (don't require it)
        if (QiData.spend(sp, SONIC_BOOM_QI_COST)) {
            QiManager.syncNow(sp);
        }

        // Apply sonic boom damage (bypasses armor)
        ServerLevel serverLevel = sp.serverLevel();
        DamageSource sonicBoomDamage = serverLevel.damageSources().sonicBoom(sp);
        target.hurt(sonicBoomDamage, LevelPower.scaled(sp, SONIC_BOOM_DAMAGE));

        // Knockback the target away from the player
        double dx = target.getX() - sp.getX();
        double dz = target.getZ() - sp.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0) {
            target.push(dx / dist * 2.5D, 0.5D, dz / dist * 2.5D);
        }

        // Spawn sonic boom particles along the path from player to target
        Vec3 start = sp.getEyePosition();
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        Vec3 step = direction.normalize();
        for (double d = 0; d < length; d += 1.0) {
            Vec3 particlePos = start.add(step.scale(d));
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Play the warden sonic boom sound
        serverLevel.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 1.0F);
    }

    /**
     * Raycasts from the player's eye position along their look direction to find
     * the first LivingEntity within range.
     */
    @Nullable
    private static LivingEntity findLookedAtEntity(ServerPlayer sp, double range) {
        Vec3 eyePos = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(range));

        // Build an AABB that encompasses the entire ray
        AABB searchBox = sp.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        List<net.minecraft.world.entity.Entity> candidates = sp.level().getEntities(sp, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && e != sp);

        LivingEntity closest = null;
        double closestDist = range + 1.0;

        for (net.minecraft.world.entity.Entity entity : candidates) {
            AABB entityBB = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.3F);
            Optional<Vec3> hitOpt = entityBB.clip(eyePos, endPos);
            if (hitOpt.isPresent()) {
                double d = eyePos.distanceTo(hitOpt.get());
                if (d < closestDist) {
                    closestDist = d;
                    closest = (LivingEntity) entity;
                }
            }
        }
        return closest;
    }

    // -------------------------------------------------------------------------
    // Skeleton arrow shooting (right-click)
    // -------------------------------------------------------------------------

    private static void tryShootArrow(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;

        EntityType<?> morphType = data.getEntityType();
        if (!SKELETON_TYPES.contains(morphType)) return;

        // Check cooldown
        long now = sp.level().getGameTime();
        Long cooldownEnd = ARROW_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        // Set cooldown
        ARROW_COOLDOWNS.put(sp.getUUID(), now + ARROW_COOLDOWN_TICKS);

        // Create and fire an arrow
        Vec3 look = sp.getLookAngle();
        Arrow arrow = new Arrow(sp.level(), sp, new ItemStack(Items.ARROW), null);
        arrow.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 3.0F, 1.0F);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED; // no free arrows from transform

        // Wither Skeleton variant: arrows apply Wither effect
        if (morphType == EntityType.WITHER_SKELETON) {
            arrow.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0)); // 3s Wither I
        }

        // Stray variant: arrows apply Slowness (like vanilla stray arrows)
        if (morphType == EntityType.STRAY) {
            arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 0)); // 30s Slowness I
        }

        sp.level().addFreshEntity(arrow);

        // Play skeleton shooting sound
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.SKELETON_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (sp.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    // -------------------------------------------------------------------------
    // Witch: throw poison splash potion (right-click, 3s cooldown)
    // -------------------------------------------------------------------------

    private static void tryWitchPotion(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        if (data.getEntityType() != EntityType.WITCH) return;

        long now = sp.level().getGameTime();
        Long cooldownEnd = WITCH_POTION_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        WITCH_POTION_COOLDOWNS.put(sp.getUUID(), now + WITCH_POTION_COOLDOWN_TICKS);

        // Create a Poison II splash potion item
        ItemStack potionStack = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_POISON); // Poison II

        ThrownPotion thrownPotion = new ThrownPotion(sp.level(), sp);
        thrownPotion.setItem(potionStack);
        thrownPotion.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), -20.0F, 0.5F, 1.0F);
        sp.level().addFreshEntity(thrownPotion);

        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.WITCH_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // -------------------------------------------------------------------------
    // Llama: spit (right-click, 1s cooldown)
    // -------------------------------------------------------------------------

    private static void tryLlamaSpit(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        EntityType<?> morphType = data.getEntityType();
        if (morphType != EntityType.LLAMA && morphType != EntityType.TRADER_LLAMA) return;

        long now = sp.level().getGameTime();
        Long cooldownEnd = LLAMA_SPIT_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        LLAMA_SPIT_COOLDOWNS.put(sp.getUUID(), now + LLAMA_SPIT_COOLDOWN_TICKS);

        net.minecraft.world.entity.projectile.LlamaSpit spit =
                new net.minecraft.world.entity.projectile.LlamaSpit(
                        EntityType.LLAMA_SPIT, sp.level());
        Vec3 look = sp.getLookAngle();
        spit.setPos(sp.getX() + look.x * 1.5,
                sp.getEyeY() + look.y * 1.5,
                sp.getZ() + look.z * 1.5);
        spit.setDeltaMovement(look.scale(1.5));
        spit.setOwner(sp);
        sp.level().addFreshEntity(spit);

        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.LLAMA_SPIT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // -------------------------------------------------------------------------
    // Guardian: laser at looked-at entity (right-click, 3s cooldown)
    // -------------------------------------------------------------------------

    /** Right-click: lock onto a target to start the sustained laser beam. */
    private static void tryGuardianLaser(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        if (!GUARDIAN_TYPES.contains(data.getEntityType())) return;

        // Already channelling — ignore
        if (GUARDIAN_LASERS.containsKey(sp.getUUID())) return;

        long now = sp.level().getGameTime();
        Long cooldownEnd = GUARDIAN_LASER_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        // Find target within range
        LivingEntity target = findLookedAtEntity(sp, GUARDIAN_LASER_RANGE);
        if (target == null) return;

        // Start sustained beam
        GUARDIAN_LASERS.put(sp.getUUID(), new LaserState(target.getId(), now));

        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    /**
     * Tick the sustained guardian laser beam. While channelling:
     * <ul>
     *   <li>Spawns bubble particles between player and target each tick</li>
     *   <li>Deals 1 damage every 10 ticks</li>
     *   <li>Applies Mining Fatigue I to the target</li>
     *   <li>Channels for up to 5 seconds (100 ticks)</li>
     *   <li>Cancelled if player shifts, target dies/leaves range, or player detransforms</li>
     *   <li>3-second cooldown after laser ends</li>
     * </ul>
     * Call from TransformationManager's tick handler.
     */
    public static void tickGuardianLaser(ServerPlayer sp) {
        LaserState state = GUARDIAN_LASERS.get(sp.getUUID());
        if (state == null) return;

        // Cancel if player is shifting
        if (sp.isShiftKeyDown()) {
            endGuardianLaser(sp);
            return;
        }

        // Cancel if no longer transformed as guardian
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null || !GUARDIAN_TYPES.contains(data.getEntityType())) {
            endGuardianLaser(sp);
            return;
        }

        // Resolve target entity
        net.minecraft.world.entity.Entity rawTarget = sp.level().getEntity(state.targetId());
        if (!(rawTarget instanceof LivingEntity target) || !target.isAlive()
                || target.distanceTo(sp) > GUARDIAN_LASER_RANGE + 5) {
            endGuardianLaser(sp);
            return;
        }

        ServerLevel level = sp.serverLevel();
        long now = level.getGameTime();
        int elapsed = (int)(now - state.startTick());

        // Max channel duration reached (5 seconds)
        if (elapsed >= GUARDIAN_LASER_MAX_TICKS) {
            endGuardianLaser(sp);
            return;
        }

        // --- Spawn bubble particles between player and target each tick ---
        Vec3 start = sp.getEyePosition();
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        Vec3 step = dir.normalize();

        for (double d = 0; d < length; d += 0.5) {
            Vec3 pos = start.add(step.scale(d));
            level.sendParticles(ParticleTypes.BUBBLE,
                    pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
        }

        // --- Deal the laser's damage (at the player's power) every 10 ticks ---
        if (elapsed > 0 && elapsed % GUARDIAN_LASER_DAMAGE_INTERVAL == 0) {
            target.hurt(level.damageSources().magic(), LevelPower.scaled(sp, GUARDIAN_LASER_TICK_DAMAGE));
        }

        // --- Apply Mining Fatigue I to target each tick (refresh duration) ---
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 0, false, true, true));

        // --- Ambient sound every 20 ticks ---
        if (elapsed % 20 == 0) {
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    /** Ends the guardian laser and starts the 3-second cooldown. */
    private static void endGuardianLaser(ServerPlayer sp) {
        GUARDIAN_LASERS.remove(sp.getUUID());
        long now = sp.level().getGameTime();
        GUARDIAN_LASER_COOLDOWNS.put(sp.getUUID(), now + GUARDIAN_LASER_COOLDOWN_TICKS);

        // End sound
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 1.0F, 1.5F);
    }

    // -------------------------------------------------------------------------
    // Snow Golem: throw snowball (right-click, 0.5s cooldown)
    // -------------------------------------------------------------------------

    private static void trySnowballThrow(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        if (data.getEntityType() != EntityType.SNOW_GOLEM) return;

        long now = sp.level().getGameTime();
        Long cooldownEnd = SNOWBALL_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        SNOWBALL_COOLDOWNS.put(sp.getUUID(), now + SNOWBALL_COOLDOWN_TICKS);

        Snowball snowball = new Snowball(sp.level(), sp);
        snowball.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 1.5F, 1.0F);
        sp.level().addFreshEntity(snowball);

        // Track the snowball so we can deal extra damage on impact
        ACTIVE_SNOWBALLS.add(new SnowballEntry(snowball, sp.getUUID()));

        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * Tick snowball impact checks. Call from TransformationManager's tick handler.
     * When a snowball is removed (hit something), deal damage to nearby entities.
     */
    public static void tickSnowballDamage(ServerLevel level) {
        ACTIVE_SNOWBALLS.removeIf(entry -> {
            Snowball sb = entry.snowball();
            if (sb.isRemoved()) {
                // Snowball hit something -- find what it hit by checking nearby entities
                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(sb.getX() - 1, sb.getY() - 1, sb.getZ() - 1,
                                sb.getX() + 1, sb.getY() + 1, sb.getZ() + 1),
                        e -> e.isAlive() && !e.getUUID().equals(entry.ownerUUID()));
                if (!nearby.isEmpty()) {
                    LivingEntity hit = nearby.get(0);
                    ServerPlayer thrower = level.getServer().getPlayerList().getPlayer(entry.ownerUUID());
                    DamageSource src = thrower != null
                            ? level.damageSources().playerAttack(thrower)
                            : level.damageSources().generic();
                    hit.hurt(src, thrower != null ? LevelPower.scaled(thrower, SNOWBALL_DAMAGE) : SNOWBALL_DAMAGE);
                }
                return true; // remove from list
            }
            // Safety net: remove if alive too long
            if (sb.tickCount > 100) return true;
            return false;
        });
    }

    // -------------------------------------------------------------------------
    // Iron Golem: ground slam (right-click, 5s cooldown)
    // -------------------------------------------------------------------------

    private static void tryIronGolemSlam(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        if (data.getEntityType() != EntityType.IRON_GOLEM) return;

        long now = sp.level().getGameTime();
        Long cooldownEnd = IRON_GOLEM_SLAM_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        IRON_GOLEM_SLAM_COOLDOWNS.put(sp.getUUID(), now + IRON_GOLEM_SLAM_COOLDOWN_TICKS);

        ServerLevel serverLevel = sp.serverLevel();

        // Find all mobs within radius
        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(IRON_GOLEM_SLAM_RADIUS),
                e -> e != sp && e.isAlive());

        for (LivingEntity target : nearby) {
            // Deal damage
            target.hurt(serverLevel.damageSources().playerAttack(sp), LevelPower.scaled(sp, IRON_GOLEM_SLAM_DAMAGE));

            // Launch upward
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x, IRON_GOLEM_SLAM_LAUNCH_Y, motion.z);
            target.hasImpulse = true;
            if (target instanceof ServerPlayer targetPlayer) {
                targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
            }
        }

        // Visual/audio feedback
        serverLevel.sendParticles(ParticleTypes.CRIT,
                sp.getX(), sp.getY() + 0.5, sp.getZ(),
                30, IRON_GOLEM_SLAM_RADIUS * 0.5, 0.2, IRON_GOLEM_SLAM_RADIUS * 0.5, 0.1);
        serverLevel.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.5F, 0.8F);
    }

    // -------------------------------------------------------------------------
    // Evoker: fangs line (right-click, 3s cd) / summon vex (shift+right, 5s cd)
    // -------------------------------------------------------------------------

    private static void tryEvokerAbility(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        if (data.getEntityType() != EntityType.EVOKER) return;

        long now = sp.level().getGameTime();

        if (sp.isShiftKeyDown()) {
            // Shift+right-click: summon 3 Vex
            Long cooldownEnd = EVOKER_VEX_COOLDOWNS.get(sp.getUUID());
            if (cooldownEnd != null && now < cooldownEnd) return;

            EVOKER_VEX_COOLDOWNS.put(sp.getUUID(), now + EVOKER_VEX_COOLDOWN_TICKS);

            ServerLevel serverLevel = sp.serverLevel();
            long despawnTick = now + 200L; // die after 200 ticks (10 seconds)

            for (int i = 0; i < 3; i++) {
                Vex vex = EntityType.VEX.create(serverLevel);
                if (vex == null) continue;

                // Position slightly spread around the player
                double offsetX = (sp.getRandom().nextDouble() - 0.5) * 2.0;
                double offsetZ = (sp.getRandom().nextDouble() - 0.5) * 2.0;
                vex.moveTo(sp.getX() + offsetX, sp.getY() + 1.0, sp.getZ() + offsetZ,
                        sp.getYRot(), 0.0F);

                // Make sure NoAI is cleared so they fight
                vex.setNoAi(false);

                // Make the vex bound to the summoner's position
                vex.setBoundOrigin(sp.blockPosition());

                // Vex.setOwner requires a Mob, not a Player.
                // Instead, store the player UUID in the custom name for identification
                // and let natural targeting handle the rest.
                vex.setCustomName(net.minecraft.network.chat.Component.literal(
                        "jttw_vex:" + sp.getUUID()));
                vex.setCustomNameVisible(false);

                serverLevel.addFreshEntity(vex);
                ACTIVE_VEX.add(new VexDespawnEntry(vex, despawnTick));
            }

            serverLevel.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            // Right-click: spawn 5 evoker fangs in a line
            Long cooldownEnd = EVOKER_FANGS_COOLDOWNS.get(sp.getUUID());
            if (cooldownEnd != null && now < cooldownEnd) return;

            EVOKER_FANGS_COOLDOWNS.put(sp.getUUID(), now + EVOKER_FANGS_COOLDOWN_TICKS);

            ServerLevel serverLevel = sp.serverLevel();
            Vec3 look = sp.getLookAngle();
            // Flatten look to horizontal
            Vec3 horizontal = new Vec3(look.x, 0, look.z).normalize();

            for (int i = 1; i <= 5; i++) {
                double distance = i * 1.25; // space fangs 1.25 blocks apart
                double fx = sp.getX() + horizontal.x * distance;
                double fz = sp.getZ() + horizontal.z * distance;
                // Find ground level at this position
                BlockPos fangPos = BlockPos.containing(fx, sp.getY(), fz);
                // Search down to find ground
                for (int y = 0; y < 3; y++) {
                    BlockPos checkDown = fangPos.below(y);
                    if (serverLevel.getBlockState(checkDown).isSolidRender(serverLevel, checkDown)) {
                        fangPos = checkDown.above();
                        break;
                    }
                }

                net.minecraft.world.entity.projectile.EvokerFangs fangs =
                        new net.minecraft.world.entity.projectile.EvokerFangs(
                                sp.level(), fx, fangPos.getY(), fz,
                                (float) Math.atan2(horizontal.z, horizontal.x), // yRot
                                i * 2, // warmup delay (staggered)
                                sp);
                serverLevel.addFreshEntity(fangs);
            }

            serverLevel.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /**
     * Tick vex despawn timer. Call from TransformationManager's tick handler.
     */
    public static void tickVexDespawn(ServerLevel level) {
        long now = level.getGameTime();
        ACTIVE_VEX.removeIf(entry -> {
            if (entry.vex().isRemoved()) return true;
            if (now >= entry.despawnTick()) {
                entry.vex().discard();
                return true;
            }
            return false;
        });
    }

    // -------------------------------------------------------------------------
    // Melee attack effects (LivingIncomingDamageEvent)
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        // --- Melee attack effects (attacker must be a transformed player) ---
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (attacker.level().isClientSide) return;

        // Only apply for direct melee (not projectile damage)
        if (event.getSource().getDirectEntity() != attacker) return;

        TransformationData data = TransformationManager.getTransformation(attacker);
        if (data == null) return;

        EntityType<?> morphType = data.getEntityType();
        LivingEntity target = event.getEntity();

        // Bee: Slowness I (3s) + Poison I (3s)
        if (BEE_TYPES.contains(morphType)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0)); // 3s, level I
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0)); // 3s, level I
            return;
        }

        // Spider/Cave Spider: Poison II (4s) + Slowness I (2s)
        if (SPIDER_TYPES.contains(morphType)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1)); // 4s, level II
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0)); // 2s, level I
            return;
        }

        // Zombie variants: Slowness I (3s)
        if (ZOMBIE_TYPES.contains(morphType)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0)); // 3s, level I
            return;
        }

        // Wither: Wither II (5s)
        if (morphType == EntityType.WITHER) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1)); // 5s, level II
            return;
        }

        // Wither Skeleton: Wither I (10s) -- matches vanilla
        if (morphType == EntityType.WITHER_SKELETON) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0)); // 10s, level I
            return;
        }

        // Ravager, and the Great White Bull: 3x knockback on melee
        if (morphType == EntityType.RAVAGER || morphType == com.jttw.engine.registry.ModEntities.GREAT_WHITE_BULL.get()) {
            double dx = target.getX() - attacker.getX();
            double dz = target.getZ() - attacker.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0) {
                // Default knockback is roughly 0.4 strength; 3x = push with ~1.2 total
                double knockStrength = 1.2;
                target.push(dx / dist * knockStrength, 0.3, dz / dist * knockStrength);
                if (target instanceof ServerPlayer targetPlayer) {
                    targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
                }
            }
            return;
        }

        // Iron Golem: melee launches target up (Y += 0.5)
        if (morphType == EntityType.IRON_GOLEM) {
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x, motion.y + IRON_GOLEM_MELEE_LAUNCH_Y, motion.z);
            target.hasImpulse = true;
            if (target instanceof ServerPlayer targetPlayer) {
                targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Fall damage immunity for Slime/Magma Cube
    // -------------------------------------------------------------------------

    /**
     * Returns true if the player's current transform should cancel fall damage entirely.
     * Called from TransformationManager.onLivingFall.
     */
    public static boolean shouldCancelFall(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return false;
        return SLIME_TYPES.contains(data.getEntityType());
    }

    // -------------------------------------------------------------------------
    // Horse jump boost (LivingJumpEvent)
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;

        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;

        EntityType<?> morphType = data.getEntityType();
        if (!HORSE_TYPES.contains(morphType)) return;

        // Don't double-boost if the player can fly (same pattern as JumpTraits)
        if (data.canFly()) return;

        // Apply boosted jump
        var dm = sp.getDeltaMovement();
        sp.setDeltaMovement(dm.x, HORSE_JUMP_VELOCITY, dm.z);
        sp.hasImpulse = true;
        sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        sp.fallDistance = 0.0F;
    }

    // -------------------------------------------------------------------------
    // Horse fall damage reduction + step assist tick
    // -------------------------------------------------------------------------

    /**
     * Called from TransformationManager.onLivingFall to reduce fall distance for horse morphs.
     */
    public static float adjustHorseFallDistance(ServerPlayer sp, float fallDistance) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return fallDistance;
        if (!HORSE_TYPES.contains(data.getEntityType())) return fallDistance;
        return Math.max(0.0F, fallDistance - HORSE_SAFE_FALL);
    }

    /**
     * Applies or removes the step height increase for horse morphs.
     * Called from TransformationManager when traits are applied/cleared.
     */
    /** The step height the player had before a horse form raised it; restored on leaving the form. */
    private static final String NBT_STEP_BEFORE_HORSE = "jttw_step_before_horse";

    public static void applyHorseStepAssist(ServerPlayer sp, TransformationData data) {
        var pd = sp.getPersistentData();
        // the step height's base value (bonuses, Byakko's among them, are modifiers on top)
        var step = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        if (step == null) return;
        if (data != null && HORSE_TYPES.contains(data.getEntityType())) {
            if (!pd.contains(NBT_STEP_BEFORE_HORSE)) pd.putFloat(NBT_STEP_BEFORE_HORSE, (float) step.getBaseValue());
            step.setBaseValue(1.5F + 0.6F); // vanilla 0.6 + 1.5 bonus
        } else if (pd.contains(NBT_STEP_BEFORE_HORSE)) {
            // Put back exactly what was there
            step.setBaseValue(pd.getFloat(NBT_STEP_BEFORE_HORSE));
            pd.remove(NBT_STEP_BEFORE_HORSE);
        }
    }

    // -------------------------------------------------------------------------
    // Enderman teleport (right-click: tp toward cursor, up to 20 blocks)
    // -------------------------------------------------------------------------

    private static void tryEnderTeleport(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;

        EntityType<?> morphType = data.getEntityType();
        if (morphType != EntityType.ENDERMAN) return;

        long now = sp.level().getGameTime();
        Long cooldownEnd = ENDER_TP_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        // Raycast to find where the player is looking (up to 20 blocks)
        Vec3 eyePos = sp.getEyePosition();
        Vec3 lookVec = sp.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(ENDER_TP_RANGE));

        net.minecraft.world.level.ClipContext clipCtx = new net.minecraft.world.level.ClipContext(
                eyePos, endPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, sp);
        net.minecraft.world.phys.BlockHitResult hitResult = sp.level().clip(clipCtx);

        Vec3 targetPos;
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            // Teleport to just before the block, not inside it
            targetPos = hitResult.getLocation().subtract(lookVec.scale(0.5));
        } else {
            // No block hit -- teleport the full 20 blocks
            targetPos = endPos;
        }

        // Find safe Y (don't teleport inside blocks)
        net.minecraft.core.BlockPos targetBlock = net.minecraft.core.BlockPos.containing(targetPos);
        ServerLevel level = sp.serverLevel();

        // Move up if inside solid block
        for (int i = 0; i < 5; i++) {
            if (!level.getBlockState(targetBlock).isSolidRender(level, targetBlock)
                    && !level.getBlockState(targetBlock.above()).isSolidRender(level, targetBlock.above())) {
                break;
            }
            targetBlock = targetBlock.above();
        }

        // Enderman particles at origin
        level.sendParticles(ParticleTypes.PORTAL,
                sp.getX(), sp.getY() + 1, sp.getZ(),
                32, 0.5, 1.0, 0.5, 0.1);

        // Teleport
        sp.teleportTo(targetBlock.getX() + 0.5, targetBlock.getY(), targetBlock.getZ() + 0.5);

        // Enderman particles at destination
        level.sendParticles(ParticleTypes.PORTAL,
                sp.getX(), sp.getY() + 1, sp.getZ(),
                32, 0.5, 1.0, 0.5, 0.1);

        // Sound
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

        ENDER_TP_COOLDOWNS.put(sp.getUUID(), now + ENDER_TP_COOLDOWN_TICKS);
    }

    // -------------------------------------------------------------------------
    // Wither skull (right-click: shoot wither skull + wither on impact)
    // -------------------------------------------------------------------------

    private static void tryWitherSkull(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        if (data.getEntityType() != EntityType.WITHER) return;

        long now = sp.level().getGameTime();
        Long cooldownEnd = WITHER_SKULL_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        Vec3 look = sp.getLookAngle();
        net.minecraft.world.entity.projectile.WitherSkull skull =
                new net.minecraft.world.entity.projectile.WitherSkull(
                        sp.level(), sp, look);
        skull.setPos(sp.getX() + look.x * 1.5,
                sp.getEyeY() + look.y * 1.5,
                sp.getZ() + look.z * 1.5);
        skull.setOwner(sp);
        sp.level().addFreshEntity(skull);

        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);

        WITHER_SKULL_COOLDOWNS.put(sp.getUUID(), now + WITHER_SKULL_COOLDOWN_TICKS);
    }

    // -------------------------------------------------------------------------
    // Creeper self-destruct (right-click: start charge, explode after 1.5s)
    // -------------------------------------------------------------------------

    private static void tryCreeperCharge(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        if (data.getEntityType() != EntityType.CREEPER) return;

        // Shift+right-click: toggle charged mode
        if (sp.isShiftKeyDown()) {
            UUID id = sp.getUUID();
            if (CHARGED_CREEPERS.contains(id)) {
                CHARGED_CREEPERS.remove(id);
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a77Creeper mode: \u00a7fNormal"));
            } else {
                CHARGED_CREEPERS.add(id);
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7b\u26a1 Creeper mode: \u00a7bCharged"));
                // Lightning sound for the charge-up
                sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.5F);
            }
            return;
        }

        // Right-click: start detonation if not already charging
        long now = sp.level().getGameTime();
        Long explosionCd = CREEPER_EXPLOSION_COOLDOWNS.get(sp.getUUID());
        if (explosionCd != null && now < explosionCd) return;

        if (!CREEPER_CHARGE.containsKey(sp.getUUID())) {
            CREEPER_CHARGE.put(sp.getUUID(), CREEPER_CHARGE_TICKS);
            boolean charged = CHARGED_CREEPERS.contains(sp.getUUID());
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    charged ? "\u00a7b\u00a7l\u26a1 SSSSSSSSS..." : "\u00a7c\u00a7lSSSSSSSS..."));
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.CREEPER_PRIMED, SoundSource.PLAYERS, 1.0F,
                    charged ? 0.5F : 1.0F); // deeper pitch for charged
        }
    }

    /**
     * Tick creeper charge countdown. Call from TransformationManager's tick handler.
     */
    public static void tickCreeperCharge(ServerPlayer sp) {
        Integer remaining = CREEPER_CHARGE.get(sp.getUUID());
        if (remaining == null) return;

        // Check still a creeper
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null || data.getEntityType() != EntityType.CREEPER) {
            CREEPER_CHARGE.remove(sp.getUUID());
            return;
        }

        remaining--;
        if (remaining <= 0) {
            // Explode -- player is immune to their own explosion
            CREEPER_CHARGE.remove(sp.getUUID());
            boolean charged = CHARGED_CREEPERS.contains(sp.getUUID());
            float power = charged ? CHARGED_EXPLOSION_POWER : CREEPER_EXPLOSION_POWER;
            double effectRadius = charged ? 8.0 : 5.0;
            ServerLevel level = sp.serverLevel();

            // Store HP, do explosion, restore HP (player doesn't die)
            float hpBefore = sp.getHealth();
            float absorptionBefore = sp.getAbsorptionAmount();
            // Breaks blocks like a creeper's blast (the mobGriefing rule applies); the player is
            // the explosion's source, which an explosion never damages or pushes
            level.explode(sp, sp.getX(), sp.getY(), sp.getZ(),
                    power, Level.ExplosionInteraction.MOB);
            sp.setHealth(hpBefore);
            sp.setAbsorptionAmount(absorptionBefore);
            sp.clearFire();

            // Remove slowness applied during countdown
            sp.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

            // Set 30-second cooldown
            CREEPER_EXPLOSION_COOLDOWNS.put(sp.getUUID(), level.getGameTime() + CREEPER_EXPLOSION_COOLDOWN_TICKS);

            // Apply effects to everything nearby except the player
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(effectRadius), e -> e != sp && e.isAlive());
            for (LivingEntity e : nearby) {
                if (charged) {
                    // Charged: Wither III for 8s + Slowness II for 5s
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 2));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                } else {
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1)); // 5s Wither II
                }
            }
        } else {
            CREEPER_CHARGE.put(sp.getUUID(), remaining);
            // Apply Slowness 255 during countdown (immobilize the player)
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 255, false, false, false));
            // Hissing particles
            if (remaining % 5 == 0) {
                ((ServerLevel) sp.level()).sendParticles(ParticleTypes.SMOKE,
                        sp.getX(), sp.getY() + 1, sp.getZ(),
                        10, 0.3, 0.5, 0.3, 0.02);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Spider wall climbing (tick handler)
    // -------------------------------------------------------------------------

    /**
     * Allows spider-transformed players to climb walls like spiders.
     * Call from TransformationManager's tick handler.
     */
    public static void tickSpiderClimb(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        EntityType<?> type = data.getEntityType();
        if (type != EntityType.SPIDER && type != EntityType.CAVE_SPIDER) return;

        // Check if player is against a wall (horizontally colliding)
        if (sp.horizontalCollision) {
            Vec3 motion = sp.getDeltaMovement();
            double climbY;
            if (sp.isShiftKeyDown()) {
                // Shift = descend slowly
                climbY = -0.2;
            } else if (sp.zza > 0) {
                // Forward = climb up
                climbY = 0.2;
            } else {
                // No input = cling to wall (no falling)
                climbY = 0.0;
            }
            sp.setDeltaMovement(motion.x, climbY, motion.z);
            sp.fallDistance = 0;
        }
    }

    // -------------------------------------------------------------------------
    // Snow Golem snow trail (tick handler)
    // -------------------------------------------------------------------------

    /**
     * Places snow at the Snow Golem player's feet every tick, unless the biome
     * is too hot (temperature > 1.0). Call from TransformationManager's tick handler.
     */
    public static void tickSnowGolemTrail(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        if (data.getEntityType() != EntityType.SNOW_GOLEM) return;

        ServerLevel level = sp.serverLevel();
        BlockPos feetPos = sp.blockPosition();

        // Don't place snow in hot biomes (temperature > 1.0)
        float temperature = level.getBiome(feetPos).value().getBaseTemperature();
        if (temperature > 1.0F) return;

        // Place snow if feet position is air and block below is solid
        BlockState feetState = level.getBlockState(feetPos);
        BlockPos belowPos = feetPos.below();
        BlockState belowState = level.getBlockState(belowPos);

        if (feetState.isAir() && belowState.isSolidRender(level, belowPos)) {
            level.setBlockAndUpdate(feetPos, Blocks.SNOW.defaultBlockState());
        }
    }

    // -------------------------------------------------------------------------
    // Silverfish stone phasing (tick handler)
    // -------------------------------------------------------------------------

    /**
     * When transformed as Silverfish and horizontally colliding, phases through
     * stone-type blocks (up to 3 blocks thick). 1-second cooldown.
     * Call from TransformationManager's tick handler.
     */
    public static void tickSilverfishPhase(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        if (data.getEntityType() != EntityType.SILVERFISH) return;

        if (!sp.horizontalCollision) return;

        // Check cooldown
        long now = sp.level().getGameTime();
        Long cooldownEnd = SILVERFISH_PHASE_COOLDOWNS.get(sp.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) return;

        // Determine the horizontal direction the player is walking into (look direction flattened to Y=0)
        Vec3 look = sp.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1.0e-9) return;
        horizontal = horizontal.normalize();

        // Find the block the player is colliding with (one block in the look direction)
        BlockPos startPos = sp.blockPosition().offset(
                (int) Math.round(horizontal.x),
                0,
                (int) Math.round(horizontal.z)
        );

        ServerLevel level = sp.serverLevel();
        BlockState startState = level.getBlockState(startPos);

        // Check if the starting block is a phaseable type
        if (!SILVERFISH_PHASEABLE.contains(startState.getBlock())) return;

        // Scan up to 3 blocks in the movement direction to find air on the other side
        BlockPos checkPos = startPos;
        boolean foundExit = false;
        for (int i = 1; i <= 3; i++) {
            checkPos = startPos.offset(
                    (int) Math.round(horizontal.x * i),
                    0,
                    (int) Math.round(horizontal.z * i)
            );
            BlockState checkState = level.getBlockState(checkPos);
            if (checkState.isAir() || !checkState.isSolidRender(level, checkPos)) {
                // Also need head room
                BlockState aboveState = level.getBlockState(checkPos.above());
                if (aboveState.isAir() || !aboveState.isSolidRender(level, checkPos.above())) {
                    foundExit = true;
                    break;
                }
            }
        }

        if (!foundExit) return;

        // Teleport the player to the exit position
        sp.teleportTo(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);

        // Play silverfish sounds
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.SILVERFISH_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);

        // Set cooldown
        SILVERFISH_PHASE_COOLDOWNS.put(sp.getUUID(), now + SILVERFISH_PHASE_COOLDOWN_TICKS);
    }

    // -------------------------------------------------------------------------
    // Villager / Iron Golem trade discounts (tick handler)
    // -------------------------------------------------------------------------

    /**
     * When transformed as Villager or Iron Golem and within 8 blocks of a villager,
     * applies Hero of the Village I effect. Checked every 40 ticks.
     * Call from TransformationManager's tick handler.
     */
    public static void tickVillagerDiscount(ServerPlayer sp) {
        // Only check every 40 ticks
        if (sp.tickCount % 40 != 0) return;

        TransformationData data = TransformationManager.getTransformation(sp);

        // If not transformed as villager/iron golem, remove the effect if they have it
        if (data == null || !VILLAGER_DISCOUNT_TYPES.contains(data.getEntityType())) {
            if (sp.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
                sp.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
            }
            return;
        }

        // Check if any villagers are within 8 blocks
        AABB searchBox = sp.getBoundingBox().inflate(8.0);
        List<Villager> nearbyVillagers = sp.level().getEntitiesOfClass(
                Villager.class, searchBox, Villager::isAlive);

        if (!nearbyVillagers.isEmpty()) {
            // Apply Hero of the Village I (ambient, no particles)
            sp.addEffect(new MobEffectInstance(
                    MobEffects.HERO_OF_THE_VILLAGE, 220, 0, true, false, true));
        } else {
            // No villagers nearby -- remove the effect
            if (sp.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
                sp.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Witch passive: remove Poison effect each tick
    // -------------------------------------------------------------------------

    /**
     * Removes poison effect from witch-transformed players each tick.
     * Call from TransformationManager's tick handler.
     */
    public static void tickWitchPoisonImmunity(ServerPlayer sp) {
        TransformationData data = TransformationManager.getTransformation(sp);
        if (data == null) return;
        if (data.getEntityType() != EntityType.WITCH) return;

        if (sp.hasEffect(MobEffects.POISON)) {
            sp.removeEffect(MobEffects.POISON);
        }
    }

    // -------------------------------------------------------------------------
    // Cleanup on logout
    // -------------------------------------------------------------------------

    /**
     * Removes stale cooldown entries when players log out.
     */
    public static void onPlayerLogout(UUID playerId) {
        FIREBALL_COOLDOWNS.remove(playerId);
        SONIC_BOOM_COOLDOWNS.remove(playerId);
        ARROW_COOLDOWNS.remove(playerId);
        ENDER_TP_COOLDOWNS.remove(playerId);
        WITHER_SKULL_COOLDOWNS.remove(playerId);
        CREEPER_CHARGE.remove(playerId);
        CHARGED_CREEPERS.remove(playerId);
        CREEPER_EXPLOSION_COOLDOWNS.remove(playerId);
        SILVERFISH_PHASE_COOLDOWNS.remove(playerId);
        WITCH_POTION_COOLDOWNS.remove(playerId);
        LLAMA_SPIT_COOLDOWNS.remove(playerId);
        GUARDIAN_LASER_COOLDOWNS.remove(playerId);
        GUARDIAN_LASERS.remove(playerId);
        SNOWBALL_COOLDOWNS.remove(playerId);
        IRON_GOLEM_SLAM_COOLDOWNS.remove(playerId);
        EVOKER_FANGS_COOLDOWNS.remove(playerId);
        EVOKER_VEX_COOLDOWNS.remove(playerId);
        ACTIVE_SNOWBALLS.removeIf(e -> e.ownerUUID().equals(playerId));
    }
}
