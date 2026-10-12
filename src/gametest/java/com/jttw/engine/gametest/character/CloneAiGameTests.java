package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.wukong.CloneWork;
import com.jttw.engine.character.wukong.WuCloneManager;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.entity.WuCloneEntity;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/**
 * Shadow clones acting as players: gathering with the tools their owner carries, keeping a pack,
 * emptying it into a chest or sending him their best finds, fighting with a ring of fire when
 * hurt, working where they are set or following him, and opening their pack to him.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class CloneAiGameTests {

    /** Wukong with a clone out, holding nothing; the clone is returned. */
    private static WuCloneEntity clone(GameTestHelper h, ServerPlayer p) {
        grant(p, "wu_clone_1");
        WukongCharacter.INSTANCE.use(p, "wu_clone", AbilityAction.USE);
        List<WuCloneEntity> mine = near(p, WuCloneEntity.class, 8).stream()
                .filter(c -> c.getOwnerUUID().map(p.getUUID()::equals).orElse(false)).toList();
        check(h, mine.size() == 1, "one clone out");
        com.jttw.engine.gametest.GT.onEnd(h, () -> WuCloneManager.dismissAll(p));
        return mine.get(0);
    }

    private static ServerPlayer wukong(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        qi(p, 100f, 100f);
        return p;
    }

    private static boolean packHas(WuCloneEntity clone, net.minecraft.world.item.Item item) {
        return clone.pack().countItem(item) > 0;
    }

    @GameTest(template = PLATFORM)
    public static void hisClonesAreFoundToCopyHim(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        check(h, WuCloneManager.clonesOf(p).contains(c), "his clone, found");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "clone_staff", timeoutTicks = 120)
    public static void aClonesStaffGrowsAndFallsAsHisDoes(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        var at = h.absolutePos(new BlockPos(1, 2, 1));
        var his = new com.jttw.engine.entity.PlacedStaffEntity(h.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getUUID());
        h.getLevel().addFreshEntity(his);
        var echo = com.jttw.engine.entity.PlacedStaffEntity.echoOf(his, at.getX() + 3.5, at.getY(), at.getZ() + 3.5);
        h.getLevel().addFreshEntity(echo);
        com.jttw.engine.gametest.GT.onEnd(h, () -> { his.discard(); echo.discard(); });
        check(h, echo.isEcho() && !his.isEcho(), "his, and its echo");
        echo.adjustScale(5);
        eq(h, 1f, echo.getStaffScale(), "the echo heeds no wheel of its own");
        his.adjustScale(3);
        float grown = his.getStaffScale();
        p.setYRot(90f);
        h.runAfterDelay(2, () -> {
            eq(h, grown, echo.getStaffScale(), "it grew as his grew");
            check(h, his.toppleBy(p), "he strikes his down");
        });
        h.runAfterDelay(4, () -> {
            check(h, echo.isFalling(), "the echo falls too");
            eq(h, 90f, echo.getToppleYaw(), "the same way");
            his.discard(); // back to his hand
        });
        h.runAfterDelay(6, () -> {
            check(h, !echo.isAlive(), "and the echo is gone with it");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void aCloneHasItsOwnersHealth(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        com.jttw.engine.progression.level.PlayerLevel.setLevel(p, 30);
        com.jttw.engine.progression.level.LevelManager.reapplyStats(p);
        WuCloneEntity c = clone(h, p);
        near(h, p.getMaxHealth(), c.getMaxHealth(), "its owner's max health");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void aCloneMinesAnOreItCanSeeWithAGoodEnoughPickaxe(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.IRON_PICKAXE));
        BlockPos ore = new BlockPos(4, 2, 4);
        h.setBlock(ore, Blocks.IRON_ORE);
        WuCloneEntity c = clone(h, p);
        h.succeedWhen(() -> {
            check(h, h.getBlockState(ore).isAir(), "the ore is mined");
            check(h, packHas(c, Items.RAW_IRON), "its raw iron is in the clone's pack");
            check(h, p.getInventory().countItem(Items.IRON_PICKAXE) == 1, "and his pickaxe is still his");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void withTooPoorAPickaxeItLeavesTheOre(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.WOODEN_PICKAXE));
        BlockPos ore = new BlockPos(4, 2, 4);
        h.setBlock(ore, Blocks.IRON_ORE);
        clone(h, p);
        h.runAfterDelay(100, () -> {
            check(h, h.getBlockState(ore).is(Blocks.IRON_ORE), "iron needs a stone pickaxe or better");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void aCloneFellsALogWithAnAxe(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getInventory().add(new ItemStack(Items.STONE_AXE));
        BlockPos log = new BlockPos(4, 2, 4);
        h.setBlock(log, Blocks.OAK_LOG);
        WuCloneEntity c = clone(h, p);
        h.succeedWhen(() -> {
            check(h, h.getBlockState(log).isAir(), "the log is felled");
            check(h, packHas(c, Items.OAK_LOG), "into the pack");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120)
    public static void withoutAnAxeItLeavesTheTrees(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        BlockPos log = new BlockPos(4, 2, 4);
        h.setBlock(log, Blocks.OAK_LOG);
        clone(h, p);
        h.runAfterDelay(100, () -> {
            check(h, h.getBlockState(log).is(Blocks.OAK_LOG), "no axe, no felling");
            h.succeed();
        });
    }

    private static void fill(WuCloneEntity c, int cobble, int diamonds) {
        for (int i = 0; i < cobble; i++) c.pack().addItem(new ItemStack(Items.COBBLESTONE, 64));
        for (int i = 0; i < diamonds; i++) c.pack().addItem(new ItemStack(Items.DIAMOND, 64));
    }

    // alone: a hostile mob in a neighbouring test would rightly come first, and it would never stash
    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "clone_stash")
    public static void aNearlyFullPackGoesIntoTheNearestChest(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        BlockPos chest = new BlockPos(4, 2, 4);
        h.setBlock(chest, Blocks.CHEST);
        WuCloneEntity c = clone(h, p);
        fill(c, 25, 0);
        check(h, CloneWork.nearlyFull(c.pack()), "nearly full");
        h.succeedWhen(() -> {
            var box = (ChestBlockEntity) h.getBlockEntity(chest);
            eq(h, 25 * 64, box.countItem(Items.COBBLESTONE), "the chest took all of it");
            eq(h, 0, c.pack().countItem(Items.COBBLESTONE), "and the pack is empty");
        });
    }

    @GameTest(template = PLATFORM)
    public static void withNoChestAboutItsBestFindsGoToItsOwner(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        fill(c, 20, 4);
        CloneWork.sendValuables(c, p);
        eq(h, 4 * 64, p.getInventory().countItem(Items.DIAMOND), "every diamond, first");
        check(h, CloneWork.value(new ItemStack(Items.DIAMOND)) > CloneWork.value(new ItemStack(Items.COBBLESTONE)), "diamonds over stone");
        check(h, !CloneWork.nearlyFull(c.pack()) && c.pack().countItem(Items.COBBLESTONE) > 0, "until it has room; the stone stays");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100)
    public static void badlyHurtItLightsARingOfFireThatSparesItsOwnSide(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        grant(p, "wu_ring_1", "wu_clone_2");
        WuCloneEntity c = clone(h, p);
        WukongCharacter.INSTANCE.use(p, "wu_clone", AbilityAction.USE); // a second clone, by it
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        zombie.setNoAi(true);
        c.getPersistentData().putFloat(WuCloneManager.CLONE_QI_TAG, 50f);
        c.setHealth(c.getMaxHealth() * 0.2f);
        h.succeedWhen(() -> {
            check(h, c.ringBurning(), "its ring is lit");
            check(h, zombie.isOnFire(), "what is near it burns");
            for (WuCloneEntity other : near(p, WuCloneEntity.class, 8)) {
                check(h, other == c || !other.isOnFire(), "not his other clone");
            }
            check(h, !p.isOnFire(), "nor him");
        });
    }

    @GameTest(template = PLATFORM)
    public static void sneakUsingItSetsItToWorkWhereItStands(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        check(h, c.getPriority() == WuCloneEntity.Priority.FOLLOW, "it follows him at first");
        check(h, WuCloneManager.followsOwner(c), "and so keeps with him");
        p.setShiftKeyDown(true);
        c.interact(p, InteractionHand.MAIN_HAND);
        check(h, c.getPriority() == WuCloneEntity.Priority.WORK, "set to work");
        eq(h, c.blockPosition(), c.workSite(), "where it stands");
        check(h, !WuCloneManager.followsOwner(c), "it stays at its work");
        c.tickCount += CloneWork.IDLE_BEFORE_FOLLOW + 1; // a long while with nothing to do
        check(h, WuCloneManager.followsOwner(c), "with nothing left to do, it comes back to him");
        c.interact(p, InteractionHand.MAIN_HAND);
        check(h, c.getPriority() == WuCloneEntity.Priority.FOLLOW, "and again: follow");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void usingItOpensItsPackToItsOwnerAlone(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        c.pack().addItem(new ItemStack(Items.DIAMOND, 3));
        p.setShiftKeyDown(false);
        c.interact(p, InteractionHand.MAIN_HAND);
        check(h, p.containerMenu instanceof ChestMenu menu && menu.getContainer() == c.pack(), "its pack, open to him");
        ServerPlayer stranger = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        c.interact(stranger, InteractionHand.MAIN_HAND);
        check(h, !(stranger.containerMenu instanceof ChestMenu), "not to anyone else");
        p.closeContainer();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aFallenOrDismissedCloneSpillsItsPack(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        WuCloneEntity c = clone(h, p);
        c.pack().addItem(new ItemStack(Items.EMERALD, 5));
        WuCloneManager.dismissAll(p);
        var dropped = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, c.getBoundingBox().inflate(3),
                e -> e.getItem().is(Items.EMERALD));
        eq(h, 5, dropped.stream().mapToInt(e -> e.getItem().getCount()).sum(), "its emeralds where it stood");
        dropped.forEach(net.minecraft.world.entity.Entity::discard);
        h.succeed();
    }
}
