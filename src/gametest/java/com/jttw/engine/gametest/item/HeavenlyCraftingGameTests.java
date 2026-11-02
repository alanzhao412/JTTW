package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.gametest.character.CharTest;
import com.jttw.engine.item.Armaments;
import com.jttw.engine.item.Armory;
import com.jttw.engine.item.FireTipSpearItem;
import com.jttw.engine.item.HeavenlyCraftItem;
import com.jttw.engine.item.craft.ArmamentGuide;
import com.jttw.engine.item.craft.Crafters;
import com.jttw.engine.item.craft.GatedRecipe;
import com.jttw.engine.item.craft.RefineRecipe;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.jttw.engine.gametest.GT.*;

/**
 * The Heavenly craft: every armament is crafted at tier I by its owner alone, refined tier by tier
 * to VIII (a crafting table to IV, the furnace from V), none of it found in the world; their
 * strength leans on those tiers (the armory's attunement); and their talismans are theirs alone.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class HeavenlyCraftingGameTests {

    private static final ReincarnationOutcome NEZHA = ReincarnationOutcome.THIRD_LOTUS_PRINCE, ERLANG = ReincarnationOutcome.EYE_OF_HEAVEN,
            MARSHAL = ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY;

    /** A shaped recipe's grid, filled with the first item of each ingredient. */
    private static CraftingInput grid(ShapedRecipe r) {
        List<ItemStack> items = new ArrayList<>();
        for (Ingredient i : r.getIngredients()) items.add(i.isEmpty() ? ItemStack.EMPTY : i.getItems()[0].copyWithCount(1));
        return CraftingInput.of(r.getWidth(), r.getHeight(), items);
    }

    /** What the crafting grid makes for {@code who} crafting (null: no one, as a Crafter block). */
    private static Optional<RecipeHolder<CraftingRecipe>> craft(GameTestHelper h, @Nullable Player who, CraftingInput in) {
        List<Optional<RecipeHolder<CraftingRecipe>>> out = new ArrayList<>();
        Crafters.as(who, () -> out.add(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, h.getLevel())));
        return out.get(0);
    }

    private static List<RecipeHolder<CraftingRecipe>> tierOnes(GameTestHelper h) {
        List<RecipeHolder<CraftingRecipe>> out = new ArrayList<>();
        for (var r : h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (r.value() instanceof GatedRecipe.Shaped && Armaments.of(r.value().getResultItem(h.getLevel().registryAccess())) != null) out.add(r);
        }
        return out;
    }

    @GameTest(template = PLATFORM)
    public static void everyArmamentIsCraftedAtTierOneByItsOwnerAlone(GameTestHelper h) {
        ServerPlayer nezha = CharTest.as(h, NEZHA), erlang = CharTest.as(h, ERLANG), marshal = CharTest.as(h, MARSHAL),
                wukong = CharTest.as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        List<String> made = new ArrayList<>();
        for (var r : tierOnes(h)) {
            ShapedRecipe recipe = (ShapedRecipe) r.value();
            Armaments.Armament a = Armaments.of(recipe.getResultItem(h.getLevel().registryAccess()));
            ServerPlayer owner = a.owner() == NEZHA ? nezha : a.owner() == ERLANG ? erlang : marshal;
            CraftingInput in = grid(recipe);
            var mine = craft(h, owner, in);
            check(h, mine.isPresent() && mine.get().id().equals(r.id()), a.id() + ": its owner crafts it");
            ItemStack out = mine.get().value().assemble(in, h.getLevel().registryAccess());
            check(h, a.is().test(out), a.id() + ": the right armament (the Yin and Yang apart)");
            eq(h, 1, Armaments.tier(out), a.id() + " at tier I");
            for (ServerPlayer other : List.of(nezha, erlang, marshal, wukong)) {
                if (other == owner) continue;
                check(h, craft(h, other, in).isEmpty(), a.id() + ": no one else crafts it");
            }
            check(h, craft(h, null, in).isEmpty(), a.id() + ": nor a Crafter block, with no one at it");
            made.add(a.id());
        }
        for (Armaments.Armament a : Armaments.ALL) check(h, made.contains(a.id()), a.id() + " has a tier-I recipe");
        eq(h, Armaments.ALL.size(), made.size(), "one recipe each");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void everyArmamentRefinesThroughEveryTierToEight(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        for (Armaments.Armament a : Armaments.ALL) {
            for (int t = 2; t <= Armaments.TIERS; t++) {
                var next = ArmamentGuide.next(recipes, Armaments.at(a, t - 1));
                check(h, next != null, a.id() + ": a recipe to tier " + t);
                eq(h, t, next.tier(), a.id() + " to " + t);
                eq(h, t >= 5, next.furnace(), a.id() + " tier " + t + ": the furnace from V, a crafting table before");
                check(h, next.gate().allows(a.owner().id), a.id() + " tier " + t + ": its owner refines it");
                for (ReincarnationOutcome o : ReincarnationOutcome.values()) {
                    if (o != a.owner()) check(h, !next.gate().allows(o.id), a.id() + " tier " + t + ": not " + o.id);
                }
            }
            check(h, ArmamentGuide.next(recipes, Armaments.at(a, Armaments.TIERS)) == null, a.id() + ": nothing past VIII");
        }
        h.succeed();
    }

    /** Nezha's tier-II ingredients (data/jttw_engine/recipe/heavenly/nezha_tier_2.json) round an armament. */
    private static CraftingInput nezhaTierTwo(ItemStack armament) {
        List<ItemStack> g = new ArrayList<>(List.of(armament,
                new ItemStack(ModItems.SERVANT_BONE.get()), new ItemStack(ModItems.SERVANT_BONE.get()), new ItemStack(ModItems.SERVANT_BONE.get()),
                new ItemStack(ModItems.WRAITH_WISP.get()), new ItemStack(ModItems.WRAITH_WISP.get()),
                new ItemStack(com.jttw.engine.registry.FlowerFruitRegistry.MONKEY_HAIR.get()), new ItemStack(ModItems.CAPTAINS_SIGIL.get()), ItemStack.EMPTY));
        return CraftingInput.of(3, 3, g);
    }

    @GameTest(template = PLATFORM)
    public static void aCraftingTableRefinesATierAndKeepsTheRestOfIt(GameTestHelper h) {
        ServerPlayer nezha = CharTest.as(h, NEZHA), erlang = CharTest.as(h, ERLANG);
        ItemStack spear = new ItemStack(ModItems.FIRE_TIP_SPEAR.get());
        FireTipSpearItem.setAflame(spear, true);
        CraftingInput in = nezhaTierTwo(spear);
        var r = craft(h, nezha, in);
        check(h, r.isPresent() && r.get().value() instanceof RefineRecipe, "Nezha refines it");
        ItemStack out = r.get().value().assemble(in, h.getLevel().registryAccess());
        check(h, out.is(ModItems.FIRE_TIP_SPEAR.get()), "the same spear");
        eq(h, 2, Armaments.tier(out), "at tier II");
        check(h, FireTipSpearItem.isAflame(out), "all else about it kept");
        check(h, craft(h, erlang, in).isEmpty(), "Erlang Shen cannot");
        Armaments.setTier(spear, 2);
        check(h, craft(h, nezha, nezhaTierTwo(spear)).isEmpty(), "a tier-II spear does not take tier II's recipe again");
        List<ItemStack> short1 = new ArrayList<>(in.items());
        short1.set(7, ItemStack.EMPTY);
        Armaments.setTier(spear, 1);
        check(h, craft(h, nezha, CraftingInput.of(3, 3, short1)).isEmpty(), "a sigil short: nothing");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void noArmamentIsFoundInTheWorld(GameTestHelper h) {
        var server = h.getLevel().getServer();
        List<String> ids = new ArrayList<>();
        for (Armaments.Armament a : Armaments.ALL) ids.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(a.forged().get().getItem()).toString());
        int tables = 0;
        for (var e : server.getResourceManager().listResources("loot_table", rl -> rl.getPath().endsWith(".json")).entrySet()) {
            tables++;
            String text;
            try (BufferedReader r = e.getValue().openAsReader()) {
                text = r.lines().reduce("", (x, y) -> x + y);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
            for (String id : ids) check(h, !text.contains("\"" + id + "\""), e.getKey() + " hands out " + id);
        }
        check(h, tables > 50, "every loot table read: " + tables);
        // and no recipe makes one but the gated ones
        for (var r : h.getLevel().getRecipeManager().getRecipes()) {
            ItemStack out = r.value().getResultItem(h.getLevel().registryAccess());
            if (Armaments.of(out) == null) continue;
            check(h, r.value() instanceof GatedRecipe || r.value() instanceof com.jttw.engine.item.craft.TrigramsRefining,
                    r.id() + " makes an armament without a gate");
        }
        h.succeed();
    }

    /** Puts these armaments, each at this tier, in the player's armory. */
    private static void arm(ServerPlayer p, int tier, String... ids) {
        var armory = Armory.of(p);
        List<Armaments.Armament> slots = Armory.slots(p);
        for (int i = 0; i < slots.size(); i++) {
            for (String id : ids) if (slots.get(i).id().equals(id)) armory.setItem(i, Armaments.at(slots.get(i), tier));
        }
    }

    @GameTest(template = PLATFORM)
    public static void theHeavenlyStrengthLeansOnTheirArmamentsTiers(GameTestHelper h) {
        ServerPlayer nezha = CharTest.as(h, NEZHA);
        PlayerLevel.setLevel(nezha, 40); // ring III: levels 37-46
        near(h, Armory.attunement(3), Armory.strength(nezha), "no armaments at all: three tiers behind, the worst");
        String[] all = {"fire_tip_spear", "qiankun_ring", "yin_sword", "yang_sword", "wind_fire_wheels", "huntian_cloth"};
        arm(nezha, 3, all);
        near(h, 1.36, Armory.strength(nezha), "every one at tier III: x1, and 6% each");
        arm(nezha, 2, all);
        near(h, 1.36 / 1.3, Armory.strength(nezha), "every one a tier behind: a step down");
        arm(nezha, 3, "fire_tip_spear", "qiankun_ring", "yin_sword");
        near(h, 1.36 / Math.pow(1.3, 0.5), Armory.strength(nezha), "half of them behind: half a step");
        arm(nezha, 8, all);
        near(h, 1.36, Armory.strength(nezha), "ahead of the ring counts as level with it");
        // the stat it lands on
        ServerPlayer erlang = CharTest.as(h, ERLANG);
        PlayerLevel.setLevel(erlang, 60);
        com.jttw.engine.progression.level.LevelManager.reapplyStats(erlang);
        double bare = erlang.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        arm(erlang, 5, "three_pointed_spear", "erlang_axe", "golden_bow");
        Armory.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(erlang));
        erlang.tickCount = 20;
        Armory.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(erlang));
        near(h, bare * 1.18, erlang.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), "Erlang at 60 with all three at tier V: x1.18");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theHeavenlyCraftsAreTheirsAlone(GameTestHelper h) {
        ServerPlayer marshal = CharTest.as(h, MARSHAL), phoenix = CharTest.as(h, ReincarnationOutcome.PHOENIX);
        NonNullList<ItemStack> g = NonNullList.withSize(4, ItemStack.EMPTY);
        g.set(0, new ItemStack(net.minecraft.world.item.Items.PAPER));
        g.set(1, new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET));
        g.set(2, new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET));
        g.set(3, new ItemStack(net.minecraft.world.item.Items.REDSTONE));
        CraftingInput in = CraftingInput.of(2, 2, g);
        var r = craft(h, marshal, in);
        check(h, r.isPresent() && r.get().value().getResultItem(h.getLevel().registryAccess()).is(ModItems.FIVE_THUNDER_TALISMAN.get()), "a Heavenly crafts the Five-Thunder Talisman");
        eq(h, 2, r.get().value().assemble(in, h.getLevel().registryAccess()).getCount(), "two of them");
        check(h, craft(h, phoenix, in).isEmpty(), "a Divine Beast cannot");
        // and only a Heavenly can burn it
        Pig pig = CharTest.mob(h, EntityType.PIG, 2, 4);
        pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1e6);
        pig.setHealth(1e6f);
        phoenix.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, pig.position().add(0, 0.5, 0));
        phoenix.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIVE_THUNDER_TALISMAN.get()));
        phoenix.getMainHandItem().use(h.getLevel(), phoenix, InteractionHand.MAIN_HAND);
        eq(h, 1, phoenix.getMainHandItem().getCount(), "in a beast's hand it is paper");
        near(h, 1e6, pig.getHealth(), "and nothing struck");
        HeavenlyCraftItem.thunder(marshal, pig);
        check(h, pig.getHealth() < 1e6, "the Marshal's thunder strikes");
        check(h, HeavenlyCraftItem.THUNDER_AT_LEVEL_ONE * LevelPower.of(marshal) > 0, "scaled by his power");
        HeavenlyCraftItem.net(marshal, pig);
        check(h, com.jttw.engine.world.Stun.held(pig), "Heaven's Net pins it");
        pig.discard();
        h.succeed();
    }
}
