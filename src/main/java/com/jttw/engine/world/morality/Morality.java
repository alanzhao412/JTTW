package com.jttw.engine.world.morality;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.heaven.HeavenlySoldierEntity;
import com.jttw.engine.entity.ridge.LadyWhiteBoneEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.entity.cave.LadyEarthFlowEntity;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncKarmaPacket;
import com.jttw.engine.registry.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * Karma: what Heaven makes of a player's deeds, from {@value #MIN} to {@value #MAX}, starting at
 * 0. Slaying monsters and dealing fairly raise it; killing the innocent, and above all robbing
 * Heaven (its peaches, its wine, its treasuries, the Book of Life and Death), lower it.
 *
 * <p>Sink low enough and Heaven takes notice: an outlaw at {@value #OUTLAW}, its enemy at
 * {@value #ENEMY}, and at {@value #GREAT_REBEL} a rebel the Jade Emperor sends his whole host
 * against, as he did against the Monkey King. While a wanted player is in Heaven, its soldiers
 * come for them in waves ({@link HeavenlyHost}). Karma is the soul's, so it carries through death
 * and into each new life.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Morality {

    private Morality() {}

    public static final String KEY = "jttw_karma";
    public static final int MIN = -10_000, MAX = 10_000;

    /** Where Heaven's wrath begins, and where it deepens. */
    public static final int OUTLAW = -250, ENEMY = -1_000, GREAT_REBEL = -3_000;

    // ── deeds ──────────────────────────────────────────────────────────
    public static final int STEAL_PEACH = -40, STEAL_WINE = -40, LOOT_HEAVEN = -30, STRIKE_THE_BOOK = -200;
    public static final int KILL_VILLAGER = -50, KILL_PET = -30, KILL_GOLEM = -10, KILL_PLAYER = -60, KILL_SOLDIER = -15;
    public static final int SLAY_MONSTER = 1, SLAY_CAPTAIN = 5, SLAY_LADY = 150, TRADE = 1;
    /** Lady Earth Flow, who ate the Buddha's candles and preyed on monks: as much as Lady White Bone. */
    public static final int SLAY_EARTH_FLOW = SLAY_LADY;
    /**
     * The Dragon Kings are Heaven's officials over the Four Seas: killing one is a crime against
     * Heaven (Ao Guang's memorial against the Monkey King is what first brought Heaven down on
     * him). All four put a player past {@link #OUTLAW}. Plundering their treasuries is a lesser one.
     */
    public static final int SLAY_DRAGON_KING = -80, LOOT_DRAGON_PALACE = -10;
    /**
     * The Ten Kings of Hell are Heaven's judges of the dead: striking one down defies Heaven's
     * order (as Sun Wukong's rampage through the underworld did), King Yama most of all. All ten put
     * a player past {@link #OUTLAW}, as the four Dragon Kings do. Hell's lesser officials (Ox-Head,
     * Horse-Face, the clerks, the Impermanence) are doing their work, and a fight with them is no
     * deed either way (a captain still counts as any captain does); laying a wandering ghost to rest
     * is a small good, as slaying any monster is.
     */
    public static final int SLAY_KING_OF_HELL = -35, SLAY_KING_YAMA = -70;
    /**
     * Red Boy, the Holy Child King, who wanted to eat the Tang Monk (chapters 40–42): subduing him is a
     * good deed, as much as Lady White Bone. His Six Strong Generals are captains (+5 each).
     */
    public static final int SLAY_RED_BOY = SLAY_LADY;
    /**
     * Mount Wuzhuang's household is righteous: the Great Immortal Zhenyuan and his disciples.
     * Killing a disciple is a bad deed, captain or not; his trial is no killing (he yields, and
     * acknowledging whoever passed it is a good deed); only what kills outright could kill him or
     * his youths, and that is a grave wrong. Bringing a ripe ginseng fruit down without his leave is
     * theft, as picking Heaven's peaches is. The mountain's cloud cranes are no one's: a fight with
     * one is no deed either way.
     */
    public static final int KILL_DISCIPLE = -12, STEAL_GINSENG = -40, PASS_THE_TRIAL = 10, SLAY_TEMPLE_YOUTH = -25, SLAY_ZHENYUAN = -100;
    /** How recently a victim must have struck their killer for the kill to be a fight, not a murder: 10 s. */
    public static final int PROVOKED_TICKS = 200;

    public static int karma(Player p) {
        return p.getPersistentData().getInt(KEY);
    }

    /** Sets karma outright (tests, commands, the client's mirror of it). */
    public static void set(Player p, int karma) {
        p.getPersistentData().putInt(KEY, Mth.clamp(karma, MIN, MAX));
    }

    /** How hard Heaven hunts someone with this karma: 0 not at all, then 1, 2, 3. */
    public static int wanted(int karma) {
        return karma <= GREAT_REBEL ? 3 : karma <= ENEMY ? 2 : karma <= OUTLAW ? 1 : 0;
    }

    public static int wanted(Player p) {
        return wanted(karma(p));
    }

    /** What Heaven calls someone with this karma. */
    public static String standing(int karma) {
        return switch (wanted(karma)) {
            case 3 -> "Great Rebel";
            case 2 -> "Enemy of Heaven";
            case 1 -> "Outlaw of Heaven";
            default -> karma >= 500 ? "Virtuous" : karma >= 100 ? "Good" : karma > -100 ? "Neutral" : "Suspect";
        };
    }

    /**
     * A deed: moves their karma by {@code delta}, tells them why, and if it moves them into or out
     * of Heaven's wrath, tells them that too.
     */
    public static void deed(ServerPlayer p, int delta, String what) {
        if (delta == 0) return;
        int before = karma(p);
        set(p, before + delta);
        int after = karma(p);
        if (after == before) return;
        p.displayClientMessage(Component.literal((delta > 0 ? "§aKarma +" : "§cKarma ") + delta + " §7· " + what), true);
        int was = wanted(before), now = wanted(after);
        if (now > was) {
            p.sendSystemMessage(Component.literal(switch (now) {
                case 1 -> "§cHeaven has marked you an outlaw. Its soldiers will be waiting for you there.";
                case 2 -> "§4Heaven names you its enemy. Its generals ride against you now.";
                default -> "§4§lThe Jade Emperor declares war on you. All the host of Heaven is roused.";
            }));
        } else if (now < was) {
            p.sendSystemMessage(Component.literal(now == 0
                    ? "§aHeaven no longer hunts you."
                    : "§eHeaven's anger against you cools, a little."));
        }
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        NetworkHandler.sendToPlayer(p, new SyncKarmaPacket(karma(p)));
    }

    // ── what counts ────────────────────────────────────────────────────

    /** What a kill says of the killer, or 0 for a kill that says nothing (livestock, game). */
    public static int killDeed(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof HeavenlySoldierEntity) return KILL_SOLDIER;
        if (victim instanceof LadyWhiteBoneEntity) return SLAY_LADY;
        if (victim instanceof LadyEarthFlowEntity) return SLAY_EARTH_FLOW;
        if (victim instanceof com.jttw.engine.entity.fire.RedBoyEntity) return SLAY_RED_BOY;
        if (victim instanceof com.jttw.engine.entity.waters.DragonKingEntity) return SLAY_DRAGON_KING;
        if (victim instanceof com.jttw.engine.entity.hell.KingYamaEntity) return SLAY_KING_YAMA;
        if (victim instanceof com.jttw.engine.entity.hell.KingOfHellEntity) return SLAY_KING_OF_HELL;
        if (victim instanceof com.jttw.engine.entity.wuzhuang.ZhenyuanEntity) return SLAY_ZHENYUAN;
        if (victim instanceof com.jttw.engine.entity.wuzhuang.TempleYouthEntity) return SLAY_TEMPLE_YOUTH;
        if (victim instanceof com.jttw.engine.entity.wuzhuang.DaoistDiscipleEntity) return KILL_DISCIPLE;
        if (victim instanceof com.jttw.engine.entity.wuzhuang.CloudCraneEntity) return 0;
        if (victim instanceof ZoneMonster m && m.isCaptain()) return SLAY_CAPTAIN;
        if (victim instanceof com.jttw.engine.entity.hell.HellMonster m && m.isOfficial()) return 0;
        if (victim instanceof AbstractVillager) return KILL_VILLAGER;
        if (victim instanceof IronGolem) return KILL_GOLEM;
        if (victim instanceof TamableAnimal pet && pet.isTame() && !killer.getUUID().equals(pet.getOwnerUUID())) return KILL_PET;
        if (victim instanceof Player other) {
            boolean provoked = killer.getLastHurtByMob() == other
                    && killer.tickCount - killer.getLastHurtByMobTimestamp() <= PROVOKED_TICKS;
            return provoked ? 0 : KILL_PLAYER;
        }
        if (victim instanceof Enemy) return SLAY_MONSTER;
        return 0;
    }

    private static String killWhat(int deed, LivingEntity victim) {
        if (deed == SLAY_CAPTAIN && victim instanceof com.jttw.engine.entity.waters.SeaMonster) return "you slew a captain of the sea";
        if (deed == SLAY_CAPTAIN && victim instanceof com.jttw.engine.entity.hell.HellMonster) return "you slew a captain of the underworld";
        if (deed == SLAY_MONSTER && victim instanceof com.jttw.engine.entity.hell.WanderingGhostEntity) return "you laid a wandering ghost to rest";
        if (deed == SLAY_CAPTAIN && victim instanceof com.jttw.engine.entity.fire.StrongGeneralEntity) return "you struck down one of Red Boy's Six Strong Generals";
        if (victim instanceof com.jttw.engine.entity.fire.RedBoyEntity) return "you subdued Red Boy, the Holy Child King";
        return switch (deed) {
            case SLAY_DRAGON_KING -> "you slew a Dragon King, an official of Heaven";
            case SLAY_KING_OF_HELL -> "you struck down a King of Hell, a judge of the dead";
            case SLAY_KING_YAMA -> "you struck down King Yama, Lord of Death";
            case KILL_DISCIPLE -> "you killed a disciple of the Temple of the Five Villages";
            case SLAY_TEMPLE_YOUTH -> "you killed one of Zhenyuan's youths";
            case SLAY_ZHENYUAN -> "you killed the Great Immortal Zhenyuan, Master of the Earth Immortals";
            case KILL_SOLDIER -> "you struck down a soldier of Heaven";
            case KILL_VILLAGER -> "you killed a villager";
            case KILL_GOLEM -> "you killed a village's guardian";
            case KILL_PET -> "you killed someone's companion";
            case KILL_PLAYER -> "you murdered a player";
            case SLAY_LADY -> "you destroyed Lady White Bone";
            case SLAY_CAPTAIN -> "you slew a demon captain";
            default -> "you slew a monster";
        };
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || event.getEntity() == killer) return;
        int deed = killDeed(killer, event.getEntity());
        if (deed != 0) deed(killer, deed, event.getEntity() instanceof LadyEarthFlowEntity ? "you subdued Lady Earth Flow"
                : killWhat(deed, event.getEntity()));
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) deed(p, TRADE, "you dealt fairly");
    }

    /** Picking Heaven's peaches is theft; the peaches are the Queen Mother's. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p && !p.isCreative()
                && event.getState().is(ModBlocks.PEACH_FRUIT.get())) {
            deed(p, STEAL_PEACH, "you stole the Queen Mother's peaches");
        }
    }

    /** Karma is the soul's: it carries through death and into a new life. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().putInt(KEY, event.getOriginal().getPersistentData().getInt(KEY));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
    }
}
