package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.MixedIronCudgelItem;
import com.jttw.engine.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/**
 * How the signature weapons and treasures are come by in survival: the Mixed-Iron Cudgel at a
 * crafting table, the Nine-Tooth Rake in the Eight Trigrams Furnace ({@code FurnaceGameTests}), the
 * Bell of Taiyi in the Jade Palace's treasury ({@code HeavenGameTests}), the Yin-Yang Vase in
 * Lion-Camel City's ({@code LionCamelGameTests}); and none of them in any other loot.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class SignatureItemsGameTests {

    private static ItemStack craft(GameTestHelper h, Item... grid) {
        List<ItemStack> items = new ArrayList<>();
        for (Item i : grid) items.add(i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i));
        return h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, items), h.getLevel())
                .map(r -> r.value().assemble(CraftingInput.of(3, 3, items), h.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    @GameTest(template = PLATFORM)
    public static void theCudgelIsCraftedCheaplyAtACraftingTable(GameTestHelper h) {
        Item i = Items.IRON_INGOT, n = Items.IRON_NUGGET, s = Items.STICK, o = Items.AIR;
        check(h, craft(h, o, n, i, o, i, n, s, o, o).is(ModItems.MIXED_IRON_CUDGEL.get()), "two ingots, two nuggets and a stick make the cudgel");
        check(h, !craft(h, o, i, i, o, i, n, s, o, o).is(ModItems.MIXED_IRON_CUDGEL.get()), "not from the wrong pattern");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theCudgelIsAPlainIronClub(GameTestHelper h) {
        ItemStack club = new ItemStack(ModItems.MIXED_IRON_CUDGEL.get());
        double attack = ItemTestSupport.PLAYER_BASE_ATK + ItemTestSupport.modifiers(club, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE)
                .stream().mapToDouble(AttributeModifier::amount).sum();
        double speed = 4 + ItemTestSupport.modifiers(club, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED)
                .stream().mapToDouble(AttributeModifier::amount).sum();
        near(h, MixedIronCudgelItem.DAMAGE, attack, "its attack");
        near(h, MixedIronCudgelItem.SPEED, speed, "its swings a second");
        check(h, attack * speed < 6 * 1.6, "it hits no harder over time than an iron sword: " + attack * speed);
        eq(h, 250, club.getMaxDamage(), "an iron tool's durability");
        h.succeed();
    }

    /** Each signature item, and the only loot tables that may name it. */
    private static final Map<String, Set<String>> ONLY_IN = Map.of(
            "jttw_engine:bell_of_taiyi", Set.of("chests/jade_palace_treasury"),
            "jttw_engine:yin_yang_vase", Set.of("chests/lion_camel_treasury"),
            "jttw_engine:nine_tooth_rake", Set.of(),
            "jttw_engine:mixed_iron_cudgel", Set.of(),
            "jttw_engine:banana_leaf_fan", Set.of("chests/kunlun_treasure"));

    @GameTest(template = PLATFORM)
    public static void noOtherLootTableHoldsTheSignatureItems(GameTestHelper h) throws IOException {
        var resources = h.getLevel().getServer().getResourceManager().listResources("loot_table", p -> p.getPath().endsWith(".json"));
        check(h, resources.size() > 100, "the loot tables, vanilla's with ours: " + resources.size());
        List<String> found = new ArrayList<>();
        for (var e : resources.entrySet()) {
            String text;
            try (Reader r = e.getValue().openAsReader()) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[4096];
                for (int n; (n = r.read(buf)) > 0; ) sb.append(buf, 0, n);
                text = sb.toString();
            }
            ResourceLocation id = e.getKey();
            String table = id.getPath().substring("loot_table/".length(), id.getPath().length() - ".json".length());
            for (var item : ONLY_IN.entrySet()) {
                if (!text.contains(item.getKey())) continue;
                found.add(item.getKey() + " in " + id.getNamespace() + ":" + table);
                check(h, id.getNamespace().equals(JTTWEngine.MOD_ID) && item.getValue().contains(table),
                        item.getKey() + " is in " + id + ", and should be nowhere but " + item.getValue());
            }
        }
        eq(h, 3, found.size(), "the bell, the vase and the fan, each in its one table: " + found);
        h.succeed();
    }
}
