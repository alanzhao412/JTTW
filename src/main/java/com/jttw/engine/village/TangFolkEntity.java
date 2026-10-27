package com.jttw.engine.village;

import com.jttw.engine.registry.ModVillages;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BedBlock;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * The folk of the Tang villages, and the merchants of the supernatural world: one creature, of two
 * kinds ({@code tang_villager}, mortal; {@code spirit_merchant}), each following a {@link Calling}
 * and trading by it in copper and silver ({@link TangTrades}) through the vanilla trading screen.
 *
 * <p>A mortal lives as a villager does: wanders the village by day, goes home to its bed at night
 * and sleeps there, flees monsters (and, from {@value YaoguaiFear#RADIUS} blocks, an undisguised
 * Yaoguai: {@link YaoguaiFear}), panics when struck. It will not trade with a demon. Killing one is
 * killing a villager (karma {@link Morality#KILL_VILLAGER}). The village's folk are put there as it
 * is made, and never spawn of themselves.
 *
 * <p>A spirit merchant keeps to its stall, never sleeps and fears no one: the ghost market's dead
 * shopkeepers deal with demons as with anyone; Heaven's merchant-immortal deals with anyone Heaven
 * does not hunt ({@link Morality#wanted}). Each restocks every morning.
 */
public class TangFolkEntity extends AbstractVillager {

    private static final EntityDataAccessor<Integer> CALLING = SynchedEntityData.defineId(TangFolkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> WOMAN = SynchedEntityData.defineId(TangFolkEntity.class, EntityDataSerializers.BOOLEAN);

    /** How far from home a villager wanders; a spirit merchant from its stall. */
    public static final int VILLAGE_RANGE = 40, STALL_RANGE = 3;

    @Nullable
    private BlockPos bed;
    private long stockedDay = Long.MIN_VALUE;

    public TangFolkEntity(EntityType<? extends TangFolkEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    /** Whether this is a mortal of the villages (else a merchant of the supernatural world). */
    public boolean isMortal() {
        return getType() == ModVillages.TANG_VILLAGER.get();
    }

    public Calling calling() {
        return Calling.byOrdinal(entityData.get(CALLING));
    }

    public boolean isWoman() {
        return entityData.get(WOMAN);
    }

    /** Gives it its calling (a merchant's offers are made anew), and whether a woman follows it. */
    public void setCalling(Calling calling, boolean woman) {
        entityData.set(CALLING, calling.ordinal());
        entityData.set(WOMAN, woman && calling.mayBeWoman());
        offers = null;
    }

    @Nullable
    public BlockPos bed() {
        return bed;
    }

    /** Settles it: its calling, its bed (null for none), and the place it keeps near. */
    public void settle(Calling calling, boolean woman, @Nullable BlockPos bed, BlockPos home) {
        setCalling(calling, woman);
        this.bed = bed == null ? null : bed.immutable();
        restrictTo(home, isMortal() ? VILLAGE_RANGE : STALL_RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CALLING, getType() == ModVillages.SPIRIT_MERCHANT.get() ? Calling.GHOST_MERCHANT.ordinal() : Calling.FARMER.ordinal());
        builder.define(WOMAN, false);
    }

    @Override
    protected void registerGoals() {
        boolean mortal = isMortal();
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        if (mortal) {
            goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, YaoguaiFear.RADIUS, 0.6, 0.75,
                    e -> !isSleeping() && YaoguaiFear.frightens(this, e)));
            goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Mob.class, 8f, 0.6, 0.75,
                    e -> !isSleeping() && e instanceof Enemy && !(e instanceof TangFolkEntity)));
            goalSelector.addGoal(1, new PanicGoal(this, 0.75));
            goalSelector.addGoal(2, new SleepInBedGoal(this));
        }
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.5));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, mortal ? 0.45 : 0.3));
        goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3f, 1f));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        // one summoned or hatched from an egg follows a calling of its kind, at random
        if (reason != MobSpawnType.STRUCTURE && isMortal()) {
            RandomSource r = level.getRandom();
            Calling c = Calling.byOrdinal(r.nextInt(8));
            setCalling(c, r.nextBoolean());
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ── Trading ──────────────────────────────────────────────────────

    /** Why it will not trade with {@code player} (a line to show them), or null if it will. */
    @Nullable
    public Component refusal(Player player) {
        if (isSleeping()) return Component.translatable("entity.jttw_engine.tang_villager.asleep", getDisplayName());
        if (isMortal() && YaoguaiFear.dreaded(player)) return Component.translatable("entity.jttw_engine.tang_villager.refuse_demon", getDisplayName());
        if (calling() == Calling.CELESTIAL_MERCHANT && Morality.wanted(player) > 0) {
            return Component.translatable("entity.jttw_engine.spirit_merchant.refuse_outlaw");
        }
        return null;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading() || isBaby()) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        Component no = refusal(player);
        if (no != null) {
            player.displayClientMessage(no, true);
            playSound(SoundEvents.VILLAGER_NO, 0.6f, 0.8f);
            return InteractionResult.CONSUME;
        }
        if (getOffers().isEmpty()) return InteractionResult.CONSUME;
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName(), 1);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void updateTrades() {
        ServerLevel server = level() instanceof ServerLevel s ? s : null;
        for (TangTrades.Deal deal : TangTrades.of(calling())) {
            MerchantOffer offer = VillageOffers.offer(deal, server, position());
            if (offer != null) offers.add(offer);
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) level().addFreshEntity(new ExperienceOrb(level(), getX(), getY() + 0.5, getZ(), 2 + random.nextInt(4)));
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    /** Every morning its stock is renewed. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long day = level().getDayTime() / 24000L;
        if (day != stockedDay) {
            if (offers != null) offers.forEach(MerchantOffer::resetUses);
            stockedDay = day;
        }
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.jttw_engine." + (isMortal() ? "tang_villager." : "spirit_merchant.") + calling().id());
    }

    // ── Being a mortal ───────────────────────────────────────────────

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isMortal() ? SoundEvents.PLAYER_HURT : SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isMortal() ? SoundEvents.PLAYER_DEATH : SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.CHAIN_PLACE;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean yes) {
        return yes ? SoundEvents.CHAIN_STEP : SoundEvents.VILLAGER_NO;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("calling", calling().id());
        tag.putBoolean("woman", isWoman());
        if (bed != null) tag.put("bed", NbtUtils.writeBlockPos(bed));
        if (hasRestriction()) {
            tag.put("home", NbtUtils.writeBlockPos(getRestrictCenter()));
            tag.putInt("home_range", (int) getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("calling")) {
            entityData.set(CALLING, Calling.byId(tag.getString("calling")).ordinal());
            entityData.set(WOMAN, tag.getBoolean("woman"));
        }
        bed = NbtUtils.readBlockPos(tag, "bed").orElse(null);
        NbtUtils.readBlockPos(tag, "home").ifPresent(home -> restrictTo(home, tag.getInt("home_range")));
    }

    /** At night a mortal walks home to its bed and sleeps till morning (struck, it wakes). */
    static final class SleepInBedGoal extends Goal {
        private final TangFolkEntity folk;
        private int repath;

        SleepInBedGoal(TangFolkEntity folk) {
            this.folk = folk;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private boolean bedtime() {
            BlockPos bed = folk.bed;
            return bed != null && folk.level().isNight() && !folk.isTrading()
                    && folk.level().getBlockState(bed).getBlock() instanceof BedBlock
                    && folk.distanceToSqr(bed.getCenter()) < 64 * 64;
        }

        @Override
        public boolean canUse() {
            return bedtime() && folk.getLastHurtByMob() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return bedtime();
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            if (folk.isSleeping()) {
                folk.getNavigation().stop();
                return;
            }
            BlockPos bed = folk.bed;
            if (folk.distanceToSqr(bed.getCenter()) < 2.6) {
                folk.getNavigation().stop();
                folk.startSleeping(bed);
            } else if (--repath <= 0) {
                repath = 20;
                folk.getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 0.5);
            }
        }

        @Override
        public void stop() {
            if (folk.isSleeping()) folk.stopSleeping();
        }
    }

    /** Whether a creature is one this one would flee: a monster, or a dreaded demon. */
    public boolean wouldFlee(LivingEntity e) {
        return e instanceof Enemy || YaoguaiFear.frightens(this, e);
    }
}
