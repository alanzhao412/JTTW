package com.jttw.engine.entity.hell;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * King Yama (阎罗王, Yanluo), Lord of the Fifth Court, the most feared of the Ten Kings: the
 * strongest of them (level {@value #LEVEL}, {@value #YAMA_HITS} hits) and the zone's climax, in his
 * great hall at the head of the avenue. A {@link KingOfHellEntity} whose fight comes in three
 * phases ({@link #phase()}):
 * <ol start="0">
 *   <li><b>the Writ</b>: besides his tablet's blows, every few seconds he writes his foe's name in
 *       the register: they glow, and every creature of Hell near them comes for them;</li>
 *   <li>from {@value #COURT_AT_PERCENT}%: <b>his court</b> rises (Ox-Head, Horse-Face and a clerk at
 *       his side), and he passes <b>sentence</b>: a ring of soul fire marks where his foe stands, and
 *       a moment later judgement falls there, a third of the health of whoever is still in it, and
 *       they burn; step out of it;</li>
 *   <li>from {@value #WRATH_AT_PERCENT}%: <b>the wrath of Yama</b>: he grows, he strikes harder and
 *       moves quicker, sentences fall faster, and soul fire burns round him, scorching whoever
 *       stands close.</li>
 * </ol>
 */
public class KingYamaEntity extends KingOfHellEntity {

    public static final int LEVEL = 66;
    /** A hundred and forty of a level-66 character's ordinary hits to kill; his blows a sixth of their health. */
    public static final double YAMA_HITS = 140, YAMA_BLOW = 0.17;
    public static final int COURT_AT_PERCENT = 66, WRATH_AT_PERCENT = 33;
    /** His hall: he keeps within this of his throne. */
    public static final int GREAT_HALL = 18;
    /** The Writ: how often. */
    public static final int WRIT_EVERY = 200;
    /** A sentence: how long between its mark and its fall; how wide; what share of health it takes; how often, and in his wrath. */
    public static final int SENTENCE_DELAY = 30, SENTENCE_EVERY = 120, WRATH_SENTENCE_EVERY = 70;
    public static final double SENTENCE_RADIUS = 2.5, SENTENCE_SHARE = 1 / 3.0;
    /** His wrath: how much larger, how much stronger and quicker; the soul fire round him, how near and how hot. */
    public static final float WRATH_SCALE = 1.35f;
    public static final double WRATH_POWER = 0.3, AURA = 3.5, AURA_SHARE = 0.25;

    private static final EntityDataAccessor<Boolean> WRATH = SynchedEntityData.defineId(KingYamaEntity.class, EntityDataSerializers.BOOLEAN);

    /** A sentence passed: where it falls, and when. */
    public record Sentence(Vec3 at, long due) {}

    private final List<Sentence> sentences = new ArrayList<>();
    private int writ = WRIT_EVERY / 2, sentence = SENTENCE_EVERY / 2;

    public KingYamaEntity(EntityType<? extends KingYamaEntity> type, Level level) {
        super(type, level);
        xpReward = 800;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, YAMA_HITS * com.jttw.engine.entity.MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, YAMA_BLOW * com.jttw.engine.entity.MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 14)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public int court() {
        return 5;
    }

    @Override
    public int kingLevel() {
        return LEVEL;
    }

    @Override
    public int lairRadius() {
        return GREAT_HALL;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.PURPLE;
    }

    @Override
    public double bossBarRange() {
        return 40;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{COURT_AT_PERCENT / 100f, WRATH_AT_PERCENT / 100f};
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) callCourt();
        if (phase == 2) wrath();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WRATH, false);
    }

    /** Whether his wrath is on him (his last phase). */
    public boolean inWrath() {
        return entityData.get(WRATH);
    }

    @Override
    public float getScale() {
        return inWrath() ? WRATH_SCALE : 1f;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WRATH.equals(key)) refreshDimensions();
    }

    // ── The Writ ────────────────────────────────────────────────────────

    /** He writes {@code target}'s name in the register: marked, as a clerk marks, and Hell's creatures near come for it. Returns how many came. */
    public int writ(LivingEntity target) {
        target.getPersistentData().putLong(JudgeClerkEntity.KEY_MARKED, level().getGameTime() + JudgeClerkEntity.MARKED);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,
                JudgeClerkEntity.MARKED, 0, false, false), this);
        int roused = 0;
        for (HellMonster m : level().getEntitiesOfClass(HellMonster.class, target.getBoundingBox().inflate(32), LivingEntity::isAlive)) {
            if (m.getTarget() != null && m.getTarget().isAlive()) continue;
            m.setTarget(target);
            if (m.getTarget() == target) roused++;
        }
        showJudging();
        playSound(SoundEvents.BOOK_PAGE_TURN, 2f, 0.5f);
        if (target instanceof ServerPlayer p) p.displayClientMessage(Component.translatable("entity.jttw_engine.king_yama.writ"), true);
        return roused;
    }

    // ── His court ───────────────────────────────────────────────────────

    public static final String KEY_COURT = "jttw_yama_court";

    /** His court rises round him: Ox-Head, Horse-Face and a clerk. Returns them. */
    public List<HellMonster> callCourt() {
        List<HellMonster> court = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return court;
        List<EntityType<? extends HellMonster>> who = List.of(ModEntities.OX_HEAD.get(), ModEntities.HORSE_FACE.get(), ModEntities.JUDGE_CLERK.get());
        for (int i = 0; i < who.size(); i++) {
            double a = i * Math.PI * 2 / who.size() + 0.4;
            HellMonster m = ZoneMonster.summon(server, who.get(i), BlockPos.containing(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3), false);
            if (m == null) continue;
            m.getPersistentData().putBoolean(KEY_COURT, true);
            if (getTarget() != null) m.setTarget(getTarget());
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, m.getX(), m.getY(0.5), m.getZ(), 30, 0.3, 0.8, 0.3, 0.03);
            court.add(m);
        }
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2f, 0.6f);
        return court;
    }

    // ── Sentence ────────────────────────────────────────────────────────

    /** Passes sentence on the ground at {@code at}: marked now, it falls {@value #SENTENCE_DELAY} ticks later. */
    public Sentence sentence(Vec3 at) {
        Sentence s = new Sentence(at, level().getGameTime() + SENTENCE_DELAY);
        sentences.add(s);
        showJudging();
        playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.5f, 0.7f);
        return s;
    }

    /** The sentences passed and not yet fallen. */
    public List<Sentence> pending() {
        return List.copyOf(sentences);
    }

    /** Lets a sentence fall: whoever (not of Hell) still stands within it loses a third of their health, and burns. Returns how many. */
    public int fall(Sentence s) {
        int n = 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(s.at(), s.at()).inflate(SENTENCE_RADIUS, 2, SENTENCE_RADIUS),
                e -> e.isAlive() && !(e instanceof HellMonster) && !(e instanceof KingOfHellEntity)
                        && e.position().multiply(1, 0, 1).distanceTo(s.at().multiply(1, 0, 1)) <= SENTENCE_RADIUS)) {
            if (e instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) continue;
            e.invulnerableTime = 0;
            e.hurt(damageSources().indirectMagic(this, this), (float) (e.getMaxHealth() * SENTENCE_SHARE));
            e.igniteForTicks(60);
            n++;
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, s.at().x, s.at().y + 0.2, s.at().z, 80, SENTENCE_RADIUS / 2, 1.2, SENTENCE_RADIUS / 2, 0.08);
            server.sendParticles(ParticleTypes.EXPLOSION, s.at().x, s.at().y + 0.5, s.at().z, 2, 0.5, 0.3, 0.5, 0);
        }
        level().playSound(null, s.at().x, s.at().y, s.at().z, SoundEvents.LIGHTNING_BOLT_IMPACT, getSoundSource(), 1.5f, 0.6f);
        return n;
    }

    private void markRing(Sentence s, ServerLevel server) {
        for (int k = 0; k < 20; k++) {
            double a = k * Math.PI / 10;
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, s.at().x + Math.cos(a) * SENTENCE_RADIUS, s.at().y + 0.1,
                    s.at().z + Math.sin(a) * SENTENCE_RADIUS, 1, 0, 0.02, 0, 0);
        }
    }

    // ── Wrath ───────────────────────────────────────────────────────────

    private static final net.minecraft.resources.ResourceLocation WRATH_POWER_ID = JTTWEngine.id("yama_wrath_power"),
            WRATH_SPEED_ID = JTTWEngine.id("yama_wrath_speed");

    /** His wrath comes on him: larger, stronger, quicker. */
    public void wrath() {
        if (inWrath()) return;
        entityData.set(WRATH, true);
        refreshDimensions();
        var power = getAttribute(Attributes.ATTACK_DAMAGE);
        if (power != null && power.getModifier(WRATH_POWER_ID) == null) power.addPermanentModifier(
                new AttributeModifier(WRATH_POWER_ID, WRATH_POWER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(WRATH_SPEED_ID) == null) speed.addPermanentModifier(
                new AttributeModifier(WRATH_SPEED_ID, WRATH_POWER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        playSound(SoundEvents.WITHER_SPAWN, 1.5f, 0.6f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(1), getZ(), 150, 1.2, 1.5, 1.2, 0.1);
            for (ServerPlayer p : server.players()) {
                if (p.distanceTo(this) < 40) p.sendSystemMessage(Component.translatable("entity.jttw_engine.king_yama.wrath"));
            }
        }
    }

    /** The soul fire round him in his wrath: whoever stands close is scorched. Returns how many. */
    public int scorch() {
        int n = 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(AURA),
                e -> e.isAlive() && e != this && !(e instanceof HellMonster) && canAttack(e))) {
            if (e.distanceTo(this) > AURA + getBbWidth() / 2) continue;
            e.invulnerableTime = 0;
            e.hurt(damageSources().inFire(), (float) (blow() * AURA_SHARE));
            n++;
        }
        return n;
    }

    @Override
    public void tick() {
        super.tick();
        // in his wrath soul fire wreathes him
        if (level().isClientSide && inWrath()) {
            for (int i = 0; i < 2; i++) level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getRandomX(1.2), getRandomY(), getRandomZ(1.2), 0, 0.03, 0);
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    // ── His judgement, every tick ───────────────────────────────────────

    /** His court's judgement is the Writ (and, from his second phase, sentence). */
    @Override
    public boolean judge(LivingEntity target) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) return;
        long now = level().getGameTime();
        LivingEntity t = getTarget();
        if (writ > 0) writ--;
        if (sentence > 0) sentence--;
        if (t != null && t.isAlive() && distanceTo(t) <= JUDGES_WITHIN + 6) {
            if (writ == 0) {
                writ(t);
                writ = WRIT_EVERY;
            }
            if (phase() >= 1 && sentence == 0) {
                sentence(t.position());
                sentence = inWrath() ? WRATH_SENTENCE_EVERY : SENTENCE_EVERY;
            }
        }
        for (Sentence s : List.copyOf(sentences)) {
            if (now >= s.due()) {
                sentences.remove(s);
                fall(s);
            } else if (now % 4 == 0) markRing(s, server);
        }
        if (inWrath() && tickCount % 20 == 0) {
            scorch();
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.1, getZ(), 30, AURA / 2, 0.1, AURA / 2, 0.01);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Wrath", inWrath());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Wrath")) entityData.set(WRATH, true);
    }
}
