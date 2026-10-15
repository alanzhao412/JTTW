package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.LegacyItems;
import com.jttw.engine.transformation.BlockForms;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/** Items the mod saved itself under 1.20.1 still load under 1.21 (see LegacyItems). */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class LegacyItemsGameTests {

    /** A worn, renamed sword, as 1.20.1 saved it. */
    private static final String OLD_SWORD =
            "{id:\"minecraft:diamond_sword\",Count:1b,tag:{Damage:7,display:{Name:'{\"text\":\"Old Faithful\"}'}}}";

    private static CompoundTag parse(GameTestHelper h, String snbt) {
        try {
            return TagParser.parseTag(snbt);
        } catch (Exception e) {
            throw new IllegalStateException("bad test data: " + snbt, e);
        }
    }

    @GameTest(template = PLATFORM)
    public static void aStackSavedUnder1201LoadsWithItsCountDamageAndName(GameTestHelper h) {
        ItemStack sword = ItemStack.parseOptional(h.getLevel().registryAccess(), LegacyItems.upgrade(parse(h, OLD_SWORD)));
        eq(h, Items.DIAMOND_SWORD, sword.getItem(), "item");
        eq(h, 1, sword.getCount(), "count");
        eq(h, 7, sword.getDamageValue(), "damage");
        eq(h, "Old Faithful", sword.get(DataComponents.CUSTOM_NAME) == null ? null
                : sword.get(DataComponents.CUSTOM_NAME).getString(), "name");

        ItemStack pearls = ItemStack.parseOptional(h.getLevel().registryAccess(),
                LegacyItems.upgrade(parse(h, "{id:\"minecraft:ender_pearl\",Count:12b}")));
        eq(h, 12, pearls.getCount(), "stack size");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aStackSavedUnder121IsLeftAlone(GameTestHelper h) {
        CompoundTag saved = (CompoundTag) new ItemStack(Items.ENDER_PEARL, 5).save(h.getLevel().registryAccess());
        check(h, LegacyItems.upgrade(saved) == saved, "already 1.21: unchanged");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theChestFormKeepsWhatItHeldUnder1201(GameTestHelper h) {
        ServerPlayer p = player(h);
        CompoundTag sword = parse(h, OLD_SWORD);
        sword.putByte("Slot", (byte) 4);
        ListTag items = new ListTag();
        items.add(sword);
        CompoundTag chest = new CompoundTag();
        chest.put("Items", items);
        p.getPersistentData().put(BlockForms.KEY_CHEST, chest);

        List<ItemStack> loaded = BlockForms.loadChest(p);
        eq(h, Items.DIAMOND_SWORD, loaded.get(4).getItem(), "slot 4 holds the sword");
        eq(h, 7, loaded.get(4).getDamageValue(), "still worn");
        h.succeed();
    }
}
