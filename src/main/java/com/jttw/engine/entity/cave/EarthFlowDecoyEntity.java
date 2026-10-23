package com.jttw.engine.entity.cave;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * What Lady Earth Flow leaves behind when she flees: one of her embroidered shoes, made to look
 * like her, with her swords and her name, fighting on feebly in her place. It is no spirit, only a
 * shoe: no XP, no karma, no boss bar. Struck down, or after a minute, it falls back into the shoe
 * it was (dropped where it stood), and whoever is near learns she has fled deeper into the cave.
 */
public class EarthFlowDecoyEntity extends PathfinderMob {

    /** Six of a level-43 character's ordinary hits to fell; a cut of a twentieth of their health. */
    public static final double HITS = 6, CUT = 0.05;
    /** How long the likeness holds, in ticks. */
    public static final int LASTS = 1200;
    /** How far its unmasking is heard. */
    public static final double TELLS = 32;

    public EarthFlowDecoyEntity(EntityType<? extends EarthFlowDecoyEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.EARTH_FLOW_SWORD_MALE.get()));
        setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.EARTH_FLOW_SWORD_FEMALE.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
        setDropChance(EquipmentSlot.OFFHAND, 0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LadyEarthFlowEntity.LEVEL))
                .add(Attributes.ATTACK_DAMAGE, CUT * MobScaling.typicalHealth(LadyEarthFlowEntity.LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Her name, as she would wear it. */
    @Override
    protected Component getTypeName() {
        return Component.empty().append(Component.translatable("entity.jttw_engine.lady_earth_flow")).append(" · Lv " + LadyEarthFlowEntity.LEVEL);
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= LASTS && isAlive()) {
            unmask();
            discard();
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && dead) unmask();
    }

    /** Falls back into the shoe it was: the shoe left where it stood, and those near told. */
    public void unmask() {
        if (!(level() instanceof ServerLevel server)) return;
        spawnAtLocation(new ItemStack(ModItems.EMBROIDERED_SHOE.get()));
        server.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY(1), getZ(), 50, 0.5, 0.9, 0.5, 0.05);
        server.sendParticles(ParticleTypes.POOF, getX(), getY(0.5), getZ(), 15, 0.3, 0.5, 0.3, 0.02);
        playSound(SoundEvents.WOOL_BREAK, 1.5f, 0.8f);
        for (ServerPlayer p : server.players()) {
            if (p.distanceToSqr(this) <= TELLS * TELLS) {
                p.sendSystemMessage(Component.translatable("entity.jttw_engine.earth_flow_decoy.unmasked"));
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOL_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOL_BREAK;
    }
}
