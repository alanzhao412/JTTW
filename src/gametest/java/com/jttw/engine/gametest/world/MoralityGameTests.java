package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.heaven.HeavenlySoldierEntity;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.morality.HeavenlyHost;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/** Karma, and the host Heaven sends after those it outlaws. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class MoralityGameTests {

    private static <T extends LivingEntity> T slay(GameTestHelper h, ServerPlayer by, EntityType<T> type) {
        T victim = h.spawn(type, new BlockPos(2, 2, 3));
        victim.hurt(h.getLevel().damageSources().playerAttack(by), Float.MAX_VALUE);
        return victim;
    }

    @GameTest(template = PLATFORM)
    public static void heavenWatchesHowYouKill(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        slay(h, p, EntityType.ZOMBIE);
        eq(h, Morality.SLAY_MONSTER, Morality.karma(p), "slaying a monster is a good deed");
        slay(h, p, EntityType.COW);
        eq(h, Morality.SLAY_MONSTER, Morality.karma(p), "butchering a cow is nothing either way");
        slay(h, p, EntityType.VILLAGER);
        eq(h, Morality.SLAY_MONSTER + Morality.KILL_VILLAGER, Morality.karma(p), "killing a villager is not");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aFightIsNotAMurder(GameTestHelper h) {
        ServerPlayer killer = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        ServerPlayer victim = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        eq(h, Morality.KILL_PLAYER, Morality.killDeed(killer, victim), "an unprovoked kill is murder");
        killer.setLastHurtByMob(victim);
        eq(h, 0, Morality.killDeed(killer, victim), "killing someone who struck you first is not");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void robbingHeavenIsTheGravestTheft(GameTestHelper h) {
        ServerPlayer wukong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        BlockPos jar = new BlockPos(2, 2, 2), book = new BlockPos(3, 2, 2);
        h.setBlock(jar, ModBlocks.WINE_JAR.get());
        h.setBlock(book, ModBlocks.BOOK_OF_LIFE_AND_DEATH.get());
        for (BlockPos at : List.of(jar, book)) {
            BlockPos abs = h.absolutePos(at);
            var state = h.getLevel().getBlockState(abs);
            var hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
            if (state.useItemOn(wukong.getItemInHand(InteractionHand.MAIN_HAND), h.getLevel(), wukong, InteractionHand.MAIN_HAND, hit)
                    == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                state.useWithoutItem(h.getLevel(), wukong, hit);
            }
        }
        eq(h, Morality.STEAL_WINE + Morality.STRIKE_THE_BOOK, Morality.karma(wukong), "the wine and the Book");
        h.setBlock(new BlockPos(2, 3, 3), ModBlocks.PEACH_FRUIT.get());
        wukong.gameMode.destroyBlock(h.absolutePos(new BlockPos(2, 3, 3)));
        eq(h, Morality.STEAL_WINE + Morality.STRIKE_THE_BOOK + Morality.STEAL_PEACH, Morality.karma(wukong), "and a peach");
        // the heist of the Havoc in Heaven: enough for Heaven to hunt him, as it hunted the Monkey King
        eq(h, 1, Morality.wanted(wukong), "an outlaw of Heaven");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void heavensWrathDeepensAsKarmaSinks(GameTestHelper h) {
        eq(h, 0, Morality.wanted(Morality.OUTLAW + 1), "just short of outlawry");
        eq(h, 1, Morality.wanted(Morality.OUTLAW), "an outlaw");
        eq(h, 2, Morality.wanted(Morality.ENEMY), "Heaven's enemy");
        eq(h, 3, Morality.wanted(Morality.GREAT_REBEL), "a great rebel");
        ServerPlayer p = player(h);
        Morality.set(p, Morality.MIN - 500);
        eq(h, Morality.MIN, Morality.karma(p), "karma has a floor");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "heavenly_host")
    public static void heavenSendsItsHostAfterItsEnemies(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        List<HeavenlySoldierEntity> sent = new java.util.ArrayList<>();
        onEnd(h, () -> sent.forEach(HeavenlySoldierEntity::discard));
        Morality.set(p, Morality.ENEMY);
        sent.addAll(HeavenlyHost.wave(h.getLevel(), p, Morality.wanted(p)));
        eq(h, HeavenlyHost.WAVE[2] + HeavenlyHost.GENERALS[2], sent.size(), "a wave of soldiers with a general at their head");
        eq(h, 1, (int) sent.stream().filter(HeavenlySoldierEntity::isGeneral).count(), "one general");
        check(h, sent.stream().allMatch(s -> p.getUUID().equals(s.quarry()) && s.getTarget() == p), "every one sent after them");
        check(h, sent.stream().allMatch(s -> s.distanceTo(p) >= 15), "coming down from round about, not on top of them");
        HeavenlySoldierEntity soldier = sent.stream().filter(s -> !s.isGeneral()).findFirst().orElseThrow();
        HeavenlySoldierEntity general = sent.stream().filter(HeavenlySoldierEntity::isGeneral).findFirst().orElseThrow();
        check(h, general.getMaxHealth() > soldier.getMaxHealth() * 2, "a general far stronger than a soldier");
        check(h, soldier.mobLevel() >= 20 + HeavenlyHost.ABOVE[2] - 1, "levelled above the one they hunt");
        // waves keep coming, never more out at once than Heaven lets loose
        for (int i = 0; i < 10; i++) sent.addAll(HeavenlyHost.wave(h.getLevel(), p, 2));
        eq(h, HeavenlyHost.MOST[2], HeavenlyHost.hunting(h.getLevel(), p).size(), "no more than the most at once");
        check(h, HeavenlyHost.wave(h.getLevel(), p, 0).isEmpty(), "and none for the unhunted");
        h.succeed();
    }
}
