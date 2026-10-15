package com.jttw.engine.gametest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/** Abilities Wukong earns through the story rather than the skill tree. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class WukongStoryGameTests {

    @GameTest(template = PLATFORM)
    public static void goldenEyesTakeSlotNOnceGranted(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        LevelManager.initClass(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        List<AbilitySlot> before = WukongCharacter.INSTANCE.skillBar(p);
        check(h, before.get(2).isEmpty(), "slot N empty before the eyes");
        ImmortalItems.grantFieryGoldenEyes(p);
        check(h, ImmortalItems.hasFieryGoldenEyes(p), "eyes granted");
        eq(h, WukongCharacter.GOLDEN_EYES, WukongCharacter.INSTANCE.skillBar(p).get(2), "slot N");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void otherCharactersNeverGetTheGoldenEyesSlot(GameTestHelper h) {
        ServerPlayer p = player(h);
        AlignmentData.markReincarnated(p, ReincarnationOutcome.YUAN_DI);
        LevelManager.initClass(p, ReincarnationOutcome.YUAN_DI);
        ImmortalItems.grantFieryGoldenEyes(p);
        List<AbilitySlot> bar = com.jttw.engine.character.Characters.of(p).skillBar(p);
        check(h, !bar.contains(WukongCharacter.GOLDEN_EYES), "Yuan Di's bar");
        h.succeed();
    }
}
