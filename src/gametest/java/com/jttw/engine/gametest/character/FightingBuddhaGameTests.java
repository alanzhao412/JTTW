package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.wukong.BuddhaBreaking;
import com.jttw.engine.character.wukong.BuddhaVanish;
import com.jttw.engine.character.wukong.FightingBuddha;
import com.jttw.engine.character.wukong.SomersaultTravel;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.registry.ModEffects;
import com.jttw.engine.world.Stun;
import com.jttw.engine.world.TerrainBreaker;
import com.jttw.engine.world.tough.Carving;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/**
 * The Victorious Fighting Buddha, Wukong at level 100: nothing harms him, the void included (he
 * stays in it unharmed); he vanishes from creatures; his abilities break anything, bedrock and the
 * technical blocks too; Somersault Anywhere finds a safe place to stand by a player in another realm
 * or at typed coordinates; and none of it is anyone else's, nor his below level 100.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FightingBuddhaGameTests {

    private static ServerPlayer wukong(GameTestHelper h, int level) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(p, level);
        return p;
    }

    private static ServerPlayer buddha(GameTestHelper h) {
        ServerPlayer p = wukong(h, 100);
        p.getPersistentData().putBoolean(FightingBuddha.KEY_ANNOUNCED, true);
        return p;
    }

    /** Every kind of harm there is, by name. */
    private static Map<String, DamageSource> everyHarm(GameTestHelper h, ServerPlayer victim) {
        ServerLevel level = h.getLevel();
        DamageSources s = level.damageSources();
        ServerPlayer other = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        Zombie zombie = mob(h, EntityType.ZOMBIE, 1, 1);
        var wither = EntityType.WITHER.create(level);
        Map<String, DamageSource> m = new LinkedHashMap<>();
        m.put("another player", s.playerAttack(other));
        m.put("a mob", s.mobAttack(zombie));
        m.put("a boss", s.mobAttack(wither));
        m.put("an arrow", s.arrow(null, zombie));
        m.put("fire", s.inFire());
        m.put("burning", s.onFire());
        m.put("lava", s.lava());
        m.put("hot floor", s.hotFloor());
        m.put("a fall", s.fall());
        m.put("drowning", s.drown());
        m.put("the void", s.fellOutOfWorld());
        m.put("magic (and poison)", s.magic());
        m.put("indirect magic", s.indirectMagic(zombie, other));
        m.put("wither", s.wither());
        m.put("an explosion", s.explosion(null, null));
        m.put("a player's explosion", s.explosion(other, other));
        m.put("lightning", s.lightningBolt());
        m.put("the sonic boom", s.sonicBoom(zombie));
        m.put("freezing", s.freeze());
        m.put("starving", s.starve());
        m.put("a cactus", s.cactus());
        m.put("suffocation", s.inWall());
        m.put("thorns", s.thorns(zombie));
        m.put("dragon breath", s.dragonBreath());
        m.put("generic", s.generic());
        if (wither != null) wither.discard();
        return m;
    }

    @GameTest(template = PLATFORM, batch = "fightingBuddhaRefusesEveryKindOfDamage")
    public static void fightingBuddhaRefusesEveryKindOfDamage(GameTestHelper h) {
        ServerPlayer p = buddha(h);
        check(h, FightingBuddha.is(p), "Wukong at level 100 is the Fighting Buddha");
        List<String> landed = new ArrayList<>();
        for (var e : everyHarm(h, p).entrySet()) {
            boolean cancelled = NeoForge.EVENT_BUS.post(incoming(p, e.getValue(), 50f)).isCanceled();
            boolean invulnerable = NeoForge.EVENT_BUS.post(new EntityInvulnerabilityCheckEvent(p, e.getValue(), false)).isInvulnerable();
            boolean deathStopped = NeoForge.EVENT_BUS.post(new LivingDeathEvent(p, e.getValue())).isCanceled();
            if (!cancelled || !invulnerable || !deathStopped) landed.add(e.getKey());
        }
        check(h, landed.isEmpty(), "nothing harms him, but these landed: " + landed);
        // only an operator's /kill
        DamageSource kill = h.getLevel().damageSources().genericKill();
        check(h, !NeoForge.EVENT_BUS.post(incoming(p, kill, Float.MAX_VALUE)).isCanceled(), "/kill still kills him");
        check(h, !NeoForge.EVENT_BUS.post(new EntityInvulnerabilityCheckEvent(p, kill, false)).isInvulnerable(), "/kill is not refused");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "fightingBuddhaIsNotFlinchedPoisonedStunnedOrHurt")
    public static void fightingBuddhaIsNotFlinchedPoisonedStunnedOrHurt(GameTestHelper h) {
        ServerPlayer p = buddha(h);
        check(h, NeoForge.EVENT_BUS.post(new LivingKnockBackEvent(p, 2f, 1, 0)).isCanceled(), "no knockback");
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
        p.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
        Stun.stun(p, 100, null);
        Stun.pin(p, 100, null);
        check(h, !p.hasEffect(MobEffects.POISON) && !p.hasEffect(MobEffects.WITHER) && !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "no harmful effect takes hold");
        check(h, !p.hasEffect(ModEffects.STUNNED) && !p.hasEffect(ModEffects.PINNED), "neither Stunned nor Pinned holds him");
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
        check(h, p.hasEffect(MobEffects.DAMAGE_BOOST), "a helpful effect still does");
        // whatever slips past the events (health set straight down, fire, air) is undone each tick
        p.setHealth(1f);
        p.igniteForSeconds(5);
        p.setAirSupply(-10);
        tick(p, (PlayerTickEvent.Post e) -> FightingBuddha.onPlayerTick(e), 1);
        near(h, p.getMaxHealth(), p.getHealth(), "health back to full");
        check(h, p.getRemainingFireTicks() <= 0 && p.getAirSupply() == p.getMaxAirSupply(), "no fire, full air");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "fightingBuddhaStaysInTheVoidUnharmed")
    public static void fightingBuddhaStaysInTheVoidUnharmed(GameTestHelper h) {
        ServerPlayer p = buddha(h);
        double deep = h.getLevel().getMinBuildHeight() - 200;
        p.moveTo(p.getX(), deep, p.getZ());
        DamageSource voidDamage = h.getLevel().damageSources().fellOutOfWorld();
        check(h, NeoForge.EVENT_BUS.post(incoming(p, voidDamage, 4f)).isCanceled(), "the void does not harm him");
        tick(p, (PlayerTickEvent.Post e) -> FightingBuddha.onPlayerTick(e), 40);
        near(h, deep, p.getY(), "he is left where he is in the void (no rescue)");
        near(h, p.getMaxHealth(), p.getHealth(), "unharmed");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "fightingBuddhaVanishedNoMobTargetsHim")
    public static void fightingBuddhaVanishedNoMobTargetsHim(GameTestHelper h) {
        ServerPlayer p = buddha(h);
        ServerPlayer watcher = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        Zombie zombie = mob(h, EntityType.ZOMBIE, 2, 4);
        zombie.setTarget(p);
        check(h, zombie.getTarget() == p, "seen, the zombie hunts him");
        check(h, !BuddhaVanish.hiddenFrom(p, watcher), "seen by other players");

        WukongCharacter.INSTANCE.use(p, FightingBuddha.VANISH, com.jttw.engine.character.AbilityAction.USE);
        check(h, BuddhaVanish.isVanished(p), "Shift + B vanishes him");
        check(h, zombie.getTarget() == null, "the zombie lets him go");
        zombie.setTarget(p);
        check(h, zombie.getTarget() == null, "and cannot take him as its target again");
        check(h, BuddhaVanish.hiddenFrom(p, watcher), "no other player's game is told he is there");
        check(h, !BuddhaVanish.hiddenFrom(p, p), "he still sees himself");
        check(h, p.isSilent(), "no sound of his is heard");

        WukongCharacter.INSTANCE.use(p, FightingBuddha.VANISH, com.jttw.engine.character.AbilityAction.USE);
        check(h, !BuddhaVanish.isVanished(p) && !p.isSilent(), "pressed again, he appears");
        zombie.setTarget(p);
        check(h, zombie.getTarget() == p, "seen again, he can be hunted again");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "fightingBuddhaBreaksBedrockAndAnyBlock")
    public static void fightingBuddhaBreaksBedrockAndAnyBlock(GameTestHelper h) {
        ServerPlayer p = buddha(h);
        ServerLevel level = h.getLevel();
        var blocks = List.of(Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.REINFORCED_DEEPSLATE, Blocks.BARRIER,
                Blocks.COMMAND_BLOCK, Blocks.END_PORTAL_FRAME, Blocks.STRUCTURE_BLOCK,
                com.jttw.engine.registry.ModBlocks.BOTTOMLESS_STONE.get());
        List<BlockPos> at = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos pos = h.absolutePos(new BlockPos(i % 4, 3 + i / 4, 4));
            level.setBlock(pos, blocks.get(i).defaultBlockState(), 3);
            at.add(pos);
        }
        // nothing costs him more than stone to carve
        check(h, Carving.cost(level, at.get(0), level.getBlockState(at.get(0)), BuddhaBreaking.strength(p)) <= Carving.STONE,
                "bedrock costs him no more than stone");
        TerrainBreaker.breakBlocks(level, at, p);
        TerrainBreaker.work(level, 4096);
        List<String> stood = new ArrayList<>();
        for (BlockPos pos : at) if (!level.getBlockState(pos).isAir()) stood.add(level.getBlockState(pos).getBlock().getName().getString());
        check(h, stood.isEmpty(), "his blow breaks everything, but these stood: " + stood);

        // his blast too: bedrock in it goes
        BlockPos rock = h.absolutePos(new BlockPos(2, 3, 4));
        level.setBlock(rock, Blocks.BEDROCK.defaultBlockState(), 3);
        TerrainBreaker.explode(level, p, rock.getX() + 0.5, rock.getY() + 0.5, rock.getZ() - 0.5, 4f);
        TerrainBreaker.work(level, 4096);
        check(h, level.getBlockState(rock).isAir(), "his blast breaks bedrock");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "bedrockHoldsAgainstWukongBelowOneHundredAndOthers")
    public static void bedrockHoldsAgainstWukongBelowOneHundredAndOthers(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer young = wukong(h, 99);
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(erlang, 100);
        for (ServerPlayer p : List.of(young, erlang)) {
            BlockPos pos = h.absolutePos(new BlockPos(1, 3, 4));
            level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
            TerrainBreaker.breakBlocks(level, List.of(pos), p);
            TerrainBreaker.work(level, 4096);
            check(h, level.getBlockState(pos).is(Blocks.BEDROCK), "bedrock holds against " + p.getGameProfile().getName());
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "nothingOfTheBuddhaBelowOneHundredOrForOthers")
    public static void nothingOfTheBuddhaBelowOneHundredOrForOthers(GameTestHelper h) {
        ServerPlayer young = wukong(h, 99);
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(erlang, 100);
        ServerPlayer peng = as(h, ReincarnationOutcome.GOLD_WINGED_PENG); // Primordial, as Wukong
        PlayerLevel.setLevel(peng, 100);
        DamageSource fire = h.getLevel().damageSources().inFire();
        for (ServerPlayer p : List.of(young, erlang, peng)) {
            String who = p == young ? "Wukong at 99" : p == erlang ? "Erlang Shen at 100" : "the Peng at 100";
            check(h, !FightingBuddha.is(p), who + " is not the Fighting Buddha");
            check(h, !NeoForge.EVENT_BUS.post(new EntityInvulnerabilityCheckEvent(p, fire, false)).isInvulnerable(), who + " can be harmed");
            BuddhaVanish.toggle(p);
            check(h, !BuddhaVanish.isVanished(p), who + " cannot vanish");
            check(h, !SomersaultTravel.travel(p, SomersaultTravel.COORDS, 0, 100, 0, ""), who + " cannot somersault anywhere");
            tick(p, (PlayerTickEvent.Post e) -> FightingBuddha.onPlayerTick(e), 1);
            check(h, !p.getPersistentData().getBoolean(FightingBuddha.KEY_ANNOUNCED), who + " is not told he ascended");
        }
        PlayableCharacter wukong = WukongCharacter.INSTANCE;
        check(h, wukong.fullBar(young).stream().map(AbilitySlot::id)
                .noneMatch(id -> id.equals(FightingBuddha.VANISH) || id.equals(FightingBuddha.TRAVEL)), "no Buddha slots at 99");
        ServerPlayer old = buddha(h);
        List<AbilitySlot> bar = wukong.fullBar(old);
        eq(h, FightingBuddha.VANISH, bar.get(5).id(), "Shift + B at 100");
        eq(h, FightingBuddha.TRAVEL, bar.get(7).id(), "Shift + M at 100");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "fightingBuddhaAscensionIsAnnouncedOnce")
    public static void fightingBuddhaAscensionIsAnnouncedOnce(GameTestHelper h) {
        ServerPlayer p = wukong(h, 100); // an old save at 100, not yet told
        List<String> heard = new ArrayList<>();
        listen(p, heard);
        tick(p, (PlayerTickEvent.Post e) -> FightingBuddha.onPlayerTick(e), 1);
        check(h, p.getPersistentData().getBoolean(FightingBuddha.KEY_ANNOUNCED), "ascension marked");
        check(h, heard.stream().anyMatch(s -> s.contains("Victorious Fighting Buddha")), "announced: " + heard);
        int n = heard.size();
        tick(p, (PlayerTickEvent.Post e) -> FightingBuddha.onPlayerTick(e), 5);
        eq(h, n, heard.size(), "only once");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "somersaultLandsSafelyAtCoordinatesInsideRock")
    public static void somersaultLandsSafelyAtCoordinatesInsideRock(GameTestHelper h) {
        ServerPlayer p = buddha(h);
        ServerLevel level = h.getLevel();
        // a solid cube of stone, the coordinates asked for at its heart
        BlockPos heart = h.absolutePos(new BlockPos(2, 6, 2));
        for (int dx = -2; dx <= 2; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -2; dz <= 2; dz++) {
            level.setBlock(heart.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 3);
        }
        var t = SomersaultTravel.coords(p, heart.getX(), heart.getY(), heart.getZ(), level.dimension().location().toString());
        check(h, t != null && t.level() == level, "typed coordinates resolve in that realm");
        Vec3 land = SomersaultTravel.landing(level, t.pos());
        BlockPos feet = BlockPos.containing(land);
        check(h, SomersaultTravel.room(level, feet), "never inside a block: " + feet + " for " + heart);
        check(h, SomersaultTravel.stands(level, feet), "on solid ground: " + feet);
        check(h, feet.distManhattan(heart) <= 12, "near where he asked: " + feet);
        // and in the open air above the platform, he comes down onto it
        BlockPos above = h.absolutePos(new BlockPos(0, 5, 0));
        BlockPos onFloor = BlockPos.containing(SomersaultTravel.landing(level, above));
        check(h, SomersaultTravel.stands(level, onFloor), "stands on the platform: " + onFloor);
        check(h, SomersaultTravel.travel(p, SomersaultTravel.COORDS, heart.getX(), heart.getY(), heart.getZ(), ""), "the somersault goes");
        check(h, !SomersaultTravel.travel(p, SomersaultTravel.COORDS, heart.getX(), heart.getY(), heart.getZ(), ""), "a short cooldown");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "somersaultFindsAPlayerInAnotherRealm", timeoutTicks = 400)
    public static void somersaultFindsAPlayerInAnotherRealm(GameTestHelper h) {
        ServerPlayer p = buddha(h);
        ServerPlayer other = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        ServerLevel nether = h.getLevel().getServer().getLevel(Level.NETHER);
        check(h, nether != null, "the Nether is there");
        other.setServerLevel(nether);
        other.moveTo(40.5, 70, 40.5);
        online(h, other);
        check(h, SomersaultTravel.menu(p).stream().anyMatch(d -> d.id().equals("player:" + other.getUUID())), "he is on the menu");
        var t = SomersaultTravel.resolve(p, "player:" + other.getUUID());
        check(h, t != null && t.level() == nether, "found in the Nether");
        Vec3 land = SomersaultTravel.landing(nether, t.pos());
        BlockPos feet = BlockPos.containing(land);
        check(h, SomersaultTravel.room(nether, feet), "never inside a block: " + feet);
        check(h, land.distanceTo(other.position()) <= 40, "near him: " + land);
        // typed coordinates in another realm, likewise
        var c = SomersaultTravel.coords(p, -20, 64, 30, "minecraft:the_nether");
        check(h, c != null && c.level() == nether, "coordinates in the Nether");
        check(h, SomersaultTravel.room(nether, BlockPos.containing(SomersaultTravel.landing(nether, c.pos()))), "lands safely there too");
        h.succeed();
    }
}
