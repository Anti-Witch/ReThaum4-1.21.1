package thaumcraft.common.lib;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketWarpMessage;
import thaumcraft.common.lib.research.ResearchManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandThaumcraft {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("thaumcraft")
                .requires(source -> source.hasPermission(2));

        // Aliases
        dispatcher.register(Commands.literal("thaum").redirect(builder.build()));
        dispatcher.register(Commands.literal("tc").redirect(builder.build()));

        // Help
        builder.then(Commands.literal("help")
                .executes(context -> help(context.getSource())));

        // Reload
        builder.then(Commands.literal("reload")
                .executes(context -> reload(context.getSource())));

        // Research
        builder.then(Commands.literal("research")
                .then(Commands.literal("list")
                        .executes(context -> listResearch(context.getSource())))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("list")
                                .executes(context -> listPlayerResearch(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                        .then(Commands.literal("all")
                                .executes(context -> giveAllResearch(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                        .then(Commands.literal("reset")
                                .executes(context -> resetResearch(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                        .then(Commands.argument("research", StringArgumentType.string())
                                .suggests(RESEARCH_SUGGESTIONS)
                                .executes(context -> giveResearch(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "research")))
                                .then(Commands.literal("revoke")
                                        .executes(context -> revokeResearch(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "research"))))
                        )
                )
        );

        // Warp
        builder.then(Commands.literal("warp")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> addWarp(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "amount"), ""))
                                        .then(Commands.literal("PERM")
                                                .executes(context -> addWarp(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "amount"), "PERM")))
                                        .then(Commands.literal("TEMP")
                                                .executes(context -> addWarp(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "amount"), "TEMP")))
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> setWarp(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "amount"), ""))
                                        .then(Commands.literal("PERM")
                                                .executes(context -> setWarp(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "amount"), "PERM")))
                                        .then(Commands.literal("TEMP")
                                                .executes(context -> setWarp(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "amount"), "TEMP")))
                                )
                        )
                )
        );

        dispatcher.register(builder);
    }

    private static final SuggestionProvider<CommandSourceStack> RESEARCH_SUGGESTIONS = (context, builder) -> {
        List<String> keys = new ArrayList<>();
        for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : cat.research.values()) {
                keys.add(ri.getKey());
            }
        }
        return SharedSuggestionProvider.suggest(keys, builder);
    };

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("В§3You can also use /thaum or /tc instead of /thaumcraft."), false);
        source.sendSuccess(() -> Component.literal("В§3Use this to give research to a player."), false);
        source.sendSuccess(() -> Component.literal("  /thaumcraft research <list|player> <list|all|reset|<research>>"), false);
        source.sendSuccess(() -> Component.literal("В§3Use this to remove research from a player."), false);
        source.sendSuccess(() -> Component.literal("  /thaumcraft research <player> <research> revoke"), false);
        source.sendSuccess(() -> Component.literal("В§3Use this to set a player's warp level."), false);
        source.sendSuccess(() -> Component.literal("  /thaumcraft warp <player> <add|set> <amount> [PERM|TEMP]"), false);
        source.sendSuccess(() -> Component.literal("  not specifying perm or temp will just add normal warp"), false);
        source.sendSuccess(() -> Component.literal("В§3Use this to reload json research data"), false);
        source.sendSuccess(() -> Component.literal("  /thaumcraft reload"), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        ConfigResearch.init(); 
        source.sendSuccess(() -> Component.literal("В§5Reloaded research data."), true);
        return 1;
    }

    private static int listResearch(CommandSourceStack source) {
        for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : cat.research.values()) {
                source.sendSuccess(() -> Component.literal("В§5" + ri.getKey()), false);
            }
        }
        return 1;
    }

    private static int listPlayerResearch(CommandSourceStack source, ServerPlayer player) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null) {
            String ss = String.join(", ", knowledge.getResearchList());
            source.sendSuccess(() -> Component.literal("В§5Research for " + player.getName().getString()), false);
            source.sendSuccess(() -> Component.literal("В§5" + ss), false);
        }
        return 1;
    }

    private static int giveAllResearch(CommandSourceStack source, ServerPlayer player) {
        for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : cat.research.values()) {
                giveRecursiveResearch(player, ri.getKey());
            }
        }
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null) {
            knowledge.sync(player);
        }
        source.sendSuccess(() -> Component.literal("В§5Gave all research to " + player.getName().getString()), true);
        return 1;
    }

    private static int resetResearch(CommandSourceStack source, ServerPlayer player) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge != null) {
            knowledge.clear();
            for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
                for (ResearchEntry ri : cat.research.values()) {
                    if (ri.hasMeta(ResearchEntry.EnumResearchMeta.AUTOUNLOCK)) {
                        ResearchManager.completeResearch(player, ri.getKey(), false);
                    }
                }
            }
            knowledge.sync(player);
        }
        source.sendSuccess(() -> Component.literal("В§5Reset research for " + player.getName().getString()), true);
        return 1;
    }

    private static int giveResearch(CommandSourceStack source, ServerPlayer player, String research) {
        if (ResearchCategories.getResearch(research) != null) {
            giveRecursiveResearch(player, research);
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge != null) {
                knowledge.sync(player);
            }
            source.sendSuccess(() -> Component.literal("В§5Gave research " + research + " to " + player.getName().getString()), true);
        } else {
            source.sendFailure(Component.literal("В§cResearch does not exist."));
        }
        return 1;
    }

    public static void giveRecursiveResearch(ServerPlayer player, String research) {
        if (research.contains("@")) {
            research = research.substring(0, research.indexOf("@"));
        }
        String finalResearch = research;
        ResearchEntry res = ResearchCategories.getResearch(research);
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        
        if (knowledge != null && !knowledge.isResearchComplete(finalResearch)) {
            if (res != null && res.getParents() != null) {
                for (String rsi : res.getParentsStripped()) {
                    giveRecursiveResearch(player, rsi);
                }
            }
            if (res != null && res.getStages() != null) {
                for (ResearchStage page : res.getStages()) {
                    if (page.getResearch() != null) {
                        for (String gr : page.getResearch()) {
                            ResearchManager.completeResearch(player, gr);
                        }
                    }
                }
            }
            ResearchManager.completeResearch(player, finalResearch);
            
            // Addendums
            for (String rc : ResearchCategories.researchCategories.keySet()) {
                for (ResearchEntry ri : ResearchCategories.getResearchCategory(rc).research.values()) {
                    if (ri.getStages() != null) {
                        for (ResearchStage stage : ri.getStages()) {
                            if (stage.getResearch() != null && Arrays.asList(stage.getResearch()).contains(finalResearch)) {
                                knowledge.setResearchFlag(ri.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE);
                                break;
                            }
                        }
                    }
                }
            }
            
            if (res != null && res.getSiblings() != null) {
                for (String rsi : res.getSiblings()) {
                    giveRecursiveResearch(player, rsi);
                }
            }
        }
    }

    private static int revokeResearch(CommandSourceStack source, ServerPlayer player, String research) {
        if (ResearchCategories.getResearch(research) != null) {
            revokeRecursiveResearch(player, research);
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge != null) {
                knowledge.sync(player);
            }
            source.sendSuccess(() -> Component.literal("В§5Revoked research " + research + " from " + player.getName().getString()), true);
        } else {
            source.sendFailure(Component.literal("В§cResearch does not exist."));
        }
        return 1;
    }

    public static void revokeRecursiveResearch(ServerPlayer player, String research) {
        if (research.contains("@")) {
            research = research.substring(0, research.indexOf("@"));
        }
        String finalResearch = research;
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        
        if (knowledge != null && knowledge.isResearchComplete(finalResearch)) {
            for (String rc : ResearchCategories.researchCategories.keySet()) {
                for (ResearchEntry ri : ResearchCategories.getResearchCategory(rc).research.values()) {
                    if (ri != null && ri.getParents() != null && knowledge.isResearchComplete(ri.getKey())) {
                        for (String rsi : ri.getParentsStripped()) {
                            if (rsi.equals(finalResearch)) {
                                revokeRecursiveResearch(player, ri.getKey());
                            }
                        }
                    }
                }
            }
            knowledge.removeResearch(finalResearch);
        }
    }

    private static int addWarp(CommandSourceStack source, ServerPlayer player, int amount, String type) {
        IPlayerWarp.EnumWarpType warpType = IPlayerWarp.EnumWarpType.NORMAL;
        if (type.equalsIgnoreCase("PERM")) warpType = IPlayerWarp.EnumWarpType.PERMANENT;
        else if (type.equalsIgnoreCase("TEMP")) warpType = IPlayerWarp.EnumWarpType.TEMPORARY;

        final IPlayerWarp.EnumWarpType finalWarpType = warpType;
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null) {
            warp.add(finalWarpType, amount);
            warp.sync(player);
        }
        
        if (type.isEmpty()) {
            PacketHandler.sendToPlayer(new PacketWarpMessage(player, (byte)0, amount), player);
        }
        
        source.sendSuccess(() -> Component.literal("В§5Added " + amount + " " + finalWarpType.name() + " warp to " + player.getName().getString()), true);
        return 1;
    }

    private static int setWarp(CommandSourceStack source, ServerPlayer player, int amount, String type) {
        IPlayerWarp.EnumWarpType warpType = IPlayerWarp.EnumWarpType.NORMAL;
        if (type.equalsIgnoreCase("PERM")) warpType = IPlayerWarp.EnumWarpType.PERMANENT;
        else if (type.equalsIgnoreCase("TEMP")) warpType = IPlayerWarp.EnumWarpType.TEMPORARY;

        final IPlayerWarp.EnumWarpType finalWarpType = warpType;
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null) {
            warp.set(finalWarpType, amount);
            warp.sync(player);
        }
        
        source.sendSuccess(() -> Component.literal("В§5Set " + finalWarpType.name() + " warp to " + amount + " for " + player.getName().getString()), true);
        return 1;
    }
}




