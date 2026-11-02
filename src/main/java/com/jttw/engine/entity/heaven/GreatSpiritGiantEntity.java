package com.jttw.engine.entity.heaven;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Great Spirit Giant (巨灵神), Li Jing's vanguard against the Monkey King (chapter 4), a towering god
 * with a pair of great axes (宣花斧): a miniboss of the Jade Palace. Thirty-six ordinary hits to kill.
 *
 * <p>His <b>axe-quake</b> (every {@value #EVERY} ticks, a foe within {@value #REACH} blocks): he heaves
 * both axes high over his head ({@value #WIND_UP} ticks, the ring of ground they will shake marked), then
 * brings them down together, the shock striking all round him ({@value #QUAKE} as a regular's, a
 * miniboss's hardest: seven kill) and throwing them up into the air.
 */
public class GreatSpiritGiantEntity extends HeavenMonster {

    public static final double HITS = 18, BLOW = 0.115, QUAKE = 0.12, REACH = 6;
    public static final int WIND_UP = 30, EVERY = 170;

    public GreatSpiritGiantEntity(EntityType<? extends GreatSpiritGiantEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(HITS, BLOW, 0.26, 0.45).add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 4.5).add(Attributes.ATTACK_KNOCKBACK, 1.2);
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
    protected double[] specialShares() {
        return new double[]{QUAKE};
    }

    @Override
    public boolean alwaysMiniboss() {
        return true;
    }

    @Override
    protected AreaBlow makeBlow() {
        return new AreaBlow(this, new AreaBlow.Spec(AreaBlow.Shape.AROUND, REACH, 0, WIND_UP, EVERY, QUAKE,
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GOLD_BLOCK.defaultBlockState()),
                SoundEvents.RAVAGER_ROAR, SoundEvents.ANVIL_LAND),
                (e, from) -> {
                    Vec3 out = e.position().subtract(from).multiply(1, 0, 1);
                    Vec3 n = out.lengthSqr() > 1e-4 ? out.normalize() : Vec3.ZERO;
                    e.push(n.x * 0.4, 0.75, n.z * 0.4);
                    e.hurtMarked = true;
                });
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }
}
