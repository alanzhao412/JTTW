package com.jttw.engine.entity.hell;

import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Ten Kings of Hell but King Yama ({@link KingYamaEntity}), each with his court's judgement:
 * <ol>
 *   <li><b>Qinguang</b> (秦广王) and the <b>Karma Mirror</b>: it shows a soul its deeds and strikes
 *       it for them: a light blow for the good, up to three times as heavy for the wicked (by
 *       karma, {@link Qinguang#mirrorFactor});</li>
 *   <li><b>Chujiang</b> (楚江王), the cold hell: everyone near him is <b>frozen</b>, slowed hard;</li>
 *   <li><b>Songdi</b> (宋帝王), the iron <b>hooks</b>: he hauls whoever he fights to his feet;</li>
 *   <li><b>Wuguan</b> (五官王), the <b>scales</b>: he weighs his health against his foe's, and takes
 *       half the difference from the healthier foe;</li>
 *   <li>(King Yama);</li>
 *   <li><b>Biancheng</b> (卞城王), the bell of the Wailing City: when it tolls everyone near him
 *       <b>kneels</b> (cannot move) for a moment;</li>
 *   <li><b>Taishan</b> (泰山王), the cauldron of <b>boiling oil</b>: everyone near him burns;</li>
 *   <li><b>Dushi</b> (都市王), the <b>crushing carts</b>: he charges, and what he runs down is
 *       crushed and flung;</li>
 *   <li><b>Pingdeng</b> (平等王), "Equal": <b>equal measure</b>, a third of every blow struck him is
 *       struck back on whoever struck it;</li>
 *   <li><b>Zhuanlun</b> (转轮王), the <b>Wheel of Reincarnation</b>: struck down the first time,
 *       he is born again, at half his health; and his judgement is Meng Po's soup, forgetting: the
 *       good effects on his foe are gone, and it reels.</li>
 * </ol>
 */
public final class HellKings {

    private HellKings() {}

    /** The king of the n-th court's type. */
    public static EntityType<? extends KingOfHellEntity> type(int court) {
        return switch (court) {
            case 1 -> ModEntities.KING_QINGUANG.get();
            case 2 -> ModEntities.KING_CHUJIANG.get();
            case 3 -> ModEntities.KING_SONGDI.get();
            case 4 -> ModEntities.KING_WUGUAN.get();
            case 5 -> ModEntities.KING_YAMA.get();
            case 6 -> ModEntities.KING_BIANCHENG.get();
            case 7 -> ModEntities.KING_TAISHAN.get();
            case 8 -> ModEntities.KING_DUSHI.get();
            case 9 -> ModEntities.KING_PINGDENG.get();
            case 10 -> ModEntities.KING_ZHUANLUN.get();
            default -> throw new IllegalArgumentException("no court " + court);
        };
    }

    /** The registry names of the kings, court by court. */
    public static final List<String> IDS = List.of("king_qinguang", "king_chujiang", "king_songdi", "king_wuguan", "king_yama",
            "king_biancheng", "king_taishan", "king_dushi", "king_pingdeng", "king_zhuanlun");

    /** The players within {@code r} of him, not watching or building. */
    static List<Player> near(KingOfHellEntity king, double r) {
        return king.level().getEntitiesOfClass(Player.class, king.getBoundingBox().inflate(r),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && king.canAttack(p));
    }

    // ── 1 ───────────────────────────────────────────────────────────────

    public static class Qinguang extends KingOfHellEntity {
        /** The mirror's blow, of his: for the good (karma 100 and up), and at most for the wicked. */
        public static final double FOR_THE_GOOD = 0.6, AT_WORST = 3;

        public Qinguang(EntityType<? extends Qinguang> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 1;
        }

        /** How heavily the mirror strikes a soul of this karma: lightly the good, more the worse they are, up to three times. */
        public static double mirrorFactor(int karma) {
            if (karma >= 100) return FOR_THE_GOOD;
            if (karma >= 0) return 1;
            return Math.min(AT_WORST, 1 + -karma / 500.0);
        }

        /** The mirror's blow on {@code target}, as it would fall. */
        public float mirrorBlow(LivingEntity target) {
            int karma = target instanceof Player p ? Morality.karma(p) : 0;
            return (float) (blow() * mirrorFactor(karma));
        }

        @Override
        public boolean judge(LivingEntity target) {
            if (!hasLineOfSight(target)) return false;
            target.invulnerableTime = 0;
            target.hurt(damageSources().indirectMagic(this, this), mirrorBlow(target));
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.5f);
            if (level() instanceof ServerLevel server) {
                Vec3 from = position().add(0, getBbHeight() * 0.7, 0), to = target.position().add(0, target.getBbHeight() * 0.6, 0);
                for (int i = 0; i <= 12; i++) {
                    Vec3 p = from.add(to.subtract(from).scale(i / 12.0));
                    server.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                }
            }
            return true;
        }
    }

    // ── 2 ───────────────────────────────────────────────────────────────

    public static class Chujiang extends KingOfHellEntity {
        public static final double REACH = 8;
        public static final int FROZEN = 100;

        public Chujiang(EntityType<? extends Chujiang> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 2;
        }

        /** Freezes everyone within his reach: frost over them, slowed hard. Returns how many. */
        public int freeze(List<? extends LivingEntity> near) {
            int n = 0;
            for (LivingEntity e : near) {
                if (e.distanceTo(this) > REACH) continue;
                e.setTicksFrozen(e.getTicksRequiredToFreeze() + FROZEN);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FROZEN, 2), this);
                e.invulnerableTime = 0;
                e.hurt(damageSources().freeze(), blow() * 0.4f);
                n++;
            }
            playSound(SoundEvents.POWDER_SNOW_BREAK, 2f, 0.6f);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(1), getZ(), 120, REACH / 2, 1, REACH / 2, 0.02);
            }
            return n;
        }

        @Override
        public boolean judge(LivingEntity target) {
            List<LivingEntity> near = new java.util.ArrayList<>(near(this, REACH));
            if (!near.contains(target)) near.add(target);
            return freeze(near) > 0;
        }
    }

    // ── 3 ───────────────────────────────────────────────────────────────

    public static class Songdi extends KingOfHellEntity {
        public static final double REACH = 14;

        public Songdi(EntityType<? extends Songdi> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 3;
        }

        /** Hooks {@code target} and hauls it to his feet, a blow and a half from him. Returns whether he did. */
        public boolean hookAndHaul(LivingEntity target) {
            if (distanceTo(target) > REACH) return false;
            Vec3 front = target.position().subtract(position()).multiply(1, 0, 1);
            front = front.lengthSqr() < 1e-4 ? getLookAngle().multiply(1, 0, 1) : front.normalize();
            Vec3 to = position().add(front.scale(getBbWidth() / 2 + 1.2));
            Vec3 from = target.position();
            target.teleportTo(to.x, getY(), to.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            strike(target);
            playSound(SoundEvents.CHAIN_BREAK, 2f, 0.6f);
            if (level() instanceof ServerLevel server) {
                for (int i = 0; i <= 10; i++) {
                    Vec3 p = from.add(to.subtract(from).scale(i / 10.0)).add(0, 1, 0);
                    server.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
                }
            }
            return true;
        }

        @Override
        public boolean judge(LivingEntity target) {
            return distanceTo(target) > 3 && hookAndHaul(target);
        }
    }

    // ── 4 ───────────────────────────────────────────────────────────────

    public static class Wuguan extends KingOfHellEntity {
        /** The share of the difference he takes. */
        public static final double LEVY = 0.5;

        public Wuguan(EntityType<? extends Wuguan> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 4;
        }

        /** What the scales take from {@code target}: half the difference of their health shares, of its health, if it is the healthier. */
        public float levy(LivingEntity target) {
            double diff = target.getHealth() / target.getMaxHealth() - getHealth() / getMaxHealth();
            return diff <= 0 ? 0 : (float) (diff * LEVY * target.getMaxHealth());
        }

        @Override
        public boolean judge(LivingEntity target) {
            float take = levy(target);
            playSound(SoundEvents.ANVIL_PLACE, 1f, 1.6f);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY(1.2), target.getZ(), 40, 0.5, 0.5, 0.5, 0.5);
            }
            if (take <= 0) return true;
            target.invulnerableTime = 0;
            target.hurt(damageSources().indirectMagic(this, this), take);
            return true;
        }
    }

    // ── 6 ───────────────────────────────────────────────────────────────

    public static class Biancheng extends KingOfHellEntity {
        public static final double REACH = 7;
        public static final int KNEEL = 40;

        public Biancheng(EntityType<? extends Biancheng> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 6;
        }

        /** Tolls the bell of the Wailing City: all within his reach kneel (cannot move) a moment. Returns how many. */
        public int toll(List<? extends LivingEntity> near) {
            int n = 0;
            for (LivingEntity e : near) {
                if (e.distanceTo(this) > REACH) continue;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, KNEEL, 6), this);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, KNEEL, 1), this);
                e.setDeltaMovement(Vec3.ZERO);
                e.hurtMarked = true;
                n++;
            }
            playSound(SoundEvents.BELL_BLOCK, 3f, 0.5f);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY(1.2), getZ(), 1, 0, 0, 0, 0);
            }
            return n;
        }

        @Override
        public boolean judge(LivingEntity target) {
            List<LivingEntity> near = new java.util.ArrayList<>(near(this, REACH));
            if (!near.contains(target)) near.add(target);
            return toll(near) > 0;
        }
    }

    // ── 7 ───────────────────────────────────────────────────────────────

    public static class Taishan extends KingOfHellEntity {
        public static final double REACH = 7;
        public static final int BURNS = 100;

        public Taishan(EntityType<? extends Taishan> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 7;
        }

        @Override
        public boolean fireImmune() {
            return true;
        }

        /** The boiling oil: all within his reach are scalded and burn. Returns how many. */
        public int scald(List<? extends LivingEntity> near) {
            int n = 0;
            for (LivingEntity e : near) {
                if (e.distanceTo(this) > REACH) continue;
                e.igniteForTicks(BURNS);
                e.invulnerableTime = 0;
                e.hurt(damageSources().inFire(), blow() * 0.5f);
                n++;
            }
            playSound(SoundEvents.LAVA_POP, 2f, 0.6f);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.LAVA, getX(), getY(0.5), getZ(), 30, REACH / 3, 0.5, REACH / 3, 0.1);
                server.sendParticles(ParticleTypes.FLAME, getX(), getY(0.3), getZ(), 80, REACH / 2, 0.2, REACH / 2, 0.02);
            }
            return n;
        }

        @Override
        public boolean judge(LivingEntity target) {
            List<LivingEntity> near = new java.util.ArrayList<>(near(this, REACH));
            if (!near.contains(target)) near.add(target);
            return scald(near) > 0;
        }
    }

    // ── 8 ───────────────────────────────────────────────────────────────

    public static class Dushi extends KingOfHellEntity {
        public static final double SPEED = 0.95, CRUSH = 1.6, FLING = 2.0;
        public static final int RUN = 14;
        private int run;
        private Vec3 heading = Vec3.ZERO;

        public Dushi(EntityType<? extends Dushi> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 8;
        }

        /** Whether he is charging now. */
        public boolean charging() {
            return run > 0;
        }

        /** Crushes {@code target} under the cart: his blow and a half and more, and it is flung. */
        public boolean crush(LivingEntity target) {
            target.invulnerableTime = 0;
            boolean hit = target.hurt(damageSources().mobAttack(this), (float) (blow() * CRUSH));
            if (hit) {
                Vec3 away = target.position().subtract(position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1e-4 ? heading : away.normalize();
                target.push(away.x * FLING, 0.6, away.z * FLING);
                target.hurtMarked = true;
            }
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 2f, 0.6f);
            return hit;
        }

        /** Charges at {@code target}. */
        public void charge(LivingEntity target) {
            Vec3 to = target.position().subtract(position()).multiply(1, 0, 1);
            heading = to.lengthSqr() < 1e-4 ? getLookAngle().multiply(1, 0, 1).normalize() : to.normalize();
            run = RUN;
            playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.7f);
        }

        @Override
        public boolean judge(LivingEntity target) {
            if (distanceTo(target) < 3) return false;
            charge(target);
            return true;
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (run <= 0) return;
            run--;
            getNavigation().stop();
            setYRot((float) Math.toDegrees(Math.atan2(-heading.x, heading.z)));
            setYBodyRot(getYRot());
            setDeltaMovement(heading.x * SPEED, getDeltaMovement().y, heading.z * SPEED);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 2, 0.3, 0.1, 0.3, 0.01);
            }
            for (LivingEntity hit : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.7),
                    e -> e != this && e.isAlive() && !(e instanceof HellMonster) && !(e instanceof KingOfHellEntity) && canAttack(e))) {
                crush(hit);
                run = 0;
            }
            if (horizontalCollision) run = 0;
        }
    }

    // ── 9 ───────────────────────────────────────────────────────────────

    public static class Pingdeng extends KingOfHellEntity {
        /** The share of a blow he strikes back. */
        public static final double EQUAL = 1 / 3.0;

        public Pingdeng(EntityType<? extends Pingdeng> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 9;
        }

        /** He has no judgement to pass: equal measure is always his. */
        @Override
        public boolean judge(LivingEntity target) {
            return false;
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            float before = getHealth();
            boolean hurt = super.hurt(source, amount);
            if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity striker && striker != this
                    && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
                float taken = Math.max(0, before - getHealth());
                if (taken > 0) {
                    striker.invulnerableTime = 0;
                    striker.hurt(damageSources().thorns(this), (float) (taken * EQUAL));
                    if (level() instanceof ServerLevel server) {
                        server.sendParticles(ParticleTypes.ENCHANTED_HIT, striker.getX(), striker.getY(1), striker.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
                    }
                }
            }
            return hurt;
        }
    }

    // ── 10 ──────────────────────────────────────────────────────────────

    public static class Zhuanlun extends KingOfHellEntity {
        /** Reborn at this share of his health. */
        public static final double REBORN_AT = 0.5;
        public static final int REELS = 100;
        private boolean reborn;

        public Zhuanlun(EntityType<? extends Zhuanlun> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 10;
        }

        /** Whether he has turned the Wheel once already (been born again). */
        public boolean reborn() {
            return reborn;
        }

        /** Meng Po's soup on {@code target}: its good effects forgotten, and it reels. */
        public void soup(LivingEntity target) {
            for (MobEffectInstance e : List.copyOf(target.getActiveEffects())) {
                if (e.getEffect().value().isBeneficial()) target.removeEffect(e.getEffect());
            }
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, REELS, 0), this);
            playSound(SoundEvents.GENERIC_DRINK, 1.5f, 0.6f);
            if (target instanceof ServerPlayer p) p.displayClientMessage(net.minecraft.network.chat.Component.translatable("entity.jttw_engine.king_zhuanlun.soup"), true);
        }

        @Override
        public boolean judge(LivingEntity target) {
            soup(target);
            return true;
        }

        /** Struck down the first time, the Wheel turns and he is born again. */
        @Override
        public void die(DamageSource source) {
            if (!reborn && !level().isClientSide && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                reborn = true;
                setHealth((float) (getMaxHealth() * REBORN_AT));
                removeAllEffects();
                playSound(SoundEvents.TOTEM_USE, 1.5f, 0.6f);
                if (level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY(1), getZ(), 80, 0.6, 1, 0.6, 0.3);
                    for (ServerPlayer p : server.players()) {
                        if (p.distanceTo(this) < 32) p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("entity.jttw_engine.king_zhuanlun.reborn"));
                    }
                }
                return;
            }
            super.die(source);
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putBoolean("Reborn", reborn);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            reborn = tag.getBoolean("Reborn");
        }
    }
}
