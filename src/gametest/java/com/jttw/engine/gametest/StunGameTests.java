package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModEffects;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Stun;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * The full stops ({@link Stun}): stunned, a creature neither moves nor strikes; pinned, it does not
 * move but strikes what is in reach; both let go when they end; both hold a player; bosses shrug
 * them off in half the time. Each ability that stuns or pins is tested with its character.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class StunGameTests {

    /** How long the creatures are held in these tests, and how long after they are given to show they are free. */
    private static final int HOLD = 60, AFTER = 100;

    /** How far {@code e} has walked from {@code from}, along the ground. */
    private static double walked(LivingEntity e, Vec3 from) {
        double dx = e.getX() - from.x, dz = e.getZ() - from.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** A villager that stands where it is put, for the husks to want. */
    private static Villager prey(GameTestHelper h, BlockPos at) {
        return h.spawnWithNoFreeWill(EntityType.VILLAGER, at);
    }

    /**
     * A husk two blocks short of a villager (out of its reach) and another beside one: stunned, the
     * first never takes a step toward its prey and neither strikes, though both are set on their
     * prey every tick; once it ends, the first walks and strikes.
     */
    @GameTest(template = PLATFORM, batch = "fullStop", timeoutTicks = HOLD + AFTER + 20)
    public static void aStunnedCreatureNeitherMovesNorStrikesAndIsFreeAfter(GameTestHelper h) {
        Husk walker = h.spawn(EntityType.HUSK, new BlockPos(1, 2, 1));
        Villager far = prey(h, new BlockPos(1, 2, 4));
        Husk striker = h.spawn(EntityType.HUSK, new BlockPos(4, 2, 1));
        Villager beside = prey(h, new BlockPos(4, 2, 2));
        Stun.stun(walker, HOLD, null);
        Stun.stun(striker, HOLD, null);
        Vec3 from = walker.position();
        List<String> wrong = new ArrayList<>();
        h.onEachTick(() -> {
            if (!walker.hasEffect(ModEffects.STUNNED)) return;
            walker.setTarget(far);
            striker.setTarget(beside);
            if (walker.getTarget() != null || striker.getTarget() != null) wrong.add("took up its prey at " + h.getTick());
            if (walked(walker, from) >= 0.01) wrong.add("stepped " + walked(walker, from) + " at " + h.getTick());
            if (beside.getHealth() < beside.getMaxHealth() || far.getHealth() < far.getMaxHealth()) wrong.add("struck at " + h.getTick());
        });
        h.runAfterDelay(HOLD + 1, () -> {
            check(h, wrong.isEmpty(), "stunned, it neither moves nor strikes: " + wrong);
            check(h, !walker.hasEffect(ModEffects.STUNNED), "the stun ends");
            near(h, walker.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), walker.getAttributeValue(Attributes.MOVEMENT_SPEED), "its speed back");
            walker.setTarget(far);
            striker.setTarget(beside);
        });
        h.runAfterDelay(HOLD + 2, () -> h.succeedWhen(() -> {
            check(h, walked(walker, from) > 0.5, "free, it walks to its prey");
            check(h, beside.getHealth() < beside.getMaxHealth(), "free, it strikes");
        }));
    }

    /**
     * Pinned the same: the far husk never takes a step, but the one beside its prey strikes it;
     * once it ends, the far one walks.
     */
    @GameTest(template = PLATFORM, batch = "fullStop", timeoutTicks = HOLD + AFTER + 20)
    public static void aPinnedCreatureCannotMoveButStrikesWhatIsInReach(GameTestHelper h) {
        Husk walker = h.spawn(EntityType.HUSK, new BlockPos(1, 2, 1));
        Villager far = prey(h, new BlockPos(1, 2, 4));
        Husk striker = h.spawn(EntityType.HUSK, new BlockPos(4, 2, 1));
        Villager beside = prey(h, new BlockPos(4, 2, 2));
        Stun.pin(walker, HOLD, null);
        Stun.pin(striker, HOLD, null);
        Vec3 from = walker.position(), strikerFrom = striker.position();
        boolean[] struck = {false};
        List<String> wrong = new ArrayList<>();
        h.onEachTick(() -> {
            if (!walker.hasEffect(ModEffects.PINNED)) return;
            walker.setTarget(far);
            striker.setTarget(beside);
            if (walked(walker, from) >= 0.01) wrong.add("stepped " + walked(walker, from) + " at " + h.getTick());
            if (walked(striker, strikerFrom) >= 0.01) wrong.add("the striker stepped at " + h.getTick());
            if (beside.getHealth() < beside.getMaxHealth()) struck[0] = true;
        });
        h.runAfterDelay(HOLD + 1, () -> {
            check(h, wrong.isEmpty(), "pinned, it does not move: " + wrong);
            check(h, struck[0], "pinned, it still struck what was in reach");
            check(h, !walker.hasEffect(ModEffects.PINNED), "the pin ends");
            walker.setTarget(far);
        });
        h.runAfterDelay(HOLD + 2, () -> h.succeedWhen(() -> check(h, walked(walker, from) > 0.5, "free, it walks to its prey")));
    }

    private static double value(LivingEntity e, Holder<Attribute> a) {
        return e.getAttributeValue(a);
    }

    /** A player, stunned: no speed (not even sprinting), no jump, no swimming; his blows land on nothing. Then all back. */
    @GameTest(template = PLATFORM)
    public static void aStunnedPlayerCannotMoveJumpOrStrike(GameTestHelper h) {
        ServerPlayer p = player(h);
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(3, 2, 2));
        double walk = value(p, Attributes.MOVEMENT_SPEED), jump = value(p, Attributes.JUMP_STRENGTH), swim = value(p, NeoForgeMod.SWIM_SPEED);
        Stun.stun(p, 40, null);
        p.setSprinting(true);
        eq(h, 0.0, value(p, Attributes.MOVEMENT_SPEED), "no speed, sprinting or not");
        eq(h, 0.0, value(p, Attributes.JUMP_STRENGTH), "no jump");
        eq(h, 0.0, value(p, NeoForgeMod.SWIM_SPEED), "no swimming");
        p.setDeltaMovement(0, 0.42, 0);
        NeoForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(p));
        check(h, p.getDeltaMovement().y <= 0, "a jump (Jump Boost's too) goes nowhere");
        p.attack(golem);
        eq(h, golem.getMaxHealth(), golem.getHealth(), "his blow lands on nothing");
        p.removeEffect(ModEffects.STUNNED);
        p.setSprinting(false);
        near(h, walk, value(p, Attributes.MOVEMENT_SPEED), "his speed back");
        near(h, jump, value(p, Attributes.JUMP_STRENGTH), "his jump back");
        near(h, swim, value(p, NeoForgeMod.SWIM_SPEED), "his swimming back");
        p.attack(golem);
        check(h, golem.getHealth() < golem.getMaxHealth(), "and his blows");
        h.succeed();
    }

    /** A player, pinned: no speed, no jump, but his blows land. */
    @GameTest(template = PLATFORM)
    public static void aPinnedPlayerCannotMoveButStrikes(GameTestHelper h) {
        ServerPlayer p = player(h);
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(3, 2, 2));
        double walk = value(p, Attributes.MOVEMENT_SPEED);
        Stun.pin(p, 40, null);
        eq(h, 0.0, value(p, Attributes.MOVEMENT_SPEED), "no speed");
        eq(h, 0.0, value(p, Attributes.JUMP_STRENGTH), "no jump");
        p.attack(golem);
        check(h, golem.getHealth() < golem.getMaxHealth(), "his blow lands");
        p.removeEffect(ModEffects.PINNED);
        near(h, walk, value(p, Attributes.MOVEMENT_SPEED), "his speed back");
        h.succeed();
    }

    /** Bosses shrug a stun or a pin off in half the time; anything else takes all of it. */
    @GameTest(template = PLATFORM)
    public static void bossesShrugItOffInHalfTheTime(GameTestHelper h) {
        var king = h.spawn(ModEntities.KING_BIANCHENG.get(), new BlockPos(1, 2, 1));
        king.setNoAi(true);
        IronGolem golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(4, 2, 4));
        for (LivingEntity e : List.of(king, golem)) {
            Stun.stun(e, 40, null);
            Stun.pin(e, 60, null);
        }
        check(h, Stun.isBoss(king) && !Stun.isBoss(golem), "a king of hell is a boss; a golem is not");
        eq(h, 20, duration(king, ModEffects.STUNNED), "the king: stunned 1 s of 2");
        eq(h, 30, duration(king, ModEffects.PINNED), "pinned 1.5 s of 3");
        eq(h, 40, duration(golem, ModEffects.STUNNED), "the golem: all 2 s");
        eq(h, 60, duration(golem, ModEffects.PINNED), "all 3 s");
        king.discard();
        h.succeed();
    }

    private static int duration(LivingEntity e, Holder<net.minecraft.world.effect.MobEffect> effect) {
        MobEffectInstance i = e.getEffect(effect);
        return i == null ? -1 : i.getDuration();
    }
}
