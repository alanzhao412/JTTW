package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.ridge.BoneServantEntity;
import com.jttw.engine.item.Armaments;
import com.jttw.engine.item.YinYangSwordItem;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;

/** Laozi's Eight Trigrams Furnace: armaments are forged there from the zones' materials, refined there, and nowhere else. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FurnaceGameTests {

    private static TrigramsFurnaceBlockEntity furnace(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, ModBlocks.EIGHT_TRIGRAMS_FURNACE.get());
        return (TrigramsFurnaceBlockEntity) h.getBlockEntity(at);
    }

    /** Runs the furnace so many ticks. */
    private static void run(GameTestHelper h, TrigramsFurnaceBlockEntity f, int ticks) {
        for (int i = 0; i < ticks; i++) {
            TrigramsFurnaceBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), h.getLevel().getBlockState(f.getBlockPos()), f);
        }
    }

    private static void put(TrigramsFurnaceBlockEntity f, int slot, Item item, int count) {
        f.setItem(slot, new ItemStack(item, count));
    }

    @GameTest(template = PLATFORM)
    public static void exactlyTheMaterialsWithFuelForgeAnArmament(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        put(f, 0, ModItems.SERVANT_BONE.get(), 6);
        put(f, 1, ModItems.WRAITH_WISP.get(), 2);
        put(f, 2, ModItems.CAPTAINS_SIGIL.get(), 1);
        put(f, TrigramsFurnaceBlockEntity.FUEL, ModItems.TRUE_SAMADHI_FIRE.get(), 8);
        check(h, f.forging() != null && f.forging().id().equals("fire_tip_spear"), "it would forge the Fire-Tipped Spear");
        run(h, f, TrigramsFurnaceBlockEntity.FORGE_TICKS - 1);
        check(h, f.getItem(TrigramsFurnaceBlockEntity.CENTRE).isEmpty(), "not before its time");
        run(h, f, 1);
        check(h, f.getItem(TrigramsFurnaceBlockEntity.CENTRE).is(ModItems.FIRE_TIP_SPEAR.get()), "forged into the centre");
        for (int i = 0; i < TrigramsFurnaceBlockEntity.TRIGRAMS; i++) check(h, f.getItem(i).isEmpty(), "its materials used");
        eq(h, 0, Armaments.refine(f.getItem(TrigramsFurnaceBlockEntity.CENTRE)), "unrefined");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nothingForgesWithoutFuelOrWithTheWrongMaterials(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        put(f, 0, ModItems.SERVANT_BONE.get(), 6);
        put(f, 1, ModItems.WRAITH_WISP.get(), 2);
        put(f, 2, ModItems.CAPTAINS_SIGIL.get(), 1);
        run(h, f, TrigramsFurnaceBlockEntity.FORGE_TICKS + 5);
        check(h, f.getItem(TrigramsFurnaceBlockEntity.CENTRE).isEmpty(), "no fire, no forging");
        put(f, TrigramsFurnaceBlockEntity.FUEL, ModItems.TRUE_SAMADHI_FIRE.get(), 8);
        put(f, 3, Items.DIRT, 1);
        check(h, f.forging() == null, "a stray material: no armament's recipe");
        run(h, f, TrigramsFurnaceBlockEntity.FORGE_TICKS + 5);
        check(h, f.getItem(TrigramsFurnaceBlockEntity.CENTRE).isEmpty(), "nothing forged");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theYinAndYangSwordsAreForgedApart(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        put(f, 0, ModItems.WRAITH_WISP.get(), 4);
        put(f, 1, ModItems.SERVANT_BONE.get(), 3);
        put(f, 2, ModItems.CAPTAINS_SIGIL.get(), 1);
        put(f, TrigramsFurnaceBlockEntity.FUEL, ModItems.TRUE_SAMADHI_FIRE.get(), 8);
        run(h, f, TrigramsFurnaceBlockEntity.FORGE_TICKS);
        check(h, YinYangSwordItem.isYin(f.getItem(TrigramsFurnaceBlockEntity.CENTRE)), "wisp-heavy: Yin");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void refiningRaisesAnArmamentALevelForItsCost(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        f.setItem(TrigramsFurnaceBlockEntity.CENTRE, new ItemStack(ModItems.THREE_POINTED_SPEAR.get()));
        put(f, 0, ModItems.SERVANT_BONE.get(), 5); // level 0 -> 1 takes 2 bones and a wisp
        put(f, 1, ModItems.WRAITH_WISP.get(), 1);
        put(f, TrigramsFurnaceBlockEntity.FUEL, ModItems.TRUE_SAMADHI_FIRE.get(), 8);
        run(h, f, TrigramsFurnaceBlockEntity.REFINE_BASE_TICKS);
        eq(h, 1, Armaments.refine(f.getItem(TrigramsFurnaceBlockEntity.CENTRE)), "refined to 1");
        eq(h, 3, f.getItem(0).getCount(), "two bones used, the rest left");
        check(h, f.getItem(1).isEmpty(), "its wisp used");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theLastRefinementTakesLadyWhiteBonesHeart(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        ItemStack axe = new ItemStack(ModItems.ERLANG_AXE.get());
        Armaments.setRefine(axe, Armaments.MAX_REFINE - 1);
        f.setItem(TrigramsFurnaceBlockEntity.CENTRE, axe);
        put(f, 0, ModItems.SERVANT_BONE.get(), 64);
        put(f, 1, ModItems.WRAITH_WISP.get(), 64);
        put(f, 2, ModItems.CAPTAINS_SIGIL.get(), 64);
        put(f, TrigramsFurnaceBlockEntity.FUEL, ModItems.TRUE_SAMADHI_FIRE.get(), 64);
        check(h, f.refining() == null, "not without the heart");
        put(f, 3, ModItems.HEART_BONE.get(), 1);
        check(h, f.refining() != null, "with it");
        run(h, f, TrigramsFurnaceBlockEntity.REFINE_BASE_TICKS + TrigramsFurnaceBlockEntity.REFINE_TICKS_PER_LEVEL * (Armaments.MAX_REFINE - 1));
        eq(h, Armaments.MAX_REFINE, Armaments.refine(f.getItem(TrigramsFurnaceBlockEntity.CENTRE)), "fully refined");
        check(h, f.refining() == null, "and no further");
        h.succeed();
    }

    private static double attackBonus(ItemStack stack) {
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream()
                .mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount).sum();
    }

    @GameTest(template = PLATFORM)
    public static void aRefinedArmamentGivesMoreOfItsBonus(GameTestHelper h) {
        ItemStack spear = new ItemStack(ModItems.THREE_POINTED_SPEAR.get());
        near(h, 0.20, attackBonus(spear), "+20% attack unrefined");
        Armaments.setRefine(spear, 5);
        near(h, 0.30, attackBonus(spear), "+30% at refinement 5 (its bonus x1.5)");
        Armaments.setRefine(spear, Armaments.MAX_REFINE);
        near(h, 0.40, attackBonus(spear), "+40% fully refined (x2)");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void noVanillaRecipeMakesAnArmament(GameTestHelper h) {
        for (Recipe<?> r : h.getLevel().getRecipeManager().getRecipes()) {
            ItemStack out = r.getResultItem(h.getLevel().registryAccess());
            check(h, Armaments.of(out) == null, r.getId() + " makes an armament: only the furnace forges them");
        }
        check(h, h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(JTTWEngine.MOD_ID, "eight_trigrams_furnace")).isPresent(),
                "the furnace itself is crafted");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aCaptainLeavesItsSigil(GameTestHelper h) {
        ServerPlayer p = player(h);
        BoneServantEntity captain = h.spawn(ModEntities.BONE_SERVANT.get(), new BlockPos(3, 2, 3));
        captain.become(27, true);
        captain.hurt(h.getLevel().damageSources().playerAttack(p), Float.MAX_VALUE);
        var sigils = h.getLevel().getEntitiesOfClass(ItemEntity.class, captain.getBoundingBox().inflate(4),
                e -> e.getItem().is(ModItems.CAPTAINS_SIGIL.get()));
        check(h, !sigils.isEmpty(), "a sigil dropped");
        sigils.forEach(ItemEntity::discard);
        h.succeed();
    }
}
