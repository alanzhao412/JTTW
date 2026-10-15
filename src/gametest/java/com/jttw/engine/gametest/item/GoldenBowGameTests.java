package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.Armory;
import com.jttw.engine.item.GoldenBowItem;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/** Erlang Shen's Golden Bow: one of his armaments, its arrows of light striking by his power. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class GoldenBowGameTests {

    @GameTest(template = PLATFORM)
    public static void theBowIsOneOfErlangsArmaments(GameTestHelper h) {
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        check(h, Armory.slots(erlang).stream().anyMatch(a -> a.id().equals("golden_bow")), "in his armory");
        ServerPlayer nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        check(h, Armory.slots(nezha).stream().noneMatch(a -> a.id().equals("golden_bow")), "not Nezha's");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void itsArrowsStrikeByHisPower(GameTestHelper h) {
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        var low = GoldenBowItem.loose(erlang, 1f);
        PlayerLevel.setLevel(erlang, 60);
        var high = GoldenBowItem.loose(erlang, 1f);
        onEnd(h, () -> { low.discard(); high.discard(); });
        check(h, low.getPersistentData().getBoolean(GoldenBowItem.KEY_GOLDEN), "an arrow of light");
        check(h, high.getBaseDamage() > low.getBaseDamage() * 5, "far stronger at level 60: " + low.getBaseDamage() + " -> " + high.getBaseDamage());
        eq(h, GoldenBowItem.ARROW_DAMAGE * LevelPower.of(erlang), high.getBaseDamage(), "his power, exactly");
        h.succeed();
    }
}
