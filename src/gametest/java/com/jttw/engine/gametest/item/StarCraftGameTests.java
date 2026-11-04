package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.gametest.character.CharTest;
import com.jttw.engine.item.StarCraftItem;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.registry.HeavenRegistry;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.registry.ModRecipes;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity;
import com.jttw.engine.world.furnace.TrigramsRecipe;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jttw.engine.gametest.GT.*;

/**
 * Star Shards' crafts: the Eight Trigrams Furnace forges the Starfall Talisman and the Star-Essence
 * Elixir from them for any class; the star strikes every foe round where it lands, two of its strikes
 * killing a foe of the burner's level; the elixir heals, restores and washes off; and the shard, Heaven's
 * loot, is used.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class StarCraftGameTests {

    /** One of every class but the Heavenly, and a Heavenly. */
    private static final ReincarnationOutcome[] ANYONE = {ReincarnationOutcome.PHOENIX, ReincarnationOutcome.BULL_DEMON_KING,
            ReincarnationOutcome.CELESTIAL_PRIMATE, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY};

    private static TrigramsFurnaceBlockEntity furnace(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, com.jttw.engine.registry.ModBlocks.EIGHT_TRIGRAMS_FURNACE.get());
        return (TrigramsFurnaceBlockEntity) h.getBlockEntity(at);
    }

    private static void run(GameTestHelper h, TrigramsFurnaceBlockEntity f, int ticks) {
        for (int i = 0; i < ticks; i++) {
            TrigramsFurnaceBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), h.getLevel().getBlockState(f.getBlockPos()), f);
        }
    }

    /** Forges {@code result} from these materials (one slot each) for a smith of {@code who}; what lies in the centre after. */
    private static ItemStack forge(GameTestHelper h, ReincarnationOutcome who, Map<Item, Integer> materials) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        int slot = 0;
        for (var m : materials.entrySet()) f.setItem(slot++, new ItemStack(m.getKey(), m.getValue()));
        f.setItem(TrigramsFurnaceBlockEntity.FUEL, new ItemStack(ModItems.TRUE_SAMADHI_FIRE.get(), 8));
        f.setSmith(CharTest.as(h, who));
        check(h, f.forgingRecipe() != null, who.id + ": the furnace would forge it");
        run(h, f, TrigramsFurnaceBlockEntity.FORGE_TICKS);
        for (int i = 0; i < TrigramsFurnaceBlockEntity.TRIGRAMS; i++) check(h, f.getItem(i).isEmpty(), who.id + ": its materials used, slot " + i);
        ItemStack out = f.getItem(TrigramsFurnaceBlockEntity.CENTRE).copy();
        f.setItem(TrigramsFurnaceBlockEntity.CENTRE, ItemStack.EMPTY);
        return out;
    }

    private static Map<Item, Integer> talismanMaterials() {
        return Map.of(HeavenRegistry.STAR_SHARD.get(), 4, Items.PAPER, 2, Items.GLOWSTONE_DUST, 2, Items.GOLD_INGOT, 1);
    }

    private static Map<Item, Integer> elixirMaterials() {
        return Map.of(HeavenRegistry.STAR_SHARD.get(), 6, Items.GLOWSTONE_DUST, 3, Items.GLISTERING_MELON_SLICE, 1);
    }

    @GameTest(template = PLATFORM)
    public static void anyClassForgesTheStarCraftsInTheFurnace(GameTestHelper h) {
        for (ReincarnationOutcome who : ANYONE) {
            ItemStack talismans = forge(h, who, talismanMaterials());
            check(h, talismans.is(ModItems.STARFALL_TALISMAN.get()), who.id + " forges Starfall Talismans: " + talismans);
            eq(h, 2, talismans.getCount(), who.id + ": two talismans");
            ItemStack elixirs = forge(h, who, elixirMaterials());
            check(h, elixirs.is(ModItems.STAR_ESSENCE_ELIXIR.get()), who.id + " forges Star-Essence Elixirs: " + elixirs);
            eq(h, 2, elixirs.getCount(), who.id + ": two elixirs");
        }
        // a shard short, nothing
        TrigramsFurnaceBlockEntity f = furnace(h);
        f.setItem(0, new ItemStack(HeavenRegistry.STAR_SHARD.get(), 5));
        f.setItem(1, new ItemStack(Items.GLOWSTONE_DUST, 3));
        f.setItem(2, new ItemStack(Items.GLISTERING_MELON_SLICE, 1));
        f.setSmith(CharTest.as(h, ReincarnationOutcome.PHOENIX));
        check(h, f.forgingRecipe() == null, "a star shard short: nothing");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theStarShardIsAForgingMaterialForEveryone(GameTestHelper h) {
        Item shard = HeavenRegistry.STAR_SHARD.get();
        List<Item> made = new ArrayList<>();
        for (var r : h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.TRIGRAMS_FORGING.get())) {
            TrigramsRecipe recipe = r.value();
            if (!recipe.cost().containsKey(shard)) continue;
            made.add(recipe.result().getItem());
            for (ReincarnationOutcome o : ReincarnationOutcome.values()) {
                check(h, recipe.gate().allows(o.id), r.id() + ": no gate, but it stops " + o.id);
            }
        }
        check(h, made.contains(ModItems.STARFALL_TALISMAN.get()), "the shards make the Starfall Talisman");
        check(h, made.contains(ModItems.STAR_ESSENCE_ELIXIR.get()), "and the Star-Essence Elixir");
        // nothing else takes them: the Heavenly armaments' refining tiers are untouched
        for (var r : h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.TRIGRAMS_REFINING.get())) {
            check(h, r.value().materials().stream().noneMatch(m -> m.item() == shard), r.id() + " takes star shards");
        }
        h.succeed();
    }

    /** A pig with this much health (a pig has no resistances), standing at (x, z) on the platform. */
    private static Pig pig(GameTestHelper h, int x, int z, double health) {
        Pig pig = CharTest.mob(h, EntityType.PIG, x, z);
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        pig.setHealth((float) health);
        return pig;
    }

    @GameTest(template = PLATFORM)
    public static void theStarfallStrikesEveryFoeRoundWhereItLands(GameTestHelper h) {
        ServerPlayer phoenix = CharTest.as(h, ReincarnationOutcome.PHOENIX);
        PlayerLevel.setLevel(phoenix, 30);
        float blow = StarCraftItem.starfallDamage(phoenix);
        Pig target = pig(h, 2, 4, 1e6), beside = pig(h, 4, 4, 1e6), far = pig(h, 2, 4, 1e6);
        far.moveTo(far.getX(), far.getY() + 6, far.getZ()); // six blocks over it: out of the burst
        Wolf pet = CharTest.mob(h, EntityType.WOLF, 0, 4);
        pet.tame(phoenix);
        float petHealth = pet.getHealth();
        phoenix.lookAt(EntityAnchorArgument.Anchor.EYES, target.position().add(0, 0.5, 0));
        phoenix.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STARFALL_TALISMAN.get(), 3));
        phoenix.getMainHandItem().use(h.getLevel(), phoenix, InteractionHand.MAIN_HAND);
        eq(h, 2, phoenix.getMainHandItem().getCount(), "a Divine Beast burns it: one used");
        check(h, phoenix.getCooldowns().isOnCooldown(ModItems.STARFALL_TALISMAN.get()), "and a moment before the next");
        lost(h, blow, target, "the creature looked at, struck");
        lost(h, blow, beside, "and the one beside it");
        lost(h, 0, far, "but not one out of its reach");
        near(h, petHealth, pet.getHealth(), "nor the burner's own pet");
        // with nothing looked at, it is not spent
        phoenix.lookAt(EntityAnchorArgument.Anchor.EYES, phoenix.getEyePosition().add(0, -5, -20));
        phoenix.getCooldowns().removeCooldown(ModItems.STARFALL_TALISMAN.get());
        phoenix.getMainHandItem().use(h.getLevel(), phoenix, InteractionHand.MAIN_HAND);
        eq(h, 2, phoenix.getMainHandItem().getCount(), "nothing there: not burned");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void twoStarfallsKillAFoeOfTheBurnersLevel(GameTestHelper h) {
        for (ReincarnationOutcome who : ANYONE) {
            ServerPlayer p = CharTest.as(h, who);
            for (int level : new int[]{1, 10, 20, 35, 50, 65, 80, 95, 100}) {
                PlayerLevel.setLevel(p, level);
                // a Divine Beast owing an evolution fights at the level before it, as its power does
                double health = MobScaling.typicalHealth(com.jttw.engine.character.divinebeast.BeastEvolution.poweredLevel(p, level));
                close(h, health / 2, StarCraftItem.starfallDamage(p), who.id + " at " + level + ": half a same-level character's health");
                Pig foe = pig(h, 2, 4, health);
                StarCraftItem.starfall(p, foe);
                check(h, foe.isAlive() && foe.getHealth() > 0, who.id + " at " + level + ": one strike leaves a same-level foe standing");
                foe.invulnerableTime = 0;
                StarCraftItem.starfall(p, foe);
                check(h, !foe.isAlive() || foe.getHealth() <= 0, who.id + " at " + level + ": two kill it (" + foe.getHealth() + " left of " + health + ")");
                foe.discard();
            }
        }
        h.succeed();
    }

    /** It has lost this much of its million health (to a float's grain). */
    private static void lost(GameTestHelper h, double expected, LivingEntity e, String what) {
        double lost = 1e6 - e.getHealth();
        check(h, Math.abs(lost - expected) < 0.2, what + ": lost " + lost + ", expected " + expected);
    }

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= expected * 1e-5, what + ": expected " + expected + " but was " + actual);
    }

    @GameTest(template = PLATFORM)
    public static void theStarEssenceElixirHealsRestoresAndWashesOff(GameTestHelper h) {
        ServerPlayer tiger = CharTest.as(h, ReincarnationOutcome.WHITE_TIGER);
        PlayerLevel.setLevel(tiger, 40);
        com.jttw.engine.progression.level.LevelManager.reapplyStats(tiger);
        float max = tiger.getMaxHealth();
        tiger.setHealth(max * 0.2f);
        CharTest.qi(tiger, 0, 500);
        tiger.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400));
        tiger.addEffect(new MobEffectInstance(MobEffects.POISON, 400));
        tiger.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 1));
        tiger.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400));
        ItemStack elixirs = new ItemStack(ModItems.STAR_ESSENCE_ELIXIR.get(), 2);
        tiger.setItemInHand(InteractionHand.MAIN_HAND, elixirs);
        tiger.getMainHandItem().use(h.getLevel(), tiger, InteractionHand.MAIN_HAND);
        check(h, tiger.isUsingItem(), "a Divine Beast eats it");
        ItemStack left = elixirs.finishUsingItem(h.getLevel(), tiger);
        eq(h, 1, left.getCount(), "one eaten");
        close(h, max * 0.7, tiger.getHealth(), "half its health back");
        close(h, 250, QiData.getCurrent(tiger), "half its Qi");
        for (var e : List.of(MobEffects.BLINDNESS, MobEffects.POISON, MobEffects.MOVEMENT_SLOWDOWN)) {
            check(h, !tiger.hasEffect(e), e.getRegisteredName() + " washed off");
        }
        check(h, tiger.hasEffect(MobEffects.DAMAGE_BOOST), "a good effect kept");
        check(h, tiger.hasEffect(MobEffects.NIGHT_VISION), "starlight sight");
        check(h, tiger.getCooldowns().isOnCooldown(ModItems.STAR_ESSENCE_ELIXIR.get()), "and half a minute before the next");
        // it never heals past full
        tiger.setHealth(max * 0.9f);
        StarCraftItem.drink(tiger);
        close(h, max, tiger.getHealth(), "no more than full");
        h.succeed();
    }
}
