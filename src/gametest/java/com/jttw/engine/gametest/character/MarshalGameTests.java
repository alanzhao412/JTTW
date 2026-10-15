package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.marshal.MarshalAbilityHandler;
import com.jttw.engine.character.marshal.MarshalCharacter;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** The Marshal of the Heavenly Canopy: belly slam, truffle snout, stomachs, admiralty, rake. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class MarshalGameTests {

    /** The Marshal at (2,2,2) facing south (+z), on the ground, with Qi to spend. */
    private static ServerPlayer marshal(GameTestHelper h, String... nodes) {
        ServerPlayer p = as(h, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        p.setYRot(0f); p.setXRot(0f); p.yHeadRot = 0f;
        p.setOnGround(true);
        qi(p, 200f, 200f);
        grant(p, nodes);
        return p;
    }

    private static void key(ServerPlayer p, int slot) {
        MarshalCharacter.INSTANCE.activate(p, slot, AbilityAction.USE);
    }

    private static void at(GameTestHelper h, ServerPlayer p, double x, double z) {
        Vec3 v = h.absoluteVec(new Vec3(x, 2, z));
        p.moveTo(v.x, v.y, v.z, 0f, 0f);
    }

    private static void fill(ServerPlayer p, int food, int reserve) {
        p.getFoodData().setFoodLevel(food);
        MarshalAbilityHandler.setReserve(p, reserve);
    }

    @GameTest(template = PLATFORM)
    public static void theOutcomeIsTheMarshalWithSlamSnoutAndTheMenuOnM(GameTestHelper h) {
        ServerPlayer p = marshal(h);
        eq(h, MarshalCharacter.INSTANCE, Characters.of(p), "character");
        List<AbilitySlot> bar = MarshalCharacter.INSTANCE.skillBar(Set.of("mc_slam_1", "mc_snout_1", "mc_transform_1"));
        eq(h, "mc_slam", bar.get(0).id(), "V");
        eq(h, "mc_snout", bar.get(1).id(), "B");
        check(h, bar.get(2).isEmpty(), "N empty");
        eq(h, "mc_transform", bar.get(3).id(), "M");
        eq(h, AbilityType.MENU, bar.get(3).type(), "M is the menu");
        h.succeed();
    }

    // ── V: Belly Slam ──────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void slamLeapsForwardFromTheGroundAndDropsFromTheAir(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_slam_1");
        key(p, 0);
        Vec3 v = p.getDeltaMovement();
        check(h, v.y > 0.5 && v.z > 0.5 && Math.abs(v.x) < 1e-6, "leaps up and forward (south): " + v);
        check(h, p.getPersistentData().getBoolean(MarshalAbilityHandler.KEY_SLAMMING), "the slam is live");
        near(h, 200f - MarshalAbilityHandler.SLAM_QI, QiData.getCurrent(p), "Qi spent");

        key(p, 0);
        near(h, 200f - MarshalAbilityHandler.SLAM_QI, QiData.getCurrent(p), "cooling down: nothing spent");

        p.getPersistentData().remove("jttw_mc_slam_ready_at");
        p.setOnGround(false);
        key(p, 0);
        check(h, p.getDeltaMovement().y <= -1.9, "in the air, drops like a stone: " + p.getDeltaMovement());
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void withoutTheNodeSlamDoesNothing(GameTestHelper h) {
        ServerPlayer p = marshal(h);
        key(p, 0);
        check(h, !p.getPersistentData().getBoolean(MarshalAbilityHandler.KEY_SLAMMING), "no slam");
        near(h, 200f, QiData.getCurrent(p), "no Qi spent");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theQuakeGrowsWithHowFullHeIs(GameTestHelper h) {
        near(h, 3.0, MarshalAbilityHandler.slamRadius(0f, 1), "hungry reach");
        near(h, 6.0, MarshalAbilityHandler.slamRadius(1f, 1), "full reach");
        near(h, 8.0, MarshalAbilityHandler.slamRadius(1f, 3), "full reach at III");
        near(h, 8.0, MarshalAbilityHandler.slamDamage(10f, 0f, 1), "hungry damage");
        near(h, 20.0, MarshalAbilityHandler.slamDamage(10f, 1f, 1), "full damage");
        near(h, 30.0, MarshalAbilityHandler.slamDamage(10f, 1f, 3), "full damage at III");

        // Corner to corner is about 5.7 blocks: out of a hungry quake's reach, inside a full one's
        ServerPlayer p = marshal(h, "mc_slam_1", "mc_stomach_1");
        at(h, p, 0.5, 0.5);
        Zombie near = mob(h, EntityType.ZOMBIE, 2, 1);
        Zombie far = mob(h, EntityType.ZOMBIE, 4, 4);
        List<Hit> nearLog = watch(near), farLog = watch(far);

        fill(p, 0, 0);
        MarshalAbilityHandler.slamImpact(p);
        check(h, !of(nearLog, DamageTypes.MOB_ATTACK).isEmpty(), "hungry: the near zombie is quaked");
        check(h, of(farLog, DamageTypes.MOB_ATTACK).isEmpty(), "hungry: the far one is not");
        check(h, near.getDeltaMovement().y > 0.3, "the quaked are thrown up");
        check(h, near.getDeltaMovement().x > 0, "and outward");

        fill(p, 20, 20);
        MarshalAbilityHandler.slamImpact(p);
        check(h, !of(farLog, DamageTypes.MOB_ATTACK).isEmpty(), "full: the far zombie is quaked");
        check(h, !h.getBlockState(new BlockPos(2, 1, 1)).isAir(), "nothing broken");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void slamTwoStaggersAndNoSlamTakesFallDamage(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_slam_1", "mc_slam_2");
        var z = mob(h, EntityType.IRON_GOLEM, 2, 4); // survives the quake to show it
        key(p, 0);
        LivingFallEvent fall = new LivingFallEvent(p, 12f, 1f);
        MarshalAbilityHandler.onFall(fall);
        check(h, fall.isCanceled(), "a slam lands on its belly: no fall damage");
        MarshalAbilityHandler.slamImpact(p);
        eq(h, 1, amp(z, MobEffects.MOVEMENT_SLOWDOWN), "II: staggered (Slowness II)");
        check(h, !p.getPersistentData().getBoolean(MarshalAbilityHandler.KEY_SLAMMING), "the slam is over");
        h.succeed();
    }

    // ── B: Truffle Snout ───────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "marshalSnout")
    public static void snoutOneSmellsCreaturesButNoTreasure(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_snout_1");
        Zombie z = mob(h, EntityType.ZOMBIE, 2, 4);
        h.setBlock(new BlockPos(4, 1, 4), Blocks.DIAMOND_ORE);
        MarshalAbilityHandler.Scent scent = MarshalAbilityHandler.sniff(p);
        check(h, scent != null, "sniffed");
        check(h, scent.entityIds().contains(z.getId()), "smells the zombie");
        check(h, !scent.entityIds().contains(p.getId()), "not himself");
        check(h, scent.blocks().isEmpty(), "I: no treasure");
        eq(h, 200, scent.ticks(), "10 seconds");
        check(h, MarshalAbilityHandler.sniff(p) == null, "cooling down");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "marshalSnout2")
    public static void snoutTwoSmellsOresAndChests(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_snout_1", "mc_snout_2");
        BlockPos ore = new BlockPos(4, 1, 4), chest = new BlockPos(0, 2, 4);
        h.setBlock(ore, Blocks.DIAMOND_ORE);
        h.setBlock(chest, Blocks.CHEST);
        MarshalAbilityHandler.Scent scent = MarshalAbilityHandler.sniff(p);
        int oreAt = scent.blocks().indexOf(h.absolutePos(ore));
        int chestAt = scent.blocks().indexOf(h.absolutePos(chest));
        check(h, oreAt >= 0, "smells the diamond ore");
        eq(h, 0xFF00BBFF, scent.colours().get(oreAt), "drawn diamond blue");
        check(h, chestAt >= 0, "smells the chest");
        eq(h, MarshalAbilityHandler.COLOUR_CONTAINER, scent.colours().get(chestAt), "drawn gold");
        check(h, !scent.blocks().contains(h.absolutePos(new BlockPos(2, 1, 2))), "plain floor is no treasure");
        eq(h, 16, MarshalAbilityHandler.treasureRange(2), "II reach");
        eq(h, 32, MarshalAbilityHandler.treasureRange(3), "III reach");
        eq(h, 400, MarshalAbilityHandler.scentTicks(3), "III lingers 20 seconds");
        h.succeed();
    }

    // ── Bottomless Stomach ─────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void heCanAlwaysEatOthersCannotWhenFull(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_stomach_1");
        fill(p, 20, 20);
        check(h, p.canEat(false), "the Marshal eats however full");
        ServerPlayer plain = marshal(h);
        plain.getFoodData().setFoodLevel(20);
        check(h, !plain.canEat(false), "not without the stomach");
        ServerPlayer other = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        other.getFoodData().setFoodLevel(20);
        check(h, !other.canEat(false), "nor anyone else");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void eatingPastFullFillsTheSecondStomach(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_stomach_1");
        fill(p, 16, 0);
        p.eat(p.level(), new ItemStack(Items.COOKED_BEEF)); // 8 food: 4 fit, 4 overflow
        eq(h, 20, p.getFoodData().getFoodLevel(), "first stomach full");
        eq(h, 4, MarshalAbilityHandler.reserve(p), "the overflow is in the second");

        ServerPlayer other = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        other.getFoodData().setFoodLevel(16);
        other.eat(other.level(), new ItemStack(Items.COOKED_BEEF));
        eq(h, 0, MarshalAbilityHandler.reserve(other), "nobody else has a second stomach");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void whenBothStomachsAreFullTheFeastHeals(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_stomach_1");
        fill(p, 20, 18);
        p.setHealth(6f);
        p.eat(p.level(), new ItemStack(Items.COOKED_BEEF)); // 2 fill the second stomach, 6 heal
        eq(h, 20, MarshalAbilityHandler.reserve(p), "second stomach full");
        near(h, 12f, p.getHealth(), "6 surplus heals 6");

        ServerPlayer two = marshal(h, "mc_stomach_1", "mc_stomach_2");
        fill(two, 20, 20);
        two.setHealth(4f);
        two.eat(two.level(), new ItemStack(Items.COOKED_BEEF));
        near(h, 16f, two.getHealth(), "II: 8 surplus heals 12");

        ServerPlayer three = marshal(h, "mc_stomach_1", "mc_stomach_2", "mc_stomach_3");
        fill(three, 20, 20);
        three.setHealth(three.getMaxHealth() - 2f);
        three.eat(three.level(), new ItemStack(Items.COOKED_BEEF));
        near(h, three.getMaxHealth(), three.getHealth(), "III: healed to full");
        near(h, 14f, three.getAbsorptionAmount(), "and the rest of 16 as golden hearts");

        ServerPlayer strong = marshal(h, "mc_stomach_1");
        PlayerLevel.setLevel(strong, 20);
        LevelManager.reapplyStats(strong);
        fill(strong, 20, 20);
        strong.setHealth(4f);
        strong.eat(strong.level(), new ItemStack(Items.COOKED_BEEF));
        near(h, 4 + 8 * LevelPower.of(strong), strong.getHealth(), "level 20: 8 surplus heals 8 at his power");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theSecondStomachFeedsTheFirst(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_stomach_1");
        fill(p, 10, 15);
        tick(p, MarshalAbilityHandler::onPlayerTick, 1);
        eq(h, 20, p.getFoodData().getFoodLevel(), "first stomach topped up");
        eq(h, 5, MarshalAbilityHandler.reserve(p), "from the second");
        h.succeed();
    }

    // ── Admiral of the Heavenly River ──────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theAdmiralSwimsFastAndBreathesUnderwater(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_admiral_1");
        tick(p, MarshalAbilityHandler::onPlayerTick, 1);
        near(h, 0.5, modifier(p, NeoForgeMod.SWIM_SPEED, "mc_admiral_swim"), "I: +50% swim");
        h.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
        h.setBlock(new BlockPos(2, 3, 2), Blocks.WATER);
        p.setAirSupply(0);
        tick(p, MarshalAbilityHandler::onPlayerTick, 1);
        eq(h, p.getMaxAirSupply(), p.getAirSupply(), "breathes underwater");
        grant(p, "mc_admiral_2");
        tick(p, MarshalAbilityHandler::onPlayerTick, 20);
        near(h, 1.0, modifier(p, NeoForgeMod.SWIM_SPEED, "mc_admiral_swim"), "II: +100% swim");
        check(h, p.hasEffect(MobEffects.NIGHT_VISION), "II: sees underwater");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theAdmiralsFleetSwimsWithHim(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_admiral_1", "mc_admiral_2", "mc_admiral_3");
        ServerPlayer mate = player(h);
        online(h, mate);
        ServerPlayer ashore = player(h);
        online(h, ashore);
        ashore.moveTo(h.absoluteVec(new Vec3(4.5, 2, 4.5)));
        h.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
        h.setBlock(new BlockPos(2, 3, 2), Blocks.WATER);
        tick(p, MarshalAbilityHandler::onPlayerTick, 20);
        check(h, mate.hasEffect(MobEffects.DOLPHINS_GRACE), "the fleet swims fast");
        check(h, mate.hasEffect(MobEffects.WATER_BREATHING), "and breathes");
        check(h, !ashore.hasEffect(MobEffects.DOLPHINS_GRACE), "not those ashore");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void othersLoseTheAdmiralsSwimBonus(GameTestHelper h) {
        ServerPlayer p = marshal(h, "mc_admiral_1");
        tick(p, MarshalAbilityHandler::onPlayerTick, 1);
        p.getPersistentData().putString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME,
                ReincarnationOutcome.CELESTIAL_PRIMATE.id);
        tick(p, MarshalAbilityHandler::onPlayerTick, 20);
        check(h, modifier(p, NeoForgeMod.SWIM_SPEED, "mc_admiral_swim") == null, "bonus gone");
        h.succeed();
    }

    // ── Nine-Tooth Rake ────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void anyoneTillsThreeByThreeWithTheRake(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NINE_TOOTH_RAKE.get()));
        p.moveTo(h.absoluteVec(new Vec3(2.5, 2, 4.5)));
        BlockPos centre = h.absolutePos(new BlockPos(2, 1, 2));
        p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(centre).add(0, 0.5, 0), Direction.UP, centre, false)));
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) {
            check(h, h.getBlockState(new BlockPos(x, 1, z)).is(Blocks.FARMLAND), "tilled " + x + "," + z);
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theMarshalsRakeBurstsOncePerSwingAndBreaksNothing(GameTestHelper h) {
        ServerPlayer p = marshal(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NINE_TOOTH_RAKE.get()));
        Zombie struck = mob(h, EntityType.ZOMBIE, 2, 3);
        Zombie beside = mob(h, EntityType.ZOMBIE, 3, 4);
        List<Hit> struckLog = watch(struck), besideLog = watch(beside);
        p.attack(struck);
        Hit blow = firstHit(h, struckLog, DamageTypes.PLAYER_ATTACK, struck);
        check(h, !of(besideLog, DamageTypes.PLAYER_EXPLOSION).isEmpty(), "the burst hits the one beside");
        int besideHits = besideLog.size();
        struck.invulnerableTime = 0; // let the second blow land, so only the swing gate can stop the burst
        p.attack(struck);
        check(h, of(struckLog, DamageTypes.PLAYER_ATTACK).size() == 2, "the second blow landed");
        eq(h, besideHits, besideLog.size(), "a second, uncharged swing does not burst");
        check(h, !h.getBlockState(new BlockPos(2, 1, 3)).isAir(), "the floor is unbroken");
        check(h, blow.amount() > 0, "the blow landed");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void inOtherHandsTheRakeDoesNotBurst(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NINE_TOOTH_RAKE.get()));
        Zombie struck = mob(h, EntityType.ZOMBIE, 2, 3);
        Zombie beside = mob(h, EntityType.ZOMBIE, 3, 4);
        List<Hit> besideLog = watch(beside);
        p.attack(struck);
        check(h, besideLog.isEmpty(), "no burst for anyone else");
        h.succeed();
    }
}
