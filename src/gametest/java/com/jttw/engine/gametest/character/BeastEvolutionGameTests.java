package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.divinebeast.BeastEvolution;
import com.jttw.engine.character.divinebeast.Offering;
import com.jttw.engine.item.craft.Crafters;
import com.jttw.engine.item.craft.GatedRecipe;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.BeastFormShown;
import com.jttw.engine.progression.stats.FormStage;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/**
 * The Divine Beasts evolve by ritual: XP stops at each evolution until the beast burns its offering at
 * the Altar of the Four Symbols, which evolves it (form, stats, the leap) and lets XP flow again; each
 * beast's offerings are its own to craft, from materials its stage of the world gives.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class BeastEvolutionGameTests {

    /** A beast at the level just short of an evolution, its XP there. */
    private static ServerPlayer beastAt(GameTestHelper h, ReincarnationOutcome who, int level) {
        ServerPlayer p = CharTest.as(h, who);
        PlayerLevel.setLevel(p, level);
        PlayerLevel.setXp(p, ProgressionClass.DIVINE_BEAST.growthRate.totalXpForLevel(level));
        return p;
    }

    private static long toNext(int level) {
        return ProgressionClass.DIVINE_BEAST.growthRate.totalXpForLevel(level + 1) - ProgressionClass.DIVINE_BEAST.growthRate.totalXpForLevel(level);
    }

    private static BlockPos altar(GameTestHelper h) {
        BlockPos at = new BlockPos(1, 2, 1);
        h.setBlock(at, ModBlocks.FOUR_SYMBOLS_ALTAR.get());
        return h.absolutePos(at);
    }

    @GameTest(template = PLATFORM)
    public static void xpStopsAtEachEvolutionUntilItsRitual(GameTestHelper h) {
        for (int evolution : new int[]{20, 40, 60, 80}) {
            ServerPlayer p = beastAt(h, ReincarnationOutcome.WHITE_TIGER, evolution - 1);
            PlayerLevel.setFormStage(p, FormStage.forLevel(evolution - 1).stageIndex); // as its earlier rituals left it
            LevelManager.grantXp(p, toNext(evolution - 1) + toNext(evolution) * 3, "test"); // enough for several levels
            eq(h, evolution, PlayerLevel.getLevel(p), "levelling stops at the evolution, " + evolution);
            eq(h, evolution, BeastEvolution.owed(p), "the evolution owed");
            check(h, BeastEvolution.isBlocked(p), "XP held");
            long xp = PlayerLevel.getXp(p);
            LevelManager.grantXp(p, 1_000_000_000L, "test");
            eq(h, xp, PlayerLevel.getXp(p), "no XP at all until the ritual, " + evolution);
            // the evolution waits: its power, its stats and its form are the level before's
            near(h, LevelPower.of(ProgressionClass.DIVINE_BEAST, ReincarnationOutcome.WHITE_TIGER, evolution - 1), LevelPower.of(p), "power held at " + (evolution - 1));
            eq(h, FormStage.forLevel(evolution - 1).stageIndex, PlayerLevel.getFormStage(p), "form held");
            eq(h, BeastFormShown.stage(evolution - 1), BeastFormShown.packed(p) % 8, "its look held");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void theRitualConsumesTheOfferingEvolvesAndUnblocks(GameTestHelper h) {
        ServerPlayer p = beastAt(h, ReincarnationOutcome.PHOENIX, 39);
        LevelManager.grantXp(p, toNext(39) * 4, "test");
        eq(h, 40, BeastEvolution.owed(p), "owes its evolution at 40");
        BlockPos altar = altar(h);
        ItemStack wrong = new ItemStack(Offering.KINDLED_PLUME.item(), 2);
        check(h, !BeastEvolution.offer(p, altar, wrong), "the wrong offering (level 20's) is refused");
        eq(h, 2, wrong.getCount(), "and kept");
        ItemStack dragons = new ItemStack(Offering.WATER_CURTAIN_PEARL.item());
        check(h, !BeastEvolution.offer(p, altar, dragons), "another beast's is refused");
        ItemStack offering = new ItemStack(Offering.WISP_FIRE_PLUME.item(), 2);
        check(h, BeastEvolution.offer(p, altar, offering), "its own begins the ritual");
        eq(h, 1, offering.getCount(), "one burned");
        check(h, BeastEvolution.inRitual(p), "under way");
        check(h, !BeastEvolution.offer(p, altar, offering), "one ritual at a time");
        h.runAfterDelay(BeastEvolution.RITUAL_TICKS + 5, () -> {
            check(h, !BeastEvolution.inRitual(p), "done");
            eq(h, 0, BeastEvolution.owed(p), "nothing owed");
            check(h, BeastEvolution.performed(p, 40), "the evolution at 40 performed");
            eq(h, FormStage.forLevel(40).stageIndex, PlayerLevel.getFormStage(p), "its new form");
            near(h, LevelPower.of(ProgressionClass.DIVINE_BEAST, ReincarnationOutcome.PHOENIX, 40), LevelPower.of(p), "the leap, at once");
            check(h, LevelPower.of(p) > 3 * LevelPower.of(ProgressionClass.DIVINE_BEAST, ReincarnationOutcome.PHOENIX, 39), "a leap of over x3");
            long xp = PlayerLevel.getXp(p);
            LevelManager.grantXp(p, 10, "test");
            check(h, PlayerLevel.getXp(p) > xp || PlayerLevel.getLevel(p) > 40, "XP flows again");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void theLastEvolutionAtTheCapWaitsForItsRitualToo(GameTestHelper h) {
        ServerPlayer p = beastAt(h, ReincarnationOutcome.TURTLE, 99);
        LevelManager.grantXp(p, toNext(99) * 2, "test");
        eq(h, 100, PlayerLevel.getLevel(p), "at the cap");
        eq(h, 100, BeastEvolution.owed(p), "its apex owed");
        near(h, LevelPower.of(ProgressionClass.DIVINE_BEAST, ReincarnationOutcome.TURTLE, 99), LevelPower.of(p), "its power still 99's");
        check(h, BeastEvolution.offer(p, altar(h), new ItemStack(Offering.WORLD_TURTLE_SHELL.item())), "the World-Turtle Shell");
        h.runAfterDelay(BeastEvolution.RITUAL_TICKS + 5, () -> {
            near(h, LevelPower.of(ProgressionClass.DIVINE_BEAST, ReincarnationOutcome.TURTLE, 100), LevelPower.of(p), "the last leap");
            eq(h, 5, BeastFormShown.packed(p) % 8, "the apex's look");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void evolutionsPerformedAreKeptAndAReachedLevelOwesNothingTwice(GameTestHelper h) {
        ServerPlayer p = beastAt(h, ReincarnationOutcome.DRAGON, 19);
        LevelManager.grantXp(p, toNext(19), "test");
        eq(h, 20, BeastEvolution.owed(p), "owed");
        BeastEvolution.evolve(p, null);
        LevelManager.grantXp(p, toNext(20), "test");
        eq(h, 21, PlayerLevel.getLevel(p), "on past 20");
        eq(h, 0, BeastEvolution.owed(p), "nothing owed");
        // an operator's level owes nothing; a Wukong owes nothing
        ServerPlayer set = CharTest.as(h, ReincarnationOutcome.DRAGON);
        PlayerLevel.setLevel(set, 45);
        eq(h, 0, BeastEvolution.owed(set), "a level set by hand owes nothing");
        ServerPlayer wukong = CharTest.as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setLevel(wukong, 19);
        PlayerLevel.setXp(wukong, ProgressionClass.PRIMORDIAL.growthRate.totalXpForLevel(19));
        LevelManager.grantXp(wukong, ProgressionClass.PRIMORDIAL.growthRate.totalXpForLevel(22), "test");
        check(h, PlayerLevel.getLevel(wukong) > 20, "only the Divine Beasts evolve by ritual");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theAltarIsCraftedByTheBeastsAlone(GameTestHelper h) {
        var r = h.getLevel().getRecipeManager().byKey(JTTWEngine.id("offering/four_symbols_altar"));
        check(h, r.isPresent() && r.get().value() instanceof GatedRecipe g && g.gate().allows(ReincarnationOutcome.TURTLE.id)
                && !g.gate().allows(ReincarnationOutcome.THIRD_LOTUS_PRINCE.id), "the altar: Divine Beasts only");
        h.succeed();
    }

    /** Where each stage's materials come from: the creatures of that part of the world (their loot tables). */
    private static final Map<Integer, List<String>> STAGE_CREATURES = Map.of(
            20, List.of("goblin", "kobold", "wolfman", "ogre"),
            40, List.of("bone_servant", "wraith", "lady_white_bone", "bear_soldier", "wind_imp", "boar_demon", "black_bear_demon",
                    "spear_ape", "stone_gibbon", "great_baboon", "fake_pilgrim"),
            60, List.of("mouse_spirit", "flower_maid", "lady_earth_flow", "fish_soldier", "shrimp_soldier", "turtle_soldier", "crab_general", "yaksha"),
            80, List.of("ox_head", "horse_face", "judge_clerk", "black_impermanence", "white_impermanence", "wandering_ghost", "yin_soldier",
                    "king_qinguang", "king_chujiang", "ember_imp", "furnace_brute", "strong_general", "red_boy"),
            100, List.of("cloud_crane", "daoist_disciple", "clear_wind", "bright_moon", "zhenyuan", "spider_demon", "spider_spirit",
                    "mantis_son", "stinger_son", "cockroach_son", "hundred_eyed_demon", "lion_demon", "elephant_demon", "peng_demon"));

    /** Mod items every stage's world gives besides its creatures' drops: any captain's sigil, and what the furnace's fuel is crafted from there. */
    private static final Map<Integer, Set<String>> STAGE_ALSO = Map.of(80, Set.of("jttw_engine:true_samadhi_fire"));

    private static Set<String> dropsOf(GameTestHelper h, int stage) {
        Set<String> out = new HashSet<>(Set.of("jttw_engine:captains_sigil"));
        out.addAll(STAGE_ALSO.getOrDefault(stage, Set.of()));
        var rm = h.getLevel().getServer().getResourceManager();
        for (String creature : STAGE_CREATURES.get(stage)) {
            var res = rm.getResource(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "loot_table/entities/" + creature + ".json"));
            check(h, res.isPresent(), creature + "'s loot table");
            try (BufferedReader r = res.get().openAsReader()) {
                var m = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([a-z_:]+)\"").matcher(r.lines().reduce("", String::concat));
                while (m.find()) out.add(m.group(1));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return out;
    }

    @GameTest(template = PLATFORM)
    public static void eachBeastHasItsOwnOfferingForEveryEvolutionFromItsStagesMaterials(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        Map<Integer, Set<String>> drops = new java.util.HashMap<>();
        for (int stage : BeastEvolution.EVOLUTIONS) drops.put(stage, dropsOf(h, stage));
        int found = 0;
        for (Offering o : Offering.values()) {
            var holder = recipes.getAllRecipesFor(RecipeType.CRAFTING).stream()
                    .filter(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(o.item())).toList();
            eq(h, 1, holder.size(), o.id() + ": one recipe");
            var recipe = holder.get(0).value();
            check(h, recipe instanceof GatedRecipe g && g.gate().allows(o.beast().id), o.id() + ": its beast crafts it");
            for (ReincarnationOutcome other : ReincarnationOutcome.values()) {
                if (other != o.beast()) check(h, !((GatedRecipe) recipe).gate().allows(other.id), o.id() + ": not " + other.id);
            }
            // its materials, each from its stage's world (vanilla's own things, from the world at large)
            List<ItemStack> grid = new ArrayList<>();
            for (Ingredient i : recipe.getIngredients()) {
                ItemStack first = i.getItems()[0];
                String id = BuiltInRegistries.ITEM.getKey(first.getItem()).toString();
                check(h, id.startsWith("minecraft:") || drops.get(o.level).contains(id), o.id() + ": " + id + " is not from its stage's world");
                grid.add(first.copyWithCount(1));
            }
            while (grid.size() < 9) grid.add(ItemStack.EMPTY);
            // and the beast at a crafting table makes it, from them
            ServerPlayer beast = CharTest.as(h, o.beast());
            ServerPlayer other = CharTest.as(h, o.beast() == ReincarnationOutcome.PHOENIX ? ReincarnationOutcome.DRAGON : ReincarnationOutcome.PHOENIX);
            CraftingInput in = CraftingInput.of(3, 3, grid);
            List<Boolean> ok = new ArrayList<>();
            Crafters.as(beast, () -> ok.add(recipes.getRecipeFor(RecipeType.CRAFTING, in, h.getLevel()).isPresent()));
            Crafters.as(other, () -> ok.add(recipes.getRecipeFor(RecipeType.CRAFTING, in, h.getLevel()).isPresent()));
            check(h, ok.get(0), o.id() + ": crafted by its beast");
            check(h, !ok.get(1), o.id() + ": not by another");
            found++;
        }
        eq(h, 20, found, "four beasts, five evolutions");
        h.succeed();
    }
}
