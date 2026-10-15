package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.AlignmentEarningHandler;
import com.jttw.engine.reincarnation.ReincarnationHandler;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;

/** Earning alignment before the first death, and reincarnating from it. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ReincarnationGameTests {

    // ── Earning alignment ─────────────────────────────────────────

    private static void breakBlock(GameTestHelper h, ServerPlayer p, BlockState state) {
        AlignmentEarningHandler.onBlockBreak(new BlockEvent.BreakEvent(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), state, p));
    }

    private static void kill(GameTestHelper h, ServerPlayer p, EntityType<? extends LivingEntity> type) {
        LivingEntity victim = h.spawn(type, new BlockPos(3, 2, 3));
        AlignmentEarningHandler.onEntityKill(new LivingDeathEvent(victim, h.getLevel().damageSources().playerAttack(p)));
        victim.discard();
    }

    @GameTest(template = PLATFORM)
    public static void miningLogsOresAndDebrisEarnsAlignment(GameTestHelper h) {
        ServerPlayer p = player(h);
        breakBlock(h, p, Blocks.OAK_LOG.defaultBlockState());
        eq(h, 1, AlignmentData.getSpiritBeast(p), "log");
        breakBlock(h, p, Blocks.DEEPSLATE_IRON_ORE.defaultBlockState());
        eq(h, 4, AlignmentData.getSpiritBeast(p), "log + iron ore");
        breakBlock(h, p, Blocks.DIAMOND_ORE.defaultBlockState());
        eq(h, 9, AlignmentData.getSpiritBeast(p), "log + iron + diamond");
        breakBlock(h, p, Blocks.ANCIENT_DEBRIS.defaultBlockState());
        eq(h, 15, AlignmentData.getPrimordial(p), "ancient debris");
        breakBlock(h, p, Blocks.STONE.defaultBlockState());
        eq(h, 24, AlignmentData.getTotal(p), "stone earns nothing");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void killingUndeadIsHeavenlyAndInnocentsAreYaoguai(GameTestHelper h) {
        ServerPlayer p = player(h);
        kill(h, p, EntityType.ZOMBIE);
        eq(h, 2, AlignmentData.getHeavenly(p), "zombie");
        kill(h, p, EntityType.COW);
        eq(h, 2, AlignmentData.getYaoguai(p), "cow");
        kill(h, p, EntityType.VILLAGER);
        eq(h, 17, AlignmentData.getYaoguai(p), "cow + villager");
        kill(h, p, EntityType.IRON_GOLEM);
        eq(h, 27, AlignmentData.getYaoguai(p), "cow + villager + golem");
        kill(h, p, EntityType.CREEPER);
        eq(h, 29, AlignmentData.getTotal(p), "creeper (not undead) earns nothing");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wardenKillIsWorthTwoHundredPrimordial(GameTestHelper h) {
        ServerPlayer p = player(h);
        kill(h, p, EntityType.WARDEN);
        eq(h, 200, AlignmentData.getPrimordial(p), "warden");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void killsNotByAPlayerEarnNothing(GameTestHelper h) {
        ServerPlayer p = player(h);
        LivingEntity zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        AlignmentEarningHandler.onEntityKill(new LivingDeathEvent(zombie, h.getLevel().damageSources().generic()));
        eq(h, 0, AlignmentData.getTotal(p), "total");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void eatingFoodIsYaoguaiButOtherItemsAreNot(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentEarningHandler.onEatFood(new LivingEntityUseItemEvent.Finish(p, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY));
        eq(h, 1, AlignmentData.getYaoguai(p), "bread");
        AlignmentEarningHandler.onEatFood(new LivingEntityUseItemEvent.Finish(p, new ItemStack(Items.BOW), 0, ItemStack.EMPTY));
        eq(h, 1, AlignmentData.getYaoguai(p), "bow is not food");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void explorationAdvancementsArePrimordial(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentEarningHandler.onAdvancement(new AdvancementEvent.AdvancementEarnEvent(p, advancement(h, "adventure/root")));
        eq(h, 5, AlignmentData.getPrimordial(p), "adventure tab");
        AlignmentEarningHandler.onAdvancement(new AdvancementEvent.AdvancementEarnEvent(p, advancement(h, "nether/find_fortress")));
        eq(h, 20, AlignmentData.getPrimordial(p), "nether tab + major milestone");
        AlignmentEarningHandler.onAdvancement(new AdvancementEvent.AdvancementEarnEvent(p, advancement(h, "story/mine_stone")));
        eq(h, 20, AlignmentData.getPrimordial(p), "story tab earns nothing");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void alignmentStopsAccumulatingAfterReincarnation(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.DRAGON);
        breakBlock(h, p, Blocks.OAK_LOG.defaultBlockState());
        kill(h, p, EntityType.ZOMBIE);
        eq(h, 0, AlignmentData.getTotal(p), "total");
        h.succeed();
    }

    // ── Death and rebirth ─────────────────────────────────────────

    private static AdvancementHolder advancement(GameTestHelper h, String path) {
        AdvancementHolder a = h.getLevel().getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath("minecraft", path));
        if (a == null) throw new IllegalStateException("missing advancement " + path);
        return a;
    }

    /**
     * Marks "We Need to Go Deeper" done. Forge's PlayerAdvancements.award refuses fake players,
     * so the criteria are granted on the progress record directly.
     */
    private static void enterNether(GameTestHelper h, ServerPlayer p) {
        AdvancementHolder adv = advancement(h, "story/enter_the_nether");
        var progress = p.getAdvancements().getOrStartProgress(adv);
        for (String criterion : adv.value().criteria().keySet()) progress.grantProgress(criterion);
        check(h, progress.isDone(), "Nether advancement done");
    }

    private static void die(GameTestHelper h, ServerPlayer p) {
        ReincarnationHandler.onPlayerDeath(new LivingDeathEvent(p, h.getLevel().damageSources().generic()));
    }

    private static ServerPlayer respawn(GameTestHelper h, ServerPlayer dead) {
        ServerPlayer reborn = player(h);
        ReincarnationHandler.onPlayerClone(new PlayerEvent.Clone(reborn, dead, true));
        return reborn;
    }

    private static void earnHeavenlyLed(ServerPlayer p) {
        AlignmentData.setHeavenly(p, 200);
        AlignmentData.setPrimordial(p, 100);
        AlignmentData.setYaoguai(p, 20);
    }

    @GameTest(template = PLATFORM)
    public static void deathAfterTheNetherWithEnoughAlignmentReincarnates(GameTestHelper h) {
        ServerPlayer p = player(h);
        enterNether(h, p);
        earnHeavenlyLed(p); // 320 total, threshold 300; heavenly then primordial
        die(h, p);
        ServerPlayer reborn = respawn(h, p);
        check(h, AlignmentData.hasReincarnated(reborn), "reborn player is marked reincarnated");
        eq(h, ReincarnationOutcome.EYE_OF_HEAVEN.id,
                reborn.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME), "outcome");
        eq(h, ProgressionClass.HEAVENLY, PlayerLevel.getProgressionClass(reborn), "class initialised");
        eq(h, 1, PlayerLevel.getLevel(reborn), "starts at level 1");
        eq(h, 0, AlignmentData.getTotal(reborn), "alignment reset for the new life");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void deathBeforeTheNetherDoesNotReincarnate(GameTestHelper h) {
        ServerPlayer p = player(h);
        earnHeavenlyLed(p);
        die(h, p);
        check(h, !AlignmentData.hasReincarnated(respawn(h, p)), "no Nether advancement yet");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void deathBelowTheThresholdDoesNotReincarnate(GameTestHelper h) {
        ServerPlayer p = player(h);
        enterNether(h, p);
        AlignmentData.setHeavenly(p, 299); // threshold is 300 total
        die(h, p);
        check(h, !AlignmentData.hasReincarnated(respawn(h, p)), "299 of 300");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void reincarnationHappensOnlyOnce(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.TURTLE);
        enterNether(h, p);
        earnHeavenlyLed(p);
        die(h, p);
        ServerPlayer reborn = respawn(h, p);
        check(h, reborn.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME).isEmpty(),
                "second death does not pick a new outcome");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void respawnWithoutDyingKeepsThePendingRebirth(GameTestHelper h) {
        ServerPlayer p = player(h);
        enterNether(h, p);
        earnHeavenlyLed(p);
        die(h, p);
        // returning from the End also clones the player, but is not a death
        ServerPlayer returned = player(h);
        ReincarnationHandler.onPlayerClone(new PlayerEvent.Clone(returned, p, false));
        check(h, !AlignmentData.hasReincarnated(returned), "not a death, no rebirth");
        check(h, AlignmentData.hasReincarnated(respawn(h, p)), "the actual death still reincarnates");
        h.succeed();
    }
}
