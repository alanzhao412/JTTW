package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.saurian.SaurianAbilityHandler;
import com.jttw.engine.character.saurian.SaurianCharacter;
import com.jttw.engine.character.saurian.SaurianSkillTree;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.registry.ModAttributes;
import com.jttw.engine.registry.ModEffects;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/** The Saurian Demon King, the Flood-Dragon: his own character, never the Nine Spirits'. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class SaurianGameTests {

    /** The Saurian with these nodes, Qi to spare, facing south (toward +z) level. */
    private static ServerPlayer saurian(GameTestHelper h, String... nodes) {
        ServerPlayer p = as(h, ReincarnationOutcome.SAURIAN_DEMON_KING);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        QiData.setMax(p, 1000f);
        QiData.setCurrent(p, 1000f);
        p.setYRot(0f);
        p.setXRot(0f);
        return p;
    }

    private static Zombie still(GameTestHelper h, BlockPos at) {
        Zombie z = h.spawn(EntityType.ZOMBIE, at);
        z.setNoAi(true);
        return z;
    }

    @GameTest(template = PLATFORM)
    public static void heIsHisOwnCharacter(GameTestHelper h) {
        ServerPlayer p = saurian(h);
        check(h, SaurianCharacter.is(p), "the Saurian Demon King plays himself");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "saurian")
    public static void riptideSurgeLaunchesHimSpinningThroughHisFoes(GameTestHelper h) {
        ServerPlayer p = saurian(h, SaurianSkillTree.SURGE_1);
        Zombie z = still(h, new BlockPos(2, 2, 4));
        float before = z.getHealth();
        SaurianCharacter.INSTANCE.use(p, "sdk_surge", AbilityAction.START);
        SaurianCharacter.INSTANCE.use(p, "sdk_surge", AbilityAction.STOP);
        check(h, SaurianAbilityHandler.surging(p), "let go: he surges");
        check(h, p.isAutoSpinAttack(), "spinning, as with a riptide");
        check(h, p.getDeltaMovement().z > 1, "the way he looks: " + p.getDeltaMovement());
        check(h, z.getHealth() < before, "striking what he passes");
        float struck = z.getHealth();
        SaurianAbilityHandler.tickSurge(p);
        eq(h, struck, z.getHealth(), "once a surge");
        SaurianAbilityHandler.endSurge(p);
        check(h, !p.isAutoSpinAttack() && !SaurianAbilityHandler.surging(p), "and it ends");
        SaurianCharacter.INSTANCE.use(p, "sdk_surge", AbilityAction.START);
        SaurianCharacter.INSTANCE.use(p, "sdk_surge", AbilityAction.STOP);
        check(h, !SaurianAbilityHandler.surging(p), "then it must rest");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "saurian")
    public static void tidalLashYanksTheFoeAtTheCrosshair(GameTestHelper h) {
        ServerPlayer p = saurian(h, SaurianSkillTree.LASH_1);
        Zombie z = still(h, new BlockPos(2, 2, 4));
        float before = z.getHealth();
        var caught = SaurianAbilityHandler.lash(p);
        check(h, caught == z, "the foe on the line: eye " + p.getEyePosition() + " look " + p.getLookAngle()
                + " zombie " + z.getBoundingBox() + " caught " + caught);
        check(h, z.getHealth() < before, "lashed");
        check(h, z.getDeltaMovement().z < -0.2, "and yanked toward him: " + z.getDeltaMovement());
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "saurian")
    public static void sneakingTheLashPullsHimToABlock(GameTestHelper h) {
        ServerPlayer p = saurian(h, SaurianSkillTree.LASH_1);
        h.setBlock(new BlockPos(2, 3, 9), net.minecraft.world.level.block.Blocks.STONE);
        p.setShiftKeyDown(true);
        check(h, SaurianAbilityHandler.lash(p) == null, "no foe caught");
        check(h, p.getDeltaMovement().z > 0.5, "pulled toward the block: " + p.getDeltaMovement());
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "saurian_whirlpool", timeoutTicks = 100)
    public static void aWhirlpoolDragsFoesInAndDrownsThem(GameTestHelper h) {
        ServerPlayer p = saurian(h, SaurianSkillTree.WHIRL_1);
        p.setXRot(60f); // looking at the ground ahead
        var pool = SaurianAbilityHandler.whirlpool(p);
        check(h, pool != null, "a whirlpool where he looks");
        Vec3 c = pool.at();
        Zombie victim = EntityType.ZOMBIE.create(h.getLevel());
        victim.moveTo(c.x + 2.5, c.y, c.z);
        h.getLevel().addFreshEntity(victim); // with its wits: a mob without AI cannot be moved at all
        onEnd(h, victim::discard);
        float before = victim.getHealth();
        double startDist = victim.position().distanceTo(c);
        h.succeedWhen(() -> {
            check(h, victim.getHealth() < before, "drowning");
            check(h, victim.position().distanceTo(c) < startDist - 0.5, "dragged in: " + victim.position().distanceTo(c));
        });
    }

    @GameTest(template = PLATFORM, batch = "saurian")
    public static void underTheWrathHisBlowsBurstInFloodwater(GameTestHelper h) {
        ServerPlayer p = saurian(h, SaurianSkillTree.WRATH_1);
        check(h, SaurianAbilityHandler.wrath(p), "the Wrath");
        check(h, p.hasEffect(ModEffects.FLOOD_WRATH), "on him, for a time");
        check(h, !SaurianAbilityHandler.wrath(p), "and not again while it lasts");
        Zombie struck = still(h, new BlockPos(2, 2, 3)), beside = still(h, new BlockPos(3, 2, 4));
        float before = beside.getHealth();
        p.attack(struck);
        check(h, beside.getHealth() < before, "the burst reaches the one beside");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "saurian")
    public static void hisVenomStacksToItsMost(GameTestHelper h) {
        ServerPlayer p = saurian(h, SaurianSkillTree.VENOM_1);
        Zombie z = still(h, new BlockPos(2, 2, 3));
        for (int i = 0; i < 5; i++) {
            z.invulnerableTime = 0;
            p.attack(z);
        }
        eq(h, SaurianAbilityHandler.venomMaxStacks(1), SaurianAbilityHandler.venomStacks(z), "three stacks at most at I");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void scaledHideAndRiverborn(GameTestHelper h) {
        ServerPlayer p = saurian(h, SaurianSkillTree.HIDE_1, SaurianSkillTree.HIDE_2, SaurianSkillTree.HIDE_3, SaurianSkillTree.RIVER_1);
        double pres = p.getAttributeValue(ModAttributes.PHYSICAL_RESISTANCE);
        SaurianAbilityHandler.passives(p, true);
        check(h, p.getAttributeValue(ModAttributes.PHYSICAL_RESISTANCE) >= pres + 30, "his hide, tougher against blows");
        check(h, p.getAttributeValue(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED) > 1.5, "and he swims swiftly");
        SaurianAbilityHandler.passives(p, false);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nobodyElseHasHisArts(GameTestHelper h) {
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        QiData.setMax(erlang, 1000f);
        QiData.setCurrent(erlang, 1000f);
        SaurianAbilityHandler.startCharge(erlang);
        SaurianAbilityHandler.release(erlang);
        check(h, !SaurianAbilityHandler.surging(erlang), "no surge");
        check(h, SaurianAbilityHandler.lash(erlang) == null && SaurianAbilityHandler.whirlpool(erlang) == null
                && !SaurianAbilityHandler.wrath(erlang), "no lash, no whirlpool, no wrath");
        h.succeed();
    }
}
