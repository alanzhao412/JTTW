package com.jttw.engine.commands;

import com.jttw.engine.progression.LevelManager;
import com.jttw.engine.progression.PlayerLevel;
import com.jttw.engine.progression.ProgressionClass;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class LevelDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("jttw_setlevel")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int level = IntegerArgumentType.getInteger(ctx, "level");

                                    // Set level
                                    PlayerLevel.setLevel(player, level);

                                    // Optional but recommended: set XP to the floor for that level
                                    ProgressionClass cls = PlayerLevel.getProgressionClass(player);
                                    if (cls != null) {
                                        PlayerLevel.setXp(player, cls.growthRate.totalXpForLevel(level));
                                    }

                                    // Recompute and apply attributes for the new level
                                    LevelManager.reapplyStats(player);

                                    // Sync everything back to the client so UI/HUD updates immediately
                                    LevelManager.syncToClient(player);

                                    player.sendSystemMessage(
                                            net.minecraft.network.chat.Component.literal(
                                                    "§6Level manually set to §e" + level
                                            )
                                    );

                                    return 1;
                                })
                        )
        );
    }
}