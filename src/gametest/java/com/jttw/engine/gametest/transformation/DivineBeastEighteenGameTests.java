package com.jttw.engine.gametest.transformation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.gametest.character.CharTest;
import com.jttw.engine.network.c2s.RequestTransformPacket;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.FormCategory;
import com.jttw.engine.transformation.FormClassifier;
import com.jttw.engine.transformation.TransformationAccess;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import com.jttw.engine.transformation.TransformationSchool;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * The four Divine Beasts practise the 18 Transformations: its menu on M once the first node is
 * learned, its forms by level, and nothing of the 36 or the 72.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class DivineBeastEighteenGameTests {

    private static final List<ReincarnationOutcome> BEASTS = List.of(
            ReincarnationOutcome.DRAGON, ReincarnationOutcome.PHOENIX,
            ReincarnationOutcome.TURTLE, ReincarnationOutcome.WHITE_TIGER);

    /** A beast with its 18 Transformations nodes 1..level. */
    private static ServerPlayer beast(GameTestHelper h, ReincarnationOutcome outcome, int level) {
        ServerPlayer p = CharTest.as(h, outcome);
        String branch = branch(p);
        for (int i = 1; i <= level; i++) SkillTreeData.grantNode(p, branch + "_" + i);
        return p;
    }

    /** The beast's 18 Transformations branch, e.g. {@code dr_transform}. */
    private static String branch(ServerPlayer p) {
        return Characters.of(p).skillTree().idPrefix() + "transform";
    }

    /** Sends a request and reports whether the player ended up in that form. */
    private static boolean takes(ServerPlayer p, EntityType<?> type) {
        TransformationManager.demorph(p);
        boolean object = FormClassifier.classify(type, type.create(p.serverLevel())) == FormCategory.OBJECT;
        Tf.send(p, object ? Tf.requestObject(type) : Tf.request(type));
        TransformationData d = TransformationManager.getTransformation(p);
        return d != null && d.getEntityType() == type;
    }

    /** What the M key opens: the menu opens for an art at level 1 or more (TransformationScreen). */
    private static boolean menuOnM(ServerPlayer p) {
        AbilitySlot m = Characters.of(p).fullBar(p).get(3);
        return m.id().equals(branch(p)) && m.type() == AbilityType.MENU
                && TransformationAccess.schoolOf(p) == TransformationSchool.EIGHTEEN
                && TransformationAccess.levelOf(p, SkillTreeData.getUnlockedNodes(p)) >= 1;
    }

    @GameTest(template = PLATFORM)
    public static void eachBeastWithTheNodeOpensTheEighteenOnMAndTakesAForm(GameTestHelper h) {
        for (ReincarnationOutcome o : BEASTS) {
            ServerPlayer p = beast(h, o, 1);
            check(h, menuOnM(p), o.id + ": the 18's menu on M");
            eq(h, "18 Transformations", Characters.of(p).fullBar(p).get(3).displayName(), o.id + ": its name");
            check(h, takes(p, EntityType.COW), o.id + ": neutral land at level 1");
            check(h, !takes(p, EntityType.SQUID), o.id + ": neutral water is level 2");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void eachBeastsLevelsOpenWaterThenAir(GameTestHelper h) {
        for (ReincarnationOutcome o : BEASTS) {
            ServerPlayer p2 = beast(h, o, 2);
            check(h, takes(p2, EntityType.SQUID), o.id + ": neutral water at level 2");
            check(h, !takes(p2, EntityType.BEE), o.id + ": neutral air is level 3");
            ServerPlayer p3 = beast(h, o, 3);
            check(h, takes(p3, EntityType.BEE), o.id + ": neutral air at level 3");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aBeastWithoutTheNodeHasNoMenuAndNoForms(GameTestHelper h) {
        for (ReincarnationOutcome o : BEASTS) {
            ServerPlayer p = beast(h, o, 0);
            check(h, Characters.of(p).fullBar(p).get(3).isEmpty(), o.id + ": nothing on M");
            check(h, !menuOnM(p), o.id + ": no menu");
            check(h, !takes(p, EntityType.COW), o.id + ": no forms");
        }
        h.succeed();
    }

    /** Even at level III, nothing beyond the 18's neutral forms; and another art's nodes do not count. */
    @GameTest(template = PLATFORM)
    public static void aBeastCannotUseTheThirtySixOrSeventyTwo(GameTestHelper h) {
        ServerPlayer target = Tf.player(h);
        CharTest.online(h, target);
        for (ReincarnationOutcome o : BEASTS) {
            ServerPlayer p = beast(h, o, 3);
            check(h, !takes(p, EntityType.ZOMBIE), o.id + ": hostile land (36 III, 72 II)");
            check(h, !takes(p, EntityType.DROWNED), o.id + ": hostile sea (72)");
            check(h, !takes(p, EntityType.PHANTOM), o.id + ": hostile sky (72)");
            check(h, !takes(p, EntityType.WARDEN), o.id + ": boss (72)");
            check(h, !takes(p, EntityType.MINECART), o.id + ": object (72)");
            TransformationManager.demorph(p);
            Tf.send(p, RequestTransformPacket.player(target.getUUID()));
            check(h, TransformationManager.getTransformation(p) == null, o.id + ": player form (72)");

            ServerPlayer other = beast(h, o, 0);
            CharTest.grant(other, "wu_transform_1", "wu_transform_2", "wu_transform_3",
                    "mc_transform_1", "mc_transform_2", "mc_transform_3");
            check(h, !takes(other, EntityType.COW), o.id + ": Wukong's and the Marshal's nodes");
        }
        h.succeed();
    }

    /** The 18 on M leaves the true form on Shift + M. */
    @GameTest(template = PLATFORM)
    public static void theTrueFormStaysOnShiftM(GameTestHelper h) {
        for (ReincarnationOutcome o : BEASTS) {
            ServerPlayer p = beast(h, o, 3);
            PlayableCharacter c = Characters.of(p);
            String prefix = c.skillTree().idPrefix();
            CharTest.grant(p, prefix + "true_1");
            List<AbilitySlot> bar = c.fullBar(p);
            eq(h, branch(p), bar.get(3).id(), o.id + ": M");
            eq(h, prefix + "true", bar.get(7).id(), o.id + ": Shift + M");
        }
        h.succeed();
    }
}
