package com.jttw.engine.entity.wuzhuang;

import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.wuzhuang.Wuzhuang;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One of the immortals of the Temple of the Five Villages (Zhenyuan, and his senior disciples
 * Clear Wind and Bright Moon): a {@link ZoneBoss} whose fight is a <b>trial</b>, not a killing.
 * <ul>
 *   <li>righteous: they fight no one unprovoked but a thief of the fruit;</li>
 *   <li>brought down to {@value #YIELD_PERCENT}% of their health they <b>yield</b>: they stop
 *       fighting, cannot be harmed, and acknowledge whoever fought them in the last
 *       {@value #CHALLENGER_MEMORY} ticks: each such player's reward ({@link #reward}: the trial's
 *       XP, as a kill of their tier would pay; their loot table; and, the first time, their own
 *       gift), at most once in {@value #REWARD_EVERY} ticks a player;</li>
 *   <li>after {@value #REST} ticks they rise whole, ready to be challenged again. No blow kills
 *       them (only what kills outright, as /kill, does).</li>
 * </ul>
 */
public abstract class TempleImmortal extends ZoneBoss implements TempleFolk {

    public static final int YIELD_PERCENT = 10;
    /** How long they rest yielded, how long a challenger is remembered, how often a player may be rewarded. */
    public static final int REST = 6000, CHALLENGER_MEMORY = 600, REWARD_EVERY = 24000;
    public static final String KEY_TRIAL = "jttw_trial_";

    private static final EntityDataAccessor<Boolean> YIELDED = SynchedEntityData.defineId(TempleImmortal.class, EntityDataSerializers.BOOLEAN);

    private long restUntil;
    private final Map<UUID, Long> challengers = new HashMap<>();
    private final Map<UUID, ServerPlayer> challengerRefs = new HashMap<>();

    protected TempleImmortal(EntityType<? extends TempleImmortal> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    // ── What a subclass says ────────────────────────────────────────────

    /** Its level. */
    public abstract int immortalLevel();

    /** The tier its trial pays as. */
    protected abstract EnemyTier trialTier();

    /** Its own gift, given to whoever passes its trial the first time. */
    protected abstract void firstGift(ServerPlayer p);

    /** The largest share of its health one blow can take. */
    protected double hitCap() {
        return 1.0;
    }

    /** What else passing its trial means for {@code p} (Zhenyuan's leave). */
    protected void onPassed(ServerPlayer p, boolean first) {}

    @Override
    public Zones.Zone zone() {
        return Zones.MOUNT_WUZHUANG;
    }

    // ── What it is ──────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(YIELDED, false);
    }

    public boolean isYielded() {
        return entityData.get(YIELDED);
    }

    @Override
    public boolean showsBossBar() {
        return !isYielded();
    }

    @Override
    protected Component getTypeName() {
        return Component.empty().append(super.getTypeName()).append(" · Lv " + immortalLevel())
                .withStyle(s -> s.withColor(TextColor.fromRgb(0xF0D060)));
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    /** Fights no one unprovoked but a thief: these, with its own goals. */
    protected void addTrialTargets() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> p instanceof Player pl && Wuzhuang.isThief(pl)));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && isYielded()) return;
        super.setTarget(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !isYielded() && super.doHurtTarget(target);
    }

    // ── The trial ───────────────────────────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        boolean outright = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (isYielded() && !outright) return false;
        if (!outright) amount = (float) Math.min(amount, getMaxHealth() * hitCap());
        if (source.getEntity() instanceof ServerPlayer p && !isYielded()) {
            challengers.put(p.getUUID(), level().getGameTime());
            challengerRefs.put(p.getUUID(), p);
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        super.actuallyHurt(source, amount);
        if (level().isClientSide || isYielded() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (getHealth() <= getMaxHealth() * YIELD_PERCENT / 100f) {
            setHealth(Math.max(1f, getHealth()));
            yieldTrial();
        }
    }

    /** The players it remembers fighting it lately. */
    public List<ServerPlayer> challengers() {
        List<ServerPlayer> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return out;
        long now = level().getGameTime();
        for (Map.Entry<UUID, Long> e : challengers.entrySet()) {
            if (now - e.getValue() > CHALLENGER_MEMORY) continue;
            ServerPlayer p = server.getPlayerByUUID(e.getKey()) instanceof ServerPlayer live ? live : challengerRefs.get(e.getKey());
            if (p != null) out.add(p);
        }
        return out;
    }

    /** It yields: the fight is over, and those who fought it are acknowledged. */
    public void yieldTrial() {
        if (isYielded()) return;
        entityData.set(YIELDED, true);
        restUntil = level().getGameTime() + REST;
        super.setTarget(null);
        getNavigation().stop();
        removeAllEffects();
        onYield();
        playSound(SoundEvents.BELL_RESONATE, 1.5f, 1.2f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.6), getZ(), 40, 0.6, 0.8, 0.6, 0.05);
            for (ServerPlayer p : challengers()) reward(p);
            for (ServerPlayer p : server.players()) {
                if (p.distanceTo(this) < 48) p.sendSystemMessage(Component.translatable(getType().getDescriptionId() + ".yields"));
            }
        }
        challengers.clear();
        challengerRefs.clear();
    }

    /** Called as it yields, before its challengers are rewarded. */
    protected void onYield() {}

    /** Whether {@code p} may be rewarded by this one's trial now (first time, or a day since the last). */
    public boolean mayReward(Player p) {
        CompoundTag d = p.getPersistentData();
        String key = KEY_TRIAL + trialName();
        return !d.getBoolean(key + "_passed") || level().getGameTime() - d.getLong(key) >= REWARD_EVERY;
    }

    /** Whether {@code p} has ever passed this one's trial. */
    public boolean passedBy(Player p) {
        return p.getPersistentData().getBoolean(KEY_TRIAL + trialName() + "_passed");
    }

    private String trialName() {
        return getType().builtInRegistryHolder().key().location().getPath();
    }

    /** Acknowledges {@code p}: the XP of its tier, its loot, its gift the first time. Returns whether anything was given. */
    public boolean reward(ServerPlayer p) {
        if (!mayReward(p)) {
            p.displayClientMessage(Component.translatable("entity.jttw_engine.temple_immortal.already"), true);
            return false;
        }
        String key = KEY_TRIAL + trialName();
        boolean first = !p.getPersistentData().getBoolean(key + "_passed");
        p.getPersistentData().putBoolean(key + "_passed", true);
        p.getPersistentData().putLong(key, level().getGameTime());
        if (first) firstGift(p);
        for (ItemStack stack : rollLoot(p)) give(p, stack);
        BiomeLevelBand band = zone().levelBand();
        if (PlayerLevel.getProgressionClass(p) != null && band.isRewardingForLevel(PlayerLevel.getLevel(p))) {
            LevelManager.grantXp(p, band.xpForTier(trialTier()), band.biome() + ":trial");
        }
        onPassed(p, first);
        return true;
    }

    /** Puts {@code stack} in {@code p}'s inventory, or at their feet. */
    public static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }

    /** Its loot table, rolled for {@code p} as though they had struck it down. */
    public List<ItemStack> rollLoot(ServerPlayer p) {
        if (!(level() instanceof ServerLevel server)) return List.of();
        LootTable table = server.getServer().reloadableRegistries().getLootTable(getLootTable());
        DamageSource source = damageSources().playerAttack(p);
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, p)
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, p)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, p)
                .withLuck(p.getLuck())
                .create(LootContextParamSets.ENTITY);
        return table.getRandomItems(params);
    }

    /** It rises whole from its rest, ready to be challenged again. */
    public void rise() {
        entityData.set(YIELDED, false);
        setHealth(getMaxHealth());
        resetPhase();
        challengers.clear();
        challengerRefs.clear();
    }

    /** For tests: when its rest ends. */
    public long restUntil() {
        return restUntil;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (isYielded() && tickCount % 10 == 0) level().addParticle(ParticleTypes.END_ROD, getRandomX(0.6), getY(1.1), getRandomZ(0.6), 0, 0.02, 0);
            return;
        }
        if (isYielded()) {
            if (level().getGameTime() >= restUntil) rise();
            else if (tickCount % 20 == 0) heal(getMaxHealth() * 0.01f);
        } else if (tickCount % 20 == 0 && getTarget() == null && challengers().isEmpty() && getHealth() < getMaxHealth()) {
            heal(getMaxHealth() * 0.01f); // at peace, it mends
        }
    }

    /** Spoken to: a word, as befits the one it is. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            String key = getType().getDescriptionId() + (isYielded() ? ".resting" : Wuzhuang.isThief(player) ? ".thief" : ".greet");
            player.displayClientMessage(Component.translatable(key), false);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Yielded", isYielded());
        tag.putLong("RestUntil", restUntil);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(YIELDED, tag.getBoolean("Yielded"));
        restUntil = tag.getLong("RestUntil");
    }
}
