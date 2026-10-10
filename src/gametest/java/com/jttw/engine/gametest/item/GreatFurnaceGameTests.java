package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.WukongCheckpoints;
import com.jttw.engine.character.wukong.WukongCheckpoints.Checkpoint;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.skilltree.SkillPurchase;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.StatBlock;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.furnace.FurnacePartBlock;
import com.jttw.engine.world.furnace.TrigramsFurnaceBlock;
import com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity;
import com.jttw.engine.world.furnace.WukongTrial;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/**
 * The great Eight Trigrams Furnace: three by three by five, fed only True Samadhi Fire (or lit by
 * Nezha's or the Phoenix's), a hatch into a fire that seals in and burns to death whatever falls
 * in; and Sun Wukong, made immortal, who bursts it apart.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class GreatFurnaceGameTests {

    private static final BlockPos CORE = new BlockPos(2, 2, 2);

    private static TrigramsFurnaceBlockEntity build(GameTestHelper h) {
        return TrigramsFurnaceBlock.build(h.getLevel(), h.absolutePos(CORE));
    }

    private static void light(TrigramsFurnaceBlockEntity f) {
        f.setItem(TrigramsFurnaceBlockEntity.FUEL, new ItemStack(ModItems.TRUE_SAMADHI_FIRE.get(), 2));
        f.ignite(TrigramsFurnaceBlockEntity.SAMADHI_BURN);
    }

    @GameTest(template = PLATFORM)
    public static void itStandsThreeByThreeByFiveAndFallsAsOne(GameTestHelper h) {
        build(h);
        int parts = 0;
        for (int[] o : TrigramsFurnaceBlock.partOffsets()) {
            if (h.getBlockState(CORE.offset(o[0], o[1], o[2])).getBlock() instanceof FurnacePartBlock) parts++;
        }
        eq(h, 44, parts, "44 parts round its core");
        h.getLevel().destroyBlock(h.absolutePos(CORE.offset(1, 3, 1)), true); // one part, up its side
        h.runAtTickTime(h.getTick() + 2, () -> {
            for (int[] o : TrigramsFurnaceBlock.partOffsets()) {
                check(h, h.getBlockState(CORE.offset(o[0], o[1], o[2])).isAir(), "every part gone");
            }
            check(h, h.getBlockState(CORE).isAir(), "its core too");
            var dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(CORE)).inflate(4),
                    e -> e.getItem().is(ModItems.EIGHT_TRIGRAMS_FURNACE_ITEM.get()));
            eq(h, 1, dropped.stream().mapToInt(e -> e.getItem().getCount()).sum(), "one furnace comes back");
            dropped.forEach(ItemEntity::discard);
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void onlyTrueSamadhiFireFeedsItAndLitItGivesLight(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = build(h);
        check(h, !f.canPlaceItem(TrigramsFurnaceBlockEntity.FUEL, new ItemStack(Items.COAL)), "not coal");
        check(h, !f.canPlaceItem(TrigramsFurnaceBlockEntity.FUEL, new ItemStack(Items.LAVA_BUCKET)), "not lava");
        check(h, f.canPlaceItem(TrigramsFurnaceBlockEntity.FUEL, new ItemStack(ModItems.TRUE_SAMADHI_FIRE.get())), "True Samadhi Fire");
        light(f);
        TrigramsFurnaceBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), f.getBlockState(), f);
        check(h, f.isLit(), "lit");
        check(h, h.getBlockState(CORE.above(4)).getValue(FurnacePartBlock.LIT), "every part shows it");
        check(h, h.getLevel().getLightEmission(h.absolutePos(CORE.offset(1, 2, 0))) > 0, "and gives light");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nezhasOrThePhoenixsFireLightsIt(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = build(h);
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0)); // stands in for the one breathing fire
        Vec3 eye = h.absoluteVec(new Vec3(0.5, 4.5, -2.5));
        TrigramsFurnaceBlockEntity.igniteAlong(h.getLevel(), eye, new Vec3(0.4, -0.1, 1).normalize(), 10, zombie);
        check(h, f.isLit(), "his fire on its side lit it");
        zombie.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 300)
    public static void whateverFallsInBurnsToDeathSealedIn(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = build(h);
        light(f);
        f.setHatch(true);
        check(h, f.hatchOpen(), "the hatch opens");
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, CORE.above(1));
        zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 600));
        h.succeedWhen(() -> {
            check(h, !f.hatchOpen(), "the fire sealed the hatch on it");
            check(h, !zombie.isAlive(), "and burned it to death, fire resistance or none (health " + zombie.getHealth() + ")");
        });
    }

    @GameTest(template = PLATFORM)
    public static void wukongUnreadyBurnsLikeAnyone(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        TrigramsFurnaceBlockEntity f = build(h);
        light(f);
        WukongCheckpoints.complete(p, Checkpoint.IMMORTAL_PEACHES);
        WukongCheckpoints.complete(p, Checkpoint.IMMORTALITY_PILLS); // no wine, no book
        check(h, !WukongTrial.endures(f, p), "the fire is as deadly to him");
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        check(h, !WukongTrial.endures(f, erlang), "and to anyone else");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wukongImmortalBurstsItAndComesOutChanged(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(p, 70);
        LevelManager.reapplyStats(p);
        double before = StatBlock.of(p).maxHealth();
        check(h, WukongTrial.lockedUntilFurnace(p, "wu_combat_2"), "Yao Wang locked before");
        SkillTreeData.addPoints(p, 10);
        check(h, !SkillPurchase.purchase(p, "wu_combat_1"), "not even with the points");
        for (Checkpoint cp : WukongTrial.PREREQUISITES) WukongCheckpoints.complete(p, cp);
        TrigramsFurnaceBlockEntity f = build(h);
        light(f);
        for (int t = 0; t < WukongTrial.TRIAL_TICKS - 1; t++) check(h, WukongTrial.endures(f, p), "the fire cannot harm him");
        check(h, h.getBlockState(CORE).getBlock() instanceof TrigramsFurnaceBlock, "still whole, just before");
        WukongTrial.endures(f, p);
        check(h, h.getBlockState(CORE).isAir() && h.getBlockState(CORE.above(4)).isAir(), "the furnace is burst apart");
        check(h, WukongCheckpoints.isComplete(p, Checkpoint.TRUE_SAMADHI_FIRE), "his level-70 checkpoint passed");
        check(h, ImmortalItems.hasFieryGoldenEyes(p), "the Fiery Golden Eyes");
        check(h, SkillTreeData.hasNode(p, "wu_combat_1"), "Yao Wang I, his outright");
        check(h, !WukongTrial.lockedUntilFurnace(p, "wu_combat_2"), "the rest open to him");
        near(h, before * WukongTrial.BOON, StatBlock.of(p).maxHealth(), "his power half again what it was");
        h.succeed();
    }
}
