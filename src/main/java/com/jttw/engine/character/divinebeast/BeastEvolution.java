package com.jttw.engine.character.divinebeast;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.craft.GatedRecipe;
import com.jttw.engine.progression.level.LevelConstants;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.FormStage;
import com.jttw.engine.progression.stats.ProgressionClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Divine Beast evolves by ritual, not by level alone.
 *
 * <p>At each evolution (levels 20, 40, 60, 80 and 100) a beast that has levelled up to it stops: it
 * holds the level, but the evolution is <b>owed</b>. Until it is performed it gains no more XP (as
 * Wukong at his checkpoints), its power stays the level before's (the form's leap in
 * {@link com.jttw.engine.progression.stats.LevelPower} waits: {@link #poweredLevel}), and its form
 * stays the one before. The ritual: craft the beast's {@link Offering} for that evolution from the
 * materials of the part of the world that leads to it, and burn it at an Altar of the Four Symbols
 * ({@link FourSymbolsAltarBlock}). Three seconds of the beast's element gathering round the altar,
 * and the beast evolves: its form, its stats and its leap all at once, and levelling resumes.
 *
 * <p>A level reached by other means than levelling (an operator setting it) owes nothing.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class BeastEvolution {

    private BeastEvolution() {}

    /** The evolutions, by level. */
    public static final int[] EVOLUTIONS = {20, 40, 60, 80, 100};
    /** The evolution owed, by its level (0: none). */
    public static final String KEY_OWED = "jttw_evolution_owed";
    private static final String KEY_DONE = "jttw_evolution_done_";
    /** How long the ritual takes, in ticks. */
    public static final int RITUAL_TICKS = 60;

    public static boolean isEvolution(int level) {
        for (int e : EVOLUTIONS) if (e == level) return true;
        return false;
    }

    public static boolean isBeast(Player p) {
        return PlayerLevel.getProgressionClass(p) == ProgressionClass.DIVINE_BEAST;
    }

    /** The evolution this beast owes (its level), or 0. */
    public static int owed(Player p) {
        return isBeast(p) ? p.getPersistentData().getInt(KEY_OWED) : 0;
    }

    public static boolean performed(Player p, int evolution) {
        return p.getPersistentData().getBoolean(KEY_DONE + evolution);
    }

    /** Whether its XP is held: it owes an evolution. */
    public static boolean isBlocked(Player p) {
        return owed(p) > 0;
    }

    /** The level its power and stats are read at: the level before an owed evolution, else its own. */
    public static int poweredLevel(Player p, int level) {
        int owed = owed(p);
        return owed > 0 ? Math.min(level, owed - 1) : level;
    }

    /**
     * Called as a beast levels up to {@code level}: at an evolution not yet performed, it is owed, the
     * beast is told what the ritual takes, and levelling stops there. Returns whether it stopped.
     */
    public static boolean reach(ServerPlayer p, int level) {
        if (!isBeast(p) || !isEvolution(level) || performed(p, level)) return false;
        p.getPersistentData().putInt(KEY_OWED, level);
        tellOwed(p);
        return true;
    }

    /** Tells the beast what its owed evolution's ritual takes. */
    public static void tellOwed(ServerPlayer p) {
        int level = owed(p);
        if (level == 0) return;
        Offering o = Offering.of(PlayerLevel.getBeastLine(p), level);
        p.sendSystemMessage(Component.literal("§d[Evolution] §rYour body strains at its shell: §eno more XP§r until you evolve."));
        if (o == null) return;
        p.sendSystemMessage(Component.literal("§7Craft the §6" + o.displayName + "§7 ("
                + describeRecipe(p.serverLevel(), o) + ") and burn it at an §6Altar of the Four Symbols§7."));
        p.sendSystemMessage(Component.literal("§8Its materials come from " + o.regions() + "."));
    }

    /** "4 Feather, 2 Gold Ingot": what crafting this offering takes, read from its recipe. */
    public static String describeRecipe(ServerLevel level, Offering o) {
        for (var r : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(r.value() instanceof GatedRecipe) || !r.value().getResultItem(level.registryAccess()).is(o.item())) continue;
            Map<String, Integer> counted = new LinkedHashMap<>();
            for (Ingredient i : r.value().getIngredients()) {
                if (i.isEmpty()) continue;
                ItemStack[] items = i.getItems();
                counted.merge(items.length == 0 ? "?" : items[0].getHoverName().getString(), 1, Integer::sum);
            }
            StringBuilder sb = new StringBuilder();
            counted.forEach((n, c) -> sb.append(sb.isEmpty() ? "" : ", ").append(c).append(' ').append(n));
            return sb.toString();
        }
        return "no recipe";
    }

    // ── The ritual ───────────────────────────────────────────────────────

    /** A ritual under way at an altar. */
    public static final class Ritual {
        final ServerPlayer player;
        final ServerLevel level;
        final BlockPos altar;
        final Offering offering;
        int ticks;

        Ritual(ServerPlayer player, BlockPos altar, Offering offering) {
            this.player = player;
            this.level = player.serverLevel();
            this.altar = altar.immutable();
            this.offering = offering;
        }
    }

    private static final List<Ritual> RITUALS = new ArrayList<>();

    public static boolean inRitual(Player p) {
        for (Ritual r : RITUALS) if (r.player == p) return true;
        return false;
    }

    /**
     * A beast burns {@code stack} at the altar at {@code altar}. If it is the offering its owed evolution
     * takes, one is consumed and the ritual begins; otherwise it is told what is wanted. Returns whether
     * the ritual began.
     */
    public static boolean offer(ServerPlayer p, BlockPos altar, ItemStack stack) {
        if (!isBeast(p)) {
            p.displayClientMessage(Component.literal("§7The altar answers only to the Four Symbols' kin."), true);
            return false;
        }
        int owed = owed(p);
        if (owed == 0) {
            p.displayClientMessage(Component.literal("§7You owe no evolution now."), true);
            return false;
        }
        if (inRitual(p)) return false;
        Offering wanted = Offering.of(PlayerLevel.getBeastLine(p), owed);
        Offering given = Offering.of(stack.getItem());
        if (wanted == null || given != wanted) {
            p.displayClientMessage(Component.literal("§cThe altar wants the " + (wanted == null ? "?" : wanted.displayName) + "."), true);
            return false;
        }
        if (!p.getAbilities().instabuild) stack.shrink(1);
        RITUALS.add(new Ritual(p, altar, wanted));
        p.level().playSound(null, altar, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.7f);
        p.displayClientMessage(Component.literal("§dThe " + wanted.displayName + " catches fire on the altar..."), true);
        return true;
    }

    /** Tells one at the altar empty-handed what their next ritual takes. */
    public static void explain(ServerPlayer p) {
        if (!isBeast(p)) {
            p.displayClientMessage(Component.literal("§7An altar of the Four Symbols: the Divine Beasts evolve here."), true);
            return;
        }
        if (owed(p) > 0) {
            tellOwed(p);
            return;
        }
        int next = 0;
        for (int e : EVOLUTIONS) if (!performed(p, e) && e > PlayerLevel.getLevel(p)) { next = e; break; }
        Offering o = next == 0 ? null : Offering.of(PlayerLevel.getBeastLine(p), next);
        p.sendSystemMessage(Component.literal(o == null ? "§7Every evolution is behind you."
                : "§7Your next evolution, at level " + next + ", will want the §6" + o.displayName + "§7 ("
                + describeRecipe(p.serverLevel(), o) + ")."));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<Ritual> it = RITUALS.iterator(); it.hasNext(); ) {
            Ritual r = it.next();
            if (r.player.isRemoved() || r.player.level() != r.level) {
                // gone mid-ritual: the offering is given back at the altar, the evolution still owed
                net.minecraft.world.Containers.dropItemStack(r.level, r.altar.getX() + 0.5, r.altar.getY() + 1.2, r.altar.getZ() + 0.5, new ItemStack(r.offering.item()));
                it.remove();
                continue;
            }
            gather(r);
            if (++r.ticks >= RITUAL_TICKS) {
                it.remove();
                evolve(r.player, r.altar);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RITUALS.clear();
    }

    /** The beast's element: what gathers round the altar while the ritual burns. */
    private static ParticleOptions element(DivineBeastLine line) {
        return switch (line) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.DRIPPING_WATER;
            case BEAST -> new DustParticleOptions(new Vector3f(0.95f, 0.95f, 1.0f), 1.4f);
            case EARTH -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState());
        };
    }

    private static ParticleOptions accent(DivineBeastLine line) {
        return switch (line) {
            case FIRE -> ParticleTypes.LAVA;
            case WATER -> ParticleTypes.GLOW;
            case BEAST -> ParticleTypes.CRIT;
            case EARTH -> ParticleTypes.COMPOSTER;
        };
    }

    private static SoundEvent roar(DivineBeastLine line) {
        return switch (line) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case WATER -> SoundEvents.TRIDENT_THUNDER.value();
            case BEAST -> SoundEvents.RAVAGER_ROAR;
            case EARTH -> SoundEvents.ANVIL_LAND;
        };
    }

    /** One tick of the ritual: the element spirals up off the altar and gathers round the beast. */
    private static void gather(Ritual r) {
        Vec3 top = Vec3.atBottomCenterOf(r.altar).add(0, 1.1, 0);
        Vec3 at = r.player.position().add(0, 1.0, 0);
        double t = r.ticks / (double) RITUAL_TICKS;
        ParticleOptions element = element(r.offering.line);
        for (int arm = 0; arm < 4; arm++) { // four arms, the Four Symbols
            double a = r.ticks * 0.35 + arm * Math.PI / 2;
            double radius = 1.2 * (1 - t) + 0.3;
            Vec3 p = top.lerp(at, t).add(Math.cos(a) * radius, t * 1.5 + Math.sin(r.ticks * 0.2) * 0.2, Math.sin(a) * radius);
            r.level.sendParticles(element, p.x, p.y, p.z, 2, 0.04, 0.04, 0.04, 0.01);
        }
        if (r.ticks % 4 == 0) r.level.sendParticles(accent(r.offering.line), top.x, top.y + 0.2, top.z, 3, 0.25, 0.1, 0.25, 0.02);
        if (r.ticks % 20 == 0) {
            r.level.playSound(null, r.altar, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.6f + (float) t);
            r.level.playSound(null, r.altar, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.5f, 1.0f + (float) t * 0.5f);
        }
    }

    /** The form each beast takes at each evolution. */
    public static String formName(DivineBeastLine line, int evolution) {
        String[] names = switch (line) {
            case FIRE -> new String[]{"Flame Guardian", "Phoenix", "Celestial Phoenix", "Vermilion Bird", "Solar Sovereign"};
            case WATER -> new String[]{"River Guardian", "Azure Dragon", "Dragon Prince", "Dragon King", "Azure Dragon of the East"};
            case BEAST -> new String[]{"Western Guardian", "White Tiger", "Celestial White Tiger", "Lord of the West", "Byakko"};
            case EARTH -> new String[]{"Stone Sentinel", "Mountain Guardian", "Earth Titan", "Black Tortoise", "World Turtle"};
        };
        for (int i = 0; i < EVOLUTIONS.length; i++) if (EVOLUTIONS[i] == evolution) return names[i];
        return names[0];
    }

    /**
     * The owed evolution, performed: its form, its stats and its leap at once, XP flowing again, and its
     * arrival seen and heard. (The ritual calls it at its end; tests may call it.)
     */
    public static void evolve(ServerPlayer p, @Nullable BlockPos altar) {
        int owed = owed(p);
        if (owed == 0) return;
        CompoundTag pd = p.getPersistentData();
        pd.putBoolean(KEY_DONE + owed, true);
        pd.putInt(KEY_OWED, 0);
        FormStage stage = FormStage.forLevel(owed);
        PlayerLevel.setFormStage(p, stage.stageIndex);
        LevelManager.reapplyStats(p);
        p.setHealth(p.getMaxHealth());
        LevelManager.syncToClient(p);

        DivineBeastLine line = PlayerLevel.getBeastLine(p);
        String form = line == null ? stage.displayName : formName(line, owed);
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.x, at.y + 1, at.z, 80, 0.6, 1.0, 0.6, 0.5);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        if (line != null) level.sendParticles(accent(line), at.x, at.y + 1, at.z, 40, 1.0, 1.0, 1.0, 0.1);
        if (line != null) level.playSound(null, p.blockPosition(), roar(line), SoundSource.PLAYERS, 1.6f, 0.8f);
        level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6" + form)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§dEvolution at level " + owed)));
        p.sendSystemMessage(Component.literal("§d[Evolution] §rYou are the §6" + form + "§r. " + stage.transitionMessage
                + (owed < LevelConstants.MAX_LEVEL ? " §7XP flows again." : "")));
    }

    // ── Kept through death ───────────────────────────────────────────────

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag from = event.getOriginal().getPersistentData(), to = event.getEntity().getPersistentData();
        if (from.contains(KEY_OWED)) to.putInt(KEY_OWED, from.getInt(KEY_OWED));
        for (int e : EVOLUTIONS) if (from.getBoolean(KEY_DONE + e)) to.putBoolean(KEY_DONE + e, true);
    }

    /** Clears a character's evolutions (a new reincarnation starts over). */
    public static void reset(Player p) {
        CompoundTag pd = p.getPersistentData();
        pd.remove(KEY_OWED);
        for (int e : EVOLUTIONS) pd.remove(KEY_DONE + e);
    }
}
