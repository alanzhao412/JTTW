package com.jttw.engine.gametest.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.peng.PengSkillTree;
import com.jttw.engine.character.wukong.WukongSkillTree;
import com.jttw.engine.flight.FlyingState;
import com.jttw.engine.flight.JTTWFlight;
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

/** Who flies on their own, who rides a cloud (Wukong only, at his top speed) and who flies in a squall (Erlang). */
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
    public static void wukongsCloudIsHisSprint(GameTestHelper h) {
        ServerPlayer p = flying(h, ReincarnationOutcome.CELESTIAL_PRIMATE, WukongSkillTree.FLIGHT_1, WukongSkillTree.FLIGHT_2);
        p.setSprinting(true);
        eq(h, FlyingState.FLYING, FlyingState.mode(p), "before Flight III, no cloud even sprinting");
        SkillTreeData.grantNode(p, WukongSkillTree.FLIGHT_3);
        eq(h, FlyingState.ON_CLOUD, FlyingState.mode(p), "Flight III, sprinting through the air: on his cloud");
        p.setSprinting(false);
        eq(h, FlyingState.FLYING, FlyingState.mode(p), "cruising: stretched out along his flight");
        p.setSprinting(true);
        p.getAbilities().flying = false;
        eq(h, FlyingState.GROUNDED, FlyingState.mode(p), "and none sprinting on the ground");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void erlangLeapsThroughWindAndFog(GameTestHelper h) {
        ServerPlayer erlang = flying(h, ReincarnationOutcome.EYE_OF_HEAVEN, com.jttw.engine.character.erlang.ErlangShenSkillTree.FLIGHT_3);
        eq(h, FlyingState.IN_SQUALL, FlyingState.mode(erlang), "in his squall, at every tier");
        erlang.getAbilities().flying = false;
        eq(h, FlyingState.GROUNDED, FlyingState.mode(erlang), "and none on the ground");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nobodyElseRidesACloud(GameTestHelper h) {
        ServerPlayer peng = flying(h, ReincarnationOutcome.GOLD_WINGED_PENG, PengSkillTree.SOAR_1);
        eq(h, FlyingState.FLYING, FlyingState.mode(peng), "the Peng, at his fastest, on his own wings");
        h.succeed();
    }

    /**
     * In creative, one who flies by their own power flies (and is drawn) their own way: Wukong
     * stretched out, and on his cloud at his top speed. Creative's flight alone is vanilla's.
     */
    @GameTest(template = PLATFORM)
    public static void inCreativeACharactersOwnFlightIsStillTheirs(GameTestHelper h) {
        ServerPlayer wukong = flying(h, ReincarnationOutcome.CELESTIAL_PRIMATE, WukongSkillTree.FLIGHT_1, WukongSkillTree.FLIGHT_2, WukongSkillTree.FLIGHT_3);
        JTTWFlight.setUniversalFlight(wukong, true);
        JTTWFlight.setFlightSpeedTier(wukong, 3);
        wukong.setGameMode(GameType.CREATIVE);
        wukong.getAbilities().flying = true;
        eq(h, FlyingState.FLYING, FlyingState.mode(wukong), "his own flight, in creative: stretched out along it");
        wukong.setSprinting(true);
        eq(h, FlyingState.ON_CLOUD, FlyingState.mode(wukong), "and sprinting, on his cloud");
        ServerPlayer plain = as(h, ReincarnationOutcome.BULL_DEMON_KING); // no flight of his own
        plain.setGameMode(GameType.CREATIVE);
        plain.getAbilities().flying = true;
        eq(h, FlyingState.GROUNDED, FlyingState.mode(plain), "creative's flight alone is vanilla's");
        h.succeed();
    }
}
