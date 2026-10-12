package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.PlacedStaffEntity;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** The planted Ruyi Jingu Bang is aimed at as it is drawn, at any size, from its planting reach. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class PlacedStaffGameTests {

    private static PlacedStaffEntity plant(GameTestHelper h, ServerPlayer owner, float scale) {
        Vec3 at = h.absoluteVec(new Vec3(2.5, 2, 2.5));
        PlacedStaffEntity staff = new PlacedStaffEntity(h.getLevel(), at.x, at.y, at.z, owner.getUUID());
        staff.setStaffScale(scale);
        h.getLevel().addFreshEntity(staff);
        return staff;
    }

    @GameTest(template = PLATFORM)
    public static void aFreshStaffIsItsDrawnSize(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        Vec3 at = h.absoluteVec(new Vec3(2.5, 2, 2.5));
        PlacedStaffEntity staff = new PlacedStaffEntity(h.getLevel(), at.x, at.y, at.z, p.getUUID());
        near(h, 1.47, staff.getBbHeight(), "1.47 tall, not the old 3");
        near(h, 0.6, staff.getBbWidth(), "0.6 wide, not the old 1.5");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aTallStaffCanBeAimedAtNearItsTop(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlacedStaffEntity staff = plant(h, p, 100f);
        AABB box = staff.standingBox();
        near(h, 146.875, box.getYsize(), "as tall as it is drawn (1.46875 a scale)");
        check(h, box.getXsize() >= 0.11 * 100, "as wide as it is drawn: " + box.getXsize());
        check(h, staff.getBbHeight() <= 20f, "the collision box stays capped");

        // Flying beside it, 140 blocks up, 4 blocks out from its side, looking at it
        Vec3 eye = new Vec3(box.maxX + 4, staff.getY() + 140, staff.getZ());
        double d = staff.aimDistance(eye, new Vec3(-1, 0, 0));
        near(h, 4.0, d, "hit 4 blocks off, near the tip");
        check(h, staff.aimDistance(eye, new Vec3(1, 0, 0)) < 0, "looking away misses");
        check(h, staff.aimDistance(new Vec3(box.maxX + 7, staff.getY() + 140, staff.getZ()), new Vec3(-1, 0, 0)) < 0,
                "7 blocks off is out of reach");
        staff.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theServerChecksReachAndOwner(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlacedStaffEntity staff = plant(h, owner, 1f);
        Vec3 base = staff.position();
        owner.moveTo(base.x + 4.5, base.y, base.z);
        check(h, staff.withinReachOf(owner), "4.5 blocks: within the 5-block reach (vanilla stops at 3)");
        owner.moveTo(base.x + 12, base.y, base.z);
        check(h, !staff.withinReachOf(owner), "12 blocks: not");

        ServerPlayer stranger = player(h);
        stranger.moveTo(base.x + 1, base.y, base.z);
        check(h, !staff.toppleBy(stranger), "only its owner topples it");
        owner.moveTo(base.x + 2, base.y, base.z);
        check(h, staff.toppleBy(owner), "its owner does");
        check(h, staff.isFalling() && !staff.isStanding(), "falling");
        check(h, staff.aimDistance(owner.getEyePosition(), new Vec3(-1, 0, 0)) >= 0, "a fallen staff is aimed at, to call it back");
        staff.discard();
        h.succeed();
    }

    // ── Fallen: a rod that lies where it falls ──────────────────────────

    /** The staff, planted at {@code rel} on the platform, toppled by its owner facing +z (south). */
    private static PlacedStaffEntity felled(GameTestHelper h, ServerPlayer owner, net.minecraft.core.BlockPos rel, float scale) {
        var at = h.absolutePos(rel);
        PlacedStaffEntity staff = new PlacedStaffEntity(h.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, owner.getUUID());
        staff.setStaffScale(scale);
        h.getLevel().addFreshEntity(staff);
        com.jttw.engine.gametest.GT.onEnd(h, staff::discard);
        owner.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5, 0f, 0f);
        check(h, staff.toppleBy(owner), "toppled");
        return staff;
    }

    @GameTest(template = PLATFORM, batch = "staff_physics", timeoutTicks = 200)
    public static void itFallsAndLiesInTheHollowItLandsIn(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        // a hollow in the floor where its far end will come down
        h.setBlock(new net.minecraft.core.BlockPos(2, 1, 4), net.minecraft.world.level.block.Blocks.AIR);
        h.setBlock(new net.minecraft.core.BlockPos(2, 1, 3), net.minecraft.world.level.block.Blocks.AIR);
        PlacedStaffEntity staff = felled(h, owner, new net.minecraft.core.BlockPos(2, 2, 1), 2f);
        double floor = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2)).getY();
        h.succeedWhen(() -> {
            Vec3 tip = staff.rodTip(), foot = staff.rodBase();
            check(h, foot.distanceTo(tip) > staff.length() * 0.98 && foot.distanceTo(tip) < staff.length() * 1.02, "still its own length");
            check(h, tip.y < floor - 0.3, "its tip down in the hollow, below the floor: " + tip.y + " vs " + floor);
            check(h, staff.isAlive() && staff.isRod(), "lying where it fell");
        });
    }

    @GameTest(template = PLATFORM, batch = "staff_physics", timeoutTicks = 200)
    public static void aStaffFallingOnAnotherDoesNotPassThroughIt(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        // a staff standing in the way, and one toppled toward it
        var at = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 3));
        PlacedStaffEntity post = new PlacedStaffEntity(h.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, owner.getUUID());
        post.setStaffScale(3f);
        h.getLevel().addFreshEntity(post);
        com.jttw.engine.gametest.GT.onEnd(h, post::discard);
        PlacedStaffEntity falling = felled(h, owner, new net.minecraft.core.BlockPos(2, 2, 0), 3f);
        Vec3 postFoot = post.position(), postTop = postFoot.add(0, post.length(), 0);
        h.succeedWhen(() -> {
            Vec3 a = falling.rodBase(), b = falling.rodTip();
            double[] st = PlacedStaffEntity.closest(a, b, postFoot, postTop);
            double gap = a.lerp(b, st[0]).distanceTo(postFoot.lerp(postTop, st[1]));
            check(h, gap >= falling.radius() + post.radius() - 0.06, "never through the other: gap " + gap);
            check(h, b.y > postFoot.y + 0.8, "it rests leaning on the other, not on the floor beyond it: tip at " + (b.y - postFoot.y));
            check(h, a.distanceTo(b) > falling.length() * 0.97, "keeping its length");
        });
    }

    @GameTest(template = PLATFORM, batch = "staff_physics", timeoutTicks = 200)
    public static void itStaysWhereItLiesUntilStruckThenComesBack(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlacedStaffEntity staff = felled(h, owner, new net.minecraft.core.BlockPos(2, 2, 1), 1.5f);
        h.runAfterDelay(120, () -> {
            check(h, staff.isAlive() && staff.isRod(), "still lying there after six seconds: no return by itself");
            owner.moveTo(staff.rodBase().x, staff.rodBase().y, staff.rodBase().z - 1);
            check(h, staff.strikeBy(owner), "struck again");
            check(h, !staff.isAlive(), "it is gone from the ground");
            check(h, owner.getInventory().countItem(com.jttw.engine.registry.ModItems.RUYI_JINGU_BANG.get()) == 1, "back in his hand");
            h.succeed();
        });
    }

    /** Its own landing blast (and anything else but its owner's hand) does not call it back. */
    @GameTest(template = PLATFORM, batch = "staff_probe", timeoutTicks = 200)
    public static void aBigStaffSurvivesItsOwnBlast(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlacedStaffEntity staff = felled(h, owner, new net.minecraft.core.BlockPos(2, 2, 1), 6f);
        ServerPlayer thief = player(h);
        h.runAfterDelay(120, () -> {
            check(h, staff.isAlive() && staff.isRod(), "still lying where it fell, after its blast");
            // only he takes it back: another's blow does nothing to it
            thief.moveTo(staff.rodBase().x, staff.rodBase().y, staff.rodBase().z - 1);
            check(h, !staff.strikeBy(thief), "another cannot call it back");
            staff.hurt(h.getLevel().damageSources().playerAttack(thief), 100);
            check(h, staff.isAlive() && staff.isRod(), "nor knock it away");
            check(h, thief.getInventory().countItem(com.jttw.engine.registry.ModItems.RUYI_JINGU_BANG.get()) == 0, "nor take it");
            check(h, owner.getInventory().countItem(com.jttw.engine.registry.ModItems.RUYI_JINGU_BANG.get()) == 0, "and not back in his hand");
            h.succeed();
        });
    }

    /** A tall staff struck down topples as the heavy thing it is: it is pushed over, never flung up. */
    // its own batch: 44 blocks long, it falls across where the other staff tests stand
    @GameTest(template = PLATFORM, batch = "staff_tall", timeoutTicks = 200)
    public static void aTallStaffTopplesAndIsNeverFlungUp(GameTestHelper h) {
        ServerPlayer owner = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlacedStaffEntity staff = felled(h, owner, new net.minecraft.core.BlockPos(2, 2, 1), 30f);
        double floor = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 1)).getY();
        double tipStart = floor + staff.length();
        double[] highest = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        h.onEachTick(() -> {
            highest[0] = Math.max(highest[0], staff.rodBase().y);
            highest[1] = Math.max(highest[1], staff.rodTip().y);
        });
        h.runAfterDelay(60, () -> {
            check(h, highest[0] < floor + 0.3, "its foot stays down: rose " + (highest[0] - floor));
            check(h, highest[1] < tipStart + 0.3, "its tip never rises above where it stood: " + (highest[1] - tipStart));
            h.succeed();
        });
    }
}
