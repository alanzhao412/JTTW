package com.jttw.engine.commands;

import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.reincarnation.ReincarnationResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class AlignmentDebugCommand {
    private AlignmentDebugCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("jttw")
                // -------------------------
                // Alignment debug commands
                // -------------------------
                .then(literal("align")
                    .requires(cs -> cs.hasPermission(2)) // OP only
                    .then(literal("show")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int h = AlignmentData.getHeavenly(p);
                            int sb = AlignmentData.getSpiritBeast(p);
                            int pr = AlignmentData.getPrimordial(p);
                            int y = AlignmentData.getYaoguai(p);
                            int total = AlignmentData.getTotal(p);

                            ctx.getSource().sendSuccess(
                                () -> Component.literal("Alignment: H=" + h + " SB=" + sb + " P=" + pr + " Y=" + y + " | TOTAL=" + total),
                                false
                            );
                            return 1;
                        })
                    )
                    .then(literal("set")
                        .then(argument("heavenly", IntegerArgumentType.integer(0))
                            .then(argument("spiritBeast", IntegerArgumentType.integer(0))
                                .then(argument("primordial", IntegerArgumentType.integer(0))
                                    .then(argument("yaoguai", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            int h = IntegerArgumentType.getInteger(ctx, "heavenly");
                                            int sb = IntegerArgumentType.getInteger(ctx, "spiritBeast");
                                            int pr = IntegerArgumentType.getInteger(ctx, "primordial");
                                            int y = IntegerArgumentType.getInteger(ctx, "yaoguai");

                                            AlignmentData.setHeavenly(p, h);
                                            AlignmentData.setSpiritBeast(p, sb);
                                            AlignmentData.setPrimordial(p, pr);
                                            AlignmentData.setYaoguai(p, y);

                                            ctx.getSource().sendSuccess(
                                                () -> Component.literal("Set alignment: H=" + h + " SB=" + sb + " P=" + pr + " Y=" + y),
                                                false
                                            );
                                            return 1;
                                        })
                                    )
                                )
                            )
                        )
                    )
                    .then(literal("add")
                        .then(argument("heavenly", IntegerArgumentType.integer(0))
                            .then(argument("spiritBeast", IntegerArgumentType.integer(0))
                                .then(argument("primordial", IntegerArgumentType.integer(0))
                                    .then(argument("yaoguai", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            int h = IntegerArgumentType.getInteger(ctx, "heavenly");
                                            int sb = IntegerArgumentType.getInteger(ctx, "spiritBeast");
                                            int pr = IntegerArgumentType.getInteger(ctx, "primordial");
                                            int y = IntegerArgumentType.getInteger(ctx, "yaoguai");

                                            AlignmentData.addHeavenly(p, h);
                                            AlignmentData.addSpiritBeast(p, sb);
                                            AlignmentData.addPrimordial(p, pr);
                                            AlignmentData.addYaoguai(p, y);

                                            ctx.getSource().sendSuccess(
                                                () -> Component.literal("Added alignment: +H=" + h + " +SB=" + sb + " +P=" + pr + " +Y=" + y),
                                                false
                                            );
                                            return 1;
                                        })
                                    )
                                )
                            )
                        )
                    )
                    .then(literal("predict")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ReincarnationOutcome o = ReincarnationResolver.resolve(
                                AlignmentData.getHeavenly(p),
                                AlignmentData.getSpiritBeast(p),
                                AlignmentData.getPrimordial(p),
                                AlignmentData.getYaoguai(p)
                            );

                            ctx.getSource().sendSuccess(
                                () -> Component.literal("Predicted reincarnation: " + o.id),
                                false
                            );
                            return 1;
                        })
                    )
                    .then(literal("force")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ReincarnationOutcome o = ReincarnationResolver.resolve(
                                AlignmentData.getHeavenly(p),
                                AlignmentData.getSpiritBeast(p),
                                AlignmentData.getPrimordial(p),
                                AlignmentData.getYaoguai(p)
                            );

                            AlignmentData.markReincarnated(p, o);
                            AlignmentData.resetAlignment(p);

                            ctx.getSource().sendSuccess(
                                () -> Component.literal("FORCED reincarnation marker as: " + o.id + " (alignment reset)"),
                                false
                            );
                            return 1;
                        })
                    )
                )

                // -------------------------
                // Flight debug commands
                // -------------------------
                .then(literal("flight")
                    .requires(cs -> cs.hasPermission(2)) // OP only
                    .then(literal("status")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            boolean enabled = JTTWFlight.hasUniversalFlight(p);
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("Universal flight is " + (enabled ? "ON" : "OFF")),
                                false
                            );
                            return 1;
                        })
                    )
                    .then(literal("on")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();

                            JTTWFlight.setUniversalFlight(p, true);

                            // apply immediately (your tick should also keep it correct)
                            p.getAbilities().mayfly = true;
                            p.onUpdateAbilities();

                            ctx.getSource().sendSuccess(() -> Component.literal("Universal flight enabled."), false);
                            return 1;
                        })
                    )
                    .then(literal("off")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();

                            JTTWFlight.setUniversalFlight(p, false);

                            // apply immediately
                            p.getAbilities().flying = false;
                            p.getAbilities().mayfly = false;
                            p.onUpdateAbilities();

                            ctx.getSource().sendSuccess(() -> Component.literal("Universal flight disabled."), false);
                            return 1;
                        })
                    )
                    .then(literal("toggle")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            boolean next = !JTTWFlight.hasUniversalFlight(p);

                            JTTWFlight.setUniversalFlight(p, next);

                            p.getAbilities().mayfly = next;
                            if (!next) p.getAbilities().flying = false;
                            p.onUpdateAbilities();

                            ctx.getSource().sendSuccess(
                                () -> Component.literal("Universal flight toggled " + (next ? "ON" : "OFF") + "."),
                                false
                            );
                            return 1;
                        })
                    )
                )

                // -------------------------
                // Character switch command
                // -------------------------
                .then(literal("setclass")
                    .requires(cs -> cs.hasPermission(2))
                    .then(argument("class", StringArgumentType.word())
                        .suggests(CLASS_SUGGESTIONS)
                        .then(argument("subclass", StringArgumentType.word())
                            .suggests(SUBCLASS_SUGGESTIONS)
                            .executes(ctx -> {
                                ServerPlayer p = ctx.getSource().getPlayerOrException();
                                String subclassName = StringArgumentType.getString(ctx, "subclass");

                                ReincarnationOutcome found = null;
                                for (ReincarnationOutcome o : ReincarnationOutcome.values()) {
                                    if (o.id.equalsIgnoreCase(subclassName) || o.name().equalsIgnoreCase(subclassName)) {
                                        found = o;
                                        break;
                                    }
                                }

                                if (found == null) {
                                    ctx.getSource().sendFailure(Component.literal(
                                            "§cUnknown subclass: " + subclassName + ". Use tab to see options."));
                                    return 0;
                                }

                                AlignmentData.markReincarnated(p, found);
                                AlignmentData.resetAlignment(p);
                                LevelManager.initClass(p, found);

                                final ReincarnationOutcome result = found;
                                ctx.getSource().sendSuccess(
                                    () -> Component.literal("§aCharacter set to: §e" + result.id
                                            + " §a(" + com.jttw.engine.progression.stats.ProgressionClass.fromOutcome(result).name() + ")"),
                                    false
                                );
                                return 1;
                            })
                        )
                    )
                )

                // -------------------------
                // Show current character
                // -------------------------
                .then(literal("whoami")
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        return showPlayerInfo(ctx.getSource(), p);
                    })
                    .then(argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            return showPlayerInfo(ctx.getSource(), p);
                        })
                    )
                )
        );
    }

    private static int showPlayerInfo(CommandSourceStack source, ServerPlayer p) {
        String outcome = p.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME);
        boolean reincarnated = AlignmentData.hasReincarnated(p);
        String name = p.getGameProfile().getName();

        if (!reincarnated || outcome.isEmpty()) {
            source.sendSuccess(
                () -> Component.literal("§f" + name + " §7has not yet reincarnated."),
                false
            );
        } else {
            int h = AlignmentData.getHeavenly(p);
            int sb = AlignmentData.getSpiritBeast(p);
            int pr = AlignmentData.getPrimordial(p);
            int y = AlignmentData.getYaoguai(p);
            int total = h + sb + pr + y;

            source.sendSuccess(
                () -> Component.literal("§f" + name + " §e— " + outcome
                        + "\n§eAlignment: §fH=" + h + " SB=" + sb + " P=" + pr + " Y=" + y + " (total=" + total + ")"),
                false
            );
        }
        return 1;
    }

    /** Tab-completion for class names (primordial, heavenly, divine_beast, yaoguai). */
    private static final SuggestionProvider<CommandSourceStack> CLASS_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.stream(com.jttw.engine.progression.stats.ProgressionClass.values())
                            .map(c -> c.name().toLowerCase()),
                    builder
            );

    /** Tab-completion for subclass/outcome names, filtered by the selected class. */
    private static final SuggestionProvider<CommandSourceStack> SUBCLASS_SUGGESTIONS =
            (ctx, builder) -> {
                String className = StringArgumentType.getString(ctx, "class");
                com.jttw.engine.progression.stats.ProgressionClass cls = null;
                for (com.jttw.engine.progression.stats.ProgressionClass c : com.jttw.engine.progression.stats.ProgressionClass.values()) {
                    if (c.name().equalsIgnoreCase(className)) {
                        cls = c;
                        break;
                    }
                }
                final com.jttw.engine.progression.stats.ProgressionClass finalCls = cls;
                return SharedSuggestionProvider.suggest(
                        Arrays.stream(ReincarnationOutcome.values())
                                .filter(o -> finalCls == null || com.jttw.engine.progression.stats.ProgressionClass.fromOutcome(o) == finalCls)
                                .map(o -> o.id),
                        builder
                );
            };
}