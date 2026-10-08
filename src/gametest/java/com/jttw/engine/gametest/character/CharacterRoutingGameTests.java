package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.erlang.ErlangShenAbilityHandler;
import com.jttw.engine.character.marshal.MarshalCharacter;
import com.jttw.engine.character.peng.PengCharacter;
import com.jttw.engine.character.yuandi.YuanDiAbilityHandler;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Which character a key press reaches, and the characters that have no abilities yet. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class CharacterRoutingGameTests {

    private static void pressEverything(PlayableCharacter c, ServerPlayer p) {
        for (int slot = -1; slot <= PlayableCharacter.SLOT_COUNT; slot++) {
            for (AbilityAction action : AbilityAction.values()) c.activate(p, slot, action);
        }
    }

    private static void assertKeysDoNothing(GameTestHelper h, ReincarnationOutcome outcome, PlayableCharacter expected,
                                            String... nodes) {
        ServerPlayer p = as(h, outcome);
        grant(p, nodes);
        qi(p, 100f, 100f);
        PlayableCharacter c = Characters.of(p);
        eq(h, expected, c, "character for " + outcome.id);
        CompoundTag before = p.getPersistentData().copy();
        int entities = near(p, LivingEntity.class, 16).size();
        pressEverything(c, p);
        eq(h, before, p.getPersistentData(), outcome.id + ": saved data unchanged");
        eq(h, entities, near(p, LivingEntity.class, 16).size(), outcome.id + ": nothing spawned");
        near(h, 100f, QiData.getCurrent(p), outcome.id + ": Qi unchanged");
    }

    @GameTest(template = PLATFORM, batch = "pengHasNoAbilitiesSoEveryKeyDoesNothing")
    public static void pengHasNoAbilitiesSoEveryKeyDoesNothing(GameTestHelper h) {
        assertKeysDoNothing(h, ReincarnationOutcome.GOLD_WINGED_PENG, PengCharacter.INSTANCE);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "marshalsMenuNeverReachesTheServer")
    public static void marshalsMenuNeverReachesTheServer(GameTestHelper h) {
        // the 36 Transformations slot is a menu opened on the client; his other keys need their own nodes
        assertKeysDoNothing(h, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY, MarshalCharacter.INSTANCE,
                "mc_transform_1", "mc_transform_2", "mc_transform_3");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "everyReincarnatedPlayerResolvesToTheirOwnCharacter")
    public static void everyReincarnatedPlayerResolvesToTheirOwnCharacter(GameTestHelper h) {
        for (ReincarnationOutcome o : new ReincarnationOutcome[]{
                ReincarnationOutcome.CELESTIAL_PRIMATE, ReincarnationOutcome.YUAN_DI,
                ReincarnationOutcome.GOLD_WINGED_PENG, ReincarnationOutcome.EYE_OF_HEAVEN,
                ReincarnationOutcome.THIRD_LOTUS_PRINCE, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY,
                ReincarnationOutcome.DRAGON, ReincarnationOutcome.PHOENIX,
                ReincarnationOutcome.WHITE_TIGER, ReincarnationOutcome.TURTLE}) {
            ServerPlayer p = as(h, o);
            PlayableCharacter c = Characters.of(p);
            check(h, c != null, o.id + " has a character");
            eq(h, o.id, c.outcomeId(), "character for " + o.id);
            eq(h, c, Characters.byOutcome(o.id), "byOutcome agrees for " + o.id);
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "nodesFromAnotherCharactersTreeDoNotFireThroughYourKeys")
    public static void nodesFromAnotherCharactersTreeDoNotFireThroughYourKeys(GameTestHelper h) {
        // a Yuan Di player who somehow holds Erlang Shen's Eye of Heaven node
        ServerPlayer p = as(h, ReincarnationOutcome.YUAN_DI);
        qi(p, 100f, 100f);
        grant(p, "es_eye_1", "wu_ring_1", "wu_clone_1");
        PlayableCharacter c = Characters.of(p);
        pressEverything(c, p);
        check(h, !ErlangShenAbilityHandler.isActive(p.getUUID()), "Erlang's eye stays closed");
        check(h, !YuanDiAbilityHandler.isTransformed(p), "no Yuan Di node, no Yuan Di ability");
        near(h, 100f, QiData.getCurrent(p), "nothing spent");
        h.succeed();
    }
}
