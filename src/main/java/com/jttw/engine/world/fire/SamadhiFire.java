package com.jttw.engine.world.fire;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.fire.FireCartEntity;
import com.jttw.engine.entity.fire.FireMonster;
import com.jttw.engine.entity.fire.RedBoyEntity;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Red Boy's True Samadhi Fire (三昧真火), breathed from his mouth and nose (chapter 41). It is not
 * ordinary fire, and the rules for it are its own:
 * <ul>
 *   <li>it <b>burns</b> as a status ({@link ModEffects#SAMADHI_FIRE}), {@value #BURN_SHARE_PERCENT}% of
 *       health a second (half again more at its fiercer, amplifier 1), whatever the burning one is:
 *       the fire-proof burn too, and armour does not soften it;</li>
 *   <li><b>water does not put it out</b>: not a lake, not rain, not milk. Water feeds it, as the Dragon
 *       Kings' rain fed it when Sun Wukong called on them: in water or rain it burns
 *       {@value #WET_PERCENT}% as hard. On the ground ({@link SamadhiFlameBlock}) no water flows into
 *       it nor is poured on it;</li>
 *   <li>only <b>Fire Resistance</b> stands against it, and only halves it (his breath's blow, and the
 *       burning after);</li>
 *   <li>it spares Red Boy's own household: him, his fire carts, the mountains' creatures.</li>
 * </ul>
 */
public final class SamadhiFire {

    private SamadhiFire() {}

    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, JTTWEngine.id("samadhi_breath"));

    public static final int BURN_SHARE_PERCENT = 3, WET_PERCENT = 150;
    /** What it takes each second, as a share of health; what water multiplies it by; what Fire Resistance does. */
    public static final float BURN_SHARE = BURN_SHARE_PERCENT / 100f, WET = WET_PERCENT / 100f, RESISTED = 0.5f;
    /** The effect's colour: the fire's own crimson. */
    public static final int COLOUR = 0xE0301E;

    /** Whether the fire leaves {@code e} alone: Red Boy's own, or a player out of play. */
    public static boolean spared(LivingEntity e) {
        return e instanceof RedBoyEntity || e instanceof FireCartEntity || e instanceof FireMonster
                || (e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    /** What Fire Resistance and water make of a hurt of {@code amount} on {@code e}: halved, then (for the burning) half again in water. */
    public static float against(LivingEntity e, float amount, boolean burning) {
        if (e.hasEffect(MobEffects.FIRE_RESISTANCE)) amount *= RESISTED;
        if (burning && e.isInWaterRainOrBubble()) amount *= WET;
        return amount;
    }

    public static DamageSource source(Level level, @Nullable Entity by) {
        var type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE);
        return by == null ? new DamageSource(type) : new DamageSource(type, by);
    }

    /** Sets {@code e} burning with the Samadhi fire for {@code seconds} (at {@code amplifier}), unless it is spared. */
    public static void ignite(LivingEntity e, int seconds, int amplifier) {
        if (spared(e)) return;
        e.addEffect(new MobEffectInstance(ModEffects.SAMADHI_FIRE, seconds * 20, amplifier, false, true, true));
    }

    /** Whether {@code e} burns with it now. */
    public static boolean burning(LivingEntity e) {
        return e.hasEffect(ModEffects.SAMADHI_FIRE);
    }

    /** What one second of the fire takes from {@code e} at {@code amplifier}, before Fire Resistance and water. */
    public static float burnOf(LivingEntity e, int amplifier) {
        return Math.max(1f, e.getMaxHealth() * BURN_SHARE * (1 + 0.5f * amplifier));
    }

    /** One second of the fire on {@code e}. Returns the hurt it was dealt (0 if none landed). */
    public static float burn(LivingEntity e, int amplifier) {
        if (spared(e) || !e.isAlive()) return 0;
        float amount = against(e, burnOf(e, amplifier), true);
        e.invulnerableTime = 0; // a beat of true fire always lands
        float before = e.getHealth();
        e.hurt(source(e.level(), null), amount);
        if (e.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY(0.5), e.getZ(), 10, e.getBbWidth() * 0.4, e.getBbHeight() * 0.4, e.getBbWidth() * 0.4, 0.02);
            if (e.isInWaterRainOrBubble()) server.sendParticles(ParticleTypes.LAVA, e.getX(), e.getY(0.8), e.getZ(), 3, 0.3, 0.3, 0.3, 0);
        }
        return before - e.getHealth();
    }

    /**
     * A breath of the fire from {@code mouth} along {@code dir}: a cone out to {@code range} blocks,
     * {@code halfAngle} degrees either side. Everyone in it but {@code by}'s own takes {@code blow}
     * (halved under Fire Resistance) and burns for {@code seconds} at {@code amplifier}; flames are
     * left on the ground along it, out to {@code layTo} (where it falls on its mark); a furnace in
     * its line is lit. Returns who it caught.
     */
    public static List<LivingEntity> breathe(LivingEntity by, Vec3 mouth, Vec3 dir, double range, double halfAngle,
                                             float blow, int seconds, int amplifier, double layTo) {
        Level level = by.level();
        Vec3 d = dir.normalize();
        double cos = Math.cos(Math.toRadians(halfAngle));
        List<LivingEntity> caught = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(mouth, mouth).inflate(range + 1),
                e -> e != by && e.isAlive() && !spared(e) && !by.isAlliedTo(e))) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(mouth);
            double len = to.length();
            if (len > range + e.getBbWidth() / 2) continue;
            if (len > 1.0 && to.dot(d) / len < cos) continue;
            e.hurt(source(level, by), against(e, blow, false));
            ignite(e, seconds, amplifier);
            caught.add(e);
        }
        if (level instanceof ServerLevel server) {
            for (double k = 1; k <= range; k += 0.75) {
                Vec3 at = mouth.add(d.scale(k));
                double spread = k * Math.tan(Math.toRadians(halfAngle)) * 0.5;
                server.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 6, spread, spread * 0.6, spread, 0.03);
                if (k % 3 < 0.75) server.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 1, spread, 0.2, spread, 0);
            }
            server.sendParticles(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z, 12, 0.3, 0.3, 0.3, 0.05);
            lay(server, by, mouth, d, Math.min(range, layTo), halfAngle);
            com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity.igniteAlong(level, mouth, d, range, by); // true fire lights the furnace
        }
        return caught;
    }

    /** Leaves the fire on the ground along a breath: on open ground a few blocks under its line, a dozen flames at most. */
    private static void lay(ServerLevel level, Entity by, Vec3 mouth, Vec3 d, double range, double halfAngle) {
        int placed = 0;
        Vec3 side = new Vec3(-d.z, 0, d.x);
        if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        double tan = Math.tan(Math.toRadians(halfAngle));
        for (double k = 2; k <= range && placed < 12; k += 1.5) {
            for (int s = -1; s <= 1 && placed < 12; s++) {
                Vec3 at = mouth.add(d.scale(k)).add(side.scale(s * k * tan * 0.6));
                BlockPos p = BlockPos.containing(at);
                for (int down = 0; down < 5; down++, p = p.below()) {
                    if (place(level, p)) {
                        placed++;
                        break;
                    }
                    if (!level.getBlockState(p).isAir()) break;
                }
            }
        }
    }

    /** Puts a flame of it at {@code p}, if it is open air over solid ground. Returns whether it did. */
    public static boolean place(Level level, BlockPos p) {
        var flame = ModBlocks.SAMADHI_FLAME.get().defaultBlockState();
        if (!level.getBlockState(p).isAir() || !flame.canSurvive(level, p)) return false;
        return level.setBlock(p, flame, net.minecraft.world.level.block.Block.UPDATE_ALL);
    }

    /** The Samadhi fire, burning: a beat each second; no milk cures it, only a totem's death-defiance clears it. */
    public static final class Burning extends net.minecraft.world.effect.MobEffect {
        public Burning() {
            super(MobEffectCategory.HARMFUL, COLOUR);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            if (!e.level().isClientSide) burn(e, amplifier);
            return true;
        }

        @Override
        public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {
            cures.add(EffectCures.PROTECTED_BY_TOTEM);
        }
    }
}
