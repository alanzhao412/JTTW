package com.jttw.engine.entity.heaven;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A heavenly archer (天兵弓手): a soldier of the host with a long gilt bow, who keeps his distance on his
 * cloud and looses arrows, each {@value #BLOW} of a same-level character's health. Lightly armoured:
 * three ordinary hits to kill.
 */
public class HeavenlyArcherEntity extends HeavenMonster implements RangedAttackMob {

    public static final double HITS = 3, BLOW = 0.08;
    /** How far off it shoots from, and how often. */
    public static final float RANGE = 14f;
    public static final int EVERY = 40;
    /** The speed an arrow leaves its bow at. */
    public static final float ARROW_SPEED = 1.6f;

    public HeavenlyArcherEntity(EntityType<? extends HeavenlyArcherEntity> type, Level level) {
        super(type, level);
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.03f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(HITS, BLOW, 0.3, 0.55);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BLOW;
    }

    @Override
    protected void addAttackGoals() {
        // as vanilla's bow goal does, it shows it is fighting (the renderer draws the bow while it is)
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, EVERY, RANGE) {
            @Override
            public void start() {
                super.start();
                setAggressive(true);
            }

            @Override
            public void stop() {
                super.stop();
                setAggressive(false);
            }
        });
    }

    /** An arrow at {@code target}, landing at its blow. */
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), null);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        double dy = target.getY(0.35) - arrow.getY();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + flat * 0.12, dz, ARROW_SPEED, 2f);
        arrow.setBaseDamage(hitOf(BLOW) / ARROW_SPEED);
        playSound(SoundEvents.SKELETON_SHOOT, 1f, 1f / (getRandom().nextFloat() * 0.4f + 0.8f));
        level().addFreshEntity(arrow);
    }
}
