package com.jttw.engine.entity.waters;

import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A bolt of the sea's power, hurled by the Yaksha and by the Dragon Kings: it cuts straight through
 * the water (the sea does not slow it), and what it strikes takes its hurt, as magic, and its
 * element's touch:
 * <ul>
 *   <li>{@link #TIDE}: a blow of the tide, throwing them back;</li>
 *   <li>{@link #STEAM}: scalding, sapping their strength;</li>
 *   <li>{@link #CURRENT}: a twist of current, dragging them toward whoever threw it;</li>
 *   <li>{@link #ICE}: freezing, slowing them hard.</li>
 * </ul>
 * It passes through the sea's own creatures.
 */
public class SeaBoltEntity extends Fireball {

    public static final int TIDE = 0, STEAM = 1, CURRENT = 2, ICE = 3;
    /** How long it flies before it spends itself, in ticks. */
    public static final int LIFE = 80;

    private float damage = 10f;
    private int element = TIDE;

    public SeaBoltEntity(EntityType<? extends SeaBoltEntity> type, Level level) {
        super(type, level);
    }

    /** A bolt from {@code thrower}'s eyes along {@code way}, of {@code element}. */
    public SeaBoltEntity(Level level, LivingEntity thrower, Vec3 way, float damage, int element) {
        super(ModEntities.SEA_BOLT.get(), thrower.getX(), thrower.getEyeY() - 0.2, thrower.getZ(), way.normalize(), level);
        setOwner(thrower);
        this.damage = damage;
        this.element = element;
        accelerationPower = 0.06;
        setDeltaMovement(way.normalize().scale(0.9));
        setItem(new ItemStack(switch (element) {
            case STEAM -> Items.MAGMA_CREAM;
            case CURRENT -> Items.NAUTILUS_SHELL;
            case ICE -> Items.BLUE_ICE;
            default -> Items.HEART_OF_THE_SEA;
        }));
    }

    public int element() {
        return element;
    }

    public float damage() {
        return damage;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return element == ICE ? ParticleTypes.SNOWFLAKE : element == STEAM ? ParticleTypes.CLOUD : ParticleTypes.BUBBLE;
    }

    /** The sea does not slow it. */
    @Override
    protected float getLiquidInertia() {
        return 0.95f;
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && !(e instanceof SeaMonster) && !(e instanceof DragonKingEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity owner = getOwner();
        if (hit.getEntity() == owner || !(hit.getEntity() instanceof LivingEntity target)) return;
        if (target.hurt(damageSources().indirectMagic(this, owner instanceof LivingEntity o ? o : this), damage)) {
            touch(target, owner);
        }
    }

    /** Its element's touch on whoever it struck. */
    public void touch(LivingEntity target, Entity owner) {
        switch (element) {
            case STEAM -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), owner);
            case ICE -> {
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 80));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), owner);
            }
            case CURRENT -> {
                if (owner != null) {
                    Vec3 pull = owner.position().subtract(target.position()).normalize().scale(0.8);
                    target.push(pull.x, pull.y * 0.5, pull.z);
                    target.hurtMarked = true;
                }
            }
            default -> {
                Vec3 push = getDeltaMovement().normalize().scale(1.2);
                target.push(push.x, 0.25, push.z);
                target.hurtMarked = true;
            }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(getTrailParticle(), getX(), getY(), getZ(), 14, 0.3, 0.3, 0.3, 0.05);
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIFE) discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Element", element);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        element = tag.getInt("Element");
    }
}
