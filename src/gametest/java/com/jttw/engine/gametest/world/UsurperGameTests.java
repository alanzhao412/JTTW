package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.fire.RedBoyEntity;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.BlackWindRegistry;
import com.jttw.engine.registry.FlowerFruitRegistry;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Usurpers;
import com.jttw.engine.world.ZonePosts;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/**
 * A fallen boss's seat is taken by a usurper ({@link Usurpers}): after {@value ZonePosts#RESPAWN_MINUTES}
 * minutes, never with someone in the lair, a boss of another kind (never the Six-Eared Macaque) sits
 * there, of the lair's zone, at its band's top and {@value ZoneBoss#USURPER_LEVELS_ABOVE_BAND}, with
 * {@value ZoneBoss#USURPER_HITS} of his kind's hits, his blows scaled to his level, half a boss's XP and
 * a usurper's loot; and his own fall seats another. The seats are the tests' own, on the platform.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class UsurperGameTests {

    private static final double EPS = 1e-3;

    /** A boss as the world puts him in his lair (a structure's: his seat where he stands), still. */
    private static <T extends ZoneBoss> T seated(ServerLevel level, EntityType<T> type, BlockPos at) {
        T b = type.create(level);
        b.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        b.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(b);
        b.setNoAi(true);
        return b;
    }

    /** A fresh one of {@code kind}, its own lair's (not in the world): what his kind is. */
    private static ZoneBoss kindOf(ServerLevel level, EntityType<?> kind) {
        return (ZoneBoss) kind.create(level);
    }

    /** Checks {@code u} is a usurper of {@code zone}'s lair at {@code seat}, as easy as he should be. */
    private static void isUsurper(GameTestHelper h, ZoneBoss u, Zones.Zone zone, BlockPos seat, int reach) {
        ServerLevel level = h.getLevel();
        String n = u.getType().toShortString();
        check(h, u.isUsurper(), n + " a usurper");
        check(h, Usurpers.candidates().contains(u.getType()), n + " one who may usurp");
        check(h, u.getType() != FlowerFruitRegistry.SIX_EARED_MACAQUE.get(), "never the Six-Eared Macaque");
        eq(h, zone.maxLevel() + ZoneBoss.USURPER_LEVELS_ABOVE_BAND, u.mobLevel(), n + "'s level: his lair's band's top and 5");
        eq(h, zone, u.lairZone(), n + " of the lair's zone");
        eq(h, zone.id(), u.getPersistentData().getString(ZoneMonster.KEY_ZONE), n + "'s kill paid by the lair's zone");
        near(h, ZoneBoss.USURPER_XP, u.getPersistentData().getDouble(ZoneMonster.KEY_XP_SHARE), n + " pays half a boss's XP");
        eq(h, ZoneBoss.USURPER_LOOT, u.getLootTable(), n + " leaves a usurper's loot, not his kind's treasures");
        check(h, u.getName().getString().endsWith("· Lv " + u.mobLevel()), n + " named with his new level: " + u.getName().getString());
        eq(h, seat, u.seat(), n + "'s seat, the lair's");
        check(h, u.hasRestriction() && seat.equals(u.getRestrictCenter()) && u.getRestrictRadius() == reach, n + " keeps to the lair");
        eq(h, reach, u.homeReach(), n + " keeps to the fallen master's ground");
        // easier: 60% of his kind's hits (at least 150), of a character of the level he is measured at now
        ZoneBoss kind = kindOf(level, u.getType());
        double kindHits = kind.hitsToKill(), hits = u.hitsToKill();
        double want = Math.max(ZoneBoss.USURPER_HITS * kindHits, ZoneBoss.USURPER_LEAST_HITS);
        check(h, Math.abs(hits - want) < 0.01 * want, n + ": " + hits + " hits, of his kind's " + kindHits);
        check(h, hits >= ZoneBoss.USURPER_LEAST_HITS - EPS && hits < kindHits, n + " easier than his kind: " + hits);
        eq(h, u.getMaxHealth(), u.getHealth(), n + " whole");
        // his blow and every special, scaled to his level: the same share of a character of it as his kind's of theirs
        double th = MobScaling.typicalHealth(u.measuredLevel()), kindTh = MobScaling.typicalHealth(kind.nativeMeasured());
        double blow = u.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        double share = Math.min(blow / kindTh, ZoneBoss.MOST_BLOW);
        check(h, Math.abs(u.strikes((float) blow) / th - share) < EPS, n + "'s blow " + u.strikes((float) blow) / th + " of a character of his level, his kind's " + share);
        for (double special : u.specialShares()) {
            double lands = u.strikes(u.hitOf(special)) / th;
            check(h, Math.abs(lands - Math.min(special, ZoneBoss.MOST_BLOW)) < EPS, n + "'s special " + special + " lands at " + lands);
        }
        // the hook on all he strikes: what he deals is scaled as he lands it
        var golem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        var event = incoming(golem, level.damageSources().mobAttack(u), (float) blow);
        Usurpers.onIncomingDamage(event);
        check(h, Math.abs(event.getAmount() - u.strikes((float) blow)) < 1e-3 * blow, n + "'s blow scaled as it lands: " + event.getAmount());
        var thorns = incoming(golem, level.damageSources().thorns(u), 100f);
        Usurpers.onIncomingDamage(thorns);
        near(h, 100f, thorns.getAmount(), "a blow turned back on its striker is his own, not scaled");
        golem.discard();
    }

    /** Clears the tests' seat (and anything it left) at the end. */
    private static void clearing(GameTestHelper h, BlockPos seat, List<Entity> made) {
        ServerLevel level = h.getLevel();
        onEnd(h, () -> {
            made.forEach(Entity::discard);
            Usurpers.State.of(level).forget(level.dimension(), seat);
        });
    }

    @GameTest(template = PLATFORM, batch = "usurpers_seat", timeoutTicks = 200)
    public static void aFallenBossIsSucceededByAnEasierUsurper(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = player(h);
        BlockPos seat = h.absolutePos(new BlockPos(2, 2, 2));
        List<Entity> made = new ArrayList<>();
        clearing(h, seat, made);
        var state = Usurpers.State.of(level);
        state.forget(level.dimension(), seat);
        RedBoyEntity boy = seated(level, ModEntities.RED_BOY.get(), seat);
        made.add(boy);
        eq(h, seat, boy.seat(), "the world's boss takes his lair for his seat");
        check(h, !boy.isUsurper() && boy.mobLevel() == RedBoyEntity.LEVEL, "Red Boy, his own lair's, at his own level");
        boy.kill();
        CompoundTag t = state.get(level.dimension(), seat);
        check(h, t != null, "his seat written down as he fell");
        eq(h, Zones.FIRE_MOUNTAIN.id(), t.getString("Zone"), "his lair's zone");
        eq(h, "jttw_engine:red_boy", t.getString("Fallen"), "who fell there");
        eq(h, boy.homeReach(), t.getInt("Reach"), "his lair's ground");
        // not before its time
        p.moveTo(seat.getX() + 200, seat.getY(), seat.getZ());
        state.fellAt(level.dimension(), seat, level.getGameTime() - ZonePosts.RESPAWN + 200);
        check(h, Usurpers.look(level, seat, List.of(p)) == null, "not taken before its time");
        // its time come round, but someone in the lair
        state.fellAt(level.dimension(), seat, level.getGameTime() - ZonePosts.RESPAWN - 1);
        p.moveTo(seat.getX() + RedBoyEntity.LAIR, seat.getY(), seat.getZ());
        check(h, Usurpers.look(level, seat, List.of(p)) == null, "not taken under someone's nose");
        // no one in it: taken
        p.moveTo(seat.getX() + 200, seat.getY(), seat.getZ());
        ZoneBoss u = Usurpers.look(level, seat, List.of(p));
        check(h, u != null, "the empty seat taken");
        made.add(u);
        u.setNoAi(true);
        check(h, u.getType() != ModEntities.RED_BOY.get(), "by another kind: " + u.getType().toShortString());
        check(h, state.get(level.dimension(), seat) == null, "the seat no longer empty");
        isUsurper(h, u, Zones.FIRE_MOUNTAIN, seat, RedBoyEntity.LAIR);
        // saved and loaded, a usurper still
        CompoundTag saved = u.saveWithoutId(new CompoundTag());
        ZoneBoss again = (ZoneBoss) u.getType().create(level);
        again.load(saved);
        check(h, again.isUsurper() && again.mobLevel() == u.mobLevel() && again.lairZone() == Zones.FIRE_MOUNTAIN
                && Math.abs(again.strikeScale() - u.strikeScale()) < 1e-9 && again.getMaxHealth() == u.getMaxHealth()
                && seat.equals(again.seat()) && again.homeReach() == RedBoyEntity.LAIR, "kept as he was, saved and loaded");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "usurpers_follow", timeoutTicks = 200)
    public static void aUsurpersFallSeatsAnother(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = player(h);
        BlockPos seat = h.absolutePos(new BlockPos(2, 2, 2));
        List<Entity> made = new ArrayList<>();
        clearing(h, seat, made);
        var state = Usurpers.State.of(level);
        state.forget(level.dimension(), seat);
        ZoneBoss bear = Usurpers.seatAs(level, BlackWindRegistry.BLACK_BEAR_DEMON.get(), seat, Zones.WHITE_BONE, 20);
        made.add(bear);
        bear.setNoAi(true);
        isUsurper(h, bear, Zones.WHITE_BONE, seat, 20);
        eq(h, 41, bear.mobLevel(), "the Black Bear, usurping White Bone Ridge, at 41");
        bear.kill();
        CompoundTag t = state.get(level.dimension(), seat);
        check(h, t != null, "a usurper's fall leaves the seat empty again");
        eq(h, Zones.WHITE_BONE.id(), t.getString("Zone"), "of the same lair's zone");
        eq(h, "jttw_engine:black_bear_demon", t.getString("Fallen"), "the usurper who fell");
        eq(h, 20, t.getInt("Reach"), "the same ground");
        state.fellAt(level.dimension(), seat, level.getGameTime() - ZonePosts.RESPAWN - 1);
        p.moveTo(seat.getX() + 200, seat.getY(), seat.getZ());
        ZoneBoss next = Usurpers.look(level, seat, List.of(p));
        check(h, next != null, "another follows him");
        made.add(next);
        next.setNoAi(true);
        check(h, next.getType() != BlackWindRegistry.BLACK_BEAR_DEMON.get(), "of another kind: " + next.getType().toShortString());
        isUsurper(h, next, Zones.WHITE_BONE, seat, 20);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "usurpers_who", timeoutTicks = 200)
    public static void theMacaqueNeverUsurpsAndHisSanctumIsTaken(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        RandomSource r = RandomSource.create(7);
        Set<EntityType<?>> barredKinds = Set.of(FlowerFruitRegistry.SIX_EARED_MACAQUE.get(), ModEntities.ZHENYUAN.get(),
                ModEntities.CLEAR_WIND.get(), ModEntities.BRIGHT_MOON.get(), ModEntities.LADY_WHITE_BONE.get());
        Set<EntityType<?>> seaKings = new HashSet<>(Usurpers.seaKings());
        Set<EntityType<?>> seen = new HashSet<>();
        for (int i = 0; i < 600; i++) {
            EntityType<?> k = Usurpers.choose(false, ModEntities.RED_BOY.get(), Set.of(), r);
            check(h, k != null && !barredKinds.contains(k) && !seaKings.contains(k) && k != ModEntities.RED_BOY.get(), "a land seat's usurper: " + k);
            seen.add(k);
        }
        check(h, seen.contains(ModEntities.LADY_EARTH_FLOW.get()) && seen.contains(BlackWindRegistry.BLACK_BEAR_DEMON.get())
                && seen.contains(ModEntities.KING_YAMA.get()) && seen.contains(ModEntities.AZURE_LION.get())
                && seen.contains(ModEntities.WHITE_ELEPHANT.get()), "every house comes in its turn: " + seen.size() + " kinds");
        for (int i = 0; i < 100; i++) {
            EntityType<?> k = Usurpers.choose(true, ModEntities.AO_GUANG.get(), Set.of(), r);
            check(h, seaKings.contains(k) && k != ModEntities.AO_GUANG.get(), "a drowned palace's usurper, a king of another sea: " + k);
        }
        // the pair's throne room: never the king still sat there, nor the fallen one
        for (int i = 0; i < 200; i++) {
            EntityType<?> k = Usurpers.choose(false, ModEntities.WHITE_ELEPHANT.get(), Set.of(ModEntities.AZURE_LION.get()), r);
            check(h, k != ModEntities.AZURE_LION.get() && k != ModEntities.WHITE_ELEPHANT.get(), "not of the pair: " + k);
        }
        // the Macaque's sanctum: his fall leaves a seat of Flower-Fruit's, for a usurper at its band's top and 5
        ServerPlayer p = player(h);
        BlockPos seat = h.absolutePos(new BlockPos(2, 2, 2));
        List<Entity> made = new ArrayList<>();
        clearing(h, seat, made);
        var state = Usurpers.State.of(level);
        state.forget(level.dimension(), seat);
        ZoneBoss macaque = seated(level, FlowerFruitRegistry.SIX_EARED_MACAQUE.get(), seat);
        made.add(macaque);
        macaque.kill();
        check(h, state.get(level.dimension(), seat) != null, "his sanctum's seat empty");
        state.fellAt(level.dimension(), seat, level.getGameTime() - ZonePosts.RESPAWN - 1);
        p.moveTo(seat.getX() + 200, seat.getY(), seat.getZ());
        ZoneBoss u = Usurpers.look(level, seat, List.of(p));
        check(h, u != null, "his sanctum taken");
        made.add(u);
        u.setNoAi(true);
        isUsurper(h, u, Zones.FLOWER_FRUIT, seat, macaque.homeReach());
        eq(h, 35, u.mobLevel(), "a usurper of Flower-Fruit's band, at 35");
        // the temple's youths are no lair's masters: their fall leaves no seat
        ZoneBoss youth = seated(level, ModEntities.CLEAR_WIND.get(), seat);
        made.add(youth);
        check(h, youth.seat() == null && !youth.headsLair(), "a youth holds no seat");
        h.succeed();
    }
}
