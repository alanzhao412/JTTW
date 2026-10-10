package com.jttw.engine.gametest.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.peng.PengSkillTree;
import com.jttw.engine.character.wukong.WukongSkillTree;
import com.jttw.engine.flight.FlyingState;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/** Who flies on their own and who rides a cloud: Wukong only once he has the Somersault Cloud. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlightLookGameTests {

    private static ServerPlayer flying(GameTestHelper h, ReincarnationOutcome who, String... nodes) {
        ServerPlayer p = as(h, who);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        return p;
    }

    @GameTest(template = PLATFORM)
    public static void wukongFliesOnHisOwnUntilTheSomersaultCloud(GameTestHelper h) {
        ServerPlayer early = flying(h, ReincarnationOutcome.CELESTIAL_PRIMATE, WukongSkillTree.FLIGHT_1, WukongSkillTree.FLIGHT_2);
        eq(h, FlyingState.FLYING, FlyingState.mode(early), "Flight I and II: no cloud yet");
        SkillTreeData.grantNode(early, WukongSkillTree.FLIGHT_3);
        eq(h, FlyingState.ON_CLOUD, FlyingState.mode(early), "Flight III: on his cloud");
        early.getAbilities().flying = false;
        eq(h, FlyingState.GROUNDED, FlyingState.mode(early), "and none on the ground");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nobodyElseRidesACloud(GameTestHelper h) {
        ServerPlayer peng = flying(h, ReincarnationOutcome.GOLD_WINGED_PENG, PengSkillTree.SOAR_1);
        eq(h, FlyingState.FLYING, FlyingState.mode(peng), "the Peng, at his fastest, on his own wings");
        ServerPlayer creative = flying(h, ReincarnationOutcome.CELESTIAL_PRIMATE, WukongSkillTree.FLIGHT_3);
        creative.setGameMode(GameType.CREATIVE);
        creative.getAbilities().flying = true;
        eq(h, FlyingState.GROUNDED, FlyingState.mode(creative), "creative flight is vanilla's");
        h.succeed();
    }
}
