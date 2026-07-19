package com.gly091020.SableRagdollLib.command;

import com.gly091020.SableRagdollLib.api.RagdollHelper;
import com.gly091020.SableRagdollLib.api.RagdollTypeRegistry;
import com.gly091020.SableRagdollLib.common.DefFileLoader;
import com.gly091020.SableRagdollLib.common.RagdollReloadListener;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public class SableRagdollLibCommand {
    public static final String COMMAND = "sable_ragdoll_lib";
    public static void registry(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal(COMMAND);

        root.requires(source -> source.hasPermission(2));
        root.then(Commands.literal("reload").executes(SableRagdollLibCommand::reload));
        root.then(Commands.literal("create")
                .then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests(RagdollSuggestionProvider.INSTANCE)
                        .executes(SableRagdollLibCommand::create)));

        dispatcher.register(root);
    }

    public static int create(CommandContext<CommandSourceStack> context){
        var id = ResourceLocationArgument.getId(context, "id");
        var defFile = DefFileLoader.getDefFile(id);
        if(defFile == null){
            context.getSource().sendFailure(Component.translatable("command.sableragdolllib.create.no_id"));
            return 0;
        }
        var type = RagdollTypeRegistry.getRagdollType(defFile.type());
        if(type == null){
            context.getSource().sendFailure(Component.translatable("command.sableragdolllib.create.no_id"));
            return 0;
        }

        try{
            RagdollHelper.createRagdoll(context.getSource().getLevel(), context.getSource().getPosition(), id);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.sableragdolllib.create.error").append(e.getLocalizedMessage()));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("command.sableragdolllib.create.success", id.toString()), false);
        return 1;
    }

    public static int reload(CommandContext<CommandSourceStack> context){
        var resourceManager = context.getSource().getLevel().getServer().getResourceManager();
        RagdollReloadListener.reload(resourceManager);
        context.getSource().sendSuccess(() -> Component.translatable("command.sableragdolllib.reload"), false);
        return 1;
    }

    public static class RagdollSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        public static final RagdollSuggestionProvider INSTANCE = new RagdollSuggestionProvider();
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            DefFileLoader.getAllKeys().forEach(r -> builder.suggest(r.toString()));
            return builder.buildFuture();
        }
    }
}
