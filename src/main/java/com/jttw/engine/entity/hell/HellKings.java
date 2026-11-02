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
 * The Ten Kings of Hell but King Yama ({@link KingYamaEntity}), each with his court's judgement
 * (and, for those whose judgement falls on one alone, a <b>sentence on all</b> besides: see
 * {@link KingOfHellEntity#beginArea}, the emblem raised {@value KingOfHellEntity#AREA_WIND_UP} ticks
 * and then brought down on everyone in its reach):
 * <ol>
 *   <li><b>Qinguang</b> (秦广王) and the <b>Karma Mirror</b>: it shows a soul its deeds and strikes
 *       it for them: a light blow for the good, up to {@link Qinguang#AT_WORST} times as heavy for
 *       the wicked (by karma, {@link Qinguang#mirrorFactor}; no heavier than a boss's hardest blow); and the <b>mirror's flash</b>: he holds it up and
 *       its light falls over everyone before him ({@link Qinguang#FLASH_REACH} blocks, a cone),
 *       weighed by each one's karma as the mirror's blow is;</li>
 *   <li><b>Chujiang</b> (楚江王), the cold hell: everyone near him is <b>frozen</b>, slowed hard;</li>
 *   <li><b>Songdi</b> (宋帝王), the iron <b>hooks</b>: he hauls whoever he fights to his feet; and
 *       the <b>ring of hooks</b>: they lash out all round him, tearing at everyone near and
 *       dragging them in;</li>
 *   <li><b>Wuguan</b> (五官王), the <b>scales</b>: he weighs his health against his foe's, and takes
 *       half the difference from the healthier foe; and the <b>weight of the scales</b>: the pans
 *       come down on everyone near him, crushing and weighing them down;</li>
 *   <li>(King Yama);</li>
 *   <li><b>Biancheng</b> (卞城王), the bell of the Wailing City: when it tolls everyone near him
 *       <b>kneels</b> (cannot move) for a moment, and is struck by its note;</li>
 *   <li><b>Taishan</b> (泰山王), the cauldron of <b>boiling oil</b>: everyone near him burns;</li>
 *   <li><b>Dushi</b> (都市王), the <b>crushing carts</b>: he charges, and everything he runs down
 *       on his way is crushed and flung;</li>
 *   <li><b>Pingdeng</b> (平等王), "Equal": <b>equal measure</b>, a third of every blow struck him is
 *       struck back on whoever struck it; and his <b>impartial judgement</b>: one blow, the same on
 *       everyone near him, whoever they are;</li>
 *   <li><b>Zhuanlun</b> (转轮王), the <b>Wheel of Reincarnation</b>: struck down the first time,
 *       he is born again, at half his health; and his judgement is Meng Po's soup, forgetting: the
 *       good effects on his foe are gone, and it reels; and the <b>Wheel turning</b>: it rolls out
 *       from him in a shockwave over everyone near, flinging them back.</li>
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
        /** The mirror's blow, of his: for the good (karma 100 and up), and at most for the wicked (at -1000 and below: a third of their health). */
        public static final double FOR_THE_GOOD = 0.6, AT_WORST = 1.3;
        /** The mirror's flash, of a same-level character's health, for one of no account (weighed by karma as the mirror's blow); at the worst, {@link #AT_WORST} times it. */
        public static final double FLASH = 0.2;
        /** How far the flash reaches before him, and how wide it spreads (each side of where he faced, in degrees). */
        public static final double FLASH_REACH = 10, FLASH_HALF_ANGLE = 45;
        /** How long it dazzles. */
        public static final int DAZZLES = 30;

        public Qinguang(EntityType<? extends Qinguang> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 1;
        }

        /** How heavily the mirror strikes a soul of this karma: lightly the good, more the worse they are, up to {@link #AT_WORST} at -1000. */
        public static double mirrorFactor(int karma) {
            if (karma >= 100) return FOR_THE_GOOD;
            if (karma >= 0) return 1;
            return 1 + (AT_WORST - 1) * Math.min(1, -karma / 1000.0);
        }

        /** The mirror's blow on {@code target}, as it would fall. */
        public float mirrorBlow(LivingEntity target) {
            int karma = target instanceof Player p ? Morality.karma(p) : 0;
            return (float) (blow() * mirrorFactor(karma));
        }

        @Override
        public double areaShare() {
            return FLASH;
        }

        @Override
        public double areaReach() {
            return FLASH_REACH;
        }

        /** The flash and the mirror's blow, at their hardest: on the wickedest. */
        @Override
        public double[] specialShares() {
            return new double[]{FLASH * AT_WORST, BLOW * AT_WORST};
        }

        /** Before him only: within {@value #FLASH_HALF_ANGLE} degrees of where he faced as he raised the mirror. */
        @Override
        public boolean inArea(LivingEntity e) {
            if (!super.inArea(e)) return false;
            Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
            if (to.lengthSqr() < 1) return true;
            return to.normalize().dot(areaFacing()) >= Math.cos(Math.toRadians(FLASH_HALF_ANGLE));
        }

        /** Weighed by karma, as the mirror's blow. */
        @Override
        public float areaHitOn(LivingEntity e) {
            int karma = e instanceof Player p ? Morality.karma(p) : 0;
            return (float) (hitOf(FLASH) * mirrorFactor(karma));
        }

        @Override
        protected void raiseSound() {
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 2.5f, 0.5f);
            playSound(SoundEvents.BEACON_POWER_SELECT, 1.5f, 1.4f);
        }

        /** Light gathering in the mirror, and the ground it will fall on glinting. */
        @Override
        protected void raiseSigns(ServerLevel server, int left) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() + 0.6, getZ(), 2, 0.25, 0.25, 0.25, 0.01);
            if (left % 3 == 0) {
                for (int i = 0; i < 10; i++) {
                    Vec3 way = areaFacing().yRot((float) Math.toRadians((random.nextDouble() * 2 - 1) * FLASH_HALF_ANGLE));
                    double d = 1.5 + random.nextDouble() * (FLASH_REACH - 1.5);
                    server.sendParticles(ParticleTypes.WAX_OFF, getX() + way.x * d, getY() + 0.2, getZ() + way.z * d, 1, 0, 0, 0, 0);
                }
            }
        }

        /** It dazzles. */
        @Override
        protected void onAreaHit(LivingEntity e) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DAZZLES, 0), this);
        }

        /** The flash: a sheet of light out over the cone. */
        @Override
        protected void releaseSigns() {
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3f, 0.4f);
            playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 2f, 1.2f);
            if (level() instanceof ServerLevel server) {
                for (int i = 0; i < 70; i++) {
                    Vec3 way = areaFacing().yRot((float) Math.toRadians((random.nextDouble() * 2 - 1) * FLASH_HALF_ANGLE));
                    double d = 1 + random.nextDouble() * (FLASH_REACH - 1);
                    server.sendParticles(ParticleTypes.END_ROD, getX() + way.x * d, getY() + 0.4 + random.nextDouble() * 1.6, getZ() + way.z * d,
                            1, 0, 0, 0, 0.02);
                }
                server.sendParticles(ParticleTypes.FLASH, getX() + areaFacing().x, getY(0.8), getZ() + areaFacing().z, 1, 0, 0, 0, 0);
            }
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
        /** His frost's bite, of his blow: no less than the heaviest of Hell's creatures' hits. */
        public static final double FROST = 0.6;

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
                e.hurt(damageSources().freeze(), strikes((float) (blow() * FROST))); // frost names no striker: scaled here
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
        /** The ring of hooks: how far it reaches, how hard it tears (of a same-level character's health), how hard it drags in. */
        public static final double RING_REACH = 7, RING = 0.22, DRAG = 0.9;

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

        @Override
        public double areaShare() {
            return RING;
        }

        @Override
        public double areaReach() {
            return RING_REACH;
        }

        @Override
        protected DamageSource areaSource() {
            return damageSources().mobAttack(this);
        }

        @Override
        protected void raiseSound() {
            playSound(SoundEvents.CHAIN_PLACE, 2.5f, 0.5f);
            playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 2f, 0.6f);
        }

        /** The chains rattling round him, and the ring they will reach marked out in sparks. */
        @Override
        protected void raiseSigns(ServerLevel server, int left) {
            double a = left * 0.6;
            server.sendParticles(ParticleTypes.CRIT, getX() + Math.cos(a) * 1.2, getY(0.6), getZ() + Math.sin(a) * 1.2, 3, 0.05, 0.3, 0.05, 0);
            if (left % 5 == 0) {
                ring(server, ParticleTypes.CRIT, RING_REACH, 32);
                playSound(SoundEvents.CHAIN_STEP, 1.5f, 0.5f);
            }
        }

        /** The hook bites and drags it in toward him. */
        @Override
        protected void onAreaHit(LivingEntity e) {
            Vec3 in = position().subtract(e.position()).multiply(1, 0, 1);
            if (in.lengthSqr() > 1e-4) {
                in = in.normalize().scale(DRAG);
                e.setDeltaMovement(in.x, 0.25, in.z);
                e.hurtMarked = true;
            }
        }

        /** The hooks flung out all round him and back. */
        @Override
        protected void releaseSigns() {
            playSound(SoundEvents.CHAIN_BREAK, 2.5f, 0.5f);
            playSound(SoundEvents.TRIDENT_RETURN, 2f, 0.6f);
            if (level() instanceof ServerLevel server) {
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI * 2 / 12;
                    for (int i = 1; i <= 7; i++) {
                        double d = i * RING_REACH / 7;
                        server.sendParticles(ParticleTypes.CRIT, getX() + Math.cos(a) * d, getY(0.5), getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    // ── 4 ───────────────────────────────────────────────────────────────

    public static class Wuguan extends KingOfHellEntity {
        /** The share of the difference he takes. */
        public static final double LEVY = 0.5;
        /** The weight of the scales: how far, how hard (of a same-level character's health); how long it weighs one down. */
        public static final double WEIGHT_REACH = 6, WEIGHT = 0.24;
        public static final int WEIGHED_DOWN = 60;

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

        @Override
        public double areaShare() {
            return WEIGHT;
        }

        @Override
        public double areaReach() {
            return WEIGHT_REACH;
        }

        @Override
        protected void raiseSound() {
            playSound(SoundEvents.ANVIL_USE, 1.5f, 0.5f);
            playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 2f, 0.6f);
        }

        /** The glyphs of the weighing settle on the ground it will fall on. */
        @Override
        protected void raiseSigns(ServerLevel server, int left) {
            server.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + getBbHeight() + 0.8, getZ(), 6, 0.4, 0.2, 0.4, 0.6);
            if (left % 4 == 0) {
                ring(server, ParticleTypes.ENCHANT, WEIGHT_REACH, 28);
                for (int i = 0; i < 8; i++) {
                    double a = random.nextDouble() * Math.PI * 2, d = random.nextDouble() * WEIGHT_REACH;
                    server.sendParticles(ParticleTypes.ENCHANT, getX() + Math.cos(a) * d, getY() + 2.5, getZ() + Math.sin(a) * d, 0, 0, -1, 0, 0.6);
                }
            }
        }

        /** Weighed down. */
        @Override
        protected void onAreaHit(LivingEntity e) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WEIGHED_DOWN, 2), this);
            e.setDeltaMovement(e.getDeltaMovement().multiply(0.2, 0, 0.2).add(0, -0.5, 0));
            e.hurtMarked = true;
        }

        /** The pans come down: the ground shakes. */
        @Override
        protected void releaseSigns() {
            playSound(SoundEvents.ANVIL_LAND, 2.5f, 0.5f);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.4f);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                                net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState()),
                        getX(), getY() + 0.1, getZ(), 120, WEIGHT_REACH / 2, 0.1, WEIGHT_REACH / 2, 0.2);
                ring(server, ParticleTypes.ENCHANTED_HIT, WEIGHT_REACH, 36);
            }
        }
    }

    // ── 6 ───────────────────────────────────────────────────────────────

    public static class Biancheng extends KingOfHellEntity {
        public static final double REACH = 7;
        public static final int KNEEL = 40;
        /** The bell's note, of a same-level character's health: everyone it brings to their knees, it strikes. */
        public static final double NOTE = 0.15;

        public Biancheng(EntityType<? extends Biancheng> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 6;
        }

        /** The bell's note on all it tolls for. */
        @Override
        public double[] specialShares() {
            return new double[]{NOTE};
        }

        /** Tolls the bell of the Wailing City: all within his reach kneel (cannot move) a moment. Returns how many. */
        public int toll(List<? extends LivingEntity> near) {
            int n = 0;
            for (LivingEntity e : near) {
                if (e.distanceTo(this) > REACH) continue;
                com.jttw.engine.world.Stun.pin(e, KNEEL, this);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, KNEEL, 1), this);
                e.setDeltaMovement(Vec3.ZERO);
                e.hurtMarked = true;
                e.invulnerableTime = 0;
                e.hurt(damageSources().sonicBoom(this), hitOf(NOTE));
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
        /** The oil's scald, of his blow: no less than the heaviest of Hell's creatures' hits. */
        public static final double SCALD = 0.6;

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
                e.hurt(damageSources().inFire(), strikes((float) (blow() * SCALD))); // the oil names no striker: scaled here
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
        /** His charge's speed; the crush, of his blow (no heavier than a boss's hardest blow); how far it flings. */
        public static final double SPEED = 0.95, CRUSH = 1.3, FLING = 2.0;
        public static final int RUN = 14;
        private int run;
        private Vec3 heading = Vec3.ZERO;
        /** What this charge has run down already: each once, and on it goes through the rest. */
        private final java.util.Set<java.util.UUID> crushed = new java.util.HashSet<>();

        public Dushi(EntityType<? extends Dushi> type, Level level) {
            super(type, level);
        }

        @Override
        public int court() {
            return 8;
        }

        /** The carts' crush, on everyone they run down. */
        @Override
        public double[] specialShares() {
            return new double[]{BLOW * CRUSH};
        }

        /** Whether he is charging now. */
        public boolean charging() {
            return run > 0;
        }

        /** Crushes {@code target} under the cart: his blow and a third, and it is flung. */
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
            crushed.clear();
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
                    e -> e != this && e.isAlive() && !(e instanceof HellMonster) && !(e instanceof KingOfHellEntity) && canAttack(e)
                            && !crushed.contains(e.getUUID()))) {
                crushed.add(hit.getUUID());
                crush(hit);
            }
            if (horizontalCollision) run = 0;
        }
    }

    // ── 9 ───────────────────────────────────────────────────────────────

    public static class Pingdeng extends KingOfHellEntity {
        /** The share of a blow he strikes back. */
        public static final double EQUAL = 1 / 3.0;
        /** His impartial judgement: how far, and how hard (of a same-level character's health: the same on all). */
        public static final double JUDGEMENT_REACH = 8, JUDGEMENT = 0.25;

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
        public double areaShare() {
            return JUDGEMENT;
        }

        @Override
        public double areaReach() {
            return JUDGEMENT_REACH;
        }

        @Override
        protected void raiseSound() {
            playSound(SoundEvents.BELL_RESONATE, 2f, 0.7f);
            playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 2f, 0.6f);
        }

        /** A balance of gold over his head, and the ring of those it will weigh alike. */
        @Override
        protected void raiseSigns(ServerLevel server, int left) {
            double y = getY() + getBbHeight() + 0.7;
            server.sendParticles(ParticleTypes.WAX_ON, getX() - 0.6, y, getZ(), 1, 0.05, 0.05, 0.05, 0);
            server.sendParticles(ParticleTypes.WAX_ON, getX() + 0.6, y, getZ(), 1, 0.05, 0.05, 0.05, 0);
            if (left % 4 == 0) ring(server, ParticleTypes.WAX_ON, JUDGEMENT_REACH, 40);
        }

        /** The judgement falls alike on all: a column of light on each. */
        @Override
        protected void onAreaHit(LivingEntity e) {
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY(1.5), e.getZ(), 12, 0.1, 0.8, 0.1, 0.02);
            }
        }

        @Override
        protected void releaseSigns() {
            playSound(SoundEvents.BELL_BLOCK, 2.5f, 1.2f);
            playSound(SoundEvents.TRIDENT_THUNDER.value(), 1f, 1.5f);
            if (level() instanceof ServerLevel server) ring(server, ParticleTypes.END_ROD, JUDGEMENT_REACH, 48);
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
        /** The Wheel turning: how far the shockwave goes, how hard (of a same-level character's health), how far it flings. */
        public static final double WHEEL_REACH = 8, WHEEL = 0.25, FLING = 1.4;
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

        @Override
        public double areaShare() {
            return WHEEL;
        }

        @Override
        public double areaReach() {
            return WHEEL_REACH;
        }

        @Override
        protected void raiseSound() {
            playSound(SoundEvents.BEACON_ACTIVATE, 2f, 0.6f);
            playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 2f, 0.6f);
        }

        /** The Wheel turning over his head, faster and faster, its six spokes in soul fire. */
        @Override
        protected void raiseSigns(ServerLevel server, int left) {
            double turn = (AREA_WIND_UP - left) * (AREA_WIND_UP - left) * 0.01, y = getY() + getBbHeight() + 0.9;
            for (int k = 0; k < 6; k++) {
                double a = turn + k * Math.PI / 3;
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(a) * 1.1, y + Math.sin(a) * 1.1, getZ(), 1, 0, 0, 0, 0);
            }
            if (left % 6 == 0) ring(server, ParticleTypes.SOUL, WHEEL_REACH, 32);
        }

        /** Flung out from him. */
        @Override
        protected void onAreaHit(LivingEntity e) {
            Vec3 out = e.position().subtract(position()).multiply(1, 0, 1);
            out = out.lengthSqr() < 1e-4 ? areaFacing() : out.normalize();
            e.push(out.x * FLING, 0.45, out.z * FLING);
            e.hurtMarked = true;
        }

        /** The shockwave rolling out from him. */
        @Override
        protected void releaseSigns() {
            playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.7f);
            playSound(SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 2f, 0.6f);
            if (level() instanceof ServerLevel server) {
                for (double r = 1; r <= WHEEL_REACH; r += 1.5) ring(server, ParticleTypes.SOUL_FIRE_FLAME, r, (int) (8 * r));
                server.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY(1), getZ(), 1, 0, 0, 0, 0);
            }
        }

        /** Two lives: his own, then, the Wheel turned, half of it again. */
        @Override
        public double[] lives() {
            return new double[]{1, REBORN_AT};
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
