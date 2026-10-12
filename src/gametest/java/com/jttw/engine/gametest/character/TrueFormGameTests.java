package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.divinebeast.TrueForm;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.registry.ModEffects;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** A divine beast's true form: taken by its key, paid for, mighty while it lasts, then exhaustion. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class TrueFormGameTests {

    private static ServerPlayer byakko(GameTestHelper h, String... nodes) {
        ServerPlayer p = as(h, ReincarnationOutcome.WHITE_TIGER);
        qi(p, 100f, 100f);
        grant(p, nodes);
        return p;
    }

    @GameTest(template = PLATFORM, batch = "trueForm")
    public static void theWhiteTigerIsTakenPaidForAndMighty(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        var c = Characters.of(p);
        c.activate(p, 3, AbilityAction.USE);
        check(h, !TrueForm.active(p), "not before it is learned");
        grant(p, "bk_true_1");
        float health = p.getMaxHealth();
        double attack = p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        c.activate(p, 3, AbilityAction.USE);
        check(h, TrueForm.active(p), "M: the White Tiger");
        eq(h, ModEntities.WHITE_TIGER.get(), TransformationManager.getTransformation(p).getEntityType(), "in its shape");
        near(h, 60f, QiData.getCurrent(p), "two fifths of its Qi");
        near(h, health * 3, p.getMaxHealth(), "three times the health");
        near(h, p.getMaxHealth(), p.getHealth(), "and all of it");
        near(h, attack * 2.5, p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), "two and a half times the strength");
        c.activate(p, 3, AbilityAction.USE); // ended early
        check(h, !TrueForm.active(p), "its key ends it");
        eq(h, null, TransformationManager.getTransformation(p), "back to himself");
        near(h, health, p.getMaxHealth(), "his own health again");
        check(h, TrueForm.exhausted(p), "and exhausted");
        float qi = QiData.getCurrent(p);
        c.activate(p, 0, AbilityAction.USE);
        near(h, qi, QiData.getCurrent(p), "exhausted, no ability answers");
        p.removeEffect(ModEffects.EXHAUSTED.get());
        c.activate(p, 3, AbilityAction.USE);
        check(h, !TrueForm.active(p), "it rests ten minutes from its taking");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "trueFormTimesOut", timeoutTicks = 500)
    public static void theTrueFormRunsOutOnItsOwn(GameTestHelper h) {
        ServerPlayer p = byakko(h, "bk_true_1");
        TrueForm.toggle(p, ModEntities.WHITE_TIGER.get(), 1, "bk_true");
        h.runAfterDelay(TrueForm.seconds(1) * 20 + 5, () -> {
            tick(p, TrueForm::onPlayerTick, 1);
            check(h, !TrueForm.active(p), "after 20 s it is over");
            check(h, TrueForm.exhausted(p), "and he is exhausted");
            h.succeed();
        });
    }

    /** Each divine beast's key takes its own true form: the Dragon (Shift+B), Turtle (Shift+B), Phoenix (M). */
    @GameTest(template = PLATFORM, batch = "trueFormsOfEach")
    public static void eachBeastTakesItsOwnTrueForm(GameTestHelper h) {
        Object[][] cases = {
                {ReincarnationOutcome.DRAGON, "dr_true_1", 5, ModEntities.AZURE_DRAGON.get()},
                {ReincarnationOutcome.TURTLE, "tu_true_1", 5, ModEntities.BLACK_TORTOISE.get()},
                {ReincarnationOutcome.PHOENIX, "ph_true_1", 3, ModEntities.VERMILION_BIRD.get()}};
        for (Object[] c : cases) {
            ServerPlayer p = as(h, (ReincarnationOutcome) c[0]);
            qi(p, 100f, 100f);
            grant(p, (String) c[1]);
            Characters.of(p).activate(p, (Integer) c[2], AbilityAction.USE);
            check(h, TrueForm.active(p), c[0] + ": its key takes the true form");
            eq(h, c[3], TransformationManager.getTransformation(p).getEntityType(), c[0] + ": in its own shape");
            TrueForm.end(p);
        }
        h.succeed();
    }
}
