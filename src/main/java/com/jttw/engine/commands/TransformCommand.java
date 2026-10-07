package com.jttw.engine.commands;

import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.OpenTransformationMenuPacket;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;

public final class TransformCommand {

    private TransformCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("jttw")
                        .then(Commands.literal("transform")
                                // /jttw transform -> open GUI
                                .executes(TransformCommand::openMenu)

                                // /jttw transform <entity>
                                .then(Commands.argument("entity", ResourceLocationArgument.id())
                                        .executes(TransformCommand::transformSelf)

                                        // /jttw transform <entity> <targets>
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .requires(source -> source.hasPermission(2))
                                                .executes(TransformCommand::transformTargets)
                                        )
                                )
                        )

                        .then(Commands.literal("demorph")
                                .executes(TransformCommand::demorphSelf)
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(TransformCommand::demorphTargets)
                                )
                        )

                        .then(Commands.literal("status")
                                .executes(TransformCommand::statusSelf)
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(TransformCommand::statusTargets)
                                )
                        )
        );
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static ServerPlayer selfPlayerOrFail(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer sp) return sp;
        return null;
    }

    /**
     * Robust LivingEntity validation for your environment:
     * - look up type
     * - instantiate in server level
     * - check instanceof LivingEntity
     */
    private static EntityType<?> requireLivingEntityType(ServerPlayer validationPlayer, ResourceLocation id) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) return null;

        Entity created = type.create(validationPlayer.serverLevel());
        if (!(created instanceof LivingEntity)) return null;

        return type;
    }

    // ------------------------------------------------------------
    // /jttw transform
    // ------------------------------------------------------------

    private static int openMenu(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = selfPlayerOrFail(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only a player can open the transformation menu."));
            return 0;
        }
        NetworkHandler.sendToPlayer(player, new OpenTransformationMenuPacket());
        return 1;
    }

    private static int transformSelf(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = selfPlayerOrFail(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only a player can transform themselves."));
            return 0;
        }

        ResourceLocation id = ResourceLocationArgument.getId(ctx, "entity");
        EntityType<?> type = requireLivingEntityType(player, id);

        if (type == null) {
            player.sendSystemMessage(Component.literal("Invalid entity (must be a LivingEntity): " + id));
            return 0;
        }

        TransformationManager.transform(player, type);
        player.sendSystemMessage(Component.literal("Transformed into " + id));
        return 1;
    }

    private static int transformTargets(CommandContext<CommandSourceStack> ctx) {
        // Use the command runner (source player) to validate/instantiate the entity
        ServerPlayer sourcePlayer = selfPlayerOrFail(ctx);
        if (sourcePlayer == null) {
            ctx.getSource().sendFailure(Component.literal("Only a player can run this command."));
            return 0;
        }

        ResourceLocation id = ResourceLocationArgument.getId(ctx, "entity");
        EntityType<?> type = requireLivingEntityType(sourcePlayer, id);

        if (type == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid entity (must be a LivingEntity): " + id));
            return 0;
        }

        final Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(ctx, "targets");
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid target(s)."));
            return 0;
        }

        for (ServerPlayer target : targets) {
            TransformationManager.transform(target, type);
        }

        ctx.getSource().sendSuccess(() ->
                        Component.literal("Transformed " + targets.size() + " player(s) into " + id),
                true
        );
        return targets.size();
    }

    // ------------------------------------------------------------
    // /jttw demorph
    // ------------------------------------------------------------

    private static int demorphSelf(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = selfPlayerOrFail(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only a player can demorph themselves."));
            return 0;
        }
        TransformationManager.demorph(player);
        player.sendSystemMessage(Component.literal("You have returned to normal."));
        return 1;
    }

    private static int demorphTargets(CommandContext<CommandSourceStack> ctx) {
        final Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(ctx, "targets");
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid target(s)."));
            return 0;
        }

        for (ServerPlayer target : targets) {
            TransformationManager.demorph(target);
        }

        ctx.getSource().sendSuccess(() ->
                        Component.literal("Demorphed " + targets.size() + " player(s)."),
                true
        );
        return targets.size();
    }

    // ------------------------------------------------------------
    // /jttw status
    // ------------------------------------------------------------

    private static int statusSelf(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = selfPlayerOrFail(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only a player can check status."));
            return 0;
        }

        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null) {
            player.sendSystemMessage(Component.literal("You are not transformed."));
        } else {
            player.sendSystemMessage(Component.literal("Current form: " + data.getEntityId()));
        }
        return 1;
    }

    private static int statusTargets(CommandContext<CommandSourceStack> ctx) {
        final Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(ctx, "targets");
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid target(s)."));
            return 0;
        }

        for (ServerPlayer target : targets) {
            TransformationData data = TransformationManager.getTransformation(target);
            if (data == null) {
                ctx.getSource().sendSuccess(() ->
                                Component.literal(target.getGameProfile().getName() + " is not transformed."),
                        false
                );
            } else {
                ctx.getSource().sendSuccess(() ->
                                Component.literal(target.getGameProfile().getName() + " is transformed into " + data.getEntityId()),
                        false
                );
            }
        }

        return targets.size();
    }
}