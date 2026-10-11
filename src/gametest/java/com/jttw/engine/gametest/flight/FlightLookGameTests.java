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

    /** Moves him {@code perTick} blocks along x for {@code ticks} ticks, as flying does, his state ticking. */
    private static void fly(ServerPlayer p, double perTick, int ticks) {
        for (int i = 0; i < ticks; i++) {
            p.setPos(p.getX() + perTick, p.getY(), p.getZ());
            FlyingState.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END, p));
        }
    }

    @GameTest(template = PLATFORM)
    public static void wukongsCloudMarksHisTopSpeed(GameTestHelper h) {
        ServerPlayer p = flying(h, ReincarnationOutcome.CELESTIAL_PRIMATE, WukongSkillTree.FLIGHT_1, WukongSkillTree.FLIGHT_2);
        JTTWFlight.setFlightSpeedTier(p, 2);
        fly(p, 2.8, 10);
        eq(h, FlyingState.FLYING, FlyingState.mode(p), "before Flight III, no cloud even at his fastest");
        SkillTreeData.grantNode(p, WukongSkillTree.FLIGHT_3);
        JTTWFlight.setFlightSpeedTier(p, 3);
        fly(p, 0.5, 10);
        eq(h, FlyingState.FLYING, FlyingState.mode(p), "Flight III, flying slowly: no cloud");
        fly(p, 2.8, 4);
        eq(h, FlyingState.ON_CLOUD, FlyingState.mode(p), "at his full speed, at once (within a few ticks), on his cloud");
        fly(p, 1.8, 2);
        eq(h, FlyingState.ON_CLOUD, FlyingState.mode(p), "easing off a moment, he keeps it");
        fly(p, 0.3, 10);
        eq(h, FlyingState.FLYING, FlyingState.mode(p), "slowed, it is gone");
        fly(p, 2.8, 6);
        p.getAbilities().flying = false;
        eq(h, FlyingState.GROUNDED, FlyingState.mode(p), "and none on the ground");
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
        fly(wukong, 2.8, 4);
        eq(h, FlyingState.ON_CLOUD, FlyingState.mode(wukong), "and at his full speed, on his cloud");
        ServerPlayer plain = as(h, ReincarnationOutcome.BULL_DEMON_KING); // no flight of his own
        plain.setGameMode(GameType.CREATIVE);
        plain.getAbilities().flying = true;
        eq(h, FlyingState.GROUNDED, FlyingState.mode(plain), "creative's flight alone is vanilla's");
        h.succeed();
    }
}
