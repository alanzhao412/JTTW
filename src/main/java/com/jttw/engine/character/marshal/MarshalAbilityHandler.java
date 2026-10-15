package com.jttw.engine.character.marshal;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.item.NineToothRakeItem;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncStomachPacket;
import com.jttw.engine.network.s2c.TruffleScentPacket;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Marshal of the Heavenly Canopy's abilities.
 *
 * <ul>
 *   <li><b>V, Belly Slam:</b> leaps forward (or, in the air, drops straight down) and quakes the
 *       ground where he lands. The fuller his stomachs, the wider and harder the quake.</li>
 *   <li><b>B, Truffle Snout:</b> a sniff that shows him, through walls, every creature near
 *       (II: ores and chests too; III: further, for longer).</li>
 *   <li><b>Bottomless Stomach</b> (passive): a second stomach as large as the first, filled by
 *       what he eats past full, emptied before the first goes hungry. He can always eat; what
 *       fits in neither stomach heals him.</li>
 *   <li><b>Admiral of the Heavenly River</b> (passive): breathes underwater and swims fast;
 *       II sees clearly underwater; III his fleet, the players near him, swims and breathes
 *       as he does.</li>
 * </ul>
 * With the Nine-Tooth Rake in hand, each full swing of his bursts in an explosion that harms
 * foes around the one struck and never breaks a block.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class MarshalAbilityHandler {

    private MarshalAbilityHandler() {}

    // ── keys and numbers ───────────────────────────────────────────

    private static final String KEY_SLAM_READY_AT = "jttw_mc_slam_ready_at";
    private static final String KEY_SNOUT_READY_AT = "jttw_mc_snout_ready_at";
    /** Game time of the last leap; the slam is live while {@link #KEY_SLAMMING} is set. */
    public static final String KEY_SLAM_START = "jttw_mc_slam_start";
    public static final String KEY_SLAMMING = "jttw_mc_slamming";
    /** Food in the second stomach, 0 to {@link #RESERVE_MAX}. */
    public static final String KEY_RESERVE = "jttw_mc_reserve";
    private static final String KEY_RESERVE_SENT = "jttw_mc_reserve_sent";
    private static final String KEY_RAKE_NEXT = "jttw_mc_rake_next";

    public static final int SLAM_COOLDOWN = 100;
    public static final float SLAM_QI = 10f;
    /** A leap that has not landed by now ends without a quake. */
    public static final int SLAM_AIR_TICKS = 60;
    public static final int SNOUT_COOLDOWN = 600;
    public static final float SNOUT_QI = 10f;
    public static final int SCENT_CAP = 1024;
    public static final int RESERVE_MAX = 20;
    public static final int FOOD_MAX = 20;
    public static final double FLEET_RANGE = 16;
    public static final double RAKE_BLAST_RADIUS = 3;
    public static final float RAKE_BLAST_SHARE = 0.5f;

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
        return MarshalSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(p), branch);
    }

    private static boolean isFoe(LivingEntity e, ServerPlayer p) {
        return e != p && e.isAlive() && !e.isAlliedTo(p);
    }

    // ── V: Belly Slam ──────────────────────────────────────────────

    /** How full both stomachs are, 0 to 1. */
    public static float fullness(ServerPlayer p) {
        return (p.getFoodData().getFoodLevel() + reserve(p)) / (float) (FOOD_MAX + RESERVE_MAX);
    }

    /** The quake's reach: 3 blocks hungry, 6 full; III adds 2. */
    public static double slamRadius(float fullness, int tier) {
        return 3 + 3 * fullness + (tier >= 3 ? 2 : 0);
    }

    /** The quake's damage: ATK x (0.8 hungry to 2.0 full) x 1.0 / 1.25 / 1.5 by tier. */
    public static float slamDamage(float atk, float fullness, int tier) {
        return atk * (0.8f + 1.2f * fullness) * (1f + 0.25f * (tier - 1));
    }

    /** Press V: leap forward, or in the air drop like a stone. The quake comes on landing. */
    public static void bellySlam(ServerPlayer p) {
        if (!MarshalCharacter.is(p) || tier(p, "mc_slam") < 1) return;
        if (onCooldown(p, KEY_SLAM_READY_AT) || !spendQi(p, SLAM_QI)) return;
        Vec3 dm = p.getDeltaMovement();
        if (p.onGround()) {
            Vec3 look = p.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            flat = flat.lengthSqr() < 1e-4 ? Vec3.ZERO : flat.normalize();
            p.setDeltaMovement(flat.x * 0.9, 0.9, flat.z * 0.9);
        } else {
            p.setDeltaMovement(dm.x * 0.3, -2.0, dm.z * 0.3);
        }
        p.hurtMarked = true;
        CompoundTag pd = p.getPersistentData();
        pd.putLong(KEY_SLAM_START, p.level().getGameTime());
        pd.putBoolean(KEY_SLAMMING, true);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PIG_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.6f);
        startCooldown(p, KEY_SLAM_READY_AT, "mc_slam", SLAM_COOLDOWN);
    }

    /** The landing: quakes the ground around him. Returns how many it struck. */
    public static int slamImpact(ServerPlayer p) {
        CompoundTag pd = p.getPersistentData();
        pd.remove(KEY_SLAMMING);
        p.fallDistance = 0;
        ServerLevel level = p.serverLevel();
        int tier = Math.max(1, tier(p, "mc_slam"));
        float full = fullness(p);
        double radius = slamRadius(full, tier);
        float damage = slamDamage((float) p.getAttributeValue(Attributes.ATTACK_DAMAGE), full, tier);
        double push = 0.8 + 0.8 * full;

        int struck = 0;
        AABB box = p.getBoundingBox().inflate(radius, 2, radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(e, p))) {
            double dx = e.getX() - p.getX(), dz = e.getZ() - p.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius) continue;
            // Credited to him, but not a swing: the rake does not burst on quake damage
            e.hurt(p.damageSources().mobAttack(p), damage);
            Vec3 out = dist < 1e-3 ? Vec3.ZERO : new Vec3(dx / dist, 0, dz / dist);
            e.push(out.x * push, 0.5 + 0.3 * full, out.z * push);
            e.hurtMarked = true;
            if (tier >= 2) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            struck++;
        }

        // The ground cracks in rings outward; nothing is broken
        BlockState ground = level.getBlockState(p.blockPosition().below());
        if (ground.isAir()) ground = level.getBlockState(p.blockPosition());
        BlockParticleOption crack = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        for (double r = 1; r <= radius; r += 1) {
            int points = (int) (r * 8);
            for (int i = 0; i < points; i++) {
                double a = 2 * Math.PI * i / points;
                level.sendParticles(crack, p.getX() + Math.cos(a) * r, p.getY() + 0.1, p.getZ() + Math.sin(a) * r,
                        2, 0.1, 0.1, 0.1, 0.15);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.3, p.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 20, radius / 3, 0.1, radius / 3, 0.05);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1f, 0.6f);
        return struck;
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(KEY_SLAM_START)) return;
        // A slam lands on its belly, not its legs
        if (p.level().getGameTime() - pd.getLong(KEY_SLAM_START) <= SLAM_AIR_TICKS + 5) event.setCanceled(true);
    }

    // ── B: Truffle Snout ───────────────────────────────────────────

    /** What a sniff turned up: creatures by entity id, and blocks with the colour to draw them. */
    public record Scent(List<Integer> entityIds, List<BlockPos> blocks, List<Integer> colours, int ticks) {}

    public static final int COLOUR_CONTAINER = 0xFFFFD27A;

    private static final Map<TagKey<Block>, Integer> ORE_COLOURS = Map.ofEntries(
            Map.entry(Tags.Blocks.ORES_COAL, 0xFF444444),
            Map.entry(Tags.Blocks.ORES_IRON, 0xFFFF8800),
            Map.entry(Tags.Blocks.ORES_COPPER, 0xFFCC7744),
            Map.entry(Tags.Blocks.ORES_GOLD, 0xFFFFDD00),
            Map.entry(Tags.Blocks.ORES_REDSTONE, 0xFFFF0000),
            Map.entry(Tags.Blocks.ORES_LAPIS, 0xFF2244FF),
            Map.entry(Tags.Blocks.ORES_EMERALD, 0xFF00FF44),
            Map.entry(Tags.Blocks.ORES_DIAMOND, 0xFF00BBFF),
            Map.entry(Tags.Blocks.ORES_QUARTZ, 0xFFEEEEDD),
            Map.entry(Tags.Blocks.ORES_NETHERITE_SCRAP, 0xFF8A4B33));

    /** Colour to draw an ore in, or 0 if the block is no ore. */
    public static int oreColour(BlockState state) {
        if (!state.is(Tags.Blocks.ORES)) return 0;
        for (Map.Entry<TagKey<Block>, Integer> e : ORE_COLOURS.entrySet()) {
            if (state.is(e.getKey())) return e.getValue();
        }
        return 0xFFCCCCCC;
    }

    public static int creatureRange(int tier) { return tier >= 3 ? 32 : 24; }
    public static int treasureRange(int tier) { return tier >= 3 ? 32 : tier >= 2 ? 16 : 0; }
    public static int scentTicks(int tier) { return tier >= 3 ? 400 : 200; }

    /** Press B: sniffs, and shows him what he smelt. Returns it, or null if he could not sniff. */
    public static Scent sniff(ServerPlayer p) {
        int tier = MarshalCharacter.is(p) ? tier(p, "mc_snout") : 0;
        if (tier < 1) return null;
        if (onCooldown(p, KEY_SNOUT_READY_AT) || !spendQi(p, SNOUT_QI)) return null;
        Scent scent = smell(p, tier);
        NetworkHandler.sendToPlayer(p, TruffleScentPacket.of(scent));
        ServerLevel level = p.serverLevel();
        Vec3 nose = p.getEyePosition().add(p.getLookAngle().scale(0.9));
        level.sendParticles(ParticleTypes.SNEEZE, nose.x, nose.y - 0.3, nose.z, 4, 0.1, 0.05, 0.1, 0.01);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PIG_AMBIENT, SoundSource.PLAYERS, 1f, 1.4f);
        p.displayClientMessage(Component.literal("§6Sniffed out " + scent.entityIds().size() + " creatures"
                + (tier >= 2 ? " and " + scent.blocks().size() + " treasures" : "") + "."), true);
        startCooldown(p, KEY_SNOUT_READY_AT, "mc_snout", SNOUT_COOLDOWN);
        return scent;
    }

    /** What is within his nose's reach at this tier. */
    public static Scent smell(ServerPlayer p, int tier) {
        ServerLevel level = p.serverLevel();
        List<Integer> ids = new ArrayList<>();
        int cr = creatureRange(tier);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(cr),
                e -> e != p && e.isAlive())) {
            if (ids.size() >= SCENT_CAP) break;
            ids.add(e.getId());
        }

        List<BlockPos> blocks = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        int tr = treasureRange(tier);
        if (tr > 0) {
            BlockPos c = p.blockPosition();
            int yr = Math.min(tr, 16);
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = -tr; x <= tr && blocks.size() < SCENT_CAP; x++) {
                for (int y = -yr; y <= yr; y++) {
                    for (int z = -tr; z <= tr; z++) {
                        m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                        int colour = oreColour(level.getBlockState(m));
                        if (colour != 0 && blocks.size() < SCENT_CAP) {
                            blocks.add(m.immutable());
                            colours.add(colour);
                        }
                    }
                }
            }
            // Chests, barrels and every other container, from the chunks' block entities
            for (int cx = (c.getX() - tr) >> 4; cx <= (c.getX() + tr) >> 4; cx++) {
                for (int cz = (c.getZ() - tr) >> 4; cz <= (c.getZ() + tr) >> 4; cz++) {
                    if (!level.hasChunk(cx, cz)) continue;
                    LevelChunk chunk = level.getChunk(cx, cz);
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        BlockPos bp = be.getBlockPos();
                        if (!(be instanceof Container) || blocks.size() >= SCENT_CAP) continue;
                        if (Math.abs(bp.getX() - c.getX()) > tr || Math.abs(bp.getZ() - c.getZ()) > tr
                                || Math.abs(bp.getY() - c.getY()) > yr) continue;
                        blocks.add(bp.immutable());
                        colours.add(COLOUR_CONTAINER);
                    }
                }
            }
        }
        return new Scent(ids, blocks, colours, scentTicks(tier));
    }

    // ── Bottomless Stomach ─────────────────────────────────────────

    public static int reserve(Player p) {
        return p.getPersistentData().getInt(KEY_RESERVE);
    }

    public static void setReserve(ServerPlayer p, int amount) {
        p.getPersistentData().putInt(KEY_RESERVE, Math.max(0, Math.min(RESERVE_MAX, amount)));
        syncStomach(p);
    }

    /** True if this player may eat however full he is. Asked on both sides. */
    public static boolean canAlwaysEat(Player p) {
        return MarshalCharacter.is(p) && tier(p, "mc_stomach") >= 1;
    }

    /** Healing per point of food that fits in neither stomach, at level 1: grows with level power. */
    public static float overeatHealing(int tier) {
        return tier >= 3 ? 2f : tier >= 2 ? 1.5f : 1f;
    }

    /**
     * Called as he eats, before the food lands: what overflows the first stomach goes to the
     * second, and what overflows that heals him (III: past full health, as golden hearts).
     */
    public static void overeat(ServerPlayer p, int foodBefore, int nutrition) {
        int tier = MarshalCharacter.is(p) ? tier(p, "mc_stomach") : 0;
        if (tier < 1) return;
        int overflow = foodBefore + nutrition - FOOD_MAX;
        if (overflow <= 0) return;
        int stored = Math.min(RESERVE_MAX - reserve(p), overflow);
        if (stored > 0) setReserve(p, reserve(p) + stored);
        int surplus = overflow - stored;
        if (surplus <= 0) return;
        float heal = LevelPower.scaled(p, surplus * overeatHealing(tier));
        float missing = p.getMaxHealth() - p.getHealth();
        p.heal(heal);
        if (tier >= 3 && heal > missing) {
            // Golden hearts up to 20 at level 1, a cap that grows with the heal
            stomachHearts(p, tier);
            p.setAbsorptionAmount(Math.min(LevelPower.scaled(p, 20f), p.getAbsorptionAmount() + (heal - missing)));
        }
        p.serverLevel().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 3, 0.4, 0.2, 0.4, 0);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1f, 0.8f);
    }

    /**
     * Room for the stomach's golden hearts. Absorption is capped by the max absorption attribute
     * (0 for a player) and clamped whenever it is set, so III keeps its cap there.
     */
    private static void stomachHearts(ServerPlayer p, int tier) {
        PassiveModifiers.add(p, Attributes.MAX_ABSORPTION, "mc_stomach_hearts",
                tier >= 3 ? LevelPower.scaled(p, 20f) : 0);
    }

    /** -1 tells the client he has no second stomach to draw. */
    private static int shownReserve(ServerPlayer p) {
        return canAlwaysEat(p) ? reserve(p) : -1;
    }

    public static void syncStomach(ServerPlayer p) {
        int shown = shownReserve(p);
        p.getPersistentData().putInt(KEY_RESERVE_SENT, shown + 2); // +2 so 0 means "never sent"
        NetworkHandler.sendToPlayer(p, new SyncStomachPacket(shown));
    }

    // ── Admiral of the Heavenly River ──────────────────────────────

    /** Swim speed bonus: +50% / +100%. */
    public static double swimBonus(int tier) {
        return tier >= 2 ? 1.0 : tier >= 1 ? 0.5 : 0;
    }

    /** Water at the eyes; read from the world, not the per-tick fluid flags. */
    public static boolean underwater(Entity e) {
        return e.level().getFluidState(BlockPos.containing(e.getEyePosition())).is(FluidTags.WATER);
    }

    /** Water at the feet or the eyes. */
    public static boolean inWater(Entity e) {
        return underwater(e) || e.level().getFluidState(e.blockPosition()).is(FluidTags.WATER);
    }

    private static void admiral(ServerPlayer p, int tier) {
        PassiveModifiers.set(p, NeoForgeMod.SWIM_SPEED, "mc_admiral_swim", swimBonus(tier),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (tier < 1) return;
        boolean under = underwater(p);
        if (under) p.setAirSupply(p.getMaxAirSupply());
        if (p.tickCount % 20 != 0) return;
        if (tier >= 2 && under) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
        }
        if (tier >= 3) {
            for (ServerPlayer mate : p.server.getPlayerList().getPlayers()) {
                if (mate == p || mate.level() != p.level() || mate.distanceToSqr(p) > FLEET_RANGE * FLEET_RANGE
                        || !inWater(mate)) continue;
                mate.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false));
                mate.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false));
            }
        }
    }

    // ── Nine-Tooth Rake ────────────────────────────────────────────

    /** Bursts around the struck foe; the struck take the blast on top of the blow. */
    public static float rakeBlast(ServerPlayer p, LivingEntity struck) {
        ServerLevel level = p.serverLevel();
        float blast = RAKE_BLAST_SHARE * (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 c = struck.position().add(0, struck.getBbHeight() / 2, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(c, c).inflate(RAKE_BLAST_RADIUS), e -> isFoe(e, p) && e != struck)) {
            Vec3 away = e.position().subtract(c);
            if (away.length() > RAKE_BLAST_RADIUS + e.getBbWidth() / 2) continue;
            e.hurt(p.damageSources().explosion(p, p), blast);
            Vec3 dir = away.lengthSqr() < 1e-4 ? Vec3.ZERO : away.normalize();
            e.push(dir.x * 0.8, 0.3, dir.z * 0.8);
            e.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 12, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.2f);
        return blast;
    }

    @SubscribeEvent
    public static void onHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (event.getSource().getDirectEntity() != p || !event.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        if (!(p.getMainHandItem().getItem() instanceof NineToothRakeItem) || !MarshalCharacter.is(p)) return;
        CompoundTag pd = p.getPersistentData();
        long now = p.level().getGameTime();
        if (now < pd.getLong(KEY_RAKE_NEXT)) return; // one blast per swing
        pd.putLong(KEY_RAKE_NEXT, now + Math.max(1, (long) Math.floor(p.getCurrentItemAttackStrengthDelay())));
        event.setAmount(event.getAmount() + rakeBlast(p, event.getEntity()));
    }

    // ── ticking and lifecycle ──────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        boolean marshal = MarshalCharacter.is(p);
        if (!marshal) {
            if (p.tickCount % 20 == 0) {
                PassiveModifiers.set(p, NeoForgeMod.SWIM_SPEED, "mc_admiral_swim", 0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                stomachHearts(p, 0);
                if (p.getPersistentData().getInt(KEY_RESERVE_SENT) > 1) syncStomach(p);
            }
            return;
        }
        CompoundTag pd = p.getPersistentData();

        // The slam lands
        if (pd.getBoolean(KEY_SLAMMING)) {
            long airborne = p.level().getGameTime() - pd.getLong(KEY_SLAM_START);
            if (airborne > SLAM_AIR_TICKS) pd.remove(KEY_SLAMMING);
            else if (airborne >= 3 && p.onGround()) slamImpact(p);
        }

        // The second stomach feeds the first
        int reserve = reserve(p);
        int food = p.getFoodData().getFoodLevel();
        if (reserve > 0 && food < FOOD_MAX) {
            int moved = Math.min(FOOD_MAX - food, reserve);
            p.getFoodData().setFoodLevel(food + moved);
            setReserve(p, reserve - moved);
        }
        if (p.tickCount % 40 == 0 && pd.getInt(KEY_RESERVE_SENT) != shownReserve(p) + 2) syncStomach(p);
        if (p.tickCount % 20 == 0) stomachHearts(p, tier(p, "mc_stomach"));

        admiral(p, tier(p, "mc_admiral"));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            p.getPersistentData().remove(KEY_SLAMMING);
            syncStomach(p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) syncStomach(p);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // Death empties both stomachs, as it does the first; leaving the End keeps them
        int kept = event.isWasDeath() ? 0 : reserve(event.getOriginal());
        event.getEntity().getPersistentData().putInt(KEY_RESERVE, kept);
        event.getEntity().getPersistentData().remove(KEY_SLAMMING);
    }
}
