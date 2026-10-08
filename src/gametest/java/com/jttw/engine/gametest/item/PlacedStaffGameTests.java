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
        check(h, staff.aimDistance(owner.getEyePosition(), new Vec3(-1, 0, 0)) < 0, "a falling staff is not aimed at");
        staff.discard();
        h.succeed();
    }
}
