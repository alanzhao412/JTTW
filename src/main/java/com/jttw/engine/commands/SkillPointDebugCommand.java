package com.jttw.engine.commands;

import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SkillPointDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("jttw_addsp")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                    // Skill points are stored in SkillTreeData (persistent NBT)
                                    SkillTreeData.addPoints(player, amount);

                                    // Sync to client so UI updates immediately (includes skill tree packet)
                                    LevelManager.syncToClient(player);

                                    player.sendSystemMessage(Component.literal(
                                            "§6Added §e" + amount + "§6 skill points. Total available: §e"
                                                    + SkillTreeData.getPoints(player)
                                    ));

                                    return 1;
                                })
                        )
        );
    }
}