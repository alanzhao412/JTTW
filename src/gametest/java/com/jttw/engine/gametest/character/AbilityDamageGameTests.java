package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.stats.AbilityDamage;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.projectile.Arrow;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;

/**
 * The Yaoguai are ability spammers: their abilities' blows x1.5, their weapon's x0.6; the Phoenix's
 * abilities x1.3. Read off the incoming-damage event, as the blows land.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class AbilityDamageGameTests {

    /** What a blow of 10 from this source comes to after the rule, inside an ability of {@code caster} or not. */
    private static float land(Pig pig, net.minecraft.world.damagesource.DamageSource source, ServerPlayer caster) {
        LivingIncomingDamageEvent[] e = new LivingIncomingDamageEvent[1];
        Runnable r = () -> {
            e[0] = incoming(pig, source, 10f);
            AbilityDamage.onIncoming(e[0]);
        };
        if (caster != null) AbilityDamage.as(caster, r);
        else r.run();
        return e[0].getAmount();
    }

    /** Whether a blow of 10 from this source to {@code victim} is let through by the PvP guard. */
    private static boolean lands(net.minecraft.world.entity.LivingEntity victim, net.minecraft.world.damagesource.DamageSource source) {
        LivingIncomingDamageEvent e = incoming(victim, source, 10f);
        AbilityDamage.guardPlayers(e);
        return !e.isCanceled();
    }

    /** With PvP off, no ability of one player hurts another, whatever its blow names: the friendly fire a dedicated server showed. */
    @GameTest(template = PLATFORM)
    public static void withPvpOffAbilitiesSpareOtherPlayers(GameTestHelper h) {
        var server = h.getLevel().getServer();
        boolean was = server.isPvpAllowed();
        onEnd(h, () -> server.setPvpAllowed(was));
        // the caster a plain player, whose canHarmPlayer is the game's own (the test players' never harm)
        ServerPlayer caster = new ServerPlayer(server, h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "jttw_pvp_caster"),
                net.minecraft.server.level.ClientInformation.createDefault());
        ServerPlayer friend = CharTest.as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        Pig pig = CharTest.mob(h, EntityType.PIG, 0, 0);
        var ds = h.getLevel().damageSources();
        boolean[] got = new boolean[4];

        server.setPvpAllowed(false);
        AbilityDamage.as(caster, () -> got[0] = lands(friend, ds.magic()));
        AbilityDamage.onBehalfOf(caster, () -> got[1] = lands(friend, ds.explosion(null, null)));
        check(h, !got[0], "an ability's magic, crediting no one, spares him");
        check(h, !got[1], "an explosion credited to no one, on the caster's behalf, spares him");
        check(h, !lands(friend, ds.indirectMagic(caster, caster)), "a blow naming the caster spares him");
        check(h, !AbilityDamage.mayHarm(caster, friend), "the rule: PvP off");
        check(h, AbilityDamage.mayHarm(caster, caster), "the caster himself is his own affair");
        AbilityDamage.as(caster, () -> got[2] = lands(pig, ds.magic()));
        check(h, got[2], "a beast is struck as ever");
        float before = friend.getHealth();
        check(h, !AbilityDamage.hurtBy(caster, friend, ds.magic(), 5f), "hurtBy spares him");
        eq(h, before, friend.getHealth(), "his health untouched");
        var bolt = EntityType.LIGHTNING_BOLT.create(h.getLevel());
        bolt.setCause(caster);
        var struck = new net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent(friend, bolt);
        AbilityDamage.guardPlayersFromLightning(struck);
        check(h, struck.isCanceled(), "his lightning spares him");
        var wild = new net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent(friend, EntityType.LIGHTNING_BOLT.create(h.getLevel()));
        AbilityDamage.guardPlayersFromLightning(wild);
        check(h, !wild.isCanceled(), "a storm's lightning is no one's: it strikes as ever");

        server.setPvpAllowed(true);
        AbilityDamage.as(caster, () -> got[3] = lands(friend, ds.magic()));
        check(h, got[3], "with PvP on, his abilities hurt other players again");
        pig.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void yaoguaiAbilitiesHitHardAndTheirWeaponsModestly(GameTestHelper h) {
        Pig pig = CharTest.mob(h, EntityType.PIG, 0, 0);
        for (ReincarnationOutcome y : new ReincarnationOutcome[]{ReincarnationOutcome.BULL_DEMON_KING, ReincarnationOutcome.SAURIAN_DEMON_KING,
                ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS}) {
            ServerPlayer p = CharTest.as(h, y);
            var ds = h.getLevel().damageSources();
            near(h, 6f, land(pig, ds.playerAttack(p), null), y.id + ": a weapon hit x0.6");
            near(h, 15f, land(pig, ds.playerAttack(p), p), y.id + ": an ability's blow, even as a player attack, x1.5");
            near(h, 15f, land(pig, ds.indirectMagic(p, p), p), y.id + ": an ability's magic x1.5");
            near(h, 15f, land(pig, ds.onFire(), p), y.id + ": an ability's blow credited to no one x1.5");
            Arrow arrow = new Arrow(EntityType.ARROW, h.getLevel());
            arrow.setOwner(p);
            near(h, 6f, land(pig, ds.arrow(arrow, p), null), y.id + ": an arrow is a weapon hit");
            near(h, 10f, land(pig, ds.magic(), null), y.id + ": anything else untouched");
            arrow.discard();
        }
        pig.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theOtherClassesHitAsTheyDidAndThePhoenixCastsHarder(GameTestHelper h) {
        Pig pig = CharTest.mob(h, EntityType.PIG, 0, 0);
        var ds = h.getLevel().damageSources();
        for (ReincarnationOutcome o : new ReincarnationOutcome[]{ReincarnationOutcome.CELESTIAL_PRIMATE, ReincarnationOutcome.THIRD_LOTUS_PRINCE,
                ReincarnationOutcome.DRAGON}) {
            ServerPlayer p = CharTest.as(h, o);
            near(h, 10f, land(pig, ds.playerAttack(p), null), o.id + ": a weapon hit as it was");
            near(h, 10f, land(pig, ds.playerAttack(p), p), o.id + ": an ability as it was");
        }
        ServerPlayer phoenix = CharTest.as(h, ReincarnationOutcome.PHOENIX);
        near(h, 13f, land(pig, ds.onFire(), phoenix), "the Phoenix's fire x1.3");
        near(h, 10f, land(pig, ds.playerAttack(phoenix), null), "its weapon as it was");
        pig.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aBlowStruckInAnAbilitysOwnBodyIsTheAbilitys(GameTestHelper h) {
        Pig pig = CharTest.mob(h, EntityType.PIG, 0, 0);
        ServerPlayer bull = CharTest.as(h, ReincarnationOutcome.BULL_DEMON_KING);
        bull.getPersistentData().putBoolean(com.jttw.engine.character.bulldemon.BullDemonAbilityHandler.KEY_GREAT_BULL, true);
        check(h, AbilityDamage.inAbilityForm(bull), "in the Great White Bull");
        near(h, 15f, land(pig, h.getLevel().damageSources().playerAttack(bull), null), "its blow is the ability's: x1.5");
        bull.getPersistentData().remove(com.jttw.engine.character.bulldemon.BullDemonAbilityHandler.KEY_GREAT_BULL);
        pig.discard();
        h.succeed();
    }
}
