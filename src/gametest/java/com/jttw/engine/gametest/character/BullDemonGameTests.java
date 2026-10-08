package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.bulldemon.BullDemonAbilityHandler;
import com.jttw.engine.character.bulldemon.BullDemonCharacter;
import com.jttw.engine.item.BananaLeafFanItem;
import com.jttw.engine.item.MixedIronCudgelItem;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** The Bull Demon King: rush, great bull, thick hide, fan and cudgel. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class BullDemonGameTests {

    /** The Bull Demon King at (2,2,2) facing south (+z), with Qi to spend. */
    private static ServerPlayer bull(GameTestHelper h, String... nodes) {
        ServerPlayer p = as(h, ReincarnationOutcome.BULL_DEMON_KING);
        p.setYRot(0f); p.setXRot(0f); p.yHeadRot = 0f;
        qi(p, 200f, 200f);
        grant(p, nodes);
        return p;
    }

    @GameTest(template = PLATFORM)
    public static void theOutcomeIsTheBullWithRushTransformsAndTheGreatBull(GameTestHelper h) {
        ServerPlayer p = bull(h);
        eq(h, BullDemonCharacter.INSTANCE, Characters.of(p), "character");
        List<AbilitySlot> bar = BullDemonCharacter.INSTANCE.skillBar(Set.of("bdk_rush_1", "bdk_transform_1", "bdk_bull_1"));
        eq(h, AbilityType.HELD, bar.get(0).type(), "V: rush, held");
        eq(h, AbilityType.MENU, bar.get(1).type(), "B: the 72 Transformations");
        check(h, bar.get(2).isEmpty(), "N empty");
        eq(h, "bdk_bull", bar.get(3).id(), "M: the Great White Bull");
        h.succeed();
    }

    // ── V: Bull Rush ───────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theRushChargesAndGoresWhatIsInTheWayOnce(GameTestHelper h) {
        ServerPlayer p = bull(h, "bdk_rush_1");
        Zombie z = mob(h, EntityType.ZOMBIE, 2, 3);
        List<Hit> log = watch(z);
        BullDemonCharacter.INSTANCE.activate(p, 0, AbilityAction.START);
        check(h, BullDemonAbilityHandler.isRushing(p), "charging");
        BullDemonAbilityHandler.tickRush(p);
        near(h, BullDemonAbilityHandler.rushSpeed(1), p.getDeltaMovement().z, "driven forward");
        Hit gore = firstHit(h, log, DamageTypes.PLAYER_ATTACK, z);
        near(h, BullDemonAbilityHandler.rushDamage((float) p.getAttributeValue(Attributes.ATTACK_DAMAGE), 1), gore.amount(), "gored");
        check(h, z.getDeltaMovement().z > 0.5, "tossed ahead");
        z.invulnerableTime = 0;
        BullDemonAbilityHandler.tickRush(p);
        eq(h, 1, of(log, DamageTypes.PLAYER_ATTACK).size(), "gored once per rush");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theRushSteersWhereHeLooks(GameTestHelper h) {
        ServerPlayer p = bull(h, "bdk_rush_1");
        BullDemonAbilityHandler.startRush(p);
        p.setYRot(90f); // west
        BullDemonAbilityHandler.tickRush(p);
        check(h, p.getDeltaMovement().x < -0.8 && Math.abs(p.getDeltaMovement().z) < 0.05, "turned west: " + p.getDeltaMovement());
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void rammingAWallStunsThoseNearAndEndsTheRush(GameTestHelper h) {
        ServerPlayer p = bull(h, "bdk_rush_1");
        h.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
        h.setBlock(new BlockPos(2, 3, 3), Blocks.STONE);
        Zombie pinned = mob(h, EntityType.ZOMBIE, 3, 2);
        BullDemonAbilityHandler.startRush(p);
        BullDemonAbilityHandler.tickRush(p);
        check(h, !BullDemonAbilityHandler.isRushing(p), "the wall ends the charge");
        eq(h, 9, amp(pinned, MobEffects.MOVEMENT_SLOWDOWN), "stunned");
        check(h, pinned.hasEffect(MobEffects.CONFUSION), "and reeling");
        check(h, h.getBlockState(new BlockPos(2, 2, 3)).is(Blocks.STONE), "the wall stands");
        BullDemonAbilityHandler.startRush(p);
        check(h, !BullDemonAbilityHandler.isRushing(p), "cooling down after");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theRushRunsOutAndLettingGoEndsIt(GameTestHelper h) {
        ServerPlayer p = bull(h, "bdk_rush_1");
        BullDemonAbilityHandler.startRush(p);
        BullDemonCharacter.INSTANCE.activate(p, 0, AbilityAction.STOP);
        check(h, !BullDemonAbilityHandler.isRushing(p), "let go: stops");
        eq(h, 40, BullDemonAbilityHandler.rushTicks(1), "I: two seconds at most");
        eq(h, 80, BullDemonAbilityHandler.rushTicks(3), "III: four");
        h.succeed();
    }

    // ── M: Great White Bull ────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theGreatWhiteBullIsBigToughAndDrainsQi(GameTestHelper h) {
        ServerPlayer p = bull(h, "bdk_bull_1", "bdk_bull_2");
        double baseHealth = p.getMaxHealth();
        BullDemonCharacter.INSTANCE.activate(p, 3, AbilityAction.USE);
        check(h, BullDemonAbilityHandler.inGreatBull(p), "the bull");
        TransformationData data = TransformationManager.getTransformation(p);
        check(h, data != null && data.getEntityType() == EntityType.RAVAGER, "giant form");
        near(h, baseHealth + 30, p.getMaxHealth(), "II: +30 health");
        near(h, 0.5, modifier(p, Attributes.ATTACK_DAMAGE, "bdk_bull_attack"), "II: +50% attack");
        near(h, 1.0, modifier(p, Attributes.KNOCKBACK_RESISTANCE, "bdk_bull_steady"), "unmoved by blows");
        tick(p, BullDemonAbilityHandler::onPlayerTick, 10);
        check(h, QiData.getCurrent(p) < 200f - 2.9f, "drains Qi");

        BullDemonCharacter.INSTANCE.activate(p, 3, AbilityAction.USE);
        check(h, !BullDemonAbilityHandler.inGreatBull(p), "back to himself");
        check(h, TransformationManager.getTransformation(p) == null, "demorphed");
        near(h, baseHealth, p.getMaxHealth(), "health as before");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theGreatBullEndsWhenQiRunsOut(GameTestHelper h) {
        ServerPlayer p = bull(h, "bdk_bull_1");
        BullDemonAbilityHandler.toggleGreatBull(p);
        qi(p, 0.1f, 200f);
        tick(p, BullDemonAbilityHandler::onPlayerTick, 2);
        check(h, !BullDemonAbilityHandler.inGreatBull(p), "out of Qi, out of the bull");
        check(h, TransformationManager.getTransformation(p) == null, "demorphed");
        h.succeed();
    }

    // ── Thick Hide ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void thickHideTakesNoFireAndToughens(GameTestHelper h) {
        ServerPlayer p = bull(h, "bdk_hide_1");
        LivingAttackEvent fire = new LivingAttackEvent(p, p.damageSources().inFire(), 4f);
        BullDemonAbilityHandler.onAttacked(fire);
        check(h, fire.isCanceled(), "fire cannot burn him");
        LivingAttackEvent blow = new LivingAttackEvent(p, p.damageSources().generic(), 4f);
        BullDemonAbilityHandler.onAttacked(blow);
        check(h, !blow.isCanceled(), "a blow still lands");
        tick(p, BullDemonAbilityHandler::onPlayerTick, 20);
        near(h, 10.0, modifier(p, PHYSICAL_RESISTANCE(), "bdk_hide_pres"), "I: +10 physical resistance");
        h.succeed();
    }

    // ── Banana-Leaf Fan ────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void anyonesFanThrowsBackWhatIsInFront(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        p.setYRot(0f); p.setXRot(0f);
        Zombie front = mob(h, EntityType.ZOMBIE, 2, 4);
        Zombie behind = mob(h, EntityType.ZOMBIE, 2, 0);
        List<LivingEntity> caught = BananaLeafFanItem.fan(p);
        check(h, caught.contains(front) && !caught.contains(behind), "only in front");
        check(h, front.getDeltaMovement().z > 1, "thrown back");
        check(h, !front.getPersistentData().contains(BullDemonAbilityHandler.KEY_BLOWN_AT), "not blown away");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theBullsFanBlowsThemOutOfSight(GameTestHelper h) {
        ServerPlayer p = bull(h);
        Zombie z = mob(h, EntityType.ZOMBIE, 2, 4);
        BananaLeafFanItem.fan(p);
        check(h, z.getPersistentData().contains(BullDemonAbilityHandler.KEY_BLOWN_AT), "blown away");
        z.getPersistentData().remove(BullDemonAbilityHandler.KEY_BLOWN_AT); // land it here instead

        Zombie far = mob(h, EntityType.ZOMBIE, 2, 3);
        Vec3 from = far.position();
        BullDemonAbilityHandler.blowAway(p, far, new Vec3(0, 0, 1), 40);
        BullDemonAbilityHandler.landBlown(far);
        near(h, 40.0, far.position().subtract(from).horizontalDistance(), "lands 40 blocks off");
        check(h, far.hasEffect(MobEffects.SLOW_FALLING), "drifting down");
        far.discard();

        near(h, 37.0, BullDemonAbilityHandler.fanReach(1f), "weak: 37 blocks");
        near(h, 1000.0, BullDemonAbilityHandler.fanReach(27.03f), "strong: 1000");
        near(h, 1000.0, BullDemonAbilityHandler.fanReach(200f), "never more than 1000");
        h.succeed();
    }

    // ── Mixed-Iron Cudgel ──────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theCudgelsShockwaveTravelsABlockATick(GameTestHelper h) {
        ServerPlayer p = bull(h);
        Vec3 edge = h.absoluteVec(new Vec3(2.5, 2, 0.5));
        p.moveTo(edge.x, edge.y, edge.z, 0f, 0f);
        Zombie z = mob(h, EntityType.ZOMBIE, 2, 4); // 4 blocks ahead; the wave strikes within 1.5 of its front
        List<Hit> log = watch(z);
        BullDemonAbilityHandler.Shockwave w = BullDemonAbilityHandler.newShockwave(p, 1);
        BullDemonAbilityHandler.advance(w);
        BullDemonAbilityHandler.advance(w);
        check(h, log.isEmpty(), "not there yet after two ticks");
        BullDemonAbilityHandler.advance(w);
        check(h, !of(log, DamageTypes.MOB_ATTACK).isEmpty(), "reached it on the third");
        check(h, z.getDeltaMovement().y > 0.5, "tossed up");
        while (BullDemonAbilityHandler.advance(w)) { }
        eq(h, 6, w.step(), "charge 1: six blocks");
        check(h, !h.getBlockState(new BlockPos(2, 1, 4)).isAir(), "breaks nothing");
        eq(h, 12, BullDemonAbilityHandler.shockwaveLength(3), "charge 3: twelve");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aWallStopsTheShockwave(GameTestHelper h) {
        ServerPlayer p = bull(h);
        h.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
        h.setBlock(new BlockPos(2, 3, 4), Blocks.STONE);
        BullDemonAbilityHandler.Shockwave w = BullDemonAbilityHandler.newShockwave(p, 3);
        BullDemonAbilityHandler.advance(w);
        check(h, !BullDemonAbilityHandler.advance(w), "stopped at the wall");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void onlyTheBullCanChargeTheCudgel(GameTestHelper h) {
        eq(h, 0, MixedIronCudgelItem.charge(19), "too short");
        eq(h, 1, MixedIronCudgelItem.charge(20), "one second");
        eq(h, 3, MixedIronCudgelItem.charge(200), "three at most");
        ServerPlayer other = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        ItemStack club = new ItemStack(ModItems.MIXED_IRON_CUDGEL.get());
        other.setItemInHand(InteractionHand.MAIN_HAND, club);
        club.use(other.level(), other, InteractionHand.MAIN_HAND);
        check(h, !other.isUsingItem(), "anyone else just swings it");

        ServerPlayer p = bull(h);
        ItemStack cudgel = new ItemStack(ModItems.MIXED_IRON_CUDGEL.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, cudgel);
        cudgel.use(p.level(), p, InteractionHand.MAIN_HAND);
        check(h, p.isUsingItem(), "the Bull raises it");
        cudgel.releaseUsing(p.level(), p, cudgel.getUseDuration() - 40);
        check(h, p.getCooldowns().isOnCooldown(ModItems.MIXED_IRON_CUDGEL.get()), "smashed: the club rests");
        h.succeed();
    }
}
