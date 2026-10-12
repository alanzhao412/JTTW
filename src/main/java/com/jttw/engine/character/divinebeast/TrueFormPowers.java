package com.jttw.engine.character.divinebeast;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What each true form does beyond its strength (TrueForm). The Black Tortoise (II): its serpent
 * strikes the nearest foe within 5 blocks once a second, and every blow to it is halved. The
 * Vermilion Bird (II): fire trails where it flies, and its wingbeats burn all within 4 blocks.
 * (The White Tiger's and the Azure Dragon's are in their own abilities.)
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class TrueFormPowers {

    private TrueFormPowers() {}

    static boolean is(ServerPlayer p, EntityType<?> form) {
        if (!TrueForm.active(p)) return false;
        TransformationData data = TransformationManager.getTransformation(p);
        return data != null && data.getEntityType() == form;
    }

    static int tier(ServerPlayer p, String prefix) {
        String tree = prefix.substring(0, 2);
        var nodes = SkillTreeData.getUnlockedNodes(p);
        int t = 0;
        for (int i = 1; i <= 3; i++) if (nodes.contains(prefix + "_" + i)) t = i;
        return t;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        if (!TrueForm.active(p)) return;
        if (is(p, ModEntities.BLACK_TORTOISE.get()) && tier(p, "tu_true") >= 2 && p.tickCount % 20 == 0) {
            // the serpent strikes the nearest foe within 5 blocks
            LivingEntity prey = null;
            double best = 25;
            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(5),
                    e -> e != p && e.isAlive() && e instanceof Enemy)) {
                double d = e.distanceToSqr(p);
                if (d < best) { best = d; prey = e; }
            }
            if (prey != null) {
                prey.hurt(p.damageSources().playerAttack(p), LevelPower.scaled(p, 6f));
                p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, prey.getX(), prey.getY() + prey.getBbHeight() * 0.6, prey.getZ(), 1, 0, 0, 0, 0);
                p.level().playSound(null, prey.getX(), prey.getY(), prey.getZ(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
        }
        if (is(p, ModEntities.VERMILION_BIRD.get()) && tier(p, "ph_true") >= 2) {
            // fire trails where it flies; its wingbeats burn all near
            p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 6, 1.2, 0.4, 1.2, 0.02);
            if (p.tickCount % 20 == 0) {
                for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(4),
                        e -> e != p && e.isAlive() && e.distanceTo(p) <= 4)) {
                    e.setSecondsOnFire(3);
                    e.hurt(p.damageSources().playerAttack(p), LevelPower.scaled(p, 3f));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && is(p, ModEntities.BLACK_TORTOISE.get()) && tier(p, "tu_true") >= 2) {
            event.setAmount(event.getAmount() * 0.5f); // its shell
        }
    }
}
